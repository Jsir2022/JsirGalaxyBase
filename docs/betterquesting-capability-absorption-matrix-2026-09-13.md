# BetterQuesting 关键能力吸收矩阵与实施路线

日期：2026-09-13
更新：2026-10-13，活动路线为 Base-only 全新服务器内容导入；玩家从零开始，导入任务全部 PLAYER。

## 唯一当前阶段表

| 阶段 | 当前状态与下一门槛 |
|---|---|
| N.1 | 自动验收完成，`adfb691`；真实 9 类 Task / 4 类 Reward 门禁及 PLAYER/Party 隔离，Core 299 项、个人库存/奖励 JDBC 16 项通过 |
| N.2 | 自动验收完成，`adfb691`；完整 3,744 Quest / 47 章空库导入、来源幂等、更新/并发/失败事务/最新批次回滚，Importer 最终 74 项全部执行通过 |
| N.3 | 代表性新玩家自动闭环及证据分层审计完成；详见 Base-only 验收文档。未逐个实际游玩 3,744 Quest，不等于 Forge 实机通过 |
| N.4 | 原入口/管理员健康与有限场景自动验收完成，`664436e`；不代表真实任务书内容完整。2026-10-12 复评确认目标/奖励/Choice 截断、图标/分项/搜索缺口，整改列入 N.9.1 |
| N.5 | 自动验收完成：根/全部模块 1,113 项、1,061 执行、52 非 Quest 条件跳过、0 failure/error；assemble、自包含、252 视觉与 JAR 内容通过，制品与 SHA 见验收文档 |
| N.6 | 自动验收完成：Base composition root 注册 9 类导入求值器、启动事件入口，4 类导入奖励均进入唯一 Router；完整自动回归见验收文档 |
| N.7 | 自动验收完成：固定 DefaultQuests 5,620 Task/5,448 Reward 参数键和值格式审计，未知键与非法布尔值 fail-closed；Importer 81 项、全工程 1,121 项报告通过。尚未证明所有参数配置与 BQ 运行语义等价 |
| N.8 | 自动验收完成：真实来源画像→Importer/evaluator；消耗 Retrieval/Fluid 经 Quest Detect、服务端授权和隔离 PostgreSQL；Item/Choice/XP/Quest Completion 来源奖励到处理器/PG；固定来源 Retrieval 同时进入 Minecraft ItemStack matcher。Core 299、Postgres 61、Importer 99 全执行通过；全工程 1,143 项、52 项非 Quest 环境跳过、0 failure/error；assemble、自包含和 252 份终端视觉通过。实际 Forge 服务端/玩家验收未完成 |
| N.9.1 | 自动验收完成：1,159 tests、0 skip/failure/error；固定来源 3,744 Quest definition→Mapper→codec→VisualModel 审计；6 个真实边界 Quest 三档 409 帧/202 次转换与 81 帧 HTML/PNG 画廊；隔离 CLI apply + Base JAR read-only preflight；assemble、自包含和 252 场景正式终端视觉门禁通过。Java2D/隔离 PG 不代表 Forge 实机验收。详见工作包和最新 Worklog |
| N.9.2 | D 自动 smoke 完成并复验：固定 3,744 Quest/47 章 apply、Base JAR 只读 preflight、1,159 tests（52 个非 Quest DB 配置 skip）、assemble、自包含和许可证门禁通过。A 仍停在 EULA：Forge 未加载 Base/Quest、未创建世界；真实 Forge 事实/奖励/存档/重启未测。须用户明确接受 EULA 后继续，且不得复用 supervisord 生产镜像或会终止现有进程的脚本。合同与证据见[N.9.2 隔离运行验收](betterquesting-base-only-runtime-acceptance-2026-10-12.md) |

当前开发入口以[N.9.2 Base-only 隔离运行验收合同](betterquesting-base-only-runtime-acceptance-2026-10-12.md)为准。可重复 Quest/PG/UI2 smoke、全工程 test、`assemble` 与自包含检查已通过：1,159 tests、52 个非 Quest 独立数据库配置型 skip、0 failure/error。N.9.2-A 的 `runServer` 目录/JDK/classpath 已检查，但 Forge 启动因未接受 EULA 停在 Mod bootstrap 前；没有声称 Base-only 实服已启动。Forge 事实/奖励/存档/重启仍未测。N.9.2 不复用 `galaxy-gtnh` 生产组服务镜像或会终止已有进程的本地双客户端脚本；不触及生产数据、DDL、开关或部署。
当前 smoke 另已通过隔离任务书 apply 与 Base JAR 只读预检（3,744 Quest、47 章、3,791 个实体未变更）。真正运行 Quest 时还需要专用新库包含 Banking 基线 schema 和 Quest schema/migrations；目前 Forge 尚未越过 EULA，不能将自动回归表述为 Mod runtime 验收。
旧玩家进度迁移、旧 Party 映射和 BQ/Base 实时影子已退出活动路线；不读取历史进度或补发迁移奖励。
M.0/M.1/M.2 已完成的自动证据作为基线保留，下文历史阶段表不再构成待办。
旧段落的测试数、待确认项及 BLOCKED 原因均是当时证据；不能作为新的工作目标。
事件时 Crafting 归属与 Detect 新旧版本并发已由用户决定并由 R.2.34 实现，
不再向用户重复询问这两项。

<details>
<summary>R.2.34 及以前的历史证据与阶段快照（展开查看）</summary>

最新窄范围进展（2026-10-11，R.2.34）：用户确认并完成两项代码级合同：Crafting 事实按事件时冻结完整 roster/Quest 资格/定义请求，权威快照失败即 fail-closed；Detect 批次允许新版发布，旧批次按固定旧定义/奖励恢复，参与者+Quest 维持单一活动批次。新增有界队列快照容量与版本迁移回归。Core 292 项、临时隔离 PostgreSQL Quest 模块 61 项、全工程 1,082 项（1,030 实际执行、52 项非 Quest PostgreSQL/Warehouse 配置 assumption-skip）均 0 failure/error；`assemble` 与 `verifyUi2SelfContained` 成功。测试不覆盖真实 Forge/GTNH 运行时或生产 DDL，M.2 整体仍 BLOCKED。
状态：R.1 与 R.2.1–R.2.34 均已有相应窄范围自动化工作包，但不代表 M.2 整体兼容。R.2.34 的事件时快照只在当前进程内有界队列中恢复；正常停服清空、进程崩溃会丢失尚未由 PostgreSQL 接收的事实。新版定义不会改写旧版进度，旧批次可按旧奖励结束；被新版删除的旧目标可能需要管理员人工补偿。S2 DefaultQuests 只读画像含 308 个消耗 Task occurrence（307 Retrieval、1 Fluid），均 `autoConsume=false`；玩家中心实际仅授权已验证 PLAYER Retrieval/Optional Retrieval/Fluid 子集。真实 Forge/GTNH 事件、物品/容器、玩家存档与世界掉落崩溃边界、跨服在线时序和生产 DDL 仍未验证。以下历史段落中“R.2.19/版本决策待确认”均为 R.2.34 前的审计状态，不是当前待办。所有 `ADAPTED` 只表示定义解析，不表示运行语义等价；本矩阵仍是唯一权威阶段口径。

2026-10-10 检测入口合同已确认：非消耗库存任务自动观察；消耗任务使用 Quest 级 Detect，并由服务端按定义顺序逐任务持久化/恢复。跨任务失败采用逐项结果，不提供跨物品栏与数据库的全有或全无。固定 BQ 源码进一步确认 Quest 完成/奖励逻辑在整轮 Task 检测之后刷新；Base 当前已用持久批次协调器将 Quest 完成/奖励延迟到批次末，自动回归见 R.2.17。

2026-10-10 当前工作树全量重读 S2 `DefaultQuests`：importer 解析 3,744 个 Quest、5,620 个 Task、5,448 个 Reward、47 章和 3,847 个放置；规划无 ERROR、`applicable=true`，未知 Task/Reward 类型为 0。仍有 64 个未放置 Quest、4 个 seeded XOR cycle 警告；`ADAPTED` 仅表示内容解析，所有运行类型继续 `BLOCKED`。这一项是只读定义解析，不含玩家进度/数据库，也不改变阶段状态。

2026-10-10 结构警告影响复核：迁移规划遍历 manifest 中全部 Quest，章节放置单独处理。合成 BQ→隔离 PostgreSQL→玩家任务中心回归确认，未放置定义会发布且不会被加入源章节 placement；分页器提供虚拟 `unplaced` / “Other quests” 汇总，默认章节结果仍只含源 placement。64 个 S2 未放置定义的具体意图仍未逐项判断，不自动补坐标。4 个 seeded XOR warning 与固定 BQ“恰有一个前置完成、完成数 ≤1 时可解锁”的规则相符；当前静态检查只证明每个循环成员有组外前置边，未证明那些入口可达。保持警告、不升级行为兼容状态。

