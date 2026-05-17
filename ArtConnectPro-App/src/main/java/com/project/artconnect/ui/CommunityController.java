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
import java.util.List;

public class CommunityController {
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> cityFilter;
    @FXML
    private TableView<CommunityMember> memberTable;
    @FXML
    private TableColumn<CommunityMember, String> nameColumn;
    @FXML
    private TableColumn<CommunityMember, String> emailColumn;
    @FXML
    private TableColumn<CommunityMember, String> cityColumn;
    @FXML
    private Button addMemberButton;
    @FXML
    private Button editMemberButton;
    @FXML
    private Button deleteMemberButton;

    private final CommunityService communityService = ServiceProvider.getCommunityService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));

        refreshTable();
        refreshFilters();
        UserSession.activeRoleProperty().addListener((obs, oldRole, newRole) -> applyRole(newRole));
        applyRole(UserSession.getActiveRole());
    }

    @FXML
    private void handleSearch() {
        String query = normalize(searchField.getText());
        String city = cityFilter.getValue();
        List<CommunityMember> filtered = communityService.getAllMembers().stream()
                .filter(member -> query.isBlank()
                        || contains(member.getName(), query)
                        || contains(member.getEmail(), query)
                        || contains(member.getCity(), query)
                        || contains(member.getMembershipType(), query))
                .filter(member -> city == null || city.equals(member.getCity()))
                .toList();
        memberTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        cityFilter.setValue(null);
        refreshTable();
        refreshFilters();
    }

    @FXML
    private void handleAddMember() {
        if (!canManageCommunity()) {
            showWarning("Only admins can manage community members.");
            return;
        }
        showMemberDialog(null).ifPresent(member -> {
            communityService.createMember(member);
            refreshVisibleData();
        });
    }

    @FXML
    private void handleEditMember() {
        if (!canManageCommunity()) {
            showWarning("Only admins can manage community members.");
            return;
        }
        CommunityMember selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a member to edit.");
            return;
        }
        showMemberDialog(selected).ifPresent(member -> {
            communityService.updateMember(member);
            refreshVisibleData();
        });
    }

    @FXML
    private void handleDeleteMember() {
        if (!canManageCommunity()) {
            showWarning("Only admins can manage community members.");
            return;
        }
        CommunityMember selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a member to delete.");
            return;
        }
        if (confirm("Delete member", "Delete " + selected.getName() + "?")) {
            communityService.deleteMember(selected.getEmail());
            refreshVisibleData();
        }
    }

    private void refreshTable() {
        memberTable.setItems(FXCollections.observableArrayList(communityService.getAllMembers()));
    }

    private void refreshFilters() {
        cityFilter.setItems(FXCollections.observableArrayList(communityService.getAllMembers().stream()
                .map(CommunityMember::getCity)
                .filter(city -> city != null && !city.isBlank())
                .distinct()
                .sorted()
                .toList()));
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private void refreshVisibleData() {
        refreshFilters();
        if (hasActiveFilters()) {
            handleSearch();
        } else {
            refreshTable();
        }
        memberTable.refresh();
    }

    private boolean hasActiveFilters() {
        return (searchField.getText() != null && !searchField.getText().isBlank())
                || cityFilter.getValue() != null;
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

    private void applyRole(UserRole role) {
        boolean canManage = role.canManageCommunity();
        addMemberButton.setDisable(!canManage);
        editMemberButton.setDisable(!canManage);
        deleteMemberButton.setDisable(!canManage);
    }

    private boolean canManageCommunity() {
        return UserSession.getActiveRole().canManageCommunity();
    }
}
