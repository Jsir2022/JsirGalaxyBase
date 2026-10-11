# BQ M.2 自动验收收口目标

日期：2026-10-11。阶段状态仍以能力矩阵为准；本文件记录本次有边界的执行目标。

## 目标 Prompt

按开发导航、权威能力矩阵和真实 GTNH 内容，完成 M.2 自动验收收口：
核对实际任务与奖励的剩余兼容缺口，区分代码缺失、产品差异与运行验证缺失；
优先核对 Checkbox 与奖励恢复，补齐证实存在的最小修复及故障回归；
整理过期待办，更新导航、能力矩阵与 Worklog，使用隔离数据库完成测试、
assemble、verifyUi2SelfContained 和 git diff --check，形成本地稳定 Git 提交。
保持 Base 自包含、PostgreSQL 权威及现有 M.2 模型。涉及业务合同变更先报告。
不开展生产进度写入、生产 DDL、部署、Quest 开关或 BQ 停用。
不能将自动收口表述为 BQ 全面替代。明确 M.3 入口和未验证运行边界。

## 本次步骤

| 步骤 | 交付 | 状态 |
|---|---|---|
| S1 | 对照当前代码、固定 BQ 源码与内容频率，建立有限缺口清单 | 已完成 |
| S2 | Checkbox 周期与奖励恢复回归；只修复有证据的问题 | 自动验收完成 |
| S3 | 整理历史记录与当前待办；明确 M.3 前置条件 | 已完成 |
| S4 | 隔离自动验收、运行 JAR 检查与本地 Git 检查点 | 已完成；代码提交 d74844c，文档随本文件单独提交 |

## 已发现的具体问题

重复 Checkbox 入口仅按 completedAt 判定下一周期，而 Runtime 同时遵守待领奖、
待消费恢复和领取后的 repeat anchor。提前点击可能消耗下一周期的稳定事实键，
使真正允许进入下一周期时的点击无法推进。修复必须在记录事实前遵守已有规则，
并在 Runtime acceptance 事务中再次确认目标周期，避免授权与写入之间的状态变化。

## 固定边界

事件时 Crafting 请求快照为进程内有界重试；未被数据库接收的事实可能在关服、
进程崩溃或队列拒绝时丢失。物品奖励的世界掉落与玩家 marker/数据库确认之间
仍存在崩溃重放窗口。该限制必须如实保留，不能以增加测试宣称 exactly-once。
观察站继续延期；非当前内容使用的 GLOBAL/命令等扩展不擅自加入本批。
本批完成后的 M.2 实机状态仍单独列出。

## 实际内容覆盖与有限缺口

来源为已授权只读的 S2 DefaultQuests 历史画像；本次没有重新读取服务器玩家数据。
9 类 Task 合计 5,620 项，4 类 Reward 合计 5,448 项。
下表中的自动证据指源码/合成快照/隔离 PostgreSQL，不代表完整 DefaultQuests 实机等价。

| 内容 | 已有自动证据 | 剩余边界 |
|---|---|---|
| Retrieval 3,863 / Optional 1,267 | 物品/meta/NBT/OreDict、需求顺序、库存下降、groupDetect、optional、PARTY 租约、消费恢复 | GTNH 注册表/真实背包与跨服在线租约时序；观察站延期 |
| Checkbox 203 | 认证身份、任务/版本绑定、前置、手动确认；R.2.35 重复周期/claim anchor/消费 fence | 当前已支持 PLAYER；fullySyncQuests=true、TEAM/PUBLIC 不宣称兼容，真实配置加载未验证 |
| Crafting 133 | craft/smelt/anvil、个人统计、来源内 max/来源间求和、周期隔离、事件时 roster、快照失败拒绝 | Forge 回调与统计读取；内存事实未被数据库接收时的丢失边界 |
| Hunt 104 / Location 33 / Meeting 1 / Block Break 1 | BQ 条件匹配、位置/AABB、目标封顶、重复、PARTY 与 JDBC 回归 | Forge 实体/方块事件、FakePlayer 过滤和多人实际时序；不能将 Crafting 的归属快照承诺扩展给这些事件 |
| Fluid 15 | 非消耗快照、PARTY 聚合、手动消费、实际超额向量、Tag 精确匹配与恢复 | Forge 流体容器、满背包返回容器、真实保存顺序 |
| Item 4,181 / Choice 957 | typed NBT、变量替换、大堆叠、个人选项、部分入包/重放、延迟旧周期恢复 | World drop 与 marker/租约确认非原子；真实玩家/世界保存和多服租约 |
| QuestCompletion 308 | 完整合成来源领取→JDBC Worker→目标完成；缺目标 no-op、幂等与个人归属 | BQ tick、full reset、版本与真实多人运行尚未完整对照 |
| XP reward 2 | 导入 levels/points、1.7.10 曲线、marker、flush 失败恢复 | 真实 EntityPlayerMP/客户端经验同步、跨服保存 |

