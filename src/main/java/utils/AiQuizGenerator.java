package utils;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class AiQuizGenerator {

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    private static final Dotenv dotenv = Dotenv.load();

    private static final String apiKey = dotenv.get("OPENROUTER_API_KEY");

    // 🔐 Load API key safely from config.properties

    public static JSONArray generateQuiz(String description) {

        try {

            //String API_KEY = apiKey;

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);


            // REQUIRED headers for OpenRouter
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("HTTP-Referer", "http://localhost");
            conn.setRequestProperty("X-Title", "NEXUM AI Quiz");

            // Better prompt (forces JSON reliability)
            String prompt = """
You are an educational quiz generator.

You MUST output ONLY valid JSON.
No explanations.
No text before JSON.
No text after JSON.

Generate exactly 5 quiz questions from this course.

Rules:
- Language: French
- Each question has 3 answers
- Only ONE correct answer
- "correct" value must be 1 or 2 or 3

STRICT JSON FORMAT:

{
  "quizzes":[
    {"question":"", "r1":"", "r2":"", "r3":"", "correct":1}
  ]
}

Course content:
""" + description;

            // Build request body
            JSONObject body = new JSONObject();
            body.put("model", "openai/gpt-3.5-turbo");

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", prompt));

            body.put("messages", messages);
            body.put("temperature", 0.4); // makes output stable

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Handle HTTP errors properly
            int status = conn.getResponseCode();
            InputStream responseStream;

            if (status >= 200 && status < 300) {
                responseStream = conn.getInputStream();
            } else {
                responseStream = conn.getErrorStream();
                if (responseStream == null) {
                    System.out.println("OpenRouter returned status: " + status);
                    return null;
                }
            }

            // Read response
            BufferedReader br = new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }

            br.close();

            JSONObject json = new JSONObject(response.toString());

            // Check if OpenRouter error message exists
            if (json.has("error")) {
                System.out.println("OpenRouter Error: " + json.getJSONObject("error").getString("message"));
                return null;
            }

            JSONArray choices = json.getJSONArray("choices");

            if (choices.length() == 0) {
                System.out.println("No AI response received.");
                return null;
            }

            String content = choices
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            // 🔧 VERY IMPORTANT: remove ```json ``` wrappers
            content = content.replace("```json", "")
                    .replace("```", "")
                    .trim();

            JSONObject result = new JSONObject(content);

            if (!result.has("quizzes")) {
                System.out.println("AI returned invalid format.");
                return null;
            }

            return result.getJSONArray("quizzes");

        } catch (Exception e) {
            System.out.println("AI Quiz Generation Failed:");
            e.printStackTrace();
            return null;
        }
    }
}