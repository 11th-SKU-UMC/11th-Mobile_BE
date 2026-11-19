package com.umc.movie.util;

import com.umc.movie.exception.ApiException;
import com.umc.movie.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long clockSkew;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.access-expiration}") long accessExpiration,
                   @Value("${jwt.clock-skew:0}") long clockSkew) {
        // JJWT가 HMAC 키의 최소 길이(32바이트)를 검사합니다.
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        if (accessExpiration <= 0 || clockSkew < 0) {
            throw new IllegalArgumentException("JWT 만료 시간은 양수, 허용 오차는 0 이상이어야 합니다.");
        }
        this.accessExpiration = accessExpiration;
        this.clockSkew = clockSkew;
    }

    public String createAccessToken(Long memberId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessExpiration)))
                .signWith(secretKey)
                .compact();
    }

    public Long getMemberId(String token) {
        try {
            // 디코딩만 해서는 신뢰할 수 없습니다. 서명과 만료 검증 후에만 sub를 사용합니다.
            Claims claims = Jwts.parser().verifyWith(secretKey)
                    .clockSkewSeconds(clockSkew).build().parseSignedClaims(token).getPayload();
            long memberId = Long.parseLong(claims.getSubject());
            if (memberId <= 0 || claims.getExpiration() == null) {
                throw new ApiException(ErrorCode.INVALID_TOKEN);
            }
            return memberId;
        } catch (ExpiredJwtException exception) {
            throw new ApiException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }
    }
}
