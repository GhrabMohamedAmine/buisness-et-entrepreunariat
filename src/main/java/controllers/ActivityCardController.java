package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

public class ActivityCardController {
    @FXML private StackPane iconWrap;
    @FXML private FontIcon activityIcon;
    @FXML private Label titleLabel;
    @FXML private Label dateLabel;
    @FXML private Label messageLabel;

    public void setData(String title, String message, String dateText, String iconStyleClass) {
        titleLabel.setText(title == null ? "" : title);
        messageLabel.setText(message == null ? "" : message);
        dateLabel.setText(dateText == null ? "Date unavailable" : dateText);

        if (iconStyleClass == null || iconStyleClass.isBlank()) {
            iconWrap.setManaged(false);
            iconWrap.setVisible(false);
            return;
        }

        iconWrap.setManaged(true);
        iconWrap.setVisible(true);
        activityIcon.getStyleClass().removeAll(
                "activity-icon-created",
                "activity-icon-completed",
                "activity-icon-updated",
                "activity-icon-deleted"
        );
        activityIcon.getStyleClass().add(iconStyleClass);
        activityIcon.setIconLiteral(resolveIconLiteral(iconStyleClass));
    }

    private String resolveIconLiteral(String iconStyleClass) {
        if ("activity-icon-completed".equals(iconStyleClass)) return "mdi2c-check-circle-outline";
        if ("activity-icon-updated".equals(iconStyleClass)) return "mdi2p-pencil-outline";
        if ("activity-icon-deleted".equals(iconStyleClass)) return "mdi2d-delete-outline";
        return "mdi2p-plus-circle-outline";
    }
}
