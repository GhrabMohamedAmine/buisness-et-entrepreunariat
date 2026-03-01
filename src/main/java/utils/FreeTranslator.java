package utils;

 import org.json.JSONObject;

 import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class FreeTranslator {

    public static String translate(String text, String from, String to) {

        try {

            String encoded = URLEncoder.encode(text, "UTF-8");

            String link =
                    "https://api.mymemory.translated.net/get?q="
                            + encoded +
                            "&langpair=" + from + "|" + to;

            URL url = new URL(link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder response = new StringBuilder();
            String line;

            while((line = br.readLine()) != null)
                response.append(line);

            br.close();

            JSONObject json = new JSONObject(response.toString());

            return json
                    .getJSONObject("responseData")
                    .getString("translatedText");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return text; // fallback
    }
}