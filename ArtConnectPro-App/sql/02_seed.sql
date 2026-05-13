USE artconnect_pro;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE review;
TRUNCATE TABLE booking;
TRUNCATE TABLE member_favorite_discipline;
TRUNCATE TABLE community_member;
TRUNCATE TABLE exhibition_artwork;
TRUNCATE TABLE exhibition;
TRUNCATE TABLE workshop;
TRUNCATE TABLE gallery;
TRUNCATE TABLE artwork_tag_map;
TRUNCATE TABLE artwork_tag;
TRUNCATE TABLE artwork;
TRUNCATE TABLE artist_discipline;
TRUNCATE TABLE discipline;
TRUNCATE TABLE artist;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO artist (artist_id, name, bio, birth_year, contact_email, phone, city, website, social_media, is_active) VALUES
(1, 'Leonardo Vinci', 'Peintre et createur passionne par les techniques classiques et les portraits intemporels.', 1985, 'leo@vincistudio.it', '+39 055 100 200', 'Florence', 'https://vincistudio.it', '@leovinci', TRUE),
(2, 'Claude Monet', 'Artiste inspire par la lumiere, les jardins et les paysages impressionnistes.', 1980, 'claude@monet.fr', '+33 1 45 10 20 30', 'Giverny', 'https://monet.fr', '@claudemonet', TRUE),
(3, 'Ansel Adams', 'Photographe specialise dans les paysages et la mise en valeur de la nature.', 1978, 'ansel@adams.co', '+1 415 555 1001', 'San Francisco', 'https://anseladams.co', '@anseladams', TRUE),
(4, 'Frida Kahlo', 'Artiste connue pour ses portraits puissants et son univers symbolique.', 1988, 'frida@kahlo.mx', '+52 55 5555 0101', 'Mexico City', 'https://fridakahlo.mx', '@fridakahlo', TRUE),
(5, 'Auguste Rodin', 'Sculpteur reconnu pour ses oeuvres expressives et son approche du mouvement.', 1975, 'auguste@rodin.fr', '+33 1 40 00 10 20', 'Paris', 'https://rodin.fr', '@augusterodin', TRUE);

INSERT INTO discipline (discipline_id, name) VALUES
(1, 'Painting'),
(2, 'Sculpture'),
(3, 'Photography'),
(4, 'Digital Art'),
(5, 'Mixed Media');

INSERT INTO artist_discipline (artist_id, discipline_id) VALUES
(1, 1),
(1, 2),
(2, 1),
(3, 3),
(4, 1),
(4, 5),
(5, 2);

INSERT INTO artwork (artwork_id, artist_id, title, creation_year, type, medium, dimensions, description, price, status) VALUES
(1, 1, 'Mona Lisa', 2018, 'Painting', 'Oil on panel', '77 cm x 53 cm', 'Portrait celebre au sourire mysterieux.', 85000000.00, 'EXHIBITED'),
(2, 1, 'The Last Supper', 2016, 'Painting', 'Tempera and oil', '460 cm x 880 cm', 'Scene religieuse monumentale inspiree de la Renaissance.', 45000000.00, 'EXHIBITED'),
(3, 2, 'Water Lilies', 2019, 'Painting', 'Oil on canvas', '200 cm x 180 cm', 'Serie inspiree du jardin et des reflets de lumiere.', 4000000.00, 'FOR_SALE'),
(4, 3, 'Monolith, The Face of Half Dome', 2020, 'Photography', 'Black and white print', '40 cm x 50 cm', 'Photographie iconique du Yosemite.', 100000.00, 'FOR_SALE'),
(5, 4, 'The Two Fridas', 2021, 'Painting', 'Oil on canvas', '173 cm x 173 cm', 'Double autoportrait explorant identite et emotion.', 5000000.00, 'EXHIBITED'),
(6, 5, 'The Thinker', 2017, 'Sculpture', 'Bronze', '186 cm x 98 cm', 'Sculpture expressive representant la reflexion humaine.', 15000000.00, 'EXHIBITED'),
(7, 2, 'Impression Sunrise', 2015, 'Painting', 'Oil on canvas', '48 cm x 63 cm', 'Oeuvre evocatrice de la naissance de l impressionnisme.', 3200000.00, 'SOLD'),
(8, 4, 'Self-Portrait with Thorn Necklace', 2022, 'Painting', 'Oil on canvas', '61 cm x 47 cm', 'Autoportrait symbolique aux details marquants.', 2800000.00, 'FOR_SALE');

INSERT INTO artwork_tag (tag_id, name) VALUES
(1, 'Portrait'),
(2, 'Nature'),
(3, 'Renaissance'),
(4, 'Black and White'),
(5, 'Impressionism'),
(6, 'Symbolism'),
(7, 'Bronze');

