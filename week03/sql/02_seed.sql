INSERT INTO users (nickname) VALUES ('민서'), ('수현');
INSERT INTO category (name) VALUES ('문학'), ('과학');
INSERT INTO book (category_id, title, description, is_available) VALUES
  (1, '달빛 도서관', '소설', TRUE),
  (2, '코스모스', '과학 교양', TRUE);
INSERT INTO rental (user_id, book_id, rented_at, due_at, returned_at)
VALUES (1, 1, '2026-08-10 10:00:00', '2026-08-17 10:00:00', NULL);
