# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep audio effect classes accessed via reflection by the platform
-keep class android.media.audiofx.** { *; }
