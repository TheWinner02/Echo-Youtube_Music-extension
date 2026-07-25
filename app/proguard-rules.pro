# Add the missing rules to suppress warnings about java.beans classes
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor

# Suppress missing JVM desktop classes referenced by Ktor and logging
-dontwarn java.lang.management.**
-dontwarn io.ktor.**
-dontwarn org.slf4j.**

## Rules for NewPipeExtractor
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**

# Rhino JavaScript engine
-dontwarn javax.script.** 
-dontwarn jdk.dynalink.**

# JSoup re2j optional dependency
-dontwarn com.google.re2j.Matcher
-dontwarn com.google.re2j.Pattern

# Prevent R8 optimization issues and IllegalAccessError across library boundaries
-dontobfuscate
-dontoptimize
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keep class dev.brahmkshatriya.echo.extension.** { *; }
-keepclassmembers class dev.brahmkshatriya.echo.extension.** { *; }

-keep class dev.toastbits.ytmkt.** { *; }
-keepclassmembers class dev.toastbits.ytmkt.** { *; }

-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }

-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }

-keep class org.schabi.newpipe.** { *; }
-keepclassmembers class org.schabi.newpipe.** { *; }