R.2.16 最新业务自动证据：完整合成 BQ 来源/目标 Quest 均经 importer 发布，来源 Party Retrieval→显式 claim→JDBC Worker→目标 PLAYER 完成闭环在隔离 PostgreSQL 测试类中 15 项通过，0 跳过/失败/错误；仍不是 DefaultQuests 全量或 Forge/GTNH 实机签收。2026-10-10 Crafting 来源计数迁移补充验证后，Core 273、Postgres 56、根 PARTY/JDBC 15 共 344 项定向测试通过，0 跳过/失败/错误；`:assemble :verifyUi2SelfContained` 通过。该复验增加一条迁移幂等/旧进度保留测试，不升级 M.2 整体状态。

2026-10-09 开发职责、工作包与验收规则统一见 [开发导航](betterquesting-development-guide-2026-10-09.md)。本矩阵负责阶段状态，导航负责实施范围；历史交接和 Worklog 不另设权威进度。

### R.2.34 以前的阶段快照（历史，不是当前待办）

R.2.18 已补齐 BQ consuming Fluid 对不可分割容器“实际排出量可能超过剩余需求、超量仍进入 Task 进度”的自动回归和最小修复；S2 只读定义中实际有 1 个 consuming Fluid（Honey，1,000 mB）。此项只关闭算法差异的自动证据，真实 Forge/GTNH FluidContainerRegistry、IFluidContainerItem 和运行背包仍未验证。R.2.20 已修正 Crafting 来源服统计聚合会截断到目标的问题，自动 Core/JDBC 回归通过；BQ 绝对统计与跨来源总和可高于 Task 目标，完成门槛仍为达到目标。R.2.21 补齐 Hunt 新事实按目标封顶、但保留历史超目标值的 Core 回归；R.2.22 增加 Crafting 来源账本跨重复周期隔离及多来源 max/sum 的 JDBC JSONB 往返回归，最新 Core 全套 283 项、隔离 PostgreSQL 定向 2 项通过。R.2.31 另按 BQ `TaskCrafting.detect()` 的逐分量 `current < target` 条件门控来源服统计观察；未达标分量仍保留超目标绝对统计，Core 全套强制重跑 290 项通过。真实 Forge 击杀入口、Crafting/GTNH 统计读取与服务端在线时序仍未验证。Crafting R.2.19 源码复核进一步确认：固定 BQ `TaskCrafting.onItemInternal` 在合成事件中使用事件时 `ParticipantInfo.ALL_UUIDS` 扇出任务进度，并在同一检测中只对在线玩家读取其个人统计；Base 重试队列保存事件时生成的不可变事实，但事实未冻结 Party ID，Planner 在重试时按目标玩家当前 Party 归属。用户已确认来源服计数合并，但尚未决定此边界按事件时 Party 还是重试时 Party；答复前不改变进度归属。真实 Forge/GTNH 时序和 R.2.19 未决边界使 Crafting/M.2 继续 BLOCKED。

2026-10-11 用户再次确认消耗型任务采用 Quest 级 Detect，非消耗任务持续自动观察；Crafting 按来源服分别取 `max(事件累计, 绝对统计快照)` 后求和，Party 成员进度仍相互独立。这两项合同与当前实现及既有回归一致。本轮重跑 Core/UI2/根事件与 Quest 页面，以及隔离 PostgreSQL Crafting、Detect 批次和来源进度迁移测试，共 121 项通过、0 跳过/失败/错误；未改变 R.2.19 归属选择，事件积压期间 Party 变化仍待确认，M.2 保持 BLOCKED。详细命令与隔离边界见 Worklog 2026-10-11。R.2.26 另以 Core 全套 289 项验证 simultaneous 新完成转换与 Quest Detect 延迟结算；当前 S2 聚合为 3,744 个 Quest、启用 simultaneous 的为 0，因此仅是通用代码回归，不构成内容影子对照。

本次续审复确认 Quest Detect 与 Crafting 来源账本合同，并补跑库存观察回归与 Importer 数据库集成。Core 全套 289 项的强制重跑见 Worklog 前条；当前隔离重跑 Importer 全部 50 项，0 跳过/失败/错误。早先没有测试库时的 43/50 结果保留为历史记录。根 Forge 定向测试因设置插件离线缺失、临时代理解析无进展而未进入测试。R.2.19 仍待用户选择事件时或重试时 Party 归属，M.2 维持 BLOCKED。

2026-10-11 Retrieval 源码对照补记：BQ `TaskRetrieval` 的非消耗活动库存扫描、`groupDetect` 分量重置、需求顺序和 `TaskOptionalRetrieval.ignored()` 与 Base 快照 evaluator/既有测试合同吻合；没有新发现当前 S2 参数所触发的求值器差异。本轮是静态源码审计，未重跑 Retrieval 测试。对固定 BQ `TileObservationStation.updateEntity` 再核实：服务端每 20 tick 仅在站点 owner 在线时扫描六邻的 `IInventory`、`IFluidTank`、`IFluidHandler`；它以 owner 的 `ParticipantInfo.ALL_UUIDS` 调用 `TaskRetrieval.retrieveItems`、`TaskFluid.retrieveItems/retrieveFluids`，而这些路径对 consuming 或已完成任务立即返回，所以观察站只会贡献非消耗任务存在检测，不会扣除邻接容器内容。Base 仍没有对应世界入口；DefaultQuests 不记录玩家是否放置/使用观察站，不能据此推断 S2 依赖。按既有范围决定，该辅助入口继续延期；`fullySyncQuests` 实际配置仍未知，Retrieval/M.2 维持 BLOCKED。

2026-10-11 配置口径更正：本矩阵较早段落中的“`fullySyncQuests` 实际配置未知”指运行时进程是否加载/覆盖尚未验证；S2 磁盘 `questbook.cfg` 已只读检查为 false。固定 BQ 源码虽在注释标记 `[NYI]`，但 `QBConfig.init` 仍读取该选项，Checkbox、QuestInstance、Retrieval 和 Fluid 均有直接使用分支。

2026-10-11 Crafting PARTY 统计故障路径复核更正：Party membership 查询异常会使当次匹配产物事件的在线成员枚举退化为触发玩家，但不能据此推断在线 peer 的个人 Crafting 统计永久漏记。`QuestGameplayEventHandler.onPlayerTick` 每 60 tick 会对每名在线玩家调用自动统计检测；导入 BQ Crafting Task 由 `QuestObservationPlan` 纳入目标，`AuthenticatedCraftingStatisticsDetectionService` 以该玩家自己的 vanilla 统计生成个人事实，并由 Planner 按检测时当前 Party/活动资格建立其 PLAYER 进度请求。因此，membership/数据库恢复后仍在线且仍符合资格的 peer 可由自己的后续周期采样补齐；这是 Base 的自动轮询，不是事件时 roster 重试，也不保证玩家离线、离队或资格变化后仍能追补。R.2.19 的产物事件重试归属问题独立保留，不能通过这条周期采样声称已解决。

R.2.19 可行性复核补充：普通 `GameplayFact` 不含事件时 Party scope；Planner 重试时用 JDBC membership resolver 重新解析，并用 JDBC 进度/完成及当前在线目录判定活动 Quest。只冻结 Party UUID 或 roster 仍不够。事件时 PostgreSQL 故障时当前组合根没有新鲜权威 membership/活动资格缓存；若引入缓存，必须定义新鲜度、无可信快照时的失败/诊断和旧成员迟到投影。重试队列仅在内存，服务进程崩溃仍会丢失，因此不能承诺跨崩溃完整等价。用户已收到“事件时 Party（推荐）/重试时当前 Party”选择请求，答案前不改变进度归属。

2026-10-11 Crafting Party 语义细分：固定 BQ `getSharedQuests()` 以本服在线 `ACTIVE_PLAYERS` 的活动 Quest 并集决定事件资格；通过资格后，`TaskCrafting.onItemInternal` 将事件进度写给全体 `ALL_UUIDS`，离线成员也包括在内；绝对统计只读取在线成员自己的 `objectCraftStats`。Base 的 `BqPartyActiveQuestQuery` 同样以至少一名本服在线且 Quest 可活动成员作门控，`BqHuntPartyAssignmentPolicy` 在事实处理时向当前完整 Party roster 的 PLAYER 进度投影匹配事件；统计快照单独按在线成员读取。因而成功即时处理时，在线资格和全 roster 事件 fan-out 已在实现中分层；剩余差异是队列延迟期间 Party roster/资格按重试时重新解析，而 BQ 在事件回调时同步处理。这个差异需结合 R.2.19 的快照新鲜度、故障失败和进程崩溃合同决策，不属于“离线成员绝对统计”问题。

