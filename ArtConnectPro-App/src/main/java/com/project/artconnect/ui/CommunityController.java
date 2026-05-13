package com.project.artconnect.ui;

import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.service.CommunityService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

public class CommunityController {
    @FXML
    private TableView<CommunityMember> memberTable;
    @FXML
    private TableColumn<CommunityMember, String> nameColumn;
    @FXML
    private TableColumn<CommunityMember, String> emailColumn;
    @FXML
    private TableColumn<CommunityMember, String> cityColumn;

    private final CommunityService communityService = ServiceProvider.getCommunityService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));

        refreshTable();
    }

    @FXML
    private void handleAddMember() {
        showMemberDialog(null).ifPresent(member -> {
            communityService.createMember(member);
            refreshTable();
        });
    }

    @FXML
    private void handleEditMember() {
        CommunityMember selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a member to edit.");
            return;
        }
        showMemberDialog(selected).ifPresent(member -> {
            communityService.updateMember(member);
            refreshTable();
        });
    }

    @FXML
    private void handleDeleteMember() {
        CommunityMember selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a member to delete.");
            return;
        }
        if (confirm("Delete member", "Delete " + selected.getName() + "?")) {
            communityService.deleteMember(selected.getEmail());
            refreshTable();
        }
    }

    private void refreshTable() {
        memberTable.setItems(FXCollections.observableArrayList(communityService.getAllMembers()));
    }

    private java.util.Optional<CommunityMember> showMemberDialog(CommunityMember existing) {
        Dialog<CommunityMember> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Member" : "Edit Member");
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        TextField emailField = new TextField(existing != null ? existing.getEmail() : "");
        TextField cityField = new TextField(existing != null ? existing.getCity() : "");
        TextField typeField = new TextField(existing != null ? existing.getMembershipType() : "FREE");
        if (existing != null) {
            emailField.setDisable(true);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Name"), nameField);
        grid.addRow(1, new Label("Email"), emailField);
        grid.addRow(2, new Label("City"), cityField);
        grid.addRow(3, new Label("Membership"), typeField);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != saveButton) return null;
            CommunityMember member = existing != null ? existing : new CommunityMember();
            member.setName(nameField.getText().trim());
            member.setEmail(emailField.getText().trim());
            member.setCity(cityField.getText().trim());
            member.setMembershipType(typeField.getText().trim());
            return member;
        });
        return dialog.showAndWait();
    }

    private boolean confirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showWarning(String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK).showAndWait();
    }
}
