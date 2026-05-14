package com.example.testp1.controllers;

import com.example.testp1.BudgetProfileCard;
import com.example.testp1.entities.BudgetProfil;
import com.example.testp1.services.ServiceBudgetProfil;
//import com.example.testp1.FinanceController;
import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;

import java.sql.SQLException;
import java.util.List;

public class BudgetProfileSelectionController {

    @FXML private FlowPane profilesFlowPane;

    private ServiceBudgetProfil service = new ServiceBudgetProfil();

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
        try {
            profilesFlowPane.getChildren().clear();
            List<BudgetProfil> all = service.getAll();
            for (BudgetProfil p : all) {
                BudgetProfileCard card = new BudgetProfileCard();
                card.setProfileData(p, this::toggleStatus, this::deleteProfile);
                profilesFlowPane.getChildren().add(card);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void toggleStatus(BudgetProfil p) {
        try {
            if ("ACTIVE".equalsIgnoreCase(p.getStatus())) {
                p.setStatus("ARCHIVED");
            } else {
                p.setStatus("ACTIVE");
            }
            service.update(p);
            loadData();
        } catch (SQLException e) {
            System.err.println("Could not toggle status: " + e.getMessage());
        }
    }

    private void deleteProfile(BudgetProfil p) {
        try {
            service.delete(p);
            loadData();
        } catch (SQLException e) {
            System.err.println("Could not delete profile: " + e.getMessage());
        }
    }

    @FXML
    public void handleBack() {
        FinanceController.getInstance().loadView("Overviewpage.fxml");
    }
}