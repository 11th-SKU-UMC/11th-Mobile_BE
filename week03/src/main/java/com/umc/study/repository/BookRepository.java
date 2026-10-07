package com.umc.study.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class BookRepository {

    private final JdbcTemplate jdbcTemplate;

    // [실습 1] 도서 전체 목록 조회
    public List<Map<String, Object>> findAll() {
        String sql = "SELECT * FROM book";
        return jdbcTemplate.queryForList(sql);
    }

    // [실습 2] 신규 도서 등록 (book_id는 AUTO_INCREMENT라 생략)
    public void save(Map<String, Object> body) {
        String sql = "INSERT INTO book (category_id, title, description, is_available) VALUES (?, ?, ?, true)";
        jdbcTemplate.update(
                sql,
                body.get("categoryId"),
                body.get("title"),
                body.get("description")
        );
    }

    // [미션 1] 특정 카테고리 도서 목록 조회 - 파라미터 바인딩으로 SQL Injection 방지
    public List<Map<String, Object>> findByCategoryId(Long categoryId) {
        String sql = "SELECT * FROM book WHERE category_id = ?";
        return jdbcTemplate.queryForList(sql, categoryId);
    }
}
