// src/main/java/com/umc/movie/config/WebConfig.java
package com.umc.movie.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.umc.movie.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해
                .allowedOrigins("http://localhost:5173") // 이 출처에서 오는 요청만 허용
                .allowedMethods("GET", "POST", "PATCH", "DELETE")
                .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")              // 일단 전부 잠그고
                .excludePathPatterns(                // 열 곳만 예외
                        "/auth/**",                  // 로그인
                        "/members",                  // 회원가입
                        "/members/nickname/**",      // 닉네임 중복 체크
                        "/members/email/**",         // 이메일 중복 체크
                        "/members/*/ratings",        // 유저별 평점 조회
                        "/error"
                );
    }
}

