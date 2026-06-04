# Keep kotlinx.serialization-generated companions on @Serializable classes;
# R8 otherwise strips them and json (de)serialization breaks at runtime.
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations
-keepattributes Signature

-keep,includedescriptorclasses class xyz.ludothegreat.audiobooktv.**$$serializer { *; }
-keepclassmembers class xyz.ludothegreat.audiobooktv.** {
    *** Companion;
}
-keepclasseswithmembers class xyz.ludothegreat.audiobooktv.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt + Dagger generated code paths.
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# Media3 reflects on its own classes for some session callbacks.
-keep class androidx.media3.session.** { *; }

# Retrofit interface methods are looked up by name reflectively.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepattributes RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
