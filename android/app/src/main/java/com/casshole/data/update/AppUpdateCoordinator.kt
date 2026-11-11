package com.casshole.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.casshole.BuildConfig
import com.casshole.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed class AppUpdateState {
    data object Idle : AppUpdateState()
    data object Checking : AppUpdateState()
    data object UpToDate : AppUpdateState()

    /** Local build ("0.0.0-dev"): not a release, so there's nothing to compare against. */
    data object DevBuild : AppUpdateState()
    data class Downloading(val version: String, val progress: Float) : AppUpdateState()
    data class Ready(val version: String, val apk: File) : AppUpdateState()
    data class Installing(val version: String, val apk: File) : AppUpdateState()
    data class Failed(val message: String) : AppUpdateState()
}

/**
 * Self-update from the project's GitHub Releases - the app is side-loaded, there's no
 * store doing this for it. Once per cold start it checks the latest release and, when
 * it's newer, downloads the APK in the background; installing is one tap (Dashboard
 * banner or Settings > Sobre), because installing an update kills the running app and
 * doing that on its own would close it mid-use.
 *
 * The install goes through PackageInstaller, the platform's session API. With
 * UPDATE_PACKAGES_WITHOUT_USER_ACTION (Android 12+) a self-update needs no system
 * confirmation once the user has allowed Casshole to install apps; otherwise the system
 * shows its own dialog. Android itself refuses an APK not signed with the same key, so
 * a debug build can never be "updated" into the release one.
 */
object AppUpdateCoordinator {

    private const val TAG = "AppUpdateCoordinator"
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/GuiMartins/casshole/releases/latest"
    private const val UPDATES_DIR = "updates"
    const val ACTION_INSTALL_STATUS = "com.casshole.action.UPDATE_INSTALL_STATUS"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    private var autoCheckTriggered = false

    private val installedVersion: SemVer? = SemVer.parse(BuildConfig.VERSION_NAME)

    fun checkOnAppStart(context: Context) {
        if (autoCheckTriggered) return
        autoCheckTriggered = true
        check(context.applicationContext, silent = true)
    }

    /** Manual "check for updates" from Settings. */
    fun checkNow(context: Context) {
        check(context.applicationContext, silent = false)
    }

    private fun check(context: Context, silent: Boolean) {
        val current = installedVersion
        if (current == null) {
            _state.value = AppUpdateState.DevBuild
            return
        }
        when (_state.value) {
            is AppUpdateState.Checking, is AppUpdateState.Downloading,
            is AppUpdateState.Ready, is AppUpdateState.Installing -> return
            else -> {}
        }
        _state.value = AppUpdateState.Checking
        scope.launch {
            val release = try {
                ReleaseParser.newerThan(current, ReleaseParser.parse(httpGet(LATEST_RELEASE_URL)))
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed", e)
                // The automatic check runs on every launch, offline included - not worth an error.
                _state.value = if (silent) {
                    AppUpdateState.Idle
                } else {
                    AppUpdateState.Failed(context.getString(R.string.update_error_check))
                }
                return@launch
            }
            if (release == null) {
                clearDownloads(context, keep = null)
                _state.value = AppUpdateState.UpToDate
                return@launch
            }
            download(context, release)
        }
    }

    private suspend fun download(context: Context, release: ReleaseInfo) {
        val version = release.version.toString()
        val target = File(File(context.cacheDir, UPDATES_DIR), release.apkName)
        clearDownloads(context, keep = target)
        try {
            // Already fetched on an earlier launch (user hadn't installed it yet).
            val alreadyDownloaded = target.exists() && release.apkSize > 0 && target.length() == release.apkSize
            if (!alreadyDownloaded) {
                _state.value = AppUpdateState.Downloading(version, 0f)
                val partial = File(target.parentFile, "${target.name}.part")
                downloadTo(release.apkUrl, partial, release.apkSize) { progress ->
                    _state.value = AppUpdateState.Downloading(version, progress)
                }
                if (release.apkSize > 0 && partial.length() != release.apkSize) {
                    error("Size mismatch: ${partial.length()} != ${release.apkSize}")
                }
                partial.renameTo(target)
            }
            if (!isValidUpdate(context, target)) {
                target.delete()
                _state.value = AppUpdateState.Failed(context.getString(R.string.update_error_invalid_apk))
                return
            }
            _state.value = AppUpdateState.Ready(version, target)
        } catch (e: Exception) {
            Log.w(TAG, "Update download failed", e)
            _state.value = AppUpdateState.Failed(context.getString(R.string.update_error_download))
        }
    }

    /** Same package and a higher versionCode - the signature is checked by Android itself. */
    private fun isValidUpdate(context: Context, apk: File): Boolean {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageArchiveInfo(apk.path, 0) ?: return false
        return info.packageName == context.packageName &&
            PackageInfoCompat.getLongVersionCode(info) > BuildConfig.VERSION_CODE
    }

    fun install(context: Context) {
        val ready = _state.value as? AppUpdateState.Ready ?: return
        val appContext = context.applicationContext
        _state.value = AppUpdateState.Installing(ready.version, ready.apk)
        scope.launch {
            try {
                commitInstallSession(appContext, ready.apk)
            } catch (e: Exception) {
                Log.w(TAG, "Could not start the install session", e)
                _state.value = AppUpdateState.Failed(
                    appContext.getString(R.string.update_error_install, e.message.orEmpty()),
                )
            }
        }
    }

    private suspend fun commitInstallSession(context: Context, apk: File) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("casshole.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val statusIntent = Intent(context, UpdateInstallReceiver::class.java)
                .setAction(ACTION_INSTALL_STATUS)
            // Mutable: the system fills in the status extras. Explicit intent, so that's allowed.
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                statusIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pendingIntent.intentSender)
        }
    }

    /** Called by [UpdateInstallReceiver] when the install didn't go through. */
    internal fun onInstallFailed(context: Context, status: Int, message: String?) {
        val installing = _state.value as? AppUpdateState.Installing ?: return
        if (status == PackageInstaller.STATUS_FAILURE_ABORTED) {
            // The user dismissed the system dialog - leave the update on offer.
            _state.value = AppUpdateState.Ready(installing.version, installing.apk)
            return
        }
        val reason = when (status) {
            PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                context.getString(R.string.update_error_signature)
            else -> message.orEmpty()
        }
        _state.value = AppUpdateState.Failed(context.getString(R.string.update_error_install, reason))
    }

    private fun clearDownloads(context: Context, keep: File?) {
        val dir = File(context.cacheDir, UPDATES_DIR)
        dir.mkdirs()
        dir.listFiles()?.filter { it != keep }?.forEach { it.delete() }
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            // GitHub's API rejects requests without a User-Agent.
            setRequestProperty("User-Agent", "Casshole-Android")
            setRequestProperty("Accept", "application/vnd.github+json")
        }

    private fun httpGet(url: String): String {
        val connection = openConnection(url)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                error("HTTP ${connection.responseCode}")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** The asset URL redirects to GitHub's CDN (https -> https), which HttpURLConnection follows. */
    private fun downloadTo(url: String, file: File, expectedSize: Long, onProgress: (Float) -> Unit) {
        val connection = openConnection(url)
        connection.setRequestProperty("Accept", "application/octet-stream")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                error("HTTP ${connection.responseCode}")
            }
            val total = expectedSize.takeIf { it > 0 } ?: connection.contentLengthLong
            var written = 0L
            var lastReported = -1
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) {
                            val percent = (written * 100 / total).toInt()
                            if (percent != lastReported) {
                                lastReported = percent
                                onProgress(percent / 100f)
                            }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
