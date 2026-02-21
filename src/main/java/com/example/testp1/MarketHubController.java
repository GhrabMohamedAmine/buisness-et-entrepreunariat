package com.example.testp1;

import com.example.testp1.entities.CurrencyResponse;
import com.example.testp1.services.ServiceMarketingHub;
import com.example.utils.CurrencyDetails;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.TilePane;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class MarketHubController {

    public Label lastSyncLabel;
    @FXML private TilePane majorGrid, regionalGrid;
    @FXML private Button majorTabBtn, regionalTabBtn;
    @FXML private ScrollPane majorContent, regionalContent;
    @FXML private Label breadcrumbProjectName;

    private final ServiceMarketingHub marketService = new ServiceMarketingHub();

    public void initialize() {
        // 1. Set default view
        showMajorMarkets();

        // 2. Fill with dummy data just to test the visual layout
        //createTestCards();
        loadMarketData();
    }

    private void loadMarketData() {
        // We use TND as the base since Nexum is tailored for the local context
        marketService.getLiveExchangeRates("TND").thenAccept(data -> {

            // ALWAYS use Platform.runLater when updating UI from an Async call
            Platform.runLater(() -> {
                updateUI(data);
                lastSyncLabel.setText("Last Sync: " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            });

        }).exceptionally(ex -> {
            Platform.runLater(() -> System.err.println("Failed to load rates: " + ex.getMessage()));
            return null;
        });
    }

    private void updateUI(CurrencyResponse data) {
        // Populate the 20 Global Currencies
        populateGrid(majorGrid, CurrencyDetails.getMajorCodes(), data.rates);

        // Populate the 20 Regional Currencies
        populateGrid(regionalGrid, CurrencyDetails.getRegionalCodes(), data.rates);
    }

    private void populateGrid(TilePane grid, Set<String> codes, Map<String, Double> rates) {
        grid.getChildren().clear();
        System.out.println("API returned " + rates.size() + " currencies.");

        if (rates == null) {
            System.err.println("CRITICAL: Rates Map is NULL. API mapping failed.");
            return;
        }

        for (String code : codes) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("CurrencyCard.fxml"));
                Node card = loader.load();

                CurrencyCardController controller = loader.getController();

                // Pull details from our Helper and rate from the API Map
                String name = CurrencyDetails.getName(code);
                String country = CurrencyDetails.getCountry(code);
                Double rate = rates.get(code);

                if (rate != null) {
                    controller.setData(code, name, country, rate);
                    grid.getChildren().add(card);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    private void createTestCards() {
//        // We load 4 cards manually just to see if the TilePane works
//        for (int i = 0; i < 4; i++) {
//            addMockCard(majorGrid, "USD", "US Dollar", "United States", "0.322");
//            addMockCard(regionalGrid, "SAR", "Saudi Riyal", "Saudi Arabia", "1.210");
//        }
//    }

    private void addMockCard(TilePane grid, String code, String name, String country, String rate) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("CurrencyCard.fxml"));
            Node card = loader.load();
            CurrencyCardController controller = loader.getController();
            controller.setData(code, name, country, Double.parseDouble(rate));
            grid.getChildren().add(card);
        } catch (IOException e) {
            System.err.println("Could not load CurrencyCard: " + e.getMessage());
        }
    }

    @FXML
    private void showMajorMarkets() {
        majorContent.setVisible(true); majorContent.setManaged(true);
        regionalContent.setVisible(false); regionalContent.setManaged(false);
        majorTabBtn.getStyleClass().setAll("tab-active");
        regionalTabBtn.getStyleClass().setAll("tab-inactive");
    }

    @FXML
    private void showRegionalMarkets() {
        regionalContent.setVisible(true); regionalContent.setManaged(true);
        majorContent.setVisible(false); majorContent.setManaged(false);
        regionalTabBtn.getStyleClass().setAll("tab-active");
        majorTabBtn.getStyleClass().setAll("tab-inactive");
    }

    @FXML
    private void handleRefreshData() {
        // Reuse the logic for the refresh icon
        loadMarketData();
    }


    @FXML
    private void handleBackToOverview() {
        FinanceController.getInstance().loadView("Overviewpage.fxml");
        System.out.println("Navigation: Back to Financial Overview");
        // Add your screen switcher logic here later
    }

}