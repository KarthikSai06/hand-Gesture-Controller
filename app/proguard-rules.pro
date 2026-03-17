# ── MediaPipe ──────────────────────────────────────────────────────────────────
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.mediapipe.proto.**

# ── TensorFlow Lite ────────────────────────────────────────────────────────────
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-dontwarn org.tensorflow.**

# ── App services (must survive minification) ───────────────────────────────────
-keep class com.gesturecontrol.service.GestureAccessibilityService { *; }
-keep class com.gesturecontrol.service.CameraForegroundService { *; }
-keep class com.gesturecontrol.service.GestureQuickTileService { *; }

# ── App data classes used by TFLite / GestureEventBus ─────────────────────────
-keep class com.gesturecontrol.GestureEventBus { *; }
-keep class com.gesturecontrol.GestureEventBus$* { *; }
-keep class com.gesturecontrol.GestureSettings { *; }
-keep class com.gesturecontrol.recognition.** { *; }

# ── CameraX ────────────────────────────────────────────────────────────────────
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ── Kotlin coroutines / reflection ─────────────────────────────────────────────
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# ── Java annotation processors (suppress warnings from annotation libs) ────────
-dontwarn javax.annotation.processing.**
-dontwarn javax.lang.model.**

# ── General: suppress any remaining warnings from third-party libs ─────────────
-dontwarn com.google.common.**
-ignorewarnings
