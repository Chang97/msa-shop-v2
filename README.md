# MSA Shop v2

주문, 재고, 결제 흐름을 Kafka Saga와 Outbox/Processed Event 패턴으로 분리한 Spring Boot 기반 MSA 쇼핑몰 프로젝트입니다.  
단순 CRUD 구현보다, 서비스 경계가 분리된 환경에서 상태 일관성과 복구 가능성을 어떻게 설계할지에 초점을 맞췄습니다.

## 링크

- 아키텍처 다이어그램: [system-diagram.svg](./docs/system-diagram.svg)
- 주문 상태 다이어그램: [status-diagram.svg](./docs/status-diagram.svg)
- 결제 Saga 시퀀스: [payment-sequence.svg](./docs/payment-sequence.svg)
- AWS 배포 정리 1: [aws-part1.md](./docs/blog/aws-part1.md)
- AWS 배포 정리 2: [aws-part2.md](./docs/blog/aws-part2.md)
- AWS 배포 정리 3: [aws-part3.md](./docs/blog/aws-part3.md)
- 포트폴리오 요약: [portfolio.md](./docs/portfolio.md)

외부 링크는 아래 자리에 추가하면 됩니다.

- 데모/시연 링크: `TODO`
- 블로그 링크 1: `TODO`
- 블로그 링크 2: `TODO`
- 블로그 링크 3: `TODO`

## 프로젝트 개요

이 프로젝트는 주문, 재고, 결제처럼 여러 서비스의 상태가 연쇄적으로 바뀌는 문제를 MSA 환경에서 어떻게 다룰지 검증하기 위해 만들었습니다.

서비스는 아래처럼 분리되어 있습니다.

- `gateway`
- `auth-service`
- `user-service`
- `product-service`
- `order-service`
- `payment-service`

각 서비스는 자신의 DB만 소유하고, 중요한 비즈니스 흐름은 Kafka 기반 Saga로 연결합니다.

![시스템 다이어그램](./docs/system-diagram.svg)

## 핵심 기능

- JWT 기반 인증/인가
- 회원가입 Saga와 사용자 프로필 생성 연동
- 상품 조회 및 관리자 상품 등록
- 주문 생성, 결제 시작, 주문 상태 추적
- 재고 예약, 재고 확정, 재고 해제, 예약 만료 처리
- 결제 승인/실패/불명확 상태(`APPROVAL_UNKNOWN`) 처리
- Redis 기반 로그인 실패 잠금
- Redis 기반 API rate limit

## 서비스별 책임

### `gateway`

- 클라이언트 요청의 단일 진입점
- JWT 검증 후 사용자 정보를 내부 헤더로 전달
- Redis 기반 rate limit 적용
  - `/api/auth/login`
  - `/api/auth/refresh`
  - `/api/orders/*/pay`

### `auth-service`

- 로그인, 토큰 발급/재발급/로그아웃
- 인증 계정과 권한 정보 소유
- Redis 기반 로그인 실패 횟수 누적 및 계정 잠금
- 회원가입 시작점
- `USER_DEACTIVATED` 이벤트 소비 후 인증 계정 비활성화

### `user-service`

- 사용자 프로필 정보 소유
- 내 정보 조회/수정/비활성화
- 회원가입 Saga에서 사용자 프로필 생성
- 비활성화 시 `USER_DEACTIVATED` 이벤트 발행

### `product-service`

- 상품 정보와 재고 소유
- 주문-결제 Saga에서 재고 예약/확정/해제 처리
- 예약 만료 스케줄러 운영

### `order-service`

- 주문 생성, 조회, 취소
- 결제 Saga 시작점
- 결제/재고 결과를 반영해 주문 상태 전이
- `PENDING_PAYMENT` 만료 스케줄러 운영

### `payment-service`

- 결제 정보 소유
- 실제 결제 요청 처리
- `APPROVAL_UNKNOWN` 상태 저장
- reconciliation으로 최종 결제 상태 확정

## 핵심 설계 포인트

### 1. Kafka Saga로 주문-재고-결제 흐름 분리

주문, 재고, 결제를 하나의 DB 트랜잭션으로 묶을 수 없는 구조에서 Saga로 상태 전이를 연결했습니다.

흐름 요약:

1. `order-service`가 결제를 시작하고 `STOCK_RESERVATION_REQUESTED` 발행
2. `product-service`가 재고를 예약
3. 예약 성공 시 `payment-service`가 결제를 진행
4. 결제 결과에 따라 재고를 확정하거나 해제
5. `order-service`가 최종 주문 상태를 반영

