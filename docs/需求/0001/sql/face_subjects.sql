-- 主体：租户内的核验对象，不保存业务用户资料。
CREATE TABLE face_subjects (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES face_tenants (id),
    external_subject_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'deleted')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    UNIQUE (tenant_id, external_subject_id)
);

CREATE INDEX idx_face_subjects_tenant_status ON face_subjects (tenant_id, status);
