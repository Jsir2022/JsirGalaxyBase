# Server Tools / Cluster 第二阶段第一优先级执行 Prompt

日期：2026-04-11

下面这份 prompt 用于推进 `JsirGalaxyBase` 中 `servertools / cluster` 的第二阶段第一优先级工作。

这不是第一期收口 prompt，也不是继续扩命令数量的 prompt。

你的任务是把“已经具备跨服语义的传送主链”推进成“具备真实跨服闭环能力的传送主链”。

---

你正在 `JsirGalaxyBase` 仓库中工作。请直接修改现有 `modules.cluster` 与 `modules.servertools` 实现，把当前以 `TransferTicket + GatewayAdapter` 为核心的跨服占位链，推进到真实可消费、可恢复、可排障的第二阶段第一优先级状态。

## 开始前必须先读

开始编码前，先完整阅读并遵守下面这些文档：

- `README.md`
- `docs/servertools-phase1-requirements.md`
- `docs/servertools-phase1-command-reference.md`
- `docs/servertools-phase1-execution-prompt-2026-04-11.md`
- `docs/servertools-phase1-acceptance-close-prompt-2026-04-11.md`
- `docs/servertools-cluster-postgresql-ddl.sql`
- `docs/WORKLOG.md`

如果你需要补看群组服边界与真实运行环境，再读：

- `../Docs/技术边界文档.md`
- `../Docs/群组服.md`

## 本轮目标

本轮只做下面三件事：

1. 把 `GatewayAdapter` 从单纯占位实现推进成真实跨服网关适配主链
2. 把 `cluster_transfer_ticket` 从“只记录请求”推进成“源服派发、目标服消费、状态闭环、失败可恢复”的完整票据模型
3. 把目标服落点恢复做成真实运行链，而不是只停留在数据库里有 ticket

换句话说，本轮完成后，`home` / `back` / `tpa` / `warp` 等已有跨服语义的命令，至少在 runtime 结构上不再只是“写 ticket + 打日志”，而是能形成真实的跨服进入与落点恢复闭环。

## 本轮不做什么

下面这些范围本轮不要碰：

- 不新增新命令类别
- 不做 GUI
- 不扩 terminal / market / bank
- 不引入 `ServerUtilities` 的旧网络框架
- 不顺手进入 claims / chunkloading / team / ranks
- 不做“完整后端中心服务重构”
- 不为了接线而推翻当前 `ClusterModule` / `ServerToolsModule` 结构

本轮目标很聚焦：

- 真实网关接线
- ticket 生命周期闭环
- 目标服落点恢复

## 当前已知现状

当前第一期已经具备：

- `ClusterModule` / `ServerToolsModule`
- `ServerDirectory`
- `TeleportTarget`
- `TransferTicket`
- `TeleportTicketRepository`
- `GatewayAdapter`
- `ClusterTeleportService`
- `home` / `back` / `spawn` / `tpa` / `rtp` / `warp` 命令主链

当前问题是：

- 跨服目标会落到 `GatewayAdapter` 占位实现
- ticket 虽然已能持久化状态，但还没有真实的目标服消费闭环
- 玩家跨服后如何在目标服完成最终落点恢复，当前没有真实运行链

因此本轮重点不是继续做命令层，而是把 cluster runtime 做实。

## 必须完成的任务

### 1. 定义并落地真实网关适配边界

你需要把 `GatewayAdapter` 推进到能表达真实跨服派发的状态。

最低要求：

- 不再只是 `LoggingGatewayAdapter` 风格的占位输出
- 要有明确的“如何把 ticket 从源服送到目标服”的适配边界
- 当前实现可以先基于你在本仓库和群组服中的现实条件，做最小可运行适配

允许的实现方向：

- 基于当前群组服已有入口链路的最小代理接线
- 基于数据库 / 共享真源的目标服轮询消费模式
- 基于现有 dedicated server 环境可验证的最小跨服信号交付机制

但必须满足：

- 源服发起后，不只是“记一条 pending 日志”
- 目标服确实能够看到并消费这张 ticket
- 适配层接口与 cluster 应用层边界清晰

不接受：

- 在命令类里直接写跨服转服逻辑
- 把真实网关细节散落到 `PlayerTeleportService`
- 为了图快，重新引入一套不受当前模块体系约束的全局静态网络中心

### 2. 把 transfer ticket 做成完整生命周期闭环

当前 `TransferTicket` 已存在，但本轮要把它推进成真正的运行时契约。

最低要求：

- 明确 ticket 的阶段状态
- 明确源服创建、网关派发、目标服接收、目标服消费成功、失败、超时的状态演进
- 明确相同 request / 玩家重复触发时的幂等策略

建议至少覆盖下面这些语义：

- `PENDING_GATEWAY`
- 已交付目标服但尚未消费
- 目标服已消费 / 已完成落点恢复
- 失败
- 过期 / 超时

如果现有状态枚举不足，可以扩展，但必须保持最小必要，不要做成过度复杂状态机。

实现要求：