2026-10-09 [需求复盘与后续评估](betterquesting-requirements-reassessment-2026-10-09.md)：独立 Base/数据库/终端目标不变，不改成 BQ Mod 存储替换方案；当前阶段仍 M.2。自动回归不代表 Forge/GTNH 行为或整体兼容完成。R.2.6 已确认不同 Party 成员的 Crafting 统计分别进入各自个人进度；各服发布本服在线成员快照；同一玩家多服统计按来源服最大值后求和，已实现并通过隔离 JDBC 往返测试。Quest 级消耗 Detect 已实现自动测试覆盖，但批次迁移尚未应用到生产环境。R.2.7 的 block_break 与 R.2.8 的 Hunt 均有合成事实/隔离 JDBC 自动回归，但不调用真实 Forge 事件。真实 GTNH 事件时序及各类型运行行为仍待验收。

详见 [进度与代码质量收口](betterquesting-progress-quality-review-2026-10-02.md)。代码实现、历史自动验收、当前验证和生产切换必须分开表述。

</details>

### R.2.17：Quest 级消耗 Detect

| 子批次 | 状态 | 当前边界 |
|---|---|---|
| Quest 级批次协调、稳定幂等键、游标恢复、批次末 Quest 结算 | 自动验收完成；版本发布并发恢复边界待定；Forge/GTNH 实机仍待验证 | UI 只提交 Quest ID；当前仅支持 PLAYER scope 的 consuming Retrieval、Optional Retrieval、Fluid 子集。S2 DefaultQuests 的 308 个消耗 Task occurrence（307 Retrieval、1 Fluid）均为 `autoConsume=false` 且没有显式 `isGlobal=true`；S2 磁盘 `questbook.cfg` 为 `fullySyncQuests=false`，与固定 BQ PLAYER 默认分支相符，但不证明服务器进程已加载该文件。操作按定义顺序逐 Task 使用现有消费 Saga；中断可从源服游标恢复，跨服切换时等待源服，不跨服访问玩家背包。OR/Quest 完成与奖励在批次末收束。PG16 隔离回归现验证 3 项有序 Task key 经 journal round-trip 后顺序不变，重复入口不能替换原列表，cursor 可幂等地逐项推进至完成；Core 服务测试另覆盖按 Quest 定义顺序处理。CodeGraph 复查确认入口仅按当前 published version 查活动批次，续跑仍通过当前定义授权且最终结算要求版本相同；活动唯一索引也包含 definition_version。因此新版本发布后旧批次无法恢复/结算，且当前版本可能启动并行消耗批次。仍待用户选择：阻止新版本发布直至活动批次结束，或允许发布但固定旧定义/旧奖励续跑并阻止跨版本并行。该并发场景尚无回归。生产迁移 `20261010_002_add_quest_consumption_detect_batch.sql` 未执行。 |

2026-10-11 自动验收：Core 278、Quest Postgres 58、UI2 Terminal 28、根项目 591，共 955 项测试；52 项根项目可选 PostgreSQL 集成因各自独立数据库配置缺失而 assumption-skip，0 failure/error，实际执行 903 项。Quest Postgres 58 项均在临时隔离 PostgreSQL 16 上执行。`:compileJava :assemble verifyUi2SelfContained` 成功。该证据只关闭本批自动门槛，不解除 M.2 BLOCKED。

### R.2.20：Crafting 统计超目标值保留

| 子批次 | 状态 | 当前边界 |
|---|---|---|
| `TaskProgress` 来源服统计聚合保留超过 Task 目标的绝对计数 | 自动回归完成；Forge/GTNH 统计读取待验证 | 固定 BQ `onItemInternal()` 将单服事件进度封顶于目标；`TaskCrafting.detect()` 则可将更大的绝对统计值交给不截断的 `setUserProgress()`。Base 对每来源事件累计封顶、统计观察值不封顶，仍按来源内 max、来源间求和，因此总值可超过 target，完成判断仍是 `value >= target`。Core 全套 281 项（含事件封顶新增回归）、隔离 PostgreSQL Crafting/JDBC 定向 1 项通过；`:assemble` 与 `verifyUi2SelfContained` 通过。R.2.19 Party 事实重试归属未决，Crafting/M.2 仍 BLOCKED。 |

### R.2.22：Crafting 来源事件账本按重复周期隔离

| 子批次 | 状态 | 当前边界 |
|---|---|---|
| 新周期清空旧来源 ledger；多来源 max/sum 可持久化往返 | Core + 隔离 JDBC 自动回归完成；Forge 仍待验证 | 固定 BQ `QuestInstance.resetUser(uuid, false)` 会重置各 Task 的玩家进度。Core 回归验证旧周期 S2 来源事件完成后，新周期只保留新的 Lobby 事件；隔离 PostgreSQL 回归验证 cycle 1 upsert/reload 后 `source_progress_json` 不含旧 S2 ledger，并用两个来源证明单来源 event/absolute 取最大值、跨来源求和在 JSONB 重读后不变。Core 全套 283 项、PostgreSQL 定向集成 2 项通过，均 0 跳过/失败/错误；无生产代码/schema 修改。R.2.19 和 M.2 仍 BLOCKED。 |

### R.2.23：无关 Crafting 产物不触发 PARTY 活动资格检查

| 子批次 | 状态 | 当前边界 |
|---|---|---|
| PARTY Crafting scope 使用与 Task evaluator 相同的精确通道/物品匹配 | Core 自动回归完成；R.2.19 归属和 Forge 运行仍 BLOCKED | 之前只检查 Task 是否开启 craft/smelt/anvil 通道，未检查产物是否命中需求，会让无关输出进入 PARTY 在线活动资格检查。现复用相同 matcher（meta/damageable、OreDictionary、NBT、partial-match）；未命中输出不再 PARTY fan-out，个人 evaluator 的 no-op 语义保持。CodeGraph impact 仅覆盖 matcher 与 Party scope 三层；`affected` 给出 56 个宽候选，按直接链运行 Core 全套：284 项、0 skipped/failure/error。第一次输出落在历史 root-owned `build` 并遇权限拒绝；隔离运行的新测试首轮暴露一条错误断言并已修正，最终全套通过。无 PG/Forge/根打包运行；R.2.19 仍需用户确认事件时或重试时 Party 归属。 |

### R.2.24：QuestCompletion 缺失目标按 BQ no-op 收束

| 子批次 | 状态 | 当前边界 |
|---|---|---|
| 目标 Quest 不存在时不生成永久恢复失败 | Core 自动回归、根编译/打包/自包含通过；真实奖励运行仍待验证 | S2 `DefaultQuests/Quests` 只读解析 3,744 个定义、308 条 QuestCompletion 奖励、251 个不同目标：219 个可解析、32 个缺失，malformed/missing UUID 为 0。固定 BQ `RewardQuestCompletion.claimReward0` 对缺失目标静默 no-op；Base handler 现在将 `TARGET_NOT_FOUND` 映射为 delivered marker，格式错误 UUID 和目标 scope 不可用仍失败。Core 286 项、0 skip/failure/error；`compileJava assemble verifyUi2SelfContained` 通过。只读取 Quests 定义；没有玩家进度、PostgreSQL、生产 DDL 或部署。Forge/GTNH 的实际奖励投递、玩家存档和重启窗口未验证，M.2 仍 BLOCKED。 |

### R.2.25：Crafting 周期自动复评的单目标失败隔离与重试回归

| 子批次 | 状态 | 当前边界 |
|---|---|---|
| 一次自动目标故障不阻塞同批任务，并在下一轮重新尝试 | 根事件分发回归完成；服务端运行仍待验证 | 根测试模拟导入 Crafting 目标首轮抛出瞬时数据库异常，确认同批后续目标继续、下一次周期调用重试成功，项目自有 Task 不参与 BQ 统计轮询。隔离 Docker 定向类 4 项、0 skipped/failure/error；仅测直接周期检测入口，未覆盖 Forge 60-tick 调度或真实 PostgreSQL 恢复时序，不改变生产代码。Crafting/M.2 仍 BLOCKED。 |

### R.2.27：Optional Retrieval 导入与 Quest 完成链

| 子批次 | 状态 | 当前边界 |
|---|---|---|
| `optional_retrieval` 类型映射、库存进度更新与 Quest 忽略规则的跨模块回归 | Importer 定向自动验收完成；Forge/GTNH 仍 BLOCKED | BQ `TaskOptionalRetrieval.ignored()` 恒为 true；Base importer 保留 `bq_standard:optional_retrieval` 类型并映射 `TaskDefinition.optional=true`，Core QuestEngine 将其排除于必需完成计数。新用例经 typed JSON importer→`InventorySnapshotTaskEvaluator`→`QuestEngine`，验证必需 Checkbox 完成时即使 optional Retrieval 仅部分满足，Quest 仍完成且可选任务不被伪造完成。Importer 定向 5 项、0 skipped/failure/error；没有生产代码变化、数据库或 Forge 运行证据，M.2 仍 BLOCKED。 |

### R.2.28：未知 Task ID 不得推导 optional 语义

