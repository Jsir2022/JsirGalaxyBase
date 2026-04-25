# Terminal BetterQuesting 风格 UI 框架第一阶段执行 Prompt

日期：2026-04-14

下面这份 prompt 可直接交给另一个 AI，用于在 `JsirGalaxyBase` 中落地第一阶段的 BetterQuesting 风格 UI framework。

这轮不是终端迁移，不是银行页迁移，不是市场页迁移，也不是协议重构收口。

这轮只做一件事：

- 先把仓库自有的 BetterQuesting 风格 GUI framework 基础层落下来，并验证它可以独立运行

---

你正在 `JsirGalaxyBase` 仓库中工作。你的任务不是继续评估，而是直接落地终端 GUI 新框架的第一阶段基础设施。

## 本轮唯一目标

在不迁移现有 terminal 业务页面的前提下，把 `Reference/BetterQuesting` 中终端后续需要的最小 GUI framework 子集 vendoring 到当前仓库，去掉 BetterQuesting 业务耦合，并跑通一个最小可显示的占位 screen。

这轮的重点是“先解决 UI framework”，而不是“顺手把 terminal 一起迁了”。

## 开始前必须先读

开始编码前，先完整阅读并遵守下面这些文档：

- `README.md`
- `docs/terminal-plan.md`
- `docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`
- `docs/WORKLOG.md`

必须参考但不能整包照搬的源码：

- `Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/GuiScreenCanvas.java`
- `Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/IScene.java`
- `Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/IGuiPanel.java`
- `Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/CanvasEmpty.java`
- `Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/CanvasTextured.java`
- `Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/controls/PanelButton.java`
- `Reference/BetterQuesting/src/main/java/betterquesting/client/themes/ThemeRegistry.java`

## 本轮必须完成的内容

### 1. 落地最小 vendored GUI framework

至少新增一套当前仓库自己的 GUI framework 基础层，建议落在：

- `src/main/java/com/jsirgalaxybase/client/gui/framework/`
- `src/main/java/com/jsirgalaxybase/client/gui/theme/`

第一阶段至少应包含下面这些能力：

- 一个 `GuiScreen` 根屏幕基类，语义等价于 `GuiScreenCanvas`
- 一个 scene / panel 树的最小接口层
- 一个基础 panel 容器
- 一个贴图 canvas
- 一个基础按钮控件
- 一个最小 popup 或浮层承载能力
- 一个最小 theme registry 或等价的主题资源访问层
- 至少一组最小纹理 / 颜色资源接口

注意：

- 名称可以按当前仓库风格调整，不必强行与 BetterQuesting 同名
- 但结构职责必须清晰，不要把所有类堆进一个文件

### 2. 去掉 BetterQuesting 业务耦合

vendoring 时必须去掉下面这些 BetterQuesting 特有依赖：

- `QuestingAPI`
- `BQ_Settings`
- `QuestTranslation`
- `ConfigHandler`
- `BetterQuesting` 主 mod 类
- BetterQuesting 自己的 quest / party / NBT editor / 文件浏览页面注册逻辑

要求：

- 不允许引入 BetterQuesting mod 作为运行时依赖
- 不允许保留对 BetterQuesting 包名的直接编译依赖
- 不允许在新 framework 里继续假设 BetterQuesting 的全局配置和翻译系统存在

### 3. 建立最小可运行的主题与资源骨架

第一阶段不要求把终端正式美术全部做完，但至少要有一套最小可运行主题能力，用来证明这套 framework 不是裸 `GuiScreen` 手搓散件。

至少需要：

- 主 panel 背景资源访问
- 基础按钮颜色或纹理状态
- 文本主色 / 次色
- 一个默认主题入口

如需贴图，可以先用仓库内可控的最小占位资源，但资源路径必须属于 `JsirGalaxyBase` 自己，而不是继续引用 BetterQuesting 原资源。

### 4. 跑通一个最小占位 screen

必须新增一个最小可运行的占位 screen，用于证明第一阶段 framework 已经具备基本可用性。

建议新增：

- `TerminalFrameworkTestScreen`
  或
- `TerminalHomeScreen` 的极简占位版

这个 screen 至少要验证：

- 根屏幕可打开
- 至少一个 panel 能绘制
- 至少一个按钮能挂上点击回调
- 至少一个 popup 或二级层能打开 / 关闭
- 至少一个主题资源能被正常读取并显示

注意：

- 这个 screen 目前不要求接银行、市场或 terminal 打开链
- 它可以是独立测试屏幕或开发验证屏幕
- 但不能只是空类或只打印日志，必须真的走到 GUI framework 的绘制与交互链

### 5. 补第一阶段文档与 WORKLOG

本轮实际代码变更后，必须同步更新：

- `docs/WORKLOG.md`

如有必要，也可以补：

- `docs/README.md`
- `docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`

但不要把本轮文档继续扩写成新的终端业务设计稿。

## 本轮明确不做什么

下面这些内容本轮不要碰：

