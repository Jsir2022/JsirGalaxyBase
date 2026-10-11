# BetterQuesting M.2 行为兼容工作矩阵

日期：2026-09-23
状态：实施中；已获用户授权只读服务端任务定义并完成首次真实频率清点

## 当前入口（2026-10-11）

阶段状态只取能力矩阵顶部；有限缺口和下一步见
[M.2 自动收口](betterquesting-m2-automatic-closeout-2026-10-11.md)。
Crafting 已冻结事件时 roster/资格/定义请求；无法捕获权威快照即拒绝。
Detect 新版发布允许，旧批次固定旧版恢复并阻止跨版本并发。
下方描述上述问题“未决/未实现”的段落仅是 R.2.34 以前的历史记录。
R.2.35 为 Checkbox 补齐 Runtime acceptance 内的 repeat claim/consumption fence，
避免提前确认消耗下周期事实键；不改变确认归属或奖励策略。

## 2026-10-11 合同与历史自动证据

用户确认的当前语义：非消耗物品/流体任务持续自动观察；消耗型任务使用 Quest 级 Detect，按发布定义顺序处理所有未完成的消耗 Task，每项独立持久化并可恢复；不承诺库存副作用与 PostgreSQL 的跨系统全有或全无。重复周期奖励仍可领取时不提前进入下一周期。Crafting 同一玩家按来源服分别取 `max(该服事件累计, 该服绝对统计快照)` 后求和，不同 Party 成员仍分别记入自己的 PLAYER 进度。BQ 会保留高于需求量的绝对 Crafting 统计值；完成状态按目标阈值判断，不把观察值截断到目标。

当前工作树已有 Quest 级服务端批次协调器，自动测试覆盖稳定幂等键、游标恢复、OR Quest 批次末结算、重复周期冷却和源服恢复限制。运行入口目前只支持 PLAYER scope 下已验证的 consuming Retrieval、Optional Retrieval 与 Fluid 子集。新增 PostgreSQL 批次表迁移已在隔离测试库验证，不代表生产 DDL 已执行；真实 Forge/GTNH 库存交互、服务切换恢复和整体 M.2 仍为 BLOCKED。下方较早日期的“未实现/待确认”均为历史状态，以本节和权威[能力矩阵](betterquesting-capability-absorption-matrix-2026-09-13.md)为准。

2026-10-11 Quest Detect 的 PostgreSQL journal 回归扩展为三项有序 Task：验证写入/重读保留 key 顺序，重复 Detect 入口不能覆盖原批次 Task 列表，游标对每个索引幂等推进并最终完成。该集成用例 1/1 通过；与 Core 定义顺序服务测试合看只证明代码/隔离数据库合同，不证明真实玩家背包或 Forge 运行。

2026-10-11 R.2.20 对照固定 BQ `TaskCrafting.detect()`/`onItemInternal()` 与 `TaskProgressableBase.setUserProgress()`：事件进度单服封顶，统计绝对值可高于 Task 目标并原样保存。Base 对来源事件累计封顶、对统计观察值不封顶，来源 max/sum 后可超目标。Core 全套 281 项、隔离 PostgreSQL 定向 1 项通过；`assemble` 与 `verifyUi2SelfContained` 通过。仅关闭数值保存差异；Crafting/M.2 仍 BLOCKED，R.2.19 的 Party 重试归属时点仍未决定。

2026-10-11 R.2.21 对照固定 BQ `TaskHunt.onKilledByPlayer()`：每个匹配击杀以 `min(required, current + 1)` 保存进度。Base `SubjectCountTaskEvaluator` 现限制新事件观察到目标值，且保留已持久化的更高历史观察值。Core 全套 282 项、`assemble` 与 `verifyUi2SelfContained` 通过；这是纯 Core 数值回归，没有新增 JDBC 场景。仅关闭 Hunt 新事件数值封顶差异；Forge/GTNH 事件与 PARTY 时序仍未验证，Hunt/M.2 继续 BLOCKED。

2026-10-11 R.2.22 固定 BQ `QuestInstance.resetUser(uuid, false)` 的重复周期 Task 重置语义。Core 与隔离 PostgreSQL 回归验证 cycle 1 不继承旧来源事件 JSONB；另验证 event/absolute 在每个来源内取最大值、多个来源求和后持久化/reload 不变（示例总值 9，而非双重累加的 14）。Core 全套 283 项、PostgreSQL 定向集成 2 项通过，均无跳过/失败/错误；玩家生命周期 Crafting 统计不因 Quest reset 清零。Forge 读取、R.2.19 Party 重试归属及 Crafting/M.2 仍 BLOCKED。

2026-10-11 再核对固定 BQ 源码并区分两种 Party 语义：`EventHandler` 在事件时创建 `ParticipantInfo`，`getSharedQuests()` 只从本服在线 `ACTIVE_PLAYERS` 的 active Quest 并集决定任务是否参与；一旦任务参与，`TaskCrafting.onItemInternal` 将匹配产物事件进度写给完整 `ALL_UUIDS`（含离线 Party 成员），随后 `TaskCrafting.detect()` 只对在线成员读取各自的 `objectCraftStats`。Base 在成功处理时也先以本服在线成员/活动 Quest 资格作门控，再由 `BqHuntPartyAssignmentPolicy` 将匹配事件投影到当前 Party 的全部 PLAYER 进度行；个人绝对统计快照仅对在线成员生成，不把不同成员计数相加。因此，“在线成员决定 Quest 是否活动”与“Party 全成员接收事件进度”不能混为一谈。尚未对齐的是事实延迟时点：Base 队列保存事实数据但不保存事件时 Party/活动 Quest 快照，数据库恢复后由 Planner 按重试时当前 Party 和资格重新判断；若成员关系或资格改变，可能与 BQ 的同步事件时语义不同。R.2.19 保持 BLOCKED，不能仅凭在线成员统计回归宣称等价。

2026-10-11 实现可行性细化：`QuestModule` 的 membership 入口是 `JdbcParticipantMembershipResolver`，`BqPartyActiveQuestQuery` 通过 JDBC 完成/进度仓储与在线目录重新判断资格。仅给 `GameplayFact` 增加 Party UUID/成员列表，不会固定活动 Quest、前置、repeat/cooldown 和待领奖资格；而 PostgreSQL 已不可用时又没有新鲜权威快照可供事件入口捕获。要选择事件时语义，必须连同缓存新鲜度、无快照时失败闭合/诊断、旧成员迟到投影和现有内存重试队列的进程崩溃边界一起定合同。仅靠保留原幂等键或冻结 Party UUID 不足以声称 BQ 等价。当前已向用户发出事件时归属（推荐）/重试时当前 Party 两选一；未确认前不修改进度归属。

2026-10-11 R.2.23 补齐 PARTY 活动检查的精确 Crafting 事件前置：`BqPartyTaskProgressScope` 现在复用与 Task evaluator 相同的通道、物品、Meta/damageable、OreDictionary 和 portable NBT matcher；没有命中任何需求的产物不再触发 PARTY 在线资格查询或成员 fan-out。新增无关物品回归断言其只保留个人请求且不查询 Party 活跃性；包含在 Core 全套 284 项（0 跳过/失败/错误）中。该项只减少无效共享事件资格检查，不改变匹配事件进度，也不解决 R.2.19 Party 延迟归属。

2026-10-11 R.2.26 修复 simultaneous 在 Quest Detect 与延迟批次结算中的转换边界：仅当本轮确有此前未完成的 Task 转为完成、但 Quest 逻辑仍未满足时才清空该周期 Task 进度；历史上已完成的 Task 单独存在不再触发重置。Quest 级消耗批次延迟结算会依据批次选中的 Task key 保留这一转换事实。Core 全套 289 项（0 跳过/失败/错误）通过。只读 S2 定义的聚合统计为 3,744 个 Quest、simultaneous 启用 0 个；因此该通用边界有合成 Core 回归，但当前 S2 内容不提供实样本，影子结果对照和 M.2 仍 BLOCKED。一次 `behaviorInventory` 运行曾将逐 Quest 名称/UUID写到终端输出，不符合匿名聚合口径；该输出未转存到仓库，频率结论改由只输出总数的只读统计取得。2026-10-11 importer 回归进一步确认 `BqBehaviorInventory` 还会输出最多三个 sample Quest UUID；现已删除该字段和 manifest 逐条行，仅保留匿名计数及问题代码聚合，详见 Worklog。

## 2026-10-02 当前范围收口

用户确认背包物品检测为主线。优先修复背包观察、提交/消费、完成和领奖的可靠性及性能；观察站邻接库存保留为已知差异并延后，不作为当前必须先确定的交互方案。源码语义已进一步限定：BQ Observation Station 每 20 tick 扫 owner 所在站点六邻的物品/流体容器，借 `ParticipantInfo.ALL_UUIDS` 更新 owner 的共享 Quest；`TaskRetrieval.retrieveItems`、`TaskFluid.retrieveItems/retrieveFluids` 对 consuming 或已完成任务不做处理，因此它只补充非消耗库存存在检测，不消费容器内容。当前 DefaultQuests 无法说明玩家是否实际使用该方块。类型仍按证据保持 BLOCKED，不能因为延后辅助入口就改报兼容通过。阶段与源码风险详见 [进度质量复核](betterquesting-progress-quality-review-2026-10-02.md)；本段优先于历史记录中的“下一步”。

## 证据口径

`BqBehaviorInventory` 对传入目录生成来源 SHA-256、Task/Reward 使用次数、参数键频率、关键行为开关的
真假分布、按类型聚合的联合开关组合，以及物品匹配开关与 NBT/OreDictionary 配置的交叉计数；不保留单独 Quest 名称、UUID 或逐 Quest 行。
`behaviorInventory` CLI 的 manifest 摘要也只输出总数和按问题代码聚合的诊断；导入规划/审计所用的详细 manifest 输出是另一份操作报告，不应冒充匿名盘点。画像不会读取固定路径，也不会连接数据库。报告按次数降序；认识某种 JSON 类型只说明可解析，运行行为未经
逐项对照时仍标 `BLOCKED`。未知类型标 `UNKNOWN`。既有 `BqCompatibilityReport` 的 `ADAPTED` 仅表示
内容格式已映射，不代表检测、重复周期或奖励交付等价。

