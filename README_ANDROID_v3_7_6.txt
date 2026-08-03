PERSONAL ASSISTANT ANDROID v3.7.6
Build without Android Studio using GitHub Actions

REPOSITORY ROOT
Upload the contents of this folder so the repository root directly contains:
  .github/
  app/
  build.gradle
  settings.gradle
  gradle.properties

BUILD APK
1. Open the GitHub repository.
2. Open Actions.
3. Select Build Android APK.
4. Run the workflow, or wait for the push-triggered run.
5. Open the successful run.
6. Download PersonalAssistant-Android-debug-APK under Artifacts.
7. Extract app-debug.apk and install it on the Android phone.

NOTIFICATIONS
- Approve notification permission when Android asks.
- Open Settings in the app.
- Use Send test notification now.
- On Android 12 or newer, use Allow precise reminder timing if it is shown.
- Use Reschedule all reminder notifications after importing a database.
- Scheduled alerts are: 1 day before, 1 hour before, 10 minutes before, and at the reminder time.

DATABASE TRANSFER
The Android app uses a local working copy of assistant_data.db.
- Import the database before working on the phone.
- Export/Sync database after working on the phone.
- Avoid editing independent copies on two devices at the same time.

FULL SCREEN AND NAVIGATION
- The app runs in immersive full-screen mode.
- Swipe from an edge to temporarily reveal Android system bars.
- The phone Back control and the in-app Back button return to the previous app screen.
- Back on the Dashboard does not close the app.
