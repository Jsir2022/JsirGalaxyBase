# BQ 替代：Base-only 新服实施路线

日期：2026-10-11。本文是当前活动路线；阶段事实以[能力矩阵](betterquesting-capability-absorption-matrix-2026-09-13.md)为准，历史研究保留在 Worklog 和旧专题文档中。

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

## 当前两天工作包

| 阶段 | 交付 | 状态 |
|---|---|---|
| N.1 发布门禁与个人语义 | 真实 9 类 Task/4 类 Reward 的机器可执行运行清单；未知、仅可解析或参数非法类型阻断；导入定义显式 PLAYER/个人模式，切断旧 Party 聚合和事实扇出 | 自动验收完成，`adfb691` |
| N.2 完整版本化导入 | `plan → dry-run → preview → apply → rollback`；空库全量导入、相同来源幂等、变化内容版本化、后续依赖保护、来源证据与 Base-only 初始化说明 | 自动验收完成，`adfb691` |
| N.3 全新玩家运行闭环 | 九类 Task、四类 Reward、前置/秘密/重复/simultaneous、Quest Detect、Choice、重启/重试/重复消息和严格一次奖励 | 代表性自动闭环与逐项证据核对完成；见验收文档，实机未验证 |
| N.4 终端与管理员健康页 | 玩家章节/详情/进度/Detect/领取/Choice；管理员只读来源、批次、类型、警告、失败与恢复队列摘要 | 自动验收完成，`664436e`；三档视觉覆盖 |
| N.5 自动收口 | 隔离 PostgreSQL、UI2 三档视觉、根测试、assemble、自包含、JAR 内容、diff、CodeGraph affected；分阶段本地提交 | 自动验收完成：1,113 项、1,061 执行、52 非 Quest 跳过、0 failure/error；252 视觉通过，最终制品和证据见验收文档 |

## N.1 运行能力清单

当前真实任务书允许发布的 Task：`retrieval`、`optional_retrieval`、`crafting`、`hunt`、`location`、`meeting`、`fluid`、`block_break`、`checkbox`。

当前真实任务书允许发布的 Reward：`item`、`choice`、`questcompletion`、`xp`。

`interact_item`、`interact_entity`、Task XP/Scoreboard、Reward Command/Scoreboard 即使 codec 能解析，也保持 `ADAPTED`，未来内容一旦实际出现便阻断 apply，直到拥有运行处理器和自动证据。发布门禁必须检查实际 occurrence，而不是把“能解析”当作“能运行”。

新导入的每个 Task 携带 `bq.partyShare=false`；缺少该字段的历史已发布定义保持旧兼容语义，避免静默改写既有版本。新定义的个人事实只能写入触发玩家的 PLAYER 行，Party 聚合库存、事件扇出和 Party 在线资格都不得参与。

## N.2 导入合同

离线命令保持 `plan / dry-run / preview / apply / rollback`。默认只读；`preview/apply/rollback` 需要显式 `BQ_MIGRATION_WRITE_CONFIRM=ISOLATED_ONLY` 和数据库配置。新服初始化只允许指向明确隔离或经生产变更审批的空目标库；本工作包只使用临时 PostgreSQL。

完整任务书自动门禁至少证明：首次创建 3,744 Quest + 47 章；重复 apply 复用同一批次且不新增版本；单个内容变化只版本化变化实体；失败事务不留下部分写入；较早批次存在后续版本时拒绝回滚；最新无依赖批次可回滚。64 个未放置 Quest 由终端虚拟 `Other quests` 分组展示，不伪造坐标。

## 后续唯一入口

本工作包完成后，下一阶段是 **Base-only 新服人工运行验收与灰度启用准备**：在全新玩家、全新数据库、无 BQ Mod 环境检查真实 Forge 事件、物品/流体、奖励落地、重启和终端交互。它不是旧进度迁移，也不是双算影子。任何生产 DDL、部署或开关变化仍需单独授权。
