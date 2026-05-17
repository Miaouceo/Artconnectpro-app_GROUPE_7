package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.ArtworkService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import java.util.List;

public class ArtworkController {
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> typeFilter;
    @FXML
    private TableView<Artwork> artworkTable;
    @FXML
    private TableColumn<Artwork, String> titleColumn;
    @FXML
    private TableColumn<Artwork, String> typeColumn;
    @FXML
    private TableColumn<Artwork, Double> priceColumn;
    @FXML
    private TableColumn<Artwork, String> statusColumn;
    @FXML
    private TableColumn<Artwork, String> artistColumn;
    @FXML
    private Button addArtworkButton;
    @FXML
    private Button editArtworkButton;
    @FXML
    private Button deleteArtworkButton;

    private final ArtworkService artworkService = ServiceProvider.getArtworkService();
    private final ArtistService artistService = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        artistColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getArtist() != null ? cellData.getValue().getArtist().getName() : "Unknown"));

        refreshTable();
        refreshFilters();
        UserSession.activeRoleProperty().addListener((obs, oldRole, newRole) -> applyRole(newRole));
        applyRole(UserSession.getActiveRole());
    }

    @FXML
    private void handleSearch() {
        String query = normalize(searchField.getText());
        String type = typeFilter.getValue();
        List<Artwork> filtered = artworkService.getAllArtworks().stream()
                .filter(artwork -> query.isBlank()
                        || contains(artwork.getTitle(), query)
                        || contains(artwork.getType(), query)
                        || contains(artistName(artwork), query))
                .filter(artwork -> type == null || type.equals(artwork.getType()))
                .toList();
        artworkTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        typeFilter.setValue(null);
        refreshTable();
        refreshFilters();
    }

    @FXML
    private void handleAddArtwork() {
        if (!canManageArtworks()) {
            showWarning("Only artists and admins can manage artworks.");
            return;
        }
        showArtworkDialog(null).ifPresent(artwork -> {
            artworkService.createArtwork(artwork);
            refreshVisibleData();
        });
    }

    @FXML
    private void handleEditArtwork() {
        if (!canManageArtworks()) {
            showWarning("Only artists and admins can manage artworks.");
            return;
        }
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select an artwork to edit.");
            return;
        }
        showArtworkDialog(selected).ifPresent(artwork -> {
            artworkService.updateArtwork(artwork);
            refreshVisibleData();
        });
    }

    @FXML
    private void handleDeleteArtwork() {
        if (!canManageArtworks()) {
            showWarning("Only artists and admins can manage artworks.");
            return;
        }
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select an artwork to delete.");
            return;
        }
        if (confirm("Delete artwork", "Delete " + selected.getTitle() + "?")) {
            artworkService.deleteArtwork(selected.getTitle());
            refreshVisibleData();
        }
    }

    private void refreshTable() {
        artworkTable.setItems(FXCollections.observableArrayList(artworkService.getAllArtworks()));
    }

    private void refreshFilters() {
        typeFilter.setItems(FXCollections.observableArrayList(artworkService.getAllArtworks().stream()
                .map(Artwork::getType)
                .filter(type -> type != null && !type.isBlank())
                .distinct()
                .sorted()
                .toList()));
    }

    private String artistName(Artwork artwork) {
        return artwork.getArtist() == null ? "" : artwork.getArtist().getName();
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
        artworkTable.refresh();
    }

    private boolean hasActiveFilters() {
        return (searchField.getText() != null && !searchField.getText().isBlank())
                || typeFilter.getValue() != null;
    }

    private java.util.Optional<Artwork> showArtworkDialog(Artwork existing) {
        Dialog<Artwork> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Artwork" : "Edit Artwork");
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        TextField titleField = new TextField(existing != null ? existing.getTitle() : "");
        ComboBox<String> artistBox = new ComboBox<>(FXCollections.observableArrayList(
                artistService.getAllArtists().stream().map(Artist::getName).toList()));
        artistBox.setValue(existing != null && existing.getArtist() != null ? existing.getArtist().getName() : null);
        TextField typeField = new TextField(existing != null ? existing.getType() : "");
        TextField priceField = new TextField(existing != null ? String.valueOf(existing.getPrice()) : "");
        TextField yearField = new TextField(existing != null && existing.getCreationYear() != null ? existing.getCreationYear().toString() : "");
        ComboBox<Artwork.Status> statusBox = new ComboBox<>(FXCollections.observableArrayList(Artwork.Status.values()));
        statusBox.setValue(existing != null && existing.getStatus() != null ? existing.getStatus() : Artwork.Status.FOR_SALE);
        if (existing != null) {
            titleField.setDisable(true);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Title"), titleField);
        grid.addRow(1, new Label("Artist"), artistBox);
        grid.addRow(2, new Label("Type"), typeField);
        grid.addRow(3, new Label("Price"), priceField);
        grid.addRow(4, new Label("Year"), yearField);
        grid.addRow(5, new Label("Status"), statusBox);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != saveButton) {
                return null;
            }
            Artwork artwork = existing != null ? existing : new Artwork();
            artwork.setTitle(titleField.getText().trim());
            artwork.setArtist(artistService.getArtistByName(artistBox.getValue()).orElse(null));
            artwork.setType(typeField.getText().trim());
            artwork.setPrice(Double.parseDouble(priceField.getText().trim()));
            artwork.setCreationYear(yearField.getText().isBlank() ? null : Integer.parseInt(yearField.getText().trim()));
            artwork.setStatus(statusBox.getValue());
            if (artwork.getMedium() == null) artwork.setMedium("");
            if (artwork.getDimensions() == null) artwork.setDimensions("");
            if (artwork.getDescription() == null) artwork.setDescription("");
            return artwork;
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
        boolean canManage = role.canManageArtworks();
        addArtworkButton.setDisable(!canManage);
        editArtworkButton.setDisable(!canManage);
        deleteArtworkButton.setDisable(!canManage);
    }

    private boolean canManageArtworks() {
        return UserSession.getActiveRole().canManageArtworks();
    }
}
