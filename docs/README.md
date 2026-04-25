# JsirGalaxyBase Docs

这里放的是 `JsirGalaxyBase` 的基础说明和持续记录，方便玩家、协作者和后续维护者快速理解这个项目。

## 当前最建议先看的内容

## 当前终端 GUI 迁移进度

- BetterQuesting 风格 phase 1 framework 已经落到仓库自有命名空间。
- BetterQuesting 风格 phase 2 打开链已经切到“客户端请求、服务端授权、客户端开 `TerminalHomeScreen` 占位根屏”，旧 `ModularUI` terminal 业务页仍保留为过渡实现。
- BetterQuesting 风格 phase 3 已把 `TerminalHomeScreen` 推进为首页壳，当前已经具备顶部状态带、左侧导航、首页摘要区、通知宿主和 popup 宿主；旧银行页与市场页仍刻意保留在 `ModularUI` 过渡实现中。
- BetterQuesting 风格 phase 3 严格验收收口已把首页壳当前页语义收口到 `selectedPageId` 单一真源；导航高亮现在只作为派生结果，不代表第二套独立状态，也没有提前进入 phase 4 的 section 宿主切换。
- BetterQuesting 风格 phase 4 已把首页壳推进为真实的 section 宿主：主体区现在按 `selectedPageId` 映射到顶层 section snapshot，客户端已具备最小 `TerminalActionMessage` / `TerminalSnapshotMessage` 刷新闭环，但银行页与市场页业务内容仍未迁入新壳。
- BetterQuesting 风格 phase 5 已把银行页迁成新壳上的第一张完整业务页：`TerminalBankSection` 现在直接挂在 `TerminalHomeScreen` 宿主上，已接通开户、刷新、转账确认 popup 和 action -> snapshot 回写闭环；市场页仍明确留给 phase 6。
- BetterQuesting 风格 phase 6 已把 MARKET 根页和 MARKET_STANDARDIZED 迁入新壳：MARKET 根页现在承接共享摘要与入口卡，MARKET_STANDARDIZED 作为第二张完整业务页承接真实标准商品买单 / claim 主链；定制商品市场、汇率市场、正式 cutover 与旧 ModularUI 删除继续留给 phase 7 到 phase 9。
- BetterQuesting 风格 phase 6 严格验收收口已补上最小可复用滚动能力：`CanvasScreen` 现在会转发滚轮输入，framework 已新增局部 `VerticalScrollPanel`，`TerminalHomeScreen` 放宽了主体尺寸策略，`TerminalMarketSection` 也已收掉商品 / claim / 规则 / 盘口等关键数据的固定上限；这轮只修 phase 6 可用性缺口，不提前进入 phase 7 的 custom / exchange 迁移。
- BetterQuesting 风格 phase 7 已把 MARKET_CUSTOM 与 MARKET_EXCHANGE 迁入新壳：`TerminalCustomMarketSection` 承接 listing-first 浏览、详情、购买 / 下架 / 领取确认与 snapshot 回写，`TerminalExchangeMarketSection` 承接 quote-first 标的、规则、刷新报价与确认兑换；新壳侧已不再直接依赖旧 market builder / sync binder / dialog / session controller，旧 ModularUI terminal 仅作为 phase 8 cutover 前的过渡实现保留。
- BetterQuesting 风格 phase 8 已把正式终端打开链 cutover 到新 `TerminalHomeScreen`：G 键与背包按钮默认发送 `OpenTerminalRequestMessage`，服务端经 `TerminalService.approveTerminalClientScreen(...)` 生成初始 snapshot 与 session token，再由 `OpenTerminalApprovedMessage` 在客户端打开新壳。
- BetterQuesting 风格 phase 9 已删除旧 terminal ModularUI 过渡实现：旧 fallback packet、旧 GUI factory 注册、旧 terminal builder / binder / sync state / session controller / dialog 均已移除；BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 继续由新 `TerminalHomeScreen` 主链承接。

- `../README.md`
  - 项目定位、代码结构、开发原则，以及 `Reference/ServerUtilities` 的后续整合边界
- `WORKLOG.md`
  - 开发工作记录
- `serverutilities-integration-mapping.md`
  - `Reference/ServerUtilities` 的逐项整合映射表，明确哪些能力可直接复用、需抽取后复用、建议重写或暂不整合
- `servertools-phase1-requirements.md`
  - 已确认的第一批 server tools / cluster 需求，明确 `home` / `back` / `spawn` / `tpa` / `rtp` / `warp` 必须实现、需要跨服能力、直接接数据库且暂不做 GUI
- `servertools-phase1-execution-prompt-2026-04-11.md`
  - 可直接交给另一个 AI 的第一期执行 prompt，明确本轮必须实现的命令范围、跨服与数据库硬约束、禁止引入旧框架和不兼容旧命令格式的要求
- `servertools-phase1-acceptance-close-prompt-2026-04-11.md`
  - 第一批 server tools / cluster 严格验收后的收口 prompt，聚焦修复 transfer ticket 状态持久化不完整、TPA 目标服未校验和禁止继续扩大无关改动范围
