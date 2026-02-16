# MSA Shop v2

> Spring Boot 3.2 기반 MSA 쇼핑몰 실무 시뮬레이션 프로젝트
> 상태 전이 통제 · 결제 멱등성 · 서비스 경계 설계에 초점을 둔 구현

---

# 🎯 1. 설계 목표 (Design Goals)

이 프로젝트는 단순 CRUD 쇼핑몰이 아닙니다.

다음 설계 요소를 명확히 구현하는 것을 목표로 했습니다.

* 주문 상태 전이를 **도메인 모델에서 통제**
* 결제 승인 API의 **멱등(Idempotency) 보장**
* 서비스 간 DB 공유 금지 (데이터 경계 유지)
* Gateway 단일 JWT 검증 구조
* 내부 API 보호 전략 구현
* 분산 트랜잭션 없이 일관성 유지 설계

---

# 🏗 2. 전체 아키텍처

![전체 시스템 구성도](./docs/system-diagram.png)

각 서비스는 자신의 DB를 소유하며,
다른 서비스의 데이터베이스에 직접 접근하지 않습니다.

---

# 🔐 3. 인증 및 보안 설계

## 3.1 Gateway 단일 JWT 검증 구조

* JWT는 Gateway에서만 검증
* 검증 성공 시 다음 헤더를 Downstream 서비스로 전달

    * `X-User-Id`
    * `X-Roles`

### 왜 Gateway에서만 검증하는가?

* 인증 책임을 단일 계층에 집중
* 하위 서비스 단순화
* JWT 파싱 중복 제거
* 테스트 용이성 향상

## 3.2 토큰/세션 설계 요약
- Access Token은 RS256 기반 JWT로 발급되며, roles 클레임을 포함합니다.
- Refresh Token은 HttpOnly Cookie로 전달하고, 서버에는 원문이 아닌 해시(SHA-256)로 저장합니다.
- Refresh는 회전(rotate) 전략을 사용하며, Logout은 멱등하게 동작합니다.
- Gateway가 JWT를 검증한 뒤 `X-User-Id`, `X-Roles`로 인증 컨텍스트를 Downstream 서비스에 전파합니다.


---

## 3.2 Downstream 서비스 보안 모델

하위 서비스는 JWT를 직접 검증하지 않습니다.

대신:

* `GatewayAuthHeaderFilter`가 SecurityContext 구성
* `@AuthenticationPrincipal CurrentUser`로 사용자 정보 주입
* `@PreAuthorize`로 권한 검증 수행

---

## 3.3 공개 API / 인증 API / 내부 API 구분

| 구분 | 예시 | 보호 방식 |
|---|---|---|
| 공개 API | `GET /api/products/**` | `permitAll` |
| 인증 필요 API | 주문/결제/내정보 (`/api/orders/**`, `/api/payments/**`, `/api/users/**`) | Gateway JWT 검증 + 헤더 기반 인증 컨텍스트 |
| 내부 API | `/internal/**` | `X-Internal-Secret` 검증(InternalSecretFilter) |

> 인증(Authentication)은 Gateway에서 처리하고, 인가(Authorization)는 각 서비스에서 `@PreAuthorize`로 수행합니다.

📌 이 구조를 통해 “인증”과 “인가” 책임을 분리했습니다.

---
## 3.4 내부 호출 신뢰 모델

현재 구조에서는 Gateway만 외부 JWT를 검증하며,
서비스 간 호출은 `X-Internal-Secret` 헤더 기반으로 보호합니다.

이는 다음을 전제로 합니다:

- 내부 네트워크는 신뢰 영역으로 가정
- 외부 요청은 반드시 Gateway를 통과
- 내부 호출은 인증(Authentication)이 아닌 서비스 간 신뢰(Trust) 기반

> 실제 운영 환경에서는 mTLS, 서비스 토큰, 네트워크 레벨 방화벽 등을 추가로 고려할 수 있습니다.
---

# 🧠 4. 도메인 중심 설계

## 4.1 Order Aggregate가 상태를 통제

```text
CREATED
PENDING_PAYMENT
PAID
CANCELLED
```

전이 규칙:

* CREATED → PENDING_PAYMENT
* PENDING_PAYMENT → PAID
* CREATED/PENDING_PAYMENT → CANCELLED
* PAID 이후 취소 불가

### 왜 상태를 Domain에서 관리하는가?

* 서비스 레이어 우회 방지
* 잘못된 상태 점프 방지
* 멱등 처리 자연스러운 구현
* 비즈니스 규칙 중앙 집중

---
## 4.2 상태 전이 제약 조건

