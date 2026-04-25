# Terminal BetterQuesting 风格 UI 框架第三阶段执行 Prompt

日期：2026-04-15

下面这份 prompt 可直接交给另一个 AI，用于在 `JsirGalaxyBase` 中落地第三阶段的 BetterQuesting 风格终端 UI 集成。

这轮不是银行页迁移，不是市场页迁移，也不是删除旧 `ModularUI` terminal 实现。

这轮只做一件事：

- 把已经接进 terminal 主链的 `TerminalHomeScreen` 占位根屏，推进成“首页壳 + 共用组件层”

---

你正在 `JsirGalaxyBase` 仓库中工作。请直接修改现有 terminal client screen / viewmodel / framework 相关实现，把第二阶段已经接通的新 terminal 打开链，推进到第三阶段的真实首页壳。

## 本轮唯一目标

在不迁移现有银行页、市场页和其他 terminal 业务页的前提下，把当前 phase 2 的 `TerminalHomeScreen` 占位根屏，推进成可以承载后续全部 terminal 业务页的首页壳。

重点是：

- 顶部状态带
- 左侧导航
- 首页主体摘要区
- 全局通知层
- popup 生命周期
- 可复用 panel / section 组件

这轮的目标不是“让首页更漂亮”，而是“把 terminal 新壳真正做成后续银行页和市场页的宿主”。

## 开始前必须先读

开始编码前，先完整阅读并遵守下面这些文档：

- `README.md`
- `docs/terminal-plan.md`
- `docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`
- `docs/terminal-betterquesting-ui-phase1-framework-prompt-2026-04-14.md`
- `docs/terminal-betterquesting-ui-phase2-open-chain-prompt-2026-04-14.md`
- `docs/WORKLOG.md`

必须先看当前新终端主链代码：

- `src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`
- `src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`
- `src/main/java/com/jsirgalaxybase/terminal/client/TerminalClientScreenController.java`
- `src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalOpenSummaryFormatter.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`

必须先看当前仍在旧 `ModularUI` 终端里的可复用 UI 语义来源：

- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalWidgetFactory.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalDialogFactory.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalNotification.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalNotificationSeverity.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalHudNotificationManager.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalHudOverlayHandler.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`

必须先看 phase 1 已落好的 framework 基础层：

- `src/main/java/com/jsirgalaxybase/client/gui/framework/**`
- `src/main/java/com/jsirgalaxybase/client/gui/theme/**`

## 本轮必须完成的内容

### 1. 把 `TerminalHomeScreen` 从占位屏推进成首页壳

当前 `TerminalHomeScreen` 仍然是 phase 2 占位根屏，只承载标题、摘要和关闭按钮。

本轮必须把它推进成真正的首页壳，至少包含：

- 顶部状态带
- 左侧导航区
- 右侧首页主体区
- 全局 popup 宿主
- 全局通知宿主

要求：

- 首页壳必须继续基于 phase 1 的自有 framework 实现
- 不要退回原生散装 `GuiScreen`
- 不要把银行页和市场页真实业务内容直接塞进首页壳里

### 2. 定义首页壳所需的最小 viewmodel 结构

当前 `TerminalHomeScreenModel` 只有：

- `initialPageId`
- `terminalTitle`
- `terminalSubtitle`
- `homeSummaryText`
- `sessionToken`

这不足以支撑后续首页壳。

本轮必须把它推进成能表达“导航 + 状态 + 首页摘要”的最小 screen model。

至少需要考虑：

- 当前选中 page
- 导航项列表或最小导航描述
- 顶部状态带摘要
- 首页主体摘要块
- 会话标识
- 可选的轻量 toast / notification 入口位

要求：

- 字段尽量显式，不要把全部内容重新压回一段 summary string
- 允许新增嵌套 DTO，例如 `TerminalNavItemModel`、`TerminalStatusBandModel`、`TerminalHomeSectionModel`
- 但不要一步到位做成完整银行页 / 市场页 snapshot 大包

### 3. 迁首页壳和共用 panel 组件，不迁业务页本体

本轮必须把“后续各页都会复用”的 panel / section 能力先抽出来。

建议新增：

- `TerminalPanelFactory`
- `TerminalShellPanels`
- `TerminalPopupFactory`
- `TerminalHomeSection`

名称可以调整，但至少要把下面这些共用壳层沉淀出来：

- 顶部状态带 panel
- 左侧导航按钮 / 当前页高亮
- 内容区 section shell
- 首页摘要卡或状态卡
- popup 基础壳

要求：

- 这些类应放在新的 terminal client screen/component 层，而不是继续写回旧 `terminal.ui`
- 本轮抽出的共用壳层，后续银行页和市场页必须能自然站上去

### 4. 建立新 terminal 壳上的全局通知入口

当前仓库已有通知语义和 HUD manager，但新 screen 壳还没有自己的全局通知承载位。

本轮至少要完成下面两件事中的最小闭环：

- 在新 `TerminalHomeScreen` 内建立可渲染的全局 notification 区或 toast 宿主
- 或为后续 `TerminalToastMessage` 留下明确的 viewmodel / renderer 接口

要求：

- 现在不要求完整消息中心
- 也不要求把旧 HUD overlay 全迁过来
- 但新壳必须明确“通知将挂在哪”，不能继续只有业务语义没有宿主

### 5. 建立新 terminal 壳上的 popup 生命周期

phase 1 framework 已经具备 popup 能力，但 phase 3 要把它从 framework 演示能力推进为 terminal 壳级能力。

要求：

- `TerminalHomeScreen` 上必须有明确的 popup 打开 / 关闭策略
- 至少迁出一个 terminal 级 popup 工厂或 helper
- 后续银行确认弹窗、市场确认弹窗必须可以挂到这套壳上，而不是继续依赖旧 `ModularUI` `Dialog`

注意：

- 这轮只迁 popup 壳，不迁具体银行/市场确认业务

### 6. 保留旧 `ModularUI` 业务页作为过渡，不提前接首页壳

这轮目标是首页壳和共用组件，不是业务页挂接。

要求：

- 旧 `TerminalHomeGuiFactory`
- 旧 `TerminalBankPageBuilder`
- 旧 `TerminalMarketPageBuilder`
- 旧 binder / sync state

都继续保留，不要开始半迁。

不允许的做法：

- 把银行首页摘要、市场首页摘要的真实交互页硬塞进新首页壳
- 在新首页壳里偷偷直接调用旧 `ModularUI` widget 或 `PanelSyncManager`

### 7. 补第三阶段文档与 WORKLOG

本轮实际代码变更后，必须同步更新：

- `docs/WORKLOG.md`
- `docs/README.md`

如有必要，也可以轻量更新：

- `docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`

但不要把这轮扩写成完整银行迁移或市场迁移设计稿。

## 本轮明确不做什么

### 1. 不迁银行页

不要开始迁：

- `TerminalBankPageBuilder`
- `TerminalBankSyncBinder`
- `TerminalBankSessionController`

### 2. 不迁市场页

不要开始迁：

- `TerminalMarketPageBuilder`
- `TerminalMarketSyncBinder`
- `TerminalCustomMarket*`
- `TerminalExchangeMarket*`

### 3. 不做完整动作 / 快照协议

本轮不要一步到位写完整：

- `TerminalActionMessage`
- `TerminalSnapshotMessage`
- `TerminalToastMessage`

如果需要为了类型落点新增壳类或接口，允许；但不要把 phase 4 和 phase 5 一起做了。

### 4. 不删除旧 terminal ModularUI 实现

本轮不要开始：

- 删除 `TerminalHomeGuiFactory`
- 删除旧 builder / binder / state
- 删除 `ModularUI2` 依赖

### 5. 不要求启动游戏做人工目检

当前验收口径仍然是“静态验证优先”。

因此本轮必须完成：

- 编译验证
- 最小相关测试或静态结构验证

但不要求：

- 启动 client
- 启动 dedicated server
- 进入游戏人工点击

## 架构要求

### 1. `TerminalHomeScreen` 必须成为后续业务页的真正宿主

这轮做完后，新首页壳至少要在结构上具备：

- 顶部状态带
- 左侧导航
- 主体内容区
- 通知区 / popup 宿主

否则后面银行页和市场页还是没有地方可挂。

### 2. 首页壳的共用组件必须和业务页分层

本轮尽量区分：

- shell 层
- section / panel 共用组件层
- screen model 层

不要把所有内容继续糊在 `TerminalHomeScreen` 一个文件里。

### 3. 保留 `TerminalPage` 作为页面语义基准

当前页面语义和 index 已经固定在 `TerminalPage`。

本轮新导航壳应尽量建立在这个语义基准上，而不是重新发明另一套页面常量。

### 4. 尽量复用既有通知和弹窗语义，而不是重命名一整套制度词汇

比如：

- `TerminalNotification`
- `TerminalNotificationSeverity`
- 已有 title / body / severity 语义

如果能复用，优先复用；不要只是因为换了 UI 框架就重新发明一套命名。

## 推荐实施顺序

建议按下面顺序推进：

1. 扩展 `TerminalHomeScreenModel` 为首页壳模型
2. 抽导航项 / 状态带 / 首页摘要块的最小 DTO
3. 重构 `TerminalHomeScreen` 为顶部状态带 + 左侧导航 + 主体区布局
4. 抽出可复用的 shell / panel 工厂
5. 建立 terminal 级 popup / notification 宿主位
6. 补编译验证、最小测试与 WORKLOG

## 验收标准

只有同时满足下面条件，这轮才算合格：

1. 新 `TerminalHomeScreen` 已经不再是 phase 2 的纯占位板，而是具备首页壳结构
2. 首页壳已经具备顶部状态带、左侧导航和主体区三块基本骨架
3. 新 terminal 壳已经明确了全局通知和 popup 的挂载位置
4. 已沉淀出至少一层后续业务页可复用的 shell / panel 共用组件
5. 旧银行页和市场页没有被半迁移进新壳
6. 编译验证通过
7. 如有新增测试，最小相关测试通过
8. `docs/WORKLOG.md` 已补记录

## 建议修改范围

本轮原则上允许修改：

- `src/main/java/com/jsirgalaxybase/terminal/client/**`
- `src/main/java/com/jsirgalaxybase/client/gui/framework/**`
- `src/main/java/com/jsirgalaxybase/client/gui/theme/**`
- 与 phase 3 直接相关的最小测试文件
- `docs/README.md`
- `docs/WORKLOG.md`

## 建议的静态验证

至少完成下面这些验证：

1. `compileJava`
2. `compileTestJava`
3. 跑新增的最小 screen model / formatter / shell 相关定向测试
4. 确认 `TerminalHomeScreen` 仍通过 phase 2 打开链进入
5. 确认旧 `TerminalHomeGuiFactory`、银行页和市场页仍保留为过渡实现

## 完成后请按这个格式回报

1. 本轮如何把 `TerminalHomeScreen` 从占位屏推进成首页壳
2. `TerminalHomeScreenModel` 或其拆分 DTO 新增了哪些字段 / 类型
3. 抽出了哪些后续业务页可复用的 shell / panel 组件
4. 新 terminal 壳上的通知宿主和 popup 宿主是怎么放的
5. 哪些旧 terminal 业务页被刻意保留没有迁
6. 做了哪些静态验证
7. 下一阶段最自然的进入点是什么

## 给实现者的最后提醒

第三阶段的关键词不是“先迁一个业务页试试”，而是“把新终端壳做成真正能托住所有业务页的宿主”。

本轮完成后，应该得到的是：

- terminal 新主链已经不只是能打开一个首页字符串板
- 首页壳和共用 panel 组件已经长出来
- 后续银行页和市场页迁移终于有了明确挂载面

不要把这轮做成“新的首页装饰稿”；要把它做成后续所有 terminal 业务页都会站上去的壳层地基。