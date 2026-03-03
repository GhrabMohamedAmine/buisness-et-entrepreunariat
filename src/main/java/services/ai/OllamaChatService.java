package services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

public class OllamaChatService {

    private static final String URL = "http://localhost:1234/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(20))
            .readTimeout(Duration.ofMinutes(5))
            .writeTimeout(Duration.ofMinutes(2))
            .callTimeout(Duration.ofMinutes(6))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    private final String model = "dolphin3.0-llama3.1-8b";

    public String chat(List<Message> conversation) throws IOException {

        List<Map<String, String>> msgs = new ArrayList<>();
        for (Message m : conversation) {
            msgs.add(Map.of("role", m.role(), "content", m.content()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", msgs);
        body.put("stream", false);

        // ✅ chat/completions params (top-level)
        body.put("max_tokens", 100);
        body.put("temperature", 0.0);
        body.put("top_p", 0.9);

        String json = mapper.writeValueAsString(body);

        Request request = new Request.Builder()
                .url(URL)
                .post(RequestBody.create(json, JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                return "Ollama error: HTTP " + response.code() + " " + err;
            }

            String responseBody = Objects.requireNonNull(response.body()).string();
            JsonNode root = mapper.readTree(responseBody);

            // ✅ correct for /v1/chat/completions
            String out = root.path("choices").path(0).path("message").path("content").asText(null);

            return (out == null || out.isBlank()) ? "(empty reply)" : out;
        }
    }

    public record Message(String role, String content) {}
}