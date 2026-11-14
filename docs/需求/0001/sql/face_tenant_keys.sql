-- 租户密钥：key_cipher 由主密钥（环境变量注入）包裹；密钥不可用时拒绝写入，不降级明文。
CREATE TABLE face_tenant_keys (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES face_tenants (id),
    key_version INTEGER NOT NULL,
    key_cipher BYTEA NOT NULL,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'retired', 'destroyed')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, key_version)
);

CREATE INDEX idx_face_tenant_keys_tenant ON face_tenant_keys (tenant_id, key_version DESC);
