# JsirGalaxyBase Work Log

日期：2026-03-29

这份文件用于记录 `JsirGalaxyBase` 的持续开发摘要。
从本次开始，后续每次实际代码变更都应补一条简要 work log。

## 记录规则

- 每次代码变更后，追加一条简要记录
- 每条记录至少包含：
  - 日期
  - 变更主题
  - 影响范围
  - 简要原因
- 如果变更依赖外部制度文档或外部参考源码，应写出引用来源

## 当前关键引用文档

当前开发默认参考下面这些文档：

- `../README.md`
  - 当前项目定位、代码结构和正式架构约束
- `../../Docs/设定.md`
  - 制度目标、职业、市场、贡献度、跨服阶段路线
- `../../Docs/技术边界文档.md`
  - `JsirGalaxyBase` 的责任边界与跨服同步边界
- `../../Docs/做法.md`
  - 群组服、中心数据库、同步方案与后端边界
- `../../Docs/市场经济推进.md`
  - 市场账本、订单、托管库存与一致性要求
- `../../Docs/下次对话议程.md`
  - 当前已定稿制度结论与后续讨论顺序

## 初次对话摘要

### 2026-04-10 - 把 ServerUtilities 的后续整合边界写入现有架构文档

- 主题：把 `Reference/ServerUtilities` 作为参考源码来源时的整合边界正式写回现有架构文档，明确后续服务器工具能力只能按 cluster / server tools 子系统拆解吸收，不能整包并入
- 影响范围：`README.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：银行与市场主链已经基本成型，后续跨服传送、服务器状态、home 与其他服务器工具能力需要进入正式架构讨论阶段；如果不先写清楚整合规则，后面很容易把 `ServerUtilities` 的整套历史结构直接搬进当前仓库，破坏现有模块化单体边界
- 引用来源：`Reference/ServerUtilities/README.md`、`Reference/ServerUtilities/src/main/java/serverutils/ServerUtilities.java`、`Reference/ServerUtilities/src/main/java/serverutils/data/ServerUtilitiesPlayerData.java`、`Reference/ServerUtilities/src/main/java/serverutils/data/ServerUtilitiesUniverseData.java`、`Reference/ServerUtilities/src/main/java/serverutils/net/ServerUtilitiesNetHandler.java`
- 结果：根架构文档现已明确 `modules.cluster` / `modules.servertools` 的推荐落点，以及 `GatewayAdapter`、`ClusterStateRepository`、`PlayerSnapshotRepository`、`ServerRegistry` 等抽象优先策略，并明确禁止把 `ServerUtilities` 的单入口大模组、全局数据宿主和多通道网络注册表原样迁入

### 2026-04-10 - 产出 ServerUtilities 逐项整合映射表

- 主题：把 `Reference/ServerUtilities` 中后续可能需要的能力逐项列成映射表，并标注为可直接复用、需抽取后复用、建议重写或暂不整合
- 影响范围：`docs/serverutilities-integration-mapping.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：仅靠“需要重构”这一层判断还不够，后续第一批要吸收哪些工具能力，必须落实到具体模块与命令级别，否则仍容易在实现阶段回到“整包看起来都能用”的误判
- 引用来源：`Reference/ServerUtilities/src/main/java/serverutils/data`、`Reference/ServerUtilities/src/main/java/serverutils/command`、`Reference/ServerUtilities/src/main/java/serverutils/client`、`Reference/ServerUtilities/src/main/java/serverutils/ranks`、`Reference/ServerUtilities/src/main/java/serverutils/task`
- 结果：新增正式映射文档，已把 `home` / `warp` / `tpa` / `TeleportTracker` / leaderboard 等列入优先评估清单，把 player data / universe data / net handler / ranks 等标为建议重写，并把 claims、chunkloading、backup、watchdog、invsee 等标记为暂不整合

### 2026-04-11 - 记录第一批 server tools / cluster 已确认需求

- 主题：把 `ServerUtilities` 第一批整合范围中已经拍板的命令、跨服、数据库、权限与 GUI 约束正式写入 docs，供后续执行 prompt 直接引用
- 影响范围：`docs/servertools-phase1-requirements.md`、`docs/serverutilities-integration-mapping.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：后续执行 prompt 已不再缺架构边界，而是缺一份稳定、可引用的需求确认单；如果这些要求只留在对话里，后面实现时仍容易回到“先做单服占位”或“顺手加 GUI”的偏航
- 引用来源：本轮对话中已确认的第一期范围；`docs/serverutilities-integration-mapping.md`
- 结果：新增一份正式需求确认文档，明确 `home` / `back` / `spawn` / `tpa` / `rtp` / `warp` 为一期必须实现范围，明确跨服和数据库为硬约束、GUI 暂缓、权限只做玩家语义但要预留职业 / 贡献度 / 声望制度扩展接口

### 2026-04-11 - 产出可直接交付给另一个 AI 的 server tools 第一期执行 Prompt

- 主题：把已确认需求收口成一份可直接交给另一个 AI 执行的正式 prompt，避免后续实现阶段再次回到“继续评估”或“顺手扩范围”的状态
- 影响范围：`docs/servertools-phase1-execution-prompt-2026-04-11.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：第一期范围、跨服能力、数据库硬约束、权限边界和不做 GUI 都已经明确，当前最需要的是一份强约束执行文本，供后续代码搬运、修改与适配直接使用
- 引用来源：`docs/servertools-phase1-requirements.md`、`docs/serverutilities-integration-mapping.md`、`README.md`
- 结果：新增一份正式执行 prompt，已明确本轮必须实现 `home` / `back` / `spawn` / `tpa` / `rtp` / `warp`，必须支持跨服并直接接数据库，不兼容旧命令格式，不引入旧 rank / net / GUI 框架，只允许按当前项目模块化单体结构完成服务、仓储、命令与 cluster 接线

### 2026-04-11 - 落地 server tools / cluster 第一期命令链、仓储链与模块装配

- 主题：在 `JsirGalaxyBase` 中正式新增 `ClusterModule` 与 `ServerToolsModule`，把 `home` / `back` / `spawn` / `tpa` / `rtp` / `warp` 的最小可运行第一期主链直接落到当前仓库
- 影响范围：`src/main/java/com/jsirgalaxybase/module/ModuleContext.java`、`src/main/java/com/jsirgalaxybase/bootstrap/ModBootstrap.java`、`src/main/java/com/jsirgalaxybase/modules/cluster/`、`src/main/java/com/jsirgalaxybase/modules/servertools/`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/servertools/application/PlayerTeleportServiceTest.java`、`ops/sql/migrations/20260411_001_add_servertools_cluster_phase1.sql`、`docs/servertools-cluster-postgresql-ddl.sql`、`docs/servertools-phase1-command-reference.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：第一期范围已经从“评估”转入“直接实现”，而且需求已明确要求必须走数据库真源、模块生命周期装配、cluster/servertools 双模块边界和跨服票据预留，不允许再停留在单服 NBT / 文件占位版
- 引用来源：`README.md`、`docs/serverutilities-integration-mapping.md`、`docs/servertools-phase1-requirements.md`、`Reference/ServerUtilities/src/main/java/serverutils/data/TeleportTracker.java`、`Reference/ServerUtilities/src/main/java/serverutils/data/TeleportLog.java`、`Reference/ServerUtilities/src/main/java/serverutils/command/tp/`
- 结果：当前仓库已新增 cluster server directory / transfer ticket / homes / back / warp / tpa / rtp 记录表与 fail-fast JDBC factory；`home`、`back`、`spawn`、`tpa`、`rtp`、`warp` 顶层命令已注册到 dedicated server 启动链；本服目标会直接传送，跨服目标会通过 `cluster_transfer_ticket` 和 `GatewayAdapter` 进入下一阶段真实代理接线预留；同时补齐了纯服务层与 cluster 分发层单测，以及一份正式命令/表结构说明文档

### 2026-04-11 - 产出 server tools / cluster 第一期验收收口 Prompt

- 主题：把本轮严格验收发现的阻塞项收口成一份新的整改 prompt，要求后续执行只修已确认问题，不再继续扩大第一期提交范围
- 影响范围：`docs/servertools-phase1-acceptance-close-prompt-2026-04-11.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：本轮验收已经确认第一期主链基本成立，但仍存在 transfer ticket 状态未完整持久化、TPA 目标服未校验以及提交范围失控这三类问题；如果不把这些问题单独固化成整改 prompt，后续执行很容易再次把修复做成新一轮扩范围开发
- 引用来源：本轮严格验收结论；`docs/servertools-phase1-execution-prompt-2026-04-11.md`；`docs/servertools-phase1-command-reference.md`
- 结果：新增一份只面向验收收口的执行 prompt，已经明确本轮仅允许修复 cluster ticket 状态回写与 TPA target server 校验，并明确禁止继续改 terminal / market / bank 等无关范围

### 2026-04-11 - 收口第一期验收问题：cluster ticket 状态回写与 TPA 目标服校验

- 主题：只修第一期验收中已经确认的两处根因问题，不扩大 servertools / cluster 之外的提交范围
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`、`src/main/java/com/jsirgalaxybase/modules/servertools/application/PlayerTeleportService.java`、`src/main/java/com/jsirgalaxybase/modules/servertools/ServerToolsModule.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/servertools/application/PlayerTeleportServiceTest.java`、`docs/servertools-phase1-command-reference.md`、`docs/WORKLOG.md`
- 原因：严格验收确认当前远端派发返回 pending ticket 时没有把 adapter 返回状态统一回写数据库，同时 `tpa <playerName> [targetServerId]` 会把未注册或 disabled 的 serverId 错写进业务记录；这两处都属于第一期实现收口缺陷
- 引用来源：`docs/servertools-phase1-acceptance-close-prompt-2026-04-11.md`、`docs/servertools-phase1-command-reference.md`
- 结果：cluster 远端派发现在只要 adapter 返回 ticket 就会统一持久化其最新状态与消息；TPA 创建链现在会复用 cluster server directory 校验目标服是否存在且 enabled，校验失败时不会写入 `player_tpa_request`；同时补齐了对应定向单测，且没有继续触碰 terminal / market / bank 范围

### 2026-04-12 - 产出 cluster 第二阶段严格验收收口 Prompt

- 主题：补出一份只处理 cluster 第二阶段严格验收阻塞项的收口 prompt，避免下一轮执行继续扩散到新功能或无关模块
- 影响范围：`docs/servertools-phase2-cluster-close-prompt-2026-04-12.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：本轮严格验收确认第二阶段主链虽然已接通，但仍有两个阻塞项未收口：其一是 ticket 过期清理会保留旧 `status_message`，导致 `EXPIRED` 状态与消息语义不一致；其二是重复 `requestId` 命中终态 ticket 时，`ClusterTeleportService` 仍会无条件返回 `PENDING_REMOTE`，破坏最小幂等与恢复语义
- 引用来源：`src/main/java/com/jsirgalaxybase/modules/cluster/infrastructure/jdbc/JdbcTeleportTicketRepository.java`、`src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/PlayerArrivalRestoreServiceTest.java`
- 结果：新增一份只面向这两个阻塞问题的收口 prompt，已经明确本轮只修 ticket 过期消息覆盖、重复 requestId 的终态返回语义，并要求补对应定向测试和 WORKLOG，不再扩大到 terminal / market / bank 或继续加 cluster 新能力

### 2026-04-12 - 收口 cluster 第二阶段严格验收阻塞项：过期 message 终态覆盖与重复 requestId 终态映射

- 主题：只修 cluster 第二阶段严格验收已确认的两个阻塞项，不继续扩 gateway、生命周期或其他模块范围
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`、`src/main/java/com/jsirgalaxybase/modules/cluster/infrastructure/jdbc/JdbcTeleportTicketRepository.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/PlayerArrivalRestoreServiceTest.java`、`docs/WORKLOG.md`
- 原因：当前实现里，过期清理仍会因 `COALESCE` 保留旧的 waiting/retry message，且重复 `requestId` 命中 `FAILED / COMPLETED / EXPIRED` ticket 时会被统一伪装成 `PENDING_REMOTE`；这两点直接破坏终态可观测性与最小幂等语义
- 引用来源：`docs/servertools-phase2-cluster-close-prompt-2026-04-12.md`、`src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`、`src/main/java/com/jsirgalaxybase/modules/cluster/infrastructure/jdbc/JdbcTeleportTicketRepository.java`
- 结果：过期清理现在会无条件把 `status_message` 覆盖成明确的 expired 文案；重复 `requestId` 命中已有 ticket 时，会按真实状态返回 pending/completed/failed 语义，不再把终态 ticket 统一伪装成 pending；同时补了对应定向单测

### 2026-04-12 - 产出第三阶段 MCSM 灰度联调准备执行 Prompt

- 主题：把 server tools / cluster 下一阶段工作收口成一份严格执行 prompt，只允许利用 MCSM 下的代理 / 大厅 / S2 组一条不影响在线 S1 的灰度联调链
- 影响范围：`docs/servertools-phase3-mcsm-gray-rollout-prompt-2026-04-12.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前第二阶段代码语义已经通过严格验收，但现网资源评估确认 Lobby 与 S2 仍未部署 `JsirGalaxyBase` 模组和相关配置，现阶段真正需要的不是继续扩代码，而是把代理 / 大厅 / S2 准备到可启动、可观察、可继续联调的灰度环境，同时绝对不触碰正在承载玩家的 S1
- 引用来源：`../GroupServer/Galaxy_GTNH_Entrance/velocity.toml`、`../GroupServer/Galaxy_GTNH_Lobby/server.properties`、`../GroupServer/Galaxy_GTNH284_S2/server.properties`、`../GroupServer/mcsmanager/daemon/data/InstanceConfig/b3e4f9f9aefd4d2a9f57c338c1a0f3b8.json`、`../GroupServer/mcsmanager/daemon/data/InstanceConfig/d1e307c5d40745079df2e398e9d85db2.json`、`../GroupServer/mcsmanager/daemon/data/InstanceConfig/d705aa8c32c649228a84a323e1504f62.json`
- 结果：新增一份第三阶段严格执行 prompt，已经明确本轮只允许审计、构建、部署并通过 MCSM 启动代理 / Lobby / S2 的灰度链，必须保持不动 S1、不得绕开 MCSM 直接管控实服进程，也不得顺手扩大到 terminal / market / bank 或新一轮 cluster 功能扩写

### 2026-04-12 - 产出 terminal bank / market 审查后续修复 Prompt

- 主题：把 terminal bank / market 审查中确认的两个后续风险收口成一份可直接执行的修复 prompt，避免下一轮实现时再次混入无关 GUI 打磨或业务扩写
- 影响范围：`docs/terminal-bank-market-risk-fix-prompt-2026-04-12.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：本轮代码审查已经确认，一处高优风险是 market/custom 动作后没有联动失效银行快照，导致银行页可能展示过期余额与状态；另一处低优风险是 `TerminalHomeGuiFactory` 旧 market 挂载残留分支仍保留 custom panel 错接线，虽然当前不是主链，但后续误启用会直接带回错误弹窗接线
- 引用来源：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSessionController.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`
- 结果：新增一份 terminal bank / market 风险修复 prompt，已经明确本轮优先补齐跨页银行快照失效链，并最小清理旧 market 挂载残留的 custom panel 错接线，不继续扩大到 market / bank 业务语义、terminal 视觉层或更大范围重构

### 2026-04-11 - 产出第二阶段第一优先级 cross-server gateway 执行 Prompt

- 主题：把 servertools / cluster 下一阶段最优先的工作进一步收口成一份可直接交给另一个 AI 执行的 prompt，聚焦真实跨服闭环而不是继续扩命令数量
- 影响范围：`docs/servertools-phase2-cross-server-gateway-prompt-2026-04-11.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：第一期命令链和数据库真源已经成立，下一阶段真正的高优任务不再是继续加 `servertools` 命令名，而是把 `GatewayAdapter + TransferTicket` 从占位链推进到源服派发、目标服消费、落点恢复和失败可排障的真实跨服闭环
- 引用来源：`docs/servertools-phase1-requirements.md`、`docs/servertools-phase1-command-reference.md`、`docs/servertools-phase1-execution-prompt-2026-04-11.md`、`docs/servertools-phase1-acceptance-close-prompt-2026-04-11.md`
- 结果：新增一份第二阶段第一优先级执行 prompt，已经明确本轮只做真实网关适配边界、transfer ticket 生命周期闭环、目标服消费与落点恢复、最小失败恢复与排障观测，不再扩 terminal / market / bank 或 GUI 范围

### 2026-04-11 - 产出 terminal bank / market 严格收口执行 Prompt

- 主题：补出一份只面向 terminal bank / market sync 回归收口的执行 prompt，避免后续实现继续把主链阻塞项、非阻塞残留和 GUI 打磨需求混在一起
- 影响范围：`docs/terminal-bank-market-strict-close-prompt-2026-04-11.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前 terminal bank / market 的严格审阅已经确认，高风险问题集中在文本输入框再次混用 binder 手工 sync 与 `TextFieldWidget.value(...)` auto sync，以及缺少覆盖 terminal open 装配链的回归测试；如果不单独起一份收口 prompt，后续实现容易顺手扩大到旧装配残留清理、page 重构或视觉打磨
- 引用来源：`docs/terminal-gui-regression-chain-2026-04-05.md`、`docs/terminal-gui-continue-current-implementation-prompt-2026-04-06.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`
- 结果：新增一份 terminal bank / market 严格收口 prompt，已经明确本轮只移除 market 与 bank 文本输入框的手工 sync 注册、补 terminal open 装配链测试、更新 WORKLOG，并明确把旧 market page container 残留和更大范围页面装配测试留到下一轮

### 2026-04-05 - 产出市场终端 asset-first 重构评估与执行 Prompt

- 主题：基于现有三市场终端代码，正式评估“先选物品 / 挂牌 / 标的，再进详情页”的 asset-first 重构方向，并产出下一轮可执行 prompt
- 影响范围：`docs/market-terminal-asset-first-refactor-evaluation-2026-04-05.md`、`docs/market-terminal-asset-first-refactor-prompt-2026-04-05.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前终端虽然已经完成 MARKET 总入口拆分，但只有标准商品市场半成型地具备对象浏览与详情节奏；定制商品市场仍主要是说明页，汇率市场仍以手持驱动面板为主，继续直接堆字段会把终端重新推回统一 MARKET 巨石结构
- 引用来源：`docs/market-three-part-architecture.md`、`docs/market-entry-overview.md`、`docs/custom-market-minimal-model.md`、用户提出的 AE 终端 / 售货机式交互需求
- 结果：新增一份正式评估文档，明确标准商品市场适合完整采用物品优先终端、定制商品市场应做成 listing-first GUI、汇率市场应做成标的优先详情页；并同步新增可直接执行的重构 prompt

### 2026-04-05 - 补充终端 GUI 回归链文档中的验收阻塞与修复指令

- 主题：把本轮验收发现的实际阻塞项补写进终端 GUI 回归链统一文档，避免后续执行 AI 只看“已完成”结论而继续把兼容边界修歪
- 影响范围：`docs/terminal-gui-regression-chain-2026-04-05.md`、`docs/WORKLOG.md`
- 原因：市场终端与汇率市场大批实现已经落地，但定向测试显示旧任务书兑换兼容入口对非任务书物品的拒绝语义发生回归；需要把“问题是什么、允许怎么修、禁止怎么修、修后必须验证什么”直接写进 prompt/事故链文档
- 引用来源：`src/test/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangeServiceTest.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangeService.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/ExchangeMarketService.java`
- 结果：在统一事故文档末尾新增“本轮验收阻塞与修复执行 Prompt”段，明确要求保留新 formal quote 链，同时恢复旧兼容入口对非任务书物品的直接拒绝语义，并要求后续执行补 WORKLOG 和定向测试验证

