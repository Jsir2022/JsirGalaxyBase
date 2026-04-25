# Terminal BetterQuesting 风格 UI 框架第四阶段执行 Prompt

日期：2026-04-18

下面这份 prompt 可直接交给另一个 AI，用于在 JsirGalaxyBase 中落地终端 BetterQuesting 风格 UI 迁移的第四阶段。

注意：

- 这份 phase 4 prompt 采用当前实际执行链的编号语义。
- 原集成方案文档里的高层路线写的是“首页壳与共用组件之后，先迁银行页”。
- 但 phase 3 严格验收与收口已经明确，真正能安全进入银行页迁移前，还需要先把首页壳推进到“可切换 section 的真实宿主”，并为动作 / 快照协议补最小地基。

所以这轮 phase 4 不是银行页迁移 prompt，而是：

- 首页壳 section 宿主切换
- 最小动作协议落点
- 最小快照回写落点

这轮完成后，下一阶段才进入第一张完整业务页迁移，优先仍是银行页。

---

你正在 JsirGalaxyBase 仓库中工作。请直接修改当前 terminal client screen / viewmodel / network / service 相关实现，把已经完成 phase 3 收口的首页壳，推进成真正可切换 section 的宿主，并为后续银行页迁移补齐最小协议边界。

## 本轮唯一目标

在不迁移现有银行页、市场页与其他 terminal 业务页面的前提下，把当前 TerminalHomeScreen 从“静态首页壳 + 导航高亮 + 非首页弹 popup”推进成：

- 可以按 selectedPageId 切换 section 宿主
- 具备最小 terminal 动作消息落点
- 具备最小 terminal 快照回写落点

重点是：

- 先把新壳真正做成宿主
- 先把后续业务页需要依附的 page/action/snapshot 结构立住
- 不顺手开始迁银行页和市场页

## 开始前必须先读

开始编码前，先完整阅读并遵守下面这些文档：

- README.md
- docs/terminal-plan.md
- docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md
- docs/terminal-betterquesting-ui-phase2-open-chain-prompt-2026-04-14.md
- docs/terminal-betterquesting-ui-phase3-home-shell-prompt-2026-04-15.md
- docs/terminal-betterquesting-ui-phase3-close-prompt-2026-04-15.md
- docs/WORKLOG.md

必须先看当前实现：

- src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPopupFactory.java
- src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java
- src/main/java/com/jsirgalaxybase/terminal/client/TerminalClientScreenController.java
- src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java
- src/main/java/com/jsirgalaxybase/terminal/TerminalOpenApproval.java
- src/main/java/com/jsirgalaxybase/terminal/TerminalService.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java
- src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModelTest.java

同时必须先看当前仍在旧 ModularUI 终端中的业务页边界，确认这轮不要误迁：

- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankPageBuilder.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalDialogFactory.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java

## 本轮必须完成的内容

### 1. 把 TerminalHomeScreen 推进成真正的 section 宿主

当前 TerminalHomeScreen 已经有：

- 顶部状态带
- 左侧导航
- 首页主体区
- 通知宿主
- popup 宿主

但它还没有真正按当前页切换主体 section；非首页点击仍主要是 popup 占位。

本轮必须把它推进成真正的 section 宿主，至少满足：

- 当前主体区根据 selectedPageId 渲染对应的 section
- home、career、public_service、market、bank 至少各自有独立 section 落点
- 非首页导航点击不再只弹“未来再做”的 popup，而是进入对应 section 的正式占位内容

要求：

- 继续基于 phase 1 的自有 framework 实现
- section 可以先是占位 section，但必须真实挂到宿主切换链上
- 当前页切换必须继续以 selectedPageId 为唯一真源

### 2. 建立首页壳的最小 section 路由层

本轮必须让“page id -> section 宿主”成为明确结构，而不是散落在 if/else 里临时拼。

可接受做法包括：

- 引入 TerminalSectionHost / TerminalSectionRouter / TerminalHomeSections 一类的宿主层
- 或在 TerminalHomeScreen 内抽出清晰的 page-to-section 装配方法

但最终必须满足：

