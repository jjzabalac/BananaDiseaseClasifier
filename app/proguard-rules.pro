# Mantener las clases del modelo y datos
-keep class com.bananascan.classifier.data.** { *; }
-keep class com.bananascan.classifier.Classification { *; }
-keep class com.bananascan.classifier.MainActivity { *; }

# Reglas específicas para la UI
-keep class com.bananascan.classifier.screens.** { *; }
-keep class com.bananascan.classifier.ui.theme.** { *; }

# Reglas para Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Mantener clases de Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Reglas para Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Reglas para Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Reglas para TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Reglas para Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# Mantener anotaciones importantes
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends android.app.Application { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Reglas específicas para tu app
-keepclassmembers class com.bananascan.classifier.** { *; }
-keepattributes JavascriptInterface

# Mantener todas las clases serializables
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Reglas específicas para TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-keep class org.tensorflow.lite.nnapi.** { *; }
-keep interface org.tensorflow.lite.gpu.** { *; }
-keep interface org.tensorflow.lite.nnapi.** { *; }

# Regla específica para GpuDelegateFactory
-keep class org.tensorflow.lite.gpu.GpuDelegateFactory$Options { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Reglas adicionales para TensorFlow
-keep class org.tensorflow.lite.support.** { *; }
-keep interface org.tensorflow.lite.support.** { *; }
-keep class org.tensorflow.lite.annotations.** { *; }

# Mantener las clases nativas
-keepclasseswithmembers class * {
    native <methods>;
}

# Reglas para evitar warnings de deprecación
-dontwarn org.tensorflow.lite.gpu.**
-dontwarn org.tensorflow.lite.nnapi.**