- `servertools-phase2-cross-server-gateway-prompt-2026-04-11.md`
  - server tools / cluster 第二阶段第一优先级执行 prompt，聚焦真实跨服网关接线、transfer ticket 生命周期闭环和目标服落点恢复
- `servertools-phase2-cluster-close-prompt-2026-04-12.md`
  - server tools / cluster 第二阶段严格验收后的收口 prompt，聚焦修复 ticket 过期消息写回错误和重复 requestId 的终态幂等语义错误
- `servertools-phase3-mcsm-gray-rollout-prompt-2026-04-12.md`
  - server tools / cluster 下一阶段的 MCSM 灰度联调准备执行 prompt，聚焦只用代理 / 大厅 / S2 搭出不影响在线 S1 的可启动、可观察、可继续联调灰度链
- `servertools-phase1-command-reference.md`
  - 第一批 server tools / cluster 命令格式、当前跨服边界、数据库表落点与网关预留说明
- `../Reference/ServerUtilities/README.md`
  - 外部参考源码入口；当前只作为工具能力拆解参考，不是 `JsirGalaxyBase` 的直接架构模板
- `terminal-plan.md`
  - 终端入口与终端壳总方案；GUI 内核迁移只保留路线结论，详细集成设计见独立方案文档
- `terminal-betterquesting-ui-integration-plan-2026-04-14.md`
  - 终端从 `ModularUI 2` 迁往内置 BetterQuesting 风格 GUI 框架的独立集成方案，覆盖 vendoring 范围、包结构、协议重构、页面装配和分阶段实施顺序
- `terminal-betterquesting-ui-phase1-framework-prompt-2026-04-14.md`
  - 可直接交给另一个 AI 的第一阶段执行 prompt，只做 BetterQuesting 风格 UI framework 落地、最小主题骨架和占位 screen 验证，不提前做 terminal 页面迁移
- `terminal-betterquesting-ui-phase2-open-chain-prompt-2026-04-14.md`
  - 可直接交给另一个 AI 的第二阶段执行 prompt，只做终端打开链改造成“服务端授权 + 客户端开屏”的最小闭环，并把 phase 1 framework 真正接进 terminal 主链
- `terminal-betterquesting-ui-phase3-home-shell-prompt-2026-04-15.md`
  - 可直接交给另一个 AI 的第三阶段执行 prompt，只做新 `TerminalHomeScreen` 首页壳、左侧导航、全局通知 / popup 宿主和共用 panel 组件层，不提前迁银行页和市场页
- `terminal-betterquesting-ui-phase3-close-prompt-2026-04-15.md`
  - 第三阶段严格验收后的收口 prompt，只修首页壳 `selectedPageId` 与导航选中态的单一真源问题，不提前进入 phase 4 section 路由和动作协议
- `terminal-betterquesting-ui-phase4-section-host-prompt-2026-04-18.md`
  - 第四阶段执行 prompt，先把首页壳推进成真实的 section 宿主，并补最小 action / snapshot 协议落点，再进入银行页作为第一张完整业务页的迁移
- `terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md`
  - 第五阶段执行 prompt，把银行页作为新终端壳上的第一张完整业务页迁入，并以“从当前起再压 5 个阶段内删除终端旧 ModularUI 实现”为节奏约束后续阶段
- `terminal-betterquesting-ui-phase6-market-overview-standardized-prompt-2026-04-19.md`
  - 第六阶段执行 prompt，只迁 MARKET 总入口与标准商品市场主链到新终端壳，明确把定制商品市场、汇率市场、cutover 与旧 ModularUI 删除留给 phase 7 到 phase 9
- `terminal-betterquesting-ui-phase6-close-prompt-2026-04-19.md`
  - 第六阶段严格验收后的收口 prompt，只修新壳滚动能力、标准商品市场布局密度和关键数据截断问题，不提前进入 phase 7 的 custom / exchange 迁移
- `terminal-betterquesting-ui-phase7-custom-exchange-residue-prompt-2026-04-19.md`
  - 第七阶段执行 prompt，把定制商品市场与汇率市场完整迁入新终端壳，并收干新壳对旧 terminal 市场装配的直接依赖残留，为 phase 8 cutover 做准备
- `terminal-phase7-handover-to-chatgpt5.5.md`
  - Phase 7 交接文档，供后续先进 AI（ChatGPT 5.5 或更新）执行 MARKET_CUSTOM 与 MARKET_EXCHANGE 迁移，包含项目背景、阶段定位、技术架构要点、验收标准与已知风险
- `terminal-gui-regression-chain-2026-04-05.md`
  - 2026-04-04 到 2026-04-05 终端 GUI 回归链统一事故文档，覆盖银行 GUI、转账、按钮门禁、sync、HUD 泡泡与 DevB 开户崩溃
- `terminal-gui-continue-current-implementation-prompt-2026-04-06.md`
  - 终端 GUI 持续实装 prompt，明确不再保留 fallback GUI，而是继续在当前 ModularUI 实现上打磨原先预期效果
