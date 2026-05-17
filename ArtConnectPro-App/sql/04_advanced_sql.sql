USE artconnect_pro;

-- ============================================================
-- Vues avancees (6)
-- ============================================================

CREATE OR REPLACE VIEW v_artist_portfolio_summary AS
SELECT
    a.artist_id,
    a.name AS artist_name,
    a.city,
    COUNT(DISTINCT aw.artwork_id) AS artwork_count,
    COUNT(DISTINCT w.workshop_id) AS workshop_count,
    COALESCE(AVG(r.rating), 0) AS average_rating,
    COALESCE(SUM(CASE WHEN aw.status = 'FOR_SALE' THEN aw.price ELSE 0 END), 0) AS available_portfolio_value
FROM artist a
LEFT JOIN artwork aw ON aw.artist_id = a.artist_id
LEFT JOIN review r ON r.artwork_id = aw.artwork_id
LEFT JOIN workshop w ON w.instructor_artist_id = a.artist_id
GROUP BY a.artist_id, a.name, a.city;

CREATE OR REPLACE VIEW v_available_artworks AS
SELECT
    aw.artwork_id,
    aw.title,
    aw.type,
    aw.medium,
    aw.price,
    a.name AS artist_name,
    a.city AS artist_city
FROM artwork aw
JOIN artist a ON a.artist_id = aw.artist_id
WHERE aw.status = 'FOR_SALE';

CREATE OR REPLACE VIEW v_exhibition_calendar AS
SELECT
    e.exhibition_id,
    e.title AS exhibition_title,
    e.start_date,
    e.end_date,
    e.theme,
    g.name AS gallery_name,
    g.address AS gallery_address,
    COUNT(ea.artwork_id) AS artwork_count
FROM exhibition e
JOIN gallery g ON g.gallery_id = e.gallery_id
LEFT JOIN exhibition_artwork ea ON ea.exhibition_id = e.exhibition_id
GROUP BY e.exhibition_id, e.title, e.start_date, e.end_date, e.theme, g.name, g.address;

CREATE OR REPLACE VIEW v_workshop_booking_status AS
SELECT
    w.workshop_id,
    w.title,
    w.workshop_datetime,
    w.max_participants,
    COUNT(CASE WHEN b.payment_status <> 'CANCELLED' THEN 1 END) AS active_booking_count,
    w.max_participants - COUNT(CASE WHEN b.payment_status <> 'CANCELLED' THEN 1 END) AS available_seats,
    a.name AS instructor_name
FROM workshop w
JOIN artist a ON a.artist_id = w.instructor_artist_id
LEFT JOIN booking b ON b.workshop_id = w.workshop_id
GROUP BY w.workshop_id, w.title, w.workshop_datetime, w.max_participants, a.name;

CREATE OR REPLACE VIEW v_member_activity AS
SELECT
    m.member_id,
    m.name AS member_name,
    m.email,
    m.membership_type,
    COUNT(DISTINCT b.booking_id) AS booking_count,
    COUNT(DISTINCT r.review_id) AS review_count,
    COALESCE(AVG(r.rating), 0) AS average_given_rating
FROM community_member m
LEFT JOIN booking b ON b.member_id = m.member_id
LEFT JOIN review r ON r.member_id = m.member_id
GROUP BY m.member_id, m.name, m.email, m.membership_type;

CREATE OR REPLACE VIEW v_gallery_performance AS
SELECT
    g.gallery_id,
    g.name AS gallery_name,
    g.rating,
    COUNT(DISTINCT e.exhibition_id) AS exhibition_count,
    COUNT(DISTINCT ea.artwork_id) AS displayed_artwork_count
FROM gallery g
LEFT JOIN exhibition e ON e.gallery_id = g.gallery_id
LEFT JOIN exhibition_artwork ea ON ea.exhibition_id = e.exhibition_id
GROUP BY g.gallery_id, g.name, g.rating;

-- ============================================================
-- Index explicites (8)
-- Les PRIMARY KEY et UNIQUE creent deja des index implicites;
-- ceux-ci optimisent les recherches applicatives les plus courantes.
-- ============================================================

CREATE INDEX idx_artist_city ON artist (city);
CREATE INDEX idx_artwork_status_type ON artwork (status, type);
CREATE INDEX idx_artwork_artist_status ON artwork (artist_id, status);
CREATE INDEX idx_exhibition_dates ON exhibition (start_date, end_date);
CREATE INDEX idx_workshop_datetime ON workshop (workshop_datetime);
CREATE INDEX idx_workshop_level ON workshop (level);
CREATE INDEX idx_booking_member_status ON booking (member_id, payment_status);
CREATE INDEX idx_review_artwork_rating ON review (artwork_id, rating);

