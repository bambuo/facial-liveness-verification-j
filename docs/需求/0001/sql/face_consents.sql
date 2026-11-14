-- 同意记录：用于证明采集与处理已获授权，支持撤回。
CREATE TABLE face_consents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES face_tenants (id),
    subject_id UUID NOT NULL REFERENCES face_subjects (id),
    policy_version TEXT NOT NULL,
    agreed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    ip_hash TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_face_consents_subject ON face_consents (subject_id, agreed_at DESC);
CREATE INDEX idx_face_consents_tenant ON face_consents (tenant_id, agreed_at DESC);
