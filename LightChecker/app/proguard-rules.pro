# LightChecker ProGuard Rules

# Keep line numbers for crash reports (Crashlytics)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Room - keep entity classes
-keep class com.bowlof.lightchecker.data.local.db.** { *; }

# Hilt
-dontwarn dagger.hilt.**

# WorkManager
-keep class com.bowlof.lightchecker.work.** { *; }

# Glance widget
-keep class com.bowlof.lightchecker.widget.** { *; }
-keep class androidx.glance.** { *; }

# Firestore DTOs
-keep class com.bowlof.lightchecker.data.remote.dto.** { *; }

# Keep enum values (LocationSource, SelectedScheduleDay)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
