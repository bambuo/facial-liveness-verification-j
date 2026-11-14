-- 人脸档案：只存加密特征，不存原始帧。
CREATE TABLE face_profiles (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES face_tenants (id),
    subject_id UUID NOT NULL REFERENCES face_subjects (id),
    embedding_cipher BYTEA NOT NULL,
    key_version INTEGER NOT NULL DEFAULT 1,
    model_version TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('enrolling', 'active', 'deleting', 'deleted')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_face_profiles_active_subject
    ON face_profiles (subject_id)
    WHERE status = 'active';

CREATE INDEX idx_face_profiles_tenant_model ON face_profiles (tenant_id, model_version);
