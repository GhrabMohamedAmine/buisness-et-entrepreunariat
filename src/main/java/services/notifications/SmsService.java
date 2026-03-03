package services.notifications;

import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SmsService {

    private final HttpClient client = HttpClient.newHttpClient();
    private static final Dotenv dotenv = Dotenv.load();
    private static final String accountSid = dotenv.get("SMS_KEY1");
    private static final String authToken = dotenv.get("SMS_KEY2");
    // Put these in environment variables (recommended)
    private final String fromNumber = "+16402236936";
    // e.g. +1xxxx

    public SmsService() {

        System.out.println("DEBUG SID: " + accountSid);
        System.out.println("DEBUG TOKEN: " + authToken);
        System.out.println("DEBUG FROM: " + fromNumber);

        if (accountSid == null || authToken == null || fromNumber == null) {
            throw new IllegalStateException(
                    "Missing env vars: TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM_NUMBER"
            );
        }
    }

    public void sendSms(String to, String message) {
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            String form = "To=" + enc(to)
                    + "&From=" + enc(fromNumber)
                    + "&Body=" + enc(message);

            String basicAuth = Base64.getEncoder()
                    .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + basicAuth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Twilio SMS failed. HTTP " + response.statusCode() + " -> " + response.body());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error sending SMS: " + e.getMessage(), e);
        }
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}