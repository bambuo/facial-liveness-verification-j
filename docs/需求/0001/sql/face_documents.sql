-- 证件记录：字段加密存储，原图按 retention_until 清理。
CREATE TABLE face_documents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES face_tenants (id),
    subject_id UUID REFERENCES face_subjects (id),
    document_type TEXT NOT NULL,
    fields_cipher BYTEA,
    validation_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    status TEXT NOT NULL CHECK (status IN ('uploaded', 'ocr_processing', 'ocr_done', 'validated', 'matched', 'mismatched', 'rejected')),
    retention_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_face_documents_tenant_created ON face_documents (tenant_id, created_at DESC);
CREATE INDEX idx_face_documents_subject ON face_documents (subject_id, created_at DESC);
CREATE INDEX idx_face_documents_retention ON face_documents (retention_until) WHERE retention_until IS NOT NULL;
