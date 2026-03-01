package com.example.testp1.controllers;

import com.example.testp1.entities.ProjectBudget;
import com.example.testp1.entities.Transaction;
import com.example.testp1.services.ServiceProjectBudget;
import com.example.testp1.services.ServiceTransaction;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.sql.SQLException;

public class PBOverviewController implements Initializable {
    private static final String[] CATEGORY_COLORS = {
            "#6C5CE7", "#10B981", "#F59E0B", "#EF4444", "#3B82F6", "#A855F7", "#14B8A6", "#F97316"
    };

    @FXML
    private PieChart expensePieChart;

    @FXML
    private BarChart<String, Number> transactionBarChart;
    @FXML
    private FlowPane barCategoryLegend;
    @FXML
    private AreaChart<String, Number> runwayOverviewChart;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Enable default chart animations for the dynamic growing effect
        expensePieChart.setAnimated(true);
        transactionBarChart.setAnimated(true);

        // Delay the animation slightly so it starts right after the UI renders
        Platform.runLater(this::loadAndAnimateData);
    }

    private void loadAndAnimateData() {
        loadAndAnimatePieChart();
        loadAndAnimateBarChart();
        loadAndStyleRunwayChart();
    }

    private void loadAndAnimatePieChart() {
        ObservableList<PieChart.Data> pieData = fetchPieDataFromExpenses();
        expensePieChart.setData(pieData);

        // 2. Wait exactly 1 frame for JavaFX to physically draw the shapes
        Platform.runLater(() -> {
            for (int i = 0; i < pieData.size(); i++) {
                PieChart.Data data = pieData.get(i);
                if (data.getNode() != null) {
                    data.getNode().setStyle("-fx-pie-color: " + pickColor(i) + ";");
                }
            }

            // --- Animate the Pie Chart (Pop-in scale) ---
            // Pure radial wipes are nearly impossible in pure JavaFX without heavy custom drawing,
            // so a smooth staggered pop-in is the industry standard for JavaFX pie charts.
            int delay = 0;
            for (Node slice : expensePieChart.lookupAll(".chart-pie")) {
                slice.setScaleX(0);
                slice.setScaleY(0);

                javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(500), slice);
                st.setToX(1);
                st.setToY(1);
                st.setDelay(Duration.millis(delay));
                st.play();

                delay += 100; // Stagger each slice by 100ms
            }
        });
    }

    private ObservableList<PieChart.Data> fetchPieDataFromExpenses() {
        int budgetId = FinanceController.getCurrentBudgetId();
        if (budgetId <= 0) {
            return FXCollections.observableArrayList(new PieChart.Data("No Expenses", 1));
        }

        try {
            ServiceTransaction transactionService = new ServiceTransaction();
            List<Transaction> transactions = transactionService.getTransactionsByBudget(budgetId);
            if (transactions == null || transactions.isEmpty()) {
                return FXCollections.observableArrayList(new PieChart.Data("No Expenses", 1));
            }

            Map<String, Double> totalsByCategory = new LinkedHashMap<>();
            for (Transaction t : transactions) {
                if (t == null) {
                    continue;
                }
                String category = t.getExpenseCategory();
                if (category == null || category.isBlank()) {
                    category = "Other";
                }
                totalsByCategory.merge(category, Math.max(0.0, t.getCost()), Double::sum);
            }

            ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
            for (Map.Entry<String, Double> entry : totalsByCategory.entrySet()) {
                if (entry.getValue() > 0) {
                    data.add(new PieChart.Data(entry.getKey(), entry.getValue()));
                }
            }

            if (data.isEmpty()) {
                data.add(new PieChart.Data("No Expenses", 1));
            }
            return data;
        } catch (SQLException e) {
            e.printStackTrace();
            return FXCollections.observableArrayList(new PieChart.Data("No Expenses", 1));
        }
    }

    private void loadAndAnimateBarChart() {
        XYChart.Series<String, Number> barSeries = fetchExpenseAmountSeriesByCategory();
        transactionBarChart.getData().setAll(barSeries);
        styleBarChartAxes();
        renderCategoryLegend(barSeries);

        // 2. Wait exactly 1 frame for JavaFX to physically draw the shapes
        Platform.runLater(() -> {
            // --- Animate the Bar Chart (Grow from X-Axis like bamboo) ---
            for (int i = 0; i < barSeries.getData().size(); i++) {
                XYChart.Data<String, Number> data = barSeries.getData().get(i);
                Node bar = data.getNode();
                if (bar == null) {
                    continue;
                }
                // Get the actual height of the fully drawn bar
                double height = bar.getBoundsInParent().getHeight();

                // Squish it flat, then push it down so it rests exactly on the X-axis
                bar.setScaleY(0);
                bar.setTranslateY(height / 2.0);
                bar.setStyle("-fx-bar-fill: " + pickColor(i) + ";");

                // Animate the scale back to 100%
                javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(800), bar);
                st.setToY(1);

                // Simultaneously animate the position back to its true center
                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(Duration.millis(800), bar);
                tt.setToY(0);

                // Run them together for the locked bottom-edge effect
                javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(st, tt);
                pt.setDelay(Duration.millis(200));
                pt.play();
            }
        });
    }

    private void styleBarChartAxes() {
        if (!(transactionBarChart.getXAxis() instanceof CategoryAxis xAxis)) {
            return;
        }
        if (!(transactionBarChart.getYAxis() instanceof NumberAxis yAxis)) {
            return;
        }

        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setStartMargin(18);
        xAxis.setEndMargin(18);
        xAxis.setGapStartAndEnd(true);
        xAxis.setTickLabelFont(Font.font("Inter", 10));
        yAxis.setTickLabelFont(Font.font("Inter", 11));

        transactionBarChart.setCategoryGap(26);
        transactionBarChart.setBarGap(8);
        transactionBarChart.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #FFFFFF, #FAFAFF);" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 8;"
        );
    }

    private void renderCategoryLegend(XYChart.Series<String, Number> series) {
        if (barCategoryLegend == null) {
            return;
        }
        barCategoryLegend.getChildren().clear();

        for (int i = 0; i < series.getData().size(); i++) {
            XYChart.Data<String, Number> point = series.getData().get(i);
            String color = pickColor(i);
            String text = String.format(
                    Locale.US,
                    "%s: $%,.0f",
                    point.getXValue(),
                    point.getYValue().doubleValue()
            );
            Label label = new Label(text);
            label.setStyle(
                    "-fx-font-size: 11px;" +
                            "-fx-text-fill: #334155;" +
                            "-fx-background-color: #F8FAFC;" +
                            "-fx-background-radius: 10;" +
                            "-fx-border-color: " + color + ";" +
                            "-fx-border-width: 0 0 0 3;" +
                            "-fx-border-radius: 10;" +
                            "-fx-padding: 4 8 4 8;"
            );
            barCategoryLegend.getChildren().add(label);
        }
    }

    private String pickColor(int index) {
        return CATEGORY_COLORS[index % CATEGORY_COLORS.length];
    }

    private XYChart.Series<String, Number> fetchExpenseAmountSeriesByCategory() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        int budgetId = FinanceController.getCurrentBudgetId();
        if (budgetId <= 0) {
            series.getData().add(new XYChart.Data<>("No Data", 0));
            return series;
        }

        try {
            ServiceTransaction transactionService = new ServiceTransaction();
            List<Transaction> transactions = transactionService.getTransactionsByBudget(budgetId);
            if (transactions == null || transactions.isEmpty()) {
                series.getData().add(new XYChart.Data<>("No Expenses", 0));
                return series;
            }

            Map<String, Double> totalsByCategory = new LinkedHashMap<>();
            for (Transaction t : transactions) {
                if (t == null) {
                    continue;
                }
                String category = t.getExpenseCategory();
                if (category == null || category.isBlank()) {
                    category = "Other";
                }
                totalsByCategory.merge(category, Math.max(0.0, t.getCost()), Double::sum);
            }

            for (Map.Entry<String, Double> entry : totalsByCategory.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            series.getData().clear();
            series.getData().add(new XYChart.Data<>("Error", 0));
        }
        return series;
    }

    private void loadAndStyleRunwayChart() {
        double[] metrics = fetchBudgetMetrics();
        double totalBudget = metrics[0];
        double currentSpend = metrics[1];

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        String[] months = {"M+1", "M+2", "M+3", "M+4", "M+5", "M+6"};

        double burnBase = Math.max(totalBudget * 0.05, currentSpend / 4.0);
        double cumulativeSpend = currentSpend;

        for (int i = 0; i < months.length; i++) {
            double monthlyBurn = burnBase * (1.0 + 0.06 * (i + 1));
            cumulativeSpend += monthlyBurn;

            double remainingPct = ((totalBudget - cumulativeSpend) / totalBudget) * 100.0;
            remainingPct = Math.max(0.0, Math.min(100.0, remainingPct));
            series.getData().add(new XYChart.Data<>(months[i], remainingPct));
        }

        runwayOverviewChart.getData().setAll(series);
        runwayOverviewChart.setCreateSymbols(true);
        runwayOverviewChart.setHorizontalGridLinesVisible(true);
        runwayOverviewChart.setVerticalGridLinesVisible(false);
        runwayOverviewChart.setAlternativeColumnFillVisible(false);
        runwayOverviewChart.setAlternativeRowFillVisible(false);
        runwayOverviewChart.setAnimated(false);
        runwayOverviewChart.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #FFFFFF, #F7FBFF);" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 10;"
        );

        Platform.runLater(() -> {
            Node line = series.getNode();
            if (line != null) {
                line.setStyle("-fx-stroke: #2563EB; -fx-stroke-width: 3px;");
            }
            Node areaFill = runwayOverviewChart.lookup(".chart-series-area-fill");
            if (areaFill != null) {
                areaFill.setStyle("-fx-fill: rgba(37,99,235,0.18);");
            }

            for (XYChart.Data<String, Number> point : series.getData()) {
                if (point.getNode() == null) {
                    continue;
                }
                double v = point.getYValue().doubleValue();
                String color = v >= 40 ? "#16A34A" : (v >= 15 ? "#F59E0B" : "#DC2626");
                point.getNode().setStyle(
                        "-fx-background-color: white, " + color + ";" +
                                "-fx-background-insets: 0, 2;" +
                                "-fx-background-radius: 8px;" +
                                "-fx-padding: 6px;"
                );
                double remainingAmount = Math.max(0.0, totalBudget * v / 100.0);
                Tooltip.install(
                        point.getNode(),
                        new Tooltip(String.format(
                                Locale.US,
                                "Runway%n%s: %.1f%%%nRemaining: $%,.0f",
                                point.getXValue(),
                                v,
                                remainingAmount
                        ))
                );
            }
        });
    }

    private double[] fetchBudgetMetrics() {
        int budgetId = FinanceController.getCurrentBudgetId();
        if (budgetId <= 0) {
            return new double[]{20000.0, 0.0};
        }

        try {
            ServiceProjectBudget service = new ServiceProjectBudget();
            ProjectBudget budget = service.getById(budgetId);
            if (budget != null && budget.getTotalBudget() > 0) {
                return new double[]{
                        budget.getTotalBudget(),
                        Math.max(0.0, budget.getActualSpend())
                };
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new double[]{20000.0, 0.0};
    }
}
