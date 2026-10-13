# Base-only 新服工作包验收证据

本文按新服合同核对交付，历史数字保留对应批次含义。N.9.1 A–D 自动验收已完成；当时 1,159 tests、0 skip/failure/error，真实来源全链审计和 81 帧画廊、隔离 CLI apply + Base JAR read-only preflight、assemble、自包含与终端视觉门禁均通过。N.9.2 已验证隔离 `runServer` 目录/JDK/classpath，Quest/BQ 自动回归与全量构建通过；但 Forge 启动停在 EULA 提示，Base/Quest 没有加载，未创建世界或玩家存档。自动证据不等于 Forge runtime、人工交互、生产切换或 BQ 全生态兼容。

| 合同 | 自动证据入口 | 当前状态 |
|---|---|---|
| 真实九类 Task / 四类 Reward、已知参数键及合法 flag 才允许运行发布 | `BqRuntimeCapabilityRegistryTest`、`BqBehaviorInventoryTest`、完整任务书 occurrence 审计 | N.7 Importer 81 项通过；未知键及非法布尔值 fail-closed。语义等价需看下方 N.7 边界 |
| 所有导入定义 PLAYER，不随 Party 聚合/扇出 | `BqQuestDefinitionImporterTest`、`QuestEvaluationPlannerTest`、`BqPartyInventorySnapshotFactSinkPostgresTest` | Core 299、根 JDBC 16 项通过 |
| 3,744 Quest / 47 章空库导入、来源幂等、完整回滚 | `BqMigrationDatabaseIntegrationTest.completeGtnhBookPublishesAsPersonalContentAndRollsBackFromEmptyDatabase` | 复验通过，健康计数断言通过 |
| 并发应用只产生一个批次和版本 | `concurrentIdenticalImportsCreateOneBatchAndOneVersion` | Importer 全量 73 项中已执行通过 |
| 最新批次回滚、后续版本/依赖保护、失败事务清理 | `unchangedLaterBatchStillPreventsEarlierRollback`、`changedContentCreatesControlledVersionAndProtectsEarlierRollback`、`failedApplyRollsBackEveryMigrationWrite` | Importer 全量 73 项中已执行通过 |
| 新玩家检测、前置、领取和奖励幂等 | Importer 运行集成、Core Runtime/Detect/Choice/Reward 测试、`QuestPostgresIntegrationTest`、根库存奖励集成 | 代表性自动闭环通过；证据分层见下文，不等于逐任务实机验收 |
| 管理员健康查询经服务端授权，玩家不得读取 | `AuthenticatedQuestContentMigrationQueryTest`、`TerminalServiceTest`、`TerminalQuestCenterCodecTest` | 授权、网络往返与不可用状态通过 |
| Base 启动组合实际接入 9 类 Task / 4 类 Reward | `QuestModuleTest`、`BqRuntimeCapabilityRegistryTest`、Quest gameplay/reward 桥测试 | QuestModuleTest 6 项通过；完整自动测试构建复验通过 |
| 终端章节/详情/Choice/错误及管理健康三档布局 | `QuestRuntimeHealthVisualTest`、统一正式终端画廊 | 252 份既有有限场景结构门禁通过；真实内容完整性仍有缺口，见 N.9.1，不代表全部内容和选项可访问 |
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

## N.7 固定 DefaultQuests 参数审计与门禁

来源哈希仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。只读完整来源得到 3,744 Quest、5,620 Task occurrence、5,448 Reward occurrence、47 章；类型计数见活动路线 N.7。此次没有读取 `Players`、旧进度、领取历史或 Party 数据。所有实际 Task/Reward 类型均属于已注册的 9+4 类型；参数选项键按类型白名单逐一检查。导入运行门禁现在也审查未知键和被解析为 invalid 的布尔 flag。

关键固定来源画像（用于下一轮行为符合性测试，不应被误读成全量运行等价证明）：

