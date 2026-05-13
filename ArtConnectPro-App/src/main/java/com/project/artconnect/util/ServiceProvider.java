package com.project.artconnect.util;

import com.project.artconnect.persistence.JdbcArtistDao;
import com.project.artconnect.persistence.JdbcArtworkDao;
import com.project.artconnect.persistence.JdbcCommunityMemberDao;
import com.project.artconnect.persistence.JdbcGalleryDao;
import com.project.artconnect.persistence.JdbcWorkshopDao;
import com.project.artconnect.service.*;
import com.project.artconnect.service.impl.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Service Provider to manage singleton instances of services and handle their
 * initialization.
 */
public class ServiceProvider {
    private static final boolean jdbcAvailable = requireJdbcAvailability();
    private static final ArtistService artistService = new JdbcArtistService(new JdbcArtistDao());
    private static final ArtworkService artworkService = new JdbcArtworkService(new JdbcArtworkDao());
    private static final GalleryService galleryService = new JdbcGalleryService(new JdbcGalleryDao());
    private static final WorkshopService workshopService = new JdbcWorkshopService(new JdbcWorkshopDao());
    private static final CommunityService communityService = new JdbcCommunityService(new JdbcCommunityMemberDao());

    public static ArtistService getArtistService() {
        return artistService;
    }

    public static ArtworkService getArtworkService() {
        return artworkService;
    }

    public static GalleryService getGalleryService() {
        return galleryService;
    }

    public static WorkshopService getWorkshopService() {
        return workshopService;
    }

    public static CommunityService getCommunityService() {
        return communityService;
    }

    private static boolean requireJdbcAvailability() {
        try (Connection ignored = ConnectionManager.getConnection()) {
            if (!schemaLooksReady(ignored)) {
                throw new IllegalStateException(
                        "JDBC is enabled but the ArtConnect schema is incomplete or inaccessible.");
            }
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Unable to connect to MySQL. Check DatabaseConfig and verify that the artconnect_pro database is running.",
                    e);
        }
    }

    private static boolean schemaLooksReady(Connection connection) {
        return tableExists(connection, "artist")
                && tableExists(connection, "artwork")
                && tableExists(connection, "gallery")
                && tableExists(connection, "exhibition")
                && tableExists(connection, "workshop")
                && tableExists(connection, "community_member")
                && tableExists(connection, "booking")
                && tableExists(connection, "review");
    }

    private static boolean tableExists(Connection connection, String tableName) {
        String sql = "SELECT 1 FROM " + tableName + " LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeQuery();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
