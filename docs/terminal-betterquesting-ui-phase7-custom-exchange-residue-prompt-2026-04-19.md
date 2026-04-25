# Terminal BetterQuesting 风格 UI 框架第七阶段执行 Prompt

日期：2026-04-19

下面这份 prompt 可直接交给另一个 AI，用于在 JsirGalaxyBase 中落地终端 BetterQuesting 风格 UI 迁移的第七阶段。

注意：

- 当前仓库已经完成 phase 1 到 phase 6：framework 地基、打开链、首页壳、section 宿主与最小 action / snapshot 协议、银行页完整迁移、MARKET 总入口与标准商品市场迁移，以及 phase 6 严格验收后的滚动/布局/数据截断收口。
- 当前新壳已经能稳定承接 BANK 与 MARKET_STANDARDIZED 两张完整业务页，并已具备可复用的局部滚动能力。
- 后续阶段顺序已经明确，不允许在这轮重新发散评估：

1. phase 7：定制商品市场与汇率市场迁移，并收干旧 terminal 装配残留
2. phase 8：新终端壳正式 cutover
3. phase 9：删除旧 terminal ModularUI 实现

这份 prompt 只负责 phase 7，不提前做 phase 8 到 phase 9 的工作。

---

你正在 JsirGalaxyBase 仓库中工作。请直接修改当前 terminal client screen / component / viewmodel / network / service 相关实现，把剩余 terminal 真实业务页全部迁进新 TerminalHomeScreen 宿主，并把旧 terminal 装配残留收干到“只剩 cutover 所需过渡外壳”的状态。

## 本轮唯一目标

在不做正式 cutover、不删除旧 terminal ModularUI 实现的前提下，把 MARKET_CUSTOM 与 MARKET_EXCHANGE 两类真实业务页完整迁入新 TerminalHomeScreen 宿主，并收干新壳对旧 terminal 市场装配的直接依赖残留。

本轮做完后，新 terminal 应至少具备：

- MARKET_CUSTOM 真实业务页
- MARKET_EXCHANGE 真实业务页
- custom / exchange 的真实 action -> snapshot 回写闭环
- 至少各一条 custom / exchange 的确认语义迁到新 popup 生命周期
- 新 terminal 对旧 market builder / sync binder / dialog / session controller 的直接依赖全部移除

重点是：

- phase 7 结束时，新壳必须已经承接 BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 全部正式业务页
- 旧 terminal 可以继续存在，但不再承载任何必须保留的正式业务交互
- 本轮结束后，phase 8 应只剩正式 cutover，而不该再补业务页迁移

## 开始前必须先读

开始编码前，先完整阅读并遵守下面这些文档：

- README.md
- docs/terminal-plan.md
- docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md
- docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md
- docs/terminal-betterquesting-ui-phase6-market-overview-standardized-prompt-2026-04-19.md
- docs/terminal-betterquesting-ui-phase6-close-prompt-2026-04-19.md
- docs/market-three-part-architecture.md
- docs/market-entry-overview.md
- docs/custom-market-minimal-model.md
- docs/WORKLOG.md

必须先看当前新终端主链代码：

- src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalSectionRouter.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPopupFactory.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionState.java
- src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java
- src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalMarketSectionModel.java
- src/main/java/com/jsirgalaxybase/terminal/network/TerminalActionMessage.java
- src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java
- src/main/java/com/jsirgalaxybase/terminal/TerminalService.java
- src/main/java/com/jsirgalaxybase/terminal/TerminalActionType.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java

必须先看当前旧 terminal 市场实现，确认 phase 7 迁移的是业务语义而不是把旧 UI 容器重新接回来：

- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketRoutePlan.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSessionController.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncState.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSnapshot.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeQuoteView.java

如需追业务真实边界，也必须先看：

