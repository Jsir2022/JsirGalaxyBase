# 定制商品市场最小挂牌链阶段收口 Prompt

你现在不是继续扩定制商品市场功能，更不是开始 MARKET 总入口拆分或完整 GUI。

这次只做一件事：

把已经落地的 `定制商品市场最小挂牌链 v1` 做完最后一轮收口，补齐当前验收里仍然真实存在的闭环缺口。

## 当前验收结论

这一阶段主体已经成立：

1. `CustomMarketListing`
2. `CustomMarketItemSnapshot`
3. `CustomMarketTradeRecord`
4. `CustomMarketAuditLog`
5. `CustomMarketService`
6. 独立 JDBC 仓储与 PostgreSQL migration
7. `market custom list hand / browse / inspect / buy / cancel / pending` 最小兼容入口

当前也已经确认：

1. 定制商品市场没有继续复用标准商品订单簿作为主交易模型
2. 发布、浏览、查看详情、购买、下架、pending 查询主链都已经存在
3. 应用层、命令层、PostgreSQL 集成测试都已跑通

但还剩 3 个真实收口缺口：

### 缺口 1：交付状态机没有闭环到 `COMPLETED`

当前代码里：

1. 发布时进入 `ESCROW_HELD`
2. 购买后进入 `BUYER_PENDING_CLAIM`
3. 下架进入 `CANCELLED`

但没有任何正式应用服务或命令入口能把记录推进到：

1. 买家已领取
2. 卖家待交付已完结
3. `deliveryStatus=COMPLETED`

结果是：

1. `CustomMarketDeliveryStatus.COMPLETED` 目前不可达
2. `pending` 只能累积，不能真正完结
3. 这条链还不能叫“收口”

### 缺口 2：`单件商品` 边界还没有真正落到实现

当前定制商品市场 prompt 和模型文档都把 v1 定义成：

1. 单件商品
2. 带 NBT 商品
3. 二手 / 非标商品

但当前实现仍接受 `stack_size > 1` 的挂牌，并且测试已经把多件堆叠当成合法输入。

这意味着当前运行时边界其实更接近：

- `非标商品快照挂牌`

而不是：

- `单件商品挂牌`

必须明确二选一，不允许继续文档和实现各说各话：

1. 如果 v1 目标仍然是 `单件商品挂牌链`，那就把命令、服务、DDL、测试一起收紧到 `stackSize == 1`
2. 如果你决定 v1 允许整组快照挂牌，那就必须同步改 prompt、模型文档、命令帮助文案和验收标准，不再继续声称这是“单件商品链”

默认推荐：

- 按原方案收紧为 `单件商品挂牌链`

### 缺口 3：市场 JDBC 仍只有“表存在”校验，没有 fail-fast 列级校验

当前仓库已经为数据库迁移补了 `scripts/db-migrate.sh` 和版本化 migration。

但市场 JDBC 初始化仍然只校验：

1. 连接可用
2. 表存在

没有像银行那样继续校验：

1. 必需列是否齐全
2. schema 漂移时是否直接 fail-fast
3. 出错时是否明确提示运维运行 migration 脚本

这会导致：

1. 表被手工建了一半也能通过启动
2. 问题延后到第一次真正写入自定义市场表时才爆
3. migration 制度已经存在，但运行时没有真正把它作为唯一升级入口

## 这轮唯一目标

只做上面 3 个缺口的收口，不继续扩新功能。

也就是说：

1. 让定制商品交付状态机真正闭环
2. 让 `单件商品` 边界和代码实现完全一致
3. 让市场 JDBC 也具备和银行一致的 fail-fast schema 校验能力

## 必须先看这些文件

