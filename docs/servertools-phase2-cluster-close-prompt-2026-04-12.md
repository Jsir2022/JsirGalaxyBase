# Server Tools / Cluster 第二阶段严格验收收口 Prompt

日期：2026-04-12

下面这份 prompt 用于处理 `JsirGalaxyBase` 当前 `servertools / cluster` 第二阶段实现经过严格验收后暴露出的阻塞问题。

这不是新功能扩展 prompt，也不是继续推进第三阶段的 prompt。

你的任务是只修这次严格验收已经明确指出的两个阻塞项，把当前第二阶段实现收口到“可接受继续联调”的状态。

---

你正在 `JsirGalaxyBase` 仓库中工作。请直接修改现有 `modules.cluster` 实现，修复这轮严格验收已经确认的问题。

## 开始前必须先读

开始编码前，先完整阅读并遵守下面这些文档：

- `README.md`
- `docs/servertools-phase1-command-reference.md`
- `docs/servertools-phase2-cross-server-gateway-prompt-2026-04-11.md`
- `docs/WORKLOG.md`

同时必须阅读这几个实际实现文件：

- `src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`
- `src/main/java/com/jsirgalaxybase/modules/cluster/application/PlayerArrivalRestoreService.java`
- `src/main/java/com/jsirgalaxybase/modules/cluster/infrastructure/jdbc/JdbcTeleportTicketRepository.java`
- `src/main/java/com/jsirgalaxybase/modules/cluster/domain/GatewayDispatchResult.java`
- `src/main/java/com/jsirgalaxybase/modules/cluster/domain/TransferTicket.java`
- `src/main/java/com/jsirgalaxybase/modules/cluster/domain/TransferTicketStatus.java`
- `src/test/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportServiceTest.java`
- `src/test/java/com/jsirgalaxybase/modules/cluster/application/PlayerArrivalRestoreServiceTest.java`

如果你需要同步核对 schema，再读：

- `docs/servertools-cluster-postgresql-ddl.sql`
- `ops/sql/migrations/20260411_002_expand_cluster_transfer_ticket_lifecycle.sql`

## 本轮目标

本轮只处理下面两个阻塞项：

1. 修复 ticket 过期清理后 `status_message` 仍保留旧内容的问题
2. 修复重复 `requestId` 命中终态 ticket 时仍被错误包装成 `PENDING_REMOTE` 的问题

注意：

- 本轮不是继续扩 cluster 生命周期状态
- 本轮不是继续改 gateway 接线能力
- 本轮不是继续做 servertools 新命令
- 本轮不是 terminal / market / bank 修复

## 已确认的严格验收阻塞问题

### 问题 1：过期清理后 message 语义错误

当前 `JdbcTeleportTicketRepository.expireActiveTickets(...)` 会把超时 ticket 推进到 `EXPIRED`，但 SQL 里对 `status_message` 使用了 `COALESCE(status_message, ...)`。

这会导致：

- ticket 状态虽然变成 `EXPIRED`
- 但如果原来 `status_message` 已经有值，例如：
  - `waiting for target restore`
  - `Target restore retry pending: ...`
- 那么过期后 message 仍会保留旧内容

这会让数据库和日志的可观测语义变成：

- 状态显示 `EXPIRED`
- 但 message 看起来仍像“等待恢复中”或“下次重试中”

严格验收下这是错误的，因为终态必须有终态语义。

本轮要求：

- ticket 一旦被过期清理推进为 `EXPIRED`
- `status_message` 必须同步改成明确的 expired 语义
- 不允许继续保留旧的 pending / retry message

允许的结果示例：

- `Cluster transfer ticket expired before target restore completed`
- 或等价但明确表达“已过期、恢复未完成”的文案

不接受：

- `EXPIRED` + 旧 message 仍是 `waiting` / `retry pending`

### 问题 2：重复 requestId 的终态幂等语义错误

当前 `ClusterTeleportService.dispatchTeleport(...)` 中，只要 `findByRequestId(requestId)` 查到旧 ticket，就会直接：

