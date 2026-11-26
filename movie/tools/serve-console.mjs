import http from 'node:http';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);
const routes = new Map([
  ['/', ['tools/week07-console.html', 'text/html; charset=utf-8']],
  ['/week07-report', ['docs/evidence/week07/newman-report.json', 'application/json; charset=utf-8']],
  ['/db-email', ['build/mission-evidence/db-unique-email.txt', 'text/plain; charset=utf-8']],
  ['/db-nickname', ['build/mission-evidence/db-unique-nickname.txt', 'text/plain; charset=utf-8']],
]);
const server = http.createServer(async (request, response) => {
  const route = routes.get(new URL(request.url, 'http://localhost').pathname);
  if (request.method !== 'GET' || !route) {
    response.writeHead(404).end();
    return;
  }
  try {
    response.writeHead(200, { 'Content-Type': route[1], 'Cache-Control': 'no-store' });
    response.end(await readFile(new URL(route[0], root)));
  } catch {
    response.writeHead(404).end('먼저 테스트 또는 Newman 컬렉션을 실행해주세요.');
  }
});
server.listen(5173, '127.0.0.1', () => console.log('API console: http://localhost:5173'));
