// src/main/java/com/umc/movie/controller/RatingController.java
package com.umc.movie.controller;

import com.umc.movie.dto.RatingResponse;
import com.umc.movie.dto.ApiResponse;
import com.umc.movie.dto.CreateRatingRequest;
import com.umc.movie.dto.UpdateRatingRequest;
import com.umc.movie.service.RatingService;
import com.umc.movie.auth.LoginMember;
import com.umc.movie.config.ApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.umc.movie.exception.ErrorCode.DUPLICATE_RATING;
import static com.umc.movie.exception.ErrorCode.MEMBER_NOT_FOUND;
import static com.umc.movie.exception.ErrorCode.RATING_FORBIDDEN;
import static com.umc.movie.exception.ErrorCode.RATING_NOT_FOUND;

@RestController
@RequiredArgsConstructor
@Tag(name = "평점", description = "평점 등록, 수정, 삭제와 회원별 목록 조회")
public class RatingController {

    private final RatingService ratingService;

    @Operation(summary = "유저별 평점 조회", description = "회원 ID로 해당 회원의 공개 평점 목록을 최신 등록 순서로 조회합니다.")
    @ApiErrors(MEMBER_NOT_FOUND)
    @GetMapping("/members/{memberId}/ratings")
    public List<RatingResponse> getMemberRatings(@PathVariable Long memberId) {
        return ratingService.getRatingsByMember(memberId);
    }

    @Operation(summary = "평점 등록", description = "로그인한 회원이 영화에 1~5점의 평점과 선택적인 후기를 남깁니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "평점 등록 성공", useReturnTypeSchema = true)
    @ApiErrors({MEMBER_NOT_FOUND, DUPLICATE_RATING})
    @PostMapping("/ratings")
    public ResponseEntity<ApiResponse<RatingResponse>> create(
            @Parameter(hidden = true) @LoginMember Long memberId, @Valid @RequestBody CreateRatingRequest request) {
        return ResponseEntity.status(201).body(
                ApiResponse.created("평점 등록에 성공했습니다.", ratingService.create(memberId, request)));
    }

    @Operation(summary = "평점 수정", description = "로그인한 회원이 직접 작성한 평점과 후기를 수정합니다.")
    @ApiErrors({RATING_FORBIDDEN, RATING_NOT_FOUND})
    @PatchMapping("/ratings/{ratingId}")
    public ResponseEntity<ApiResponse<RatingResponse>> update(
            @Parameter(hidden = true) @LoginMember Long memberId, @PathVariable Long ratingId,
            @Valid @RequestBody UpdateRatingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("평점 수정에 성공했습니다.", ratingService.update(memberId, ratingId, request)));
    }

    @Operation(summary = "평점 삭제", description = "로그인한 회원이 직접 작성한 평점을 삭제합니다.")
    @ApiErrors({RATING_FORBIDDEN, RATING_NOT_FOUND})
    @DeleteMapping("/ratings/{ratingId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(hidden = true) @LoginMember Long memberId, @PathVariable Long ratingId) {
        ratingService.delete(memberId, ratingId);
        return ResponseEntity.ok(ApiResponse.ok("평점을 삭제했습니다.", null));
    }
}
