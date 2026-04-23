# MSA Shop v2

주문, 재고, 결제가 분리된 환경에서 단일 트랜잭션 없이도 하나의 주문 흐름을 일관되게 끝낼 수 있는지 검증하기 위해 만든 MSA 프로젝트입니다.
이 프로젝트에서 보고 싶었던 것은 CRUD 구현 자체가 아니라 **실패가 정상인 분산 환경에서 주문 흐름을 어떻게 설계하고 검증할 것인가**입니다.

핵심은 세 가지입니다.

- 주문, 재고, 결제를 **Saga**로 연결해 상태 전이를 분산 처리
- **Outbox + Processed Event Claim + idempotencyKey**로 유실과 중복 소비 대응
- 결제 결과가 즉시 확정되지 않는 경우를 위해 **`APPROVAL_UNKNOWN` + reconciliation** 설계

구현에서 끝내지 않고 AWS에 직접 배포해, 실제 네트워크와 인프라 조건에서도 같은 흐름이 유지되는지 확인했습니다.

## 문제 정의

MSA에서는 서비스가 분리되는 순간 각 서비스가 자기 DB만 책임집니다.
주문 생성, 재고 차감, 결제 승인 같은 흐름을 하나의 로컬 트랜잭션으로 묶을 수 없습니다.

여기서 실제 문제는 기능이 아니라 정합성입니다.

- 주문은 생성됐는데 재고가 부족하면 어떻게 정리할 것인가
- 재고는 예약됐는데 결제가 실패하면 누가 어떤 기준으로 복구할 것인가
- 결제 결과가 timeout이나 네트워크 예외로 모호하면 실패로 단정해도 되는가
- Kafka 메시지가 중복 소비돼도 같은 결과를 보장할 수 있는가

즉 이 프로젝트의 핵심 질문은 **분리된 서비스들이 서로 다른 DB를 가진 상태에서, 하나의 주문 흐름을 어떻게 일관되게 완료할 것인가**입니다.

## 해결 전략

단일 트랜잭션 대신 **Saga + Outbox + Processed Event Claim** 구조를 선택했습니다.

- 상태 변경과 이벤트 적재는 같은 로컬 트랜잭션 안에서 처리합니다.
- relay가 outbox를 읽어 Kafka로 발행합니다.
- consumer는 `processed_event`를 먼저 claim해 중복 소비와 동시 처리 경쟁을 줄입니다.
- 결제 요청은 `idempotencyKey`를 기준으로 중복 PG 호출을 막습니다.

실제 결제 Saga 흐름은 다음과 같습니다.

1. `order-service`가 주문 상태를 `PENDING_PAYMENT`로 바꾸고 `STOCK_RESERVATION_REQUESTED`를 적재합니다.
2. `product-service`가 재고 예약을 수행하고, 성공 시 `STOCK_RESERVED`, 실패 시 `STOCK_RESERVATION_FAILED`를 발행합니다.
3. `payment-service`는 `STOCK_RESERVED`를 받은 경우에만 PG 호출을 진행합니다.
4. `payment-service`는 결제 결과에 따라 `PAYMENT_APPROVED`, `PAYMENT_FAILED`를 발행하거나, 결과가 모호하면 `APPROVAL_UNKNOWN`으로 보류합니다.
5. `order-service`와 `product-service`는 같은 결제 결과 이벤트를 각각 소비해 주문 상태 반영과 재고 확정/해제를 수행합니다.

![주문-재고-결제 Saga 흐름](./payment-sequence.svg)

_주문 상태 변경, 재고 예약, 결제 실행을 단일 트랜잭션 대신 Saga로 연결한 흐름_

## 핵심 설계 포인트

### 1. `APPROVAL_UNKNOWN`을 별도 상태로 둔 이유

결제는 승인과 실패만 있는 문제가 아닙니다.
PG timeout, 네트워크 단절, 응답 지연처럼 결과를 즉시 확정할 수 없는 경우가 존재합니다.

이 상태를 곧바로 실패로 처리하면 이미 승인된 결제를 잘못 보상할 수 있습니다.
그래서 `payment-service`는 이런 경우를 `APPROVAL_UNKNOWN`으로 저장하고, 현재 메시지는 처리 완료로 남긴 뒤 reconciliation이 나중에 PG 상태를 다시 조회해 최종 승인 또는 실패로 확정하도록 했습니다.

즉 **모르는 사실을 별도 상태로 남긴다**는 것이 중요한 설계 포인트입니다.

### 2. 재고는 결제 전에 차감하지 않고 먼저 예약

결제가 끝난 뒤 재고를 차감하면 트래픽이 몰리는 시간에 같은 재고를 여러 주문이 동시에 잡을 수 있습니다.
그래서 `product-service`는 예약 개념을 두고 다음 순서로 처리합니다.

- 결제 전: `RESERVED`
- 결제 승인: `CONFIRMED`
- 결제 실패: `RELEASED`
- 예약 만료: `EXPIRED`

이 구조 덕분에 결제 성공 전에는 재고를 일시 점유하되, 실패나 만료가 되면 원래 수량으로 복구할 수 있습니다.

### 3. 중복 메시지에 의존하지 않고 같은 결과를 보장

Kafka consumer의 중복 소비를 완전히 막을 수 없다고 보고 설계했습니다.
그래서 consumer가 먼저 `processed_event`를 claim하고, 이미 처리한 이벤트면 같은 작업을 다시 하지 않도록 했습니다.

또한 결제는 `idempotencyKey`를 기준으로 중복 요청을 막습니다.
이미 승인됐거나 실패한 결제는 PG를 다시 호출하지 않고 기존 결과를 재사용하도록 구성했습니다.

