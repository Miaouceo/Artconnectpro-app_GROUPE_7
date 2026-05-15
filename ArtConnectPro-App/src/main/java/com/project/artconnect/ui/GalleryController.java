package com.project.artconnect.ui;

import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.GalleryService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class GalleryController {
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> ratingFilter;
    @FXML
    private ListView<Gallery> galleryList;

    private final GalleryService galleryService = ServiceProvider.getGalleryService();

    @FXML
    public void initialize() {
        refreshList();
        refreshFilters();

        // Custom cell factory to show more info
        galleryList.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Gallery item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " - " + item.getAddress() + " (" + item.getRating() + "/5.0)");
                }
            }
        });
    }

    @FXML
    private void handleSearch() {
        String query = normalize(searchField.getText());
        String rating = ratingFilter.getValue();
        galleryList.setItems(FXCollections.observableArrayList(galleryService.getAllGalleries().stream()
                .filter(gallery -> query.isBlank()
                        || contains(gallery.getName(), query)
                        || contains(gallery.getAddress(), query)
                        || contains(gallery.getOwnerName(), query))
                .filter(gallery -> rating == null || matchesRating(gallery, rating))
                .toList()));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        ratingFilter.setValue(null);
        refreshList();
        refreshFilters();
    }

    @FXML
    private void handleAddGallery() {
        showGalleryDialog(null).ifPresent(gallery -> {
            galleryService.createGallery(gallery);
            refreshVisibleData();
        });
    }

    @FXML
    private void handleEditGallery() {
        Gallery selected = galleryList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a gallery to edit.");
            return;
        }
        showGalleryDialog(selected).ifPresent(gallery -> {
            galleryService.updateGallery(gallery);
            refreshVisibleData();
        });
    }

    @FXML
    private void handleDeleteGallery() {
        Gallery selected = galleryList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a gallery to delete.");
            return;
        }
        if (confirm("Delete gallery", "Delete " + selected.getName() + "?")) {
            galleryService.deleteGallery(selected.getName());
            refreshVisibleData();
        }
    }

    private void refreshList() {
        galleryList.setItems(FXCollections.observableArrayList(galleryService.getAllGalleries()));
    }

    private void refreshFilters() {
        ratingFilter.setItems(FXCollections.observableArrayList("4.5+", "4.0+", "3.0+"));
    }

    private boolean matchesRating(Gallery gallery, String rating) {
        double minimum = Double.parseDouble(rating.replace("+", ""));
        return gallery.getRating() >= minimum;
    }

    private java.util.Optional<Gallery> showGalleryDialog(Gallery existing) {
        Dialog<Gallery> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Gallery" : "Edit Gallery");
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        TextField addressField = new TextField(existing != null ? existing.getAddress() : "");
        TextField ownerField = new TextField(existing != null ? safe(existing.getOwnerName()) : "");
        TextField ratingField = new TextField(existing != null ? String.valueOf(existing.getRating()) : "0");
        if (existing != null) {
            nameField.setDisable(true);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Name"), nameField);
        grid.addRow(1, new Label("Address"), addressField);
        grid.addRow(2, new Label("Owner"), ownerField);
        grid.addRow(3, new Label("Rating"), ratingField);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != saveButton) return null;
            Gallery gallery = existing != null ? existing : new Gallery();
            gallery.setName(nameField.getText().trim());
            gallery.setAddress(addressField.getText().trim());
            gallery.setOwnerName(ownerField.getText().trim());
            gallery.setRating(Double.parseDouble(ratingField.getText().trim()));
            return gallery;
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

    private String safe(String value) {
        return value == null ? "" : value;
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
            refreshList();
        }
        galleryList.refresh();
    }

    private boolean hasActiveFilters() {
        return (searchField.getText() != null && !searchField.getText().isBlank())
                || ratingFilter.getValue() != null;
    }
}
