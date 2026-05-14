package com.example.testp1;

import com.example.testp1.entities.BudgetProfil;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class BudgetProfileCard extends AnchorPane {
    private BudgetProfil budgetProfile;

    @FXML private Label yearLabel, currencyLabel, statusLabel;
    @FXML private Label totalBudgetLabel, actualSpendLabel, remainingLabel, periodLabel;
    @FXML private StackPane statusContainer;
    @FXML private HBox hoverActions;

    private Consumer<BudgetProfil> onViewDashboard;

    @FXML
    public void initialize() {
        if (hoverActions != null) {
            this.setOnMouseEntered(e -> runHoverEffect(true));
            this.setOnMouseExited(e -> runHoverEffect(false));
        }
    }

    public BudgetProfileCard() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/testp1/BudgetProfileCard.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setProfileData(BudgetProfil profile, Consumer<BudgetProfil> onViewDashboard) {
        this.budgetProfile = profile;
        this.onViewDashboard = onViewDashboard;

        yearLabel.setText("Fiscal Year " + profile.getFiscalYear().getValue());
        currencyLabel.setText("Base: " + profile.getBaseCurrency());
        statusLabel.setText(profile.getStatus());

        BigDecimal budget = profile.getBudgetDisposable() != null ? profile.getBudgetDisposable() : BigDecimal.ZERO;
        BigDecimal spent = profile.getTotalExpense() != null ? profile.getTotalExpense() : BigDecimal.ZERO;
        BigDecimal remaining = budget.subtract(spent);

        totalBudgetLabel.setText(String.format("$%,.0f", budget));
        actualSpendLabel.setText(String.format("$%,.0f", spent));
        remainingLabel.setText(String.format("$%,.0f", remaining));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        String start = profile.getStartDate() != null ? profile.getStartDate().format(formatter) : "N/A";
        String end = profile.getEndDate() != null ? profile.getEndDate().format(formatter) : "N/A";
        periodLabel.setText(start + " - " + end);

        // Logic to swap status colors
        statusContainer.getStyleClass().removeAll("status-badge-OT", "status-badge-AR", "status-badge-OB");
        if ("ACTIVE".equalsIgnoreCase(profile.getStatus())) {
            statusContainer.getStyleClass().add("status-badge-OT"); // Green
        } else if ("DRAFT".equalsIgnoreCase(profile.getStatus())) {
            statusContainer.getStyleClass().add("status-badge-AR"); // Amber
        } else {
            statusContainer.getStyleClass().add("status-badge-OB"); // Red/Archived
        }
    }

    private void runHoverEffect(boolean isActive) {
        Duration duration = Duration.millis(300);

        FadeTransition fade = new FadeTransition(duration, hoverActions);
        fade.setFromValue(isActive ? 0.0 : 1.0);
        fade.setToValue(isActive ? 1.0 : 0.0);

        TranslateTransition slide = new TranslateTransition(duration, hoverActions);
        slide.setFromX(isActive ? 15.0 : 0.0);
        slide.setToX(isActive ? 0.0 : 15.0);

        ParallelTransition anim = new ParallelTransition(fade, slide);
        anim.play();
    }

    @FXML
    public void handleViewDashboard() {
        if (onViewDashboard != null && budgetProfile != null) {
            onViewDashboard.accept(budgetProfile);
        }
    }
}
