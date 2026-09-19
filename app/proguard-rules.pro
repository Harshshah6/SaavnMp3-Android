# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ─── Debugging ────────────────────────────────────────────────────────────────
# Uncomment to preserve line numbers for stack traces
#-keepattributes SourceFile,LineNumberTable
#-renamesourcefileattribute SourceFile

# ─── Kotlin ───────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }

# ─── Gson / JSON Models ───────────────────────────────────────────────────────
# Prevent R8 from stripping fields used by Gson for JSON deserialization
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep all data/model/record classes that Gson deserializes into
-keep class com.harsh.shah.saavnmp3.model.** { *; }
-keep class com.harsh.shah.saavnmp3.records.** { *; }

# ─── OkHttp ───────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ─── Glide ────────────────────────────────────────────────────────────────────
# Glide ships its own consumer ProGuard rules but keep these as a safety net
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# ─── Room ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ─── jAudioTagger ─────────────────────────────────────────────────────────────
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
# jAudioTagger uses SLF4J; StaticLoggerBinder is an optional binding removed in SLF4J 2.x
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.**

# ─── ExoPlayer / Media3 ───────────────────────────────────────────────────────
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }

# ─── AndroidX / Support ───────────────────────────────────────────────────────
-keep class androidx.core.app.CoreComponentFactory { *; }

# ─── WebView (if ever used) ───────────────────────────────────────────────────
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}