# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ViewModels are instantiated reflectively by the AndroidX factory; keep their
# (Application) constructors so R8 doesn't strip them in the release build.
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
