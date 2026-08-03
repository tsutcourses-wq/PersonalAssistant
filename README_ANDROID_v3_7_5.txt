Personal Assistant Android v3.7.5
=================================

Mobile adaptation of the Windows v3.7.5 data model and main functional modules.

Included in this update
-----------------------
- Phone reminder notifications at 1 day, 1 hour, and 10 minutes before a reminder.
- Notification rescheduling after reboot, app update, time change, database import, or manual request.
- Dashboard without the former Mobile Dashboard and Overview cards.
- Dashboard quick actions for reminder, student, and project creation.
- One-week Shamsi calendar on the dashboard.
- Three-line side navigation menu for Dashboard, Reminders, Tasks, Students, Projects, Courses,
  File Inventory, and Settings.
- Day/night switch in Settings using sun/moon switch text.
- File Inventory with folder-first organization, folder rename/delete/export-as-ZIP, file upload,
  file opening, title/type editing, deletion, Save As, and manual ordering arrows.
- Students with B.S.c, M.S.c, and P.h.D information forms, Shamsi registration year, registration
  semester, referee, B.S.c project/form date behavior, M.S.c/P.h.D academic dates, documents and tasks.
- Projects with information, tasks, staff, documents, letters, WBS, CBS, S-curve data, and Gantt data.
- Courses and course documents.
- Reminder month-selection Excel (.xlsx) reports.
- Database import/export compatible with the Windows assistant_data.db schema.

Important mobile adaptation notes
---------------------------------
- Hover actions from Windows are represented by visible details, tap, or long-press menus on Android.
- Desktop charts are represented by editable mobile data lists. The same records remain compatible with
  the Windows database.
- Android keeps uploaded files in its private app storage. A database imported from Windows can show
  file metadata, but a Windows-only file path cannot be opened on Android until that file is uploaded or
  replaced on the phone.
- The current synchronization method is manual database import/export. Do not edit the Windows and
  Android copies simultaneously.