![결제 Saga 시퀀스](./docs/payment-sequence.svg)

### 2. Outbox + Processed Event Claim

단순 Kafka publish/consume만으로는 중복 처리와 발행 누락 문제를 피하기 어렵기 때문에 아래 구조를 적용했습니다.

- Outbox
  - 비즈니스 상태 변경과 이벤트 적재를 같은 로컬 트랜잭션에서 처리
  - relay가 outbox를 읽어 Kafka로 발행
- Processed Event Claim
  - consumer가 `processed_event`를 먼저 claim
  - 동일 이벤트를 여러 인스턴스가 동시에 처리하지 않도록 제어
  - 예상 가능한 비즈니스 실패와 예외 상황을 분리

### 3. 결제 불명확 상태와 reconciliation

PG 응답이 timeout 또는 네트워크 예외로 불명확한 경우, 즉시 실패로 단정하지 않고 `APPROVAL_UNKNOWN`으로 저장합니다.

- 즉시 승인/실패 확정이 어려우면 `APPROVAL_UNKNOWN`
- 현재 메시지는 처리 완료로 기록
- 이후 reconciliation 스케줄러가 PG 상태를 다시 조회
- 최종적으로 `APPROVED` 또는 `FAILED`로 확정

### 4. Redis 기반 운영 보강

Redis는 단순 캐시가 아니라 운영성 보강 용도로 사용했습니다.

- gateway rate limit
- auth-service 로그인 실패 잠금

이로써 브루트포스와 과도한 민감 API 호출을 억제할 수 있게 했습니다.

## 주문 상태 모델

주문 aggregate는 아래 상태를 가집니다.

- `CREATED`
- `PENDING_PAYMENT`
- `PAID`
- `PAYMENT_FAILED`
- `PAYMENT_EXPIRED`
- `CANCELLED`

주문 상태 전이와 이력 관리는 domain 중심으로 통제합니다.

![주문 상태 다이어그램](./docs/status-diagram.svg)

## 테스트 전략

테스트는 `unit` / `integration` / `e2e`로 나눠 구성했습니다.

### Unit Test

- 서비스 분기 로직
- 상태 전이
- 이벤트 발행/처리 조건
- Redis 로그인 잠금

### Integration Test

- JPA persistence adapter
- Kafka Saga 서비스 조합
- Testcontainers 기반 PostgreSQL 검증

### E2E Test

`e2e-test` 모듈에서 gateway 기준 HTTP 시나리오를 검증합니다.

- 회원가입 / 로그인
- 상품 조회
- 주문 생성
- 결제 Saga 정상 흐름
- 결제 실패 및 재고 복구 흐름

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

프런트 same-origin 구조를 유지하기 위해 public ALB와 internal ALB를 분리했습니다.

자세한 내용은 아래 문서를 참고하면 됩니다.

- [aws-part1.md](./docs/blog/aws-part1.md)
- [aws-part2.md](./docs/blog/aws-part2.md)
- [aws-part3.md](./docs/blog/aws-part3.md)

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
├─ docker/
├─ infra/
└─ docs/
```

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

## 기술 스택

- Backend: `Java 21`, `Spring Boot 3.2`, `Spring Security`, `Spring Data JPA`, `Flyway`
- Messaging: `Kafka`
- Database: `PostgreSQL`
- Cache / NoSQL: `Redis`
- Infra: `Docker`, `AWS ECS Fargate`, `ALB`, `RDS`, `EC2`, `CloudWatch Logs`, `ECR`
- Test: `JUnit 5`, `Mockito`, `Testcontainers`

## 트러블슈팅 키워드

아래 이슈들을 직접 겪고 구조를 정리했습니다.

- `REQUIRES_NEW`와 테스트 트랜잭션 충돌
- Flyway validation failure
- public ALB 재프록시로 인한 `463` loop
- internal ALB 연결 중 `504`
- local seed 계정과 배포 환경 계정 전략 차이

## 남은 개선 포인트

- PG webhook 연동
- DLQ 후처리 도구
- 모니터링/알림 체계 보강
- CI/CD 자동화

## 메모

외부에 항상 배포 상태를 유지하기보다, 비용을 줄이기 위해 평소에는 인프라를 내려두고 필요 시 재기동하는 방식으로 운영했습니다.  
대신 배포 구조, 검증 화면, 트러블슈팅 내용을 문서와 블로그로 남겨 재현 가능성과 설명 가능성을 확보했습니다.
