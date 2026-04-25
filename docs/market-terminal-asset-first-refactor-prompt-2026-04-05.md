# 市场终端 Asset-First 重构 Prompt

你现在接手的不是继续扩标准商品市场业务，也不是继续扩定制商品市场最小挂牌链，更不是回头重做汇率市场规则层。

这次只做一件事：

把当前已经按三市场分家的终端市场页，从 `page-first` 结构正式推进成 `asset-first` 导航结构。

这里说的 asset-first，不是指三类市场全部变成同一张页面，而是指：

- 玩家先找到自己关心的物品 / 挂牌 / 可兑换标的
- 再进入该对象的详情页
- 再在详情页里执行对应操作

## 当前已成立的前提

这些结论已经成立，不能回退：

1. `MARKET` 已经是总入口，不再是混合详情页
2. `标准商品市场`、`定制商品市场`、`汇率市场` 已经是正式三分结构
3. 标准商品市场拥有订单簿、统一仓储和 CLAIMABLE 语义
4. 定制商品市场拥有自己的 listing / snapshot / trade / audit 模型
5. 汇率市场拥有自己的 formal quote / ruleVersion / limitStatus / execute 语义

因此这轮不能做下面这些事：

1. 不能把三类市场重新混成一个统一 MARKET 巨页
2. 不能为了统一 UI，把定制商品市场改写成标准商品盘口页
3. 不能为了统一 UI，把汇率市场改写成商品订单簿页
4. 不能继续在 `TerminalMarketSnapshot` 上直接追加越来越多的跨市场字段
5. 不能顺手扩议价、复杂搜索排序、历史中心或完整物流系统

## 这轮唯一目标

把市场终端改造成：

- 共享导航壳一致
- 三类市场详情语义分离
- 玩家交互从“先找页面”变成“先找对象”

换句话说，这轮目标不是“重画一页 UI”，而是：

- `市场总入口 -> 子市场浏览层 -> 子市场详情层 -> 动作确认层`

## 必须先看这些文件

1. `docs/market-three-part-architecture.md`
2. `docs/market-entry-overview.md`
3. `docs/custom-market-minimal-model.md`
4. `docs/market-terminal-asset-first-refactor-evaluation-2026-04-05.md`
5. `docs/WORKLOG.md`
6. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`
7. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`
8. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`
9. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSessionController.java`
10. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSnapshot.java`
11. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncState.java`
12. 现有市场终端测试和命令测试

看的目的不是为了继续堆字段，而是先确认：

1. 当前哪些状态只属于标准商品市场
2. 当前哪些页面已经有可复用的浏览壳
3. 下一轮应该把哪些市场状态拆成子控制器

## 当前问题判断

当前真正的问题不是“市场页内容不够多”，而是：

1. 只有标准商品市场半成型地拥有“对象浏览 -> 详情 -> 动作”的节奏
2. 定制商品市场仍是说明页，无法进入真实 listing 详情操作
3. 汇率市场仍是手持驱动面板，不是明确的标的详情页
4. `TerminalMarketSnapshot` / `TerminalMarketSessionController` 已经天然偏向标准商品市场
5. 继续往这两个类里堆 custom / exchange 状态，只会造出新的统一大 MARKET DTO 和统一大 MARKET controller

## 这轮必须完成的重构任务

### 1. 正式拆出“共享市场导航壳”和“子市场控制器边界”

最低要求：

1. 不再把全部市场交互状态继续集中在一个 `TerminalMarketSessionController`
2. 为标准商品市场、定制商品市场、汇率市场各自建立更薄、更明确的 session/controller 边界
3. 总控层只负责：
   - 当前子市场路由
   - 浏览层 / 详情层切换
   - 共享通知与弹窗壳

不接受：

1. 继续往当前 `TerminalMarketSessionController` 里加 custom listing id、custom price、exchange pair selection 等新字段
2. 继续往 `TerminalMarketSnapshot` 里叠更多跨市场大杂烩字段

### 2. 把标准商品市场正式收口成真正的商品优先终端

目标：

