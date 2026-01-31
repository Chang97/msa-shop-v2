# MSA Shop Frontend (Vue 3 + Vite)

Gateway(API Base: `http://localhost:8080/api`)와 연동되는 쇼핑몰형 SPA입니다. 로그인 여부와 관계없이 상품을 조회할 수 있으며, 장바구니와 주문/결제를 통해 실제 주문 흐름을 체험할 수 있습니다.

## 1. 실행 방법

```bash
cd frontend
npm install
npm run dev
```

- 개발 서버: <http://localhost:5173>
- 환경 변수: `.env.development`

```ini
VITE_API_BASE=/api
VITE_DEV_SERVER_PROXY_TARGET=http://localhost:8080
```

- 개발 서버는 `/api` 요청을 `VITE_DEV_SERVER_PROXY_TARGET`(기본: `http://localhost:8080`)으로 프록시합니다. 로컬/리모트 어디서든 브라우저는 현재 접속한 호스트 기준 `/api`만 호출하면 되기 때문에 HttpOnly 쿠키가 정상적으로 저장됩니다. 필요한 경우 두 변수를 원하는 주소로 조정하세요.

## 2. NPM Scripts

| Script        | 설명                               |
| ------------- | ---------------------------------- |
| `npm run dev` | Vite 개발 서버                     |
| `npm run build` | 프로덕션 번들 빌드              |
| `npm run preview` | 빌드 결과 미리보기            |
| `npm run lint` | ESLint(.js/.vue) 검사             |
| `npm run format` | Prettier 코드 정렬              |

## 3. 폴더 구조

```
src/
  api/http.js           # Axios 인스턴스
  components/           # AppShell, StatusChip, ProductPickerModal, 등 UI 컴포넌트
  pages/                # Login, Products, Cart, Checkout, Orders, Profile
  router/index.js       # 인증 가드 포함 라우터
  stores/               # auth, order, product Pinia 스토어
  styles/app.css        # 기본 스타일
  main.js               # Vue/Pnia/Router 부트스트랩
```

## 4. 주요 페이지

| Route            | 설명                                                |
|------------------|-----------------------------------------------------|
| `/products`      | 상품 목록 + 검색/장바구니 담기                      |
| `/products/:id`  | 상품 상세, 수량 선택 후 장바구니/바로 주문          |
| `/cart`          | 장바구니 관리 및 수량 변경                          |
| `/checkout`      | 배송 정보 입력 후 주문 생성 (로그인 필요)          |
| `/products/new`  | 판매자 권한 사용자의 상품 등록 폼                   |
| `/orders`        | 주문 목록 (인증 필요)                               |
| `/orders/:id`    | 주문 상세, 상태 변경                                |
| `/profile`       | 사용자 정보 및 로그아웃                              |
| `/login`         | 로그인 폼 (`POST /auth/login`)                       |

## 5. API 요청 요약

- **인증**: `/auth/login`, `/auth/me`, `/auth/logout`
- **상품**: `/products`, `/products/{id}`, `POST /products`(판매자)
- **주문**: `/orders`, `/orders/{id}`, `/orders/{id}/status`

모든 요청은 `withCredentials: true` 상태로 Axios가 전송하며, 401 응답 시 `/login`으로 리다이렉트합니다.

## 6. 주의사항

- HttpOnly 쿠키 기반 인증이므로 브라우저/클라이언트에서 별도의 토큰 저장 필요 없음.
- 목록 필터/페이지 파라미터는 Axios `params`로 직렬화됩니다.
- 상태 변경 실패, 검증 에러 등은 AppShell 상단의 toast 메시지로 표시됩니다.

## 7. 빌드 산출물

빌드 결과(`npm run build`)는 `/dist` 폴더에 생성되며, Vite preview 또는 Gateway 정적 호스팅 영역에 배포할 수 있습니다.
