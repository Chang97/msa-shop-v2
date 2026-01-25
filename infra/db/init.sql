-- =====================================================
-- 1) 서비스별 유저 생성
-- =====================================================
CREATE USER auth_user WITH PASSWORD 'auth_pw';
CREATE USER auth_test_user WITH PASSWORD 'auth_test_pw';
CREATE USER product_user WITH PASSWORD 'product_pw';
CREATE USER order_user WITH PASSWORD 'order_pw';
CREATE USER payment_user WITH PASSWORD 'payment_pw';

-- =====================================================
-- 2) 서비스별 DB 생성 (각 DB의 OWNER 지정)
-- =====================================================
CREATE DATABASE auth_db OWNER auth_user;
CREATE DATABASE auth_test_db OWNER auth_test_user;
CREATE DATABASE product_db OWNER product_user;
CREATE DATABASE order_db OWNER order_user;
CREATE DATABASE payment_db OWNER payment_user;

-- =====================================================
-- 3) auth_db 권한 설정
-- =====================================================
\c auth_db

-- public 스키마에 대한 기본 권한 제거
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- auth_user만 스키마 사용/생성 가능
GRANT USAGE, CREATE ON SCHEMA public TO auth_user;

-- auth_test_db 권한 설정
\c auth_test_db

-- public 스키마에 대한 기본 권한 제거
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- auth_test_user만 스키마 사용/생성 가능
GRANT USAGE, CREATE ON SCHEMA public TO auth_test_user;

-- =====================================================
-- 4) product_db 권한 설정
-- =====================================================
\c product_db

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO product_user;

-- =====================================================
-- 5) order_db 권한 설정
-- =====================================================
\c order_db

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO order_user;

-- =====================================================
-- 6) payment_db 권한 설정
-- =====================================================
\c payment_db

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO payment_user;

-- =====================================================
-- 완료
-- 각 서비스는 자신의 DB + public 스키마만 접근 가능
-- =====================================================
