package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class CompreFaceService {

    private static final String API_KEY = "8d0ee048-6e06-4249-9099-24e533506d34";
    private static final String BASE_URL = "http://localhost:8000/api/v1/recognition";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String registerFace(byte[] imageData, String subject) {
        try {
            System.out.println("Taille image: " + imageData.length);

            Path tempFile = Paths.get("capture_" + System.currentTimeMillis() + ".jpg");
            Files.write(tempFile, imageData);
            System.out.println("Image sauvegardée: " + tempFile.toAbsolutePath());

            String boundary = UUID.randomUUID().toString();
            String header = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"face.jpg\"\r\n" +
                    "Content-Type: image/jpeg\r\n\r\n";
            String footer = "\r\n--" + boundary + "--";

            byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
            byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);
            byte[] body = new byte[headerBytes.length + imageData.length + footerBytes.length];
            System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
            System.arraycopy(imageData, 0, body, headerBytes.length, imageData.length);
            System.arraycopy(footerBytes, 0, body, headerBytes.length + imageData.length, footerBytes.length);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/faces?subject=" + subject))
                    .header("x-api-key", API_KEY)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Code: " + response.statusCode());
            System.out.println("Réponse: " + response.body());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                JsonNode root = objectMapper.readTree(response.body());
                // La réponse peut contenir "image_id" ou "face_token" selon la version
                if (root.has("image_id")) {
                    return root.get("image_id").asText();
                } else if (root.has("face_token")) {
                    return root.get("face_token").asText();
                } else {
                    System.err.println("Champ inconnu dans la réponse : " + response.body());
                }
            } else {
                System.err.println("Erreur CompreFace: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String recognizeFace(byte[] imageData) {
        try {
            System.out.println("Taille image: " + imageData.length);

            String boundary = UUID.randomUUID().toString();
            String header = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"face.jpg\"\r\n" +
                    "Content-Type: image/jpeg\r\n\r\n";
            String footer = "\r\n--" + boundary + "--";

            byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
            byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);
            byte[] body = new byte[headerBytes.length + imageData.length + footerBytes.length];
            System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
            System.arraycopy(imageData, 0, body, headerBytes.length, imageData.length);
            System.arraycopy(footerBytes, 0, body, headerBytes.length + imageData.length, footerBytes.length);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/recognize"))
                    .header("x-api-key", API_KEY)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Code: " + response.statusCode());
            System.out.println("Réponse: " + response.body());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode result = root.path("result").get(0);
                if (result != null) {
                    JsonNode subjects = result.path("subjects");
                    if (subjects.size() > 0) {
                        return subjects.get(0).get("subject").asText();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}