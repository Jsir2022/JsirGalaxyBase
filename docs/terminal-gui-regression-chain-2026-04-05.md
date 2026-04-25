# 2026-04-05 终端 GUI 回归链统一事故文档

## 文档目的

这份文档统一收口 2026-04-04 到 2026-04-05 之间与终端 GUI 相关的一整串回归事故，重点覆盖：

- 银行 GUI 打开与子页装配
- 玩家转账表单输入与服务端消费
- 开户按钮与转账确认按钮
- 市场页同模型字段的连带回归
- GUI 内 HUD 泡泡反馈

这不是单个 bug 的一次性复盘，而是一条连续回归链的统一事故记录。以后遇到“终端 GUI 能看不能提、能提不能执行、按钮消失、点一下直接崩、失败没有提示”这类问题，应先看这份文档。

## 已并入的旧文档

- `docs/terminal-g-key-fatal-sync-incident-2026-04-04.md`
- `docs/terminal-sync-form-regression-2026-04-05.md`

另外，`docs/WORKLOG.md` 中 2026-04-04 到 2026-04-05 与终端银行 / 市场表单、按钮门禁、失败反馈相关的条目，也都已经在这里按一条事故链重新整理。

## 事故链总览

### 阶段 1：G 键打开终端直接 fatal 断线

症状：

- 玩家连接 dedicated server 后按 `G` 打开终端，客户端直接 fatal 并断线。
- 典型异常：`Old and new sync handler must both be either not auto or auto!`

根因：

- 同一批文本输入字段同时走了两套 sync 注册：
  - binder 手工 `syncManager.syncValue(...)`
  - `TextFieldWidget.value(...)` 自带 auto sync
- ModularUI 不允许同一字段同时混用 manual 和 auto sync。

涉及字段：

- 银行转账：`bankTransferTargetName`、`bankTransferAmountText`、`bankTransferComment`
- 市场交易：限价买、限价卖、即时买、即时卖等数量/价格文本框

修复：

- 去掉文本框字段的重复手工 sync 注册。
- 保留 `TextFieldWidget.value(...)` 作为文本框字段唯一入口。

### 阶段 2：输入框不 fatal 了，但一打字直接崩

症状：

- 终端打开后，银行转账表单或市场交易表单一输入就崩。
- 典型异常：`Sync handler is not yet initialised!`

根因：

- 某些 `StringSyncValue` 被创建出来了，但并没有注册到 `PanelSyncManager`。
- 文本框 `onTextChanged` 触发时，控件试图写入一个还没挂到 panel sync manager 的 handler。

修复：

- 补回表单字段在 `PanelSyncManager` 上的真实注册。
- 这一轮只解决了“能输入”，还没有解决“服务端一定拿到最新值”。

### 阶段 3：GUI 看起来已提交，但服务端实际没有执行转账

症状：

- 银行终端确认弹窗能弹出。
- 点击确认后客户端出现“已提交”之类反馈。
- 但服务端并未真正执行转账，流水也不变化。

根因：

- 字段虽然已经有 sync 注册，但仍然只是本地 getter/setter 绑定。
- 对于“客户端先填值，服务端 synced action 再消费”的字段，这还不够。
- 服务端执行 `bankTransferConfirmed` 时，读到的仍可能是空值或旧值。

本质上，这类字段都需要双端同步模型：

- 客户端有输入缓存
- 服务端有真实会话值
- synced action 消费的是服务端值，而不是客户端临时 UI 对象里的值

修复：

- 银行转账字段改成“客户端缓存 + 服务端 getter/setter”的双端同步模式。
- 市场页同类高风险字段一起按相同模型收口，包括：
  - 商品选择
  - 限价/即时交易输入
  - 取消订单
  - 领取托管

### 阶段 4：双端同步补上后，按钮又像“消失”了一样

症状：

- 银行转账按钮长期灰掉。
- 市场确认、撤单、领取、存入、兑换等按钮长期灰掉。
- 用户体感是“按钮没了”或“按钮永远不可用”。

根因：

