# BetterQuesting 替代：下一长期目标交接单

日期：2026-09-17
状态：M.1 自动验收完成，待本轮提交推送；下一目标为 M.2 行为兼容

## 结论

Base 目前不是“只差跨服”或“已经替代 BetterQuesting”。现有代码已经具备任务定义、章节、进度、奖励、
PostgreSQL、UI2 任务中心和跨服主体等基础能力，但距离玩家能够安全停用 BetterQuesting 仍有四条关键链路：

1. 把现有 GTNH 任务书定义和章节可靠导入 Base；
2. 逐类证明 Task/Reward 的运行行为与 BetterQuesting 一致；
3. 合并各服务器玩家进度且绝不重复发奖；
4. 通过实时影子双算、灰度和可回滚切换证明 Base 可以成为唯一任务真源。

因此，下一目标不应写成笼统的“完成 BQ 替代”。这会把内容迁移、行为兼容、玩家进度和生产切换混成一个
难以验证的大任务。应先完成 M.1，并把 M.2–M.6 保留为后续独立目标。

## 当前能力边界

### 已有稳定基础

- Base Quest Core、PostgreSQL 仓储、Minecraft 事实检测和奖励交付基础；
- UI2 玩家任务中心与管理员编辑入口；
- PLAYER/PARTY/TEAM/PUBLIC 跨服主体及逐成员奖励归属；
- BetterQuesting 拆分 JSON 的只读定义、章节和玩家进度解析；
- 标准 Task/Reward 参数映射、兼容性报告和离线进度差异算法；
- importer 与运行 JAR 隔离，Quest 生产开关默认关闭。

### M.1 已完成的自动化能力

- 确定性定义/章节迁移清单、逐元素兼容矩阵和失败闭合参数校验；
- 零写入 dry-run 与显式审计 preview；
- 隔离 PostgreSQL 的原子提交、幂等重跑、受控升版和整批回滚；
- 迁移审计、UI2 管理端结果摘要及玩家任务中心浏览验证；
- 788 项测试、258 张无结构问题视觉快照和运行 JAR 自包含检查。

这些能力只证明内容迁移闭环，不证明导入后的每一种 Task/Reward 已在 Minecraft 中保持 BQ 行为。

### 明确未完成

- 全量 GTNH Task/Reward 行为兼容证明；
- 多服玩家进度合并、Choice 与重复周期迁移；
- 管理员迁移诊断和受控修复完整能力；
- BetterQuesting/Base 实时影子双算；
- 生产 DDL、灰度运行、正式切换和停用 BetterQuesting。

## 建议设立的下一长期目标

> 完成 BetterQuesting Task/Reward 行为兼容 M.2：只读取用户明确提供的 GTNH `DefaultQuests` 副本，生成真实
> 类型与参数频率清单；按使用频率逐类实现并验证配置解码、Minecraft 服务端事实采集、进度归约、重复周期、
> 自动完成和奖励交付语义；以 BQ 源码和固定样本为判据，建立逐类型兼容矩阵与差异测试。缺少事实入口、交付
> 处理器或无法证明等价的类型必须保持 BLOCKED。不得读取生产目录、导入玩家进度、发放生产奖励、执行生产
> DDL、部署、启用 Quest 生产开关或停用 BetterQuesting；完成隔离自动测试、文档、干净提交并通过 SSH 推送。

## M.1 已满足的判据

- 同一输入重复规划得到相同来源哈希、批次 ID、清单顺序和兼容结论；
- 重复 UUID、缺失前置、前置环、章节悬空/重复放置、非法参数和未知类型全部失败闭合；
- dry-run 不写数据库，阻塞批次不能提交；
- 提交要么整批成功，要么没有任何定义、章节或审计残留；
- 相同批次重跑不增加版本或审计行，内容变化必须形成显式新版本；
- 回滚只撤销该批次引入且未被后续内容依赖的变更；危险回滚必须拒绝并列出原因；
- UI2 管理端能够区分新增、版本变化、相同、阻塞和未知项，并有稳定分页或有界详情；
- 导入后的章节、任务、前置、图标、目标和奖励能在玩家任务中心浏览；
- 最终 Base 运行 JAR 不包含 importer、Gson CLI、BetterQuesting 类、BQ GUI/Packet 或 JSON 存储；
- Quest、UI2、根项目、视觉报告、`assemble`、`verifyUi2SelfContained` 和 `git diff --check` 全部通过；
- 工作树干净且本地 `main` 与 `origin/main` 一致。

## 后续目标顺序

M.1 完成后按以下顺序逐项设立新目标，不并入本轮：

1. **M.2 行为兼容**：按真实任务库使用频率完成 Task/Reward 的事实、归约、周期和交付语义；
2. **M.3 玩家进度迁移**：跨服合并完成、领取、Choice、向量进度和重复周期，防止重复发奖；
3. **M.4 诊断修复**：提供权限受控、可审计、可 dry-run 的诊断与有限修复；
4. **M.5 影子双算**：BQ 继续发奖，Base 只计算并持续输出可解释差异；
5. **M.6 灰度切换**：完成备份、冻结、增量迁移、灰度、回滚窗口和最终停用门禁。

只有 M.2、M.3、M.5 和 M.6 均通过，才能对外宣称 BetterQuesting 已被 Base 正式替代。

## 与其他文档的关系

- 总路线与阶段边界：`betterquesting-content-compatibility-migration-roadmap-2026-09-17.md`；
- 能力完成矩阵：`betterquesting-capability-absorption-matrix-2026-09-13.md`；
- 职业、贡献和公共任务的长期产品设计：
  `galaxy-career-contribution-cross-server-quest-platform-2026-09-08.md`。

本交接单只负责定义下一长期目标与验收边界，不替代上述权威进度文档。
