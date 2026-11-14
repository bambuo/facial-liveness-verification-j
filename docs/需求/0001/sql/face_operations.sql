-- 异步任务：OCR、1:N 等长任务的状态与结果。
CREATE TABLE face_operations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES face_tenants (id),
    subject_id UUID REFERENCES face_subjects (id),
    type TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('queued', 'running', 'succeeded', 'failed')),
    result_json JSONB,
    error_code TEXT,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_face_operations_tenant_created ON face_operations (tenant_id, created_at DESC);
CREATE INDEX idx_face_operations_status ON face_operations (status, created_at);
