package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import java.time.LocalDateTime;

public class WorkshopController {
    @FXML
    private TableView<Workshop> workshopTable;
    @FXML
    private TableColumn<Workshop, String> titleColumn;
    @FXML
    private TableColumn<Workshop, LocalDateTime> dateColumn;
    @FXML
    private TableColumn<Workshop, String> instructorColumn;
    @FXML
    private TableColumn<Workshop, Double> priceColumn;
    @FXML
    private TableColumn<Workshop, String> levelColumn;

    private final WorkshopService workshopService = ServiceProvider.getWorkshopService();
    private final ArtistService artistService = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("level"));

        instructorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getInstructor() != null ? cellData.getValue().getInstructor().getName()
                        : "Unknown"));

        refreshTable();
    }

    @FXML
    private void handleAddWorkshop() {
        showWorkshopDialog(null).ifPresent(workshop -> {
            workshopService.createWorkshop(workshop);
            refreshTable();
        });
    }

    @FXML
    private void handleEditWorkshop() {
        Workshop selected = workshopTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a workshop to edit.");
            return;
        }
        showWorkshopDialog(selected).ifPresent(workshop -> {
            workshopService.updateWorkshop(workshop);
            refreshTable();
        });
    }

    @FXML
    private void handleDeleteWorkshop() {
        Workshop selected = workshopTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Select a workshop to delete.");
            return;
        }
        if (confirm("Delete workshop", "Delete " + selected.getTitle() + "?")) {
            workshopService.deleteWorkshop(selected.getTitle());
            refreshTable();
        }
    }

    private void refreshTable() {
        workshopTable.setItems(FXCollections.observableArrayList(workshopService.getAllWorkshops()));
    }

    private java.util.Optional<Workshop> showWorkshopDialog(Workshop existing) {
        Dialog<Workshop> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Workshop" : "Edit Workshop");
        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        TextField titleField = new TextField(existing != null ? existing.getTitle() : "");
        ComboBox<String> instructorBox = new ComboBox<>(FXCollections.observableArrayList(
                artistService.getAllArtists().stream().map(Artist::getName).toList()));
        instructorBox.setValue(existing != null && existing.getInstructor() != null ? existing.getInstructor().getName() : null);
        TextField dateField = new TextField(existing != null && existing.getDate() != null ? existing.getDate().toString() : "2026-06-01T14:00");
        TextField priceField = new TextField(existing != null ? String.valueOf(existing.getPrice()) : "0");
        TextField levelField = new TextField(existing != null ? existing.getLevel() : "");
        if (existing != null) {
            titleField.setDisable(true);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Title"), titleField);
        grid.addRow(1, new Label("Instructor"), instructorBox);
        grid.addRow(2, new Label("Date"), dateField);
        grid.addRow(3, new Label("Price"), priceField);
        grid.addRow(4, new Label("Level"), levelField);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != saveButton) return null;
            Workshop workshop = existing != null ? existing : new Workshop();
            workshop.setTitle(titleField.getText().trim());
            workshop.setInstructor(artistService.getArtistByName(instructorBox.getValue()).orElse(null));
            workshop.setDate(LocalDateTime.parse(dateField.getText().trim()));
            workshop.setPrice(Double.parseDouble(priceField.getText().trim()));
            workshop.setLevel(levelField.getText().trim());
            if (workshop.getDurationMinutes() == 0) workshop.setDurationMinutes(180);
            if (workshop.getMaxParticipants() == 0) workshop.setMaxParticipants(10);
            if (workshop.getLocation() == null) workshop.setLocation("");
            if (workshop.getDescription() == null) workshop.setDescription("");
            return workshop;
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
