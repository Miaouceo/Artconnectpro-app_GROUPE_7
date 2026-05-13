package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.ArtworkTag;
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
 * JDBC implementation for ArtworkDao.
 */
public class JdbcArtworkDao implements ArtworkDao {
    private static final String BASE_SELECT = """
            SELECT aw.artwork_id,
                   aw.title,
                   aw.creation_year,
                   aw.type,
                   aw.medium,
                   aw.dimensions,
                   aw.description,
                   aw.price,
                   aw.status,
                   a.artist_id,
                   a.name AS artist_name,
                   a.bio AS artist_bio,
                   a.birth_year AS artist_birth_year,
                   a.contact_email,
                   a.phone,
                   a.city,
                   a.website,
                   a.social_media,
                   a.is_active,
                   d.name AS discipline_name,
                   t.name AS tag_name
            FROM artwork aw
            JOIN artist a ON a.artist_id = aw.artist_id
            LEFT JOIN artist_discipline ad ON ad.artist_id = a.artist_id
            LEFT JOIN discipline d ON d.discipline_id = ad.discipline_id
            LEFT JOIN artwork_tag_map atm ON atm.artwork_id = aw.artwork_id
            LEFT JOIN artwork_tag t ON t.tag_id = atm.tag_id
            """;

    @Override
    public List<Artwork> findAll() {
        return loadArtworks(BASE_SELECT + " ORDER BY aw.title", null);
    }

