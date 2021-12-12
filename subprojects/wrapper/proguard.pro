#-dontoptimize
#-dontobfuscate
-allowaccessmodification
-overloadaggressively

-keepattributes Signature, SourceFile, LineNumberTable, *Annotation*

# Ignore warnings about these missing files.
-dontwarn javax.annotation.**
-dontwarn com.google.**
-dontwarn org.apache.**
-dontwarn net.covers1624.tconsole.**
-dontwarn net.covers1624.quack.**
-dontwarn org.fusesource.jansi.**
-dontwarn org.gradle.**
-dontwarn org.objectweb.**

# Don't obfuscate the following packages.
-keepnames class net.covers1624.wt.**
-keepnames class net.rubygrapefruit.**
-keepnames class org.apache.maven.model.**
-keepnames class org.apache.logging.**
-keepnames class org.apache.commons.logging.**

# Keep all wrapper classes.
-keep class net.covers1624.wt.** {
	public protected *;
}

-keep class org.apache.commons.logging.** {
	public protected private *;
}

# Keep enum special members.
-keepclassmembers enum * {
		*;
}

-keepnames class com.google.gson.annotations.** {
	<fields>;
	<methods>;
}

# Keep Natives.
-keepclasseswithmembers,allowshrinking class * {
    native <methods>;
}
