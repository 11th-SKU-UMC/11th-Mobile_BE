# 9주차 백엔드 미션 기록

기존 영화 평점 서비스의 API 15개를 OpenAPI로 문서화했다. 공개 API 5개는 로그인 없이 사용할 수 있고, 나머지 10개는 Bearer JWT가 필요하다. 즐겨찾기와 평점의 단위·통합 테스트, 영화 기록의 롤백 실험을 포함해 전체 테스트 74개를 통과했다.

## Swagger 실습

1. 프로젝트의 Spring Boot 4.1.2에 맞춰 springdoc 3.1.0을 적용했다. Swagger UI와 OpenAPI JSON/YAML 경로를 인증 인터셉터 예외에 추가했다. [springdoc 3.1.0 릴리스](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.1.0)
2. 회원가입과 로그인 요청 DTO에 이메일, 비밀번호, 닉네임 예시를 달았다. 즐겨찾기 등록에는 영화 ID 예시와 성공·실패 응답 예시를 추가했다.
3. `@LoginMember`가 붙은 메서드에만 `bearerAuth`를 적용했다. 토큰에서 읽는 회원 ID는 요청 입력란에서 숨겼다. 기존 `ErrorCode`를 사용해 HTTP 상태와 응답 `code`를 함께 보여준다. [OpenAPI 설정](../src/main/java/com/umc/movie/config/OpenApiConfig.java)
4. 실행 JAR을 로컬 H2로 올려 Swagger UI HTML과 OpenAPI 문서가 토큰 없이 `200`으로 열리는 것을 확인했다. 임시 계정을 가입시킨 뒤 로그인으로 받은 토큰을 써서 즐겨찾기 등록 `201 COMMON201`, 동일 요청의 `409 FAVORITE409_1`을 실제 HTTP로 확인했다. [HTTP 검증 기록](evidence/week09/http-transcript.txt)

| 필수 문서화 범위 | 인증 | 대표 실패 응답 |
| --- | --- | --- |
| 평점 등록 | Bearer JWT | `401 AUTH401_2`, `409 RATING409_1` |
| 평점 수정·삭제 | Bearer JWT | `403 RATING403_1`, `404 RATING404_1` |
| 유저별 평점 조회 | 공개 | `404 MEMBER404_1` |
| 내 정보 조회·수정 | Bearer JWT | `404 MEMBER404_1`, 수정 시 `409 MEMBER409_1` |
| 내 즐겨찾기 조회·즐겨찾기 여부 조회 | Bearer JWT | `401 AUTH401_2` |
| 즐겨찾기 해제 | Bearer JWT | `404 FAVORITE404_1` |

Windows에서 브라우저 도구가 현재 URL을 확인하지 못해 Swagger 화면 캡처와 Authorize 버튼 조작은 완료하지 못했다. HTTP 검증 기록에는 생성된 문서의 인증 설정, 즐겨찾기 응답 예시, 실제 요청·응답을 넣었다.

## 테스트

| 구분 | 확인한 동작 |
| --- | --- |
| 즐겨찾기 단위 테스트 2개 | 신규 등록과 중복 등록 `FAVORITE409_1` |
| 즐겨찾기 통합 테스트 4개 | 토큰 누락 `401`, 정상 등록 `201`, 중복 등록 `409`, 미등록 영화 조회 `200` 및 `isFavorite: false` |
| 평점 단위 테스트 2개 | 중복 등록 `RATING409_1`, 다른 회원의 평점 수정 `RATING403_1` |
| 평점 통합 테스트 2개 | 토큰 누락 `401 AUTH401_2`, 토큰을 사용한 등록 `201 COMMON201` 및 DB 저장 |
| OpenAPI 통합 테스트 3개 | 문서 공개 경로, API별 Bearer 표시, 성공·실패 예시 |
| 선택 미션 | 즐겨찾기가 이미 있는 영화 기록 저장 시 `409 FAVORITE409_1`을 반환하고 평점 개수가 유지되는지 확인 |