즉 이 프로젝트는 **exactly-once 전달을 전제하지 않고, at-least-once 전달 환경에서 멱등하게 동작하는 구조**를 목표로 했습니다.

### 4. 만료와 늦게 도착한 결과까지 상태 모델에 포함

분산 환경에서는 결제 결과가 항상 제때 도착하지 않습니다.
그래서 주문 상태에 `PAYMENT_EXPIRED`를 두고, 결제가 정해진 시간 안에 끝나지 않으면 만료로 전이되게 했습니다.

여기서 끝내지 않고, 만료 후 늦게 도착한 결과도 다시 반영하도록 설계했습니다.

- `PAYMENT_EXPIRED` 이후 늦게 도착한 승인 이벤트는 `PAID`로 반영
- `PAYMENT_EXPIRED` 이후 늦게 도착한 실패 이벤트는 `PAYMENT_FAILED`로 반영
- `PAYMENT_EXPIRED` 주문은 재결제를 허용해 다시 `PENDING_PAYMENT`로 진입 가능

즉 "만료"를 단순 종료 상태로 보지 않고 **늦은 이벤트와 재시도까지 고려한 상태 전이 모델**로 설계했습니다.

![주문 상태 전이](./status-diagram.svg)

_결제 진행 중 실패, 만료, 늦게 도착한 결과까지 포함한 주문 상태 전이_

## 실패 시나리오 대응

### 재고 부족

주문 생성 단계에서 재고를 미리 차감하지 않습니다.
재고 부족은 `/api/orders`가 아니라 `/api/orders/{id}/pay` 이후 재고 예약 단계에서 드러나도록 설계했습니다.

재고 예약에 실패하면 `product-service`가 `STOCK_RESERVATION_FAILED`를 발행합니다.
이 경우 `payment-service`는 결제를 시도하지 않고, `order-service`가 주문을 `PAYMENT_FAILED`로 정리합니다.

### 결제 실패

결제 실패 이벤트를 기준으로 주문은 `PAYMENT_FAILED`로 전이합니다.
`product-service`는 예약 재고를 해제해 수량을 원복합니다.

### 결제 결과 불명확

PG 호출 결과가 예외나 timeout으로 불명확하면 즉시 실패로 처리하지 않고 `APPROVAL_UNKNOWN`으로 남깁니다.
이후 reconciliation이 PG 상태를 재조회해 최종 승인 또는 실패를 반영합니다.

### 중복 메시지

`processed_event` claim과 `idempotencyKey`를 함께 사용해 같은 메시지 또는 같은 결제 요청이 다시 들어와도 결과가 달라지지 않도록 설계했습니다.

## 무엇을 검증했는가

이 프로젝트는 구조 설명으로 끝내지 않고, 테스트와 배포 환경에서 아래를 확인했습니다.

- 단위 테스트로 주문 상태 전이, 재고 예약/해제/확정, 결제 승인/실패/불명확 상태 반영을 검증했습니다.
- 통합 테스트로 Outbox 적재, `processed_event` 처리, 재고 부족 시 실패 반영, `PAYMENT_EXPIRED` 저장을 검증했습니다.
- reconciliation 테스트로 `APPROVAL_UNKNOWN`이 나중에 승인 또는 실패로 보정되는 흐름을 검증했습니다.
- E2E 테스트로 로그인, refresh/logout, 상품 권한, 주문 생성, 결제 성공 후 `PAID` 반영과 재고 감소를 확인했습니다.
- E2E 테스트로 PG 실패, 재고 부족, `APPROVAL_UNKNOWN -> reconciliation`, `PAYMENT_EXPIRED`, 만료 후 재결제, 결제 완료 주문 취소 불가를 확인했습니다.
- AWS 배포 환경에서 gateway health check, 로그인, 주문, 결제 API가 실제 인프라 위에서 동작하는지 확인했습니다.

즉 "설계했다"에서 끝나지 않고 **정상 흐름, 실패 흐름, 불명확한 상태, 만료, 재시도와 늦은 결과까지 코드와 배포 환경에서 확인했다**는 것이 이 프로젝트의 도달 결과입니다.

## 서비스별 역할

![전체 서비스 아키텍처](./system-diagram.svg)

_Gateway, 인증, 주문, 상품, 결제 서비스를 분리하고 Kafka, Redis, 각 DB를 역할에 맞게 배치한 구조_

- `gateway`
  - 모든 요청 진입점
  - JWT 검증, 내부 헤더 전파, rate limit 담당
- `auth-service`
  - 로그인, 토큰, 계정 잠금 담당
- `user-service`
  - 사용자 프로필 담당
- `product-service`
  - 상품 정보와 재고 예약/해제/확정 담당
- `order-service`
  - 주문 상태 관리, Saga 시작, 최종 결과 반영 담당
- `payment-service`
  - PG 호출, 결제 상태 저장, reconciliation 담당

## AWS 배포 / 트러블슈팅 기록

아래 글은 AWS 배포 구조와 문제 해결 과정을 정리한 기록입니다.

- AWS 배포 구조 정리: <https://chang97.tistory.com/162>
- ECS/Fargate 배포 시행착오 정리: <https://chang97.tistory.com/163>
- 프론트 연결 포함 최종 배포 구조: <https://chang97.tistory.com/164>
- 프로젝트 저장소: <https://github.com/Chang97/msa-shop-v2>

## 이 프로젝트가 보여주는 역량

- 서비스 경계를 나눈 뒤 상태 전이를 이벤트 흐름으로 설계하는 능력
- 실패, 중복 소비, 결제 불명확 상태, 만료와 늦은 결과까지 고려해 모델링하는 능력
- 구현에 그치지 않고 테스트와 AWS 배포 환경까지 이어서 검증하는 능력
