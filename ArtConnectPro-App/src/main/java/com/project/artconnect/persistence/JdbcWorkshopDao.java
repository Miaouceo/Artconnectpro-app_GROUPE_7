package com.project.artconnect.persistence;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.util.ConnectionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcWorkshopDao implements WorkshopDao {
    private static final String BASE_SELECT = """
            SELECT w.workshop_id,
                   w.title,
                   w.workshop_datetime,
                   w.duration_minutes,
                   w.max_participants,
                   w.price,
                   w.location,
                   w.description,
                   w.level,
                   a.artist_id,
                   a.name,
                   a.bio,
                   a.birth_year,
                   a.contact_email,
                   a.phone,
                   a.city,
                   a.website,
                   a.social_media,
                   a.is_active
            FROM workshop w
            JOIN artist a ON a.artist_id = w.instructor_artist_id
            """;

    @Override
    public Optional<Workshop> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return loadWorkshops(BASE_SELECT + " WHERE w.workshop_id = ? ORDER BY w.workshop_datetime", id).stream().findFirst();
    }

    @Override
    public List<Workshop> findAll() {
        return loadWorkshops(BASE_SELECT + " ORDER BY w.workshop_datetime", null);
    }

    @Override
    public void save(Workshop workshop) {
        String sql = """
                INSERT INTO workshop(instructor_artist_id, title, workshop_datetime, duration_minutes, max_participants, price, location, description, level)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            fillWorkshopStatement(connection, statement, workshop);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save workshop " + workshop.getTitle(), e);
        }
    }

    @Override
    public void update(Workshop workshop) {
        String sql = """
                UPDATE workshop
                SET instructor_artist_id = ?, workshop_datetime = ?, duration_minutes = ?, max_participants = ?, price = ?, location = ?, description = ?, level = ?
                WHERE title = ?
                """;
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, findArtistId(connection, workshop.getInstructor()));
            statement.setTimestamp(2, Timestamp.valueOf(workshop.getDate()));
            statement.setInt(3, workshop.getDurationMinutes());
            statement.setInt(4, workshop.getMaxParticipants());
            statement.setDouble(5, workshop.getPrice());
            statement.setString(6, workshop.getLocation());
            statement.setString(7, workshop.getDescription());
            statement.setString(8, workshop.getLevel());
            statement.setString(9, workshop.getTitle());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update workshop " + workshop.getTitle(), e);
        }
    }

    @Override
    public void delete(String title) {
        String sql = "DELETE FROM workshop WHERE title = ?";
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete workshop " + title, e);
        }
    }

    private List<Workshop> loadWorkshops(String sql, Long id) {
        Map<Long, Artist> artistsById = new LinkedHashMap<>();
        List<Workshop> workshops = new ArrayList<>();
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (id != null) {
                statement.setLong(1, id);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Artist instructor = artistsById.computeIfAbsent(rs.getLong("artist_id"), ignored -> mapArtist(rs));
                    workshops.add(mapWorkshop(rs, instructor));
                }
            }
            return workshops;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load workshops from database.", e);
        }
    }

    private Artist mapArtist(ResultSet rs) {
        try {
            Artist artist = new Artist();
            artist.setName(rs.getString("name"));
            artist.setBio(rs.getString("bio"));
            int birthYear = rs.getInt("birth_year");
            artist.setBirthYear(rs.wasNull() ? null : birthYear);
            artist.setContactEmail(rs.getString("contact_email"));
            artist.setPhone(rs.getString("phone"));
            artist.setCity(rs.getString("city"));
            artist.setWebsite(rs.getString("website"));
            artist.setSocialMedia(rs.getString("social_media"));
            artist.setActive(rs.getBoolean("is_active"));
            return artist;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to map workshop instructor.", e);
        }
    }

    private Workshop mapWorkshop(ResultSet rs, Artist instructor) {
        try {
            Workshop workshop = new Workshop();
            workshop.setTitle(rs.getString("title"));
            Timestamp timestamp = rs.getTimestamp("workshop_datetime");
            if (timestamp != null) {
                workshop.setDate(timestamp.toLocalDateTime());
            }
            workshop.setDurationMinutes(rs.getInt("duration_minutes"));
            workshop.setMaxParticipants(rs.getInt("max_participants"));
            workshop.setPrice(rs.getDouble("price"));
            workshop.setLocation(rs.getString("location"));
            workshop.setDescription(rs.getString("description"));
            workshop.setLevel(rs.getString("level"));
            workshop.setInstructor(instructor);
            return workshop;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to map workshop row.", e);
        }
    }

    private void fillWorkshopStatement(Connection connection, PreparedStatement statement, Workshop workshop)
            throws SQLException {
        statement.setInt(1, findArtistId(connection, workshop.getInstructor()));
        statement.setString(2, workshop.getTitle());
        statement.setTimestamp(3, Timestamp.valueOf(workshop.getDate()));
        statement.setInt(4, workshop.getDurationMinutes());
        statement.setInt(5, workshop.getMaxParticipants());
        statement.setDouble(6, workshop.getPrice());
        statement.setString(7, workshop.getLocation());
        statement.setString(8, workshop.getDescription());
        statement.setString(9, workshop.getLevel());
    }

    private int findArtistId(Connection connection, Artist artist) throws SQLException {
        if (artist == null || artist.getName() == null || artist.getName().isBlank()) {
            throw new SQLException("Workshop must reference an instructor.");
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT artist_id FROM artist WHERE name = ?")) {
            statement.setString(1, artist.getName());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("artist_id");
                }
            }
        }
        throw new SQLException("Unable to find instructor " + artist.getName());
    }
}