- src/main/java/com/jsirgalaxybase/modules/core/market/**

## 本轮必须完成的内容

### 1. 把 MARKET_CUSTOM 顶层 page 升级成真实 CustomMarketSection

本轮必须把定制商品市场迁成真实 section，而不是继续停留在旧 terminal 页或说明占位内容。

至少具备：

- listing-first 浏览层
- 当前 listing 摘要或详情层
- 玩家当前可操作动作区
- 待领取 / claim / 个人挂单或等价资产摘要
- 最近动作反馈展示

要求：

- UI 层必须基于当前 BetterQuesting 风格 framework 与 terminal client component 层重建
- 不允许直接把旧 TerminalMarketPageBuilder 的 custom panel 嵌回新壳
- 不允许把定制商品市场硬扳成 standardized market 的商品优先终端
- 继续保持 listing-first 节奏，不回退到“全字段摊平式大详情页”

### 2. 把 MARKET_EXCHANGE 顶层 page 升级成真实 ExchangeMarketSection

本轮必须把汇率市场迁成真实 section，而不是继续依赖旧 terminal exchange 页。

至少具备：

- 标的或对象浏览入口
- 当前 quote / pair / rule / 状态摘要
- 刷新报价或等价浏览动作
- 至少一条真实确认兑换动作
- 最近动作反馈展示

要求：

- UI 层必须基于当前 BetterQuesting 风格 framework 与 terminal client component 层重建
- 不允许直接把旧 TerminalExchangeQuoteView 或旧 exchange panel 回嵌到新壳
- 保持汇率市场“标的优先 / quote 优先”的节奏，不要被 standardized 或 custom 的布局套路带偏

### 3. 为 custom / exchange 引入独立 section model 与 snapshot

本轮不能把 phase 7 简化成“继续往 TerminalMarketSectionModel 塞更多字符串字段”。

至少需要为 custom / exchange 建立清晰的模型边界，建议包括：

- custom market 顶层 model / snapshot
- listing 浏览 model
- listing 详情 / 资产摘要 model
- exchange market 顶层 model / snapshot
- quote / pair / rule / 状态摘要 model
- 动作反馈 model

要求：

- standardized、custom、exchange 的模型边界必须清晰，不搞“全市场统一超级模型”
- 允许抽共享小组件模型，但不能把三个业务页重新捏回统一 MARKET 巨石状态
- 新模型必须能自然经由 TerminalSnapshotMessage 回写

### 4. 把 custom / exchange 的真实动作正式接到 TerminalActionMessage 主链

本轮至少要把下面这些动作接到现有 action 协议上：

- 刷新 MARKET_CUSTOM 或 MARKET_EXCHANGE 页
- custom market 至少一类真实业务动作，例如购买、取消、claim、下架中的一类或多类
- exchange market 至少一类真实业务动作，例如刷新 quote、确认兑换中的一类或多类

如果动作拆分需要更细，可以增加更明确的 action type，但最终必须满足：

- 真正改变业务状态的动作都走 TerminalActionMessage
- 服务端处理后通过 TerminalSnapshotMessage 回写
- 不允许重新引入 PanelSyncManager 语义

要求：

- actionType 必须显式，不要把 custom / exchange 全塞进一个 payload 字符串分支
- action 的 pageId 必须继续围绕 MARKET_CUSTOM / MARKET_EXCHANGE 语义展开
- 不要把 standardized、custom、exchange 混成一套当前页判断

### 5. 建立 custom / exchange 的 snapshot 回写闭环

本轮必须让 custom / exchange 的 action 执行后，能回写新的 section 数据，而不是只发一条 toast。

至少要完成下面闭环：

- 客户端 section 触发 action
- 服务端通过 TerminalService 或等价装配层处理该 action
- 生成新的 custom market snapshot 或 exchange market snapshot
- 通过 TerminalSnapshotMessage 回写客户端
- 已打开的 TerminalHomeScreen 原地刷新 section 内容

要求：

- 不接受只更新通知文本、不更新 section 数据的半链路
- 不接受操作后强制重新打开整个 terminal screen
- 至少各有一条真实业务动作不是停留在本地假刷新

### 6. 迁移 custom / exchange 的关键确认语义到新 popup 生命周期

本轮至少要把下面这些确认语义中的关键一条或多条迁到新壳 popup 体系：

- custom market 的购买确认 / 取消确认 / claim 确认
- exchange market 的确认兑换

要求：

- 不再依赖旧 Dialog 生命周期
- 必须挂在当前 TerminalPopupFactory / CanvasScreen popup 宿主上
- 确认后要继续走新 action / snapshot 主链

### 7. 收干新 terminal 对旧市场装配的直接依赖残留

phase 7 不只是把 custom / exchange 做出来，还必须把新壳对旧 terminal 市场装配的直接依赖收干。

至少要做到：

- 新 TerminalHomeScreen 不再直接调用旧 TerminalMarketPageBuilder
- 新 market section / popup 不再借壳使用旧 sync binder / old dialog / old session controller
- 新壳继续只使用自己的 framework、section component、action / snapshot 主链

注意：

- 本轮允许旧 terminal 代码继续存在于仓库中
- 但它们不能再是新壳必须依赖的正式运行部件

### 8. 统一 phase 7 页面的滚动、布局与长列表策略

phase 6 已证明新壳必须有局部滚动与更紧凑的密度。

本轮 custom / exchange 页面必须直接沿用或复用 phase 6 已建立的能力，至少满足：

- 不再出现固定前 N 条数据硬裁断
- 小屏可局部滚动
- 大屏信息密度合理，不重新退回“过度放大”观感

### 9. 做 phase 7 文档与 WORKLOG 同步

本轮实际代码变更后，必须同步更新：

- docs/README.md
- docs/WORKLOG.md

如有必要，也可以轻量更新：

- docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md

但不要把本轮扩写成完整 cutover 设计稿。

## 本轮明确不做什么

### 1. 不做新 terminal 正式 cutover

本轮不要开始：

- 把所有 terminal 打开链默认切到“只有新壳”
- 清理旧 terminal 最后过渡装配分支
- 移除任何开发回退路径

### 2. 不删除旧 terminal ModularUI 实现

本轮不要开始：

- 删除 TerminalHomeGuiFactory
- 删除 TerminalMarketPageBuilder
- 删除旧 market binder / session controller / sync state
- 删除终端侧 ModularUI 依赖

### 3. 不把 phase 8 和 phase 9 提前做掉

这轮允许继续扩写 custom / exchange 相关 action / snapshot 结构。

但不要借机把：

- terminal 打开链完全切到新壳
- 旧 terminal 全部删除
- 全 terminal 最终态消息总线
- 统一超级表单系统
- 完整消息中心

提前做掉。

## 测试要求

本轮至少补或更新下面这些验证：

1. custom market 定向测试
   - custom action message 构造测试
   - custom action 进入 TerminalService 后会触发真实处理并回写 snapshot

2. exchange market 定向测试
   - exchange action message 构造测试
   - exchange action 进入 TerminalService 后会触发真实处理并回写 snapshot

3. popup 主链测试
   - 至少证明 custom 或 exchange 的关键确认按钮，最终继续走新 TerminalActionMessage 主链

4. section 内容或 helper 测试
   - 如果 custom / exchange 页面有长列表或复杂 child 生成，至少要有定向测试证明不会再被固定上限裁掉

5. 回归测试
   - 继续运行并保持通过：
   - src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java
   - src/test/java/com/jsirgalaxybase/terminal/TerminalMarketActionMessageFactoryTest.java
   - phase 6 收口引入的滚动与内容生成测试

如遇 LWJGL 或 GuiScreen 级测试环境限制，不要硬造脆弱测试。优先把 popup 确认链、section 内容生成和布局边界下沉到可单测的 helper / state / model 层。

## 本地实装与联调要求

本轮完成后，不只要跑测试，还必须按当前仓库已验证流程完成一次本地联调：

1. 在 JsirGalaxyBase 下执行构建或测试，确认产物最新
2. 用 runServer 启动本地 dedicated server，不手工复制 dev jar 到 run/server/mods
3. 用 runClient 启动本地 client，不手工复制 dev jar 到 run/client/mods
4. 明确验证：
   - 新 terminal 内 BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 都能打开
   - custom / exchange 页面都可滚动并完整浏览关键数据
   - custom / exchange 的关键确认 popup 都能正常命中
   - phase 7 不会把银行页和 standardized 页回归打坏

如果运行态还有非阻塞噪声，要明确区分“功能阻塞”还是“已启动但有噪声日志”。

## 最终输出要求

完成后，请明确汇报：

1. MARKET_CUSTOM 与 MARKET_EXCHANGE 分别落在哪些新 section / model / snapshot 类上
2. custom / exchange 的哪些真实动作已接入 TerminalActionMessage -> TerminalSnapshotMessage 主链
3. 哪些确认语义已迁到新 popup 生命周期
4. 新壳对旧 market builder / binder / dialog / session controller 的哪些直接依赖已被移除
5. 补了哪些测试，分别验证什么
6. 实际运行了哪些定向测试与本地 client/server 联调命令
7. phase 7 完成后，旧 terminal 还剩哪些仅用于 phase 8 cutover 的过渡装配
8. 哪些内容仍明确留给 phase 8 和 phase 9，没有在本轮提前实现

这轮的验收标准很直接：

- 新壳必须已经承接 BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 全部正式业务页
- custom / exchange 的关键动作与确认语义必须走新 action / snapshot / popup 主链
- 新壳不能再直接借壳旧 market builder / binder / dialog / session controller
- 本地 client/server 联调后，可以直接进入 phase 8，而不需要再补一轮业务页迁移

如果这四点没有同时做到，就不算完成。