### 1. 不迁移 terminal 业务页面

不要开始迁移：

- `TerminalHomeGuiFactory`
- `TerminalBankPageBuilder`
- `TerminalMarketPageBuilder`
- 银行页
- 市场总览页
- 标准商品市场页
- 定制商品市场页
- 汇率市场页

### 2. 不重写终端打开链

本轮不要开始改：

- `TerminalService.openTerminal(...)`
- `TerminalModule` 的终端 factory 注册链
- `TerminalNetwork`
- `OpenTerminalMessage`

也就是说：

- 不把当前 terminal 从 `ModularUI` 切走
- 不把终端真正接到新 screen 上

### 3. 不做协议重构

本轮不要引入完整的：

- `OpenTerminalApprovedMessage`
- `TerminalActionMessage`
- `TerminalSnapshotMessage`

如果你为了占位 screen 的本地验证需要极薄的开发辅助入口，可以做，但不要借机把整套终端协议提前写一半。

### 4. 不整包复制 BetterQuesting

明确禁止：

- 整包复制 `betterquesting.api2.client.gui`
- 整包复制 `betterquesting.client`
- 整包复制 `betterquesting.handlers`
- 在仓库里保留大面积未使用的 BetterQuesting 源码

本轮必须控制 vendoring 范围，只引入终端第一阶段真正用得到的最小 framework 子集。

## 架构要求

### 1. 命名空间必须改为当前仓库自有

所有 vendored framework 代码必须进入 `com.jsirgalaxybase` 自己的命名空间。

不允许保留：

- `betterquesting.*`

作为新 framework 的正式落点。

### 2. framework 与 terminal 业务页面分层

这轮至少要把下面两层分开：

- 通用 GUI framework 层
- terminal 专用 screen / component 层

不要把 framework 类直接塞回 `com.jsirgalaxybase.terminal.ui`，否则后面会和旧 `ModularUI` 终端代码继续混在一起。

### 3. 允许做极简 theme，但不能省掉

这轮不是做正式美术，但必须证明 framework 具备自己的主题访问层。

不要把结果做成：

- 一个能显示文字的裸 `GuiScreen`
- 所有颜色和贴图都硬编码在测试 screen 里

### 4. 尽量为后续 terminal 集成保留自然扩展点

比如：

- 根屏幕可容纳导航和正文 panel
- popup 生命周期不要只服务于一个测试按钮
- theme registry 不要只对一个固定资源名生效

但注意：

- 只需要保留扩展点
- 不要提前把第二阶段终端迁移也做了

## 推荐实施顺序

建议按下面顺序推进：

1. 从 BetterQuesting 中挑出最小 framework 白名单
2. 复制并改名到 `com.jsirgalaxybase.client.gui.framework`
3. 去掉 BetterQuesting 全局依赖和业务耦合
4. 落最小 theme/resource 访问层
5. 做一个可显示、可点击、可弹 popup 的占位 screen
6. 补最小验证和 WORKLOG

## 验收标准

只有同时满足下面条件，这轮才算合格：

1. 当前仓库新增了一套自有命名空间下的 BetterQuesting 风格 GUI framework 基础层
2. 新 framework 不再依赖 BetterQuesting 主 mod、Quest API、设置、翻译和业务页面
3. 至少有一个最小 screen 真正证明 panel、按钮、popup、theme 这四类能力已可运行
4. 当前 terminal 主链仍然保持原状，没有被半迁移到新框架上
5. 没有整包照搬 BetterQuesting 大量无关源码
6. 至少完成一次编译验证；如有对应测试，也应运行最小相关验证
7. `docs/WORKLOG.md` 已补记录

## 建议修改范围

本轮原则上允许修改：

- 新增 `src/main/java/com/jsirgalaxybase/client/gui/framework/**`
- 新增 `src/main/java/com/jsirgalaxybase/client/gui/theme/**`
- 如需占位 screen，可新增 `src/main/java/com/jsirgalaxybase/terminal/client/**`
- 最小必要的资源文件
- 与本轮直接相关的最小测试文件
- `docs/WORKLOG.md`

如果必须为了本地验证补一个极薄的 client-side 调试入口，也只能做最小必要改动，并在最终说明里讲清楚用途。

## 最终回报要求

完成后，请明确汇报：

1. 第一阶段实际 vendoring 了哪些 BetterQuesting framework 类或能力
2. 去掉了哪些 BetterQuesting 全局依赖
3. 新 framework 的正式落点包结构是什么
4. 占位 screen 实际验证了哪些能力
5. 跑了哪些编译或测试验证
6. 哪些内容被明确留到第二阶段，而没有在本轮提前实现

## 给执行 AI 的最后一句话

这轮不是“先迁一点 terminal 页面看看”，而是“先把 UI framework 地基打稳”。

正确做法是：

- 只做最小 framework vendoring
- 只做最小 theme/resource 骨架
- 只做最小占位 screen 验证
- 不提前把 terminal 迁移混进来