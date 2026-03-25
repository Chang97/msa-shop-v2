# MSA Shop v2

Spring Boot 3.2 기반 이커머스 MSA 학습/포트폴리오 프로젝트입니다.

이 프로젝트의 핵심 목적은 단순 CRUD 구현이 아니라, 주문/재고/결제/회원가입처럼 서비스 경계가 분리된 상황에서 다음 질문에 답할 수 있는 구조를 만드는 것입니다.

- 서비스 경계를 어디까지 나눌 것인가
- 분산 트랜잭션 없이 데이터 정합성을 어떻게 맞출 것인가
- 장애와 중복 소비를 어떻게 다룰 것인가
- 운영 시 실패 격리와 사후 복구를 어떻게 설계할 것인가

---

## 프로젝트 목적

이 프로젝트는 "MSA를 사용했다"보다 "왜 이렇게 설계했는지 설명 가능한 상태"를 목표로 진행했습니다.

직접 구현한 핵심 포인트:

- `Gateway`에서 인증을 처리하고 각 서비스는 자신의 비즈니스 책임에 집중하는 구조
- `Outbox / Inbox Claim` 기반 멀티 인스턴스 안전한 이벤트 처리
- 주문-재고-결제를 `Saga`로 풀어낸 구조
- `DLQ`, `Retry`, `APPROVAL_UNKNOWN`, `Reconciliation`까지 고려한 운영 지향 결제 흐름

---

## 전체 구조

![시스템 다이어그램](./docs/system-diagram.svg)

현재 서비스 구성:

- `gateway`
- `auth-service`
- `user-service`
- `product-service`
- `order-service`
- `payment-service`

기본 원칙:

- 각 서비스는 자신의 DB만 소유합니다
- 다른 서비스 DB를 직접 조회하지 않습니다
- 동기 HTTP는 최소화하고, 중요한 분산 흐름은 Kafka 이벤트로 연결합니다
- 데이터 정합성은 로컬 트랜잭션 + 이벤트 + 멱등성으로 맞춥니다

---

## 인증 / 보안

인증과 도메인 책임을 분리했습니다.

- `Gateway`에서 JWT를 검증합니다
- 검증 성공 후 `X-User-Id`, `X-Roles` 정보를 downstream 서비스로 전달합니다
- 각 서비스는 전달받은 사용자 컨텍스트 기준으로 비즈니스 로직을 수행합니다

이렇게 한 이유:

- JWT 검증 로직을 각 서비스에 중복하지 않기 위해
- 인증 책임을 경계층으로 모아 운영 복잡도를 줄이기 위해
- 도메인 서비스는 비즈니스 규칙에 집중하기 위해

---

## 도메인 흐름

### 주문 상태 모델

주문 aggregate는 아래 상태를 가집니다.

```text
CREATED
PENDING_PAYMENT
PAID
CANCELLED
```

상태 전이 규칙:

- `CREATED -> PENDING_PAYMENT`
- `PENDING_PAYMENT -> PAID`
- `CREATED / PENDING_PAYMENT -> CANCELLED`
- `PAID` 이후 취소 불가

상태 전이 규칙은 service가 아니라 domain에서 제어합니다.

![주문 상태 다이어그램](./docs/status-diagram.svg)

---

## 회원가입 Choreography Saga

`auth-service`와 `user-service` 사이 회원가입 흐름은 Kafka 기반 choreography saga로 구현했습니다.

흐름:

1. 사용자가 회원가입 요청
2. `auth-service`가 credential을 `enabled=false`로 저장
3. 같은 로컬 트랜잭션에서 `AuthUserCreated`를 outbox에 적재
4. relay가 Kafka로 이벤트 발행
5. `user-service`가 프로필 생성
6. 성공 시 `UserProfileCreated`, 실패 시 `UserProfileCreationFailed`를 outbox에 적재
7. `auth-service`가 결과 이벤트를 수신
8. 성공이면 `enabled=true`, 실패면 disabled 유지

보상 전략:

- 회원가입 실패 시 auth credential을 바로 삭제하지 않습니다
- 현재 보상은 `disabled 유지`입니다
- 즉, 데이터 롤백보다 상태 격리와 후속 복구를 우선했습니다

---

