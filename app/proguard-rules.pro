# Project specific ProGuard rules

# Keep our data models so GSON/Firebase can parse them correctly
-keep class com.basanti.app.TmdbMovie { *; }
-keep class com.basanti.app.TmdbMovieDetail { *; }
-keep class com.basanti.app.TmdbSearchResponse { *; }
-keep class com.basanti.app.Video { *; }
-keep class com.basanti.app.MovieRequest { *; }

# Retrofit HTTP annotations (CRITICAL)
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}

# Kotlin metadata (CRITICAL for Gson)
-keep class kotlin.Metadata { *; }

# Reflection constructors for GSON/Firebase
-keepclassmembers class * {
    public <init>(...);
}

# WorkManager rules
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Room (used by WorkManager)
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Retrofit rules
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod

# GSON rules
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.** { *; }

# Firebase rules (usually handled by the SDK, but safe to keep)
-keep class com.google.firebase.** { *; }

# Coil rules
-keep class coil.** { *; }

# Media3 / ExoPlayer rules
-keep class androidx.media3.** { *; }

# AndroidX / Compose specific
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# To preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