-- ============================================================
-- Triggers (6)
-- ============================================================

DELIMITER //

DROP TRIGGER IF EXISTS trg_community_member_bi_normalize//
CREATE TRIGGER trg_community_member_bi_normalize
BEFORE INSERT ON community_member
FOR EACH ROW
BEGIN
    IF NEW.membership_type IS NULL OR TRIM(NEW.membership_type) = '' THEN
        SET NEW.membership_type = 'FREE';
    ELSE
        SET NEW.membership_type = UPPER(TRIM(NEW.membership_type));
    END IF;
END//

DROP TRIGGER IF EXISTS trg_community_member_bu_normalize//
CREATE TRIGGER trg_community_member_bu_normalize
BEFORE UPDATE ON community_member
FOR EACH ROW
BEGIN
    IF NEW.membership_type IS NULL OR TRIM(NEW.membership_type) = '' THEN
        SET NEW.membership_type = 'FREE';
    ELSE
        SET NEW.membership_type = UPPER(TRIM(NEW.membership_type));
    END IF;
END//

DROP TRIGGER IF EXISTS trg_artwork_bi_normalize//
CREATE TRIGGER trg_artwork_bi_normalize
BEFORE INSERT ON artwork
FOR EACH ROW
BEGIN
    IF NEW.status IS NULL OR TRIM(NEW.status) = '' THEN
        SET NEW.status = 'FOR_SALE';
    ELSE
        SET NEW.status = UPPER(TRIM(NEW.status));
    END IF;
END//

DROP TRIGGER IF EXISTS trg_artwork_bu_validate_sale//
CREATE TRIGGER trg_artwork_bu_validate_sale
BEFORE UPDATE ON artwork
FOR EACH ROW
BEGIN
    SET NEW.status = UPPER(TRIM(NEW.status));
    IF NEW.status = 'SOLD' AND NEW.price <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'A sold artwork must have a positive price.';
    END IF;
END//

DROP TRIGGER IF EXISTS trg_booking_bi_capacity//
CREATE TRIGGER trg_booking_bi_capacity
BEFORE INSERT ON booking
FOR EACH ROW
BEGIN
    DECLARE active_bookings INT DEFAULT 0;
    DECLARE capacity INT DEFAULT 0;

    IF NEW.payment_status IS NULL OR TRIM(NEW.payment_status) = '' THEN
        SET NEW.payment_status = 'PENDING';
    ELSE
        SET NEW.payment_status = UPPER(TRIM(NEW.payment_status));
    END IF;

    IF NEW.payment_status <> 'CANCELLED' THEN
        SELECT max_participants
        INTO capacity
        FROM workshop
        WHERE workshop_id = NEW.workshop_id;

        SELECT COUNT(*)
        INTO active_bookings
        FROM booking
        WHERE workshop_id = NEW.workshop_id
          AND payment_status <> 'CANCELLED';

        IF active_bookings >= capacity THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Workshop capacity reached.';
        END IF;
    END IF;
END//

DROP TRIGGER IF EXISTS trg_booking_bu_capacity//
CREATE TRIGGER trg_booking_bu_capacity
BEFORE UPDATE ON booking
FOR EACH ROW
BEGIN
    DECLARE active_bookings INT DEFAULT 0;
    DECLARE capacity INT DEFAULT 0;

    SET NEW.payment_status = UPPER(TRIM(NEW.payment_status));

    IF NEW.payment_status <> 'CANCELLED' THEN
        SELECT max_participants
        INTO capacity
        FROM workshop
        WHERE workshop_id = NEW.workshop_id;

        SELECT COUNT(*)
        INTO active_bookings
        FROM booking
        WHERE workshop_id = NEW.workshop_id
          AND payment_status <> 'CANCELLED'
          AND booking_id <> OLD.booking_id;

        IF active_bookings >= capacity THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Workshop capacity reached.';
        END IF;
    END IF;
END//

-- ============================================================
-- Fonctions (2)
-- ============================================================

DROP FUNCTION IF EXISTS fn_member_booking_count//
CREATE FUNCTION fn_member_booking_count(p_member_id INT)
RETURNS INT
READS SQL DATA
BEGIN
    DECLARE booking_total INT DEFAULT 0;

    SELECT COUNT(*)
    INTO booking_total
    FROM booking
    WHERE member_id = p_member_id
      AND payment_status <> 'CANCELLED';

    RETURN booking_total;
