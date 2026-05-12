package services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.example.utils.Config.dotenv;
import static controllers.TaskValueMapper.normalizePriority;

public class AiTaskGeneratorService {
    private static final String API_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    private static final String MODEL = "gemini-2.5-flash";
    private static final Gson GSON = new Gson();
    //private static final String API_KEY = dotenv.get("GEMINI_API_KEY");

    private final HttpClient client = HttpClient.newHttpClient();

    public List<GeneratedTask> generateTasks(String projectName, String projectDescription, LocalDate startDate, LocalDate dueDate, int maxTasks)
            throws IOException, InterruptedException {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY missing. Set env var, JVM -DGEMINI_API_KEY, or .env file.");
        }

        String userPrompt = """
                Project name: %s
                Project description: %s
                Project start date: %s
                Project end date: %s

                Suggest %d starter tasks for this project.
                """.formatted(
                nullSafe(projectName),
                projectDescription == null || projectDescription.isBlank() ? "No description provided." : projectDescription.trim(),
                startDate == null ? "" : startDate,
                dueDate == null ? "" : dueDate,
                maxTasks
        );

        JsonObject requestPayload = new JsonObject();

        JsonObject systemInstruction = new JsonObject();
        JsonArray systemParts = new JsonArray();
        JsonObject systemText = new JsonObject();
        systemText.addProperty("text", """
                You are a project planning assistant.
                Return practical task suggestions for a newly created project.
                Focus on realistic execution steps, not vague goals.
                Keep titles concise and descriptions short.
                Use only priorities HIGH, MEDIUM, or LOW.
                Do not include any text outside the JSON response.
                """);
        systemParts.add(systemText);
        systemInstruction.add("parts", systemParts);
        requestPayload.add("systemInstruction", systemInstruction);

        JsonObject requestContent = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject userText = new JsonObject();
        userText.addProperty("text", userPrompt);
        parts.add(userText);
        requestContent.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(requestContent);
        requestPayload.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.2);
        generationConfig.addProperty("responseMimeType", "application/json");
        generationConfig.add("responseJsonSchema", createResponseSchema(Math.max(1, maxTasks)));
        requestPayload.add("generationConfig", generationConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildApiUrl(apiKey)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestPayload)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Gemini API error " + response.statusCode() + ": " + extractErrorMessage(response.body()));
        }

        JsonObject root = GSON.fromJson(response.body(), JsonObject.class);
        String content = extractGeminiContent(root);
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
            if (tasks.size() >= maxTasks) {
                break;
            }
        }

        return tasks;
    }

    private static String buildApiUrl(String apiKey) {
        return API_URL_TEMPLATE.formatted(
                URLEncoder.encode(MODEL, StandardCharsets.UTF_8),
                URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
        );
    }

    private static JsonObject createResponseSchema(int maxTasks) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.addProperty("additionalProperties", false);

        JsonObject properties = new JsonObject();
        JsonObject tasks = new JsonObject();
        tasks.addProperty("type", "array");
        tasks.addProperty("minItems", maxTasks);
        tasks.addProperty("maxItems", maxTasks);

        JsonObject item = new JsonObject();
        item.addProperty("type", "object");
        item.addProperty("additionalProperties", false);

        JsonObject itemProperties = new JsonObject();
        itemProperties.add("title", stringSchema("Short task title."));
        itemProperties.add("description", stringSchema("Short explanation of the task."));

        JsonObject priority = new JsonObject();
        priority.addProperty("type", "string");
        JsonArray priorityValues = new JsonArray();
        priorityValues.add("HIGH");
        priorityValues.add("MEDIUM");
        priorityValues.add("LOW");
        priority.add("enum", priorityValues);
        itemProperties.add("priority", priority);

        item.add("properties", itemProperties);
        JsonArray itemRequired = new JsonArray();
        itemRequired.add("title");
        itemRequired.add("description");
        itemRequired.add("priority");
        item.add("required", itemRequired);

        tasks.add("items", item);
        properties.add("tasks", tasks);
        schema.add("properties", properties);

        JsonArray required = new JsonArray();
        required.add("tasks");
        schema.add("required", required);
        return schema;
    }

    private static JsonObject stringSchema(String description) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "string");
        schema.addProperty("description", description);
        return schema;
    }

    private static String extractGeminiContent(JsonObject root) {
        if (root == null || !root.has("candidates") || !root.get("candidates").isJsonArray()) {
            return "";
        }

        JsonArray candidates = root.getAsJsonArray("candidates");
        if (candidates.isEmpty() || !candidates.get(0).isJsonObject()) {
            return "";
        }

        JsonObject candidate = candidates.get(0).getAsJsonObject();
        JsonObject content = candidate.getAsJsonObject("content");
        if (content == null || !content.has("parts") || !content.get("parts").isJsonArray()) {
            return "";
        }

        JsonArray parts = content.getAsJsonArray("parts");
        if (parts.isEmpty() || !parts.get(0).isJsonObject()) {
            return "";
        }

        return getString(parts.get(0).getAsJsonObject(), "text");
    }

    private static String extractErrorMessage(String responseBody) {
        try {
            JsonObject root = GSON.fromJson(responseBody, JsonObject.class);
            JsonObject error = root == null ? null : root.getAsJsonObject("error");
            String message = getString(error, "message");
            return message.isBlank() ? responseBody : message;
        } catch (Exception ignored) {
            return responseBody;
        }
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
        String fromEnv = System.getenv("GEMINI_API_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        String fromProperty = System.getProperty("GEMINI_API_KEY");
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
                if (trimmed.isBlank() || trimmed.startsWith("#")) {
                    continue;
                }

                int separatorIndex = trimmed.indexOf('=');
                if (separatorIndex < 0) {
                    continue;
                }

                String key = trimmed.substring(0, separatorIndex).trim();
                if (!"GEMINI_API_KEY".equals(key)) {
                    continue;
                }

                String value = trimmed.substring(separatorIndex + 1).trim();
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
