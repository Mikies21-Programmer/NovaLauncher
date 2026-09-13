# Reglas generales de Android
-keepattributes *Annotation*
-keepattributes Signature

# Gson data classes y modelos de datos
-keepclassmembers class com.daybreak.animelauncher.AppShortcut { <fields>; }
-keepclassmembers class com.daybreak.animelauncher.ViewConfig { <fields>; }
-keepclassmembers class com.daybreak.animelauncher.AdvancedStyleConfig { <fields>; }
-keepclassmembers class com.daybreak.animelauncher.DrawerCategory { <fields>; }
-keepclassmembers class com.daybreak.animelauncher.DrawerConfig { <fields>; }
-keepclassmembers class com.daybreak.animelauncher.GesturesConfig { <fields>; }
-keepclassmembers class com.daybreak.animelauncher.LauncherState { <fields>; }
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Media3 / ExoPlayer
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.ui.PlayerView { *; }

# Coil
-keep class coil.** { *; }

# Mantener los nombres de los métodos de las actividades para el sistema Android
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

