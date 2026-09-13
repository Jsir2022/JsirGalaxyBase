# JsirGalaxyBase Docs

这里放的是 `JsirGalaxyBase` 的基础说明和持续记录，方便玩家、协作者和后续维护者快速理解这个项目。

## 当前最建议先看的内容

- `betterquesting-capability-absorption-matrix-2026-09-13.md`
  - BetterQuesting 替代工作的唯一权威进度表；区分已完成、部分完成和未完成，并明确当前稳定检查点不代表
    正式替代、生产迁移或切换已经完成
- `galaxy-ui2-pure-java-visual-validation-2026-09-08.md`
  - UI2 正式页面的纯 Java 视觉正确性闭环：明确自动与人工验收边界、三档截图/结构报告、Mod F9 调试截图，
    以及逐页下沉生产 Document 的阶段路线
- `victoria3-economy-mod-research-2026-09-07.md`
  - ASE 与 Victoria 3 Economic and Financial Mod 的固定源码来源、许可证边界和第一轮经济机制分析；提出适用于
    GTNH 的库存覆盖率、公共储备、经济解释指标及先离线回放再 SHADOW 的实施路线
- `galaxy-ui2-terminal-personalization-and-migration-2026-09-06.md`
  - UI2 从首页与资产中心走向完整终端的当前执行计划；记录文字溢出根因、三套客户端主题、窗口/密度设置、
    Resource Pack 主题合同，以及轻量页面、市场、地产和旧 Canvas 删除顺序
- `galaxy-ui2-production-migration-roadmap-2026-09-05.md`
  - UI2 从资产中心开始的正式迁移状态表；C.0/C.1/C.2 自动验收已完成，当前等待资产中心实机操作门槛，
    通过后才依次迁移首页、通知、银行、ServerTools、市场与地产并最终删除旧 Canvas
- `galaxy-ui2-qz-derived-integration-plan-2026-09-05.md`
  - UI2 从 Qz 1.8.2、4.1.3-LTS、4.7 和 4.8 剪裁成熟算法的正式边界；记录固定提交、许可证、
    圆角/裁剪/字体 generation/Modal 加固方案、自包含门禁与人工验收条件
- `galaxy-ui-2-modern-java-framework.md`
  - 下一代终端 UI 的正式架构决策：以 Vue 的组件、状态和单向数据流思想用 Java 重建现代 UI2，明确 Minecraft/Forge
    底层边界、双宿主、字体/渲染/布局/输入设计，以及完成迁移后删除旧 Panel 框架的顺序
- `galaxy-ui-2-vue-reference-map.md`
  - Vue 官方英文/中文文档的本地固定版本说明与概念映射；明确哪些组件、状态、生命周期和 Key 思想进入 Java UI2，
    哪些 DOM、CSS、Proxy、模板编译器和浏览器机制不得机械照搬
- `galaxy-ui-2-rebuild-execution-spec.md`
  - UI2 的可执行重构规格；定义纯 Java 8 内核、Java2D 独立 Demo、Minecraft 双宿主适配、与旧 Canvas 的差异、
    自动化边界和从资产中心开始的分阶段替换门槛；Batch A 独立内核与 Batch B Minecraft 双宿主 UI Lab 已完成
- `ae2-galactic-warehouse-terminal-bay-v2.md`
  - 当前个人 AE2 银河仓储的正式方向：终端托管的一格真实 Storage Cell Bay 与 Base Vault 统一资产中心；
    不需要实体 Drive、不建立外部 ME 网络，Cell 的容量和内容限制仍完全由 AE2 负责
- `ae2-galactic-warehouse-drive-v1.md`
  - 已部署的实体 Drive 兼容批次；保留登记/审计历史，已被 v2 终端 Bay 作为后续主入口取代

- `serverutilities-land-property-integration-v1.md`
  - 修正“传送能力已落地不等于 ServerUtilities 已完整集成”，定义 JGB 原生领地保护、一次性旧数据迁移、独立 Mod 卸载，以及地皮、Team、职业、银行和地产市场的制度关系
- `serverutilities-personal-land-code-reuse-evaluation-v1.md`
  - 当前个人优先版本的代码级评估；列出 SU 可复制、需重构和不应搬入的类，以及个人挂牌、银行结算、PROPERTY 页面和旧 team 数据 staging 边界
- `personal-land-postgresql-runtime-v1.md`
  - 个人地皮 PostgreSQL 真源、幂等事务、服务端配置和 SHADOW/ENFORCE 保护边界
- `terminal-number-display-standard-v1.md`
  - 终端数字显示的语义化规范；定义盘口/图表三位有效数字的 `K -> M -> G -> T` 紧凑数量，以及银行、价格、订单和确认流程必须保留的精确千分位
