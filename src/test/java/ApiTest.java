
import com.example.testp1.entities.Article;
import com.example.testp1.entities.CurrencyResponse;
import com.example.testp1.entities.NewsResponse;
import com.example.utils.Config;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiTest {
    public static void main(String[] args) {
        try {
            // 1. Get the Key from your .env via the Config class
            String apiKey = Config.get("CURRENCY_API_KEY");
            if (apiKey == null) {
                System.err.println("Error: API Key not found in .env file!");
                return;
            }

            // 2. Build the Request
            String url = "https://v6.exchangerate-api.com/v6/" + apiKey + "/latest/TND";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            System.out.println("Connecting to API...");

            // 3. Send the Request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 4. Check if successful (Status 200)
            if (response.statusCode() == 200) {
                String jsonBody = response.body();
                System.out.println("JSON Received successfully!");

                // 5. Use Jackson to translate JSON -> Java Object
                ObjectMapper mapper = new ObjectMapper();
                CurrencyResponse data = mapper.readValue(jsonBody, CurrencyResponse.class);

                // 6. Print the results to verify
                System.out.println("Base Currency: " + data.base_code);
                System.out.println("Rate for USD: " + data.rates.get("USD"));
                System.out.println("Rate for EUR: " + data.rates.get("EUR"));
            } else {
                System.out.println("Failed! Status code: " + response.statusCode());
                System.out.println("Error details: " + response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        testNewsApi();
    }

    private static void testNewsApi() {
        try {
            String apiKey = Config.get("NEWS_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                System.err.println("[ERROR] NEWS_API_KEY is missing from .env file!");
                return;
            }

            String url = "https://newsapi.org/v2/top-headlines?category=business&language=en&apiKey=" + apiKey;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            System.out.println("[INFO] Connecting to News API...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                NewsResponse newsData = mapper.readValue(response.body(), NewsResponse.class);

                System.out.println("[SUCCESS] News Data Mapped Successfully.");
                System.out.println("Total Articles Found: " + newsData.totalResults);
                System.out.println("-------------------------------------------------");

                // Print first 3 results to verify nesting (Source -> Article)
                int limit = Math.min(newsData.articles.size(), 3);
                for (int i = 0; i < limit; i++) {
                    Article art = newsData.articles.get(i);
                    String sourceName = (art.source != null) ? art.source.name : "Unknown Source";

                    System.out.println("Headline " + (i + 1) + ": " + art.title);
                    System.out.println("Source: " + sourceName);
                    System.out.println("Link: " + art.url);
                    System.out.println("-------------------------------------------------");
                }
            } else {
                System.err.println("[FAILED] HTTP Status: " + response.statusCode());
                System.err.println("Response Body: " + response.body());
            }

        } catch (Exception e) {
            System.err.println("[CRITICAL ERROR] News API Test Failed!");
            e.printStackTrace();
        }
    }
}