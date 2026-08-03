CREATE TABLE projects (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                status TEXT DEFAULT 'Not started',
                progress INTEGER DEFAULT 0,
                start_jdate TEXT DEFAULT '',
                end_jdate TEXT DEFAULT '',
                principal_investigator TEXT DEFAULT '',
                contractor TEXT DEFAULT '',
                notes TEXT DEFAULT '',
                created_at TEXT NOT NULL
            );

CREATE TABLE sqlite_sequence(name,seq);

CREATE TABLE project_sections (
                project_id INTEGER NOT NULL,
                section_name TEXT NOT NULL,
                content TEXT DEFAULT '',
                PRIMARY KEY (project_id, section_name),
                FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE
            );

CREATE TABLE reminders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                details TEXT DEFAULT '',
                project_id INTEGER,
                remind_at_iso TEXT NOT NULL,
                jdate TEXT NOT NULL,
                time_text TEXT NOT NULL,
                created_at TEXT NOT NULL,
                FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE SET NULL
            );

CREATE TABLE reminder_alerts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                reminder_id INTEGER NOT NULL,
                alert_at_iso TEXT NOT NULL,
                alert_type TEXT NOT NULL,
                fired INTEGER DEFAULT 0,
                UNIQUE(reminder_id, alert_type),
                FOREIGN KEY(reminder_id) REFERENCES reminders(id) ON DELETE CASCADE
            );

CREATE TABLE todos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                done INTEGER DEFAULT 0,
                project_id INTEGER,
                due_iso TEXT DEFAULT '',
                due_jdate TEXT DEFAULT '',
                responsible TEXT DEFAULT '',
                created_at TEXT NOT NULL,
                FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE SET NULL
            );

CREATE TABLE todo_alerts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                todo_id INTEGER NOT NULL,
                alert_at_iso TEXT NOT NULL,
                alert_type TEXT NOT NULL,
                fired INTEGER DEFAULT 0,
                UNIQUE(todo_id, alert_type),
                FOREIGN KEY(todo_id) REFERENCES todos(id) ON DELETE CASCADE
            );

CREATE TABLE staff_members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id INTEGER NOT NULL,
                first_name TEXT DEFAULT '',
                family_name TEXT DEFAULT '',
                role TEXT DEFAULT '',
                position TEXT DEFAULT '',
                email TEXT DEFAULT '',
                telephone TEXT DEFAULT '',
                created_at TEXT NOT NULL,
                FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE
            );

CREATE TABLE wbs_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id INTEGER NOT NULL,
                code TEXT DEFAULT '',
                title TEXT DEFAULT '',
                description TEXT DEFAULT '',
                responsible TEXT DEFAULT '',
                deliverable TEXT DEFAULT '',
                weight_percent REAL DEFAULT 0,
                completed INTEGER DEFAULT 0,
                start_month INTEGER DEFAULT 1,
                end_month INTEGER DEFAULT 1,
                start_jdate TEXT DEFAULT '',
                end_jdate TEXT DEFAULT '',
                progress INTEGER DEFAULT 0,
                created_at TEXT NOT NULL,
                FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE
            );

CREATE TABLE cbs_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id INTEGER NOT NULL,
                code TEXT DEFAULT '',
                cost_item TEXT DEFAULT '',
                category TEXT DEFAULT '',
                unit TEXT DEFAULT '',
                quantity REAL DEFAULT 0,
                unit_cost REAL DEFAULT 0,
                total_cost REAL DEFAULT 0,
                created_at TEXT NOT NULL,
                FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE
            );

CREATE TABLE app_settings (
                key TEXT PRIMARY KEY,
                value TEXT DEFAULT ''
            );

CREATE TABLE project_files (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id INTEGER NOT NULL,
                section_name TEXT NOT NULL,
                display_name TEXT NOT NULL,
                subject TEXT DEFAULT '',
                document_type TEXT DEFAULT '',
                stored_path TEXT NOT NULL,
                original_path TEXT DEFAULT '',
                uploaded_at TEXT NOT NULL,
                FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE
            );

