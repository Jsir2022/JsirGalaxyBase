# BQ Base-only 隔离初始化预检

日期：2026-10-12。状态：隔离 CLI 导入与只读预检端到端自动验收完成；没有执行新服/生产初始化。

本文定义一个可重复的本地验收闭环：校验固定任务书来源与 Base JAR → 在临时隔离库应用任务定义 → 只读预检 schema、应用批次、发布内容和章节顺序。所有数据库操作限定在 loopback `_test`/`_it` 数据库；不适用于生产变更。

## 与运行开关的边界

`questPostgresReadEnabled` 默认 `false`，只会在服务端配置中读取。`QuestModule` 仅在 dedicated server、该开关开启、共享 Banking JDBC 已提供时进入启动流程。名称中的 “Read” 只描述终端查询，不代表只读 Quest 模块：启动校验通过后还会安装玩家 Quest 查询、任务追踪、Checkbox/Quest Detect、奖励领取/Choice、管理员定义编辑、消费恢复、事件观察和奖励投递等服务。schema validation 本身只读且不会自动运行 DDL；不能把开启配置当成安全预览。

`noRewards` 默认 `false`，仅控制奖励抑制策略；打开它时 Base 会额外检查 `20260929_001_add_quest_reward_suppressed_status.sql` 对应 schema。Quest 仍使用 Banking 提供的共享 PostgreSQL JDBC，不另配 Quest 数据库连接。

## Schema 准备范围

先由独立、获授权的数据库流程建立基础 schema：`galaxy-quest-postgres/src/main/resources/com/jsirgalaxybase/quest/postgres/quest-postgresql-ddl.sql`，或使用经审查、等价的全库初始化方案。Quest 的追加迁移按文件名顺序核验：

1. `ops/sql/migrations/20260914_001_add_cross_server_quest_platform.sql`
2. `ops/sql/migrations/20260917_001_add_quest_content_migration_audit.sql`
3. `ops/sql/migrations/20260929_001_add_quest_reward_suppressed_status.sql`
4. `ops/sql/migrations/20260930_001_add_bq_party_inventory_snapshot_leases.sql`
5. `ops/sql/migrations/20260930_002_add_quest_lifetime_completion_latch.sql`
6. `ops/sql/migrations/20261009_001_add_quest_definition_catalog_revision.sql`
7. `ops/sql/migrations/20261010_001_add_quest_task_source_progress.sql`
8. `ops/sql/migrations/20261010_002_add_quest_consumption_detect_batch.sql`
9. `ops/sql/migrations/20261011_001_pin_consumption_detect_batch_across_versions.sql`

这些文件是既有项目 schema 的增量升级材料，不由预检自动执行，也不是对某个生产库迁移完整性的批准。Base 的启动校验会检查实际运行所需表和 catalog revision；预检还检查奖励抑制约束、所有来源 Quest/章节已发布、来源批次状态及章节目录顺序。缺表或未应用迁移会给出具体缺项并失败关闭。

## 隔离初始化步骤

1. 将固定来源 `DefaultQuests` 只读挂载；当前预期 SHA-256 为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。确认 Base JAR 已构建，并记录其 SHA-256。JAR 必须含 `GalaxyBase` 与 `QuestModule`，但不含 BQ Importer CLI、Gson、Demo 或 BetterQuesting 运行类。
2. 启动一次性 PostgreSQL `_it`/`_test` 数据库，监听地址只绑定 `127.0.0.1`，数据目录使用容器 tmpfs，不挂载宿主持久卷。建立隔离 schema 后运行上节所列 DDL；预检工具不会替操作者执行 DDL。
3. 仅在该临时库内运行 `apply`。变更命令现在要求 `BQ_MIGRATION_WRITE_CONFIRM=ISOLATED_ONLY`，并会在建立数据库连接前拒绝非 loopback、数据库名不以 `_test`/`_it` 结尾、带 URL 凭据/query/fragment 的连接目标。数据库用户名和密码通过环境变量提供，不写入 URL 或日志。

