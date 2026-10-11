# BQ 替代开发导航与变更边界

更新：2026-10-11。用途：下一轮开发先读本页，不从历史 Worklog 的“下一步”自行推断目标。
当前状态以能力矩阵顶部唯一阶段表为准，当前工作入口为
[Base-only 新服实施路线](betterquesting-next-stages-2026-10-11.md)，当前执行 N.1–N.5；
[M.3 离线预览工作包](betterquesting-m3-offline-preview-2026-10-11.md)仅保留为历史工具证据，不再迁移旧玩家进度；
[M.2 自动收口目标](betterquesting-m2-automatic-closeout-2026-10-11.md)保留为已完成的有限自动基线。
R.2.19 事件归属与 Detect 发布并发均已由 R.2.34 解决，不再列为待决。

**2026-10-11 当前产品合同（优先于下方历史记录）**：目标为无 BetterQuesting 的全新服务器，所有玩家从零开始；只导入 `DefaultQuests` 内容，不迁移旧进度、领取/Choice 历史或旧 Party。所有导入 Quest 和事实均为 PLAYER 个人语义，即使玩家加入 Base Party 也不聚合或扇出。旧 M.3 进度迁移与 M.5 实时影子退出活动路线；下一生产门槛是 Base-only 新服人工验收。

当前交付证据见 [Base-only 验收表](betterquesting-base-only-acceptance-2026-10-11.md)。代码检查点为 `adfb691`（个人导入与门禁）和 `664436e`（终端健康摘要）；本轮未推送、未部署。

<details>
<summary>历史 M.2/M.3 开发记录（非活动待办）</summary>

M.3 离线预览：已接入定义 hash/version、多来源向量/完成/领取/时间关系和源文件 SHA-256。
合成文件运行实际 CLI；报告禁止自动应用，repeat/Choice 明确阻断。
Importer 隔离回归 65 项全部执行、零跳过/失败/错误。此为离线工作包，不包含写入器、
回滚或生产迁移；下一步不得把结构上的 AHEAD 当作授权选源策略。

R.2.35：已复现重复 Checkbox 提前点击占用下一周期事实键的问题；入口现于
Runtime acceptance 事务内检查待领奖、待消费恢复、领取后的 repeat anchor 及周期身份，
拒绝时不写事实。三个故障回归先失败、修复后通过；Core 295、隔离 PG 模块 61、
Importer 52 均零跳过/失败/错误。全工程 1,086 项（1,034 执行、52 条非 Quest
环境 assumption-skip）零失败/错误；assemble、自包含和 Golden 成功。
本次有限自动收口通过；M.2 实机仍 BLOCKED，M.3 写入器尚未实现。

<details>
<summary>近期历史验收记录（测试数量按批次区分）</summary>


2026-10-11 R.2.34：用户确认两项兼容合同并完成实现。Crafting 事实在回调时冻结完整 roster、当时 Quest 资格与定义请求；无法取得权威快照则 fail-closed。旧 Quest 级消耗 Detect 批次可在管理员发布新版后继续按固定旧定义/旧奖励恢复；同一参与者/Quest 禁止跨版本并行活动批次。增加快照重试、字节预算与版本并发回归；Core 292 项、隔离 Quest PostgreSQL 61 项、全工程 1,082 项（1,030 实际执行、52 条无关业务 PostgreSQL/Warehouse 配置 assumption-skip）均 0 failure/error；`assemble`、`verifyUi2SelfContained` 通过。详情见 Worklog。真实 Forge/GTNH 回调时序、在线运行和生产 DDL 仍未验证，M.2 继续 `BLOCKED`。

2026-10-11 R.2.33：将 PlayerTick 的 20/60/100 tick 周期门槛收敛到 Minecraft 无关的小型纯 Java helper；60 tick Crafting 统计复评仍是既有 Base 调度。初次纯离线构建在 GTNH convention 插件外部元数据处停止；确认本机 7892 代理监听后，通过一次性容器代理重跑根定向 `:test`，`compileJava`/`compileTestJava` 成功，两个测试类共 6 项通过、0 skipped/failure/error（33 actionable tasks 全执行）。随后 `:assemble verifyUi2SelfContained` 成功（29 actionable tasks，7 executed、22 up-to-date）。不证明真实 Forge tick 时序；未跑全量测试。此窄门禁不解除 M.2 `BLOCKED`。

2026-10-11 R.2.32：新增 Core 组合回归，确认本服至少一名在线且已解锁成员可激活 BQ PARTY Crafting 事件；一旦通过门控，任务事件仍扇出到完整 roster（含离线成员）的独立 PLAYER 进度。若唯一合格成员离线，则不处理该事件。定向测试类 9 项通过；该回归不解决排队重试时 PARTY/资格变化的 R.2.19，也不证明 Forge/GTNH 运行时。

2026-10-11 R.2.31 补记：固定 BQ `TaskCrafting.detect()` 按需求分量检查，只在该来源服的分量进度低于目标时读取 `objectCraftStats`。Base Crafting evaluator 已按来源分量应用同一门槛：本服事件已把一个分量推进到目标后，同事件统计快照不再把该分量抬高；其它未达标分量和其它来源服仍独立检测，统计在确实用于未达标分量时继续保留超目标绝对值。新增 Core 回归覆盖多分量及跨来源。Core 全套强制重跑 290 项，0 skipped/failure/error；无数据库、根 Forge 构建、assemble 或自包含门禁。本项只补窄算法证据，M.2 继续 `BLOCKED`。

2026-10-11 当前工作补记：R.2.28 修正 BQ 导入器把任意类型 ID 中的 `optional` 子串误当可选语义的问题。只有已知 `bq_standard:optional_retrieval` 明确标记 optional；未知任务默认保留为必需项，并继续报告 `UNKNOWN`，避免不支持的任务静默降低 Quest 完成门槛。Importer 全模块本轮运行 51 项，44 执行通过、7 项因缺少隔离 PostgreSQL URL assumption-skip，0 failure/error；未改数据库或运行时任务行为。R.2.19 Party 事实延迟重试归属仍待用户决定；M.2 整体仍 `BLOCKED`。

2026-10-11 R.2.29 自动回归：重复 Quest cycle 0 的 Item reward 部分写入玩家 NBT 后，压缩 NBT 重建并先完成 cycle 1，再延迟恢复 cycle 0；两周期 entitlement marker/部分进度互不污染。根定向奖励投递与 BQ 导入交付共 34 项通过（0 skip/failure/error）；只证明 Minecraft NBT 对象序列化恢复与 Base marker 算法，不证明真实 Forge 玩家/World 保存或跨服租约，也没有修复 world drop 已生成、marker 尚未最终保存时的崩溃窗口。M.2 继续 `BLOCKED`。

2026-10-11 当前审计补记（R.2.34 前的历史状态）：用户再次确认消耗任务使用单一 Quest 级 Detect，按定义顺序处理未完成消耗 Task；同一玩家的 Crafting 进度按来源服分别取 event/absolute max 后求和，不同 Party 成员仍写各自 PLAYER 进度。以上实现已有 Core/JDBC 回归，Core 当时全套 289 项通过。R.2.19 归属与定义版本并发当时仍待选择，随后已由 R.2.34 决策与实现收口，见本页最新记录。行为盘点 CLI 逐 Quest 身份输出已修复并有 importer 回归。

2026-10-11 R.2.30 自动回归：从合成 BQ JSON 导入一个 OR Quest，其中按定义顺序包含两个 consuming Retrieval；隔离 PostgreSQL 上通过 Quest Detect 消耗首项后，在第二项应用前注入故障。恢复后从持久游标继续、重用第二项稳定幂等键，两个 Task 全部结束前 Quest 不提前结算。根 PostgreSQL PARTY/库存集成类 16 项全通过、0 skip/failure/error；仅证明合成 ItemStack 适配器与批次/JDBC 恢复，不是 Forge 实机或物理存档崩溃验收。非消耗 Retrieval importer→PARTY snapshot→Runtime/JDBC 和 groupDetect JDBC 回归此前已存在，不再作为“尚缺代表性端到端用例”列出。M.2 仍 `BLOCKED`。

2026-10-11 隔离数据库补验：使用仅内部可达、无发布端口、512 MiB tmpfs 的临时 PostgreSQL 16 `_test` 数据库执行 `:galaxy-quest-bq-importer:test`，50 项全部执行、0 跳过/失败/错误；其中 Importer schema 集成用例完成创建/删除自身临时 schema，测试结束后 schema 数为 0。测试网络、容器与数据库随后销毁。该证据补上此前缺少 `QUEST_TEST_POSTGRES_URL` 时 7 项 assumption-skip 的验证空档；不涉及生产库/DDL，也不提升 M.2 状态。

</details>

## 权威顺序与固定目标

用户最新明确决定优先；其次按以下文件分工读取，避免多个计划互相覆盖：

1. [需求复盘](betterquesting-requirements-reassessment-2026-10-09.md)：目标与不可越过的边界。
2. [能力矩阵](betterquesting-capability-absorption-matrix-2026-09-13.md)：唯一阶段状态；本导航不另设完成百分比。
3. 本页：代码职责、下一批工作包、变更与验收规则。
4. [M.2 行为矩阵](betterquesting-m2-behavior-compatibility-2026-09-23.md)：类型/参数兼容证据。
5. [迁移路线](betterquesting-content-compatibility-migration-roadmap-2026-09-17.md)：M.0–M.6 的阶段定义。
6. [Worklog](WORKLOG.md)：历史执行证据，不是新的需求或当前待办。

固定目标：**Base 自包含 + PostgreSQL 跨服权威 + UI2 终端替代任务书 + 复用 BQ 成熟行为与现有 GTNH 内容**。先兼容，后新玩法。不能擅自改为依赖 BQ 的数据库补丁，也不能借“独立”之名重写所有成熟算法。

文档冲突时先核实代码与证据，再修正文档；不能选择最宽松的旧描述继续扩功能。架构目标改变必须先向用户说明变化、收益与迁移代价，记录决定后才实施。

## 已确认的 M.2 行为合同（2026-10-10）

- 2026-10-11 用户再次确认：非消耗库存任务由系统持续自动观察；仅消耗型任务通过单一 Quest 级 Detect 按定义顺序提交。Crafting 仍只接受玩家 craft/smelt/anvil 事件，GT 机器产物不能伪造成合成事件；同一玩家跨服统计按各来源的“事件累计/绝对统计快照”最大值再求和。
- 归并口径：不同 Party 成员的 Crafting 统计仍各自进入个人 PLAYER 进度，各服发布本服在线成员的个人快照。最新确认是同一玩家按来源服分别取 `max(本服 crafting 事件累计, 本服 crafting 绝对统计快照)`，再把来源服结果求和；不能跨来源简单求和快照，也不能在绝对统计上重复叠加事件。
- 数值口径：固定 BQ Crafting 产物事件会把单服事件进度封顶在目标；`TaskCrafting.detect()` 则会把更大的 `objectCraftStats` 绝对计数原样写回，不按目标数量截断。Base 按相同区分处理：每来源事件累计封顶、统计观察值不封顶，跨来源仍取各自最大值后求和，因此总进度可高于目标；完成判断仍使用 `value >= target`。R.2.20 已补窄范围 Core/JDBC 自动回归，不代表真实 Forge 统计读取已验收。
- 统计检测门槛：BQ `TaskCrafting.detect()` 逐需求分量检查 `current < target` 才读该物品的绝对统计。R.2.31 已让 Base 对每个来源服的分量使用同一门槛；事件已达标的分量不被同一轮统计再抬高，尚未达标分量仍可记录高于目标的绝对值，来源间 max/sum 不变。此为合成事实/Core 自动回归，不证明真实 `EntityPlayerMP.objectCraftStats` 时序。

- 当前实现已为 Crafting `TaskProgress` 保存 legacy baseline 与来源服 event/absolute 两组向量，单个来源贡献按 max、来源间求和；旧进度先保留为非叠加基线。JSONB 持久化与幂等重放已通过隔离 PostgreSQL 回归。新增迁移脚本尚未执行；真实多服统计来源和在线时序仍需运行环境验证。

- 2026-10-10 隔离 PostgreSQL 实测已补 migration 回归：从无 `source_progress_json` 的旧表开始，重复执行 `20261010_001_add_quest_task_source_progress.sql`，既有数组进度与完成标记不变，新 JSONB 列默认 `{}`、NOT NULL，且不增加/删除行。此为测试库升级证据，不是生产 DDL 批准或执行。

