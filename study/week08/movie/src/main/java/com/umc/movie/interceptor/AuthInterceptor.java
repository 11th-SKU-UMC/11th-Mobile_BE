package com.umc.movie.interceptor;

import com.umc.movie.exception.ApiException;
import com.umc.movie.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String MEMBER_ID = "memberId";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 브라우저 사전 요청(OPTIONS)은 토큰이 없으니 통과
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        // 1. 헤더에서 토큰 꺼내기
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH401_2", "로그인이 필요한 요청입니다.");
        }

        // 2. 검증해서 memberId 얻기 (실패하면 JwtUtil이 401 던짐)
        Long memberId = jwtUtil.getMemberId(header.substring(BEARER_PREFIX.length()));

        // 3. 요청에 담아두기 → 컨트롤러가 꺼내 씀
        request.setAttribute(MEMBER_ID, memberId);
        return true;
    }
}