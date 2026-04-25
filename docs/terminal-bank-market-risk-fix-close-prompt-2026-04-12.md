# Terminal Bank / Market 风险修复收口 Prompt

日期：2026-04-12

下面这份 prompt 用于处理 `JsirGalaxyBase` 当前 terminal bank / market 风险修复在严格验收后仍然留下的两个收口缺口。

这不是新一轮功能扩展 prompt，也不是继续重构 terminal 装配结构的 prompt。

你的任务是只把这轮已经完成的大部分修复做实收口，补齐文档闭环与回归测试闭环，避免后续再次出现“代码已改但 WORKLOG 缺项”或“helper 计划与实际 action 注册漂移但测试没拦住”的问题。

---

你正在 `JsirGalaxyBase` 仓库中工作。请直接修改当前 terminal bank / market 实现，只处理这次严格验收后确认仍未完全收口的两项问题。

## 开始前必须先读

开始编码前，先完整阅读并遵守下面这些文档：

- `README.md`
- `docs/terminal-gui-continue-current-implementation-prompt-2026-04-06.md`
- `docs/terminal-bank-market-strict-close-prompt-2026-04-11.md`
- `docs/terminal-bank-market-risk-fix-prompt-2026-04-12.md`
- `docs/WORKLOG.md`

同时必须阅读这几个实际实现文件：

- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeActionBindingPlan.java`
- `src/test/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactoryTest.java`

## 本轮目标

本轮只处理下面两件事：

1. 补一条真正记录“本次 terminal bank / market 风险修复已落地实现”的 `docs/WORKLOG.md` 条目，而不是只保留之前那条 prompt 产出记录
2. 把回归测试从“只验证 helper plan 自身”提升到“能约束 `TerminalHomeGuiFactory` 实际 action 注册与 helper plan 保持一致”，避免后续 action 注册漂移却仍然测试通过

注意：

- 本轮不是继续补新的银行快照失效动作
- 本轮不是重新讨论 `marketDepositConfirmed` 业务语义
- 本轮不是继续改 market / bank / exchange controller 逻辑
- 本轮不是继续做 terminal 架构重构

## 已确认的剩余收口问题

### 问题 1：WORKLOG 只有 prompt 记录，没有实际实现记录

当前 `docs/WORKLOG.md` 已经有一条关于：

- `terminal-bank-market-risk-fix-prompt-2026-04-12.md`

的“产出 prompt”记录。

但严格验收后已经确认，缺少一条真正描述本次代码实现完成情况的 worklog 条目。

这会导致文档链路出现偏差：

- 看 WORKLOG 只能看到“提出修复 prompt”
- 看不到“实际已经完成了哪些 terminal risk fix”

本轮要求：

- 在 `docs/WORKLOG.md` 新增一条实际实现记录
- 必须明确写清这次真正落地了什么，而不是重复写“产出 prompt”
- 至少明确记录下面三点：
  - 通过 `TerminalHomeActionBindingPlan` 收口了会触发银行快照失效的 terminal action 集合
  - `marketDepositConfirmed` 被明确排除在银行快照失效集合外，并写清判断边界
  - `TerminalHomeGuiFactory` 旧 market 挂载残留分支已最小删除或等价收口，不再保留已确认的 custom panel 错接线风险

### 问题 2：当前测试只锁 helper plan，没有真正锁住 HomeGuiFactory 注册一致性

当前 `TerminalHomeGuiFactoryTest` 已经覆盖：

- 哪些 action name 应该出现在 `TerminalHomeActionBindingPlan` 的银行快照失效集合中
- `marketDepositConfirmed` 不在这个集合中

但这类测试只证明 helper plan 自己写对了，不能充分证明：

- `TerminalHomeGuiFactory` 里实际注册的 action 真的和 helper plan 保持一致
- 后续有人在 HomeGuiFactory 新增、删改或绕过 helper 注册时，测试一定会失败

这会留下一个真实维护风险：

- helper 计划表是对的
- 实际 GUI action 装配却可能悄悄漂移
- 结果测试仍然通过

本轮要求：

- 至少新增或改造一条测试，能直接约束 `TerminalHomeGuiFactory` 的实际 action 注册与 `TerminalHomeActionBindingPlan` 的关系
- 如果需要为测试提炼 package-private 的极薄装配入口、可枚举 action 名单或注册计划快照，可以做，但必须是最小改动
- 不允许为了写测试把 `TerminalHomeGuiFactory` 大改成另一套架构

可接受的收口方式示例：

1. 提炼 `TerminalHomeGuiFactory` 内部实际会走 helper 包装的 action 名单，并让测试断言它与 `TerminalHomeActionBindingPlan.bankSnapshotInvalidationActions()` 完全一致
2. 或者提炼一层最薄的注册计划对象，让测试既能看到 action 名称，也能看到哪些 action 会附带 `bankSessionController::markSnapshotDirty`

核心标准只有一个：

- 未来如果 helper plan 和 HomeGuiFactory 实际注册发生漂移，测试必须能拦住

## 允许修改的范围

本轮原则上只应修改与这两个收口问题直接相关的文件，例如：

- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`
- `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeActionBindingPlan.java`
- `src/test/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactoryTest.java`
- `docs/WORKLOG.md`

