package com.umc.movie.interceptor;

import com.umc.movie.exception.ApiException;
import com.umc.movie.exception.ErrorCode;
import com.umc.movie.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
        // CORS 사전 요청에는 토큰이 없으므로 먼저 통과시킵니다.
        if ("OPTIONS".equals(request.getMethod()) || isPublicRequest(request)) return true;

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new ApiException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        Long memberId = jwtUtil.getMemberId(header.substring(BEARER_PREFIX.length()));
        request.setAttribute(MEMBER_ID, memberId);
        return true;
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        // 경로뿐 아니라 메서드도 확인하여 회원가입 경로 전체가 공개되는 것을 막습니다.
        if ("POST".equals(method)) return path.equals("/members") || path.equals("/auth/login");
        return "GET".equals(method) && (path.matches("/members/(email|nickname)/[^/]+")
                || path.matches("/members/[^/]+/ratings"));
    }
}
