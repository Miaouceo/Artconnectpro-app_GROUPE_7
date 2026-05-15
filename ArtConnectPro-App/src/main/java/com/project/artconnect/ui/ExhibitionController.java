package com.project.artconnect.ui;

import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.GalleryService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExhibitionController {
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> galleryFilter;
    @FXML
    private TableView<Exhibition> exhibitionTable;
    @FXML
    private TableColumn<Exhibition, String> titleColumn;
    @FXML
    private TableColumn<Exhibition, LocalDate> dateColumn;
    @FXML
    private TableColumn<Exhibition, String> themeColumn;
    @FXML
    private TableColumn<Exhibition, String> galleryColumn;

    private final GalleryService galleryService = ServiceProvider.getGalleryService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        themeColumn.setCellValueFactory(new PropertyValueFactory<>("theme"));

        galleryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getGallery() != null ? cellData.getValue().getGallery().getName() : "Unknown"));

        refreshData();
        refreshFilters();
    }

    @FXML
    private void handleSearch() {
        String query = normalize(searchField.getText());
        String gallery = galleryFilter.getValue();
        List<Exhibition> filtered = getAllExhibitions().stream()
                .filter(exhibition -> query.isBlank()
                        || contains(exhibition.getTitle(), query)
                        || contains(exhibition.getTheme(), query)
                        || contains(galleryName(exhibition), query))
                .filter(exhibition -> gallery == null || gallery.equals(galleryName(exhibition)))
                .toList();
        exhibitionTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        galleryFilter.setValue(null);
        refreshData();
        refreshFilters();
    }

    private void refreshData() {
        exhibitionTable.setItems(FXCollections.observableArrayList(getAllExhibitions()));
    }

    private List<Exhibition> getAllExhibitions() {
        List<Exhibition> all = new ArrayList<>();
        for (Gallery g : galleryService.getAllGalleries()) {
            all.addAll(g.getExhibitions());
        }
        return all;
    }

    private void refreshFilters() {
        galleryFilter.setItems(FXCollections.observableArrayList(galleryService.getAllGalleries().stream()
                .map(Gallery::getName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted()
                .toList()));
    }

    private String galleryName(Exhibition exhibition) {
        return exhibition.getGallery() == null ? "" : exhibition.getGallery().getName();
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
