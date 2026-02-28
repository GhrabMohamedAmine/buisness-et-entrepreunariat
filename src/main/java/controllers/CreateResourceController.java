package controllers;

import entities.Resource;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.ResourceService;

import java.io.File;
import java.nio.file.*;

public class CreateResourceController {

    @FXML private TextField nameField;
    @FXML private TextField quantityField;
    @FXML private TextField codeField;
    @FXML private TextField unitcostField;
    @FXML private TextField avquantField;

    @FXML private ComboBox<String> typeCombo;

    // ✅ add this in FXML: <ImageView fx:id="imagePreview" .../>
    @FXML private ImageView imagePreview;

    private final ResourceService service = new ResourceService();

    // Will be stored in DB as image_path
    private String selectedImagePath = null;

    @FXML
    public void initialize() {
        typeCombo.getItems().addAll("PHYSICAL", "SOFTWARE");
        typeCombo.getSelectionModel().selectFirst();

        // Optional: placeholder preview
        if (imagePreview != null) {
            imagePreview.setImage(null);
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) codeField.getScene().getWindow()).close();
    }

    // ✅ Add a "Choose..." button in FXML with onAction="#chooseImage"
    @FXML
    private void chooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Resource Image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = (Stage) codeField.getScene().getWindow();
        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        try {
            // Save to a stable folder (recommended)
            Path destDir = Paths.get(System.getProperty("user.home"),
                    "eco_adventure_uploads", "resources");
            Files.createDirectories(destDir);

            String fileName = file.getName();
            String ext = "";
            int dot = fileName.lastIndexOf('.');
            if (dot >= 0) ext = fileName.substring(dot);

            String newName = "res_" + System.currentTimeMillis() + ext;
            Path dest = destDir.resolve(newName);

            Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            selectedImagePath = dest.toString();

            if (imagePreview != null) {
                imagePreview.setImage(new Image(dest.toUri().toString(), true));
            }

        } catch (Exception e) {
            showError("Image error", "Failed to import image:\n" + e.getMessage());
        }
    }

    @FXML
    void handleAdd() {
        try {
            // Basic validation
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isEmpty()) throw new IllegalArgumentException("Resource name is required.");

            String type = typeCombo.getValue();
            if (type == null || type.isBlank()) throw new IllegalArgumentException("Type is required.");

            int code = parseInt(codeField.getText(), "Resource code");
            double unitcost = parseDouble(unitcostField.getText(), "Unit cost");
            int quantity = parseInt(quantityField.getText(), "Total quantity");
            double avquant = parseDouble(avquantField.getText(), "Available quantity");

            if (quantity < 0) throw new IllegalArgumentException("Total quantity cannot be negative.");
            if (avquant < 0) throw new IllegalArgumentException("Available quantity cannot be negative.");
            if (avquant > quantity) throw new IllegalArgumentException("Available quantity cannot exceed total quantity.");

            // Your constructor: (name, code, unitcost, type, quantity, avquant)
            Resource r = new Resource(name, code, unitcost, type, quantity, avquant);

            // ✅ You MUST add this field in Resource:
            // private String imagePath;
            // getter/setter
            r.setImagePath(selectedImagePath);

            service.add(r);

            showInfo("Success", "Resource added successfully!");

            clearForm();

        } catch (Exception e) {
            showError("Add resource failed", "Error: " + e.getMessage());
        }
    }

    private void clearForm() {
        nameField.clear();
        quantityField.clear();
        avquantField.clear();
        codeField.clear();
        unitcostField.clear();
        typeCombo.getSelectionModel().selectFirst();

        selectedImagePath = null;
        if (imagePreview != null) imagePreview.setImage(null);
    }

    private int parseInt(String value, String fieldName) {
        try {
            if (value == null) throw new NumberFormatException();
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " must be a valid integer.");
        }
    }

    private double parseDouble(String value, String fieldName) {
        try {
            if (value == null) throw new NumberFormatException();
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}