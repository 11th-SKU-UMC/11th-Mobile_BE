package com.umc.movie.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "MEMBER4091", "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "MEMBER4092", "이미 사용 중인 닉네임입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER4041", "존재하지 않는 유저입니다."),
    DUPLICATE_RATING(HttpStatus.CONFLICT, "RATING4091", "이미 이 영화에 평점을 남기셨습니다."),
    RATING_NOT_FOUND(HttpStatus.NOT_FOUND, "RATING4041", "존재하지 않는 평점입니다."),
    RATING_FORBIDDEN(HttpStatus.FORBIDDEN, "RATING4031", "본인이 작성한 평점만 수정하거나 삭제할 수 있습니다."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "COMMON4091", "데이터 저장 제약 조건을 위반했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
