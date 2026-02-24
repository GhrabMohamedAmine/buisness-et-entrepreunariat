package com.example.testp1.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class PBOverviewController implements Initializable {

    @FXML
    private PieChart expensePieChart;

    @FXML
    private BarChart<String, Number> transactionBarChart;

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
    }

    private void loadAndAnimatePieChart() {
        // 1. Load the real final data immediately
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Software", 4500.0),
                new PieChart.Data("Marketing", 2000.0),
                new PieChart.Data("Office", 800.0),
                new PieChart.Data("Travel", 1200.0)
        );
        expensePieChart.setData(pieData);

        // 2. Wait exactly 1 frame for JavaFX to physically draw the shapes
        Platform.runLater(() -> {
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

    private void loadAndAnimateBarChart() {
        // 1. Load the real final data immediately
        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        barSeries.getData().add(new XYChart.Data<>("Software", 3));
        barSeries.getData().add(new XYChart.Data<>("Marketing", 5));
        barSeries.getData().add(new XYChart.Data<>("Office", 24));
        barSeries.getData().add(new XYChart.Data<>("Travel", 4));
        transactionBarChart.getData().add(barSeries);

        // 2. Wait exactly 1 frame for JavaFX to physically draw the shapes
        Platform.runLater(() -> {
            // --- Animate the Bar Chart (Grow from X-Axis like bamboo) ---
            for (Node bar : transactionBarChart.lookupAll(".chart-bar")) {
                // Get the actual height of the fully drawn bar
                double height = bar.getBoundsInParent().getHeight();

                // Squish it flat, then push it down so it rests exactly on the X-axis
                bar.setScaleY(0);
                bar.setTranslateY(height / 2.0);

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
}