| 子批次 | 状态 | 当前边界 |
|---|---|---|
| 仅为已知 BQ `bq_standard:optional_retrieval` 指定 optional；未知 ID 默认必需 | Importer 全模块自动回归完成；未改运行时或数据库 | 原 importer 用 `type.contains("optional")` 推导 `TaskDefinition.optional`，导致未支持类型 `addon:optional_special` 被错误排除于 Quest 必需任务计数；尽管兼容报告仍将其标记为 `UNKNOWN`，仍存在降低完成门槛的风险。现在 `BqStandardCodec.isOptionalTaskType` 仅精确识别已知 BQ Optional Retrieval；新增导入回归确认未知类型保持 `optional=false` 且无任务进度时 Quest 仍为 AVAILABLE，兼容报告仍输出 UNKNOWN。先运行旧逻辑回归失败，再修改后全模块 `:galaxy-quest-bq-importer:test` 通过 51 项，其中 44 执行、7 项因未配置隔离 `QUEST_TEST_POSTGRES_URL` 按 assumption 跳过，0 failure/error。无 PostgreSQL、Forge、assemble 或生产改动证据；M.2 仍 BLOCKED。 |

### R.2.29：重复周期物品奖励的 marker 与延迟恢复隔离

| 旧周期物品奖励部分投递后，新周期权益先完成，压缩玩家 NBT 重建后再恢复旧权益 | 根测试自动回归完成；World/Forge 与跨服存档边界仍 BLOCKED | 测试用真实 `RepeatPolicy`/`RewardEntitlement` 生成 cycle 0/1 稳定 entitlement key；cycle 0 将 64 个物品写入背包后模拟 overflow drop 失败并保留进度，通过 Minecraft `CompressedStreamTools` 与实际 Inventory/ItemStack NBT 序列化重建 PlayerState；cycle 1 先完成，随后 cycle 0 延迟恢复。断言两周期 marker/partial progress 独立，奖励总数守恒且不因旧周期 marker 完成而重复发放。CodeGraph `impact` 覆盖 item delivery 和 entitlement key，`affected` 收敛到根奖励测试；改后 Docker 根测试 34 项（Minecraft reward 29、BQ import delivery 5），0 skipped/failure/error。首轮失败来自测试夹具复用奖励物品填充背包、断言误计；改用独立 filler 物品后通过。全量测试、assemble/self-contained、真实 Forge 世界掉落保存及跨服租约没有运行/验证；World drop 已创建而玩家/NBT marker 尚未落盘的崩溃窗口仍未解决，M.2 保持 BLOCKED。 |

### M.2 行为子批次 R.2.30 状态

| 工作包 | 状态 | 证据/剩余边界 |
|---|---|---|
| 多个 consuming Retrieval 的 Quest 级 Detect 顺序、Task 间故障恢复与批次末结算 | 隔离 PostgreSQL 自动回归完成；Forge/GTNH 仍 BLOCKED | typed BQ JSON 导入两个按序 Retrieval，在首项消费后于第二项应用前中断；验证持久游标、稳定 submission key、首项不重复消费、OR Quest 不提前结算及恢复后两项完成。新增测试和完整根 JDBC 集成类合计 16 项全部通过，0 skipped/failure/error；测试只使用合成 ItemStack/adapter 与 tmpfs PostgreSQL，不修改生产代码/DDL。PlayerData 崩溃窗口、多服切换/恢复、运行期 `fullySyncQuests` 仍未验证，M.2 保持 BLOCKED。 |

### M.3 进度迁移源码审计（2026-10-10；仅诊断，不是迁移实现）

- `BqPlayerProgressImporter` 目前只读单个玩家 UUID JSON，`BqProgressMapper` 只映射 Task 向量；两者没有 PostgreSQL 写入、批次、事务回滚或生产进度读取调用链。现有 CLI 仅做预览/分析。
- 固定 BQ `QuestInstance` 源码以 `completeUsers` 和 `userProgress` 保存每玩家状态；`resetUser(uuid, false)` 会把旧完成记录的 claimed 复位、timestamp 清零并重置 Task 向量。该格式没有可单调比较的 repeat-cycle ordinal，因此不能把“向量最大”或“时间戳最新”直接当成重复 Quest 的当前周期。
- `BqProgressConflictAnalyzer` 以前只比较完成/领取标记和 Task 向量，会把状态相同但 BQ timestamp 不同的快照判断为 `IDENTICAL`。现已保守地把 timestamp 不同判为 `DIVERGENT`，并新增回归；它是诊断分类修正，不代表已经确定所有 Quest 类型的合并策略。
- 重复 Quest 的不同快照如何处理仍待产品确认。建议无可靠周期身份时将非完全一致状态隔离供审计，不猜测来源优先级、不自动拼接向量；完全一致快照也只能证明观测值相同，不能证明来源文件处于同一周期。M.3 写入器与生产数据仍未开始，且 M.2 行为兼容门禁继续阻断正式进度迁移。

### M.2 可靠性子批次 R.1 状态

| 工作包 | 状态 | 证据/剩余边界 |
|---|---|---|
| R1-01 测试数据库隔离 | 自动验收完成 | Postgres、Importer 与根集成测试要求显式隔离标记和 `_test`/`_it` 库；在无数据卷的临时 PostgreSQL 上实际通过。缺少 URL 时条件跳过，显式配置错误则失败，不回退 `galaxy-base`。 |
| R1-02 周期复评异常边界 | 自动验收完成 | 锁定进度复评的数据库 RuntimeException 被事件边界捕获并记录；回归证明异常不向 tick 调用者传播，后续既有周期可再次尝试。 |
| R1-03 一次性事实重试 | 自动验收完成（有限可靠性） | 主线程 FIFO 内存队列，限 1,024 条与估算 8 MiB；每 20 个 END tick 最多重试 32 条，保留原 GameplayFact/幂等键及登录会话投递操作。满载拒绝最新事实；关服记录后清空；进程崩溃不能恢复，提交前崩溃仍可能丢失。 |
| R1-04 背包候选规划与缓存 | 自动验收完成 | 单行 PostgreSQL revision 使不可变发布目录仅在定义变化时重载；按事实类型/目标 Quest/前置 Quest 建索引。2,501 定义固定规模测试只选中 1 个事实候选；成员、前置完成、活动资格及周期仍逐事实判定。新增 `20261009_001` 迁移尚未应用生产。 |
| R1-05 可靠性总收口 | 自动验收完成（仅 R.1） | R1-01～04 定向及隔离 PostgreSQL 测试、全量 `test`、`assemble`、自包含检查均有证据；全量运行有 56 个环境假设跳过，Quest JDBC 场景已单独隔离实跑。该状态不解除 M.2 BLOCKED。 |

本轮 Docker Gradle 隔离定向运行 Core 250、Postgres 52、Importer 46，共 348 项，0 failure/error/skipped；根 PARTY 快照 JDBC 另 4 项在隔离临时 PostgreSQL 上通过。全量 `test` XML 汇总 991 项：935 执行、56 assumption-skip、0 failure/error；跳过原因是未提供其它业务 PostgreSQL 配置（48）、本次全量运行未配置 Quest 隔离库（4，已单独实跑）、Warehouse 专用 `JGB_BANKING_IT_*` 配置缺失（4）。`assemble`、`verifyUi2SelfContained`、只读字体挂载后的全量 UI2 Demo 测试通过；`git diff --check` 记录最终检查结果。临时 Quest PostgreSQL 容器仅使用新建无卷测试库，未访问生产数据库。以上只关闭 R.1：Task/Reward 语义等价、玩家进度迁移、实时影子和生产切换仍未完成，M.2 与 BQ 替代继续 `BLOCKED`。

### M.2 行为子批次 R.2.1 状态

| 工作包 | 状态 | 证据/剩余边界 |
|---|---|---|
| R2.1 Retrieval 匹配与分配顺序 | 自动验收完成（仅离线算法回归） | 固定 BQ 检索源码已对照；新增重叠 NBT 需求竞争时按背包首见槽位分配的回归。Core 251、Importer 定向 9、根消费/导入适配 19 项，0 跳过/失败/错误；assemble、自包含检查通过。没有生产行为改写；SubmitStation/背包提交入口差异及 Forge/GTNH 实机仍未签收。 |

### M.2 行为子批次 R.2.2 状态

| 工作包 | 状态 | 证据/剩余边界 |
|---|---|---|
| R2.2 导入、库存事实、JDBC 进度与消费恢复 | 自动验收完成（合成/隔离集成闭环） | 新增两条根 PostgreSQL 集成链：导入 BQ 非消耗 Retrieval → PARTY 背包观察 → `QuestRuntimeService` / JDBC 逐成员进度与完成；导入 consuming Retrieval → 授权 / `MinecraftPlayerConsumptionPort.planItems` → 持久化 `APPLIED` → 故障后 `resume` → JDBC 完成且不重复扣物品。Core 251、Postgres 52、Importer 46、根 Quest 相关 25，共 374 项，0 跳过/失败/错误；`assemble`、自包含检查通过。仅用绑定 loopback 的 tmpfs 临时 PostgreSQL 与合成 Item/背包，不改生产行为；真实 EntityPlayer、Forge/GTNH、多服时序、SubmitStation 与奖励物理投递仍未验证。 |

