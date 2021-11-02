#-dontoptimize
#-dontobfuscate
-allowaccessmodification
-overloadaggressively

-keepattributes Signature, SourceFile, LineNumberTable

# Ignore warnings about these missing files.
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn com.google.**
-dontwarn org.eclipse.**
-dontwarn org.apache.**
-dontwarn net.covers1624.tconsole.**
-dontwarn net.covers1624.quack.**
-dontwarn org.fusesource.jansi.**
-dontwarn org.gradle.**

# Don't obfuscate the following packages.
-keepnames class net.covers1624.wt.**
-keepnames class net.rubygrapefruit.**
-keepnames class org.apache.maven.model.**
-keepnames class org.apache.logging.**

# Keep all wrapper classes.
-keep class net.covers1624.wt.** {
	public protected *;
}

# Breaks maven resolution if these classes are stripped.
-keep class org.apache.maven.model.** {
	public protected private *;
}

# Keep enum special members.
-keepclassmembers enum  * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Natives.
-keepclasseswithmembers,allowshrinking class * {
    native <methods>;
}
