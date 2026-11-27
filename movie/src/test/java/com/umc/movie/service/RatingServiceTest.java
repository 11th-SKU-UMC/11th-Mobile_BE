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

/**
 * RatingService의 비즈니스 규칙(중복 금지, 본인 것만 수정)을 DB 없이 확인하는 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private RatingService ratingService;

    @Test
    @DisplayName("같은 영화에 평점을 두 번 등록하면 RATING409_1 예외가 나고 저장하지 않는다")
    void create_duplicate_throwsConflict() {
        // given: 회원 1번이 영화 100번에 이미 평점을 남긴 상태
        Member member = member(1L, "writer");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(ratingRepository.existsByMember_MemberIdAndMovieId(1L, 100L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> ratingService.create(1L, new CreateRatingRequest(100L, 5, "또 남기기")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException ex = (ApiException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getCode()).isEqualTo("RATING409_1");
                });
        verify(ratingRepository, never()).save(any());
    }

    @Test
    @DisplayName("남이 작성한 평점을 수정하려 하면 RATING403_1 예외가 나고 평점 내용은 그대로다")
    void update_othersRating_throwsForbidden() {
        // given: 평점 10번은 회원 1번이 쓴 것인데 회원 2번이 수정을 시도
        Member owner = member(1L, "owner");
        Rating rating = Rating.builder().member(owner).movieId(100L).score(3).comment("원래 후기").build();
        given(ratingRepository.findById(10L)).willReturn(Optional.of(rating));

        // when & then
        assertThatThrownBy(() -> ratingService.update(2L, 10L, new UpdateRatingRequest(1, "몰래 수정")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException ex = (ApiException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getCode()).isEqualTo("RATING403_1");
                });
        assertThat(rating.getScore()).isEqualTo(3);
        assertThat(rating.getComment()).isEqualTo("원래 후기");
        verify(ratingRepository, never()).save(any());
    }

    // memberId는 DB가 채워주는 값이라 setter가 없습니다. 테스트에서만 리플렉션으로 넣어줍니다.
    private Member member(Long memberId, String nickname) {
        Member member = Member.builder()
                .email(nickname + "@test.com")
                .password("hashed")
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        return member;
    }
}
