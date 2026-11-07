# SecureBox release rules.
# Keep the app free of reflection surprises while shrinking aggressively.

-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# Kotlinx Serialization keeps its generated serializers reachable.
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static **$* *;
}
-keepclassmembers class **$$serializer {
    *** descriptor;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt-generated components are referenced from the manifest/bytecode.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep the app's own entry points and BuildConfig constants used at runtime.
-keep class io.securebox.SecureBoxApp { *; }
-keep class io.securebox.MainActivity { *; }

# OkHttp/Okio platform hooks.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# Ed25519 feed verification. The library has an optional branch for the JDK-only X509Key class,
# which does not exist on Android; keep the concrete keys and ignore that missing reference.
-dontwarn sun.security.x509.X509Key
-keep class net.i2p.crypto.eddsa.** { *; }