    @Override
    public void save(Artwork artwork) {
        String sql = """
                INSERT INTO artwork(artist_id, title, creation_year, type, medium, dimensions, description, price, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            connection.setAutoCommit(false);
            try {
                fillArtworkStatement(connection, statement, artwork);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Unable to retrieve generated artwork id.");
                    }
                    syncTags(connection, keys.getInt(1), artwork.getTags());
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save artwork " + artwork.getTitle(), e);
        }
    }

    @Override
    public void update(Artwork artwork) {
        String sql = """
                UPDATE artwork
                SET artist_id = ?, creation_year = ?, type = ?, medium = ?, dimensions = ?, description = ?, price = ?, status = ?
                WHERE title = ?
                """;
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            try {
                int artistId = findArtistId(connection, artwork.getArtist());
                statement.setInt(1, artistId);
                if (artwork.getCreationYear() != null) {
                    statement.setInt(2, artwork.getCreationYear());
                } else {
                    statement.setNull(2, java.sql.Types.SMALLINT);
                }
                statement.setString(3, artwork.getType());
                statement.setString(4, artwork.getMedium());
                statement.setString(5, artwork.getDimensions());
                statement.setString(6, artwork.getDescription());
                statement.setDouble(7, artwork.getPrice());
                statement.setString(8, artwork.getStatus() != null ? artwork.getStatus().name() : Artwork.Status.FOR_SALE.name());
                statement.setString(9, artwork.getTitle());
                statement.executeUpdate();

                Integer artworkId = findArtworkIdByTitle(connection, artwork.getTitle());
                if (artworkId == null) {
                    throw new SQLException("Artwork not found for title " + artwork.getTitle());
                }
                clearTagLinks(connection, artworkId);
                syncTags(connection, artworkId, artwork.getTags());
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update artwork " + artwork.getTitle(), e);
        }
    }

    @Override
    public void delete(String title) {
        String sql = "DELETE FROM artwork WHERE title = ?";
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete artwork " + title, e);
        }
    }

    @Override
    public List<Artwork> findByArtistName(String artistName) {
        return loadArtworks(BASE_SELECT + " WHERE a.name = ? ORDER BY aw.title", artistName);
    }

    private List<Artwork> loadArtworks(String sql, String artistName) {
        Map<Integer, Artist> artistsById = new LinkedHashMap<>();
        Map<Integer, Artwork> artworksById = new LinkedHashMap<>();
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (artistName != null) {
                statement.setString(1, artistName);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Artist artist = artistsById.computeIfAbsent(rs.getInt("artist_id"), ignored -> mapArtist(rs));
                    Artwork artwork = artworksById.computeIfAbsent(rs.getInt("artwork_id"),
                            ignored -> mapArtwork(rs, artist));

                    addDisciplineIfMissing(artist, rs.getString("discipline_name"));
                    addTagIfMissing(artwork, rs.getString("tag_name"));
                }
            }
            return new ArrayList<>(artworksById.values());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load artworks from database.", e);
        }
    }

    private Artist mapArtist(ResultSet rs) {
        try {
            Artist artist = new Artist();
            artist.setName(rs.getString("artist_name"));
            artist.setBio(rs.getString("artist_bio"));
            int birthYear = rs.getInt("artist_birth_year");
            artist.setBirthYear(rs.wasNull() ? null : birthYear);
            artist.setContactEmail(rs.getString("contact_email"));
            artist.setPhone(rs.getString("phone"));
            artist.setCity(rs.getString("city"));
            artist.setWebsite(rs.getString("website"));
            artist.setSocialMedia(rs.getString("social_media"));
            artist.setActive(rs.getBoolean("is_active"));
            return artist;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to map artist row for artwork.", e);
        }
    }

    private Artwork mapArtwork(ResultSet rs, Artist artist) {
        try {
            Artwork artwork = new Artwork();
            artwork.setTitle(rs.getString("title"));
            int creationYear = rs.getInt("creation_year");
            artwork.setCreationYear(rs.wasNull() ? null : creationYear);
            artwork.setType(rs.getString("type"));
            artwork.setMedium(rs.getString("medium"));
            artwork.setDimensions(rs.getString("dimensions"));
            artwork.setDescription(rs.getString("description"));
            artwork.setPrice(rs.getDouble("price"));
            artwork.setStatus(Artwork.Status.valueOf(rs.getString("status")));
            artwork.setArtist(artist);
            artist.getArtworks().add(artwork);
            return artwork;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to map artwork row.", e);
        }
    }

    private void addDisciplineIfMissing(Artist artist, String disciplineName) {
        if (disciplineName == null) {
            return;
        }
        boolean exists = artist.getDisciplines().stream()
                .anyMatch(discipline -> disciplineName.equalsIgnoreCase(discipline.getName()));
        if (!exists) {
            artist.getDisciplines().add(new Discipline(disciplineName));
        }
    }

    private void addTagIfMissing(Artwork artwork, String tagName) {
        if (tagName == null) {
            return;
        }
        boolean exists = artwork.getTags().stream()
                .anyMatch(tag -> tagName.equalsIgnoreCase(tag.getName()));
        if (!exists) {
            artwork.getTags().add(new ArtworkTag(tagName));
        }
    }

    private void fillArtworkStatement(Connection connection, PreparedStatement statement, Artwork artwork)
            throws SQLException {
        statement.setInt(1, findArtistId(connection, artwork.getArtist()));
        statement.setString(2, artwork.getTitle());
        if (artwork.getCreationYear() != null) {
            statement.setInt(3, artwork.getCreationYear());
        } else {
            statement.setNull(3, java.sql.Types.SMALLINT);
        }
        statement.setString(4, artwork.getType());
        statement.setString(5, artwork.getMedium());
        statement.setString(6, artwork.getDimensions());
        statement.setString(7, artwork.getDescription());
        statement.setDouble(8, artwork.getPrice());
        statement.setString(9, artwork.getStatus() != null ? artwork.getStatus().name() : Artwork.Status.FOR_SALE.name());
    }

    private int findArtistId(Connection connection, Artist artist) throws SQLException {
        if (artist == null) {
            throw new SQLException("Artwork must reference an artist.");
        }
        String sql = artist.getContactEmail() != null && !artist.getContactEmail().isBlank()
                ? "SELECT artist_id FROM artist WHERE contact_email = ?"
                : "SELECT artist_id FROM artist WHERE name = ?";
        String value = artist.getContactEmail() != null && !artist.getContactEmail().isBlank()
                ? artist.getContactEmail()
                : artist.getName();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("artist_id");
                }
            }
        }
        throw new SQLException("Unable to find artist id for " + artistLabel(artist));
    }

    private String artistLabel(Artist artist) {
        return artist != null ? artist.getName() : "unknown artist";
    }

    private Integer findArtworkIdByTitle(Connection connection, String title) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT artwork_id FROM artwork WHERE title = ?")) {
            statement.setString(1, title);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt("artwork_id") : null;
            }
        }
    }

    private void clearTagLinks(Connection connection, int artworkId) throws SQLException {
        try (PreparedStatement statement = connection
                .prepareStatement("DELETE FROM artwork_tag_map WHERE artwork_id = ?")) {
            statement.setInt(1, artworkId);
            statement.executeUpdate();
        }
    }

    private void syncTags(Connection connection, int artworkId, List<ArtworkTag> tags) throws SQLException {
        if (tags == null) {
            return;
        }
        for (ArtworkTag tag : tags) {
            if (tag == null || tag.getName() == null || tag.getName().isBlank()) {
                continue;
            }
            int tagId = ensureTag(connection, tag.getName());
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT IGNORE INTO artwork_tag_map(artwork_id, tag_id) VALUES (?, ?)")) {
                statement.setInt(1, artworkId);
                statement.setInt(2, tagId);
                statement.executeUpdate();
            }
        }
    }

    private int ensureTag(Connection connection, String tagName) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("SELECT tag_id FROM artwork_tag WHERE name = ?")) {
            select.setString(1, tagName);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("tag_id");
                }
            }
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO artwork_tag(name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, tagName);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException ignored) {
            // Another transaction may have inserted the row; retry the lookup below.
        }

        try (PreparedStatement select = connection.prepareStatement("SELECT tag_id FROM artwork_tag WHERE name = ?")) {
            select.setString(1, tagName);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("tag_id");
                }
            }
        }
        throw new SQLException("Unable to resolve tag id for " + tagName);
    }
}
