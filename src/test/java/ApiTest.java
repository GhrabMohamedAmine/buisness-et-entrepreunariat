package utils;

import org.json.JSONArray;

public class ApiTest {
    public static void main(String[] args) {
        System.out.println("🚀 Starting AI Quiz Generation Test...");

        // 1. Define a sample course description (French, as per your prompt)
        String sampleDescription = "L'histoire de la Révolution française commence en 1789. " +
                "Elle a mené à l'abolition de la monarchie et à l'établissement d'une république.";

        // 2. Call the generator
        JSONArray result = AiQuizGenerator.generateQuiz(sampleDescription);

        // 3. Validate results
        if (result != null && result.length() > 0) {
            System.out.println("✅ Success! Received " + result.length() + " questions.");

            // Print the first question to verify structure
            System.out.println("--- Sample Output ---");
            System.out.println(result.toString(4)); // Indented for readability
        } else {
            System.err.println("❌ Test Failed: No quiz data returned.");
        }
    }
}