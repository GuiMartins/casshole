# Jakarta/JavaMail (com.sun.mail:android-mail, android-activation) loads IMAP
# providers via reflection at runtime (META-INF/javamail.providers) - R8
# stripping/renaming these classes breaks IMAP silently at runtime, not at
# build time. Keep the whole tree, matching the library's own documented
# ProGuard recommendation.
-keep class com.sun.mail.** { *; }
-keep class javax.mail.** { *; }
-keep class com.sun.activation.** { *; }
-keep class javax.activation.** { *; }
-dontwarn com.sun.mail.**
-dontwarn javax.mail.**
-dontwarn javax.activation.**

# PdfBox-Android has known R8 warnings for optional/unused codecs it
# references reflectively - safe to silence, this app never hits those paths.
-dontwarn com.tom_roush.**
-dontwarn org.apache.pdfbox.**

# Google Tink (androidx.security:security-crypto's crypto backend for
# EncryptedSharedPreferences) references compile-time-only annotations
# (errorprone, javax.annotation) that don't exist at runtime and aren't
# needed there - safe to silence, confirmed by building+testing this
# specific class of warning live.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
