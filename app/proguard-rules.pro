# Keep Media3 / ExoPlayer classes (reflection-based extractor loading)
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep WebView JS bridge (dipanggil dari JavaScript)
-keepclassmembers class id.irgunawan.winanp.MainActivity$WinanpNativeBridge {
    public *;
}
