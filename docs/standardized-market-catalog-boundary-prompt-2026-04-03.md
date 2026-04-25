# 标准商品市场商品目录与正式准入边界 Prompt

你现在接手的不是继续扩汇率市场，也不是提前做定制商品市场，更不是把 MARKET 终端再堆更多交互。

当前三市场正式执行顺序已经走到第二步：

1. 汇率市场正式规则层
2. 标准商品市场商品目录与正式准入边界
3. 定制商品市场最小挂牌链
4. MARKET 总入口拆分

汇率市场规则层 v1 已经完成收口。

这一次只做一件事：

把当前标准商品市场从“临时靠 GregTech 金属目录挡住入口”推进到“有正式商品目录和准入边界”的状态。

## 先承认当前事实

这些事实已经成立，不能回退：

1. `StandardizedSpotMarketService` 只属于 `标准商品市场早期残片`
2. 当前撮合、统一仓储、CLAIMABLE、恢复链都已经能跑，但“哪些商品允许进入”仍然主要被临时实现控制
3. `GregTechStandardizedMetalCatalog` 当前只能算 `临时适配目录 / 实验目录`
4. `GregTechStandardizedMetalCatalog != 标准商品市场正式制度边界`
5. 当前标准商品市场仍然不能被解释成“金属专场”

因此这轮不能做下面这些事：

1. 不能继续把 GregTech 前缀白名单直接当成最终制度定义
2. 不能继续扩订单簿、撮合、撤单、CLAIMABLE、恢复功能当作本轮主任务
3. 不能开始做定制商品市场挂牌
4. 不能顺手拆 MARKET 总入口
5. 不能把标准商品市场重新写回某类材料专场语境

## 这轮唯一目标

把“什么商品可以进入标准商品市场”这件事，从当前零散的临时判断，收口成一套明确的正式目录 / 准入边界。

重点不是多做交易功能，而是把边界先立起来。

完成后，代码里应该能清楚回答：

1. 什么是标准商品市场可交易商品
2. 为什么它可以进入标准商品市场
3. 当前这个判断是依据哪一版目录 / 准入规则
4. 为什么 `GregTechStandardizedMetalCatalog` 只是临时适配来源，而不是正式制度本体

## 必须先看这些文件

