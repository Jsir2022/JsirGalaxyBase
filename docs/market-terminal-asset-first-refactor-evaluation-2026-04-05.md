# 市场终端 Asset-First 重构评估

日期：2026-04-05

这份文档的目标不是直接开始改代码，而是基于当前仓库里的真实实现，先把市场终端下一轮重构的正式边界写清楚。

本轮评估只回答三件事：

1. 当前终端市场页到底是什么结构
2. 用户提出的“像 AE 终端 / 售货机那样先选物品，再进入该物品详情”的方向是否正确
3. 如果要做，哪些地方应该统一，哪些地方不能硬统一

## 1. 当前真实状态

### 1.1 MARKET 总入口已经拆开，但子页并不对称

当前 `MARKET` 已经不是旧的混合详情页，而是三类市场入口的总页。

这部分已经成立：

- `MARKET`
- `MARKET_STANDARDIZED`
- `MARKET_CUSTOM`
- `MARKET_EXCHANGE`

对应代码主要在：

- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketRoutePlan.java`

但三类子页当前并不处于同一成熟度：

- `标准商品市场` 已经有真实商品浏览、选中商品、盘口、交易动作和仓储动作
- `汇率市场` 已经有正式 quote / refresh / confirm / execute 主链
- `定制商品市场` 仍然主要是说明页和兼容命令提示

这意味着当前终端结构虽然“按三市场分家”，但并没有形成统一的可操作交互范式。

### 1.2 当前只有标准商品市场是半个“物品优先”结构

当前最接近用户提出方向的是 `标准商品市场`：

- 左侧是商品浏览区
- 右侧是商品详情区
- 点击商品后再进入对应盘口和操作

对应代码主要在：

- `TerminalMarketPageBuilder#createStandardizedMarketDashboard(...)`
- `TerminalMarketPageBuilder#createProductBrowser(...)`
- `TerminalMarketPageBuilder#createProductDetail(...)`
- `TerminalMarketSessionController#selectedProductKey`
- `TerminalMarketService#createSnapshot(...)`

但它仍存在明显问题：

- 当前的“选中物品”更像内部状态字段 `selectedProductKey`，而不是真正顺手的资产浏览体验
- 浏览列表来源主要围绕标准商品目录、订单簿、仓储和当前手持补齐，不是围绕“玩家实际想找的物品”设计
- 商品浏览、详情、动作、我的订单、CLAIMABLE 都被压在同一个 snapshot 结构里，导致标准商品市场状态天然成为整个市场终端的中心

### 1.3 定制商品市场和汇率市场并不符合当前标准商品页的数据假设

当前 `TerminalMarketSnapshot` 和 `TerminalMarketSyncState` 的主体字段几乎全部围绕标准商品市场组织：

- `productKeys`
- `productLabels`
- `selectedProductKey`
- `askLines / bidLines`
- `limitBuy / limitSell / instantBuy / instantSell`
- `sourceAvailable / claimableQuantity / frozenFunds`

然后再把汇率市场字段以附挂方式塞进同一个 snapshot：

- `exchangeHeldSummary`
- `exchangePairCode`
- `exchangeRuleVersion`
- `exchangeExecutableFlag`

这套结构对 `汇率市场` 已经只是勉强可用，对 `定制商品市场` 则几乎没有自然落点。

原因不是定制市场没做完，而是它的模型本来就不是：

- 商品目录
- 标准单位
- 盘口深度
- AVAILABLE / CLAIMABLE 仓储

定制商品市场的正式模型是：

- `CustomMarketListing`
- `CustomMarketItemSnapshot`
- `CustomMarketTradeRecord`
- `CustomMarketAuditLog`

所以当前终端层真正的问题不是“页面不够漂亮”，而是“状态模型把标准商品市场当成了市场终端默认母体”。

## 2. 用户提出的方向是否正确

结论：方向是正确的，而且比当前页面优先结构更接近玩家心智。

用户提出的核心需求不是单纯换皮，而是：

- 先看到自己关心的物品或可交易标的
- 点击后进入这个物品或标的的市场详情
- 再在详情页里执行买卖、挂牌、兑换、提取、取消等动作

这个方向可以概括为：

- `page-first` 终端 -> `asset-first` 终端

也就是：

- 当前是先进入某个“市场页”，再在页内寻找资产
- 用户希望先看到“资产列表 / 物品列表 / 标的列表”，再进入该资产详情

