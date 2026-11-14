-- 租户与策略。策略中的阈值、帧数、留存周期均按租户覆盖。
-- 租户密钥存 face_tenant_keys，key_cipher 由主密钥（环境变量注入）包裹。
CREATE TABLE face_tenants (
    id UUID PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'disabled')),
    policy_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_face_tenants_status ON face_tenants (status);