INSERT INTO artwork_tag_map (artwork_id, tag_id) VALUES
(1, 1),
(1, 3),
(2, 3),
(3, 2),
(3, 5),
(4, 2),
(4, 4),
(5, 1),
(5, 6),
(6, 7),
(7, 5),
(8, 1),
(8, 6);

INSERT INTO gallery (gallery_id, name, address, owner_name, opening_hours, contact_phone, rating, website) VALUES
(1, 'Louvre Art House', 'Rue de Rivoli, Paris', 'Camille Durand', '09:00-18:00', '+33 1 44 55 66 77', 4.90, 'https://louvrearthouse.fr'),
(2, 'The British Gallery', 'Great Russell St, London', 'Edward Collins', '10:00-18:30', '+44 20 7000 1000', 4.70, 'https://britishgallery.uk'),
(3, 'Metropolitan Hub', '1000 5th Ave, New York', 'Sophia Reed', '10:00-19:00', '+1 212 555 2020', 4.80, 'https://metropolitanhub.us');

INSERT INTO exhibition (exhibition_id, gallery_id, title, start_date, end_date, description, curator_name, theme) VALUES
(1, 1, 'Renaissance Revival', '2026-04-01', '2026-08-31', 'Exposition consacree aux chefs-d oeuvre inspires de la Renaissance.', 'Dr. Elena Rossi', 'Classic Renaissance'),
(2, 2, 'Sculpting the Soul', '2026-05-01', '2026-09-15', 'Selection d oeuvres autour du geste, de la matiere et de l expression.', 'Marcus Thorne', 'Modern and Classical Sculpture'),
(3, 3, 'Impressionist Dreams', '2026-03-15', '2026-07-30', 'Parcours artistique autour de la couleur, de la lumiere et du paysage.', 'Sarah Jenkins', 'Light and Color');

INSERT INTO exhibition_artwork (exhibition_id, artwork_id) VALUES
(1, 1),
(1, 2),
(2, 6),
(3, 3),
(3, 7);

INSERT INTO workshop (workshop_id, instructor_artist_id, title, workshop_datetime, duration_minutes, max_participants, price, location, description, level) VALUES
(1, 1, 'Mastering Oil Painting', '2026-06-10 14:00:00', 180, 10, 150.00, 'Florence Studio', 'Atelier consacre aux bases du portrait et des glacis en peinture a l huile.', 'INTERMEDIATE'),
(2, 2, 'Impressionist Landscapes', '2026-06-15 10:00:00', 150, 12, 120.00, 'Giverny Gardens', 'Travail sur la lumiere, la touche et la composition en exterieur.', 'BEGINNER'),
(3, 5, 'Sculpting Modernity', '2026-06-20 15:30:00', 210, 8, 200.00, 'Paris Workshop', 'Initiation a la sculpture expressive et aux volumes du corps.', 'ADVANCED');

INSERT INTO community_member (member_id, name, email, birth_year, phone, city, membership_type) VALUES
(1, 'Alice Wonderland', 'alice@art.com', 1995, '+33 6 10 20 30 40', 'Paris', 'PREMIUM'),
(2, 'Bob Ross', 'bob@happytrees.com', 1988, '+44 7700 900100', 'London', 'FREE'),
(3, 'Charlie Brown', 'charlie@peanuts.com', 1992, '+1 646 555 0101', 'New York', 'PREMIUM'),
(4, 'Sofia Martinez', 'sofia@creativehub.mx', 1998, '+52 55 5000 1111', 'Mexico City', 'FREE');

INSERT INTO member_favorite_discipline (member_id, discipline_id) VALUES
(1, 1),
(1, 5),
(2, 3),
(3, 2),
(3, 1),
(4, 1),
(4, 5);

INSERT INTO booking (booking_id, workshop_id, member_id, booking_date, payment_status) VALUES
(1, 1, 1, '2026-05-05 11:00:00', 'PAID'),
(2, 2, 2, '2026-05-06 09:30:00', 'PENDING'),
(3, 3, 3, '2026-05-07 14:15:00', 'PAID'),
(4, 2, 4, '2026-05-08 16:45:00', 'CANCELLED');

INSERT INTO review (review_id, member_id, artwork_id, rating, comment, review_date) VALUES
(1, 1, 1, 5, 'Un portrait fascinant et intemporel.', '2026-05-02'),
(2, 2, 3, 4, 'Les couleurs et l atmosphere sont magnifiques.', '2026-05-03'),
(3, 3, 6, 5, 'Une oeuvre puissante qui transmet une vraie tension interieure.', '2026-05-04'),
(4, 4, 5, 4, 'Une peinture intense et tres touchante.', '2026-05-05');
