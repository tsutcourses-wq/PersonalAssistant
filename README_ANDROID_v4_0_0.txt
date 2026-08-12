Personal Assistant — Android v4.0.0 (independent Android edition)
=================================================================

Purpose
-------
This is the first Android release maintained independently from the Windows
version numbering/features. The Android interface is intentionally simplified
for reminders, tasks, calendar navigation and notes.

Implemented changes
-------------------
1. Students section removed from Android navigation/UI.
2. Projects section removed from Android navigation/UI.
3. Courses section removed from Android navigation/UI.
4. File inventory section removed from Android navigation/UI.
5. Database / sync section removed from Settings.
6. Central server / Phase 1 controls are not present in this Android edition.
7. Dashboard Quick Actions section removed.
8. Dashboard now contains:
   - Jalali calendar + Add reminder
   - up to 5 upcoming reminders
   - up to 5 open/upcoming tasks
   - Add task button
   - See all tasks button
   - persistent Notes area supporting English and Persian text
9. Calendar supports Week / Month / Year modes, Previous/Next navigation,
   Today reset, and left/right swipe on the calendar card. Tapping a date opens
   Add Reminder with that Jalali date pre-filled.
10. Tasks are generic personal tasks only; there is no Project Task / Student
    Task type in Add Task.
11. Every task can be edited after creation, marked done/open, or deleted.
12. No item 12 instruction was supplied in the request, so no additional item
    was applied.

Upgrade/data safety
-------------------
- Application ID is unchanged: com.personalassistant.mobile
- Android versionCode is 4000 so it can update older builds with lower codes.
- Existing legacy student/project/course tables are NOT deleted on upgrade.
  They are simply no longer exposed by the Android UI.
- Existing rows from legacy 'todos' and 'student_tasks' are copied once into
  the new generic task list when those legacy tables exist.
- Removing Database/Sync/Central Server means this Android edition is LOCAL to
  the phone. New reminders/tasks/notes are not automatically sent to Windows
  or the central server.

GitHub build
------------
Repository root must contain:
  .github/
  app/
  build.gradle
  settings.gradle

The included workflow is:
  .github/workflows/build-android.yml

In GitHub:
1. Upload/replace the repository files with this package.
2. Open Actions -> Build Android APK.
3. Run workflow if it did not start automatically.
4. Download artifact: PersonalAssistant-Android-v4.0.0-APK
5. Extract it and install app-debug.apk on the phone.

For an in-place update, install the APK over the currently installed Personal
Assistant app. Do not uninstall the old app first if you want to preserve its
local Android database/data.
