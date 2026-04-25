# Terminal BetterQuesting 风格 UI 框架集成方案

日期：2026-04-14

## 文档目的

这份文档专门描述 `JsirGalaxyBase` 终端从当前 `ModularUI 2` 实现，迁移到“仓库内置 BetterQuesting 风格 GUI 框架”的集成方案。

它不是终端业务需求文档，也不是银行/市场功能设计文档。

它只回答下面这些问题：

- 为什么终端 GUI 长期内核不再继续押注 `ModularUI 2`
- 为什么要采用 BetterQuesting 风格的 `GuiScreenCanvas + panel tree + theme registry`
- 现有终端代码里哪些层保留，哪些层必须重写
- 如何把 BetterQuesting 的 GUI framework 安全 vendoring 到当前仓库
- 新终端打开链、消息协议、页面装配和主题资源应如何落地
- 后续真正开始实装时应按什么顺序推进

## 结论先行

终端 GUI 的集成方案不是“继续在当前 `TerminalHomeGuiFactory` 外面套一层 BetterQuesting 外观”，而是：

1. 保留现有终端入口链、服务端权威、业务服务层和页面语义
2. vendoring BetterQuesting 的最小 GUI framework 子集到 `com.jsirgalaxybase` 自己的命名空间
3. 把终端 GUI 从 `ModularUI` 的同步容器模型，重构为 client-side `GuiScreenCanvas` + 显式 packet 驱动的 screen 模型
4. 先迁首页壳和共用组件，再迁银行页，再迁市场三分页
5. 最终清理旧 `ModularUI` 终端实现与终端专属依赖

这是一轮 GUI 内核替换，不是银行、市场或 server tools 业务语义重写。

## 一、为什么不是继续深化 ModularUI 2

当前终端使用 `ModularUI 2` 已经证明可以快速搭出运行中的 GUI，但它不适合作为长期统一终端的自有框架，原因有三层。

### 1. 共享运行时依赖风险

- `ModularUI 2` 在 GTNH 中不是 `JsirGalaxyBase` 独占依赖，而是共享运行时依赖
- 之前已经验证过，上游版本升级会引发 pack 级 ABI 断裂，而不是只影响终端自身
- 这意味着终端 GUI 的演化会继续受制于整包兼容窗口，而不是仓库自己控制节奏

### 2. 当前终端结构已经深绑 ModularUI 同步模型

现有终端 GUI 不只是用了几个 `widget`，而是整套页面装配方式都建在下面这些能力上：

- `AbstractUIFactory<GuiData>`
- `GuiManager.open(...)`
- `PanelSyncManager`
- `StringSyncValue` / `IntSyncValue`
- `Dialog`、`TextFieldWidget`、`Flow`、`ListWidget`

如果继续在这套模型上加页，只会让未来迁移成本继续变高。

### 3. BetterQuesting 更接近“应用壳”而不是“同步面板”

BetterQuesting 提供的是：

- `GuiScreenCanvas` 根屏幕
- `IGuiPanel`/canvas/panel tree 结构
- popup 生命周期
- 主题、切片纹理、颜色和线条注册
- 可承载全屏、导航、多面板、滚动容器的应用式布局

这更适合终端长期目标：统一承载银行、市场、通知、后续 server tools 以及更多制度功能。

## 二、集成目标与边界

### 保留不动的部分

下面这些层应尽量保留，作为新 GUI 的既有业务基础：

- 终端双入口：`TerminalKeyHandler`、`TerminalInventoryButtonHandler`
- 客户端注册入口：`TerminalClientBootstrap`
- 服务端权威原则：客户端只请求，服务端决定能否打开终端
- 业务服务层：`TerminalBankingService`、`TerminalMarketService`
- 快照提供层：`TerminalHomeSnapshotProvider`、`TerminalBankSnapshotProvider`
- 页面语义与路由边界：`TerminalPage`
- 通知语义：`TerminalNotification`、`TerminalHudNotificationManager`

### 必须重写的部分

下面这些类和职责属于当前 `ModularUI` 终端的框架耦合层，应作为新 GUI 集成的主要改造面：

- `TerminalHomeGuiFactory`
- `TerminalWidgetFactory`
- `TerminalBankPageBuilder`
- `TerminalMarketPageBuilder`
- `TerminalDialogFactory`
- `TerminalBankSyncBinder`
- `TerminalMarketSyncBinder`
- `TerminalCustomMarketSyncBinder`
- `TerminalExchangeMarketSyncBinder`
- `TerminalBankSyncState`
- `TerminalMarketSyncState`
- `TerminalCustomMarketSyncState`
- `TerminalExchangeMarketSyncState`
- `TerminalService` 中“服务端直接开 `ModularUI`”的部分
- `TerminalModule` 中 factory 注册部分
- `TerminalHudOverlayHandler` 中识别 `ModularUI` screen 的部分

