# MARKET 总入口拆分阶段收口 Prompt

你现在不是继续扩标准商品市场、定制商品市场或汇率市场的业务能力。

这次只做一件事：

把 `MARKET 总入口拆分` 这一阶段剩下的最后两个真实缺口收口，让它可以正式关阶段。

## 当前验收结论

这一阶段主体已经成立：

1. `MARKET` 根页已经不再直接充当标准商品交易详情页
2. 终端里已经有：
   - `MARKET`
   - `MARKET_STANDARDIZED`
   - `MARKET_CUSTOM`
   - `MARKET_EXCHANGE`
3. `TerminalMarketPageBuilder` 已经把：
   - 市场总览
   - 标准商品市场页
   - 定制商品市场页
   - 汇率市场页
   分开承接
4. `/jsirgalaxybase market` 帮助已经按三类市场入口分组
5. `market-entry-overview.md` 已经写出 MARKET 只做总入口、不再做混合详情页的说明
6. 定向测试和 assemble 当前都能通过

但还有 2 个真实缺口没有收口：

### 缺口 1：终端“路由回归测试”仍然太弱

当前仓库里的相关测试主要是：

- `src/test/java/com/jsirgalaxybase/terminal/ui/TerminalPageTest.java`

它目前只验证：

1. `TerminalPage` 的枚举分类
2. 页面 title 文本
3. `byIndex(...)` 的映射

这还不等于真正的 MARKET 路由回归保护。

因为真正的 MARKET 根页和三个市场子页装配发生在：

- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`

如果后续有人把：

1. `MARKET` 根页重新挂回标准商品详情页
2. 或把 `MARKET_CUSTOM` / `MARKET_EXCHANGE` 从装配链路里删掉

当前 `TerminalPageTest` 依然可能全部通过。

这意味着：

- 当前所谓“终端路由层回归测试”还没有真正保护住入口拆分成果

### 缺口 2：README 还没有把入口说明文档挂进索引

当前：

- `docs/market-entry-overview.md` 已经存在

但：

- `docs/README.md` 还没有把它列入当前建议先看的内容

结果是：

1. 这阶段虽然补了说明文档
2. 但 docs 索引没有把它正式纳入
3. 后续协作时容易只看到 prompt，看不到当前已落地的入口说明

## 这轮唯一目标

只收口上面 2 个缺口，不继续扩任何市场业务。

也就是说：

1. 补一条真正保护 MARKET 根页与三个子市场页装配关系的回归测试
2. 把 `market-entry-overview.md` 正式挂进 `docs/README.md`
3. 同步更新 `docs/WORKLOG.md`

## 必须先看这些文件

1. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`
2. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`
3. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`
4. `src/test/java/com/jsirgalaxybase/terminal/ui/TerminalPageTest.java`
5. `src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`
6. `docs/market-entry-overview.md`
7. `docs/README.md`
8. `docs/WORKLOG.md`
9. `docs/market-total-entry-split-prompt-2026-04-04.md`

## 必须完成的修补任务

### 1. 补一条真正的 MARKET 路由回归测试

目标不是继续测 `TerminalPage` 枚举，而是测：

- `TerminalHomeGuiFactory` 里的实际页面装配关系

最低要求：

1. 测试能证明 `MARKET` 根页对应的是市场总览页，而不是标准商品详情页
2. 测试能证明 `MARKET_STANDARDIZED`
3. 测试能证明 `MARKET_CUSTOM`
4. 测试能证明 `MARKET_EXCHANGE`
5. 这些页都真实挂在主终端装配链里，而不是只有枚举存在

允许的实现方式：

1. 为 `TerminalHomeGuiFactory` 提炼一个很薄的、可测试的 page registration / route mapping helper
2. 或增加 package-private 辅助方法，让测试能验证页面索引到 builder 的映射结果
3. 或用当前可承受的最小方式验证 `createPageBody(...)` 的装配决策

不接受：

1. 再加一条只测 `TerminalPage` 文本字段的测试
2. 只测 `isMarketPage()` / `byIndex()`，继续回避真正的装配关系
3. 为了测试把整个终端架构大改

原则：

- 用最小改动把真正需要保护的“挂页关系”暴露出来并测试掉

### 2. 把入口说明文档补进 docs 索引

要求：

1. 在 `docs/README.md` 中新增 `docs/market-entry-overview.md` 的索引项
2. 文案要说明它是 MARKET 总入口三分后的落地说明文档
3. 位置应放在当前 MARKET 总入口拆分 prompt 附近，保持阅读顺序合理

### 3. 更新 WORKLOG

要求：

1. 在 `docs/WORKLOG.md` 增加一条新的简短记录
2. 明确这一轮是“MARKET 总入口拆分阶段最后两处收口缺口修补”
3. 记录影响范围至少包括：
   - 相关测试文件
   - `docs/README.md`
   - `docs/WORKLOG.md`
4. 简要写清楚原因和结果

## 推荐实施顺序

1. 先判断如何以最小代价暴露可测试的 MARKET 路由装配关系
2. 先把测试补出来
3. 跑定向测试
4. 再补 `docs/README.md` 索引
5. 最后更新 `docs/WORKLOG.md`

## 这轮明确不做什么

1. 不继续改 MARKET 终端业务文案
2. 不继续扩标准商品市场 GUI
3. 不继续扩定制商品市场 GUI
4. 不继续扩汇率市场 GUI
5. 不重构整个终端框架
6. 不顺手做新的市场能力

## 验收标准

只有同时满足下面条件，这轮才算真正收口：

1. 已新增一条真正保护 MARKET 根页与三个市场子页装配关系的测试
2. 该测试不是枚举元数据测试，而是实际挂页关系测试
3. `docs/market-entry-overview.md` 已出现在 `docs/README.md` 索引里
4. `docs/WORKLOG.md` 已补记录
5. 定向测试通过

## 推荐汇报格式

完成后请按下面格式回报：

1. 这轮新增的 MARKET 路由回归测试保护了什么真实关系
2. 为了让测试可写，对 `TerminalHomeGuiFactory` 或相关类做了什么最小暴露
3. `docs/README.md` 新增了哪条入口说明索引
4. `docs/WORKLOG.md` 如何记录了这次收口
5. 跑了哪些定向测试，结果如何

## 给实现者的最后提醒

这轮不是“再做一点点功能”，而是“把当前已经做对的事真正锁住”。

如果你做完之后：

1. 后续有人误把 `MARKET` 根页重新接回混合详情页时，测试会立刻红
2. 协作者翻 `docs/README.md` 时能直接看到 `market-entry-overview.md`

那这轮收口就算完成了。