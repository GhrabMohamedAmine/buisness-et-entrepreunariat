package utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class DialogUtil {

    // ================= BASE METHOD =================
    private static Alert createAlert(Alert.AlertType type, String title, String msg) {

        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);

        alert.initModality(Modality.APPLICATION_MODAL);

        try {
            alert.getDialogPane().getStylesheets().add(
                    DialogUtil.class.getResource("/style.css").toExternalForm()
            );
            alert.getDialogPane().getStyleClass().add("dialog-pane");
        } catch (Exception ignored) {}

        return alert;
    }

    // ================= SUCCESS =================
    public static void success(String title, String msg) {

        Runnable action = () -> {
            Alert alert = createAlert(Alert.AlertType.INFORMATION, title, msg);
            alert.showAndWait();
        };

        if (Platform.isFxApplicationThread())
            action.run();
        else
            Platform.runLater(action);
    }

    // ================= ERROR =================
    public static void error(String title, String msg) {

        Runnable action = () -> {
            Alert alert = createAlert(Alert.AlertType.ERROR, title, msg);
            alert.showAndWait();
        };

        if (Platform.isFxApplicationThread())
            action.run();
        else
            Platform.runLater(action);
    }

    // ================= CONFIRM =================
    public static boolean confirm(String title, String msg) {

        // if already FX thread
        if (Platform.isFxApplicationThread()) {
            Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, msg);
            Optional<ButtonType> r = alert.showAndWait();
            return r.orElse(ButtonType.CANCEL) == ButtonType.OK;
        }

        // if NOT FX thread (ex: services, DB, tests)
        final boolean[] result = {false};
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, msg);
            Optional<ButtonType> r = alert.showAndWait();
            result[0] = r.orElse(ButtonType.CANCEL) == ButtonType.OK;
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException ignored) {}

        return result[0];
    }
}
