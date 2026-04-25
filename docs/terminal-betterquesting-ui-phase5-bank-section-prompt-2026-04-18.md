# Terminal BetterQuesting 风格 UI 框架第五阶段执行 Prompt

日期：2026-04-18

下面这份 prompt 可直接交给另一个 AI，用于在 JsirGalaxyBase 中落地终端 BetterQuesting 风格 UI 迁移的第五阶段。

注意：

- 当前仓库已经完成 phase 1 到 phase 4：framework 地基、打开链、首页壳、section 宿主与最小 action / snapshot 协议。
- 从当前阶段开始，目标是加快终端迁移节奏，并尽可能在“从现在起再往后不超过五个阶段”内，达到删除终端旧 ModularUI 实现的条件。
- 因此这一轮必须只做“银行页作为第一张完整业务页”的真正迁移，不允许再回到大范围评估或重复搭壳。

压缩后的后续目标建议按下面节奏推进：

1. phase 5：银行页迁移
2. phase 6：市场总览与标准商品市场主链迁移
3. phase 7：定制商品市场与汇率市场迁移，并收干 terminal 级旧装配残留
4. phase 8：终端正式 cutover 到新壳，旧 terminal 打开链退为仅过渡或开发回退
5. phase 9：删除旧 ModularUI terminal 实现并评估移除终端侧 ModularUI 依赖

这份 prompt 只负责 phase 5，不提前做 phase 6 到 phase 9 的工作。

---

你正在 JsirGalaxyBase 仓库中工作。请直接修改当前 terminal client screen / viewmodel / network / service 相关实现，把 bank 顶层 section 从 phase 4 的宿主占位内容，推进成新终端壳上的第一张完整业务页。

## 本轮唯一目标

在不迁移市场页和不删除旧 terminal ModularUI 实现的前提下，把银行页完整迁入新 TerminalHomeScreen 宿主。

本轮做完后，bank 顶层 section 应至少具备：

- 开户状态展示
- 余额与基础摘要刷新
- 转账表单
- 转账确认弹窗
- action 执行后的 snapshot 回写与通知反馈

重点是：

- 银行页成为新壳上的第一张完整业务页
- 继续沿用 phase 4 已建立的 selectedPageId / action / snapshot 主链
- 不顺手开始迁市场页

## 开始前必须先读

开始编码前，先完整阅读并遵守下面这些文档：

- README.md
- docs/terminal-plan.md
- docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md
- docs/terminal-betterquesting-ui-phase4-section-host-prompt-2026-04-18.md
- docs/banking-system-requirements.md
- docs/banking-terminal-gui-design.md
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

必须先看当前银行旧实现，确认迁移的是业务语义而不是旧 UI 容器：

- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankPageBuilder.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSessionController.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalDialogFactory.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSnapshotProvider.java
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankingService.java

如需追业务真实边界，也必须先看：

