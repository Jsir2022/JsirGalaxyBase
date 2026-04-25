# Terminal BetterQuesting 风格 UI 框架第二阶段执行 Prompt

日期：2026-04-14

下面这份 prompt 可直接交给另一个 AI，用于在 `JsirGalaxyBase` 中落地第二阶段的 BetterQuesting 风格终端 UI 集成。

这轮不是银行页迁移，不是市场页迁移，也不是清理旧 `ModularUI` 终端尾巴。

这轮只做一件事：

- 把终端从“服务端直接打开 `ModularUI`”推进到“服务端授权 + 客户端打开自有 screen”的最小可运行打开链

---

你正在 `JsirGalaxyBase` 仓库中工作。请直接修改现有 terminal 相关实现，把第一阶段已经落下来的 BetterQuesting 风格 framework 地基，推进到第二阶段的真实终端打开链。

## 本轮唯一目标

在不迁移现有银行页、市场页与其他 terminal 业务页面的前提下，把终端打开链从当前的 `OpenTerminalMessage -> TerminalService.openTerminal(player) -> TerminalHomeGuiFactory.INSTANCE.open(player)`，改造成“客户端请求、服务端授权、客户端自行打开新 framework screen”的最小闭环。

重点是：

- 先把打开协议和 screen 生命周期立住
- 暂时只接一个极简 `TerminalHomeScreen` 占位壳
- 不顺手开始迁银行页和市场页

## 开始前必须先读

开始编码前，先完整阅读并遵守下面这些文档：

- `README.md`
- `docs/terminal-plan.md`
- `docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`
- `docs/terminal-betterquesting-ui-phase1-framework-prompt-2026-04-14.md`
- `docs/WORKLOG.md`

必须先看当前终端主链代码：

- `src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalClientBootstrap.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`
- `src/main/java/com/jsirgalaxybase/terminal/network/TerminalNetwork.java`
- `src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalMessage.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalKeyHandler.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalInventoryButtonHandler.java`

必须先看第一阶段已落好的 framework 地基：

- `src/main/java/com/jsirgalaxybase/client/gui/framework/CanvasScreen.java`
- `src/main/java/com/jsirgalaxybase/client/gui/framework/PanelContainer.java`
- `src/main/java/com/jsirgalaxybase/client/gui/framework/TexturedCanvasPanel.java`
- `src/main/java/com/jsirgalaxybase/client/gui/framework/ButtonPanel.java`
- `src/main/java/com/jsirgalaxybase/client/gui/framework/ModalPopupPanel.java`
- `src/main/java/com/jsirgalaxybase/client/gui/theme/TerminalThemeRegistry.java`
- `src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalFrameworkTestScreen.java`

## 本轮必须完成的内容

### 1. 重写终端打开协议为“服务端授权 + 客户端开屏”

本轮必须新增一条新的终端打开链，至少包含：

- `OpenTerminalRequestMessage`
- `OpenTerminalApprovedMessage`

要求：

- 客户端快捷键和背包按钮仍然从 client 发起请求
- 服务端负责校验玩家是否允许打开终端
- 服务端不再直接调用 `GuiManager.open(...)` 去打开新 UI
- 客户端收到授权消息后，自行 `displayGuiScreen(new TerminalHomeScreen(...))`

注意：

- 消息名可以微调，但语义必须清楚
- 不要继续复用 `PanelSyncManager` 去模拟这条新协议

### 2. 新增最小 `TerminalHomeScreen` 占位壳

这轮必须新增一个真实挂在新打开链上的终端根屏，而不是继续复用 `TerminalFrameworkTestScreen`。

建议新增：

- `src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`

这个 screen 目前只需要承担：

- 顶部标题或状态带
- 最小首页摘要区
- 一个明确的“当前仍处于 phase 2 占位壳”提示
- 至少一个关闭按钮

它必须明确是“终端新主链的占位根屏”，不是 framework 调试屏。

### 3. 定义最小初始化快照 / screen model

`OpenTerminalApprovedMessage` 不能只回一个布尔值。

至少要带下面这些字段中的最小子集：

- 初始页面标识
- 终端标题或终端实例名
- 首页摘要文本或状态摘要
- session token 或等价终端会话标识

要求：

- 字段尽量显式，不要全塞进一段自由字符串
- 不要求一步到位做完整银行/市场 snapshot 体系
- 但必须为后续 `TerminalActionMessage / TerminalSnapshotMessage / TerminalToastMessage` 留出自然扩展位

### 4. 保留旧 terminal 主链作为过渡，不提前迁业务页

本轮目标是建立新打开链，不是强拆旧页面。

要求：

- 旧 `TerminalHomeGuiFactory`
- 旧 `TerminalBankPageBuilder`
- 旧 `TerminalMarketPageBuilder`
- 旧 `PanelSyncManager` 业务页同步链

都不要开始迁移业务逻辑。

允许的做法：

- 旧链仍保留在仓库中作为过渡实现
- 新链先独立跑通新 `TerminalHomeScreen`

不允许的做法：

- 把银行页或市场页半迁到新 screen
- 让新旧两套打开链在同一请求里同时打开两种 UI

### 5. 收口第一阶段遗留的调试入口边界

如果当前仓库里仍保留 `TerminalFrameworkTestScreen` 的 F8 调试入口，本轮必须把它收口成不会污染正式终端打开链的状态。

可接受做法包括：

