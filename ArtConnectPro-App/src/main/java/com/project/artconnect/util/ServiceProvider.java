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
    private static final ArtistService artistService;
    private static final ArtworkService artworkService;
    private static final GalleryService galleryService;
    private static final WorkshopService workshopService;
    private static final CommunityService communityService;

    static {
        if (isJdbcAvailable()) {
            artistService = new JdbcArtistService(new JdbcArtistDao());
            artworkService = new JdbcArtworkService(new JdbcArtworkDao());
            galleryService = new JdbcGalleryService(new JdbcGalleryDao());
            workshopService = new JdbcWorkshopService(new JdbcWorkshopDao());
            communityService = new JdbcCommunityService(new JdbcCommunityMemberDao());
        } else {
            InMemoryArtistService inMemoryArtistService = new InMemoryArtistService();
            InMemoryArtworkService inMemoryArtworkService = new InMemoryArtworkService();
            InMemoryGalleryService inMemoryGalleryService = new InMemoryGalleryService();
            InMemoryWorkshopService inMemoryWorkshopService = new InMemoryWorkshopService();
            InMemoryCommunityService inMemoryCommunityService = new InMemoryCommunityService();

            inMemoryArtworkService.initData(inMemoryArtistService);
            inMemoryGalleryService.initData(inMemoryArtworkService);
            inMemoryWorkshopService.initData(inMemoryArtistService);
            inMemoryCommunityService.initData(inMemoryArtworkService);

            artistService = inMemoryArtistService;
            artworkService = inMemoryArtworkService;
            galleryService = inMemoryGalleryService;
            workshopService = inMemoryWorkshopService;
            communityService = inMemoryCommunityService;
        }
    }

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

    private static boolean isJdbcAvailable() {
        try (Connection ignored = ConnectionManager.getConnection()) {
            return schemaLooksReady(ignored);
        } catch (SQLException e) {
            return false;
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
