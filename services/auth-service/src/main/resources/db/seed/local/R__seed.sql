-- =====================================================
-- auth-service seed data (local/dev)
-- =====================================================

-- roles
INSERT INTO role (role_name, use_yn) VALUES
('ADMIN', true),
('USER', true)
ON CONFLICT (role_name) DO NOTHING;

-- permissions (필요 최소)
INSERT INTO permission (permission_code, permission_name, use_yn) VALUES
('PRODUCT_READ', '상품 조회', true),
('PRODUCT_UPDATE', '상품 수정', true),
('ORDER_CREATE', '주문 생성', true),
('ORDER_READ', '주문 조회', true),
('PAYMENT_APPROVE', '결제 승인', true)
ON CONFLICT (permission_code) DO NOTHING;

-- role-permission mapping (ADMIN = all, USER = read/create)
-- ADMIN
INSERT INTO role_permission_map (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
JOIN permission p ON 1=1
WHERE r.role_name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- USER (subset)
INSERT INTO role_permission_map (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
JOIN permission p ON p.permission_code IN ('PRODUCT_READ','ORDER_CREATE','ORDER_READ')
WHERE r.role_name = 'USER'
ON CONFLICT DO NOTHING;

-- admin user (password: admin1234!)
-- user_password is BCrypt hash
INSERT INTO users (email, login_id, user_password, user_name, use_yn)
VALUES ('admin@msashop.local', 'admin', '$2b$10$Sj0qxmNEDrFK2FrhiUV3A.lWE.cwPpPj7Ps/0bHbP9owORbBfKS3q', '관리자', true)
ON CONFLICT (email) DO NOTHING;

-- admin role mapping
INSERT INTO user_role_map (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN role r ON r.role_name = 'ADMIN'
WHERE u.email = 'admin@msashop.local'
ON CONFLICT DO NOTHING;
