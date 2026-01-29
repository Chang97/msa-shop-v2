-- =====================================================
-- user_db seed (local/dev)
-- - auth_db seed에서 admin/user1의 user_id가 1,2라는 보장은 없다.
--   따라서 이 seed는 "없으면 넣는다" 정도의 샘플로만 사용.
-- - 실제 로컬에서는 register 플로우로 자동 생성되는 것이 정상.
-- =====================================================

-- 예시: auth_user_id=1을 admin이라고 가정하는 샘플(환경에 맞게 수정)
insert into users (auth_user_id, user_name, use_yn)
values (1, 'ADMIN', true)
    on conflict (auth_user_id) do nothing;

-- 예시: auth_user_id=2를 user1이라고 가정
insert into users (auth_user_id, user_name, use_yn)
values (2, 'USER1', true)
    on conflict (auth_user_id) do nothing;
