package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.util.ConnectionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC implementation for ArtistDao.
 */
public class JdbcArtistDao implements ArtistDao {
    private static final String BASE_SELECT = """
            SELECT a.artist_id,
                   a.name,
                   a.bio,
                   a.birth_year,
                   a.contact_email,
                   a.phone,
                   a.city,
                   a.website,
                   a.social_media,
                   a.is_active,
                   d.name AS discipline_name
            FROM artist a
            LEFT JOIN artist_discipline ad ON ad.artist_id = a.artist_id
            LEFT JOIN discipline d ON d.discipline_id = ad.discipline_id
            """;

    @Override
    public List<Artist> findAll() {
        return loadArtists(BASE_SELECT + " ORDER BY a.name", null);
    }

    @Override
    public void save(Artist artist) {
        String sql = """
                INSERT INTO artist(name, bio, birth_year, contact_email, phone, city, website, social_media, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            connection.setAutoCommit(false);
            try {
                fillArtistStatement(statement, artist);
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Unable to retrieve generated artist id.");
                    }
                    syncDisciplines(connection, keys.getInt(1), artist.getDisciplines());
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save artist " + artist.getName(), e);
        }
    }

    @Override
    public void update(Artist artist) {
        String sql = """
                UPDATE artist
                SET name = ?, bio = ?, birth_year = ?, phone = ?, city = ?, website = ?, social_media = ?, is_active = ?
                WHERE contact_email = ?
                """;
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            try {
                statement.setString(1, artist.getName());
                statement.setString(2, artist.getBio());
                if (artist.getBirthYear() != null) {
                    statement.setInt(3, artist.getBirthYear());
                } else {
                    statement.setNull(3, java.sql.Types.SMALLINT);
                }
                statement.setString(4, artist.getPhone());
                statement.setString(5, artist.getCity());
                statement.setString(6, artist.getWebsite());
                statement.setString(7, artist.getSocialMedia());
                statement.setBoolean(8, artist.isActive());
                statement.setString(9, artist.getContactEmail());
                statement.executeUpdate();

                Integer artistId = findArtistIdByEmail(connection, artist.getContactEmail());
                if (artistId == null) {
                    throw new SQLException("Artist not found for email " + artist.getContactEmail());
                }
                clearDisciplineLinks(connection, artistId);
                syncDisciplines(connection, artistId, artist.getDisciplines());
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update artist " + artist.getName(), e);
        }
    }

    @Override
    public void delete(String artistName) {
        String sql = "DELETE FROM artist WHERE name = ?";
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, artistName);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete artist " + artistName, e);
        }
    }

    @Override
    public List<Artist> findByCity(String city) {
        return loadArtists(BASE_SELECT + " WHERE a.city = ? ORDER BY a.name", city);
    }

    private List<Artist> loadArtists(String sql, String city) {
        Map<Integer, Artist> artistsById = new LinkedHashMap<>();
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (city != null) {
                statement.setString(1, city);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int artistId = rs.getInt("artist_id");
                    Artist artist = artistsById.computeIfAbsent(artistId, ignored -> mapArtist(rs));
                    String disciplineName = rs.getString("discipline_name");
                    if (disciplineName != null && artist.getDisciplines().stream()
                            .noneMatch(discipline -> disciplineName.equalsIgnoreCase(discipline.getName()))) {
                        artist.getDisciplines().add(new Discipline(disciplineName));
                    }
                }
            }
            return new ArrayList<>(artistsById.values());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load artists from database.", e);
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
            throw new IllegalStateException("Failed to map artist row.", e);
        }
    }

    private void fillArtistStatement(PreparedStatement statement, Artist artist) throws SQLException {
        statement.setString(1, artist.getName());
        statement.setString(2, artist.getBio());
        if (artist.getBirthYear() != null) {
            statement.setInt(3, artist.getBirthYear());
        } else {
            statement.setNull(3, java.sql.Types.SMALLINT);
        }
        statement.setString(4, artist.getContactEmail());
        statement.setString(5, artist.getPhone());
        statement.setString(6, artist.getCity());
        statement.setString(7, artist.getWebsite());
        statement.setString(8, artist.getSocialMedia());
        statement.setBoolean(9, artist.isActive());
    }

    private Integer findArtistIdByEmail(Connection connection, String email) throws SQLException {
        String sql = "SELECT artist_id FROM artist WHERE contact_email = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt("artist_id") : null;
            }
        }
    }

    private void clearDisciplineLinks(Connection connection, int artistId) throws SQLException {
        try (PreparedStatement statement = connection
                .prepareStatement("DELETE FROM artist_discipline WHERE artist_id = ?")) {
            statement.setInt(1, artistId);
            statement.executeUpdate();
        }
    }

    private void syncDisciplines(Connection connection, int artistId, List<Discipline> disciplines) throws SQLException {
        if (disciplines == null) {
            return;
        }
        for (Discipline discipline : disciplines) {
            if (discipline == null || discipline.getName() == null || discipline.getName().isBlank()) {
                continue;
            }
            int disciplineId = ensureDiscipline(connection, discipline.getName());
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT IGNORE INTO artist_discipline(artist_id, discipline_id) VALUES (?, ?)")) {
                statement.setInt(1, artistId);
                statement.setInt(2, disciplineId);
                statement.executeUpdate();
            }
        }
    }

    private int ensureDiscipline(Connection connection, String disciplineName) throws SQLException {
        try (PreparedStatement select = connection
                .prepareStatement("SELECT discipline_id FROM discipline WHERE name = ?")) {
            select.setString(1, disciplineName);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("discipline_id");
                }
            }
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO discipline(name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, disciplineName);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException ignored) {
            // Another transaction may have inserted the row; retry the lookup below.
        }

        try (PreparedStatement select = connection
                .prepareStatement("SELECT discipline_id FROM discipline WHERE name = ?")) {
            select.setString(1, disciplineName);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("discipline_id");
                }
            }
        }
        throw new SQLException("Unable to resolve discipline id for " + disciplineName);
    }
}
