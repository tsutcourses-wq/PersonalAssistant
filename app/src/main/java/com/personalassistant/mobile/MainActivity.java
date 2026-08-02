package com.personalassistant.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String APP_TITLE = "Personal Assistant";
    private static final String APP_VERSION = "3.6.66 Android";
    private static final String DB_NAME = "assistant_data.db";
    private static final int REQ_IMPORT_DB = 1001;
    private static final int REQ_EXPORT_DB = 1002;
    private static final int REQ_EXPORT_STUDENTS_CSV = 1003;
    private DbHelper db;
    private boolean darkMode;
    private int bgColor, panelColor, panel2Color, textColor, mutedColor, primaryColor, dangerColor, lineColor;

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

    private void addButton(LinearLayout parent, Button b, View.OnClickListener l) {
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        parent.addView(b, lp);
    }

    private void setScreen(String title, LinearLayout content) {
        LinearLayout root = vbox();
        root.setBackgroundColor(bgColor);

        LinearLayout header = vbox();
        header.setPadding(dp(14), dp(14), dp(14), dp(8));
        header.setBackgroundColor(primaryColor);
        TextView titleTv = tv(title, 21, 0xFFFFFFFF, Typeface.BOLD);
        TextView sub = tv(APP_TITLE + " • " + APP_VERSION, 12, 0xFFEAF2FF, Typeface.NORMAL);
        header.addView(titleTv);
        header.addView(sub);
        root.addView(header);

        HorizontalScrollView navScroll = new HorizontalScrollView(this);
        navScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout nav = hbox();
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        String[] items = {"Dashboard", "Reminders", "Students", "Projects", "Courses", "Settings"};
        for (String it : items) {
            Button nb = outlineBtn(it);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nlp.setMargins(dp(3), 0, dp(3), 0);
            nav.addView(nb, nlp);
            if (it.equals("Dashboard")) nb.setOnClickListener(v -> showDashboard());
            if (it.equals("Reminders")) nb.setOnClickListener(v -> showReminders());
            if (it.equals("Students")) nb.setOnClickListener(v -> showStudents());
            if (it.equals("Projects")) nb.setOnClickListener(v -> showProjects());
            if (it.equals("Courses")) nb.setOnClickListener(v -> showCourses());
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

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_LONG).show(); }

    private String nowIso() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
    }

    private String nowJalali() {
        Calendar c = Calendar.getInstance();
        int[] j = gregorianToJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        return String.format(Locale.US, "%04d/%02d/%02d", j[0], j[1], j[2]);
    }

    private String nowTime() {
        return new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
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

    private int count(String table) {
        Cursor c = db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + table, null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }

    private void showDashboard() {
        LinearLayout content = vbox();
        LinearLayout c = card();
        c.addView(tv("Mobile dashboard", 18, textColor, Typeface.BOLD));
        c.addView(tv("This Android version is adjusted for phone use. It uses the same assistant_data.db database format as the Windows version.", 13, mutedColor, Typeface.NORMAL));
        content.addView(c);

        LinearLayout counts = card();
        counts.addView(tv("Overview", 17, textColor, Typeface.BOLD));
        counts.addView(tv("Students: " + count("students"), 15, textColor, Typeface.NORMAL));
        counts.addView(tv("Projects: " + count("projects"), 15, textColor, Typeface.NORMAL));
        counts.addView(tv("Courses: " + count("courses"), 15, textColor, Typeface.NORMAL));
        counts.addView(tv("Reminders: " + count("reminders"), 15, textColor, Typeface.NORMAL));
        content.addView(counts);

        LinearLayout actions = card();
        actions.addView(tv("Quick actions", 17, textColor, Typeface.BOLD));
        addButton(actions, btn("Add reminder"), v -> editReminder(0));
        addButton(actions, btn("Add student"), v -> editStudent(0));
        addButton(actions, btn("Add project"), v -> editProject(0));
        addButton(actions, outlineBtn(darkMode ? "Switch to day view" : "Switch to night view"), v -> toggleDarkMode());
        content.addView(actions);

        LinearLayout upcoming = card();
        upcoming.addView(tv("Upcoming reminders", 17, textColor, Typeface.BOLD));
        Cursor cur = db.getReadableDatabase().rawQuery("SELECT id,title,jdate,time_text,details FROM reminders ORDER BY remind_at_iso LIMIT 5", null);
        try {
            if (!cur.moveToFirst()) {
                upcoming.addView(tv("No reminders.", 13, mutedColor, Typeface.NORMAL));
            } else {
                do {
                    int id = cur.getInt(0);
                    String line = safe(cur, "title") + "\n" + safe(cur, "jdate") + "  " + safe(cur, "time_text");
                    Button rb = outlineBtn(line);
                    rb.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                    addButton(upcoming, rb, v -> editReminder(id));
                } while (cur.moveToNext());
            }
        } finally { cur.close(); }
        content.addView(upcoming);
        setScreen("Dashboard", content);
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        getSharedPreferences("pa_settings", MODE_PRIVATE).edit().putBoolean("dark_mode", darkMode).apply();
        applyColors();
        showDashboard();
    }

    private void showSettings() {
        LinearLayout content = vbox();
        LinearLayout c = card();
        c.addView(tv("Database / sync", 18, textColor, Typeface.BOLD));
        c.addView(tv("Android cannot select a live Google Drive folder path like Windows. Use Import to load assistant_data.db from Google Drive, and Export/Sync to save the current database back to Google Drive.", 13, mutedColor, Typeface.NORMAL));
        c.addView(tv("Local Android database: " + getDatabasePath(DB_NAME).getAbsolutePath(), 12, mutedColor, Typeface.NORMAL));
        c.addView(tv("Records: students " + count("students") + ", projects " + count("projects") + ", courses " + count("courses") + ", reminders " + count("reminders"), 13, textColor, Typeface.NORMAL));
        addButton(c, btn("Import database from Google Drive / file"), v -> pickImportDatabase());
        addButton(c, btn("Export / Sync database now"), v -> pickExportDatabase());
        addButton(c, outlineBtn("Export student list for Excel (CSV)"), v -> pickExportStudentsCsv());
        addButton(c, outlineBtn(darkMode ? "Switch to day view" : "Switch to night view"), v -> toggleDarkMode());
        content.addView(c);

        LinearLayout help = card();
        help.addView(tv("Safe use", 17, textColor, Typeface.BOLD));
        help.addView(tv("1. On Windows, close the desktop program after editing.\n2. Wait for Google Drive to finish syncing assistant_data.db.\n3. On Android, open Settings and import assistant_data.db from Google Drive.\n4. After editing on Android, press Export / Sync database now and save it to Google Drive as assistant_data.db.\n5. Wait for Google Drive to sync before opening the Windows app again.", 13, mutedColor, Typeface.NORMAL));
        content.addView(help);
        setScreen("Settings", content);
    }

    private void pickImportDatabase() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, REQ_IMPORT_DB);
    }

    private void pickExportDatabase() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/octet-stream");
        i.putExtra(Intent.EXTRA_TITLE, DB_NAME);
        startActivityForResult(i, REQ_EXPORT_DB);
    }

    private void pickExportStudentsCsv() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/csv");
        i.putExtra(Intent.EXTRA_TITLE, "students_export.csv");
        startActivityForResult(i, REQ_EXPORT_STUDENTS_CSV);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == REQ_IMPORT_DB) {
                importDatabase(uri);
                showSettings();
            } else if (requestCode == REQ_EXPORT_DB) {
                exportDatabase(uri);
                showSettings();
            } else if (requestCode == REQ_EXPORT_STUDENTS_CSV) {
                exportStudentsCsv(uri);
                showSettings();
            }
        } catch (Exception ex) {
            toast("Operation failed: " + ex.getMessage());
        }
    }

    private void importDatabase(Uri uri) throws Exception {
        File dbFile = getDatabasePath(DB_NAME);
        File temp = new File(getCacheDir(), "import_check.db");
        try (InputStream in = getContentResolver().openInputStream(uri); OutputStream out = new FileOutputStream(temp, false)) {
            if (in == null) throw new Exception("Cannot open selected file.");
            copyStream(in, out);
        }
        SQLiteDatabase check = SQLiteDatabase.openDatabase(temp.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        Cursor cc = check.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='students'", null);
        boolean ok = cc.moveToFirst();
        cc.close();
        check.close();
        if (!ok) throw new Exception("Selected file is not a compatible assistant_data.db database.");

        db.close();
        File backup = new File(dbFile.getParentFile(), "assistant_data_backup_" + System.currentTimeMillis() + ".db");
        if (dbFile.exists()) copyFile(dbFile, backup);
        deleteSidecars(dbFile);
        copyFile(temp, dbFile);
        deleteSidecars(dbFile);
        temp.delete();
        db = new DbHelper(this);
        db.getWritableDatabase();
        toast("Database imported. Backup created before import.");
    }

    private void exportDatabase(Uri uri) throws Exception {
        File dbFile = getDatabasePath(DB_NAME);
        Cursor checkpoint = db.getWritableDatabase().rawQuery("PRAGMA wal_checkpoint(FULL)", null);
        if (checkpoint != null) checkpoint.close();
        try (InputStream in = new FileInputStream(dbFile); OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) throw new Exception("Cannot write to selected file.");
            copyStream(in, out);
        }
        toast("Database exported/synced.");
    }

    private void deleteSidecars(File dbFile) {
        try { new File(dbFile.getAbsolutePath() + "-wal").delete(); } catch (Exception ignored) {}
        try { new File(dbFile.getAbsolutePath() + "-shm").delete(); } catch (Exception ignored) {}
    }

    private void copyFile(File src, File dst) throws Exception {
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) { copyStream(in, out); }
    }

    private void copyStream(InputStream in, OutputStream out) throws Exception {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
    }

    private String displayName(Uri uri) {
        String result = "";
        if ("content".equals(uri.getScheme())) {
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (idx >= 0) result = c.getString(idx);
                    }
                } finally { c.close(); }
            }
        }
        if (result == null || result.trim().isEmpty()) result = uri.getLastPathSegment();
        return result == null ? "file" : result;
    }

    private void exportStudentsCsv(Uri uri) throws Exception {
        String[] headers = {"Level", "First name", "Family name", "Student No", "National ID", "Registration date", "Registration semester", "Email", "Telephone", "Supervisor", "Second supervisor", "Advisor", "Status", "Thesis status", "Research background", "Seminar", "Proposal", "Notes"};
        StringBuilder sb = new StringBuilder();
        appendCsvRow(sb, headers);
        Cursor c = db.getReadableDatabase().rawQuery("SELECT * FROM students ORDER BY level,family_name,first_name", null);
        try {
            while (c.moveToNext()) {
                appendCsvRow(sb, new String[]{safe(c,"level"), safe(c,"first_name"), safe(c,"family_name"), safe(c,"student_no"), safe(c,"national_id"), safe(c,"registration_date"), safe(c,"registration_semester"), safe(c,"email"), safe(c,"telephone"), safe(c,"supervisor"), safe(c,"second_supervisor"), safe(c,"advisor"), safe(c,"status"), safe(c,"thesis_status"), safe(c,"research_background"), safe(c,"seminar"), safe(c,"proposal"), safe(c,"notes")});
            }
        } finally { c.close(); }
        try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) throw new Exception("Cannot write CSV.");
            out.write(sb.toString().getBytes("UTF-8"));
        }
        toast("Student CSV exported.");
    }

    private void appendCsvRow(StringBuilder sb, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            String v = values[i] == null ? "" : values[i];
            sb.append('"').append(v.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ")).append('"');
        }
        sb.append('\n');
    }

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
        e.setPadding(dp(10), dp(4), dp(10), dp(4));
        e.setBackground(rounded(panel2Color, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        e.setLayoutParams(lp);
        return e;
    }

    private void showReminders() {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Reminders", 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Add reminder"), v -> editReminder(0));
        content.addView(top);
        Cursor cur = db.getReadableDatabase().rawQuery("SELECT id,title,details,jdate,time_text FROM reminders ORDER BY remind_at_iso", null);
        try {
            if (!cur.moveToFirst()) {
                LinearLayout empty = card();
                empty.addView(tv("No reminders yet.", 14, mutedColor, Typeface.NORMAL));
                content.addView(empty);
            } else {
                do {
                    int id = cur.getInt(0);
                    LinearLayout c = card();
                    c.addView(tv(safe(cur, "title"), 17, textColor, Typeface.BOLD));
                    c.addView(tv(safe(cur, "jdate") + "  " + safe(cur, "time_text"), 14, primaryColor, Typeface.BOLD));
                    String det = safe(cur, "details");
                    if (!det.isEmpty()) c.addView(tv(det, 13, mutedColor, Typeface.NORMAL));
                    addButton(c, outlineBtn("Edit"), v -> editReminder(id));
                    content.addView(c);
                } while (cur.moveToNext());
            }
        } finally { cur.close(); }
        setScreen("Reminders", content);
    }

    private void editReminder(long id) {
        String title = "", details = "", jdate = nowJalali(), time = nowTime();
        boolean exists = id > 0;
        if (exists) {
            Cursor cur = db.getReadableDatabase().rawQuery("SELECT * FROM reminders WHERE id=?", new String[]{String.valueOf(id)});
            try {
                if (cur.moveToFirst()) {
                    title = safe(cur, "title"); details = safe(cur, "details"); jdate = safe(cur, "jdate"); time = safe(cur, "time_text");
                }
            } finally { cur.close(); }
        }
        LinearLayout content = vbox();
        LinearLayout f = card();
        f.addView(tv(exists ? "Edit reminder" : "Add reminder", 18, textColor, Typeface.BOLD));
        EditText titleE = input("Title", title, false);
        EditText detailsE = input("Details", details, true);
        EditText dateE = input("Jalali/display date, e.g. 1404/04/23", jdate, false);
        EditText timeE = input("Time, e.g. 14:30", time, false);
        f.addView(titleE); f.addView(detailsE); f.addView(dateE); f.addView(timeE);
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
            showReminders();
        });
        if (exists) addButton(f, dangerBtn("Delete reminder"), v -> { delete("reminders", id); toast("Reminder deleted."); showReminders(); });
        addButton(f, outlineBtn("Back"), v -> showReminders());
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

    private void showStudents() {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Students", 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Add student"), v -> editStudent(0));
        addButton(top, outlineBtn("Export list for Excel (CSV)"), v -> pickExportStudentsCsv());
        content.addView(top);
        Cursor cur = db.getReadableDatabase().rawQuery("SELECT id,level,first_name,family_name,student_no,status,thesis_status FROM students ORDER BY level,family_name,first_name", null);
        try {
            if (!cur.moveToFirst()) {
                LinearLayout empty = card(); empty.addView(tv("No students yet.", 14, mutedColor, Typeface.NORMAL)); content.addView(empty);
            } else {
                do {
                    int id = cur.getInt(0);
                    String name = (safe(cur,"first_name") + " " + safe(cur,"family_name")).trim();
                    if (name.isEmpty()) name = "Unnamed student";
                    LinearLayout c = card();
                    c.addView(tv(name, 17, textColor, Typeface.BOLD));
                    c.addView(tv(safe(cur,"level") + " • " + safe(cur,"student_no"), 13, primaryColor, Typeface.BOLD));
                    c.addView(tv("Status: " + safe(cur,"status") + " | Thesis: " + safe(cur,"thesis_status"), 13, mutedColor, Typeface.NORMAL));
                    addButton(c, outlineBtn("Open student page"), v -> editStudent(id));
                    content.addView(c);
                } while (cur.moveToNext());
            }
        } finally { cur.close(); }
        setScreen("Students", content);
    }

    private void editStudent(long id) {
        Map<String, String> data = new LinkedHashMap<>();
        String[] fields = {"level","first_name","family_name","student_no","national_id","registration_date","registration_semester","email","telephone","supervisor","second_supervisor","advisor","status","thesis_status","research_background","seminar","proposal","notes"};
        for (String f : fields) data.put(f, "");
        data.put("level", "MSc");
        boolean exists = id > 0;
        if (exists) {
            Cursor cur = db.getReadableDatabase().rawQuery("SELECT * FROM students WHERE id=?", new String[]{String.valueOf(id)});
            try { if (cur.moveToFirst()) for (String f : fields) data.put(f, safe(cur, f)); } finally { cur.close(); }
        }
        LinearLayout content = vbox();
        LinearLayout f = card();
        f.addView(tv(exists ? "Student page" : "Add student", 18, textColor, Typeface.BOLD));
        Map<String, EditText> e = new LinkedHashMap<>();
        addField(f, e, "level", "Level", data, false);
        addField(f, e, "first_name", "First name", data, false);
        addField(f, e, "family_name", "Family name", data, false);
        addField(f, e, "student_no", "Student number", data, false);
        addField(f, e, "national_id", "National ID", data, false);
        addField(f, e, "registration_date", "Registration date", data, false);
        addField(f, e, "registration_semester", "Registration semester", data, false);
        addField(f, e, "email", "Email", data, false);
        addField(f, e, "telephone", "Telephone", data, false);
        addField(f, e, "supervisor", "Supervisor", data, false);
        addField(f, e, "second_supervisor", "Second supervisor", data, false);
        addField(f, e, "advisor", "Advisor", data, false);
        addField(f, e, "status", "Status", data, false);
        addField(f, e, "thesis_status", "Thesis status", data, false);
        f.addView(tv("Research background", 15, textColor, Typeface.BOLD)); addField(f, e, "research_background", "Research background", data, true);
        f.addView(tv("Seminar", 15, textColor, Typeface.BOLD)); addField(f, e, "seminar", "Seminar", data, true);
        f.addView(tv("Proposal", 15, textColor, Typeface.BOLD)); addField(f, e, "proposal", "Proposal", data, true);
        addField(f, e, "notes", "Notes", data, true);
        addButton(f, btn("Save student"), v -> {
            ContentValues cv = new ContentValues();
            for (String key : fields) cv.put(key, e.get(key).getText().toString());
            if (exists) update("students", cv, id); else { cv.put("folder_path", ""); cv.put("created_at", nowIso()); insert("students", cv); }
            toast("Student saved.");
            showStudents();
        });
        if (exists) {
            addButton(f, outlineBtn("Important dates"), v -> showStudentDates(id));
            addButton(f, outlineBtn("Student tasks"), v -> showStudentTasks(id));
            addButton(f, dangerBtn("Delete student"), v -> { delete("students", id); toast("Student deleted."); showStudents(); });
        }
        addButton(f, outlineBtn("Back"), v -> showStudents());
        content.addView(f);
        setScreen(exists ? "Student page" : "Add student", content);
    }

    private void addField(LinearLayout parent, Map<String, EditText> map, String key, String label, Map<String, String> data, boolean multi) {
        EditText ed = input(label, data.get(key), multi);
        map.put(key, ed);
        parent.addView(ed);
    }

    private void showStudentDates(long studentId) {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Important dates", 18, textColor, Typeface.BOLD));
        EditText label = input("Date title", "", false);
        EditText value = input("Date value", "", false);
        top.addView(label); top.addView(value);
        addButton(top, btn("Add date"), v -> {
            if (label.getText().toString().trim().isEmpty() || value.getText().toString().trim().isEmpty()) { toast("Enter title and date."); return; }
            ContentValues cv = new ContentValues();
            cv.put("student_id", studentId); cv.put("date_label", label.getText().toString()); cv.put("date_value", value.getText().toString()); cv.put("created_at", nowIso());
            insert("student_dates", cv); showStudentDates(studentId);
        });
        addButton(top, outlineBtn("Back to student"), v -> editStudent(studentId));
        content.addView(top);
        Cursor cur = db.getReadableDatabase().rawQuery("SELECT id,date_label,date_value FROM student_dates WHERE student_id=? ORDER BY id", new String[]{String.valueOf(studentId)});
        try {
            while (cur.moveToNext()) {
                long id = cur.getLong(0);
                LinearLayout c = card();
                c.addView(tv(safe(cur,"date_label"), 16, textColor, Typeface.BOLD));
                c.addView(tv(safe(cur,"date_value"), 14, primaryColor, Typeface.BOLD));
                addButton(c, dangerBtn("Delete"), v -> { delete("student_dates", id); showStudentDates(studentId); });
                content.addView(c);
            }
        } finally { cur.close(); }
        setScreen("Important dates", content);
    }

    private void showStudentTasks(long studentId) {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Student tasks", 18, textColor, Typeface.BOLD));
        EditText title = input("Task title", "", false);
        EditText details = input("Details", "", true);
        top.addView(title); top.addView(details);
        addButton(top, btn("Add task"), v -> {
            if (title.getText().toString().trim().isEmpty()) { toast("Enter task title."); return; }
            ContentValues cv = new ContentValues();
            cv.put("student_id", studentId); cv.put("title", title.getText().toString()); cv.put("details", details.getText().toString()); cv.put("done", 0); cv.put("created_at", nowIso()); cv.put("completed_at", "");
            insert("student_tasks", cv); showStudentTasks(studentId);
        });
        addButton(top, outlineBtn("Back to student"), v -> editStudent(studentId));
        content.addView(top);
        Cursor cur = db.getReadableDatabase().rawQuery("SELECT id,title,details,done FROM student_tasks WHERE student_id=? ORDER BY done,id", new String[]{String.valueOf(studentId)});
        try {
            while (cur.moveToNext()) {
                long id = cur.getLong(0); int done = safeInt(cur,"done");
                LinearLayout c = card();
                c.addView(tv((done == 1 ? "✓ " : "") + safe(cur,"title"), 16, textColor, Typeface.BOLD));
                String det = safe(cur,"details"); if (!det.isEmpty()) c.addView(tv(det, 13, mutedColor, Typeface.NORMAL));
                addButton(c, outlineBtn(done == 1 ? "Mark not done" : "Mark done"), v -> {
                    ContentValues cv = new ContentValues(); cv.put("done", done == 1 ? 0 : 1); cv.put("completed_at", done == 1 ? "" : nowIso()); update("student_tasks", cv, id); showStudentTasks(studentId);
                });
                addButton(c, dangerBtn("Delete"), v -> { delete("student_tasks", id); showStudentTasks(studentId); });
                content.addView(c);
            }
        } finally { cur.close(); }
        setScreen("Student tasks", content);
    }

    private void showProjects() {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Projects", 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Add project"), v -> editProject(0));
        content.addView(top);
        Cursor cur = db.getReadableDatabase().rawQuery("SELECT id,name,status,progress,principal_investigator,contractor FROM projects ORDER BY name", null);
        try {
            if (!cur.moveToFirst()) { LinearLayout e = card(); e.addView(tv("No projects yet.", 14, mutedColor, Typeface.NORMAL)); content.addView(e); }
            else do {
                int id = cur.getInt(0);
                LinearLayout c = card();
                c.addView(tv(safe(cur,"name"), 17, textColor, Typeface.BOLD));
                c.addView(tv("Status: " + safe(cur,"status") + " • Progress: " + safeInt(cur,"progress") + "%", 13, primaryColor, Typeface.BOLD));
                c.addView(tv("PI: " + safe(cur,"principal_investigator") + " | Contractor: " + safe(cur,"contractor"), 13, mutedColor, Typeface.NORMAL));
                addButton(c, outlineBtn("Open project"), v -> editProject(id));
                content.addView(c);
            } while (cur.moveToNext());
        } finally { cur.close(); }
        setScreen("Projects", content);
    }

    private void editProject(long id) {
        Map<String,String> data = new LinkedHashMap<>();
        String[] fields = {"name","status","progress","start_jdate","end_jdate","principal_investigator","contractor","notes"};
        for (String f : fields) data.put(f, "");
        data.put("status", "Not started"); data.put("progress", "0");
        boolean exists = id > 0;
        if (exists) {
            Cursor cur = db.getReadableDatabase().rawQuery("SELECT * FROM projects WHERE id=?", new String[]{String.valueOf(id)});
            try { if (cur.moveToFirst()) for (String f : fields) data.put(f, safe(cur, f)); } finally { cur.close(); }
        }
        LinearLayout content = vbox();
        LinearLayout f = card();
        f.addView(tv(exists ? "Project page" : "Add project", 18, textColor, Typeface.BOLD));
        Map<String, EditText> e = new LinkedHashMap<>();
        addField(f, e, "name", "Project name", data, false);
        addField(f, e, "status", "Status", data, false);
        addField(f, e, "progress", "Progress %", data, false);
        addField(f, e, "start_jdate", "Start date", data, false);
        addField(f, e, "end_jdate", "End date", data, false);
        addField(f, e, "principal_investigator", "Principal investigator", data, false);
        addField(f, e, "contractor", "Contractor", data, false);
        addField(f, e, "notes", "Notes", data, true);
        addButton(f, btn("Save project"), v -> {
            String name = e.get("name").getText().toString().trim();
            if (name.isEmpty()) { toast("Enter project name."); return; }
            ContentValues cv = new ContentValues();
            for (String key : fields) {
                if (key.equals("progress")) {
                    try { cv.put(key, Integer.parseInt(e.get(key).getText().toString().trim())); } catch (Exception ex) { cv.put(key, 0); }
                } else cv.put(key, e.get(key).getText().toString());
            }
            if (exists) update("projects", cv, id); else { cv.put("created_at", nowIso()); insert("projects", cv); }
            toast("Project saved."); showProjects();
        });
        if (exists) {
            addButton(f, outlineBtn("Project tasks"), v -> showProjectTasks(id));
            addButton(f, dangerBtn("Delete project"), v -> { delete("projects", id); toast("Project deleted."); showProjects(); });
        }
        addButton(f, outlineBtn("Back"), v -> showProjects());
        content.addView(f);
        setScreen(exists ? "Project page" : "Add project", content);
    }

    private void showProjectTasks(long projectId) {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Project tasks", 18, textColor, Typeface.BOLD));
        EditText title = input("Task title", "", false);
        EditText due = input("Due date", "", false);
        EditText resp = input("Responsible", "", false);
        top.addView(title); top.addView(due); top.addView(resp);
        addButton(top, btn("Add task"), v -> {
            if (title.getText().toString().trim().isEmpty()) { toast("Enter task title."); return; }
            ContentValues cv = new ContentValues(); cv.put("title", title.getText().toString()); cv.put("project_id", projectId); cv.put("due_jdate", due.getText().toString()); cv.put("due_iso", ""); cv.put("responsible", resp.getText().toString()); cv.put("done", 0); cv.put("created_at", nowIso());
            insert("todos", cv); showProjectTasks(projectId);
        });
        addButton(top, outlineBtn("Back to project"), v -> editProject(projectId));
        content.addView(top);
        Cursor cur = db.getReadableDatabase().rawQuery("SELECT id,title,responsible,due_jdate,done FROM todos WHERE project_id=? ORDER BY done,id", new String[]{String.valueOf(projectId)});
        try {
            while (cur.moveToNext()) {
                long id = cur.getLong(0); int done = safeInt(cur,"done");
                LinearLayout c = card(); c.addView(tv((done == 1 ? "✓ " : "") + safe(cur,"title"), 16, textColor, Typeface.BOLD)); c.addView(tv(safe(cur,"responsible") + " • " + safe(cur,"due_jdate"), 13, mutedColor, Typeface.NORMAL));
                addButton(c, outlineBtn(done == 1 ? "Mark not done" : "Mark done"), v -> { ContentValues cv = new ContentValues(); cv.put("done", done == 1 ? 0 : 1); update("todos", cv, id); showProjectTasks(projectId); });
                addButton(c, dangerBtn("Delete"), v -> { delete("todos", id); showProjectTasks(projectId); });
                content.addView(c);
            }
        } finally { cur.close(); }
        setScreen("Project tasks", content);
    }

    private void showCourses() {
        LinearLayout content = vbox();
        LinearLayout top = card(); top.addView(tv("Courses", 18, textColor, Typeface.BOLD)); addButton(top, btn("+ Add course"), v -> editCourse(0)); content.addView(top);
        Cursor cur = db.getReadableDatabase().rawQuery("SELECT id,level,course_title,course_code,semester,instructor FROM courses ORDER BY semester,course_title", null);
        try {
            if (!cur.moveToFirst()) { LinearLayout e = card(); e.addView(tv("No courses yet.", 14, mutedColor, Typeface.NORMAL)); content.addView(e); }
            else do {
                int id = cur.getInt(0); LinearLayout c = card(); c.addView(tv(safe(cur,"course_title"), 17, textColor, Typeface.BOLD)); c.addView(tv(safe(cur,"level") + " • " + safe(cur,"course_code") + " • " + safe(cur,"semester"), 13, primaryColor, Typeface.BOLD)); c.addView(tv("Instructor: " + safe(cur,"instructor"), 13, mutedColor, Typeface.NORMAL)); addButton(c, outlineBtn("Open course"), v -> editCourse(id)); content.addView(c);
            } while (cur.moveToNext());
        } finally { cur.close(); }
        setScreen("Courses", content);
    }

    private void editCourse(long id) {
        Map<String,String> data = new LinkedHashMap<>(); String[] fields = {"level","course_title","course_code","semester","instructor","start_date","end_date","notes"}; for (String f : fields) data.put(f, ""); data.put("level", "MSc"); boolean exists = id > 0;
        if (exists) { Cursor cur = db.getReadableDatabase().rawQuery("SELECT * FROM courses WHERE id=?", new String[]{String.valueOf(id)}); try { if (cur.moveToFirst()) for (String f : fields) data.put(f, safe(cur, f)); } finally { cur.close(); } }
        LinearLayout content = vbox(); LinearLayout f = card(); f.addView(tv(exists ? "Course page" : "Add course", 18, textColor, Typeface.BOLD)); Map<String, EditText> e = new LinkedHashMap<>();
        addField(f, e, "level", "Level", data, false); addField(f, e, "course_title", "Course title", data, false); addField(f, e, "course_code", "Course code", data, false); addField(f, e, "semester", "Semester", data, false); addField(f, e, "instructor", "Instructor", data, false); addField(f, e, "start_date", "Start date", data, false); addField(f, e, "end_date", "End date", data, false); addField(f, e, "notes", "Notes", data, true);
        addButton(f, btn("Save course"), v -> { String title = e.get("course_title").getText().toString().trim(); if (title.isEmpty()) { toast("Enter course title."); return; } ContentValues cv = new ContentValues(); for (String key : fields) cv.put(key, e.get(key).getText().toString()); if (exists) update("courses", cv, id); else { cv.put("created_at", nowIso()); insert("courses", cv); } toast("Course saved."); showCourses(); });
        if (exists) addButton(f, dangerBtn("Delete course"), v -> { delete("courses", id); toast("Course deleted."); showCourses(); });
        addButton(f, outlineBtn("Back"), v -> showCourses()); content.addView(f); setScreen(exists ? "Course page" : "Add course", content);
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
        int[] sal_a = {0,31, (isLeap(gy) ? 29 : 28),31,30,31,30,31,31,30,31,30,31};
        int gm;
        for (gm = 1; gm <= 12 && gd > sal_a[gm]; gm++) gd -= sal_a[gm];
        return new int[]{gy, gm, gd};
    }

    private boolean isLeap(int y) { return (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0); }

    public static class DbHelper extends SQLiteOpenHelper {
        public DbHelper(Activity ctx) { super(ctx, DB_NAME, null, 66); }

        @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
        @Override public void onCreate(SQLiteDatabase db) { ensureSchema(db); }
        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { ensureSchema(db); }

        private void ensureSchema(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS projects (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, status TEXT DEFAULT 'Not started', progress INTEGER DEFAULT 0, start_jdate TEXT DEFAULT '', end_jdate TEXT DEFAULT '', principal_investigator TEXT DEFAULT '', contractor TEXT DEFAULT '', notes TEXT DEFAULT '', created_at TEXT NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS project_sections (project_id INTEGER NOT NULL, section_name TEXT NOT NULL, content TEXT DEFAULT '', PRIMARY KEY (project_id, section_name), FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS reminders (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, details TEXT DEFAULT '', project_id INTEGER, remind_at_iso TEXT NOT NULL, jdate TEXT NOT NULL, time_text TEXT NOT NULL, created_at TEXT NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE SET NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS reminder_alerts (id INTEGER PRIMARY KEY AUTOINCREMENT, reminder_id INTEGER NOT NULL, alert_at_iso TEXT NOT NULL, alert_type TEXT NOT NULL, fired INTEGER DEFAULT 0, UNIQUE(reminder_id, alert_type), FOREIGN KEY(reminder_id) REFERENCES reminders(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, done INTEGER DEFAULT 0, project_id INTEGER, due_iso TEXT DEFAULT '', due_jdate TEXT DEFAULT '', responsible TEXT DEFAULT '', created_at TEXT NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE SET NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS todo_alerts (id INTEGER PRIMARY KEY AUTOINCREMENT, todo_id INTEGER NOT NULL, alert_at_iso TEXT NOT NULL, alert_type TEXT NOT NULL, fired INTEGER DEFAULT 0, UNIQUE(todo_id, alert_type), FOREIGN KEY(todo_id) REFERENCES todos(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS staff_members (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, first_name TEXT DEFAULT '', family_name TEXT DEFAULT '', role TEXT DEFAULT '', position TEXT DEFAULT '', email TEXT DEFAULT '', telephone TEXT DEFAULT '', created_at TEXT NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS wbs_items (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, code TEXT DEFAULT '', title TEXT DEFAULT '', description TEXT DEFAULT '', responsible TEXT DEFAULT '', deliverable TEXT DEFAULT '', weight_percent REAL DEFAULT 0, completed INTEGER DEFAULT 0, start_month INTEGER DEFAULT 1, end_month INTEGER DEFAULT 1, start_jdate TEXT DEFAULT '', end_jdate TEXT DEFAULT '', progress INTEGER DEFAULT 0, created_at TEXT NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS cbs_items (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, code TEXT DEFAULT '', cost_item TEXT DEFAULT '', category TEXT DEFAULT '', unit TEXT DEFAULT '', quantity REAL DEFAULT 0, unit_cost REAL DEFAULT 0, total_cost REAL DEFAULT 0, created_at TEXT NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS app_settings (key TEXT PRIMARY KEY, value TEXT DEFAULT '')");
            db.execSQL("CREATE TABLE IF NOT EXISTS project_files (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, section_name TEXT NOT NULL, display_name TEXT NOT NULL, subject TEXT DEFAULT '', document_type TEXT DEFAULT '', stored_path TEXT NOT NULL, original_path TEXT DEFAULT '', uploaded_at TEXT NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS students (id INTEGER PRIMARY KEY AUTOINCREMENT, level TEXT NOT NULL, first_name TEXT DEFAULT '', family_name TEXT DEFAULT '', student_no TEXT DEFAULT '', national_id TEXT DEFAULT '', registration_date TEXT DEFAULT '', registration_semester TEXT DEFAULT '', email TEXT DEFAULT '', telephone TEXT DEFAULT '', supervisor TEXT DEFAULT '', second_supervisor TEXT DEFAULT '', advisor TEXT DEFAULT '', status TEXT DEFAULT '', thesis_status TEXT DEFAULT '', research_background TEXT DEFAULT '', seminar TEXT DEFAULT '', proposal TEXT DEFAULT '', notes TEXT DEFAULT '', folder_path TEXT DEFAULT '', created_at TEXT NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS student_files (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, document_type TEXT DEFAULT '', display_name TEXT NOT NULL, stored_path TEXT NOT NULL, original_path TEXT DEFAULT '', uploaded_at TEXT NOT NULL, FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS student_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, title TEXT NOT NULL, details TEXT DEFAULT '', done INTEGER DEFAULT 0, created_at TEXT NOT NULL, completed_at TEXT DEFAULT '', FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS student_dates (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, date_label TEXT NOT NULL, date_value TEXT NOT NULL, created_at TEXT NOT NULL, FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS scurve_points (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, month_no INTEGER NOT NULL, month_label TEXT NOT NULL, plan_progress REAL, actual_progress REAL, UNIQUE(project_id, month_no), FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS gantt_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, task_name TEXT NOT NULL, start_jdate TEXT NOT NULL, end_jdate TEXT NOT NULL, start_iso TEXT NOT NULL, end_iso TEXT NOT NULL, progress INTEGER DEFAULT 0, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS custom_sheets (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, color TEXT DEFAULT '#2563EB', created_at TEXT NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS custom_sheet_staff (id INTEGER PRIMARY KEY AUTOINCREMENT, sheet_id INTEGER NOT NULL, first_name TEXT DEFAULT '', family_name TEXT DEFAULT '', role TEXT DEFAULT '', position TEXT DEFAULT '', email TEXT DEFAULT '', telephone TEXT DEFAULT '', notes TEXT DEFAULT '', created_at TEXT NOT NULL, FOREIGN KEY(sheet_id) REFERENCES custom_sheets(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS custom_sheet_files (id INTEGER PRIMARY KEY AUTOINCREMENT, sheet_id INTEGER NOT NULL, document_type TEXT DEFAULT '', display_name TEXT NOT NULL, stored_path TEXT NOT NULL, original_path TEXT DEFAULT '', uploaded_at TEXT NOT NULL, FOREIGN KEY(sheet_id) REFERENCES custom_sheets(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS custom_sheet_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, sheet_id INTEGER NOT NULL, title TEXT NOT NULL, responsible TEXT DEFAULT '', due_jdate TEXT DEFAULT '', details TEXT DEFAULT '', done INTEGER DEFAULT 0, created_at TEXT NOT NULL, completed_at TEXT DEFAULT '', FOREIGN KEY(sheet_id) REFERENCES custom_sheets(id) ON DELETE CASCADE)");
            db.execSQL("CREATE TABLE IF NOT EXISTS courses (id INTEGER PRIMARY KEY AUTOINCREMENT, level TEXT NOT NULL, course_title TEXT NOT NULL, course_code TEXT DEFAULT '', semester TEXT DEFAULT '', instructor TEXT DEFAULT '', start_date TEXT DEFAULT '', end_date TEXT DEFAULT '', notes TEXT DEFAULT '', created_at TEXT NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS course_files (id INTEGER PRIMARY KEY AUTOINCREMENT, course_id INTEGER NOT NULL, document_type TEXT DEFAULT '', display_name TEXT NOT NULL, stored_path TEXT NOT NULL, original_path TEXT DEFAULT '', uploaded_at TEXT NOT NULL, FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE)");
            addColumn(db, "students", "research_background", "TEXT DEFAULT ''");
            addColumn(db, "students", "seminar", "TEXT DEFAULT ''");
            addColumn(db, "students", "proposal", "TEXT DEFAULT ''");
        }

        private void addColumn(SQLiteDatabase db, String table, String column, String type) {
            Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            boolean found = false;
            try {
                while (c.moveToNext()) if (column.equals(c.getString(1))) { found = true; break; }
            } finally { c.close(); }
            if (!found) db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }
    }
}
