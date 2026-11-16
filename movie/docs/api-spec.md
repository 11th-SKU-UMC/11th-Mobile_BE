# API 명세

기본 주소: `http://localhost:8080`

JSON 요청에는 `Content-Type: application/json`을 사용합니다. 6주차 쓰기 API와 오류 응답은 `isSuccess`, `code`, `message`, `result` 구조를 사용합니다. DELETE 성공은 HTTP 규칙과 워크북에 맞춰 **204, 본문 없음**입니다. 5주차 GET 응답은 기존 화면과의 호환성을 위해 원래 구조를 유지합니다.

## 회원가입

`POST /members` → `201 Created`

```json
{
  "email": "umc@example.com",
  "password": "Umc!1234",
  "nickname": "moviefan"
}
```

| 필드 | 검증 |
| --- | --- |
| email | 필수, 공백 불가, 이메일 형식, 최대 254자 |
| nickname | 필수, 공백 불가, 2~12자 |
| password | 필수, 영문 대·소문자/숫자/`!@#$%^&*` 중 특수문자를 각각 포함, 8자 이상, UTF-8 최대 72바이트 |

```json
{
  "isSuccess": true,
  "code": "COMMON201",
  "message": "회원가입에 성공했습니다.",
  "result": {
    "memberId": 1,
    "email": "umc@example.com",
    "nickname": "moviefan"
  }
}
```

응답에는 비밀번호와 해시를 포함하지 않습니다. 저장은 bcrypt cost 10으로 수행합니다.

## 평점 등록

`POST /members/{memberId}/ratings` → `201 Created`

```json
{
  "movieId": 550,
  "score": 5,
  "comment": "다시 보고 싶은 영화입니다."
}
```

- `movieId`: 필수, 양의 정수(Long), TMDB 영화 ID. 별도 movie 테이블 FK 없음.
- `score`: 필수, 1~5 정수. 소수 입력은 400.
- `comment`: 생략/null 가능, 최대 500자.
- 회원이 없으면 404. 동일한 회원·영화 조합의 평점이 이미 있으면 409.

```json
{
  "isSuccess": true,
  "code": "COMMON201",
  "message": "평점 등록에 성공했습니다.",
  "result": 1
}
```

`result`는 생성된 `ratingId`입니다.

## 평점 수정

`PATCH /members/{memberId}/ratings/{ratingId}` → `200 OK`

```json
{
  "score": 4,
  "comment": "수정한 후기입니다."
}
```

`score`는 필수이며 1~5 정수입니다. `comment`는 최대 500자이고 생략/null이면 기존 후기를 지웁니다. `movieId`나 `memberId` 변경은 지원하지 않으며 DTO에 없는 JSON 필드는 400으로 거절합니다.

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "평점 수정에 성공했습니다.",
  "result": null
}
```

## 평점 삭제

`DELETE /members/{memberId}/ratings/{ratingId}` → `204 No Content`

요청 본문과 성공 응답 본문은 없습니다. 수정·삭제는 평점을 먼저 조회하고, 없으면 404, 경로 `memberId`와 작성자가 다르면 403을 반환합니다.

## 오류 응답

```json
{
  "isSuccess": false,
  "code": "MEMBER4091",
  "message": "이미 사용 중인 이메일입니다.",
  "result": null
}
```

| HTTP | code | 의미 |
| --- | --- | --- |
| 400 | COMMON4001 | DTO 검증 실패. message에 실패 사유 한 개 전달 |
| 400 | COMMON400 | 잘못된 JSON, 소수 정수 변환, 알 수 없는 필드, 경로 타입 오류 |
| 403 | RATING4031 | 다른 회원의 평점 수정/삭제 |
| 404 | MEMBER4041 | 존재하지 않는 회원의 평점 등록/조회 |
| 404 | RATING4041 | 수정/삭제할 평점 없음 |
| 409 | MEMBER4091 | 이메일 중복 (사전 조회 및 이름 붙은 DB UNIQUE) |
| 409 | MEMBER4092 | 닉네임 중복 (사전 조회 및 이름 붙은 DB UNIQUE) |
| 409 | RATING4091 | 회원·영화 평점 중복 (사전 조회 및 이름 붙은 DB UNIQUE) |
| 409 | COMMON4091 | 분류되지 않은 DB 저장 제약 위반 |

## 기존 조회 API

| Method | 경로 | 응답 |
| --- | --- | --- |
| GET | /members/nickname/{nickname} | `{"available":true}` 또는 `false` |
| GET | /members/email/{email} | `{"available":true}` 또는 `false` |
| GET | /members/{memberId}/ratings | 평점 배열, ratingId 내림차순. 없으면 `[]` |

평점 배열의 항목은 `ratingId`, `movieId`, `score`, `comment`, `createdAt`, `updatedAt`입니다.