这与 AE 终端、售货机、VendorMachine 一类交互模型是一致的。

### 2.1 为什么这个方向更适合玩家

因为玩家真正关心的不是：

- 我现在在标准商品市场第几块卡片
- 我现在在 MARKET_CUSTOM 还是 MARKET_EXCHANGE 的哪一排说明区

玩家真正关心的是：

- 我想看这个物品现在多少钱
- 这个物品有没有卖盘或挂牌
- 我能不能把手里的这件东西卖掉
- 这个可兑换标的现在能换多少

所以从玩家视角看，“先找物品 / 标的，再进详情”比“先进页面，再理解这个页面属于哪类市场，再选内部对象”更顺手。

### 2.2 为什么这个方向不能简单照搬成一套统一页面

虽然方向正确，但不能直接把三类市场全部硬做成一模一样的“物品详情页”。

原因是三类市场的核心对象并不相同：

- `标准商品市场` 的核心对象是 `可目录化商品`
- `定制商品市场` 的核心对象是 `单条挂牌 / 单件物品快照`
- `汇率市场` 的核心对象是 `兑换标的 / 兑换对 / 当前手持可兑换资产`

如果强行统一成同一个详情模板，会出现三种跑偏：

1. 把定制商品市场误写成标准商品盘口页
2. 把汇率市场误写成商品订单簿页
3. 为了统一 UI，反过来破坏已经成立的三市场制度边界

所以正确方向是：

- 统一导航壳和交互节奏
- 不统一三类市场的核心详情语义

## 3. 正式设计边界

### 3.1 这轮应该统一什么

如果要做下一轮市场终端重构，应该统一的是“终端导航骨架”，而不是核心交易模型。

建议统一的内容：

- 一个共享的 `资产浏览层`
- 一个共享的 `详情页切换节奏`
- 一个共享的 `返回 / 收藏 / 最近使用 / 搜索` 交互壳
- 一个共享的 `选中对象 -> 进入详情 -> 打开确认动作` 操作节奏
- 一个共享的 `空状态 / 服务不可用 / 权限不足 / 无结果` 展示风格

也就是说，统一的是：

- `terminal market navigation shell`

不是：

- `terminal market domain model`

### 3.2 这轮不能统一什么

下面这些不能为了视觉统一而硬合并：

- 标准商品市场的买卖盘和即时成交
- 定制商品市场的单件挂牌和 pending / claim 状态
- 汇率市场的正式 quote、ruleVersion、limitStatus 和 execute
- 标准商品市场的仓储 AVAILABLE / ESCROW / CLAIMABLE
- 汇率市场的当前手持驱动兑换语义

正式要求应该是：

- 标准商品市场详情页仍是盘口页
- 定制商品市场详情页应是挂牌详情页
- 汇率市场详情页应是兑换详情页

### 3.3 标准商品市场的正式定位

标准商品市场最适合首先采用完整的 asset-first 模式。

因为它天然满足：

- 先选商品
- 再看盘口
- 再对该商品执行买卖和仓储动作

所以这条线的正式方向应是：

- 从当前“可用但偏内部状态驱动”的选中机制
- 升级为真正的“商品浏览器 + 商品详情终端”

这意味着：

- 商品列表应更像玩家可搜索和可浏览的目录视图
- 详情页应只聚焦当前商品，不继续堆积全市场信息块

### 3.4 定制商品市场的正式定位

定制商品市场不能照搬标准商品盘口页。

它的浏览层应该是：

- 挂牌列表
- 按物品快照聚焦的 listing 浏览
- 或当前玩家自己的 pending / selling / purchased 视图

它的详情页应该聚焦：

- 单件商品快照
- 卖家 / 买家 / 价格 / 挂牌状态
- buy / cancel / claim 等动作

所以定制商品市场适合做成：

- `listing-first` 的 asset-first 终端

而不是：

- `order-book-first` 的终端

### 3.5 汇率市场的正式定位

汇率市场也不应照搬标准商品盘口页。

它当前的真实业务核心仍然是：

- 当前手持任务书硬币
- 读取正式报价
- 查看 reasonCode / limitStatus / ruleVersion
- 确认执行兑换

因此汇率市场更适合分成两层：

- `标的入口层`
  - 当前支持的兑换标的或兑换对
- `兑换详情层`
  - 当前手持输入
  - 正式 quote
  - 执行入口

