# Terminal BetterQuesting 风格 UI 框架第六阶段收口 Prompt

日期：2026-04-19

下面这份 prompt 用于处理 JsirGalaxyBase 当前 terminal BetterQuesting 风格 GUI 第六阶段在严格验收后确认存在的收口项。

这不是 phase 7 的执行 prompt，也不是继续推进定制商品市场、汇率市场、cutover 或删除旧 terminal ModularUI 实现的 prompt。

你的任务是只修这次 phase 6 验收已经明确指出的可用性与布局缺口，把当前 MARKET 总入口与标准商品市场收口到“可滚动、可完整浏览、可安全进入 phase 7”的状态。

---

你正在 JsirGalaxyBase 仓库中工作。请直接修改当前 terminal framework / client screen / market section 相关实现，修复这次 phase 6 验收确认的问题。

## 开始前必须先读

开始编码前，先完整阅读并遵守下面这些文档：

- README.md
- docs/terminal-plan.md
- docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md
- docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md
- docs/terminal-betterquesting-ui-phase6-market-overview-standardized-prompt-2026-04-19.md
- docs/market-three-part-architecture.md
- docs/market-entry-overview.md
- docs/WORKLOG.md

同时必须阅读这些实际实现文件：

- src/main/java/com/jsirgalaxybase/client/gui/framework/CanvasScreen.java
- src/main/java/com/jsirgalaxybase/client/gui/framework/GuiPanel.java
- src/main/java/com/jsirgalaxybase/client/gui/framework/PanelContainer.java
- src/main/java/com/jsirgalaxybase/client/gui/framework/LabelPanel.java
- src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionState.java
- src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java
- src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalMarketSectionModel.java
- src/main/java/com/jsirgalaxybase/terminal/TerminalService.java
- src/main/java/com/jsirgalaxybase/terminal/TerminalMarketActionMessageFactory.java
- src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java
- src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java
- src/test/java/com/jsirgalaxybase/terminal/TerminalMarketActionMessageFactoryTest.java

如需参考旧终端滚动事故与边界，也要阅读：

- docs/WORKLOG.md 中 2026-04-04 到 2026-04-05 的 terminal 滚动相关条目
- src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java

注意：读取旧 TerminalHomeGuiFactory 的目的只是吸取“哪些滚动装配曾出过问题”的教训，不是把旧 ModularUI 滚动容器重新接回新壳。

## 本轮目标

本轮只处理 phase 6 严格验收已经确认的三类问题：

1. 新 framework 与新 market section 当前没有真正的滚动能力
2. 新 terminal 壳和标准商品市场布局密度过大，即使全屏也难以完整显示有效信息
3. 标准商品市场 section 仍存在主动截断数据的问题，例如商品按钮、claim 按钮、规则条目和部分盘口/说明信息只显示固定上限

注意：

- 本轮不是 phase 7
- 本轮不是开始迁移 MARKET_CUSTOM 或 MARKET_EXCHANGE
- 本轮不是开始 cutover
- 本轮不是删除旧 terminal ModularUI
- 本轮不是重新设计市场主链或扩大动作协议范围

## 已确认的验收缺口

### 问题 1：新壳没有滚动输入通路，市场页当前不可滚动

当前 phase 6 代码中：

- CanvasScreen 只转发点击、抬起和键盘输入
- framework 没有明确的滚轮或局部滚动契约
- TerminalMarketSection 使用固定卡片堆叠布局，但正文内容超出后没有任何可滚动容器承接

这会直接导致：

- 玩家在 MARKET_STANDARDIZED 页面无法滚动查看更多商品、规则或 claim 内容
- 全屏也只能看到被截进固定高度区域的一部分内容

本轮必须把这个问题真正修掉，而不是靠减少文案或继续裁切内容规避。

### 问题 2：新壳尺寸上限和标准市场固定高度布局过于保守

当前 phase 6 代码中：

- TerminalHomeScreen 对终端主体设置了偏紧的固定最大尺寸
- TerminalMarketSection 对 metrics、buy、book、claim、rule 等卡片采用固定高度布局
- 市场正文区在高分辨率或全屏下没有充分吃满可用空间

