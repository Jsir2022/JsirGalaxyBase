# Base-only 新服工作包验收证据

本文按新服合同核对交付，状态只取本轮实际证据。人工 Forge/GTNH 验收属于下一阶段。

| 合同 | 自动证据入口 | 当前状态 |
|---|---|---|
| 真实九类 Task / 四类 Reward 才允许运行发布 | `BqRuntimeCapabilityRegistryTest`、`BqBehaviorInventoryTest`、完整任务书 occurrence 审计 | Importer 74 项全部通过，含服务层门禁与实际求值器联动 |
| 所有导入定义 PLAYER，不随 Party 聚合/扇出 | `BqQuestDefinitionImporterTest`、`QuestEvaluationPlannerTest`、`BqPartyInventorySnapshotFactSinkPostgresTest` | Core 299、根 JDBC 16 项通过 |
| 3,744 Quest / 47 章空库导入、来源幂等、完整回滚 | `BqMigrationDatabaseIntegrationTest.completeGtnhBookPublishesAsPersonalContentAndRollsBackFromEmptyDatabase` | 复验通过，健康计数断言通过 |
| 并发应用只产生一个批次和版本 | `concurrentIdenticalImportsCreateOneBatchAndOneVersion` | Importer 全量 73 项中已执行通过 |
| 最新批次回滚、后续版本/依赖保护、失败事务清理 | `unchangedLaterBatchStillPreventsEarlierRollback`、`changedContentCreatesControlledVersionAndProtectsEarlierRollback`、`failedApplyRollsBackEveryMigrationWrite` | Importer 全量 73 项中已执行通过 |
| 新玩家检测、前置、领取和奖励幂等 | Importer 运行集成、Core Runtime/Detect/Choice/Reward 测试、`QuestPostgresIntegrationTest`、根库存奖励集成 | 代表性自动闭环通过；证据分层见下文，不等于逐任务实机验收 |
| 管理员健康查询经服务端授权，玩家不得读取 | `AuthenticatedQuestContentMigrationQueryTest`、`TerminalServiceTest`、`TerminalQuestCenterCodecTest` | 授权、网络往返与不可用状态通过 |
| Base 启动组合实际接入 9 类 Task / 4 类 Reward | `QuestModuleTest`、`BqRuntimeCapabilityRegistryTest`、Quest gameplay/reward 桥测试 | QuestModuleTest 6 项通过；完整自动测试构建复验通过 |
| 终端章节/详情/Choice/错误及管理健康三档布局 | `QuestRuntimeHealthVisualTest`、统一正式终端画廊 | 252 份结构门禁通过，健康摘要 PNG 已检查 |
| 根全量测试、assemble、自包含、运行 JAR 排除导入/BQ 类 | 根 Gradle 验收与 JAR 内容核对 | 1,113 项、1,061 执行、52 非 Quest 跳过，0 failure/error；构建与排除核验通过 |
| 分阶段本地提交、真实数量/跳过原因/制品哈希 | Git 与 Worklog | 代码提交 `adfb691` / `664436e` / `6181ece`，最终文档单独提交；制品见下文 |

## 环境与输入