M.2 与正式替代仍 `BLOCKED`。R2.4 补 consuming Fluid 的 importer→PLAYER 手动授权→容器规划→JDBC 进度/恢复回归；R2.5 补非消耗 Fluid 绝对快照、`groupDetect` 与 PARTY 新鲜租约汇总/逐成员进度；R2.18 已修正不可分割 Fluid 容器的实际排出量超目标保留。真实 Forge/GTNH 容器注册与运行背包仍未验收。后续工作按开发导航和固定 BQ 源码证据挑选，不把自动集成测试当作 Forge/GTNH 等价或生产切换证据。

### M.2 行为子批次 R.2.3 状态

| 工作包 | 状态 | 证据/剩余边界 |
|---|---|---|
| R2.3 手动 Retrieval 提交拆分与投影 | 自动验收完成（合成/隔离集成回归） | BQ `detect` 全库存扫描与 `submitItem` 单栈输入分别对照 Base 终端两种明确提交动作；新增隔离 JDBC 用例验证导入的重叠 NBT 需求按定义顺序将 7 件栈投影为 `[4,3]`，第二次只补剩余 1 件并完成，未选中栈不动，重放不重复消费。根 Party/JDBC 类 7 项、Core 提交服务 5 项、根消费适配器 19 项，共 31 项，0 跳过/失败/错误；`assemble`、自包含通过。仅合成 Item/Inventory 替身；SubmitStation 实体/10 tick 节奏、GTNH 实物、多服与崩溃恢复未验证。 |

### M.2 行为子批次 R.2.4 状态

| 工作包 | 状态 | 证据/剩余边界 |
|---|---|---|
| R2.4 consuming Fluid 手动提交闭环 | 自动验收完成（合成容器/隔离 JDBC 恢复链；超量语义由 R.2.18 修正） | 原 R.2.4 回归证明 400 mB 可分割提交在玩家 marker 已写、数据库仍 PREPARED 时恢复且不重复消费；当时旧实现将不可分割 750 mB 容器的实际排出量按剩余 600 mB 封顶。R.2.18 已改为完整记录实际排出量，当前回归为 400+750=1,150/1,000，重放不重扣。原批 Core 251、Postgres 52、Importer 46、根端口 19、根集成类 8 的数字是历史验收；R.2.18 最新全量及定向统计见 Worklog。模拟容器/玩家 marker 非真实 Forge 注册容器、玩家存档或 GTNH 行为签收；无空容器槽时 Base 拒绝仍不同于 BQ 库存检测的世界掉落。 |

### M.2 行为子批次 R.2.5 状态

| 工作包 | 状态 | 证据/剩余边界 |
|---|---|---|
| R2.5 非消耗 Fluid 快照与 PARTY 兼容 | 自动验收完成（Core + 隔离 PostgreSQL） | 对照 `TaskFluid.detect`：每个需求独立扫描未排出的库存，重叠需求可复用同一观察量；`groupDetect=false` 保留单项完成并重算未完成项，true 重算全部，任务完成后锁存。将 Fluid-only 观察纳入既有 PARTY lease；同一租约保留当前 Item/Fluid 属性，汇总仅使用新鲜成员快照，再向 PARTY 成员分别写入 PLAYER 进度。修复 `QuestEngine` 将流体绝对观察误按单调事件合并的问题；consuming Fluid 不变。隔离 JDBC 用例覆盖 900→700 mB 回落、过期成员排除、进度扇出和完成；Core 257、Postgres 52、Importer 46、根 PARTY/JDBC 类 9，共 364 项，0 skipped/failure/error；assemble、自包含和 diff 检查通过。真实 GTNH 流体容器、跨服在线资格/时序仍未验收，M.2 保持 BLOCKED。 |

### Crafting 多服统计归并注意事项