- 当前页路由是显式结构
- 后续银行 section 与市场 section 能自然挂上去
- 不能继续把主体区默认写死为首页摘要块

### 3. 为后续页内交互建立最小 TerminalActionMessage 落点

这轮不要把完整业务动作协议一次写完，但必须把动作消息的正式落点立住。

至少需要：

- 一个明确的 TerminalActionMessage 或等价消息类型
- 显式包含 sessionToken
- 显式包含 pageId 或 page 语义
- 显式包含 actionType
- 允许最小 payload 结构，哪怕当前只支持非常窄的壳级动作

当前这轮至少要让新壳里的一类轻量动作真正走上这条新协议，例如：

- section 切换后的“刷新当前 section 占位快照”
- 或一个明确的 section 内占位 action

要求：

- 不允许继续依赖 PanelSyncManager 语义
- 不允许把协议只写成注释或空壳而没有最小实际调用面
- 但也不要借机把银行 / 市场完整动作全迁进来

### 4. 为后续刷新建立最小 TerminalSnapshotMessage 落点

这轮必须把“服务端回写当前页或当前 section 新快照”的落点立起来。

至少需要：

- 一个 TerminalSnapshotMessage 或等价消息类型
- 能表达当前 page 的最小快照回写
- 客户端收到后能更新当前 screen model 或替换当前 section 所需视图数据

要求：

- 这轮不要求完整银行快照或市场快照协议
- 但必须证明新壳不再只有打开时一次性批准载荷，后续刷新链已经有正式落点
- 最好让一条最小 section 刷新链真正跑通

### 5. 把 phase 3 的临时 popup 行为收口成 section 占位内容

phase 3 中，非首页导航点击仍会用 popup 表达“新壳已接线，但业务页未迁”。

本轮要求：

- 非首页导航点击后，主内容区优先切换到对应 section 占位内容
- popup 仍可保留，但只作为 section 内附加说明或帮助层
- 不再让 popup 继续承担 page routing 的主要职责

### 6. 保持旧 ModularUI 业务页为过渡实现，不提前迁业务

这轮的目标是：

- 宿主切换
- 动作 / 快照协议地基

不是：

- 银行页迁移
- 市场页迁移
- 旧终端删除

因此必须保持：

- TerminalHomeGuiFactory
- TerminalBankPageBuilder
- TerminalMarketPageBuilder
- 旧 binder / sync state

继续保留为过渡实现。

不允许的做法：

- 把旧银行页直接嵌回新首页壳
- 把旧市场页直接嵌回新首页壳
- 为了演示动作协议，偷偷开始接银行开户 / 转账 / 市场下单

### 7. 补第四阶段文档与 WORKLOG

本轮实际代码变更后，必须同步更新：

- docs/WORKLOG.md
- docs/README.md

如有必要，也可以轻量更新：

- docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md

但不要把本轮扩写成完整银行迁移设计稿。

## 本轮明确不做什么

### 1. 不迁银行页

不要开始迁：

- TerminalBankPageBuilder
- TerminalBankSessionController
- TerminalBankSyncBinder
- 开户 / 刷新 / 转账 / 确认弹窗的真实新壳业务实现

### 2. 不迁市场页

不要开始迁：

- TerminalMarketPageBuilder
- TerminalMarketSyncBinder
- TerminalCustomMarket*
- TerminalExchangeMarket*

### 3. 不删除旧 terminal ModularUI 实现

本轮不要开始：

- 删除 TerminalHomeGuiFactory
- 删除旧 builder / binder / sync state
- 删除 ModularUI2 依赖
- 改 TerminalModule 去掉旧 factory 注册

### 4. 不把协议一次写成最终形态

这轮允许建立：

- TerminalActionMessage 最小正式落点
- TerminalSnapshotMessage 最小正式落点

但不要一步到位把：

- 全银行动作
- 全市场动作
- 完整消息中心
- 全页快照总线

一起做掉。

### 5. 不要求启动游戏做人工目检

当前验收口径仍然是静态验证优先。

因此本轮必须完成：

- 编译验证
- 最小相关测试或静态结构验证

但不要求：

- 启动 client
- 启动 dedicated server
- 进入游戏人工点击