用户于 2026-09-23 明确授权读取服务端任务文件，并于 2026-09-29 再次确认可直接读取。2026-09-29
复核时只将 S2 的 `config/betterquesting/DefaultQuests` 目录以只读挂载交给隔离开发容器；未读取玩家进度、
数据库，也未修改服务端文件。清单来源哈希为
`9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，共 3,744 个 Quest 定义文件、5,620 个 Task 实例、47 个章节。
此摘要是规划器对导入内容的确定性哈希，不应误称整个目录的原始文件 SHA-256。不要把任务 JSON、
玩家进度或实际物品 NBT 写入本报告。
2026-09-30 按用户明确回复，再次直接从 S2 服务端定义目录只读运行 `behaviorInventory`；摘要哈希、任务/章节/放置数
与本表一致。输入只包含 `DefaultQuests`，未挂载同级玩家进度目录。

2026-10-10 使用当前工作树重新全量运行 `:galaxy-quest-bq-importer:behaviorInventory`，仍得到相同来源规划摘要：3,744 个 Quest 定义、5,620 个 Task occurrence、5,448 个 Reward occurrence、47 个章节、3,847 个放置；规划 `applicable=true` 且无 ERROR。已知 Task/Reward 均能被 importer 解码，未知类型集合为空，但状态 `ADAPTED` 只代表解析。规划仍有 64 个未放置 Quest 和 4 个有种子前置循环警告，所有类型的运行时状态仍 `BLOCKED`。只读挂载限于 `DefaultQuests`；未读取玩家进度、未连接数据库。

当前只读清单还按 Quest 定义汇总 BQ `isGlobal/globalShare/autoClaim/lockedProgress/simultaneous/repeatable` 组合：3,744 个
定义中 `isGlobal=true` 为 0；`globalShare=true` 为 2,494，但这些定义全部 `isGlobal=false`，因此不会进入
`QuestInstance.hasClaimed` 的全局共享奖励分支。203 个 Checkbox 中 76 个所在任务有 `globalShare=true`，但也全部
`isGlobal=false`。`globalShare` 不应被误解成 Party 进度同步。2026-10-11 后续只读检查 S2 磁盘
`config/betterquesting/questbook.cfg` 得到 `fullySyncQuests=false`；源码注释仍标记 `[NYI]`，文件值不证明运行进程已加载，
因此运行时开关语义仍须留在未验收范围。

2026-10-11 补做 Base 运行语义审计：Importer、QuestBehavior 与 PostgreSQL codec 会保留 `isGlobal/globalShare`，管理页也能显示/编辑；但 Base 的 Quest 求值、完成和领取路径没有实现 BQ 全局语义。固定 BQ `QuestInstance.isComplete(uuid)` 在 `isGlobal=true` 时以任意玩家已有完成记录作为全局完成；`hasClaimed(uuid)` 则仅在 `isGlobal && !globalShare` 时把领取状态按全局共享。CodeGraph `explore/impact QuestBehavior.isGlobal` 显示 Base 当前调用集中于定义 JSON codec、管理快照与 Crafting 统计资格限制，没有进入 QuestEngine/奖励领取归约。由于当前 S2 3,744 个定义中 `isGlobal=true` 为 0，这不是该快照的已知运行内容阻断，但“global/globalShare 已兼容”的旧矩阵表述不准确；通用导入内容仍不得宣称这项行为已支持。单独的 `globalShare=true` 不能替代 PARTY 进度语义。

2026-10-11 对同一只读 DefaultQuests 范围补做消耗型任务画像：308 个 `consume=true` Task occurrence 中有 307 个
Retrieval、1 个 Fluid；全部 `autoConsume=false`，没有定义显式 `isGlobal=true`（283 个显式 false、25 个省略并按
BQ 默认 false 处理）。其中 284 个设置 `globalShare=true`，但这不是 Party 共享进度开关。固定 BQ 源码中
`fullySyncQuests` 默认 false、注释为 `[NYI]`；`TaskRetrieval`/`TaskFluid` 的 consuming Detect 在该默认语义下读取
发起者库存，并把进度写入其 PLAYER 身份。当前 Base 的 PLAYER-only consuming Quest Detect 与此源码默认行为及
S2 定义画像吻合。S2 磁盘配置当前为 `fullySyncQuests=false`，但文件值不能证明运行进程已加载；若运行时覆写该开关，
Party 进度语义仍需另外确认。该画像只说明内容范围，不代表真实运行配置加载、客户端请求、Forge 库存副作用或运行时兼容已验收。

| 实际 Task 类型 | 次数 | 关键配置分布 |
|---|---:|---|
| retrieval | 3,863 | `consume=true` 307；`groupDetect=true` 4；`ignoreNBT=true` 2,029；`partialMatch=true` 3,861；`autoConsume=true` 0 |
| optional_retrieval | 1,267 | 均非消耗；`ignoreNBT=true` 689；`groupDetect=true` 0；`partialMatch=true` 1,263 |
| checkbox | 203 | 需核对服务端认证的手动完成语义 |
| crafting | 133 | `allowCraftedFromStatistics=true` 6；`allowAnvil=true` 0 |
| hunt | 104 | `ignoreNBT=false` 2 |
| location | 33 | 维度分布及重复/领取行为已分组 |
| fluid | 15 | 共 16 项需求；单项需求量分布：144 mB ×8、576 mB ×1、1,000 mB ×6、5,000 mB ×1；`consume=true` 1；`ignoreNBT=false` 5 |
| block_break | 1 | 有序需求导入和 Core 方块匹配已按 BQ 源码回归；Forge/GTNH 运行链仍待验证 |
| meeting | 1 | `QuestObservationPlan` 从发布定义取得最大观察半径；已注册的服务端 PlayerTick Handler 每 60 tick 生成附近实体快照，`MeetingTaskEvaluator` 按范围、实体注册名/子类别名、数量和可选 NBT 求值。对照 BQ `TaskMeeting.detect/tickTask` 后，可信 PARTY 事实向每位成员投影 PLAYER 进度；至少一名队员满足前置才处理，锁定成员可暂存完成但不提前获奖。最大半径合并快照会按每项任务的 `player.boundingBox.expand(range)` 对实体 AABB 再过滤；超范围快照若缺 observer/entity geometry 则失败闭合，精确半径快照仍兼容旧事实。R.2.9 已有边界、相交、仅接触、缺字段和非法坐标回归。仍 `BLOCKED`：未在 Forge/GTNH 验证 `EntityList`、实体 NBT 序列化、Tick 与跨服成员实际行为（当前只读 DefaultQuests 中 meeting 类型仅 1 项） |

2026-10-11 Optional Retrieval 导入边界补验：固定 BQ `TaskOptionalRetrieval.ignored(UUID)` 恒为 true；新增 typed JSON importer→`TaskDefinition.optional`→库存快照求值→`QuestEngine` 自动回归，确认部分满足的 optional Retrieval 不阻挡已满足必需任务的 Quest 完成，且自身进度仍保持未完成。Importer 定向测试 5 项、0 skipped/failure/error。该回归只补齐跨模块自动链，不证明 BQ `QuestCache`、Forge 背包监听、Party 在线库存或 GTNH 实际交互；optional Retrieval/M.2 继续 BLOCKED。

2026-10-11 R.2.28 导入 fail-closed 修正：原实现用 Task ID 是否包含 `optional` 子串推导 optional，可能把不支持的 `addon:optional_special` 当作可忽略目标。现在仅精确识别已知 `bq_standard:optional_retrieval`；未知 Task 默认保留为 required，并仍在兼容报告中标为 `UNKNOWN`。回归确认这类 Quest 没有其他任务进度时仍为 AVAILABLE。Importer 全模块 51 项中 44 项执行通过、7 项因未设置隔离 `QUEST_TEST_POSTGRES_URL` assumption-skip，0 failure/error。此修复避免静默放宽未支持定义，不构成未知类型兼容或 M.2 签收。

以上 9 种 Task 类型合计 5,620 个 occurrence。当前只读 `DefaultQuests` 中 `xp`、`scoreboard`、`interact_item`、`interact_entity` 均为 0；下方这些类型的通用能力状态属于未来导入兼容边界，不是当前任务库的 M.2 内容门禁。本次只统计 `DefaultQuests/Quests`，未读取 Players 或其它服务器配置。

| 实际 Reward 类型 | 次数 |
|---|---:|
| item | 4,181 |
| choice | 957 |
| questcompletion | 308 |
| xp | 2 |

`BqBehaviorInventory` 现将奖励级 `ignoreDisabled` 作为行为开关统计；只读 S2 清单显示 item 4,181/4,181、
choice 957/957、XP 2/2 均为 `false`，questcompletion 308/308 均为 `true`。因此 BQ `noRewards` 开关若启用，
前三类受其影响，questcompletion 不受影响。源码 `BQ_Settings.noRewards` 默认 `false`，它是 BQ 全局服务端设置，
不在 `DefaultQuests` 定义中；本次没有读取服务器的其它配置文件，因此不能断言 S2 当前是否开启。
`AbstractReward.claimReward` 在 `noRewards && !ignoreDisabled` 时跳过奖励处理，而 `QuestInstance.claimReward`
仍把任务标记为已领取。Base 已新增默认关闭的服务端 `quest.noRewards` 配置，并在自动奖励生成和手动领取时
使用同一策略。受抑制权益进入独立 `SUPPRESSED` 终态、显示在任务摘要中且不会进入投递队列；选择奖励仍要求
玩家先完成选择，不会因全局开关而静默代选。旧数据库启用此选项前必须应用
`ops/sql/migrations/20260929_001_add_quest_reward_suppressed_status.sql`；启动时只读校验约束，缺失则
Quest runtime fail closed。本轮没有执行该迁移或读取生产数据库。
导入器现对缺省 `ignoreDisabled` 补用 BQ 标准奖励类的源码默认值：command、questcompletion、scoreboard 为 true，
item、choice、XP 为 false；文件显式保存的值保持原样。未知扩展奖励不猜默认值。

本次没有未知 Task/Reward 类型；未使用的标准类型仍保留在下方矩阵，但不构成本快照的优先实现项。全部
**行为状态仍为 `BLOCKED`**：计数只证明存在，不证明当前 Base evaluator 或奖励交付与 BQ 等价。

真实定义的结构清单目前 `applicable=true`，但**不等于 M.2 可运行**。最后一条方块配置是“原木 ×1 +
空气 ×0”；BQ 把数量 0 当已满足分量，Base 向量也支持，只放开方块列表的零数量。原先 18 条前置环报错
实际对应 4 组、共 12 个 `XOR` 互斥选择任务：每个任务都有组外前置作为入口。导入器先前把 XOR 错读为
AND；现补齐 BQ 六种逻辑真值表，并只把“所有成员均 XOR 且各自有组外前置”的强连通组降为有界警告，
无入口或非 XOR 环仍阻断。真实清单复算无结构错误，保留 4 条互斥组警告和 64 条未放置警告。

离线 CLI 的 `apply` 现在解析清单后、创建数据库连接前调用 M.2 行为门槛；只要存在尚未逐类型签收的
Task/Reward 就拒绝发布，并有无数据库配置的回归测试。
`plan`、`dry-run` 和隔离预览仍可用于内容检查；既有 M.1 数据库服务内部 API 仍服务于隔离内容迁移测试，
不能把结构可用或其测试通过误报为行为批准。

## 初始类型矩阵

2026-10-11 R2.18 更正 Fluid 消耗差异：固定 BQ `TaskFluid.Detector.run` 按每个容器实际返回的 drained amount 累加；注册的不可分割容器可以使进度超过目标。Base 现把服务端记录的实际排出量传入 Fluid 进度（不影响 Item/XP 的目标封顶），并由 Quest 级 Detect 批次末完成结算。该修正覆盖自动隔离回归；S2 DefaultQuests 当前唯一消耗 Fluid 定义为 Honey 1,000 mB，但实际 Forge/GTNH 容器注册与背包运行仍未验证，故 Fluid/M.2 继续 `BLOCKED`。这段更新优先于下方较早记录中的“只记剩余目标”描述。

下表状态针对 **BQ 行为等价**，不是“Base 是否已有同名 evaluator”。所有类型需要固定 BQ 源码版本、
代表性任务配置、输入事件、进度与奖励输出、重复周期和差异测试，才可从 `BLOCKED` 升级。

| BQ 类型 | Base 当前基础 | M.2 状态 | 已知差异或待核对项 |
|---|---|---|---|
| retrieval / optional_retrieval | 库存快照；个人手动消耗请求已绑定周期并保护待恢复周期；BQ 耐久物品与 NBT 列表匹配已覆盖 Core 和 Minecraft 消耗适配器 | BLOCKED | 非消耗进度回落、damageable StackMatch、NBT 列表复用/数组唯一匹配、重复消耗周期保护及 optional retrieval 不阻断 Quest 完成已有回归；R2.1 新增重叠 NBT 需求争用测试，确认按规范化后保留的首见背包槽位顺序分配，Core/Importer/根消费适配定向 279 项均通过；本轮无生产求值器改写。审计发现 retrieval 中 3 处完全重复 compound-list 需求形状。导入非消耗任务的 PARTY 路径已编码：登录创建 fenced session，服务端按 1 秒节奏刷新 15 秒租约，在同事务中生成 PARTY 聚合事实并路由到每名成员的独立 PLAYER 进度；过期成员不计入、锁定成员可保存 Task 完成但不会提前获奖。隔离 PostgreSQL 已覆盖租约过期、跨会话防护和事实隐私裁剪。新增生产快照 sink 的 fake-store 自动测试：覆盖 solo 只发布个人事实、Party 合并本地与新鲜成员背包并分别发布个人/聚合事实、旧 session 无法发布；这是 producer 单元测试，不证明 JDBC 全链事务或 Forge/GTNH 运行时。另按 BQ `ParticipantInfo` 源码补齐无 PARTY 时的 singleton 退化：只接受无 scope 的服务端个人库存事实；有 PARTY 时仍拒绝普通单人快照，必须使用精确可信的 PARTY 聚合快照。最新隔离 PostgreSQL 场景把 `optional_retrieval` 与必需 Retrieval 放入同一 PARTY 完成链：当前库存缺少可选物品时，逐玩家 Quest 仍完成并入账 item/choice 权益，同时可选任务进度保持未完成。消耗提交路径已区分：背包匹配项映射 BQ `detect`，手持栈映射 `submitItem`；Base 使用 hotbar 而 BQ 使用 SubmitStation 输入槽。新核对到 BQ `TileObservationStation` 还会每 20 tick 检查绑定玩家站点六个相邻方块中的 `IInventory`、`IFluidTank` 与 `IFluidHandler`，并把观测交给其共享任务；Base 当前 PARTY 库存事实只覆盖玩家背包，尚无等价的邻接容器观测入口。DefaultQuests 定义不记录玩家是否实际摆放/使用该站，因此这是已确认的行为缺口、不是已证实的当前游玩依赖。BQ `fullySyncQuests` 默认 false 且源码注释标记 `[NYI]`；true 时消耗进度扇出整队，Base 尚未实现，实际服务器开关值未读取。多服实机、断线/切服边界、GTNH 真实物品/NBT、邻接容器与存档恢复仍未验收，因此整体保持阻断 |
| fluid | 非消耗快照、手动个人消费；保留流体 Tag 是否存在，精确匹配并记录实际部分扣除量；R2.5 已对齐非消耗需求独立重算、`groupDetect` 和进度回落，并复用有效 PARTY inventory leases 汇总流体且逐成员写入 PLAYER 进度；R2.18 已让 consuming Detect 持久化不可分割容器的实际超量 | BLOCKED | 当前 S2 定义有 15 个 fluid Task、16 项需求，需求量分布为 144×8、576×1、1,000×6、5,000×1 mB；唯一 consuming Task 仍是 1 项需求，配置 `consume=true`、`autoConsume=false`、`groupDetect=false`、`ignoreNBT=false` 且无 `Tag`。隔离 Core/JDBC 自动测试覆盖非消耗快照 900→700 mB、过期 lease 排除和 Party fan-out；R.2.18 覆盖 400+750=1,150/1,000 的 Quest Detect 超量进度。`fullySyncQuests` 实际运行值、真实 GTNH 流体容器、无空容器槽时的掉落语义未验收，跨服在线资格时序也未实机验证 |
| crafting | 制作、熔炼、铁砧与个人统计事实；匹配产物按 Task 自动复评；每服对本服在线 Party 成员分别采样统计并写入其 PLAYER 进度 | BLOCKED | 用户已确认：不同 Party 成员计数不合并；同一玩家跨来源服按每来源 `max(事件累计, 绝对统计快照)` 后求和；非消耗库存任务自动观察，消耗任务通过 Quest 级 Detect 按定义顺序逐项提交；Crafting 统计不提供手动 Detect，仍由匹配产物事件和 Base 每 60 tick 自动复评。来源贡献与 legacy baseline 已进入 TaskProgress/JDBC JSONB，并有隔离数据库往返/迁移回归。S2 当前 133 个 Crafting occurrence 中统计模式启用 6 个；只读复核这 6 个启用项均为单一目标、`allowCraft=true`、`allowSmelt=true`、`allowAnvil=false`、`ignoreNBT=false`。本轮再次只读核对其目标为 furnace×1、flint×3、chest×1、crafting_table×1、piston×1、flint_and_steel×1；全部 meta=0、无 OreDictionary 目标，未发现需扩展匹配形状。固定 BQ 源码确认产物事件时按完整 `ParticipantInfo.ALL_UUIDS` 扇出进度，再对在线成员读取各自统计；Base 有序队列保存生成后的不可变事实，却没有冻结事件时 Party ID，重试时规划器可能按当前 Party 重新归属。用户尚未确认该故障边界采用事件时还是重试时归属，见 R.2.19 前置审计。自动复评与匹配产物路径已有 Core、事件分发和隔离 PostgreSQL 回归，但测试不覆盖真实 Forge 回调、`EntityPlayerMP.objectCraftStats`、生产在线 Party 查询和多服时序，因此 M.2 行为等价继续 BLOCKED。|
| block_break | `TaskBlockBreak` 的有序需求已导入为 block-vector；方块事实携带注册名、元数据、OreDictionary 与 TileEntity NBT；进度按 BQ 规则只推进首个匹配需求。直接匹配以负元数据为通配符，32767 仅用于 OreDictionary 查询 | BLOCKED | Core 与导入器测试覆盖配置映射、负值/32767 元数据、OreDictionary、部分 NBT 和首项推进。新增根项目隔离 PostgreSQL 回归从合成 BQ `block_break` JSON 导入/发布，经过正式 snapshot sink、Planner、Runtime 与 JDBC 验证同事实键重放幂等、首个匹配需求达标后仍遮蔽后续重叠需求，并由另一个需求完成 Quest；Core 38、Importer 5、根集成 11 共 54 项通过，0 skip/failure/error。该用例使用合成事实及单人 membership，不调用 Forge `BreakEvent`。实际 TileEntity/OreDictionary 注册值、取消事件、Party 在线资格与 GTNH 运行时仍未验证，故保持 BLOCKED |
| hunt | 玩家击杀计数、精确/子类、伤害类型、部分 NBT、目标封顶、重复周期及 PARTY 到每位成员独立 PLAYER 进度/权益的服务端扇出已实现；未注册实体和缺失目标现在按 BQ 规则拒绝；Core 与隔离 PostgreSQL 均有回归 | BLOCKED | S2 104 项：`subtypes=true` 103、false 1；`ignoreNBT=false` 2；全部 104 项 `damageType` 为空；58 项存有非空 `targetNBT`，但仅 2 项启用 NBT 比较；84 项所在任务可重复。R2.8 对照 `TaskHunt.onKilledByPlayer` 修复未注册实体子类仍借父类别名推进、缺失目标可被宽松计数；R.2.21 补齐新事件按 `required` 封顶且保留历史超目标值。最新 Core 全套 283 项通过，既有隔离 PostgreSQL Hunt PARTY/JDBC 用例 1 项通过。未证明真实 Forge `LivingDeathEvent`、FakePlayer/取消事件过滤、GTNH EntityList/NBT 与跨服 PARTY 事件链，因此类型和 M.2 仍 BLOCKED |
| location | 每 100 tick 的权威位置观察；当前 33 项画像只使用 dimension 条件，现有 evaluator 与 BQ 对该配置组合一致；导入的 PLAYER 任务按 PARTY 全员各自进度扇出，重复周期使用领取时间锚点 | BLOCKED | 当前 33 个 Task occurrence 均为 biome=-1、range<=0、无 structure、visible/invert/taxiCabDist=false，实际完成条件仅为 dimension；17 种维度（-1:2、1:2、28:2、41:2、55:13，其余 12 种各 1）。11 项可重复且 11/11 有奖励、autoClaim=false，重复间隔为相对 0 tick 4 项、相对 1,728,000 tick（24 小时）7 项；另 22 项不可重复，2 项无奖励。Base 已阻止未领取的 CLAIMABLE 权益跨周期，并以 claimed_at 作为 repeat anchor；R2.9 已新增合成 BQ JSON→PostgreSQL 发布→位置观察计划→在线 PARTY/个人 PLAYER 进度→手动领奖→claimed_at 冷却→cycle 1 的端到端隔离回归，原 event key 幂等也有验证。BQ 在线 `activeQuests`/`canSubmit` 的真实成员状态、Forge/GTNH 100 tick、领奖/存档与多服时序仍未验收 |
| meeting | 邻近实体观察；导入 meeting 事实按可信 PARTY→每位成员 PLAYER 进度扇出，至少一名队员满足前置后处理；锁定成员可暂存完成但不会提前生成可领取权益；每 Task 按 BQ 扩展玩家 AABB 过滤 | BLOCKED | BQ 源码每 60 tick 以每个 Task 的 `player.boundingBox.expand(range,range,range)` 查询。Base 最大共享半径快照已增加观察者/实体 AABB，`MeetingTaskEvaluator` 逐 Task 恢复该查询边界；Core 41 项、隔离 PostgreSQL PARTY/JDBC 1 项通过，根编译通过。R2.9 Meeting/Location 自动闭环已完成；仍需在 Forge/GTNH 验证 EntityList、实体 NBT 序列化、tick 与跨服成员实际行为；当前只读 DefaultQuests 中 meeting 类型仅 1 项 |
| interact_item / interact_entity | Forge 交互事实桥接与共享计数 evaluator 已有；右键空气与未注册实体过滤已修正 | BLOCKED | `interact_item`：BQ 将左键方块、右键方块、右键空气都交给 `TaskInteractItem`，只有左键方块标记为 hit；Base 接受 Forge 1.7.10 的三个实际 Action，并保留 canceled/server-player/FakePlayer 过滤。`interact_entity`：BQ 要求目标 ID 能由 `EntityList` 解析，未注册的目标实体会被忽略；Base 现为空主体/别名且 Core evaluator 拒绝空目标、空实际主体，避免父类别名误计。此次 Core `BqCompatibleTaskEvaluatorsTest` 41 项与根 `QuestGameplayEventHandlerFailureTest` 2 项均通过。仍未逐项证明目标配置画像、OreDictionary/NBT 与真实注册表行为、EntityInteract/Attack 到 PARTY/PLAYER/JDBC 的完整重复/取消边界；无 DefaultQuests occurrence 与 Forge/GTNH 运行时证据，故保持 BLOCKED。|
| scoreboard | 计分板观察 | BLOCKED | 运算符、目标创建与观察时机待对照 |
| xp | XP 观察与消费提交 | BLOCKED | points/levels 换算和消费时机待对照 |
| checkbox | 服务端确认事实 evaluator 与玩家确认入口 | BLOCKED | Task/版本身份绑定、锁定前置门控与幂等键回归已覆盖；UI2 确认按钮的实际命中区域新增三档窗口测试，锁定/不可确认任务隐藏入口。当前 S2 203 项全部位于 `isGlobal=false` 定义中，其中 76 项的 `globalShare=true` 对 BQ `hasClaimed` 无效。BQ `fullySyncQuests` 源码默认 false 且注释 `[NYI]`，但配置字段仍由 `QBConfig.init` 读取并被多个代码路径直接使用；S2 磁盘 `questbook.cfg` 为 false，运行时加载/覆盖仍未验证。Checkbox PARTY/TEAM/PUBLIC、重复重置和实机奖励链仍未证明 |
| item reward | 物品交付与恢复 | BLOCKED | 空列表 no-op、变量 NBT 替换、全局 `noRewards` 抑制及 `Count <= 0` 至少生成 1 件已实现；只读 S2 定义含 6,271 个 Item 奖励栈且 Count 全为正，15 项大于 64、最大 8,192；另有 3,062 个 Choice 选项。9,333 个 Item/Choice 栈均为字符串注册名，440 栈带非空 Tag；typed Tag 实际使用类型 1–6、8–11。端到端导入测试现覆盖 typed 数值、嵌套 Compound/List、变量替换和 ByteArray/IntArray 还原，详细见开发导航 2026-10-11 R.2.11 补充；130 件普通物品按 64/64/2 拆分、metadata 与 `VAR_NAME`/`VAR_UUID` 替换、marker 与重放幂等均有回归。BQ `BigItemStack.getCombinedStacks()` 与 Base 生成世界溢出栈均按物品自身 max stack 切分；Base 背包插入再同时服从 1.7.10 inventory slot 的 64 上限。Core/适配器回归覆盖 max stack=128 的 130 件溢出为 128/2，以及 8,192 件普通奖励进入 36 格背包后余量按 64 分栈掉落。仍使用测试玩家替身。插入计划按 `InventoryPlayer.storeItemStack` 对齐 damage/subtype/NBT，并拒绝把已损坏耐久物品并入已有栈。容量不足时，Base 先持久化成功入包部分，再按 BQ 的 `dropPlayerItemWithRandomChoice(stack,false)` 将各剩余栈掉落世界；最终完成 marker 持久化后重放幂等。掉落与最终 marker/租约确认之间的崩溃可能重试掉落；测试替身不能证明 Forge toss、真实 WorldSave 重启或跨服租约下的行为等价。真实玩家存档恢复、重复周期与 WorldSave/多服时序仍待端到端对照 |
| choice reward | 服务端选择与交付；新周期 entitlement 继承同玩家此前有效选择 | BLOCKED | 个人重复周期选择、领取前改选、全局 `noRewards` 抑制及强制随机选择已有 Core/隔离 PostgreSQL 验证；集成用例证明普通领取仍要求显式选择，强制路径在同一事务内优先随机选择 Lootbag 并只排入该选项，重复领取幂等；Minecraft 适配器测试证明只物化被选中的 ItemStack 选项。2026-09-30 的 PARTY Retrieval 隔离 PostgreSQL 链再覆盖三位成员各自选择不同选项、个人领取、JDBC router/worker 投递及按各自选择收到对应物品，证明 PARTY 进度共享未共享 choice index。BQ 的 `GuiQuestLines` 会用显式 `claim-all(forceChoice=true)` 发起随机选择，服务端再校验该玩家的任务可领取状态；这不是管理员专属权限。Base 已把其 per-quest `forceClaimQuest` 语义接入玩家认证终端：仅在任务可领取且存在未选 Choice 时显示“随机选择并领取”，普通“领取全部奖励”仍要求显式选择，UUID、随机结果与资格均由服务端处理；这不等于 BQ 跨多个任务的批量领取 UI 已移植。测试玩家仍为替身；真实背包持久化、进程重启、Forge/WorldSave 与跨服租约时序未证明 |
| command reward | 命令白名单交付 | BLOCKED | Base 默认空白名单拒绝全部命令，当前默认任务库只读扫描未发现该类型；源码语义仍不同：BQ `viaPlayer=false` 使用 `CommandBlockLogic`（发送者名 `BetterQuesting`、玩家坐标/世界），`viaPlayer=true` 使用权限检查恒真的 `AdminExecute`，并排队到后续服务器 START tick。Base 则对允许根命令立即执行，false 使用 Server sender、true 使用真实玩家。安全限制保留；不能将 allowlist 写成“完全兼容”，未来若导入该类型必须逐条审核实际命令、权限、发送者和时序 |
| questcompletion reward | 目标任务完成处理器 | BLOCKED | 真实 308 条引用均设置 `ignoreDisabled=true`，因此不受 BQ `noRewards` 开关影响；在 3,744 个定义中，251 个不同目标有 219 个可解析、32 个不存在（均无 malformed/missing UUID），可解析目标均非重复、非 GLOBAL；来源 `autoClaim=true` 为 288/308、目标为 0/308。固定 BQ `RewardQuestCompletion.claimReward0` 对目标缺失静默 no-op，Base 现将该结果确认投递，避免永久失败/恢复项；R.2.24 有 handler 回归。Base 已有跨周期“曾完成”闩锁。隔离 PostgreSQL worker 链覆盖自动领取来源→完成奖励→个人目标完成，目标奖励保持可领取；R.2.12 验证合成 BQ typed JSON 紧凑 UUID 经 importer 解码、导入奖励经 `JdbcQuestCompletionRewardPort` 完成目标 PLAYER 进度；R.2.14 再验证合成来源 Quest 经个人进度完成、显式 claim、JDBC delivery Worker/router 后调用导入奖励并完成目标，重复 poll 不再租赁且目标权益仅一次。R.2.14 的来源/目标定义是合成构造，并未覆盖完整 DefaultQuests 导入批次。BQ tick 调度与实际运行时仍未对照；full reset、旧周期历史恢复、多人主体和版本迁移仍阻断 |
| scoreboard reward | 计分板交付 | BLOCKED | 固定源码 `RewardScoreboard.claimReward0`：目标不存在时按 `type` 创建 objective，拒绝只读 criteria，`relative=true` 累加、false 覆盖。Base handler 已映射 objective/type/value/relative，并先把绝对目标与 entitlement marker 写入玩家存档，再设置 scoreboard，避免 worker 重试重复累加；现有测试只验证绝对目标和整数饱和，不是实际 Scoreboard/Forge 运行测试。objective 创建冲突、玩家存档/世界存档顺序及线上多服观察仍需回归 |
| xp reward | 在线玩家 XP 交付处理器；持久 entitlement 标记、写盘失败回滚和客户端经验包同步 | BLOCKED | 2026-09-30 只读重扫 S2 `DefaultQuests`：2 项 XP 奖励均为 `isLevels=true`，BQ `RewardXP` 调用 `XPHelper.getLevelXP(amount)` 后增加总 XP；Base 使用同一 1.7.10 分段曲线。既有可替身 XP 端口测试覆盖等级奖励计算、先 flush 后同步、重复租约幂等及 flush 失败回滚；2026-10-10 新增 typed BQ JSON→Importer→`RewardDefinition`→XP handler 回归，分别验证 `isLevels=true/false` 对应 levels/points、状态写入与重放幂等。负 XP 属 BQ 可表达但当前样本未触发的边界；这些测试未使用真实 `EntityPlayerMP`，玩家存档/进程重启、多人租约与 XP 状态持久化仍未端到端证明 |
| 任何扩展类型 | 尚无默认注册 | UNKNOWN | 必须找到 GTNH 实际注册与源码，再实现独立事实和交付入口 |

2026-10-11 状态更正：上表和更早 Worklog 中“服务器实际配置未读取/未知”均应理解为**运行时进程加载与覆盖状态未验证**；S2 磁盘 `questbook.cfg` 已只读检查，当前值为 `fullySyncQuests=false`。BQ 源码的 `[NYI]` 是注释，不代表配置读取和代码分支不存在；`QBConfig.init` 会读取配置，Checkbox、QuestInstance、Retrieval、Fluid 有直接引用。磁盘文件不等同于运行时快照。

2026-09-30 复核 BQ `QuestInstance.detect` 的完成门控：完成且不能提交的 Quest 不再留在 `QuestCache.activeQuests`，因此非重复 Quest 在同周期后续库存中才出现的 optional retrieval 不会继续被检测。Base 的 planner 会持续选择已发布定义，旧 `QuestEngine` 会在已完成进度上继续合并观察值，可能产生 BQ 不会出现的完成后可选进度。本轮已让引擎冻结已完成周期；只有 `beginsNextCycle` 确认开始新重复周期时才接受观察。Core 回归及 PARTY/JDBC 闭环均验证完成后库存即使出现可选物品，已完成成员的可选进度仍不变化。该差异修复不证明完整 active-quest/claim/reset 调度等价，M.2 仍 `BLOCKED`。

兼容矩阵中的 Retrieval“fake-store 测试不证明 JDBC 全链”的描述只针对当时的 producer 单元测试，已由 2026-09-30 随后的 PostgreSQL 集成补充覆盖：真实数据库快照 producer 已连入 QuestRuntimeService 与 JDBC fact/progress 投影，并验证 PARTY 成员的个人完成进度和事实重放去重。随后又将同一真实运行链扩展到 `bq_standard:item`，验证每位 PARTY 成员各自持久化一条 `CLAIMABLE` 物品奖励权益，重放不会重复入账。测试仍未调用 Minecraft ItemStack 投递 handler，也不是 Forge/GTNH 实机验证，因此不能据此把 Retrieval、item reward 或 M.2 状态从 `BLOCKED` 提升。

### 2026-10-09 Retrieval 导入与手动消费恢复闭环

新增根集成回归使用合成 BQ JSON，经真实 importer 生成非消耗 Retrieval 定义，再发布到隔离 PostgreSQL；生产 Party inventory snapshot sink → 评估规划 → `QuestRuntimeService` → JDBC fact/progress 链验证当前 PARTY 成员各自累计观察值并完成。另一条合成 `consume=true` 定义经授权和 `MinecraftPlayerConsumptionPort.planItems` 精确匹配后，将消费提交持久化为 `APPLIED`；测试让事实投影暂不可用，再恢复提交并确认物品只扣一次、进度完成一次、重复请求幂等。Core 251、Postgres 52、Importer 46、根 Quest 定向 25，共 374 项全部执行，0 skipped/failure/error；`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过。测试端口使用合成 Item/库存，不等于真实在线 GTNH 背包、SubmitStation、跨服时序或玩家持久化恢复证据；此闭环仅提高自动回归覆盖，retrieval/optional_retrieval、Reward 与 M.2 继续 `BLOCKED`。

### 2026-10-09 手动 Retrieval 提交向量回归

固定 BQ 源码确认 Quest Detect 会按完整玩家库存顺序扫描并消费；`TileSubmitStation` 则检查单个输入栈，最多 64 件并以 10 tick 节奏调用 `submitItem`。Base UI2 已有“全库存匹配项”和“当前服务端捕获热键栏栈”两种入口，当前产品交互合同不变。本轮新增隔离 PostgreSQL 回归，将合成 BQ `consume=true` JSON 导入并发布，针对两个重叠 NBT 需求各需 4 件：第一次从所选 7 件栈投影实际额度 `[4,3]`，不触碰未选的 64 件栈；第二次仅扣 1 件补足第二需求并完成；重复请求不再扣物品。根 Party/JDBC 类 7 项、Core 提交服务 5 项、根消费/导入适配器 19 项，共 31 项全部执行且无跳过/失败/错误，构建和自包含检查通过。

这仅证明测试数据下 BQ Retrieval 分量顺序、服务器选栈、实际扣除向量和 JDBC 进度闭环；不等价于真实 GTNH ItemStack、站台实体/节奏、跨服在线队伍或玩家重启恢复。BQ SubmitStation 与 Base UI2 入口仍是明示的交互差异，retrieval 与整体 M.2 继续 `BLOCKED`。

### 2026-10-09 手动 Fluid 消费与 PostgreSQL 恢复闭环

新增根隔离 PostgreSQL 回归将合成 consuming `bq_standard:fluid` JSON 导入并发布，确认唯一已支持的 PLAYER 手动消费授权生成剩余额度。生产 `MinecraftPlayerConsumptionPort.planFluidsDetailed` 在合成容器上处理 400 mB 可分割流体；测试在玩家侧库存和模拟幂等 marker 已完成、数据库提交仍为 `PREPARED` 时调用 `resume`，marker 重放仅把 `[400]` 写入 JDBC 一次。第二个不可分割 750 mB 容器面对剩余 600 mB 被整份排出。此处“Base applied vector 只记 `[600]`、总进度封顶 1,000 mB”是 R.2.18 修复前的历史实现；当前实际应用量为 `[750]`、总进度 1,150 mB，已确认提交重放仍不再排流体。

既有容器规划器测试另覆盖精确 Fluid Name、`ignoreNBT=false` 时缺少 Tag 与显式空 compound 不相等、部分可用量、服务端选中栈最多一容器，以及空容器无法放回时拒绝计划且输入库存不变。R.2.18 已修复不可分割容器的超量进度差异。合成适配器和 marker 不测试真实 Forge 注册容器、玩家 NBT 写盘或进程重启；无空位时 Base 的拒绝也有意不同于 BQ SubmitStation 输出槽/库存掉落。

CodeGraph 检查 importer `appendFluids`、服务端 `ConsumptionRequestAuthorizer.authorize`、`MinecraftPlayerConsumptionPort.planFluidsDetailed`、`ForgeQuestFluidContainerAdapter` 与 `ConsumptionSubmissionService.process`。Docker Gradle Core 251、Postgres 52、Importer 46、根消费端口 19、根 Party/JDBC 集成类 8，共 376 项均执行、0 skipped/failure/error；`:assemble :verifyUi2SelfContained` 成功。使用独立 PostgreSQL 16 tmpfs 容器、无宿主卷、仅 loopback 端口；无生产数据库/玩家进度访问、DDL、部署或 Quest/BQ 开关改动。Flow 自动回归提升但真实 Forge/GTNH、BQ 整体兼容与 M.2 仍 `BLOCKED`。后续顺序以开发导航和能力矩阵为准。

### 2026-10-09 非消耗 Fluid 快照与 PARTY 归约

对照固定 BQ `TaskFluid.detect`：非消耗流体任务对每项需求重新扫描同一份未排出的库存，因此重叠需求可复用相同观察流体量；`groupDetect=false` 保留已经满足的需求、仅重算部分需求，`true` 重算全部未完成需求，完成 Task 保持锁存。Base 的 `FluidSnapshotTaskEvaluator` 已产生绝对观察值，但 `QuestEngine` 原先只对非消耗 Retrieval 使用绝对值归约，Fluid 观察仍被通用 `max(old, observed)` 合并，因此库存减少后任务进度不回落。现在引擎将 `factType=galaxy:fluid_inventory_observed` 的非消耗 `bq_standard:fluid` 识别为快照任务；其他任务类型与 consuming Fluid 的单调进度合同不变。

