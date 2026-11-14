-- 审计日志：只记录操作与结果，不记录人脸图、特征、证件号、JWT 原文。
CREATE TABLE face_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES face_tenants (id),
    actor TEXT NOT NULL,
    action TEXT NOT NULL,
    resource TEXT NOT NULL,
    result TEXT NOT NULL,
    request_id TEXT,
    detail_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_face_audit_logs_tenant_created ON face_audit_logs (tenant_id, created_at DESC);
CREATE INDEX idx_face_audit_logs_resource ON face_audit_logs (resource, created_at DESC);
