package com.umc.movie.controller;

import com.umc.movie.config.SwaggerConfig;
import com.umc.movie.dto.ApiResponse;
import com.umc.movie.dto.CreateRatingRequest;
import com.umc.movie.dto.RatingResponse;
import com.umc.movie.dto.UpdateRatingRequest;
import com.umc.movie.interceptor.AuthInterceptor;
import com.umc.movie.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "평점", description = "영화 평점 등록/수정/삭제 및 유저별 평점 조회")
@RestController
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    // 5주차에 만든 유저별 평점 조회입니다. 공개 API라 토큰을 받지 않습니다. (자물쇠 없음)
    @Operation(summary = "유저별 평점 조회", description = "특정 회원이 남긴 평점 목록을 최신순으로 조회합니다. 토큰 없이 누구나 볼 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON4002 - 회원 ID 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MEMBER404_1 - 회원을 찾을 수 없음")
    })
    @GetMapping("/members/{memberId}/ratings")
    public List<RatingResponse> getMemberRatings(@PathVariable Long memberId) {
        return ratingService.getRatingsByMember(memberId);
    }

    // 경로에서 /members/{memberId}가 사라지고 memberId는 인터셉터가 담아둔 값을 씁니다
    @Operation(summary = "평점 등록", description = "영화에 별점(1~5)과 후기를 남깁니다. 영화당 한 번만 등록할 수 있습니다.")
    @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON4001 - 영화 ID/별점/후기 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH401_2 - 토큰 없음 / AUTH401_3 - 유효하지 않은 토큰 / AUTH401_4 - 만료된 토큰"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MEMBER404_1 - 회원을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "RATING409_1 - 이미 이 영화에 평점을 남김")
    })
    @PostMapping("/ratings")
    public ResponseEntity<ApiResponse<RatingResponse>> create(
            @Parameter(hidden = true) @RequestAttribute(AuthInterceptor.MEMBER_ID) Long memberId,
            @RequestBody @Valid CreateRatingRequest request
    ) {
        RatingResponse response = ratingService.create(memberId, request);
        return ResponseEntity.status(201)
                .body(ApiResponse.created("평점을 등록했습니다.", response));
    }

    // 경로에서도 /{memberId}가 사라지고 인터셉터가 담아둔 값을 씁니다
    @Operation(summary = "평점 수정", description = "내가 남긴 평점의 별점과 후기를 수정합니다.")
    @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON4001 - 별점/후기 검증 실패 / COMMON4002 - 평점 ID 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH401_2 - 토큰 없음 / AUTH401_3 - 유효하지 않은 토큰 / AUTH401_4 - 만료된 토큰"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "RATING403_1 - 본인이 작성한 평점이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "RATING404_1 - 평점을 찾을 수 없음")
    })
    @PatchMapping("/ratings/{ratingId}")
    public ResponseEntity<ApiResponse<RatingResponse>> update(
            @Parameter(hidden = true) @RequestAttribute(AuthInterceptor.MEMBER_ID) Long memberId,
            @PathVariable Long ratingId,
            @RequestBody @Valid UpdateRatingRequest request
    ) {
        RatingResponse response = ratingService.update(memberId, ratingId, request);
        return ResponseEntity.ok(ApiResponse.ok("평점을 수정했습니다.", response));
    }

    @Operation(summary = "평점 삭제", description = "내가 남긴 평점을 삭제합니다.")
    @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON4002 - 평점 ID 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH401_2 - 토큰 없음 / AUTH401_3 - 유효하지 않은 토큰 / AUTH401_4 - 만료된 토큰"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "RATING403_1 - 본인이 작성한 평점이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "RATING404_1 - 평점을 찾을 수 없음")
    })
    @DeleteMapping("/ratings/{ratingId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(hidden = true) @RequestAttribute(AuthInterceptor.MEMBER_ID) Long memberId,
            @PathVariable Long ratingId
    ) {
        ratingService.delete(memberId, ratingId);
        return ResponseEntity.ok(ApiResponse.ok("평점을 삭제했습니다.", null));
    }
}
