package controllers;

import entities.Resource;
import javafx.scene.control.ComboBox;
import services.ResourceService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CreateResourceController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField typeField;

    @FXML
    private TextField quantityField;

    @FXML
    private TextField codeField;

    @FXML
    private TextField unitcostField;

    @FXML
    private TextField avquantField;

    @FXML private ComboBox<String> typeCombo;


    private ResourceService service = new ResourceService();

    @FXML
    public void initialize() {
        typeCombo.getItems().addAll("PHYSICAL", "SOFTWARE");
    }
    @FXML
    private void handleCancel() {
        ((Stage) codeField.getScene().getWindow()).close();
    }


    @FXML
    void handleAdd() {
        try {
            String name = nameField.getText();
            int code = Integer.parseInt(codeField.getText());
            double unitcost =  Double.parseDouble(unitcostField.getText());
            String type = typeCombo.getValue();

            int quantity = Integer.parseInt(quantityField.getText());
            double avquant = Double.parseDouble(avquantField.getText());

            Resource r = new Resource(name, code, unitcost, type, quantity, avquant);
            service.add(r);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Resource added successfully!");
            alert.show();

            nameField.clear();
            quantityField.clear();
            avquantField.clear();
            codeField.clear();
            unitcostField.clear();

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Error: " + e.getMessage());
            alert.show();
        }
    }
}