- 改成明确的 debug 开关控制
- 只在开发配置下注册
- 或保留文件但不再默认挂入正式终端 client bootstrap

要求：

- 正式终端入口不再依赖 F8 调试屏
- phase 2 的新打开链必须走真实终端请求协议，而不是调试热键

### 6. 补第二阶段文档与 WORKLOG

本轮实际代码变更后，必须同步更新：

- `docs/WORKLOG.md`
- `docs/README.md`

如有必要，也可以轻量更新：

- `docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`

但不要把这轮再扩写成完整业务迁移设计稿。

## 本轮明确不做什么

### 1. 不迁银行页和市场页

不要开始迁：

- `TerminalBankPageBuilder`
- `TerminalMarketPageBuilder`
- 银行开户 / 转账页
- 标准商品市场页
- 定制商品市场页
- 汇率市场页

### 2. 不做完整动作协议

本轮不要一步到位写完整：

- `TerminalActionMessage`
- `TerminalSnapshotMessage`
- `TerminalToastMessage`

如果为了类型落点需要先加壳类或最小接口，允许；但不要借机把第三阶段和第四阶段一起做掉。

### 3. 不删除旧 `ModularUI` 终端实现

本轮不要开始：

- 删除 `TerminalHomeGuiFactory`
- 删除旧 binder / sync state / page builder
- 从 `dependencies.gradle` 中清理终端对 `ModularUI2` 的依赖

这些都属于更后面的阶段。

### 4. 不要求启动游戏做人工目检

这轮验收口径已经明确改成“静态验证优先”。

因此本轮必须完成：

- 编译验证
- 最小相关测试或静态结构验证

但不要求：

- 启动 client
- 启动 dedicated server
- 进入游戏人工点击

不要把本轮做成运行态实装回归。

## 架构要求

### 1. 新打开链必须站在第一阶段 framework 上

phase 2 的 `TerminalHomeScreen` 必须基于第一阶段已落好的自有 framework 构建，而不是退回原生散装 `GuiScreen` 手搓。

### 2. screen model 与业务服务层分开

本轮就开始区分：

- client-side screen / snapshot model
- server-side terminal authority / summary provider

不要把服务端业务对象直接塞进 client screen。

### 3. 终端入口语义保持不变

用户视角下：

- 终端仍然由快捷键和背包按钮打开

本轮变化的是内部打开机制，不是用户入口语义。

### 4. 继续保持 package 分层

至少保持下面分层：

- `com.jsirgalaxybase.client.gui.framework`
- `com.jsirgalaxybase.client.gui.theme`
- `com.jsirgalaxybase.terminal.client.screen`
- `com.jsirgalaxybase.terminal.network`

不要把 phase 2 的新协议和新 screen 全塞回旧 `terminal.ui`。

## 推荐实施顺序

建议按下面顺序推进：

1. 新增最小 terminal open request / approved 消息
2. 抽出最小 `TerminalHomeScreenModel` 或等价 DTO
3. 新增 `TerminalHomeScreen` 占位根屏
4. 改客户端收到授权消息后打开新 screen
5. 收口 F8 调试入口，避免继续污染正式链路
6. 补编译验证、最小测试与 WORKLOG

## 验收标准

只有同时满足下面条件，这轮才算合格：

1. 终端打开链已经存在“客户端请求、服务端授权、客户端开新 screen”的最小闭环
2. 新打开链挂到真实 `TerminalHomeScreen`，而不是继续依赖 F8 调试屏
3. 旧 terminal 业务页没有被半迁移到新框架上
4. 旧 `ModularUI` 终端实现仍保留为过渡，不被提前删除
5. 编译验证通过
6. 如有新增测试，最小相关测试通过
7. `docs/WORKLOG.md` 已补记录

## 建议修改范围

本轮原则上允许修改：

- `src/main/java/com/jsirgalaxybase/terminal/network/**`
- `src/main/java/com/jsirgalaxybase/terminal/**`
- `src/main/java/com/jsirgalaxybase/terminal/client/**`
- 与 phase 2 直接相关的最小测试文件
- `docs/README.md`
- `docs/WORKLOG.md`

## 建议的静态验证

至少完成下面这些验证：

1. `compileJava`
2. `compileTestJava`
3. 若新增测试，则跑对应最小定向测试
4. 确认旧 `TerminalService` / `TerminalModule` 没有被误删到无法继续过渡
5. 确认 `TerminalFrameworkTestScreen` 不再是正式主入口

## 完成后请按这个格式回报

1. 本轮如何改造了终端打开链
2. `OpenTerminalRequestMessage / OpenTerminalApprovedMessage` 或等价消息里包含了哪些最小字段
3. 新 `TerminalHomeScreen` 当前承担了哪些职责
4. 哪些旧 terminal 实现被刻意保留没有迁
5. 做了哪些静态验证
6. 下一阶段最自然的进入点是什么

## 给实现者的最后提醒

第二阶段的关键词不是“页面更丰富”，而是“终端终于脱离服务端直接开 `ModularUI` 的旧生命周期”。

本轮完成后，应该得到的是：

- 新 framework 已经真正接到终端打开主链
- 终端可以以 client-side screen 的方式存在
- 后续首页壳、通知层、银行页和市场页迁移终于有了正确宿主

不要把这轮重新做成新的占位快捷键；要把它做成 terminal 新主链的第一块真正骨架。