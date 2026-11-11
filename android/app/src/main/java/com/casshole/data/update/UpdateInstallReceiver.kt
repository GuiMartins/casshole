package com.casshole.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat

/**
 * Receives the PackageInstaller session status. On success there's nothing to do: the
 * update replaces this very app, which the system restarts as the new version.
 */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AppUpdateCoordinator.ACTION_INSTALL_STATUS) return
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // System confirmation (or "allow installing apps from Casshole" on first use).
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                    ?: return
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> {}
            else -> AppUpdateCoordinator.onInstallFailed(
                context,
                status,
                intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE),
            )
        }
    }
}
