# 7주차 미션 기록 — 인증·JWT·소유권

## 작업 기반과 범위

`week07`은 6주차 구현 이전 커밋에서 생성되어, 기존 6주차 완료 커밋 `f1babe5`의 변경을 cherry-pick한 뒤 구현했습니다. 원래 작업 폴더의 미커밋 변경은 건드리지 않고 별도 Git worktree에서 작업했습니다.

필수 실습 1~6, 필수 HTTP 9단계, 추가 체크리스트, 선택 미션 2개를 구현하고 실행했습니다. 필수 화면 캡처 4장은 아직 없습니다. 아래 실행 증거는 실제 API에 Postman 컬렉션을 Newman으로 실행한 기록이며 공개용 파일에서는 JWT 값을 제거했습니다. Postman 데스크톱 화면 캡처를 대체했다고 주장하지 않습니다.

## 구현 과정

1. JJWT 0.12.6 의존성, JWT 환경변수, `.env` Git 제외 규칙을 추가했습니다. 기존 `ErrorCode`/`ApiException`/전역 처리기를 재사용해 워크북의 오류 코드를 등록했습니다.
2. `findByEmail`과 `AuthService`를 추가했습니다. `PasswordEncoder.matches`로 bcrypt 해시를 비교하고, 실패 원인을 구분하지 않는 `AUTH401_1`을 반환합니다. 로그인 DTO에는 회원가입의 복잡도 규칙을 다시 적용하지 않습니다.
3. `JwtUtil`에서 문자열 memberId를 `sub`로 넣고 email, iat, exp와 함께 서명합니다. 입력 토큰은 서명·만료 검증을 통과한 뒤에만 읽고 만료는 `AUTH401_4`, 그 외 잘못된 토큰은 `AUTH401_3`으로 처리합니다. exp 누락과 양수가 아닌 sub도 거절합니다.
4. `AuthInterceptor`를 전체 경로에 등록하고 공개 요청만 메서드·경로 기준으로 통과시킵니다. 검증된 memberId는 서버 요청 attribute에 보관합니다. 브라우저 CORS OPTIONS를 통과시키고 기존 CORS 설정을 유지했습니다.
5. 내 정보 조회·닉네임 수정을 추가했습니다. 응답 DTO는 password를 포함하지 않으며 중복 확인에서 나 자신을 제외합니다. UNIQUE 제약 예외 처리도 기존대로 유지했습니다.
6. 평점 쓰기 경로의 memberId를 제거하고, 서비스 호출은 `(memberId, ratingId)` 순서로 통일했습니다. `findOwnedRating`에서 대상 없음→404, 다른 작성자→403 순서로 검사합니다. 등록·수정은 RatingResponse, 삭제는 200/null을 반환합니다.
7. 선택 미션으로 `@LoginMember`와 argument resolver를 적용했습니다. 컨트롤러는 문자열 `"memberId"`나 `@RequestAttribute` 없이 인증 사용자 ID를 받습니다.
8. 이전 회귀 테스트를 새 경로·상태·인증 헤더에 맞춰 변경하고, 인증/변조/만료/닉네임/클라이언트 ID 조작/CORS 테스트를 추가했습니다. Postman 컬렉션의 실제 HTTP 실행도 수행했습니다.

## 필수 9단계 실행 결과

| 단계 | 실제 결과 |
| --- | --- |
| 1. A/B 가입 | 각각 201 / COMMON201 |
| 2. A/B 로그인 | 각각 200 / COMMON200 및 서로 다른 토큰 |
| 3. A 평점 등록 | 201 / COMMON201, result.ratingId 획득, 바디에 memberId 없음 |
| 4. 같은 영화 중복 | 409 / RATING409_1 |
| 5. A 본인 수정 | 200 / COMMON200, score=4 확인 |
| 6. B 토큰으로 수정 | 403 / RATING403_1 |
| 7. 토큰 제거 | 401 / AUTH401_2 |
| 8. 없는 평점 삭제 | 404 / RATING404_1 |
| 9. A 본인 삭제 | 200 / COMMON200, result=null |

추가로 헤더 없는 GET /members/me, 정상 내 정보 조회, 기존 닉네임 유지, 남의 닉네임 중복, 닉네임 수정, 없는 이메일/틀린 비밀번호의 동일 응답, payload 변조를 확인했습니다.

- [실제 HTTP 요청·응답 전체](evidence/week07/http-transcript.txt)
- [Newman 실행 JSON · JWT 제거](evidence/week07/newman-report.json)
- [테스트·실행 결과 요약](evidence/week07/test-summary.txt)
- [Postman 컬렉션](postman/week07.postman_collection.json)

## 선택 미션 1 — 실제 만료

