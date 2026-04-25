# Terminal GUI 第一轮重构 Prompt（含 ModularUI2 API 参考）

你现在接手的是 JsirGalaxyBase 终端 GUI 的第一轮结构化重构，不是第二层市场逻辑开发，也不是银行业务重构。本轮目标是先把当前终端页面从“能显示但容易溢出、卡片高度僵硬、长文本体验差”的状态，收口到“文本不轻易爆框、布局以弹性流式为主、后续可继续长成桌面化终端”的状态。

## 先看这些现有文件

1. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`
2. `src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java`
3. `src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`
4. `src/main/java/com/jsirgalaxybase/terminal/TerminalInventoryButtonHandler.java`
5. `dependencies.gradle`
6. `docs/banking-terminal-gui-design.md`
7. `docs/terminal-plan.md`

## 已确认的框架事实（务必基于这些事实做设计）

当前终端 GUI 基于 ModularUI2，而不是原生 `GuiScreen` 手工绘制。

依赖来源：

- `dependencies.gradle` 中已经声明 `ModularUI2:2.3.45-1.7.10`

当前项目实际已使用的核心类型包括：

- `ModularPanel`
- `ModularScreen`
- `Flow`
- `ParentWidget`
- `ListWidget`
- `TextWidget`
- `TextFieldWidget`
- `ButtonWidget`

已从本地开发包确认的 ModularUI2 API 能力摘要如下。

### 1. 布局能力

框架不是只能写死坐标。

已确认可用：

- `Flow.row()` / `Flow.column()` 流式布局
- `childPadding(...)`
- `mainAxisAlignment(...)`
- `crossAxisAlignment(...)`
- `wrap(...)`
- `collapseDisabledChild(...)`
- 通用 `IPositioned` 能力：`widthRel`、`heightRel`、`sizeRel`、`expanded`、`coverChildrenHeight`、`coverChildrenWidth`、`align`、`center`、`padding`、`margin`

结论：

当前“像写死坐标”的主要原因不是框架不支持弹性布局，而是现有页面虽然用了 `Flow`，但内部大量区块仍然套了固定高度卡片和固定文本高度。

### 2. 文本能力

已确认可用：

- `TextWidget.maxWidth(int)`
- `TextWidget.scale(float)`
- `TextWidget.alignment(...)`
- `TextWidget.shadow(...)`
- `RichTextWidget`
- `ScrollingTextWidget`

结论：

当前文本越界并不是框架无能，而是现有代码几乎全部仍在使用普通 `TextWidget`，且没有使用 `maxWidth`、`RichTextWidget` 或 `ScrollingTextWidget` 去处理长文本。

### 3. 输入与提示能力

已确认可用：

- `TextFieldWidget.hintText(...)`
- `TextFieldWidget.setMaxLength(...)`
- `TextFieldWidget.setPattern(...)`
- `TextFieldWidget.setValidator(...)`
- `TextFieldWidget.setNumbersLong(...)`
- `TextFieldWidget.tooltip(...)`
- `RichTooltip`

结论：

输入框、悬浮提示、复杂 tooltip 已经够做正式交互页，不需要另造一层基础控件系统。

### 4. 图片 / 资源 / 内容展示能力

已确认可用：

- `UITexture.fullImage(...)`
- `UITexture.getSubArea(...)`
- `Rectangle`
- `ItemDisplayWidget`
- `FluidDisplayWidget`
- `EntityDisplayWidget`
- `ItemSlot`
- `FluidSlot`
- `ProgressWidget`

结论：

后续终端页支持贴图、图标、物品展示、流体展示、实体展示、进度条是没问题的。

### 5. 弹层 / 菜单 / 子面板能力

已确认可用：

- `Dialog`
- `SecondaryPanel`
- `DropDownMenu`
- `ContextMenuButton`
- `PagedWidget`
- `PageButton`
- `MenuPanel`

结论：

后续做二级弹窗、轻量通知面板、右键菜单、分页路由都具备框架基础。

### 6. 声音与通知能力

已确认可用：

- `ButtonWidget.playClickSound(...)`
- `ButtonWidget.clickSound(...)`
- 游戏内 tooltip / dialog / secondary panel / overlay 体系

结论：

框架支持“游戏内交互声音挂钩”和“游戏内弹层式通知”，但它不是桌面操作系统通知框架。若要做更复杂声音播放，应该在交互回调中调用 Minecraft 原生声音接口，而不是期待 ModularUI2 提供完整音频系统。

## 当前实现的主要问题（本轮必须正视）

问题不是单点，而是一组风格问题叠加。

### A. 大量固定高度卡片让页面非常僵硬

当前 `TerminalHomeGuiFactory` 中大量区块直接写死高度，例如：

- 首页卡片 54 / 68 / 108
- 银行页卡片 76 / 86 / 100 / 108 / 122 / 128
- `createSectionShell(...)` 还把固定高度作为主入口参数

结果：

- 文案略长就会挤爆
- 后续加图标、按钮、提示会非常难扩
- 不同屏幕比例下观感不稳定

### B. 长文本仍然几乎全用普通 `TextWidget`

当前大量说明文、摘要文、状态文都用普通 `TextWidget`，同时配合固定 `height(9~12)`。

结果：

- 长文本容易越界
- 一旦后续改为真实动态文案，溢出会更频繁

### C. 主体布局虽是流式，但组件壳层不够“内容优先”

也就是说：

- 外层用了 `Flow`
- 但内层区块仍然按静态海报拼接
- 这让 UI 看上去更像静态拼板，而不是可成长的终端系统

### D. 真正写死坐标的地方主要在终端入口按钮

`TerminalInventoryButtonHandler` 里背包按钮位置仍然是通过原版 GUI 左上角偏移后手工写入的。这部分先记录为后续问题，但本轮不要求把它一起大改。

## 本轮重构目标

只做第一轮结构化收口，不要一步到位做成完整“操作系统桌面”。

本轮目标只有四件事：

1. 收口文本越界问题
2. 把页面主体从“固定高度优先”调整为“内容与滚动优先”
3. 为后续图片、通知、弹层、市场操作页预留更健康的结构
4. 不破坏现有终端入口、银行交互、页面路由与同步逻辑

## 本轮建议实施方案

### 第一部分：先抽一层文本组件策略

在 `TerminalHomeGuiFactory` 内优先新增一组统一文本构造方法，至少包括下面几类：

1. 单行短标题组件
2. 单行动态值组件
3. 可限制宽度的状态行组件
4. 长说明文本组件
5. 需要跑马灯或横向滚动的摘要组件

建议方向：

- 短标签仍可保留 `TextWidget`
- 动态长文本优先考虑 `TextWidget.maxWidth(...)`
- 明显可能超过单行宽度的说明文案优先考虑 `RichTextWidget`
- 必须保持单行但内容可能偏长的摘要优先考虑 `ScrollingTextWidget`

本轮不要继续在每个业务区块里随手 new 一堆裸 `TextWidget`。

### 第二部分：重构 `createSectionShell(...)`

把 `createSectionShell(...)` 从“固定高度容器工厂”改成“可选固定高度、默认内容自适应”的容器工厂。

要求：

1. 保留兼容接口也可以，但内部要能支持不传固定高度时走 `coverChildrenHeight()` 或 `expanded()` 的弹性路径
2. 页面正文中的大多数信息块，优先改成内容自适应
3. 只有确实需要视觉统一的模块，再保留固定高度

### 第三部分：先改最容易爆框的几个区域

本轮至少优先改下面这些区域：

1. 页头 lead 文案
2. 银行页中的“当前说明”“读取说明”“当前规则”这类说明块
3. toast overlay 的正文
4. `createBulletPanel(...)` / `createBulletLine(...)`
5. `createDataRow(...)` 中动态值行

这些区域最容易在后续真实数据接入后出现溢出或视觉拥挤。

### 第四部分：保留现有路由和同步，不扩业务

以下内容本轮不要动或只允许最小触碰：

1. `selectedPageSync` 的页路由机制
2. `TerminalBankSessionState` 和 `BankPageSyncState` 的同步模型
3. `TerminalService.openTerminal(...)`
4. `TerminalModule` 的 GUI 注册方式
5. 银行动作本身的业务语义

本轮是 GUI 壳层结构收口，不是银行/市场业务改造。

## 明确禁止做的事

1. 不要在本轮直接开做市场 GUI 功能页
2. 不要顺手重写整个终端架构
3. 不要把背包入口按钮坐标系统一起大改
4. 不要引入新的网络协议
5. 不要更改已有银行业务流程
6. 不要把本轮变成“做一个完整桌面系统”

## 本轮完成后应达到的效果

完成后至少应满足下面这些验收点：

1. 长文本不再轻易超出内容框
2. 说明型文案不再依赖脆弱的固定高度 9/10/11/12 文本盒
3. 多数正文卡片改为内容优先，而不是写死高度优先
4. 滚动区域与内容容器关系更清晰
5. 终端视觉仍保持当前深蓝灰主题，不要求完全重做美术
6. 现有银行子页跳转、开户、转账、摘要同步行为不被破坏

## 推荐的落地顺序

1. 先抽文本 helper
2. 再改 `createSectionShell(...)`
3. 再改最容易溢出的页面块
4. 最后做一次统一清理，把零散的固定文本高度和不必要的固定卡片高度压掉一轮

## 文档要求

1. 更新 `docs/WORKLOG.md`
2. 如有必要，可在 `docs/terminal-plan.md` 里补一句“已完成第一轮 GUI 结构收口，后续进入通知层/市场页/图形化组件增强”
3. 不要求额外扩写长篇制度文档

## 测试与验证要求

至少做下面这些验证：

1. 编译通过
2. 终端能正常打开
3. 导航切页正常
4. 银行主页、账户页、转账页、Exchange 页、账本页都能正常显示
5. 选择一些故意偏长的文案，确认不再明显爆框

如果仓库已有相关测试可跑，优先保持现有测试继续通过；如果本轮没有现成 GUI 自动化测试，也至少给出手工验证清单。

## 交付时请按这个格式回报

1. 本轮具体重构了哪些 GUI 结构
2. 哪些文本区域从普通 `TextWidget` 升级成了更适合长文本的组件
3. 哪些卡片从固定高度改成了内容优先
4. 还有哪些 GUI 能力已经确认框架支持，但本轮刻意没做

## 给实现者的最终提醒

这轮不是为了把终端做得更花，而是为了给后续真正的市场 GUI、图片化终端、通知中心、二级弹层留出生长空间。请优先追求结构健康，而不是继续堆页面拼板。