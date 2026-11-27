package com.umc.movie.service;

import com.umc.movie.dto.CreateFavoriteRequest;
import com.umc.movie.entity.Member;
import com.umc.movie.exception.ApiException;
import com.umc.movie.repository.FavoriteRepository;
import com.umc.movie.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 스프링을 띄우지 않고 FavoriteService만 떼어서 확인하는 단위 테스트입니다.
 * 레포지토리는 Mockito 가짜(Mock)로 바꿔서 DB 없이 돌아갑니다.
 */
@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    @Test
    @DisplayName("이미 즐겨찾기한 영화를 다시 등록하면 FAVORITE409_1 예외가 나고 저장하지 않는다")
    void create_duplicate_throwsConflict() {
        // given: 회원은 있고, 그 회원이 영화 100번을 이미 즐겨찾기한 상태
        Member member = Member.builder().email("a@test.com").password("hashed").nickname("tester").build();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(favoriteRepository.existsByMember_MemberIdAndMovieId(1L, 100L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> favoriteService.create(1L, new CreateFavoriteRequest(100L)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException ex = (ApiException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getCode()).isEqualTo("FAVORITE409_1");
                });
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("즐겨찾기하지 않은 영화를 해제하면 FAVORITE404_1 예외가 나고 삭제하지 않는다")
    void delete_notFavorited_throwsNotFound() {
        // given: 회원 1번은 영화 100번을 즐겨찾기한 적이 없음
        given(favoriteRepository.findByMember_MemberIdAndMovieId(1L, 100L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> favoriteService.delete(1L, 100L))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException ex = (ApiException) e;
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getCode()).isEqualTo("FAVORITE404_1");
                });
        verify(favoriteRepository, never()).delete(any());
    }
}
