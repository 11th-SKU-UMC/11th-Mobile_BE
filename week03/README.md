# UMC 3주차 - 첫 API 만들고 검증하기 (Spring Boot)

Raw SQL(JdbcTemplate) + No DTO 방식으로 만든 도서 대여 API.

## 실행
1. 2주차에 만든 DB가 있으면 그대로 사용. 없으면 `sql/01_schema.sql` → `sql/02_seed.sql` 순서로 실행
2. IntelliJ 실행 구성 → 환경 변수에 `DB_URL`, `DB_USER`, `DB_PW` 등록
   - 예: `DB_URL=jdbc:mysql://localhost:3306/study`
3. `StudyApplication` 실행 → `Started StudyApplication` 로그 확인

## API

| Method | URL | Body | 성공 응답 |
|---|---|---|---|
| GET | `/books` | - | 200, 도서 JSON 배열 |
| POST | `/books` | `{"categoryId":1,"title":"클린 코드","description":"애자일 소프트웨어 장인 정신"}` | 200, "도서 등록이 완료되었습니다!" |
| GET | `/books/category/{categoryId}` | - | 200, 해당 카테고리 도서 배열 |
| POST | `/rentals` | `{"userId":2,"bookId":2}` | 201, "대여 기록이 생성되었습니다!" |
| PATCH | `/rentals/{rentalId}/return` | - | 200 / 이미 반납·없는 ID면 404 |

## 구조
```
controller/  BookController, RentalController   (요청 받고 응답)
service/     BookService, RentalService         (비즈니스 로직 자리)
repository/  BookRepository, RentalRepository   (SQL 실행 전담)
```
