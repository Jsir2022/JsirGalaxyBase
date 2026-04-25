# JsirGalaxyBase 终端实施方案

日期：2026-03-29

## 目标

终端不是单一功能页面，而是 `JsirGalaxyBase` 后续制度核心与能力模块的统一入口壳。

第一阶段目标不是一次做完职业、市场、公会和公共服务，而是先把：

- 快捷键入口
- 背包界面入口
- 服务端权威的打开链
- 可替换的终端窗口壳

先稳定下来。

## 终端定位

终端承担下面这些职责：

- 职业信息与升级入口
- 市场与订单入口
- 福利与公共服务入口
- 后续能力模块的统一导航壳

因此终端必须按：

- `统一外壳`
- `分页能力`
- `服务端鉴权`
- `可替换 UI 实现`

来设计，而不是先做一个孤立 GUI。

## 打开方式

当前确认采用双入口方案：

### 1. 快捷键打开

- 客户端注册终端快捷键
- 按键后只发送“请求打开终端”到服务端
- 服务端判断玩家状态后统一打开终端

这条链路参考 `Reference/Garbage`：

- 客户端注册按键
- 客户端按键事件发送打开请求
- 服务端处理网络包后调用统一 GUI 打开入口

### 2. 背包界面按钮打开

- 在玩家背包界面注入终端按钮
- 点击后发送同一个打开请求到服务端
- 与快捷键共用同一条服务端打开链

这条链路参考 `Reference/ServerUtilities`：

- 在 `GuiScreenEvent.InitGuiEvent.Post` 中向背包类 GUI 注入按钮
- 入口按钮只负责触发，不直接持有业务逻辑

## 为什么先不做 Pad 物品

Pad 物品属于第三入口层，适合后面补：

- 便携终端
- 职业专用终端
- 管理员终端

但它不该阻塞基础入口上线。

当前优先级应为：

1. 快捷键
2. 背包按钮
3. 统一终端壳
4. Pad 物品

## 第一阶段技术策略

为了先把入口和服务端打开链做成可运行状态，第一阶段采用：

- `稳定入口链`
- `占位终端壳`
- `后续再替换为正式终端 GUI 内核`

原因如下：

- 当前仓库尚未落地终端 GUI 依赖与页面体系
- 入口链、网络包、服务端打开逻辑应先稳定
- 这样后续替换 UI 内核时，不需要重做快捷键、背包按钮和网络入口

这不意味着放弃 `ModularUI 2`。
相反，这意味着：

- 第一阶段先验证入口架构
- 第二阶段把终端壳替换为当时选定的正式 GUI 内核
- 第三阶段再逐页填充职业、市场、福利、订单等内容

## 计划中的代码结构

建议新增一组终端相关包：

- `com.jsirgalaxybase.terminal`
  - 终端打开服务、常量、打开请求入口
- `com.jsirgalaxybase.terminal.network`
  - 终端打开请求包
- `com.jsirgalaxybase.terminal.client`
  - 快捷键与背包按钮入口
- `com.jsirgalaxybase.terminal.gui`
  - 第一阶段占位终端 GUI 与容器

## 第一阶段范围

本阶段只实现：

- 快捷键打开终端
- 背包界面按钮打开终端
- 服务端统一打开终端
- 可运行的占位终端窗口

本阶段明确不实现：

- Pad 物品
- 职业正式页面
- 市场正式页面
- 公会正式页面
- 福利正式页面
- 跨服终端同步

## 服务端权威约束

终端打开与后续动作都必须遵守：

- 客户端只请求，不裁决
- 服务端统一决定是否允许打开
- 后续分页切换和业务操作也应沿用相同思路

## 第二阶段预留

等入口稳定后，下一步将切换为真正的终端首页壳：

- 顶部状态条
- 左侧导航
- 中央主内容区
- 首页摘要卡片

届时 UI 内核切换到 `ModularUI 2`，但入口链与服务端打开链保持不变。

## 2026-04-02 第二轮落地补充

在首页与银行子页第一轮结构收口之后，终端已经进入“交互壳层”补齐阶段。

本轮已把下面这些能力正式落到终端实现：

- 统一通知模型，银行操作反馈不再只是一段散落文本
- 终端内通知浮层，继续保留页内快速反馈
- 终端外 HUD 通知，允许玩家关闭终端后继续看到结果提示
- 玩家转账确认弹窗，避免直接把当前表单无确认地提交到服务端
- 轻量视觉状态卡，使用真实物品图标给银行状态加语义锚点

这意味着终端后续再接市场页或其他服务页时，不必从零重复搭通知、弹层和基础视觉组件。

## 2026-04-02 第三轮工程收口补充

终端现在已经具备可扩展的页面装配与交互壳层。

当前稳定落点已经明确分成下面几层：

- `TerminalHomeGuiFactory` 负责总装配、页面路由和入口壳
- `TerminalBankPageBuilder` 负责银行子页构建与确认弹窗接线
- `TerminalWidgetFactory` 负责通用 section、文本、数据行和按钮壳
- `TerminalBankSessionController` 与 `TerminalBankSyncBinder` 负责银行本地会话状态和 sync 绑定

这意味着市场 GUI 后续接入时，不需要再把页面细节继续堆回主工厂中段。

## 2026-04-14 GUI 内核路线调整：从 ModularUI 2 转向内置 BetterQuesting 风格框架

本节只保留路线结论与文档索引，具体集成方案已经独立沉淀到：

- `docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`

当前正式结论如下：

- 当前仓库里已经落地的终端 GUI 仍然基于 `ModularUI 2`
- 但它不再是后续继续扩张的默认长期方案
- 终端长期方向改为：在当前仓库内 vendoring 一套去 BetterQuesting 业务耦合后的 GUI framework
- 迁移目标是保留现有终端入口链、服务端权威和业务服务层，只替换 GUI 内核与客户端装配方式
- 迁移的关键不只是控件替换，还包括把打开链从“服务端直接开 `ModularUI`”改成“服务端授权 + 客户端开 `GuiScreenCanvas` 风格 screen”

如果后续开始真正实装，应以独立集成方案文档为准，而不是继续在本文件里追加实现细节。

## 2026-04-25 Phase 8 cutover 补充

终端正式玩家入口已经切到新 BetterQuesting 风格打开链：

- G 键入口与背包按钮入口默认发送 `OpenTerminalRequestMessage`
- 服务端继续通过 `TerminalService.approveTerminalClientScreen(...)` 授权并生成初始 snapshot / session token
- 客户端收到 `OpenTerminalApprovedMessage` 后由 `TerminalClientScreenController` 打开或刷新 `TerminalHomeScreen`
- 旧 `OpenTerminalMessage`、`TerminalService.openTerminal(...)` 与 `TerminalHomeGuiFactory` 仅作为 legacy fallback 保留到 phase 9，不再是正式入口默认路径

## 2026-04-25 Phase 9 删除旧 terminal ModularUI 过渡实现

Phase 9 已完成终端侧旧 GUI 过渡层删除：

- `TerminalNetwork` 只注册 `OpenTerminalRequestMessage`、`OpenTerminalApprovedMessage`、`TerminalActionMessage` 与 `TerminalSnapshotMessage`
- `TerminalService` 不再保留旧 `openTerminal(...)` / legacy fallback 打开方法，也不再依赖旧 GUI factory
- `TerminalModule` 不再注册 terminal 专属 `ModularUI` factory
- 旧 terminal builder / binder / sync state / session controller / dialog / widget factory 已从生产代码中移除
- 保留的 `terminal.ui` 类只承担新壳仍在使用的服务、快照、通知、页面枚举和 market quote 读模型职责
