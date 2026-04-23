# MSA Shop v2

주문, 재고, 결제 흐름을 Kafka Saga와 Outbox/Processed Event Claim 패턴으로 분리한 Spring Boot 기반 MSA 프로젝트입니다.
단순 CRUD보다, 서비스 경계가 분리된 환경에서 상태 일관성과 복구 가능성을 어떻게 설계할지에 초점을 맞췄습니다.

## 링크

- 아키텍처 다이어그램: [system-diagram.svg](./docs/system-diagram.svg)
- 주문 상태 다이어그램: [status-diagram.svg](./docs/status-diagram.svg)
- 결제 Saga 시퀀스: [payment-sequence.svg](./docs/payment-sequence.svg)
- 포트폴리오 요약: [portfolio.md](./docs/portfolio.md)
- AWS 배포 구조 정리: <https://chang97.tistory.com/162>
- ECS/Fargate 배포 시행착오 정리: <https://chang97.tistory.com/163>
- 프론트 연결 포함 최종 배포 구조: <https://chang97.tistory.com/164>

## 프로젝트 개요

이 프로젝트는 주문, 재고, 결제처럼 여러 서비스의 상태가 연쇄적으로 바뀌는 문제를 MSA 환경에서 다룹니다.

서비스는 아래처럼 분리되어 있습니다.

- `gateway`
- `auth-service`
- `user-service`
- `product-service`
- `order-service`
- `payment-service`

각 서비스는 자기 DB만 소유하고, 중요한 비즈니스 흐름은 Kafka 기반 Saga로 연결합니다.

![시스템 다이어그램](./docs/system-diagram.svg)

## 핵심 기능

- JWT 기반 인증/인가
- 회원가입 Saga와 사용자 프로필 생성 연동
- 상품 조회 및 관리자 상품 등록
- 주문 생성, 결제 시작, 주문 상태 추적
- 재고 예약, 재고 확정, 재고 해제, 예약 만료 처리
- 결제 승인/실패/불명확 상태(`APPROVAL_UNKNOWN`) 처리
- `PAYMENT_EXPIRED` 만료와 만료 주문 재결제
- Redis 기반 로그인 실패 잠금
- Redis 기반 API rate limit

## 서비스별 책임

### `gateway`

- 클라이언트 요청의 단일 진입점
- JWT 검증 후 사용자 정보를 내부 헤더로 전파
- Redis 기반 rate limit 적용
  - `/api/auth/login`
  - `/api/auth/refresh`
  - `/api/orders/*/pay`

### `auth-service`

- 로그인, 토큰 발급, refresh, logout
- 인증 계정과 권한 정보 관리
- Redis 기반 로그인 실패 횟수 추적 및 계정 잠금
- 회원가입 Saga 시작

### `user-service`

- 사용자 프로필 정보 관리
- 회원가입 Saga에서 사용자 프로필 생성
- 비활성화 시 `USER_DEACTIVATED` 이벤트 발행

### `product-service`

- 상품 정보와 재고 관리
- 주문-결제 Saga에서 재고 예약, 확정, 해제 처리
- 예약 만료 스케줄러 운영

### `order-service`

- 주문 생성, 조회, 취소
- 결제 Saga 시작
- 결제/재고 결과를 반영한 주문 상태 전이
- `PENDING_PAYMENT` 만료 스케줄러 운영

### `payment-service`

- 결제 정보 저장
- PG 호출
- `APPROVAL_UNKNOWN` 상태 저장
- reconciliation으로 최종 결제 상태 확정

## 핵심 설계 포인트

### 1. Kafka Saga로 주문-재고-결제 흐름 분리

주문, 재고, 결제를 하나의 DB 트랜잭션으로 묶을 수 없는 구조에서 Saga로 상태 전이를 연결했습니다.

흐름 요약:

1. `order-service`가 주문을 `PENDING_PAYMENT`로 바꾸고 `STOCK_RESERVATION_REQUESTED`를 발행
2. `product-service`가 재고를 예약하고 `STOCK_RESERVED` 또는 `STOCK_RESERVATION_FAILED`를 발행
3. `payment-service`는 `STOCK_RESERVED`를 받은 경우에만 PG 호출을 진행
4. `payment-service`는 결제 결과에 따라 `PAYMENT_APPROVED` 또는 `PAYMENT_FAILED`를 발행
5. `order-service`와 `product-service`가 같은 결과 이벤트를 각각 소비해 주문 상태 반영과 재고 확정/해제를 수행

![결제 Saga 시퀀스](./docs/payment-sequence.svg)

### 2. Outbox + Processed Event Claim

단순 Kafka publish/consume만으로는 발행 유실, 중복 처리, 멀티 인스턴스 경쟁 문제를 다루기 어렵습니다.
그래서 아래 구조를 적용했습니다.

- 비즈니스 상태 변경과 outbox 적재를 같은 로컬 트랜잭션에서 처리
- relay가 outbox를 읽어 Kafka로 발행
- consumer가 `processed_event`를 먼저 claim
- 동일 이벤트를 여러 인스턴스가 동시에 처리하지 않도록 제어
- 처리 완료된 이벤트는 다시 들어와도 같은 작업을 반복하지 않도록 방어

