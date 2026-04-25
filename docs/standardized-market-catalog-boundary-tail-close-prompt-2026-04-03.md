# 标准商品市场目录边界阶段收口 Prompt

你现在不是继续扩标准商品市场功能，也不是进入定制商品市场，更不是去重做 MARKET GUI。

这次只做一件事：

把已经落地的“标准商品市场目录与正式准入边界”做完最后一个收口，消除命令层、终端层、服务层仍各自持有默认 catalog 实例的分叉风险。

## 当前验收结论

这一阶段主体已经成立：

1. `StandardizedMarketCatalogVersion`
2. `StandardizedMarketCatalogEntry`
3. `StandardizedMarketAdmissionDecision`
4. `StandardizedMarketAdmissionReason`
5. `StandardizedMarketCatalogService`
6. `StandardizedMarketCatalogSource`

这些正式对象都已经落地。

当前也已经确认：

1. `GregTechStandardizedMetalCatalog` 已经下沉为目录来源适配器
2. `StandardizedSpotMarketService` 已经把 `depositInventory / createSellOrder / createBuyOrder` 接到了目录 decision
3. 命令层已能输出 `version / reason / source`
4. 终端市场页已能显示目录版本与来源说明
5. 针对性测试已经通过

但还剩一个真实收口缺口：

- `InstitutionCoreModule` 里运行时服务装配了一份 catalog
- `GalaxyBaseCommand` 又自己 `createDefaultCatalog(...)` 了一份
- `TerminalMarketService` 也自己 `createDefaultCatalog(...)` 了一份

这意味着当前虽然“接口名统一了”，但还没有真正收口成“同一个运行时目录边界”。

只要后续目录来源、目录版本或服务装配方式发生变化，命令层与终端层就可能和真实交易服务分叉。

这不符合本阶段原目标：

- 不要让命令层、终端层、服务层各保留一套自己的可交易商品标准。

## 这轮唯一目标

把标准商品市场目录边界真正收口成“单一运行时事实来源”。

也就是说：

1. 命令层不要再自己 new 默认 catalog 当正式判断源
2. 终端层不要再自己 new 默认 catalog 当正式判断源
3. 标准商品市场运行时的目录 decision 应从同一服务边界透出
4. 后续替换目录来源时，不应需要再同时改三处判断逻辑

## 必须先看这些文件

1. `src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`
2. `src/main/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketService.java`
3. `src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`
4. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`
5. `src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`
6. `src/test/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketServiceTest.java`
7. `docs/standardized-market-catalog-boundary.md`
8. `docs/WORKLOG.md`

## 必须完成的修补任务

### 1. 把目录边界提升成运行时共享依赖

可接受做法示例：

1. 由 `InstitutionCoreModule` 暴露标准商品市场运行时 catalog
2. 或由 `StandardizedSpotMarketService` 继续作为唯一目录 decision 入口，并让上层通过 `inspectCatalogProduct / inspectCatalogStack / getProductCatalog` 访问
3. 或新增一个很薄的 runtime-level catalog accessor，但必须保持单一事实来源

重点不是类名，而是：

1. MARKET 命令
2. MARKET 终端
3. 标准商品交易服务

最终都必须基于同一份运行时目录边界决策，而不是三份默认实例。

### 2. 命令层切到运行时目录判断

要求：

1. `GalaxyBaseCommand` 不再把本地 `createDefaultCatalog(...)` 作为正式主路径
2. 有 `InstitutionCoreModule` / `StandardizedSpotMarketService` 时，应优先从运行时目录判断拿 decision
3. 对 `sell create`、`buy create`、`book`、`sell deposit hand` 等命令，准入消息与真实服务边界保持一致

注意：

1. 如果服务运行时不可用，可以保留窄范围 fallback
2. 但 fallback 不能继续冒充正式主路径

### 3. 终端层切到运行时目录判断

要求：

1. `TerminalMarketService` 不再长期持有自己的默认 catalog 作为正式判断源
2. 商品浏览、手持识别、商品摘要、存入、挂单、即时交易前校验，都应优先走运行时目录边界
3. 终端显示的 `目录版本 / 来源 / reason` 必须与真实交易服务一致

注意：

1. 终端在运行时未就绪时可以保留只读或空状态提示
2. 但一旦运行时已就绪，不应再用私有默认 catalog 代替真实目录边界

### 4. 补测试，证明不是“三份同名实现碰巧一致”

至少补下面测试中的大部分：

1. 命令层测试：当服务注入的目录判断与默认 catalog 不同时，命令层仍跟随运行时服务结果
2. 终端层测试：当运行时目录判断拒绝某商品时，终端不会因为本地默认 catalog 而继续展示为可交易
3. 服务或模块测试：运行时 catalog accessor 返回的 decision 能被上层复用

重点：

1. 不能只证明默认 catalog 还能跑
2. 要证明“单一运行时事实来源”真的成立了

## 这轮明确不做什么

1. 不新增目录后台配置能力
2. 不扩新的商品来源
3. 不调整撮合、费率、CLAIMABLE、恢复机制
4. 不开始定制商品市场
5. 不拆 MARKET 总入口
6. 不重写终端页面结构

## 文档要求

1. 更新 `docs/WORKLOG.md`
2. 如有必要，补一句 `docs/standardized-market-catalog-boundary.md`，说明当前目录边界已经收口为运行时单一事实来源
3. 若新增收口 prompt，请同步 `docs/README.md`

## 验收标准

只有同时满足下面条件，这轮才算真正收口：

1. 命令层、终端层、服务层不再各自保留正式目录判断实例
2. 目录 decision 已真正收口为同一运行时边界
3. 命令输出与终端显示的 `version / reason / source` 和真实服务一致
4. 至少有测试证明“运行时目录变化时，上层不会继续跟本地默认 catalog 走”
5. `docs/WORKLOG.md` 已记录

## 推荐汇报格式

完成后请按下面格式回报：

1. 目录边界现在如何作为单一运行时事实来源暴露
2. 命令层如何切离本地默认 catalog
3. 终端层如何切离本地默认 catalog
4. 新增了哪些防分叉测试
