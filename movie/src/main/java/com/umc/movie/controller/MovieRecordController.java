package com.umc.movie.controller;

import com.umc.movie.auth.LoginMember;
import com.umc.movie.config.ApiErrors;
import com.umc.movie.dto.ApiResponse;
import com.umc.movie.dto.CreateMovieRecordRequest;
import com.umc.movie.dto.MovieRecordResponse;
import com.umc.movie.service.MovieRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.umc.movie.exception.ErrorCode.DUPLICATE_FAVORITE;
import static com.umc.movie.exception.ErrorCode.DUPLICATE_RATING;
import static com.umc.movie.exception.ErrorCode.MEMBER_NOT_FOUND;

@RestController
@RequiredArgsConstructor
@Tag(name = "영화 기록", description = "평점과 즐겨찾기 한 번에 저장")
public class MovieRecordController {

    private final MovieRecordService movieRecordService;

    @Operation(summary = "영화 기록 저장", description = "평점을 등록합니다. addToFavorites가 true이면 즐겨찾기도 동일한 트랜잭션으로 저장합니다.")
    @ApiErrors({MEMBER_NOT_FOUND, DUPLICATE_RATING, DUPLICATE_FAVORITE})
    @PostMapping("/movie-records")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MovieRecordResponse> create(
            @Parameter(hidden = true) @LoginMember Long memberId, @Valid @RequestBody CreateMovieRecordRequest request) {
        return ApiResponse.created("영화 기록을 저장했습니다.", movieRecordService.create(memberId, request));
    }
}
