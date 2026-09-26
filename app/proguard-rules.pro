# OneMessages release R8 rules. Keep this file intentionally narrow so release
# shrinking/obfuscation can remove unused Compose/UI code while preserving
# Android/Room/WorkManager entry points.

# ---- Room ----
# Room entities/DAOs are discovered through annotations and generated code.
-keep @androidx.room.Entity class * { <fields>; }
-keep @androidx.room.Dao interface *
-keep class com.oneui.sms.data.local.**_Impl { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod

# ---- WorkManager scheduled messages / retention ----
# Workers are instantiated by WorkManager by class name. Keep only the two
# concrete workers and their required public constructor.
-keep class com.oneui.sms.worker.ScheduledSendWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.oneui.sms.worker.RetentionPurgeWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ---- Manifest entry points ----
# Explicit keep rules are retained for components instantiated by Android.
-keep class com.oneui.sms.OneMessagesApp { <init>(...); }
-keep class com.oneui.sms.MainActivity { <init>(...); }
-keep class com.oneui.sms.receiver.SmsSentReceiver { <init>(...); }
-keep class com.oneui.sms.receiver.SmsDeliveredReceiver { <init>(...); }
-keep class com.oneui.sms.receiver.NotificationReplyReceiver { <init>(...); }
-keep class com.oneui.sms.receiver.SmsDeliverReceiver { <init>(...); }
-keep class com.oneui.sms.receiver.MmsStubReceiver { <init>(...); }
-keep class com.oneui.sms.receiver.HeadlessSmsSendService { <init>(...); }
-keep class com.oneui.sms.reminder.ReminderReceiver { <init>(...); }

# ---- Kotlin / coroutine metadata ----
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-dontwarn kotlinx.coroutines.debug.**