这会直接导致：

- 页面看起来“缩放偏大、信息偏挤”
- 即使游戏已全屏，正文区也仍然装不下核心信息

本轮必须把布局调整到“屏幕越大，正文区越充分；屏幕较小时仍然能靠局部滚动完整浏览”。

### 问题 3：标准市场 section 仍主动裁掉真实数据

当前 phase 6 代码里，标准市场 section 仍存在一些固定上限，例如：

- 商品列表只显示固定数量按钮
- claim 只显示固定数量入口
- ruleLines 只显示固定数量
- 部分订单簿与说明区仍按固定行数或固定可见值布局

这类做法在 phase 6 初次迁移时可以理解，但在本次严格验收后必须收掉。否则即便补了滚动，仍会因为 section 主动裁切而看不到完整数据。

## 本轮要求

### 1. 为新 framework 补上最小可用的局部滚动能力

你必须在当前 BetterQuesting 风格 framework 内，补出一套最小但真实可用的滚动能力，至少满足：

- CanvasScreen 能接收并转发滚轮输入
- GuiPanel / PanelContainer 或等价层有明确的滚动事件入口
- market section 内至少一个或多个正文区域可局部滚动，而不是整屏硬滚
- popup 生命周期与普通点击输入不被新滚动处理破坏

可接受实现包括：

- 新增专门的 scroll panel / viewport panel / clipped container
- 或在现有 panel/container 基础上补出最小滚动抽象

无论采用哪种方式，都必须满足：

- 不引回旧 ModularUI 的 ListWidget 或旧 terminal.ui 容器
- 不把滚动逻辑偷塞进 TerminalMarketSection 某个临时分支里
- 滚动能力应可复用于 phase 7 的 custom / exchange 页面，而不是只够这一个页面勉强跑

### 2. 调整 terminal 壳与标准市场 section 的空间策略

你必须让新 terminal 壳在大屏时使用更充足的可用区域，同时保持小屏不崩。

至少要做到：

- 放宽 TerminalHomeScreen 当前过于保守的 panel 尺寸上限
- 让 section body 在全屏下明显增大可视区域
- 不再把标准商品市场核心区完全依赖固定高度卡片堆叠来承载
- 小屏或紧凑布局下，内容应优先通过局部滚动解决，而不是继续缩死或截断

注意：

- 本轮目标是提高可读性与完整可浏览性，不是做一次视觉重设计
- 不要顺手把 phase 3 到 phase 6 的全部布局统一重写一遍
- 保持当前新壳风格，只做与可用性直接相关的空间策略调整

### 3. 消除标准市场 section 中的主动数据截断

你必须把当前标准市场 section 里“写死只显示前 N 条”的关键截断收掉。

至少要覆盖：

- 商品浏览入口
- claimable 资产入口
- 规则提示
- 盘口 / 个人订单 / 摘要中当前被固定行数压扁的内容

可接受方式包括：

- 滚动列表
- 动态生成更多 child panel
- 局部 viewport 内完整渲染所有条目

但不接受：

- 继续把上限从 3 改成 5、从 6 改成 8 这种换汤不换药的处理
- 继续只显示第一条个人订单或第一小段摘要，然后宣称已有滚动

### 4. 不改 phase 6 已成立的主链边界

本轮必须保持 phase 6 已成立的这些事实不变：

- MARKET 根页继续只做总入口与共享摘要
- MARKET_STANDARDIZED 继续承接真实标准商品市场动作
- custom market 与 exchange market 继续留给 phase 7
- 现有 TerminalActionMessage -> TerminalSnapshotMessage 主链继续成立

本轮不要求你重新设计动作类型；如果为配合滚动或布局需要补最小改动，可以做，但不要把任务扩成新一轮市场协议改造。

### 5. 保持 popup 与输入焦点行为稳定

新增滚动或 viewport 之后，必须确保下面这些已有行为不被破坏：

- 银行页 popup 仍可正常点击确认 / 取消
- 标准商品市场买单确认 popup 仍走新 popup 生命周期
- claim 确认 popup 仍走新 popup 生命周期
- 文本框焦点、输入和按钮点击不被滚轮事件吞掉或打乱

