package controllers;

import Mains.MainFX;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import services.AnalyticsService;
import services.MonthlyUsage;
import services.TypeForecastRow;

import java.sql.SQLException;
import java.util.List;

public class DemandForecastController {

    @FXML private ComboBox<String> typeBox;

    @FXML private Label predictedLabel;
    @FXML private Label availableLabel;
    @FXML private Label riskLabel;

    @FXML private LineChart<String, Number> usageChart;

    @FXML private TableView<TypeForecastRow> forecastTable;
    @FXML private TableColumn<TypeForecastRow, String> typeCol;
    @FXML private TableColumn<TypeForecastRow, Integer> availableCol;
    @FXML private TableColumn<TypeForecastRow, Integer> predictedCol;
    @FXML private TableColumn<TypeForecastRow, String> riskCol;

    private final AnalyticsService analytics = new AnalyticsService();

    @FXML
    public void initialize() {
        // table columns
        typeCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getType()));
        availableCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getAvailableQuantity()));
        predictedCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getPredictedDemand()));
        riskCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getRisk()));

        // optional: style risk rows
        riskCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                if (empty || item == null) {
                    setStyle("");
                } else if ("HIGH".equalsIgnoreCase(item)) {
                    setStyle("-fx-text-fill: #b00020; -fx-font-weight: bold;");
                } else if ("MEDIUM".equalsIgnoreCase(item)) {
                    setStyle("-fx-text-fill: #b26a00; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #1e5631; -fx-font-weight: bold;");
                }
            }
        });

        refresh(); // load everything once
    }

    @FXML
    private void backToResources() {
        MainFX.loadPage("/back/manage-resources.fxml");
    }

    @FXML
    private void refresh() {
        try {
            // load types once
            if (typeBox.getItems().isEmpty()) {
                List<String> types = analytics.getAllTypes();
                typeBox.setItems(FXCollections.observableArrayList(types));
                if (!types.isEmpty()) typeBox.getSelectionModel().select(0);
            }

            // fill table of all types
            List<TypeForecastRow> all = analytics.forecastAllTypes();
            forecastTable.setItems(FXCollections.observableArrayList(all));

            // update selected type UI + chart
            onTypeChanged();

        } catch (SQLException e) {
            showError("Database Error", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onTypeChanged() {
        String type = typeBox.getValue();
        if (type == null || type.isBlank()) return;

        try {
            TypeForecastRow row = analytics.forecastType(type);

            predictedLabel.setText(String.valueOf(row.getPredictedDemand()));
            availableLabel.setText(String.valueOf(row.getAvailableQuantity()));
            riskLabel.setText("RISK: " + row.getRisk());

            loadChart(type);

        } catch (SQLException e) {
            showError("Database Error", e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadChart(String type) throws SQLException {
        usageChart.getData().clear();

        List<MonthlyUsage> usage = analytics.getMonthlyUsageByType(type, 12);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(type);

        for (MonthlyUsage u : usage) {
            series.getData().add(new XYChart.Data<>(u.getMonth(), u.getQuantity()));
        }

        usageChart.getData().add(series);
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}