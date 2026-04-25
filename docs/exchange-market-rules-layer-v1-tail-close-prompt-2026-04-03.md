# 汇率市场规则层 v1 收口修补 Prompt

你现在不是继续扩功能，也不是进入标准商品市场下一阶段。

这次只做一件事：把已经落地的 `汇率市场正式规则层 v1` 做完最后一个收口修补，让兼容命令桥不再把 `禁兑报价` 提前抛成异常。

## 当前验收结论

本轮主体方向已经成立：

1. `ExchangeMarketService` 已经把任务书硬币兑换正式收口为汇率市场规则层 v1
2. 命令层与终端首页文案已经改成“汇率市场兼容入口”
3. 正式报价 / 执行对象、审计字段、规则版本都已落地
4. `IV` 被误判为禁用档位的问题已经修掉
5. 现有针对性测试已通过

但还剩一个真实收口缺口：

- `ExchangeMarketService.quoteTaskCoinToStarcoin(...)` 已经能返回结构化的 `DISALLOWED` 报价结果
- 可是兼容桥 `TaskCoinExchangeService.resolveHeldSelection(...)` 在看到 `!formalQuote.getLimitPolicy().isExecutable()` 时，直接抛 `MarketExchangeException`
- 结果导致 `/jsirgalaxybase market quote hand` 对“禁兑但可报价”的任务书硬币，仍然只会输出通用拒绝文本，而不会展示正式规则字段

这会让兼容命令桥和正式规则层出现语义断层。

## 这轮唯一目标

让 `/jsirgalaxybase market quote hand` 在遇到“不支持兑换、但属于汇率市场规则判断范围内”的任务书硬币时，能够输出正式的结构化报价结果，而不是提前抛错。

也就是说：

1. `quote hand` 要能展示 `DISALLOWED` 报价
2. `exchange hand` 仍然必须拒绝真正不可执行的兑换
3. 这轮只修兼容桥，不新增新资产对，不改执行顺序，不扩标准商品市场

## 必须先看这些文件

1. `src/main/java/com/jsirgalaxybase/modules/core/market/application/ExchangeMarketService.java`
2. `src/main/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangeService.java`
3. `src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`
4. `src/test/java/com/jsirgalaxybase/modules/core/market/application/ExchangeMarketServiceTest.java`
5. `src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`
6. `docs/WORKLOG.md`

## 必须完成的修补任务

### 1. 拆开“报价可返回”和“执行必须可成交”这两个判断

当前问题在于：

- 兼容桥把 `preview` 和 `execute` 都绑定到了同一条 `resolveHeldSelection(...)`
- 这条路径把 `DISALLOWED` 报价提前拦成异常

你需要改成：

1. `previewHeldCoinFormal(...)` 可以返回正式报价结果，即使它的 `limitStatus = DISALLOWED`
2. `exchangeHeldCoinFormal(...)` 仍然必须只允许 `isExecutable()` 的报价进入执行阶段
3. 不要让 `quote hand` 和 `exchange hand` 共用同一条“必须可执行”的前置判断

可接受方式：

1. 拆出 `resolveHeldQuote(...)` 与 `resolveExecutableHeldSelection(...)`
2. 或保留现有结构，但必须明确让 preview 走“可报价”路径、execute 走“可执行”路径

### 2. 兼容命令输出必须覆盖禁兑报价

`/jsirgalaxybase market quote hand` 在下列情况下必须仍然输出正式字段：

1. `pair`
2. `ruleVersion`
3. `limitStatus`
4. `reasonCode`
5. `notes`
6. 如有需要，可保留 `UNRESOLVED family/tier`，但必须把“这是规则层禁兑结论”显示出来

注意：

1. 对“根本不是任务书硬币”的物品，仍然可以继续报错
2. 但对“属于任务书硬币体系，只是当前规则禁兑”的物品，不应再退化成一条普通异常消息

### 3. `exchange hand` 仍然保持拒绝语义

这轮不要把执行入口也放宽。

要求：

1. `exchange hand` 对 `DISALLOWED` 报价仍然必须拒绝执行
2. 拒绝时不得吞物品
3. 拒绝时错误消息应来自正式规则层 note 或 reason，而不是新写一套散乱文案

### 4. 补测试，覆盖这条真正的收口缺口

至少新增或补强下面测试：

1. `TaskCoinExchangeService` 或命令层测试：`quote hand` 对高阶禁用档位能返回 `DISALLOWED` 正式字段
2. 测试：`exchange hand` 对同一类禁用档位仍然拒绝执行
3. 测试：遇到禁兑时，兼容桥不会把它误当成“非汇率市场资产”

优先级：

1. 先测兼容桥行为
2. 再测命令输出文本

不要只补 `ExchangeMarketServiceTest`，因为正式规则层本身现在已经能返回禁兑报价，缺口在桥接层。

## 这轮明确不做什么

1. 不新增新的兑换对
2. 不修改标准商品市场代码
3. 不扩终端 GUI
4. 不重做命令树
5. 不改 `market exchange hand` 的外部命令字符串
6. 不顺手再做 rate 字段重设计

## 文档要求

1. 更新 `docs/WORKLOG.md`
2. work log 中明确写出：本次修补的是“兼容 quote 桥对禁兑结果过早抛错”

## 验收标准

只有同时满足下面条件，这轮才算真正收口：

1. `quote hand` 对禁兑任务书硬币能输出正式规则字段
2. `exchange hand` 对同一输入仍然拒绝执行
3. 禁兑不会再被误报成“不是汇率市场支持资产”
4. 兼容桥测试已补上
5. `docs/WORKLOG.md` 已记录

## 推荐汇报格式

完成后请按下面格式回报：

1. 兼容桥如何拆开了 preview 与 execute 的判断
2. `quote hand` 现在如何展示 `DISALLOWED` 正式字段
3. `exchange hand` 如何继续保持拒绝执行且不吞物
4. 新增了哪些测试