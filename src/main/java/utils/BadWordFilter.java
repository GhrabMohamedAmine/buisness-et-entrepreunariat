package utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class BadWordFilter {

    public static String cleanText(String text) {

        try {

            String encoded = URLEncoder.encode(text, "UTF-8");

            String link =
                    "https://www.purgomalum.com/service/plain?text=" + encoded;

            URL url = new URL(link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");


            BufferedReader in =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String response = in.readLine();
            in.close();

            return response;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return text; // fallback si pas internet
    }
}