동일 빌드를 별도 local 서버(8081)에서 `JWT_ACCESS_EXPIRATION=60`, `JWT_CLOCK_SKEW=0`으로 실행했습니다. 로그인 직후 GET /members/me는 200, 62초 대기 후 **동일한 Authorization 헤더**로 호출한 결과는 401/AUTH401_4였습니다. 새 토큰을 발급하지 않았습니다. 두 요청의 헤더가 같다는 것도 실행 기록 생성 시 확인합니다.

워크북의 `clockSkewSeconds(60)`을 그대로 사용하면 1분 뒤 바로 만료되지 않을 수 있습니다. 이 구현에서는 허용 오차 기본값을 0으로 정했고 필요 시 `JWT_CLOCK_SKEW`로 조정할 수 있습니다.

만료 전후 응답은 HTTP 기록의 `선택 03`, `선택 04`에 있습니다. 공개 기록에서는 토큰을 제거했지만 상태·응답 code·시각·검증 결과는 보존합니다. 동일 토큰 여부는 로컬 원본에서 evidence export 시 검증했습니다. 나란히 표시하는 기록 뷰어도 제공하지만 화면 캡처는 미완료입니다.

## 선택 미션 2 — @LoginMember

적용 전(워크북 기본 구현):

```java
public ResponseEntity<ApiResponse<MemberInfoResponse>> getMyInfo(
        @RequestAttribute(AuthInterceptor.MEMBER_ID) Long memberId) {
    // ...
}
```

적용 후(실제 구현):

```java
public ResponseEntity<ApiResponse<MemberInfoResponse>> getMyInfo(@LoginMember Long memberId) {
    return ResponseEntity.ok(ApiResponse.ok("회원 정보를 조회했습니다.", memberService.getMyInfo(memberId)));
}
```

resolver는 `@LoginMember Long`만 지원하고 request scope의 인증 결과를 꺼냅니다. 해당 값이 없으면 401입니다. 새로운 인증을 수행하거나 클라이언트의 쿼리 memberId를 읽지 않습니다. 내 정보 조회·수정과 평점 등록·수정·삭제에 모두 적용했고 통합 테스트 및 실제 HTTP에서 정상 동작했습니다. 적용 전 코드는 비교를 위한 워크북 방식의 예시입니다.

## 남은 제출용 캡처

Postman 데스크톱 앱은 설치되어 있지 않습니다. 브라우저를 통한 캡처도 Computer Use가 현재 URL을 확실히 확인할 수 없어 도구 차원에서 중단되었습니다. 다른 방식으로 화면을 조작하거나 캡처를 꾸며 만들지 않았습니다.

Postman에서 컬렉션을 실행한 뒤 아래 4장을 남겨야 합니다. 선택 미션 제출 시 만료 전후 나란히 캡처와 @LoginMember 정상 응답 캡처도 추가합니다.

- [ ] `02A 로그인 A`: accessToken이 보이는 200 응답
- [ ] `추가 01 토큰 없이 내 정보`: 401 / AUTH401_2 응답
- [ ] `06 B가 A 평점 수정`: 403 / RATING403_1 응답
- [ ] `03 A 평점 등록`: memberId 없는 요청 바디와 201 응답

Notion 기록·공유 및 umc.ai.kr 제출은 수행하지 않았습니다. 이 문서와 실제 실행 기록을 미션 페이지에 옮겨 사용할 수 있습니다.

## 이해할 핵심

- 인증은 로그인과 이후 토큰 검증, 인가는 서비스의 평점 소유권 확인입니다.
- bcrypt salt는 저장된 해시에 포함되어 있으므로 입력 비밀번호를 새 salt로 해시해 문자열 비교하지 않습니다.
- JWT payload는 읽을 수 있습니다. 서명 검증 전의 값을 신뢰하지 않으며 비밀번호나 비밀키를 넣지 않습니다.
- 서버가 인증한 memberId를 사용해야 합니다. URL에서 memberId를 없애도 서비스에서 소유권 비교를 빼면 남의 ratingId를 조작할 수 있습니다.
- 토큰 없음/위조/만료는 모두 401이지만 code로 구분하고, 정상 토큰으로 남의 데이터를 건드릴 때는 403입니다.
- DB UNIQUE 제약은 동시 요청에도 필요합니다. 사전 중복 조회만으로 제약을 대체하지 않습니다.

검증 DB는 H2 MySQL 호환 모드입니다. 실제 MySQL DB의 실행·변경은 수행하지 않았습니다. 임시 H2 계정용 서명 키는 매 실행 무작위 생성 후 저장하지 않으며 검증 서버는 종료했습니다. 원본 Newman 보고서는 Git에서 제외된 `build/newman-week07.json`에만 남기고, 공개 보고서의 Authorization 헤더·응답 바이트 배열·변수에 들어 있는 JWT는 모두 제거했습니다.