- src/main/java/com/jsirgalaxybase/modules/core/banking/**

## 本轮必须完成的内容

### 1. 把 bank 顶层 section 升级成真实 TerminalBankSection

phase 4 中，bank section 仍只是说明性 snapshot。

本轮必须把它升级成真正的银行页 section，至少具备：

- 银行状态摘要区
- 余额或账户摘要区
- 开户入口或开户状态展示
- 转账入口与表单区
- 最近一次动作反馈展示

要求：

- bank section 必须挂在 phase 4 已有的 section 宿主上
- 不允许直接把旧 TerminalBankPageBuilder 塞回新壳
- UI 层必须基于当前 BetterQuesting 风格 framework 与 terminal client component 层重建

### 2. 定义独立的银行页 view model

本轮不能只靠通用 PageSnapshotModel 拼字符串过活。

至少需要为银行页引入清晰的 view model 结构，建议包括：

- bank section 顶层 model
- 开户状态 / 账户状态 model
- 余额摘要 model
- 转账表单 model
- 最近动作反馈 model

要求：

- 不直接复用旧 ModularUI 的 sync state 作为新模型
- 新模型要能自然经由 TerminalSnapshotMessage 回写
- 尽量围绕“银行页是第一张完整业务页”来设计，而不是做成 market 也能乱套用的大而空模型

### 3. 把银行动作正式接到 TerminalActionMessage 主链

本轮至少要把下面这些动作接到现有 action 协议上：

- 刷新银行页
- 开户
- 打开转账确认
- 确认转账

如果你认为“打开转账确认”不应是服务端动作，也可以保留为 client-side popup 行为，但最终必须满足：

- 真正改变业务状态的动作都走 TerminalActionMessage
- 服务端处理后通过 TerminalSnapshotMessage 回写
- 不允许重新引入 PanelSyncManager 语义

要求：

- actionType 必须显式，不要把全部动作塞进一个 payload 字符串分支
- action 的 pageId 必须继续围绕 bank page 语义展开

### 4. 建立银行页 snapshot 回写闭环

本轮必须让银行页 action 执行后，能回写新的 bank section 数据，而不是只发一条 toast。

至少要完成下面闭环：

- 客户端 bank section 触发 action
- 服务端通过 TerminalService 或等价装配层处理该 action
- 生成新的 bank section snapshot
- 通过 TerminalSnapshotMessage 回写客户端
- 已打开的 TerminalHomeScreen 原地刷新 bank section 内容

要求：

- 不接受只更新通知文本、不更新 section 数据的半链路
- 不接受操作后强制重新打开整个 terminal screen

### 5. 迁移银行确认弹窗到新 popup 生命周期

本轮至少要把银行里最关键的确认语义迁到新壳 popup 体系，例如：

- 转账确认弹窗

要求：

- 不再依赖旧 TerminalDialogFactory 的 ModularUI Dialog 生命周期
- 必须挂在当前 TerminalPopupFactory / CanvasScreen popup 宿主上
- 确认后要继续走新 action/snapshot 主链

### 6. 保留旧银行页为过渡实现，但不允许重新接回新壳

本轮允许保留：

- TerminalHomeGuiFactory
- TerminalBankPageBuilder
- TerminalBankSessionController
- TerminalBankSyncBinder

继续作为过渡实现存在于仓库中。

但不允许：

- 从新 TerminalHomeScreen 直接调用旧银行页 builder
- 从新 bank section 借壳使用旧 sync state / old dialog
- 用“先嵌旧页，再慢慢替换”的方式偷渡迁移

### 7. 补 phase 5 文档与 WORKLOG

本轮实际代码变更后，必须同步更新：

- docs/README.md
- docs/WORKLOG.md

如有必要，也可以轻量更新：

- docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md

但不要把本轮再扩写成完整市场迁移设计稿。

## 本轮明确不做什么

### 1. 不迁市场页

不要开始迁：

- MARKET 总览真实页
- 标准商品市场页
- 定制商品市场页
- 汇率市场页

### 2. 不删除旧 terminal ModularUI 实现

本轮不要开始：

- 删除 TerminalHomeGuiFactory
- 删除 TerminalBankPageBuilder / TerminalMarketPageBuilder
- 删除旧 binder / state
- 删除终端侧 ModularUI 依赖

### 3. 不把 terminal 总线一步扩成最终形态

这轮允许继续扩写 bank 相关 action / snapshot 结构。

但不要借机把：

- 全 market 动作
- 全 terminal 统一表单系统
- 完整消息中心
- 大而全页面状态总线

一起做掉。

### 4. 不要求启动游戏做人工目检

当前阶段仍以静态验证优先。

因此本轮必须完成：

- 编译验证
- 银行页相关定向测试

但不要求：

- 启动 client
- 启动 dedicated server
- 进入游戏手点银行页

## 架构要求

### 1. 当前页真源仍然只有 selectedPageId

银行页迁入后，也不能让当前页语义重新分裂回：

- selectedPageId
- bank section 自己的局部 current page
- 某些按钮态反推页面态

bank section 只是 bank 顶层 page 的真实内容实现，不是第二套 page routing。

### 2. 银行业务模型与 UI 装配分层

这轮至少要分清：

- bank section 的 view model
- bank section 的 panel 装配
- bank action / snapshot 协议
- bank 底层业务服务调用

不要把 terminal client section 直接写成 service 脚本式拼接。

### 3. 复用现有业务服务，不重写银行领域语义

本轮重点是终端 GUI 迁移，不是重写银行系统。

应优先复用现有：

- TerminalBankSnapshotProvider
- TerminalBankingService
- 底层 banking module

不允许为了 GUI 迁移，顺手改写银行业务规则、账本语义或数据库边界。

### 4. 为后续市场迁移保留节奏，但不提前统一过度抽象

银行页是第一张完整业务页，应优先保证真实可迁。

可以抽少量共用组件，但不要为了“以后 market 可能也用”把 bank view model 过度抽象成看不清语义的通用表单框架。

## 推荐实施顺序

建议按下面顺序推进：

1. 定义 bank section 的最小 view model
2. 抽出 TerminalBankSection 或等价真实 section 实现
3. 先接银行摘要刷新与开户状态
4. 接转账表单与确认 popup
5. 接开户 / 转账 action -> snapshot 回写闭环
6. 补银行页定向测试、README 与 WORKLOG

## 测试要求

本轮至少补或更新下面这些验证：

1. bank section model 测试：
   - 账户已开 / 未开两种状态
   - 余额与摘要字段映射正确

2. bank action 测试：
   - 开户 action 能触发服务端处理并回写新的 bank snapshot
   - 转账 action 在确认后能触发服务端处理并回写新的 bank snapshot

3. popup / 确认链测试：
   - 转账确认不会绕回旧 Dialog 体系
   - 确认后继续走新 action/snapshot 主链

4. 至少运行与本轮直接相关的编译与定向测试，并在最终说明中列出

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
- 与 bank section 迁移直接相关的最小 service / provider 装配文件
- 与本轮直接相关的最小测试文件
- docs/README.md
- docs/WORKLOG.md

不要碰：

- modules/core/market/**
- modules/servertools/**
- 旧市场页主实现

## 最终输出要求

完成后，请明确汇报：

1. 新 bank section 落在哪些类上
2. 银行页 view model 如何拆分
3. 哪些银行动作已经走上 TerminalActionMessage 主链
4. 哪条银行 snapshot 回写闭环已经真正跑通
5. 转账确认弹窗如何挂到新 popup 生命周期上
6. 补了哪些测试
7. 实际运行了哪些编译 / 定向测试
8. 哪些内容明确留给 phase 6 市场迁移

## 明确禁止事项

- 不开始市场页迁移
- 不把旧银行页直接嵌回新壳
- 不回到 PanelSyncManager 同步模型
- 不重写银行业务规则
- 不删除旧 terminal ModularUI 实现

这轮的验收标准很直接：

- bank 顶层 section 已成为新壳上的第一张完整业务页
- 开户 / 刷新 / 转账确认至少有一条真实 action -> snapshot 回写闭环
- 银行业务内容已经不再依赖旧 ModularUI UI 容器
- 修复后可以直接进入 phase 6 的市场迁移