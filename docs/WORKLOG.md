# JsirGalaxyBase Work Log

### 2026-10-11 - R.2.35 Checkbox 周期修复与 M.2 有限自动收口

- 用户授权自设目标并继续完成 M.2 自动收口；目标已存于独立文档 `betterquesting-m2-automatic-closeout-2026-10-11.md`。以真实内容的 9 类 Task / 4 类 Reward 为范围，明确区分代码缺口、范围限制、内容警告与运行验证。没有为零使用类型或观察站扩展玩法。
- 发现并复现 Checkbox 重复周期错误：入口按完成时间计算下一周期事实键，Runtime 却可能因待领奖/待消费或 claim-time cooldown 保持旧周期，提前点击会写入未来周期键。三个 Core 回归初次全部失败。修复后仅在 Runtime acceptance 事务内按已有 reward/consumption fence、领取 anchor 和当前周期授权，拒绝不落事实；初始读取只生成候选键，避免在事务外调用 JDBC 必须事务化的查询。服务端 identity/task/version 和既有 scope 不变。
- 新隔离集成覆盖 typed BQ JSON 导入、重复 Checkbox 完成、待领奖时拒绝、领取后 99/100 ms 边界、重建 JDBC/服务对象且切换来源服、第二周期完成、旧实例重复点击和其他玩家隔离。实例切换不等于真实 Forge 多服运行。物品奖励审计保留世界掉落与最终 marker 非原子限制，并纠正 handler 的过度“Crash-safe”注释；没有修改奖励策略。
- CodeGraph 起始 up-to-date（旧 engine 提示）；confirm callers/callees/impact 和 item reward deliver impact 定位 Core、Importer 集成与终端调用。改后首次 sync 4 files/193 nodes，最终 sync 1 file/19 nodes；confirm impact 14 symbols，affected 51 个候选，使用全工程覆盖。
- 全量 Docker Gradle `test assemble verifyUi2SelfContained :ui2-demo:renderGolden` 成功，56 tasks 全执行。最终 XML 227 suites / 1,086 tests / 1,034 executed / 52 assumption-skip / 0 failure / 0 error；Core 295、Quest PostgreSQL 61、Importer 52 全部实际执行，0 skipped。52 项跳过为 48 项其它业务 PostgreSQL 未配置、4 项 Warehouse 专用 `JGB_BANKING_IT_*` 缺失。已有预览 Noto TTC 只读挂载用于 Java2D，不打入 Mod。
- 初次 Core Docker 运行因 root 容器 Git ownership 提示使用 NO-GIT-TAG 版本覆盖，但测试成功；正式全量运行使用仅对容器进程生效的 `GIT_CONFIG_*` safe.directory，未改宿主 Git 配置。构建目录复用忽略的 `.gradle/codex-isolated-build-r234`，测试任务强制执行，未使用旧 XML 冒充重跑。直接 javac/JUnit 复现 7 tests 中 3 failure，修复后 7/7；正式验收以 Docker Gradle 结果为准。
- 临时 PostgreSQL 16 为 `jgb-m2-closeout-pg`，512 MiB tmpfs、无宿主数据卷、仅 loopback 55436，专用 `jgb_m2_closeout_test`；未加载 Compose `.env`，测试结束自建 schema 残留为 0。全量预提交 JAR SHA-256 `b0d9137dc178793707a61b51d984244615dfafef7094f05f36ef9b5f42a2881c`，仅是当时工作树验收快照，后续版本元数据改变会生成新哈希。
- 累计变更按 Quest Core/Postgres/Importer、Minecraft Quest、终端动作、UI2 Quest 和四个 SQL 迁移明确路径审计并暂存；未使用 `git add -A`。代码检查点提交 `d74844cb26a773c676fabe5f68def0ca3a6007a7`，105 files / 8,456 additions / 415 deletions；含此前可靠性/兼容累计实现及本次修复。文档单独提交；本次没有推送授权目标，不自行推送。
- 权威能力矩阵只保留一张当前阶段表，早期矛盾“待确认”放入历史折叠区；开发导航明确 R.2.19/版本并发已经解决。独立收口文档记录有限清单与 M.3 前置：重复 Quest 来源冲突选择、无生产写入的 dry-run、manifest/领取映射与审计回滚。M.2 实机仍 BLOCKED，M.3 写入器、M.4 诊断、M.5 实时影子与 M.6 切换仍未完成。
- 未访问生产玩家进度/数据库，未执行生产 DDL、部署、Quest/BQ 开关切换或操作 S1/S2。当前目标止于有限自动收口和本地稳定检查点，不扩大为 BQ 已替代。
- 代码提交后的最终构建：依赖 jars 13 tasks 全执行、`:assemble :verifyUi2SelfContained` 29 tasks 全执行成功。JAR `.gradle/codex-isolated-build-r234/_root/libs/jsirgalaxybase-d74844c-main+d74844cb26-dirty.jar`，SHA-256 `965cb0bb595c6b066147f102db81b6faaea6d7e78fee121673003d4bda430fc1`；dirty 仅为构建时未提交的文档。内容复核保留 BetterQuesting MIT 来源文件并包含 Core/Postgres，无 importer/BQ 运行类/Gson 导入 CLI/Noto 字体。临时 PG 已销毁，55436 已释放，init 脚本删除；ignored 构建报告/JAR 保留供审阅。文档单独提交后工作树检查为空。

### 2026-10-11 - R.2.34 事件时 Crafting 归属与 Detect 版本并发收口

- 用户确认：Crafting 事实按事件发生时的 Quest 资格和完整 Party roster 归属；权威快照无法取得时 fail-closed。Detect 进行中允许管理员发布新版；旧批次保留旧版定义、Task 顺序、周期及奖励完成，旧进度不被新版覆盖。新版删除旧目标后如需人工补偿，由管理员处理。
- 实现：生产 `QuestGameplayEventHandler` 通过 `QuestEvaluationPlanner.requestsFor(fact)` 同步捕获事件时请求，`QuestFactSink`/Runtime sink 及 FIFO 重试队列传递同一不可变请求；捕获失败记录错误并拒绝事实。队列上限为 1,024 条和估算 8 MiB，单次最多重试 32 条、20 tick 周期；正常关服清空，进程崩溃丢失尚未被 PostgreSQL 接收的内存事实，不提供 durable queue / exactly-once。快照字节估算覆盖定义名/描述、任务/奖励及其参数和成员请求，新增大定义快照超内存上限拒绝回归。
- Detect 版本恢复：旧活动批次按 participant+Quest 查询及唯一约束，允许新版发布但不允许同主体/Quest 并发批次；批次授权、消费确认事实、进度结算均读取其固定历史 PUBLISHED 版本。新增 `20261011_001_pin_consumption_detect_batch_across_versions.sql`，迁移先拒绝存在多活动批次的数据，不自动合并。Quest 集成回归覆盖 v1 批次启动、发布 v2、入口重新采用 v1、旧版任务/奖励继续完成及迁移幂等；迁移只在临时测试数据库执行。
- CodeGraph：起始 `status` 检出 4 个待同步文件；首轮 `sync` 更新 4 files / 291 nodes。核心影响为 `QuestEvaluationPlanner` 111、`OrderedQuestFactRetryQueue` 74 个符号；首轮 `affected` 给出 51 个候选测试文件。补充 sink fail-closed 防降级后第二次 `sync` 更新 2 files / 58 nodes；最终 `impact` 为 `QuestGameplayEventHandler` 84、`AuthenticatedQuestConsumptionDetectService` 38、`JdbcConsumptionDetectBatchRepository` 33 个影响符号，最终 `affected` 扩展到 81 个候选。选择 Quest Core、PostgreSQL 与根全套门禁实际覆盖。
- 自动验收：`:galaxy-quest-core:test` 292 tests，0 skipped/failure/error；`:galaxy-quest-postgres:test` 在隔离 PG16 上 61 tests，0 skipped/failure/error。根 `test assemble verifyUi2SelfContained` 最终成功；全工程报告共 1,082 tests，1,030 executed、52 assumption-skip、0 failure/error。48 项来自未配置其它业务模块自己的隔离 PostgreSQL 环境；4 项来自未提供 Warehouse `JGB_BANKING_IT_*` 专用配置。开发容器只读挂载宿主 Noto TTC 为 Java2D 预览字体，未进入源代码或 JAR。首次离线网络构建在 GTNH convention 插件 DNS 处停止；全量视觉任务首次因缺预览字体失败；按已记录的代理/只读字体方式重跑通过。独立 buildDir 中第一次组装前缺少 UI2 project jars，先生成依赖 jars 后最终构建成功。另一次根测试复跑时临时 PG 已清理，导致 16 个根 JDBC 用例连接失败；重新创建隔离库后再次全量运行，最终结果为上述 0 failure/error。
- PostgreSQL 使用临时 PostgreSQL 16 容器，数据目录 512 MiB tmpfs、仅 `127.0.0.1:55432`、无宿主数据卷，容器已销毁。没有访问生产数据库/玩家进度，没有执行生产 DDL、部署、启用 Quest、停用 BQ 或触碰 S1/S2。
- 最终 runtime JAR：`.gradle/codex-isolated-build-r234/_root/libs/jsirgalaxybase-6e80f68-main+6e80f68c22-dirty.jar`；SHA-256 `c9bfea25b27c930376a1fe49b716f1857cd0341066a77a8f81a89c10f0061cfe`。仅构建产物，未部署。`git diff --check` 通过。
- M.2 仍为 `BLOCKED`：真实 Forge/GTNH 事件时序、线上数据库恢复和物品/玩家存档崩溃窗口未覆盖；进程崩溃可丢未持久化 Crafting 事实是已知的产品边界。

### 2026-10-11 - R.2.33 PlayerTick 周期边界隔离回归

- 将 `QuestGameplayEventHandler` 的 20/60/100 tick 间隔检查抽为 `PeriodicTickCadence.isDue`，保留现有调度语义；Crafting 统计自动复评仍在 60 tick 周期，不改变 Quest Detect、Party 或来源服合并规则。
- CodeGraph 起始 `status` up-to-date；`callees QuestGameplayEventHandler.onPlayerTick` 确认三个间隔调用。首次 `sync` 更新 4 个 Java 文件，最后一次同步两处 helper/test 修改为 2 files / 13 nodes；`impact PeriodicTickCadence.isDue` 定位事件处理器及新测试。`affected` 对事件处理器/helper/测试返回 46 个宽候选，直接逻辑门槛为 `PeriodicTickCadenceTest`。
- 根 Gradle 定向任务首次使用一次性 `galaxy-dev:local` 容器、`--network none`、现有项目/Gradle 缓存挂载与隔离 buildDir；构建在 `build.gradle.kts` 应用 GTNH convention 插件时因离线 DNS 无法解析 `raw.githubusercontent.com` 退出，未进入源码编译或根测试。只读检查确认本机 7892 代理仍监听，未改动该网络服务；随后用 Docker `run --network host` 和 HTTP(S) 代理、同一离线缓存重跑两个定向测试类，成功通过 `compileJava`、`compileTestJava`、`:test`。两个 XML suite 分别 4、2 项，合计 6 tests、0 skipped/failure/error；33 actionable tasks / 33 executed。Compose `run` 不接受 `--network` 参数，故最终使用直接 Docker `run`。
- 曾尝试用 Mockito 构造 `EntityPlayerMP` 以直接派发 `PlayerTickEvent`，但根测试 classpath 未声明 Mockito，编译失败。没有为单个夹具新增测试依赖；撤回该夹具，保留无依赖的 cadence 谓词测试和既有自动 Crafting 分发回归。此边界明确记录为真实 Forge 事件派发未覆盖。
- 随后在同一隔离 buildDir 执行 `:assemble verifyUi2SelfContained`，成功：29 actionable tasks，7 executed、22 up-to-date。runtime JAR `.gradle/codex-r2-crafting-cadence-build/root/libs/jsirgalaxybase-6e80f68-main+6e80f68c22-dirty.jar`，SHA-256 `795d471321ef287b6b838fdab2751c0f724c1ad233b06fa00c801295464698ae`；仅为构建产物，未部署。
- 另用 JDK 17 `javac -source 8 -target 8` 单独编译 helper 与测试，使用缓存 JUnit 4.13.2/hamcrest 实际运行 2 项：`OK (2 tests)`，0 failure/error；有 bootstrap classpath 警告。最终 `git diff --check` 通过。真实 Forge PlayerTick 派发和全量测试未完成；`assemble` 与自包含门禁已按上项通过。
- 未访问生产数据库/玩家进度，未执行 DDL、部署、启用 Quest、停用 BQ 或操作 S1/S2。全量 `test` 未运行；R.2.19 Party 延迟事实归属、Detect 活动批次遇新定义发布策略、真实 Forge/GTNH tick 时序仍待处理；M.2 与 BQ 替代保持 `BLOCKED`。

### 2026-10-11 - R.2.32 PARTY Crafting 在线门控与全 roster fan-out 组合回归

- CodeGraph 起始索引 up-to-date。`impact BqPartyActiveQuestQuery.hasActiveMember`、`impact BqHuntPartyAssignmentPolicy.select` 各显示 8 个影响符号；`affected galaxy-quest-core/src/test/java/com/jsirgalaxybase/quest/core/BqPartyActiveQuestQueryTest.java` 选择该 Core 测试文件。新增后 `codegraph sync` 更新该测试文件 57 nodes。
- 新增 `oneOnlineEligiblePartyMemberActivatesCraftingAndFansOutToTheEntireRoster`：在线但未解锁的产物触发者与一名在线已解锁成员共同组成 PARTY，第三名成员离线且未解锁；匹配事件向三名成员各自产生 PLAYER 进度请求。将唯一满足资格的成员设为离线后，同一事件不再路由。该用例锁定本服在线资格并集与资格通过后的全 roster 事件 fan-out 分工。
- Docker `galaxy-dev:local`、离线执行 `:galaxy-quest-core:test --tests com.jsirgalaxybase.quest.core.BqPartyActiveQuestQueryTest`：9 项、0 skipped/failure/error，`BUILD SUCCESSFUL`。宿主 UID 首次运行在共享 Gradle 配置缓存 keystore 权限处退出，未进入测试；以隔离 buildDir 的 root 容器重跑成功。Git dubious-ownership 输出由既有版本覆盖继续处理，没有改 Git 全局配置。
- 更新能力矩阵、M.2 行为矩阵和本开发导航记录。只改 Core 测试与文档；没有生产 Java、PostgreSQL、DDL、Forge 构建、自包含门禁、部署或 Quest/BQ 开关变化。该回归不覆盖延迟队列中 Party 变化、事件时资格快照、真实 Forge 回调或跨服运行；R.2.19 和 M.2 保持 `BLOCKED`。未读取生产数据库/玩家进度。

### 2026-10-11 - R.2.31 Crafting 统计按来源服逐需求门控

- 按用户已确认的 Crafting 来源服 `max(event, absolute)` 后求和合同，核对固定 BQ `TaskCrafting.onItemInternal/detect` 与 Base 的 `ItemVectorTaskEvaluator`。BQ 对每个需求分别检查该来源当前进度是否 `< target`，只为未达标分量读 `objectCraftStats`；Base 原先没有这层门槛，事件已经让本服需求达标时，紧随其后的绝对统计仍会把该分量抬至历史总量。
- 最小修复按当前来源贡献的 event/absolute max 做逐需求门控；别的需求与别的来源仍独立观察。新增 `craftingStatisticsOnlyRefreshRequirementsBelowTargetOnThatSource`，验证同源事件达标分量不再被统计抬高、另一来源仍贡献个人计数、未达标第二分量仍可保留超目标绝对值。未改变用户选定的个人/Party/跨服归并合同。
- CodeGraph 起始 `status` up-to-date；`explore` 检查 `TaskCrafting` 源码、`ItemVectorTaskEvaluator.evaluate`、`TaskProgress.SourceContribution`；`impact ItemVectorTaskEvaluator.evaluate` 覆盖 14 个符号；`affected` 返回 56 个宽候选，直接测试收敛到 Core `BqCompatibleTaskEvaluatorsTest`。`sync` 更新 2 个 Java 文件。
- 验收：Crafting evaluator 专项类 45 项通过；Core 全套强制重跑 62 suites / 290 tests，0 skipped/failure/error。使用 JDK 17 与已缓存 Gradle 离线运行。首次 Docker 断网尝试在 GTNH convention 插件解析阶段因 raw.githubusercontent.com DNS 失败，未进入测试；该次不计测试失败。最终 `git diff --check` 通过。未跑 PostgreSQL、根 Forge 测试、assemble、自包含门禁；未读取生产玩家进度/数据库、执行 DDL、部署或切换 Quest/BQ；M.2 仍 `BLOCKED`。

### 2026-10-11 - BQ Detect 与跨服 Crafting 归并合同复核

- 用户明确确认：消耗型物品/流体任务使用单一 Quest 级 Detect，按定义顺序提交未完成任务；非消耗任务继续自动观察。Crafting 同一玩家跨服按每个来源服的事件累计与绝对统计快照取最大值，再对来源服求和；不同 Party 成员仍保持个人进度。该答复确认既有合同，没有扩大到消费/数据库全有或全无。
- CodeGraph `status/sync` 确认索引最新；`explore` 跟踪 `AuthenticatedQuestConsumptionDetectService`、`ConsumptionRequestAuthorizer.planQuestDetect`、`QuestCenterVisualDocument`、`QuestGameplayEventHandler`、`AuthenticatedCraftingStatisticsDetectionService` 与 `TaskProgress`。UI2 已显示单一“检测全部消耗目标”入口，旧单任务提交入口未被页面调用；统计来源 ID 由各服 `bankingSourceServerId` 进入生产 detector/fact factory。现有回归 `importedQuestDetectProcessesMultipleConsumingRetrievalTasksInOrderAndResumesAfterInterruption` 与 `automaticBqCraftingStatisticsKeepPersonalCountersAndSumPerServerMaxima` 分别覆盖顺序/恢复及 s2=5、lobby=2 后求和为 7、来源内取最大值与成员个人隔离。
- 本轮未更改生产代码，因为已确认的两个合同已有对应实现和回归；待决的定义版本发布与活动批次并发、Crafting 事实延迟时 Party 归属仍未被本次答复覆盖。未访问生产玩家进度/数据库，未执行 DDL、部署、Quest/BQ 开关或服务器操作。M.2 仍 `BLOCKED`。

### 2026-10-11 - R.2.30 多消耗 Retrieval Quest Detect 顺序与恢复闭环

- 依据用户已确认的 Quest 级 Detect 合同，先用 CodeGraph `explore/impact` 检查 `BqQuestDefinitionImporter.read`、`AuthenticatedQuestConsumptionDetectService.detect`、`MinecraftPlayerConsumptionPort.planItems`、JDBC 批次与 Runtime；改前影响链包括 Quest Detect 单测、Quest 终端调用点、既有单任务 Fluid 恢复集成。`affected` 将本次测试选择收敛到根 `BqPartyInventorySnapshotFactSinkPostgresTest`。新增后 `codegraph sync` 更新该测试文件 221 nodes，`affected` 仍仅选该集成类。
- 新增 `importedQuestDetectProcessesMultipleConsumingRetrievalTasksInOrderAndResumesAfterInterruption`：导入 typed BQ JSON 的 OR Quest（两个不同 Item 的 consuming Retrieval），在首项已扣除并提交进度后、第二项物品操作前注入故障。验证活动批次游标停留第二项，OR Quest 不提前结算；恢复复用第二项幂等键、不重复扣第一项，最终两项完成后 Quest 才结算。
- 隔离回归先跑新增方法 1 项，再强制重跑完整根 JDBC 集成类 16 项，0 skipped/failure/error。测试用 PostgreSQL 16，只在 internal Docker 网络可达，数据目录为 512 MiB tmpfs、无宿主卷/端口映射；结束前确认测试 schema 数为 0，之后删除临时容器与网络。Minecraft 测试初始化向只读 `logs/latest.log` 写日志时输出权限诊断，但 JUnit 结果仍全通过。`git diff --check` 后续收口时复核。
- 本批只新增测试与文档，无生产 Java、数据库合同或迁移变化。未运行全量测试、`assemble`、自包含门禁或 Forge/GTNH 实机；未访问生产玩家进度/数据库、执行生产 DDL、部署、Quest/BQ 开关或操作 S1/S2。之前已存在的非消耗 Retrieval importer→PARTY snapshot→Runtime/JDBC 与 `groupDetect` 隔离集成不再列为缺失门槛。PlayerData/World 崩溃边界、R.2.19 Party 延迟事实归属、Detect 活跃批次遇到新定义发布的版本策略仍未收口；M.2 保持 `BLOCKED`。

### 2026-10-11 - R.2.29 重复周期物品奖励延迟恢复隔离

- CodeGraph 起始 `status` 为 up-to-date；`explore` 核对 `MinecraftItemRewardDeliveryHandler.deliver/deliverOverflow`、`QuestRewardMarker.key`、`RewardEntitlement` 与 Worker 确认链。改动前 `impact` 覆盖投递 handler 和周期权益键，`affected` 收敛到根 `MinecraftRewardDeliveryTest`；改动后 `sync` 更新 1 个测试文件/127 节点，`impact` 将新增回归纳入影响集，`affected` 仍只选该根测试文件。
- 新增 `delayedPreviousCycleItemRewardRecoveryDoesNotReuseNextCycleMarker`：用真实 `RepeatPolicy`/`RewardEntitlement` 生成相邻周期权益；在旧周期部分写入背包后模拟溢出掉落失败，执行压缩玩家 NBT 往返，再先完成新周期权益、后恢复旧周期。断言周期 marker/部分进度互相隔离，140 个奖励物品总量无重复或丢失。此测试验证 marker 键与 NBT 恢复，不模拟真实 Forge 世界保存或跨服在线租约。
- Docker `galaxy-dev:local`、`--network none`、隔离构建输出下运行根 `:test` 的 `MinecraftRewardDeliveryTest` 与 `BqItemRewardImportDeliveryTest`：29 + 5 = 34 项，0 skipped/failure/error。`prepareUi2QzLicense` 因联网 DNS 不可用按既有离线流程跳过。第一次测试失败是测试夹具把奖励物品本身用作背包填充物并误计数量；改用独立 filler 项后通过。此前一次把 `--tests` 施加到所有子项目，非根 UI 子项目因无匹配类失败，未进入目标测试；均非生产代码故障。
- 本批只有测试和文档变化；没有改奖励算法、数据库合同或迁移。未运行全量测试、`assemble`、自包含门禁或 Forge/GTNH；未访问生产玩家进度/数据库、执行 DDL、部署或切换 Quest/BQ。World drop 已生成而玩家标记尚未落盘的崩溃窗口、真实跨服玩家存档/租约仍未验证；M.2 保持 `BLOCKED`。

### 2026-10-11 - R.2.28 未知 optional 命名任务导入 fail-closed

- CodeGraph 起始索引 up-to-date；`explore`/`impact` 覆盖 `BqStandardCodec.task/supportedTaskTypes`、`BqQuestDefinitionImporter.read`、`QuestEngine.evaluate` 及未知类型兼容报告，`affected` 给出 20 个宽候选，直接验证收敛到 Importer 模块。旧逻辑回归先失败：`addon:optional_special` 因 `type.contains("optional")` 被静默视为可选任务。
- 修复为 `BqStandardCodec.isOptionalTaskType` 精确识别 `bq_standard:optional_retrieval`。新断言确认未知命名 Task 的 `optional=false`、即使没有观察进度也不会让 Quest 变为 COMPLETED（状态保持 AVAILABLE），兼容报告仍标为 UNKNOWN。已知 Optional Retrieval 原有 importer→库存快照→QuestEngine 用例继续覆盖真实 optional 语义。
- Docker `galaxy-dev:local`，`--network none`，使用仓库已有 Gradle cache 且构建输出隔离到 `.gradle/codex-r2-confirmed-build`，重跑 `:galaxy-quest-bq-importer:test`：51 项总计，44 执行、7 assumption-skip、0 failure/error；7 项均因未设置隔离 `QUEST_TEST_POSTGRES_URL`，未触及数据库。前两次离线容器尝试分别因 wrapper 缓存路径不匹配、settings 插件依赖元数据不齐而未进入测试；改用仓库已有完整缓存后测试完成。
- 仅修改 importer 语义与测试及进度文档；未执行 PostgreSQL、根 Forge 构建、`assemble`、自包含检查，未访问生产玩家进度/数据库，未做 DDL、部署或 Quest/BQ 开关操作。R.2.19 Party 事件归属与 Detect 版本发布并发待决，M.2 仍 BLOCKED。

### 2026-10-11 - R.2.27 Optional Retrieval typed import 到完成语义回归

- 固定 BQ `TaskOptionalRetrieval.ignored(UUID)` 返回 true，且继承非消耗 `TaskRetrieval` 的库存观察行为。CodeGraph 起始 `status` up-to-date；`explore` 核对 `BqQuestDefinitionImporter.read`、`TaskDefinition.isOptional`、`InventorySnapshotTaskEvaluator.evaluate` 与 `QuestEngine.evaluate`；`impact` 覆盖 importer/QuestEngine，`affected` 将验证范围收敛到 importer 与 Core 对应测试。
- 新增跨模块回归：从合成 typed BQ JSON 导入必需 Checkbox + `optional_retrieval`，由库存快照 evaluator 推进可选任务至部分进度，再由 Core QuestEngine 确认必需任务完成即完成 Quest，且 optional Task 保持未完成。未修改生产代码；补齐的是 importer 类型映射和 Core 忽略规则此前未在一个场景串联的测试边界。
- `galaxy-dev:local` / `--network none` / 隔离输出强制执行 `:galaxy-quest-bq-importer:test --tests com.jsirgalaxybase.quest.bqimport.BqQuestDefinitionImporterTest --rerun-tasks`：5 项，0 skipped/failure/error，`BUILD SUCCESSFUL`。本用例不访问数据库；未跑 PostgreSQL、Forge 根测试、assemble 或 self-contained。`git diff --check` 通过。
- 不将窄自动回归宣称为 Optional Retrieval 或 M.2 行为等价；Forge/GTNH 背包事件、在线 Party 快照与实机运行仍未验收。无生产玩家进度/数据库访问、DDL、部署、Quest/BQ 开关或 S1/S2 操作。

### 2026-10-11 - BQ Observation Station 非消耗任务边界复核

- 固定源码 `TileObservationStation.updateEntity` 每 20 tick 在服务端检查站点 owner 是否在线，只扫描六邻方块中的 `IInventory`、`IFluidTank` 和 `IFluidHandler`；随后用 owner 的 `ParticipantInfo.ALL_UUIDS` 遍历共享 Quest。
- 对照 `TaskRetrieval.retrieveItems`、`TaskFluid.retrieveItems` 与 `TaskFluid.retrieveFluids`，这些路径都在任务为 consuming 或 owner 已完成时立即返回。由此确认 Observation Station 是非消耗库存/流体存在检测入口，不是 Quest 级消耗 Detect 的另一入口，也不会从邻接容器扣除物品或流体。
- CodeGraph `status` up-to-date；`explore` 覆盖 Base 的 `MinecraftQuestFactFactory`、`BqPartyInventorySnapshotFactSink`、`QuestGameplayEventHandler` 与 `QuestObservationPlan` 调用链。BQ Reference 源码不在 CodeGraph 索引中，因此对其精确方法做只读源码核对。文档已同步更新能力矩阵、M.2 行为矩阵和开发导航。
- 当前 Base 仍没有 Observation Station 世界入口；DefaultQuests 不包含方块放置/使用证据，不能推断 S2 玩家依赖该机制。按此前“背包主线、辅助入口延期”的范围决定，本轮没有增加方块、扫描或进度代码；该差异继续明确记录为 BLOCKED。未访问生产玩家进度/数据库、执行 DDL、部署或改变 Quest/BQ 开关。

### 2026-10-11 - Crafting 来源归并与 PARTY 资格当前树强制复验

- 按 `jsirgalaxybase` 规程先检查 CodeGraph：索引 up-to-date。`explore` 覆盖 `AuthenticatedCraftingStatisticsDetectionService`、`BqPartyCraftingStatisticsScope`、`TaskProgress`、`QuestEvaluationPlanner`；`impact` 分别确认统计服务、PARTY 授权与来源进度归并的调用范围。`affected` 给出 94 个宽候选，直接相关回归在 Core 统计服务、Crafting evaluator、Party 活跃资格、观察计划、TaskProgress 与 QuestEngine 测试。
- 在 `galaxy-dev:local` 的 `--network none` 容器中，使用 `.gradle/codex-r2-confirmed-contracts-20261011.init.gradle` 把构建产物隔离到忽略目录，强制重跑 `:galaxy-quest-core:test --rerun-tasks`：62 个测试套件、289 项，0 skipped/failure/error。该批复验来源服各取 `max(event, absolute)` 后求和、不同玩家统计隔离、PARTY 在线资格与自动统计观察计划；没有重跑 Forge 根事件/事务集成或 PostgreSQL 集成。
- 当前用户确认的 Quest 级 Detect 与来源服计数规则符合已有实现和上述 Core 回归；本轮没有改动业务代码或数据库合同。源码审查另确认两项仍待收口：Crafting 重试时 Party 归属选择尚待用户确认；活动 Detect 批次按当前定义版本查找，若管理员发布新版本，旧批次可能失去恢复/结算入口，发布并发策略尚未确定。M.2 维持 `BLOCKED`。
- `git diff --check` 通过。未访问生产进度/数据库、未执行 DDL、部署、Quest/BQ 开关或操作任何服务器；本轮没有运行 `assemble`、自包含门禁、Importer、Forge 或 PostgreSQL 测试。

### 2026-10-11 - GT 机器产物的库存型任务路径复核

- 按用户刚确认的语义，GT 机器产物不伪造成 craft/smelt/anvil 事件；若设计为物品/流体存在任务，应由玩家背包或已明确接入的玩家容器快照观察。CodeGraph `status` 为 up-to-date；`impact InventorySnapshotTaskEvaluator.evaluate` 覆盖 14 个符号，`affected` 返回 80 个宽候选。直接核对 `QuestObservationPlan` 对非消耗 BQ Retrieval 开启库存周期观察、`QuestGameplayEventHandler` 每 20 tick 采集玩家 `EntityPlayer.inventory`，而 Crafting 事件/统计走独立事实通道。
- 在无网络 `galaxy-dev:local` 容器内，将 Core 模块源码复制到临时目录（不复制受权限保护的旧 build 产物），运行 `BqCompatibleTaskEvaluatorsTest` 与 `QuestObservationPlanTest`：2 suite / 53 项通过，0 skipped/failure/error。覆盖绝对库存快照、非消耗 Retrieval、消耗任务拒绝自动快照、Crafting 统计目标与库存观察计划。此前整模块复制因旧 root-owned 测试结果不可读而退出，未触碰或改写该产物。
- 未发现与该项用户合同冲突的 Core 行为，因此没有再加重复实现；本次不是 Forge/GTNH 容器实测，当前 Minecraft adapter 只接入玩家背包，其他容器必须显式增加安全适配器。R.2.19 的延迟事实 Party 归属已单独再次请求用户选择；M.2 仍 BLOCKED。未访问数据库/玩家进度、执行 DDL、部署或更改 Quest/BQ 开关。

### 2026-10-11 - BQ Importer 隔离 PostgreSQL 集成全量补验

- 由于此前 Importer 50 项中 7 项因未配置隔离 PostgreSQL URL 按设计 assumption-skip，本轮补齐环境而不改测试护栏。CodeGraph 索引保持 up-to-date；`impact BqMigrationDatabaseService.apply` 覆盖 8 个符号，`affected` 收敛到 `BqBehaviorInventoryTest` 与 `BqMigrationDatabaseIntegrationTest`，后者含 6 个 apply/dry-run/rollback/JDBC 端到端用例。本轮未改 Java。
- `:galaxy-quest-bq-importer:test` 在临时 PostgreSQL 16 上实际运行：13 suites / 50 tests，0 skipped/failure/error。数据库只对一次性 Docker internal network 可见，无 host port、无宿主数据卷；`quest_importer_test` 名称和显式 `QUEST_TEST_POSTGRES_ISOLATED=true` 均通过测试端隔离校验。执行后查询 `bq_migration_it_%` schema 数为 0，Importer teardown 已删除测试 schema。
- 临时容器使用 512 MiB tmpfs；执行后已销毁容器、internal network 与数据库数据。未连接生产 Compose 网络或 `galaxy-base`，未访问玩家进度、执行生产 DDL、部署、启用 Quest/停用 BQ，也未操作 S1/S2。该结果只补全 Importer 自动集成证据，不解除 R.2.19、Detect 版本发布边界或 M.2 的 BLOCKED 状态。

### 2026-10-11 - R.2 Retrieval 源码行为交叉审计

- CodeGraph `status` 为 up-to-date；`explore TaskRetrieval/InventorySnapshotTaskEvaluator/BqPartyInventorySnapshotFactSink`、`explore TaskProgress.replaceIncomplete/groupDetect`、`impact InventorySnapshotTaskEvaluator.evaluate` 与 `affected` 核对 Base 当前评估链。上游 `Reference/BetterQuesting` 未纳入 CodeGraph 符号索引，因此直接读取仓库内现有 BQ 源码副本 `TaskRetrieval` 与 `TaskOptionalRetrieval`。BQ 非消耗 `detect` 遍历在线 `ACTIVE_PLAYERS`，`groupDetect` 决定重算全部或仅未完成分量；`Detector.run` 按需求顺序将单栈数量拆给各需求；`onInventoryChange` 对所有非消耗 Retrieval 自动检测；Optional Retrieval 的 `ignored()` 恒为 true。
- Base `InventorySnapshotTaskEvaluator` 使用服务端库存观察，不把快照转换为增量；普通模式保留达到目标的分量，groupDetect 重算不完整任务中的需求。已存在的 Core 回归覆盖非消耗回落、groupDetect 聚合、NBT/Meta/OreDictionary 规则、重复需求单栈分配、首见槽位争用和 optional 不阻断 Quest；真实 Party 快照则由 fencing session/有效 lease 聚合到每位成员自己的 PLAYER 进度。消耗型 Retrieval 走本轮已确认的 Quest 级持久 Detect 批次。`impact` 命中 14 个符号，`affected` 返回 80 个宽候选，直接相关用例位于 `BqCompatibleTaskEvaluatorsTest`、根 PARTY/JDBC 与消费适配回归。
- 本轮没有发现需要根据已审阅 S2 参数画像立即修复的新的 evaluator 差异，未改代码、未重跑上述 Retrieval 测试。既有 `TaskObservationStation` 六邻块 `IInventory/IFluidTank/IFluidHandler` 能力未移植，静态任务定义不能证明玩家是否实际使用；`fullySyncQuests` 实际配置未知且未读取。根 Forge 测试此前因插件拉取无进展未启动；生产数据库、玩家进度、DDL、部署和 Quest/BQ 开关均未触及，Retrieval 与 M.2 仍 BLOCKED。

### 2026-10-11 - Crafting 跨服与 Quest Detect 合同确认续记

- 用户明确再次确认：消耗型库存任务采用单一 Quest 级 Detect，按发布定义顺序处理未完成任务；同一玩家 Crafting 统计按每个来源服分别取 `max(该服事件累计, 该服绝对统计快照)` 后求和；不同 Party 成员进度仍独立为 PLAYER。CodeGraph 本轮核对 `QuestGameplayEventHandler`、`QuestObservationPlan`、统计检测服务、`QuestEvaluationPlanner`、`BqPartyActiveQuestQuery` 与 `TaskProgress.observeFromSource`；既有实现符合这两项确认，未重复改运行合同。
- 影响范围：`impact` 统计自动检测、Party 活跃资格与来源进度归并；`affected` 返回 94 个宽候选，按直接调用链选择 Core evaluator/统计/Party/观察计划/来源进度覆盖。隔离 Docker 强制重跑 Core：289 项，0 skipped/failure/error。Importer 当前已验收 50 项、43 执行、7 项因缺少隔离 `QUEST_TEST_POSTGRES_URL` assumption-skip、0 failure/error；其输出身份脱敏修复已有回归。
- 根 Forge 定向 `QuestCraftingStatisticsDispatchTest` 本轮没有完成：一次离线尝试先因 GTNH settings convention plugin 未缓存而在配置阶段退出；通过本地 7892 代理的临时容器重试超过两分钟无输出后，仅中断了本次启动的 Gradle 进程（exit 130）。两次均未进入测试用例，不计为测试失败或通过。Qz LGPL 许可证 SHA-256 与构建声明一致，下载任务被排除；无网络/服务配置更改。
- 仅修订开发导航与 M.2 行为文档中的过期描述，使历史记录保留“当时状态”而不再覆盖后续决策。R.2.19 事件事实排队期间 Party 变化的归属仍是另一项未决合同，尚未获用户答复；未改变代码或 schema。未访问生产数据库/玩家进度、未执行 DDL、部署、Quest/BQ 开关或服务操作；M.2 仍 BLOCKED。文档修改后执行 `git diff --check`。

### 2026-10-11 - Crafting PARTY 统计与来源账本当前树复核

- CodeGraph `status` 为 up-to-date；`explore` 核对 `QuestGameplayEventHandler.onPlayerTick/submitCraftingStatistics`、`QuestObservationPlan.fromPublished`、`AuthenticatedCraftingStatisticsDetectionService.detectAutomatically`、`MinecraftCraftingStatisticReader.craftedCount`、`QuestEvaluationPlanner.requestsFor`、`BqPartyActiveQuestQuery.hasActiveMember`、`TaskProgress.observeFromSource` 与 JDBC JSONB 持久化链。每 60 tick 的检测由本服在线玩家各自触发，读取其 vanilla `objectCraftStats`；合成事件则按当前 Party 在线 roster 为每位目标玩家分别采样。Planner 只对来源服 Party 中存在有效在线成员的匹配事实建立请求；进度主体仍为各自 PLAYER，不合并 Party 成员统计。来源服统计通过每来源 event/absolute max、来源间 sum 存入 `source_progress_json`。
- `impact` 覆盖自动检测、Party 活跃资格和来源合并；`affected` 给出 94 个宽候选，直接测试范围收敛到 Core 的 Crafting evaluator、统计服务、Party active query、ObservationPlan 与 TaskProgress 测试。Docker `galaxy-dev:local`、`--network none`、隔离输出目录强制重跑 `:galaxy-quest-core:test --rerun-tasks`：289 项，0 skipped/failure/error。PostgreSQL 与 Forge 根事件测试本轮未重跑；不将历史 JDBC/根测试结果记成本轮结果。
- R.2.19 仍取决于用户对延迟事实跨 Party 变更时的归属选择；已发出单独确认，未改事实/Planner 合同。`git diff --check` 在本轮文档修改后通过。无 DefaultQuests 新读取、玩家进度/数据库访问、生产 DDL、部署、Quest/BQ 开关或 S1/S2 操作；M.2 仍 BLOCKED。

### 2026-10-11 - 修复 BQ 行为盘点 CLI 的逐任务身份泄漏

- CodeGraph `impact BqInventoryMain` 确认影响局限于盘点入口；修改后 `sync` 更新 2 个 Java 文件，`affected` 收敛到 `BqBehaviorInventoryTest` 与新增 `BqInventoryMainTest`。首次回归暴露除 manifest 逐条清单外，`BqBehaviorInventory` 还会输出最多三个 `sampleQuestIds`；已删除该身份收集/输出，并将盘点 CLI 的 manifest 细节改成 Quest/章节总数及按严重级别和问题代码的聚合计数。普通导入规划所需的详细 manifest 合同未改。
- Docker `galaxy-dev:local`、`--network none`、离线运行 `:galaxy-quest-bq-importer:test`：50 项，43 执行、7 assumption-skip、0 failure/error；7 项均因未设置隔离用 `QUEST_TEST_POSTGRES_URL` 而跳过数据库集成。新增真实 CLI 输出回归验证不含 Quest 名称、UUID 或逐 Quest 行，同时保留匿名类型计数。此次不读取 S2 定义，故没有分享或转存任何任务内容。
- 不改变 M.2/替代状态：Crafting 来源服 max/sum 与 Quest 级消耗 Detect 仍按用户确认的合同；R.2.19 的 Party 延迟事实归属仍未确认。未跑 Core/Postgres/root build、未执行 DDL、部署、启用 Quest/停用 BQ、访问玩家进度或操作 S1/S2。

### 2026-10-11 - R.2.26 simultaneous Quest Detect 转换边界

- CodeGraph 起始 `status` 为 up-to-date；此前已检查 `QuestEngine.evaluate`、`QuestRuntimeService.reconcileAfterConsumptionDetectBatch` 与 `AuthenticatedQuestConsumptionDetectService.detect` 的调用/影响链，本次修正限于 Core 进度归约与批次末结算。`QuestEngine` 只在本轮观察到 incomplete→complete 转换且 Quest 逻辑仍不满足时重置 `SIMULTANEOUS`；批次结算使用批次选中的 Task keys 还原此前已持久化的转换信号。新增 Core 回归覆盖“旧完成任务不触发重置”以及“延迟 Quest Detect 结算能识别新完成任务”。
- Docker `galaxy-dev:local`、离线且隔离至 `.gradle/codex-r2-confirmed-build` 执行 `:galaxy-quest-core:test`：289 项、0 skipped/failure/error。CodeGraph 覆盖的影响范围为 Core Engine、Runtime、认证 Detect 服务和对应测试；本轮未重跑 PostgreSQL 集成、根 `assemble` 或自包含门禁，不能把之前 Worklog 的构建结果算作本轮验收。只读 S2 定义用匿名聚合统计得到 3,744 个 Quest、0 个启用 `simultaneous`，该通用 Core 语义因此没有当前 S2 实例可做内容对照。
- 调查行为频率时运行了一次 `behaviorInventory`，发现 stdout 泄漏逐 Quest 名称和 UUID；原始输出未转存，频率结论改由只输出总数的统计取得。该 CLI 的脱敏缺陷已在紧随本条的 2026-10-11 Worklog 记录中修复并由 importer 回归验证。本轮无生产玩家进度/数据库读取、生产 DDL、部署、Quest/BQ 开关变化或 S1/S2 服务操作；R.2.19 的延迟 Crafting Party 归属仍待用户选择，M.2 与 BQ 替代继续 BLOCKED。

### 2026-10-11 - M.2 Quest Detect 与 Crafting 来源账本当前树复验

- CodeGraph `status` 为 up-to-date；`explore` 核对 `AuthenticatedQuestConsumptionDetectService`、`QuestEngine`、多来源 Crafting max/sum Core/JDBC 测试和批次游标集成测试。固定 BQ `TaskRetrieval.detect` / `QuestInstance.detect` 复核仍表明：Quest Detect 按定义顺序遍历未完成 Task；默认 `fullySyncQuests=false` 时 consuming Retrieval 读取发起者背包并写发起者进度，Party 非消耗快照另按 BQ 活跃成员路径处理。当前实际服务器配置值没有读取。
- 以 `galaxy-dev:local` 在 `--network none` 容器强制执行 `:galaxy-quest-core:test`：286 项、0 跳过/失败/错误。随后在临时 PostgreSQL 16 上执行 `:galaxy-quest-core:test :galaxy-quest-postgres:test`：Core 286 + Postgres 60 = 346 项、0 跳过/失败/错误。Postgres 使用 `_test` 库名、显式隔离标记、512 MiB tmpfs、无数据卷/发布端口，仅连接内部 Docker 网络；复验过程未接入生产 Compose 网络。
- 首次容器运行因共享 Gradle 缓存的配置缓存锁权限未进入 Gradle；未改缓存权限，改以容器 root 读取既有缓存并把 build 输出隔离至 `.gradle/codex-goal-check/build` 后通过。另一条测试启动脚本在 shell 解析阶段失败，未创建容器/网络或执行 Gradle。最终测试后已删除临时 PostgreSQL 容器/网络、init script 和本次生成的隔离 build 输出。
- `codegraph affected` 为 Forge 根整合选中 `BqPartyInventorySnapshotFactSinkPostgresTest`；该类测试启动未进入测试阶段：GTNH `applyJST` 要求 Azul JDK 21，`galaxy-dev:local` 仅有 Temurin 25、宿主仅有 OpenJDK 17，离线模式下 Foojay 无法补齐工具链。未更改项目工具链或连接外网；临时 PG16 容器与网络随后清理。此项是环境阻断，不记作测试失败或通过。
- 本次为当前树验证，无生产源码修改；未重跑 `assemble`/自包含门禁。未访问生产数据库/玩家进度，未执行生产 DDL、部署、Quest/BQ 开关或操作 S1/S2。Crafting 延迟事实遇玩家 Party 变化时按事件时或重试时归属仍待用户确认；M.2 与整体 BQ 替代继续 BLOCKED。

### 2026-10-11 - PARTY groupDetect 当前 Party 库存重算隔离复验

- 重新核对实际 Retrieval `groupDetect=true` 差异。CodeGraph `status` 为 up-to-date；`explore` 查看 `BqPartyInventorySnapshotFactSink`、`BqPartyInventorySnapshotScope`、`ItemVectorTaskEvaluator`、`TaskProgress` 与 Planner，`callers/impact BqPartyInventorySnapshotFactSink.accept` 显示生产聚合链及 `groupDetectPartyRetrievalReplacesProgressFromCurrentPartyInventory` JDBC 回归。当前实现已覆盖新 Party 快照替换旧的未完成分量；该行为此前已有实现，本轮未改业务代码。
- Docker `galaxy-dev:local` 使用隔离 Gradle 输出 `.gradle/codex-r2-current-build` 重跑 `:galaxy-quest-core:test`：284 项、0 skipped/failure/error。初次写入旧结果目录遇 `results-generic.bin` 权限拒绝，未修改权限或覆盖旧文件。随后单独重跑根 PostgreSQL 用例 `groupDetectPartyRetrievalReplacesProgressFromCurrentPartyInventory`：1 项、0 skipped/failure/error；临时 PG16 仅用 512 MiB tmpfs、无宿主卷、无端口发布、internal Docker network，结束后已删除容器和网络。第一次根任务仅因离线许可证下载 `prepareUi2QzLicense` 失败而未进入测试；最终只排除该下载任务后成功，未更改许可证或 JAR 内容。
- `git diff --check` 在本记录加入后执行。此次是对已实现行为的当前工作树复验，不升级 Retrieval/M.2 兼容状态；Forge/GTNH 库存与 Party 时序仍未验证，R.2.19 Crafting 延迟事实归属仍等待用户选择。没有访问生产进度/数据库、执行生产 DDL、部署、切换 Quest/BQ 或操作 S1/S2。

### 2026-10-11 - R.2.22 Crafting 重复周期来源事件隔离

- 固定 BQ `QuestInstance.resetUser(uuid, false)` 会逐 Task 调用 `resetUser(uuid)`，新重复周期不继承旧 Task 进度。Base `QuestEngine.evaluate` 在冷却到期时以空 cycle 基线重算观察。Core 回归验证旧周期 S2 event 完成后，新周期仅含 Lobby 1 个 event；隔离 JDBC 回归验证 cycle 1 upsert/reload 不会让旧 S2 `source_progress_json` 复活，之后新采到的 S2 绝对统计 3 会重新参与聚合（总值 4，S2 event=0、observed=3）。第二个 JDBC 往返用例验证 Lobby event=3/absolute=5 只贡献 5，S2 event=4/absolute=2 贡献 4，重读总值为 9，确认来源内 max、来源间 sum 且不双重计数。
- CodeGraph `status` 起始 up-to-date；检查 `TaskProgress.empty/advanceFromSource/merge`、`QuestEngine.evaluate`、`QuestRuntimeService.execute`、`JdbcQuestRuntimeRepository.saveProgress`。`impact QuestEngine.evaluate` 覆盖 21 个符号、`impact JdbcQuestRuntimeRepository.saveProgress` 覆盖 17 个符号；宽候选经调用链收敛到 Core `QuestEngineTest` 与 PostgreSQL `QuestPostgresIntegrationTest`。最终 `sync` 更新 PostgreSQL 集成测试 1 文件/146 nodes；`affected` 返回该集成测试文件。
- Docker `galaxy-dev:local` 离线运行完整 `:galaxy-quest-core:test`，XML 汇总 **283 项、0 skipped、0 failure、0 error**；隔离 PostgreSQL 定向集成 **2 项、0 skipped/failure/error**。第一次 JDBC 启动因临时 Gradle init 脚本已清理而在任务配置前退出，未执行测试；恢复隔离脚本后通过。PG16 临时容器 `jgb-r222-cycle-it-20261011` 使用 512 MiB tmpfs、无宿主数据卷、只发布到 `127.0.0.1:55521`；测试后已销毁并确认端口释放。`git diff --check` 通过。本轮仅修改测试与文档，没有改生产 Java/JDBC/schema；未跑根 `assemble`/自包含门禁，最近生产代码门禁沿用紧邻的 R.2.21。
- 固定 BQ 绝对 `objectCraftStats` 属于玩家生命周期计数，不会由 `resetUser` 清零。回归证明它可在新周期重新观察，但不代表真实 Forge/GTNH 统计读取已验证。未访问生产数据、执行 DDL/部署、改变 Quest/BQ 开关或触碰 S1/S2；R.2.19 Party 重试归属和 M.2 整体继续 BLOCKED。

### 2026-10-11 - R.2.21 Hunt 新事件计数上限

- 固定 BQ `TaskHunt.onKilledByPlayer()` 对匹配击杀使用 `min(required, current + 1)`。CodeGraph `status` 起始为 up-to-date；检查 evaluator、Forge 击杀入口及固定参考实现的 `node/explore`、`impact` 与 `affected` 将本次影响收敛到 `SubjectCountTaskEvaluator` 和 `BqCompatibleTaskEvaluatorsTest`。
- Base 通用 Hunt evaluator 原先按事实 amount 累加，允许批量事实使新进度超过目标。现将新观察限制到目标值；`TaskProgress.merge` 保留历史已持久化的超目标值，不会倒退。新增目标 2/amount 7 的封顶回归，并断言历史 5/2 不被改写。
- Docker `galaxy-dev:local` 隔离运行 `:galaxy-quest-core:test`，实际 282 项、0 skipped/failure/error。离线构建复用 SHA-256 已核验的缓存 Qz 许可证，仅排除许可证下载任务；根 `:assemble :verifyUi2SelfContained` 成功，35 actionable tasks（23 executed、12 up-to-date），自包含检查实际执行。隔离运行 JAR SHA-256：`ecefbb0b0968a73f425050c768e0516d0b77e973721ccf076b2771d7e975ba42`。最终 `git diff --check` 通过。
- 本次未新增 JDBC 用例、未运行 PostgreSQL、未访问生产数据、未执行 DDL/部署/Quest-BQ 开关，也未触碰 S1/S2。新事件计数差异已有纯 Core 自动回归；真实 Forge `LivingDeathEvent`、FakePlayer/取消事件、GTNH EntityList/NBT 与 PARTY 时序仍 BLOCKED。R.2.19 延迟事实的 Party 归属时点继续等待用户决策，Hunt/M.2 与 BQ 替代整体仍 BLOCKED。

### 2026-10-11 - Crafting Party 事件归属证据复核

- 按 `jsirgalaxybase` 项目工作规程先检查 CodeGraph；索引 up-to-date。`explore`/`impact` 核对 `TaskCrafting`、`ParticipantInfo`、`QuestGameplayEventHandler.submitCraftingStatistics`、`OrderedQuestFactRetryQueue`、`MinecraftQuestFactFactory.craftingStatisticObserved`、`QuestEvaluationPlanner.bqPartyActivityMembership`。固定 BQ 源码显示：产物事件用事件时 `ParticipantInfo.ALL_UUIDS` 扇出任务进度，统计复评才按当时在线成员分别读取个人统计。
- Base 在事件处理期间固定在线目标 UUID，并同步生成不可变事实；失败时队列重试的是该事实本身。但事实没有包含事件时 Party ID，规划器会按事实交付时的成员关系解析 Party。由此可复现一个未封口的合同边界：数据库不可用期间目标玩家改变 Party，积压事实可能归到新 Party。用户刚确认的“来源服最大值后求和”不决定这一归属规则。
- 已在 M.2 行为矩阵中追加源码证据，R.2.19 继续标记 BLOCKED。正在等待用户选择“冻结事件时 Party”或“按重试时 Party”，收到明确合同前不改进度归属算法；此次仅更新文档，没有修改运行代码，也未声称新增测试通过。本轮未访问生产进度/数据库、执行 DDL、部署、改变 Quest/BQ 开关或操作 S1/S2。

### 2026-10-11 - Crafting 与 Quest Detect 用户合同复核

- 用户确认 Quest 级单一消耗 Detect；非消耗库存任务仍自动观察。Crafting 统计按每名玩家、每来源服分别取 `max(事件累计, 本服绝对统计快照)` 后求和，Party 成员之间不合并。当前实现已有 Quest 批次协调器与 source-scoped TaskProgress；此轮只复核合同，没有再改运行代码或数据库。
- CodeGraph 起始 `status` up-to-date；`explore` 检查 `AuthenticatedQuestConsumptionDetectService`、`JdbcConsumptionDetectBatchRepository`、`BqPartyCraftingStatisticsScope`、`TaskProgress` 与统计采样服务，`impact` 确认 Quest Detect 批次的 Core/JDBC/UI2 调用链，以及 Crafting PARTY scope、planner/runtime 和对应回归范围。
- 首次定向 Core 测试未进入用例：历史 `build/test-results` 文件不可读。未改权限或清理旧文件；改为新的忽略构建输出 `.gradle/codex-r2-confirmed-build` 后，Docker `galaxy-dev` 隔离运行 `AuthenticatedQuestConsumptionDetectServiceTest`、`BqCompatibleTaskEvaluatorsTest`、`TaskProgressVectorTest` 共 52 项，0 skipped/failure/error。该批不含 JDBC 集成或全量构建；既有隔离 JDBC 证据见此前 R.2.6/R.2.17 Worklog 记录。
- R.2.19 是独立未决合同：Crafting 事实在数据库故障入队后，Party 变更时究竟冻结事件时资格，还是按数据库接收/重试时成员关系计算。上述答案只确认来源计数合并和 Quest Detect，不据此推断该归属选择。未访问生产玩家进度/数据库、未执行 DDL/部署/Quest-BQ 开关或 S1/S2 操作。

### 2026-10-11 - R.2.17 行为口径收口与隔离完整复验

- 按用户确认收口两项行为：玩家页面只提供 Quest 级单一消耗 Detect；非消耗库存持续自动观察，Crafting 统计由匹配产物事件与 60 tick 自动复评，不再出现手动“检测合成”。旧 `QUEST_DETECT_CRAFTING` ID 保留解码兼容，但只返回自动复评说明，不修改进度；`QUEST_DETECT_CONSUMPTION` 才创建/恢复持久批次。Crafting 统计跨服按每来源分别取 `max(本服事件累计, 本服绝对统计快照)` 后求和，不合并不同玩家的个人进度。
- CodeGraph `status` 起始为 up-to-date；检查 `QuestGameplayEventHandler.submitCraftingStatistics` callers/callees、`AuthenticatedQuestConsumptionDetectService` impact（26 符号）、`TerminalService.buildAuthenticatedQuestCenterSnapshot` callers（9）/impact（11 符号）及 `TerminalQuestCenterSnapshotMapper.detail` impact（4 符号）。`affected` 对组合根返回 141 个宽泛候选；按调用链覆盖 Core、Quest Postgres、Importer、UI2 Terminal 与根测试。
- 当前全量 `test` XML：根项目 592 项，67 项条件跳过、525 项执行，0 failure/error；模块 Core 278、Postgres 58、Importer 49、UI2 Core 41、UI2 Lab 2、UI2 Terminal 28、UI2 Demo 5 均通过。Postgres/Importer 任务首次在无数据库环境运行时有 42/7 项 assumption-skip；随后在隔离库中 `--rerun-tasks` 全模块，相关 58/49 项全部实际执行。根 `BqPartyInventorySnapshotFactSinkPostgresTest` 在同一隔离库运行 15 项，0 skip/failure/error。`:assemble`、`verifyUi2SelfContained` 通过；`:ui2-demo:renderGolden` 首次缺只读预览字体未进入渲染，按既有流程只读挂载宿主 CJK 字体重跑通过。
- 临时 PostgreSQL 16 使用 `quest_r217_test`、512 MiB tmpfs、无宿主数据卷，只绑定 `127.0.0.1:55497`；任务结束后容器已删除且端口释放。Gradle 通过一次性 host-network 容器访问已存在的本地 7892 代理；未改变代理/网络配置。未访问生产进度/数据库、未执行生产 DDL、部署、Quest/BQ 开关或 S1/S2 操作。最终 `git diff --check` 通过。
- M.2 仍 `BLOCKED`：Quest Detect 仅涵盖 PLAYER consuming Retrieval/Optional Retrieval/Fluid 子集；真实 Forge/GTNH 背包与事件时序、切服/源服停机恢复、物品存档与 entitlement 的崩溃边界未通过实机验证。来源服 JSONB migration 仅隔离测试，生产 DDL 未执行。

### 2026-10-11 - R.2.17 Quest 级消耗 Detect 批次协调

- 用户确认两项合同：Quest 级单一 Detect，按任务定义顺序处理未完成消耗 Task，逐项持久化并可恢复，不承诺跨玩家库存/数据库全有或全无；同一玩家多服 Crafting 来源分别取 `max(本服事件累计, 本服绝对统计快照)` 后求和。M.2 Quest/Party 主体模型不变。
- CodeGraph 起始 `status` 有待同步文件；先 `sync` 17 个相关文件，再对 `AuthenticatedQuestConsumptionDetectService`、`QuestRuntimeService.reconcileAfterConsumptionDetectBatch`、`ConsumptionRequestAuthorizer.planQuestDetect`、`JdbcConsumptionDetectBatchRepository` 执行 `impact`。影响链落到 Core 协调/Runtime/授权、Postgres 批次仓储、QuestModule 组合根、TerminalService 与 Quest UI2 动作。最终新增周期回归后 `sync` 再更新 4 个文件；`affected` 对源/测试/接线文件给出 141 个宽泛测试候选，按具体调用链选 Core、Quest Postgres、UI2 Terminal 和根 Quest 全量测试。
- 新增服务器认证的 Quest 级协调器：读取已发布定义并创建稳定的 Task 顺序，活动批次持久化源服、玩家、参与主体、Quest 版本、周期、Task 列表与游标；每项使用 `batchId:index` 作为原幂等键，并复用现有单 Task 消费 Saga。批次中暂缓 OR/Quest 完成和奖励，全部 Task 到达终态后在同一 PostgreSQL 事务中删除批次并做最终 Quest 结算。源服绑定限制避免另一台服务器访问不存在的本地玩家库存；玩家切服时展示等待源服而不伪造恢复成功。
- 新增/扩展回归覆盖两项消耗任务顺序、第二项故障后游标与稳定键恢复、OR Quest 不提前完成/发奖、跨源服不执行库存副作用、奖励未领取时重复 Quest 周期不提前开放，以及无 Task 事实持久化时按批次绑定的下一周期结算。修复授权计划原先忽略 claimable reward repeat anchor 的问题；最终结算允许仅在旧周期确已到期且绑定下一周期时从旧完成行启动该周期。UI2 只展示单一 Quest 动作。
- 新增 `20261010_002_add_quest_consumption_detect_batch.sql` 与基线 schema；`JdbcQuestCenterQuery.validateSchema` 要求批次表存在以 fail closed。临时 PostgreSQL 16 集成实跑从空表重复应用 migration、schema validation、活动批次唯一约束和游标幂等推进。此迁移没有在生产库执行，部署仍需审批并应用迁移。
- Docker Gradle 完整回归实际执行 Core 278、Quest Postgres 58、UI2 Terminal 28、根项目 591，共 955 项；52 项根层可选 PostgreSQL 集成因各自独立环境配置缺失而 assumption-skip，0 failure/error，实际执行 903 项。Quest Postgres 58 项在新建 `quest_detect_test` 隔离 PostgreSQL 16 容器中全部执行、0 跳过。`compileJava :assemble verifyUi2SelfContained` 成功（39 actionable tasks，12 executed、27 up-to-date）；其后新增 claim-cooldown Core 回归，单独重跑 Core 全套 278 项成功。首次构建完整集成时报告 277 个 Core 项；差异仅为后来补加的这 1 个测试。Java 8 source/target 和 JDBC deprecated API 只有编译警告。
- 临时数据库仅用 512 MiB tmpfs、无宿主数据卷、loopback `127.0.0.1:55498`，测试后容器已停止自动删除并确认端口释放。没有访问生产玩家进度/数据库、执行生产 DDL、部署、启用 Quest、停用 BQ 或触碰 S1/S2；真实 Forge/GTNH 背包操作、源服停机切服和长期运行仍待验证。`:git diff --check` 与文档数字同步后复核。
- 当前结论：R.2.17 自动门槛完成，但只覆盖授权的 PLAYER consuming Retrieval/Optional Retrieval/Fluid 子集，不是完整 BQ Quest Detect 兼容；M.2 与 BetterQuesting 替代继续 `BLOCKED`。开发导航、唯一能力矩阵、M.2 行为矩阵已标记该限制与生产迁移未执行状态。

### 2026-10-10 - Crafting 来源计数迁移与 Quest Detect 时序复核

- CodeGraph `status/sync` 纳入新增隔离 PostgreSQL 迁移回归；`explore` 核对 `QuestInstance.detect` 对照证据、`QuestRuntimeService.execute` 的即时完成/奖励路径、`JdbcQuestRuntimeRepository` 与来源进度迁移。`impact TaskProgress` 显示 202 个关联符号；`affected` 对新增测试选择其自身，执行范围另按 JDBC 完整测试模块与根 PARTY/JDBC 集成类覆盖。
- 新增 `QuestTaskSourceProgressMigrationIntegrationTest`：在显式隔离 schema 中模拟旧版 `galaxy_quest_task_progress` 表，实际执行 `20261010_001_add_quest_task_source_progress.sql` 两次；验证老 `progress_values/target_values/complete` 原样保留、新 JSONB 默认为 `{}` 且 NOT NULL、行数不变。生产迁移仍未执行。
- 复验 Core 273、Postgres 模块 56、根 PARTY/JDBC 集成 15，共 344 项实际执行，0 skipped/failure/error；`:assemble :verifyUi2SelfContained` 成功（29 actionable tasks，29 executed），`git diff --check` 通过。第一次完整 Postgres 复跑前测试容器已被清理，产生连接拒绝；重建全新 PostgreSQL 16 隔离容器后完整 56 项全部通过。最终临时库使用 512 MiB tmpfs、无宿主卷、只绑定 `127.0.0.1:55494`，现已移除且端口释放。
- 固定 BQ `QuestInstance.detect` 的源码显示：定义顺序遍历所有未完成 Task，Task 更新自己的进度/完成标记，Quest 汇总状态只在整轮扫描后刷新。结合用户已选择 Quest 级 Detect，兼容目标应是批次末判定 Quest 完成/奖励；现有 Base 每条事实立即完成并冻结周期，因此服务端持久批次协调尚未实现。此轮未改 Quest 完成、奖励或消费策略，也未部署、访问生产玩家进度/数据库、执行生产 DDL、切换 Quest/BQ 开关或触碰 S1/S2。

### 2026-10-10 - 未放置 Quest 导入与任务中心分组回归

- CodeGraph `status/sync` 核对导入测试文件；`explore` 检查 `BqMigrationPlanner.plan`、`BqMigrationDatabaseService.apply`、`JdbcQuestCenterQuery.loadPageForPlayer` 与 `QuestCenterPaginator`。`impact` 确认迁移入口影响 importer PostgreSQL 集成类；`affected` 选中该测试类。
- 扩展合成 BQ→隔离 PostgreSQL→玩家任务中心测试：未放置 Quest 定义必须发布且不出现在源章节 placement；分页器为其返回独立虚拟 `unplaced` / “Other quests” 汇总，默认章节仍只列出原有两个 placement。未推断 S2 的未放置内容都是隐藏辅助节点，也没有自动补坐标或改生产行为。
- 首次回归发现测试原断言把虚拟汇总误计作源章节，故从预期 1 改为预期 2 并直接断言虚拟分组 ID/数量；最终 `BqMigrationDatabaseIntegrationTest` 7 项和 isolation URL guard 1 项共 8 项实际执行，0 skipped/failure/error。使用 PostgreSQL 16、512 MiB tmpfs、无宿主卷，仅绑定 `127.0.0.1:55493`；测试后容器删除且端口释放。
- 既有 root-owned Gradle binary report 曾阻挡普通 UID 测试，原文件保留到独立 `/tmp` 备份目录后，普通 UID 按常规输出目录重跑成功。仅验证 importer/任务中心窄闭环；未跑全量 test、assemble、自包含门禁或实机行为。没有访问生产数据库/玩家进度、执行 DDL、部署、改 Quest/BQ 开关或触碰 S1/S2；最终 `git diff --check` 通过。

### 2026-10-10 - BQ 全量导入结构警告影响复核

- CodeGraph `status` 显示索引 up-to-date；`explore BqMigrationPlanner.validate/detectCycles`、`explore BqMigrationDatabaseService.apply/plan` 与 `node BqMigrationPlannerTest.java` 核对 warning 生成、迁移清单及两个既有回归。`supportedUnplacedQuestIsAWarningOnly` 明确允许单个未放置 Quest 进入 manifest；`plan(manifest)` 对全部 Quest 逐一生成迁移项，章节放置独立，因此全量解析的 64 个未放置 Quest 不会静默丢弃，但章节地图可见性仍未确认，不自动补坐标。
- 固定 BQ 官方提交 [`EnumLogic.java`](https://github.com/GTNewHorizons/BetterQuesting/blob/524b365211b6b3a9672cab8ae45b4e07e726d49e/src/main/java/betterquesting/api/enums/EnumLogic.java) / [`QuestInstance.java`](https://github.com/GTNewHorizons/BetterQuesting/blob/524b365211b6b3a9672cab8ae45b4e07e726d49e/src/main/java/betterquesting/questing/QuestInstance.java) 确认 XOR 要求恰有一个前置完成，`isUnlockable` 对 XOR 接受 `completed <= 1`。现有 4 个 warning 检测到组外前置边，但未证明该入口可达；按警告保留，不把结构可疑误写成行为等价。
- 本轮只读审计与开发导航/能力矩阵记录，没有改 Java、数据库、网络或 UI。一次性 `galaxy-dev:local` Docker 容器、无数据库环境变量运行 `:galaxy-quest-bq-importer:test --tests '*BqMigrationPlannerTest'`：8 项实际执行，0 skipped/failure/error，BUILD SUCCESSFUL（14 秒；2 个任务执行、6 个 up-to-date）。Gradle 输出容器用户与挂载仓库属主不一致的 Git `dubious ownership` 警告，但项目版本插件使用已有 `NO-GIT-TAG-SET` override，构建仍成功；未修改 Git 全局配置。此定向单测不重报此前全量静态解析或扩大为行为验收。未读取玩家进度/生产数据库、执行 DDL、部署、改变 Quest/BQ 开关或触碰 S1/S2。更新后 `git diff --check` 无 whitespace 报告（新建开发导航文件的 `--no-index` 检查因“文件不同”返回 1，输出为空）。

### 2026-10-10 - R.2.6 Crafting 当前工作树定向复验

- CodeGraph `status` 显示索引 up-to-date；`explore` 核对 `ItemVectorTaskEvaluator.evaluate`、`AuthenticatedCraftingStatisticsDetectionService.detectAutomatically`、`QuestGameplayEventHandler.submitCraftingStatistics` 与 Party membership 路由。`impact ItemVectorTaskEvaluator.evaluate` 命中 Crafting 专项 evaluator 回归；`impact QuestGameplayEventHandler.submitCraftingStatistics` 命中事件分发测试；`affected` 返回 75 个宽泛候选，按直接调用链选择 Core 6 类与根 3 类测试。
- 一次性 `galaxy-dev:local` 容器、无数据库环境变量执行 Core 6 类 100 项（Crafting evaluator 41、统计服务 7、Party 成员 3、观察计划 9、Planner 23、Engine 17）及根 3 类 25 项（事件分发 4、事件故障边界 2、Minecraft 消费适配器 19），共 125 项实际执行，0 skipped/failure/error。编译任务均由现有构建缓存命中；两个 Gradle Test 任务均 `BUILD SUCCESSFUL`。
- 这是针对当前工作树的定向复验，不重复 PostgreSQL 集成测试，也不证明 Forge 实际事件、`EntityPlayerMP.objectCraftStats`、Party 在线目录或跨服时序；未运行 `assemble` / 自包含门禁。本轮未改生产 Java、网络、数据库或玩家交互。未访问生产数据库/进度、未执行 DDL、部署、Quest/BQ 开关或触碰 S1/S2；`git diff --check` 通过。

### 2026-10-10 - Crafting 多服统计与事件计数避免重复的源码核验

- CodeGraph `status` 确认索引 up-to-date；`explore QuestGameplayEventHandler.submitCraftingStatistics`、`node OrderedQuestFactRetryQueue.submit/retry/QuestGameplayEventHandler.submitFact` 与 `explore QuestEvaluationPlanner.factMembershipFor/bqPartyActivityMembership` 核对事实创建、FIFO 重试和 Party 重新解析链。
- 对照固定 BQ 官方提交 `524b365211b6b3a9672cab8ae45b4e07e726d49e` 的 [`TaskCrafting.java`](https://github.com/GTNewHorizons/BetterQuesting/blob/524b365211b6b3a9672cab8ae45b4e07e726d49e/src/main/java/bq_standard/tasks/TaskCrafting.java)：匹配 craft/smelt/anvil 事件先按产出数量增加 ALL_UUIDS 每位玩家的任务向量，再调用 `detect`，读取本服每位在线玩家的 `objectCraftStats` 并仅在统计更大时覆盖。故“每来源服务器累计”若被确认，应按每服 `max(本服事件累计, 本服最新统计快照)` 形成来源贡献后再求和；不能把多服绝对快照直接相加，亦不能在共享总向量中同时累加事件增量与绝对值。
- 当前 `GameplayFact` 在首次 sink 尝试前已创建，队列保存原对象/幂等键；重试只重新调用 sink，但 Planner 按当前 `fact.playerId` 再查 Party。事实没有事件时 Party roster/在线资格。恢复事件时队伍语义需要另一项决定；不可用“修队列”或单存 Party ID 掩盖成员资格变化。
- 仅补充开发导航与能力矩阵中的源码结论和待决口径，没有改 Java、数据库/网络合同或玩家交互；没有运行测试。官方固定提交作为可复核来源；未读取服务器玩家进度/数据库、未执行 DDL、部署、Quest/BQ 开关或触碰 S1/S2。`git diff --check` 通过。

### 2026-10-10 - Quest 级消耗检测接入边界审计

- CodeGraph `status` 确认索引 up-to-date；`explore AuthenticatedQuestConsumptionService.submit`、`ConsumptionSubmissionService.submit/process` 检查唯一消费入口、事务与恢复调用链。当前服务输入一个 Task key，生成独立随机提交键；每 Task 单独经过 `PREPARED → APPLIED → CONFIRMED/REJECTED`，真实库存扣除与数据库进度入账通过可恢复 Saga 衔接。
- 因而复刻 BQ Quest 级 Detect 不能仅在客户端顺序调用多个现有提交：这既不是一个 server-authoritative Quest 操作，也没有跨 Task 的稳定批次身份；必须先定任务顺序、部分成功后的回报/重试、每 Task Saga 与批次身份的关系，以及手持单栈路径是否保留为 Task 级动作。真实玩家库存与 PostgreSQL 不具备跨系统原子提交，文档不宣称“批量全有或全无”。
- 仅记录实现边界；没有改变 UI 或消费合同、数据库、网络消息，也没有运行测试。CodeGraph 为只读调用链分析；未访问生产玩家进度/数据库、未执行 DDL、部署或切换 Quest/BQ 开关。

### 2026-10-10 - Crafting 事实重试与 PARTY 归属时点审计

- CodeGraph `status` 确认索引 up-to-date；`explore` 检查 `QuestGameplayEventHandler.submitCraftingStatistics`、`OrderedQuestFactRetryQueue.retry`、`RuntimeQuestFactSink.accept` 与 `QuestEvaluationPlanner.factMembershipFor/bqPartyActivityMembership`，`impact` 确认事件分发影响 `QuestCraftingStatisticsDispatchTest`，Planner 路由影响 Party/Crafting 及消费授权测试。
- 确认 R1-03 队列原样保存 immutable fact 和幂等键；事实首次尝试失败后会于稍后的 tick 再次进入 `RuntimeQuestFactSink`，Planner 此时重新解析 `fact.playerId` 当前所属的 Party。CodeGraph 对 `BqPartyTaskProgressScope.supports` 的源码显示，共享事件路由覆盖 Crafting、Hunt、Location、Meeting；这些普通事件事实均未记录发生时的 Party roster/在线资格。因此玩家若在积压期间换队，这几类旧事件可能按重试时的新成员关系判定。这是未覆盖的跨服时序风险，不等于已证实发生了数据损坏。
- 仅记录审计结论，未改事实字段、Party/Quest 归属、重试合同或数据库。严格复现事件时的 BQ active Party 需要定义捕获时的 Party roster/在线资格如何固定，以及旧 Party 在成员离队后的延迟事件如何处理；不能只在重试时加一个 Party UUID 就宣称等价。待形成有界方案和回归后再实施。未访问生产数据库/玩家进度、未执行 DDL、部署或切换 Quest/BQ 开关。

### 2026-10-10 - BQ 检测入口固定源码基线校正

- 复核 `Reference/BetterQuesting` 的来源边界：该目录被 `.gitignore` 忽略且没有独立 `.git`，因此旧记录中的本地镜像 HEAD 不能在当前工作站从该目录验证，也不能证明文件对应固定提交。能力矩阵现以 GTNewHorizons/BetterQuesting 固定提交 `524b365211b6b3a9672cab8ae45b4e07e726d49e` 的官方永久链接作为可复现基线，本地副本只作为浏览便利。
- 直接核对固定提交 `GuiQuest`、`QuestInstance`、`TaskRetrieval`：GUI 单个 Quest Detect 按钮发送 Quest ID；Quest 服务端按定义顺序对该 Quest 的全部未完成 Task 调用 `detect`；Retrieval 库存变化时仅在 `!consume || autoConsume` 条件成立时自动检测。该源码行为与产品交互合同是两件事：用户确认了非消耗库存持续自动检测、消耗任务显式提交，但 Base 是否要一次聚合提交该 Quest 多个消耗 Task，仍未明确；逐 Task 重叠需求、部分成功与恢复顺序尚不能擅自改变。
- 更新能力矩阵的源代码基线和入口语义摘要，移除无法复核的本地镜像提交断言；未改代码、数据库/网络合同或行为测试。仅文档变更，`git diff --check` 通过；未连接数据库、读取玩家进度、部署或操作 Quest/BQ 开关。

### 2026-10-10 - M.2 interact_entity 未注册实体过滤兼容

- 对照固定 BQ `TaskInteractEntity.onInteract` 与 `EventHandler.onEntityInteract/onEntityAttack`：目标 ID 必须能由 `EntityList` 解析；交互实体没有注册资源 ID 时跳过；开启子类型匹配时才允许注册父类 ID 命中。Base 事实工厂原先在实体未注册时写入 Java 类名，并仍附加已注册父类别名，导致 Core evaluator 可能把 BQ 会忽略的实体计入。现将无注册名主体/别名编码为空，并在 evaluator 中拒绝空目标与空实际主体，再进行精确/子类型匹配；已注册实体与当前子类型、物品、NBT 规则不变。
- CodeGraph 起始 `status` up-to-date；`explore` 检查 BQ 两个 Task/事件入口、Base `entityInteracted` 和 `InteractionTaskEvaluator`；`impact InteractionTaskEvaluator.evaluate` 检查 evaluator 及复合调用链，`affected` 给出 74 个宽泛候选后以直接测试链确定范围。修改后 `sync` 更新 3 文件、147 nodes；后验 `impact` 确认 5 个直接影响符号。Core 全套 270 项（其中 `BqCompatibleTaskEvaluatorsTest` 41 项）、根 `QuestGameplayEventHandlerFailureTest` 2 项均执行通过，0 skipped/failure/error；根 `compileJava`、`:assemble`、`verifyUi2SelfContained` 均通过（assemble 29 actionable tasks，5 executed、24 up-to-date）；最终 `git diff --check` 通过。
- 这是离线主体匹配边界修复，不代表 S2 DefaultQuests 存在此类任务，也不证明 Forge 实体事件、EntityList 注册时序、真实实体 NBT、PARTY/JDBC 多服时序。未访问数据库/玩家进度、未执行 DDL、部署或改变 Quest/BQ 开关；interact 类与 M.2 继续 BLOCKED。

### 2026-10-10 - M.2 interact_item 右键空气事件兼容修复

- 对照固定 `Reference/BetterQuesting` 的 `EventHandler.onPlayerInteract`：BQ 不过滤 `PlayerInteractEvent.Action`，`isHit` 仅在 `LEFT_CLICK_BLOCK` 时为 true，所以右键空气会按普通交互提交给 `TaskInteractItem`。Base 原入口只接受左/右键方块，遗漏 `RIGHT_CLICK_AIR`。现补齐 1.7.10 Forge 的三种实际 Action（`LEFT_CLICK_BLOCK`、`RIGHT_CLICK_BLOCK`、`RIGHT_CLICK_AIR`）；取消事件、服务端玩家与 FakePlayer 检查不变，只有左键方块视为 hit。源码 API 没有 `LEFT_CLICK_AIR`，初次编译捕获后已按实际枚举修正。
- CodeGraph 起始 `status` up-to-date；`explore` 检查 `QuestGameplayEventHandler.onPlayerInteract`、`MinecraftQuestFactFactory.itemInteracted`、`InteractionTaskEvaluator` 与 BQ 对应 Task/事件处理器；`impact` 覆盖事件入口和事实工厂，`sync` 更新 handler/test 两文件 115 nodes。`affected` 给出 45 个宽泛候选；依据直接调用链定向运行根 `QuestGameplayEventHandlerFailureTest` 2 项与 Core `BqCompatibleTaskEvaluatorsTest` 41 项，共 43 项，0 skipped/failure/error。`:assemble :verifyUi2SelfContained` 通过，29 actionable tasks（6 executed、23 up-to-date）；最终 `git diff --check` 通过。
- 首次 Docker Gradle 网络隔离导致插件配置超时；一次性 host-network 构建容器补齐 `JAVA_TOOL_OPTIONS` 的 7892 HTTP/HTTPS proxy 后运行成功。未改系统代理、Compose、服务/防火墙；未连接数据库、访问玩家进度、部署或改 Quest/BQ 开关。此差异回归不证明 DefaultQuests 实际用量、完整交互任务等价或 Forge/GTNH 实机行为，`interact_item / interact_entity` 和 M.2 继续 BLOCKED。

### 2026-10-10 - R.2.6 同一玩家多服 Crafting 绝对统计归并现状回归

- 扩展既有 PostgreSQL 集成场景并重命名为 `automaticBqCraftingStatisticsUsePersonalMaximumAcrossServersWithoutSumming`。同一合成玩家从 `s2` 发布 5、从 `lobby` 发布 2，现有进度保持 5；`lobby` 后续发布 7，进度更新为 7；同一来源/计数重放返回 `DUPLICATE`。这只刻画当前 Runtime 的最大值归并，不是用户已确认的跨服规则。已确认的“不相加”仅针对不同 Party 成员；同一玩家的多服本地统计归并策略待用户确认。
- CodeGraph 起始 `status` up-to-date；`explore` 检查 `QuestRuntimeService.applyIf`、`TaskProgress.merge`、`AuthenticatedCraftingStatisticsDetectionService.detectAutomatically` 和 `BqPartyActiveQuestQuery.isActiveFor/hasActiveMember`；`impact` 分别覆盖 Runtime/检测调用链与 TaskProgress 的 42 个下游相关符号。修改后 `sync` 更新集成测试文件 217 nodes；`affected` 收敛至 `BqPartyInventorySnapshotFactSinkPostgresTest`。
- Docker Gradle 自动回归：Core 全套 270 项、根 PostgreSQL 集成类 15 项、Crafting 分发 4 项与事件失败 1 项，共 290 项实际执行，0 skipped/failure/error。`:assemble :verifyUi2SelfContained` 成功；最终 `git diff --check` 通过。临时 PostgreSQL 16 使用 tmpfs、无宿主数据卷，仅映射 `127.0.0.1:55493`；测试后容器已删除并确认端口释放。前两次测试容器网络配置失败发生在 Gradle 插件初始化、未进入测试；最终沿用本机 7892 代理的一次性 Gradle host-network，并未修改系统代理/网络。
- 测试全部使用合成玩家、Quest、Party 与统计读取器；不覆盖真实 `EntityPlayerMP.objectCraftStats` 数据、跨服在线资格/时序或 Forge tick。未访问生产数据库/玩家进度，未执行生产 DDL/迁移、部署、Quest/BQ 开关或 S1/S2 操作；R.2.6 自动门槛完成，Crafting/M.2 真实兼容仍 BLOCKED。

### 2026-10-10 - R.2.6 Crafting 自动检测链定向复验

- 继续核对 Crafting 自动统计路径：`QuestObservationPlan` 只对已发布且启用 BQ statistics 的任务产生轮询目标；tick 每 60 玩家 tick 调用检测服务；统计求值按 Item ID/注册名推进，不重新匹配 metadata/NBT；Party 在线玩家的个人统计分别生成 PLAYER 事实，不合并计数。CodeGraph `explore` 检查 `detectAutomatically`、`QuestEvaluationPlanner.requestsFor`、`BqPartyCraftingStatisticsMembers.localOnlineMembers`、`BqPartyActiveQuestQuery.hasActiveMember` 与 `ItemVectorTaskEvaluator.evaluate/matches`；`impact detectAutomatically` 为 5 个直接关联符号，`impact fromPublished` 为 18 个关联符号。
- Docker Gradle 离线重跑核心 `AuthenticatedCraftingStatisticsDetectionServiceTest` 7、`BqCompatibleTaskEvaluatorsTest` 41、`BqPartyCraftingStatisticsMembersTest` 3、`QuestObservationPlanTest` 9，共 60 项；根 `QuestCraftingStatisticsDispatchTest` 4 与 `QuestGameplayEventHandlerFailureTest` 1，共计 65 项，均 0 skipped/failure/error。两个 Gradle 命令均 `BUILD SUCCESSFUL`。
- 本轮未改生产代码或改变 UI/服务端合同；该复验不替代既有隔离 PostgreSQL 15 项回归，也不证明真实 Forge tick、玩家统计文件或 GTNH 多服在线时序。BQ Quest 级 Detect 与 Base 当前逐 Task 消耗入口的粒度选择仍待用户答复；Crafting 和 M.2 继续 `BLOCKED`。未访问生产数据/数据库、未执行 DDL/部署/Quest 开关或服务操作。

### 2026-10-10 - M.2 检测入口粒度源码复核（等待产品选择）

- 固定 BQ 源码显示非消耗 Retrieval/Fluid 在 `EventHandler` 处理库存变化队列时调用 `onInventoryChange` 自动检测；`consume=false` 时会检测，不会扣库存。Quest 级 `QuestInstance.detect` 则按定义顺序遍历全部未完成 Task 并调用各自 `detect`，因此 consuming Retrieval/Fluid 可能在一次 Quest 动作中连续消费多个任务所需库存。Base 当前用 20 玩家 tick 周期快照自动观察非消耗库存，消耗型任务由玩家逐 Task 提交。
- 补查固定 GUI 入口：`GuiQuest` 只提供单个 Quest 的 Detect/Submit 按钮，点击后 `NetQuestAction.requestDetect(Collections.singletonList(questID))` 发送 Quest ID；服务器再由 `QuestInstance.detect` 顺序遍历此 Quest 全部未完成 Task。按钮状态由 `quest.canSubmit(player)` 决定。一次 Quest 操作可触发多个 Task 的检测/消耗。
- 用户已确认非消耗任务持续自动检测、消耗物品任务显式提交/检测；仍未明确 Base 的交互粒度是逐 Task、Quest 级聚合还是两者并存。聚合入口会改变消费范围，并涉及多 Task 顺序、重叠需求与跨任务消费恢复；在用户选择前不新增该动作或改现有逐 Task 交互。
- CodeGraph 起始 `status` up-to-date；`explore` 检查 `QuestObservationPlan.fromPublished`、`AuthenticatedCraftingStatisticsDetectionService.detectAutomatically`、`QuestGameplayEventHandler.onPlayerTick`、玩家 Quest 文档与服务端路由；`impact QuestObservationPlan.fromPublished` 显示 18 个关联符号，`affected` 对候选文件列出 127 个宽泛测试候选，需按具体改动再收敛。本轮无生产代码变更，因此未执行构建或测试。
- 本轮只读 `Reference/BetterQuesting` 固定源码，不访问服务器定义或玩家进度，不连接数据库、执行 DDL/部署、修改 Quest/BQ 开关或触碰 S1/S2；将行为粒度未决项写回开发导航与能力矩阵，M.2 仍 `BLOCKED`。

### 2026-10-10 - R.2.16 完整导入 QuestCompletion 来源/目标定义

- 扩展既有 QuestCompletion 隔离 PostgreSQL worker 回归：来源与目标 Quest 现在都由完整合成 BQ typed JSON 经 `BqQuestDefinitionImporter` 导入；来源含非消耗 Retrieval 与 QuestCompletion 奖励，目标含 Checkbox Task 与 Item 奖励。来源 Retrieval 经生产 `BqPartyInventorySnapshotFactSink`、JDBC Party membership 与 Planner/Runtime 完成，再显式 claim，由 JDBC `RewardDeliveryWorker` 投递 QuestCompletion 到已发布的导入目标；断言目标 PLAYER 进度完成、目标 entitlement 仅一条、后续 poll 不再租赁来源 entitlement。仅测试变化，无生产行为/数据库合同修改。
- CodeGraph 起始 `status` up-to-date，`explore` 检查 importer、QuestCompletion handler/JDBC port 与 worker 测试；`impact BqQuestDefinitionImporter.read` 和目标测试方法、`affected` 都将回归收敛到 `BqPartyInventorySnapshotFactSinkPostgresTest`。修改后 `sync` 更新该测试文件 217 nodes，索引 up-to-date。
- Docker Gradle 根 `:test --tests ...BqPartyInventorySnapshotFactSinkPostgresTest` 在隔离 PostgreSQL 16 Alpine 上执行 15 项，0 skipped/failure/error。测试库 `quest_r216_questcompletion_it`、512 MiB tmpfs、无宿主卷，只绑定 `127.0.0.1:55491`；测试后容器已停止自动删除，确认无容器及监听端口。测试 schema 由用例在隔离库内创建/删除；未执行生产 DDL或连接业务数据库。
- R.2.16 消除了 R.2.14 中“只有奖励导入、来源/目标 Quest 手工构造”的自动覆盖缺口，但仍使用合成 Quest JSON、玩家与 Party，不是完整 DefaultQuests 批次或 Forge/GTNH 实机证据。`:assemble :verifyUi2SelfContained` 成功（29 actionable tasks，1 executed、28 up-to-date），最终 `git diff --check` 通过；未访问生产进度、部署、改 Quest/BQ 开关或触碰 S1/S2。

### 2026-10-10 - R.2.15 导入 Item 最大数量到背包溢出交付回归

- 只读复核授权的 S2 `DefaultQuests` 定义目录；匿名聚合来源哈希仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，3,744 个 Quest、47 章、3,847 个章节放置及任务/奖励类型计数与既有基线一致。只读输入限定为 DefaultQuests，没有访问玩家进度、其他配置、数据库或服务器运行状态。
- 对照固定 BQ `RewardItem.claimReward0` / `BigItemStack.getCombinedStacks`：数量按物品最大堆叠拆分，背包无法容纳的栈会掉落到世界。新增合成 BQ typed JSON 数量 8,192 → importer → Minecraft item delivery adapter 的回归：36 格背包收到 36×64，剩余 92×64 作为 overflow drops；总量仍为 8,192，marker 持久化，重放不再重复掉落或 flush。只增加测试，不改生产奖励策略。
- CodeGraph 起始 `status` 为 up-to-date；`explore`/`node` 检查 importer、`rewardStacksForPlayerName`、Minecraft delivery handler、overflow 与 marker 回归；`impact` 覆盖 item reward 拆栈/投递链，`affected` 对新增测试选择 `BqItemRewardImportDeliveryTest`。同步后 1 个测试文件、63 个节点。
- Docker Gradle 使用已缓存的隔离开发 Gradle Home 离线运行根限定测试：`BqItemRewardImportDeliveryTest` 4 项 + `MinecraftRewardDeliveryTest` 28 项，共 32 项实际执行，0 skipped/failure/error。初次误用未限定 `test` 导致子模块过滤器提示 No tests found（非测试失败），随后改用 `:test --tests ...` 成功。首次 Wrapper 尝试因默认 Gradle Home 无发行版而尝试网络下载，已中断；没有改动系统代理/网络。
- `:assemble :verifyUi2SelfContained` 成功（29 actionable tasks，7 executed、22 up-to-date）；最终 `git diff --check` 通过。M.2 仍 `BLOCKED`：该替身测试没有创建真实 EntityPlayer/World，不覆盖世界掉落与玩家存档/PostgreSQL entitlement 之间的崩溃窗口或 GTNH 实机行为。未连接数据库、未执行生产 DDL/部署/开关，也未触碰 S1/S2 服务器。

### 2026-10-10 - M.3 进度冲突诊断的 repeat 时间戳边界

- 从只读 BQ 源码复核 `QuestInstance` 的 `completeUsers` / `userProgress` 与 `resetUser(uuid, false)`：重复重置会复位 claimed、把 timestamp 清零并重置 Task 向量，没有可单调比较的 cycle ordinal。当前 `BqPlayerProgressImporter` 和 `BqProgressMapper` 只用于离线解析/向量映射，未发现 PostgreSQL 进度写入、批次或回滚调用链；本轮没有读取任何生产玩家进度。
- 修复 `BqProgressConflictAnalyzer` 的假等价：完成/领取标记和 Task 向量相同但 timestamp 不同时，改判 `DIVERGENT`；新增 `differentBqCompletionTimestampsAreNotClassifiedAsIdentical`。CodeGraph `sync` 更新分析器及回归测试（2 文件/35 节点）；`impact BqProgressConflictAnalyzer.dominates` 与 `affected` 指向冲突/影子分析测试。
- Docker Gradle `:galaxy-quest-bq-importer:test` 总计 49 项：42 执行、7 条件跳过、0 失败/错误。7 条均为 `BqMigrationDatabaseIntegrationTest`，因未设置隔离 `QUEST_TEST_POSTGRES_URL` 而按设计跳过；本轮没有启动数据库或连接任何数据库。冲突分析及影子分析定向回归通过。`:assemble :verifyUi2SelfContained` 成功（29 actionable，1 executed/28 up-to-date）；最终 `git diff --check` 通过。
- M.3 仍只有只读映射和离线差异基础。由于重复周期没有可靠身份，跨服冲突/来源选择规则尚未确定；这里记录的是保守审计与诊断修正，不是进度合并实现。建议无法证明同周期时隔离冲突，不自动按最大向量/最新时间戳/固定服优先级合并；等合同确认后再设计写入器。M.2 整体仍 BLOCKED；不执行生产 DDL、部署、Quest 开关或 S1/S2 操作。

### 2026-10-10 - R.2.6 Crafting 统计自动复评与解锁收口

- 固定 BQ 的被动 60 tick `QuestInstance.update()` 只检查 Quest 完成/同时任务状态，`TaskCrafting.detect()` 实际由 Quest 级手动 Detect 或同 Task 匹配产物事件触发。按用户明确选择，Base 保留匹配产物即时统计，并额外每 60 tick 自动复评已发布、启用 `allowCraftedFromStatistics` 且带 `bq.raw` 的 Task；这是有意的体验差异，不宣称是 BQ 原生 60 tick 检测。每份快照保持对应在线玩家个人绝对统计，不把 PARTY/跨服计数求和。
- 额外修复锁定后解锁边界：Party 任务完成进度仍为 LOCKED 时不提前发奖；当玩家自身前置满足，使用独立 `craft-stat-auto-unlock` 稳定 fact key 重评已完成 Task，避免旧事实幂等键阻止 Quest 完成。每个目标异常捕获记录，单目标数据库失败不逃出 tick，下个既有周期重试。
- CodeGraph `status/sync` 首次更新 12 个文件/749 nodes，最终更新 3 个文件/322 nodes；`impact` 覆盖 `QuestObservationPlan.fromPublished`、`AuthenticatedCraftingStatisticsDetectionService.detectAutomatically`、`QuestEvaluationPlanner.requestsFor`、`QuestRuntimeService.applyIf`、`BqPartyActiveQuestQuery.hasActiveMember`、`QuestGameplayEventHandler.onPlayerTick`；`affected` 收敛至 Core 自动统计/Party/Runtime/观察计划测试、根事件分发/故障测试与 PostgreSQL 集成类。
- Docker `:galaxy-quest-core:test` 全套 270 项、根 `QuestCraftingStatisticsDispatchTest` 4 项与 `QuestGameplayEventHandlerFailureTest` 1 项、隔离 PostgreSQL `BqPartyInventorySnapshotFactSinkPostgresTest` 14 项，共 289 项实际执行，0 skipped/failure/error。PostgreSQL 16 临时 DB `quest_r26_auto_crafting_it` 容器使用 512 MiB tmpfs、无宿主卷，仅绑定 `127.0.0.1:55490`；测试后移除并确认监听释放。首次回归发现 LOCKED 解锁复评被普通锁定拒绝条件挡住，修复后通过；`--network none` 因 GTNH 插件解析需联网而失败，随后只对一次性 Gradle 容器使用本机 7892 代理，无系统网络改动。
- 所有数据均为合成定义/玩家/统计读取器；没有访问生产玩家进度/数据库、执行生产 DDL、部署、启用 Quest、停用 BQ 或触碰 S1/S2。Forge tick、真实 `EntityPlayerMP.objectCraftStats`、在线目录与多服时序仍未验证，R.2.6 自动门槛完成但 Crafting/M.2 仍 `BLOCKED`。`:assemble :verifyUi2SelfContained` 成功（29 actionable tasks，6 executed、23 up-to-date）；`git diff --check` 通过。未部署。

### 2026-10-10 - R.2.6 LOCKED Crafting 解锁后的 JDBC 结算回归

- 固定 BQ 来源语义后，补齐此前只有 Core/runtime 单测覆盖的组合边界：合成 Party 中 owner 已满足前置、peer 的个人统计达到 Crafting 目标但 peer 仍 LOCKED；peer 自己完成前置后，必须用新的稳定事实键重新结算，不能由旧统计 fact key 去重挡住完成与奖励。新增 `lockedPartyCraftingStatisticsUnlockThroughPersonalStableFactInJdbc`，直接使用 `AuthenticatedCraftingStatisticsDetectionService`、数据库 Party roster、在线目录、`BqPartyActiveQuestQuery`、Planner/Runtime、PostgreSQL 进度与奖励权益仓储。验证锁定时不发奖、解锁后个人 Quest 完成且仅一条奖励权益、后续轮询为空。
- CodeGraph `status` 显示索引 up-to-date；`impact detectAutomatically` 命中 QuestModule、玩家 tick handler 和两条 JDBC 检测用例，`impact QuestRuntimeService.applyIf` 命中统计服务；`affected` 对新增/变更集成文件选中 `BqPartyInventorySnapshotFactSinkPostgresTest`。
- 隔离实跑：Core 全套 270 项；根 `QuestCraftingStatisticsDispatchTest` 4 项、`QuestGameplayEventHandlerFailureTest` 1 项及 PostgreSQL 集成类 15 项，共 290 项实际执行，0 skipped/failure/error。PostgreSQL 16 临时库 `quest_r261_locked_test` 使用 512 MiB tmpfs、无宿主卷，仅映射 `127.0.0.1:55490`；验收后容器已移除并确认监听释放。`:assemble :verifyUi2SelfContained` 成功（29 actionable tasks，1 executed、28 up-to-date）；`git diff --check` 通过。初次 PG 定向运行 15/15 成功；为最终总数复核，另在新建的同配置隔离库重跑根三个类。
- 只有合成定义/玩家和统计读取器参与测试；不代表 Forge tick、真实 `EntityPlayerMP.objectCraftStats`、生产 Party 在线目录或跨服时序。未访问生产进度/数据库、执行生产 DDL、部署、启用 Quest/停用 BQ 或触碰 S1/S2；R.2.6 自动门槛完成，但 Crafting/M.2 等价仍 `BLOCKED`。

### 2026-10-10 - R.2.14 QuestCompletion 导入奖励贯通来源领取与 JDBC Worker

- 在已有“导入奖励→JDBC 目标完成”测试旁新增隔离 PostgreSQL 闭环：来源 Quest 由玩家库存事实完成，生成可领取的个人 QuestCompletion 奖励权益；显式 JDBC claim 后，`RewardDeliveryWorker`/`RewardDeliveryRouter` 处理经 BQ importer 导入的奖励，目标 PLAYER 任务完成并只创建一条目标奖励权益，Worker 后续轮询不再重复租赁。
- 来源 Quest 本身是合成构造定义，只有其 QuestCompletion reward 来自 BQ typed JSON importer；目标 Quest 也是合成发布定义。该测试不代表 DefaultQuests 全量导入、玩家真实库存/存档或实际跨服 Worker 竞争行为，也不改变 claim/奖励合同。
- CodeGraph `sync` 更新根 PostgreSQL 测试 1 文件/212 nodes；`impact importedBqQuestCompletionRewardFlowsFromSourceClaimThroughJdbcWorker` 与 `affected BqPartyInventorySnapshotFactSinkPostgresTest` 将影响范围锁在该集成类。
- Docker Gradle `:test --tests ...BqPartyInventorySnapshotFactSinkPostgresTest` 在专用 PostgreSQL 16 Alpine 临时库执行，XML 为 13 项、0 skipped/failure/error；26 actionable tasks，2 executed、24 up-to-date。DB `quest_r214_questcompletion_it` 仅 loopback `127.0.0.1:55489`、512 MiB tmpfs、无宿主卷；完成后容器已停止自动删除，确认无同名容器和监听端口。一次性 Gradle 容器仅使用进程级本地 7892 代理。
- 这补齐 imported QuestCompletion 从可领取来源权益到目标完成的 JDBC Worker 自动链；QuestCompletion 仍因实际 BQ 导入批次、真实 Forge/GTNH 玩家/跨服时序和目标重置等未验收而 BLOCKED。未访问生产进度/数据库、未执行 DDL/部署/Quest 开关，未触碰 S1/S2。`git diff --check` 待文档收尾后复核。

### 2026-10-10 - R.2.13 BQ XP 导入定义贯通经验投递

- 对 S2 默认定义实际存在的 2 个 `bq_standard:xp` 奖励补齐一段窄回归：合成 typed BQ JSON 经 `BqQuestDefinitionImporter` 得到 `RewardDefinition`，再交给生产 `MinecraftXpRewardDeliveryHandler` 的经验端口；分别验证 `isLevels=true` 映射为等级、false 映射为经验点、经验状态更新、持久标记、flush/sync 顺序和重复投递幂等。
- 未改 XP 曲线或奖励策略。输入/玩家均为合成内容和 fake port；没有验证真实 `EntityPlayerMP`、GTNH 玩家存档、重启、多服租约或 S2 两条实际配置的具体值。
- CodeGraph：起始 `status` up-to-date；`explore/node` 检查 importer 的 XP 字段映射、XP handler 和既有交付测试；改后 `sync` 更新 1 个测试文件/61 nodes。`impact MinecraftXpRewardDeliveryHandler.deliverToPlayer` 命中新导入回归和既有 XP 交付测试；`affected` 选择 `BqItemRewardImportDeliveryTest`。
- Docker Gradle `:test --tests ...BqItemRewardImportDeliveryTest --tests ...MinecraftRewardDeliveryTest` 实际 31 项（3 + 28），0 skipped/failure/error；26 actionable tasks，8 executed、18 up-to-date。一次性容器只给构建进程设置本机 7892 代理，没有改系统网络/代理配置；本轮没有数据库容器或数据库连接。`git diff --check` 待文档收尾后复核。
- XP 的 importer→Minecraft adapter 自动证据已补；奖励整体、M.2 与 BQ 替代仍 `BLOCKED`。未访问生产进度/数据库、未执行 DDL/部署/Quest 开关，未触碰 S1/S2。

### 2026-10-10 - R.2.12 Choice 与 QuestCompletion 导入/JDBC 回归

- Choice 的 PARTY 集成用例改为从合成 BQ JSON 经 importer 构建 Item/Choice 奖励定义，再走隔离 PostgreSQL 发布、认证的个人选择、领取和 JDBC worker/router；三位成员不同选择仍分别收到对应的 item/meta/NBT，重复领取/投递不重复发放。
- 新增 QuestCompletion 导入链：BQ typed JSON 的紧凑 UUID 由 importer 解码，目标 Quest 发布到隔离 PostgreSQL 后，导入的奖励交给基于 `JdbcQuestCompletionRewardPort` 的 handler 两次；目标 PLAYER 进度只完成一次、完成时间稳定且只产生一条目标奖励权益。该用例没有从来源 Quest entitlement 经 claim/JDBC worker 发起，因此不宣称完整来源领取链。
- CodeGraph 起始 `status` up-to-date；`explore/node` 检查 Choice importer、领取/worker、QuestCompletion handler/JDBC port；两轮 `sync` 更新 PARTY/JDBC 测试 1 文件/202 nodes 和 Choice/QuestCompletion 直连测试 1 文件/209 nodes；`impact`/`affected` 选择两个直接测试类。
- Docker Gradle 两个选定测试类共 14 项（导入 adapter 2、隔离 PostgreSQL PARTY/JDBC 类 12），0 skipped/failure/error。临时 PostgreSQL 16 Alpine 使用 512 MiB tmpfs、无宿主数据卷，测试 URL 为 `quest_r212_questcompletion_it`，仅绑定 `127.0.0.1:55488`；测试后已停止移除并确认端口释放。缺失 import 的首次编译未进入测试，修复后上述结果为最终有效结果。
- Choice 与 QuestCompletion 的导入/部分数据库适配证据得到补充；真实 Forge/GTNH、来源奖励完整 worker 链、物品/世界/存档崩溃边界及跨服运行仍未验证，M.2 与 BQ 替代继续 `BLOCKED`。没有访问生产数据、执行生产 DDL/部署/开关或触碰 S1/S2。

### 2026-10-10 - R.2.11 Choice 导入定义贯通 JDBC 选择与奖励投递

- 先新增轻量合成 BQ `bq_standard:choice` typed JSON→Importer→Minecraft adapter 回归，验证选项参数、被选项 materialization、metadata/NBT、玩家 UUID 替换和 marker 幂等。再把现有 PARTY/JDBC 集成用例的 Item/Choice 奖励定义改为从合成 BQ JSON 经 `BqQuestDefinitionImporter` 产生，而不再直接构造 `RewardDefinition`。
- PARTY 集成仍通过既有隔离 PostgreSQL：发布导入的奖励定义，三名玩家分别认证选择不同 Choice，个人领取后由 JDBC worker/router 交给真实 Minecraft ItemStack adapter；验证只有各自选项被发放、导入的 metadata/NBT 经过 JDBC 保存后仍正确、marker 和重复领取/投递幂等。没有更改奖励行为、数据库 schema 或服务端合同。
- CodeGraph `explore/node` 检查 importer→reward codec、`rewardStacks`、现有 PARTY/JDBC 选择/领取/交付调用链；改前 `impact` 确认 importer 与 handler 受影响测试；改后两次 `sync` 分别更新 Choice 直连测试 1 文件/41 nodes、PARTY 集成测试 1 文件/202 nodes。最终 `impact`/`affected` 将回归范围收敛到 `BqItemRewardImportDeliveryTest` 与 `BqPartyInventorySnapshotFactSinkPostgresTest`。
- Docker Gradle 实际 13 项通过（直连 adapter 2 项、导入 Choice PARTY/JDBC 集成类 11 项），0 skipped/failure/error；`26 actionable tasks`，2 executed、24 up-to-date。临时 PostgreSQL 16 Alpine 使用 512 MiB tmpfs、无宿主数据卷，测试 URL 明确指向 `quest_r211_choice_it`，只绑定 `127.0.0.1:55487`；测试结束后已停止并自动删除，端口确认释放。首次容器运行因 Compose `run` 不支持 `--network` 参数退出，未启动 Gradle；首次 Gradle 尝试缺少代理而在插件配置阶段超时，随后仅给一次性构建进程传入已验证的本地 7892 代理后通过，没有修改全局网络或代理配置。`git diff --check` 待文档收尾后复核。
- Choice 的 imported definition→隔离 PostgreSQL publish→认证个人选择→claim→JDBC worker→Minecraft adapter 现有自动闭环证据；仍是测试玩家替身，不证明真实 Forge/GTNH 背包存档、WorldSave/掉落崩溃窗口或在线跨服租约。奖励系统、M.2 与 BQ 替代继续 `BLOCKED`；未访问生产进度/数据库、未执行 DDL/部署/Quest 开关，也未触碰 S1/S2。

### 2026-10-10 - R.2.6 UI 检测语义收敛与 S2 定义基线纠正

- 用户已确认：非消耗 Retrieval/Fluid 持续观察玩家库存快照；消耗型任务才要求逐 Task 提交；匹配产物事件会自动复评 Crafting 统计，不应让玩家手动点“检测合成”。据此从共享 `QuestCenterVisualDocument` 移除 Crafting 检测按钮及其布局占位；Checkbox 手动确认、消耗任务提交均保留。客户端仍不可信，旧显式 Crafting 检测服务/网络动作留作兼容，不作为正常完成路径。
- 重新只读统计授权的 S2 `DefaultQuests/Quests` 定义目录，不打印名称/UUID或任何进度数据：3,744 个 Quest JSON 定义文件包含 5,620 个 Task 实例。类型为 `block_break` 1、`checkbox` 203、`crafting` 133、`fluid` 15、`hunt` 104、`location` 33、`meeting` 1、`optional_retrieval` 1,267、`retrieval` 3,863。此前文档/工作记录中的“3,744 个任务”口径不准确，应理解为 Quest 定义文件数；唯一当前基线现明确同时给出 5,620 个 Task 实例数。此前基线摘要哈希和 47 章/3,847 放置数保留为既有记录，本次没有重算哈希。
- CodeGraph：`status` 检出 Java 文件待同步；`explore`/`callers` 复核 `QuestCenterVisualDocument.detail`、检测 Action Bridge 与 `AuthenticatedCraftingStatisticsDetectionService`，源码侧 Crafting 自动复评与 Base 数据链另按 R.2.6 既有回归；改前 `impact` 确认 UI 详情文档为变更入口。同步后 index up-to-date，`impact`/`affected` 复核并将视觉回归收敛至 `QuestParticipantManagementVisualTest`。
- 验收：Docker `:ui2-terminal:test` 共 27 项实际执行、0 skipped/failure/error；`:assemble` 成功（38 actionable tasks，13 executed、25 up-to-date）；`verifyUi2SelfContained` 成功（24 actionable tasks，1 executed、23 up-to-date）；`git diff --check` 在文档最终更新后复核。构建产物哈希 `3e975692dd37c7609a7ff6f844a38f52ced9433a3e32acf9ab2386e197888f30`，仅构建、未部署。
- 本次不改服务端任务、网络/数据库合同或 M.2 Party 主体；未访问玩家进度/生产数据库、未执行生产 DDL/部署/Quest 开关，未触碰 S1/S2 服务器。

### 2026-10-10 - R.2.11.1 Choice 非正数量语义与 S2 奖励画像

- 按固定 BQ 源码复核 `RewardItem/BigItemStack` 与 `RewardChoice.claimReward0`：普通 Item 对非正 `Count` 会经 `getCombinedStacks()` 最少发放 1 个；Choice 对被选中的非正数量在调用拆栈前直接返回。Base 原处理器共用 Item 规则，存在 Choice 多发 1 个物品的窄兼容差异。
- 对获授权的本地 S2 `DefaultQuests` 定义副本做匿名只读聚合：sourceHash `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`；Item 4,181、Choice 957、QuestCompletion 308、XP 2；无 Command、Scoreboard、未知奖励类型。Choice 共 3,062 个选项，数量全部为正，最小 1、最大 4,096。没有输出任务名/ID，也没有读取进度数据。
- 修复 `MinecraftItemRewardDeliveryHandler`：非正 Choice 选项生成空奖励列表，Item 行为保持不变；现有交付流程将空结果作为无物品完成并持久化 entitlement marker。新增回归验证不发物品、marker 持久化与重复 delivery 不重复 flush。该异常值不在当前 S2 任务库中，修复针对 BQ 导入兼容边界，不改变现有任务内容。
- CodeGraph：起始 `status` up-to-date；`explore` 覆盖 handler/claim/worker、固定 BQ RewardChoice/BigItemStack 和 importer 参数约束；`impact rewardStacksForPlayerName` 命中 handler、delivery API 与 `MinecraftRewardDeliveryTest`，最终 14 个相关符号；`affected` 给出 48 个测试候选，按直接调用收敛至该根测试类。第一次 `sync` 更新生产类+测试类 2 文件/184 nodes；补充负数量断言后第二次 `sync` 更新测试 1 文件/120 nodes，索引最后 up-to-date。
- 最终命令：`GRADLE_USER_HOME=... ./gradlew :test --tests com.jsirgalaxybase.modules.quest.infrastructure.minecraft.MinecraftRewardDeliveryTest --offline --no-daemon --no-configuration-cache -PforceToolchainVersion=17` 成功；XML 实际 28 项，0 skipped/failure/error；Gradle 26 actionable tasks，2 executed、24 up-to-date，23 秒。首次完整 Forge/Minecraft 与根测试源集编译耗时 4m28，最终同一回归增量复跑通过。没有运行全量 test、`assemble` 或 `verifyUi2SelfContained`；没有访问数据库。
- `git diff --check` 在文档更新后通过。未访问生产玩家进度/数据库，未执行生产 DDL、部署或 Quest/BQ 开关，未触碰 S1/S2 服务器。

### 2026-10-10 - R.2.11 标准奖励源码与投递差异只读审计

- 按 R.2.10 后续边界，继续核查固定 BQ 源码的 Item/Choice/XP/Scoreboard/Command/QuestCompletion/`noRewards` 行为及 Base handler。CodeGraph 起始 `status` 为 up-to-date；`explore` 检查各奖励 handler、Claim 与 Delivery Worker 调用链；`impact MinecraftItemRewardDeliveryHandler.deliver` 复核 overflow 相关测试影响范围。没有代码变化，因此未运行 Gradle、未报新增测试通过。
- 对照发现 M.2 文档的 Item 奖励说明陈旧：当前 Base 并非整批拒绝，而是先保存成功入包部分和分项进度，再逐栈向世界掉落并最终写完成 marker。世界实体、玩家 NBT 与 PostgreSQL 租约无法原子提交，崩溃/异常边界仍可能重复掉落或无法证明已落盘。已修正该冲突描述，不改变奖励策略。
- 补记 Scoreboard：BQ 按 `relative` 累加/覆盖并创建 objective；Base 先持久化 absolute target marker 后写分数，纯计算已有覆盖但未证明真实 Minecraft Scoreboard/WorldSave。补记 Command 的明确兼容差异：BQ 默认走 `CommandBlockLogic`，`viaPlayer` 走权限恒真的 `AdminExecute`，并调度到后续 START tick；Base 使用显式 root allowlist、默认拒绝，sender 分别为 Server/真实玩家且立即执行。现有 DefaultQuests 只读画像未发现 command reward，默认安全限制不影响当前内容；将来迁入含此类型的定义需逐命令审查。
- 更新开发导航、能力矩阵、M.2 行为矩阵，状态仅记“源码审计完成”，不升级 Reward、M.2 或 BQ 替代状态。下步先从获授权定义副本统计实际 reward 类型/关键参数，再挑无待决策差异项做 Importer→隔离 JDBC→handler 闭环；不得擅改命令权限或满背包崩溃合同。未读生产玩家进度/数据库、未执行生产 DDL/部署或 Quest/BQ 开关操作，未触碰 S1/S2。
- 本轮文档修改后执行 `git diff --check`；无代码改动、测试数量变化、JAR 构建或部署记录。

### 2026-10-10 - R.2.10 Checkbox 确认语义与导入/JDBC 回归

- 按用户刚确认的检测合同收敛任务边界：非消耗 Retrieval/Fluid 自动观察玩家背包/已接入容器快照，消耗型任务才显式提交；GT 机器产物按物品/流体存在检测，不伪造玩家 Crafting 事件；同一 Quest 的混合个人/Party Task 归属留到职业系统设计阶段。当前 Base 的 `QuestObservationPlan` 已符合非消耗项自动轮询、消耗项不启用快照，但采集入口现在只读取 `EntityPlayer.inventory`，不代表任意容器或 GT 机器已接入。
- 对照固定源码 `TaskCheckbox.detect`、`NetTaskCheckbox`、`ParticipantInfo`：Checkbox 本身是手动确认任务，服务端从认证玩家解析身份；`fullySyncQuests=false` 仅写发送者，true 时写 Party UUID 列表。上游配置默认 false 并标 NYI；本轮未读取 S2 `questbook.cfg`，不推断服务器覆盖值。
- 新增 `importedBqCheckboxIsAnAuthenticatedManualPlayerAction`：合成 Checkbox BQ JSON → importer/migration → 隔离 PostgreSQL 发布目录 → `AuthenticatedCheckboxConfirmationService` + JDBC `QuestEvaluationPlanner`/Runtime；验证只有点击者完成、其他玩家无进度、重复确认无二次完成，并且观察计划不将 Checkbox 误列为库存/流体轮询任务。没有修改生产逻辑、参与主体模型、Quest 网络合同或数据库结构。
- CodeGraph 起始 `status` 索引干净；`explore`/`node` 查看认证确认、Quest Planner、观察计划调用链，`impact AuthenticatedCheckboxConfirmationService.confirm` 命中服务及调用/单测，`affected` 对 Core 返回 46 个宽候选、按真实变更的导入集成测试收敛 1 个用例；改后 `sync` 更新 1 文件/49 nodes，`impact` 与 `affected` 命中新集成方法/测试类。
- 自动验证：`:galaxy-quest-core:test` 267 项与 `:galaxy-quest-bq-importer:test` 48 项，共 315 项实际执行，0 skipped/failure/error；`:assemble` 和 `verifyUi2SelfContained` 通过。临时 PostgreSQL 16 使用 `quest_r210_checkbox_it`、512 MiB tmpfs、无宿主卷，仅绑定 `127.0.0.1:55486`；删除容器并确认端口释放。首次测试的默认 root 容器遗留 root 所有权导致普通 UID 重跑 AccessDenied；仅恢复本轮生成的 importer `build/test-results` 与 `build/reports/tests` 目录所有权后，以 UID 1000 重跑通过。`git diff --check` 通过。没有生产进度/数据库读取、生产 DDL、部署、Quest/BQ 开关或 S1/S2 操作。
- 自动 Checkbox 个人 JDBC 路径可复验；`fullySyncQuests` 服务器配置、真实终端/Forge 玩家动作、Party Checkbox 与跨服时序仍 BLOCKED。Checkbox/M.2/BQ 替代整体不升级为完成。下一项先按能力矩阵审计剩余实际奖励类型，奖励行为差异未经用户确认不得更改。

### 2026-10-10 - R.2.9 Location 导入与重复领奖周期自动闭环

- 在保留既有未提交工作树内容的基础上，只新增导入器隔离集成回归，没有改变 Location evaluator、Party 合同、数据库 schema 或玩家行为。测试从合成 BQ `bq_standard:location` JSON 经 `BqMigrationPlanner`/`BqMigrationDatabaseService` 写入并发布 PostgreSQL 定义，重读后验证 cooldown ticks→毫秒、`QuestObservationPlan.observesLocation()` 及 `factType/dimension/range/invert` 参数。
- 以 `MinecraftQuestFactFactory.locationObserved` 实际输出形状（`dimension/x/y/z/biome`）构造合成事实；与数据库 Party roster、本服在线成员目录、`BqPartyActiveQuestQuery`、Planner、`BqHuntPartyAssignmentPolicy`、Runtime 和 JDBC 进度/权益完整串接。两名在线成员分别得到 PLAYER cycle 0 完成与可领取权益；原 event id 重放不能再次插入；未领取/领取前冷却不会重开；两人分别领取后使用 PostgreSQL `claimed_at` 锚定相对周期，冷却到期时两人各进入并完成 cycle 1。
- CodeGraph 改前 `status` up-to-date；`impact LocationTaskEvaluator.evaluate` 指向 evaluator/Core 用例，`impact QuestEngine.evaluate` 覆盖 Runtime 与周期回归，`impact BqMigrationDatabaseService.apply` 指向导入 PostgreSQL 集成；新增测试完成后 `sync` 更新 1 文件/47 nodes，`impact` 命中新集成方法，`affected` 命中 `BqMigrationDatabaseIntegrationTest`。同时静态核实 `MinecraftQuestFactFactory.locationObserved` 字段和 `QuestGameplayEventHandler` 的 100 tick 位置调度；没有声称真实 Forge 对象已测试。
- 实际验证：`:galaxy-quest-bq-importer:test` 47 项（含 6 个 importer PostgreSQL 集成方法）全执行、0 skipped/failure/error；`:galaxy-quest-core:test` 267 项全执行、0 skipped/failure/error；`QuestPostgresIntegrationTest.bqMeetingObservationUsesDatabasePartyMembershipForPersonalProgress` 1 项全执行、0 skipped/failure/error。共 315 项；`:compileJava :compileTestJava assemble verifyUi2SelfContained` 通过（44 actionable tasks，18 executed、26 up-to-date），最终 `git diff --check` 通过。临时 PostgreSQL 16 使用测试库 `quest_r29_location_test`、512 MiB tmpfs、无宿主卷，仅绑定 `127.0.0.1:55485`；完成后容器已删除并确认端口释放。首次 Gradle 插件解析遇网络超时；只读验证后仅对单次临时构建使用本机 7892 代理变量，没有改系统网络/代理服务。
- R.2.9 的 Location/Meeting 自动门槛完成；真实 Forge/GTNH 玩家 tick、Entity/World 行为、BQ Party 活跃缓存以及多服投递/领取时序仍 BLOCKED。未读取生产玩家进度或数据库，未执行生产 DDL/部署，未启用 Quest 或停用 BQ，未操作 S1/S2。

### 2026-10-10 - R.2.9 Meeting 合并半径逐 Task AABB 修正（Location 周期链继续中）

- 固定源码对照 `Reference/BetterQuesting/.../TaskMeeting.java:detect/tickTask`：BQ 每 60 个玩家 tick，按单个 Meeting Task 使用 `player.boundingBox.expand(range, range, range)` 查询实体。Base 为避免重复查询会按已发布任务最大半径生成共享快照；此前 evaluator 只验证 `observedRadius >= task.range`，会让小半径 Task 错把大半径快照里的远处实体计入。
- 最小修复：Minecraft 附近实体事实现在携带观察者和每个注册实体的 AABB 六个边界；Core `MeetingTaskEvaluator` 在共享观察半径大于当前 Task range 时，按 BQ 严格 AABB 相交规则再次筛选实体。几何缺失/非法时不计数；旧的精确半径快照因原采集查询已经等于任务查询范围，仍可按旧事实处理。未改 Party 身份、注册实体匹配、数量/NBT 或服务端任务合同。
- Core 新增边界回归：远实体被小范围 Task 排除、实体中心略超圆形距离但 AABB 与任务查询盒相交时计入、仅接触面不计入、超范围快照缺失 geometry 时失败闭合。将既有隔离 PostgreSQL Meeting Party 用例扩成 range=4/range=8 两个 Task：最大半径快照先只完成 range=8、不发奖励；第二个实体盒与小范围 AABB 相交后两位 Party 成员各自 PLAYER 进度完成并各有唯一奖励，事实重放为 `DUPLICATE`。
- CodeGraph：开始 `status` 为 up-to-date；改后显示 4 个待同步文件，`sync` 更新 4 文件/284 nodes。`impact MeetingTaskEvaluator.evaluate` 命中 evaluator 与 Core 测试；`impact MinecraftQuestFactFactory.nearbyEntitiesObserved` 命中 PlayerTick 调度；`affected` 返回 82 个跨项目候选，按直接路径选择 Core evaluator 测试与 `QuestPostgresIntegrationTest.bqMeetingObservationUsesDatabasePartyMembershipForPersonalProgress`。
- 实际验证：Docker `:galaxy-quest-core:test --tests ...BqCompatibleTaskEvaluatorsTest` 41 项、隔离 PostgreSQL 16 集成用例 1 项，共 42 项实际执行，0 skipped/failure/error；根 `:compileJava :compileTestJava` 通过（22 actionable tasks，5 executed、17 up-to-date）；`:assemble :verifyUi2SelfContained` 通过（29 actionable tasks，6 executed、23 up-to-date）。临时 PG `jgb-r29-meeting-it` 使用 512 MiB tmpfs、无宿主数据卷、仅发布 `127.0.0.1:55484`，结束后已停止自动移除且端口释放。最终 `git diff --check` 通过；未跑全量 root `test`。
- 同步开发导航、唯一能力矩阵、M.2 行为矩阵与 Docs README。另按 33 个 Location occurrence 的既有只读画像确认当前实际判定只依赖 dimension；本轮没有改 Location evaluator。R.2.9 仍进行中，Location 的导入→Party/PLAYER/JDBC→重复周期回归及 Adapter 事实格式专项验证待做；真实 Forge/GTNH tick、EntityList/NBT 与跨服时序仍 BLOCKED。仅用合成事实与隔离测试数据库；未访问生产玩家进度/数据库、执行生产 DDL/部署、启用 Quest、停用 BQ 或操作 S1/S2。

### 2026-10-10 - R.2.6 在线 PARTY Crafting 个人统计自动闭环

- 按用户确认的合同完成 R.2.6：每个服务端只采样本服在线 PARTY 成员各自的 `objectCraftStats`，为每名玩家提交独立 PLAYER 绝对快照；不把 PARTY 成员或不同服务器计数相加。非消耗物品/流体任务由玩家背包或已接入容器快照自动观察，GT 机器产物不伪造成玩家 crafting 事件；消耗型任务仍要求单独提交。M.2 主体模型未变。
- 新增 `BqPartyCraftingStatisticsMembers`，根据数据库 PARTY membership 与本服在线玩家目录解析采样名单；无 PARTY 时退化为当前在线玩家，拒绝歧义 membership。`QuestGameplayEventHandler` 对匹配的 craft/smelt/anvil 事件按“在线成员→同 Task 统计目标”顺序读取个人统计，事实保留各自玩家 UUID 和原有幂等键；事件入口捕获异常，统计快照不走 Party 进度 fan-out。观察计划仍过滤不匹配输出。
- 增加 Core 成员名单/观察计划/evaluator/在线资格回归、根事件分发测试，以及 `QuestPostgresIntegrationTest.bqCraftingStatisticsPersistEachOnlinePartyMembersPersonalSnapshot`：隔离 PostgreSQL 中两个成员的 `[5,2]` 与 `[1,4]` 个人向量保持分离，重放原 fact key 得到 `DUPLICATE`。CodeGraph 开始 `status` 发现 3 added/9 modified；`sync` 纳入 12 文件、665 nodes；完成后针对事件处理、Party active-query/planner 做 `impact`，并对变化文件执行 `affected`（83 个候选，按直接链路收敛到 Core 5 类、根 2 类与 PostgreSQL 集成类）。索引最后一次 sync 更新新集成测试 140 nodes。
- 最终隔离验证：Core 定向 81 项，根事件分发/失败 4 项，PostgreSQL 个人统计 PARTY 集成 1 项，共 86 项实际执行，0 skipped/failure/error。PG16 容器 `jgb-r26-craft-stats-it` 使用 512 MiB tmpfs、无宿主数据卷，仅绑定 `127.0.0.1:55483`；测试结束后停止并移除，端口已确认释放。Gradle 在 `galaxy-dev:local` 的一次初次组合运行无法清理此前 root-owned 的精确测试报告目录；核实属主后只修复 `galaxy-quest-postgres/build/test-results/test/binary` 生成目录属主，独立重跑通过。最初两个事件用例暴露 null 触发者测试 seam 未调用自定义成员解析器，以及 PARTY/目标遍历顺序与 BQ 源码相反；已修正并复验。
- 开发导航、唯一能力矩阵、M.2 行为矩阵、Docs README 与本 Worklog 已同步产品合同和状态。当前只完成自动回归与代码路径，不宣称真实 Forge/GTNH 验收、M.2 完成或 BQ 替代；实际 `EntityPlayerMP` 统计文件、各服在线目录/事件时序仍待运行证据。未访问生产玩家进度/数据库、执行生产 DDL/部署、启用 Quest/停用 BQ，也未触碰 S1/S2；`:assemble :verifyUi2SelfContained` 通过（29 actionable tasks，29 executed），`git diff --check` 通过；未运行全量根 `test`、未部署。

### 2026-10-10 - R.2.6 Crafting/Party 回归链复核

- 根据当前能力矩阵继续核对普通产物与统计检测的分流，CodeGraph `explore` 覆盖 `QuestGameplayEventHandler`、`QuestEvaluationPlanner`、`BqPartyActiveQuestQuery`、`QuestRuntimeService`、`RuntimeQuestFactSink`；`impact BqPartyActiveQuestQuery.hasActiveMember` 指向 Planner 与资格测试；`affected` 给出 82 个跨项目候选，按直接调用边界运行 Core 五类与根两类测试。
- `:galaxy-quest-core:test` 定向运行 `AuthenticatedCraftingStatisticsDetectionServiceTest` 6、`BqCompatibleTaskEvaluatorsTest` 40、`BqPartyActiveQuestQueryTest` 6、`QuestEvaluationPlannerTest` 23、`QuestObservationPlanTest` 7；根 `:test` 运行 `QuestCraftingStatisticsDispatchTest` 2、`QuestGameplayEventHandlerFailureTest` 1。XML 合计 85 项实际执行，0 skipped/failure/error；Gradle 28 actionable tasks，2 executed、26 up-to-date。
- 这确认现有回归覆盖了 Party 活跃 Quest 在线资格、Crafting Task matcher、统计事实分发及事件失败边界；并不证明真实 Forge 回调、`EntityPlayerMP` 本地统计读取或 BQ 端到端等价。显式检测的 Quest/Task 粒度和 PARTY 统计接收范围仍属未确认产品合同；没有改业务代码、网络或数据库合同。本轮无数据库访问、生产 DDL、部署、Quest 开关或 BQ 停用，M.2 仍 BLOCKED。

### 2026-10-10 - Crafting 普通产物 PARTY 资格与 JDBC 进度回归

- 在不触碰待用户确认的“显式统计检测范围/入口”合同下，补齐 R.2.6 普通 craft 事实的数据库 PARTY 资格链测试：PostgreSQL Party roster + 合成当前服在线目录 + `BqPartyActiveQuestQuery` + Planner/Runtime/JDBC。前置已完成的在线成员获得自己的 PLAYER 任务进度；未解锁的事件来源玩家仍保留 LOCKED 进度；完成奖励权益只归符合资格的成员；进行中的同事实键重放为 `DUPLICATE`。
- 新增 `QuestPostgresIntegrationTest.bqCraftingEventsUseDatabasePartyEligibilityAndPersistPersonalProgress`。初次测试把“任务完成后重放同事实仍必为 DUPLICATE”设成了过强断言，实际 Planner 在无剩余活动 Quest 时先返回 `IGNORED`；没有据此更改运行时语义，而是将幂等断言移至任务仍进行中的重放，并保留完成后奖励权益仅一条的断言。
- CodeGraph 起始索引显示该 PostgreSQL 集成文件 1 个待同步修改；最终 `sync` 更新 1 文件、137 nodes；`impact` 与 `affected` 均指向新集成方法/测试类。最终回归使用专用 PostgreSQL 16 Alpine 容器 `jgb-quest-r26-craft-party-final`，数据库 `quest_r26_crafting_party_test`，tmpfs、无数据卷、只绑定 `127.0.0.1:55482`；Gradle 容器未继承 Compose 配置。首次 Gradle 配置因未传代理而在 GTNH 插件请求超时，检查确认本机 7892 正在监听后，仅给本次临时 Gradle 容器设置代理变量并重跑成功；未修改主机代理或网络服务。PostgreSQL 集成方法 1/1、Core `QuestEvaluationPlannerTest` 23/23，0 skipped/failure/error；测试容器已停止移除，端口确认释放，`git diff --check` 通过。
- 该回归验证 Base 当前普通产物事实 PARTY 路径在合成在线目录下的 JDBC 行为，不调用真实 Forge 事件或 Minecraft 在线玩家目录；不验证 `craft-statistics` 逐 PARTY 成员采集，也不证明完整 BQ 等价。R.2.6 两项产品合同仍待用户确认，R.2.9 Location/Meeting 仍排队；M.2/BQ 替代保持 BLOCKED。未访问生产数据库/玩家进度、未执行生产 DDL/部署、未启用 Quest/停用 BQ，未操作 S1/S2。

### 2026-10-10 - 选择下一长工作包：Location / Meeting 行为兼容

- 对照唯一能力矩阵、M.2 行为矩阵、需求复盘、迁移路线和开发导航后，确认当前仍处于 M.2；R.1 已收口，Crafting 的 Quest 级统计入口与本服在线 PARTY 统计归属仍待产品合同，不能自行变更。
- 为避免只修一个小点就切换目标，已将 R.2.9 排入后续长工作包：覆盖 S2 `DefaultQuests` 实际存在的 33 个 Location、1 个 Meeting，从固定 BQ 源码到 Base 采集、求值、Party/周期与隔离 PostgreSQL 进度链逐段审计。未出现在实际清单中的 interact 类型不纳入本包；按长期目标顺序，必须先解决 R.2.6 Crafting 合同并收口后才能启动 R.2.9。
- 已发现的优先回归点是 Meeting 合并最大观察半径后，各 Task 是否仍按自身 BQ bounding-box 范围筛选附近实体；计划中的实现必须复现 BQ 的 AABB 语义，不以实体中心距离近似。Location 仅对当前任务实际使用的维度语义做必要验证，不为结构/视线/Biome 等未使用选项泛化。
- 预查 CodeGraph：`LocationTaskEvaluator.evaluate` 影响 evaluator 与两条 Core 测试；`MeetingTaskEvaluator.evaluate` 影响 evaluator 与既有 Core Meeting 回归；`QuestGameplayEventHandler.onPlayerTick` 和 `QuestObservationPlan` 为 Minecraft 采集边界。CodeGraph 索引当前可用；实现前仍须对准确的 snapshot/fact 工厂/Planner 调用链执行 `impact/affected`。
- 本记录仅指定下一工作包，没有改业务代码或测试。实施后要求 Core + Adapter + 隔离 PostgreSQL 导入/进度/周期回归、根构建与自包含门禁；未测试的 Forge/GTNH 事件时序继续标 BLOCKED。无生产数据库/玩家进度访问、生产 DDL、部署或 Quest/BQ 开关变化。

### 2026-10-10 - Crafting shift-craft 数量回退边界对齐

- 固定 BQ `EventHandler.onItemCrafted`/`TaskCrafting.onItemInternal` 源码只对 `InventoryCrafting` 且事件产物 `stackSize == 0` 回查配方；非正回查结果不会推进任务。Base 原先用 `<= 0` 触发回查，可能把负数异常栈量误当有效合成。现改为仅零值触发，并通过包级纯函数隔离回退规则。
- 新增 `craftingAmountFallbackMatchesBqShiftCraftBoundary`：零数量回查配方、负数和正数直通、无合成矩阵不回查；与既有统计分发测试合计根 `QuestCraftingStatisticsDispatchTest` 2 项通过。
- CodeGraph `sync` 更新根事件处理器和测试 2 文件、139 nodes；`impact onItemCrafted` 限于 Forge 回调入口，`impact resolveCraftedOutputAmount` 命中新规则与测试；`affected` 给出 20 个 Quest 测试候选，按直接影响选根 Crafting 回归。Docker 构建中 Core 4 个相关类共 87 项测试因前一轮相同输入而 `UP-TO-DATE`（此前该轮均实际执行并通过）；本次根侧重新执行统计分发 2、事件失败边界 1、消费适配 19，共 22 项，0 skipped/failure/error。合并当前源代码下 109 项均通过；`git diff --check` 通过。
- `:assemble :verifyUi2SelfContained` 通过（29 actionable tasks，6 executed、23 up-to-date）。未连接数据库、未访问玩家进度、未改网络/Party 合同、未部署或触碰生产 Quest 开关。Crafting Quest 级显式检测范围及 PARTY 本服在线成员统计归属仍待用户决定，M.2 状态不变。

### 2026-10-10 - Crafting 统计事件分发回归补齐

- 在不改变 Crafting/Party 语义的前提下，为 `QuestGameplayEventHandler.submitCraftingStatistics` 增加包内可替换的统计事实生成 seam，并新增 `QuestCraftingStatisticsDispatchTest`。回归证明无关产物不会采集或持久化统计；匹配产物只分发同一匹配 Task 的所有统计目标、不带入另一 Task 的目标，并保留 craft/smelt/anvil 渠道。
- 测试使用合成 `Item` 和确定性属性映射，因为该纯 JUnit 环境未初始化 Minecraft 1.7.10 vanilla `Items` 注册表；因此这验证的是观察计划到事实分发，不是 Forge 事件构造、真实 `ItemStack` 注册名或 `EntityPlayerMP.objectCraftStats` 读取。隔离 `galaxy-dev:local` 执行 `:test --tests com.jsirgalaxybase.modules.quest.infrastructure.minecraft.QuestCraftingStatisticsDispatchTest`：1 项，0 skipped/failure/error。前几轮尝试分别暴露输出 fixture 未得到匹配、测试辅助列表类型不符、vanilla Item 注册表未初始化；改为独立合成 Item 与显式属性映射后通过。
- 随后联合执行 Core 的 `BqCompatibleTaskEvaluatorsTest` 40、`QuestObservationPlanTest` 7、`QuestEvaluationPlannerTest` 23、`QuestEngineTest` 17，以及根项目 `QuestGameplayEventHandlerFailureTest` 1、`MinecraftPlayerConsumptionPortTest` 19、新分发测试 1；XML 共 108 项，全部执行，0 skipped/failure/error。`:galaxy-quest-core:test :test` 成功。该验证未启动 PostgreSQL，也未跑全量 `assemble`/自包含门禁。
- CodeGraph `sync` 更新 1 个根事件处理器文件（55 nodes）；`impact submitCraftingStatistics` 命中三个 Forge 回调及新测试，`impact craftingStatisticsForOutput` 命中 Core 观察计划、隔离 JDBC 回归和相关测试；`affected` 给出 20 个 Quest 测试候选，按直接边界选择根分发测试。最终 `git diff --check` 通过。R2.6 的 Quest 级检测入口及 PARTY 在线统计归属仍待用户定合同；没有生产玩家进度/数据库访问、生产 DDL、部署、Quest 开关或 BQ 停用。

### 2026-10-10 - Crafting 兼容 WIP 定向回归复核

- 保护现有未提交 Quest 工作树；CodeGraph `status` 为 up-to-date（索引由较早引擎版本创建）。通过 `explore/node` 复核 `QuestGameplayEventHandler.submitCraftingStatistics`、`MinecraftQuestFactFactory.craftingStatisticObserved`、`QuestObservationPlan.craftingStatisticsForOutput` 与 BQ `TaskCrafting.onItemInternal/detect`；`impact` 与 `affected` 选择了 Core 求值/观察计划测试及根事件失败、消费适配测试。
- 用不含 Compose `.env`、数据库凭据或 Quest 数据库环境变量的一次性 `galaxy-dev:local` 容器重跑：Core 的 `BqCompatibleTaskEvaluatorsTest` 40、`QuestObservationPlanTest` 7、`QuestEvaluationPlannerTest` 23、`QuestEngineTest` 17；根 `QuestGameplayEventHandlerFailureTest` 1、`MinecraftPlayerConsumptionPortTest` 19。共 107 项，0 skipped/failure/error；Core Gradle 3 项任务、根项目 26 项任务均成功，`git diff --check` 通过。未连接数据库。
- 覆盖边界仍然明确：R.2.6 的 BQ JSON→合成统计事实→隔离 JDBC 回归不调用真实 Forge `ItemCraftedEvent`、`ItemSmeltedEvent` 或 `AnvilRepairEvent`，也不验证 `EntityPlayerMP` 的 `objectCraftStats` 读取。当前根侧唯一直接命中的 `QuestGameplayEventHandlerFailureTest` 测的是 tick 数据库失败边界，不是 Crafting 事件桥接。Quest 级显式检测范围和 PARTY 成员统计归属仍待用户决定；本轮未改行为、网络、数据库合同或生产开关。

### 2026-10-10 - Crafting 统计事实 PARTY 路由差异确认

- CodeGraph `status` 为 up-to-date；`explore`/`node` 检查 `QuestGameplayEventHandler.submitCraftingStatistics`、`MinecraftQuestFactFactory.craftingStatisticObserved`、`BqPartyTaskProgressScope.isCraftingEvent`、`BqHuntPartyAssignmentPolicy.select`、`QuestEvaluationPlanner.factMembershipFor`，并追踪 `Reference/BetterQuesting/TaskCrafting.detect`。确认普通 craft/smelt/anvil 产物事实和 `craft-statistics` 统计事实走不同分支。
- BQ `TaskCrafting.detect` 对本服在线 PARTY 成员各自读取本地统计并更新个人进度。Base 统计 fact 由触发事件玩家提供统计值，通道为 `craft-statistics`；该通道不被 PARTY fan-out 识别，因此目前只更新触发玩家的 PLAYER 统计进度。普通产物事实会 Party fan-out，不能据此认为统计值也在队员间逐人采集。
- 这是现存源码行为与 BQ 的明确差异，不改变实现：统计接收者范围及终端动作粒度仍在等待用户合同选择。更新开发导航、唯一能力矩阵和 M.2 行为矩阵；未改 Java、网络/数据库合同或测试。`git diff --check` 通过。

### 2026-10-10 - Crafting 自动统计跨 Task 隔离回归

- 继续 R.2.6 源码核对，确认 `QuestObservationPlan` 为所有被同一输出触发的 Task 合并所需统计目标；统计事实携带原触发产物/渠道，`ItemVectorTaskEvaluator` 会对每个 Task 再次匹配触发项。因此未发现跨 Task 进度串扰，且无需生产逻辑修改。
- 新增 `craftingStatisticsFromOneTaskDoNotRefreshAnotherTaskWithoutMatchingTrigger`：含 iron+gold 的 Task 被 iron 产物触发后，可刷新自身 gold 统计；只含 gold 的另一 Task 收到相同统计事实时因原触发项不匹配而保持 0。此行为对应 BQ 对每个 Crafting Task 分别调用 `onItemInternal` 和 `detect`。
- CodeGraph `status` 起始 up-to-date；`explore` 检查 BQ `TaskCrafting`、Base `QuestObservationPlan.craftingStatisticsForOutput`、`QuestGameplayEventHandler.submitCraftingStatistics`、`ItemVectorTaskEvaluator.evaluate`。改后 `sync` 更新 1 个 Core 测试文件（52 nodes）；`impact ItemVectorTaskEvaluator.evaluate` 覆盖同 Task/跨 Task evaluator 回归，`affected` 精确命中 `BqCompatibleTaskEvaluatorsTest`。
- `galaxy-dev:local` 执行 `:galaxy-quest-core:test --tests com.jsirgalaxybase.quest.core.BqCompatibleTaskEvaluatorsTest` 成功；XML 为 40 项、0 skipped/failure/error，Gradle 3 actionable tasks，2 executed、1 up-to-date。`git diff --check` 通过。
- 本轮仅新增 Core 回归与文档，没有生产代码变化；未运行隔离 PostgreSQL、全量 test、assemble 或自包含门禁。Crafting Quest 级入口与本服在线 PARTY 统计归属仍待用户合同选择；未访问生产玩家进度/数据库、执行生产 DDL、部署、启用 Quest、停用 BQ 或操作 S1/S2。

### 2026-10-10 - R.2.8 Hunt 注册实体边界与隔离 JDBC 回归

- 按固定 BQ `Reference/BetterQuesting/src/main/java/bq_standard/tasks/TaskHunt.java:onKilledByPlayer` 和 `bq_standard/handlers/EventHandler.java:onEntityKilled` 核对：BQ 要求目标类可由 `EntityList.stringToClassMapping` 解析，且被击实体有注册 ID；目标或实体注册数据缺失均不增加进度，子类匹配只在有效注册目标下执行。
- 修复 `MinecraftQuestFactFactory.entityKilled`：未注册的击杀实体不附带父类 aliases，也不尝试写实体 NBT。修复 `SubjectCountTaskEvaluator`：任务目标 subject 或事实 exact subject 缺失/空白时保持现有进度。新增 `huntRejectsMissingTargetAndUnregisteredEntitySubject` 覆盖缺失目标，以及只有已注册父类 alias 的未注册实体事实。
- CodeGraph 开始前 `status` 为 up-to-date；检查 `QuestGameplayEventHandler.onEntityKilled` → fact factory、`SubjectCountTaskEvaluator.evaluate`、`BqHuntPartyAssignmentPolicy.select` 的调用与影响链。改后同步 Minecraft fact factory、evaluator 与测试文件（133 nodes）；`impact` 覆盖两个被改入口及回归测试，`affected` 对源码返回较宽的跨模块候选集，按直接调用关系补跑 Core evaluator、隔离 PostgreSQL Hunt PARTY/JDBC 集成与根编译。
- `galaxy-dev:local` 执行 `:galaxy-quest-core:test --tests com.jsirgalaxybase.quest.core.BqCompatibleTaskEvaluatorsTest :compileJava` 成功；测试 XML 为 39 项、0 skipped/failure/error，根 `compileJava` 成功（19 actionable tasks，4 executed、15 up-to-date）。随后仅针对 `QuestPostgresIntegrationTest.concurrentBqHuntFactsUseDatabasePartyMembershipAndCompletePersonalProgress` 运行 1 项隔离 PostgreSQL 测试，0 skipped/failure/error。测试库 `quest_r28_hunt_test` 使用 PostgreSQL 16 Alpine、tmpfs、无宿主数据卷、绑定 `127.0.0.1:55481`；容器已停止并移除，确认端口释放。
- `git diff --check` 通过。没有运行全量 `test`、`assemble` 或 `verifyUi2SelfContained`；没有真实 Forge `LivingDeathEvent`、FakePlayer/取消事件、GTNH EntityList/NBT 或跨服时序证据，故 Hunt 与 M.2 仍 BLOCKED。没有访问生产玩家进度/数据库、执行生产 DDL、部署、启用 Quest、停用 BQ 或操作 S1/S2。

### 2026-10-10 - R.2.7 block_break 导入与隔离 PostgreSQL 进度回归

- 按固定 BQ 参考源码 `EventHandler.onBlockBreak` / `TaskBlockBreak.onBlockBreak` 核对：顺序匹配需求，首个匹配命中后立即 break；即使该项已达标，后续同类方块仍不会越过它推进后续重叠项。只新增自动回归，不修改生产代码、玩家交互、Party 合同或网络/数据库合同。
- 新增 `importedBlockBreakPreservesFirstMatchingRequirementOrderInJdbcProgress`：合成 BQ 定义 → importer → 隔离 PostgreSQL 发布 → `BqPartyInventorySnapshotFactSink` → Planner/Runtime → JDBC 进度。验证 `[1,0]`、原事实键重放为 DUPLICATE、第一项达标后同类事实仍为 `[2,0]` 且 Quest 未完成，独立的另一需求后完成为 `[2,1]`。
- CodeGraph `status/sync` 起始索引干净；核对 `QuestGameplayEventHandler.onBlockBreak` 的 `callees/impact`、`MinecraftQuestFactFactory.blockBroken`、`BlockVectorTaskEvaluator.evaluate`，以及 sink `accept` 的 callers/callees/impact。改后 `sync` 更新测试文件（202 nodes），新测试 `impact` 与 `affected` 均指向 `BqPartyInventorySnapshotFactSinkPostgresTest`。
- 使用 `galaxy-dev:local` 运行根 `:test --tests com.jsirgalaxybase.modules.quest.infrastructure.minecraft.BqPartyInventorySnapshotFactSinkPostgresTest`（11 项）、`:galaxy-quest-core:test --tests com.jsirgalaxybase.quest.core.BqCompatibleTaskEvaluatorsTest`（38 项）、`:galaxy-quest-bq-importer:test --tests com.jsirgalaxybase.quest.bqimport.BqStandardCodecTest`（5 项），共 54 项，0 skipped/failures/errors。根 Gradle 26 actionable tasks，9 executed、17 up-to-date。数据库为临时 PostgreSQL 16 Alpine，`quest_r27_blockbreak_test`，tmpfs、无数据卷、仅 loopback `127.0.0.1:55480`；测试后已停止并移除容器，确认端口释放。未使用生产数据库凭据或数据。
- 本轮没有运行全量 `test`、`assemble` 或 `verifyUi2SelfContained`；该集成只使用合成事实，不调用 Forge `BreakEvent`，不证明真实 TileEntity/OreDictionary 注册值、Party 在线资格或 GTNH 时序。block_break 与 M.2 继续 BLOCKED；无生产玩家进度/数据库访问、生产 DDL、部署、Quest 开关、停用 BQ 或 S1/S2 操作。

### 2026-10-10 - R.2.6 Crafting 合成事实到隔离 PostgreSQL 回归

- 在不改变 R.2.6 待确认合同的前提下，新增根项目回归 `importedCraftingOutputRefreshesSameTaskStatisticsAndPersistsAbsoluteProgress`：合成 BQ Crafting JSON → importer → 发布目录/观察计划 → 匹配产物事实 → 同 Task 两个绝对统计目标 → `BqPartyInventorySnapshotFactSink`/Runtime/Planner/JDBC。断言进度 `[1,0] → [4,3] → [4,5]`、单个匹配输出会刷新同 Task 所有统计目标、统计事实按原 event key 去重并最终完成。用单人 membership 明确隔离用例范围；未改变 Forge 接线、Party 统计归属、终端入口或服务端行为。
- CodeGraph `status` 起始 up-to-date；`explore` 核对 `QuestObservationPlan.craftingStatisticsForOutput`、`QuestGameplayEventHandler.submitCraftingStatistics`、`BqPartyTaskProgressScope.isCraftingEvent`、`ItemVectorTaskEvaluator.evaluate`、`BqStandardCodec.taskParameters`、`QuestEvaluationPlanner.requestsFor` 与 `QuestRuntimeService.apply`。`sync` 更新集成测试（201 nodes）；`impact` 检查观察计划、Planner、Runtime 入口；`affected` 选择该 JDBC 集成类及 Core 观察/求值/Planner 回归。
- CodeGraph 选择的 Core/Importer/PostgreSQL 模块定向组运行 357 项，44 项因本次未提供数据库配置而 assumption-skip，0 failure/error。根隔离 PostgreSQL 集成类完整运行 10 项，0 skipped/failure/error；新用例单独运行也通过。临时 PostgreSQL 16 Alpine 容器使用 tmpfs、无宿主数据卷、测试库 `quest_r26_crafting_test`、loopback 端口 55479；容器已停止并因 `--rm` 移除，端口已释放。首次回归失败是夹具错误地使用 JDBC Party membership，资格门控挡住了 PLAYER 测试事实；改为显式单人 resolver 后通过。一次中间编译错误为新增测试遗漏 import，已修复。`git diff --check` 通过。
- 本轮仅新增测试与文档记录；未运行全量根 `test`、`assemble` 或 `verifyUi2SelfContained`，本轮 357 项中的 44 个 skip 不能视作通过，故不能作为完整 M.2 验收证据。合成事实链不证明实际 Forge 回调、Party 统计范围/在线顺序、Quest 级显式动作；这些继续待用户确认/后续验收。M.2/BQ 替代仍 `BLOCKED`。未访问生产数据库/玩家进度、执行生产 DDL/部署、启用 Quest、停用 BQ 或操作 S1/S2。

### 2026-10-10 - R.2.6 合成统计事实按匹配 Task 采集

- 按现有 BQ 合同收口一个纯噪声路径：产物事件只会使匹配当前 Crafting Task 的 BQ `onItemInternal` 进入该 Task 的统计 `detect`。Base 原先对所有已发布统计目标读取 `objectCraftStats` 并生成事实，再由 evaluator 拒绝与触发产物无关的事实；现在由 `QuestObservationPlan` 复用 `ItemVectorTaskEvaluator` 的物品与渠道 matcher，在读取玩家统计前筛出被触发的任务目标。无关输出不会读取统计或入队；匹配输出仍刷新该任务所有已启用的统计目标，PLAYER/PARTY 归属与 UI 动作未改。
- `MinecraftQuestFactFactory.itemProducedAttributes` 与正式产物事实共享注册名、meta、damageable、OreDictionary、NBT、数量和渠道属性，避免筛选与后续 evaluator 使用不同的物品描述。Core 测试覆盖：匹配目标可带出同 Task 的其它统计目标、无关目标返回空、禁用渠道不触发。
- CodeGraph 起始 `status` 提示 2 个待同步文件；先同步此前改动，`explore` 核对观察计划、产出事件、统计 evaluator 调用链。完成本轮主要代码后 `sync` 更新 3 个文件（161 nodes）；将 matcher 实例改为静态复用后再同步 1 个文件（43 nodes）。`impact` 检查 `craftingStatisticsForOutput`、`matchesCraftingStatisticsTrigger`、`submitCraftingStatistics`、`itemProducedAttributes`；`affected` 返回 81 个跨模块候选，按直接边界选择 `QuestObservationPlanTest`、`BqCompatibleTaskEvaluatorsTest` 与根项目 Java 编译。最终 CodeGraph status 为 up-to-date。
- 项目缓存 Gradle 9.3.1 离线执行 `:galaxy-quest-core:test` 的两个测试类：`QuestObservationPlanTest` 7 项、`BqCompatibleTaskEvaluatorsTest` 38 项，共 45 项，0 skipped/failure/error。Docker 首次 `compileJava` 因 convention 插件网络超时未进入编译；只读确认本机 7892 代理监听后，在一次性 `docker run` 环境变量中设置代理并重跑根 `compileJava`，通过（19 actionable tasks，7 executed、12 up-to-date）。未修改系统代理、Compose、远程访问服务或防火墙；本轮未运行隔离 PostgreSQL、assemble、自包含门禁或全量 test。`git diff --check` 在文档更新后另行执行。
- 尚未完成 Crafting 的事件→隔离 PostgreSQL 进度闭环；本轮仅验证筛选与 Core 行为。显式检测仍按单 Task/认证玩家，Quest 聚合入口及 PARTY 本服在线成员统计范围仍待产品合同确认；真实 Forge/GTNH 事件和在线顺序未验收。M.2 与 BQ 替代继续 `BLOCKED`。无生产玩家进度/数据库读取、生产 DDL、部署、Quest 开关或 BQ 停用，也未操作 S1/S2。

### 2026-10-09 - BQ M.2 Crafting 自动统计复评语义回归

- 补充核对固定 BQ `TaskCrafting`：`onItemCraft`、`onItemSmelt`、`onItemAnvil` 均进入 `onItemInternal`；只有输出匹配当前 Task 并推进进度后，才调用同一个 Task 的 `detect`。该检测会刷新该 Task 的其他统计型未完成目标；完全无关输出不会触发。修正 R2.6 旧文字中“BQ 只在显式 Quest Detect 回填统计”的错误理解。显式 Quest Detect 仍是另一条 Quest 级入口。
- Base 现有 evaluator 已按触发输出匹配当前 Task 的任一目标及渠道筛选绝对统计快照。本轮新增 `craftingStatisticsRefreshOtherRequirementsAfterMatchingOutputInSameTask`，验证 craft/smelt/anvil 任一匹配目标事件都允许另一目标的统计快照更新；既有单目标无关输出拒绝测试继续覆盖负例。没有修改生产逻辑、UI、网络载荷或 PARTY 归属。
- CodeGraph 改前 `status` 为 up-to-date；`explore` 检查 `ItemVectorTaskEvaluator`、`TaskCrafting`、`QuestGameplayEventHandler`、观察计划与统计事实入口。改后 `sync` 更新 1 个测试文件（50 nodes）；`impact ItemVectorTaskEvaluator.evaluate` 为 11 个符号，`affected` 命中 `BqCompatibleTaskEvaluatorsTest`。
- 定向 `:galaxy-quest-core:test --tests com.jsirgalaxybase.quest.core.BqCompatibleTaskEvaluatorsTest --offline` 最终 38 项，0 skipped/failure/error。初次尝试 Wrapper 发行包在线下载超时；改用项目缓存内 Gradle 9.3.1 主机离线执行成功。首轮新测试 fixture 漏设 `progressMode=item-vector` 导致宽度异常，修正 fixture 后整类通过。此前 Core/Importer 测试未受生产源码影响；R2.6 当前不同定向覆盖为 82 项（历史 81 项加新回归）。
- 开发导航、能力矩阵与 M.2 行为矩阵已同步修正自动统计语义并记录证据。当前 `BqCompatibleTaskEvaluatorsTest` XML 为 38 项，0 skipped/failure/error；最终 `git diff --check` 通过。尚未确定显式检测的统计范围（认证玩家或本服在线 PARTY）及终端动作粒度（单 Task 或 Quest 级仅统计）；R2.6 与整体 M.2 仍 `BLOCKED`。未访问玩家进度/生产数据库、未执行 DDL/部署/Quest 开关、未停用 BQ，未操作 S1/S2 服务。

### 2026-10-09 - BQ M.2 R2.6 Crafting 统计检测与 PARTY 范围源码审计

- 按固定 BQ 源码复核 `NetQuestAction.detectQuest` → `QuestInstance.detect` → `TaskCrafting.detect`，并用 CodeGraph 检查 Base 的 `AuthenticatedCraftingStatisticsDetectionService.detect`、`QuestGameplayEventHandler.submitCraftingStatistics`、`ItemVectorTaskEvaluator.statisticsDetectionAllowed`、`BqPartyTaskProgressScope.isCraftingEvent`、`BqPartyActiveQuestQuery.isActiveFor` 与 `QuestModule.createPartyActivityQuery`。当前 S2 `DefaultQuests` 只读清点复核摘要 hash `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`、3,744 个任务、47 章、3,847 个放置；133 个 Crafting 中 `allowCraftedFromStatistics=true` 为 6、false 为 127。输出经筛选，不含任务身份、物品内容或玩家进度。
- 确认 BQ 显式 Detect 是 Quest 级、遍历未完成 Tasks；Crafting 统计对 `ALL_UUIDS` 成员逐人更新 PLAYER 进度，但只读取当前服务器在线成员的 `objectCraftStats`，匹配仅按 Item ID。Base 显式按钮/服务仍是认证玩家单 Task；Base 直接 Crafting 事件已用在线 PARTY active-query 过滤，不能沿用旧文档所说“尚无在线资格查询”。Base 现于每次产出事件提交统计观察快照，再由 evaluator 根据触发 Item 与渠道筛选；BQ 只在 Quest Detect 调用链中回填统计。后续若改 Quest 级入口，只应聚合统计型 Crafting Task，不调用会触发 Retrieval/Fluid 消耗的泛化任务检测。
- 更新唯一能力矩阵、开发导航、M.2 行为矩阵与 Docs README，明确修正旧状态并新增 R2.6 决策门。统计范围与入口选择已向用户询问；答案未到前不改玩家交互、网络载荷、归属或自动刷新行为。
- CodeGraph `impact AuthenticatedCraftingStatisticsDetectionService.detect` 仅返回目标方法（调用通过终端动作桥动态接线）；`impact BqPartyActiveQuestQuery.hasActiveMember` 覆盖 8 个符号；`affected` 对 Core/Planner 返回 60 个候选，人工收敛到直接回归。Core `AuthenticatedCraftingStatisticsDetectionServiceTest` 6、`BqCompatibleTaskEvaluatorsTest` 37、`BqPartyActiveQuestQueryTest` 6、`QuestEvaluationPlannerTest` 23，Importer `BqBehaviorInventoryTest` 9，共 81 项，均执行、0 skipped/failure/error。Docker 离线清点首次因容器 `/workspace` Git safe-directory 校验及网络隔离未到达分析任务；随后以主机 JDK 17.0.20.1、项目 Gradle 缓存离线运行成功。`git diff --check` 通过；本轮没有改 Java、数据库或玩法，未执行 assemble/self-contained。
- R.2.6 仍“实施待合同确认”，M.2 与 BQ 替代仍 `BLOCKED`。无生产数据库/玩家进度访问、DDL、部署、Quest 开关或 BQ 停用。

### 2026-10-09 - BQ M.2 R2.5 非消耗 Fluid 快照与 PARTY 兼容

- 先按固定 BQ `TaskFluid.detect` 对照非消耗语义：每个需求独立读取同一份未排出库存；`groupDetect=false` 锁存已完成分量并重算未完成分量，`true` 重算所有未完成分量；Task 完成后锁存。修复 Core Fluid evaluator 将同一流体量跨需求共享扣减的错误，并使非消耗快照的组重算规则与 BQ 一致。
- PARTY 接线复用现有 PostgreSQL inventory session lease：Fluid-only 任务也启用 lease；Item/Fluid 属性合并存放，避免单域刷新覆盖另一域；party 聚合只读取有效期内成员快照，随后由现有 PARTY 事实 fan-out 为每位成员写入独立 PLAYER 进度。限制只有带匹配 BQ 导入标记的非消耗 Fluid 才接受 party aggregate。
- 根 JDBC 回归先失败并定位到 `QuestEngine`：Fluid evaluator 的绝对值被通用进度 `max(old, observed)` 再次改成只增不减。将仅作用于非消耗 `bq_standard:fluid`/对应 factType 的快照分支接入既有组重算逻辑；consuming Fluid 与其他任务事件归约不变。修复后回归证明 fresh party sum `900 → 700 mB`、过期成员排除、成员进度扇出和完成。另一次早期 Core 测试失败是合成 importer fixture 漏填必需 `factType`，补齐测试输入后通过。
- 新增/调整 Core evaluator、QuestEngine、ObservationPlan、Fluid attribute codec 测试与根 `BqPartyInventorySnapshotFactSinkPostgresTest`。CodeGraph 起始索引同步；`impact FluidSnapshotTaskEvaluator.evaluate` 7 个关联符号、`BqPartyInventorySnapshotFactSink.publishPartyAggregate` 4 个、`QuestEngine.isNonConsumingSnapshotTask` 3 个；`affected` 命中相关 Core 与根 PostgreSQL 回归。最终 `sync/status` 显示索引 up-to-date。
- Docker Gradle 完整 `:galaxy-quest-core:test` 257 项、`:galaxy-quest-postgres:test` 52 项、`:galaxy-quest-bq-importer:test` 46 项，根隔离 `:test --tests ...BqPartyInventorySnapshotFactSinkPostgresTest` 9 项，共 364 项全部执行，0 skipped/failure/error。`:assemble :verifyUi2SelfContained` 成功；`git diff --check` 通过。Gradle 发行版最初挂载到空缓存造成慢速下载，停止该次下载后改挂载项目专用缓存，没有清理缓存。
- PostgreSQL 16 测试库 `quest_runtime_test` 运行在 `jgb-r25-quest-it` 临时容器，tmpfs、无宿主卷，仅绑定 `127.0.0.1:55432`，使用合成玩家/任务。验收后容器随停止自动删除，确认容器与端口均不存在。未访问生产 DB/真实玩家进度，未执行生产 DDL/迁移、部署、Quest 开关或停用 BQ。真实 Forge/GTNH 容器、跨服在线资格与时序未验收；R2.5 仅关闭自动兼容回归，M.2 与 BQ 替代继续 `BLOCKED`。

### 2026-10-09 - BQ M.2 R2.4 consuming Fluid 手动提交闭环

- CodeGraph `status` 起始为 up-to-date；对照固定参考 `TaskFluid`、`TileSubmitStation` 与 Base importer `appendFluids`、`ConsumptionRequestAuthorizer.authorize`、`MinecraftPlayerConsumptionPort.planFluidsDetailed`、`ForgeQuestFluidContainerAdapter`、`ConsumptionSubmissionService.process`。改后 `sync` 2 个测试文件（253 nodes）；`impact` 检查流体规划和提交恢复链，`affected` 返回 52 个候选，再人工收敛到容器规划器和根 PARTY/JDBC 集成回归。
- 仅新增自动回归，不改生产逻辑/服务端合同。根隔离 PostgreSQL 用合成 BQ Fluid JSON 验证 400 mB 部分提交、玩家 marker 已应用但 PG 状态仍 `PREPARED` 时 `resume` 恢复且不重复排流；随后不可分割 750 mB 容器针对剩余 600 mB 整份排出但只将 600 记入向量，JDBC 总进度到 1,000 并完成，重复请求不重复消费。既有单测另验证 Fluid Name 精确匹配、无 Tag 与空 Tag 精确区分、选中栈最多一个容器、部分量和空容器放不回时拒绝且源库存不变。保留 BQ 会把不可分割容器实际排出量全计入进度而 Base 按剩余目标封顶的已知差异；无槽时 Base 拒绝与 BQ 输出槽/掉落也不宣称相同。
- Docker Gradle 实际 XML：Core 251、Postgres 52、Importer 46、根 `MinecraftPlayerConsumptionPortTest` 19、根 `BqPartyInventorySnapshotFactSinkPostgresTest` 8，共 376 项，0 skipped/failure/error。`:assemble :verifyUi2SelfContained` 成功；最终 `git diff --check` 通过。首次未限定的 `test --tests` 将类过滤传播给所有子项目并因无匹配而失败，没有构成测试结果；显式使用 `:test` 后跑过。新增集成用例首轮因直接在 JDBC transaction 外调用 authorizer 而失败，按生产事务边界修正后两目标根类全通过。构建有既有 Java 8 source/target 过时警告。
- 测试 PostgreSQL 16 `jgb-r24-quest-it` 使用 tmpfs、无宿主卷、只发布到 `127.0.0.1:5433`，专用网络 `jgb-r24-testnet`；验收后清理容器与网络。未访问生产数据库/玩家进度、执行生产 DDL、部署、启用 Quest 或停用 BQ。R2.4 是合成 adapter/JDBC 自动闭环，不证明真实 GTNH 容器、EntityPlayer 持久化或玩家崩溃恢复；M.2 与 BQ 替代仍 `BLOCKED`。

### 2026-10-09 - BQ M.2 R2.3 手动 Retrieval 部分提交语义

- CodeGraph `status` 确认索引同步；`explore` 与节点/影响查询检查 UI `QuestCenterVisualDocument.detail` → `TerminalQuestCenterDocument.QuestActionsBridge` → `TerminalService` → 服务端捕获热键栏栈 → `ConsumptionRequestAuthorizer` → `ConsumptionSubmissionService` → `MinecraftPlayerConsumptionPort.planItems`。`impact` 覆盖 `planItems` 的消费适配及 `process` 的提交/恢复链；`affected` 将新增回归落到根 `BqPartyInventorySnapshotFactSinkPostgresTest`。
- 固定 BQ 源码 `TaskRetrieval.detect`、`TaskRetrieval.submitItem`、`TileSubmitStation.updateEntity` 对照：Quest Detect 顺序扫描完整玩家库存并消费实际命中量；SubmitStation 每次处理一个至多 64 件的输入栈，10 tick 间隔调用提交。Base 当前终端分别提供“全库存匹配项”与“当前服务器捕获热键栏栈”两条动作。保留现有玩家入口与服务端授权合同，不模拟站台方块或节奏。
- 新增根隔离 PostgreSQL 测试 `importedHeldStackSplitsAcrossOverlappingRetrievalRequirementsAndProjectsActualAmounts`：导入两个重叠 NBT Retrieval 需求（各 4 件），选中 7 件栈经生产匹配器得到 `[4,3]`，未选中栈保持 64 件；下一提交仅扣 1 件得到 `[0,1]` 并完成，重复提交不再扣除。没有改生产逻辑或可见交互合同。
- 实际测试报告：根 Party/JDBC 测试类 7 项、Core `ConsumptionSubmissionServiceTest` 5 项、根 BQ consumption importer / Minecraft port 测试 19 项，共 31 项，均执行、0 skipped/failure/error。`:assemble verifyUi2SelfContained` 成功（39 actionable tasks，1 executed、38 up-to-date）；`git diff --check` 通过。初次重编译仅发现 NBT `copy()` 返回 `NBTBase`，在测试中显式转换为 `NBTTagCompound` 后通过。
- 测试 PostgreSQL 16 `jgb-r23-quest-it` 使用 tmpfs、无宿主卷、只绑定 `127.0.0.1:5433`，验收后容器与 `jgb-r23-testnet` 网络均已移除。未访问生产玩家进度/数据库、执行生产 DDL、部署或更改 Quest/BQ 开关。31 项自动证据不等价于真实 GTNH ItemStack、SubmitStation、多服队伍/恢复或 BQ 整体兼容；M.2 仍 BLOCKED。下一窄批 R2.4 验证唯一 consuming Fluid 手动提交的 importer→授权→适配→JDBC/恢复链。

### 2026-10-09 - BQ M.2 R2.2 导入、库存观察与消费恢复闭环

- CodeGraph `status` 发现根集成测试文件有 1 个待同步变更，`sync` 更新 176 nodes。检查 `BqPartyInventorySnapshotFactSink.accept`（`impact` 9 个符号）、`QuestRuntimeService.apply`（25）、`ConsumptionSubmissionService.process`（12）、`MinecraftPlayerConsumptionPort.planItems`（6）；改后 `affected` 指向根 `BqPartyInventorySnapshotFactSinkPostgresTest`。调用链覆盖 Party 快照、事实投影、提交恢复及消费适配器，不因静态图结果替代人工范围判断。
- 补两条根 PostgreSQL 集成回归。其一将合成 BQ 非消耗 Retrieval JSON 经 importer 发布，再由生产 Party 库存快照 sink、Runtime、planner 和 JDBC 仓储验证当前成员分别累积库存进度并完成。其二将合成 consuming Retrieval 经授权与生产 `MinecraftPlayerConsumptionPort.planItems` 匹配精确 meta/NBT；故意让事实投影暂不可用，验证提交先持久化 `APPLIED`，随后 `resume` 不重扣物品而只完成一次 JDBC 进度，确认后的重复提交仍幂等。测试端口和在线库存为合成替身，不宣称真实 `EntityPlayerMP`/GTNH 行为；本轮无生产行为代码或玩家可见合同变化。
- 使用一次性 `jgb-r22-quest-it` PostgreSQL 16 容器，数据库仅绑定 `127.0.0.1:5433`、数据目录 tmpfs、无宿主数据卷，并显式设置 `QUEST_TEST_POSTGRES_ISOLATED=true`。实际测试 XML：Core 251、Postgres 52、Importer 46、根 Quest 相关测试 25，共 374；全部执行，0 skipped/failure/error。`:assemble verifyUi2SelfContained` 成功，39 actionable tasks（10 executed、29 up-to-date）。编译仅有 Java 8 source/target 过时警告，无构建失败。
- `git diff --check` 通过。验收结束后已移除本轮临时数据库容器与专用网络；未连接生产数据库、读取玩家进度、执行生产 DDL、部署、启用 Quest 或停用 BQ。R2.2 只证明合成数据下的跨层数据库链与 APPLIED 恢复可自动回归，不证明真实 Forge/GTNH 背包、SubmitStation、多服切换、玩家崩溃恢复或奖励物理投递。能力矩阵与导航更新为 R2.2 自动验收完成、M.2/BQ 替代仍 BLOCKED；下一窄批 R2.3 只对照现有合同内的手动 Retrieval 提交语义，若需改变提交入口/扣除/进度/奖励合同须先征求用户决定。

### 2026-10-09 - BQ M.2 R2.1 Retrieval 匹配与库存分配复核

- CodeGraph `status` 确认索引 up-to-date；`explore`、`callers/callees`、`impact` 检查 `InventorySnapshotTaskEvaluator.evaluate` 与 `MinecraftPlayerConsumptionPort.planItems`，`affected` 收敛到 Core evaluator 测试以及根消费/导入适配回归。固定参考源码为 `Reference/BetterQuesting/src/main/java/bq_standard/tasks/TaskRetrieval.java`、`TaskOptionalRetrieval.java` 与 `betterquesting/api/utils/ItemComparison.java`。
- 对照 BQ 按槽位顺序、再按定义需求顺序分配匹配数量的算法；核验 Base 编码保留库存首见顺序，NBT compound 子集比较、OreDictionary 比较方向、damageable 元数据规则，以及列表/数组匹配复用差异。没有发现需要改生产 evaluator 的可复现差异。
- 仅新增 Core 回归 `retrievalKeepsBqSlotOrderWhenOverlappingNbtRequirementsCompeteForStacks`：构造宽/窄重叠 NBT 需求，分别交换物品观察顺序，经 `InventoryObservationAttributeCodec` 验证 `[1,0]` 与 `[1,1]` 的分配结果，固定槽位顺序语义。已有手动消费适配回归仍验证相应匹配计划；BQ SubmitStation 与 Base hotbar 提交入口的产品差异不在本轮更改范围。
- Docker Gradle XML：完整 `:galaxy-quest-core:test` 251 项；Importer 选定 `BqQuestDefinitionImporterTest`、`BqStandardCodecTest` 9 项；根项目选定 `BqConsumingRetrievalImportTest`、`MinecraftPlayerConsumptionPortTest` 19 项；合计 279 项，0 skipped/failure/error。新增测试首轮因 fixture 未声明 `item-vector` 失败，修复测试输入后复跑通过。`:assemble :verifyUi2SelfContained` 成功（29 actionable tasks，6 executed、23 up-to-date），`git diff --check` 通过。构建首轮在插件解析阶段因容器未使用本机代理超时；经只读确认 7892 代理监听且可达后，仅在一次性 host-network 开发容器设置临时 HTTP/Java 代理，重跑成功，未改系统网络或代理服务。
- 本批没有生产代码改写、数据库连接/DDL、玩家进度读取、部署、Quest 开关或 BQ 状态变化。离线算法回归不代表 Forge/GTNH 背包、NBT 注册表、SubmitStation、多服时序已签收；M.2/BQ 替代仍 `BLOCKED`。下一小批 R2.2 先审计“导入定义→事实→Runtime/JDBC”与手动消费调用链的既有覆盖，再只补可复现测试缺口，使用显式隔离临时数据库。

### 2026-10-09 - BQ M.2 R1-04 候选索引与可靠性收口

- CodeGraph `status/sync` 确认索引同步；检查 `QuestEvaluationPlanner.requestsFor`、`JdbcQuestDefinitionRepository.findAllPublished/currentRevision`、`QuestModule.createGameplayEventHandler` 的 callers/callees/impact。改后影响覆盖 Core Planner、Runtime、Postgres 定义仓储、Importer 和周期复评；`affected` 候选较宽，按实际入口选择 Core、Postgres、Importer、Quest 根集成与全量 UI2/Mod 测试。
- 发布定义不再按每个库存事实完整扫描/解码：PostgreSQL revision 行由与发布目录同事务的触发器递增；共享 `RevisionAwarePublishedQuestCatalog` 每次先读取轻量 revision，仅变化时重载不可变目录。Planner 对单个目录快照建立按事实类型、目标 Quest、前置 Quest 和锁定进度 ID 的候选索引。成员、完成前置、活动 PARTY 资格和 repeat 周期仍实时求值；数据库 revision 读取失败时不使用旧目录继续规划。
- 回归覆盖 2,501 个定义中仅选择 1 个匹配候选、目录每 revision 只加载一次、revision 更新与故障、前置索引刷新、repeat 冷却后重新检测，以及 PostgreSQL publish/retire 与迁移升级。新增 `ops/sql/migrations/20261009_001_add_quest_definition_catalog_revision.sql`；只在隔离测试库执行迁移升级，没有生产 DDL。
- CodeGraph 改后 `impact QuestEvaluationPlanner`、`QuestEvaluationCandidateIndex`、`JdbcQuestDefinitionRepository` 与事件入口；`affected` 选测范围包括 98 个跨项目候选，人工收敛至 Quest Core/Postgres/Importer/根 PARTY JDBC 测试及全量测试，不以 CodeGraph 自动选择结果替代调用链判断。
- 隔离 PostgreSQL 模块测试：Core 250、Postgres 52、Importer 46，共 348 项，全部运行且 0 failure/error/skipped；根 PARTY JDBC 4 项也在新建的无卷临时 PostgreSQL 上通过。全量 XML 共 991 项、935 执行、56 assumption-skip、0 failure/error：48 项缺其它业务 PostgreSQL 测试配置，4 项 Quest PARTY JDBC 用例因全量运行未设隔离库（单独实跑通过），4 项 Warehouse JDBC 用例缺 `JGB_BANKING_IT_*` 配置。首次全量任务因 Docker 开发镜像未挂载 CJK 预览字体失败；只读挂载宿主字体并设置 `UI2_PREVIEW_FONT` 后 `test assemble verifyUi2SelfContained` 成功，字体未复制入仓库或 JAR。
- R.1 的 R1-01～R1-05 仅此可靠性子批次自动验收完成。新增 revision migration 尚未应用生产；生产迁移、Quest 开关、停用 BQ、玩家进度访问、部署均未做。M.2 的 BQ 行为等价、奖励崩溃策略、玩家迁移、实时影子和正式切换仍为 BLOCKED。下个窄工作包按导航进入 R.2.1：固定 BQ 源码对照高频背包匹配与库存分配，先锁定可复现差异，不改玩家可见合同。

### 2026-10-09 - BQ M.2 可靠性 R1-01～R1-03

- CodeGraph `status/sync` 更新 4 个 Java 文件；检查 `impact QuestGameplayEventHandler.onServerTick`、`QuestGameplayEventHandler.submitInventorySession`、`OrderedQuestFactRetryQueue.retry`，调用链覆盖周期 tick、登录会话首快照和 FIFO 重试。`affected` 返回 47 个跨项目候选，范围过宽；据直接调用/归属手动执行 Core、Postgres、Importer 和根 Quest 定向测试。
- R1-01：Postgres、BQ Importer、根 PARTY 快照集成测试都需要显式隔离标记及 `_test`/`_it` 数据库名；移除 `galaxy-base`、`POSTGRES_DB` 默认回退。未设置 URL 时按条件跳过；显式配置的隔离库不可达或凭据错误则失败。三个 PostgreSQL 集成范围本轮在无数据卷临时容器 `jgb-quest-r1-it` / `quest_runtime_test` 上运行，未连接 `galaxy-base`。
- R1-02：玩家锁定进度复评的 `RuntimeException` 在事件处理边界记录并吞止，不逃出服务器 tick；下一个既有周期仍可再次尝试。注入首轮故障、次轮恢复的测试通过；未添加后台线程、持久重试或更改资格/奖励逻辑。
- R1-03：普通 GameplayFact 和登录 `beginInventorySession` 均进入有序队列，原始不可变事实及幂等键、投递操作类型不变。队列限 1,024 条且估算持有量 8 MiB，每 20 个 END tick 最多重试 32 条；满限拒绝最新并记录，永久失败阻塞队首。关服日志记录未提交/溢出后清空；进程崩溃不可恢复，队列非 WAL/outbox，崩溃或拒绝事实仍可能丢失。已测 FIFO、操作保留、幂等键、重试预算、条数/估算字节上限与关服清理。
- Docker Gradle：`:galaxy-quest-core:test` 244、`:galaxy-quest-postgres:test` 49、`:galaxy-quest-bq-importer:test` 46、根 `:test` 指定 4 个 Quest 测试类 12 项，合计 351 项，0 failure/error/skipped；Postgres 49 与 Importer 46 以及 PARTY JDBC 测试实际使用隔离临时 DB。`:assemble`、`:verifyUi2SelfContained`、`git diff --check` 通过。临时容器在验收后停止并自动移除；未挂载或读取生产进度，未执行生产 DDL、部署、启用 Quest 或停用 BQ。
- 过程异常如实记录：首轮根测试启动遇到既有 `build/test-results` 由 root 所有导致 UID 1000 无法访问，修正的仅是该生成目录权限；首轮新增重试测试把故障注入在初次入队而非重试阶段，调整 fixture 后全套重跑通过。最终测试报告 0 跳过，不把 Gradle 任务级 `SKIPPED` 误计作测试。
- 导航与权威矩阵已记录 R1-01～03 自动验收完成，R1-04 未开始、R1-05 仅本子批次验收完成；M.2 与 BetterQuesting 替代仍 `BLOCKED`。没有将本批可靠性修复表述为行为完全等价或替代完成。

### 2026-10-09 - 代码职责与文档开发入口整理

- CodeGraph status/sync、QuestModule/ConsumptionSubmissionService/TerminalQuestCenterSnapshotMapper 符号查询核对现有入口；按 Core、JDBC、Minecraft Adapter、UI2、Importer 和参考源码建立代码导航。没有为整理而搬移文件、删除 evaluator 或重写业务。
- 新增统一开发导航，明确需求/状态/实施/行为证据文档分工，固定 R1-01 至 R1-05 工作包、验收条件、后续背包主链边界与变更证据要求。README 和能力矩阵指向导航。
- 修正路线“没有可执行迁移批次”和交接“应先完成 M.1/待本轮提交”的冲突口径，保留历史背景并说明当前 M.2。未把 311 项环境测试冒充全量行为验收，未重跑测试、部署或访问数据库。仅文档改动，检查本地链接及 diff 空白。

### 2026-10-09 - BQ 替代需求与路线复盘

- 核对 9 月 8 日平台设计、能力矩阵、9 月 17 日路线/交接及当前 BQ/Base 源码。纠正上一轮“应保留 BQ 运行时只换数据库”的泛化建议：既定需求仍是 Base 自包含、PostgreSQL 真源、终端替代任务书、吸收成熟行为，暂停新玩法。方向不推翻，实施顺序收紧。
- CodeGraph status/sync 更新一个文件，通过 explore 复核当前求值/领取结构；本地参考 TaskRetrieval.detect 与 QuestInstance.claimReward 明确库存规则可剪裁，而单例进度和先奖励后 claimed 不能直接沿用。下一顺序：隔离验证与可靠性 → 背包到领奖 → 实际 Task/Reward → 原 M.3–M.6。
- 当前 HEAD 6e80f68，已存在累计兼容提交，不能沿用旧“待统一提交”。相关五项风险路径本轮仍可定位，但未进行故障或性能复现。新增需求复盘文档并链接权威矩阵；无业务修改、测试重跑、数据库或部署操作。diff 空白检查单独执行。

### 2026-10-06 - 使用本地 7892 代理完成 Docker 构建验证

- 实测 HTTP 代理 `127.0.0.1:7892` 正常访问 GitHub raw 与 Docker Registry（Registry 401 为正常认证响应）。仅在临时开发容器配置代理，不改 daemon、系统网络、远控或生产 Compose。
- 使用恢复的 galaxy-dev:local，`--network host` 使容器可访问本机代理；UID/GID 1000、cap-drop ALL、no-new-privileges，仅挂载项目和开发 Gradle 缓存，未传数据库凭据。HTTP_PROXY/HTTPS_PROXY 指向本地 7892；Java 必须另配 `JAVA_TOOL_OPTIONS` 的 http/https.proxyHost=127.0.0.1、proxyPort=7892（仅环境变量不足以覆盖插件 Java 请求）。
- 补齐 LWJGL 等依赖后 Minecraft 编译与打包完成。首次命令因 prepareUi2QzLicense/verifyUi2SelfContained 不兼容配置缓存，在任务执行后整体失败；按既有构建脚本关闭配置缓存重跑成功，没有修改插件或业务代码。
- Docker Gradle `--no-daemon --no-configuration-cache -PforceToolchainVersion=17`：Quest Core、UI2 Core、UI2 Terminal 三个 test 显式 --rerun，311 项、0 failure/error/skipped；`:assemble :verifyUi2SelfContained` 成功。此为开发恢复验证，不是全量业务/数据库验收。
- 运行 JAR：`build/libs/jsirgalaxybase-d27d2ee-main+d27d2ee371-dirty.jar`，SHA-256 `2aa193068ff147bffd9ee205649491f6836647c8f12c97a075c6a10de978e5e9`。未部署、未启动游戏或数据库；临时容器清理后无运行容器，runawesun 仍 active。diff 空白检查通过。

### 2026-10-06 - 离线镜像备份恢复与开发容器核对

- 更正上一轮漏查镜像归档：`docker/galaxy-images.tar`（2026-04-26，1,232,844,800 字节）SHA-256 为 `54b25cfe0f6f3a6e32f2830a0c18a6417fa75687bda4cd99d2708612f1394bb7`，与随包校验文件一致。离线导入 galaxy-base、galaxy-gtnh、galaxy-dev 三张 local 镜像成功；导入不启动服务。
- 开发镜像 ID 前缀 `2e7eb57702c0`，实测 Temurin 25.0.2、javac 25.0.2、Git 2.43.0，curl/psql/unzip 存在。版本检查使用只读根文件系统、无网络、无业务挂载的临时容器。
- 再以 UID/GID 1000 和迁移 Gradle 缓存运行任务内核测试，仅挂载代码/开发缓存，未传数据库环境变量，网络禁用。构建插件即使收到 `--offline` 仍尝试访问 raw.githubusercontent.com，初始化 UnknownHostException，未进入测试；不计为 Docker 测试通过。镜像恢复已完成，但严格断网构建仍受插件外部访问制约；上一轮本机 311 项通过证据不变。临时容器自动删除，未启动 GTNH/数据库，结束时无运行容器。

### 2026-10-06 - 新主机开发环境恢复

- 当前项目位于 `/mnt/m600/zhitai-data/gtnh/JsirGalaxyBase`；保留迁移来的工作树，没有提交、推送或部署。恢复 Git 2.53.0、OpenJDK 17、Node 22、CodeGraph 1.6.2、Docker 29.1.3、Compose 2.40.3 和 Buildx。u26 加入 docker 组，新登录后生效；Docker 已启动但不设开机自启，没有启动 GTNH 或数据库容器，远控 runawesun 仍 active。
- CodeGraph status/sync 和符号查询可运行；迁移索引仍报告大量未解析引用，不能把同步成功当作所有调用边完整。构建脚本数据根目录改为按项目位置推导；移除 Gradle 绑定旧主机 Java 25 绝对路径。脚本 dry-run 指向新目录，bash 语法及 diff 空白检查通过。
- 复用迁移的 `docker/docker-data/galaxy-dev/gradle-home`，本机实际执行 Quest Core 244、UI2 Core 41、UI2 Terminal 26 项测试，合计 311，零失败/错误/跳过。没有执行可能连接生产数据库的集成测试。
- 完整离线 assemble/self-contained 因缺少 LWJGL 2.9.4 缓存未通过；联网补依赖停留配置解析阶段，本轮有界终止该次 Wrapper 及其专属 Daemon，不遗留等待进程，不能作为通过证据。Docker 开发镜像构建在 Docker Hub metadata 直连阶段超时；CLI/守护进程恢复不等于 galaxy-dev 镜像就绪。不修改远控、代理配置或生产数据，不升级项目构建插件。

### 2026-10-02 - BQ 进度与代码质量收口复核

- 按用户要求停止扩展辅助入口，本轮仅源码审查、验证尝试与文档收口。当前处于 M.2，非 BQ 已替代；背包 retrieval/optional_retrieval 占已有真实清点 Task 出现次数约 91.3%。观察站差异延后，下面观察站记录中的“下一步”已被本轮优先级替代。
- 确认已有检测、消费、进度和奖励实现；发现五项优先风险：锁定进度复评异常逃出 tick、背包轮询同步查询并解码全发布目录、一次性事件失败仅日志且丢失、集成测试默认数据库连接隔离不足、满背包掉落与回执保存间的崩溃重放窗口。均为源码依据，不冒充实机事故复现。
- CodeGraph status/sync 已更新，tick 入口 impact 返回 34 符号；相关 affected 未识别测试，不能由此认定无需测试。首次 Gradle 过滤器传播到子模块导致无匹配测试，纠正后插件初始化 SocketTimeout，重试仍未进入测试；本轮不报全量通过或新测试数量。连接环境变量显式清除，未访问生产数据库。
- 新增独立质量报告，同步权威能力矩阵、M.2 范围和索引，区分 M.1 定义迁移与 M.3 玩家迁移。没有修改业务代码、提交、部署、生产 DDL、Quest 开关或服务状态。下一轮先恢复可信隔离验证，再修复背包到领奖主链；文档整理后执行 diff 空白检查。

### 2026-10-02 - Retrieval 邻接库存检测差异核对

- 用户确认可直接读取 S2 服务端 quest 定义；本轮分析仍限定在 `config/betterquesting/DefaultQuests`。对照本地 BQ `TileObservationStation.updateEntity` 与 `TaskRetrieval.retrieveItems`：观察站每 20 tick 采集绑定方块六个相邻位置的库存/流体容器内容，并对该玩家的共享 Quest 调用相应任务检测；当前 Base 检索事实来源是认证玩家库存快照，没有邻接容器观察入口。这是 BQ 的已确认能力差异，但任务定义不包含观察站是否实际放置/使用的信息，不能据此推断 S2 玩家依赖该机制。
- 尝试以 `:galaxy-quest-bq-importer:behaviorInventory` 只读刷新当前定义聚合，S2 `DefaultQuests` 被单独挂载为只读；Gradle 在开发容器中长时间无输出、进程处于等待，约 3 分钟后停止了本次分析任务。未得到新的清点结果；不把先前快照冒充本次刷新。未改服务端目录，也未访问玩家进度、其他配置或数据库。
- 已把该差异及证据边界补入 M.2 权威矩阵。Retrieval / optional_retrieval 与整体 M.2 继续 `BLOCKED`；下一步需先决定 Base 的等价交互是终端触发邻接库存扫描，还是保留/提供观察站式世界入口，再为扫描范围、任务资格和 PARTY 快照定义服务端合同。无代码、数据库、部署或 Quest 开关改动。

### 2026-10-02 - Fluid 需求量画像与不可分割容器进度差异

- 在用户授权范围内，将 S2 `config/betterquesting/DefaultQuests` 单独只读挂载到开发容器，重新运行匿名 `behaviorInventory`：快照 hash `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，47 章、3,847 个放置；15 个 fluid Task 共 16 项需求，量值分布 144 mB×8、576 mB×1、1,000 mB×6、5,000 mB×1。分析只输出聚合量，不输出任务 ID/名称、流体名或 NBT；未读取玩家进度、其它配置或数据库。
- Importer 新增匿名 fluid amount histogram 及私有流体名/Tag/任务名不泄漏的合成回归。CodeGraph 同步 2 个 Java 文件；`impact BqBehaviorInventory.from` 80 个符号，`affected` 命中 1 个测试类。Docker Gradle 定向 `BqBehaviorInventoryTest` 9 项通过；Importer 全套 45 项，5 项因未配置隔离 PostgreSQL 按条件跳过，其余 40 项通过，0 失败/错误。首次测试因构建插件仓库 SocketTimeout 未进入测试，重试成功。Wrapper 长等待原因已查明是误用未挂载的 Gradle 缓存目录，使用开发容器 `/gradle-home` 后匿名扫描数秒完成。
- 对照 BQ `TaskFluid.Detector.run(ItemStack, ...)` 与 Base `planFluidsDetailed`：不可分割容器均整份消耗；BQ 进度计整份实际排出量，Base 进度最多记到目标剩余值，因此消费和完成结果相同、完成后的数值进度可能不同。保持通用消费额度上限不变，将超额显示记为有界差异；Fluid 与 M.2 仍 `BLOCKED`。无生产 DDL、部署、Quest 开关或服务操作。

### 2026-09-30 - 修复 Quest 详情任务动作无命中区域

- 为 checkbox 确认入口增加 UI2 视觉与输入回归，覆盖 `350×193`、`427×240`、`620×340`：自动测试发现确认按钮虽然进入组件树，但 `detailLayout` 没为它分配几何，实际 bounds 为 `0×0`，所以玩家点击无法提交。现在 task 行为确认、Crafting 检测和 Retrieval 提交/检测控件显式分配布局区域；测试验证可用 checkbox 点击只发送 quest/task 意图，锁定或不可确认任务不显示入口。
- CodeGraph 改前状态为 up to date；改后同步 UI Document 和视觉测试 2 个 Java 文件（205 nodes），`impact detailLayout` 检查布局影响链，`affected` 收敛至 `QuestParticipantManagementVisualTest`。Docker Gradle `:ui2-terminal:test` 通过，26 项、0 failure/error；`git diff --check` 通过。首次回归运行暴露真实 0×0 命中区域问题，修复后复跑成功。
- 此修复只证明 UI 命中区和动作意图路由，不证明 BQ checkbox 的 party/global 行为、Forge 运行时或奖励等价；Checkbox 与整体 M.2 仍 `BLOCKED`。只读取获授权的 GTNH `DefaultQuests` 定义范围；未读玩家进度、配置或数据库，未执行 DDL、部署、服务器操作或 Quest 开关。

### 2026-09-30 - 修正 BQ 非全局 Quest 的 globalShare 误阻断

- 对照 BQ `QuestInstance.hasClaimed`：`globalShare` 仅在 `isGlobal=true && globalShare=false` 时改为全局单次领取；当前 S2 清单的 3,744 个 Quest 全部 `isGlobal=false`，所以 `globalShare=true` 不应影响普通个人统计检测。Base 此前在 `AuthenticatedCraftingStatisticsDetectionService` 和终端按钮映射中单独拒绝 `globalShare=true`，会使标记为分享但实际非全局的个人 Quest 无法使用统计检测。
- 将拒绝边界收窄为真正的 global Quest 与非 PLAYER 主体；新增回归证明 `global=false/globalShare=true` 可产生个人检测事实，而 `global=true` 仍阻断，并验证 UI 按钮与服务端一致。没有改变任何队伍 fan-out 或奖励资格规则。
- CodeGraph 改前 `impact AuthenticatedCraftingStatisticsDetectionService.detect` 24 个符号、`affected` 命中 `AuthenticatedCraftingStatisticsDetectionServiceTest`；Mapper impact 28 个符号、`affected` 命中 Mapper 与领取测试。Docker Gradle Core 定向测试 5 项、根 Mapper 测试 3 项均通过，0 跳过/失败/错误；`git diff --check` 通过。M.2 仍 `BLOCKED`：全局 Quest 与跨服实机路径未验证；未读玩家进度/数据库/服务配置，未执行 DDL、部署或 Quest 开关。

### 2026-09-30 - M.2 BQ 全局任务属性与 Checkbox 任务关系复核

- 为避免把 Quest 属性 `globalShare` 错当成 PARTY 进度同步，在纯 Java `BqBehaviorInventory` 增加不含名称/UUID 的聚合 `QUEST_PROFILE`：按 `isGlobal/globalShare/autoClaim/lockedProgress/simultaneous/repeatable` 分组统计 Quest 数及其 Task/Reward 类型。测试验证分组稳定、内容只含聚合标识与计数。
- 经用户明确允许，S2 `DefaultQuests` 仍以只读挂载重新扫描；摘要 SHA-256 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc` 未变。3,744 个定义均 `isGlobal=false`；2,494 个 `globalShare=true`，但 BQ 源码只在 `isGlobal && !globalShare` 时改变 `hasClaimed` 领取判定。203 个 Checkbox 中 76 个处于 `globalShare=true` 任务，也都不是 global Quest。因此该属性不解释 Checkbox 的队伍确认同步；源码独立配置 `fullySyncQuests` 默认 false、注释标记 `[NYI]`，本轮未读取服务端配置，不能推断当前覆盖值。
- CodeGraph 改前 `impact BqBehaviorInventory.from` 57 个符号，`affected` 选中 `BqBehaviorInventoryTest`；改后同步 2 个 Java 文件、98 nodes，索引 up to date。Docker Gradle `:galaxy-quest-bq-importer:test --tests ...BqBehaviorInventoryTest` 通过，测试类 7 项、0 跳过/失败/错误。`behaviorInventory` 仅将 `DefaultQuests` 目录只读挂载，输出过滤为摘要、类型组合与数量，不显示 Quest 名称/UUID；`git diff --check` 通过。
- 当前类型仍保持 `BLOCKED`：清单证明的是内容属性，不是运行时配置或多服确认/奖励行为。未读取玩家进度、服务端配置、数据库；未执行生产 DDL、部署或 Quest 开关。

### 2026-09-30 - M.2 Meeting 的 BQ Party 完成扇出

- 按用户最新确认，只读重扫 S2 `DefaultQuests` 定义目录；摘要 SHA-256 仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，3,744 个任务、47 个章节、3,847 个放置，`bq_standard:meeting` 仍为 1 项。只向终端输出类型频数与汇总值，没有输出 Quest 名称、UUID 或玩家数据。源码 `TaskMeeting.tickTask` 每 60 tick 检测存活玩家周围的扩展 AABB；目标匹配使用 EntityList 注册类及子类、数量与可选 NBT，完成时 `pInfo.ALL_UUIDS` 均置为完成。Base 的发布定义观察计划、每 60 tick 的注册 Handler 与单人 evaluator 已存在，调用链复核发现此前 Party assignment policy 漏掉 meeting，进度只写触发玩家。
- Base 现仅对带 `bq.raw` 且使用 `galaxy:nearby_entities_observed` 的导入 meeting 事实执行可信 PARTY→每位成员独立 PLAYER 进度扇出；有前置条件时要求至少一名成员已解锁，锁定成员可暂存完成状态，权益仍由自己的解锁状态决定。普通自建 meeting 不因此扩大到 Party。
- CodeGraph 改前 impact：`BqPartyTaskProgressScope.supports` 7 个符号、`requiresUnlockedPartyMember` 28 个、`BqHuntPartyAssignmentPolicy.select` 22 个、`QuestEvaluationPlanner.requestsFor` 41 个。改后同步 3 个 Java 文件 129 nodes，`affected` 命中 `QuestEvaluationPlannerTest` 与 `QuestRuntimeServiceTest`；随后同步 PostgreSQL 测试文件并由 `affected` 命中 `QuestPostgresIntegrationTest`。Quest Core 全套 226 项通过，0 跳过/失败/错误；隔离 PostgreSQL 16 上定向集成测试 1 项通过，0 跳过/失败/错误。测试容器和网络随后删除，没有宿主端口映射或生产 DB 挂载。最终 `git diff --check` 通过。
- Meeting 与整体 M.2 继续 `BLOCKED`：自动测试不证明 Forge `EntityList`/NBT/60 tick、WorldSave 重启或跨服租约路径；单次快照采用所有 meeting 的最大 range 时，多个不同 range 的任务还需实体级距离过滤。未操作服务端进程、生产数据库、DDL 或 Quest 开关，也未部署。

### 2026-09-30 - M.2 物品奖励满包掉落语义与重放边界

- 对照 BQ `RewardItem.claimReward0`、`BigItemStack.getCombinedStacks()` 与 `QuestInstance.claimReward`：BQ 将每个最大栈先交给 `InventoryPlayer.addItemStackToInventory`，不完全插入时把剩余栈交给 `EntityPlayer.dropPlayerItemWithRandomChoice(stack,false)`，随后才记录 Quest claimed。Base 先保留主背包可接受的数量并持久化进度，再调用相同 Forge toss API 掉落剩余栈，最后持久化 entitlement marker；完成 marker 后重放不会重复掉落。
- 对 overflow 路径增加明确恢复阶段：玩家存档先记录背包插入数量；所有溢出栈掉落后写完成 marker。最终 marker flush 失败时，改为持久化每栈完整已发数量，让租约重试只补完成状态而不再发奖。若实体已掉落而玩家数据/世界/数据库在此窗口同时崩溃，实体与玩家 NBT/租约并非原子，仍有重复掉落风险；保持 `BLOCKED`，不声称 exactly-once。
- 另对照 Minecraft 1.7.10 `InventoryPlayer.storeItemStack` 与 `ItemStack.isStackable`：物品未声明 subtypes 时 damage 不参与合并判定；但已损坏的耐久物品即使 Mod 把最大堆叠数设为大于 1，也不会并入已有栈。Base 插入计划分别对齐这两条规则并新增回归用例。CodeGraph 后续 `impact MinecraftItemRewardDeliveryHandler.insert` 覆盖 6 个符号；同步 2 个 Java 文件共 167 节点后，`affected` 仍只命中 `MinecraftRewardDeliveryTest`。Docker Gradle 根 `:test --tests ...MinecraftRewardDeliveryTest` 最终 25 项通过、0 跳过/失败/错误；一次首轮失败是旧用例仍断言 deferred，已按 BQ 行为改为断言溢出掉落与幂等重放。`git diff --check` 通过。
- `MinecraftRewardDeliveryTest` 的 fake `PlayerState` 不等于真实 Forge `ForgeHooks`、WorldSave 写盘/重启或 Lobby/S2 租约运行；item reward 与 M.2 仍 `BLOCKED`。未读取玩家进度、数据库或服务配置，未执行生产 DDL、部署、Quest 开关或服务状态操作。

### 2026-09-30 - M.2 物品奖励非正数量兼容边界

- 经 CodeGraph `status` 确认索引最新；`explore` 对照 Base `BqStandardCodec.items/rewardParameters` 与 `MinecraftItemRewardDeliveryHandler.rewardStacksForPlayerName`，`impact` 指向生产 handler 和 `MinecraftRewardDeliveryTest`。只读查阅已安装的 BetterQuesting 3.7.15-GTNH JAR 字节码及仓库参考源 `BigItemStack.getCombinedStacks()`：BQ 使用 `Math.max(1, stackSize)`，故 `Count <= 0` 也会构造一个物品栈；Base importer 保留原 Count，但 handler 原先跳过非正数量。
- Base 物品奖励现在将每个条目数量下限规范为 1，并增加生产解析路径回归用例覆盖零数和负数。S2 `DefaultQuests` 最新只读汇总的 6,271 个物品奖励栈 Count 均为正，此兼容修正不改变当前任务结果。
- item reward 与整体 M.2 保持 `BLOCKED`：只验证构造与适配算法，未证明真实 EntityPlayer/WorldSave 重启、满包恢复、多服租约及向世界掉落策略；也没有因为实现边界而把兼容状态上调。未读取玩家进度/其它配置或数据库，未执行 DDL、部署、服务器操作或 Quest 开关。
- 改后 CodeGraph `sync` 更新 2 个 Java 文件；`impact MinecraftItemRewardDeliveryHandler.rewardStacksForPlayerName` 涉及 5 个符号，`affected` 只命中 `MinecraftRewardDeliveryTest`。Docker Gradle 根 `:test --tests ...MinecraftRewardDeliveryTest` 21 项通过、0 跳过/失败/错误；`git diff --check` 通过。初次误用跨子项目的聚合 `test` 任务导致各模块无匹配测试过滤失败，改为限定根 `:test` 后通过。

### 2026-09-30 - M.2 Retrieval 背包检测与单栈提交映射复核

- 按用户授权重新只读刷新 S2 `DefaultQuests`，摘要哈希仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`；本次继续对照最高频 Retrieval / Optional Retrieval 的源码路径，没有读取玩家进度、`questbook.cfg`、数据库或其它服务配置。
- 对照 BQ `TaskRetrieval.detect` 与 `submitItem`，并经 CodeGraph 检查 Base `TerminalService.createCareerPageSnapshot` / `buildAuthenticatedQuestCenterSnapshot`：终端“提交并消耗背包匹配项”对应整库存检测；“提交并消耗手持栈”由服务端从认证玩家捕获 hotbar slot 与 stack fingerprint，消费计划限于该槽，对应单栈提交。UI 交互差异仅在 BQ 可把物品放进 SubmitStation 输入槽、Base 当前入口使用 hotbar 栈。原兼容矩阵中“动作语义尚未明确映射”已修正。再对照 BQ `QBConfig` 确认 `fullySyncQuests` 默认 false、注释为 `[NYI]`；false 分支仅给当前成员写消费进度，true 分支把同一次消费进度写给整队。Base 尚未支持 true 分支，S2 实际 `questbook.cfg` 未读取。
- 已有 `MinecraftPlayerConsumptionPortTest` 覆盖全背包计划、单栈 64 件上限和捕获槽变化后拒绝；本轮在 Docker Gradle 环境重跑该类，16 项、0 跳过/失败/错误。没有提升 Retrieval 行为状态：`fullySyncQuests=true` 分支、多服/断线恢复、GTNH 真实 Item/NBT 与运行时路径仍未验证，类型和整体 M.2 保持 `BLOCKED`。只更新文档并运行定向测试，未改服务端文件或运行状态，未执行 DDL、部署或 Quest 开关。

### 2026-09-30 - M.2 Choice 领取前改选语义与事务锁定

- 对照本地 BQ `RewardChoice.setSelection(UUID,int)` 和选项 UI 请求路径，确认玩家可在领取前覆盖自己的 Choice 索引；原 Base 代码只允许首次选择，与 BQ 不符。
- Base 终端现在对 `CLAIMABLE` Choice 保持选项可点击；PostgreSQL 仓储先锁定 entitlement 行，再验证身份、类型和索引。领取前改选返回 `RESELECTED`；领取/投递开始后拒绝更换。自动领取 `PENDING`、重试 `FAILED` 且缺少历史选择时，仅服务端 force-claim 路径允许首次补选；普通玩家终端入口无法借此抢先选择。选项状态以服务端当前玩家、权益行和版本/周期为准，客户端仍只发送 quest/reward/index 意图。
- 测试首轮发现自动领取 entitlement 初始可为 `PENDING`，因此不能把“首次写入”同“领取前改选”套用同一个状态门槛；最终实现拆分普通玩家 `select` 与内部 `selectForForcedClaim`：普通玩家只能在 CLAIMABLE 新选/改选，PENDING/FAILED 的空选择仅 force-claim 可补写，已有选择不允许覆盖。新增 UI 实际命中测试与 JDBC 集成覆盖：CLAIMABLE 首选/改选、领取后拒改、最终投递内容与最新选择一致、自动队列不能被客户端抢先选定、force-claim 随机补选仍工作。
- CodeGraph 改前检查 Choice 仓储与服务/UI 调用；改后 `sync` 7 个文件，`impact JdbcRewardChoiceSelectionRepository` 16 个符号、`impact QuestCenterVisualDocument` 76、`impact choiceMessage` 3，`affected` 收敛到 Choice 的 PostgreSQL 集成测试和 UI2 `QuestParticipantManagementVisualTest`。
- Docker Gradle `:ui2-terminal:test` 25 项、`:galaxy-quest-core:test` 224 项、根 `compileJava compileTestJava` 与 `:galaxy-quest-postgres:compileTestJava` 通过。完整 `:galaxy-quest-postgres:test` 45 项通过（含 PostgreSQL 集成类 34 项，均 0 跳过/失败/错误）；PostgreSQL 只在无宿主端口映射、tmpfs、独立 Docker 网络的一次性 16 容器运行，任务结束后容器/网络已清理。根定向 `:test` 两次均在 GTNH convention plugin 初始化阶段 `SocketTimeoutException`，未进入测试；根主码与测试码编译成功。`git diff --check` 通过。
- Choice 行为的自动兼容切片完成，但真实 Minecraft 物品交付、玩家存档重启、队伍接收者与多人游戏仍未证明；Choice 与整个 M.2 继续 `BLOCKED`。未读取玩家进度、未连接生产数据库、未执行 migration/DDL、部署、Quest 开关或服务器操作。

### 2026-09-30 - M.2 XP 奖励计算与幂等交付自动覆盖

- 按用户再次授权，只读读取 S2 `config/betterquesting/DefaultQuests` 定义目录；聚合行确认当前 2 项 `bq_standard:xp` 奖励均为 `isLevels=true`，与源码 `RewardXP.claimReward0 → XPHelper.getLevelXP(amount) → addXP` 的等级奖励路径对应。未读取玩家进度、其它配置、物品数据或数据库。
- `MinecraftXpRewardDeliveryHandler` 的 XP 状态访问抽为包内端口，生产 Minecraft 适配和算法不变；新增自动用例验证 2 级奖励换算、持久标记/XP 写盘先于同步包、重复交付不二次加经验，以及写盘异常时 XP 与标记回滚。现有边界用例覆盖 BQ 1.7.10 XP 曲线分段。
- CodeGraph 变更前 `impact MinecraftXpRewardDeliveryHandler` 26 个符号，调用方仅 `QuestModule`；`callees deliver` 覆盖在线解析、XP 曲线、Experience 更新和持久化。改后同步 2 个源文件、impact 45 个符号，`affected` 命中 `MinecraftRewardDeliveryTest`。根 `:test` 对 `MinecraftRewardDeliveryTest` 与 `MinecraftPlayerConsumptionPortTest` 共 36 项通过，0 跳过/失败/错误；首次插件网络超时及未限定根项目的过滤命令失败后，修正为 `:test` 并重试成功。M.2 XP 与整体替代仍为 `BLOCKED`：假端口测试不等同真实 `EntityPlayerMP` 存档、重启及跨服租约验证。未执行生产 DDL、部署、Quest 开关或服务器操作。

### 2026-09-30 - M.2 Crafting PARTY 事件进度扇出

- 按用户授权，将 S2 `config/betterquesting/DefaultQuests` 单独以只读卷挂载到 `galaxy-dev`；清点输出经过过滤，只保留 crafting 聚合行、章节和放置数，不读取玩家进度、其他配置或数据库。来源摘要仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`；133 个 crafting 全部允许 craft/smelt、均禁用 anvil，其中 6 个允许统计检测。
- 对照本地 BQ `TaskCrafting` 和事件处理器，普通事件写入 `ALL_UUIDS` 每人的独立进度；活动 Quest 则是在线成员 active quest 的并集。Base 新增基于服务端可信 PARTY 的导入 crafting 进度扇出，并要求至少一名队员满足 Quest 前置；锁定队员只累积任务进度，解锁前不发权益。统计检测仍是个人、按目标任务触发。
- CodeGraph 同步后，`impact BqHuntPartyAssignmentPolicy.select` 16 个符号、`QuestEvaluationPlanner.requestsFor` 34 个、`QuestRuntimeService.isBqPartyProgressFanout` 4 个、`QuestEngine.evaluate` 26 个；`affected` 指向 Planner 与 Runtime 两个 Core 测试文件。Docker Gradle 首次在线启动因 GTNH 构建插件 `SocketTimeoutException` 失败，使用已缓存依赖离线重跑 `:galaxy-quest-core:test` 成功：218 项、0 跳过/失败/错误；`git diff --check` 通过。
- BQ 的在线 active quest 并集、`canSubmit` 和重复/领取状态还没有 Base 等价快照；PARTY 运行时、真实 GTNH 合成事件及存档恢复未验收。因此 crafting 133 项与整个 M.2 继续 `BLOCKED`。未读取 `questbook.cfg`、玩家进度或数据库，未执行 DDL、部署、服务器操作或 Quest 开关变更。

### 2026-09-30 - M.2 Checkbox 锁定确认幂等边界

- 对照本地 BQ `PanelTaskCheckbox`、`NetTaskCheckbox`、`ParticipantInfo` 与 `QBConfig`：网络端只校验 Quest/Task
  实体和类型；同步开关默认 false、注释标记 NYI；开关开启时写队伍 `ALL_UUIDS`。S2 `questbook.cfg` 本轮未读取，
  不推断实际覆盖值。样本 203 个 Checkbox 均非重复，76 个所属 Quest 带 `globalShare`；该字段不能代替全局同步配置。
- 修复锁定 Checkbox 的幂等键消耗问题：`QuestRuntimeService.applyIf` 在事实落库之前、同一事务和同一请求快照中
  执行服务端资格门控；`AuthenticatedCheckboxConfirmationService` 要求当前玩家的精确 PLAYER 请求已满足前置条件，
  除非定义显式允许 locked progress。终端 Snapshot Mapper 同步隐藏锁定态的确认按钮；解锁后使用原稳定幂等键可正常确认。
  这选择了 Base 的安全锁定规则，不仿造 BQ 未经确认的锁定任务底层点击行为。
- CodeGraph `impact AuthenticatedCheckboxConfirmationService.confirm` 19 个符号、`impact QuestRuntimeService.applyIf`
  6 个、`impact TerminalQuestCenterSnapshotMapper.detail` 3 个；`affected` 指向 Checkbox 服务测试和 Snapshot Mapper 测试。
  Docker Gradle `:galaxy-quest-core:test` 215 项、根定向 `TerminalQuestCenterSnapshotMapperTest` 3 项均通过，0 跳过/失败/错误；
  根测试首次因 `--tests` 过滤扩散至无匹配用例的 UI 子项目失败，排除非目标子项目后定向测试通过。`git diff --check` 通过。
- PARTY 同步配置/整队扇出、任务进度及奖励接收者归属、重复 Checkbox 重置、BQ 锁定任务面板可见性和真实 GTNH 交互仍未验证；
  Checkbox 与 M.2 继续 `BLOCKED`。未读取玩家进度或 S2 配置、未连接生产数据库、未执行 DDL、未部署或修改 Quest 开关。

### 2026-09-30 - M.2 Quest Completion 引用目标真实分布画像

- 按用户确认，只读扫描 S2 `config/betterquesting/DefaultQuests` 定义目录；没有挂载或访问玩家进度、其它服务端配置、
  玩家物品 NBT 或数据库。清点器只打印完成奖励的聚合行，不输出任务名/UUID：308 条引用全部有效，涉及 67 个来源任务、
  251 个唯一目标任务；无坏格式、缺失目标或自引用。按引用次数计，来源与目标都未配置 repeatable/GLOBAL；来源
  `autoClaim=true` 为 288/308、目标为 0/308。来源 `globalShare=true` 引用 26/308、目标为 117/308；这些 flag
  不代表 GLOBAL，也不能据此映射为 Base 队伍共享。画像仍不输出任务名或 UUID。
- 新增可复用 `questcompletion` 来源/目标配置聚合画像和不泄露 UUID 的合成回归测试。CodeGraph 已同步 importer 变更；
  `BqBehaviorInventory.from` 影响链 40 个符号，`affected` 命中 `BqBehaviorInventoryTest`。Docker Gradle
  `:galaxy-quest-bq-importer:test --tests BqBehaviorInventoryTest --rerun-tasks` 4 项通过，0 跳过/失败/错误；
  CodeGraph 对 `QuestCompletionRewardDeliveryHandler.deliver` 影响 13 个符号、对 `QuestRuntimeService.apply` 影响 3 个，
  `affected` 命中 PostgreSQL 集成类。新增个人目标隔离用例和自动来源→完成奖励→目标奖励仍可领取的 worker/JDBC
  链路用例；完整 `QuestPostgresIntegrationTest` 31 项在专用临时 PostgreSQL 16 中通过，0 跳过/失败/错误。
  数据库只在 tmpfs 容器中运行、无发布端口；容器与临时 Docker 网络已自动清理。首次 internal 网络仅导致 Gradle 上游
  脚本 DNS 失败；首次编译发现测试构造器参数误用，修正后完整集成类通过。
  `git diff --check` 在最终文档更新后通过。
- 此分布使后续优先验证对象从 GLOBAL/重复周期目标收窄到实际常见个人普通目标，但主体解析、full reset、历史恢复与
  BQ/Base 完成奖励时序仍没有等价证据；类型与整体 M.2 继续 `BLOCKED`。未执行迁移、连接生产数据库、部署或启用 Quest。

### 2026-09-30 - M.2 Quest Completion 奖励跨周期语义对齐

- 对照本地 BQ `RewardQuestCompletion.claimReward0`、`QuestInstance.isComplete`、`resetUser`：BQ 只在目标玩家尚未完成时写入完成时间；普通重复周期重置保留 completion entry，只有 full reset 删除。因此该奖励是“曾完成”而非只看当前周期状态。
- Base 的最新进度 upsert 会覆盖前周期状态，原 completion port 在目标 repeat quest 的新周期可能再次强制完成。新增进度级 `ever_completed` 闩锁、同事务身份锁查询和 `20260930_002_add_quest_lifetime_completion_latch.sql`；所有完成状态写入自动置闩，后续周期不能清除。迁移只从仍为 `COMPLETED` 的旧行回填；已被新周期覆盖的历史不能从现有表恢复。迁移未执行。
- 新增隔离 PostgreSQL 回归：完成 target 后把进度写入 cycle 1/IN_PROGRESS，再次执行 completion reward 必须返回 `ALREADY_COMPLETED`，保持第 1 周期进度与仅一份目标奖励 entitlement。
- CodeGraph 同步 4 个 Java 文件；`impact JdbcQuestRuntimeRepository.wasEverCompleted` 3 个符号、`impact JdbcQuestCompletionRewardPort.complete` 3 个符号；`affected` 命中 `QuestPostgresIntegrationTest`。Docker 一次性 PostgreSQL 16 容器、tmpfs 数据目录和独立网络中，Core + PostgreSQL 模块共 253 项通过，PostgreSQL 集成类 29 项执行且 0 跳过/失败/错误；另用临时旧表验证迁移回填与重复执行。完整根测试、assemble 和 Forge 运行时未执行。临时容器与网络已删除；`galaxy-base`、`galaxy-gtnh` 仍保持原运行状态。
- 仍存在并记录的差异：BQ 对 GLOBAL 任务按任一 UUID 的 completion entry 判定，Base 通过 participant scope/成员解析器选择 owner；BQ 写完成后由常规 tick 处理 autoClaim，Base 在一个事务内同时生成目标奖励 entitlement；full reset 的 Base 管理动作与旧历史恢复也未证实。`questcompletion` 和整个 M.2 仍 `BLOCKED`。未连接生产数据库、未执行生产 DDL、未部署、未切换或停用 BetterQuesting。

### 2026-09-30 - M.2 检索与制作物品 NBT 匹配语义对齐

- 对照本地 BetterQuesting `ItemComparison.StackMatch`、`OreDictionaryMatch`，以及 `TaskRetrieval.Detector` / `TaskCrafting.onItemInternal`：直接物品匹配比较 `(requiredTag, observedTag)`；OreDictionary 分支历史上反向调用 `(observedTag, requiredTag)`。BQ 还要求需求与观察到的 NBT 都为空或都非空；Base 之前在需求 NBT 为空时直接视为通配，并把 OreDictionary 路径也统一成直接匹配方向。
- Base 的非消耗库存快照、消耗/事件向量 evaluator 与 Minecraft 消耗适配器现在分别保留两种分支的参数方向，并使用同一空值/空 compound 语义。添加 Core 回归覆盖空需求 NBT 不匹配带标签物品、OreDictionary 反向 partial 比较、消耗检索及 crafting 事件；Minecraft 适配器的方向选择通过无 Forge 全局初始化的纯匹配辅助单测覆盖。
- 再次只读运行 S2 `DefaultQuests` 行为清单，结果仅输出 retrieval / optional_retrieval 的聚合画像，不输出物品名、Quest UUID 或原始 NBT：retrieval 5,539 个物品需求中 2,761 个开启 NBT 检查（空需求标签 2,343、非空 418），84 个含 OreDictionary、其中 49 个同时开启 NBT 检查；optional_retrieval 3,274 个需求中 1,432 个开启检查（空 1,327、非空 105），168 个含 OreDictionary、其中 117 个同时开启检查。两类中“OreDictionary + NBT 检查 + 非空需求标签”均为 0；反向比较是源码兼容实现，不把它误报成当前清单已触发的运行差异。来源摘要仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。
- CodeGraph `sync` 后 `impact InventorySnapshotTaskEvaluator.evaluate` 为 22 个符号、`impact ItemVectorTaskEvaluator.evaluate` 为 24、`impact MinecraftPlayerConsumptionPort.matchesItemNbt` 为 5、`impact BqBehaviorInventory.from` 为 35；CLI `affected` 对这些变更文件返回 0 个自动测试候选，因此按影响链显式运行三个定向测试类。临时隔离 Gradle settings 下 Core BQ evaluator 29 项、Importer 清点器 3 项；根 Forge 适配测试 12 项，合计 44 项，均 0 跳过/失败/错误。标准根构建首次遇到 GTNH convention 插件 `SocketTimeoutException`；模块测试用临时 settings 隔离，根测试用临时 LOCAL no-op Spotless shim 并跳过联网许可证任务，临时配置已移除。`git diff --check` 通过；未跑全量根 `test` / `assemble`。
- 直接调用 Forge `OreDictionary` 的首版单测因无 Forge bootstrap 的 JUnit 环境中 `FMLRelaunchLog.side == null` 而在初始化阶段失败；测试已改为覆盖生产代码共用的纯 NBT 分支选择，不宣称真实 OreDictionary 注册运行时已验证。retrieval、optional_retrieval、crafting 与整体 M.2 仍 `BLOCKED`；没有读取玩家进度/库存/数据库，没有生产 DDL、部署、服务操作或 Quest 开关变更。

### 2026-09-30 - M.2 物品奖励交付路径自动化与 S2 清单复核

- 按用户确认直接只读读取 S2 `config/betterquesting/DefaultQuests`：通过开发容器把该目录挂载为 `:ro`，运行仅解析定义文件的 `behaviorInventory`。摘要哈希仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，3,744 个任务、47 个章节、3,847 个放置；物品奖励 4,181、Choice 957、XP 2、Quest Completion 308；类型/行为频率与兼容矩阵一致。未读取玩家进度、NBT、数据库或其它服务器配置，未写服务端目录。
- 将 `MinecraftItemRewardDeliveryHandler` 的在线玩家副作用抽至包内 `PlayerState` 端口，生产仍包装真实 `EntityPlayerMP`、主背包、`QuestRewardMarker` 与 `QuestPlayerDataFlusher`；测试通过同一个 `deliver` 实现驱动 Minecraft `ItemStack`/`NBTTagCompound`，不模拟另写交付算法。
- 新增 4 项交付路径测试：同一 entitlement 重放不会重复发物；玩家存档写入异常回滚库存及标记且允许重试；空间不足先持久化已发数量、腾出空间后只交付余量；worker 已执行玩家保存但租约确认过期时，新 worker 重试只确认租约、不再次发奖。测试暴露原实现对 `complete` 先置 false 后从未重置的问题：一次完整插入被误报为 deferred。修复后完整写入即刻保存完成标记，部分写入仍 deferred。
- CodeGraph 改前 `impact MinecraftItemRewardDeliveryHandler.deliver` 涉及 34 个符号，只有算法级测试覆盖、未映射 `deliver` 测试；改后同步 2 个源文件共 116 节点，impact 涉及 57 个符号，`affected` 命中 `MinecraftRewardDeliveryTest`。本续轮 `impact RewardDeliveryWorker.runOnce` 涉及 20 个符号，新增 stale-lease 组合用例后同步 1 个测试文件 73 节点，`affected` 仍收敛到 `MinecraftRewardDeliveryTest`。首次定向运行因新增测试发现 `rewardStacks(lease, null)` 与新字符串重载歧义，改用私有命名方法；下一次运行暴露完成标记缺陷。Docker Gradle 根 `:test --tests MinecraftRewardDeliveryTest` 18 项与 `:galaxy-quest-core:test --tests RewardDeliveryWorkerTest` 4 项最终通过，均 0 跳过/失败/错误；`git diff --check` 通过。
- 此测试覆盖交付编排与租约过期确认后的幂等重放，但 fake `PlayerState` 不等同于真实 Forge 玩家对象、世界存档实现或进程重启测试；item reward 和整体 M.2 仍 `BLOCKED`。未执行生产 DDL、部署、服务器操作或 Quest 开关。

### 2026-09-30 - M.2 Choice 玩家随机领取终端闭环切片

- 将 BQ 已核实的 per-quest `forceClaimQuest` 随机选择语义接入现有玩家任务详情：任务可领取且至少一项 Choice 尚未选择时显示“随机选择并领取”，同时禁用普通领取并提示先选奖励；显式选择后恢复普通领取。已有选择、无 Choice 或不可领取时不显示随机入口；这不是跨多个任务的批量领取 UI。首次点击回归发现该新按钮虽已生成组件，却遗漏详情 LayoutSpec 节点、命中区为零；补齐布局后用 UiRuntime 在三档窗口实际指针事件验证互斥意图和命中区。
- 新增独立 `QUEST_FORCE_CLAIM` 动作键和终端桥接；客户端 payload 仍只有有界任务/浏览意图，无玩家 UUID、choice index 或随机结果。`TerminalService` 以当前服务端认证玩家调用 `forceClaimWithBqRandomChoices`，保留 Core 同事务资格预检、服务端随机策略、选择持久化与幂等领取。
- CodeGraph 开始前状态为 1,098 文件、29,281 节点、78,270 边；审查 `TerminalActionType`、`TerminalService`、`QuestCenterActionPort`、`QuestCenterVisualDocument` 与 BQ Claim 路径。Docker 定向测试最终通过：`:ui2-terminal:test --tests QuestParticipantManagementVisualTest` 3 项；根 `:test --tests TerminalQuestActionPayloadTest --tests TerminalServiceTest` 43 项，合计 46 项、0 跳过/失败/错误。首次 Docker Gradle 临时目录启动遇到下载超时，改用 compose 持久 `/gradle-home` 后完成。
- Core 与隔离 PostgreSQL 的随机选择事务测试、Minecraft 只物化所选 ItemStack 的适配器测试来自先前验证；本轮没有重跑它们。choice reward 与 M.2 仍为 `BLOCKED`：仍缺真实生产配置启用后的玩家奖励完整链、玩家背包/存档持久化和重启恢复、队伍接收者语义及 GTNH 多人实测。未执行生产 DDL、未读取玩家进度/生产数据库、未部署或启用 Quest 运行时，也未操作游戏服务器。

### 2026-09-30 - M.2 Choice 选项物化与强制领取语义复核

- 对照本地 BQ `GuiQuestLines.claimAll` / `NetQuestAction.forceClaimQuest`：强制随机选择是玩家显式“领取全部”意图，服务端按当前连接玩家复核 `canClaim(player, true)`，不是管理员专属权限。Base 仍未接对应的认证终端意图；客户端只能表达 force-claim 意图，玩家 UUID、领取周期、任务定义版本、选择结果必须继续由服务端解析。
- 补充 `MinecraftItemRewardDeliveryHandler.ItemLookup` 包内注入点，生产构造函数保持使用 Minecraft `Item.itemRegistry`；使纯 JUnit 可测试 item/choice 解析，而无需伪造 LaunchClassLoader 或修改全局 Forge Registry。新增回归验证 choice index 只物化被选中的选项、忽略未选项的数量，并按玩家最大栈数拆分且保留 meta。
- CodeGraph `impact MinecraftItemRewardDeliveryHandler` 影响 36 个符号，构造调用点仍只有 `QuestModule`；`affected` 对主源码未找到测试候选，因此运行 `MinecraftRewardDeliveryTest` 全类 14 项（0 跳过/失败/错误）通过。临时 PG Choice 事务用例已在前一条验证中通过；本轮 `git diff --check` 通过。
- Choice/item reward 以及整体 M.2 继续 `BLOCKED`：终端尚无 force-claim 玩家意图；多人/队伍身份、Minecraft 实际玩家背包写盘、重启恢复和 BQ 溢出掉落差异仍未做端到端验收。未读取玩家进度、NBT 或生产数据库；未执行生产 DDL、部署、Quest 开关或服务器操作。

### 2026-09-30 - M.2 Choice 强制随机领取服务端切片

- 按用户授权将 S2 `config/betterquesting/DefaultQuests` 只读挂载到一次性 `galaxy-dev` 容器并重跑行为清单：源摘要
  `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，3,744 个任务、47 个章节、3,847 个放置；
  retrieval 3,863、optional retrieval 1,267、choice reward 957。只解析任务/章节定义，没有读取玩家进度、背包或数据库，
  没有写服务器目录。
- 对照 BQ 的普通领取与 `force=true` 路径：普通玩家领取仍需先明确选择；强制领取才为未选 choice 随机挑选，若有
  `enhancedlootbags` 则仅在这些选项中均匀随机，否则在全部选项中均匀随机。`AuthenticatedQuestClaimService` 新增
  服务端 `forceClaimWithBqRandomChoices`：先以服务端锁定的领取目标预检，再在同一事务记录不可变选项并领取；保留既有选择，
  异常状态通过事务回滚。`QuestModule` 使用与领取仓储相同的 JDBC 连接管理器注入选择仓储。此能力尚无权限门控入口，未接终端，
  不能从客户端调用；普通 `claim` 路径不变。
- 新增 4 个 Core 回归：普通领取不自动代选、Lootbag 优先、无 Lootbag 时回退全选项、已有选择冲突保留及失败回滚。
  `:galaxy-quest-core:test` 210 项通过（0 跳过/失败/错误）；`:test --tests QuestModuleTest` 5 项通过；根 `compileJava`
  与 PostgreSQL 测试源码编译通过。随后在仅含临时 PostgreSQL 16、tmpfs 数据目录及专用 Docker 网络的隔离环境，
  执行 `QuestPostgresIntegrationTest.forceClaimWithBqRandomChoicesPersistsSelectionAndQueuesOnlyThatOption` 通过：
  普通领取仍返回 `NEEDS_CHOICE`，强制路径持久化唯一 Lootbag 选项并排入该选择对应的交付租约，重复强制领取返回
  `ALREADY_CLAIMED`。CodeGraph 同步后 `impact AuthenticatedQuestClaimService` 覆盖 40 个符号、
  `impact JdbcRewardChoiceSelectionRepository` 覆盖 18 个；`affected` 未形成有用测试候选，故显式运行 Core、模块及该 JDBC 集成用例。
- 临时 PostgreSQL 容器和专用网络已清理；未连接 `galaxy-base`，未执行生产 DDL、部署、Quest 开关或服务器操作。
  choice 和整体 M.2 继续 `BLOCKED`：强制领取尚无权限门控/调用入口，多人主体映射及 GTNH 真实物品交付/重启恢复仍待证明。

### 2026-09-30 - M.2 optional_retrieval 与手动物品提交语义核对

- 对照 `TaskOptionalRetrieval.ignored(UUID)`、`QuestInstance.update/detect` 与 Base `QuestEngine`：BQ optional retrieval 不参与 Quest 完成门槛；Base importer 已将其映射成 `optional=true`，引擎按非必需任务处理。新增 `QuestEngineTest.bqOptionalRetrievalDoesNotBlockQuestCompletion`，验证必需任务完成时可选检索未完成也不阻挡 Quest 完成，并保留可选任务未完成状态。
- 按用户再次授权，只读挂载服务器 S2 `config/betterquesting/DefaultQuests` 并复核；输出基线为 3,744 任务、47 章节、3,847 放置，与先前统计相符。只读范围不含进度、NBT、数据库；解析输出完整，但 Gradle 最后报 `SocketTimeoutException`，不记为构建通过。
- Base 现保留全库存匹配物品提交，同时新增服务端绑定的单个手持栈提交。后者只接受认证玩家当前 hotbar 槽位，待恢复记录保存槽位和 NBT 指纹并在扣除前复核，数量最多 64；两种 UI 动作均明确标成“提交并消耗”，不再将扣物品操作称为“检测”。仍未声称模拟完整 SubmitStation 或 BQ Detect。
- CodeGraph 影响审查：`JdbcConsumptionSubmissionRepository.prepareIfAbsent` 21 个符号，`QuestCenterVisualDocument.detail` 3 个；`affected` 指向 `QuestPostgresIntegrationTest`，UI2 文档无自动测试映射。`:galaxy-quest-core:test` 206 项、`:ui2-terminal:test` 24 项零失败；根 `compileJava` / `compileTestJava` 成功，使用 `--no-configuration-cache` 避开 `prepareUi2QzLicense` 的既有缓存序列化问题。临时隔离 PostgreSQL 16 的持久往返用例 1 项通过，未连接项目运行库。`git diff --check` 通过。
- 没有生产 DDL、部署、Quest 开关或服务状态变更；不读取玩家任务进度或资产。retrieval / optional_retrieval 与整体 M.2 继续 `BLOCKED`，GTNH 实际物品恢复、多服时序及完整站台边界仍未验收。

### 2026-09-30 - M.2 BQ 非消耗 Retrieval 的 PARTY Core 扇出切片

- 按 S2 `DefaultQuests` 中 retrieval/optional_retrieval 的真实高频配置，新增明确的服务端 PARTY 聚合库存事实标记；planner 验证来源玩家仍属于同一权威 PARTY，并仅为非消耗检索任务展开 PARTY 全体成员的独立 PLAYER 进度请求。与旧的 PARTY/TEAM 共享进度行分开处理。
- 每位成员保留自己的物品需求向量和前置 Quest 判定；普通 retrieval 的已达标分量按 BQ 规则保留。任一成员的任务向量达标时，Task 完成标记扇出至同 PARTY 其他个人进度。锁定成员可以保存该 Task 完成，但不提前完成 Quest 或生成奖励权益；其前置满足后再完成并生成个人权益。
- 新增 Planner 回归覆盖精确 PARTY 绑定、仅个人非消耗任务、party scope/consuming 排除、错误成员拒绝和来源玩家前置门控；Runtime 回归覆盖两名玩家不同向量、任务完成扇出、锁定期间不发奖及解锁后完成。测试发现并修复 Engine 合并快照时忽略显式扇出完成标记的问题。
- CodeGraph `status/sync` 后检查 `QuestRuntimeService`（34 个影响符号）、`QuestEvaluationPlanner`（31）和 `QuestEngine`（27）。CLI `affected` 对这些文件返回 0 条测试依赖边；故据 impact 明确选择 Planner、Projector、Engine、Runtime 四个 Core 测试类，而非把空 affected 结果当作无需测试。
- Docker Gradle 使用持久挂载的 `/gradle-home` 执行：`:galaxy-quest-core:test` 198 项通过，0 跳过/失败/错误；`:galaxy-quest-postgres:test` 35 项中 10 项通过、25 项隔离数据库条件测试跳过，未声称 PostgreSQL 集成已验证。`git diff --check` 通过。
- Core 合同目前没有生产事实来源；现有 Minecraft tick 只提交触发玩家自己的库存快照，尚无跨服活跃成员库存聚合/过期快照/可信生成器。源码审查还确认当前 `galaxy_quest_fact.attributes_json` 会持久保存完整库存观察且表结构没有 TTL 清理；不得直接把历史事实表误用为“当前在线库存”，下一步须先设计有界、可过期的快照存储/清理及跨服在线语义。因此 retrieval 与 optional_retrieval 继续 `BLOCKED`，不部署、不运行 migration、不读玩家进度或数据库，也不更改线上 Quest/BQ 开关。

### 2026-09-30 - M.2 BQ Hunt PARTY 逐玩家进度扇出

- 对照 BQ `TaskHunt.onKilledByPlayer` 与 `ParticipantInfo.ALL_UUIDS` 后，将 Hunt PARTY 行为接入实际 gameplay planner：匹配的
  BQ Hunt fact 通过服务端解析的 PARTY 成员快照，为全体成员分别生成 PLAYER 进度请求；普通事件、TEAM/PARTY 聚合范围、
  Checkbox 和消费请求不走 fan-out。
- `QuestEvaluationRequest` 与 `QuestFactProjector` 分离进度 owner 和事实授权 membership；仅 PARTY 快照同时包含触发玩家
  与目标成员时允许投影，成员快照外的人仍拒绝。每位成员独立检查前置 Quest、重复周期与奖励权益；重复事实仍幂等。
- 增加 3 项回归，覆盖 planner 全成员展开及成员前置条件、非 Hunt/无 PARTY/聚合 scope 不扩展、runtime 为触发成员和其他队员
  各完成个人进度并生成个人奖励且相同事实不重复。
- 增加 PostgreSQL 集成回归：在临时 PostgreSQL 16 实例内创建 PARTY 和两名成员，让 hunter/member 分别模拟 S2、Lobby
  同时发出击杀 fact，通过真实 JDBC membership resolver 运行 planner/runtime，确认每名成员个人进度各累计两次、两份
  奖励权益持久化，重复 fact 不再发放。隔离 schema 在测试 teardown 删除，临时容器随后移除，没有使用运行数据库。
- CodeGraph 同步后检查 `BqHuntPartyAssignmentPolicy`、`QuestEvaluationPlanner.requestsFor`、`QuestFactProjector.project` 的
  impact；`affected` 选择 Planner、Runtime、PostgreSQL 集成三个测试文件。根 `compileJava` 通过；Core 194 项和 PostgreSQL
  35 项共 229 项测试通过，0 跳过/失败/错误；`git diff --check` 通过。未运行全仓 `test` / `assemble`。
- Hunt 仍 `BLOCKED`：隔离 PostgreSQL 已验证 Base PARTY 成员快照、两来源并发 fact 和奖励幂等，但 Base PARTY 与历史
  BQ Party 数据的身份/成员映射、真实跨服投递及 GTNH 实体注册/NBT 仍需证据。未读取玩家进度或项目数据库，未部署、未运行
  migration、未改现网任务系统。

### 2026-09-30 - M.2 BQ Hunt 重复周期修复与 PARTY 语义核对

- 核对本地 BetterQuesting 源码 `TaskHunt` 与事件处理器：合格的玩家击杀遍历 `ParticipantInfo.ALL_UUIDS`，将每位
  party 成员各自的 hunt 进度加 1；重复重置清空新周期任务进度。`globalShare` 不是 party 同步选项，只影响全局任务
  在 `isGlobal && !globalShare` 时的 claim 语义。
- 只读统计 S2 `DefaultQuests` 的 104 个 hunt：subtypes true/false 为 103/1，ignoreNBT=false 为 2，damageType
  全为空；58 个配置有 targetNBT，但仅 2 个启用 NBT 检测；84 个 hunt 项的所属 Quest 可重复（按 hunt 项计）。没有记录 quest 身份、名称、
  实体目标或原始 NBT。
- 新增 `QuestRuntimeServiceTest.repeatableBqHuntStartsNextCycleFromFreshKillProgress`，先复现冷却期满后的首个
  击杀错误复用前周期已完成计数；修复后首个击杀回到 1/2，第二个完成新周期并产生第二份权益。引擎与 runtime
  共用 `beginsNextCycle` 判定；开始新周期时用空进度计算当前事实，pending-consumption hold 仍阻止提早 rollover。
- CodeGraph `impact QuestRuntimeService.execute` 3 个符号、`QuestEngine.beginsNextCycle` 6 个；`affected` 指向
  `QuestRuntimeServiceTest`。根 Gradle 在 GTNH convention 插件离线初始化阶段超时；采用只挂载 `galaxy-quest-core`
  的临时纯 Java harness，完整 Core 191 项通过，0 跳过/失败/错误。未运行根构建、数据库集成测试或生产操作。
- hunt 仍为 `BLOCKED`：BQ 会把击杀同步到 PARTY 所有成员的独立玩家进度；Base 导入默认 PLAYER scope，现有 planner
  未证明兼容该 fan-out；真实 GTNH EntityList/NBT 与跨服组队 membership 也未实机验证。矩阵已记录此差异。

### 2026-09-30 - M.2 retrieval BQ 队伍库存语义复核

- 用户明确授权直接只读读取服务端 quest 定义。将 S2 `config/betterquesting/DefaultQuests` 只读挂载到临时
  `galaxy-dev` 容器并运行 importer `behaviorInventory`；准确复核为 3,744 个任务、47 个章节、3,847 个放置，
  source hash `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。retrieval 共 3,863 项
  （consume=true 307、groupDetect=true 4、autoConsume=true 0）；optional retrieval 共 1,267 项，全部非消耗且
  groupDetect=false。首跑任务报告已打印，但 Gradle 因既有 configuration-cache 序列化问题返回失败；加
  `--no-configuration-cache` 后同一只读清点以 exit 0 完成。未读取玩家进度/背包、其它服务器配置或数据库，未写服务端目录。
- 对照 BQ `TaskRetrieval.Detector`、`ParticipantInfo`、`TaskBase` 与 `QuestInstance`：所有非消耗 retrieval 都对
  `ALL_UUIDS` 保持独立 UUID 进度，并扫描 `ACTIVE_PLAYERS` 的库存；`groupDetect` 只改变重算时清零哪些向量。任一
  UUID 达标会将该 Task 标记给所有 UUID。`globalShare` 不负责此库存/进度同步，只改变 `isGlobal && !globalShare`
  下的 quest reward claim 查询。
- 因此当前 Core 的 `InventorySnapshotScope`（把群组快照投影到 PARTY/TEAM 共享进度行）不能代表 BQ retrieval；
  虽有单人/显式聚合快照 evaluator 测试，但仍缺跨服活跃在线成员库存源、逐 UUID 个人进度投影、Task 完成扇出及其与重复周期、
  奖励的事务对照。本轮只更正文档与阶段边界，未改业务代码、生产 DDL、玩家数据或服务器状态；retrieval 与
  optional_retrieval 继续 `BLOCKED`。
- CodeGraph 状态已同步；只读检查 retrieval evaluator、planner/projector 与消费/事件入口影响链。Gradle 行为清点不属于
  测试套件；未运行全仓测试、assemble 或部署。下一步应先设计并隔离验证可过期的服务端在线库存快照合同，而不是把 BQ
  `groupDetect` 直接映射为 Base 的共享 participant scope。

### 2026-09-30 - M.2 BQ NBT 列表部分匹配对齐

- 用户确认可直接读取服务端 quest 定义。将 S2 `config/betterquesting/DefaultQuests` 只读挂载至隔离
  `galaxy-dev` 容器并运行 `behaviorInventory`：源摘要仍为
  `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，3,744 个任务、47 个章节、3,847 个放置；
  清单与既有频率统计一致。只访问任务/章节定义，没有读取玩家进度、物品、其它配置或数据库，没有改服务端文件。
- 对照本地 BQ `ItemComparison.CompareNBTTag`：普通 `NBTTagList` 每个需求都会重新搜索全列表，因此可复用同一个
  实际元素；byte/int array 则用 `usedIdxs` 阻止复用。Base 的 `PortableNbtMatcher` 此前把三种都按唯一元素匹配，
  现已按这两个 BQ 分支拆开，并为重复 compound 列表和两类数组加入对照测试。重复列表匹配会较多重集合语义更宽松，
  是明确复现 BQ 行为的安全审查项。只读扫描 S2 `DefaultQuests/Quests` 中 `ignoreNBT=0`、`partialMatch=1` 的
  三类任务后，在 retrieval 的必需物品需求里确认 3 处完全重复的 compound 列表条目（optional retrieval 与 crafting
  各为 0）；这证实真实定义存在相关结构，但不推断受影响任务数或实际玩家运行结果。
- 定义扫描统计 retrieval / optional retrieval / crafting 分别为 1,834 / 577 / 87 个筛选任务；非空需求 NBT
  物品栈为 418 / 105 / 7，通用 NBTTagList 节点为 247 / 72 / 5。扫描仅读取任务定义，不涉及玩家进度、物品、
  其它配置或数据库，未修改服务端文件。源摘要仍为
  `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。
- CodeGraph 改前 `impact PortableNbtMatcher.matches` 覆盖 35 个符号；新增 evaluator 回归测试前
  `impact InventorySnapshotTaskEvaluator.evaluate` 覆盖 19 个符号；`affected` 命中 matcher 与 evaluator 两个测试类。
  Docker Gradle `:galaxy-quest-core:test` 190 项通过、0 跳过、0 failure、0 error；根
  `MinecraftPortableNbtCodecTest` + `MinecraftPlayerConsumptionPortTest` 前序验证 9 项通过；`git diff --check` 已通过。
- 本项 NBT 列表对照涉及 Core matcher、相应测试及兼容矩阵/Worklog；没有部署、生产 DDL、数据库连接、Quest 开关变化
  或 BetterQuesting 停用，没有改变 S1/S2 运行状态。

### 2026-09-29 - M.2 noRewards 奖励抑制实现与旧库启动门禁

- 接着上节的源码与真实 `DefaultQuests` 评估，实现默认关闭的服务端 `quest.noRewards`：BQ `ignoreDisabled` 类型缺省值
  与数字布尔 `0/1` 一致；自动领取权益与人工领取分别在生成/事务内标记 `SUPPRESSED`，不排入真实交付；豁免奖励仍
  可投递，choice 仍要求玩家先选择。任务摘要区分抑制状态，终端返回明确状态。
- 为避免旧 schema 接受不了新终态，仅当该开关启用时做只读检查；无状态约束就 fail closed。新增迁移
  `ops/sql/migrations/20260929_001_add_quest_reward_suppressed_status.sql`，只新增允许状态。本轮没有执行迁移、读取
  生产数据库或设置开关；服务端 Quest 与 BetterQuesting 均未切换。
- CodeGraph `status/sync` 后检查 `QuestModule.serverStarting`、创建工厂及奖励仓储调用链；对 `RewardSuppressionPolicy`
  影响 182 个符号、`JdbcRewardClaimRepository` 171 个、`QuestRewardSummary` 41 个；`affected` 命中 Core 策略、
  PostgreSQL 集成、配置与模块启动测试共 4 个类。
- 测试：Core 全模块 189 项；PostgreSQL 全模块 34 项（含 24 项集成测试，0 跳过）；根模块定向 6 项；均为
  0 failure/0 error。PostgreSQL 测试运行在临时网络和 tmpfs PostgreSQL 16 容器中，网络与 `galaxy-base` 隔离；
  测试完成后容器和网络均已移除。一次全任务强制重跑误触发离线下需要网络的许可证准备任务，改回增量构建后
  根测试通过；未修改许可证和构建任务。
- 这只闭合全局奖励抑制与旧库门禁，item/choice/XP 的 GTNH 实际发放、组队归属、强制随机领取和恢复仍未完成；
  M.2 与 BetterQuesting 替代继续为 `BLOCKED`。CodeGraph 与测试通过后更新本矩阵；未部署。

### 2026-09-29 - M.2 重复手动消耗检索的周期绑定

- 对照只读 BQ 源码 `TaskRetrieval.submitItem`、`QuestInstance.resetUser` 和 `EventHandler` 重复任务重置：手动物品
  提交会推进当前周期；冷却重置清空任务进度。基于已授权 `DefaultQuests` 行为清点，228 个 retrieval 属于
  “重复 + 手动消耗”。没有读取玩家进度、数据库或个人物品。
- 消耗请求将 quest cycle 写入已有 `resource_parameters_json` 保留字段及确认事实，不新增数据库列/DDL。个人重复
  retrieval 现在可授权；同一题、同一周期已有 `PREPARED/APPLIED` saga 时拒绝并行扣除；运行时在同一进度事务中查询
  待恢复行并暂缓周期 rollover。相同稳定请求键重试先复用并校验原请求，避免周期切换后重新计算不同扣除量。
- CodeGraph 改前 impact：`QuestRuntimeRepository` 40、`QuestRuntimeService` 32、`QuestEngine` 13、
  `ConsumptionRequestAuthorizer` 18、`JdbcQuestRuntimeRepository` 37 个符号；随后执行 `codegraph status/sync`，
  索引仍为 1,079 文件/28,623 节点/75,823 边。`codegraph affected` 命中 PostgreSQL 集成测试；根据调用影响范围
  显式执行 Core 与 PostgreSQL 全模块测试。
- Docker Gradle `:galaxy-quest-core:test` 与 `:galaxy-quest-postgres:test` 通过，分别 179/29 项，0 跳过、失败或错误。
  PostgreSQL 29 项中有 20 项在独立网络 `quest-m2-it-20260929` 上连接临时 PostgreSQL 16 容器实测；已停止并移除
  临时容器和网络，没有连接运行中的 `galaxy-base`。首次使用空 `/tmp/gradle-home` 时仅 Gradle distribution 下载超时，
  改用开发容器缓存后完成测试。最终 `git diff --check` 通过。未部署、未执行生产 DDL、未修改
  玩家进度、未启用 Quest 或停用 BQ。
- `docs/betterquesting-m2-behavior-compatibility-2026-09-23.md` 已补周期 saga 实现边界、隔离 PostgreSQL 证据和
  剩余运行时门禁。retrieval/optional_retrieval、M.2 和 BetterQuesting 替代整体仍为 `BLOCKED`；旧格式 pending 行按 cycle 0 读取，
  生产启用前必须先核对不存在跨周期旧待处理 saga。

### 2026-09-29 - M.2 BQ 全局 noRewards 行为与状态边界

- 用户确认可直接读取 GTNH 服务端 `DefaultQuests`；继续遵守只读范围。本次补查本地 BQ 源码中的
  `BQ_Settings.noRewards`、`AbstractReward.claimReward`、`QuestInstance.claimReward` 与 `RewardChoice`：全局开关
  默认关闭；开启时 `ignoreDisabled=false` 的奖励函数直接跳过，但 BQ 仍将任务记为已领取；强制领取仅对尚未选中的
  choice 做随机选择，并优先 enhancedlootbags。该开关不属于任务 JSON。没有读取服务器其它配置、玩家进度、数据库或物品数据。
- 重新对 S2 `DefaultQuests` 执行只读 `behaviorInventory`：规划源摘要仍为同一 SHA-256，3,744 个任务、47 个章节、
  3,847 个章节放置；最高频 retrieval 3,863 项中 `partialMatch=true` 3,861、false 2 项；optional_retrieval
  1,267 项中 true 1,263、false 4 项。行为清单与文档中的主要频率一致，未把 Quest UUID 或 JSON 写入报告。
- 实际清单的 `ignoreDisabled` 分布已由只读扫描确认：item 4,181、choice 957、XP 2 全为 false；questcompletion
  308 全为 true。可确认开启全局开关时前三类会被抑制、questcompletion 仍执行；不能据此推断 S2 当前开关值。
- 评估 Base 当前权益状态机后，拒绝用“成功交付”伪装被抑制的奖励。正确实现需要 `SUPPRESSED` 终态、订单/任务摘要
  显示、投递尝试审计与数据库约束迁移，并需把领取时的服务器策略固定到该次权益。现有 schema 无此状态，故本轮
  不做隐式状态映射，不执行也不新增生产数据库变更；全局 `noRewards` 兼容继续 `BLOCKED`。
- 补齐导入器对 BQ 标准奖励 `ignoreDisabled` 缺省值的处理：command、questcompletion、scoreboard 默认 true，
  item、choice、XP 默认 false；显式存储值优先，未知 reward 类型不推测。新增 importer 回归覆盖默认与显式覆盖。
  CodeGraph `impact BqStandardCodec.reward` 覆盖 46 个符号，`affected` 将测试范围收敛到
  `BqQuestDefinitionImporterTest`。CodeGraph 刷新后 impact 31 个符号，affected 仍只有该 importer 测试类。
  Docker Gradle `:galaxy-quest-bq-importer:test` 通过：37 项、5 项环境条件跳过、
  0 failure/0 error；`git diff --check` 通过。只构建/测试 importer 模块，没有部署或运行生产任务。
- 更新 `docs/betterquesting-m2-behavior-compatibility-2026-09-23.md` 与本 Worklog；这是源码行为结论和实现边界，
  不把它误报为功能已实现。M.2 整体仍 `BLOCKED`，生产 Quest 与 BetterQuesting 均未切换。

### 2026-09-29 - M.2 选择奖励重复周期继承与服务端清单复核

- 按用户授权，将 S2 `config/betterquesting/DefaultQuests` 以只读卷挂载到隔离开发容器，仅运行 `behaviorInventory`；
  确认来源摘要仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`、3,744 个任务、47 个章节，
  类型频率与矩阵一致。未读取玩家进度、背包/物品或数据库，未写服务端目录。
- 对照 BQ `RewardChoice` 的玩家选项在任务重置后保留语义，修改 `JdbcQuestRuntimeRepository`：只在新建
  `bq_standard:choice` entitlement 时，从同玩家、同任务定义版本、同 reward key 的最近先前周期继承仍有效的
  选项索引；无历史选择或选项已越界时保持未选择，不做随机代选。无需 DDL。
- 将集成用例改为验证“旧周期选择在新周期继承并只交付本周期奖励”及“从未选择者仍需选择”。CodeGraph
  `impact JdbcQuestRuntimeRepository.insertEntitlementsIfAbsent` 覆盖 36 个符号；`affected` 指向
  `QuestPostgresIntegrationTest`。隔离 Gradle harness 中 Quest Core 174 项、PostgreSQL 29 项全通过，0 跳过、
  0 failure/0 error；20 项 PostgreSQL 集成测试在临时 PostgreSQL 16 容器执行。临时数据库容器和网络已移除，
  仅运行此隔离数据库，不连接生产 `galaxy-base`。本轮未跑根项目完整构建或部署。
- 继续对照最高频 `bq_standard:item` 的 `RewardItem.claimReward0` / `BigItemStack`：发现 BQ 仅将主背包作为收件空间，
  Base 此前以 `getSizeInventory()` 可能把奖励写入护甲槽。现将奖励快照及失败回滚限制到 `mainInventory`，并增加
  不能借用空护甲槽存放奖励的测试；CodeGraph `impact deliver` 覆盖 20 个符号、`affected` 指向
  `MinecraftRewardDeliveryTest`。标准离线配置因 GTNH convention 插件初始化抛出 `SocketTimeoutException`；随后用
  临时 `LOCAL` no-op Spotless shim 启动仅限 root `:test` 的定向构建，Forge/Base 主源码与测试源码编译通过，
  `MinecraftRewardDeliveryTest` 9 项全通过、0 跳过/失败/错误。该临时 shim 已删除；未运行完整 root suite 或部署。
- 更新 M.2 矩阵：个人选择奖励重复周期行为已实现并过隔离 DB 验证，但 `choice` 仍为 `BLOCKED`，因为多人接收者、
  服务器 `noRewards`、BQ 强制随机选择及真实 GTNH 奖励交付仍未闭环；Base 选择跨进程持久与 BQ 内存映射的差异也保留。
- 清单审计还发现 `BqBehaviorInventory` 漏记了 `ignoreDisabled` 的真假分布；将它纳入行为开关并补回归测试。只读
  S2 扫描确认 item 4,181 项、choice 957 项、XP 2 项全部 `ignoreDisabled=false`，questcompletion 308 项全部为
  `true`，因此 BQ 的 `noRewards` 只抑制前三类。Importer 测试共 37 项、5 项条件跳过、0 失败/错误；清单分析仍不
  读取玩家进度/物品或数据库，未保存任务 UUID/原始配置。

### 2026-09-29 - M.2 retrieval `groupDetect` 聚合快照核心语义

- 经用户授权只读扫描 S2 `DefaultQuests/Quests` 定义，确认恰有 4 个 `groupDetect=1` 的 retrieval：
  `AvengersAssemble`、`RefinedFirestone`、`AlloySmelter`、`PrecisionLaserEn`；均为非消耗任务。没有读取
  玩家进度、库存或数据库。其 `globalShare` 字段不被擅自等同于 Base 的 PARTY/TEAM/PUBLIC 主体。
- Core 新增显式群组库存快照 scope 合同：Planner 从事实中的 participant ID 重新解析当前成员，并要求事实来源玩家
  仍属于该成员集合；只路由到同 scope 的非消耗 retrieval/optional retrieval。Projector 拒绝单玩家快照覆盖共享进度、
  拒绝群组快照写入个人或其他群组。`groupDetect=true` 重算所有未完成需求，QuestEngine 以最新绝对快照替换未完成
  进度，完成任务保持锁存。该改动仅定义并验证“若将来有正确聚合快照”的算法，不新增运行时快照 producer。
- CodeGraph 同步 7 个源文件；`impact QuestFactProjector` 20 个符号、`InventorySnapshotTaskEvaluator` 20 个、
  `QuestEvaluationPlanner` 25 个、`QuestEngine` 28 个；`affected` 收敛到
  `QuestEngineTest`、`QuestFactProjectorTest`、`QuestEvaluationPlannerTest` 与
  `BqCompatibleTaskEvaluatorsTest`。Core 单模块测试 174 项全通过、0 跳过、0 failure/0 error；使用临时独立
  settings 绕过 root convention 插件、且测试结束已删除临时设置文件。标准 root Gradle 因 GTNH convention 初始化
  网络超时；试用 LOCAL 模式又因仓库未提供 `gtnhShared/spotless.gradle` 未能配置，故本轮不宣称根构建通过。
  `git diff --check` 通过。没有部署或更改生产任务/数据库；retrieval 与 optional_retrieval 继续 `BLOCKED`，因为
  当前没有跨服活跃成员库存聚合 producer，且 `globalShare` 到主体模型的映射语义仍未确认。

### 2026-09-29 - M.2 高频检索耐久物品匹配对齐

- 依据只读 GTNH 任务清单的频率，`retrieval` 3,863 项、`optional_retrieval` 1,267 项，是当前最高频两类。
  对照 BQ `TaskRetrieval.Detector` → `ItemComparison.StackMatch`，确认与 crafting 相同：registry/Item 相同且
  物品 `isDamageable()` 时，需求与背包栈可忽略 metadata/damage；非耐久 Item 仍要求 metadata 或 wildcard。
- Base 现在从真实库存栈采集 `damageable` 属性，纯 Java `InventorySnapshotTaskEvaluator` 仅在 registry name
  相同时应用该兼容规则；库存规范化会保留属性且不会合并属性不一致的条目。新增 evaluator 正反例与
  canonicalizer 合并测试。没有假设当前清单中一定包含耐久型需求；这是对 BQ 通用匹配语义的实现。
- CodeGraph `status/sync` 同步 6 个变更文件；`impact InventorySnapshotTaskEvaluator` 覆盖 20 个符号，
  `impact inventoryObserved` 覆盖 26 个符号，`affected` 指向 `BqCompatibleTaskEvaluatorsTest` 与
  `InventoryObservationCanonicalizerTest`。完整 Docker Gradle 门禁（Quest Core/PostgreSQL/BQ importer、UI2、
  根 `test assemble verifyUi2SelfContained` 与 `:ui2-demo:renderGolden`）通过；XML 合计 821 项测试、
  77 项环境条件跳过、0 failure/0 error。PostgreSQL 凭据置空，DB 集成测试按约定跳过；`git diff --check`
  通过。运行 JAR `build/libs/jsirgalaxybase-d27d2ee-main+d27d2ee371-dirty.jar` SHA-256 为
  `a4303acfce04738b62547236d4cdbd9eea9d7f98382fadc51b827c617bc9e581`；未部署、未接触生产进度或数据库。
- retrieval/optional_retrieval 仍为 `BLOCKED`：damageable 与群组聚合快照规则的 Core 测试不证明 GTNH
  注册/NBT/OreDictionary、活跃成员 inventory 时序等价；跨服活跃成员快照 producer、重复周期及真实消耗恢复仍待实现/验证。

### 2026-09-29 - M.2 crafting 耐久物品匹配对齐

- 继续读取只读 S2 `DefaultQuests` 清单（用户已明确授权）；crafting 133 项中 `allowCraft/allowSmelt=true`、
  `allowAnvil=false`、`partialMatch=true` 全量一致；NBT 严格匹配 87 项、忽略 NBT 46 项；仅 6 项启用原版
  合成统计。只读取任务定义，没有访问玩家进度、物品或数据库。
- 对照 `Reference/BetterQuesting` 的 `TaskCrafting.onItemInternal`、`TaskCrafting.detect`、
  `TaskProgressableBase.resetUser`、`ItemComparison.StackMatch`：发现 BQ 同一 `Item` 的耐久型物品不要求
  damage/meta 相同。Base 事实现在携带 `Item.isDamageable()`，`ItemVectorTaskEvaluator` 仅在 registry ID 相同
  且事实标明可损坏时忽略 damage；新增 mismatched damage 正反例。又发现 BQ 统计 fallback 只查 Item ID 的
  `objectCraftStats`，不经过 metadata/NBT 匹配；Base 现对 `craft-statistics` 独立走 registry/Item ID 匹配，
  并以带 NBT、不同 damage 的任务项验证。另增加 crafting 周期用例验证向量周期重置后绝对计数可重新完成并
  生成新周期奖励资格。
- CodeGraph `status/sync` 确认索引最新；签名修正后同步 1 个文件。`impact` 检查
  `ItemVectorTaskEvaluator`（19 个受影响符号）与 `QuestGameplayEventHandler`（26 个受影响符号）。本轮
  `affected` 对传入源文件未解析出测试文件，因此没有据此缩小测试面，直接执行 Quest Core、Minecraft 主源集
  编译与完整门禁。
- 开始时发现 Base 每 20 ticks 无条件采样 `StatList.objectCraftStats[itemId]`，与 BQ 主要由匹配产物事件或手动
  Detect 触发不同。本轮后续已移除无条件轮询，改在 craft/smelt/anvil 事件附带统计和触发物品，由 evaluator
  复核任务 item 匹配及该事件渠道开关，才允许绝对统计推进；新增无关产物、禁用熔炼、匹配合成三个门禁测试。
  终端显式 Detect 仍未实现；只读历史统计的新任务在尚无匹配事件时不能主动检查，故 crafting 继续 `BLOCKED`。
  本轮未部署、未触碰生产数据/数据库，也未启用 Quest 或停用 BetterQuesting。
- 修复统计刷新门控的 `Map`/`GameplayFact` 辅助方法签名不匹配后，完整 Docker Gradle 门禁
  `:galaxy-quest-core:test :galaxy-quest-postgres:test :galaxy-quest-bq-importer:test`、UI2 模块测试、根
  `test assemble verifyUi2SelfContained` 通过；另行运行 `:ui2-demo:renderGolden` 成功。测试 XML 合计
  819 项、77 项条件跳过、0 failure/0 error（Quest Core 167、PostgreSQL 29/20 skipped、BQ importer
  37/5 skipped、根 514/52 skipped、UI2 其余模块 72 项）。PostgreSQL 凭据置空，DB 集成场景按约定跳过；
  `git diff --check` 通过。组装产物 `build/libs/jsirgalaxybase-d27d2ee-main+d27d2ee371-dirty.jar`
  SHA-256 为 `e49a2a3c25657cb89460b5546fc2f78f32130ee57885f2b3f3a65fc5c88be32f`；只构建、未部署。
- 溯源边界：项目既有文档记录上游 BetterQuesting 参考 commit `524b365211b6b3a9672cab8ae45b4e07e726d49e`，
  但当前 `Reference/BetterQuesting` 没有独立 Git 元数据，该 commit object 也无法从本仓库对象库读取；当前
  文件只能作为本地源码参考，尚未证明与该 pin 一致。

### 2026-09-29 - BQ M.2 检索任务库存进度回落

- 对照 `TaskRetrieval.Detector` 发现 BQ 的普通非消耗检索任务会在检测时保留已达标需求、重算未完成需求；Base evaluator 已这么计算，但 `QuestEngine` 的通用单调合并又把旧进度覆盖回来，物品离开背包后显示进度无法下降。
- 为 `bq_standard:retrieval` 与 `bq_standard:optional_retrieval` 的非消耗、非 `groupDetect` 快照增加专用合并语义：未完成分量按最新库存替换，任务完成后保持锁存；消耗型检索仍累计经过授权的扣除事实。增加 QuestEngine 回归测试覆盖降进度、完成锁存和消耗累计。
- CodeGraph 检查 `InventorySnapshotTaskEvaluator`、`QuestEngine` 与 `QuestFactProjector` 影响链；新增回归仅影响 `QuestEngineTest`、`QuestFactProjectorTest`。Docker Gradle 最终在临时指定 `gtnh.settings.blowdryerTag=LOCAL`、关闭格式检查并跳过联网下载的 `prepareUi2QzLicense` 后通过：Quest Core 161 项、BQ importer 37 项（5 项环境条件跳过）、根项目 512 项（52 项环境条件跳过），总计 710 项、57 项跳过、0 failure/0 error；`assemble` 和 `verifyUi2SelfContained` 通过，`git diff --check` 通过。构建前核验复用的 Qz 许可证 SHA-256 与固定值一致。PostgreSQL 凭据显式置空，未执行数据库集成测试；多人 groupDetect、全部消耗提交时序、物品精确匹配、重复周期和奖励交付仍需验证，两个检索类型维持 `BLOCKED`；未部署、未修改生产数据。

### 2026-09-23 - BQ M.2 复选任务认证确认入口

- 冻结 M.2 行为边界，在既有终端任务详情为未完成的 Checkbox 增加“确认”按钮；网络动作只携带任务与 Task key，身份取自服务端认证玩家。纯任务服务校验发布定义、版本、类型和周期，拒绝非 PLAYER 主体，重复点击不推进进度。
- 终端快照、网络编解码、UI2 纯视觉模型与动作端口同步传递 `confirmable`；服务端事实仍受任务 UUID、版本、Task key 精确匹配约束。CodeGraph 复核 QuestRuntimeService、TerminalService、任务详情文档及网络影响链。
- Docker Gradle `:galaxy-quest-core:test :ui2-terminal:test :test` 通过。BQ 队伍共享、锁定任务、重复重置、实机点击和奖励交付未验证，203 项 Checkbox 仍为 `BLOCKED`；未部署、未修改生产数据库或切换现网任务。


### 2026-09-23 - BQ M.2 复选任务跨定义隔离

- 只读核对服务端任务定义与本地 BQ `TaskCheckbox`/`NetTaskCheckbox`：203 项均非重复、无额外 Task 参数且关闭自动领取；76 项所在任务设置 `globalShare`。BQ 用认证玩家和精确任务/Task 标识处理点击。
- CodeGraph 查询 `QuestEvaluationPlanner`、`QuestFactProjector`、终端动作链并复核影响范围，发现 Base 规划器原本仅按确认事实类型筛任务；不同任务共用 `task-0` 时可能串改。现将确认事实限制为精确任务 UUID、定义版本、Task key 及 Checkbox 类型，并增加两任务同 key、错误版本、错误 key、缺失 UUID 的回归测试。
- Docker Gradle `:galaxy-quest-core:test` 与 `:galaxy-quest-bq-importer:test` 通过；规划器测试 5 项、行为清单测试 2 项均零跳过、零失败。根 `test assemble verifyUi2SelfContained` 两次在 GTNH convention 插件初始化联网超时，未执行到构建任务，需后续补验；`git diff --check` 通过。终端玩家确认动作及队伍同步还未实现，类型仍为 `BLOCKED`；未修改生产数据、部署或切换现网任务。

### 2026-09-23 - BQ M.2 选择奖励只读对照

- 经用户授权，仅扫描 S2 `DefaultQuests` 的临时只读副本；未读玩家进度或数据库，未修改现网。957 项选择奖励均有 2–16 个选项，全部手动领取；10 项处于相对计时的重复任务，814 项含 Lootbag。
- 对照本地 BQ `RewardChoice`/`QuestInstance` 与 Base 选择、领取、投递仓储：BQ 重置重复任务时保留玩家旧选择，Base 新周期要求重选；强制领奖的 Lootbag 优先随机规则也尚未移植。记录在 M.2 行为矩阵，继续保持 `choice` 为 `BLOCKED`，未放开发布门槛。
- 补充重复选择奖励的 PostgreSQL 集成用例。CodeGraph 确认影响集中于奖励选择、领取仓储及 `QuestPostgresIntegrationTest`。Gradle 两次在 GTNH convention 插件联网初始化阶段超时，尚未进入测试；改用相同已编译依赖和 JUnit 4 在临时隔离 PostgreSQL 16 容器执行该测试类，20 项通过。临时容器已停止并自动移除；未接触生产数据库、服务端开关或部署。本轮仍需恢复正常 Gradle 全量验收。

日期：2026-03-29

这份文件用于记录 `JsirGalaxyBase` 的持续开发摘要。
从本次开始，后续每次实际代码变更都应补一条简要 work log。

### 2026-09-08 - 银河职业、贡献与跨服任务平台 Q.0 / Q.1 起步

- 源码与现场：固定调查 GTNH 当前 `BetterQuesting-3.7.15-GTNH` 源码提交 `524b365211b6b3a9672cab8ae45b4e07e726d49e`；确认 BQ 将定义、玩家进度、队伍、生命和名称分离保存，现场 S1/S2 分别有 40/29 个玩家进度文件且存在分歧，不能以单服 JSON 充当跨服真源。
- 方案：新增 `docs/galaxy-career-contribution-cross-server-quest-platform-2026-09-08.md`，明确吸收任务、编辑、检测、进度和奖励语义，但不搬 BQ GUI、网络、文件存储单例与 Mod 启动架构；阶段按 Q.0-Q.8 推进，最终由 PostgreSQL 和 UI2 终端接管。
- 实现：新增纯 Java 8 `galaxy-quest-core`，首批提供稳定 UUID 任务定义、Task/Reward 定义、AND/OR、前置门禁、单调进度合并、完成状态和稳定奖励资格键，以及编辑器的重复 key、未知类型、自依赖、缺失依赖和依赖环校验；继续加入跨服稳定事实键、Task evaluator 注册表及计数事实投影，为后续 Minecraft 检测器提供平台无关归约入口。
- 合规：记录 BetterQuesting MIT 许可证、固定版本和来源清单，并将许可证/来源纳入正式 JAR 资源；新任务内核没有 BetterQuesting 运行时依赖。
- 验证：CodeGraph 同步新增 22 个、修改 2 个源文件，`QuestEngine` 影响仅落在新内核及其定向测试；Docker Gradle 的 `:galaxy-quest-core:test`、根项目 `test`、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 全部通过。正式 JAR 已核验包含任务内核 class、BetterQuesting MIT 文本和来源清单，SHA256 为 `41d49725aa039f0afaf6527c79f5eff100184699faf035c66fea3cb21d1cbe32`；本批未部署、未修改数据库、未触碰 S1/S2 运行状态。
- Q.1 扩展：参与主体扩展为 PLAYER/PARTY/TEAM/PUBLIC，队伍事实按显式成员快照归入同一进度；重复任务使用 cycle 隔离进度与奖励资格，仓储端口要求事实去重、进度和奖励资格在事务中提交。新增定义版本身份保护，防止旧进度误套新定义。
- Q.2 起步：新增纯 Java `galaxy-quest-bq-importer`，只读解析 BQ typed NBT-JSON，保留完整原始 Task/Reward 配置，并导入玩家完成、领取和时间戳。实际扫描 S2 DefaultQuests 共 3744 个任务，仅使用 9 种 BQ Standard Task 和 4 种 Reward；导入器测试通过，不写现场文件或数据库。
- 本轮续验：Q.1/Q.2 定向测试、CodeGraph 同步与影响检查、根 `assemble`、`git diff --check` 均通过；加入 importer 子项目后的正式 JAR SHA256 为 `5d48cecbe3dac6feb74aaeb97cfd5d353afdde6aa7b4a0034b944de005a4d313`。Importer 是开发/迁移工具，当前没有打入运行 JAR，也没有引入 BetterQuesting 运行时依赖。

### 2026-09-08 - UI2 纯 Java视觉闭环 V.0

- 新增独立方案 `docs/galaxy-ui2-pure-java-visual-validation-2026-09-08.md`：自动化只负责越界、重叠、文本、裁剪、图表可见性和多尺寸等结构正确性，交互手感与主观视觉继续由人工验收。
- `UiDebugSnapshot` 可导出 UI2 组件树、最终边界、DrawList 及越界、文字高度、重复外部区域 ID 告警；UI Lab 无头导出扩展到 `350x193`、`427x240`、`620x340`，并生成 PNG、DrawList、`.ui2.txt` 和 HTML 画廊。
- UI2 Screen/Container 宿主增加只读调试捕获桥；现代终端页面按 F9 会在客户端 `screenshots/` 同时保存 Minecraft Framebuffer PNG 与 UI2 结构侧车文件，不自动操作、上传或改变业务状态。
- CodeGraph 在改动前同步完成；`UiLabExporter` 仅影响 Demo 测试，现代 Screen 宿主影响终端应用页和 UI Lab，`TerminalPageDocument` 影响 223 个符号，因此正式业务 Document 下沉拆到后续 V.1-V.4，避免本批形成根项目到 Demo 的反向依赖。
- 自动验收：Core 41、Lab 2、Demo 5、根项目 458 项（52 项按既有环境条件跳过），共 506 项零失败；三档 15 张 PNG 的结构审计均为 0 告警，`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过。运行 JAR SHA-256 为 `9d27e17c1e6504e8157388835660e681f057fb9cfe285a930f9d1d293bce4afb`，本轮未部署或启动客户端。

### 2026-09-06 - UI2 文字正确性、主题包与终端设置页

- 根据实机首页截图和 CodeGraph 调用链确认：正式 UI2 只有首页与资产中心，其他业务仍进入旧
  `TerminalHomeScreen`；首页窗口尺寸本身有效，爆版来自 35px 卡片承载三条 14px 行高文字、无换行、
  无省略且无裁剪，原有测试只检查窗口矩形而未检查文字命令。
- UI2 Core 新增测量驱动 `TextFlow`，支持中文逐字换行、显式换行、最大行数、Unicode 代理对安全截断和
  末行省略。生产 Label、按钮、徽章、状态页和 Dialog 统一使用主题字体 Token并在自身边界建立 Clip；
  首页改为紧凑两列卡片，并补三档验收尺寸的文字边界测试。
- 新增客户端 `TerminalPreferences`、`TerminalThemeRegistry` 与 `TerminalAppearance`，提供
  `glass_mono / hacker_green / high_contrast` 三套内置主题、三档密度、三档窗口尺寸、减少动画和 Tooltip
  延迟。偏好写入独立 `config/jsirgalaxybase-terminal-ui.cfg`，不进入服务器配置、网络或数据库。
- 新增 Resource Pack 数据主题入口；多个已启用资源包的 `ui2/themes/index.json` 可注册数据主题，坏条目
  单独忽略并回退内置主题，不允许主题携带代码或业务行为。
- 新增现代 `TerminalSettingsScreen/Document`，设置入口固定在共享导航底部；主题、密度、窗口和减少动画
  即时应用并持久化，原生物品槽保持固定逻辑尺寸。下一步继续建立统一非 Container 页面注册表并迁移
  通知、银行与 ServerTools，不用现代边框包装旧 Panel。
- CodeGraph 在修改前确认 `TerminalAppShell.layout` 仅有首页与资产两个调用者，旧主屏影响 212 个符号；
  修改中同步 32 个文件，并复核 `TextFlow`、新主题注册表和设置页影响链；CodeGraph 未自动推导测试，故按
  合同显式执行。Docker Gradle 最终通过根项目 525 项（52 项条件跳过）、UI2 Core 30 项、Lab 2 项、
  Demo 4 项，0 failure/0 error；`:ui2-demo:renderGolden`、`assemble`、`verifyUi2SelfContained` 与
  `git diff --check` 均通过。runtime JAR SHA-256 为
  `4e416c24eeb56d4f4f18cd1d8aa01c1d514015bcbe6608993f076cabf9d75826`。本批未部署、未启动客户端、
  未触碰 S1/S2。

### 2026-09-06 - UI2 终端窗口化、玻璃主题与首页同壳整改

- 根据资产中心实机截图将 C.2 标记为“业务可用、视觉未通过”：根因是 `ModernContainerHost` 仍以全屏
  viewport 承载生产文档、UI2 使用近不透明藏蓝主题，而资产返回由 `TerminalRouteCoordinator` 重建旧
  `TerminalHomeScreen`，形成全屏新页面与旧窗口首页之间的代际跳变。
- 新增共享 `TerminalWindowMetrics`：在完整 Minecraft 事件宿主内居中放置 `350×193` 至 `620×340`
  的响应式终端窗口；常规视口按 90% 宽、88% 高布局。资产 Vault、背包、Bay 与 Cell 外部区域继续使用
  原生 Slot Bridge，但全部限制在窗口内。
- 新增生产专用 `GlassTerminalTheme`，使用半透明黑色窗口、低透明中性表面、冷白文字和青绿色信号色；
  选中页签改为弱玻璃底、细强调边框及一像素指示线，导航改用紧凑文字样式，槽位颜色改由 Theme Token
  提供。F8 UI Lab 继续保留原工业主题，避免影响平台实验基线。
- 新增 `TerminalHomeScreenV2` 与 `TerminalHomeDocument`。终端首次进入首页、资产中心返回首页、旧页面
  点击首页均进入同一个 UI2 窗口壳；仓储入口继续由服务端 `VAULT_OPEN` 打开权威 Container，其他未迁移
  业务页保持旧路由。刷新、帮助 Modal、HUD 通知和通知目标路由一并接回。
- CodeGraph 同步 19 个文件；`callers TerminalAppShell.layout` 确认仅首页与资产使用新窗口合同，`impact`
  复核窗口、首页、路由与双宿主链路，`affected` 未自动推导测试，因此显式执行窗口、首页、资产定向测试
  及全量回归。Docker Gradle 的 Core/Lab/Demo、10 组 golden、根 `test`、`assemble`、
  `verifyUi2SelfContained` 全部通过，共 565 项、52 项既有条件跳过、0 failure、0 error；
  `git diff --check` 通过。
- 交付：同一 runtime JAR 已部署到 Lobby 与客户端，SHA-256 均为
  `d5a5f330f298a6d69810b651a8b8de0316ea99509f48371d7ffa2440484e313c`；Lobby 恢复为 `RUNNING`
  并在最终启动日志到达 `Done (1.271s)`。未启动客户端、未执行数据库 migration，未触碰 S1、S2。
  当前等待玩家实机确认窗口比例、透明度、导航密度和首页/资产往返观感。

### 2026-09-05 - UI2 实机圆角、槽位与字体首帧稳定性修正

- 根据 F8 UI Lab 实机截图定位三项根因：圆角抗锯齿带只向外扩造成边缘毛刺；Container
  方形槽位底板覆盖了 UI2 小圆角；Noto 字形异步上传前使用 9px 原版字体及其 advance，
  上传后才切换到 UI2 最终度量，产生约一秒后的文字放大。
- 圆角 coverage 改为数学边界内外各半个物理像素，hairline 始终为一个物理像素，细分环
  使用三角形并关闭面剔除；槽位视觉与原生 Slot Bridge 统一使用 16px/2px 间隔网格，
  Container 仅绘制原生物品和 Tooltip，不再覆盖方形底板。
- 字体回退按最终 Noto 字形宽高缩放并沿用最终 advance；可见文字先预取，最多 48 个字形的
  上传步骤从“每条文字”改为“一帧一次”，Container 的浮层二次绘制也不重复上传。
- 新增回退字体尺寸、物理像素圆角、共享槽位几何及 Slot 对齐测试。CodeGraph 同步 15 个
  改动文件并复核 Font、Renderer、双宿主和 Slot Bridge 影响链；`affected` 未自动推导测试，
  因此显式执行全部 UI2 与根项目回归。Docker Gradle 的 Core/Lab/Demo、10 组 golden、全量
  `test`、`assemble`、`verifyUi2SelfContained` 全部通过，共 543 项、52 项既有条件跳过、
  0 failure、0 error；`git diff --check` 通过。
- 交付：runtime JAR 与 Prism 客户端目标 SHA-256 均为
  `9ecd9956e92191cdf8e7e2eb0f068f4b50ce44c8b2bb6b32007e6cb6b8d4ee9c`。仅部署客户端且未启动，
  未触碰 Lobby、S1、S2；未修改正式终端业务页面、服务端合同或数据库。

### 2026-09-05 - Galaxy UI 2 Batch B.1-B.4 Qz 算法剪裁与渲染加固

- 边界：保留 Base 自研 UI2 Core、单向 Store、组件树、DrawList、双宿主和 Java2D 后端；仅按固定提交剪裁
  Qz 1.8.2/4.1.3-LTS/4.7/4.8 的成熟算法，不依赖、不调用、不打包 Qz Mod，也未修改整合包现有 Qz。
- 稳定性：Modal 以稳定 key 跨 Runtime 重建保留焦点和捕获，Effect 重入动作在一次 flush 内排队收敛并有
  1000 动作循环门禁。实机第四页崩溃定位为 Modal 两条文字误用了 CONTENT 层，现已统一为 MODAL 层并补
  打开、重建、Escape 关闭回归。
- 渲染：`MinecraftUiRenderer` 拆分 Shape/Text/Image/Clip/GL 状态后端；圆角改为半径限幅、物理密度动态细分、
  内部实色加一物理像素 coverage fringe，边框改用内外填充环，卡片/按钮圆角同步收敛。
- 字体：增加 generation 化字形请求去重、每帧 48 个上传预算、过期拒绝和 Atlas 上传失败整体回滚；UI Lab
  增加 generation、页数、队列、上传、DrawCommand、Clip、frame/render 时间诊断。
- UI Lab：浮层页现在同时展示 ContextMenu、Tooltip、Dialog 和 Toast 的真实层级，Dialog 仍位于普通浮层之上，
  Toast 位于反馈顶层；库存页继续沿用原生 Slot Bridge 和 Modal 阻断。
- 合规：新增独立设计、固定提交来源清单、MIT/LGPL 文本和第三方声明；`verifyUi2SelfContained` 扫描生产源码与
  JAR class，禁止 Qz 包、Mod ID、Mixin 或运行时引用，并验证许可证和 provenance 均进入 JAR。
- 验证：CodeGraph 同步 27 个改动文件并复核 Runtime、Renderer、Font 与双宿主影响链；`affected` 未推导出测试，
  因此按影响链显式执行 UI2 Core/Lab/Demo、Minecraft adapter 与全量回归。Docker Gradle 的 UI2 定向测试、
  Java2D 10 组 golden、全量 `test`、`assemble` 和自包含检查全部通过；合计 538 项，52 项既有环境条件跳过，
  0 failure、0 error。`git diff --check` 通过。
- 交付：runtime JAR 与 Prism 客户端目标的 SHA-256 均为
  `5cd41fbfab168dafbb4d3235823c54d7daa6ee335d712e1c4b586e8ef5311b95`；仅部署客户端且未启动，
  未部署 Lobby、S1、S2。正式资产中心和终端业务页面仍未迁移，需先完成人工 F8 UI Lab 验收。

### 2026-09-05 - 补齐 Galaxy UI 2 Batch B 的 Lobby 同版部署

- 现象：Batch B 首轮按计划只部署客户端后，Forge 握手检测到客户端 `3ca9376-main+3ca937653a-dirty` 与 Lobby 旧版 `073a5a6-main+073a5a622c-dirty` 不一致并以 Mod rejection 拒绝连接。
- 处理：使用既有灰度脚本将已经通过全量测试的同一运行 JAR 补充部署到 Lobby；客户端与 Lobby SHA-256 均为 `75cd26ff04d71e13d6410a4fb69a3b13b6550521c25457f806ff4e27c61ac05a`。
- 验证：Lobby supervisor 为 `RUNNING`，`latest.log` 已出现 `Done (1.609s)`，本机 `127.0.0.1:25566` 正常监听；JGB 的银行、市场、地产 SHADOW、Warehouse、Cluster 与 ServerTools 启动日志均正常。本次未触碰 S1、S2，未启动客户端。

### 2026-09-05 - 完成 Galaxy UI 2 Batch B Minecraft 双宿主 UI Lab

- 模块：新增纯 Java 8 `ui2-lab`，把概览、控件、数据、浮层和库存五页场景、Store 与假数据从 Java2D 后端抽离；`ui2-demo` 和 Minecraft 适配共同使用同一组件文档与 `UiRuntime`。
- 核心：补齐重建/对账/测量/布局/输入/DrawList/失效/卸载 Runtime，增加平台无关按键、文本度量、资源解析、渲染器、层级、渐变、阴影、图片和稳定外部区域合同。
- Minecraft：新增逻辑坐标 OpenGL renderer、scissor 换算、GL 状态保护、普通 `ModernScreenHost`、原生 `ModernContainerHost` 和 `NativeSlotBridge`。Container Lab 使用本地假库存并拦截窗口点击，不向服务端发送未知窗口操作。
- 字体：固定并内置 Noto Sans CJK SC 2.004（OFL 1.1），灰度字形可后台生成，纹理仅在渲染线程上传；Atlas 限 8 页、每页 1024x1024，支持代际取消、LRU 淘汰、资源重载和原版字体逐字形回退。
- 入口：注册客户端 `F8` UI2 Lab；`D` 切换调试层、`M` 切换 reduced-motion，Inventory 页用 `C` 进入双宿主 Slot 实验。没有接正式终端路由、业务网络或数据库。
- 验证：`:ui2-core:test`、`:ui2-lab:test`、`:ui2-demo:test`、`:ui2-demo:renderGolden`、根项目全量 `test` 与 `assemble` 通过，共 527 项测试、0 failure、0 error（52 项按既有条件跳过）。运行 JAR 内含 UI2 Core、Lab、Minecraft Adapter、固定字体和 OFL，Core/Lab 均为 Java 8 class 52；未包含 `ui2-demo`、Swing 或 Java2D Demo class。旧 Canvas 和资产中心保持不变。
- 交付：仅将同一运行 JAR 部署到 Prism 客户端 Mods，源文件与目标文件 SHA-256 均为 `75cd26ff04d71e13d6410a4fb69a3b13b6550521c25457f806ff4e27c61ac05a`；未启动客户端，未部署或重启 Lobby、S1、S2。

### 2026-09-05 - 完成 Galaxy UI 2 Batch A 独立内核与 Java2D UI Lab

- 构建：新增 `ui2-core`、`ui2-demo` Gradle 子项目。内核以标准 Java 8（class 52）独立编译，不依赖 Minecraft、Forge、LWJGL、AE2、ModularUI、BetterQuesting 或旧 Canvas；根项目编译使用 project dependency，正式 shadow/reobf JAR 通过内核 JAR 文件合入 49 个 UI2 class，Demo class 保持为 0。
- 内核：实现不可变 `UiElement`、稳定 `UiKey`、`UiNode`/reconciler、Component 工厂、Store/Reducer/Selector/Effect、约束几何、Row/Column/Stack/Grid/Scroll 布局、捕获/目标/冒泡输入、焦点、Pointer Capture、Modal、Theme Token、固定 Motion Clock 与平台无关 DrawList。
- Demo：Java2D UI Lab 提供概览、控件、数据、浮层、库存五页，支持 `350x193`、`620x340`、调试层和 reduced-motion；无头任务已生成 10 张 PNG 和 10 份结构化 DrawList，输出位于 `ui2-demo/build/ui-lab`。
- 验证：`:ui2-core:test`、`:ui2-demo:test`、`:ui2-demo:renderGolden`、根项目全量 `test` 与 `assemble` 均通过，`git diff --check` 通过；纯内核禁止依赖扫描通过。当前 Canvas、正式终端路由、市场、资产、地产和服务端合同均未修改，也未部署任何服务器或客户端。
- 构建经验：GTNH RFG 的 `shadowImplementation` 不能直接消费没有 MCP/deobfuscator attributes 的普通 Java project variant；最终采用 `implementation(project)` 加 `shadowImplementation(files(ui2CoreJar))`，既保持开发期类型依赖，又由正式发布链内嵌核心。

### 2026-09-05 - 固化 Galaxy UI 2 独立构建与 Demo 重构规格

- 主题：将“参考 Vue 逐步重建 UI 框架”收口为可执行规格，并明确与 Vue Web Runtime、当前 BetterQuesting 风格 Canvas 框架的差异。
- 构建决定：优先采用纯 Java 8 `ui2-core`、Java2D `ui2-demo` 和 Minecraft/Forge adapter 三层结构；核心必须能够独立编译和执行 JUnit，Demo 必须能够独立运行并导出两档尺寸 PNG/DrawList golden，根 Mod 只负责合入核心 class 和接平台能力。
- 测试边界：组件、Store、布局、输入、焦点、模态、Theme、DrawList 和 Motion 自动化；OpenGL 状态、字形 atlas、ItemRenderer、原生 Container/Slot 和实际观感保留游戏内 UI Lab 与资产中心人工验收。
- 迁移顺序：先做构建探针和纯内核，再做独立 Demo、Minecraft 双宿主、资产中心垂直迁移、普通终端页迁移，最后用 CodeGraph 确认零调用并删除旧框架；禁止未建立新基线前直接铲除现有 Canvas。

### 2026-09-05 - 固定 Vue 官方手册并建立 Galaxy UI 2 概念映射

- 主题：通过 GitHub SSH 拉取 Vue 官方英文文档与官方中文翻译的固定版本，建立 Vue 到 Java/Forge UI2 的工程映射。
- 来源：`vuejs/docs@b75d188ab16bf83bd1f364a77dfd2315be8f3fa4`、`vuejs-translations/docs-zh-cn@638fe472ad67f36399e7b73400c3a19ba6a461a2`；仅保留 Guide、API 与 Tutorial，文档许可为 CC BY 4.0。
- 结果：本地参考位于受忽略的 `Reference/VueDocs`，不会成为 Gradle 依赖或打入 JAR；新增 `galaxy-ui-2-vue-reference-map.md`，明确采用 Component/Props/Action/Slot/Store/Selector/Lifecycle/Key 等公开思想，自主实现 Java UI2，不移植 DOM、CSS、JavaScript Proxy、Vue Runtime 或模板编译器。
- 现状追溯：当前 Base GUI 由早期 ModularUI 2 迁移而来，现框架参考 BetterQuesting 的 Canvas/Panel/Theme 结构并基于 Minecraft `GuiScreen`/`GuiContainer` 自研；Forge 提供宿主与底层绘制能力，不提供当前的高级 Canvas 组件框架。

### 2026-09-01 - 银河仓储 v2：终端托管真实 Cell Bay 与统一资产中心

- 将后续主体验收收口为“终端提供一个 Bay，而非要求玩家摆放实体 Drive”：每位玩家在银河仓储页打开一格原版
  `Container`，可从背包拖放或 Shift 插入/取出一枚真实 AE2 Storage Cell。Cell 的完整 NBT 由 PostgreSQL
  保存，容量、物品/类型限制继续由 AE2 Cell 自身解释；不构造第二套物品账本。
- 新增 `warehouse_terminal_bay` 与 `warehouse_terminal_bay_operation` migration、JDBC 仓储和共享 JDBC schema
  校验。写入按本服/服务端 UUID/requestId/语义键/版本串行化，Cell 只能是一枚经 AE2 注册表认可的 Storage Cell；
  重放返回原回执，语义冲突被拒绝并保留前后快照审计。
- 银河终端改为统一资产中心：Base Vault、一个 Cell Bay 与最近操作在同一页展示；打开 Base Vault 或 Bay 是两个
  明确入口。页面不再把 Bay 描述成供电、频道或已接入外部 ME 网络，亦不列举或复制 Cell 内物品。
- 实体 `Warehouse Drive`、其已应用 migration 和方块 ID 保留为兼容内容，不新增配方、不迁移真实 Cell；生产库尚无
  Drive 登记。灰度已于 2026-09-02 应用纯新增 `20260901_001_add_terminal_warehouse_bay.sql`，同一 JAR 已复制到
  Lobby 与 Prism 客户端（客户端未启动）；Lobby 重启后完成 `Done`，共享 JDBC schema 校验通过，Bay 与操作审计表初始均为
  0 行。S1、S2 与 ServerUtilities 未触碰，旧 JAR 已备份到对应 deploy backup 目录。
- 自动验证：Docker 定向单测、终端页面路由测试和隔离 PostgreSQL schema 集成测试通过；覆盖 Bay 空槽、非 AE2
  物品拒绝、单格规范化、版本冲突、requestId 幂等/语义冲突、Drive/Bay 表隔离和 schema fail-fast。

### 2026-08-31 - AE2 银河仓储 v1：个人 Warehouse Drive、产权审计与终端状态

- 将原有未注册的 `WarehouseDriveTile` 尖峰升级为个人实体仓入口：注册 AE2 Drive 风格方块、Tile 和单一真实
  Cell Bay；普通右键不打开原生十格 Drive GUI，终端也不构造第二个 ME Terminal。
- 新增 `warehouse_drive` 与 `warehouse_drive_operation` migration。它们只保存 Drive 的位置、个人所有权、版本和
  服务端操作审计；所有 Cell 内容、容量、频道、供电与 Storage Grid 真相仍在 AE2。Base Vault 未实现
  `ICellContainer`，未接入 AE 网络。
- 放置、Cell 安装、空 Cell 取出和拆除都由共享 PostgreSQL 事务中的 UUID/本服/坐标/所有权检查授权；假玩家、
  非所有者、非空 Cell 拆除和爆炸不能绕过。侧向自动化不再暴露 Cell 槽。启用 `warehouseEnabled=true` 时缺 AE2、
  共享 JDBC 或 schema 会 fail-fast。
- 新增顶层“银河仓储”终端页，只读显示本人已登记 Drive、已加载状态、Cell、频道/供电、物品/类型摘要与近期审计；
  不主动加载区块、不列举 ME 物品、不跨服访问。
- 自动验证：新增 `WarehouseDriveServiceTest`、`WarehousePostgresIntegrationTest`，并回归单 Bay 与终端路由测试；
  Docker Gradle `compileJava`、`testClasses`、定向单测及隔离 PostgreSQL schema 集成测试通过。Warehouse Port、
  市场自动投递、组织仓和跨服 AE 网络均明确留在后续批次。
- 灰度：已应用 `20260831_003_add_ae2_warehouse_drive.sql`，构建产物、Lobby 与 Prism 客户端 JAR 的
  SHA-256 均为 `007cb3274b832f54f1633bc9c0659ead5e859204945bf87af30311e5bed0fbb0`。Lobby 先以
  `warehouseEnabled=false` 成功启动，再切换为 `true`；日志确认个人 Warehouse Drive runtime 已使用
  `galaxy_gtnh_lobby` 的共享 JDBC 就绪。客户端未启动，未创建 Drive，S1、S2 与 ServerUtilities 未触碰。

### 2026-08-31 - 系统 Warp 与服务器目录：受审计维护入口

- 新增 `/jgbst admin setwarp|delwarp|warp-enabled|server-enabled`。裸 `/setwarp` 与 `/delwarp` 暂不注册，
  避免与仍保留的 ServerUtilities 冲突；普通玩家仍仅能浏览和使用系统 Warp。
- 管理入口要求原版命令等级 2，维护坐标和服务器身份只从服务端在线操作者注入。Warp 仅能在维护者当前服设置，
  跨服目标需在目标服设置；不能把客户端坐标或远程服位置写入数据库。
- 新增 migration `20260831_001_add_servertools_admin_audit.sql` 和 `servertools_admin_operation`：Warp 写入、
  删除、启停以及目录服务器启停与操作者/前后快照审计行在同一 PostgreSQL 事务中提交。审计 schema 缺失只会拒绝
  管理动作，不破坏既有玩家传送链。
- 自动验证：新增 `ServerToolsAdminServiceTest`，并扩展隔离 PostgreSQL `ServerToolsPostgresIntegrationTest`
  覆盖审计行、Warp 启停和服务器目录启停；定向 Docker Gradle 回归通过。

### 2026-08-31 - 终端通知中心页面 v1：筛选、定位与有界玩家快照

- 影响范围：`TerminalPlayerNotificationCenter`、`TerminalNotificationFeed`、终端页面快照/网络编解码、
  `TerminalNotificationCenterSection` 与对应定向测试。
- 新增顶层 `通知 / 消息中心`；市场恢复、跨服传送、地产 `SHADOW` 与银行等来源继续各自拥有业务真源，
  通知中心仅保存当前玩家的 40 条有界展示索引，不建立第二套订单、票据、产权或账本。
- 同一来源/目标/内容的通知合并为出现次数；普通 INFO 刷新不进入历史。全量通知页快照不会被 HUD
  当作新的 Toast 重播，Toast 仍只读取本次动作反馈。
- 页面支持来源和严重等级循环筛选、固定 6 条分页，以及点击记录复用既有受控页面定位；不发送玩家、
  订单所有者、坐标或客户端筛选状态给服务端。
- 验证：Docker Gradle 定向 `TerminalNotificationFeedTest`、`TerminalPlayerNotificationCenterTest`、
  `TerminalNotificationCenterStateTest`、`TerminalNotificationPacketTest`、`TerminalServiceTest` 通过。

### 2026-08-31 - ServerTools 终端 TPA 页面 v1

- `群组服传送` 页新增服务端快照驱动的 TPA 表单和近期收发件箱：显式目标服务器发起、收到请求接受/拒绝、
  已发待确认请求取消，以及 `PENDING / ACCEPTED / DECLINED / CANCELLED / EXPIRED` 状态展示。
- 新增版本化五段 `TerminalServerToolsActionPayload`，同时兼容旧 Warp、快捷动作和 Home payload。TPA payload
  只包含另一位玩家和目标服；服务端继续从 `EntityPlayerMP` 与本服配置注入身份，所有动作都复用既有
  `PlayerTeleportService`、PostgreSQL 条件状态更新和接受后源服 ticket 派发链。
- 快照与网络模型只传当前玩家自身的请求方向、对手名、目标服和状态；不向客户端发送 UUID、坐标、
  接受者落点或其他玩家的 TPA。写动作均经过终端精确确认框，最终由服务端重验。
- 自动验证：Docker Gradle `compileJava compileTestJava`、`TerminalServiceTest` 与
  `TerminalServerToolsSectionPacketTest` 通过；CodeGraph 同步后复核终端 section、壳、网络与 runtime bridge，
  Docker Gradle 全量 `test`、`assemble` 和 `git diff --check` 均通过。
- 灰度：同一 runtime JAR SHA-256 `13614be469755ab7420b930a9d1f79be456103075c1389f4d7b665b73b4c5b26`
  已同步到构建产物、Lobby 与 Prism 客户端；Lobby 本次启动到达 `Done (1.153s)!`。客户端未启动，
  S1、S2、ServerUtilities 和 PostgreSQL schema 均未修改。

### 2026-08-31 - 市场恢复扫描与 PostgreSQL 回归审计收口

- 严格只读市场审计定位到 4 条 8 月旧 `BUY_ORDER_CREATE` 恢复记录：关联订单、正式冻结银行流水和返还流水均已不存在，玩家账户冻结额为 `0`；旧版本曾错误地把同账户解冻当作账户间转账，之后每次启动又因“冻结余额不足”在第一条记录中止整批恢复扫描。
- `MarketRecoveryService` 现在仅在存在关联订单或正式冻结流水时执行资金返还；若两者都不存在则写入明确的
  `NO_PERSISTED_FREEZE_OR_ORDER` 审计结论并安全收口，不会从玩家账户的聚合冻结余额中猜测或释放其他订单的资金。
  已存在正式返还流水的重复扫描仍只重放原结论，不会二次返还。
- 修正 `MarketPostgresIntegrationTest` 的隔离 JVM 夹具：定制市场快照不再依赖未启动 Forge 时没有 ItemBlock 的
  `Blocks.stone`，改用本地 `Item` 夹具，实际 PostgreSQL 的发布、购买、领取路径重新可执行。
- 验证：CodeGraph 复核恢复链及其受影响测试；`StandardizedSpotMarketServiceTest`、Docker Gradle 全量
  `test`、`assemble`、`git diff --check` 通过；真实 PostgreSQL 隔离 schema 的银行、市场、地产、物品策略、
  ServerTools 五组集成测试通过（不写生产业务数据）。部署后 Lobby 启动扫描已收口 4 条旧记录，
  `scripts/market-audit.sh --strict` 返回 `anomaly_count=0`。
- 灰度：runtime JAR SHA-256 `20a0f5045c2f1eafc18162dc91e628bed19e5c08a7676d6d26edee1fa490df1f`
  已同步到构建产物、Lobby 与 Prism 客户端，Lobby 到达 `Done (1.155s)!`；客户端未启动，S1/S2 和
  ServerUtilities 未触碰。

### 2026-08-30 - 定制市场与汇率市场规则收口

- 复核并固化定制市场的独立 `listing / snapshot / trade / audit` 链：发布、浏览、购买、取消、待领取、
  Base Vault 投递、未知交付锁定和管理员受审计恢复均不复用标准商品订单簿；单元与 PostgreSQL 回归覆盖
  发布、成交、领取、投递失败、完成写入失败、手工恢复及 requestId 语义冲突。
- 汇率市场将规则版本升级为 `exchange-taskcoin-v1-stack64`：任务书硬币报价与执行均由服务端限制为
  单个 Base Vault 物品栈（64 枚），超限返回结构化 `EXCHANGE_INPUT_QUANTITY_LIMIT`；终端提交改为实际按所选
  Vault 格完整数量结算，修复报价按整叠但旧实现只扣一枚的不一致。
- `ExchangeMarketService` 以本服配置身份产生报价与账本，执行拒绝外部来源服伪造；银行兑换审计新增最大输入量，
  储备不足不写半条事务，重复 requestId 重放原交易而不再次扣减储备。新增单元与 PostgreSQL 集成回归覆盖这些边界。
- 本批不引入市场化汇率、手续费、日/周期额度、自动补储备或第二套兑换账本；固定规则的后续经济调整必须以新规则版本
  进入独立评审。
- 验证：CodeGraph 复核 `CustomMarketService`、`ExchangeMarketService`、终端执行入口与受影响测试；定向服务回归、
  实际 PostgreSQL 隔离 schema 的银行/市场集成测试、Docker Gradle 全量 `test`、`assemble` 与 `git diff --check`
  均通过。集成测试还发现并修复规则键超过既有 `VARCHAR(32)` 的回滚问题；测试不会写入生产业务 schema。
- 灰度：同一 JAR（SHA-256 `f485ee269b878dff30b70d322ae206bab8a1fc7d218f5630ab595df823edbf69`）已部署 Lobby
  与 Prism 客户端，三处哈希一致；无新增 migration。Lobby 到达 `Done (1.209s)!`，客户端未启动，S1/S2 与
  ServerUtilities 未触碰。

### 2026-08-30 - AE2 Warehouse Phase 0：单 Cell Bay 技术尖峰

- 新增仅供技术验证的 `WarehouseDriveTile` 和 `Ae2WarehouseDriveProbe`：直接复用 GTNH AE2 的真实 Cell、
  频道、供电、容量与 `Actionable.SIMULATE` 语义，限制为一个 Cell Bay，不复制 Cell 内容进入 JGB 数据库。
- 新增纯 Java 单 Bay 状态机与定向测试，覆盖非法/重复插拔、非空 Cell 拆卸拒绝、无频道或无供电拒绝、部分容量与
  模拟操作不改变已存数量。
- 该尖峰没有注册 Block 或 GUI，也不接市场、Base Vault、跨服 ME、账户归属、组织权限或 Warehouse Port；防止
  未完成拆装权限和审计前向玩家暴露半成品实体仓。
- AE2 使用 `compileOnly` 依赖且不被打包；新增 LGPL 来源说明与随 JAR 打包的许可证。Docker Gradle 全量
  `test`、`assemble`、AE2 未打包断言与 `git diff --check` 均通过。
- 灰度：同一 JAR（SHA-256 `e7f79a2a3c9d358eb08ac73b6834e4651dae6ec18b04edbfb155bf41b504f035`）已部署 Lobby
  与 Prism 客户端，三处哈希一致；Lobby 到达 `Done (1.163s)!`，客户端未启动，S1/S2、市场交付和现有
  ServerUtilities 未触碰。

### 2026-08-30 - BanItem 物品准入策略核心

- 新增服务端纯策略模块：以真实 `ItemStack` 注册名和 metadata 匹配严格配置的 deny rule，范围覆盖标准市场
  托管、定制挂牌托管、Base Vault 存入，并为地产自动化预留范围；默认关闭且不附带任何禁用表。
- 标准市场与定制挂牌均在从 Vault 取物前判定；Base Vault 原生容器仅在新增/增加物品提交前判定。拒绝会保留
  原库存、写入 `item_policy_audit`，不会扫描、删除或改写既有物品。
- 新增 `20260830_001_add_item_policy_audit.sql`，启用时要求 InstitutionCore 的共享 PostgreSQL 连接、完整表和
  索引，否则 fail-fast，禁止未审计的静默降级。
- 自动验证覆盖规则解析、范围、metadata、配置错误、先审计后拒绝、Vault/终端配置回归；PostgreSQL 集成测试
  覆盖缺表 fail-fast 与审计落库（仅在注入 `JGB_BANKING_IT_*` 时执行）。Docker Gradle 全量 `test` 为 437 项、
  0 失败、43 项按环境跳过；`assemble` 和 `git diff --check` 均通过。
- 灰度：`20260830_001_add_item_policy_audit.sql` 已应用；同一 JAR（SHA-256
  `004113cadd53db5a1483fe84d1b51442629ec372439f09d9481c83625cf6a02e`）已部署 Lobby 与 Prism 客户端，三处
  哈希一致。Lobby 到达 `Done (1.229s)!`，日志确认策略为默认 disabled；客户端未启动，S1/S2 和
  ServerUtilities 未触碰。

### 2026-08-30 - 终端统一通知中心：结构化来源与跳转合同

- 将终端通知从“标题/正文/级别”扩展为结构化来源、目标页面和目标记录；市场恢复、跨服 ticket 与地产
  `SHADOW` 可分别标识为 `market-recovery`、`transfer-ticket`、`land-shadow`，客户端点击优先按合同跳转，
  旧通知仍保留文本推断回退。
- 新增纯 Java、有界 `TerminalNotificationFeed`：同键合并计数、最新优先、每页最多 12 条、总量最多 40 条；
  供后续终端通知页和服务端事件来源复用，不创建第二套聊天或资产状态系统。
- 地产 `SHADOW` 对真实玩家的拒绝判断会记录最新观察事项，继续不取消事件；终端地产页可显示并定位该观察记录。
- 自动验证覆盖通知网络 round-trip、来源/目标保真、分页/限频语义及 SHADOW 观察记录；Docker Gradle 全量
  `test`、`assemble` 与 `git diff --check` 均通过。该能力随本次后续 JAR 继续部署于 Lobby 与客户端，客户端
  未启动，Lobby 已到达 `Done`。

### 2026-08-30 - ServerTools 终端 1.1：命名 Home 管理与跨服详情

- 群组服传送页新增当前玩家的命名 Home 列表、目标服/维度/坐标详情、名称输入、设定、删除和前往动作；
  所有写操作与传送均经过确认弹窗。
- `TerminalServerToolsActionPayload` 保持一段 Warp、两段 Warp/快捷动作兼容解码，第三段追加 Home 名称；
  终端快照和两套网络包同步传递 Home 列表及选中详情。
- 服务端始终从在线玩家构造 `TeleportActor`；设定/删除复用 `PlayerTeleportService`，前往复用
  `prepareHomeTeleport -> Cluster ticket -> 目标服恢复`。不新增 Home 表、坐标客户端信任或另一套传送系统。
- 自动验证：覆盖旧 payload 兼容、原始网络 `ByteBuf` round-trip、Home 跨服计划派发、设定/删除运行时桥接和
  终端滚动布局；Docker Gradle 全量 `test`、`assemble` 与 `git diff --check` 均通过。
- 灰度：同一 runtime JAR（SHA-256 `1f27da49d27112f34cdd73e077acd92b6a52aa11bcf1d50e5f3a080b35f41240`）已部署到 Lobby
  与 Prism 客户端，三处哈希一致；客户端未启动。Lobby 已到达 `Done (1.530s)!`，S1/S2 未触碰。

### 2026-08-28 - ServerTools 终端快捷传送 1.0

- 复核后确认终端已有“服务器目录 + Warp + 最近 ticket”页面，本轮不重复创建页面；在既有右侧工作区
  增加“回家 / 返回 / 出生点”快捷操作，并保留 Warp 选择与精确确认弹窗。
- 快捷操作经版本化 `TerminalServerToolsActionPayload` 进入服务端，分别复用默认 `home`、`back`、
  `spawn` 的既有 `PlayerTeleportService -> ClusterTeleportService -> ticket/Connect -> 目标服恢复` 主链，
  没有新增客户端身份、数据库表或第二套传送实现。旧单段 Warp payload 继续兼容解码。
- 本版明确不在终端伪造家园名称输入、家园列表删除或 RTP 安全选点；它们保留命令入口，后续应作为
  ServerTools 页面 1.1 的独立交互批次，而不是把随机落点逻辑复制进 UI。
- 验证与部署：CodeGraph 复核 `TerminalService`、payload、终端壳与 ServerTools runtime bridge 的影响链；
  定向终端测试、全量 `test`、`assemble` 与 `git diff --check` 通过。JAR SHA-256 为
  `b79ff0648c24206e951b03b90aa1339d0094c6af5ecdb8708afa6e8b84d81a62`，已部署 Lobby 与客户端，
  客户端未启动；Lobby 到达 `Done (1.345s)!`。S1、S2 与数据库未修改。

### 2026-08-28 - 无真人跨服 TPA 传送模拟回归

- 将 ServerTools 已接受 TPA 的“源服扫描、计划构造、进程内重复抑制”抽为纯 Java
  `AcceptedTpaSourceDispatcher`；Forge runtime controller 仅提供真实在线连接并调用该调度器，
  没有向生产环境加入假人、机器人或第二套传送链。
- 新增无真人端到端模拟：保留真实 TPA 服务、Cluster ticket 服务与目标服恢复服务，以受控 gateway 和
  本服传送执行器替代网络与客户端，验证目标接受落点、源服派发、确定性 requestId、重复 tick、重启后
  ticket 幂等、目标恢复完成、源服隔离与 30 秒超时边界。
- 该回归不能替代在线模式下的 Velocity/Bungee 实际切服、Forge 重连和真实玩家落点验收；这些仍等待
  S2 的既有世界文件启动问题解决后再由真人账号执行。
- 验证与部署：CodeGraph `impact/affected` 复核调度器、Forge 控制器和模拟链；定向模拟回归、全量
  `test`、`assemble` 与 `git diff --check` 通过。JAR SHA-256 为
  `2fe8ae4b4c58a3682b6f30b0e2786fd06b2937289a736689cee2650e3cf1dd2c`，已部署到 Lobby 与客户端，
  客户端未启动；Lobby 到达 `Done (1.252s)!` 且 TPA runtime controller 已注册。S1、S2 和数据库均未改动。

### 2026-08-28 - ServerTools 跨服 TPA 闭环与 SU 兼容命令

- 新增 `/sethome`、`/delhome`、`/tpaccept`、`/tpdeny`；保留既有 `/home set/delete` 与
  `/tpa accept/deny`，新增 `/tpa cancel` 和 `/tpa status`。
- `player_tpa_request` 增加接受者落点与 `DECLINED/CANCELLED` 状态；新增 migration 以条件更新保证
  接受、拒绝、取消竞争时仅一个状态迁移成功，并增加目标提示、源服派发与近期查询索引。
- 新增 ServerTools tick/login 控制器：目标服只记录接受者落点，源服在请求者在线、仍在原服且未超时
  时以确定性 requestId 派发现有 cluster ticket，修复跨服接受时目标服没有请求者连接的问题。
- ServerTools 及 TPA 玩家反馈改为中英文翻译键；PostgreSQL 真源、代理 Connect、目标服落点恢复链
  保持不变，未引入 Redis、Presence 或第二套传送系统。
- 自动验证：CodeGraph 复核服务、模块、JDBC 仓储和测试影响链；新增兼容命令、状态机、超时、并发、
  migration 升级、schema fail-fast 与源服隔离回归；Docker Gradle `test`、`assemble` 与
  `git diff --check` 均通过。
- 灰度：已将同一 JAR（SHA-256 `2c3e283dc49009f55810f4ea1211e9b47aa4c1b7037f82ab7be99ece5f8be786`）
  部署至 Lobby、S2 与客户端（未启动客户端），并成功应用
  `20260828_001_expand_tpa_request_lifecycle.sql`。Lobby 正常启动且 TPA runtime controller 已注册；
  S2 因既有 `World/level.dat`/`level.dat_old` 的 ZLIB 损坏退出，未对该世界文件作任何处理。

### 2026-08-28 - ServerUtilities 跨服指令架构与开源实现复核

- 使用 CodeGraph 复核 `PlayerTeleportService -> ClusterTeleportService -> BungeeCordGatewayAdapter
  -> PlayerArrivalRestoreService`：现有 PostgreSQL 数据、幂等 ticket、代理切服和目标服落点恢复已构成
  可恢复的跨服主链，不需要重写第二套传送系统。
- 对照 ServerUtilities 源码确认应吸收的是 `sethome/delhome/tpaccept/tpdeny` 等玩家入口和
  warmup/cooldown 体验，不引入其单服 Universe、NBT 数据或旧网络宿主。
- 对照 HuskHomes、Velocity/BungeeCord 官方文档和 Spigot 社区实现：共享 SQL 与实时消息 broker
  应分层；插件消息受在线玩家载体限制，Redis 适合后续低延迟 Presence/通知但不是当前前置条件。
- 将下一实施批收敛为兼容命令、TPA 拒绝/取消/查询/过期、目标服数据库轮询提示、本地化和状态并发
  测试；自动找服、Redis/代理消息总线、全局 spawn/RTP、warmup/cooldown、收费与 death-back
  进入后续优化池。
- 本轮仅更新已有设计与命令文档，没有修改 Java、数据库、配置或部署产物；未启动客户端，未触碰
  Lobby、S1、S2 和 ServerUtilities JAR。

### 2026-08-27 - 个人地产 C.2 高 GUI Scale 视觉比例修正

- 根据 1680x840 实机截图反推约 350x193 逻辑工作区，修正原先固定最小尺寸在 GUI Scale 4 下
  被放大的问题：顶部业务区压缩到 32px，地图恢复到至少 74% 内容宽度，检查器收敛到约 24%。
- 地图工具栏由 22px 缩为 13px，底部状态条同步压缩；悬浮卡改用地图相对宽度，不再使用
  170px 硬下限。
- 玩家皮肤定位针取消 34px 最小值，改为随区块尺寸计算且不覆盖多个区块；头像、针体和朝向箭头
  保持同一缩放关系。
- 检查器拆分所有者、产权、保护、地形与四类市场占位信息，修复中文界面显示字面量 `\n`。
- 本轮未改地产网络合同、PostgreSQL、产权数据、认领安全规则和价格能力；Lobby 继续保持
  `SHADOW`，ServerUtilities 保留。
- 验证与部署：CodeGraph `query/impact/affected` 将回归范围收敛到地产 section、地图输入与布局测试；
  Docker Gradle 定向测试和完整 `test` 均通过，全量为 413 项、0 失败、37 项按环境跳过，
  `assemble`、语言文件换行检查和 `git diff --check` 通过。构建、Lobby 与客户端 JAR SHA-256
  均为 `a44faaf8661d8e2a3db3cf9f33205b2011ee7d3b5ce84a35ba225a76a1fa094e`。
- Lobby 本次启动到达 `Done (1.409s)!`，地产运行时为
  `SHADOW / activeTitles=1 / fakePlayersAllowed=false`；Lobby 与客户端的 ServerUtilities 2.2.2
  均保留。未启动客户端，未触碰 S1/S2。

### 2026-08-24 - 个人地产 Batch C.1 真实区块地图

- 用专用地图画布替换 225 个普通按钮：客户端只读取已加载区块并生成 16x16 地形瓦片，
  叠加本人/他人/保留区产权色、外边界、选择框和玩家方向标记；未知区块明确显示斜纹。
- 新增三级缩放、滚轮、拖拽平移、定位与刷新；视口最多偏离玩家 16 区块，远级最大 31x31。
  地形使用内存 LRU 和单后台采样线程，OpenGL 纹理只在渲染线程创建、上传和释放，不写磁盘。
- 地产网络合同改为受限视口与稀疏产权单元，兼容旧六段 action payload；越界认领返回
  `OUT_OF_RANGE`，不会错误认领脚下区块。认领范围仍是玩家实时位置周围 15x15。
- 来源与许可：地形采样参考 `Reference/ServerUtilities` 的 `ThreadReloadChunkSelector`，派生
  文件保留 LGPL-3.0-or-later SPDX；SU 仍是参考源码和灰度期并存 Mod，不成为 JGB 运行依赖。
- 自动化：Docker Gradle 全量 `test` 已通过，覆盖负坐标、视口限幅、缩放锚点、旧payload、
  稀疏产权、保留区、越界认领、网络往返、布局和地形明暗。人工地图观感与保护事件矩阵后置。
- 验证与部署：全量测试共 404 项、0 失败、37 项按环境跳过；另行注入现有测试数据库配置后，
  PostgreSQL 地产集成测试 5/5 通过。`assemble` 与 `git diff --check` 通过，最终 JAR SHA-256 为
  `6d23dd948b028352f581b50ee56324b34bc7fda177edd4f049f4fe8b4e94f76d`。
- 同一 JAR 已部署到 Lobby 与客户端，客户端未启动，S1/S2 未修改。Lobby 到达
  `Done (1.154s)!`，地产运行时为 `SHADOW / activeTitles=0 / fakePlayersAllowed=false`，Lobby 和
  客户端的 ServerUtilities 2.2.2 均保留。启动日志仍有本批无关的既有市场恢复警告
  `insufficient frozen balance`，不影响地产运行时就绪，但应在市场恢复账本任务中继续处理。

### 2026-08-23 - 个人地皮 Batch C 终端闭环与自动验证

- 新增独立顶层 `PROPERTY / 地产` 页面，首版提供“附近地皮 / 我的地皮”：附近页使用服务端当前玩家位置生成 15x15 网格，我的地皮使用服务端计数与分页，并显示选中产权、版本、配额和 SHADOW/ENFORCE 状态。
- 新增结构化地产动作载荷、快照、客户端模型和网络编解码；客户端不能提交玩家 UUID、本服 ID或维度，服务端从在线玩家和当前服运行时注入身份。任意远程空区块会被收敛回当前网格，只有已证实属于本人的产权可从“我的地皮”跨距离选中。
- 认领与放弃使用精确确认框、唯一 requestId 和 expectedVersion；服务端继续复用 Batch B 的归属、配额、禁用维度、保留区、幂等与版本校验，成功后返回最新保护快照。
- 新增中英文地产 UI 文案、负坐标/15x15 映射、越界选择、认领放弃、11 条/每页 4 条、载荷边界、网络往返和专用页面边界测试。
- 修复灰度部署脚本可能接受旧 `Done (` 日志的问题：每次重启创建时间标记，只接受本次重启后更新的日志；新增独立脚本回归。
- 人工 Forge 事件矩阵仍后置；Lobby 必须保持 `SHADOW`，ServerUtilities 继续保留，S1/S2 不在本批部署范围。
- 验证与部署：CodeGraph 查询/调用链/impact 确认影响集中在终端快照、网络编解码、页面模型、专用 section 和地皮服务；Docker Gradle 全量为 396 项、0 失败、37 项按环境跳过，5 项 PostgreSQL 地皮集成测试另行强制执行通过，`assemble`、`git diff --check`、语言键一致性与部署脚本回归通过。
- 构建、Lobby 与客户端 JAR SHA-256 均为 `bcb8136e7febc625900c426946e2ae86d5e0e956a21fd4c8db9b881c083854b0`；Lobby 本次到达 `Done (1.205s)!`，地产运行时为 `SHADOW / activeTitles=0 / fakePlayersAllowed=false`，数据库仍为 0 条产权和 0 条操作。客户端未启动，ServerUtilities 在 Lobby 和客户端均保留。

### 2026-08-22 - 个人地皮 Batch B PostgreSQL 与保护执行层

- 新增个人产权与操作日志 migration；产权按 server/dimension/chunk 唯一，放弃改为 `REVOKED + version++`，操作保存结构化前后快照。
- 新增共享 JDBC 仓储、事务执行器和 schema fail-fast；请求、玩家配额与区块使用 advisory lock，服务重启可从 PostgreSQL 重建本服保护快照，失败事务不会留下半条产权。
- 增加服务端 land 配置和严格解析；`LandModule` 不再创建内存正式运行时，只在 dedicated server 启用后复用 Institution Core 的共享连接。
- 增加 SHADOW/ENFORCE 保护层，覆盖破坏、放置、交互、非玩家实体攻击、爆炸方块和假玩家；PvP 不变，假玩家默认拒绝，日志与玩家提示限频。
- 本批没有命令、终端地产页、挂牌或交易。Lobby 已进入 SHADOW 并继续保留 ServerUtilities；启动日志确认 `activeTitles=0`、`fakePlayersAllowed=false`，数据库产权与操作表均为 0 行。
- 验证与部署：`git diff --check`、Docker Gradle 地皮定向测试、5 项真实 PostgreSQL 隔离 schema 测试、常规全量 `test` 与 `assemble` 通过；全量为 385 项、0 失败、37 项按环境跳过。强制开启全部 PostgreSQL 集成测试时另发现 1 个既有银行 missing-schema 错误文案断言不匹配，本批未改无关银行合同。
- migration `20260822_001_add_personal_land.sql` 已应用；最终构建、Lobby 与客户端 JAR SHA-256 均为 `659bd21cc5e744038837baaba29c841d0660da049519cebe2a6ff52025787494`。Lobby 到达 `Done (1.380s)!`，客户端未启动；S1/S2、旧 Team claim 均未修改。

### 2026-08-22 - 完成个人地皮 Batch A 代码尖峰

- 新增并注册 `LandModule`，落地跨服区块键、版本化个人产权、状态/结果枚举、结构化动作回执，以及个人认领和放弃服务。
- 认领支持 `requestId + semanticsKey` 幂等、单人上限、禁用维度和保留区块；放弃时重新校验所有者、版本和产权状态。新增可从仓储重建、以不可变快照发布的 `LandProtectionIndex`。
- 当前只使用内存仓储，默认上限 4 是测试占位；尚未接 PostgreSQL、Forge 保护事件、终端 PROPERTY 页面、银行地产交易或旧 SU 数据迁移，因此不能视为游戏内功能已经上线。
- 补齐 `THIRD_PARTY_NOTICES.md`、派生源码 SPDX/来源说明和发布资源打包，修正资源目录 MIT 许可证的占位版权。构建 JAR 已确认包含第三方声明与 ServerUtilities LGPL 全文。
- 验证：`git diff --check`、Docker Gradle 地皮定向测试和 `assemble` 通过。本轮未部署、未删除 ServerUtilities JAR，也未触碰 S1；既有市场弹窗工作区修改保持原样。

### 2026-08-22 - 个人地皮优先与 ServerUtilities 代码复用评估

- 主题：在确认可沿用 LGPL-3.0-or-later 后，评估 ServerUtilities claim 源码可复制范围，并把首轮范围收紧为个人认领、个人挂牌和个人购买。
- 源码结论：SU 全仓 592 个 Java 文件、约 54,091 行；claim 数据、命令、网络和旧 GUI 候选面约 2,317 行。区块坐标、claim 状态、内存索引、Forge chunk ticket、保护事件与地图算法有复用价值；TeamData、Universe、PermissionAPI、旧 wrapper、旧 GUI 和命令宿主不进入个人首版。
- 架构结论：只新增一个内部 `LandModule`，不把保护、产权、交易拆成多个 Mod；PostgreSQL 是产权/挂牌/审计真源，本服内存 map 是可重建保护索引。
- 交易与 UI：个人地产借鉴定制市场的 requestId、row lock、状态机、共享事务和审计，但无 ItemStack/待领取；终端新增 `PROPERTY` 页，使用“附近地皮 / 我的地皮 / 在售地产 / 交易记录”，MARKET 仅作为路由入口。
- 迁移边界：旧 S1/S2 claim 属于 team，组织关系未定前只进入 migration staging，不自动归个人；个人版可先在无旧 claim 的隔离环境或 Lobby 验证无 SU 独立运行，S1/S2 删除后置。
- 文档：新增 `serverutilities-personal-land-code-reuse-evaluation-v1.md`，同步修订地产总设计、映射表、产品方向与文档入口。本轮没有复制生产代码、改数据库、删除 JAR、部署或触碰 S1。

### 2026-08-22 - 修正 ServerUtilities 最终形态并补齐地产制度关系

- 主题：纠正“长期保留 ServerUtilities 保护适配器”的错误理解，明确把所需能力重构进 BaseMod，迁移验收后删除独立 ServerUtilities。
- 文档核对：复查项目定位、银行账户/账本、三类市场、定制挂牌、ServerTools 权限预留、终端职业占位及 ServerUtilities 映射；仓库文档引用的旧 `../../Docs/设定.md` 等外部策划文件当前实际路径不存在，因此未把这些历史引用当成现行事实。
- 设计结果：JGB 原生 PostgreSQL 产权与本服内存保护索引是唯一正式运行态；ServerUtilities 只用于冻结参考行为和一次性只读导出/导入，不得成为编译、启动、页面或保护判断依赖。
- 制度关系：Team 是组织、共同产权和 Team 银行账户的主体；职业是个人规则输入，不是 Team 岗位或产权主体；个人/Team 资金统一走银行账本；地产使用独立固定价挂牌和产权过户，不进入标准商品订单簿；BQ/职业后续通过事实和版本化规则接入。
- 最小路线：原生个人 claim → GTNH 保护矩阵 → 最小 Team → 迁移演练与灰度卸载 → 固定价地产交易；本轮不修改代码、数据库、服务器配置或任何 JAR，不触碰 S1。

### 2026-08-22 - 修正 ServerUtilities 集成状态并建立领地/地产路线

- 修正此前判断：JGB 只吸收了 `home/back/spawn/warp/tpa/rtp`、跨服 ticket 与终端传送页，并未完成 ServerUtilities 的 claim、chunk loading、team/rank、领地地图或其他工具集成。
- 实时只读检查确认 Lobby、S1、S2 与客户端均有 ServerUtilities 2.2.2，三服 claim/protection/chunk-loading 配置启用；S1 有 23 个、S2 有 16 个现存团队 claim 数据文件，Lobby 为 0。本轮不修改配置或 `.dat`，不触碰 S1。
- 新增 `serverutilities-land-property-integration-v1.md`：把领地保护、产权登记和地产交易分开。该条最初写成运行时只读 Adapter 路线，已由同日上方“修正 ServerUtilities 最终形态”记录废止；现行路线是 JGB 原生实现和一次性旧数据迁移。地产不进入标准商品订单簿，chunk loading 不随产权出售。
- 更新 ServerUtilities 映射、当前产品方向、AE2 仓储优先级和文档索引。AE2 方向保留但暂缓；BanItem 策略、BQ 任务事实、职业系统和连锁挖矿均记录为后续任务，其中 BQ/职业尚无权威制度合同，不做占位实现。
- 本轮仅更新设计文档，没有修改代码、数据库、服务器配置或部署产物。

### 2026-08-20 - 修复空对手盘被误报为余额不足

- 实机截图确认买入弹窗处于市价模式，账户余额为 `891385`，但盘口为“买一 61 / 最新 69 / 卖一 --”；根因不是银行余额，而是当前没有可立即成交的卖单。客户端此前将缺失的卖一价格解析为 0，再由最大可买量分支误报“银行可用余额不足”，并把预估总额错误显示为 0。
- 市价买入缺少卖盘时现在明确提示“卖盘为空，无法市价买入；可改用限价挂单”；市价卖出缺少买盘时给出对称提示。对手价不存在时预估总额显示 `--`，真实余额不足与库存不足校验保持不变；限价单仍可在无对手盘时正常挂入订单簿等待成交。
- 验证与部署：Docker Gradle 定向 `MarketOrderEntryPopupTest` 通过，新增买卖双向空对手盘回归；`assemble` 通过。构建产物、Lobby 与 Prism 客户端 JAR SHA-256 均为 `c953d7e4f47a4de2b7551e958582be0066cf57d4af4fe452794743285bbca040`。Lobby 已完成重启与部署脚本启动检查；未触碰 S1/S2，也未启动客户端。

### 2026-08-19 - 复盘并收口终端数字显示遗漏

- 复盘语义化数字规范的全部客户端落点，确认旧的独立 `K/M` 算法已经清除，服务端快照、网络结构和操作请求继续携带原始整数。
- 修复三类遗漏：订单与资产中心顶部银行/冻结/Vault 摘要及结构化订单、成交、交付行改为精确千分位；紧凑行情区的 24h 成交额与成交量统一复用三位有效数字规则；Vault 格位缩略图使用紧凑数量并在格位右下角缩放对齐，已选操作数量保留精确千分位。
- 修复结构化订单在极大数量下计算 `filled * 100 / original` 可能长整型溢出的边界，改用无溢出的整数比例计算；新增大额精确分组和 `Long.MAX_VALUE` 成交进度回归测试。
- 验证与部署：`git diff --check`、Docker Gradle 全量 `test` 与 `assemble` 通过；测试结果为 361 项、0 失败、0 错误、32 项按环境跳过。构建产物、Lobby、S2 与 Prism 客户端四处 SHA-256 均为 `6b504b524bb74c50a8e6642d2b664dbeb8939aeb4d852a582baf8bff2df6598f`。Lobby 到达 `Done (1.433s)!`；S2 已收到相同 JAR，但仍因既有 `World/level.dat` ZLIB 截断、`level.dat_old` EOF 及后续世界为空异常退出。本轮未触碰 S1，也没有启动或重启客户端；部署时客户端本来就在运行，需玩家正常重启后加载新 JAR。

### 2026-08-18 - 建立终端语义化数字显示规范并改造市场紧凑读数

- 实时库确认铁锭买单 #228 并非空数量：原始 2990、已成交 960、剩余 2030。客户端原始盘口行包含 `80 x2,030`，但三列盘口的通用标签在临界宽度按空格换行，只有价格可见，数量被放到不可见的第二行。
- 新增 `docs/terminal-number-display-standard-v1.md`，把数字显示拆成紧凑数量、精确分组值和精确操作值：盘口、图表坐标、最新成交窄列表使用三位有效数字的 `K -> M -> G -> T -> P -> E`；银行余额、冻结资金、价格、订单中心和确认流程保留精确千分位。
- 新增共享客户端数字格式器；盘口现在显示无空格的 `80x2.03K`，行情坐标和最新成交数量复用同一规则，市场深度向零截断以免夸大流动性。无空格形式消除窄列按空格换行的根因；盘口点击仍读取未经缩写的服务端原始行，因此打开订单时保持精确价格 80、数量 2030；行情悬停也改回精确成交量和成交额。
- 验证与部署：`git diff --check`、Docker Gradle 全量 `test` 与 `assemble` 通过；测试结果为 360 项、0 失败、0 错误、32 项按环境跳过。构建产物、Lobby、S2 与 Prism 客户端四处 SHA-256 均为 `e10dbcb0793c23007f9c54e7209c8cba2235ae873355d84ac8c30637b2c86732`。Lobby 到达 `Done (1.520s)!`；S2 已收到相同 JAR，但仍因既有 `World/level.dat` ZLIB 截断、`level.dat_old` EOF 及后续世界为空异常退出。本轮未触碰 S1，也没有启动或重启客户端；部署时客户端本来就在运行，需由玩家正常重启后加载新 JAR。

### 2026-08-18 - 修复测试买单手续费预留阻断真实卖单

- 实机日志确认卖价 20 的正常卖单在跳过玩家自己的 80 买单后，被测试做市买单 #58 阻断；根因是 `market-demo-fixture-v2/v3` 只冻结限价本金，没有预留买方成交手续费。失败事务已整体回滚，没有生成卖单、成交、银行划转或托管变化。
- 测试做市脚本现在按真实买单合同预留“剩余限价本金 + 最高 taker 手续费”，并已修复实时库 v2/v3 的全部活动测试买单；铁锭 #58 从 2331 调整为 2349，测试做市账户银行冻结总额同步为订单预留总额。加强后的审计显示 `active_buy_orders_under_reserved=0`、`buy_order_owners_without_bank_frozen_coverage=0`。
- 撮合增加最后防线：锁定的静止买单若仍不足额，则以幂等请求释放残余冻结、标为 `EXCEPTION`、记录 `ORDER_QUARANTINE`，并继续匹配下一张有效外部买单，不再让无过错卖家承担坏盘口状态。
- 手续费制度保持“成交后收费”：买单入簿时只冻结本金和最坏手续费容量，不产生税费流水；逐笔成交只收真实 maker/taker 费用，未成交数量、价格改善及未使用费用预留在成交完成或撤单时释放。终端确认和回执改称“资金预留”，明确预留不等于已经收费；卖方费用继续从真实成交收入中扣除。
- 新增挂单未收费、坏买单隔离后继续成交的回归测试，并扩展活动买单逐单预留和银行聚合冻结审计。本轮实时严格审计仍报告4条 8月12日旧自成交事故的 `RECOVERY_REQUIRED`，与本次手续费修复无关，未擅自执行玩家资产恢复。
- 验证与部署：`bash -n scripts/market-demo-fixture.sh scripts/market-audit.sh`、`git diff --check`、Docker Gradle 定向测试及全量 `test` 均通过；全量结果为 352 项、0 失败、0 错误、32 项按环境跳过。构建产物、Lobby、S2 与 Prism 客户端四处 SHA-256 均为 `2fc49e717b1357b70d5ec5c9e45da94c39bb62849781880c4a30dce49be001af`。Lobby 到达 `Done (1.437s)!`；S2 已收到相同 JAR，但仍因既有 `World/level.dat` ZLIB 截断、`level.dat_old` EOF 及其后 WR-CBE 世界为空异常退出。本轮未触碰 S1，也未启动客户端。

### 2026-08-18 - 记录交易所级能力缺口、结算制度与远期复杂产品边界

- 更新 `docs/modern-trading-terminal-redesign-v1.md`，把标准市场从撮合核心走向交易所级系统仍缺少的能力按正式路线收口：版本化规则、盘前风险网关、全局事件序列、独立对账、操纵监控、运行韧性、自动化接口准入，以及异常成交和申诉制度。
- 明确实施优先级：AE/机器订单开放前先完成限额与 Kill Switch、权威事件序列和独立对账，再推进监察、制度治理、争议处理、容灾演练及强制价格带/重开竞价。
- 记录当前标准市场不是 T+1：买卖双方在入簿前分别足额冻结资金和真实商品，成交事务立即完成资金、费用、订单、卖方托管扣减、成交记录和买方 `CLAIMABLE` 权利；随后自动投递 Base Vault，投递失败只进入待领取/恢复，不推翻已完成结算。
- 将做空、融资杠杆和期权明确排除在当前范围之外，同时作为远期研究项保留；禁止用负库存、负余额或未足额冻结订单进行简化伪实现，并记录未来重新评估各自所需的借贷、保证金、清算、行权和违约制度前提。本轮仅更新设计文档与工作日志，未修改代码或部署。

### 2026-08-18 - 记录标准市场自成交未绕过 BUG

- 新增 `docs/standardized-market-self-match-bug-2026-08-18.md`，记录批量测试中同一玩家提交交叉买卖单后被错误送入银行同账户冻结结算、最终显示“同步失败”的现场现象。
- 明确缺陷位于市场撮合候选选择阶段：市场层必须自动跳过同一账户的对手单，并继续按价格优先、时间优先寻找下一笔合格外部订单；银行层同账户转账拒绝继续保留为最终安全防线。
- 固定修复边界与八组验收场景，覆盖只有自有对手单、后续存在外部订单、部分成交、并发状态变化、账本与托管零副作用及终端反馈。本轮只更新 BUG 文档、文档索引和工作日志，未修改或部署代码。

### 2026-08-18 - 修复标准市场自成交候选未绕过

- `StandardizedSpotMarketService` 在候选订单锁定并重验后比较服务端订单所有者；候选属于主动单所有者时直接跳过，并继续按仓储层既有价格优先、时间优先顺序扫描后续外部订单。
- 不取消、不改价也不移动被跳过的自有订单；可交叉范围内只有自己的对手单时，新限价单与原订单均保持开放，不产生成交、税费、资产转移或同账户银行结算。
- 新增双向绕过及“只有自己的交叉单”三组回归测试。Docker Gradle 定向 `StandardizedSpotMarketServiceTest` 与完整 `test` 均通过；数据库结构、银行安全校验和客户端协议未改动。
- `git diff --check` 通过；构建产物、Lobby、S2 与 Prism 客户端四处 SHA-256 均为 `f0a32851567e2a1158136791eee96ae822474db1fc8a1f6ae446cc3c7cd9c29d`。Lobby 到达 `Done (1.384s)`；S2 已部署相同 JAR，但仍因既有 `level.dat` ZLIB 截断与 `level.dat_old` EOF 损坏退出。本轮未触碰 S1，也未启动客户端。

### 2026-08-18 - 记录标准市场扫盘与静止单定价实测规则

- 更新 `docs/modern-trading-terminal-redesign-v1.md`，把批量交易实测确认的连续订单簿语义固定为正式设计：买入限价是最高可接受价、卖出限价是最低可接受价，成交逐档使用订单簿中静止对手单的价格，而不是把全部成交强制写成主动单限价。
- 增加 `100 x100` 扫过 `90 x10 / 91 x20 / 95 x30` 的示例：前三档分别按 90、91、95 成交，剩余 `40` 才以买价 100 留在盘口；后来外部卖单可以按该静止买价成交，但责任仅限剩余数量与已冻结资金。
- 记录价格优先、同价时间优先、自成交自动绕过，以及与 SEC 限价定义和 Nasdaq 价格/时间优先、自成交防止、静止单价格成交示例的对应关系；同时明确项目不宣称实现 NBBO、跨市场路由、竞价、熔断、价格保护或完整美国证券法规。本轮仅更新设计与工作日志，未修改或部署代码。

### 2026-08-18 - 记录 7x24 标准市场竞价、熔断与自动化风控方向

- 更新 `docs/modern-trading-terminal-redesign-v1.md`，明确 7x24 市场继续以连续价格/时间优先订单簿为主，不照搬每日开收盘；集合竞价用于新商品、个品种熔断后、关键服务故障恢复、重大规则变化及长期缺少可信价格后的重开定价。
- 固定 `CONTINUOUS -> LIMITED -> PAUSED -> AUCTION -> CONTINUOUS` 个商品状态机：暂停不回滚已成交交易，始终允许撤销未成交余量；重开竞价公开预估清算价、配对量、失衡方向和倒计时，并按最大成交量、最小失衡、最接近可信参考价的顺序确定单一价格。
- 参考 LULD、NYSE MWCB、Nasdaq Halt Cross 与交易前风控，但不照抄依赖成熟指数和交易日收盘的 `7% / 13% / 20%` 参数。价格带先以影子模式记录，可信参考价需满足成交笔数、数量与金额门槛，防止一单位成交操纵熔断。
- 明确首版全市场暂停优先服务结算完整性：数据库、银行、Vault/AE 预留、恢复账本或行情新鲜度无法证明安全时，按最小影响范围进入降级或暂停，恢复后必要时通过集合竞价重开。
- 在 `docs/market-three-part-architecture.md` 增加市场保护与重开能力；未来 AE/API 必须经过统一认证订单合同、原子预留、幂等版本、心跳租约、速率/敞口限制、cancel-on-disconnect 与账户级 Kill Switch，并监控刷单、分层挂单、关联账户对敲及小额标记价格。本轮仅固化设计与阶段顺序，未修改或部署代码。

### 2026-08-15 - 市场快速撤单与订单资产中心产品修订

- 更新 `docs/market-action-receipt-and-personal-history-v1.md`：商品详情第三动作由“历史”修订为“撤单”，但只打开当前商品、当前玩家且仍有剩余量的活动委托弹窗，继续要求精确订单目标和服务端二次校验。
- 将既有个人历史页升级规划为市场与仓库共用的“订单与资产中心”，明确当前委托、成交记录、资产与交付、历史查询四个标签，并增加银行、Base Vault、活动委托和待交付的紧凑账户摘要。
- 历史成交退为辅助查询；高频买卖与快速撤单留在商品详情，跨商品订单和资产管理进入独立全页，泡泡回执负责单次动作反馈。本轮仅更新产品与交互合同，未修改运行代码。

### 2026-08-15 - 标准市场实机视觉验收纠偏

- 根据实机截图重新区分“功能完成”和“界面完成”：详情页账户区移除整句动作反馈，只保留行情同步状态与待入库数量，完整原因继续由通知和订单历史承载。
- 买卖弹窗重排为行情上下文、订单参数、结算预览和最终动作四个层级；数量比例、盘口价格快捷项与对应输入框就近排列，并保留服务端最终校验提示。
- 个人订单中心将四个整行大筛选器压缩为左对齐工具栏，右侧显示当前生效筛选摘要；搜索、重置和表格不再争夺同一层级。
- 本轮不修改撮合、银行、Base Vault、托管或订单协议，只收口三个已经在实机截图中确认层级过弱的客户端渲染路径。

### 2026-08-14 - 标准市场连续行情桶与零成交时段

- 新增 `docs/standardized-market-continuous-candles-v1.md`，锁定 1h/24h/7d 分别采用 12 个 5 分钟桶、24 个 1 小时桶和 28 个 6 小时桶；横轴始终按固定桶等距显示。
- 服务端行情聚合补齐 `TRADE / CARRY_FORWARD / REFERENCE / EMPTY` 来源。无成交时段沿用最近真实收盘价，或在从未成交时显示正式目录参考基线；成交量与成交额始终为 0，不生成假成交。
- 行情网络模型传递桶来源；客户端真实成交绘制红绿 K 线和成交量柱，无成交桶只延续价格并保留空的成交量位置，十字光标明确显示数据来源。
- 增加固定桶数量、时间间距、真实 OHLCV、前收延续、参考基线与完全空态测试。本轮不修改撮合、订单、银行、Vault、定制市场或汇率市场语义。
- 验证：`git diff --check` 与 Docker Gradle 全量测试通过，共 331 项完成、32 项跳过；Lobby、S2 与 Prism 客户端部署产物 SHA-256 均为 `bf8bde9ccf50fef9482ad2e6eb9c1ef654debca40717433cedf51bc62ef6dbf4`。
- Lobby 已到达 `Done (1.491s)!`。S2 仍因既有 `World/level.dat` 的 ZLIB 截断及 `level.dat_old` EOF 损坏而退出，本轮未擅自重建或回滚世界。

### 2026-08-13 - 标准市场红绿多形态行情图与十字读数

- 保持标准市场详情页现有左右比例和图表边界，不扩大行情区；行情按照真实数据密度自动降级：4 个及以上有效桶绘制红绿 OHLC K 线，2 至 3 个点绘制按涨跌分段着色的平滑价格线，单点绘制真实成交点，无成交才显示空态。
- 成交量柱与价格涨跌使用同一红绿语义，移除贯穿全图的单一蓝色走势；补齐价格、时间和成交量坐标，并增加仅在图表内部生效的十字光标。
- 十字光标浮层读取服务端真实时间、开高低收、成交量和成交额；浮层自动翻转并限制在图表边界内，不参与交易动作。
- 行情快照和网络模型扩展为完整 OHLCV 与成交额，服务端聚合不再在客户端退化为收盘价列表；旧三字段构造器继续保留给现有调用点。
- 本轮不修改撮合、银行、Base Vault、定制市场或汇率市场业务语义。

### 2026-08-13 - 个人订单历史分页容量与撤单链复核

- 修复订单历史工作台“可见 4 行、服务端却按 7 条分页”的合同错位。历史页大小统一为 4，并由服务端覆盖旧客户端传入的页大小；因此 11 条订单现在稳定显示为 3 页，不再有 3 条记录落在不可见区域。
- 新增 11 条订单首尾页回归测试，约束总页数、上一页和下一页状态；筛选、搜索和撤单刷新继续由服务端将页码夹在真实有效范围内。
- 复核撤单主链：UI 只对可撤订单携带真实订单 ID；服务端再次校验订单归属、方向和状态。卖单撤回未成交托管库存，买单释放剩余冻结资金，已成交订单拒绝撤回。

### 2026-08-13 - 订单搜索断线修复与终端数据包故障隔离

- 修复个人订单历史搜索在 PostgreSQL 中使用多字符 `ESCAPE` 导致 `invalid escape string` 的问题；查询改用单字符 `!` 转义，并安全处理 `%`、`_` 与转义符本身。
- 为终端客户端动作数据包增加服务端运行时异常边界。数据库或业务查询异常会记录完整服务端上下文，并向玩家返回通用失败提示，不再让异常穿透 Forge 数据包处理器并断开玩家连接。
- 新增搜索转义单元测试，并将真实 PostgreSQL 订单历史搜索纳入本轮定向验证。

### 2026-08-13 - 个人订单历史搜索、筛选与紧凑管理表

- 将标准市场“历史”页从四个大筛选按钮和松散文本行改为订单管理表：顶部第一行提供商品名称/物品键搜索、搜索与清空，第二行提供带字段前缀的商品、方向、状态和时间筛选。
- 搜索词随历史请求发送到服务端；PostgreSQL 同时匹配订单 `product_key` 与正式目录 `display_name`，所有筛选变更回到第一页，清空操作取消搜索和全部约束。
- 订单行改为结构化列，展示中文方向、正式商品名、价格、成交/总量、剩余量、中文状态和创建时间；仅可撤订单在本行显示精确撤单按钮，页脚与翻页控件不再覆盖列表内容。
- 扩展历史 payload、查询对象与目录名称回写，同时保留旧长度网络载荷兼容；补充 payload、筛选状态、订单行解析和草稿回写测试。
- 增加历史工作台在典型终端尺寸下的边界回归测试，约束搜索栏、筛选栏、订单表、行内撤单与页脚分页不得相互覆盖或越出内容区。

### 2026-08-12 - 市场操作回执与个人历史管理需求定稿

- 新增 `docs/market-action-receipt-and-personal-history-v1.md`，正式定义标准市场的即时操作回执和个人订单历史管理边界。
- 商品详情页三个主按钮锁定为 `买入 / 卖出 / 历史`；删除无目标的详情页撤单，撤单统一进入历史页后针对具体可撤委托执行。
- 历史页默认支持全部商品，并可按当前商品、买卖方向、订单状态和时间范围筛选；页面分为当前委托、成交记录、交付与异常三个标签。
- 右上角泡泡被定义为独立动作结果回执，必须与可重复刷新的市场 snapshot 分离，并提供持久操作记录作为遗漏回执的追溯入口。
- 本轮仅记录产品、交互、安全和接口需求，没有修改代码、构建或部署。

### 2026-08-11 - 标准市场全目录排序与历史恢复告警收口

- 标准市场的筛选和排序改为服务端先读取完整正式目录及批量真实行情，再执行“有成交 / 有盘口”和价格、涨跌、成交量排序，最后分页。客户端不再对已经分页的 12 个商品二次排序，避免页内排序伪装成全市场排序。
- 盘口点击继续走统一订单弹窗：点击买盘准备卖出限价单，点击卖盘准备买入限价单；数量和价格只作为弹窗草稿，最终仍由服务端重新校验。订单弹窗的“最大”按银行可用余额或账户仓可售数量计算。
- 市场审计扩展 Base Vault 未完成操作检查。严格审计确认正式目录、标准订单托管、买单冻结、定制市场、汇率市场和 Base Vault 当前均无新增异常。
- 对历史操作 16、17 完成资产核对：关联订单 52 已 `FILLED`、未成交量为 0，托管 9 已 `SETTLED`、数量为 0；它们是旧版本把“成交后再次撤单”误报为 `RECOVERY_REQUIRED` 的审计遗留。当前服务已将同类业务拒绝记为普通 `FAILED`，且不会改写已结算订单或托管。
- 修复重启恢复扫描器仍会把上述 `FAILED / SELL_ORDER_CANCEL` 再次升级的问题。恢复服务现在专门核对撤单关联订单；订单已 `FILLED` 或 `CANCELLED` 时保持终态 `FAILED`，不改写订单和托管，只有无法证明安全终态时才进入人工恢复。
- 验证：`git diff --check`、`bash -n scripts/market-audit.sh` 与标准市场、终端状态、订单弹窗定向 Docker Gradle 测试通过；历史记录结案后再次执行 `scripts/market-audit.sh --strict`。
- 最终交付：Docker Gradle `test` 与 `assemble` 全部通过；新 Lobby 启动后恢复扫描器自动将历史操作 16、17 收敛为安全 `FAILED`，`scripts/market-audit.sh --strict` 返回 `anomaly_count=0`。运行时 JAR SHA256 为 `178f2011eb92499872c1a821701400a4ea9d15fc676d17b5236c28a80a71e56c`，Lobby、S2 mods 目录与 Prism 客户端均已落同一文件；Lobby 达到 `Done (1.242s)!`。S2 未达到启动完成，其既有 `World/level.dat` 读取报 `Unexpected end of ZLIB input stream`，`level.dat_old` 为 0 字节；这是存档恢复事项，不通过重建世界或静默回滚绕过。

### 2026-08-11 - 玩家市场旧命令下线与汇率详情快照竞态修复

- 玩家标准、定制与汇率市场操作统一收口到银河终端 UI；`/jsirgalaxybase market` 不再路由买卖、报价、兑换或手持挂牌等旧玩家命令，补全也不再暴露这些入口。管理员市场恢复命令继续保留，用于资产异常处置。
- 汇率详情选择响应新增服务端权威 `selectedCoinCode`，并贯穿 snapshot、网络序列化和客户端 model。客户端在替换整屏 model 前核对当前待选币种或当前详情币种，丢弃晚到的旧选择响应，避免快速点击 A、B 后被 A 的异步快照覆盖 B。
- 汇率浏览模式仍接受普通刷新；返回浏览、滚动、切页及打开详情时维持既有 Hover 清理规则。共享四列浏览器补充正式空结果提示，不再留下无解释的整块空白。
- 验证：`git diff --check` 通过；补充汇率 A/B 乱序响应状态测试、`selectedCoinCode` 网络往返测试与市场命令表面测试。Docker Gradle 定向测试结果见本轮最终记录。

### 2026-08-10 - 定制与汇率市场工具栏、详情容器边界修复

- 修复非标准市场浏览页顶部空白/只剩范围按钮的问题：工具栏恢复由 `PanelContainer` 绘制真实搜索、刷新或发布、翻页控件，范围按钮继续作为紧凑筛选项，不再用整段自绘覆盖子控件。
- 修复定制与汇率详情动作按钮错误挂载到外层 section 的问题；详情面板改为拥有自身子控件的容器，切换页面、返回浏览和刷新快照时不会遗留跨页面按钮。
- 定制市场不再套用标准市场详情几何，新增专用挂牌详情布局，将商品/价格/托管状态、交易关系与购买/下架/领取动作约束在终端内容区。汇率详情同步压缩 Hero，并固定个人仓来源、正式报价与刷新/确认兑换动作区域。
- 验证：`git diff --check` 与 Docker Gradle 全量测试通过；新增工具栏子控件可见性、详情动作归属及两种专用详情布局边界测试。随后仅部署 Lobby 与客户端，不触碰 S1/S2，不启动客户端。

### 2026-08-09 - 定制市场演示挂牌与汇率专用详情收口

- 修复市场顶栏路由判定顺序：定制、汇率 snapshot 即使同时携带共享标准市场模型，也分别显示“市场 / 定制商品”和“市场 / 汇率市场”，不再误判为总入口。
- 汇率市场移除标准市场订单簿模板，改为专用的任务书硬币浏览、个人 Base Vault 来源、正式报价与兑换操作布局；目录由服务端结构化分页快照驱动，搜索、刷新和翻页均保留服务端上下文。
- 定制市场演示脚本新增 8 条带 `custom-market-ui-demo-v1` 来源标记的只读挂牌。应用服务在冻结资金、结算、下架和领取之前强制拒绝这些记录，演示数据只用于核验四列浏览、Hover 和详情页，不进入真实资产链。
- 验证：`git diff --check` 与 Docker Gradle 全量测试通过；新增汇率详情边界、市场顶栏优先级和演示挂牌只读隔离测试。Lobby、客户端与构建产物 SHA256 均为 `658ac0d069229e4fd585b5762baf320d07b8b2bfb1306086b6c7bf24c72a0b28`；Lobby 达到 `RUNNING` 并记录 `Done (1.425s)!`。PostgreSQL 已写入 8 条只读演示挂牌；严格审计仅报告 2026-08-07 遗留的两条 `SELL_ORDER_CANCEL / RECOVERY_REQUIRED`，本轮新增市场检查项均为 0。

### 2026-08-09 - 定制市场真实分页与非标准详情返回收口

- 修复定制市场此前仅在客户端对一次加载的最多 50 条挂牌再分页的问题。现在 PostgreSQL 按当前范围（全部挂牌、我的出售、待领取）、查询词、偏移和页大小计算总数并返回当前页，终端的总数与上一页/下一页状态来自服务端真实结果。
- 定制浏览 UI 不再回退解析旧的平行字符串列表；结构化浏览条目是唯一渲染来源。旧字段仍保留一轮网络兼容，但不再决定可见挂牌。
- 定制与汇率详情移除局部“返回浏览”按钮，顶栏返回在两种详情模式下统一回到保留搜索、范围、页码和滚动位置的浏览模式；标准市场既有行为未改。
- 没有改写定制挂牌、购买、下架、领取或汇率报价/兑换的服务端结算链。JDBC 分页查询已由编译和服务层定向测试覆盖；真实 PostgreSQL 中超过 50 条挂牌的范围、搜索和翻页仍列为灰度人工验收项。

### 2026-08-09 - 非标准市场三段式语义收口

- 共享市场浏览模型新增市场类型边界。定制挂牌与汇率硬币不再复用标准市场的买一、卖一、可卖、锁定、待收和价格图；卡片与 Hover 分别展示挂牌价格/交易方/交付状态，或硬币面值/目录层级/兑换资格。
- 定制市场发布改为“选择个人 Vault 的单件 -> 在独立价格弹窗填写正整数报价 -> 服务端确认”，不再依赖已移除的常驻价格输入。切换挂牌范围、搜索、翻页均请求服务端刷新，不再将空挂牌 id 误当作详情选择或因刷新强制进入详情。
- 汇率市场的搜索与分页同样通过结构化目录快照驱动；客户端不再重新从本地 `TaskCoinCatalog` 拼列表，避免分页、搜索与服务端目录状态失步。
- 保留服务端既有复核：定制实际购买、下架、领取仍由挂牌服务判断权限和交付状态；汇率目录选择不绕过个人 Base Vault 里的实际硬币与正式报价校验。
- 验证：Docker `compileJava`、市场消息/目录包、市场页面内容与壳层滚动定向测试，以及 `git diff --check` 通过；随后构建并部署 Lobby 与客户端，未启动客户端、未触碰 S1/S2。

### 2026-08-08 - 标准市场实时 JDBC 快照刷新

- 标准商品市场在终端停留期间每五秒请求一次新的服务端 snapshot，并保留搜索、页码、当前商品、图表周期与网格滚动位置。
- 自动刷新仅在标准市场、无 modal 且未编辑市场字段时运行；保持单一请求在途，并在响应丢失后有界恢复。后台 snapshot 不会盖掉打开中的确认弹窗。
- 刷新继续经过 `TerminalMarketService` 与 `StandardizedMarketReadRepository`，读取 PostgreSQL 正式目录、订单簿、成交记录、个人 Vault 可售量和银行状态；客户端不合成价格、盘口或流动性。
- 新增定时刷新状态机测试；Docker Gradle 定向验证与 Lobby/客户端部署在本条之后记录。

### 2026-08-08 - 标准市场紧凑字号与图表边界收口

- 将详情页“24h 行情 / 账户与交付”及其字段统一为盘口使用的 `0.78` 文字比例，并压缩行距与统计区高度；释放出的垂直空间交给五档买盘、最近成交和卖盘。
- 新增市场紧凑文字度量工具，主图坐标使用 `0.68` 比例并按缩放后的真实宽度预留轴空间，降低价格、成交量和时间标签重叠风险。
- Hover 图表坐标使用相同紧凑轴字号与宽度计算，保证最大/最小价格及起止时间始终留在浮层边界内。
- 删除浏览商品卡右下角流动性状态点；流动性文字仍保留在 Hover 中，不损失行情语义。
- 验证：`git diff --check` 与 Docker Gradle 全量测试通过；构建并部署 Lobby 与客户端后由用户执行实机视觉验收。

### 2026-08-08 - 标准市场日内涨跌、图表与交易弹窗收口

- 浏览行情：商品卡的涨跌改为以上海时区当日 `00:00` 后第一笔真实成交为开盘基准，并显示服务端计算的日内涨跌百分比；当日无成交时明确显示中性空态。紧凑卡片、Hover 和详情摘要移除重复的 `STARCOIN` 后缀，货币含义由市场上下文承担。
- 浏览与 Hover：商品卡移除占空间的迷你图，只保留价格、日内涨跌和流动性。Hover 补齐最新价、今日涨跌、零点基准、买一/卖一、24h 成交量、流动性、账户仓状态，并以红绿涨跌柱和明确的价格/时间坐标展示真实成交变化；不足两个真实成交点时不保留空图区域。
- 详情布局：压缩 Hero 与页脚高度，扩大图表和盘口占比；价格区使用蓝色真实成交柱，成交量按相邻价格涨跌显示红绿柱，补齐价格、成交量和时间坐标。买盘使用绿色、成交使用中性色、卖盘使用红色。
- 交易交互：详情页移除常驻价格与数量输入框，只保留买入、卖出、撤单命令。买入/卖出打开独立订单弹窗，在弹窗内选择市价/限价并填写数量、价格，继续复用既有统一订单 payload 和服务端复核链。
- 防叠层：修复 `LabelPanel` 在零高度布局中仍绘制首行的框架缺陷，防止已隐藏标签跨卡片叠层。
- 验证：`git diff --check`、Docker Gradle 全量测试和 `assemble` 通过；与本轮详情分栏一起部署 Lobby 与客户端，由用户执行实机视觉验收。

### 2026-08-08 - 标准市场详情左右主分栏收口

- 依据用户确认的最新效果图，将标准市场详情改为左侧约 48% 的紧凑行情与操作区、右侧约 52% 的全高主图；商品头不再横跨全页，主行情图获得完整纵向空间。
- 左侧依次放置紧凑商品条、买盘/最近成交/卖盘、两列关键统计和买入/卖出/撤单按钮。移除底部三张大状态卡及按钮下方操作提示，交易参数继续在独立弹窗填写。
- 为避免共用布局回归，保留定制市场和汇率市场使用的原 `MarketDetailLayout.within(...)`，仅标准市场改用 `withinStandardSplit(...)`。
- 验证与部署：`git diff --check`、Docker Gradle 全量测试和 `assemble` 通过；构建端、Lobby 与客户端 JAR SHA256 均为 `03d87c311071a35b1667f31016f96607913f100517e136953892ad78da4c5118`。Lobby 已监听 `25564` 并记录 `Done (1.419s)!`；未启动客户端，未触碰 S1/S2。

### 2026-08-08 - 标准市场现代交易终端 P0-P3 第一轮落地

- 主题：以 `/home/u24/图片/市场第三版.png` 为视觉基准，将标准市场从旧托管操作页改为账户仓自动交割的现代交易工作台。
- 交易主链：新增账户库存解析接口及 Base Vault 实现。限价/即时卖出从个人 Vault 跨格聚合预留后进入既有市场托管和订单链；即时剩余量与撤单库存尝试返回 Vault。买入成交自动尝试投递 Vault，容量或交付异常继续保留既有待处理和恢复记录。旧存入/领取路由保留兼容，但不再是新 UI 的常规主流程。
- 行情与 UI：新增批量只读行情投影和成交/订单读取索引；浏览页固定 `4 x 3`，加入真实涨跌、迷你走势、流动性、筛选和排序；Hover 收成真实行情比较卡。详情页采用横向图表、盘口和统一订单票据，页脚只保留账户仓、委托和异常待收货摘要。
- 合同：新增统一 `MARKET_CONFIRM_ORDER` 及可兼容旧 payload 的买卖方向、订单类型、数量、限价和浏览上下文字段；服务端继续重新校验价格、余额、库存与手续费。
- 验证：`git diff --check` 通过；Docker Gradle 全量测试通过（301 个完成，31 个按环境跳过）。详情图后续补接 `1h / 24h / 7d` 真实聚合桶切换，当前显示收盘价线与成交量；完整 OHLC 实体、数据库级全目录排序、盘口回填和最大数量快捷操作明确留作后续，不作完成宣称。

### 2026-08-08 - 标准市场真实行情区间接线与重新部署

- 主题：补齐第一轮复盘发现的“服务端已有三周期聚合、客户端仍固定 24h”断点。
- 结果：标准市场详情图新增 `1h / 24h / 7d` 区间控件；浏览/刷新 payload 保留区间，服务端按区间读取真实桶，并通过兼容价格点合同回写收盘价、成交量和桶时间。区间切换不改撮合、银行、Vault 或定制/汇率市场。
- 边界：当前仍是聚合收盘价线与成交量柱，不是完整 OHLC K 线；全目录数据库级排序、盘口点击回填和“最大”快捷数量仍未完成。
- 验证与部署：`git diff --check`、Docker Gradle 全量测试和 `assemble` 通过（302 个测试完成，31 个按环境跳过）；仅部署 Lobby 与客户端，三处 runtime jar SHA256 均为 `54b8514b85f5f498e58b813bc1927e994262eedbc4f5612f3972d3155865fc6c`。Lobby 达到 `RUNNING` 并记录 `Done (1.723s)!`；S1/S2 未启动，客户端未启动。

### 2026-08-08 - 现代交易终端最新效果图差异基线

- 主题：以 `/home/u24/图片/市场第三版.png` 为标准商品市场唯一视觉目标，复盘当前浏览、Hover 和详情三张实机图。
- 结论：终端壳、四列骨架、真实 ItemStack、Hover 与独立详情路由已经成立；主要差距为浏览仅 8 项且缺少涨跌/迷你走势、Hover 过大且缺少真实行情、详情仍是旧盘口文本和七按钮工作流，以及手动存入/领取仍暴露内部托管语义。
- 产物：新增 `modern-trading-terminal-visual-gap-baseline-2026-08-08.md`，按 UI、行情聚合、交易 payload、账户仓自动交割四类记录差距，并给出 P0-P3 优先级和逐屏验收清单；`modern-trading-terminal-redesign-v1.md` 与文档索引已建立追溯链接。
- 验证：仅文档变更；不修改代码、数据库或部署产物。执行 `git diff --check` 后收口。

### 2026-08-07 - 个人 Base Vault 成为市场唯一物品边界

- 主题：标准、定制与汇率市场统一以个人 Base Vault 为物品来源与收货账户。
- 结果：新增只读 `VaultAssetPickerPopup`，标准市场按正式目录商品聚合选择数量，定制发布与汇率兑换选择精确 Vault 格位。标准入托管复用同一 request id 完成 `Base Vault -> AVAILABLE`，定制市场的单件托管和汇率的任务书硬币扣除继续走 Vault 服务；标准与定制领取均投递个人 Vault，满仓则保留既有待领取状态。定制/汇率页面现在也随市场快照携带只读 Vault 资产，选择器不再依赖玩家背包或当前手持。
- 安全边界：选择器只读，市场动作重新校验 Vault 格位/商品目录/报价；版本冲突、数量不足、Vault 满仓或报价过期不会自动改读玩家背包或自动重试。
- 验证：`git diff --check`、Docker `compileJava` 已通过；待执行市场 payload、服务与 UI 定向测试后部署 Lobby 与客户端，不触碰 S2。

### 2026-07-24 - Base Vault Shift 转移：补齐客户端原版容器预测

- 主题：修复个人 Base Vault 中 Shift 快速转移会使 GTNH 客户端卡死的问题。
- 根因：服务端 `BaseVaultContainer` 已实现 Vault 与玩家背包之间的 `transferStackInSlot(...)`，但客户端占位 `ClientContainer` 沿用 `Container` 的默认空实现。服务端可以成功写入 Vault 审计与槽位，而客户端本地容器预测不执行对应搬运，造成原版槽位同步路径失配。
- 结果：客户端容器现在镜像服务端的 Vault/玩家背包快速转移规则，并补齐原版 `onPickupFromSlot(...)` 回调；服务端同样补齐该回调和“无实际变更返回空”的原版语义。普通点击与既有 PostgreSQL 版本校验、审计、恢复链未改。
- 安全补充：显式拒绝原版 `mode=3` 的创造模式中键复制，避免把跨服持久 Vault 槽位复制到光标而没有对应账本扣减。
- 验证：待执行 Docker 定向编译/测试与 Lobby、客户端部署；人工验收应覆盖玩家背包到 Vault 和 Vault 到玩家背包两个方向的 Shift 操作，以及关闭后重新打开的持久性。

### 2026-07-23 - Base Vault 容器化 Phase 1：原版格位交互接入跨服槽位账本

- 主题：将终端“仓库”从说明 section 替换为服务器权威的原版 `Container + Slot` 保险箱页。
- 结果：个人 Vault 以固定 `9 x 3` 格显示在上方，玩家背包和快捷栏显示在下方；左/右键、拖拽、拆分、双击合并与 Shift 转移继续由原版 Container 语义处理。Vault 格位是会话库存，完成一次真实变动后以原开仓版本进行 PostgreSQL 校验和提交；提交失败会还原本次会话与玩家背包快照，成功操作记录 `VAULT_CONTAINER_MUTATION` 审计。
- 入口与边界：终端导航“仓库”直接打开 Forge GUI ID `41`，不再显示通用仓储说明卡。容器仅开放个人 27 格；企业/公共容量、市场 Vault 选择器、AE2 Cell Bay/Drive/Port 保持后续阶段，不与本轮原版搬运交互混合。
- 验证：`git diff --check`、Docker `assemble`、Docker 定向 `BaseVaultServiceTest` 通过；新增容器快照提交与版本更新回归测试。按灰度约束仅准备部署 Lobby 与客户端，不触碰已知损坏的 S2 世界。

### 2026-07-20 - Base Vault Phase 0：有限账户仓与市场交付边界

- 主题：建立跨服持久的有限 Base Vault，作为个人、企业、公共账户的起步仓与市场交付目标。
- 结果：新增账户、槽位与操作日志迁移；个人容量为 27 格，企业/公共为 54 格。槽位保存完整压缩 `ItemStack` NBT 并遵守原版堆叠上限。标准市场领取、定制市场领取/下架改为投递个人 Vault；Vault 满时资产保留在现有待领取状态。标准市场存入改为 Vault -> 市场托管的共享 JDBC 事务，失败整体回滚。
- 恢复边界：每次 Vault 操作使用 request id 幂等记录；未完成操作拒绝自动重放。玩家背包只会在后续独立 Vault 页的显式存取适配器中接触，市场主页面不再允许把背包作为标准商品存入来源。
- 验证：Docker `compileJava` 通过；定向测试通过 `BaseVaultServiceTest`、`TerminalMarketServiceTest`、`StandardizedSpotMarketServiceTest`；`git diff --check` 通过。未部署，必须先在停服窗口运行新迁移。

### 2026-07-20 - 标准市场浏览：空选中状态不再丢失正式目录页

- 主题：修复标准商品市场首次进入浏览模式时，数据库目录已存在但终端显示 `0 项 / 0/0` 的断链。
- 根因：`TerminalMarketService.createSnapshot(...)` 会先读取数据库正式目录，但当玩家尚未选中商品时提前返回详情空态，未将刚读取的 `catalogPage` 与批量行情摘要附加到该 snapshot；网络层因此只能收到默认空目录。
- 结果：目录附加收口为 `attachCatalogBrowserData(...)`，无选中商品和已选商品两条路径都回传当前搜索页、分页元数据与行情摘要。未选中仅意味着详情页为空，绝不意味着目录为空。
- 验证：新增空选中快照保留正式目录页的回归测试；Docker Gradle 定向测试通过 `TerminalMarketSectionServiceTest`、`TerminalMarketServiceTest`、`TerminalMarketCatalogPacketTest`，`git diff --check` 通过。runtime jar 已部署至 Lobby 与客户端，SHA-256 为 `bdaea9c249eca7e4b173f7f55ecc2d67fdd3d54370e362411d385b71cc77a7f4`；数据库烟测确认正式启用目录为 8 项。

### 2026-07-19 - 市场三段式浏览基础：四列目录、只读 Hover 与独立详情模式

- 主题：将标准市场从常驻三栏工作台转为可被三个市场复用的“浏览 -> 对比 -> 操作”交互基础。
- 结果：新增四列 `MarketItemGridPanel`，搜索和分页保持固定、只有网格可滚动；目录行带真实物品图标与结构化行情摘要。`CanvasScreen` 新增被动 hover layer，层级固定为正文、hover、modal popup，悬浮信息按终端边界自动翻转夹紧，滚动、点击、离开与 popup 打开都会关闭。标准市场点击目录项先沿用现有选择/快照回写，服务端确认选中后进入客户端 `DETAIL` 模式；顶栏返回回到浏览模式并保留浏览上下文。目录快照补充批量成交摘要与最多十二个真实价格点，无真实历史时明确空态。
- 验证：新增 tooltip 定位与四列网格边界测试；更新标准市场布局测试以匹配浏览全页、详情独立页的边界。待执行 Docker Gradle 定向测试、部署 Lobby 与客户端；本轮不启动客户端，实机观感交由人工验收。

### 2026-07-19 - 市场三段式整改第二阶段：标准商品详情工作台边界收口

- 主题：将标准市场点击后的详情模式从临时拼装的工作区，收口为可供定制和汇率市场复用的固定预算详情工作台。
- 结果：新增 `MarketDetailLayout`，由详情父容器统一推导 Hero、三列盘口、资产状态卡和底部交易区的边界；详情页不再拥有整页滚动。交易区将六个输入拆为两行，避免与交易 CTA 在窄高区域竞争同一行；固定动作条补齐领取与撤单，且仅在快照给出目标 custody/order 时启用。库存、待领取、订单/冻结三张卡的标题、数值和语义行分开绘制，消除数值与副文案叠字；浏览网格滚动偏移回写到 `TerminalMarketSectionState`，顶栏从详情返回浏览后可恢复查询、页码和网格位置。既有存入、限价、即时、撤单、领取确认与服务端 snapshot 链未改变。
- 验证：新增 `MarketDetailLayoutTest` 和详情返回浏览上下文测试；Docker Gradle 定向测试通过。待构建部署 Lobby 与客户端；按约定不启动客户端，实机 UI 和点击由人工确认。

### 2026-07-19 - 市场现场可用性：弹窗渲染隔离、受管演示流动性与任务书硬币目录

- 主题：修复市场现场验证中暴露的图标层级、定制市场底部动作条、空经济无法验证买卖，以及汇率页把任务书硬币过度简化为单一目标的问题。
- 结果：`CanvasScreen` 和真实 `ItemStack` 渲染器会在物品图标绘制后清理深度状态，popup 遮罩和正文不再被旧物品图层穿透；定制市场的发布/购买/下架/领取统一保留在固定高度底部操作条内。新增显式 `TaskCoinCatalog`，仅接纳 GTNH 本地语言数据确认的 15 个任务书硬币家族、每族 5 个面额（共 75 种），排除捐赠币与区块加载币；汇率页将它们作为可滚动的正式输入目录，实际报价仍由玩家手持物和服务端规则决定。新增 `market-demo-fixture.sh`，隔离没有托管或资金约束的旧视觉 demo 订单，并创建有真实 escrow/frozen-funds 的受管 Steel 流动性；当前离线 UUID 的玩家账户已单独补齐测试余额和可卖托管库存。审计把已安全失败、没有资产副作用的历史 preflight 操作作为历史计数显示，不再误报成需要恢复的未完成操作。
- 验证：待执行 Docker Gradle 定向测试、严格市场审计与 Lobby/客户端部署；不启动客户端，实际 UI 和点击由人工确认。

### 2026-07-19 - Round 4：仓库跨域转移与审计合同收口

- 主题：把市场托管仓、未来企业仓与公共仓从概念性区分推进为可实现的转移/审计边界，不提前实现不成熟的企业或公共库存。
- 结果：新增仓库转移合同，定义四类资产域、允许/禁止的转移矩阵、`InventoryTransferOperation` 必填字段、request id 幂等要求，以及 Minecraft 物品交付与数据库状态无法原子化时的 `RECOVERY_REQUIRED` 原则。现有市场托管、定制交付和汇率兑换均被明确纳入同一中断处理约束，但仍保持各自独立账本。
- 验证：文档结构复核；未触及运行时代码、数据库或客户端。

### 2026-07-15 - 汇率市场 Round 4：物理任务书硬币与银行结算的中断可审计

- 主题：补齐汇率兑换中“玩家手持任务书硬币”这一非数据库资产与银行正式结算之间的中断边界。
- 结果：兑换在移除手持物品前创建 `EXCHANGE_EXECUTION` 操作日志，并与银行结算复用同一个 request id。成功时日志记录银行交易与兑换记录；明确失败时恢复手持物品并留下已收口失败记录；启动/管理员恢复扫描若发现已经存在银行结算便完成操作，若无法确认结算则标为 `RECOVERY_REQUIRED`，附带物品、数量、meta、槽位等核对元数据，禁止系统擅自重试或静默吞没资产。
- 验证：Docker `compileJava` 已通过；新增 `MarketRecoveryService` 单测覆盖“已结算自动收口”与“未确认物理输入升级人工核对”。不启动客户端。

### 2026-07-15 - 标准市场领取恢复：安全交付失败在事务回滚后即时收口

- 主题：修复标准市场领取时“背包明确未收到物品”的安全失败在 JDBC 事务回滚后仍停留于 `PROCESSING` 的状态滞后。
- 影响范围：`StandardizedSpotMarketService`、PostgreSQL 市场集成测试与市场命令帮助。
- 结果：背包满等 `safeToRestoreClaimable` 失败现在在领取事务回滚后另开收口事务，将托管资产确认回 `CLAIMABLE` 并将操作日志标为 `FAILED`；若此收口本身失败，才按既有规则升级为 `RECOVERY_REQUIRED`。这样玩家可用新请求重试领取，审计也不会把正常的安全失败误表示为卡住的处理中操作。市场帮助同时列出定制市场管理员 `custom recover` 入口。
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过 `StandardizedSpotMarketServiceTest`、`MarketPostgresIntegrationTest`、`GalaxyBaseCommandTest`，其中新增 PostgreSQL 回归覆盖安全发放失败后的 `CLAIMABLE + FAILED` 即时状态。

### 2026-07-15 - 汇率市场二次确认：服务端绑定正式报价而非仅信任客户端弹窗

- 主题：将终端汇率市场的二次确认从“客户端显示过 popup”补为服务端可验证的短时、一次性确认许可。
- 结果：每次服务器生成可执行兑换快照时，会将正式兑换标的、输入物品、数量、资产对、规则版本、限额状态、结算额与汇率绑定到玩家和终端会话；确认时服务端重新读取当前 quote 并逐项校验，成功后即消费许可。手持物品、规则、限额或会话发生变化均不能沿用旧确认，必须刷新报价并重新确认。实际金额仍由 `ExchangeMarketService` 在结算前重新计算，客户端不提供可被信任的金额字段。
- 验证：新增确认闸门单测覆盖一次性消费与报价金额/汇率变化拒绝；后续与 `TerminalServiceTest`、`ExchangeMarketServiceTest` 一起通过 Docker Gradle 定向验证。

### 2026-07-15 - 定制市场交付恢复：区分安全失败与不确定交付

- 主题：为定制挂牌的下架/领取交付补齐防重复派发保护，避免数据库状态与背包发放的非原子边界被静默误判。
- 影响范围：`CustomMarketService`、`GalaxyBaseCommand`、`CustomMarketAuditLog` repository、`custom_market_audit_log` migration、市场审计脚本与定制市场服务测试。
- 结果：终端及兼容命令入口都会在交付前先将挂牌（及成交记录）置于保护状态，并创建 `LISTING_DELIVERY` 审计记录。背包满等明确未交付错误会恢复原始 `ESCROW_HELD` 或 `BUYER_PENDING_CLAIM`；普通运行时中断或显式不确定交付被标为 `DELIVERY_UNKNOWN` 并保持 `EXCEPTION`，后续请求不能自动再次发货，必须由审计/人工恢复流程处理。管理员可用 `/jsirgalaxybase market custom recover <listingId> <restore|complete> [reason]` 显式决议：`restore` 仅在确认未交付后恢复原状态，`complete` 仅在确认已交付后收口状态，两个分支都不会再次操作背包。审计脚本将“处理中”和“不确定交付”纳入严格异常计数。
- 验证：Docker Gradle 定向测试通过 `CustomMarketServiceTest`、`GalaxyBaseCommandTest`、`MarketPostgresIntegrationTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`、`StandardizedSpotMarketServiceTest` 与 `ExchangeMarketServiceTest`；其中新增覆盖未知交付失败、交付成功后完成落库失败，以及对应的禁止自动重试。已通过部署脚本将 runtime jar `73f39d17b1ef0e54afb72711026707ef0f73adb9330e0d4677e9a9c886eb3fb2` 同步到 Lobby、S2、客户端；migration `20260715_003_add_custom_market_delivery_audit_type.sql` 已应用，Lobby 的市场烟雾测试和严格审计通过。S2 jar 已同步，但其 `World/level.dat` 与 `level.dat_old` 均为 EOF 损坏，世界启动失败，和本轮 market jar 或迁移无关。

### 2026-07-15 - 市场恢复链：定制挂牌接入真实背包交付与统一审计

- 主题：收口定制市场过去“只改数据库状态、不把物品交给玩家”的断链，并把它接入现有无客户端市场审计。
- 影响范围：`CustomMarketService`、Minecraft 定制交付适配器、终端定制发布/下架/领取动作、`scripts/market-audit.sh` 与仓库边界文档。
- 结果：终端可以将当前手持的单件物品发布为定制挂牌；卖家下架或买家领取时会先执行背包容量检查和 `ItemStack` 快照交付，再收口数据库状态。市场审计现在额外检查定制挂牌快照缺失、已售缺成交、挂牌/成交交付状态不一致与待领取/异常资产；`--strict` 对真正异常返回非零，待领取只作为运维待办列出。
- 验证：`git diff --check`、`bash -n scripts/market-audit.sh` 通过；只读审计的标准与定制异常计数均为 `0`；Docker Gradle 定向测试通过 `CustomMarketServiceTest`、`TerminalMarketServiceTest` 与 `TerminalServiceTest`。不启动客户端，实机 UI 与点击仍交由人工验收。

### 2026-07-15 - 标准市场 Round 3：确认报价补齐费用、来源与恢复语义

- 主题：让标准市场的确认弹窗不再只写“确认交易”，而是展示服务端生成的真实报价摘要和资产去向。
- 影响范围：`TerminalMarketService`、`TerminalHomeScreen`。
- 结果：即时买入报价明确拆出本金、taker 手续费、冻结/结算总额和极限成交价；即时卖出明确拆出成交额、手续费与净到账。限价买卖、存入、即时买卖、撤单和领取确认弹窗均补充资产来源、冻结/托管去向，以及余额、库存、深度、背包发放失败等服务端拒绝或恢复条件。
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过 `TerminalServiceTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalMarketSectionContentTest`、`TerminalMarketServiceTest`、`StandardizedSpotMarketServiceTest`。未启动客户端。

### 2026-07-15 - 汇率市场 Round 4：正式结算链的无客户端审计接入

- 主题：确认汇率市场不是独立的 GUI 计算，而是具有规则、限额、二次确认、银行结算和审计关联的正式链路。
- 影响范围：`scripts/market-audit.sh`。
- 结果：审计会检查每条 `coin_exchange_record` 对应银行交易的双方审计元数据都标明 `marketType=exchange`；这与现有兑换服务写入的兑换对、规则版本、限额状态和输入资产审计字段共同构成可追溯证据。
- 验证：`bash -n scripts/market-audit.sh`、`scripts/market-audit.sh --strict` 通过；当前库没有历史兑换记录，因此这是结构对账验证，不替代实际兑换样本验证。

### 2026-07-15 - 市场 Round 4：定制挂牌与汇率页面收口为受边界约束工作台

- 主题：将定制市场和汇率市场从旧的卡片内标题、子页返回按钮及自然向下堆叠，收口为与标准市场一致的双栏工作台。
- 影响范围：`TerminalCustomMarketSection`、`TerminalExchangeMarketSection` 与市场滚动布局测试。
- 结果：两页均移除了重复的“返回总入口”和正文标题，统一由终端顶栏的返回、刷新、帮助、关闭控制；左侧挂牌/兑换标的列表成为各自唯一滚动区，右侧商品交付详情或正式报价、执行信息及确认按钮保持固定在页面边界内。定制市场继续使用独立挂牌/交付托管语义，汇率市场不引入商品托管状态。
- 验证：`compileJava`、Docker Gradle 定向测试 `TerminalShellPanelsScrollTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalMarketSectionServiceTest`、`StandardizedSpotMarketServiceTest` 与 `git diff --check` 通过。

### 2026-07-15 - 标准商品目录：完成 8 条 GTNH 正式条目

- 主题：收口历史目录迁移遗留的技术展示名，确保正式目录不再把 `product_key` 直接暴露给玩家。
- 影响范围：新增 GTNH 元数据正式化迁移、目录制度文档和 `market-smoke-test.sh` 自检。
- 结果：将铝锭、铜锭、银锭、铁板和钢板补为经 GTNH 2.8.4 `GregTech.lang` 元数据核验的名称与单位，并写入已有最近成交价和确定性排序；烟雾测试现在会拒绝任何仍以技术键作为展示名的启用目录条目。

### 2026-07-15 - 标准商品市场 Round 3：增加无客户端状态审计

- 主题：为既有托管、冻结资金、领取恢复和市场操作日志提供可重复执行的只读审计入口。
- 影响范围：新增 `scripts/market-audit.sh` 与标准商品目录运维文档。
- 结果：审计可检查目录正式化、开放卖单对应 `ESCROW_SELL`、开放买单冻结资金、陈旧 `CLAIMING`、异常托管和未收口操作；`--strict` 会以非零状态提示管理员干预，恢复动作仍需显式使用 `/jsirgalaxybase market recover`。

### 2026-07-15 - 标准商品市场 Round 2：目录浏览器接入结构化目录与分页

- 主题：将 `MARKET_STANDARDIZED` 左栏从旧的平行字符串商品列表切换到正式目录快照，并使查询和分页通过既有市场 payload 回到服务端。
- 影响范围：`TerminalMarketSectionState`、`TerminalMarketSectionContent`、`TerminalMarketSection`、`TerminalShellPanels`、`TerminalHomeScreen` 及市场布局/内容测试。
- 结果：
  - 左栏优先读取 `CatalogProductModel`，使用正式商品键、registry/meta、展示名、单位、参考价和可交易状态；停用目录项不会作为可点击商品出现。
  - 商品行使用 registry/meta 构建真实 `ItemStack` 图标引用；渲染失败时才退化为已有 badge。
  - 搜索词、页码、上一页/下一页控制写入 `TerminalMarketSectionState -> TerminalMarketActionPayload`，并以既有 `MARKET_REFRESH` 请求服务端目录页，不引入第二条网络链。
  - 左栏仍是标准市场唯一的 `VerticalScrollPanel`；搜索和分页器保持固定，右侧行情、资产状态和动作条保持固定布局。
  - 更新布局测试，验证浏览器正式控件、固定分页器以及右侧无滚动边界。

### 2026-07-15 - 标准商品市场 Round 1 收尾：结构化目录快照与参考价

- 主题：把正式商品目录的商品行、查询和分页状态纳入终端快照/网络契约，替代 UI 只能依赖 `productKeys/productLabels` 平行字符串数组的状态。
- 影响范围：新增 `CatalogProduct` / `CatalogProductModel`，包含商品键、registry/meta、展示名、单位、排序、启用状态、参考价与可交易状态；市场 section 快照、客户端模型和终端网络包均追加目录页数据；新增 `reference_price` 迁移与目录管理 SQL 说明。
- 行为边界：旧字符串数组暂时保留，只用于现有 UI 兼容；Round 2 将改为直接消费结构化目录行。禁用商品仍由 JDBC 查询和目录准入边界排除，不能浏览或通过手持物快捷存入。
- 验证：Docker Gradle `compileJava` 与定向测试通过：`StandardizedMarketCatalogServiceTest`、`TerminalMarketCatalogPacketTest`、`TerminalMarketActionPayloadTest`、`TerminalMarketSectionServiceTest`；覆盖搜索/页码、空目录、禁用准入和目录网络 round-trip。

### 2026-07-14 - 标准商品市场 Round 1：正式目录与托管主链收口

- 主题：把标准商品市场从 GregTech 硬编码推断准入转为管理员维护的 PostgreSQL 正式目录，并让终端浏览来源与玩家手持、订单、成交反推解耦。
- 影响范围：新增 `standardized_market_catalog` 迁移和 JDBC 目录源；`InstitutionCoreModule` 运行时改为注入数据库目录；`StandardizedSpotMarketService` 新增目录分页能力；终端 payload 增加查询、页码和筛选上下文；部署编排在服务端替换 jar 前自动执行版本化迁移。
- 行为边界：卖单和即时卖出仍只消耗 `AVAILABLE`；`ESCROW_SELL`、`CLAIMABLE` 和买单冻结资金继续复用现有订单、托管、银行链。手持物品仅可快捷存入已准入商品，不能再成为正式目录真相来源。
- 验证：`git diff --check`、`bash -n` 通过；Docker Gradle `compileJava` 通过；定向单测通过 `StandardizedMarketCatalogServiceTest`、`TerminalMarketActionPayloadTest`、`TerminalMarketSectionServiceTest`、`StandardizedSpotMarketServiceTest`、`MarketPostgresIntegrationTest`、`InstitutionCoreModuleTest`、`TerminalMarketServiceTest`。
- 后续：Round 2 用正式目录数据收口标准市场浏览器 UI，包括搜索、分页控制、紧凑商品行和结构化目录行网络 DTO；Round 3 覆盖完整交易恢复、审计与异常资产场景。

## 记录规则

- 每次代码变更后，追加一条简要记录
- 每条记录至少包含：
  - 日期
  - 变更主题
  - 影响范围
  - 简要原因
- 如果变更依赖外部制度文档或外部参考源码，应写出引用来源

## 当前关键引用文档

当前开发默认参考下面这些文档：

- `../README.md`
  - 当前项目定位、代码结构和正式架构约束
- `../../Docs/设定.md`
  - 制度目标、职业、市场、贡献度、跨服阶段路线
- `../../Docs/技术边界文档.md`
  - `JsirGalaxyBase` 的责任边界与跨服同步边界
- `../../Docs/做法.md`
  - 群组服、中心数据库、同步方案与后端边界
- `../../Docs/市场经济推进.md`
  - 市场账本、订单、托管库存与一致性要求
- `../../Docs/下次对话议程.md`
  - 当前已定稿制度结论与后续讨论顺序

## 初次对话摘要

### 2026-06-29 - 标准商品市场默认交易草稿与按钮启用链路修复

- 主题：修复 `MARKET_STANDARDIZED` 已显示商品但买卖按钮不可用的问题，把紧凑交易 UI 接回现有确认 popup 与市场服务动作链
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSectionService.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSectionServiceTest.java`、`docs/WORKLOG.md`
- 原因：数据库、运行时目录和终端 snapshot 已能读取 8 个标准商品，但新版紧凑 UI 不再展示价格/数量输入框；旧动作链仍要求 `LimitBuyDraft`、`LimitSellDraft`、`InstantDraft` 中存在价格和数量，导致 `即时买`、`即时卖`、`买挂牌`、`卖挂牌` 在选中商品后仍保持 disabled，没有进入 `TerminalMarketService` 的真实买卖逻辑
- 结果：标准商品 snapshot 现在会从当前盘口和玩家 AVAILABLE 库存生成保守默认草稿：限价买默认最低卖盘价/数量 1，即时买在有卖盘时默认数量 1；限价卖默认最高买盘价/数量 1，即时卖仅在玩家有 AVAILABLE 且有买盘时默认数量 1；显式 payload 输入会被保留，不会被默认值覆盖；按钮启用条件改为基于生成后的可执行草稿和库存/盘口状态
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionServiceTest`、`TerminalMarketServiceTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalMarketSectionStateTest`、`TerminalMarketSectionContentTest`、`StandardizedSpotMarketServiceTest`；`scripts/deploy-jgb.sh --targets lobby,s2,client` 已构建 runtime jar SHA256 `b105a92c1a77ac6df416ecab64fe2368142beff916f83e10e102f3c684c6b6c9` 并替换 Lobby、S2 与客户端 mods；Lobby `market-smoke-test.sh --target lobby` 通过，DB 有 8 个活跃商品且运行时目录准入 8/8；S2 因 `World/level.dat` 与 `level.dat_old` 读取 EOF 导致世界启动失败，未达到 `Done`，不是本轮 market jar 编译或部署失败

### 2026-06-28 - 标准商品市场空列表自测与运行时目录诊断

- 主题：补齐 `MARKET_STANDARDIZED` 空列表问题的可靠自测链路，区分数据库有数据、运行时目录拒绝和终端 snapshot 未刷新三类原因
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/GregTechStandardizedMetalCatalog.java`、`scripts/market-smoke-test.sh`、`docs/WORKLOG.md`
- 原因：实机页面已能正常显示布局，但商品列表仍为 `共 0 项`；直接看 PostgreSQL 只能证明订单/成交/仓储表存在候选 `product_key`，不能证明这些 key 被 `GregTechStandardizedMetalCatalog` 在 Forge/GT 运行时准入。此前 `appendTradableKeys(...)` 会静默丢弃目录拒绝的 key，导致“DB 有数据但页面为空”缺少日志证据
- 结果：`TerminalMarketService` 在标准商品 snapshot 中记录来自活跃订单、成交记录、玩家订单和玩家 custody 的 DB 候选 key；如果候选 key 存在但运行时目录一个都不准入，会按 30 秒节流输出 `[market-smoke]` 诊断日志和前 12 条拒绝原因；新增 `scripts/market-smoke-test.sh --target lobby|s2`，可直接检查 DB 活跃商品、成交、仓储、服务器 `Done` 状态和最新运行时目录拒绝日志
- 补充：`InstitutionCoreModule` 在市场运行时创建完成后会主动执行标准商品目录 smoke，不需要打开 G 也能在服务器日志里看到 `[market-smoke] Standardized market startup smoke admitted ...` 或 `rejected all ...`；脚本现在要求同时存在 DB 候选和运行时准入日志，否则不会把数据链判定为通过
- 修复：实测发现当前 GTNH `gregtech.api.enums.Materials` 没有 `hasMetalItems()` 方法，旧目录适配器把它当作必需方法导致 8 个 DB 商品全部被拒；已改为兼容式可选检查，并以 `SubTag.METAL` 作为真实金属准入判断，同时保留更具体的反射失败信息
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`InstitutionCoreModuleTest`、`StandardizedSpotMarketServiceTest`、`TerminalMarketServiceTest`、`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已部署 runtime jar SHA256 `8f7145db3b9bff94f62549834dd9202fdd69d740c01e4a2af87988e88b0953e7`；`scripts/market-smoke-test.sh --target lobby` 最终通过，DB 有 8 个活跃商品 key、8 类 recent trades、玩家 custody 有 `AVAILABLE/CLAIMABLE` 数据，Lobby 运行时目录准入 `8/8` 个 DB product keys

### 2026-06-28 - 标准商品市场紧凑文字错位修复与分页/上架现状确认

- 主题：修复 `MARKET_STANDARDIZED` 实机截图中搜索状态行和状态小卡片的文字错位，并核清当前标准商品目录、分页和上架链路的真实能力边界
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：实机截图中 `服务: 在线` 与搜索占位文本容易挤压，`库存`、`可领`、`订单` 三个状态卡把标题、数值和副说明压成三行后在 GTNH 字体下仍有错位风险；同时玩家看到 `共 0 项` 后容易误解为分页第 0 页
- 结果：搜索框改为左侧搜索占位、右侧服务状态的单行布局；三张状态卡改为两行结构，第二行直接显示 `数值 + 语义`，不再渲染第三行副说明；新增/保留布局测试约束，确保搜索状态不横向重叠、状态卡文本不纵向重叠；确认当前标准商品页没有分页按钮，`共 0 项` 表示当前发现的商品数为空，标准商品上架链路为“持有准入标准商品 -> 选中商品 -> 存入 AVAILABLE -> 卖挂牌”
- 验证：`git diff --check` 通过；宿主机无 Java，`java -version` 为 `command not found`；当前会话普通 Docker 被 `/var/run/docker.sock` 权限阻止，sudo 被 `no new privileges` 阻止，因此未能执行 Docker Gradle 测试或部署

### 2026-06-22 - 标准商品市场页内标题与返回按钮清理

- 主题：继续收口 `MARKET_STANDARDIZED` 的实机像素预算，删除标准商品页内的重复标题、说明和返回按钮，让左侧浏览器与右侧交易仪表盘直接占满工作区
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：实机截图中 `商品浏览`、`商品详情`、`交易动作` 标题以及 `返回 MARKET` 按钮继续挤占标准商品页内部高度，导致商品摘要、盘口、库存状态和动作按钮仍有文本错位风险；返回/帮助/刷新应归属终端顶栏，而不是重复占用市场工作区
- 结果：标准商品页左侧浏览器移除内部标题和返回按钮，搜索/状态条直接贴近卡片顶部；右侧详情仪表盘移除内部标题和动作反馈标题，摘要/盘口/库存/动作区按剩余高度重新铺开；状态卡短标签改为 `库存`、`可领`、`订单`，降低窄卡片文字拥挤；测试新增约束，确保标准页左栏不再含按钮、右侧只保留 5 个交易动作按钮
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalShellPanelsScrollTest`、`TerminalMarketSectionContentTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `00ced52418da2b099bd87df5d243f3252139e28674ce76609421e95c454a3f51`，Lobby `latest.log` 到达 `Done (1.630s)!`

### 2026-06-22 - 标准商品市场像素预算修复：去标题占高与右侧卡片防重叠

- 主题：继续按效果图整改 `MARKET_STANDARDIZED`，优先解决标准商品页标题/说明占用工作区、右侧商品摘要/盘口/状态/动作区互相重叠的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：上一轮虽然改为二栏工作台，但标准页仍保留 `TerminalMarketSection` 标题/说明和 `TerminalMarketShell.computeWorkbenchLayout(...)` 的 30px section header 预留；右侧详情区还用固定行距和若干最小高度拼接，在实际 GUI scale 下会让商品摘要、订单簿、状态卡和动作按钮互相压住
- 结果：标准页隐藏自身标题/说明，workbench 从内容区顶部开始布局；右侧详情区改为按实际剩余高度分配摘要、订单簿、状态卡和动作区；订单簿行距按卡片高度压缩，状态卡高度不足时隐藏副标题，商品摘要图标和文字按卡片高度重新定位；未选商品时隐藏 6 个数字输入框，只保留底部动作按钮，减少空状态挤压
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalShellPanelsScrollTest`、`TerminalMarketSectionContentTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `35e61825bef46e9a208880544437e164d32b99b1f515b790e6a50c8cb28f4091`，Lobby `latest.log` 到达 `Done (1.548s)!`

### 2026-06-21 - 标准商品市场工作台整改：二栏浏览器与右侧仪表盘

- 主题：按效果图差异先集中整改 `MARKET_STANDARDIZED`，把标准商品市场从“左/中/右三块独立栏目”收口为“左商品浏览器 + 右交易仪表盘”的工作台结构
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：实机页虽然不再越界，但右侧动作栏仍像独立表单列，导致商品详情、行情、状态和交易按钮被挤碎；效果图中的标准市场应以左侧商品列表为唯一滚动区，右侧为无外部滚动的行情/资产/动作仪表盘
- 结果：`TerminalMarketShell.computeWorkbenchLayout(...)` 改为标准市场二栏布局；左侧浏览器保留搜索/过滤外观、列表滚动和返回入口；右侧详情卡内合并商品摘要、买卖盘/最新成交、资产状态卡与底部紧凑动作条；修复商品摘要右侧指标文字和名称区域的宽度分配，避免窄宽度下互相覆盖
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalShellPanelsScrollTest`、`TerminalMarketSectionContentTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `4724ecd36d9f9ffab9a388fa71fab66506bb1958d0a5d23c48f8f58897c8e1e1`，Lobby `latest.log` 到达 `Done (1.692s)!`

### 2026-06-21 - 记录市场终端效果图差异复盘

- 主题：把 `/home/u24/图片/市场.png` 效果图与当前四张实机市场页面的差异沉淀为独立文档，作为后续子页面重构的对照基准
- 影响范围：`docs/market-terminal-effect-gap-review-2026-06-21.md`、`docs/WORKLOG.md`
- 原因：当前 MARKET 总入口已接近方向，但标准商品、定制商品、汇率市场仍存在工作台结构、信息密度、真实物品图标、动作区边界和低价值说明文字等差距，需要先固定问题清单，避免继续零碎 patch
- 结果：新增独立差异复盘，按 MARKET、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 分别记录与效果图差距，并拆分 UI 层问题、可能后端字段缺口和后续执行顺序
- 验证：文档变更，无编译验证

### 2026-06-21 - 终端视觉系统三次收口：比例圆角、顶栏容器约束与低价值文案清理

- 主题：按实机截图继续修复终端公共壳层，解决圆角不自然、顶栏按钮超出 title bar、导航栏标签/图标可读性不足，以及 MARKET 入口存在低价值说明文案的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/framework/RoundedRectPainter.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeLayout.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalIconPainter.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContent.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreenLayoutTest.java`
- 原因：上一轮已有圆角和图标方向，但圆角只是固定 2px 切角，放大后不够自然；顶栏按钮使用固定最小尺寸，可能反过来超过 title bar 高度；左侧导航受配置和最大宽度限制仍偏窄；MARKET 入口页仍显示“列表每 30 秒自动刷新”和“先选市场类型”这类页面说明，信息价值低
- 结果：`RoundedRectPainter` 改为按控件短边乘固定 ratio 计算半径，并逐行绘制圆角实体/边框；顶栏按钮改为按父容器高度扣内边距后夹紧，从右向左布局，新增紧凑 title bar 子控件不越界测试；导航栏默认宽度和最小宽度上调，避免短标签裁剪；图标改为固定点阵模板再按控件尺寸缩放，替代临时线段拼图；MARKET 总入口 lead 改为规则摘要，移除 30 秒刷新页脚，帮助卡改为交易规则提示
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `5f9384e79529bd87c984bedc6f307f8b81a520ca0381c2c2c51a4644ed22c0d1`，Lobby `latest.log` 到达 `Done (1.460s)!`

### 2026-06-21 - 终端视觉系统二次收口：圆角、导航宽度与图标可读性

- 主题：根据实机截图修复终端壳层视觉问题，重点解决左侧导航图标成块、标签裁剪、顶栏按钮过大和面板方角问题
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/framework/RoundedRectPainter.java`、`src/main/java/com/jsirgalaxybase/client/gui/framework/TexturedCanvasPanel.java`、`src/main/java/com/jsirgalaxybase/client/gui/framework/ButtonPanel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeLayout.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalIconPainter.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalIconButtonPanel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketVisuals.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreenLayoutTest.java`
- 原因：上一轮深色主题和图标体系已经接近方向，但实机下导航栏仍过窄，短标签被裁剪；顶栏图标按钮按栏高放大，显得拥挤并接近越界；导航图标使用填充块绘制，小尺寸下会变成白色块；通用面板和按钮仍是硬直角，和效果图的圆角质感差距明显
- 结果：新增 `RoundedRectPainter`，将通用面板、按钮、图标按钮和市场卡片切换为像素伪圆角；导航栏默认宽度提升到可容纳图标 + 两字标签，新增回归测试防止退回窄栏；顶栏图标按钮缩小为紧凑规格，信号图标同步缩小；导航/顶栏图标改为细线像素绘制，减少白块感
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `965525fa6425aa00eb904e68c5bafca2fb479f576d6ca30e818b4fa4b0c6149f`，Lobby `latest.log` 到达 `Done (1.598s)!`

### 2026-06-21 - 终端视觉系统收口：深色主题、顶栏图标按钮与导航图标

- 主题：从市场单页修补上升到终端壳层视觉系统，按效果图方向统一配色、导航图标和右上角图标按钮
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/theme/TerminalThemeRegistry.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalIconKind.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalIconPainter.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalIconButtonPanel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`
- 原因：市场总入口已基本解决越界和重叠，但终端壳层仍是文字按钮和浅蓝灰面板，和效果图中的深色玻璃面板、图标导航、右上角帮助/刷新/返回/关闭图标差距明显；市场说明卡内部的“查看说明”按钮也会和说明文字抢空间
- 结果：新增像素图标绘制器和图标按钮组件；左侧导航改为图标 + 文本，支持首页、职业、公共、市场、传送、银行图标；顶栏右侧改为信号、刷新、帮助、返回、关闭图标按钮；市场说明卡去掉内部按钮，只保留摘要，详细说明统一走顶栏帮助；主题色改为更深的蓝黑面板、暗卡片和蓝色按钮体系
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `d499e4fb7b4f1958a9bb344726d443a79e2c383b8b90de5690a789bd329000b0`，Lobby `latest.log` 到达 `Done (1.684s)!`

### 2026-06-21 - 市场终端 Round 3 复盘修复：非列表区去滚动与固定工作台

- 主题：按实机反馈继续收口市场 UI，可变列表保留独立滚动，其他核心信息区必须在终端边界内固定显示
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalCustomMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalExchangeMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`
- 原因：上一轮虽然解决了明显出界，但总入口卡仍会文字/按钮抢空间，标准商品、定制商品和汇率市场的详情/动作区仍以滚动容器承载普通信息，导致玩家必须滚动才能看完非列表内容，不符合效果图的工作台式信息密度
- 结果：MARKET 入口卡图标和文本槽位压缩，避免 CTA 覆盖正文；标准商品页改为左商品浏览器滚动 + 中行情/仓储固定栏 + 右交易动作固定栏；定制市场改为左挂牌列表滚动 + 右商品详情/反馈/动作固定栏；汇率市场改为左兑换目标/参数 + 右汇率详情/执行固定栏；真实物品图标链路继续保留，无法解析时才退回绘制 badge
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `d4b3d8f698ff8b1876df9b360c743c7b334812fe22c0de0099b5ea930aa062a1`，Lobby `latest.log` 到达 `Done (1.357s)!`

### 2026-06-21 - 市场终端 Round 3 收口：真实物品图标链路

- 主题：继续收口市场终端视觉密度，补齐标准商品、定制挂牌、汇率报价中“只画色块、不显示真实物品”的核心缺口
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketVisuals.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalCustomMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalExchangeMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalCustomMarketSectionModel.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalCustomMarketSectionSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalCustomMarketSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSectionService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java`
- 原因：效果图的市场页以物品图标作为浏览与详情的主要视觉锚点；此前标准商品和定制市场虽然有 badge，但没有真正解析 `ItemStack`，定制 listing 行甚至没有把每行 item identity 传到客户端
- 结果：`TerminalMarketVisuals` 支持从 `modid:item:meta`、`modid:item@meta`、`modid:item#meta` 等 iconRef 渲染真实 MC 物品图标，失败时退回原有 badge；标准商品列表/详情使用 productKey 渲染；定制市场 snapshot/model/network 增加 active/selling/pending listing icon refs，列表和详情均尝试渲染真实物品；汇率报价 hero 使用当前手持物品 id 渲染输入资产，目标资产仍保留货币图标
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `7cb747129fc524ed231d10564940b5c63bc00fdc71729530d0b49e30582bc378`，Lobby `latest.log` 到达 `Done (1.545s)!`

### 2026-06-20 - 市场终端 Round 3：视觉密度与物品感增强

- 主题：继续对照市场效果图优化 MARKET 总入口、标准商品、定制商品、汇率市场的视觉表达，优先解决“栏目紧张、图标少、缺少物品感”的实机观感问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketVisuals.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalCustomMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalExchangeMarketSection.java`、`docs/WORKLOG.md`
- 原因：当前市场功能链路已经基本存在，但客户端 snapshot 尚未携带完整 `ItemStack` 图标字段，页面仍偏文字面板，和效果图中的物品浏览器、listing 卡、汇率卡、状态卡相比缺少视觉锚点
- 结果：新增 `TerminalMarketVisuals` 作为轻量市场视觉 helper，统一绘制市场图标、物品 badge、货币 badge 和状态点；MARKET 入口卡图标与文字/按钮边界重新排布；标准商品浏览器条目、商品摘要和状态卡加入物品/状态视觉锚点并放松行高；定制市场 listing 改成带物品 badge 的可选卡片，详情页加入商品 hero；汇率市场目标列表和报价详情加入货币/汇率视觉卡
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`

### 2026-06-20 - 市场终端 Round 2 复盘修复：仅裁剪网络尾部 padding，避免列表错位

- 主题：复盘 Round 2 代码后修复 viewmodel padding 过滤策略，避免 custom listing 与 exchange target 的并行数组在异常空字段下错位
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalCustomMarketSectionModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalExchangeMarketSectionModel.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContentTest.java`、`docs/WORKLOG.md`
- 原因：Round 2 初版为解决固定长度网络数组补空导致的假空行，采用了删除所有空字符串的 compact 策略；该策略能处理正常尾部 padding，但如果未来并行数组中间出现空字段，可能导致 `ids/lines` 或 `codes/labels` 下标错位
- 结果：过滤策略改为只裁剪末尾 padding，保留中间空字段的下标位置；新增测试覆盖“尾部 padding 被清理”和“中间空字段不触发错位”两种情况
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`、`TerminalExchangeQuoteViewTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已重新部署，runtime jar SHA256 为 `d0be046a9e13840b3898845d2045a40f36ec5341840e167e39b99510b2b55d2d`，Lobby `latest.log` 到达 `Done (1.433s)!`

### 2026-06-20 - 市场终端 Round 2：空态、数据映射与 disabled reason 收口

- 主题：执行市场终端子页面整改计划 Round 2，区分后端无数据和 UI 表达问题，先把空态、数据来源说明和动作禁用原因收清楚
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContent.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalCustomMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalExchangeMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalCustomMarketSectionModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalExchangeMarketSectionModel.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContentTest.java`、`docs/market-terminal-child-pages-defect-plan-2026-06-20.md`、`docs/WORKLOG.md`
- 原因：实机市场子页仍容易把“后端没有商品/listing/quote”表现成未完成 UI；同时 custom listing 网络数组会被固定长度 padding，客户端此前会把空字符串当成空列表行渲染，造成大面积假骨架；exchange 未选择标的、无手持、规则禁兑等状态也缺少清晰禁用原因
- 结果：标准商品市场新增空目录/未选商品原因推导，浏览器无商品时显示正式空态，交易动作提示改为说明“没有可交易标准商品/先选商品”；custom/exchange viewmodel 过滤网络 padding 空字符串，custom 详情追加不可执行原因并统计真实 listing 数，exchange quote 追加不可执行原因并处理无可选标的；文档记录了标准商品目录、行情、custom listing、exchange quote 的实际数据来源和后端缺口
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`、`TerminalExchangeQuoteViewTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `fd9986a5ad90c156a11f7865bba192d4982b3a9e2bcf25ce87e9350a03f86418`，Lobby `latest.log` 到达 `Done (1.916s)!`

### 2026-06-20 - 市场终端 Round 1 复盘修复：紧凑边界与空 handler 防御

- 主题：复盘 Round 1 代码后修复已发现的确定性纰漏，重点补极小 GUI scale 边界和空 action handler 防御
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalCustomMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalExchangeMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：复盘发现 `TerminalCustomMarketSection` 的购买/下架/领取按钮 action 没有显式空 handler 防御；同时 custom/exchange 在极小逻辑尺寸下仍保留固定按钮宽度和堆叠最小高度，理论上可能导致按钮或子卡片越过父容器；`TerminalMarketShell` 也存在用 `Math.max(180/140)` 把布局高度撑出父容器的边界风险
- 结果：custom 动作按钮统一通过安全包装执行；custom/exchange 的堆叠高度、返回按钮宽度、底部动作按钮宽度和内部滚动区高度都改为按父容器实际尺寸收敛；`TerminalMarketShell` 不再用固定最小可用高度撑开市场布局；新增紧凑尺寸回归测试，覆盖 overview、standardized、custom、exchange 在小逻辑 bounds 下直接子控件不越界
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalShellPanelsScrollTest`、`TerminalMarketSectionContentTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已重新部署，runtime jar SHA256 为 `0619b87bd734b1f7854c085d56ec7d8e26de1faf2b72c8450cfc95cabecca70d`，Lobby `latest.log` 到达 `Done (1.581s)!`

### 2026-06-20 - 市场终端 Round 1：子页面结构重排与边界收口

- 主题：执行 `market-terminal-child-pages-defect-plan-2026-06-20.md` 的 Round 1，先修市场子页面结构与边界，不扩后端数据和制度模型
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalCustomMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalExchangeMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：MARKET 总入口虽然已经接近效果图方向，但三个子页面仍表现为通用面板拼接：标准商品市场是三块独立竖栏，定制市场和汇率市场的动作区容易漂浮或贴边，入口卡和共享状态也仍有文字/按钮重叠风险
- 结果：MARKET 总入口卡片内部改成固定图标/标题/摘要/状态/CTA 纵向关系，状态/帮助卡只显示可容纳内容；标准商品市场改成左商品浏览器 + 右侧上下组合工作台，右侧上方承接商品详情/盘口/状态，下方承接交易动作；定制商品市场收成左挂牌浏览器 + 右详情/动作；汇率市场收成左兑换参数 + 右报价详情/动作；自定义和汇率页面不再使用独立底部动作卡，按钮按右侧工作区宽度收进终端内部
- 验证：`git diff --check` 通过；按 Docker Gradle 路径执行定向测试通过：`TerminalShellPanelsScrollTest`、`TerminalMarketSectionContentTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `5c004f2e74f8614c7c847bc722a91ff441873cbbc74532d905d05bb6f9ba94cf`，Lobby `latest.log` 到达 `Done (1.516s)!`

### 2026-06-20 - 记录市场终端子页面缺陷与三轮整改计划

- 主题：把当前实机截图中已经确认的市场子页面缺陷、后端数据缺口和后续三轮整改计划沉淀为正式文档
- 影响范围：`docs/market-terminal-child-pages-defect-plan-2026-06-20.md`、`docs/WORKLOG.md`
- 原因：MARKET 总入口已经比早期说明页明显改善，但 `MARKET_STANDARDIZED`、`MARKET_CUSTOM`、`MARKET_EXCHANGE` 仍然存在结构不像效果图、内容/按钮越界或贴边、空数据状态不专业、后端数据缺口与 UI 问题混在一起的问题；如果继续零散 patch，容易反复在截图反馈中修局部而不收敛
- 结果：新增缺陷与计划文档，明确当前问题分为总入口重叠风险、标准商品市场工作台结构错误、定制商品市场 listing-first 表达不足、汇率市场 quote-first 表达不足，以及标准商品目录/行情、定制挂牌、汇率报价等数据缺口；后续按三轮推进：Round 1 子页面结构重排与边界收口，Round 2 空态/数据映射/后端缺口识别，Round 3 视觉密度/交互动作/统一验收
- 验证：本轮为产品与缺陷文档收口，无代码与测试变更

### 2026-06-20 - 市场终端布局修正：GUI scale 窄屏误判与子市场外溢

- 主题：继续对照市场效果图修正实机布局，优先解决“明明窗口很宽但市场页仍竖排/外溢”的结构问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalCustomMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalExchangeMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：GTNH GUI scale 会把实际屏幕宽度换算成更小的逻辑宽度，原先 `500/520` 的堆叠阈值会把大窗口误判成窄屏，导致 MARKET 总入口三卡变成纵向大条、标准商品工作台退回上下/拥挤结构；同时 `MARKET_CUSTOM` 和 `MARKET_EXCHANGE` 还被 `TerminalShellPanels` 外层滚动二次包裹，局部页面高度容易超出终端边界
- 结果：市场专用布局的堆叠阈值下调到更保守的逻辑宽度；标准商品页三栏宽度比例收紧，避免右侧动作栏挤压中栏；定制商品和汇率市场不再被外层 `VerticalScrollPanel` 包裹，改为和标准商品页一样由页面内部管理滚动；补充布局测试，覆盖 460 逻辑宽度下 MARKET 总入口仍保持三入口同排，以及 custom/exchange 不出现外层滚动壳
- 验证：`git diff --check` 通过；当前 Codex 沙箱无法访问 Docker socket，宿主机也没有 Java，因此本轮未能执行 Gradle/JUnit 与部署，需在允许 Docker 的环境下重跑 `TerminalShellPanelsScrollTest` 后再灰度

### 2026-06-15 - 市场终端重构：总入口改三主卡，标准商品页改专用工作台

- 主题：把 `MARKET` 和 `MARKET_STANDARDIZED` 从 generic section 结构真正拉回到效果图方向，不再继续做文字段落和卡片堆叠式微调
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：上一轮虽然已经把市场页和标准商品页从语义上分开，但实际实机仍然表现为“总入口纵向堆块”和“标准商品页上下大框”；问题本质不是文案，而是顶层布局骨架仍然在沿用通用终端 section
- 结果：`MARKET` 根页现改成“三张主入口卡 + 下方状态卡/帮助卡 + 底部刷新脚注”的专用总入口；`MARKET_STANDARDIZED` 现改成三栏工作台：左栏商品浏览，中栏商品摘要/订单簿/状态卡，右栏交易动作块与最近反馈，局部滚动只保留在各自工作区内部；同时 `TerminalMarketShell` 重算了市场页的总入口和标准商品页主布局比例
- 验证：`git diff --check` 通过；宿主机无 Java，按仓库 Docker Gradle 路径执行定向测试：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest` 通过

### 2026-06-15 - 市场终端结构收口：市场共享骨架 + 标准商品工作台

- 主题：不再继续做 MARKET 零碎 UI patch，而是把 Phase 1 和 Phase 2 收口成可复用的市场页骨架，并让 `MARKET_STANDARDIZED` 真正落在这套骨架上
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContent.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContentTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：当前 `MARKET` 根页和 `MARKET_STANDARDIZED` 虽然都能用，但仍然过于依赖 generic terminal section 表达；缺的不是一个按钮，而是市场页自己的顶栏上下文、入口布局和工作台布局抽象
- 结果：新增轻量 `TerminalMarketShell` 作为市场共享骨架，统一承载市场页顶栏上下文、MARKET 根页入口布局和标准商品页三栏工作台布局；`MARKET` 根页从“四卡说明区”收口成“一条共享状态 + 三张主入口卡 + 帮助按钮”；`MARKET_STANDARDIZED` 继续保持正式三栏，但标题、卡片职责和滚动边界都改成更明确的交易工作台语义；全程未改服务端字段、业务链或 terminal 主路由
- 验证：`git diff --check` 通过；宿主机无 Java，按仓库 Docker Gradle 路径执行定向测试：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest` 通过

### 2026-06-14 - 市场终端 Phase 2：标准商品市场工作台收口

- 主题：把 `MARKET_STANDARDIZED` 从“已有功能页”继续收口成标准商品市场正式工作台，不扩后端业务模型
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContent.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContentTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionStateTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：当前标准商品市场已经具备真实动作主链，但页面仍偏“功能集合页”，商品浏览、详情层级、动作 enable 条件和最近反馈没有完全收口成顺手的玩家工作流
- 结果：标准商品市场现已按三栏工作台进一步收口：左栏改成更明确的商品目录浏览器并弱化说明字段；中栏按“当前商品 -> 核心行情 -> 个人状态 -> 我的订单 -> CLAIMABLE -> 规则提示”重排；右栏改成交易操作台，分别为存入、限价买、限价卖、即时买、即时卖提供更明确的预览与输入引导，同时继续复用现有 popup 确认链与 snapshot 回写；市场页帮助 popup 也补上标准商品页专属说明
- 验证：`git diff --check` 通过；宿主机无 Java，按仓库 Docker Gradle 路径执行定向测试：`TerminalMarketSectionContentTest`、`TerminalMarketSectionStateTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest` 通过

### 2026-06-14 - 市场终端 Phase 1：MARKET 总入口与共享壳收口

- 主题：把 `MARKET` 根页从说明型混排 section 收口成真正的三市场入口页，并顺手固定市场共享壳层的首轮约定
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContent.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContentTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：当前 `MARKET` 根页虽然已有三类市场路由，但 UI 仍更像“说明页 + 字段堆叠”，玩家首屏看到的是解释文本而不是市场分流入口，同时顶栏帮助/刷新和页面主体之间也缺少市场专属约束
- 结果：`MARKET` 根页已改成“共享状态条 + 帮助入口 + 标准/定制/汇率三张入口卡”的专用入口布局，不再在根页混排交易正文；市场页顶栏现对 `MARKET` / `MARKET_STANDARDIZED` / `MARKET_CUSTOM` / `MARKET_EXCHANGE` 统一使用更紧凑的市场上下文文案，并把刷新提升到顶栏小按钮；帮助说明改为通过共享 `帮助` 按钮和市场页 popup 承接，明确 `MARKET` 根页只负责分流与轻量状态，工作页正文继续留给各自子市场
- 验证：`git diff --check` 通过；宿主机无 Java，按仓库 Docker Gradle 路径执行定向测试：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalHomeScreenModelTest`、`TerminalMarketActionMessageFactoryTest` 通过

### 2026-06-14 - 新增市场终端分阶段执行计划

- 主题：把当前已确认的市场终端方向收口成一份正式阶段计划，便于后续持续开发、AI 交接和验收追溯
- 影响范围：`docs/market-terminal-phased-execution-plan-2026-06-14.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前已经明确 MARKET 根页不能再做成说明页，标准商品市场、定制商品市场、汇率市场也不应按统一大桶继续演化；如果不先钉执行顺序，后续很容易在“是不是一页一个阶段”上反复摇摆
- 结果：新增一份正式计划文档，明确采用 `Phase 1 共享市场壳与 MARKET 总入口 -> Phase 2 标准商品市场 -> Phase 3 定制商品市场 -> Phase 4 汇率市场 -> Phase 5 统一收口` 的顺序，并把每阶段的目标、范围、非目标、产出物和验收标准写清
- 验证：本轮为产品文档收口，无代码与测试变更

### 2026-06-14 - 新增市场仓库 v1 制度边界草案

- 主题：在市场终端概念方向基本确认后，先把“仓库到底是什么”从模糊的页面想象，收口成正式制度边界草案
- 影响范围：`docs/market-warehouse-v1-product-boundary-draft.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前市场系统已经在向服务器核心玩法内核演化；如果不先定义仓库边界，后续很容易把市场托管仓、企业私仓、公共仓和普通大箱子 GUI 混成一团，最终反过来破坏三市场与银行结算的制度模型
- 结果：新增 `市场仓库 v1 产品边界草案`，明确仓库首先是资产状态系统，v1 只优先落标准商品市场托管仓；同时把现实模型拆成 `市场托管仓 / 企业私有仓 / 公共仓` 三类，并强调企业仓与公共仓可以和市场发生流转，但不应直接与市场托管仓混成同一个库存概念
- 验证：本轮为产品文档收口，无代码与测试变更

### 2026-06-14 - 标准商品市场终端窄修复：银行快照回写顺序与即时成交残单语义

- 主题：对“标准商品市场终端收口”后的两个一致性问题做窄修复，不扩市场功能、不改 ServerTools
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketServiceTest.java`、`docs/WORKLOG.md`
- 原因：一是标准商品市场动作完成后，终端快照仍可能先读取旧银行页摘要，导致余额显示落后于本次资金变动；二是即时买入/卖出部分成交后，如果“剩余自动撤回”失败，终端会把整次操作误报成笼统失败，掩盖“已真实部分成交且残单可能仍然挂着”的事实
- 结果：`TerminalService.buildTerminalSnapshot(...)` 现已先执行 market action 再生成 bank snapshot，保证市场动作后的银行摘要读取的是最新状态；`TerminalMarketService` 现把即时买入/卖出的“部分成交但残余撤回失败”单独转成 warning 反馈，明确展示已成交数量、`orderId`、剩余未撤数量、可能仍处于开放/托管状态以及原始撤回失败原因，引导玩家去“我的订单”继续处理
- 验证：`git diff --check` 通过；宿主机 `java` 不可用，按仓库已验证 Docker Gradle 路径执行定向测试：`TerminalServiceTest`、`TerminalMarketServiceTest`、`StandardizedSpotMarketServiceTest` 通过
- 备注：首次 Docker Gradle 运行命中过往进程持有的 workspace `.gradle` 锁；随后改用仓库脚本同款挂载参数（独立 `GRADLE_USER_HOME` + `--project-cache-dir` + `--no-daemon`）重跑通过

### 2026-06-14 - ServerTools 传送页按最终效果图方向做一轮产品化收口

- 主题：在传送链已验证可用后，继续按最终效果图方向把 `SERVER_TOOLS` 页面收口到更接近成品的状态，不扩后端动作范围
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`docs/WORKLOG.md`
- 原因：当前页面主体结构已经稳定，但仍存在两类产品缺口：一是文档要求中的“已知服务器目录”在实际 UI 中不可见；二是“最近传送状态”只突出单条结果，没有把已有 recent ticket 列表转成可读历史摘要
- 结果：左侧传送点卡片顶部新增紧凑服务器目录摘要，明确当前服与已知目录；右侧最近状态卡新增历史条数与紧凑历史摘要；中栏布局随之调整但仍保持列表/详情两块都在终端内滚动；本轮继续保持 warp-only 终端范围，不把 home/back/spawn/rtp/tpa 扩进页面
- 验证：`git diff --check` 通过；`./scripts/build-mod.sh --task assemble` 通过；Docker Gradle 定向测试 `TerminalServiceTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest` 通过

### 2026-06-13 - 修复 ServerTools 传送页内容越过终端边界

- 主题：修复 `SERVER_TOOLS` 三段式传送页中列表栏和右侧详情栏越过终端底边的问题，并恢复页面内部滚动
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`docs/terminal-servertools-overflow-bug-2026-06-13.md`、`docs/WORKLOG.md`
- 原因：上一轮为了压缩视觉结构，把右侧详情区从滚动 viewport 改成了直接堆叠，同时 `setBounds` 仍用固定最小高度撑开 section；在实际 Minecraft GUI 高度不足时，中栏和右栏会突破父容器底边，按钮和状态卡被挤到终端外
- 结果：`SERVER_TOOLS` 外层布局只使用父容器真实高度，不再用最小高度撑开；中栏传送点列表继续保留独立 `VerticalScrollPanel`，底部自动刷新提示按剩余高度参与布局；右侧详情、最近状态、反馈、风险提示和确认按钮重新进入同一个内部 `VerticalScrollPanel`，内容多时在终端内部滚轮浏览而不是向外溢出
- 验证：`./scripts/build-mod.sh --task assemble` 通过；`scripts/deploy-gray-chain.sh --jar build/libs/jsirgalaxybase-ed7e2cf-main+ed7e2cfb16-dirty.jar --targets lobby,client` 已部署，jar SHA256 `d1486cc48da02a260d6f75db00bc8a56a8e5f6c19582619007e494ff54d2b365`；Lobby 日志到达 `Done (1.424s)!`

### 2026-06-09 - 修正 ServerTools 页被误判窄屏并补右侧工作台滚动

- 主题：针对实际游戏截图中 `SERVER_TOOLS` 仍退回“上列表 / 下详情”的两段堆叠问题，继续收口传送页布局判定和内部滚动
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：虽然上一轮已经把页面朝效果图重组，但 Minecraft GUI 坐标下的宽度阈值仍然过高，导致常见 GUI scale 下被误判成窄屏并回退到上下堆叠；同时右侧工作台没有独立滚动容器，长内容只能把页面往下顶
- 结果：`SERVER_TOOLS` 窄屏阈值已明显下调，常见宽度下优先保持“左导航 + 中列表 + 右工作台”的三段结构；右侧工作台改成独立 `VerticalScrollPanel`，超长详情/状态可直接滚轮浏览；顶栏对 `SERVER_TOOLS` 也不再拼接长串 detail，只保留更紧凑的传送路径上下文
- 验证：`git diff --check` 通过；Docker Gradle 定向测试 `TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalServiceTest` 通过

### 2026-06-09 - 开始把 ServerTools 传送页收口成成品化工作台

- 主题：把 `SERVER_TOOLS` 页面从“后端已通但仍偏工程拼装”的 v1 形态，推进成更接近目标效果图的专用传送工作台
- 影响范围：`docs/terminal-ui-redesign-toward-concept-mockup-2026-06-07.md`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`scripts/lib/deploy-common.sh`
- 原因：当前 `warp` 主链和终端确认链已可用，但与效果图相比，主要差距已不在后端，而在于右侧仍像三张平行卡片、中栏列表还不够产品化、页面缺少图标化层级锚点
- 结果：文档已明确新增两条判断：
  - 右侧应收口为一个连续工作区，而不是 `detail/recent/action` 三张并列卡
  - 现阶段应优先采用轻量绘制的图标化表达，不等待新的素材或纹理资源链
  同时代码开始按该方向重做 `TerminalServerToolsSection`，保留现有 warp 后端调用链，仅重组页面工作区与视觉层级；部署脚本实测时出现 `lobby` 已经 `Done` 但仍被误判失败，因此把 gray-chain 的 `RUNNING` / `Done` 等待窗口进一步放宽，减少 GTNH 慢启动时的假失败

### 2026-06-08 - 终端改成顶部锚定并强制现网比例回落到更小值

- 主题：继续收口终端共享壳层，解决“左栏与内容区缝隙仍大”和“实际游戏里终端看起来仍接近满屏”两个直接可见问题
- 影响范围：`src/main/java/com/jsirgalaxybase/config/ModConfiguration.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeLayout.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalLayoutMetrics.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalHomeSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreenLayoutTest.java`、`docs/WORKLOG.md`
- 原因：虽然上一版已经删除普通页底栏并压缩了字号，但客户端与灰链配置文件仍保留旧的 `0.88 / 0.72 / 0.12`，叠加布局仍在用“伪居中”算法，导致用户实际看到的终端尺寸和位置几乎没有肉眼差异
- 结果：终端默认比例进一步下调到 `width=0.72 / height=0.50 / nav=0.07`；`TerminalHomeLayout` 改成显式顶部锚定，并同步降低最大宽高上限、内边距、导航最大宽度和正文区 gap；左栏继续去外框，仅保留与内容区之间的细竖线；终端共享文本、按钮和 section 内边距再缩一档；部署后直接把客户端、Lobby、S2 的现网 `jsirgalaxybase.cfg` 同步写成新的更小比例，避免旧配置继续覆盖视觉结果
- 验证：`git diff --check` 通过；Docker Gradle 定向测试 `TerminalHomeScreenLayoutTest`、`TerminalShellPanelsScrollTest`、`TerminalServiceTest` 通过；`scripts/deploy-jgb.sh --targets lobby,s2,client` 已完成构建与部署，最新运行 jar 哈希 `2c72f7f3750fb5392483e64f36dba9d58e73941b128d562cc4beaeffffad144f`

### 2026-06-08 - 终端壳层改为常显左栏与上方 75% 工作台

- 主题：把终端壳层从“居中弹窗 + hover 导航”收口到更接近目标图的“常显左栏 + 紧凑顶栏 + 上方固定工作台”，并继续压缩 `SERVER_TOOLS` 页的内容竞争
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeLayout.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreenLayoutTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：实际游戏内终端仍明显偏离目标图，核心问题不再是单页 spacing，而是壳层仍保留 hover 导航和居中弹窗比例，导致下方物品栏不可稳定保留、导航形态不对、`传送` 页主体空间不够完整
- 结果：`TerminalHomeLayout` 现改为更接近屏幕 75% 高度的上方锚定布局，并显式预留底部热键栏可见空间；导航轨默认常显，不再依赖 `EdgeRevealNavigationPanel`；顶栏压缩为更紧凑的单行状态带；`SERVER_TOOLS` 页取消底部 footer 竞争，把空间让给左侧传送点列表和右侧详情/状态/确认区，同时把列表改成单一主卡片并补上底部自动刷新提示
- 验证：`git diff --check` 通过；Docker Gradle 定向测试 `TerminalHomeScreenLayoutTest`、`TerminalShellPanelsScrollTest`、`TerminalServiceTest` 通过；`assemble` 首次尝试被远端 TLS handshake 中断，重试后成功

### 2026-06-08 - 增加灰链与客户端一键部署脚本

- 主题：把反复手工执行的 `build -> 替换 jar -> 重启 lobby/s2 -> 校验 hash/状态` 收口为可重复脚本，减少后续终端与 ServerTools 开发中的部署噪音
- 影响范围：`scripts/lib/deploy-common.sh`、`scripts/build-mod.sh`、`scripts/deploy-gray-chain.sh`、`scripts/deploy-jgb.sh`、`docs/WORKLOG.md`
- 原因：当前 `JsirGalaxyBase` 的验证节奏高度依赖灰链 `Lobby/S2` 和 Prism 客户端实例；如果每次都靠人工逐步部署，不仅重复，而且容易漏掉 hash 一致性、日志 `Done` 校验和旧 jar 清理
- 结果：新增三层脚本链：
  - `scripts/build-mod.sh`：用已验证的 Docker Gradle 路径构建运行 jar
  - `scripts/deploy-gray-chain.sh`：备份并清理旧 `jsirgalaxybase*.jar`，替换 `lobby/s2/client`，重启灰链服，校验 `RUNNING`、`Done` 和哈希一致性
  - `scripts/deploy-jgb.sh`：总控脚本，串联 build/deploy，并支持可选 `--launch-client`
- 验证：`bash -n` 通过；`--dry-run` 串联通过；`scripts/deploy-jgb.sh --skip-build --targets lobby,s2,client` 已真实运行成功，完成 `lobby/s2/client` 同步与哈希校验，不触碰 `S1`

### 2026-06-08 - 终端继续压缩到更接近效果图的密度

- 主题：继续收口终端壳层，把“物品栏可见、左栏更窄、字号更小、普通页无底栏”落成共享终端行为
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/framework/LabelPanel.java`、`src/main/java/com/jsirgalaxybase/client/gui/framework/ButtonPanel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalLayoutMetrics.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeLayout.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreenLayoutTest.java`、`docs/WORKLOG.md`
- 原因：上一轮虽然已经改成常显左栏和上方工作台，但实际游戏内仍然偏大；普通页底部 `刷新分区 / 关闭终端` 仍然浪费空间，导航与文字密度也还明显高于目标效果图
- 结果：普通页底部 footer 已删除；壳层比例、上方锚定和左侧导航宽度进一步收紧；终端共享 `LabelPanel` 与 `ButtonPanel` 增加缩放能力，并由 `TerminalPanelFactory` 默认启用更小的终端字号；`TerminalLayoutMetrics` 同步下调行高、按钮高和内边距，使同样区域内可容纳更多内容
- 验证：`git diff --check` 通过；Docker Gradle 定向测试 `TerminalHomeScreenLayoutTest`、`TerminalShellPanelsScrollTest`、`TerminalServiceTest` 通过；`scripts/deploy-jgb.sh --targets lobby,s2,client` 已真实构建并完成新 jar 部署

### 2026-06-08 - 终端壳层高度和传送页栏宽继续整改

- 主题：针对实际游戏内仍然偏满屏的问题，继续压低终端整体高度、扩大底部热键栏预留，并收窄 `SERVER_TOOLS` 传送页中栏
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeLayout.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreenLayoutTest.java`、`docs/WORKLOG.md`
- 原因：前一版虽然已经删除底部 footer 和缩小字号，但当前布局高度比例仍然偏大，`SERVER_TOOLS` 中栏也占用过多横向空间，右侧详情区没有充分接近目标图
- 结果：默认终端高度比例从偏大的 0.79 级别压到 0.68，并提高底部预留空间；导航轨最大宽度进一步收紧；传送页中栏从约 38% 收到约 34%，卡片 gap、padding、传送点行高和右侧详情/状态/action 分区高度全部压缩，给右侧内容更多横向空间
- 验证：`git diff --check` 通过；Docker Gradle 定向测试 `TerminalHomeScreenLayoutTest`、`TerminalShellPanelsScrollTest`、`TerminalServiceTest` 通过；`scripts/deploy-jgb.sh --targets lobby,s2,client` 已真实构建并部署，jar 哈希 `82191c0dea739f2002d7170ffb963cb8db90772909482a8ea6b7845991c791f3`

### 2026-06-07 - 记录传送页与效果图逐项对比后的产品结论更新

- 主题：把 `传送 / 群组服` 当前运行页面与目标效果图做逐项对比，并把“问题到底是什么”从模糊的 UI 不满意收口成可执行的产品结论
- 影响范围：`docs/terminal-ui-redesign-toward-concept-mockup-2026-06-07.md`、`docs/terminal-servertools-page-v1-acceptance-review-2026-06-06.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前页面虽然已经接通真实 warp 后端，也已经能在终端中进入 `传送` 页，但和目标图相比，核心差距并不是 hover 行为或单个 spacing，而是页面仍沿用 generic section/scroll 模型，导致信息架构、主次动作和内容分区都不对
- 结果：文档中已明确记录六类关键差距：`信息架构不对`、`导航形态不对`、`warp 列表不是稳定中栏`、`右栏详情不是结构化面板`、`主次动作关系错误`、`整体仍像系统说明页而非玩家工具页`；同时把后续方向明确改成“保持后端链不变，但把 `传送 / 群组服` 重做成常驻窄导航 + 中栏列表 + 右栏详情/状态/确认按钮的专用传送工具页”，不再把下一轮定义成单纯 hover 或密度微调

### 2026-06-07 - 记录终端朝效果图方向的整体 UI 改造设计与阶段计划

- 主题：在 `Lobby <-> S2` 的真实 warp 链路已经验证通过后，把当前终端工作的主阻塞从“传送是否可用”正式切换为“界面是否好用”，并沉淀一份面向效果图方向的整体设计与实施计划
- 影响范围：`docs/terminal-ui-redesign-toward-concept-mockup-2026-06-07.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前 BetterQuesting 风格终端在功能迁移上已经覆盖银行、市场、群组服传送等正式页面，但实际游戏内使用仍然存在导航过重、顶栏浪费高度、传送页信息层级混杂等问题；需要先确定“继续坚持当前游戏内 GUI 路线，但重做交互结构”的产品决策，再开始阶段性实施
- 结果：新增一份正式设计文档，明确不回退到纯 web 路线、不重开 BetterQuesting 风格技术选型，而是把终端重做拆成四段：`Phase 1 导航壳重做`、`Phase 2 顶栏与全局状态重做`、`Phase 3 传送页专项重做`、`Phase 4 肉眼观察收口`；其中当前后端基线已确认为 `Lobby <-> S2` warp 主链可用，下一轮执行应从导航壳开始

### 2026-06-06 - 验收 ServerTools 群组服终端页 v1 并产出收口 Prompt

- 主题：回顾当前工作树中已出现的 `SERVER_TOOLS` 终端页面实现，判断它是否已经接入真实 ServerTools warp 后端，并把剩余缺口收口成下一轮执行 prompt
- 影响范围：`docs/terminal-servertools-page-v1-acceptance-review-2026-06-06.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前方向已经确认要推进群组服工具和市场产品化；检查代码时发现 ServerTools 终端页并非缺失，而是已有 v1 实现，需要避免后续开发 AI 从零重写
- 结果：确认 `TerminalPage.SERVER_TOOLS`、`SERVER_TOOLS_REFRESH / SELECT_WARP / CONFIRM_WARP`、snapshot / payload / viewmodel / network / client section 已接入；默认服务端 facade 会读取 `ServerToolsModule`、warp 列表和 server directory，确认 warp 复用 `PlayerTeleportService.prepareWarpTeleport(...)` 与 `ServerToolsModule.dispatchTeleport(...)`；剩余缺口收口为 recent transfer ticket 状态展示、默认 facade 后端调用测试、游戏内鼠标浏览验证和 UI 密度复测
- 验证：本轮前半段测试曾被数据盘满和 Gradle 缓存锁阻塞；清理环境后，`compileTestJava` 成功，`TerminalServiceTest`、`TerminalHomeScreenModelTest`、`TerminalPageTest`、`ClusterTeleportServiceTest`、`PlayerArrivalRestoreServiceTest`、`TerminalHomeScreenLayoutTest`、`TerminalShellPanelsScrollTest` 均通过；临时 `galaxy-dev-run` 测试容器已清理，未触碰正式游戏服务器进程

### 2026-06-06 - 收口 ServerTools 终端页 recent transfer 状态与默认 facade 测试

- 主题：把群组服终端页 v1 验收中确认的两项代码缺口直接补齐，不重写现有页面结构
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/cluster/port/TeleportTicketRepository.java`、`src/main/java/com/jsirgalaxybase/modules/cluster/infrastructure/jdbc/JdbcTeleportTicketRepository.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalServerToolsSectionSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalServerToolsSectionModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/PlayerArrivalRestoreServiceTest.java`、`docs/terminal-servertools-page-v1-acceptance-review-2026-06-06.md`
- 原因：`SERVER_TOOLS` 页面已接入真实 warp 主链，但此前还缺少最近传送票据状态展示，以及默认 facade 是否真正走 `prepareWarpTeleport(...)` / `dispatchTeleport(...)` 的可测证明
- 结果：新增 `findRecentForPlayer(...)` 仓储入口并接入 JDBC；ServerTools terminal snapshot 现在可显示最近 transfer ticket 状态行；`TerminalService` 增加 runtime bridge seam，默认 facade 的本服完成、跨服派发、runtime 不可用和后端异常路径均已补单测证明；当前剩余收口仅为游戏内点击验收与必要的局部密度微调

### 2026-05-18 - 记录当前产品方向、进度判断与剩余需求清单

- 主题：把当前共识从“继续单点 UI 打磨”收口为“终端作为全面智能控制台”，并明确下一阶段优先推进群组服工具页面和市场功能/页面产品化
- 影响范围：`docs/current-product-direction-and-gap-review-2026-05-18.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：终端新壳可用性已经明显改善，ServerTools 跨服 warp 已完成真实 Lobby <-> S2 验证，后续重点需要转向把 ServerTools 和三类市场变成终端中的正式玩家工作流
- 结果：新增当前方向与缺口评审文档，明确 terminal foundation、ServerTools/cluster、market 三条线的已完成进度、剩余需求、推荐执行阶段和需要用户确认的产品决策；同时标注旧 Phase 7 交接文档与 2026-05-17 灰度状态记录中的过期状态点

### 2026-05-18 - 完成 ServerTools 跨服 warp 真实灰度验证

- 主题：用游戏内 `/jgbst warp ...` 对 Lobby 与 S2 之间的跨服传送闭环做真实验证，并收口超时问题
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`、Velocity gray chain 配置、`docs/servertools-phase3-gray-rollout-status-2026-05-17.md`、`docs/WORKLOG.md`
- 原因：此前灰度链已启动，但还未实际验证玩家级跨服传送；第一次实测暴露出 transfer ticket TTL 与代理 read timeout 对 GTNH 切服耗时不够宽容
- 结果：`/jgbst warp list` 可列出系统 warp；`/jgbst warp s2test` 完成 Lobby 到 S2；`/jgbst warp lobbytest` 完成 S2 回 Lobby；相关 `cluster_transfer_ticket` 进入 `COMPLETED`；后续 ServerTools 可进入终端页面产品化阶段

### 2026-05-17 - 增加 servertools 命名空间 warp 入口

- 主题：为 ServerTools 增加不与 GTNH 整合包裸 `/warp` 冲突的命名空间入口，优先打通跨服 warp 灰度测试
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/servertools/command/`、`src/main/java/com/jsirgalaxybase/modules/servertools/ServerToolsModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/test/java/com/jsirgalaxybase/modules/servertools/command/JgbServerToolsCommandTest.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/servertools-phase1-command-reference.md`、`docs/WORKLOG.md`
- 原因：灰度实测 `/warp s2test` 与 `/warp lobbytest` 被整合包内其他模组接走，未进入 JsirGalaxyBase 的 `WarpCommand`，导致 `cluster_transfer_ticket` 没有新增记录
- 结果：新增 `/jgbst warp list`、`/jgbst warp <name>`，别名保留 `/jst` 与 `/jsirst`；主命令新增 `/jsirgalaxybase servertools warp ...` 与 `/jsirgalaxybase st warp ...`；裸 `/warp` 仍保留兼容注册，但文档改为以命名空间入口作为验收命令；新入口统一转入 `ServerToolsCommandHandler`，复用 `PlayerTeleportService.prepareWarpTeleport` 与现有 cluster dispatch，不复制传送业务逻辑
- 验证：本机无 Java/JDK，容器内 `compileJava` 在拉取 `ModularUI2:2.3.45-1.7.10` 时因远端 TLS handshake 被断开，未进入 Java 编译阶段；已补最小命令路由测试与手动游戏内/SQL 验证步骤，后续可在依赖缓存可用时重跑 `docker compose -f /media/u24/data/gtnh/docker/projects/docker-compose.yml run --rm -e GRADLE_USER_HOME=/tmp/gradle-home galaxy-dev ./gradlew compileJava --no-configuration-cache -PforceToolchainVersion=17`

### 2026-05-17 - 完成 servertools / cluster Phase 3 灰度链部署准备

- 主题：按第三阶段灰度联调目标，把 Entrance / Lobby / S2 准备到可启动、可观察、可继续做真实跨服联调的状态，同时不触碰在线 S1
- 影响范围：`docs/servertools-phase3-gray-rollout-status-2026-05-17.md`、`docs/README.md`、`docs/WORKLOG.md`、`/media/u24/data/gtnh/data/Galaxy_GTNH_Lobby/**`、`/media/u24/data/gtnh/data/Galaxy_GTNH284_S2/**`、`/media/u24/data/gtnh/docker/projects/.env`、`galaxy-base` PostgreSQL schema、`galaxy-gtnh` 容器内 supervisor 当前配置
- 原因：原 phase 3 prompt 仍按 MCSM 路径描述，但当前机器真实运行链已经是 Docker + supervisor；灰度链此前已有旧 jar 和配置残留，但未真正启动，数据库也没有基础 schema，会阻塞 Lobby / S2 进入 JsirGalaxyBase banking / market / cluster runtime
- 结果：确认当前运行目录为 `/media/u24/data/gtnh/data` 并挂载到容器 `/gtnh/GroupServer`；将当前 `build/libs/jsirgalaxybase-ed7e2cf.jar` 部署到 Lobby 与 S2，旧 `7545ce9` jar 移入各自 `mods_disabled`；用项目 DDL 初始化空库后再执行既有 `scripts/db-migrate.sh` 完成全部版本化迁移；把 PostgreSQL 角色密码对齐到既有 server cfg；在不重启 S1 的前提下通过 supervisor 启动 Entrance、Lobby、S2，并把持久 `.env` 同步为灰度链启用状态；最终 Entrance 监听 `25566`，Lobby 监听 `25564`，S2 监听 `25567`，Lobby / S2 日志均出现 JsirGalaxyBase banking、market、cluster runtime prepared 和 Minecraft `Done`
- 验证：`docker compose ... galaxy-dev ./gradlew assemble --no-configuration-cache -PforceToolchainVersion=17` 成功；部署 jar 与构建 jar SHA256 均为 `02dcd79439cb7e8fa7896299d047d637e3d9dd3bddd7d7b54fdff8beca98e065`；host TCP probes 到 `127.0.0.1:25564` / `25566` / `25567` 成功；`galaxy-gtnh` supervisor 中 S1 uptime 未重置且 S1 的 JsirGalaxyBase 文件未改动

### 2026-04-10 - 把 ServerUtilities 的后续整合边界写入现有架构文档

- 主题：把 `Reference/ServerUtilities` 作为参考源码来源时的整合边界正式写回现有架构文档，明确后续服务器工具能力只能按 cluster / server tools 子系统拆解吸收，不能整包并入
- 影响范围：`README.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：银行与市场主链已经基本成型，后续跨服传送、服务器状态、home 与其他服务器工具能力需要进入正式架构讨论阶段；如果不先写清楚整合规则，后面很容易把 `ServerUtilities` 的整套历史结构直接搬进当前仓库，破坏现有模块化单体边界
- 引用来源：`Reference/ServerUtilities/README.md`、`Reference/ServerUtilities/src/main/java/serverutils/ServerUtilities.java`、`Reference/ServerUtilities/src/main/java/serverutils/data/ServerUtilitiesPlayerData.java`、`Reference/ServerUtilities/src/main/java/serverutils/data/ServerUtilitiesUniverseData.java`、`Reference/ServerUtilities/src/main/java/serverutils/net/ServerUtilitiesNetHandler.java`
- 结果：根架构文档现已明确 `modules.cluster` / `modules.servertools` 的推荐落点，以及 `GatewayAdapter`、`ClusterStateRepository`、`PlayerSnapshotRepository`、`ServerRegistry` 等抽象优先策略，并明确禁止把 `ServerUtilities` 的单入口大模组、全局数据宿主和多通道网络注册表原样迁入

### 2026-04-10 - 产出 ServerUtilities 逐项整合映射表

- 主题：把 `Reference/ServerUtilities` 中后续可能需要的能力逐项列成映射表，并标注为可直接复用、需抽取后复用、建议重写或暂不整合
- 影响范围：`docs/serverutilities-integration-mapping.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：仅靠“需要重构”这一层判断还不够，后续第一批要吸收哪些工具能力，必须落实到具体模块与命令级别，否则仍容易在实现阶段回到“整包看起来都能用”的误判
- 引用来源：`Reference/ServerUtilities/src/main/java/serverutils/data`、`Reference/ServerUtilities/src/main/java/serverutils/command`、`Reference/ServerUtilities/src/main/java/serverutils/client`、`Reference/ServerUtilities/src/main/java/serverutils/ranks`、`Reference/ServerUtilities/src/main/java/serverutils/task`
- 结果：新增正式映射文档，已把 `home` / `warp` / `tpa` / `TeleportTracker` / leaderboard 等列入优先评估清单，把 player data / universe data / net handler / ranks 等标为建议重写，并把 claims、chunkloading、backup、watchdog、invsee 等标记为暂不整合

### 2026-04-11 - 记录第一批 server tools / cluster 已确认需求

- 主题：把 `ServerUtilities` 第一批整合范围中已经拍板的命令、跨服、数据库、权限与 GUI 约束正式写入 docs，供后续执行 prompt 直接引用
- 影响范围：`docs/servertools-phase1-requirements.md`、`docs/serverutilities-integration-mapping.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：后续执行 prompt 已不再缺架构边界，而是缺一份稳定、可引用的需求确认单；如果这些要求只留在对话里，后面实现时仍容易回到“先做单服占位”或“顺手加 GUI”的偏航
- 引用来源：本轮对话中已确认的第一期范围；`docs/serverutilities-integration-mapping.md`
- 结果：新增一份正式需求确认文档，明确 `home` / `back` / `spawn` / `tpa` / `rtp` / `warp` 为一期必须实现范围，明确跨服和数据库为硬约束、GUI 暂缓、权限只做玩家语义但要预留职业 / 贡献度 / 声望制度扩展接口

### 2026-04-11 - 产出可直接交付给另一个 AI 的 server tools 第一期执行 Prompt

- 主题：把已确认需求收口成一份可直接交给另一个 AI 执行的正式 prompt，避免后续实现阶段再次回到“继续评估”或“顺手扩范围”的状态
- 影响范围：`docs/servertools-phase1-execution-prompt-2026-04-11.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：第一期范围、跨服能力、数据库硬约束、权限边界和不做 GUI 都已经明确，当前最需要的是一份强约束执行文本，供后续代码搬运、修改与适配直接使用
- 引用来源：`docs/servertools-phase1-requirements.md`、`docs/serverutilities-integration-mapping.md`、`README.md`
- 结果：新增一份正式执行 prompt，已明确本轮必须实现 `home` / `back` / `spawn` / `tpa` / `rtp` / `warp`，必须支持跨服并直接接数据库，不兼容旧命令格式，不引入旧 rank / net / GUI 框架，只允许按当前项目模块化单体结构完成服务、仓储、命令与 cluster 接线

### 2026-04-11 - 落地 server tools / cluster 第一期命令链、仓储链与模块装配

- 主题：在 `JsirGalaxyBase` 中正式新增 `ClusterModule` 与 `ServerToolsModule`，把 `home` / `back` / `spawn` / `tpa` / `rtp` / `warp` 的最小可运行第一期主链直接落到当前仓库
- 影响范围：`src/main/java/com/jsirgalaxybase/module/ModuleContext.java`、`src/main/java/com/jsirgalaxybase/bootstrap/ModBootstrap.java`、`src/main/java/com/jsirgalaxybase/modules/cluster/`、`src/main/java/com/jsirgalaxybase/modules/servertools/`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/servertools/application/PlayerTeleportServiceTest.java`、`ops/sql/migrations/20260411_001_add_servertools_cluster_phase1.sql`、`docs/servertools-cluster-postgresql-ddl.sql`、`docs/servertools-phase1-command-reference.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：第一期范围已经从“评估”转入“直接实现”，而且需求已明确要求必须走数据库真源、模块生命周期装配、cluster/servertools 双模块边界和跨服票据预留，不允许再停留在单服 NBT / 文件占位版
- 引用来源：`README.md`、`docs/serverutilities-integration-mapping.md`、`docs/servertools-phase1-requirements.md`、`Reference/ServerUtilities/src/main/java/serverutils/data/TeleportTracker.java`、`Reference/ServerUtilities/src/main/java/serverutils/data/TeleportLog.java`、`Reference/ServerUtilities/src/main/java/serverutils/command/tp/`
- 结果：当前仓库已新增 cluster server directory / transfer ticket / homes / back / warp / tpa / rtp 记录表与 fail-fast JDBC factory；`home`、`back`、`spawn`、`tpa`、`rtp`、`warp` 顶层命令已注册到 dedicated server 启动链；本服目标会直接传送，跨服目标会通过 `cluster_transfer_ticket` 和 `GatewayAdapter` 进入下一阶段真实代理接线预留；同时补齐了纯服务层与 cluster 分发层单测，以及一份正式命令/表结构说明文档

### 2026-04-11 - 产出 server tools / cluster 第一期验收收口 Prompt

- 主题：把本轮严格验收发现的阻塞项收口成一份新的整改 prompt，要求后续执行只修已确认问题，不再继续扩大第一期提交范围
- 影响范围：`docs/servertools-phase1-acceptance-close-prompt-2026-04-11.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：本轮验收已经确认第一期主链基本成立，但仍存在 transfer ticket 状态未完整持久化、TPA 目标服未校验以及提交范围失控这三类问题；如果不把这些问题单独固化成整改 prompt，后续执行很容易再次把修复做成新一轮扩范围开发
- 引用来源：本轮严格验收结论；`docs/servertools-phase1-execution-prompt-2026-04-11.md`；`docs/servertools-phase1-command-reference.md`
- 结果：新增一份只面向验收收口的执行 prompt，已经明确本轮仅允许修复 cluster ticket 状态回写与 TPA target server 校验，并明确禁止继续改 terminal / market / bank 等无关范围

### 2026-04-11 - 收口第一期验收问题：cluster ticket 状态回写与 TPA 目标服校验

- 主题：只修第一期验收中已经确认的两处根因问题，不扩大 servertools / cluster 之外的提交范围
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`、`src/main/java/com/jsirgalaxybase/modules/servertools/application/PlayerTeleportService.java`、`src/main/java/com/jsirgalaxybase/modules/servertools/ServerToolsModule.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/servertools/application/PlayerTeleportServiceTest.java`、`docs/servertools-phase1-command-reference.md`、`docs/WORKLOG.md`
- 原因：严格验收确认当前远端派发返回 pending ticket 时没有把 adapter 返回状态统一回写数据库，同时 `tpa <playerName> [targetServerId]` 会把未注册或 disabled 的 serverId 错写进业务记录；这两处都属于第一期实现收口缺陷
- 引用来源：`docs/servertools-phase1-acceptance-close-prompt-2026-04-11.md`、`docs/servertools-phase1-command-reference.md`
- 结果：cluster 远端派发现在只要 adapter 返回 ticket 就会统一持久化其最新状态与消息；TPA 创建链现在会复用 cluster server directory 校验目标服是否存在且 enabled，校验失败时不会写入 `player_tpa_request`；同时补齐了对应定向单测，且没有继续触碰 terminal / market / bank 范围

### 2026-04-12 - 产出 cluster 第二阶段严格验收收口 Prompt

- 主题：补出一份只处理 cluster 第二阶段严格验收阻塞项的收口 prompt，避免下一轮执行继续扩散到新功能或无关模块
- 影响范围：`docs/servertools-phase2-cluster-close-prompt-2026-04-12.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：本轮严格验收确认第二阶段主链虽然已接通，但仍有两个阻塞项未收口：其一是 ticket 过期清理会保留旧 `status_message`，导致 `EXPIRED` 状态与消息语义不一致；其二是重复 `requestId` 命中终态 ticket 时，`ClusterTeleportService` 仍会无条件返回 `PENDING_REMOTE`，破坏最小幂等与恢复语义
- 引用来源：`src/main/java/com/jsirgalaxybase/modules/cluster/infrastructure/jdbc/JdbcTeleportTicketRepository.java`、`src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/PlayerArrivalRestoreServiceTest.java`
- 结果：新增一份只面向这两个阻塞问题的收口 prompt，已经明确本轮只修 ticket 过期消息覆盖、重复 requestId 的终态返回语义，并要求补对应定向测试和 WORKLOG，不再扩大到 terminal / market / bank 或继续加 cluster 新能力

### 2026-04-12 - 收口 cluster 第二阶段严格验收阻塞项：过期 message 终态覆盖与重复 requestId 终态映射

- 主题：只修 cluster 第二阶段严格验收已确认的两个阻塞项，不继续扩 gateway、生命周期或其他模块范围
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`、`src/main/java/com/jsirgalaxybase/modules/cluster/infrastructure/jdbc/JdbcTeleportTicketRepository.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/PlayerArrivalRestoreServiceTest.java`、`docs/WORKLOG.md`
- 原因：当前实现里，过期清理仍会因 `COALESCE` 保留旧的 waiting/retry message，且重复 `requestId` 命中 `FAILED / COMPLETED / EXPIRED` ticket 时会被统一伪装成 `PENDING_REMOTE`；这两点直接破坏终态可观测性与最小幂等语义
- 引用来源：`docs/servertools-phase2-cluster-close-prompt-2026-04-12.md`、`src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`、`src/main/java/com/jsirgalaxybase/modules/cluster/infrastructure/jdbc/JdbcTeleportTicketRepository.java`
- 结果：过期清理现在会无条件把 `status_message` 覆盖成明确的 expired 文案；重复 `requestId` 命中已有 ticket 时，会按真实状态返回 pending/completed/failed 语义，不再把终态 ticket 统一伪装成 pending；同时补了对应定向单测

### 2026-04-12 - 产出第三阶段 MCSM 灰度联调准备执行 Prompt

- 主题：把 server tools / cluster 下一阶段工作收口成一份严格执行 prompt，只允许利用 MCSM 下的代理 / 大厅 / S2 组一条不影响在线 S1 的灰度联调链
- 影响范围：`docs/servertools-phase3-mcsm-gray-rollout-prompt-2026-04-12.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前第二阶段代码语义已经通过严格验收，但现网资源评估确认 Lobby 与 S2 仍未部署 `JsirGalaxyBase` 模组和相关配置，现阶段真正需要的不是继续扩代码，而是把代理 / 大厅 / S2 准备到可启动、可观察、可继续联调的灰度环境，同时绝对不触碰正在承载玩家的 S1
- 引用来源：`../GroupServer/Galaxy_GTNH_Entrance/velocity.toml`、`../GroupServer/Galaxy_GTNH_Lobby/server.properties`、`../GroupServer/Galaxy_GTNH284_S2/server.properties`、`../GroupServer/mcsmanager/daemon/data/InstanceConfig/b3e4f9f9aefd4d2a9f57c338c1a0f3b8.json`、`../GroupServer/mcsmanager/daemon/data/InstanceConfig/d1e307c5d40745079df2e398e9d85db2.json`、`../GroupServer/mcsmanager/daemon/data/InstanceConfig/d705aa8c32c649228a84a323e1504f62.json`
- 结果：新增一份第三阶段严格执行 prompt，已经明确本轮只允许审计、构建、部署并通过 MCSM 启动代理 / Lobby / S2 的灰度链，必须保持不动 S1、不得绕开 MCSM 直接管控实服进程，也不得顺手扩大到 terminal / market / bank 或新一轮 cluster 功能扩写

### 2026-04-12 - 产出 terminal bank / market 审查后续修复 Prompt

- 主题：把 terminal bank / market 审查中确认的两个后续风险收口成一份可直接执行的修复 prompt，避免下一轮实现时再次混入无关 GUI 打磨或业务扩写
- 影响范围：`docs/terminal-bank-market-risk-fix-prompt-2026-04-12.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：本轮代码审查已经确认，一处高优风险是 market/custom 动作后没有联动失效银行快照，导致银行页可能展示过期余额与状态；另一处低优风险是 `TerminalHomeGuiFactory` 旧 market 挂载残留分支仍保留 custom panel 错接线，虽然当前不是主链，但后续误启用会直接带回错误弹窗接线
- 引用来源：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSessionController.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`
- 结果：新增一份 terminal bank / market 风险修复 prompt，已经明确本轮优先补齐跨页银行快照失效链，并最小清理旧 market 挂载残留的 custom panel 错接线，不继续扩大到 market / bank 业务语义、terminal 视觉层或更大范围重构

### 2026-04-11 - 产出第二阶段第一优先级 cross-server gateway 执行 Prompt

- 主题：把 servertools / cluster 下一阶段最优先的工作进一步收口成一份可直接交给另一个 AI 执行的 prompt，聚焦真实跨服闭环而不是继续扩命令数量
- 影响范围：`docs/servertools-phase2-cross-server-gateway-prompt-2026-04-11.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：第一期命令链和数据库真源已经成立，下一阶段真正的高优任务不再是继续加 `servertools` 命令名，而是把 `GatewayAdapter + TransferTicket` 从占位链推进到源服派发、目标服消费、落点恢复和失败可排障的真实跨服闭环
- 引用来源：`docs/servertools-phase1-requirements.md`、`docs/servertools-phase1-command-reference.md`、`docs/servertools-phase1-execution-prompt-2026-04-11.md`、`docs/servertools-phase1-acceptance-close-prompt-2026-04-11.md`
- 结果：新增一份第二阶段第一优先级执行 prompt，已经明确本轮只做真实网关适配边界、transfer ticket 生命周期闭环、目标服消费与落点恢复、最小失败恢复与排障观测，不再扩 terminal / market / bank 或 GUI 范围

### 2026-04-11 - 产出 terminal bank / market 严格收口执行 Prompt

- 主题：补出一份只面向 terminal bank / market sync 回归收口的执行 prompt，避免后续实现继续把主链阻塞项、非阻塞残留和 GUI 打磨需求混在一起
- 影响范围：`docs/terminal-bank-market-strict-close-prompt-2026-04-11.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前 terminal bank / market 的严格审阅已经确认，高风险问题集中在文本输入框再次混用 binder 手工 sync 与 `TextFieldWidget.value(...)` auto sync，以及缺少覆盖 terminal open 装配链的回归测试；如果不单独起一份收口 prompt，后续实现容易顺手扩大到旧装配残留清理、page 重构或视觉打磨
- 引用来源：`docs/terminal-gui-regression-chain-2026-04-05.md`、`docs/terminal-gui-continue-current-implementation-prompt-2026-04-06.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`
- 结果：新增一份 terminal bank / market 严格收口 prompt，已经明确本轮只移除 market 与 bank 文本输入框的手工 sync 注册、补 terminal open 装配链测试、更新 WORKLOG，并明确把旧 market page container 残留和更大范围页面装配测试留到下一轮

### 2026-04-05 - 产出市场终端 asset-first 重构评估与执行 Prompt

- 主题：基于现有三市场终端代码，正式评估“先选物品 / 挂牌 / 标的，再进详情页”的 asset-first 重构方向，并产出下一轮可执行 prompt
- 影响范围：`docs/market-terminal-asset-first-refactor-evaluation-2026-04-05.md`、`docs/market-terminal-asset-first-refactor-prompt-2026-04-05.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前终端虽然已经完成 MARKET 总入口拆分，但只有标准商品市场半成型地具备对象浏览与详情节奏；定制商品市场仍主要是说明页，汇率市场仍以手持驱动面板为主，继续直接堆字段会把终端重新推回统一 MARKET 巨石结构
- 引用来源：`docs/market-three-part-architecture.md`、`docs/market-entry-overview.md`、`docs/custom-market-minimal-model.md`、用户提出的 AE 终端 / 售货机式交互需求
- 结果：新增一份正式评估文档，明确标准商品市场适合完整采用物品优先终端、定制商品市场应做成 listing-first GUI、汇率市场应做成标的优先详情页；并同步新增可直接执行的重构 prompt

### 2026-04-05 - 补充终端 GUI 回归链文档中的验收阻塞与修复指令

- 主题：把本轮验收发现的实际阻塞项补写进终端 GUI 回归链统一文档，避免后续执行 AI 只看“已完成”结论而继续把兼容边界修歪
- 影响范围：`docs/terminal-gui-regression-chain-2026-04-05.md`、`docs/WORKLOG.md`
- 原因：市场终端与汇率市场大批实现已经落地，但定向测试显示旧任务书兑换兼容入口对非任务书物品的拒绝语义发生回归；需要把“问题是什么、允许怎么修、禁止怎么修、修后必须验证什么”直接写进 prompt/事故链文档
- 引用来源：`src/test/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangeServiceTest.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangeService.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/ExchangeMarketService.java`
- 结果：在统一事故文档末尾新增“本轮验收阻塞与修复执行 Prompt”段，明确要求保留新 formal quote 链，同时恢复旧兼容入口对非任务书物品的直接拒绝语义，并要求后续执行补 WORKLOG 和定向测试验证

### 2026-04-06 - 补出终端 GUI 持续实装 Prompt，明确不再保留回退方案

- 主题：补出一份新的终端 GUI 执行 prompt，明确后续只允许继续修改当前 GUI 实现，不再引入 fallback GUI 或新旧双轨并存
- 影响范围：`docs/terminal-gui-continue-current-implementation-prompt-2026-04-06.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前终端 GUI 主链已经落地，后续重点应转为继续打磨当前 ModularUI 实装效果；如果此时继续让执行 AI保留可回退 GUI，会把结构重新带回双轨维护与巨石装配
- 引用来源：`docs/terminal-modularui2-gui-refactor-prompt-2026-04-02.md`、`docs/terminal-gui-regression-chain-2026-04-05.md`、`docs/market-terminal-asset-first-refactor-evaluation-2026-04-05.md`
- 结果：新增一份直接面向下一轮 GUI 持续实装的 prompt，明确要求只在当前 GUI 上继续实现原先效果、保留真实银行/市场/汇率主链，并接受后续基于用户截图继续微调

### 2026-04-06 - 在当前单实现终端上继续收口导航壳与业务详情层次

- 主题：继续直接修改当前 ModularUI 终端实现，把总壳、银行页、标准商品市场、定制商品市场和汇率市场进一步收口成更接近正式终端的层次，而不是再引入任何回退 GUI
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalWidgetFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankPageBuilder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`、`docs/WORKLOG.md`
- 原因：当前 GUI 主链已经可用，但首页/导航/详情页仍有明显“工程拼板态”；银行页说明块仍压着真实操作区，标准商品详情页买卖/仓储/订单主次不够清晰，定制商品与汇率页也还缺少更明确的对象详情节奏
- 引用来源：`docs/terminal-gui-continue-current-implementation-prompt-2026-04-06.md`、`docs/terminal-modularui2-gui-refactor-prompt-2026-04-02.md`、`docs/market-terminal-asset-first-refactor-evaluation-2026-04-05.md`
- 结果：新增共享 hero band / summary banner 壳层，用于统一页头与状态带；终端总壳导航改成更明确的当前页/当前终端分区提示；银行页改成状态、表单预览、服务反馈和确认门禁闭环；标准商品页把详情层进一步收口成商品摘要 + 盘口 + 交易动作台 + 订单/仓储闭环；定制商品和汇率页也补出更明确的当前详情提示与执行顺序说明，且全程保留现有真实银行 / 市场 / 汇率业务链

### 2026-04-06 - 修复终端首页路由矩阵导致的 ModularUI 尺寸循环

- 主题：修复 client 打开终端首页时因为路由矩阵尺寸推导互相依赖而触发的 ModularUI 死循环卡死
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：首页“终端路由矩阵”区块把 `expanded()` 的矩阵容器放进了 `coverChildrenHeight()` 的 section 里，父容器需要靠子项算高度，子项又反过来等待父高度，运行时就会刷出大量 `MUI [SIZING][Column]: Can't cover children when all children depend on their parent!`
- 结果：矩阵区改成由表头和固定行高直接撑开，不再把相对扩展高度控件塞进按子项包高的 section，从而收口终端首页打开即卡死的问题

### 2026-04-14 - 把终端 GUI 长期路线正式改为内置 BetterQuesting 风格框架

- 主题：把终端 GUI 的长期技术路线从继续深化 `ModularUI 2`，正式调整为 vendoring BetterQuesting 风格 GUI 框架，并把改造面与实施顺序写入终端实施方案
- 影响范围：`docs/terminal-plan.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：前序调研和真实兼容性实验已经证明，`ModularUI 2` 属于 GTNH 共享运行时依赖，版本漂移会带来 pack 级 ABI 风险；而终端未来要承载银行、市场与后续 server tools 统一应用壳，更适合采用仓库自有的 `GuiScreenCanvas + panel tree + theme registry` 体系
- 引用来源：`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/GuiScreenCanvas.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/IGuiPanel.java`、`Reference/BetterQuesting/src/main/java/betterquesting/client/themes/ThemeRegistry.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`
- 结果：终端实施方案现已明确哪些业务层保持不动、哪些 `ModularUI` 类必须重写、为什么打开链要从服务端直接开 GUI 改成“服务端授权 + 客户端开屏”，以及后续 vendoring、首页壳、银行页、市场页与旧依赖清理的建议实施顺序

### 2026-04-14 - 把 BetterQuesting 风格 GUI 集成方案拆成独立文档

- 主题：将终端 GUI 的 BetterQuesting 风格 framework 集成设计从 `terminal-plan.md` 中拆出，形成单独的实施方案文档，避免终端总方案与 GUI 内核集成细节混在同一份文档里
- 影响范围：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-plan.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：原终端实施方案需要继续承担入口、分阶段目标和总体路线说明；而 BetterQuesting 风格 GUI 集成已经进入到 vendoring 范围、协议重构、包结构和页面装配层级，继续混写会让后续执行文档失焦
- 引用来源：`docs/terminal-plan.md`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/GuiScreenCanvas.java`、`Reference/BetterQuesting/src/main/java/betterquesting/client/themes/ThemeRegistry.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`
- 结果：现在 `terminal-plan.md` 只保留 GUI 路线结论与索引，具体的 BetterQuesting 风格 GUI 集成边界、协议变化、包结构、迁移顺序和验收标准已经转入独立方案文档，后续真正实装时可直接以该文档为主

### 2026-04-14 - 产出 BetterQuesting 风格 UI framework 第一阶段执行 Prompt

- 主题：补出一份只面向 BetterQuesting 风格 GUI framework 第一阶段落地的执行 prompt，明确本轮只做最小 vendoring、theme/resource 骨架和占位 screen，不提前混入 terminal 迁移
- 影响范围：`docs/terminal-betterquesting-ui-phase1-framework-prompt-2026-04-14.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：下一步已经从“继续评估”转入“交给另一个 AI 实做第一阶段 framework 地基”；如果没有一份强约束 prompt，执行时很容易顺手把 terminal 页面迁移、协议重构和旧 `ModularUI` 清理提前混进来，导致范围失控
- 引用来源：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/GuiScreenCanvas.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/IGuiPanel.java`、`Reference/BetterQuesting/src/main/java/betterquesting/client/themes/ThemeRegistry.java`
- 结果：现已新增一份可直接执行的第一阶段 prompt，明确要求只落地仓库自有 GUI framework 基础层、去 BetterQuesting 全局依赖、建立最小主题骨架并跑通占位 screen，同时明确禁止提前迁 terminal 页面和打开协议

### 2026-04-14 - 落地终端 phase 2 新打开链：服务端授权 + 客户端开 TerminalHomeScreen

- 主题：把终端正式入口从“客户端请求后由服务端直接打开 ModularUI”推进到“客户端请求、服务端授权、客户端自行打开 BetterQuesting 风格 TerminalHomeScreen 占位根屏”的最小闭环
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/network/`、`src/main/java/com/jsirgalaxybase/terminal/client/`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/`、`src/test/java/com/jsirgalaxybase/terminal/`、`src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 1 framework 已经可独立运行，但终端主链仍停在 `OpenTerminalMessage -> TerminalService.openTerminal(player) -> TerminalHomeGuiFactory.INSTANCE.open(player)`；第二阶段需要先把 screen 生命周期和授权协议站稳，而不是提前迁银行页和市场页
- 引用来源：`README.md`、`docs/terminal-plan.md`、`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase1-framework-prompt-2026-04-14.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`
- 结果：新增 `OpenTerminalRequestMessage` 与 `OpenTerminalApprovedMessage`，客户端快捷键和背包按钮现已走新请求链；服务端通过 `TerminalService.approveTerminalClientScreen(...)` 生成最小初始化快照与 session token；客户端收到授权后由 `TerminalClientScreenController` 在主线程打开 `TerminalHomeScreen` 占位根屏；`TerminalFrameworkTestScreen` 的 F8 调试入口仍保留文件但不再注册进正式 client bootstrap；旧 `TerminalHomeGuiFactory`、银行页、市场页与 `OpenTerminalMessage` 旧链仍保留为过渡实现，未提前迁移业务

### 2026-04-15 - 把 TerminalHomeScreen 从 phase 2 占位板推进为 phase 3 首页壳

- 主题：只把新终端主链上的 `TerminalHomeScreen` 推进成真正可承载后续业务页的首页壳，并补齐最小 screen model、共用 shell panel、通知宿主与 popup 生命周期，不提前迁银行页和市场页
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalOpenApproval.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalOpenSummaryFormatter.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalOpenSummaryFormatterTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModelTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 2 已经接通“客户端请求、服务端授权、客户端开屏”的新主链，但 `TerminalHomeScreen` 仍只是单块摘要板，后续银行页和市场页还没有可直接挂接的顶部状态带、导航、通知和 popup 宿主层
- 引用来源：`README.md`、`docs/terminal-plan.md`、`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase1-framework-prompt-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase2-open-chain-prompt-2026-04-14.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalWidgetFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalDialogFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalNotification.java`
- 结果：`TerminalHomeScreenModel` 现已扩成包含状态带、导航项、首页 section 和通知入口位的最小首页壳模型；新增 `TerminalPanelFactory`、`TerminalShellPanels`、`TerminalHomeSection` 与 `TerminalPopupFactory` 作为后续业务页可复用的壳层组件；`TerminalHomeScreen` 已重构为顶部状态带 + 左侧导航 + 主内容区 + 通知宿主 + popup 宿主的首页壳；非首页导航点击现在会走新壳级 popup 而不是偷接旧业务页；旧 `TerminalHomeGuiFactory`、银行页、市场页、binder 和 sync state 继续保留为过渡实现

### 2026-04-15 - 收口 phase 3 当前页语义：selectedPageId 成为首页壳单一真源

- 主题：只修 phase 3 严格验收确认的当前页语义分裂问题，把首页壳当前页统一收口到 `selectedPageId`，不提前进入 phase 4 section 宿主切换
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`、`src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModelTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 3 虽然已经显式传 `selectedPageId`，但首页壳实际判断当前页仍主要依赖 `navItems[].selected`；如果后续服务端只切 page id 或 nav flag 不一致，状态带、主体区和导航高亮就可能出现错页
- 引用来源：`docs/terminal-betterquesting-ui-phase3-home-shell-prompt-2026-04-15.md`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`
- 结果：`selectedPageId` 现在是首页壳当前页语义的唯一真源；`NavItemModel.selected` 仍保留，但只作为模型归一化后的派生高亮结果；模型会把子页 id 映射到顶层导航页并重建 nav 选中态，不再信任调用方传入的 selected 标记；本轮没有开始 section 真切换、动作协议或业务页迁移

### 2026-04-15 - 产出终端 phase 3 严格验收收口 Prompt

- 主题：补出一份只面向 terminal phase 3 严格验收收口项的执行 prompt，明确下一轮只修首页壳 `selectedPageId` 与导航选中态仍然分裂的语义缺口，不提前进入 phase 4
- 影响范围：`docs/terminal-betterquesting-ui-phase3-close-prompt-2026-04-15.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 3 首页壳主结构已经落地并通过静态验收，但严格审阅确认首页当前页语义仍同时依赖 `selectedPageId` 与 `navItems[].selected` 两套来源；如果不先收干净，下一阶段 section 宿主切换和真实页面挂载会先踩当前页高亮与主体区错页风险
- 引用来源：`docs/terminal-betterquesting-ui-phase3-home-shell-prompt-2026-04-15.md`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`
- 结果：现已新增一份 phase 3 收口 prompt，范围收口到首页壳当前页语义的单一真源、最小模型与测试修正，以及 README / WORKLOG 同步，不再把 phase 4 的 section 路由、动作协议和业务页迁移混进这轮

### 2026-04-18 - 产出终端 phase 4 section 宿主与最小协议地基 Prompt

- 主题：补出一份只面向 terminal 新首页壳 section 宿主切换与最小 action / snapshot 协议地基的第四阶段执行 prompt，明确这轮先把新壳做成真正宿主，再进入银行页迁移
- 影响范围：`docs/terminal-betterquesting-ui-phase4-section-host-prompt-2026-04-18.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：原高层集成方案虽然把“迁银行页”列为首页壳之后的下一大阶段，但 phase 3 严格验收与收口已经确认，若不先把首页壳推进成真实 section 宿主，并把 action / snapshot 协议补到最小正式落点，后续银行页迁移仍会缺少稳定宿主和刷新边界

### 2026-04-19 - 修复新终端客户端在窄文本区渲染中文标签时的递归换行崩溃

- 主题：修复 `TerminalHomeScreen` 打开后，`LabelPanel` 在极窄文本宽度下调用原版 `FontRenderer.wrapFormattedStringToWidth` 导致的栈溢出崩溃
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/framework/LabelPanel.java`、`docs/WORKLOG.md`
- 原因：phase 6 市场页接入后，部分标签在小屏或紧凑布局下会落入极窄宽度；1.7.10 原版 `FontRenderer` 对中文等宽字符在过窄宽度下可能返回 `sizeStringToWidth == 0`，随后进入无限递归换行
- 引用来源：`run/client/crash-reports/crash-2026-04-19_16.29.23-client.txt`
- 结果：`LabelPanel` 现已改为使用自有安全换行逻辑，在无法安全消费首字符时回退为截断显示，避免终端页再次因窄文本区渲染而直接崩溃
- 引用来源：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase3-home-shell-prompt-2026-04-15.md`、`docs/terminal-betterquesting-ui-phase3-close-prompt-2026-04-15.md`、`docs/README.md`
- 结果：现已新增一份 phase 4 执行 prompt，范围收口到首页壳 section 切换、最小 `TerminalActionMessage` / `TerminalSnapshotMessage` 落点、README / WORKLOG 同步，以及明确把银行页作为下一阶段的第一张完整业务页，而不是在本轮提前迁移

### 2026-04-18 - 落地终端 phase 4 section 宿主、最小 action 协议与 snapshot 回写链

- 主题：把 `TerminalHomeScreen` 从 phase 3 首页壳推进成真实 section 宿主，并补齐最小 `TerminalActionMessage` / `TerminalSnapshotMessage` 往返链，但不提前迁入银行页与市场页业务内容
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalActionType.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalOpenApproval.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/`、`src/main/java/com/jsirgalaxybase/terminal/client/`、`src/main/java/com/jsirgalaxybase/terminal/client/component/`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalSectionRouterTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModelTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 3 收口后虽然已经保证了 `selectedPageId` 是首页壳单一真源，但主体区仍固定渲染 home sections，导航点击也仍停留在 popup 占位，导致后续银行页迁移依然缺少真实 section 宿主和最小刷新闭环
- 引用来源：`docs/terminal-betterquesting-ui-phase4-section-host-prompt-2026-04-18.md`、`docs/terminal-betterquesting-ui-phase3-close-prompt-2026-04-15.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`
- 结果：`TerminalHomeScreenModel` 已改为 page snapshot 结构，`TerminalShellPanels` 主体区现按顶层 section host 渲染当前 page snapshot，导航点击会先切换本地壳再发最小 action 给服务端，服务端通过 `TerminalService.handleClientAction(...)` 返回新的 shell snapshot，客户端可在已打开的 `TerminalHomeScreen` 上原地刷新；本轮仍刻意保留旧 `ModularUI` 银行页与市场页，不做业务迁移

### 2026-04-18 - 补 phase 4 action/snapshot 往返链定向测试收口

- 主题：只补 phase 4 严格验收指出的缺口，为最小 `TerminalActionMessage` / `TerminalSnapshotMessage` 往返链增加直接定向测试，不改主实现边界
- 影响范围：`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`docs/WORKLOG.md`
- 原因：前一轮 phase 4 代码与现有定向测试已经覆盖 section 宿主和 page snapshot 归一化，但还缺一条直接证明“服务端 action 处理结果可回写为 snapshot，并进入客户端刷新入口”的最小测试，严格口径下不算完全收口
- 引用来源：`docs/terminal-betterquesting-ui-phase4-section-host-prompt-2026-04-18.md`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalActionMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/client/TerminalClientScreenController.java`
- 结果：`TerminalServiceTest` 现已补齐 service -> snapshot model round-trip 验证，以及 `TerminalSnapshotMessage.Handler` 向 `TerminalClientScreenController` 排队刷新模型的直接测试；本轮仍未提前迁银行页、市场页或扩写完整业务协议

### 2026-04-18 - 产出终端 phase 5 银行页迁移 Prompt，并压缩剩余阶段目标

- 主题：补出一份只面向银行页迁移的 phase 5 执行 prompt，并把从当前阶段起后续目标压缩为“再往后不超过五个阶段达到删除旧 terminal ModularUI 实现的条件”
- 影响范围：`docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 4 已经把首页壳推进成真实 section 宿主，下一步不该继续停留在抽象层，而应直接让银行页成为新壳上的第一张完整业务页；同时用户明确希望后续节奏加快，尽量在有限阶段内完成 terminal 对旧 ModularUI 的切换与删除准备
- 引用来源：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase4-section-host-prompt-2026-04-18.md`、`docs/banking-terminal-gui-design.md`、`docs/README.md`
- 结果：现已新增一份 phase 5 prompt，范围收口到银行页真实迁移、bank action / snapshot 闭环、确认 popup 迁移和文档同步，并把后续节奏压缩为 phase 6 市场迁移、phase 7 收干 terminal 旧装配残留、phase 8 cutover、phase 9 删除旧 terminal ModularUI 实现

### 2026-04-18 - 落地终端 phase 5 银行完整业务页迁移

- 主题：把 bank 顶层 section 从 phase 4 的宿主占位推进成新 `TerminalHomeScreen` 上的第一张完整业务页，并接通开户、刷新、转账确认 popup 与 snapshot 回写闭环
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalActionType.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalOpenApproval.java`、`src/main/java/com/jsirgalaxybase/terminal/network/`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalBankSectionModelTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalPopupFactoryTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 4 已经具备 section 宿主与最小 action/snapshot 地基，但银行页仍只是说明性 snapshot；下一步必须直接迁入第一张真实业务页，证明新壳可以承接完整终端业务而不是继续搭壳
- 引用来源：`docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md`、`docs/banking-system-requirements.md`、`docs/banking-terminal-gui-design.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSnapshotProvider.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankingService.java`
- 结果：新增 bank 专属 snapshot/view model、表单 payload 与本地输入状态；`TerminalService` 现已能处理 `BANK_REFRESH`、`BANK_OPEN_ACCOUNT`、`BANK_CONFIRM_TRANSFER` 并回写新的 bank snapshot；`TerminalHomeScreen` 现已通过 `TerminalBankSection` 与 `TerminalPopupFactory` 承接新银行页和转账确认 popup，且没有把旧 `TerminalBankPageBuilder`、`TerminalDialogFactory` 或旧 sync binder 重新接回新壳；同时补齐银行模型、action 回写和 popup 生命周期定向测试

### 2026-04-19 - 收口 phase 5 银行页服务端门禁与确认发送链测试

- 主题：只修 phase 5 严格验收指出的两处收口项：服务端银行 action 的 bank page 语义门禁，以及新壳转账确认 popup 到 `TerminalActionMessage` 发送链的直接测试
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalBankActionMessageFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalActionMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalBankActionMessageFactoryTest.java`、`docs/WORKLOG.md`
- 原因：上一轮严格验收确认，银行 action 在服务端仍可绕开 bank page 语义直接执行，同时 popup 测试只证明了新 modal 生命周期存在，还没有直接证明确认按钮继续走新 action/snapshot 主链
- 引用来源：`docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPopupFactory.java`
- 结果：服务端现在只会在当前 page 属于 bank 语义时执行 `BANK_OPEN_ACCOUNT` / `BANK_CONFIRM_TRANSFER`；同时把“转账确认后进入 `BANK_CONFIRM_TRANSFER` 主链”的消息构造收口到独立 helper，并新增对应定向测试，结合既有 `TerminalPopupFactoryTest` 一起证明确认链继续走新 popup + action/snapshot 主链，而不是回退到旧 Dialog 链

### 2026-04-19 - 产出终端 phase 6 市场总览与标准商品市场迁移 Prompt

- 主题：补出一份只面向 MARKET 总入口与标准商品市场迁移的 phase 6 执行 prompt，并把 phase 6 到 phase 9 的职责边界继续收口固定下来，避免下一轮又把 custom / exchange / cutover / 删除旧实现混在一起
- 影响范围：`docs/terminal-betterquesting-ui-phase6-market-overview-standardized-prompt-2026-04-19.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 5 已经把银行页迁成新壳上的第一张完整业务页，下一步必须直接让 MARKET 根页和标准商品市场进入新壳，证明 terminal 已能承接第二类复杂业务页；同时用户已经明确 phase 7 到 phase 9 的顺序，需立即固化到文档中避免后续节奏漂移
- 引用来源：`docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md`、`docs/market-three-part-architecture.md`、`docs/market-entry-overview.md`、`docs/standardized-market-catalog-boundary.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`
- 结果：现已新增一份 phase 6 prompt，明确本轮只迁 MARKET 总入口与标准商品市场真实业务页，要求继续沿用 selectedPageId / action / snapshot 主链、保留 MARKET 根页作为总入口语义、补至少一条真实市场动作回写闭环和一条新 popup 确认链，并明确把定制商品市场、汇率市场、正式 cutover 与旧 terminal ModularUI 删除留给 phase 7 到 phase 9

### 1. 项目定位

- `CustomClient` 不是当前主要 Java 源码仓
- 真正的开发主体是 `JsirGalaxyBase`
- `JsirGalaxyBase` 目标不是普通业务后台，而是：
  - `GTNH 服务器制度核心模组`
  - 并预留后续玩法能力扩展

### 2. 架构总判断

- 不采用传统 Web 框架优先的思路
- 不采用早期扁平模组写法继续扩展
- 当前正式架构定为：
  - `模块化单体`
  - `制度核心 + 能力模块`
  - `服务端权威`
  - `可替换持久化`

### 3. 当前模块边界

- `制度核心模块`
  - 职业
  - 经济
  - 贡献度 / 声望
  - 公共订单 / 公共工程
  - 群组服同步核心状态
- `能力模块`
  - 共享背包
  - 市场终端
  - 其他玩法增强能力
- `诊断模块`
  - 客户端物品导出
  - 开发观测工具

### 5. 本轮代码重构结果

- 删除了旧的示例式写法：
  - `HelloWorldCommand`
  - 旧 `CommonProxy`
  - 旧 `ClientProxy`
  - 旧扁平 `Config`
  - 旧 `client` 包中的导出控制器
- 引入新的启动和模块骨架：
  - `bootstrap/`
  - `module/`
  - `modules/core/`
  - `modules/capability/`
  - `modules/diagnostics/`

## 条目

### 2026-04-19 - 新终端壳接入 MARKET 总入口与标准商品市场

- 主题：把 MARKET 根页和 MARKET_STANDARDIZED 接到 BetterQuesting 风格 `TerminalHomeScreen` 新壳，并补真实标准商品 action / snapshot / popup 闭环
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalActionType.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalMarketActionPayload.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalMarketSectionSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalMarketActionMessageFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalMarketSectionModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionState.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSectionService.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalMarketActionMessageFactoryTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 5 已把银行页迁成新壳上的第一张完整业务页，但 MARKET 根页仍只有占位 section，标准商品市场的真实交易链仍停在旧终端实现，无法满足 phase 6 的范围约束和动作链要求
- 结果：新壳现在会为 MARKET 顶层 page snapshot 携带专用 market section model；MARKET 根页改成共享摘要 + 入口卡，MARKET_STANDARDIZED 改成真实标准商品 section；至少一条真实标准商品动作和一条后处理动作现在通过 `TerminalActionMessage -> TerminalSnapshotMessage` 回写；确认买单与 claim 走新 popup 生命周期；custom / exchange、cutover 与旧 ModularUI 删除仍保持出界

### 2026-04-19 - 产出终端 phase 6 严格验收收口 Prompt

- 主题：补出一份只面向 phase 6 严格验收缺口的收口 prompt，聚焦新壳滚动能力、标准商品市场布局密度和关键数据截断问题
- 影响范围：`docs/terminal-betterquesting-ui-phase6-close-prompt-2026-04-19.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 6 主链已经接通，但严格验收与实际 runClient 目视验证确认 MARKET_STANDARDIZED 仍存在无法滚动、全屏下信息可见面积不足，以及商品 / claim / 规则等内容被固定上限主动裁断的问题；这些缺口会阻塞 phase 7，但又不应把任务扩大成 custom / exchange 迁移
- 结果：现已新增一份 phase 6 close prompt，明确要求只修 framework 层滚动输入与局部滚动宿主、TerminalHomeScreen 与 TerminalMarketSection 的空间策略，以及标准商品市场 section 的关键数据完整显示，并要求保住 phase 6 已成立的 action / snapshot / popup 主链和本地 client/server 联调验证

### 2026-04-19 - 收紧新终端壳缩放与 phase 6 市场页布局密度

- 主题：在 phase 6 收口已恢复滚动与完整数据浏览后，继续把新 terminal 壳的占屏比例和市场页内部 chrome 密度收紧，修复实机目视下仍然“整体过度放大”的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`docs/WORKLOG.md`
- 原因：上一轮主要通过放宽 shell 尺寸上限和引入局部滚动来解决“看不全、滚不动”，但实际进游戏目视后，新壳仍然过于贴满 GUI 视口，状态带、导航项、页头和市场卡片的间距也偏松，导致整体观感依然像被放大过头
- 结果：终端壳现在改为按视口比例取值而不是几乎贴满屏幕；状态带、导航项、页头、footer 与市场 section 的顶部留白、左右分栏比例和买单卡高度也同步压紧，在不回退滚动与长列表能力的前提下提高信息密度并减轻“过度放大”观感

### 2026-04-19 - 产出终端 phase 7 定制商品市场 / 汇率市场迁移 Prompt

- 主题：补出一份只面向 phase 7 的执行 prompt，把剩余 terminal 真实业务页全部迁进新壳，并把新壳对旧 terminal 市场装配的直接依赖残留收干
- 影响范围：`docs/terminal-betterquesting-ui-phase7-custom-exchange-residue-prompt-2026-04-19.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 6 与其收口轮已经把 standardized market 和新壳滚动/布局问题收住，下一步必须明确把 MARKET_CUSTOM、MARKET_EXCHANGE 和旧 market 装配残留一起放进同一阶段完成，避免 phase 8 cutover 前还残留“业务页未迁完”的模糊状态
- 结果：现已新增一份 phase 7 prompt，明确本轮只迁 custom market 与 exchange market 真实业务页、接通各自 action / snapshot / popup 主链，并收干新壳对旧 market builder / binder / dialog / session controller 的直接依赖；同时继续把正式 cutover 与旧 ModularUI 删除留给 phase 8 和 phase 9

### 2026-04-19 - 收口 phase 6 市场页滚动、布局和数据截断缺口

- 主题：在不进入 phase 7 的前提下，把新 terminal framework 与标准商品市场收口到可滚动、可完整浏览、可继续进入下一阶段的状态
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/framework/CanvasScreen.java`、`src/main/java/com/jsirgalaxybase/client/gui/framework/GuiPanel.java`、`src/main/java/com/jsirgalaxybase/client/gui/framework/PanelContainer.java`、`src/main/java/com/jsirgalaxybase/client/gui/framework/VerticalScrollPanel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContent.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/test/java/com/jsirgalaxybase/client/gui/framework/VerticalScrollPanelTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContentTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 6 运行态验收已经确认新壳没有滚轮输入通路、标准市场正文区尺寸偏紧、商品 / claim / 规则 / 盘口等关键数据仍被固定上限主动截断；如果不先收掉这些可用性问题，phase 7 就会建立在不可完整浏览的页面上
- 结果：framework 现在具备最小可复用滚动能力，滚轮事件会经 `CanvasScreen -> PanelContainer -> VerticalScrollPanel` 分发；`TerminalHomeScreen` 放宽了壳层尺寸上限并扩大正文区；`TerminalMarketSection` 改为大屏优先吃满、局部滚动承接小屏溢出，同时把商品浏览、盘口 / 我的订单、claim 和规则条目改成完整 child 生成，不再靠固定前 N 条硬裁；本轮仍保持 MARKET 根页只做总入口，MARKET_CUSTOM / MARKET_EXCHANGE、cutover 与旧 ModularUI 删除继续明确留给 phase 7 之后

### 2026-04-05 - 恢复终端内容区滚动能力

- 主题：恢复终端导航列与正文列的局部滚动能力，同时保留已经验证有效的单页宿主切页结构
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：上一轮为收口点击与布局问题，先移除了局部滚动控件，结果正文区虽然可正常切页，但较长内容不再能滚动，等于把原本功能一起删掉了
- 结果：把导航列和正文列的 `createScrollableBody(...)` 恢复为真正的 `ListWidget` 局部滚动容器，但不恢复“所有页面同时挂载”的旧正文结构；这样保留单页宿主修复成果的同时，拿回原本的滚动能力

### 2026-04-05 - 补回银行与市场输入框的同步注册

- 主题：补回银行转账表单和市场交易表单在终端面板里的输入同步注册，恢复原本可正常输入的行为
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`docs/WORKLOG.md`
- 原因：客户端崩溃日志明确指向 `Sync handler is not yet initialised!`，说明文本框绑定的 `StringSyncValue` 被创建了，但没有注册进 `PanelSyncManager`；打字时触发 `onTextChanged` 就会直接崩溃
- 结果：已补回银行转账的收款人 / 金额 / 备注字段，以及市场限价 / 即时交易数量字段的 `syncManager.syncValue(...)` 注册；这样恢复原有文本输入与服务端同步能力，而不是继续靠删功能规避问题

### 2026-04-04 - 修复终端主界面打开后整体不可点击

- 主题：移除终端主界面导航列与正文列对 `ListWidget` 的顶层包裹，改回普通布局容器，收口打开终端后整页无法点击的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：最新 client log 在终端打开瞬间连续出现大量 `MUI [SIZING][Column]: Can't cover children when all children depend on their parent!`，而终端主页唯一的 `ListWidget` 同时包住导航和正文，最符合尺寸循环与点击事件被滚动容器吞掉的现象
- 结果：终端主界面改为直接使用普通 `Flow.column()` 承载导航与正文内容，避免顶层滚动容器参与尺寸推导和输入捕获；后续点击验证以最新构建实装结果为准

### 2026-04-04 - 汇率市场 GUI 回归测试最后收口

- 主题：只补 exchange 子页正式 quote / 空状态 / 控制器门禁的回归测试，不再扩实现范围
- 影响范围：`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeQuoteViewTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSessionControllerTest.java`、`docs/WORKLOG.md`
- 原因：上一轮已经把 exchange 子页 GUI 主链落地，但关键事实仍主要依赖人工阅读，需要把正式字段映射、空状态和执行门禁锁成稳定回归
- 结果：新增 exchange 页空状态回归、TerminalMarketService 的 exchange 空状态映射回归，以及 TerminalMarketSessionController 的确认兑换门禁与本地错误反馈回归

### 2026-04-04 - 汇率市场子入口第一轮正式 GUI 实装

- 主题：把汇率市场子页从说明页改成真实可操作的终端页，支持正式 quote 预览、确认弹窗和 GUI 内兑换执行
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeQuoteView.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncState.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSessionController.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSessionController.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/ExchangeMarketService.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeQuoteViewTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/ExchangeMarketServiceTest.java`、`docs/market-entry-overview.md`、`docs/WORKLOG.md`
- 原因：汇率市场子入口已经完成路由拆分，但终端页仍停留在静态说明状态，玩家必须退回命令行才能看到正式 quote 字段和完成兑换，不符合汇率市场子入口的正式 GUI 实装目标
- 引用来源：`docs/market-total-entry-split-tail-close-prompt-2026-04-04.md`、`docs/market-entry-overview.md`
- 结果：汇率市场子页现在会读取当前手持物品并走正式 quote 路径，展示 pairCode / assetCode / ruleVersion / limitStatus / reasonCode / notes 等字段；支持“刷新报价”和“确认兑换”两步动作；确认后通过终端弹窗走服务端兑换，并刷新银行摘要与终端通知

### 2026-04-04 - MARKET 总入口正式拆成三类市场入口

- 主题：把终端与命令层里仍然像“统一 MARKET 大桶”的入口残留，正式拆成标准商品市场、定制商品市场、汇率市场三类入口
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalPageTest.java`、`docs/market-entry-overview.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：三市场边界已经定稿，但 MARKET 根页仍直接落到标准商品交易详情页，命令帮助也仍停留在“统一 market 兼容入口”语义，继续放任会让后续 GUI 和命令补丁再次回到混合大市场
- 引用来源：`docs/market-total-entry-split-prompt-2026-04-04.md`、`docs/market-three-part-architecture.md`、`docs/custom-market-minimal-model.md`
- 结果：MARKET 首页现在只负责总览和三类市场入口；标准商品交易页被下沉到独立子页；定制商品市场和汇率市场有了各自终端落点页；`/jsirgalaxybase market` 帮助明确按三类市场入口分组，并补了一条终端路由层单测

### 2026-04-04 - 产出 MARKET 总入口拆分阶段收口 Prompt

- 主题：为 MARKET 总入口拆分阶段补出最后一轮收口 prompt，聚焦真实路由回归测试和 docs 索引补齐
- 影响范围：`docs/market-total-entry-split-tail-close-prompt-2026-04-04.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：阶段主体已经成立，但当前所谓“终端路由回归测试”仍主要停留在 `TerminalPage` 枚举元数据层，且 `market-entry-overview.md` 尚未正式挂入 docs 索引，还不适合直接无保留关阶段
- 引用来源：`docs/market-total-entry-split-prompt-2026-04-04.md`、`docs/market-entry-overview.md`
- 结果：新增一份只针对最后两处真实缺口的收口 prompt，要求下一轮只补 MARKET 根页与三个子市场页的真实装配回归测试，并同步补齐 README 索引与 WORKLOG 记录

### 2026-04-04 - 产出 MARKET 总入口拆分阶段 Prompt

- 主题：为三市场执行顺序中的最后一步补出 MARKET 总入口拆分 prompt，要求入口层正式按标准商品市场、定制商品市场、汇率市场三类入口收口
- 影响范围：`docs/market-total-entry-split-prompt-2026-04-04.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：汇率市场规则层、标准商品市场目录边界、定制商品市场最小挂牌链都已经完成验收；如果此时还不拆 MARKET 总入口，后续终端和命令层仍会继续沿“统一 MARKET 大桶”漂移
- 引用来源：`docs/market-three-part-architecture.md`、`../../Docs/下次对话议程.md`、`../../Docs/市场经济推进.md`
- 结果：新增一份只针对 MARKET 总入口拆分的正式 prompt，明确这一轮只处理入口、路由、文案和命令帮助分组，不继续扩三类市场各自的业务语义

### 2026-04-04 - 修复终端 GUI 转账表单未同步到服务端

- 主题：修复银行终端 GUI 在点击确认后只显示“已提交”但服务端实际拿到空表单，导致转账并未真正执行的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`docs/WORKLOG.md`
- 原因：终端表单字段虽然绑定了 `StringSyncValue`，但没有通过 `syncManager.syncValue(...)` 注册到 panel sync manager；因此 synced action 在服务端执行时读到的仍是默认空值，命令行链路不受影响所以表现正常
- 结果：银行转账的收款玩家、金额、备注现在会随面板同步到服务端；同时把市场页的限价/即时交易输入一起补上同步注册，避免同类“GUI 看似提交、服务端实际空参数”的问题再次出现

### 2026-04-14 - 落地终端 BetterQuesting 风格 GUI framework 第一阶段地基

- 主题：在不迁移现有 terminal 主链的前提下，新增仓库自有命名空间下的最小 BetterQuesting 风格 GUI framework、theme registry 与占位测试屏
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/framework/`、`src/main/java/com/jsirgalaxybase/client/gui/theme/`、`src/main/java/com/jsirgalaxybase/terminal/client/`、`src/main/resources/assets/jsirgalaxybase/textures/gui/framework/`、`src/main/java/com/jsirgalaxybase/terminal/TerminalClientBootstrap.java`、`docs/WORKLOG.md`
- 原因：终端长期 GUI 路线已经明确转向仓库内置的 BetterQuesting 风格 `GuiScreenCanvas + panel tree + theme registry` 体系；本轮需要先把 framework 地基、最小主题骨架和独立可运行 screen 跑通，而不是提前混入 terminal 页面迁移和协议重构
- 引用来源：`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/GuiScreenCanvas.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/IScene.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/IGuiPanel.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/CanvasTextured.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/controls/PanelButton.java`、`Reference/BetterQuesting/src/main/java/betterquesting/client/themes/ThemeRegistry.java`
- 结果：当前仓库已新增自有的 root screen / scene / panel container / textured canvas / button / popup / theme registry 最小集合，并补出 `TerminalFrameworkTestScreen` 与 client-only 调试热键用于验证根屏、panel 绘制、按钮回调、popup 开关和主题资源访问；现有 `TerminalService.openTerminal(...)`、`TerminalModule` 的旧 `ModularUI` factory 主链与网络协议均保持不动

### 2026-04-14 - 产出 BetterQuesting 风格 UI framework 第二阶段打开链执行 Prompt

- 主题：补出一份只面向终端打开链改造的第二阶段执行 prompt，明确本轮只把 terminal 从“服务端直接开 `ModularUI`”推进到“服务端授权 + 客户端开自有 screen”的最小闭环
- 影响范围：`docs/terminal-betterquesting-ui-phase2-open-chain-prompt-2026-04-14.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：第一阶段 framework 地基已经落下，下一步最关键的不是提前迁银行页和市场页，而是先把新 framework 真正接进 terminal 主链；同时用户已明确当前阶段只按静态验证口径推进，不要求启动游戏做运行态验收
- 引用来源：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase1-framework-prompt-2026-04-14.md`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalMessage.java`、`src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java`
- 结果：现已新增一份 phase 2 prompt，范围收口到新终端打开协议、最小 `TerminalHomeScreen` 占位壳、初始化 snapshot / session model、F8 调试入口收口以及静态编译验证，为后续首页壳和业务页迁移提供正确宿主

### 2026-04-15 - 产出 BetterQuesting 风格 UI framework 第三阶段首页壳执行 Prompt

- 主题：补出一份只面向新 terminal 首页壳和共用组件层的第三阶段执行 prompt，明确本轮只推进 `TerminalHomeScreen`、导航壳、通知宿主、popup 宿主与共用 panel 组件，不提前迁业务页
- 影响范围：`docs/terminal-betterquesting-ui-phase3-home-shell-prompt-2026-04-15.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 2 已经把 terminal 打开链切到“客户端请求、服务端授权、客户端开 `TerminalHomeScreen` 占位根屏”，下一步最关键的不是抢先迁银行页和市场页，而是先把新首页壳做成后续所有业务页的真正宿主
- 引用来源：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase2-open-chain-prompt-2026-04-14.md`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/TerminalClientScreenController.java`
- 结果：现已新增一份 phase 3 prompt，范围收口到首页壳结构、顶栏 / 左侧导航 / 主体区、全局通知与 popup 挂载位、共用 shell / panel 组件抽取以及静态编译验证，为后续银行页与市场页迁移提供稳定壳层

### 2026-04-05 - 收口终端表单双端同步缺口并沉淀排障文档

- 主题：把终端里所有“客户端输入或选择后，再由服务端 synced action 消费”的字段统一改成双端同步注册，而不是只做本地 getter/setter 绑定
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`docs/terminal-sync-form-regression-2026-04-05.md`、`docs/WORKLOG.md`
- 原因：上一步虽然补回了 `syncManager.syncValue(...)` 注册，解决了输入时崩溃，但银行转账实测仍出现“GUI 提交后服务端不处理”；继续排查后确认根因是可编辑字段需要区分客户端缓存和服务端真实会话值，否则服务端动作仍可能读到空值或旧值
- 结果：银行转账表单、市场商品选择、限价/即时交易输入、取消订单与领取托管等动作参数都改成客户端缓存 + 服务端 getter/setter 的双端同步模式，并补一份单独 bug 文档供后续排障复用

### 2026-04-05 - 恢复双端同步后的终端按钮门禁

- 主题：修复银行转账按钮与市场确认按钮在双端同步改造后长期被客户端误判为不可用的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankPageBuilder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`、`docs/WORKLOG.md`
- 原因：可编辑字段切到客户端缓存 + 服务端 getter/setter 之后，页面按钮的 enabled 判定如果仍读取本地 sessionController，就会一直看到旧空值，表现成“按钮消失”或始终灰掉
- 结果：银行转账、市场限价/即时单、撤单/提取、存入以及兑换确认等入口现在统一基于当前 sync 值判定；同时补齐盘口价格预填时对 sync 文本框的直接回写，避免按钮和输入状态再次脱节

### 2026-04-05 - 市场终端从 page-first 收口为 asset-first 导航壳

- 主题：把市场终端正式切成标准商品、定制商品、汇率三条独立状态链，并把三类子页都改成先找对象再进详情的节奏
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalCustomMarket*.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeMarket*.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalCustomMarketSessionControllerTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeMarketSessionControllerTest.java`、`docs/WORKLOG.md`
- 原因：此前终端虽然已经完成三市场路由拆分，但运行时状态仍然集中在统一 market controller/snapshot 里，custom 仍停留在说明页，exchange 仍更像手持说明板，不符合 asset-first 终端目标
- 结果：标准商品市场默认停留在商品浏览层，不再自动选中首个商品；定制商品市场新增 listing-first 的浏览范围、详情与 buy/cancel/claim 确认链；汇率市场新增明确的兑换标的入口与详情层；终端总装配也改成标准商品 / 定制商品 / 汇率三套独立 controller + sync binder，并补了对应回归测试

### 2026-04-05 - 修复银行失败反馈不出泡泡与 DevB 开户抢跑崩溃

- 主题：修正银行终端在 GUI 打开时 HUD 泡泡被全局屏蔽的问题，并去掉开户按钮对未初始化 `InteractionSyncHandler` 的依赖
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalWidgetFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankPageBuilder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalHudOverlayHandler.java`、`docs/WORKLOG.md`
- 原因：一方面 HUD overlay 之前在任何 `currentScreen != null` 的情况下都会停止渲染，所以终端内触发的银行失败反馈虽然进入了通知队列，却不会冒泡显示；另一方面开户按钮直接绑定 `InteractionSyncHandler`，在 ModularUI sync manager 尚未完成初始化时被 DevB 抢先点击，会触发 `Sync handler is not yet initialised!`
- 结果：银行开户改成普通本地按钮 + synced action 提交，不再走会抢跑的 `InteractionSyncHandler`；银行转账也改成只展示服务端真实反馈；终端所用的 ModularUI 屏幕打开时 HUD 泡泡现在仍可渲染，转账失败和开户结果都能在 GUI 期间直接看到

### 2026-04-05 - 统一终端 GUI 回归事故文档

- 主题：把终端银行 GUI、转账、sync、按钮门禁、失败提示与开户点击崩溃这整套连续回归，从多份零散事故文档统一收口到单一文档
- 影响范围：`docs/terminal-gui-regression-chain-2026-04-05.md`、`docs/terminal-g-key-fatal-sync-incident-2026-04-04.md`、`docs/terminal-sync-form-regression-2026-04-05.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：之前事故记录已经拆成“G 键 fatal 断线”和“表单双端同步回归”两份文档，再叠加 WORKLOG 里的按钮门禁与 HUD 泡泡问题，后续排障需要来回跳文档，信息已经碎片化
- 结果：新增一份统一事故文档，按整条回归链重新整理银行 GUI、转账、按钮、开户与失败反馈问题；原先两份事故文档改成并入说明；docs 索引同步指向新的统一入口

### 2026-04-04 - 定制商品市场最小挂牌链 v1 最后一轮收口

- 主题：补齐定制商品市场最小挂牌链 v1 的完结动作、单件边界和市场 JDBC 列级 fail-fast 校验
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/custom-market-minimal-model.md`、`docs/custom-market-minimal-listing-chain-prompt-2026-04-03.md`、`docs/market-postgresql-ddl.sql`、`ops/sql/migrations/20260404_002_align_custom_market_single_item_claim_completion.sql`、`docs/WORKLOG.md`
- 原因：上一轮实现已经落地最小挂牌、浏览、购买、下架与 pending 主链，但仍存在 `COMPLETED` 不可达、代码仍允许堆叠挂牌、市场 JDBC 只校验表不校验列这 3 个真实收口缺口
- 引用来源：`docs/custom-market-minimal-listing-chain-tail-close-prompt-2026-04-04.md`、`docs/custom-market-minimal-model.md`、`ops/sql/migrations/20260404_001_add_custom_market_minimal_listing_chain.sql`
- 结果：新增买家 `claim` 完结动作，把交付状态闭环到 `COMPLETED` 并清空双方 pending；发布与快照统一收紧为单件挂牌；市场 JDBC 现在和银行一样对缺列 schema 直接 fail-fast，并提示运维运行 `scripts/db-migrate.sh`

### 2026-04-04 - 验收补出定制商品市场最小挂牌链收口 Prompt

- 主题：在定制商品市场最小挂牌链 v1 验收后，补出只针对剩余闭环缺口的收口 prompt
- 影响范围：`docs/custom-market-minimal-listing-chain-tail-close-prompt-2026-04-04.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前实现已经具备挂牌、浏览、购买、下架与 pending 主链，但验收发现仍存在交付状态机未闭环、单件商品边界与实现不一致、市场 JDBC 缺少 fail-fast 列级 schema 校验这 3 个真实收口缺口
- 引用来源：`docs/custom-market-minimal-listing-chain-prompt-2026-04-03.md`、`docs/custom-market-minimal-model.md`
- 结果：新增一份窄范围收口 prompt，要求下一轮只补 pending 到 completed 的完结动作、统一单件商品边界、以及市场 JDBC 的 fail-fast schema 校验，不再继续扩大功能面

### 2026-04-04 - 定制商品市场最小挂牌链 v1 正式落地

- 主题：为定制商品市场补上独立于标准商品订单簿的最小挂牌、快照、购买、下架与 pending 审计主链
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/market-postgresql-ddl.sql`、`docs/custom-market-minimal-model.md`、`ops/sql/migrations/20260404_001_add_custom_market_minimal_listing_chain.sql`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前仓库已经明确三市场分工，但定制商品市场此前没有正式实现；如果继续把非标商品硬塞进标准商品订单簿，会再次模糊标准商品市场与定制商品市场的边界
- 引用来源：`docs/custom-market-minimal-listing-chain-prompt-2026-04-03.md`、`docs/market-three-part-architecture.md`、`../../Docs/市场经济推进.md`、`../../Docs/下次对话议程.md`
- 结果：新增 `CustomMarketListing / ItemSnapshot / TradeRecord / AuditLog` 和对应 JDBC 仓储、DDL、migration、应用服务与兼容命令入口；发布时保存手持物快照，购买后进入定制商品市场自己的 `BUYER_PENDING_CLAIM` 语义，并可通过 `market custom pending` 查看卖家/买家侧待完结记录

### 2026-04-03 - 银行 PostgreSQL 改为显式版本化迁移与 fail-fast schema 校验

- 主题：把银行数据库结构变更从“应用启动时隐式补列”调整为“运维显式执行版本化 migration，应用启动只做 fail-fast 校验”
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankingInfrastructureFactory.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`scripts/db-migrate.sh`、`ops/sql/migrations/20260403_001_align_banking_ledger_entry_frozen_balances.sql`、`docs/postgresql-schema-migrations.md`、`docs/postgresql-local-setup-and-migration.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：数据库结构漂移不应由应用在启动时静默修复；更稳妥的做法是使用可审计的版本化 migration，在停服窗口由运维显式执行，应用只负责在发现旧 schema 时拒绝启动并给出升级入口
- 结果：新增 `scripts/db-migrate.sh` 和版本化 migration 目录/记录表，本地开发库已通过正式 migration 入口完成修复；银行 JDBC 初始化现在会在 schema 过旧时直接提示运维运行 migration，相关 PostgreSQL 集成测试也已改成校验 fail-fast 行为

### 2026-04-03 - 银行 JDBC 基础设施补上旧版 ledger_entry 列自动修复与列级校验

- 主题：修复本地银行转账在写入 `ledger_entry` 时因旧 PostgreSQL 表结构缺列而失败的问题，并把 schema 校验前移到基础设施初始化阶段
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankingInfrastructureFactory.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`docs/WORKLOG.md`
- 原因：旧开发库的 `ledger_entry` 缺少 `frozen_balance_before` / `frozen_balance_after`，但启动阶段此前只验证“表存在”，没有验证“列齐全”，导致问题直到实际转账写库时才暴露
- 结果：银行 JDBC 基础设施初始化现在会先对旧版 `ledger_entry` 自动补齐冻结余额列，再对核心表执行列级校验；新增 PostgreSQL 集成测试，验证删掉这两列后初始化仍能自动修复并正常启动

### 2026-04-03 - 本地联调脚本改为先起 client 再进 server 控制台

- 主题：调整本地三进程联调脚本的启动顺序和前台行为，方便在服务端控制台直接给在线测试号发放初始资金和做转账联调
- 影响范围：`scripts/start-local-test-stack.sh`、`docs/WORKLOG.md`
- 原因：当前银行管理员命令和玩家间转账都依赖在线玩家身份解析；先让 `DevA` / `DevB` 等测试号进入客户端，再把当前终端切到前台 `runServer` 控制台，才能边看服务端日志边直接执行管理员资金操作
- 结果：脚本现在会先后台启动两个 client，再以前台方式启动 `runServer` 并把当前终端附着到服务端控制台；脚本退出时仍会清理它拉起的 client 进程

### 2026-04-03 - 编译并实装最新 JsirGalaxyBase 到实际 client/server mods 目录

- 主题：将当前标准商品市场目录边界收口后的最新构建产物编译完成，并直接实装到真实 client/server 的 mods 目录
- 影响范围：`docs/WORKLOG.md`
- 原因：本轮代码与测试已经完成，但本地 `runServer` 开发运行时在 FML 扫描 `org.antlr:antlr4:4.13.2` 时出现 class 读取异常，无法把“已编译产物可用”继续建立在 dev runtime 成功拉起上，因此改走真实 mods 目录实装路径
- 结果：
  - 使用 `assemble` 成功产出最新正式 jar：`build/libs/jsirgalaxybase-7545ce9-main+7545ce9201-dirty.jar`
  - 已将同一构建实装到客户端 `CustomClient/Galaxy GTNH 2.8.4/instances/Galaxy GTNH 284/.minecraft/mods/` 与服务端 `GroupServer/Galaxy_GTNH284_S1/mods/`
  - 双端落地文件大小一致，均为 `516806` bytes，可确认当前 client/server 已使用同一份 JsirGalaxyBase 构建产物等待后续启动生效

### 2026-04-03 - 标准商品市场目录边界改为单一运行时事实来源

- 主题：把标准商品市场目录 decision 从“命令层、终端层、服务层各自 new 默认目录”收口为同一运行时事实来源
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketServiceTest.java`、`docs/standardized-market-catalog-boundary.md`、`docs/WORKLOG.md`
- 原因：上一阶段虽然已经建立正式目录对象和统一语义，但命令层与终端层仍各自持有默认目录实例，未来一旦运行时目录来源或版本变化，仍可能与真实服务边界分叉
- 引用来源：`docs/standardized-market-catalog-boundary-tail-close-prompt-2026-04-03.md`、`docs/standardized-market-catalog-boundary.md`
- 结果：
  - `InstitutionCoreModule` 现在显式暴露运行时目录检查入口，命令层统一经由运行时 spot market service 取目录 decision
  - `TerminalMarketService` 的商品浏览、选中商品详情、下单、即时成交、手持存入判定已切到运行时目录 decision；默认目录只保留为运行时离线时的窄回退
  - 新增命令层与终端层测试，证明上层会跟随注入的运行时目录 version/source/reject decision，而不是依赖本地默认目录碰巧一致

### 2026-04-03 - 产出定制商品市场最小挂牌链分阶段 Prompt

- 主题：为三市场执行顺序中的下一阶段补出定制商品市场最小挂牌链实现 prompt，并明确按阶段逐步落地的边界
- 影响范围：`docs/custom-market-minimal-listing-chain-prompt-2026-04-03.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：标准商品市场目录边界已经收口，下一阶段不应继续扩标准商品市场或提前拆 MARKET 总入口，而应开始定义定制商品市场自己的挂牌、成交、待领取与交付留痕主链
- 引用来源：`docs/market-three-part-architecture.md`、`../../Docs/下次对话议程.md`、`../../Docs/市场经济推进.md`
- 结果：新增一份面向定制商品市场的正式实现 prompt，要求按“模型与 DDL -> 应用服务 -> 兼容入口 -> 测试与文档”逐阶段推进，并明确禁止复用标准商品市场订单簿与统一仓储模型来硬凑非标商品交易

### 2026-04-03 - 验收补出标准商品市场目录边界收口 Prompt

- 主题：在本轮验收后补出一个只针对“命令层、终端层、服务层仍各自持有默认目录实例”的收口 prompt
- 影响范围：`docs/standardized-market-catalog-boundary-tail-close-prompt-2026-04-03.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：标准商品市场正式目录对象、来源分层、服务层准入和测试主体都已成立，但命令层与终端层仍直接 `createDefaultCatalog(...)`，还没有完全收口成单一运行时目录边界
- 结果：新增一份窄范围收口 prompt，要求把目录 decision 收口为同一运行时事实来源，并补测试证明命令层与终端层不会再和真实服务边界分叉

### 2026-04-03 - 标准商品市场正式目录与准入边界落地

- 主题：把标准商品市场从直接依赖 GregTech 金属临时目录，推进为拥有正式目录版本、准入决策和来源分层的运行时边界
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/application/`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/`、`src/test/java/com/jsirgalaxybase/modules/core/market/infrastructure/jdbc/MarketPostgresIntegrationTest.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/standardized-market-catalog-boundary.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前标准商品市场撮合和仓储链已经能跑，但“哪些商品允许进入市场”仍主要被 `GregTechStandardizedMetalCatalog` 这类临时适配实现直接决定，导致正式制度边界没有独立出来
- 引用来源：`docs/market-three-part-architecture.md`、`../../Docs/市场经济推进.md`、`../../Docs/下次对话议程.md`
- 结果：
  - 新增 `StandardizedMarketCatalogVersion`、`StandardizedMarketCatalogEntry`、`StandardizedMarketAdmissionDecision`、`StandardizedMarketAdmissionReason`、`StandardizedMarketCatalogService`、`StandardizedMarketCatalogSource`
  - 当前默认目录版本已固定为 `standardized-spot-catalog-v1`
  - `GregTechStandardizedMetalCatalog` 已下沉为 `目录来源适配器`，不再承担正式制度边界语义
  - `StandardizedSpotMarketService`、命令层与终端层已统一走新的目录准入主路径，并可带出目录版本、准入 reason 与来源标识
  - 新增标准商品目录边界说明文档，并补齐目录服务、来源桥接、服务层拒绝语义与命令层输出测试

### 2026-04-03 - 产出标准商品市场商品目录与正式准入边界 Prompt

- 主题：为汇率市场规则层收口后的下一阶段，产出标准商品市场商品目录与正式准入边界实现 prompt
- 影响范围：`docs/standardized-market-catalog-boundary-prompt-2026-04-03.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：汇率市场规则层 v1 已完成收口，正式执行顺序应进入“标准商品市场商品目录与正式准入边界”；当前仓库仍主要依赖 `GregTechStandardizedMetalCatalog` 作为临时准入来源，需要下一阶段 prompt 明确把正式目录边界与临时适配来源拆开
- 引用来源：`docs/market-three-part-architecture.md`、`../../Docs/市场经济推进.md`、`../../Docs/下次对话议程.md`
- 结果：新增一份只针对标准商品市场商品目录与正式准入边界的 prompt，并同步到 docs 索引，作为汇率市场之后的下一阶段实现依据

### 2026-04-03 - 汇率市场兼容 quote 桥收口禁兑结果过早抛错

- 主题：修补汇率市场兼容桥在 `quote hand` 上把禁兑报价过早抛成异常的问题，确保禁兑任务书硬币也能返回正式规则层报价
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangeService.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangeServiceTest.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/WORKLOG.md`
- 原因：正式规则层已经能返回 `DISALLOWED` 报价，但兼容桥此前把 preview 与 execute 共用成“必须可执行”路径，导致 `/jsirgalaxybase market quote hand` 无法展示正式规则字段，并把禁兑误退化成普通拒绝
- 引用来源：`docs/exchange-market-rules-layer-v1-tail-close-prompt-2026-04-03.md`、`docs/market-three-part-architecture.md`
- 结果：
  - 兼容桥已拆开 `preview` 与 `execute` 判断：报价路径允许返回 `DISALLOWED` 正式报价，执行路径仍然在吞物前拒绝不可执行输入
  - `/jsirgalaxybase market quote hand` 现在会输出 `pair`、`ruleVersion`、`limitStatus`、`reasonCode`、`notes` 等正式字段，不再把禁兑误报成“非汇率市场资产”
  - `/jsirgalaxybase market exchange hand` 对同类禁兑输入仍保持拒绝执行语义，且错误消息来自正式规则层 note
  - 新增桥接层测试与命令输出测试，并通过针对性 Gradle 验证

### 2026-04-03 - 验收补出汇率市场兼容 quote 桥收口修补 Prompt

- 主题：在本轮验收后补出一个只针对“禁兑报价被兼容桥过早抛错”的收口修补 prompt
- 影响范围：`docs/exchange-market-rules-layer-v1-tail-close-prompt-2026-04-03.md`、`docs/WORKLOG.md`
- 原因：正式规则层已经能返回 `DISALLOWED` 报价，但兼容桥仍会把这类结果提前抛成异常，导致 `/jsirgalaxybase market quote hand` 无法展示正式 `limitStatus / reasonCode / notes`
- 结果：新增一份窄范围修补 prompt，明确要求把 preview 与 execute 判断拆开，让 `quote hand` 能展示禁兑正式字段，而 `exchange hand` 仍保持拒绝执行

### 2026-04-03 - 汇率市场正式规则层 v1 落地并保留旧命令兼容入口

- 主题：把任务书硬币兑换从“能跑的固定规则入口”收口为汇率市场正式规则层 v1，同时保留旧 `/jsirgalaxybase market quote hand` 与 `/jsirgalaxybase market exchange hand` 作为兼容入口
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/application/`、`src/main/java/com/jsirgalaxybase/modules/core/market/domain/`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/WORKLOG.md`
- 原因：三市场结构已经定稿，当前最先要落的不是继续扩标准商品市场，而是把已有任务书硬币兑换正式归位为 `汇率市场` 规则层，并把旧 MARKET 文案从“商品市场一期”改正为“汇率市场兼容入口”
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/下次对话议程.md`、`docs/market-three-part-architecture.md`
- 结果：
  - 新增正式 `ExchangeMarket*` 规则对象与 `ExchangeMarketService`，把 pair、ruleVersion、limitPolicy、quote result、execution result 明确建模
  - 旧 `TaskCoinExchangeService` 已改为手持物品兼容桥，不再直接承载汇率市场主语义
  - 命令层和终端首页文案已改成“汇率市场兼容入口”，并在报价/执行输出中补充 pair、ruleVersion、limitStatus、reasonCode 等正式字段
  - 修复了任务书硬币不支持档位识别把 `IV` 误判成高阶禁用的问题
  - 已通过针对性测试：`GalaxyBaseCommandTest`、`TaskCoinExchangePlannerTest`、`ExchangeMarketServiceTest`

### 2026-04-03 - 新增本地三进程联调启动脚本

- 主题：为 dedicated server + 双 client 人工联调新增用户可自行执行的一键启动脚本，避免再依赖 agent 后台终端持有进程
- 影响范围：`scripts/start-local-test-stack.sh`、`docs/WORKLOG.md`
- 原因：用户需要自己在本地 shell 中稳定维持三进程联调，并且每次启动前都要自动清理旧的 client/server 进程，避免残留实例和共享 gameDir 干扰测试
- 结果：
  - 新增 `scripts/start-local-test-stack.sh`，会先搜索并杀掉当前工作区相关的 runServer/runClient 进程
  - 脚本按 `server -> client A -> client B` 顺序启动，并为两个 client 显式使用独立 gameDir
  - 脚本在前台作为 supervisor 持有三进程，按 `Ctrl-C` 会统一清理

### 2026-04-03 - 市场三分结构正式设计落库并归位旧实现

- 主题：把市场重新收口为标准商品市场、定制商品市场、汇率市场三条正式产品线，并把现有代码残留归位到正式设计文档
- 影响范围：`docs/market-three-part-architecture.md`、`docs/README.md`、`../Docs/市场经济推进.md`、`../Docs/下次对话议程.md`、`docs/WORKLOG.md`
- 原因：当前仓库虽然已经在制度文档里恢复三市场方向，但仓库内仍缺少一份明确说明“旧单一路线残留属于哪一类市场、哪些应冻结、接下来先做哪条线”的正式设计文档
- 结果：
  - 新增 `docs/market-three-part-architecture.md`，正式写清三类市场边界、共享能力、最小模型拆分、旧实现归位判断和下一阶段执行顺序
  - 明确现有标准化现货代码只属于 `标准商品市场早期残片`，任务书硬币兑换只属于 `汇率市场早期残片`
  - 明确当前还没有真正属于 `定制商品市场` 的正式实现，禁止再把旧 MARKET 页面继续扩成统一大市场
  - 同步更新文档索引与下次议程，把后续优先级改为“先收口三市场执行顺序，再细化汇率和回收规则”

### 2026-04-03 - 三市场下一阶段顺序同步修正

- 主题：把“下一阶段执行顺序”从包含已完成的三市场总设计步骤，修正为从汇率市场正式规则层开始
- 影响范围：`docs/market-three-part-architecture.md`、`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
- 原因：三市场总设计已经正式落库，后续执行顺序应从 `汇率市场正式规则层 -> 标准商品目录与准入边界 -> 定制商品市场最小挂牌链 -> MARKET 总入口拆分` 开始，避免继续把已完成前置步骤写成“下一阶段”
- 结果：正式架构文档与推进文档的下一阶段顺序已统一为从汇率市场正式规则层开始

### 2026-04-03 - 产出汇率市场正式规则层改代码 Prompt

- 主题：为三市场结构验收后的首个代码阶段产出汇率市场正式规则层改代码 prompt
- 影响范围：`docs/exchange-market-rules-layer-code-prompt-2026-04-03.md`、`docs/WORKLOG.md`
- 原因：三市场正式边界和下一阶段顺序已经验收，需要一个明确限制范围的代码 prompt，把当前 `TaskCoinExchange*` 早期入口收口成汇率市场正式规则层
- 结果：新增一份只针对汇率市场正式规则层的代码 prompt，明确禁止继续扩标准商品市场、定制商品市场或 MARKET 总入口

### 2026-04-03 - 撤销金属市场文档方向并恢复三市场结构

- 主题：撤销文档中把市场收窄为金属专场和单一路线的错误方向，统一恢复为标准商品市场、定制商品市场、汇率市场三层结构
- 影响范围：`../Docs/设定.md`、`../Docs/市场经济推进.md`、`../Docs/下次对话议程.md`、`docs/market-gui-phase1-product-detail-prompt-2026-04-02.md`、`docs/market-gui-phase1-tail-fix-prompt-2026-04-03.md`、`docs/banking-java-domain-draft.md`、`.github/agents/GalaxyMod.agent.md`、`docs/WORKLOG.md`
- 原因：用户确认此前讨论的正式市场结构一直是 `标准商品市场 / 定制商品市场 / 汇率市场`，文档里后续把它误收窄成单一商品专场属于方向漂移
- 结果：
  - 主文档已统一改回三市场结构，不再把标准商品市场默认等同于金属专场
  - 两份旧的市场 phase-one prompt 已降级为废弃说明，不再作为后续实现依据
  - 下一阶段已改成先补三市场总设计，再分别推进标准商品市场、定制商品市场和汇率市场边界

### 2026-04-03 - 产出市场三分结构下一阶段 prompt

- 主题：为后续市场开发产出新的下一阶段 prompt，明确从旧单一路线切回三市场结构后的正式执行顺序
- 影响范围：`docs/market-three-part-structure-next-phase-prompt-2026-04-03.md`、`docs/WORKLOG.md`
- 原因：用户要求直接给出下一阶段 prompt，而且当前最重要的不是继续堆旧 MARKET 页，而是先把 `标准商品市场 / 定制商品市场 / 汇率市场` 三条线的边界、共享能力和执行顺序固定下来
- 结果：
  - 新增三市场结构下一阶段 prompt，可直接交给下一轮实现者使用
  - prompt 已明确要求先做三市场总设计与旧实现归位，不再沿旧单一路线继续扩功能

### 2026-04-03 - 市场第一阶段收尾补齐 DDL 与 deposit 恢复收口

- 主题：把旧单一标准商品撮合方案的第一阶段尾差从 Java 语义层推进到 PostgreSQL DDL 与 deposit 恢复路径，避免 AVAILABLE / INVENTORY_DEPOSIT 只停留在枚举里
- 影响范围：`docs/market-postgresql-ddl.sql`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketService.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/MarketRecoveryService.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/`、`src/test/java/com/jsirgalaxybase/modules/core/market/infrastructure/jdbc/`
- 原因：最新审查确认还有两个阻塞点未收口：数据库 check constraint 仍未接受 `AVAILABLE / INVENTORY_DEPOSIT`，且 deposit 失败后会沿通用异常路径把库存语义漂移到错误状态
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/设定.md`
- 结果：
  - PostgreSQL DDL 已同步接受 `market_custody_inventory.custody_status = AVAILABLE` 与 `market_operation_log.operation_type = INVENTORY_DEPOSIT`
  - deposit 失败现在写入专用 recovery metadata，并由 `MarketRecoveryService` 显式收口，不再继续走通用 `escalateGeneric(...)`
  - 新增共享商品目录测试替身与目录校验测试，覆盖“允许的标准商品可通过、普通方块会被拒绝”
  - PostgreSQL 集成测试已切到“先 deposit、再卖出/撤单/claim”的新边界，并新增 deposit DDL 与恢复验证

### 2026-04-03 - 市场第一阶段收尾 prompt 补充阻塞修复与双 client 联调要求

- 主题：把第一阶段收尾 prompt 从“语义修补”补强到“阻塞修复 + 双 client 联调验收”版本
- 影响范围：`docs/market-gui-phase1-tail-fix-prompt-2026-04-03.md`、`docs/WORKLOG.md`
- 原因：最新审查确认除了统一仓储与标准化商品准入外，还存在 DDL 未同步与 deposit 恢复未收口两处阻塞；同时银行转账与市场交易都需要双人在线联调，不能再只靠单 client 自测
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/设定.md`
- 结果：
  - 在 prompt 中新增了 `AVAILABLE / INVENTORY_DEPOSIT` 的 DDL 同步要求
  - 在 prompt 中新增了 deposit 失败恢复或安全回滚必须显式收口的要求
  - 在 prompt 中把“与用户配合的双 client 人工联调”升级为硬性验收条件，明确覆盖银行转账与市场交易链路

### 2026-04-03 - 市场第一阶段收尾修复 prompt

- 主题：为市场终端第一阶段补齐统一仓储与标准化商品准入两处尾巴，产出收尾修复 prompt
- 影响范围：`docs/market-gui-phase1-tail-fix-prompt-2026-04-03.md`、`docs/WORKLOG.md`
- 原因：首轮 MARKET 商品详情页虽然已经接线完成，但审查发现卖单仍依赖手持扣物、标准化商品仍缺少集中准入校验，这两点会直接破坏既定市场边界
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/设定.md`
- 结果：
  - 明确当前已有的是统一市场托管库存与 CLAIMABLE 资产链路，而不是完整的统一仓储可卖库存闭环
  - 要求后续修复优先在现有 market custody inventory 上补 `AVAILABLE` 一类可卖状态，而不是再拆一套平行仓储系统
  - 要求新增集中式标准化商品准入校验，并强制命令层、终端层与服务层统一收口

### 2026-04-03 - 市场终端第一轮旧单一路线商品详情页落地

- 主题：把终端 MARKET 页从静态占位页切到旧单一标准商品撮合方案的商品详情页，并补齐最小本地测试
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/`、`src/main/java/com/jsirgalaxybase/modules/core/market/infrastructure/jdbc/JdbcMarketOrderBookRepository.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSessionControllerTest.java`、`docs/WORKLOG.md`
- 原因：市场 GUI 第一轮目标已经从 prompt 明确为“商品点击后的交易详情页”，继续保留只读占位页会阻塞真实市场终端的后续联调与视觉验收
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/设定.md`
- 结果：
  - 新增市场终端的 snapshot、service、session controller、sync binder、sync state 与 page builder，MARKET 页现在可浏览商品、查看买卖盘、提交限价/即时交易、查看个人订单并发起撤单与 CLAIMABLE 提取
  - `TerminalHomeGuiFactory` 已正式接入市场页主面板、6 个确认弹窗和市场 toast/HUD 转发，不再复用旧只读详情页壳
  - 市场订单簿 JDBC 查询现在包含 `PARTIALLY_FILLED`，GUI 可看到剩余可成交深度
  - 新增市场终端本地纯逻辑测试，覆盖输入 sanitize、数量解析与待处理 ID 标记规则

### 2026-04-03 - 修复终端打开即断线的 ModularUI sync 冲突

- 主题：修复按 `G` 打开终端时服务端因 ModularUI sync handler auto/manual 注册冲突而踢线的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`docs/WORKLOG.md`
- 原因：市场页 6 个数值输入框对应的 `StringSyncValue` 被 market binder 手工 `syncValue(...)` 注册后，又被 `TextFieldWidget.value(...)` 作为 auto sync handler 收集，触发 `Old and new sync handler must both be either not auto or auto!`
- 结果：改为与银行页一致，只保留文本框侧的 auto 注册，终端主界面打开时不再因 `jgb_terminal` 包处理中的 sync 冲突被服务端断开

### 2026-04-04 - 修复 G 键打开终端导致 fatal disconnect 的 sync 回退

- 主题：修复按 `G` 打开终端时再次触发的 ModularUI sync auto/manual 混链断线，并补独立事故文档
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`docs/terminal-g-key-fatal-sync-incident-2026-04-04.md`、`docs/WORKLOG.md`
- 原因：市场页 6 个数值输入框的 `StringSyncValue` 又被手工 `syncValue(...)` 注册回 binder，和 `TextFieldWidget.value(...)` 的 auto 注册重新混在同一条 sync 链上，服务端在 `TerminalHomeGuiFactory.open(...)` 期间抛 `Old and new sync handler must both be either not auto or auto!`，客户端因此被 fatal 断开
- 结果：再次移除这 6 个数值输入框的手工 sync 注册，只保留文本框自身的 auto 注册；补充独立事故文档沉淀症状、堆栈、修复方式与复发预防点

### 2026-04-04 - 修复脚本联调环境下银行转账输入框再次触发终端 fatal

- 主题：修复 `scripts/start-local-test-stack.sh` 联调时，按 `G` 打开终端仍因银行页转账输入框重复 sync 注册而 fatal 断线的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`、`docs/terminal-g-key-fatal-sync-incident-2026-04-04.md`、`docs/WORKLOG.md`
- 原因：市场页 6 个文本输入框虽然已经移除手工 sync，但银行页的 `bankTransferTargetName`、`bankTransferAmountText`、`bankTransferComment` 仍同时走了 binder 手工 `syncValue(...)` 和 `TextFieldWidget.value(...)` auto sync；终端首页会一并收集 bank 页面控件，因此打开终端时仍会触发同一个 `Old and new sync handler must both be either not auto or auto!`
- 结果：移除银行转账 3 个输入框的手工 sync 注册，保留文本框自身 auto sync；事故文档同步扩大到 market 与 bank 两类文本输入控件，避免以后只修一半

### 2026-04-02 - 市场 GUI 第一轮商品详情页 prompt

- 主题：为终端市场页从只读占位升级到“点击商品后的交易详情页”产出第一轮实现 prompt
- 影响范围：`docs/market-gui-phase1-product-detail-prompt-2026-04-02.md`、`docs/WORKLOG.md`
- 原因：终端第三轮工程收口已经完成，下一阶段不应继续空谈市场页，而要把用户点击商品后的真实交易详情页结构、动作和验收范围写成可直接执行的实现规格
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/设定.md`、`Reference/VendingMachine/src/main/java/com/cubefury/vendingmachine/blocks/gui/`
- 结果：
  - 明确市场页第一轮目标是商品点击后的交易详情页，而不是完整市场总站
  - 把订单簿、下单区、个人订单区、仓储/冻结/待领取区和规则提示区列为首轮必做
  - 明确 VendingMachine 只作为浏览与状态表达参考，不照搬自动售货机式快捷交易交互

### 2026-04-02 - Terminal GUI 第三轮工程收口与市场前置整理

- 主题：在不重写终端框架的前提下，对终端主工厂、通知语义、Dialog 壳层和纯逻辑测试做市场 GUI 前的最后一轮工程收口
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/`、`src/main/java/com/jsirgalaxybase/terminal/TerminalHudNotificationManager.java`、`src/test/java/com/jsirgalaxybase/terminal/`、`src/test/java/com/jsirgalaxybase/terminal/ui/`、`docs/terminal-plan.md`
- 原因：第二轮以后主要风险已经从“能不能做出来”转成“市场 GUI 接进来后主工厂会不会继续膨胀，以及通知/弹窗/本地表单逻辑是否缺少最小保护”
- 结果：
  - 把银行页面构建、银行本地会话状态、银行 sync 绑定和通用 widget helper 从 `TerminalHomeGuiFactory` 明确拆出
  - `TerminalNotification` 现在优先消费结构化 `TerminalActionFeedback`，银行旧文本推断仅保留为 fallback 兼容桥接
  - `TerminalDialogFactory` 升级为 `TerminalDialogSpec` 驱动，支持 severity、尺寸预设和更稳的长正文/detail lines 容器
  - 新增终端侧纯逻辑测试，覆盖通知构造、HUD 队列行为、银行本地 sanitize/parse 和 Dialog 配置默认值

### 2026-04-02 - Terminal GUI 第三轮精修 prompt：工程收口与市场 GUI 前置整理

- 主题：在终端第一轮排版收口、第二轮通知与弹层能力落地之后，补一份进入市场 GUI 前的稳健精修 prompt
- 影响范围：`docs/terminal-gui-phase3-polish-prompt-2026-04-02.md`、`docs/WORKLOG.md`
- 原因：当前终端已经达到可用且观感明显改善的阶段，下一步主要风险不再是功能缺失，而是主工厂继续膨胀、通知语义仍偏文案驱动、弹窗工厂和终端侧逻辑测试不足，若不先收口会放大市场 GUI 接入成本
- 结果：
  - 输出第三轮 GUI 精修 prompt，范围明确限制为主工厂最小职责拆分、通知结构化优先、Dialog 工厂市场前置升级、终端侧纯逻辑测试补齐
  - 明确本轮不做框架重写、不做过度设计、不直接开始完整市场 GUI
  - 保留客户端与服务端实装、启动并人工目检 GUI 的硬性验收要求

### 2026-04-02 - Terminal GUI 第二轮通知层与确认弹窗落地

- 主题：把终端第二轮 prompt 中的通知层、确认弹窗、终端外可见提示与轻量视觉组件正式接入当前银行终端
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/terminal-plan.md`
- 原因：第一轮已经把布局与文本结构收口，但终端仍缺少可复用通知壳层、真实确认交互和终端外提示路径，导致 GUI 还停留在“静态信息板”阶段
- 结果：
  - 新增统一 `TerminalNotification` 模型与 HUD 通知管理器，银行反馈可同时驱动页内通知与终端外提示
  - 客户端 bootstrap 新增终端 HUD overlay 注册，关闭终端后仍可看到银行结果提示
  - 玩家转账改为先经过确认弹窗，再通过 synced action 提交到服务端，不改既有银行业务语义
  - 银行首页新增物品图标状态卡，并显式给按钮接入点击音反馈

### 2026-04-02 - Terminal GUI 第二轮 prompt：通知层、弹层与轻量视觉组件

- 主题：基于第一轮结构收口后的实际游戏内目检结果，沉淀终端 GUI 第二轮 prompt，推进通知层、弹层交互和基础视觉组件能力
- 影响范围：`docs/terminal-gui-phase2-prompt-2026-04-02.md`
- 原因：第一轮已经证明终端文本与排版明显改善，下一步不应继续停留在静态内容卡片，而要开始补齐通知、确认弹窗、终端外可见提示与轻量视觉能力
- 结果：
  - 输出第二轮 GUI prompt，范围收敛为通知层、二级弹层、最小视觉组件与基础声音反馈
  - 将“编译产物必须实装到客户端和服务端并启动，由人工肉眼验证 GUI 效果”写为硬性验收要求
  - 明确第二轮不直接跳去做完整市场 GUI，而是先沉淀可复用交互壳层

### 2026-04-02 - 沉淀本地实装流程到 GalaxyMod agent

- 主题：把本轮 JsirGalaxyBase 本地编译、dedicated server 拉起、client 联调与人工目检流程写入工作区 agent，供后续复用
- 影响范围：`../../.github/agents/GalaxyMod.agent.md`、`docs/WORKLOG.md`
- 原因：本轮已经验证出一套可重复的本地实装顺序，也踩清了 duplicate mod、后台 cwd 丢失、非阻塞 Forge 噪声与市场表缺失这几个关键坑点，需要沉淀为下次可直接复用的操作规范
- 结果：
  - `GalaxyMod` 不再是占位模板，而是明确面向 GTNH / JsirGalaxyBase 开发与实装的自定义代理
  - 写入了标准本地实装流程、推荐启动命令、运行态检查点与常见坑点
  - 后续再做 client/server 实装时，可以直接按 agent 中的顺序执行，不必重新试错

### 2026-04-02 - Terminal GUI 第一轮结构收口

- 主题：对终端首页与银行子页做第一轮结构化 GUI 收口，减少固定高度卡片与长文本爆框风险
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：当前终端虽然已基于 ModularUI2，但正文区仍然大量依赖固定高度卡片和裸 `TextWidget`，不利于长文本、动态状态和后续市场页/通知层继续生长
- 结果：
  - 新增统一文本 helper，把说明文本、动态摘要和滚动摘要从零散裸 `TextWidget` 收口到统一策略
  - `createSectionShell(...)` 现在默认支持内容自适应，仅在少数视觉统一块保留固定高度
  - 页头 lead、toast 正文、bullet panel、data row 与银行说明块改为更偏内容优先的布局
  - 不改终端入口、页路由、银行同步模型与现有银行业务语义

### 2026-04-02 - Terminal GUI 第一轮重构方案与 ModularUI2 API 评估

- 主题：沉淀终端 GUI 的第一轮结构化重构 prompt，并把 ModularUI2 API 能力评估写入文档供后续实现参考
- 影响范围：`docs/terminal-modularui2-gui-refactor-prompt-2026-04-02.md`
- 原因：当前终端页面虽然已切到 ModularUI2，但仍存在长文本溢出、固定高度卡片过多、结构不利于后续市场 GUI 和通知层扩展的问题，需要先形成基于真实框架能力的重构方案
- 结果：
  - 确认当前 GUI 主体基于 ModularUI2 流式布局，不是纯绝对坐标系统
  - 确认框架已具备富文本、滚动文本、贴图、物品展示、对话框、子面板、tooltip 和基础点击音接口
  - 输出第一轮 GUI 重构 prompt，明确本轮先收口文本与布局，再为后续终端桌面化扩展预留结构

### 2026-04-02 - 第二层旧单一商品撮合方案命令层尾修

- 主题：补齐旧单一标准商品撮合方案命令层 cancel/claim 测试覆盖，并修正买单撤销后的释放金额回显
- 影响范围：`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketService.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketServiceTest.java`
- 原因：第二层运行时接线已完成，本轮只剩命令层覆盖和玩家回显这类尾修项，避免把已释放冻结金误报成 `0`
- 结果：
  - 新增卖单撤销、买单撤销、claim 命令分发测试
  - 买单撤销回显改为使用 service 返回的真实 `releasedFunds`
  - 不改运行时装配、不扩阶段边界，只收口命令层可测性和玩家提示准确性

### 2026-04-02 - 第二层旧单一标准商品撮合方案接入服务器运行时

- 主题：把已完成的旧单一标准商品撮合服务层能力正式接入 dedicated-server 运行时、玩家命令入口与人工恢复触发
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/`、`src/test/java/com/jsirgalaxybase/modules/core/`、`src/test/java/com/jsirgalaxybase/command/`、`../../Docs/市场经济推进.md`
- 原因：上一轮虽然已经补齐第二层旧单一商品撮合方案的服务、JDBC 和恢复闭环，但服务器运行时还没有真正挂载市场服务，也没有玩家入口和管理员恢复触发，离“可实际使用”还差最后一段接线
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/技术边界文档.md`
- 结果：
  - `InstitutionCoreModule` 现在会在 dedicated-server 路径下同时装配 banking 与 shared JDBC market runtime
  - `GalaxyBaseCommand` 新增第二层现货命令入口，保留原有 phase-1 `quote/exchange` 路径不混用
  - 卖单创建入口现在会先扣除玩家手持标准化物，再调用市场服务，失败时原样回滚，避免虚空卖单
  - 新增管理员 `market recover` 手动恢复触发与启动时轻量恢复扫描挂点
  - 补充运行时装配测试与命令分发测试，覆盖 dedicated-server 装配、卖单扣物/失败回滚、买单分发、claim 列表与恢复入口

### 2026-04-01 - 旧单一标准商品撮合方案补齐买单恢复与 CLAIMABLE 提取闭环

- 主题：补齐旧单一标准商品撮合方案的买单冻结资金失败恢复闭环与 `CLAIMABLE` 资产提取写路径
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/modules/core/market/`、`docs/market-postgresql-ddl.sql`、`../../Docs/市场经济推进.md`
- 原因：上一轮只完成了最小买卖闭环，买方冻结金异常恢复与玩家真正提取 `CLAIMABLE` 资产仍未收口，存在一致性和可用性缺口
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/技术边界文档.md`
- 结果：
  - 新增 `recoveryMetadataKey`，把买单冻结金恢复与 claim 恢复线索显式落到操作日志
  - `MarketRecoveryService` 可释放未完成买单剩余冻结资金，并把订单状态收口到 `CANCELLED / FILLED`
  - 新增 `ClaimMarketAssetCommand`、`CLAIMING / CLAIMED` 状态与真实 claim delivery port
  - claim 成功后托管资产进入 `CLAIMED`，安全失败则恢复到 `CLAIMABLE`
  - 补齐市场单测与 PostgreSQL 集成测试，覆盖买单恢复和 claim 写路径

### 2026-03-29 - 初始化仓库与工作记录机制

- 主题：初始化本地 git 仓库与 work log 机制
- 影响范围：仓库根目录
- 原因：为后续 GitHub 上传、版本管理和持续开发记录做准备
- 结果：
  - 在 `JsirGalaxyBase` 仓库下初始化了本地 git 仓库
  - 建立本 work log 作为统一开发记录入口
  - 补录初次对话形成的架构和制度上下文
  - 后续每次代码更改都应在此追加简要记录

### 2026-03-29 - 排除外层仓库中的 Reference 目录

- 主题：将 `Reference/` 排除出外层 git 仓库
- 影响范围：`.gitignore`
- 原因：`Reference/` 下包含多个独立 git 仓库，直接加入外层仓库会形成嵌套仓库或 gitlink，上传到 GitHub 后不适合作为当前项目源码的一部分
- 结果：
  - 外层仓库不再跟踪 `Reference/`
  - `Reference/` 继续保留在本地，作为开发参考源码使用

### 2026-03-29 - 合并根文档并停用自动化 workflow

- 主题：重写根 README，建立 `docs/` 目录，并停用 GitHub Actions workflow
- 影响范围：`README.md`、`docs/`、`.github/workflows/`
- 原因：不再需要把架构拆成单独根文档，同时希望把面向玩家和协作者的基础材料集中到 `docs/` 目录中，并关闭当前自动化编译流程
- 结果：
  - 根 `README.md` 合并了原先独立架构文档的核心内容
  - `WORKLOG.md` 迁移到 `docs/WORKLOG.md`
  - 新增 `docs/README.md` 作为文档入口
  - 删除现有自动化编译与 release workflow 文件

### 2026-03-29 - 将项目正式更名为 JsirGalaxyBase

- 主题：将项目名称从 `CustomMod` 统一更名为 `JsirGalaxyBase`
- 影响范围：Java 包名、主类名、命令类名、Gradle 模组元数据、配置路径、README 与 docs 文档
- 原因：`CustomMod` 过于临时和泛化，无法准确承载当前制度核心加能力模块的长期定位；`JsirGalaxyBase` 更适合作为正式项目名
- 结果：
  - 根包切换为项目正式包名
  - 主类切换为 `GalaxyBase`
  - 命令切换为 `/jsirgalaxybase`
  - 模组 `modid` 切换为 `jsirgalaxybase`
  - 文档标题与项目引用同步更新

### 2026-03-29 - 排除本机环境名并建立正式命名约定

- 主题：从代码与元数据中移除本机环境名痕迹，并建立正式命名约定
- 影响范围：Java 根包、Gradle 元数据、mcmod 作者字段、README、工作目录命名说明
- 原因：本机环境名不应进入模组正式命名空间；需要把工程名、模组名、包名和仓库名分开定义清楚
- 结果：
  - Java 根包统一为 `com.jsirgalaxybase`
  - Gradle `modGroup` 与生成的 `Tags` 类路径同步改为 `com.jsirgalaxybase`
  - `mcmod.info` 作者显示改为 `Jsir2022`
  - README 新增命名约定章节，明确仓库名、目录名、模组名、`modid` 和包名的分工

### 2026-03-29 - 工作目录与 GitHub 仓库名对齐

- 主题：将本地工作目录从 `CustomMod` 改为 `JsirGalaxyBase`
- 影响范围：本地仓库目录路径
- 原因：保持本地工作目录与 GitHub 仓库名一致，减少工程名、目录名和远端仓库名之间的混淆
- 结果：
  - 本地仓库目录已改为 `JsirGalaxyBase`
  - 当前仓库名、工作目录名和 GitHub 仓库名保持一致
  - 模组运行时名称继续保持为 `JsirGalaxyBase`

### 2026-03-29 - 统一重命名为 JsirGalaxyBase / GalaxyBase

- 主题：将仓库、模组和文档名称统一切换为 `JsirGalaxyBase`，并把代码主类简写为 `GalaxyBase`
- 影响范围：`README.md`、`docs/`、Gradle 模组元数据、Java 根包、Forge 主类、命令类与本地目录命名
- 原因：用户要求统一正式名称，减少旧命名和运行时代号混杂；同时保留代码类名的可读性
- 结果：
  - 模组展示名与 `modid` 改为 `JsirGalaxyBase` / `jsirgalaxybase`
  - Java 根包改为 `com.jsirgalaxybase`
  - Forge 主类改为 `GalaxyBase`
  - 根命令改为 `/jsirgalaxybase`
  - README 与 docs 的命名约定同步更新
  - 本地工作目录已切换为 `JsirGalaxyBase`
  - GitHub 新仓库地址 `git@github.com:Jsir2022/JsirGalaxyBase.git` 当前尚不存在，远端切换需等待 GitHub 侧先完成仓库重命名

### 2026-03-29 - 确认终端入口与终端壳实施方案

- 主题：确定终端第一阶段采用快捷键入口加背包按钮入口，并先落稳定打开链与占位终端壳
- 影响范围：`docs/terminal-plan.md`、`README.md`、`docs/README.md`
- 原因：终端将承担职业、市场、福利和公共服务的统一入口，必须先把入口链、服务端鉴权与可替换 UI 壳分离清楚
- 结果：
  - 确认终端第一阶段先实现快捷键打开与背包按钮打开
  - 确认两条入口共用同一条服务端打开链
  - 确认 Pad 物品入口延后到后续阶段
  - 确认当前先使用占位终端壳，后续再替换为 `ModularUI 2`

### 2026-03-29 - 终端首页切换到 ModularUI 2

- 主题：移除旧占位 GUI，改为真实的 `ModularUI 2` 终端首页壳
- 影响范围：`dependencies.gradle`、`src/main/java/com/jsirgalaxybase/modules/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/ui/`
- 原因：当前入口链已经稳定，下一步需要把终端正式切到 `ModularUI 2`，为职业、贡献度、声望、公共任务和市场摘要首页建立可扩展的同步面板
- 结果：
  - 新增 `ModularUI2` 依赖并注册终端 UI 工厂
  - 服务端打开链改为 `GuiManager.open(...)`，继续保持服务端权威
  - 删除旧 `IGuiHandler` 和占位容器 / 占位界面实现
  - 新增终端首页快照与只读首页面板，接入职业、贡献度、声望、公共任务、市场摘要五项展示

### 2026-03-29 - 终端补左侧导航与正式分页壳

- 主题：把终端从单页总览扩成左侧导航加右侧内容区的正式框架
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/`、`docs/WORKLOG.md`
- 原因：后续职业、公共事务、市场等页面需要统一壳，不能继续停留在单页演示态
- 结果：
  - 终端改为左侧导航与右侧分页内容结构
  - 首页继续保留制度摘要，同时新增职业、公共、市场三页的只读占位内容
  - 分页状态已接入 `IntSyncValue`，后续可在同一终端壳内继续扩页而不改入口链

### 2026-03-29 - 终端改为宽屏控制台风格

- 主题：参考 AE2 终端比例，重做终端观感与窗口尺寸

### 2026-03-30 - 整理 Ubuntu 24 向日葵安装复用资料

- 主题：补充 Ubuntu 24 下向日葵 15.2.0.63064 的安装记录、复用脚本与依赖包整理目录
- 影响范围：`../../Docs/sunlogin-ubuntu24/`、`docs/WORKLOG.md`
- 原因：本次实际排查出 Ubuntu 24 官方仓库缺失旧版 `libgconf-2-4` 依赖，且 Wayland 会导致向日葵被控黑屏，需要把安装包、步骤和 Xorg 配置整理成可复用资料，便于后续其他机器快速落地
- 结果：
  - 新增 `Docs/sunlogin-ubuntu24/README.md` 记录完整安装与黑屏修复流程
  - 新增 `Docs/sunlogin-ubuntu24/install_sunlogin_ubuntu24.sh` 作为复用脚本
  - 将新版安装包与所需旧依赖 `.deb` 统一整理到 `Docs/sunlogin-ubuntu24/packages/`
  - 记录 GDM 关闭 Wayland、改走 Xorg 的关键配置点
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：上一版窗口过小、信息层级过弱、默认按钮味太重，不适合作为长期制度终端壳
- 结果：
  - 终端窗口显著放大，改成更接近控制台的宽屏比例
  - 新增顶部状态带、侧栏导航、首页摘要卡片和分区内容卡
  - 视觉风格从默认灰底面板改为更偏控制台的深色块面布局

### 2026-03-29 - 终端切到居中加分辨率自适应布局

- 主题：让终端窗口和主内容区按屏幕比例自动缩放并保持居中
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：终端不能只针对当前开发分辨率，必须在不同屏幕尺寸下保持可读和稳定布局
- 结果：
  - 外层面板改为相对屏幕宽高的居中布局
  - 内层主容器、导航列和内容列切到相对宽高分配
  - 页面页脚文案同步更新为自适应布局状态

### 2026-03-29 - 终端窗口比例提升到接近全屏

- 主题：把终端窗口提高到接近屏幕 90% 的使用面积
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：当前终端已经可用，但仍然偏保守，用户希望更接近全屏控制台体验
- 结果：
  - 终端外层窗口改为宽高各占屏幕约 90%
  - 保留居中和相对布局逻辑，不回退到固定像素窗口

### 2026-03-29 - 终端切换到深蓝灰控制台主题

- 主题：参考现代控制面板网页的配色和细边线分区，重做终端首页观感
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：原版 Minecraft 容器风格不适合制度终端，上一版纯色卡片也不够像真正的控制台页面
- 结果：
  - 终端整体配色改为深蓝灰主背景加蓝色高亮
  - 页面结构改成细边线包裹的分区面板，而不是大块卡片堆叠
  - 首页新增路由矩阵、联机概览、公共队列和市场监控四类控制台区块

### 2026-03-29 - 终端压缩左栏并关闭调试噪声

- 主题：缩小左侧导航、把标题并入导航头，并清理开发环境中无意义的启动告警
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`、`run/client/config/`
- 原因：当前样式已定稿，但左栏和顶部标题带占用过多空间，右侧内容区过挤，同时 `ModularUI 2` 调试模式和 Forge 版本检查噪声影响最终体验
- 结果：
  - 标题并入左侧导航头，顶部独立标题带移除
  - 左侧导航列缩窄，右侧内容区获得更多宽高空间
  - 首页和详情页区块高度、表头和文案密度下调，减少挤压与遮挡
  - 关闭 `ModularUI 2` 的测试 GUI 与调试描边，补齐 `NEI` 缺失的 `untranslator.cfg`
  - 关闭开发客户端中的 Forge 版本检查，减少启动时无关异常输出
  - 为开发环境补齐 `minecraft` 与 `gtnhlib` 缺失的 `items` 纹理别名，消除启动时的缺失贴图告警
  - 补齐 `IGuiHolder#createScreen(...)` 并回调若干紧凑行高，消除 `ModularUI 2` 的屏幕创建与纵向 padding 警告
  - 参考 AE2 终端的上下分区布局，把左侧导航改成顶部固定、底部固定、中间弹性伸缩的自适应结构，避免窗口高度变化时溢出边框
  - 重新压缩右侧内容区的长文本和中部比例，避免中文长句在窄栏内被挤成纵向显示
  - 进一步收窄左侧导航为固定窄比例，并移除矩阵左侧辅助说明列，把联机概览缩成纯图例卡，解决中部两块的剩余挤压
  - 把首页高度分配改为固定头部、弹性中段、固定底部，并缩减矩阵行数和右侧概览内容，保证内容不再越出终端下边

### 2026-03-29 - 修正 WorldEdit 服务端分发形态

- 主题：修正 GTNH 服务端侧的 WorldEdit 分发，补齐 CUIFe，并保留 dist 包替代旧 core 的方案
- 影响范围：`GroupServer/packwiz/sync_root/mods/`、`GroupServer/GTNHServerConfig/server_mods/mods/`、`GroupServer/Galaxy_GTNH284_S1/mods/`、`GroupServer/Galaxy_GTNH284_S2/mods/`、`GroupServer/Galaxy_GTNH_Lobby/mods/`
- 原因：复核后确认 `worldedit-forge-mc1.7.10-6.1.1-dist.jar` 已内置 core 类，不应再额外叠加旧 `worldedit-core`；但 `WorldEditCuiFe` 需要同步补到服务端侧，之前这一部分漏放了
- 结果：
  - 保持 `worldedit-forge-mc1.7.10-6.1.1-dist.jar` 作为 WorldEdit 服务端实现
  - 将 `WorldEditCuiFe-v1.0.7` 补入 `GTNHServerConfig` 服务端私货目录和 S1/S2/Lobby live 实例
  - 未执行 live 服重启，当前仍为落盘待生效状态

### 2026-03-29 - 调整 GTNH 默认同步包的跨维传送与黑暗模组

- 主题：在 `packwiz` 同步源中放开 ServerUtilities 跨维传送，并移除 `Darkerer`
- 影响范围：`GroupServer/packwiz/sync_root/serverutilities/server/ranks.txt`、`GroupServer/packwiz/sync_root/mods/`、`GroupServer/packwiz/sync_root/config/`
- 原因：改善玩家跨维 `/home` 和 `/warp` 体验，并移除 GTNH 包内的真实黑暗实现，后续由 `packwiz` 同步到客户端与服务端
- 结果：
  - 为 `player`、`vip`、`admin` 全部开启 `serverutilities.homes.cross_dim` 与 `serverutilities.warps.cross_dim`
  - 从默认同步源删除 `darkerer-1.0.6.jar`
  - 从默认同步源删除 `config/darkerer.cfg`

### 2026-03-29 - 补齐私货 WorldEdit 分发与 live 服跨维传送配置

- 主题：整理根目录私货 jar，准备替换 WorldEdit 为 dist 包，并把 live 实例的 ServerUtilities 跨维权限直接改到位
- 影响范围：`GroupServer/packwiz/sync_root/mods/`、`GroupServer/GTNHServerConfig/`、`GroupServer/Galaxy_GTNH284_S1/`、`GroupServer/Galaxy_GTNH284_S2/`、`GroupServer/Galaxy_GTNH_Lobby/`
- 原因：在不重启 live 服的前提下，先把创世神相关模组文件、WorldEdit 配置镜像和 `/home` `/warp` 跨维权限准备完成
- 结果：
  - 计划用私货 `worldedit-forge-mc1.7.10-6.1.1-dist.jar` 替换旧 `worldedit-core + worldedit-forge` 组合
  - GTNHServerConfig 补齐 `config/worldedit/worldedit.properties` 镜像
  - GTNHServerConfig 与 S1/S2/Lobby 的 `serverutilities/server/ranks.txt` 全部开启跨维 `home/warp`

### 2026-03-30 - 明确群组服一期后端与同步边界

- 主题：把一期后端架构、同步范围和免费传送规则正式写入文档
- 影响范围：`README.md`、`../Docs/群组服.md`、`../Docs/技术边界文档.md`、`docs/WORKLOG.md`
- 原因：当前已明确一期不接 `Redis`，并且需要先把银行系统、主背包同步和免费跨服传送的边界定死，避免后续设计反复摇摆
- 结果：
  - 明确一期唯一中心化存储为 `PostgreSQL`
  - 明确一期采用模组服务端直连 `PostgreSQL`，不单独建设中心后端服务
  - 明确一期共享范围包括制度数据、主物品栏、护甲、经验、血量、饥饿
  - 明确“共享背包”当前等同于玩家按 `E` 打开的主背包数据
  - 明确跨服传送与 `home` 当前为免费规则
  - 明确一期实施顺序为：先银行系统，再落库与同步，再传送

### 2026-03-30 - 新增银行系统一期需求文档

- 主题：把银行系统一期能力、边界和非目标整理成正式需求文档
- 影响范围：`docs/banking-system-requirements.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：当前已经明确银行系统是一切制度与跨服能力的前置底座，需要先把需求边界固定，再进入具体表结构设计
- 结果：
  - 新增银行系统一期需求文档，明确初始玩家余额为 `0`
  - 明确玩家账户、税池、兑换所储备、后续公会资金都必须作为独立账户存在，不能退化为简单变量
  - 明确一期只做固定规则兑换结算，不做单独硬币交易市场与汇率系统
  - 明确一期不做任何扩展金融能力，如利息、贷款、定存等
  - 将银行系统需求文档加入仓库文档入口

### 2026-03-30 - 新增银行系统数据表与事务边界设计

- 主题：基于银行需求文档，落一期数据库表设计与事务边界草案
- 影响范围：`docs/banking-schema-design.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：银行系统需求边界已经固定，需要尽快把账户表、交易表、账本分录表和关键事务流程正式定稿，避免后续编码时重新发明模型
- 结果：
  - 新增银行系统数据表与事务边界设计文档
  - 明确一期核心表为 `bank_account`、`bank_transaction`、`ledger_entry` 与 `coin_exchange_record`
  - 明确所有资金变动必须在同一事务中完成锁定、校验、分录和余额更新
  - 明确幂等键、行级锁、固定加锁顺序和禁止修改历史账本的约束
  - 将数据表设计文档加入仓库文档入口

### 2026-03-30 - 新增银行 Java 领域模型与仓储接口草案

- 主题：把银行需求与表设计翻译成 Java 侧代码骨架
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/`、`docs/banking-java-domain-draft.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：仅有表设计还不够，必须尽快把领域模型、仓储接口和事务边界接口固定下来，避免后续业务实现直接散落 SQL 和字符串常量
- 结果：
  - 新增银行领域对象、关键枚举和仓储接口草案
  - 新增 `BankingTransactionRunner` 事务边界接口，用于表达银行写操作必须运行在同一事务中
  - 新增 Java 侧设计说明文档，建立文档设计与代码骨架之间的映射关系
  - 将 Java 侧设计文档加入仓库文档入口

### 2026-03-30 - 新增银行 PostgreSQL DDL 草案

- 主题：把一期银行表设计正式落成 PostgreSQL SQL 文件
- 影响范围：`docs/banking-postgresql-ddl.sql`、`docs/banking-schema-design.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：仅有 Markdown 表设计还不够，后续开始写 JDBC 仓储或迁移脚本前，需要一份可以直接对照执行和继续演进的 DDL 草案
- 结果：
  - 新增一期银行核心表的 PostgreSQL DDL 草案文件
  - 固定 `bank_account`、`bank_transaction`、`ledger_entry`、`coin_exchange_record` 与可选 `bank_daily_snapshot` 的字段、约束和索引
  - 明确账户 `updated_at` 触发器策略以及金额非负、幂等键唯一、账本顺序唯一等数据库侧约束
  - 将 SQL 草案加入设计文档与仓库文档入口

### 2026-03-30 - 开始落银行应用服务实现

- 主题：把银行一期业务动作从纯文档和接口草图推进到 application 层代码
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/application/`、`src/main/java/com/jsirgalaxybase/modules/core/banking/domain/CoinExchangeRecord.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/repository/LedgerEntryRepository.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：DDL 和领域模型已经齐备，下一步必须先把一期真正的业务动作编排固定下来，后续 JDBC 仓储才能按稳定签名实现
- 结果：
  - 新增 `BankingApplicationService`，统一承接开户、查询、玩家转账、内部划转和硬币兑换结算
  - 新增对应命令对象与 `BankPostingResult`，收束一期业务入参与出参
  - 补上基于 `request_id` 的幂等回放能力所需仓储查询签名
  - 将 `CoinExchangeRecord` 字段补齐到与 PostgreSQL DDL 更一致的状态

### 2026-03-30 - 补齐银行 JDBC 基础设施层

- 主题：把银行 application 层继续落到可对接 PostgreSQL 的 JDBC 边界
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：只有 application service 还不够，必须同步提供事务执行器与仓储实现骨架，后续才能真正接数据库和跑集成验证
- 结果：
  - 新增 JDBC 连接管理器与事务执行器，支持线程内事务连接复用
  - 新增账户、交易、账本分录、兑换记录的 JDBC 仓储实现
  - 将 `SELECT ... FOR UPDATE`、幂等查询、批量追加分录和账户余额更新明确落到代码层
  - 编译验证通过，当前只差 PostgreSQL 连接配置与模块装配

### 2026-03-30 - 接入银行配置项与模块初始化挂载点

- 主题：把银行 JDBC 基础设施从“仅可编译类库”推进到模块生命周期中的可装配状态
- 影响范围：`src/main/java/com/jsirgalaxybase/config/ModConfiguration.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：如果不把配置项和初始化入口接上，后续命令或 GUI 层仍然拿不到银行服务实例
- 结果：
  - 新增 PostgreSQL 银行连接配置项与 `source_server_id` 配置项
  - 新增 `BankingInfrastructure` 聚合对象与基于 `DriverManager` 的 `DataSource` 工厂
  - `InstitutionCoreModule` 已可在服务端按配置准备银行基础设施实例
  - 编译验证通过，当前剩余工作聚焦于 PostgreSQL 驱动依赖、真实连通验证和上层入口接线

### 2026-03-30 - 补充 PostgreSQL 本地安装与迁移说明

- 主题：补上 Ubuntu 宿主机 PostgreSQL 安装、初始化与换机迁移指导
- 影响范围：`docs/postgresql-local-setup-and-migration.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：当前机器没有安装 PostgreSQL，且当前会话没有无密码 sudo，无法直接代装到宿主机；需要把安装与迁移流程沉淀为可执行说明
- 结果：
  - 新增 Ubuntu 24.04 下 PostgreSQL 安装与建库说明
  - 补充基于当前 DDL 的初始化命令
  - 明确换机迁移推荐走逻辑备份而不是直接拷贝数据目录
  - 补充最小备份命令，降低后续换主机时的数据丢失风险

### 2026-03-30 - 完成本机 PostgreSQL 安装与模组真实连通验证

- 主题：把 PostgreSQL 从文档方案推进到宿主机实际运行与模组服务端启动验证
- 影响范围：`dependencies.gradle`、`repositories.gradle`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankingInfrastructureFactory.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`docs/banking-java-domain-draft.md`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：银行 JDBC 实现已经存在，但如果没有真实数据库、真实驱动和真实服务端启动验证，就还不能说明这条链路可用
- 结果：
  - 在 Ubuntu 24.04 宿主机安装并启动 PostgreSQL 16
  - 创建本地开发账号 `jsirgalaxybase_app` 与数据库 `jsirgalaxybase`
  - 将一期银行 DDL 实际执行到本地数据库，确认核心表全部存在
  - 新增 PostgreSQL JDBC 驱动依赖与 Maven Central 仓库声明
  - 启动 `runServer` 完成模组服务端真实联调，日志明确显示银行 PostgreSQL 基础设施已准备并验证成功

### 2026-03-30 - 收紧本地数据库监听并接入银行管理员测试命令

- 主题：把本地 PostgreSQL 显式限制在回环地址，并提供游戏内银行管理测试入口
- 影响范围：`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/module/ModuleManager.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：当前数据库不应暴露外网监听，同时银行系统已经具备基础设施，需要第一个实际管理员入口来驱动开户、查余额、发钱和转账测试
- 结果：
  - PostgreSQL 已显式配置为只监听 `127.0.0.1:5432`
  - 本地开发业务账号密码已切换为用户指定的新密码
  - 在 `/jsirgalaxybase bank` 下新增 `open`、`balance`、`grant`、`transfer` 四个管理员测试子命令
  - 通过实际 `runServer` 自动控制台执行验证了 bank 命令帮助输出与命令注册链路

### 2026-03-31 - 放开 NEI 客户端主配置的本地持久化

- 主题：让 GTNH 客户端的 `NEI` 主配置不再被 `packwiz` 更新与 `Default Configs` 启动流程反复覆盖
- 影响范围：`GroupServer/packwiz/sync_root/packwiz-whitelist.json`、`GroupServer/packwiz/sync_root/config/localconfig.txt`、`GroupServer/packwiz/whitelist-localconfig-notes.md`、`docs/WORKLOG.md`
- 原因：玩家反馈 `NEI` 配置经常被自动替换；排查确认活跃文件 `config/NEI/client.cfg` 未进入白名单，同时 `localconfig.txt` 还在接管整份 `NEI/client.cfg`
- 结果：
  - 将 `config/NEI/client.cfg` 加入 `packwiz` 白名单
  - 保留 `config/NEI/client.cfg.bak` 白名单不变
  - 注释掉 `localconfig.txt` 中对 `[NEI/client.cfg]/*/*` 的接管规则
  - 在既有白名单说明文档中补记 `NEI` 案例，方便后续处理同类客户端配置问题

### 2026-03-30 - 扩展银行管理员命令到系统账户与最近流水查询

- 主题：把第一版银行测试命令从单纯改余额扩展到状态查询与账本查看
- 影响范围：`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：仅有开户、查余额、发钱、转账还不够，管理员需要直接查看最近流水和系统测试账户状态，才能形成最小可用的联调闭环
- 结果：
  - 在 `/jsirgalaxybase bank` 下新增 `ledger`、`system` 和 `system ledger` 命令
  - recent ledger 输出已包含交易号、方向、金额、变动前后余额和时间戳
  - system summary 会显示测试系统账户的编号、类型、状态和当前余额
  - 通过实际 `runServer` 自动执行验证了 system summary 与 system ledger 命令回显

### 2026-03-30 - 扩展银行管理员命令到公共账户、交易详情与系统账户初始化

- 主题：把银行管理命令从单账户测试扩展到公共账户与交易审计层面
- 影响范围：`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/repository/BankTransactionRepository.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankTransactionRepository.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：当前需要直接初始化系统账户集、查看公共账户状态，并能按交易号定位单笔交易详情，方便后续联调和审计
- 结果：
  - 新增 `/jsirgalaxybase bank public` 与 `/jsirgalaxybase bank public ledger` 命令用于查看受管公共/系统账户
  - 新增 `/jsirgalaxybase bank tx <transactionId>` 命令用于查询单笔交易与关联账本分录
  - 新增 `/jsirgalaxybase bank init system` 命令，用于初始化测试系统资金、系统运营账户、税池和兑换储备账户
  - 通过实际 `runServer` 自动执行验证了系统账户初始化、公共账户汇总与交易不存在时的详情查询回显

### 2026-03-30 - 收敛受管系统账户并补齐备份恢复脚本

- 主题：把系统账户模型收敛为 `ops + exchange`，并把 PostgreSQL 逻辑备份/恢复脚本正式落地
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/ManagedBankAccounts.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`scripts/db-backup.sh`、`scripts/db-restore.sh`、`docs/postgresql-backup-and-restore.md`、银行相关文档
- 原因：当前不再需要测试资金池与独立税池，系统运营收支应统一落在 `ops`；同时换机和演练必须从“会手敲命令”升级到“有固定脚本可执行”
- 结果：
  - 受管系统账户收敛为 `ops` 系统运营账户与 `exchange` 兑换储备账户
  - 玩家账户仍保持按需懒初始化，不做自动开户
  - `InstitutionCoreModule` 在服务端启动时自动确保系统账户存在
  - `/jsirgalaxybase bank system` 与 `grant` 改为围绕 `ops` 账户工作，公共账户查询只展示 `ops` 与 `exchange`
  - 新增 PostgreSQL 逻辑备份与恢复脚本，并补充使用方式、演练步骤与风险控制说明

### 2026-03-30 - 增补 systemd 定时备份方案并明确快照技术取舍

- 主题：把 PostgreSQL 备份方案从“手动脚本可用”推进到“systemd 每日自动备份可落地”
- 影响范围：`ops/systemd/`、`scripts/install-systemd-backup.sh`、`docs/postgresql-backup-and-restore.md`、`docs/README.md`
- 原因：当前实际需求已经明确为“单数据库每日一份、保留七份”，需要正式选主方案，并说明为什么不把文件系统快照当当前主链路
- 结果：
  - 新增 system 级 `systemd service + timer` 模板与环境文件样例
  - 新增安装脚本，用于把 unit 安装到 `/etc/systemd/system/` 并启用 timer
  - 明确当前主方案是 `pg_dump -Fc + systemd timer + RETAIN_COUNT=7`
  - 明确文件系统快照不是当前主方案，后续如需更细恢复点应升级到 `pg_basebackup + WAL archive`

### 2026-03-30 - 补齐 PostgreSQL 备份恢复值班手册与真实演练说明

- 主题：把备份恢复文档从“方案说明”补成“后续维护者可以直接照抄命令执行”的操作手册
- 影响范围：`docs/postgresql-backup-and-restore.md`、`docs/WORKLOG.md`
- 原因：当前备份与恢复链路已经真实安装和演练通过，但如果不把日常查看、手动备份、恢复到测试库、覆盖正式库和清理测试库等指令写清楚，后续维护者仍然会不知道怎么用
- 结果：
  - 文档补充了当前机器上的实际部署状态、备份目录与 systemd 单元名称
  - 文档补充了日常查看、立即备份、恢复到测试库、覆盖正式库和删除测试库的完整命令
  - 文档明确了 `oneshot` service 的状态表现与业务账号无建库权限这两个常见注意事项

### 2026-03-30 - 明确银行终端页的信息架构并开始接真实只读快照

- 主题：把普通玩家正式入口的银行 GUI 从“想法”落成文档，并开始按终端壳的嵌套菜单模式实现
- 影响范围：`docs/banking-terminal-gui-design.md`、`docs/README.md`、终端 GUI 与终端快照提供者
- 原因：当前终端已经是左侧导航 + 主区板块的统一壳，银行页不能再做成单独弹窗或纯目录页，而应先展示关键内容，再提供二级子页跳转
- 结果：
  - 文档固定了银行主页、个人账户、转账服务、Exchange 公开页、个人流水五页结构
  - 明确 Exchange 储备余额与最近流水属于玩家公开透明内容，而不是仅供管理员查看
  - 实施方向改为“先做真实只读快照与嵌套菜单，再继续接正式写操作”

### 2026-03-30 - 修正终端页签只换标题不换正文的 ModularUI 用法错误

- 主题：修复终端切到银行页后右侧正文不切换、只有标题变化的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：原先正文区是在 `buildUI()` 时按当时的 `selectedPage` 一次性 `switch` 构建，后续虽然标题使用了动态文本，但正文没有进入框架的启用/禁用重布局链
- 结果：
  - 改为把所有页面挂进同一个正文容器
  - 每个页面容器使用 `setEnabledIf(...)` 绑定 `selectedPage`
  - 父级 `Flow` 开启 `collapseDisabledChild(true)`，让页签变化时正文区实际切换并重新布局

### 2026-03-31 - 修复终端打开即断线并记录两类联调阻塞根因

- 主题：收敛本地专用测试服最近两类核心阻塞：进服阶段的 `Fatally Missing blocks and items`，以及打开终端后的 `Disconnected from server`
- 影响范围：`src/main/java/com/jsirgalaxybase/GalaxyBase.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalNetwork.java`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：一方面 `ModularUI2` 的 dev 运行产物把测试映射带进了 Forge 注册表握手；另一方面 Forge 1.7.10 对自定义包通道名存在 20 字符上限，原终端通道名超长后会在服务端 `C17PacketCustomPayload` 解码阶段直接踢线
- 结果：
  - 在模组入口里忽略了 `modularui2:test_block` 与 `modularui2:test_item` 这类瞬时 dev 缺失映射，进服不再被这类测试映射阻塞
  - 把终端 `SimpleNetworkWrapper` 通道名从超长值收敛到 `jgb_terminal`
  - 确认这类终端断线修复后，客户端与服务端都必须重启，否则旧客户端仍会继续发送旧通道名
  - 把两类问题的根因与处理办法补进 PostgreSQL/联调文档，后续排障可以直接按文档核对

### 2026-03-31 - 明确银行终端“未启用基础设施”其实是服务端配置未打开

- 主题：把银行终端里的“未启用 PostgreSQL 基础设施”从模糊报错改成可操作的配置诊断
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankingService.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSnapshotProvider.java`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：本地专用测试服的 `run/server/config/jsirgalaxybase-server.cfg` 当时仍是默认占位状态，`bankingPostgresEnabled=false`、JDBC 地址仍指向 `db-host`，终端只能统一回显“当前世界未启用 PostgreSQL 银行基础设施”，信息不足以指导维护者下一步该改哪里
- 结果：
  - 银行终端现在会优先区分“服务端显式关闭银行 PostgreSQL”“JDBC 地址未配置”“用户名/密码未填”“初始化失败”几类状态
  - 银行快照页会提示优先检查 `jsirgalaxybase-server.cfg`、JDBC 配置与服务端启动日志
  - 文档补充了本地 `runServer` 联调至少要打开 `bankingPostgresEnabled` 并填好 JDBC 凭据这一前置条件

### 2026-04-01 - 银行第三轮收口：补真实 JDBC 验证、内部划转语义测试与开户复用规则

- 主题：把银行模块当前最大缺口从“服务层逻辑”收敛到“真实 PostgreSQL 路径验证”和“剩余语义定稿”
- 影响范围：`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`docs/banking-java-domain-draft.md`、`docs/banking-terminal-gui-design.md`、`docs/WORKLOG.md`
- 原因：第二轮已经修完 `request_id` 幂等重放、历史余额回放与语义冲突校验，但真实 JDBC 路径验证、`postInternalTransfer` 同等级补测，以及 `openAccount` 命中已有账户时的资料处理规则仍未完全收口
- 结果：
  - 新增真实 PostgreSQL 集成测试，基于独立测试 schema 验证 `saveIfOwnerAbsent`、`saveIfRequestAbsent`、`request_id` 历史余额重放、语义冲突拒绝与事务回滚不半提交
  - 为 `postInternalTransfer` 补齐历史余额回放、`amount`、`fromAccountId`、`toAccountId`、`businessType`、`operatorType`、`operatorRef`、`sourceServerId` 冲突测试
  - 明确 `openAccount(...)` 命中已有账户时保持既有 `display_name` 与 `metadata_json` 不刷新
  - 文档明确当前终端只承担开户、只读快照和玩家转账，任务书硬币兑换真实入口延期到市场阶段

### 2026-04-01 - 银行第四阶段收口：补工厂初始化验证、deduct 管理闭环与独立测试入口

- 主题：把银行一期在“非市场阶段”剩余的初始化链路、管理命令和测试执行入口真正收住
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankingInfrastructureFactory.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`build.gradle.kts`、`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：第三轮后还剩三个明显尾巴没有收口：工厂初始化校验仍写死 `public` schema、`transactionType` 语义矩阵缺显式补测、管理员命令只有 `grant` 没有对称扣减入口，也缺独立的集成测试执行命令
- 结果：
  - `JdbcBankingInfrastructureFactory` 改为按当前 JDBC 连接的 `search_path/currentSchema` 校验必需表，独立 schema 的 PostgreSQL 集成测试终于能真实覆盖工厂初始化路径
  - 新增工厂初始化成功/缺表失败两条真实 PostgreSQL 集成测试，并补上 `transactionType` 冲突测试
  - 新增 `./gradlew bankingIt` 与 `./gradlew banking-it` 两个银行集成测试入口，便于单独跑 PostgreSQL 路径
  - `/jsirgalaxybase bank` 管理命令补上 `deduct <player> <amount> [comment]`，与既有 `grant` 形成对称的管理员修正闭环
  - 明确幂等重放返回的是“历史 availableBalance + 当前非余额字段”的结果对象，而不是完整历史账户快照

### 2026-04-01 - 市场阶段前置分析：收口市场与银行的职责边界

- 主题：在进入市场实施前，把制度文档中的市场需求与现有银行能力做一次正式对齐，避免后续把市场和银行混成一层
- 影响范围：`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
- 原因：银行一期已经完成非市场阶段收口，但市场真正开做之前，必须先明确哪些能力可以直接复用银行，哪些最小能力仍需银行补齐，以及哪些责任必须留在市场模块
- 结果：
  - 在 `../Docs/市场经济推进.md` 中新增“市场阶段与银行系统边界结论”章节
  - 明确市场可直接复用现有银行账户、账本、兑换结算与系统划转能力
  - 明确银行仍需补齐的最小能力是：`冻结资金/解冻资金`、`税池账户`、`市场结算业务类型`

### 2026-04-01 - 市场阶段第一轮：接入真实任务书硬币兑换入口并补银行最小缺口

- 主题：按“真实兑换入口 + 银行最小缺口 + 市场骨架”收下市场阶段第一轮
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/`、`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangePlannerTest.java`、`docs/banking-java-domain-draft.md`、`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
- 原因：制度上已经明确市场阶段不应重做第二套货币底层，但现有代码还缺玩家可调用的真实兑换入口，以及挂单市场前必需的冻结余额、税池账户和市场结算语义边界
- 结果：
  - 新增 `/jsirgalaxybase market quote hand` 与 `/jsirgalaxybase market exchange hand`，先落地“手持一叠任务书硬币”的真实兑换入口
  - 银行应用服务补齐 `冻结 / 解冻 / 从冻结余额结算` 三个最小动作，并新增市场相关交易类型、业务类型与 `tax` 受管公共账户
  - 新增 `modules/core/market/` 首轮骨架，把任务书硬币识别、兑换规则和银行结算桥接从 banking 中拆到 market 侧
  - 当前实现明确为 `source-blind`：仅按 Dreamcraft coin 物品注册名识别，不在本轮尝试从物品本体反推“一次性任务 / 循环悬赏”来源
  - 补了银行冻结生命周期单测与市场任务书硬币规则单测，完整挂单订单簿、托管库存、撮合、CLAIMABLE/EXCEPTION 与崩服恢复仍留待下一轮
  - 明确订单簿、托管库存、撮合、内部操作日志、异常恢复属于市场模块自身，不应继续塞回银行模块

### 2026-04-01 - 市场阶段第二阶段：收口冻结回放语义并切入旧单一标准商品撮合方案最小骨架

- 主题：先把第一层金融底座补到可承载市场，再谨慎切入旧单一标准商品撮合方案
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/`、`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/`、`docs/banking-postgresql-ddl.sql`、`docs/market-postgresql-ddl.sql`、`docs/banking-java-domain-draft.md`、`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
- 原因：第一轮已经补出冻结/解冻/从冻结结算动作，但 replay 语义仍然把 `available` 和 `frozen` 混成“半历史半当前”；同时第二层旧单一商品撮合方案还没有真正的订单 / 托管 / 操作日志结构，仍停在空接口阶段
- 结果：
  - `TaskCoinExchangePlanner` 补齐 `IV = 10000`，并避免把未知更高罗马后缀误判成 `I` 或 `BASE`
  - `ledger_entry` 扩展为同时保存 `available` 与 `frozen` 的 before/after，`freeze/release/settleFrozenTransfer` 的 replay 现在能一致返回历史余额态
  - 补齐银行服务层 replay 单测，以及 PostgreSQL 下的 freeze/release/settle 成功、重放、冲突与回滚回归
  - 新增标准商品、市场订单、托管库存、内部操作日志、成交记录等领域对象与仓储接口，不再只是空接口占位
  - 新增旧单一商品撮合方案的最小应用服务，先支持 `创建卖单`、`撤销卖单` 与 `查看 OPEN 卖单` 读模型骨架，且明确卖单托管与 CLAIMABLE 返还路径
  - 当前仍未进入买单冻结闭环、真实撮合、税收结算、GUI、跨服消息和第三层定制化交易

  ### 2026-04-01 - 市场阶段第三阶段：补请求语义与恢复闭环并打通旧单一路线的最小买卖撮合

  - 主题：先修第二阶段一致性缺口，再把旧单一标准商品撮合方案推进到最小可运行买卖闭环
  - 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/`、`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/infrastructure/jdbc/MarketPostgresIntegrationTest.java`、`build.gradle.kts`、`docs/market-postgresql-ddl.sql`、`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
  - 原因：第二阶段虽然已经有卖单骨架，但 `requestId` 还没有完整请求语义校验，失败路径也缺少最小恢复闭环，同时第三阶段要求的买单冻结、同步撮合、CLAIMABLE 生成和真实 JDBC 市场持久化仍未接通
  - 结果：
    - 旧单一商品撮合服务补齐 `BUY_ORDER_CREATE / BUY_ORDER_CANCEL / MATCH_EXECUTION`，并把买单冻结、撤单释放、同步单商品限价撮合、税池入账、CLAIMABLE 生成与成交记录写入收口到 market application service
    - `MarketOperationLog` 现在按 `requestSemanticsKey` 校验完整请求语义，重复 `requestId` 不再只校验操作类型；卖单创建与撤单失败时会保留相关 `order / custody / trade` 线索，并进入 `RECOVERY_REQUIRED` 或 `FAILED`
    - 新增 `MarketRecoveryService`，可以扫描 `CREATED / PROCESSING / FAILED / RECOVERY_REQUIRED` 并把关联订单与托管库存收口到 `EXCEPTION`
    - 补齐市场 JDBC 基础设施：真实 PostgreSQL 仓储、`JdbcMarketInfrastructureFactory`、`marketIt / market-it` 任务，以及 PostgreSQL 下的卖单创建/撤单、请求语义冲突和恢复扫描回归
    - 为了让 market JDBC 能复用 banking 的同一连接基础设施，把 `AbstractJdbcRepository` 与 `JdbcConnectionCallback` 开放为可跨包复用的公共类型，保持市场与银行共享一条事务链
    - 补了单元测试覆盖：卖单/撤单 request 语义冲突、买单冻结与撤单释放、同步撮合后的成交记录与 CLAIMABLE 资产、恢复扫描收口
    - 已实际执行并通过：`./gradlew test`、`./gradlew bankingIt`、`./gradlew marketIt`

### 2026-04-04 - 将终端正文改为单页宿主

- 主题：继续修复终端打开后聊天栏持续刷 `ModularUI` Column resize 错误的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：终端正文区此前把 home、market、bank 各页同时挂在同一棵 `Flow.column()` 下，再通过 `setEnabledIf(...)` 切换可见性；在当前复杂布局下，隐藏页仍会形成重复的 `Column` 高度求解链，导致父级布局长期不收敛
- 结果：
  - 正文区改为 `SingleChildWidget` 单页宿主，只保留当前选中页的一棵 widget 子树
  - 页签切换时按 `selectedPage` 动态替换正文，移除多页常驻叠加带来的重复 `Column` 布局链

### 2026-04-25 - 产出 Phase 7 交接文档

- 主题：产出终端 GUI 迁移 Phase 7 交接文档，供后续先进 AI 执行 MARKET_CUSTOM 与 MARKET_EXCHANGE 迁移
- 影响范围：`docs/terminal-phase7-handover-to-chatgpt5.5.md`、`docs/WORKLOG.md`
- 原因：Phase 6 已完成标准商品市场迁移与滚动/布局/数据截断修复，Phase 7 需要迁移定制商品市场与汇率市场；为确保后续 AI 能清晰理解上下文、阶段定位、技术约束与验收标准，产出本交接文档
- 引用来源：`docs/terminal-betterquesting-ui-phase7-custom-exchange-residue-prompt-2026-04-19.md`、`docs/terminal-plan.md`、`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`
- 结果：产出完整交接文档，包含项目背景、阶段定位、技术架构要点、必须阅读的文档、Phase 7 执行 prompt 核心摘要、验收标准、已知风险与坑点、交接地清单
  - 已实际执行并通过：`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && /media/u24game/gtnh/JsirGalaxyBase/gradlew -p /media/u24game/gtnh/JsirGalaxyBase assemble --no-configuration-cache -PforceToolchainVersion=17`

### 2026-04-25 - Terminal BetterQuesting UI Phase 7：迁入定制商品市场与汇率市场

- 主题：把 MARKET_CUSTOM 与 MARKET_EXCHANGE 两张剩余真实业务页迁入新 `TerminalHomeScreen` 宿主，并收干新壳对旧市场装配残留的直接依赖
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/client/component/`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/`、`src/main/java/com/jsirgalaxybase/terminal/network/`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSectionService.java`、终端相关测试、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 6 后新壳已承接 BANK 与 MARKET_STANDARDIZED，phase 7 必须补齐 listing-first 的定制商品市场和 quote-first 的汇率市场，让 phase 8 只剩正式 cutover
- 结果：
  - 新增 `TerminalCustomMarketSection` / `TerminalCustomMarketSectionModel` / `TerminalCustomMarketSectionSnapshot`，承接 active / selling / pending listing 浏览、当前 listing 详情、资产摘要、购买 / 下架 / 领取确认与最近动作反馈
  - 新增 `TerminalExchangeMarketSection` / `TerminalExchangeMarketSectionModel` / `TerminalExchangeMarketSectionSnapshot`，承接兑换标的浏览、formal quote、pair / rule / limit 状态、刷新报价、确认兑换与动作反馈
  - 新增 custom / exchange 独立 action payload，并扩展 `TerminalActionType`、`TerminalActionMessage` 主链与 `TerminalSnapshotMessage` 序列化回写
  - `TerminalPopupFactory` 生命周期下已承接 custom 购买 / 下架 / 领取确认，以及 exchange 确认兑换；确认后继续走新 action -> snapshot 主链
  - `TerminalMarketSectionService` 侧新壳装配不再直接实例化旧 market / custom / exchange session controller，也不借旧 builder、sync binder 或旧 dialog；旧 ModularUI 代码继续保留给 phase 8 cutover 前过渡
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.terminal.TerminalServiceTest --tests com.jsirgalaxybase.terminal.TerminalMarketActionMessageFactoryTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.client.gui.framework.VerticalScrollPanelTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --tests com.jsirgalaxybase.terminal.ui.TerminalMarketServiceTest --tests com.jsirgalaxybase.terminal.ui.TerminalMarketSessionControllerTest --tests com.jsirgalaxybase.terminal.ui.TerminalCustomMarketSessionControllerTest --tests com.jsirgalaxybase.terminal.ui.TerminalExchangeMarketSessionControllerTest --no-configuration-cache -PforceToolchainVersion=17`
  - `./gradlew test --no-configuration-cache -PforceToolchainVersion=17` 当前仍有既存 PostgreSQL 银行集成测试 `BankingPostgresIntegrationTest.factoryCreateRejectsMissingTablesInCurrentSchema` 断言失败；终端定向测试均已通过

### 2026-04-25 - Terminal BetterQuesting UI Phase 8：正式入口 cutover 到新终端壳

- 主题：把 G 键与背包按钮的正式终端打开链收口到 `OpenTerminalRequestMessage -> OpenTerminalApprovedMessage -> TerminalHomeScreen`
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalNetwork.java`、`src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalOpenCutoverTest.java`、`docs/README.md`、`docs/terminal-plan.md`、`docs/WORKLOG.md`
- 原因：phase 7 已经让新 `TerminalHomeScreen` 承接 BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 全部正式业务页，phase 8 只需要把正式打开入口切到新壳并把旧 ModularUI 链降级为 legacy fallback
- 结果：
  - G 键与背包按钮正式入口保持默认发送 `OpenTerminalRequestMessage`，服务端通过 `TerminalService.approveTerminalClientScreen(...)` 生成初始 snapshot 与 session token，客户端经 `OpenTerminalApprovedMessage` 打开新 `TerminalHomeScreen`
  - 旧 `OpenTerminalMessage` 明确标注为 legacy ModularUI fallback，并改为调用 `TerminalService.openLegacyTerminal(...)`
  - `TerminalService.openTerminal(...)` 保留为 deprecated 兼容别名，不再作为正式入口语义；`TerminalModule` 日志也明确旧 `TerminalHomeGuiFactory` 只是 fallback factory
  - 新增 `TerminalOpenCutoverTest`，覆盖 approval -> `TerminalHomeScreenModel` 序列化、BANK / MARKET_STANDARDIZED / MARKET_CUSTOM / MARKET_EXCHANGE 新壳模型可打开、key/button 源码装配不再引用旧包、网络注册包含 open/action/snapshot 主链且 legacy packet 明确降级
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.terminal.TerminalOpenCutoverTest --tests com.jsirgalaxybase.terminal.TerminalServiceTest --tests com.jsirgalaxybase.terminal.TerminalMarketActionMessageFactoryTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.client.gui.framework.VerticalScrollPanelTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --tests com.jsirgalaxybase.terminal.ui.TerminalMarketServiceTest --tests com.jsirgalaxybase.terminal.ui.TerminalMarketSessionControllerTest --tests com.jsirgalaxybase.terminal.ui.TerminalCustomMarketSessionControllerTest --tests com.jsirgalaxybase.terminal.ui.TerminalExchangeMarketSessionControllerTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew assemble --no-configuration-cache -PforceToolchainVersion=17`

### 2026-04-25 - Terminal BetterQuesting UI Phase 9：删除旧 terminal ModularUI 过渡实现

- 主题：在 Phase 8 正式入口 cutover 后，删除 terminal 专属旧 ModularUI fallback、旧 GUI 装配层和旧同步/session/dialog 过渡件
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/network/`、`src/main/java/com/jsirgalaxybase/terminal/ui/`、`src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java`、终端相关测试、`docs/README.md`、`docs/terminal-plan.md`、`docs/WORKLOG.md`
- 原因：BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 已经全部由新 `TerminalHomeScreen` 主链承接；旧 `OpenTerminalMessage` / `TerminalHomeGuiFactory` fallback 已不再是正式运行部件，继续保留会增加误接线和回归风险
- 结果：
  - 删除旧 `OpenTerminalMessage`，`TerminalNetwork` 现在只注册新 request / approval / action / snapshot 四类消息
  - 删除 `TerminalService.openLegacyTerminal(...)` 与 deprecated `TerminalService.openTerminal(...)` 兼容别名，`TerminalService` 不再依赖旧 `TerminalHomeGuiFactory`
  - 删除 `TerminalModule` 中 terminal 专属 `GuiManager.registerFactory(...)` 注册和 legacy fallback 日志
  - 删除旧 terminal ModularUI 装配件：`TerminalHomeGuiFactory`、银行/市场 page builder、旧 dialog / widget factory、旧 sync binder / sync state、旧 bank / market / custom / exchange session controller，以及对应旧测试
  - 保留新链仍使用的共享类：`TerminalBankingService`、`TerminalBankSnapshotProvider`、`TerminalHomeSnapshotProvider`、`TerminalMarketService`、`TerminalMarketSectionService`、`TerminalMarketSnapshot`、`TerminalCustomMarketSnapshot`、`TerminalExchangeMarketSnapshot`、`TerminalExchangeQuoteView`、通知与 `TerminalPage`
  - `TerminalMarketService` 已切断对旧 session controller 常量/接口的依赖，custom scope 与 exchange target 由 service / payload 层自身表达
  - 旧入口引用扫描结果：`rg -n "TerminalHomeGuiFactory|OpenTerminalMessage|openLegacyTerminal|openTerminal\\(|TerminalBankPageBuilder|TerminalMarketPageBuilder|TerminalDialogFactory|PanelSyncManager|Terminal[A-Za-z]*SyncBinder|Terminal[A-Za-z]*SessionController|GuiManager\\.registerFactory" src/main/java src/test/java` 无命中
  - terminal 侧 ModularUI 引用扫描结果：`rg -n "com\\.cleanroommc\\.modularui|modularui" src/main/java/com/jsirgalaxybase/terminal src/test/java/com/jsirgalaxybase/terminal` 无命中
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.terminal.TerminalOpenCutoverTest --tests com.jsirgalaxybase.terminal.TerminalServiceTest --tests com.jsirgalaxybase.terminal.TerminalMarketActionMessageFactoryTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.client.gui.framework.VerticalScrollPanelTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --tests com.jsirgalaxybase.terminal.ui.TerminalMarketServiceTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew assemble --no-configuration-cache -PforceToolchainVersion=17`

### 2026-04-25 - Terminal BetterQuesting UI Phase 10：修复新壳缩放、滚动与裁剪

- 主题：只修新 `TerminalHomeScreen` 壳层在高 GUI Scale / 小 GUI 坐标空间下的尺寸、滚动和内容裁剪问题，不改银行/市场业务语义
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeLayout.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、终端壳层布局测试、`docs/WORKLOG.md`
- 原因：实测截图显示新壳在较大 GUI Scale 下仍按硬最小尺寸撑开，导致顶部/底部被挤压；左侧导航与 HOME/CAREER/PUBLIC_SERVICE 等普通 section 页没有局部滚动，section 与通知在内容超出时会越界或重叠
- 结果：
  - 新增 `TerminalHomeLayout`，把主 panel 尺寸改为基于当前 `GuiScreen.width/height` 的安全边距计算；小屏/高 GUI Scale 下不再用 468x320 硬最小值强行撑开
  - `TerminalHomeScreen` 使用新的 layout 结果装配 surface、status band、navigation rail 与 body bounds
  - `TerminalShellPanels.createNavigationRail(...)` 将 nav item 列表放入 `VerticalScrollPanel`，标题固定，导航项超出 rail 时通过滚轮浏览并由 scroll panel scissor 裁剪
  - 普通 section 页正文改为 `VerticalScrollPanel`：section 卡片与通知卡片进入可滚动内容区，底部 session / 刷新 / 关闭按钮继续固定在 body 底部
  - section 均分高度 helper 先扣除 gap 再计算高度，避免 `count * height + gaps` 超过可用高度；通知不再固定裁断前 2 条，而是全部进入滚动区
  - BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 专用业务 section 未改业务语义，继续沿用各自页面内部布局与滚动策略
  - 新增 `TerminalHomeScreenLayoutTest` 与 `TerminalShellPanelsScrollTest`，覆盖小屏 panel 不越安全边距、导航 rail 使用滚动容器、普通 section/notification 使用滚动容器、gap 计算不溢出
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.terminal.client.screen.TerminalHomeScreenLayoutTest --tests com.jsirgalaxybase.terminal.client.component.TerminalShellPanelsScrollTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.client.gui.framework.VerticalScrollPanelTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.terminal.TerminalOpenCutoverTest --tests com.jsirgalaxybase.terminal.TerminalServiceTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew assemble --no-configuration-cache -PforceToolchainVersion=17`
  - 本地体验环境：检测到已有 `runServer` / `runClient` 进程在运行，未杀用户进程；`ss -ltnp | grep 25100` 显示 `127.0.0.1:25100` 已监听，server 日志有 `Done`、banking prepared 与 `Market runtime prepared`，client 日志有 `Registered terminal client entry handlers` 且已连接 `127.0.0.1:25100`

### 2026-06-07 - Terminal ServerTools 收口：传送页改为三栏工具页

- 主题：把 `SERVER_TOOLS` 从 generic section 风格收口为 workflow-first 的专用传送工具页，不改真实 warp 后端主链
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalServerToolsSectionSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalServerToolsSectionModel.java`、相关序列化与测试、`docs/WORKLOG.md`
- 原因：当前 `传送 / 群组服` 页已接通真实 warp 链，但页面仍是混合 section/scroll 形态；warp 列表、说明、状态与按钮竞争同一正文区，不符合三栏传送工具页目标
- 结果：
  - `SERVER_TOOLS` 正文切到专用布局：左侧保留窄导航轨，主体改为服务器目录卡 + warp 列表卡 + 右侧详情/最近状态/风险/主 CTA 三段结构
  - warp 列表改为稳定 item 几何，支持主名、副文案、状态标签与选中高亮，不再依赖普通文本段落
  - 右栏详情改为结构化字段：当前服务器、目标服务器、目标坐标、传送说明；最近传送状态单独成卡
  - 顶部状态栏为 `SERVER_TOOLS` 增加紧凑刷新按钮；底部 footer 不再给该页放同级刷新按钮，只保留弱化关闭入口
  - ServerTools snapshot/model 追加每个 warp 的副文案/状态标签，以及右栏和最近状态所需的结构字段；网络序列化按现有 server-tools nullable block 后续追加
  - 传送确认 popup 继续走现有确认弹窗，服务端仍复用 `PlayerTeleportService.prepareWarpTeleport(...)` 和 `ServerToolsModule.dispatchTeleport(...)`

### 2026-06-14 - Terminal 标准商品市场收口：补齐存入/卖出/即时成交/撤单工作流

- 主题：把 `MARKET_STANDARDIZED` 从“买单 + 文本说明”页收口成标准商品市场正式工作页，不改三市场架构、不重写标准市场后端
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalActionType.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalMarketActionPayload.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalMarketSectionSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalMarketActionMessageFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSectionService.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContent.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionState.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalMarketSectionModel.java`、相关测试、`docs/WORKLOG.md`
- 原因：标准市场后端已具备 `depositInventory(...)`、`createSellOrder(...)`、`cancelSellOrder(...)`、`createBuyOrder(...)`、`cancelBuyOrder(...)` 等能力，但终端页此前只露出限价买单和 claim，无法完成玩家实际交易闭环
- 结果：
  - 标准市场新增正式 action：`MARKET_CONFIRM_DEPOSIT_HELD`、`MARKET_CONFIRM_LIMIT_SELL`、`MARKET_CONFIRM_INSTANT_BUY`、`MARKET_CONFIRM_INSTANT_SELL`、`MARKET_CANCEL_ORDER`
  - `TerminalMarketActionPayload` 扩展为兼容旧 4 段和新 9 段编码，保留 `selectedProductKey` / 买单字段，同时追加卖单、即时买卖、撤单所需字段
  - `TerminalMarketSectionService` 与 `TerminalService` 补齐标准市场动作闭环，继续复用 `TerminalMarketService` 已有真实链路：存入、限价卖单、即时买入、即时卖出、撤单、claim 全部通过 action -> snapshot 回写
  - 标准市场 snapshot/model 追加盘口数量、仓储提示、动作预览、订单 id/可撤销标记、卖单/即时交易 drafts 与 `depositEnabled`，网络序列化同步扩展
  - `TerminalShellPanels` 对 `MARKET_STANDARDIZED` 取消外层 generic scroll wrapper，正文直接挂专用 section；`TerminalMarketSection` 改成稳定三栏：左侧商品与仓储、中间盘口/订单/CLAIMABLE、右侧存入/限价买卖/即时买卖动作区
  - 高风险动作统一走确认 popup：存入、限价卖单、即时买入、即时卖出、撤单；原有限价买单和 claim popup 继续保留
  - `CLAIMABLE` 与“我的订单”从纯文本摘要改为可操作区：claim 行可直接弹确认，订单行可直接发起撤单确认
  - 已实际执行并通过：`git diff --check`
  - 已实际执行并通过：`docker compose -f /media/u24/data/gtnh/docker/projects/docker-compose.yml run --rm -e GRADLE_USER_HOME=/tmp/gradle-home galaxy-dev ./gradlew test --tests com.jsirgalaxybase.terminal.TerminalServiceTest --tests com.jsirgalaxybase.terminal.TerminalMarketActionMessageFactoryTest --tests com.jsirgalaxybase.terminal.client.component.TerminalShellPanelsScrollTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --tests com.jsirgalaxybase.modules.core.market.application.StandardizedSpotMarketServiceTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`docker compose -f /media/u24/data/gtnh/docker/projects/docker-compose.yml run --rm -e GRADLE_USER_HOME=/tmp/gradle-home galaxy-dev ./gradlew test --tests com.jsirgalaxybase.terminal.client.screen.TerminalHomeScreenLayoutTest --tests com.jsirgalaxybase.terminal.client.component.TerminalShellPanelsScrollTest --tests com.jsirgalaxybase.terminal.TerminalServiceTest --tests com.jsirgalaxybase.terminal.TerminalMarketActionMessageFactoryTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --no-configuration-cache -PforceToolchainVersion=17`
  - 本机仍无 `java`，本轮验证全部通过仓库既有 Docker Gradle 路径完成
### 2026-07-20 - Standard market hover lifecycle and visual liquidity fixture

- Fixed the GUI lifecycle fault where a browse hover overlay could survive `initGui()` after a product click rebuilt the terminal into standardized-market `DETAIL` mode. Root rebuild and GUI close now clear transient hover overlays before the new route is drawn.
- Expanded `scripts/market-demo-fixture.sh` to a managed v2 fixture. It seeds every enabled formal standardized product with escrow-backed sell liquidity, frozen-funds-backed buy liquidity, and recent real `market_trade_record` samples for 24-hour volume and hover/detail price history. The fixture is idempotent by product/version and does not mutate player accounts, player custody, or player orders.
- Fixed the standardized buy-order banking correlation overflow: a terminal request id could be valid in the market log yet become too long after `market:buy-freeze:` was prepended for the bank `business_ref VARCHAR(64)`. Long references now use a deterministic UUID-derived compact key, while the full request id remains in the market operation log and bank audit JSON.
- Cancel requests against an order that is already filled/cancelled are now recorded as safe `FAILED` precondition rejections. They no longer rewrite settled orders or custody to `EXCEPTION`; migration `20260720_001_repair_safe_cancel_rejections.sql` repairs the two affected historical cancel logs and their settled order.
- 验证：`git diff --check`、`StandardizedSpotMarketServiceTest`、`TerminalMarketServiceTest`、`TerminalMarketSectionServiceTest`、`MarketItemGridPanelTest` 与 `HoverOverlayPositionerTest` 均通过。无客户端烟测确认严格市场审计异常数为 `0`，正式目录为 8 项、活跃买单 9、卖单 13、24 小时成交 81，8 个商品每项都有至少 10 个真实价格点。runtime jar `1737f92895c92d7d179efee7751c60f228b926da3259c163479bcb76114fd85c` 已部署至 Lobby 与客户端，Lobby 进程为 `RUNNING`。

### 2026-08-08 - Standard market five-level visual closeout

- Compressed the selected-product header to a single row, expanded bid/trade/ask to five rows, split the compact statistics panel into explicit market and account groups, and introduced semantic green/red/amber action buttons.
- Expanded the right chart to use the full detail height. It now connects real price samples with a blue path, retains red/green volume bars, and labels intermediate time coordinates in addition to price and volume axes.
- Upgraded `market-demo-fixture.sh` to v3 with five escrow-backed ask levels, five frozen-funds-backed bid levels and 24 time-ordered real trade samples per enabled catalog product. No client-rendered fake values were added.

### 2026-07-20 - 市场三段式整改 Phase 3：定制与汇率市场接入共用浏览/详情骨架

- 定制市场和汇率市场迁移到共用 `BROWSE -> DETAIL` 客户端状态：浏览使用四列网格，详情单独占用工作区；市场语义仍分别保持挂牌交付链与任务书硬币正式报价链。
- 定制市场保留全部挂牌、我的出售、待领取三个服务器权威范围。切换范围会清空旧选择并请求对应 scope 的新 snapshot，避免本地状态变化而网格继续显示旧范围。
- 汇率市场按完整 `TaskCoinCatalog` 浏览所有任务书硬币，点击仅选择用于展示的目录币种；兑换执行继续由服务端验证玩家实际手持物、报价有效期和限额，不能通过浏览选择绕过 gate。
- 新增 `MarketBrowseDetailController`，统一保存查询、页码、网格滚动位置与当前选择；空 snapshot 会彻底重置该临时状态。更新终端布局测试，使其校验新 browse/detail surface 的边界契约，而非已移除的双常驻面板。
- 验证：`git diff --check`；Docker Gradle 定向通过 `TerminalServiceTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`MarketItemGridPanelTest`；`assemble` 通过。未启动客户端或做截图验收。

### 2026-07-20 - 银河仓储网络 v1：AE2 实体仓储方向确认

- 确认未来个人、企业和公共仓优先复用 GTNH AE2 的真实存储元件、容量、频道、供电和网络拓扑；JGB 不实现无限虚拟背包，也不复制 ME 单元的物品余额。
- JGB 的责任收口为仓库账户、权限、登记端口、资产域转移、审计、恢复与容量健康摘要；标准市场托管继续是独立数据库结算账本。
- 新增 `docs/warehouse-ae2-integration-v1.md`，定义 `Warehouse Port -> Warehouse Controller -> 已登记 AE2 网络` 的受控接入模型，以及市场托管桥、物品边界与分阶段实施顺序。

### 2026-07-20 - 银河仓储网络 Phase 0：账户绑定 AE2 Drive 调查

- 根据 AE2 Unofficial 源码确认，未来 JGB 仓储的准确模型调整为“账户绑定的
  `Warehouse Drive` + 真实 AE2 Storage Cell”，而不是 JGB 虚拟仓或仅靠 Port
  接入任意 ME 网络。
- `Warehouse Drive` 将以 AE2 `IChestOrDrive` / `ICellContainer` 的方式作为真实
  Grid Host 接线；AE2 自动纳入其 Cell，JGB 不复制物品内容、不另造终端。
- JGB 管理 Drive 归属、Cell Bay 数、插拔/拆卸锁、端口交割、审计与恢复；市场
  托管继续为独立数据库资产域。
- 记录关键边界：接入同一 AE 网络后的原生终端读写需依赖 AE2 Security 的
  `INJECT` / `EXTRACT` 权限；企业/公共仓 v1 不承诺在普通 AE 线缆上实施逐物品
  JGB 权限过滤。详细调查、风险与 Phase 0 尖峰清单见
  `docs/warehouse-ae2-integration-v1.md`。

### 2026-07-20 - 银河仓储网络 Phase 0：Base Vault 起步保险箱补充

- 明确玩家无 AE2 元件时仍需要一个账户绑定的基础存储：`Base Vault`，体验类似
  末影箱但由 JGB 的 PostgreSQL 槽位账本保存，初始固定 27 格，完整保留
  `ItemStack` NBT，支持跨灰度服、企业/公共账户权限、幂等交付与恢复。
- Base Vault 是有限的 `BASE_VAULT` 资产域，承担市场 `CLAIMABLE`、定制交付和
  离线奖励的安全第一落点；满仓时资产留在来源域/PENDING 状态，绝不丢失或重复发放。
- Base Vault 不接入 AE2 Storage Grid；真实 AE2 Cell 容量仍由账户绑定
  `Warehouse Drive` 提供。后续通过受审计 Port 在 Base Vault、AE2 Drive 与市场
  托管之间迁移，避免形成无成本无限 ME 存储。
## 2026-07-24 - Base Vault native container polish

- Compressed the personal Base Vault container to the natural 9 x 3 chest-plus-player-inventory proportion, removing redundant Vault/player-inventory headings and the persistent Shift instruction.
- Kept the Galaxy Terminal visual skin but simplified the header to `银河终端` plus capacity, matching the spatial hierarchy of an Ender Chest rather than a terminal documentation page.
- Added a compact header return button that closes the container and reopens the Galaxy Terminal through the existing terminal approval route.

### 2026-08-07 - Base Vault audited server sort

- Added a Vault-only sort request and header control. It never registers the persistent Vault with Inventory Bogo Sorter and never mutates player inventory, cursor or drops.
- `BaseVaultService` now locks the personal account, deterministically groups/merges full-NBT-identical stacks using `registry-meta-nbt-v1`, and commits changed slots atomically.
- Added `warehouse_operation_slot_change` migration. Every changed sort slot records before/after compressed ItemStack snapshots and version values under its `VAULT_SORT` operation for recovery and audit review.
- Generalized the same audited sort contract to `ENTERPRISE` and `PUBLIC` Base Vault accounts, each with its fixed 54-slot capacity. Their GUI opening remains blocked until enterprise/public authority checks exist; the service path is available to those future authorized controllers now.
- Validation: `git diff --check`, Docker `testClasses`, and targeted Docker tests for `BaseVaultServiceTest` / `VaultSortPlannerTest` pass. The 54-slot enterprise/public test performs a real reorder and asserts two audited slot deltas per account type. No server, client, or database deployment was performed.
- Deployment follow-up: migration `20260807_001_add_base_vault_sort_audit.sql` applied successfully and runtime jar SHA256 `c19e783cde05a651868c6bf72ec123b57dc40ce7c4977c4fbb630d3afbed538d` was copied to Lobby, S2 and the GTNH client. Lobby reached `RUNNING` and logged `Done`. S2 received the same jar but remains `EXITED` because both `World/level.dat` and `level.dat_old` are corrupt; no world recovery or rollback was attempted.
- Vault UI follow-up: replaced the header's text-only `S` sort control with an icon-only three-row sort glyph and a hover label (`整理保险箱`), keeping the action auditable and visually compact.
### 2026-08-07 - 现代交易终端改造方案

- 主题：将标准市场从手动托管操作页收口为以账户仓自动交割为基础的现代交易终端。
- 决策：个人 Base Vault 是当前账户仓；未来已授权 AE 仓优先、Base Vault 回退。卖单确认时内部锁定账户仓物品进入市场托管，买入后自动投递账户仓；只有投递失败才显示待领取恢复动作。`AVAILABLE`、`ESCROW_SELL`、`CLAIMABLE` 与冻结资金保留为审计状态，不再作为玩家日常操作步骤。
- UI 方向：四列浏览卡必须显示真实价格波动；Hover 提供只读行情摘要；点击后进入含真实 K 线/成交量、盘口和可编辑买卖交易单的独立详情页。详见 `docs/modern-trading-terminal-redesign-v1.md`。

### 2026-08-08 - 非标准市场三段式浏览服务端收口

- 定制市场与汇率市场的浏览页统一使用服务端输出的结构化 `TerminalMarketBrowseEntry`、查询和分页边界；客户端不再以平行字符串列表或本地任务书硬币目录作为主要真相来源。
- 汇率市场把用户点选的目录币种保留为详情上下文，但正式报价和执行仍以服务器验证的个人 Base Vault 格位为准，不能通过 UI 选择绕过资产校验。
- 补充 Custom/Exchange 浏览页终端网络包往返测试，覆盖条目、查询词、页码与总数。业务结算、定制挂单交付与标准市场撮合均未改写。

### 2026-08-11 - 市场与仓储边界收口

- 终端请求与快照追加单调递增的响应序号，并保留旧包兼容解码。客户端只接收不早于当前已应用响应的快照，解决汇率报价、自动刷新和页面切换之间的异步回写竞态。
- 标准、定制与汇率市场统一使用轻量自动刷新；输入框正在编辑、确认弹窗打开或页面已经切换时不会用后台刷新覆盖玩家上下文。普通汇率刷新只读取现有报价状态，不再隐式生成新报价。
- 清理玩家可见的协议和资产工程术语：确认弹窗、空态和反馈改为商品、账户仓、待收货、报价状态等玩家语言；内部审计状态与兼容路由继续保留，不作为新 UI 主流程入口。
- 新增 Base Vault 授权地基：个人仓仅本人和管理员可访问；企业/公共 54 格仓为后续权限适配预留，未提供权威授权时默认拒绝。VIEW、DEPOSIT、WITHDRAW、SORT 分离，避免未来组织仓沿用个人仓判断。
- `scripts/market-audit.sh --strict` 扩展仓储运维检查：未完成/待恢复操作、账户容量契约（个人 27，企业/公共 54）和越界槽位，并输出可行动记录及槽位变更数量。
- 本轮自动验证覆盖协议兼容、响应序号、三市场业务服务、市场页面状态、自动刷新、Base Vault 权限与 54 格容量。真实玩家资金和物品交易验收仍由服主单独执行，不以静态测试代替。
- 完整 Docker Gradle 测试通过：共执行 316 项测试，31 项按既有条件跳过，无失败；`git diff --check` 与 `scripts/market-audit.sh --strict` 通过，实时审计 `anomaly_count=0`。审计发现的两条历史失败操作均为对已成交订单的重复撤单，服务端已安全拒绝，不计为资产异常。
- 构建产物 SHA-256 为 `6a151221d6a0c8c8f999cac46c476888e5b5e63d5ff9464c13695c940f69ad72`，Lobby、S2 和客户端目录中的模组文件哈希一致。Lobby 已进入 `Done`，市场启动烟测确认数据库正式目录 `8/8` 准入；S2 虽已替换同一产物，但仍因既有世界数据初始化崩溃而退出，本轮未擅自重建、回滚或修复其世界。
### 2026-08-12 - 市场操作反馈 Toast 与请求 ID 长度收口

- 定位 Lobby 标准市场卖出失败并非数据库断连，而是市场请求 ID 在追加 `:custody` / `:order` 等派生后超过 PostgreSQL `VARCHAR(64)`；新增统一请求 ID 长度预算，根 ID 固定为最多 46 字符，为最长恢复后缀预留空间。
- 市场异常反馈区分连接不可用、数据库约束拒绝和普通业务拒绝，不再把原始 JDBC 文案直接显示给玩家；失败事务保持服务端回滚语义。
- 客户端将市场/银行/传送动作的成功、警告和失败通知桥接到全局右上角 Toast；静态 INFO 说明不会反复弹出。Toast 改为响应式宽度、最多四行正文，错误提示保留 7 秒，便于阅读真实失败原因。
- 验证：`git diff --check` 通过；Docker Gradle 定向执行 `MarketRequestIdFactoryTest`、`TerminalMarketServiceTest` 与 `TerminalServiceTest` 通过。部署后 Lobby 达到 `Done (1.560s)!`，构建产物、Lobby 与 Prism 客户端 JAR SHA256 均为 `5099c5bd49b0af144bfda1cc42c7d109c6313623a10101a044dcd54ea30e1c97`。PostgreSQL 实际约束仍为 `market_operation_log.request_id VARCHAR(64)`，新根 ID/派生后缀测试覆盖此边界。
# 2026-08-12 标准市场个人历史与动作回执收口

- 标准商品详情第三按钮从无目标撤单改为“历史”，新增账户级跨商品订单历史视图。
- 历史支持商品范围、买卖方向、订单状态和时间范围筛选；仅具体可撤订单行提供撤单动作。
- 终端内动作通知按请求序列去重，并在展示前屏蔽数据库/异常实现细节。
- 订单摘要补齐商品键、方向、总量、成交量、剩余量、状态和创建时间，便于玩家追踪部分成交与撤单结果。
- 历史筛选和分页正式下沉到服务端数据库查询：客户端只提交商品范围、买卖方向、状态、时间、页码和页大小，账户身份由服务端当前登录玩家解析。
- 历史页筛选、翻页和具体订单撤单后均重新请求服务端；撤单 payload 保留当前筛选上下文，不再退回默认历史页。
- 修复标准市场草稿默认值补全时丢失历史查询字段的问题，并补充筛选 payload、精确撤单上下文、分页元数据网络往返测试。
- 验证：`git diff --check` 通过；Docker Gradle 定向执行 `TerminalMarketSectionServiceTest`、`TerminalMarketSectionStateTest`、`TerminalMarketActionMessageFactoryTest` 和 `TerminalMarketCatalogPacketTest` 通过。

### 2026-08-15 - 标准市场本地化名称与盘口语义收口

- 标准商品身份继续使用稳定商品键与 `registry + meta`，不把客户端语言写入数据库；商品浏览、详情 Hero 和订单历史改为从真实 `ItemStack` 读取当前客户端语言的显示名，数据库目录名仅作为管理、搜索和异常回退文本。
- 行情图继续允许使用 `CARRY_FORWARD` / `REFERENCE` 桶保持时间轴连续，但“最新成交”列只展示真实成交记录，不再把补齐行情误报为 `x0` 成交。
- 盘口标题统一为“买盘 / 最新成交 / 卖盘”，账户区统一为“可用库存 / 冻结资金 / 当前委托 / 待入库”，并以库存箱、入库箭头和订单单据图标替代三个含义相同的状态点。
- 验证：`git diff --check` 通过；Docker Gradle 完整 `test` 通过（`BUILD SUCCESSFUL`）。
- 部署：构建产物已同步到 Lobby、S2 与客户端，三处 SHA-256 均为 `4e416c24eeb56d4f4f18cd1d8aa01c1d514015bcbe6608993f076cabf9d75826`；Lobby 已启动至 `Done`。S2 仍因既有 `World/level.dat` 与 `level.dat_old` ZLIB/EOF 损坏退出，本轮未擅自重建或回滚世界。
# Standardized market UX completion baseline (2026-08-15)

- Added `docs/standardized-market-ux-completion-v1.md` as the independent completion matrix for order entry, freshness, order history, notifications, localized discovery, charts, explicit states, and non-color-only status.
- The work deliberately preserves server-authoritative pricing, fees, inventory, banking, matching, delivery, and recovery semantics.
- Manual visual acceptance remains a final step after targeted tests and Lobby/S2/client deployment.

### 2026-08-15 - Standardized market UX completion implementation

- Completed the modern order ticket with percentage quantity shortcuts, bid/latest/ask price shortcuts, explicit source/destination, gross estimate, server fee policy, and concrete disabled reasons. Server-side settlement remains authoritative.
- Added explicit live-data freshness states (`FRESH`, `REFRESHING`, `DELAYED`, `STALE`) and prevented localized search text from being replaced while the stable product-key request is in flight.
- Improved personal order history with localized product names, percentage fill progress, lifecycle markers, and cancellation only for orders with an open remainder.
- Replaced generic blank browser panels with distinct catalog, search, filter, and stale-data empty states. Important states continue to include text or numeric meaning in addition to color.
- Validation: `git diff --check`; Docker Gradle targeted tests passed for `TerminalMarketSectionStateTest`, `TerminalMarketSectionContentTest`, `MarketLiveRefreshControllerTest`, `MarketOrderEntryPopupTest`, `TerminalShellPanelsScrollTest`, `TerminalHomeScreenLayoutTest`, `TerminalHomeScreenModelTest`, `TerminalMarketActionMessageFactoryTest`, `TerminalServiceTest`, and `TerminalMarketServiceTest`.
- Dynamic visual acceptance is intentionally left to the user after deployment: localized search, ticket shortcuts and disabled reasons, freshness transitions, history filters/cancellation, notifications, chart interaction, and empty/stale states.
- Deployment verification: runtime artifact SHA-256 `a975e711dd8adbc59d4828164fddd00ff8fe16a0ea0584c92e68cded7a174fd6` matches the Lobby, S2, and Prism client copies. Lobby reached `Done (1.406s)`. S2 received the same artifact but remains `EXITED` because the pre-existing `World/level.dat` and `level.dat_old` both fail with ZLIB/EOF corruption; no S2 world repair, replacement, or rollback was attempted.

### 2026-08-16 - 标准市场撤单与订单资产中心首轮路由收口

- 商品详情的第三主动作改为 `撤单`，详情快照只读取当前商品的个人委托；没有 `OPEN` 或 `PARTIALLY_FILLED` 剩余量时显示明确空态，不会误指向其他商品的订单。
- 新增受限的 `MarketAccountCenterQuery` 与 `MarketAccountCenterSnapshot` 合同：账户所有者不在客户端查询中，搜索词、页码和每页数量均在服务端值对象中收敛；快照以服务端总数计算页数。
- 新增 `MARKET_ACCOUNT_CENTER` 路由并复用已有服务器分页查询和逐行撤单链，页面标题与状态栏明确为“订单与资产中心”。旧历史 payload 保留兼容解码，标准市场撮合、银行、Base Vault 与恢复服务未改写。
- 验证：`git diff --check` 通过。Docker Gradle 编译在本轮环境中启动后未在工具的 30 秒输出窗口内返回最终状态，未部署 Lobby、S2 或客户端。

### 2026-08-16 - 标准市场撤单与订单资产中心完整整改

- 商品详情固定为 `买入 / 卖出 / 撤单`；当前商品撤单弹窗只展示本人 `OPEN` / `PARTIALLY_FILLED` 且剩余量大于零的委托，支持表格区滚动。二次确认展示短订单号、方向、成交/总量、剩余量、预计返还和真实返还目标。
- 撤单请求新增幂等 `requestId` 与订单更新时间版本；终端与业务服务均重新校验玩家归属、状态、剩余量和版本。确认期间变化会警告并刷新；过期版本被归类为安全拒绝，不会把正常订单误标为异常，也不会重复解冻或返还。
- 新增独立全屏 `订单与资产中心`：六项账户摘要、`当前委托 / 成交记录 / 资产与交付 / 历史查询` 四页签、统一筛选栏、唯一表格滚动区与固定分页脚。市场与 Vault 顶栏图标分别直达当前委托和资产交付页签。
- 新增受限的 `MarketAccountCenterQuery`、四类结构化 `MarketAccountCenterSnapshot` 行及结构化网络 DTO。商品身份使用 `registry + meta`，客户端创建真实 `ItemStack` 并本地化名称；中心 UI 不再拆解旧历史显示字符串。旧历史 payload 仅保留兼容解码。
- 当前委托与历史、成交、市场异常、Base Vault 异常均由服务端当前玩家身份查询并使用真实总数分页。资产页合并待收货、Vault 满仓、市场返还/银行解冻失败、Vault `FAILED` 与 `RECOVERY_REQUIRED`；不自动执行恢复动作。
- Toast 支持点击进入对应页签、按记录号筛选并高亮目标。定制挂牌和汇率兑换继续保持原业务入口，不混入标准订单中心。
- 自动验证新增查询上限、`0/1/4/5/8/9/11` 分页边界、`11/4=3`、结构化协议往返、过期版本撤单和重复请求不重复解冻测试。定向 Docker Gradle 已执行 83 项并全部通过；最终完整测试、diff 检查和部署结果见本次交付记录。
- 最终验证：`git diff --check` 通过；Docker Gradle 完整 `test` 通过（298 项完成，32 项按既有条件跳过，无失败）；Lobby 市场烟测通过，数据库 8 个启用标准商品全部被运行时准入，8 类商品均有真实成交数据。
- 部署：`scripts/deploy-jgb.sh --targets lobby,s2,client` 已构建并同步 runtime jar，源码产物、Lobby、S2 与客户端四处 SHA-256 均为 `b435264bb2e6166f291295ad5f7044699791cda6abdaf435c14196e8d650ae9b`；未启动客户端，也未触碰 S1。Lobby 达到 `Done (1.423s)`。S2 收到同一产物后仍因既有世界 NBT 的 `Unexpected end of ZLIB input stream` / `EOFException` 退出，本轮未修复、替换或回滚其世界。

### 2026-08-17 - 订单中心入口、撤单弹窗实底与 GregTech 中文名复核整改

- 根据实际客户端截图复核，确认首轮交付仍有三处不合格：订单中心入口只是 8–12 像素的无文字标题栏图标且中心路由自身不显示入口；当前商品撤单弹窗覆盖了 `drawSelf` 却未绘制父级弹窗底板；GregTech 商品仍可能直接落回目录英文名。
- 市场与 Vault 标题栏入口改为带订单图标的 `订单中心` / `资产中心` 宽按钮，订单中心路由自身继续显示入口并使用亮蓝边框与高亮填充，避免把侧栏的“市场”选中态误称为订单中心高亮。
- 撤单弹窗恢复父级 `ModalPopupPanel` 底板绘制；`POPUP_FILL` 从 alpha `0xF0` 调整为 `0xFF`（100% 不透明）。屏幕外层压暗遮罩保持 `0xAA`（170/255，约 66.7% 不透明），两者语义分离。
- 已核对 Prism 实例 `options.txt` 为 `lang:zh_CN`。商品浏览、详情、撤单标题和结构化商品行统一使用 `registry@meta` 解析真实 `ItemStack`；GregTech 元物品通过其 `GTLanguageManager` 读取中文格式，并使用 OreDict 关联材质替换 `%material`，例如 `Material.steel=钢` 与 `%material锭` 组合为 `钢锭`，不再把服务端英文目录名作为正常显示结果。
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过 `TerminalMarketVisualsTest`、`TerminalShellPanelsScrollTest`、`TerminalMarketSectionContentTest`、`TerminalThemeRegistryTest`；Docker Gradle 完整 `test` 通过（`BUILD SUCCESSFUL`）。未启动客户端或自动截图。
- 部署：`scripts/deploy-jgb.sh --targets lobby,s2,client` 已构建并同步；构建产物、Lobby、S2 与 Prism 客户端四处 SHA-256 均为 `69a9a68f82c24a57f24c2661dbb3592c6b7280c75ac91d21e50eca7f484bc4a3`，客户端文件时间为 `2026-08-17 12:28:16 +0800`。Lobby 达到 `Done (1.365s)`；S2 仍因既有 `level.dat` / `level.dat_old` 的 `Unexpected end of ZLIB input stream` / `EOFException` 未到 Done，故部署脚本最终返回非零。未启动客户端、未自动截图，目标列表未包含 S1。

### 2026-08-17 - 撤单弹窗终端内约束与订单中心内容区扩容

- 根据实际客户端截图修正缩放布局：当前商品撤单弹窗不再以整个 Minecraft 屏幕为定位边界，改为使用 `TerminalHomeLayout.panelBounds`。弹窗四边始终留在终端内部，高度按实际委托行数计算，最多直接展示 5 行，更多记录只在表格区滚动；单行/空态不再占满终端高度。
- 订单与资产中心重新分配纵向预算：账户摘要卡由 34 压缩至 28，页签由 20 压缩至 18；宽度不少于 600 时，搜索、搜索/重置动作及商品、方向、状态、时间筛选合并为单行。表头偏移从 120 降至 80，窄界面保留双行工具栏并使用 103 偏移，固定页脚和服务端分页语义不变。
- 新增弹窗宿主边界/内容驱动高度测试，以及订单中心宽窄布局的表格预算测试。Docker Gradle 定向测试与完整 `test` 均通过（`BUILD SUCCESSFUL`）；`git diff --check` 通过。未启动客户端或自动截图。
- 部署：`scripts/deploy-jgb.sh --targets lobby,s2,client` 已构建并同步；构建产物、Lobby、S2、Prism 客户端四处 SHA-256 均为 `59ca489d69a3d485a6b1f755e6764ed35ce868c3ea2f9ac474b96d8cf43115bf`，客户端文件时间为 `2026-08-17 22:40:57 +0800`。Lobby 达到 `Done (1.548s)`；S2 仍因既有世界 NBT 的 ZLIB `EOFException` 未到 Done，部署脚本因此返回非零。未启动客户端、未自动截图，部署目标未包含 S1。

### 2026-08-24 - 个人地产 Batch C.2 大地图与交互改造

- 地产附近地图从居中正方形改为完整矩形工作区，最长边按近 9、中 15、远 31 个正方形区块显示；地图工具栏、图例和指针状态收进画布，右侧检查器限制为紧凑宽度。
- 玩家标记改为真实客户端皮肤头像定位针并带方向箭头；悬浮补齐安全长整型方块范围、距离、加载/生物群系、产权号/版本、所有者脱敏状态和操作原因。
- 接入左键选择、4px 拖拽阈值、滚轮锚点缩放、右键权威快照后菜单、`F/R/+/-/Tab/Esc`。确认框打开时继续由终端 popup 层独占输入，认领/放弃仍走服务端身份、半径、版本和幂等校验。
- 认领费用、参考地价、挂牌价和最近过户价只显示“未启用 / 未挂牌 / —”；本批没有增加数据库字段、银行扣款、客户端身份字段或地产交易合同。
- CodeGraph 前置查询覆盖 `TerminalLandMapPanel`、`LandMapViewport`、`TerminalLandSection` 和 `CanvasScreen`；自动测试覆盖矩形数学、输入分流、图层状态和最小 620×340 布局。最终构建、部署和哈希记录在本批交付结果中补齐。
- 最终验证：Docker Gradle 全量 `test` 通过（409 项，37 项环境跳过，0 失败）；另以 Lobby PostgreSQL 配置强制执行 `LandPostgresIntegrationTest` 5 项，0 跳过/失败。`assemble`、语言文件结构检查和 `git diff --check` 均通过。
- 部署：同一 runtime JAR 仅同步到 Lobby 与 Prism 客户端，三处 SHA-256 均为 `7725268364894f4dacd3b25dd7db8917930d06f1c2719bbda36943724f8bc45d`；客户端未启动，S1/S2 不在目标中。Lobby 达到 `Done (1.435s)`，地产运行时以 `SHADOW` 就绪并恢复 1 条有效产权；Lobby 与客户端均继续保留 `ServerUtilities-2.2.2.jar`。
# 2026-08-31 - 物品准入策略终端诊断页

- 终端新增“物品策略”只读页：显示服务器已解析拒绝规则，以及当前玩家、本服范围内最近 12 条拒绝审计。页面不扫描背包、不改写既有物品，也不暴露其他玩家或跨服记录。
- 策略未启用、运行时不可用和审计数据库暂时不可读均显示明确状态；策略本身不会因诊断查询失败降级为允许。
- 新增 PostgreSQL 查询隔离/时间排序覆盖，并回归终端导航与禁用策略安全空态。
# 2026-08-31 - 全局 Hub / Spawn 与目标服 RTP

- `/spawn` 现在可由服务端 `globalHubTarget` 配置提升为跨服全局 Hub；未配置时严格保留当前世界出生点语义。
- `/rtp <targetServerId>` 使用现有 transfer ticket：源服只校验配置和服务器目录，目标服才在自己的维度选择安全随机点，随后以 ticket requestId 幂等写入最终 RTP 审计。源服不会访问或加载远端世界。
- 新增 `20260831_002_add_global_entry_rtp_audit.sql`，为 RTP 审计补齐 requestId 和实际目标服；覆盖配置解析、远端计划不预写伪坐标、目标服解析与 PostgreSQL 迁移/幂等写入。
- 验证：Docker 定向单测、Lobby PostgreSQL 临时 schema 集成测试、完整 Docker `test` 与 `assemble` 均通过；完整测试为 465 项通过、48 项既有环境条件跳过。
- 部署：`20260831_001_add_servertools_admin_audit.sql` 和 `20260831_002_add_global_entry_rtp_audit.sql` 已按迁移历史表应用。runtime JAR SHA-256 `6409361a211ca9644d0031382906f2af625e17de2fe899aadc5db1f548116b20` 已同步至 Lobby 与 Prism 客户端；Lobby 已到 `Done (1.296s)`，客户端未启动，S1/S2 未触碰。全局 Hub 与目标服 RTP 配置保持默认空值，未在本轮启用。

# 2026-09-02 - 银河资产中心统一容器与视觉重构

- 将终端仓储入口重构为紧凑的双列 Vault/Bay 状态卡、两个直接工作区入口和自适应审计区，移除窄终端下强制纵向堆叠造成的越界。
- 新增接近全屏的 `资产概览 / Base Vault / AE2 存储 / 操作记录` 四页签资产中心。Base Vault 27 格、单个真实 AE2 Cell Bay 与玩家背包由同一个服务器权威 Container 管理，不再弹出两套简陋小窗口。
- Forge 只注册一个资产 GUI handler，并兼容旧 Vault/Bay GUI ID；修复同一 Mod 实例重复注册 handler 可能覆盖前一个入口的问题。
- 服务端按页签拒绝隐藏槽位点击，Vault 与 Bay 继续分别使用既有版本、幂等和审计合同；失败交互恢复 Vault、Bay、玩家背包和光标快照。操作记录仅查询当前玩家并合并普通 Vault/Bay 记录。
- 不伪造银行、ME 网络、频道、供电或 64K 容量；Cell 未插入时显示真实空态，插入后才读取其真实 AE2 容量与类型数据。
- 自动验证覆盖 `350×193` 和 `620×340` 布局、终端入口不越界、页签槽位可见性、审计协议上限、Vault 最近操作隔离以及 Bay/Vault 服务回归。Docker Gradle 全量 `test`、使用 Lobby PostgreSQL 配置的 `WarehousePostgresIntegrationTest`、`assemble` 与 `git diff --check` 均通过。
- 部署：runtime JAR、Lobby 与 Prism 客户端三处 SHA-256 均为 `c36ee1b9a837abdd82abe3fa7ddeb01ccf7d642b9342f63804be2fafba5a9aeb`。Lobby 完成 `FMLServerStartedEvent`，日志确认 `Registered unified personal asset center container GUI`；客户端未启动，部署目标未包含 S1、S2。

# 2026-09-02 - 资产显示框架与业务动态后续待办

- 实机复核确认独立资产 Container 仍存在两项架构问题：它没有复用终端壳组件，返回时关闭容器后重新请求终端会闪屏；当前“操作记录”把手工槽位变更和 Bay 版本审计暴露成了玩家内容。
- 后续不把整个终端根页面升级成 Container。计划抽离无继承关系的 `CanvasSceneRuntime`，由普通 `GuiScreen` 宿主和原生 `GuiContainer` 宿主共同委托；在其上分别建立 `TerminalScreenBase` 与 `TerminalContainerScreenBase`，并共享状态栏、导航、主题、弹窗和内容区布局。
- 资产 Container 页面将直接展示 Vault、Cell Bay 与玩家背包，移除说明卡和二次入口；服务端 Container、版本、准入、失败恢复与技术审计继续保留。
- 玩家可见记录改为结构化“资产动态”，只纳入真实卖出成交到账、买入入库、定制交付、撤单返还以及交付/恢复结果；手工存取、整理、Cell 插拔等记录只留在后台审计。卖出不得以挂单托管代替成交，业务链需用稳定关联键去重。
- CodeGraph 复核：`CanvasScreen` 的框架影响面约 47 个符号，而直接改造 `TerminalHomeScreen` 会影响约 202 个符号，因此采用组合式双宿主框架。此次仅更新待办与设计文档，没有修改代码、数据库或部署状态。

# 2026-09-04 - 终端 GUI 双宿主框架与资产页原生化

- 抽取无 Minecraft 宿主继承的 `CanvasSceneRuntime`；`CanvasScreen` 与新增 `CanvasContainerScreen` 共同委托根 Panel、Popup、Hover、主题和输入分派。新增普通/Container 两种终端基类，并用 `TerminalShellFrame` 复用状态栏、导航、背景和 `TerminalHomeLayout`。
- 资产导航现在直接打开统一 Container。存储管理在同一会话中显示 27 格 Base Vault、真实 AE2 Cell Bay 与玩家背包；窄屏纵向堆叠、宽屏并排。背包 Shift-click 固定进入 Vault，Cell 仅能手动拖入 Bay，Vault/Bay 可 Shift-click 返回背包。
- 新增同周期 `TerminalRouteCoordinator`，Container 返回或切换普通终端页面时立即恢复缓存终端壳并请求服务端快照，不再先关闭到游戏画面。
- 玩家“操作记录”改为“资产动态”：结构化投影标准市场卖出结算、实际 Vault 买入交付、定制市场交付、撤单返还、失败与恢复；手工存取、整理和 Cell 插拔仍保留后台审计但不显示。新增定制成交按玩家隔离的近期查询，不新增数据库表。
- CodeGraph 前置确认 `CanvasScreen` 影响 47 个符号、`TerminalAssetCenterContainer` 影响 51 个符号、`TerminalHomeScreen` 影响 202 个符号，因此保留 `GuiScene` 合同并采用组合迁移；完成后重新同步索引并复核 `CanvasSceneRuntime`、`CanvasContainerScreen`、资产 Container 与资产动态投影的影响链。
- 自动验证：双宿主/布局/协议/资产动态定向测试通过；Docker Gradle 全量 `test` 为 492 项、52 项既有环境条件跳过、0 失败；Lobby PostgreSQL 上强制执行市场集成 15/15 与仓储集成 4/4，均 0 跳过、0 失败；`assemble` 与 `git diff --check` 通过。
- 部署：同一 runtime JAR 仅同步到 Lobby 与 Prism 客户端，构建产物和两处目标 SHA-256 均为 `472c675afb3b987da5484ad47420a3262eb23c8b0613e50f2272e07d3e95e717`。Lobby 达到 `Done (1.312s)`；客户端未启动，部署目标不包含 S1、S2。本批没有新增或修改资产数据库结构。

# 2026-09-05 - 资产中心单 Cell 内容浏览与左右分区

- 存储管理调整为明确的左右结构：左侧同时显示 27 格 Base Vault 与玩家背包，右侧始终保留一个真实 Cell Bay；Cell 未插入时不显示空内容网格，插入后直接显示该 Cell 的分页内容。
- 新增服务端 Cell 内容快照与有限存取动作。每种物品按虚拟条目显示真实 `long` 数量；左键提取一组、右键提取一个，手持物品经明确按钮存入。容量、类型与物品准入继续由 AE2 Cell inventory 和现有物品策略决定，不建立外部 AE 网络或第二套库存。
- Cell 写操作复用 Bay 的 PostgreSQL 产权行、版本和操作审计：请求使用幂等 `requestId`，同时校验所有者、Bay 版本和当前服务器侧光标；客户端不能提交玩家身份或任意存取数量，提取物品标识也只作不可信提示并由当前 Cell 重新匹配。协议保留旧资产动态快照和旧请求的兼容解码。
- 本轮只完成资产中心功能与布局，现代化平滑主题暂不实施，等待实机核对后再单独设计。
- 验证：CodeGraph 完成变更后同步并复核资产 GUI、Bay 服务、快照协议和受影响测试链；仓储/协议/布局定向测试、仓储 PostgreSQL 4 项集成测试、全量 `test`（496 项，52 项既有环境条件跳过，0 失败）、`assemble` 与 `git diff --check` 均通过。
- 部署：runtime JAR、Lobby 与 Prism 客户端三处 SHA-256 均为 `edce03d59ac46d70903f9962dc55ff87bd9674dbf7170d0ee0b1d144d7a3223c`。Lobby 达到 `Done (1.281s)` 并注册统一资产中心 Container；客户端未启动，部署目标不包含 S1、S2。本轮没有新增或修改数据库结构。

# 2026-09-05 - Galaxy UI 2 现代 Java 框架决策

- 新增独立设计文档 `galaxy-ui-2-modern-java-framework.md`，确认采用“Vue 式架构、Minecraft 原生渲染”：借鉴组件、Props、Event、Slot、响应式派生状态和单向数据流，不嵌入 JavaScript、DOM、CSS 或 Chromium，也不机械翻译依赖浏览器运行时的 Vue 源码。
- 明确 Forge/Minecraft 只提供 Screen/Container/Slot、输入回调、纹理、字体和 OpenGL/Tessellator 等底层能力；当前 `Canvas*`、Panel、Theme 和输入调度均是项目自建层，下一代必须自行实现约束布局、DrawList、抗锯齿字体、设计 Token、焦点/模态和现代组件。
- CodeGraph 复核当前 `GuiPanel` 影响约 558 个符号、`TerminalShellFrame` 约 255 个、`CanvasSceneRuntime` 约 98 个。最终仍会铲除旧框架，但采用独立 UI2 内核、UI Lab、双宿主、资产中心纵向切换、全页面迁移、最后删除旧框架的替换顺序；Git 提交只解决回滚，不替代迁移期间的可编译基线和行为对照。
- 本轮只记录架构决策和实施路线，没有修改客户端代码、业务合同、数据库或部署状态。

# 2026-09-06 - Galaxy UI 2 C.0-C.2 生产组件与资产中心正式迁移

- C.0：新增 `ComponentUiDocument`、`UiWidgetRegistry`、`UiWidgetAdapter`、`UiActionHandler` 与生产
  `StandardWidgets`；组件绘制先逐组件采集，再按层级稳定合成，解决原生物品层与后续兄弟内容层冲突。
  新增共享 `TerminalAppShell`、普通/Container UI2 终端基类和 `NativeItemBridge`。F8 UI Lab 已迁移到同一生产
  组件实现，不再维护手写 DrawList/裸坐标控件副本。
- C.1：`GuiTerminalAssetCenter` 已切换为 UI2 Container 宿主并新增
  `AssetCenterUiState/Action/Reducer/Effect/Document`。页面保持左侧 Vault 与背包、右侧 Bay 与可选 Cell 内容、
  第二页签系统资产动态；服务端 Container、快照、版本、幂等、准入、失败回滚和无闪烁路由合同未改。旧
  `AssetContentPanel` 以及资产专用旧布局类/测试已删除。
- C.2：三档布局 `350×193`、`427×240`、`620×340`、Modal 阻断、稳定 external region、Cell 中文反馈、
  Vault/Bay/资产动态/协议与双宿主定向回归通过。全量自动测试共 545 项，52 项按既有环境条件跳过，0 失败；
  其中根项目 513 项、UI2 Core 26 项、Lab 2 项、Demo 4 项。Warehouse PostgreSQL 临时 schema 集成测试
  4/4 通过并清理。
- CodeGraph：变更前后均执行 `status/sync`；`GuiTerminalAssetCenter` 影响链为快照消息与 `ClientProxy` 两个外部
  入口，`ComponentUiDocument` 影响资产文档、UI Lab 和组件测试，`TerminalAppShell` 影响两类终端宿主与资产页。
  `affected` 对跨子项目新文件未返回测试，故按影响链人工选择并执行上述定向与全量矩阵。
- 构建：`:ui2-core:test`、`:ui2-lab:test`、`:ui2-demo:test`、`:ui2-demo:renderGolden`、根 `test`、
  `assemble`、`verifyUi2SelfContained` 与 `git diff --check` 均通过。最终 JAR 自包含 UI2 Core/Lab/适配层和许可，
  不含 Qz 运行时引用。
- 部署：同一 JAR 仅同步到 Lobby 与 Prism 客户端，构建产物和两处目标 SHA-256 均为
  `8ea74527024712a2ddba8effe527771d8c7064ebb55a9fe1c9c21d66d502e45c`。Lobby 达到 `Done (1.432s)`；客户端
  未启动，数据库 migration 被显式跳过，S1/S2 不在目标中。
- 当前门槛：C.0/C.1/C.2 自动验收完成，等待玩家人工验证资产中心真实拖放、Shift-click、数字键、双击、Cell
  浏览/存取、Tooltip、Modal 阻断、中文字体和返回路由；通过前不进入 D 阶段。

# 2026-09-06 - Galaxy UI 2 全终端统一路由与业务页面迁移

- 修复首页在高 GUI Scale 下的文字布局：所有 Label 使用主题字号参与测量、换行、省略和裁剪，首页与只读摘要页按窗口宽度切换单列/双列，不再让文本越过卡片或终端窗口。
- 增加客户端独立外观系统和设置页：`glass_mono`、`hacker_green`、`high_contrast` 三套内置主题，Resource Pack 主题索引，紧凑/标准/舒适密度，小窗/标准/宽大窗口和减少动画均即时保存；不进入网络或服务端数据库。
- 建立稳定 `TerminalApplicationScreen` 和 `TerminalPageRegistry`。首页、职业、公共、通知、物品策略、银行、ServerTools、市场总览、标准市场、订单与交付、定制市场、汇率市场和地产均使用同一个窗口化 UI2 宿主；正式普通页面不再回退 `TerminalHomeScreen`。
- 三类市场继续使用既有服务端 payload、玩家身份注入、订单版本、托管、交付与幂等合同。定制发布和汇率输入在现代页面内选择真实 Base Vault 资产，所有确认动作仍由服务端重新校验。
- 地产页面的页签、我的产权、检查器、市场未启用占位、确认框和路由已迁移。真实地形、产权覆盖、拖拽、滚轮、快捷键和客户端缓存暂通过 UI2 `ExternalSurface` 嵌入已验证地图渲染器；Modal 打开时宿主先阻断地图输入。
- CodeGraph 在各阶段执行 `status/sync/impact`，确认正式路由只剩资产 Container 与统一普通宿主。旧 Canvas 完整删除仍有一个明确前置：将 `TerminalLandMapPanel` 抽为无 Canvas 继承的地图后端；在此之前不伪称旧框架已移除。
- 最终自动验收：根项目 538 项（52 项既有环境条件跳过）、UI2 Core 31 项、UI2 Lab 2 项、UI2 Demo 4 项，全部 0 失败；`:ui2-demo:renderGolden`、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 均通过。当前 runtime JAR 为 `jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `e1651ce1c9730f890e37aeed70a1e454117f87c87e5934f13c1ad0b3caa5bdf4`。
- 地产地图后端已从旧 `AbstractGuiPanel/GuiRect` 解耦，改用 UI2 `UiRect`；正式代码中的旧 `CanvasScreen`、`CanvasContainerScreen`、Theme、Popup、Panel、终端页面工厂和重复输入运行时均已删除。`verifyUi2SelfContained` 增加回归守卫，JAR 若重新包含旧 framework/theme 类会直接失败。
- 删除旧专用视觉测试和无调用遗留工具后重新执行完整矩阵：根项目 454 项（52 项既有环境条件跳过）、UI2 Core 31 项、UI2 Lab 2 项、UI2 Demo 4 项，全部 0 失败；Golden 导出、`assemble`、自包含检查与 `git diff --check` 通过。新 runtime JAR SHA-256 为 `7c2b95eccc5209c8d6e9afd1e93a410346c51ebd42b08b6470933fc4c8873fc5`。
- 2026-09-07 部署：同一 runtime JAR 已同步到 Lobby 与 Prism 客户端，构建产物和两处目标 SHA-256 均为 `7c2b95eccc5209c8d6e9afd1e93a410346c51ebd42b08b6470933fc4c8873fc5`。Lobby 重启后为 `RUNNING`，日志达到 `Done (1.455s)`；客户端未启动。部署器只执行既有迁移历史表的幂等存在性检查，输出中没有应用新的业务 migration；S1、S2 未被部署或操作，保持原状态。下一门槛为玩家完整实机视觉与交互矩阵。

# 2026-09-07 - UI2 固定密度、字体来源与文字边界合同

- 根据实机反馈取消玩家可调界面密度和字号：主题继续固定语义字号、行高、间距和控件高度；设置页只保留主题、窗口尺寸、字体来源、减少动画与恢复默认。
- 新增独立字体来源 `MINECRAFT / TERMINAL`。前者直接复用客户端已加载的 `FontRenderer` 并跟随资源包，作为旧配置和恢复默认后的默认值；后者继续使用内置 Noto Sans CJK 平滑字体。普通 Screen 与 Container 的文本测量和绘制统一使用同一选择。
- 旧 `density` 配置键在兼容加载时移除，不进入服务端 Snapshot、网络或数据库。字体选择仍是纯客户端偏好，与主题包相互独立。
- UI2 公共文本流新增固定主题字号下的单行自动适配：优先原字号，空间不足时有限缩小到可读下限，仍不足则省略；异常宽单字不会逃出边界。所有正式终端渲染增加窗口级硬裁剪兜底。
- CodeGraph 前置同步确认 `TerminalPreferences` 影响约 186 个符号、`TextFlow` 约 103 个、`StandardWidgets` 约 80 个，因此改造集中于偏好、双宿主与公共文字组件，没有逐页添加字号补丁。
- 配置迁移测试覆盖独立字体来源持久化、旧 `density` 清理和缺失字体项默认迁移；纯 JUnit 中显式提供 Forge `Configuration` 所需的临时游戏目录，并在测试结束后恢复全局状态。
- 自动验证：UI2 Core/Lab/Demo 与根项目共 499 项（52 项既有环境条件跳过），0 失败；`:ui2-demo:renderGolden`、根 `test`、`assemble`、`verifyUi2SelfContained` 和 `git diff --check` 全部通过。runtime JAR SHA-256 为 `30380622ace6d44f6a576afb0b3ba9dece57ea6fe1664d90628facd889c2cb24`。本轮尚未部署、未启动客户端、未触碰 Lobby/S1/S2 或数据库。

# 2026-09-07 - UI2 统一 Minecraft 字体与语义字号上限

- 根据最终实机决策移除内置 Noto Sans CJK、字体来源选项、后台字形栅格线程、Atlas、逐帧上传和回退切换；UI2 从测量到绘制只使用 Minecraft 当前 `FontRenderer`，自动跟随语言与资源包。
- 客户端兼容加载会清理旧 `density` 和 `fontSource` 配置。设置页只保留主题、窗口尺寸、减少动画和恢复默认；Base JAR 不再包含 CJK 字体或 OFL 文件。
- 公共 `TextFit` 将每个文本框的语义字号视为上限：短文本保持标题/正文/说明的默认层级，溢出时才在可读下限内缩小；单行仍不足则省略，多行先在限定行数内换行，仍不足再省略。文本框光标使用最终适配字号测量。
- CodeGraph `impact` 显示 `MinecraftFontService` 影响链由原 87 个符号收敛为 26 个，`TextFit` 影响 21 个符号；`affected` 对跨模块公共组件未返回测试，因此执行 UI2 Core/Lab/Demo、设置配置与根项目全量矩阵。
- 干净构建验证共 494 项（52 项既有环境条件跳过）、0 失败；`:ui2-demo:renderGolden`、`test`、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 全部通过。JAR 内无 Noto/字体/OFL 残留。
- 已部署同一 runtime JAR 到 Lobby 与 Prism 客户端，SHA-256 均为 `a75dea120b4799159a12cc6706c00cc900e2ea29de27d2415cd892b3064c80f3`；Lobby 由部署器重启并通过完整启动检查，客户端未启动。数据库只执行既有迁移历史表的幂等存在性检查，无业务 migration；S1/S2 未触碰。

# 2026-09-07 - UI2 字体崩溃修复与标准市场工作台恢复

- 根据两份客户端崩溃报告定位到资源包字体测量返回负宽度后直接进入 `UiSize`；新增饱和测量边界，负值、非法缩放和极大结果均被限制为安全尺寸，绘制对齐宽度同步防御。
- 对照 Git 中旧 `TerminalMarketSection`、`TerminalMarketSectionContent` 及既有市场 UX 合同，将过度简化的标准市场 UI2 页面恢复为三栏工作台：左侧商品浏览，中间行情/盘口/个人资产，右侧交易台。
- 重新接回商品筛选、排序、分页，`1h/24h/7d` 行情周期，三档盘口，可用/锁定/待收/冻结资金，以及订单与交付入口；服务端市场 Snapshot、订单 payload、身份、托管和结算合同未修改。
- CodeGraph 前置确认 `MinecraftFontService` 影响 26 个符号、`TerminalStandardMarketDocument` 影响 91 个符号；测试覆盖字体异常测量及三档正式窗口的标准/定制/汇率市场文本边界。
- 定向测试、根项目全量 `test`、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过。runtime JAR、Lobby 和 Prism 客户端 SHA-256 均为 `557d79f2b2295baab5974e384f5e092e1512828c117fae3818b03bebeb0a4de4`；Lobby 完成重启健康检查，客户端未启动，S1/S2 未触碰。

# 2026-09-07 - 标准市场 UI2 渐进式还原整改

- 实机复核确认临时三栏工作台偏离既有市场交互，现已整体替换为“商品目录 -> 悬浮行情 -> 商品详情 -> 下单/撤单浮层或订单中心”的渐进流程；定制与汇率市场没有重排。
- 商品目录使用真实 `ItemStack`、响应式 2/3/4 列网格、搜索/筛选/排序/服务器分页、迷你走势及悬浮行情；详情页恢复左侧商品/五档盘口/24h/个人资产与右侧主图，底部仅保留买入、卖出、撤单、订单中心。
- 新增 UI2 市场图表几何与渲染：OHLC、稀疏折线、单点、空态、成交量、图内十字光标；全部坐标和 long 运算饱和到图表范围，不恢复旧 Canvas 绘制。
- 下单改为稳定键浮层，恢复市价/限价、数量、`25% / 50% / 最大`、`买一 / 最新 / 卖一`、结算预览、禁用原因和二次确认；撤单使用当前商品可撤委托及版本绑定确认。
- 订单与资产中心恢复六项账户摘要、四页签、商品/方向/状态/时间筛选、清除筛选、真实物品行、服务端分页、撤单及领取动作。宿主新增通用外部物品/图表合同，地产地图同步采用同一合同且行为不变。
- 服务端市场 Snapshot、撮合、银行、托管、Vault、交付、恢复、玩家身份、网络 payload 和 PostgreSQL 均未修改；本轮没有数据库 migration。
- CodeGraph `status/sync` 后确认 `TerminalStandardMarketDocument` 影响 161 个符号，通用 `TerminalPageDocument` 影响 256 个符号并覆盖地产外部画布；`affected` 未返回跨模块测试，因此按调用链执行终端、市场、UI2 与全量矩阵。
- 自动验收共 501 项：根项目 456 项（52 项既有环境条件跳过）、UI2 Core 39 项、Lab 2 项、Demo 4 项，0 失败；Golden 导出、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 全部通过。runtime JAR SHA-256 为 `f469da65463007e0fdfabd0f8ec61fb368cab6a428304dd7ccd12eb192d52bb6`。
- 已将同一 runtime JAR 同步到 Lobby 与 Prism 客户端，两处目标 SHA-256 均为 `f469da65463007e0fdfabd0f8ec61fb368cab6a428304dd7ccd12eb192d52bb6`；Lobby 重启后为 `RUNNING`，日志达到 `Done (1.311s)`。客户端未启动，部署器只幂等确认 migration 历史表，没有业务 migration；S1/S2 未触碰。人工门槛为目录、悬浮行情、详情图表、买卖、撤单和订单中心的实机逐项检查。

# 2026-09-07 - UI2 点击、市场详情与帮助层紧急修复

- 检查用户最新客户端崩溃报告 `crash-2026-09-07_19.38.09-client.txt`，确认直接原因是地产“我的产权”页签和内容根重复使用 `land-mine`，`LayoutEngine` 在渲染期拒绝重复键；现将页签与内容键拆为独立稳定常量并增加回归测试。
- 标准市场商品卡点击失败的原因是事件命中卡片内的被动物品/图表子节点后，Card 只接受 TARGET、不接受 BUBBLE；公共控件现只允许“可点击 Card”消费未被子控件处理的冒泡点击，嵌套按钮仍在 TARGET 阶段优先消费，避免双触发。
- 标准市场帮助层增加明确“关闭”操作，并修正帮助状态误走确认框命中区的问题；点击帮助层或按 Enter/Escape 都只关闭当前帮助层，下一次关闭操作才能退出终端。
- CodeGraph 同步后确认 `TerminalLandDocument` 影响 57 个符号、`StandardWidgets` 影响整个 UI2 控件输入面，因此除地产/市场定向测试外执行 UI2 与根项目全量矩阵。
- 自动验证共 504 项：根项目 458 项（52 项既有环境条件跳过）、UI2 Core 40 项、Lab 2 项、Demo 4 项，0 失败；`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过。runtime JAR SHA-256 为 `118d83ed4efe5647db1d36c7508fe063e0f1daf797b1a51f9c34a004261e295a`。
- 同一修复 JAR 已部署 Lobby 与 Prism 客户端，两处目标哈希一致；Lobby 重启后为 `RUNNING`，日志达到 `Done (1.545s)`。客户端未启动，S1/S2 未触碰，数据库没有业务 migration。

# 2026-09-07 - UI2 Modal 失效边界与市场图表渲染修复

- 实机确认首页帮助层虽然消费了 Escape/点击，却未必显式请求 Runtime 重建，旧 Modal 因而持续缓存并拦截 R、返回和关闭。现将“被消费的 UI 输入”定义为 UI2 Runtime 的统一失效边界；关闭状态在下一帧自动重建，普通 Screen 与 Container 共用此行为，不再依赖每个页面手工调用 `invalidate()`。
- 标准市场图表此前在 `RenderItem` 之后直接使用 Minecraft 固定管线绘制，可能继承物品渲染留下的颜色、光照和混合状态，实机表现为整块灰色。现调整为先绘制页面外部图表、后绘制真实物品，并用独立 `GlStateGuard` 隔离两段状态。
- 详情图增加明确周期标题；真实成交不足时保留网格和参考价格线，并显示“暂无成交数据 · 参考价”，避免无成交商品呈现为疑似故障的空白块。服务端行情、撮合和数据库合同未改。
- CodeGraph 同步后复核 `UiRuntime`（71 个受影响符号）和 `MarketChartRenderer`（117 个受影响符号），测试范围覆盖 UI2 Runtime、首页 Modal、标准市场文档与全部终端/业务回归。
- 自动验证共 504 项（52 项既有环境条件跳过），0 失败；`:ui2-core:test`、`:ui2-lab:test`、`:ui2-demo:test`、Golden 导出、根项目 `test`、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 全部通过。runtime JAR SHA-256 为 `1b7dea024df84fe5527d574617c68d01f28949d52a9c7fe71d1ae4794142295e`。
- 同一 JAR 已部署到 Lobby 与 Prism 客户端，构建产物及两处目标哈希一致；Lobby 由部署器完成重启和启动日志健康检查，客户端未启动。部署器仅幂等确认既有 migration 历史表；S1/S2 未触碰。
- 二次实机反馈确认前述隔离只恢复了宿主状态，却没有为外部二维画布建立确定状态；图表实际被恢复后的深度测试遮挡，只露出底层 Card。现于图表阶段显式关闭深度测试/写入与光照、启用标准 Alpha 混合并重置颜色。定向测试、`assemble`、自包含检查与差异检查通过；重新部署后的构建/Lobby/客户端 SHA-256 均为 `581597c3e38afdda0874b6287ad7a44675ec32c4529cf9691055588782a90459`。

# 2026-09-07 - Victoria 3 经济 Mod 源码研究基线

- 通过 GitHub SSH 将 Anbeeld's Stockpile Economy 与 Economic and Financial Mod 拉取到仓库外的 `reference-sources/victoria3-economy/`；固定提交分别为 `c7a7e0cd5c1646b9a449854d0cefb27a7ef8ea9b` 与 `48c3f7014dec0dab86899cca64fce1c06a10e4a1`，不会进入 Base 构建、JAR 或部署。
- 新增 `docs/victoria3-economy-mod-research-2026-09-07.md`，记录 ASE 的目标库存、双阈值滞回、扣除自身订单、批处理和限幅设计，以及 E&F 的货币、银行、利率、债券、股票、通胀、流动性、信用与危机系统。
- 许可证边界：ASE 根目录为 MIT；E&F 当前提交和历史检查未发现许可证文件，因此只研究思想与行为，不复制其代码、数据、GUI 或美术资源。
- 初步建议先执行只读经济事实盘点与历史数据离线计算，再做经济观测页和公共储备回放；未经单独批准不修改市场、银行、Vault、数据库或生产服务器。
# 2026-09-08 - Galaxy UI2 正式终端纯 Java视觉闭环 V.1/V.2 与 V.3 基础

- 新增独立 Java 8 `ui2-terminal` 子模块，依赖方向固定为 `ui2-terminal -> ui2-core`；根 Mod 与 Java2D Demo 均消费同一终端壳和正式 Document。纯模块新增 `TerminalVisualModel`、`VisualItem`、`ExternalVisualRegion`、动作端口、窗口度量与确定性场景，不引用 Minecraft、Forge、LWJGL 或 AE2。
- 首页、设置和资产中心已下沉为平台无关 Document；根 Mod 只负责把现有服务端快照、客户端偏好、`ItemStack`、光标栈及操作动作转换到纯模型。资产中心仍复用原 Container、Vault、背包、Bay、Cell、幂等和版本合同。
- Java2D 报告已从 15 张 UI Lab 扩展到 63 张正式终端截图，覆盖三套主题和 `350×193 / 427×240 / 620×340`。视觉门禁实际发现并修复了资产内容区整体 0 高度、市场商品价格行 0 高度以及物品数量文字行高不足三类布局缺陷，并新增“挂载内容塌缩为 0”失败规则。
- V.3 已建立纯标准市场目录/详情模型和平台无关 `CHART_LINE / CHART_VOLUME` DrawCommand；Java2D 与 Minecraft 渲染后端均支持同一命令。正常、稀疏、单点和空行情场景进入报告，有数据却无图形及空态缺失会使 `verifyTerminalVisuals` 失败。正式下单/撤单/订单中心适配尚未迁移，因此 V.3 保持实施中。
- `scripts/render-ui2-visuals.sh` 现在执行 Core、Terminal、Lab、Demo 测试与 `:ui2-demo:verifyTerminalVisuals`，统一报告仍为 `ui2-demo/build/ui-lab/index.html`。本记录写入时定向视觉矩阵通过：63 张正式终端快照及 15 张 Lab 快照，0 个结构告警。
- 自动验收：根项目 458 项（52 项既有条件跳过）、UI2 Core 41 项、Terminal 1 项、Lab 2 项、Demo 5 项，全部 0 failure/0 error；`test`、`assemble`、`verifyUi2SelfContained`、`verifyTerminalVisuals` 与 `git diff --check` 均通过。运行 JAR SHA-256 为 `9062bbe4d41e4a72a952b569e89e5e512134d948c769a4063cd37ea3c5e6213a`。按阶段约束本轮未部署，未启动客户端，未触碰 Lobby/S1/S2 或数据库。

# 2026-09-08 - 跨服任务平台 Q.2 玩家进度结构化导入

- `galaxy-quest-bq-importer` 将 BQ 玩家进度从不透明 Task JSON 提升为结构化 Task 进度：稳定 task key、类型、
  当前玩家完成标记和数值列表；兼容现网 `data` 整数数组与击杀任务 `value` 标量，同时继续保留完整原始 JSON。
- 增加 9 种现网 Task 与 4 种 Reward 的固定兼容矩阵测试，以及只读 `progressInventory` 工具。S2 实跑读取
  29 个玩家文件、108557 个任务进度、4524 个已完成、1279 个已领取；所有类型均在兼容矩阵内。
- S2 DefaultQuests 再次全量导入 3744 个任务，unknown Task/Reward 集合均为空。现场文件只读挂载，无写回、
  无数据库 migration、无部署；S1 未操作。
- 移除定义导入阶段错误的统一 `target=1` 占位，改为任务类型对应的规范参数；检索、合成、流体和方块破坏
  使用逐项向量目标，击杀、位置、会面、选择奖励与 XP 保留源码中的关键语义。兼容报告现明确输出
  `ADAPTED/UNKNOWN`，不会把“能导入”误报成“运行时已完整支持”。
- `galaxy-quest-core` 的 `TaskProgress` 已升级为向量兼容模型，旧标量构造器保持可用；逐项进度单调合并、
  全项完成判断及形状冲突失败均有测试。新增 `BqProgressMapper` 将旧进度按定义形状映射到核心模型，拒绝
  task 类型、数量或向量宽度漂移，避免跨服迁移静默损坏。
- CodeGraph 复核 `TaskProgress` 影响 43 个符号、`BqStandardCodec` 影响 35 个符号、`BqProgressMapper`
  影响 7 个符号；导入器定向测试、Core 测试、根项目全量 `test`、`assemble` 与 `git diff --check` 通过。
  新构建 JAR SHA-256 为 `bf085903b7a0967fcae9645b352868d5de87030dc625ec1a8680d0089e53fe6c`；本轮未部署。

# 2026-09-09 - 跨服任务平台 Q.3 PostgreSQL 权威运行时第一批

- 新增纯 Java 8 `galaxy-quest-postgres` 模块。第一版 PostgreSQL DDL 包含版本化任务定义、唯一事实收件箱、
  PLAYER/PARTY/TEAM/PUBLIC 进度、BIGINT 数组 Task 进度和唯一奖励资格；尚未接入生产 migration。
- 新增 JDBC 连接/事务边界和 `JdbcQuestRuntimeRepository`。事实、进度和奖励资格变更强制要求活动事务；
  进度读取使用参与主体+任务版本的事务 advisory lock，覆盖“记录尚不存在”时的并发首次更新。
- 核心新增 `QuestRuntimeService`，把事实去重、进度锁定、事实投影、任务归约和奖励资格生成固定在一个事务
  工作流中，重复事实不会再次运行任何任务。
- 使用独立临时 PostgreSQL 数据库和角色完成 8 项仓储/事务测试，其中 4 项为真库测试：相同事实并发、
  不同事实并发首次更新、向量进度往返、奖励资格幂等及强制故障整体回滚全部通过。临时数据库和角色已删除；
  现有数据库用户、业务 schema 和数据未修改。
- CodeGraph 复核 `QuestRuntimeService` 影响 19 个符号、`JdbcQuestRuntimeRepository` 影响 28 个、
  `JdbcQuestTransaction` 影响 17 个。Quest Core、PostgreSQL 模块、根项目全量 `test`、`assemble` 和
  `git diff --check` 均通过；新 JAR SHA-256 为
  `49cae7ad0108e326a2e5c0818154f7294b722c01236175c31e3a68d2aaa5245e`，未部署。

# 2026-09-09 - 跨服任务平台 Q.3 定义发布、奖励恢复与双源冲突预演

- 新增任务定义草稿、内容哈希乐观锁、发布和退役仓储；发布版本不可原地修改，运行时只读取最新发布版本。
- 奖励资格增加租约、尝试和恢复状态机：多工作器通过 `FOR UPDATE SKIP LOCKED` 领取，失败可延迟重试，
  达到上限进入 `ABANDONED`，租约过期可被其他工作器接管，旧工作器确认会被拒绝。外部交付端合同强制
  使用稳定 entitlement key 实现幂等。
- 新增纯 Java 工作器测试，覆盖成功确认、稳定外部键、可重试失败、最大次数放弃和陈旧确认；一次性真
  PostgreSQL 库覆盖草稿冲突、发布不可变、失败重试、租约接管和尝试审计。测试结束后临时库与角色已删除，
  生产数据库和 migration 未修改。
- 新增 BetterQuesting 双源只读冲突分析工具，按单调偏序区分一致、单边存在、左/右领先和真正分叉；向量
  交叉领先、Task 类型或形状漂移不会被静默合并。因 S1 操作需单独授权，本轮未读取 S1 现场数据。
- CodeGraph 前置同步并复核 `JdbcQuestDefinitionRepository`、`JdbcRewardDeliveryRepository` 与
  `RewardDeliveryWorker` 的影响链，改动保持在 quest-core、quest-postgres 和 quest-bq-importer 新模块内。
  三个模块共 36 项测试通过，根项目全量 `test`、`assemble` 与 `git diff --check` 通过；runtime JAR
  SHA-256 为 `42c636444185f0779aa1a5710cfdabf1254b36894d917a559ae58dc4a42121a4`。尚未接入 Mod 启动、
  未部署、未触碰 Lobby/S1/S2。

# 2026-09-09 - 跨服任务平台 Q.4 第一批事件检测器

- 对照 BetterQuesting `EventHandler`、`TaskHunt`、`TaskCrafting` 和 `TaskCheckbox`，采用其成熟的事件/周期
  检测分工，但将调用方向改为 Forge 事件生成不可变事实，禁止事件处理器遍历任务或直接修改进度。
- 新增击杀、合成、熔炼、铁砧事实工厂与服务器事件处理器，过滤客户端世界、FakePlayer 和取消事件；保留
  BQ 对 Shift-click 合成空栈重新求配方数量以及熔炼异常数量的兼容修正。每次服务进程使用启动 ID 加单调
  序号生成唯一事件键，重试继续携带同一个不可变事实。
- 纯核心新增击杀父类型/伤害源求值、合成物品向量求值和服务端绑定复选确认求值；导入的 hunt、crafting、
  checkbox 定义获得稳定 `galaxy:*` fact type。注册名、meta、通配 meta、OreDict 以及三种生产通道均有
  明确合同；NBT 字段已保留但部分匹配尚未宣告完成。
- 当前 Handler 尚未注册，避免在发布定义目录、参与主体解析和生产 DDL 未接线前丢弃事实；没有生产
  migration、部署或服务器操作。CodeGraph 复核 `ItemVectorTaskEvaluator`（21 个影响符号）与
  `QuestGameplayEventHandler`（12 个影响符号）；Quest 模块当前 40 项测试为 0 failure/0 error，其中默认
  环境下 7 项真 PostgreSQL 测试按连接条件跳过（本轮前一批已用一次性真库通过）。根项目 `test`、
  `assemble` 和 `git diff --check` 通过，runtime JAR SHA-256 为
  `6c7978dd7245d44d04b535a5a65e4fbca9249bcf98bf7a77fbddf00e555d8b86`。

# 2026-09-09 - 跨服任务平台 Q.4 发布计划、方块与库存快照

- 定义仓储新增最新发布版本目录，进度查询新增参与主体的已完成前置集合；`QuestEvaluationPlanner` 在
  `QuestRuntimeService` 的同一事务内生成执行计划，先按 fact type 剔除无关任务，再通过显式 Assignment
  Policy 选择 PLAYER/PARTY/TEAM/PUBLIC 范围。当前导入任务默认 PLAYER-only。
- 无相关任务的事件现在返回 `IGNORED` 且不写事实表；定义、成员和前置状态不会在事务外形成陈旧快照。
- 新增方块破坏向量求值和事实采集，保持 BQ“一次破坏只命中第一个需求”的算法，支持直接方块、meta
  通配和 OreDict。
- 新增非消费型库存权威快照：未完成的观察进度允许回落，完成后锁定；同一栈按需求顺序分配，禁止重复
  计数。`consume=true` 的检索不会走快照捷径，后续使用事务提交协议。
- 使用一次性 PostgreSQL 数据库验证最新发布目录和完成前置查询，全部测试通过后临时库与角色已删除；
  生产数据库、migration、Mod 注册和部署均未变更。CodeGraph 复核 `QuestEvaluationPlanner`（15 个影响
  符号）、`InventorySnapshotTaskEvaluator`（20 个）与 `MinecraftQuestFactFactory`（24 个）；Quest 三模块
  共 47 项测试全部执行且 0 failure/0 error/0 skipped，根项目 `test`、`assemble` 与 `git diff --check`
  通过。runtime JAR SHA-256 为 `cb1587546e75ac3a3e289af48ed932795758d465a62653ed2318425b9a06c1d5`。

# 2026-09-09 - 跨服任务平台 Q.4 流体与地点权威观察

- 新增非消费型流体库存快照求值器及 Minecraft 容器扫描，按毫桶和需求顺序分配，避免同一容器内容被多个
  条件重复计数；`consume=true` 明确保持不推进，等待事务型扣除协议。
- 新增地点求值器和每 100 tick 的基础位置事实，对齐 BQ 的维度、三维欧氏/曼哈顿距离、范围、生物群系与
  invert 语义。结构与目标可见性必须携带显式服务端证据；缺失证据按 unknown 处理，不会误判为 false，
  因而 inverted 任务也不会误完成。
- BQ 导入器为 fluid 与 location 写入稳定 fact type；兼容注册表现在覆盖现网 9 类 Task 中的 8 类，尚缺
  meeting。消费型 retrieval/fluid、部分 NBT、结构/视线探针和奖励交付仍保持待实现状态。
- Handler 仍未注册，生产数据库、migration、Mod 生命周期和服务器均未修改。CodeGraph 同步覆盖流体、地点、
  Fact Factory 和 Handler 调用链；Quest 三模块共 49 项测试，0 failure/0 error，其中默认环境下 7 项真库
  测试按连接条件跳过。根项目全量 `test`、`assemble` 与 `git diff --check` 通过，runtime JAR SHA-256 为
  `f9b95d51d842a579b86e5d175e2651c6e88c1acbb02cf504864eb39fcd360d27`。本批未部署。

# 2026-09-09 - 跨服任务平台 Q.4 会面检测与周期探针计划

- 新增 meeting 求值器，支持精确实体、子类型别名、所需数量与可选 NBT；附近实体事实携带完整扫描半径，
  小半径快照无法完成大范围任务，避免把未观察区域错误当成空区域。
- 新增 `QuestObservationPlan`，从发布定义汇总库存、流体、地点和 meeting 的周期观察需求，并取全部会面任务
  的最大半径。Handler 按 BQ 的 20/60/100 tick 周期只生成计划要求的事实，不再无条件扫描和写库。
- BQ 导入器现为当前任务库全部 9 种 Task 写入稳定 fact type，核心注册表也覆盖全部 9 类。这里的“覆盖”
  不等于已经生产启用：消费型任务、BQ 部分 NBT、地点结构/视线探针和奖励交付仍需完成。
- 事件 Handler 依旧未注册，生产 migration、数据库和服务器均未操作。CodeGraph 复核 meeting、周期计划、
  Fact Factory 与 Handler 影响链；Quest 三模块共 51 项测试，0 failure/0 error，默认环境下 7 项真库测试按
  连接条件跳过。根项目 `test`、`assemble` 与 `git diff --check` 通过；runtime JAR SHA-256 为
  `455e37c673a79427940ca2e1f0f892252b2a2c1516f623814d37e35ffaa1ab85`。本轮未部署。

# 2026-09-09 - 跨服任务平台 Q.4 消费型任务 Saga 第一批

- 对照 BQ retrieval/fluid 的检测、手工提交与直接扣除逻辑，新增跨服安全的消费协议：稳定提交键、
  `PREPARED/APPLIED/CONFIRMED/REJECTED` 状态、幂等外部扣除端和确认事实。只有外部扣除返回持久证据后，
  consuming retrieval/fluid 才按逐项增量推进；非消费快照与消费确认通过组合 evaluator 共存。
- 消费事实绑定玩家、Quest UUID、定义版本、Task key 和 item/fluid 种类；Planner 精确验证这些边界，无法
  匹配发布任务时不会把已扣资源静默确认为成功。周期计划也不再为 consume=true 的任务生成无意义快照。
- PostgreSQL DDL 新增消费提交表与恢复索引，JDBC 仓储拒绝同一提交键对应不同内容，并校验合法状态迁移。
  使用一次性独立数据库/角色执行全部真 PostgreSQL 集成测试，覆盖准备幂等、应用、确认、重复确认与内容
  碰撞；测试结束后临时数据库和角色均已删除，现有业务 schema 未修改。
- 尚未实现 Minecraft 玩家 NBT 的幂等扣除端，因此 Handler/消费入口仍不注册；无生产 migration、部署或
  服务器改动。
- 后续补齐服务端规范请求授权器：外部意图不含资源或数量，授权器从最新发布定义、锁定玩家进度、前置任务
  和重复周期生成规范请求；锁定/完成、非消费、错误 Quest/Task 均拒绝。任意请求直提入口已移除，恢复只能
  按数据库既有提交键执行。
- CodeGraph 复核 `ConsumptionSubmissionService`、`ConsumptionRequestAuthorizer`、Planner 与 JDBC 提交仓储
  影响链。Quest 三模块默认矩阵共 56 项，0 failure/0 error，其中 8 项真库测试在默认无连接环境下跳过；
  另使用一次性数据库实际执行全部 8 项 PostgreSQL 集成测试并通过，临时库/角色已删除。根项目 `test`、
  `assemble`、`git diff --check` 通过，runtime JAR SHA-256 为
  `c86229a97680bbf927312dcfdb4cd50597aa9aa60ec158640d6892729610b532`。未部署。

# 2026-09-10 - 跨服任务平台 Q.4 玩家物品幂等消费端

- 新增 Minecraft 服务端物品消费适配：只按服务端 UUID 查找在线玩家，客户端仍不能提交物品定义、数量或
  目标玩家。扣除前先为所有需求生成完整分配计划，同一物品栈不会重复满足多个需求；任何需求不足时背包
  保持不变。
- 物品匹配覆盖注册名、耐久/通配 meta、OreDict，并按 BetterQuesting MIT 源码语义重写完整/部分 NBT
  比较，包括嵌套 Compound、无序 List、跨整数类型数值和带重复控制的 byte/int 数组。
- 每个 submission key 以 SHA-256 标记写入玩家 persisted NBT；背包变更与标记经世界玩家数据接口同步写盘。
  重试命中标记时直接返回既有成功证据，持久化抛错则恢复完整背包快照并撤销标记，Saga 不会进入 APPLIED。
- 新增可注入的在线玩家解析与玩家数据 flush 边界，以及默认服务器实现；未注册 Handler、消费入口或 Mod
  生命周期。本批尚不处理 fluid 容器消费，生产 migration、数据库和服务器均未改动，也未部署。
- CodeGraph 前置复核 `ConsumptionPort`（27 个影响符号）与 `ConsumptionSubmissionService`（43 个）；
  Minecraft 适配编译通过，新增 NBT 语义测试及根项目测试通过。全量测试中的 `ui2-demo` 两项 Java2D 导出
  因容器字体环境失败，排除该已知视觉环境项后根 `:test` 通过；该失败与本批 Quest 代码无调用关系。
  `assemble` 与 `git diff --check` 通过，runtime JAR SHA-256 为
  `a0997dbfb8f81278f4425ef5fd79df043f9f72d23af0a208514ab987129fa1cf`。

# 2026-09-10 - 跨服任务平台 Q.4 流体容器幂等消费端

- `MinecraftPlayerConsumptionPort` 现同时支持 item 与 fluid。流体需求在完整背包副本上依次分配，兼容 Forge
  固定流体容器和 `IFluidContainerItem` 可变容器，并按流体注册名及可选 NBT 匹配；部分排空容器和空容器
  会进入规划后的背包状态，输入背包在规划阶段保持不变。
- 满背包无法接纳返回容器时整次提交拒绝且不扣流体。此处有意不采用 BQ 的掉落空容器行为，因为世界实体
  无法与玩家 persisted NBT 的幂等标记一起回滚；玩家整理出空间后可以使用新的 submission key 重试。
- Forge 流体操作下沉到 `QuestFluidContainerAdapter`，生产实现调用 Forge 1.7.10，测试使用确定性 fake，
  解决无完整 Minecraft Bootstrap 时 `FluidRegistry` 不能初始化的问题。新增测试覆盖 1500 mB 跨容器部分
  排空、返回容器数量守恒、输入不变以及满背包拒绝。
- 根 `:test`（排除既有 `ui2-demo` 字体环境项）通过：464 项、52 项条件跳过、0 失败。Handler、生产
  migration、数据库和服务器未改动，未部署。`assemble` 与 `git diff --check` 通过，runtime JAR SHA-256
  为 `d4324f1202bca016dc13cfe7df20edd9ce03bdc1bd6cc988bd94d2838733fd43`。

# 2026-09-10 - 跨服任务平台 Q.3 奖励类型路由与选择门禁

- 新增纯 Java `RewardDeliveryHandler/RewardDeliveryRouter`：按完整 Reward type ID 精确路由，重复 handler
  注册立即失败，未知类型返回不可重试失败，防止 worker 把尚未支持的奖励静默标记为已领取。
- 新增 PostgreSQL 选择奖励表与 `JdbcRewardChoiceSelectionRepository`。选择只接受服务端会话提供的当前玩家
  UUID，校验 entitlement 所有权、`bq_standard:choice` 类型、可交付状态和 `item.count` 索引范围；首次选择
  不可变，相同选择幂等，不同选择报告冲突。
- Delivery lease 查询会跳过尚未选择的 choice entitlement；选择后才允许租赁，并把数据库中的选择索引作为
  `choice.index` 注入不可变 Reward 参数。客户端提交的 Reward 参数不会进入该链路。
- 使用一次性数据库/角色 `jgb_quest_choice_it` 实际执行全部 PostgreSQL 集成测试，覆盖未选择不可租赁、跨玩家
  拒绝、越界拒绝、同值幂等、改选冲突及租约参数；测试通过后数据库和角色均删除并核验不存在。生产 schema、
  migration、服务器和 Handler 未修改，未部署。Quest Core 36 项、Importer 11 项、PostgreSQL 13 项真库测试
  及根项目 464 项（52 项既有条件跳过）均为 0 失败；`assemble`、`git diff --check` 通过。runtime JAR
  SHA-256 为 `47968f4089b090e234cdc303c7602cb2f1dd0d97a7b638f9910df1be7e6dd367`。

# 2026-09-10 - 跨服任务平台 Q.4 物品、选择与经验奖励交付器

- 对照 BetterQuesting `RewardItem/RewardChoice/RewardXP/XPHelper/NBTReplaceUtil` 源码，实现 PLAYER 范围的
  `bq_standard:item`、`bq_standard:choice` 和 `bq_standard:xp` handler。选择索引只取数据库租约注入值；
  物品 NBT 支持递归 `VAR_NAME/VAR_UUID` 替换；XP 等级曲线与 BQ 的三个区间保持一致。
- 交付前在背包副本中完成全部堆叠规划。背包不足返回可重试失败，不像 BQ 那样生成世界掉落，从而保证
  entitlement 标记、背包或经验变化可由同一次玩家数据持久化覆盖；保存异常恢复玩家状态和标记。
- CodeGraph 前置复核 `RewardDeliveryRouter`（10 个影响符号）及 `MinecraftPlayerConsumptionPort`（42 个）；
  改动后同步新增 5 个文件、修改 1 个文件。新增确定性测试覆盖堆叠、满背包全量拒绝、NBT 替换、XP 边界与
  标记稳定性，连同消费端定向测试共 10 项通过。Quest Core 36 项、Importer 11 项、PostgreSQL 13 项（默认
  环境 9 项真库条件跳过）及根项目 469 项（52 项条件跳过）均为 0 失败；`assemble`、
  `verifyUi2SelfContained` 与 `git diff --check` 通过，runtime JAR SHA-256 为
  `a54b034adcd9feb9434b9dc1931ed65120443074edc8547f3fb8620633f1a0d1`。运行时注册、生产 migration、服务器
  和数据库均未修改，本批不部署；quest-completion 与非 PLAYER 奖励策略留待下一批。

# 2026-09-10 - 跨服任务平台 Q.4 任务完成奖励事务化

- 对照 BetterQuesting `RewardQuestCompletion`，新增纯核心 `QuestCompletionRewardPort` 和对应 delivery
  handler；目标 UUID 只取已发布 Reward 定义参数，非法 UUID 与未发布目标为永久失败。
- PostgreSQL 实现把目标任务进度强制完成和目标奖励资格创建置于同一事务。目标已完成作为幂等成功；目标
  Task 各分量写至定义目标值，使进度快照与完成状态一致。该语义保留 BQ 不检查目标前置的行为，同时利用
  参与主体进度锁、稳定资格键和通用租约恢复消除跨服重复完成/重复发奖。
- CodeGraph 复核 `QuestRuntimeService`（36 个影响符号）、新 handler（12 个）和 JDBC port（11 个）。Core
  39 项测试通过；使用一次性数据库/角色实际执行 PostgreSQL 14 项测试，0 failure/0 error/0 skipped，验证
  完成时间不被重试覆盖、Task 完成及奖励只产生一次。测试库和角色随后删除并核验均为 0。生产 schema、
  Mod 生命周期和服务器未修改，本批不部署。

# 2026-09-10 - 跨服任务平台 Q.4 扩展到完整 BQ Standard 重要类型

- 源码复核发现，现网 9 Task/4 Reward 之外，BQ Standard 还提供实体交互、物品/方块交互、计分板、XP Task，
  以及命令、计分板 Reward。导入兼容集合扩展为 13 Task/6 Reward，避免未来内容或编辑器创建这些类型时被
  错误归为 unknown。
- 新增稳定 fact type 与参数归一化：交互任务保留次数和事件类型；计分板把可能为负的比较阈值单独保存为
  `scoreTarget`，TaskProgress 使用布尔完成目标，避免破坏非负进度不变量；XP 等级阈值按 BQ 曲线转为经验点。
  所有原始配置继续保存在 `bq.raw`，当前未实现的运行适配仍明确为 ADAPTED。
- CodeGraph 复核 `BqStandardCodec`（35 个影响符号）。Importer 12、Core 39、PostgreSQL 14 项以及根项目
  469 项（52 项条件跳过）均 0 失败；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。runtime
  JAR SHA-256 为 `8d6ee70d5bc254b976e2a0072d4b2f25f5cc225ae31279c703c71aeebe022086`。未部署。

# 2026-09-10 - 跨服任务平台 Q.4 计分板、XP 与交互检测

- 新增计分板和非消费 XP evaluator：计分板支持 BQ 六种比较操作和负阈值，完成后锁定；XP 使用绝对经验点
  快照，未完成值允许下降，消费型 XP 不走观察捷径。
- `QuestObservationPlan` 增加发布定义感知的 XP 与计分板目标；Minecraft Handler 按 BQ 的 20/60 tick 周期
  采集服务端权威值。缺失 objective 按定义 criteria 创建，避免每个 Task 自行扫描。
- 新增方块左/右键和实体攻击/交互事实，基础求值覆盖动作、实体父类型、手持物、方块、meta 与 OreDict。
  有 NBT 限制的交互目前 fail-closed，等待 canonical NBT 比较，绝不误推进。Handler 仍未注册。
- CodeGraph 影响范围：`QuestObservationPlan` 29、`InteractionTaskEvaluator` 36、
  `QuestGameplayEventHandler` 19 个符号。Core 43、Importer 12、PostgreSQL 14 项（默认环境 10 项条件跳过）
  以及根项目 469 项（52 项条件跳过）均 0 失败；`assemble`、`verifyUi2SelfContained`、`git diff --check`
  通过。runtime JAR SHA-256 为 `b7966f336cd12662fb9c5efdcaa81318d5eceeafd87acf8c783af88749496cfc`。
  未部署，也未修改生产配置或数据库。

# 2026-09-10 - 跨服任务平台 Q.4 跨模块 canonical NBT 匹配

- 新增纯 Java `PortableNbt/PortableNbtMatcher`：确定性编码保留 Compound、List、String、Number、byte[] 与
  int[] 类型，Compound 键排序，数值跨原 NBT 宽度归一；部分 Compound 和无序集合匹配均禁止一个实际元素
  被重复使用，畸形编码 fail-closed。
- BQ Importer 将带类型后缀的 NBT-JSON 同时写入 `*.nbtPortable`，Minecraft 事实工厂则从真实 `NBTBase`
  生成相同编码；原始字段继续保留用于奖励重建与审计。物品生产、库存、流体、方块、击杀、会面和实体/
  方块交互求值现共用该合同，hunt 和 interact_entity 的目标实体 NBT 采用 BQ 的部分匹配语义。
- CodeGraph 前置同步后复核 `PortableNbt`（49 个影响符号）、`InteractionTaskEvaluator`（36 个）；新增核心、
  导入和 Minecraft 适配测试。Quest Core 与 Importer 共 60 项、根项目 471 项（52 项条件跳过）均 0 失败；
  `compileJava` 通过。未挂载开发字体的普通聚合 `test` 会在既有两项 Java2D 导出测试失败；使用正式视觉脚本
  只读挂载 CJK 开发字体后，UI2 四模块测试及 63 个正式终端视觉快照全部通过。`assemble`、
  `verifyUi2SelfContained` 和 `git diff --check` 通过，runtime JAR SHA-256 为
  `397310493f01bf5731ff3be65fe455f8a411dc7fc4b37085fda4a71f7681ea5b`。未注册 Handler、未部署，生产数据库
  和 migration 未修改。

# 2026-09-10 - 跨服任务平台 Q.4 消费型 XP 与实际扣除向量

- 对照 BQ `TaskXP.detect`，修正原 Saga 只能表达“全额请求/全额确认”的限制。外部扣除结果、Submission 和
  PostgreSQL 现持久化本次实际扣除向量；确认事实使用实际量而非请求上限。物品/流体继续全额原子扣除，XP
  可以按玩家当前经验部分扣除并在后续新提交中继续推进。
- Minecraft 玩家 NBT 的 SHA-256 submission marker 从布尔值升级为实际向量 Compound；读取旧布尔 marker
  时仍按旧全额语义兼容。扣 XP、写 marker 和玩家数据 flush 共享回滚边界，保存失败恢复经验和 marker；
  成功后同步经验条。消费型 XP 已加入授权器和组合求值器，非消费型仍使用周期快照。
- Quest DDL 新增 `applied_values BIGINT[]`，资源类型允许 `xp`；JDBC 在 APPLIED 转换前校验向量宽度、非负、
  不超过请求量。使用一次性 `jgb_quest_xp_it` 数据库与角色实际执行 PostgreSQL 14 项测试，0 skipped/0
  failure，随后删除并核验数据库/角色计数均为 0。CodeGraph 复核 `ConsumptionSubmissionService`（20 个影响
  符号）和 `MinecraftPlayerConsumptionPort`（43 个）；Quest 三模块 75 项、根项目 472 项（52 项条件跳过）、
  63 个终端视觉快照均 0 失败。`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过，runtime JAR
  SHA-256 为 `e0a0fe8bda149327bd71235611a09979de12c4ecfcbc85e00639ba5c95fdd5b5`。生产 schema、Handler 注册、
  服务器和部署均未修改。

# 2026-09-10 - 跨服任务平台 Q.4 计分板与安全命令奖励

- 对照 BQ `RewardScoreboard`，实现 PLAYER 计分板奖励。相对值先转换为 int 饱和的绝对目标并持久化到
  entitlement marker，之后只执行幂等 set；崩溃重试不会把相对奖励重复相加。缺失 objective 按 criteria
  创建并回退 dummy，只读 objective 永久拒绝。
- 对照 BQ `RewardCommand` 保留变量替换和 `viaPlayer`，但把任意命令执行替换为纯核心白名单与
  `CommandRewardExecutor` 合同。执行端必须按 entitlement key 持久去重；默认空白名单，控制字符、超长和
  未允许根命令不会进入执行端。扫描当前 GTNH 默认任务库未发现 command reward，因此该安全默认不改变现有
  内容。没有复制 BQ 网络、调度器或管理员 CommandSender。
- 新增核心策略/handler 测试与 Minecraft 计分目标计算测试；Handler 仍未注册，生产配置、数据库和服务器
  均未修改。Quest Core 51、Importer 12、PostgreSQL 14、根项目 473 项测试均为 0 失败（条件测试按环境跳过）；
  63 个终端视觉快照、`assemble`、`verifyUi2SelfContained` 和 `git diff --check` 通过。runtime JAR SHA-256
  为 `d76ccb3b86d8c5d1d9c4e271fe1179a866cd17bc195f0b19f1207965cd2be83d`，本批未部署。

# 2026-09-10 - 跨服任务平台 Q.6 编辑领域与章节迁移基础

- 对照 BetterQuesting `GuiQuestEditor`、`QuestLine` 与 `QuestLineEntry`，开始吸收“编辑任务内容和编排章节”
  的领域能力，但不复制 GUI、Packet、全局数据库单例或 NBT 文件写入。新增纯 Java `QuestDraftEditor`，支持
  基本字段、前置关系、Task/Reward 的增删、替换和稳定排序；所有操作输出新不可变定义，发布版本不原地改写。
- 新增版本化 `QuestChapterDefinition/QuestChapterEntry` 与章节草稿编辑器；章节只保存展示编排，不承载玩家
  进度。PostgreSQL 新增章节定义表和 JDBC 仓储，复用 DRAFT/PUBLISHED/RETIRED、内容哈希、事务写入及乐观锁，
  为多个管理端跨服编辑提供冲突保护。
- BQ 导入器新增 split QuestLines 只读解析：读取 `QuestLinesOrder.txt` 的章节顺序/名称、`QuestLine.json` 的
  说明/图标/背景，以及位置文件的 UUID、坐标和尺寸；URL-safe Base64 目录后缀按完整 16 字节 UUID 验证，
  避免连字符被误判为名称分隔符。对当前客户端只读实跑通过：3739 个 Quest、46 个章节、3842 个位置，Task/
  Reward 均无未知类型。
- CodeGraph 同步 14 个新增文件；影响范围为 `QuestDraftEditor` 44、`QuestChapterDefinition` 76、
  `JdbcQuestChapterRepository` 17、`BqQuestChapterImporter` 28 个符号。Core 56、Importer 15、PostgreSQL 15、
  根项目 473 项测试均 0 失败（PostgreSQL 10、根项目 52 项按环境跳过）；63 个终端视觉快照、`assemble`、
  `verifyUi2SelfContained` 和 `git diff --check` 通过。runtime JAR SHA-256 为
  `fff4604bdd8d8e6fbe90fab078d4e8fac1220b8101148f7d601c03923fa17485`。生产 migration、Handler 注册、服务器
  和数据库均未修改，本批不部署。

# 2026-09-10 - 跨服任务平台 Q.4/Q.6 行为、领取与编辑类型合同

- 导入并内部化 BQ Quest 行为：七类可见性、图标引用、main/silent、自动领取、锁定进度、simultaneous、
  global/globalShare、音效和相对/固定重复周期。纯核心运行时默认拒绝锁定任务进度，显式属性才允许推进；
  simultaneous 在未同时满足时清除局部结果，可见性求值不依赖 BQ 客户端单例。
- 奖励资格新增 `CLAIMABLE`。普通完成等待玩家本人领取，auto-claim 才直接 `PENDING`；手动 Choice 必须先由
  当前认证玩家做不可变选择，再经领取事务转入投递队列。领取按玩家、Quest 版本和周期加锁，区分未找到、
  非本人、缺少选择、已领取和非法状态；DDL 增加 `claimed_at`，但未向生产数据库执行。
- 新增纯 Java 编辑描述层：`QuestEditorTypeRegistry`、Task/Reward 元素描述、字段类型和结构化参数问题。
  BQ 适配目录完整覆盖 13 种 Standard Task 与 6 种 Reward，能驱动布尔、数字、枚举、UUID、物品/流体/
  方块列表、实体、NBT 与命令控件，并逐项拒绝空身份、非整数和非正数量；不引入 BQ GUI 或网络。
- CodeGraph 复核 `RewardEntitlement`（49 个影响符号）、`QuestBehavior`（90）、
  `QuestEditorTypeRegistry`（25）和 `BqStandardEditorTypeRegistry`（19）。Quest 三模块定向测试通过；另用
  一次性数据库/角色实际执行 PostgreSQL 集成测试，覆盖手动奖励及 Choice 的“选择-领取-租约”完整链，随后
  删除并核验数据库与角色计数均为 0。当前客户端 DefaultQuests 只读复跑为 3739 Quest、46 章节、3842
  位置，未知 Task/Reward 均为空。生产注册、migration、服务器和部署均未修改。
- 最终自动门禁：Core 63、Importer 17、PostgreSQL 19 项均 0 failure/0 error（默认无连接环境下 PostgreSQL
  12 项条件跳过，另已在一次性真库中无跳过通过）；根项目 473 项、52 项条件跳过、0 failure/0 error。
  正式字体挂载的 UI2 脚本验证 63 个终端视觉快照；`assemble`、`verifyUi2SelfContained` 与
  `git diff --check` 均通过。runtime dev JAR SHA-256 为
  `e845ae956199c1e62e173e4729729e90e24dca98bdd02caa6b0db54ffbb4bc4f`。未部署。

# 2026-09-10 - 跨服任务平台 Q.6 玩家任务中心与有界分页

- 新增平台无关 `QuestCenterVisualModel/QuestCenterVisualDocument/QuestCenterActionPort`，玩家任务中心已经能在
  Java2D 与 Minecraft 双宿主之间共享浏览、章节、筛选、详情、Task 进度、奖励与领取动作。浏览、详情、空态、
  错误态覆盖三套主题和三档视口，统一视觉报告现验证 99 个终端快照。
- 修复大型任务库不可用的结构问题：Document 不再静默只绘制前 6/9 个章节和 7 个任务。模型新增查询词、
  章节/任务页索引、页大小、总数和前后页状态，动作端口新增查询与两类翻页意图，页面明确显示分页控件。
- 纯核心新增 `QuestCenterPageRequest/QuestCenterPaginator/QuestCenterPage/QuestCenterChapterSummary`。搜索和
  `all/active/claimable/completed` 筛选先于分页，非法页码会限幅，客户端页大小硬限制为章节 12、任务 24；
  `QuestCenterQuery.loadPage` 使 PostgreSQL 读取结果能在进入网络前缩成有界快照，章节摘要不携带全部布局坐标。
- PostgreSQL `JdbcQuestCenterQuery` 已按服务端参与主体批量连接最新发布定义、章节、玩家进度、Task 向量和
  奖励状态；根适配器可直接映射有界服务端页，不再二次截断。正式终端 Snapshot/Action 接线仍未启用，生产
  migration、Forge Handler、数据库和服务器均未修改，也未部署。
- CodeGraph 同步 21 个文件；`QuestCenterPaginator` 影响 27 个符号，`QuestCenterVisualModel` 影响 194 个符号。
  Core 66 项、UI2 Terminal 4 项以及根适配器 2 项定向测试均 0 failure；99 个视觉快照通过结构审计。
  章节与任务节点使用实体 ID 而非页内序号作为对账 key，翻页不会把上一页控件状态错误保留给下一页实体。
  `assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过；runtime JAR SHA-256 为
  `0043011a280aed98ffd84ad23fbd0537bf9f96ba35b299357ea79fab1e3035b6`。未部署。

# 2026-09-10 - 跨服任务平台 Q.6 Base 终端协议接线

- 新增 Base 自有 `TerminalQuestCenterSectionSnapshot` 和 `TerminalQuestActionPayload`，把玩家任务中心接入现有
  `TerminalSnapshotMessage/TerminalActionMessage` 主链；没有复制 BetterQuesting Packet、客户端数据库或
  玩家身份字段。职业正式路由改用 `TerminalQuestCenterDocument`，共享纯 Java Document，Minecraft 层只解析
  真实物品图标。
- 服务端只从当前认证 `EntityPlayerMP` 的 UUID 构造 `ParticipantId`。查询词、筛选、章节选择和两级页码均受
  限；编码端将章节、任务、Task、Reward 分别截断到 12、24、64、64，解码端拒绝越界数量。详情页刷新保留
  当前任务，只有返回动作退出详情。领取仍明确只读，生产 `QuestCenterQuery` 未装配，现有 BQ 不受影响。
- 新增动作载荷、协议完整详情往返、集合上限、认证身份和快照回传测试。CodeGraph 已同步 18 个改动文件；
  `TerminalService` 影响 396 个符号，`TerminalQuestCenterDocument` 影响 63 个符号，`affected` 未额外识别测试，
  因此执行了全量测试。根项目 481、Quest Core 66、Importer 17、PostgreSQL 20、UI2 Core 41、UI2 Lab 2、
  UI2 Terminal 4、UI2 Demo 5 项均 0 failure/0 error；挂载开发用 CJK 字体后 99 个终端视觉快照通过。
- `assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过；runtime JAR SHA-256 为
  `7d46c38bd001b96eea877f85b80cc131c87b3d1c87ec35d98535d403e8f41c7c`。生产 migration、Handler、数据库、
  Lobby/S1/S2 和客户端均未修改，本批不部署。

# 2026-09-10 - 跨服任务平台 Q.6 只读运行时与认证领取装配

- 新增默认关闭的 `questPostgresReadEnabled` 服务端开关和 `QuestModule`。模块只在专用服务器、共享 Banking
  JDBC 已就绪、五张 Quest 表通过只读校验时安装终端查询；不自动建表、不读取客户端数据库配置、不创建第二套
  凭据。任一条件失败均清空已安装端口并保持任务中心不可用，现有 BetterQuesting 继续工作。
- 根构建开始内嵌 Base 自己的 `galaxy-quest-postgres`；自包含门禁明确检查 Quest Core 的认证领取服务和
  PostgreSQL 查询类存在于最终 JAR。玩家仍只需 Base JAR，不依赖 BetterQuesting API 或额外 Quest Mod。
- 新增 `AuthenticatedQuestClaimService`：客户端只能提交 Quest UUID；服务端在事务内从最新发布定义和当前玩家
  进度推导 version/cycle，然后调用已实现的幂等 Claim Repository。成功进入 PENDING 交付队列，重复、缺 Choice、
  非本人、非法状态与缺失任务均返回明确文案。生产开关保持关闭，领取不会在现网触发。
- CodeGraph 同步 10 个文件；`QuestModule` 影响 43 个符号，`AuthenticatedQuestClaimService` 影响 29 个符号，
  `affected` 未额外识别测试，故执行全量回归。根项目 484 项（52 项环境条件跳过）、Quest Core 68、Importer 17、
  PostgreSQL 20（13 项因本轮未注入测试库环境而条件跳过）、UI2 Core 41、Lab 2、Terminal 4、Demo 5 项均
  0 failure/0 error。99 个视觉快照、`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。
- runtime JAR SHA-256 为 `6329d193131740daeba58ae0c34671690aad676bc01ba90532256e698435b696`。
  本轮未执行生产 migration、未启用 Quest 开关、未注册 Forge Handler、未部署，也未修改 Lobby/S1/S2、客户端
  或生产数据库。
## 2026-09-10 — Q.6 奖励多选项认证边界

- 新增 `AuthenticatedRewardChoiceService`：客户端未来只需提交任务、奖励键和选项序号；玩家身份、已发布定义版本、当前循环及 entitlement key 均由服务端推导。
- 仅允许 `bq_standard:choice` 奖励进入选择仓储，非多选奖励在数据库调用前拒绝；选择仍由 PostgreSQL 仓储保证不可变与冲突检测。
- CodeGraph 确认原 `RewardChoiceSelectionRepository` 尚无调用者，本批先建立纯核心服务，不提前暴露不完整的终端按钮。
- 验证：`:galaxy-quest-core:test` 70 项通过，`git diff --check` 通过；未部署、未修改生产数据库。

### 终端闭环

- `QUEST_SELECT_REWARD_CHOICE` 已贯通 UI2、终端意图协议、认证服务与 PostgreSQL 不可变选择仓储。
- 任务奖励快照现在携带最多 32 个有界选项及服务端读取的已选序号；客户端仍不能提交玩家、定义版本、周期或 entitlement key。
- `JdbcQuestCenterQuery` 联查 choice selection，启动校验增加对应表；UI2 详情使用稳定键显示选项，选定后禁止改选，随后可继续执行原有领取流程。
- 旧 6 段任务意图仍可解码；新协议增加奖励键和 0–31 的选项序号。协议往返测试覆盖选项物品、数量和已选状态。
- 验证：核心、PostgreSQL、UI2 Terminal、根项目全量测试通过；99 个终端视觉快照通过；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。
- 运行 JAR：`build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `8babf202d363bb6ef10c6c0e96b022d581491df6054dd4c3fe6e5602576c789a`。未部署、未修改生产数据库。

## 2026-09-10 — Q.6 跨服任务追踪真源

- 新增纯核心 `AuthenticatedQuestTrackingService` / `QuestTrackingRepository` 与 PostgreSQL `JdbcQuestTrackingRepository`；追踪状态由数据库保存，不复用 BetterQuesting 客户端 JSON。
- 新 DDL `galaxy_quest_player_preference` 以 `(player_id, quest_id)` 唯一，原子切换 `tracked`，并为玩家最近追踪查询建立部分索引。该 DDL 仍只是评审/测试制品，未应用生产库。
- `JdbcQuestCenterQuery`、终端快照/编解码及 UI2 目录/详情已贯通追踪状态；客户端只提交任务 ID，服务端验证最新已发布任务并使用认证玩家身份。
- CodeGraph 影响集中在 QuestModule、TerminalService、查询映射与 UI2 任务文档；因此执行了全量回归。
- 验证：根项目 485 项（52 跳过）、Quest Core 72 项、Quest PostgreSQL 20 项（13 项因集成环境条件跳过）、UI2 Terminal 4 项、UI2 Demo 5 项，均 0 失败；99 个终端视觉快照通过；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。
- 运行 JAR：`build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `7b6d3f84bca076285a8545bf69b6d28d9a227b87de3db7ba35e93ed31ad68eeb`。未部署、未改生产数据库。

## 2026-09-11 — Q.6 已追踪任务 HUD

- 新增有界 `TerminalTrackedQuestSnapshot`：最多 5 个任务、每项最多 3 个目标，只有展示字段，不含玩家身份、版本、周期或任何写权限。
- `TrackedQuestSnapshotFactory` 从服务端 `QuestCenterQuery` 投影数据库真源；`TrackedQuestSyncController` 在登录及低频服务端周期通过 Base 自有消息发送，不使用 BetterQuesting Packet/JSON。
- 客户端 `TrackedQuestHudState` 保存不可变快照，离开世界立即清空；原 HUD 通知层增加左上角任务摘要，最多显示 3 项并对标题、目标文本和进度做宽度限制。
- Forge 事件总线注册被抽成可替换组合边界，避免纯 JVM 测试初始化 Forge 单例；全量测试曾捕获此问题，修复后重跑通过。
- CodeGraph 影响集中在 QuestModule、同步控制器、网络消息和 HUD；执行全量回归而非仅定向测试。
- 验证：根项目 487 项（52 跳过）、Quest Core 72 项、Quest PostgreSQL 20 项（13 项环境条件跳过）、UI2 Terminal 4 项、UI2 Demo 5 项，均 0 失败；99 个终端视觉快照通过；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。
- 运行 JAR：`build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `44c41b41f968c62ec58a8fb3f567d6ec4fe75466b3f6afe428fe39a9b9f6868a`。未部署、未修改生产数据库。

## 2026-09-11 — Q.6 服务端草稿管理命令层

- 新增纯 Java `QuestDraftManagementService`、认证编辑者与权限合同、结构化结果；支持草稿创建、内容哈希乐观锁更新、发布和退役，并保持所有写操作处于 `QuestTransaction` 内。
- `QuestDefinitionValidator` 现在可直接用 `QuestEditorTypeRegistry` 校验 Task/Reward 类型及其字段参数，问题路径稳定到具体元素和字段；未知类型、自依赖与重复 key 均在持久化前拒绝。
- 权限来源被明确限制为服务端认证的 `QuestEditorActor`，客户端未来只发送编辑意图和预期内容哈希，不携带管理员判定。BetterQuesting GUI、Packet、客户端数据库和 JSON 写入均未引入。
- 新增 5 组管理服务测试，覆盖拒绝未授权、结构校验、完整创建/更新/发布/退役事务、过期哈希冲突及不存在目标。CodeGraph 已同步新增文件；Quest Core 77 项、根项目 487 项（52 项环境条件跳过）均 0 failure/0 error，99 个终端视觉快照、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过。
- 运行 JAR：`build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `3b01573ae6179077796dabdce01ed1dde14d750377f3e6125d56fbce5a80057a`。生产组合根、管理网络、数据库和部署尚未改动。

## 2026-09-11 — Q.6 管理查询与 Forge 权限组合

- 新增独立 `QuestDefinitionManagementQuery`、有界请求和分页结果，不扩张运行时 `QuestDefinitionRepository` 的玩家目录合同。PostgreSQL 查询支持 DRAFT/PUBLISHED/RETIRED、名称或完整 UUID 筛选、稳定排序和最多 50 项服务器分页。
- 新增认证查询包装；未授权编辑者不会触发存储访问。`MinecraftQuestEditorAuthorization` 只按当前在线 `EntityPlayerMP` 的认证 UUID 判断，并复用 Forge 命令权限 level 2，客户端无法提交权限布尔值或冒用离线名称。
- BQ Standard 编辑类型规范从导入器下沉至纯 Core；导入器保留兼容门面，生产 QuestModule 可直接组合 13 类 Task、6 类 Reward 的字段规范，不需要把导入 CLI/Gson 打进 Base JAR。
- CodeGraph 影响：管理查询 44 个符号、QuestModule 55 个符号、标准类型注册表 33 个符号。新增 3 项认证查询/边界测试，并扩充 PostgreSQL 乐观锁集成用例验证管理筛选。Quest Core 80 项、根项目 487 项（52 项环境条件跳过）均 0 failure/0 error；Importer/PostgreSQL 定向测试、99 个终端视觉快照、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 全部通过。
- 运行 JAR：`build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `a2dc7225711a78828e07552e432b937b94d83b665e330b80a859c8a0730b18df`。未部署、未执行生产 DDL，Lobby/S1/S2 与客户端均未修改。

## 2026-09-11 — Q.6 任务管理终端只读闭环

- Base 终端新增 `QUEST_ADMIN_OPEN/FILTER/PAGE`，任务动作载荷扩展管理生命周期和页码，同时继续兼容原 6/8 段编码。管理快照最多 50 条，只包含任务 ID、版本、名称、生命周期、内容哈希、发布时间及 Task/Reward 数量。
- `TerminalService` 从当前 `EntityPlayerMP` 传入认证 UUID/名称，认证管理查询再次通过在线实体与 OP level 2 校验；未授权访问返回稳定拒绝页，不执行数据库查询。QuestModule 负责安装和失败时清空查询端口。
- 新增纯 Java `QuestManagementVisualModel/Document/ActionPort`。玩家任务页提供管理入口，管理页支持返回、生命周期筛选和服务器分页；三主题下的正常/空态及三档视口均进入 Java2D 结构审计。
- 新增协议截断、管理动作兼容、认证身份映射与 UI2 视觉测试。根项目 490 项（52 项环境条件跳过）、UI2 Terminal 5 项均 0 failure/0 error；终端视觉快照由 99 增至 117，`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 全部通过。
- 运行 JAR：`build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `770f177ed1e31bf62a99576e9721fcbd76ca86128aef18bd53a2da1dfcb5739f`。未部署、未执行生产 DDL，Lobby/S1/S2 与客户端均未修改。

## 2026-09-11 — Q.6 定义详情与发布生命周期闭环

- 管理查询增加按 `quest_id + version` 读取定义，终端详情以有界结构返回说明、前置、Task、Reward、逻辑和行为选项；UI2 列表可进入详情，并只在 DRAFT/PUBLISHED 状态分别显示发布/退役动作。
- 管理动作只提交任务 ID、版本和已读内容哈希。`TerminalService` 使用当前认证玩家构造编辑者，写服务再次执行服务端授权、类型/参数校验和 PostgreSQL 乐观锁；`QuestModule` 在启动、失败清理与测试复位时成对安装/卸载读写端口。
- 发布前新增依赖图门禁：缺少已发布直接前置、不可完整读取的依赖链、超过有界遍历规模以及由候选最新版本形成的循环均拒绝发布。玩家运行时仍只读取最新已发布版本。
- CodeGraph 已同步；`buildQuestManagementSnapshot` 影响 3 个符号，图校验影响 20 个符号。全量 666 项测试（65 项环境条件跳过）0 failure/0 error，126 个终端视觉快照通过；`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过。
- 运行 JAR：`build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `1ac899ab4204033ba9717b6f02ce0f2f662ba57d0a3272ca7335571d3a672da1`。未部署、未执行生产 DDL，Lobby/S1/S2 与客户端均未修改。下一项为草稿创建和字段编辑表单。

### 字段编辑命令基础

- 新增平台无关 `QuestDraftEditOperation/QuestDraftEditRequest`，用最多 128 个白名单语义操作表达一次编辑，不接受反射、任意对象图或客户端权限字段。
- `QuestDraftManagementService.applyEdit` 仅允许编辑 DRAFT：先在不可变定义的工作副本上应用完整批次，再复用类型注册表验证并以预期内容哈希执行一次事务写入；错误 key、位置、类型或陈旧哈希均不会产生部分保存。
- 新增原子批量编辑与失败不落库测试；Quest Core 定向测试通过。该合同尚未暴露到网络或正式 UI，下一步是有界操作编解码和由字段描述器驱动的表单。

### 基础字段编辑端到端

- 新增 Base 自有 `TerminalQuestDraftEditPayload`，使用带版本标识的有界二进制载荷承载最多 128 个语义操作；限制载荷、文本、参数数量和值长度，并拒绝未知操作、重复参数与尾随内容，不复用 BetterQuesting Packet。
- `QUEST_ADMIN_EDIT` 已接入正式终端。DRAFT 详情可编辑名称、说明、前置逻辑和目标逻辑；客户端只形成语义批次，服务端重新鉴权、读取当前草稿并通过内容哈希一次提交。保存成功后返回新哈希，UI 编辑会话自动收口；取消不会发送网络动作。
- 编辑页新增三主题、三视口确定性场景。第一次门禁发现 350 宽详情操作栏出现 0 宽按钮，随后把详情头改为响应式两行结构；最终 135 个终端视觉快照全部通过。
- CodeGraph 显示编辑载荷解码影响 40 个符号、编辑会话影响 52 个符号。全量 670 项测试（65 项环境条件跳过）0 failure/0 error；`assemble`、`verifyUi2SelfContained` 和 `git diff --check` 通过。
- 运行 JAR SHA-256：`a18e6157de0a7660dbe20479e5ec4a656f5dd817f966db0c4d00242af0f7051b`。未部署、未执行生产 DDL、未触碰 Lobby/S1/S2 或客户端。下一项为字段描述器驱动的 Task/Reward 编辑器、前置选择器与草稿创建模板。

### Task/Reward 描述器驱动编辑

- 管理快照新增有界 Task/Reward 类型目录，字段描述包含类型、必填、数值范围和枚举选项；目录只在管理查询已经通过服务端授权后生成。协议往返测试覆盖 nullable 上下界和字段类型。
- UI2 不再按具体 BetterQuesting 类型硬编码表单。现有 Task/Reward 行可进入元素编辑器；BOOLEAN、ENUM 与其他字段分别生成开关、受限选择和文本输入，字段超过四项时分页。稳定 key、Task 可选属性及参数通过 `REPLACE_TASK/REPLACE_REWARD` 原子保存并复用服务端校验。
- 元素编辑器增加三主题、三视口结构场景，视觉快照由 135 增至 144；详情和元素编辑在 350×193 下均无 0 宽或越界内容。
- CodeGraph 显示类型目录影响 26 个符号，元素编辑器核心影响 4 个直接符号。全量 670 项测试（65 项环境条件跳过）0 failure/0 error，144 个终端视觉快照通过；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。
- 运行 JAR SHA-256：`d96ee45042e7ea69cb6028ae961f4746001de7b4f67b0a1d81ddd05523ae9da9`。未部署、未执行生产 DDL。下一项为元素新增/删除/排序、前置任务选择器和新草稿模板。

### Task/Reward 元素新增、删除与排序

- DRAFT 详情的目标与奖励卡增加类型目录驱动的新增入口；新元素只允许在服务端已声明的类型间切换，并在必填字段完整后启用保存。现有元素提供上移、下移和两步删除确认。
- Minecraft 适配桥把新元素映射为 `ADD_TASK/ADD_REWARD`，把排序与删除分别映射为 `MOVE_*`、`REMOVE_*`；载荷继续只承载 Base 白名单语义操作，不发送完整定义或客户端权限。
- CodeGraph 同步后确认主要影响为 `QuestManagementVisualDocument`、`TerminalQuestCenterDocument` 和视觉场景。UI2 Terminal 定向测试、根项目全量 670 项测试（65 项环境条件跳过）、`assemble` 与 `verifyUi2SelfContained` 均通过；三主题三视口的终端视觉快照由 144 增至 153，结构告警为 0。
- 未部署、未执行生产 DDL、未触碰 Lobby/S1/S2 或客户端。下一项为前置任务选择器和新草稿模板。

### 前置任务选择器

- DRAFT 基础编辑页增加独立前置选择子页；服务端在详情已授权后读取最多 50 个最新 PUBLISHED 定义作为候选并排除自身，普通管理列表分页合同不变。
- 客户端保存前后 UUID 集合差异，只发出 `ADD_PREREQUISITE/REMOVE_PREREQUISITE` 白名单操作；服务端继续执行内容哈希乐观锁，发布时继续执行完整依赖存在性与循环检测。
- 新增三主题、三视口前置选择场景；162 个终端视觉快照全部通过且结构告警为 0。根项目 670 项测试（65 项环境条件跳过）、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过。运行 JAR SHA-256：`f3de8c1212f071b5c35ab649a337b381c572819411af7e6069e764a9c5c94053`。未部署、未执行生产 DDL。下一项为受限的新草稿模板与创建协议。

### 服务端所有的新草稿模板

- 新增 `QuestDraftTemplate`：空白任务、人工确认目标和经验目标。客户端只提交模板 ID 与任务名称；UUID、版本、逻辑和初始 Task 均由服务端构造，再复用原管理服务授权、标准类型校验和事务创建。
- 终端任务管理列表增加新建入口与独立创建页；`QUEST_ADMIN_CREATE` 使用任务意图协议新增的专用 `managementTemplate` 字段，不复用奖励字段，也不承载玩家身份、任意参数或完整定义；旧 6/8/10/11/12 段意图继续兼容解码。创建成功后返回 DRAFT 查询结果。
- 模板合法性、“身份由服务端生成”和专用模板字段往返有自动测试；创建页增加三主题、三视口场景。全量 673 项测试（65 项环境条件跳过）、171 个终端视觉快照、`assemble`、`verifyUi2SelfContained`、`git diff --check` 全部通过。
- 运行 JAR SHA-256：`b7249a5897ba8df7ba9773174a1e56eebe65d580c27cfc2d02ecdcd9ef180b3c`。未部署、未执行生产 DDL、未触碰 Lobby/S1/S2 或客户端。下一批继续吸收任务行为选项、重复策略和章节编辑能力。

### 重复策略与任务行为编辑

- 草稿语义操作新增末尾兼容的 `SET_OPTION`。服务端白名单覆盖重复周期/边界、可见性、主线、静默、自动领取、锁定进度、同时满足、全局/共享、图标与进度/完成音效；严格拒绝未知键和非法值，并保持批次原子回滚。
- UI2 提供三页响应式表单，分别编辑重复与可见性、执行语义、资源与音效；选项保存只发送实际变化。客户端仍不能提交完整 `RepeatPolicy`、`QuestBehavior` 或任意 JSON。
- 新增连续选项合并及未知选项不落库测试，编辑载荷往返覆盖 `SET_OPTION`；三主题三视口视觉场景增至 189 张且 0 结构告警。全量 675 项测试（65 项跳过）、`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。
- 运行 JAR SHA-256：`1a95d3a71de20a7cea11095f727c184c541a7e21dd844fa46f2596cfc3ed76ba`。未部署、未执行生产 DDL。下一阶段进入章节管理、布局放置和章节发布生命周期。

### 章节管理服务端基础

- 新增章节批量语义编辑请求、结构化结果与认证管理查询。章节草稿支持名称、说明、图标、背景、可见性以及任务位置的放置、移动、缩放和移除；整批先在内存完成并校验，再以预期内容哈希执行一次乐观锁写入。
- 章节发布会重新校验所有位置，并要求引用的任务存在已发布版本；非法输入、越权、未找到与并发冲突使用独立结果。认证查询在访问仓储前检查当前服务端编辑者权限，并提供生命周期、名称/完整 UUID 和最多 50 条的服务器分页。
- `JdbcQuestChapterRepository` 实现管理查询，`QuestModule` 在同一共享 JDBC 与 Forge 权限边界下安装章节读写端口，停用或初始化失败时成对清空。当前尚未增加客户端章节载荷或 UI2 页面，因此正式玩家入口行为未改变。
- CodeGraph 已同步并检查 `QuestChapterManagementService`、`JdbcQuestChapterRepository` 的影响链。Quest Core 定向测试、PostgreSQL/根源码编译与 `QuestModuleTest` 通过；PostgreSQL 真库用例已增加章节名称/生命周期查询断言但未执行生产数据库。未部署、未执行 DDL、未触碰 Lobby/S1/S2 或客户端。下一步为章节专用有界意图载荷和 UI2 布局编辑器。

### 章节终端载荷与纯 Java 布局画布

- 新增独立 `TerminalQuestChapterEditPayload`，完整覆盖章节的 10 类白名单操作，同时限制编码大小、操作数量、文本长度、UUID、版本和预期哈希；畸形编码、未知序号和尾随字节均拒绝，不复用 BQ Packet 或普通任务导航字段。
- 新增平台无关章节管理模型、动作端口和生产 UI2 Document。章节详情使用专用画布按内容边界确定性缩放，可显示负坐标、不同尺寸及重叠任务；后放置条目保持上层绘制/优先命中。选中后可步进移动、扩大或移除，并可从服务端候选加入章节。
- 载荷往返/失败关闭测试通过；章节列表与空间详情在 `350×193`、`427×240`、`620×340` 下结构审计均为 0 问题。当前 Document 尚未接正式终端网络快照，下一步补章节 DTO/编解码、认证路由和 Minecraft Action Bridge。

### 章节管理正式终端闭环

- 正式终端快照和网络编解码新增有界章节管理投影；列表、详情、空间位置和候选任务均不携带玩家身份或写权限。
- `TerminalService` 接入章节打开、筛选、分页、选择、创建、编辑、发布和退役；定向测试证明当前认证玩家身份、20 项服务端分页以及负坐标/尺寸可完整映射到终端 DTO。
- `TerminalQuestCenterDocument` 与 `TerminalApplicationScreen` 已接通章节动作，任务定义管理页增加章节入口。章节 UI2 新增三主题下列表、空态、空间详情共 9 个场景，三档视口共 27 张快照。
- CodeGraph 影响集中于 `TerminalService`、`TerminalQuestCenterDocument` 和 `QuestChapterManagementVisualDocument`，因此回归包含协议、服务端路由、UI2 Terminal 和根构建。
- 自动验收：全仓 686 项测试（65 项环境条件跳过）0 failure/0 error；231 张 UI Lab/正式终端 PNG 与结构快照生成成功；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。运行 JAR SHA-256：`53bcc1a39a6aa757d52609536b1673f97761d18bc3760bf5f089d61aec80a3ff`。
- 本批未部署、未执行生产 DDL、未启用 Quest 生产开关，也未触碰 Lobby/S1/S2 或客户端。章节字段表单、候选搜索和直接拖拽/缩放仍为下一批。

### 章节新建命名表单

- 章节管理的新建动作不再立即提交固定“新任务章节”。新增纯 Java UI2 名称表单，限制 96 字符、空名称禁用提交、取消不产生服务端动作；UUID、版本和初始布局继续完全由服务端创建。
- 新建表单加入三套主题和三档视口，统一视觉报告由 231 增至 240 张 PNG/结构快照，结构问题为 0。
- 全仓 686 项测试（65 项环境条件跳过）0 failure/0 error；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。运行 JAR SHA-256：`fbdb51fa24a02ebef869885cd361f64ff1bf644df051c980b3b2eed9aceb0304`。
- 未部署、未执行生产 DDL。下一项是章节基础属性的两页编辑表单，然后进入候选任务搜索和直接画布拖拽。

### 章节基础属性编辑表单

- DRAFT 章节详情增加两页纯 Java UI2 编辑器：名称/说明/可见性，以及图标/背景/背景尺寸；已发布章节没有编辑入口。
- 保存通过现有 `saveBasics` 桥产生 `SET_NAME/SET_DESCRIPTION/SET_ICON/SET_BACKGROUND/SET_BACKGROUND_SIZE/SET_VISIBILITY` 白名单操作，继续绑定章节版本和已读内容哈希。
- 新增真实点击编辑、保存的动作测试，逐项断言 ID、版本、哈希和六类字段；两页表单加入三主题三视口视觉门禁。
- 全仓 687 项测试（65 项环境条件跳过）0 failure/0 error；258 张 PNG/结构快照生成成功；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。运行 JAR SHA-256：`71522dd13fb39c8260dcf60b539e4d28b29a134fc7c209fb186d72cf009b2051`。
- 未部署、未执行生产 DDL。下一批为候选任务搜索/分页，以及章节画布拖拽、平移和缩放。

### 章节候选检索与直接画布操作

- 候选任务侧栏增加名称/UUID 搜索和每页三项的本地分页，可从服务端授权返回的最多 50 个发布任务中选择任意一项；无匹配结果有明确空态。
- 章节画布增加 Pointer Capture：左键拖拽任务提交 `MOVE`，右键拖拽提交 `RESIZE`，拖动时提供即时轮廓预览；鼠标释放前不修改定义，最终仍由服务端鉴权和乐观锁写入。
- 自动测试真实分发按下/移动/释放事件，验证捕获、负坐标换算、移动与缩放语义；另有搜索中文并加入非首个候选的动作测试。
- 全仓 690 项测试（65 项环境条件跳过）0 failure/0 error；258 张 PNG/结构快照通过；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。运行 JAR SHA-256：`7cfb2ea1ce9fb0eba0a42b3def553807a2591d75ff4806c0d2ad42cd837d0c0c`。
- 未部署、未执行生产 DDL。下一步补画布平移、缩放、网格和键盘精调，然后继续 BetterQuesting 功能差距审计。

### 章节画布导航与键盘精调

- 章节画布增加自适应坐标网格、光标锚定滚轮缩放、中键 Pointer Capture 平移；缩放和平移仅是客户端视图状态，不进入任务定义或服务端载荷。
- DRAFT 中选中任务后，方向键按一个逻辑单位提交既有 `MOVE` 语义动作；没有新增客户端直接写定义的通道，服务端版本、哈希、授权和事务边界保持不变。
- CodeGraph 同步后确认主要影响集中于 `QuestChapterManagementVisualDocument` 及其纯 Java 测试；文件级 `affected` 未发现额外测试入口。定向测试新增网格、缩放、平移、捕获释放和键盘精调覆盖。
- 全仓 692 项测试（65 项环境条件跳过）0 failure/0 error，258 张 UI Lab/正式终端 PNG 与结构快照生成成功；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。运行 JAR SHA-256：`d80dfddf9731f713eec3045c6860e792d0f9bb31d0810684b775295172ff31c4`。
- 本批仍未部署、未执行生产 DDL、未启用 Quest 生产开关。下一步为 BetterQuesting 功能差距审计。

### BetterQuesting 能力矩阵与批量克隆核心

- 新建 `docs/betterquesting-capability-absorption-matrix-2026-09-13.md`，以本地只读源码逐项记录任务定义、13 类 Standard Task、6 类 Reward、编辑、诊断、迁移与影子运行差距；明确不搬运 GUI、Packet、客户端数据库、JSON 存档和 Mod 生命周期。
- 固定兼容提交 `524b365211b6b3a9672cab8ae45b4e07e726d49e` 仍在本地源码对象库中；本地镜像 HEAD 为 `5ff4864bb072c866ac8c527495c76d3d77e4be2f`，后续修复只能作为注明版本的补充参考。
- 新增纯 Java `QuestDefinitionBatchCloner`、有界 `QuestDefinitionCloneRequest` 和结构化结果。组选中复制保持源顺序，为复制品生成不碰撞 UUID，只重映射组内前置，保留组外前置、Task、Reward、重复策略和行为选项。
- `QuestDraftManagementService.cloneDrafts` 在读取任何定义前执行服务端授权，按精确 ID+版本读取，完整校验复制品，并在单个 `QuestTransaction` 中创建所有版本 1 草稿；客户端没有 NBT 或身份写入口。
- CodeGraph `impact` 显示批量克隆影响集中于纯核心管理服务与新增测试，文件级 `affected` 没有遗漏额外测试入口。全仓 698 项测试（65 项环境条件跳过）0 failure/0 error，258 张 UI Lab/正式终端 PNG 与结构快照通过；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。
- 运行 JAR SHA-256：`80415a66ac7fe895b7747d7af8346ccd0d586e035ead65ff94f3b0eb79167ef5`。未部署、未执行生产 DDL、未启用 Quest 生产开关。下一步把任务克隆计划与章节位置合并为服务端原子事务，再开放有界终端动作。

### BQ 替代目标文档进度回填

- 将当前代码与自动门禁证明的已完成能力回填到原始目标文档，而不再只散落在阶段 Worklog；同步修正 Q.4、Q.6 状态。
- 新增“尚未完成”和六项最终验收定义，明确批量编辑、检测兼容、团队奖励、影子运行、迁移演练及冻结 BQ 都是目标组成部分，后续不得把某个中间批次通过误报为完成替代。
- 本次仅更新文档，不修改运行代码、不部署、不执行数据库操作。

### 任务复制与章节布局原子复合操作

- 新增 `QuestChapterCloneService`、有界请求和结果：服务端先授权，再按章节 ID/版本/哈希读取 DRAFT 的真实布局，按精确任务 ID/版本读取源定义；未放置、缺失、发布章节、越界坐标及无效定义都会在写入前拒绝。
- 复制保留 BQ 组选中语义：首个选中位置是锚点，后续位置相对偏移保持；新任务 UUID 只由服务端生成，组内前置重映射、组外前置保留。所有新 DRAFT 定义和章节更新落在同一个 `QuestTransaction`，章节乐观锁冲突将触发事务回滚。
- 纯核心测试覆盖相对位置、内部前置、单事务、未放置源和越权早拒绝。终端专用载荷、服务端路由和 UI 多选还未接入，因此不能向玩家展示或称为完整复制功能。
- 全仓 701 项测试（65 项环境条件跳过）0 failure/0 error，258 张 UI Lab/正式终端 PNG 与结构快照通过；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。运行 JAR SHA-256：`dc153ef9e8fa1b3bc0769bdc67f4e929696f528e3d681d73481412622a03479b`。
- 未部署、未执行生产 DDL、未启用 Quest 生产开关。下一步实现专用有界终端载荷及服务端路由。

### 任务复制终端动作与画布多选

- 新增 `TerminalQuestChapterClonePayload` 与 `QUEST_CHAPTER_ADMIN_CLONE`：载荷只允许章节 ID、版本、内容哈希、锚点、查询上下文和 1–128 个任务 ID/版本；Base64 解析要求 magic、边界和无尾随字节，服务端从当前认证玩家构造 `QuestEditorActor`。
- `QuestModule` 在已启用的 PostgreSQL Quest 运行时时安装 `QuestChapterCloneService`；`TerminalService` 将复制结果回填章节管理快照。运行时未启用、越权、缺失、冲突和无效数据均为失败闭合且不产生写入。
- 章节画布增加草稿多选模式与复制按钮；普通单选可复制一项，多选只在显式模式下切换，复制语义继续由服务端读取真实布局并做内部前置重映射。UI2 纯 Java 测试覆盖选择后动作和三档布局。
- CodeGraph 对 `QuestChapterCloneService`、`TerminalQuestChapterClonePayload`、`QuestChapterManagementVisualDocument` 执行 `impact`，并对实际文件执行 `affected`；完整 Docker 门禁 `:galaxy-quest-core:test :galaxy-quest-postgres:test :ui2-core:test :ui2-lab:test :ui2-terminal:test :ui2-demo:test :ui2-demo:renderGolden test assemble verifyUi2SelfContained` 和 `git diff --check` 通过，258 张 PNG 生成。
- 未部署、未执行生产 DDL、未启用 Quest 生产开关；下一项转入 QB.2：章节排序/对齐、依赖连线及影响预览。

### QB.2 章节依赖图只读投影基础

- 根据 BQ `QuestLineDatabase#lineOrder` 与 `NetChapterEdit` 的源码行为复核：章节排序是独立目录顺序，不等同于画布任务坐标；当前 Base 尚无该 PostgreSQL 权威目录模型，不能把拖拽误实现为排序。
- 新增纯 Java `QuestChapterDependencyGraph`：用服务器加载的 `QuestDefinition` 和章节真实放置项生成依赖边，区分 `INTERNAL`、`EXTERNAL` 与 `MISSING`，并提供入边计数；不接受客户端提交的连线或定义。
- `QuestChapterDependencyGraphTest` 覆盖三类边与计数。定向测试后完整 Docker 门禁 `:galaxy-quest-core:test :galaxy-quest-postgres:test :ui2-core:test :ui2-lab:test :ui2-terminal:test :ui2-demo:test :ui2-demo:renderGolden test assemble verifyUi2SelfContained` 与 `git diff --check` 均通过；下一步将该投影接入有界终端快照、网络编码和 UI2 画布连线。
- 预接入复核：章节管理快照采用顺序二进制编码；依赖边必须作为受版本控制的有界扩展，与同一 Base JAR 同步发布，禁止向旧字段序列中直接插入数据或复用 BetterQuesting 网络同步。

### QB.2 章节依赖图正式快照与画布连接

- 新增 `AuthenticatedQuestChapterDependencyQuery`：先以当前服务端 `QuestEditorActor` 鉴权，再读取所选章节的真实版本与章节内任务的最新已发布定义；同时读取直接画布外前置，以准确区分内部边、外部边与缺失前置。客户端没有提供依赖边、定义或玩家身份的入口。
- `TerminalQuestCenterSectionSnapshot.ChapterDetail`、`OpenTerminalApprovedMessage` 和 `TerminalQuestCenterDocument` 现携带最多 8192 条有界依赖边；读端拒绝负数和越界数量。该序列变更要求同一 Base JAR 的客户端与服务端同时更新，未试图兼容或复用 BetterQuesting Packet。
- UI2 章节画布在网格和任务卡之下绘制内部前置的三段正交连接；画布外或缺失前置没有被虚构为节点，后续将在侧栏增加明确的诊断/影响预览。
- 自动覆盖：核心授权/外部/缺失分类，终端编解码往返，UI2 内部边几何与三档布局。CodeGraph 已对 `AuthenticatedQuestChapterDependencyQuery`、`QuestChapterManagementVisualDocument` 执行 `impact`，并对核心、终端网络与 UI 文件执行 `affected`。
- Docker 门禁使用只读预览字体挂载后通过：`:galaxy-quest-core:test`、`:ui2-terminal:test`、根 `test`、`:ui2-demo:test`、`:ui2-demo:renderGolden`、`assemble`、`verifyUi2SelfContained` 与 `git diff --check`；生成 258 张 UI 预览。未部署、未执行生产 DDL、未启用 Quest 生产开关，也未触碰 Lobby/S1/S2 或客户端。

### QB.2 前置诊断摘要

- 章节侧栏新增固定高度的“内部 / 外部 / 缺失”前置汇总，所有三类服务端投影边都会被看见；仅内部边进入画布，避免把画布外定义或坏引用画成可点击的伪节点。
- UI2 回归覆盖最小 `350×193` 视口仍保留诊断区域；`:ui2-terminal:test`、`:galaxy-quest-core:test` 和 `git diff --check` 通过。未部署、未执行 DDL。

### QB.2 章节目录排序与相对移动

- 依据 BetterQuesting `QuestLineDatabase#lineOrder` 与 `NetChapterEdit.reorderChapters` 的语义，新增纯核心 `QuestChapterCatalogEntry`、`QuestChapterCatalogRepository`、`QuestChapterOrderingService`：目录顺序独立于章节定义版本和画布坐标，不允许把拖拽任务卡误当成章节排序。
- PostgreSQL 新增待迁移的 `galaxy_quest_chapter_catalog` 权威目录表；新草稿章节会在同一事务补齐目录项，读取按目录排序。重排在事务内锁定目录、验证客户端不能伪造或遗漏完整集合后原子替换；相对移动由服务端读取完整当前目录后转换为完整顺序，避免分页、筛选或陈旧 UI 覆盖全局目录。
- 终端新增有界 `TerminalQuestChapterOrderPayload` 与 `QUEST_CHAPTER_ADMIN_MOVE_BEFORE`；上/下操作只提交“源章节、目标前置章节（或追加）”和查询上下文，认证、章节目录读取及最终写入均在服务端完成。UI2 回归验证首行下移会发送第二章置于第一章前的相对语义。
- CodeGraph 已复核 `QuestChapterOrderingService`、`JdbcQuestChapterRepository` 及其终端入口影响范围；Docker 全量门禁在只读预览字体挂载下通过：`:galaxy-quest-core:test`、`:galaxy-quest-postgres:test`、`:ui2-terminal:test`、`:ui2-demo:test`、`:ui2-demo:renderGolden`、根 `test`、`assemble`、`verifyUi2SelfContained` 与 `git diff --check`。未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2 或客户端；仍待管理员实机验收及后续对齐/影响预览。

### QB.2 多选对齐与中断工作收口

- 接手时本批处于半接线状态：核心对齐类、协议和服务端路由已添加，但正式 Screen 未实现发送接口、UI 测试桩缺方法，且 QuestModule 异常清理路径误漏了 tracking 服务重置。先停止扩展，逐项补齐并由编译器复核，没有把不可编译状态留给后续批次。
- 新增服务端权威 `QuestChapterAlignmentService`：请求只包含章节 ID/版本/哈希、2–128 个互异已放置任务 ID 和 `LEFT/CENTER_X/RIGHT/TOP/CENTER_Y/BOTTOM` 白名单模式。服务器从当前 DRAFT 真实布局计算选中组包围盒及新坐标，一次性乐观更新；越权、缺失任务、重复选择、非草稿和版本冲突均失败闭合。
- 新增有界 `TerminalQuestChapterAlignmentPayload`、`QUEST_CHAPTER_ADMIN_ALIGN`、QuestModule 组合根和 UI2 动作链。多选时侧栏显示六种对齐按钮并替代单项移动控件，避免小窗堆叠；三档 `350×193`、`427×240`、`620×340` 结构审计均无越界。
- CodeGraph 同步并复核 `QuestChapterAlignmentService`（31 个受影响符号）、`TerminalQuestChapterAlignmentPayload`（30 个）、`QuestChapterManagementVisualDocument`（119 个）和 `QuestModule`（73 个）；`affected` 未推导测试，故显式执行核心、协议、模块、UI2、PostgreSQL 与全量门禁。
- Docker 在只读预览字体挂载下通过 `:galaxy-quest-core:test :galaxy-quest-postgres:test :ui2-core:test :ui2-lab:test :ui2-terminal:test :ui2-demo:test :ui2-demo:renderGolden test assemble verifyUi2SelfContained`，`git diff --check` 通过。运行 JAR SHA-256 为 `f0bb1b428f7e7be13fdb489ef2cdd08a53e238ab628b0020b1d7522f2aa33698`。未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2 或客户端。

### QB.3 任务定义影响预览核心

- CodeGraph 复核显示直接修改任务生命周期服务会影响约 70 个符号、章节生命周期服务会影响约 40 个符号，因此先建立独立只读预检，没有把未验证的图算法直接嵌入退休事务。
- 新增 `QuestDefinitionImpactAnalyzer`、不可变有界报告和 `AuthenticatedQuestDefinitionImpactQuery`：从服务端权威已发布目录计算直接/传递依赖与章节放置，稳定排序并显式标记截断；越权请求在仓储读取前拒绝，客户端不能提交边或“安全”结论。
- 核心测试覆盖直接与两级传递影响、章节放置、草稿排除及保守 `safeToRetire`。本批只是影响预览底座，尚未接终端协议和退休确认，也未部署、未执行 DDL 或触碰任何服务器。
- CodeGraph 对新分析器复核为 34 个局部受影响符号；完整 Docker 门禁 56 项任务通过，包括 Quest Core/PostgreSQL、UI2 全模块、258 张视觉快照、根 `test`、`assemble` 与 `verifyUi2SelfContained`，`git diff --check` 通过。运行 JAR SHA-256：`240279a4a9965f63bbcfe4c2c35633e4098a3cbdf7b89f0b79d23e3477a44014`。
- 随后完成生产接线：`QuestModule` 使用共享 PostgreSQL 连接安装认证影响查询；管理员定义详情显示直接、间接依赖、章节放置及截断摘要。退役按钮仅在服务端报告明确安全时启用，服务端动作仍再次预检，查询缺失、越权、异常或截断一律拒绝退役。
- 影响摘要复用既有有界 options 编解码，避免改动影响 402 个符号的任务快照顺序协议；关系图及最终安全判断始终由服务端生成。三档窗口、全部主题和影响卡进入纯 Java 视觉审计。
- CodeGraph 复核认证查询组合根影响、任务管理快照 7 个直接下游和 UI 文档 88 个局部符号；完整 Docker 门禁再次通过 56 项任务及 258 张视觉快照，`git diff --check` 通过。最新运行 JAR SHA-256：`a7391ccd8d1348500d9350346610c6ff09bca652f5ca9189317376a02485f8ed`。未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰任何服务器或客户端。
- 完成后语义复核发现 `bq_standard:questcompletion` 奖励也会引用任务；影响分析现同时识别前置集合和 Task/Reward 参数中的 `questUuid`，并记录 `PREREQUISITE / TASK_REFERENCE / REWARD_REFERENCE` 来源。新增测试证明“完成另一任务”的奖励引用同样会阻止不安全退役。
- 上述引用补全后再次执行完整 56 项 Docker 门禁，Quest、PostgreSQL、UI2、258 张视觉快照、根测试、整包和自包含检查全部通过；最终 `git diff --check` 通过，运行 JAR SHA-256：`67f0ad0d5bc151ebdc557420c6cd3da00864d912c4fe96ef1c7bc26acfd28cbd`。

### QB.4 批量退役纯核心与原子服务

- 对照 BetterQuesting `ToolboxToolRemove` 与 `ToolboxToolDelete` 的多选语义，Base 没有吸收客户端修改整库 NBT 后同步的做法；新增 1–128 项有界批量请求、结构化影响报告和服务端原子退役服务。
- 批量影响分析会忽略选中集合内部的前置及 Task/Reward 任务引用，但集合外依赖、已发布章节放置、缺失/非发布目标和结果截断全部阻止退役。越权请求在仓储读取和事务开始前失败。
- `QuestDefinitionBatchRetirementService` 在同一 `QuestTransaction` 内重新读取已发布任务与章节目录、核对所有精确版本，再执行全部生命周期写入；任一 JDBC 写冲突通过异常触发整体回滚，不会像 BQ 客户端包那样留下半批状态。
- CodeGraph 同步后确认批量退役服务影响 53 个符号、扩展后的影响分析器影响 69 个符号；文件级 `affected` 没有推导额外测试，因此显式执行 Quest、PostgreSQL、UI2 与根项目完整门禁。
- Docker 完整 56 项任务通过：全仓 732 项测试（66 项环境条件跳过）0 failure/0 error，258 张视觉快照、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 均通过。运行 JAR SHA-256：`ebbdf6526210146acc0fb263bd5af2041dd94b0d4de11dc1f2fec16dd4433234`。
- 本段只建立可复用的核心合同，尚未接终端协议或正式管理 UI；下一步为有界批量载荷、管理列表多选、确认与完整影响项浏览。未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰任何服务器或客户端。

### QB.4 批量退役终端载荷与管理多选

- 将批量退役接入正式终端链路：`TerminalQuestBatchRetirePayload` 采用 magic、长度和 2–128 项边界，只传任务
  UUID/精确版本、查询上下文、生命周期和页码；客户端不能提交定义、依赖图、身份或“安全”结论。
- `TerminalService` 由当前认证管理员创建核心请求，`QuestModule` 在共享 PostgreSQL 管理器上安装
  `QuestDefinitionBatchRetirementService`；查询、影响预检、版本核对和逐项退役均在同一事务内完成，异常写冲突
  通过事务回滚阻止半批状态。未启用 Quest 运行时或服务未安装时返回失败快照，不产生写入。
- `QuestManagementVisualDocument` 增加显式“多选退役/取消多选”和选中计数；仅已发布条目可进入选择，单击多选行不
  打开详情，提交后清除选择。小视口保留筛选行并通过结构审计，避免批量按钮把控件画到视口外。
- CodeGraph 已同步（1,014 files / 26,939 nodes / 71,432 edges）。`QuestDefinitionBatchRetirementService`
  影响 29 个符号，`TerminalQuestBatchRetirePayload` 影响 32 个符号；文件级 `affected` 未推导额外测试，
  因此显式执行核心、PostgreSQL、UI2、终端和根项目门禁。
- 最新门禁报告：全仓 735 项测试，0 failure/0 error（66 项环境条件跳过）；`ui2-demo:renderGolden` 生成三档
  视觉 PNG/DrawList，`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过。运行 JAR SHA-256：
  `539101997e81f61c19f7495cd6cea8880e60e6443fbf084a6b28598349d15203`。
- 本段未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2。仍待独立批量确认框、完整影响项分页
  与移动/属性批处理；这些完成前不宣称 BQ 替代完成。

### QB.4 批量退役确认浮层收口

- 批量退役按钮不再直接发送破坏性动作；在选中至少两项后打开稳定键
  `quest-admin-batch-confirm`，显示退役数量、服务端依赖/章节/版本复检和单事务说明。
- 模态输入层优先消费 Escape、Enter、遮罩点击和确认区域点击，确认只关闭一次并提交快照中的 UUID/版本；弹窗
  打开期间底层列表与快捷键不会收到输入，选中状态在确认提交后清除。
- `:ui2-terminal:test` 通过（含 Escape/Enter 模态阻断测试）；根 `test assemble verifyUi2SelfContained` 在只读
  预览字体挂载下通过，共 736 项测试、0 failure/0 error，最新运行 JAR SHA-256 为
  `8f0da5b3c23b6ea96ac9e0e26da106f939d0c97432244a189a377b3037ef5268`。
- 当前仍未部署、未执行生产 DDL、未启用 Quest 生产开关。QB.4 剩余工作是完整影响项分页/稳定业务键展示，
  以及批量移动和属性修改；完成前不宣称 BQ 替代完成。

### 2026-09-14 - QC 运行时检测接线收口

- CodeGraph 先复核 `QuestRuntimeService`、`RuntimeQuestFactSink` 与 `QuestGameplayEventHandler`：此前只有定义和
  测试引用，没有生产注册调用，检测事实并未真正进入 PostgreSQL 进度运行时。对 `QuestModule` 与事件处理器执行
  `impact`（分别 67、18 个符号），并以 `callers/callees` 确认缺口后再修改。
- `QuestModule` 现在在共享连接通过 schema 校验后装配 `QuestRuntimeService`、`QuestEvaluationPlanner`、BQ 兼容
  evaluator registry、事实幂等仓储、投影器和引擎；`QuestGameplayEventHandler` 同时注册 FML 总线与 Forge 总线，
  事件与周期观察因此拥有真实生产入口。观察计划由已发布目录生成并以 5 秒窗口缓存，瞬时数据库失败保留上次
  成功计划；重复 `serverStarting` 会先解除旧处理器，避免跨世界重复计数。
- 所有运行时对象仍使用当前服务器身份和 banking source server，客户端、未启用 Quest 配置和缺少共享 JDBC 的环境
  保持失败闭合。新增模块测试断言成功组合根会创建运行时服务，并通过测试覆写阻断静态 Forge 总线初始化。
- Docker 编译与定向 `QuestModuleTest` 通过；随后完整门禁（只读挂载 `/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc`
  为 `UI2_PREVIEW_FONT`）通过：根 `test` 共 738 项、66 项条件跳过，0 failure/0 error，`assemble`、
  `verifyUi2SelfContained` 与 `git diff --check` 均通过。最新运行 JAR SHA-256：
  `70e4f3548b6a78a905910bb991e29f0b939b215c481804df6ccd608b17f4942a`。
- 本轮未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2。下一项为 QC crafting statistics
  模式与扩展检测边界；随后再做库存去重、流体/NBT 矩阵和 BQ 影子差异报告。

### 2026-09-14 - QC crafting statistics 兼容

- 对照 BetterQuesting `TaskCrafting.detect()` 的 `allowCraftedFromStatistics` 分支，补充
  `QuestCraftingStatisticObservation` 与 `QuestObservationPlan` 统计需求集合：只从已发布、明确开启该选项的
  crafting 任务收集实际需求物品，按稳定注册名/meta 去重。
- Minecraft 事实工厂新增原版 `StatisticsFile/objectCraftStats` 绝对累计值读取，使用稳定
  `channel=craft-statistics` 事实；`ItemVectorTaskEvaluator` 对该通道取快照最大值而不是叠加增量，并要求
  `allowCraftedFromStatistics=true`。周期事件每 20 tick 只提交需求集合，避免扫描全物品注册表和无界写入。
- 统计读取仅接受 `EntityPlayerMP`，沿用 FakePlayer、客户端世界、取消事件过滤；当时将 1.7.10 原版 Item ID
  聚合与 meta 维度差异列为待影子对照。此语义已于 2026-09-29 的 M.2 crafting 对照中按 BQ 行为修正：
  `craft-statistics` 通道使用 Item ID 绝对值并忽略 meta/NBT。Base 定时轮询现已移除，但尚未接入 BQ 等价的
  显式 Detect 动作，因此 crafting 仍为 `BLOCKED`。
- 运行时复核发现请求规划器必须与进度仓储共享同一个 `JdbcQuestConnectionManager`，否则规划查询会脱离事实
  事务读取。本次已将 runtime repository、transaction、definition catalog 与 completion query 收口到同一管理器，
  保证“规划—归约—奖励资格”处于同一 PostgreSQL 事务边界。
- `:galaxy-quest-core:test` 通过（新增观察计划与绝对统计快照测试）。随后在只读预览字体挂载下完成完整 Docker
  门禁：`:galaxy-quest-core:test`、`:galaxy-quest-postgres:test`、`:ui2-core:test`、`:ui2-lab:test`、
  `:ui2-terminal:test`、`:ui2-demo:test`、`:ui2-demo:renderGolden`、根 `test`、`assemble` 与
  `verifyUi2SelfContained` 均通过；根共 738 项测试、66 项条件跳过，0 failure/0 error，`git diff --check`
  通过。最新运行 JAR SHA-256：`8b97c0fc938e0db32d93742097b07d17d47e685499ee34bdca466299e706f13f`。
- 未部署、未执行生产 DDL、未启用 Quest 生产开关；下一项为事件取消/FakePlayer 边界、扩展类型注册表、库存去重、
  流体/NBT 矩阵和 BQ 影子差异报告。

### 2026-09-14 - QC 运行时扩展类型目录

- CodeGraph 在编辑前复核 `TaskEvaluatorRegistry`、`QuestEditorTypeRegistry`、`QuestGameplayEventHandler` 与
  `QuestModule` 的调用链；确认此前标准 evaluator 与编辑器描述分别构造，无法让 GTNH 专用类型以同一合同进入
  求值和管理校验。
- 新增纯 Java `QuestRuntimeTypeExtension` 与 `QuestRuntimeTypeRegistry`：以标准 BQ 兼容类型为基线，组合根可
  显式注册成对的 evaluator/编辑器描述；扩展 ID、重复类型、空描述均在组合阶段拒绝。QuestModule 现在让运行时、
  草稿编辑和章节复制共享同一目录，未知类型继续失败闭合。
- 新增核心目录组合与重复注册测试，并在 QuestModule 测试断言生产组合根安装标准目录。未引入 ServiceLoader、反射、
  BQ 类或额外 Mod 依赖；当前没有外部扩展提供者，GTNH 专用类型仍需后续明确语义后显式加入。
- CodeGraph 已同步并执行 `impact QuestRuntimeTypeRegistry`（24 个符号）、`impact QuestRuntimeTypeExtension`
  （29 个符号）、`impact QuestModule`（79 个符号）及实际文件 `affected`；affected 未推导额外测试，故显式执行
  Quest Core、BQ importer 与 QuestModule 定向测试，全部通过。
- 本轮未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2 或客户端。下一项继续处理事件取消/
  FakePlayer 边界、库存快照去重、流体/NBT 兼容矩阵与影子差异报告。

### 2026-09-14 - QC 库存观察去重与服务端玩家边界

- CodeGraph 复核 `QuestGameplayEventHandler`、`MinecraftQuestFactFactory` 的调用链后，收紧事实入口为真实
  `EntityPlayerMP` 且排除 `FakePlayer`、客户端世界；事件取消检查继续在各可取消事件入口执行。
- 新增纯 Java `InventoryObservationEntry` 与 `InventoryObservationCanonicalizer`。背包快照按注册名、meta、
  OreDict 和 portable NBT 合并相同条目，保持首次出现顺序并对数量溢出饱和；不同 NBT 保持独立，避免改变 BQ
  retrieval 的匹配/分配语义。Minecraft 事实工厂现在使用该核心算法，不再维护第二套去重逻辑。
- 新增核心去重、顺序、NBT 隔离和溢出测试；`QuestGameplayEventHandler` 与事实工厂的 CodeGraph `impact` 分别
  覆盖 33/54 个符号，文件级 `affected` 未推导额外测试，故显式执行 Quest Core、根编译与 BQ importer 定向门禁。
- Docker 定向门禁通过：`:galaxy-quest-core:test`、`:galaxy-quest-bq-importer:test`、根 `testClasses`，0 failure。
  本轮未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2 或客户端。下一项为流体/NBT 兼容矩阵
  与 BQ 影子差异报告。

### 2026-09-14 - QD/QE 进度影子差异报告基础

- 新增 `BqShadowProgressAnalyzer`、`BqShadowDifference`、`BqShadowDifferenceReport` 与稳定关系枚举；对只读
  BQ 导入进度和 Base `QuestProgressSnapshot` 按玩家/任务对账，区分 `IDENTICAL`、`BASE_AHEAD`、`BQ_AHEAD`、
  `DIVERGENT`、单侧记录及 `INCOMPARABLE`，并给出完成状态、任务键/类型/向量的具体原因。
- 报告固定最多 4096 行、稳定 UUID 顺序、重复玩家/任务拒绝；没有合并、写库、发奖或接受客户端结论。它是未来
  BQ/Base 并行事实计算报告的可复用证据层，不能替代尚未完成的实时双算接线。
- CodeGraph 在实现前已复核 `BqProgressConflictAnalyzer`、`BqProgressMapper`、`QuestProgressSnapshot` 与
  `QuestEngine` 的关系；文件同步后显式执行 importer 定向测试。`:galaxy-quest-bq-importer:test` 通过，0 failure。
- 本轮未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2 或客户端。下一项仍为流体/NBT 兼容
  矩阵、事件边界及把影子报告接入受控管理诊断入口。

### 2026-09-14 - QC/QE 全量门禁收口

- 在类型目录、服务端玩家边界、库存快照去重和影子差异基础完成后，重新执行 CodeGraph `status/sync`、主要
  符号 `impact` 与实际文件 `affected`，确认没有额外测试被漏选。
- Docker 完整门禁通过：`:galaxy-quest-core:test`、`:galaxy-quest-postgres:test`、`:galaxy-quest-bq-importer:test`、
  `:ui2-core:test`、`:ui2-lab:test`、`:ui2-terminal:test`、`:ui2-demo:test`、`:ui2-demo:renderGolden`、根
  `test`、`assemble`、`verifyUi2SelfContained`；共 744 项测试、66 项条件跳过，0 failure/0 error。
- `git diff --check` 通过；本次运行 JAR SHA-256：
  `12911d81008b2cd51b5cf55a1a1a6aeb6b5f308143ff7baec08ed650339d18b9`。
- 本轮仍未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2 或客户端。当前可继续推进流体/NBT
  兼容矩阵、事件取消边界的细粒度测试，以及把影子报告接入受控管理员诊断入口。

### 2026-09-14 - QE 影子对账边界与流体快照规范化

- CodeGraph 先执行 `status/sync`，并对 `BqShadowProgressAnalyzer`、`FluidObservationCanonicalizer` 与
  `MinecraftQuestFactFactory` 执行 `impact/affected`。影子分析器现在拒绝 PARTY/TEAM/PUBLIC 快照进入玩家对账，
  防止把群体主体 UUID 误当成玩家；任务类型缺失、向量宽度不一致和任务单侧缺失统一标记为
  `INCOMPARABLE`，不再错误归类为某一侧领先。
- 新增纯 Java `FluidObservationEntry` 与 `FluidObservationCanonicalizer`。流体库存事实按流体名＋portable NBT
  合并，保留首次出现顺序和诊断用原始 NBT，数量使用饱和加法；Minecraft 适配器改用该核心算法，匹配器的
  requirement allocation 语义不变。
- 新增影子组主体、任务类型/向量形状及流体名称/NBT/溢出测试。Docker 定向门禁后又完成 Quest Core、
  PostgreSQL、BQ importer、UI2、终端、视觉导出、根 `test`、`assemble` 和 `verifyUi2SelfContained` 全量门禁：
  共 748 项测试、66 项环境条件跳过，0 failure/0 error；`git diff --check` 通过。最新运行 JAR SHA-256：
  `11748467d73adcf615c77f3f97774081ae1071b120acb9a4bf9d7fd4523277d9`。
- 本轮未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2。影子报告仍是只读证据层，下一步
  可接入受控管理员诊断入口，并继续补 fluid/NBT 行为矩阵与实时 shadow 计算接线。

### 2026-09-14 - UI2 与跨服任务平台稳定检查点收口

- 冻结功能范围并完成 Git 审计：558 个非构建未跟踪文件按 UI2、Quest Core、PostgreSQL、BQ importer、
  Minecraft/终端适配、测试、许可证和文档分类；四个新增子项目补齐独立 `build/` 忽略规则。代码、构建、
  许可证和测试以明确路径暂存，没有使用无审计的 `git add -A`。
- CodeGraph 已同步并复核 UI2 Screen/Container 双宿主、`QuestModule`、`QuestRuntimeService`、
  `TerminalService` 与 `BqShadowProgressAnalyzer`。全仓搜索确认正式生产源码不再引用旧 Canvas/Panel 工厂；
  BQ 影子分析器没有生产调用者，仍是离线只读证据层。
- Docker 门禁通过 56 项 Gradle 任务：三个 Quest 模块、四个 UI2 模块、258 张终端视觉快照、根 `test`、
  `assemble` 和 `verifyUi2SelfContained` 均成功；测试报告合计 748 项、66 项环境条件跳过、0 failure、
  0 error，`git diff --check` 通过。
- 运行 JAR 为 `build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 为
  `11748467d73adcf615c77f3f97774081ae1071b120acb9a4bf9d7fd4523277d9`。内容审计确认包含 UI2、Quest Core
  和 PostgreSQL 运行代码，不包含 BQ importer、BetterQuesting 类、Gson 导入 CLI、BQ GUI/Packet/JSON 存储。
- 代码稳定检查点提交为 `4f51c92`（`feat: 完成UI2终端与跨服任务平台稳定检查点`）。本次没有部署、没有执行
  PostgreSQL migration、没有启用 Quest 生产开关、没有停用 BetterQuesting，也没有触碰 Lobby/S1/S2/客户端。
- 后续唯一权威进度见 `betterquesting-capability-absorption-matrix-2026-09-13.md`。PARTY/TEAM/PUBLIC 奖励归属、
  管理诊断与受控修复、幂等迁移/回滚、实时影子双算和自动切换证据均明确未完成；历史各段“下一步”只保留
  为当时记录，不再作为当前计划依据。

### 2026-09-16 - PARTY/TEAM/PUBLIC 跨服主体与逐成员奖励归属收口

- PostgreSQL 新增稳定 PARTY/TEAM/PUBLIC 主体、成员关系、`OWNER / ADMIN / MEMBER`、乐观修订、离队、移除、
  所有权转让与归档合同；玩家每种主体类型最多一个活跃成员关系。任务定义的 scope 由服务端选择唯一进度
  主体，事实不会因多重成员关系重复推进。
- 组任务完成或进入重复周期时，在同一事务中冻结当时活跃成员并生成逐玩家 entitlement。后来加入者不补领
  旧周期，离队者保留既得资格；离线交付进入延期而不消耗失败预算。物品、XP、计分板和命令交付统一解析为
  具体 PLAYER 收件人，禁止把组 UUID 当成玩家。
- 领取批次增加服务端 `RewardClaimTarget`，精确包含原进度主体与周期。终端按认证玩家的
  `recipient_player_id` 显示最早尚可领取批次，即使已经离队仍可操作；领取和 Choice 不接受客户端玩家身份，
  并避免不同队伍恰好同周期时串领。终端详情显示奖励来源 scope 与周期。
- 命令奖励默认拒绝，只接受显式根命令白名单；空命令、控制字符、超长命令和前缀伪装均拒绝。任意命令的
  execute/marker 崩溃间隙仍不能承诺 exactly-once，文档明确要求仅配置自身可幂等的审计命令。
- 新增的 `20260914_001_add_cross_server_quest_platform.sql` 是真实增量迁移而非全量 DDL 复制：旧 PLAYER
  entitlement 会回填收件人，旧非 PLAYER 且无法推导收件人的数据失败闭合；唯一键、消费主体检查及失败计数
  均可重复应用。迁移在隔离 PostgreSQL 16 上从旧提交 DDL 连续执行两次通过；未对生产数据库执行。
- CodeGraph 同步后对 `AuthenticatedParticipantAdministrationService`、`RewardClaimTarget`、
  `JdbcRewardClaimRepository` 和 `JdbcQuestCenterQuery` 执行 impact；奖励目标影响 30 个符号，领取仓储影响
  20 个符号，任务中心查询影响 48 个符号。显式覆盖 Quest Core、PostgreSQL、Minecraft 奖励适配、终端网络、
  UI2 主体管理和根项目回归。
- 隔离 PostgreSQL 16 的真实集成测试通过，覆盖离队奖励可见、后加入者无旧资格、同周期不同主体隔离、
  成员冻结、Choice、租约交付和延期重试。完整 Docker 门禁通过 56 项 Gradle 任务；当前报告合计 776 项测试、
  52 项环境条件跳过、0 failure/0 error，258 张视觉快照、`assemble`、`verifyUi2SelfContained` 与
  `git diff --check` 均通过。运行 JAR 为
  `build/libs/jsirgalaxybase-a8ca03e-main+a8ca03eb8d-dirty.jar`，SHA-256：
  `fb9cc0983b7c6ec4aa58f88cd221469d245128a662bd664a91eb9a41fb158540`。
- 本批未部署、未执行生产 DDL、未启用 Quest 生产开关、未停用 BetterQuesting，也未触碰 Lobby/S1/S2/客户端。
  后续仍是管理员诊断与受控修复、迁移 dry-run/回滚、实时影子双算及正式切换证据；这些不属于本批完成声明。

### 2026-09-17 - BetterQuesting 内容兼容与迁移路线定稿

- 新增独立路线文档 `betterquesting-content-compatibility-migration-roadmap-2026-09-17.md`，明确暂停职业、贡献等
  新玩法，先完成现有 GTNH BQ 内容的定义导入、行为兼容、玩家进度合并、影子双算和可回滚切换。
- 核对当前实现：`galaxy-quest-bq-importer` 已能只读解析 `DefaultQuests/Quests`、`QuestLines`、
  `QuestLinesOrder.txt` 和玩家 UUID 进度 JSON，但当前只生成内存模型与报告，没有 PostgreSQL 写入、迁移批次、
  dry-run/回滚或生产调用；importer 继续不进入运行 JAR。
- 下一实施点固定为 M.1 定义与章节迁移闭环：稳定清单、兼容矩阵、严格校验、隔离 PostgreSQL 幂等提交与
  整批回滚、UI2 导入预览，以及导入后玩家任务中心的完整浏览。玩家进度、事实运行、奖励、部署、生产 DDL
  和 BQ 停用均明确不在 M.1 范围。
- 本次仅修改文档，没有代码、构建、数据库、部署或运行时状态变化。

### 2026-09-17 - BQ 定义迁移 M.1 离线清单开工

- CodeGraph 索引为最新；沿 `BqQuestDefinitionImporter`、`BqQuestChapterImporter`、定义/章节 PostgreSQL
  Repository、`TerminalService` 与 `QuestManagementVisualDocument` 六条入口核对影响边界。
- 新增确定性 `BqMigrationManifest`/`BqMigrationPlanner`：同一拆分任务书生成稳定批次哈希，并失败闭合检查未知
  Task/Reward、重复 UUID、缺失/循环前置和悬空章节引用；未放入章节的任务以明确警告保留。
- Docker `:galaxy-quest-bq-importer:test` 通过（23 项）；没有读取生产任务目录、连接数据库、执行 DDL、导入
  玩家进度、发放奖励、部署或修改 BetterQuesting。下一步是隔离 PostgreSQL staging/dry-run 与原子提交合同。

### 2026-09-17 - BetterQuesting 替代下一长期目标交接单

- 新增 `betterquesting-replacement-goal-handoff-2026-09-17.md`，将“完成 BQ 替代”拆成定义/章节迁移、行为兼容、
  玩家进度迁移、诊断修复、影子双算和灰度切换六段，避免把尚不能一次验收的工作误设为单一无限目标。
- 交接单明确当前工作树已有 M.1 实现但尚未完成全量验收、提交与推送；下一目标必须延续现有实现收口，禁止
  另起平行 importer 或把未验收代码写成已完成。
- 固化可直接设立的 M.1 长期目标、自动完成判据和禁止边界。本次只更新文档与索引，没有读取生产 BQ 目录、
  执行数据库变更、部署、启用 Quest 或停用 BetterQuesting。

### 2026-09-17 - BQ 定义与章节迁移 M.1 自动收口

- 完成确定性迁移清单、逐元素兼容矩阵和失败闭合校验：覆盖重复 UUID、缺失/循环前置、章节悬空与重复放置、
  顺序缺失、资源引用边界、未知类型，并复用正式 `StandardQuestEditorTypeRegistry` 校验 13 类任务和 6 类奖励
  的归一化参数。原始 BQ JSON 继续保存在离线迁移载荷中，不进入 Base 运行时读取路径。
- 新增明确分离的 `dry-run / preview / apply / rollback`：dry-run 零写入；preview 只在显式隔离写入确认后记录
  PREVIEW 或 BLOCKED；apply 以 PostgreSQL advisory lock 和事务完成 CREATED/VERSIONED/UNCHANGED；相同批次
  幂等，内容变化受控升版，整批回滚受后续版本与依赖保护。
- 新增审计表增量迁移 `20260917_001_add_quest_content_migration_audit.sql`，只在隔离 PostgreSQL 16 中从空审计
  表连续执行两次验证；未执行生产 DDL。真实集成测试覆盖故障事务回滚、阻塞预览、升版恢复、章节图标/背景、
  前置、目标、奖励和玩家任务中心浏览。
- UI2 管理终端接入最近迁移状态、来源哈希、创建/升版/不变计数、错误/警告和有界诊断；三档终端尺寸的迁移
  预览结构审计通过。CodeGraph 已同步，并复核规划器、数据库服务、审计仓储、管理文档和 `TerminalService`
  的 impact/affected；显式门禁覆盖了 CodeGraph 未能从新增文件推导的 importer 与 UI2 测试。
- Docker 完整门禁通过：Quest 三模块、UI2 四模块、视觉导出、根 `test`、`assemble` 和
  `verifyUi2SelfContained`；共 788 项测试、71 项环境条件跳过、0 failure/0 error，258 张视觉快照均为
  `SUMMARY|issues=0`，`git diff --check` 通过。代码提交为 `b963344`。
- 提交后先显式生成 UI2 子模块 JAR，再通过根 `assemble` 与 `verifyUi2SelfContained`（规避旧 Forge 转换器在
  新版本名首次构建时的任务排序竞争）。运行 JAR 为
  `build/libs/jsirgalaxybase-b963344-main+b963344e96-dirty.jar`，SHA-256：
  `0e5176f01227abbef1948a3eaa2f24c9487b279142f5362ebbd3fc31bcefdd88`；内容审计确认没有 importer、Gson CLI、
  BetterQuesting 类、BQ GUI/Packet 或 JSON 存储，仅保留来源声明。
- 本轮没有读取生产 BQ 目录、导入玩家进度、发放奖励、部署、启用 Quest 生产开关、停用 BetterQuesting，
  也未触碰 Lobby/S1/S2/客户端。M.1 只完成内容迁移闭环；下一阶段仍是 M.2 Task/Reward 行为兼容。

### 2026-09-23 - M.2 行为兼容清点入口与失败闭合口径

- 核对当前工作树与 CodeGraph：M.1 已在 `d27d2ee` 收口，任务/奖励已有标准类型解析和 Base evaluator，但还没有
  用真实 GTNH 任务书证明行为等价。发现的 S2 和客户端 `DefaultQuests` 属运行目录，本轮未读取；已向用户索取
  明确的只读副本路径。
- 新增 `BqBehaviorInventory`：只接受调用方提供的迁移清单，按实际使用次数降序输出 Task/Reward 类型、参数键
  频率、关键行为开关真假分布和来源哈希。最初实现曾保留最多三个任务 UUID；2026-10-11 已删除该身份样本及盘点入口的逐 Quest 输出，当前行为画像只输出匿名聚合。行为状态默认 `BLOCKED`，未知类型为 `UNKNOWN`；离线 inventory 命令
  明确区分行为结论与旧 `ADAPTED` 内容解析结论。
- 修复通用编辑器参数错误未传递到逐元素兼容矩阵的问题：`INVALID_ELEMENT_PARAMETER` 对应的 Task/Reward
  现在会显示 `BLOCKED`，新增回归断言。
- 对照本地 BQ `TaskRetrieval.Detector` 与 Base `InventorySnapshotTaskEvaluator`，记录 `groupDetect`、
  活跃成员库存、非消耗部分进度重置和 `autoConsume` 差异；详见独立 M.2 工作矩阵。本地 BQ 参考目录无独立
  Git 元数据，固定源码提交仍需后续验证。
- Gradle 配置阶段曾两次在线和一次 `--offline` 在 `com.gtnewhorizons.gtnhconvention` 插件应用时发生
  `SocketTimeoutException`；随后重试成功，Docker `:galaxy-quest-bq-importer:test` 通过。手工 Java 8
  编译与 JUnit 定向运行的 6 项测试也通过。未执行生产读取、数据库迁移、部署或 Quest 切换。

### 2026-09-23 - M.2 服务端任务定义只读清点与章节误判修正

- 用户随后明确授权直接读取服务端任务文件。仅将 S2 `DefaultQuests` 复制到 `/tmp` 作只读离线分析；
  未读取玩家进度或数据库，未修改服务器文件。确定性清单来源哈希
  `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，共 3,744 个任务、47 个章节。
- 实际 Task 次数：retrieval 3,863、optional_retrieval 1,267、checkbox 203、crafting 133、hunt 104、
  location 33、fluid 15、block_break 1、meeting 1；Reward 次数：item 4,181、choice 957、
  questcompletion 308、xp 2。完整关键开关分布及行为阻断状态见独立 M.2 矩阵。
- BQ 每个章节有独立的任务条目数据库，故同一任务可以显示在多个章节。修复 M.1 规划器把 167 次
  跨章节放置误报为错误的问题；同一章节内重复仍阻断，新增两章节同任务适用测试。
- 修正后真实清单仍不可应用：36 个元素参数错误、2 个奖励参数错误、18 条前置环检测结果；另有 64 个
  未放入章节的警告。下一步逐样本核对 BQ 原义，不直接放松校验。Docker
  `:galaxy-quest-bq-importer:test` 重新通过；未部署、迁移或切换现网 Quest/BQ。
- 进一步核对 BQ `TaskLocation`：`range <= 0` 表示同维度不限距离，Base 位置检测器已有相同处理；
  编辑器原来却要求正数。改为允许整数范围并测试 `-1`。真实清单中的 33 条位置范围误判消失；剩余 3 条
  元素错误（两条空物品奖励、一个方块数量 0）、2 条对应的奖励错误及 18 条前置环检测结果仍阻断。
  Docker `:galaxy-quest-core:test` 与 `:galaxy-quest-bq-importer:test` 均通过。
- CodeGraph 已同步；`StandardQuestEditorTypeRegistry` impact 覆盖规划器、QuestModule 和核心测试。
  完整 Docker 门禁 `test assemble verifyUi2SelfContained` 通过，测试 XML 汇总 791 项、76 项条件跳过、
  0 failure/0 error；`git diff --check` 通过。首次完整门禁因未挂载 Java2D 预览字体及 Gradle 配置缓存
  与构建脚本任务不兼容而失败，改用现有脚本同样的只读字体挂载、`--no-configuration-cache` 并先生成
  UI2 子模块 JAR 后重跑通过。当前未提交运行 JAR 为
  `build/libs/jsirgalaxybase-d27d2ee-main+d27d2ee371-dirty.jar`，SHA-256
  `f275bbcb83981b7eb853a5585cfc8d9ed7f97cd480dbc418d6c4538ee44fbd0e`；未部署。

### 2026-09-23 - M.2 高频物品检测进度语义对照

- 对照本地 BQ `TaskRetrieval.Detector`，修复 Base 非消耗库存快照把已达标的单个物品需求降回去、并让其
  继续占用后续需求库存的问题；这同时作用于 `retrieval` 与继承同一 BQ 行为的 `optional_retrieval`。
  增加已达标保留、未达标回落、同物品跨需求重分配测试。
- `groupDetect=true` 的 BQ 语义需要活跃队伍成员库存，单人库存快照不足以证明，因此暂不推进这类任务，
  避免误完成。整体类型仍标 `BLOCKED`，消费、组队、自动消耗和重复周期尚未签收。
- CodeGraph 同步后 `impact InventorySnapshotTaskEvaluator` 为 19 个符号，`affected` 指向
  `BqCompatibleTaskEvaluatorsTest`。本轮 Docker Gradle 三次均在 GTNH convention 插件应用时超时，
  未进入测试；改用同容器的 Java 8 编译和 JUnit 4.13.2 运行该测试类，19 项通过。
  `git diff --check` 通过。本轮没有重新完成全量 Gradle 门禁、生成新 JAR、部署、迁移或切换现网任务系统。
- 对照 BQ `RewardItem.claimReward0`：空物品列表为 no-op。Base 的物品交付规划也支持空列表，故仅对
  `bq_standard:item` 放开空列表校验，`choice` 仍要求至少一个选项；新增编辑器、导入规划器和交付规划测试。
  真实清单复算后只剩 1 个方块数量 0 参数错误及 18 条前置环检测结果。BQ 背包满时掉落、Base 保留
  可重试权益的安全策略差异已记录，不宣称物品奖励整体等价。
- Gradle 再次在插件初始化时报 `Network is unreachable`，故使用同一 Docker Java 8/JUnit 环境定向
  编译改动并运行 30 项核心和导入测试，全部通过；Minecraft 交付测试的新增断言尚待 Gradle 根测试复核。

### 2026-09-23 - M.2 方块零目标与 XOR 互斥章节对照

- 真实快照仅有一条方块任务，配置为“原木 ×1 + 空气 ×0”。BQ `TaskBlockBreak` 对 0 目标视为已满足；
  Base 向量同样可表示。只放开方块列表 0 数量，物品/流体 0 仍非法，负数量仍阻断；补验证与进度测试。
- 用只读快照构建前置强连通组：4 组共 12 个任务均是 XOR，且各成员有组外前置入口。发现导入器之前
  只区分 AND/OR，把 XOR 静默读成 AND。补齐 BQ 六种逻辑的真值表、严格导入与 QuestEngine 测试；
  迁移规划器改为强连通组检测，只允许有外部入口的 XOR 互斥组作为警告，其余环仍阻断。
- 修正后真实定义清单 `applicable=true`，4 条 `SEEDED_XOR_CYCLE` 警告，无结构错误；这不是任务行为
  已兼容的证明。离线 CLI `apply` 额外检查 M.2 行为清单，未签收的 Task/Reward 继续拒绝发布。
  Docker Java 8/JUnit 定向编译与 47 项核心、导入测试通过；完整 Gradle/Minecraft 测试仍待恢复。
- 以上仅从用户授权的服务端任务定义临时只读副本取证；未读玩家进度、未连接生产数据库、未部署、
  未启用 Quest 或停用 BetterQuesting。
- 随后 Docker 完整门禁恢复并通过：`:galaxy-quest-core:test`、`:galaxy-quest-bq-importer:test`、根
  `test`、`assemble`、`verifyUi2SelfContained`；测试 XML 汇总 802 项、76 项条件跳过、
  0 failure/0 error，`git diff --check` 通过。CodeGraph 已同步；`QuestLogic` impact 51 个符号，
  `BqMigrationPlanner` impact 38 个符号，覆盖核心引擎、PostgreSQL 编解码、导入器、终端消息和对应测试。
  未提交运行 JAR SHA-256 为 `dc57191c0c1894a1bc53e879193501700f35fd713d1e18bbc8575fcba41de5ac`；
  本轮不部署。
- 为避免结构清单已适用时误用 CLI `apply`，将行为门槛移至创建数据库服务之前；新增不提供 JDBC
  配置仍必须首先报 M.2 未签收的测试。内部 M.1 隔离数据库服务保留内容迁移测试合同；正式 CLI 仍阻断。
- 门槛调整后重新执行 Docker 根 `test assemble verifyUi2SelfContained` 成功；测试 XML 合计 803 项、
  76 项条件跳过、0 failure/0 error，`git diff --check` 通过。运行 JAR 内容未因离线 importer 改动而变化，
  SHA-256 仍为 `dc57191c0c1894a1bc53e879193501700f35fd713d1e18bbc8575fcba41de5ac`；未部署。

### 2026-09-29 - M.2 手动消耗型物品检索受限实现

- 用户确认直接读取服务端 quest 文件。只读复核 S2 `config/betterquesting/DefaultQuests`，将该目录只读挂载至
  隔离开发容器，未读玩家进度/物品、数据库，未改服务端文件；清单仍为 3,744 quests、47 chapters，来源
  清单哈希 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。3,863 项 retrieval 中
  307 项 `consume=true` 且全部 `autoConsume=false`；1,267 项 optional_retrieval 全部不消耗。
- 终端接入“提交并消耗”动作：服务端使用已认证玩家身份、重新验证发布任务和剩余进度，仅允许 PLAYER、
  不可重复、手动 item retrieval；按真实实际扣除量逐项推进进度。增加玩家存档 marker 与消费 saga 的登录恢复，
  只在原始 source server 恢复该玩家待处理记录，每次最多 64 条；没有新增数据库结构。
- CodeGraph 先查 `QuestModule`、`TerminalService`、消费 authorizer/port 与 repository 的调用影响；修订后
  执行 `codegraph sync`（本轮变更 21 个代码文件），并复核 `AuthenticatedQuestConsumptionService`、
  `ConsumptionRequestAuthorizer`、`MinecraftPlayerConsumptionPort`、`TerminalQuestCenterSnapshotMapper`、
  `TerminalActionType` 的 impact。`codegraph affected` 未解析出测试依赖（0 个候选），故依据 impact 结果显式
  执行根 Minecraft 适配、quest core、PostgreSQL、终端消息和 UI2 视觉模型相关测试。
- Docker 定向门禁 `:galaxy-quest-core:test :galaxy-quest-postgres:test :ui2-terminal:test :test` 通过；
  根测试 514 项、52 项条件跳过、0 failure。PostgreSQL 运行集成用例所需环境变量为空，故数据库集成部分按
  测试约定跳过；这次没有生产数据库连接或 DDL。`git diff --check` 待最终收口再记录。
- M.2 和 BQ 替代整体仍为 `BLOCKED`：物品注册/NBT 匹配、真实 GTNH 消耗时序、群组/自动/重复任务、流体/XP、
  跨服恢复以及任务与奖励端到端验收未签收。未部署、未启用生产切换、未停止 BetterQuesting，也未触碰 S1、S2 运行状态。
- 补挂开发专用 CJK 字体（只读，不进入 JAR）后重新通过完整门禁：三个 Quest 模块、四个 UI2 模块、根 `test`、
  `assemble` 与 `verifyUi2SelfContained`；测试 XML 汇总 815 项、77 项条件跳过、0 failure/0 error，
  `git diff --check` 通过。首次运行 UI2 Demo 时未挂载预览字体导致 2 项预期失败，按视觉脚本只读挂载后通过。
  PostgreSQL 测试环境变量留空，需数据库的集成测试按约定跳过。本轮构建 JAR
  `build/libs/jsirgalaxybase-d27d2ee-main+d27d2ee371-dirty.jar`，SHA-256
  `4f168f7bba692dcf1eeb1c1f3534aa7cc00ac3237dd4d608e2e640759af38d37`；未部署。

### 2026-09-29 - 修复 BQ NBT 数字布尔标志解析

- 对照只读 DefaultQuests 的已统计配置确认 BQ 把布尔值编码成数字 0/1。旧运行时把字符串 1 交给
  Boolean.parseBoolean 会得到 false，可能反转 consume、NBT、部分匹配、组检测及 crafting 统计开关。
- 新增 Core QuestBooleanParameter 统一解析 numeric/string booleans；Importer 将识别出的行为布尔项规范为
  true/false、保留 bq.raw 原文，并对未知布尔文本拒绝导入。补了核心 evaluator、观察计划、Importer 编解码回归测试，
  扩充真实行为清单对布尔项的归一化统计键。
- :galaxy-quest-core:test 与根定向 MinecraftPlayerConsumptionPortTest + MinecraftRewardDeliveryTest
  （16 项）通过。Importer Gradle 全量测试在最后增加 scoreboard relative 断言前曾通过；之后 Gradle 在线/离线
  多次在 GTNH convention 插件初始化时超时。最终源码状态下使用同一 Docker JDK 容器 javac --release 8 重编全部
  importer 主源码和三个受影响测试类，JUnit 4.13.2 共 11 项通过。一次初始 Core 测试因新测试夹具类型设置错误失败，
  修复夹具后复跑成功；根测试筛选最初误作用于所有子项目，改为显式 :test 后 16 项通过。
- CodeGraph 同步；QuestBooleanParameter.parse impact 59 个符号，Importer codec impact 包含 4 个回归测试；
  codegraph affected 返回无测试候选，因此按影响链显式运行 Core、Importer 和根 Minecraft 适配测试。最后源码/文档修订后的 git diff --check 通过。
- 真实快照有 228 个“重复 + 手动消耗”的 retrieval，当前受限授权器拒绝重复任务；明确继续 BLOCKED，需先设计
  周期绑定 saga/marker/进度事实。本轮只读任务定义；未读取玩家进度/资产，未连接或改生产数据库，无 DDL、部署、Quest 开关或 BQ 停用操作。

### 2026-09-29 - BetterQuesting crafting statistics Detect 接入（受限、未实机签收）

- 用户确认可直接读取服务器 quest 定义。只读检查 S2 `config/betterquesting/DefaultQuests`：133 个 crafting
  任务中 6 个启用 `allowCraftedFromStatistics`；不读取玩家进度、背包或数据库，不改服务器文件。
- 对照本地 BQ 源码后，为 Base 增加经服务器认证的个人任务显式“检测合成”动作、Minecraft lifetime craft
  statistic reader、快照型幂等事实、重复周期支持、任务/版本/周期精确路由与 UI 显示资格判断。共享任务被拒绝。
- CodeGraph `sync` 后检查 detection service、Minecraft reader、snapshot mapper、approved terminal packet 的影响；
  `affected` 未自动找到根测试候选时，显式执行 Core detection 测试及根 Mapper、QuestModule、TerminalService、
  Quest Center codec/model tests。
- 结果：Core 新服务 5 项通过；根相关 5 个测试类共 49 项通过；`:ui2-terminal:test` 通过；根
  `compileJava compileTestJava` 通过。`git diff --check` 在最终文档更新后待执行。未跑全量测试/assemble，未部署。
- 保持 M.2 状态 `BLOCKED`：统计桥尚未实机确认；BQ 的 Quest 级 Detect 与当前 Base 任务级按钮不一致；共享主体
  和统计跨服扇出未实现。没有生产开关、DDL、部署或 BetterQuesting 停用操作；不触碰 S1/S2 运行状态。

### 2026-09-30 - M.2 跨服 PARTY 活跃库存租约（代码完成，未生产启用）

- 用户确认可直接读取服务端 quest 定义。仅对 S2 `config/betterquesting/DefaultQuests` 做只读扫描：3,744 个任务、
  47 个章节、3,847 个放置；来源清单哈希 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。
  retrieval 的 `groupDetect=true` 共 4 项；optional_retrieval 为 0 项。未读取玩家进度、背包、其它服务端配置或数据库，
  未改服务器文件。
- 观察计划现仅为导入且 `groupDetect=true` 的非消耗 retrieval 启动 Party 快照租约。Minecraft 登录时创建随机 session；
  每 20 ticks 更新时携带同一 token，服务端按 15 秒有效期判定在线快照。先锁定当前 PARTY 行、再查询成员并汇集仍有效的
  跨服成员快照；人数超过 512 的 PARTY 不生成聚合。个人事实、聚合事实及 runtime 处理在同一 PostgreSQL 事务中。
  旧服务器 session 的 tick/logout 无法覆盖或删除新 session；库存事实账本已裁剪物品明细，短期租约仅含 evaluator 所需字段，
  不保存原始 NBT 文本。新表及到期索引的 DDL/迁移已加入代码库，但没有在任何数据库执行。
- CodeGraph 同步后，`QuestObservationPlan.fromPublished` impact 59 符号、`QuestGameplayEventHandler.onPlayerTick`
  36、租约 `publish` 4、`JdbcQuestRuntimeRepository.recordFactIfAbsent` 61；`affected` 未自动推导测试文件，按影响链
  显式运行 Core 与 PostgreSQL 测试。
- 隔离 Docker PostgreSQL 16 上 `:galaxy-quest-postgres:test` 38 项通过，PG 用例 0 跳过；随后为回归
  “单玩家快照不得推进 BQ groupDetect”而新增/修订核心用例，使用 Docker JDK 8 兼容编译参数重新编译 Core
  全部主源码与测试源码，并以 JUnit 4.13.2 实跑 205 项，全部通过。PG 集成库为本轮专用临时容器，不是
  `galaxy-base`。根 `compileJava compileTestJava` 与定向 `QuestModuleTest` 5 项曾在前次任务调用中通过。
  本次尝试刷新 Gradle 全部定向任务时两次在 GTNH convention 插件初始化阶段 `SocketTimeoutException`；离线
  LOCAL 方式因仓库没有 `gtnhShared/spotless.gradle` 也无法配置。故本轮不宣称根 `test`、`assemble` 或
  自包含门禁通过；最终 `git diff --check` 待文档收口后复核。
- retrieval/optional_retrieval、整个 M.2 和 BQ 替代仍为 `BLOCKED`：新 producer 尚未真实多服运行验证，生产迁移未执行、
  Quest 生产开关未启用，GTNH 物品/NBT、断线/切服、保存恢复及奖励事务仍待验证。本轮未部署、未连接生产数据库、
  未执行 PostgreSQL DDL、未停用 BetterQuesting，也未操作 S1/S2 服务状态。

### 2026-09-30 - 修正 Minecraft 消耗适配器的 BQ NBTTagList 重复匹配

- CodeGraph 在复核 `MinecraftPlayerConsumptionPort.compareNbt` 时发现运行时消耗路径与 Core 的 `PortableNbtMatcher`
  行为不一致：BQ `ItemComparison.CompareNBTTag` 对普通 `NBTTagList` 的每个需求条目都重新扫描完整样本列表，允许同一
  compound 满足重复需求；byte/int array 则使用 `usedIdxs`，不允许复用。Base 的消费适配器此前把普通列表也做成一对一，
  会拒绝真实检索配置中出现的重复 compound 列表形状。
- 消费适配器现与 BQ 一致：普通列表匹配不消费样本下标，byte/int array 保留唯一匹配。回归覆盖重复 compound 列表正例、
  byte/int array 重复项反例。CodeGraph 同步 2 个 Java 文件；`impact MinecraftPlayerConsumptionPort.compareNbt` 覆盖
  matcher、item/fluid 调用者，`affected` 命中 `MinecraftPlayerConsumptionPortTest`。
- 先用离线 LOCAL no-op Spotless shim 配置，再强制重跑 `:galaxy-quest-core:test`（205 项）、
  `:galaxy-quest-postgres:test`（38 项、隔离 PostgreSQL 16、0 跳过）、`:galaxy-quest-bq-importer:test`（38 项、5 项
  条件跳过）、根 `compileJava compileTestJava` 和 QuestModule 定向测试（5 项），全部通过；之后根源码/测试源码再次编译，
  `MinecraftPlayerConsumptionPortTest` 8 项通过。外部 Gradle 插件网络可用时全量定向任务成功；临时 shim 只用于绕过缺失的
  `gtnhShared/spotless.gradle`，不进入产品改动。全量根 `test`、`assemble` 和自包含门禁未运行。
- CodeGraph 最终索引为 up to date，`git diff --check` 通过；临时 Spotless shim、PostgreSQL 容器及其专用网络均已删除。
  未执行生产 DDL、部署或 Quest 开关；retrieval / optional_retrieval 与整体 M.2 仍为 `BLOCKED`：仍需多服实机、
  Forge/GTNH 真实物品 NBT、玩家断线/切服与奖励闭环证明。

### 2026-09-30 - M.2 高频 item 奖励数量与背包堆叠边界核验

- 只读 S2 `DefaultQuests/Quests`，仅输出聚合统计、不打印 Quest 名称、UUID 或物品 NBT：4,181 个 `bq_standard:item`
  奖励包含 6,271 个栈定义；Count 全为正数，15 个大于 64、11 个大于 127，最大为 8,192。因此 BQ
  `BigItemStack.getCombinedStacks()` 对非正数量至少发一个的异常边界与当前任务内容无交集。
- 对照 BQ `BigItemStack.getCombinedStacks()` 与当前 1.7.10 `InventoryPlayer.addItemStackToInventory`：前者按物品
  max stack 形成输入栈，但 Player Inventory 每槽 `getInventoryStackLimit()` 固定为 64，最终会继续拆分；Base 规划器
  直接按 64 上限分槽，因此正常可容纳时结果等价。对 8,192 个物品这类超出主背包容量的奖励，BQ 将剩余项掉落到世界，
  Base 则事务式拒绝部分插入并保留待交付；这是有意的跨服安全差异，避免掉落实体与租约确认之间崩溃导致重复或丢失。
- 新增 Minecraft 奖励规划回归：Item 最大堆叠 128、奖励 130 时仍按玩家槽上限落为 64/64/2；真实最大观察数量 8,192
  放不进空的 36 格主背包时，全量计划失败且原库存不变。CodeGraph `impact MinecraftItemRewardDeliveryHandler.plan`
  覆盖交付规划与 handler，`affected` 命中 `MinecraftRewardDeliveryTest`。在临时 LOCAL Spotless shim 下强制重跑根
  `compileJava compileTestJava` 和该测试类，11 项全通过；shim 已删除，`git diff --check` 通过。
- item reward 仍标 `BLOCKED`：本轮验证配置数量边界和纯规划算法，不等价于实际 GTNH ItemStack/NBT、player NBT marker
  与进程重启恢复的端到端证明；没有部署、生产 DDL、Quest 开关或现网任务系统切换。

### 2026-09-30 - M.2 超容量物品奖励可续领

- 对照本地 BQ `RewardItem`：BQ 尝试放入背包后会把剩余栈掉到世界；Base 原子拒绝整笔交付会让真实配置中的 8,192 件奖励永远无法进入 36 格背包。为避免跨越玩家存档与数据库租约的非原子世界掉落，改为安全分批交付：先发放可容纳部分，在同一玩家持久数据中保存每个拆分奖励栈的已发数量，余量使用非耗尽次数的 deferred 重试；完成后写入原有稳定 entitlement marker 并清理进度。若玩家数据 flush 失败，回滚本次背包和 marker/progress 变化。
- 新增部分插入计划测试，36 格空背包可先接收 2,304/8,192，再在后续有空间时继续；新增 NBT 进度类型与清零测试。保留旧原子 `plan` 作为兼容合同。CodeGraph 同步 3 个修改文件，`impact MinecraftItemRewardDeliveryHandler.deliver` 命中 handler、规划 helper、奖励测试和 `QuestModule` 注册点，`affected` 命中奖励测试。
- Docker Gradle 使用既有 `/gradle-home` 缓存；共享生成目录原有 root-owned 结果导致普通用户首跑被拒，先以容器 root 执行验证并将涉及的 `build/` 生成产物所有权恢复为 UID 1000，之后普通开发用户重跑根定向 suite 通过。根项目 `MinecraftRewardDeliveryTest` 13 项、`:galaxy-quest-core:test` 206 项通过，均零跳过/失败/错误；`git diff --check` 通过。临时 `/tmp/gradle-home` 首次只因下载 Gradle 发行包超时，未进入构建；随后使用持久缓存成功。
- 差异明确保留：这不是 BQ 的“立即掉落溢出物”行为等价，玩家需要稍后腾出空间；分批恢复尚未经过真实玩家 NBT 写盘/重启与多服交付验证，故 item reward 及 M.2 继续 `BLOCKED`。未读取玩家进度/数据库，未执行生产 DDL、部署或启用 Quest。

### 2026-09-30 - 修正 BQ 非消耗 Retrieval 的 PARTY 库存检测

- 对照 `TaskRetrieval.detect` 与其 `Detector`：BQ 的全部非消耗 Retrieval 都扫描活跃 party 成员库存；`groupDetect` 控制重置粒度，不控制是否使用 party 库存。true 每轮重置所有需求分量；false 仅重置未满足分量，单项已满足后保留；整项任务完成后锁存并广播给本次 party UUID。
- 修复观察计划、`BqHuntPartyAssignmentPolicy`、planner 与 `QuestFactProjector` 中仅允许 `groupDetect=true` 的遗漏。导入的非消耗 retrieval/optional_retrieval 现在都要求带可信当前 PARTY 身份的聚合库存快照并展开到个人进度；普通单人库存事实不会推进导入任务。消耗型 retrieval 路径不变。
- `TaskProgress` / `QuestEngine` 增加 false 模式下“保留已满足分量、重算未满足分量”的实现；true 仍重算所有分量。定向验证覆盖 planner 选择、单人快照拒绝、两种进度算法以及 RuntimeService 非 groupDetect fan-out 和锁定成员奖励延迟。
- CodeGraph 改前 impact：观察计划 44、planner accepts 14、QuestEngine 58、inventory evaluator 13 个符号；改后 impact 覆盖 party assignment policy 16、事实投影 14、观察计划 35、QuestEngine 38 个符号。`affected` 未发现 Core 单元测试候选；按调用链运行 `:galaxy-quest-core:test` 213 项及 `:galaxy-quest-postgres:test` 43 项，均 0 skipped/failure/error。新增 PG 用例通过 JDBC membership resolver 验证非 groupDetect party snapshot fan-out 与双人进度持久化；PostgreSQL 16 仅在独立网络 + tmpfs 容器运行，无宿主端口映射，测试后清理。第一次发现 3 项测试失败后补齐另外两个仍按 groupDetect 门控的层；最终两模块通过。空 `/tmp/gradle-home` 的 Gradle 分发下载超时，使用项目持久 Gradle 缓存重跑通过。`git diff --check` 通过；全量 root 构建未运行。
- 只读检索扩展名曾意外匹配到 `questbook.cfg` 一行，该值没有用于本次结论；未进一步读取其他配置、玩家进度或数据库，未改 DefaultQuests 或服务器文件。没有生产 DDL、部署、Quest 开关变化或 S1/S2 服务操作。
- PARTY 库存生产端到端和 Forge 实机时序尚未验证；`retrieval / optional_retrieval`、M.2 与 BQ 替代继续 `BLOCKED`。

### 2026-09-30 - M.2 Fluid 手动消耗子集与流体 NBT 精确匹配

- 按用户明确授权仅读取 S2 `config/betterquesting/DefaultQuests/Quests`，不读取玩家进度、服务端其他配置、数据库或运行状态。仅输出汇总计数：15 个 fluid Task 中 consuming 1 个；该任务 1 个 required fluid，`consume=true / autoConsume=false / groupDetect=false / ignoreNBT=false`，`Tag` 不存在；不保留任务名、UUID 或 NBT 内容。
- 对照本地 BQ `TaskFluid` / `TileSubmitStation`：consume 且非 autoConsume 时不随普通库存变化自动检测；Quest Detect 会扫描玩家库存，SubmitStation 只提交一个 `stackSize=1` 输入；流体允许按实际排出的量部分推进；FluidStack Tag 精确比较。`fullySyncQuests` 会改变 consuming 任务进度传播，但本轮没有读取实际服务端配置，也没有把它映射为 Base PARTY。
- Core 授权器现允许已发布、PLAYER scope、`consume=true && autoConsume=false` 的手动 fluid 提交；`autoConsume=true`、非消耗和共享主体继续拒绝。全库存入口按任务扫描；手持入口使用服务端捕获的槽位与完整 ItemStack 指纹，并最多处理所选栈一个容器。消费计划在变更前计算，记录每个需求项实际排出的流体量，沿持久 marker、APPLIED submission 与确认事实保存部分数量；同一稳定 key 重试不会二次扣除。
- Importer 与非消耗 fluid 快照新增 `nbtPresent`，运行时分别匹配缺失、显式空 compound 与非空 NBT；容器适配器按 BQ FluidStack 的精确 Tag 相等语义判断。容器不可分割时可能消耗超额容器但进度只计剩余目标；Base 无可用槽存放空容器会整笔拒绝且库存不变，BQ 则使用 SubmitStation 输出槽或在库存检测失败时掉落。这些安全差异已明确记录在行为矩阵，不宣称等价。
- CodeGraph：改后 `impact` `planFluidsDetailed` 4 符号、`ConsumptionRequestAuthorizer.supports` 5、`FluidSnapshotTaskEvaluator.evaluate` 16、`BqStandardCodec.appendFluids` 7；`affected` 未自动找到候选，按调用链选定 Core、Importer 与 Minecraft 消费测试。
- Docker Gradle 全量 `:galaxy-quest-core:test` 224 项、`:galaxy-quest-bq-importer:test` 42 项（5 条既有条件跳过）与根 `MinecraftPlayerConsumptionPortTest` 16 项通过，共 282 项、5 skipped、0 failure/error。`git diff --check` 通过；未运行 PostgreSQL、全量根 `test`、`assemble`、自包含门禁或 Forge 实机；未部署、未执行生产 DDL、未启用 Quest、未停用 BetterQuesting、未操作 S1/S2 服务状态。Fluid 及整体 M.2/BQ 替代继续 `BLOCKED`。

### 2026-09-30 - M.2 Location 重复周期领取门控

- 按用户授权只读读取 S2 `config/betterquesting/DefaultQuests` 定义，未读取玩家进度、其他配置或数据库。33 个 Location Task occurrence 中 11 个可重复，且均有奖励、`autoClaim=false`；重复间隔为相对 0 tick 的 4 项与相对 1,728,000 tick（24 小时）的 7 项。详细分布与 BQ 源码行为已记入 `docs/betterquesting-m2-behavior-compatibility-2026-09-23.md`。
- Base 的重复任务现在会在下一周期前检查当前周期是否仍有 `CLAIMABLE` 奖励权益；未领取时冷却届满也不重置，领取后使用数据库 `claimed_at` 作为冷却锚点。无奖励时回退到完成时间。新增 Core PARTY 独立周期/领取门控测试及 JDBC 奖励领取后的周期锚点集成测试。
- CodeGraph `impact` 检查 `LocationTaskEvaluator.evaluate`（17 个符号，命中 evaluator 用例）、`QuestRuntimeService.execute`（4）、`JdbcQuestRuntimeRepository.hasClaimableRewardEntitlement`（3）和 `BqBehaviorInventory.from`（57）；`affected` 命中行为画像、Core Runtime 与 PostgreSQL 集成测试。定向结果：Core 222 项、Importer 42 项（5 项条件跳过）、隔离 PostgreSQL 16 集成套件 44 项；总计 308 项、5 项跳过、0 失败/错误。最终 `git diff --check` 通过。
- 该修正只覆盖样本中可重复 Location 的手动领取门控，不代表 BQ 行为全等：BQ 在线 `activeQuests`/`canSubmit`、成员在线变化、Minecraft 调度与存档恢复尚未在隔离运行时或实机验证。未执行生产 DDL、部署或 Quest 开关操作，M.2 与 BQ 替代继续 `BLOCKED`。

### 2026-09-30 - M.2 检索任务单人回退

- 对照本地 GTNH 1.7.10 BQ `TaskRetrieval.detect` / `ParticipantInfo`：有 Party 时检查当前在线 Party 成员背包并按 `ALL_UUIDS` 写入队员个人进度；无 Party 时 `ALL_UUIDS` 和 `ACTIVE_PLAYERS` 都退化为当前玩家。
- 发现 Planner 之前对导入的非消耗 `retrieval / optional_retrieval` 一律要求可信 PARTY 快照，导致 solo 玩家普通背包事实被拒绝。现在只有在服务端解析确认玩家无 PARTY membership、事实无其他聚合 scope 标记时才允许 singleton 的个人观察；Party 成员普通个人快照仍拒绝，必须走可信 PARTY 聚合快照。导入的 BQ 任务也不能被 Base GROUP snapshot 绕过。
- 增加 Planner 回归测试，覆盖 solo fallback 和 Party 快照准入隔离。CodeGraph 改前检查 `QuestEvaluationPlanner.accepts` 调用链和影响面，改后 `QuestEvaluationPlanner` impact 37 个符号、`affected` 命中 `QuestEvaluationPlannerTest`。Docker Gradle `:galaxy-quest-core:test` 227 项通过，0 skipped/failure/error；`git diff --check` 通过。
- 采用此前用户明确授权、仅统计 `DefaultQuests` 的定义画像，没有重新读取或扩展服务端数据范围；不读玩家进度、其他配置或数据库。无生产 DDL、部署、服务状态或 Quest 开关变化。跨服 PARTY 库存 producer 的 Forge/GTNH 实机闭环仍未验证，故检索任务与 M.2 继续 `BLOCKED`。

### 2026-09-30 - M.2 PARTY 库存快照生产链单元测试

- 为 `BqPartyInventorySnapshotFactSink` 抽取 package-private `BqPartyInventorySnapshotStore` 端口；生产构造仍委托给原 JDBC repository，测试通过 fake store 验证 sink 的会话与事实发布逻辑，不改变数据库合同。
- 新增 3 个测试：solo 不创建 Party lease 且只发布个人事实；Party session 合并本地与新鲜成员库存，分别发个人与 PARTY 聚合事实；已被替换的旧 session 不能发布。CodeGraph 同步 3 个源/测试文件后，sink impact 纳入新测试，`affected` 选中该测试。
- Docker Gradle 定向测试 `:test --tests com.jsirgalaxybase.modules.quest.infrastructure.minecraft.BqPartyInventorySnapshotFactSinkTest` 通过：3 项、0 skipped/failure/error；`:galaxy-quest-core:test` 为 UP-TO-DATE（前一阶段已通过 227 项）；`git diff --check` 通过。首次把 `--tests` 错误地应用到根聚合 `test` 导致无匹配测试，随后改为根项目任务并通过。
- 此处只验证 producer 单元逻辑；没有验证真实 JDBC repository 与 sink 的事务闭环、Forge tick/登录调度、GTNH 实际 Party 在线名单或跨服传输。未执行 PostgreSQL 测试、生产 DDL、部署、服务操作或 Quest 开关变更。M.2 与 BQ 替代仍为 `BLOCKED`。

### 2026-09-30 - M.2 PARTY 库存快照生产链 JDBC 集成验证

- 根据此前实际缺口，新增 PostgreSQL 集成用例，使用生产 `BqPartyInventorySnapshotFactSink`、`JdbcQuestTransaction`、JDBC PARTY 成员解析器和真实租约仓储。真实 PARTY 的两份新鲜库存汇总正确；过期成员的 900 件测试库存不进入聚合；下游处理异常会在同一事务回滚登录租约。进一步把快照 sink 接到真实 `QuestRuntimeService`、`QuestEvaluationPlanner` 和 JDBC fact/progress repository：两名在线成员的有效物品库存完成非消耗 BQ Retrieval，并把独立 PLAYER 进度投影给当前 PARTY 的所有成员；重放同一源事实由 fact ledger 幂等拒绝。测试配置必须显式传 `QUEST_TEST_POSTGRES_URL`，不回退到 `galaxy-base`。
- 端到端用例先失败并定位到一个生产幂等缺陷：`bqPartyInventoryObserved` 原先每次被 sink 重建时都取新 event ID，使相同源观察事实重放会被当成新 PARTY fact。现将聚合 event ID 稳定派生自源 inventory event ID 与 PARTY UUID，并继承源观察时间；同一真实 JDBC 链回归通过。CodeGraph 改后同步 2 个 Java 文件；`impact MinecraftQuestFactFactory.bqPartyInventoryObserved` 29 个符号、`impact BqPartyInventorySnapshotFactSink` 35 个符号，`affected` 收敛至该 PostgreSQL 集成测试。Docker Gradle 根定向 22 项（PostgreSQL sink 3、fake sink 3、Minecraft consumption adapter 16），0 跳过/失败/错误。PostgreSQL 16 只运行在一次性独立 Docker 网络及 512 MiB tmpfs 容器中，无宿主端口；结束后删除容器和网络。`git diff --check` 通过。
- 根据用户授权仅读取 S2 `DefaultQuests` 定义目录，摘要哈希仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`；未读取玩家进度、其他配置或数据库。此闭环未验证 Forge 玩家库存枚举、真实登录与 20 tick 调度、跨服 transport、GTNH Item/NBT 或 BQ 在线成员与完整客户端行为；故 retrieval、optional retrieval 及整体 M.2 继续 `BLOCKED`。未执行生产 DDL、部署、Quest 开关或服务器操作。

### 2026-09-30 - PARTY Retrieval 完成后的 item 奖励权益入账

- 将隔离 PostgreSQL 集成场景从“完成 Retrieval 并写入个人进度”扩展为服务端奖励权益入账：奖励使用 BQ importer `bq_standard:item` 的 `item.count / registryName / meta / amount / oreDictionary / nbt` 参数形状。完成后直接查询 JDBC 写入表，验证 PARTY 三名成员各有且仅有一条个人 `CLAIMABLE` 钻石奖励权益，recipient 与 PLAYER progress 主体一致，物品注册名和数量保真；重放同一库存事实后仍保持一人一条。
- 测试复用生产 `BqPartyInventorySnapshotFactSink`、`QuestRuntimeService`、`QuestEvaluationPlanner`、数据库成员解析器与 `JdbcQuestRuntimeRepository`，运行在无宿主端口映射的一次性 PostgreSQL 16/tmpfs 容器。CodeGraph 已同步测试改动；`impact QuestEngine` 覆盖运行时和既有 Core/PG 奖励调用者，`impact JdbcQuestRuntimeRepository` 覆盖 entitlement JDBC 写入和 PG 回归，`affected` 命中本集成测试。根定向 Gradle 用例 3 项通过，0 跳过/失败/错误；`git diff --check` 通过。临时 Spotless shim、数据库容器和专用网络均已清理。
- 证据止于 PARTY 完成后持久化逐玩家奖励权益，不证明物品已进入 GTNH 玩家背包；Minecraft `ItemStack` 物化、玩家存档写盘/重启、跨服租约及在线领奖仍未由该用例覆盖。未读取玩家进度、生产数据库或其他服务配置；未执行生产 DDL、部署、Quest 开关或服务器操作。item reward、Retrieval、M.2 与 BQ 替代继续 `BLOCKED`。

### 2026-09-30 - PARTY item 奖励领取与 Minecraft 投递适配器集成

- 将上述隔离场景继续接入 `AuthenticatedQuestClaimService`、`JdbcRewardClaimRepository`、真实 `JdbcRewardDeliveryRepository`/`RewardDeliveryWorker` 与生产 `MinecraftItemRewardDeliveryHandler`。三个 PARTY 成员分别领取各自权益；测试玩家端口各收到 2 个 diamond `ItemStack`，与 entitlement 对应的持久 NBT marker 被写入并 flush，JDBC 状态成为 `DELIVERED`；领取重放返回 `ALREADY_CLAIMED`，worker 重跑无新租约、无重复入包，库存事实重放仍为 `DUPLICATE`。
- PostgreSQL 仍为独立网络、无宿主端口、512 MiB tmpfs 的一次性 PG16 容器。CodeGraph 改后 `impact MinecraftItemRewardDeliveryHandler.deliver` 为 66 个符号、`impact RewardDeliveryWorker.runOnce` 为 26 个符号，`affected` 收敛到该集成测试。测试类 3 项通过、0 跳过/失败/错误；首次编译暴露 1.7.10 `NBTTagCompound` 不支持 `getKeySet()`，改为按 entitlement key 检验 marker；首次运行还发现断言把已投递权益误期望为 `CLAIMABLE`，修正后通过。`git diff --check` 通过；临时 shim、容器与网络均清理。
- 按用户授权仅对 S2 `config/betterquesting/DefaultQuests` 做只读行为清点，输出过滤为任务/奖励及行为参数的聚合计数，不输出 quest 名称、UUID、NBT 或逐任务内容；retrieval 3,863、optional retrieval 1,267、item reward 4,181，未发现超出既有清单的新 Task/Reward 类型。没有读取玩家进度、数据库、同级其他配置，也未改动服务器数据。测试中的玩家和 Item 均为替身：不证明真实 `EntityPlayerMP` / WorldSave 进程重启、在线跨服租约或 Forge 世界溢出掉落实测；item reward、Retrieval、M.2 与 BQ 替代继续 `BLOCKED`，未部署、未执行生产 DDL、未启用 Quest 或操作 S1/S2 服务。

### 2026-09-30 - PARTY choice 奖励个人选择与投递

- 将 PARTY Retrieval PostgreSQL 场景扩展为同时带有 `bq_standard:item` 和三选一 `bq_standard:choice` 的 BQ 导入参数形状。三名成员在认证选择服务中分别选不同选项，领取后由真实 JDBC reward router/worker 送入各自测试背包；直接核对 entitlement 的 recipient、持久 choice index、Minecraft marker，两类权益合计六条均为 `DELIVERED`，并按物品类型和数量断言各成员只收到本人选项。重复选择返回 `ALREADY_SELECTED`，claim 重放、worker 重跑和事实重放都不重复发奖。
- CodeGraph 改后 `impact AuthenticatedRewardChoiceService.select` 为 17 个符号、`impact RewardDeliveryRouter.deliver` 为 12 个符号；`affected` 选中 PARTY PostgreSQL 集成与 Minecraft reward 测试。定向 Docker Gradle 共 28 项（集成类 3、`MinecraftRewardDeliveryTest` 25），0 skipped/failure/error；PG16 使用独立网络、tmpfs、无宿主端口，临时 shim/容器/网络均已清理，`git diff --check` 通过。
- 这验证个人 choice 不会因 PARTY 进度共享而串选项，且沿用 item handler 投递链；仍是测试 Item/玩家端口，不证明真实玩家 NBT 重载、Forge/WorldSave、掉落崩溃窗口或多服玩家身份时序。只读 S2 定义频数清点的 choice reward 为 957 项；未读取玩家进度、数据库或其他配置。Choice/item/retrieval 与总体 M.2 继续 `BLOCKED`，无生产 DDL、部署或 Quest 开关变化。

### 2026-09-30 - PARTY Retrieval 闭环补充 optional_retrieval

- 按用户授权范围复用只读 S2 `config/betterquesting/DefaultQuests` 聚合画像：其中有 1,267 个 `optional_retrieval` 定义。对照本地 BQ `TaskOptionalRetrieval.ignored()` 与 `QuestInstance.update/detect`：该任务仍运行检测、可记录进度，但从完成逻辑的分子/分母中排除。Importer 已将该类型映射为 `optional=true`。
- 扩展既有隔离 PostgreSQL PARTY 闭环场景：Quest 同时要求 7 个 iron_ingot，并含未满足的 5 个 gold_ingot optional retrieval；两名活跃成员快照触发 PARTY 聚合，服务端为三个成员分别持久化个人进度。断言必需任务完成、可选任务保持 0/5 未完成、Quest 仍完成并产生个人 item/choice 权益，确认选择/领取/ItemStack 测试适配器投递链不受影响。
- CodeGraph 在修改前检查 `QuestEngine.evaluate`（51 个受影响符号，覆盖 RuntimeService、既有 Core/PG 测试），修改后 `affected` 命中本 PostgreSQL 集成类。Docker Gradle `:test --tests com.jsirgalaxybase.modules.quest.infrastructure.minecraft.BqPartyInventorySnapshotFactSinkPostgresTest` 通过：3 项、0 跳过/失败/错误。PG16 使用临时 bridge 网络、512 MiB tmpfs、无宿主端口；临时容器和网络随测试清理。`git diff --check` 通过。
- 这补齐的是 optional retrieval 的 PARTY/JDBC/奖励权益自动证据，不代表真实 Forge 玩家库存、BQ 客户端显示、存档恢复或跨服在线时序已验证；retrieval、optional retrieval、item/choice reward、M.2 与 BQ 替代继续 `BLOCKED`。未读取玩家进度/生产数据库或其他服务器配置；无生产 DDL、部署、Quest 开关或服务器状态变化。

### 2026-09-30 - 冻结 BQ 已完成 Quest 的旧周期进度

- 对照本地 BQ `QuestInstance.detect`、`canSubmit` 与 `QuestCache.updateCache`：已完成且本周期不可提交的 Quest 会离开 `activeQuests`，自动库存检测不会再推进旧周期；重复任务在领取及重置条件满足后才进入新周期。
- 发现 Base `QuestEvaluationPlanner` 面向所有已发布定义产出请求，而 `QuestEngine` 原先会把已完成之后的新观察继续合并到旧进度。对于 `optional_retrieval`，这会导致 BQ 已停止检测后 Base 仍可能补完成可选任务。现调整为已完成周期冻结全部观察；只有完成新周期条件后才接受新数据，不改变完成时间或重复生成权益。
- 增加 Core 回归：必需任务完成、optional retrieval 为 0/5 后，新事实即使报告 5/5，旧周期仍保持 0/5 且不再生成奖励。扩展 PARTY/JDBC PostgreSQL 场景：完成 Quest 后，同一在线 PARTY 的后续快照加入此前缺失的 gold_ingot，三个成员仍保持已完成状态及可选 0/5，证明冻结贯穿生产 sink、RuntimeService 与 JDBC 进度存储。
- CodeGraph `impact QuestEngine.evaluate` 30 个符号、`impact QuestRuntimeService.execute` 4 个；`affected` 命中 Core `QuestEngineTest` 与根 PARTY PostgreSQL 集成类。Docker Gradle `:galaxy-quest-core:test` 228 项、根 PostgreSQL 集成类 3 项，合计 231 项，0 跳过/失败/错误；PG16 使用无端口映射的一次性 bridge 网络与 512 MiB tmpfs。`git diff --check` 通过。
- 该实现只修复完成后的周期进度冻结，不代表 BQ 完整 activeQuest、领取、延迟重置调度或在线队伍时序等价；retrieval/optional retrieval 与 M.2 继续 `BLOCKED`。未执行生产 DDL、部署、Quest 开关或服务操作，未访问玩家进度/生产数据库。

### 2026-09-30 - BQ item reward 导入到 Minecraft 投递适配器闭环

- 按用户确认只读读取 S2 `config/betterquesting/DefaultQuests`，重新挂载源目录为只读并运行 `behaviorInventory`；来源摘要仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，3,744 个任务、47 个章节、3,847 个放置，retrieval 3,863、optional retrieval 1,267、item reward 4,181。retrieval 的 5,539 个物品需求中 2,761 个开启 NBT 检查，84 个带 Ore Dictionary；没有输出任务名/UUID，也没有读取玩家进度、其他配置、数据库或 NBT 内容。
- 新增纯测试依赖 `testImplementation(project(":galaxy-quest-bq-importer"))`，不进入 Mod 运行依赖或 Shadow JAR。根项目自动回归从临时 BetterQuesting typed JSON 文件读取一个真实结构的 item reward，经 `BqQuestDefinitionImporter` 产出 `RewardDefinition`，再由生产 `MinecraftItemRewardDeliveryHandler` 处理；验证数量 130 按 64/64/2 分栈、metadata、`VAR_NAME`/`VAR_UUID` NBT 替换、持久完成 marker 与重复租约不重复加物品。所有数据和玩家均为测试替身。
- CodeGraph 修改前检查 importer `read` 的 callers/callees/impact，以及奖励 handler materializer 的 callers/impact；改后同步 2 个变更文件，`affected` 命中 importer 测试和新根测试。Docker Gradle `:test --tests ...BqItemRewardImportDeliveryTest` 1 项通过，`:galaxy-quest-bq-importer:test --tests ...BqQuestDefinitionImporterTest` 4 项通过，0 跳过/失败/错误；行为清点使用 `--no-configuration-cache` 后成功，`git diff --check` 通过。
- 此闭环证明的是导入参数到测试 Minecraft 适配器的直接兼容，不证明真实 EntityPlayer/WorldSave 写盘恢复、世界溢出掉落、崩溃窗口、在线跨服租约或 GTNH 实际物品行为。item reward、任务类型和整体 M.2/BQ 替代继续 `BLOCKED`；未执行生产 DDL、部署、Quest 开关或服务器操作。

### 2026-09-30 - Retrieval strict NBT 的 Compound 语义对齐

- 对照 BetterQuesting `ItemComparison.CompareNBTTagCompound/CompareNBTTag`：Compound 无论 `partialMatch` 值如何都以需求键为子集匹配；strict flag 仅让 List 和 byte/int array 长度必须相等。Base 的 `PortableNbtMatcher` 原先在 `partial=false` 时错误要求 Compound 键数完全相同，现已删除此条件；增加 Core evaluator 与 NBT matcher 回归，覆盖额外 Compound 元数据可通过、需求字段缺失拒绝，以及 strict List 长度校验。
- 扩充只读 `BqBehaviorInventory` 输出按类型的联合布尔开关画像和 strict+NBT 内容交叉数。只读 S2 `DefaultQuests` 联合分布：Retrieval 2 项 `partialMatch=false` 均 `ignoreNBT=true`；optional Retrieval 4 项中 3 项 `ignoreNBT=true`、1 项开启 NBT 检查。后者的 3 个需求栈均为空 Tag；真实清单没有非空 Compound 触发此次差异，因此该修正以 BQ 源码语义和自动合成回归为依据，不宣称改变当前 S2 任务结果。只读摘要仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`；未读玩家进度、其他配置、数据库或物品 NBT 内容。
- CodeGraph 改前查询 evaluator 与 matcher callers/callees/impact；改后 `impact PortableNbtMatcher.matches` 11 个符号、`impact BqBehaviorInventory.TypeUsage.add` 31 个符号，`affected` 命中 3 个 Core/Importer 单测文件。Docker Gradle 完整 `:galaxy-quest-core:test` 229 项、`:galaxy-quest-bq-importer:test` 44 项（5 项隔离数据库条件跳过），0 失败/错误；仅为绕过 GTNH Style 模块远程 spotless 配置超时，命令行使用 `-PdisableSpotless=true`，未改项目配置；`git diff --check` 通过。
- 此修正恢复的是 BQ NBT 配置比较语义，不证明 GTNH 真实 ItemStack/NBT、OreDictionary、Forge 消耗时序、多服成员快照或存档恢复。未执行生产 DDL、部署、Quest 开关或服务器操作；Retrieval / optional Retrieval 与整体 M.2/BQ 替代仍 `BLOCKED`。

### 2026-09-30 - 对齐 Retrieval 的 BQ NBTTagList 与严格 Compound 比较

- 按高频 Retrieval 继续核对本地 BQ `ItemComparison.CompareNBTTag`。BQ 对 `NBTTagList` 的每项比较调用 `getCompoundTagAt()`；因此列表中的非 Compound 项按空 Compound 参与比较，而不是比较其原始 String/数值。Base Core matcher 与 Minecraft 消耗适配器此前直接比较列表值；现在两条路径统一按 BQ accessor 语义处理，且保留 BQ 列表重复匹配、不消耗候选索引的特征。检查时还发现消耗适配器残留 strict Compound 键数相等条件，已移除；`partialMatch=false` 对 Compound 仍只校验需求键，对 List/byte/int array 仍校验长度。
- 按用户授权将 S2 `DefaultQuests` 以只读卷挂载，清点器只输出过滤后的 Retrieval/optional Retrieval 匿名聚合行。画像新增不含标签键/值的 NBT 节点/列表元素形状和 `ignoreNBT=false` 交叉计数：Retrieval 有 560 个 Compound 与 6 个 String 列表元素；开启 NBT 比较时为 527 个 Compound、0 个非 Compound；optional Retrieval 有 601 个 Compound 列表元素，其中 600 个开启 NBT 比较，没有非 Compound 列表元素。此次列表语义修正未触发当前 NBT-enabled 数据；作为 BQ 源行为兼容由合成测试固定。摘要哈希保持 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，没有读取玩家进度、其他配置、数据库或原始物品 NBT。
- CodeGraph 改前检查 `PortableNbtMatcher.matches`、`MinecraftPlayerConsumptionPort.compareNbt` 与 `BqBehaviorInventory.TypeUsage.add` 的调用/影响；改后首次同步 6 个源/测试文件（263 nodes），新增 strict List 适配器回归后再同步 1 个测试文件（58 nodes）。最终 impact 分别为 14/12/3 个符号，`affected` 命中 3 个测试文件。Docker Gradle：完整 `:galaxy-quest-core:test` 229 项、`:galaxy-quest-bq-importer:test` 44 项（5 项隔离数据库条件跳过），根定向 `MinecraftPlayerConsumptionPortTest` 18 项，0 failure/error。完整根 `test` 首次因开发容器缺 CJK 字体导致 UI Demo 2 项失败；随后将宿主 Noto TTC 只读挂载为 `UI2_PREVIEW_FONT` 重跑，全量 `test` 成功，字体未进入仓库或 Mod。使用命令行 `-PdisableSpotless=true` 绕过远程 Spotless 配置超时，没有改变仓库构建配置；`git diff --check` 通过。一次初始根聚合测试命令把 `--tests` 错误传递给子模块，随后排除子模块测试任务并通过目标套件；前次输出不是代码断言失败。
- 此处只证明 BQ 源码 NBT 比较语义和脱敏配置画像，不证明真实 GTNH ItemStack/Forge NBT/OreDictionary、在线多服或存档恢复。Retrieval / optional Retrieval 与整体 M.2/BQ 替代继续 `BLOCKED`；无生产 DDL、部署、Quest 开关或服务操作。

### 2026-09-30 - Item 奖励进度压缩 NBT 恢复回归

- CodeGraph 改前查询 `MinecraftItemRewardDeliveryHandler.deliver` impact（66 个符号）及 `MinecraftRewardDeliveryTest` impact（75 个符号）；同步后 `affected` 选中 `MinecraftRewardDeliveryTest`。
- 新增 70 个 Item 奖励、单槽库存部分插入 64 个后世界掉落实验失败的恢复用例。测试把 Forge `ForgeData/PlayerPersisted` 中的奖励进度经 Minecraft 1.7.10 `CompressedStreamTools` 实际压缩/解压，再重建 PlayerState 重试；确认已记账的 64 不重复、剩余 6 只补发一次，成功后 entitlement marker 存在、临时进度清空。因纯 JVM 测试没有 LaunchWrapper/Forge registry，背包 ItemStack 在 fake 重启时复制而非序列化；该测试不等于真实 InventoryPlayer、WorldSave 或 EntityPlayer 重启闭环。
- Docker Gradle `:test --tests com.jsirgalaxybase.modules.quest.infrastructure.minecraft.MinecraftRewardDeliveryTest`：26 项通过，0 skipped/failure/error；`git diff --check` 通过。首轮使用未初始化的 Vanilla Items registry 失败，改用既有测试 Item 后复测通过。未扫描新的服务器文件；沿用此前获授权只读 S2 `DefaultQuests` 聚合画像。未访问玩家进度/数据库，未执行生产 DDL、部署、Quest 开关或 S1/S2 服务操作。Item reward、Retrieval、M.2 与 BQ 替代继续 `BLOCKED`。

### 2026-09-30 - Item 奖励恢复测试扩展到 InventoryPlayer NBT

- 在纯 Minecraft 测试对象上补足上一条恢复测试的背包存档环节：测试物品通过仅测试用途的数字 ID 映射供 `ItemStack` 解码，不调用 Forge Loader/真实 GTNH item registration；背包本身改由 Minecraft 1.7.10 `InventoryPlayer.writeToNBT/readFromNBT` 实际编解码，保存和恢复 36 格槽位；奖励进度仍经过 `ForgeData/PlayerPersisted` 与 `CompressedStreamTools` 压缩流往返。
- CodeGraph `status/sync` 显示索引无待处理；`impact MinecraftItemRewardDeliveryHandler.deliver` 66 个符号，`affected` 命中 `MinecraftRewardDeliveryTest`。Docker Gradle 根定向测试 26 项通过，0 skipped/failure/error；`git diff --check` 通过。
- 本测试证明 Minecraft NBT/InventoryPlayer API 的对象级存档往返，不证明真实 `EntityPlayerMP`、GTNH 注册表生命周期、`WorldSaveQuestPlayerDataFlusher` 或跨服交付。掉落实体生成后、最终奖励 marker 写盘前的崩溃窗口仍待解决/验证；item reward、retrieval、M.2 与 BQ 替代保持 `BLOCKED`。未扫描服务器文件、未访问玩家进度/数据库、未执行生产 DDL、部署、Quest 开关或 S1/S2 服务操作。

### BetterQuesting Item 奖励堆叠算法对齐（2026-09-30）

- 用户授权直接读取服务端 quest 定义后，本轮限定只读 S2 `DefaultQuests`；未读取玩家进度、其他配置或数据库。聚合发现 `bq_standard:item` 有 15 个配置数量大于 64，最大为 8192；不记录任务/物品标识。目录摘要 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc` 未变。
- 对照 GTNH BQ `3.7.14-GTNH`：`BigItemStack.getCombinedStacks()` 以物品 `getMaxStackSize()` 拆分；`RewardItem` 背包插入失败时掉落剩余栈；`QuestInstance.claimReward` 在跑完奖励后才写 claimed。Base 已将物品栈最大数与玩家背包每格 64 上限分开，新增 stack-size 128、数量 130 的 overflow 回归；CodeGraph 同步 2 文件，`impact deliver` 66 个符号，`affected` 选择 `MinecraftRewardDeliveryTest`。Docker 定向 suite 27/27 通过，零跳过/失败/错误，`git diff --check` 通过。
- 此测试用合成大堆叠物品固定上游算法，不证明 DefaultQuests 中目标物品的实际注册表最大堆叠数。BQ 源码确认发奖在 claimed 标记之前，但 Base 世界掉落实体生成与玩家 NBT flush 的进程崩溃窗口仍无真实服务端证据；item reward、Retrieval 与 M.2 保持 `BLOCKED`。无生产 DDL、部署、Quest 开关或服务器操作。

### 2026-09-30 - PARTY groupDetect Retrieval 重算回归

- 对照本地 BQ `TaskRetrieval.Detector`：非消耗 `groupDetect=true` 会在每次扫描前清空所有成员的需求向量，按当前在线 Party 库存重新计算；普通 Retrieval 只清除未完成部分。S2 `DefaultQuests` 脱敏聚合中 4 个 groupDetect Retrieval 均为非消耗。
- 新增 PostgreSQL 集成回归，经过生产 `BqPartyInventorySnapshotFactSink`、JDBC Party membership、Planner、Runtime 与 progress repository。Party 先只有 5 个 iron，随后同一在线成员的快照替换成 5 个 gold；三名 Party 成员的个人进度均由 `[5,0]` 重算为 `[0,5]`，保持未完成；另一名在线成员带回 5 个 iron 后，当前聚合令三名成员均完成 `[5,5]`。
- CodeGraph 改前检查 `QuestEngine.evaluate` 与 `InventorySnapshotTaskEvaluator.evaluate`；改后 sync 1 个测试文件，`impact BqPartyInventorySnapshotFactSink.process` 与 `QuestRuntimeService.execute` 各 4 个符号，`affected` 未给出测试候选，故直接执行目标 PostgreSQL 集成类。一次性 PostgreSQL 16 使用独立 Docker 网络、512 MiB tmpfs、无宿主端口；4 项执行、0 跳过/失败/错误。首轮发现未传 `POSTGRES_DB` 会使 JUnit Assume 跳过，补齐环境后以 XML 确认全数执行。临时容器/网络已删除，`git diff --check` 通过。
- 该闭环不证明 Forge/GTNH 在线清单、真实 Item/NBT、BQ tick 与跨服时序；Retrieval/M.2 继续 `BLOCKED`。未读取玩家进度、生产数据库或其他服务器配置，未执行生产 DDL、部署、Quest 开关或服务器操作。

### 2026-09-30 - 消耗型 Retrieval 导入至玩家库存规划

- 用户确认可以直接只读读取服务端 quest 定义。本次只对 S2 `config/betterquesting/DefaultQuests` 进行只读挂载，运行 `behaviorInventory` 并过滤输出匿名类型行；确认 Retrieval 3,863 项中有 307 项 `consume=true`，均为 `autoConsume=false`；optional Retrieval 1,267 项全部非消耗。来源摘要仍是 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。未读取玩家进度、其他配置、数据库或任何任务 NBT/身份字段。
- 新增 `BqConsumingRetrievalImportTest`：合成 BQ typed JSON 经 `BqQuestDefinitionImporter`、`ConsumptionRequestAuthorizer`、`MinecraftPlayerConsumptionPort.planItems`，验证需求数量、注册名、Damage/NBT、`ignoreNBT` 和 `partialMatch` 传递后可以按 BQ NBT 子集语义规划扣除，源 ItemStack 不被提前改动。Forge 注册 API 在纯 JVM 下要求 LaunchWrapper，最终测试通过反射只修改隔离测试 registry 映射；不代表真实 Forge/GTNH 物品注册。
- CodeGraph 改前检查 importer `read` / `BqStandardCodec.task`、消费授权 `authorize` 与适配器 `planItems`；改后同步 1 个新测试文件，`affected` 选中该测试。Docker Gradle 根定向测试 1 项通过，0 跳过/失败/错误；`git diff --check` 通过。首次尝试分别遇到测试编译注册键类型不一致、Forge 注册 API 缺少 LaunchWrapper，修正后最终绿测。
- 该自动闭环只证明合成导入—授权—库存规划合同；不证明 S2 实际物品、真实 BQ SubmitStation/UI、服务端实体库存、提交持久化与崩溃恢复、跨服语义等价。因此 Retrieval、optional Retrieval、M.2 和 BQ 替代继续 `BLOCKED`。没有生产 DDL、部署、Quest 开关、服务器服务操作或 S1 接触。

### 2026-09-30 - `lockedProgress` PARTY 制作事件门控

- 对照 BQ `QuestCache.updateCache`：前置未解锁的任务在 `lockedProgress=true` 时仍进入玩家的 `activeQuests`；因此制作类事件可以启动任务检测。Base 的 PARTY 规划此前无条件要求至少一名成员满足前置，导致所有成员都未解锁时即使任务显式允许锁定期间进度也被丢弃。
- 修改 `QuestEvaluationPlanner`：对导入 BQ PARTY 任务事件，仅当 `progressWhileLocked/lockedProgress` 为 false 时才执行“至少一名成员已解锁”门控。新增 Core 回归验证全员未解锁时，普通任务仍无请求，而 `lockedProgress=true` 会为 PARTY 成员生成独立 PLAYER 请求并保留 PARTY 事实来源。
- CodeGraph 改前 `impact QuestEvaluationPlanner.requestsFor` 39 个符号；改后同步 2 个 Java 文件（90 nodes），`affected` 命中 `QuestEvaluationPlannerTest`，改后 impact 为 37 个符号。Docker Gradle 定向规划器测试 19/19、完整 `:galaxy-quest-core:test` 230/230 均通过，0 跳过/失败/错误；`git diff --check` 通过。未读取本轮服务端定义；使用仓库内 BQ 源码对照。尚未执行 PARTY/Forge/GTNH 在线运行时验证，crafting 与 M.2 继续 `BLOCKED`；未访问进度/数据库、未执行 DDL、部署、Quest 开关或服务操作。

### 2026-09-30 - 合成统计检测遵守领取锚点且避免过早消耗 Fact Key

- CodeGraph 改前查看 `AuthenticatedCraftingStatisticsDetectionService.detect` 的 callers/callees 与 16 个受影响符号。该服务原先在事务外只按进度快照的 `completedAt` 推算下一周期，再直接 `runtime.apply`；它没有先使用通用 Runtime 的未领取奖励门槛、待恢复消耗门槛和 `findRepeatCycleAnchorMillis`。因此过早点击可能先登记下一周期的稳定 fact key，而进度仍被冻结；同一累计统计在真正可重复时会被去重掉。
- 现改为 `runtime.applyIf`：候选 fact 在写入幂等键前，于 Runtime 事务内重读当前进度，核对任务/前置、周期号、待恢复消费、可领取奖励和数据库领取时间锚点；被拒绝的检测不留下 fact key。测试验证未领取奖励时拒绝、领取后 10ms 冷却未到时拒绝、待恢复消费时拒绝，然后同一绝对生命周期统计在锚点 120ms 后的 130ms 进入新周期成功，且拒绝路径没有消耗 fact key。
- CodeGraph 改后 impact 为 17 个符号，`sync` 更新源/测试，`affected` 命中 `AuthenticatedCraftingStatisticsDetectionServiceTest`。Docker 定向测试 6/6、完整 `:galaxy-quest-core:test` 231/231 通过，0 跳过/失败/错误；`git diff --check` 通过。Core 测试使用内存仓库；已有 PostgreSQL 集成测试分别覆盖 claimable 门槛与领取时间锚点，但本次尚未将整个统计检测路径跑过 JDBC 集成。合成统计、crafting 与 M.2 仍 `BLOCKED`；未读取服务端文件、未访问生产进度/数据库、未执行 DDL、部署、Quest 开关或服务器操作。

### 2026-09-30 - PARTY Crafting active-quest 并集差异复核

- 依据仓库内 GTNH BetterQuesting 参考源码确认：BQ `ParticipantInfo` 只把当前 Minecraft server 的 Party 在线成员放入 `ACTIVE_PLAYERS`；`getSharedQuests()` 对这些在线成员的 `QuestCache.activeQuests` 求并集。`QuestCache` 同时考虑前置解锁、`LOCKED_PROGRESS`、任务可提交性及完成/领取/重复状态；事件任务通过并集筛选后，`TaskCrafting` 再向 `ALL_UUIDS` 全体 Party 玩家写进度。本轮没有把本地参考源码版本号当作现场 JAR 版本。
- CodeGraph 已检查 `ParticipantMembershipResolver`、`JdbcParticipantMembershipResolver`、`QuestEvaluationPlanner.hasUnlockedPartyMember`、`BqPartyTaskProgressScope` 与 crafting 事件入口。Base 的 JDBC membership 是全体当前 Party roster，没有在线成员维度；Planner 的现门控也只检查 roster 成员的前置完成状态。因此离线的已解锁成员可能使在线未解锁玩家的 crafting event 获准，这是具体行为差异，而非已完成兼容。
- 没有改代码：精确实现需要服务端权威的本服在线 Party 子集及活跃 Quest 资格输入，且要处理查询与事件入账的事务时序；现有 inventory lease 只在启用 Party Retrieval 观察的会话中存在，不能冒充通用 presence。兼容矩阵补充了来源细节与所需前提，Crafting/M.2 仍 `BLOCKED`。
- 本轮为只读源码/代码图评估和文档记录；没有访问玩家进度或生产数据库，没有 DDL、部署、Quest 开关或服务器操作。

### 2026-09-30 - PARTY 在线任务资格与前置完成闩锁

- 在前一轮发现 Base 用全体 Party roster 近似 BQ 本服在线 `activeQuests` 并集后，新增 `QuestPartyActivityQuery` / `BqPartyActiveQuestQuery`；生产 `QuestModule` 的四个规划器接入服务端在线玩家目录。检查事件来源服、来源玩家属于可信 Party、本服在线队员、前置/`lockedProgress`、已完成状态、待恢复消费、待领取权益及 repeat 锚点，再允许已有个人进度扇出。默认规划器仍 fail-closed。
- 修正 `JdbcQuestCompletionQuery` 使用 `ever_completed=TRUE` 查询前置完成，使重复 Quest 当前周期重置后仍保留终身解锁；新增 PostgreSQL 集成回归。
- CodeGraph `sync/status` 完成；改后 `impact QuestEvaluationPlanner.requestsFor` 59 个符号、`BqPartyActiveQuestQuery.hasActiveMember` 12 个、`JdbcQuestCompletionQuery.findCompletedQuestIds` 19 个、`QuestModule.createPartyActivityQuery` 7 个。`affected` 对相关 Core / adapter 路径没有自动选出测试，故直接执行关联测试。
- Docker Gradle：`:galaxy-quest-core:test` 237 项通过；隔离 PostgreSQL `QuestPostgresIntegrationTest` 36 项、根 `BqPartyInventorySnapshotFactSinkPostgresTest` 4 项通过，均 0 跳过/失败/错误；根 `compileJava` 与 `compileTestJava` 通过。PG 测试使用临时隔离网络与容器，没有宿主端口映射；未连接生产数据库。
- 此记录补充并取代此前“本轮未改 planner”的评估状态。自动查询尚未验证并发事件时序、GTNH 实机在线列表和性能；迁移旧进度无法重建历史 `ever_completed`。全量根 `test` / `assemble` 未运行；未部署、未执行生产 DDL、Quest 开关或服务操作。PARTY/Crafting 与 M.2 继续 `BLOCKED`。

### 2026-09-30 - `lockedProgress` 前置解锁后补算已存进度（首次事件版）

- 只读检查经用户授权的 S2 `DefaultQuests` 定义目录，分析器仅输出匿名 `QUEST_PROFILE` 聚合：50 个启用 `lockedProgress` 的 Quest（均 non-repeat），这些 Quest 累计含 46 个 Crafting、54 个 Retrieval/optional Retrieval、2 个 Checkbox task occurrence。未读取进度、其它配置、数据库或生产运行状态。
- 对照本地 BQ `QuestCache.updateCache` / `EventHandler`：锁定进度满足时，BQ 仍会周期性调用 `QuestInstance.update`，前置一旦满足就完成；Base 过去只在新事实到来时评价，存在完成前置后子任务停留 LOCKED 的差异。现在 `QuestRuntimeService` 在同一事务中从新完成的 PLAYER Quest 递归补算直接依赖的、已发布且 `progressWhileLocked` 的 PLAYER Quest；重用持久任务向量、保存状态和权益，并继续应用 noRewards 规则。Planner 要求当前全部前置已满足，未将逻辑外推到 PARTY。
- 新增 Planner 与 Runtime 回归，覆盖筛选、多前置门槛、存储进度解锁完成及奖励抑制。CodeGraph `impact QuestRuntimeService.reconcileNewlyUnlockedLockedProgress` 4 个符号，`impact QuestEvaluationPlanner.requestsForNewlyUnlockedLockedProgress` 47 个符号；`affected` 选中 `QuestEvaluationPlannerTest`、`QuestRuntimeServiceTest`。Docker Gradle `:galaxy-quest-core:test` 全量 239 项通过，0 跳过/失败/错误；`git diff --check` 通过。
- 这不是 BQ tick/login 的完整等价：补偿依赖当前新父 Quest 完成事件；离线期间已完成前置、旧锁定存档与无新事件情况仍未补算。无 PostgreSQL 端到端、Forge/GTNH 行为验证；不改变 PARTY、repeat 或跨服签收。根项目 `compileJava compileTestJava --offline` 未进入编译，因 GTNH convention plugin 初始化触发 `SocketTimeoutException`（`build.gradle.kts:10`）；本轮没有全量根 test/assemble。相关行为和 M.2/BQ 替代继续 `BLOCKED`；未执行生产 DDL、部署、Quest 开关或服务操作。

### 2026-09-30 - `lockedProgress` 在线周期扫描补算

- 本节取代上一节中“依赖新前置完成事件”的当前状态描述；那一节保留为首次实现记录。
- 根据本地 BQ `EventHandler` 约每 60 个玩家 tick 主动更新已激活 Quest 的行为，增加在线 PLAYER 周期复评。`QuestObservationPlan` 只在已发布定义包含 PLAYER `progressWhileLocked` 时开启；Runtime 事务读取当前玩家既有 `LOCKED` 进度与任务向量，Planner 只构造已发布、仍有持久化锁定进度、允许锁定期间推进且所有前置当前满足的 PLAYER 请求。PostgreSQL 查询按当前玩家及 `status='LOCKED'` 限定并锁定对应 progress 行；复评保存既有 task vector 的新状态和权益，继续应用 `noRewards` 规则，并递归处理新完成 Quest 的直接锁定 PLAYER 依赖。
- CodeGraph `impact QuestRuntimeService.reconcileLockedProgress` 71 个符号、`impact JdbcQuestRuntimeRepository.findLockedProgress` 76 个、`impact QuestGameplayEventHandler.onPlayerTick` 34 个、`impact QuestObservationPlan.fromPublished` 50 个；改后 `affected` 选出 `QuestEvaluationPlannerTest`、`QuestObservationPlanTest`、`QuestRuntimeServiceTest`、`QuestPostgresIntegrationTest` 四个测试文件。Docker Gradle `:galaxy-quest-core:test` 242 项、隔离 PostgreSQL 查询集成 1 项、根 `compileJava compileTestJava --offline` 均通过，0 跳过/失败/错误；PG16 使用无宿主端口映射的独立网络和 tmpfs，一次性容器/网络已清理。首次根编译被历史 root-owned 忽略输出阻断；将精确的旧 class/resource 文件和根 `build/resources/main` 生成输出目录改名保留在同一 `build/` 下后重跑成功，没有删除这些生成物。
- 修复范围仅限在线 PLAYER 的持久化 `LOCKED` 进度，不扩展到 PARTY/TEAM/PUBLIC，也不宣称 BQ 周期、领取、重复周期、定义版本迁移和奖励时序完全一致。未验证真实 Forge/GTNH tick 路由及在线规模下查询性能；行为与整体 M.2/BQ 替代继续 `BLOCKED`。仅读取经用户授权的 S2 `DefaultQuests` 定义目录，未读取玩家进度、其它配置或生产数据库；未执行生产 DDL、部署、Quest 开关或服务器操作。最终 `git diff --check` 通过。

### 2026-10-02 - `block_break` BQ 元数据语义回归

- 按用户明确授权，只读挂载 S2 `config/betterquesting/DefaultQuests`；`behaviorInventory` 的匿名聚合与源摘要一致：当前有 1 个 `bq_standard:block_break` occurrence。没有在文档保存任务名、UUID 或原始定义。
- 对照仓库内 BQ `TaskBlockBreak`，确认直接方块匹配只接受负 metadata 通配符；32767 用于 OreDictionary 查询而不是注册名直接匹配。修正 `BlockVectorTaskEvaluator`，并使 `MinecraftQuestFactFactory.blockBroken` 只在方块声明 TileEntity 时读取其 NBT，吻合 BQ 事件代码。
- 增加 Core evaluator 对 metadata、OreDictionary、partial NBT、每事件只推进首个需求的测试，以及 importer 的 block vector/目标/portable NBT 映射测试。CodeGraph 改前 impact：evaluator 14、事实工厂 53、事件入口 34 个符号；改后 sync 4 个文件/138 nodes，索引 1,109 files/30,110 nodes/81,853 edges，`affected` 选中两个对应测试类。
- Docker 隔离开发容器：`:galaxy-quest-core:test` 244/244；`:galaxy-quest-bq-importer:test` 44 项、5 项因无隔离 PostgreSQL 环境而 assumption-skip，0 failure/error。测试 setup 曾继承 Compose 的 PostgreSQL 环境变量并在认证阶段拒绝连接，失败发生在测试 setup 获取连接阶段、没有创建 schema/执行 DDL；随后显式 unset `POSTGRES_USER/PASSWORD/DB/HOST` 并 `--rerun-tasks`，安全重跑通过，5 项数据库集成明确跳过。根 `compileJava compileTestJava --offline --no-configuration-cache` 通过；配置缓存模式曾因现有 `prepareUi2QzLicense` task 闭包不可序列化报错，禁用配置缓存后构建成功。以后需用隔离数据库显式跑这些 PostgreSQL 集成测试，不能从 `galaxy-dev` 的 `.env` 继承连接配置。
- 行为仍 `BLOCKED`：纯 evaluator/importer 测试不覆盖 Forge/GTNH BreakEvent、实际 TileEntity/OreDictionary、持久化 Party fan-out 与 BQ `ALL_UUIDS` 完成顺序。未读玩家进度、其他服务端配置或生产数据库；未执行生产 DDL、部署、Quest 开关或服务操作。

### 2026-10-10 - Crafting 跨服绝对统计归并模型审计

- CodeGraph `status` 为 up-to-date；`explore AuthenticatedCraftingStatisticsDetectionService / ItemVectorTaskEvaluator / TaskProgress`、`node recordFactIfAbsent` 与 `impact detectAutomatically` 核对统计采集、事实幂等和进度归约。事实账本以 `(source_server,event_id)` 为键，不同服务器的个人快照可分别入账；但 Crafting 统计投影使用同一 Task 向量，`TaskProgress.merge` 对各分量取最大值，不保留来源服务器贡献。因此同一玩家 S1=5、S2=7 当前显示 7，而不是按来源累加。现有跨服集成回归只锁定这一实现行为，并非产品合同。
- 进一步确认若采用“每服最高绝对计数相加”，不能简单把通用向量 max 改成 sum：任务进度还接收 craft/smelt/anvil 的增量事实，BQ 在同一 Task 收到匹配产物后会把绝对统计与事件进度合并。正确实现需分开保存逐来源统计基线并与事件增量协调；这可能触及进度模型和数据库持久化合同。
- 本轮没有改代码或 schema；已把事实键、现状例子和实现影响补入开发导航及唯一能力矩阵。已向用户明确询问跨服样例 5/7 的期望值，答案返回前保持现状，不执行 DDL/部署或访问玩家进度。Crafting 与 M.2 仍 `BLOCKED`。

### 2026-10-10 - BQ 自动检测与消耗提交入口复核

- 用户再次确认任务检测分工：非消耗物品/流体目标应持续自动检测；只有消耗型目标需要玩家明确提交。固定 BQ 源码 `QuestInstance.detect` 显示原版 GUI 每个 Quest 只有一个 Detect/Submit 按钮，服务端按任务定义顺序调用该 Quest 所有未完成 Task 的 `detect`；`TaskRetrieval.onInventoryChange` 则对 `!consume || autoConsume` 自动调用该任务检测。由此可区分“检测何时发生”与“玩家看到几个提交入口”。
- Crafting 细节也已逐段确认：BQ 的 `TaskCrafting.onItemInternal` 在一个必需物品匹配后调用该 Task 的 `detect`，后者重新读取该 Task 的全部统计目标。Base `QuestObservationPlan.craftingStatisticsForOutput` 按匹配 Task 收集其全部目标，`ItemVectorTaskEvaluator` 校验触发产物归属后更新对应 Task；现有测试覆盖同 Task 全目标与无关 Task 排除。此处未发现待修行为差异，也未重跑测试。
- 当前 Base 已满足自动/显式分工，但 UI 是每个消耗 Task 分别提供背包匹配提交与手持栈提交；这不同于 BQ 的 Quest 级单入口，后者可能一次按序消费多个 Task。实现原版聚合入口需要明确跨 Task 重叠需求、部分成功及崩溃恢复的批处理合同，不能简单把多个独立提交串在客户端。
- 已向用户请求在现有逐 Task 提交与 Quest 级一键检测间明确选择；本轮未改运行代码、任务合同或数据库，也未访问玩家进度/生产数据库、执行 DDL、部署或切换 Quest。Crafting 同一玩家跨服绝对计数归并口径仍另待确认；相关自动门槛和 M.2 继续按矩阵标注为 BLOCKED。

### 2026-10-10 - Crafting PARTY 活跃资格调用链复核

- CodeGraph `status` 为 up-to-date；`explore BqPartyCraftingStatisticsMembers / BqPartyActiveQuestQuery / QuestEvaluationPlanner / QuestGameplayEventHandler` 与 `node factMembershipFor`、`node BqHuntPartyAssignmentPolicy.select` 核对完整路径。成员解析要求来源玩家属于唯一 Party，并仅返回本服在线 UUID；solo 回退仅包含来源玩家。`BqPartyActiveQuestQuery` 限定本服来源后检查是否至少一名在线成员的 Quest 当前可活动（前置/`progressWhileLocked`、完成周期、待消费恢复、待领奖与 repeat 时间）；Planner 对统计快照逐玩家写 PLAYER 进度，并由 Party 活跃资格证明该快照属于共享 Quest。匹配产物增量仍按 BQ Task 规则向 Party 成员个人进度扇出。
- 与本地 BQ `TaskCrafting.detect/onItemInternal` 对照后，没有发现 PARTY 在线资格或“制作事件共享、个人统计分别采样”的新代码差异。CodeGraph 对成员解析测试的自动关联不完整，但仓库有专用 `BqPartyCraftingStatisticsMembersTest`；现已直接选择并重跑。
- CodeGraph `impact`：`BqPartyActiveQuestQuery.hasActiveMember` 8 个符号、`BqPartyCraftingStatisticsMembers.localOnlineMembers` 6 个；`affected` 对四个入口文件返回 51 个宽泛候选，按调用链选 Core 五类与根事件分发类。Docker `galaxy-dev:local` 无网络运行 Core 五类 50 项、根 `QuestCraftingStatisticsDispatchTest` 4 项，共 54 项，0 skipped/failure/error。首次无网络配置因 convention plugin 访问 `raw.githubusercontent.com` 失败；关闭 Spotless 后 Core 通过；根定向测试另排除只负责下载许可证的 `prepareUi2QzLicense`，因此这不是打包/自包含验收。未部署或访问数据库。
- 仍待用户选择的是不同服务器的同一玩家个人绝对统计如何合并，以及 Quest 级批量 Detect 与现有逐 Task 消耗入口的交互差异。本轮没有代码/数据库变更；`git diff --check` 通过。未访问玩家进度或生产数据库，未执行 DDL、部署、Quest 开关或服务器操作。

### 2026-10-10 - Crafting 检测分工确认与 DefaultQuests 样本复核

- 用户最新确认：非消耗库存任务应持续自动检测；只有消耗型任务需要玩家提交/检测。此语义与 Base 当前非消耗快照、消耗任务显式逐 Task 提交一致，不需要新增“检测合成”按钮，也不需要改变自动统计复评。
- 在用户已授权读取的 S2 `DefaultQuests/Quests` 静态定义目录中，只读复核统计检测启用的 6 个 Crafting occurrence：每项均只有一个目标物品，`allowCraft=1`、`allowSmelt=1`、`allowAnvil=0`、`ignoreNBT=0`。没有读取 `Players`、在线状态、玩家进度或数据库；该样本形状不要求扩展 Crafting 统计 matcher。
- CodeGraph `status` 为 up-to-date；`explore QuestGameplayEventHandler submitCraftingStatistics BqPartyInventorySnapshotFactSink AuthenticatedCraftingStatisticsDetectionService BqPartyCraftingStatisticsScope QuestObservationPlan` 核对当前接线，`impact detectAutomatically` 与 `impact BqPartyInventorySnapshotFactSink.accept` 确认事件/快照调用链，`affected` 给出 75 个宽泛测试候选。结合实际调用链定向回归 Core 6 类 100 项、根项目 3 类 25 项，共 125 项执行，0 skipped/failure/error；本轮没有代码变更，`git diff --check` 通过。
- 这次答复确认的是自动/显式触发语义，并未明确是否也要把 Base 当前逐 Task 消耗入口合为 BQ 每 Quest 一个批量 Detect；该差异涉及多个消费 Saga 的先后、部分成功与恢复，不擅自改变。相同玩家多服本地绝对统计 5/7 的合并值也仍未确认，保持现有最大值实现但不宣称为用户合同。Crafting/M.2 仍 `BLOCKED`；未连接数据库、未执行 DDL/部署/Quest 开关，也未触碰 S1/S2 运行服务。

### 2026-10-10 - R.2 当前工作树对 S2 DefaultQuests 全量定义解析

- CodeGraph `status` 为 up-to-date；`explore BqQuestDefinitionImporter BqBehaviorInventory`、`explore BqInventoryMain BqMigrationPlanner.plan`、`explore BqBehaviorInventory.toText BqCompatibilityReport.toText` 核对只读解析入口、结构校验和输出口径。无代码变更，因此未运行 `impact/affected`；工作树已有改动均保留。
- 以 `galaxy-dev:local` 运行 `:galaxy-quest-bq-importer:behaviorInventory -PbqDefaultQuests=/quest-source --no-daemon --no-configuration-cache -PforceToolchainVersion=17`，S2 `DefaultQuests` 仅以 `:ro` 挂载。当前 importer 实际解析 3,744 个 Quest、5,620 个 Task、5,448 个 Reward、47 个章节、3,847 个 placement；来源规划摘要 hash 与既有基线一致，`applicable=true`、ERROR 计数为 0、未知 Task/Reward 类型为 0。仍有 `UNPLACED_QUEST=64` 与 `SEEDED_XOR_CYCLE=4` 警告。所有类型的 `ADAPTED` 仅证明可解析；行为矩阵仍将运行兼容状态标为 `BLOCKED`。
- Gradle 任务 `BUILD SUCCESSFUL`，约 18 秒；这是全量定义导入/规划门禁，不是 JUnit 数量或运行行为测试。没有连接 PostgreSQL、读取玩家进度、读取其他 S2 配置或触碰运行服务；未执行 DDL、部署、Quest 开关或 S1 操作。文档同步更新导航、M.2 行为矩阵与权威能力矩阵，`git diff --check` 通过。

### 2026-10-10 - 手动消耗任务入口条件回归

- CodeGraph `status` 起始为 up-to-date；`explore`/`node` 检查 `TerminalQuestCenterSnapshotMapper.detail`、`ConsumptionRequestAuthorizer.supports` 与 Quest 中心 UI；`impact` 确认 mapper 的直接范围，改后 `sync` 更新测试文件 33 nodes，`affected` 只选中 `TerminalQuestCenterSnapshotMapperTest`。
- 新增 `exposesManualSubmissionOnlyForConsumingInventoryTasks`，验证 `consume=true && autoConsume=false` 的 Retrieval 仅在消费运行时可用时暴露手动入口；非消耗任务、`autoConsume=true` 与运行时不可用均隐藏。未改生产代码、任务合同或网络协议。
- Docker Gradle 根 `:test` 成功：123 个测试套件、591 个测试，67 个被条件跳过、0 failure/error；目标 mapper 类 4 项全部通过。跳过项是当前根测试环境的条件跳过，本轮没有配置或连接隔离 PostgreSQL，因此不把这些跳过计作实际执行。`git diff --check` 通过。
- 此回归只锁定“非消耗自动观察、消耗显式提交”的入口资格，不解决每 Quest 一个批量 Detect 与逐 Task 入口的选择，也不解决同一玩家跨服绝对统计归并口径。Crafting/M.2 仍 `BLOCKED`；未读取玩家进度/生产数据库，未执行 DDL、部署、启用 Quest 或停用 BQ。

### 2026-10-10 - BQ 当前工作树全量测试复验

- 首次使用通用构建脚本运行根 `test` 时，UI2 Demo 的 Java2D 预览缺少开发专用 CJK 字体，导致 2 个 Demo 测试失败；异常明确为 `no CJK preview font available`，不是布局/渲染断言失败。
- 按 `scripts/render-ui2-visuals.sh` 的既定方式，将宿主 Noto CJK TTC 只读挂载到开发容器并设置 `UI2_PREVIEW_FONT`，重跑 `test assemble verifyUi2SelfContained`，`BUILD SUCCESSFUL`。当前所有 XML 合计 1,039 项，其中 924 项未跳过、115 项条件跳过，0 failure/error；`:galaxy-quest-core:test` 报告 270 项、0 跳过/失败/错误。字体仅供预览测试，不进入源码或运行 JAR。
- 本轮未改运行代码、数据库或用户交互。没有连接生产数据库或读取玩家进度，未执行生产 DDL、部署、Quest 开关或服务器操作；文档更新后 `git diff --check` 通过。

### 2026-10-10 - BetterQuesting 阶段摘要与实现状态对齐

- 发现权威能力矩阵顶部仍称 R.2.11 正在实施，而同一文档、开发导航及 Worklog 已记录到 R.2.16；这会使后续执行顺序误判。将矩阵摘要统一为 R.1 与 R.2.1–R.2.16 均有自动回归记录，但 M.2 整体继续 BLOCKED，并明确 Crafting 两项待确认合同及真实 Forge/GTNH、奖励掉落/存档崩溃证据缺口。
- CodeGraph `status` 为 up-to-date；`explore` 检查进度 importer/mapper/conflict analyzer 与 Core/JDBC progress snapshot/repository，确认当前仍无 BQ 玩家进度 PostgreSQL 写入器，M.3 未开始写入实现。这里只同步阶段摘要，不擅自提前进入 M.3。
- 仅改文档；没有代码、数据库或运行状态变更。本轮不重跑测试；更新后 `git diff --check` 通过。

### 2026-10-10 - Crafting 来源服计数归并合同实现与 Quest Detect 边界审查

- CodeGraph 起始 `status` 提示 1 个待同步源文件，`sync` 更新完成；之后 `status` up-to-date。`explore` 核对消费 Saga、认证授权、Quest 页面动作和 Runtime 完成路径；`impact TaskProgress` 覆盖 202 个符号，`impact ItemVectorTaskEvaluator.evaluate` 覆盖 13 个直接符号，`impact JdbcQuestRuntimeRepository` 覆盖 116 个符号。`affected` 对三处生产文件给出 92 个宽泛测试候选，实际聚焦 Core `TaskProgressVectorTest`/`BqCompatibleTaskEvaluatorsTest`、PostgreSQL 往返/schema contract 与根 Party/JDBC 集成类。
- 按用户确认的合同，实现同一玩家跨服务器的 Crafting 统计：每个来源分别取 `max(来源事件累计, 来源绝对统计快照)`，来源之间相加；不同 Party 成员仍是互相独立的 PLAYER 进度。`TaskProgress` 保存来源贡献及 legacy baseline；旧数据作为非叠加下限，避免升级后把既有聚合值重复加到各服来源上。JDBC 使用 `source_progress_json` 持久化；新增 idempotent migration 文件，但没有执行 DDL。首次隔离 JDBC 运行发现新 insert 少一个 JSONB 参数占位符，修复后回归通过。
- Docker 隔离验证实际执行 Core 273 项、Quest PostgreSQL 55 项、根 Quest Party/JDBC 15 项，共 343 项；0 skipped、failure、error。隔离 PostgreSQL 16 测试库仅在临时网络/tmpfs 容器运行，无生产 DSN/卷/对外端口；测试后已停止并删除本任务创建的精确容器与网络。`:assemble verifyUi2SelfContained` 通过：39 actionable tasks，7 executed、32 up-to-date。最终 `git diff --check` 在文档收口时复核。
- Quest 级 Detect 已由用户选定：UI 仅提交 Quest ID，服务端按发布定义顺序逐项处理未完成消耗 Task，使用每 Task 独立持久化 Saga/登录恢复，不承诺跨数据库和背包全有或全无。实现审查发现 `QuestRuntimeService` 在每条任务事实后立即判定完成、生成 entitlement 并冻结已完成周期；OR/可选任务可能在批次中途完成。继续消费会有物品被扣除但进度写不进去的风险。因此本轮没有添加 Quest Detect 入口，已向用户确认应采用“批次末延迟完成结算”还是“首次达成即停止”；未得到选择前不改奖励时机。
- 开发导航、权威能力矩阵、行为兼容历史与文档索引已同步新的归并合同和当前实现边界。数据层和源计数自动测试完成不代表 M.2 或 BetterQuesting 替代完成；来源 migration 未执行，没有生产数据查询、部署、Quest 开关、生产 DDL 或 S1/S2 操作。真实 Forge/GTNH 事件、多服在线时序和 migration 升级演练仍待验证。

### 2026-10-11 - R.2.18 Fluid Quest Detect 保留不可分割容器的实测超量

- 对照本地固定 BQ `TaskFluid.detect` / `Detector.run`：每个背包槽依序检测；消耗时请求目标剩余量，但将 fluid container 实际返回量加到该项进度。不可分割注册容器可整份排空并使进度高于目标。只读 S2 `DefaultQuests` 当前有 15 个 Fluid Task、16 项需求，唯一消耗项为 Honey 1,000 mB；未读取玩家进度、在线状态或其他服务器配置。
- 原 Base 适配器会完整排空不可分割容器，却只把剩余需求传给进度，和 BQ 的数值进度不等价。现由消费 Saga/JDBC 保存实际 fluid 排出量，Fluid evaluator 允许实测量超过当前目标；请求与数据库校验只为 `fluid` 允许该实际值，其它物品/XP 消费保持请求上限。通用 Task 目标完成判定和 Quest Detect 批次末结算未改。
- 新回归含 Core 400+750=1,150/1,000、Minecraft adapter 过量返回，以及合成 BQ Fluid JSON 导入→隔离 PostgreSQL 发布→Quest 级 Detect→容器扣除/空容器返回→JDBC 进度 1,150→重放不再扣除。该测试模拟不可分割容器，不证明 Forge/GTNH registry 的真实注册行为。
- CodeGraph `status/sync` 后同步 8 个文件；`impact` 覆盖 TaskProgress、ConsumptionApplied evaluator、Fluid adapter、ConsumptionRequest 和 JDBC `markApplied`；`affected` 给出 94 个宽候选后按真实调用链选择 Core、Root adapter 与 PARTY/JDBC 测试。Docker Gradle：Core 279、Quest PostgreSQL 58、Importer 49 均 0 skipped/failure/error；根 `:test` 593 项、52 条条件跳过（缺少其它业务模块各自 DB 配置）、541 项实际执行、0 failure/error；根隔离 PARTY/JDBC 类 15 项 0 skipped。`:assemble`、`verifyUi2SelfContained` 成功；`:test` 首次通过 Docker 内未安装的全局 `gradle` 启动而未运行，改用 `./gradlew` 后成功。Gradle 有非致命 Git safe-directory/no-tag 版本提示；本批不部署、不生成生产版本。`git diff --check` 通过。
- PostgreSQL 16 测试库为临时容器、512 MiB tmpfs、无数据卷、仅 `127.0.0.1:55498`；测试后已删除，端口确认释放。未执行生产 DDL/迁移、未部署、未开关 Quest/BQ、未读取生产玩家进度或触碰 S1/S2。此项只关闭一个 Fluid 自动语义差异；Fluid 与 M.2 仍 `BLOCKED`，Forge/GTNH 实机及跨服时序未验收。

### 2026-10-11 - Crafting PARTY 延迟事实归属审计（R.2.19 前置；决策待确认）

- 按目标先核对实际 Crafting 业务链。CodeGraph `status` 显示索引 up-to-date；`explore` 检查 Crafting 事件、统计 Detect、Party scope 和有界重试；`node` 核对 `QuestGameplayEventHandler.submitCraftingStatistics/onItemCrafted/localOnlineStatisticMembers`、`OrderedQuestFactRetryQueue.retry`、`RuntimeQuestFactSink.accept`、`QuestEvaluationPlanner.requestsFor/accepts/bqPartyActivityMembership`、`BqHuntPartyAssignmentPolicy.select` 与 `BqPartyCraftingStatisticsMembers.localOnlineMembers`；`impact` 覆盖 `submitCraftingStatistics`（6 个符号）和 Party 在线成员解析（6 个直接符号）。固定 BQ 工作树为 `Reference/BetterQuesting` HEAD `6e80f68c22ede0e52247c6ea8d1587f5010eb77e`。
- 固定 BQ `EventHandler.onItemCrafted` 在同步回调内构造 `ParticipantInfo`，以事件时在线成员 Quest cache 取得 shared Quest；`TaskCrafting.onItemInternal` 对当时 `ALL_UUIDS` 写入匹配产物计数，再调用同 Task `detect()`。BQ 的 `TaskCrafting.detect()` 对各 UUID 单独读取当前服务端在线玩家的 `objectCraftStats`。Base 的个人统计事件会在触发时解析本服在线 Party UUID、读取并冻结各自的统计值；但 generic `galaxy:item_produced` 事实未携带事件时 Party roster/可接受 Quest，失败事实被队列重试后，Planner 再按数据库当前 membership 与活动 Quest 资格规划。故 Party 变更与队列积压交错时，共享 Crafting 产物可能从事件时旧 Party 漏记，或在重试时被归入新 Party；普通无故障同步路径不暴露此差异。
- 这不是源服计数 max/sum 算法或当前 60 tick 个体统计轮询缺陷；它是 R1-03 队列重试语义和 BQ 同步事件时点之间的跨服归属合同缺口。完整对齐 BQ 建议冻结可信的事件时 Party/成员及活动 Quest 资格后再异步应用，但 PostgreSQL 不可用时如何取得/验证快照，以及旧成员在离队后是否继续收到待处理事件，需要一起设计。暂不修改代码或做部分快照，避免只冻结 Party UUID 却仍使用重试时 roster/Quest 资格造成假等价。
- 已更新开发导航、唯一能力矩阵与文档索引。仅做源码/调用链审计；本轮没有代码或回归测试、没有访问 PostgreSQL/玩家进度、没有执行 DDL、部署或 Quest/BQ 开关操作。`git diff --check` 为最终文档门槛；下一步需用户选择事件时归属（推荐，匹配 BQ）或接收时归属（保留当前行为并记录差异）。R.2.19 未实施；R.2.6、Crafting 和 M.2 仍 `BLOCKED`。

### 2026-10-11 - Crafting 与 Quest Detect 合同复核及隔离回归

- 用户再次确认：非消耗库存任务自动观察，消耗型任务通过 Quest 级 Detect；同一玩家的 Crafting 按每个来源服 `max(事件累计, 绝对统计快照)` 后求和，不同 Party 成员仍是独立 PLAYER 进度。该回答确认既有开发导航合同，没有授权改变 R.2.19 的事件时/接收时归属。
- 固定 BQ `EventHandler.onItemSmelted/onItemAnvil` 与 `TaskCrafting.onItemInternal/detect`；CodeGraph `status` 为 up-to-date，`node/impact` 核对 `QuestGameplayEventHandler.onItemSmelted/onItemAnvil`、`TaskProgress.advanceFromSource`、`JdbcQuestRuntimeRepository.findProgress`、Quest 页面 Document/Mapper 与已有目标测试。`affected` 对核心/JDBC/Forge 入口给出 94 个宽候选，按直接调用链运行 Crafting evaluator、统计采样、成员资格、Planner/Runtime、Minecraft 事件分发、任务中心动作和 PostgreSQL 回归。
- Docker 定向测试使用 `galaxy-dev:local`、无外网的容器；总计 Core 101、UI2 Terminal 6、根 Quest 事件/Mapper 10、根隔离 Party/JDBC 2、Quest Postgres 隔离 2，共 121 项，0 skipped/failure/error。覆盖来源内最大值/来源间求和、不同玩家隔离、Quest Detect 页面合同、批次游标持久化、source-progress migration 幂等。根标准构建曾被旧 root-owned `injectTags` 输出目录阻挡；没有移动/删除旧产物，改用仓库 `.gradle/codex-root-build` 临时输出后通过。一次初始 Gradle 配置尝试因远程 Spotless 资源不可达未进入测试，最终在 `--offline -PdisableSpotless=true` 下通过；临时 init 脚本已删除。
- PostgreSQL 为临时 PG16，512 MiB tmpfs、无宿主卷、无发布端口，仅处于 `--internal` Docker 网络；只注入新建 `quest_it_test` 测试库凭据。测试后数据库容器和网络均已删除并复核不存在。未访问生产玩家进度/数据库、未执行生产 DDL、未部署、未切换 Quest/BQ、未触碰 S1/S2。
- 本轮不改运行代码，因此 R.2.19 尚未实施。仍需单独决定 Party 变更与有界重试交错时，Crafting 事实按事件发生时快照还是数据库成功接收时成员归属；没有把统计来源合并合同误当作这项归属决定。Crafting/M.2 以及整体 BetterQuesting 替代继续 `BLOCKED`。`git diff --check` 收口时复核。

### 2026-10-11 - Crafting 兼容最小回归复验

- 按用户项目技能先读当前产品方向文档；CodeGraph `status` 为 up-to-date，`node` 复核 `onPlayerTick`、60 tick 自动统计检测、服务器 tick 有界重试与 FIFO `retry`，`affected` 对 Crafting、TaskProgress、终端 Detect 入口给出 135 个宽泛候选。调用链没有发现绕开 R.2.19 Party 归属选择即可安全修正的新增行为差异。
- 用无网络 `galaxy-dev:local` 对当前工作树重跑核心 evaluator/统计来源/Party 活跃资格/观察计划/来源进度 5 个测试类 73 项、UI2 Quest 级单入口 6 项、Forge 事件分发/故障及详情 Snapshot mapper 3 类 10 项，共 89 项，0 skipped/failure/error。没有运行 PostgreSQL 测试或声称验证 Forge 实机。
- 根测试首次被历史 root-owned 测试结果文件阻断；没有清理或覆盖旧输出，改用 `.gradle/codex-isolated-build` 临时输出目录。其后根 Forge 构建因 `prepareUi2QzLicense` 访问 `raw.githubusercontent.com` 在测试前失败；按既有离线边界排除此纯下载任务后，所选 10 项通过。临时 init 脚本随后删除；隔离构建报告保留在忽略目录。
- 这次仅复验已确认合同，不改业务代码或数据库。R.2.19 仍需用户明确事件发生时快照，还是数据库接收/重试时成员归属；M.2 继续 `BLOCKED`。没有生产玩家进度/数据库访问、生产 DDL、部署、Quest/BQ 开关或 S1/S2 服务操作。

### 2026-10-11 - R.2.11 Item typed NBT 导入交付回归

- 固定 BQ 源码仍为 `Reference/BetterQuesting` HEAD `6e80f68c22ede0e52247c6ea8d1587f5010eb77e`；对照 `RewardItem`、`RewardChoice`、`BigItemStack`、`NBTConverter.NBTtoJSON_Compound(format=true)` 与 `NBTReplaceUtil.replaceStrings`。确认 BQ 使用带类型后缀的 Compound/List JSON；Base importer 保留该 raw typed JSON，`BqTypedNbtCodec` 解码后构造 Minecraft NBT 并递归替换变量。本轮未发现当前 DefaultQuests 注册名/奖励数量或已支持类型的静态差异。
- 按用户此前授权只读扫描 S2 `config/betterquesting/DefaultQuests/Quests`：Item 4,181 条/6,271 栈、Choice 957 条/3,062 选项；9,333 个 Item/Choice 栈的 `id` 均为字符串，Count 均为正数、范围 1–8,192；440 个栈有非空 Tag。匿名递归类型计数为 NBT 1–6、8–11，包含 5 处 IntArray。没有读取 Players、其他配置、生产玩家进度或数据库，也未导出任务名/UUID/NBT 内容。
- 为 `BqItemRewardImportDeliveryTest` 增加 `importedBqItemRewardPreservesTypedNestedNbtListsAndArrays`，走真实 importer→RewardDefinition→Minecraft item handler 替身，验证 byte/short/int/long/float/double、嵌套 Compound/List、VAR_NAME/VAR_UUID、ByteArray 和 IntArray。CodeGraph 起始 `status` up-to-date，`explore` 核对 importer/NBT codec/reward handler；`impact BqTypedNbtCodec` 覆盖 20 个符号，`affected` 将新测试收敛到本类；`sync` 更新 1 文件/66 nodes。
- Docker `galaxy-dev` 隔离运行根测试类 5 项，0 skipped/failure/error。首跑在 `injectTags` 清理历史 root-owned `build/generated/sources/injectTags` 时因权限失败，未改权限、移动或删除该文件；随后为每个 Gradle project 设置全新的忽略输出根 `.gradle/codex-reward-nbt-build-20261011`，定向测试 `BUILD SUCCESSFUL`。测试 XML 显示 5/0/0/0；Minecraft Log4j 对 `logs/latest.log` 有非致命 Permission denied 诊断，未影响测试。没有数据库任务，未运行 `assemble`/自包含检查（本批只有测试改动，运行时产物未变）。
- 更新开发导航、M.2 行为矩阵、权威能力矩阵和文档索引；`git diff --check` 通过。Item 世界掉落与玩家存档/entitlement 的崩溃窗口、真实 Forge/GTNH 玩家和 WorldSave 仍 `BLOCKED`；R.2.19 Party 事件时点归属未由本轮答案决定，M.2 整体仍 `BLOCKED`。没有生产 DDL、部署、Quest/BQ 开关或服务器操作。

### 2026-10-11 - Fluid 返回空容器空间不足语义复核

- 在上述 item 奖励测试收口后继续 M.2 源码核对。CodeGraph `status` up-to-date；`explore TaskRetrieval/TaskFluid/MinecraftPlayerConsumptionPort.planFluidsDetailed/ConsumptionAppliedTaskEvaluator` 与 `impact planFluidsDetailed` 覆盖适配器和隔离 PARTY/JDBC 回归调用链。固定 BQ `TaskFluid.getFluid` 的 `IFluidContainerItem` 与 `FluidContainerRegistry` 两条 drain 路径，确认成功排空后都会先从输入槽扣除，再尝试把单个空容器放回玩家背包；放不下则调用 `dropPlayerItemWithRandomChoice` 丢到世界。
- Base `planFluidsDetailed` 采用先计划再整体应用；若空容器无法放回当前玩家库存，`insert` 返回 false 并令整次 fluid 计划为 null，原库存保持不变。因此这是已确认的安全失败差异，不是容器未成功排空后的返还丢失。完全复制 BQ 的世界掉落语义，需要把空容器溢出纳入 player marker/消费 Saga，并面对玩家存档与 WorldSave 两侧无法原子提交导致的掉落重复或丢失窗口。本轮没有更改该策略，也没有声称兼容；待后续确定安全边界后单独处理。
- 本轮仅静态读固定 BQ 源码与当前 Base 适配器，无新增测试/代码、无 DB 查询或生产操作。该差异与当前 M.2 文档中“无空容器槽 Base 拒绝仍不同于 BQ 世界掉落”记录一致；Fluid 与 M.2 继续 `BLOCKED`。

### 2026-10-11 - R.2.20 Crafting 统计超目标值保留

- 用户确认 Quest 级 Detect，以及同一玩家按每个来源服 `max(事件累计, 绝对统计快照)` 后求和；这没有决定 R.2.19 的 PARTY 事实在重试期间如何归属，本轮继续不改该边界。
- CodeGraph `status` up-to-date；`node TaskProgress.aggregate`、`node JdbcQuestRuntimeRepository.readTaskProgress` 确认聚合只在 Core 计算、JDBC 重读时核验 JSONB/可见值一致；`impact TaskProgress.aggregate` 命中 10 个符号，`affected` 给 79 个宽候选后，按调用链选择 Core 全套与一个 Crafting/JDBC 集成回归。固定 BQ 源码 `TaskCrafting.detect()` 将更大的 `objectCraftStats` 绝对计数写回；`TaskProgressableBase.setUserProgress()` 不按任务目标截断。
- BQ `onItemInternal()` 对每名成员的单服事件向量按目标封顶，而 `detect()` 统计快照不封顶；Base `advanceFromSource()` 现按该单服 target 封顶，`aggregate()` 则对每来源取 `max(封顶事件累计, 未封顶绝对快照)` 并跨来源求和。legacy baseline 仍不被截断，避免丢弃历史已保存值。新增 Core 回归验证事件封顶、同来源统计值覆盖事件值、来源间求和后值可高于目标；扩展 PostgreSQL PARTY Crafting 测试，目标 4、统计快照 5，验证 `progress_values` 与 `source_progress_json` 经 JDBC 写入/重读仍一致且保留 5。Completion 仍以 `value >= target` 判定。
- Docker `galaxy-dev` 全套 `:galaxy-quest-core:test` 最终实际 281 项，0 skipped/failure/error；第一次全套运行发现既有断言仍期待统计目标截断（7/5 被断言为 2/3），按 BQ 统计语义更新。后续源码复核又发现 BQ 产物事件自身会封顶，补了事件封顶回归并重新运行全套，最终 281/0/0/0。隔离 PostgreSQL Crafting/JDBC 定向集成 1 项通过。PG16 使用新建 `--internal` 网络、512 MiB tmpfs、无数据卷和无端口发布；只配置 `quest_it_test` 合成库凭据，测试后容器/网络均已移除并确认不存在。
- `:assemble :verifyUi2SelfContained` 最终通过（28 actionable tasks，4 executed、24 up-to-date）。首次尝试遇到历史 build 结果文件权限拒绝，未改权限/清理旧输出，改用独立 `.gradle/codex-crafting-over-target-build-20261011`；Forge Shadow 依赖变换前显式生成 UI2 JAR。离线环境中的 `prepareUi2QzLicense` 下载动作未能完成，使用已有缓存许可证（SHA-256 与构建中声明值相同）填充隔离输出，并仅跳过该下载任务；JAR 自包含验证本身实际运行且通过。`git diff --check` 在最终文档修改后另行执行。
- 更新开发导航、M.2 行为矩阵与权威能力矩阵。该变更只关闭 Crafting 数值聚合的窄差异；真实 `objectCraftStats`/Forge 事件、Party 重试时点、生产迁移与玩家进度均未验证/访问。没有执行生产 DDL、部署、Quest/BQ 开关或服务器操作；R.2.19 与整体 M.2/BQ 替代仍 `BLOCKED`。

### 2026-10-11 - R.2.19 Party 重试归属实现边界细化（仅源码/架构审计）

- CodeGraph `status` 为 up-to-date；`node` 检查 `OrderedQuestFactRetryQueue.retry/enqueue/deliver`、`QuestGameplayEventHandler.onItemCrafted`、`QuestEvaluationPlanner.requestsFor/forcedMemberships`、`BqHuntPartyAssignmentPolicy.select`、`BqPartyCraftingStatisticsMembers.localOnlineMembers`。事实当前不可变且重试保留原键，但普通 Crafting 事实不携带 Party/成员快照；Planner 按重试时 membership 重建请求。
- 进一步核对 `QuestModule`：Party membership 由 `JdbcParticipantMembershipResolver` 读取；`BqPartyActiveQuestQuery` 还经 JDBC 检查每个在线成员的前置、任务进度、重复周期、未完成消费和待领奖状态。仅冻结 Party UUID 或 roster 不能固定 BQ 事件时活动 Quest 资格。事件入口没有可在 PostgreSQL 故障时使用的新鲜权威 membership/进度快照；使用无新鲜度界限的缓存会引入另一种误归属。`OrderedQuestFactRetryQueue` 仅在内存中，进程崩溃后不恢复，因此快照方案也不能覆盖当前既定的进程崩溃窗口。
- 用户已通过另一组确认 Quest 级消耗 Detect 与来源内 max/来源间 sum；这两项不等于 R.2.19 Party 故障时点选择。事件时归属与 BQ 同步语义更接近，但若采用，必须定义缓存新鲜度、无可信快照时的失败/诊断、旧成员迟到投影与崩溃边界；重试时当前 Party 则维持实现简单但保留已记录的 BQ 差异。已向用户单独请求选择，本轮未更改业务代码/数据合同、未运行测试、未访问 PostgreSQL/玩家进度、未执行 DDL/部署或 Quest/BQ 开关。文档差异复核与 `git diff --check` 为本批门槛。

### 2026-10-11 - R.2.23 Crafting PARTY 事件匹配前置过滤

- CodeGraph `status` 起始显示 3 个待同步文件，`sync` 后复核 `ItemVectorTaskEvaluator.matchesCraftingStatisticsTrigger`、`BqPartyTaskProgressScope.isCraftingEvent`、`QuestEvaluationPlanner.bqPartyActivityMembership`；`impact` 显示新 matcher 仅进入 Party scope，随后影响 `requiresUnlockedPartyMember/supports`；`affected` 对三个改动文件给出 56 个宽候选。固定 BQ `TaskCrafting.onItemInternal` 的必要条件是产物命中某项需求；Base 之前只按通道开关判断是否进入 PARTY 在线资格检查。
- 新增 `ItemVectorTaskEvaluator.matchesCraftingProgressEvent`，与实际 Task evaluator 共用通道准入和 `matches`，涵盖注册名、meta/wildcard、damageable、OreDictionary、NBT/partial match。`BqPartyTaskProgressScope` 仅在真实输出命中至少一个导入 BQ Crafting 需求后认定为共享任务事件。新增 Planner 回归验证无关 Gold 产物只产生来源玩家本地请求、不触发 Party 活动查询或 PARTY fan-out；真实命中输出既有 Party 路径不变。
- Docker `galaxy-dev:local` 无网络运行隔离输出下 Core 全套：62 suite / 284 项，0 skipped/failure/error。首跑先被既有 root-owned `galaxy-quest-core/build/test-results/test/binary/results-generic.bin` 权限拒绝，未改权限；隔离运行的新测试首轮发现测试错误假定个人 `factMembership` 为 null，按 Core 合同修正为来源 PLAYER membership。随后 Planner 类 24 项、Core 全套最终均通过。`git diff --check` 与本次文档空白检查通过。
- 本批只改纯 Java Core 与文档；没有运行 PostgreSQL、Forge 实机、根 `assemble` 或自包含门禁，因为变化只改变无关事件的资格预检查，不改事实/JDBC合同。R.2.19 的“事件时 Party / 重试时当前 Party”选择仍待答，R.2.23 不将其视作已解决；未访问生产进度/数据库、未执行 DDL/部署/Quest/BQ 开关或服务器操作。

### 2026-10-11 - M.2 Meeting 半径实现与文档状态复核

- CodeGraph `status` 为 up-to-date；`impact MeetingTaskEvaluator.evaluate` 显示 Core evaluator 与两项 Meeting 回归，`affected` 为 71 个宽候选。固定 BQ `TaskMeeting.detect` 对每个任务独立查询 `player.boundingBox.expand(range)`；当前工作树实际已在 `MeetingTaskEvaluator` 对最大半径共享快照执行逐任务实体 AABB 交集过滤，缺少几何或几何非法时拒绝完成，精确半径旧快照仍可按其已筛选事实处理。已有回归覆盖远处实体排除、AABB 相交、只接触边界、缺失坐标与非法坐标。
- 发现 M.2 行为矩阵的 Meeting 汇总行仍写“过滤缺失”，与已存在的 R.2.9 实现、测试及同一文档后文相矛盾。未改生产代码；已将摘要和能力矩阵阶段行同步到现有代码证据，保留真实 Forge/GTNH `EntityList`、NBT、tick 与多服 PARTY 验收为未完成。
- 使用 `galaxy-dev:local` Docker 环境、`--network none`、隔离 Gradle build dir 强制执行 `:galaxy-quest-core:test --rerun-tasks`：62 suite / 284 tests，0 skipped/failure/error；包括 Meeting AABB 过滤回归。最终 `git diff --check` 通过。
- 本次只修正文档状态，没有访问 PostgreSQL/玩家进度、执行 DDL、部署、启用 Quest 或停用 BQ。R.2.19 Party 事实重试归属时点仍待用户选择；M.2 与整体 BQ 替代继续 BLOCKED。

### 2026-10-11 - M.2 Item 奖励最大堆叠数文档复核

- CodeGraph `impact MinecraftItemRewardDeliveryHandler.deliver` 覆盖交付 handler 与现有恢复/溢出测试。固定 BQ `BigItemStack.getCombinedStacks()` 按物品 `getMaxStackSize()` 拆分；当前 Base 奖励生成和世界溢出分栈也使用物品 max stack，背包槽位插入另外取 `min(item max, inventory 64)`。
- 对照已有回归，确认 max stack=128 的物品发放 130 件时世界溢出栈为 128/2；导入 JSON 的普通物品奖励 8,192 件在 36 格背包后余量按 64 分栈掉落。发现行为矩阵仍误写“Base 也按 64 上限分栈”，已更正为“世界溢出按物品上限、库存插入另受 64 限制”。未改生产代码。
- Docker `galaxy-dev:local` 无网络环境定向实跑 2 项：`MinecraftRewardDeliveryTest.rewardOverflowUsesBqItemSpecificMaximumStackSize` 与 `BqItemRewardImportDeliveryTest.importedBqMaximumItemRewardSplitsAndDropsOverflowOnce`，0 skipped/failure/error。首次运行被 `prepareUi2QzLicense` 的 GitHub 下载前置阻断；按离线构建边界排除此下载任务后，所选测试成功。此处没有执行 JAR/自包含门禁。
- 测试使用替身玩家，不证明真实 Forge 掉落、玩家存档或 WorldSave 崩溃窗口；未访问 PostgreSQL/玩家进度，未执行 DDL、部署、启用 Quest 或停用 BQ。Item 奖励与 M.2 仍 BLOCKED。

### 2026-10-11 - S2 消耗型 Quest Detect 范围复核

- 按用户授权只读 `/mnt/m600/zhitai-data/gtnh/data/Galaxy_GTNH284_S2/config/betterquesting/DefaultQuests/Quests`，只输出 Task/Reward 类型及布尔配置计数，不读取 Players、服务器配置、数据库或任何玩家进度。Task 画像为 308 个 `consume=true` occurrence：307 Retrieval、1 Fluid；全部 `autoConsume=false`；无显式 `isGlobal=true`（283 个显式 false、25 个省略并按 BQ 默认 false）；284 个有 `globalShare=true`，但该字段不能推出 Party 进度同步。Reward 画像为 Item 4,181、Choice 957、QuestCompletion 308、XP 2；未出现 command 或 scoreboard reward。仅有的 2 项 XP 定义已在既有矩阵中记录为 `isLevels=true`，Base 已有 XP handler 与 typed-import 测试，本轮未执行该测试。
- 复核固定 `Reference/BetterQuesting` 源码：`TaskRetrieval.detect` 与 `TaskFluid.detect` 在 `fullySyncQuests=false` 时对消耗型检测使用发起玩家身份/库存；源码默认值为 false，注释标记 `[NYI]`。BQ 服务器的实际配置没有读取，因此不能认定 GTNH 运行值一定等于源码默认值。现有 Base PLAYER-only Quest Detect 与当前定义范围在该默认语义下吻合；本项没有扩大到 PARTY 消耗，也没有改变模型/代码。
- 本轮先前 `:galaxy-quest-bq-importer:behaviorInventory` 因容器首次配置请求 Mojang version manifest 超时而未完成；相同内容的计数通过只读 `jq` 画像取得，故没有把失败命令记为成功测试。未运行测试（纯文档证据同步），没有 PostgreSQL 查询或 DDL、部署、Quest/BQ 开关或 S1/S2 服务操作。`git diff --check` 为本批收口门槛；R.2.19 的 Party 事件时/重试时归属问题仍未由当前答案决定，M.2 继续 BLOCKED。

### 2026-10-11 - Quest Detect 批次与定义版本并发审计

- CodeGraph `status` up-to-date；追踪 `AuthenticatedQuestConsumptionDetectService.detect/continueBatch`、`ConsumptionRequestAuthorizer.publishedVersion/published`、`JdbcQuestDefinitionRepository.publish/findAllPublished` 与 `JdbcConsumptionDetectBatchRepository.findActive/createIfAbsent`。当前发布流程允许新版本发布且保留旧 PUBLISHED 行；目录只返回每个 Quest 的最高版本。Detect 查活动批次时同时要求当前版本；持久化唯一键也含 definition version。因此，已暂停的旧版批次在管理员发布新版本后不会被当前正常 Detect 入口找到，也无法走批次末旧版 settlement，存在“旧版部分消费/提交已发生但 Quest 未结算，且新版本可以再建另一批”的恢复缺口。
- 尚无用户决定要采用“活动批次存在时拒绝发布新版本”，还是“旧批次固定旧定义并按旧版完成/奖励后允许新版本并行”。后者更符合批次保存版本身份的意图，但会允许新版本发布后旧版任务与奖励继续完成；这是管理员发布和玩家奖励可见性的合同变化。本轮只记录问题，不改代码、DDL或运行行为。
- CodeGraph 显示当前定向回归覆盖游标持久化/幂等及来源服恢复，但没有覆盖“部分批次暂停→发布新版本→重启检测”的版本并发场景。未运行测试；`git diff --check` 收口。没有访问 PostgreSQL/玩家进度或服务器配置，没有执行生产 DDL、部署、Quest/BQ 开关或 S1/S2 服务操作。

### 2026-10-11 - Crafting PARTY 统计采样失败路径审计

- CodeGraph `status` 为 up-to-date；`explore` 核对 `QuestGameplayEventHandler.submitCraftingStatistics/localOnlineStatisticMembers/onItemCrafted/onItemSmelted/onItemAnvil`、`QuestModule.createGameplayEventHandler`、`MinecraftQuestFactFactory.craftingStatisticObserved` 与 `AuthenticatedCraftingStatisticsDetectionService.detectAutomatically`。生产组合根用本服在线 Party 成员列表对每个成员读取个人 vanilla Crafting 统计并生成独立事实；失败/空列表时的 handler 明确回退到触发玩家 singleton。自动周期服务拒绝 `participantScope != PLAYER` 或 global Quest，因此 PARTY 绝对统计没有周期补偿路径。
- 结论：一次 PostgreSQL membership lookup 失败可使当次 crafting 事件缺少 Party peer 的统计事实；除非下一匹配产物再次触发采样，否则这些 peer 可能漏记。若事后按当前 Party 重试，会有归属变化差异；若要严格按事件时归属，则需要具备新鲜度边界的 event-time roster/Quest 资格来源。现有代码无此权威缓存，用户尚未决定 R.2.19 事件时或 retry-time 合同，因此本轮只记录风险，不改 fallback 或事实语义。
- 本轮未运行测试或修改生产代码。`git diff --check` 为文档验收门槛；没有访问 PostgreSQL、玩家进度、服务端配置，未执行 DDL/部署/Quest/BQ 开关或 S1/S2 操作。

### 2026-10-11 - Crafting PARTY 周期统计补偿结论更正

- 复核上方故障审计时发现把“统计服务仅处理 PLAYER scope”错误地等同于“不能补齐 PARTY peer”。实际路径是：`QuestGameplayEventHandler.onPlayerTick` 每 60 tick 对每名在线玩家运行 `detectAutomaticCraftingStatistics`；导入 BQ Crafting Task 已进入 `QuestObservationPlan` 自动目标；服务为传入玩家读取个人绝对统计，`BqPartyCraftingStatisticsScope`/Planner 可在当前 Party 活动资格成立时为该玩家建立独立 PLAYER 进度请求。隔离 PostgreSQL 测试 `automaticBqCraftingStatisticsKeepPersonalCountersAndSumPerServerMaxima` 已分别检测 Party 中的 owner 和 peer，并验证个人进度隔离。
- 因此，当次事件 membership 查询失败只会漏掉该次事件采样的 peer 列表；数据库/membership 恢复后，仍在线且资格未变的 peer 可在下一周期由自己的轮询补采，不要求再发生匹配输出。它不冻结事件时 Party roster，也不保证离线、离队或资格已变化的玩家追补；真实 Forge tick 与生产时序仍未验证。该结论修正本 Worklog 上一节、较早 R.2.6 中“统计仅更新触发玩家”的记录、开发导航和 M.2 行为文档中的过度断言；R.2.19 产物事实延迟重试归属仍未决。
- 本轮为代码审计与文档纠错，没有生产代码变化，因此未运行测试；CodeGraph `status` up-to-date，`impact detectAutomatically` 与 `impact detectAutomaticCraftingStatistics` 已确认实际调用/回归范围。未访问 PostgreSQL 或玩家进度，未执行 DDL、部署、Quest/BQ 开关或 S1/S2 操作。

### 2026-10-11 - S2 实际启用 Crafting 统计任务形状复核

- 按用户此前授权只读 S2 `DefaultQuests/Quests` 定义文件，以 `allowCraftedFromStatistics:1 == 1` 筛选实际启用项。当前找到 6 个启用定义，目标形状为 furnace×1、flint×3、chest×1、crafting_table×1、piston×1、flint_and_steel×1；均 `allowCraft=true`、`allowSmelt=true`、`allowAnvil=false`、`partialMatch=true`、`ignoreNBT=false`、meta=0，未用 OreDictionary 目标。没有读取玩家进度、服务端其他配置或数据库。
- 与当前实现对照：CodeGraph `status` up-to-date；`explore QuestGameplayEventHandler.submitCraftingStatistics QuestObservationPlan.craftingStatisticsForOutput MinecraftQuestFactFactory.craftingStatisticObserved BqPartyCraftingStatisticsScope.matchesAutomaticPartyActivity` 及 `explore QuestModule.createGameplayEventHandler` 核实生产接线。事件 matcher 支持的通道与这些真实配置一致；绝对 `objectCraftStats` 按 Item ID 读取且本样本目标无 metadata/OreDictionary 特例。此画像不改变已确认的来源内 max/来源间 sum，也不关闭 R.2.19 的事件重试 Party 归属差异；它只说明当前 S2 启用统计的 Crafting 任务没有额外 Item matcher 形状缺口。
- 这是只读定义检查和文档补充，无代码变化、无需测试。修正 M.2 表格与早期 active-quest 审计的过时句子；`git diff --check` 通过。没有 PostgreSQL 连接/查询、生产 DDL、部署、Quest/BQ 开关或 S1/S2 服务操作。

### 2026-10-11 - Quest Core 当前状态复验尝试

- 按用户确认的 Quest 级消耗 Detect 与 Crafting 来源服 max/sum 合同，尝试重新执行 `:galaxy-quest-core:test`。项目构建脚本及显式 `--offline` 路径均在应用 `com.gtnewhorizons.gtnhconvention` 时遇到 `java.net.SocketTimeoutException: timeout`；直接运行时又发现历史 root-owned 测试结果阻止普通开发 UID 访问。没有修改旧产物权限或删除文件。
- 随后用只对一次性 `galaxy-dev` 容器生效的本机 7892 代理，并以临时 Gradle init script 将所有 project build output 重定向到容器 `/tmp`，强制 `--rerun-tasks` 后完整执行 Core suite：284 项，0 failure、0 skipped、0 errors，`BUILD SUCCESSFUL`。init script 已删除，容器退出后临时输出销毁；工作树旧构建结果保持原样。
- 未更改生产代码、Gradle 项目配置、宿主代理/防火墙或游戏服务。未访问 PostgreSQL、玩家进度或服务器配置，未执行 DDL、部署、Quest/BQ 开关或 S1/S2 服务操作。

### 2026-10-11 - R.2.24 QuestCompletion 缺失目标 no-op 兼容

- 按既有授权只读 S2 `DefaultQuests/Quests`：匿名解析 3,744 个定义、0 个解析错误；308 个 QuestCompletion reward 有 251 个不同目标，219 个可解析、32 个缺失，malformed/missing UUID 为 0。没有读取 Players、服务器运行配置、在线状态或数据库。固定 BQ `RewardQuestCompletion.claimReward0` 在目标 Quest 缺失时直接返回，不阻止来源奖励领取结束。
- CodeGraph `status` up-to-date；`explore JdbcQuestCompletionRewardPort RewardQuestCompletionDeliveryHandler`、`impact JdbcQuestCompletionRewardPort.completeInTransaction`、`explore QuestCompletionRewardDeliveryHandlerTest`、`explore RewardDeliveryOutcome RewardDeliveryWorker JdbcRewardDeliveryRepository RewardDeliveryController`、`impact QuestCompletionRewardDeliveryHandler.deliver` 核对端口/handler/worker；改后 `sync` 更新 2 个文件，`affected` 返回 49 个宽候选。Base 原来把 `TARGET_NOT_FOUND` 永久失败；现按 BQ no-op 映射为已投递，避免实际定义中的缺失目标变成永久失败/恢复项。坏 UUID 和目标 scope unavailable 仍失败；测试拆分覆盖三态。
- Docker `galaxy-dev:local` 隔离输出强制执行完整 `:galaxy-quest-core:test`：286 项、0 skipped/failure/error。`compileJava assemble verifyUi2SelfContained` 成功：36 actionable tasks，24 executed、12 up-to-date；先在独立忽略目录编译 7 个子模块 jar（15 tasks）。首次将全部 buildDir 临时指向一次性容器 `/tmp` 时，旧 Forge Gradle dependency transform 在任务图解析时找不到尚未生成的子模块 jar；未碰历史 root-owned build。改用 `.gradle/codex-bq-r224-build` 持久隔离目录、分两次构建后根门禁通过。测试后 XML 汇总第一次因容器没有 `python3` 返回 127（Gradle 测试已成功）；随后用 awk 强制复跑并读取 XML，以上计数为最终结果。临时 init 脚本已删除；隔离 build 输出位于忽略目录。
- 隔离构建的 runtime JAR 为 `.gradle/codex-bq-r224-build/_/libs/jsirgalaxybase-6e80f68-main+6e80f68c22-dirty.jar`，SHA-256 `0c8ff31e6698c633c584897bac2ad28fcdbbef8cff0a099a91c16eff3cc94656`；只作构建验收，未部署。
- 已同步开发导航、唯一能力矩阵及 M.2 行为矩阵；`git diff --check` 通过。没有 PostgreSQL 测试/查询、生产玩家进度访问、DDL、部署、Quest/BQ 开关或 S1/S2 操作。此项只完成 Core 自动语义修复，真实奖励运行/玩家存档窗口与整体 M.2 仍 BLOCKED。

### 2026-10-11 - BQ GLOBAL / globalShare 运行语义状态纠正

- 固定 BQ `QuestInstance.isComplete(UUID)` 在 `isGlobal=true` 时以任意完成记录作为全局完成；`hasClaimed(UUID)` 仅在 `isGlobal && !globalShare` 时采用全局领取状态。`globalShare=true` 本身不是 Party 进度同步开关。
- CodeGraph `explore/impact QuestBehavior.isGlobal` 核对 Base 当前调用链：定义导入/JSON 持久化、管理快照以及 Crafting 统计资格检查；未进入 `QuestEngine` 完成归约或奖励领取路径。因此字段可保存不等于 BQ 全局行为已实现。只读 S2 画像为 3,744 个定义中 `isGlobal=true` 0 个、`globalShare=true` 2,494 个且均 `isGlobal=false`，所以这是通用 BQ 导入能力缺口，不是当前快照中已观察到的内容行为差异。
- 据此修正能力矩阵的过度表述，并在 M.2 行为文档标明该范围。未扩展数据库模型或运行语义，也未修改代码/测试；尚未决定是否把通用 GLOBAL 作为本轮实际内容兼容门槛。`git diff --check` 通过。没有访问玩家进度或数据库、执行 DDL/部署、启用 Quest/停用 BQ 或操作 S1/S2。

### 2026-10-11 - Crafting 自动复评单目标失败重试回归

- 按既定周期检测边界补充根测试 `automaticBqDetectionRetriesFailedImportedTaskOnTheNextPeriodicPass`：首轮让一个带 `bq.raw` 的导入 Crafting Task 抛出瞬时数据库异常，同时验证下一个导入目标不被阻塞；第二轮再次调度后失败目标被重试并成功。项目自有（无 `bq.raw`）Task 在两轮均不进入自动轮询。此回归不改变服务端行为或任务合同，也不模拟真实 Forge Tick/数据库。
- CodeGraph `status` 为 up-to-date（工具提示索引由早期版本生成）；`impact QuestGameplayEventHandler.detectAutomaticCraftingStatistics` 覆盖事件处理器 `onPlayerTick` 与此根测试，`affected` 收敛到 `QuestCraftingStatisticsDispatchTest.java`。测试通过 `galaxy-dev:local`、`--network none`、不加载 Compose `.env` 的一次性 Docker 容器运行；Gradle build 输出重定向到忽略的 `.gradle/codex-r2-periodic-retry-build`，项目缓存放容器 `/tmp`。XML 计数：4 tests、0 skipped、0 failures、0 errors；`BUILD SUCCESSFUL`，32 actionable tasks/32 executed。没有连接数据库。
- `git diff --check` 通过。未访问生产玩家进度/数据库、执行生产 DDL、部署、启用 Quest、停用 BQ 或操作 S1/S2。该自动回归不证明真实 60-tick Forge 调度和 PostgreSQL 故障恢复，也不解除 R.2.6 的运行时/M.2 BLOCKED 状态。

### 2026-10-11 - TaskXP 实际使用范围排除复核

- 为选择下一项 M.2 实际兼容工作，固定参考 `Reference/BetterQuesting/src/main/java/bq_standard/tasks/TaskXP.java` 的 `detect/tickTask`，并对 S2 授权目录 `DefaultQuests/Quests` 的 typed NBT key（`tasks:9` / `taskID:8`）匿名聚合。当前 5,620 个 Task occurrence 完整分布为 Retrieval 3,863、Optional Retrieval 1,267、Checkbox 203、Crafting 133、Fluid 15、Hunt 104、Location 33、Block Break 1、Meeting 1；XP、Scoreboard、Item/Entity Interaction 均为 0。此前通用矩阵中的 XP 条目不是当前定义内容，故不应为本轮 M.2 优先实现/签收 consuming XP。只读取 `DefaultQuests/Quests`，未读取 Players、其他配置或数据库。
- CodeGraph `status` up-to-date；`explore TaskXP/XpSnapshotTaskEvaluator/QuestObservationPlan`、`impact XpSnapshotTaskEvaluator.evaluate` 及 `affected` 确认现有通用 evaluator 与 PlayerTick 观察链，但此类为当前内容零 occurrence；未改代码、未运行测试。更新 M.2 实际类型矩阵与下一门禁：用户已确认 Quest 级消耗 Detect、Crafting 自动复评及来源服 max/sum，不再列为待决选择；R.2.19 仅 Party 延迟事实归属仍需用户决策。下一代码包按实际频率优先审计 Retrieval/Optional Retrieval。
- `git diff --check` 通过。没有生产数据库/玩家进度访问、DDL、部署、Quest/BQ 开关或 S1/S2 服务操作；R.2.19 与 M.2 真实 Forge/GTNH 运行状态保持 BLOCKED。

### 2026-10-11 - Quest 级消耗 Detect 隔离回归复验

- 用户再次确认：消耗型任务采用单一 Quest 级 Detect，按定义顺序逐项持久化/恢复；不同 Party 成员 Crafting 计数不合并，同一玩家跨来源服按各自 `max(事件累计, 绝对统计快照)` 后求和。该决定与开发导航和能力矩阵一致；未改变业务实现。
- CodeGraph `status` 为 up-to-date（索引版本提示较旧）；`explore` 核对 `AuthenticatedQuestConsumptionDetectService.detect/continueBatch`、`ConsumptionRequestAuthorizer.planQuestDetect`、`ConsumptionSubmissionService.submit/process`、`QuestRuntimeService.execute/reconcileAfterConsumptionDetectBatch` 与 JDBC 批次游标；`impact` 覆盖 Detect 服务及批次末结算，`affected` 返回 49 个宽候选。直接回归集中在 Core Detect/授权/Runtime 测试与 PostgreSQL 批次仓储集成测试。
- 根 Gradle 构建在 GTNH convention 插件配置时因离线容器无法解析 `raw.githubusercontent.com` 而未进入测试；这不是测试失败，也没有尝试连接生产环境。改用一次性 `galaxy-dev:local`、`--network none`、独立 Java Core 构建目录，强制执行 `galaxy-quest-core` 全套：62 suites / 286 tests，0 skipped/failure/error。该路径不包含 Forge 根构建。
- 批次游标用例在临时 PostgreSQL 16 上执行 1 项，0 skipped/failure/error。数据库仅位于无外部路由的 `--internal` 网络，512 MiB tmpfs、无宿主卷/发布端口；Gradle 隔离运行仅为该不使用 Mockito 的测试排除了缓存缺失的 `objenesis` 运行时传递依赖，项目依赖未改。临时网络与数据库容器已移除。该测试验证游标幂等/恢复与结算围栏，不代表生产 DDL 或真实库存副作用。
- 现有 Core 回归已验证 Quest OR 条件不会在批次中途提前结算奖励、任务按顺序执行、失败后沿稳定幂等键恢复；定义在批次执行期间被重新发布的并发策略仍待用户选择，故本轮不改发布或旧批次合同。`git diff --check` 收口时复核。
- 本轮没有读取/连接生产数据库或玩家进度、执行生产 DDL、部署、启用 Quest、停用 BQ 或操作 S1/S2。真实 Forge/GTNH 时序与消费恢复仍未验收，M.2 继续 BLOCKED。

### 2026-10-11 - R.2.17 多任务 Detect 有序 journal PostgreSQL 回归

- 用户再次确认现有合同：消耗型任务由一次 Quest 级 Detect 按定义顺序逐项持久化/恢复；不同 Party 成员 Crafting 进度独立；同一玩家同一来源取 `max(事件累计, 绝对统计快照)`，再汇总各来源。开发导航与能力矩阵已记录该语义，本次不改生产实现。
- 扩展 `QuestPostgresIntegrationTest.consumingDetectBatchCursorIsDurableIdempotentAndFencesQuestSettlement`：以三项有序 task key 验证 PostgreSQL journal 重读顺序不变、重复入口不能覆盖原 Task 列表、cursor 按索引推进且重复推进幂等，最后完成并释放活动批次。与 Core 的 `questDetectProcessesEveryConsumingTaskInOrderAndSettlesOnlyAfterTheBatch` 合看，补足“Core 按定义顺序处理 + JDBC 持久化顺序/恢复”的自动证据；仍不覆盖真实玩家背包/Forge。
- CodeGraph 起始 `status` 为 up-to-date（索引由早期 engine 版本生成）；`explore` 核对 Quest Detect 服务、授权器、JDBC repository 与既有多任务 Core 测试。修改后 `sync` 更新 1 个测试文件/147 nodes；`impact JdbcConsumptionDetectBatchRepository.advance` 覆盖 repository 接口、服务端 continueBatch 与该集成测试；`affected` 选择 `QuestPostgresIntegrationTest`。
- 最终通过一次性 `galaxy-dev:local` 容器在内部 Docker 网络中的 PostgreSQL 16 上执行此隔离用例：1 test、0 skipped/failure/error。初次尝试分别因独立 Gradle Home 缺少 Gson 缓存、历史 `build/test-results` 权限以及子项目继承单测试过滤器而未进入目标测试；未改旧产物权限。最终通过挂载已有离线缓存、忽略目录隔离 build output、排除无关 `:galaxy-quest-core:test` 后成功。临时 settings/init 文件已移除；数据库使用 512 MiB tmpfs、无宿主卷/发布端口，网络与容器已删除。
- 已更新唯一能力矩阵和 M.2 行为矩阵；最终 `git diff --check` 通过。临时设置文件/init script 已删除，临时 PostgreSQL 容器/内部网络已移除。本轮未访问生产数据库/玩家进度、执行生产 DDL、部署、启用 Quest/停用 BQ 或操作 S1/S2。活动批次遇到新定义发布时的旧版恢复/结算合同仍待用户决定；M.2 继续 BLOCKED。

### 2026-10-11 - Retrieval/Detect 合同复核与 S2 磁盘配置核实

- 按用户本轮再次确认的合同更新评估基线：消耗任务使用单一 Quest 级 Detect，按定义顺序逐项持久化并恢复；Crafting 统计对同一玩家按来源服分别取 `max(事件累计, 绝对统计快照)` 后求和，Party 成员各自保留 PLAYER 进度。
- CodeGraph `status` 显示索引 up-to-date；`explore` 核对 `InventorySnapshotTaskEvaluator`、`QuestObservationPlan`、`BqPartyInventorySnapshotFactSink`、`QuestRuntimeService` 及 Detect 批次链；`impact InventorySnapshotTaskEvaluator.evaluate` 为 14 个符号，`impact AuthenticatedQuestConsumptionDetectService.detect` 覆盖 Core/根集成/终端调用方，`affected` 返回 71 个宽候选。与固定 BQ `TaskRetrieval`、`TaskOptionalRetrieval` 源码逐项对照：非消耗 ACTIVE_PLAYERS 库存、`groupDetect`、需求顺序与 matcher、optional 忽略语义都已有 Base 逻辑和既有回归；Minecraft 快照枚举玩家 InventoryPlayer 全部槽位，规范化保持首次出现顺序并仅合并完全相同的物品形状。没有发现可由安全代码修改闭合的新 Retrieval 求值差异，因此不为增加代码而改写稳定实现。
- 只读检查 S2 `config/betterquesting/questbook.cfg` 得到磁盘 `fullySyncQuests=false`；实际 GTNH 3.7.15-GTNH 源码读取该配置，但注释标记 `[NYI]`，该文件值不证明运行进程已加载。固定 `TaskRetrieval`/`TaskFluid` 在 false 分支使用发起者库存和 PLAYER 进度；Crafting 不读此开关。对应修正开发导航、能力矩阵和 M.2 行为说明中“配置文件未读取”的过时状态，并保留运行时加载/覆盖未验证的边界。
- BQ Retrieval 的 Observation Station 会补充 owner 在线时的六邻方块容器库存观察；其 retrieval/fluid 路径不消费容器物品。按已确认的背包优先范围与既有计划，本轮仍延期该入口，DefaultQuests 定义也不能证明玩家是否实际使用。非消耗库存时序、多服真实在线 roster、ItemStack/NBT 与 Forge 实机仍未验收。
- 本轮仅更新文档，无生产代码/测试改动，因此未运行测试。`git diff --check` 作为文档收口门槛；没有读取玩家进度、连接 PostgreSQL、执行 DDL/部署、启用 Quest/停用 BQ 或操作 S1/S2。R.2.19 Party 延迟事实归属与活动 Detect 批次遇新定义发布后的恢复政策仍待用户选择；M.2 继续 BLOCKED。

### 2026-10-11 - Crafting PARTY 在线资格与全 roster 进度语义核对

- CodeGraph `status` 为 up-to-date；`explore`/`callers`/`callees`/`impact` 复核 `QuestGameplayEventHandler.onItemCrafted/submitCraftingStatistics`、`QuestObservationPlan`、`MinecraftQuestFactFactory.craftingStatisticObserved`、`BqPartyTaskProgressScope`、`BqHuntPartyAssignmentPolicy.select`、`QuestEvaluationPlanner.requestsFor`、`BqPartyActiveQuestQuery.hasActiveMember`、`AuthenticatedCraftingStatisticsDetectionService.detectAutomatically` 与 Quest 级消耗 Detect 调用；随后以本地固定 BQ `ParticipantInfo`、`EventHandler`、`TaskCrafting` 源码核对上游时序。
- 发现必须分开的两件事：BQ 用本服在线 `ACTIVE_PLAYERS` 的 active Quest 并集决定该事件 Quest 是否参与；任务被选中后，`TaskCrafting.onItemInternal` 将匹配产物事件进度投影到完整 `ALL_UUIDS`，包括离线 Party 成员，而绝对 `objectCraftStats` 只读取在线成员各自的统计。Base 成功处理时对应为 `BqPartyActiveQuestQuery` 在线活动资格门控、`BqHuntPartyAssignmentPolicy` 对当前完整 Party roster 的 PLAYER 进度 fan-out；Crafting 统计事实则仍按各在线玩家个人采集。自动统计不跨成员相加；该路径的绝对值继续按用户确认的来源服 max/sum。
- 因此目前发现的不是“离线成员未获 Crafting 事件增量”，也不是“Party 统计被聚合到一个人”；自动实现已经分离在线资格与全 roster 事件 fan-out。仍未解决的是事实因数据库故障排队后，Base 在重试时重新解析 Party 和活动 Quest，而固定 BQ 在事件回调中同步读取当时资格/roster。严格事件时快照还受当前无新鲜权威缓存、内存队列崩溃丢失等边界影响，需等用户对 R.2.19 选择后再实施。
- 更新 M.2 行为矩阵、权威能力矩阵与开发导航，避免把在线资格误写成在线进度接收范围。只做源码/调用链审计和文档更正，无运行代码/测试改动；本批 CodeGraph 影响查询范围覆盖上述事件、统计与 Planner 链。`git diff --check` 已通过。没有访问生产玩家进度/数据库、执行生产 DDL、部署、启用 Quest、停用 BQ 或操作 S1/S2。R.2.19 与定义发布并发选择仍待用户确认，M.2 继续 BLOCKED。

### 2026-10-11 - Quest Detect 活动批次与发布版本并发边界补查

- CodeGraph `status` 为 up-to-date；`node`/`impact` 追踪 `AuthenticatedQuestConsumptionDetectService.detect/continueBatch`、`ConsumptionRequestAuthorizer.planQuestDetect`、`JdbcConsumptionDetectBatchRepository.findActive/createIfAbsent` 与 `QuestDraftManagementService.publish`。影响面集中在 Core Detect、Quest 管理终端与已有 Core/根 PARTY-JDBC 回归。
- 具体恢复缺口：入口按当前 `publishedVersion` 查询批次；旧批次在 `continueBatch` 中仍调用当前定义授权，并在结算时硬性校验定义版本相同。PostgreSQL 活动唯一索引目前按 `(participant_type, participant_id, quest_id, definition_version)` 限定，因此新版本发布后不仅旧批次不可续跑，当前版本也能创建另一批。若选择“允许发布并续跑旧批次”，必须同时固定旧定义的提交/结算语义并阻止同一主体同一 Quest 跨版本并行消耗；只调整 `findActive` 或仅续跑旧 batch 都不足以保证库存安全。若选择阻止发布，则现有版本授权与源服恢复模型可继续使用，但服务器或玩家长期不可用会使发布受阻。
- R.2.19 归属审计进一步确认 `GameplayFact` 目前只固化来源服、事件键、玩家、类型、发生时间和属性；`QuestEvaluationPlanner` 在队列重试时重新读取发布 Quest、Party 成员、在线资格、完成前置和个人进度。因此要冻结事件时语义，不只是把 UUID 列表写进事实：还需快照哪些 Quest/版本当时符合 BQ active cache，并冻结对应 Party roster/接收 PLAYER 集合。若这些权威数据无法及时取得，必须选择 fail-closed（不重分配）或保留当前 retry-time 语义；本次已向用户明确询问，不预先假设。
- 已有自动测试覆盖批次 Task 顺序、游标幂等、源服门控和最终结算 fence；未发现覆盖“批次部分应用 → 发布新定义 → 重启后 Detect”的测试。用户最新确认的 Quest 级消耗 Detect、来源内 max/来源间求和均已与权威文档相符；这不是对上述版本并发选项的回答。
- 本次为静态审计与 Worklog 补记，无生产代码/测试改动，因此未运行测试；不访问生产数据库/玩家进度，不执行 DDL、部署或 Quest/BQ 开关变更。版本并发与 R.2.19 延迟事实归属仍待用户确认；M.2 保持 BLOCKED。

### 2026-10-11 - Checkbox 配置语义更正与 QuestCompletion 奖励链复核

- 只读复核固定 BQ `QBConfig.init` 与 `fullySyncQuests` 调用点：默认值 false 与 `[NYI]` 注释不是“配置未接线”的证据；该值仍从 Forge Configuration 读取，并被 `NetTaskCheckbox`、`QuestInstance`、`TaskRetrieval`、`TaskFluid` 直接分支使用。此前只读得到的 S2 磁盘 `questbook.cfg` 值为 false；它不证明服务器进程已加载或未被运行时覆写。已修正开发导航、能力矩阵及 M.2 行为说明的陈旧措辞。没有把 Checkbox PARTY/TEAM/PUBLIC 兼容性提升为已完成。
- 继续检查 DefaultQuests 实际使用的 QuestCompletion 奖励：BQ `RewardQuestCompletion.claimReward0` 对缺目标、无 QuestCache 或无目标 UUID 是 no-op；目标已经对当前玩家完成时不重复完成；否则以该玩家身份设置目标完成并标记客户端 QuestCache dirty。Base 生产链为 entitlement handler → `JdbcQuestCompletionRewardPort`，在事务中解析目标已发布版本、解析目标 scope/参与成员、实施“曾完成”闩锁、创建目标 Task 完成态并插入目标奖励权益。CodeGraph `impact JdbcQuestCompletionRewardPort.complete` 命中接口、handler 和 4 个 PostgreSQL 用例（目标原子完成、仅接收玩家、重复周期不重做、目标 scope/成员冻结）；`affected` 返回 49 个宽测试候选。现有测试覆盖合成 definitions 的数据库语义，尚非真实 BQ `QuestCache`/Forge 或 DefaultQuests 全量影子验证；未发现可据此安全改写的当前行为差异。
- 本轮环境验证：Gradle `:galaxy-quest-core:test --rerun-tasks` 在测试启动前因构建要求 JDK 25、Foojay toolchain 下载不可用而终止；这不是测试失败。随后用 JDK 17 对 Core 主/测试源码执行 `javac --release 8`，使用缓存的 JUnit 4.13.2/hamcrest 运行 62 个测试类、290 项，结果 `OK (290 tests)`，0 failure/error。该直接 JUnit 路径不是根 Forge/Gradle、PostgreSQL、assemble 或自包含验收。
- 本轮只作源码/CodeGraph 审计及文档更正，不修改生产 Java、数据库合同或配置；继续不读取玩家进度/生产数据库，不执行 DDL、部署、启用 Quest、停用 BQ 或操作 S1/S2。R.2.19 事实重试归属与活动 Detect 遇新定义发布的恢复策略仍待用户选择，M.2 继续 BLOCKED。
