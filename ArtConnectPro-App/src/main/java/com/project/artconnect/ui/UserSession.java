package com.project.artconnect.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public final class UserSession {
    private static final ObjectProperty<UserRole> activeRole = new SimpleObjectProperty<>(UserRole.VISITOR);

    private UserSession() {
    }

    public static ObjectProperty<UserRole> activeRoleProperty() {
        return activeRole;
    }

    public static UserRole getActiveRole() {
        return activeRole.get();
    }

    public static void setActiveRole(UserRole role) {
        activeRole.set(role == null ? UserRole.VISITOR : role);
    }
}
