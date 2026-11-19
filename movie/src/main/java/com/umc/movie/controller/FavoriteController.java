package com.umc.movie.controller;

import com.umc.movie.auth.LoginMember;
import com.umc.movie.dto.ApiResponse;
import com.umc.movie.dto.CreateFavoriteRequest;
import com.umc.movie.dto.FavoriteResponse;
import com.umc.movie.dto.FavoriteStatusResponse;
import com.umc.movie.service.FavoriteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FavoriteResponse> create(
            @LoginMember Long memberId, @Valid @RequestBody CreateFavoriteRequest request) {
        return ApiResponse.created("즐겨찾기를 등록했습니다.", favoriteService.create(memberId, request));
    }

    @GetMapping
    public ApiResponse<List<FavoriteResponse>> getMyFavorites(@LoginMember Long memberId) {
        return ApiResponse.ok("즐겨찾기 목록을 조회했습니다.", favoriteService.getMyFavorites(memberId));
    }

    @GetMapping("/{movieId}")
    public ApiResponse<FavoriteStatusResponse> getStatus(@LoginMember Long memberId, @PathVariable @Positive Long movieId) {
        return ApiResponse.ok("즐겨찾기 여부를 조회했습니다.", favoriteService.getStatus(memberId, movieId));
    }

    @DeleteMapping("/{movieId}")
    public ApiResponse<Void> delete(@LoginMember Long memberId, @PathVariable @Positive Long movieId) {
        favoriteService.delete(memberId, movieId);
        return ApiResponse.ok("즐겨찾기를 해제했습니다.", null);
    }
}