### 2026-04-06 - 补出终端 GUI 持续实装 Prompt，明确不再保留回退方案

- 主题：补出一份新的终端 GUI 执行 prompt，明确后续只允许继续修改当前 GUI 实现，不再引入 fallback GUI 或新旧双轨并存
- 影响范围：`docs/terminal-gui-continue-current-implementation-prompt-2026-04-06.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前终端 GUI 主链已经落地，后续重点应转为继续打磨当前 ModularUI 实装效果；如果此时继续让执行 AI保留可回退 GUI，会把结构重新带回双轨维护与巨石装配
- 引用来源：`docs/terminal-modularui2-gui-refactor-prompt-2026-04-02.md`、`docs/terminal-gui-regression-chain-2026-04-05.md`、`docs/market-terminal-asset-first-refactor-evaluation-2026-04-05.md`
- 结果：新增一份直接面向下一轮 GUI 持续实装的 prompt，明确要求只在当前 GUI 上继续实现原先效果、保留真实银行/市场/汇率主链，并接受后续基于用户截图继续微调

### 2026-04-06 - 在当前单实现终端上继续收口导航壳与业务详情层次

- 主题：继续直接修改当前 ModularUI 终端实现，把总壳、银行页、标准商品市场、定制商品市场和汇率市场进一步收口成更接近正式终端的层次，而不是再引入任何回退 GUI
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalWidgetFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankPageBuilder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`、`docs/WORKLOG.md`
- 原因：当前 GUI 主链已经可用，但首页/导航/详情页仍有明显“工程拼板态”；银行页说明块仍压着真实操作区，标准商品详情页买卖/仓储/订单主次不够清晰，定制商品与汇率页也还缺少更明确的对象详情节奏
- 引用来源：`docs/terminal-gui-continue-current-implementation-prompt-2026-04-06.md`、`docs/terminal-modularui2-gui-refactor-prompt-2026-04-02.md`、`docs/market-terminal-asset-first-refactor-evaluation-2026-04-05.md`
- 结果：新增共享 hero band / summary banner 壳层，用于统一页头与状态带；终端总壳导航改成更明确的当前页/当前终端分区提示；银行页改成状态、表单预览、服务反馈和确认门禁闭环；标准商品页把详情层进一步收口成商品摘要 + 盘口 + 交易动作台 + 订单/仓储闭环；定制商品和汇率页也补出更明确的当前详情提示与执行顺序说明，且全程保留现有真实银行 / 市场 / 汇率业务链

### 2026-04-06 - 修复终端首页路由矩阵导致的 ModularUI 尺寸循环

- 主题：修复 client 打开终端首页时因为路由矩阵尺寸推导互相依赖而触发的 ModularUI 死循环卡死
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：首页“终端路由矩阵”区块把 `expanded()` 的矩阵容器放进了 `coverChildrenHeight()` 的 section 里，父容器需要靠子项算高度，子项又反过来等待父高度，运行时就会刷出大量 `MUI [SIZING][Column]: Can't cover children when all children depend on their parent!`
- 结果：矩阵区改成由表头和固定行高直接撑开，不再把相对扩展高度控件塞进按子项包高的 section，从而收口终端首页打开即卡死的问题

### 2026-04-14 - 把终端 GUI 长期路线正式改为内置 BetterQuesting 风格框架

- 主题：把终端 GUI 的长期技术路线从继续深化 `ModularUI 2`，正式调整为 vendoring BetterQuesting 风格 GUI 框架，并把改造面与实施顺序写入终端实施方案
- 影响范围：`docs/terminal-plan.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：前序调研和真实兼容性实验已经证明，`ModularUI 2` 属于 GTNH 共享运行时依赖，版本漂移会带来 pack 级 ABI 风险；而终端未来要承载银行、市场与后续 server tools 统一应用壳，更适合采用仓库自有的 `GuiScreenCanvas + panel tree + theme registry` 体系
- 引用来源：`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/GuiScreenCanvas.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/IGuiPanel.java`、`Reference/BetterQuesting/src/main/java/betterquesting/client/themes/ThemeRegistry.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`
- 结果：终端实施方案现已明确哪些业务层保持不动、哪些 `ModularUI` 类必须重写、为什么打开链要从服务端直接开 GUI 改成“服务端授权 + 客户端开屏”，以及后续 vendoring、首页壳、银行页、市场页与旧依赖清理的建议实施顺序

### 2026-04-14 - 把 BetterQuesting 风格 GUI 集成方案拆成独立文档

- 主题：将终端 GUI 的 BetterQuesting 风格 framework 集成设计从 `terminal-plan.md` 中拆出，形成单独的实施方案文档，避免终端总方案与 GUI 内核集成细节混在同一份文档里
- 影响范围：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-plan.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：原终端实施方案需要继续承担入口、分阶段目标和总体路线说明；而 BetterQuesting 风格 GUI 集成已经进入到 vendoring 范围、协议重构、包结构和页面装配层级，继续混写会让后续执行文档失焦
- 引用来源：`docs/terminal-plan.md`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/GuiScreenCanvas.java`、`Reference/BetterQuesting/src/main/java/betterquesting/client/themes/ThemeRegistry.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`
- 结果：现在 `terminal-plan.md` 只保留 GUI 路线结论与索引，具体的 BetterQuesting 风格 GUI 集成边界、协议变化、包结构、迁移顺序和验收标准已经转入独立方案文档，后续真正实装时可直接以该文档为主

### 2026-04-14 - 产出 BetterQuesting 风格 UI framework 第一阶段执行 Prompt

- 主题：补出一份只面向 BetterQuesting 风格 GUI framework 第一阶段落地的执行 prompt，明确本轮只做最小 vendoring、theme/resource 骨架和占位 screen，不提前混入 terminal 迁移
- 影响范围：`docs/terminal-betterquesting-ui-phase1-framework-prompt-2026-04-14.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：下一步已经从“继续评估”转入“交给另一个 AI 实做第一阶段 framework 地基”；如果没有一份强约束 prompt，执行时很容易顺手把 terminal 页面迁移、协议重构和旧 `ModularUI` 清理提前混进来，导致范围失控
- 引用来源：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/GuiScreenCanvas.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/IGuiPanel.java`、`Reference/BetterQuesting/src/main/java/betterquesting/client/themes/ThemeRegistry.java`
- 结果：现已新增一份可直接执行的第一阶段 prompt，明确要求只落地仓库自有 GUI framework 基础层、去 BetterQuesting 全局依赖、建立最小主题骨架并跑通占位 screen，同时明确禁止提前迁 terminal 页面和打开协议

### 2026-04-14 - 落地终端 phase 2 新打开链：服务端授权 + 客户端开 TerminalHomeScreen

- 主题：把终端正式入口从“客户端请求后由服务端直接打开 ModularUI”推进到“客户端请求、服务端授权、客户端自行打开 BetterQuesting 风格 TerminalHomeScreen 占位根屏”的最小闭环
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/network/`、`src/main/java/com/jsirgalaxybase/terminal/client/`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/`、`src/test/java/com/jsirgalaxybase/terminal/`、`src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 1 framework 已经可独立运行，但终端主链仍停在 `OpenTerminalMessage -> TerminalService.openTerminal(player) -> TerminalHomeGuiFactory.INSTANCE.open(player)`；第二阶段需要先把 screen 生命周期和授权协议站稳，而不是提前迁银行页和市场页
- 引用来源：`README.md`、`docs/terminal-plan.md`、`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase1-framework-prompt-2026-04-14.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`
- 结果：新增 `OpenTerminalRequestMessage` 与 `OpenTerminalApprovedMessage`，客户端快捷键和背包按钮现已走新请求链；服务端通过 `TerminalService.approveTerminalClientScreen(...)` 生成最小初始化快照与 session token；客户端收到授权后由 `TerminalClientScreenController` 在主线程打开 `TerminalHomeScreen` 占位根屏；`TerminalFrameworkTestScreen` 的 F8 调试入口仍保留文件但不再注册进正式 client bootstrap；旧 `TerminalHomeGuiFactory`、银行页、市场页与 `OpenTerminalMessage` 旧链仍保留为过渡实现，未提前迁移业务

### 2026-04-15 - 把 TerminalHomeScreen 从 phase 2 占位板推进为 phase 3 首页壳

- 主题：只把新终端主链上的 `TerminalHomeScreen` 推进成真正可承载后续业务页的首页壳，并补齐最小 screen model、共用 shell panel、通知宿主与 popup 生命周期，不提前迁银行页和市场页
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalOpenApproval.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalOpenSummaryFormatter.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalOpenSummaryFormatterTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModelTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 2 已经接通“客户端请求、服务端授权、客户端开屏”的新主链，但 `TerminalHomeScreen` 仍只是单块摘要板，后续银行页和市场页还没有可直接挂接的顶部状态带、导航、通知和 popup 宿主层
- 引用来源：`README.md`、`docs/terminal-plan.md`、`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase1-framework-prompt-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase2-open-chain-prompt-2026-04-14.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalWidgetFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalDialogFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalNotification.java`
- 结果：`TerminalHomeScreenModel` 现已扩成包含状态带、导航项、首页 section 和通知入口位的最小首页壳模型；新增 `TerminalPanelFactory`、`TerminalShellPanels`、`TerminalHomeSection` 与 `TerminalPopupFactory` 作为后续业务页可复用的壳层组件；`TerminalHomeScreen` 已重构为顶部状态带 + 左侧导航 + 主内容区 + 通知宿主 + popup 宿主的首页壳；非首页导航点击现在会走新壳级 popup 而不是偷接旧业务页；旧 `TerminalHomeGuiFactory`、银行页、市场页、binder 和 sync state 继续保留为过渡实现

### 2026-04-15 - 收口 phase 3 当前页语义：selectedPageId 成为首页壳单一真源

- 主题：只修 phase 3 严格验收确认的当前页语义分裂问题，把首页壳当前页统一收口到 `selectedPageId`，不提前进入 phase 4 section 宿主切换
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`、`src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModelTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 3 虽然已经显式传 `selectedPageId`，但首页壳实际判断当前页仍主要依赖 `navItems[].selected`；如果后续服务端只切 page id 或 nav flag 不一致，状态带、主体区和导航高亮就可能出现错页
- 引用来源：`docs/terminal-betterquesting-ui-phase3-home-shell-prompt-2026-04-15.md`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`
- 结果：`selectedPageId` 现在是首页壳当前页语义的唯一真源；`NavItemModel.selected` 仍保留，但只作为模型归一化后的派生高亮结果；模型会把子页 id 映射到顶层导航页并重建 nav 选中态，不再信任调用方传入的 selected 标记；本轮没有开始 section 真切换、动作协议或业务页迁移

### 2026-04-15 - 产出终端 phase 3 严格验收收口 Prompt

- 主题：补出一份只面向 terminal phase 3 严格验收收口项的执行 prompt，明确下一轮只修首页壳 `selectedPageId` 与导航选中态仍然分裂的语义缺口，不提前进入 phase 4
- 影响范围：`docs/terminal-betterquesting-ui-phase3-close-prompt-2026-04-15.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 3 首页壳主结构已经落地并通过静态验收，但严格审阅确认首页当前页语义仍同时依赖 `selectedPageId` 与 `navItems[].selected` 两套来源；如果不先收干净，下一阶段 section 宿主切换和真实页面挂载会先踩当前页高亮与主体区错页风险
- 引用来源：`docs/terminal-betterquesting-ui-phase3-home-shell-prompt-2026-04-15.md`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`
- 结果：现已新增一份 phase 3 收口 prompt，范围收口到首页壳当前页语义的单一真源、最小模型与测试修正，以及 README / WORKLOG 同步，不再把 phase 4 的 section 路由、动作协议和业务页迁移混进这轮

### 2026-04-18 - 产出终端 phase 4 section 宿主与最小协议地基 Prompt

- 主题：补出一份只面向 terminal 新首页壳 section 宿主切换与最小 action / snapshot 协议地基的第四阶段执行 prompt，明确这轮先把新壳做成真正宿主，再进入银行页迁移
- 影响范围：`docs/terminal-betterquesting-ui-phase4-section-host-prompt-2026-04-18.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：原高层集成方案虽然把“迁银行页”列为首页壳之后的下一大阶段，但 phase 3 严格验收与收口已经确认，若不先把首页壳推进成真实 section 宿主，并把 action / snapshot 协议补到最小正式落点，后续银行页迁移仍会缺少稳定宿主和刷新边界

### 2026-04-19 - 修复新终端客户端在窄文本区渲染中文标签时的递归换行崩溃

- 主题：修复 `TerminalHomeScreen` 打开后，`LabelPanel` 在极窄文本宽度下调用原版 `FontRenderer.wrapFormattedStringToWidth` 导致的栈溢出崩溃
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/framework/LabelPanel.java`、`docs/WORKLOG.md`
- 原因：phase 6 市场页接入后，部分标签在小屏或紧凑布局下会落入极窄宽度；1.7.10 原版 `FontRenderer` 对中文等宽字符在过窄宽度下可能返回 `sizeStringToWidth == 0`，随后进入无限递归换行
- 引用来源：`run/client/crash-reports/crash-2026-04-19_16.29.23-client.txt`
- 结果：`LabelPanel` 现已改为使用自有安全换行逻辑，在无法安全消费首字符时回退为截断显示，避免终端页再次因窄文本区渲染而直接崩溃
- 引用来源：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase3-home-shell-prompt-2026-04-15.md`、`docs/terminal-betterquesting-ui-phase3-close-prompt-2026-04-15.md`、`docs/README.md`
- 结果：现已新增一份 phase 4 执行 prompt，范围收口到首页壳 section 切换、最小 `TerminalActionMessage` / `TerminalSnapshotMessage` 落点、README / WORKLOG 同步，以及明确把银行页作为下一阶段的第一张完整业务页，而不是在本轮提前迁移

### 2026-04-18 - 落地终端 phase 4 section 宿主、最小 action 协议与 snapshot 回写链

- 主题：把 `TerminalHomeScreen` 从 phase 3 首页壳推进成真实 section 宿主，并补齐最小 `TerminalActionMessage` / `TerminalSnapshotMessage` 往返链，但不提前迁入银行页与市场页业务内容
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalActionType.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalOpenApproval.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/`、`src/main/java/com/jsirgalaxybase/terminal/client/`、`src/main/java/com/jsirgalaxybase/terminal/client/component/`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalSectionRouterTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModelTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 3 收口后虽然已经保证了 `selectedPageId` 是首页壳单一真源，但主体区仍固定渲染 home sections，导航点击也仍停留在 popup 占位，导致后续银行页迁移依然缺少真实 section 宿主和最小刷新闭环
- 引用来源：`docs/terminal-betterquesting-ui-phase4-section-host-prompt-2026-04-18.md`、`docs/terminal-betterquesting-ui-phase3-close-prompt-2026-04-15.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`
- 结果：`TerminalHomeScreenModel` 已改为 page snapshot 结构，`TerminalShellPanels` 主体区现按顶层 section host 渲染当前 page snapshot，导航点击会先切换本地壳再发最小 action 给服务端，服务端通过 `TerminalService.handleClientAction(...)` 返回新的 shell snapshot，客户端可在已打开的 `TerminalHomeScreen` 上原地刷新；本轮仍刻意保留旧 `ModularUI` 银行页与市场页，不做业务迁移

### 2026-04-18 - 补 phase 4 action/snapshot 往返链定向测试收口

- 主题：只补 phase 4 严格验收指出的缺口，为最小 `TerminalActionMessage` / `TerminalSnapshotMessage` 往返链增加直接定向测试，不改主实现边界
- 影响范围：`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`docs/WORKLOG.md`
- 原因：前一轮 phase 4 代码与现有定向测试已经覆盖 section 宿主和 page snapshot 归一化，但还缺一条直接证明“服务端 action 处理结果可回写为 snapshot，并进入客户端刷新入口”的最小测试，严格口径下不算完全收口
- 引用来源：`docs/terminal-betterquesting-ui-phase4-section-host-prompt-2026-04-18.md`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalActionMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/client/TerminalClientScreenController.java`
- 结果：`TerminalServiceTest` 现已补齐 service -> snapshot model round-trip 验证，以及 `TerminalSnapshotMessage.Handler` 向 `TerminalClientScreenController` 排队刷新模型的直接测试；本轮仍未提前迁银行页、市场页或扩写完整业务协议

### 2026-04-18 - 产出终端 phase 5 银行页迁移 Prompt，并压缩剩余阶段目标

- 主题：补出一份只面向银行页迁移的 phase 5 执行 prompt，并把从当前阶段起后续目标压缩为“再往后不超过五个阶段达到删除旧 terminal ModularUI 实现的条件”
- 影响范围：`docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 4 已经把首页壳推进成真实 section 宿主，下一步不该继续停留在抽象层，而应直接让银行页成为新壳上的第一张完整业务页；同时用户明确希望后续节奏加快，尽量在有限阶段内完成 terminal 对旧 ModularUI 的切换与删除准备
- 引用来源：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase4-section-host-prompt-2026-04-18.md`、`docs/banking-terminal-gui-design.md`、`docs/README.md`
- 结果：现已新增一份 phase 5 prompt，范围收口到银行页真实迁移、bank action / snapshot 闭环、确认 popup 迁移和文档同步，并把后续节奏压缩为 phase 6 市场迁移、phase 7 收干 terminal 旧装配残留、phase 8 cutover、phase 9 删除旧 terminal ModularUI 实现

### 2026-04-18 - 落地终端 phase 5 银行完整业务页迁移

- 主题：把 bank 顶层 section 从 phase 4 的宿主占位推进成新 `TerminalHomeScreen` 上的第一张完整业务页，并接通开户、刷新、转账确认 popup 与 snapshot 回写闭环
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalActionType.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalOpenApproval.java`、`src/main/java/com/jsirgalaxybase/terminal/network/`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalBankSectionModelTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalPopupFactoryTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 4 已经具备 section 宿主与最小 action/snapshot 地基，但银行页仍只是说明性 snapshot；下一步必须直接迁入第一张真实业务页，证明新壳可以承接完整终端业务而不是继续搭壳
- 引用来源：`docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md`、`docs/banking-system-requirements.md`、`docs/banking-terminal-gui-design.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSnapshotProvider.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankingService.java`
- 结果：新增 bank 专属 snapshot/view model、表单 payload 与本地输入状态；`TerminalService` 现已能处理 `BANK_REFRESH`、`BANK_OPEN_ACCOUNT`、`BANK_CONFIRM_TRANSFER` 并回写新的 bank snapshot；`TerminalHomeScreen` 现已通过 `TerminalBankSection` 与 `TerminalPopupFactory` 承接新银行页和转账确认 popup，且没有把旧 `TerminalBankPageBuilder`、`TerminalDialogFactory` 或旧 sync binder 重新接回新壳；同时补齐银行模型、action 回写和 popup 生命周期定向测试

### 2026-04-19 - 收口 phase 5 银行页服务端门禁与确认发送链测试

- 主题：只修 phase 5 严格验收指出的两处收口项：服务端银行 action 的 bank page 语义门禁，以及新壳转账确认 popup 到 `TerminalActionMessage` 发送链的直接测试
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalBankActionMessageFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalActionMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalBankActionMessageFactoryTest.java`、`docs/WORKLOG.md`
- 原因：上一轮严格验收确认，银行 action 在服务端仍可绕开 bank page 语义直接执行，同时 popup 测试只证明了新 modal 生命周期存在，还没有直接证明确认按钮继续走新 action/snapshot 主链
- 引用来源：`docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPopupFactory.java`
- 结果：服务端现在只会在当前 page 属于 bank 语义时执行 `BANK_OPEN_ACCOUNT` / `BANK_CONFIRM_TRANSFER`；同时把“转账确认后进入 `BANK_CONFIRM_TRANSFER` 主链”的消息构造收口到独立 helper，并新增对应定向测试，结合既有 `TerminalPopupFactoryTest` 一起证明确认链继续走新 popup + action/snapshot 主链，而不是回退到旧 Dialog 链

