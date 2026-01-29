-- =====================================================
-- auth_db seed (local/dev)
-- =====================================================

-- Roles
insert into role (role_name, use_yn)
values
    ('ROLE_ADMIN', true),
    ('ROLE_USER', true)
    on conflict (role_name) do nothing;

-- Admin credential
insert into auth_user_credential (email, login_id, password_hash, enabled)
values ('admin@msashop.local', 'admin', '$argon2id$v=19$m=16384,t=2,p=1$OJ0tCd2hvSn/5ukBSacGmw$d2v0DbDqyk/3I9sSxDP0xIbTxw0CMdefqThx1g6Qvhw', true)
    on conflict (email) do nothing;

-- Admin role mapping
insert into user_role_map (user_id, role_id)
select c.user_id, r.role_id
from auth_user_credential c
         join role r on r.role_name = 'ROLE_ADMIN'
where c.login_id = 'admin'
    on conflict do nothing;

-- (선택) 일반 유저 샘플
insert into auth_user_credential (email, login_id, password_hash, enabled)
values ('user1@msashop.local', 'user1', '$argon2id$v=19$m=16384,t=2,p=1$OJ0tCd2hvSn/5ukBSacGmw$d2v0DbDqyk/3I9sSxDP0xIbTxw0CMdefqThx1g6Qvhw', true)
    on conflict (email) do nothing;

insert into user_role_map (user_id, role_id)
select c.user_id, r.role_id
from auth_user_credential c
         join role r on r.role_name = 'ROLE_USER'
where c.login_id = 'user1'
    on conflict do nothing;
