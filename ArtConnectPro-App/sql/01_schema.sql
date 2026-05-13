CREATE DATABASE IF NOT EXISTS artconnect_pro
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE artconnect_pro;

CREATE TABLE artist (
    artist_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    bio TEXT NULL,
    birth_year SMALLINT NULL,
    contact_email VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NULL,
    city VARCHAR(120) NULL,
    website VARCHAR(255) NULL,
    social_media VARCHAR(255) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_artist_contact_email UNIQUE (contact_email),
    CONSTRAINT chk_artist_birth_year CHECK (birth_year IS NULL OR birth_year BETWEEN 1900 AND 2100)
);

CREATE TABLE discipline (
    discipline_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_discipline_name UNIQUE (name)
);

CREATE TABLE artist_discipline (
    artist_id INT NOT NULL,
    discipline_id INT NOT NULL,
    PRIMARY KEY (artist_id, discipline_id),
    CONSTRAINT fk_artist_discipline_artist
        FOREIGN KEY (artist_id) REFERENCES artist (artist_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_artist_discipline_discipline
        FOREIGN KEY (discipline_id) REFERENCES discipline (discipline_id)
        ON DELETE RESTRICT
);

CREATE TABLE artwork (
    artwork_id INT AUTO_INCREMENT PRIMARY KEY,
    artist_id INT NOT NULL,
    title VARCHAR(180) NOT NULL,
    creation_year SMALLINT NULL,
    type VARCHAR(80) NOT NULL,
    medium VARCHAR(120) NULL,
    dimensions VARCHAR(100) NULL,
    description TEXT NULL,
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'FOR_SALE',
    CONSTRAINT fk_artwork_artist
        FOREIGN KEY (artist_id) REFERENCES artist (artist_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_artwork_price CHECK (price >= 0),
    CONSTRAINT chk_artwork_status CHECK (status IN ('FOR_SALE', 'SOLD', 'EXHIBITED')),
    CONSTRAINT chk_artwork_creation_year CHECK (creation_year IS NULL OR creation_year BETWEEN 1900 AND 2100)
);

CREATE TABLE artwork_tag (
    tag_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    CONSTRAINT uq_artwork_tag_name UNIQUE (name)
);

CREATE TABLE artwork_tag_map (
    artwork_id INT NOT NULL,
    tag_id INT NOT NULL,
    PRIMARY KEY (artwork_id, tag_id),
    CONSTRAINT fk_artwork_tag_map_artwork
        FOREIGN KEY (artwork_id) REFERENCES artwork (artwork_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_artwork_tag_map_tag
        FOREIGN KEY (tag_id) REFERENCES artwork_tag (tag_id)
        ON DELETE RESTRICT
);

CREATE TABLE gallery (
    gallery_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(255) NOT NULL,
    owner_name VARCHAR(150) NULL,
    opening_hours VARCHAR(120) NULL,
    contact_phone VARCHAR(30) NULL,
    rating DECIMAL(3,2) NULL,
    website VARCHAR(255) NULL,
    CONSTRAINT chk_gallery_rating CHECK (rating IS NULL OR (rating >= 0 AND rating <= 5))
);

CREATE TABLE exhibition (
    exhibition_id INT AUTO_INCREMENT PRIMARY KEY,
    gallery_id INT NOT NULL,
    title VARCHAR(180) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    description TEXT NULL,
    curator_name VARCHAR(150) NULL,
    theme VARCHAR(120) NULL,
    CONSTRAINT fk_exhibition_gallery
        FOREIGN KEY (gallery_id) REFERENCES gallery (gallery_id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_exhibition_dates CHECK (end_date >= start_date)
);

CREATE TABLE exhibition_artwork (
    exhibition_id INT NOT NULL,
    artwork_id INT NOT NULL,
    PRIMARY KEY (exhibition_id, artwork_id),
    CONSTRAINT fk_exhibition_artwork_exhibition
        FOREIGN KEY (exhibition_id) REFERENCES exhibition (exhibition_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_exhibition_artwork_artwork
        FOREIGN KEY (artwork_id) REFERENCES artwork (artwork_id)
        ON DELETE CASCADE
);

CREATE TABLE workshop (
    workshop_id INT AUTO_INCREMENT PRIMARY KEY,
    instructor_artist_id INT NOT NULL,
    title VARCHAR(180) NOT NULL,
    workshop_datetime DATETIME NOT NULL,
    duration_minutes INT NOT NULL,
    max_participants INT NOT NULL,
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    location VARCHAR(255) NULL,
    description TEXT NULL,
    level VARCHAR(30) NULL,
    CONSTRAINT fk_workshop_instructor
        FOREIGN KEY (instructor_artist_id) REFERENCES artist (artist_id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_workshop_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_workshop_max_participants CHECK (max_participants > 0),
    CONSTRAINT chk_workshop_price CHECK (price >= 0),
    CONSTRAINT chk_workshop_level CHECK (level IS NULL OR level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
);

CREATE TABLE community_member (
    member_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    birth_year SMALLINT NULL,
    phone VARCHAR(30) NULL,
    city VARCHAR(120) NULL,
    membership_type VARCHAR(30) NOT NULL DEFAULT 'FREE',
    CONSTRAINT uq_community_member_email UNIQUE (email),
    CONSTRAINT chk_community_member_birth_year CHECK (birth_year IS NULL OR birth_year BETWEEN 1900 AND 2100),
    CONSTRAINT chk_membership_type CHECK (membership_type IN ('FREE', 'PREMIUM'))
);

CREATE TABLE member_favorite_discipline (
    member_id INT NOT NULL,
    discipline_id INT NOT NULL,
    PRIMARY KEY (member_id, discipline_id),
    CONSTRAINT fk_member_favorite_discipline_member
        FOREIGN KEY (member_id) REFERENCES community_member (member_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_member_favorite_discipline_discipline
        FOREIGN KEY (discipline_id) REFERENCES discipline (discipline_id)
        ON DELETE RESTRICT
);

CREATE TABLE booking (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    workshop_id INT NOT NULL,
    member_id INT NOT NULL,
    booking_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT fk_booking_workshop
        FOREIGN KEY (workshop_id) REFERENCES workshop (workshop_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_booking_member
        FOREIGN KEY (member_id) REFERENCES community_member (member_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_booking_workshop_member UNIQUE (workshop_id, member_id),
    CONSTRAINT chk_booking_payment_status CHECK (payment_status IN ('PENDING', 'PAID', 'CANCELLED'))
);

CREATE TABLE review (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL,
    artwork_id INT NOT NULL,
    rating INT NOT NULL,
    comment TEXT NULL,
    review_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT fk_review_member
        FOREIGN KEY (member_id) REFERENCES community_member (member_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_review_artwork
        FOREIGN KEY (artwork_id) REFERENCES artwork (artwork_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_review_member_artwork UNIQUE (member_id, artwork_id),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
);