同时修复 PARTY Fluid 观察接线：Fluid-only 任务会建立现有带 session fence 的 inventory lease；同一 lease attributes 同时保留本轮启用的物品/流体观察，避免分别刷新时互相覆盖。只允许携带 BQ 导入标记并匹配该 factType 的 Fluid 任务消费 PARTY 流体聚合。聚合读取未过期成员快照，然后按现有 PARTY fan-out 为成员分别归约 PLAYER 进度；没有新增 DDL、表、网络协议或消费/奖励行为。

自动回归覆盖共享观察量分配、`groupDetect` 两种进度重算、个人/导入任务作用域、Fluid-only 租约计划、Codec 的 NBT Tag 存在性和顺序聚合。隔离 PostgreSQL 用例覆盖鲜活成员 400+500 mB 汇总为 900 mB、同一成员随后降至 300 mB 后 PARTY 总值由 900 回落到 700、过期成员不计入以及每名成员各自保存进度并完成。CodeGraph 对 `FluidSnapshotTaskEvaluator.evaluate`（7 个关联符号）、`BqPartyInventorySnapshotFactSink.publishPartyAggregate`（4）、`QuestEngine.isNonConsumingSnapshotTask`（3）完成影响复核并同步至干净索引。

Docker Gradle 完整 Core 257、Postgres 52、Importer 46、根 `BqPartyInventorySnapshotFactSinkPostgresTest` 9 项，共 364 项，0 skipped/failure/error；`:assemble :verifyUi2SelfContained` 与 `git diff --check` 通过。数据库为 PostgreSQL 16 临时容器 `jgb-r25-quest-it`，tmpfs、无宿主数据卷、仅绑定 `127.0.0.1:55432`，只含合成内容；测试后容器自动退出，确认实例与端口均已释放。未访问生产玩家进度/数据库，未执行生产 DDL、迁移、部署、Quest 开关或停用 BQ。

该闭环只证明 Core 和隔离 JDBC 的非消耗快照/Party 租约合同，不证明 Forge/GTNH 流体容器枚举、跨服在线资格与切服时序、玩家存档或 BQ `activePlayers` 运行时完全等价；Fluid 与 M.2/BQ 替代继续 `BLOCKED`。

### 2026-09-30 item reward 自动测试补充

上表 item reward 行记录的是先前算法级覆盖。当前 `MinecraftRewardDeliveryTest` 新增了驱动生产
`MinecraftItemRewardDeliveryHandler.deliver` 的测试端口：背包插入后幂等标记与物品一同 flush，重复执行不重复发放；
flush 失败回滚主背包和标记并可重试。背包不足策略已在下文的 2026-09-30 复核中改为 BQ 世界掉落。测试发现并修复完整插入
仍返回 `deferred` 的状态计算缺陷。另以真实 `RewardDeliveryWorker` 串接处理器，模拟“玩家持久标记已 flush、数据库租约确认过期”，
再租约重试后只确认而不重复发放。fake `PlayerState` 仍不等同于真实 `EntityPlayerMP` / WorldSave 写盘或进程重启验收；
item reward 仍为 `BLOCKED`。

本轮继续对照 BetterQuesting 3.7.15-GTNH：`RewardItem.claimReward0` 将每项奖励交给 `BigItemStack.getCombinedStacks()`，
而该方法使用 `Math.max(1, stackSize)`，故 `Count <= 0` 也生成 1 件。Importer 原样保存 Count，Base handler 此前却将其跳过；
现已改为与 BQ 一致并添加零数、负数回归测试。只读 S2 定义中的 6,271 个奖励栈数量全部为正，故此修正不改变当前任务书结果。
数量语义闭环仍不代表真实玩家背包持久化、重启恢复或 Forge World toss 路径已验证，item reward 继续 `BLOCKED`。

### 2026-09-30 PARTY item reward 领取与投递链自动验证

隔离 PostgreSQL 集成场景现从 PARTY Retrieval 事实推进到每位成员通过 `AuthenticatedQuestClaimService` 领取个人权益，再由真实 JDBC delivery repository/worker 调用 `MinecraftItemRewardDeliveryHandler`。测试用固定 ItemLookup 和内存 `PlayerState` 端口物化 BQ 形状的 diamond 奖励：三名成员各收到 2 件，持久 marker 与各自 entitlement key 一一对应并 flush，数据库权益均转为 `DELIVERED`；重复领取、worker 重跑和源 inventory fact 重放都不重复发放。

这补上了“权益入账之后，领取状态、数据库租约和 Minecraft ItemStack 适配器是否能串起来”的自动证据，但玩家、世界和持久化仍是测试替身。它不验证真实 `EntityPlayerMP` 玩家存档写盘/进程重启、跨服玩家租约、GTNH 物品 NBT 和世界溢出掉落时序，不能据此将 item reward 或 Retrieval 提升为 SUPPORTED；M.2 与整体 BQ 替代仍为 `BLOCKED`。

2026-09-30 再对照 `RewardItem.claimReward0` 与 `EntityPlayer.dropPlayerItemWithRandomChoice`：BQ 先尝试将每个拆分栈加入主背包，
若仍有剩余则直接调用 Forge toss 路径，再由 `QuestInstance.claimReward` 写入 claimed 状态。Base 已按此正常路径加入剩余栈掉落；
在掉落前先持久化入包数量，掉落后持久化已发数量/marker，失败重试可避免常规重复。世界实体与玩家 NBT/DB 租约并非原子，
故崩溃窗口的重复掉落仍为待验证风险，不把隔离端口测试称作 exactly-once。

### 2026-09-30 PARTY Retrieval → item 奖励权益入账集成补充

隔离 PostgreSQL 场景现把生产 `BqPartyInventorySnapshotFactSink` 连到 `QuestRuntimeService`、真实 Planner、JDBC PARTY 成员解析与
JDBC runtime repository。非消耗 Retrieval 完成后，测试直接查询持久化 entitlement 表：三名 party 成员各有一条 PLAYER 主体的
`bq_standard:item` / `CLAIMABLE` 权益，recipient 与个人任务主体一致，奖励物品注册名和数量与 BQ importer 的 item 参数合同一致；
同一快照重放后仍只有三条权益。此测试验证的是奖励资格的持久化生成与事实幂等，不是奖励已经送进 Minecraft 背包；实际 ItemStack
物化、玩家数据写盘/重启和多服投递仍需分别验收，故 item reward 与整体 M.2 维持 `BLOCKED`。

## 首轮源码对照

本地 `Reference/BetterQuesting/src/main/java/bq_standard/tasks/TaskRetrieval.java` 的 `Detector` 在非消耗
模式下始终为 `pInfo.ALL_UUIDS` 读取独立玩家进度，并遍历 `pInfo.ACTIVE_PLAYERS` 的库存；这一点与
`groupDetect` 无关。每个活跃库存栈都会分别为每个 UUID 计算需求分配。普通模式只清零未达标的部分进度并保留已达标分量，
`groupDetect=true` 则在扫描前清零所有进度向量；若任意 UUID 的向量达标，BQ 会为该 Task 将所有 UUID 标记完成。
Base 的 `InventorySnapshotTaskEvaluator` 已覆盖单个绝对快照的进度回落规则，但现有 `InventorySnapshotScope`
把聚合快照路由至非玩家共享进度，与 BQ 的“成员独立进度 + 聚合库存观察 + Task 完成扇出”并不等价，不能据此宣称
BQ retrieval 的 PARTY 语义已实现。此前 Core 中的共享 scope 路由仍只是另一种 Base 主体合同。

沿 `QuestEngine` 发现通用单调合并会覆盖 evaluator 的库存回落结果，因此所有非消耗检索快照现在均采用“替换未完成分量、完成任务锁存”；
普通 evaluator 保留已达标项，`groupDetect` evaluator 则重算它们。消耗型任务继续累计已提交扣除量。引擎级测试覆盖
单人普通检测与显式聚合快照的进度回落以及完成锁存。经用户授权只读扫描 S2
`DefaultQuests`，确认 `groupDetect=1` 的 4 个 retrieval 定义为 `AvengersAssemble`、`RefinedFirestone`、
`AlloySmelter`、`PrecisionLaserEn`，均为非消耗任务。Core 现在接受显式标记、绑定当前 participant 的服务端聚合
快照；planner 只把它路由至匹配的群组 scope，projector 拒绝单人快照或其他 participant 的快照；但该合同不是 BQ 导入任务所需的
逐玩家进度 fan-out。后续已添加带会话 fencing 的 Minecraft producer 与短期 PostgreSQL lease，按当前服务端 PARTY 成员关系汇总各服在线成员快照，并以同一事务提交 PARTY 聚合事实和进度投影；这补齐了代码路径，但不是运行环境验证。四个任务的 `globalShare=1` 不会被 importer 或 Base
擅自映射成 PARTY/TEAM/PUBLIC；其 BQ 全局共享行为与新系统主体语义的对应关系仍需专门核定。消耗提交身份/周期绑定、
多人库存和活跃成员快照、自动检测时机、OreDictionary/NBT
运行边界与真实闭环仍待验证，因此 retrieval 与 optional_retrieval 继续为 `BLOCKED`。源码比对另确认 BQ
`ItemComparison.StackMatch` 在同一 damageable Item 上忽略 damage/meta；Base 库存事实现传递 `damageable` 并由
evaluator 按 registry name 限定匹配，正反例已覆盖，详见下方 2026-09-29 对照记录。

2026-09-30 再核对 S2 `DefaultQuests` 与 BQ 源码后，确认快照基线为 3,744 个 Quest 定义文件、5,620 个 Task 实例、47 个章节、3,847 个放置，
`sourceHash=9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`；retrieval 为 3,863 项，其中
307 项消耗、4 项启用 `groupDetect`，optional retrieval 为 1,267 项且全部非消耗、无 `groupDetect`。BQ 的
`globalShare` 不是 party inventory/progress 开关；`QuestInstance.hasClaimed` 只在 `isGlobal && !globalShare`
分支改变奖励领取检查。Core 现新增受限消费合同：只有服务端明确标记
`inventory.bqPartySnapshot=true` 并绑定 `inventory.bqPartyId`、且当前玩家仍属于 resolver 返回的该 PARTY，planner
才将非消耗 retrieval/optional_retrieval 路由到 PARTY 中每个成员的个人进度；每名成员仍独立采用自己的向量与前置 Quest，
任一成员满足时仅扇出该 Task 的完成标记。源玩家未满足自己的前置条件时不生成请求；被扇出的锁定成员可保存 Task 完成，
但不会提前完成 Quest 或生成奖励权益。本轮新建 Minecraft producer：登录时领取会话租约，每 20 ticks 用同一会话 token 更新；数据库以 15 秒到期时间判断活跃性，读取前锁定并重新解析 PARTY 成员，聚合仍有效的各服成员快照，再与个人事实及进度投影放在同一 PostgreSQL 事务。过期/断线快照不参与，旧会话 tick 与 logout 不可覆盖或删除新会话；事实账本对库存事实只保留数量/脱敏标志，短租约仅存 evaluator 所需的规范化 registry/meta/amount/OreDict/portable NBT，不存原始 NBT 文本。新增迁移尚未在任何生产库执行，Quest 生产开关也未启用；隔离测试仅验证租约到期、会话 fencing、数据裁剪与数据库约束，没有多服 Minecraft 实机验证。因此 retrieval 与 optional_retrieval 仍为 `BLOCKED`。不能用“把玩家 Party 当共享 progress row”替代，也不能将本代码路径误报为运行时兼容完成。
2026-09-30 新增生产 `BqPartyInventorySnapshotFactSink` 的 JDBC 集成覆盖：真实 PostgreSQL membership、租约仓储与事务管理器证明过期成员快照不会进入 PARTY 聚合，且下游处理异常会回滚登录租约。随后把生产快照 sink 接到正式 `QuestRuntimeService`、`QuestEvaluationPlanner` 与 JDBC fact/progress repository：两名在线成员的有效库存经过 PARTY 聚合后，为当前 PARTY 的每个成员写入独立 PLAYER Retrieval 进度；同一触发快照重放应被 fact ledger 去重。该端到端测试首次暴露聚合事实每次重建都会生成新 event ID 的幂等缺陷；现改为从不可变源库存事实 ID 与 PARTY ID 派生稳定 event ID，并沿用源观察时间。全部测试只在独立临时 PostgreSQL 16（tmpfs、独立网络、无宿主端口）运行。它验证 Base producer→运行时投影的数据库链，不证明 Forge 玩家库存枚举、登录/20 tick 调度、跨服网络、GTNH Item/NBT 或 BQ 多服实机行为；检索类型与整体 M.2 继续 `BLOCKED`。

`Reference/BetterQuesting` 当前没有独立 `.git` 元数据，尚不能从本地目录证明
文档所述固定提交；后续行为签收需要可验证的上游提交或固定源码包校验值。

本地 BQ `RewardItem.claimReward0` 对空 `items` 不产生物品；Base 的 `plan(before, empty)` 同样不变更
背包并可完成稳定 entitlement。BQ `BigItemStack.getCombinedStacks()` 按每种物品的最大堆叠数拆分，并把
非正 stackSize 也规范成至少一个物品；但 BQ `RewardChoice.claimReward0` 在调用该方法前会拒绝所选项的非正
数量。Base 对 `bq_standard:item` 保持至少发放一个的语义，并在 R.2.11.1 将 Choice 的非正数量改为无物品
发放；交付仍以空奖励成功完成并留下幂等 marker。S2 DefaultQuests 静态画像中 957 个 Choice 奖励含 3,062
个选项，数量均为正（1–4,096），因此该修正不改变现有任务内容，仅对齐未来/异常定义。BQ 通过
`InventoryPlayer.addItemStackToInventory`
只使用主背包；Base 的快照/回滚现改为只覆盖 `mainInventory`，不把护甲槽当作奖励空间。BQ 主背包满时把
剩余物品掉到世界。Base 当前实现并非“整批拒绝”：它先把可放入的部分和分项进度写入玩家存档，再逐栈掉落溢出，最后持久化完成 marker；正常返回时与 BQ 的满背包可见结果接近。世界掉落与玩家 NBT/PostgreSQL 租约不是原子事务：崩溃或投递异常窗口可能重复掉落，也不能证明一次掉落已被世界存档持久化。延期到邮箱/稍后领取会改变 BQ 满背包行为，尚未获准替换此语义。两者都递归替换 NBT 中的 `VAR_NAME`/`VAR_UUID` 字符串；Base 已有纯 Java/Minecraft 边界测试，但真实物品注册、模组堆叠上限和服务端领取周期仍未集成验证。

### 2026-10-10 奖励投递源码复核（R.2.11）

本轮按固定 BetterQuesting 源码核对奖励实现与 Base 适配：`RewardItem`/`BigItemStack` 负责数量拆分、NBT 变量替换、背包插入和剩余物品掉落；`RewardChoice` 只投递玩家已选中的一项，随机选择优先考虑 Lootbag，且非正数量会在拆栈前直接无操作返回；`RewardXP` 以 `XPHelper` 换算等级奖励；`RewardScoreboard` 创建/复用目标、拒绝只读 criteria，再按 relative 累加或覆盖；`RewardQuestCompletion` 只在目标任务存在且当前玩家未完成时标记目标完成；`RewardCommand` 的发送者权限、世界/坐标和延迟执行与 Base 当前实现不同。`AbstractReward` 的全局 `noRewards` 门控与 `ignoreDisabled` 默认值已在既有策略和导入器中处理。

获授权 S2 `DefaultQuests` 副本（sourceHash `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`）实际包含 Item 4,181、Choice 957、QuestCompletion 308、XP 2 个奖励；没有 Command、Scoreboard 或未知类型。3,062 个 Choice 选项数量均为正，范围 1–4,096。源对照发现 Base 曾把 Item 的 `BigItemStack` 最小数量规则错误地共用于 Choice；R.2.11.1 已改为 Choice 非正数量无物品、Item 仍至少一个，并增加交付 marker/重放回归。Base 的 Item 溢出逻辑已有逐部分进度持久化和重试覆盖，但 Minecraft 世界掉落实体与玩家存档、PostgreSQL entitlement 之间没有原子提交；这不是可由普通重复测试消除的 exactly-once 问题。Base Command 为安全采用默认拒绝和根命令 allowlist，故意不执行 BQ 的任意管理员命令；同时 sender 与调度时机不同。由于当前副本没有 Command reward，该限制不影响当前任务库内容，但会阻断包含该类型的通用导入。Scoreboard 已有稳定绝对目标 marker 的适配代码，当前测试只覆盖纯计算，不足以签收真实 Minecraft Scoreboard 写入。

2026-10-10 补充 Choice、QuestCompletion 与 XP 导入回归：Choice 的 PARTY/JDBC 用例改由 BQ typed JSON 经 importer 生成奖励定义，覆盖认证个人选择/领取和 worker 投递；直连 Choice adapter 测试中的 `choice.index` 仍由测试直接注入。QuestCompletion 测试覆盖紧凑目标 UUID 导入与隔离 JDBC 目标完成；R.2.14 再接入合成来源进度、claim 与 JDBC worker/router，但来源及目标 Quest 定义仍为合成构造，非完整 DefaultQuests 导入。XP 测试覆盖导入 `isLevels` 到 adapter 的 levels/points 语义，不代表真实玩家实体或存档。

CodeGraph 起始 `status` 为 up-to-date；本轮 `explore/node` 检查 importer、Reward handlers、Claim/Delivery Worker 与 JDBC Completion port；`impact` 分别覆盖 Choice/QuestCompletion/XP 入口，`affected` 选择直接回归类。对 S2 目录只使用先前授权的匿名聚合值，未读取玩家进度。R.2.12 的 14 项（Choice adapter 2、隔离 PARTY/JDBC 类 12）、R.2.13 的 31 项（导入 XP 3、既有 XP handler 类 28）及 R.2.14 的 13 项隔离 PARTY/JDBC 测试均实际执行且 0 skipped/failure/error，详见 Worklog。以上仅补窄的自动化导入/适配证据；奖励整体、真实 Forge/GTNH 投递、世界掉落与玩家存档原子性仍未签收，R.2.11–R.2.14/M.2 不因此整体完成。后续按既定 M.2 实际行为矩阵继续；命令权限、奖励溢出崩溃取舍或 Scoreboard/WorldSave 持久化边界仍需单独评估，不静默换语义。

本地 BQ `EnumLogic` 支持 AND/NAND/OR/NOR/XOR/XNOR，`QuestInstance.isUnlocked()` 对非空前置调用
`getResult()`。快照实际仅用 AND 3,621、OR 111、XOR 12；任务逻辑仅用 AND 3,608、OR 136。
XOR 的目标是恰好一个前置完成，而不是全部完成。选择环的组外前置提供初始入口；结构上可导入，
但选择互斥在完成后、跨服主体与可见性方面还需行为签收，故仍不启用生产任务。

### 选择奖励的真实配置与周期差异

只读扫描上述服务端任务定义：957 个 `bq_standard:choice` 分别位于 957 个任务中；每项有 2–16 个
有效选项（2/3/4 项分别为 276/408/203 个），没有空选项。全部 `autoClaim=0`、
`ignoreDisabled=0`；947 项不可重复，10 项可重复且均为相对计时。814 项至少含一个
`enhancedlootbags` 选项。这些是当前任务书的分布，不是对其它任务包的泛化保证。

BQ 的 `RewardChoice.canClaim()` 要求该 Questing UUID 已选择有效索引；普通领取不会代选。`RewardChoice.setSelection(UUID,int)`
会直接覆盖 UUID 对应的选项索引，选项界面可在领取前再次点选，因此选择并非一经保存就不可变。
`QuestInstance.claimReward(..., true)` 的强制领取则调用 `selectRandomChoice()`：未选时优先从
Lootbag 选项随机选，否则从全部选项随机选。当前任务书无自动领取，因此普通完成流程应明确要求
玩家选择，不能把强制随机路径当默认行为。BQ 选择保存在奖励对象的 UUID→索引映射中；
`QuestInstance.resetUser()` 只重置任务和领取状态，没有清除这个映射，所以重复周期默认沿用旧选择。
Base 选择记录以 entitlement 为键。现已在同一玩家、任务定义版本、奖励 key 的新周期 entitlement
创建时，从最近一个更早周期复制选择；只继承仍落在新选项范围内的索引。找不到旧选择或旧索引已失效时，
仍返回 `NEEDS_CHOICE`，不自动随机代选。这与 BQ 重复任务沿用奖励对象内玩家选择的通常路径一致；但 Base
选择写入 PostgreSQL、跨进程重启仍持久，而 BQ `selected` 映射不是 NBT 持久字段，重启后的表现不同。

`QuestPostgresIntegrationTest.repeatedChoiceRetainsSelectionAndKeepsEachCycleDeliveryIsolated` 在临时隔离
PostgreSQL 16 上验证：第 0 周期选择后可领取并交付；第 1 周期继承同一有效选项，可直接领取并只投递
该周期奖励；旧周期再次领取返回 `ALREADY_CLAIMED`。另一未曾选择的玩家在后续周期仍得到
`NEEDS_CHOICE`。Core 174 项、PostgreSQL 29 项测试均通过，包含完整 PostgreSQL 集成套件（0 跳过、0 失败、
0 错误）。这证明 Base 的个人重复周期选择链路，不证明多人主体、强制随机领取或真实 GTNH 交付等价。

Base 当前领取仓储会阻止尚未选择的 choice 从 `CLAIMABLE` 转为 `PENDING`，投递租约也跳过无选择
记录的 choice；这是正确的防误发边界。Base 现在允许玩家在权益仍为 `CLAIMABLE` 时改选，并以同一 entitlement 行锁
串行化选择更新和领取；一旦领取进入 `PENDING`/投递阶段，选择即锁定，避免已排队的奖励与界面选项不一致。这个锁定时点是
相对 BQ GUI 的安全差异：BQ 允许覆盖内存选择，但 Base 在开始发奖后不接受改变。多人 Questing UUID 到显式领取人的映射仍未完整对照。强制领取会在真正运行奖励前，
为尚未选择的 choice 优先随机选择 `enhancedlootbags` 项（若无则在全部选项中随机），而非默认发放完整 choices；当前 Base
正常玩家领取必须由玩家选择，不能将强制路径误当作默认路径。

