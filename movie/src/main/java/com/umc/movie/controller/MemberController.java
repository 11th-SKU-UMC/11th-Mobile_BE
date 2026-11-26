// src/main/java/com/umc/movie/controller/MemberController.java
package com.umc.movie.controller;

import com.umc.movie.dto.EmailCheckResponse;
import com.umc.movie.dto.NicknameCheckResponse;
import com.umc.movie.dto.ApiResponse;
import com.umc.movie.dto.MemberResponse;
import com.umc.movie.dto.SignUpRequest;
import com.umc.movie.dto.MemberInfoResponse;
import com.umc.movie.dto.MemberUpdateRequest;
import com.umc.movie.auth.LoginMember;
import com.umc.movie.service.MemberService;
import com.umc.movie.config.ApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.umc.movie.exception.ErrorCode.DUPLICATE_EMAIL;
import static com.umc.movie.exception.ErrorCode.DUPLICATE_NICKNAME;
import static com.umc.movie.exception.ErrorCode.MEMBER_NOT_FOUND;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
@Tag(name = "회원", description = "회원가입, 중복 확인, 내 프로필 관리")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "내 정보 조회", description = "로그인한 회원의 이메일과 닉네임을 조회합니다.")
    @ApiErrors(MEMBER_NOT_FOUND)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> getMyInfo(@Parameter(hidden = true) @LoginMember Long memberId) {
        return ResponseEntity.ok(ApiResponse.ok("회원 정보를 조회했습니다.", memberService.getMyInfo(memberId)));
    }

    @Operation(summary = "내 정보 수정", description = "로그인한 회원의 닉네임을 수정합니다.")
    @ApiErrors({MEMBER_NOT_FOUND, DUPLICATE_NICKNAME})
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> updateMyInfo(
            @Parameter(hidden = true) @LoginMember Long memberId, @Valid @RequestBody MemberUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("회원 정보를 수정했습니다.", memberService.updateMyInfo(memberId, request)));
    }

    @Operation(summary = "회원가입", description = "사용 가능한 이메일과 닉네임으로 가입합니다. 비밀번호는 영문 대소문자, 숫자, 특수문자를 포함한 8자 이상이며 UTF-8 기준 72바이트 이하여야 합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공", useReturnTypeSchema = true)
    @ApiErrors({DUPLICATE_EMAIL, DUPLICATE_NICKNAME})
    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(201).body(
                ApiResponse.created("회원가입에 성공했습니다.", memberService.signUp(request)));
    }

    @Operation(summary = "닉네임 중복 확인", description = "닉네임을 사용할 수 있는지 확인합니다.")
    @GetMapping("/nickname/{nickname}")
    public NicknameCheckResponse checkNickname(@PathVariable String nickname) {
        return memberService.checkNickname(nickname);
    }

    @Operation(summary = "이메일 중복 확인", description = "이메일을 사용할 수 있는지 확인합니다.")
    @GetMapping("/email/{email}")
    public EmailCheckResponse checkEmail(@PathVariable String email) {
        return memberService.checkEmail(email);
    }
}
