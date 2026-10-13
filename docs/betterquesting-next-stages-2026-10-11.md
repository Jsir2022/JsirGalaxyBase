# BQ 替代：Base-only 新服实施路线

日期：2026-10-12。本文是当前活动路线；阶段事实以[能力矩阵](betterquesting-capability-absorption-matrix-2026-09-13.md)为准，历史研究保留在 Worklog 和旧专题文档中。

## 产品边界

- 新服务器不安装 BetterQuesting；Base 不依赖 BQ。
- 导入只处理 GTNH `DefaultQuests` 的定义、章节和展示顺序，不读取或迁移旧玩家进度、领取历史、Choice 历史或旧 BQ Party。
- 所有导入 Quest 均为 `PLAYER` 个人进度。玩家加入 Base Party 后，库存、合成、击杀、位置等事实也不得向队友扇出。
- Base 自研 PARTY/TEAM/PUBLIC 框架继续保留，未来只用于明确设计的公共任务和职业玩法。
- 内容重复导入采用来源哈希与逐实体内容哈希：相同内容幂等，变化内容发布新版本，旧进度仍绑定旧版本。
- 自动验收不等于生产切换；本路线不授权部署、生产 DDL、Quest 开关或停用旧服 BQ。

## 基线证据（已完成，不再列为活动待办）

