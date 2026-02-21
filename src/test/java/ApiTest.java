
import com.example.testp1.entities.CurrencyResponse;
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
    }
}