- `standardized-market-self-match-bug-2026-08-18.md`
  - 标准市场自成交候选未绕过的 P0 BUG 记录；包含实机复现、银行同账户结算异常证据、正确的自动跳过规则和修复验收用例
- `modern-trading-terminal-visual-gap-baseline-2026-08-08.md`
  - 以最新 `/home/u24/图片/市场第三版.png` 为唯一视觉目标，对比当前浏览、Hover、详情三张实机图，逐项记录 UI、行情数据、交易票据与账户仓自动交割差距及最终验收清单
- `modern-trading-terminal-redesign-v1.md`
  - 标准商品市场向现代证券交易终端演进的产品与技术方向，定义四列行情浏览、只读 Hover、图表/盘口/订单票据和账户仓自动预留/交付模型
- `current-product-direction-and-gap-review-2026-05-18.md`
  - 当前产品方向、进度判断、剩余需求清单与下一阶段建议；优先用于判断 terminal、market、ServerTools 后续工作顺序
- `market-terminal-phased-execution-plan-2026-06-14.md`
  - 市场终端后续实施的正式阶段计划；明确为什么不能按四页机械平摊推进，而应先做共享市场壳与标准商品市场工作台
- `market-terminal-child-pages-defect-plan-2026-06-20.md`
  - 当前市场终端子页面实机缺陷、后端数据缺口与三轮整改计划；用于后续对照效果图推进 MARKET_STANDARDIZED / MARKET_CUSTOM / MARKET_EXCHANGE 收口
- `market-warehouse-v1-product-boundary-draft.md`
  - 市场仓库 v1 的制度边界草案；重点定义“仓库首先是资产状态系统”，并区分市场托管仓、企业私仓与公共仓
- `warehouse-transfer-and-audit-boundary-v1.md`
  - 四类库存之间允许的转移矩阵、幂等审计合同及物理物品中断恢复原则；企业仓和公共仓实现前的硬约束
- `terminal-servertools-page-v1-acceptance-review-2026-06-06.md`
  - 群组服 / ServerTools 终端页 v1 的验收判断和收口记录；当前结论是页面、recent transfer ticket 状态展示和默认 facade 后端测试都已接入，剩余工作主要是游戏内验收
- `terminal-ui-redesign-toward-concept-mockup-2026-06-07.md`
  - 终端朝效果图方向改造的整体设计、分阶段计划和逐项对图差异记录；确认继续走 BetterQuesting 风格游戏内 GUI 路线，并明确 `传送 / 群组服` 不是做 hover 细修，而是要重做成专用的三栏传送工具页

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
- ServerTools 群组服传送页 v1 已出现在新壳中：`SERVER_TOOLS` 顶层页已接入导航、snapshot、网络序列化、客户端 section、warp 选择与确认弹窗，确认 warp 复用 `PlayerTeleportService.prepareWarpTeleport(...)` 与 `ServerToolsModule.dispatchTeleport(...)`；recent transfer ticket 状态展示和默认 facade 后端调用测试也已补齐，后续重点转为游戏内验收与局部密度微调。

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
- `servertools-phase3-gray-rollout-status-2026-05-17.md`
  - server tools / cluster 第三阶段灰度准备实操记录；当前真实运行链为 Docker + supervisor，Entrance / Lobby / S2 已部署当前 JsirGalaxyBase jar、完成数据库前置并启动，不触碰 S1
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
- `market-warehouse-v1-product-boundary-draft.md`
  - 市场仓库 v1 的产品边界草案，强调标准商品市场托管仓先行，并预留企业私仓 / 公共仓的制度分层
- `warehouse-transfer-and-audit-boundary-v1.md`
  - 市场托管仓、玩家背包、企业仓与公共仓的转移/审计合同，规定未来跨域库存实现的边界
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
- `market-terminal-phased-execution-plan-2026-06-14.md`
  - 市场终端后续阶段计划，定义 `共享市场壳 -> 标准商品市场 -> 定制商品市场 -> 汇率市场 -> 统一收口` 的执行顺序与验收目标
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

## 职业、贡献与跨服任务平台

- `galaxy-career-contribution-cross-server-quest-platform-2026-09-08.md`
  - BetterQuesting 3.7.15-GTNH 的吸收边界、PostgreSQL 权威模型、职业/贡献/公共任务分层及 Q.0-Q.8 实施路线
# Galaxy UI 2 正式迁移

- `galaxy-ui2-production-migration-roadmap-2026-09-05.md`：从生产组件、资产中心纵向迁移到旧 Canvas 删除的阶段状态与人工闸门。
# BetterQuesting 能力吸收

- [BetterQuesting 关键能力吸收矩阵与实施路线](betterquesting-capability-absorption-matrix-2026-09-13.md)
