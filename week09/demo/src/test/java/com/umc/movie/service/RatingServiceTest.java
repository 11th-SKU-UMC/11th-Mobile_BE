// src/test/java/com/umc/movie/service/RatingServiceTest.java
package com.umc.movie.service;

import com.umc.movie.dto.CreateRatingRequest;
import com.umc.movie.dto.UpdateRatingRequest;
import com.umc.movie.entity.Member;
import com.umc.movie.entity.Rating;
import com.umc.movie.exception.ApiException;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.repository.RatingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private RatingService ratingService;

    private Member newMember(String email, String nickname, Long memberId) {
        Member member = Member.builder()
                .email(email)
                .password("암호화된비밀번호")
                .nickname(nickname)
                .build();
        // memberId는 DB가 만들어주는 값이라 테스트에서는 직접 채워 넣습니다
        ReflectionTestUtils.setField(member, "memberId", memberId);
        return member;
    }

    @Test
    @DisplayName("같은 영화에 평점을 중복 등록하면 409와 RATING409_1 예외를 던지고 저장하지 않는다")
    void createRatingWhenDuplicated() {
        // given: 회원은 존재하고, 이 영화에는 이미 평점을 남긴 상황
        given(memberRepository.findById(1L)).willReturn(Optional.of(newMember("a@umc.com", "영화조아", 1L)));
        given(ratingRepository.existsByMember_MemberIdAndMovieId(1L, 550L)).willReturn(true);

        // when & then: 예외가 나고, 저장은 시도조차 하지 않는다
        assertThatThrownBy(() -> ratingService.create(1L, new CreateRatingRequest(550L, 5, "인생 영화")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("RATING409_1");
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });

        verify(ratingRepository, never()).save(any(Rating.class));
    }

    @Test
    @DisplayName("남의 평점을 수정하려 하면 403과 RATING403_1 예외를 던지고 값은 바뀌지 않는다")
    void updateRatingOfOthers() {
        // given: 평점의 주인은 2번 회원인데, 1번 회원이 수정하려는 상황
        Member owner = newMember("owner@umc.com", "주인장", 2L);
        Rating rating = Rating.builder().member(owner).movieId(550L).score(4).comment("원래 후기").build();
        given(ratingRepository.findById(10L)).willReturn(Optional.of(rating));

        // when & then: 예외가 나고, 저장도 되지 않고, 평점 값도 그대로다
        assertThatThrownBy(() -> ratingService.update(1L, 10L, new UpdateRatingRequest(1, "수정 시도")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("RATING403_1");
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        verify(ratingRepository, never()).save(any(Rating.class));
        assertThat(rating.getScore()).isEqualTo(4);
    }
}
