# 汇率市场正式规则层改代码 Prompt

你现在接手的不是旧 MARKET 单一路线补尾巴，也不是继续扩标准商品市场，更不是去做定制商品市场第一版。

当前三市场结构已经正式定稿：

1. 标准商品市场
2. 定制商品市场
3. 汇率市场

这一次只做一件事：把当前“任务书硬币兑换入口”从早期残片，收口成 `汇率市场正式规则层 v1`。

## 先承认当前正式结论

这些结论已经定稿，不允许再改回去：

1. `StandardizedSpotMarketService` 这一套只能算 `标准商品市场早期残片`
2. `TaskCoinExchangeService` 这一套只能算 `汇率市场早期入口`
3. 当前还没有真正属于 `定制商品市场` 的正式实现
4. 旧的 MARKET 单一路线 prompt 只保留为历史废案
5. 当前正式执行顺序已经固定为：
   - 先做 `汇率市场正式规则层`
   - 再做 `标准商品市场商品目录与准入边界`
   - 再做 `定制商品市场最小挂牌链`
   - 最后再拆 `MARKET` 总入口

因此你这轮不能做下面这些事：

1. 不能继续扩标准商品市场 GUI 或订单簿能力
2. 不能继续把任务书硬币兑换解释成“商品市场一期”
3. 不能顺手去做定制商品市场挂牌
4. 不能提前拆 MARKET 总入口
5. 不能把 `GregTechStandardizedMetalCatalog` 继续上升成市场正式边界

## 这轮的唯一目标

把当前任务书硬币兑换相关代码，从“能跑的固定规则兑换入口”，改造成“边界清楚、规则明确、审计字段稳定、还能兼容现有入口”的 `汇率市场正式规则层`。

注意：

- 这轮不是重做银行
- 这轮不是做新 GUI
- 这轮不是上完整交易所
- 这轮是把 `汇率市场` 先从代码语义上立起来

## 必须先读这些文件

