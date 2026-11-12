# 7주차 API 명세

로그인 후 받은 `result.accessToken`을 `Authorization: Bearer {token}` 헤더로 전달합니다. 요청자의 ID는 검증한 JWT의 `sub`에서만 얻습니다. 요청 바디나 쿼리의 ID로 인증 사용자를 바꿀 수 없습니다.

## 신규·변경 API

| Method | URL | 인증 | 요청 바디 | 성공 result / 상태 |
| --- | --- | --- | --- | --- |
| POST | `/auth/login` | 공개 | `email`, `password` | `nickname`, `accessToken` / 200 |
| GET | `/members/me` | Bearer | 없음 | `email`, `nickname`, `createdAt` / 200 |
| PATCH | `/members/me` | Bearer | `nickname` | `email`, `nickname`, `createdAt` / 200 |
| POST | `/ratings` | Bearer | `movieId`, `score`, 선택 `comment` | `RatingResponse` / 201 |
| PATCH | `/ratings/{ratingId}` | Bearer | `score`, 선택 `comment` | `RatingResponse` / 200 |
| DELETE | `/ratings/{ratingId}` | Bearer | 없음 | `null` / 200 |

`RatingResponse`: `ratingId`, `movieId`, `score`, `comment`, `createdAt`, `updatedAt`. 기존 `/members/{memberId}/ratings` 쓰기 API는 제거했습니다. `memberId`는 바디에 추가해도 입력 오류(400)입니다. 닉네임은 공백 불가, 2~12자이며 자신을 제외하고 중복을 검사합니다. 평점 점수는 정수 1~5, comment는 최대 500자입니다.

## 유지하는 공개 API

- `POST /members`: 기존 회원가입. 201 및 공통 응답.
- `GET /members/email/{email}`, `GET /members/nickname/{nickname}`: 기존 중복 확인, `{ "available": boolean }`.
- `GET /members/{memberId}/ratings`: 기존 공개 목록 조회, `RatingResponse[]`.

공개 예외는 메서드와 경로를 함께 확인합니다. CORS OPTIONS와 내부 오류 처리 경로 `/error`는 인증 검사에서 제외합니다. 공개 조회 응답은 이전 주차의 계약을 유지합니다.

## 공통 응답과 오류

```json
{
  "isSuccess": false,
  "code": "AUTH401_2",
  "message": "로그인이 필요한 요청입니다.",
  "result": null
}
```

성공 code는 200에서 `COMMON200`, 201에서 `COMMON201`입니다. 삭제도 200 JSON이며 204를 사용하지 않습니다.

| HTTP | code | 상황 |
| --- | --- | --- |
| 401 | AUTH401_1 | 없는 이메일 또는 틀린 비밀번호. 동일한 메시지와 응답 |
| 401 | AUTH401_2 | Authorization 헤더 없음 또는 Bearer 방식이 아님 |
| 401 | AUTH401_3 | 서명 불일치, 형식 오류, 잘못된 sub, exp 누락 |
| 401 | AUTH401_4 | 만료된 토큰 |
| 404 | MEMBER404_1 | 토큰의 회원이나 공개 조회 대상 회원이 없음 |
| 409 | MEMBER409_1 | 다른 회원이 이미 사용하는 닉네임 |
| 404 | RATING404_1 | 평점이 없음 |
| 403 | RATING403_1 | 평점 작성자와 인증 사용자가 다름 |
| 409 | RATING409_1 | 같은 회원·영화의 평점 중복 |

회원가입 이메일 중복의 기존 `MEMBER4091`, 입력 검증의 `COMMON4001`, 기타 DB 제약의 `COMMON4091`은 유지합니다. JSON 형식·타입 오류 등은 기존 `ResponseEntityExceptionHandler`가 정확한 HTTP 상태와 공통 바디로 처리합니다. 기존 `ApiException(ErrorCode)`와 전역 처리기를 확장하여 오류 정의를 한곳에서 관리합니다.

## Postman 실행

`postman/week07.postman_collection.json`을 Import합니다. `baseUrl`은 기본 `http://localhost:8080`입니다. `필수 9단계` 폴더부터 Collection Runner로 실행하면 매번 새 A/B 계정, 토큰, ratingId가 자동 저장됩니다. 이어서 `추가 체크리스트`를 실행합니다. 요청을 개별 실행할 때도 가입→로그인→등록 순서를 지켜야 합니다.

선택 만료 폴더는 별도 8081 서버를 `JWT_ACCESS_EXPIRATION=60`, `JWT_CLOCK_SKEW=0`으로 실행한 후 실행합니다. 마지막 요청의 pre-request script는 62초 기다리며 같은 토큰을 재사용합니다. Newman 실행 시에는 `--timeout-script 70000`을 지정합니다.

이 컬렉션은 테스트용 회원·평점을 실제로 생성·수정·삭제하므로 local/H2 환경에서 실행합니다.
