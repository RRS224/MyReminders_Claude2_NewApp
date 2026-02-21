# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}
-dontwarn androidx.room.**

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Firebase / Firestore ──────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── Firestore data models (must not be obfuscated) ────────────────────────────
-keep class com.example.myreminders_claude2.data.FirestoreReminder { *; }
-keep class com.example.myreminders_claude2.data.FirestoreCategory { *; }
-keep class com.example.myreminders_claude2.data.FirestoreTemplate { *; }

# ── Room entities and DAOs ────────────────────────────────────────────────────
-keep class com.example.myreminders_claude2.data.Reminder { *; }
-keep class com.example.myreminders_claude2.data.Category { *; }
-keep class com.example.myreminders_claude2.data.Template { *; }
-keep class com.example.myreminders_claude2.data.ReminderDao { *; }
-keep class com.example.myreminders_claude2.data.CategoryDao { *; }
-keep class com.example.myreminders_claude2.data.TemplateDao { *; }

# ── ViewModels ────────────────────────────────────────────────────────────────
-keep class com.example.myreminders_claude2.viewmodel.** { *; }

# ── Alarm / BroadcastReceivers / Services ────────────────────────────────────
-keep class com.example.myreminders_claude2.alarm.** { *; }

# ── Gson (if used) ────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# ── General Android ───────────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver