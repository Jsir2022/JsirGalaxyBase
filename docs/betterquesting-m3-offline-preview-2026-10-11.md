# M.3 离线进度预览工作包

本轮目标：复用现有定义 manifest、玩家进度解析、向量映射和冲突分析，产出可复查的多来源报告。没有数据库写入器，不发奖励，也不决定重复周期来源。

## 实现边界

- `BqProgressMigrationPreview` 接收固定 manifest 与命名来源，按玩家、Quest、来源稳定排序。
- 报告包含定义 hash/version、完成/领取标志、时间、映射 Task 向量及两两结构关系。`applyAllowed` 永远为 false；没有阻断也只是 `REVIEW_ONLY`。
- 缺定义、非法 manifest、向量形状、负数、重复 Task、领取但未完成、负时间均拒绝或明确阻断。重复玩家/Quest 输入直接失败，不静默覆盖。
- 所有 repeat Quest 标记 `REPEAT_CYCLE_UNRESOLVED`；含 Choice 的 Quest 标记 `CHOICE_SELECTION_UNMAPPED`。完成/领取标志不能代表当前重复周期或历史 Choice 选择，不能借此重发奖励。
- CLI `BqProgressMigrationPreviewMain <DefaultQuests> <来源ID=QuestProgress目录> [...]` 只输出 JSON 到 stdout。建议通过 importer distribution 的 Java classpath 运行该 main；不连接数据库。输入必须是固定离线副本。
- CLI 为每个文件记录 SHA-256，解析前后检测文件变化。不是全目录原子快照，不能用正在写入的服务器目录作为一致性证据。报告含玩家 UUID，应作为私有迁移材料，不提交 Git。
- 映射向量是 Core 解释结果，不替代原文件；原始任务信息须通过对应哈希的副本复查。来源标识不等于权威优先级。

## 阶段

| 工作 | 状态 |
|---|---|
| 多来源离线报告、基本阻断、映射器拒绝静默覆盖 | 已实现，验收结果见 Worklog |
| CLI 文件读取/变化/哈希及完整合成目录端到端故障矩阵 | 待补充专门测试 |
| Choice 历史选择与 repeat 周期决策 | 未完成；禁止自动猜测 |
| 持久化 dry-run 批次、来源授权、受控写入与幂等重跑 | 未完成 |
| 回滚前值、奖励权益隔离、冲突解决及恢复验收 | 未完成 |

本轮新增测试只证明确定排序、结构关系不自动合并、跨玩家隔离、缺定义、repeat/Choice 阻断及负数/重复 Task 拒绝。不能称 M.3 完成，更不能称 BQ 已替代。
