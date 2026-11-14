-- 1:N 人脸库：阈值与 margin 必须按真实 gallery 校准后写入，不设通用默认值。
CREATE TABLE face_galleries (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES face_tenants (id),
    name TEXT NOT NULL,
    threshold NUMERIC(5, 4) NOT NULL,
    margin NUMERIC(5, 4) NOT NULL,
    model_version TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, name)
);

CREATE INDEX idx_face_galleries_tenant ON face_galleries (tenant_id);
