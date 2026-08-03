package com.personalassistant.mobile;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {
    private static final String APP_TITLE = "Personal Assistant";
    private static final String APP_VERSION = "3.7.7 Android";
    private static final int REQ_NOTIFICATION = 90;
    private static final int REQ_IMPORT_DB = 1001;
    private static final int REQ_EXPORT_DB = 1002;
    private static final int REQ_EXPORT_STUDENTS = 1003;
    private static final int REQ_EXPORT_REMINDERS = 1004;
    private static final int REQ_EXPORT_COURSES = 1005;
    private static final int REQ_EXPORT_PROJECTS = 1006;
    private static final int REQ_EXPORT_TASKS = 1007;
    private static final int REQ_UPLOAD_FILE = 1100;
    private static final int REQ_SAVE_FILE_AS = 1101;
    private static final int REQ_EXPORT_FOLDER_ZIP = 1102;

    private DbHelper db;
    private boolean darkMode;
    private int bgColor;
    private int panelColor;
    private int panel2Color;
    private int textColor;
    private int mutedColor;
    private int primaryColor;
    private int accentColor;
    private int dangerColor;
    private int lineColor;

    private String pendingUploadKind = "";
    private long pendingParentId = 0;
    private String pendingSection = "";
    private String pendingDocumentType = "";
    private String pendingTitle = "";
    private String pendingSourcePath = "";
    private byte[] pendingExportBytes;
    private String pendingExportMessage = "";
    private String pendingInventoryFolder = "";
    private String pendingExportReturn = "settings";

    private final Deque<ScreenSnapshot> screenHistory = new ArrayDeque<>();
    private View currentRootView;
    private String currentScreenTitle = "";
    private String currentRouteKey = "";
    private Runnable currentScreenRenderer;
    private boolean skipHistoryOnce = false;
    private TextView currentClockView;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTicker = new Runnable() {
        @Override
        public void run() {
            if (currentClockView != null) {
                currentClockView.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            }
            long delay = 60000L - (System.currentTimeMillis() % 60000L);
            clockHandler.postDelayed(this, Math.max(1000L, delay));
        }
    };

    private static final class ScreenSnapshot {
        final Runnable renderer;
        final String title;
        final String routeKey;
        ScreenSnapshot(Runnable renderer, String title, String routeKey) {
            this.renderer = renderer;
            this.title = title;
            this.routeKey = routeKey;
        }
    }

    private interface TextCallback { void accept(String value); }
    private interface ConfirmCallback { void run(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyFullscreenMode();
        clockHandler.removeCallbacks(clockTicker);
        clockHandler.post(clockTicker);
        SharedPreferences prefs = getSharedPreferences("pa_settings", MODE_PRIVATE);
        darkMode = prefs.getBoolean("dark_mode", false);
        applyColors();
        db = new DbHelper(this);
        db.getWritableDatabase();
        NotificationReceiver.ensureChannel(this);
        requestNotificationPermissionIfNeeded();
        ReminderScheduler.scheduleAll(this);
        if (getIntent() != null && getIntent().getBooleanExtra("open_reminders", false)) showReminders();
        else showDashboard();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra("open_reminders", false)) showReminders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyFullscreenMode();
        if (db != null) ReminderScheduler.scheduleAll(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyFullscreenMode();
    }

    @Override
    protected void onDestroy() {
        clockHandler.removeCallbacks(clockTicker);
        if (db != null) db.close();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        goBackOneStep();
    }

    private void applyFullscreenMode() {
        Window window = getWindow();
        if (window == null) return;
        window.setStatusBarColor(0x00000000);
        window.setNavigationBarColor(0x00000000);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
    }

    private void goBackOneStep() {
        if (!screenHistory.isEmpty()) {
            ScreenSnapshot snapshot = screenHistory.pop();
            skipHistoryOnce = true;
            snapshot.renderer.run();
            return;
        }
        if (!"Dashboard".equals(currentScreenTitle)) {
            skipHistoryOnce = true;
            showDashboard();
        } else {
            toast("You are already on the dashboard.");
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                NotificationReceiver.ensureChannel(this);
                ReminderScheduler.scheduleAll(this);
                toast("Reminder notifications are enabled.");
            } else {
                toast("Notification permission was not granted. You can enable it later in Android settings.");
            }
        }
    }

    private void sendTestNotification() {
        requestNotificationPermissionIfNeeded();
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("reminder_id", 0L);
        intent.putExtra("title", "Personal Assistant test");
        intent.putExtra("details", "Reminder notifications are working on this phone.");
        intent.putExtra("jdate", Jalali.today());
        intent.putExtra("time_text", Jalali.nowTime());
        intent.putExtra("alert_type", "Test notification");
        intent.putExtra("notification_id", (int) (System.currentTimeMillis() % Integer.MAX_VALUE));
        sendBroadcast(intent);
    }

    private boolean canUseExactAlarms() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        return manager != null && manager.canScheduleExactAlarms();
    }

    private void requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            toast("Precise alarms are already supported on this Android version.");
            return;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception ex) {
            toast("Open Android settings and allow alarms and reminders for this app.");
        }
    }

    private void applyColors() {
        if (darkMode) {
            bgColor = 0xFF08111F;
            panelColor = 0xFF111C2E;
            panel2Color = 0xFF19263A;
            textColor = 0xFFF8FAFC;
            mutedColor = 0xFFAFC0D4;
            primaryColor = 0xFF4F8CFF;
            accentColor = 0xFF21C7A8;
            dangerColor = 0xFFF05D6F;
            lineColor = 0xFF2D405B;
        } else {
            bgColor = 0xFFF2F6FC;
            panelColor = 0xFFFFFFFF;
            panel2Color = 0xFFF7FAFF;
            textColor = 0xFF122033;
            mutedColor = 0xFF66758A;
            primaryColor = 0xFF315FEA;
            accentColor = 0xFF0FA88E;
            dangerColor = 0xFFDA3F52;
            lineColor = 0xFFDCE5F2;
        }
        Window window = getWindow();
        if (window != null) window.getDecorView().setBackgroundColor(bgColor);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), lineColor);
        return drawable;
    }

    private GradientDrawable headerGradient() {
        int end = darkMode ? 0xFF183B70 : 0xFF1648C9;
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{primaryColor, end});
        drawable.setCornerRadius(0);
        return drawable;
    }

    private int dialogTheme() {
        return darkMode ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert;
    }

    private AlertDialog.Builder alertBuilder() {
        return new AlertDialog.Builder(this, dialogTheme());
    }

    private LinearLayout vbox() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout hbox() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private TextView tv(String text, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(text == null ? "" : text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        view.setPadding(dp(2), dp(2), dp(2), dp(2));
        return view;
    }

    private Button btn(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(0xFFFFFFFF);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(rounded(primaryColor, 14));
        button.setMinHeight(dp(48));
        button.setPadding(dp(14), dp(7), dp(14), dp(7));
        if (Build.VERSION.SDK_INT >= 21) button.setElevation(dp(2));
        return button;
    }

    private Button outlineBtn(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(rounded(panel2Color, 14));
        button.setMinHeight(dp(48));
        button.setPadding(dp(14), dp(7), dp(14), dp(7));
        if (Build.VERSION.SDK_INT >= 21) button.setElevation(dp(1));
        return button;
    }

    private Button dangerBtn(String text) {
        Button button = btn(text);
        button.setBackground(rounded(dangerColor, 12));
        return button;
    }

    private LinearLayout card() {
        LinearLayout card = vbox();
        card.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.setBackground(rounded(panelColor, 20));
        if (Build.VERSION.SDK_INT >= 21) card.setElevation(dp(4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(12), dp(7), dp(12), dp(7));
        card.setLayoutParams(lp);
        return card;
    }

    private void addButton(LinearLayout parent, Button button, View.OnClickListener listener) {
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        parent.addView(button, lp);
    }

    private TextView headerIcon(String text, int textSizeSp, String description) {
        TextView icon = tv(text, textSizeSp, 0xFFFFFFFF, Typeface.BOLD);
        icon.setContentDescription(description);
        icon.setGravity(Gravity.CENTER);
        icon.setIncludeFontPadding(false);
        icon.setTextDirection(View.TEXT_DIRECTION_LTR);
        icon.setBackground(rounded(0x22FFFFFF, 14));
        icon.setClickable(true);
        icon.setFocusable(true);
        if (Build.VERSION.SDK_INT >= 21) icon.setElevation(dp(1));
        return icon;
    }

    private String routeFamily(String routeKey) {
        if (routeKey == null) return "";
        int split = routeKey.indexOf(':');
        return split < 0 ? routeKey : routeKey.substring(0, split);
    }

    private void setScreen(String title, LinearLayout content, Runnable renderer, String routeKey) {
        String resolvedKey = routeKey == null || routeKey.isEmpty() ? title : routeKey;
        boolean sameScreen = resolvedKey.equals(currentRouteKey)
                || (!currentRouteKey.isEmpty() && routeFamily(resolvedKey).equals(routeFamily(currentRouteKey)));
        boolean canGoBack = currentScreenRenderer != null || !screenHistory.isEmpty() || !"Dashboard".equals(title);

        LinearLayout root = vbox();
        root.setBackgroundColor(bgColor);

        LinearLayout header = vbox();
        header.setPadding(dp(10), dp(12), dp(12), dp(10));
        header.setBackground(headerGradient());
        if (Build.VERSION.SDK_INT >= 21) header.setElevation(dp(7));

        LinearLayout clockRow = hbox();
        clockRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        TextView clock = tv(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()), 15, 0xFFFFFFFF, Typeface.BOLD);
        clock.setTag("pa_clock");
        clock.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        clock.setIncludeFontPadding(false);
        currentClockView = clock;
        clockRow.addView(clock, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(28)));
        header.addView(clockRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));

        LinearLayout navigationRow = hbox();
        navigationRow.setGravity(Gravity.CENTER_VERTICAL);
        if (canGoBack && !"Dashboard".equals(title)) {
            TextView back = headerIcon("←", 25, "Back");
            back.setOnClickListener(v -> goBackOneStep());
            LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(dp(52), dp(52));
            backLp.setMargins(0, 0, dp(4), 0);
            navigationRow.addView(back, backLp);
        }
        TextView menu = headerIcon("☰", 22, "Open menu");
        menu.setOnClickListener(v -> showSideMenu());
        LinearLayout.LayoutParams menuLp = new LinearLayout.LayoutParams(dp(54), dp(52));
        menuLp.setMargins(dp(4), 0, dp(10), 0);
        navigationRow.addView(menu, menuLp);

        LinearLayout titleBox = vbox();
        titleBox.setPadding(dp(2), 0, 0, 0);
        titleBox.addView(tv(title, 21, 0xFFFFFFFF, Typeface.BOLD));
        titleBox.addView(tv(APP_TITLE + "  •  " + APP_VERSION, 11, 0xFFEAF2FF, Typeface.NORMAL));
        navigationRow.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        if ("Dashboard".equals(title)) {
            TextView settings = headerIcon("⚙", 22, "Settings");
            settings.setOnClickListener(v -> showSettings());
            LinearLayout.LayoutParams settingsLp = new LinearLayout.LayoutParams(dp(52), dp(52));
            settingsLp.setMargins(dp(8), 0, 0, 0);
            navigationRow.addView(settings, settingsLp);
        }

        header.addView(navigationRow);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bgColor);
        content.setPadding(0, dp(8), 0, dp(34));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        if (currentScreenRenderer != null && !sameScreen && !skipHistoryOnce) {
            screenHistory.push(new ScreenSnapshot(currentScreenRenderer, currentScreenTitle, currentRouteKey));
        }
        skipHistoryOnce = false;
        currentRootView = root;
        currentScreenTitle = title;
        currentRouteKey = resolvedKey;
        currentScreenRenderer = renderer;
        setContentView(root);
        applyFullscreenMode();
    }

    private void showSideMenu() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout menu = vbox();
        menu.setPadding(dp(14), dp(24), dp(14), dp(20));
        menu.setBackgroundColor(panelColor);
        menu.addView(tv(APP_TITLE, 22, textColor, Typeface.BOLD));
        menu.addView(tv(APP_VERSION, 12, mutedColor, Typeface.NORMAL));
        String[] names = {"Dashboard", "Reminders", "All tasks", "Students", "Projects", "Courses", "File Inventory", "Settings"};
        Runnable[] actions = {
                this::showDashboard,
                this::showReminders,
                this::showAllTasks,
                this::showStudents,
                this::showProjects,
                this::showCourses,
                this::showFileInventory,
                this::showSettings
        };
        for (int i = 0; i < names.length; i++) {
            final Runnable action = actions[i];
            Button item = outlineBtn(names[i]);
            item.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            addButton(menu, item, v -> {
                dialog.dismiss();
                action.run();
            });
        }
        dialog.setContentView(menu);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(window.getAttributes());
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.82f);
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.START;
            window.setAttributes(params);
        }
        dialog.show();
        if (window != null) {
            window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.82f), WindowManager.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.START);
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String nowIso() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
    }

    private String safe(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) return "";
        return cursor.getString(index);
    }

    private int safeInt(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) return 0;
        return cursor.getInt(index);
    }

    private long insert(String table, ContentValues values) {
        return db.getWritableDatabase().insert(table, null, values);
    }

    private void update(String table, ContentValues values, long id) {
        db.getWritableDatabase().update(table, values, "id=?", new String[]{String.valueOf(id)});
    }

    private void delete(String table, long id) {
        db.getWritableDatabase().delete(table, "id=?", new String[]{String.valueOf(id)});
    }

    private int count(String table) {
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + table, null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    private EditText input(String label, String value, boolean multiLine) {
        EditText edit = new EditText(this);
        edit.setHint(label);
        edit.setText(value == null ? "" : value);
        edit.setTextColor(textColor);
        edit.setHintTextColor(mutedColor);
        edit.setTextSize(15);
        edit.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        edit.setGravity(Gravity.START | (multiLine ? Gravity.TOP : Gravity.CENTER_VERTICAL));
        edit.setSingleLine(!multiLine);
        edit.setMinHeight(dp(multiLine ? 82 : 52));
        edit.setSelectAllOnFocus(false);
        if (multiLine) {
            edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            edit.setMinLines(3);
            edit.setMaxLines(10);
        } else {
            edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        edit.setPadding(dp(10), dp(4), dp(10), dp(4));
        edit.setBackground(rounded(panel2Color, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        edit.setLayoutParams(lp);
        return edit;
    }

    private Spinner spinner(String[] values, String selected) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, values) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView text = (TextView) view;
                    text.setTextColor(textColor);
                    text.setBackgroundColor(panel2Color);
                    text.setPadding(dp(10), dp(8), dp(10), dp(8));
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView text = (TextView) view;
                    text.setTextColor(textColor);
                    text.setBackgroundColor(panelColor);
                    text.setPadding(dp(14), dp(12), dp(14), dp(12));
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int index = Arrays.asList(values).indexOf(selected);
        spinner.setSelection(index < 0 ? 0 : index);
        spinner.setBackground(rounded(panel2Color, 10));
        spinner.setPadding(dp(8), dp(4), dp(8), dp(4));
        return spinner;
    }

    private EditText dateField(LinearLayout parent, String label, String value, boolean yearOnly) {
        parent.addView(tv(label, 14, textColor, Typeface.BOLD));
        LinearLayout row = hbox();
        EditText edit = input(label, value, false);
        edit.setFocusable(false);
        edit.setClickable(true);
        Button pick = outlineBtn(yearOnly ? "Select year" : "Select date");
        pick.setOnClickListener(v -> showJalaliPicker(edit, yearOnly));
        edit.setOnClickListener(v -> showJalaliPicker(edit, yearOnly));
        row.addView(edit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(pick, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        parent.addView(row);
        return edit;
    }

    private EditText timeField(LinearLayout parent, String label, String value) {
        parent.addView(tv(label, 14, textColor, Typeface.BOLD));
        LinearLayout row = hbox();
        EditText edit = input(label, value, false);
        edit.setFocusable(false);
        edit.setClickable(true);
        Button pick = outlineBtn("Select time");
        View.OnClickListener listener = v -> {
            int hour = 9;
            int minute = 0;
            try {
                String[] parts = edit.getText().toString().split(":");
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}
            new TimePickerDialog(this, dialogTheme(),
                    (view, h, m) -> edit.setText(String.format(Locale.US, "%02d:%02d", h, m)),
                    hour, minute, true).show();
        };
        edit.setOnClickListener(listener);
        pick.setOnClickListener(listener);
        row.addView(edit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(pick, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        parent.addView(row);
        return edit;
    }

    private void showJalaliPicker(EditText target, boolean yearOnly) {
        String raw = Jalali.normalizeDigits(target.getText().toString());
        int year = Jalali.currentYear();
        int month = 1;
        int day = 1;
        try {
            String[] parts = raw.split("/");
            year = Integer.parseInt(parts[0]);
            if (parts.length > 1) month = Integer.parseInt(parts[1]);
            if (parts.length > 2) day = Integer.parseInt(parts[2]);
        } catch (Exception ignored) {}

        LinearLayout box = vbox();
        box.setPadding(dp(18), dp(8), dp(18), dp(8));
        final Spinner yearSpinner = spinner(rangeStrings(Math.max(1300, year - 50), year + 30), String.valueOf(year));
        box.addView(tv("Year", 14, textColor, Typeface.BOLD));
        box.addView(yearSpinner);
        final Spinner monthSpinner;
        final Spinner daySpinner;
        if (!yearOnly) {
            monthSpinner = spinner(rangeStrings(1, 12), String.valueOf(month));
            daySpinner = spinner(rangeStrings(1, 31), String.valueOf(day));
            box.addView(tv("Month", 14, textColor, Typeface.BOLD));
            box.addView(monthSpinner);
            box.addView(tv("Day", 14, textColor, Typeface.BOLD));
            box.addView(daySpinner);
        } else {
            monthSpinner = null;
            daySpinner = null;
        }
        alertBuilder()
                .setTitle(yearOnly ? "Select year" : "Select date")
                .setView(box)
                .setPositiveButton("Select", (dialog, which) -> {
                    int y = Integer.parseInt(yearSpinner.getSelectedItem().toString());
                    if (yearOnly) {
                        target.setText(String.valueOf(y));
                    } else {
                        int m = Integer.parseInt(monthSpinner.getSelectedItem().toString());
                        int d = Integer.parseInt(daySpinner.getSelectedItem().toString());
                        d = Math.min(d, Jalali.daysInMonth(y, m));
                        target.setText(String.format(Locale.US, "%04d/%02d/%02d", y, m, d));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String[] rangeStrings(int start, int end) {
        String[] values = new String[end - start + 1];
        for (int i = start; i <= end; i++) values[i - start] = String.valueOf(i);
        return values;
    }

    private void promptText(String title, String label, String initial, TextCallback callback) {
        EditText edit = input(label, initial, false);
        alertBuilder()
                .setTitle(title)
                .setView(edit)
                .setPositiveButton("Save", (dialog, which) -> callback.accept(edit.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirm(String title, String message, ConfirmCallback callback) {
        alertBuilder()
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> callback.run())
                .setNegativeButton("No", null)
                .show();
    }

    // ---------------- Dashboard ----------------

    private void showDashboard() {
        LinearLayout content = vbox();
        content.addView(buildWeekCalendar());

        LinearLayout actions = card();
        actions.addView(tv("Quick actions", 18, textColor, Typeface.BOLD));
        addButton(actions, btn("Add reminder"), v -> editReminder(0));
        addButton(actions, btn("Add task"), v -> chooseTaskTarget());
        addButton(actions, btn("Add student"), v -> chooseStudentLevelForAdd());
        addButton(actions, btn("Add project"), v -> editProject(0));
        content.addView(actions);

        LinearLayout upcoming = card();
        upcoming.addView(tv("Upcoming reminders", 18, textColor, Typeface.BOLD));
        Cursor cursor = db.getReadableDatabase().rawQuery(
                "SELECT id,title,jdate,time_text,details FROM reminders WHERE remind_at_iso>=? ORDER BY remind_at_iso LIMIT 8",
                new String[]{new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(new Date())}
        );
        try {
            if (!cursor.moveToFirst()) {
                upcoming.addView(tv("No upcoming reminders.", 13, mutedColor, Typeface.NORMAL));
            } else {
                do {
                    final long id = cursor.getLong(0);
                    LinearLayout item = vbox();
                    item.setPadding(dp(10), dp(8), dp(10), dp(8));
                    item.setBackground(rounded(panel2Color, 10));
                    item.addView(tv(safe(cursor, "title"), 16, textColor, Typeface.BOLD));
                    item.addView(tv(safe(cursor, "jdate") + "  " + safe(cursor, "time_text"), 13, primaryColor, Typeface.BOLD));
                    String details = safe(cursor, "details");
                    if (!details.isEmpty()) item.addView(tv(details, 13, mutedColor, Typeface.NORMAL));
                    item.setOnClickListener(v -> editReminder(id));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, dp(4), 0, dp(4));
                    upcoming.addView(item, lp);
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        content.addView(upcoming);

        LinearLayout tasks = card();
        tasks.addView(tv("Recent open tasks", 18, textColor, Typeface.BOLD));
        int shown = addDashboardTasks(tasks);
        if (shown == 0) tasks.addView(tv("No open tasks.", 13, mutedColor, Typeface.NORMAL));
        addButton(tasks, outlineBtn("Open all tasks"), v -> showAllTasks());
        content.addView(tasks);
        setScreen("Dashboard", content, this::showDashboard, "dashboard");
    }

    private LinearLayout buildWeekCalendar() {
        LinearLayout card = card();
        card.addView(tv("Calendar • this week", 18, textColor, Typeface.BOLD));
        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        LinearLayout week = hbox();
        week.setPadding(0, dp(8), 0, dp(4));
        Calendar start = Calendar.getInstance();
        int offset = (start.get(Calendar.DAY_OF_WEEK) - Calendar.SATURDAY + 7) % 7;
        start.add(Calendar.DAY_OF_MONTH, -offset);
        String[] names = {"شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"};
        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) start.clone();
            day.add(Calendar.DAY_OF_MONTH, i);
            int[] j = Jalali.gregorianToJalali(day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH));
            String date = String.format(Locale.US, "%04d/%02d/%02d", j[0], j[1], j[2]);
            int reminderCount = countRemindersForDate(date);
            LinearLayout dayCard = vbox();
            dayCard.setGravity(Gravity.CENTER);
            dayCard.setPadding(dp(10), dp(10), dp(10), dp(10));
            dayCard.setBackground(rounded(date.equals(Jalali.today()) ? primaryColor : panel2Color, 12));
            dayCard.addView(tv(names[i], 13, date.equals(Jalali.today()) ? 0xFFFFFFFF : textColor, Typeface.BOLD));
            dayCard.addView(tv(String.format(Locale.US, "%02d/%02d", j[1], j[2]), 15, date.equals(Jalali.today()) ? 0xFFFFFFFF : primaryColor, Typeface.BOLD));
            if (reminderCount > 0) dayCard.addView(tv(reminderCount + " reminder" + (reminderCount == 1 ? "" : "s"), 10, date.equals(Jalali.today()) ? 0xFFFFFFFF : accentColor, Typeface.BOLD));
            final String selectedDate = date;
            dayCard.setOnClickListener(v -> showRemindersForDate(selectedDate));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(100), ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(3), 0, dp(3), 0);
            week.addView(dayCard, lp);
        }
        horizontal.addView(week);
        card.addView(horizontal);
        return card;
    }

    private int countRemindersForDate(String jdate) {
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM reminders WHERE jdate=?", new String[]{jdate});
        try { return cursor.moveToFirst() ? cursor.getInt(0) : 0; }
        finally { cursor.close(); }
    }

    private int addDashboardTasks(LinearLayout parent) {
        int shown = 0;
        Cursor projectTasks = db.getReadableDatabase().rawQuery(
                "SELECT t.id,t.title,t.due_jdate,p.name FROM todos t LEFT JOIN projects p ON p.id=t.project_id WHERE t.done=0 ORDER BY t.created_at DESC LIMIT 4",
                null
        );
        try {
            while (projectTasks.moveToNext()) {
                parent.addView(tv("• " + projectTasks.getString(1) + " — " + (projectTasks.getString(3) == null ? "General" : projectTasks.getString(3)) + "  " + projectTasks.getString(2), 13, textColor, Typeface.NORMAL));
                shown++;
            }
        } finally { projectTasks.close(); }
        if (shown < 8) {
            Cursor studentTasks = db.getReadableDatabase().rawQuery(
                    "SELECT st.title,st.due_jdate,s.first_name,s.family_name FROM student_tasks st JOIN students s ON s.id=st.student_id WHERE st.done=0 ORDER BY st.created_at DESC LIMIT ?",
                    new String[]{String.valueOf(8 - shown)}
            );
            try {
                while (studentTasks.moveToNext()) {
                    parent.addView(tv("• " + studentTasks.getString(0) + " — " + studentTasks.getString(2) + " " + studentTasks.getString(3) + "  " + studentTasks.getString(1), 13, textColor, Typeface.NORMAL));
                    shown++;
                }
            } finally { studentTasks.close(); }
        }
        return shown;
    }

    // ---------------- Settings / database ----------------

    private void showSettings() {
        LinearLayout content = vbox();
        LinearLayout appearance = card();
        appearance.addView(tv("Appearance", 18, textColor, Typeface.BOLD));
        LinearLayout modeRow = hbox();
        modeRow.addView(tv("☀", 24, textColor, Typeface.NORMAL));
        final Switch modeSwitch = new Switch(this);
        modeSwitch.setShowText(true);
        modeSwitch.setTextOff("☀");
        modeSwitch.setTextOn("☾");
        modeSwitch.setChecked(darkMode);
        modeSwitch.setTextColor(textColor);
        modeSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked == darkMode) return;
            darkMode = checked;
            getSharedPreferences("pa_settings", MODE_PRIVATE).edit().putBoolean("dark_mode", darkMode).apply();
            applyColors();
            showSettings();
        });
        modeRow.addView(modeSwitch, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        modeRow.addView(tv("☾", 24, textColor, Typeface.NORMAL));
        appearance.addView(modeRow);
        content.addView(appearance);

        LinearLayout notifications = card();
        notifications.addView(tv("Reminder notifications", 18, textColor, Typeface.BOLD));
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        boolean enabled = manager != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || manager.areNotificationsEnabled());
        notifications.addView(tv(enabled ? "Notifications are enabled." : "Notifications are disabled. Enable them so reminders appear on the phone.", 13, enabled ? accentColor : dangerColor, Typeface.BOLD));
        boolean exactEnabled = canUseExactAlarms();
        notifications.addView(tv(exactEnabled ? "Precise reminder timing is enabled." : "Precise alarm access is not enabled; Android may delay reminders slightly.", 13, exactEnabled ? accentColor : dangerColor, Typeface.BOLD));
        addButton(notifications, btn("Request notification permission"), v -> requestNotificationPermissionIfNeeded());
        addButton(notifications, outlineBtn("Send test notification now"), v -> sendTestNotification());
        if (!exactEnabled) addButton(notifications, outlineBtn("Allow precise reminder timing"), v -> requestExactAlarmAccess());
        addButton(notifications, outlineBtn("Reschedule all reminder notifications"), v -> {
            ReminderScheduler.scheduleAll(this);
            toast("Future reminder notifications were rescheduled.");
        });
        content.addView(notifications);

        LinearLayout database = card();
        database.addView(tv("Database / sync", 18, textColor, Typeface.BOLD));
        database.addView(tv("Android uses a local working copy of assistant_data.db. Import it from cloud storage before work, then export it back after work.", 13, mutedColor, Typeface.NORMAL));
        database.addView(tv("Local database: " + getDatabasePath(DbHelper.DB_NAME).getAbsolutePath(), 11, mutedColor, Typeface.NORMAL));
        database.addView(tv("Records: students " + count("students") + ", projects " + count("projects") + ", courses " + count("courses") + ", reminders " + count("reminders"), 13, textColor, Typeface.NORMAL));
        addButton(database, btn("Import database from cloud / file"), v -> pickImportDatabase());
        addButton(database, btn("Export / Sync database now"), v -> pickExportDatabase());
        addButton(database, outlineBtn("Export student list (CSV)"), v -> { pendingExportReturn = "settings"; pickExportStudentsCsv(); });
        addButton(database, outlineBtn("Export courses (CSV)"), v -> pickExportCoursesCsv());
        addButton(database, outlineBtn("Export projects (CSV)"), v -> pickExportProjectsCsv());
        addButton(database, outlineBtn("Export all tasks (CSV)"), v -> pickExportTasksCsv());
        content.addView(database);
        setScreen("Settings", content, this::showSettings, "settings");
    }

    private void pickImportDatabase() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQ_IMPORT_DB);
    }

    private void pickExportDatabase() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, DbHelper.DB_NAME);
        startActivityForResult(intent, REQ_EXPORT_DB);
    }

    private void pickExportStudentsCsv() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "students_export.csv");
        startActivityForResult(intent, REQ_EXPORT_STUDENTS);
    }

    private void pickExportCoursesCsv() {
        pendingExportReturn = "settings";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "courses_export.csv");
        startActivityForResult(intent, REQ_EXPORT_COURSES);
    }

    private void pickExportProjectsCsv() {
        pendingExportReturn = "settings";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "projects_export.csv");
        startActivityForResult(intent, REQ_EXPORT_PROJECTS);
    }

    private void pickExportTasksCsv() {
        pendingExportReturn = "settings";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "tasks_export.csv");
        startActivityForResult(intent, REQ_EXPORT_TASKS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == REQ_IMPORT_DB) {
                importDatabase(uri);
                ReminderScheduler.scheduleAll(this);
                showSettings();
            } else if (requestCode == REQ_EXPORT_DB) {
                exportDatabase(uri);
                showSettings();
            } else if (requestCode == REQ_EXPORT_STUDENTS) {
                exportStudentsCsv(uri);
                returnAfterCsvExport();
            } else if (requestCode == REQ_EXPORT_COURSES) {
                exportCoursesCsv(uri);
                returnAfterCsvExport();
            } else if (requestCode == REQ_EXPORT_PROJECTS) {
                exportProjectsCsv(uri);
                returnAfterCsvExport();
            } else if (requestCode == REQ_EXPORT_TASKS) {
                exportTasksCsv(uri);
                returnAfterCsvExport();
            } else if (requestCode == REQ_EXPORT_REMINDERS) {
                writePendingBytes(uri);
                showReminders();
            } else if (requestCode == REQ_UPLOAD_FILE) {
                handleUploadedFile(uri);
            } else if (requestCode == REQ_SAVE_FILE_AS) {
                copyPathToUri(pendingSourcePath, uri);
                toast("File saved.");
            } else if (requestCode == REQ_EXPORT_FOLDER_ZIP) {
                writePendingBytes(uri);
                showFileInventory();
            }
        } catch (Exception ex) {
            toast("Operation failed: " + ex.getMessage());
        }
    }

    private void importDatabase(Uri uri) throws Exception {
        File dbFile = getDatabasePath(DbHelper.DB_NAME);
        File temp = new File(getCacheDir(), "import_check.db");
        try (InputStream in = getContentResolver().openInputStream(uri); OutputStream out = new FileOutputStream(temp, false)) {
            if (in == null) throw new Exception("Cannot open selected file.");
            copyStream(in, out);
        }
        SQLiteDatabase check = SQLiteDatabase.openDatabase(temp.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
        Cursor cursor = check.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='students'", null);
        boolean compatible = cursor.moveToFirst();
        cursor.close();
        if (!compatible) {
            check.close();
            throw new Exception("Selected file is not a compatible assistant_data.db database.");
        }
        DbHelper.ensureSchema(check);
        check.close();

        db.close();
        File backup = new File(dbFile.getParentFile(), "assistant_data_backup_" + System.currentTimeMillis() + ".db");
        if (dbFile.exists()) copyFile(dbFile, backup);
        deleteSidecars(dbFile);
        copyFile(temp, dbFile);
        deleteSidecars(dbFile);
        temp.delete();
        db = new DbHelper(this);
        db.getWritableDatabase();
        toast("Database imported. A local backup was created.");
    }

    private void exportDatabase(Uri uri) throws Exception {
        File dbFile = getDatabasePath(DbHelper.DB_NAME);
        Cursor checkpoint = db.getWritableDatabase().rawQuery("PRAGMA wal_checkpoint(FULL)", null);
        checkpoint.close();
        try (InputStream in = new FileInputStream(dbFile); OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) throw new Exception("Cannot write selected file.");
            copyStream(in, out);
        }
        toast("Database exported/synced.");
    }

    private void deleteSidecars(File dbFile) {
        new File(dbFile.getAbsolutePath() + "-wal").delete();
        new File(dbFile.getAbsolutePath() + "-shm").delete();
    }

    private void copyFile(File source, File destination) throws Exception {
        try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(destination)) {
            copyStream(in, out);
        }
    }

    private void copyStream(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[8192];
        int count;
        while ((count = in.read(buffer)) >= 0) out.write(buffer, 0, count);
    }

    private void writePendingBytes(Uri uri) throws Exception {
        if (pendingExportBytes == null) throw new Exception("No report data is ready.");
        try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) throw new Exception("Cannot write selected file.");
            out.write(pendingExportBytes);
        }
        toast(pendingExportMessage.isEmpty() ? "Export completed." : pendingExportMessage);
        pendingExportBytes = null;
        pendingExportMessage = "";
    }

    private void exportStudentsCsv(Uri uri) throws Exception {
        String[] headers = {"Level", "First name", "Family name", "Student No", "National ID", "Registration year", "Registration semester", "Email", "Telephone", "Supervisor", "Second supervisor", "Advisor", "Referee", "Status", "Thesis status"};
        StringBuilder text = new StringBuilder();
        appendCsvRow(text, headers);
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM students ORDER BY level,family_name,first_name", null);
        try {
            while (cursor.moveToNext()) {
                appendCsvRow(text, new String[]{safe(cursor,"level"), safe(cursor,"first_name"), safe(cursor,"family_name"), safe(cursor,"student_no"), safe(cursor,"national_id"), safe(cursor,"registration_date"), safe(cursor,"registration_semester"), safe(cursor,"email"), safe(cursor,"telephone"), safe(cursor,"supervisor"), safe(cursor,"second_supervisor"), safe(cursor,"advisor"), safe(cursor,"referee"), safe(cursor,"status"), safe(cursor,"thesis_status")});
            }
        } finally { cursor.close(); }
        writeCsv(uri, text, "Student CSV exported.");
    }

    private void returnAfterCsvExport() {
        skipHistoryOnce = true;
        if ("students".equals(pendingExportReturn)) showStudents();
        else showSettings();
        pendingExportReturn = "settings";
    }

    private void writeCsv(Uri uri, StringBuilder text, String successMessage) throws Exception {
        try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) throw new Exception("Cannot write CSV.");
            out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            out.write(text.toString().getBytes(StandardCharsets.UTF_8));
        }
        toast(successMessage);
    }

    private void exportCoursesCsv(Uri uri) throws Exception {
        StringBuilder text = new StringBuilder();
        appendCsvRow(text, new String[]{"Level", "Course title", "Course code", "Semester", "Instructor", "Start date", "End date", "Notes", "Created"});
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM courses ORDER BY created_at DESC,id DESC", null);
        try {
            while (cursor.moveToNext()) appendCsvRow(text, new String[]{safe(cursor,"level"), safe(cursor,"course_title"), safe(cursor,"course_code"), safe(cursor,"semester"), safe(cursor,"instructor"), safe(cursor,"start_date"), safe(cursor,"end_date"), safe(cursor,"notes"), safe(cursor,"created_at")});
        } finally { cursor.close(); }
        writeCsv(uri, text, "Courses CSV exported.");
    }

    private void exportProjectsCsv(Uri uri) throws Exception {
        StringBuilder text = new StringBuilder();
        appendCsvRow(text, new String[]{"Project", "Status", "Progress %", "Start date", "End date", "Principal investigator", "Contractor", "Notes", "Created"});
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM projects ORDER BY created_at DESC,id DESC", null);
        try {
            while (cursor.moveToNext()) appendCsvRow(text, new String[]{safe(cursor,"name"), safe(cursor,"status"), String.valueOf(safeInt(cursor,"progress")), safe(cursor,"start_jdate"), safe(cursor,"end_jdate"), safe(cursor,"principal_investigator"), safe(cursor,"contractor"), safe(cursor,"notes"), safe(cursor,"created_at")});
        } finally { cursor.close(); }
        writeCsv(uri, text, "Projects CSV exported.");
    }

    private void exportTasksCsv(Uri uri) throws Exception {
        StringBuilder text = new StringBuilder();
        appendCsvRow(text, new String[]{"Task type", "Title", "Owner", "Responsible", "Due date", "Details", "Completed", "Created"});
        Cursor projectTasks = db.getReadableDatabase().rawQuery("SELECT t.*,p.name AS owner_name FROM todos t LEFT JOIN projects p ON p.id=t.project_id ORDER BY t.created_at DESC,t.id DESC", null);
        try {
            while (projectTasks.moveToNext()) appendCsvRow(text, new String[]{safe(projectTasks,"project_id").isEmpty() ? "General" : "Project", safe(projectTasks,"title"), safe(projectTasks,"owner_name"), safe(projectTasks,"responsible"), safe(projectTasks,"due_jdate"), "", safeInt(projectTasks,"done") == 1 ? "Yes" : "No", safe(projectTasks,"created_at")});
        } finally { projectTasks.close(); }
        Cursor studentTasks = db.getReadableDatabase().rawQuery("SELECT st.*,s.first_name,s.family_name FROM student_tasks st JOIN students s ON s.id=st.student_id ORDER BY st.created_at DESC,st.id DESC", null);
        try {
            while (studentTasks.moveToNext()) appendCsvRow(text, new String[]{"Student", safe(studentTasks,"title"), (safe(studentTasks,"first_name") + " " + safe(studentTasks,"family_name")).trim(), "", safe(studentTasks,"due_jdate"), safe(studentTasks,"details"), safeInt(studentTasks,"done") == 1 ? "Yes" : "No", safe(studentTasks,"created_at")});
        } finally { studentTasks.close(); }
        Cursor sheetTasks = db.getReadableDatabase().rawQuery("SELECT ct.*,cs.title AS owner_name FROM custom_sheet_tasks ct JOIN custom_sheets cs ON cs.id=ct.sheet_id ORDER BY ct.created_at DESC,ct.id DESC", null);
        try {
            while (sheetTasks.moveToNext()) appendCsvRow(text, new String[]{"Custom sheet", safe(sheetTasks,"title"), safe(sheetTasks,"owner_name"), safe(sheetTasks,"responsible"), safe(sheetTasks,"due_jdate"), safe(sheetTasks,"details"), safeInt(sheetTasks,"done") == 1 ? "Yes" : "No", safe(sheetTasks,"created_at")});
        } finally { sheetTasks.close(); }
        writeCsv(uri, text, "Tasks CSV exported.");
    }

    private void appendCsvRow(StringBuilder builder, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) builder.append(',');
            String value = values[i] == null ? "" : values[i];
            builder.append('"').append(value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ")).append('"');
        }
        builder.append('\n');
    }

    // ---------------- Reminders ----------------

    private void showReminders() {
        showRemindersForDate("");
    }

    private void showRemindersForDate(String filterDate) {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv(filterDate.isEmpty() ? "Reminders" : "Reminders on " + filterDate, 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Add reminder"), v -> editReminder(0, filterDate));
        addButton(top, outlineBtn("Excel report by month"), v -> showReminderReportMonths());
        if (!filterDate.isEmpty()) addButton(top, outlineBtn("Show all reminders"), v -> showReminders());
        content.addView(top);

        String sql = "SELECT id,title,details,jdate,time_text FROM reminders" + (filterDate.isEmpty() ? "" : " WHERE jdate=?") + " ORDER BY remind_at_iso";
        String[] args = filterDate.isEmpty() ? null : new String[]{filterDate};
        Cursor cursor = db.getReadableDatabase().rawQuery(sql, args);
        try {
            if (!cursor.moveToFirst()) {
                LinearLayout empty = card();
                empty.addView(tv("No reminders.", 14, mutedColor, Typeface.NORMAL));
                content.addView(empty);
            } else {
                do {
                    final long id = cursor.getLong(0);
                    LinearLayout item = card();
                    item.addView(tv(safe(cursor,"title"), 17, textColor, Typeface.BOLD));
                    item.addView(tv(safe(cursor,"jdate") + "  " + safe(cursor,"time_text"), 14, primaryColor, Typeface.BOLD));
                    String details = safe(cursor,"details");
                    if (!details.isEmpty()) item.addView(tv(details, 14, mutedColor, Typeface.NORMAL));
                    addButton(item, outlineBtn("Edit"), v -> editReminder(id, filterDate));
                    content.addView(item);
                } while (cursor.moveToNext());
            }
        } finally { cursor.close(); }
        setScreen("Reminders", content, () -> showRemindersForDate(filterDate), "reminders:" + filterDate);
    }

    private void editReminder(long id) {
        editReminder(id, "");
    }

    private void editReminder(long id, String returnFilterDate) {
        String title = "";
        String details = "";
        String jdate = returnFilterDate == null || returnFilterDate.isEmpty() ? Jalali.today() : returnFilterDate;
        String time = Jalali.nowTime();
        boolean exists = id > 0;
        if (exists) {
            Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM reminders WHERE id=?", new String[]{String.valueOf(id)});
            try {
                if (cursor.moveToFirst()) {
                    title = safe(cursor,"title");
                    details = safe(cursor,"details");
                    jdate = safe(cursor,"jdate");
                    time = safe(cursor,"time_text");
                }
            } finally { cursor.close(); }
        }
        LinearLayout content = vbox();
        LinearLayout form = card();
        form.addView(tv(exists ? "Edit reminder" : "Add reminder", 18, textColor, Typeface.BOLD));
        final EditText titleEdit = input("Title", title, false);
        final EditText detailsEdit = input("Details", details, true);
        form.addView(titleEdit);
        form.addView(detailsEdit);
        final EditText dateEdit = dateField(form, "Reminder date", jdate, false);
        final EditText timeEdit = timeField(form, "Reminder time", time);
        addButton(form, btn("Save reminder"), v -> {
            String value = titleEdit.getText().toString().trim();
            if (value.isEmpty()) { toast("Enter a reminder title."); return; }
            ContentValues cv = new ContentValues();
            cv.put("title", value);
            cv.put("details", detailsEdit.getText().toString());
            cv.putNull("project_id");
            cv.put("jdate", dateEdit.getText().toString().trim());
            cv.put("time_text", timeEdit.getText().toString().trim());
            cv.put("remind_at_iso", Jalali.toIso(cv.getAsString("jdate"), cv.getAsString("time_text")));
            long reminderId;
            if (exists) {
                update("reminders", cv, id);
                reminderId = id;
            } else {
                cv.put("created_at", nowIso());
                reminderId = insert("reminders", cv);
            }
            ReminderScheduler.scheduleReminder(this, db.getWritableDatabase(), reminderId, value, detailsEdit.getText().toString(), cv.getAsString("jdate"), cv.getAsString("time_text"));
            toast("Reminder saved and phone notifications scheduled.");
            skipHistoryOnce = true;
            if (returnFilterDate == null || returnFilterDate.isEmpty()) showReminders();
            else showRemindersForDate(returnFilterDate);
        });
        if (exists) addButton(form, dangerBtn("Delete reminder"), v -> confirm("Delete reminder", "Delete this reminder?", () -> {
            ReminderScheduler.cancelReminder(this, id);
            delete("reminders", id);
            skipHistoryOnce = true;
            if (returnFilterDate == null || returnFilterDate.isEmpty()) showReminders();
            else showRemindersForDate(returnFilterDate);
        }));
        addButton(form, outlineBtn("Back"), v -> goBackOneStep());
        content.addView(form);
        setScreen(exists ? "Edit reminder" : "Add reminder", content, () -> editReminder(id, returnFilterDate), "reminder:" + id + ":" + returnFilterDate);
    }

    private void showReminderReportMonths() {
        LinearLayout box = vbox();
        box.setPadding(dp(12), dp(8), dp(12), dp(8));
        EditText yearEdit = input("Year", String.valueOf(Jalali.currentYear()), false);
        yearEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        box.addView(yearEdit);
        final List<CheckBox> checks = new ArrayList<>();
        String[] names = {"Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar", "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand"};
        for (int row = 0; row < 4; row++) {
            LinearLayout line = hbox();
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                CheckBox check = new CheckBox(this);
                check.setText((index + 1) + " - " + names[index]);
                check.setTextColor(textColor);
                checks.add(check);
                line.addView(check, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            }
            box.addView(line);
        }
        alertBuilder()
                .setTitle("Select one or more months")
                .setView(box)
                .setPositiveButton("Create Excel", (dialog, which) -> {
                    List<Integer> months = new ArrayList<>();
                    for (int i = 0; i < checks.size(); i++) if (checks.get(i).isChecked()) months.add(i + 1);
                    if (months.isEmpty()) { toast("Select at least one month."); return; }
                    int year;
                    try { year = Integer.parseInt(Jalali.normalizeDigits(yearEdit.getText().toString())); }
                    catch (Exception ex) { toast("Enter a valid year."); return; }
                    try {
                        prepareReminderXlsx(year, months);
                        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                        intent.putExtra(Intent.EXTRA_TITLE, "reminders_" + year + ".xlsx");
                        startActivityForResult(intent, REQ_EXPORT_REMINDERS);
                    } catch (Exception ex) {
                        toast("Cannot create report: " + ex.getMessage());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void prepareReminderXlsx(int year, List<Integer> months) throws Exception {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Title", "Details", "Date", "Time"});
        StringBuilder where = new StringBuilder();
        List<String> args = new ArrayList<>();
        for (int i = 0; i < months.size(); i++) {
            if (i > 0) where.append(" OR ");
            where.append("jdate LIKE ?");
            args.add(String.format(Locale.US, "%04d/%02d/%%", year, months.get(i)));
        }
        Cursor cursor = db.getReadableDatabase().rawQuery(
                "SELECT title,details,jdate,time_text FROM reminders WHERE " + where + " ORDER BY remind_at_iso",
                args.toArray(new String[0])
        );
        try {
            while (cursor.moveToNext()) rows.add(new String[]{cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)});
        } finally { cursor.close(); }
        pendingExportBytes = XlsxWriter.createWorkbook("Reminders", rows);
        pendingExportMessage = "Reminder Excel report exported.";
    }

    // ---------------- Students ----------------

    private void chooseStudentLevelForAdd() {
        String[] levels = {"B.S.c", "M.S.c", "P.h.D"};
        alertBuilder()
                .setTitle("Student level")
                .setItems(levels, (dialog, which) -> editStudent(0, levels[which]))
                .show();
    }

    private void showStudents() {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Students", 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Add student"), v -> chooseStudentLevelForAdd());
        addButton(top, outlineBtn("Export list for Excel (CSV)"), v -> { pendingExportReturn = "students"; pickExportStudentsCsv(); });
        content.addView(top);
        Cursor cursor = db.getReadableDatabase().rawQuery(
                "SELECT id,level,first_name,family_name,student_no,status,thesis_status FROM students ORDER BY level,family_name,first_name",
                null
        );
        try {
            if (!cursor.moveToFirst()) {
                LinearLayout empty = card();
                empty.addView(tv("No students yet.", 14, mutedColor, Typeface.NORMAL));
                content.addView(empty);
            } else {
                do {
                    final long id = cursor.getLong(0);
                    String name = (safe(cursor,"first_name") + " " + safe(cursor,"family_name")).trim();
                    if (name.isEmpty()) name = "Unnamed student";
                    LinearLayout item = card();
                    item.addView(tv(name, 17, textColor, Typeface.BOLD));
                    item.addView(tv(canonicalLevel(safe(cursor,"level")) + " • " + safe(cursor,"student_no"), 13, primaryColor, Typeface.BOLD));
                    item.addView(tv("Status: " + safe(cursor,"status") + " | Thesis: " + safe(cursor,"thesis_status"), 13, mutedColor, Typeface.NORMAL));
                    addButton(item, outlineBtn("Open student"), v -> editStudent(id, ""));
                    content.addView(item);
                } while (cursor.moveToNext());
            }
        } finally { cursor.close(); }
        setScreen("Students", content, this::showStudents, "students");
    }

    private String canonicalLevel(String value) {
        if (value == null) return "";
        if (value.equalsIgnoreCase("BSc") || value.equalsIgnoreCase("B.S.c")) return "B.S.c";
        if (value.equalsIgnoreCase("MSc") || value.equalsIgnoreCase("M.S.c")) return "M.S.c";
        if (value.equalsIgnoreCase("PhD") || value.equalsIgnoreCase("P.h.D")) return "P.h.D";
        return value;
    }

    private void editStudent(long id, String requestedLevel) {
        Map<String,String> data = new LinkedHashMap<>();
        String[] fields = {"level","first_name","family_name","student_no","national_id","registration_date","registration_semester","email","telephone","supervisor","second_supervisor","advisor","referee","bsc_project_form_date","status","thesis_status","research_background"};
        for (String field : fields) data.put(field, "");
        data.put("level", requestedLevel.isEmpty() ? "M.S.c" : requestedLevel);
        data.put("registration_semester", "1");
        boolean exists = id > 0;
        if (exists) {
            Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM students WHERE id=?", new String[]{String.valueOf(id)});
            try {
                if (cursor.moveToFirst()) for (String field : fields) data.put(field, safe(cursor, field));
            } finally { cursor.close(); }
        }
        final String level = canonicalLevel(data.get("level"));
        final boolean isBsc = level.equals("B.S.c");
        final boolean isMscOrPhd = level.equals("M.S.c") || level.equals("P.h.D");

        LinearLayout content = vbox();
        LinearLayout form = card();
        form.addView(tv(exists ? "Student information" : "Add " + level + " student", 18, textColor, Typeface.BOLD));
        form.addView(tv("Level: " + level, 14, primaryColor, Typeface.BOLD));

        final EditText firstName = input("Name", data.get("first_name"), false);
        final EditText familyName = input("Family name", data.get("family_name"), false);
        final EditText studentNo = input("Student number", data.get("student_no"), false);
        final EditText nationalId = input("National ID / passport", data.get("national_id"), false);
        final EditText email = input("Email", data.get("email"), false);
        final EditText telephone = input("Telephone", data.get("telephone"), false);
        form.addView(firstName); form.addView(familyName); form.addView(studentNo); form.addView(nationalId); form.addView(email); form.addView(telephone);
        final EditText registrationYear = dateField(form, "Registration year", registrationYearOnly(data.get("registration_date")), true);
        form.addView(tv("Registration semester", 14, textColor, Typeface.BOLD));
        final Spinner registrationSemester = spinner(new String[]{"1", "2"}, data.get("registration_semester"));
        form.addView(registrationSemester);

        final EditText supervisor = input("Supervisor", data.get("supervisor"), false);
        final EditText secondSupervisor = input("Second supervisor", data.get("second_supervisor"), false);
        final EditText advisor = input("Advisor", data.get("advisor"), false);
        final EditText referee = input("Referee name", data.get("referee"), false);
        form.addView(supervisor); form.addView(secondSupervisor); form.addView(advisor); form.addView(referee);

        final EditText bscFormDate;
        if (isBsc) bscFormDate = dateField(form, "تاریخ ارسال فرم پروژه به دانشکده", data.get("bsc_project_form_date"), false);
        else bscFormDate = null;

        final Map<String,EditText> academicDates = new LinkedHashMap<>();
        if (isMscOrPhd) {
            String[] labels = {"تاریخ ارائه سمینار", "تاریخ تصویب پروپوزال", "تاریخ دفاع"};
            for (String label : labels) academicDates.put(label, dateField(form, label, getStudentDate(id, label), false));
        }

        form.addView(tv("Student status", 14, textColor, Typeface.BOLD));
        final Spinner status = spinner(new String[]{"Active", "Graduated", "Suspended", "Withdrawn", "Other"}, data.get("status").isEmpty() ? "Active" : data.get("status"));
        form.addView(status);
        form.addView(tv("Thesis status", 14, textColor, Typeface.BOLD));
        final Spinner thesisStatus = spinner(new String[]{"Not started", "In progress", "Proposal approved", "Ready for defense", "Defended", "Completed"}, data.get("thesis_status").isEmpty() ? "Not started" : data.get("thesis_status"));
        form.addView(thesisStatus);

        final EditText projectTitle;
        if (isBsc) {
            form.addView(tv("Project title", 14, textColor, Typeface.BOLD));
            projectTitle = input("Project title", data.get("research_background"), true);
            form.addView(projectTitle);
        } else {
            projectTitle = null;
        }

        addButton(form, btn("Save student"), v -> {
            if (firstName.getText().toString().trim().isEmpty() && familyName.getText().toString().trim().isEmpty()) {
                toast("Enter at least the student name or family name.");
                return;
            }
            ContentValues cv = new ContentValues();
            cv.put("level", level);
            cv.put("first_name", firstName.getText().toString());
            cv.put("family_name", familyName.getText().toString());
            cv.put("student_no", studentNo.getText().toString());
            cv.put("national_id", nationalId.getText().toString());
            cv.put("registration_date", registrationYear.getText().toString());
            cv.put("registration_semester", registrationSemester.getSelectedItem().toString());
            cv.put("email", email.getText().toString());
            cv.put("telephone", telephone.getText().toString());
            cv.put("supervisor", supervisor.getText().toString());
            cv.put("second_supervisor", secondSupervisor.getText().toString());
            cv.put("advisor", advisor.getText().toString());
            cv.put("referee", referee.getText().toString());
            cv.put("bsc_project_form_date", bscFormDate == null ? "" : bscFormDate.getText().toString());
            cv.put("status", status.getSelectedItem().toString());
            cv.put("thesis_status", thesisStatus.getSelectedItem().toString());
            if (isBsc) cv.put("research_background", projectTitle.getText().toString());
            long studentId;
            if (exists) {
                update("students", cv, id);
                studentId = id;
            } else {
                cv.put("notes", "");
                cv.put("folder_path", "");
                cv.put("created_at", nowIso());
                studentId = insert("students", cv);
            }
            if (isMscOrPhd) {
                for (Map.Entry<String,EditText> entry : academicDates.entrySet()) setStudentDate(studentId, entry.getKey(), entry.getValue().getText().toString());
            }
            toast("Student saved.");
            editStudent(studentId, level);
        });

        if (exists) {
            addButton(form, outlineBtn("Student documents"), v -> showStudentDocuments(id));
            addButton(form, outlineBtn("Student tasks"), v -> showStudentTasks(id));
            addButton(form, dangerBtn("Delete student"), v -> confirm("Delete student", "Delete this student and related records?", () -> {
                delete("students", id);
                skipHistoryOnce = true;
                showStudents();
            }));
        }
        addButton(form, outlineBtn("Back"), v -> goBackOneStep());
        content.addView(form);
        setScreen(exists ? "Student information" : "Add student", content, () -> editStudent(id, requestedLevel), "student:" + id + ":" + requestedLevel);
    }

    private String registrationYearOnly(String value) {
        String normalized = Jalali.normalizeDigits(value);
        if (normalized.contains("/")) return normalized.split("/")[0];
        return normalized;
    }

    private String getStudentDate(long studentId, String label) {
        if (studentId <= 0) return "";
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT date_value FROM student_dates WHERE student_id=? AND date_label=? ORDER BY id DESC LIMIT 1", new String[]{String.valueOf(studentId), label});
        try { return cursor.moveToFirst() ? cursor.getString(0) : ""; }
        finally { cursor.close(); }
    }

    private void setStudentDate(long studentId, String label, String value) {
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT id FROM student_dates WHERE student_id=? AND date_label=? ORDER BY id DESC LIMIT 1", new String[]{String.valueOf(studentId), label});
        long existingId = 0;
        try { if (cursor.moveToFirst()) existingId = cursor.getLong(0); }
        finally { cursor.close(); }
        String clean = value == null ? "" : value.trim();
        if (existingId > 0 && clean.isEmpty()) delete("student_dates", existingId);
        else if (existingId > 0) {
            ContentValues cv = new ContentValues();
            cv.put("date_value", clean);
            update("student_dates", cv, existingId);
        } else if (!clean.isEmpty()) {
            ContentValues cv = new ContentValues();
            cv.put("student_id", studentId);
            cv.put("date_label", label);
            cv.put("date_value", clean);
            cv.put("created_at", nowIso());
            insert("student_dates", cv);
        }
    }

    private void showStudentTasks(long studentId) {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Student tasks", 18, textColor, Typeface.BOLD));
        final EditText title = input("Task title", "", false);
        final EditText details = input("Details", "", true);
        top.addView(title);
        top.addView(details);
        final EditText dueDate = dateField(top, "Due date", "", false);
        addButton(top, btn("Add task"), v -> {
            if (title.getText().toString().trim().isEmpty()) { toast("Enter task title."); return; }
            ContentValues cv = new ContentValues();
            cv.put("student_id", studentId);
            cv.put("title", title.getText().toString());
            cv.put("details", details.getText().toString());
            cv.put("due_jdate", dueDate.getText().toString());
            cv.put("done", 0);
            cv.put("created_at", nowIso());
            cv.put("completed_at", "");
            insert("student_tasks", cv);
            showStudentTasks(studentId);
        });
        addButton(top, outlineBtn("Back to student"), v -> goBackOneStep());
        content.addView(top);

        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT id,title,details,due_jdate,done FROM student_tasks WHERE student_id=? ORDER BY created_at DESC,id DESC", new String[]{String.valueOf(studentId)});
        try {
            while (cursor.moveToNext()) {
                final long taskId = cursor.getLong(0);
                final int done = safeInt(cursor,"done");
                LinearLayout item = card();
                item.addView(tv((done == 1 ? "✓ " : "") + safe(cursor,"title"), 16, textColor, Typeface.BOLD));
                if (!safe(cursor,"details").isEmpty()) item.addView(tv(safe(cursor,"details"), 13, mutedColor, Typeface.NORMAL));
                if (!safe(cursor,"due_jdate").isEmpty()) item.addView(tv("Due: " + safe(cursor,"due_jdate"), 13, primaryColor, Typeface.BOLD));
                addButton(item, outlineBtn(done == 1 ? "Mark not done" : "Mark done"), v -> {
                    ContentValues cv = new ContentValues();
                    cv.put("done", done == 1 ? 0 : 1);
                    cv.put("completed_at", done == 1 ? "" : nowIso());
                    update("student_tasks", cv, taskId);
                    showStudentTasks(studentId);
                });
                addButton(item, dangerBtn("Delete"), v -> {
                    delete("student_tasks", taskId);
                    showStudentTasks(studentId);
                });
                content.addView(item);
            }
        } finally { cursor.close(); }
        setScreen("Student tasks", content, () -> showStudentTasks(studentId), "student-tasks:" + studentId);
    }

    private void showStudentDocuments(long studentId) {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Student documents", 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Upload document"), v -> promptStudentUpload(studentId));
        addButton(top, outlineBtn("Back to student"), v -> goBackOneStep());
        content.addView(top);
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM student_files WHERE student_id=? ORDER BY sort_order ASC,id DESC", new String[]{String.valueOf(studentId)});
        try {
            while (cursor.moveToNext()) {
                final long fileId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                LinearLayout item = fileCard(safe(cursor,"display_name"), safe(cursor,"document_type"), safe(cursor,"uploaded_at"));
                item.setOnClickListener(v -> openStoredFile("student_files", fileId));
                item.setOnLongClickListener(v -> {
                    showStudentFileMenu(fileId, studentId);
                    return true;
                });
                addOrderButtons(item, "student_files", fileId, "student_id=?", new String[]{String.valueOf(studentId)}, () -> showStudentDocuments(studentId));
                content.addView(item);
            }
        } finally { cursor.close(); }
        setScreen("Student documents", content, () -> showStudentDocuments(studentId), "student-documents:" + studentId);
    }

    private void promptStudentUpload(long studentId) {
        String[] types = {"Article", "Conference", "Report", "Thesis", "Seminar", "Proposal", "Patent", "Form", "Other"};
        LinearLayout form = vbox();
        form.setPadding(dp(12), dp(4), dp(12), dp(4));
        Spinner type = spinner(types, "Other");
        EditText title = input("Document title (optional)", "", false);
        form.addView(tv("Document type", 14, textColor, Typeface.BOLD));
        form.addView(type);
        form.addView(title);
        alertBuilder()
                .setTitle("Upload student document")
                .setView(form)
                .setPositiveButton("Choose file", (dialog, which) -> pickFileForUpload("student", studentId, "", type.getSelectedItem().toString(), title.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStudentFileMenu(long fileId, long studentId) {
        String[] actions = {"Open", "Delete", "Edit file type", "Edit title"};
        alertBuilder()
                .setTitle("Student document")
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) openStoredFile("student_files", fileId);
                    else if (which == 1) confirm("Delete document", "Delete this document?", () -> {
                        deleteFileRecord("student_files", fileId);
                        showStudentDocuments(studentId);
                    });
                    else if (which == 2) promptText("Edit file type", "Document type", getFileColumn("student_files", fileId, "document_type"), value -> {
                        ContentValues cv = new ContentValues();
                        cv.put("document_type", value);
                        update("student_files", cv, fileId);
                        showStudentDocuments(studentId);
                    });
                    else if (which == 3) promptText("Edit title", "Document title", getFileColumn("student_files", fileId, "display_name"), value -> {
                        ContentValues cv = new ContentValues();
                        cv.put("display_name", value);
                        update("student_files", cv, fileId);
                        showStudentDocuments(studentId);
                    });
                })
                .show();
    }

    // ---------------- Unified tasks ----------------

    private void chooseTaskTarget() {
        String[] options = {"General task", "Project task", "Student task"};
        alertBuilder()
                .setTitle("Add task")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showAddGeneralTask();
                    else if (which == 1) chooseProjectForTask();
                    else chooseStudentForTask();
                })
                .show();
    }

    private void showAddGeneralTask() {
        LinearLayout content = vbox();
        LinearLayout form = card();
        form.addView(tv("Add general task", 18, textColor, Typeface.BOLD));
        final EditText title = input("Task title", "", false);
        final EditText responsible = input("Responsible", "", false);
        form.addView(title);
        form.addView(responsible);
        final EditText due = dateField(form, "Due date", "", false);
        addButton(form, btn("Save task"), v -> {
            if (title.getText().toString().trim().isEmpty()) { toast("Enter task title."); return; }
            ContentValues cv = new ContentValues();
            cv.put("title", title.getText().toString().trim());
            cv.putNull("project_id");
            cv.put("due_iso", "");
            cv.put("due_jdate", due.getText().toString());
            cv.put("responsible", responsible.getText().toString());
            cv.put("done", 0);
            cv.put("created_at", nowIso());
            insert("todos", cv);
            skipHistoryOnce = true;
            showAllTasks();
        });
        addButton(form, outlineBtn("Back"), v -> goBackOneStep());
        content.addView(form);
        setScreen("Add task", content, this::showAddGeneralTask, "task:add");
    }

    private void chooseProjectForTask() {
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT id,name FROM projects ORDER BY name", null);
        List<Long> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        try {
            while (cursor.moveToNext()) { ids.add(cursor.getLong(0)); names.add(cursor.getString(1)); }
        } finally { cursor.close(); }
        if (ids.isEmpty()) { toast("Create a project first."); return; }
        alertBuilder().setTitle("Select project").setItems(names.toArray(new String[0]), (d, which) -> showProjectTasks(ids.get(which))).show();
    }

    private void chooseStudentForTask() {
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT id,first_name,family_name,level FROM students ORDER BY family_name,first_name", null);
        List<Long> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
                names.add((cursor.getString(1) + " " + cursor.getString(2)).trim() + " • " + canonicalLevel(cursor.getString(3)));
            }
        } finally { cursor.close(); }
        if (ids.isEmpty()) { toast("Create a student first."); return; }
        alertBuilder().setTitle("Select student").setItems(names.toArray(new String[0]), (d, which) -> showStudentTasks(ids.get(which))).show();
    }

    private void showAllTasks() {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("All tasks", 18, textColor, Typeface.BOLD));
        top.addView(tv("Tasks are sorted by creation date, newest first.", 13, mutedColor, Typeface.NORMAL));
        addButton(top, btn("+ Add task"), v -> chooseTaskTarget());
        content.addView(top);

        Cursor projectTasks = db.getReadableDatabase().rawQuery(
                "SELECT t.id,t.title,t.done,t.due_jdate,t.responsible,p.name FROM todos t LEFT JOIN projects p ON p.id=t.project_id ORDER BY t.created_at DESC,t.id DESC",
                null
        );
        try {
            while (projectTasks.moveToNext()) {
                final long id = projectTasks.getLong(0);
                final int done = projectTasks.getInt(2);
                LinearLayout item = card();
                item.addView(tv((done == 1 ? "✓ " : "") + projectTasks.getString(1), 16, textColor, Typeface.BOLD));
                item.addView(tv("Project: " + (projectTasks.getString(5) == null ? "General" : projectTasks.getString(5)) + " • Due: " + projectTasks.getString(3) + " • Responsible: " + projectTasks.getString(4), 12, mutedColor, Typeface.NORMAL));
                addButton(item, outlineBtn(done == 1 ? "Mark not done" : "Mark done"), v -> {
                    ContentValues cv = new ContentValues();
                    cv.put("done", done == 1 ? 0 : 1);
                    update("todos", cv, id);
                    showAllTasks();
                });
                content.addView(item);
            }
        } finally { projectTasks.close(); }

        Cursor studentTasks = db.getReadableDatabase().rawQuery(
                "SELECT st.id,st.title,st.done,st.due_jdate,s.first_name,s.family_name FROM student_tasks st JOIN students s ON s.id=st.student_id ORDER BY st.created_at DESC,st.id DESC",
                null
        );
        try {
            while (studentTasks.moveToNext()) {
                final long id = studentTasks.getLong(0);
                final int done = studentTasks.getInt(2);
                LinearLayout item = card();
                item.addView(tv((done == 1 ? "✓ " : "") + studentTasks.getString(1), 16, textColor, Typeface.BOLD));
                item.addView(tv("Student: " + studentTasks.getString(4) + " " + studentTasks.getString(5) + " • Due: " + studentTasks.getString(3), 12, mutedColor, Typeface.NORMAL));
                addButton(item, outlineBtn(done == 1 ? "Mark not done" : "Mark done"), v -> {
                    ContentValues cv = new ContentValues();
                    cv.put("done", done == 1 ? 0 : 1);
                    cv.put("completed_at", done == 1 ? "" : nowIso());
                    update("student_tasks", cv, id);
                    showAllTasks();
                });
                content.addView(item);
            }
        } finally { studentTasks.close(); }
        setScreen("All tasks", content, this::showAllTasks, "tasks");
    }

    // ---------------- Projects ----------------

    private void showProjects() {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Projects", 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Add project"), v -> editProject(0));
        content.addView(top);
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT id,name,status,progress,principal_investigator,contractor FROM projects ORDER BY name", null);
        try {
            if (!cursor.moveToFirst()) {
                LinearLayout empty = card();
                empty.addView(tv("No projects yet.", 14, mutedColor, Typeface.NORMAL));
                content.addView(empty);
            } else {
                do {
                    final long id = cursor.getLong(0);
                    LinearLayout item = card();
                    item.addView(tv(safe(cursor,"name"), 17, textColor, Typeface.BOLD));
                    item.addView(tv("Status: " + safe(cursor,"status") + " • Progress: " + safeInt(cursor,"progress") + "%", 13, primaryColor, Typeface.BOLD));
                    item.addView(tv("PI: " + safe(cursor,"principal_investigator") + " | Contractor: " + safe(cursor,"contractor"), 13, mutedColor, Typeface.NORMAL));
                    addButton(item, outlineBtn("Open project"), v -> editProject(id));
                    content.addView(item);
                } while (cursor.moveToNext());
            }
        } finally { cursor.close(); }
        setScreen("Projects", content, this::showProjects, "projects");
    }

    private void editProject(long id) {
        Map<String,String> data = new LinkedHashMap<>();
        String[] fields = {"name","status","progress","start_jdate","end_jdate","principal_investigator","contractor","notes"};
        for (String field : fields) data.put(field, "");
        data.put("status", "Not started");
        data.put("progress", "0");
        boolean exists = id > 0;
        if (exists) {
            Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM projects WHERE id=?", new String[]{String.valueOf(id)});
            try { if (cursor.moveToFirst()) for (String field : fields) data.put(field, safe(cursor, field)); }
            finally { cursor.close(); }
        }
        LinearLayout content = vbox();
        LinearLayout form = card();
        form.addView(tv(exists ? "Project page" : "Add project", 18, textColor, Typeface.BOLD));
        final EditText name = input("Project name", data.get("name"), false);
        final EditText status = input("Status", data.get("status"), false);
        final EditText progress = input("Progress %", data.get("progress"), false);
        progress.setInputType(InputType.TYPE_CLASS_NUMBER);
        form.addView(name); form.addView(status); form.addView(progress);
        final EditText startDate = dateField(form, "Start date", data.get("start_jdate"), false);
        final EditText endDate = dateField(form, "End date", data.get("end_jdate"), false);
        final EditText principalInvestigator = input("Principal investigator", data.get("principal_investigator"), false);
        final EditText contractor = input("Contractor", data.get("contractor"), false);
        final EditText notes = input("Notes", data.get("notes"), true);
        form.addView(principalInvestigator); form.addView(contractor); form.addView(notes);
        addButton(form, btn("Save project"), v -> {
            if (name.getText().toString().trim().isEmpty()) { toast("Enter project name."); return; }
            ContentValues cv = new ContentValues();
            cv.put("name", name.getText().toString());
            cv.put("status", status.getText().toString());
            try { cv.put("progress", Integer.parseInt(progress.getText().toString().trim())); }
            catch (Exception ex) { cv.put("progress", 0); }
            cv.put("start_jdate", startDate.getText().toString());
            cv.put("end_jdate", endDate.getText().toString());
            cv.put("principal_investigator", principalInvestigator.getText().toString());
            cv.put("contractor", contractor.getText().toString());
            cv.put("notes", notes.getText().toString());
            long projectId;
            if (exists) {
                update("projects", cv, id);
                projectId = id;
            } else {
                cv.put("created_at", nowIso());
                projectId = insert("projects", cv);
            }
            toast("Project saved.");
            editProject(projectId);
        });
        if (exists) {
            addButton(form, outlineBtn("Project tasks"), v -> showProjectTasks(id));
            addButton(form, outlineBtn("Project staff"), v -> showProjectStaff(id));
            addButton(form, outlineBtn("Project documents"), v -> showProjectFiles(id, "Documents"));
            addButton(form, outlineBtn("Project letters"), v -> showProjectFiles(id, "Letters"));
            addButton(form, outlineBtn("WBS"), v -> showWbs(id));
            addButton(form, outlineBtn("CBS"), v -> showCbs(id));
            addButton(form, outlineBtn("S-curve data"), v -> showSCurve(id));
            addButton(form, outlineBtn("Gantt tasks"), v -> showGantt(id));
            addButton(form, dangerBtn("Delete project"), v -> confirm("Delete project", "Delete this project and related records?", () -> {
                delete("projects", id);
                skipHistoryOnce = true;
                showProjects();
            }));
        }
        addButton(form, outlineBtn("Back"), v -> goBackOneStep());
        content.addView(form);
        setScreen(exists ? "Project page" : "Add project", content, () -> editProject(id), "project:" + id);
    }

    private void showProjectTasks(long projectId) {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Project tasks", 18, textColor, Typeface.BOLD));
        final EditText title = input("Task title", "", false);
        final EditText responsible = input("Responsible", "", false);
        top.addView(title); top.addView(responsible);
        final EditText due = dateField(top, "Due date", "", false);
        addButton(top, btn("Add task"), v -> {
            if (title.getText().toString().trim().isEmpty()) { toast("Enter task title."); return; }
            ContentValues cv = new ContentValues();
            cv.put("title", title.getText().toString());
            cv.put("project_id", projectId);
            cv.put("due_jdate", due.getText().toString());
            cv.put("due_iso", "");
            cv.put("responsible", responsible.getText().toString());
            cv.put("done", 0);
            cv.put("created_at", nowIso());
            insert("todos", cv);
            showProjectTasks(projectId);
        });
        addButton(top, outlineBtn("Back to project"), v -> goBackOneStep());
        content.addView(top);
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT id,title,responsible,due_jdate,done FROM todos WHERE project_id=? ORDER BY created_at DESC,id DESC", new String[]{String.valueOf(projectId)});
        try {
            while (cursor.moveToNext()) {
                final long taskId = cursor.getLong(0);
                final int done = safeInt(cursor,"done");
                LinearLayout item = card();
                item.addView(tv((done == 1 ? "✓ " : "") + safe(cursor,"title"), 16, textColor, Typeface.BOLD));
                item.addView(tv(safe(cursor,"responsible") + " • Due: " + safe(cursor,"due_jdate"), 13, mutedColor, Typeface.NORMAL));
                addButton(item, outlineBtn(done == 1 ? "Mark not done" : "Mark done"), v -> {
                    ContentValues cv = new ContentValues();
                    cv.put("done", done == 1 ? 0 : 1);
                    update("todos", cv, taskId);
                    showProjectTasks(projectId);
                });
                addButton(item, dangerBtn("Delete"), v -> {
                    delete("todos", taskId);
                    showProjectTasks(projectId);
                });
                content.addView(item);
            }
        } finally { cursor.close(); }
        setScreen("Project tasks", content, () -> showProjectTasks(projectId), "project-tasks:" + projectId);
    }

    private void showProjectStaff(long projectId) {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Project staff", 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Add staff member"), v -> editProjectStaff(projectId, 0));
        addButton(top, outlineBtn("Back to project"), v -> goBackOneStep());
        content.addView(top);
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM staff_members WHERE project_id=? ORDER BY family_name,first_name,id", new String[]{String.valueOf(projectId)});
        try {
            while (cursor.moveToNext()) {
                final long staffId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                LinearLayout item = card();
                String title = safe(cursor,"title");
                String name = (safe(cursor,"first_name") + " " + safe(cursor,"family_name")).trim();
                item.addView(tv((title.isEmpty() ? "" : title + " ") + name, 16, textColor, Typeface.BOLD));
                item.addView(tv(safe(cursor,"role") + " • " + safe(cursor,"email") + " • " + safe(cursor,"telephone"), 13, mutedColor, Typeface.NORMAL));
                addButton(item, outlineBtn("Edit"), v -> editProjectStaff(projectId, staffId));
                content.addView(item);
            }
        } finally { cursor.close(); }
        setScreen("Project staff", content, () -> showProjectStaff(projectId), "project-staff:" + projectId);
    }

    private void editProjectStaff(long projectId, long staffId) {
        Map<String,String> data = new HashMap<>();
        String[] fields = {"title","first_name","family_name","role","email","telephone"};
        for (String field : fields) data.put(field, "");
        boolean exists = staffId > 0;
        if (exists) {
            Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM staff_members WHERE id=?", new String[]{String.valueOf(staffId)});
            try { if (cursor.moveToFirst()) for (String field : fields) data.put(field, safe(cursor, field)); }
            finally { cursor.close(); }
        }
        LinearLayout content = vbox();
        LinearLayout form = card();
        form.addView(tv(exists ? "Edit staff member" : "Add staff member", 18, textColor, Typeface.BOLD));
        final EditText title = input("Title", data.get("title"), false);
        final EditText firstName = input("Name", data.get("first_name"), false);
        final EditText familyName = input("Family name", data.get("family_name"), false);
        final EditText role = input("Role", data.get("role"), false);
        final EditText email = input("Email", data.get("email"), false);
        final EditText telephone = input("Telephone", data.get("telephone"), false);
        form.addView(title); form.addView(firstName); form.addView(familyName); form.addView(role); form.addView(email); form.addView(telephone);
        addButton(form, btn("Save staff member"), v -> {
            ContentValues cv = new ContentValues();
            cv.put("project_id", projectId);
            cv.put("title", title.getText().toString());
            cv.put("first_name", firstName.getText().toString());
            cv.put("family_name", familyName.getText().toString());
            cv.put("role", role.getText().toString());
            cv.put("position", "");
            cv.put("email", email.getText().toString());
            cv.put("telephone", telephone.getText().toString());
            if (exists) update("staff_members", cv, staffId);
            else { cv.put("created_at", nowIso()); insert("staff_members", cv); }
            skipHistoryOnce = true;
            showProjectStaff(projectId);
        });
        if (exists) addButton(form, dangerBtn("Delete"), v -> {
            delete("staff_members", staffId);
            skipHistoryOnce = true;
            showProjectStaff(projectId);
        });
        addButton(form, outlineBtn("Back"), v -> goBackOneStep());
        content.addView(form);
        setScreen(exists ? "Edit staff member" : "Add staff member", content, () -> editProjectStaff(projectId, staffId), "staff:" + projectId + ":" + staffId);
    }

    private void showProjectFiles(long projectId, String section) {
        boolean letters = section.equalsIgnoreCase("Letters");
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Project " + section.toLowerCase(Locale.US), 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Upload " + (letters ? "letter" : "document")), v -> promptProjectUpload(projectId, section));
        addButton(top, outlineBtn("Back to project"), v -> goBackOneStep());
        content.addView(top);
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM project_files WHERE project_id=? AND section_name=? ORDER BY sort_order ASC,id DESC", new String[]{String.valueOf(projectId), section});
        try {
            while (cursor.moveToNext()) {
                final long fileId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String subtitle = letters ? safe(cursor,"subject") : safe(cursor,"document_type");
                LinearLayout item = fileCard(safe(cursor,"display_name"), subtitle, safe(cursor,"uploaded_at"));
                item.setOnClickListener(v -> openStoredFile("project_files", fileId));
                item.setOnLongClickListener(v -> {
                    showProjectFileMenu(fileId, projectId, section, letters);
                    return true;
                });
                addOrderButtons(item, "project_files", fileId, "project_id=? AND section_name=?", new String[]{String.valueOf(projectId), section}, () -> showProjectFiles(projectId, section));
                content.addView(item);
            }
        } finally { cursor.close(); }
        setScreen("Project " + section, content, () -> showProjectFiles(projectId, section), "project-files:" + projectId + ":" + section);
    }

    private void promptProjectUpload(long projectId, String section) {
        boolean letters = section.equalsIgnoreCase("Letters");
        LinearLayout form = vbox();
        form.setPadding(dp(12), dp(4), dp(12), dp(4));
        EditText title = input("File title (optional)", "", false);
        EditText type = input(letters ? "Letter subject" : "Document type", "", false);
        form.addView(title); form.addView(type);
        alertBuilder()
                .setTitle("Upload project " + (letters ? "letter" : "document"))
                .setView(form)
                .setPositiveButton("Choose file", (dialog, which) -> pickFileForUpload("project", projectId, section, type.getText().toString(), title.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showProjectFileMenu(long fileId, long projectId, String section, boolean letters) {
        String[] actions = letters
                ? new String[]{"Open", "Edit subject", "Delete", "Save as"}
                : new String[]{"Open", "Edit title", "Edit file type", "Delete", "Save as"};
        alertBuilder()
                .setTitle(letters ? "Project letter" : "Project document")
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) openStoredFile("project_files", fileId);
                    else if (letters && which == 1) promptText("Edit subject", "Subject", getFileColumn("project_files", fileId, "subject"), value -> {
                        ContentValues cv = new ContentValues();
                        cv.put("subject", value);
                        update("project_files", cv, fileId);
                        showProjectFiles(projectId, section);
                    });
                    else if (letters && which == 2) confirm("Delete letter", "Delete this letter?", () -> {
                        deleteFileRecord("project_files", fileId);
                        showProjectFiles(projectId, section);
                    });
                    else if (letters && which == 3) saveFileAs("project_files", fileId);
                    else if (!letters && which == 1) promptText("Edit title", "Title", getFileColumn("project_files", fileId, "display_name"), value -> {
                        ContentValues cv = new ContentValues();
                        cv.put("display_name", value);
                        update("project_files", cv, fileId);
                        showProjectFiles(projectId, section);
                    });
                    else if (!letters && which == 2) promptText("Edit file type", "Document type", getFileColumn("project_files", fileId, "document_type"), value -> {
                        ContentValues cv = new ContentValues();
                        cv.put("document_type", value);
                        update("project_files", cv, fileId);
                        showProjectFiles(projectId, section);
                    });
                    else if (!letters && which == 3) confirm("Delete document", "Delete this document?", () -> {
                        deleteFileRecord("project_files", fileId);
                        showProjectFiles(projectId, section);
                    });
                    else if (!letters && which == 4) saveFileAs("project_files", fileId);
                })
                .show();
    }

    private void showWbs(long projectId) {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Work Breakdown Structure (WBS)", 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Add WBS item"), v -> editWbs(projectId, 0));
        addButton(top, outlineBtn("Back to project"), v -> goBackOneStep());
        content.addView(top);
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM wbs_items WHERE project_id=? ORDER BY code,id", new String[]{String.valueOf(projectId)});
        try {
            while (cursor.moveToNext()) {
                final long itemId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                LinearLayout item = card();
                item.addView(tv(safe(cursor,"code") + "  " + safe(cursor,"title"), 16, textColor, Typeface.BOLD));
                item.addView(tv("Responsible: " + safe(cursor,"responsible") + " • Deliverable: " + safe(cursor,"deliverable"), 12, mutedColor, Typeface.NORMAL));
                item.addView(tv("Progress: " + safeInt(cursor,"progress") + "% • Weight: " + safe(cursor,"weight_percent") + "%", 12, primaryColor, Typeface.BOLD));
                addButton(item, outlineBtn("Edit"), v -> editWbs(projectId, itemId));
                content.addView(item);
            }
        } finally { cursor.close(); }
        setScreen("WBS", content, () -> showWbs(projectId), "wbs:" + projectId);
    }

    private void editWbs(long projectId, long itemId) {
        Map<String,String> data = rowMap("wbs_items", itemId, new String[]{"code","title","description","responsible","deliverable","weight_percent","start_jdate","end_jdate","progress"});
        LinearLayout content = vbox();
        LinearLayout form = card();
        form.addView(tv(itemId > 0 ? "Edit WBS item" : "Add WBS item", 18, textColor, Typeface.BOLD));
        final EditText code = input("Code", data.get("code"), false);
        final EditText title = input("Title", data.get("title"), false);
        final EditText description = input("Description", data.get("description"), true);
        final EditText responsible = input("Responsible", data.get("responsible"), false);
        final EditText deliverable = input("Deliverable", data.get("deliverable"), false);
        final EditText weight = input("Weight percent", data.get("weight_percent"), false);
        form.addView(code); form.addView(title); form.addView(description); form.addView(responsible); form.addView(deliverable); form.addView(weight);
        final EditText start = dateField(form, "Start date", data.get("start_jdate"), false);
        final EditText end = dateField(form, "End date", data.get("end_jdate"), false);
        final EditText progress = input("Progress %", data.get("progress"), false);
        form.addView(progress);
        addButton(form, btn("Save WBS item"), v -> {
            ContentValues cv = new ContentValues();
            cv.put("project_id", projectId);
            cv.put("code", code.getText().toString());
            cv.put("title", title.getText().toString());
            cv.put("description", description.getText().toString());
            cv.put("responsible", responsible.getText().toString());
            cv.put("deliverable", deliverable.getText().toString());
            cv.put("weight_percent", parseDouble(weight.getText().toString()));
            cv.put("start_jdate", start.getText().toString());
            cv.put("end_jdate", end.getText().toString());
            cv.put("progress", parseInt(progress.getText().toString()));
            cv.put("completed", parseInt(progress.getText().toString()) >= 100 ? 1 : 0);
            if (itemId > 0) update("wbs_items", cv, itemId);
            else { cv.put("created_at", nowIso()); insert("wbs_items", cv); }
            skipHistoryOnce = true;
            showWbs(projectId);
        });
        if (itemId > 0) addButton(form, dangerBtn("Delete"), v -> {
            delete("wbs_items", itemId);
            skipHistoryOnce = true;
            showWbs(projectId);
        });
        addButton(form, outlineBtn("Back"), v -> goBackOneStep());
        content.addView(form);
        setScreen(itemId > 0 ? "Edit WBS item" : "Add WBS item", content, () -> editWbs(projectId, itemId), "wbs-item:" + projectId + ":" + itemId);
    }

    private void showCbs(long projectId) {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Cost Breakdown Structure (CBS)", 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Add CBS item"), v -> editCbs(projectId, 0));
        addButton(top, outlineBtn("Back to project"), v -> goBackOneStep());
        content.addView(top);
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM cbs_items WHERE project_id=? ORDER BY code,id", new String[]{String.valueOf(projectId)});
        double total = 0;
        try {
            while (cursor.moveToNext()) {
                final long itemId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                double itemTotal = cursor.getDouble(cursor.getColumnIndexOrThrow("total_cost"));
                total += itemTotal;
                LinearLayout item = card();
                item.addView(tv(safe(cursor,"code") + "  " + safe(cursor,"cost_item"), 16, textColor, Typeface.BOLD));
                item.addView(tv("Category: " + safe(cursor,"category") + " • " + safe(cursor,"quantity") + " " + safe(cursor,"unit") + " × " + safe(cursor,"unit_cost"), 12, mutedColor, Typeface.NORMAL));
                item.addView(tv("Total: " + itemTotal, 13, primaryColor, Typeface.BOLD));
                addButton(item, outlineBtn("Edit"), v -> editCbs(projectId, itemId));
                content.addView(item);
            }
        } finally { cursor.close(); }
        LinearLayout totalCard = card();
        totalCard.addView(tv("Project CBS total: " + total, 16, accentColor, Typeface.BOLD));
        content.addView(totalCard);
        setScreen("CBS", content, () -> showCbs(projectId), "cbs:" + projectId);
    }

    private void editCbs(long projectId, long itemId) {
        Map<String,String> data = rowMap("cbs_items", itemId, new String[]{"code","cost_item","category","unit","quantity","unit_cost"});
        LinearLayout content = vbox();
        LinearLayout form = card();
        form.addView(tv(itemId > 0 ? "Edit CBS item" : "Add CBS item", 18, textColor, Typeface.BOLD));
        final EditText code = input("Code", data.get("code"), false);
        final EditText costItem = input("Cost item", data.get("cost_item"), false);
        final EditText category = input("Category", data.get("category"), false);
        final EditText unit = input("Unit", data.get("unit"), false);
        final EditText quantity = input("Quantity", data.get("quantity"), false);
        final EditText unitCost = input("Unit cost", data.get("unit_cost"), false);
        form.addView(code); form.addView(costItem); form.addView(category); form.addView(unit); form.addView(quantity); form.addView(unitCost);
        addButton(form, btn("Save CBS item"), v -> {
            double q = parseDouble(quantity.getText().toString());
            double uc = parseDouble(unitCost.getText().toString());
            ContentValues cv = new ContentValues();
            cv.put("project_id", projectId);
            cv.put("code", code.getText().toString());
            cv.put("cost_item", costItem.getText().toString());
            cv.put("category", category.getText().toString());
            cv.put("unit", unit.getText().toString());
            cv.put("quantity", q);
            cv.put("unit_cost", uc);
            cv.put("total_cost", q * uc);
            if (itemId > 0) update("cbs_items", cv, itemId);
            else { cv.put("created_at", nowIso()); insert("cbs_items", cv); }
            skipHistoryOnce = true;
            showCbs(projectId);
        });
        if (itemId > 0) addButton(form, dangerBtn("Delete"), v -> {
            delete("cbs_items", itemId);
            skipHistoryOnce = true;
            showCbs(projectId);
        });
        addButton(form, outlineBtn("Back"), v -> goBackOneStep());
        content.addView(form);
        setScreen(itemId > 0 ? "Edit CBS item" : "Add CBS item", content, () -> editCbs(projectId, itemId), "cbs-item:" + projectId + ":" + itemId);
    }

    private void showSCurve(long projectId) {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("S-curve data", 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Add month"), v -> editSCurvePoint(projectId, 0));
        addButton(top, outlineBtn("Back to project"), v -> goBackOneStep());
        content.addView(top);
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM scurve_points WHERE project_id=? ORDER BY month_no", new String[]{String.valueOf(projectId)});
        try {
            while (cursor.moveToNext()) {
                final long pointId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                LinearLayout item = card();
                item.addView(tv("Month " + safeInt(cursor,"month_no") + " — " + safe(cursor,"month_label"), 16, textColor, Typeface.BOLD));
                item.addView(tv("Plan: " + safe(cursor,"plan_progress") + "% • Actual: " + safe(cursor,"actual_progress") + "%", 13, primaryColor, Typeface.BOLD));
                addButton(item, outlineBtn("Edit"), v -> editSCurvePoint(projectId, pointId));
                content.addView(item);
            }
        } finally { cursor.close(); }
        setScreen("S-curve data", content, () -> showSCurve(projectId), "s-curve:" + projectId);
    }

    private void editSCurvePoint(long projectId, long pointId) {
        Map<String,String> data = rowMap("scurve_points", pointId, new String[]{"month_no","month_label","plan_progress","actual_progress"});
        LinearLayout content = vbox();
        LinearLayout form = card();
        form.addView(tv(pointId > 0 ? "Edit S-curve point" : "Add S-curve point", 18, textColor, Typeface.BOLD));
        final EditText monthNo = input("Month number", data.get("month_no"), false);
        final EditText label = input("Month label", data.get("month_label"), false);
        final EditText plan = input("Plan progress %", data.get("plan_progress"), false);
        final EditText actual = input("Actual progress %", data.get("actual_progress"), false);
        form.addView(monthNo); form.addView(label); form.addView(plan); form.addView(actual);
        addButton(form, btn("Save"), v -> {
            ContentValues cv = new ContentValues();
            cv.put("project_id", projectId);
            cv.put("month_no", parseInt(monthNo.getText().toString()));
            cv.put("month_label", label.getText().toString());
            cv.put("plan_progress", parseDouble(plan.getText().toString()));
            cv.put("actual_progress", parseDouble(actual.getText().toString()));
            if (pointId > 0) update("scurve_points", cv, pointId);
            else insert("scurve_points", cv);
            skipHistoryOnce = true;
            showSCurve(projectId);
        });
        if (pointId > 0) addButton(form, dangerBtn("Delete"), v -> {
            delete("scurve_points", pointId);
            skipHistoryOnce = true;
            showSCurve(projectId);
        });
        addButton(form, outlineBtn("Back"), v -> goBackOneStep());
        content.addView(form);
        setScreen(pointId > 0 ? "Edit S-curve point" : "Add S-curve point", content, () -> editSCurvePoint(projectId, pointId), "s-curve-point:" + projectId + ":" + pointId);
    }

    private void showGantt(long projectId) {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Gantt tasks", 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Add Gantt task"), v -> editGantt(projectId, 0));
        addButton(top, outlineBtn("Back to project"), v -> goBackOneStep());
        content.addView(top);
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM gantt_tasks WHERE project_id=? ORDER BY start_iso,id", new String[]{String.valueOf(projectId)});
        try {
            while (cursor.moveToNext()) {
                final long taskId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                LinearLayout item = card();
                item.addView(tv(safe(cursor,"task_name"), 16, textColor, Typeface.BOLD));
                item.addView(tv(safe(cursor,"start_jdate") + " → " + safe(cursor,"end_jdate") + " • " + safeInt(cursor,"progress") + "%", 13, primaryColor, Typeface.BOLD));
                addButton(item, outlineBtn("Edit"), v -> editGantt(projectId, taskId));
                content.addView(item);
            }
        } finally { cursor.close(); }
        setScreen("Gantt tasks", content, () -> showGantt(projectId), "gantt:" + projectId);
    }

    private void editGantt(long projectId, long taskId) {
        Map<String,String> data = rowMap("gantt_tasks", taskId, new String[]{"task_name","start_jdate","end_jdate","progress"});
        LinearLayout content = vbox();
        LinearLayout form = card();
        form.addView(tv(taskId > 0 ? "Edit Gantt task" : "Add Gantt task", 18, textColor, Typeface.BOLD));
        final EditText name = input("Task name", data.get("task_name"), false);
        form.addView(name);
        final EditText start = dateField(form, "Start date", data.get("start_jdate"), false);
        final EditText end = dateField(form, "End date", data.get("end_jdate"), false);
        final EditText progress = input("Progress %", data.get("progress"), false);
        form.addView(progress);
        addButton(form, btn("Save"), v -> {
            ContentValues cv = new ContentValues();
            cv.put("project_id", projectId);
            cv.put("task_name", name.getText().toString());
            cv.put("start_jdate", start.getText().toString());
            cv.put("end_jdate", end.getText().toString());
            cv.put("start_iso", Jalali.toIso(start.getText().toString(), "00:00"));
            cv.put("end_iso", Jalali.toIso(end.getText().toString(), "23:59"));
            cv.put("progress", parseInt(progress.getText().toString()));
            if (taskId > 0) update("gantt_tasks", cv, taskId);
            else insert("gantt_tasks", cv);
            skipHistoryOnce = true;
            showGantt(projectId);
        });
        if (taskId > 0) addButton(form, dangerBtn("Delete"), v -> {
            delete("gantt_tasks", taskId);
            skipHistoryOnce = true;
            showGantt(projectId);
        });
        addButton(form, outlineBtn("Back"), v -> goBackOneStep());
        content.addView(form);
        setScreen(taskId > 0 ? "Edit Gantt task" : "Add Gantt task", content, () -> editGantt(projectId, taskId), "gantt-task:" + projectId + ":" + taskId);
    }

    // ---------------- Courses ----------------

    private void showCourses() {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Courses", 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Add course"), v -> editCourse(0));
        content.addView(top);
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT id,level,course_title,course_code,semester,instructor FROM courses ORDER BY semester,course_title", null);
        try {
            if (!cursor.moveToFirst()) {
                LinearLayout empty = card();
                empty.addView(tv("No courses yet.", 14, mutedColor, Typeface.NORMAL));
                content.addView(empty);
            } else {
                do {
                    final long id = cursor.getLong(0);
                    LinearLayout item = card();
                    item.addView(tv(safe(cursor,"course_title"), 17, textColor, Typeface.BOLD));
                    item.addView(tv(canonicalLevel(safe(cursor,"level")) + " • " + safe(cursor,"course_code") + " • " + safe(cursor,"semester"), 13, primaryColor, Typeface.BOLD));
                    item.addView(tv("Instructor: " + safe(cursor,"instructor"), 13, mutedColor, Typeface.NORMAL));
                    addButton(item, outlineBtn("Open course"), v -> editCourse(id));
                    content.addView(item);
                } while (cursor.moveToNext());
            }
        } finally { cursor.close(); }
        setScreen("Courses", content, this::showCourses, "courses");
    }

    private void editCourse(long id) {
        Map<String,String> data = rowMap("courses", id, new String[]{"level","course_title","course_code","semester","instructor","start_date","end_date","notes"});
        if (data.get("level").isEmpty()) data.put("level", "M.S.c");
        boolean exists = id > 0;
        LinearLayout content = vbox();
        LinearLayout form = card();
        form.addView(tv(exists ? "Course page" : "Add course", 18, textColor, Typeface.BOLD));
        form.addView(tv("Level", 14, textColor, Typeface.BOLD));
        final Spinner level = spinner(new String[]{"B.S.c","M.S.c","P.h.D"}, canonicalLevel(data.get("level")));
        form.addView(level);
        final EditText title = input("Course title", data.get("course_title"), false);
        final EditText code = input("Course code", data.get("course_code"), false);
        final EditText semester = input("Semester", data.get("semester"), false);
        final EditText instructor = input("Instructor", data.get("instructor"), false);
        form.addView(title); form.addView(code); form.addView(semester); form.addView(instructor);
        final EditText start = dateField(form, "Start date", data.get("start_date"), false);
        final EditText end = dateField(form, "End date", data.get("end_date"), false);
        final EditText notes = input("Notes", data.get("notes"), true);
        form.addView(notes);
        addButton(form, btn("Save course"), v -> {
            if (title.getText().toString().trim().isEmpty()) { toast("Enter course title."); return; }
            ContentValues cv = new ContentValues();
            cv.put("level", level.getSelectedItem().toString());
            cv.put("course_title", title.getText().toString());
            cv.put("course_code", code.getText().toString());
            cv.put("semester", semester.getText().toString());
            cv.put("instructor", instructor.getText().toString());
            cv.put("start_date", start.getText().toString());
            cv.put("end_date", end.getText().toString());
            cv.put("notes", notes.getText().toString());
            long courseId;
            if (exists) { update("courses", cv, id); courseId = id; }
            else { cv.put("created_at", nowIso()); courseId = insert("courses", cv); }
            editCourse(courseId);
        });
        if (exists) {
            addButton(form, outlineBtn("Course documents"), v -> showCourseFiles(id));
            addButton(form, dangerBtn("Delete course"), v -> {
                delete("courses", id);
                skipHistoryOnce = true;
                showCourses();
            });
        }
        addButton(form, outlineBtn("Back"), v -> goBackOneStep());
        content.addView(form);
        setScreen(exists ? "Course page" : "Add course", content, () -> editCourse(id), "course:" + id);
    }

    private void showCourseFiles(long courseId) {
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("Course documents", 18, textColor, Typeface.BOLD));
        addButton(top, btn("+ Upload document"), v -> promptCourseUpload(courseId));
        addButton(top, outlineBtn("Back to course"), v -> goBackOneStep());
        content.addView(top);
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM course_files WHERE course_id=? ORDER BY sort_order ASC,id DESC", new String[]{String.valueOf(courseId)});
        try {
            while (cursor.moveToNext()) {
                final long fileId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                LinearLayout item = fileCard(safe(cursor,"display_name"), safe(cursor,"document_type"), safe(cursor,"uploaded_at"));
                item.setOnClickListener(v -> openStoredFile("course_files", fileId));
                item.setOnLongClickListener(v -> {
                    showSimpleFileMenu("course_files", fileId, () -> showCourseFiles(courseId));
                    return true;
                });
                addOrderButtons(item, "course_files", fileId, "course_id=?", new String[]{String.valueOf(courseId)}, () -> showCourseFiles(courseId));
                content.addView(item);
            }
        } finally { cursor.close(); }
        setScreen("Course documents", content, () -> showCourseFiles(courseId), "course-files:" + courseId);
    }

    private void promptCourseUpload(long courseId) {
        LinearLayout form = vbox();
        form.setPadding(dp(12), dp(4), dp(12), dp(4));
        EditText type = input("Document type", "", false);
        EditText title = input("Title (optional)", "", false);
        form.addView(type); form.addView(title);
        alertBuilder()
                .setTitle("Upload course document")
                .setView(form)
                .setPositiveButton("Choose file", (dialog, which) -> pickFileForUpload("course", courseId, "", type.getText().toString(), title.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------- File Inventory ----------------

    private long fileInventorySheetId() {
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT id FROM custom_sheets WHERE title='File Inventory' ORDER BY id LIMIT 1", null);
        try {
            if (cursor.moveToFirst()) return cursor.getLong(0);
        } finally { cursor.close(); }
        ContentValues cv = new ContentValues();
        cv.put("title", "File Inventory");
        cv.put("color", "#0891B2");
        cv.put("created_at", nowIso());
        return insert("custom_sheets", cv);
    }

    private List<String> inventoryFolders() {
        List<String> folders = new ArrayList<>();
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT value FROM app_settings WHERE key LIKE 'inventory_folder_%' AND key<>'inventory_folder_order' ORDER BY value COLLATE NOCASE", null);
        try {
            while (cursor.moveToNext()) {
                String value = cursor.getString(0);
                if (value != null && !value.trim().isEmpty() && !folders.contains(value.trim())) folders.add(value.trim());
            }
        } finally { cursor.close(); }
        String order = getSetting("inventory_folder_order", "");
        if (!order.isEmpty()) {
            List<String> ordered = new ArrayList<>();
            for (String value : order.split("\n")) if (folders.contains(value) && !ordered.contains(value)) ordered.add(value);
            for (String value : folders) if (!ordered.contains(value)) ordered.add(value);
            folders = ordered;
        }
        return folders;
    }

    private void saveInventoryFolderOrder(List<String> folders) {
        setSetting("inventory_folder_order", joinLines(folders));
    }

    private void showFileInventory() {
        List<String> folders = inventoryFolders();
        if (pendingInventoryFolder.isEmpty() || !folders.contains(pendingInventoryFolder)) pendingInventoryFolder = folders.isEmpty() ? "" : folders.get(0);
        LinearLayout content = vbox();
        LinearLayout top = card();
        top.addView(tv("File Inventory", 18, textColor, Typeface.BOLD));
        top.addView(tv("Create a folder first, select it, then add documents.", 13, mutedColor, Typeface.NORMAL));
        addButton(top, btn("+ Create folder"), v -> promptText("Create folder", "Folder name", "", value -> {
            if (value.isEmpty()) return;
            if (inventoryFolders().contains(value)) { toast("Folder already exists."); return; }
            setSetting("inventory_folder_" + safeName(value), value);
            List<String> newOrder = inventoryFolders();
            if (!newOrder.contains(value)) newOrder.add(value);
            saveInventoryFolderOrder(newOrder);
            pendingInventoryFolder = value;
            showFileInventory();
        }));
        content.addView(top);

        LinearLayout foldersCard = card();
        foldersCard.addView(tv("Folders", 16, textColor, Typeface.BOLD));
        for (int i = 0; i < folders.size(); i++) {
            final int index = i;
            final String folder = folders.get(i);
            LinearLayout row = hbox();
            Button folderButton = outlineBtn((folder.equals(pendingInventoryFolder) ? "✓ " : "") + folder);
            folderButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            folderButton.setOnClickListener(v -> {
                pendingInventoryFolder = folder;
                showFileInventory();
            });
            folderButton.setOnLongClickListener(v -> {
                showInventoryFolderMenu(folder);
                return true;
            });
            row.addView(folderButton, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            Button up = outlineBtn("▲");
            up.setEnabled(index > 0);
            up.setOnClickListener(v -> {
                List<String> current = inventoryFolders();
                if (index > 0) {
                    String moved = current.remove(index);
                    current.add(index - 1, moved);
                    saveInventoryFolderOrder(current);
                    showFileInventory();
                }
            });
            Button down = outlineBtn("▼");
            down.setEnabled(index < folders.size() - 1);
            down.setOnClickListener(v -> {
                List<String> current = inventoryFolders();
                if (index < current.size() - 1) {
                    String moved = current.remove(index);
                    current.add(index + 1, moved);
                    saveInventoryFolderOrder(current);
                    showFileInventory();
                }
            });
            row.addView(up, new LinearLayout.LayoutParams(dp(54), ViewGroup.LayoutParams.WRAP_CONTENT));
            row.addView(down, new LinearLayout.LayoutParams(dp(54), ViewGroup.LayoutParams.WRAP_CONTENT));
            foldersCard.addView(row);
        }
        content.addView(foldersCard);

        LinearLayout filesCard = card();
        filesCard.addView(tv(pendingInventoryFolder.isEmpty() ? "Select or create a folder" : "Documents in: " + pendingInventoryFolder, 16, textColor, Typeface.BOLD));
        if (!pendingInventoryFolder.isEmpty()) addButton(filesCard, btn("+ Add file"), v -> promptInventoryUpload(pendingInventoryFolder));
        long sheetId = fileInventorySheetId();
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM custom_sheet_files WHERE sheet_id=? AND document_type=? ORDER BY sort_order ASC,id DESC", new String[]{String.valueOf(sheetId), pendingInventoryFolder});
        try {
            while (cursor.moveToNext()) {
                final long fileId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                LinearLayout item = fileCard(safe(cursor,"display_name"), "", safe(cursor,"uploaded_at"));
                item.setOnClickListener(v -> openStoredFile("custom_sheet_files", fileId));
                item.setOnLongClickListener(v -> {
                    showSimpleFileMenu("custom_sheet_files", fileId, this::showFileInventory);
                    return true;
                });
                addOrderButtons(item, "custom_sheet_files", fileId, "sheet_id=? AND document_type=?", new String[]{String.valueOf(sheetId), pendingInventoryFolder}, this::showFileInventory);
                filesCard.addView(item);
            }
        } finally { cursor.close(); }
        content.addView(filesCard);
        setScreen("File Inventory", content, this::showFileInventory, "file-inventory");
    }

    private void showInventoryFolderMenu(String folder) {
        String[] actions = {"Delete", "Save the folder as", "Rename"};
        alertBuilder()
                .setTitle(folder)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) confirm("Delete folder", "Delete this folder and all documents inside it?", () -> deleteInventoryFolder(folder));
                    else if (which == 1) exportInventoryFolder(folder);
                    else if (which == 2) promptText("Rename folder", "New folder name", folder, value -> renameInventoryFolder(folder, value));
                })
                .show();
    }

    private void deleteInventoryFolder(String folder) {
        long sheetId = fileInventorySheetId();
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT id FROM custom_sheet_files WHERE sheet_id=? AND document_type=?", new String[]{String.valueOf(sheetId), folder});
        List<Long> ids = new ArrayList<>();
        try { while (cursor.moveToNext()) ids.add(cursor.getLong(0)); }
        finally { cursor.close(); }
        for (Long id : ids) deleteFileRecord("custom_sheet_files", id);
        db.getWritableDatabase().delete("app_settings", "key=?", new String[]{"inventory_folder_" + safeName(folder)});
        List<String> folders = inventoryFolders();
        folders.remove(folder);
        saveInventoryFolderOrder(folders);
        pendingInventoryFolder = folders.isEmpty() ? "" : folders.get(0);
        showFileInventory();
    }

    private void renameInventoryFolder(String oldName, String newName) {
        if (newName == null || newName.trim().isEmpty()) return;
        String clean = newName.trim();
        if (!oldName.equalsIgnoreCase(clean) && inventoryFolders().contains(clean)) { toast("Folder already exists."); return; }
        long sheetId = fileInventorySheetId();
        ContentValues cv = new ContentValues();
        cv.put("document_type", clean);
        db.getWritableDatabase().update("custom_sheet_files", cv, "sheet_id=? AND document_type=?", new String[]{String.valueOf(sheetId), oldName});
        db.getWritableDatabase().delete("app_settings", "key=?", new String[]{"inventory_folder_" + safeName(oldName)});
        setSetting("inventory_folder_" + safeName(clean), clean);
        List<String> folders = inventoryFolders();
        for (int i = 0; i < folders.size(); i++) if (folders.get(i).equals(oldName)) folders.set(i, clean);
        saveInventoryFolderOrder(folders);
        pendingInventoryFolder = clean;
        showFileInventory();
    }

    private void exportInventoryFolder(String folder) {
        try {
            long sheetId = fileInventorySheetId();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                Cursor cursor = db.getReadableDatabase().rawQuery("SELECT display_name,stored_path FROM custom_sheet_files WHERE sheet_id=? AND document_type=? ORDER BY sort_order ASC,id DESC", new String[]{String.valueOf(sheetId), folder});
                try {
                    int count = 1;
                    while (cursor.moveToNext()) {
                        File file = new File(cursor.getString(1));
                        if (!file.isFile()) continue;
                        String title = cursor.getString(0);
                        String extension = extension(file.getName());
                        String name = safeName(title.isEmpty() ? "file_" + count : title);
                        if (!extension.isEmpty() && !name.toLowerCase(Locale.US).endsWith(extension.toLowerCase(Locale.US))) name += extension;
                        zip.putNextEntry(new ZipEntry(name));
                        try (InputStream in = new FileInputStream(file)) {
                            byte[] buffer = new byte[8192];
                            int n;
                            while ((n = in.read(buffer)) >= 0) zip.write(buffer, 0, n);
                        }
                        zip.closeEntry();
                        count++;
                    }
                } finally { cursor.close(); }
            }
            pendingExportBytes = bytes.toByteArray();
            pendingExportMessage = "Inventory folder exported as ZIP.";
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            intent.putExtra(Intent.EXTRA_TITLE, safeName(folder) + ".zip");
            startActivityForResult(intent, REQ_EXPORT_FOLDER_ZIP);
        } catch (Exception ex) {
            toast("Cannot export folder: " + ex.getMessage());
        }
    }

    private void promptInventoryUpload(String folder) {
        EditText title = input("Document title (optional)", "", false);
        alertBuilder()
                .setTitle("Add file to " + folder)
                .setView(title)
                .setPositiveButton("Choose file", (dialog, which) -> pickFileForUpload("inventory", fileInventorySheetId(), "", folder, title.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------- Generic file operations ----------------

    private LinearLayout fileCard(String title, String type, String uploaded) {
        LinearLayout item = vbox();
        item.setPadding(dp(10), dp(10), dp(10), dp(10));
        item.setBackground(rounded(panel2Color, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(5), 0, dp(5));
        item.setLayoutParams(lp);
        item.addView(tv(title == null || title.isEmpty() ? "Untitled file" : title, 16, textColor, Typeface.BOLD));
        if (type != null && !type.isEmpty()) item.addView(tv(type, 13, primaryColor, Typeface.BOLD));
        if (uploaded != null && !uploaded.isEmpty()) item.addView(tv("Uploaded: " + uploaded, 11, mutedColor, Typeface.NORMAL));
        item.addView(tv("Tap to open • Long press for options", 11, mutedColor, Typeface.NORMAL));
        return item;
    }

    private void pickFileForUpload(String kind, long parentId, String section, String documentType, String title) {
        pendingUploadKind = kind;
        pendingParentId = parentId;
        pendingSection = section == null ? "" : section;
        pendingDocumentType = documentType == null ? "" : documentType;
        pendingTitle = title == null ? "" : title;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQ_UPLOAD_FILE);
    }

    private void handleUploadedFile(Uri uri) throws Exception {
        String originalName = displayName(uri);
        String title = pendingTitle.trim().isEmpty() ? stripExtension(originalName) : pendingTitle.trim();
        String folder = pendingUploadKind + "_" + pendingParentId;
        if (pendingUploadKind.equals("project")) folder += "_" + safeName(pendingSection);
        File destinationFolder = new File(getFilesDir(), "assistant_files/" + folder);
        if (!destinationFolder.exists() && !destinationFolder.mkdirs()) throw new Exception("Cannot create file storage folder.");
        String uniqueName = System.currentTimeMillis() + "_" + safeName(originalName);
        File destination = new File(destinationFolder, uniqueName);
        try (InputStream in = getContentResolver().openInputStream(uri); OutputStream out = new FileOutputStream(destination)) {
            if (in == null) throw new Exception("Cannot open selected file.");
            copyStream(in, out);
        }
        ContentValues cv = new ContentValues();
        cv.put("display_name", title);
        cv.put("stored_path", destination.getAbsolutePath());
        cv.put("original_path", uri.toString());
        cv.put("uploaded_at", nowIso());
        if (pendingUploadKind.equals("student")) {
            cv.put("student_id", pendingParentId);
            cv.put("document_type", pendingDocumentType);
            cv.put("sort_order", nextSortOrder("student_files", "student_id=?", new String[]{String.valueOf(pendingParentId)}));
            insert("student_files", cv);
            showStudentDocuments(pendingParentId);
        } else if (pendingUploadKind.equals("project")) {
            cv.put("project_id", pendingParentId);
            cv.put("section_name", pendingSection);
            if (pendingSection.equalsIgnoreCase("Letters")) cv.put("subject", pendingDocumentType);
            else cv.put("document_type", pendingDocumentType);
            cv.put("sort_order", nextSortOrder("project_files", "project_id=? AND section_name=?", new String[]{String.valueOf(pendingParentId), pendingSection}));
            insert("project_files", cv);
            showProjectFiles(pendingParentId, pendingSection);
        } else if (pendingUploadKind.equals("course")) {
            cv.put("course_id", pendingParentId);
            cv.put("document_type", pendingDocumentType);
            cv.put("sort_order", nextSortOrder("course_files", "course_id=?", new String[]{String.valueOf(pendingParentId)}));
            insert("course_files", cv);
            showCourseFiles(pendingParentId);
        } else if (pendingUploadKind.equals("inventory")) {
            cv.put("sheet_id", pendingParentId);
            cv.put("document_type", pendingDocumentType);
            cv.put("sort_order", nextSortOrder("custom_sheet_files", "sheet_id=? AND document_type=?", new String[]{String.valueOf(pendingParentId), pendingDocumentType}));
            insert("custom_sheet_files", cv);
            pendingInventoryFolder = pendingDocumentType;
            showFileInventory();
        }
        pendingUploadKind = "";
    }

    private int nextSortOrder(String table, String where, String[] args) {
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT COALESCE(MAX(sort_order),-1)+1 FROM " + table + " WHERE " + where, args);
        try { return cursor.moveToFirst() ? cursor.getInt(0) : 0; }
        finally { cursor.close(); }
    }

    private void addOrderButtons(LinearLayout item, String table, long id, String where, String[] args, Runnable refresh) {
        LinearLayout row = hbox();
        Button up = outlineBtn("Move up ▲");
        Button down = outlineBtn("Move down ▼");
        up.setOnClickListener(v -> {
            moveFile(table, id, where, args, -1);
            refresh.run();
        });
        down.setOnClickListener(v -> {
            moveFile(table, id, where, args, 1);
            refresh.run();
        });
        row.addView(up, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(down, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        item.addView(row);
    }

    private void moveFile(String table, long id, String where, String[] args, int direction) {
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT id FROM " + table + " WHERE " + where + " ORDER BY sort_order ASC,id DESC", args);
        List<Long> ids = new ArrayList<>();
        try { while (cursor.moveToNext()) ids.add(cursor.getLong(0)); }
        finally { cursor.close(); }
        int index = ids.indexOf(id);
        int target = index + direction;
        if (index < 0 || target < 0 || target >= ids.size()) return;
        Long moved = ids.remove(index);
        ids.add(target, moved);
        SQLiteDatabase database = db.getWritableDatabase();
        database.beginTransaction();
        try {
            for (int i = 0; i < ids.size(); i++) {
                ContentValues cv = new ContentValues();
                cv.put("sort_order", i);
                database.update(table, cv, "id=?", new String[]{String.valueOf(ids.get(i))});
            }
            database.setTransactionSuccessful();
        } finally { database.endTransaction(); }
    }

    private void showSimpleFileMenu(String table, long fileId, Runnable refresh) {
        String[] actions = {"Open", "Edit title", "Edit file type", "Delete", "Save as"};
        alertBuilder()
                .setTitle("File options")
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) openStoredFile(table, fileId);
                    else if (which == 1) promptText("Edit title", "Title", getFileColumn(table, fileId, "display_name"), value -> {
                        ContentValues cv = new ContentValues();
                        cv.put("display_name", value);
                        update(table, cv, fileId);
                        refresh.run();
                    });
                    else if (which == 2) promptText("Edit file type", "File type", getFileColumn(table, fileId, "document_type"), value -> {
                        ContentValues cv = new ContentValues();
                        cv.put("document_type", value);
                        update(table, cv, fileId);
                        refresh.run();
                    });
                    else if (which == 3) confirm("Delete file", "Delete this file?", () -> {
                        deleteFileRecord(table, fileId);
                        refresh.run();
                    });
                    else if (which == 4) saveFileAs(table, fileId);
                })
                .show();
    }

    private String getFileColumn(String table, long id, String column) {
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT " + column + " FROM " + table + " WHERE id=?", new String[]{String.valueOf(id)});
        try { return cursor.moveToFirst() && !cursor.isNull(0) ? cursor.getString(0) : ""; }
        finally { cursor.close(); }
    }

    private void openStoredFile(String table, long id) {
        String path = getFileColumn(table, id, "stored_path");
        File file = new File(path);
        if (!file.isFile()) { toast("File is not available on this phone. Upload or replace it on Android."); return; }
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", file);
            String mime = mimeFor(file.getName());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open file"));
        } catch (Exception ex) {
            toast("No app can open this file: " + ex.getMessage());
        }
    }

    private void saveFileAs(String table, long id) {
        String path = getFileColumn(table, id, "stored_path");
        File file = new File(path);
        if (!file.isFile()) { toast("File is not available on this phone."); return; }
        pendingSourcePath = path;
        String title = getFileColumn(table, id, "display_name");
        String extension = extension(file.getName());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeFor(file.getName()));
        intent.putExtra(Intent.EXTRA_TITLE, safeName(title.isEmpty() ? stripExtension(file.getName()) : title) + extension);
        startActivityForResult(intent, REQ_SAVE_FILE_AS);
    }

    private void copyPathToUri(String path, Uri uri) throws Exception {
        try (InputStream in = new FileInputStream(path); OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) throw new Exception("Cannot save selected file.");
            copyStream(in, out);
        }
    }

    private void deleteFileRecord(String table, long id) {
        String path = getFileColumn(table, id, "stored_path");
        delete(table, id);
        if (!path.isEmpty()) new File(path).delete();
    }

    private String displayName(Uri uri) {
        String result = "";
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (index >= 0) result = cursor.getString(index);
                    }
                } finally { cursor.close(); }
            }
        }
        if (result == null || result.trim().isEmpty()) result = uri.getLastPathSegment();
        return result == null ? "file" : result;
    }

    private String mimeFor(String fileName) {
        String extension = extension(fileName).replace(".", "").toLowerCase(Locale.US);
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return mime == null ? "application/octet-stream" : mime;
    }

    private String extension(String name) {
        int index = name.lastIndexOf('.');
        return index >= 0 ? name.substring(index) : "";
    }

    private String stripExtension(String name) {
        int index = name.lastIndexOf('.');
        return index > 0 ? name.substring(0, index) : name;
    }

    private String safeName(String text) {
        String clean = text == null ? "file" : text.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        clean = clean.replaceAll("\\s+", " ");
        return clean.isEmpty() ? "file" : clean;
    }

    // ---------------- Data helpers ----------------

    private Map<String,String> rowMap(String table, long id, String[] columns) {
        Map<String,String> data = new LinkedHashMap<>();
        for (String column : columns) data.put(column, "");
        if (id <= 0) return data;
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT * FROM " + table + " WHERE id=?", new String[]{String.valueOf(id)});
        try {
            if (cursor.moveToFirst()) for (String column : columns) data.put(column, safe(cursor, column));
        } finally { cursor.close(); }
        return data;
    }

    private int parseInt(String value) {
        try { return Integer.parseInt(Jalali.normalizeDigits(value)); }
        catch (Exception ex) { return 0; }
    }

    private double parseDouble(String value) {
        try { return Double.parseDouble(Jalali.normalizeDigits(value)); }
        catch (Exception ex) { return 0.0; }
    }

    private String getSetting(String key, String defaultValue) {
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT value FROM app_settings WHERE key=?", new String[]{key});
        try { return cursor.moveToFirst() ? cursor.getString(0) : defaultValue; }
        finally { cursor.close(); }
    }

    private void setSetting(String key, String value) {
        ContentValues cv = new ContentValues();
        cv.put("key", key);
        cv.put("value", value == null ? "" : value);
        db.getWritableDatabase().insertWithOnConflict("app_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private String joinLines(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) builder.append('\n');
            builder.append(value);
        }
        return builder.toString();
    }
}