2026-09-30 根据用户授权，以只读方式重扫 S2 `config/betterquesting/DefaultQuests`：源摘要仍为
`9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，3,744 个 Quest 定义文件、5,620 个 Task 实例、47 个章节、3,847 个放置；
choice reward 仍为 957 项。只扫描任务与章节定义，不读玩家进度、物品或数据库。

Base 新增 `AuthenticatedQuestClaimService.forceClaimWithBqRandomChoices` 作为服务端内部切片：在 `QuestTransaction`
中先以 `RewardClaimRepository` 锁定同一领取目标并预检；仅遇到 `NEEDS_CHOICE` 才逐奖励随机选项；若存在
`enhancedlootbags` 选项则在该子集均匀随机，否则在全部选项均匀随机；已进入领取流程的既有选择通过 `CONFLICT` 保留；全部选择成功后
才再次领取。任一异常选择或最终领取状态不符合预期会抛出内部中止信号，使 JDBC 事务回滚。普通 `claim()` 完全不调用该逻辑。
`QuestModule` 为服务注入与领取仓储共享事务上下文的 JDBC 选择仓储。现已将该能力接入独立的玩家认证终端动作：详情页只有在当前快照显示任务可领取、且至少有一项 Choice 未选时显示“随机选择并领取”；此时普通领取显示为禁用的“请先选择奖励”，完成显式选择后才恢复普通领取。此处只移植每项任务的 `forceClaimQuest` 语义，不代表 BQ 跨任务批量领取 UI 已移植。
本地 BQ 源码显示该语义不是管理员专属权限：`GuiQuestLines.claimAll(..., true)` 发送 force-claim 请求，服务端
`NetQuestAction.forceClaimQuest` 仍以当前连接玩家检查 `canClaim(player, true)`，然后调用 `claimReward(player, true)`。
Base 客户端也只发送任务 ID 与浏览上下文；`TerminalService` 从服务端认证连接取得玩家身份，claim service 再解析 recipient/version/cycle 并随机选择，不接受客户端提供 recipient/version/cycle/choice。普通领取与强制领取走不同动作键。
`QuestPostgresIntegrationTest.forceClaimWithBqRandomChoicesPersistsSelectionAndQueuesOnlyThatOption`
已在 tmpfs 的临时 PostgreSQL 16 和独立 Docker 网络中通过：先验证普通领取返回 `NEEDS_CHOICE` 且没有可租约奖励，再验证
强制路径持久化唯一 `enhancedlootbags` 选择、领取状态变为已排队，并且交付租约仅携带 `choice.index=1`；重复强制领取返回
`ALREADY_CLAIMED`。这只证明个人目标的 Core/JDBC 事务路径，不覆盖权限门控、多玩家/队伍身份映射、真实 GTNH 奖励物品调用或重启恢复。
本轮终端动作映射、缺运行时失败关闭、共享 UI2 三档视口的普通/随机按钮互斥与点击回调也已通过定向测试；仍无真实 GTNH 玩家奖励库存/存档闭环证据。

2026-09-30 对照 `RewardChoice.setSelection(UUID,int)` 及选项按钮的网络动作，确认 BQ 在领取前允许覆盖同一玩家当前选项。
Base 的玩家任务详情现允许对 `CLAIMABLE` Choice 再次点选；仓储锁定 entitlement 行，与领取事务串行化，并以 `RESELECTED`
表示更新成功。进入领取流程后，同一选择不可再变更；自动领取 `PENDING`/重试 `FAILED` 且尚无选择的 entitlement 仍允许服务端
首次补选，但此通道仅供服务端 force-claim（`selectForForcedClaim`）；玩家终端动作只能对 `CLAIMABLE` entitlement 新选或改选。已有选择只能原样重试。
这是为防止已排队奖励与最新选项不一致保留的安全差异。UI 只在 Choice entitlement 可领取时
开放选项输入。根项目与 UI2/Core/PostgreSQL 的自动验证已通过，完整 PostgreSQL 集成类在独立临时数据库执行；该验证不解除
真实 Minecraft 玩家交付、玩家存档恢复及多人主体映射等剩余阻断。

BQ 背包满会掉落物品，Base 会保留
可重试权益，仍是有意的安全差异。上述差异不应通过简单放开 `requireRuntimeReady()` 掩盖。

### Quest Completion 奖励：BQ 生命周期与 Base 跨周期闩锁

本地 `RewardQuestCompletion.claimReward0` 只在目标任务存在、目标玩家缓存可用且 `targetQuest.isComplete(uuid)` 为 false
时调用 `setComplete(uuid, now)` 并标记目标任务脏；它不直接调用目标任务奖励处理器。BQ `QuestInstance.isComplete` 对个人
任务检查该 UUID 的 completion entry，对 `GLOBAL` 任务则检查是否存在任一 completion entry。常规重复周期的
`resetUser(uuid, false)` 保留 entry，只把领取标记和时间戳清零，因此目标任务只要曾完成过，之后的新周期仍会被
`questcompletion` 视为已完成；`resetUser(..., true)` 才删除 entry。

Base 只保存 participant/quest/version 的最新进度行，重复周期 upsert 会覆盖上一周期的 `COMPLETED` 状态。此前
`JdbcQuestCompletionRewardPort` 只看当前行，故在新周期内可能再次被 completion reward 强制完成。现已增加
`galaxy_quest_progress.ever_completed` 持久闩锁：每次 `COMPLETED` 写入时置真，后续新周期进度更新只能保留真值；
completion reward 在同一事务和进度身份锁下先查询该闩锁，再决定是否首次完成目标。新增迁移
`ops/sql/migrations/20260930_002_add_quest_lifetime_completion_latch.sql`，只回填迁移当时仍处于
`COMPLETED` 的行。此前已经被新周期覆盖的历史完成无法由现有最新状态表还原，因此旧数据迁移后仍可能与 BQ 的历史
completion entry 不同；本轮没有执行迁移或连接生产数据库。

尚未宣称等价的行为包括 full reset 清除 BQ completion entry 与 Base 生命周期闩锁的对应操作、被新周期覆盖的旧完成历史恢复、
跨服成员归属，以及真实 Minecraft 运行时/调度时序。新增隔离 PostgreSQL 链路测试把自动领取的来源任务完成后生成的
`bq_standard:questcompletion` 权益送入真实 `RewardDeliveryWorker` 与 JDBC port：接收玩家的个人目标变为已完成，目标自身的
`autoClaim=false` 奖励仍保持可领取而未被 worker 自动发出。此处验证的是 Base 的完整数据库奖励链，不是 BQ 每秒 tick 与
实际服务端调度的等价证明。故 308 项 `questcompletion` 仍保持 `BLOCKED`。

隔离 PostgreSQL 16 回归新增“第 0 周期由 completion reward 完成目标，第 1 周期已重置后再次请求仍返回
`ALREADY_COMPLETED`、进度和奖励权益均不被覆盖”的测试。Core 与 PostgreSQL 模块本轮共 253 项通过，PostgreSQL
集成类 29 项全部执行、0 跳过。另在独立临时 PostgreSQL 16 库上把迁移应用两次：旧 `COMPLETED` 行回填为 true、
`IN_PROGRESS` 保持 false，第二次执行无副作用。生产迁移、运行数据库、部署与 Quest 开关均未触碰。

2026-09-30 对获授权的 S2 `DefaultQuests` 只读目录新增完成奖励引用聚合画像：308 条引用全部解析成功，涉及
67 个来源任务与 251 个唯一目标任务；无格式错误、缺失目标或自引用。来源和目标的这些引用均指向非重复、非 GLOBAL
任务。按引用次数计，来源 `autoClaim=true` 为 288/308、目标为 0/308；来源 `globalShare=true` 为 26/308，目标为
117/308。后两个计数不是唯一任务数；`globalShare` 不等于 GLOBAL，也不能推出队伍主体语义。该画像只输出聚合计数，
没有任务名或 UUID，也未读取玩家进度、其它服务器配置、物品 NBT 或数据库。来源 autoClaim 分布说明大多数来源完成后
会自动结算其 completion reward，而目标任务本身仍不自动领取其奖励；不能仅以一个 autoClaim 标志概括整条奖励链。

当前 importer 单独保留 BQ `isGlobal/globalShare`，没有把它们映射成 PARTY/TEAM；`QuestBehavior` 的 participant scope
仍为默认 PLAYER。新增隔离 PostgreSQL 用例使用此当前默认映射及非重复目标，验证完成奖励只完成接收玩家的任务、不为另一
玩家创建进度，且目标奖励只生成一份权益；该定向用例通过。清点器合成测试 4 项全部通过。统计缩小了当前样本的验证范围，
却不验证 full reset、完成/奖励事务边界或恢复旧历史；因此 `questcompletion` 仍为 `BLOCKED`。

### 全局 noRewards 的局部实现门禁

服务端 `quest.noRewards` 默认为 `false`，与 BQ `BQ_Settings.noRewards` 默认值一致。Core 在自动领取奖励生成时
将符合条件的权益标为 `SUPPRESSED`；手动领取会在事务中锁定符合条件的权益并写入该终态；`ignoreDisabled=true`
的奖励继续进入正常投递。重复点击已抑制奖励返回已处理状态，抑制记录不会被 delivery worker 租用。任务中心摘要可
区分全抑制与部分抑制。BQ 数字布尔 `0/1` 由既有 `QuestBooleanParameter` 解析，显式值覆盖类型默认值。

为避免旧数据库检查约束拒绝受抑制状态，`noRewards=true` 时 Quest 模块先只读确认数据库含 `SUPPRESSED` 约束；
缺少约束会拒绝启动 Quest runtime，并提示迁移文件。迁移只新增状态白名单，本轮未执行。验证使用 Core 单测、模块
启动门禁测试和临时隔离 PostgreSQL；生产 Quest 开关未启用。该实现只闭合全局抑制语义，不代表 item/choice/XP
的真实 GTNH 发放、组队归属、强制随机选择、重试恢复或 BQ 全量替代已经完成。

### 已确认的自动观察与显式提交边界（2026-10-10）

用户确认的检测/提交边界是：非消耗物品/流体任务持续由服务端观察玩家背包或已明确接入的玩家容器快照；消耗型库存任务需要玩家显式提交/检测。GT 机器产物如果没有玩家 craft/smelt/anvil 事件，应按玩家可观察到的物品/流体存在条件建任务，不伪造 Crafting 事件。BQ 实际 GUI 是 Quest 级按钮，并由 `QuestInstance.detect` 顺序处理未完成 Tasks。**本段在 2026-10-10 写作时 Base 仍是逐 Task 提交；用户随后选择单一 Quest 级 Detect，R.2.17 已实现持久批次，当前进度见后文。**Base 当前观察计划只为非消耗 Retrieval/Fluid 开启周期快照；`QuestGameplayEventHandler` 当前每 20 个玩家 tick 采集 `EntityPlayer.inventory`，其它容器须有明确适配器才纳入，不扫描任意 GT 机器库存。导入 BQ Crafting 统计除匹配产物即时复评外，还按 60 玩家 tick 周期做任务级自动统计复评；每份事实写入对应玩家自己的 PLAYER 进度，不合并不同 Party 成员。该调度是 Base 实现，不构成真实 Forge/GTNH tick 等价证明。

Quest 参与主体仍按 Quest 级选择和既有 BQ Party 规则处理。用户明确将“同一 Quest 内部分 Task 个人、部分 Task Party 共享”的模型扩展推迟至职业系统设计阶段；本批不得借 Checkbox 或检测动作引入任务级归属。

2026-10-10 归并口径更正（历史状态）：当时已确认不相加的是不同 Party 成员的个人计数，但同一玩家多来源服如何合并尚未确认。2026-10-11 用户明确选择来源服分别取 `max(事件累计, 绝对统计快照)` 后求和；对应实现与 Core/JDBC 回归见本文件后文。下方更早 R.2.6 记录中“跨服不累加”一类措辞仅记载当时状态，受此最终决策覆盖。

### 复选任务的服务端边界

真实任务书使用 `bq_standard:checkbox` 203 次，均无额外 Task 配置、均非重复任务且均关闭
`autoClaim`；其中 76 项所在任务启用 `globalShare`。BQ `NetTaskCheckbox` 从服务端认证的发送者
取得 `ParticipantInfo`，校验目标任务的指定 Task 确为 Checkbox 后标记完成；`fullySyncQuests=false`
时只写发送者的 Questing UUID，开启时写 `ParticipantInfo.ALL_UUIDS`（队伍全体成员，而非仅在线成员）。
本地 BQ 源码中的 `QBConfig.fullySyncQuests` 默认值为 `false`，注释同时标记该“完全同步”选项为
`NYI`；该字段仍由 `QBConfig.init` 从 Forge Configuration 读取，且 `NetTaskCheckbox`、`QuestInstance`、
`TaskRetrieval` 与 `TaskFluid` 存在直接分支使用它，因此 `[NYI]` 注释不能解释为代码路径必定无效。S2
磁盘 `config/betterquesting/questbook.cfg` 的值为 `false`；这只证明文件内容，不证明服务器进程已加载该值
或运行中未被覆盖。`globalShare` 不等价于此设置，也不能单独推导 Checkbox 的主体或奖励接收者。

Base 的确认事实绑定 Quest UUID、定义版本、Task key 与 Checkbox 类型，避免不同任务复用 `task-0`
时串改进度；重复确认以玩家、Quest、版本、周期和 Task key 幂等。此次对照还发现锁定任务的确认
曾可能先落幂等事实、再被 `QuestEngine` 拒绝推进，导致解锁后同一个稳定事件键仍被当作重复。现在
`QuestRuntimeService.applyIf` 在同一事务内根据同一请求/前置快照先检查资格，再记录事实；锁定且未启用
`progressWhileLocked` 时返回 ignored，不消耗事件键。终端详情相应隐藏锁定 Checkbox 的确认按钮；显式
允许锁定进度的定义仍可确认。

BQ 的网络处理器本身没有前置解锁校验，Checkbox 面板在任务未完成时可点击；但锁定任务详情是否在
实际界面可见未验证。因此 Base 采取“锁定时不提供确认操作”的安全边界，不宣称与 BQ 对伪造/锁定
点击的底层行为完全一致。当前仍仅支持 PLAYER 主体；BQ 开启 `fullySyncQuests` 时对整队写入、
PARTY/TEAM/PUBLIC 进度与奖励归属、重复周期重置以及真实 GTNH 交互/奖励交付尚未端到端证明，故不将
203 项 Checkbox 标为可运行，M.2 继续 `BLOCKED`。

2026-10-10 新增隔离 PostgreSQL 导入链 `importedBqCheckboxIsAnAuthenticatedManualPlayerAction`：合成 BQ Checkbox JSON 导入并发布后，使用生产 `AuthenticatedCheckboxConfirmationService`、真实 JDBC Planner/Runtime 完成认证点击者的 PLAYER 进度；验证另一玩家不被写入、重复确认不重复完成，且 Checkbox 不启用库存/流体轮询。Core 267、Importer 48 共 315 项全部实际执行，0 skipped/failure/error；assemble、自包含检查、`git diff --check` 通过。测试只证明 Base 个人确认链，不覆盖 `fullySyncQuests` 的服务端实际配置、PARTY/TEAM/PUBLIC Checkbox、Forge 终端消息或跨服时序，故不提升 Checkbox/M.2 状态。

### 2026-09-29 - 手动消耗型物品检索的受限实现

用户确认可直接读取服务端 quest 定义后，重新用只读挂载的 S2 `DefaultQuests` 执行行为清点：3,744 个 Quest 定义文件、5,620 个 Task 实例、
47 个章节、3,847 个章节放置；来源清单哈希仍为
`9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。3,863 个 retrieval 中 307 个
配置 `consume=true`，全部 `autoConsume=false`；optional_retrieval 的 1,267 个都不消耗。此分布说明当前
高频需求是玩家显式提交，而不是自动扣包。源目录只读；未读玩家进度、数据库或个人物品数据。

对照本地 `TaskRetrieval` 的逐需求扣除方式，为终端新增“提交并消耗”入口，但服务端重新读取已发布定义和
进度，客户端只传任务 UUID/Task key，玩家身份只来自当前已认证连接。首轮允许范围严格限定为：个人
`PLAYER` 主体、不可重复任务、`retrieval`/`optional_retrieval`、`consume=true`、`autoConsume=false`。
消耗端按需求顺序扣除当前玩家在线背包，记录实际每项扣除量，再用该实际数量推进向量进度；玩家存档中的
幂等 marker 与 PostgreSQL `PREPARED/APPLIED/CONFIRMED` 记录配合。新增登录恢复：玩家回到发起提交的源服时，
最多读取并恢复 64 条该玩家、该源服的待处理项；不会在玩家离线时把 `PREPARED` 错误拒绝。该改动未新增
数据库结构，且没有执行生产 DDL。

这是受限代码实现，不是 BQ 行为签收。自动消耗、共享主体/`groupDetect`、重复任务、流体、XP、NBT/物品注册
差异、跨服往返恢复和真实 GTNH 背包保存时序仍需隔离环境或实机验证；本轮没有部署，Quest 开关和 BQ 均未
切换。因此 retrieval/optional_retrieval、整个 M.2 以及 BQ 替代状态继续为 `BLOCKED`。

### 2026-09-29 - crafting 可损耗物品匹配与重复周期对照

只读 `DefaultQuests` 行为清单中有 133 个 `bq_standard:crafting`：全部 `allowCraft=true`、
`allowSmelt=true`、`allowAnvil=false`、`partialMatch=true`；`ignoreNBT=false` 87 项、true 46 项；
`allowCraftedFromStatistics=true` 6 项、false 127 项。没有读取玩家任务进度或数据库。

对照本地 BQ `TaskCrafting.onItemInternal` 与 `betterquesting.api.utils.ItemComparison.StackMatch`：
匹配项会逐个推进所有符合的需求（不是先分配/消耗产物）；启用熔炼时按事件产量累计，BQ 对损坏工具有
特殊兼容规则——同一 Item 只要是 `isDamageable()`，metadata/damage 不同也仍匹配。Base evaluator 之前仅接受
精确 metadata 或通配值。现在生产物品事实携带 `damageable`，纯 Java evaluator 按 BQ 的同物品可损耗规则
匹配；统计观察也填入该属性。新增正/反例测试，防止把不损坏的不同变体错误合并。

重复周期源码确认 `TaskProgressableBase.resetUser` 清空每项累计向量，而 `TaskCrafting.detect` 的统计分支读取
玩家生命周期 `StatList.objectCraftStats[itemId]` 绝对计数，且这条 fallback 不检查 metadata、NBT 或 OreDictionary。
Base 原先复用了普通事件匹配，会在有 NBT 的需求上错误拒绝统计快照；现在统计通道仅按同一 registry/Item ID
匹配，新增 NBT/metadata 不同的绝对计数用例。新周期中再次运行检测时，历史统计可以直接满足需求。Base 的通用
周期引擎会重置向量并重新应用绝对统计快照，新增 crafting-specific 周期/奖励资格测试。

此前 Base 每 20 ticks 无条件轮询统计，与 BQ 的检测触发时机不一致；现已移除该无条件轮询，改为只在
craft/smelt/anvil 产物事件后附带统计快照。事实携带触发产物与渠道，evaluator 要求产物匹配当前任务至少一个
需求且该渠道在任务配置中启用，之后才应用该任务全部需求的绝对统计值。测试覆盖不匹配产物、关闭的熔炼渠道和
匹配合成。之后增加了服务端认证的手动“检测合成”入口，使个人任务可在没有新产物事件时读取既有累计值；
该入口按单个任务触发，尚未达到 BQ 的 Quest 级 Detect 语义，也未实机证明 GTNH 统计文件的读取/写入时序。

本轮是源码算法对照和 Base 单测，不是 Minecraft/GTNH 运行证明。项目早期文档记录了 BetterQuesting 上游参考
提交 `524b365211b6b3a9672cab8ae45b4e07e726d49e`，但当前 `Reference/BetterQuesting` 没有独立 Git 元数据，
该 commit object 也不在本仓库对象库中，故目前不能证明目录内容正是该提交。实际 `ItemStack`、NBT、
OreDictionary 注册和 GTNH 事件顺序未在运行时验证。故 133 项 crafting 全部维持 `BLOCKED`。

### 2026-09-30 - 普通 crafting 事件的 PARTY 个人进度扇出