### 6. 补与本轮直接相关的测试

本轮至少补或更新下面这些验证：

1. framework 级最小测试
   - 如果你引入了新的 scroll panel / viewport / offset 逻辑，必须有定向测试覆盖：
   - 滚动偏移不会越界
   - 可视区域切换不会导致空白或负偏移
   - 滚动输入不会破坏已有普通点击分发语义

2. market section 级最小测试
   - 至少有定向测试证明标准商品市场不会再把关键数据集固定裁成前 N 条
   - 如果很难直接做 GUI-level 测试，可在 helper、layout model 或 child 生成层建立可测试 seam

3. 回归测试
   - 继续运行并保持通过：
   - src/test/java/com/jsirgalaxybase/terminal/TerminalMarketActionMessageFactoryTest.java
   - src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java

如遇 LWJGL 或 GuiScreen 级测试环境限制，不要硬造脆弱测试。优先把滚动状态、child 生成和布局边界下沉到可单测的 helper / state / model 层。

### 7. 做本地编译与 client/server 联调验证

本轮完成后，不只要跑测试，还必须按当前仓库已验证流程完成一次本地联调：

1. 在 JsirGalaxyBase 下执行构建或测试，确认产物最新
2. 用 runServer 启动本地 dedicated server，不手工复制 dev jar 到 run/server/mods
3. 用 runClient 启动本地 client，不手工复制 dev jar 到 run/client/mods
4. 明确验证：
   - 新 terminal 可正常打开
   - MARKET_STANDARDIZED 页面可以滚动
   - 全屏时可见区域明显改善
   - 商品列表、claim、规则等不再被固定上限截断

如果运行态还有非阻塞噪声，要明确区分“功能阻塞”还是“已启动但有噪声日志”。

### 8. 补文档与 WORKLOG

本轮实际代码变更后，必须同步更新：

- docs/README.md
- docs/WORKLOG.md

文档要明确写清：

- 这是 phase 6 严格验收后的收口 prompt
- 只修滚动、布局和数据截断问题
- 不是开始 phase 7 的 custom / exchange 迁移

## 允许修改的范围

本轮原则上只应触达与滚动、布局和数据完整显示直接相关的文件，例如：

- src/main/java/com/jsirgalaxybase/client/gui/framework/**
- src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java
- src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java
- 必要时与之直接相关的 helper / state / test 文件
- docs/README.md
- docs/WORKLOG.md

不要碰：

- src/main/java/com/jsirgalaxybase/terminal/ui/** 里的旧 ModularUI 市场/银行实现，除非只是阅读参考
- modules.core.market 的业务规则边界
- MARKET_CUSTOM / MARKET_EXCHANGE 的真实业务页迁移
- cutover 与旧 terminal 删除逻辑

## 明确禁止事项

- 不开始 phase 7
- 不迁移 MARKET_CUSTOM
- 不迁移 MARKET_EXCHANGE
- 不做 terminal 正式 cutover
- 不删除旧 terminal ModularUI 实现
- 不把任务扩大成新的市场动作协议重构
- 不用“继续加大固定条目上限”来伪装修复滚动与截断问题

## 最终输出要求

完成后，请明确汇报：

1. 新 framework 的滚动能力最终落在哪些类上
2. 标准商品市场哪些区域现在支持局部滚动
3. TerminalHomeScreen 与 TerminalMarketSection 的布局策略具体改了什么
4. 哪些固定数据截断被移除了
5. 补了哪些测试，分别验证什么
6. 实际运行了哪些定向测试与本地 client/server 联调命令
7. 运行态最终结论里，哪些问题已完成，哪些若仍存在只是非阻塞噪声
8. 哪些内容仍明确留给 phase 7，没有在本轮提前实现

这轮的验收标准很直接：

- MARKET_STANDARDIZED 页面必须可滚动
- 全屏下信息可见面积必须明显改善
- 商品、claim、规则等关键数据不能再被固定上限主动裁掉
- phase 6 既有 action / snapshot / popup 主链不能被修坏

如果这四点没有同时做到，就不算完成。