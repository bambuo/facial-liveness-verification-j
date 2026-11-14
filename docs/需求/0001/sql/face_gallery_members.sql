-- 1:N 向量成员。vector(N) 的维度必须与服务启动时探测的模型维度一致；
-- mica-ai-face 的 SFace 输出 128d L2 归一化向量，示例使用 128；启动时仍需探测校验后再建索引。
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE face_gallery_members (
    gallery_id UUID NOT NULL REFERENCES face_galleries (id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES face_subjects (id) ON DELETE CASCADE,
    embedding vector(128) NOT NULL,
    model_version TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (gallery_id, subject_id)
);

CREATE INDEX idx_face_gallery_members_embedding
    ON face_gallery_members
    USING hnsw (embedding vector_cosine_ops);
