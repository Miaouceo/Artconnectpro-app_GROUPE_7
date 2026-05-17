USE artconnect_pro;

-- 1. Verifier les artistes
SELECT artist_id, name, city, contact_email
FROM artist
ORDER BY artist_id;

-- 2. Verifier les disciplines de chaque artiste
SELECT a.name AS artist_name, d.name AS discipline_name
FROM artist a
JOIN artist_discipline ad ON ad.artist_id = a.artist_id
JOIN discipline d ON d.discipline_id = ad.discipline_id
ORDER BY a.name, d.name;

-- 3. Verifier les oeuvres avec leur artiste
SELECT aw.title, aw.type, aw.status, aw.price, a.name AS artist_name
FROM artwork aw
JOIN artist a ON a.artist_id = aw.artist_id
ORDER BY aw.artwork_id;

-- 4. Verifier les tags des oeuvres
SELECT aw.title, t.name AS tag_name
FROM artwork aw
JOIN artwork_tag_map atm ON atm.artwork_id = aw.artwork_id
JOIN artwork_tag t ON t.tag_id = atm.tag_id
ORDER BY aw.title, t.name;

-- 5. Verifier les galeries et expositions
SELECT g.name AS gallery_name, e.title AS exhibition_title, e.start_date, e.end_date
FROM gallery g
LEFT JOIN exhibition e ON e.gallery_id = g.gallery_id
ORDER BY g.name, e.start_date;

-- 6. Verifier les oeuvres presentes dans les expositions
SELECT e.title AS exhibition_title, aw.title AS artwork_title
FROM exhibition e
JOIN exhibition_artwork ea ON ea.exhibition_id = e.exhibition_id
JOIN artwork aw ON aw.artwork_id = ea.artwork_id
ORDER BY e.title, aw.title;

-- 7. Verifier les ateliers et leurs instructeurs
SELECT w.title, w.workshop_datetime, w.level, w.price, a.name AS instructor_name
FROM workshop w
JOIN artist a ON a.artist_id = w.instructor_artist_id
ORDER BY w.workshop_datetime;

-- 8. Verifier les membres
SELECT member_id, name, email, city, membership_type
FROM community_member
ORDER BY member_id;

-- 9. Verifier les disciplines favorites des membres
SELECT m.name AS member_name, d.name AS favorite_discipline
FROM community_member m
JOIN member_favorite_discipline mfd ON mfd.member_id = m.member_id
JOIN discipline d ON d.discipline_id = mfd.discipline_id
ORDER BY m.name, d.name;

-- 10. Verifier les reservations
SELECT m.name AS member_name, w.title AS workshop_title, b.booking_date, b.payment_status
FROM booking b
JOIN community_member m ON m.member_id = b.member_id
JOIN workshop w ON w.workshop_id = b.workshop_id
ORDER BY b.booking_date;

-- 11. Verifier les avis
SELECT m.name AS member_name, aw.title AS artwork_title, r.rating, r.comment, r.review_date
FROM review r
JOIN community_member m ON m.member_id = r.member_id
JOIN artwork aw ON aw.artwork_id = r.artwork_id
ORDER BY r.review_date;

-- 12. Test rapide pour prouver que JDBC lit bien la base
SELECT name
FROM artist
WHERE name LIKE '%TEST%' OR name LIKE '%Monet%';

-- 13. Verifier les vues avancees
SELECT *
FROM v_artist_portfolio_summary
ORDER BY artist_name;

SELECT *
FROM v_workshop_booking_status
ORDER BY workshop_datetime;

SELECT *
FROM v_member_activity
ORDER BY member_name;

-- 14. Verifier les fonctions avancees
SELECT
    fn_member_booking_count(1) AS alice_active_bookings,
    fn_artwork_average_rating(1) AS mona_lisa_average_rating;

-- 15. Verifier les procedures transactionnelles
-- Ces appels modifient les donnees de demonstration. A lancer seulement pour tester les transactions.
-- CALL sp_book_workshop(1, 2, 'PAID');
-- CALL sp_cancel_booking(2);
-- CALL sp_sell_artwork(4, 125000.00);
-- CALL sp_move_artwork_between_exhibitions(3, 3, 1);
