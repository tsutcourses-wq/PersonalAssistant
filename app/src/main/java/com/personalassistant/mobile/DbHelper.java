package com.personalassistant.mobile;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "assistant_data.db";
    private static final int DB_VERSION = 3820;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
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

    public static void ensureSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS projects (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, status TEXT DEFAULT 'Not started', progress INTEGER DEFAULT 0, start_jdate TEXT DEFAULT '', end_jdate TEXT DEFAULT '', principal_investigator TEXT DEFAULT '', contractor TEXT DEFAULT '', notes TEXT DEFAULT '', created_at TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS project_sections (project_id INTEGER NOT NULL, section_name TEXT NOT NULL, content TEXT DEFAULT '', PRIMARY KEY (project_id, section_name), FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS reminders (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, details TEXT DEFAULT '', project_id INTEGER, remind_at_iso TEXT NOT NULL, jdate TEXT NOT NULL, time_text TEXT NOT NULL, created_at TEXT NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE SET NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS reminder_alerts (id INTEGER PRIMARY KEY AUTOINCREMENT, reminder_id INTEGER NOT NULL, alert_at_iso TEXT NOT NULL, alert_type TEXT NOT NULL, fired INTEGER DEFAULT 0, UNIQUE(reminder_id, alert_type), FOREIGN KEY(reminder_id) REFERENCES reminders(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, done INTEGER DEFAULT 0, project_id INTEGER, due_iso TEXT DEFAULT '', due_jdate TEXT DEFAULT '', responsible TEXT DEFAULT '', details TEXT DEFAULT '', created_at TEXT NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE SET NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS todo_alerts (id INTEGER PRIMARY KEY AUTOINCREMENT, todo_id INTEGER NOT NULL, alert_at_iso TEXT NOT NULL, alert_type TEXT NOT NULL, fired INTEGER DEFAULT 0, UNIQUE(todo_id, alert_type), FOREIGN KEY(todo_id) REFERENCES todos(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS staff_members (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, first_name TEXT DEFAULT '', family_name TEXT DEFAULT '', role TEXT DEFAULT '', title TEXT DEFAULT '', position TEXT DEFAULT '', email TEXT DEFAULT '', telephone TEXT DEFAULT '', created_at TEXT NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS wbs_items (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, code TEXT DEFAULT '', title TEXT DEFAULT '', description TEXT DEFAULT '', responsible TEXT DEFAULT '', deliverable TEXT DEFAULT '', weight_percent REAL DEFAULT 0, completed INTEGER DEFAULT 0, start_month INTEGER DEFAULT 1, end_month INTEGER DEFAULT 1, start_jdate TEXT DEFAULT '', end_jdate TEXT DEFAULT '', progress INTEGER DEFAULT 0, created_at TEXT NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS cbs_items (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, code TEXT DEFAULT '', cost_item TEXT DEFAULT '', category TEXT DEFAULT '', unit TEXT DEFAULT '', quantity REAL DEFAULT 0, unit_cost REAL DEFAULT 0, total_cost REAL DEFAULT 0, created_at TEXT NOT NULL, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS app_settings (key TEXT PRIMARY KEY, value TEXT DEFAULT '')");
        db.execSQL("CREATE TABLE IF NOT EXISTS dashboard_notes (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, body TEXT DEFAULT '', created_at TEXT NOT NULL, updated_at TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS project_files (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, section_name TEXT NOT NULL, display_name TEXT NOT NULL, subject TEXT DEFAULT '', document_type TEXT DEFAULT '', stored_path TEXT NOT NULL, original_path TEXT DEFAULT '', uploaded_at TEXT NOT NULL, sort_order INTEGER DEFAULT 0, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS students (id INTEGER PRIMARY KEY AUTOINCREMENT, level TEXT NOT NULL, first_name TEXT DEFAULT '', family_name TEXT DEFAULT '', student_no TEXT DEFAULT '', national_id TEXT DEFAULT '', registration_date TEXT DEFAULT '', registration_semester TEXT DEFAULT '', email TEXT DEFAULT '', telephone TEXT DEFAULT '', supervisor TEXT DEFAULT '', second_supervisor TEXT DEFAULT '', advisor TEXT DEFAULT '', referee TEXT DEFAULT '', bsc_project_form_date TEXT DEFAULT '', status TEXT DEFAULT '', thesis_status TEXT DEFAULT '', research_background TEXT DEFAULT '', seminar TEXT DEFAULT '', proposal TEXT DEFAULT '', notes TEXT DEFAULT '', folder_path TEXT DEFAULT '', created_at TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS student_files (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, document_type TEXT DEFAULT '', display_name TEXT NOT NULL, stored_path TEXT NOT NULL, original_path TEXT DEFAULT '', uploaded_at TEXT NOT NULL, sort_order INTEGER DEFAULT 0, FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS student_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, title TEXT NOT NULL, details TEXT DEFAULT '', due_jdate TEXT DEFAULT '', done INTEGER DEFAULT 0, created_at TEXT NOT NULL, completed_at TEXT DEFAULT '', FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS student_dates (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, date_label TEXT NOT NULL, date_value TEXT NOT NULL, created_at TEXT NOT NULL, FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS scurve_points (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, month_no INTEGER NOT NULL, month_label TEXT NOT NULL, plan_progress REAL, actual_progress REAL, UNIQUE(project_id, month_no), FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS gantt_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL, task_name TEXT NOT NULL, start_jdate TEXT NOT NULL, end_jdate TEXT NOT NULL, start_iso TEXT NOT NULL, end_iso TEXT NOT NULL, progress INTEGER DEFAULT 0, FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS custom_sheets (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, color TEXT DEFAULT '#2563EB', created_at TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS custom_sheet_staff (id INTEGER PRIMARY KEY AUTOINCREMENT, sheet_id INTEGER NOT NULL, first_name TEXT DEFAULT '', family_name TEXT DEFAULT '', role TEXT DEFAULT '', title TEXT DEFAULT '', position TEXT DEFAULT '', email TEXT DEFAULT '', telephone TEXT DEFAULT '', notes TEXT DEFAULT '', created_at TEXT NOT NULL, FOREIGN KEY(sheet_id) REFERENCES custom_sheets(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS custom_sheet_files (id INTEGER PRIMARY KEY AUTOINCREMENT, sheet_id INTEGER NOT NULL, document_type TEXT DEFAULT '', display_name TEXT NOT NULL, stored_path TEXT NOT NULL, original_path TEXT DEFAULT '', uploaded_at TEXT NOT NULL, sort_order INTEGER DEFAULT 0, FOREIGN KEY(sheet_id) REFERENCES custom_sheets(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS custom_sheet_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, sheet_id INTEGER NOT NULL, title TEXT NOT NULL, responsible TEXT DEFAULT '', due_jdate TEXT DEFAULT '', details TEXT DEFAULT '', done INTEGER DEFAULT 0, created_at TEXT NOT NULL, completed_at TEXT DEFAULT '', FOREIGN KEY(sheet_id) REFERENCES custom_sheets(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE IF NOT EXISTS courses (id INTEGER PRIMARY KEY AUTOINCREMENT, level TEXT NOT NULL, course_title TEXT NOT NULL, course_code TEXT DEFAULT '', semester TEXT DEFAULT '', instructor TEXT DEFAULT '', start_date TEXT DEFAULT '', end_date TEXT DEFAULT '', notes TEXT DEFAULT '', created_at TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS course_files (id INTEGER PRIMARY KEY AUTOINCREMENT, course_id INTEGER NOT NULL, document_type TEXT DEFAULT '', display_name TEXT NOT NULL, stored_path TEXT NOT NULL, original_path TEXT DEFAULT '', uploaded_at TEXT NOT NULL, sort_order INTEGER DEFAULT 0, FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE)");

        addColumn(db, "staff_members", "title", "TEXT DEFAULT ''");
        addColumn(db, "students", "referee", "TEXT DEFAULT ''");
        addColumn(db, "students", "bsc_project_form_date", "TEXT DEFAULT ''");
        addColumn(db, "student_tasks", "due_jdate", "TEXT DEFAULT ''");
        addColumn(db, "todos", "details", "TEXT DEFAULT ''");
        addColumn(db, "project_files", "sort_order", "INTEGER DEFAULT 0");
        addColumn(db, "student_files", "sort_order", "INTEGER DEFAULT 0");
        addColumn(db, "custom_sheet_staff", "title", "TEXT DEFAULT ''");
        addColumn(db, "custom_sheet_files", "sort_order", "INTEGER DEFAULT 0");
        addColumn(db, "course_files", "sort_order", "INTEGER DEFAULT 0");
        migrateLegacyTasksToGeneric(db);
        migrateLegacyDashboardNote(db);
    }

    private static void migrateLegacyTasksToGeneric(SQLiteDatabase db) {
        Cursor flag = db.rawQuery("SELECT value FROM app_settings WHERE key='android_generic_task_migration_v381'", null);
        try {
            if (flag.moveToFirst() && "done".equals(flag.getString(0))) return;
        } finally { flag.close(); }
        boolean ownTransaction = !db.inTransaction();
        if (ownTransaction) db.beginTransaction();
        try {
            db.execSQL("INSERT INTO todos (title,done,project_id,due_iso,due_jdate,responsible,details,created_at) " +
                    "SELECT title,done,NULL,'',due_jdate,'',details,created_at FROM student_tasks");
            db.execSQL("INSERT INTO todos (title,done,project_id,due_iso,due_jdate,responsible,details,created_at) " +
                    "SELECT title,done,NULL,'',due_jdate,responsible,details,created_at FROM custom_sheet_tasks");
            db.execSQL("INSERT OR REPLACE INTO app_settings (key,value) VALUES ('android_generic_task_migration_v381','done')");
            if (ownTransaction) db.setTransactionSuccessful();
        } finally {
            if (ownTransaction) db.endTransaction();
        }
    }

    private static void migrateLegacyDashboardNote(SQLiteDatabase db) {
        Cursor flag = db.rawQuery("SELECT value FROM app_settings WHERE key='android_note_migration_v382'", null);
        try {
            if (flag.moveToFirst() && "done".equals(flag.getString(0))) return;
        } finally { flag.close(); }

        Cursor legacy = db.rawQuery("SELECT value FROM app_settings WHERE key='dashboard_note'", null);
        String body = "";
        try {
            if (legacy.moveToFirst() && legacy.getString(0) != null) body = legacy.getString(0).trim();
        } finally { legacy.close(); }

        boolean ownTransaction = !db.inTransaction();
        if (ownTransaction) db.beginTransaction();
        try {
            if (!body.isEmpty()) {
                android.content.ContentValues cv = new android.content.ContentValues();
                cv.put("title", "Imported dashboard note");
                cv.put("body", body);
                cv.put("created_at", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(new java.util.Date()));
                cv.put("updated_at", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(new java.util.Date()));
                db.insert("dashboard_notes", null, cv);
            }
            db.execSQL("INSERT OR REPLACE INTO app_settings (key,value) VALUES ('android_note_migration_v382','done')");
            if (ownTransaction) db.setTransactionSuccessful();
        } finally {
            if (ownTransaction) db.endTransaction();
        }
    }

    private static void addColumn(SQLiteDatabase db, String table, String column, String type) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null);
        boolean found = false;
        try {
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(1))) {
                    found = true;
                    break;
                }
            }
        } finally {
            cursor.close();
        }
        if (!found) db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
    }
}
