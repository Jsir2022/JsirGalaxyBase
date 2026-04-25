# Terminal BetterQuesting 风格 UI 框架第三阶段收口 Prompt

日期：2026-04-15

下面这份 prompt 用于处理 `JsirGalaxyBase` 当前 terminal BetterQuesting 风格 GUI 第三阶段在严格验收后确认存在的唯一收口项。

这不是 phase 4 的执行 prompt，也不是继续推进 section 路由、动作协议或真实业务页迁移的 prompt。

你的任务是只修这次 phase 3 验收已经明确指出的语义缺口，把当前首页壳收口到“当前页语义单一真源、可安全进入下一阶段”的状态。

---

你正在 `JsirGalaxyBase` 仓库中工作。请直接修改当前 terminal client home shell 相关实现，修复这次 phase 3 验收确认的问题。

## 开始前必须先读

开始编码前，先完整阅读并遵守下面这些文档：

- `README.md`
- `docs/terminal-plan.md`
- `docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`
- `docs/terminal-betterquesting-ui-phase2-open-chain-prompt-2026-04-14.md`
- `docs/terminal-betterquesting-ui-phase3-home-shell-prompt-2026-04-15.md`
- `docs/WORKLOG.md`

同时必须阅读这些实际实现文件：

- `src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`
- `src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`
- `src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`
- `src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalOpenApproval.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`
- `src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModelTest.java`

## 本轮目标

本轮只处理一个问题：

1. 把 `selectedPageId` 与导航选中态收口成首页壳当前页语义的单一真源

注意：

- 本轮不是继续做 phase 4 section 宿主切换
- 本轮不是继续补 `TerminalActionMessage`
- 本轮不是继续补 `TerminalSnapshotMessage`
- 本轮不是开始迁银行页或市场页
- 本轮不是继续重构首页壳视觉

## 已确认的验收缺口

### 问题：当前页语义仍然分裂为两套来源

当前 phase 3 代码中：

- `TerminalHomeScreenModel` 已经持有 `selectedPageId`
- 服务端批准载荷和 `OpenTerminalApprovedMessage` 也会传这个字段

但首页壳实际判断“当前页”的逻辑，主要仍依赖：

- `NavItemModel.selected`

也就是说，当前页语义同时存在两套来源：

1. `selectedPageId`
2. `navItems[].selected`

这会导致一个明确风险：

- 如果后续服务端只切 `selectedPageId`
- 或 `selectedPageId` 与 `navItems[].selected` 不一致
- 顶部状态带、主体区、导航高亮就可能展示错页

当前 phase 3 因为服务端还固定发首页，所以这个问题还没在运行面暴露，但它会直接阻塞下一阶段 section 宿主切换和真实页面挂载。

## 本轮要求

### 1. 首页壳当前页必须只有一个真源

你必须把当前页语义收口成单一真源。可接受方案包括：

- 方案 A：`selectedPageId` 成为唯一真源，`navItems[].selected` 改为派生状态
- 方案 B：保留 `navItems[].selected`，但 `selectedPageId` 不再单独持有，统一由同一来源生成

推荐优先采用方案 A，因为：

- 网络批准载荷已经显式传 `selectedPageId`
- 后续 section 路由和动作协议也更容易围绕 page id 展开

无论选哪种方案，最终都必须满足：

- `TerminalHomeScreen`
- `TerminalShellPanels`
- `TerminalHomeScreenModel.getSelectedNavItem()`

看到的都是同一套当前页语义，而不是靠两个字段碰运气保持一致。

### 2. 不能靠“约定调用方总是传一致数据”解决

不接受的做法：

- 继续保留两套来源，只是默认认为服务端会一直传一致
- 继续让 `defaultNavItems()` 硬编码首页为 selected
- 继续把当前页判断隐含绑定在 nav item 的 selected 标记上

本轮必须从模型和装配层把语义真正收干净。

### 3. 保持 phase 3 范围，不提前做 phase 4

本轮只收口当前页真源，不要顺手开始：

- 首页 section 真切换逻辑
- 新动作消息协议
- 新快照消息协议
- 真实银行页或市场页挂载

### 4. 继续保持旧业务页过渡边界

本轮修复后仍必须保持：

- 旧 `TerminalHomeGuiFactory`
- 旧 `TerminalBankPageBuilder`
- 旧 `TerminalMarketPageBuilder`
- 旧 binder / sync state

继续保留为过渡实现。

不要为了修这个问题而去接旧 ModularUI 页面。

## 允许修改的范围

本轮原则上只应触达与当前页语义收口直接相关的文件，例如：

- `src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`
- `src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`
- `src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`
- `src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalOpenApproval.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`
- 必要时 `src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModelTest.java`
- 与本轮直接相关的最小测试文件
- `docs/README.md`
- `docs/WORKLOG.md`

不要碰：

- `terminal.ui` 旧业务页实现
- `PanelSyncManager` 旧同步链
- `modules.core.market`
- `modules.core.banking`
- `modules.servertools`

## 测试要求

本轮至少补或更新下面这些验证：

1. `TerminalHomeScreenModelTest` 必须覆盖：
   - 当 `selectedPageId` 指向非首页时，当前页解析与导航高亮仍然一致
   - 当 nav 列表没有显式 selected 项时，模型仍能根据单一真源得出当前页

2. 如果你保留了 `NavItemModel.selected` 字段：
   - 必须有测试证明它只是派生结果，不会与 `selectedPageId` 分裂

3. 至少运行与本轮直接相关的定向测试，并在最终说明里列出

## 实现原则

- 只修这次验收确认的当前页语义缺口
- 不扩范围
- 不提前进入 phase 4
- 不把这轮变成新一轮首页壳重构
- 保持最小必要改动

## 文档要求

本轮至少要做：

- 更新 `docs/WORKLOG.md`
- 更新 `docs/README.md`

文档要明确写清：

- 这轮是 phase 3 严格验收后的收口
- 只修首页壳当前页语义单一真源
- 不是开始 section 宿主切换或业务页迁移

## 最终输出要求

完成后，请明确汇报：

1. 最终把当前页语义收口到了哪一个真源
2. 哪些类跟着一起调整了
3. 是否保留了 `NavItemModel.selected`，如果保留，它现在是什么语义
4. 补了哪些测试
5. 实际运行了哪些定向测试
6. 哪些内容被明确留给 phase 4，而没有在本轮提前实现

## 明确禁止事项

- 不开始 phase 4 section 路由
- 不开始 `TerminalActionMessage`
- 不开始 `TerminalSnapshotMessage`
- 不开始迁银行页或市场页
- 不接回旧 ModularUI 页面
- 不扩大到视觉改版或首页再设计

这轮的验收标准很直接：

- 首页壳当前页语义只能有一个真源
- `selectedPageId` 与导航高亮不能再分裂
- 修复后可以安全进入下一阶段的宿主切换工作

如果这三点没有同时做到，就不算完成。