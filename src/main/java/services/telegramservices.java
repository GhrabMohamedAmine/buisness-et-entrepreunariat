package services;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class telegramservices {

    // Récupération sécurisée du token
    private static final String BOT_TOKEN = "8718206953:AAH5Y8QKKwgCt01mAzaHrQniF6qd4XZLlhk";
    private static final String API_URL = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";

    public static boolean sendMessage(String chatId, String text) {
        if (BOT_TOKEN == null || BOT_TOKEN.isEmpty()) {
            System.err.println("Erreur : Token Telegram non défini. " +
                    "Veuillez définir la variable d'environnement TELEGRAM_BOT_TOKEN.");
            return false;
        }

        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Construction du corps de la requête JSON
            String jsonPayload = String.format(
                    "{\"chat_id\":\"%s\",\"text\":\"%s\"}",
                    escapeJson(chatId),
                    escapeJson(text)
            );

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                System.out.println("Notification Telegram envoyée avec succès.");
                return true;
            } else {
                System.err.println("Telegram API a répondu avec le code : " + responseCode);
                return false;
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi du message Telegram : " + e.getMessage());
            return false;
        }
    }

    /**
     * Échappe les caractères spéciaux pour une chaîne JSON.
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '/': sb.append("\\/"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }
}