如果为了测试需要补 very thin 的 package-private 辅助方法或可见性调整，可以做，但必须保持最小、直接服务于这轮收口。

## 本轮明确不做什么

下面这些不要顺手扩大范围：

1. 不再新增或删除银行快照失效动作集合
2. 不继续改 market / bank / exchange 业务语义
3. 不重做 terminal action 路由或 GUI 装配架构
4. 不继续清理与本轮无关的 terminal 历史残留
5. 不继续做 terminal 视觉层、页面层或 sync 层重构

## 测试要求

本轮至少补或更新下面这些验证：

1. 一条能证明 `TerminalHomeGuiFactory` 实际 action 注册和 `TerminalHomeActionBindingPlan` 不会发生静默漂移的测试
2. 保留现有“银行快照失效 action 集合”断言价值，但不能只停留在 helper 自测层
3. 至少运行与本轮直接相关的 terminal UI 定向测试，并在最终说明里列出

如果受限于当前测试环境，无法直接读取完整 GUI 运行态注册结果，可以提炼最小可测装配入口，但必须在最终说明中明确测试边界。

## 实现原则

- 只修收口缺口，不扩大业务范围
- 优先让测试绑定真实装配链，而不是重复验证静态常量
- 保持当前 terminal 单实现主链，不回退 legacy 结构
- WORKLOG 记录要写成“实际实现完成”，不是“再次产出 prompt”

## 文档要求

本轮至少要做：

- 更新 `docs/WORKLOG.md`

必须明确写清：

- 这是 terminal bank / market 风险修复的实际收口实现
- 已补银行快照失效 action 计划与 HomeGuiFactory 装配的一致性保护
- 已把上轮风险修复真正落地的事实补回记录链

## 最终输出要求

完成后，请明确汇报：

1. 你新增的 WORKLOG 实现记录写了什么重点
2. 你如何让测试不再只验证 helper plan，而是能约束 HomeGuiFactory 实际注册
3. 改了哪些文件
4. 补了哪些测试或测试入口
5. 实际运行了哪些定向测试
6. 是否还有明确留给下一轮的问题

## 明确禁止事项

- 不新增 fallback GUI
- 不重写 terminal action 架构
- 不把这轮扩大成新一轮 terminal / market / bank 功能开发
- 不把这轮再次写成只产出 prompt、不落实际收口

这轮的验收标准很直接：

- WORKLOG 里必须能看到这次风险修复已经真实落地，而不只是曾经写过 prompt
- 测试必须能在 HomeGuiFactory 实际注册与 helper plan 漂移时失败

如果这两点没有同时做到，就不算完成。