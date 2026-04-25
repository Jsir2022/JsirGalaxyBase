# Terminal Bank / Market 审查后续修复 Prompt

日期：2026-04-12

下面这份 prompt 用于处理 `JsirGalaxyBase` 当前 terminal bank / market 代码在严格审查后确认存在的两个后续风险。

这不是新功能扩展 prompt，也不是继续打磨 terminal 视觉层的 prompt。

你的任务是只修这次审查已经明确指出的风险，把 terminal bank / market 主链收口到“跨页状态一致、旧残留分支不会埋错线”的状态。

---

你正在 `JsirGalaxyBase` 仓库中工作。请直接修改当前 terminal bank / market 实现，修复已经确认存在的装配与状态刷新风险。

## 开始前必须先读

开始编码前，先完整阅读并遵守下面这些文档：

- `README.md`
- `docs/terminal-gui-continue-current-implementation-prompt-2026-04-06.md`
- `docs/terminal-bank-market-strict-close-prompt-2026-04-11.md`
- `docs/WORKLOG.md`

同时必须阅读这几个实际实现文件：

- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSessionController.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSessionController.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalCustomMarketSessionController.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeMarketSessionController.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`
- `src/test/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactoryTest.java`

## 本轮目标

本轮只处理下面两件事：

1. 补齐 market / custom / exchange 动作与银行页之间的快照失效链，避免玩家完成市场相关操作后切到银行页时看到过期余额或状态
2. 最小清理 `TerminalHomeGuiFactory` 中旧 market 挂载残留分支的 custom panel 错接线风险，避免后续误复用该分支时把 custom buy / claim 弹窗接错

注意：

- 本轮不是继续扩 market / bank 业务语义
- 本轮不是继续改 sync 机制
- 本轮不是继续做 terminal 美化
- 本轮不是继续做更大范围 page builder 重构

## 已确认的审查问题

### 问题 1：market/custom 动作后，银行页快照不会同步失效

当前 `TerminalHomeGuiFactory` 里，只有汇率兑换确认动作在执行后显式调用了：

- `bankSessionController.markSnapshotDirty()`

但标准商品市场与定制商品市场的确认动作并没有做这件事。

这会导致：

- 玩家在 market/custom 页面完成买入、卖出、下架、提取、成交等动作后
- 相关银行余额、冻结资金、公开储备或账户状态其实已经变化
- 但切回银行页时，`TerminalBankSessionController` 仍可能沿用旧 snapshot

这不是单纯 UI 延迟问题，因为这些市场动作会真实触发资金冻结、释放、结算或兑换。

本轮要求：

- 只要某个 terminal market/custom/exchange 动作会影响银行可见状态，就必须在终端装配链上补齐银行快照失效
- 优先在 `TerminalHomeGuiFactory` 当前的 synced action 注册点收口，不要顺手改成另一套架构
- 修完后，银行页至少要在用户完成这些动作并切页后看到最新快照，而不是旧余额

最低应覆盖的动作包括：

- `marketLimitBuyConfirmed`
- `marketLimitSellConfirmed`
- `marketInstantBuyConfirmed`
- `marketInstantSellConfirmed`
- `marketCancelOrderConfirmed`
- `marketClaimConfirmed`
- `customMarketBuyConfirmed`
- `customMarketCancelConfirmed`
- `customMarketClaimConfirmed`
- `marketExchangeConfirmed`

你可以在实现时进一步判断 `marketDepositConfirmed` 是否需要一起刷新银行快照；若该动作只影响仓储、不会改变银行页可见状态，可以在最终说明里明确写出你的判断。

### 问题 2：旧 market 挂载残留分支里 custom panel 仍然错接线

当前 terminal 主链已经通过 `createCurrentPageBody(...)` 直接进入：

- `createCustomMarketDashboard(selectedPageSync, customMarketBuyPanel, customMarketCancelPanel, customMarketClaimPanel)`

这条主链参数是对的。

但 `TerminalHomeGuiFactory` 里仍保留了一条旧的 market page container 残留：

- `createMarketPageContainers(...)`
- `createMountedMarketPageBody(...)`

在这条残留分支里，custom page 仍被错误接成类似：

- buy 位置接到了 market claim panel
- claim 位置也接到了 market claim panel

虽然当前看起来不是主链，但这是一个明确的维护风险：

- 后续如果有人恢复或复用这条挂载路径
- custom market 页面会立刻弹出错误确认面板

本轮要求：

- 不要求你重做整个旧挂载结构
- 但必须把这条残留里的 custom panel 接线至少修到与当前主链一致
- 或者如果你确认这条分支已彻底无用，也可以做最小删除

原则是二选一：

1. 最小修正残留分支参数，让它即使被误启用也不会错线
2. 最小删除已无主链价值的残留分支

不要把这件事扩大成 terminal 路由大重构。

## 允许修改的范围

本轮原则上只应修改与这两个问题直接相关的文件，例如：

- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`
- 必要时 `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSessionController.java`
- 必要时 terminal 相关测试文件
- `docs/WORKLOG.md`

如果你认为需要补 very thin 的测试辅助入口，可以做，但必须保持 package-private、最小、直接服务于这轮风险修复。

## 本轮明确不做什么

下面这些不要顺手扩大范围：

1. 不继续改 bank / market 业务规则
2. 不继续改 terminal sync 设计
3. 不继续做 HUD notification 或 dialog 体系重构
4. 不继续做 market asset-first 二次大改
5. 不继续把旧 market 挂载残留扩成完整新路由系统

## 测试要求

本轮至少补或更新下面这些验证：

1. 增加一条能证明“market/custom/exchange 相关动作之后，银行快照会被正确失效”的测试
2. 如果你保留旧 market page container 残留分支，至少补一条测试或断言，证明 custom panel 不再错接
3. 至少运行与本轮直接相关的 terminal UI 定向测试，并在最终说明里列出

如果受限于当前 plain JUnit / ModularUI 上下文，无法直接做完整运行态 GUI 测试，可以提炼最小可测装配入口，但必须明确说明测试覆盖边界。

## 实现原则

- 只修根因，不做表面补丁
- 保持当前单实现 terminal 主链
- 优先在现有 synced action 装配层收口跨页银行快照失效问题
- 对旧残留分支只做最小修正或最小删除
- 不扩大到无关 terminal 范围

## 文档要求

本轮至少要做：

- 更新 `docs/WORKLOG.md`

必须明确写清：

- 这是 terminal bank / market 审查后续风险修复
- 一条是银行快照失效链补齐
- 一条是旧 custom panel 错接线残留处理

## 最终输出要求

完成后，请明确汇报：

1. 哪些 market/custom/exchange 动作现在会联动失效银行快照
2. 你对 `marketDepositConfirmed` 是否需要刷新银行快照的判断是什么
3. 旧 market 挂载残留分支最终是被最小修正还是最小删除
4. 改了哪些文件
5. 补了哪些测试
6. 实际运行了哪些定向测试
7. 是否还有明确留给下一轮的问题

## 明确禁止事项

- 不新增 fallback GUI
- 不重写 terminal 路由架构
- 不继续扩大到 market / bank 业务功能开发
- 不把这轮变成 terminal 大重构

这轮的验收标准很直接：

- 市场相关资金动作之后，银行页不能继续展示旧快照
- 旧 custom market 挂载残留不能继续保留明显错接线风险

如果这两点没有同时做到，就不算完成。