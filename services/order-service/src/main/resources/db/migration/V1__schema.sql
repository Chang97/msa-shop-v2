-- =====================================================
-- order_db schema
-- 실행 대상: order_db (접속 유저: order_user 권장)
-- =====================================================

-- 주문 마스터
CREATE TABLE orders (
  id BIGSERIAL PRIMARY KEY,
  order_number      VARCHAR(32) NOT NULL UNIQUE,            -- 예: ORD-20251115-000123
  user_id           BIGINT NOT NULL,                        -- 외부(Auth) 참조용 숫자
  status            VARCHAR(20) NOT NULL,                   -- CREATED, PENDING_PAYMENT, PAID, CANCELLED, FULFILLED
  currency          CHAR(3) NOT NULL DEFAULT 'KRW',
  subtotal_amount   NUMERIC(18,2) NOT NULL,
  discount_amount   NUMERIC(18,2) NOT NULL DEFAULT 0,
  shipping_fee      NUMERIC(18,2) NOT NULL DEFAULT 0,
  total_amount      NUMERIC(18,2) NOT NULL,
  receiver_name     VARCHAR(80) NOT NULL,
  receiver_phone    VARCHAR(32) NOT NULL,
  shipping_postcode VARCHAR(10) NOT NULL,
  shipping_address1 VARCHAR(255) NOT NULL,
  shipping_address2 VARCHAR(255),
  memo              VARCHAR(255),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by bigint,
  updated_by bigint,
  CONSTRAINT chk_amounts_nonneg CHECK (
    subtotal_amount >= 0 AND discount_amount >= 0 AND shipping_fee >= 0 AND total_amount >= 0
  ));

CREATE INDEX idx_orders_user_created   ON orders (user_id, created_at DESC);
CREATE INDEX idx_orders_status_created ON orders (status, created_at DESC);

-- 주문 품목
CREATE TABLE order_item (
  id BIGSERIAL PRIMARY KEY,
  order_id     BIGINT NOT NULL,
  product_id   BIGINT NOT NULL,                  -- 외부(Product) 참조용 숫자
  product_name VARCHAR(200) NOT NULL,            -- 주문 시점 스냅샷
  unit_price   NUMERIC(18,2) NOT NULL,
  qty          INTEGER NOT NULL CHECK (qty > 0),
  line_amount  NUMERIC(18,2) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by bigint,
  updated_by bigint,
  CONSTRAINT fk_order_item_order
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE);

CREATE INDEX idx_order_item_order   ON order_item (order_id);
CREATE INDEX idx_order_item_product ON order_item (product_id);

-- 상태 변경 이력
CREATE TABLE order_status_history (
  id BIGSERIAL PRIMARY KEY,
  order_id    BIGINT NOT NULL,
  from_status VARCHAR(20),
  to_status   VARCHAR(20) NOT NULL,
  reason      VARCHAR(255),
  changed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  changed_by  BIGINT,                               -- 변경 주체(시스템/사용자),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by bigint,
  updated_by bigint,
  CONSTRAINT fk_status_hist_order
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE);

CREATE INDEX idx_status_hist_order ON order_status_history (order_id, changed_at);

-- Outbox (도메인 이벤트)
CREATE TABLE order_outbox (
  id BIGSERIAL PRIMARY KEY,
  aggregate_id BIGINT NOT NULL,                     -- orders.id
  event_type   VARCHAR(80) NOT NULL,                -- ORDER_CREATED, ...
  payload      JSONB NOT NULL,                      -- 이벤트 데이터
  status       VARCHAR(16) NOT NULL DEFAULT 'PENDING',  -- PENDING, PUBLISHED, FAILED
  created_at timestamptz NOT NULL DEFAULT now(),
  published_at TIMESTAMPTZ,
  updated_at timestamptz NOT NULL DEFAULT now(),
  created_by bigint,
  updated_by bigint);

CREATE INDEX idx_outbox_status_created ON order_outbox (status, created_at);

-- 주문번호 시퀀스/함수(선택)
CREATE SEQUENCE IF NOT EXISTS order_no_seq;
CREATE OR REPLACE FUNCTION next_order_number()
RETURNS varchar AS $$
BEGIN
  RETURN 'ORD-' || to_char(current_date,'YYYYMMDD') || '-' ||
         lpad(nextval('order_no_seq')::text,6,'0');
END;
$$ LANGUAGE plpgsql;