- 只读挂载 S2 `config/betterquesting/DefaultQuests`，没有挂载旧玩家进度。
- 来源摘要：`9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。
- 临时 PostgreSQL 16 Alpine，专用 `quest_it` 数据库、512 MiB tmpfs、仅 loopback 55436。
- 开发容器使用外部只读测试字体；运行 JAR 不增加字体或 BetterQuesting 依赖。
- 所有本轮数据库写入均发生于测试自行创建的临时 schema。

本表未完成项必须保留到具备证据；不得把隔离自动验收表述为生产切换完成。

## 新玩家闭环的证据分层

没有用旧玩家进度补齐任何用例。完整任务书做定义/occurrence 静态审计与空库发布；运行行为使用合成玩家和代表性导入定义验证，不宣称逐个实际游玩了 3,744 个任务。

- 九类目标：`BqCompatibleTaskEvaluatorsTest` 验证实际求值器；`BqRuntimeCapabilityRegistryTest` 将允许发布的类型与真实求值器注册表、事实类型联动。根事件/库存测试覆盖平台事实桥接。
- 导入到运行：`BqMigrationDatabaseIntegrationTest` 覆盖章节/前置/奖励/玩家浏览、导入 Location 周期、Checkbox 鉴权、服务重建和换来源服；`BqPartyInventorySnapshotFactSinkPostgresTest` 覆盖导入的个人背包检测与领取→Quest Completion→目标奖励链。
- 前置、秘密、主线、重复和 simultaneous：`QuestEngineTest`、`QuestVisibilityEvaluatorTest`、章节依赖与任务中心查询测试；`AuthenticatedQuestConsumptionDetectServiceTest` 验证 Quest Detect 顺序、发布新版不改正在执行批次及周期领取门禁。
- Item/Choice/XP：`MinecraftRewardDeliveryTest` 覆盖真实奖励处理器、保存失败回滚、持久化标记、重放、NBT 重载和周期隔离；`QuestPostgresIntegrationTest` 覆盖未选择不得领取、选择锁定、租约接管和周期 Choice。
- Quest Completion/手动与自动领取：PostgreSQL 奖励链测试覆盖个人目标完成、自动领取源任务与仍可领取的目标奖励；重复事实只创建一次权益。
- 跨服与故障：PostgreSQL Crafting 来源最大值求和、幂等事实、事务回滚和消费批次游标；根 `OrderedQuestFactRetryQueueTest`、`QuestGameplayEventHandlerFailureTest` 保留故障边界与有界有序重试基线。

Command/Scoreboard 奖励有既有处理器单测，但不在固定任务书的四种实际奖励内，本轮不新增运行发布许可；未来来源出现时仍阻断，不能拿旧单测自动批准新内容。

这些证据不替代真实 Forge 事件、资源包物品和玩家交互验收，也不把数据库与 Minecraft 玩家文件的交付宣称为跨存储原子事务。

## 最终自动验收与制品

- Core 299、Postgres 61、Importer 74、UI2 Core 41、Lab 2、Terminal 29、Demo 5、根 602：共 1,113 项，1,061 执行，0 failure/error。
- 根 52 项 assumption-skip：Bank 18、Market 15、Land 5、ServerTools 7、Warehouse 4、ItemPolicy 3，缺少各业务独立测试数据库配置；Quest 数据库测试全部实际执行。
- 提交后 `test assemble verifyUi2SelfContained :ui2-demo:renderGolden :ui2-demo:verifyTerminalVisuals` 成功，57 tasks，20 executed、37 up-to-date；Importer 最新 74 项此前实际重跑成功。
- 252 个正式终端视觉快照通过；直接检查了小窗健康页 PNG。图形结果仅为 Java2D 证据。
- 运行 JAR：`.gradle/codex-r2-confirmed-build/root/libs/jsirgalaxybase-6181ece-main+6181ece57c-dirty.jar`。
- SHA-256：`0b8266264ced24ea90757bed584b6c8ce19d991996adcba64fb673184d602645`。`dirty` 来自构建时尚未提交的文档；代码对应 `6181ece`。
- `verifyUi2SelfContained` 通过，另直接检查 ZIP 项无 BetterQuesting 运行包、Importer/CLI、Gson 和 Java2D Demo 类。
- CodeGraph 已同步且 up-to-date；运行清单 impact 与 affected 指向兼容报告、发布服务和六个 Importer 测试类。此前 Runtime/Planner/投影/终端服务影响链见 Worklog。

- N.6 验收：全模块报告共 1,114 项、1,062 执行、52 非 Quest 条件跳过、0 failure/error；`QuestModuleTest` 6 项全部执行。完整命令 `:galaxy-quest-core:test :galaxy-quest-postgres:test :galaxy-quest-bq-importer:test :ui2-core:test :ui2-lab:test :ui2-terminal:test :ui2-demo:test test assemble verifyUi2SelfContained :ui2-demo:renderGolden :ui2-demo:verifyTerminalVisuals` 成功，57 tasks、15 executed、42 up-to-date；252 终端快照通过。
- N.6 JAR：`.gradle/codex-r2-confirmed-build/root/libs/jsirgalaxybase-18d3301-main+18d3301be4-dirty.jar`，SHA-256 `e939581b58bd58ec9c1a3275e65d7b37b835c829cc0c92c2c2b9ebfadf624943`；反编译前 class entry 确认存在新 handler factory，ZIP 排除扫描通过。`dirty` 表示当时 N.6 源码和阶段文档尚未提交。
- N.6 CodeGraph sync 2 个文件、impact `QuestModule` 142 符号；随后 sync 1 个测试改动，handler factory impact 5 符号。affected 候选较宽，直接测试范围收敛至 `QuestModuleTest` 和已存在的 BQ Item delivery 测试；报告合计测试通过。

下一门槛只保留 Base-only 新服人工运行及单独授权的启用准备；本次未推送、未部署、未执行生产 DDL、未改变服务器开关。
