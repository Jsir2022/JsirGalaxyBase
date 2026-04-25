# Terminal GUI 第三轮 Prompt：工程收口与市场 GUI 前置精修

你现在接手的是 JsirGalaxyBase 终端 GUI 在进入市场 GUI 之前的最后一轮精修。

这一轮的目标不是做新页面，不是继续堆更多视觉花样，也不是为了设计模式而设计模式。

这一轮只做一件事：把当前已经“好用、能看、可交互”的终端，收口成一个足够稳、后续市场 GUI 可以直接接上去的工程结构。

## 本轮前置事实

目前已经确认的结果：

1. 第一轮已解决大量长文本爆框和固定高度卡片问题
2. 第二轮已完成通知层、确认弹窗、终端外可见提示、轻量视觉组件与声音反馈
3. 玩家已经实际进游戏目检，确认当前 GUI 体验相比早期版本好了非常多
4. 当前主要工程风险不再是“能不能做出来”，而是“后续市场 GUI 是否会把当前主工厂继续堆成巨石类”

因此本轮不要回头重做视觉风格，也不要再做一轮纯 UI 美化；本轮是工程收口，不是视觉翻新。

## 先看这些文件

1. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`
2. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalDialogFactory.java`
3. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalNotification.java`
4. `src/main/java/com/jsirgalaxybase/terminal/ui/TerminalNotificationSeverity.java`
5. `src/main/java/com/jsirgalaxybase/terminal/TerminalHudNotificationManager.java`
6. `src/main/java/com/jsirgalaxybase/terminal/TerminalClientBootstrap.java`
7. `docs/terminal-gui-phase2-prompt-2026-04-02.md`
8. `docs/terminal-plan.md`
9. `docs/WORKLOG.md`
10. `../../.github/agents/GalaxyMod.agent.md`

## 当前问题判断

你需要基于当前代码承认下面这些问题已经出现，但不要用过度设计去解决：

### 1. TerminalHomeGuiFactory 已经偏大

它现在同时承担：

1. sync 注册
2. 页面装配
3. 通用 section / text / button helper
4. 银行页面构建
5. 银行表单输入与校验
6. toast 转发
7. 银行本地会话状态
8. 银行 sync state 打包

这还不是屎山，但如果市场 GUI 继续直接塞进去，很快就会变成屎山。

### 2. 通知语义仍然偏文案驱动

当前通知模型虽然已经存在，但仍然有一部分逻辑依赖从文本里推断 success / warning / error。

银行页还能勉强忍，市场页后续会出现：

- 下单已提交
- 订单部分成交
- 撤单成功
- claim 成功
- 资产冻结释放
- 操作失败但可恢复

这些都不应该靠字符串 contains 去猜语义。

### 3. Dialog 工厂已经够用，但还不够稳

它已经比早期版本好很多，但后续市场页会带来更长的确认信息、状态摘要和失败反馈。

因此现在需要把 Dialog 工厂再补一层“适合复用”的能力，而不是等市场页接进来再临时补丁。

### 4. 当前终端缺少最小测试保护

业务层测试已经不少，但终端 GUI 的基础逻辑几乎没有测试保护。

本轮不要求把 GUI 测试体系做成大工程，但至少要把最容易回归的纯 Java 逻辑补上。

## 本轮目标

本轮只做下面四件事：

1. 把 TerminalHomeGuiFactory 做一次最小但明确的职责拆分
2. 把通知从“文案推断”收口到“结构化语义优先”
3. 把 Dialog 工厂升级为足以承接市场确认链的可复用壳层
4. 补少量终端侧纯逻辑测试，给后续市场 GUI 提供最基本保护

## 本轮不做的事

1. 不重写整个终端框架
2. 不引入复杂 MVC / MVP / MVVM 套皮
3. 不为了“模式正确”拆出一堆只有一个调用点的类
4. 不把简单 helper 全部抽成抽象工厂、策略树或注册中心
5. 不开始做完整市场 GUI 页面
6. 不改银行业务语义
7. 不重做背包入口按钮坐标系统
8. 不把通知系统扩成全局消息中心或事件总线

## 设计原则

这是本轮最重要的限制，请严格遵守：

1. 只抽能明显降低主工厂负担、并且市场 GUI 未来会复用的部分
2. 如果一个抽象只服务一个地方，且没有明显复用价值，就不要硬抽
3. 优先做“组合型 helper / builder / section factory”，少做层层接口
4. 新增类数量要克制，目标是变清楚，不是变得更散
5. 以 Java 8 时代、当前代码风格、当前项目体量为准，不要引入现代企业框架式写法

## 建议实施范围

### 第一部分：对 TerminalHomeGuiFactory 做最小职责拆分

目标不是把 1800 行拆成 18 个类，而是把最会阻碍市场 GUI 的部分拿出去。

建议优先抽出下面几块中的 2 到 4 块：

1. 银行页面构建器
2. 通用终端 widgets / section / text helper
3. 银行本地会话状态控制器
4. 银行 sync state 绑定器或组装器

要求：

1. `TerminalHomeGuiFactory` 仍然保留为总装配入口
2. 页面路由和 open chain 不要改语义
3. 抽出的类名必须朴素直接，例如：
   - `TerminalBankPageBuilder`
   - `TerminalWidgetFactory`
   - `TerminalBankSessionController`
   - `TerminalBankSyncBinder`
4. 不要引入无意义接口，除非确实有两个以上稳定实现

验收标准：

1. 主工厂不再同时承载所有银行页面细节和全部通用 widget helper
2. 市场 GUI 后续如果要接页面，明显有新的落点，而不是继续往主工厂中段插 400 行代码

### 第二部分：把通知语义改成结构化优先

当前 `TerminalNotification` 可以保留，但通知的来源不应继续主要依赖文本推断。

要求：

1. 为终端动作结果建立最小结构化通知入口
2. 至少要能显式表达：
   - severity
   - title
   - body
   - autoCloseMillis
3. 银行当前已有的动作结果如果还只能返回文本，可以保留兼容桥接，但新的 GUI 层调用应优先走结构化构造
4. 文本推断逻辑可以保留为 fallback，但不要再作为主路径

可接受做法：

1. 给银行终端内部引入一个很薄的 `TerminalActionFeedback` / `TerminalNotificationPayload`
2. 或在现有 `TerminalNotification` 上补一个更明确的工厂方法，避免所有地方都 `fromBankMessage(...)`

不接受做法：

1. 为了这点需求引入完整事件总线
2. 为了“解耦”把所有字符串再包三层 DTO / mapper / adapter

### 第三部分：升级 Dialog 工厂，但只做市场前置需要的能力

你不需要做一个万能弹窗系统，只需要把它升级到足以服务市场确认链。

本轮至少补下面能力中的大部分：

1. 支持 detail lines 的换行或更稳的内容容器
2. 支持长正文时仍然可读，不再过度依赖固定高度普通 `TextWidget`
3. 支持可选 severity / accent，用于成功、警告、失败语义统一
4. 支持一到两个尺寸预设，避免未来每个确认框都重新写 magic number

要求：

1. 现有银行转账确认弹窗必须继续正常工作
2. 升级后的工厂调用方式要比现在更清楚，而不是参数越来越长
3. 如果参数已经太多，应改成朴素 builder 或 config object，但不要搞复杂继承体系

### 第四部分：补最小测试保护

本轮至少补一部分“纯逻辑、无需真实 GUI 环境”的测试。

优先级从高到低如下：

1. `TerminalNotification` 的结构化构造与 fallback 行为
2. `TerminalHudNotificationManager` 的去重、上限、过期移除
3. 银行表单本地 sanitize / parse 逻辑
4. Dialog config / builder 的默认值和关键行为

要求：

1. 不要求写 UI 截图测试
2. 不要求模拟 ModularUI2 真正渲染
3. 只测试纯 Java 逻辑，避免测试过脆

## 推荐实施顺序

1. 先抽主工厂里最重的通用 helper 或银行页构建器
2. 再把银行会话状态或 sync 绑定块从主工厂收口出去
3. 再升级通知入口，让结构化通知成为主路径
4. 再升级 Dialog 工厂接口
5. 最后补纯逻辑测试并跑完整验证

## 文档要求

1. 更新 `docs/WORKLOG.md`
2. 如果你新增了稳定可复用的终端组件层，可在 `docs/terminal-plan.md` 里补一句“终端已具备可扩展的页面装配与交互壳层”
3. 不要求额外写长文档，除非拆分后确实需要一个很短说明文件

## 强制验证要求

这一条仍然是硬性要求：

1. 编译出的产物需要实装到客户端和服务端
2. 客户端和服务端都需要实际启动
3. 需要进入游戏，由人工肉眼验证 GUI 没有因为本轮重构而退化

本轮不能只靠静态阅读和单测宣称完成。

至少要验证：

1. 主终端仍能正常打开
2. 银行首页、账户页、转账页、账本页仍能切换
3. 确认弹窗仍能正常拦截提交
4. 页内通知与终端外通知都没有退化
5. 长文本、tooltip、滚动文本、状态卡没有因为拆分类而错位

## 推荐命令

你可以按当前仓库和 GalaxyMod agent 已沉淀的流程执行，但至少要说明：

1. 编译命令
2. 测试命令
3. 客户端启动命令
4. 服务端启动命令
5. 实际人工验证了哪些界面

## 完成后请按这个格式回报

1. 本轮拆出了哪些类，各自职责是什么
2. `TerminalHomeGuiFactory` 现在相比之前少承担了哪些责任
3. 通知结构化入口是怎么落地的，哪些旧逻辑被保留为 fallback
4. Dialog 工厂新增了哪些市场前置能力
5. 新增了哪些终端侧纯逻辑测试
6. 客户端与服务端如何实装、如何启动、人工验证结果如何
7. 还有哪些工程问题被明确留到市场 GUI 之后再处理

## 给实现者的最后提醒

第三轮的关键词不是“优雅架构”，而是“可继续长”。

你要交付的是：

1. 不过度设计
2. 不继续堆巨石类
3. 足够支撑下一阶段市场 GUI 接入
4. 出问题时还能快速定位和修改

如果你写完之后感觉“类变多了一些，但主入口更清楚了，后续市场页知道该接到哪里”，那这轮方向就是对的。