按用户授权，只读挂载 S2 `config/betterquesting/DefaultQuests` 目录并运行定义行为清点；输出过滤为聚合数据，不包含任务名、UUID、玩家进度、NBT 或其他服务端配置。摘要哈希仍为
`9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，133 个 crafting 的 `allowCraft=1`、
`allowSmelt=1`、`allowAnvil=0`，`allowCraftedFromStatistics=1` 为 6 项。

对照本地 BQ `TaskCrafting.onItemInternal`：普通合成/熔炼/铁砧事件为 `ParticipantInfo.ALL_UUIDS` 中每人单独推进
向量；事件处理器只遍历在线队员 active quest 的并集。Base 现为启用对应直接事件渠道的导入 crafting 任务增加可信 PARTY
到每位成员独立 PLAYER 进度的扇出；仅当 PARTY 中至少一人满足前置条件才规划该任务，锁定成员允许积累任务进度但不提前完成
Quest/生成权益；统计检测通道不扇出，继续按被服务端认证的个人任务执行。测试覆盖渠道旗标、无已解锁成员、Party 事实归属、锁定成员
进度暂存与解锁后奖励。

这是算法层进展，不等同 BQ 等价：Base 目前不能读取 BQ 每名在线队员的 `activeQuests` 并集及 `canSubmit`/完成领取状态，
使用前置完成状态近似；尤其“只有离线队员解锁、在线触发者仍锁定”的场景可能与 BQ 不同。常规 GTNH 合成事件、PARTY
成员在线变化、存档恢复和奖励时序尚未在隔离运行时验证，所以 crafting 133 项和 M.2 仍为 `BLOCKED`。

### 2026-09-30 - Location 实际配置与 PARTY 完成语义

按用户授权，对 S2 `DefaultQuests` 只读运行位置配置聚合；输出仅含频率和值域分类，不含任务名、UUID、坐标或结构名称。33 个 Task occurrence 的 dimension 分布为 `-1:2, 1:2, 28:2, 41:2, 55:13`，其他 12 个维度各 1 项；全部均为 `biome=-1`、`range<=0`、空 structure、`visible=false`、`invert=false`、`taxiCabDist=false`、原点 target。重复/领取画像为：`repeatable=false:22`、`true:11`；11 个重复 Task occurrence 全有奖励且 `autoClaim=false`；间隔为 `relative-0:4` 与 `relative-1,728,000 ticks:7`（24 小时）；无奖励 2 项、均在不重复组。全部 33 项 `lockedProgress=false`，`globalShare=true:28`、`false:5`。计数均按 location Task occurrence，不应误读为唯一 Quest 数。

对照 `TaskLocation.internalDetect`、`tickTask`、标准 `EventHandler.onEntityLiving`、`ParticipantInfo.getSharedQuests` 与 `QuestCache.updateCache`：位置检查每 100 个在线服务端玩家 tick 发生；BQ 只对在线 party 成员 `activeQuests` 的并集调用 tick，而 Quest 是否 active 还取决于解锁/`lockedProgress` 和 `canSubmit`。命中时 Location 将完成标记写给 `ALL_UUIDS`；重复重置则在奖励 `hasClaimed` 后才排程，以领取时保存的 timestamp 作为相对/固定周期锚点，无奖励任务的 `hasClaimed` 直接为 true。所有 11 个真实重复位置项都要求玩家手动领取，因此未领取门控是实际路径而非理论边缘。

Base 的 `LocationTaskEvaluator` 已对齐维度、正范围、群系、结构、视线和 invert 短路；位置观察仍每 100 tick 采样。可信 PARTY 的导入 PLAYER 任务会给每位成员分别推进，锁定成员可保存 Task 完成但不发 Quest 权益。重复周期现从 `galaxy_quest_reward_entitlement` 检查当前 cycle 是否仍有 `CLAIMABLE` 权益；未领取时即使冷却届满也不开始下一周期，领取后以数据库 `claimed_at` 作为 cooldown anchor，未配置奖励时回退到完成时间。新增的 Core 周期测试覆盖“冷却先到但尚未领取”“领取后从领取时间重新计时”及 PARTY 每人独立周期；PostgreSQL 集成测试检查 `CLAIMABLE` 变为已领取后 anchor 更新。

仍不能称为等价：Base 的 `hasUnlockedPartyMember` 只是 prerequisite 近似，不持有 BQ 在线成员 `activeQuests`、`canSubmit` 与领奖缓存快照；在线/离线成员组合可能改变某位置观察是否被 BQ 检测。Base `autoClaim` 权益在创建时即进入 PENDING，而 BQ auto-claim 经事件队列发生，时间点近似但不完全相同。真实清单当前不含 structure/visibility，但未来启用时仍缺对应 Minecraft 证据桥；GTNH tick、领取、party 在线变化及存档恢复没有实机/隔离运行时证明。Location 和整体 M.2 继续 `BLOCKED`。

### 2026-09-29 - BQ 数字布尔配置解析修复与重复消耗阻断

复核真实 DefaultQuests 时确认，BQ NBT JSON 把任务行为布尔值序列化为数字 `0/1`。此前导入器将这些配置保留为
字符串 `"0"`/`"1"`，而多处运行时使用 `Boolean.parseBoolean`；Java 会把 `"1"` 解析为 false。尤其是
`consume=1`、`groupDetect=1`、`ignoreNBT=1`、`partialMatch=1` 和 `allowCraftedFromStatistics=1` 会被读反，
可能影响提交消耗授权、观察计划、任务检测及 NBT 匹配。BQ 定义里的原始 JSON 仍完整保存在 `bq.raw` 中。

Core 新增兼容数字和文本形式的 `QuestBooleanParameter`，覆盖任务 evaluator、planner、进度投影、观察计划、
消耗授权和奖励处理；Minecraft 消耗匹配与计分板奖励也使用同一解析规则。Importer 对已知 BQ 布尔选项进行严格
规范化，数据库参数写成 `true/false`，遇到无法解释的布尔值就拒绝导入而不猜测；`isLevels` 和默认
`ignoreDisabled` 同样走标准布尔语义。行为清单将实际布尔选项归一化统计，并覆盖位置、交互、子类与奖励执行标志。
单测覆盖 `0/1`、文本形式、NBT 部分匹配、消耗观测分流、XP/统计观察计划和原始 JSON 保留。

同一实际快照显示，3,863 个 retrieval 中有 228 个是“重复周期 + 手动消耗”，另有 79 个“不可重复 + 手动消耗”；
307 个消耗型 retrieval 全部 `autoConsume=false`。本节编写时，授权器仍拒绝重复手动物品任务。随后在
“2026-09-29 - 重复手动消耗检索的周期绑定与恢复保护”中，已为个人主体绑定请求周期、保护旧周期 saga，且在隔离
PostgreSQL 16 集成测试验证周期查询；这些 228 项仍不能签收，因为真实 Forge 物品/NBT 匹配和跨服 marker 恢复链
尚未端到端证明，也没有修改生产 DDL。

本轮 Docker Gradle `:galaxy-quest-core:test` 通过；根项目仅定向运行
`MinecraftPlayerConsumptionPortTest` 和 `MinecraftRewardDeliveryTest`，16 项通过。Importer 完整 Gradle 测试在
最后补入 scoreboard `relative` 规范化断言前曾通过；最后源码状态下在线及离线 Gradle 均在 GTNH convention
插件初始化阶段超时，因此使用同一 Docker JDK 容器 `javac --release 8` 重编全部 importer 主源码及三个相关测试类，
JUnit 4.13.2 共 11 项通过。CodeGraph 同步后，
`QuestBooleanParameter.parse` impact 覆盖 59 个符号，`BqStandardCodec.scalarOptions` impact 覆盖 importer 测试；
`codegraph affected` 未识别到测试，故显式运行上述模块和适配测试。`git diff --check` 通过。
这次没有读取玩家进度、数据库或物品数据；没有连接生产数据库、执行 DDL、部署、启用 Quest 或停用 BQ。
所有 M.2 类型和整体 BQ 替代状态仍为 `BLOCKED`。

### 2026-09-29 - 重复手动消耗检索的周期绑定与恢复保护

源码对照 `TaskRetrieval.submitItem`、`QuestInstance.resetUser` 与 `EventHandler` 的重复任务调度：手动提交同步扣除物品
并推进任务向量；冷却到期后 BQ 才清空本轮任务进度。Base 的扣除是可恢复 saga，若服务器在物品扣除后、确认事实前
中断，恢复过程必须仍绑定原周期，不能把旧提交计入新周期。

`ConsumptionRequest` 现在把周期编号写进已有 `resource_parameters_json` 保留键，并在确认事实中带回该周期；
无需数据库 schema 变更。授权器开放个人 `retrieval`/`optional_retrieval` 的重复任务手动提交，并阻止同题同周期在
前一提交处于 `PREPARED`/`APPLIED` 时再次扣除。`QuestRuntimeService` 在进度身份锁内查询同周期待恢复提交，必要时
让 `QuestEngine` 暂缓清空已完成周期。稳定提交键重试会先读取并核对原请求的玩家、来源服、任务和题目，避免周期
切换后重新授权出不同资源请求。

Core 新增“冷却已到但存在待恢复提交时保持原周期、提交完成后下一事实才创建新周期”的回归，以及重复检索从旧周期
进入新周期并完成奖励资格的回归。PostgreSQL 集成用例在隔离 PostgreSQL 16 上验证 JSON 周期键的同周期/异周期查询，
并确认状态转为 `CONFIRMED` 后不再视为待处理。使用独立 Docker 网络 `quest-m2-it-20260929` 和临时数据库容器，
没有连接运行中的 `galaxy-base` 服务；测试容器与网络已在测试后停止并移除。Core 179 项、PostgreSQL 29 项
（其中真实 PostgreSQL 集成 20 项）均通过，0 跳过、0 失败、0 错误。CodeGraph `affected` 命中 PostgreSQL 集成测试；
结合调用影响范围补跑了 Core 与 PostgreSQL 全模块测试。旧格式待处理行没有周期键，读取时按 cycle 0 兼容；Quest 生产启用
前必须在隔离/受控流程中确认不存在跨过周期的旧待处理请求。整个 retrieval/optional_retrieval 仍 `BLOCKED`：
活跃成员库存聚合、真实 Forge Item/NBT/OreDictionary 与物品 marker 重启恢复尚未端到端证明；没有部署、生产 DDL、玩家
进度读取或 Quest 开关变化。

### 2026-09-29 - 接入个人 crafting 统计显式检测（未实机签收）

- 用户授权读取服务端任务定义后，只读复核 S2 `DefaultQuests`：133 项 crafting 中 6 项启用
  `allowCraftedFromStatistics`。对照 `TaskCrafting.detect`：BQ 使用在线参与者的 `StatList.objectCraftStats`
  绝对累计值，仅在进度不足时更新目标分量；统计分支按 Item ID 判断，不按 metadata、NBT 或 OreDictionary 复核。
  BQ 网络 Detect 面向 Quest 并遍历该 Quest 的所有任务。
- Base 新增只接受服务端认证玩家、已发布个人任务、明确选中的 stats-enabled crafting task 的检测服务；只读
  Minecraft lifetime craft statistic，事实绑定 Quest/version/task/cycle，阻止投射到其他任务。支持可用重复周期
  使用累计统计；GROUP/global/globalShare、未解锁任务及未启用统计参数均拒绝。当时 UI 显示“检测合成”动作；2026-10-10 按用户确认已移除此手动入口，旧服务端动作保留兼容。
- 统计快照事实键按玩家、定义版本、周期、任务、物品和累计数稳定生成，重复点击不重复写入；累计值变化产生新事实。
  新增覆盖精确任务隔离、重复周期、拒绝共享/未启用任务、前置条件和重复点击幂等的 Core 测试。
- CodeGraph 同步后服务影响 34 个符号（稳定事实键修订后 23 个）；终端 Mapper 影响 23 个符号，网络入口影响
  102 个符号。`affected` 找到 Core 检测服务测试；另显式跑 Mapper、QuestModule、TerminalService、协议编解码
  和 UI2 Quest Center 测试。
- 验证：Core 检测用例 5 项通过；根终端/Quest 相关 5 个测试类共 49 项通过；`:ui2-terminal:test` 通过；
  根 Minecraft 主源码及测试源码编译通过。没有执行全量测试、`assemble`、部署或生产数据库操作。
- 仍为 `BLOCKED`：Minecraft 统计桥只完成编译和 API 源码级核对，未在 GTNH 运行实例验证统计文件是否及时含有
  当前玩家数据；BQ 是 Quest 级 Detect、Base 目前是任务级按钮，交互语义有明确差异；共享参与者/跨服 UUID
  统计扇出未实现。未读取玩家进度或资产；未部署、未启用 Base Quest、未停用 BetterQuesting，也未触碰 S1/S2 运行状态。

### 2026-09-30 - BQ 普通 NBT 列表重复匹配语义

- 对照本地 `Reference/BetterQuesting/.../betterquesting/api/utils/ItemComparison.java` 的
  `CompareNBTTag`：NBTTagList 分支逐个遍历需求元素、每次都从完整样本列表搜索，未记录已使用的样本下标；因此
  重复需求可以复用同一个匹配样本。相比之下，同文件的 byte/int array 分支显式记录 `usedIdxs`，要求不同需求值
  匹配不同数组位置。此前 Base 将三种容器都按不复用元素处理，与 BQ 不一致。
- `PortableNbtMatcher` 现在分别实现：普通 NBT 列表逐项搜索且允许复用；byte/int array 保留唯一位置匹配。
  新增重复 compound 列表正例及两种数组反例。普通列表规则可能使重复、部分匹配的任务比“多重集合匹配”更宽松；
  这是为了忠实复现现有 BQ 行为，需在正式启用前作为明确的兼容/安全差异审查。下方只读结构扫描在
  `ignoreNBT=0`、`partialMatch=1` 的 retrieval 必需物品需求中发现 3 处完全重复的 compound 列表条目；这只证明
  配置形状存在，不据此推断受影响任务数或玩家运行时结果。`Reference/BetterQuesting` 仍无可验证 Git 元数据，
  来源版本需后续固定。
- 重新只读运行 S2 `DefaultQuests` 的 `behaviorInventory`（目录只读挂载）：源摘要仍为
  `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，3,744 个 Quest 定义文件、5,620 个 Task 实例、47 个章节、3,847 个放置；
  与上次频率清单一致。没有读取玩家进度、物品、其它配置或数据库，也未修改服务端文件。
- 对同一只读 S2 `DefaultQuests/Quests` 定义做结构审计，仅统计 `retrieval / optional_retrieval / crafting` 中
  `ignoreNBT=0` 且 `partialMatch=1` 的任务：分别选中 1,834 / 577 / 87 个任务；其中带非空需求 NBT 的物品栈
  为 418 / 105 / 7 个，通用 NBTTagList 节点为 247 / 72 / 5 个；只在 retrieval 检出 3 处完全重复的
  compound 列表条目，另两类为 0。此审计按 JSON 中显式类型标签遍历，仅识别结构完全相同的重复 compound，不能
  统计语义等价但编码不同的需求，也不代表已完成 GTNH 运行时兼容验收。
- CodeGraph 改前 `impact PortableNbtMatcher.matches` 覆盖 35 个符号，包括库存/制作/流体/实体/方块匹配器、
  消耗口和 NBT codec 测试；新增 evaluator 级回归用例前 `impact InventorySnapshotTaskEvaluator.evaluate`
  覆盖 19 个符号。同步后 `affected` 命中 `PortableNbtMatcherTest` 与
  `BqCompatibleTaskEvaluatorsTest`。Docker Gradle `:galaxy-quest-core:test` 现为 190 项通过、0 跳过、0 failure、
  0 error；根 `MinecraftPortableNbtCodecTest` 与 `MinecraftPlayerConsumptionPortTest` 前序验证共 9 项通过。
  没有部署或连接生产数据库。

### 2026-09-30 - BQ Hunt 计数、重复周期与组队差异

- 对照 `bq_standard/tasks/TaskHunt.java` 与 `bq_standard/handlers/EventHandler.java`：BQ 只处理玩家造成的未取消
  `LivingDeathEvent`；按 `EntityList.stringToClassMapping` 做目标实体精确匹配或 Java 类子类匹配，按忽略大小写的
  `damageType` 过滤；`ignoreNBT=false` 时用 `CompareNBTTag(targetTags, subjectTags, true)` 做需求子集匹配。
  合格击杀把每个 UUID 的计数加 1 并封顶到 required，遍历的是 `ParticipantInfo.ALL_UUIDS`；任务进度仍是各玩家
  独立记录，重复任务重置后从新周期重新计数。BQ 的 `globalShare` 不代表该击杀 fan-out：源码只在 `isGlobal &&
  !globalShare` 时改变全局任务的 `hasClaimed` 语义。
- 对 S2 只读 `DefaultQuests/Quests` 的 104 个 hunt 配置审计：103 个 `subtypes=true`、1 个 false；2 个启用
  NBT 比较，所有 `damageType` 为空；58 个存有非空 `targetNBT`，但其余大多数因 `ignoreNBT=true` 不参与匹配；
  84 个 hunt 项的所属 Quest 配置了重复周期（按 hunt 项计）。此审计没有输出或记录 Quest UUID、名称、目标实体或原始 NBT。
- Base 已有 `SubjectCountTaskEvaluator` 和服务端玩家击杀 fact：映射精确 registry ID、服务端生成的可兼容实体别名、
  damageType 与 portable 部分 NBT；Core 已覆盖子类匹配、伤害过滤和 NBT 正反例。新增 `BqHuntPartyAssignmentPolicy` 后，
  事件规划器只对匹配到的 `bq_standard:hunt` fact 扩展目标：读取服务端 PARTY 成员快照，为每位成员建立独立 PLAYER
  进度请求，并把该 PARTY 快照作为事实来源证明；没有 PARTY 时仍只推进击杀者。普通任务、TEAM/PARTY 聚合 scope、
  Checkbox 和消费提交流程不受影响。奖励随各自 PLAYER 完成分别建立权益，重复事实仍由同一 fact key 幂等拦截。
  `globalShare` 仍不是 Hunt party 开关。
- 新增通过 `QuestRuntimeService`、Core BQ hunt evaluator 与 `QuestEngine` 的重复周期回归：目标为 2 的击杀任务在
  周期 0 完成；冷却到期前继续完成锁存；到期后的第一击杀从 1/2 开始，第二击杀才完成周期 1 并产生新权益。该测试
  先在修复前复现“新周期首个击杀直接完成”，随后将周期切换判定统一为 `QuestEngine.beginsNextCycle`，并让 runtime
  在新周期用空进度投影当前事实，避免沿用已完成周期的累积事件计数。
- 新增 party planner 与 runtime 回归：一次击杀为服务端 PARTY 快照内的触发成员和其他成员各推进个人进度；前置 Quest
  完成查询按每个玩家分别执行；完成时各自创建一份奖励权益；相同事实再次送达不会重复进度或奖励。额外验证非 Hunt
  任务、无 PARTY 以及显式 PARTY 聚合 scope 不被错误扩展。请求与投影器均校验 PARTY 来源包含事实玩家和目标进度玩家。
- 新增 PostgreSQL 集成回归，使用隔离 schema 写入 Base PARTY 成员，经真实 `JdbcParticipantMembershipResolver` 构建 Hunt
  planner；模拟 S2 与 Lobby 同时发生的两条击杀事实，验证两个 PLAYER 进度各累计两次、只产生两份个人 reward
  entitlement，同一 fact 重放会去重。测试连接到临时一次性 PostgreSQL 16 容器，不使用项目运行数据库。
- CodeGraph `impact QuestRuntimeService.execute` 覆盖 3 个符号、`impact QuestEngine.beginsNextCycle` 覆盖 6 个；
  `impact BqHuntPartyAssignmentPolicy`、`impact QuestEvaluationPlanner.requestsFor`、`impact QuestFactProjector.project`
  与 `affected` 将回归范围指向两个 Core 测试文件及 PostgreSQL 集成测试文件。隔离纯 Java harness 运行
  `:galaxy-quest-core:test` 194 项及 `:galaxy-quest-postgres:test` 35 项，共 229 项，0 跳过、失败或错误；
  根项目 `compileJava` 通过。未运行全仓 `test` / `assemble`。
  隔离 PostgreSQL 测试 schema 与临时容器均已删除；未连接项目运行数据库、部署或改变 S1/S2 状态。Hunt 仍需 GTNH
  实体/NBT 与实际跨服事件链验收。

### 2026-09-30 - optional retrieval 完成门槛与手动提交入口语义

- 对照 BQ `TaskOptionalRetrieval.ignored(UUID)` 与 `QuestInstance.update/detect`：optional retrieval 即使自身未完成也计入
  Quest 的逻辑完成数，并从 Detect 的有效任务总数中排除。Base 导入器按 `optional_retrieval` 类型生成 `optional=true`，
  `QuestEngine` 对可选任务不增加 required count；新增 Core 回归，证明必需 checkbox 完成时未完成的 optional retrieval 不阻挡
  Quest 完成，且可选任务进度仍保留为未完成。`QuestEngineTest` 定向 Gradle 测试 15 项通过，0 跳过/失败/错误。
- 另对照 BQ `TaskRetrieval.submitItem` 和 `TileSubmitStation.updateEntity`：SubmitStation 接收一个输入 ItemStack，最多
  64 个，并以 10 tick 间隔处理；BQ 的 Quest 级 `detect` 则会按任务扫描玩家库存。Base 当前终端的“提交并消耗”动作由
  `MinecraftPlayerConsumptionPort.planItems` 扫描整份库存，因此其扣除范围更接近任务级 Detect，而不是逐栈 SubmitStation。
 不能把两种行为笼统记成已等价；同日后续切片增加了明确的逐栈入口，细节见下节。
- CodeGraph 当前索引已同步；本轮仅修改 `QuestEngineTest`，依赖范围由 `QuestEngine.evaluate` 的调用图锁定在该测试类。
  首次以普通 Docker 开发用户编译时，已有 root-owned 的生成 class 触发文件替换拒绝；随后仅在 `galaxy-dev` 临时 root
  运行同一 QuestEngine 定向测试，并将 `galaxy-quest-core/build` 生成物所有权恢复给 uid 1000。未改源码权限或其他 build 目录。
  `git diff --check` 通过。未运行数据库、全仓测试或部署；retrieval / optional_retrieval 及整个 M.2 仍为 `BLOCKED`。

### 2026-09-30 - S2 任务定义复核与单栈提交闭环

- 按用户本轮授权，将服务端 S2 的 `config/betterquesting/DefaultQuests` 以只读卷挂入隔离开发容器，仅读取任务定义；未读玩家进度、个人物品 NBT 或数据库，也未修改服务端文件。清点再次得到 3,744 个 Quest 定义文件、5,620 个 Task 实例、47 个章节、3,847 个章节放置，适用性为 true；类型与配置分布和本文件前述基线一致。清点器已打印完整类型聚合，但 Gradle 最终以 `SocketTimeoutException` 退出，所以不把该次运行记作构建成功。
- 保留原“提交背包匹配项”全库存消耗路径，并增加“提交并消耗手持栈”逐栈路径：服务器从已认证玩家读取当前热键栏槽位及该栈 NBT 指纹；客户端不提供槽位、物品或数量。Core 把槽位/指纹放进既有 `resource_parameters_json`，恢复时重新核对同一槽位仍是同一栈，不匹配就拒绝扣除；单栈数量受 Minecraft / SubmitStation 的 64 上限约束。没有增加数据库列或执行 DDL。
- 发现原按钮把会扣除全库存匹配物品的动作标成“检测背包”，容易误导玩家；现在两个入口都明确写明“提交并消耗”，服务端反馈也不再称为单纯检测。旧全库存路径的消费范围与 Quest 级扫描相似，但扣除/提交时机仍不是 BQ 的逐栈站台流程；新入口复现单个热键栏栈范围，不模拟站台实体或 10 tick 传送节奏。
- 单测覆盖手持栈只消耗所选槽、跨栈同类物品保持不动、64 上限和指纹失配拒绝；隔离 PostgreSQL 集成测试覆盖选择槽与指纹在准备、落库重读后的持久往返。`:galaxy-quest-core:test` 206 项、`:ui2-terminal:test` 24 项均为 0 跳过/失败/错误；根 `compileJava` / `compileTestJava` 成功（`--no-configuration-cache`）。隔离 PostgreSQL 16 上定向测试 1 项通过，0 跳过/失败/错误；未连接 `galaxy-base`。
- CodeGraph 对 `JdbcConsumptionSubmissionRepository.prepareIfAbsent` 的影响范围为 21 个符号、对 `QuestCenterVisualDocument.detail` 为 3 个符号；`affected` 命中 `QuestPostgresIntegrationTest`，UI 文档路径没有自动关联测试，因此补跑整个 `ui2-terminal` 模块。首次构建曾被预存 root-owned 的测试 class 阻断；后续仅在隔离容器运行，并只恢复相关生成 `build/` 产物到 uid 1000。
- retrieval / optional_retrieval、M.2 和 BQ 替代仍为 `BLOCKED`：尚未证明真实 Forge 物品保存/断线往返/服务端启动恢复、多服实机时序、SubmitStation 与 Quest Detect 的全部边界或跨服主体行为。本轮无部署、生产 DDL、运行数据库访问、Quest 开关变化或 S1/S2 服务状态变化。

### 2026-09-30 - 检索与制作物品 NBT 匹配分支对照

本轮直接核对本地参考源码 `Reference/BetterQuesting/src/main/java/betterquesting/api/utils/ItemComparison.java`、
`bq_standard/tasks/TaskRetrieval.java` 与 `TaskCrafting.java`。本地参考目录没有独立 Git 元数据，故只把它当作现有源码副本，
不声称已证明与历史记录的上游 commit 完全一致。

BQ 的 `StackMatch` 以需求栈 NBT 为左侧参数、观察栈为右侧参数；任务随后 OR 一个独立 `OreDictionaryMatch`，后者在
`CompareNBTTagCompound` 中传入观察栈标签在前、需求标签在后。空 NBT 也不是“需求侧通配”：BQ 将 null/空 compound/空 list
视为同类空值，只允许双方均空时通过。Base 之前的两项差异是：库存和事件 evaluator 对空需求 NBT 直接放行；OreDictionary
命中后沿用直接物品分支的 NBT 方向。本轮已分别修正 `InventorySnapshotTaskEvaluator`、`ItemVectorTaskEvaluator` 和
`MinecraftPlayerConsumptionPort`，并覆盖普通/消耗 retrieval、crafting 事件与适配器分支。

本轮只读重跑 S2 `DefaultQuests`，并只输出计数画像，不记录物品名、任务 ID 或原始 NBT：

| 类型 | 物品需求数 | NBT 检查开启 | 需求 NBT 为空 | 需求 NBT 非空 | 含 OreDictionary | OreDictionary 且检查 NBT | OreDictionary、检查 NBT 且需求 NBT 非空 |
|---|---:|---:|---:|---:|---:|---:|---:|
| retrieval | 5,539 | 2,761 | 2,343 | 418 | 84 | 49 | 0 |
| optional_retrieval | 3,274 | 1,432 | 1,327 | 105 | 168 | 117 | 0 |

因此当前真实定义大量启用了“空需求 NBT”的比较；旧 Base 快捷放行确有高频配置风险。反向 OreDictionary NBT 规则是为了源码
一致性实现，但上表没有发现“OreDictionary + NBT 检查 + 非空需求标签”组合，不宣称其已在当前任务定义中触发。该清点不模拟
实际玩家物品注册、NBT 比较、OreDictionary 注册顺序或 Forge 运行时，因此两个检索类型继续 `BLOCKED`。

验证：隔离 Core `BqCompatibleTaskEvaluatorsTest` 29 项、BQ importer `BqBehaviorInventoryTest` 3 项、根
`MinecraftPlayerConsumptionPortTest` 12 项，共 44 项，零跳过/失败/错误。根测试使用一次性 LOCAL no-op Spotless 配置并跳过
联网许可证准备；临时文件已移除。`affected` 未自动给出测试候选，测试范围按 CodeGraph `impact` 的 evaluator/适配器调用链显式
选定。全量 root suite、`assemble` 与 Forge 实机没有在本轮执行；NBT/物品兼容不因这些定向用例升级状态。

## 下一步门禁

1. R.2.29 已用根回归验证：旧周期 Item reward 部分投递 → 压缩玩家 NBT 重建 → 新周期权益先完成 → 旧周期延迟恢复时 marker/部分进度隔离。剩余门槛是实际 Forge `World` 掉落实体与玩家存档故障窗口、跨服玩家数据/entitlement 租约往返；数据库周期查询已有隔离 PostgreSQL 测试，但完整物品恢复链仍未签收。
2. 对真实 XOR 互斥组补全完成后的可见性、互斥及跨服主体测试；确认重复周期和奖励不会越界。
3. Crafting 的交互合同现已确认：统计不设手动 Detect，沿匹配产物与 Base 60 tick 自动复评；消耗任务统一由 Quest 级 Detect 逐项提交；来源内 max/跨来源求和与个人 Party 进度也已确定。不要再把这些作为待选产品问题。仍未闭合的是 R.2.19 延迟队列中 Party 归属取事件时还是重试时；在改变进度归属前必须取得用户选择。CodeGraph/合成回归只能验证代码边界，真实 Forge `objectCraftStats`、事件注册和在线时序继续阻断等价签收。
4. 高频非消耗 Retrieval 的 importer→PARTY inventory snapshot→Planner/Runtime/JDBC、`groupDetect` 汇总替换已有隔离端到端覆盖；R.2.30 又覆盖多个 consuming Retrieval 按定义顺序进行 Quest Detect、Task 间中断后恢复与批次末结算。不要再把通用 importer→JDBC 用例写成缺失。剩余门槛转为真实 Forge/GTNH ItemStack、NBT/注册表和背包时序验收，以及逐项核对重复周期；观察站相邻库存是独立差异，不在本步擅自扩展。
5. 奖励逐类验证交付与失败恢复；不能安全等价的配置保持阻断并记录差异。
6. 完成隔离自动门禁后再更新本矩阵；不以类型注册数量代替行为覆盖率。
7. 继续验证 fluid 的真实 1.7.10 `IFluidContainerItem` / `FluidContainerRegistry` 适配、NBT 语义、部分进度与失败回滚；确认 `fullySyncQuests` 和 SubmitStation / Quest Detect 的边界后才能从 `BLOCKED` 签收。

