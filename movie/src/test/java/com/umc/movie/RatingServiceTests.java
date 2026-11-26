package com.umc.movie;

import com.umc.movie.dto.CreateRatingRequest;
import com.umc.movie.dto.UpdateRatingRequest;
import com.umc.movie.entity.Member;
import com.umc.movie.entity.Rating;
import com.umc.movie.exception.ApiException;
import com.umc.movie.exception.ErrorCode;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.repository.RatingRepository;
import com.umc.movie.service.RatingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RatingServiceTests {
    @Mock RatingRepository ratings;
    @Mock MemberRepository members;
    @InjectMocks RatingService service;

    @Test
    void rejectsDuplicateRatingWithRating409_1WithoutSaving() {
        Member member = new Member("rating@example.com", "unused", "리뷰어");
        given(members.findById(1L)).willReturn(Optional.of(member));
        given(ratings.existsByMember_MemberIdAndMovieId(1L, 550L)).willReturn(true);

        var exception = assertThrows(ApiException.class,
                () -> service.create(1L, new CreateRatingRequest(550L, 5, "다시 보고 싶은 영화")));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_RATING);
        assertThat(exception.getErrorCode().getCode()).isEqualTo("RATING409_1");
        verify(ratings, never()).saveAndFlush(any(Rating.class));
    }

    @Test
    void rejectsUpdatingAnotherMembersRatingWithRating403_1WithoutChangingIt() {
        Member owner = mock(Member.class);
        given(owner.getMemberId()).willReturn(2L);
        Rating rating = new Rating(owner, 550L, 3, "기존 후기");
        given(ratings.findById(10L)).willReturn(Optional.of(rating));

        var exception = assertThrows(ApiException.class,
                () -> service.update(1L, 10L, new UpdateRatingRequest(5, "수정된 후기")));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RATING_FORBIDDEN);
        assertThat(exception.getErrorCode().getCode()).isEqualTo("RATING403_1");
        assertThat(rating.getScore()).isEqualTo(3);
        assertThat(rating.getComment()).isEqualTo("기존 후기");
        verify(ratings, never()).saveAndFlush(any(Rating.class));
    }
}
