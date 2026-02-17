package com.example.testp1;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.PasswordTextField;
import com.example.utils.PasswordFieldHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;

public class SigninController {
    @FXML
    private PasswordTextField signup_pwd;
    @FXML
    private CustomTextField fullNameField;
    @FXML
    private PasswordTextField confirm_signup_pwd;
    @FXML
    private CustomTextField departmentField;
    @FXML
    private ComboBox jobRoleCombo;
    @FXML
    private StackPane avatarContainer;
    @FXML
    private CustomTextField email;
    public void initialize(){
        PasswordFieldHelper.setupPasswordToggle(signup_pwd);
        PasswordFieldHelper.setupPasswordToggle(confirm_signup_pwd);

        jobRoleCombo.setItems(FXCollections.observableArrayList(
                "Project Manager",
                "Product Owner",
                "UI/UX Designer",
                "Full-Stack Developer",
                "Quality Assurance",
                "Stakeholder"
        ));


    }

}
