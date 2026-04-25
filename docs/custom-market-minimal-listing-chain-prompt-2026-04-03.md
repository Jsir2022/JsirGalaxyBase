# 定制商品市场最小挂牌链分阶段实现 Prompt

你现在接手的不是继续扩标准商品市场，也不是回头补汇率市场，更不是提前拆 MARKET 总入口或重做终端 GUI。

当前三市场正式执行顺序已经进入第三步：

1. 汇率市场正式规则层
2. 标准商品市场商品目录与正式准入边界
3. 定制商品市场最小挂牌链
4. MARKET 总入口拆分

前两步已经完成收口。

这一次只做一件事：

把当前仍然不存在正式实现的 `定制商品市场`，先落成一条可运行、可审计、边界清楚的 `最小挂牌链 v1`。

补充收口约束：当前 v1 明确按“单件手持挂牌 -> 购买 -> 买家 claim 完结”落地，不把多件堆叠挂牌留作隐式兼容语义。

## 先承认当前事实

这些结论已经成立，不能回退：

1. `StandardizedSpotMarketService` 只属于 `标准商品市场`
2. `TaskCoinExchangeService / ExchangeMarketService` 只属于 `汇率市场`
3. 当前仓库里还没有真正属于 `定制商品市场` 的正式实现
4. 定制商品市场处理的是 `单件商品 / 带 NBT 商品 / 二手商品 / 非标商品`
5. 定制商品市场不是标准商品订单簿的一个“特殊模式”

因此这轮不能做下面这些事：

1. 不能复用 `MarketOrder` 去硬承接单件挂牌
2. 不能复用 `MarketCustodyInventory` 去硬承接定制商品托管主链
3. 不能把非标商品先抹平再塞回标准商品市场
4. 不能提前拆 `MARKET` 总入口
5. 不能把这轮做成一个完整 GUI 大页工程

## 这轮唯一目标

先把定制商品市场的第一条正式业务链立起来：

- 卖家发布挂牌
- 买家浏览挂牌
- 查看挂牌详情
- 买家购买单件商品
- 买家领取已购单件商品并完结交付状态
- 卖家下架未成交挂牌
- 成交后形成待领取 / 待交付状态
- 全链路留痕与审计

重点不是把所有交互都做完，而是先把 `定制商品市场自己的模型和状态机` 立起来。

完成后，代码里应该能明确回答：

1. 什么叫一条定制商品挂牌
2. 挂牌中的商品快照如何保存
3. 买卖双方状态如何流转
4. 为什么它不属于标准商品订单簿
5. 成交后为什么是“待领取 / 待交付”，而不是 CLAIMABLE 标准资产

## 必须先看这些文件

