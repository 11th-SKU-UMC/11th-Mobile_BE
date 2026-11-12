# 7주차 영화 평점 서비스 — 로그인·JWT·소유권

Spring Boot 4.1.2 / Java 21 / Spring Data JPA / Bean Validation / bcrypt / JJWT 0.12.6을 사용합니다. 6주차 구현 위에 로그인, 내 정보 조회·수정, 공통 JWT 검증, 토큰 기반 평점 CRUD를 추가했습니다. 선택 미션의 실제 만료 검증과 `@LoginMember`도 구현했습니다.

- [7주차 API 명세와 Postman 실행](docs/week07-api-spec.md)
- [7주차 미션 기록 · 구현 과정 · 남은 캡처](docs/week07-mission-record.md)
- [7주차 트러블슈팅](docs/week07-troubleshooting.md)
- [7주차 실제 HTTP 실행 증거](docs/evidence/week07)
- [Postman 컬렉션](docs/postman/week07.postman_collection.json)
- 이전 기록: [6주차 미션](docs/mission-record.md), [6주차 트러블슈팅](docs/troubleshooting.md)

**검증 완료:** JUnit 56개, 실제 HTTP 23개 요청/75개 검증 모두 통과. **제출 전 남은 작업:** Postman 응답 화면 캡처 4장. Postman 미설치 및 Computer Use의 URL 확인 실패로 캡처는 수행하지 못했습니다. 실제 실행 JSON과 텍스트를 보존하되 공개 기록의 JWT는 제거했습니다.

## 빠르게 실행하기

JDK 21을 준비하고 이 `movie` 디렉터리에서 실행합니다. `JAVA_HOME`이 다른 버전을 가리키면 JDK 21 설치 경로로 변경합니다.

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
$env:JWT_ACCESS_EXPIRATION = '3600'
$env:JWT_CLOCK_SKEW = '0'
.\gradlew.bat test
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

`local` 프로필은 H2 메모리 DB를 사용합니다. MySQL 설치 없이 `http://localhost:8080`에서 API를 확인할 수 있고, 서버 종료 시 실습 데이터가 사라집니다.

로그인은 `POST /auth/login`에 이메일·비밀번호를 보내고, 이후에는 `Authorization: Bearer {result.accessToken}` 헤더를 전달합니다. 평점 등록·수정·삭제에서 클라이언트의 memberId는 받지 않습니다.

다른 터미널에서 아래 명령을 실행하면 저장된 Newman 요청·응답 기록을 볼 수 있습니다. Node.js가 필요합니다.

```powershell
node tools/serve-console.mjs
```

브라우저에서 `http://localhost:5173`을 열고 결과를 선택합니다. 로그인 성공, 토큰 없는 401, 다른 작성자의 403, 평점 등록 201, 만료 전후 비교를 확인할 수 있습니다.

이 화면은 `docs/evidence/week07/newman-report.json`의 실제 실행 기록을 읽는 뷰어이며 API를 새로 호출하지 않습니다. 재검증은 Postman Collection Runner 또는 아래 Newman 명령으로 수행합니다. 이전 `tools/api-console.html`은 6주차 전용 도구로 보존되어 있으며 7주차 API에는 사용하지 않습니다.

## Postman / Newman 재검증

Node.js와 npm이 설치된 환경에서 기본 서버(8080)와 별도 만료 서버(8081)를 실행합니다. 만료 서버는 새 터미널에서 새 무작위 JWT_SECRET, `JWT_ACCESS_EXPIRATION=60`, `JWT_CLOCK_SKEW=0`을 설정한 뒤 다음과 같이 실행합니다.

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local --server.port=8081'
```

두 서버가 시작된 후 실행합니다. 마지막 요청은 만료 확인을 위해 실제로 62초 대기합니다.

```powershell
npx --yes newman@6.2.1 run docs/postman/week07.postman_collection.json --timeout-script 70000 --bail --reporters cli,json --reporter-json-export build/newman-week07.json
node tools/export-week07-evidence.mjs
```

만료 서버 없이 필수 검증만 수행할 때는 `--folder '필수 9단계' --folder '추가 체크리스트'`를 Newman 명령에 추가합니다. 이 경우 전체 23개 실행을 요구하는 evidence export 스크립트는 실행하지 않습니다. Postman 앱에서는 JSON을 Import 후 같은 순서로 폴더를 실행합니다.

컬렉션 수정 시 `node tools/generate-postman.mjs`로 JSON을 다시 생성할 수 있습니다. `.env.example`은 변수 목록 예시이며 Spring은 `.env`를 자동으로 읽지 않습니다. 실제 JWT_SECRET은 터미널/IDE 환경변수로 설정하고 커밋하지 않습니다.

## MySQL로 실행하기

기본 프로필은 MySQL의 기존 테이블을 검증합니다. 비밀번호는 코드에 작성하지 않고 환경변수로 전달합니다.

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
$env:DB_URL = 'jdbc:mysql://localhost:3306/umc_movie'
$env:DB_USERNAME = '본인의_DB_계정'
$env:DB_PASSWORD = '본인의_DB_비밀번호'
.\gradlew.bat bootRun
```

