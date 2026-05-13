package com.project.artconnect.service.impl;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ConnectionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class JdbcWorkshopService implements WorkshopService {
    private final WorkshopDao workshopDao;

    public JdbcWorkshopService(WorkshopDao workshopDao) {
        this.workshopDao = workshopDao;
    }

    @Override
    public List<Workshop> getAllWorkshops() {
        return workshopDao.findAll();
    }

    @Override
    public Optional<Workshop> getWorkshopByTitle(String title) {
        return workshopDao.findAll().stream()
                .filter(workshop -> workshop.getTitle() != null && workshop.getTitle().equalsIgnoreCase(title))
                .findFirst();
    }

    @Override
    public void createWorkshop(Workshop workshop) {
        workshopDao.save(workshop);
    }

    @Override
    public void updateWorkshop(Workshop workshop) {
        workshopDao.update(workshop);
    }

    @Override
    public void deleteWorkshop(String title) {
        workshopDao.delete(title);
    }

    @Override
    public void bookWorkshop(Workshop workshop, CommunityMember member) {
        if (workshop == null || workshop.getTitle() == null || member == null || member.getEmail() == null) {
            return;
        }
        String sql = """
                INSERT INTO booking(workshop_id, member_id, booking_date, payment_status)
                VALUES (
                    (SELECT workshop_id FROM workshop WHERE title = ? LIMIT 1),
                    (SELECT member_id FROM community_member WHERE email = ? LIMIT 1),
                    CURRENT_TIMESTAMP,
                    'PENDING'
                )
                """;
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, workshop.getTitle());
            statement.setString(2, member.getEmail());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to book workshop " + workshop.getTitle(), e);
        }
    }

    @Override
    public List<Booking> getBookingsByMember(CommunityMember member) {
        if (member == null) {
            return List.of();
        }
        if (!member.getBookings().isEmpty()) {
            return member.getBookings();
        }
        if (member.getEmail() == null || member.getEmail().isBlank()) {
            return List.of();
        }

        String sql = """
                SELECT b.booking_date,
                       b.payment_status,
                       w.title
                FROM booking b
                JOIN community_member m ON m.member_id = b.member_id
                JOIN workshop w ON w.workshop_id = b.workshop_id
                WHERE m.email = ?
                ORDER BY b.booking_date
                """;
        try (Connection connection = ConnectionManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, member.getEmail());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Booking booking = new Booking();
                    booking.setBookingDate(rs.getTimestamp("booking_date").toLocalDateTime());
                    booking.setPaymentStatus(rs.getString("payment_status"));
                    Workshop workshop = new Workshop();
                    workshop.setTitle(rs.getString("title"));
                    booking.setWorkshop(workshop);
                    booking.setMember(member);
                    member.getBookings().add(booking);
                }
            }
            return member.getBookings();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load bookings for member " + member.getName(), e);
        }
    }
}
