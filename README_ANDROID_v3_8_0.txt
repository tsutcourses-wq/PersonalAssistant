PERSONAL ASSISTANT ANDROID v3.8.0 — CENTRAL SERVER READY

All v3.7.7 mobile screens and features remain available.

NEW CENTRAL SERVER PHASE 1
- Settings now contains Central server controls.
- Configure the server URL, username and password.
- Test server connection.
- Upload all information and managed files.
- Download all information and managed files.
- Server revisions prevent silent overwrites.
- A local backup is created before a server download is applied.
- Reminder notifications are rescheduled after download.

LOCAL TEST ADDRESS
Use the LAN address of the computer running the server, for example:
http://192.168.1.10:8000
Do not use 127.0.0.1 on the phone because that refers to the phone itself.

BUILD WITHOUT ANDROID STUDIO
Upload the project contents to a GitHub repository root. The root must contain:
.github
app
build.gradle
gradle.properties
settings.gradle

Open Actions → Build Android APK and download the successful artifact.

SECURITY
Cleartext HTTP is enabled only to allow local LAN testing. Use HTTPS before deploying the server on the internet.
