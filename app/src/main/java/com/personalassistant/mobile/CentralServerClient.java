package com.personalassistant.mobile;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * HTTP client isolated from the Activity so the current UI can remain unchanged
 * while the storage layer moves toward a central server.
 */
public final class CentralServerClient {
    public static final class Settings {
        public String url = "";
        public String username = "";
        public String password = "";
        public int revision = 0;
    }

    public static final class DownloadResult {
        public final byte[] data;
        public final int revision;
        DownloadResult(byte[] data, int revision) {
            this.data = data;
            this.revision = revision;
        }
    }

    public static final class ConflictException extends Exception {
        public final int currentRevision;
        ConflictException(String message, int currentRevision) {
            super(message);
            this.currentRevision = currentRevision;
        }
    }

    private final Settings settings;
    private String token = "";

    public CentralServerClient(Settings settings) {
        this.settings = settings;
    }

    private String baseUrl() throws Exception {
        String value = settings.url == null ? "" : settings.url.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        if (value.isEmpty()) throw new Exception("Central server address is empty.");
        return value;
    }

    private static byte[] readAll(InputStream input) throws Exception {
        if (input == null) return new byte[0];
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) >= 0) out.write(buffer, 0, count);
            return out.toByteArray();
        }
    }

    private HttpURLConnection open(String method, String path, boolean authenticated) throws Exception {
        URL url = new URL(baseUrl() + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(180000);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");
        if (authenticated) {
            if (token.isEmpty()) login();
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
        return connection;
    }

    private static Exception httpError(HttpURLConnection connection, byte[] body) {
        String text = new String(body, StandardCharsets.UTF_8);
        try {
            JSONObject json = new JSONObject(text);
            Object detail = json.opt("detail");
            if (detail != null) text = String.valueOf(detail);
        } catch (Exception ignored) { }
        return new Exception("Server returned HTTP " + safeCode(connection) + ": " + text);
    }

    private static int safeCode(HttpURLConnection connection) {
        try { return connection.getResponseCode(); }
        catch (Exception ignored) { return -1; }
    }

    public JSONObject health() throws Exception {
        HttpURLConnection connection = open("GET", "/health", false);
        try {
            int code = connection.getResponseCode();
            byte[] body = readAll(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (code >= 400) throw httpError(connection, body);
            return new JSONObject(new String(body, StandardCharsets.UTF_8));
        } finally { connection.disconnect(); }
    }

    public String login() throws Exception {
        HttpURLConnection connection = open("POST", "/api/v1/auth/login", false);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        JSONObject payload = new JSONObject();
        payload.put("username", settings.username == null ? "" : settings.username);
        payload.put("password", settings.password == null ? "" : settings.password);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }
        try {
            int code = connection.getResponseCode();
            byte[] body = readAll(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (code >= 400) throw httpError(connection, body);
            JSONObject response = new JSONObject(new String(body, StandardCharsets.UTF_8));
            token = response.optString("access_token", "");
            if (token.isEmpty()) throw new Exception("Server did not return an access token.");
            return token;
        } finally { connection.disconnect(); }
    }

    public JSONObject snapshotInfo() throws Exception {
        HttpURLConnection connection = open("GET", "/api/v1/snapshot/info", true);
        try {
            int code = connection.getResponseCode();
            byte[] body = readAll(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (code >= 400) throw httpError(connection, body);
            return new JSONObject(new String(body, StandardCharsets.UTF_8));
        } finally { connection.disconnect(); }
    }

    public JSONObject uploadSnapshot(byte[] data, int baseRevision, boolean force) throws Exception {
        String path = "/api/v1/snapshot/upload?base_revision=" + baseRevision + "&force=" + force;
        HttpURLConnection connection = open("POST", path, true);
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(data.length);
        connection.setRequestProperty("Content-Type", "application/zip");
        try (OutputStream out = connection.getOutputStream()) { out.write(data); }
        try {
            int code = connection.getResponseCode();
            byte[] body = readAll(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
            String text = new String(body, StandardCharsets.UTF_8);
            if (code == 409) {
                int current = 0;
                String detail = "The server contains a different revision.";
                try {
                    JSONObject json = new JSONObject(text);
                    current = json.optInt("current_revision", 0);
                    detail = json.optString("detail", detail);
                } catch (Exception ignored) { }
                throw new ConflictException(detail, current);
            }
            if (code >= 400) throw httpError(connection, body);
            return new JSONObject(text);
        } finally { connection.disconnect(); }
    }

    public DownloadResult downloadSnapshot() throws Exception {
        HttpURLConnection connection = open("GET", "/api/v1/snapshot/download", true);
        try {
            int code = connection.getResponseCode();
            byte[] body = readAll(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (code >= 400) throw httpError(connection, body);
            int revision = 0;
            try { revision = Integer.parseInt(connection.getHeaderField("X-Personal-Assistant-Revision")); }
            catch (Exception ignored) { }
            return new DownloadResult(body, revision);
        } finally { connection.disconnect(); }
    }
}
