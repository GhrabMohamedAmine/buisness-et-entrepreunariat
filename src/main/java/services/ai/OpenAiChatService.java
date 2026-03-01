package services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

public class OpenAiChatService {

    private static final String API_URL = "https://api.openai.com/v1/responses";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(45))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    // Read key from environment variable: OPENAI_API_KEY
    private final String apiKey = "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";

    public String reply(String systemPrompt, List<ChatMessage> conversation) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            return "OPENAI_API_KEY is missing. Add it as an environment variable, then restart the app.";
        }

        // Build "input" array for Responses API (role + content)
        List<Map<String, Object>> input = new ArrayList<>();

        // System message
        input.add(Map.of(
                "role", "system",
                "content", List.of(Map.of("type", "input_text", "text", systemPrompt))
        ));

        // Conversation
        for (ChatMessage m : conversation) {
            input.add(Map.of(
                    "role", m.role(), // "user" or "assistant"
                    "content", List.of(Map.of("type", "input_text", "text", m.text()))
            ));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", "gpt-4.1-mini"); // good/cheap; you can switch later
        payload.put("input", input);

        String json = mapper.writeValueAsString(payload);

        Request req = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(json, JSON))
                .build();

        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) {
                String err = (res.body() != null) ? res.body().string() : "";
                return "OpenAI error: HTTP " + res.code() + " " + err;
            }

            String body = Objects.requireNonNull(res.body()).string();
            JsonNode root = mapper.readTree(body);

            // Some SDKs provide output_text, but raw HTTP won't.
            // We'll safely extract text from output[] items.
            return extractText(root);
        }
    }

    private String extractText(JsonNode root) {
        JsonNode output = root.get("output");
        if (output == null || !output.isArray()) return "(no output)";

        StringBuilder sb = new StringBuilder();
        for (JsonNode item : output) {
            JsonNode content = item.get("content");
            if (content != null && content.isArray()) {
                for (JsonNode c : content) {
                    if ("output_text".equals(c.path("type").asText())) {
                        sb.append(c.path("text").asText()).append("\n");
                    }
                }
            }
        }

        String text = sb.toString().trim();
        return text.isEmpty() ? "(empty response)" : text;
    }

    public record ChatMessage(String role, String text) {}
}