### 2026-04-19 - 产出终端 phase 6 市场总览与标准商品市场迁移 Prompt

- 主题：补出一份只面向 MARKET 总入口与标准商品市场迁移的 phase 6 执行 prompt，并把 phase 6 到 phase 9 的职责边界继续收口固定下来，避免下一轮又把 custom / exchange / cutover / 删除旧实现混在一起
- 影响范围：`docs/terminal-betterquesting-ui-phase6-market-overview-standardized-prompt-2026-04-19.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 5 已经把银行页迁成新壳上的第一张完整业务页，下一步必须直接让 MARKET 根页和标准商品市场进入新壳，证明 terminal 已能承接第二类复杂业务页；同时用户已经明确 phase 7 到 phase 9 的顺序，需立即固化到文档中避免后续节奏漂移
- 引用来源：`docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md`、`docs/market-three-part-architecture.md`、`docs/market-entry-overview.md`、`docs/standardized-market-catalog-boundary.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`
- 结果：现已新增一份 phase 6 prompt，明确本轮只迁 MARKET 总入口与标准商品市场真实业务页，要求继续沿用 selectedPageId / action / snapshot 主链、保留 MARKET 根页作为总入口语义、补至少一条真实市场动作回写闭环和一条新 popup 确认链，并明确把定制商品市场、汇率市场、正式 cutover 与旧 terminal ModularUI 删除留给 phase 7 到 phase 9

### 1. 项目定位

- `CustomClient` 不是当前主要 Java 源码仓
- 真正的开发主体是 `JsirGalaxyBase`
- `JsirGalaxyBase` 目标不是普通业务后台，而是：
  - `GTNH 服务器制度核心模组`
  - 并预留后续玩法能力扩展

### 2. 架构总判断

- 不采用传统 Web 框架优先的思路
- 不采用早期扁平模组写法继续扩展
- 当前正式架构定为：
  - `模块化单体`
  - `制度核心 + 能力模块`
  - `服务端权威`
  - `可替换持久化`

### 3. 当前模块边界

- `制度核心模块`
  - 职业
  - 经济
  - 贡献度 / 声望
  - 公共订单 / 公共工程
  - 群组服同步核心状态
- `能力模块`
  - 共享背包
  - 市场终端
  - 其他玩法增强能力
- `诊断模块`
  - 客户端物品导出
  - 开发观测工具

### 5. 本轮代码重构结果

- 删除了旧的示例式写法：
  - `HelloWorldCommand`
  - 旧 `CommonProxy`
  - 旧 `ClientProxy`
  - 旧扁平 `Config`
  - 旧 `client` 包中的导出控制器
- 引入新的启动和模块骨架：
  - `bootstrap/`
  - `module/`
  - `modules/core/`
  - `modules/capability/`
  - `modules/diagnostics/`

## 条目

### 2026-04-19 - 新终端壳接入 MARKET 总入口与标准商品市场

- 主题：把 MARKET 根页和 MARKET_STANDARDIZED 接到 BetterQuesting 风格 `TerminalHomeScreen` 新壳，并补真实标准商品 action / snapshot / popup 闭环
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalActionType.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalMarketActionPayload.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalMarketSectionSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalMarketActionMessageFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalMarketSectionModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionState.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSectionService.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalMarketActionMessageFactoryTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 5 已把银行页迁成新壳上的第一张完整业务页，但 MARKET 根页仍只有占位 section，标准商品市场的真实交易链仍停在旧终端实现，无法满足 phase 6 的范围约束和动作链要求
- 结果：新壳现在会为 MARKET 顶层 page snapshot 携带专用 market section model；MARKET 根页改成共享摘要 + 入口卡，MARKET_STANDARDIZED 改成真实标准商品 section；至少一条真实标准商品动作和一条后处理动作现在通过 `TerminalActionMessage -> TerminalSnapshotMessage` 回写；确认买单与 claim 走新 popup 生命周期；custom / exchange、cutover 与旧 ModularUI 删除仍保持出界

### 2026-04-19 - 产出终端 phase 6 严格验收收口 Prompt

- 主题：补出一份只面向 phase 6 严格验收缺口的收口 prompt，聚焦新壳滚动能力、标准商品市场布局密度和关键数据截断问题
- 影响范围：`docs/terminal-betterquesting-ui-phase6-close-prompt-2026-04-19.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 6 主链已经接通，但严格验收与实际 runClient 目视验证确认 MARKET_STANDARDIZED 仍存在无法滚动、全屏下信息可见面积不足，以及商品 / claim / 规则等内容被固定上限主动裁断的问题；这些缺口会阻塞 phase 7，但又不应把任务扩大成 custom / exchange 迁移
- 结果：现已新增一份 phase 6 close prompt，明确要求只修 framework 层滚动输入与局部滚动宿主、TerminalHomeScreen 与 TerminalMarketSection 的空间策略，以及标准商品市场 section 的关键数据完整显示，并要求保住 phase 6 已成立的 action / snapshot / popup 主链和本地 client/server 联调验证

### 2026-04-19 - 收紧新终端壳缩放与 phase 6 市场页布局密度

- 主题：在 phase 6 收口已恢复滚动与完整数据浏览后，继续把新 terminal 壳的占屏比例和市场页内部 chrome 密度收紧，修复实机目视下仍然“整体过度放大”的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`docs/WORKLOG.md`
- 原因：上一轮主要通过放宽 shell 尺寸上限和引入局部滚动来解决“看不全、滚不动”，但实际进游戏目视后，新壳仍然过于贴满 GUI 视口，状态带、导航项、页头和市场卡片的间距也偏松，导致整体观感依然像被放大过头
- 结果：终端壳现在改为按视口比例取值而不是几乎贴满屏幕；状态带、导航项、页头、footer 与市场 section 的顶部留白、左右分栏比例和买单卡高度也同步压紧，在不回退滚动与长列表能力的前提下提高信息密度并减轻“过度放大”观感

### 2026-04-19 - 产出终端 phase 7 定制商品市场 / 汇率市场迁移 Prompt

- 主题：补出一份只面向 phase 7 的执行 prompt，把剩余 terminal 真实业务页全部迁进新壳，并把新壳对旧 terminal 市场装配的直接依赖残留收干
- 影响范围：`docs/terminal-betterquesting-ui-phase7-custom-exchange-residue-prompt-2026-04-19.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 6 与其收口轮已经把 standardized market 和新壳滚动/布局问题收住，下一步必须明确把 MARKET_CUSTOM、MARKET_EXCHANGE 和旧 market 装配残留一起放进同一阶段完成，避免 phase 8 cutover 前还残留“业务页未迁完”的模糊状态
- 结果：现已新增一份 phase 7 prompt，明确本轮只迁 custom market 与 exchange market 真实业务页、接通各自 action / snapshot / popup 主链，并收干新壳对旧 market builder / binder / dialog / session controller 的直接依赖；同时继续把正式 cutover 与旧 ModularUI 删除留给 phase 8 和 phase 9

### 2026-04-19 - 收口 phase 6 市场页滚动、布局和数据截断缺口

- 主题：在不进入 phase 7 的前提下，把新 terminal framework 与标准商品市场收口到可滚动、可完整浏览、可继续进入下一阶段的状态
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/framework/CanvasScreen.java`、`src/main/java/com/jsirgalaxybase/client/gui/framework/GuiPanel.java`、`src/main/java/com/jsirgalaxybase/client/gui/framework/PanelContainer.java`、`src/main/java/com/jsirgalaxybase/client/gui/framework/VerticalScrollPanel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContent.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/test/java/com/jsirgalaxybase/client/gui/framework/VerticalScrollPanelTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContentTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 6 运行态验收已经确认新壳没有滚轮输入通路、标准市场正文区尺寸偏紧、商品 / claim / 规则 / 盘口等关键数据仍被固定上限主动截断；如果不先收掉这些可用性问题，phase 7 就会建立在不可完整浏览的页面上
- 结果：framework 现在具备最小可复用滚动能力，滚轮事件会经 `CanvasScreen -> PanelContainer -> VerticalScrollPanel` 分发；`TerminalHomeScreen` 放宽了壳层尺寸上限并扩大正文区；`TerminalMarketSection` 改为大屏优先吃满、局部滚动承接小屏溢出，同时把商品浏览、盘口 / 我的订单、claim 和规则条目改成完整 child 生成，不再靠固定前 N 条硬裁；本轮仍保持 MARKET 根页只做总入口，MARKET_CUSTOM / MARKET_EXCHANGE、cutover 与旧 ModularUI 删除继续明确留给 phase 7 之后

### 2026-04-05 - 恢复终端内容区滚动能力

- 主题：恢复终端导航列与正文列的局部滚动能力，同时保留已经验证有效的单页宿主切页结构
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：上一轮为收口点击与布局问题，先移除了局部滚动控件，结果正文区虽然可正常切页，但较长内容不再能滚动，等于把原本功能一起删掉了
- 结果：把导航列和正文列的 `createScrollableBody(...)` 恢复为真正的 `ListWidget` 局部滚动容器，但不恢复“所有页面同时挂载”的旧正文结构；这样保留单页宿主修复成果的同时，拿回原本的滚动能力

### 2026-04-05 - 补回银行与市场输入框的同步注册

- 主题：补回银行转账表单和市场交易表单在终端面板里的输入同步注册，恢复原本可正常输入的行为
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`docs/WORKLOG.md`
- 原因：客户端崩溃日志明确指向 `Sync handler is not yet initialised!`，说明文本框绑定的 `StringSyncValue` 被创建了，但没有注册进 `PanelSyncManager`；打字时触发 `onTextChanged` 就会直接崩溃
- 结果：已补回银行转账的收款人 / 金额 / 备注字段，以及市场限价 / 即时交易数量字段的 `syncManager.syncValue(...)` 注册；这样恢复原有文本输入与服务端同步能力，而不是继续靠删功能规避问题

### 2026-04-04 - 修复终端主界面打开后整体不可点击

- 主题：移除终端主界面导航列与正文列对 `ListWidget` 的顶层包裹，改回普通布局容器，收口打开终端后整页无法点击的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：最新 client log 在终端打开瞬间连续出现大量 `MUI [SIZING][Column]: Can't cover children when all children depend on their parent!`，而终端主页唯一的 `ListWidget` 同时包住导航和正文，最符合尺寸循环与点击事件被滚动容器吞掉的现象
- 结果：终端主界面改为直接使用普通 `Flow.column()` 承载导航与正文内容，避免顶层滚动容器参与尺寸推导和输入捕获；后续点击验证以最新构建实装结果为准

### 2026-04-04 - 汇率市场 GUI 回归测试最后收口

- 主题：只补 exchange 子页正式 quote / 空状态 / 控制器门禁的回归测试，不再扩实现范围
- 影响范围：`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeQuoteViewTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSessionControllerTest.java`、`docs/WORKLOG.md`
- 原因：上一轮已经把 exchange 子页 GUI 主链落地，但关键事实仍主要依赖人工阅读，需要把正式字段映射、空状态和执行门禁锁成稳定回归
- 结果：新增 exchange 页空状态回归、TerminalMarketService 的 exchange 空状态映射回归，以及 TerminalMarketSessionController 的确认兑换门禁与本地错误反馈回归

### 2026-04-04 - 汇率市场子入口第一轮正式 GUI 实装

- 主题：把汇率市场子页从说明页改成真实可操作的终端页，支持正式 quote 预览、确认弹窗和 GUI 内兑换执行
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeQuoteView.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncState.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSessionController.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSessionController.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/ExchangeMarketService.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeQuoteViewTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/ExchangeMarketServiceTest.java`、`docs/market-entry-overview.md`、`docs/WORKLOG.md`
- 原因：汇率市场子入口已经完成路由拆分，但终端页仍停留在静态说明状态，玩家必须退回命令行才能看到正式 quote 字段和完成兑换，不符合汇率市场子入口的正式 GUI 实装目标
- 引用来源：`docs/market-total-entry-split-tail-close-prompt-2026-04-04.md`、`docs/market-entry-overview.md`
- 结果：汇率市场子页现在会读取当前手持物品并走正式 quote 路径，展示 pairCode / assetCode / ruleVersion / limitStatus / reasonCode / notes 等字段；支持“刷新报价”和“确认兑换”两步动作；确认后通过终端弹窗走服务端兑换，并刷新银行摘要与终端通知

### 2026-04-04 - MARKET 总入口正式拆成三类市场入口

- 主题：把终端与命令层里仍然像“统一 MARKET 大桶”的入口残留，正式拆成标准商品市场、定制商品市场、汇率市场三类入口
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalPageTest.java`、`docs/market-entry-overview.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：三市场边界已经定稿，但 MARKET 根页仍直接落到标准商品交易详情页，命令帮助也仍停留在“统一 market 兼容入口”语义，继续放任会让后续 GUI 和命令补丁再次回到混合大市场
- 引用来源：`docs/market-total-entry-split-prompt-2026-04-04.md`、`docs/market-three-part-architecture.md`、`docs/custom-market-minimal-model.md`
- 结果：MARKET 首页现在只负责总览和三类市场入口；标准商品交易页被下沉到独立子页；定制商品市场和汇率市场有了各自终端落点页；`/jsirgalaxybase market` 帮助明确按三类市场入口分组，并补了一条终端路由层单测

### 2026-04-04 - 产出 MARKET 总入口拆分阶段收口 Prompt

- 主题：为 MARKET 总入口拆分阶段补出最后一轮收口 prompt，聚焦真实路由回归测试和 docs 索引补齐
- 影响范围：`docs/market-total-entry-split-tail-close-prompt-2026-04-04.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：阶段主体已经成立，但当前所谓“终端路由回归测试”仍主要停留在 `TerminalPage` 枚举元数据层，且 `market-entry-overview.md` 尚未正式挂入 docs 索引，还不适合直接无保留关阶段
- 引用来源：`docs/market-total-entry-split-prompt-2026-04-04.md`、`docs/market-entry-overview.md`
- 结果：新增一份只针对最后两处真实缺口的收口 prompt，要求下一轮只补 MARKET 根页与三个子市场页的真实装配回归测试，并同步补齐 README 索引与 WORKLOG 记录

### 2026-04-04 - 产出 MARKET 总入口拆分阶段 Prompt

- 主题：为三市场执行顺序中的最后一步补出 MARKET 总入口拆分 prompt，要求入口层正式按标准商品市场、定制商品市场、汇率市场三类入口收口
- 影响范围：`docs/market-total-entry-split-prompt-2026-04-04.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：汇率市场规则层、标准商品市场目录边界、定制商品市场最小挂牌链都已经完成验收；如果此时还不拆 MARKET 总入口，后续终端和命令层仍会继续沿“统一 MARKET 大桶”漂移
- 引用来源：`docs/market-three-part-architecture.md`、`../../Docs/下次对话议程.md`、`../../Docs/市场经济推进.md`
- 结果：新增一份只针对 MARKET 总入口拆分的正式 prompt，明确这一轮只处理入口、路由、文案和命令帮助分组，不继续扩三类市场各自的业务语义

### 2026-04-04 - 修复终端 GUI 转账表单未同步到服务端

- 主题：修复银行终端 GUI 在点击确认后只显示“已提交”但服务端实际拿到空表单，导致转账并未真正执行的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`docs/WORKLOG.md`
- 原因：终端表单字段虽然绑定了 `StringSyncValue`，但没有通过 `syncManager.syncValue(...)` 注册到 panel sync manager；因此 synced action 在服务端执行时读到的仍是默认空值，命令行链路不受影响所以表现正常
- 结果：银行转账的收款玩家、金额、备注现在会随面板同步到服务端；同时把市场页的限价/即时交易输入一起补上同步注册，避免同类“GUI 看似提交、服务端实际空参数”的问题再次出现

### 2026-04-14 - 落地终端 BetterQuesting 风格 GUI framework 第一阶段地基

- 主题：在不迁移现有 terminal 主链的前提下，新增仓库自有命名空间下的最小 BetterQuesting 风格 GUI framework、theme registry 与占位测试屏
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/framework/`、`src/main/java/com/jsirgalaxybase/client/gui/theme/`、`src/main/java/com/jsirgalaxybase/terminal/client/`、`src/main/resources/assets/jsirgalaxybase/textures/gui/framework/`、`src/main/java/com/jsirgalaxybase/terminal/TerminalClientBootstrap.java`、`docs/WORKLOG.md`
- 原因：终端长期 GUI 路线已经明确转向仓库内置的 BetterQuesting 风格 `GuiScreenCanvas + panel tree + theme registry` 体系；本轮需要先把 framework 地基、最小主题骨架和独立可运行 screen 跑通，而不是提前混入 terminal 页面迁移和协议重构
- 引用来源：`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/GuiScreenCanvas.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/IScene.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/IGuiPanel.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/CanvasTextured.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/controls/PanelButton.java`、`Reference/BetterQuesting/src/main/java/betterquesting/client/themes/ThemeRegistry.java`
- 结果：当前仓库已新增自有的 root screen / scene / panel container / textured canvas / button / popup / theme registry 最小集合，并补出 `TerminalFrameworkTestScreen` 与 client-only 调试热键用于验证根屏、panel 绘制、按钮回调、popup 开关和主题资源访问；现有 `TerminalService.openTerminal(...)`、`TerminalModule` 的旧 `ModularUI` factory 主链与网络协议均保持不动

### 2026-04-14 - 产出 BetterQuesting 风格 UI framework 第二阶段打开链执行 Prompt

- 主题：补出一份只面向终端打开链改造的第二阶段执行 prompt，明确本轮只把 terminal 从“服务端直接开 `ModularUI`”推进到“服务端授权 + 客户端开自有 screen”的最小闭环
- 影响范围：`docs/terminal-betterquesting-ui-phase2-open-chain-prompt-2026-04-14.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：第一阶段 framework 地基已经落下，下一步最关键的不是提前迁银行页和市场页，而是先把新 framework 真正接进 terminal 主链；同时用户已明确当前阶段只按静态验证口径推进，不要求启动游戏做运行态验收
- 引用来源：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase1-framework-prompt-2026-04-14.md`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalMessage.java`、`src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java`
- 结果：现已新增一份 phase 2 prompt，范围收口到新终端打开协议、最小 `TerminalHomeScreen` 占位壳、初始化 snapshot / session model、F8 调试入口收口以及静态编译验证，为后续首页壳和业务页迁移提供正确宿主

### 2026-04-15 - 产出 BetterQuesting 风格 UI framework 第三阶段首页壳执行 Prompt

- 主题：补出一份只面向新 terminal 首页壳和共用组件层的第三阶段执行 prompt，明确本轮只推进 `TerminalHomeScreen`、导航壳、通知宿主、popup 宿主与共用 panel 组件，不提前迁业务页
- 影响范围：`docs/terminal-betterquesting-ui-phase3-home-shell-prompt-2026-04-15.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 2 已经把 terminal 打开链切到“客户端请求、服务端授权、客户端开 `TerminalHomeScreen` 占位根屏”，下一步最关键的不是抢先迁银行页和市场页，而是先把新首页壳做成后续所有业务页的真正宿主
- 引用来源：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase2-open-chain-prompt-2026-04-14.md`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/TerminalClientScreenController.java`
- 结果：现已新增一份 phase 3 prompt，范围收口到首页壳结构、顶栏 / 左侧导航 / 主体区、全局通知与 popup 挂载位、共用 shell / panel 组件抽取以及静态编译验证，为后续银行页与市场页迁移提供稳定壳层

