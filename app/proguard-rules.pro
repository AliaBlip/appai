# HyAI ProGuard Rules

# Keep Gson models
-keepclassmembers class com.hyai.app.MainActivity$ChatMessage { *; }
-keepclassmembers class com.hyai.app.MainActivity$ModelInfo { *; }

# Keep data classes
-keepattributes Signature
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod
