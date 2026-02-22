package com.example.testp1;

import com.example.testp1.entities.ProjectAnalysisResult;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class AnalysisResultController {

    // Main Views
    @FXML private VBox graphView;
    @FXML private HBox summaryView;

    // Core Data Labels
    @FXML private Label varianceLabel;
    @FXML private Label varianceSubtext;
    @FXML private Label riskBadge;

    // Extra Grid Data Labels
    @FXML private Label projectedTotalLabel;
    @FXML private Label allocatedBudgetLabel;
    @FXML private Label inflectionDateLabel;

    // Probability Animation Nodes
    @FXML private Arc probabilityArc;
    @FXML private Label probabilityLabel;

    // Carousel Controls
    @FXML private Circle dot1;
    @FXML private Circle dot2;
    @FXML private LineChart<String, Number> projectionChart;

    private boolean isSummaryActive = true;

    @FXML
    public void initialize() {
        // Ready for mock data injection
    }

    // --- Navigation Logic (Same as before) ---
    @FXML
    private void showGraphView() {
        if (!isSummaryActive) return;
        isSummaryActive = false;
        updateDots(dot2, dot1);
        slideTransition(summaryView, graphView, -50, 50);
    }

    @FXML
    private void showSummaryView() {
        if (isSummaryActive) return;
        isSummaryActive = true;
        updateDots(dot1, dot2);
        slideTransition(graphView, summaryView, 50, -50);
    }

    private void updateDots(Circle active, Circle inactive) {
        active.setFill(Color.web("#7F22FE"));
        inactive.setFill(Color.web("#D6D3D1"));
    }

    private void slideTransition(javafx.scene.Node outgoing, javafx.scene.Node incoming, double outX, double inX) {
        incoming.setVisible(true);
        incoming.setTranslateX(inX);
        incoming.setOpacity(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), outgoing);
        slideOut.setToX(outX);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), outgoing);
        fadeOut.setToValue(0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), incoming);
        slideIn.setToX(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), incoming);
        fadeIn.setToValue(1);

        ParallelTransition p1 = new ParallelTransition(slideOut, fadeOut);
        p1.setOnFinished(e -> outgoing.setVisible(false));

        ParallelTransition p2 = new ParallelTransition(slideIn, fadeIn);

        new SequentialTransition(p1, p2).play();
    }

    // --- Data Injection ---

    /**
     * Animates the circular progress bar.
     */
    public void animateProbability(int probability) {
        double targetAngle = -(probability / 100.0) * 360.0;

        if (probability >= 50) {
            probabilityArc.setStroke(Color.web("#10B981")); // Green
        } else {
            probabilityArc.setStroke(Color.web("#EF4444")); // Red
        }

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(probabilityArc.lengthProperty(), 0),
                        new KeyValue(probabilityLabel.textProperty(), "0%")
                ),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(probabilityArc.lengthProperty(), targetAngle, Interpolator.EASE_OUT)
                )
        );

        timeline.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            double progress = newTime.toMillis() / 1500.0;
            int currentVal = (int) (progress * probability);
            probabilityLabel.setText(currentVal + "%");
        });

        timeline.play();
    }

    /**
     * Populates the view with all the new data fields!
     */
    public void injectMockData() {
        // Top Level Badges
        riskBadge.setText("HIGH RISK");
        riskBadge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; -fx-padding: 6 12 6 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 12px;");

        // Main Variance
        varianceLabel.setText("-$5,000");
        varianceLabel.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
        varianceSubtext.setText("Expected Overrun");
        varianceSubtext.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");

        // The New Grid Details
        projectedTotalLabel.setText("$45,000");
        allocatedBudgetLabel.setText("$40,000");
        inflectionDateLabel.setText("March 15, 2026");

        // Trigger the circle animation
        animateProbability(35); // 35% chance of staying under budget

        // BOOM! Draw the chart lines!
        populateChart();
    }

    public void populateChart() {
        // Clear any old data
        projectionChart.getData().clear();

        // 1. Create the Spend Series (The Cost Curve)
        XYChart.Series<String, Number> spendSeries = new XYChart.Series<>();
        spendSeries.setName("Cumulative Spend");

        spendSeries.getData().add(new XYChart.Data<>("May", 30000));
        spendSeries.getData().add(new XYChart.Data<>("Jun", 35000));
        spendSeries.getData().add(new XYChart.Data<>("Jul", 40000));
        spendSeries.getData().add(new XYChart.Data<>("Aug", 42000)); // Inflection month
        spendSeries.getData().add(new XYChart.Data<>("Sep", 43000));

        // 2. Create the Revenue Series (The Value Curve)
        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Cumulative Revenue");

        revenueSeries.getData().add(new XYChart.Data<>("May", 10000));
        revenueSeries.getData().add(new XYChart.Data<>("Jun", 25000));
        revenueSeries.getData().add(new XYChart.Data<>("Jul", 38000));
        revenueSeries.getData().add(new XYChart.Data<>("Aug", 45000)); // Crosses above spend!
        revenueSeries.getData().add(new XYChart.Data<>("Sep", 55000));

        // Add the series to the chart
        projectionChart.getData().addAll(spendSeries, revenueSeries);

        // 3. Highlight the Inflection Point (Golden Cross)
        highlightInflectionPoint(revenueSeries, "Aug");
    }

    /**
     * Finds the data point where revenue overtakes spend and adds a visual flair.
     */
    public void injectRealData(ProjectAnalysisResult data) {
        // 1. Extract the budget directly from your data object!
        double originalBudget = data.getOriginalBudget();

        // 2. Top Risk Badge Logic
        if (data.getSuccessProbability() < 40) {
            riskBadge.setText("CRITICAL RISK");
            riskBadge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; -fx-padding: 6 12 6 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 12px;");
        } else if (data.getSuccessProbability() < 70) {
            riskBadge.setText("MODERATE RISK");
            riskBadge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-padding: 6 12 6 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 12px;");
        } else {
            riskBadge.setText("LOW RISK");
            riskBadge.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #059669; -fx-padding: 6 12 6 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 12px;");
        }

        // 3. Main Labels (Variance and Totals)
        double variance = data.getProjectedFinalCost() - originalBudget;

        // Explicitly set the sign so overruns have a '+' and under budget has a '-'
        String sign = variance > 0 ? "+" : (variance < 0 ? "-" : "");

        // Using Locale.US forces the comma format (e.g., $50,000) regardless of system language
        varianceLabel.setText(String.format(java.util.Locale.US, "%s$%,.0f", sign, Math.abs(variance)));

        // Set text and color explicitly based on the variance
        if (variance > 0) {
            varianceSubtext.setText("Expected Overrun");
            varianceLabel.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: #EF4444;"); // Red
        } else if (variance < 0) {
            varianceSubtext.setText("Under Budget");
            varianceLabel.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: #10B981;"); // Green
        } else {
            varianceSubtext.setText("Exactly On Budget");
            varianceLabel.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: #10B981;"); // Green
        }

        // 4. The Grid Details (Forcing Locale.US here too for clean commas)
        projectedTotalLabel.setText(String.format(java.util.Locale.US, "$%,.0f", data.getProjectedFinalCost()));
        allocatedBudgetLabel.setText(String.format(java.util.Locale.US, "$%,.0f", originalBudget));
        inflectionDateLabel.setText(data.getInflectionDate());

        // 5. Trigger the Probability Animation
        animateProbability(data.getSuccessProbability());

        // 6. Populate the Chart with AI Timeline Data
        updateChart(data);
    }

    private void updateChart(ProjectAnalysisResult data) {
        projectionChart.getData().clear();

        XYChart.Series<String, Number> spendSeries = new XYChart.Series<>();
        spendSeries.setName("Projected Spend");

        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Projected Revenue");

        for (ProjectAnalysisResult.TimelinePoint point : data.getTimelineData()) {
            spendSeries.getData().add(new XYChart.Data<>(point.getMonth(), point.getSpend()));
            revenueSeries.getData().add(new XYChart.Data<>(point.getMonth(), point.getRevenue()));
        }

        projectionChart.getData().addAll(spendSeries, revenueSeries);

        // 6. Highlight the Golden Cross if it exists
        if (data.getInflectionDate() != null && !data.getInflectionDate().equalsIgnoreCase("N/A")) {
            highlightInflectionPoint(revenueSeries, data.getInflectionDate());
        }
    }

    /**
     * Finds the data point where revenue overtakes spend and adds a visual flair.
     * Patched to prevent NullPointerExceptions when rendering.
     */
    private void highlightInflectionPoint(XYChart.Series<String, Number> series, String month) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            if (data.getXValue().equals(month)) {

                // If the node is already drawn, style it immediately
                if (data.getNode() != null) {
                    applyHighlightStyle(data.getNode());
                } else {
                    // If JavaFX hasn't drawn it yet, wait for the node property to populate
                    data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                        if (newNode != null) {
                            applyHighlightStyle(newNode);
                        }
                    });
                }
                break; // Found the month, no need to keep looping
            }
        }
    }

    private void applyHighlightStyle(javafx.scene.Node node) {
        node.setStyle(
                "-fx-background-color: #10B981; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-padding: 6px; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(16, 185, 129, 0.6), 10, 0, 0, 0);"
        );
        Tooltip tooltip = new Tooltip("Break-Even Point Reached!\nRevenue exceeds Spend.");
        Tooltip.install(node, tooltip);
    }
}