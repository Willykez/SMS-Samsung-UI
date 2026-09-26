# OneMessages release ProGuard/R8 rules.
# Applied on top of AGP's default proguard-android-optimize.txt.

# ---- Room ----
# Room's own consumer rules (bundled in the AAR) already keep most of what's
# needed for entities/DAOs/generated *_Impl classes, but these are kept
# explicitly since a broken Room mapping would silently corrupt local data
# rather than crash loudly.
-keep class com.oneui.sms.data.local.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# ---- WorkManager ----
# CoroutineWorker subclasses are instantiated by WorkManager's default
# WorkerFactory via reflection using the (Context, WorkerParameters)
# constructor — R8 must not rename/remove the class or that constructor,
# or scheduled sends (#1) and the retention purge (#11) silently stop firing
# with no crash to point at.
-keep class com.oneui.sms.worker.** extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.oneui.sms.worker.ScheduledSendWorker { *; }
-keep class com.oneui.sms.worker.RetentionPurgeWorker { *; }

# ---- Manifest-declared components ----
# AGP auto-keeps classes referenced in AndroidManifest.xml, but kept
# explicitly here since these are entry points the system instantiates by
# name (BroadcastReceiver/Service), where an R8 mistake fails silently at
# runtime rather than at compile time.
-keep class com.oneui.sms.receiver.** { *; }
-keep class com.oneui.sms.reminder.ReminderReceiver { *; }
-keep class com.oneui.sms.MainActivity { *; }
-keep class com.oneui.sms.OneMessagesApp { *; }

# ---- Kotlin coroutines ----
# Standard recommended rules — kotlinx.coroutines references some
# optional/debug-only classes that don't exist on Android at runtime.
-dontwarn kotlinx.coroutines.debug.**
-keepclassmembernames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ---- Kotlin metadata ----
# Preserves @Metadata so reflection-based parts of Room/Kotlin stdlib can
# still read data class component info after obfuscation.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keep class kotlin.Metadata { *; }