- 可编辑字段已经切到 sync 侧缓存。
- 但按钮 `enabled` 判定仍然读的是旧的本地 session controller 状态。
- 结果是 UI 表面有输入，按钮门禁却持续看到空值。

修复：

- 银行转账确认按钮改为基于当前 sync 值判定。
- 市场限价、即时、撤单、领取、存入、兑换确认等入口也统一改成基于当前 sync 值判定。
- 盘口价格预填时，直接回写 sync 文本框，而不是只改 controller 本地值。

### 阶段 5：DevB 点开户按钮直接把游戏点崩

症状：

- DevB 在终端里尝试开户时，client 直接崩溃。
- 典型异常：`Sync handler is not yet initialised!`
- 崩溃栈落在 `InteractionSyncHandler.onMouseTapped(...)`。

根因：

- 开户按钮直接依赖 `InteractionSyncHandler`。
- 在 ModularUI sync manager 还没完全初始化时，玩家抢先点击按钮，会触发未初始化 sync handler 的点击路径。

修复：

- 开户按钮改成普通本地按钮。
- 按钮点击后通过单独的 synced action 提交到服务端。
- 避开了会抢跑的 `InteractionSyncHandler` 点击链。

### 阶段 6：转账失败时没有泡泡提示

症状：

- 比如“收款账户不存在”这种失败，银行动作确实失败了。
- 但用户在终端 GUI 打开期间看不到 HUD 泡泡反馈。

根因：

- 终端通知本身已经进入队列。
- 但 HUD overlay 之前在任何 `currentScreen != null` 的情况下都直接停止渲染。
- 终端本身就是 GUI，因此银行失败反馈在终端打开期间被全局屏蔽。

额外缺口：

- 转账确认时客户端还会额外弹一个“请求已提交”的假提示，进一步掩盖真实服务端结果。

修复：

- HUD overlay 改成允许在终端所用的 ModularUI 屏幕上继续渲染。
- 银行转账改成只展示服务端真实反馈，不再额外塞一个“已提交”的假泡泡。

## 这套事故链的统一根因

表面上看，这些问题像是 6 个不同 bug：

- 打开 GUI fatal
- 文本输入崩溃
- 表单提交无效
- 按钮灰掉
- 开户点击崩溃
- 失败无泡泡

但本质上只有两类系统性缺口：

### 1. sync 语义没有统一

终端里至少存在 4 种不同字段模型：

- 只读展示字段
- 文本框输入字段
- 客户端选择后由服务端动作消费的字段
- 点击动作或确认弹窗触发字段

这几类字段不能混用同一套“看起来能跑”的写法。尤其是：

- 文本框字段不能同时手工 sync 和 auto sync
- 客户端改、服务端读的字段不能只做本地 getter/setter 绑定
- 按钮 enabled 判定必须读取当前 sync 值，而不是旧 controller 缓存

### 2. GUI 生命周期与反馈通路没有按真实交互闭环设计

表现为：

- 按钮点击早于 sync manager 初始化
- 终端内动作反馈进入队列后又被 GUI 渲染条件挡住
- 客户端假提示和服务端真实反馈语义混在一起

这说明终端不仅要保证“能提交动作”，还要保证：

- 初始化阶段不会因为早点击而崩
- 失败提示在 GUI 打开时仍然可见
- 用户看到的是服务端真实执行结果，而不是客户端假象

## 当前结论

到 2026-04-05 这一轮收口后，这套问题链已经完成以下修复：

- 终端不再因 manual/auto sync 混用而在打开时 fatal 断线
- 银行/市场表单输入不再因未注册 sync handler 而一打字就崩
- 银行转账表单可以真实送达服务端并生成流水
- 双端同步后的银行/市场动作按钮门禁已恢复
- DevB 开户按钮不会再因 `InteractionSyncHandler` 抢跑而崩 client
- 终端 GUI 打开期间，银行失败反馈泡泡可见
- 客户端不再用“已提交”假提示掩盖真实服务端结果

## 以后遇到同类问题时的排查顺序