### 2026-04-05 - 收口终端表单双端同步缺口并沉淀排障文档

- 主题：把终端里所有“客户端输入或选择后，再由服务端 synced action 消费”的字段统一改成双端同步注册，而不是只做本地 getter/setter 绑定
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`docs/terminal-sync-form-regression-2026-04-05.md`、`docs/WORKLOG.md`
- 原因：上一步虽然补回了 `syncManager.syncValue(...)` 注册，解决了输入时崩溃，但银行转账实测仍出现“GUI 提交后服务端不处理”；继续排查后确认根因是可编辑字段需要区分客户端缓存和服务端真实会话值，否则服务端动作仍可能读到空值或旧值
- 结果：银行转账表单、市场商品选择、限价/即时交易输入、取消订单与领取托管等动作参数都改成客户端缓存 + 服务端 getter/setter 的双端同步模式，并补一份单独 bug 文档供后续排障复用

### 2026-04-05 - 恢复双端同步后的终端按钮门禁

- 主题：修复银行转账按钮与市场确认按钮在双端同步改造后长期被客户端误判为不可用的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankPageBuilder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`、`docs/WORKLOG.md`
- 原因：可编辑字段切到客户端缓存 + 服务端 getter/setter 之后，页面按钮的 enabled 判定如果仍读取本地 sessionController，就会一直看到旧空值，表现成“按钮消失”或始终灰掉
- 结果：银行转账、市场限价/即时单、撤单/提取、存入以及兑换确认等入口现在统一基于当前 sync 值判定；同时补齐盘口价格预填时对 sync 文本框的直接回写，避免按钮和输入状态再次脱节

### 2026-04-05 - 市场终端从 page-first 收口为 asset-first 导航壳

- 主题：把市场终端正式切成标准商品、定制商品、汇率三条独立状态链，并把三类子页都改成先找对象再进详情的节奏
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalCustomMarket*.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeMarket*.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalCustomMarketSessionControllerTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeMarketSessionControllerTest.java`、`docs/WORKLOG.md`
- 原因：此前终端虽然已经完成三市场路由拆分，但运行时状态仍然集中在统一 market controller/snapshot 里，custom 仍停留在说明页，exchange 仍更像手持说明板，不符合 asset-first 终端目标
- 结果：标准商品市场默认停留在商品浏览层，不再自动选中首个商品；定制商品市场新增 listing-first 的浏览范围、详情与 buy/cancel/claim 确认链；汇率市场新增明确的兑换标的入口与详情层；终端总装配也改成标准商品 / 定制商品 / 汇率三套独立 controller + sync binder，并补了对应回归测试

### 2026-04-05 - 修复银行失败反馈不出泡泡与 DevB 开户抢跑崩溃

- 主题：修正银行终端在 GUI 打开时 HUD 泡泡被全局屏蔽的问题，并去掉开户按钮对未初始化 `InteractionSyncHandler` 的依赖
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalWidgetFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankPageBuilder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalHudOverlayHandler.java`、`docs/WORKLOG.md`
- 原因：一方面 HUD overlay 之前在任何 `currentScreen != null` 的情况下都会停止渲染，所以终端内触发的银行失败反馈虽然进入了通知队列，却不会冒泡显示；另一方面开户按钮直接绑定 `InteractionSyncHandler`，在 ModularUI sync manager 尚未完成初始化时被 DevB 抢先点击，会触发 `Sync handler is not yet initialised!`
- 结果：银行开户改成普通本地按钮 + synced action 提交，不再走会抢跑的 `InteractionSyncHandler`；银行转账也改成只展示服务端真实反馈；终端所用的 ModularUI 屏幕打开时 HUD 泡泡现在仍可渲染，转账失败和开户结果都能在 GUI 期间直接看到

### 2026-04-05 - 统一终端 GUI 回归事故文档

- 主题：把终端银行 GUI、转账、sync、按钮门禁、失败提示与开户点击崩溃这整套连续回归，从多份零散事故文档统一收口到单一文档
- 影响范围：`docs/terminal-gui-regression-chain-2026-04-05.md`、`docs/terminal-g-key-fatal-sync-incident-2026-04-04.md`、`docs/terminal-sync-form-regression-2026-04-05.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：之前事故记录已经拆成“G 键 fatal 断线”和“表单双端同步回归”两份文档，再叠加 WORKLOG 里的按钮门禁与 HUD 泡泡问题，后续排障需要来回跳文档，信息已经碎片化
- 结果：新增一份统一事故文档，按整条回归链重新整理银行 GUI、转账、按钮、开户与失败反馈问题；原先两份事故文档改成并入说明；docs 索引同步指向新的统一入口

### 2026-04-04 - 定制商品市场最小挂牌链 v1 最后一轮收口

- 主题：补齐定制商品市场最小挂牌链 v1 的完结动作、单件边界和市场 JDBC 列级 fail-fast 校验
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/custom-market-minimal-model.md`、`docs/custom-market-minimal-listing-chain-prompt-2026-04-03.md`、`docs/market-postgresql-ddl.sql`、`ops/sql/migrations/20260404_002_align_custom_market_single_item_claim_completion.sql`、`docs/WORKLOG.md`
- 原因：上一轮实现已经落地最小挂牌、浏览、购买、下架与 pending 主链，但仍存在 `COMPLETED` 不可达、代码仍允许堆叠挂牌、市场 JDBC 只校验表不校验列这 3 个真实收口缺口
- 引用来源：`docs/custom-market-minimal-listing-chain-tail-close-prompt-2026-04-04.md`、`docs/custom-market-minimal-model.md`、`ops/sql/migrations/20260404_001_add_custom_market_minimal_listing_chain.sql`
- 结果：新增买家 `claim` 完结动作，把交付状态闭环到 `COMPLETED` 并清空双方 pending；发布与快照统一收紧为单件挂牌；市场 JDBC 现在和银行一样对缺列 schema 直接 fail-fast，并提示运维运行 `scripts/db-migrate.sh`

### 2026-04-04 - 验收补出定制商品市场最小挂牌链收口 Prompt

- 主题：在定制商品市场最小挂牌链 v1 验收后，补出只针对剩余闭环缺口的收口 prompt
- 影响范围：`docs/custom-market-minimal-listing-chain-tail-close-prompt-2026-04-04.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前实现已经具备挂牌、浏览、购买、下架与 pending 主链，但验收发现仍存在交付状态机未闭环、单件商品边界与实现不一致、市场 JDBC 缺少 fail-fast 列级 schema 校验这 3 个真实收口缺口
- 引用来源：`docs/custom-market-minimal-listing-chain-prompt-2026-04-03.md`、`docs/custom-market-minimal-model.md`
- 结果：新增一份窄范围收口 prompt，要求下一轮只补 pending 到 completed 的完结动作、统一单件商品边界、以及市场 JDBC 的 fail-fast schema 校验，不再继续扩大功能面

### 2026-04-04 - 定制商品市场最小挂牌链 v1 正式落地

- 主题：为定制商品市场补上独立于标准商品订单簿的最小挂牌、快照、购买、下架与 pending 审计主链
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/market-postgresql-ddl.sql`、`docs/custom-market-minimal-model.md`、`ops/sql/migrations/20260404_001_add_custom_market_minimal_listing_chain.sql`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前仓库已经明确三市场分工，但定制商品市场此前没有正式实现；如果继续把非标商品硬塞进标准商品订单簿，会再次模糊标准商品市场与定制商品市场的边界
- 引用来源：`docs/custom-market-minimal-listing-chain-prompt-2026-04-03.md`、`docs/market-three-part-architecture.md`、`../../Docs/市场经济推进.md`、`../../Docs/下次对话议程.md`
- 结果：新增 `CustomMarketListing / ItemSnapshot / TradeRecord / AuditLog` 和对应 JDBC 仓储、DDL、migration、应用服务与兼容命令入口；发布时保存手持物快照，购买后进入定制商品市场自己的 `BUYER_PENDING_CLAIM` 语义，并可通过 `market custom pending` 查看卖家/买家侧待完结记录

### 2026-04-03 - 银行 PostgreSQL 改为显式版本化迁移与 fail-fast schema 校验

- 主题：把银行数据库结构变更从“应用启动时隐式补列”调整为“运维显式执行版本化 migration，应用启动只做 fail-fast 校验”
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankingInfrastructureFactory.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`scripts/db-migrate.sh`、`ops/sql/migrations/20260403_001_align_banking_ledger_entry_frozen_balances.sql`、`docs/postgresql-schema-migrations.md`、`docs/postgresql-local-setup-and-migration.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：数据库结构漂移不应由应用在启动时静默修复；更稳妥的做法是使用可审计的版本化 migration，在停服窗口由运维显式执行，应用只负责在发现旧 schema 时拒绝启动并给出升级入口
- 结果：新增 `scripts/db-migrate.sh` 和版本化 migration 目录/记录表，本地开发库已通过正式 migration 入口完成修复；银行 JDBC 初始化现在会在 schema 过旧时直接提示运维运行 migration，相关 PostgreSQL 集成测试也已改成校验 fail-fast 行为

### 2026-04-03 - 银行 JDBC 基础设施补上旧版 ledger_entry 列自动修复与列级校验

- 主题：修复本地银行转账在写入 `ledger_entry` 时因旧 PostgreSQL 表结构缺列而失败的问题，并把 schema 校验前移到基础设施初始化阶段
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankingInfrastructureFactory.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`docs/WORKLOG.md`
- 原因：旧开发库的 `ledger_entry` 缺少 `frozen_balance_before` / `frozen_balance_after`，但启动阶段此前只验证“表存在”，没有验证“列齐全”，导致问题直到实际转账写库时才暴露
- 结果：银行 JDBC 基础设施初始化现在会先对旧版 `ledger_entry` 自动补齐冻结余额列，再对核心表执行列级校验；新增 PostgreSQL 集成测试，验证删掉这两列后初始化仍能自动修复并正常启动

### 2026-04-03 - 本地联调脚本改为先起 client 再进 server 控制台

- 主题：调整本地三进程联调脚本的启动顺序和前台行为，方便在服务端控制台直接给在线测试号发放初始资金和做转账联调
- 影响范围：`scripts/start-local-test-stack.sh`、`docs/WORKLOG.md`
- 原因：当前银行管理员命令和玩家间转账都依赖在线玩家身份解析；先让 `DevA` / `DevB` 等测试号进入客户端，再把当前终端切到前台 `runServer` 控制台，才能边看服务端日志边直接执行管理员资金操作
- 结果：脚本现在会先后台启动两个 client，再以前台方式启动 `runServer` 并把当前终端附着到服务端控制台；脚本退出时仍会清理它拉起的 client 进程

### 2026-04-03 - 编译并实装最新 JsirGalaxyBase 到实际 client/server mods 目录

- 主题：将当前标准商品市场目录边界收口后的最新构建产物编译完成，并直接实装到真实 client/server 的 mods 目录
- 影响范围：`docs/WORKLOG.md`
- 原因：本轮代码与测试已经完成，但本地 `runServer` 开发运行时在 FML 扫描 `org.antlr:antlr4:4.13.2` 时出现 class 读取异常，无法把“已编译产物可用”继续建立在 dev runtime 成功拉起上，因此改走真实 mods 目录实装路径
- 结果：
  - 使用 `assemble` 成功产出最新正式 jar：`build/libs/jsirgalaxybase-7545ce9-main+7545ce9201-dirty.jar`
  - 已将同一构建实装到客户端 `CustomClient/Galaxy GTNH 2.8.4/instances/Galaxy GTNH 284/.minecraft/mods/` 与服务端 `GroupServer/Galaxy_GTNH284_S1/mods/`
  - 双端落地文件大小一致，均为 `516806` bytes，可确认当前 client/server 已使用同一份 JsirGalaxyBase 构建产物等待后续启动生效

### 2026-04-03 - 标准商品市场目录边界改为单一运行时事实来源

- 主题：把标准商品市场目录 decision 从“命令层、终端层、服务层各自 new 默认目录”收口为同一运行时事实来源
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketServiceTest.java`、`docs/standardized-market-catalog-boundary.md`、`docs/WORKLOG.md`
- 原因：上一阶段虽然已经建立正式目录对象和统一语义，但命令层与终端层仍各自持有默认目录实例，未来一旦运行时目录来源或版本变化，仍可能与真实服务边界分叉
- 引用来源：`docs/standardized-market-catalog-boundary-tail-close-prompt-2026-04-03.md`、`docs/standardized-market-catalog-boundary.md`
- 结果：
  - `InstitutionCoreModule` 现在显式暴露运行时目录检查入口，命令层统一经由运行时 spot market service 取目录 decision
  - `TerminalMarketService` 的商品浏览、选中商品详情、下单、即时成交、手持存入判定已切到运行时目录 decision；默认目录只保留为运行时离线时的窄回退
  - 新增命令层与终端层测试，证明上层会跟随注入的运行时目录 version/source/reject decision，而不是依赖本地默认目录碰巧一致

### 2026-04-03 - 产出定制商品市场最小挂牌链分阶段 Prompt

- 主题：为三市场执行顺序中的下一阶段补出定制商品市场最小挂牌链实现 prompt，并明确按阶段逐步落地的边界
- 影响范围：`docs/custom-market-minimal-listing-chain-prompt-2026-04-03.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：标准商品市场目录边界已经收口，下一阶段不应继续扩标准商品市场或提前拆 MARKET 总入口，而应开始定义定制商品市场自己的挂牌、成交、待领取与交付留痕主链
- 引用来源：`docs/market-three-part-architecture.md`、`../../Docs/下次对话议程.md`、`../../Docs/市场经济推进.md`
- 结果：新增一份面向定制商品市场的正式实现 prompt，要求按“模型与 DDL -> 应用服务 -> 兼容入口 -> 测试与文档”逐阶段推进，并明确禁止复用标准商品市场订单簿与统一仓储模型来硬凑非标商品交易

### 2026-04-03 - 验收补出标准商品市场目录边界收口 Prompt

- 主题：在本轮验收后补出一个只针对“命令层、终端层、服务层仍各自持有默认目录实例”的收口 prompt
- 影响范围：`docs/standardized-market-catalog-boundary-tail-close-prompt-2026-04-03.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：标准商品市场正式目录对象、来源分层、服务层准入和测试主体都已成立，但命令层与终端层仍直接 `createDefaultCatalog(...)`，还没有完全收口成单一运行时目录边界
- 结果：新增一份窄范围收口 prompt，要求把目录 decision 收口为同一运行时事实来源，并补测试证明命令层与终端层不会再和真实服务边界分叉

### 2026-04-03 - 标准商品市场正式目录与准入边界落地

- 主题：把标准商品市场从直接依赖 GregTech 金属临时目录，推进为拥有正式目录版本、准入决策和来源分层的运行时边界
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/application/`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/`、`src/test/java/com/jsirgalaxybase/modules/core/market/infrastructure/jdbc/MarketPostgresIntegrationTest.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/standardized-market-catalog-boundary.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前标准商品市场撮合和仓储链已经能跑，但“哪些商品允许进入市场”仍主要被 `GregTechStandardizedMetalCatalog` 这类临时适配实现直接决定，导致正式制度边界没有独立出来
- 引用来源：`docs/market-three-part-architecture.md`、`../../Docs/市场经济推进.md`、`../../Docs/下次对话议程.md`
- 结果：
  - 新增 `StandardizedMarketCatalogVersion`、`StandardizedMarketCatalogEntry`、`StandardizedMarketAdmissionDecision`、`StandardizedMarketAdmissionReason`、`StandardizedMarketCatalogService`、`StandardizedMarketCatalogSource`
  - 当前默认目录版本已固定为 `standardized-spot-catalog-v1`
  - `GregTechStandardizedMetalCatalog` 已下沉为 `目录来源适配器`，不再承担正式制度边界语义
  - `StandardizedSpotMarketService`、命令层与终端层已统一走新的目录准入主路径，并可带出目录版本、准入 reason 与来源标识
  - 新增标准商品目录边界说明文档，并补齐目录服务、来源桥接、服务层拒绝语义与命令层输出测试

### 2026-04-03 - 产出标准商品市场商品目录与正式准入边界 Prompt

- 主题：为汇率市场规则层收口后的下一阶段，产出标准商品市场商品目录与正式准入边界实现 prompt
- 影响范围：`docs/standardized-market-catalog-boundary-prompt-2026-04-03.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：汇率市场规则层 v1 已完成收口，正式执行顺序应进入“标准商品市场商品目录与正式准入边界”；当前仓库仍主要依赖 `GregTechStandardizedMetalCatalog` 作为临时准入来源，需要下一阶段 prompt 明确把正式目录边界与临时适配来源拆开
- 引用来源：`docs/market-three-part-architecture.md`、`../../Docs/市场经济推进.md`、`../../Docs/下次对话议程.md`
- 结果：新增一份只针对标准商品市场商品目录与正式准入边界的 prompt，并同步到 docs 索引，作为汇率市场之后的下一阶段实现依据

### 2026-04-03 - 汇率市场兼容 quote 桥收口禁兑结果过早抛错

- 主题：修补汇率市场兼容桥在 `quote hand` 上把禁兑报价过早抛成异常的问题，确保禁兑任务书硬币也能返回正式规则层报价
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangeService.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangeServiceTest.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/WORKLOG.md`
- 原因：正式规则层已经能返回 `DISALLOWED` 报价，但兼容桥此前把 preview 与 execute 共用成“必须可执行”路径，导致 `/jsirgalaxybase market quote hand` 无法展示正式规则字段，并把禁兑误退化成普通拒绝
- 引用来源：`docs/exchange-market-rules-layer-v1-tail-close-prompt-2026-04-03.md`、`docs/market-three-part-architecture.md`
- 结果：
  - 兼容桥已拆开 `preview` 与 `execute` 判断：报价路径允许返回 `DISALLOWED` 正式报价，执行路径仍然在吞物前拒绝不可执行输入
  - `/jsirgalaxybase market quote hand` 现在会输出 `pair`、`ruleVersion`、`limitStatus`、`reasonCode`、`notes` 等正式字段，不再把禁兑误报成“非汇率市场资产”
  - `/jsirgalaxybase market exchange hand` 对同类禁兑输入仍保持拒绝执行语义，且错误消息来自正式规则层 note
  - 新增桥接层测试与命令输出测试，并通过针对性 Gradle 验证

### 2026-04-03 - 验收补出汇率市场兼容 quote 桥收口修补 Prompt

- 主题：在本轮验收后补出一个只针对“禁兑报价被兼容桥过早抛错”的收口修补 prompt
- 影响范围：`docs/exchange-market-rules-layer-v1-tail-close-prompt-2026-04-03.md`、`docs/WORKLOG.md`
- 原因：正式规则层已经能返回 `DISALLOWED` 报价，但兼容桥仍会把这类结果提前抛成异常，导致 `/jsirgalaxybase market quote hand` 无法展示正式 `limitStatus / reasonCode / notes`
- 结果：新增一份窄范围修补 prompt，明确要求把 preview 与 execute 判断拆开，让 `quote hand` 能展示禁兑正式字段，而 `exchange hand` 仍保持拒绝执行

### 2026-04-03 - 汇率市场正式规则层 v1 落地并保留旧命令兼容入口

