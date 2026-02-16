-- =====================================================
-- product-service seed data (local/dev)
-- =====================================================

INSERT INTO product(product_name, price, stock, use_yn)
VALUES
('Keyboard 87', 89000, 12, true),
('Mouse Silent', 32000, 45, true),
('Monitor 27"', 289000, 7, true)
;

-- E2E 전용 비활성/판매중단 상품 시드
INSERT INTO product(product_name, price, stock, status, use_yn)
VALUES
('E2E Disabled Product', 10000, 5, 'ON_SALE', false),
('E2E Stopped Product', 10000, 5, 'STOPPED', true);
