# MSA Shop Frontend (Auth 전용 미니 UI)

기존 쇼핑몰 화면을 모두 정리하고, Auth 서비스 CORS 동작을 확인하기 위한 **로그인 전용 단일 페이지**만 남겨두었습니다. `POST /api/auth/login` 호출과 쿠키/토큰 반환 여부를 빠르게 점검할 때 사용하세요.

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

1. 로그인 ID와 비밀번호를 입력하고 **로그인**을 누릅니다.
2. 성공 시 응답으로 받은 Access Token을 화면에서 확인하거나 복사할 수 있습니다.
3. Refresh Token은 HttpOnly 쿠키로 저장됩니다. **다시 로그인하기** 버튼으로 로그아웃 후 재시도할 수 있습니다.

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

## 폴더 구조 (간소화)

```
src/
  api/http.js        # Axios 인스턴스
  components/AppShell.vue
  pages/LoginPage.vue
  router/index.js    # 로그인 전용 라우터
  stores/user.js     # 로그인/토큰 상태
  styles/app.css     # 최소 스타일
  main.js
```

## API 메모

- `POST /api/auth/login` : Access Token(JSON), Refresh Token은 HttpOnly 쿠키
- `POST /api/auth/logout` : 쿠키 정리 및 재로그인 대비
- 모든 Axios 요청은 `withCredentials: true`로 전송하며 401 응답 시 `/login`으로 이동합니다.
