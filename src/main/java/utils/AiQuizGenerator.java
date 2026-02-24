package utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class AiQuizGenerator {

    private static final String API_KEY = "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";

    public static JSONArray generateQuiz(String description){

        try{

            URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization","Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type","application/json");

            String prompt = """
            Create 5 quiz questions from this course.

            Conditions:
            - language: French
            - 3 answers only
            - one correct answer
            - JSON ONLY

            JSON format:
            {
              "quizzes":[
                {"question":"","r1":"","r2":"","r3":"","correct":1}
              ]
            }

            Course description:
            """ + description;

            JSONObject body = new JSONObject();
            body.put("model","mistralai/mistral-7b-instruct");

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role","user")
                    .put("content",prompt));

            body.put("messages",messages);
            body.put("response_format", new JSONObject().put("type","json_object"));

            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes());
            os.close();

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder response = new StringBuilder();
            String line;
            while((line = br.readLine())!=null)
                response.append(line);

            br.close();

            JSONObject json = new JSONObject(response.toString());

            String content = json
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            return new JSONObject(content).getJSONArray("quizzes");

        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
}