END//

DROP FUNCTION IF EXISTS fn_artwork_average_rating//
CREATE FUNCTION fn_artwork_average_rating(p_artwork_id INT)
RETURNS DECIMAL(3,2)
READS SQL DATA
BEGIN
    DECLARE average_rating DECIMAL(3,2) DEFAULT 0.00;

    SELECT COALESCE(AVG(rating), 0)
    INTO average_rating
    FROM review
    WHERE artwork_id = p_artwork_id;

    RETURN average_rating;
END//

-- ============================================================
-- Procedures transactionnelles (4 scenarios)
-- ============================================================

DROP PROCEDURE IF EXISTS sp_book_workshop//
CREATE PROCEDURE sp_book_workshop(
    IN p_workshop_id INT,
    IN p_member_id INT,
    IN p_payment_status VARCHAR(20)
)
BEGIN
    DECLARE capacity INT DEFAULT 0;
    DECLARE active_bookings INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT max_participants
    INTO capacity
    FROM workshop
    WHERE workshop_id = p_workshop_id
    FOR UPDATE;

    SELECT COUNT(*)
    INTO active_bookings
    FROM booking
    WHERE workshop_id = p_workshop_id
      AND payment_status <> 'CANCELLED';

    IF active_bookings >= capacity THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Workshop capacity reached.';
    END IF;

    INSERT INTO booking (workshop_id, member_id, booking_date, payment_status)
    VALUES (p_workshop_id, p_member_id, CURRENT_TIMESTAMP, COALESCE(UPPER(p_payment_status), 'PENDING'))
    ON DUPLICATE KEY UPDATE
        booking_date = CURRENT_TIMESTAMP,
        payment_status = VALUES(payment_status);

    COMMIT;
END//

DROP PROCEDURE IF EXISTS sp_cancel_booking//
CREATE PROCEDURE sp_cancel_booking(IN p_booking_id INT)
BEGIN
    DECLARE locked_booking_id INT DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT booking_id
    INTO locked_booking_id
    FROM booking
    WHERE booking_id = p_booking_id
    FOR UPDATE;

    UPDATE booking
    SET payment_status = 'CANCELLED'
    WHERE booking_id = p_booking_id;

    COMMIT;
END//

DROP PROCEDURE IF EXISTS sp_sell_artwork//
CREATE PROCEDURE sp_sell_artwork(
    IN p_artwork_id INT,
    IN p_sale_price DECIMAL(10,2)
)
BEGIN
    DECLARE locked_artwork_id INT DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT artwork_id
    INTO locked_artwork_id
    FROM artwork
    WHERE artwork_id = p_artwork_id
    FOR UPDATE;

    UPDATE artwork
    SET status = 'SOLD',
        price = p_sale_price
    WHERE artwork_id = p_artwork_id;

    COMMIT;
END//

DROP PROCEDURE IF EXISTS sp_move_artwork_between_exhibitions//
CREATE PROCEDURE sp_move_artwork_between_exhibitions(
    IN p_artwork_id INT,
    IN p_from_exhibition_id INT,
    IN p_to_exhibition_id INT
)
BEGIN
    DECLARE locked_artwork_id INT DEFAULT NULL;
    DECLARE locked_from_exhibition_id INT DEFAULT NULL;
    DECLARE locked_to_exhibition_id INT DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT artwork_id
    INTO locked_artwork_id
    FROM artwork
    WHERE artwork_id = p_artwork_id
    FOR UPDATE;

    SELECT exhibition_id
    INTO locked_from_exhibition_id
    FROM exhibition
    WHERE exhibition_id = p_from_exhibition_id
    FOR UPDATE;

    SELECT exhibition_id
    INTO locked_to_exhibition_id
    FROM exhibition
    WHERE exhibition_id = p_to_exhibition_id
    FOR UPDATE;

    DELETE FROM exhibition_artwork
    WHERE exhibition_id = p_from_exhibition_id
      AND artwork_id = p_artwork_id;

    INSERT INTO exhibition_artwork (exhibition_id, artwork_id)
    VALUES (p_to_exhibition_id, p_artwork_id)
    ON DUPLICATE KEY UPDATE artwork_id = VALUES(artwork_id);

    COMMIT;
END//

DELIMITER ;

-- Exemples de verification rapide:
-- SELECT * FROM v_artist_portfolio_summary;
-- SELECT fn_member_booking_count(1);
-- SELECT fn_artwork_average_rating(1);