## 주문 / 재고 / 결제 Saga

현재 포트폴리오의 핵심 흐름은 주문-재고-결제 saga입니다.

### 설계 방향

결제 성공 뒤 재고 부족이 발생하면 보상 비용이 커집니다.  
그래서 이 프로젝트는 `결제 전에 재고를 예약`하는 방향을 선택했습니다.

흐름:

1. 사용자가 `order-service`에 결제 시작 요청
2. `order-service`가 주문을 `PENDING_PAYMENT`로 전이하고 `StockReservationRequested` 발행
3. `product-service`가 재고 예약
4. 성공 시 `StockReserved`, 실패 시 `StockReservationFailed`
5. `payment-service`는 `StockReserved`를 받은 뒤에만 PG 호출
6. PG 성공 시 `PaymentApproved`
7. PG 명시 실패 시 `PaymentFailed`
8. `product-service`는 승인 시 예약 확정, 실패 시 예약 해제
9. `order-service`는 승인 시 `PAID`, 실패 시 상태 유지 + history 기록

![결제 시퀀스 다이어그램](./docs/payment-sequence.svg)

### 재고 전략

`product-service`가 예약 재고의 진실 소스입니다.

- 예약 시 실제 `stock`을 먼저 감소
- 결제 성공 시 상태만 `CONFIRMED`
- 결제 실패 시 예약 해제 + `stock` 복구

이 구조로 `결제 성공 후 재고 부족` 시나리오를 앞단에서 차단합니다.

### 주문 실패 상태를 따로 두지 않은 이유

현재는 `PaymentFailed`, `StockReservationFailed`가 와도 주문 상태를 별도 실패 상태로 바꾸지 않고 `PENDING_PAYMENT`를 유지합니다.

대신:

- 상태 history에 실패 사유를 남기고
- 사용자가 다시 결제를 시도하거나
- 주문을 취소할 수 있도록 열어둡니다

즉, 이 단계에서는 상태 모델 단순화와 재시도 가능성을 우선했습니다.

### 결제 상태 전략

`payment-service`는 아래 상태를 사용합니다.

- `REQUESTED`
- `APPROVAL_UNKNOWN`
- `APPROVED`
- `FAILED`

핵심은 `APPROVAL_UNKNOWN`입니다.

- PG 호출 전에 먼저 `REQUESTED`를 저장합니다
- PG timeout, 네트워크 오류처럼 결과를 확정할 수 없으면 `APPROVAL_UNKNOWN`으로 남깁니다
- 이 경우 즉시 실패 이벤트를 발행하지 않습니다
- 이후 `Reconciliation`이 PG 상태를 재조회해 최종 결과를 확정합니다

즉, 이 구조는 "PG는 성공했는데 우리 서비스가 직후 죽는 경우"를 복구하기 위한 설계입니다.

---

## 장애 대응 전략

### 1. Outbox Claim

`outbox_event`는 단순 적재 테이블이 아니라 발행 제어 테이블입니다.

- 상태: `PENDING`, `PROCESSING`, `PUBLISHED`, `FAILED`
- relay는 `FOR UPDATE SKIP LOCKED`로 row를 점유합니다
- `locked_by`, `locked_at`으로 현재 worker를 추적합니다
- stale claim takeover를 지원합니다

효과:

- 멀티 인스턴스 환경에서 중복 발행 위험을 줄임
- 한 worker가 멈춰도 다른 worker가 후속 처리 가능

### 2. Inbox Claim / Deduplication

`processed_event`는 단순 처리 이력 테이블이 아니라 consumer claim 테이블 역할도 합니다.

- 같은 이벤트를 여러 pod가 동시에 받아도 한 worker만 claim 성공
- 나머지는 조용히 ack 가능
- stale claim takeover 고려

### 3. Retry / Failed Isolation

outbox publish 실패 시:

- `retry_count`
- `next_retry_at`
- 최대 재시도 초과 시 `FAILED`

즉, 무한 즉시 재시도를 피하고 운영자가 확인 가능한 실패 상태를 남깁니다.

### 4. DLQ

비즈니스 실패와 메시지 자체 오류를 구분합니다.

- 비즈니스 실패: saga 실패 이벤트로 처리
- 메시지 파손 / payload 역직렬화 실패: DLQ로 격리

