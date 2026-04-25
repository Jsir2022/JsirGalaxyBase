# Terminal Bank / Market 严格收口 Prompt

日期：2026-04-11

下面这份 prompt 用于处理 `JsirGalaxyBase` 当前 terminal bank / market 主链经过严格审阅后确认存在的收口问题。

这不是新功能扩展 prompt，也不是继续打磨 GUI 观感的 prompt。

你的任务是只修这次已经确认的主链阻塞项，把 terminal bank / market 收口到“不会因 sync 回归再次导致打开终端 fatal 或缺少关键自动保护”的状态。

---

你正在 `JsirGalaxyBase` 仓库中工作。请直接修改当前 terminal bank / market 实现，修复已经确认存在的 sync 回归与测试缺口。

## 开始前必须先读

开始编码前，先完整阅读并遵守下面这些文档：

- `README.md`
- `docs/terminal-gui-regression-chain-2026-04-05.md`
- `docs/terminal-gui-continue-current-implementation-prompt-2026-04-06.md`
- `docs/WORKLOG.md`

如需核对当前 terminal market 三分边界，也应再读：

- `docs/market-entry-overview.md`
- `docs/market-terminal-asset-first-refactor-evaluation-2026-04-05.md`

## 本轮目标

只处理下面四件事：

1. 去掉 terminal market 文本输入框的手工 sync 注册，只保留 `TextFieldWidget.value(...)` 的 auto sync
2. 去掉 terminal bank 转账输入框的手工 sync 注册，只保留 `TextFieldWidget.value(...)` 的 auto sync
3. 增加一条真正覆盖 terminal open 装配链的回归验证，至少保护“打开终端不会因 auto/manual sync 冲突而 fatal disconnect”
4. 更新 `docs/WORKLOG.md`，明确这次修的是 terminal bank / market 共用的 sync 回归，而不是业务层变更

注意：

- 本轮不是继续做 GUI 美化
- 本轮不是继续扩标准商品市场、定制商品市场或汇率市场能力
- 本轮也不是继续重构 terminal 总壳或 page builder 结构

## 已确认的阻塞问题

### 问题 1：market 文本输入框再次混用了 manual sync 和 auto sync

当前 `TerminalMarketSyncBinder` 里，限价买、限价卖、即时买、即时卖的文本输入字段仍被 binder 手工注册为 editable sync。

但同一批字段又被 `TerminalMarketPageBuilder` 中的 `TextFieldWidget.value(...)` 直接绑定。

这与之前已经确认过的 fatal disconnect 根因完全一致：

- binder 手工 `syncManager.syncValue(...)`
- `TextFieldWidget.value(...)` auto sync

两条路径混在同一字段上时，终端打开装配阶段可能再次触发：

- `Old and new sync handler must both be either not auto or auto!`

本轮要求：

- 只移除 market 中由 `TextFieldWidget.value(...)` 绑定的文本输入字段手工 sync 注册
- 保留文本框侧 auto sync 作为唯一入口
- 不要顺手误删仍需保留的非文本框字段 sync，例如待撤单 / 待 claim 这类不是 `TextFieldWidget` 直绑的字段

### 问题 2：bank 转账输入框同样混用了 manual sync 和 auto sync

当前 `TerminalBankSyncBinder` 中：

- `bankTransferTargetName`
- `bankTransferAmountText`
- `bankTransferComment`

仍同时走了：

- binder 手工 sync 注册
- `TerminalBankPageBuilder` 中 `TextFieldWidget.value(...)` auto sync

由于 bank 和 market 共处同一张终端主页装配链，打开 terminal 时会一起参与 sync handler 收集。

因此：

- 只修 market，不修 bank，不满足本轮严格收口

本轮要求：

- 去掉 bank 转账 3 个文本框字段的手工 sync 注册
- 保留 `TextFieldWidget.value(...)` auto sync 作为唯一入口
- 不要改动银行业务语义、按钮门禁和确认弹窗链路

### 问题 3：当前缺少真正覆盖 terminal open 装配链的回归测试

当前通过的测试大多是 controller 纯逻辑测试，例如：

