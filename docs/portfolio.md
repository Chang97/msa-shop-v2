# 포트폴리오 프로젝트 문구

## 1. 카드형 요약

### 프로젝트명

`MSA Shop`

### 한 줄 소개

Kafka Saga와 Outbox/Inbox Claim 패턴으로 주문-재고-결제 흐름을 구현한 이커머스 백엔드 MSA 프로젝트

### 짧은 설명

단순 CRUD 대신 주문, 재고, 결제가 함께 얽히는 흐름을 서비스 분리 환경에서 어떻게 처리할지 고민한 프로젝트입니다.  
Spring Boot 기반 서비스를 Kafka, PostgreSQL, Redis, AWS ECS Fargate 환경 위에서 직접 설계하고 배포했습니다.

### 기술 스택

`Java 21` `Spring Boot` `Kafka` `PostgreSQL` `Redis` `AWS ECS Fargate` `ALB` `RDS`

### 링크 영역 예시

- GitHub
- 블로그 1편: AWS 구조 정리
- 블로그 2편: AWS 배포 / 트러블슈팅

## 2. 상세형 소개

### 프로젝트 소개

주문, 재고, 결제처럼 여러 서비스의 상태가 함께 바뀌는 문제를 다루기 위해 만든 이커머스 백엔드 MSA 프로젝트입니다.  
서비스는 `gateway`, `auth-service`, `user-service`, `product-service`, `order-service`, `payment-service`로 분리했고, 긴 비즈니스 흐름은 Kafka 기반 Saga로 연결했습니다.

### 해결하려고 한 문제

- 주문, 재고, 결제를 하나의 DB 트랜잭션처럼 묶을 수 없는 환경에서 일관성을 어떻게 유지할지
- 단순 동기 HTTP 호출만으로는 실패 지점과 보상 흐름이 애매한 문제를 어떻게 풀지
- 이벤트 기반 구조에서 중복 소비, 재처리, 멀티 인스턴스 안정성을 어떻게 확보할지

### 내가 한 일

- 서비스 경계를 나누고 `gateway / auth / user / product / order / payment` 구조 설계
- 주문-재고-결제 흐름을 Kafka Saga 기반으로 구현
- Outbox / Inbox Claim 패턴으로 멀티 인스턴스 안전한 이벤트 처리 구조 구현
- DLQ, Retry, `APPROVAL_UNKNOWN`, reconciliation 흐름 반영
- payment-service에서 `REQUESTED 저장 -> PG 호출(트랜잭션 밖) -> 최종 상태 + outbox 적재` 흐름으로 트랜잭션 경계 정리
- AWS ECS Fargate, ALB, RDS, Kafka, Redis 환경에 직접 배포
- private subnet, NAT Gateway, Service Connect, health check, CloudWatch Logs 이슈 해결
- AWS 전용 프로필(`application-aws.yml`) 분리 및 불필요한 직접 호출 경로 정리

### 핵심 기술 포인트

- Kafka Saga 기반 분산 트랜잭션 처리
- Outbox / Inbox Claim 기반 이벤트 멱등성 보장
- 로컬 상태 변경과 outbox 적재를 같은 로컬 트랜잭션 안에서 처리하도록 정리
- private subnet + NAT Gateway 구조로 ECS Fargate 배포
- Service Connect 기반 내부 서비스 이름 통신
- CloudWatch Logs, health check, 환경변수 정리까지 포함한 배포 트러블슈팅

### 결과

- 로컬뿐 아니라 AWS 배포 환경에서도 핵심 API 흐름 검증
- `/health`, 상품 조회, 로그인, 주문/결제 흐름까지 실제 동작 확인
- 구조 설계, 이벤트 처리, 배포, 트러블슈팅을 하나의 프로젝트 안에서 설명 가능한 포트폴리오로 정리

## 3. 포트폴리오 소개 문구 후보

### 버전 A

Kafka Saga와 Outbox/Inbox Claim 패턴을 적용해 주문-재고-결제 흐름을 분산 트랜잭션으로 풀어낸 이커머스 백엔드 MSA 프로젝트입니다.  
Spring Boot 기반 서비스를 AWS ECS Fargate, ALB, RDS, Kafka, Redis 환경에 직접 배포하고, 로컬 트랜잭션과 outbox 경계, 네트워크, 로그, 헬스체크 문제까지 정리했습니다.

### 버전 B

서비스를 단순히 쪼개는 데 그치지 않고, 주문-재고-결제 흐름을 Kafka Saga로 풀어낸 이커머스 백엔드 프로젝트입니다.  
Outbox / Inbox Claim, DLQ, reconciliation을 적용해 이벤트 처리 구조를 설계했고, 이를 AWS ECS Fargate 환경까지 직접 배포했습니다.

## 4. 이력서용 짧은 요약

### 버전 A

Spring Boot 기반 이커머스 MSA 프로젝트를 구현하고, 주문-재고-결제 흐름을 Kafka Saga와 Outbox/Inbox Claim 패턴으로 설계했습니다.  
AWS ECS Fargate, ALB, RDS, Kafka, Redis 환경에 직접 배포하며 private subnet, NAT Gateway, Service Connect, health check 이슈와 payment-service 트랜잭션 경계를 정리했습니다.

### 버전 B

주문-재고-결제 도메인을 Kafka Saga로 설계하고, Outbox/Inbox Claim 기반 이벤트 처리 구조를 구현한 이커머스 백엔드 프로젝트입니다.  
AWS ECS Fargate 환경에 직접 배포하며 구조 설계부터 트러블슈팅까지 전 과정을 정리했습니다.

## 5. 포트폴리오에 넣을 핵심 키워드

- Kafka Saga
- Outbox / Inbox Claim
- 로컬 트랜잭션 / outbox 경계
- 분산 트랜잭션
- 멱등성
- DLQ / Retry / Reconciliation
- AWS ECS Fargate
- ALB / RDS / NAT Gateway / Service Connect

## 6. Kubernetes 요구사항 대응 문구

### 짧은 버전

Kubernetes 운영 경험은 없지만, ECS Fargate 환경에서 컨테이너 기반 서비스 배포와 네트워크 구성을 직접 다뤘습니다.  
ALB, private subnet, NAT Gateway, health check, 로그 수집, 서비스 디스커버리(Service Connect)까지 직접 구성하고 문제를 해결한 경험이 있어 Kubernetes 개념에도 빠르게 전이할 수 있다고 생각합니다.

### 면접형 버전

실무 Kubernetes 운영 경험은 아직 없습니다.  
다만 AWS ECS Fargate 환경에서 컨테이너 서비스를 private subnet에 배치하고, ALB, NAT Gateway, Service Connect, CloudWatch Logs, health check 문제를 직접 다뤘습니다.  
즉 컨테이너 애플리케이션 배포에서 발생하는 네트워크, 서비스 디스커버리, 헬스체크, 로그 수집 문제를 실제로 한 번 풀어본 경험이 있고, 이를 바탕으로 Kubernetes도 빠르게 연결해 학습할 수 있습니다.