- Retrieval 3,863：`consume=true` 307、false 3,556；`groupDetect=true` 4、false 3,859；`ignoreNBT=true` 2,029、false 1,834；`partialMatch=true` 3,861、false 2。
- Optional Retrieval 1,267：`consume`、`groupDetect`、`autoConsume` 均为 false；其它布尔选项只出现 0/1。
- Crafting 133：`allowAnvil=false` 全部；`allowCraft`、`allowSmelt` 全部为 true；`allowCraftedFromStatistics=true` 6、false 127；`ignoreNBT=true` 46、false 87；`partialMatch=true` 全部。
- Hunt 104：`ignoreNBT=true` 102、false 2；`subtypes=true` 103、false 1。
- Location 33：`invert`、`taxiCabDist`、`visible` 均 false；`biome=-1`、range 无界、structure 未配置、坐标为原点。
- Quest behavior：所有 Quest 的 `global=false`、`simultaneous=false`；2,494 个 `globalShare=true`。后者在新服合同下仍导入为 PLAYER，是明确的旧 Party 语义不迁移，不代表 BQ 等价。
- Reward：Item 4,181、Choice 957 的 `ignoreDisabled=false`；QuestCompletion 308 的 `ignoreDisabled=true`；XP 2 均 `isLevels=true`、`ignoreDisabled=false`。所有 QuestCompletion 目标 UUID 均可解析，无缺失或自引用。

修复/门禁包括：严格解析布尔值；未知 visibility 不静默回退；XP amount 缺失按 BQ 的整数 getter 默认为 0，负数、非法或超出 signed 32-bit 范围阻断；QuestCompletion 目标缺失/自引用阻断。以上只是 N.7 当时的键/格式证据，并非完整参数语义等价；后续 N.8 结果见下节。

N.7 验收：Importer 全量 81 项在隔离 PostgreSQL 上通过，0 skipped/failure/error；全工程聚合 1,121 项、1,069 执行、52 非 Quest 条件跳过、0 failure/error。52 项为 Bank 18、Market 15、Land 5、ServerTools 7、Warehouse 4、ItemPolicy 3 的数据库配置 assumption-skip，Quest 测试均执行。`assemble`、`verifyUi2SelfContained`、PNG/终端视觉 252 场景通过。CodeGraph sync 6 个 Java 文件，`affected` 选出 24 个相关测试候选。JAR SHA-256：`c13f112703b3cb85d7f39d2715509d2a82724df6256f915aea321c942b4463c5`；ZIP 内容扫描无 BetterQuesting、Importer CLI、Gson 或 Demo 运行类，仅有许可证文本。

## N.8 自动验收完成：参数语义符合性矩阵

CodeGraph `sync` 覆盖 5 个 Java 文件/171 nodes，后续再同步 4 个文件/121 nodes；`impact BqTypedJson.int32` 21 个符号，`affected` 给出 25 个测试候选。发现嵌套 Item Count/Damage、Fluid Amount、Block meta/amount、任务/奖励索引和 BQ int 型任务选项可能在 `long → int` 中回绕，小数可能被 `getAsLong()` 截断。新增精确 signed-32-bit 解析，并在运行能力清单登记 Hunt、Location、Meeting、XP Reward 的数值键；章节几何与 Quest repeatTime 也按源字段类型严格解析。

固定来源测试仅对 `DefaultQuests` 目录做只读挂载，锁定 SHA `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc` 与 3,744 Quest/5,620 Task/5,448 Reward/47 章计数。画像矩阵类 10 项通过，校验 Retrieval 六组 1,584/2/1,966/4/250/57、Optional Retrieval 四组 1/577/3/686、Crafting 三组 81/46/6、Hunt 三个行为组合与全部 14 个 `required` 数值、Location 全部 17 个维度、Meeting、Block Break、Checkbox、Fluid 三个行为组合与全部 4 个流体数量值，以及 4 类 Reward 对应的全部 occurrence。Importer 还验证所有 5,539 Retrieval、3,274 Optional Retrieval、179 Crafting 物品需求均被数量画像统计覆盖，并将全部 distinct Item Count 送入 evaluator 验证阈值边界。

固定 GTNH 环境安装的 `BetterQuesting-3.7.15-GTNH.jar` 只读字节码检查（SHA-256 `9f72317dda06cd883109e9852f2536625412e874c39aa0cb8b70663b716bdbb5`）确认本批字段映射参考了 `TaskRetrieval`/`TaskFluid` Detector、`TaskCrafting`、`TaskHunt`、`TaskLocation`、`TaskMeeting` 及 BQ 原生 int 字段。该检查不复制或打包 BQ 代码，也不构成对所有配置行为的测试。

