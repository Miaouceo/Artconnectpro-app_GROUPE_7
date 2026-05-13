package com.project.artconnect.persistence;

import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.model.Review;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.util.ConnectionManager;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcCommunityMemberDao implements CommunityMemberDao {
    private static final String MEMBER_SELECT = """
            SELECT m.member_id,
                   m.name,
                   m.email,
                   m.birth_year,
                   m.phone,
                   m.city,
                   m.membership_type,
                   d.name AS discipline_name
            FROM community_member m
            LEFT JOIN member_favorite_discipline mfd ON mfd.member_id = m.member_id
            LEFT JOIN discipline d ON d.discipline_id = mfd.discipline_id
            """;

    @Override
    public Optional<CommunityMember> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return loadMembers(MEMBER_SELECT + " WHERE m.member_id = ? ORDER BY m.name", id).stream().findFirst();
    }

    @Override
    public List<CommunityMember> findAll() {
        return loadMembers(MEMBER_SELECT + " ORDER BY m.name", null);
    }

    @Override
    public void save(CommunityMember member) {
        String sql = """
                INSERT INTO community_member(name, email, birth_year, phone, city, membership_type)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            fillMemberStatement(statement, member);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save community member " + member.getName(), e);
        }
    }

    @Override
    public void update(CommunityMember member) {
        String sql = """
                UPDATE community_member
                SET name = ?, birth_year = ?, phone = ?, city = ?, membership_type = ?
                WHERE email = ?
                """;
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, member.getName());
            if (member.getBirthYear() != null) {
                statement.setInt(2, member.getBirthYear());
            } else {
                statement.setNull(2, java.sql.Types.SMALLINT);
            }
            statement.setString(3, member.getPhone());
            statement.setString(4, member.getCity());
            statement.setString(5, member.getMembershipType());
            statement.setString(6, member.getEmail());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update community member " + member.getName(), e);
        }
    }

    @Override
    public void delete(String email) {
        String sql = "DELETE FROM community_member WHERE email = ?";
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete community member " + email, e);
        }
    }

    private List<CommunityMember> loadMembers(String sql, Long id) {
        Map<Long, CommunityMember> membersById = new LinkedHashMap<>();
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (id != null) {
                statement.setLong(1, id);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    long memberId = rs.getLong("member_id");
                    CommunityMember member = membersById.computeIfAbsent(memberId, ignored -> mapMember(rs));
                    String disciplineName = rs.getString("discipline_name");
                    if (disciplineName != null && member.getFavoriteDisciplines().stream()
                            .noneMatch(discipline -> disciplineName.equalsIgnoreCase(discipline.getName()))) {
                        member.getFavoriteDisciplines().add(new Discipline(disciplineName));
                    }
                }
            }

            for (Map.Entry<Long, CommunityMember> entry : membersById.entrySet()) {
                loadBookings(connection, entry.getKey(), entry.getValue());
                loadReviews(connection, entry.getKey(), entry.getValue());
            }

            return new ArrayList<>(membersById.values());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load community members from database.", e);
        }
    }

    private CommunityMember mapMember(ResultSet rs) {
        try {
            CommunityMember member = new CommunityMember();
            member.setName(rs.getString("name"));
            member.setEmail(rs.getString("email"));
            int birthYear = rs.getInt("birth_year");
            member.setBirthYear(rs.wasNull() ? null : birthYear);
            member.setPhone(rs.getString("phone"));
            member.setCity(rs.getString("city"));
            member.setMembershipType(rs.getString("membership_type"));
            return member;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to map community member row.", e);
        }
    }

    private void loadBookings(Connection connection, long memberId, CommunityMember member) throws SQLException {
        String sql = """
                SELECT b.booking_date,
                       b.payment_status,
                       w.title,
                       w.workshop_datetime,
                       w.duration_minutes,
                       w.max_participants,
                       w.price,
                       w.location,
                       w.description,
                       w.level,
                       a.name AS instructor_name
                FROM booking b
                JOIN workshop w ON w.workshop_id = b.workshop_id
                JOIN artist a ON a.artist_id = w.instructor_artist_id
                WHERE b.member_id = ?
                ORDER BY b.booking_date
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Booking booking = new Booking();
                    Timestamp bookingDate = rs.getTimestamp("booking_date");
                    if (bookingDate != null) {
                        booking.setBookingDate(bookingDate.toLocalDateTime());
                    }
                    booking.setPaymentStatus(rs.getString("payment_status"));

                    Workshop workshop = new Workshop();
                    workshop.setTitle(rs.getString("title"));
                    Timestamp workshopDate = rs.getTimestamp("workshop_datetime");
                    if (workshopDate != null) {
                        workshop.setDate(workshopDate.toLocalDateTime());
                    }
                    workshop.setDurationMinutes(rs.getInt("duration_minutes"));
                    workshop.setMaxParticipants(rs.getInt("max_participants"));
                    workshop.setPrice(rs.getDouble("price"));
                    workshop.setLocation(rs.getString("location"));
                    workshop.setDescription(rs.getString("description"));
                    workshop.setLevel(rs.getString("level"));

                    Artist instructor = new Artist();
                    instructor.setName(rs.getString("instructor_name"));
                    workshop.setInstructor(instructor);

                    booking.setWorkshop(workshop);
                    booking.setMember(member);
                    member.getBookings().add(booking);
                }
            }
        }
    }

    private void loadReviews(Connection connection, long memberId, CommunityMember member) throws SQLException {
        String sql = """
                SELECT r.rating,
                       r.comment,
                       r.review_date,
                       aw.title,
                       aw.creation_year,
                       aw.type,
                       aw.medium,
                       aw.dimensions,
                       aw.description,
                       aw.price,
                       aw.status,
                       a.name AS artist_name
                FROM review r
                JOIN artwork aw ON aw.artwork_id = r.artwork_id
                JOIN artist a ON a.artist_id = aw.artist_id
                WHERE r.member_id = ?
                ORDER BY r.review_date DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Review review = new Review();
                    review.setReviewer(member);
                    review.setRating(rs.getInt("rating"));
                    review.setComment(rs.getString("comment"));
                    Date reviewDate = rs.getDate("review_date");
                    if (reviewDate != null) {
                        review.setReviewDate(reviewDate.toLocalDate());
                    }

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

                    Artist artist = new Artist();
                    artist.setName(rs.getString("artist_name"));
                    artwork.setArtist(artist);

                    review.setArtwork(artwork);
                    member.getReviews().add(review);
                }
            }
        }
    }

    private void fillMemberStatement(PreparedStatement statement, CommunityMember member) throws SQLException {
        statement.setString(1, member.getName());
        statement.setString(2, member.getEmail());
        if (member.getBirthYear() != null) {
            statement.setInt(3, member.getBirthYear());
        } else {
            statement.setNull(3, java.sql.Types.SMALLINT);
        }
        statement.setString(4, member.getPhone());
        statement.setString(5, member.getCity());
        statement.setString(6, member.getMembershipType());
    }
}