1. 先区分是“打开 GUI 就炸”还是“打开后输入/点击才炸”。
2. 如果是打开就炸，先查是否混用了 manual sync 和 auto sync。
3. 如果是输入就炸，先查字段是否真的注册到了 `PanelSyncManager`。
4. 如果是不炸但服务端不执行，先查该字段是不是双端同步，而不是只做了本地 getter/setter。
5. 如果是按钮灰掉，先查按钮门禁是不是仍在读旧 controller 状态。
6. 如果是失败没提示，先查通知是否已进入队列，再查 HUD overlay 是否在当前 GUI 下被屏蔽。
7. 任何“客户端先改值、服务端后消费”的字段，都默认按高风险字段处理，不要只修当前报错项。

## 维护约束

以后新增终端银行/市场表单或按钮时，至少遵守下面几条：

1. 文本框字段只保留一套 sync 注册路径，不要再混用 binder 手工注册和 `TextFieldWidget.value(...)`。
2. 只要某个字段会被服务端 synced action 消费，就优先考虑双端同步，而不是本地 getter/setter 绑定。
3. 按钮 enabled 判定必须和当前 sync 值保持同源。
4. GUI 内需要可见的反馈，不要默认依赖“关掉 GUI 才能看到”的 HUD 路径。
5. 不要再给银行转账这类真实服务端动作额外塞客户端假成功提示。

## 相关代码入口

- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankPageBuilder.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSessionController.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalWidgetFactory.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalHudOverlayHandler.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalHudNotificationManager.java`

## 相关记录入口

- `docs/WORKLOG.md` 中 2026-04-04 到 2026-04-05 与终端 GUI、银行转账、按钮门禁、失败提示相关的条目
- 本文档作为这一串事故的唯一统一入口

## 本轮验收阻塞与修复执行 Prompt

下面这段不是事故复盘，而是给后续执行 AI 的明确收口要求。当前终端 / 市场大批实现虽然已经落地，但本轮还不能直接判定为完成，因为存在一条已经被定向测试打实的兼容回归。

### 当前不能验收通过的原因

当前定向验证结果不是“感觉上差不多”，而是已经有明确失败：

- Gradle 定向测试执行到市场终端与市场业务相关链路时，`TaskCoinExchangeServiceTest` 失败。
- 失败点是：`previewRegistryQuoteStillRejectsNonTaskCoinAsset`。
- 失败说明：旧的任务书兑换兼容入口，对“非任务书物品”的处理语义被改掉了。

当前测试期望是：

- 旧兼容入口 `TaskCoinExchangeService.previewRegistryQuote(...)`
- 当输入 `minecraft:stick` 这类非任务书物品时
- 必须继续直接抛出 `MarketExchangeException`
- 错误文案应保持为：`当前手持物品不属于汇率市场支持的任务书硬币资产对`

但当前实际实现已经变成：

- 先走 `ExchangeMarketService.quoteTaskCoinToStarcoin(...)`
- 把非任务书物品收敛成 formal disallowed quote
- 因为现在 `quoteTaskCoinToStarcoin(...)` 对 unsupported asset 返回的不是空，而是 `DISALLOWED`
- 所以 `previewRegistryQuote(...)` 不再抛异常，而是错误地把旧兼容入口也吞成 formal quote 结果

这会导致一个非常具体的回归：

- 新终端 formal quote 链是成立的
- 但旧 market quote / exchange hand 兼容入口的行为契约被 silently 改坏了

### 必须保持的边界

执行修复时，不要把问题理解成“把 formal disallowed quote 整体删掉”。正确边界是：

1. 新终端 / 新 formal quote 链继续保留：
  - `ExchangeMarketService.quoteTaskCoinToStarcoin(...)`
  - 可以对 unsupported asset 返回 `DISALLOWED`
  - 这样终端详情页仍可展示 `limitStatus / reasonCode / notes`

2. 旧兼容入口必须继续保留旧拒绝语义：
  - `TaskCoinExchangeService.previewRegistryQuote(...)`
  - 对“非任务书物品”仍应直接抛 `MarketExchangeException`
  - 不能把旧兼容入口强行改造成“总是返回 formal quote”

3. 不能为了让测试过掉，直接改测试去接受当前错误行为。

4. 不能把“非任务书物品”和“任务书硬币但 tier 不支持”混成同一类错误：
  - 非任务书物品：旧兼容入口直接拒绝
  - 任务书硬币但 tier 不支持：允许形成 formal disallowed quote，且 `requireExecutableRegistryQuote(...)` 再按 note 拒绝执行

### 推荐修复方式

优先按下面思路修：

1. 在 `TaskCoinExchangeService.previewRegistryQuote(...)` 中恢复旧兼容入口边界。
2. 调用 `ExchangeMarketService.quoteTaskCoinToStarcoin(...)` 后，不要只用 `Optional.orElseThrow(...)` 判空。
3. 需要额外识别：
  - 如果 formal quote 的 `reasonCode` 是 `TASK_COIN_ASSET_UNSUPPORTED`
  - 则在旧兼容入口这里重新抛出 `MarketExchangeException("当前手持物品不属于汇率市场支持的任务书硬币资产对")`
4. 对 `TASK_COIN_TIER_DISALLOWED` 则不要回退成旧异常，而是继续保留 formal disallowed quote。

这样可以同时满足：

- 旧兼容入口语义不回归
- 新 formal quote 终端链不被打掉
- unsupported asset 与 unsupported tier 两类输入重新分流

### 明确禁止的错误修法

不要这么修：

1. 直接把 `ExchangeMarketService.quoteTaskCoinToStarcoin(...)` 改回 unsupported asset 返回 `Optional.empty()`。
  - 这样会把新终端 formal quote 页的 reasonCode / notes 链一起打坏。

2. 直接修改 `TaskCoinExchangeServiceTest` 让它接受当前行为。
  - 这属于拿测试迎合回归，不是修兼容边界。

3. 为了“统一”而把所有 disallowed 输入都强制返回 formal quote，再让旧兼容入口完全失去旧异常语义。
  - 这会继续破坏旧命令 / 旧调用方契约。

4. 顺手扩大改动范围，把汇率市场整个 formal quote / execute 链重写。
  - 这次只需要修复兼容入口边界，不需要重做汇率市场规则层。

### 执行后必须验证的内容

修完后至少要重新跑下面这些定向验证：

1. `TaskCoinExchangeServiceTest`
  - 重点确认 `previewRegistryQuoteStillRejectsNonTaskCoinAsset` 恢复通过
  - 同时确认 `previewRegistryQuoteReturnsDisallowedFormalQuoteForUnsupportedTier` 仍然通过

2. `ExchangeMarketServiceTest`
  - 确认 formal disallowed quote 语义没有被兼容修法误伤

3. `TerminalExchangeQuoteViewTest`
  - 确认终端仍能展示 unsupported asset 的 formal rejection 字段

4. 如有条件，继续跑当前市场终端相关定向链：
  - `TerminalMarketSessionControllerTest`
  - `TerminalMarketServiceTest`
  - `TerminalExchangeMarketSessionControllerTest`
  - `TerminalCustomMarketSessionControllerTest`
  - `CustomMarketServiceTest`

### 另外一个必须补齐的收口项

当前这批大规模代码改动已经落地，但 `docs/WORKLOG.md` 还没有对应记录这次真正的终端 / 市场代码实装，只记录了 earlier 的文档评估与 prompt 产出。

因此执行 AI 在修复上述兼容回归后，还必须：

1. 补写本轮真实代码变更的 WORKLOG 条目
2. 写明影响范围
3. 写明这次兼容回归修复与终端 asset-first 实装的关系

### 给执行 AI 的一句话目标

不是简单“让测试绿”，而是：

- 保住新终端的 formal quote 展示链
- 恢复旧兼容入口对非任务书物品的直接拒绝语义
- 不把 unsupported asset 和 unsupported tier 混为一谈
- 最后补齐 WORKLOG 并用定向测试证明没有再修歪