- 检测/提交分工：非消耗库存任务持续自动观察，不扣物品/流体；只有消耗型任务要求玩家提交。用户已选定 Quest 级 Detect：客户端只提交 Quest ID，服务端按定义顺序逐个处理本 Quest 未完成消耗任务，每个 Task 独立持久化并可恢复；不承诺跨玩家库存与数据库的全有或全无，也不得用客户端循环多个单 Task 请求冒充一个 Quest Detect。2026-10-11 当前工作树已实现服务端持久批次、稳定幂等键、游标恢复及批次末完成/奖励结算；当前授权仍限 PLAYER scope 的 consuming Retrieval、Optional Retrieval 与 Fluid 子集。新批次表迁移仅在隔离 PostgreSQL 测试中验证，未执行生产 DDL；M.2 仍 BLOCKED。
- **R.2.34 已确认并实现的延迟事实归属**：BQ Crafting 产物事实在事件回调时通过权威 Planner 冻结可评估 Quest、完整事件时 Party roster、活动资格、定义版本及个人进度请求；服务端事实队列只重试这份不可变请求，不在重试时重新解析 Party/资格。若数据库故障或资格快照无法可靠取得，事件直接 fail-closed，不进入待重试队列。队列保持 FIFO，容量最多 1,024 条、估算保留内存最多 8 MiB；每次重试最多 32 条、间隔 20 tick。正常停服会清空内存队列，进程崩溃也会丢失尚未被 PostgreSQL 接收的事实；没有声称跨崩溃持久化或 exactly-once。代码和自动测试证明的是同进程恢复合同，真实 Forge/GTNH 事件时序仍待运行环境验收。
- **R.2.34 已确认并实现的 Detect 版本固定**：管理员可在活动批次期间发布新定义；现有批次仍按其持久化 `definition_version`、原 Task 顺序/周期和历史 PUBLISHED 定义恢复、结算旧版本奖励。数据库只允许同一进度参与者与 Quest 存在一个活动批次，不按版本拆分唯一性，避免并行扣除；新版本不会重写旧版本进度。若管理员删除或重构旧任务而其活动批次仍完成，后续迁移/补偿可能需要管理员人工处理。迁移 `20261011_001` 已在隔离 PostgreSQL 测试中重复应用并覆盖版本冲突，不代表生产 DDL 已执行。
- BQ `SIMULTANEOUS` 转换边界：Quest 级 Detect 本轮仅在至少一个此前未完成的 Task 转为完成、而 Quest AND/OR 逻辑仍未满足时重置本周期 Task 进度；仅有历史已完成 Task 不应触发重置。延迟消耗批次必须把批次开始时选中的 Task key 带入最终结算，不能因该 Task 在前序事务中已保存完成而丢失本轮转换信号。当前 Core 回归覆盖了该条件及批次结算；只读 S2 汇总为 3,744 个 Quest 中 0 个启用 `simultaneous`，故仍需影子对照，不能把此项测试写成 S2 行为验收。
- 2026-10-11 只读 S2 `DefaultQuests/Quests` 消耗配置画像：共 308 个 consuming Task occurrence（307 Retrieval、1 Fluid），全部 `autoConsume=false`；没有任何定义显式设置 `isGlobal=true`（283 个显式 false、25 个省略，按 BQ 默认 false），284 个 `globalShare=true` 不能据此推导共享进度。随后只读检查 S2 磁盘配置 `config/betterquesting/questbook.cfg`，其 `fullySyncQuests=false`，但注释标记 `[NYI]`；此文件值不证明运行进程已加载该配置。现场 3.7.15-GTNH `TaskRetrieval`/`TaskFluid` 在 false 分支按发起玩家库存与 PLAYER 身份处理，`TaskCrafting` 不读取此开关。现有 PLAYER-only Quest Detect 与当前消耗定义及磁盘配置相符，但运行时配置加载和 Forge/GTNH 实机仍未证明；不因此扩大 Base PARTY 消耗授权。
- **版本并发风险状态：R.2.34 已关闭代码级故障路径，运行证据仍有限。** 活动批次查询/唯一性现按参与者与 Quest 固定，不含定义版本；恢复入口继续批次保存的版本，授权、消费事实和批次末结算均读取确切历史 PUBLISHED 版本。测试覆盖“v1 部分批次 → 发布 v2 → 重入沿 v1 完成”及 JDBC 迁移。真实 Forge 玩家物品/断线及生产版本发布并发尚未实机验证，不提升 M.2 状态。
- 源码时序：固定 BQ `QuestInstance.detect` 按定义顺序遍历所有未完成 Task；`TaskRetrieval`/`TaskFluid` 的检测更新 Task 自身进度与完成标记，Quest 汇总完成逻辑在整轮扫描后刷新。用户已选择 Quest 级 Detect，因此 Base 的兼容目标是批次末判定 Quest 完成/奖励，而不是中途达成 OR 条件后停止。现有 Base `QuestRuntimeService` 每条事实会即时完成 Quest、生成奖励权益并冻结周期，批次入口必须绕过这段逐事实结算并在结束时统一收束；不能通过客户端循环旧单 Task 请求实现。实际消费和 PostgreSQL 不具备跨系统全有或全无；系统故障需要保留批次身份及每 Task Saga 状态，恢复顺序保持定义顺序。
- BQ 非消耗 Retrieval/Fluid 会由库存变化调度 `onInventoryChange` 自动检测；Base 以周期快照实现相同的自动体验，不声称调度时序完全相同。非默认容器只有在明确接入对应安全适配器后才纳入观察范围，不扫描任意 GT 机器库存。
- Crafting 任务只由真实 craft/smelt/anvil 产物事件推进。GT 机器产物若不代表玩家合成事件，应使用物品/流体存在型任务并从玩家库存快照观察，不伪造合成事实。
- 对启用 `allowCraftedFromStatistics` 的导入 BQ Crafting Task，匹配产物事实立即刷新统计；按用户明确选择，Base 还每 60 tick 对在线玩家的个人统计做任务定向自动复评。后者是 Base 有意增加的体验行为，不是 BQ 的 60 tick 语义：固定 BQ 的被动 `QuestInstance.update()` 只检查 Quest/同时任务状态；`TaskCrafting.detect()` 实际由 Quest 级手动 Detect 或该 Task 收到匹配产物事件时调用。不同 Party 成员的计数不合并；同一玩家的多来源统计已按上文逐来源最大值求和。资格、前置和周期仍由服务端 Planner/Runtime 判定。
- **R.2.19 前置审计（历史，已由 R.2.34 覆盖）**：固定 BQ `EventHandler` 只从本服在线 `ACTIVE_PLAYERS` 的 active Quest 并集选择要处理的 Quest；`TaskCrafting.onItemInternal` 随后把匹配产物事件进度写入完整 `ALL_UUIDS`，包括离线 Party 成员，而 `TaskCrafting.detect` 只读取在线成员各自的绝对统计。改动前 Base 会在重试时重新解析当前 Party/资格，导致排队期间归属可变。R.2.34 已改为回调时捕获完整评估请求并让队列保持事件时归属；上面的失败与进程边界是当前合同。
  - 2026-10-11 CodeGraph/源码复核细化了实现边界：`GameplayFact` 属性虽可携带字符串，但 `QuestEvaluationPlanner.forcedMemberships` 目前只信任 PARTY 库存快照与授权消费 scope；普通 Crafting 事实仍从 `JdbcParticipantMembershipResolver` 解析当前 membership。`BqPartyActiveQuestQuery` 还会查询在线成员、前置完成、当前周期、待提交/领奖状态；单独冻结 PARTY UUID 或成员列表并不能冻结 BQ 的事件时 Quest 资格。
  - R.2.34 的实现不使用过期缓存：快照在事件回调同步读取权威 Planner；读取异常时拒绝事实。不可变快照仅在当前进程的有界 FIFO 队列中恢复，正常关服清空、进程崩溃丢失未持久事实，详见上方边界。
-  - Crafting 统计的周期复评仍逐名在线玩家运行，来源服 max/sum 与 Party 成员个人进度合同不变。该路径不用于修补 fail-closed 丢弃的产物事实；两者是不同事实种类。
- 2026-10-11 安全前置过滤（历史窄优化）：Crafting PARTY 事件只有在 BQ Crafting Task 的通道开启、且产物按同一个 item/meta/damageable/OreDictionary/NBT 匹配器命中至少一项需求时，才触发 PARTY 活动资格查询与成员 fan-out。R.2.34 的事件时快照沿用该过滤，不改变无关产物行为。
- 2026-10-11 Retrieval 源码复核：固定 BQ `TaskRetrieval.detect/Detector/onInventoryChange` 对非消耗任务读取 `ACTIVE_PLAYERS` 库存；`groupDetect=true` 每轮重算所有分量，false 保留已满足分量并重算未满足项；单栈按需求顺序分配，`partialMatch`、`ignoreNBT` 与 OreDictionary 按 BQ matcher 处理。`TaskOptionalRetrieval.ignored()` 恒为 true。Base 对应为 `InventorySnapshotTaskEvaluator`、fenced PARTY 库存 lease、服务端 Quest 级消耗批次；Minecraft 快照按 `InventoryPlayer.getSizeInventory()` 采集全部槽位，规范化保留首次槽位顺序并合并完全相同栈，现有 Core/JDBC 回归覆盖非消耗回落、groupDetect、重叠 NBT/槽位分配与 optional 不阻断完成。CodeGraph `impact InventorySnapshotTaskEvaluator.evaluate` 为 14 个符号，`affected` 给 80 个宽候选；没有发现与已记录 S2 参数画像冲突的新求值差异，本轮未改代码、未重跑这些测试。另核实 BQ Observation Station：服务端每 20 tick、owner 在线时扫描六邻容器，并通过 owner 的 `ParticipantInfo.ALL_UUIDS` 调用 `TaskRetrieval.retrieveItems`、`TaskFluid.retrieveItems/retrieveFluids`；这些路径对 consuming 或已完成任务直接返回，因此它只补充非消耗检测、不消费邻接内容。Base 没有该世界入口，但 DefaultQuests 不能证明玩家实际使用；按既有范围决定继续延期。S2 磁盘配置当前 `fullySyncQuests=false`，但不等同于已验证运行时加载；Retrieval/M.2 继续 `BLOCKED`。
- 2026-10-11 奖励堆叠源码复核：固定 BQ `BigItemStack.getCombinedStacks()` 按 `baseStack.getMaxStackSize()` 拆分。当前 Base 奖励构造与世界溢出也按物品 max stack 拆分；放入玩家库存时另外受 1.7.10 inventory slot 的 64 上限约束。已有回归覆盖 max stack=128 的 130 件溢出为 128/2，以及 8,192 件普通奖励在 36 格背包后的 64 件溢出分栈。此为源码/替身自动回归，不证明真实 Forge toss 或 WorldSave 崩溃恢复。
- Checkbox 是定义本身要求玩家确认的显式动作；这不意味着其它非消耗库存任务也需要手动检测。
- M.2 继续沿用 Quest 级参与主体与现有 BQ Party 语义；同一 Quest 内混合个人与共享 Task 归属留到职业系统设计阶段，不在当前批次重构模型。

本节是当前已确认合同；下方带早期日期的“待确认”记录为历史决策状态，若与本节冲突，以本节及权威能力矩阵为准。

现有 `QuestObservationPlan` 已只为非消耗物品/流体定义开启周期观察；`QuestGameplayEventHandler` 当前每 20 个玩家 tick 采集 `EntityPlayer.inventory`。Crafting 统计另按既定 Base 合同每 60 tick 复评。上述周期是 Base 的实现调度，不构成真实 Forge/GTNH tick 等价证明。

### 2026-10-10：当前工作树对 S2 DefaultQuests 的全量定义解析

使用只读挂载的 S2 `DefaultQuests` 执行当前工作树 `:galaxy-quest-bq-importer:behaviorInventory`：3,744 个 Quest 定义、5,620 个 Task occurrence、5,448 个 Reward occurrence、47 个章节与 3,847 个章节放置均被导入器读取；当前来源规划摘要为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。规划 `applicable=true`，没有 ERROR；`BqCompatibilityReport` 未发现未知 Task/Reward 类型，所有已知类型为 `ADAPTED`（仅表示成功解析）。同时仍报告 64 个 `UNPLACED_QUEST` 与 4 个 `SEEDED_XOR_CYCLE` 警告；行为清单继续将所有实际 Task/Reward 标为 `BLOCKED`，全量解析不能替代运行时兼容门禁。未读取玩家进度或连接数据库。

对两类结构警告的影响复核：`supportedUnplacedQuestIsAWarningOnly` 明确把未放置但可解析的 Quest 视为可迁移警告；Importer manifest 保留全部 3,744 个定义，数据库 dry-run 逐个为 manifest 中每个 Quest 生成迁移项，章节放置作为独立数据导入。新增合成 BQ→隔离 PostgreSQL→玩家任务中心回归确认：未放置 Quest 会发布，但不会加入源章节 placement；分页器另返回虚拟 `unplaced` / “Other quests” 汇总，默认章节页面仍只显示原有 placement。这个虚拟分组不是伪造坐标，也没有逐项判定 S2 的未放置内容是否为隐藏辅助节点、旧内容残留或遗漏，不应擅自自动补坐标。4 个循环按“每个强连通组成员均为 XOR 且各自存在组外前置”放行为警告。固定 BQ 提交 `524b365211b6b3a9672cab8ae45b4e07e726d49e` 的 `EnumLogic` 将 XOR 定义为恰有一个前置完成，`QuestInstance.isUnlockable` 在完成数不超过 1 时允许进入候选；这与互斥分支结构相符，但当前静态检查没有证明组外前置本身可达，也没有验证实际玩家路径。两者属于内容可见性/可达性复核项，不是解析失败，也不解除任何行为类型的 `BLOCKED`。

2026-10-10 对 R1-03 延迟重试的 PARTY 时点做了静态调用链审计：重试队列保留原事实与幂等键，但 `RuntimeQuestFactSink` 重试时会重新进入 Planner；CodeGraph 对 `BqPartyTaskProgressScope.supports` 的源码显示，共享事件路由覆盖 Crafting、Hunt、Location、Meeting。此类普通事件事实未保存发生时的 Party roster/在线资格，因此若队列积压期间有成员变更，共享任务资格可能与事件发生时不同。当前只确认这是未覆盖的时序风险，尚未修改事实/成员合同；若要严格复刻 BQ，需要定义事件捕获时的 Party 与在线成员快照如何随重试固定，以及旧 Party 在成员离队后的延迟处理，不可仅保存一个 Party UUID 就视为解决。

