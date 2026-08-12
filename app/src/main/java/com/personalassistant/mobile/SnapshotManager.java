package com.personalassistant.mobile;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Portable database + managed-file snapshots used by central-server Phase 1. */
public final class SnapshotManager {
    private static final String FILE_TOKEN = "@FILES/";
    private static final String[] FILE_TABLES = {
            "project_files", "student_files", "custom_sheet_files", "course_files"
    };

    private SnapshotManager() { }

    private static void copy(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[8192];
        int count;
        while ((count = in.read(buffer)) >= 0) out.write(buffer, 0, count);
    }

    private static void copyFile(File source, File destination) throws Exception {
        File parent = destination.getParentFile();
        if (parent != null) parent.mkdirs();
        try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(destination)) {
            copy(in, out);
        }
    }

    private static void addFileToZip(ZipOutputStream zip, File file, String entryName) throws Exception {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    addFileToZip(zip, child, entryName + "/" + child.getName());
                }
            }
            return;
        }
        zip.putNextEntry(new ZipEntry(entryName.replace('\\', '/')));
        try (InputStream in = new FileInputStream(file)) { copy(in, zip); }
        zip.closeEntry();
    }

    private static String relativePath(File root, File file) {
        String rootPath = root.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (!filePath.startsWith(rootPath)) return "";
        String value = filePath.substring(rootPath.length());
        while (value.startsWith(File.separator)) value = value.substring(1);
        return value.replace(File.separatorChar, '/');
    }

    public static byte[] createSnapshot(Context context, DbHelper helper, String appVersion) throws Exception {
        SQLiteDatabase live = helper.getWritableDatabase();
        Cursor checkpoint = live.rawQuery("PRAGMA wal_checkpoint(FULL)", null);
        checkpoint.close();

        File working = new File(context.getCacheDir(), "central_snapshot_work");
        deleteRecursive(working);
        working.mkdirs();
        File sourceDb = context.getDatabasePath(DbHelper.DB_NAME);
        File portableDb = new File(working, DbHelper.DB_NAME);
        copyFile(sourceDb, portableDb);

        File managedRoot = new File(context.getFilesDir(), "assistant_files");
        SQLiteDatabase portable = SQLiteDatabase.openDatabase(portableDb.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
        try {
            for (String table : FILE_TABLES) {
                Cursor cursor;
                try { cursor = portable.rawQuery("SELECT id,stored_path FROM " + table + " WHERE stored_path<>''", null); }
                catch (Exception ignored) { continue; }
                try {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(0);
                        File stored = new File(cursor.getString(1));
                        String relative = relativePath(managedRoot, stored);
                        if (!relative.isEmpty()) {
                            portable.execSQL("UPDATE " + table + " SET stored_path=? WHERE id=?", new Object[]{FILE_TOKEN + relative, id});
                        }
                    }
                } finally { cursor.close(); }
            }
            portable.execSQL("INSERT OR REPLACE INTO app_settings(key,value) VALUES('storage_root','@FILES')");
        } finally { portable.close(); }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            addFileToZip(zip, portableDb, DbHelper.DB_NAME);
            if (managedRoot.isDirectory()) addFileToZip(zip, managedRoot, "assistant_files");
            JSONObject manifest = new JSONObject();
            manifest.put("format", "personal-assistant-central-snapshot");
            manifest.put("format_version", 1);
            manifest.put("app_version", appVersion);
            manifest.put("created_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date()));
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(manifest.toString(2).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        deleteRecursive(working);
        return bytes.toByteArray();
    }

    public static void applySnapshot(Context context, byte[] payload) throws Exception {
        File working = new File(context.getCacheDir(), "central_snapshot_download");
        deleteRecursive(working);
        working.mkdirs();
        String root = working.getCanonicalPath() + File.separator;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(payload))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                File target = new File(working, entry.getName());
                String canonical = target.getCanonicalPath();
                if (!canonical.equals(working.getCanonicalPath()) && !canonical.startsWith(root)) {
                    throw new Exception("Downloaded snapshot contains an unsafe path.");
                }
                if (entry.isDirectory()) target.mkdirs();
                else {
                    File parent = target.getParentFile();
                    if (parent != null) parent.mkdirs();
                    try (OutputStream out = new FileOutputStream(target)) { copy(zip, out); }
                }
                zip.closeEntry();
            }
        }
        File incomingDb = new File(working, DbHelper.DB_NAME);
        if (!incomingDb.isFile()) throw new Exception("Server snapshot does not contain assistant_data.db.");
        SQLiteDatabase check = SQLiteDatabase.openDatabase(incomingDb.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
        Cursor integrity = check.rawQuery("PRAGMA integrity_check", null);
        boolean ok = integrity.moveToFirst() && "ok".equalsIgnoreCase(integrity.getString(0));
        integrity.close();
        if (!ok) { check.close(); throw new Exception("Downloaded database failed integrity check."); }
        DbHelper.ensureSchema(check);
        check.close();

        File currentDb = context.getDatabasePath(DbHelper.DB_NAME);
        File managedRoot = new File(context.getFilesDir(), "assistant_files");
        File backupRoot = new File(context.getFilesDir(), "server_backups/" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()));
        backupRoot.mkdirs();
        if (currentDb.isFile()) copyFile(currentDb, new File(backupRoot, DbHelper.DB_NAME));
        if (managedRoot.isDirectory()) copyDirectory(managedRoot, new File(backupRoot, "assistant_files"));

        deleteDatabaseSidecars(currentDb);
        copyFile(incomingDb, currentDb);
        deleteDatabaseSidecars(currentDb);
        deleteRecursive(managedRoot);
        File incomingFiles = new File(working, "assistant_files");
        if (incomingFiles.isDirectory()) copyDirectory(incomingFiles, managedRoot);

        SQLiteDatabase restored = SQLiteDatabase.openDatabase(currentDb.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
        try {
            for (String table : FILE_TABLES) {
                Cursor cursor;
                try { cursor = restored.rawQuery("SELECT id,stored_path FROM " + table + " WHERE stored_path LIKE '@FILES/%'", null); }
                catch (Exception ignored) { continue; }
                try {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(0);
                        String relative = cursor.getString(1).substring(FILE_TOKEN.length());
                        File absolute = new File(managedRoot, relative.replace('/', File.separatorChar));
                        restored.execSQL("UPDATE " + table + " SET stored_path=? WHERE id=?", new Object[]{absolute.getAbsolutePath(), id});
                    }
                } finally { cursor.close(); }
            }
            restored.execSQL("INSERT OR REPLACE INTO app_settings(key,value) VALUES('storage_root',?)", new Object[]{managedRoot.getAbsolutePath()});
        } finally { restored.close(); }
        deleteRecursive(working);
    }

    private static void deleteDatabaseSidecars(File db) {
        new File(db.getAbsolutePath() + "-wal").delete();
        new File(db.getAbsolutePath() + "-shm").delete();
    }

    private static void copyDirectory(File source, File destination) throws Exception {
        if (source.isDirectory()) {
            destination.mkdirs();
            File[] children = source.listFiles();
            if (children != null) for (File child : children) copyDirectory(child, new File(destination, child.getName()));
        } else copyFile(source, destination);
    }

    private static void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteRecursive(child);
        }
        file.delete();
    }
}
