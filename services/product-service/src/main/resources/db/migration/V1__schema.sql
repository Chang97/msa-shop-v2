-- =====================================================
-- product_db schema
-- 실행 대상: product_db (접속 유저: product_user 권장)
-- 참고: 기존 DDL에 updated_at/updated_by 등이 없어서 v2 표준이 필요하면 V2 마이그레이션에서 추가 권장
-- =====================================================

create table if not exists product (
    product_id bigserial primary key,
    product_name varchar(120) not null,
    price numeric(12,2) not null check (price >= 0),
    stock int not null check (stock >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by bigint,
    updated_by bigint,
    use_yn boolean NOT NULL DEFAULT true);

-- comments
COMMENT ON TABLE product IS '테이블: product';
COMMENT ON COLUMN product.product_id IS 'product id';
COMMENT ON COLUMN product.product_name IS 'product name';
COMMENT ON COLUMN product.price IS 'price';
COMMENT ON COLUMN product.stock IS 'stock';
COMMENT ON COLUMN product.created_at IS 'created at';
COMMENT ON COLUMN product.updated_at IS 'updated at';
COMMENT ON COLUMN product.created_by IS 'created by';
COMMENT ON COLUMN product.updated_by IS 'updated by';
COMMENT ON COLUMN product.use_yn IS 'use yn';
