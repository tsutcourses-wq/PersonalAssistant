package com.personalassistant.mobile;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "personal_assistant_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        ensureChannel(context);
        long reminderId = intent.getLongExtra("reminder_id", 0L);
        String title = intent.getStringExtra("title");
        String details = intent.getStringExtra("details");
        String jdate = intent.getStringExtra("jdate");
        String time = intent.getStringExtra("time_text");
        String alertType = intent.getStringExtra("alert_type");
        int notificationId = intent.getIntExtra("notification_id", (int) (reminderId % Integer.MAX_VALUE));
        if (title == null || title.trim().isEmpty()) title = "Personal Assistant reminder";
        if (details == null) details = "";
        String summary = (alertType == null ? "" : alertType + " • ") + (jdate == null ? "" : jdate) + " " + (time == null ? "" : time);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.putExtra("open_reminders", true);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                (int) (reminderId % Integer.MAX_VALUE),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(details.isEmpty() ? summary : details)
                .setSubText(summary)
                .setStyle(new Notification.BigTextStyle().bigText(details.isEmpty() ? summary : details + "\n" + summary))
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setDefaults(Notification.DEFAULT_ALL)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis());

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(notificationId, builder.build());

        if (reminderId > 0 && alertType != null) {
            DbHelper helper = new DbHelper(context);
            SQLiteDatabase db = helper.getWritableDatabase();
            android.content.ContentValues cv = new android.content.ContentValues();
            cv.put("fired", 1);
            db.update("reminder_alerts", cv, "reminder_id=? AND alert_type=?", new String[]{String.valueOf(reminderId), alertType});
            helper.close();
        }
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Personal Assistant reminders",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Notifications for reminders created in Personal Assistant");
                channel.enableVibration(true);
                manager.createNotificationChannel(channel);
            }
        }
    }
}