1. `src/main/java/com/jsirgalaxybase/modules/core/market/application/CustomMarketService.java`
2. `src/main/java/com/jsirgalaxybase/modules/core/market/domain/CustomMarketDeliveryStatus.java`
3. `src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`
4. `src/main/java/com/jsirgalaxybase/modules/core/market/infrastructure/jdbc/JdbcMarketInfrastructureFactory.java`
5. `src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankingInfrastructureFactory.java`
6. `src/test/java/com/jsirgalaxybase/modules/core/market/application/CustomMarketServiceTest.java`
7. `src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`
8. `src/test/java/com/jsirgalaxybase/modules/core/market/infrastructure/jdbc/MarketPostgresIntegrationTest.java`
9. `docs/custom-market-minimal-model.md`
10. `docs/custom-market-minimal-listing-chain-prompt-2026-04-03.md`
11. `docs/market-postgresql-ddl.sql`
12. `scripts/db-migrate.sh`

## 必须完成的修补任务

### 1. 补出最小完结动作，让 `COMPLETED` 可达

至少补下面两类能力中的一类，最好两类都补：

1. 买家领取定制商品
2. 卖家侧待交付记录完结

最小可接受做法：

1. 新增 `claimPurchasedListing` 或等价动作
2. 领取成功后把 `deliveryStatus` 推进到 `COMPLETED`
3. `pending` 视图不再永久保留已完结记录

要求：

1. 完结动作必须有正式应用服务入口
2. 最小兼容命令也要能联调，不接受只有内部 helper 没入口
3. 完结后 seller/buyer 两侧查询都能观察到状态变化

### 2. 收紧或重写 `单件商品` 边界，但必须统一

默认要求：

1. `market custom list hand` 只允许手持单件商品挂牌
2. `publishListing` 正式拒绝 `stackSize != 1`
3. DDL / 测试 / 文档与这个边界保持一致

如果你坚持允许堆叠挂牌，则必须同时完成：

1. 改 prompt
2. 改模型文档
3. 改命令帮助和验收文字
4. 补测试证明这是有意为之，不是遗漏

不接受：

1. 文档继续写“单件商品”，代码继续允许整组挂牌

### 3. 给市场 JDBC 补 fail-fast schema 校验

要求：

1. `JdbcMarketInfrastructureFactory` 不再只校验表存在
2. 至少校验标准市场与定制市场必需列是否齐全
3. 发现 schema 漂移时直接拒绝启动
4. 错误信息里明确提示运维执行 `scripts/db-migrate.sh`

重点：

1. 不要在应用启动时静默修表
2. 继续沿用显式 migration 作为唯一升级入口
3. 市场库的行为要和银行库一致，而不是退回“运行时碰到再说”

### 4. 补针对性测试，证明这轮真的收口

至少补下面测试中的大部分：

1. 买家领取后 `deliveryStatus` 进入 `COMPLETED`
2. `pending` 查询不会继续返回已完结记录
3. `publishListing` 拒绝非单件挂牌，或明确证明已改为允许整组挂牌
4. PostgreSQL 集成测试：删掉定制市场关键列后，市场 JDBC 初始化会 fail-fast
5. 命令层测试：新增的 claim/complete 入口能真实推进状态

## 这轮明确不做什么

1. 不扩新的定制商品筛选、搜索、排序能力
2. 不开始议价系统
3. 不开始纠纷系统完整版
4. 不做 MARKET 总入口拆分
5. 不做完整 GUI 页
6. 不重构标准商品市场或汇率市场

## 文档要求

1. 更新 `docs/WORKLOG.md`
2. 更新 `docs/custom-market-minimal-model.md`
3. 如补充收口说明，更新 `docs/README.md`
4. 若调整 `单件商品` 边界，必须同步更新原 prompt 或补一句显式说明

## 验收标准

只有同时满足下面条件，这轮才算真正收口：

1. `CustomMarketDeliveryStatus.COMPLETED` 已经通过正式业务动作可达
2. `pending` 不再是永久堆积视图，而是真正的待完结视图
3. `单件商品` 边界和代码实现已经一致
4. 市场 JDBC 已具备 fail-fast 列级 schema 校验
5. 相关测试已补齐并通过
6. `docs/WORKLOG.md` 已记录

## 推荐汇报格式

完成后请按下面格式回报：

1. 新增了哪个完结动作，如何把 `BUYER_PENDING_CLAIM` 推进到 `COMPLETED`
2. `单件商品` 边界最终是如何收口的
3. 市场 JDBC 如何做 fail-fast schema 校验
4. 新增了哪些针对性测试