- UI2 玩家任务中心、管理员编辑入口、Quest Core、PostgreSQL 进度/奖励/消费恢复框架已存在。
- 离线 importer 可解析 split `DefaultQuests`、章节、顺序、typed NBT 与原始 BQ 参数；Gson、CLI 和 BQ 类不进入运行 JAR。
- 固定只读来源哈希 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`：3,744 Quest、5,620 Task occurrence、5,448 Reward occurrence、47 章、3,847 个放置、64 个未放置 Quest、4 个 seeded XOR 警告，无未知类型。
- M.0/M.1/M.2 的解析、求值器、奖励、故障恢复与隔离自动证据折叠为历史基线；不再推进旧 M.3 玩家进度迁移、旧 Party 映射或 M.5 BQ/Base 实时影子。

## 开发基线与当前阶段

| 阶段 | 交付 | 状态 |
|---|---|---|
| N.1 发布门禁与个人语义 | 真实 9 类 Task/4 类 Reward 的机器可执行运行清单；未知、仅可解析或参数非法类型阻断；导入定义显式 PLAYER/个人模式，切断旧 Party 聚合和事实扇出 | 自动验收完成，`adfb691` |
| N.2 完整版本化导入 | `plan → dry-run → preview → apply → rollback`；空库全量导入、相同来源幂等、变化内容版本化、后续依赖保护、来源证据与 Base-only 初始化说明 | 自动验收完成，`adfb691` |
| N.3 全新玩家运行闭环 | 九类 Task、四类 Reward、前置/秘密/重复/simultaneous、Quest Detect、Choice、重启/重试/重复消息和严格一次奖励 | 代表性自动闭环与逐项证据核对完成；见验收文档，实机未验证 |
| N.4 终端与管理员健康页 | 玩家入口与管理员只读健康摘要 | 原入口/有限视觉自动验收完成，`664436e`；真实内容展示缺口现列入 N.9.1，不能称完整任务书产品已验收 |
| N.5 自动收口 | 隔离 PostgreSQL、UI2 三档视觉、根测试、assemble、自包含、JAR 内容、diff、CodeGraph affected；分阶段本地提交 | 自动验收完成：1,113 项、1,061 执行、52 非 Quest 跳过、0 failure/error；252 视觉通过，最终制品和证据见验收文档 |
| N.6 Base 运行组合接线验收 | 核对 9 类 Task 求值器和 4 类 Reward 在 Base `QuestModule` 启动组合中的实际接线，并覆盖事件入口注册、Reward Router 唯一映射和导入内容到运行器的代表性闭环 | 自动验收完成：`QuestModuleTest` 6 项通过；完整任务/终端回归、252 视觉、自包含与制品检查通过，详见验收文档 |
| N.7 固定任务书参数门禁 | 审计来源任务书 5,620 个 Task 与 5,448 个 Reward occurrence 的类型、参数键、布尔值和目标引用；未知键/非法 flag fail-closed，修正默认值与目标引用校验 | 自动验收完成：固定来源完整审计；Importer 81 项及隔离数据库全量门禁通过。语义等价和 Forge 运行仍未验收，详见 N.7 证据 |
| N.8 参数语义符合性矩阵 | 将固定 DefaultQuests 的真实参数 profile 对照 BQ 规则和 Base importer/evaluator/Detect/reward/授权链，修复证实的语义差异 | 自动验收完成：来源 profile 贯通 importer/evaluator、Quest Detect/授权/隔离 PG，奖励处理及 Minecraft ItemStack matcher 有来源数据回归；Core/Postgres/Importer 459 项全执行通过，全工程 1,143 项、52 项非 Quest 环境跳过、0 failure/error；assemble、自包含、252 视觉通过 |
| N.9.1 玩家完整性与隔离准备 | 完整详情数据、全部目标/奖励/Choice 可达、长说明/搜索/前置、真实内容视觉门禁、隔离初始化预检 | 自动验收完成：1,159 tests、0 skip/failure/error；固定源全链审计、6 个边界 Quest 的 409 帧/202 次转换、81 帧 HTML/PNG 画廊；隔离 CLI apply 后对本轮 Base JAR 执行只读 preflight；assemble、自包含与 252 终端视觉通过。非 Forge/玩家实测 |
| N.9.2 Base-only 新服运行验收 | 在不含 BetterQuesting 的隔离新服/新玩家环境验证启动、真实 Forge 事实、物品/流体、奖励存档、重启恢复和终端操作 | A 部分完成：运行目录/JDK/classpath 检查通过；Forge 在 EULA 提示退出，未加载 Base/Quest 或创建世界。Quest/BQ 隔离自动测试通过；Forge runtime 与玩家链未验收。需用户明确接受 EULA 才能继续启动。禁止生产组服务镜像、终止进程脚本及正式数据库/DDL/部署/开关。见[N.9.2 隔离运行验收](betterquesting-base-only-runtime-acceptance-2026-10-12.md) |

## N.1 运行能力清单

当前真实任务书允许发布的 Task：`retrieval`、`optional_retrieval`、`crafting`、`hunt`、`location`、`meeting`、`fluid`、`block_break`、`checkbox`。

当前真实任务书允许发布的 Reward：`item`、`choice`、`questcompletion`、`xp`。

`interact_item`、`interact_entity`、Task XP/Scoreboard、Reward Command/Scoreboard 即使 codec 能解析，也保持 `ADAPTED`，未来内容一旦实际出现便阻断 apply，直到拥有运行处理器和自动证据。发布门禁必须检查实际 occurrence，而不是把“能解析”当作“能运行”。

新导入的每个 Task 携带 `bq.partyShare=false`；缺少该字段的历史已发布定义保持旧兼容语义，避免静默改写既有版本。新定义的个人事实只能写入触发玩家的 PLAYER 行，Party 聚合库存、事件扇出和 Party 在线资格都不得参与。

## N.2 导入合同

离线命令保持 `plan / dry-run / preview / apply / rollback`。默认只读；`preview/apply/rollback` 需要显式 `BQ_MIGRATION_WRITE_CONFIRM=ISOLATED_ONLY` 和数据库配置。新服初始化只允许指向明确隔离或经生产变更审批的空目标库；本工作包只使用临时 PostgreSQL。

完整任务书自动门禁至少证明：首次创建 3,744 Quest + 47 章；重复 apply 复用同一批次且不新增版本；单个内容变化只版本化变化实体；失败事务不留下部分写入；较早批次存在后续版本时拒绝回滚；最新无依赖批次可回滚。64 个未放置 Quest 由终端虚拟 `Other quests` 分组展示，不伪造坐标。

## 后续唯一入口

N.8 的自动阶段现已完成：固定 SHA 来源画像覆盖全部实际 Task/Reward 类型、已观察布尔 profile、目标数量/维度/流体量与 Retrieval 系列全部 distinct Item Count；来源消耗 Retrieval/Fluid 经过导入、Quest 级 Detect、服务端授权和隔离 PostgreSQL 进度持久化。Item/Choice/XP 来源奖励到达真实 Base Minecraft 奖励处理器，Quest Completion 到达隔离 PostgreSQL 目标 Quest；固定来源 Retrieval 也经授权请求进入实际 Minecraft `ItemStack` matcher。详见[验收证据](betterquesting-base-only-acceptance-2026-10-11.md)。

N.9.1 自动验收已完成，详见[玩家任务书工作包](betterquesting-player-book-completeness-work-package-2026-10-12.md)。N.9.2 唯一入口是 Base-only 新服运行验收：隔离目录和 classpath 已核实，Forge 尚未越过用户 EULA 接受步骤；隔离 Quest/BQ 测试、全量自动测试、assemble、自包含已通过。用户明确接受 EULA 后再准备隔离 Base-only Forge 服务端、测试数据库/世界与合成玩家，验证启动组合、真实库存/流体事实、奖励持久化和重启恢复。启用运行链前须审查 `questPostgresReadEnabled` 的完整效果与回滚步骤；本阶段不等于生产切换授权。

## N.6：Base 运行组合接线验收

目标是证明“有 evaluator/handler 单测”同时也意味着 Base 启动时真的把实现装进运行链，而不是仅验证孤立类。覆盖范围：

- `QuestModule` 建立的真实 `QuestRuntimeTypeRegistry` 包含全部 9 类导入任务；Minecraft gameplay 事件处理器在 Quest runtime 请求启用时注册。
- `QuestModule` 构造的奖励 handler 清单包含 GTNH 实际 4 类 Reward，`RewardDeliveryRouter` 可无重复地建立唯一映射；其余配置 handler 的存在单独记录，不自动开放导入门禁。
- 用代表性导入定义贯穿 importer → PLAYER 定义 → JDBC 运行时 → Base reward delivery；现有 importer/Postgres/root 测试可复用，避免新造第二套框架。
- 逐项复核事实入口到评估器覆盖；无对应真实 Forge 事件的类型只能明确标出具体适配点，不能用 evaluator 单测伪装平台闭环。

完成证据后更新本文件、[能力矩阵](betterquesting-capability-absorption-matrix-2026-09-13.md)、[验收记录](betterquesting-base-only-acceptance-2026-10-11.md)和 Worklog。

## N.7：固定任务书参数键与解析门禁

固定来源哈希 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc` 的只读审计覆盖 3,744 Quest、5,620 Task occurrence、5,448 Reward occurrence、47 章。Task occurrence 分布为 Retrieval 3,863、Optional Retrieval 1,267、Checkbox 203、Crafting 133、Hunt 104、Location 33、Fluid 15、Block Break 1、Meeting 1；Reward 为 Item 4,181、Choice 957、QuestCompletion 308、XP 2。没有读取 `Players` 或进度数据。