### 本轮明确不做的事

- 不直接重写银行业务规则
- 不直接重写市场业务规则
- 不在这一轮引入 BetterQuesting quest / party / editor / 配置体系
- 不把 BetterQuesting 整个 mod 当成运行时依赖
- 不把原 BetterQuesting 包结构整包照搬到仓库里直接使用

## 三、建议的集成架构

## 1. Vendoring 结构

建议新增以下包：

- `com.jsirgalaxybase.client.gui.framework`
  - `GuiScreenCanvas`
  - `IScene`
  - `IGuiPanel`
  - 基础 canvas/panel
  - 基础按钮、滚动容器、popup、事件广播
- `com.jsirgalaxybase.client.gui.theme`
  - 终端主题注册
  - 纹理、颜色、线条、图标预设
- `com.jsirgalaxybase.terminal.client.screen`
  - `TerminalHomeScreen`
  - `TerminalBankScreenSection`
  - `TerminalMarketHomeSection`
  - `TerminalStandardMarketSection`
  - `TerminalCustomMarketSection`
  - `TerminalExchangeMarketSection`
- `com.jsirgalaxybase.terminal.client.viewmodel`
  - 页面快照 DTO
  - 当前选中态
  - 表单输入态
  - 本地通知态
- `com.jsirgalaxybase.terminal.network`
  - 打开终端请求/授权消息
  - 动作消息
  - 快照消息
  - 通知消息

## 2. BetterQuesting 代码引入策略

采用“最小 framework vendoring”，不采用“运行时依赖 BetterQuesting mod”。

### 第一批建议引入的最小范围

- `GuiScreenCanvas`
- `IScene`
- `IGuiPanel`
- `CanvasEmpty`
- `CanvasTextured`
- 至少一个滚动容器
- 基础按钮控件
- popup 支持
- 最小 theme registry
- 纹理/颜色/线条资源接口

### 第一批明确不要引入的部分

- `QuestingAPI`
- `BQ_Settings`
- `QuestTranslation`
- `ConfigHandler`
- `PresetGUIs` 的 BetterQuesting 页面注册逻辑
- quest / party / NBT editor / 文件浏览等业务页面

### Vendoring 的具体规则

- 全部改到 `com.jsirgalaxybase` 自己的包名下
- 所有 BetterQuesting 全局单例依赖要去耦
- 所有资源路径改成 `JsirGalaxyBase` 自己的资源命名
- 不允许保留对 BetterQuesting 主 mod 类的编译依赖

## 四、打开链与协议集成方案

这一部分是整个集成方案里最关键的变化。

当前模型：

- 客户端发 `OpenTerminalMessage`
- 服务端执行 `TerminalService.openTerminal(player)`
- 服务端直接 `GuiManager.open(...)`
- GUI 运行期依赖 `PanelSyncManager`

新模型应改成显式消息驱动：

### 1. 打开链

- 客户端发 `OpenTerminalRequestMessage`
- 服务端校验玩家状态、世界状态、权限与可用性
- 服务端返回 `OpenTerminalApprovedMessage`
- 客户端收到后执行 `displayGuiScreen(new TerminalHomeScreen(initialSnapshot))`

### 2. 初始化快照

`OpenTerminalApprovedMessage` 至少应包含：

- 当前首页快照
- 初始页面枚举值
- 首页摘要状态
- 通知或错误提示
- 终端实例标识或 session token

### 3. 后续动作消息

统一使用显式 action 包，而不是 `PanelSyncManager` 的 `registerSyncedAction(...)`。

建议格式：

- `TerminalActionMessage`
  - `sessionToken`
  - `page`
  - `actionType`
  - `payloadJson` 或结构化字段

### 4. 快照回写消息

建议拆两类：

- `TerminalSnapshotMessage`
  - 回写整页或子页快照
- `TerminalToastMessage`
  - 回写轻量通知，不强制重刷整页

### 5. 为什么不继续照搬原同步模型

因为 BetterQuesting 风格 screen 是 client-side `GuiScreen`，没有 `ModularUI` 的容器同步生命周期；如果硬把旧同步模型迁进去，只会得到一套更难维护的半成品框架。

## 五、终端页面装配集成方案

## 1. 根屏幕

建议新增 `TerminalHomeScreen extends GuiScreenCanvas`，它只负责：

- 顶部状态带
- 左侧导航
- 当前 page section 宿主
- popup 生命周期
- 全局通知浮层

它不直接承担银行和市场全部业务细节。

## 2. Section 化页面

建议把当前 builder 模式改成 section 模式：

- 首页 section
- 银行 section
- 市场总览 section
- 标准商品市场 section
- 定制商品市场 section
- 汇率市场 section

