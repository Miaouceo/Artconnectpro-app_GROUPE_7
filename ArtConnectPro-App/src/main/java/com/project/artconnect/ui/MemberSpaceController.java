package com.project.artconnect.ui;

import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Review;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.ArtworkService;
import com.project.artconnect.service.CommunityService;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;

public class MemberSpaceController {
    @FXML
    private ComboBox<CommunityMember> memberSelector;
    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField cityField;
    @FXML
    private TextField membershipField;
    @FXML
    private Button saveProfileButton;
    @FXML
    private ComboBox<Workshop> workshopSelector;
    @FXML
    private Button bookWorkshopButton;
    @FXML
    private ListView<String> bookingList;
    @FXML
    private ComboBox<Artwork> artworkSelector;
    @FXML
    private Spinner<Integer> ratingSpinner;
    @FXML
    private TextField commentField;
    @FXML
    private Button addReviewButton;
    @FXML
    private ListView<String> reviewList;

    private final CommunityService communityService = ServiceProvider.getCommunityService();
    private final WorkshopService workshopService = ServiceProvider.getWorkshopService();
    private final ArtworkService artworkService = ServiceProvider.getArtworkService();

    @FXML
    public void initialize() {
        ratingSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 5));
        memberSelector.valueProperty().addListener((obs, oldMember, newMember) -> showMember(newMember));
        UserSession.activeRoleProperty().addListener((obs, oldRole, newRole) -> applyRole(newRole));
        handleRefresh();
        applyRole(UserSession.getActiveRole());
    }

    @FXML
    private void handleRefresh() {
        memberSelector.setItems(FXCollections.observableArrayList(communityService.getAllMembers()));
        workshopSelector.setItems(FXCollections.observableArrayList(workshopService.getAllWorkshops()));
        artworkSelector.setItems(FXCollections.observableArrayList(artworkService.getAllArtworks()));
        if (memberSelector.getValue() == null && !memberSelector.getItems().isEmpty()) {
            memberSelector.setValue(memberSelector.getItems().get(0));
        } else {
            showMember(memberSelector.getValue());
        }
    }

    @FXML
    private void handleSaveProfile() {
        if (!canUseMemberSpace()) {
            showWarning("Only members and admins can edit a member profile.");
            return;
        }
        CommunityMember member = selectedMember();
        if (member == null) {
            showWarning("Select a member profile first.");
            return;
        }
        member.setName(nameField.getText().trim());
        member.setCity(cityField.getText().trim());
        member.setMembershipType(membershipField.getText().trim());
        communityService.updateMember(member);
        handleRefresh();
    }

    @FXML
    private void handleBookWorkshop() {
        if (!canUseMemberSpace()) {
            showWarning("Only members and admins can create bookings.");
            return;
        }
        CommunityMember member = selectedMember();
        Workshop workshop = workshopSelector.getValue();
        if (member == null || workshop == null) {
            showWarning("Select a member and a workshop first.");
            return;
        }
        workshopService.bookWorkshop(workshop, member);
        showMember(member);
    }

    @FXML
    private void handleAddReview() {
        if (!canUseMemberSpace()) {
            showWarning("Only members and admins can publish reviews.");
            return;
        }
        CommunityMember member = selectedMember();
        Artwork artwork = artworkSelector.getValue();
        if (member == null || artwork == null) {
            showWarning("Select a member and an artwork first.");
            return;
        }
        String comment = commentField.getText() == null ? "" : commentField.getText().trim();
        member.getReviews().add(new Review(member, artwork, ratingSpinner.getValue(), comment));
        commentField.clear();
        showMember(member);
    }

    private void showMember(CommunityMember member) {
        if (member == null) {
            nameField.clear();
            emailField.clear();
            cityField.clear();
            membershipField.clear();
            bookingList.setItems(FXCollections.observableArrayList());
            reviewList.setItems(FXCollections.observableArrayList());
            return;
        }
        nameField.setText(safe(member.getName()));
        emailField.setText(safe(member.getEmail()));
        cityField.setText(safe(member.getCity()));
        membershipField.setText(safe(member.getMembershipType()));
        bookingList.setItems(FXCollections.observableArrayList(workshopService.getBookingsByMember(member).stream()
                .map(this::formatBooking)
                .toList()));
        reviewList.setItems(FXCollections.observableArrayList(communityService.getReviewsByMember(member).stream()
                .map(this::formatReview)
                .toList()));
    }

    private CommunityMember selectedMember() {
        return memberSelector.getValue();
    }

    private String formatBooking(Booking booking) {
        String title = booking.getWorkshop() == null ? "Atelier" : booking.getWorkshop().getTitle();
        String date = booking.getBookingDate() == null ? "" : booking.getBookingDate().toString();
        return title + " | " + date + " | " + safe(booking.getPaymentStatus());
    }

    private String formatReview(Review review) {
        String title = review.getArtwork() == null ? "Oeuvre" : review.getArtwork().getTitle();
        return title + " | " + review.getRating() + "/5 | " + safe(review.getComment());
    }

    private void applyRole(UserRole role) {
        boolean enabled = role.canUseMemberSpace();
        saveProfileButton.setDisable(!enabled);
        bookWorkshopButton.setDisable(!enabled);
        addReviewButton.setDisable(!enabled);
    }

    private boolean canUseMemberSpace() {
        return UserSession.getActiveRole().canUseMemberSpace();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showWarning(String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK).showAndWait();
    }
}
