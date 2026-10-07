-- 2주차 스키마를 3주차 실습 기준으로 재구성한 참고용 파일
-- (이미 2주차 테이블이 있으면 실행하지 않아도 됨)
CREATE TABLE IF NOT EXISTS users (
    user_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS category (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS book (
    book_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id  BIGINT       NOT NULL,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    is_available BOOLEAN      NOT NULL DEFAULT TRUE,
    FOREIGN KEY (category_id) REFERENCES category (category_id)
);

CREATE TABLE IF NOT EXISTS rental (
    rental_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL,
    book_id     BIGINT   NOT NULL,
    rented_at   DATETIME NOT NULL,
    due_at      DATETIME NOT NULL,
    returned_at DATETIME NULL,
    FOREIGN KEY (user_id) REFERENCES users (user_id),
    FOREIGN KEY (book_id) REFERENCES book (book_id)
);
