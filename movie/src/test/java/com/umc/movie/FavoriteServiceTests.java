package com.umc.movie;

import com.umc.movie.dto.CreateFavoriteRequest;
import com.umc.movie.entity.Favorite;
import com.umc.movie.entity.Member;
import com.umc.movie.exception.ApiException;
import com.umc.movie.exception.ErrorCode;
import com.umc.movie.repository.FavoriteRepository;
import com.umc.movie.repository.MemberRepository;
import com.umc.movie.service.FavoriteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTests {
    @Mock FavoriteRepository favorites;
    @Mock MemberRepository members;
    @InjectMocks FavoriteService service;

    @Test
    void createsFavoriteForMemberWhenMovieIsNotAlreadyFavorited() {
        Member member = new Member("favorite@example.com", "unused", "영화조아");
        given(members.findById(1L)).willReturn(Optional.of(member));
        given(favorites.existsByMember_MemberIdAndMovieId(1L, 550L)).willReturn(false);
        given(favorites.saveAndFlush(any(Favorite.class))).willAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(1L, new CreateFavoriteRequest(550L));

        assertThat(response.movieId()).isEqualTo(550L);
        verify(favorites).saveAndFlush(argThat(favorite ->
                favorite.getMember() == member && favorite.getMovieId().equals(550L)));
    }

    @Test
    void rejectsDuplicateFavoriteWithFavorite409_1WithoutSaving() {
        Member member = new Member("favorite@example.com", "unused", "영화조아");
        given(members.findById(1L)).willReturn(Optional.of(member));
        given(favorites.existsByMember_MemberIdAndMovieId(1L, 550L)).willReturn(true);
        // The mutation exercise reaches save when the duplicate check is removed.
        lenient().when(favorites.saveAndFlush(any(Favorite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var exception = assertThrows(ApiException.class, () -> service.create(1L, new CreateFavoriteRequest(550L)));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_FAVORITE);
        assertThat(exception.getErrorCode().getCode()).isEqualTo("FAVORITE409_1");
        verify(favorites, never()).saveAndFlush(any(Favorite.class));
    }
}
