package services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

public class OllamaChatService {

    private static final String URL = "http://127.0.0.1:11434/api/chat";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // ✅ VERY GENEROUS TIMEOUTS
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(20))
            .readTimeout(Duration.ofMinutes(5))
            .writeTimeout(Duration.ofMinutes(2))
            .callTimeout(Duration.ofMinutes(6))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    // Current lightweight model
    private final String model = "llama3.2:3b";

    public String chat(List<Message> conversation) throws IOException {

        // Convert to maps (Jackson-friendly)
        List<Map<String, String>> msgs = new ArrayList<>();
        for (Message m : conversation) {
            msgs.add(Map.of("role", m.role(), "content", m.content()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", msgs);
        body.put("stream", false);

        // ✅ STEP 2: Strict deterministic settings
        Map<String, Object> options = new HashMap<>();
        options.put("num_predict", 100);     // limit response length
        options.put("temperature", 0.0);     // 🔥 no creativity (deterministic)
        options.put("top_p", 0.9);
        body.put("options", options);

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
            String out = root.path("message").path("content").asText();

            return (out == null || out.isBlank()) ? "(empty reply)" : out;
        }
    }

    public record Message(String role, String content) {}
}