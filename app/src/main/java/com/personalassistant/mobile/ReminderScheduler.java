package com.personalassistant.mobile;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ReminderScheduler {
    private ReminderScheduler() {}

    private static final long[] OFFSETS = {86_400_000L, 3_600_000L, 600_000L};
    private static final String[] LABELS = {"1 day before", "1 hour before", "10 minutes before"};

    public static void scheduleAll(Context context) {
        DbHelper helper = new DbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT id,title,details,jdate,time_text FROM reminders", null);
        try {
            while (cursor.moveToNext()) {
                scheduleReminder(
                        context,
                        db,
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4)
                );
            }
        } finally {
            cursor.close();
            helper.close();
        }
    }

    public static void scheduleReminder(Context context, SQLiteDatabase db, long reminderId,
                                        String title, String details, String jdate, String timeText) {
        cancelReminder(context, reminderId);
        db.delete("reminder_alerts", "reminder_id=?", new String[]{String.valueOf(reminderId)});
        long reminderTime = Jalali.toMillis(jdate, timeText);
        long now = System.currentTimeMillis();
        for (int i = 0; i < OFFSETS.length; i++) {
            long trigger = reminderTime - OFFSETS[i];
            if (trigger <= now) continue;
            Intent intent = new Intent(context, NotificationReceiver.class);
            intent.setAction("com.personalassistant.mobile.REMINDER_" + reminderId + "_" + i);
            intent.putExtra("reminder_id", reminderId);
            intent.putExtra("title", title == null ? "Reminder" : title);
            intent.putExtra("details", details == null ? "" : details);
            intent.putExtra("jdate", jdate == null ? "" : jdate);
            intent.putExtra("time_text", timeText == null ? "" : timeText);
            intent.putExtra("alert_type", LABELS[i]);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode(reminderId, i),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, trigger, pendingIntent);
                }
            }
            android.content.ContentValues cv = new android.content.ContentValues();
            cv.put("reminder_id", reminderId);
            cv.put("alert_at_iso", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date(trigger)));
            cv.put("alert_type", LABELS[i]);
            cv.put("fired", 0);
            db.insert("reminder_alerts", null, cv);
        }
    }

    public static void cancelReminder(Context context, long reminderId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        for (int i = 0; i < OFFSETS.length; i++) {
            Intent intent = new Intent(context, NotificationReceiver.class);
            intent.setAction("com.personalassistant.mobile.REMINDER_" + reminderId + "_" + i);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode(reminderId, i),
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    private static int requestCode(long reminderId, int index) {
        long raw = reminderId * 10L + index;
        return (int) (raw % Integer.MAX_VALUE);
    }
}