코드는 [즐겨찾기 단위 테스트](../src/test/java/com/umc/movie/FavoriteServiceTests.java), [평점 단위 테스트](../src/test/java/com/umc/movie/RatingServiceTests.java), [즐겨찾기·영화 기록 통합 테스트](../src/test/java/com/umc/movie/MovieRecordIntegrationTests.java), [평점 통합 테스트](../src/test/java/com/umc/movie/AuthIntegrationTests.java)에 있다.

### 회귀 실험

- **즐겨찾기 중복 검사 제거** — 작업 브랜치의 소스를 복제해 서비스의 중복 확인을 빼고 두 테스트를 실행했다. 단위 테스트는 예상한 `ApiException`이 발생하지 않아 실패했고, 통합 테스트는 중복 요청의 `saveAndFlush` 호출을 감지해 실패했다. DB의 UNIQUE 제약과 예외 변환이 `409 FAVORITE409_1`을 유지하므로, 통합 테스트에는 실제 저장소를 감시해 불필요한 INSERT 시도까지 확인하는 검증을 추가했다. [실패 로그](evidence/week09/duplicate-removed.txt) · [실행 결과 이미지](evidence/week09/02-duplicate-removed.png)
- **영화 기록 트랜잭션 제거** — 복제본에서 `MovieRecordService.create`의 `@Transactional`을 뺐다. 즐겨찾기 등록 실패 전에 실행된 평점 INSERT가 커밋되어, 평점 개수가 예상한 0건 대신 1건으로 남았다. [실패 로그](evidence/week09/transaction-removed.txt) · [실행 결과 이미지](evidence/week09/03-transaction-removed.png)
- **전체 재검증** — 원본 소스로 전체 테스트와 실행 JAR 빌드를 실행했다. 74개 모두 통과했다. [전체 결과](evidence/week09/test-results.txt) · [실행 결과 이미지](evidence/week09/01-test-results.png)

## 학습 정리

- OpenAPI는 요청, 응답, 인증 요구사항을 기술하는 규격이다. Swagger UI는 생성된 OpenAPI 문서를 읽어 API 목록을 보여주고 요청을 실행한다. [OpenAPI 명세](https://spec.openapis.org/oas/v3.1.html) · [springdoc 문서](https://springdoc.org/)
- 단위 테스트는 저장소를 Mockito로 대체하고 서비스의 분기와 호출을 확인한다. 이번 실습에서 stub은 `given(...).willReturn(...)`으로 저장소의 응답을 정하고, mock의 `verify(...)`로 저장 호출 여부를 검증했다.
- 통합 테스트는 MockMvc로 실제 Spring MVC 설정에 요청을 전달하고 JWT 인터셉터, DTO 검증, 서비스, H2 저장 결과를 함께 확인한다. [Spring MockMvc 문서](https://docs.spring.io/spring/reference/testing/mockmvc.html)
- Given–When–Then으로 데이터와 토큰을 준비하고, 요청 또는 서비스 호출을 수행한 뒤, HTTP 상태와 응답 코드 및 데이터 변화를 검증했다. 회귀 테스트가 실제로 잡는 조건은 코드를 잠시 바꿔 확인할 수 있었다.

## 로컬 실행

`movie` 디렉터리에서 아래 명령을 실행한다. 테스트는 메모리 H2를 사용한다.

```powershell
.\gradlew.bat test bootJar
$env:JWT_SECRET = [guid]::NewGuid().ToString('N')
java -jar .\build\libs\movie-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.address=127.0.0.1
```

`http://localhost:8080/swagger-ui.html`에서 가입과 로그인을 실행한 뒤, `result.accessToken` 값만 Authorize에 넣으면 된다. `POST /favorites`의 예시 요청을 두 번 실행하면 처음에는 `201`, 두 번째에는 `409`를 확인할 수 있다.
