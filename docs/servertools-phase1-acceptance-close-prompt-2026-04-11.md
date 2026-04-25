# Server Tools / Cluster 第一期验收收口 Prompt

日期：2026-04-11

下面这份 prompt 用于处理 `JsirGalaxyBase` 当前第一期 `servertools / cluster` 实现经过严格验收后暴露出的收口问题。

这不是新功能扩展 prompt，也不是第二期规划 prompt。

你的任务是只修这次验收已经明确指出的问题，把第一期实现收口到“可接受交付”的状态。

---

你正在 `JsirGalaxyBase` 仓库中工作。请直接修改现有第一期 `servertools / cluster` 实现，修复本轮验收已经确认的问题。

## 开始前必须先读

开始编码前，先完整阅读并遵守下面这些文档：

- `README.md`
- `docs/servertools-phase1-requirements.md`
- `docs/servertools-phase1-command-reference.md`
- `docs/servertools-phase1-execution-prompt-2026-04-11.md`
- `docs/WORKLOG.md`

如果你需要回看当前 schema 与阶段边界，也可再读：

- `docs/servertools-cluster-postgresql-ddl.sql`
- `ops/sql/migrations/20260411_001_add_servertools_cluster_phase1.sql`

## 本轮目标

只处理下面三类问题：

1. 修复 cluster 远端派发票据状态没有完整持久化的问题
2. 修复 `tpa` 目标服参数未校验、无效 serverId 会被错误写入数据库的问题
3. 收口本轮提交范围，避免继续把无关 terminal / market 改动混进这次 servertools / cluster 修复

注意：

- 本轮不是继续扩展 `home` / `warp` / `tpa` / `rtp` 的新能力
- 本轮不是进入第二期真实跨服网关实现
- 本轮也不是终端、银行或市场 GUI 的修复阶段

## 已确认的验收问题

### 问题 1：cluster_transfer_ticket 状态回写不完整

当前 `ClusterTeleportService` 中，远端派发时会先保存一条 `PENDING_GATEWAY` ticket，然后调用 `GatewayAdapter`。

但现状是：

- 只有 `FAILED` 分支会把 adapter 返回的 ticket/status/message 回写数据库
- 如果 adapter 返回的是 `PENDING_REMOTE`，当前仓储状态不会同步更新
- 这会导致数据库中的 `cluster_transfer_ticket` 与运行时返回值不一致

本轮要求：

- 只要 `GatewayAdapter` 返回了带 ticket 的结果，就要把返回 ticket 的最新状态与消息持久化回仓储
- 不要只处理 `FAILED`
- 若结果不带 ticket，再维持现有最小保守行为
- 修完后，要确保后续真实网关接入时，库内状态能准确反映 adapter 返回状态

注意：

- 当前阶段仍然允许 `GatewayAdapter` 是占位实现
- 本轮不要扩展新的状态机，只需让现有状态持久化行为正确、统一、可预期

### 问题 2：TPA 目标服未校验

当前 `tpa <playerName> [targetServerId]` 会直接把 `targetServerId` 传到服务层，并写入请求记录。

但现状是：

- 服务层没有校验 `targetServerId` 是否存在于 cluster server directory
- 也没有校验该目标服是否启用
- 玩家输入一个拼错的 serverId，也会收到“request recorded”的成功反馈
- 这会把无效目标持久化成有效业务数据

本轮要求：

- 在 `tpa` 创建请求链路里，对 `targetServerId` 做真实校验
- 校验粒度至少包括：
  - serverId 存在
  - server enabled
- 校验失败时，命令必须直接报错，不允许写入 `player_tpa_request`

实现要求：

- 优先复用现有 `modules.cluster` 里的 `ServerDirectory` / runtime，而不是新造一套查询来源
- 不要把 server 校验逻辑散落在命令类里；应尽量保持应用服务层或 module runtime facade 层的边界清晰
- 如果你需要给 `PlayerTeleportService` 增加最小依赖或通过 `ServerToolsModule` 注入校验端口，可以做，但必须保持最小改动

### 问题 3：本轮范围必须收住

上一次执行结果中混入了大量 terminal / market / bank GUI 相关改动。严格验收下，这属于超范围提交。

本轮要求：

- 不修改无关的 terminal / market / bank 代码
- 不新增任何新的终端、市场或银行文档
- 本轮所有代码改动必须只围绕 servertools / cluster 验收问题本身

注意：

- 如果已有无关改动留在工作区，不要回退它们
- 你只需要确保本轮新增修改不再扩大无关范围

## 允许修改的范围

本轮原则上只应触达与这两个问题直接相关的文件，例如：

- `src/main/java/com/jsirgalaxybase/modules/cluster/...`
- `src/main/java/com/jsirgalaxybase/modules/servertools/...`
- 必要时对应的 `src/test/java/com/jsirgalaxybase/modules/cluster/...`
- 必要时对应的 `src/test/java/com/jsirgalaxybase/modules/servertools/...`
- `docs/WORKLOG.md`
- 如果命令参考文档确实需要补一句校验行为说明，可最小更新 `docs/servertools-phase1-command-reference.md`

不要碰：

- `terminal/...`
- `modules.core.market/...`
- `modules.core.banking/...`
- 任何 GUI page builder、sync binder、notification、dialog 相关实现

## 测试要求

至少补或更新下面这些验证：

1. `ClusterTeleportService` 的测试要覆盖：
   - 远端派发返回 pending ticket 时，仓储状态被更新
   - 失败分支仍然保持正确

2. `PlayerTeleportService` 或 `tpa` 相关测试要覆盖：
   - 无效 target serverId 被拒绝
   - disabled target serverId 被拒绝
   - 校验失败时不会写入 `player_tpa_request`

3. 至少运行与本轮直接相关的定向测试，并在最终说明里列出

如果你新增了一个更合适的测试类来承载 server 校验，也可以，但不要把测试范围扩到无关模块。

## 实现原则

- 只修根因，不做表面补丁
- 不改变现有第一期公开边界
- 不引入第二期能力
- 不顺手重构 unrelated code
- 保持现有架构风格与最小必要改动

## 文档要求

本轮至少要做：

- 更新 `docs/WORKLOG.md`

如命令文档行为有变化，可补最小说明，例如：

- `tpa` 目标服参数现在要求是已注册且 enabled 的 serverId

但不要把文档扩写成第二期设计稿。

## 最终输出要求

完成后，请明确汇报：

- 修了哪两个验收问题
- 改了哪些文件
- 新增或修改了哪些测试
- 实际运行了哪些定向测试
- 是否仍有遗留但属于下一阶段的问题

## 明确禁止事项

- 不实现真实跨服网关
- 不新增 GUI
- 不顺手改 terminal / market / bank
- 不扩大 schema 范围
- 不把本轮修复重新写成第二期大改造

如果你发现某个细节还可以继续优化，但不属于上述两个验收问题，请不要扩范围；优先把这轮收口做干净。