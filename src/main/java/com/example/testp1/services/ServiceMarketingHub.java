package com.example.testp1.services;

import com.example.testp1.entities.CurrencyResponse;
//import com.example.testp1.entities.NewsResponse; //
import com.example.testp1.entities.NewsResponse;
import com.example.utils.Config;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * ServiceMarketingHub handles all external financial intelligence data.
 * It provides non-blocking, asynchronous access to Currency and News APIs.
 */
public class ServiceMarketingHub {

    // Reuse a single client and mapper for efficiency
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetches live exchange rates based on a specific currency (e.g., TND).
     * @param baseCurrency The code for the base currency.
     * @return A CompletableFuture containing the mapped CurrencyResponse.
     */
    public CompletableFuture<CurrencyResponse> getLiveExchangeRates(String baseCurrency) {
        String apiKey = Config.get("CURRENCY_API_KEY");
        String url = "https://v6.exchangerate-api.com/v6/" + apiKey + "/latest/" + baseCurrency;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        // sendAsync returns immediately; the UI thread stays free
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> handleResponse(response, CurrencyResponse.class));
    }

    /**
     * Fetches the latest global business and financial news.
     * @return A CompletableFuture containing the NewsResponse.
     */
    public CompletableFuture<NewsResponse> getLatestBusinessNews() {
        // 1. Pull the key from your .env via your Config utility
        String apiKey = Config.get("NEWS_API_KEY");

        // 2. Build the URL (Filtering for business category and English language)
        String url = "https://newsapi.org/v2/top-headlines?category=business&language=en&apiKey=" + apiKey;

        // 3. Create the HTTP Request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        // 4. Send asynchronously and funnel the response through our handleResponse helper
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> handleResponse(response, NewsResponse.class));
    }

    /**
     * Helper method to validate status codes and translate JSON to Java objects.
     */
    private <T> T handleResponse(HttpResponse<String> response, Class<T> responseClass) {
        if (response.statusCode() != 200) {
            throw new RuntimeException("API Request Failed with Status: " + response.statusCode());
        }
        try {
            return objectMapper.readValue(response.body(), responseClass);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error mapping " + responseClass.getSimpleName(), e);
        }
    }
}