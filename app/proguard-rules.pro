# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.swappergallery.**$$serializer { *; }
-keepclassmembers class com.swappergallery.** {
    *** Companion;
}
-keepclasseswithmembers class com.swappergallery.** {
    kotlinx.serialization.KSerializer serializer(...);
}
