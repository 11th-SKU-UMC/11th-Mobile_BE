import { mkdir, readFile, readdir, writeFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);
const reportText = await readFile(new URL('build/newman-week07.json', root), 'utf8');
const report = JSON.parse(reportText);
if (report.run.failures.length || report.run.executions.length !== 23) {
  throw new Error('성공한 전체 23개 HTTP 요청의 보고서가 필요합니다.');
}
const out = new URL('docs/evidence/week07/', root);
await mkdir(out, { recursive: true });
// 공개 저장소에 bearer 자격 증명을 남기지 않습니다. 로컬 build의 원본은 Git에서 제외됩니다.
const jwtPattern = /\beyJ[A-Za-z0-9_-]*\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b/g;
const redact = text => text.replace(jwtPattern, '[REDACTED_LOCAL_TEST_JWT]');
function publicEvidence(value) {
  if (typeof value === 'string') return redact(value);
  if (Array.isArray(value)) return value.map(publicEvidence);
  if (value && typeof value === 'object') {
    // Newman의 response stream에도 토큰이 바이트 배열로 들어 있습니다.
    if (value.type === 'Buffer' && Array.isArray(value.data)) {
      return { ...value, data: [...Buffer.from(redact(Buffer.from(value.data).toString('utf8')))] };
    }
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, publicEvidence(item)]));
  }
  return value;
}
const publicReport = publicEvidence(report);
function assertNoJwt(value) {
  if (typeof value === 'string' && value.match(jwtPattern)) throw new Error('공개 기록에 JWT가 남아 있습니다.');
  if (!value || typeof value !== 'object') return;
  if (value.type === 'Buffer' && Array.isArray(value.data)) assertNoJwt(Buffer.from(value.data).toString('utf8'));
  for (const item of Object.values(value)) if (typeof item !== 'number') assertNoJwt(item);
}
assertNoJwt(publicReport);
publicReport._evidence = {
  redacted: 'All JWT values in request headers, variables and response streams are removed. HTTP status, response fields, timestamps and assertions are preserved.',
  original: 'build/newman-week07.json is local-only and excluded from Git.',
};
await writeFile(new URL('newman-report.json', out), JSON.stringify(publicReport, null, 2) + '\n');
const decode = execution => JSON.parse(Buffer.from(execution.response.stream.data).toString('utf8'));
const address = request => typeof request.url === 'string' ? request.url :
  `${request.url.protocol}://${request.url.host.join('.')}${request.url.port ? ':' + request.url.port : ''}/${request.url.path.join('/')}`;
const transcript = publicReport.run.executions.map(execution => {
  const headers = execution.request.header.filter(h => !h.disabled && ['content-type', 'authorization'].includes(h.key.toLowerCase()));
  const date = execution.response.header.find(h => h.key.toLowerCase() === 'date')?.value;
  return [execution.item.name, `${execution.request.method} ${address(execution.request)}`,
    ...headers.map(h => `${h.key}: ${h.value}`), execution.request.body?.raw || '(요청 바디 없음)',
    `HTTP ${execution.response.code} ${execution.response.status}`, `Date: ${date}`,
    JSON.stringify(decode(execution), null, 2),
    ...execution.assertions.map(a => `${a.error ? 'FAIL' : 'PASS'} ${a.assertion}`), ''].join('\n');
}).join('\n');
await writeFile(new URL('http-transcript.txt', out), transcript);
assertNoJwt(transcript);
const testDirectory = new URL('build/test-results/test/', root);
let total = 0, failed = 0, errors = 0, skipped = 0;
const suites = [];
for (const file of await readdir(testDirectory)) {
  if (!file.startsWith('TEST-') || !file.endsWith('.xml')) continue;
  const xml = await readFile(new URL(file, testDirectory), 'utf8');
  const attributes = Object.fromEntries([...xml.match(/<testsuite\b([^>]*)>/)[1].matchAll(/([\w-]+)="([^"]*)"/g)].map(m => [m[1], m[2]]));
  total += Number(attributes.tests); failed += Number(attributes.failures); errors += Number(attributes.errors); skipped += Number(attributes.skipped);
  suites.push(`${attributes.name}: ${attributes.tests} tests, ${attributes.failures} failures, ${attributes.errors} errors, ${attributes.skipped} skipped`);
}
const before = report.run.executions.find(e => e.item.name.startsWith('선택 03'));
const after = report.run.executions.find(e => e.item.name.startsWith('선택 04'));
const auth = e => e.request.header.find(h => h.key.toLowerCase() === 'authorization').value;
if (auth(before) !== auth(after) || before.response.code !== 200 || decode(after).code !== 'AUTH401_4') {
  throw new Error('동일 토큰의 만료 전후 검증에 실패했습니다.');
}
const summary = [
  `실행 시작(KST): ${new Date(report.run.timings.started).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' })}`,
  '환경: Spring Boot 4.1.2 / Java 21 toolchain / H2 MySQL mode / JJWT 0.12.6',
  '빌드: gradlew.bat test bootJar --console=plain — BUILD SUCCESSFUL',
  ...suites, `JUnit total: ${total}, failures: ${failed}, errors: ${errors}, skipped: ${skipped}`,
  `Newman 6.2.1: ${report.run.stats.requests.total} HTTP requests, ${report.run.stats.assertions.total} assertions, ${report.run.failures.length} failures`,
  `실제 HTTP 실행 시간: ${report.run.timings.completed - report.run.timings.started} ms`,
  '선택 1: JWT_ACCESS_EXPIRATION=60, JWT_CLOCK_SKEW=0. 동일 토큰으로 200, 62초 대기 후 401/AUTH401_4 확인.',
  '선택 2: @LoginMember 적용. 내 정보 조회/수정 및 평점 쓰기 API를 통합 테스트와 Newman으로 검증.',
  '공개 기록의 모든 JWT는 [REDACTED_LOCAL_TEST_JWT]로 제거했습니다. 원본은 Git에서 제외된 build에만 있습니다.',
  '테스트 전용 계정만 사용하고 실행 시 JWT 서명 키를 무작위 생성했습니다. 키는 저장하지 않습니다.',
  '제약: 실제 MySQL DB 검증은 수행하지 않았습니다.',
  '캡처: Postman 데스크톱 앱 미설치. 브라우저 URL 확인 실패로 Computer Use가 중단되어 화면 캡처 4장은 미완료입니다.',
].join('\n') + '\n';
await writeFile(new URL('test-summary.txt', out), summary);
console.log(summary);
