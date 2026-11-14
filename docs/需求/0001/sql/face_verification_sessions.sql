-- 核验会话：challenge、结果、分数与审计关联。
CREATE TABLE face_verification_sessions (
    id UUID PRIMARY KEY,
    challenge_id TEXT NOT NULL UNIQUE,
    tenant_id UUID NOT NULL REFERENCES face_tenants (id),
    subject_id UUID REFERENCES face_subjects (id),
    scene TEXT NOT NULL,
    ref TEXT,
    mode TEXT NOT NULL CHECK (mode IN ('match', 'liveness', 'enroll', 'verify', 'identify', 'document')),
    status TEXT NOT NULL CHECK (status IN ('created', 'challenging', 'collecting', 'processing', 'passed', 'failed', 'expired')),
    score NUMERIC(6, 5),
    liveness_status TEXT CHECK (liveness_status IN ('real', 'spoof', 'fuzzy', 'detecting', 'error')),
    clarity NUMERIC(6, 5),
    reality NUMERIC(6, 5),
    device_id TEXT,
    request_id TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_face_sessions_tenant_created ON face_verification_sessions (tenant_id, created_at DESC);
CREATE INDEX idx_face_sessions_subject ON face_verification_sessions (subject_id, created_at DESC);
CREATE INDEX idx_face_sessions_status_expires ON face_verification_sessions (status, expires_at);
