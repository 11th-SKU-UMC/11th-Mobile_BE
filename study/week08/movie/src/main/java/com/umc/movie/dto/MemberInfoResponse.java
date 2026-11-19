package com.umc.movie.dto;

import com.umc.movie.entity.Member;
import java.time.LocalDateTime;

public record MemberInfoResponse(
        String email,
        String nickname,
        LocalDateTime createdAt
) {
    // 엔티티를 그대로 내보내면 password까지 나가서 DTO로 옮겨 담음
    public static MemberInfoResponse from(Member member) {
        return new MemberInfoResponse(member.getEmail(), member.getNickname(), member.getCreatedAt());
    }
}