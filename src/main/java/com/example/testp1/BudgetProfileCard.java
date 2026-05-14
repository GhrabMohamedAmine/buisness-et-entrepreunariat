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
import org.kordamp.ikonli.javafx.FontIcon;

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
    @FXML private FontIcon toggleIcon, deleteIcon;

    private Consumer<BudgetProfil> onToggle;
    private Consumer<BudgetProfil> onDelete;

    @FXML
    public void initialize() {
        if (hoverActions != null && toggleIcon != null) {
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

    public void setProfileData(BudgetProfil profile, Consumer<BudgetProfil> onToggle, Consumer<BudgetProfil> onDelete) {
        this.budgetProfile = profile;
        this.onToggle = onToggle;
        this.onDelete = onDelete;

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
            toggleIcon.setIconLiteral("mdi2a-archive");
        } else if ("DRAFT".equalsIgnoreCase(profile.getStatus())) {
            statusContainer.getStyleClass().add("status-badge-AR"); // Amber
            toggleIcon.setIconLiteral("mdi2c-check-circle");
        } else {
            statusContainer.getStyleClass().add("status-badge-OB"); // Red/Archived
            toggleIcon.setIconLiteral("mdi2c-check-circle");
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

        RotateTransition rotate = new RotateTransition(duration, toggleIcon);
        rotate.setToAngle(isActive ? 90 : 0);

        ParallelTransition anim = new ParallelTransition(fade, slide, rotate);
        anim.play();
    }

    @FXML
    public void handleToggle() {
        if (onToggle != null && budgetProfile != null) {
            onToggle.accept(budgetProfile);
        }
    }

    @FXML
    public void handleDelete() {
        if (onDelete != null && budgetProfile != null) {
            onDelete.accept(budgetProfile);
        }
    }
}