- 主题：把任务书硬币兑换从“能跑的固定规则入口”收口为汇率市场正式规则层 v1，同时保留旧 `/jsirgalaxybase market quote hand` 与 `/jsirgalaxybase market exchange hand` 作为兼容入口
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/application/`、`src/main/java/com/jsirgalaxybase/modules/core/market/domain/`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/WORKLOG.md`
- 原因：三市场结构已经定稿，当前最先要落的不是继续扩标准商品市场，而是把已有任务书硬币兑换正式归位为 `汇率市场` 规则层，并把旧 MARKET 文案从“商品市场一期”改正为“汇率市场兼容入口”
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/下次对话议程.md`、`docs/market-three-part-architecture.md`
- 结果：
  - 新增正式 `ExchangeMarket*` 规则对象与 `ExchangeMarketService`，把 pair、ruleVersion、limitPolicy、quote result、execution result 明确建模
  - 旧 `TaskCoinExchangeService` 已改为手持物品兼容桥，不再直接承载汇率市场主语义
  - 命令层和终端首页文案已改成“汇率市场兼容入口”，并在报价/执行输出中补充 pair、ruleVersion、limitStatus、reasonCode 等正式字段
  - 修复了任务书硬币不支持档位识别把 `IV` 误判成高阶禁用的问题
  - 已通过针对性测试：`GalaxyBaseCommandTest`、`TaskCoinExchangePlannerTest`、`ExchangeMarketServiceTest`

### 2026-04-03 - 新增本地三进程联调启动脚本

- 主题：为 dedicated server + 双 client 人工联调新增用户可自行执行的一键启动脚本，避免再依赖 agent 后台终端持有进程
- 影响范围：`scripts/start-local-test-stack.sh`、`docs/WORKLOG.md`
- 原因：用户需要自己在本地 shell 中稳定维持三进程联调，并且每次启动前都要自动清理旧的 client/server 进程，避免残留实例和共享 gameDir 干扰测试
- 结果：
  - 新增 `scripts/start-local-test-stack.sh`，会先搜索并杀掉当前工作区相关的 runServer/runClient 进程
  - 脚本按 `server -> client A -> client B` 顺序启动，并为两个 client 显式使用独立 gameDir
  - 脚本在前台作为 supervisor 持有三进程，按 `Ctrl-C` 会统一清理

### 2026-04-03 - 市场三分结构正式设计落库并归位旧实现

- 主题：把市场重新收口为标准商品市场、定制商品市场、汇率市场三条正式产品线，并把现有代码残留归位到正式设计文档
- 影响范围：`docs/market-three-part-architecture.md`、`docs/README.md`、`../Docs/市场经济推进.md`、`../Docs/下次对话议程.md`、`docs/WORKLOG.md`
- 原因：当前仓库虽然已经在制度文档里恢复三市场方向，但仓库内仍缺少一份明确说明“旧单一路线残留属于哪一类市场、哪些应冻结、接下来先做哪条线”的正式设计文档
- 结果：
  - 新增 `docs/market-three-part-architecture.md`，正式写清三类市场边界、共享能力、最小模型拆分、旧实现归位判断和下一阶段执行顺序
  - 明确现有标准化现货代码只属于 `标准商品市场早期残片`，任务书硬币兑换只属于 `汇率市场早期残片`
  - 明确当前还没有真正属于 `定制商品市场` 的正式实现，禁止再把旧 MARKET 页面继续扩成统一大市场
  - 同步更新文档索引与下次议程，把后续优先级改为“先收口三市场执行顺序，再细化汇率和回收规则”

### 2026-04-03 - 三市场下一阶段顺序同步修正

- 主题：把“下一阶段执行顺序”从包含已完成的三市场总设计步骤，修正为从汇率市场正式规则层开始
- 影响范围：`docs/market-three-part-architecture.md`、`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
- 原因：三市场总设计已经正式落库，后续执行顺序应从 `汇率市场正式规则层 -> 标准商品目录与准入边界 -> 定制商品市场最小挂牌链 -> MARKET 总入口拆分` 开始，避免继续把已完成前置步骤写成“下一阶段”
- 结果：正式架构文档与推进文档的下一阶段顺序已统一为从汇率市场正式规则层开始

### 2026-04-03 - 产出汇率市场正式规则层改代码 Prompt

- 主题：为三市场结构验收后的首个代码阶段产出汇率市场正式规则层改代码 prompt
- 影响范围：`docs/exchange-market-rules-layer-code-prompt-2026-04-03.md`、`docs/WORKLOG.md`
- 原因：三市场正式边界和下一阶段顺序已经验收，需要一个明确限制范围的代码 prompt，把当前 `TaskCoinExchange*` 早期入口收口成汇率市场正式规则层
- 结果：新增一份只针对汇率市场正式规则层的代码 prompt，明确禁止继续扩标准商品市场、定制商品市场或 MARKET 总入口

### 2026-04-03 - 撤销金属市场文档方向并恢复三市场结构

- 主题：撤销文档中把市场收窄为金属专场和单一路线的错误方向，统一恢复为标准商品市场、定制商品市场、汇率市场三层结构
- 影响范围：`../Docs/设定.md`、`../Docs/市场经济推进.md`、`../Docs/下次对话议程.md`、`docs/market-gui-phase1-product-detail-prompt-2026-04-02.md`、`docs/market-gui-phase1-tail-fix-prompt-2026-04-03.md`、`docs/banking-java-domain-draft.md`、`.github/agents/GalaxyMod.agent.md`、`docs/WORKLOG.md`
- 原因：用户确认此前讨论的正式市场结构一直是 `标准商品市场 / 定制商品市场 / 汇率市场`，文档里后续把它误收窄成单一商品专场属于方向漂移
- 结果：
  - 主文档已统一改回三市场结构，不再把标准商品市场默认等同于金属专场
  - 两份旧的市场 phase-one prompt 已降级为废弃说明，不再作为后续实现依据
  - 下一阶段已改成先补三市场总设计，再分别推进标准商品市场、定制商品市场和汇率市场边界

### 2026-04-03 - 产出市场三分结构下一阶段 prompt

- 主题：为后续市场开发产出新的下一阶段 prompt，明确从旧单一路线切回三市场结构后的正式执行顺序
- 影响范围：`docs/market-three-part-structure-next-phase-prompt-2026-04-03.md`、`docs/WORKLOG.md`
- 原因：用户要求直接给出下一阶段 prompt，而且当前最重要的不是继续堆旧 MARKET 页，而是先把 `标准商品市场 / 定制商品市场 / 汇率市场` 三条线的边界、共享能力和执行顺序固定下来
- 结果：
  - 新增三市场结构下一阶段 prompt，可直接交给下一轮实现者使用
  - prompt 已明确要求先做三市场总设计与旧实现归位，不再沿旧单一路线继续扩功能

### 2026-04-03 - 市场第一阶段收尾补齐 DDL 与 deposit 恢复收口

- 主题：把旧单一标准商品撮合方案的第一阶段尾差从 Java 语义层推进到 PostgreSQL DDL 与 deposit 恢复路径，避免 AVAILABLE / INVENTORY_DEPOSIT 只停留在枚举里
- 影响范围：`docs/market-postgresql-ddl.sql`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketService.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/MarketRecoveryService.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/`、`src/test/java/com/jsirgalaxybase/modules/core/market/infrastructure/jdbc/`
- 原因：最新审查确认还有两个阻塞点未收口：数据库 check constraint 仍未接受 `AVAILABLE / INVENTORY_DEPOSIT`，且 deposit 失败后会沿通用异常路径把库存语义漂移到错误状态
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/设定.md`
- 结果：
  - PostgreSQL DDL 已同步接受 `market_custody_inventory.custody_status = AVAILABLE` 与 `market_operation_log.operation_type = INVENTORY_DEPOSIT`
  - deposit 失败现在写入专用 recovery metadata，并由 `MarketRecoveryService` 显式收口，不再继续走通用 `escalateGeneric(...)`
  - 新增共享商品目录测试替身与目录校验测试，覆盖“允许的标准商品可通过、普通方块会被拒绝”
  - PostgreSQL 集成测试已切到“先 deposit、再卖出/撤单/claim”的新边界，并新增 deposit DDL 与恢复验证

### 2026-04-03 - 市场第一阶段收尾 prompt 补充阻塞修复与双 client 联调要求

- 主题：把第一阶段收尾 prompt 从“语义修补”补强到“阻塞修复 + 双 client 联调验收”版本
- 影响范围：`docs/market-gui-phase1-tail-fix-prompt-2026-04-03.md`、`docs/WORKLOG.md`
- 原因：最新审查确认除了统一仓储与标准化商品准入外，还存在 DDL 未同步与 deposit 恢复未收口两处阻塞；同时银行转账与市场交易都需要双人在线联调，不能再只靠单 client 自测
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/设定.md`
- 结果：
  - 在 prompt 中新增了 `AVAILABLE / INVENTORY_DEPOSIT` 的 DDL 同步要求
  - 在 prompt 中新增了 deposit 失败恢复或安全回滚必须显式收口的要求
  - 在 prompt 中把“与用户配合的双 client 人工联调”升级为硬性验收条件，明确覆盖银行转账与市场交易链路

### 2026-04-03 - 市场第一阶段收尾修复 prompt

- 主题：为市场终端第一阶段补齐统一仓储与标准化商品准入两处尾巴，产出收尾修复 prompt
- 影响范围：`docs/market-gui-phase1-tail-fix-prompt-2026-04-03.md`、`docs/WORKLOG.md`
- 原因：首轮 MARKET 商品详情页虽然已经接线完成，但审查发现卖单仍依赖手持扣物、标准化商品仍缺少集中准入校验，这两点会直接破坏既定市场边界
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/设定.md`
- 结果：
  - 明确当前已有的是统一市场托管库存与 CLAIMABLE 资产链路，而不是完整的统一仓储可卖库存闭环
  - 要求后续修复优先在现有 market custody inventory 上补 `AVAILABLE` 一类可卖状态，而不是再拆一套平行仓储系统
  - 要求新增集中式标准化商品准入校验，并强制命令层、终端层与服务层统一收口

### 2026-04-03 - 市场终端第一轮旧单一路线商品详情页落地

- 主题：把终端 MARKET 页从静态占位页切到旧单一标准商品撮合方案的商品详情页，并补齐最小本地测试
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/`、`src/main/java/com/jsirgalaxybase/modules/core/market/infrastructure/jdbc/JdbcMarketOrderBookRepository.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSessionControllerTest.java`、`docs/WORKLOG.md`
- 原因：市场 GUI 第一轮目标已经从 prompt 明确为“商品点击后的交易详情页”，继续保留只读占位页会阻塞真实市场终端的后续联调与视觉验收
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/设定.md`
- 结果：
  - 新增市场终端的 snapshot、service、session controller、sync binder、sync state 与 page builder，MARKET 页现在可浏览商品、查看买卖盘、提交限价/即时交易、查看个人订单并发起撤单与 CLAIMABLE 提取
  - `TerminalHomeGuiFactory` 已正式接入市场页主面板、6 个确认弹窗和市场 toast/HUD 转发，不再复用旧只读详情页壳
  - 市场订单簿 JDBC 查询现在包含 `PARTIALLY_FILLED`，GUI 可看到剩余可成交深度
  - 新增市场终端本地纯逻辑测试，覆盖输入 sanitize、数量解析与待处理 ID 标记规则

### 2026-04-03 - 修复终端打开即断线的 ModularUI sync 冲突

- 主题：修复按 `G` 打开终端时服务端因 ModularUI sync handler auto/manual 注册冲突而踢线的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`docs/WORKLOG.md`
- 原因：市场页 6 个数值输入框对应的 `StringSyncValue` 被 market binder 手工 `syncValue(...)` 注册后，又被 `TextFieldWidget.value(...)` 作为 auto sync handler 收集，触发 `Old and new sync handler must both be either not auto or auto!`
- 结果：改为与银行页一致，只保留文本框侧的 auto 注册，终端主界面打开时不再因 `jgb_terminal` 包处理中的 sync 冲突被服务端断开

### 2026-04-04 - 修复 G 键打开终端导致 fatal disconnect 的 sync 回退

- 主题：修复按 `G` 打开终端时再次触发的 ModularUI sync auto/manual 混链断线，并补独立事故文档
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`docs/terminal-g-key-fatal-sync-incident-2026-04-04.md`、`docs/WORKLOG.md`
- 原因：市场页 6 个数值输入框的 `StringSyncValue` 又被手工 `syncValue(...)` 注册回 binder，和 `TextFieldWidget.value(...)` 的 auto 注册重新混在同一条 sync 链上，服务端在 `TerminalHomeGuiFactory.open(...)` 期间抛 `Old and new sync handler must both be either not auto or auto!`，客户端因此被 fatal 断开
- 结果：再次移除这 6 个数值输入框的手工 sync 注册，只保留文本框自身的 auto 注册；补充独立事故文档沉淀症状、堆栈、修复方式与复发预防点

### 2026-04-04 - 修复脚本联调环境下银行转账输入框再次触发终端 fatal

- 主题：修复 `scripts/start-local-test-stack.sh` 联调时，按 `G` 打开终端仍因银行页转账输入框重复 sync 注册而 fatal 断线的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`、`docs/terminal-g-key-fatal-sync-incident-2026-04-04.md`、`docs/WORKLOG.md`
- 原因：市场页 6 个文本输入框虽然已经移除手工 sync，但银行页的 `bankTransferTargetName`、`bankTransferAmountText`、`bankTransferComment` 仍同时走了 binder 手工 `syncValue(...)` 和 `TextFieldWidget.value(...)` auto sync；终端首页会一并收集 bank 页面控件，因此打开终端时仍会触发同一个 `Old and new sync handler must both be either not auto or auto!`
- 结果：移除银行转账 3 个输入框的手工 sync 注册，保留文本框自身 auto sync；事故文档同步扩大到 market 与 bank 两类文本输入控件，避免以后只修一半

### 2026-04-02 - 市场 GUI 第一轮商品详情页 prompt

- 主题：为终端市场页从只读占位升级到“点击商品后的交易详情页”产出第一轮实现 prompt
- 影响范围：`docs/market-gui-phase1-product-detail-prompt-2026-04-02.md`、`docs/WORKLOG.md`
- 原因：终端第三轮工程收口已经完成，下一阶段不应继续空谈市场页，而要把用户点击商品后的真实交易详情页结构、动作和验收范围写成可直接执行的实现规格
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/设定.md`、`Reference/VendingMachine/src/main/java/com/cubefury/vendingmachine/blocks/gui/`
- 结果：
  - 明确市场页第一轮目标是商品点击后的交易详情页，而不是完整市场总站
  - 把订单簿、下单区、个人订单区、仓储/冻结/待领取区和规则提示区列为首轮必做
  - 明确 VendingMachine 只作为浏览与状态表达参考，不照搬自动售货机式快捷交易交互

### 2026-04-02 - Terminal GUI 第三轮工程收口与市场前置整理

- 主题：在不重写终端框架的前提下，对终端主工厂、通知语义、Dialog 壳层和纯逻辑测试做市场 GUI 前的最后一轮工程收口
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/`、`src/main/java/com/jsirgalaxybase/terminal/TerminalHudNotificationManager.java`、`src/test/java/com/jsirgalaxybase/terminal/`、`src/test/java/com/jsirgalaxybase/terminal/ui/`、`docs/terminal-plan.md`
- 原因：第二轮以后主要风险已经从“能不能做出来”转成“市场 GUI 接进来后主工厂会不会继续膨胀，以及通知/弹窗/本地表单逻辑是否缺少最小保护”
- 结果：
  - 把银行页面构建、银行本地会话状态、银行 sync 绑定和通用 widget helper 从 `TerminalHomeGuiFactory` 明确拆出
  - `TerminalNotification` 现在优先消费结构化 `TerminalActionFeedback`，银行旧文本推断仅保留为 fallback 兼容桥接
  - `TerminalDialogFactory` 升级为 `TerminalDialogSpec` 驱动，支持 severity、尺寸预设和更稳的长正文/detail lines 容器
  - 新增终端侧纯逻辑测试，覆盖通知构造、HUD 队列行为、银行本地 sanitize/parse 和 Dialog 配置默认值

### 2026-04-02 - Terminal GUI 第三轮精修 prompt：工程收口与市场 GUI 前置整理

- 主题：在终端第一轮排版收口、第二轮通知与弹层能力落地之后，补一份进入市场 GUI 前的稳健精修 prompt
- 影响范围：`docs/terminal-gui-phase3-polish-prompt-2026-04-02.md`、`docs/WORKLOG.md`
- 原因：当前终端已经达到可用且观感明显改善的阶段，下一步主要风险不再是功能缺失，而是主工厂继续膨胀、通知语义仍偏文案驱动、弹窗工厂和终端侧逻辑测试不足，若不先收口会放大市场 GUI 接入成本
- 结果：
  - 输出第三轮 GUI 精修 prompt，范围明确限制为主工厂最小职责拆分、通知结构化优先、Dialog 工厂市场前置升级、终端侧纯逻辑测试补齐
  - 明确本轮不做框架重写、不做过度设计、不直接开始完整市场 GUI
  - 保留客户端与服务端实装、启动并人工目检 GUI 的硬性验收要求

### 2026-04-02 - Terminal GUI 第二轮通知层与确认弹窗落地

- 主题：把终端第二轮 prompt 中的通知层、确认弹窗、终端外可见提示与轻量视觉组件正式接入当前银行终端
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/terminal-plan.md`
- 原因：第一轮已经把布局与文本结构收口，但终端仍缺少可复用通知壳层、真实确认交互和终端外提示路径，导致 GUI 还停留在“静态信息板”阶段
- 结果：
  - 新增统一 `TerminalNotification` 模型与 HUD 通知管理器，银行反馈可同时驱动页内通知与终端外提示
  - 客户端 bootstrap 新增终端 HUD overlay 注册，关闭终端后仍可看到银行结果提示
  - 玩家转账改为先经过确认弹窗，再通过 synced action 提交到服务端，不改既有银行业务语义
  - 银行首页新增物品图标状态卡，并显式给按钮接入点击音反馈

### 2026-04-02 - Terminal GUI 第二轮 prompt：通知层、弹层与轻量视觉组件

- 主题：基于第一轮结构收口后的实际游戏内目检结果，沉淀终端 GUI 第二轮 prompt，推进通知层、弹层交互和基础视觉组件能力
- 影响范围：`docs/terminal-gui-phase2-prompt-2026-04-02.md`
- 原因：第一轮已经证明终端文本与排版明显改善，下一步不应继续停留在静态内容卡片，而要开始补齐通知、确认弹窗、终端外可见提示与轻量视觉能力
- 结果：
  - 输出第二轮 GUI prompt，范围收敛为通知层、二级弹层、最小视觉组件与基础声音反馈
  - 将“编译产物必须实装到客户端和服务端并启动，由人工肉眼验证 GUI 效果”写为硬性验收要求
  - 明确第二轮不直接跳去做完整市场 GUI，而是先沉淀可复用交互壳层

### 2026-04-02 - 沉淀本地实装流程到 GalaxyMod agent

- 主题：把本轮 JsirGalaxyBase 本地编译、dedicated server 拉起、client 联调与人工目检流程写入工作区 agent，供后续复用
- 影响范围：`../../.github/agents/GalaxyMod.agent.md`、`docs/WORKLOG.md`
- 原因：本轮已经验证出一套可重复的本地实装顺序，也踩清了 duplicate mod、后台 cwd 丢失、非阻塞 Forge 噪声与市场表缺失这几个关键坑点，需要沉淀为下次可直接复用的操作规范
- 结果：
  - `GalaxyMod` 不再是占位模板，而是明确面向 GTNH / JsirGalaxyBase 开发与实装的自定义代理
  - 写入了标准本地实装流程、推荐启动命令、运行态检查点与常见坑点
  - 后续再做 client/server 实装时，可以直接按 agent 中的顺序执行，不必重新试错

### 2026-04-02 - Terminal GUI 第一轮结构收口