-- =========================
-- 주문 마스터 설명 추가
-- =========================
COMMENT ON TABLE orders IS '주문 마스터(헤더). 주문 기본정보와 금액 합계/수취인/주소를 보관';
COMMENT ON COLUMN orders.id                IS 'PK (BIGINT, IDENTITY)';
COMMENT ON COLUMN orders.order_number      IS '주문번호(노출용): 예) ORD-YYYYMMDD-000123. 유니크 보장';
COMMENT ON COLUMN orders.user_id           IS '주문자 ID (Auth 서비스의 사용자 식별자, FK 미설정-서비스 독립성 유지)';
COMMENT ON COLUMN orders.status            IS '주문 상태: CREATED, PENDING_PAYMENT, PAID, CANCELLED, FULFILLED';
COMMENT ON COLUMN orders.currency          IS '통화 코드(ISO 4217), 기본 KRW';
COMMENT ON COLUMN orders.subtotal_amount   IS '상품 합계(아이템 금액 총합)';
COMMENT ON COLUMN orders.discount_amount   IS '할인 금액(쿠폰/프로모션 등)';
COMMENT ON COLUMN orders.shipping_fee      IS '배송비';
COMMENT ON COLUMN orders.total_amount      IS '최종 결제 금액 = subtotal - discount + shipping';
COMMENT ON COLUMN orders.receiver_name     IS '수취인 이름';
COMMENT ON COLUMN orders.receiver_phone    IS '수취인 연락처';
COMMENT ON COLUMN orders.shipping_postcode IS '배송지 우편번호';
COMMENT ON COLUMN orders.shipping_address1 IS '배송지 기본주소';
COMMENT ON COLUMN orders.shipping_address2 IS '배송지 상세주소(선택)';
COMMENT ON COLUMN orders.memo              IS '주문 메모(요청사항 등)';
COMMENT ON COLUMN orders.created_at        IS '행 생성 시각(UTC 권장, timestamptz)';
COMMENT ON COLUMN orders.updated_at        IS '행 수정 시각(트리거 혹은 애플리케이션에서 갱신)';

-- 인덱스 설명(선택)
COMMENT ON INDEX idx_orders_user_created   IS '사용자별 주문 최신순 조회';
COMMENT ON INDEX idx_orders_status_created IS '상태별 주문 최신순 조회';

-- =========================
-- 주문 품목 설명 추가
-- =========================
COMMENT ON TABLE order_item IS '주문 품목(라인 아이템). 주문 시점의 상품 스냅샷을 보관';
COMMENT ON COLUMN order_item.id           IS 'PK (BIGINT, IDENTITY)';
COMMENT ON COLUMN order_item.order_id     IS '주문 마스터 FK (orders.id), ON DELETE CASCADE';
COMMENT ON COLUMN order_item.product_id   IS '상품 ID (Product 서비스 식별자, FK 미설정-스냅샷 전략)';
COMMENT ON COLUMN order_item.product_name IS '상품명 스냅샷(주문 시점 명칭)';
COMMENT ON COLUMN order_item.unit_price   IS '주문 시점 단가(스냅샷)';
COMMENT ON COLUMN order_item.qty          IS '수량(>0)';
COMMENT ON COLUMN order_item.line_amount  IS '라인 금액 = unit_price * qty';
COMMENT ON COLUMN order_item.created_at   IS '행 생성 시각';

COMMENT ON INDEX idx_order_item_order   IS '주문별 품목 조회';
COMMENT ON INDEX idx_order_item_product IS '상품 ID 기준 검색(리포트/통계용)';

-- =========================
-- 주문 상태 변경 이력 설명 추가
-- =========================
COMMENT ON TABLE order_status_history IS '주문 상태 전이 이력(감사/복구용)';
COMMENT ON COLUMN order_status_history.id          IS 'PK (BIGINT, IDENTITY)';
COMMENT ON COLUMN order_status_history.order_id    IS '주문 마스터 FK (orders.id), ON DELETE CASCADE';
COMMENT ON COLUMN order_status_history.from_status IS '이전 상태(처음 생성 시 NULL 가능)';
COMMENT ON COLUMN order_status_history.to_status   IS '변경된 상태';
COMMENT ON COLUMN order_status_history.reason      IS '변경 사유(사용자/시스템 사유)';
COMMENT ON COLUMN order_status_history.changed_at  IS '변경 시각';
COMMENT ON COLUMN order_status_history.changed_by  IS '변경 주체 ID(시스템/사용자, 선택)';

COMMENT ON INDEX idx_status_hist_order IS '주문별 상태 이력 시간순 조회';

-- =========================
-- Outbox(도메인 이벤트) 설명 추가
-- =========================
COMMENT ON TABLE order_outbox IS '도메인 이벤트 Outbox. 트랜잭션 내 기록 후 별도 퍼블리셔가 전송';
COMMENT ON COLUMN order_outbox.id           IS 'PK (BIGINT, IDENTITY)';
COMMENT ON COLUMN order_outbox.aggregate_id IS '애그리거트 ID (orders.id)';
COMMENT ON COLUMN order_outbox.event_type   IS '이벤트 타입 예) ORDER_CREATED, ORDER_PAID';
COMMENT ON COLUMN order_outbox.payload      IS '이벤트 페이로드(JSONB). 외부 시스템 전달 데이터';
COMMENT ON COLUMN order_outbox.status       IS '상태: PENDING(대기), PUBLISHED(전송 완료), FAILED(전송 실패)';
COMMENT ON COLUMN order_outbox.created_at   IS '기록 시각';
COMMENT ON COLUMN order_outbox.published_at IS '전송 완료 시각(성공 시 세팅)';

