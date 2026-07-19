# ===== Sylo release (R8) keep rules =====
# Compose, Hilt, Room, WorkManager, Glance, DataStore and OkHttp ship their own
# consumer rules — only libraries that don't (or that use JNI/reflection in ways
# R8 can't see) are listed here.

# --- SQLCipher (net.zetetic:sqlcipher-android) ---
# JNI in both directions: Java methods are registered/called from native code,
# so R8 must neither strip nor rename anything in the package. Missing rules
# surface as UnsatisfiedLinkError / NoSuchMethodError on first DB open.
-keep class net.zetetic.database.** { *; }

# --- kotlinx.serialization ---
# Serializers are looked up reflectively through the generated `$$serializer`
# objects and `Companion.serializer()`; keep them for our own models
# (Navigation 3 route keys, network DTOs).
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.sylo.**$$serializer { *; }
-keepclassmembers class com.sylo.** {
    *** Companion;
}
-keepclasseswithmembers class com.sylo.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Retrofit ---
# Retrofit inspects generic signatures + annotations at runtime; R8 full mode
# needs these on top of the rules embedded in the jar.
-keepattributes Signature, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-dontwarn javax.annotation.**

# --- Debugging aids ---
# Keep readable stack traces in production crash reports.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