- 主题：对终端首页与银行子页做第一轮结构化 GUI 收口，减少固定高度卡片与长文本爆框风险
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：当前终端虽然已基于 ModularUI2，但正文区仍然大量依赖固定高度卡片和裸 `TextWidget`，不利于长文本、动态状态和后续市场页/通知层继续生长
- 结果：
  - 新增统一文本 helper，把说明文本、动态摘要和滚动摘要从零散裸 `TextWidget` 收口到统一策略
  - `createSectionShell(...)` 现在默认支持内容自适应，仅在少数视觉统一块保留固定高度
  - 页头 lead、toast 正文、bullet panel、data row 与银行说明块改为更偏内容优先的布局
  - 不改终端入口、页路由、银行同步模型与现有银行业务语义

### 2026-04-02 - Terminal GUI 第一轮重构方案与 ModularUI2 API 评估

- 主题：沉淀终端 GUI 的第一轮结构化重构 prompt，并把 ModularUI2 API 能力评估写入文档供后续实现参考
- 影响范围：`docs/terminal-modularui2-gui-refactor-prompt-2026-04-02.md`
- 原因：当前终端页面虽然已切到 ModularUI2，但仍存在长文本溢出、固定高度卡片过多、结构不利于后续市场 GUI 和通知层扩展的问题，需要先形成基于真实框架能力的重构方案
- 结果：
  - 确认当前 GUI 主体基于 ModularUI2 流式布局，不是纯绝对坐标系统
  - 确认框架已具备富文本、滚动文本、贴图、物品展示、对话框、子面板、tooltip 和基础点击音接口
  - 输出第一轮 GUI 重构 prompt，明确本轮先收口文本与布局，再为后续终端桌面化扩展预留结构

### 2026-04-02 - 第二层旧单一商品撮合方案命令层尾修

- 主题：补齐旧单一标准商品撮合方案命令层 cancel/claim 测试覆盖，并修正买单撤销后的释放金额回显
- 影响范围：`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketService.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketServiceTest.java`
- 原因：第二层运行时接线已完成，本轮只剩命令层覆盖和玩家回显这类尾修项，避免把已释放冻结金误报成 `0`
- 结果：
  - 新增卖单撤销、买单撤销、claim 命令分发测试
  - 买单撤销回显改为使用 service 返回的真实 `releasedFunds`
  - 不改运行时装配、不扩阶段边界，只收口命令层可测性和玩家提示准确性

### 2026-04-02 - 第二层旧单一标准商品撮合方案接入服务器运行时

- 主题：把已完成的旧单一标准商品撮合服务层能力正式接入 dedicated-server 运行时、玩家命令入口与人工恢复触发
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/`、`src/test/java/com/jsirgalaxybase/modules/core/`、`src/test/java/com/jsirgalaxybase/command/`、`../../Docs/市场经济推进.md`
- 原因：上一轮虽然已经补齐第二层旧单一商品撮合方案的服务、JDBC 和恢复闭环，但服务器运行时还没有真正挂载市场服务，也没有玩家入口和管理员恢复触发，离“可实际使用”还差最后一段接线
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/技术边界文档.md`
- 结果：
  - `InstitutionCoreModule` 现在会在 dedicated-server 路径下同时装配 banking 与 shared JDBC market runtime
  - `GalaxyBaseCommand` 新增第二层现货命令入口，保留原有 phase-1 `quote/exchange` 路径不混用
  - 卖单创建入口现在会先扣除玩家手持标准化物，再调用市场服务，失败时原样回滚，避免虚空卖单
  - 新增管理员 `market recover` 手动恢复触发与启动时轻量恢复扫描挂点
  - 补充运行时装配测试与命令分发测试，覆盖 dedicated-server 装配、卖单扣物/失败回滚、买单分发、claim 列表与恢复入口

### 2026-04-01 - 旧单一标准商品撮合方案补齐买单恢复与 CLAIMABLE 提取闭环

- 主题：补齐旧单一标准商品撮合方案的买单冻结资金失败恢复闭环与 `CLAIMABLE` 资产提取写路径
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/modules/core/market/`、`docs/market-postgresql-ddl.sql`、`../../Docs/市场经济推进.md`
- 原因：上一轮只完成了最小买卖闭环，买方冻结金异常恢复与玩家真正提取 `CLAIMABLE` 资产仍未收口，存在一致性和可用性缺口
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/技术边界文档.md`
- 结果：
  - 新增 `recoveryMetadataKey`，把买单冻结金恢复与 claim 恢复线索显式落到操作日志
  - `MarketRecoveryService` 可释放未完成买单剩余冻结资金，并把订单状态收口到 `CANCELLED / FILLED`
  - 新增 `ClaimMarketAssetCommand`、`CLAIMING / CLAIMED` 状态与真实 claim delivery port
  - claim 成功后托管资产进入 `CLAIMED`，安全失败则恢复到 `CLAIMABLE`
  - 补齐市场单测与 PostgreSQL 集成测试，覆盖买单恢复和 claim 写路径

### 2026-03-29 - 初始化仓库与工作记录机制

- 主题：初始化本地 git 仓库与 work log 机制
- 影响范围：仓库根目录
- 原因：为后续 GitHub 上传、版本管理和持续开发记录做准备
- 结果：
  - 在 `JsirGalaxyBase` 仓库下初始化了本地 git 仓库
  - 建立本 work log 作为统一开发记录入口
  - 补录初次对话形成的架构和制度上下文
  - 后续每次代码更改都应在此追加简要记录

### 2026-03-29 - 排除外层仓库中的 Reference 目录

- 主题：将 `Reference/` 排除出外层 git 仓库
- 影响范围：`.gitignore`
- 原因：`Reference/` 下包含多个独立 git 仓库，直接加入外层仓库会形成嵌套仓库或 gitlink，上传到 GitHub 后不适合作为当前项目源码的一部分
- 结果：
  - 外层仓库不再跟踪 `Reference/`
  - `Reference/` 继续保留在本地，作为开发参考源码使用

### 2026-03-29 - 合并根文档并停用自动化 workflow

- 主题：重写根 README，建立 `docs/` 目录，并停用 GitHub Actions workflow
- 影响范围：`README.md`、`docs/`、`.github/workflows/`
- 原因：不再需要把架构拆成单独根文档，同时希望把面向玩家和协作者的基础材料集中到 `docs/` 目录中，并关闭当前自动化编译流程
- 结果：
  - 根 `README.md` 合并了原先独立架构文档的核心内容
  - `WORKLOG.md` 迁移到 `docs/WORKLOG.md`
  - 新增 `docs/README.md` 作为文档入口
  - 删除现有自动化编译与 release workflow 文件

### 2026-03-29 - 将项目正式更名为 JsirGalaxyBase

- 主题：将项目名称从 `CustomMod` 统一更名为 `JsirGalaxyBase`
- 影响范围：Java 包名、主类名、命令类名、Gradle 模组元数据、配置路径、README 与 docs 文档
- 原因：`CustomMod` 过于临时和泛化，无法准确承载当前制度核心加能力模块的长期定位；`JsirGalaxyBase` 更适合作为正式项目名
- 结果：
  - 根包切换为项目正式包名
  - 主类切换为 `GalaxyBase`
  - 命令切换为 `/jsirgalaxybase`
  - 模组 `modid` 切换为 `jsirgalaxybase`
  - 文档标题与项目引用同步更新

### 2026-03-29 - 排除本机环境名并建立正式命名约定

- 主题：从代码与元数据中移除本机环境名痕迹，并建立正式命名约定
- 影响范围：Java 根包、Gradle 元数据、mcmod 作者字段、README、工作目录命名说明
- 原因：本机环境名不应进入模组正式命名空间；需要把工程名、模组名、包名和仓库名分开定义清楚
- 结果：
  - Java 根包统一为 `com.jsirgalaxybase`
  - Gradle `modGroup` 与生成的 `Tags` 类路径同步改为 `com.jsirgalaxybase`
  - `mcmod.info` 作者显示改为 `Jsir2022`
  - README 新增命名约定章节，明确仓库名、目录名、模组名、`modid` 和包名的分工

### 2026-03-29 - 工作目录与 GitHub 仓库名对齐

- 主题：将本地工作目录从 `CustomMod` 改为 `JsirGalaxyBase`
- 影响范围：本地仓库目录路径
- 原因：保持本地工作目录与 GitHub 仓库名一致，减少工程名、目录名和远端仓库名之间的混淆
- 结果：
  - 本地仓库目录已改为 `JsirGalaxyBase`
  - 当前仓库名、工作目录名和 GitHub 仓库名保持一致
  - 模组运行时名称继续保持为 `JsirGalaxyBase`

### 2026-03-29 - 统一重命名为 JsirGalaxyBase / GalaxyBase

- 主题：将仓库、模组和文档名称统一切换为 `JsirGalaxyBase`，并把代码主类简写为 `GalaxyBase`
- 影响范围：`README.md`、`docs/`、Gradle 模组元数据、Java 根包、Forge 主类、命令类与本地目录命名
- 原因：用户要求统一正式名称，减少旧命名和运行时代号混杂；同时保留代码类名的可读性
- 结果：
  - 模组展示名与 `modid` 改为 `JsirGalaxyBase` / `jsirgalaxybase`
  - Java 根包改为 `com.jsirgalaxybase`
  - Forge 主类改为 `GalaxyBase`
  - 根命令改为 `/jsirgalaxybase`
  - README 与 docs 的命名约定同步更新
  - 本地工作目录已切换为 `JsirGalaxyBase`
  - GitHub 新仓库地址 `git@github.com:Jsir2022/JsirGalaxyBase.git` 当前尚不存在，远端切换需等待 GitHub 侧先完成仓库重命名

### 2026-03-29 - 确认终端入口与终端壳实施方案

- 主题：确定终端第一阶段采用快捷键入口加背包按钮入口，并先落稳定打开链与占位终端壳
- 影响范围：`docs/terminal-plan.md`、`README.md`、`docs/README.md`
- 原因：终端将承担职业、市场、福利和公共服务的统一入口，必须先把入口链、服务端鉴权与可替换 UI 壳分离清楚
- 结果：
  - 确认终端第一阶段先实现快捷键打开与背包按钮打开
  - 确认两条入口共用同一条服务端打开链
  - 确认 Pad 物品入口延后到后续阶段
  - 确认当前先使用占位终端壳，后续再替换为 `ModularUI 2`

### 2026-03-29 - 终端首页切换到 ModularUI 2

- 主题：移除旧占位 GUI，改为真实的 `ModularUI 2` 终端首页壳
- 影响范围：`dependencies.gradle`、`src/main/java/com/jsirgalaxybase/modules/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/ui/`
- 原因：当前入口链已经稳定，下一步需要把终端正式切到 `ModularUI 2`，为职业、贡献度、声望、公共任务和市场摘要首页建立可扩展的同步面板
- 结果：
  - 新增 `ModularUI2` 依赖并注册终端 UI 工厂
  - 服务端打开链改为 `GuiManager.open(...)`，继续保持服务端权威
  - 删除旧 `IGuiHandler` 和占位容器 / 占位界面实现
  - 新增终端首页快照与只读首页面板，接入职业、贡献度、声望、公共任务、市场摘要五项展示

### 2026-03-29 - 终端补左侧导航与正式分页壳

- 主题：把终端从单页总览扩成左侧导航加右侧内容区的正式框架
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/`、`docs/WORKLOG.md`
- 原因：后续职业、公共事务、市场等页面需要统一壳，不能继续停留在单页演示态
- 结果：
  - 终端改为左侧导航与右侧分页内容结构
  - 首页继续保留制度摘要，同时新增职业、公共、市场三页的只读占位内容
  - 分页状态已接入 `IntSyncValue`，后续可在同一终端壳内继续扩页而不改入口链

### 2026-03-29 - 终端改为宽屏控制台风格

- 主题：参考 AE2 终端比例，重做终端观感与窗口尺寸

### 2026-03-30 - 整理 Ubuntu 24 向日葵安装复用资料

- 主题：补充 Ubuntu 24 下向日葵 15.2.0.63064 的安装记录、复用脚本与依赖包整理目录
- 影响范围：`../../Docs/sunlogin-ubuntu24/`、`docs/WORKLOG.md`
- 原因：本次实际排查出 Ubuntu 24 官方仓库缺失旧版 `libgconf-2-4` 依赖，且 Wayland 会导致向日葵被控黑屏，需要把安装包、步骤和 Xorg 配置整理成可复用资料，便于后续其他机器快速落地
- 结果：
  - 新增 `Docs/sunlogin-ubuntu24/README.md` 记录完整安装与黑屏修复流程
  - 新增 `Docs/sunlogin-ubuntu24/install_sunlogin_ubuntu24.sh` 作为复用脚本
  - 将新版安装包与所需旧依赖 `.deb` 统一整理到 `Docs/sunlogin-ubuntu24/packages/`
  - 记录 GDM 关闭 Wayland、改走 Xorg 的关键配置点
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：上一版窗口过小、信息层级过弱、默认按钮味太重，不适合作为长期制度终端壳
- 结果：
  - 终端窗口显著放大，改成更接近控制台的宽屏比例
  - 新增顶部状态带、侧栏导航、首页摘要卡片和分区内容卡
  - 视觉风格从默认灰底面板改为更偏控制台的深色块面布局

### 2026-03-29 - 终端切到居中加分辨率自适应布局

- 主题：让终端窗口和主内容区按屏幕比例自动缩放并保持居中
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：终端不能只针对当前开发分辨率，必须在不同屏幕尺寸下保持可读和稳定布局
- 结果：
  - 外层面板改为相对屏幕宽高的居中布局
  - 内层主容器、导航列和内容列切到相对宽高分配
  - 页面页脚文案同步更新为自适应布局状态

### 2026-03-29 - 终端窗口比例提升到接近全屏

- 主题：把终端窗口提高到接近屏幕 90% 的使用面积
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：当前终端已经可用，但仍然偏保守，用户希望更接近全屏控制台体验
- 结果：
  - 终端外层窗口改为宽高各占屏幕约 90%
  - 保留居中和相对布局逻辑，不回退到固定像素窗口

### 2026-03-29 - 终端切换到深蓝灰控制台主题

- 主题：参考现代控制面板网页的配色和细边线分区，重做终端首页观感
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：原版 Minecraft 容器风格不适合制度终端，上一版纯色卡片也不够像真正的控制台页面
- 结果：
  - 终端整体配色改为深蓝灰主背景加蓝色高亮
  - 页面结构改成细边线包裹的分区面板，而不是大块卡片堆叠
  - 首页新增路由矩阵、联机概览、公共队列和市场监控四类控制台区块

### 2026-03-29 - 终端压缩左栏并关闭调试噪声

- 主题：缩小左侧导航、把标题并入导航头，并清理开发环境中无意义的启动告警
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`、`run/client/config/`
- 原因：当前样式已定稿，但左栏和顶部标题带占用过多空间，右侧内容区过挤，同时 `ModularUI 2` 调试模式和 Forge 版本检查噪声影响最终体验
- 结果：
  - 标题并入左侧导航头，顶部独立标题带移除
  - 左侧导航列缩窄，右侧内容区获得更多宽高空间
  - 首页和详情页区块高度、表头和文案密度下调，减少挤压与遮挡
  - 关闭 `ModularUI 2` 的测试 GUI 与调试描边，补齐 `NEI` 缺失的 `untranslator.cfg`
  - 关闭开发客户端中的 Forge 版本检查，减少启动时无关异常输出
  - 为开发环境补齐 `minecraft` 与 `gtnhlib` 缺失的 `items` 纹理别名，消除启动时的缺失贴图告警
  - 补齐 `IGuiHolder#createScreen(...)` 并回调若干紧凑行高，消除 `ModularUI 2` 的屏幕创建与纵向 padding 警告
  - 参考 AE2 终端的上下分区布局，把左侧导航改成顶部固定、底部固定、中间弹性伸缩的自适应结构，避免窗口高度变化时溢出边框
  - 重新压缩右侧内容区的长文本和中部比例，避免中文长句在窄栏内被挤成纵向显示
  - 进一步收窄左侧导航为固定窄比例，并移除矩阵左侧辅助说明列，把联机概览缩成纯图例卡，解决中部两块的剩余挤压
  - 把首页高度分配改为固定头部、弹性中段、固定底部，并缩减矩阵行数和右侧概览内容，保证内容不再越出终端下边

### 2026-03-29 - 修正 WorldEdit 服务端分发形态

- 主题：修正 GTNH 服务端侧的 WorldEdit 分发，补齐 CUIFe，并保留 dist 包替代旧 core 的方案
- 影响范围：`GroupServer/packwiz/sync_root/mods/`、`GroupServer/GTNHServerConfig/server_mods/mods/`、`GroupServer/Galaxy_GTNH284_S1/mods/`、`GroupServer/Galaxy_GTNH284_S2/mods/`、`GroupServer/Galaxy_GTNH_Lobby/mods/`
- 原因：复核后确认 `worldedit-forge-mc1.7.10-6.1.1-dist.jar` 已内置 core 类，不应再额外叠加旧 `worldedit-core`；但 `WorldEditCuiFe` 需要同步补到服务端侧，之前这一部分漏放了
- 结果：
  - 保持 `worldedit-forge-mc1.7.10-6.1.1-dist.jar` 作为 WorldEdit 服务端实现
  - 将 `WorldEditCuiFe-v1.0.7` 补入 `GTNHServerConfig` 服务端私货目录和 S1/S2/Lobby live 实例
  - 未执行 live 服重启，当前仍为落盘待生效状态

### 2026-03-29 - 调整 GTNH 默认同步包的跨维传送与黑暗模组

- 主题：在 `packwiz` 同步源中放开 ServerUtilities 跨维传送，并移除 `Darkerer`
- 影响范围：`GroupServer/packwiz/sync_root/serverutilities/server/ranks.txt`、`GroupServer/packwiz/sync_root/mods/`、`GroupServer/packwiz/sync_root/config/`
- 原因：改善玩家跨维 `/home` 和 `/warp` 体验，并移除 GTNH 包内的真实黑暗实现，后续由 `packwiz` 同步到客户端与服务端
- 结果：
  - 为 `player`、`vip`、`admin` 全部开启 `serverutilities.homes.cross_dim` 与 `serverutilities.warps.cross_dim`
  - 从默认同步源删除 `darkerer-1.0.6.jar`
  - 从默认同步源删除 `config/darkerer.cfg`

### 2026-03-29 - 补齐私货 WorldEdit 分发与 live 服跨维传送配置

- 主题：整理根目录私货 jar，准备替换 WorldEdit 为 dist 包，并把 live 实例的 ServerUtilities 跨维权限直接改到位
- 影响范围：`GroupServer/packwiz/sync_root/mods/`、`GroupServer/GTNHServerConfig/`、`GroupServer/Galaxy_GTNH284_S1/`、`GroupServer/Galaxy_GTNH284_S2/`、`GroupServer/Galaxy_GTNH_Lobby/`
- 原因：在不重启 live 服的前提下，先把创世神相关模组文件、WorldEdit 配置镜像和 `/home` `/warp` 跨维权限准备完成
- 结果：
  - 计划用私货 `worldedit-forge-mc1.7.10-6.1.1-dist.jar` 替换旧 `worldedit-core + worldedit-forge` 组合
  - GTNHServerConfig 补齐 `config/worldedit/worldedit.properties` 镜像
  - GTNHServerConfig 与 S1/S2/Lobby 的 `serverutilities/server/ranks.txt` 全部开启跨维 `home/warp`

### 2026-03-30 - 明确群组服一期后端与同步边界