### 5. APPROVAL_UNKNOWN + Reconciliation

PG 결과가 애매한 구간은 즉시 실패 처리하지 않습니다.

- 원본 이벤트는 `processed` 처리
- 결제 row는 `APPROVAL_UNKNOWN`으로 저장
- reconciliation이 나중에 `APPROVED` 또는 `FAILED`로 확정

즉, "원본 이벤트 처리 완료"와 "비즈니스 최종 상태 확정"을 분리했습니다.

---

## 테스트 전략

현재 테스트는 설명력이 높은 흐름 위주로 구성했습니다.

### 단위 테스트

- 서비스 분기 로직
- 상태 전이
- outbox append 여부
- DLQ / ack 처리

예:

- `PayOrderServiceTest`
- `OrderPaymentRequestedSagaServiceTest`
- `OrderPaymentSagaServiceTest`
- `PaymentResultSagaServiceTest`
- `PaymentReconciliationServiceTest`

### 영속성 테스트

- `StockReservationPersistenceAdapterTest`
- `PaymentQueryPersistenceAdapterTest`

목적:

- mock만으로는 확인하기 어려운 저장/조회/상태 반영 검증

### 자동 E2E 테스트

별도 모듈 [`e2e-test`](./e2e-test)을 두고 gateway 기준 HTTP E2E를 검증합니다.

현재 검증하는 축:

- 인증/로그인 흐름
- 회원가입 choreography saga
- 상품 조회/상품 생성 권한
- 주문 생성
- 주문/결제 saga 정상 흐름
- 주문/결제 saga 실패 흐름

주요 시나리오:

- `AuthRegisterSagaE2ETest`
- `ProductE2ETest`
- `OrderE2ETest`
- `PaymentE2ETest`

`PaymentE2ETest`에서는 현재 saga 기준으로 아래를 자동 검증합니다.

- 정상 승인 -> 주문 `PAID`, 재고 감소
- PG 명시 실패 -> 주문 `PENDING_PAYMENT`, 재고 복구
- 결제 완료 주문 취소 불가 -> `409`

### 현재 자동화하지 않은 부분

아직 Kafka + PostgreSQL + 전체 서비스를 띄운 상태에서 Testcontainers 기반 cross-service 테스트까지는 넣지 않았습니다.

이유:

- 현재는 실제 로컬 Docker 스택을 띄운 뒤 `e2e-test`를 실행하는 방식으로도 충분히 시나리오 검증이 가능함
- 현 단계에서는 단위 테스트 + 영속성 테스트 + E2E 모듈 조합이 비용 대비 효율이 높다고 판단했습니다

---

## 실행 환경

현재 실행 방식은 두 가지를 지원합니다.

### 1. 로컬 개발 모드

인프라만 Docker로 띄우고, 각 서비스는 로컬 JVM에서 실행합니다.

인프라 실행:

```bash
docker compose -f infra/docker-compose.yml up -d
```

포함 인프라:

- PostgreSQL: `localhost:15432`
- Redis: `localhost:16379`
- Kafka: `localhost:19092`

서비스 실행 예시:

```bash
./gradlew :gateway:bootRun --args='--spring.profiles.active=local'
./gradlew :auth-service:bootRun --args='--spring.profiles.active=local,saga-e2e'
./gradlew :user-service:bootRun --args='--spring.profiles.active=local,saga-e2e'
./gradlew :product-service:bootRun --args='--spring.profiles.active=local,saga-e2e'
./gradlew :order-service:bootRun --args='--spring.profiles.active=local,saga-e2e'
./gradlew :payment-service:bootRun --args='--spring.profiles.active=local,saga-e2e'
```

`saga-e2e` 프로파일에서 활성화되는 항목:

- Kafka consumer
- outbox relay
- Kafka bootstrap server `localhost:19092`
- 과도한 SQL/Kafka 로그 축소

### 2. 전체 Docker 모드

인프라와 애플리케이션을 모두 Docker Compose로 실행합니다.

실행:

```bash
docker compose -f infra/docker-compose.full.yml up --build -d
```

구성:

- `postgres`
- `redis`
- `kafka`
- `gateway`
- `auth-service`
- `user-service`
- `product-service`
- `order-service`
- `payment-service`