## 架构要求

### 1. 当前页真源必须继续只有 selectedPageId

本轮开始 section 宿主切换后，更不能把当前页语义重新分裂回：

- selectedPageId
- navItems[].selected
- 某个 section 自己的局部状态

当前页必须继续只有 selectedPageId 一个真源。

### 2. section 宿主与业务页实现分层

这轮应至少分清：

- 壳级 section 宿主 / 路由
- 壳级动作消息 / 快照消息
- 未来真实业务 section 实现

不要把 phase 4 的宿主层直接写死成“为银行页特判”的一次性代码。

### 3. 动作协议与快照协议必须围绕 page 语义展开

至少要让后续协议天然能围绕：

- sessionToken
- pageId
- actionType
- section snapshot

展开，而不是回到旧同步链的控件级隐式同步。

### 4. phase 4 完成后，应自然承接“银行页作为第一张完整业务页”

也就是说，这轮完成后，下一阶段应能合理开始：

- Bank section 真实 view model
- 银行 action
- 银行快照刷新
- 银行确认弹窗

但这些内容不应在本轮提前实现。

## 推荐实施顺序

建议按下面顺序推进：

1. 抽出 page -> section 宿主切换结构
2. 把首页、职业、公共、市场、银行的 section 占位内容都挂进新壳
3. 新增最小 TerminalActionMessage 与处理链
4. 新增最小 TerminalSnapshotMessage 与客户端回写链
5. 让至少一条壳级 action -> server -> snapshot 回写闭环跑通
6. 补定向测试、README 与 WORKLOG

## 测试要求

本轮至少补或更新下面这些验证：

1. TerminalHomeScreenModelTest 或同层测试必须覆盖：
   - 当 selectedPageId 切到非首页时，section 宿主也跟着切换，而不是只改导航高亮
   - bank / market 等顶层 page 的 section 选择与 selectedPageId 一致

2. 如果新增 section 路由层：
   - 必须有测试证明 page id 会落到正确 section

3. 如果新增 TerminalActionMessage / TerminalSnapshotMessage：
   - 至少有最小定向测试证明一条壳级 action 能触发服务端处理并回写快照或刷新结果

4. 至少运行与本轮直接相关的编译与定向测试，并在最终说明里列出

## 允许修改的范围

本轮原则上允许修改：

- src/main/java/com/jsirgalaxybase/terminal/client/screen/**
- src/main/java/com/jsirgalaxybase/terminal/client/component/**
- src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/**
- src/main/java/com/jsirgalaxybase/terminal/client/**
- src/main/java/com/jsirgalaxybase/terminal/network/**
- src/main/java/com/jsirgalaxybase/terminal/TerminalService.java
- src/main/java/com/jsirgalaxybase/terminal/TerminalOpenApproval.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java
- 与本轮直接相关的最小测试文件
- docs/README.md
- docs/WORKLOG.md

不要碰：

- src/main/java/com/jsirgalaxybase/terminal/ui/ 里的旧业务页主实现
- modules/core/banking/**
- modules/core/market/**
- modules/servertools/**

## 最终输出要求

完成后，请明确汇报：

1. 当前首页壳是如何按 selectedPageId 切换 section 的
2. 这轮新增或调整了哪些 section 宿主 / 路由类
3. TerminalActionMessage 与 TerminalSnapshotMessage 最终落在哪些类上
4. 哪一条最小 action -> snapshot 回写链已经真正跑通
5. 补了哪些测试
6. 实际运行了哪些编译 / 定向测试
7. 哪些内容仍明确留给下一阶段银行页迁移

## 明确禁止事项

- 不开始银行页迁移
- 不开始市场页迁移
- 不接回旧 ModularUI 页面
- 不把当前页真源重新分裂
- 不把协议一次扩成完整终端总线
- 不扩大到 terminal 视觉改版

这轮的验收标准很直接：

- 首页壳已经成为真实的 section 宿主，而不是只有首页有内容
- 当前页切换已驱动主体区 section 切换
- 新壳已经拥有最小 action / snapshot 正式协议落点
- 修复后可以安全进入“银行页作为第一张完整业务页”的下一阶段