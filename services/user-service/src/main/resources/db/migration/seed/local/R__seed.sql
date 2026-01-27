-- =====================================================
-- auth-service seed data (local/dev)
-- =====================================================

-- roles
INSERT INTO role (role_name, use_yn) VALUES
                                         ('ROLE_ADMIN', true),
                                         ('ROLE_USER', true)
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

-- admin user (password: 1234)
INSERT INTO users (email, login_id, user_password, user_name, use_yn)
VALUES ('admin@msashop.local', 'admin', 'argon2Hash=$argon2id$v=19$m=16384,t=2,p=1$Utz5QoW2IhbCH4INZ/4uQA$tTIoEWh9GaDAGBjvg7Iuh84XEZqfR6zB4llyT/mW3Jw', '관리자', true)
    ON CONFLICT (email) DO NOTHING;

-- admin role mapping
INSERT INTO user_role_map (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
         JOIN role r ON r.role_name = 'ROLE_ADMIN'
WHERE u.email = 'admin@msashop.local'
    ON CONFLICT DO NOTHING;
