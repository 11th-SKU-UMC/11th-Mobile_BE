import { mkdir, writeFile } from 'node:fs/promises';

const info = { name: 'Week07 - JWT 인증과 평점 소유권',
  description: '필수 9단계와 추가 체크리스트, 선택 미션 60초 만료. 기본 서버 8080, 만료 실험 서버 8081(local 프로필, JWT_ACCESS_EXPIRATION=60). 임시 H2 계정만 생성합니다.',
  schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json' };
function request(name, method, path, body, status, code, token, extra = [], base = 'baseUrl', pre = []) {
  const tests = [
    `pm.test('HTTP ${status}', () => pm.response.to.have.status(${status}));`,
    'const json = pm.response.json();',
    `pm.test('응답 코드 ${code}', () => pm.expect(json.code).to.eql('${code}'));`,
    `pm.test('공통 응답 형식', () => { pm.expect(json).to.have.all.keys('isSuccess','code','message','result'); pm.expect(json.isSuccess).to.eql(${status < 400}); });`,
    ...extra,
  ];
  return { name, request: { method, header: [
      ...(body ? [{ key: 'Content-Type', value: 'application/json' }] : []),
      ...(token ? [{ key: 'Authorization', value: `Bearer {{${token}}}` }] : []),
    ], url: `{{${base}}}${path}`,
    ...(body ? { body: { mode: 'raw', raw: JSON.stringify(body, null, 2), options: { raw: { language: 'json' } } } } : {}),
  }, event: [
    ...(pre.length ? [{ listen: 'prerequest', script: { type: 'text/javascript', exec: pre } }] : []),
    { listen: 'test', script: { type: 'text/javascript', exec: tests } },
  ] };
}
const account = role => ({ email: `${role}{{runId}}@example.com`, password: 'Umc!1234', nickname: `${role}{{runId}}` });
const credentials = role => ({ email: `${role}{{runId}}@example.com`, password: 'Umc!1234' });
const mandatory = [
  request('01A 준비 - 회원 A 가입', 'POST', '/members', account('a'), 201, 'COMMON201', null,
    ["pm.collectionVariables.set('memberA', json.result.memberId);"], 'baseUrl', ["pm.collectionVariables.set('runId', Date.now().toString().slice(-9));"]),
  request('01B 준비 - 회원 B 가입', 'POST', '/members', account('b'), 201, 'COMMON201'),
  request('02A 로그인 A - 제출 캡처 1', 'POST', '/auth/login', credentials('a'), 200, 'COMMON200', null,
    ["pm.collectionVariables.set('tokenA', json.result.accessToken);", "pm.test('JWT 형식', () => pm.expect(json.result.accessToken.split('.')).to.have.lengthOf(3));"]),
  request('02B 로그인 B', 'POST', '/auth/login', credentials('b'), 200, 'COMMON200', null,
    ["pm.collectionVariables.set('tokenB', json.result.accessToken);"]),
  request('03 A 평점 등록 - 제출 캡처 4', 'POST', '/ratings', { movieId: 550, score: 5, comment: '토큰 A로 등록한 평점' }, 201, 'COMMON201', 'tokenA',
    ["pm.collectionVariables.set('ratingId', json.result.ratingId);", "pm.test('바디에 memberId 없음', () => pm.expect(JSON.parse(pm.request.body.raw)).not.to.have.property('memberId'));"]),
  request('04 같은 영화 중복 등록', 'POST', '/ratings', { movieId: 550, score: 3 }, 409, 'RATING409_1', 'tokenA'),
  request('05 A 본인 평점 수정', 'PATCH', '/ratings/{{ratingId}}', { score: 4, comment: '본인 수정 성공' }, 200, 'COMMON200', 'tokenA',
    ["pm.test('수정값', () => pm.expect(json.result.score).to.eql(4));"]),
  request('06 B가 A 평점 수정 - 제출 캡처 3', 'PATCH', '/ratings/{{ratingId}}', { score: 1 }, 403, 'RATING403_1', 'tokenB'),
  request('07 토큰 없이 평점 수정', 'PATCH', '/ratings/{{ratingId}}', { score: 1 }, 401, 'AUTH401_2'),
  request('08 없는 평점 삭제', 'DELETE', '/ratings/999999999', null, 404, 'RATING404_1', 'tokenA'),
  request('09 A 본인 평점 삭제', 'DELETE', '/ratings/{{ratingId}}', null, 200, 'COMMON200', 'tokenA',
    ["pm.test('삭제 result는 null', () => pm.expect(json.result).to.eql(null));"]),
];
const extras = [
  request('추가 01 토큰 없이 내 정보 - 제출 캡처 2', 'GET', '/members/me', null, 401, 'AUTH401_2'),
  request('추가 02 내 정보 조회 - LoginMember 정상 동작', 'GET', '/members/me', null, 200, 'COMMON200', 'tokenA',
    ["pm.test('비밀번호 미노출', () => pm.expect(json.result).to.have.all.keys('email','nickname','createdAt'));"]),
  request('추가 03 본인 닉네임 유지', 'PATCH', '/members/me', { nickname: 'a{{runId}}' }, 200, 'COMMON200', 'tokenA'),
  request('추가 04 다른 회원 닉네임 중복', 'PATCH', '/members/me', { nickname: 'b{{runId}}' }, 409, 'MEMBER409_1', 'tokenA'),
  request('추가 05 닉네임 변경', 'PATCH', '/members/me', { nickname: 'c{{runId}}' }, 200, 'COMMON200', 'tokenA'),
  request('추가 06 없는 이메일', 'POST', '/auth/login', { email: 'missing{{runId}}@example.com', password: 'wrong' }, 401, 'AUTH401_1', null,
    ["pm.collectionVariables.set('loginFailure', pm.response.text());"]),
  request('추가 07 틀린 비밀번호 - 실패 응답 일치', 'POST', '/auth/login', { email: 'a{{runId}}@example.com', password: 'wrong' }, 401, 'AUTH401_1', null,
    ["pm.test('가입 여부 노출 방지', () => pm.expect(pm.response.text()).to.eql(pm.collectionVariables.get('loginFailure')));"]),
  request('추가 08 JWT Payload 변조', 'GET', '/members/me', null, 401, 'AUTH401_3', 'tampered', [], 'baseUrl', [
    "const pieces = pm.collectionVariables.get('tokenA').split('.');",
    "const index = Math.floor(pieces[1].length / 2);",
    "pieces[1] = pieces[1].slice(0, index) + (pieces[1][index] === 'a' ? 'b' : 'a') + pieces[1].slice(index + 1);",
    "pm.collectionVariables.set('tampered', pieces.join('.'));",
  ]),
];
const expiry = [
  request('선택 01 만료 실험 회원 가입', 'POST', '/members', account('e'), 201, 'COMMON201', null, [], 'expiryBaseUrl'),
  request('선택 02 60초 토큰 발급', 'POST', '/auth/login', credentials('e'), 200, 'COMMON200', null,
    ["pm.collectionVariables.set('expiryToken', json.result.accessToken);"], 'expiryBaseUrl'),
  request('선택 03 만료 전 성공', 'GET', '/members/me', null, 200, 'COMMON200', 'expiryToken', [], 'expiryBaseUrl'),
  request('선택 04 62초 후 같은 토큰으로 실패', 'GET', '/members/me', null, 401, 'AUTH401_4', 'expiryToken', [], 'expiryBaseUrl',
    ['setTimeout(() => {}, 62000);']),
];
const collection = { info, variable: [
  { key: 'baseUrl', value: 'http://localhost:8080' }, { key: 'expiryBaseUrl', value: 'http://localhost:8081' },
], item: [{ name: '필수 9단계', item: mandatory }, { name: '추가 체크리스트', item: extras }, { name: '선택 - 60초 만료', item: expiry }] };
await mkdir(new URL('../docs/postman/', import.meta.url), { recursive: true });
await writeFile(new URL('../docs/postman/week07.postman_collection.json', import.meta.url), JSON.stringify(collection, null, 2) + '\n');
console.log('Generated docs/postman/week07.postman_collection.json');
