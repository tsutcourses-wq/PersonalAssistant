package com.personalassistant.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends Activity {
    private static final String APP_TITLE = "Personal Assistant";
    private static final String APP_VERSION = "4.0.0 Android";
    private static final String DB_NAME = "assistant_data.db";
    private static final int DB_VERSION = 4000;

    private DbHelper db;
    private boolean darkMode;
    private int bgColor, panelColor, panel2Color, textColor, mutedColor, primaryColor, dangerColor, lineColor;

    // Independent Android calendar state. The displayed dates are Jalali/Persian dates.
    private final Calendar calendarAnchor = Calendar.getInstance();
    private String calendarMode = "Month"; // Week, Month, Year

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("pa_settings", MODE_PRIVATE);
        darkMode = prefs.getBoolean("dark_mode", false);
        applyColors();
        db = new DbHelper(this);
        db.getWritableDatabase();
        showDashboard();
    }

    private void applyColors() {
        if (darkMode) {
            bgColor = 0xFF0F172A;
            panelColor = 0xFF111827;
            panel2Color = 0xFF1F2937;
            textColor = 0xFFF9FAFB;
            mutedColor = 0xFFCBD5E1;
            primaryColor = 0xFF60A5FA;
            dangerColor = 0xFFEF4444;
            lineColor = 0xFF334155;
        } else {
            bgColor = 0xFFEEF3F8;
            panelColor = 0xFFFFFFFF;
            panel2Color = 0xFFF8FBFF;
            textColor = 0xFF111827;
            mutedColor = 0xFF6B7280;
            primaryColor = 0xFF2563EB;
            dangerColor = 0xFFDC2626;
            lineColor = 0xFFD6E0EF;
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), lineColor);
        return d;
    }

    private TextView tv(String text, int sp, int color, int style) {
        TextView t = new TextView(this);
        t.setText(text == null ? "" : text);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, style);
        t.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        t.setPadding(dp(2), dp(2), dp(2), dp(2));
        return t;
    }

    private Button btn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(0xFFFFFFFF);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setBackground(rounded(primaryColor, 12));
        b.setMinHeight(dp(44));
        b.setPadding(dp(12), dp(6), dp(12), dp(6));
        return b;
    }

    private Button outlineBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(textColor);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setBackground(rounded(panel2Color, 12));
        b.setMinHeight(dp(44));
        b.setPadding(dp(12), dp(6), dp(12), dp(6));
        return b;
    }

    private Button dangerBtn(String text) {
        Button b = btn(text);
        b.setBackground(rounded(dangerColor, 12));
        return b;
    }

    private LinearLayout vbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout hbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout card() {
        LinearLayout c = vbox();
        c.setPadding(dp(12), dp(12), dp(12), dp(12));
        c.setBackground(rounded(panelColor, 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(10), dp(6), dp(10), dp(6));
        c.setLayoutParams(lp);
        return c;
    }

    private void addButton(LinearLayout parent, Button b, View.OnClickListener listener) {
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        parent.addView(b, lp);
    }

    private void addSmallButton(LinearLayout parent, Button b, View.OnClickListener listener) {
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        parent.addView(b, lp);
    }

    private void setScreen(String title, LinearLayout content) {
        LinearLayout root = vbox();
        root.setBackgroundColor(bgColor);

        LinearLayout header = vbox();
        header.setPadding(dp(14), dp(14), dp(14), dp(8));
        header.setBackgroundColor(primaryColor);
        header.addView(tv(title, 21, 0xFFFFFFFF, Typeface.BOLD));
        header.addView(tv(APP_TITLE + " • " + APP_VERSION, 12, 0xFFEAF2FF, Typeface.NORMAL));
        root.addView(header);

        HorizontalScrollView navScroll = new HorizontalScrollView(this);
        navScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout nav = hbox();
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        String[] items = {"Dashboard", "Reminders", "Tasks", "Settings"};
        for (String it : items) {
            Button nb = outlineBtn(it);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nlp.setMargins(dp(3), 0, dp(3), 0);
            nav.addView(nb, nlp);
            if (it.equals("Dashboard")) nb.setOnClickListener(v -> showDashboard());
            if (it.equals("Reminders")) nb.setOnClickListener(v -> showReminders());
            if (it.equals("Tasks")) nb.setOnClickListener(v -> showTasks());
            if (it.equals("Settings")) nb.setOnClickListener(v -> showSettings());
        }
        navScroll.addView(nav);
        root.addView(navScroll);

        ScrollView sc = new ScrollView(this);
        content.setPadding(0, dp(4), 0, dp(24));
        sc.addView(content);
        root.addView(sc, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private String nowIso() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
    }

    private String nowIsoMinute() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(new Date());
    }

    private String nowJalali() {
        Calendar c = Calendar.getInstance();
        int[] j = gregorianToJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        return jalaliText(j[0], j[1], j[2]);
    }

    private String nowTime() {
        return new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
    }

    private String jalaliText(int y, int m, int d) {
        return String.format(Locale.US, "%04d/%02d/%02d", y, m, d);
    }

    private String safe(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        if (idx < 0 || c.isNull(idx)) return "";
        return c.getString(idx);
    }

    private int safeInt(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        if (idx < 0 || c.isNull(idx)) return 0;
        return c.getInt(idx);
    }

    private long insert(String table, ContentValues cv) {
        return db.getWritableDatabase().insert(table, null, cv);
    }

    private void update(String table, ContentValues cv, long id) {
        db.getWritableDatabase().update(table, cv, "id=?", new String[]{String.valueOf(id)});
    }

    private void delete(String table, long id) {
        db.getWritableDatabase().delete(table, "id=?", new String[]{String.valueOf(id)});
    }

    // ------------------------------------------------------------
    // Dashboard
    // ------------------------------------------------------------

    private void showDashboard() {
        LinearLayout content = vbox();
        addCalendarSection(content);
        addUpcomingRemindersSection(content);
        addUpcomingTasksSection(content);
        addNotesSection(content);
        setScreen("Dashboard", content);
    }

    private void addUpcomingRemindersSection(LinearLayout content) {
        LinearLayout upcoming = card();
        LinearLayout titleRow = hbox();
        TextView title = tv("Upcoming reminders", 17, textColor, Typeface.BOLD);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button all = outlineBtn("See all");
        all.setOnClickListener(v -> showReminders());
        titleRow.addView(all, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        upcoming.addView(titleRow);

        Cursor cur = db.getReadableDatabase().rawQuery(
                "SELECT id,title,jdate,time_text,details FROM reminders " +
                "WHERE remind_at_iso>=? ORDER BY remind_at_iso LIMIT 5",
                new String[]{nowIsoMinute()});
        try {
            if (!cur.moveToFirst()) {
                upcoming.addView(tv("No upcoming reminders.", 13, mutedColor, Typeface.NORMAL));
            } else {
                do {
                    long id = cur.getLong(0);
                    String line = safe(cur, "title") + "\n" + safe(cur, "jdate") + "  " + safe(cur, "time_text");
                    Button rb = outlineBtn(line);
                    rb.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                    addButton(upcoming, rb, v -> editReminder(id));
                } while (cur.moveToNext());
            }
        } finally {
            cur.close();
        }
        content.addView(upcoming);
    }

    private void addUpcomingTasksSection(LinearLayout content) {
        LinearLayout tasks = card();
        tasks.addView(tv("Upcoming tasks", 17, textColor, Typeface.BOLD));

        Cursor cur = db.getReadableDatabase().rawQuery(
                "SELECT id,title,details,due_jdate,due_time,priority,done,due_iso FROM general_tasks " +
                "WHERE done=0 ORDER BY CASE WHEN due_iso='' THEN 1 ELSE 0 END, due_iso, id LIMIT 5", null);
        try {
            if (!cur.moveToFirst()) {
                tasks.addView(tv("No open tasks.", 13, mutedColor, Typeface.NORMAL));
            } else {
                do {
                    long id = cur.getLong(0);
                    String due = safe(cur, "due_jdate");
                    String time = safe(cur, "due_time");
                    String priority = safe(cur, "priority");
                    String second = "";
                    if (!due.isEmpty()) second = due + (time.isEmpty() ? "" : "  " + time);
                    if (!priority.isEmpty()) second += (second.isEmpty() ? "" : " • ") + priority;
                    Button tb = outlineBtn(safe(cur, "title") + (second.isEmpty() ? "" : "\n" + second));
                    tb.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                    addButton(tasks, tb, v -> editTask(id));
                } while (cur.moveToNext());
            }
        } finally {
            cur.close();
        }

        LinearLayout buttons = hbox();
        addSmallButton(buttons, btn("+ Add task"), v -> editTask(0));
        addSmallButton(buttons, outlineBtn("See all tasks"), v -> showTasks());
        tasks.addView(buttons);
        content.addView(tasks);
    }

    private void addNotesSection(LinearLayout content) {
        LinearLayout notes = card();
        notes.addView(tv("Notes", 17, textColor, Typeface.BOLD));
        notes.addView(tv("English and فارسی can be entered in the same note.", 12, mutedColor, Typeface.NORMAL));
        String saved = getSharedPreferences("pa_settings", MODE_PRIVATE).getString("dashboard_note", "");
        EditText note = input("Write a note / یادداشت", saved, true);
        note.setMinLines(5);
        note.setMaxLines(14);
        notes.addView(note);
        LinearLayout buttons = hbox();
        addSmallButton(buttons, btn("Save note"), v -> {
            getSharedPreferences("pa_settings", MODE_PRIVATE).edit()
                    .putString("dashboard_note", note.getText().toString()).apply();
            toast("Note saved on this phone.");
        });
        addSmallButton(buttons, outlineBtn("Clear"), v -> {
            note.setText("");
            getSharedPreferences("pa_settings", MODE_PRIVATE).edit().putString("dashboard_note", "").apply();
        });
        notes.addView(buttons);
        content.addView(notes);
    }

    // ------------------------------------------------------------
    // Jalali calendar: week / month / year navigation
    // ------------------------------------------------------------

    private void addCalendarSection(LinearLayout content) {
        LinearLayout calCard = card();

        LinearLayout titleRow = hbox();
        titleRow.addView(tv("Calendar", 18, textColor, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button addReminder = btn("+ Add reminder");
        addReminder.setOnClickListener(v -> editReminder(0));
        titleRow.addView(addReminder, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        calCard.addView(titleRow);

        LinearLayout modes = hbox();
        addSmallButton(modes, calendarMode.equals("Week") ? btn("Week") : outlineBtn("Week"), v -> {
            calendarMode = "Week";
            showDashboard();
        });
        addSmallButton(modes, calendarMode.equals("Month") ? btn("Month") : outlineBtn("Month"), v -> {
            calendarMode = "Month";
            showDashboard();
        });
        addSmallButton(modes, calendarMode.equals("Year") ? btn("Year") : outlineBtn("Year"), v -> {
            calendarMode = "Year";
            showDashboard();
        });
        calCard.addView(modes);

        int[] anchorJ = gregorianToJalali(calendarAnchor.get(Calendar.YEAR), calendarAnchor.get(Calendar.MONTH) + 1, calendarAnchor.get(Calendar.DAY_OF_MONTH));
        String heading;
        if (calendarMode.equals("Week")) heading = "Week around " + jalaliText(anchorJ[0], anchorJ[1], anchorJ[2]);
        else if (calendarMode.equals("Year")) heading = "Year " + anchorJ[0];
        else heading = persianMonthName(anchorJ[1]) + " " + anchorJ[0];
        TextView headingTv = tv(heading, 15, primaryColor, Typeface.BOLD);
        headingTv.setGravity(Gravity.CENTER);
        headingTv.setPadding(0, dp(8), 0, dp(5));
        calCard.addView(headingTv);

        LinearLayout nav = hbox();
        addSmallButton(nav, outlineBtn("◀ Previous"), v -> shiftCalendar(-1));
        addSmallButton(nav, outlineBtn("Today"), v -> {
            calendarAnchor.setTimeInMillis(System.currentTimeMillis());
            showDashboard();
        });
        addSmallButton(nav, outlineBtn("Next ▶"), v -> shiftCalendar(1));
        calCard.addView(nav);

        if (calendarMode.equals("Week")) renderWeekCalendar(calCard);
        else if (calendarMode.equals("Year")) renderYearCalendar(calCard);
        else renderMonthCalendar(calCard);

        calCard.addView(tv("Use Previous/Next or swipe left/right on this calendar. Tap a day to add a reminder for that date.", 11, mutedColor, Typeface.NORMAL));

        final float[] downX = {0f};
        calCard.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                downX[0] = event.getX();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float dx = event.getX() - downX[0];
                if (Math.abs(dx) > dp(70)) {
                    shiftCalendar(dx < 0 ? 1 : -1);
                    return true;
                }
            }
            return false;
        });

        content.addView(calCard);
    }

    private void shiftCalendar(int direction) {
        if (calendarMode.equals("Week")) {
            calendarAnchor.add(Calendar.DAY_OF_MONTH, 7 * direction);
        } else {
            int[] j = gregorianToJalali(calendarAnchor.get(Calendar.YEAR), calendarAnchor.get(Calendar.MONTH) + 1, calendarAnchor.get(Calendar.DAY_OF_MONTH));
            if (calendarMode.equals("Year")) {
                j[0] += direction;
            } else {
                j[1] += direction;
                if (j[1] > 12) { j[1] = 1; j[0]++; }
                if (j[1] < 1) { j[1] = 12; j[0]--; }
            }
            j[2] = Math.min(j[2], jalaliMonthLength(j[0], j[1]));
            setCalendarAnchorJalali(j[0], j[1], j[2]);
        }
        showDashboard();
    }

    private void renderWeekCalendar(LinearLayout parent) {
        Calendar start = (Calendar) calendarAnchor.clone();
        int offset = (start.get(Calendar.DAY_OF_WEEK) - Calendar.SATURDAY + 7) % 7;
        start.add(Calendar.DAY_OF_MONTH, -offset);

        LinearLayout names = hbox();
        String[] weekNames = {"Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri"};
        for (String n : weekNames) {
            TextView h = tv(n, 11, mutedColor, Typeface.BOLD);
            h.setGravity(Gravity.CENTER);
            names.addView(h, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        parent.addView(names);

        LinearLayout days = hbox();
        for (int i = 0; i < 7; i++) {
            Calendar d = (Calendar) start.clone();
            d.add(Calendar.DAY_OF_MONTH, i);
            int[] j = gregorianToJalali(d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1, d.get(Calendar.DAY_OF_MONTH));
            String date = jalaliText(j[0], j[1], j[2]);
            Button b = outlineBtn(String.valueOf(j[2]));
            b.setTextSize(13);
            b.setOnClickListener(v -> editReminderForDate(date));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(52), 1f);
            lp.setMargins(dp(1), dp(2), dp(1), dp(2));
            days.addView(b, lp);
        }
        parent.addView(days);
    }

    private void renderMonthCalendar(LinearLayout parent) {
        int[] j = gregorianToJalali(calendarAnchor.get(Calendar.YEAR), calendarAnchor.get(Calendar.MONTH) + 1, calendarAnchor.get(Calendar.DAY_OF_MONTH));
        int y = j[0], m = j[1];
        int len = jalaliMonthLength(y, m);
        int[] g1 = jalaliToGregorian(y, m, 1);
        Calendar first = Calendar.getInstance();
        first.set(g1[0], g1[1] - 1, g1[2], 12, 0, 0);
        int offset = (first.get(Calendar.DAY_OF_WEEK) - Calendar.SATURDAY + 7) % 7;

        LinearLayout names = hbox();
        String[] weekNames = {"Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri"};
        for (String n : weekNames) {
            TextView h = tv(n, 11, mutedColor, Typeface.BOLD);
            h.setGravity(Gravity.CENTER);
            names.addView(h, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        parent.addView(names);

        int day = 1;
        for (int row = 0; row < 6 && day <= len; row++) {
            LinearLayout r = hbox();
            for (int col = 0; col < 7; col++) {
                if ((row == 0 && col < offset) || day > len) {
                    TextView blank = tv("", 12, mutedColor, Typeface.NORMAL);
                    r.addView(blank, new LinearLayout.LayoutParams(0, dp(48), 1f));
                } else {
                    final int selectedDay = day;
                    String date = jalaliText(y, m, selectedDay);
                    Button b = outlineBtn(String.valueOf(selectedDay));
                    b.setTextSize(13);
                    b.setOnClickListener(v -> editReminderForDate(date));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f);
                    lp.setMargins(dp(1), dp(1), dp(1), dp(1));
                    r.addView(b, lp);
                    day++;
                }
            }
            parent.addView(r);
        }
    }

    private void renderYearCalendar(LinearLayout parent) {
        int[] j = gregorianToJalali(calendarAnchor.get(Calendar.YEAR), calendarAnchor.get(Calendar.MONTH) + 1, calendarAnchor.get(Calendar.DAY_OF_MONTH));
        final int year = j[0];
        for (int row = 0; row < 4; row++) {
            LinearLayout r = hbox();
            for (int col = 0; col < 3; col++) {
                final int month = row * 3 + col + 1;
                Button b = outlineBtn(month + "\n" + persianMonthName(month));
                b.setTextSize(12);
                addSmallButton(r, b, v -> {
                    setCalendarAnchorJalali(year, month, 1);
                    calendarMode = "Month";
                    showDashboard();
                });
            }
            parent.addView(r);
        }
    }

    private String persianMonthName(int m) {
        String[] names = {"", "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"};
        if (m < 1 || m > 12) return "Month " + m;
        return names[m];
    }

    private void setCalendarAnchorJalali(int jy, int jm, int jd) {
        int[] g = jalaliToGregorian(jy, jm, jd);
        calendarAnchor.set(g[0], g[1] - 1, g[2], 12, 0, 0);
    }

    private int jalaliMonthLength(int jy, int jm) {
        int ny = jy, nm = jm + 1;
        if (nm == 13) { nm = 1; ny++; }
        int[] g1 = jalaliToGregorian(jy, jm, 1);
        int[] g2 = jalaliToGregorian(ny, nm, 1);
        Calendar c1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Calendar c2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c1.clear(); c2.clear();
        c1.set(g1[0], g1[1] - 1, g1[2], 0, 0, 0);
        c2.set(g2[0], g2[1] - 1, g2[2], 0, 0, 0);
        return (int) ((c2.getTimeInMillis() - c1.getTimeInMillis()) / 86400000L);
    }

    // ------------------------------------------------------------
    // Reminders
    // ------------------------------------------------------------

    private void showReminders() {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Reminders", 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Add reminder"), v -> editReminder(0));
        content.addView(top);

        Cursor cur = db.getReadableDatabase().rawQuery(
                "SELECT id,title,details,jdate,time_text,remind_at_iso FROM reminders ORDER BY remind_at_iso", null);
        try {
            if (!cur.moveToFirst()) {
                LinearLayout empty = card();
                empty.addView(tv("No reminders yet.", 14, mutedColor, Typeface.NORMAL));
                content.addView(empty);
            } else {
                do {
                    long id = cur.getLong(0);
                    LinearLayout c = card();
                    c.addView(tv(safe(cur, "title"), 17, textColor, Typeface.BOLD));
                    c.addView(tv(safe(cur, "jdate") + "  " + safe(cur, "time_text"), 14, primaryColor, Typeface.BOLD));
                    String det = safe(cur, "details");
                    if (!det.isEmpty()) c.addView(tv(det, 13, mutedColor, Typeface.NORMAL));
                    addButton(c, outlineBtn("Edit reminder"), v -> editReminder(id));
                    content.addView(c);
                } while (cur.moveToNext());
            }
        } finally {
            cur.close();
        }
        setScreen("Reminders", content);
    }

    private void editReminderForDate(String date) {
        editReminderInternal(0, date);
    }

    private void editReminder(long id) {
        editReminderInternal(id, null);
    }

    private void editReminderInternal(long id, String presetDate) {
        String title = "", details = "", jdate = presetDate == null ? nowJalali() : presetDate, time = nowTime();
        boolean exists = id > 0;
        if (exists) {
            Cursor cur = db.getReadableDatabase().rawQuery("SELECT * FROM reminders WHERE id=?", new String[]{String.valueOf(id)});
            try {
                if (cur.moveToFirst()) {
                    title = safe(cur, "title");
                    details = safe(cur, "details");
                    jdate = safe(cur, "jdate");
                    time = safe(cur, "time_text");
                }
            } finally {
                cur.close();
            }
        }

        LinearLayout content = vbox();
        LinearLayout f = card();
        f.addView(tv(exists ? "Edit reminder" : "Add reminder", 18, textColor, Typeface.BOLD));
        EditText titleE = input("Title", title, false);
        EditText detailsE = input("Details", details, true);
        EditText dateE = input("Jalali date, e.g. 1405/05/21", jdate, false);
        EditText timeE = input("Time, e.g. 14:30", time, false);
        f.addView(titleE);
        f.addView(detailsE);
        f.addView(dateE);
        f.addView(timeE);

        addButton(f, btn("Save reminder"), v -> {
            String t = titleE.getText().toString().trim();
            if (t.isEmpty()) { toast("Please enter a title."); return; }
            ContentValues cv = new ContentValues();
            cv.put("title", t);
            cv.put("details", detailsE.getText().toString());
            cv.putNull("project_id");
            cv.put("jdate", dateE.getText().toString().trim());
            cv.put("time_text", timeE.getText().toString().trim());
            cv.put("remind_at_iso", reminderIso(cv.getAsString("jdate"), cv.getAsString("time_text")));
            long reminderId;
            if (exists) {
                update("reminders", cv, id);
                reminderId = id;
            } else {
                cv.put("created_at", nowIso());
                reminderId = insert("reminders", cv);
            }
            createReminderAlerts(reminderId, cv.getAsString("remind_at_iso"));
            toast("Reminder saved.");
            showDashboard();
        });

        if (exists) {
            addButton(f, dangerBtn("Delete reminder"), v -> {
                delete("reminders", id);
                toast("Reminder deleted.");
                showReminders();
            });
        }
        addButton(f, outlineBtn("Back"), v -> showDashboard());
        content.addView(f);
        setScreen(exists ? "Edit reminder" : "Add reminder", content);
    }

    private void createReminderAlerts(long reminderId, String iso) {
        db.getWritableDatabase().delete("reminder_alerts", "reminder_id=?", new String[]{String.valueOf(reminderId)});
        String[] types = {"1 day before", "1 hour before", "10 minutes before"};
        for (String type : types) {
            ContentValues cv = new ContentValues();
            cv.put("reminder_id", reminderId);
            cv.put("alert_at_iso", iso);
            cv.put("alert_type", type);
            cv.put("fired", 0);
            db.getWritableDatabase().insert("reminder_alerts", null, cv);
        }
    }

    // ------------------------------------------------------------
    // Generic tasks - no project/student task type
    // ------------------------------------------------------------

    private void showTasks() {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Tasks", 18, textColor, Typeface.BOLD));
        top.addView(tv("Tasks are independent. There is no Project task or Student task type in the Android app.", 12, mutedColor, Typeface.NORMAL));
        addButton(top, btn("+ Add task"), v -> editTask(0));
        content.addView(top);

        Cursor cur = db.getReadableDatabase().rawQuery(
                "SELECT id,title,details,due_jdate,due_time,priority,done,due_iso FROM general_tasks " +
                "ORDER BY done, CASE WHEN due_iso='' THEN 1 ELSE 0 END, due_iso, id", null);
        try {
            if (!cur.moveToFirst()) {
                LinearLayout empty = card();
                empty.addView(tv("No tasks yet.", 14, mutedColor, Typeface.NORMAL));
                content.addView(empty);
            } else {
                do {
                    long id = cur.getLong(0);
                    int done = safeInt(cur, "done");
                    LinearLayout c = card();
                    c.addView(tv((done == 1 ? "✓ " : "") + safe(cur, "title"), 17, textColor, Typeface.BOLD));
                    String due = safe(cur, "due_jdate");
                    String time = safe(cur, "due_time");
                    String priority = safe(cur, "priority");
                    String meta = "";
                    if (!due.isEmpty()) meta = due + (time.isEmpty() ? "" : "  " + time);
                    if (!priority.isEmpty()) meta += (meta.isEmpty() ? "" : " • ") + priority;
                    if (!meta.isEmpty()) c.addView(tv(meta, 13, primaryColor, Typeface.BOLD));
                    String det = safe(cur, "details");
                    if (!det.isEmpty()) c.addView(tv(det, 13, mutedColor, Typeface.NORMAL));

                    LinearLayout actions = hbox();
                    addSmallButton(actions, outlineBtn("Edit"), v -> editTask(id));
                    addSmallButton(actions, outlineBtn(done == 1 ? "Reopen" : "Done"), v -> {
                        ContentValues cv = new ContentValues();
                        cv.put("done", done == 1 ? 0 : 1);
                        cv.put("completed_at", done == 1 ? "" : nowIso());
                        update("general_tasks", cv, id);
                        showTasks();
                    });
                    c.addView(actions);
                    content.addView(c);
                } while (cur.moveToNext());
            }
        } finally {
            cur.close();
        }
        setScreen("Tasks", content);
    }

    private void editTask(long id) {
        String title = "", details = "", dueDate = "", dueTime = "", priority = "Normal";
        int done = 0;
        boolean exists = id > 0;
        if (exists) {
            Cursor cur = db.getReadableDatabase().rawQuery("SELECT * FROM general_tasks WHERE id=?", new String[]{String.valueOf(id)});
            try {
                if (cur.moveToFirst()) {
                    title = safe(cur, "title");
                    details = safe(cur, "details");
                    dueDate = safe(cur, "due_jdate");
                    dueTime = safe(cur, "due_time");
                    priority = safe(cur, "priority");
                    done = safeInt(cur, "done");
                }
            } finally {
                cur.close();
            }
        }

        LinearLayout content = vbox();
        LinearLayout f = card();
        f.addView(tv(exists ? "Edit task" : "Add task", 18, textColor, Typeface.BOLD));
        f.addView(tv("This is a general personal task; it is not linked to a project or student.", 12, mutedColor, Typeface.NORMAL));
        EditText titleE = input("Task title", title, false);
        EditText detailsE = input("Details", details, true);
        EditText dateE = input("Due date (Jalali), optional", dueDate, false);
        EditText timeE = input("Due time, optional", dueTime, false);
        EditText priorityE = input("Priority: Low / Normal / High", priority, false);
        f.addView(titleE);
        f.addView(detailsE);
        f.addView(dateE);
        f.addView(timeE);
        f.addView(priorityE);

        final int existingDone = done;
        addButton(f, btn("Save task"), v -> {
            String t = titleE.getText().toString().trim();
            if (t.isEmpty()) { toast("Please enter a task title."); return; }
            String d = dateE.getText().toString().trim();
            String tm = timeE.getText().toString().trim();
            String dueIso = d.isEmpty() ? "" : reminderIso(d, tm.isEmpty() ? "00:00" : tm);
            ContentValues cv = new ContentValues();
            cv.put("title", t);
            cv.put("details", detailsE.getText().toString());
            cv.put("due_jdate", d);
            cv.put("due_time", tm);
            cv.put("due_iso", dueIso);
            cv.put("priority", priorityE.getText().toString().trim().isEmpty() ? "Normal" : priorityE.getText().toString().trim());
            if (exists) {
                cv.put("done", existingDone);
                update("general_tasks", cv, id);
            } else {
                cv.put("done", 0);
                cv.put("created_at", nowIso());
                cv.put("completed_at", "");
                cv.put("legacy_key", "");
                insert("general_tasks", cv);
            }
            toast("Task saved.");
            showTasks();
        });

        if (exists) {
            addButton(f, outlineBtn(done == 1 ? "Mark as open" : "Mark as done"), v -> {
                ContentValues cv = new ContentValues();
                cv.put("done", existingDone == 1 ? 0 : 1);
                cv.put("completed_at", existingDone == 1 ? "" : nowIso());
                update("general_tasks", cv, id);
                showTasks();
            });
            addButton(f, dangerBtn("Delete task"), v -> {
                delete("general_tasks", id);
                toast("Task deleted.");
                showTasks();
            });
        }
        addButton(f, outlineBtn("Back"), v -> showDashboard());
        content.addView(f);
        setScreen(exists ? "Edit task" : "Add task", content);
    }

    // ------------------------------------------------------------
    // Settings - deliberately no database/sync/server sections
    // ------------------------------------------------------------

    private void showSettings() {
        LinearLayout content = vbox();
        LinearLayout appearance = card();
        appearance.addView(tv("Appearance", 18, textColor, Typeface.BOLD));
        appearance.addView(tv("The Android app is now maintained separately from the Windows application.", 13, mutedColor, Typeface.NORMAL));
        addButton(appearance, outlineBtn(darkMode ? "Switch to day view" : "Switch to night view"), v -> toggleDarkMode());
        content.addView(appearance);

        LinearLayout about = card();
        about.addView(tv("About this Android edition", 17, textColor, Typeface.BOLD));
        about.addView(tv("Dashboard, Jalali calendar, reminders, general tasks and notes are available. Students, projects, courses, file inventory, database/sync and central-server controls are intentionally hidden/removed from the Android interface.", 13, mutedColor, Typeface.NORMAL));
        about.addView(tv("Existing legacy tables are not deleted during upgrade, so installing this version does not intentionally erase old student/project/course data already stored in the app database.", 12, mutedColor, Typeface.NORMAL));
        content.addView(about);
        setScreen("Settings", content);
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        getSharedPreferences("pa_settings", MODE_PRIVATE).edit().putBoolean("dark_mode", darkMode).apply();
        applyColors();
        showDashboard();
    }

    // ------------------------------------------------------------
    // Input + date helpers
    // ------------------------------------------------------------

    private EditText input(String label, String value, boolean multiLine) {
        EditText e = new EditText(this);
        e.setHint(label);
        e.setText(value == null ? "" : value);
        e.setTextColor(textColor);
        e.setHintTextColor(mutedColor);
        e.setTextSize(15);
        e.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        e.setGravity(Gravity.START | (multiLine ? Gravity.TOP : Gravity.CENTER_VERTICAL));
        e.setSingleLine(!multiLine);
        e.setMinHeight(dp(multiLine ? 82 : 52));
        if (multiLine) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(3);
            e.setMaxLines(8);
        } else {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        e.setTextIsSelectable(true);
        e.setPadding(dp(10), dp(4), dp(10), dp(4));
        e.setBackground(rounded(panel2Color, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        e.setLayoutParams(lp);
        return e;
    }

    private String normalizeDigits(String s) {
        if (s == null) return "";
        String fa = "۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩";
        String en = "01234567890123456789";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            int idx = fa.indexOf(ch);
            out.append(idx >= 0 ? en.charAt(idx) : ch);
        }
        return out.toString().trim();
    }

    private String reminderIso(String jdate, String timeText) {
        String d = normalizeDigits(jdate).replace('-', '/').replace('.', '/');
        String t = normalizeDigits(timeText == null ? "00:00" : timeText).trim();
        int hh = 0, mm = 0;
        try {
            String[] tp = t.split(":");
            hh = Integer.parseInt(tp[0].trim());
            if (tp.length > 1) mm = Integer.parseInt(tp[1].trim());
        } catch (Exception ignored) {}
        try {
            String[] p = d.split("/");
            int y = Integer.parseInt(p[0].trim());
            int m = Integer.parseInt(p[1].trim());
            int day = Integer.parseInt(p[2].trim());
            int[] g;
            if (y < 1700) g = jalaliToGregorian(y, m, day); else g = new int[]{y, m, day};
            return String.format(Locale.US, "%04d-%02d-%02dT%02d:%02d", g[0], g[1], g[2], hh, mm);
        } catch (Exception ex) {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(new Date());
        }
    }

    private int[] gregorianToJalali(int gy, int gm, int gd) {
        int[] gdm = {0,31,59,90,120,151,181,212,243,273,304,334};
        int gy2 = (gm > 2) ? gy + 1 : gy;
        int days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1];
        int jy = -1595 + 33 * (days / 12053);
        days %= 12053;
        jy += 4 * (days / 1461);
        days %= 1461;
        if (days > 365) {
            jy += (days - 1) / 365;
            days = (days - 1) % 365;
        }
        int jm, jd;
        if (days < 186) {
            jm = 1 + days / 31;
            jd = 1 + days % 31;
        } else {
            jm = 7 + (days - 186) / 30;
            jd = 1 + (days - 186) % 30;
        }
        return new int[]{jy, jm, jd};
    }

    private int[] jalaliToGregorian(int jy, int jm, int jd) {
        jy += 1595;
        int days = -355668 + (365 * jy) + (jy / 33) * 8 + ((jy % 33 + 3) / 4) + jd + (jm < 7 ? (jm - 1) * 31 : ((jm - 7) * 30 + 186));
        int gy = 400 * (days / 146097);
        days %= 146097;
        if (days > 36524) {
            gy += 100 * (--days / 36524);
            days %= 36524;
            if (days >= 365) days++;
        }
        gy += 4 * (days / 1461);
        days %= 1461;
        if (days > 365) {
            gy += (days - 1) / 365;
            days = (days - 1) % 365;
        }
        int gd = days + 1;
        int[] salA = {0,31, (isLeap(gy) ? 29 : 28),31,30,31,30,31,31,30,31,30,31};
        int gm;
        for (gm = 1; gm <= 12 && gd > salA[gm]; gm++) gd -= salA[gm];
        return new int[]{gy, gm, gd};
    }

    private boolean isLeap(int y) {
        return (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0);
    }

    // ------------------------------------------------------------
    // Database
    // ------------------------------------------------------------

    public static class DbHelper extends SQLiteOpenHelper {
        public DbHelper(Activity ctx) {
            super(ctx, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            db.setForeignKeyConstraintsEnabled(true);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            ensureSchema(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            ensureSchema(db);
        }

        private void ensureSchema(SQLiteDatabase db) {
            // Simplified Android-facing data. Legacy tables are not dropped.
            db.execSQL("CREATE TABLE IF NOT EXISTS reminders (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, details TEXT DEFAULT '', project_id INTEGER, remind_at_iso TEXT NOT NULL, jdate TEXT NOT NULL, time_text TEXT NOT NULL, created_at TEXT NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS reminder_alerts (id INTEGER PRIMARY KEY AUTOINCREMENT, reminder_id INTEGER NOT NULL, alert_at_iso TEXT NOT NULL, alert_type TEXT NOT NULL, fired INTEGER DEFAULT 0, UNIQUE(reminder_id, alert_type), FOREIGN KEY(reminder_id) REFERENCES reminders(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS general_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, details TEXT DEFAULT '', due_jdate TEXT DEFAULT '', due_time TEXT DEFAULT '', due_iso TEXT DEFAULT '', priority TEXT DEFAULT 'Normal', done INTEGER DEFAULT 0, created_at TEXT NOT NULL, completed_at TEXT DEFAULT '', legacy_key TEXT DEFAULT '')");
            addColumn(db, "general_tasks", "details", "TEXT DEFAULT ''");
            addColumn(db, "general_tasks", "due_jdate", "TEXT DEFAULT ''");
            addColumn(db, "general_tasks", "due_time", "TEXT DEFAULT ''");
            addColumn(db, "general_tasks", "due_iso", "TEXT DEFAULT ''");
            addColumn(db, "general_tasks", "priority", "TEXT DEFAULT 'Normal'");
            addColumn(db, "general_tasks", "done", "INTEGER DEFAULT 0");
            addColumn(db, "general_tasks", "created_at", "TEXT DEFAULT ''");
            addColumn(db, "general_tasks", "completed_at", "TEXT DEFAULT ''");
            addColumn(db, "general_tasks", "legacy_key", "TEXT DEFAULT ''");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_general_tasks_legacy_key ON general_tasks(legacy_key) WHERE legacy_key<>''");
            db.execSQL("CREATE TABLE IF NOT EXISTS app_settings (key TEXT PRIMARY KEY, value TEXT DEFAULT '')");

            // Upgrade safety: old Project/Student tasks are copied once into the new generic task list.
            migrateLegacyTasks(db);
        }

        private static void addColumn(SQLiteDatabase db, String table, String column, String type) {
            Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            boolean found = false;
            try {
                while (c.moveToNext()) {
                    if (column.equals(c.getString(1))) { found = true; break; }
                }
            } finally { c.close(); }
            if (!found) db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }

        private static boolean tableExists(SQLiteDatabase db, String table) {
            Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{table});
            try { return c.moveToFirst(); } finally { c.close(); }
        }

        private static void migrateLegacyTasks(SQLiteDatabase db) {
            if (tableExists(db, "todos")) {
                Cursor c = db.rawQuery("SELECT id,title,done,due_iso,due_jdate,responsible,created_at FROM todos", null);
                try {
                    while (c.moveToNext()) {
                        String key = "todo:" + c.getLong(0);
                        ContentValues cv = new ContentValues();
                        cv.put("title", c.isNull(1) ? "Task" : c.getString(1));
                        String responsible = c.isNull(5) ? "" : c.getString(5);
                        cv.put("details", responsible.isEmpty() ? "" : "Responsible: " + responsible);
                        cv.put("due_jdate", c.isNull(4) ? "" : c.getString(4));
                        cv.put("due_time", "");
                        cv.put("due_iso", c.isNull(3) ? "" : c.getString(3));
                        cv.put("priority", "Normal");
                        cv.put("done", c.getInt(2));
                        cv.put("created_at", c.isNull(6) ? "" : c.getString(6));
                        cv.put("completed_at", "");
                        cv.put("legacy_key", key);
                        db.insertWithOnConflict("general_tasks", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
                    }
                } finally { c.close(); }
            }

            if (tableExists(db, "student_tasks")) {
                Cursor c = db.rawQuery("SELECT id,title,details,done,created_at,completed_at FROM student_tasks", null);
                try {
                    while (c.moveToNext()) {
                        String key = "student_task:" + c.getLong(0);
                        ContentValues cv = new ContentValues();
                        cv.put("title", c.isNull(1) ? "Task" : c.getString(1));
                        cv.put("details", c.isNull(2) ? "" : c.getString(2));
                        cv.put("due_jdate", "");
                        cv.put("due_time", "");
                        cv.put("due_iso", "");
                        cv.put("priority", "Normal");
                        cv.put("done", c.getInt(3));
                        cv.put("created_at", c.isNull(4) ? "" : c.getString(4));
                        cv.put("completed_at", c.isNull(5) ? "" : c.getString(5));
                        cv.put("legacy_key", key);
                        db.insertWithOnConflict("general_tasks", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
                    }
                } finally { c.close(); }
            }
        }
    }
}
