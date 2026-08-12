PERSONAL ASSISTANT ANDROID v3.7.7

This is the no-Android-Studio GitHub Actions build package.

Repository root must contain:
  .github/
  app/
  build.gradle
  gradle.properties
  settings.gradle

Build steps:
1. Upload the extracted contents to the repository root.
2. Open GitHub Actions.
3. Run "Build Android APK" or wait for the automatic run.
4. Open the successful run and download the APK artifact.
5. Extract the artifact ZIP and install app-debug.apk on the phone.

Important:
- Android may ask for notification and exact-alarm permissions.
- The application uses Persian calendar dates, although the word "Shamsi" is no longer shown in the interface.
- Import/export the shared assistant_data.db through Settings when transferring data between Windows and Android.
