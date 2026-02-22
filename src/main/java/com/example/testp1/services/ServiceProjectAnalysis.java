package com.example.testp1.services;

import io.github.cdimascio.dotenv.Dotenv;
import java.net.URI;
import java.net.http.*;
import org.json.JSONObject;

public class ServiceProjectAnalysis {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("GEMINI_API_KEY");

    // Using the 2.5-flash model
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    public void fetchProjectProjection(double budget, double spent, String transactionsJson) {
        HttpClient client = HttpClient.newHttpClient();

        String systemInstruction = "You are a financial AI analyst. Analyze the velocity of project spending based on transaction history. " +
                "Predict the future spending path. Return ONLY a JSON object with these keys: " +
                "{ \"projectedTotal\": double, \"riskLevel\": \"LOW|MEDIUM|HIGH\", \"points\": double[] }";

        String userPrompt = String.format("Budget: %.2f, Current Spent: %.2f, Transaction History: %s", budget, spent, transactionsJson);

        String jsonBody = new JSONObject()
                .put("system_instruction", new JSONObject().put("parts", new JSONObject().put("text", systemInstruction)))
                .put("contents", new JSONObject().put("parts", new JSONObject().put("text", userPrompt)))
                .put("generationConfig", new JSONObject().put("response_mime_type", "application/json"))
                .toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        System.out.println("-> Sending Synchronous request to Google API...");

        try {
            // BLOCKING call. Java will freeze here until Google replies.
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("-> HTTP Status Code: " + response.statusCode());
            System.out.println("-> AI Response Body: \n" + response.body());

        } catch (Exception e) {
            System.err.println("-> CRITICAL ERROR: The request failed to send.");
            e.printStackTrace();
        }
    }
}