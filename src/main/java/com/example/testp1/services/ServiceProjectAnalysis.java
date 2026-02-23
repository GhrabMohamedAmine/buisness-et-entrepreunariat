package com.example.testp1.services;

import com.example.testp1.model.ProjectBudgetDAO;
import com.example.testp1.model.TransactionDAO;
import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.entities.Transaction;
import com.example.testp1.entities.ProjectAnalysisResult; // Make sure this import matches where you saved the model!

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ServiceProjectAnalysis {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String API_KEY = dotenv.get("GEMINI_API_KEY");

    // Using the 2.5-flash model
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    // Direct DAO connections instead of Services
    private final ProjectBudgetDAO budgetDAO = new ProjectBudgetDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    /**
     * Fetches data, queries Gemini, parses the JSON, and returns a usable Java Object.
     */
    public ProjectAnalysisResult analyzeProject(int budgetId, String userNotes) {
        try {
            // 1. FETCH DATA FROM DATABASE VIA DAO
            System.out.println("-> Fetching data directly from DAOs for Budget ID: " + budgetId);
            ProjectBudget budget = budgetDAO.getById(budgetId);
            List<Transaction> transactions = transactionDAO.getByBudgetId(budgetId);

            if (budget == null) {
                System.err.println("-> Error: ProjectBudget not found in DB!");
                return null; // Return null instead of void
            }

            // 2. FORMAT THE DATABASE RECORDS INTO A READABLE STRING FOR THE AI
            StringBuilder dataContext = new StringBuilder();
            dataContext.append("PROJECT CONTEXT:\n");
            dataContext.append("Total Budget: ").append(budget.getTotalBudget()).append("\n");
            dataContext.append("Actual Spent: ").append(budget.getActualSpend()).append("\n");
            dataContext.append("Deadline: ").append(budget.getDueDate()).append("\n\n");

            dataContext.append("USER NOTES (Future Events):\n");
            dataContext.append(userNotes != null && !userNotes.trim().isEmpty() ? userNotes : "None").append("\n\n");

            dataContext.append("TRANSACTION HISTORY:\n");
            if (transactions == null || transactions.isEmpty()) {
                dataContext.append("No transactions yet.\n");
            } else {
                for (Transaction t : transactions) {
                    dataContext.append("Date: ").append(t.getDateStamp())
                            .append(" | Cost: ").append(t.getCost())
                            .append(" | Category: ").append(t.getExpenseCategory()).append("\n");
                }
            }

            // 3. DEFINE THE ROI / BREAK-EVEN INSTRUCTION
            String systemInstruction = "You are an expert financial AI. Analyze the project budget, transaction history, and user notes. " +
                    "CRITICAL RULES FOR LOGIC: " +
                    "1. If 'TRANSACTION HISTORY' is empty, you MUST rely heavily on the 'USER NOTES' to project the initial spend and trajectory. " +
                    "2. Compare projected total spend against the Total Budget. If the project stays UNDER budget, 'successProbability' should be HIGH (70-98). If it goes OVER budget, penalize it (below 40). " +
                    "CRITICAL RULES FOR CHARTING: " +
                    "3. The timelineData MUST contain EXACTLY 5 items representing 5 STRICTLY CONSECUTIVE chronological future months (e.g., 'Mar', 'Apr', 'May', 'Jun', 'Jul'). " +
                    "4. DO NOT duplicate months. DO NOT list months out of order. " +
                    "5. The 'spend' and 'revenue' values MUST be CUMULATIVE running totals. They must steadily increase or stay flat, but they can NEVER decrease. " +
                    "Return ONLY a JSON object with this exact structure: " +
                    "{ \"projectedFinalCost\": double, \"projectedFinalRevenue\": double, \"successProbability\": int, \"inflectionDate\": \"MMM\", " +
                    "\"timelineData\": [ { \"month\": \"MMM\", \"spend\": double, \"revenue\": double } ] }";

            // 4. BUILD THE JSON BODY
            JSONObject systemInstructionObj = new JSONObject()
                    .put("parts", new JSONArray().put(new JSONObject().put("text", systemInstruction)));

            JSONObject contentsObj = new JSONObject()
                    .put("parts", new JSONArray().put(new JSONObject().put("text", dataContext.toString())));

            JSONObject generationConfig = new JSONObject()
                    .put("response_mime_type", "application/json");

            JSONObject requestBody = new JSONObject()
                    .put("system_instruction", systemInstructionObj)
                    .put("contents", new JSONArray().put(contentsObj))
                    .put("generationConfig", generationConfig);

            // 5. SEND THE REQUEST
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            System.out.println("-> Sending Synchronous request to Google API...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("-> HTTP Status Code: " + response.statusCode());

            // 6. PARSE THE RESPONSE BODY INTO OUR JAVA OBJECT
            if (response.statusCode() == 200) {
                JSONObject root = new JSONObject(response.body());
                JSONArray candidates = root.getJSONArray("candidates");
                String aiText = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                // Clean up any markdown blocks just in case
                aiText = aiText.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();

                // CONVERT AI JSON TO OUR JAVA OBJECT
                JSONObject aiJson = new JSONObject(aiText);
                ProjectAnalysisResult resultData = new ProjectAnalysisResult();

                resultData.setProjectedFinalCost(aiJson.getDouble("projectedFinalCost"));
                resultData.setProjectedFinalRevenue(aiJson.getDouble("projectedFinalRevenue"));
                resultData.setSuccessProbability(aiJson.getInt("successProbability"));
                resultData.setOriginalBudget(budget.getTotalBudget());
                if (aiJson.isNull("inflectionDate")) {
                    resultData.setInflectionDate("N/A");
                } else {
                    resultData.setInflectionDate(aiJson.optString("inflectionDate", "N/A"));
                }

                JSONArray timelineArray = aiJson.getJSONArray("timelineData");
                for (int i = 0; i < timelineArray.length(); i++) {
                    JSONObject point = timelineArray.getJSONObject(i);
                    resultData.addTimelinePoint(new ProjectAnalysisResult.TimelinePoint(
                            point.getString("month"),
                            point.getDouble("spend"),
                            point.getDouble("revenue")
                    ));
                }

                System.out.println("-> Successfully mapped AI Response to ProjectAnalysisResult Object!");
                return resultData; // Send the packed object back to the UI!

            } else {
                System.err.println("-> API Error Response: " + response.body());
            }

        } catch (Exception e) {
            System.err.println("-> CRITICAL ERROR: The request failed.");
            e.printStackTrace();
        }

        return null; // Return null if the API call failed
    }
}