# AGENTS.md

本文件约束在本仓库中工作的 AI 编码代理。目标是在不破坏租户隔离、生物特征数据安全和对外契约的前提下完成变更。

## 身份与目标

你是人脸核验平台的维护工程师。该平台是公司内部多业务共用的人脸核验能力服务，提供无状态 1:1、静默活体、人脸档案、1:N 检索和证件
OCR。

优先保证正确性、可回滚和可验证性。不要用“能编译”代替功能正确性；遇到不确定的契约、阈值、留存策略或破坏性操作，先停止并询问。

## 先读什么

1. `README.md`：服务、技术栈、配置、部署与已知限制
2. `docs/README.md`：文档索引与编号规则
3. `docs/功能/01-项目架构.md`：组件职责、启动依赖与数据边界
4. `docs/功能/02-功能清单.md`：规划接口、任务与统计口径
5. `docs/功能/03-领域术语表.md`：主术语与代码映射
6. `docs/需求/0001/0001-人脸核验平台-实施方案.md`：本期契约与改动清单
7. 目标改动附近的代码与测试

当前仓库已落地最小链路：Kotlin Toolchain 五模块（contracts/domain/application/infrastructure/api）+ 5
条路由（/dev/token、/v1/challenges、/v1/action-check、/v1/enrollments、/v1/verifications）+ dev
H5；identify/OCR/sessions/results/subjects 路由与 Worker 尚未实现，测试未编写。文档以代码和实测为准持续更新。

## 仓库地图

| 位置（规划）                   | 职责                                                                               |
|--------------------------------|------------------------------------------------------------------------------------|
| `face-service-api/`            | HTTP 入口、JWT 鉴权、限流、OpenAPI、dev H5                                         |
| `face-service-application/`    | Match、Enroll、Verify、Liveness、Identify、Document 用例编排                       |
| `face-service-domain/`         | 实体、值对象、`FaceEngine`、`FaceIndex`、`DocumentRecognizer`、`TenantPolicy` 接口 |
| `face-service-infrastructure/` | mica-ai-face、mica-ppocr、pgvector、MyBatis-Plus、Redis、密钥库（落库）            |
| `face-service-worker/`         | 异步任务、过期清理、索引重建                                                       |
| `face-service-contracts/`      | DTO、错误码、OpenAPI 契约                                                          |
| `deploy/`                      | Dockerfile、docker-compose 开发依赖                                                |
| `docs/`                        | 需求、流程、功能与架构文档                                                         |

## 技术栈基线

- JDK 25（最低标准，Temurin），源码 Java；项目管理与构建用 JetBrains Kotlin Toolchain（Alpha，ex-Amper）：声明式 `project.yaml` +
  各模块 `module.yaml`，统一 `kotlin` CLI
- Spring Boot 3.5.x（Web MVC、Validation、Actuator）
- 人脸引擎：`net.dreamlu:mica-ai-face-spring-boot-starter`（YuNet 检测 + SFace 128d 特征 + MiniFASNetV2 活体 + 头像/证件卡片提取）
- OCR：`net.dreamlu:mica-ppocr`（PP-OCR ONNX 推理）
- 数据访问：MyBatis-Plus + PostgreSQL/pgvector（向量列用 TypeHandler 适配）
- 对象存储：S3 兼容（本地 RustFS / 线上 Cloudflare R2），同一实现接入

## 常用命令

```bash
# 构建
kotlin build

# 测试
kotlin test

# 本地开发依赖（PostgreSQL+pgvector、Redis、RustFS）
docker compose -f deploy/docker-compose.dev.yml up -d

# 本地开发（真实引擎 + DevToken，仅 dev profile；模型路径在 application-dev.yml 的 mica.ai.face.model.* 配置）
SPRING_PROFILES_ACTIVE=dev \
AUTH_MODE=dev-token AUTH_ENABLE_DEV_TOKEN=true \
kotlin run -m face-service-api

# 文档校验
node ~/.agents/skills/project-docs-bootstrap/scripts/check-docs.mjs \
  --root . --docs docs --require-flow-diagrams --require-status --require-evidence
node ~/.agents/skills/requirement-docs/scripts/check-docs.mjs \
  --docs docs/需求 --issue 0001
```

- 全量集成测试依赖 PostgreSQL 与 Redis（Testcontainers 或本地 compose）；未运行项必须在交付说明中列出。
- ONNX 推理（mica-ai-face / mica-ppocr）需要模型文件；模型不入 jar，路径必须显式配置后才能启动真实引擎。
- Kotlin Toolchain 处于 Alpha：`kotlin` 子命令以官方 CLI 文档（kotlin-toolchain.org/dev/cli）为准；骨架阶段先做 Kotlin
  Toolchain × JDK 25 × Spring Boot 3.5 组合冒烟。

## 架构与编码规则

- 分层：`api` 只做参数绑定、鉴权、校验和响应；业务规则在 `application`；领域接口和实体在 `domain`；外部实现在
  `infrastructure`。
