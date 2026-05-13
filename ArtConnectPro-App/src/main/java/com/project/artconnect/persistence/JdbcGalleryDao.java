package com.project.artconnect.persistence;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

public class JdbcGalleryDao implements GalleryDao {
    private static final String BASE_SELECT = """
            SELECT g.gallery_id,
                   g.name,
                   g.address,
                   g.owner_name,
                   g.opening_hours,
                   g.contact_phone,
                   g.rating,
                   g.website,
                   e.exhibition_id,
                   e.title AS exhibition_title,
                   e.start_date,
                   e.end_date,
                   e.description AS exhibition_description,
                   e.curator_name,
                   e.theme
            FROM gallery g
            LEFT JOIN exhibition e ON e.gallery_id = g.gallery_id
            """;

    @Override
    public Optional<Gallery> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return loadGalleries(BASE_SELECT + " WHERE g.gallery_id = ? ORDER BY g.name, e.start_date", id).stream().findFirst();
    }

    @Override
    public List<Gallery> findAll() {
        return loadGalleries(BASE_SELECT + " ORDER BY g.name, e.start_date", null);
    }

    @Override
    public void save(Gallery gallery) {
        String sql = """
                INSERT INTO gallery(name, address, owner_name, opening_hours, contact_phone, rating, website)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            fillGalleryStatement(statement, gallery);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save gallery " + gallery.getName(), e);
        }
    }

    @Override
    public void update(Gallery gallery) {
        String sql = """
                UPDATE gallery
                SET address = ?, owner_name = ?, opening_hours = ?, contact_phone = ?, rating = ?, website = ?
                WHERE name = ?
                """;
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, gallery.getAddress());
            statement.setString(2, gallery.getOwnerName());
            statement.setString(3, gallery.getOpeningHours());
            statement.setString(4, gallery.getContactPhone());
            statement.setDouble(5, gallery.getRating());
            statement.setString(6, gallery.getWebsite());
            statement.setString(7, gallery.getName());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update gallery " + gallery.getName(), e);
        }
    }

    @Override
    public void delete(String galleryName) {
        String sql = "DELETE FROM gallery WHERE name = ?";
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, galleryName);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete gallery " + galleryName, e);
        }
    }

    private List<Gallery> loadGalleries(String sql, Long id) {
        Map<Long, Gallery> galleriesById = new LinkedHashMap<>();
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (id != null) {
                statement.setLong(1, id);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    long galleryId = rs.getLong("gallery_id");
                    Gallery gallery = galleriesById.computeIfAbsent(galleryId, ignored -> mapGallery(rs));
                    long exhibitionId = rs.getLong("exhibition_id");
                    if (!rs.wasNull() && gallery.getExhibitions().stream()
                            .noneMatch(exhibition -> exhibition.getTitle().equals(rsSafeString(rs, "exhibition_title")))) {
                        gallery.addExhibition(mapExhibition(rs, gallery));
                    }
                }
            }
            return new ArrayList<>(galleriesById.values());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load galleries from database.", e);
        }
    }

    private Gallery mapGallery(ResultSet rs) {
        try {
            Gallery gallery = new Gallery();
            gallery.setName(rs.getString("name"));
            gallery.setAddress(rs.getString("address"));
            gallery.setOwnerName(rs.getString("owner_name"));
            gallery.setOpeningHours(rs.getString("opening_hours"));
            gallery.setContactPhone(rs.getString("contact_phone"));
            double rating = rs.getDouble("rating");
            gallery.setRating(rs.wasNull() ? 0.0 : rating);
            gallery.setWebsite(rs.getString("website"));
            return gallery;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to map gallery row.", e);
        }
    }

    private Exhibition mapExhibition(ResultSet rs, Gallery gallery) {
        try {
            Exhibition exhibition = new Exhibition();
            exhibition.setTitle(rs.getString("exhibition_title"));
            Date start = rs.getDate("start_date");
            if (start != null) {
                exhibition.setStartDate(start.toLocalDate());
            }
            Date end = rs.getDate("end_date");
            if (end != null) {
                exhibition.setEndDate(end.toLocalDate());
            }
            exhibition.setDescription(rs.getString("exhibition_description"));
            exhibition.setCuratorName(rs.getString("curator_name"));
            exhibition.setTheme(rs.getString("theme"));
            exhibition.setGallery(gallery);
            return exhibition;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to map exhibition row.", e);
        }
    }

    private String rsSafeString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read column " + column, e);
        }
    }

    private void fillGalleryStatement(PreparedStatement statement, Gallery gallery) throws SQLException {
        statement.setString(1, gallery.getName());
        statement.setString(2, gallery.getAddress());
        statement.setString(3, gallery.getOwnerName());
        statement.setString(4, gallery.getOpeningHours());
        statement.setString(5, gallery.getContactPhone());
        statement.setDouble(6, gallery.getRating());
        statement.setString(7, gallery.getWebsite());
    }
}
