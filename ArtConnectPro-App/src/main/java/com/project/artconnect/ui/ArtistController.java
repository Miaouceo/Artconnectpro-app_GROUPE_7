package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ArtistController {
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Discipline> disciplineFilter;
    @FXML
    private TableView<Artist> artistTable;
    @FXML
    private TableColumn<Artist, String> nameColumn;
    @FXML
    private TableColumn<Artist, String> cityColumn;
    @FXML
    private TableColumn<Artist, String> emailColumn;
    @FXML
    private TableColumn<Artist, Integer> yearColumn;
    @FXML
    private Button addArtistButton;
    @FXML
    private Button editArtistButton;
    @FXML
    private Button deleteArtistButton;

    private final ArtistService artistService = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("contactEmail"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("birthYear"));

        disciplineFilter.setItems(FXCollections.observableArrayList(artistService.getAllDisciplines()));
        refreshTable();
        UserSession.activeRoleProperty().addListener((obs, oldRole, newRole) -> applyRole(newRole));
        applyRole(UserSession.getActiveRole());
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        Discipline d = disciplineFilter.getValue();
        String dName = (d != null) ? d.getName() : null;
        artistTable.setItems(FXCollections.observableArrayList(artistService.searchArtists(query, dName, null)));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        disciplineFilter.setValue(null);
        refreshTable();
    }

    private void refreshTable() {
        artistTable.setItems(FXCollections.observableArrayList(artistService.getAllArtists()));
        disciplineFilter.setItems(FXCollections.observableArrayList(artistService.getAllDisciplines()));
    }

    @FXML
    private void handleAddArtist() {
        if (!canManageArtists()) {
            showWarning("Only admins can manage artists.");
            return;
        }
        showArtistDialog(null).ifPresent(artist -> {
            artistService.createArtist(artist);
            refreshVisibleData();
        });
    }

    @FXML
    private void handleEditArtist() {
        if (!canManageArtists()) {
            showWarning("Only admins can manage artists.");
            return;
        }
        Artist selected = artistTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select an artist to edit.");
            return;
        }
        showArtistDialog(selected).ifPresent(artist -> {
            artistService.updateArtist(artist);
            refreshVisibleData();
        });
    }

    @FXML
    private void handleDeleteArtist() {
        if (!canManageArtists()) {
            showWarning("Only admins can manage artists.");
            return;
        }
        Artist selected = artistTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select an artist to delete.");
            return;
        }
        if (confirm("Delete artist", "Delete " + selected.getName() + "?")) {
            artistService.deleteArtist(selected.getName());
            refreshVisibleData();
        }
    }

    private java.util.Optional<Artist> showArtistDialog(Artist existing) {
        Dialog<Artist> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Artist" : "Edit Artist");
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        TextField cityField = new TextField(existing != null ? existing.getCity() : "");
        TextField emailField = new TextField(existing != null ? existing.getContactEmail() : "");
        TextField yearField = new TextField(existing != null && existing.getBirthYear() != null ? existing.getBirthYear().toString() : "");
        TextField bioField = new TextField(existing != null ? defaultString(existing.getBio()) : "");
        TextField disciplinesField = new TextField(existing != null
                ? existing.getDisciplines().stream().map(Discipline::getName).collect(Collectors.joining(", "))
                : "");
        if (existing != null) {
            emailField.setDisable(true);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Name"), nameField);
        grid.addRow(1, new Label("City"), cityField);
        grid.addRow(2, new Label("Email"), emailField);
        grid.addRow(3, new Label("Birth Year"), yearField);
        grid.addRow(4, new Label("Bio"), bioField);
        grid.addRow(5, new Label("Disciplines"), disciplinesField);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != saveButton) {
                return null;
            }
            Artist artist = existing != null ? existing : new Artist();
            artist.setName(nameField.getText().trim());
            artist.setCity(cityField.getText().trim());
            artist.setContactEmail(emailField.getText().trim());
            artist.setBirthYear(yearField.getText().isBlank() ? null : Integer.parseInt(yearField.getText().trim()));
            artist.setBio(bioField.getText().trim());
            artist.setDisciplines(Arrays.stream(disciplinesField.getText().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(Discipline::new)
                    .collect(Collectors.toList()));
            artist.setActive(true);
            return artist;
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

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private void refreshVisibleData() {
        disciplineFilter.setItems(FXCollections.observableArrayList(artistService.getAllDisciplines()));
        if (hasActiveFilters()) {
            handleSearch();
        } else {
            refreshTable();
        }
        artistTable.refresh();
    }

    private boolean hasActiveFilters() {
        return (searchField.getText() != null && !searchField.getText().isBlank())
                || disciplineFilter.getValue() != null;
    }

    private void applyRole(UserRole role) {
        boolean canManage = role.canManageCatalog();
        addArtistButton.setDisable(!canManage);
        editArtistButton.setDisable(!canManage);
        deleteArtistButton.setDisable(!canManage);
    }

    private boolean canManageArtists() {
        return UserSession.getActiveRole().canManageCatalog();
    }
}