- 主题：把一期后端架构、同步范围和免费传送规则正式写入文档
- 影响范围：`README.md`、`../Docs/群组服.md`、`../Docs/技术边界文档.md`、`docs/WORKLOG.md`
- 原因：当前已明确一期不接 `Redis`，并且需要先把银行系统、主背包同步和免费跨服传送的边界定死，避免后续设计反复摇摆
- 结果：
  - 明确一期唯一中心化存储为 `PostgreSQL`
  - 明确一期采用模组服务端直连 `PostgreSQL`，不单独建设中心后端服务
  - 明确一期共享范围包括制度数据、主物品栏、护甲、经验、血量、饥饿
  - 明确“共享背包”当前等同于玩家按 `E` 打开的主背包数据
  - 明确跨服传送与 `home` 当前为免费规则
  - 明确一期实施顺序为：先银行系统，再落库与同步，再传送

### 2026-03-30 - 新增银行系统一期需求文档

- 主题：把银行系统一期能力、边界和非目标整理成正式需求文档
- 影响范围：`docs/banking-system-requirements.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：当前已经明确银行系统是一切制度与跨服能力的前置底座，需要先把需求边界固定，再进入具体表结构设计
- 结果：
  - 新增银行系统一期需求文档，明确初始玩家余额为 `0`
  - 明确玩家账户、税池、兑换所储备、后续公会资金都必须作为独立账户存在，不能退化为简单变量
  - 明确一期只做固定规则兑换结算，不做单独硬币交易市场与汇率系统
  - 明确一期不做任何扩展金融能力，如利息、贷款、定存等
  - 将银行系统需求文档加入仓库文档入口

### 2026-03-30 - 新增银行系统数据表与事务边界设计

- 主题：基于银行需求文档，落一期数据库表设计与事务边界草案
- 影响范围：`docs/banking-schema-design.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：银行系统需求边界已经固定，需要尽快把账户表、交易表、账本分录表和关键事务流程正式定稿，避免后续编码时重新发明模型
- 结果：
  - 新增银行系统数据表与事务边界设计文档
  - 明确一期核心表为 `bank_account`、`bank_transaction`、`ledger_entry` 与 `coin_exchange_record`
  - 明确所有资金变动必须在同一事务中完成锁定、校验、分录和余额更新
  - 明确幂等键、行级锁、固定加锁顺序和禁止修改历史账本的约束
  - 将数据表设计文档加入仓库文档入口

### 2026-03-30 - 新增银行 Java 领域模型与仓储接口草案

- 主题：把银行需求与表设计翻译成 Java 侧代码骨架
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/`、`docs/banking-java-domain-draft.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：仅有表设计还不够，必须尽快把领域模型、仓储接口和事务边界接口固定下来，避免后续业务实现直接散落 SQL 和字符串常量
- 结果：
  - 新增银行领域对象、关键枚举和仓储接口草案
  - 新增 `BankingTransactionRunner` 事务边界接口，用于表达银行写操作必须运行在同一事务中
  - 新增 Java 侧设计说明文档，建立文档设计与代码骨架之间的映射关系
  - 将 Java 侧设计文档加入仓库文档入口

### 2026-03-30 - 新增银行 PostgreSQL DDL 草案

- 主题：把一期银行表设计正式落成 PostgreSQL SQL 文件
- 影响范围：`docs/banking-postgresql-ddl.sql`、`docs/banking-schema-design.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：仅有 Markdown 表设计还不够，后续开始写 JDBC 仓储或迁移脚本前，需要一份可以直接对照执行和继续演进的 DDL 草案
- 结果：
  - 新增一期银行核心表的 PostgreSQL DDL 草案文件
  - 固定 `bank_account`、`bank_transaction`、`ledger_entry`、`coin_exchange_record` 与可选 `bank_daily_snapshot` 的字段、约束和索引
  - 明确账户 `updated_at` 触发器策略以及金额非负、幂等键唯一、账本顺序唯一等数据库侧约束
  - 将 SQL 草案加入设计文档与仓库文档入口

### 2026-03-30 - 开始落银行应用服务实现

- 主题：把银行一期业务动作从纯文档和接口草图推进到 application 层代码
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/application/`、`src/main/java/com/jsirgalaxybase/modules/core/banking/domain/CoinExchangeRecord.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/repository/LedgerEntryRepository.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：DDL 和领域模型已经齐备，下一步必须先把一期真正的业务动作编排固定下来，后续 JDBC 仓储才能按稳定签名实现
- 结果：
  - 新增 `BankingApplicationService`，统一承接开户、查询、玩家转账、内部划转和硬币兑换结算
  - 新增对应命令对象与 `BankPostingResult`，收束一期业务入参与出参
  - 补上基于 `request_id` 的幂等回放能力所需仓储查询签名
  - 将 `CoinExchangeRecord` 字段补齐到与 PostgreSQL DDL 更一致的状态

### 2026-03-30 - 补齐银行 JDBC 基础设施层

- 主题：把银行 application 层继续落到可对接 PostgreSQL 的 JDBC 边界
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：只有 application service 还不够，必须同步提供事务执行器与仓储实现骨架，后续才能真正接数据库和跑集成验证
- 结果：
  - 新增 JDBC 连接管理器与事务执行器，支持线程内事务连接复用
  - 新增账户、交易、账本分录、兑换记录的 JDBC 仓储实现
  - 将 `SELECT ... FOR UPDATE`、幂等查询、批量追加分录和账户余额更新明确落到代码层
  - 编译验证通过，当前只差 PostgreSQL 连接配置与模块装配

### 2026-03-30 - 接入银行配置项与模块初始化挂载点

- 主题：把银行 JDBC 基础设施从“仅可编译类库”推进到模块生命周期中的可装配状态
- 影响范围：`src/main/java/com/jsirgalaxybase/config/ModConfiguration.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：如果不把配置项和初始化入口接上，后续命令或 GUI 层仍然拿不到银行服务实例
- 结果：
  - 新增 PostgreSQL 银行连接配置项与 `source_server_id` 配置项
  - 新增 `BankingInfrastructure` 聚合对象与基于 `DriverManager` 的 `DataSource` 工厂
  - `InstitutionCoreModule` 已可在服务端按配置准备银行基础设施实例
  - 编译验证通过，当前剩余工作聚焦于 PostgreSQL 驱动依赖、真实连通验证和上层入口接线

### 2026-03-30 - 补充 PostgreSQL 本地安装与迁移说明

- 主题：补上 Ubuntu 宿主机 PostgreSQL 安装、初始化与换机迁移指导
- 影响范围：`docs/postgresql-local-setup-and-migration.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：当前机器没有安装 PostgreSQL，且当前会话没有无密码 sudo，无法直接代装到宿主机；需要把安装与迁移流程沉淀为可执行说明
- 结果：
  - 新增 Ubuntu 24.04 下 PostgreSQL 安装与建库说明
  - 补充基于当前 DDL 的初始化命令
  - 明确换机迁移推荐走逻辑备份而不是直接拷贝数据目录
  - 补充最小备份命令，降低后续换主机时的数据丢失风险

### 2026-03-30 - 完成本机 PostgreSQL 安装与模组真实连通验证

- 主题：把 PostgreSQL 从文档方案推进到宿主机实际运行与模组服务端启动验证
- 影响范围：`dependencies.gradle`、`repositories.gradle`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankingInfrastructureFactory.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`docs/banking-java-domain-draft.md`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：银行 JDBC 实现已经存在，但如果没有真实数据库、真实驱动和真实服务端启动验证，就还不能说明这条链路可用
- 结果：
  - 在 Ubuntu 24.04 宿主机安装并启动 PostgreSQL 16
  - 创建本地开发账号 `jsirgalaxybase_app` 与数据库 `jsirgalaxybase`
  - 将一期银行 DDL 实际执行到本地数据库，确认核心表全部存在
  - 新增 PostgreSQL JDBC 驱动依赖与 Maven Central 仓库声明
  - 启动 `runServer` 完成模组服务端真实联调，日志明确显示银行 PostgreSQL 基础设施已准备并验证成功

### 2026-03-30 - 收紧本地数据库监听并接入银行管理员测试命令

- 主题：把本地 PostgreSQL 显式限制在回环地址，并提供游戏内银行管理测试入口
- 影响范围：`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/module/ModuleManager.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：当前数据库不应暴露外网监听，同时银行系统已经具备基础设施，需要第一个实际管理员入口来驱动开户、查余额、发钱和转账测试
- 结果：
  - PostgreSQL 已显式配置为只监听 `127.0.0.1:5432`
  - 本地开发业务账号密码已切换为用户指定的新密码
  - 在 `/jsirgalaxybase bank` 下新增 `open`、`balance`、`grant`、`transfer` 四个管理员测试子命令
  - 通过实际 `runServer` 自动控制台执行验证了 bank 命令帮助输出与命令注册链路

### 2026-03-31 - 放开 NEI 客户端主配置的本地持久化

- 主题：让 GTNH 客户端的 `NEI` 主配置不再被 `packwiz` 更新与 `Default Configs` 启动流程反复覆盖
- 影响范围：`GroupServer/packwiz/sync_root/packwiz-whitelist.json`、`GroupServer/packwiz/sync_root/config/localconfig.txt`、`GroupServer/packwiz/whitelist-localconfig-notes.md`、`docs/WORKLOG.md`
- 原因：玩家反馈 `NEI` 配置经常被自动替换；排查确认活跃文件 `config/NEI/client.cfg` 未进入白名单，同时 `localconfig.txt` 还在接管整份 `NEI/client.cfg`
- 结果：
  - 将 `config/NEI/client.cfg` 加入 `packwiz` 白名单
  - 保留 `config/NEI/client.cfg.bak` 白名单不变
  - 注释掉 `localconfig.txt` 中对 `[NEI/client.cfg]/*/*` 的接管规则
  - 在既有白名单说明文档中补记 `NEI` 案例，方便后续处理同类客户端配置问题

### 2026-03-30 - 扩展银行管理员命令到系统账户与最近流水查询

- 主题：把第一版银行测试命令从单纯改余额扩展到状态查询与账本查看
- 影响范围：`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：仅有开户、查余额、发钱、转账还不够，管理员需要直接查看最近流水和系统测试账户状态，才能形成最小可用的联调闭环
- 结果：
  - 在 `/jsirgalaxybase bank` 下新增 `ledger`、`system` 和 `system ledger` 命令
  - recent ledger 输出已包含交易号、方向、金额、变动前后余额和时间戳
  - system summary 会显示测试系统账户的编号、类型、状态和当前余额
  - 通过实际 `runServer` 自动执行验证了 system summary 与 system ledger 命令回显

### 2026-03-30 - 扩展银行管理员命令到公共账户、交易详情与系统账户初始化

- 主题：把银行管理命令从单账户测试扩展到公共账户与交易审计层面
- 影响范围：`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/repository/BankTransactionRepository.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankTransactionRepository.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：当前需要直接初始化系统账户集、查看公共账户状态，并能按交易号定位单笔交易详情，方便后续联调和审计
- 结果：
  - 新增 `/jsirgalaxybase bank public` 与 `/jsirgalaxybase bank public ledger` 命令用于查看受管公共/系统账户
  - 新增 `/jsirgalaxybase bank tx <transactionId>` 命令用于查询单笔交易与关联账本分录
  - 新增 `/jsirgalaxybase bank init system` 命令，用于初始化测试系统资金、系统运营账户、税池和兑换储备账户
  - 通过实际 `runServer` 自动执行验证了系统账户初始化、公共账户汇总与交易不存在时的详情查询回显

### 2026-03-30 - 收敛受管系统账户并补齐备份恢复脚本

- 主题：把系统账户模型收敛为 `ops + exchange`，并把 PostgreSQL 逻辑备份/恢复脚本正式落地
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/ManagedBankAccounts.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`scripts/db-backup.sh`、`scripts/db-restore.sh`、`docs/postgresql-backup-and-restore.md`、银行相关文档
- 原因：当前不再需要测试资金池与独立税池，系统运营收支应统一落在 `ops`；同时换机和演练必须从“会手敲命令”升级到“有固定脚本可执行”
- 结果：
  - 受管系统账户收敛为 `ops` 系统运营账户与 `exchange` 兑换储备账户
  - 玩家账户仍保持按需懒初始化，不做自动开户
  - `InstitutionCoreModule` 在服务端启动时自动确保系统账户存在
  - `/jsirgalaxybase bank system` 与 `grant` 改为围绕 `ops` 账户工作，公共账户查询只展示 `ops` 与 `exchange`
  - 新增 PostgreSQL 逻辑备份与恢复脚本，并补充使用方式、演练步骤与风险控制说明

### 2026-03-30 - 增补 systemd 定时备份方案并明确快照技术取舍

- 主题：把 PostgreSQL 备份方案从“手动脚本可用”推进到“systemd 每日自动备份可落地”
- 影响范围：`ops/systemd/`、`scripts/install-systemd-backup.sh`、`docs/postgresql-backup-and-restore.md`、`docs/README.md`
- 原因：当前实际需求已经明确为“单数据库每日一份、保留七份”，需要正式选主方案，并说明为什么不把文件系统快照当当前主链路
- 结果：
  - 新增 system 级 `systemd service + timer` 模板与环境文件样例
  - 新增安装脚本，用于把 unit 安装到 `/etc/systemd/system/` 并启用 timer
  - 明确当前主方案是 `pg_dump -Fc + systemd timer + RETAIN_COUNT=7`
  - 明确文件系统快照不是当前主方案，后续如需更细恢复点应升级到 `pg_basebackup + WAL archive`

### 2026-03-30 - 补齐 PostgreSQL 备份恢复值班手册与真实演练说明

- 主题：把备份恢复文档从“方案说明”补成“后续维护者可以直接照抄命令执行”的操作手册
- 影响范围：`docs/postgresql-backup-and-restore.md`、`docs/WORKLOG.md`
- 原因：当前备份与恢复链路已经真实安装和演练通过，但如果不把日常查看、手动备份、恢复到测试库、覆盖正式库和清理测试库等指令写清楚，后续维护者仍然会不知道怎么用
- 结果：
  - 文档补充了当前机器上的实际部署状态、备份目录与 systemd 单元名称
  - 文档补充了日常查看、立即备份、恢复到测试库、覆盖正式库和删除测试库的完整命令
  - 文档明确了 `oneshot` service 的状态表现与业务账号无建库权限这两个常见注意事项

### 2026-03-30 - 明确银行终端页的信息架构并开始接真实只读快照

- 主题：把普通玩家正式入口的银行 GUI 从“想法”落成文档，并开始按终端壳的嵌套菜单模式实现
- 影响范围：`docs/banking-terminal-gui-design.md`、`docs/README.md`、终端 GUI 与终端快照提供者
- 原因：当前终端已经是左侧导航 + 主区板块的统一壳，银行页不能再做成单独弹窗或纯目录页，而应先展示关键内容，再提供二级子页跳转
- 结果：
  - 文档固定了银行主页、个人账户、转账服务、Exchange 公开页、个人流水五页结构
  - 明确 Exchange 储备余额与最近流水属于玩家公开透明内容，而不是仅供管理员查看
  - 实施方向改为“先做真实只读快照与嵌套菜单，再继续接正式写操作”

### 2026-03-30 - 修正终端页签只换标题不换正文的 ModularUI 用法错误

- 主题：修复终端切到银行页后右侧正文不切换、只有标题变化的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：原先正文区是在 `buildUI()` 时按当时的 `selectedPage` 一次性 `switch` 构建，后续虽然标题使用了动态文本，但正文没有进入框架的启用/禁用重布局链
- 结果：
  - 改为把所有页面挂进同一个正文容器
  - 每个页面容器使用 `setEnabledIf(...)` 绑定 `selectedPage`
  - 父级 `Flow` 开启 `collapseDisabledChild(true)`，让页签变化时正文区实际切换并重新布局

### 2026-03-31 - 修复终端打开即断线并记录两类联调阻塞根因

- 主题：收敛本地专用测试服最近两类核心阻塞：进服阶段的 `Fatally Missing blocks and items`，以及打开终端后的 `Disconnected from server`
- 影响范围：`src/main/java/com/jsirgalaxybase/GalaxyBase.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalNetwork.java`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：一方面 `ModularUI2` 的 dev 运行产物把测试映射带进了 Forge 注册表握手；另一方面 Forge 1.7.10 对自定义包通道名存在 20 字符上限，原终端通道名超长后会在服务端 `C17PacketCustomPayload` 解码阶段直接踢线
- 结果：
  - 在模组入口里忽略了 `modularui2:test_block` 与 `modularui2:test_item` 这类瞬时 dev 缺失映射，进服不再被这类测试映射阻塞
  - 把终端 `SimpleNetworkWrapper` 通道名从超长值收敛到 `jgb_terminal`
  - 确认这类终端断线修复后，客户端与服务端都必须重启，否则旧客户端仍会继续发送旧通道名
  - 把两类问题的根因与处理办法补进 PostgreSQL/联调文档，后续排障可以直接按文档核对

### 2026-03-31 - 明确银行终端“未启用基础设施”其实是服务端配置未打开

- 主题：把银行终端里的“未启用 PostgreSQL 基础设施”从模糊报错改成可操作的配置诊断
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankingService.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSnapshotProvider.java`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：本地专用测试服的 `run/server/config/jsirgalaxybase-server.cfg` 当时仍是默认占位状态，`bankingPostgresEnabled=false`、JDBC 地址仍指向 `db-host`，终端只能统一回显“当前世界未启用 PostgreSQL 银行基础设施”，信息不足以指导维护者下一步该改哪里
- 结果：
  - 银行终端现在会优先区分“服务端显式关闭银行 PostgreSQL”“JDBC 地址未配置”“用户名/密码未填”“初始化失败”几类状态
  - 银行快照页会提示优先检查 `jsirgalaxybase-server.cfg`、JDBC 配置与服务端启动日志
  - 文档补充了本地 `runServer` 联调至少要打开 `bankingPostgresEnabled` 并填好 JDBC 凭据这一前置条件

### 2026-04-01 - 银行第三轮收口：补真实 JDBC 验证、内部划转语义测试与开户复用规则

- 主题：把银行模块当前最大缺口从“服务层逻辑”收敛到“真实 PostgreSQL 路径验证”和“剩余语义定稿”
- 影响范围：`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`docs/banking-java-domain-draft.md`、`docs/banking-terminal-gui-design.md`、`docs/WORKLOG.md`
- 原因：第二轮已经修完 `request_id` 幂等重放、历史余额回放与语义冲突校验，但真实 JDBC 路径验证、`postInternalTransfer` 同等级补测，以及 `openAccount` 命中已有账户时的资料处理规则仍未完全收口
- 结果：
  - 新增真实 PostgreSQL 集成测试，基于独立测试 schema 验证 `saveIfOwnerAbsent`、`saveIfRequestAbsent`、`request_id` 历史余额重放、语义冲突拒绝与事务回滚不半提交
  - 为 `postInternalTransfer` 补齐历史余额回放、`amount`、`fromAccountId`、`toAccountId`、`businessType`、`operatorType`、`operatorRef`、`sourceServerId` 冲突测试
  - 明确 `openAccount(...)` 命中已有账户时保持既有 `display_name` 与 `metadata_json` 不刷新
  - 文档明确当前终端只承担开户、只读快照和玩家转账，任务书硬币兑换真实入口延期到市场阶段

### 2026-04-01 - 银行第四阶段收口：补工厂初始化验证、deduct 管理闭环与独立测试入口

