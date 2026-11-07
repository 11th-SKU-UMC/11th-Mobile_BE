# 6주차 영화 평점 서비스

Spring Boot 4.1.2 / Java 21 / Spring Data JPA / Bean Validation / bcrypt를 사용했습니다. 5주차 조회 API에 회원가입과 평점 등록·수정·삭제를 추가했습니다.

- [API 명세](docs/api-spec.md)
- [미션 기록 · 핵심 코드 · 캡처](docs/mission-record.md)
- [별도 트러블슈팅 기록](docs/troubleshooting.md)
- [실제 요청/응답 캡처](docs/evidence)

## 빠르게 실행하기

JDK 21을 준비하고 이 `movie` 디렉터리에서 실행합니다. `JAVA_HOME`이 다른 버전을 가리키면 JDK 21 설치 경로로 변경합니다.

```powershell
.\gradlew.bat test
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

`local` 프로필은 H2 메모리 DB를 사용합니다. MySQL 설치 없이 `http://localhost:8080`에서 API를 확인할 수 있고, 서버 종료 시 실습 데이터가 사라집니다.

다른 터미널에서 아래 명령을 실행하면 요청/응답 검증 화면을 사용할 수 있습니다. Node.js가 필요합니다.

```powershell
node tools/serve-console.mjs
```

브라우저에서 `http://localhost:5173`을 열고 **전체 시나리오 실행**을 누릅니다. 매번 새로운 실습용 회원으로 HTTP 요청 16개를 보냅니다. 마지막 두 항목은 통합 테스트가 생성한 DB 동시성 검증 기록을 표시합니다. 테스트를 먼저 실행해야 두 기록도 표시됩니다.

이 검증 화면은 개발 도구이며 Spring 서버와 별도로 실행합니다. 요청과 응답을 하드코딩하지 않고 `fetch`로 실제 API를 호출합니다.

## MySQL로 실행하기

기본 프로필은 MySQL의 기존 테이블을 검증합니다. 비밀번호는 코드에 작성하지 않고 환경변수로 전달합니다.

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/umc_movie'
$env:DB_USERNAME = '본인의_DB_계정'
$env:DB_PASSWORD = '본인의_DB_비밀번호'
.\gradlew.bat bootRun
```

신규 실습용 DB에는 [mysql-schema.sql](docs/mysql-schema.sql)을 적용합니다. 이 파일을 기존 테이블에 중복 실행하지 않습니다. 기존 5주차 DB를 쓰려면 `SHOW CREATE TABLE member`, `SHOW CREATE TABLE rating`으로 컬럼과 제약을 비교합니다. Challenge의 세부 오류 코드 구분은 UNIQUE 인덱스 이름이 `uk_member_email`, `uk_member_nickname`, `uk_rating_member_movie`일 때 동작합니다. 기존 인덱스 이름이 다르면 실제 이름을 확인한 뒤 MySQL의 `ALTER TABLE … RENAME INDEX … TO …`로 이름을 맞춥니다. 이름을 확인할 수 없는 DB 제약 오류도 공통 형식의 `COMMON4091`로 처리합니다.

실제 자동 검증과 캡처는 H2(MySQL 호환 모드)에서 수행했습니다. 기존 MySQL DB에 대한 실행이나 스키마 변경은 수행하지 않았습니다.

## 검증

```powershell
.\gradlew.bat test bootJar
```

32개 테스트가 HTTP 상태·JSON 구조·bcrypt 저장·DB 저장 결과·소유권·입력값·동시성 제약을 검증합니다. DB 테스트는 `test` 프로필의 별도 H2 메모리 DB를 사용합니다.

- HTML 테스트 보고서: `build/reports/tests/test/index.html`
- 동시 가입 요청 원본 기록: `build/mission-evidence/db-unique-email.txt`, `db-unique-nickname.txt`
- 제출용으로 보존한 동시성 기록: [이메일](docs/evidence/db-unique-email.txt), [닉네임](docs/evidence/db-unique-nickname.txt)

## 주요 코드

| 역할 | 코드 |
| --- | --- |
| 회원가입 DTO 검증 | [SignUpRequest](src/main/java/com/umc/movie/dto/SignUpRequest.java) |
| bcrypt 설정 | [PasswordConfig](src/main/java/com/umc/movie/config/PasswordConfig.java) |
| 회원가입 저장 | [MemberService](src/main/java/com/umc/movie/service/MemberService.java) |
| 평점 등록·수정·삭제 및 소유권 | [RatingService](src/main/java/com/umc/movie/service/RatingService.java) |
| 공통 응답 | [ApiResponse](src/main/java/com/umc/movie/dto/ApiResponse.java) |
| 전역 예외 처리 | [GlobalExceptionHandler](src/main/java/com/umc/movie/controller/GlobalExceptionHandler.java) |
| 오류 코드 | [ErrorCode](src/main/java/com/umc/movie/exception/ErrorCode.java) |
| API 통합 테스트 | [MissionApiIntegrationTests](src/test/java/com/umc/movie/MissionApiIntegrationTests.java) |
| DB 제약·동시성 테스트 | [UniqueConstraintIntegrationTests](src/test/java/com/umc/movie/UniqueConstraintIntegrationTests.java) |

로그인 구현 전인 6주차의 약속대로 `memberId`를 URL에서 받습니다. 따라서 소유자 비교는 경로의 ID를 기준으로 하며, 실제 사용자 인증은 7주차의 토큰 연동 범위입니다.
