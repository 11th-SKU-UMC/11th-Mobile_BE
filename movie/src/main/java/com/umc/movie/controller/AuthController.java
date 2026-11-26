package com.umc.movie.controller;

import com.umc.movie.dto.ApiResponse;
import com.umc.movie.dto.LoginRequest;
import com.umc.movie.dto.LoginResponse;
import com.umc.movie.service.AuthService;
import com.umc.movie.config.ApiErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.umc.movie.exception.ErrorCode.INVALID_CREDENTIALS;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "로그인과 액세스 토큰 발급")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "이메일 로그인", description = "이메일과 비밀번호로 로그인합니다. 응답의 accessToken을 Authorize에 입력해 인증 API를 호출할 수 있습니다.")
    @ApiErrors(INVALID_CREDENTIALS)
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("로그인에 성공했습니다.", authService.login(request)));
    }
}
