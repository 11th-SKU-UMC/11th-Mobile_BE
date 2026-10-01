package com.umc.study.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class RentalRepository {

    private final JdbcTemplate jdbcTemplate;

    // [미션 2] 대여 기록 생성 - 대여일은 지금, 반납 예정일은 7일 뒤
    public int save(Map<String, Object> body) {
        String sql = "INSERT INTO rental (user_id, book_id, rented_at, due_at, returned_at) "
                + "VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), NULL)";
        return jdbcTemplate.update(
                sql,
                body.get("userId"),
                body.get("bookId")
        );
    }

    // [선택 미션] 반납 처리 - 아직 반납 안 된 기록만 갱신 (중복 반납 방지)
    public int markReturned(Long rentalId) {
        String sql = "UPDATE rental SET returned_at = NOW() WHERE rental_id = ? AND returned_at IS NULL";
        return jdbcTemplate.update(sql, rentalId);
    }
}