```bash
export BQ_MIGRATION_JDBC_URL='jdbc:postgresql://127.0.0.1:55436/jgb_bq_n91_it'
export BQ_MIGRATION_DB_USER='postgres'
export BQ_MIGRATION_DB_PASSWORD='隔离容器的临时密码'
export BQ_MIGRATION_WRITE_CONFIRM='ISOLATED_ONLY'

./gradlew :galaxy-quest-bq-importer:run --args='apply /readonly/DefaultQuests'
```

`plan` 是纯文件只读；`dry-run` 会连接数据库但不写入；`preview`、`apply`、`rollback` 是数据库写操作，只有显式 token 与隔离 URL 同时通过才可继续。重复 `apply` 应保持幂等。禁止把临时 token 或本文命令复制到生产连接目标。

4. 对刚刚构建并准备验收的 Base JAR 计算 hash，然后运行预检。数据库连接只通过 `BQ_PREFLIGHT_*` 显式环境变量传入；`BQ_PREFLIGHT_ISOLATED_CONFIRM` 必须是 `ISOLATED_ONLY`。预检会将 JDBC 连接设为 read-only，不读取或汇总玩家进度，不执行写 SQL。

```bash
sha256sum /path/to/jsirgalaxybase.jar

./gradlew :galaxy-quest-bq-importer:baseOnlyPreflight \
  -PbqDefaultQuests=/readonly/DefaultQuests \
  -PbqBaseJar=/path/to/jsirgalaxybase.jar \
  -PbqExpectedSourceSha256=9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc \
  -PbqExpectedJarSha256=<上一步得到的64位SHA-256>
```

预检通过意味着：输入 source 和 JAR 与显式 hash 相符；source manifest 可运行；数据库 schema 通过只读校验；同源批次为 `APPLIED`；Quest/章节数量匹配；该 source 的 dry-run 对每个实体都为 `UNCHANGED`；published 内容无额外实体；章节排序与来源一致。它不证明 Forge 事件、真实库存/流体、玩家存档、跨服链路或 UI 交互通过。

## 代码与回归证据

- Importer 单元测试覆盖 loopback/数据库名/确认 token/URL 参数拒绝、Base JAR 必需类、hash mismatch 与禁止类排除。
- 隔离 PostgreSQL 集成回归覆盖：未导入时预检拒绝且不写数据；应用 fixture 后预检只读通过，批次和发布计数不变。
- 自动测试：`BqBaseOnlyPreflightTest` 3/3；临时 PostgreSQL `BqMigrationDatabaseIntegrationTest` 14/14。另在一次性 loopback/tmpfs PostgreSQL 的 `jgb_bq_n91_cli_it` 执行真实 Gradle CLI `apply /source/DefaultQuests`，产生批次 `bq-9795d6c7fe71e72e`，应用 3,744 Quest、47 章、3,791 个定义/章节实体；随后以本轮刚构建的 Base JAR 执行 `baseOnlyPreflight`，source/JAR SHA、运行类内容、schema、APPLIED 批次、3,791 实体 UNCHANGED 和章节顺序均通过，数据库模式为 read-only。
- 该端到端数据库由本轮专门创建，只在隔离 `_it` 库内应用基础 schema/DDL；预检阶段不写 SQL。没有读取 `Players`、生产进度，没有连接生产数据库或执行生产 DDL。全包测试、assemble、自包含和视觉证据见[N.9.1 工作包](betterquesting-player-book-completeness-work-package-2026-10-12.md)。
- Docker 临时数据库使用 tmpfs 和 loopback 端口，测试后移除；未读取 `Players`、生产进度，未连接生产数据库、执行生产 DDL、部署、启动 Minecraft/Forge 或改变 Quest/BQ 开关。

## 后续 N.9.2 的独立门槛

下一阶段单独规划无 BetterQuesting 的 Base-only Forge 环境：专用隔离数据库、经审核的 schema/import 流程、独立存档与合成玩家。启动运行链前需单独审核 `questPostgresReadEnabled` 的完整效果、奖励策略和回滚方式。该预检不会设置开关，不会代替 N.9.2，更不授权停用旧服 BQ。
