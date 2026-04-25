# Terminal BetterQuesting 风格 UI 框架第六阶段执行 Prompt

日期：2026-04-19

下面这份 prompt 可直接交给另一个 AI，用于在 JsirGalaxyBase 中落地终端 BetterQuesting 风格 UI 迁移的第六阶段。

注意：

- 当前仓库已经完成 phase 1 到 phase 5：framework 地基、打开链、首页壳、section 宿主与最小 action / snapshot 协议、银行页完整迁移。
- 当前 bank 已经是新终端壳上的第一张完整业务页；下一步必须把 MARKET 总入口和标准商品市场主链迁入新壳，证明新 terminal 不只会承接单页表单，而是真正能承接第二类复杂业务页。
- 后续阶段顺序已经明确，不允许在这轮重新发散评估：

1. phase 6：市场总览与标准商品市场迁移
2. phase 7：定制商品市场与汇率市场迁移，并收干旧 terminal 装配残留
3. phase 8：新终端壳正式 cutover
4. phase 9：删除旧 terminal ModularUI 实现

这份 prompt 只负责 phase 6，不提前做 phase 7 到 phase 9 的工作。

---

你正在 JsirGalaxyBase 仓库中工作。请直接修改当前 terminal client screen / viewmodel / network / service 相关实现，把 market 顶层 section 从 phase 4 的宿主占位内容，推进成“MARKET 总入口 + 标准商品市场”两层真实内容，并保持 custom market / exchange market 继续留在后续阶段。

## 本轮唯一目标

在不迁移定制商品市场、不迁移汇率市场、不做 cutover、也不删除旧 terminal ModularUI 实现的前提下，把 MARKET 总入口和标准商品市场完整迁入新 TerminalHomeScreen 宿主。

本轮做完后，market 顶层 section 应至少具备：

- MARKET 总入口真实摘要与三类子市场入口卡
- 标准商品市场浏览层与商品详情层
- 标准商品市场的价格 / 深度 / 仓储 / claimable 摘要
- 至少一条真实交易动作的 action -> snapshot 回写闭环
- 至少一条市场确认语义迁到新 popup 生命周期

重点是：

- MARKET 根页继续只是总入口与共享摘要，不重新退回混合巨石详情页
- 标准商品市场成为继银行页之后第二张完整业务页
- 继续沿用 phase 4 / phase 5 已建立的 selectedPageId / action / snapshot 主链
- 不顺手开始定制商品市场和汇率市场迁移

## 开始前必须先读

开始编码前，先完整阅读并遵守下面这些文档：

- README.md
- docs/terminal-plan.md
- docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md
- docs/terminal-betterquesting-ui-phase4-section-host-prompt-2026-04-18.md
- docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md
- docs/market-three-part-architecture.md
- docs/market-entry-overview.md
- docs/standardized-market-catalog-boundary.md
- docs/WORKLOG.md

必须先看当前新终端主链代码：

- src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalSectionRouter.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPopupFactory.java
- src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java
- src/main/java/com/jsirgalaxybase/terminal/client/TerminalClientScreenController.java
- src/main/java/com/jsirgalaxybase/terminal/network/TerminalActionMessage.java
- src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java
- src/main/java/com/jsirgalaxybase/terminal/TerminalService.java
- src/main/java/com/jsirgalaxybase/terminal/TerminalActionType.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java
- src/main/java/com/jsirgalaxybase/terminal/TerminalBankActionMessageFactory.java

必须先看当前市场旧实现，确认迁移的是业务语义而不是旧 UI 容器：

- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketRoutePlan.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSessionController.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncState.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSnapshot.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java

如需追业务真实边界，也必须先看：

