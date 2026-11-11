#---------------------------------------------------------------------------------------------------
# Exponea SDK
#---------------------------------------------------------------------------------------------------

-keep class com.exponea.** {*;}

-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable

# GSON proguard R8 full mode
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation @interface com.google.gson.annotations.SerializedName
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
# Gson uses generic type information stored in a class file when working with
# fields. Proguard removes such information by default, keep it.
-keepattributes Signature

# https://r8.googlesource.com/r8/+/refs/heads/main/compatibility-faq.md#troubleshooting-gson-gson
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Optional. For using GSON @Expose annotation
-keepattributes AnnotationDefault,RuntimeVisibleAnnotations
-keep class com.google.gson.reflect.TypeToken { <fields>; }
-keepclassmembers class **$TypeAdapterFactory { <fields>; }

# Room < 2.7 ships "-keep class * extends androidx.room.RoomDatabase" without members, so R8 in AGP 9
# strips the no-arg constructor that Room calls reflectively (e.g. androidx.work.impl.WorkDatabase_Impl).
-keep class * extends androidx.room.RoomDatabase { void <init>(); }

# WorkManager < 2.10 ships "-keep class * extends androidx.work.InputMerger" without members, so R8 in AGP 9
# strips the no-arg constructor that WorkManager calls reflectively (e.g. androidx.work.OverwritingInputMerger).
-keep class * extends androidx.work.InputMerger { void <init>(); }