N.7 将 9 类 Task/4 类 Reward 的合法参数键列入运行能力注册表；完整任务书中未出现键与注册表逐项比对，未来出现未知键或非法布尔值会阻断 apply。解析现在拒绝非规范布尔字符串与小数布尔值；未知可见性枚举不再静默回落；XP 奖励缺失 amount 按 BQ getter 默认 0，负数/越界/非法值阻断；QuestCompletion 缺失目标与自引用阻断。完整任务书中 QuestCompletion 目标引用均可解析，missing/self 均为 0。

固定来源的重点行为画像已入验收记录。一个重要边界是 2,494 个 Quest 的 `globalShare=true` 不会被搬成跨玩家共享：本项目对导入任务的明确合同仍是 PLAYER；此处是有意的产品语义差异，不宣称与旧 Party 共享完全相同。N.7 的键/类型/格式 gate 当时尚不等于行为等价；该差距已由下方 N.8 自动验收证据收口。

CodeGraph 在本阶段开始前为 up-to-date；同步 6 个 Java 文件后，Importer 相关变更影响 24 个候选测试，具体入口覆盖能力清单、迁移规划、任务定义解析与标准编解码。Importer 全量 81 项在隔离 PostgreSQL 上通过；跨 Quest/Core/根构建总报告 1,121 项、1,069 执行、52 个非 Quest 环境条件跳过、0 failure/error。另通过 `assemble`、`verifyUi2SelfContained`、终端视觉任务（252 场景）、PNG 导出与 `git diff --check`。52 个 skip 是 Bank 18、Market 15、Land 5、ServerTools 7、Warehouse 4、ItemPolicy 3 的独立测试数据库配置缺失；Quest 模块使用独立临时 PostgreSQL，未跳过。运行 JAR SHA-256 为 `c13f112703b3cb85d7f39d2715509d2a82724df6256f915aea321c942b4463c5`，未发现 BQ/Importer/Gson 运行类。