### 2026-10-11 - R.2.30 多消耗 Retrieval Quest Detect 顺序与恢复闭环

固定 BQ `QuestInstance.detect` 按定义顺序调用未完成 Task，整轮结束后再刷新 Quest 完成状态；本次据用户已确认的 Quest 级 Detect 合同补端到端自动证据，不改运行时实现。测试从 typed BQ JSON 导入 OR Quest 与两个不同物品的消耗 Retrieval，在隔离 PostgreSQL 发布定义；首个 Task 通过合成 `MinecraftPlayerConsumptionPort.planItems` 适配器扣除并投影后，在第二 Task 扣除前注入故障。断言首个 Task 已完成、第二个未完成、Quest 因批次未结束仍保持 `IN_PROGRESS`，持久批次游标停在第二项；重试从该项恢复，沿用原 submission key、不重复扣首项，批次末两个 Task 均完成且 Quest 才进入 `COMPLETED`。

CodeGraph 起始索引 up-to-date；`explore`/`impact` 核对 `BqQuestDefinitionImporter.read`、`AuthenticatedQuestConsumptionDetectService.detect`、`MinecraftPlayerConsumptionPort.planItems`、JDBC batch 与 runtime 调用链；改后 `sync` 更新测试文件 221 nodes，`affected` 仅选根 `BqPartyInventorySnapshotFactSinkPostgresTest`。一次性 PostgreSQL 16 仅在 internal Docker 网络可达、tmpfs 512 MiB、无宿主数据卷/端口映射；先单测新增用例 1/1，再全量重跑该集成类 16/16，0 skipped/failure/error。每个隔离 schema 在 teardown 后计数为 0，随后移除临时数据库容器与网络。测试环境初始化输出有 Minecraft Log4j 对只读 `logs/latest.log` 的权限诊断，但 JUnit 全部通过、无测试失败。

仅新增测试和文档；无生产 Java、数据库合同或迁移变化。未跑全量 `test`、`assemble`、自包含门禁、真实 Forge/GTNH、生产数据库/DDL、玩家进度读取、部署或 Quest/BQ 开关。真实背包存档崩溃边界、R.2.19 Party 重试归属及 Detect 发布新定义并发仍未收口；M.2 继续 `BLOCKED`。

### 2026-10-11 - R.2.29 重复周期 Item reward 延迟恢复隔离

以真实 Core `RepeatPolicy` 与 `RewardEntitlement` 生成相同 Quest/reward 的 cycle 0、cycle 1 权益。cycle 0 在 36 格库存仅余一格时先写入 64 个奖励并持久化部分进度，模拟 overflow drop 失败；随后将背包 `InventoryPlayer`/`ItemStack` 与 `ForgeData/PlayerPersisted` 通过 1.7.10 Minecraft `CompressedStreamTools` 压缩 NBT 序列化并重建 PlayerState。重启后先交付 cycle 1，再延迟恢复 cycle 0，断言两组 marker/进度独立，物品总量为 cycle0 70 + cycle1 70，没有重复或丢失。

CodeGraph `status` 起始 up-to-date；`explore`/`impact` 覆盖 `MinecraftItemRewardDeliveryHandler.deliver/deliverOverflow`、`QuestRewardMarker.key`、`RewardEntitlement.getEntitlementKey` 与 Worker，改后 `sync` 更新测试文件 127 nodes，`affected` 选中唯一根测试文件。Docker 根 `:test` 定向 `MinecraftRewardDeliveryTest` 29 项 + `BqItemRewardImportDeliveryTest` 5 项，共 34 项通过、0 skipped/failure/error。第一次 `--tests` 误作用于全部子项目造成无匹配过滤失败，离线 `prepareUi2QzLicense` 也因 raw GitHub DNS 失败；修正为根 `:test` 并按既有本地流程跳过联网准备后完成。第一版新断言把奖励物品同时用作 filler，数量统计夹具错误；改用独立注册物品后通过。

本回归不代表实际 Forge `World` 的 ItemEntity 保存与玩家 NBT/数据库 entitlement 原子，未模拟服务器进程崩溃或玩家跨服文件/租约竞争。特别是“World drop 已生成但最终玩家 marker 尚未保存”仍可能重复生成掉落，且不能在不改奖励策略/交付协议的情况下声称 exactly-once。未改生产代码、未跑完整根套件或打包门禁、未触碰生产数据库/DDL/部署/Quest 开关；Item reward、M.2 与 BQ 替代继续 `BLOCKED`。

### 2026-09-30 - 修正非消耗 Retrieval 的 PARTY 库存与 groupDetect 语义

本轮对照本地 `Reference/BetterQuesting/.../TaskRetrieval.java` 的 `detect`、`Detector` 与完成检查：所有非消耗
Retrieval 都读取当前 `ACTIVE_PLAYERS` 的库存；`groupDetect` 只影响进度向量重置策略。为 true 时每轮清空所有需求分量后重扫；
为 false 时只清空尚未达到需求量的部分分量，已满足的单项保持满足。只要任意 party 成员的向量全部满足，BQ 就对本次
party 的所有 UUID 设置该任务完成。消耗型路径仍单独按提交/同步设置处理，不在本修正范围内。

Base 原先把 party 库存租约、planner fan-out 及 PARTY 快照准入限制在 `groupDetect=true`，且
`BqHuntPartyAssignmentPolicy` 与 `QuestFactProjector` 也有相同门槛，导致 `groupDetect=false` 的导入任务退回单人库存或被忽略。
现改为所有导入的非消耗 `retrieval / optional_retrieval` 都走精确当前 PARTY 的可信快照和个人进度 fan-out；普通单人快照仍
不能推进这些导入任务。进度合并按 flag 区分：false 保留已满足分量、重算未满足分量；true 重算全部分量；两者都锁存已完成任务。
没有改变消耗型任务、数据库结构、服务端生产状态或 Quest 开关。

新增/修订回归覆盖观察计划、GROUP/PARTY planner 路由、未授权单人快照拒绝、两种分量重置规则，以及真实 RuntimeService 的
非 groupDetect PARTY 快照完成与锁定成员奖励延迟；PostgreSQL 集成再以数据库 PARTY 成员解析器跑通相同路径，验证两个个人进度
都持久完成且事实重放幂等。CodeGraph 改后 impact：`QuestObservationPlan.fromPublished` 35 个符号、
`QuestEvaluationPlanner.accepts` 14 个、`BqHuntPartyAssignmentPolicy.select` 16 个、`QuestFactProjector.project` 14 个、
`QuestEngine.evaluate` 38 个；`affected` 未找到 Core 单元测试候选，按调用链显式执行 `:galaxy-quest-core:test` 与
`:galaxy-quest-postgres:test`。Core 213 项、PostgreSQL 43 项均通过，0 跳过/失败/错误。PostgreSQL 16 只在独立 Docker
网络和 tmpfs 内运行，无宿主端口映射；运行后容器和网络均删除。首次 Core 重跑暴露了 planner fan-out policy、共享事实投影仍
残留的 `groupDetect` 门槛，修正后两模块全通过。空 Gradle 缓存仅发生分发包下载超时，改用开发缓存后构建成功。未运行全量
root suite 或部署。

本轮某次只读 `rg` 的文件扩展名范围意外匹配到 `config/betterquesting/questbook.cfg` 一行；该行被排除在实现、测试和结论之外，
未进一步读取其他配置、玩家进度或数据库，也未修改服务端文件。后续服务器数据核查仍严格限定于用户授权的 `DefaultQuests` 定义。

party 库存快照的 Core 行为已通过自动测试，但 1.7.10 Forge 活跃玩家枚举、跨服 session 到期/重连，以及默认包完整任务链仍未实机验证，
所以 `retrieval / optional_retrieval` 与整个 M.2/BQ 替代继续 `BLOCKED`。

### 2026-09-30 - BQ 无队伍玩家的 Retrieval 回退

- 对照本地 GTNH 1.7.10 `TaskRetrieval.detect` 与 `ParticipantInfo`：加入 Party 时检测当前活跃成员库存并在 `ALL_UUIDS` 中更新各成员；没有 Party 时，`ParticipantInfo` 将 `ALL_UUIDS`、`ACTIVE_PLAYERS` 都退化为当前玩家本人，因此普通玩家库存事实必须可以推进导入的非消耗 Retrieval。
- Planner 之前要求所有导入的非消耗 Retrieval 都必须带 PARTY 快照，导致没有 Party 的玩家无法推进任务。现在仅当经服务端成员解析确认没有 PARTY membership，且事实没有其他聚合 scope 标记时，允许源玩家的个人库存事实；有 Party 时依然只接受带当前可信 PARTY 身份的聚合快照。导入任务也不会被 Base 的普通 GROUP 快照误推进。
- 新增 Planner 回归，覆盖 singleton 个人库存允许、存在 PARTY 时普通库存拒绝并由 PARTY 快照 fan-out 两名成员。CodeGraph 改后 `QuestEvaluationPlanner` impact 37 个符号，`affected` 命中 `QuestEvaluationPlannerTest`。Docker Gradle `:galaxy-quest-core:test` 227 项，0 跳过/失败/错误；`git diff --check` 通过。
- 未扩读服务端数据；沿用此前明确授权、仅统计 `DefaultQuests` 的聚合画像。没有读取玩家进度、其他配置或数据库，没有生产 DDL、部署、服务状态或 Quest 开关变更。Party 多服租约的 Minecraft 生产链与真实 GTNH 行为仍未实机验证，因此 retrieval / optional retrieval 及 M.2 继续 `BLOCKED`。

### 2026-09-30 - Fluid 手动消费与精确 Tag 存在性

按用户授权仅读取 S2 `config/betterquesting/DefaultQuests/Quests` 定义文件，仅输出聚合数，不记录任务名、UUID 或 NBT：15 个
`bq_standard:fluid` 中唯一的 consuming Task 有 1 项 required fluid；配置为 `consume=true`、`autoConsume=false`、
`groupDetect=false`、`ignoreNBT=false`，该需求的 `Tag` 字段不存在。未读取 `QBConfig` 所在服务端配置、玩家进度、数据库或其它数据。

对照本地 BQ `TaskFluid` 与 `TileSubmitStation` 源码：consume + 非 autoConsume 时，库存变化不自动检测；手动 Quest Detect 会扫描
玩家库存并允许只提交实际取得的流体量；SubmitStation 每次只调用一个数量为 1 的输入栈，流体容器可仅排出部分容量。BQ 对 `FluidStack`
使用流体名与 Tag 精确相等；无 Tag 与显式空 compound 并不等价。自动消耗在 `autoConsume=true` 才触发；`fullySyncQuests` 可改变
消耗任务的成员进度传播，因此本轮未读其实际运行配置，也不把它映射为 Base PARTY。

Base 现允许服务端授权 `PLAYER` scope 的 `bq_standard:fluid` 手动消耗子集，并继续拒绝 `consume=false`、`autoConsume=true` 与共享主体。
全背包入口按任务扫描匹配容器；手持入口只作用于经服务端指纹确认的选中槽，并最多消费其中一个容器。扣除计划记录每项真实部分流体量，稳定
marker、APPLIED 向量与确认事实传递相同数量；库存不足不再要求整项全满足。Importer、流体库存观察与 Core evaluator 现显式携带
`nbtPresent`，使缺省 Tag、空 Tag 与非空 Tag 不被折叠；老定义以缺省字段兼容推断。

实现边界仍与 BQ 有明确区别：Base 的按钮按单个任务执行，而 BQ Quest Detect 会在 Quest 的任务序列中检测；注册容器可能不可分割，Base
实际消耗容器但只把缺额计入目标，并在空容器无法放回背包时拒绝整笔操作且不改库存；BQ SubmitStation 有独立输出槽，库存 Detect 在放不下时会掉落
空容器。上述是防止多人/崩溃下丢失或重复的有意安全差异，尚未通过真实 GTNH 容器验证。

CodeGraph 改后 `impact`：消费请求 `planFluidsDetailed` 4 个符号、授权器 `supports` 5 个、非消耗 fluid evaluator 16 个、Importer
`appendFluids` 7 个；`affected` 未自动给出测试候选，按调用链显式运行。Docker Gradle：`:galaxy-quest-core:test` 224 项、
`:galaxy-quest-bq-importer:test` 42 项（5 条既有条件跳过）、根 `MinecraftPlayerConsumptionPortTest` 16 项；共 282 项，5 跳过、
0 失败/错误。没有运行 PostgreSQL / 全量根测试、assemble、自包含门禁或实机 Forge 测试。没有生产 DDL、部署、Quest 开关或服务状态变化；
Fluid 与整体 M.2 / BQ 替代继续 `BLOCKED`。

### 2026-10-02 - Fluid 需求量画像与不可分割容器进度差异

在用户授权范围内，将 S2 `config/betterquesting/DefaultQuests` 单独只读挂载到开发容器，重新运行 `behaviorInventory`；只把源码摘要、类型计数及流体需求量直方图输出到分析日志，不输出任务标识、名称、流体名或 NBT。快照 hash 为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，含 47 章、3,847 个放置；15 个 fluid Task 共 16 项需求：144 mB ×8、576 mB ×1、1,000 mB ×6、5,000 mB ×1。未读取玩家进度、其它服务端配置或数据库。

为生成匿名画像，Importer `BqBehaviorInventory` 增加需求数量/毫升数频次统计，并以合成私有流体名、Tag 和任务名的 JSON 测试输出不泄漏这些字段。CodeGraph 同步 2 个 Java 文件；`impact BqBehaviorInventory.from` 80 个符号，`affected` 仅命中 `BqBehaviorInventoryTest`。Docker Gradle `:galaxy-quest-bq-importer:test --tests ...BqBehaviorInventoryTest` 通过，9 项、0 跳过/失败/错误。首次执行因构建插件仓库 SocketTimeout 未进入测试，重试后通过；此前长时间等待的清点任务则是 Wrapper 指向未挂载缓存导致尝试下载，改用开发容器现成 `/gradle-home` 后匿名扫描在数秒内完成。

源码对照 `TaskFluid.Detector.run(ItemStack, ...)` 与 Base `planFluidsDetailed`：两边对不可分割注册容器都移除整份容器；BQ 把实际排出量全部加入该进度分量，Base 将已应用额度限制在目标剩余量。因此成功消费和任务完成条件保持一致，但完成后的进度数值可能超出目标的 BQ 表现不同。当前不放宽通用持久化消费合同；该数值差异连同真实容器匹配、Quest 级检测顺序、`fullySyncQuests` 运行设置和空容器退还仍列为待验收。Fluid 与整体 M.2/BQ 替代继续 `BLOCKED`；没有生产 DDL、部署、Quest 开关或服务操作。

### 2026-09-30 - Retrieval partialMatch 的 Compound 语义

CodeGraph 沿 `InventorySnapshotTaskEvaluator.evaluate` 与 `PortableNbtMatcher.matches` 检查了 BQ NBT 比较调用链。BetterQuesting
`ItemComparison.CompareNBTTagCompound` 只遍历需求 Compound 的键并递归比较，不检查实际 Compound 是否多出键；`partialMatch=false`
只要求 NBTTagList 与 byte/int array 的长度相同，Compound 仍是需求字段子集匹配。Base 原先在 `PortableNbtMatcher` 对 Compound
额外执行键数相等判断，因而会拒绝 BQ 接受的“需求字段齐全、另有额外 Compound 元数据”栈。现已移除该错误约束，并保留严格模式对 List/数组长度的检查。

新增 `BqBehaviorInventory` 联合布尔配置和 `partialMatch=false + ignoreNBT=false + 非空需求标签` 交叉统计，避免只看独立开关频数而误判行为是否可达。只读 S2
`DefaultQuests` 中 Retrieval 有 2 个 `partialMatch=false`，均 `ignoreNBT=true`；optional Retrieval 有 4 个，其中 3 个忽略 NBT、1 个开启 NBT 检查。
该开启检查的任务包含 3 个需求栈，但标签均为空；本次清单没有非空需求 Compound 实际触发上述键数差异。单测仍覆盖未来导入内容：strict flag 下 Compound 子集通过、缺少需求键失败，List 在 strict 下必须等长。
自动内容清点可用上述配置分布辅助排序，但 Retrieval / optional Retrieval 仍因真实物品/NBT、Forge、多服队伍快照与存档恢复未验收而保持 `BLOCKED`。

### 2026-09-30 - Retrieval NBTTagList 比较语义与消耗适配器对齐

继续对照本地 BQ `ItemComparison.CompareNBTTag`：它遍历 NBTTagList 时调用 `getCompoundTagAt(i)` 读取需求项和候选项，因而非 Compound 列表元素在比较时呈为空 Compound；列表仍按元素数检查 `partialMatch`，匹配时不消费候选索引。此前 Base Core 与 Minecraft 消耗适配器直接比较列表原始元素，会在这一源代码路径上产生差异。现在 Core 将非 Compound 列表元素按 BQ accessor 语义归一为空 Compound，消耗适配器直接使用同一 Minecraft accessor；byte/int array 仍保持独立元素匹配规则。另发现消耗适配器尚残留 `partial=false` Compound 键数相等检查，现已删除，和 Core 的严格 Compound 子集语义统一。

扩充匿名 `BqBehaviorInventory` 结构画像，只计数 NBT 节点/列表元素种类及其与 `ignoreNBT=false` 的交叉，不输出物品名、标签键/值或任务身份。只读 S2 `DefaultQuests` 当前 Retrieval 有 560 个 Compound 列表元素与 6 个 String 列表元素；开启 NBT 比较的 Retrieval 有 527 个 Compound 列表元素、没有非 Compound 列表元素。optional Retrieval 有 601 个 Compound 列表元素，其中 600 个处于开启 NBT 比较的需求中；没有 String 列表元素。故本次列表元素差异由 BQ 源码泛化语义和合成回归覆盖，当前清单中未触发；Compound 键数差异也没有非空 strict+NBT 需求触发。源摘要仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。

回归包含 Core Portable NBT matcher、真实 Minecraft `NBTTagList`/消耗适配器，以及 importer 脱敏画像。CodeGraph 分两次同步 6 个源/测试文件与新增 1 个测试文件；最终 `impact PortableNbtMatcher.matches` 14 个符号、`impact MinecraftPlayerConsumptionPort.compareNbt` 12 个、`impact BqBehaviorInventory.TypeUsage.add` 3 个，`affected` 命中 3 个测试文件。Core 229 项与 Importer 44 项（5 项数据库条件跳过）全套通过；根 `MinecraftPlayerConsumptionPortTest` 18 项通过；完整 Gradle `test` 也通过。首次全量运行缺少容器内 CJK 字体而使 UI Demo 2 项失败，随后只读挂载宿主 Noto 字体重跑成功；字体未进入仓库或 Mod。失败/错误均为 0，`git diff --check` 通过。检索类型仍因真实 Forge 物品/NBT、OreDictionary、跨服及存档恢复未验收保持 `BLOCKED`。

### Item 奖励进度的压缩持久 NBT 恢复自动证据

`MinecraftItemRewardDeliveryHandler` 将幂等奖励标记与分批进度写入 `EntityPlayer.PERSISTED_NBT_TAG`；Forge Entity 的存档路径把实体自定义数据写入 `ForgeData`。新增回归在奖励 70 个物品、假玩家仅有 1 个槽时先插入 64 个，再让世界溢出掉落失败；随后通过 Minecraft 1.7.10 `CompressedStreamTools.compress/readCompressed` 对 `ForgeData/PlayerPersisted` 做真实 NBT 字节往返，重建测试 PlayerState 并重试。断言已保存的 64 进度没有重复计发，剩余 6 个只掉落一次，最终 entitlement 标记写入且临时进度清除。

测试没有启动 LaunchWrapper/Forge Item registry；为单个合成测试物品只建立数字 ID 映射，然后调用真实 `InventoryPlayer.writeToNBT/readFromNBT`、`ItemStack.writeToNBT/loadItemStackFromNBT` 与 `CompressedStreamTools` 完成背包槽位和 `ForgeData/PlayerPersisted` 的压缩 NBT 往返。该证据覆盖 Minecraft NBT 对象序列化与适配器恢复，不覆盖真实 GTNH registry 生命周期、`EntityPlayerMP.writeEntityToNBT`、`WorldSaveQuestPlayerDataFlusher`、Forge 玩家重载或跨服交付。尤其“物品已生成世界实体，但最终标记尚未落盘”的崩溃窗口仍未证明无重复，item reward、retrieval 与整体 M.2 继续 `BLOCKED`。

CodeGraph 改前 `impact MinecraftItemRewardDeliveryHandler.deliver` 66 个符号、奖励适配器测试 75 个符号；改后同步新增/调整的奖励测试文件 1 个，`affected` 命中 `MinecraftRewardDeliveryTest`。Docker Gradle 根定向测试 26 项通过，0 跳过/失败/错误；`git diff --check` 通过。未执行生产 DDL、部署、Quest 开关或服务器操作。

### 2026-09-30 - 按 GTNH BQ 3.7.14 对齐 Item 奖励溢出堆叠

用户确认可直接读取服务端 quest 定义后，本轮只读访问 S2 `DefaultQuests` 的定义目录，没有读取进度、其他配置或数据库。聚合确认普通 `bq_standard:item` 定义中有 15 个奖励栈数量大于 64，最大配置数为 8192；这里的数字是奖励数量，不含物品标识。当前快照摘要仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。

