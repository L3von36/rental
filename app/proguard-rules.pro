# Tink (pulled in by androidx.security:security-crypto) references compile-only
# Error Prone annotations that are never available at runtime; R8 fails on the
# missing classes by default (CanIgnoreReturnValue, CheckReturnValue, Immutable,
# RestrictedApi). Safe to ignore: they are CLASS-retention annotations.
-dontwarn com.google.errorprone.annotations.**

# Keep kotlinx.serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
