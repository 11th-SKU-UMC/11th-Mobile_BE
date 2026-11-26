# 7주차 트러블슈팅

## 1. week07에 6주차 구현이 없음

- 이슈: 원격 week07은 `f8461fc`를 가리켜 영화 서버 소스가 없었습니다.
- 원인: 완료된 6주차 구현 커밋보다 이전 커밋에서 브랜치를 만들었습니다.
- 해결: 원래 작업 폴더와 별도 worktree에서 `f1babe5`의 변경을 cherry-pick하고 7주차를 구현했습니다. 기존 브랜치와 미커밋 파일은 수정하지 않았습니다.

## 2. 샌드박스 안에서 Gradle 및 로컬 HTTP 서버 실행 실패

- 이슈: Gradle 캐시 잠금 파일 AccessDeniedException, Java 서버의 loopback connect Permission denied가 발생했습니다.
- 원인: 실행 환경의 파일·네트워크 제한으로 기본 실행에서 캐시 쓰기와 loopback 연결이 차단되었습니다.
- 해결: 도구의 승인된 실행 권한으로 Gradle과 로컬 검증 서버를 다시 실행했습니다. 애플리케이션 코드를 바꿔 해결한 문제가 아닙니다. 통합 테스트와 실제 HTTP가 이후 통과했습니다.

## 3. 만료 실습의 허용 오차

- 문제: 워크북의 60초 유효시간과 `clockSkewSeconds(60)`을 함께 쓰면 발급 1분 후에도 통과할 수 있습니다.
- 해결: `JWT_CLOCK_SKEW`를 별도 설정으로 분리하고 기본값을 0으로 정했습니다. 만료 서버에서 실제 62초 대기 후 동일 토큰으로 401/AUTH401_4를 확인했습니다.
- 참고: [JJWT 0.12.6 Clock Skew](https://github.com/jwtk/jjwt/tree/0.12.6#accounting-for-clock-skew)

## 4. Postman 캡처 미완료

- 이슈: Postman 데스크톱 앱이 설치되어 있지 않고, 브라우저 자동화 도구도 현재 URL을 검증하지 못해 중단되었습니다.
- 해결한 부분: 공식 Postman 컬렉션 실행기 Newman 6.2.1로 23개 실제 HTTP 요청과 75개 검증을 수행했습니다. 로컬 원본은 build에 보존하고, 공개 JSON·텍스트 기록에서는 JWT를 제거했습니다.
- 남은 부분: Postman의 실제 응답 화면 캡처 4장. 캡처를 생성했다고 기록하지 않았으며 컬렉션에 해당 요청을 표시했습니다.
- 참고: [Newman 공식 문서](https://learning.postman.com/docs/reference/newman-cli/command-line-integration-with-newman/)

## 5. 공개 저장소 푸시 자동 승인 검토 거부

- 이슈: 자동 승인 검토가 Newman 보고서와 HTTP 기록에 JWT가 포함되어 공개 자격 증명 노출 위험이 있다는 이유로 최초 푸시를 거부했습니다. 원격 푸시는 수행되지 않았습니다.
- 해결: request 헤더·변수의 문자열뿐 아니라 response stream의 바이트 배열까지 순회해 JWT를 제거했습니다. 로컬 원본은 Git 제외 build에 유지합니다. 공개 기록은 실제 응답의 상태·code·시각·검증 결과를 보존하고 JWT 제거 사실을 표시합니다. 아직 공개되지 않은 로컬 커밋을 amend하여 토큰이 포함된 이전 커밋을 브랜치 이력에서도 제외했습니다.