每个 section 负责：

- 根据 view model 组装 panel tree
- 派发本页动作
- 处理本页弹层
- 尽量不直接碰服务端业务对象

## 3. 对现有类的映射建议

- `TerminalWidgetFactory` -> `TerminalPanelFactory` 或 `TerminalThemeComponents`
- `TerminalDialogFactory` -> `TerminalPopupFactory`
- `TerminalBankPageBuilder` -> `TerminalBankSection`
- `TerminalMarketPageBuilder` -> `TerminalMarket*Section`

## 六、主题与资源集成方案

终端采用 BetterQuesting 风格框架，不等于直接复用 BetterQuesting 的主题资源命名。

建议：

- 在 `com.jsirgalaxybase.client.gui.theme` 下建立自己的主题注册器
- 第一版先只支持一套终端主题
- 但资源接口按多主题能力设计，避免后面重写

第一版主题至少要覆盖：

- 主背景 panel
- 次级 panel
- 高亮边框
- 按钮正常/悬浮/禁用态
- 提示色与风险色
- 图标槽与物品槽壳层

这部分可以参考 BetterQuesting 的切片纹理思路，但不要继续保留 `PresetGUIs` 那种和原 mod 页面注册绑定的结构。

## 七、分阶段集成顺序

### 阶段 0：冻结旧终端页面语义

- 不再继续扩大 `TerminalHomeGuiFactory` 新能力
- 把现有页面、动作名、通知名、路由名固定为迁移基准

### 阶段 1：引入最小 framework

- vendoring `GuiScreenCanvas`、panel/canvas、滚动容器、popup、theme registry 最小集
- 去掉全部 BetterQuesting 全局依赖
- 跑通一张纯占位 `TerminalHomeScreen`

### 阶段 2：改造终端打开链

- 保留快捷键和背包按钮入口
- 重写终端打开协议
- 让客户端可以在授权后自行打开终端 screen

### 阶段 3：迁首页壳和共用组件

- 顶部状态带
- 左侧导航
- 全局通知
- popup
- 共用 panel 组件

### 阶段 4：迁银行页

- 开户
- 刷新
- 转账
- 确认弹窗
- HUD 通知

银行页应作为第一张完整业务页，因为它的数据结构和动作边界最清晰。

### 阶段 5：迁市场页

- `MARKET` 总览
- `MARKET_STANDARDIZED`
- `MARKET_CUSTOM`
- `MARKET_EXCHANGE`

市场页迁移顺序建议先只读摘要，再有输入动作的交易详情。

### 阶段 6：移除旧终端 ModularUI 实现

- 删除 `TerminalHomeGuiFactory`
- 删除旧 builder/binder/state
- `TerminalModule` 不再注册 `GuiManager` factory
- `TerminalHudOverlayHandler` 改识别新 screen 基类
- 评估 `dependencies.gradle` 中是否还能完全移除终端对 `ModularUI2` 的依赖

## 八、首批推荐实施清单

如果下一轮开始真正做代码，建议先做下面 6 件事：

1. 列出 BetterQuesting framework 最小 vendoring 白名单
2. 新增 `com.jsirgalaxybase.client.gui.framework` 与 `com.jsirgalaxybase.client.gui.theme` 包
3. 建立 `TerminalHomeScreen` 占位页
4. 重写打开终端协议为“服务端授权 + 客户端开屏”
5. 先迁 `TerminalDialogFactory` 与通知浮层
6. 先迁银行页，不先迁市场页

## 九、主要风险

- `GuiScreenCanvas` 是 client-side screen，协议设计不清会导致输入、按钮和快照一致性重新出问题
- vendoring 时如果残留 BetterQuesting 全局设置和翻译依赖，后续仍会被原工程假设反噬
- 如果在旧 `terminal/ui` 目录直接混入新 framework，最后会形成双套 UI 壳交叉污染
- 如果在迁移前不固定动作名和快照边界，银行页和市场页会一边迁移一边漂移

## 十、验收标准

完成整轮集成后，至少应满足：

- 按 `G` 或背包按钮都能打开新的终端 screen
- 首页、银行页、市场页都运行在仓库自有 GUI framework 上
- 开户、转账、买卖、撤单、领取、兑换都走显式终端消息协议
- 终端主题、切片纹理、popup、滚动列表由仓库自有代码控制
- `JsirGalaxyBase` 终端不再把 `ModularUI 2` 作为默认长期 GUI 内核

## 参考来源

- `Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/GuiScreenCanvas.java`
- `Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/IGuiPanel.java`
- `Reference/BetterQuesting/src/main/java/betterquesting/client/themes/ThemeRegistry.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`
- `src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalHudOverlayHandler.java`