固定 BQ `TaskCrafting` 先按匹配产物事件增量更新本服玩家向量，再以本服 `objectCraftStats` 绝对计数校正。用户已确认跨服归并合同：对同一玩家按来源分别取 `max(本服事件累计, 本服绝对统计快照)`，再对来源求和；不同 Party 成员仍写入彼此独立的 PLAYER 进度。Base 使用来源进度 JSONB 记录每个来源，不能把绝对快照直接累加到同一来源，也不能把事件计数重复叠加到该来源快照。固定源码：[TaskCrafting.java](https://github.com/GTNewHorizons/BetterQuesting/blob/524b365211b6b3a9672cab8ae45b4e07e726d49e/src/main/java/bq_standard/tasks/TaskCrafting.java)。

### M.2 行为子批次 R.2.6 状态

早期实现曾以通用 Task 向量的全局 max 归并，并用 `s2=5,lobby=2 → 5` 的测试固定该临时行为；该实现和测试现已由来源维度方案取代，不再代表当前规则。当前实现将每来源的事件累计与绝对统计快照分别合并后再求和，并将旧聚合数据作为非叠加 legacy baseline；用户合同及后续 JDBC 往返/迁移回归见上方 R2.6 行与 Worklog 2026-10-10/11。

计数更正：下方早期 R2.6 记录的 289 项为首次复验；同日后续补齐 LOCKED→解锁 JDBC 用例后的最新完整复验为 290 项（Core 270 + 根分发/故障 5 + PostgreSQL 集成 15），见紧邻下方记录。旧数仅保留为历史，不代表当前最新验收总数。

| R2.6 Crafting 统计检测与 PARTY 在线资格 | 用户合同已确认；匹配产物即时刷新、Base 60 tick 自动复评、个人统计归属、Party 活跃资格、锁定完成后的解锁复评、来源服计数归并及隔离 JDBC 幂等回归完成；真实 Forge/GTNH 时序待验证 | 每服只读取本服在线 Party 成员各自的 `objectCraftStats` 并产生独立 PLAYER 事实，不合计不同 Party 成员的计数；同一玩家跨服按来源分别取 `max(该来源事件累计, 该来源绝对快照)` 后求和。来源贡献与 legacy baseline 分开保存；JSONB 往返与旧进度非叠加升级有自动回归，迁移脚本尚未执行。匹配产物刷新同一 Crafting Task 的统计目标，无关输出不触发；Base 另按用户选择每 60 tick 自动复评启用统计的导入 Task，不展示手动“检测合成”按钮。固定 BQ 的 60 tick `QuestInstance.update()` 只检查完成/同时任务状态，Crafting 检测由匹配产物事件或 Quest 级 Detect 调用；Base 将 Quest 级 Detect 用于消耗任务批次。组合 Core 回归现明确证明：在线且有资格的 Party peer 可激活事件，随后匹配产物仍向全 roster（包括离线玩家）投影个人 PLAYER 进度；只有离线成员有资格时不触发。已完成 Task 若仍 LOCKED，满足前置后以独立稳定键复评。非消耗物品/流体靠玩家库存或已接入容器快照自动观察，机器产物不伪造 craft 事件。自动证据使用合成输入；真实 Forge 回调、`EntityPlayerMP` 统计文件、生产在线 Party 目录与多服时序仍待运行验证，因此 R2.6 自动门槛完成、Crafting/M.2 行为仍 BLOCKED。 |

R1-03 重试与 PARTY 事件归属审计补充（2026-10-11）：CodeGraph 对 `QuestGameplayEventHandler.submitCraftingStatistics`、`OrderedQuestFactRetryQueue.retry`、`RuntimeQuestFactSink.accept`、`QuestEvaluationPlanner.requestsFor`、`BqPartyTaskProgressScope.supports` 与 `BqHuntPartyAssignmentPolicy.select` 的调用链复核确认：Crafting 产物事实只保存来源玩家和事件数据；立即成功时由当前 membership fan-out，排队后则用重试时的 membership/活动 Quest。固定 BQ `EventHandler.onItemCrafted` 在回调内构造 `ParticipantInfo`，同步处理当时的 shared Quest，并把匹配产物向量写给当时 `ALL_UUIDS`。在线个人统计快照虽然在事件时固定本服在线 Party UUID 并读取其统计值，但产物事件本身没有 Party roster 或活动 Quest 快照。Party 在重试前变化会使事实丢失给原成员或错发给新成员。当前只完成源码审计，无代码/回归修复；建议对齐 BQ 事件时归属，但会改变当前 Base 重试归属合同，需用户先确认“事件发生时”还是“数据库成功接收时”作为权威。R.2.6/M.2 继续 BLOCKED。

2026-10-10 R.2.6 后续补齐个人前置锁定边界：新增 `lockedPartyCraftingStatisticsUnlockThroughPersonalStableFactInJdbc`，隔离 PostgreSQL 验证在线 Party peer 的个人统计可在 LOCKED 期间填满 Task、不提前发奖；个人前置完成后通过独立稳定事实键完成 Quest 并只生成一条奖励。最新复验 Core 270、根事件分发/故障 5、隔离 PostgreSQL 集成类 15，共 290 项实际执行、0 skipped/failure/error；`:assemble :verifyUi2SelfContained` 及 `git diff --check` 通过。临时 PostgreSQL 16 仅使用 tmpfs、无宿主卷和 `127.0.0.1:55490`，验收后已清理。此自动回归不覆盖 Forge tick/真实统计文件/生产 Party 目录；R.2.6 自动门槛完成，Crafting/M.2 等价仍 BLOCKED。

2026-10-10 对照 `EventHandler.onItemCrafted`/`TaskCrafting.onItemInternal` 又收敛合成回报边界：BQ 只在 `InventoryCrafting` 的结果 stackSize 恰为 0 时回查配方，负值不会作为合成推进。Base 原先将所有 `<= 0` 都回查，现改为仅 `== 0`，新增事件数量回退回归。合并 Crafting 兼容定向覆盖 109 项通过、0 skipped/failure/error；其中本轮 Core 87 项由 Gradle 标记 `UP-TO-DATE`（前一轮相同源码下已实际通过），根侧 22 项重新执行。`:assemble` 与 `:verifyUi2SelfContained` 通过。此项不解除真实 Forge 回调与统计读取验收，也不改变 Quest/Party 合同。

2026-10-10 对照固定 BQ 源码并按用户合同补齐 PARTY 个人统计路径。成员按本服务器在线目录逐个采样，生成个人 PLAYER 事实；两名 Party 成员的统计计数不会被相加，Crafting 事实仍按同 Task 匹配输出刷新其它统计目标。验证包括 Core 81、根分发/事件失败 4、隔离 PostgreSQL 16 个人快照集成 1，共 86 项全部执行且无跳过/失败/错误。CodeGraph 检查事件分发、观察计划、Planner/Runtime 和 PostgreSQL 集成影响链。测试使用合成玩家/事实，不证明真实 Forge/GTNH 回调、统计文件读取和生产在线目录；R2.6 自动测试门槛完成，Crafting/M.2 行为兼容保持 BLOCKED。

（历史验证，现由来源维度实现取代）此前 `automaticBqCraftingStatisticsUsePersonalMaximumAcrossServersWithoutSumming` 将 `s2=5`、`lobby=2` 合并为 5、后续 `lobby=7` 推进到 7，锁定的是当时的 TaskProgress 全局最大值实现，不是当前产品合同。来源维度实现已由隔离 PostgreSQL JSONB 往返及旧进度迁移回归覆盖；旧 290 项记录保留作历史证据。真实 Forge/GTNH 与多服在线时序仍 BLOCKED。

### M.2 行为子批次 R.2.7 状态

| 工作包 | 状态 | 证据/剩余边界 |
|---|---|---|
| R2.7 block_break 导入与 JDBC 进度闭环 | 合成定义→隔离 PostgreSQL 自动回归完成；Forge 运行时仍待验证 | 新增根集成回归从合成 BQ `block_break` JSON 导入并发布，经生产 `BqPartyInventorySnapshotFactSink`、Planner、Runtime 与 JDBC 保存/重读进度；验证同事实键重放幂等、首个匹配需求达标后仍优先截获后续同类方块、其他独立需求完成 Quest。Core evaluator 38、Importer codec 5、根集成类 11，共 54 项通过，0 skipped/failure/error。单人 membership 与合成事实不证明真实 `BreakEvent`、TileEntity NBT、OreDictionary 注册数据或 Party 在线资格；因此 block_break 和 M.2 仍 BLOCKED。 |

### M.2 行为子批次 R.2.8 状态

| R2.8 Hunt 实体注册边界与 PARTY/JDBC 回归 | 自动回归完成；Forge/GTNH 事件语义仍 BLOCKED | 对照固定 `TaskHunt.onKilledByPlayer` 修复未注册实体仍借注册父类 alias 命中、缺失目标/subject 仍计数，以及新事件进度可超过 `required` 的差异；R.2.21 让新观察按目标封顶，同时不回写压低历史超目标值。最新 Core 全套 283 项通过。已有隔离 PostgreSQL Hunt PARTY/JDBC 回归仍适用。没有真实 `LivingDeathEvent`、FakePlayer/取消事件、GTNH EntityList/NBT 或跨服时序证据，因此不提升 Hunt/M.2 状态。 |

### M.2 行为子批次 R.2.9 状态

| 工作包 | 状态 | 选择依据与验收范围 |
|---|---|---|
| R2.9 Location / Meeting 行为兼容闭环 | 自动验收完成；Forge/GTNH 与跨服时序仍 BLOCKED | 当前只读 S2 `DefaultQuests` 画像含 33 个 Location、1 个 Meeting；互动类没有实际 occurrence，不纳入本批。固定对照 BQ `TaskLocation.detect/tickTask` 与 `TaskMeeting.detect/tickTask`。Meeting 已修复最大共享半径快照下逐 Task AABB 过滤，Core 41 项与隔离 PostgreSQL PARTY 集成 1 项通过。Location 新增完整导入/持久化/运行回归：合成 BQ JSON 经 migration 发布，读取后的 `QuestObservationPlan` 启用位置观察；与 Adapter `locationObserved` 的 `dimension/x/y/z/biome` 字段一致的事实，在在线 PARTY 资格下分别写入个人 PLAYER 进度；确认原事实键幂等、未领奖时不重开、手动领奖后按 `claimed_at` 冷却并进入 cycle 1。当前 33 项画像仍只依赖 dimension，未扩展未使用的 structure/视线/biome 行为。定向验收 Core 267、Importer 47（含隔离 PG migration 测试）、Meeting PG 1，共 315 项、0 skipped/failure/error；根 `compileJava compileTestJava assemble verifyUi2SelfContained` 通过。合成事实不证明真实 Forge/GTNH 100/60 tick、实体注册/NBT 或多服 PARTY 时序。 |
| R2.10 Checkbox 认证手动确认与导入/JDBC 闭环 | 自动验收完成；BQ 运行配置/Party/GTNH 运行仍 BLOCKED | 固定源码显示 `TaskCheckbox.detect` 为空，`NetTaskCheckbox` 只接受认证发送者；`fullySyncQuests` 默认 false 且注释标记 NYI，但配置仍被读取和使用。S2 磁盘 `questbook.cfg` 为 false，运行时是否加载/覆盖未验证。新增集成测试从 BQ Checkbox JSON 经 importer 发布，到认证服务、Planner、Runtime 与隔离 PostgreSQL：仅点击者 PLAYER 进度完成，另一玩家无进度，重复确认不重复生效，观察计划不启用背包/流体轮询。`Core 267 + Importer 48 = 315`，0 skipped/failure/error；assemble、自包含门禁通过。Base 仍不支持 Checkbox 与 PARTY/TEAM/PUBLIC 主体混合；未验证真实终端/Forge 交互。 |

## 当前 Base-only 新服口径（2026-10-11）

当前目标不再是把旧服 BQ 玩家状态搬入 Base，而是让无 BQ Mod 的新服务器从空进度启动。导入只处理 3,744 个 Quest、47 章及其版本化内容；所有导入 Quest 显式为 PLAYER，Task 带个人模式标记，玩家加入 Party 也不得聚合库存或扇出事实。旧玩家进度/领取/Choice、旧 BQ Party 与实时影子双算从活动路线删除，历史工具和审计记录继续保留但不构成待办。

当前活动门槛只有：真实 9 类 Task/4 类 Reward 的运行发布门禁；完整任务书在隔离空库的幂等版本化导入；全新合成玩家的进度/奖励严格一次闭环；玩家终端与管理员只读健康摘要；最后的全量构建、自包含和视觉收口。生产 DDL、部署、开关与 Base-only 新服人工验收仍在本工作包之外。

## 稳定检查点结论（历史基线）

本检查点已经具备 Quest 定义、编辑、检测、奖励、PostgreSQL 基础、玩家任务中心、管理员编辑入口，
以及 BetterQuesting 定义/玩家进度的只读导入能力。BQ/Base 影子差异目前仅是离线分析基础，尚未接入
管理员终端或实时双算。

稳定检查点之后已经完成 PARTY/TEAM/PUBLIC 自动验收：PostgreSQL 主体/成员关系、服务端认证管理、共享进度
选择、按完成周期冻结当前成员为奖励接收者、离线延期交付以及 UI2 主体管理均已形成代码和自动测试。
离队玩家的冻结奖励会脱离当前成员关系继续出现在终端；领取以“原进度主体＋任务版本＋周期＋认证收件人”
精确定位，同一玩家在两个主体拥有相同任务周期时不会串领。因此 BetterQuesting 仍未被替代。这里原有的旧进度迁移、旧 Party 映射和实时影子待办已退出当前路线；现行待办仅以上方 Base-only 口径和[实施路线](betterquesting-next-stages-2026-10-11.md)为准。

历史 Worklog 中各阶段的“下一步”只代表当时状态；后续工作顺序和完成判断以本矩阵为准。

## 目标

Base 最终独立承担 GTNH 任务定义、编辑、检测、进度、完成、奖励和管理能力，并用 PostgreSQL 作为跨服
权威真源、银河终端作为唯一正式入口。BetterQuesting 是成熟行为与兼容语义的只读参考，不是运行时依赖。

“整体吸收”表示覆盖玩家实际依赖的重要能力和 GTNH 现有任务库所使用的语义；不表示复制 BQ 的 GUI、
Packet、客户端 `QuestDatabase.INSTANCE`、JSON 存档、Mod 生命周期或全局 Hook。

## 源码基线与许可

- 现网版本：BetterQuesting 3.7.15-GTNH。
- 行为兼容固定参考提交：[`524b365211b6b3a9672cab8ae45b4e07e726d49e`](https://github.com/GTNewHorizons/BetterQuesting/tree/524b365211b6b3a9672cab8ae45b4e07e726d49e)（2025-10-19）。本次入口语义核对使用该提交的 [`GuiQuest`](https://github.com/GTNewHorizons/BetterQuesting/blob/524b365211b6b3a9672cab8ae45b4e07e726d49e/src/main/java/betterquesting/client/gui2/GuiQuest.java)、[`QuestInstance`](https://github.com/GTNewHorizons/BetterQuesting/blob/524b365211b6b3a9672cab8ae45b4e07e726d49e/src/main/java/betterquesting/questing/QuestInstance.java) 与 [`TaskRetrieval`](https://github.com/GTNewHorizons/BetterQuesting/blob/524b365211b6b3a9672cab8ae45b4e07e726d49e/src/main/java/bq_standard/tasks/TaskRetrieval.java)。
- 本地只读源码目录 `Reference/BetterQuesting` 被 Git 忽略且没有独立 `.git` 元数据；此前记录的本地镜像 HEAD `5ff4864bb072c866ac8c527495c76d3d77e4be2f` 当前无法从该目录复核，不能据此证明本地文件属于固定提交或含有该提交。它只作为便捷浏览副本；可复现行为基线以固定上游提交的文件永久链接为准，引用本地源码时必须先核验版本。
- BQ 主体为 MIT。Base 优先根据可观察行为重写；实际派生代码才添加来源头与第三方声明。

## 架构边界

| 能力 | 吸收方式 | 明确排除 |
|---|---|---|
| 定义与注册表 | Java 8 不可变定义、服务端类型目录、未知类型占位 | BQ 全局数据库和反射式客户端对象图 |
| 编辑 | 白名单语义命令、服务端授权、完整校验、乐观锁、单事务 | 客户端拼装 NBT 后整库覆盖 |
| 检测 | Forge 事件转稳定 Fact，PostgreSQL 幂等归约 | 遍历客户端数据库并直接改进度 |
| 进度与队伍 | PLAYER/PARTY/TEAM/PUBLIC 主体及数据库事务 | 每服 JSON 玩家文件 |
| 奖励 | 先持久化资格，再租约交付和恢复 | 先发奖励再写 claimed |
| 玩家入口 | UI2 终端、服务端分页、只传意图 | 任务书、BQ GUI、BQ Packet |

## 功能矩阵

### A. 任务定义与运行语义

| BQ 能力 | Base 状态 | 后续门禁 |
|---|---|---|
| UUID、名称、说明、图标、前置 AND/OR、Task AND/OR | 已完成 | 发布图校验持续覆盖 |
| 可见性、主线、静默、simultaneous | 实现存在；simultaneous 转换边界有 Core 自动回归 | 当前 S2 定义 0/3,744 启用 simultaneous；影子结果对照未完成，M.2 仍 BLOCKED |
| lockedProgress 玩家级被动解锁补偿 | 在线 PLAYER 每 60 tick 扫描已持久化 LOCKED 进度，并在事务内复评 | PARTY、离线旧进度、周期/领奖时序及 BQ 影子对照 |
| 重复、相对/绝对冷却、自动领取 | 已完成 | 跨服时钟与周期对照 |
| global/globalShare | 字段可导入、编辑和持久化；BQ 全局完成/领奖语义尚未实现 | 当前 S2 快照 `isGlobal=true` 为 0；仍不得宣称通用 BQ 语义已兼容 |
| 更新/完成音效 | 定义已保留 | Minecraft 播放适配仍需核对 |

### B. Standard Task 检测

| 类型 | 定义/旧进度 | 事实与求值 | Forge 接线 | 状态 |
|---|---:|---:|---:|---|
| retrieval / optional_retrieval | 是 | 是 | 库存观察 | 已接但待影子对照 |
| fluid | 是 | 是 | 流体容器观察 | 已接但待模组容器矩阵 |
| crafting | 是 | 是 | craft/smelt/anvil | 已接入运行时；统计量模式已补，待影子对照 |
| hunt | 是 | 是 | LivingDeathEvent | 已接 |
| block_break | 是 | 是 | BreakEvent | 已接 |
| location | 是 | 是 | 低频位置观察 | 已接 |
| meeting | 是 | 是 | 邻近实体观察 | 已接 |
| interact_item / interact_entity | 是 | 是 | 点击/攻击事件 | 已接；右键空气与未注册实体过滤边界已回归，待影子对照 |
| scoreboard | 是 | 是 | 低频记分板观察 | 已接；需避免无界创建目标 |
| xp | 是 | 是 | XP 观察与消费 Saga | 已接 |
| checkbox | 是 | 是 | 认证手工动作 | 已接 |

检测阶段的当前优先级是：背包检测到提交、完成和领奖的可靠性与性能整改，随后推进流体/NBT 和队伍兼容矩阵。观察站邻接库存延后，不作为背包主链必须先实现的入口；实时影子仍属于后续 M.5。
事件入口现已收紧事件取消/FakePlayer 边界；库存与流体观察现先按匹配维度去重后再进入 evaluator，
避免同一玩家背包的相同堆叠产生冗余事实；只有影子对照通过后才从 `ADAPTED` 升为
`SUPPORTED`。

运行时类型目录已建立：`QuestRuntimeTypeRegistry` 以标准 BQ evaluator/editor 描述为基线，允许 Base
组合根通过 `QuestRuntimeTypeExtension` 显式贡献成对的 evaluator 与编辑器描述；重复扩展、重复类型和空
描述在启动组合阶段拒绝。QuestModule、运行时求值、草稿编辑和章节复制共享同一目录，未注册类型仍会在
服务端拒绝，不会被当作可运行任务。当前没有外部扩展提供者，默认目录保持纯 Base、自包含；后续接入 GTNH
专用类型时必须由组合根显式注册并补齐事实采集与影子对照。

### C. Reward 与交付

物品、XP、命令、计分板、任务完成和 choice 的定义与交付合同均已建立；choice 已由服务端派生有界选项，
领取使用认证玩家和稳定 entitlement。PARTY/TEAM/PUBLIC 完成时按该周期的当前成员生成逐玩家 entitlement；
交付租约只携带具体接收玩家，绝不把组主体 UUID 或触发事实的玩家当作默认接收者。离队不会撤销已经冻结的
资格，后来加入者不会补领旧周期；重复任务的新周期重新冻结当时成员。离线仅延期，不消耗真实失败次数。
终端按认证收件人读取最早尚可领取批次，不依赖当前成员关系；领取再绑定原进度主体，避免不同主体的同周期
奖励被一次操作合并。Choice 同样按该接收者最早可领取批次选择，选择结果不会跨周期复用。

命令奖励默认全拒绝，只允许运维明确配置且能够使用 entitlement key 自身保证幂等的命令根。任意 Minecraft
命令无法由 Base 单方面消除“命令已执行但玩家幂等标记尚未落盘”的崩溃窗口，因此不得把普通命令宣称为
exactly-once。当前主体批次已完成自动门禁，仍需后续 BQ 影子对照。

### D. 编辑器

| 能力 | Base 状态 | 下一步 |
|---|---|---|
| 草稿创建、字段编辑、Task/Reward 动态表单 | 已完成 | 批量维护 |
| 前置选择、发布图校验、生命周期 | 已完成 | 批量操作复用影响诊断 |
| 章节属性、候选搜索、放置/移动/缩放/删除、多选对齐 | 自动验收完成 | 管理员实机验收与批量影响预览 |
| 网格、滚轮缩放、中键平移、键盘精调、内部依赖边 | 已完成 | 外部/缺失诊断与影响预览 |
| 单项/组选中复制 | 主要实现完成 | 服务端原子写入、章节位置复制、终端多选及 UI 动作；需要实机管理员验收 |
| 章节复制与排序 | 自动验收完成 | 生产目录表迁移、管理员实机验收、对齐与影响预览 |
| 导入/导出、版本升级 | 只读导入已完成 | 管理预检包、差异报告、签名与回滚 |
| 批量删除/移动/属性修改 | 批量退役核心、终端入口与确认已完成 | 完整影响项浏览；移动/属性批次仍待实现 |

BQ `ToolboxToolCopy` 的成熟语义是：组选中复制时生成一组新 UUID，组内前置引用改到复制品，组外前置
保持不变。Base 吸收这一算法，但拒绝其“客户端生成 NBT、任务与章节分两包发送”的一致性风险。第一步
已建立纯 Java `QuestDefinitionBatchCloner`；下一步由服务端生成 ID，并在一个数据库事务中同时创建草稿与
章节位置。

### E. 管理、诊断与迁移

- 已有：认证管理查询、服务端分页、内容哈希冲突、发布图校验、BQ 定义/玩家进度只读导入和未知类型报告。
- 已建立：只读 BQ/Base 进度影子差异分析（稳定按玩家/任务对账、单侧/超前/分歧/不可比较原因和有界报告）。
  分析器拒绝 PARTY/TEAM/PUBLIC 主体进入玩家对账，并将任务类型、向量宽度和任务单侧缺失标为
  `INCOMPARABLE`，避免群体主体误映射或把结构不一致误判为领先。该分析尚未接入实时双算或管理员终端，
  不能作为正式切换依据。
- 待补：复制、批量命令、影响预览、导入差异报告、dry-run、跨服玩家进度合并冲突策略、实时影子运行报告、
  管理员重算/重置/修复工具。
- 不吸收：BQ 任意 Packet 整库同步、任务书入口、客户端权威编辑、JSON 自动覆盖和无事务奖励执行。

## 阶段路线

1. **QA 功能差距审计**：逐类绑定 BQ 源文件、Base 实现与自动测试；发现差距不得用“已识别类型”冒充支持。
2. **QB 编辑成熟度**：任务/章节复制、依赖连线、多选对齐、章节排序、批量维护；全部使用服务端语义动作。
3. **QC 检测完备**：补统计量检测、事件/轮询调度、扩展类型兼容矩阵、NBT/OreDict/流体边界。
4. **QD 管理迁移工具**：dry-run、差异、幂等导入、玩家合并、可回滚批次和审计报告。
5. **QE 影子运行**：BQ 与 Base 并行计算但 Base 不发奖，按任务/玩家/事实类型输出差异。
6. **QF 正式切换**：先冻结 BQ 编辑和领取，再启用 PostgreSQL 运行时，观察恢复队列后移除 BQ 依赖。

每阶段都必须通过纯 Java、Minecraft 适配、PostgreSQL 临时真库、终端视觉、自包含 JAR 和 `git diff --check`
门禁。除真实 GL 与玩家手感抽检外，不把常规正确性转嫁给人工验证。

## 当前实施点

QB.1 已完成核心、章节原子层及终端动作链：`QuestDefinitionBatchCloner` 在纯核心中生成有界、不可变批量复制计划，保证
源顺序稳定、ID 不碰撞、组内依赖重映射、组外依赖保持，并保留 Task、Reward、重复与行为语义。
`QuestChapterCloneService` 会从目标 DRAFT 章节的真实已放置任务和精确任务版本读取源数据，以第一个选中
任务为相对坐标原点，在一个 `QuestTransaction` 中创建全部新草稿并更新同一章节；章节乐观哈希冲突时由
事务整体回滚。`TerminalQuestChapterClonePayload` 已限制为章节 ID/版本/哈希、锚点和最多 128 个精确源；
终端画布已有多选及复制入口，客户端不传任务定义或身份。QB.2 的第一段已完成：
`AuthenticatedQuestChapterDependencyQuery` 先以当前服务端管理员身份授权，再从权威章节和最新已发布任务定义
构建依赖图；直接前置任务即便不在画布内也会被读取，从而准确区分 `INTERNAL`、`EXTERNAL` 与 `MISSING`。
有界边集合已进入同版本终端快照、网络编解码和 UI2 画布；只有内部边绘制为连接线，外部/缺失边不会被伪造
为节点。章节排序也已实现为独立于画布坐标的服务端目录：`QuestChapterCatalogEntry`、
`QuestChapterOrderingService` 与 PostgreSQL `galaxy_quest_chapter_catalog` 以完整集合校验和事务替换维护顺序；
终端只发送“把此章节移到另一章节前（或追加）”的相对意图，服务端读取完整目录后落库，避免客户端分页或
陈旧列表覆盖全局排序。章节列表已有上/下移动入口。该实现已通过纯核心、PostgreSQL 临时真库、终端 UI2
及完整构建门禁；生产 DDL 尚未执行，也尚未进行管理员实机验收。多选对齐现由独立的
`QuestChapterAlignmentService` 完成：客户端只提交 2–128 个任务 ID 和六种白名单模式之一，服务器从当前
草稿真实布局计算左/横中/右/上/纵中/下坐标，并以一次内容哈希乐观写入提交。当前仍缺外部/缺失前置检查器、
影响预览和更广泛的管理员批处理，不能退回到客户端 NBT 拼装。

QB.3 已建立任务定义影响预览的纯核心、认证查询和终端接入：`QuestDefinitionImpactAnalyzer` 只使用服务端读取的
最新已发布任务和章节目录，计算前置关系、Task/Reward 的 `questUuid` 关系、传递依赖及章节放置位置；结果稳定排序、最多 4096 项并显式报告
截断，只有目标已发布且不存在依赖、放置或截断时才给出 `safeToRetire`。客户端不能提交关系图或影响结论，
越权查询在读取仓储前失败。管理员详情通过既有有界 options 协议显示直接/间接依赖、章节放置和截断状态；
退役按钮只在明确安全时启用，即使绕过客户端按钮，服务端仍会重新查询同一报告。查询缺失、异常、越权和
截断全部失败闭合。下一步让批量删除、移动和属性命令复用该预检，再补完整影响项浏览。

QB.4 第一段已完成批量退役的纯核心安全边界。`QuestDefinitionImpactAnalyzer#analyzeBatch` 接受 1–128 个互异
任务 ID，把选中集合内的前置或 Task/Reward `questUuid` 引用视为组内关系，只把集合外的直接/传递依赖、
任一已发布章节放置、未发布目标或截断报告为阻塞项。`AuthenticatedQuestDefinitionBatchImpactQuery` 在读取目录
前完成管理员鉴权；`QuestDefinitionBatchRetirementService` 在一个 `QuestTransaction` 中重新读取权威目录、校验
完整影响和每个精确版本，然后才逐项退役，任一写冲突会使 PostgreSQL 事务整体回滚。客户端尚无该批量动作
入口，也不能将此阶段误报为完整批量删除；下一步新增有界终端载荷、管理列表多选、批量确认与完整影响项浏览。

QB.4 第二段已把安全边界接到终端：新增 `TerminalQuestBatchRetirePayload`，只携带 2–128 个任务 UUID/版本及
查询上下文；`TerminalService` 从当前认证管理员构造服务端请求，`QuestModule` 安装共享 PostgreSQL 事务服务。
管理列表增加显式多选模式和批量退役动作，选中项仅限已发布任务；批量提交现在先进入独立确认浮层，Escape、遮罩
点击和 Enter 均由模态输入层消费，确认后服务端仍会重新读取目录、影响报告和精确版本再原子执行。完整影响项
分页、移动与属性批处理仍未实现，因此仍不能称为 BQ 完整替代。

## 2026-09-14 运行时检测接线收口

此前 `QuestGameplayEventHandler`、`RuntimeQuestFactSink` 和 `QuestEvaluationPlanner` 只存在于源码，CodeGraph
没有发现任何生产注册调用，意味着 craft、smelt、anvil、击杀、破坏、交互和周期观察不会进入 PostgreSQL
进度运行时。本次在 `QuestModule` 的共享 JDBC 组合根中补齐运行时对象图：定义/完成查询、事实幂等仓储、投影器、
引擎和事务服务使用同一连接边界；事件采集器同时注册到 FML 总线与 Forge 总线，周期观察计划按 5 秒缓存已发布
定义，数据库瞬时不可用时保留最后成功计划。服务启动失败仍整体失败闭合，不会安装半套处理器；重复启动会先解除
旧处理器，避免跨世界重复计数。

该接线只在 Quest PostgreSQL 运行时已通过只读 schema 校验、且 dedicated server 与共享 banking JDBC 均可用时
启用；未启用配置、客户端和缺少连接的环境保持原有无运行时状态。下一阶段仍需补事件取消/FakePlayer 边界、
扩展类型注册表、库存快照去重及 BQ 影子差异报告。

## 2026-09-14 crafting statistics 兼容

已补齐 BQ `TaskCrafting.allowCraftedFromStatistics` 的核心语义：发布目录扫描只收集开启该选项且实际出现在
任务需求向量中的物品；Minecraft 适配从服务器玩家的原版 `StatisticsFile/objectCraftStats` 读取绝对累计值，
以 `channel=craft-statistics` 事实送入同一 `item-vector` evaluator。该通道不会被误当作 craft 增量，核心只取
当前进度与统计快照的最大值；未显式开启选项的任务拒绝该事实。统计读取仅对 `EntityPlayerMP` 生效，FakePlayer
和客户端世界仍由既有边界排除。剩余差异包括 1.7.10 统计按 Item ID 聚合而非按 meta 分桶的已知限制，需在影子
对照报告中单独标识，不能宣称完全等价。