- `terminal-bank-market-strict-close-prompt-2026-04-11.md`
  - terminal bank / market 严格收口 prompt，聚焦移除文本框 auto/manual sync 混链、补 terminal open 装配链回归测试，以及延续 WORKLOG 事故记录
- `terminal-bank-market-risk-fix-prompt-2026-04-12.md`
  - terminal bank / market 审查后续修复 prompt，聚焦补齐 market/custom 动作后的银行快照失效链，以及清理旧 market 挂载残留里的 custom panel 错接线风险
- `banking-system-requirements.md`
  - 银行系统一期需求与边界
- `banking-schema-design.md`
  - 银行系统一期数据表与事务边界设计
- `market-three-part-architecture.md`
  - 市场三分结构的正式边界、旧实现归位判断与下一阶段执行顺序
- `standardized-market-catalog-boundary.md`
  - 标准商品市场正式目录版本、准入边界与目录来源分层说明
- `standardized-market-catalog-boundary-prompt-2026-04-03.md`
  - 标准商品市场商品目录与正式准入边界的下一阶段实现 prompt
- `standardized-market-catalog-boundary-tail-close-prompt-2026-04-03.md`
  - 标准商品市场目录边界阶段的收口 prompt，聚焦命令层、终端层、服务层共用同一运行时目录边界
- `custom-market-minimal-listing-chain-prompt-2026-04-03.md`
  - 定制商品市场最小挂牌链的分阶段实现 prompt，聚焦挂牌、浏览、购买、下架、待领取与交付留痕
- `custom-market-minimal-listing-chain-tail-close-prompt-2026-04-04.md`
  - 定制商品市场最小挂牌链阶段的收口 prompt，聚焦 pending 闭环、单件商品边界一致性与市场 JDBC fail-fast schema 校验
- `market-total-entry-split-prompt-2026-04-04.md`
  - MARKET 总入口拆分阶段 prompt，聚焦终端入口、命令帮助与路由层按标准商品市场 / 定制商品市场 / 汇率市场三类入口正式收口
- `market-total-entry-split-tail-close-prompt-2026-04-04.md`
  - MARKET 总入口拆分阶段的收口 prompt，聚焦真实路由回归测试与 docs 索引补齐
- `market-entry-overview.md`
  - MARKET 总入口三分后的落地说明，明确总入口只做三类市场入口与共享摘要，不再承担混合交易详情页
- `market-terminal-asset-first-refactor-evaluation-2026-04-05.md`
  - 市场终端下一轮重构评估，明确 asset-first 导航方向为何成立，以及哪些市场语义可以统一、哪些不能硬统一
- `market-terminal-asset-first-refactor-prompt-2026-04-05.md`
  - 市场终端 asset-first 重构执行 prompt，聚焦共享导航壳、标准商品商品优先终端、定制商品 listing-first GUI 与汇率市场标的优先详情页
- `custom-market-minimal-model.md`
  - 定制商品市场最小挂牌链 v1 的正式对象、快照保存方式和状态机说明
- `banking-postgresql-ddl.sql`
  - 银行系统一期 PostgreSQL DDL 草案
- `market-postgresql-ddl.sql`
  - 市场系统第二阶段最小骨架 DDL 草案，现已覆盖标准商品市场与定制商品市场最小挂牌链
- `servertools-cluster-postgresql-ddl.sql`
  - server tools / cluster 第一期 PostgreSQL DDL，覆盖 server directory、transfer ticket、homes、back、warp、tpa 与 rtp 记录
- `banking-java-domain-draft.md`
  - 银行系统 Java 领域模型与仓储接口草案
- `banking-terminal-gui-design.md`
  - 银行终端页面信息架构与嵌套菜单设计
- `postgresql-local-setup-and-migration.md`
  - Ubuntu 本地 PostgreSQL 安装、初始化与迁移说明
- `postgresql-backup-and-restore.md`
  - PostgreSQL 逻辑备份、systemd 定时备份与恢复演练说明
- `postgresql-schema-migrations.md`
  - PostgreSQL 版本化迁移、运维升级入口与 fail-fast schema 校验说明

## 这个项目现在是什么

`JsirGalaxyBase` 当前是一个面向 GTNH 服务器的自定义制度模组。

它的核心目标包括：

- 职业
- 市场与经济
- 贡献度 / 声望
- 公共订单 / 公共工程
- 群组服同步核心状态
- 后续玩法能力扩展

## 这个项目现在不是什么

- 不是普通客户端整合包说明文档
- 不是 Bukkit 插件项目
- 不是多仓库拆分项目
- 不是已经完成的正式玩法发布包

当前更准确的理解是：

- 一个还在持续设计和落地中的 GTNH 自定义模组仓库

## 文档说明

- 面向开发和协作的正式入口以 `../README.md` 为准
- 持续变更记录以 `WORKLOG.md` 为准
- `Reference/ServerUtilities` 只作为参考源码区使用；如果后续吸收其中能力，应以 `../README.md` 中的整合边界为准
- 服务器制度细节和更大范围的设计约束仍以工作区 `../Docs/` 下的文档为长期参考