- `TerminalMarketSessionControllerTest`
- `TerminalBankSessionControllerTest`
- `TerminalCustomMarketSessionControllerTest`
- `TerminalExchangeMarketSessionControllerTest`

这些测试可以覆盖本地 sanitize、门禁或 DTO/快照语义，但不能保护最关键的 terminal open 装配风险。

也就是说，如果有人再次把文本框字段改回 manual sync + auto sync 混用：

- 这些控制器测试仍然可能全部通过
- 但玩家按 `G` 打开终端时会再次 fatal disconnect

本轮要求：

- 至少新增一条真正覆盖 `TerminalHomeGuiFactory` 装配链的测试
- 测试目标不是银行/市场业务结果，而是 terminal open 阶段不会因 sync handler auto/manual 冲突而失败
- 允许为此提炼 package-private 的最小辅助方法或可测试装配入口
- 不允许为了写测试把整个 terminal 框架大改

### 问题 4：必须补 worklog

这个仓库已经把同类 terminal sync 事故沉淀在 `docs/WORKLOG.md` 和统一事故文档中。

本轮要求：

- 必须新增一条 worklog
- 必须明确写清这是 terminal bank / market 共用的 sync 回归收口
- 必须明确这不是市场业务规则或银行业务语义变更

## 允许修改的范围

本轮原则上只应触达与这次 sync 回归和测试保护直接相关的文件，例如：

- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`
- 必要时对应的 `src/test/java/com/jsirgalaxybase/terminal/ui/...`
- `docs/WORKLOG.md`

如需为了测试新增极薄的 terminal 装配辅助方法，也只能在上述 terminal UI 范围内最小落地。

## 本轮明确不做什么

下面这些是已识别但可延后的事项，本轮不要顺手扩大范围：

1. 不清理 `TerminalHomeGuiFactory` 中未接入当前主链的旧 market page container 残留代码
2. 不处理旧装配分支中 custom market panel 参数误传问题
3. 不继续补更大范围的 asset-first 页面装配测试
4. 不顺手统一整个 terminal market / bank 的 helper API 或重做 sync 抽象
5. 不继续改 terminal 视觉层、卡片层或布局层

如果你在实现过程中再次看到这些残留问题，可以在最终汇报里注明，但不要把它们混进本轮代码提交。

## 测试要求

本轮至少要补或更新下面这些验证：

1. 一条真正覆盖 terminal open 装配链的测试，证明 terminal 打开时不会因为 auto/manual sync handler 冲突而失败
2. 定向测试应继续保留现有 controller 纯逻辑测试作为补充，而不是用新测试替代旧测试
3. 至少运行与本轮直接相关的 terminal UI 定向测试，并在最终说明里列出

如果你必须为 terminal 装配链新增一个更薄的测试入口，可以做，但必须满足：

- 最小改动
- 不改变现有业务边界
- 不重新引入旧 market page container 主链

## 实现原则

- 只修根因，不做表面补丁
- 只收口 terminal bank / market 文本输入 sync 回归
- 不改变银行、标准商品市场、定制商品市场、汇率市场的业务语义
- 不顺手清理 unrelated UI 代码
- 保持当前单实现 terminal 主链，不回退到旧 GUI

## 文档要求

本轮至少要做：

- 更新 `docs/WORKLOG.md`

不要把本轮文档扩写成新的阶段设计稿，也不要再单独拆新的事故文档；这轮只需要把收口事实补回现有记录链。

## 最终输出要求

完成后，请明确汇报：

- 移除了哪些 market 文本框字段的手工 sync 注册
- 移除了哪些 bank 转账文本框字段的手工 sync 注册
- 为 terminal open 装配链新增了什么测试保护
- 实际运行了哪些定向测试
- 哪些问题被明确留到下一轮而未在本轮处理

## 明确禁止事项

- 不新增 fallback GUI 或 legacy GUI
- 不扩大到 terminal 美化或 page 重构
- 不修改 market / bank 业务规则
- 不顺手清理旧 market page container 残留
- 不把本轮重新做成“大规模 terminal 重构”

如果你发现某个点还可以继续优化，但不属于以上 4 个收口项，请不要扩范围；优先把这轮 terminal bank / market sync 回归收干净。