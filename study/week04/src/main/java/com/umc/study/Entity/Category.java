package com.umc.study.Entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;



    @Entity                          // 이 클래스는 DB 테이블이랑 연결된 거야
    @Table(name = "category")        // 연결할 테이블은 category
    @Getter                          // getCategoryId(), getName() 자동 생성
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public class Category {

        @Id                                                  // 이게 PK(번호표)
        @GeneratedValue(strategy = GenerationType.IDENTITY)  // 번호는 DB가 자동으로
        @Column(name = "category_id")                        // DB에선 category_id
        private Long categoryId;

        @Column(nullable = false)                            // 비어 있으면 안 됨
        private String name;                                 // 카테고리 이름 (소설, 에세이...)
    }