1. 先展示商品浏览层
2. 玩家点击商品后进入该商品详情层
3. 详情层只聚焦当前商品的：
   - 买盘 / 卖盘
   - 成交摘要
   - 买卖动作
   - 仓储与 claim 动作

要求：

1. 继续沿用已存在的标准商品业务链，不重做撮合模型
2. 优化当前“选中商品”的内部状态驱动感，让它更像真实商品终端，而不是测试用状态字段
3. 避免详情层继续混入全市场说明卡片

### 3. 为定制商品市场落第一轮真实 GUI 主链

目标不是完整市场，而是先形成：

- `listing-first` 的 custom terminal

最低要求：

1. 有可浏览的挂牌列表或玩家视图列表
2. 点击后能进入单条 listing 的详情页
3. 在详情页里支持与当前后端一致的真实动作：
   - buy
   - cancel
   - claim
4. 不把它伪装成标准商品盘口页

允许第一轮保留简单范围：

1. active listings
2. my selling / my pending
3. listing details

### 4. 把汇率市场改成“标的优先详情页”，而不是纯手持说明板

这轮不要求做完整多兑换对市场，但必须让它更像一个对象详情页。

最低要求：

1. 明确存在“兑换标的 / 当前输入对象”的详情入口
2. 当前手持正式报价、规则字段、执行门禁仍保留
3. refresh / confirm / execute 主链不能回退
4. UI 上要体现“先看对象详情，再做兑换动作”的节奏，而不是继续停留在说明板 + 按钮

不接受：

1. 把汇率市场改成标准商品盘口页
2. 把标准商品的 productKey 机制直接硬套到汇率市场

### 5. 补终端层回归测试，锁住新的导航结构

至少补这几类测试：

1. 标准商品市场浏览层与详情层切换
2. 定制商品市场 listing 详情路径
3. 汇率市场对象详情与执行门禁路径
4. 共享市场壳不会把三类市场重新混成统一详情页

原则：

1. 测真正的页面装配和控制器边界
2. 不要只测枚举文本和字符串常量

## 推荐实施顺序

1. 先拆 market terminal 的共享壳与子控制器边界
2. 先把标准商品市场收口成真正的商品优先终端
3. 再把定制商品市场接入第一轮 listing-first GUI
4. 再把汇率市场收口成标的优先详情页
5. 最后补测试、文档与 worklog

## 这轮明确不做什么

1. 不扩标准商品市场新业务模型
2. 不扩定制商品市场议价、搜索排序、争议系统
3. 不扩汇率市场新报价源或复杂兑换对配置
4. 不回退 MARKET 总入口拆分成果
5. 不做新的“统一 MARKET 大 DTO”或“统一 MARKET 大 Controller”

## 验收标准

只有同时满足下面条件，这轮才算完成：

1. 市场终端已从 page-first 明确转向 asset-first 导航壳
2. 标准商品市场是商品优先详情页
3. 定制商品市场不再只是说明页，而是有第一轮真实 listing-first GUI 主链
4. 汇率市场保留正式 quote 主链，同时具备更明确的对象详情节奏
5. 三类市场没有被重新混成统一详情页
6. 终端层回归测试覆盖新的浏览层 / 详情层主链
7. `docs/WORKLOG.md` 已记录本轮变更

## 推荐汇报格式

完成后请按下面格式回报：

1. 这轮怎样把市场终端从 page-first 改成了 asset-first
2. 标准商品市场、定制商品市场、汇率市场各自的新浏览层 / 详情层边界是什么
3. 为了避免继续长成巨石类，拆出了哪些 controller / snapshot / builder 边界
4. 新增了哪些回归测试，分别保护了什么
5. 跑了哪些定向测试，结果如何

## 给实现者的最后提醒

这轮不是为了做一张更花的市场页，而是为了让玩家以“找对象”的方式使用市场终端。

如果你做完之后：

1. 玩家先找物品 / 挂牌 / 标的，再看详情
2. 三类市场仍保持各自交易语义
3. 代码结构比现在更不容易继续长成统一 MARKET 巨石

那这轮方向就是对的。