- src/main/java/com/jsirgalaxybase/modules/core/market/**

## 本轮必须完成的内容

### 1. 把 MARKET 顶层 section 升级成真实 MarketOverviewSection

phase 4 中，MARKET 仍只是说明性 snapshot。

本轮必须把它升级成真正的市场总入口 section，至少具备：

- 市场共享状态摘要区
- 三类市场入口卡
- 当前 phase 节奏提示
- 明确指出标准商品市场已迁入、定制商品与汇率市场仍留待后续阶段

要求：

- MARKET 根页只承担总入口与共享摘要，不承担标准商品详情大杂烩
- market overview section 必须挂在 phase 4 已有的 section 宿主上
- 不允许直接把旧 TerminalMarketPageBuilder 的 MARKET 总页塞回新壳

### 2. 把 MARKET_STANDARDIZED 顶层 page 升级成真实 StandardizedMarketSection

本轮必须把标准商品市场迁成真实 section，而不是继续用占位提示页。

至少具备：

- 商品浏览区
- 当前选中商品摘要
- 盘口 / 最近价格 / 深度摘要
- 标准商品交易动作区
- 个人仓储 / claimable / 我的订单摘要
- 最近动作反馈展示

要求：

- UI 层必须基于当前 BetterQuesting 风格 framework 与 terminal client component 层重建
- 不允许直接把旧 TerminalMarketPageBuilder 的详情 panel 嵌回新壳
- 浏览层与详情层保持 asset-first 节奏，不重新退回说明优先或字段摊平式布局

### 3. 定义独立的市场页 view model 与 snapshot

本轮不能只靠通用 PageSnapshotModel 拼字符串过活。

至少需要为市场页引入清晰的模型结构，建议包括：

- market overview 顶层 model
- standardized market 顶层 model
- 商品浏览 model
- 当前商品摘要 / 盘口摘要 model
- 交易表单或动作摘要 model
- 仓储 / claimable / 我的订单 model
- 动作反馈 model

要求：

- MARKET 根页和 MARKET_STANDARDIZED 页的模型边界必须分开
- 不直接复用旧 ModularUI 的 sync state 作为新模型
- 新模型要能自然经由 TerminalSnapshotMessage 回写
- 不为了 phase 7 提前做大而空的“全市场统一超级模型”

### 4. 把标准商品市场动作正式接到 TerminalActionMessage 主链

本轮至少要把下面这些动作接到现有 action 协议上：

- 刷新 MARKET 总入口或标准商品页
- 选择标准商品对象
- 至少一类真实交易动作，例如限价买入、限价卖出、即时买入、即时卖出中的一类或多类
- 至少一类后处理动作，例如 claim 或 cancel 中的一类

如果你认为动作拆分需要更细，也可以增加更明确的 action type，但最终必须满足：

- 真正改变业务状态的动作都走 TerminalActionMessage
- 服务端处理后通过 TerminalSnapshotMessage 回写
- 不允许重新引入 PanelSyncManager 语义

要求：

- actionType 必须显式，不要把所有市场动作塞进一个 payload 字符串分支
- action 的 pageId 必须继续围绕 MARKET / MARKET_STANDARDIZED 语义展开
- MARKET 根页与标准商品页不要混成一套当前页判断

### 5. 建立标准商品市场 snapshot 回写闭环

本轮必须让标准商品市场 action 执行后，能回写新的 standardized market section 数据，而不是只发一条 toast。

至少要完成下面闭环：

- 客户端 market section 触发 action
- 服务端通过 TerminalService 或等价装配层处理该 action
- 生成新的 market overview snapshot 或 standardized market snapshot
- 通过 TerminalSnapshotMessage 回写客户端
- 已打开的 TerminalHomeScreen 原地刷新 market section 内容

要求：

- 不接受只更新通知文本、不更新 section 数据的半链路
- 不接受操作后强制重新打开整个 terminal screen
- 至少要有一条真实交易动作不是停留在本地假刷新

### 6. 迁移一条市场确认语义到新 popup 生命周期

本轮至少要把标准商品市场里一条关键确认语义迁到新壳 popup 体系，例如：

- 撤单确认
- claim 确认
- 即时交易确认

要求：

- 不再依赖旧 Dialog 生命周期
- 必须挂在当前 TerminalPopupFactory / CanvasScreen popup 宿主上
- 确认后要继续走新 action / snapshot 主链

### 7. 保留旧市场页为过渡实现，但不允许重新接回新壳

本轮允许保留旧市场实现继续存在于仓库中。

但不允许：

- 从新 TerminalHomeScreen 直接调用旧 TerminalMarketPageBuilder
- 从新 market section 借壳使用旧 sync binder / old dialog
- 用“先嵌旧 market 页，再慢慢替换”的方式偷渡迁移

### 8. 补 phase 6 文档与 WORKLOG

本轮实际代码变更后，必须同步更新：

- docs/README.md
- docs/WORKLOG.md

如有必要，也可以轻量更新：

- docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md

但不要把本轮再扩写成完整 cutover 设计稿。

## 本轮明确不做什么

### 1. 不迁移定制商品市场与汇率市场

不要开始迁：

- MARKET_CUSTOM 真实业务页
- MARKET_EXCHANGE 真实业务页
- 定制商品挂牌 / 购买 / claim 主链
- quote / exchange 正式业务页

### 2. 不删除旧 terminal ModularUI 实现

本轮不要开始：

- 删除 TerminalHomeGuiFactory
- 删除 TerminalMarketPageBuilder
- 删除旧 market binder / session controller / sync state
- 删除终端侧 ModularUI 依赖

### 3. 不做 terminal 正式 cutover

本轮不要开始：

- 把所有 terminal 打开链默认切到“只有新壳”
- 清理旧 terminal 装配分支
- 移除任何开发回退路径

### 4. 不把 terminal 总线一步扩成 phase 9 的最终形态

这轮允许继续扩写 market 相关 action / snapshot 结构。

但不要借机把：

- custom market 全动作
- exchange market 全动作
- 全 terminal 统一表单系统
- 完整消息中心
- 大而全页面状态总线

一起做掉。

### 5. 不要求启动游戏做人工目检

当前阶段仍以静态验证优先。

因此本轮必须完成：

- 编译验证
- market 页相关定向测试

但不要求：

- 启动 client
- 启动 dedicated server
- 进入游戏手点 market 页

## 架构要求

### 1. 当前页真源仍然只有 selectedPageId

市场页迁入后，也不能让当前页语义重新分裂回：

- selectedPageId
- market section 自己的局部 current page
- 某些 tab 或按钮态反推页面态

market overview 和 standardized market 都只是 page 内容实现，不是第二套路由真源。

### 2. MARKET 根页与 MARKET_STANDARDIZED 必须有清晰分层

当前正式边界已经确定：

- MARKET 是总入口与共享摘要
- MARKET_STANDARDIZED 是标准商品市场真实业务页

不要把这两层重新糊成一页。

### 3. 市场业务模型与 UI 装配必须分层

这轮至少要分清：

- market overview / standardized market 的 view model
- market section 的 panel 装配
- market action / snapshot 协议
- 标准商品底层业务服务调用

不要把 terminal client section 直接写成 service 脚本式拼接。

### 4. 复用现有业务服务，不重写市场领域语义

本轮重点是终端 GUI 迁移，不是重写市场系统。

应优先复用现有：

- TerminalMarketService 中已成型的标准商品快照语义
- modules/core/market 下的标准商品市场服务与仓储
- 现有 JDBC / 目录 / 订单簿 / claimable 领域模型

不允许为了 GUI 迁移，顺手改写市场业务规则、目录制度边界或数据库边界。

### 5. 为 phase 7 保留节奏，但不提前统一过度抽象

phase 7 才处理定制商品市场、汇率市场和 terminal 旧装配残留。

本轮可以抽少量共用组件，但不要为了“后面 custom / exchange 可能也用”把市场模型提前抽成看不清语义的全市场通用表单框架。

## 推荐实施顺序

建议按下面顺序推进：

1. 定义 market overview 与 standardized market 的最小 snapshot / view model
2. 抽出 TerminalMarketOverviewSection 与 TerminalStandardizedMarketSection 或等价真实 section
3. 先接 MARKET 根页共享摘要与子入口卡
4. 再接标准商品浏览层与详情层
5. 接至少一类真实交易动作与一类后处理动作的 action -> snapshot 回写闭环
6. 接一条市场确认 popup 到新 popup 生命周期
7. 补市场页定向测试、README 与 WORKLOG

## 测试要求

本轮至少补或更新下面这些验证：

1. market section model 测试：
   - MARKET 总入口摘要映射正确
   - standardized market 商品浏览 / 当前商品摘要字段映射正确

2. market action 测试：
   - 至少一类真实交易动作能触发服务端处理并回写新的 standardized market snapshot
   - 至少一类 claim / cancel 等后处理动作能触发服务端处理并回写新的 standardized market snapshot

3. popup / 确认链测试：
   - 市场确认弹窗不会绕回旧 Dialog 体系
   - 确认后继续走新 action / snapshot 主链

4. 至少运行与本轮直接相关的编译与定向测试，并在最终说明中列出

测试策略继续避免直接依赖 GuiScreen / LWJGL 类加载环境。

## 允许修改的范围

本轮原则上允许修改：

- src/main/java/com/jsirgalaxybase/terminal/client/screen/**
- src/main/java/com/jsirgalaxybase/terminal/client/component/**
- src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/**
- src/main/java/com/jsirgalaxybase/terminal/client/**
- src/main/java/com/jsirgalaxybase/terminal/network/**
- src/main/java/com/jsirgalaxybase/terminal/TerminalService.java
- src/main/java/com/jsirgalaxybase/terminal/TerminalOpenApproval.java
- src/main/java/com/jsirgalaxybase/terminal/TerminalActionType.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java
- 与 market section 迁移直接相关的最小 service / provider / helper 装配文件
- 与本轮直接相关的最小测试文件
- docs/README.md
- docs/WORKLOG.md

不要碰：

- modules/servertools/**
- 银行业务规则与银行页主链
- 定制商品市场真实业务主链
- 汇率市场真实业务主链
- 旧 terminal cutover / 删除工作

## 最终输出要求

完成后，请明确汇报：

1. 新 market overview section 与 standardized market section 落在哪些类上
2. 市场页 view model 如何拆分
3. 哪些市场动作已经走上 TerminalActionMessage 主链
4. 哪条标准商品市场 snapshot 回写闭环已经真正跑通
5. 哪条市场确认弹窗已经挂到新 popup 生命周期上
6. 补了哪些测试
7. 实际运行了哪些编译 / 定向测试
8. 哪些内容明确留给 phase 7

## 明确禁止事项

- 不开始定制商品市场迁移
- 不开始汇率市场迁移
- 不把旧市场页直接嵌回新壳
- 不回到 PanelSyncManager 同步模型
- 不重写市场业务规则
- 不做 terminal cutover
- 不删除旧 terminal ModularUI 实现

这轮的验收标准很直接：

- MARKET 根页已成为新壳上的真实总入口页
- 标准商品市场已成为新壳上的第二张完整业务页
- 至少一条真实市场动作已经完成 action -> snapshot 回写闭环
- 至少一条市场确认语义已经迁到新 popup 生命周期
- 修复后可以直接进入 phase 7 的定制商品市场 / 汇率市场迁移与旧 terminal 装配残留收干