# MSA Shop Frontend (Vue 3 + Vite)

Gateway(API Base: `http://localhost:8080/api`)와 연동되는 Vue SPA입니다. 상품 조회, 장바구니, 주문 생성, 결제 요청, 주문 상태 확인 흐름을 로컬에서 확인할 수 있습니다.

## 실행 방법

```bash
cd services/frontend
npm install
npm run local      # .env.localdev (직접 API 주소)
npm run dev        # .env.development (프록시 기본값)
npm run build:prod # .env.production 빌드
```

- 개발 서버: <http://localhost:5173>
- 기본 API 베이스: `/api` (`VITE_API_BASE`로 변경 가능)
- 로컬에서 Gateway를 프록시하려면 `.env.development`의 `VITE_DEV_SERVER_PROXY_TARGET` 값을 원하는 주소로 수정하세요.

## 사용 방법

1. 로그인 또는 회원가입 후 상품을 장바구니에 담습니다.
2. 장바구니에서 주문/결제를 요청합니다.
3. 주문 상세에서 결제 결과와 주문 상태를 확인합니다.

## NPM Scripts

| Script | 설명 |
| --- | --- |
| `npm run local` | 로컬 API(.env.localdev)로 개발 서버 실행 |
| `npm run dev` | 프록시 기반 개발 서버(.env.development) 실행 |
| `npm run build:local` | 로컬 설정으로 빌드 |
| `npm run build:dev` | 개발 설정으로 빌드 |
| `npm run build:prod` | 프로덕션 설정으로 빌드 |
| `npm run preview` | 빌드 결과 미리보기 |
| `npm run lint` | ESLint 검사 |
| `npm run format` | Prettier 정렬 |

## 폴더 구조

```
src/
  api/http.js        # Axios 인스턴스
  components/AppShell.vue
  pages/             # 로그인, 상품, 장바구니, 주문 화면
  router/index.js
  stores/            # 사용자, 상품, 장바구니, 주문 상태
  styles/app.css
  main.js
```

## API 메모

- `POST /api/auth/login` : Access Token(JSON), Refresh Token은 HttpOnly 쿠키
- `POST /api/auth/logout` : 쿠키 정리 및 재로그인 대비
- `POST /api/orders/{orderId}/pay` : 결제 Saga 시작
- `GET /api/orders/{orderId}` : 주문 상세와 결제 상태 조회
- 모든 Axios 요청은 `withCredentials: true`로 전송하며 401 응답 시 `/login`으로 이동합니다.