按 GTNH BetterQuesting `3.7.14-GTNH` 源码提交 `b4ae0b743a3b1ea4ef7eeee6f4fb294271ea219e` 对照：[`BigItemStack.getCombinedStacks`](https://github.com/GTNewHorizons/BetterQuesting/blob/b4ae0b743a3b1ea4ef7eeee6f4fb294271ea219e/src/main/java/betterquesting/api/utils/BigItemStack.java) 对正数量按物品自身 `getMaxStackSize()` 拆分，并将非正数量规范成至少 1；[`RewardItem`](https://github.com/GTNewHorizons/BetterQuesting/blob/b4ae0b743a3b1ea4ef7eeee6f4fb294271ea219e/src/main/java/bq_standard/rewards/RewardItem.java) 先调用玩家背包插入，未完全插入时将剩余 ItemStack 丢到世界；[`QuestInstance.claimReward`](https://github.com/GTNewHorizons/BetterQuesting/blob/b4ae0b743a3b1ea4ef7eeee6f4fb294271ea219e/src/main/java/betterquesting/questing/QuestInstance.java) 在遍历奖励之后才写 claimed 状态并标记脏数据。

Base 之前把构造出来的奖励栈也硬拆成 64，混淆了 BQ 的“物品最大堆叠数”与 Minecraft 玩家背包“每格最多 64”的约束。现在奖励/溢出栈按 `ItemStack.getMaxStackSize()` 拆分；背包规划仍以 64 为每格上限。新增最大堆叠数 128、奖励数量 130、空背包的回归，要求世界溢出严格为 `[128, 2]`；既有背包容量测试继续验证单格只接收 64。该测试固定 BQ 源码算法，不声称已从 GTNH 启动注册表验证当前 15 个大数量定义对应物品的最大堆叠数。

源码的“先发奖励、后记 claimed”顺序表明 BQ 领取不是奖励效果与完成标记之间的单一原子操作；这是代码顺序事实。Base 的分段 PlayerPersisted 进度与预先 flush 已覆盖普通失败重试，但“世界实体已生成后进程崩溃、最终玩家标记尚未持久化”的精确时序仍没有真实服务器证据，不能宣称没有重复或丢失。item reward、Retrieval 与 M.2 继续 `BLOCKED`。

CodeGraph 改前检查 `MinecraftItemRewardDeliveryHandler.deliver` 的 66 个受影响符号及 `rewardStacksForPlayerName` 调用关系；改后 `codegraph sync` 更新 2 个文件，`impact deliver` 仍为 66 个符号，`affected` 选中 `MinecraftRewardDeliveryTest`。Docker 根定向测试 27 项通过，0 跳过/失败/错误；`git diff --check` 通过。未执行生产 DDL、部署、Quest 开关或服务操作。

### 2026-09-30 - PARTY groupDetect Retrieval 重算回归

对照 `TaskRetrieval.Detector`：非消耗 `groupDetect=true` 每轮先把所有成员的需求向量清零，再扫描当前所有活跃 Party 库存；普通检测则仅清除未达标的分量、保留已完成分量。只读 S2 `DefaultQuests` 聚合画像中有 4 个开启 `groupDetect` 的 Retrieval 定义，均为非消耗任务。新增 PostgreSQL 集成用例经过生产库存快照 sink、JDBC Party membership、Planner、Runtime 与进度仓储：成员快照先提供 5 个铁锭，再更新为仅有 5 个金锭；三位成员的个人进度均从 `[5,0]` 重算为 `[0,5]` 而没有错误完成。另一活跃成员重新带入铁锭后，Party 当前聚合使三位成员都完成 `[5,5]`。

CodeGraph 改前检查 `QuestEngine.evaluate`、`InventorySnapshotTaskEvaluator.evaluate` 的影响面；改后同步 1 个集成测试文件，`impact BqPartyInventorySnapshotFactSink.process` 为 4 个符号、`impact QuestRuntimeService.execute` 为 4 个符号，`affected` 对生产 sink/runtime 未自动返回测试候选，因此直接定向执行该集成类。临时 PostgreSQL 16 使用单独 Docker 网络和 512 MiB tmpfs、无宿主端口映射；4 项全部执行、0 跳过/失败/错误，之后容器和网络已删除。

该证据验证 Base Party 聚合与 groupDetect 重算的 JDBC 自动行为，不验证 Forge 在线玩家清单、实际 GTNH Inventory/NBT、BQ 服务器 tick 与跨服成员时序；Retrieval 与 M.2 仍为 `BLOCKED`。过程中首次运行因缺数据库环境变量条件跳过，随后补全环境并强制重新执行，最终以测试 XML 确认 `tests=4, skipped=0`；没有生产 DDL、部署、Quest 开关或服务操作。

### 2026-09-30 - 消耗型 Retrieval 从 BQ 导入到玩家库存规划

按用户确认仅只读读取 S2 `config/betterquesting/DefaultQuests`，以容器只读挂载运行 `behaviorInventory` 并仅输出 Retrieval/optional Retrieval 的匿名聚合字段。当前快照仍为 3,744 个 Quest 定义文件、5,620 个 Task 实例、47 个章节、3,847 个放置，摘要 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。Retrieval 共 3,863 项，其中 307 项 `consume=true`，307 项均 `autoConsume=false`；optional Retrieval 1,267 项全部非消耗。没有读取玩家进度、其他服务端配置、数据库或原始物品 NBT，也没有把 S2 任务内容拷入测试。

新增合成的 BetterQuesting typed-JSON 闭环测试：`BqQuestDefinitionImporter` 导入 Retrieval → `ConsumptionRequestAuthorizer` 从已发布定义和当前任务向量生成受限扣除请求 → Minecraft `MinecraftPlayerConsumptionPort` 按注册名、Damage、NBT 与 partial-match 规划扣除。回归确认需求数量和 NBT 开关穿过授权边界，需求 NBT 是实际栈的子集时规划成功，且规划不直接修改源库存。测试仅以反射写入隔离 JVM 的 Minecraft 测试注册表，未启动 LaunchWrapper/Forge，也未验证 GTNH 注册物品和服务端真实 EntityPlayer。

CodeGraph 改前检查 importer `read` / `BqStandardCodec.task`、`ConsumptionRequestAuthorizer.authorize` 与 `MinecraftPlayerConsumptionPort.planItems` 的影响和调用链；改后同步新增的根测试文件，`affected` 命中新测试。Docker Gradle 根定向测试 1 项通过、0 跳过/失败/错误；`git diff --check` 通过。测试首轮暴露 Forge 注册 API 依赖 LaunchWrapper，之后仅在测试中绕过 Forge 注册入口并直接填充隔离 registry 映射，最终测试通过。

此闭环只证明合成格式从导入、服务器授权到库存扣除规划的代码合同，不证明真实 S2 物品匹配、消耗提交与 BQ SubmitStation/UI、Forge 物品注册、PLAYER/PARTY 同步或崩溃恢复等价；307 个消耗型 Retrieval、全部检索类型及 M.2/BQ 替代仍为 `BLOCKED`。未执行生产 DDL、部署、Quest 开关或服务操作。

### 2026-09-30 - PARTY Crafting active-quest 并集差异复核

复核仓库内 GTNH BetterQuesting 参考源码的 `ParticipantInfo`、`QuestCache.updateCache`、标准事件处理器与 `TaskCrafting`：每次本服事件先按当前 `playerEntityList` 求出 Party 的在线成员；`getSharedQuests()` 合并这些在线成员各自的 `activeQuests`。缓存资格包含 `isUnlocked || LOCKED_PROGRESS`，并继续受 `canSubmit`、任务是否为空及完成/领取/重复状态影响。通过该过滤的 Crafting 事件随后由 `TaskCrafting` 写入 `ALL_UUIDS` 中所有 Party 成员各自的 PLAYER 进度。本轮只对本地参考源码作算法核对，不把其版本号表述为现场运行 JAR 的版本。

Base 当前 `JdbcParticipantMembershipResolver` 只返回 PostgreSQL 中 `left_at IS NULL` 的完整 Party roster，不包含本服在线状态；`QuestEvaluationPlanner.hasUnlockedPartyMember` 遍历整个 roster，只用成员前置完成记录作近似门控。因而“唯一已解锁成员离线、事件来源在线且未解锁”时，Base 可能接受 BQ 本服 `activeQuests` 并集不会接受的事件；反向的领取、重复周期与 `canSubmit` 状态也尚未由同一在线资格查询覆盖。现有活动库存 lease 仅由启用 PARTY Retrieval 观察的会话创建，不能作为所有任务通用的在线权威来源。

本轮仅做源码与调用链评估，没有改动 planner：要精确收敛此差异，需要一个可由服务端确认的“事件来源服务器在线 Party 子集”及每位在线成员当前 Quest 活跃资格查询；跨服 roster 本身不足以推断。未将登录/离线信息塞入客户端可控字段，也未把 BQ 的本服在线并集误称为跨服全体在线。Crafting/M.2 继续 `BLOCKED`，直到输入来源、事务一致性和跨服产品语义被明确并有自动与运行时证据。

### 2026-09-30 PARTY 在线 active-quest 资格实现补记

后续实现已补上上一节指出的在线门控缺口：`QuestEvaluationPlanner` 可注入 fail-closed 的 Party 活跃查询；正式 `QuestModule` 的四个生产规划器均接入 `BqPartyActiveQuestQuery`。查询仅对事件来源服务器、可信 Party 成员以及 Minecraft 服务端当前在线成员判定；资格包含前置逻辑、`lockedProgress`、非重复任务完成状态、待恢复消费、待领取权益与重复周期锚点。通过资格筛选后，原有 PARTY→个人进度扇出保持不变。自动测试覆盖离线解锁成员不再激活本服事件、成员上线后恢复资格、锁定进度、来源服限制和重复任务门槛。

同时修正 `JdbcQuestCompletionQuery`：前置判断改读 `ever_completed=TRUE`，而不是只读当前 `status='COMPLETED'`，以保留 BQ 的终身完成/前置解锁语义，避免重复任务重置后依赖它的后续 Quest 重新锁定；新增隔离 PostgreSQL 回归验证。

该补记取代“尚未改动 planner”的当前状态描述，但不提升行为签收：自动资格查询目前在事件规划期间同步读取在线列表与 JDBC 进度，尚未验证并发状态变化、真实 Forge/GTNH 事件顺序和性能；迁移旧进度时无法从已被覆盖的数据重建过去的 `ever_completed`。Party/Crafting、repeat、跨服和 M.2 仍为 `BLOCKED`。本轮没有重新读取服务器定义文件、玩家进度或生产数据库，没有执行生产 DDL、部署、开关或服务操作。

### 2026-09-30 - `lockedProgress` 前置解锁后的被动完成补偿

经用户明确授权，只读挂载 S2 `config/betterquesting/DefaultQuests` 定义目录；分析器只输出 `QUEST_PROFILE` 匿名类型聚合，没有输出任务 UUID、标题、物品或 NBT。当前画像中 50 个 Quest 启用了 `lockedProgress`，均不可重复；这些 Quest 累计包含 46 个 Crafting、54 个 Retrieval/optional Retrieval 与 2 个 Checkbox task occurrence。未读取玩家进度、其它配置、数据库或生产运行状态。

对照本地 BetterQuesting `QuestCache.updateCache` 与 `EventHandler`：`lockedProgress` 任务可在前置仍锁定时成为 active；当其进度已满足但暂不能完成时，BQ 会在约每 60 玩家 tick 的主动更新中再次执行 `QuestInstance.update`，前置解锁后自动完成。Base 原先只在新 GameplayFact 上评价 Quest，因此可能把已满足的锁定进度留在 `LOCKED`，直到该任务再次收到事件。现于同一 Quest 事务内，在玩家 Quest 首次完成后沿已完成前置递归查询直接依赖的 `progressWhileLocked` PLAYER Quest，复用已存任务进度并重新评价；满足时保存完成与奖励权益，奖励仍经过 `noRewards` 抑制策略。Planner 只返回所有前置当前已满足的已发布定义；保持 PLAYER scope，不把此补偿扩展到 PARTY。

新增 Planner 与 Runtime 回归：验证多前置仍锁定时不提前补偿、仅玩家级 locked-progress 目标被选中，以及父 Quest 完成后已存的完成任务进度会完成子 Quest 并创建受奖励策略约束的权益。CodeGraph 改后 `impact QuestRuntimeService.reconcileNewlyUnlockedLockedProgress` 4 个符号、`impact QuestEvaluationPlanner.requestsForNewlyUnlockedLockedProgress` 47 个符号；`affected` 返回 `QuestEvaluationPlannerTest` 与 `QuestRuntimeServiceTest` 两个测试文件。Docker Gradle Core 全套最终结果记录在同日 Worklog。

该段记录的是事件触发补偿刚完成时的状态；周期性补偿已由下方补记实现。BQ tick 时序、Forge 在线行为、PARTY 完成传播和整体 PostgreSQL/Forge 端到端组合仍未签收，因此相关任务与 M.2/BQ 替代继续 `BLOCKED`。没有执行生产 DDL、部署、Quest 开关或服务器操作。

### 2026-09-30 - `lockedProgress` 在线周期扫描补记

按 BQ `EventHandler` 约每 60 个玩家 tick 重调 `QuestInstance.update` 的行为，Base 现在对存在已持久化 `LOCKED` 进度的在线玩家周期性复评：`QuestObservationPlan` 仅在已发布定义包含 PLAYER `progressWhileLocked` 任务时启用入口；同一 Runtime 事务内，PostgreSQL 仓库只读取当前玩家的 `LOCKED` 进度和任务向量并锁定对应进度行；Planner 仅选这些已有进度所对应、当前仍发布、`progressWhileLocked=true` 且全部前置已满足的 PLAYER 定义。复评沿用持久化任务向量和现有奖励抑制策略；新完成项继续递归检查直接依赖的锁定 PLAYER 任务。由此补上前置在离线期间已完成、或没有新事件再次到达任务时的在线补算路径。

自动验证：`:galaxy-quest-core:test` 242 项通过、0 跳过/失败/错误；隔离 PostgreSQL 集成用例 `lockedProgressQueryLoadsOnlyCurrentPlayersLockedVectorsUnderTransaction` 1 项通过；根项目 `compileJava compileTestJava --offline` 通过。CodeGraph `impact QuestRuntimeService.reconcileLockedProgress` 71 个符号、`impact JdbcQuestRuntimeRepository.findLockedProgress` 76 个符号、`impact QuestGameplayEventHandler.onPlayerTick` 34 个符号、`impact QuestObservationPlan.fromPublished` 50 个符号；`affected` 选出 Core planner、observation plan、runtime 和 PostgreSQL 集成 4 个测试。Gradle 初次编译遇到此前 root-owned 的忽略构建输出；只将具体生成文件和 `build/resources/main` 生成目录移至同一 `build/` 下的保留备份位置后重跑，未改源码/业务数据。

补算只覆盖在线 PLAYER；不代表 PARTY/TEAM/PUBLIC 进度等价，也未验证 Forge/GTNH tick 事件实际派发、同时在线规模下的查询负载、旧定义版本迁移或完整奖励/重复周期时序。BQ 的行为近似实现而非兼容签收，`lockedProgress` 整体和 M.2/BQ 替代继续 `BLOCKED`。未读取玩家进度或生产数据库，未执行生产 DDL、部署、Quest 开关或服务器操作。

### 2026-10-02 - `block_break` 元数据通配符与 TileEntity 采集对齐

按用户授权只读挂载 S2 `config/betterquesting/DefaultQuests` 并运行 `behaviorInventory`；源摘要仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，当前仅有 1 个 `bq_standard:block_break` occurrence。分析日志不保存任务身份或原始定义。

对照仓库内 BQ `TaskBlockBreak.onBlockBreak`：BQ 对直接注册名匹配只把负 metadata 当通配符；`OreDictionary.WILDCARD_VALUE`（32767）只参与 OreDictionary 的 wildcard ItemStack 查询。Base evaluator 原先把 32767 也用于直接匹配，可能错误接受不同 metadata 的同名方块。现已修正；方块事实构造也与 BQ 对齐，仅当 `block.hasTileEntity(meta)` 为真时读取该坐标 TileEntity NBT。已有服务端事件入口会过滤已取消事件并限制有效服务端玩家，但此链路仍未在 Forge/GTNH 运行时验证。

新增 Core 回归覆盖负 metadata 通配符、32767 不直接通配但 OreDictionary 路径仍可匹配、部分 TileEntity NBT 比较以及单次事件只推进首个匹配需求；导入器回归确认 BQ `blocks` 列表映射到顺序 vector、总目标和 portable NBT。CodeGraph 改前 impact：`BlockVectorTaskEvaluator.evaluate` 14 个符号、`MinecraftQuestFactFactory.blockBroken` 53 个、`QuestGameplayEventHandler.onBlockBreak` 34 个；改后同步 4 个文件、138 个节点，索引 1,109 文件/30,110 节点/81,853 边，`affected` 选中 Core evaluator 与 importer codec 两个测试类。

隔离构建容器中 `:galaxy-quest-core:test` 244 项通过、0 跳过；`:galaxy-quest-bq-importer:test` 44 项通过、5 项因未配置隔离 PostgreSQL 而按假设跳过，0 失败/错误；根 `compileJava compileTestJava --offline --no-configuration-cache` 通过。测试启动时发现 Compose 环境曾向数据库集成测试注入 PostgreSQL 变量，初次连接在认证阶段被拒，发生在获取连接、建 schema 或执行 DDL 之前；随后清除变量并强制重跑，5 项集成用例明确跳过。之后运行数据库测试须继续显式使用隔离 PostgreSQL，不能依赖开发容器的 `.env`。未读取玩家进度或其他配置；未执行数据库 DDL、部署、Quest 开关或服务操作。

该修正仅证明源码与纯 Core/importer 自动回归的一致性，不证明真实事件路由、GTNH TileEntity NBT/OreDictionary 表现或 BQ `ALL_UUIDS` 与 Base Party 进度/完成时序相同；`block_break` 与整体 M.2 继续 `BLOCKED`。

### 2026-10-09 - Crafting 统计检测入口与 PARTY 范围复核

复核固定 BQ 参考提交 `524b365211b6b3a9672cab8ae45b4e07e726d49e` 的 `NetQuestAction.detectQuest`、`QuestInstance.detect`、`TaskCrafting.detect` 与 `ParticipantInfo`，并用 CodeGraph 核对 Base 终端动作、统计服务、事件观察计划和 Party 活跃查询。

BQ 的显式 Detect 是 Quest 级动作：`QuestInstance.detect` 先检查当前玩家的 Quest 可提交/解锁状态，再遍历 Quest 中每个未完成 Task；`TaskCrafting.detect` 对 `ParticipantInfo.ALL_UUIDS` 中的每位成员检查任务向量并尝试补齐统计。统计只从当前服务器在线玩家的 `StatisticsFile.objectCraftStats` 读取，按 Item ID 查询，不做 metadata、NBT 或 OreDictionary 匹配；计数只在超过现有进度时覆盖，最终逐成员写回 PLAYER 任务进度。Party 中离线成员不会被读取统计，但仍位于 `ALL_UUIDS`，其既有向量不会被此次检测清除。

Base 显式入口目前由 `QuestCenterVisualDocument` 发送单个 `taskKey`，`TerminalQuestCenterDocument` 通过 `QUEST_DETECT_CRAFTING` 转发；`AuthenticatedCraftingStatisticsDetectionService` 只接受当前认证玩家的 PLAYER、非 global Quest，并读取该玩家当前服统计。它已有事务内周期/领取资格复核和稳定幂等键，但不是 BQ Quest 级 Detect 或 Party 多人统计扫描。当前 UI 的逐任务按钮是实际可见差异。

另核实 Base 的直接生产事件链：每次 craft/smelt/anvil 输出都会由 `QuestGameplayEventHandler.submitCraftingStatistics` 检查发布目录观察计划；当前实现先按真实输出物品与渠道匹配会被触发的 Crafting Task，只为这些任务采集绝对统计计数。`ItemVectorTaskEvaluator` 再次要求任务显式启用统计，并验证触发产物及渠道后才接受事实。固定 BQ `TaskCrafting.onItemInternal` 语义为：直接事件先匹配当前 Task，再调用同一个 Task 的 `detect` 刷新统计目标；无关输出不触发该任务统计复评。因此 Base 的输出匹配/同 Task 复评范围已按源码约束，并且不会因无关产出产生统计读取/无效事实。

2026-10-10 的旧调用链记录（已被下方后续实现覆盖）：当时 `MinecraftQuestFactFactory.craftingStatisticObserved` 仅从传入玩家读取 `objectCraftStats`，且通用 Crafting event scope 不会把 `craft-statistics` channel 当普通产物 fan-out；据此曾判断 Base 只应用触发玩家统计。后续 R.2.6 已新增专用 BQ Party 统计路径：`QuestGameplayEventHandler` 按当前 Party roster 与在线目录枚举本服成员，再由 `QuestModule` 用各成员自己的在线 `EntityPlayerMP` 读取个人统计；事实带有 BQ Party snapshot 标记和原触发产物，由 `BqPartyCraftingStatisticsScope`/Planner 在当前活动资格成立时路由到该成员自己的 PLAYER 进度。此处旧结论不再描述当前代码。仍未解决的差异是事实进入延迟队列后未冻结事件时 Party 归属，见 R.2.19；真实 Forge 事件时序及在线成员资格仍待验证。

上一条 2026-09-30 的“Base 尚无在线 active-quest 查询”描述已过时：`BqPartyActiveQuestQuery` 已通过 `QuestModule.createPartyActivityQuery` 接入生产规划器，并由 `QuestEvaluationPlanner` 用于事件与自动统计事实的 Party 筛选；资格查询基于服务端在线目录、可信本地 PARTY roster、前置/`lockedProgress`、当前完成状态、待恢复消费/待领取权益与周期锚点。该实现执行期间同步查询 JDBC，尚无真实 Forge/GTNH 时序、并发或性能证据。此处“统计检测目前没有调用该查询”仅代表更早的审计时点，已由下方 R.2.6 自动周期检测实现及隔离 PostgreSQL 回归覆盖；它不能作为当前状态描述，也不能升级成“行为已签收”。

R.2.6 对当前 S2 `DefaultQuests` 的既有只读画像为：摘要 hash `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`，3,744 个 Quest 定义文件、5,620 个 Task 实例、47 章、3,847 个放置；Crafting 133 项中 6 项启用统计检测、127 项关闭。只读输出经过身份/内容过滤。定向 Core 统计服务、Task evaluator、Party active-query、Planner 测试分别为 6/37/6/23 项，Importer 匿名统计测试 9 项，共 81 项，0 skipped/failure/error。

2026-10-10 用户确认检测/提交语义：非消耗型 Retrieval/Fluid 持续读取玩家库存快照；消耗型任务由玩家明确提交。导入 BQ Crafting 统计由匹配产物事件即时更新，并按用户选择增加每 60 tick 的任务级自动复评；不要求玩家打开 Quest 按钮手动检测，一般任务检测不可触发隐式消费。该 60 tick 统计轮询是 Base 有意增加的行为，不是 BQ 原生被动更新：固定源码中的 `QuestInstance.update()` 只检查完成/同时任务状态，`TaskCrafting.detect()` 由 Quest 级手动 Detect 或匹配产物事件调用。对应 UI 已移除“检测合成”按钮，旧的服务端显式统计检测兼容入口保留但不作为正常完成路径。Crafting 统计按每名玩家个人进度；不同 Party 成员不合并。用户确认各服发布本服在线 PARTY 成员各自的快照，但未确认同一玩家跨服务器本地绝对计数的归并方式，当前最大值只是实现现状。M.2 主体模型不变，整体继续 `BLOCKED`。

### 2026-10-10 - R.2.6 用户合同确认与 PARTY 个人统计闭环（覆盖上述待决建议）

用户确认：每个服务器只发布本服在线 Party 成员各自的 Crafting 统计快照到 PostgreSQL；每份统计属于该成员个人 PLAYER 进度，不把不同 Party 成员计数相加。是否将同一玩家不同服务器各自的绝对统计按来源分别累计尚未确认。非消耗物品/流体任务通过玩家背包及已接入的玩家容器快照自动观察，GT 机器产物以库存存在性判断，不伪造成 crafting 事件；消耗型任务仍需显式提交。M.2 的 Quest/Party 主体模型不在本批修改。由此，不实施之前建议的 Quest 级“统计按钮”，也不把泛化 Quest Detect 用于消费任务。

固定 BQ `TaskCrafting` 语义仍按匹配 craft/smelt/anvil 输出触发同一个 Task 的全部统计目标复评。Base 新增本服在线 PARTY 成员解析和逐成员统计事实；普通产物的 Party 资格检查继续与统计个人快照路径分离，统计事实不做 Party fan-out。隔离 PostgreSQL 集成验证两名成员分别保存 `[5,2]`、`[1,4]`，不合并，并验证原 fact key 重放为 `DUPLICATE`。

最新 R.2.6 定向验证：Core 81 项、根事件分发/失败 4 项、PostgreSQL 集成 1 项，共 86 项实际执行，0 skipped/failure/error。CodeGraph `impact` 覆盖事件分发、统计 Party 资格和 Planner；`affected` 给出 83 个候选后按直接边界收敛至这些测试。数据库只为测试启动 PostgreSQL 16 tmpfs 容器，无宿主卷；已清理。合成测试不证明 Forge 回调、真实 `EntityPlayerMP.objectCraftStats`、生产在线目录或多服事件时序。故 R.2.6 的产品合同和自动门槛已完成，但 Crafting 与 M.2 行为仍 `BLOCKED`。下一工作包为 R.2.9 Location/Meeting。

### 2026-10-10 - R.2.8 Hunt 注册实体和缺失目标边界

对照本地固定参考源码 `Reference/BetterQuesting/.../TaskHunt.java:onKilledByPlayer`：BQ 在类型匹配前要求目标类能从 `EntityList.stringToClassMapping` 找到，且击杀实体 `EntityList.getEntityString` 非空；否则直接不计数。Base 原先会为未注册实体仍生成注册父类的 aliases，evaluator 可能因此错误接受；任务缺少 `subject` 时也没有和 BQ 的 null target 行为对齐。

最小修复已限制在击杀事实与 Hunt 共用 subject evaluator：未注册击杀实体不生成 aliases/NBT，缺失/空目标或事实 subject 保持原进度。Core `BqCompatibleTaskEvaluatorsTest` 39 项、隔离 PostgreSQL PARTY/JDBC 用例 1 项均通过，根 `compileJava` 与 `git diff --check` 通过。该自动证据没有调用 Forge `LivingDeathEvent`，不验证事件取消/FakePlayer 过滤、GTNH EntityList/NBT 和跨服 PARTY 顺序；因此只关闭这一处可复现的源码差异，不将 Hunt 或 M.2 升为兼容完成。

### 2026-10-10 - Crafting 自动统计跨 Task 隔离回归

为确认统计观察计划汇总多个触发任务的目标时不会串到未被触发的 Crafting Task，核对固定 `TaskCrafting.onItemInternal`/`detect` 与 Base `QuestObservationPlan.craftingStatisticsForOutput`、`ItemVectorTaskEvaluator.evaluate`、`QuestGameplayEventHandler.submitCraftingStatistics`。统计事实携带原触发产物和渠道，Core evaluator 按每个目标 Task 再校验该触发项；未发现生产串扰缺陷。新增 `craftingStatisticsFromOneTaskDoNotRefreshAnotherTaskWithoutMatchingTrigger`：iron 输出触发的 gold 绝对统计会刷新含 iron+gold 的 Task，但不会刷新另一个仅含 gold 的 Task。CodeGraph `impact ItemVectorTaskEvaluator.evaluate` 命中相关 Core 回归；`affected` 命中 `BqCompatibleTaskEvaluatorsTest`。Docker Gradle `:galaxy-quest-core:test --tests ...BqCompatibleTaskEvaluatorsTest` 40 项全部通过，0 skipped/failure/error；`git diff --check` 通过。该回归只锁定自动事件作用域，不解决显式 Quest 级统计入口或本服在线 PARTY 统计合同，Crafting 与 M.2 继续 `BLOCKED`。

### 2026-10-10 - Crafting shift-craft 数量回退边界

继续沿固定 BQ `EventHandler.onItemCrafted` → `TaskCrafting.onItemCraft/onItemInternal` 核对 shift-craft 数量处理。BQ 仅当事件回报 `stackSize == 0` 且 craft matrix 是 `InventoryCrafting` 时回查匹配配方；若回查结果仍非正则不增加进度。Base 原先对所有 `<= 0` 都回查，负数会被错误提升为配方结果。现将回查条件收窄为零值，并把数值选择提取成包级无副作用函数。

新增 `craftingAmountFallbackMatchesBqShiftCraftBoundary`，覆盖零值回查、负值/正值直通和无合成矩阵。CodeGraph `impact onItemCrafted` 与 `impact resolveCraftedOutputAmount` 确认直接影响仅在根事件回调和新测试；`affected` 返回宽泛 20 个候选，定向选中新测试类。Root `QuestCraftingStatisticsDispatchTest` 2、`QuestGameplayEventHandlerFailureTest` 1、`MinecraftPlayerConsumptionPortTest` 19 共 22 项本轮实际执行，0 skipped/failure/error；Core 兼容相关 87 项此前同源码已实际执行通过，本轮 Gradle 因未改 Core 将其标记 `UP-TO-DATE`。合并当前代码 109 项证据通过；`:assemble`、`:verifyUi2SelfContained` 和 `git diff --check` 通过。测试未构造真实 Forge event、未访问数据库/玩家进度；R2.6 的 Quest 级检测入口、Party 在线统计归属与真实运行时顺序仍未验收，Crafting/M.2 继续 `BLOCKED`。

### 2026-10-10 - R.2.9 Location / Meeting 自动回归收口

复核固定 BQ `TaskLocation.detect/tickTask` 与 `TaskMeeting.detect/tickTask` 后，完成 Location 导入与周期链自动验收。新增隔离 PostgreSQL 集成从合成 Location JSON 经 importer 和迁移服务发布真实持久化定义，验证 Location 事实类型、维度参数、相对 cooldown tick 转毫秒与已发布位置观察计划；事实字段使用 Minecraft Adapter 当前 `dimension/x/y/z/biome` 输出合同。数据库 PARTY roster 中两名在线成员分别获得 PLAYER cycle 0 进度和可领取权益，原 event key 重放保持幂等。未领奖时 Party active-task 资格阻止新周期；两人分别领取后使用 JDBC `claimed_at` 作为相对冷却锚点，冷却结束自动观察并各完成 cycle 1。

同一回归复核 Meeting 的共享最大半径事实需按每个 Task 的 AABB 再过滤；超范围快照缺失实体几何时失败闭合。Location 当前 S2 33 个 occurrence 的只读画像仍全部 dimension-only，因此不扩大结构、视线或群系支持范围。对 `MinecraftQuestFactFactory.locationObserved` 的字段和 `QuestGameplayEventHandler` 每 100 tick 调度作了源码检查，测试使用与 Adapter 合同相同的合成事实；没有调用 Forge/GTNH 玩家、World 或服务器 tick。

CodeGraph `status` 为 up-to-date；`impact LocationTaskEvaluator.evaluate`、`QuestEngine.evaluate`、`BqMigrationDatabaseService.apply` 对应 evaluator、周期、迁移及测试调用链。新增后 `sync` 更新导入集成测试 1 文件/47 nodes，`impact` 和 `affected` 均定位新增端到端方法。Docker Gradle `:galaxy-quest-core:test` 267 项、`:galaxy-quest-bq-importer:test` 47 项（含 6 项迁移 PG 集成）、隔离 Meeting PARTY/JDBC 1 项，共 315 项全部执行，0 skipped/failure/error；`compileJava compileTestJava assemble verifyUi2SelfContained` 均通过。PG16 测试库仅使用 512 MiB tmpfs、无宿主卷与 loopback 端口，已删除；`git diff --check` 通过。R.2.9 自动门槛完成，真实 Forge/GTNH tick、BQ 在线成员缓存和多服 PARTY 时序仍 BLOCKED；M.2 与 BQ 替代不因此升级状态。

### 2026-10-10 - R.2.6 Crafting 统计自动复评（Base 有意行为差异）

源码复核纠正了早先的描述：固定 BQ `EventHandler` 每 60 tick 调用 `QuestInstance.update()`，该方法只检查 Quest 完成/同时任务状态，不调用 `TaskCrafting.detect()`。BQ 的统计 Detect 由 Quest 级手动 Detect 调用；此外 `TaskCrafting.onItemInternal()` 在同 Task 匹配产物事件后也调用 `detect()`，因此一旦该 Task 被某项匹配输出触发，会为 `ParticipantInfo.ALL_UUIDS` 中的在线 Party 成员逐个读取其个人 `objectCraftStats`。用户明确选择不要求手动 Detect，Base 遂保留匹配产物即时刷新，并增加每 60 tick 自动统计轮询；这是为了目标交互的有意差异，不再描述成 BQ 原生周期行为。周期计划仅针对已发布、启用统计且带 `bq.raw` 的 Crafting Task；轮询以每名在线玩家自己的 UUID 和绝对统计值生成个人事实，Planner 仍可基于该玩家当前 Party 活动资格路由到其独立 PLAYER 进度，不会把 Party 成员计数合并。membership 查询故障导致的当次事件 fan-out 缺口，可能在数据库恢复后由该 peer 自己的下一轮在线轮询补齐；这不等同于事件时 Party 快照，也不保证离线、离队或资格变化后的追补。

补齐锁定进度复评：若 PARTY 任务进度已因成员事件达到目标但当前玩家仍为 LOCKED，自动检测不提前发奖；当该玩家自己的前置满足后，以独立稳定的 `craft-stat-auto-unlock` 事实键重新投影同一 Task，避免先前记录的统计键阻止 Quest 完成/奖励结算。自动目标异常单独记录、不逃出玩家 tick，下一既有 60 tick 周期重试。

CodeGraph `status/sync` 更新本轮 12 个生产/测试文件；`impact` 检查 `QuestObservationPlan.fromPublished`、`AuthenticatedCraftingStatisticsDetectionService.detectAutomatically`、`QuestEvaluationPlanner.requestsFor`、`QuestRuntimeService.applyIf`、`BqPartyActiveQuestQuery.hasActiveMember` 与 `QuestGameplayEventHandler.onPlayerTick`，`affected` 收敛到 Core 检测/Party/Runtime/观察计划测试、根分发/故障测试和 PostgreSQL Party 集成类。Docker Gradle Core 全套 270 项、根分发/故障 5 项、隔离 PostgreSQL 集成类 14 项，共 289 项实际执行、0 skipped/failure/error。临时 PostgreSQL 16 `jgb-r26-auto-crafting-it` 使用 512 MiB tmpfs、无宿主数据卷，只绑定 `127.0.0.1:55490`；测试后容器已移除且端口释放。首轮 Core 回归暴露 LOCKED 解锁复评仍被普通锁定拒绝条件挡住，已最小修复并通过回归；最初无网络构建无法解析 GTNH 插件，后仅为一次性 Gradle 容器使用本机 7892 代理，没有修改系统网络/代理或 Compose 环境。根 `:assemble :verifyUi2SelfContained` 成功（29 actionable tasks，6 executed、23 up-to-date），`git diff --check` 通过。

该自动证据使用合成定义、玩家 ID 和统计读取器，不调用 Forge 玩家 tick、真实 `EntityPlayerMP.objectCraftStats` 或生产 Party 在线目录。R.2.6 自动门槛完成，Crafting/M.2 行为兼容仍 `BLOCKED`。本轮没有访问生产数据、执行生产 DDL、部署、启用 Quest/停用 BQ 或触碰 S1/S2。

### 2026-10-10 - R.2.6 LOCKED 个人进度解锁 JDBC 补充

新增 `lockedPartyCraftingStatisticsUnlockThroughPersonalStableFactInJdbc`，覆盖 PARTY 在线 peer 的个人 Task 已由统计快照填满、但自身 prerequisite 未满足的组合：第一次检测只持久化 LOCKED 进度，不产生 reward entitlement；peer 自己完成 prerequisite 后，新的 `craft-stat-auto-unlock` 稳定事实键完成 Quest，仅创建一条奖励权益；后续检测无新事实。该测试把在线 Party 活跃 Quest 资格、个人 PLAYER 统计、锁定进度与 JDBC 奖励结算串在一起，不把 Party 统计合并。

CodeGraph `status` 为 up-to-date；`impact detectAutomatically` 覆盖检测服务、QuestModule、玩家 tick handler 与相关集成用例，`impact QuestRuntimeService.applyIf` 命中检测服务，`affected` 选择根 PostgreSQL 集成类。最新定向验收 Core 270、根分发/故障 5、隔离 PostgreSQL 集成类 15，共 290 项实际执行、0 skipped/failure/error；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。PostgreSQL 16 仅在 512 MiB tmpfs、无宿主卷和 loopback 临时端口运行，测试后已清理。该自动证据仍不覆盖真实 Forge tick、玩家统计文件、生产 Party 目录或跨服在线时序，故 R.2.6 自动门槛完成但 Crafting/M.2 仍 `BLOCKED`。

### 2026-10-11 - R.2.34 Crafting 事实归属与 Detect 版本发布合同（当前决定）

用户确认并已实现：

- 匹配 Crafting 产物事实在 Forge 事件回调处冻结 Planner 求出的完整评估请求，包括当时可处理的 Quest/定义版本、在线活动资格、完整 PARTY roster 与各自 PLAYER 进度目标。后续同进程数据库重试仅复用该请求，不依据变化后的 Party/资格重新分配。
- 无法从权威数据库取得事件时快照时 fail-closed：错误记录后拒绝该事实，不把不可信事实排队等待之后重新归属。
- 延迟事实队列为进程内有界 FIFO：最多 1,024 条、最多估算 8 MiB；每 20 tick 最多重试 32 条。正常停服清空，进程崩溃丢失尚未被 PostgreSQL 接收的事件。该合同未提供跨崩溃持久化或 exactly-once。
- 管理员可在 Quest 消耗 Detect 批次活动时发布新版。该批次按固定旧 `definition_version`、Task 游标、周期和旧奖励继续；数据库活动唯一性以 participant+Quest 为准，禁止跨版本并行批次。新版不重写旧版进度；若定义删除了进行中的旧目标，管理员可在需要时手工补偿。

R.2.34 回归覆盖冻结 Party/资格快照与成员变化后的 FIFO 重试、大型冻结定义快照触发 8 MiB 拒绝、快照失败 fail-closed，以及隔离 PostgreSQL 中“旧版批次暂停 → 发布新版 → 入口恢复旧批次”的精确版本授权和迁移幂等。Core 292 项、Quest PostgreSQL 61 项、全工程 1,082 项（1,030 实际执行、52 条非 Quest PostgreSQL/Warehouse 环境假设跳过）0 failure/error；`assemble` 与自包含检查通过。只证明代码与隔离环境合同，不证明真实 Forge/GTNH 事件时序、生产 DDL 或线上玩家物品崩溃窗口。M.2 与 BQ 替代仍 `BLOCKED`。更早段落中的 R.2.19/版本发布“待确认”描述均为 R.2.34 前状态，已由本节覆盖。

### 2026-10-11 - R.2.33 PlayerTick 周期谓词回归

玩家 tick 仍按既有 20/60/100 周期分别采集背包、复评 Crafting/其它周期观察、采集位置；实现将周期边界算术提取为 `PeriodicTickCadence.isDue`，没有改变频率或任务判断。根 Gradle 定向运行 `QuestCraftingStatisticsDispatchTest` 与 `PeriodicTickCadenceTest`：分别 4、2 项，共 6 项通过，0 skipped/failure/error；`compileJava` 与 `compileTestJava` 通过。此前无网络尝试在插件元数据阶段失败，检查本机 7892 代理仍监听后通过代理重跑成功。另一次独立 JUnit helper 运行 2 项也通过。

随后隔离运行 `:assemble verifyUi2SelfContained` 成功，29 actionable tasks（7 executed、22 up-to-date）。该门禁不覆盖真实 Forge PlayerTickEvent 派发、线程或联机时序，也不是全量测试。M.2 保持 `BLOCKED`。

### 2026-10-11 - R.2.31 Crafting 统计按来源服逐需求门控

固定 BQ `TaskCrafting.onItemInternal()` 先把本次产物事件计入匹配需求（事件进度按目标封顶），随后 `detect()` 对每个需求单独检查：只有该需求当前值仍低于目标时才读取该来源服 `objectCraftStats`；尚未达标的需求允许被绝对统计一次性推进到目标以上，已由事件达标的需求则保持事件值。Base 原先会对事件同一需求的后续统计快照照常写入，导致同来源该分量从目标值被抬高至历史统计总量。

`ItemVectorTaskEvaluator` 现在根据当前来源的事件/统计最大值逐分量执行相同门槛。其它来源仍各自采集，并按用户确认的来源内 max、来源间 sum 归并；其它未达标需求仍保留超目标绝对快照。新增 Core 回归覆盖一个来源的事件达标分量、另一个来源同分量以及仍未达标的第二需求。

CodeGraph 起始状态 up-to-date；`explore` 对照 `ItemVectorTaskEvaluator.evaluate`、`TaskProgress.SourceContribution` 与固定 BQ `TaskCrafting.detect/onItemInternal`；`impact ItemVectorTaskEvaluator.evaluate` 覆盖 14 个符号，`affected` 返回 56 个宽候选，按直接链选 `BqCompatibleTaskEvaluatorsTest`。`sync` 更新 2 个 Java 文件。离线 Gradle 在 Core 全套强制运行 62 suites / 290 tests，0 skipped/failure/error；目标 Crafting evaluator 类单独运行也通过。Docker 离线测试容器未能通过 GTNH 插件 DNS 配置，未进入用例；改用本机 JDK 17 与已缓存 Gradle 的 `--offline` 路径成功。未运行 PostgreSQL、根 Forge 测试、assemble、自包含门禁；未访问生产数据库/玩家进度、执行 DDL、部署或切换 Quest/BQ。Crafting 与 M.2 仍 `BLOCKED`。

### 2026-10-11 - R.2.32 PARTY Crafting 在线资格与全 roster fan-out 组合回归

新增 `oneOnlineEligiblePartyMemberActivatesCraftingAndFansOutToTheEntireRoster`，将两条既有分层规则放进一个 Core 场景：触发者在线但未解锁、第二名成员在线且满足前置、第三名成员离线且未解锁；匹配 Crafting 产物被规划为三名 PARTY roster 成员各自的 PLAYER 进度请求。随后把唯一合格成员设为离线，同一事件不再生成请求。它确认“在线活动 Quest 并集负责本服事件门控，资格通过后完整 PARTY roster 接收事件进度”，不将 Crafting 统计变成 Party 合并值。

CodeGraph 起始 `status` 为 up-to-date；`impact BqPartyActiveQuestQuery.hasActiveMember` 与 `impact BqHuntPartyAssignmentPolicy.select` 各覆盖 8 个符号，`affected` 收敛到 `BqPartyActiveQuestQueryTest`；`sync` 更新新增回归所在测试文件。Docker `galaxy-dev:local` 离线定向运行该 Core 测试类：9 项，0 skipped/failure/error。首次使用宿主 UID 启动时在共享 Gradle 配置缓存 keystore 权限处退出，未进入测试；随后以隔离 buildDir 重跑成功，容器日志中的 Git dubious-ownership 提示由既有 `NO-GIT-TAG-SET` 版本覆盖处理。`git diff --check` 通过。本轮没有生产代码、PostgreSQL、Forge 构建、DDL、部署或 Quest/BQ 开关变化。此用例不覆盖队列积压期间的 Party/资格变化、事件时快照、真实 Forge 回调或跨服运行，R.2.19 与 M.2 仍 `BLOCKED`。

### 2026-10-10 - 当前决策补记：来源服 Crafting 归并与 Quest Detect

用户确认同一玩家 Crafting 统计按来源服分别取 `max(本服事件累计, 本服绝对统计快照)` 后求和；Party 成员之间仍保持各自 PLAYER 进度。Base 已以 source-scoped TaskProgress、legacy baseline 和 JSONB 数据列实现该口径，Core 与隔离 PostgreSQL 往返测试通过。新增 schema migration 未执行。

用户同时确认消耗任务应使用 Quest 级 Detect：客户端只发送 Quest ID，服务端从发布定义取得任务顺序，只处理未完成消耗任务，每 Task 独立持久化并可恢复；跨多个库存/数据库步骤不承诺全有或全无。**本段写作时功能仍未实现，之后 R.2.17 已完成服务端持久批次与游标恢复；用户选择批次末统一结算 Quest 完成/奖励，R.2.26 补齐 simultaneous 转换边界。**新定义版本发布与旧批次恢复的并发合同仍未解决，见开发导航中的当前风险。

### 2026-10-10 - R.2.15 BQ Item 最大数量到背包溢出投递

固定 BQ `RewardItem.claimReward0` 通过 `BigItemStack.getCombinedStacks()` 按物品最大堆叠拆分奖励，再逐栈尝试放入玩家主背包，未放入的物品掉落到世界。新增合成 BQ typed JSON `Count=8192` 经 importer 到 Minecraft item delivery adapter 的集成回归：36 格主背包接收 2,304 个物品（36×64），余下 5,888 个拆成 92 个 64-stack 溢出；合计仍为 8,192。验证 entitlement marker 已保存，重复投递不再产生掉落或额外 flush。

`BqItemRewardImportDeliveryTest` 4 项与 `MinecraftRewardDeliveryTest` 28 项，共 32 项实际执行、0 skipped/failure/error。CodeGraph `status` up-to-date；`explore/node` 检查 importer、拆栈、交付、overflow 与 marker，`impact` 覆盖 item 拆栈/交付链，`affected` 收敛到导入交付测试；`sync` 更新 1 个测试文件/63 节点。S2 `DefaultQuests` 只读匿名聚合的来源哈希及计数与既有基线一致，未读取玩家进度或数据库。

此回归使用合成 JSON、fake player/inventory/drop sink，不调用真实 EntityPlayer、World 或 GTNH 物品注册，也没有验证 World drop、玩家 NBT 与 PostgreSQL entitlement 的崩溃原子性。正常路径总量守恒和 marker 重放幂等不等于跨持久化边界 exactly-once；M.2 与奖励系统继续 `BLOCKED`。

### 2026-10-10 - R.2.16 完整导入 QuestCompletion 来源/目标定义

将 R.2.14 中手工构造来源/目标 Quest 的限制收窄：现有隔离 PostgreSQL Worker 测试的来源与目标定义均由完整合成 BQ typed JSON 经 importer 导入并发布。来源含非消耗 Retrieval 与 QuestCompletion 奖励，先经 `BqPartyInventorySnapshotFactSink` 的 PARTY 快照路径完成，再由玩家显式 claim；JDBC `RewardDeliveryWorker`/router 投递至导入的目标 Quest。目标含 Checkbox Task 与 Item reward。断言目标 PLAYER 进度/Task 完成、目标奖励 entitlement 仅一条、后续 poll 不重租来源 entitlement。

整个 `BqPartyInventorySnapshotFactSinkPostgresTest` 在 PostgreSQL 16 tmpfs 临时库实际执行 15 项，0 skipped/failure/error；只绑定 `127.0.0.1:55491`，无宿主数据卷，测试结束后容器和端口已释放。CodeGraph `sync` 更新测试 1 文件/217 nodes；`impact` 检查 importer 入口及该方法，`affected` 选择同一隔离集成类。

这仅验证合成的完整 Quest 定义进入 importer/JDBC 路径，不代表导入完整 S2 DefaultQuests、真实 BQ 目标完成时机、Forge/GTNH 玩家存档或跨服 Worker 竞争。QuestCompletion、奖励系统及 M.2 继续 `BLOCKED`。

### 2026-10-10 - 检测入口粒度复核（历史待确认状态，已由后续决策覆盖）

固定 BQ `EventHandler` 将玩家库存变化排队，并对 Quest 的 `ITaskInventory.onInventoryChange` 自动回调；非消耗 Retrieval/Fluid 会自动检测，消耗型仅在 `autoConsume=true` 时由该回调执行。BQ 玩家页面另有 Quest 级 Detect：`QuestInstance.detect` 校验 Quest 可提交后，按定义顺序调用所有未完成 Task 的 `detect`，consuming Retrieval/Fluid 会扫描当前玩家库存并消耗匹配物品/容器。S2 只读画像中 307 个 consuming Retrieval 全部 `autoConsume=false`，唯一 consuming Fluid 也为 `autoConsume=false`，因此这些内容依赖玩家显式检测/提交。

Base 当前每 20 tick 自动采集非消耗玩家背包快照，消耗型动作按单个 Task 认证提交；它没有 BQ Quest 级“全部未完成 Task Detect”入口。该段记录当时尚待选择的状态；2026-10-10 用户随后已确认 Quest 级 Detect，并规定按任务定义顺序逐项持久化、支持恢复，不要求跨数据库/库存全有或全无。实施前进一步发现当前引擎逐条事实即时结算并冻结 Quest，OR/可选任务完成时机尚待确认；在该边界解决前不改任务消费或奖励时机。

### 2026-10-10 - R.2.6 LOCKED 个人进度解锁 JDBC 补充

新增 `lockedPartyCraftingStatisticsUnlockThroughPersonalStableFactInJdbc`，覆盖 PARTY 在线 peer 的个人 Task 已由统计快照填满、但自身 prerequisite 未满足的组合：第一次检测只持久化 LOCKED 进度，不产生 reward entitlement；peer 自身前置完成后，新的 `craft-stat-auto-unlock` 稳定事实键完成 Quest，仅创建一条奖励权益；后续检测无新事实。该测试把在线 Party 活跃 Quest 资格、个人 PLAYER 统计、锁定进度与 JDBC 奖励结算串在一起，不把 Party 统计合并。

CodeGraph `status` 为 up-to-date；`impact detectAutomatically` 覆盖检测服务、QuestModule、玩家 tick handler 与相关集成用例，`impact QuestRuntimeService.applyIf` 命中检测服务，`affected` 选择根 PostgreSQL 集成类。最新定向验收 Core 270、根分发/故障 5、隔离 PostgreSQL 集成类 15，共 290 项实际执行、0 skipped/failure/error；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。PostgreSQL 16 仅在 512 MiB tmpfs、无宿主卷和 loopback 临时端口运行，测试后已清理。该自动证据仍不覆盖真实 Forge tick、玩家统计文件、生产 Party 目录或跨服在线时序，故 R.2.6 自动门槛完成但 Crafting/M.2 仍 `BLOCKED`。
