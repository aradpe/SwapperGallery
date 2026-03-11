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

# Room — keep entities, DAOs, and database classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Hilt — keep ViewModels and injected classes
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.** class * { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <fields>;
    @javax.inject.* <init>(...);
}

# Keep our data model classes (used in serialization/Room)
-keep class com.swappergallery.data.model.** { *; }
-keep class com.swappergallery.data.db.** { *; }
