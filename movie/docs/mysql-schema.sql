-- 신규 실습용 DB에 적용합니다. 기존 5주차 테이블이 있으면 SHOW CREATE TABLE로 먼저 비교하세요.
CREATE TABLE member (
    member_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password VARCHAR(60) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_member_email UNIQUE (email),
    CONSTRAINT uk_member_nickname UNIQUE (nickname)
);

CREATE TABLE rating (
    rating_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    score TINYINT NOT NULL,
    comment TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_rating_member FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT uk_rating_member_movie UNIQUE (member_id, movie_id),
    CONSTRAINT ck_rating_score CHECK (score BETWEEN 1 AND 5),
    CONSTRAINT ck_rating_movie CHECK (movie_id > 0),
    CONSTRAINT ck_rating_comment CHECK (CHAR_LENGTH(comment) <= 500)
);
