package com.example.testp1;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.TilePane;
import java.io.IOException;

public class MarketHubController {

    @FXML private TilePane majorGrid, regionalGrid;
    @FXML private Button majorTabBtn, regionalTabBtn;
    @FXML private ScrollPane majorContent, regionalContent;
    @FXML private Label breadcrumbProjectName;

    public void initialize() {
        // 1. Set default view
        showMajorMarkets();

        // 2. Fill with dummy data just to test the visual layout
        createTestCards();
    }

    private void createTestCards() {
        // We load 4 cards manually just to see if the TilePane works
        for (int i = 0; i < 4; i++) {
            addMockCard(majorGrid, "USD", "US Dollar", "United States", "0.322");
            addMockCard(regionalGrid, "SAR", "Saudi Riyal", "Saudi Arabia", "1.210");
        }
    }

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
    private void handleBackToOverview() {
        FinanceController.getInstance().loadView("Overviewpage.fxml");
        System.out.println("Navigation: Back to Financial Overview");
        // Add your screen switcher logic here later
    }

    @FXML
    private void handleRefreshData() {
        System.out.println("Refresh clicked - This will call the API later");
    }
}