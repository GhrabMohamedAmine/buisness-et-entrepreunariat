package services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import entities.ProjectFile;
import utils.MyDatabase;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CloudinaryFileUploadService {
    private static final long MAX_UPLOAD_BYTES = 2L * 1024L * 1024L;
    private static final String DEFAULT_PROJECT_FILE_TABLE = "project_file";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ProjectFile uploadProjectFile(Path file, int projectId) throws IOException, InterruptedException {
        return uploadProjectFile(file, projectId, 0);
    }

    public ProjectFile uploadProjectFile(Path file, int projectId, int uploadedByUserId) throws IOException, InterruptedException {
        if (file == null || !Files.exists(file)) {
            throw new IOException("File not found.");
        }
        long size = Files.size(file);
        if (size > MAX_UPLOAD_BYTES) {
            throw new IOException("Maximum upload size is 2 MB.");
        }

        CloudinaryConfig config = CloudinaryConfig.fromEnvironment();
        String folder = config.folder() + "/project-" + projectId;
        String boundary = "----NexumCloudinary" + UUID.randomUUID();
        List<Part> parts = new ArrayList<>();
        parts.add(Part.text("folder", folder));

        if (config.uploadPreset() != null && !config.uploadPreset().isBlank()) {
            parts.add(Part.text("upload_preset", config.uploadPreset()));
        } else {
            long timestamp = Instant.now().getEpochSecond();
            parts.add(Part.text("timestamp", String.valueOf(timestamp)));
            parts.add(Part.text("api_key", config.apiKey()));
            parts.add(Part.text("signature", sign(Map.of(
                    "folder", folder,
                    "timestamp", String.valueOf(timestamp)
            ), config.apiSecret())));
        }

        parts.add(Part.file("file", file));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudinary.com/v1_1/" + url(config.cloudName()) + "/auto/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(buildMultipartBody(parts, boundary)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Cloudinary upload failed: HTTP " + response.statusCode() + " - " + cloudinaryErrorMessage(response.body()));
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        ProjectFile uploadedFile = new ProjectFile(
                projectId,
                uploadedByUserId,
                file.getFileName().toString(),
                stringValue(json, "public_id", ""),
                stringValue(json, "resource_type", "raw"),
                stringValue(json, "format", null),
                longValue(json, "bytes", size),
                stringValue(json, "secure_url", ""),
                stringValue(json, "created_at", ""),
                uploadedByUserId,
                null
        );
        saveUploadedFile(uploadedFile);
        return uploadedFile;
    }

    public List<ProjectFile> getProjectFiles(int projectId) {
        ensureProjectFilesTable();
        List<ProjectFile> files = new ArrayList<>();
        String query = "SELECT id, project_id, uploaded_by, original_name, public_id, resource_type, format, bytes, secure_url, created_at, created_by_user_id, updated_by_user_id " +
                "FROM " + tableName() + " WHERE project_id = ? ORDER BY id DESC";
        try (Connection conn = MyDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, projectId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                files.add(new ProjectFile(
                        rs.getInt("id"),
                        rs.getInt("project_id"),
                        rs.getInt("uploaded_by"),
                        rs.getString("original_name"),
                        rs.getString("public_id"),
                        rs.getString("resource_type"),
                        rs.getString("format"),
                        rs.getLong("bytes"),
                        rs.getString("secure_url"),
                        rs.getString("created_at"),
                        rs.getInt("created_by_user_id"),
                        nullableInt(rs, "updated_by_user_id")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return files;
    }

    private void saveUploadedFile(ProjectFile uploadedFile) {
        ensureProjectFilesTable();
        String query = "INSERT INTO " + tableName() + " " +
                "(project_id, uploaded_by, original_name, public_id, resource_type, format, bytes, secure_url, created_at, created_by_user_id, updated_by_user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = MyDatabase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, uploadedFile.getProjectId());
            stmt.setInt(2, uploadedFile.getUploadedBy());
            stmt.setString(3, uploadedFile.getOriginalName());
            stmt.setString(4, uploadedFile.getPublicId());
            stmt.setString(5, uploadedFile.getResourceType());
            stmt.setString(6, uploadedFile.getFormat());
            stmt.setLong(7, uploadedFile.getBytes());
            stmt.setString(8, uploadedFile.getSecureUrl());
            stmt.setTimestamp(9, parseCreatedAt(uploadedFile.getCreatedAt()));
            stmt.setInt(10, uploadedFile.getCreatedByUserId());
            if (uploadedFile.getUpdatedByUserId() == null) {
                stmt.setNull(11, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(11, uploadedFile.getUpdatedByUserId());
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void ensureProjectFilesTable() {
        String query = "CREATE TABLE IF NOT EXISTS " + DEFAULT_PROJECT_FILE_TABLE + " (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "project_id INT NOT NULL, " +
                "uploaded_by INT NOT NULL, " +
                "original_name VARCHAR(255) NOT NULL, " +
                "public_id VARCHAR(255) NOT NULL, " +
                "resource_type VARCHAR(50) NOT NULL, " +
                "format VARCHAR(50) NULL, " +
                "bytes INT NULL, " +
                "secure_url VARCHAR(1000) NOT NULL, " +
                "created_at DATETIME NOT NULL, " +
                "created_by_user_id INT NOT NULL, " +
                "updated_by_user_id INT NULL, " +
                "INDEX idx_project_file_project_id (project_id), " +
                "INDEX idx_project_file_uploaded_by (uploaded_by), " +
                "INDEX idx_project_file_created_by_user_id (created_by_user_id), " +
                "INDEX idx_project_file_updated_by_user_id (updated_by_user_id)" +
                ")";
        try (Connection conn = MyDatabase.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String tableName() {
        try (Connection conn = MyDatabase.getConnection()) {
            if (tableExists(conn, "project_file")) {
                return "project_file";
            }
            if (tableExists(conn, "project_files")) {
                return "project_files";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return DEFAULT_PROJECT_FILE_TABLE;
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, tableName, null)) {
            return rs.next();
        }
    }

    private Timestamp parseCreatedAt(String value) {
        if (value != null && !value.isBlank()) {
            try {
                return Timestamp.from(Instant.parse(value));
            } catch (Exception ignored) {
                // Fall through to current timestamp when Cloudinary returns a non-ISO value.
            }
        }
        return Timestamp.from(Instant.now());
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private byte[] buildMultipartBody(List<Part> parts, String boundary) throws IOException {
        String lineBreak = "\r\n";
        List<byte[]> body = new ArrayList<>();
        for (Part part : parts) {
            StringBuilder header = new StringBuilder();
            header.append("--").append(boundary).append(lineBreak);
            header.append("Content-Disposition: form-data; name=\"").append(part.name()).append("\"");
            if (part.fileName() != null) {
                header.append("; filename=\"").append(part.fileName()).append("\"").append(lineBreak);
                header.append("Content-Type: application/octet-stream");
            }
            header.append(lineBreak).append(lineBreak);
            body.add(header.toString().getBytes(StandardCharsets.UTF_8));
            body.add(part.content());
            body.add(lineBreak.getBytes(StandardCharsets.UTF_8));
        }
        body.add(("--" + boundary + "--" + lineBreak).getBytes(StandardCharsets.UTF_8));

        int totalLength = body.stream().mapToInt(bytes -> bytes.length).sum();
        byte[] result = new byte[totalLength];
        int offset = 0;
        for (byte[] bytes : body) {
            System.arraycopy(bytes, 0, result, offset, bytes.length);
            offset += bytes.length;
        }
        return result;
    }

    private String sign(Map<String, String> params, String apiSecret) {
        String payload = params.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "&" + right)
                .orElse("") + apiSecret;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available.", e);
        }
    }

    private static String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String cloudinaryErrorMessage(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("error") && json.get("error").isJsonObject()) {
                JsonObject error = json.getAsJsonObject("error");
                return stringValue(error, "message", body);
            }
        } catch (Exception ignored) {
            // Return raw body below.
        }
        return body;
    }

    private static String stringValue(JsonObject json, String key, String fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : fallback;
    }

    private static long longValue(JsonObject json, String key, long fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsLong() : fallback;
    }

    private record Part(String name, byte[] content, String fileName) {
        private static Part text(String name, String value) {
            return new Part(name, value.getBytes(StandardCharsets.UTF_8), null);
        }

        private static Part file(String name, Path file) throws IOException {
            return new Part(name, Files.readAllBytes(file), file.getFileName().toString());
        }
    }

    private record CloudinaryConfig(String cloudName, String uploadPreset, String apiKey, String apiSecret, String folder) {
        private static CloudinaryConfig fromEnvironment() {
            Map<String, String> localEnv = readLocalEnv();
            CloudinaryUrl cloudinaryUrl = parseCloudinaryUrl(value("CLOUDINARY_URL", localEnv));
            String cloudName = firstNonBlank(cloudinaryUrl == null ? null : cloudinaryUrl.cloudName(), value("CLOUDINARY_CLOUD_NAME", localEnv));
            String uploadPreset = value("CLOUDINARY_UPLOAD_PRESET", localEnv);
            String apiKey = firstNonBlank(cloudinaryUrl == null ? null : cloudinaryUrl.apiKey(), value("CLOUDINARY_API_KEY", localEnv));
            String apiSecret = firstNonBlank(cloudinaryUrl == null ? null : cloudinaryUrl.apiSecret(), value("CLOUDINARY_API_SECRET", localEnv));
            if (cloudName == null || cloudName.isBlank()) {
                throw new IllegalStateException("Missing environment variable: CLOUDINARY_CLOUD_NAME or CLOUDINARY_URL");
            }
            if ((uploadPreset == null || uploadPreset.isBlank()) && (apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank())) {
                throw new IllegalStateException("Set CLOUDINARY_UPLOAD_PRESET or both CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET.");
            }
            String folder = value("CLOUDINARY_PROJECT_FILES_FOLDER", localEnv);
            if (folder == null || folder.isBlank()) {
                folder = "nexum/project-files";
            }
            return new CloudinaryConfig(cloudName, uploadPreset, apiKey, apiSecret, folder);
        }

        private static String value(String key, Map<String, String> localEnv) {
            String envValue = System.getenv(key);
            if (envValue != null && !envValue.isBlank()) {
                return envValue;
            }
            return localEnv.get(key);
        }

        private static Map<String, String> readLocalEnv() {
            Path envPath = Paths.get(".env");
            Map<String, String> values = new HashMap<>();
            if (!Files.exists(envPath)) {
                return values;
            }
            try {
                for (String line : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                        continue;
                    }
                    String[] parts = trimmed.split("=", 2);
                    values.put(parts[0].trim(), parts[1].trim());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return values;
        }

        private static CloudinaryUrl parseCloudinaryUrl(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                URI uri = URI.create(value);
                if (!"cloudinary".equalsIgnoreCase(uri.getScheme())) {
                    return null;
                }
                String userInfo = uri.getRawUserInfo();
                String host = uri.getHost();
                if (userInfo == null || host == null || !userInfo.contains(":")) {
                    return null;
                }
                String[] parts = userInfo.split(":", 2);
                return new CloudinaryUrl(
                        URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8),
                        host
                );
            } catch (Exception e) {
                throw new IllegalStateException("Invalid CLOUDINARY_URL.", e);
            }
        }

        private static String firstNonBlank(String first, String second) {
            if (first != null && !first.isBlank()) {
                return first;
            }
            return second;
        }

        private record CloudinaryUrl(String apiKey, String apiSecret, String cloudName) {}
    }
}
