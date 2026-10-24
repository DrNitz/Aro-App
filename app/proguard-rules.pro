# ProGuard/R8 rules for Aro.
# Applied by the release buildType (isMinifyEnabled = true). They keep Room,
# kotlinx.serialization and the Nav3 keys intact through shrinking.

# ---- kotlinx.serialization ------------------------------------------------
# Keep generated serializers for @Serializable classes and their companions.
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations, AnnotationDefault
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.drnitz.aro.**$$serializer { *; }
-keepclassmembers class com.drnitz.aro.** {
    *** Companion;
}
-keepclasseswithmembers class com.drnitz.aro.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Room -----------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# ---- Navigation 3 ---------------------------------------------------------
# NavKeys are @Serializable data objects; keep their generated serializers.
-keep class com.drnitz.aro.**$$serializer { *; }

# ---- Compose / Kotlin metadata -------------------------------------------
-dontwarn org.jetbrains.annotations.**