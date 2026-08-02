Personal Assistant Android - v3.6.66 Android
===========================================

This package contains an Android Studio source project for a phone-adapted Android version of the Personal Assistant app.

Important difference from Windows
---------------------------------
Windows can select a normal Google Drive folder path because Google Drive for desktop mounts folders in File Explorer.
Android does not provide a normal live Google Drive folder path to apps. Therefore the Android version uses manual database import/export through the Android file picker:

1. Import database from Google Drive / file
2. Edit reminders, students, projects, courses, tasks, and important dates on the phone
3. Export / Sync database now, and save the file as assistant_data.db in Google Drive

Phone-adapted features included
-------------------------------
- Mobile dashboard
- Students list and student page
- Student fields with Persian/Arabic-friendly text editing
- Student sections: Research background / Seminar / Proposal
- Student important dates
- Student tasks
- Reminders add/edit/delete
- Project list and project page
- Project tasks
- Course list and course page
- Day/night view
- Student list export as CSV for Excel
- Database import from Google Drive/file
- Database export/sync to Google Drive/file
- Same SQLite database name and main schema as the Windows version: assistant_data.db

How to use with the Windows version
-----------------------------------
On the Windows PC:
1. Close Personal Assistant after editing.
2. Press Sync database now in the Windows app, if needed.
3. Wait until Google Drive says Up to date.

On Android:
1. Open Personal Assistant Android.
2. Go to Settings.
3. Press Import database from Google Drive / file.
4. Select assistant_data.db from Google Drive.
5. Edit the data.
6. Press Export / Sync database now.
7. Save/replace the file as assistant_data.db in Google Drive.
8. Wait until Google Drive sync is finished before using the Windows app again.

Safe rule
---------
Do not edit the same database on two devices at the same time. Use one device, sync/export, wait, then use the other device.

Build instructions
------------------
1. Install Android Studio.
2. Open this folder: PersonalAssistantAndroid_v3_6_66
3. Let Android Studio download the Android Gradle plugin and SDK components.
4. Connect your Android phone with USB debugging enabled, or use an emulator.
5. Run the app, or use Build > Build APK(s).

Notes
-----
- The package is an Android Studio source project, not a Windows installer.
- Existing file paths saved by the Windows version may point to Windows folders. Android can still show the database records, but Windows local file paths are not directly openable on Android unless the files are separately available through Google Drive or the Android file picker.
- For full two-way file attachment syncing, a Google Drive API integration would be needed in a future version.