도커 실행 시점 특징:

- 각 서비스는 `local,saga-e2e,docker` 프로파일 조합으로 실행됩니다
- `application-docker.yml`에서 `localhost` 대신 서비스명 기반 주소를 사용합니다
- Kafka는 내부 리스너 `kafka:29092`, 외부 리스너 `localhost:19092`를 함께 사용합니다
- 이 구조는 이후 ECS/ECR 배포 시 이미지 재사용이 가능하도록 generic Dockerfile 기반으로 맞췄습니다

도커 설정 검증:

```bash
docker compose -f infra/docker-compose.full.yml config
./gradlew :gateway:bootJar :auth-service:bootJar :user-service:bootJar :product-service:bootJar :order-service:bootJar :payment-service:bootJar
```

---

## E2E 실행 방법

전체 Docker 스택이 떠 있는 상태에서 `e2e-test` 모듈만 실행하면 gateway 기준 실제 HTTP 시나리오를 검증할 수 있습니다.

전체 실행:

```bash
./gradlew :e2e-test:test
```

특정 saga 테스트만 실행:

```bash
./gradlew :e2e-test:test --tests "com.msashop.e2e.PaymentE2ETest"
./gradlew :e2e-test:test --tests "com.msashop.e2e.AuthRegisterSagaE2ETest"
```

즉, 현재는

- 수동 테스트도 가능하고
- 로컬 Docker 스택 위에서 자동 E2E도 가능한 상태입니다

---

## 수동 E2E 검증 시나리오

포트폴리오 설명용으로 아래 3개 시나리오를 기준으로 검증합니다.

### 1. 정상 승인

- 주문 결제 시작
- `order-service`: `PENDING_PAYMENT`
- `product-service`: 예약 성공
- `payment-service`: `APPROVED`
- `order-service`: `PAID`
- `product-service`: reservation `CONFIRMED`

### 2. 재고 부족

- 주문 결제 시작
- `product-service`: `StockReservationFailed`
- `order-service`: `PENDING_PAYMENT` 유지 + history 기록
- `payment-service`: 결제 row 생성 없음

### 3. PG 미확정 후 reconciliation

- 주문 결제 시작
- `payment-service`: `APPROVAL_UNKNOWN`
- 즉시 최종 이벤트 없음
- reconciliation 실행
- 이후 `APPROVED` 또는 `FAILED`로 확정

참고:

- 현재 fake PG는 정상 승인/명시 실패는 바로 재현 가능
- `APPROVAL_UNKNOWN`을 수동으로 완전히 재현하려면 fake PG에 예외 분기를 하나 더 연결해야 합니다

---

## 현재 한계

의도적으로 아직 비워둔 부분도 있습니다.

- PG webhook 미구현
- DLQ 재처리 도구 미구현
- `APPROVAL_UNKNOWN` 수동 재현용 fake PG 트리거 미연결
- AWS 배포 / CI/CD 미구현
- 관측성(Tracing, Metrics, Alerting) 보강 필요

즉, 현재는 "운영을 고려한 백엔드 구조"와 "로컬 실행/E2E 검증"까지 구현했고, 다음 단계는 실제 배포와 운영 자동화입니다.

---

## 다음 로드맵

### 필수

- README 보강 마감
- AWS 배포
- CI/CD 구축

### 선택

- PG webhook
- DLQ 재처리 도구
- Prometheus / Grafana / 로그 추적
- 운영 알림

### 인프라 방향

- `ECR`
- `RDS PostgreSQL`
- `ECS/Fargate`
- `ALB`
- `Jenkins` 또는 `GitHub Actions`

현재 기준에서는 EKS보다 ECS/Fargate 쪽이 앱 완성도와 운영 설명력을 같이 가져가기 좋은 방향이라고 판단했습니다.

---

## 정리

이 프로젝트는 아래 한 문장으로 설명하는 것이 가장 정확합니다.

> 주문/재고/결제/회원가입 흐름을 서비스 경계에 맞게 분리하고,  
> saga + outbox/inbox + 멱등성 + reconciliation으로 운영 가능한 분산 트랜잭션 구조를 만든 이커머스 백엔드 포트폴리오 프로젝트