如果后续要加更多兑换对，可以把它做成 `asset-pair-first`。

但在当前阶段，不应该为了统一体验，就把它硬改成和标准商品市场一样的物品浏览器。

## 4. 对当前代码结构的正式判断

### 4.1 `TerminalMarketSnapshot` 不适合作为三市场统一母体继续膨胀

当前 `TerminalMarketSnapshot` 已经承载：

- 标准商品浏览
- 标准商品盘口
- 标准商品动作表单
- 标准商品仓储
- 汇率市场字段

如果下一轮再继续往里加：

- custom listing lines
- custom selected listing id
- custom pending buyer / seller panels
- custom item snapshot detail fields

那它会退化成新的“统一 MARKET 大桶 DTO”。

这是下一轮必须避免的事情。

### 4.2 `TerminalMarketSessionController` 当前是标准商品市场中心控制器

它当前维护的核心状态是：

- `selectedProductKey`
- 标准商品买卖数量和价格
- 即时买卖数量
- cancel / claim 的 pending id

汇率市场只是在这个控制器上再挂了：

- refreshExchangeQuote()
- submitExchangeHeld()

这意味着如果下一轮直接把定制市场继续塞进去，这个类会进一步变成巨石控制器。

正式判断应是：

- 下一轮不应继续向 `TerminalMarketSessionController` 追加所有市场状态
- 而应考虑把标准商品 / 定制商品 / 汇率市场拆为各自 session controller
- 再由一个更薄的 market terminal shell 去负责路由和共享导航

### 4.3 `TerminalHomeGuiFactory` 当前已经有继续拆壳的基础

当前好消息是：

- `TerminalHomeGuiFactory` 已经不再直接内嵌所有市场细节
- `TerminalMarketPageBuilder` 已经独立出来
- bank / market 页面已经按 builder 和 sync state 分层

所以这轮并不是从零开始重构。

真正的下一步应该是：

- 继续沿着已拆开的 builder / sync / controller 结构，把市场三条线分得更干净

而不是：

- 回头把所有页继续塞回 `TerminalHomeGuiFactory`

## 5. 正式重构方向

基于当前代码，推荐的正式方向是：

### A. 先把市场终端重构成“共享浏览壳 + 三类详情页”

建议的统一结构：

- `市场总入口`
  - 标准商品市场
  - 定制商品市场
  - 汇率市场
- `每条子线内部`
  - 浏览层
  - 详情层
  - 动作确认层

### B. 标准商品市场先完成真正的 asset-first 商品终端

这一条线是最适合作为下一轮第一步的。

因为：

- 已有商品列表
- 已有详情区
- 已有交易动作
- 只差把“内部字段驱动”提升成“玩家视角可浏览”

### C. 定制商品市场复用浏览壳，但不用标准商品盘口模型

它应做成：

- 列表页：挂牌 / 我的挂牌 / 我的待领取
- 详情页：单条 listing 快照与操作

### D. 汇率市场复用“先选标的，再进详情”的壳，但保持兑换模型独立

它应保留：

- 正式 quote 字段
- reasonCode / notes / limitStatus
- refresh / confirm / execute

而不是强行长成标准商品盘口页。

## 6. 下一轮明确不做什么

这份评估也明确限制下一轮不要做的事情：

1. 不把三类市场重新合并成一个统一大 MARKET 详情页
2. 不为了 UI 统一，把定制市场改写成订单簿
3. 不为了 UI 统一，把汇率市场改写成商品盘口
4. 不在下一轮顺手扩议价、搜索排序、历史成交页、复杂过滤器
5. 不继续在 `TerminalMarketSnapshot` 上直接追加越来越多的跨市场字段

## 7. 当前阶段的正式结论

当前用户提出的需求方向是对的，但正确实现方式不是：

- “把当前市场页简单改得更像 AE”

而是：

- “把市场终端从 page-first 正式升级成 asset-first / listing-first / asset-pair-first 的统一导航壳”

并且必须同时承认下面三点：

1. 标准商品市场最适合完整采用物品优先终端
2. 定制商品市场应做成挂牌优先详情页，而不是盘口页
3. 汇率市场应做成兑换标的优先详情页，而不是商品订单簿

如果下一轮实现后，玩家进入市场终端时首先想到的是“我要找哪个物品 / 哪个挂牌 / 哪个可兑换标的”，而不是“我要先进入哪种页面结构”，那这轮方向就是对的。