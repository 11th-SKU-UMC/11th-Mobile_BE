package com.umc.movie.repository;

import com.umc.movie.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByMember_MemberIdOrderByRatingIdDesc(Long memberId);

    // 언더스코어(_)는 연관 엔티티를 타고 들어가라는 표시: rating.member.memberId 를 조건으로 삼습니다.
    boolean existsByMember_MemberIdAndMovieId(Long memberId, Long movieId);
}
