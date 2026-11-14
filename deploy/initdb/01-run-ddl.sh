#!/usr/bin/env bash
# 数据卷首次初始化时按外键依赖顺序执行 DDL（单一来源：docs/需求/0001/sql/）
set -euo pipefail
SQL_DIR="/sql"
ORDER=(face_tenants face_tenant_keys face_subjects face_profiles face_galleries \
       face_gallery_members face_verification_sessions face_documents face_consents \
       face_audit_logs face_operations)
for name in "${ORDER[@]}"; do
  echo ">> applying $name.sql"
  psql -v ON_ERROR_STOP=1 -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -f "$SQL_DIR/$name.sql"
done
