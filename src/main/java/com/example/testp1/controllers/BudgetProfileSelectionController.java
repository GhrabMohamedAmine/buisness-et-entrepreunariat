package com.example.testp1.controllers;

import com.example.testp1.AddBudgetProfile;
import com.example.testp1.BudgetProfileCard;
import com.example.testp1.entities.BudgetProfil;
import com.example.testp1.services.ServiceBudgetProfil;
import com.example.testp1.controllers.FinanceController;
import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.AnchorPane;

import java.sql.SQLException;
import java.util.List;

public class BudgetProfileSelectionController {

    @FXML private FlowPane profilesFlowPane;
    @FXML private AnchorPane mainPageWrapper;
    @FXML private AddBudgetProfile addBudgetProfile;

    private ServiceBudgetProfil service = new ServiceBudgetProfil();

    @FXML
    public void initialize() {
        if (addBudgetProfile != null) {
            addBudgetProfile.setOnSaveSuccess(this::loadData);
        }
        loadData();
    }

    private void loadData() {
        try {
            profilesFlowPane.getChildren().clear();
            List<BudgetProfil> all = service.getAll();
            for (BudgetProfil p : all) {
                BudgetProfileCard card = new BudgetProfileCard();
                card.setProfileData(p, this::viewDashboard);
                profilesFlowPane.getChildren().add(card);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void viewDashboard(BudgetProfil p) {
        FinanceController.setCurrentProfileId(p.getId());
        FinanceController.getInstance().loadView("Overviewpage.fxml");
    }

    @FXML
    public void handleAddProfile() {
        if (addBudgetProfile != null && mainPageWrapper != null) {
            addBudgetProfile.show(mainPageWrapper);
        }
    }
}