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

public class ArtworkController {
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
    }

    @FXML
    private void handleAddArtwork() {
        showArtworkDialog(null).ifPresent(artwork -> {
            artworkService.createArtwork(artwork);
            refreshTable();
        });
    }

    @FXML
    private void handleEditArtwork() {
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select an artwork to edit.");
            return;
        }
        showArtworkDialog(selected).ifPresent(artwork -> {
            artworkService.updateArtwork(artwork);
            refreshTable();
        });
    }

    @FXML
    private void handleDeleteArtwork() {
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select an artwork to delete.");
            return;
        }
        if (confirm("Delete artwork", "Delete " + selected.getTitle() + "?")) {
            artworkService.deleteArtwork(selected.getTitle());
            refreshTable();
        }
    }

    private void refreshTable() {
        artworkTable.setItems(FXCollections.observableArrayList(artworkService.getAllArtworks()));
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
}