- 主题：把银行一期在“非市场阶段”剩余的初始化链路、管理命令和测试执行入口真正收住
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankingInfrastructureFactory.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`build.gradle.kts`、`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：第三轮后还剩三个明显尾巴没有收口：工厂初始化校验仍写死 `public` schema、`transactionType` 语义矩阵缺显式补测、管理员命令只有 `grant` 没有对称扣减入口，也缺独立的集成测试执行命令
- 结果：
  - `JdbcBankingInfrastructureFactory` 改为按当前 JDBC 连接的 `search_path/currentSchema` 校验必需表，独立 schema 的 PostgreSQL 集成测试终于能真实覆盖工厂初始化路径
  - 新增工厂初始化成功/缺表失败两条真实 PostgreSQL 集成测试，并补上 `transactionType` 冲突测试
  - 新增 `./gradlew bankingIt` 与 `./gradlew banking-it` 两个银行集成测试入口，便于单独跑 PostgreSQL 路径
  - `/jsirgalaxybase bank` 管理命令补上 `deduct <player> <amount> [comment]`，与既有 `grant` 形成对称的管理员修正闭环
  - 明确幂等重放返回的是“历史 availableBalance + 当前非余额字段”的结果对象，而不是完整历史账户快照

### 2026-04-01 - 市场阶段前置分析：收口市场与银行的职责边界

- 主题：在进入市场实施前，把制度文档中的市场需求与现有银行能力做一次正式对齐，避免后续把市场和银行混成一层
- 影响范围：`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
- 原因：银行一期已经完成非市场阶段收口，但市场真正开做之前，必须先明确哪些能力可以直接复用银行，哪些最小能力仍需银行补齐，以及哪些责任必须留在市场模块
- 结果：
  - 在 `../Docs/市场经济推进.md` 中新增“市场阶段与银行系统边界结论”章节
  - 明确市场可直接复用现有银行账户、账本、兑换结算与系统划转能力
  - 明确银行仍需补齐的最小能力是：`冻结资金/解冻资金`、`税池账户`、`市场结算业务类型`

### 2026-04-01 - 市场阶段第一轮：接入真实任务书硬币兑换入口并补银行最小缺口

- 主题：按“真实兑换入口 + 银行最小缺口 + 市场骨架”收下市场阶段第一轮
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/`、`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangePlannerTest.java`、`docs/banking-java-domain-draft.md`、`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
- 原因：制度上已经明确市场阶段不应重做第二套货币底层，但现有代码还缺玩家可调用的真实兑换入口，以及挂单市场前必需的冻结余额、税池账户和市场结算语义边界
- 结果：
  - 新增 `/jsirgalaxybase market quote hand` 与 `/jsirgalaxybase market exchange hand`，先落地“手持一叠任务书硬币”的真实兑换入口
  - 银行应用服务补齐 `冻结 / 解冻 / 从冻结余额结算` 三个最小动作，并新增市场相关交易类型、业务类型与 `tax` 受管公共账户
  - 新增 `modules/core/market/` 首轮骨架，把任务书硬币识别、兑换规则和银行结算桥接从 banking 中拆到 market 侧
  - 当前实现明确为 `source-blind`：仅按 Dreamcraft coin 物品注册名识别，不在本轮尝试从物品本体反推“一次性任务 / 循环悬赏”来源
  - 补了银行冻结生命周期单测与市场任务书硬币规则单测，完整挂单订单簿、托管库存、撮合、CLAIMABLE/EXCEPTION 与崩服恢复仍留待下一轮
  - 明确订单簿、托管库存、撮合、内部操作日志、异常恢复属于市场模块自身，不应继续塞回银行模块

### 2026-04-01 - 市场阶段第二阶段：收口冻结回放语义并切入旧单一标准商品撮合方案最小骨架

- 主题：先把第一层金融底座补到可承载市场，再谨慎切入旧单一标准商品撮合方案
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/`、`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/`、`docs/banking-postgresql-ddl.sql`、`docs/market-postgresql-ddl.sql`、`docs/banking-java-domain-draft.md`、`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
- 原因：第一轮已经补出冻结/解冻/从冻结结算动作，但 replay 语义仍然把 `available` 和 `frozen` 混成“半历史半当前”；同时第二层旧单一商品撮合方案还没有真正的订单 / 托管 / 操作日志结构，仍停在空接口阶段
- 结果：
  - `TaskCoinExchangePlanner` 补齐 `IV = 10000`，并避免把未知更高罗马后缀误判成 `I` 或 `BASE`
  - `ledger_entry` 扩展为同时保存 `available` 与 `frozen` 的 before/after，`freeze/release/settleFrozenTransfer` 的 replay 现在能一致返回历史余额态
  - 补齐银行服务层 replay 单测，以及 PostgreSQL 下的 freeze/release/settle 成功、重放、冲突与回滚回归
  - 新增标准商品、市场订单、托管库存、内部操作日志、成交记录等领域对象与仓储接口，不再只是空接口占位
  - 新增旧单一商品撮合方案的最小应用服务，先支持 `创建卖单`、`撤销卖单` 与 `查看 OPEN 卖单` 读模型骨架，且明确卖单托管与 CLAIMABLE 返还路径
  - 当前仍未进入买单冻结闭环、真实撮合、税收结算、GUI、跨服消息和第三层定制化交易

  ### 2026-04-01 - 市场阶段第三阶段：补请求语义与恢复闭环并打通旧单一路线的最小买卖撮合

  - 主题：先修第二阶段一致性缺口，再把旧单一标准商品撮合方案推进到最小可运行买卖闭环
  - 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/`、`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/infrastructure/jdbc/MarketPostgresIntegrationTest.java`、`build.gradle.kts`、`docs/market-postgresql-ddl.sql`、`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
  - 原因：第二阶段虽然已经有卖单骨架，但 `requestId` 还没有完整请求语义校验，失败路径也缺少最小恢复闭环，同时第三阶段要求的买单冻结、同步撮合、CLAIMABLE 生成和真实 JDBC 市场持久化仍未接通
  - 结果：
    - 旧单一商品撮合服务补齐 `BUY_ORDER_CREATE / BUY_ORDER_CANCEL / MATCH_EXECUTION`，并把买单冻结、撤单释放、同步单商品限价撮合、税池入账、CLAIMABLE 生成与成交记录写入收口到 market application service
    - `MarketOperationLog` 现在按 `requestSemanticsKey` 校验完整请求语义，重复 `requestId` 不再只校验操作类型；卖单创建与撤单失败时会保留相关 `order / custody / trade` 线索，并进入 `RECOVERY_REQUIRED` 或 `FAILED`
    - 新增 `MarketRecoveryService`，可以扫描 `CREATED / PROCESSING / FAILED / RECOVERY_REQUIRED` 并把关联订单与托管库存收口到 `EXCEPTION`
    - 补齐市场 JDBC 基础设施：真实 PostgreSQL 仓储、`JdbcMarketInfrastructureFactory`、`marketIt / market-it` 任务，以及 PostgreSQL 下的卖单创建/撤单、请求语义冲突和恢复扫描回归
    - 为了让 market JDBC 能复用 banking 的同一连接基础设施，把 `AbstractJdbcRepository` 与 `JdbcConnectionCallback` 开放为可跨包复用的公共类型，保持市场与银行共享一条事务链
    - 补了单元测试覆盖：卖单/撤单 request 语义冲突、买单冻结与撤单释放、同步撮合后的成交记录与 CLAIMABLE 资产、恢复扫描收口
    - 已实际执行并通过：`./gradlew test`、`./gradlew bankingIt`、`./gradlew marketIt`

### 2026-04-04 - 将终端正文改为单页宿主

- 主题：继续修复终端打开后聊天栏持续刷 `ModularUI` Column resize 错误的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：终端正文区此前把 home、market、bank 各页同时挂在同一棵 `Flow.column()` 下，再通过 `setEnabledIf(...)` 切换可见性；在当前复杂布局下，隐藏页仍会形成重复的 `Column` 高度求解链，导致父级布局长期不收敛
- 结果：
  - 正文区改为 `SingleChildWidget` 单页宿主，只保留当前选中页的一棵 widget 子树
  - 页签切换时按 `selectedPage` 动态替换正文，移除多页常驻叠加带来的重复 `Column` 布局链

### 2026-04-25 - 产出 Phase 7 交接文档

- 主题：产出终端 GUI 迁移 Phase 7 交接文档，供后续先进 AI 执行 MARKET_CUSTOM 与 MARKET_EXCHANGE 迁移
- 影响范围：`docs/terminal-phase7-handover-to-chatgpt5.5.md`、`docs/WORKLOG.md`
- 原因：Phase 6 已完成标准商品市场迁移与滚动/布局/数据截断修复，Phase 7 需要迁移定制商品市场与汇率市场；为确保后续 AI 能清晰理解上下文、阶段定位、技术约束与验收标准，产出本交接文档
- 引用来源：`docs/terminal-betterquesting-ui-phase7-custom-exchange-residue-prompt-2026-04-19.md`、`docs/terminal-plan.md`、`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`
- 结果：产出完整交接文档，包含项目背景、阶段定位、技术架构要点、必须阅读的文档、Phase 7 执行 prompt 核心摘要、验收标准、已知风险与坑点、交接地清单
  - 已实际执行并通过：`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && /media/u24game/gtnh/JsirGalaxyBase/gradlew -p /media/u24game/gtnh/JsirGalaxyBase assemble --no-configuration-cache -PforceToolchainVersion=17`

### 2026-04-25 - Terminal BetterQuesting UI Phase 7：迁入定制商品市场与汇率市场

- 主题：把 MARKET_CUSTOM 与 MARKET_EXCHANGE 两张剩余真实业务页迁入新 `TerminalHomeScreen` 宿主，并收干新壳对旧市场装配残留的直接依赖
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/client/component/`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/`、`src/main/java/com/jsirgalaxybase/terminal/network/`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSectionService.java`、终端相关测试、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 6 后新壳已承接 BANK 与 MARKET_STANDARDIZED，phase 7 必须补齐 listing-first 的定制商品市场和 quote-first 的汇率市场，让 phase 8 只剩正式 cutover
- 结果：
  - 新增 `TerminalCustomMarketSection` / `TerminalCustomMarketSectionModel` / `TerminalCustomMarketSectionSnapshot`，承接 active / selling / pending listing 浏览、当前 listing 详情、资产摘要、购买 / 下架 / 领取确认与最近动作反馈
  - 新增 `TerminalExchangeMarketSection` / `TerminalExchangeMarketSectionModel` / `TerminalExchangeMarketSectionSnapshot`，承接兑换标的浏览、formal quote、pair / rule / limit 状态、刷新报价、确认兑换与动作反馈
  - 新增 custom / exchange 独立 action payload，并扩展 `TerminalActionType`、`TerminalActionMessage` 主链与 `TerminalSnapshotMessage` 序列化回写
  - `TerminalPopupFactory` 生命周期下已承接 custom 购买 / 下架 / 领取确认，以及 exchange 确认兑换；确认后继续走新 action -> snapshot 主链
  - `TerminalMarketSectionService` 侧新壳装配不再直接实例化旧 market / custom / exchange session controller，也不借旧 builder、sync binder 或旧 dialog；旧 ModularUI 代码继续保留给 phase 8 cutover 前过渡
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.terminal.TerminalServiceTest --tests com.jsirgalaxybase.terminal.TerminalMarketActionMessageFactoryTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.client.gui.framework.VerticalScrollPanelTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --tests com.jsirgalaxybase.terminal.ui.TerminalMarketServiceTest --tests com.jsirgalaxybase.terminal.ui.TerminalMarketSessionControllerTest --tests com.jsirgalaxybase.terminal.ui.TerminalCustomMarketSessionControllerTest --tests com.jsirgalaxybase.terminal.ui.TerminalExchangeMarketSessionControllerTest --no-configuration-cache -PforceToolchainVersion=17`
  - `./gradlew test --no-configuration-cache -PforceToolchainVersion=17` 当前仍有既存 PostgreSQL 银行集成测试 `BankingPostgresIntegrationTest.factoryCreateRejectsMissingTablesInCurrentSchema` 断言失败；终端定向测试均已通过

### 2026-04-25 - Terminal BetterQuesting UI Phase 8：正式入口 cutover 到新终端壳

- 主题：把 G 键与背包按钮的正式终端打开链收口到 `OpenTerminalRequestMessage -> OpenTerminalApprovedMessage -> TerminalHomeScreen`
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalNetwork.java`、`src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalOpenCutoverTest.java`、`docs/README.md`、`docs/terminal-plan.md`、`docs/WORKLOG.md`
- 原因：phase 7 已经让新 `TerminalHomeScreen` 承接 BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 全部正式业务页，phase 8 只需要把正式打开入口切到新壳并把旧 ModularUI 链降级为 legacy fallback
- 结果：
  - G 键与背包按钮正式入口保持默认发送 `OpenTerminalRequestMessage`，服务端通过 `TerminalService.approveTerminalClientScreen(...)` 生成初始 snapshot 与 session token，客户端经 `OpenTerminalApprovedMessage` 打开新 `TerminalHomeScreen`
  - 旧 `OpenTerminalMessage` 明确标注为 legacy ModularUI fallback，并改为调用 `TerminalService.openLegacyTerminal(...)`
  - `TerminalService.openTerminal(...)` 保留为 deprecated 兼容别名，不再作为正式入口语义；`TerminalModule` 日志也明确旧 `TerminalHomeGuiFactory` 只是 fallback factory
  - 新增 `TerminalOpenCutoverTest`，覆盖 approval -> `TerminalHomeScreenModel` 序列化、BANK / MARKET_STANDARDIZED / MARKET_CUSTOM / MARKET_EXCHANGE 新壳模型可打开、key/button 源码装配不再引用旧包、网络注册包含 open/action/snapshot 主链且 legacy packet 明确降级
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.terminal.TerminalOpenCutoverTest --tests com.jsirgalaxybase.terminal.TerminalServiceTest --tests com.jsirgalaxybase.terminal.TerminalMarketActionMessageFactoryTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.client.gui.framework.VerticalScrollPanelTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --tests com.jsirgalaxybase.terminal.ui.TerminalMarketServiceTest --tests com.jsirgalaxybase.terminal.ui.TerminalMarketSessionControllerTest --tests com.jsirgalaxybase.terminal.ui.TerminalCustomMarketSessionControllerTest --tests com.jsirgalaxybase.terminal.ui.TerminalExchangeMarketSessionControllerTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew assemble --no-configuration-cache -PforceToolchainVersion=17`

### 2026-04-25 - Terminal BetterQuesting UI Phase 9：删除旧 terminal ModularUI 过渡实现

- 主题：在 Phase 8 正式入口 cutover 后，删除 terminal 专属旧 ModularUI fallback、旧 GUI 装配层和旧同步/session/dialog 过渡件
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/network/`、`src/main/java/com/jsirgalaxybase/terminal/ui/`、`src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java`、终端相关测试、`docs/README.md`、`docs/terminal-plan.md`、`docs/WORKLOG.md`
- 原因：BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 已经全部由新 `TerminalHomeScreen` 主链承接；旧 `OpenTerminalMessage` / `TerminalHomeGuiFactory` fallback 已不再是正式运行部件，继续保留会增加误接线和回归风险
- 结果：
  - 删除旧 `OpenTerminalMessage`，`TerminalNetwork` 现在只注册新 request / approval / action / snapshot 四类消息
  - 删除 `TerminalService.openLegacyTerminal(...)` 与 deprecated `TerminalService.openTerminal(...)` 兼容别名，`TerminalService` 不再依赖旧 `TerminalHomeGuiFactory`
  - 删除 `TerminalModule` 中 terminal 专属 `GuiManager.registerFactory(...)` 注册和 legacy fallback 日志
  - 删除旧 terminal ModularUI 装配件：`TerminalHomeGuiFactory`、银行/市场 page builder、旧 dialog / widget factory、旧 sync binder / sync state、旧 bank / market / custom / exchange session controller，以及对应旧测试
  - 保留新链仍使用的共享类：`TerminalBankingService`、`TerminalBankSnapshotProvider`、`TerminalHomeSnapshotProvider`、`TerminalMarketService`、`TerminalMarketSectionService`、`TerminalMarketSnapshot`、`TerminalCustomMarketSnapshot`、`TerminalExchangeMarketSnapshot`、`TerminalExchangeQuoteView`、通知与 `TerminalPage`
  - `TerminalMarketService` 已切断对旧 session controller 常量/接口的依赖，custom scope 与 exchange target 由 service / payload 层自身表达
  - 旧入口引用扫描结果：`rg -n "TerminalHomeGuiFactory|OpenTerminalMessage|openLegacyTerminal|openTerminal\\(|TerminalBankPageBuilder|TerminalMarketPageBuilder|TerminalDialogFactory|PanelSyncManager|Terminal[A-Za-z]*SyncBinder|Terminal[A-Za-z]*SessionController|GuiManager\\.registerFactory" src/main/java src/test/java` 无命中
  - terminal 侧 ModularUI 引用扫描结果：`rg -n "com\\.cleanroommc\\.modularui|modularui" src/main/java/com/jsirgalaxybase/terminal src/test/java/com/jsirgalaxybase/terminal` 无命中
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.terminal.TerminalOpenCutoverTest --tests com.jsirgalaxybase.terminal.TerminalServiceTest --tests com.jsirgalaxybase.terminal.TerminalMarketActionMessageFactoryTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.client.gui.framework.VerticalScrollPanelTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --tests com.jsirgalaxybase.terminal.ui.TerminalMarketServiceTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew assemble --no-configuration-cache -PforceToolchainVersion=17`

### 2026-04-25 - Terminal BetterQuesting UI Phase 10：修复新壳缩放、滚动与裁剪

- 主题：只修新 `TerminalHomeScreen` 壳层在高 GUI Scale / 小 GUI 坐标空间下的尺寸、滚动和内容裁剪问题，不改银行/市场业务语义
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeLayout.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、终端壳层布局测试、`docs/WORKLOG.md`
- 原因：实测截图显示新壳在较大 GUI Scale 下仍按硬最小尺寸撑开，导致顶部/底部被挤压；左侧导航与 HOME/CAREER/PUBLIC_SERVICE 等普通 section 页没有局部滚动，section 与通知在内容超出时会越界或重叠
- 结果：
  - 新增 `TerminalHomeLayout`，把主 panel 尺寸改为基于当前 `GuiScreen.width/height` 的安全边距计算；小屏/高 GUI Scale 下不再用 468x320 硬最小值强行撑开
  - `TerminalHomeScreen` 使用新的 layout 结果装配 surface、status band、navigation rail 与 body bounds
  - `TerminalShellPanels.createNavigationRail(...)` 将 nav item 列表放入 `VerticalScrollPanel`，标题固定，导航项超出 rail 时通过滚轮浏览并由 scroll panel scissor 裁剪
  - 普通 section 页正文改为 `VerticalScrollPanel`：section 卡片与通知卡片进入可滚动内容区，底部 session / 刷新 / 关闭按钮继续固定在 body 底部
  - section 均分高度 helper 先扣除 gap 再计算高度，避免 `count * height + gaps` 超过可用高度；通知不再固定裁断前 2 条，而是全部进入滚动区
  - BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 专用业务 section 未改业务语义，继续沿用各自页面内部布局与滚动策略
  - 新增 `TerminalHomeScreenLayoutTest` 与 `TerminalShellPanelsScrollTest`，覆盖小屏 panel 不越安全边距、导航 rail 使用滚动容器、普通 section/notification 使用滚动容器、gap 计算不溢出
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.terminal.client.screen.TerminalHomeScreenLayoutTest --tests com.jsirgalaxybase.terminal.client.component.TerminalShellPanelsScrollTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.client.gui.framework.VerticalScrollPanelTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.terminal.TerminalOpenCutoverTest --tests com.jsirgalaxybase.terminal.TerminalServiceTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew assemble --no-configuration-cache -PforceToolchainVersion=17`
  - 本地体验环境：检测到已有 `runServer` / `runClient` 进程在运行，未杀用户进程；`ss -ltnp | grep 25100` 显示 `127.0.0.1:25100` 已监听，server 日志有 `Done`、banking prepared 与 `Market runtime prepared`，client 日志有 `Registered terminal client entry handlers` 且已连接 `127.0.0.1:25100`
