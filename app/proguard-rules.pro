# Add project specific ProGuard rules here.
# https://developer.android.com/guide/developing/tools/proguard.html

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Google Mobile Ads
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# Mediation / Meta Audience Network
-keep class com.google.ads.mediation.** { *; }
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**

# User Messaging Platform (consent)
-keep class com.google.android.ump.** { *; }