CREATE TABLE students (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                level TEXT NOT NULL,
                first_name TEXT DEFAULT '',
                family_name TEXT DEFAULT '',
                student_no TEXT DEFAULT '',
                national_id TEXT DEFAULT '',
                registration_date TEXT DEFAULT '',
                registration_semester TEXT DEFAULT '',
                email TEXT DEFAULT '',
                telephone TEXT DEFAULT '',
                supervisor TEXT DEFAULT '',
                second_supervisor TEXT DEFAULT '',
                advisor TEXT DEFAULT '',
                status TEXT DEFAULT '',
                thesis_status TEXT DEFAULT '',
                research_background TEXT DEFAULT '',
                seminar TEXT DEFAULT '',
                proposal TEXT DEFAULT '',
                notes TEXT DEFAULT '',
                folder_path TEXT DEFAULT '',
                created_at TEXT NOT NULL
            );

CREATE TABLE student_files (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id INTEGER NOT NULL,
                document_type TEXT DEFAULT '',
                display_name TEXT NOT NULL,
                stored_path TEXT NOT NULL,
                original_path TEXT DEFAULT '',
                uploaded_at TEXT NOT NULL,
                FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE
            );

CREATE TABLE student_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                details TEXT DEFAULT '',
                done INTEGER DEFAULT 0,
                created_at TEXT NOT NULL,
                completed_at TEXT DEFAULT '',
                FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE
            );

CREATE TABLE student_dates (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id INTEGER NOT NULL,
                date_label TEXT NOT NULL,
                date_value TEXT NOT NULL,
                created_at TEXT NOT NULL,
                FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE
            );

CREATE TABLE scurve_points (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id INTEGER NOT NULL,
                month_no INTEGER NOT NULL,
                month_label TEXT NOT NULL,
                plan_progress REAL,
                actual_progress REAL,
                UNIQUE(project_id, month_no),
                FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE
            );

CREATE TABLE gantt_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id INTEGER NOT NULL,
                task_name TEXT NOT NULL,
                start_jdate TEXT NOT NULL,
                end_jdate TEXT NOT NULL,
                start_iso TEXT NOT NULL,
                end_iso TEXT NOT NULL,
                progress INTEGER DEFAULT 0,
                FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE
            );

CREATE TABLE custom_sheets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                color TEXT DEFAULT '#2563EB',
                created_at TEXT NOT NULL
            );

CREATE TABLE custom_sheet_staff (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sheet_id INTEGER NOT NULL,
                first_name TEXT DEFAULT '',
                family_name TEXT DEFAULT '',
                role TEXT DEFAULT '',
                position TEXT DEFAULT '',
                email TEXT DEFAULT '',
                telephone TEXT DEFAULT '',
                notes TEXT DEFAULT '',
                created_at TEXT NOT NULL,
                FOREIGN KEY(sheet_id) REFERENCES custom_sheets(id) ON DELETE CASCADE
            );

CREATE TABLE custom_sheet_files (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sheet_id INTEGER NOT NULL,
                document_type TEXT DEFAULT '',
                display_name TEXT NOT NULL,
                stored_path TEXT NOT NULL,
                original_path TEXT DEFAULT '',
                uploaded_at TEXT NOT NULL,
                FOREIGN KEY(sheet_id) REFERENCES custom_sheets(id) ON DELETE CASCADE
            );

CREATE TABLE custom_sheet_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sheet_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                responsible TEXT DEFAULT '',
                due_jdate TEXT DEFAULT '',
                details TEXT DEFAULT '',
                done INTEGER DEFAULT 0,
                created_at TEXT NOT NULL,
                completed_at TEXT DEFAULT '',
                FOREIGN KEY(sheet_id) REFERENCES custom_sheets(id) ON DELETE CASCADE
            );

CREATE TABLE courses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                level TEXT NOT NULL,
                course_title TEXT NOT NULL,
                course_code TEXT DEFAULT '',
                semester TEXT DEFAULT '',
                instructor TEXT DEFAULT '',
                start_date TEXT DEFAULT '',
                end_date TEXT DEFAULT '',
                notes TEXT DEFAULT '',
                created_at TEXT NOT NULL
            );

CREATE TABLE course_files (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                course_id INTEGER NOT NULL,
                document_type TEXT DEFAULT '',
                display_name TEXT NOT NULL,
                stored_path TEXT NOT NULL,
                original_path TEXT DEFAULT '',
                uploaded_at TEXT NOT NULL,
                FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE
            );

