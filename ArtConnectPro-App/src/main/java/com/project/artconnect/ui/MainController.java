package com.project.artconnect.ui;

import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.application.Platform;

public class MainController {
    @FXML
    private TabPane mainTabPane;
    @FXML
    private ComboBox<UserRole> roleSelector;
    @FXML
    private Label roleDescriptionLabel;

    @FXML
    public void initialize() {
        roleSelector.setItems(FXCollections.observableArrayList(UserRole.values()));
        roleSelector.valueProperty().bindBidirectional(UserSession.activeRoleProperty());
        UserSession.activeRoleProperty().addListener((obs, oldRole, newRole) -> applyRole(newRole));
        applyRole(UserSession.getActiveRole());
    }

    private void applyRole(UserRole role) {
        roleDescriptionLabel.setText(role.getDescription());
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }
}