1. `docs/market-three-part-architecture.md`
2. `../Docs/市场经济推进.md`
3. `../Docs/下次对话议程.md`
4. `docs/WORKLOG.md`
5. `docs/banking-system-requirements.md`
6. `docs/banking-schema-design.md`
7. `src/main/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangeService.java`
8. `src/main/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangePlanner.java`
9. `src/main/java/com/jsirgalaxybase/modules/core/market/domain/TaskCoinExchangeQuote.java`
10. `src/main/java/com/jsirgalaxybase/modules/core/market/domain/TaskCoinDescriptor.java`
11. `src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`
12. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`
13. `src/test/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangePlannerTest.java`

## 当前问题判断

当前代码最大的问题不是“不能兑换”，而是“语义还不够正式”。

主要问题至少包括：

1. 当前兑换入口仍挂在 `market` 语义下，容易继续把它误解成商品市场的一部分
2. 规则层、报价层、限额层、审计层还没有被明确抽成汇率市场自己的正式模型
3. 当前代码更像一条可运行的固定规则脚本，而不是一个清楚的汇率市场应用层
4. 后续如果不先把汇率市场规则层立起来，标准商品市场和终端入口又会继续混线

## 这轮必须完成的代码任务

### A. 把“任务书硬币兑换”明确归到汇率市场语义下

要求：

1. 在代码层建立明确的“汇率市场 / 兑换市场”语义对象，不再让核心规则只靠 `TaskCoinExchange*` 这几个历史命名撑着
2. 可以保留现有类作为兼容桥接，但新的主路径应围绕更正式的汇率市场规则对象组织
3. 不要求这轮强行大改包路径或一次性全量 rename，但至少要让新主入口语义清楚

可接受方向示例：

1. 新增明确的规则层对象，例如：
   - `ExchangeMarketPairDefinition`
   - `ExchangeMarketRuleVersion`
   - `ExchangeMarketLimitPolicy`
   - `ExchangeMarketQuoteResult`
   - `ExchangeMarketExecutionRequest`
   - `ExchangeMarketExecutionResult`
2. 现有 `TaskCoinExchangePlanner` 与 `TaskCoinExchangeService` 改为：
   - 继续作为兼容实现细节
   - 或下沉到汇率市场正式服务后面作为特定报价源 / 特定资产适配器

重点不是类名本身，而是：

1. 规则层要像汇率市场
2. 不要再像“商品市场附属功能”

### B. 明确汇率市场正式规则层的最小能力

这一轮至少要把下面能力落成代码里的正式结构，而不是散落在文案和临时判断里：

1. `兑换对`
   - 当前至少支持：任务书硬币 -> 星光币
2. `规则版本`
   - 当前报价和兑换结果必须能带出规则版本或规则标识
3. `报价结果`
   - 必须显式表达输入资产、输出资产、数量、汇率、折价、限额结论
4. `限额判断`
   - 哪些任务书硬币允许兑、哪些禁兑、哪些折价兑，必须是结构化规则结果，而不是只留在自然语言
5. `审计字段`
   - 至少能稳定带出 requestId、sourceServerId、playerRef、ruleVersion、reason 或 notes

注意：

1. 这轮不要求把所有未来货币都做完
2. 但当前任务书硬币这条线必须先能作为“正式汇率规则层”的第一个资产对实现

### C. 保留现有命令入口，但把它们降格为兼容桥接

当前命令：

1. `/jsirgalaxybase market quote hand`
2. `/jsirgalaxybase market exchange hand`

这轮要求：

1. 现有命令仍然可用，避免把联调入口直接打断
2. 但命令内部应转向新的汇率市场正式规则层
3. 输出文案要明确这是 `汇率市场 / 兑换入口`，不是商品市场报价
4. 如果需要，可以补一个更明确的新内部服务名或新命令处理函数名

这轮不要求：

1. 立刻改掉外部命令字符串
2. 立刻拆 MARKET 顶层命令树

因为这一步属于后续的 `MARKET 总入口拆分` 阶段。

### D. 把终端或提示文案里的语义也拉正

至少检查并修正下面这种错误语义：

1. 把任务书硬币兑换描述成“市场一期已接真实商品交易入口”
2. 把 quote / exchange 说成标准商品市场功能

当前允许的做法是：

1. 保留“仍通过 MARKET 旧入口可访问”的兼容描述
2. 但必须明确这条线归属于 `汇率市场早期入口`

### E. 补测试，覆盖规则层而不是只测旧文案

这轮至少补下面几类测试：

1. 汇率市场报价结果的结构化字段测试
2. 限额 / 禁兑 / 折价判断测试
3. 规则版本和审计字段透传测试
4. 兼容命令仍然能走通新规则层的测试

如果当前已有 `TaskCoinExchangePlannerTest`，应优先保留并扩展，而不是直接废掉。

## 推荐实施方式

为了避免过度设计，建议按下面方式做：

1. 先补一个明确的汇率市场规则层主服务
2. 再让当前 `TaskCoinExchangePlanner` 变成该服务可复用的一个固定规则实现
3. 再把命令入口和文案切到新服务
4. 最后补测试与文档

不要做成：

1. 先全仓库 rename 一遍
2. 再回头想规则对象是什么

那样风险太高，而且收益很低。

## 这轮明确不做什么

1. 不补标准商品市场商品目录
2. 不扩 `StandardizedSpotMarketService`
3. 不做定制商品市场挂牌链
4. 不拆 MARKET 终端首页为三入口
5. 不新增三套 GUI
6. 不把汇率市场写成一个完整订单簿交易所

当前只做：

- `汇率市场正式规则层`

## 文档要求

1. 更新 `docs/WORKLOG.md`
2. 如有必要，可补一份很短的汇率市场规则说明文档到 `docs/`
3. 如果改了终端或命令文案，必须与 `docs/market-three-part-architecture.md` 保持一致

## 验收标准

只有同时满足下面条件，这一轮才算完成：

1. 代码里已经能明确看出“这条线属于汇率市场”
2. 当前任务书硬币兑换不再只是散落的早期脚本式实现，而是挂到正式规则层上
3. 报价、限额、规则版本、审计字段至少有一版结构化结果
4. `/jsirgalaxybase market quote hand` 与 `/jsirgalaxybase market exchange hand` 仍然可用
5. 命令或终端文案不再把这条线误写成商品市场功能
6. 测试覆盖到规则层和兼容桥接路径
7. `docs/WORKLOG.md` 已补记录

## 推荐汇报格式

完成后请按下面格式回报：

1. 这轮新增了哪些汇率市场正式规则层对象
2. 当前 `TaskCoinExchange*` 残留分别被如何保留或下沉
3. 兼容命令入口如何转到了新规则层
4. 哪些文案被拉正为汇率市场语义
5. 新增了哪些测试

## 给实现者的最后提醒

这轮的关键词不是“继续做 MARKET”，而是“先把汇率市场从旧 MARKET 影子里独立出来”。

如果你做完之后，任务书硬币兑换仍然只能被解释成“商品市场附带小功能”，那这轮就是失败的。

如果你做完之后，别人能明确回答下面三个问题，这轮才算方向对：

1. 当前汇率市场的第一个正式资产对是什么？
2. 当前规则版本、限额和审计信息在哪里体现？
3. 为什么它不属于标准商品市场？