N.7 不覆盖真实 Forge 事件、物品/流体交互、奖励写入玩家存档和跨服在线时序；不表示 BetterQuesting 替代已可上线。

### N.8 自动验收证据（2026-10-12）

CodeGraph 起始状态有 3 个待同步文件；`sync` 后更新了 3 个文件/371 nodes，新增 Minecraft matcher 测试后再同步 1 个文件/61 nodes，最终 `status` up-to-date。`impact` 覆盖 `BqStandardCodec.rewardParameters`（8 个符号）、`ConsumptionRequestAuthorizer.planQuestDetect`（12 个）、`MinecraftItemRewardDeliveryHandler.deliver`（21 个）、`MinecraftPlayerConsumptionPort.planItems`（8 个）；`affected` 根据五个实际测试文件收敛为 5 个相关测试文件。

保留前轮发现并修复的 int32 越界回绕/小数截断风险。固定来源哈希仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`；画像类 10 项对所有实际 Task/Reward 类型与行为 profile 做盘点，逐值验证来源 Hunt required、Location dimension、Fluid threshold 以及 Retrieval/Optional/Crafting 全部 distinct Item Count 的 importer→evaluator 阈值语义。

新增跨层证据：固定来源 consuming Retrieval 两种 profile 与唯一 consuming Fluid profile，经真实 importer 发布到临时 PostgreSQL，按 Quest Detect 和服务端 `ConsumptionRequestAuthorizer` 生成玩家请求并持久化完成进度；PG 隔离测试的 `ConsumptionPort` 是受控测试替身，不冒充真实玩家背包。另一个根测试将固定来源 Retrieval 授权请求交给真实 Minecraft `ItemStack` matcher/扣除规划器。固定来源 Item/Choice/XP 奖励分别通过 Base Minecraft 奖励 handler 验证一次交付与重放幂等；Quest Completion 真实来源目标在隔离 PG 发布后通过处理器完成，重复交付不重复建立权益。该组合补齐自动语义链证据，但仍不等于在 Forge 服务器上操作真实 `EntityPlayerMP`、GTNH 流体容器或逐个游玩完整任务书。

最终强制 Quest 模块为 Core 299、Postgres 61、Importer 99，共 459 项全执行通过，0 skip/failure/error。全工程 XML 汇总 1,143 项，其中 52 项非 Quest PostgreSQL 环境 assumption-skip、1,091 项执行通过，0 failure/error；跳过仍来自 Bank/Market/Land/ServerTools/Warehouse/ItemPolicy 的独立测试数据库配置。`assemble`、`verifyUi2SelfContained`、Golden 输出和 252 份终端视觉结构检查通过；Gradle 57 actionable tasks，4 executed、53 up-to-date。JAR SHA-256 为 `c13f112703b3cb85d7f39d2715509d2a82724df6256f915aea321c942b4463c5`，只用于本地自动验收，不是部署制品。自包含检查通过，JAR 未含 BQ Mod 包、Importer CLI/Gson 或 Java2D Demo 运行代码；保留 BQ 来源声明/许可证元数据及 Base 内部 `BqImportedTaskScope` 命名类。

没有读取 Players/生产进度、连接生产数据库、执行生产 DDL、部署、启用 Quest 或停用 BetterQuesting。临时 PostgreSQL 容器使用 tmpfs、无宿主数据卷，测试后已移除。N.8 自动验收完成；N.9 Base-only 新服真实运行、重启恢复和玩家交互仍未开始，也不是本地自动报告可代签的事项。