1. `docs/market-three-part-architecture.md`
2. `../Docs/市场经济推进.md`
3. `../Docs/下次对话议程.md`
4. `docs/WORKLOG.md`
5. `src/main/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketService.java`
6. `src/main/java/com/jsirgalaxybase/modules/core/market/application/StandardizedMarketProductCatalog.java`
7. `src/main/java/com/jsirgalaxybase/modules/core/market/application/GregTechStandardizedMetalCatalog.java`
8. `src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`
9. `src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`
10. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`
11. `src/test/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketServiceTest.java`
12. `src/test/java/com/jsirgalaxybase/modules/core/market/application/GregTechStandardizedMetalCatalogTest.java`

## 当前问题判断

当前标准商品市场的问题，不在于撮合链条不存在，而在于“制度边界还没独立出来”。

主要问题至少包括：

1. `StandardizedMarketProductCatalog` 现在还是一个很薄的接口，只能回答“这项商品临时能不能进”，还不能表达正式目录项和准入版本
2. `GregTechStandardizedMetalCatalog` 同时承担了：
   - 商品解析后的准入判断
   - 临时目录来源
   - 事实上的正式边界
3. `StandardizedSpotMarketService`、命令层、终端层都在直接依赖这个临时目录判断
4. 当前浏览商品、命令下单、仓储存入，都还没有统一暴露“这是按哪一版标准商品目录准入的”

如果这轮不先把目录和准入边界立起来，后面定制商品市场和 MARKET 入口继续推进时会再次混线。

## 这轮必须完成的代码任务

### A. 建立正式的标准商品目录 / 准入边界语义对象

至少要让代码层出现明确的正式对象，而不是继续只靠一个 `requireTradable...` 判断硬撑。

可接受方向示例：

1. `StandardizedMarketCatalogEntry`
2. `StandardizedMarketCatalogVersion`
3. `StandardizedMarketAdmissionDecision`
4. `StandardizedMarketAdmissionReason`
5. `StandardizedMarketCatalogService`
6. `StandardizedMarketCatalogSource`

重点不是类名，而是下面这些能力必须正式化：

1. 商品目录项
2. 目录版本
3. 准入决策
4. 拒绝原因
5. 临时适配来源与正式目录语义分离

### B. 把“正式目录边界”和“临时 GregTech 适配来源”分开

这轮必须明确：

1. 正式目录边界是标准商品市场自己的规则层
2. `GregTechStandardizedMetalCatalog` 只是一种当前可用的目录来源 / 适配来源

也就是说：

1. 可以继续复用它做首版目录来源
2. 但不能再让它既是“数据来源”又是“制度定义”

至少要做到：

1. 当前运行时能说明某商品是“被标准商品市场目录准入”
2. 同时还能说明当前目录来源暂时落在 GregTech 金属适配集合上
3. 后续替换目录来源时，不必重写全部命令、终端和撮合代码

### C. 让标准商品市场主路径统一走新的准入边界

本轮至少要统一下面几条路径：

1. `depositInventory`
2. `createSellOrder`
3. `createBuyOrder`
4. 命令层的商品参数校验
5. 终端市场页的商品展示与商品解析

要求：

1. 上述路径都应通过统一的正式目录 / 准入边界判断进入
2. 不要让命令层、终端层、服务层各自保留一套不同的“可交易商品”标准
3. 如果当前商品浏览仍基于活跃 productKey 聚合，也要通过统一准入判断过滤

### D. 补最小可见的目录版本 / 准入信息

这轮不要求做完整玩家可视化目录页，但至少要让系统内部能稳定带出：

1. 当前目录版本
2. 商品准入是否通过
3. 拒绝原因
4. 当前目录来源说明

可接受做法：

1. 服务返回结构化 decision
2. 命令输出中补一条简短目录版本 / 准入说明
3. 终端市场页商品摘要中补一条目录来源 / 目录版本说明

注意：

1. 不要求这轮就做完整 catalog browser
2. 但不能让目录版本只存在注释里

### E. 补测试，覆盖目录边界而不是只测旧金属实现

这轮至少补下面几类测试：

1. 正式目录准入对象的通过 / 拒绝测试
2. 同一商品在命令层、服务层、终端层使用同一准入结果的测试或等价覆盖
3. `GregTechStandardizedMetalCatalog` 作为临时目录来源时的桥接测试
4. 非准入商品被拒绝时，错误消息或拒绝 reason 稳定可断言
5. 如果补了目录版本字段，要有透传测试

重点：

1. 不要只停留在 `GregTechStandardizedMetalCatalogTest`
2. 要把“正式目录边界”本身测出来

## 推荐实施方式

建议按下面顺序做：

1. 先补正式目录 / 准入 decision 对象
2. 再把 `GregTechStandardizedMetalCatalog` 下沉成目录来源或临时适配器
3. 再把 `StandardizedSpotMarketService`、命令层、终端层统一切到新的准入主路径
4. 最后补测试和文档

不要做成：

1. 先全仓库 rename
2. 再到处 patch `requireTradableProduct`
3. 最后还是只有一个更长的临时目录类

那样不会真正建立制度边界。

## 这轮明确不做什么

1. 不改 maker/taker 费率
2. 不重做订单簿模型
3. 不扩 MARKET 页交易交互
4. 不做定制商品市场挂牌
5. 不拆 MARKET 总入口
6. 不把整个目录系统做成后台配置平台

当前只做：

- `标准商品市场商品目录与正式准入边界`

## 文档要求

1. 更新 `docs/WORKLOG.md`
2. 如有必要，可补一份很短的目录边界说明文档到 `docs/`
3. 若新增 prompt 文档或说明文档，建议同步 `docs/README.md`

## 验收标准

只有同时满足下面条件，这一轮才算完成：

1. 代码里已经能明确看出标准商品市场有自己的正式目录 / 准入边界
2. `GregTechStandardizedMetalCatalog` 不再承担“正式制度边界”这一层语义
3. 命令层、服务层、终端层已统一走同一套准入主路径
4. 目录版本或准入 reason 至少有一版结构化结果
5. 非准入商品被拒绝时，能说明是“目录边界拒绝”，而不是散乱异常
6. 相关测试已补齐
7. `docs/WORKLOG.md` 已补记录

## 推荐汇报格式

完成后请按下面格式回报：

1. 这轮新增了哪些标准商品目录 / 准入边界对象
2. `GregTechStandardizedMetalCatalog` 现在如何下沉为临时目录来源
3. 服务层、命令层、终端层如何统一切到新准入路径
4. 哪些目录版本 / 准入字段已经落地
5. 新增了哪些测试

## 给实现者的最后提醒

这轮的关键词不是“继续做金属市场”，而是“先把标准商品市场的正式入口边界立起来”。

如果你做完之后，别人仍然只能回答“因为它是 GregTech 金属所以能进市场”，那这轮就是失败的。

如果你做完之后，别人能明确回答下面三个问题，这轮才算方向对：

1. 当前标准商品市场的准入边界由什么正式对象表达？
2. 当前目录版本和拒绝原因在哪里体现？
3. 为什么 `GregTechStandardizedMetalCatalog` 不再等于制度边界本身？