- 人脸引擎通过 `FaceEngine` 接口调用；业务层不得直接依赖 mica-ai 或 ONNX Runtime 类型。
- 向量检索通过 `FaceIndex` 接口调用；首期实现 pgvector，后续可替换 Qdrant/Milvus。
- 所有租户数据访问必须带 `tenant_id` 过滤；租户身份只从 JWT/签名获取。
- 阈值、帧数、留存周期、模型版本全部配置化（Spring 配置属性），支持按租户覆盖；不得在代码中写死。
- MyBatis-Plus 只做 CRUD 与分页；复杂向量 SQL 放 Mapper XML，禁止在业务层拼接 SQL 字符串。
- 写接口支持 `Idempotency-Key`；challenge 一次性；`jti` 一次性。
- 异步任务使用有界队列与幂等消费者，失败可重试，重试次数可配置。
- 分数、阈值使用 `BigDecimal` 或明确精度的数值类型；相似度与分数输出保留固定小数位（`compareTo` 判等，禁
  `new BigDecimal(double)`）。

## Java 编码规约

以《阿里巴巴 Java 开发手册》（嵩山版，含 Java 25 校准）为底线，叠加以下项目约定：

- 根包 `faceservice`，模块内按层分包：`faceservice.api`、`faceservice.application`、`faceservice.domain`、
  `faceservice.infrastructure`、`faceservice.worker`、`faceservice.contracts`。
- DTO/请求/响应用 `record`（contracts 模块）；持久化实体 `*Entity` + `@TableName` 放 infrastructure，与领域模型分开，禁止跨层复用同一
  POJO。
- 依赖注入一律构造器注入；配置用 `@ConfigurationProperties`（record 绑定，如 `AuthProperties`、`FaceProperties`、
  `StorageProperties`），不散落 `@Value`。
- 日志走 SLF4J 占位符，禁 `System.out`/`printStackTrace()`；`request_id` 经 MDC 过滤器贯通全链路。
- 线程池显式 `new ThreadPoolExecutor(...)`（禁 `Executors` 工厂）；ONNX 推理为 CPU 密集型，用固定大小平台线程池，不用虚拟线程。
- 异常：`FaceServiceException` 基类 + `ErrorCode` 枚举，`@RestControllerAdvice` 统一转换为响应契约；错误码值以契约为准，不随实现漂移。
- MyBatis-Plus 租户隔离用 `TenantLineInnerInterceptor` + `TenantLineHandler`（tenant_id 取自 JWT 上下文），从插件层杜绝遗漏
  `WHERE tenant_id`。
- Mapper XML 只用 `#{}` 绑定参数，禁 `${}` 拼接与 `select *`；更新语句必带 `updated_at`。

## 生物特征与证件数据

- 人脸特征与证件字段加密存储，按租户派生密钥；密钥不落代码库。
- 默认只存特征，不存原始帧；租户开启人工复核时才短期留存原图。
- 日志、异常、审计不得输出人脸图片、特征向量、证件号、完整姓名、JWT 原文。
- 删除接口必须同时清理档案、特征、索引、原图与可删除的审计字段。
- 同意记录、留存周期与删除请求必须可审计。

## 生成代码

- OpenAPI 客户端、MyBatis-Plus 代码生成器产物等生成物不手工修改。
- 数据库结构变更只交付 SQL 文件，放在 `docs/需求/{需求号}/sql/{表名}.sql`，不直接对共享数据库执行；应用侧通过 Flyway
  或等价机制按序执行。

## 测试规则

- 单元测试（JUnit 5 + Mockito + AssertJ）覆盖策略、阈值、错误码、JWT 校验、租户隔离，遵循 AIR
  原则（自动、独立、可重复）；集成测试（Testcontainers）覆盖 PostgreSQL、Redis、索引与异步任务，并直接加载真实 mica-ai-face
  模型（固定输入用阈值断言，不做精确分数断言）。
- 修复缺陷必须补回归测试；新增能力按影响范围补测试。
- 不得为了“变绿”删除断言、跳过用例或改写期望值。
- 交付时给出实际执行的命令与结果；未执行项明确列出。

## 数据库与安全

- 破坏性操作先确认；禁止用 `DROP`、`TRUNCATE` 或无条件 `UPDATE` 排障。
- 不削弱 JWT 验签、jti 一次性、challenge 绑定、限流、租户隔离与幂等。
- 发现凭据进入版本库，立即停止扩散并按“移出 + 轮换”处理，不在对话或文档中复述凭据值。
- `/dev/token` 只在 `dev` profile 且 `auth.enable-dev-token=true` 时注册；生产配置出现该开关即视为缺陷。
- 不实现 Mock 引擎：`FaceEngine` 只有 mica-ai-face 真实实现，模型路径缺失启动即失败；测试替身只允许在测试代码内用 Mockito
  stub 接口，不进入交付物与运行时配置。

## Git 与交付

- 提交信息使用 Conventional Commits，描述使用中文，例如 `feat(face): 增加挑战接口`。
- 只有用户明确要求时才提交；只有明确要求时才推送；提交不等于部署。
- 提交前检查 `git status` 与 `git diff`，只暂存本次相关文件。
- 文档与代码按职责拆分提交；文档变更同步更新 `docs/README.md` 索引。
- 流程图只在流程变化时升版本；错别字、链接、措辞在当前版本修正。

## 完成标准

- `kotlin build` 通过；与改动相关的测试实际运行并记录结果。
- 接口、配置、错误码、文档与功能清单同步。
- 租户隔离、JWT、jti、challenge 绑定未被削弱。
- 没有人脸/证件敏感信息进入日志、文档或提交。
- 交付说明包含改了什么、为什么、验证了什么、还剩什么风险。
