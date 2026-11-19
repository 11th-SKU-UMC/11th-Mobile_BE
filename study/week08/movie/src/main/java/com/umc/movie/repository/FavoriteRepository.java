package com.umc.movie.repository;

import com.umc.movie.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // 내 즐겨찾기 목록 (최신 등록순)
    List<Favorite> findByMember_MemberIdOrderByFavoriteIdDesc(Long memberId);

    // 해제할 때 한 건 찾기
    Optional<Favorite> findByMember_MemberIdAndMovieId(Long memberId, Long movieId);

    // 이미 즐겨찾기했는지 true/false
    boolean existsByMember_MemberIdAndMovieId(Long memberId, Long movieId);
}
