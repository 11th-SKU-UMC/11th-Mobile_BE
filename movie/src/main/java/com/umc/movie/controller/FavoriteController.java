package com.umc.movie.controller;

import com.umc.movie.auth.LoginMember;
import com.umc.movie.config.ApiErrors;
import com.umc.movie.dto.ApiResponse;
import com.umc.movie.dto.CreateFavoriteRequest;
import com.umc.movie.dto.FavoriteResponse;
import com.umc.movie.dto.FavoriteStatusResponse;
import com.umc.movie.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.umc.movie.exception.ErrorCode.DUPLICATE_FAVORITE;
import static com.umc.movie.exception.ErrorCode.FAVORITE_NOT_FOUND;
import static com.umc.movie.exception.ErrorCode.MEMBER_NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Tag(name = "즐겨찾기", description = "로그인한 회원의 영화 즐겨찾기 관리")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "즐겨찾기 등록", description = "TMDB 영화 ID로 즐겨찾기를 등록하고 저장된 항목을 반환합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "즐겨찾기 등록 성공 (COMMON201)", useReturnTypeSchema = true,
            content = @Content(mediaType = APPLICATION_JSON_VALUE, examples = @ExampleObject(value = """
                    {
                      "isSuccess": true,
                      "code": "COMMON201",
                      "message": "즐겨찾기를 등록했습니다.",
                      "result": {"favoriteId": 1, "movieId": 550, "createdAt": "2026-11-26T13:00:00"}
                    }
                    """)))
    @ApiErrors({MEMBER_NOT_FOUND, DUPLICATE_FAVORITE})
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FavoriteResponse> create(
            @Parameter(hidden = true) @LoginMember Long memberId, @Valid @RequestBody CreateFavoriteRequest request) {
        return ApiResponse.created("즐겨찾기를 등록했습니다.", favoriteService.create(memberId, request));
    }

    @Operation(summary = "내 즐겨찾기 조회", description = "로그인한 회원의 즐겨찾기를 최신 등록 순서로 조회합니다.")
    @GetMapping
    public ApiResponse<List<FavoriteResponse>> getMyFavorites(@Parameter(hidden = true) @LoginMember Long memberId) {
        return ApiResponse.ok("즐겨찾기 목록을 조회했습니다.", favoriteService.getMyFavorites(memberId));
    }

    @Operation(summary = "즐겨찾기 여부 조회", description = "로그인한 회원이 이 영화를 즐겨찾기했는지 확인합니다. 등록한 기록이 없으면 isFavorite이 false입니다.")
    @GetMapping("/{movieId}")
    public ApiResponse<FavoriteStatusResponse> getStatus(
            @Parameter(hidden = true) @LoginMember Long memberId, @PathVariable @Positive Long movieId) {
        return ApiResponse.ok("즐겨찾기 여부를 조회했습니다.", favoriteService.getStatus(memberId, movieId));
    }

    @Operation(summary = "즐겨찾기 해제", description = "로그인한 회원이 이 영화에 등록한 즐겨찾기를 해제합니다.")
    @ApiErrors(FAVORITE_NOT_FOUND)
    @DeleteMapping("/{movieId}")
    public ApiResponse<Void> delete(
            @Parameter(hidden = true) @LoginMember Long memberId, @PathVariable @Positive Long movieId) {
        favoriteService.delete(memberId, movieId);
        return ApiResponse.ok("즐겨찾기를 해제했습니다.", null);
    }
}