当前盘点的 GLOBAL、任务 XP/Scoreboard/Interaction、Command/Scoreboard reward 为零，
现有通用实现或字段保存不构成兼容承诺；不为零使用类型扩展本次工作。
64 个未放置 Quest 与 4 个 seeded XOR cycle 警告是内容可见性/可达性复核项，
不自动补坐标，也不能把结构警告放行当成实际路径已验证。

## 剩余事项按性质归档

| ID | 性质 | 当前处理 |
|---|---|---|
| A1 | 代码缺陷：Checkbox 提前占用下周期键 | R.2.35 修复；Core 故障回归及隔离导入/claim/换服实例回归 |
| A2 | 自动验证 | 全工程及隔离 Quest PostgreSQL/Importer；构建、自包含和 Java2D Golden |
| R1 | 运行验证 | 真实 Forge/GTNH task/reward/库存/保存与跨服租约时序；单独运行门槛 |
| R2 | 已知故障边界 | 世界掉落崩溃重放、事件内存队列丢失；已声明，未改变奖励/持久队列策略 |
| P1 | 范围限制 | fullySyncQuests=true/GLOBAL/非 PLAYER 消耗/观察站/多 Quest claim-all；不宣称支持 |
| C1 | 内容复核 | 未放置与 XOR 入口可达性；不能自动改写原内容 |
| M3 | 下一阶段代码缺口 | 进度迁移写入、审计批次、回滚与重复周期冲突决策 |

R1 不意味着每个条目都需要继续写 Java；发现真实差异后再生成有限修复项。
没有新证据时不得重复增加镜像测试或把旧“待确认”作为新任务。

## M.3 下一工作包的前置条件

1. 自动收口基线构建通过并留本地提交；M.2 实机 BLOCKED 单独保留。
2. 新工作包优先实现离线 dry-run 和冲突隔离，不先写生产数据。
3. 明确 BQ 快照无单调 repeat cycle 时的来源选择规则。不同来源 snapshot
   不可只按最大向量/最新 timestamp 拼接；该业务选择仍需用户确认。
4. 定义、版本与章节映射固定到迁移 manifest；进度完成/领取与奖励 entitlement
   的映射必须避免重复发奖，并保留来源摘要和预迁移值用于回滚。
5. 隔离测试覆盖重复运行、失败事务、冲突、回滚保护和跨玩家隔离。
   未完成这些项前，M.3 写入器与生产替代仍标未完成。

## 本次自动验收

- 初次运行三个新增 Core 回归均失败，修复后整个 Core 295 项通过。
- 隔离 PostgreSQL 模块 61 项、Importer 52 项均实际执行，零跳过/失败/错误；
  新集成用例从 typed BQ 定义导入开始，覆盖提前确认不落事实、领取后冷却、
  重建连接/服务并以另一来源服确认下一周期、重复点击及其他玩家隔离。
  服务实例替换不是实际 Minecraft 多服验收。
- 全工程 227 suites / 1,086 tests：1,034 执行，52 assumption-skip，零失败/错误。
  48 项为其它业务缺少各自 PostgreSQL 集成配置，4 项为 Warehouse 缺少
  `JGB_BANKING_IT_*` 专用配置。Quest 数据库测试均已执行，不以跳过代替通过。
- `test assemble verifyUi2SelfContained :ui2-demo:renderGolden` 成功，56 tasks 全部执行。
  字体仅只读挂载作 Java2D 测试预览，不进入运行 JAR。
- 最终 JAR 中有 Quest Core/PostgreSQL；没有 BQ importer、BQ 类或 Gson 导入 CLI。
- CodeGraph 改前查 Checkbox confirm callers/callees、reward deliver impact；
  改后同步 4 files/193 nodes 与 1 file/19 nodes，confirm impact 14 symbols，
  affected 51 个候选，以全工程回归覆盖。旧索引引擎提示仍保留，不宣称已重建。
- 代码提交后重新生成依赖 jars（13 tasks）并执行 `:assemble :verifyUi2SelfContained`
  （29 tasks）成功。运行 JAR 为
  `.gradle/codex-isolated-build-r234/_root/libs/jsirgalaxybase-d74844c-main+d74844cb26-dirty.jar`，
  SHA-256 `965cb0bb595c6b066147f102db81b6faaea6d7e78fee121673003d4bda430fc1`。
  `dirty` 是构建时尚未提交的文档，不存在未提交 Java；没有部署。
- 临时 PostgreSQL 已销毁，loopback 55436 释放；临时 init 脚本删除。
  当前完成的是本次有限自动收口，M.2 实机 BLOCKED 及所有 M.3–M.6 缺口仍保留。
