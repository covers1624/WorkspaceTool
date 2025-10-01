#-dontoptimize
#-dontobfuscate
-allowaccessmodification
-overloadaggressively
-mergeinterfacesaggressively
-optimizeaggressively

# Go away random crap
-dontnote **

-keepattributes Signature, SourceFile, LineNumberTable, *Annotation*

# Ignore warnings about these missing files.
-dontwarn javax.annotation.**
-dontwarn com.google.**
-dontwarn org.apache.**
-dontwarn net.covers1624.**
-dontwarn org.bouncycastle.**

# Don't obfuscate the following packages.
-keepnames class net.covers1624.**
-keepnames class net.covers1624.wstool.** {
	<fields>;
	<methods>;
}

-keepnames class net.covers1624.jdkutils.** {
	<fields>;
}

# Keep all wrapper classes.
-keep class net.covers1624.wstool.** {
	public protected *;
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