Quest 级消耗检测当前实现边界（2026-10-11）：终端只发 Quest ID；`AuthenticatedQuestConsumptionDetectService` 从发布定义生成待检测 Task 顺序，并复用单 Task `PREPARED → APPLIED → CONFIRMED/REJECTED` Saga。Quest 批次表保存源服、主体、定义版本、周期、Task 列表和游标；每个 Task 使用稳定 `batchId:index` 幂等键。进度事实逐项提交，Quest 完成/奖励结算延后到批次末，并与批次删除处于同一 PostgreSQL 事务。中断后原服上的玩家再次触发 Detect 可从游标继续；切到其它服时返回等待源服状态，不跨服务器访问玩家库存。库存副作用和 PostgreSQL 无法组成一个原子事务，不承诺全有或全无。运行入口当前仅覆盖 PLAYER scope 下已验证的 consuming Retrieval、Optional Retrieval、Fluid 子集；Checkbox 等定义型确认动作不并入 Detect。

### Crafting 源码补充：事件计数与多服绝对统计

固定 BQ `TaskCrafting.onItemInternal/detect` 源码：匹配 craft/smelt/anvil 事件先按产出数量增加本机任务向量，再由同一 Task 的 `detect` 读取本机每名在线玩家的 `objectCraftStats`，只在统计值更大时覆盖。用户已确认跨服归并合同，并已实现为每个来源分别取 `max(本服事件累计, 本服绝对统计快照)` 再合计来源；来源贡献在 `source_progress_json` 持久化，旧聚合值按 legacy baseline 非叠加保留。不能把多个绝对快照直接相加，也不能在同来源重复叠加事件计数与绝对值。相关 schema migration 仅在隔离 PostgreSQL 验证，生产 DDL 未执行。固定上游：[TaskCrafting.java](https://github.com/GTNewHorizons/BetterQuesting/blob/524b365211b6b3a9672cab8ae45b4e07e726d49e/src/main/java/bq_standard/tasks/TaskCrafting.java)。

## M.3 进度迁移源码审计（2026-10-10；尚未进入写入实现）

现有 `BqPlayerProgressImporter` 只读单玩家 UUID JSON，`BqProgressMapper` 只转换 Task 向量；运行调用链停在离线预览/冲突分析，没有 PostgreSQL 进度写入器、批次事务或回滚工具。固定 BQ `QuestInstance` 源码显示每玩家完成/领取状态与 Task 进度分别保存；重复 Quest 的 `resetUser(uuid, false)` 会清除 claimed、将 timestamp 置零并重置 Task 向量，却不留下单调递增的周期号。因此跨服重复 Quest 快照无法仅凭“更大向量”或“更新 timestamp”安全选出当前周期。

本轮修复了冲突分析的一个假等价：Task 向量及完成/领取标记相同但 BQ timestamp 不同，现在归类为 `DIVERGENT`，不是 `IDENTICAL`。这是保守诊断，不是自动合并合同。建议重复 Quest 在没有可靠周期身份时隔离非一致快照供审计，不按服务器优先级或最大向量自动拼接；但具体来源选择/相同重置快照的处理需要在实现迁移前确认。M.2 仍未整体通过，当前没有开始进度写入或读取生产玩家文件。

## 现有代码导航

| 层/入口 | 当前职责 | 禁止越界 |
|---|---|---|
| `galaxy-quest-core`：QuestRuntimeService、QuestEvaluationPlanner | 不可变事实、进度归约、前置/周期、完成与资格 | 不引入 Minecraft、BQ 全局单例或 UI 对象 |
| Core：ConsumptionSubmissionService | 消费计划、提交和恢复流程 | 不用客户端库存/数量结论代替服务端校验 |
| Core：AuthenticatedQuestClaimService | 身份、领取周期及资格判断 | 不接受客户端指定奖励所有者/成功状态 |
| `galaxy-quest-postgres`：JdbcQuestTransaction、JdbcQuestRuntimeRepository、JdbcQuestDefinitionRepository | 事务、幂等事实、进度与版本定义 | 不改成每服整库回写；不把缓存当真源 |
| `src/main/java/com/jsirgalaxybase/modules/quest/QuestModule.java` | Minecraft 组合根与生命周期接线 | 不继续塞入算法或 UI 绘制；新增职责放在现有层 |
| Minecraft：QuestGameplayEventHandler、MinecraftQuestFactFactory | 游戏事件与背包快照转换 | 不在后台读取在线玩家/可变背包；不让 DB 异常逃出 tick |
| Minecraft：MinecraftItemRewardDeliveryHandler | 真实物品副作用与恢复标记 | 不把掉落实体与保存回执误报为原子操作 |
| `ui2-terminal`：QuestCenterVisualDocument / QuestCenterVisualModel | 平台无关任务中心、布局与动作意图 | 不接数据库，不新建 BQ GUI，不复制正式页面实现 |
| Root：TerminalQuestCenterSnapshotMapper | 服务端查询结果到终端模型 | 不在 Mapper 执行检测或奖励副作用 |
| `galaxy-quest-bq-importer`：BqMigrationPlanner / BqMigrationDatabaseService | 定义解析、迁移计划、显式审计应用 | 只读 dry-run 不写库；工具不进入运行 JAR |
| `Reference/BetterQuesting` | 固定版本语义与算法证据 | 不修改参考库，不直接调用其全局运行状态 |

职责整理不等于立即移动/删除文件。先按上述归属改小范围代码；拆分必须有调用链依据、行为测试和删除旧路径证据，不做全仓格式化。

## R.1 背包事实可靠性：当前状态

仍属于 M.2，不新建另一套阶段体系。R.1 是可靠性子批次，不等于 M.2 完成。R1-01～R1-05 的自动验收已收口；
新增数据库 revision 表和触发器必须先经正式发布流程评审，生产 DDL 尚未执行。

| ID | 工作 | 自动完成判据 | 状态 |
|---|---|---|---|
| R1-01 | 显式隔离测试库 | 未配置隔离库时不能回退生产地址；正向隔离库有实际集成结果；跳过不计为通过 | 自动验收完成 |
| R1-02 | 周期复评失败边界 | 注入连接/事务失败，tick 调用不抛出；下次既有周期可重试；失败写诊断 | 自动验收完成 |
| R1-03 | 一次性事实重试 | 原事实键和会话投递操作不变；失败后 FIFO 重试；容量、顺序、关服和进程崩溃边界明确 | 自动验收完成（仅内存重试，不耐进程崩溃） |
| R1-04 | 背包候选规划与缓存 | 发布定义按权威 revision 刷新；候选集按事实类型/目标任务/前置索引；成员、完成前置和周期状态保持逐事实动态查询 | 自动验收完成 |
| R1-05 | 可靠性收口 | R1-01～R1-04 的定向故障/缓存回归、隔离 PostgreSQL、全量构建、自包含与 diff 检查有有效证据；不扩大为 M.2 行为兼容 | 自动验收完成（仅 R.1） |

### R1-01～R1-03 实施边界（2026-10-09）

- Postgres、Importer 和根项目 PostgreSQL 集成测试不再回退到 `galaxy-base` 或 `POSTGRES_DB` 默认地址。只有显式提供 `QUEST_TEST_POSTGRES_URL`、`QUEST_TEST_POSTGRES_ISOLATED=true`、专用 `_test`/`_it` 数据库名以及账号时才连接；未配置 URL 时按条件跳过，已配置但标记/凭据/连接错误时失败。已在无数据卷的临时 `quest_runtime_test` 实际运行集成测试。
- 锁定进度复评仍按现有玩家周期触发；数据库 `RuntimeException` 在事件处理边界捕获并记录，不逸出 tick。下次既有周期仍会重新尝试；没有新增后台重试线程或改变任务资格/奖励行为。
- 游戏事实队列仅驻留服务器主线程内存，最多 1,024 条且估算保留体积最多 8 MiB；估算包含 UTF-16 字符、属性项和对象引用余量，属于容量上界估算而非 JVM 精确堆统计。首次写入立即尝试；存在积压时新事实排队在后；每 20 个服务器 END tick 最多处理 32 条。事实对象原样保留，因此 `sourceServer:eventId` 幂等键不变。登录 `beginInventorySession` 操作类型也随事实保留并用同一操作重试。
- 满容量或单条事实超限时拒绝最新事实，记录累计拒绝数并写错误日志；永久故障会阻塞 FIFO 队首，不越序。关服时记录未持久化条数/拒绝数并清空内存，防止跨集成服重启复用旧会话；进程崩溃无法运行清理钩子，队列内容同样丢失。队列不是 WAL/outbox，不宣称恰好一次；数据库已提交但回执丢失时靠原幂等键安全重放，提交前崩溃或容量拒绝仍可能丢失事实。
- R1-04 实现：`JdbcQuestDefinitionRepository` 读取 PostgreSQL 单行 revision；基线 DDL 与新增迁移都用同事务触发器，在已发布目录变化时递增 revision。`RevisionAwarePublishedQuestCatalog` 仅在 revision 改变时解码完整目录，并拒绝因 revision 查询失败而用旧目录继续规划。`QuestEvaluationCandidateIndex` 按事实类型、目标 Quest、前置 Quest 和持久化锁定 Quest ID 建立不可变索引。成员、完成前置、活动 PARTY 资格与周期锚点仍按原路径逐事实读取/判定，不缓存动态权限或进度。
- 固定规模回归使用 2,501 个发布定义，证明一条背包事实只选出 1 个候选；revision 回归证明同版本只加载一次、发布目录版本改变后重载、读取故障不返回旧目录。另覆盖定向事件、前置变化以及 repeat 冷却后的再次检测。Quest PostgreSQL 集成覆盖 publish/retire revision 更新和迁移升级后保留既有发布定义。
- 自动证据：隔离 PostgreSQL 的模块测试 `:galaxy-quest-core:test` 250、`:galaxy-quest-postgres:test` 52、`:galaxy-quest-bq-importer:test` 46，全部执行且 0 failure/error/skipped；根项目 PARTY 快照 JDBC 4 项另在同一类隔离临时 PostgreSQL 环境中通过。最终全量 XML 共 991 项，935 项执行、56 项 assumption-skip、0 failure/error：48 项因其它模块未提供 PostgreSQL 集成配置，4 项 Quest PARTY JDBC 用例因本次全量运行未配置隔离库（已单独实跑），另 4 项 Warehouse JDBC 用例缺少其 `JGB_BANKING_IT_*` 专用配置。首次全量运行因开发容器没有 UI2 预览 CJK 字体而失败；只读挂载宿主字体并设置 `UI2_PREVIEW_FONT` 后重跑 `test assemble verifyUi2SelfContained` 成功，字体未进入仓库/JAR。最终 `git diff --check` 通过。

R.1 只证明运行可靠性和减少重复全量目录解码；单次事实仍需查询一个轻量 PostgreSQL revision 行，未宣称 TPS 倍数提升。迁移 `20261009_001_add_quest_definition_catalog_revision.sql` 尚未应用到生产，因此已发布 R.1 代码部署前必须通过数据库变更审批并完成目标环境迁移。M.2 行为兼容、进度迁移、实时影子和正式切换仍未完成。

性能必须用查询次数与固定规模测试证明；没有 TPS 测量不能宣称提升倍数。先消除热点，再决定是否引入后台工作队列，不能预先建设大通用异步框架。

## R.2.1：背包匹配与库存分配语义复核（自动验收完成）

2026-10-09 对照固定 BQ 源码 `Reference/BetterQuesting/src/main/java/bq_standard/tasks/TaskRetrieval.java`、`TaskOptionalRetrieval.java` 与 `betterquesting/api/utils/ItemComparison.java`，检查 Item/meta 通配、耐久物品、NBT/OreDictionary 比较方向、重复需求分配、槽位顺序、非消耗观察及手动提交边界。BQ 检测按背包槽位顺序扫描，再按任务定义顺序分配需求；Base 的库存规范化保留首次出现顺序，评估器按该顺序分配可用数量。现有回归已覆盖耐久匹配、NBT 列表/数组的复用差异、OreDictionary 比较方向、观察回落、重复需求和消费计划。

新增 `BqCompatibleTaskEvaluatorsTest.retrievalKeepsBqSlotOrderWhenOverlappingNbtRequirementsCompeteForStacks`，通过真实观察属性编解码器验证两个重叠 NBT 需求在两种物品槽位顺序下的分配结果，锁定 BQ 的先槽位、后需求顺序。此次没有发现需要改动生产求值器的差异，也没有改玩家可见的提交/消费/进度/奖励合同。BQ `SubmitStation` 与 Base 玩家背包热键栏提交入口仍是明确的产品表现差异，不能靠本轮匹配算法测试宣称等价。

CodeGraph `status` 确认索引同步；`explore`、`callers/callees` 与 `impact` 检查 `InventorySnapshotTaskEvaluator.evaluate`、`MinecraftPlayerConsumptionPort.planItems`；`affected` 选择 Core evaluator 回归及根消费/导入适配测试。Docker Gradle 报告：完整 `:galaxy-quest-core:test` 251 项、Importer 选定两个测试类 9 项、根导入与消费适配器 19 项，合计 279 项，均 0 skipped/failure/error。`:assemble :verifyUi2SelfContained` 成功，`git diff --check` 通过。首次新增测试因 fixture 未声明 `item-vector` 而失败，补齐测试输入后复跑通过；首次构建因开发容器未走本机代理在插件解析阶段超时，改用已监听的临时 host 代理后成功，未修改系统网络配置。

本轮没有连接测试/生产数据库、读取玩家进度、执行 DDL、部署、启用 Quest 或停用 BQ。R.2.1 只锁定离线算法语义，不证明 Forge/GTNH 实际库存生命周期、NBT 注册表、SubmitStation 交互或跨服 PARTY 时序，因此 M.2 与 BQ 替代仍为 `BLOCKED`。

## R.2.2：导入、库存事实、JDBC 进度与消费恢复闭环（自动验收完成）

2026-10-09 完成 CodeGraph 与现有测试覆盖审计，并只对一个可复现的跨层测试缺口补回归。新增根集成测试从合成 BQ Retrieval JSON 导入定义、发布到隔离 PostgreSQL，再经生产 `BqPartyInventorySnapshotFactSink`、`QuestRuntimeService`、评估计划器和 JDBC 仓储验证 PARTY 当前成员各自的库存进度及完成状态。第二条链验证 `consume=true` 导入定义经授权后使用生产 `MinecraftPlayerConsumptionPort.planItems` 匹配精确 meta/NBT 物品，持久化 `APPLIED`；故意阻断事实投影后恢复提交，证明恢复不会再次扣除物品，最终进度只完成一次，重复请求仍返回已确认结果。

手动消费集成场景用测试端口维护合成物品列表，而非真实在线 `EntityPlayerMP` 背包；真实 Minecraft adapter 的运行环境行为仍由其独立测试覆盖。测试使用唯一注册名和反射注册的合成 Item，不触及 GTNH 注册内容。数据库仅为绑定 `127.0.0.1:5433`、tmpfs、无宿主数据卷的临时 PostgreSQL。没有修改生产代码或玩家可见的提交、扣除、进度、队伍和奖励合同。

CodeGraph `impact` 检查 `BqPartyInventorySnapshotFactSink.accept`（9 个符号）、`QuestRuntimeService.apply`（25）、`ConsumptionSubmissionService.process`（12）、`MinecraftPlayerConsumptionPort.planItems`（6）；`affected` 指向根 PARTY/JDBC 集成测试文件。实际执行 Core 251、Postgres 52、Importer 46、根 Quest 相关测试 25，共 374 项，0 skipped/failure/error。`:assemble :verifyUi2SelfContained` 成功；最终 `git diff --check` 另行记录于 Worklog。

边界：这证明合成 BQ 定义到 PARTY/JDBC 观察进度，以及手动消费的 `APPLIED` 恢复链可以被自动回归覆盖；不证明真实 Forge/GTNH ItemStack 注册、在线玩家背包读写、SubmitStation 交互、跨服延迟/切服、玩家存档崩溃恢复或奖励物理投递时序。满背包奖励的崩溃恢复策略仍需单独决策，不能宣称 exactly-once。

## R.2.3：手动 Retrieval 提交语义（自动验收完成）

固定源码对照确认 BQ 有两种玩家触发方式：Quest Detect 按玩家背包槽位顺序扫描并消费匹配栈；SubmitStation 按输入槽一次处理一个最多 64 件的栈。旧的单 Task 路由仍作为兼容/底层 Saga 保留，不是玩家主界面的目标合同。2026-10-11 Quest 级入口已通过批次末延迟结算处理 OR/可选任务中途完成风险；实现仍不模拟 SubmitStation 方块、输入槽和 10 tick 节奏。

新增根隔离 PostgreSQL 集成回归 `importedHeldStackSplitsAcrossOverlappingRetrievalRequirementsAndProjectsActualAmounts`：从合成 BQ JSON 导入两个重叠 NBT Retrieval 需求，各需 4 件；首个选中栈有 7 件，提交向量为 `[4, 3]`，未选中栈不变；第二次只补第二项 1 件，数据库进度变为 `[4, 4]` 并完成，重复提交不再扣物品。现有测试已覆盖 64 件上限、变化后的选中栈指纹拒绝、全库存按需求顺序的部分规划和幂等恢复。本批仅增加集成测试，没有修改玩家可见合同或生产逻辑。

CodeGraph 检查 `QuestCenterVisualDocument.detail`、`TerminalQuestCenterDocument.QuestActionsBridge`、`TerminalService.buildAuthenticatedQuestCenterSnapshot`、`MinecraftConsumptionSelection.captureHeldStack`、`ConsumptionRequestAuthorizer.authorize`、`ConsumptionSubmissionService.process` 和 `MinecraftPlayerConsumptionPort.planItems` 的调用链与影响；`affected` 指向根 Party/JDBC 测试。实际执行根 Party/JDBC 类 7 项、Core `ConsumptionSubmissionServiceTest` 5 项、根消费适配器测试 19 项，共 31 项，0 skipped/failure/error。`:assemble :verifyUi2SelfContained` 成功；初次测试编译发现合成 NBT `copy()` 的静态返回类型需显式转换，修正后重跑全部通过。

剩余边界：这证明合成 JSON、NBT 与库存替身下的需求向量、服务端选栈、实际扣除量和 JDBC 进度一致；不等于 GTNH 真实 ItemStack/注册表、SubmitStation GUI/节奏、多人在线队伍同步或玩家崩溃恢复验收。BQ 的站台交互与 Base UI2 入口不同，当前已明确作为产品交互差异，不在本批改动。

## R.2.4：Fluid 手动提交跨层闭环（自动验收完成）

按固定 BQ `TaskFluid` / `TileSubmitStation` 源码及已获授权的匿名定义画像，对照唯一 consuming `bq_standard:fluid` 任务：`consume=true`、`autoConsume=false`、`groupDetect=false`、`ignoreNBT=false`、单需求且没有 `Tag`。BQ 手动库存检测按槽位扫描；站台只提交一个数量为 1 的输入栈；流体匹配按 Name 与 Tag 精确语义执行。没有改变自动消耗、玩家入口、归属或副作用顺序。

新增根隔离 PostgreSQL 集成回归 `importedManualFluidSubmissionPersistsPartialAmountsAndRecoversAfterPlayerApply`：从合成 BQ JSON 导入并发布流体任务，经 `ConsumptionRequestAuthorizer` 生成剩余额度，再调用生产 `MinecraftPlayerConsumptionPort.planFluidsDetailed`。第一次提交 400 mB 后模拟“玩家侧库存与幂等 marker 已保存、PostgreSQL 仍为 PREPARED”，`resume` 从 marker 恢复并仅记录 `[400]`；随后不可分割的 750 mB 容器面对剩余 600 mB 整份消耗，当时实现仅向进度写入 `[600]`、最终 JDBC 进度封顶为 1,000 mB。R.2.18 已修正此处与 BQ 的数值差异：当前行为将实际 750 mB 记入，进度成为 1,150 mB；重复请求仍不重复消费。测试适配器/玩家库存/marker 为合成替身，不声称真实 Forge `FluidContainerRegistry`、`EntityPlayerMP` 存档写盘或进程重启已验收。

既有容器规划器单测另覆盖可分割容器部分量、当前选中栈最多处理一个容器、Name 必须精确匹配、NBT 忽略开关关闭时“缺少 Tag”和“显式空 Tag”不等价，以及空容器无空位可退时返回拒绝且原库存不变。下方 R.2.18 已将不可分割容器的实测排出量完整计入 Fluid 进度；真实 GTNH 容器注册行为和满背包差异仍需之后的运行验收。

CodeGraph 核对 `appendFluids`、`ConsumptionRequestAuthorizer.authorize`、`MinecraftPlayerConsumptionPort.planFluidsDetailed`、`ForgeQuestFluidContainerAdapter` 和 `ConsumptionSubmissionService.process`；`impact` 分别覆盖流体规划与提交/恢复链，`affected` 返回较宽的 52 个候选后人工收敛到根 PostgreSQL 集成类及容器适配测试。测试 XML：Core 251、Postgres 52、Importer 46、根消费端口 19、根 PARTY/JDBC 集成类 8，共 376 项，0 skipped/failure/error。首次裸 `test --tests` 将过滤条件传播到没有匹配用例的子项目，未进入有效测试；改用显式 `:test` 后执行。首次集成回归因测试直接在 JDBC 事务外调用 authorizer 而失败；改为与生产相同的事务边界后通过。`:assemble :verifyUi2SelfContained` 成功。

测试仅使用新建 PostgreSQL 16 tmpfs 容器、无宿主数据卷、仅 `127.0.0.1:5433` 端口及合成内容。没有生产玩家进度/数据库访问、DDL、部署、Quest 开关或 BQ 停用。R.2.4 只关闭自动回归缺口；流体行为、M.2 与 BetterQuesting 替代仍 `BLOCKED`。

## R.2.5：非消耗 Fluid 快照与 PARTY 兼容（自动验收完成）

对照固定参考 `Reference/BetterQuesting/src/main/java/bq_standard/tasks/TaskFluid.java`，确认非消耗流体检测对每个需求独立扫描同一份未排出的容器库存：重叠需求可复用相同观察量；`groupDetect=false` 保留已满足的单项、重算未满足项，`groupDetect=true` 重算全部未完成项，任务整体完成后继续锁存。Base 原评估器虽计算绝对值，但 `QuestEngine` 只把非消耗 Retrieval 当作快照，导致 Fluid 的下降值又被只增不减合并覆盖；现将指定的非消耗 `bq_standard:fluid` 流体快照也纳入绝对快照归约，消费型流体及其他事件驱动任务仍沿原单调规则。

非消耗导入 Fluid 现在通过现有 PARTY 背包租约：流体观察计划开启同一 fenced session，快照属性在一个租约中同时保存当前观测域，避免物品与流体刷新互相覆盖；仅带匹配导入标记的 Fluid 任务接受 PARTY 流体聚合。聚合使用未过期成员租约，之后按 BQ PARTY 进度路径为每位成员写入独立 PLAYER 进度。没有新增表、DDL、第二套队伍查询或消费/奖励合同。

Core 回归覆盖多需求重复使用同一库存量、`groupDetect` 两种规则、已完成锁存、非导入任务拒收 PARTY 快照及 Fluid-only 观察启用 PARTY 租约。隔离 PostgreSQL 集成覆盖新鲜租约聚合、过期租约排除、进度从 900 mB 随库存下降重算到 700 mB、成员进度扇出以及达到目标后的完成。CodeGraph 同步完成；影响复核包括 `FluidSnapshotTaskEvaluator.evaluate`（7 个关联符号）、`BqPartyInventorySnapshotFactSink.publishPartyAggregate`（4 个）与 `QuestEngine.isNonConsumingSnapshotTask`（3 个）。

Docker Gradle 完整 `:galaxy-quest-core:test` 257 项、`:galaxy-quest-postgres:test` 52 项、`:galaxy-quest-bq-importer:test` 46 项，加根隔离 `BqPartyInventorySnapshotFactSinkPostgresTest` 9 项，共 364 项，全部执行且 0 skipped/failure/error；`:assemble :verifyUi2SelfContained` 成功，`git diff --check` 通过。期间一次初始 Core 测试因测试 fixture 缺少 importer 必需的 `factType` 字段失败，修正 fixture 后全套通过；首次 JDBC 回归则精确暴露引擎归约错误，代码修正后 9 项根集成均通过。

测试只使用 `jgb-r25-quest-it` 临时 PostgreSQL 16 容器、tmpfs、无宿主卷、绑定 `127.0.0.1:55432` 和合成玩家/任务数据；验收后容器自动移除且端口已确认释放。未访问生产数据库或真实玩家进度，未执行生产 DDL/迁移、部署、Quest 开关或停用 BQ。此批关闭的是自动语义回归，不是 M.2 行为整体签收：真实 Forge/GTNH 流体容器、跨服在线/离线边界及 PARTY 成员资格时序仍未人工或实机验收，M.2 与 BQ 替代继续 `BLOCKED`。

## R.2.6：Crafting 统计检测与 PARTY 在线资格（产品合同已确认；自动回归完成，Forge 运行时待验证）

用户已确认本批目标合同：非消耗类物品/流体任务按玩家库存或已接入容器快照自动观察；GT 机器产物只按物品/流体是否存在检测，不伪造玩家合成事件；只有消耗型任务需要玩家明确提交。Crafting 统计属于个人进度，不把不同 Party 成员的计数合成一个人的值；每个服务端只发布本服在线 Party 成员各自的个人统计快照，归属各自 PLAYER 进度。同一玩家在不同服务器的计数按来源服分别取 `max(该来源事件累计, 该来源绝对统计快照)` 后求和；来源维度由 `source_progress_json` 持久化，旧版聚合值按 legacy baseline 非叠加保留。M.2 的 Quest/Party 主体模型不在 R.2.6 改动。

固定 BQ 源码确认，匹配的玩家 craft/smelt/anvil 输出会立即触发匹配的 `TaskCrafting`；该 Task 的 `onItemInternal()` 在推进事件进度后调用 `detect()`，复评同一 Task 的全部统计目标，完全无关的产出不触发。BQ 的每 60 tick `EventHandler` 只调用 `QuestInstance.update()` 检查完成/同时任务状态，并不会周期读取统计；BQ Quest 级 Detect 会遍历未完成 Task。Base 保留匹配产物即时统计，并按用户选择每 60 tick 自动复评启用统计的导入 Crafting Task，因此不向玩家展示“检测合成”手动按钮；Quest 级 Detect 专门用于逐项提交尚未完成的消耗任务。两条 Crafting 路径都逐个读取本服在线玩家本地 `EntityPlayerMP` 统计并以其 UUID 生成个人 PLAYER 事实，不经过 Party 进度 fan-out；服务端 Planner/Runtime 决定活动 PARTY 资格、进度、前置和周期。周期轮询是有意的交互差异，不声称为 BQ 原生行为。标记快照不会推进非导入任务；普通未标记的统计事实维持既有兼容处理。

2026-10-11 R.2.20 补齐数值兼容：BQ 的 `TaskProgressableBase.setUserProgress()` 不截断进度，`TaskCrafting.detect()` 在绝对统计高于当前值时直接保存完整统计量；但 `onItemInternal()` 将单服产物事件进度限制在目标。Base 现在只对每来源事件计数保留该目标上限，绝对统计快照不封顶；每来源取最大值后跨来源求和，Task 完成门槛不变。Core 回归覆盖事件封顶、同来源统计优先与跨来源超目标求和；隔离 PostgreSQL 回归覆盖目标 4、统计 5 的 JSONB 持久化和重读。

### R.2.21：Hunt 事件计数按目标封顶（自动回归完成；Forge 运行仍 BLOCKED）

固定 BQ `TaskHunt.onKilledByPlayer()` 对每个匹配击杀将进度写为 `min(required, current + 1)`，不会让新事件进度超过目标。Base 的 `SubjectCountTaskEvaluator` 原先直接累加事实 `amount`；当前 Forge 工厂每个击杀固定产生 1，但通用事实路径可带批量数量，仍与 BQ 算法不一致。现将新观察值限制到 `current.getTarget()`；`TaskProgress.merge()` 保留已存在的超目标历史值，不会因修正而倒退。

新增 Core 回归以目标 2、单事实数量 7 验证进度封顶为 2 并完成；另验证已保存值 5/2 不被重写。CodeGraph `node SubjectCountTaskEvaluator.evaluate`、`node QuestGameplayEventHandler.onEntityKilled`、固定 BQ `TaskHunt.onKilledByPlayer` 与 `impact/affected` 选择该 evaluator 和 Hunt 回归。Docker `galaxy-dev:local` 的 Core 全套 282 项实际执行，0 skipped/failure/error；根 `:assemble :verifyUi2SelfContained` 成功（35 actionable tasks，23 executed、12 up-to-date）。离线构建复用 SHA-256 经核验的缓存 Qz 许可证，仅跳过许可证下载任务；自包含检查实际执行。运行 JAR 已包含新 evaluator 类，SHA-256：`ecefbb0b0968a73f425050c768e0516d0b77e973721ccf076b2771d7e975ba42`。`git diff --check` 通过。

只关闭 Hunt 的数值上限差异，不改变参与者、击杀资格、实体匹配或奖励。没有新增 PostgreSQL 用例、生产数据访问、DDL、部署或 Quest/BQ 开关操作。S2 当前 104 个 Hunt occurrence 的真实 Forge `LivingDeathEvent`、FakePlayer/取消事件、GTNH EntityList/NBT 与 PARTY 时序仍未验收，Hunt/M.2 继续 `BLOCKED`；Crafting R.2.19 的 Party 重试归属也仍待用户决定。

### R.2.22：Crafting 来源账本周期隔离与 max/sum 持久化（Core + 隔离 JDBC 自动回归完成）

固定 BQ `QuestInstance.resetUser(uuid, false)` 会对每个 Task 调用 `resetUser(uuid)`，重复 Quest 开启新周期时旧 Task 进度必须归零。Base 的新周期从空进度投影，因此旧 Crafting `source_progress_json` 不应把上一周期来源事件贡献带入新周期。Core `QuestEngineTest.repeatableCraftingStartsNextCycleWithoutPriorSourceEventLedger` 验证上一周期 S2 事件完成后，冷却结束的新周期只保留 Lobby 事件，随后重新观察 S2 绝对统计 3 后总值为 4（S2 event=0、observed=3）。隔离 JDBC 周期回归验证 cycle 1 upsert/reload 不会让旧 S2 ledger 复活；另一个 JDBC 回归验证同来源事件 3/快照 5 只贡献 5、另一来源事件 4/快照 2 贡献 4，数据库重读后的总值仍为 9。

CodeGraph `impact QuestEngine.evaluate` 覆盖 21 个关联符号；`impact JdbcQuestRuntimeRepository.saveProgress` 覆盖 17 个符号；`affected` 按调用链收敛到 Core `QuestEngineTest` 和 PostgreSQL `QuestPostgresIntegrationTest`。Docker `galaxy-dev:local` 离线运行完整 `:galaxy-quest-core:test`，283 项、0 skipped/failure/error；随后在临时隔离 PostgreSQL 16 上运行两个 Crafting 定向集成，2 项、0 skipped/failure/error。第一次 JDBC 构建因临时 Gradle init 脚本已清理而在任务配置前退出，未执行测试；恢复隔离脚本后重跑成功。测试库使用 512 MiB tmpfs、无宿主数据卷、仅绑定 `127.0.0.1:55521`，测试后已销毁并确认端口释放。`git diff --check` 通过。本项只增加 Core 与 JDBC 测试，没有修改生产 Java、schema、统计采样或玩家合同；本轮未重跑根 `assemble` 或自包含门禁，生产代码门禁证据沿用紧邻此前 R.2.21 验收。

Core 与 JDBC 回归共同证明，周期 1 替换已完成周期时，旧来源事件 ledger 不会从内存投影或 `source_progress_json` 复活；重新读取后，新来源事件仍在，绝对快照可在新周期重新观察。多来源 round-trip 也证明采用用户确认的“来源内 max、来源间求和”，不把事件和绝对统计双重计入。它不表示 BQ 玩家生命周期统计会在新周期清零。真实 Forge/GTNH 统计读取与 Party 重试时点仍未覆盖，Crafting/M.2 继续 `BLOCKED`；R.2.19 Party 事件时/重试时归属选择仍待确认。

实现新增 `BqPartyCraftingStatisticsMembers`：基于数据库 membership 与当前服务端在线目录解析来源玩家所在的唯一 Party，只返回此服在线成员；无 Party 时退化为在线玩家自身，歧义或查询异常不扩大到全体 roster。`QuestGameplayEventHandler` 按 BQ 顺序“成员→Task 内统计目标”构造产物触发事实；60 tick 周期路径则逐个目标调用认证统计服务。事件和周期检测异常均在服务端事件边界记录，不逃出玩家 tick；下一既有周期会重试。`QuestObservationPlan` 先用 Task 的 item/channel matcher 过滤产物触发，不相关输出不读取统计。周期绝对快照使用稳定的玩家/Quest/版本/cycle/Task/物品/计数幂等键；当一个已完成 Task 仍处于 LOCKED、玩家之后满足前置时，使用独立稳定键复评 Quest 完成，避免旧统计事实的重复键阻止奖励结算。统计事实经现有 Planner、Runtime 和 JDBC 事实账本进入每位成员自己的进度行，不做跨成员求和。

自动回归覆盖成员解析/solo/离线排除/歧义拒绝；Crafting 同 Task 与跨 Task 触发范围；60 tick 目标分发和单 Task 数据库异常隔离；自动 Party 资格、个人进度与锁定完成后的解锁复评；以及 PostgreSQL roster → 在线目录 → Planner/Runtime/JDBC 的独立个人进度与幂等。2026-10-10 本轮 `:galaxy-quest-core:test` 全套 270 项、根分发/故障 5 项、隔离 PostgreSQL 集成类 14 项，共 289 项实际执行、0 skipped/failure/error。测试使用合成玩家、统计读取器和隔离 PostgreSQL 16 临时库，不代表真实 Forge 事件、`EntityPlayerMP` 统计文件或各 GTNH 服务在线时序。R.2.6 自动门槛完成；Crafting/M.2 行为等价仍因真实 Forge/GTNH 运行证据而 BLOCKED。

2026-10-10 后续补齐 JDBC 锁定/解锁组合回归：Party 在线 peer 可将自己的 Crafting 统计写入 LOCKED 个人进度但无奖励；本人前置满足后，新的稳定事实键完成 Quest 且奖励权益恰一条。最新完整定向复验为 Core 270 + 根分发/故障 5 + PostgreSQL 集成类 15 = 290 项，0 skipped/failure/error；临时 PG 16 使用 tmpfs/无宿主卷/loopback，并已清理。CodeGraph `impact detectAutomatically`、`impact QuestRuntimeService.applyIf` 和 `affected` 将影响链收敛至事件入口、统计服务及该集成类。该结论仍是自动回归，不是实机签收。

2026-10-10 曾以全局最大值实现验证 `s2=5 → lobby=2 → lobby=7` 为 `5 → 5 → 7`；该实现只作历史记录，随后按用户确认更换为来源维度 `max(本服事件累计, 本服绝对快照)` 后跨来源求和。当前实现与旧聚合数据迁移边界见 R.2.6 能力矩阵和 Worklog 2026-10-10/11。

（历史记录，已由下方 2026-10-11 R.2.17 当前交互更正覆盖）已移除玩家任务详情中的“检测合成”按钮；导入 BQ Crafting 统计使用匹配产物即时复评和 60 tick 自动复评，显式统计检测服务/旧协议仅为兼容保留。该时点尚未确定是否将消耗入口合并为 Quest 级批量 Detect；当时逐 Task 手动提交仍暴露。Checkbox 等本身定义为手动确认的任务保留自己的确认动作。配方数量回查仍仅限 `InventoryCrafting` 且上报数量恰为 0；负值不触发回查。

2026-10-11 当前交互更正：Quest 页面只保留单一 Quest 级消耗 Detect，按发布定义顺序处理可消费任务；Crafting 统计继续由匹配产物与自动周期复评，不提供手动 Crafting Detect。旧 `QUEST_DETECT_CRAFTING` 网络枚举为兼容保留，但请求只返回“合成统计由系统自动复评”的说明，不再触发进度写入。批次顺序、失败恢复及结算见 R.2.17；其支持范围仍是经过授权的 PLAYER consuming Retrieval/Optional Retrieval/Fluid 子集。

剩余验证为真实事件注册、服务端在线 Party 查询与 `objectCraftStats` 读取时序/性能；不得把合成事件和隔离 JDBC 结果写成实机签收。本轮不部署、不访问生产进度、不执行生产 DDL，也不改变 Quest 开关或 BetterQuesting 状态。

2026-10-11 自动重试边界补测：`QuestCraftingStatisticsDispatchTest.automaticBqDetectionRetriesFailedImportedTaskOnTheNextPeriodicPass` 在首次自动复评模拟一个导入 Task 的瞬时数据库异常，确认同批下一个导入 Task 仍处理；第二次复评确认失败 Task 再次尝试且成功，未带 `bq.raw` 的项目自有 Task 始终不进入轮询。Docker `galaxy-dev:local`、无网络且独立 Gradle 输出运行该根测试类 4 项，0 skipped/failure/error。此测试直接调用周期检测方法，不覆盖真实 Forge 60-tick 调度或 PostgreSQL 故障恢复时序；不提升 R.2.6 实机状态或 M.2 兼容结论。

满背包奖励存在崩溃重放窗口，策略仍需明确：延期交付与落地掉落是不同可观察行为。不能静默改玩法，也不能复制 BQ“先发奖再 claimed”顺序绕过数据库资格。与既定需求冲突的策略先说明再决定。

观察站、新职业/贡献、全注册表通用扩展、生产迁移均不进入 R.1/R.2。其余实际使用 Task/Reward 仍按原 M.2 顺序；玩家进度、影子和切换仍走 M.3–M.6。

## R.2.7：block_break 导入到 JDBC 进度回归（合成事件自动验收完成）

固定 BQ 语义来自 `bq_standard.handlers.EventHandler.onBlockBreak` 与 `TaskBlockBreak.onBlockBreak`：仅处理未取消的服务器玩家事件；按任务定义顺序检查需求，方块/NBT/OreDictionary 匹配后只命中首个需求并向参与者进度写入一次。即使首个匹配需求已达标，BQ 仍会在该匹配处停止扫描；只有后续事件不匹配该项时才可能推进下一项。Base 的 `QuestGameplayEventHandler.onBlockBreak`、事实工厂和 `BlockVectorTaskEvaluator` 已有对应实现与 Core/importer 回归。

本轮新增 `importedBlockBreakPreservesFirstMatchingRequirementOrderInJdbcProgress`：合成 BQ JSON 经 importer 导入、PostgreSQL 发布、正式 Planner/Runtime/sink 持久化；检查事实重放去重、第一项达到 `[2,0]` 后相同方块仍不会越到第二项，以及不同方块最终完成 `[2,1]`。Core evaluator 38、Importer codec 5、根项目隔离 PostgreSQL 集成类 11，共 54 项实际执行，0 skipped/failure/error。只使用隔离 PostgreSQL 16 临时库和合成事实；没有修改生产行为。此测试不调用真实 Forge BreakEvent，也不证明 GTNH 方块/NBT/OreDictionary 注册、Party 在线名单或跨服事件时序，故只关闭自动 JDBC 回归缺口，不将类型或 M.2 标记为行为等价。R.2.6 产品合同已由用户确认、自动回归已完成；真实 Forge/GTNH 运行证据仍阻断，之后按 M.2 实际使用矩阵逐项推进。

## R.2.8：Hunt 注册实体边界与隔离 JDBC 回归（自动验收完成；运行时仍阻断）

固定语义来自 `Reference/BetterQuesting/src/main/java/bq_standard/tasks/TaskHunt.java` 的 `onKilledByPlayer`：BQ 必须同时解析 `EntityList.stringToClassMapping` 中的目标类和被击实体的注册 ID；任一缺失就不计数。`subtypes=true` 只在双方注册有效时按 Java 类继承关系匹配；不存在目标不能退化成“匹配任意实体”。

发现 Base 的 Hunt 事实工厂在实体本身未注册时，仍为该 Java 类收集已注册父类别名；Core evaluator 仅凭父类别名就会允许目标推进，和 BQ 的缺失注册 ID 保护不同。另一个缺口是 evaluator 对缺失/空 `subject` 的定义或事实仍继续计数，而 BQ 对缺失目标/实体注册数据直接返回。最小修复：未注册击杀实体不提供 subject/aliases，也不尝试序列化其实体 NBT；evaluator 遇到缺失/空目标或事实 subject 时保持原进度。没有调整已注册实体的精确/子类、伤害类型、NBT、Party 扇出或周期语义。

新增 Core 回归 `huntRejectsMissingTargetAndUnregisteredEntitySubject`，覆盖缺失目标和无注册 subject、但带已注册父类 alias 的伪事实；`BqCompatibleTaskEvaluatorsTest` 39 项全过。根 `compileJava` 通过。临时 PostgreSQL 16 上运行 `QuestPostgresIntegrationTest.concurrentBqHuntFactsUseDatabasePartyMembershipAndCompletePersonalProgress`：1 项通过、0 skipped/failure/error，继续验证 PARTY 成员 JDBC 查询、个人进度与幂等链未回归。测试库 `quest_r28_hunt_test` 使用 tmpfs、无宿主数据卷、仅 loopback `127.0.0.1:55481`；容器已停止并移除，端口已释放。`git diff --check` 通过。

该证据不调用 Forge `LivingDeathEvent`，也未覆盖事件取消/FakePlayer 过滤、真实 GTNH `EntityList` 注册、实体 NBT、跨服 PARTY 时序或真实游戏测试。Hunt 与整体 M.2 仍 `BLOCKED`。本轮没有读取生产玩家进度/数据库、执行生产 DDL、部署、启用 Quest 或停用 BQ。R.2.6 的统计合同与自动回归现已收口，R.2.9 Location/Meeting 自动门槛也已完成（见下节）；下一步不再重复这两个工作包，应从真实 DefaultQuests 使用矩阵和未闭合证据挑选后续 M.2 自动差异，所有缺少 Forge/GTNH 证据的项继续保持 `BLOCKED`。

## R.2.9：Location / Meeting 行为兼容闭环（自动验收完成；平台实机证据 BLOCKED）

选择依据：M.2 行为矩阵中的 S2 `DefaultQuests` 只读画像有 33 个 `location` occurrence、1 个 `meeting` occurrence；`interact_item`/`interact_entity` 未出现在该快照中，故本包不扩展到未使用类型。R.2.6 的产品合同及自动验收已收口，真实 Forge/GTNH 运行证据仍单列 BLOCKED；本包已开始，不等待 R.2.6 实机验收。

工作范围按以下顺序执行，不把既有 evaluator 存在等同于行为完成：

1. 已完成源码入口核对：BQ `TaskLocation.detect/tickTask` 每 100 个服务端玩家 tick 检查一次；BQ `TaskMeeting.detect/tickTask` 每 60 tick 对 `player.boundingBox.expand(range, range, range)` 执行独立实体查询。Location 检查维度/范围后再依次检查 biome、structure、visibility；命中时写入参与者集合。活跃 Quest、`canSubmit`、前置、领取及重复重置会影响周期调度，不能只看 evaluator。
2. 已修复 Meeting 合并最大半径的差异：Minecraft 事实现在保留观察者与各实体的 AABB，Core 对每个 Task 重新应用该 Task 的扩展 AABB；边界回归覆盖较远实体排除、实体盒部分相交计入、只接触边界不计入、缺少 geometry 时失败闭合。Core `BqCompatibleTaskEvaluatorsTest` 41 项全过；隔离 PostgreSQL 的 PARTY→个人进度集成 1 项全过；根 `compileJava compileTestJava` 通过。
3. 已按当前 33 个 Location occurrence 的只读画像核对使用范围：全部 `biome=-1`、`range<=0`、structure 为空、visible/invert/taxiCabDist=false，因此实际匹配条件仅为 dimension；现有 `LocationTaskEvaluator` 对该参数组合与 BQ 一致，不为未使用的结构/视线/biome 配置增加泛化实现。
4. 已完成 Location 端到端隔离回归：`BqMigrationDatabaseIntegrationTest.importedRepeatableLocationFlowsThroughPartyProgressAndJdbcClaimCycles` 从合成 BQ Location JSON 经 importer/migration 发布到 JDBC，再以已发布目录生成位置观察计划；按 `MinecraftQuestFactFactory.locationObserved` 的 `dimension/x/y/z/biome` 形状提交合成事实，验证在线 Party 资格、成员各自 PLAYER 完成、原事件键幂等、未领奖阻止重开、领取时间锚定冷却及 cycle 1 重复完成。运行源码检查确认 Location Handler 每 100 ticks 采样，Meeting 每 60 ticks；事实字段测试锁定 Adapter 合同，但没有实例化 Forge `EntityPlayer`。
5. 本工作包自动验收完成：Core 267、Importer 47（含 PostgreSQL migration 集成）、Meeting PARTY/JDBC 1，共 315 项实际执行，0 skipped/failure/error；根 `compileJava compileTestJava assemble verifyUi2SelfContained` 通过，`git diff --check` 通过。隔离 PostgreSQL 16 使用 512 MiB tmpfs、无数据卷、仅绑定 `127.0.0.1:55485`，已删除并确认端口释放。已更新 M.2 行为矩阵、能力矩阵、Worklog 与 Docs README。真实 Forge/GTNH tick、BQ 在线成员缓存、多服事实时序仍为 BLOCKED；后续按只读 DefaultQuests 行为画像选下一实际类型，不访问生产进度/数据库、不执行生产 DDL/部署、不启用 Base Quest 或停用 BQ。

完成门槛：固定源码逐项映射、已识别半径/资格差异有回归与最小修复、Location 隔离 JDBC 周期链通过、相关模块及根构建/自包含检查通过。R.2.9 自动门槛现已通过；真实 Forge/GTNH 运行证据继续保留为 BLOCKED。R.2.6 自动门槛也已通过，但其真实运行时与整体 M.2 仍未签收。

## R.2.10：Checkbox 手动确认与导入/JDBC 闭环（自动验收完成；Party 配置/平台运行仍 BLOCKED）

固定 BQ `TaskCheckbox.detect` 为空；`NetTaskCheckbox` 只接收服务器认证发送者并按 `fullySyncQuests` 写个人或 Party UUID。源码默认值为 false 且注释标记 NYI，但 `QBConfig.init` 仍从 Forge Configuration 读取该字段，网络处理器、QuestInstance、Retrieval 与 Fluid 路径均直接分支使用；不能只凭注释判断代码路径无效。S2 磁盘 `config/betterquesting/questbook.cfg` 的值为 false，但不证明运行进程已加载或未被覆盖。Base 当前 Checkbox 服务要求认证玩家、Quest UUID、版本和 Task key，锁定资格通过 `applyIf` 在事务中判定；只接受 PLAYER 参与主体。主体语义边界不通过扩大单个任务的进度归属来规避。

新增 `importedBqCheckboxIsAnAuthenticatedManualPlayerAction`：合成 BQ JSON 经 importer 发布到隔离 PostgreSQL，再由真实 `QuestEvaluationPlanner`、`AuthenticatedCheckboxConfirmationService`、`QuestRuntimeService` 与 JDBC 进度完成。验证导入类型/事实映射、Checkbox 不启用背包/流体轮询、认证点击者完成、其他玩家无进度、重复确认无二次完成。CodeGraph `sync` 更新集成测试 1 文件/49 nodes；`impact` 目标测试方法，`affected` 选择同一 PostgreSQL 集成类。

本批 `:galaxy-quest-core:test` 267、`:galaxy-quest-bq-importer:test` 48，共 315 项实际执行，0 skipped/failure/error；`:assemble`、`verifyUi2SelfContained` 和 `git diff --check` 通过。隔离 PostgreSQL 16 使用 512 MiB tmpfs、无宿主卷、只绑定 `127.0.0.1:55486`，已删除并确认端口释放。真实 `EntityPlayerMP`/终端动作、服务器运行时 `fullySyncQuests` 加载/覆盖、Party/跨服时序和混合 Task 主体仍未验证；Checkbox、M.2 与 BQ 替代继续 BLOCKED。

下一步回到用户已确认的任务检测合同，按 S2 DefaultQuests 实际任务类型继续 M.2；不要把合成统计、玩家库存存在检测和消耗提交混成一种事实。

## R.2.11：标准奖励源码与内容对照（窄边界已修；整体验收未完成）

CodeGraph 起始 `status` 为 up-to-date。`explore` 检查 Item handler/overflow、XP、Scoreboard、Command、QuestCompletion、Claim 与 Delivery Worker；`impact MinecraftItemRewardDeliveryHandler.deliver` 确认 Item handler 现有测试和调用链。固定 BQ 源码入口为 `RewardItem`、`BigItemStack`、`RewardChoice`、`RewardXP`、`RewardScoreboard`、`RewardCommand`、`RewardQuestCompletion`、`AbstractReward` 与 `QuestInstance`。

S2 `DefaultQuests` 静态聚合为 Item 4,181、Choice 957、QuestCompletion 308、XP 2 个奖励；Command/Scoreboard/未知类型为 0。Choice 有 3,062 个选项，数量全部为正、范围 1–4,096。固定 BQ 源码显示 `RewardItem` 经 `BigItemStack.getCombinedStacks()` 将非正数量提升至 1，但 `RewardChoice.claimReward0` 会在该调用前对非正选择直接返回。Base 原先共用了 Item 规则，现按类型分开处理：Item 不变，Choice 非正数量不产出物品；空结果仍完成 entitlement marker。根 `MinecraftRewardDeliveryTest` 增加 marker 和幂等重放回归。实际任务库没有此异常值，因此修正兼容未来/异常定义，而非当前 S2 内容变更。

Item 正常掉落仍存在世界掉落实体与玩家 NBT/PostgreSQL entitlement 无法原子提交的崩溃窗口；Scoreboard 只有纯计算测试；Command 保留安全 allowlist，不复制 BQ 任意命令权限/发送者语义。`MinecraftRewardDeliveryTest` 28 项实际执行、0 skipped/failure/error；该类回归和窄修正不代表奖励子系统整体与 M.2 解 BLOCKED。CodeGraph 影响范围以 Worklog 为准；未访问玩家进度/生产数据库、未执行 DDL/部署/Quest 开关或触碰 S1/S2。

2026-10-11 Item NBT 补充审计：固定 BQ `NBTConverter.NBTtoJSON_Compound(..., format=true)` 以 `字段名:类型号` 保存 typed NBT，列表同样以 `索引:类型号` 保存；Base importer 保留原 typed JSON，奖励适配器再由 `BqTypedNbtCodec` 还原 Compound、List、数值及数组并递归替换玩家变量。只读 S2 `DefaultQuests` 聚合为 Item 4,181 项/6,271 栈、Choice 957 项/3,062 选项；9,333 个 Item/Choice 栈全部使用字符串注册名，数量为正且范围 1–8,192，440 栈带非空 Tag。Tag 中实际出现 NBT 类型 1–6、8–11，其中 IntArray 5 处；未发现当前 Item/Choice 的 numeric-ID 或 Count 差异。

新增 `importedBqItemRewardPreservesTypedNestedNbtListsAndArrays`，覆盖 importer→Minecraft reward handler 的 typed 数值、嵌套 Compound/List、变量替换、ByteArray 与 IntArray；这些输入是合成 BQ typed JSON，数组测试用于固定已支持类型，不声称当前 DefaultQuests 都使用这些形状。CodeGraph `impact BqTypedNbtCodec` 命中 20 个相关符号，`affected` 将变更测试收敛到 `BqItemRewardImportDeliveryTest`；Docker 隔离 Gradle 根定向类 5 项全部执行、0 skipped/failure/error。首次运行被历史 root-owned `injectTags` 输出阻止，未改权限/删除文件；改用全新 `.gradle/codex-reward-nbt-build-20261011` 输出路径通过。只读输入限于 S2 `DefaultQuests`，无数据库或生产行为变更。Item 的 World drop/玩家存档/entitlement 崩溃窗口仍 `BLOCKED`，此补充只加自动证据，不提升奖励或 M.2 兼容状态。

### R.2.12：Choice / QuestCompletion 导入定义回归（自动用例完成；运行时仍 BLOCKED）

Choice 的 PARTY 集成回归现在从合成 BQ typed JSON 导入 Item/Choice `RewardDefinition`，经过隔离 PostgreSQL 发布、三名玩家各自认证选择、领取和 JDBC worker/router，验证每人只收到自己选择的物品及对应 meta/NBT，重复领取/投递不重复发放。新增 QuestCompletion 回归则将 BQ 紧凑 UUID 导入为目标 Quest UUID，发布目标到隔离 PostgreSQL，再由 `JdbcQuestCompletionRewardPort` 完成目标 PLAYER 进度；重复调用只保留一次完成时间和目标奖励权益。该 QuestCompletion 用例直接调用已导入奖励的 handler，并未覆盖来源 Quest entitlement → claim → JDBC worker 的整条领取链。

本包两个定向类共 14 项实际执行（Choice adapter 2、PARTY/JDBC 集成类 12），0 skipped/failure/error。隔离 PostgreSQL 16 使用 512 MiB tmpfs、无宿主卷、只绑定 `127.0.0.1:55488`，测试后已移除并确认端口释放。它补上导入定义到适配器/JDBC 的证据，但不证明 Forge/GTNH 玩家/进度、WorldSave 崩溃边界、多服租约或奖励系统整体兼容；Choice、QuestCompletion、M.2 与 BQ 替代仍 `BLOCKED`。

### R.2.13：XP 导入字段到经验投递回归（自动用例完成；实机仍 BLOCKED）

合成 `bq_standard:xp` typed JSON 经 `BqQuestDefinitionImporter` 生成的 `RewardDefinition` 直接进入 `MinecraftXpRewardDeliveryHandler`，分别验证 `isLevels=true` 映射为 levels、false 映射为 points、经验等级/进度、marker、flush-before-sync 与幂等重放。`BqItemRewardImportDeliveryTest` 和既有 `MinecraftRewardDeliveryTest` 合计 31 项实际执行（3 + 28），0 skipped/failure/error；没有数据库连接。CodeGraph `impact MinecraftXpRewardDeliveryHandler.deliverToPlayer` 和 `affected BqItemRewardImportDeliveryTest` 选择了对应回归范围。

此项不改变 XP 算法或奖励策略，不覆盖真实 `EntityPlayerMP`、玩家存档/进程恢复、多服租约，也不证明 S2 的 2 条 XP 内容逐项可运行。XP 与整体 M.2/BQ 替代继续 `BLOCKED`。

### R.2.14：QuestCompletion 来源领取到 JDBC Worker（自动链通过；平台行为仍 BLOCKED）

扩展 R.2.12 的目标处理器测试：合成来源 Quest 由个人库存观察完成，QuestCompletion reward 来自 BQ typed JSON importer；玩家通过 `JdbcRewardClaimRepository` 领取后，由生产 `RewardDeliveryWorker`、JDBC delivery repository 与 router 调用目标完成 handler。断言目标 PLAYER 进度完成、目标奖励权益只创建一次，后续 Worker poll 不再租赁已投递的来源权益。来源/目标 Quest 定义均为合成构造，并不证明整份来源 Quest JSON 导入或真实游戏动作。

隔离 PostgreSQL 根测试类 13 项实际执行、0 skipped/failure/error；临时 PostgreSQL 16 只使用 loopback `127.0.0.1:55489`、tmpfs 与无宿主卷，完成后已清理。CodeGraph `impact`/`affected` 选择该单一集成类。此项补充自动调用链证据，不解除 QuestCompletion、奖励子系统、M.2 或 BQ 替代的 BLOCKED 状态。

### R.2.15：导入 Item 最大数量与背包溢出（自动回归通过；世界持久化仍 BLOCKED）

合成 BQ typed JSON 的 Item `Count=8192` 经 `BqQuestDefinitionImporter` 生成奖励定义，再交给 Minecraft item reward delivery adapter。36 格主背包收到 36 个 64-stack，剩余 5,888 个拆为 92 个 64-stack 溢出；断言总量、完成 marker、重放不重复掉落且不重复 flush。该用例对齐 BQ `RewardItem.claimReward0` / `BigItemStack.getCombinedStacks` 的拆栈和溢出形态，只验证 adapter 替身，不代表真实 World drop 或玩家存档。

`BqItemRewardImportDeliveryTest` 4 项与 `MinecraftRewardDeliveryTest` 28 项共 32 项实际执行，0 skipped/failure/error。CodeGraph 同步 1 个测试文件/63 节点；`impact` 检查奖励拆栈/交付链，`affected` 选择导入交付回归类。测试使用合成 JSON 与 fake player，不连接数据库。`:assemble :verifyUi2SelfContained` 成功（29 actionable tasks，7 executed、22 up-to-date），最终 `git diff --check` 通过。

边界仍包括真实 EntityPlayer/模组物品最大堆叠、World 存档、掉落实体与 PostgreSQL entitlement 之间的崩溃窗口；不得把“正常返回时总量一致”表述为 exactly-once。R.2.15 不改变奖励策略，不解除奖励系统、M.2 或 BQ 替代的 BLOCKED 状态。

### R.2.16：完整导入 QuestCompletion 来源/目标定义（隔离自动链完成；实机仍 BLOCKED）

扩展 R.2.14 的隔离 PostgreSQL 闭环：来源 Quest 与目标 Quest 均由合成 BQ typed JSON 经 `BqQuestDefinitionImporter` 导入并发布。来源 Quest 含非消耗 Retrieval 与 QuestCompletion reward；其 Retrieval 通过 `BqPartyInventorySnapshotFactSink` 的 PARTY 库存观察路径进入 JDBC Planner/Runtime，玩家显式 claim 后由 JDBC `RewardDeliveryWorker`/router 调用 QuestCompletion handler。目标是完整导入的 Checkbox + Item reward Quest。断言来源完成、目标 PLAYER 进度与 Task 完成、目标奖励权益仅生成一次、后续 Worker poll 不再租赁来源权益。

CodeGraph `sync` 更新测试文件 1 个/217 nodes；`impact BqQuestDefinitionImporter.read`、`impact importedBqQuestCompletionRewardFlowsFromSourceClaimThroughJdbcWorker` 与 `affected` 均定位 `BqPartyInventorySnapshotFactSinkPostgresTest`。Docker Gradle 在无数据卷 PostgreSQL 16 Alpine 临时库上运行该整个集成测试类 15 项，0 skipped/failure/error；只绑定 `127.0.0.1:55491`，测试后容器自动删除且端口释放。`:assemble :verifyUi2SelfContained` 成功（29 actionable tasks，1 executed、28 up-to-date）；最终 `git diff --check` 通过。

这补上了 R.2.14 手工构造来源/目标定义的限制，但 Quest JSON、玩家与 Party 都是合成数据；没有导入完整 DefaultQuests，也没有调用真实 Forge/GTNH 事件、玩家存档或跨服 Worker。QuestCompletion、奖励系统、M.2 与 BQ 替代仍 `BLOCKED`。

### R.2.17：Quest 级消耗 Detect 与批次末结算（自动验收完成；实机仍 BLOCKED）

按用户确认的 BQ 交互合同，UI2 仅提交 Quest ID；服务端从已发布定义中按 Task 顺序选择未完成、可消费的 PLAYER Retrieval / Optional Retrieval / Fluid，逐项调用现有消费 Saga。批次表保存源服、玩家/进度主体、Quest 版本、周期、顺序列表和下一个游标；操作键稳定为 `batchId:index`，重试时重用原提交，不重复扣库存。Task 事实逐项持久化；Quest 完成、奖励和 OR 汇总延迟至整批结束，并与批次游标/记录删除在同一事务收束。中断后只允许源服继续访问其在线玩家库存；登录恢复可推进已有单 Task Saga，批次游标/最终结算在玩家再次触发 Quest Detect 时续跑。这里明确不承诺背包副作用与 PostgreSQL 的跨系统全有或全无。

**版本并发处理已在 R.2.34 完成自动验收。** 用户选择允许管理员发布新版本；暂停中的批次继续固定旧版本定义与奖励，旧版本进度保留。活动批次唯一性改为参与者+Quest，不再按定义版本允许平行批次；重入、每个 Task 授权、确认事实与最终结算都指定批次版本。隔离 PostgreSQL 集成测试覆盖“v1 批次 → 发布 v2 → 新入口接续 v1”、历史版本精确读取与 migration 重复应用。若新定义删去旧任务，旧批次仍可能按旧定义完成，必要时由管理员人工补偿。生产 DDL 与 Forge 在线运行仍未验证。

回归覆盖两个消耗 Task 的定义顺序、第二项中断后的稳定键与游标恢复、OR Quest 不提前完成/发奖、仅源服可恢复、批次完成后一次结算；重复 Quest 在上周期奖励仍可领取时不提前进入新周期，并验证未持久化 Task 事实时可按批次绑定的下一周期结算。隔离 PostgreSQL 测试覆盖批次表幂等应用、唯一活动批次、重复创建与游标推进幂等，并验证完成结算 fence。CodeGraph 检查 `AuthenticatedQuestConsumptionDetectService`、`QuestRuntimeService.reconcileAfterConsumptionDetectBatch`、`ConsumptionRequestAuthorizer.planQuestDetect`、`JdbcConsumptionDetectBatchRepository`；影响候选较宽，按调用链选择 Core、Postgres、UI2、根 Quest/终端测试。

本批初次完整自动构建（2026-10-11）为 Core 277、Quest Postgres 58、UI2 Terminal 28、根项目 591 项；根项目有 52 个其它业务 PostgreSQL 配置缺失的 assumption-skip，0 failure/error。Quest Postgres 使用独立临时 PG16 实跑全部 58 项且 0 跳过。`compileJava :assemble verifyUi2SelfContained` 成功。其后追加的“claimable reward 冷却阻止提前开新周期”Core 回归尚待重跑，最终数字以 Worklog 的最终复验为准。批次迁移未执行生产 DDL、未部署、未启用 Quest/停用 BQ；真实 Forge/GTNH 交互与切服恢复仍待验证，因此 M.2 和整体替代继续 `BLOCKED`。

2026-10-11 最终复验更正：新增的 claimable-reward 周期回归现已重跑完整 Core 278 项，0 skipped/failure/error。上方首次完整构建报告中的 Core 277 是该新增用例之前的数量；其余当时报告的根任务计数为历史结果，当前完整复验见本节最新补记与 Worklog。

### R.2.17 最终交互与验证补记

2026-10-11 最终交互/隔离复验补记：用户确认 Quest 级单一 Detect；非消耗库存任务持续自动观察，Crafting 统计由匹配产物与周期复评，不提供手动“检测合成”。旧 `QUEST_DETECT_CRAFTING` packet ID 保留解码，但服务端只返回自动复评提示，不写进度；新的 `QUEST_DETECT_CONSUMPTION` 才触发持久批次。另确认同玩家 Crafting 统计按来源服分别取事件累计与绝对快照最大值，再按来源求和。CodeGraph 起始 `status` up-to-date；检查 `QuestGameplayEventHandler.submitCraftingStatistics` 的 callers/callees、`AuthenticatedQuestConsumptionDetectService` 与 `TerminalQuestCenterSnapshotMapper.detail` 的 impact；`affected` 对 Terminal/Minecraft 组合根给出 141 个宽泛候选，最终覆盖 Core、Quest Postgres、Importer、UI2 Terminal 与根全量。全量 `test`：592 项、67 条条件跳过、525 项实际执行，0 failure/error；模块套件 Core 278、隔离 PostgreSQL 模块 58、Importer 49、UI2 Core 41、Lab 2、Terminal 28、Demo 5 均通过。Quest Postgres 42 项数据库集成与 Importer 7 项迁移集成使用隔离库实际执行；根 PARTY/JDBC 集成类 15 项 0 skip。`:assemble`、`verifyUi2SelfContained` 与 `:ui2-demo:renderGolden` 通过；Golden 首次因遗漏只读 CJK 预览字体挂载而未进入渲染，挂载后重跑成功。测试库为 PostgreSQL 16、512 MiB tmpfs、无持久卷、仅 loopback `127.0.0.1:55497`；结束后删除并确认端口释放。未访问生产进度/数据库，未执行生产 DDL、部署、Quest/BQ 开关或 S1/S2 操作。真实 Forge/GTNH 运行、源服停机时的批次恢复和奖励/存档崩溃窗口仍未验证，M.2 与整体替代保持 BLOCKED。

### R.2.18：Quest Detect 下不可分割 Fluid 容器超量进度（自动验收完成；Forge 运行仍 BLOCKED）

固定源码 `Reference/BetterQuesting/src/main/java/bq_standard/tasks/TaskFluid.java` 的 `detect` / `Detector.run`：逐槽扫描玩家背包；对每个未满足的流体目标请求剩余量，但将容器实际返回的 `FluidStack.amount` 直接加到任务向量。通过 `FluidContainerRegistry` 取出的不可分割容器可能整份排空并让进度超过目标。S2 当前 DefaultQuests 有 15 个 Fluid Task、16 个需求，其中唯一 consuming Task 是 Honey 1,000 mB；实际 GTNH 容器注册组合尚未验证。

Base 原实现排空了不可分割容器，却把 `min(剩余需求, 实际排出量)` 作为应用量，且通用 `TaskProgress.advance` 封顶，因此数据库进度和 BQ 不同。现改为 Fluid consumption saga 保存适配器报告的实际排出量；Fluid evaluator 对实测量做饱和累加并允许越过目标。`ConsumptionRequest.confirmedFact` 与 `JdbcConsumptionSubmissionRepository.markApplied` 仅对服务端 Fluid 结果放开“大于本次剩余请求量”的边界；Retrieval/Optional Retrieval、XP 仍维持原封顶校验。Quest Detect 仍按任务定义顺序逐项扣除、持久化，批次末统一结算。

回归包括 Core 进度 400 mB 后整只 750 mB 容器满足剩余 600 mB、最终进度 1,150/1,000；Minecraft 容器规划器验证返回实际扣除量；根隔离 PostgreSQL 集成从合成 BQ Fluid JSON 导入发布，并通过 Quest 级 Detect 与消费 Saga/JDBC 验证实际排空 1,150 mB、保存超目标进度、重放不重复扣除。该输入模拟 BQ 不可分割容器，不冒称 Forge/GTNH 实机容器。

CodeGraph `status/sync` 更新 8 个变更文件；`impact` 覆盖 `TaskProgress.advanceMeasuredAmounts`、`ConsumptionAppliedTaskEvaluator.evaluate`、`MinecraftPlayerConsumptionPort.planFluidsDetailed` 及消费 Saga；`affected` 给出 94 个宽候选，按直接调用链收敛到 Core evaluator、Minecraft consumption adapter 与 `BqPartyInventorySnapshotFactSinkPostgresTest`。Docker Gradle 通过：Core 279、Quest PostgreSQL 58、Importer 49 均 0 跳过；根 `:test` 593 项中 52 项因其他模块专用测试库配置未提供而条件跳过，541 项实际执行，0 failure/error；根隔离 Quest/JDBC 集成类 15 项 0 跳过。`:assemble` 与 `:verifyUi2SelfContained` 通过，`git diff --check` 通过。临时 PostgreSQL 16 使用 512 MiB tmpfs、无宿主卷，仅 `127.0.0.1:55498`，结束后容器删除且端口释放。未执行生产 DDL、部署、Quest/BQ 开关或读取玩家进度；Fluid 与 M.2 仍 BLOCKED。

### R.2.24：QuestCompletion 缺失目标按 BQ no-op 收束（自动验收完成；运行时仍 BLOCKED）

只读复核获授权的 S2 `DefaultQuests/Quests`，共解析 3,744 个定义、0 个 JSON 错误；308 个 QuestCompletion 奖励指向 251 个不同目标，其中 219 个目标可在本目录解析、32 个目标不存在，缺少/格式错误 UUID 均为 0。该统计只读取 Quest 定义，不读取 Players、在线状态、其它服务器配置或数据库。固定 BQ `RewardQuestCompletion.claimReward0` 在目标 Quest 不存在时直接 no-op，来源奖励领取本身仍可结束。

Base 原先将 `TARGET_NOT_FOUND` 映射为不可重试失败，RewardDeliveryWorker 会把 entitlement 留在永久失败/恢复路径，与 BQ no-op 不同。现在只将明确的“目标不存在”视作已处理投递；格式错误 UUID 及目标参与范围不可用仍保持失败，不掩盖配置损坏或身份问题。`QuestCompletionRewardDeliveryHandlerTest` 拆分并覆盖这三种结果。

CodeGraph `status` 起始 up-to-date；`explore JdbcQuestCompletionRewardPort RewardQuestCompletionDeliveryHandler`、`impact JdbcQuestCompletionRewardPort.completeInTransaction`、`explore QuestCompletionRewardDeliveryHandlerTest`、`explore RewardDeliveryOutcome RewardDeliveryWorker JdbcRewardDeliveryRepository RewardDeliveryController`、`impact QuestCompletionRewardDeliveryHandler.deliver` 与改后 `sync/affected` 核对了端口→handler→Worker 的影响链。`affected` 给出 49 个宽候选，按直接调用链收敛到 Core handler/Worker 回归及既有 Quest JDBC 集成。

使用隔离 Docker Gradle 输出执行完整 `:galaxy-quest-core:test`：286 项、0 skipped/failure/error。`:compileJava :assemble :verifyUi2SelfContained` 成功（36 actionable tasks，24 executed、12 up-to-date）；由于旧 Forge Gradle 依赖变换要求子模块 jar 已存在，先在全新忽略目录构建 7 个子模块 jar（15 tasks）再执行根打包。初次把所有 buildDir 临时重定向到容器 `/tmp` 时，依赖变换找不到尚未生成的模块 jar；没有修改历史 root-owned 构建产物，改用独立、持久的 `.gradle/codex-bq-r224-build` 后通过。一次测试后的 XML 汇总因容器缺少 `python3` 返回 127；Gradle 测试本身成功，随后用 awk 汇总并重新强制执行，得到以上确切计数。

该自动修正只证明 handler 与打包门禁，不证明 BQ 真实 Worker/PlayerData 崩溃边界、完整任务库发布时序或奖励系统等价。没有 PostgreSQL 测试或查询、生产 DDL、部署、启用 Quest/停用 BQ、读取玩家进度或操作 S1/S2；QuestCompletion、M.2 与整体替代继续 `BLOCKED`。

### R.2.27：Optional Retrieval 导入到 Quest 完成语义回归（Importer 定向自动验收完成）

固定 BQ `TaskOptionalRetrieval` 源码确认它继承 `TaskRetrieval` 的非消耗库存检测，但 `ignored(UUID)` 恒为 true；因此库存可推进其展示进度，却不能阻挡 Quest 完成。Base importer 以 `bq_standard:optional_retrieval` 类型映射为 `TaskDefinition.optional=true`，QuestEngine 排除 optional Task 的 Quest 完成计数。

新增跨模块单测从合成 typed BQ JSON 导入必需 Checkbox + Optional Retrieval，使用 Core `InventorySnapshotTaskEvaluator` 将库存事实推进到部分可选进度，再由 `QuestEngine` 确认必需任务完成时 Quest 已完成、可选 Task 保持未完成。这样补上了原先分别覆盖 importer 和 Core 行为、但未直接串起的边界；没有生产逻辑变化，也不替代 PostgreSQL/Forge/GTNH 端到端验收。

CodeGraph 起始 `status` up-to-date；`explore` 核对 `BqQuestDefinitionImporter.read`、`TaskDefinition.isOptional`、`InventorySnapshotTaskEvaluator.evaluate` 与 `QuestEngine.evaluate`，`impact` 覆盖 importer 与 QuestEngine，`affected` 指向 importer 和 Core 两个定向测试文件。Docker `galaxy-dev:local`、`--network none` 强制运行 `:galaxy-quest-bq-importer:test --tests com.jsirgalaxybase.quest.bqimport.BqQuestDefinitionImporterTest --rerun-tasks`：5 项，0 skipped/failure/error，`BUILD SUCCESSFUL`。测试不需要数据库，未执行 PostgreSQL、根 Forge 构建、assemble 或自包含门禁；`git diff --check` 通过。Optional Retrieval 与 M.2 仍 `BLOCKED`，真实 Forge/GTNH 库存及 PARTY 时序未验收。

### R.2.29：重复周期物品奖励的 marker 与延迟恢复隔离

测试使用真实 `RepeatPolicy`/`RewardEntitlement` 生成 cycle 0/1 稳定 entitlement key；cycle 0 将 64 个物品写入背包后模拟 overflow drop 失败并保留部分进度，通过 Minecraft `CompressedStreamTools` 与实际 `InventoryPlayer`/`ItemStack` NBT 序列化重建 PlayerState；cycle 1 先完成，随后 cycle 0 延迟恢复。断言两周期 marker/partial progress 独立，且投递总量守恒。

CodeGraph 起始 `status` up-to-date；`explore` 核对 `MinecraftItemRewardDeliveryHandler.deliver/deliverOverflow`、`QuestRewardMarker.key`、`RewardEntitlement` 与 Worker 确认链。改动前 `impact` 覆盖 item delivery 和 entitlement key，`affected` 收敛到根奖励测试；改后 `sync` 更新 1 个测试文件/127 节点，`impact` 将新增回归纳入影响集，`affected` 仍只选该根测试文件。Docker `galaxy-dev:local`、`--network none` 下运行根 `:test` 的 `MinecraftRewardDeliveryTest` 与 `BqItemRewardImportDeliveryTest`：29 + 5 = 34 项，0 skipped/failure/error。因离线 DNS 无法访问 `raw.githubusercontent.com`，按既有隔离流程 `-x prepareUi2QzLicense`。首轮测试夹具把奖励物品本身用作背包填充物导致总量断言误计；改用独立 filler 后通过。

此测试只证明权益键周期隔离和 Minecraft NBT 对象往返，不覆盖 Forge 世界掉落实体保存、PlayerData flusher、跨服租约并发或“世界掉落已创建、玩家 marker 尚未最终保存”的崩溃窗口。未改生产奖励策略；未跑全量测试、assemble、自包含门禁、生产数据库/DDL、部署或 Quest/BQ 开关。R.2.29 自动门槛仅部分完成，M.2 保持 `BLOCKED`。

## 每个改动必须留下的证据

开工前记录：工作包 ID、目标行为、BQ 固定源码入口、影响文件与不改范围；CodeGraph status/sync → 入口/调用链 → impact/affected。affected 未识别测试时手动补齐，不免测。

完成时记录：实际改动、来源/许可证（只有真实派生才加来源头）、测试命令与计数、失败/跳过原因、构建/自包含结果、未完成门槛。行为变更先加回归；同一缺陷修复需同步测试和文档，不能只改显示。

禁止以“可以解析”“已有实现”“纯 Java 测试通过”替代“BQ 行为等价”或“生产已切换”。测试库隔离未完成前，不无差别运行会回退生产连接的根集成测试；生产 DDL、部署、玩家进度读取和停用 BQ 均不是本批默认授权。

## 可复用的当前开发命令约束

项目根：`/mnt/m600/zhitai-data/gtnh/JsirGalaxyBase`。恢复镜像：`galaxy-dev:local`。缓存：`/mnt/m600/zhitai-data/gtnh/docker/docker-data/galaxy-dev/gradle-home`，容器挂载到 `/gradle-home`。

Gradle 使用 `--no-daemon --no-configuration-cache -PforceToolchainVersion=17`。在线插件请求需同时设置 HTTP(S)_PROXY 和 Java http/https.proxyHost、proxyPort；7892 已在 10 月 6 日验证，但后续先检查监听，不把历史连通当现在正常。临时容器 host 网络可访问本机代理；不传生产数据库凭据、不启动生产 Compose。

根任务显式使用 `:test`、`:assemble`、`:verifyUi2SelfContained`；子模块过滤器不得通过未限定的 `test` 传播到所有模块。命令错误、缓存命中、条件跳过分别记录。

最近有效环境证据：S2 `DefaultQuests` 摘要 hash `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc` 对应 3,744 个 Quest 定义文件、5,620 个 Task 实例、47 章和 3,847 个放置；Crafting 133 项中统计检测 true 6、false 127。2026-10-10 R.2.6 最新自动周期复评回归为 Core 全套 270、根分发/故障 5、隔离 PostgreSQL 集成 15，共 290 项实际执行、0 skipped/failure/error。所有输入为合成玩家/统计读取器/事实，不能代表 Forge 在线玩家解析、真实 `objectCraftStats` 读取或 GTNH tick 时序。R.2.6 自动门槛完成；真实 Forge/GTNH 与整体 M.2 仍阻断。每批重新测试后更新事实，不沿用旧数量冒充当批结果。

### R.2.33：周期调度门槛的纯 Java 回归

`QuestGameplayEventHandler` 的背包/周期事实调度仍分别使用 20、60、100 tick 周期；仅将“是否到达正间隔边界”的算术提取为 `PeriodicTickCadence`，没有改动自动 Crafting 统计频率或 Quest 语义。此提取让门槛可脱离 Minecraft 单独执行。

CodeGraph 起始 `status` up-to-date；`callees QuestGameplayEventHandler.onPlayerTick` 确认三个周期调用，改后 `impact PeriodicTickCadence.isDue` 命中 onPlayerTick 和 2 个纯 JUnit 用例；`affected` 对事件处理器/新 helper/测试返回 46 个宽候选。先以 `--network none --offline` 尝试，根 Gradle 在 GTNH convention 插件请求 `raw.githubusercontent.com` 时 DNS 不可用，未进入测试。只读检查发现本机 7892 仍在监听，遵循网络安全规范未修改该进程/服务；通过 `--network host` 的一次性 `galaxy-dev:local` 容器设置 HTTP(S) 代理后，根 `:test --tests QuestCraftingStatisticsDispatchTest --tests PeriodicTickCadenceTest --rerun-tasks` 成功，`compileJava` 与 `compileTestJava` 成功。Gradle XML 为 4 + 2 = 6 项，0 skipped/failure/error；33 actionable tasks / 33 executed。此前另以 JDK 17 `javac -source 8 -target 8` 单独编译 helper 和测试，缓存 JUnit 4.13.2/hamcrest 实际运行 2 项 `OK (2 tests)`；有 bootstrap classpath 警告。`git diff --check` 通过。

这证明根工程编译、所选周期/分发回归及 JAR 自包含构建门禁；不证明 Forge `PlayerTickEvent` 的真实触发/线程/联机时序，也不替代全量测试。隔离 runtime JAR 为 `.gradle/codex-r2-crafting-cadence-build/root/libs/jsirgalaxybase-6e80f68-main+6e80f68c22-dirty.jar`，SHA-256 `795d471321ef287b6b838fdab2751c0f724c1ad233b06fa00c801295464698ae`，未部署。未访问数据库/玩家进度、未执行 DDL/部署/Quest-BQ 开关或 S1/S2 操作；R.2.19 Party 重试归属与活动 Detect 遇新定义发布策略仍待用户确认，M.2 继续 `BLOCKED`。

</details>
