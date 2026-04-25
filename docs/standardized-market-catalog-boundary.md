# 标准商品市场目录与准入边界

日期：2026-04-03

这份文档只说明一件事：

- 标准商品市场当前怎样表达自己的正式目录边界

它不是交易功能说明，也不是定制商品市场或汇率市场文档。

## 当前正式对象

当前标准商品市场的准入边界由下面这些正式对象表达：

- `StandardizedMarketCatalogVersion`
  - 表示当前目录版本
- `StandardizedMarketCatalogEntry`
  - 表示单个目录项及其准入依据
- `StandardizedMarketAdmissionDecision`
  - 表示一次商品准入判断的结构化结果
- `StandardizedMarketAdmissionReason`
  - 表示准入 / 拒绝原因代码
- `StandardizedMarketCatalogService`
  - 表示标准商品市场自己的正式目录边界服务
- `StandardizedMarketCatalogSource`
  - 表示目录来源适配层

## 当前版本

当前运行时默认目录版本为：

- `standardized-spot-catalog-v1`

它表示：

- 当前标准商品市场已经有自己的正式目录版本语义
- 命令层、服务层、终端层都应以这版目录决策为准

## 当前来源

当前默认目录来源为：

- `gregtech-standardized-metal-adapter`

它的含义是：

- 当前首版目录来源暂时落在 GregTech 标准金属适配集合上
- 这只是当前可用来源
- 它不等于标准商品市场制度边界本体

也就是说：

- `StandardizedMarketCatalogService` 定义的是市场自己的准入边界
- `GregTechStandardizedMetalCatalog` 只负责提供当前首版来源数据

后续替换目录来源时：

- 命令层
- 终端层
- `StandardizedSpotMarketService`

都不应再重写整条交易链，只需要替换目录来源适配实现。

## 当前统一准入路径

当前下面这些主路径都应通过统一目录决策进入标准商品市场：

- `depositInventory`
- `createSellOrder`
- `createBuyOrder`
- 命令层商品参数校验
- 终端市场页商品展示与手持商品识别

统一目标是：

- 非准入商品被拒绝时，原因应表现为目录边界拒绝
- 不再由零散的临时判断各自决定“能不能进市场”

当前收口要求还包括：

- 命令层与终端层不再各自常驻默认目录实例作为主判断路径
- 运行时标准商品目录 decision 统一经由 `InstitutionCoreModule -> StandardizedSpotMarketService` 暴露，作为单一事实来源