신규 실습용 DB에는 [mysql-schema.sql](docs/mysql-schema.sql)을 적용합니다. 이 파일을 기존 테이블에 중복 실행하지 않습니다. 기존 5주차 DB를 쓰려면 `SHOW CREATE TABLE member`, `SHOW CREATE TABLE rating`으로 컬럼과 제약을 비교합니다. 세부 오류 코드 구분은 UNIQUE 인덱스 이름이 `uk_member_email`, `uk_member_nickname`, `uk_rating_member_movie`일 때 동작합니다. 기존 인덱스 이름이 다르면 실제 이름을 확인한 뒤 MySQL의 `ALTER TABLE … RENAME INDEX … TO …`로 이름을 맞춥니다. 이름을 확인할 수 없는 DB 제약 오류도 공통 형식의 `COMMON4091`로 처리합니다.

실제 자동 검증과 캡처는 H2(MySQL 호환 모드)에서 수행했습니다. 기존 MySQL DB에 대한 실행이나 스키마 변경은 수행하지 않았습니다.

## 검증

```powershell
.\gradlew.bat test bootJar
```

56개 테스트가 로그인·JWT 서명/만료·본인 정보·닉네임 중복·HTTP 상태·JSON 구조·bcrypt 저장·DB 저장 결과·소유권·입력값·동시성 제약을 검증합니다. DB 테스트는 `test` 프로필의 별도 H2 메모리 DB와 테스트 전용 JWT 키를 사용하므로 실행 환경의 JWT_SECRET이 필요 없습니다.

- HTML 테스트 보고서: `build/reports/tests/test/index.html`
- 동시 가입 요청 원본 기록: `build/mission-evidence/db-unique-email.txt`, `db-unique-nickname.txt`
- 제출용으로 보존한 동시성 기록: [이메일](docs/evidence/db-unique-email.txt), [닉네임](docs/evidence/db-unique-nickname.txt)

## 주요 코드

| 역할 | 코드 |
| --- | --- |
| 로그인과 토큰 발급 | [AuthService](src/main/java/com/umc/movie/service/AuthService.java) |
| JWT 생성·서명·만료 검증 | [JwtUtil](src/main/java/com/umc/movie/util/JwtUtil.java) |
| 공통 인증 관문 | [AuthInterceptor](src/main/java/com/umc/movie/interceptor/AuthInterceptor.java) |
| 인증 사용자 파라미터 | [LoginMemberArgumentResolver](src/main/java/com/umc/movie/auth/LoginMemberArgumentResolver.java) |
| 회원가입 DTO 검증 | [SignUpRequest](src/main/java/com/umc/movie/dto/SignUpRequest.java) |
| bcrypt 설정 | [PasswordConfig](src/main/java/com/umc/movie/config/PasswordConfig.java) |
| 회원가입 저장 | [MemberService](src/main/java/com/umc/movie/service/MemberService.java) |
| 평점 등록·수정·삭제 및 소유권 | [RatingService](src/main/java/com/umc/movie/service/RatingService.java) |
| 공통 응답 | [ApiResponse](src/main/java/com/umc/movie/dto/ApiResponse.java) |
| 전역 예외 처리 | [GlobalExceptionHandler](src/main/java/com/umc/movie/controller/GlobalExceptionHandler.java) |
| 오류 코드 | [ErrorCode](src/main/java/com/umc/movie/exception/ErrorCode.java) |
| API 통합 테스트 | [MissionApiIntegrationTests](src/test/java/com/umc/movie/MissionApiIntegrationTests.java) |
| 인증·정보 수정 통합 테스트 | [AuthIntegrationTests](src/test/java/com/umc/movie/AuthIntegrationTests.java) |
| DB 제약·동시성 테스트 | [UniqueConstraintIntegrationTests](src/test/java/com/umc/movie/UniqueConstraintIntegrationTests.java) |

로그인 API와 공개 회원가입·조회 외에는 토큰이 필요합니다. 인증은 Interceptor, 평점 소유권 인가는 Service에서 수행합니다. Access Token만 구현했으며 Refresh Token·로그아웃·토큰 폐기 목록은 이번 워크북 범위에 포함하지 않습니다.