### 3. 결제 불명확 상태와 reconciliation

PG 응답이 timeout 또는 네트워크 예외로 불명확한 경우, 즉시 실패로 단정하지 않고 `APPROVAL_UNKNOWN`으로 저장합니다.

- 즉시 승인/실패 확정이 어려우면 `APPROVAL_UNKNOWN`
- 현재 메시지는 처리 완료로 기록
- 이후 reconciliation 스케줄러가 PG 상태를 다시 조회
- 최종적으로 `APPROVED` 또는 `FAILED`로 확정

### 4. 주문 상태 모델

주문 aggregate는 아래 상태를 가집니다.

- `CREATED`
- `PENDING_PAYMENT`
- `PAID`
- `PAYMENT_FAILED`
- `PAYMENT_EXPIRED`
- `CANCELLED`

주문 생성 시점에는 재고를 선점하지 않습니다.
재고 부족은 결제 시작 이후 재고 예약 단계에서 실패로 처리됩니다.

![주문 상태 다이어그램](./docs/status-diagram.svg)

### 5. Redis 기반 운영 보강

Redis는 단순 캐시가 아니라 운영성 보강 용도로 사용했습니다.

- gateway rate limit
- auth-service 로그인 실패 잠금

브루트포스나 과도한 결제 요청처럼 민감한 API 호출을 제어하기 위한 목적입니다.

## 테스트 전략

테스트는 `unit` / `integration` / `e2e`로 나눠 구성했습니다.

### Unit Test

- 서비스 분기 로직
- 주문 상태 전이
- 이벤트 발행/처리 조건
- Redis 로그인 잠금

### Integration Test

- JPA persistence adapter
- Kafka Saga 서비스 조합
- Testcontainers 기반 PostgreSQL 검증
- Outbox / processed event 처리
- `PAYMENT_EXPIRED` 저장

### E2E Test

`e2e-test` 모듈에서 gateway 기준 HTTP 시나리오를 검증합니다.

- 회원가입, 로그인, refresh/logout 인증 흐름
- 상품 공개 조회와 관리자 전용 생성 권한
- 주문 생성 시 `CREATED` 저장과 비활성/판매중지 상품 거절
- 결제 성공 후 `PAID` 반영과 재고 감소
- PG 실패 후 `PAYMENT_FAILED`와 재고 복구
- 재고 부족이 주문 생성이 아니라 결제 단계에서 실패하는 흐름
- `APPROVAL_UNKNOWN` 이후 reconciliation으로 승인 또는 실패로 수렴하는 흐름
- `PAYMENT_EXPIRED` 만료와 만료 주문 재결제 흐름
- 결제 완료 주문 취소 불가

## 로컬 실행

인프라만 Docker로 띄우고 서비스는 JVM에서 실행할 수 있습니다.

```bash
docker compose -f infra/docker-compose.yml up -d
./gradlew :gateway:bootRun --args='--spring.profiles.active=local'
./gradlew :auth-service:bootRun --args='--spring.profiles.active=local'
./gradlew :user-service:bootRun --args='--spring.profiles.active=local'
./gradlew :product-service:bootRun --args='--spring.profiles.active=local'
./gradlew :order-service:bootRun --args='--spring.profiles.active=local'
./gradlew :payment-service:bootRun --args='--spring.profiles.active=local'
```

전체 Docker 스택 실행도 가능합니다.

```bash
docker compose -f infra/docker-compose.full.yml up --build -d
```

E2E 테스트는 로컬 서비스와 인프라가 떠 있는 상태에서 실행합니다.

```bash
./gradlew :e2e-test:test
```

## AWS 배포 구조

최종적으로는 아래 구조로 배포를 검증했습니다.

- Public ALB
- frontend nginx EC2
- Internal ALB
- gateway ECS
- backend ECS services
- RDS PostgreSQL
- Kafka EC2
- Redis EC2

프론트엔드 same-origin 구조를 유지하기 위해 public ALB와 internal ALB를 분리했습니다.
자세한 배포 과정은 상단 블로그 링크에 정리했습니다.

## 디렉터리 구조

```text
.
├─ services/
│  ├─ gateway/
│  ├─ auth-service/
│  ├─ user-service/
│  ├─ product-service/
│  ├─ order-service/
│  ├─ payment-service/
│  └─ frontend/
├─ libs/
│  └─ common-web/
├─ e2e-test/
├─ infra/
└─ docs/
```

## 기술 스택

- Backend: `Java 21`, `Spring Boot 3.2`, `Spring Security`, `Spring Data JPA`, `Flyway`
- Messaging: `Kafka`
- Database: `PostgreSQL`
- Cache / NoSQL: `Redis`
- Infra: `Docker`, `AWS ECS Fargate`, `ALB`, `RDS`, `EC2`, `CloudWatch Logs`, `ECR`
- Test: `JUnit 5`, `Mockito`, `Testcontainers`, `RestAssured`

## 남은 개선 포인트

- PG webhook 연동
- DLQ 후처리 도구
- 모니터링/알림 체계 보강
- CI/CD 자동화
