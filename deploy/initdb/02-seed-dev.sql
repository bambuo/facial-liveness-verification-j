-- 本地开发租户（dev token 固定 tenant=dev）
INSERT INTO face_tenants (id, code, name, status)
VALUES ('11111111-1111-1111-1111-111111111111', 'dev', '本地开发租户', 'active')
ON CONFLICT (code) DO NOTHING;
