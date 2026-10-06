# PassVaultGen ProGuard/R8 rules
# No secrets are embedded in the application. These rules only guard against
# reflection-based access to security-critical classes and keep the release
# build minified.

# Keep Bouncy Castle Argon2 classes (used reflectively in some paths).
-keep class org.bouncycastle.crypto.generators.Argon2BytesGenerator { *; }
-keep class org.bouncycastle.crypto.params.Argon2Parameters { *; }

# Keep Room entities and DAOs (Room uses reflection/annotation processing).
-keep class com.example.passwordvault.data.database.** { *; }

# Keep backup models: Gson serializes field names into the `.pvault` format,
# so obfuscation would corrupt backups and break cross-version restore.
-keep class com.example.passwordvault.security.backup.** { *; }

# Keep Hilt generated components.
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Strip logging in release (SecureLogger already gates on BuildConfig.DEBUG).
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