COMMENT ON INDEX idx_outbox_status_created IS 'Outbox 처리 배치용: 상태+생성시각';

-- =========================
-- 시퀀스/함수 설명 추가(주문번호)
-- =========================
COMMENT ON SEQUENCE order_no_seq IS '주문번호 증가용 시퀀스(ORD-YYYYMMDD-###### 생성 함수에 사용)';

COMMENT ON FUNCTION next_order_number() IS '주문번호 생성 함수. 형식: ORD-YYYYMMDD-######';

-- comments
COMMENT ON COLUMN orders.id IS 'id';
COMMENT ON COLUMN orders.order_number IS 'order number';
COMMENT ON COLUMN orders.user_id IS 'user id';
COMMENT ON COLUMN orders.status IS 'status';
COMMENT ON COLUMN orders.currency IS 'currency';
COMMENT ON COLUMN orders.subtotal_amount IS 'subtotal amount';
COMMENT ON COLUMN orders.discount_amount IS 'discount amount';
COMMENT ON COLUMN orders.shipping_fee IS 'shipping fee';
COMMENT ON COLUMN orders.total_amount IS 'total amount';
COMMENT ON COLUMN orders.receiver_name IS 'receiver name';
COMMENT ON COLUMN orders.receiver_phone IS 'receiver phone';
COMMENT ON COLUMN orders.shipping_postcode IS 'shipping postcode';
COMMENT ON COLUMN orders.shipping_address1 IS 'shipping address1';
COMMENT ON COLUMN orders.shipping_address2 IS 'shipping address2';
COMMENT ON COLUMN orders.memo IS 'memo';
COMMENT ON COLUMN orders.created_at IS 'created at';
COMMENT ON COLUMN orders.updated_at IS 'updated at';
COMMENT ON COLUMN orders.created_by IS 'created by';
COMMENT ON COLUMN orders.updated_by IS 'updated by';
COMMENT ON COLUMN orders.subtotal_amount IS 'subtotal amount';

COMMENT ON COLUMN order_item.id IS 'id';
COMMENT ON COLUMN order_item.order_id IS 'order id';
COMMENT ON COLUMN order_item.product_id IS 'product id';
COMMENT ON COLUMN order_item.product_name IS 'product name';
COMMENT ON COLUMN order_item.unit_price IS 'unit price';
COMMENT ON COLUMN order_item.qty IS 'qty';
COMMENT ON COLUMN order_item.line_amount IS 'line amount';
COMMENT ON COLUMN order_item.created_at IS 'created at';
COMMENT ON COLUMN order_item.updated_at IS 'updated at';
COMMENT ON COLUMN order_item.created_by IS 'created by';
COMMENT ON COLUMN order_item.updated_by IS 'updated by';
COMMENT ON COLUMN order_status_history.id IS 'id';
COMMENT ON COLUMN order_status_history.order_id IS 'order id';
COMMENT ON COLUMN order_status_history.from_status IS 'from status';
COMMENT ON COLUMN order_status_history.to_status IS 'to status';
COMMENT ON COLUMN order_status_history.reason IS 'reason';
COMMENT ON COLUMN order_status_history.changed_at IS 'changed at';
COMMENT ON COLUMN order_status_history.changed_by IS 'changed by';
COMMENT ON COLUMN order_status_history.created_at IS 'created at';
COMMENT ON COLUMN order_status_history.updated_at IS 'updated at';
COMMENT ON COLUMN order_status_history.created_by IS 'created by';
COMMENT ON COLUMN order_status_history.updated_by IS 'updated by';
COMMENT ON COLUMN order_outbox.id IS 'id';
COMMENT ON COLUMN order_outbox.aggregate_id IS 'aggregate id';
COMMENT ON COLUMN order_outbox.event_type IS 'event type';
COMMENT ON COLUMN order_outbox.payload IS 'payload';
COMMENT ON COLUMN order_outbox.status IS 'status';
COMMENT ON COLUMN order_outbox.created_at IS 'created at';
COMMENT ON COLUMN order_outbox.published_at IS 'published at';
COMMENT ON COLUMN order_outbox.updated_at IS 'updated at';
COMMENT ON COLUMN order_outbox.created_by IS 'created by';
COMMENT ON COLUMN order_outbox.updated_by IS 'updated by';
