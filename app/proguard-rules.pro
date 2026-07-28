# Keep kotlinx.serialization models used for Room JSON persistence
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.pedroeu.ficha.**$$serializer { *; }
-keepclassmembers class com.pedroeu.ficha.** {
    *** Companion;
}
-keepclasseswithmembers class com.pedroeu.ficha.** {
    kotlinx.serialization.KSerializer serializer(...);
}