本阶段额外将固定来源的两种 consuming Retrieval profile 与唯一 consuming Fluid profile 经 importer → Quest Detect → 服务端消费授权 → 隔离 PostgreSQL 进度持久化；数据库测试使用受控 ConsumptionPort 替身，另有来源 Retrieval 请求进入真实 Minecraft ItemStack matcher 的根测试。Item/Choice/XP 来源奖励经实际 Minecraft handler 检查交付与重放幂等，Quest Completion 来源奖励经实际 PostgreSQL 处理器验证目标完成和权益幂等。自动覆盖证明的是实际 profile 与 Base 代码链的代表性/数据驱动组合，不是对 5,448 个 Reward 全部逐个执行交付，也不等于 GTNH Forge 实机验收。

最终验证：Core 299、Postgres 61、Importer 99，共 459 项 Quest 模块测试全执行通过，0 skipped/failure/error；全工程报告 1,143 项，其中 52 项非 Quest 环境 assumption-skip、1,091 项执行通过，0 failure/error。`assemble`、`verifyUi2SelfContained`、Golden 导出和 252 份终端视觉检查通过。运行 JAR SHA-256 `c13f112703b3cb85d7f39d2715509d2a82724df6256f915aea321c942b4463c5`，仅为本地自动验收制品，不是部署包。CodeGraph 最终 up-to-date，`affected` 5 个测试候选全部被回归覆盖。

N.8 与 N.9.1 自动阶段已完成。N.9.2-A 的目录/JDK/classpath 检查完成，但 Forge 启动未越过 EULA 接受步骤。当前未完成项是 Base-only 新服真实运行：Forge 事件、真实玩家背包/流体容器、奖励存档、重启恢复和终端交互。没有访问 Players、连接生产数据库或执行生产 DDL；本轮测试 DDL 仅发生在本地一次性隔离 `_it` 容器。没有部署或启用 Quest。继续启动前需用户明确确认接受 Minecraft EULA。

## 2026-10-12 N.9.1 玩家端完整性自动验收

同一固定来源统计：18 个 Quest 超过当前 6 Task 显示上限、22 个 Quest 超过 4 Reward 上限、70 组 Choice 超过 4 项上限；最大值分别为 12/22/16。1,486 组 Item Reward 配置多件物品，1,579 个 Task 配置多物品要求；全部 3,744 Quest 的 JSON icon 均会被当前显示映射过滤。1,968 个说明超过 200 字符，最长 5,523 字符；当前说明只给两行，搜索栏为 Label，前置只显示数量。

这些统计用于覆盖内容边界，不是完成率。本轮固定来源 3,744 Quest 通过 definition→Mapper→network codec→VisualModel 全量只读审计；6 个真实边界 Quest 三档遍历 409 帧/202 次转换，末项可达且零布局告警，并导出 81 帧 HTML/PNG 画廊。搜索、输入焦点、返回、Choice 原始索引、前置末项及未注册物品占位回归通过。可见前置仍由服务端 Query/VisibilityEvaluator 投影决定，纯 Java门禁不代替 Forge 授权实测。

最终自动测试为 236 suites / 1,159 tests，0 skipped/failure/error：Core 301、Postgres 61、Importer 103、UI2 Core 42、Lab 2、Terminal 32、Demo 5、root 613；Quest 集成测试使用隔离 PostgreSQL。`assemble`、`verifyUi2SelfContained`、`renderGolden`、`verifyTerminalVisuals` 和 `verifyBqQuestBookVisualCoverage` 成功。隔离 CLI 在 `_it` 临时库导入固定来源并以当前 Base JAR 只读预检通过。代码、报告路径、来源/JAR SHA 与边界详见 N.9.1 工作包和最新 Worklog。

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

下一阶段唯一开发入口是 N.9.2 隔离 Base-only Forge 新服真实运行验收；本次未推送、未部署、未执行生产 DDL、未改变服务器开关。
