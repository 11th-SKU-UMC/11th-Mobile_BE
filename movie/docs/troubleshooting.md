# 트러블슈팅 No.1 — 사전 중복 조회 이후에도 발생하는 DB 충돌

**이슈**

동시에 같은 이메일 또는 닉네임으로 가입시키는 테스트에서 두 요청이 사전 중복 검사를 통과하고, 한 요청의 `saveAndFlush`에서 `DataIntegrityViolationException`이 발생했습니다.

**문제**

`existsByEmail`/`existsByNickname` 호출과 INSERT는 하나의 원자적인 동작이 아닙니다. 두 트랜잭션이 같은 시점의 '없음'을 읽을 수 있으므로 서비스의 사전 조회만으로 중복을 방지할 수 없습니다.

**해결**

DB에 이름 붙은 UNIQUE 제약을 유지하고 전역 핸들러에서 Hibernate의 제약 이름을 확인했습니다. 이메일과 닉네임 충돌을 각각 다른 409 code/message로 변환했습니다. 동시 요청 테스트로 두 요청 모두 사전 조회를 통과했더라도 최종 저장은 한 건이고, 실패 응답도 공통 JSON임을 검증했습니다.

**참고**

- [Spring의 DataIntegrityViolationException](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/dao/DataIntegrityViolationException.html)
- [MySQL CREATE TABLE 제약 조건](https://dev.mysql.com/doc/refman/8.4/en/create-table.html)