- 仓储状态必须能真实表达上述阶段
- ticket 更新必须围绕统一应用服务完成，不要让多个调用点各自乱改状态
- 要考虑 dedicated server 重启后的恢复能力，至少不能因为重启就把进行中的跨服传送全部变成不可解释脏数据

### 3. 实现目标服消费 ticket 与落点恢复

这是本轮最核心的能力。

最低要求：

- 目标服能够检测到“有一张属于某玩家、目标 serverId 就是自己”的待消费 ticket
- 当玩家进入目标服，或目标服检测到玩家已在线时，能够将该 ticket 与玩家匹配
- 匹配成功后，在目标服执行最终落点恢复
- 恢复成功后，ticket 必须更新为完成状态
- 如果落点恢复失败，ticket 必须留有明确失败原因和后续可恢复语义

这里的“进入目标服”与“落点恢复”可以按当前 GTNH / Forge 1.7.10 真实运行环境，采用最小可行方案。

但必须满足：

- 最终传送落点不是源服本地假完成
- 目标服恢复逻辑不是只存在于测试里
- 玩家真实进入目标服后，runtime 能找到并执行该目标位置

### 4. 增加最小可用的失败恢复与清理策略

本轮不用做完整运维后台，但至少要补齐下面这些最小恢复能力：

- ticket 过期判断
- 目标服消费失败时的错误消息落库
- 对同一 requestId 的重复提交保持幂等
- 避免同一个 ticket 被重复消费多次

如果你发现必须新增少量辅助字段或少量 migration，允许做，但必须最小化，并同步更新 DDL / migration 文档。

### 5. 增加排障与观测信息

本轮真实跨服开始接线后，没有观测信息就无法验收。

至少补下面这些内容：

- 源服创建 ticket 时的结构化日志
- 网关派发成功 / 失败日志
- 目标服检测 ticket、匹配玩家、消费 ticket、落点恢复成功 / 失败日志
- 明确 requestId / ticketId / playerUuid / sourceServerId / targetServerId

不需要做 GUI 面板，但必须让 dedicated server 日志足够支撑排障。

## 推荐实现边界

本轮建议优先修改或新增的区域包括：

- `src/main/java/com/jsirgalaxybase/modules/cluster/...`
- 必要时 `src/main/java/com/jsirgalaxybase/modules/servertools/...`
- 必要时 dedicated server 生命周期接线点
- 对应 `src/test/java/com/jsirgalaxybase/modules/cluster/...`
- 对应文档与 migration 文件

如需新增组件，优先考虑：

- `ClusterTicketLifecycleService`
- `RemoteTransferDispatchService`
- `TicketConsumer` / `InboundTransferTicketService`
- `PlayerArrivalRestoreService`

名称可以不同，但边界要清晰：

- 源服派发
- ticket 状态推进
- 目标服消费
- 目标服落点恢复

不要把这些全部塞回一个巨大的 `ClusterTeleportService`。

## 建议验证场景

本轮至少补或运行下面这些验证：

1. 源服创建跨服传送 ticket 后，状态正确推进
2. 目标服能查询并识别属于自己的待消费 ticket
3. 玩家进入目标服后，ticket 能与玩家匹配并恢复落点
4. ticket 成功消费后不会重复消费
5. ticket 过期或失败时，状态和错误消息正确落库
6. 同一 requestId 重试时不产生不可控重复数据

允许你把验证拆成：

- 单元测试
- 服务层测试
- 如果环境允许，再做 dedicated server 联调验证

但最终说明里必须明确哪些验证是真跑过的，哪些只是测试桩。

## 文档要求

如果本轮引入了新的 ticket 状态、字段、恢复语义或运行步骤，请同步更新：

- `docs/servertools-phase1-command-reference.md`
- `docs/servertools-cluster-postgresql-ddl.sql`
- 必要时 migration 文档
- `docs/WORKLOG.md`

如果改动已经明显超出“第一期命令参考”的承载范围，可以新增第二阶段 cluster 运行时说明文档，但不要把它写成泛泛规划稿；要写成和本轮实现直接对应的运维 / 结构说明。

## 最终汇报要求

完成后，请明确汇报：

1. 真实网关适配边界是怎么设计和落地的
2. ticket 生命周期新增或调整了哪些状态
3. 目标服是如何识别并消费 ticket 的
4. 玩家到达目标服后是如何恢复落点的
5. 增加了哪些失败恢复 / 幂等 / 观测能力
6. 改了哪些文件
7. 补了哪些测试，实际跑了哪些验证
8. 还有哪些明确留给下一阶段的内容

## 明确禁止事项

- 不扩大到 terminal / market / bank
- 不顺手加 GUI
- 不把当前模块体系推翻重做
- 不把问题重写成“未来再接后端服务”的空规划
- 不只写文档不落地代码

这轮的验收标准很直接：

如果做完后，跨服传送仍然只是“写 ticket + 返回 pending”，那就不算完成。

如果做完后，源服、ticket、目标服消费、目标服落点恢复已经形成一条真实闭环，那这一轮方向就是对的。