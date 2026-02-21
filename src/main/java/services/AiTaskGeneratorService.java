package services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static controllers.TaskValueMapper.normalizePriority;

public class AiTaskGeneratorService {
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";
    private static final Gson GSON = new Gson();

    private final HttpClient client = HttpClient.newHttpClient();

    public List<GeneratedTask> generateTasks(String projectName, String projectDescription, LocalDate startDate, LocalDate dueDate, int maxTasks)
            throws IOException, InterruptedException {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY missing. Set env var, JVM -DGROQ_API_KEY, or .env file.");
        }

        String userPrompt = """
                Project name: %s
                Project description: %s
                Start date: %s
                Due date: %s

                Generate %d practical project tasks.
                Return ONLY a valid JSON array.
                Each item must contain:
                - title (string, short and actionable)
                - description (string, one sentence)
                - priority (LOW, MEDIUM, or HIGH)
                """.formatted(
                nullSafe(projectName),
                nullSafe(projectDescription),
                startDate == null ? "" : startDate,
                dueDate == null ? "" : dueDate,
                Math.max(1, Math.min(10, maxTasks))
        );

        JsonObject requestPayload = new JsonObject();
        requestPayload.addProperty("model", MODEL);

        JsonArray messages = new JsonArray();
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "You are a project planning assistant. Respond with strict JSON only.");
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userPrompt);
        messages.add(userMessage);
        requestPayload.add("messages", messages);
        requestPayload.addProperty("temperature", 0.3);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestPayload)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Groq API error " + response.statusCode() + ": " + response.body());
        }

        JsonObject root = GSON.fromJson(response.body(), JsonObject.class);
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            return List.of();
        }

        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject("message");
        if (message == null || !message.has("content")) {
            return List.of();
        }

        String content = message.get("content").getAsString();
        JsonArray taskArray = parseTaskArray(content);
        if (taskArray == null || taskArray.isEmpty()) {
            return List.of();
        }
        List<GeneratedTask> tasks = new ArrayList<>();

        for (JsonElement element : taskArray) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            String title = getString(obj, "title");
            if (title.isBlank()) {
                continue;
            }
            String description = getString(obj, "description");
            String priority = normalizePriority(getString(obj, "priority"));
            tasks.add(new GeneratedTask(title, description, priority));
        }

        return tasks;
    }

    private static String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end < 0 || end <= start) {
            return null;
        }
        return text.substring(start, end + 1);
    }

    private static JsonArray parseTaskArray(String content) {
        if (content == null || content.isBlank()) {
            return new JsonArray();
        }

        String raw = stripCodeFence(content.trim());
        try {
            JsonElement root = JsonParser.parseString(raw);
            if (root.isJsonArray()) {
                return root.getAsJsonArray();
            }
            if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("tasks") && obj.get("tasks").isJsonArray()) {
                    return obj.getAsJsonArray("tasks");
                }
            }
        } catch (Exception ignored) {
        }

        String jsonArrayText = extractJsonArray(raw);
        if (jsonArrayText == null || jsonArrayText.isBlank()) {
            return new JsonArray();
        }
        try {
            JsonElement fallback = JsonParser.parseString(jsonArrayText);
            if (fallback.isJsonArray()) {
                return fallback.getAsJsonArray();
            }
        } catch (Exception ignored) {
        }
        return new JsonArray();
    }

    private static String stripCodeFence(String text) {
        if (!text.startsWith("```")) {
            return text;
        }
        String cleaned = text.replaceFirst("^```[a-zA-Z]*\\n?", "");
        return cleaned.replaceFirst("\\n?```$", "");
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return "";
        }
        return obj.get(key).getAsString().trim();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String resolveApiKey() {
        String fromEnv = System.getenv("GROQ_API_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        String fromProperty = System.getProperty("GROQ_API_KEY");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }

        return readFromDotEnv();
    }

    private static String readFromDotEnv() {
        try {
            Path envPath = Path.of(".env");
            if (!Files.exists(envPath)) {
                return null;
            }

            for (String line : Files.readAllLines(envPath)) {
                String trimmed = line == null ? "" : line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#") || !trimmed.startsWith("GROQ_API_KEY=")) {
                    continue;
                }
                String value = trimmed.substring("GROQ_API_KEY=".length()).trim();
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }
                return value.isBlank() ? null : value;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public record GeneratedTask(String title, String description, String priority) {
    }
}