- `return GatewayDispatchResult.pendingRemote(existing.getStatusMessage(), existing)`

这意味着：

- 旧 ticket 即使已经是 `FAILED`
- 或者已经 `COMPLETED`
- 或者已经 `EXPIRED`
- 仍然会被错误包装成 `PENDING_REMOTE`

这会破坏最小幂等语义，也会误导调用方和日志判断。

严格验收要求：

- 重复 `requestId` 命中已有 ticket 时，返回语义必须与 ticket 当前真实状态一致
- 不能把终态 ticket 一律伪装成 pending

本轮最低要求：

- `PENDING_GATEWAY` / `DISPATCHED` 仍可保持 pending remote 语义
- `FAILED` 必须返回 failed 语义
- `COMPLETED` 不能返回 pending remote
- `EXPIRED` 不能返回 pending remote

如果你需要扩一个最小的结果映射逻辑，可以做；但不要顺手重写整套 dispatch 边界。

### 这两个问题为什么是阻塞项

因为第二阶段 prompt 明确要求：

- ticket 生命周期闭环
- 最小失败恢复
- 最小幂等
- 可排障观测

如果这两个问题不修：

- 运维看数据库终态会被旧 message 误导
- 上层重复请求会把终态错误地看成 pending

这会直接影响继续联调和后续验收。

## 允许修改的范围

本轮原则上只应修改与这两个问题直接相关的文件，例如：

- `src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`
- `src/main/java/com/jsirgalaxybase/modules/cluster/infrastructure/jdbc/JdbcTeleportTicketRepository.java`
- 必要时 `src/main/java/com/jsirgalaxybase/modules/cluster/domain/...`
- 对应 `src/test/java/com/jsirgalaxybase/modules/cluster/...`
- `docs/WORKLOG.md`
- 如确有必要，可最小更新 `docs/servertools-phase1-command-reference.md`

不要碰：

- `modules.servertools` 无关逻辑
- `terminal/...`
- `modules.core.market/...`
- `modules.core.banking/...`
- 任何 GUI、notification、dialog、market、bank 相关实现

## 测试要求

本轮至少补或更新下面这些验证：

1. `JdbcTeleportTicketRepository` 或相关服务测试要覆盖：
   - ticket 过期后，`status_message` 被明确改写为 expired 语义

2. `ClusterTeleportServiceTest` 要覆盖：
   - 重复 `requestId` 命中 `FAILED` ticket 时，不能返回 `PENDING_REMOTE`
   - 重复 `requestId` 命中 `COMPLETED` ticket 时，不能返回 `PENDING_REMOTE`
   - 重复 `requestId` 命中 `EXPIRED` ticket 时，不能返回 `PENDING_REMOTE`

3. 至少运行与本轮直接相关的定向测试，并在最终说明里列出

如果你发现更合适的测试放置位置，可以调整，但不要把测试范围扩到无关模块。

## 实现原则

- 只修根因，不做表面补丁
- 不扩范围
- 不继续新增生命周期阶段
- 不把这轮问题重写成第三阶段规划
- 保持最小必要改动

## 文档要求

本轮至少要做：

- 更新 `docs/WORKLOG.md`

如果命令参考文档里已有 ticket 生命周期描述，且你认为需要补一句“终态不会再被包装为 pending”，可以做最小更新；但不要把文档扩写成新设计稿。

## 最终输出要求

完成后，请明确汇报：

1. 过期 message 是怎么修的
2. 重复 `requestId` 的终态映射语义是怎么修的
3. 改了哪些文件
4. 补了哪些测试
5. 实际运行了哪些定向测试
6. 是否还有明确留给下一轮的问题

## 明确禁止事项

- 不继续改 gateway 设计
- 不扩大到 terminal / market / bank
- 不新增 GUI
- 不重写 cluster 全链路架构
- 不把这轮修复变成新一轮大改造

这轮的验收标准很直接：

- `EXPIRED` 必须有明确 expired message
- 重复 `requestId` 命中终态 ticket 时，返回语义必须和终态一致，不能再伪装成 pending

如果这两点没有同时做到，就不算完成。