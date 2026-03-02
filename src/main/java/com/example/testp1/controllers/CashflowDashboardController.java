package com.example.testp1.controllers;

import com.example.testp1.services.CashflowAnalyticsService;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

import java.sql.SQLException;

public class CashflowDashboardController {

    @FXML private PieChart categoryPieChart;
    @FXML private LineChart<String, Number> monthlyExpenseChart;
    @FXML private BarChart<String, Number> projectBurnChart;

    private final CashflowAnalyticsService analyticsService = new CashflowAnalyticsService();

    @FXML
    public void initialize() {
        loadSnapshot();
    }

    @FXML
    private void onBackToOverview() {
        FinanceController.getInstance().loadView("Overviewpage.fxml");
    }

    private void loadSnapshot() {
        try {
            CashflowAnalyticsService.CashflowSnapshot snapshot = analyticsService.buildSnapshot();

            populateCategoryChart(snapshot);
            populateMonthlyChart(snapshot);
            populateProjectBurnChart(snapshot);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void populateCategoryChart(CashflowAnalyticsService.CashflowSnapshot snapshot) {
        categoryPieChart.getData().clear();
        if (snapshot.categoryBreakdown().isEmpty()) {
            categoryPieChart.getData().add(new PieChart.Data("No Data", 1));
            return;
        }

        for (CashflowAnalyticsService.CategoryPoint p : snapshot.categoryBreakdown()) {
            categoryPieChart.getData().add(new PieChart.Data(p.category(), p.amount()));
        }
    }

    private void populateMonthlyChart(CashflowAnalyticsService.CashflowSnapshot snapshot) {
        monthlyExpenseChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Monthly Expenses");

        for (CashflowAnalyticsService.MonthlyExpensePoint p : snapshot.monthlyExpenses()) {
            series.getData().add(new XYChart.Data<>(p.monthLabel(), p.amount()));
        }
        monthlyExpenseChart.getData().add(series);
    }

    private void populateProjectBurnChart(CashflowAnalyticsService.CashflowSnapshot snapshot) {
        projectBurnChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (CashflowAnalyticsService.ProjectBurnPoint p : snapshot.projectBurn()) {
            series.getData().add(new XYChart.Data<>(p.projectName(), p.utilizationPercent()));
        }
        projectBurnChart.getData().add(series);
    }
}
