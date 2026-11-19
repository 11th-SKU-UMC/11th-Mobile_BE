# 8주차 백엔드 미션 기록

## 미션 범위

Spring Boot로 필수 미션을 진행했다. 선택 미션은 Swagger UI에서 5개 API의 요청과 성공·실패 응답 예시, Bearer 인증 요구사항을 문서화하는 과제다.

| 기능 | 메서드와 경로 | 성공 |
| --- | --- | --- |
| 즐겨찾기 등록 | `POST /favorites` | `201 COMMON201`, 등록한 즐겨찾기 |
| 내 즐겨찾기 목록 | `GET /favorites` | `200 COMMON200`, 최신 등록 순서 또는 `[]` |
| 특정 영화 즐겨찾기 여부 | `GET /favorites/{movieId}` | `200 COMMON200`, `isFavorite` |
| 즐겨찾기 해제 | `DELETE /favorites/{movieId}` | `200 COMMON200`, `result: null` |
| 영화 기록 저장 | `POST /movie-records` | `201 COMMON201`, `rating` 및 `favorite` |

다섯 API가 `Authorization: Bearer {token}`을 사용한다. 기존 `AuthInterceptor`와 `@LoginMember`가 요청자의 회원 ID를 전달한다. 즐겨찾기 등록 바디에는 `movieId`를, 영화 기록 바디에는 `movieId`, `score`, 선택적인 `comment`, `addToFavorites`를 보낸다.

## 구현 과정

1. [Favorite 엔티티](../src/main/java/com/umc/movie/entity/Favorite.java)에 회원과 TMDB 영화 ID를 저장하고 생성·수정 시각은 `BaseTimeEntity`를 상속했다. [Repository](../src/main/java/com/umc/movie/repository/FavoriteRepository.java) 조회 조건에 회원 ID를 넣어 같은 영화에 대한 회원별 등록을 허용하고 목록과 해제 대상을 제한했다.
2. [FavoriteController](../src/main/java/com/umc/movie/controller/FavoriteController.java)와 [FavoriteService](../src/main/java/com/umc/movie/service/FavoriteService.java)를 연결했다. 즐겨찾기 여부는 등록 전 `false`, 등록 후 `true`다. 등록 중복은 `409 FAVORITE409_1`, 해제 대상이 없으면 `404 FAVORITE404_1`이다.
3. [MovieRecordController](../src/main/java/com/umc/movie/controller/MovieRecordController.java)에서 [MovieRecordService](../src/main/java/com/umc/movie/service/MovieRecordService.java)를 호출한다. 서비스의 `@Transactional`이 먼저 시작되므로 기존 평점 서비스와 즐겨찾기 서비스가 동일한 트랜잭션에 참여한다. `addToFavorites`가 `false`이면 평점만 저장한다. 평점이 이미 있으면 `409 RATING409_1`을 반환한다.
4. 로컬 H2에서 [통합 테스트](../src/test/java/com/umc/movie/MovieRecordIntegrationTests.java)와 [Postman 컬렉션](postman/week08.postman_collection.json)을 실행했다. 즐겨찾기만 미리 등록한 영화에 `addToFavorites: true`를 보내 `409`를 받고, 동일 영화에 `false`로 재시도해 `201`을 확인했다. 첫 요청에서 먼저 실행된 평점 INSERT는 롤백됐다.

평점과 즐겨찾기를 같은 트랜잭션에 넣어 즐겨찾기 저장이 실패했을 때 먼저 저장한 평점도 취소되게 했다.

## 검증과 실습 인증

- `gradlew.bat --offline test bootJar --no-daemon --console=plain`으로 테스트 61개와 실행 JAR 빌드가 성공했다.
- Newman 6.2.1로 실제 HTTP 요청 19개 및 응답 검증 67개를 통과했다. [HTTP 검증 기록](evidence/week08/http-transcript.txt)
- 로컬 H2에 임시 회원 두 명을 만들어 토큰 필수, 회원별 접근 범위, 오류 코드, 롤백을 검증했다. Postman 데스크톱 캡처는 별도의 임시 계정으로 실행했으며, 앱의 기존 요청 기록 영역을 잘라냈다.
- MySQL은 [스키마](mysql-schema.sql)의 `favorite` 정의를 참고한다. 기존 데이터베이스에는 `SHOW CREATE TABLE favorite;`로 복합 UNIQUE와 이름을 확인하고, 테이블이 없다면 해당 `CREATE TABLE favorite` 블록을 적용한다. 실제 MySQL 실행 검증은 남아 있다.

| 실습 인증 | 캡처 |
| --- | --- |
| 즐겨찾기 등록 `201` | [01 등록](evidence/week08/01-favorite-create.png) |
| 중복 등록 `409 FAVORITE409_1` | [02 중복](evidence/week08/02-favorite-duplicate.png) |
| 내 즐겨찾기 목록 `200` | [03 목록](evidence/week08/03-favorite-list.png) |
| 즐겨찾기 여부 `200` | [04 여부](evidence/week08/04-favorite-status.png) |
| 즐겨찾기 해제 `200` | [05 해제](evidence/week08/05-favorite-delete.png) |
| 영화 기록 `201`, 평점과 즐겨찾기 동시 저장 | [06 영화 기록](evidence/week08/06-movie-record-create.png) |
| `@Transactional` 코드 발췌 | [07 트랜잭션 코드](evidence/week08/07-movie-record-transaction.png) |

## 트러블슈팅과 학습 내용

- **중복 요청** — 사전 조회를 두 요청이 동시에 통과할 수 있다. `(member_id, movie_id)` 복합 UNIQUE가 DB에서 중복 저장을 막는다. 애플리케이션은 사전 검사와 이름이 지정된 UNIQUE 위반을 모두 `FAVORITE409_1`로 처리한다.
- **부분 저장** — 두 저장이 개별 트랜잭션이라면 평점이 먼저 커밋되고 즐겨찾기만 실패할 수 있다. 영화 기록 서비스에서 트랜잭션 범위를 정하고, 즐겨찾기 단계 예외 후 평점이 남지 않는지 검증했다.
- **주차별 브랜치** — 첫 7주차 PR은 조직 브랜치와 6주차 커밋 이력을 공유하지 않아 `add/add` 충돌이 발생했다. 이번 `week08`은 현재 조직 `GAMJA`를 조상으로 갖는 [7주차 대체 PR #21](https://github.com/11th-SKU-UMC/11th-Mobile_BE/pull/21)의 커밋 `6b4857d`에서 시작했다. 7주차가 조직 `GAMJA`에 들어온 뒤 최신 기준으로 PR의 커밋 수와 머지 가능 여부를 다시 확인한다. 스쿼시 또는 리베이스 방식으로 머지됐다면 8주차 커밋 하나만 새 기준에 적용한다.

## 제출

Notion 미션 기록에 구현 과정, 코드 링크, Postman 캡처 6장과 트랜잭션 코드 이미지, 트러블슈팅을 첨부한다. 공유 링크를 복사해 [UMC](https://umc.ai.kr/)에 제출한다.
