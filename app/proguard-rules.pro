# Reglas generales de Android
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# Reglas para Gson (evita que se borren los campos de tus clases de datos)
-keep class com.daybreak.animelauncher.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Reglas para Media3 / ExoPlayer
-keep class androidx.media3.** { *; }

# Reglas para Coil
-keep class coil.** { *; }

# Mantener los nombres de los métodos de las actividades para el sistema Android
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