Order Aggregate는 단순 상태 변경이 아니라
**비즈니스 제약 조건을 함께 보장**합니다.

예시:

- markPaid()는 반드시 PENDING_PAYMENT 상태에서만 허용
- 이미 PAID 상태라면 no-op (멱등)
- CANCELLED 상태에서는 결제 완료 불가
- PAID 이후 취소 불가

![주문 상태 전이 다이어그램](./docs/status-diagram.png)

>재고 차감은 Order Service가 직접 수행하며, Payment Service는 Product Service를 직접 호출하지 않습니다.

---

# 💳 5. 결제 설계 및 멱등 처리

## 5.1 결제 승인 흐름

![결제 시퀀스 다이어그램](./docs/payment-sequence.png)

---

## 5.2 멱등(Idempotency) 전략

* `idempotencyKey` 필수
* DB UNIQUE 제약
* 선조회 + 충돌 처리
* 도메인 메서드 멱등 보장

결과:

* 중복 클릭 방지
* 재고 중복 차감 방지
* 주문 상태 중복 전이 방지

> 동일 idempotencyKey로 재요청이 발생하더라도,
Payment 트랜잭션은 1건만 유지되며, Order 상태 및 재고는 추가 변경되지 않습니다.

---

# 📦 6. 재고 차감 전략

재고는 결제 승인 성공 후 차감합니다.

```sql
UPDATE product
SET stock = stock - :qty
WHERE product_id = :id
  AND stock >= :qty;
```

분산 트랜잭션 대신:

* 순차 전이
* 멱등 처리
* DB 원자적 업데이트

> 재고 차감은 stock >= 요청수량 조건을 포함한 단일 UPDATE 문으로 수행되어, 동시성 환경에서도 음수 재고를 방지합니다.
---

# 🧪 7. E2E 흐름

1. 상품 조회 (비로그인 가능)
2. 주문 생성 (CREATED)
3. 결제 시작 → 상태 PENDING_PAYMENT 전이
4. 결제 승인 → 상태 PAID 전이
5. 주문 상태 PAID
6. 재고 감소
7. 동일 idempotencyKey 재호출 → 변화 없음

> 위 흐름은 통합 테스트를 통해 검증되었으며, 멱등성과 상태 전이의 일관성을 확인했습니다.
---

# ⚖ 8. 설계 트레이드오프

적용하지 않은 것:

* Saga 패턴
* Outbox 패턴
* 메시지 브로커
* 분산 트랜잭션
* 재고 예약 모델

### 이유

분산 복잡성을 추가하기 전에
**상태 관리와 멱등성에 집중**하기 위함.

---

# 🚀 9. 향후 확장 가능성

* Outbox 기반 이벤트 발행
* Saga 기반 주문–결제 오케스트레이션
* 환불/부분환불 모델
* Observability (Tracing, MDC)
* mTLS 기반 내부 통신 보안 강화

---

# 10. 로컬 실행 (Quickstart)

## 10.1 사전 준비
- JDK 21
- Docker (PostgreSQL/Redis)
- (선택) Node.js (프론트 실행 시)

## 10.2 인프라 기동
```bash
docker compose -f infra/docker-compose.yml up -d
```

## 10.3 서비스 실행
```bash
./gradlew :gateway:bootRun --args='--spring.profiles.active=local'
./gradlew :auth-service:bootRun --args='--spring.profiles.active=local'
./gradlew :user-service:bootRun --args='--spring.profiles.active=local'
./gradlew :product-service:bootRun --args='--spring.profiles.active=local'
./gradlew :order-service:bootRun --args='--spring.profiles.active=local'
./gradlew :payment-service:bootRun --args='--spring.profiles.active=local'

```

## 10.4 기본 포트

- gateway: 8080
- auth-service: 8081
- product-service: 8082
- order-service: 8083
- payment-service: 8084
- user-service: 8085

---

# 📸 프론트엔드 화면

![상품 목록 화면](./docs/상품목록.png)

![주문 화면](./docs/주문.png)

![결제 화면](./docs/결제.png)

---

## 🧪 E2E 통합 테스트

- `e2e-test` 모듈에서 Gateway(8080) 기준으로 실제 HTTP E2E 테스트를 수행합니다.
- 전제: docker-compose 인프라 + 모든 서비스가 실행 중이어야 합니다.

```bash
./gradlew :e2e-test:test
```
---

## 🧩 설계 관점 요약

이 프로젝트는 기능 구현 자체보다

- 상태 전이 통제
- 멱등 설계
- 서비스 경계 유지
- 인증/인가 책임 분리

를 실제 코드로 구현하고 검증하는 데 목적이 있습니다.