1. `docs/market-three-part-architecture.md`
2. `../../Docs/市场经济推进.md`
3. `../../Docs/下次对话议程.md`
4. `docs/WORKLOG.md`
5. `docs/standardized-market-catalog-boundary.md`
6. `src/main/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketService.java`
7. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`
8. `src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`
9. `docs/market-postgresql-ddl.sql`
10. 与银行结算相关的现有应用服务和仓储接口

目的不是复用标准商品模型，而是看清楚哪些底座能共享，哪些交易模型不能共享。

## 当前问题判断

当前缺的不是“市场页按钮”，而是 `定制商品市场自己的最小交易模型`。

主要缺口至少包括：

1. 没有正式的挂牌对象
2. 没有商品快照对象来保存单件商品与 NBT 信息
3. 没有卖家 / 买家状态机
4. 没有“购买后待交付、待领取”的正式流转模型
5. 没有定制商品市场自己的审计与留痕主链

如果这轮不先把这些东西立起来，后面继续补终端或 MARKET 入口时，只会再次把标准商品市场和定制商品市场混成一团。

## 分阶段实现要求

这轮必须按阶段推进，不要一口气把 GUI、命令、DDL、服务、交付全堆在一个巨石提交里。

推荐按下面 4 个阶段实现。

### 阶段 1：先补正式模型与 DDL

先落最小模型，不先做界面。

至少要新增或明确下面这些对象中的大部分：

1. `CustomMarketListing`
2. `CustomMarketItemSnapshot`
3. `CustomMarketListingStatus`
4. `CustomMarketDeliveryStatus` 或等价对象
5. `CustomMarketTradeRecord` 或等价成交留痕对象
6. `CustomMarketAuditLog` 或复用统一审计模型的定制商品市场业务类型

这一阶段至少要明确的字段：

1. listingId
2. sellerPlayerRef
3. buyerPlayerRef
4. askingPrice
5. currencyCode
6. listingStatus
7. deliveryStatus
8. sourceServerId
9. requestId / operationId
10. 商品快照字段：itemId、meta、stackSize、displayName、nbtSnapshot 或等价结构

数据库层至少要补：

1. 挂牌主表
2. 商品快照表或可等价表达的快照字段
3. 成交 / 状态流转留痕表

要求：

1. 允许复用 PostgreSQL 事务底座
2. 不允许复用标准商品市场的订单簿表来硬凑
3. 不要求这阶段就接终端交互

### 阶段 2：补应用服务主链

这一阶段只做最小应用服务，不追求完整交互。

至少补下面能力：

1. `publishListing`
2. `browseListings`
3. `inspectListing`
4. `purchaseListing`
5. `cancelListing`
6. `listSellerPendingDeliveries`
7. `listBuyerPendingClaims`
8. `claimPurchasedListing`

这里要明确：

1. 发布挂牌时，保存的是 `商品快照`，且当前 v1 只允许 `stackSize == 1`
2. 购买挂牌时，冻结或结算的是货币，不是标准商品订单簿里的商品份额
3. 购买成功后，不应直接进入标准商品市场的 `CLAIMABLE` 语义
4. 应进入定制商品市场自己的 `待交付 / 待领取` 语义
5. 买家领取后，定制商品市场自己的交付状态必须能正式流转到 `COMPLETED`

如果你需要最小交付动作，第一版可以采用：

1. 卖家发布时先托管物品到定制商品市场专用持有态
2. 买家支付成功后，挂牌变为已售
3. 物品进入买家待领取
4. 卖家资金进入已结算或待结算状态
5. 买家通过正式 claim 动作领取后，这条记录从双方 pending 视图中移除

但必须保持这是一条 `定制商品市场自己的状态机`。

### 阶段 3：补兼容入口，但只做最小联调入口

这一阶段不要先做 MARKET 总入口拆分。

要求：

1. 可补一组兼容命令入口
2. 或在现有终端中补一块很窄的只读/最小交互入口
3. 入口只为联调，不代表最终用户信息架构

推荐最小入口示例：

1. `/jsirgalaxybase market custom list hand <price>`
2. `/jsirgalaxybase market custom browse`
3. `/jsirgalaxybase market custom inspect <listingId>`
4. `/jsirgalaxybase market custom buy <listingId>`
5. `/jsirgalaxybase market custom claim <listingId>`
6. `/jsirgalaxybase market custom cancel <listingId>`
7. `/jsirgalaxybase market custom pending`

要求：

1. 文案必须明确这是 `定制商品市场兼容入口`
2. 不要把它写成标准商品市场功能
3. 不要在这轮就重构整个 MARKET 命令树

### 阶段 4：补测试与文档收口

这阶段至少补下面几类测试：

1. 挂牌创建测试
2. 商品快照保存测试
3. 浏览 / 详情测试
4. 购买后状态流转测试
5. 领取后完结状态测试
6. 下架测试
7. 待领取 / 待交付状态测试
8. 幂等 requestId 或重复提交保护测试
9. 如接了 JDBC，补最小 Postgres 集成测试

文档至少要更新：

1. `docs/WORKLOG.md`
2. 如有必要，补一份很短的定制商品市场模型说明文档到 `docs/`
3. 若新增 prompt 或说明文档，建议同步 `docs/README.md`

## 推荐实施顺序

建议严格按下面顺序做：

1. 先立模型与状态机
2. 再补 DDL 和仓储
3. 再补应用服务
4. 再补最小兼容入口
5. 最后补测试和文档

不要做成：

1. 先在终端里画一个“二手市场页”
2. 再临时决定后台对象是什么
3. 最后发现又借用了标准商品订单簿

那样会继续跑偏。

## 这轮明确不做什么

1. 不做标准商品市场扩展
2. 不做汇率市场扩展
3. 不拆 MARKET 总入口
4. 不做完整 GUI 交易页
5. 不做议价系统
6. 不做纠纷仲裁系统完整版
7. 不做推荐算法、排序权重或搜索平台
8. 不把所有物流、邮寄、跨服交付一次性做完

当前只做：

- `定制商品市场最小挂牌链 v1`

## 验收标准

只有同时满足下面条件，这一轮才算完成：

1. 代码里已经能明确看出定制商品市场有自己的正式挂牌模型
2. 商品快照、挂牌状态、交付状态至少有一版正式对象
3. 购买后的状态流转已经和标准商品市场分开
4. 有最小可联调入口可验证挂牌、浏览、购买、下架、待领取链路
5. 相关测试已补齐
6. `docs/WORKLOG.md` 已补记录

## 推荐汇报格式

完成后请按下面格式回报：

1. 这轮新增了哪些定制商品市场正式对象
2. 商品快照如何保存
3. 发布、购买、下架、待领取如何流转
4. 最小兼容入口是如何接上的
5. 新增了哪些测试

## 给实现者的最后提醒

这轮的关键词不是“再做一个市场页”，而是“先把非标商品交易的正式模型立起来”。

如果你做完之后，别人仍然只能回答“这只是标准商品市场的特殊单件模式”，那这轮就是失败的。

如果你做完之后，别人能明确回答下面三个问题，这轮才算方向对：

1. 定制商品市场的挂牌对象是什么？
2. 商品快照和交付状态在哪里体现？
3. 为什么这条链不属于标准商品订单簿？