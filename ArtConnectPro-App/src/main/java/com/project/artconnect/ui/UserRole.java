package com.project.artconnect.ui;

public enum UserRole {
    VISITOR("Visiteur", "Lecture seule - tous les onglets"),
    MEMBER("Membre", "Reservations, avis, profil"),
    ARTIST("Artiste", "CRUD oeuvres, animation ateliers"),
    ADMIN("Admin", "Acces complet, gestion des roles");

    private final String label;
    private final String description;

    UserRole(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean canManageArtworks() {
        return this == ARTIST || this == ADMIN;
    }

    public boolean canManageWorkshops() {
        return this == ARTIST || this == ADMIN;
    }

    public boolean canManageCommunity() {
        return this == ADMIN;
    }

    public boolean canManageCatalog() {
        return this == ADMIN;
    }

    public boolean canUseMemberSpace() {
        return this == MEMBER || this == ADMIN;
    }

    @Override
    public String toString() {
        return label;
    }
}
