# JsirGalaxyBase Work Log

日期：2026-03-29

这份文件用于记录 `JsirGalaxyBase` 的持续开发摘要。
从本次开始，后续每次实际代码变更都应补一条简要 work log。

### 2026-09-08 - 银河职业、贡献与跨服任务平台 Q.0 / Q.1 起步

- 源码与现场：固定调查 GTNH 当前 `BetterQuesting-3.7.15-GTNH` 源码提交 `524b365211b6b3a9672cab8ae45b4e07e726d49e`；确认 BQ 将定义、玩家进度、队伍、生命和名称分离保存，现场 S1/S2 分别有 40/29 个玩家进度文件且存在分歧，不能以单服 JSON 充当跨服真源。
- 方案：新增 `docs/galaxy-career-contribution-cross-server-quest-platform-2026-09-08.md`，明确吸收任务、编辑、检测、进度和奖励语义，但不搬 BQ GUI、网络、文件存储单例与 Mod 启动架构；阶段按 Q.0-Q.8 推进，最终由 PostgreSQL 和 UI2 终端接管。
- 实现：新增纯 Java 8 `galaxy-quest-core`，首批提供稳定 UUID 任务定义、Task/Reward 定义、AND/OR、前置门禁、单调进度合并、完成状态和稳定奖励资格键，以及编辑器的重复 key、未知类型、自依赖、缺失依赖和依赖环校验；继续加入跨服稳定事实键、Task evaluator 注册表及计数事实投影，为后续 Minecraft 检测器提供平台无关归约入口。
- 合规：记录 BetterQuesting MIT 许可证、固定版本和来源清单，并将许可证/来源纳入正式 JAR 资源；新任务内核没有 BetterQuesting 运行时依赖。
- 验证：CodeGraph 同步新增 22 个、修改 2 个源文件，`QuestEngine` 影响仅落在新内核及其定向测试；Docker Gradle 的 `:galaxy-quest-core:test`、根项目 `test`、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 全部通过。正式 JAR 已核验包含任务内核 class、BetterQuesting MIT 文本和来源清单，SHA256 为 `41d49725aa039f0afaf6527c79f5eff100184699faf035c66fea3cb21d1cbe32`；本批未部署、未修改数据库、未触碰 S1/S2 运行状态。
- Q.1 扩展：参与主体扩展为 PLAYER/PARTY/TEAM/PUBLIC，队伍事实按显式成员快照归入同一进度；重复任务使用 cycle 隔离进度与奖励资格，仓储端口要求事实去重、进度和奖励资格在事务中提交。新增定义版本身份保护，防止旧进度误套新定义。
- Q.2 起步：新增纯 Java `galaxy-quest-bq-importer`，只读解析 BQ typed NBT-JSON，保留完整原始 Task/Reward 配置，并导入玩家完成、领取和时间戳。实际扫描 S2 DefaultQuests 共 3744 个任务，仅使用 9 种 BQ Standard Task 和 4 种 Reward；导入器测试通过，不写现场文件或数据库。
- 本轮续验：Q.1/Q.2 定向测试、CodeGraph 同步与影响检查、根 `assemble`、`git diff --check` 均通过；加入 importer 子项目后的正式 JAR SHA256 为 `5d48cecbe3dac6feb74aaeb97cfd5d353afdde6aa7b4a0034b944de005a4d313`。Importer 是开发/迁移工具，当前没有打入运行 JAR，也没有引入 BetterQuesting 运行时依赖。

### 2026-09-08 - UI2 纯 Java视觉闭环 V.0

- 新增独立方案 `docs/galaxy-ui2-pure-java-visual-validation-2026-09-08.md`：自动化只负责越界、重叠、文本、裁剪、图表可见性和多尺寸等结构正确性，交互手感与主观视觉继续由人工验收。
- `UiDebugSnapshot` 可导出 UI2 组件树、最终边界、DrawList 及越界、文字高度、重复外部区域 ID 告警；UI Lab 无头导出扩展到 `350x193`、`427x240`、`620x340`，并生成 PNG、DrawList、`.ui2.txt` 和 HTML 画廊。
- UI2 Screen/Container 宿主增加只读调试捕获桥；现代终端页面按 F9 会在客户端 `screenshots/` 同时保存 Minecraft Framebuffer PNG 与 UI2 结构侧车文件，不自动操作、上传或改变业务状态。
- CodeGraph 在改动前同步完成；`UiLabExporter` 仅影响 Demo 测试，现代 Screen 宿主影响终端应用页和 UI Lab，`TerminalPageDocument` 影响 223 个符号，因此正式业务 Document 下沉拆到后续 V.1-V.4，避免本批形成根项目到 Demo 的反向依赖。
- 自动验收：Core 41、Lab 2、Demo 5、根项目 458 项（52 项按既有环境条件跳过），共 506 项零失败；三档 15 张 PNG 的结构审计均为 0 告警，`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过。运行 JAR SHA-256 为 `9d27e17c1e6504e8157388835660e681f057fb9cfe285a930f9d1d293bce4afb`，本轮未部署或启动客户端。

### 2026-09-06 - UI2 文字正确性、主题包与终端设置页

- 根据实机首页截图和 CodeGraph 调用链确认：正式 UI2 只有首页与资产中心，其他业务仍进入旧
  `TerminalHomeScreen`；首页窗口尺寸本身有效，爆版来自 35px 卡片承载三条 14px 行高文字、无换行、
  无省略且无裁剪，原有测试只检查窗口矩形而未检查文字命令。
- UI2 Core 新增测量驱动 `TextFlow`，支持中文逐字换行、显式换行、最大行数、Unicode 代理对安全截断和
  末行省略。生产 Label、按钮、徽章、状态页和 Dialog 统一使用主题字体 Token并在自身边界建立 Clip；
  首页改为紧凑两列卡片，并补三档验收尺寸的文字边界测试。
- 新增客户端 `TerminalPreferences`、`TerminalThemeRegistry` 与 `TerminalAppearance`，提供
  `glass_mono / hacker_green / high_contrast` 三套内置主题、三档密度、三档窗口尺寸、减少动画和 Tooltip
  延迟。偏好写入独立 `config/jsirgalaxybase-terminal-ui.cfg`，不进入服务器配置、网络或数据库。
- 新增 Resource Pack 数据主题入口；多个已启用资源包的 `ui2/themes/index.json` 可注册数据主题，坏条目
  单独忽略并回退内置主题，不允许主题携带代码或业务行为。
- 新增现代 `TerminalSettingsScreen/Document`，设置入口固定在共享导航底部；主题、密度、窗口和减少动画
  即时应用并持久化，原生物品槽保持固定逻辑尺寸。下一步继续建立统一非 Container 页面注册表并迁移
  通知、银行与 ServerTools，不用现代边框包装旧 Panel。
- CodeGraph 在修改前确认 `TerminalAppShell.layout` 仅有首页与资产两个调用者，旧主屏影响 212 个符号；
  修改中同步 32 个文件，并复核 `TextFlow`、新主题注册表和设置页影响链；CodeGraph 未自动推导测试，故按
  合同显式执行。Docker Gradle 最终通过根项目 525 项（52 项条件跳过）、UI2 Core 30 项、Lab 2 项、
  Demo 4 项，0 failure/0 error；`:ui2-demo:renderGolden`、`assemble`、`verifyUi2SelfContained` 与
  `git diff --check` 均通过。runtime JAR SHA-256 为
  `4e416c24eeb56d4f4f18cd1d8aa01c1d514015bcbe6608993f076cabf9d75826`。本批未部署、未启动客户端、
  未触碰 S1/S2。

### 2026-09-06 - UI2 终端窗口化、玻璃主题与首页同壳整改

- 根据资产中心实机截图将 C.2 标记为“业务可用、视觉未通过”：根因是 `ModernContainerHost` 仍以全屏
  viewport 承载生产文档、UI2 使用近不透明藏蓝主题，而资产返回由 `TerminalRouteCoordinator` 重建旧
  `TerminalHomeScreen`，形成全屏新页面与旧窗口首页之间的代际跳变。
- 新增共享 `TerminalWindowMetrics`：在完整 Minecraft 事件宿主内居中放置 `350×193` 至 `620×340`
  的响应式终端窗口；常规视口按 90% 宽、88% 高布局。资产 Vault、背包、Bay 与 Cell 外部区域继续使用
  原生 Slot Bridge，但全部限制在窗口内。
- 新增生产专用 `GlassTerminalTheme`，使用半透明黑色窗口、低透明中性表面、冷白文字和青绿色信号色；
  选中页签改为弱玻璃底、细强调边框及一像素指示线，导航改用紧凑文字样式，槽位颜色改由 Theme Token
  提供。F8 UI Lab 继续保留原工业主题，避免影响平台实验基线。
- 新增 `TerminalHomeScreenV2` 与 `TerminalHomeDocument`。终端首次进入首页、资产中心返回首页、旧页面
  点击首页均进入同一个 UI2 窗口壳；仓储入口继续由服务端 `VAULT_OPEN` 打开权威 Container，其他未迁移
  业务页保持旧路由。刷新、帮助 Modal、HUD 通知和通知目标路由一并接回。
- CodeGraph 同步 19 个文件；`callers TerminalAppShell.layout` 确认仅首页与资产使用新窗口合同，`impact`
  复核窗口、首页、路由与双宿主链路，`affected` 未自动推导测试，因此显式执行窗口、首页、资产定向测试
  及全量回归。Docker Gradle 的 Core/Lab/Demo、10 组 golden、根 `test`、`assemble`、
  `verifyUi2SelfContained` 全部通过，共 565 项、52 项既有条件跳过、0 failure、0 error；
  `git diff --check` 通过。
- 交付：同一 runtime JAR 已部署到 Lobby 与客户端，SHA-256 均为
  `d5a5f330f298a6d69810b651a8b8de0316ea99509f48371d7ffa2440484e313c`；Lobby 恢复为 `RUNNING`
  并在最终启动日志到达 `Done (1.271s)`。未启动客户端、未执行数据库 migration，未触碰 S1、S2。
  当前等待玩家实机确认窗口比例、透明度、导航密度和首页/资产往返观感。

### 2026-09-05 - UI2 实机圆角、槽位与字体首帧稳定性修正

- 根据 F8 UI Lab 实机截图定位三项根因：圆角抗锯齿带只向外扩造成边缘毛刺；Container
  方形槽位底板覆盖了 UI2 小圆角；Noto 字形异步上传前使用 9px 原版字体及其 advance，
  上传后才切换到 UI2 最终度量，产生约一秒后的文字放大。
- 圆角 coverage 改为数学边界内外各半个物理像素，hairline 始终为一个物理像素，细分环
  使用三角形并关闭面剔除；槽位视觉与原生 Slot Bridge 统一使用 16px/2px 间隔网格，
  Container 仅绘制原生物品和 Tooltip，不再覆盖方形底板。
- 字体回退按最终 Noto 字形宽高缩放并沿用最终 advance；可见文字先预取，最多 48 个字形的
  上传步骤从“每条文字”改为“一帧一次”，Container 的浮层二次绘制也不重复上传。
- 新增回退字体尺寸、物理像素圆角、共享槽位几何及 Slot 对齐测试。CodeGraph 同步 15 个
  改动文件并复核 Font、Renderer、双宿主和 Slot Bridge 影响链；`affected` 未自动推导测试，
  因此显式执行全部 UI2 与根项目回归。Docker Gradle 的 Core/Lab/Demo、10 组 golden、全量
  `test`、`assemble`、`verifyUi2SelfContained` 全部通过，共 543 项、52 项既有条件跳过、
  0 failure、0 error；`git diff --check` 通过。
- 交付：runtime JAR 与 Prism 客户端目标 SHA-256 均为
  `9ecd9956e92191cdf8e7e2eb0f068f4b50ce44c8b2bb6b32007e6cb6b8d4ee9c`。仅部署客户端且未启动，
  未触碰 Lobby、S1、S2；未修改正式终端业务页面、服务端合同或数据库。

### 2026-09-05 - Galaxy UI 2 Batch B.1-B.4 Qz 算法剪裁与渲染加固

- 边界：保留 Base 自研 UI2 Core、单向 Store、组件树、DrawList、双宿主和 Java2D 后端；仅按固定提交剪裁
  Qz 1.8.2/4.1.3-LTS/4.7/4.8 的成熟算法，不依赖、不调用、不打包 Qz Mod，也未修改整合包现有 Qz。
- 稳定性：Modal 以稳定 key 跨 Runtime 重建保留焦点和捕获，Effect 重入动作在一次 flush 内排队收敛并有
  1000 动作循环门禁。实机第四页崩溃定位为 Modal 两条文字误用了 CONTENT 层，现已统一为 MODAL 层并补
  打开、重建、Escape 关闭回归。
- 渲染：`MinecraftUiRenderer` 拆分 Shape/Text/Image/Clip/GL 状态后端；圆角改为半径限幅、物理密度动态细分、
  内部实色加一物理像素 coverage fringe，边框改用内外填充环，卡片/按钮圆角同步收敛。
- 字体：增加 generation 化字形请求去重、每帧 48 个上传预算、过期拒绝和 Atlas 上传失败整体回滚；UI Lab
  增加 generation、页数、队列、上传、DrawCommand、Clip、frame/render 时间诊断。
- UI Lab：浮层页现在同时展示 ContextMenu、Tooltip、Dialog 和 Toast 的真实层级，Dialog 仍位于普通浮层之上，
  Toast 位于反馈顶层；库存页继续沿用原生 Slot Bridge 和 Modal 阻断。
- 合规：新增独立设计、固定提交来源清单、MIT/LGPL 文本和第三方声明；`verifyUi2SelfContained` 扫描生产源码与
  JAR class，禁止 Qz 包、Mod ID、Mixin 或运行时引用，并验证许可证和 provenance 均进入 JAR。
- 验证：CodeGraph 同步 27 个改动文件并复核 Runtime、Renderer、Font 与双宿主影响链；`affected` 未推导出测试，
  因此按影响链显式执行 UI2 Core/Lab/Demo、Minecraft adapter 与全量回归。Docker Gradle 的 UI2 定向测试、
  Java2D 10 组 golden、全量 `test`、`assemble` 和自包含检查全部通过；合计 538 项，52 项既有环境条件跳过，
  0 failure、0 error。`git diff --check` 通过。
- 交付：runtime JAR 与 Prism 客户端目标的 SHA-256 均为
  `5cd41fbfab168dafbb4d3235823c54d7daa6ee335d712e1c4b586e8ef5311b95`；仅部署客户端且未启动，
  未部署 Lobby、S1、S2。正式资产中心和终端业务页面仍未迁移，需先完成人工 F8 UI Lab 验收。

### 2026-09-05 - 补齐 Galaxy UI 2 Batch B 的 Lobby 同版部署

- 现象：Batch B 首轮按计划只部署客户端后，Forge 握手检测到客户端 `3ca9376-main+3ca937653a-dirty` 与 Lobby 旧版 `073a5a6-main+073a5a622c-dirty` 不一致并以 Mod rejection 拒绝连接。
- 处理：使用既有灰度脚本将已经通过全量测试的同一运行 JAR 补充部署到 Lobby；客户端与 Lobby SHA-256 均为 `75cd26ff04d71e13d6410a4fb69a3b13b6550521c25457f806ff4e27c61ac05a`。
- 验证：Lobby supervisor 为 `RUNNING`，`latest.log` 已出现 `Done (1.609s)`，本机 `127.0.0.1:25566` 正常监听；JGB 的银行、市场、地产 SHADOW、Warehouse、Cluster 与 ServerTools 启动日志均正常。本次未触碰 S1、S2，未启动客户端。

### 2026-09-05 - 完成 Galaxy UI 2 Batch B Minecraft 双宿主 UI Lab

- 模块：新增纯 Java 8 `ui2-lab`，把概览、控件、数据、浮层和库存五页场景、Store 与假数据从 Java2D 后端抽离；`ui2-demo` 和 Minecraft 适配共同使用同一组件文档与 `UiRuntime`。
- 核心：补齐重建/对账/测量/布局/输入/DrawList/失效/卸载 Runtime，增加平台无关按键、文本度量、资源解析、渲染器、层级、渐变、阴影、图片和稳定外部区域合同。
- Minecraft：新增逻辑坐标 OpenGL renderer、scissor 换算、GL 状态保护、普通 `ModernScreenHost`、原生 `ModernContainerHost` 和 `NativeSlotBridge`。Container Lab 使用本地假库存并拦截窗口点击，不向服务端发送未知窗口操作。
- 字体：固定并内置 Noto Sans CJK SC 2.004（OFL 1.1），灰度字形可后台生成，纹理仅在渲染线程上传；Atlas 限 8 页、每页 1024x1024，支持代际取消、LRU 淘汰、资源重载和原版字体逐字形回退。
- 入口：注册客户端 `F8` UI2 Lab；`D` 切换调试层、`M` 切换 reduced-motion，Inventory 页用 `C` 进入双宿主 Slot 实验。没有接正式终端路由、业务网络或数据库。
- 验证：`:ui2-core:test`、`:ui2-lab:test`、`:ui2-demo:test`、`:ui2-demo:renderGolden`、根项目全量 `test` 与 `assemble` 通过，共 527 项测试、0 failure、0 error（52 项按既有条件跳过）。运行 JAR 内含 UI2 Core、Lab、Minecraft Adapter、固定字体和 OFL，Core/Lab 均为 Java 8 class 52；未包含 `ui2-demo`、Swing 或 Java2D Demo class。旧 Canvas 和资产中心保持不变。
- 交付：仅将同一运行 JAR 部署到 Prism 客户端 Mods，源文件与目标文件 SHA-256 均为 `75cd26ff04d71e13d6410a4fb69a3b13b6550521c25457f806ff4e27c61ac05a`；未启动客户端，未部署或重启 Lobby、S1、S2。

### 2026-09-05 - 完成 Galaxy UI 2 Batch A 独立内核与 Java2D UI Lab

- 构建：新增 `ui2-core`、`ui2-demo` Gradle 子项目。内核以标准 Java 8（class 52）独立编译，不依赖 Minecraft、Forge、LWJGL、AE2、ModularUI、BetterQuesting 或旧 Canvas；根项目编译使用 project dependency，正式 shadow/reobf JAR 通过内核 JAR 文件合入 49 个 UI2 class，Demo class 保持为 0。
- 内核：实现不可变 `UiElement`、稳定 `UiKey`、`UiNode`/reconciler、Component 工厂、Store/Reducer/Selector/Effect、约束几何、Row/Column/Stack/Grid/Scroll 布局、捕获/目标/冒泡输入、焦点、Pointer Capture、Modal、Theme Token、固定 Motion Clock 与平台无关 DrawList。
- Demo：Java2D UI Lab 提供概览、控件、数据、浮层、库存五页，支持 `350x193`、`620x340`、调试层和 reduced-motion；无头任务已生成 10 张 PNG 和 10 份结构化 DrawList，输出位于 `ui2-demo/build/ui-lab`。
- 验证：`:ui2-core:test`、`:ui2-demo:test`、`:ui2-demo:renderGolden`、根项目全量 `test` 与 `assemble` 均通过，`git diff --check` 通过；纯内核禁止依赖扫描通过。当前 Canvas、正式终端路由、市场、资产、地产和服务端合同均未修改，也未部署任何服务器或客户端。
- 构建经验：GTNH RFG 的 `shadowImplementation` 不能直接消费没有 MCP/deobfuscator attributes 的普通 Java project variant；最终采用 `implementation(project)` 加 `shadowImplementation(files(ui2CoreJar))`，既保持开发期类型依赖，又由正式发布链内嵌核心。

### 2026-09-05 - 固化 Galaxy UI 2 独立构建与 Demo 重构规格

- 主题：将“参考 Vue 逐步重建 UI 框架”收口为可执行规格，并明确与 Vue Web Runtime、当前 BetterQuesting 风格 Canvas 框架的差异。
- 构建决定：优先采用纯 Java 8 `ui2-core`、Java2D `ui2-demo` 和 Minecraft/Forge adapter 三层结构；核心必须能够独立编译和执行 JUnit，Demo 必须能够独立运行并导出两档尺寸 PNG/DrawList golden，根 Mod 只负责合入核心 class 和接平台能力。
- 测试边界：组件、Store、布局、输入、焦点、模态、Theme、DrawList 和 Motion 自动化；OpenGL 状态、字形 atlas、ItemRenderer、原生 Container/Slot 和实际观感保留游戏内 UI Lab 与资产中心人工验收。
- 迁移顺序：先做构建探针和纯内核，再做独立 Demo、Minecraft 双宿主、资产中心垂直迁移、普通终端页迁移，最后用 CodeGraph 确认零调用并删除旧框架；禁止未建立新基线前直接铲除现有 Canvas。

### 2026-09-05 - 固定 Vue 官方手册并建立 Galaxy UI 2 概念映射

- 主题：通过 GitHub SSH 拉取 Vue 官方英文文档与官方中文翻译的固定版本，建立 Vue 到 Java/Forge UI2 的工程映射。
- 来源：`vuejs/docs@b75d188ab16bf83bd1f364a77dfd2315be8f3fa4`、`vuejs-translations/docs-zh-cn@638fe472ad67f36399e7b73400c3a19ba6a461a2`；仅保留 Guide、API 与 Tutorial，文档许可为 CC BY 4.0。
- 结果：本地参考位于受忽略的 `Reference/VueDocs`，不会成为 Gradle 依赖或打入 JAR；新增 `galaxy-ui-2-vue-reference-map.md`，明确采用 Component/Props/Action/Slot/Store/Selector/Lifecycle/Key 等公开思想，自主实现 Java UI2，不移植 DOM、CSS、JavaScript Proxy、Vue Runtime 或模板编译器。
- 现状追溯：当前 Base GUI 由早期 ModularUI 2 迁移而来，现框架参考 BetterQuesting 的 Canvas/Panel/Theme 结构并基于 Minecraft `GuiScreen`/`GuiContainer` 自研；Forge 提供宿主与底层绘制能力，不提供当前的高级 Canvas 组件框架。

### 2026-09-01 - 银河仓储 v2：终端托管真实 Cell Bay 与统一资产中心

- 将后续主体验收收口为“终端提供一个 Bay，而非要求玩家摆放实体 Drive”：每位玩家在银河仓储页打开一格原版
  `Container`，可从背包拖放或 Shift 插入/取出一枚真实 AE2 Storage Cell。Cell 的完整 NBT 由 PostgreSQL
  保存，容量、物品/类型限制继续由 AE2 Cell 自身解释；不构造第二套物品账本。
- 新增 `warehouse_terminal_bay` 与 `warehouse_terminal_bay_operation` migration、JDBC 仓储和共享 JDBC schema
  校验。写入按本服/服务端 UUID/requestId/语义键/版本串行化，Cell 只能是一枚经 AE2 注册表认可的 Storage Cell；
  重放返回原回执，语义冲突被拒绝并保留前后快照审计。
- 银河终端改为统一资产中心：Base Vault、一个 Cell Bay 与最近操作在同一页展示；打开 Base Vault 或 Bay 是两个
  明确入口。页面不再把 Bay 描述成供电、频道或已接入外部 ME 网络，亦不列举或复制 Cell 内物品。
- 实体 `Warehouse Drive`、其已应用 migration 和方块 ID 保留为兼容内容，不新增配方、不迁移真实 Cell；生产库尚无
  Drive 登记。灰度已于 2026-09-02 应用纯新增 `20260901_001_add_terminal_warehouse_bay.sql`，同一 JAR 已复制到
  Lobby 与 Prism 客户端（客户端未启动）；Lobby 重启后完成 `Done`，共享 JDBC schema 校验通过，Bay 与操作审计表初始均为
  0 行。S1、S2 与 ServerUtilities 未触碰，旧 JAR 已备份到对应 deploy backup 目录。
- 自动验证：Docker 定向单测、终端页面路由测试和隔离 PostgreSQL schema 集成测试通过；覆盖 Bay 空槽、非 AE2
  物品拒绝、单格规范化、版本冲突、requestId 幂等/语义冲突、Drive/Bay 表隔离和 schema fail-fast。

### 2026-08-31 - AE2 银河仓储 v1：个人 Warehouse Drive、产权审计与终端状态

- 将原有未注册的 `WarehouseDriveTile` 尖峰升级为个人实体仓入口：注册 AE2 Drive 风格方块、Tile 和单一真实
  Cell Bay；普通右键不打开原生十格 Drive GUI，终端也不构造第二个 ME Terminal。
- 新增 `warehouse_drive` 与 `warehouse_drive_operation` migration。它们只保存 Drive 的位置、个人所有权、版本和
  服务端操作审计；所有 Cell 内容、容量、频道、供电与 Storage Grid 真相仍在 AE2。Base Vault 未实现
  `ICellContainer`，未接入 AE 网络。
- 放置、Cell 安装、空 Cell 取出和拆除都由共享 PostgreSQL 事务中的 UUID/本服/坐标/所有权检查授权；假玩家、
  非所有者、非空 Cell 拆除和爆炸不能绕过。侧向自动化不再暴露 Cell 槽。启用 `warehouseEnabled=true` 时缺 AE2、
  共享 JDBC 或 schema 会 fail-fast。
- 新增顶层“银河仓储”终端页，只读显示本人已登记 Drive、已加载状态、Cell、频道/供电、物品/类型摘要与近期审计；
  不主动加载区块、不列举 ME 物品、不跨服访问。
- 自动验证：新增 `WarehouseDriveServiceTest`、`WarehousePostgresIntegrationTest`，并回归单 Bay 与终端路由测试；
  Docker Gradle `compileJava`、`testClasses`、定向单测及隔离 PostgreSQL schema 集成测试通过。Warehouse Port、
  市场自动投递、组织仓和跨服 AE 网络均明确留在后续批次。
- 灰度：已应用 `20260831_003_add_ae2_warehouse_drive.sql`，构建产物、Lobby 与 Prism 客户端 JAR 的
  SHA-256 均为 `007cb3274b832f54f1633bc9c0659ead5e859204945bf87af30311e5bed0fbb0`。Lobby 先以
  `warehouseEnabled=false` 成功启动，再切换为 `true`；日志确认个人 Warehouse Drive runtime 已使用
  `galaxy_gtnh_lobby` 的共享 JDBC 就绪。客户端未启动，未创建 Drive，S1、S2 与 ServerUtilities 未触碰。

### 2026-08-31 - 系统 Warp 与服务器目录：受审计维护入口

- 新增 `/jgbst admin setwarp|delwarp|warp-enabled|server-enabled`。裸 `/setwarp` 与 `/delwarp` 暂不注册，
  避免与仍保留的 ServerUtilities 冲突；普通玩家仍仅能浏览和使用系统 Warp。
- 管理入口要求原版命令等级 2，维护坐标和服务器身份只从服务端在线操作者注入。Warp 仅能在维护者当前服设置，
  跨服目标需在目标服设置；不能把客户端坐标或远程服位置写入数据库。
- 新增 migration `20260831_001_add_servertools_admin_audit.sql` 和 `servertools_admin_operation`：Warp 写入、
  删除、启停以及目录服务器启停与操作者/前后快照审计行在同一 PostgreSQL 事务中提交。审计 schema 缺失只会拒绝
  管理动作，不破坏既有玩家传送链。
- 自动验证：新增 `ServerToolsAdminServiceTest`，并扩展隔离 PostgreSQL `ServerToolsPostgresIntegrationTest`
  覆盖审计行、Warp 启停和服务器目录启停；定向 Docker Gradle 回归通过。

### 2026-08-31 - 终端通知中心页面 v1：筛选、定位与有界玩家快照

- 影响范围：`TerminalPlayerNotificationCenter`、`TerminalNotificationFeed`、终端页面快照/网络编解码、
  `TerminalNotificationCenterSection` 与对应定向测试。
- 新增顶层 `通知 / 消息中心`；市场恢复、跨服传送、地产 `SHADOW` 与银行等来源继续各自拥有业务真源，
  通知中心仅保存当前玩家的 40 条有界展示索引，不建立第二套订单、票据、产权或账本。
- 同一来源/目标/内容的通知合并为出现次数；普通 INFO 刷新不进入历史。全量通知页快照不会被 HUD
  当作新的 Toast 重播，Toast 仍只读取本次动作反馈。
- 页面支持来源和严重等级循环筛选、固定 6 条分页，以及点击记录复用既有受控页面定位；不发送玩家、
  订单所有者、坐标或客户端筛选状态给服务端。
- 验证：Docker Gradle 定向 `TerminalNotificationFeedTest`、`TerminalPlayerNotificationCenterTest`、
  `TerminalNotificationCenterStateTest`、`TerminalNotificationPacketTest`、`TerminalServiceTest` 通过。

### 2026-08-31 - ServerTools 终端 TPA 页面 v1

- `群组服传送` 页新增服务端快照驱动的 TPA 表单和近期收发件箱：显式目标服务器发起、收到请求接受/拒绝、
  已发待确认请求取消，以及 `PENDING / ACCEPTED / DECLINED / CANCELLED / EXPIRED` 状态展示。
- 新增版本化五段 `TerminalServerToolsActionPayload`，同时兼容旧 Warp、快捷动作和 Home payload。TPA payload
  只包含另一位玩家和目标服；服务端继续从 `EntityPlayerMP` 与本服配置注入身份，所有动作都复用既有
  `PlayerTeleportService`、PostgreSQL 条件状态更新和接受后源服 ticket 派发链。
- 快照与网络模型只传当前玩家自身的请求方向、对手名、目标服和状态；不向客户端发送 UUID、坐标、
  接受者落点或其他玩家的 TPA。写动作均经过终端精确确认框，最终由服务端重验。
- 自动验证：Docker Gradle `compileJava compileTestJava`、`TerminalServiceTest` 与
  `TerminalServerToolsSectionPacketTest` 通过；CodeGraph 同步后复核终端 section、壳、网络与 runtime bridge，
  Docker Gradle 全量 `test`、`assemble` 和 `git diff --check` 均通过。
- 灰度：同一 runtime JAR SHA-256 `13614be469755ab7420b930a9d1f79be456103075c1389f4d7b665b73b4c5b26`
  已同步到构建产物、Lobby 与 Prism 客户端；Lobby 本次启动到达 `Done (1.153s)!`。客户端未启动，
  S1、S2、ServerUtilities 和 PostgreSQL schema 均未修改。

### 2026-08-31 - 市场恢复扫描与 PostgreSQL 回归审计收口

- 严格只读市场审计定位到 4 条 8 月旧 `BUY_ORDER_CREATE` 恢复记录：关联订单、正式冻结银行流水和返还流水均已不存在，玩家账户冻结额为 `0`；旧版本曾错误地把同账户解冻当作账户间转账，之后每次启动又因“冻结余额不足”在第一条记录中止整批恢复扫描。
- `MarketRecoveryService` 现在仅在存在关联订单或正式冻结流水时执行资金返还；若两者都不存在则写入明确的
  `NO_PERSISTED_FREEZE_OR_ORDER` 审计结论并安全收口，不会从玩家账户的聚合冻结余额中猜测或释放其他订单的资金。
  已存在正式返还流水的重复扫描仍只重放原结论，不会二次返还。
- 修正 `MarketPostgresIntegrationTest` 的隔离 JVM 夹具：定制市场快照不再依赖未启动 Forge 时没有 ItemBlock 的
  `Blocks.stone`，改用本地 `Item` 夹具，实际 PostgreSQL 的发布、购买、领取路径重新可执行。
- 验证：CodeGraph 复核恢复链及其受影响测试；`StandardizedSpotMarketServiceTest`、Docker Gradle 全量
  `test`、`assemble`、`git diff --check` 通过；真实 PostgreSQL 隔离 schema 的银行、市场、地产、物品策略、
  ServerTools 五组集成测试通过（不写生产业务数据）。部署后 Lobby 启动扫描已收口 4 条旧记录，
  `scripts/market-audit.sh --strict` 返回 `anomaly_count=0`。
- 灰度：runtime JAR SHA-256 `20a0f5045c2f1eafc18162dc91e628bed19e5c08a7676d6d26edee1fa490df1f`
  已同步到构建产物、Lobby 与 Prism 客户端，Lobby 到达 `Done (1.155s)!`；客户端未启动，S1/S2 和
  ServerUtilities 未触碰。

### 2026-08-30 - 定制市场与汇率市场规则收口

- 复核并固化定制市场的独立 `listing / snapshot / trade / audit` 链：发布、浏览、购买、取消、待领取、
  Base Vault 投递、未知交付锁定和管理员受审计恢复均不复用标准商品订单簿；单元与 PostgreSQL 回归覆盖
  发布、成交、领取、投递失败、完成写入失败、手工恢复及 requestId 语义冲突。
- 汇率市场将规则版本升级为 `exchange-taskcoin-v1-stack64`：任务书硬币报价与执行均由服务端限制为
  单个 Base Vault 物品栈（64 枚），超限返回结构化 `EXCHANGE_INPUT_QUANTITY_LIMIT`；终端提交改为实际按所选
  Vault 格完整数量结算，修复报价按整叠但旧实现只扣一枚的不一致。
- `ExchangeMarketService` 以本服配置身份产生报价与账本，执行拒绝外部来源服伪造；银行兑换审计新增最大输入量，
  储备不足不写半条事务，重复 requestId 重放原交易而不再次扣减储备。新增单元与 PostgreSQL 集成回归覆盖这些边界。
- 本批不引入市场化汇率、手续费、日/周期额度、自动补储备或第二套兑换账本；固定规则的后续经济调整必须以新规则版本
  进入独立评审。
- 验证：CodeGraph 复核 `CustomMarketService`、`ExchangeMarketService`、终端执行入口与受影响测试；定向服务回归、
  实际 PostgreSQL 隔离 schema 的银行/市场集成测试、Docker Gradle 全量 `test`、`assemble` 与 `git diff --check`
  均通过。集成测试还发现并修复规则键超过既有 `VARCHAR(32)` 的回滚问题；测试不会写入生产业务 schema。
- 灰度：同一 JAR（SHA-256 `f485ee269b878dff30b70d322ae206bab8a1fc7d218f5630ab595df823edbf69`）已部署 Lobby
  与 Prism 客户端，三处哈希一致；无新增 migration。Lobby 到达 `Done (1.209s)!`，客户端未启动，S1/S2 与
  ServerUtilities 未触碰。

### 2026-08-30 - AE2 Warehouse Phase 0：单 Cell Bay 技术尖峰

- 新增仅供技术验证的 `WarehouseDriveTile` 和 `Ae2WarehouseDriveProbe`：直接复用 GTNH AE2 的真实 Cell、
  频道、供电、容量与 `Actionable.SIMULATE` 语义，限制为一个 Cell Bay，不复制 Cell 内容进入 JGB 数据库。
- 新增纯 Java 单 Bay 状态机与定向测试，覆盖非法/重复插拔、非空 Cell 拆卸拒绝、无频道或无供电拒绝、部分容量与
  模拟操作不改变已存数量。
- 该尖峰没有注册 Block 或 GUI，也不接市场、Base Vault、跨服 ME、账户归属、组织权限或 Warehouse Port；防止
  未完成拆装权限和审计前向玩家暴露半成品实体仓。
- AE2 使用 `compileOnly` 依赖且不被打包；新增 LGPL 来源说明与随 JAR 打包的许可证。Docker Gradle 全量
  `test`、`assemble`、AE2 未打包断言与 `git diff --check` 均通过。
- 灰度：同一 JAR（SHA-256 `e7f79a2a3c9d358eb08ac73b6834e4651dae6ec18b04edbfb155bf41b504f035`）已部署 Lobby
  与 Prism 客户端，三处哈希一致；Lobby 到达 `Done (1.163s)!`，客户端未启动，S1/S2、市场交付和现有
  ServerUtilities 未触碰。

### 2026-08-30 - BanItem 物品准入策略核心

- 新增服务端纯策略模块：以真实 `ItemStack` 注册名和 metadata 匹配严格配置的 deny rule，范围覆盖标准市场
  托管、定制挂牌托管、Base Vault 存入，并为地产自动化预留范围；默认关闭且不附带任何禁用表。
- 标准市场与定制挂牌均在从 Vault 取物前判定；Base Vault 原生容器仅在新增/增加物品提交前判定。拒绝会保留
  原库存、写入 `item_policy_audit`，不会扫描、删除或改写既有物品。
- 新增 `20260830_001_add_item_policy_audit.sql`，启用时要求 InstitutionCore 的共享 PostgreSQL 连接、完整表和
  索引，否则 fail-fast，禁止未审计的静默降级。
- 自动验证覆盖规则解析、范围、metadata、配置错误、先审计后拒绝、Vault/终端配置回归；PostgreSQL 集成测试
  覆盖缺表 fail-fast 与审计落库（仅在注入 `JGB_BANKING_IT_*` 时执行）。Docker Gradle 全量 `test` 为 437 项、
  0 失败、43 项按环境跳过；`assemble` 和 `git diff --check` 均通过。
- 灰度：`20260830_001_add_item_policy_audit.sql` 已应用；同一 JAR（SHA-256
  `004113cadd53db5a1483fe84d1b51442629ec372439f09d9481c83625cf6a02e`）已部署 Lobby 与 Prism 客户端，三处
  哈希一致。Lobby 到达 `Done (1.229s)!`，日志确认策略为默认 disabled；客户端未启动，S1/S2 和
  ServerUtilities 未触碰。

### 2026-08-30 - 终端统一通知中心：结构化来源与跳转合同

- 将终端通知从“标题/正文/级别”扩展为结构化来源、目标页面和目标记录；市场恢复、跨服 ticket 与地产
  `SHADOW` 可分别标识为 `market-recovery`、`transfer-ticket`、`land-shadow`，客户端点击优先按合同跳转，
  旧通知仍保留文本推断回退。
- 新增纯 Java、有界 `TerminalNotificationFeed`：同键合并计数、最新优先、每页最多 12 条、总量最多 40 条；
  供后续终端通知页和服务端事件来源复用，不创建第二套聊天或资产状态系统。
- 地产 `SHADOW` 对真实玩家的拒绝判断会记录最新观察事项，继续不取消事件；终端地产页可显示并定位该观察记录。
- 自动验证覆盖通知网络 round-trip、来源/目标保真、分页/限频语义及 SHADOW 观察记录；Docker Gradle 全量
  `test`、`assemble` 与 `git diff --check` 均通过。该能力随本次后续 JAR 继续部署于 Lobby 与客户端，客户端
  未启动，Lobby 已到达 `Done`。

### 2026-08-30 - ServerTools 终端 1.1：命名 Home 管理与跨服详情

- 群组服传送页新增当前玩家的命名 Home 列表、目标服/维度/坐标详情、名称输入、设定、删除和前往动作；
  所有写操作与传送均经过确认弹窗。
- `TerminalServerToolsActionPayload` 保持一段 Warp、两段 Warp/快捷动作兼容解码，第三段追加 Home 名称；
  终端快照和两套网络包同步传递 Home 列表及选中详情。
- 服务端始终从在线玩家构造 `TeleportActor`；设定/删除复用 `PlayerTeleportService`，前往复用
  `prepareHomeTeleport -> Cluster ticket -> 目标服恢复`。不新增 Home 表、坐标客户端信任或另一套传送系统。
- 自动验证：覆盖旧 payload 兼容、原始网络 `ByteBuf` round-trip、Home 跨服计划派发、设定/删除运行时桥接和
  终端滚动布局；Docker Gradle 全量 `test`、`assemble` 与 `git diff --check` 均通过。
- 灰度：同一 runtime JAR（SHA-256 `1f27da49d27112f34cdd73e077acd92b6a52aa11bcf1d50e5f3a080b35f41240`）已部署到 Lobby
  与 Prism 客户端，三处哈希一致；客户端未启动。Lobby 已到达 `Done (1.530s)!`，S1/S2 未触碰。

### 2026-08-28 - ServerTools 终端快捷传送 1.0

- 复核后确认终端已有“服务器目录 + Warp + 最近 ticket”页面，本轮不重复创建页面；在既有右侧工作区
  增加“回家 / 返回 / 出生点”快捷操作，并保留 Warp 选择与精确确认弹窗。
- 快捷操作经版本化 `TerminalServerToolsActionPayload` 进入服务端，分别复用默认 `home`、`back`、
  `spawn` 的既有 `PlayerTeleportService -> ClusterTeleportService -> ticket/Connect -> 目标服恢复` 主链，
  没有新增客户端身份、数据库表或第二套传送实现。旧单段 Warp payload 继续兼容解码。
- 本版明确不在终端伪造家园名称输入、家园列表删除或 RTP 安全选点；它们保留命令入口，后续应作为
  ServerTools 页面 1.1 的独立交互批次，而不是把随机落点逻辑复制进 UI。
- 验证与部署：CodeGraph 复核 `TerminalService`、payload、终端壳与 ServerTools runtime bridge 的影响链；
  定向终端测试、全量 `test`、`assemble` 与 `git diff --check` 通过。JAR SHA-256 为
  `b79ff0648c24206e951b03b90aa1339d0094c6af5ecdb8708afa6e8b84d81a62`，已部署 Lobby 与客户端，
  客户端未启动；Lobby 到达 `Done (1.345s)!`。S1、S2 与数据库未修改。

### 2026-08-28 - 无真人跨服 TPA 传送模拟回归

- 将 ServerTools 已接受 TPA 的“源服扫描、计划构造、进程内重复抑制”抽为纯 Java
  `AcceptedTpaSourceDispatcher`；Forge runtime controller 仅提供真实在线连接并调用该调度器，
  没有向生产环境加入假人、机器人或第二套传送链。
- 新增无真人端到端模拟：保留真实 TPA 服务、Cluster ticket 服务与目标服恢复服务，以受控 gateway 和
  本服传送执行器替代网络与客户端，验证目标接受落点、源服派发、确定性 requestId、重复 tick、重启后
  ticket 幂等、目标恢复完成、源服隔离与 30 秒超时边界。
- 该回归不能替代在线模式下的 Velocity/Bungee 实际切服、Forge 重连和真实玩家落点验收；这些仍等待
  S2 的既有世界文件启动问题解决后再由真人账号执行。
- 验证与部署：CodeGraph `impact/affected` 复核调度器、Forge 控制器和模拟链；定向模拟回归、全量
  `test`、`assemble` 与 `git diff --check` 通过。JAR SHA-256 为
  `2fe8ae4b4c58a3682b6f30b0e2786fd06b2937289a736689cee2650e3cf1dd2c`，已部署到 Lobby 与客户端，
  客户端未启动；Lobby 到达 `Done (1.252s)!` 且 TPA runtime controller 已注册。S1、S2 和数据库均未改动。

### 2026-08-28 - ServerTools 跨服 TPA 闭环与 SU 兼容命令

- 新增 `/sethome`、`/delhome`、`/tpaccept`、`/tpdeny`；保留既有 `/home set/delete` 与
  `/tpa accept/deny`，新增 `/tpa cancel` 和 `/tpa status`。
- `player_tpa_request` 增加接受者落点与 `DECLINED/CANCELLED` 状态；新增 migration 以条件更新保证
  接受、拒绝、取消竞争时仅一个状态迁移成功，并增加目标提示、源服派发与近期查询索引。
- 新增 ServerTools tick/login 控制器：目标服只记录接受者落点，源服在请求者在线、仍在原服且未超时
  时以确定性 requestId 派发现有 cluster ticket，修复跨服接受时目标服没有请求者连接的问题。
- ServerTools 及 TPA 玩家反馈改为中英文翻译键；PostgreSQL 真源、代理 Connect、目标服落点恢复链
  保持不变，未引入 Redis、Presence 或第二套传送系统。
- 自动验证：CodeGraph 复核服务、模块、JDBC 仓储和测试影响链；新增兼容命令、状态机、超时、并发、
  migration 升级、schema fail-fast 与源服隔离回归；Docker Gradle `test`、`assemble` 与
  `git diff --check` 均通过。
- 灰度：已将同一 JAR（SHA-256 `2c3e283dc49009f55810f4ea1211e9b47aa4c1b7037f82ab7be99ece5f8be786`）
  部署至 Lobby、S2 与客户端（未启动客户端），并成功应用
  `20260828_001_expand_tpa_request_lifecycle.sql`。Lobby 正常启动且 TPA runtime controller 已注册；
  S2 因既有 `World/level.dat`/`level.dat_old` 的 ZLIB 损坏退出，未对该世界文件作任何处理。

### 2026-08-28 - ServerUtilities 跨服指令架构与开源实现复核

- 使用 CodeGraph 复核 `PlayerTeleportService -> ClusterTeleportService -> BungeeCordGatewayAdapter
  -> PlayerArrivalRestoreService`：现有 PostgreSQL 数据、幂等 ticket、代理切服和目标服落点恢复已构成
  可恢复的跨服主链，不需要重写第二套传送系统。
- 对照 ServerUtilities 源码确认应吸收的是 `sethome/delhome/tpaccept/tpdeny` 等玩家入口和
  warmup/cooldown 体验，不引入其单服 Universe、NBT 数据或旧网络宿主。
- 对照 HuskHomes、Velocity/BungeeCord 官方文档和 Spigot 社区实现：共享 SQL 与实时消息 broker
  应分层；插件消息受在线玩家载体限制，Redis 适合后续低延迟 Presence/通知但不是当前前置条件。
- 将下一实施批收敛为兼容命令、TPA 拒绝/取消/查询/过期、目标服数据库轮询提示、本地化和状态并发
  测试；自动找服、Redis/代理消息总线、全局 spawn/RTP、warmup/cooldown、收费与 death-back
  进入后续优化池。
- 本轮仅更新已有设计与命令文档，没有修改 Java、数据库、配置或部署产物；未启动客户端，未触碰
  Lobby、S1、S2 和 ServerUtilities JAR。

### 2026-08-27 - 个人地产 C.2 高 GUI Scale 视觉比例修正

- 根据 1680x840 实机截图反推约 350x193 逻辑工作区，修正原先固定最小尺寸在 GUI Scale 4 下
  被放大的问题：顶部业务区压缩到 32px，地图恢复到至少 74% 内容宽度，检查器收敛到约 24%。
- 地图工具栏由 22px 缩为 13px，底部状态条同步压缩；悬浮卡改用地图相对宽度，不再使用
  170px 硬下限。
- 玩家皮肤定位针取消 34px 最小值，改为随区块尺寸计算且不覆盖多个区块；头像、针体和朝向箭头
  保持同一缩放关系。
- 检查器拆分所有者、产权、保护、地形与四类市场占位信息，修复中文界面显示字面量 `\n`。
- 本轮未改地产网络合同、PostgreSQL、产权数据、认领安全规则和价格能力；Lobby 继续保持
  `SHADOW`，ServerUtilities 保留。
- 验证与部署：CodeGraph `query/impact/affected` 将回归范围收敛到地产 section、地图输入与布局测试；
  Docker Gradle 定向测试和完整 `test` 均通过，全量为 413 项、0 失败、37 项按环境跳过，
  `assemble`、语言文件换行检查和 `git diff --check` 通过。构建、Lobby 与客户端 JAR SHA-256
  均为 `a44faaf8661d8e2a3db3cf9f33205b2011ee7d3b5ce84a35ba225a76a1fa094e`。
- Lobby 本次启动到达 `Done (1.409s)!`，地产运行时为
  `SHADOW / activeTitles=1 / fakePlayersAllowed=false`；Lobby 与客户端的 ServerUtilities 2.2.2
  均保留。未启动客户端，未触碰 S1/S2。

### 2026-08-24 - 个人地产 Batch C.1 真实区块地图

- 用专用地图画布替换 225 个普通按钮：客户端只读取已加载区块并生成 16x16 地形瓦片，
  叠加本人/他人/保留区产权色、外边界、选择框和玩家方向标记；未知区块明确显示斜纹。
- 新增三级缩放、滚轮、拖拽平移、定位与刷新；视口最多偏离玩家 16 区块，远级最大 31x31。
  地形使用内存 LRU 和单后台采样线程，OpenGL 纹理只在渲染线程创建、上传和释放，不写磁盘。
- 地产网络合同改为受限视口与稀疏产权单元，兼容旧六段 action payload；越界认领返回
  `OUT_OF_RANGE`，不会错误认领脚下区块。认领范围仍是玩家实时位置周围 15x15。
- 来源与许可：地形采样参考 `Reference/ServerUtilities` 的 `ThreadReloadChunkSelector`，派生
  文件保留 LGPL-3.0-or-later SPDX；SU 仍是参考源码和灰度期并存 Mod，不成为 JGB 运行依赖。
- 自动化：Docker Gradle 全量 `test` 已通过，覆盖负坐标、视口限幅、缩放锚点、旧payload、
  稀疏产权、保留区、越界认领、网络往返、布局和地形明暗。人工地图观感与保护事件矩阵后置。
- 验证与部署：全量测试共 404 项、0 失败、37 项按环境跳过；另行注入现有测试数据库配置后，
  PostgreSQL 地产集成测试 5/5 通过。`assemble` 与 `git diff --check` 通过，最终 JAR SHA-256 为
  `6d23dd948b028352f581b50ee56324b34bc7fda177edd4f049f4fe8b4e94f76d`。
- 同一 JAR 已部署到 Lobby 与客户端，客户端未启动，S1/S2 未修改。Lobby 到达
  `Done (1.154s)!`，地产运行时为 `SHADOW / activeTitles=0 / fakePlayersAllowed=false`，Lobby 和
  客户端的 ServerUtilities 2.2.2 均保留。启动日志仍有本批无关的既有市场恢复警告
  `insufficient frozen balance`，不影响地产运行时就绪，但应在市场恢复账本任务中继续处理。

### 2026-08-23 - 个人地皮 Batch C 终端闭环与自动验证

- 新增独立顶层 `PROPERTY / 地产` 页面，首版提供“附近地皮 / 我的地皮”：附近页使用服务端当前玩家位置生成 15x15 网格，我的地皮使用服务端计数与分页，并显示选中产权、版本、配额和 SHADOW/ENFORCE 状态。
- 新增结构化地产动作载荷、快照、客户端模型和网络编解码；客户端不能提交玩家 UUID、本服 ID或维度，服务端从在线玩家和当前服运行时注入身份。任意远程空区块会被收敛回当前网格，只有已证实属于本人的产权可从“我的地皮”跨距离选中。
- 认领与放弃使用精确确认框、唯一 requestId 和 expectedVersion；服务端继续复用 Batch B 的归属、配额、禁用维度、保留区、幂等与版本校验，成功后返回最新保护快照。
- 新增中英文地产 UI 文案、负坐标/15x15 映射、越界选择、认领放弃、11 条/每页 4 条、载荷边界、网络往返和专用页面边界测试。
- 修复灰度部署脚本可能接受旧 `Done (` 日志的问题：每次重启创建时间标记，只接受本次重启后更新的日志；新增独立脚本回归。
- 人工 Forge 事件矩阵仍后置；Lobby 必须保持 `SHADOW`，ServerUtilities 继续保留，S1/S2 不在本批部署范围。
- 验证与部署：CodeGraph 查询/调用链/impact 确认影响集中在终端快照、网络编解码、页面模型、专用 section 和地皮服务；Docker Gradle 全量为 396 项、0 失败、37 项按环境跳过，5 项 PostgreSQL 地皮集成测试另行强制执行通过，`assemble`、`git diff --check`、语言键一致性与部署脚本回归通过。
- 构建、Lobby 与客户端 JAR SHA-256 均为 `bcb8136e7febc625900c426946e2ae86d5e0e956a21fd4c8db9b881c083854b0`；Lobby 本次到达 `Done (1.205s)!`，地产运行时为 `SHADOW / activeTitles=0 / fakePlayersAllowed=false`，数据库仍为 0 条产权和 0 条操作。客户端未启动，ServerUtilities 在 Lobby 和客户端均保留。

### 2026-08-22 - 个人地皮 Batch B PostgreSQL 与保护执行层

- 新增个人产权与操作日志 migration；产权按 server/dimension/chunk 唯一，放弃改为 `REVOKED + version++`，操作保存结构化前后快照。
- 新增共享 JDBC 仓储、事务执行器和 schema fail-fast；请求、玩家配额与区块使用 advisory lock，服务重启可从 PostgreSQL 重建本服保护快照，失败事务不会留下半条产权。
- 增加服务端 land 配置和严格解析；`LandModule` 不再创建内存正式运行时，只在 dedicated server 启用后复用 Institution Core 的共享连接。
- 增加 SHADOW/ENFORCE 保护层，覆盖破坏、放置、交互、非玩家实体攻击、爆炸方块和假玩家；PvP 不变，假玩家默认拒绝，日志与玩家提示限频。
- 本批没有命令、终端地产页、挂牌或交易。Lobby 已进入 SHADOW 并继续保留 ServerUtilities；启动日志确认 `activeTitles=0`、`fakePlayersAllowed=false`，数据库产权与操作表均为 0 行。
- 验证与部署：`git diff --check`、Docker Gradle 地皮定向测试、5 项真实 PostgreSQL 隔离 schema 测试、常规全量 `test` 与 `assemble` 通过；全量为 385 项、0 失败、37 项按环境跳过。强制开启全部 PostgreSQL 集成测试时另发现 1 个既有银行 missing-schema 错误文案断言不匹配，本批未改无关银行合同。
- migration `20260822_001_add_personal_land.sql` 已应用；最终构建、Lobby 与客户端 JAR SHA-256 均为 `659bd21cc5e744038837baaba29c841d0660da049519cebe2a6ff52025787494`。Lobby 到达 `Done (1.380s)!`，客户端未启动；S1/S2、旧 Team claim 均未修改。

### 2026-08-22 - 完成个人地皮 Batch A 代码尖峰

- 新增并注册 `LandModule`，落地跨服区块键、版本化个人产权、状态/结果枚举、结构化动作回执，以及个人认领和放弃服务。
- 认领支持 `requestId + semanticsKey` 幂等、单人上限、禁用维度和保留区块；放弃时重新校验所有者、版本和产权状态。新增可从仓储重建、以不可变快照发布的 `LandProtectionIndex`。
- 当前只使用内存仓储，默认上限 4 是测试占位；尚未接 PostgreSQL、Forge 保护事件、终端 PROPERTY 页面、银行地产交易或旧 SU 数据迁移，因此不能视为游戏内功能已经上线。
- 补齐 `THIRD_PARTY_NOTICES.md`、派生源码 SPDX/来源说明和发布资源打包，修正资源目录 MIT 许可证的占位版权。构建 JAR 已确认包含第三方声明与 ServerUtilities LGPL 全文。
- 验证：`git diff --check`、Docker Gradle 地皮定向测试和 `assemble` 通过。本轮未部署、未删除 ServerUtilities JAR，也未触碰 S1；既有市场弹窗工作区修改保持原样。

### 2026-08-22 - 个人地皮优先与 ServerUtilities 代码复用评估

- 主题：在确认可沿用 LGPL-3.0-or-later 后，评估 ServerUtilities claim 源码可复制范围，并把首轮范围收紧为个人认领、个人挂牌和个人购买。
- 源码结论：SU 全仓 592 个 Java 文件、约 54,091 行；claim 数据、命令、网络和旧 GUI 候选面约 2,317 行。区块坐标、claim 状态、内存索引、Forge chunk ticket、保护事件与地图算法有复用价值；TeamData、Universe、PermissionAPI、旧 wrapper、旧 GUI 和命令宿主不进入个人首版。
- 架构结论：只新增一个内部 `LandModule`，不把保护、产权、交易拆成多个 Mod；PostgreSQL 是产权/挂牌/审计真源，本服内存 map 是可重建保护索引。
- 交易与 UI：个人地产借鉴定制市场的 requestId、row lock、状态机、共享事务和审计，但无 ItemStack/待领取；终端新增 `PROPERTY` 页，使用“附近地皮 / 我的地皮 / 在售地产 / 交易记录”，MARKET 仅作为路由入口。
- 迁移边界：旧 S1/S2 claim 属于 team，组织关系未定前只进入 migration staging，不自动归个人；个人版可先在无旧 claim 的隔离环境或 Lobby 验证无 SU 独立运行，S1/S2 删除后置。
- 文档：新增 `serverutilities-personal-land-code-reuse-evaluation-v1.md`，同步修订地产总设计、映射表、产品方向与文档入口。本轮没有复制生产代码、改数据库、删除 JAR、部署或触碰 S1。

### 2026-08-22 - 修正 ServerUtilities 最终形态并补齐地产制度关系

- 主题：纠正“长期保留 ServerUtilities 保护适配器”的错误理解，明确把所需能力重构进 BaseMod，迁移验收后删除独立 ServerUtilities。
- 文档核对：复查项目定位、银行账户/账本、三类市场、定制挂牌、ServerTools 权限预留、终端职业占位及 ServerUtilities 映射；仓库文档引用的旧 `../../Docs/设定.md` 等外部策划文件当前实际路径不存在，因此未把这些历史引用当成现行事实。
- 设计结果：JGB 原生 PostgreSQL 产权与本服内存保护索引是唯一正式运行态；ServerUtilities 只用于冻结参考行为和一次性只读导出/导入，不得成为编译、启动、页面或保护判断依赖。
- 制度关系：Team 是组织、共同产权和 Team 银行账户的主体；职业是个人规则输入，不是 Team 岗位或产权主体；个人/Team 资金统一走银行账本；地产使用独立固定价挂牌和产权过户，不进入标准商品订单簿；BQ/职业后续通过事实和版本化规则接入。
- 最小路线：原生个人 claim → GTNH 保护矩阵 → 最小 Team → 迁移演练与灰度卸载 → 固定价地产交易；本轮不修改代码、数据库、服务器配置或任何 JAR，不触碰 S1。

### 2026-08-22 - 修正 ServerUtilities 集成状态并建立领地/地产路线

- 修正此前判断：JGB 只吸收了 `home/back/spawn/warp/tpa/rtp`、跨服 ticket 与终端传送页，并未完成 ServerUtilities 的 claim、chunk loading、team/rank、领地地图或其他工具集成。
- 实时只读检查确认 Lobby、S1、S2 与客户端均有 ServerUtilities 2.2.2，三服 claim/protection/chunk-loading 配置启用；S1 有 23 个、S2 有 16 个现存团队 claim 数据文件，Lobby 为 0。本轮不修改配置或 `.dat`，不触碰 S1。
- 新增 `serverutilities-land-property-integration-v1.md`：把领地保护、产权登记和地产交易分开。该条最初写成运行时只读 Adapter 路线，已由同日上方“修正 ServerUtilities 最终形态”记录废止；现行路线是 JGB 原生实现和一次性旧数据迁移。地产不进入标准商品订单簿，chunk loading 不随产权出售。
- 更新 ServerUtilities 映射、当前产品方向、AE2 仓储优先级和文档索引。AE2 方向保留但暂缓；BanItem 策略、BQ 任务事实、职业系统和连锁挖矿均记录为后续任务，其中 BQ/职业尚无权威制度合同，不做占位实现。
- 本轮仅更新设计文档，没有修改代码、数据库、服务器配置或部署产物。

### 2026-08-20 - 修复空对手盘被误报为余额不足

- 实机截图确认买入弹窗处于市价模式，账户余额为 `891385`，但盘口为“买一 61 / 最新 69 / 卖一 --”；根因不是银行余额，而是当前没有可立即成交的卖单。客户端此前将缺失的卖一价格解析为 0，再由最大可买量分支误报“银行可用余额不足”，并把预估总额错误显示为 0。
- 市价买入缺少卖盘时现在明确提示“卖盘为空，无法市价买入；可改用限价挂单”；市价卖出缺少买盘时给出对称提示。对手价不存在时预估总额显示 `--`，真实余额不足与库存不足校验保持不变；限价单仍可在无对手盘时正常挂入订单簿等待成交。
- 验证与部署：Docker Gradle 定向 `MarketOrderEntryPopupTest` 通过，新增买卖双向空对手盘回归；`assemble` 通过。构建产物、Lobby 与 Prism 客户端 JAR SHA-256 均为 `c953d7e4f47a4de2b7551e958582be0066cf57d4af4fe452794743285bbca040`。Lobby 已完成重启与部署脚本启动检查；未触碰 S1/S2，也未启动客户端。

### 2026-08-19 - 复盘并收口终端数字显示遗漏

- 复盘语义化数字规范的全部客户端落点，确认旧的独立 `K/M` 算法已经清除，服务端快照、网络结构和操作请求继续携带原始整数。
- 修复三类遗漏：订单与资产中心顶部银行/冻结/Vault 摘要及结构化订单、成交、交付行改为精确千分位；紧凑行情区的 24h 成交额与成交量统一复用三位有效数字规则；Vault 格位缩略图使用紧凑数量并在格位右下角缩放对齐，已选操作数量保留精确千分位。
- 修复结构化订单在极大数量下计算 `filled * 100 / original` 可能长整型溢出的边界，改用无溢出的整数比例计算；新增大额精确分组和 `Long.MAX_VALUE` 成交进度回归测试。
- 验证与部署：`git diff --check`、Docker Gradle 全量 `test` 与 `assemble` 通过；测试结果为 361 项、0 失败、0 错误、32 项按环境跳过。构建产物、Lobby、S2 与 Prism 客户端四处 SHA-256 均为 `6b504b524bb74c50a8e6642d2b664dbeb8939aeb4d852a582baf8bff2df6598f`。Lobby 到达 `Done (1.433s)!`；S2 已收到相同 JAR，但仍因既有 `World/level.dat` ZLIB 截断、`level.dat_old` EOF 及后续世界为空异常退出。本轮未触碰 S1，也没有启动或重启客户端；部署时客户端本来就在运行，需玩家正常重启后加载新 JAR。

### 2026-08-18 - 建立终端语义化数字显示规范并改造市场紧凑读数

- 实时库确认铁锭买单 #228 并非空数量：原始 2990、已成交 960、剩余 2030。客户端原始盘口行包含 `80 x2,030`，但三列盘口的通用标签在临界宽度按空格换行，只有价格可见，数量被放到不可见的第二行。
- 新增 `docs/terminal-number-display-standard-v1.md`，把数字显示拆成紧凑数量、精确分组值和精确操作值：盘口、图表坐标、最新成交窄列表使用三位有效数字的 `K -> M -> G -> T -> P -> E`；银行余额、冻结资金、价格、订单中心和确认流程保留精确千分位。
- 新增共享客户端数字格式器；盘口现在显示无空格的 `80x2.03K`，行情坐标和最新成交数量复用同一规则，市场深度向零截断以免夸大流动性。无空格形式消除窄列按空格换行的根因；盘口点击仍读取未经缩写的服务端原始行，因此打开订单时保持精确价格 80、数量 2030；行情悬停也改回精确成交量和成交额。
- 验证与部署：`git diff --check`、Docker Gradle 全量 `test` 与 `assemble` 通过；测试结果为 360 项、0 失败、0 错误、32 项按环境跳过。构建产物、Lobby、S2 与 Prism 客户端四处 SHA-256 均为 `e10dbcb0793c23007f9c54e7209c8cba2235ae873355d84ac8c30637b2c86732`。Lobby 到达 `Done (1.520s)!`；S2 已收到相同 JAR，但仍因既有 `World/level.dat` ZLIB 截断、`level.dat_old` EOF 及后续世界为空异常退出。本轮未触碰 S1，也没有启动或重启客户端；部署时客户端本来就在运行，需由玩家正常重启后加载新 JAR。

### 2026-08-18 - 修复测试买单手续费预留阻断真实卖单

- 实机日志确认卖价 20 的正常卖单在跳过玩家自己的 80 买单后，被测试做市买单 #58 阻断；根因是 `market-demo-fixture-v2/v3` 只冻结限价本金，没有预留买方成交手续费。失败事务已整体回滚，没有生成卖单、成交、银行划转或托管变化。
- 测试做市脚本现在按真实买单合同预留“剩余限价本金 + 最高 taker 手续费”，并已修复实时库 v2/v3 的全部活动测试买单；铁锭 #58 从 2331 调整为 2349，测试做市账户银行冻结总额同步为订单预留总额。加强后的审计显示 `active_buy_orders_under_reserved=0`、`buy_order_owners_without_bank_frozen_coverage=0`。
- 撮合增加最后防线：锁定的静止买单若仍不足额，则以幂等请求释放残余冻结、标为 `EXCEPTION`、记录 `ORDER_QUARANTINE`，并继续匹配下一张有效外部买单，不再让无过错卖家承担坏盘口状态。
- 手续费制度保持“成交后收费”：买单入簿时只冻结本金和最坏手续费容量，不产生税费流水；逐笔成交只收真实 maker/taker 费用，未成交数量、价格改善及未使用费用预留在成交完成或撤单时释放。终端确认和回执改称“资金预留”，明确预留不等于已经收费；卖方费用继续从真实成交收入中扣除。
- 新增挂单未收费、坏买单隔离后继续成交的回归测试，并扩展活动买单逐单预留和银行聚合冻结审计。本轮实时严格审计仍报告4条 8月12日旧自成交事故的 `RECOVERY_REQUIRED`，与本次手续费修复无关，未擅自执行玩家资产恢复。
- 验证与部署：`bash -n scripts/market-demo-fixture.sh scripts/market-audit.sh`、`git diff --check`、Docker Gradle 定向测试及全量 `test` 均通过；全量结果为 352 项、0 失败、0 错误、32 项按环境跳过。构建产物、Lobby、S2 与 Prism 客户端四处 SHA-256 均为 `2fc49e717b1357b70d5ec5c9e45da94c39bb62849781880c4a30dce49be001af`。Lobby 到达 `Done (1.437s)!`；S2 已收到相同 JAR，但仍因既有 `World/level.dat` ZLIB 截断、`level.dat_old` EOF 及其后 WR-CBE 世界为空异常退出。本轮未触碰 S1，也未启动客户端。

### 2026-08-18 - 记录交易所级能力缺口、结算制度与远期复杂产品边界

- 更新 `docs/modern-trading-terminal-redesign-v1.md`，把标准市场从撮合核心走向交易所级系统仍缺少的能力按正式路线收口：版本化规则、盘前风险网关、全局事件序列、独立对账、操纵监控、运行韧性、自动化接口准入，以及异常成交和申诉制度。
- 明确实施优先级：AE/机器订单开放前先完成限额与 Kill Switch、权威事件序列和独立对账，再推进监察、制度治理、争议处理、容灾演练及强制价格带/重开竞价。
- 记录当前标准市场不是 T+1：买卖双方在入簿前分别足额冻结资金和真实商品，成交事务立即完成资金、费用、订单、卖方托管扣减、成交记录和买方 `CLAIMABLE` 权利；随后自动投递 Base Vault，投递失败只进入待领取/恢复，不推翻已完成结算。
- 将做空、融资杠杆和期权明确排除在当前范围之外，同时作为远期研究项保留；禁止用负库存、负余额或未足额冻结订单进行简化伪实现，并记录未来重新评估各自所需的借贷、保证金、清算、行权和违约制度前提。本轮仅更新设计文档与工作日志，未修改代码或部署。

### 2026-08-18 - 记录标准市场自成交未绕过 BUG

- 新增 `docs/standardized-market-self-match-bug-2026-08-18.md`，记录批量测试中同一玩家提交交叉买卖单后被错误送入银行同账户冻结结算、最终显示“同步失败”的现场现象。
- 明确缺陷位于市场撮合候选选择阶段：市场层必须自动跳过同一账户的对手单，并继续按价格优先、时间优先寻找下一笔合格外部订单；银行层同账户转账拒绝继续保留为最终安全防线。
- 固定修复边界与八组验收场景，覆盖只有自有对手单、后续存在外部订单、部分成交、并发状态变化、账本与托管零副作用及终端反馈。本轮只更新 BUG 文档、文档索引和工作日志，未修改或部署代码。

### 2026-08-18 - 修复标准市场自成交候选未绕过

- `StandardizedSpotMarketService` 在候选订单锁定并重验后比较服务端订单所有者；候选属于主动单所有者时直接跳过，并继续按仓储层既有价格优先、时间优先顺序扫描后续外部订单。
- 不取消、不改价也不移动被跳过的自有订单；可交叉范围内只有自己的对手单时，新限价单与原订单均保持开放，不产生成交、税费、资产转移或同账户银行结算。
- 新增双向绕过及“只有自己的交叉单”三组回归测试。Docker Gradle 定向 `StandardizedSpotMarketServiceTest` 与完整 `test` 均通过；数据库结构、银行安全校验和客户端协议未改动。
- `git diff --check` 通过；构建产物、Lobby、S2 与 Prism 客户端四处 SHA-256 均为 `f0a32851567e2a1158136791eee96ae822474db1fc8a1f6ae446cc3c7cd9c29d`。Lobby 到达 `Done (1.384s)`；S2 已部署相同 JAR，但仍因既有 `level.dat` ZLIB 截断与 `level.dat_old` EOF 损坏退出。本轮未触碰 S1，也未启动客户端。

### 2026-08-18 - 记录标准市场扫盘与静止单定价实测规则

- 更新 `docs/modern-trading-terminal-redesign-v1.md`，把批量交易实测确认的连续订单簿语义固定为正式设计：买入限价是最高可接受价、卖出限价是最低可接受价，成交逐档使用订单簿中静止对手单的价格，而不是把全部成交强制写成主动单限价。
- 增加 `100 x100` 扫过 `90 x10 / 91 x20 / 95 x30` 的示例：前三档分别按 90、91、95 成交，剩余 `40` 才以买价 100 留在盘口；后来外部卖单可以按该静止买价成交，但责任仅限剩余数量与已冻结资金。
- 记录价格优先、同价时间优先、自成交自动绕过，以及与 SEC 限价定义和 Nasdaq 价格/时间优先、自成交防止、静止单价格成交示例的对应关系；同时明确项目不宣称实现 NBBO、跨市场路由、竞价、熔断、价格保护或完整美国证券法规。本轮仅更新设计与工作日志，未修改或部署代码。

### 2026-08-18 - 记录 7x24 标准市场竞价、熔断与自动化风控方向

- 更新 `docs/modern-trading-terminal-redesign-v1.md`，明确 7x24 市场继续以连续价格/时间优先订单簿为主，不照搬每日开收盘；集合竞价用于新商品、个品种熔断后、关键服务故障恢复、重大规则变化及长期缺少可信价格后的重开定价。
- 固定 `CONTINUOUS -> LIMITED -> PAUSED -> AUCTION -> CONTINUOUS` 个商品状态机：暂停不回滚已成交交易，始终允许撤销未成交余量；重开竞价公开预估清算价、配对量、失衡方向和倒计时，并按最大成交量、最小失衡、最接近可信参考价的顺序确定单一价格。
- 参考 LULD、NYSE MWCB、Nasdaq Halt Cross 与交易前风控，但不照抄依赖成熟指数和交易日收盘的 `7% / 13% / 20%` 参数。价格带先以影子模式记录，可信参考价需满足成交笔数、数量与金额门槛，防止一单位成交操纵熔断。
- 明确首版全市场暂停优先服务结算完整性：数据库、银行、Vault/AE 预留、恢复账本或行情新鲜度无法证明安全时，按最小影响范围进入降级或暂停，恢复后必要时通过集合竞价重开。
- 在 `docs/market-three-part-architecture.md` 增加市场保护与重开能力；未来 AE/API 必须经过统一认证订单合同、原子预留、幂等版本、心跳租约、速率/敞口限制、cancel-on-disconnect 与账户级 Kill Switch，并监控刷单、分层挂单、关联账户对敲及小额标记价格。本轮仅固化设计与阶段顺序，未修改或部署代码。

### 2026-08-15 - 市场快速撤单与订单资产中心产品修订

- 更新 `docs/market-action-receipt-and-personal-history-v1.md`：商品详情第三动作由“历史”修订为“撤单”，但只打开当前商品、当前玩家且仍有剩余量的活动委托弹窗，继续要求精确订单目标和服务端二次校验。
- 将既有个人历史页升级规划为市场与仓库共用的“订单与资产中心”，明确当前委托、成交记录、资产与交付、历史查询四个标签，并增加银行、Base Vault、活动委托和待交付的紧凑账户摘要。
- 历史成交退为辅助查询；高频买卖与快速撤单留在商品详情，跨商品订单和资产管理进入独立全页，泡泡回执负责单次动作反馈。本轮仅更新产品与交互合同，未修改运行代码。

### 2026-08-15 - 标准市场实机视觉验收纠偏

- 根据实机截图重新区分“功能完成”和“界面完成”：详情页账户区移除整句动作反馈，只保留行情同步状态与待入库数量，完整原因继续由通知和订单历史承载。
- 买卖弹窗重排为行情上下文、订单参数、结算预览和最终动作四个层级；数量比例、盘口价格快捷项与对应输入框就近排列，并保留服务端最终校验提示。
- 个人订单中心将四个整行大筛选器压缩为左对齐工具栏，右侧显示当前生效筛选摘要；搜索、重置和表格不再争夺同一层级。
- 本轮不修改撮合、银行、Base Vault、托管或订单协议，只收口三个已经在实机截图中确认层级过弱的客户端渲染路径。

### 2026-08-14 - 标准市场连续行情桶与零成交时段

- 新增 `docs/standardized-market-continuous-candles-v1.md`，锁定 1h/24h/7d 分别采用 12 个 5 分钟桶、24 个 1 小时桶和 28 个 6 小时桶；横轴始终按固定桶等距显示。
- 服务端行情聚合补齐 `TRADE / CARRY_FORWARD / REFERENCE / EMPTY` 来源。无成交时段沿用最近真实收盘价，或在从未成交时显示正式目录参考基线；成交量与成交额始终为 0，不生成假成交。
- 行情网络模型传递桶来源；客户端真实成交绘制红绿 K 线和成交量柱，无成交桶只延续价格并保留空的成交量位置，十字光标明确显示数据来源。
- 增加固定桶数量、时间间距、真实 OHLCV、前收延续、参考基线与完全空态测试。本轮不修改撮合、订单、银行、Vault、定制市场或汇率市场语义。
- 验证：`git diff --check` 与 Docker Gradle 全量测试通过，共 331 项完成、32 项跳过；Lobby、S2 与 Prism 客户端部署产物 SHA-256 均为 `bf8bde9ccf50fef9482ad2e6eb9c1ef654debca40717433cedf51bc62ef6dbf4`。
- Lobby 已到达 `Done (1.491s)!`。S2 仍因既有 `World/level.dat` 的 ZLIB 截断及 `level.dat_old` EOF 损坏而退出，本轮未擅自重建或回滚世界。

### 2026-08-13 - 标准市场红绿多形态行情图与十字读数

- 保持标准市场详情页现有左右比例和图表边界，不扩大行情区；行情按照真实数据密度自动降级：4 个及以上有效桶绘制红绿 OHLC K 线，2 至 3 个点绘制按涨跌分段着色的平滑价格线，单点绘制真实成交点，无成交才显示空态。
- 成交量柱与价格涨跌使用同一红绿语义，移除贯穿全图的单一蓝色走势；补齐价格、时间和成交量坐标，并增加仅在图表内部生效的十字光标。
- 十字光标浮层读取服务端真实时间、开高低收、成交量和成交额；浮层自动翻转并限制在图表边界内，不参与交易动作。
- 行情快照和网络模型扩展为完整 OHLCV 与成交额，服务端聚合不再在客户端退化为收盘价列表；旧三字段构造器继续保留给现有调用点。
- 本轮不修改撮合、银行、Base Vault、定制市场或汇率市场业务语义。

### 2026-08-13 - 个人订单历史分页容量与撤单链复核

- 修复订单历史工作台“可见 4 行、服务端却按 7 条分页”的合同错位。历史页大小统一为 4，并由服务端覆盖旧客户端传入的页大小；因此 11 条订单现在稳定显示为 3 页，不再有 3 条记录落在不可见区域。
- 新增 11 条订单首尾页回归测试，约束总页数、上一页和下一页状态；筛选、搜索和撤单刷新继续由服务端将页码夹在真实有效范围内。
- 复核撤单主链：UI 只对可撤订单携带真实订单 ID；服务端再次校验订单归属、方向和状态。卖单撤回未成交托管库存，买单释放剩余冻结资金，已成交订单拒绝撤回。

### 2026-08-13 - 订单搜索断线修复与终端数据包故障隔离

- 修复个人订单历史搜索在 PostgreSQL 中使用多字符 `ESCAPE` 导致 `invalid escape string` 的问题；查询改用单字符 `!` 转义，并安全处理 `%`、`_` 与转义符本身。
- 为终端客户端动作数据包增加服务端运行时异常边界。数据库或业务查询异常会记录完整服务端上下文，并向玩家返回通用失败提示，不再让异常穿透 Forge 数据包处理器并断开玩家连接。
- 新增搜索转义单元测试，并将真实 PostgreSQL 订单历史搜索纳入本轮定向验证。

### 2026-08-13 - 个人订单历史搜索、筛选与紧凑管理表

- 将标准市场“历史”页从四个大筛选按钮和松散文本行改为订单管理表：顶部第一行提供商品名称/物品键搜索、搜索与清空，第二行提供带字段前缀的商品、方向、状态和时间筛选。
- 搜索词随历史请求发送到服务端；PostgreSQL 同时匹配订单 `product_key` 与正式目录 `display_name`，所有筛选变更回到第一页，清空操作取消搜索和全部约束。
- 订单行改为结构化列，展示中文方向、正式商品名、价格、成交/总量、剩余量、中文状态和创建时间；仅可撤订单在本行显示精确撤单按钮，页脚与翻页控件不再覆盖列表内容。
- 扩展历史 payload、查询对象与目录名称回写，同时保留旧长度网络载荷兼容；补充 payload、筛选状态、订单行解析和草稿回写测试。
- 增加历史工作台在典型终端尺寸下的边界回归测试，约束搜索栏、筛选栏、订单表、行内撤单与页脚分页不得相互覆盖或越出内容区。

### 2026-08-12 - 市场操作回执与个人历史管理需求定稿

- 新增 `docs/market-action-receipt-and-personal-history-v1.md`，正式定义标准市场的即时操作回执和个人订单历史管理边界。
- 商品详情页三个主按钮锁定为 `买入 / 卖出 / 历史`；删除无目标的详情页撤单，撤单统一进入历史页后针对具体可撤委托执行。
- 历史页默认支持全部商品，并可按当前商品、买卖方向、订单状态和时间范围筛选；页面分为当前委托、成交记录、交付与异常三个标签。
- 右上角泡泡被定义为独立动作结果回执，必须与可重复刷新的市场 snapshot 分离，并提供持久操作记录作为遗漏回执的追溯入口。
- 本轮仅记录产品、交互、安全和接口需求，没有修改代码、构建或部署。

### 2026-08-11 - 标准市场全目录排序与历史恢复告警收口

- 标准市场的筛选和排序改为服务端先读取完整正式目录及批量真实行情，再执行“有成交 / 有盘口”和价格、涨跌、成交量排序，最后分页。客户端不再对已经分页的 12 个商品二次排序，避免页内排序伪装成全市场排序。
- 盘口点击继续走统一订单弹窗：点击买盘准备卖出限价单，点击卖盘准备买入限价单；数量和价格只作为弹窗草稿，最终仍由服务端重新校验。订单弹窗的“最大”按银行可用余额或账户仓可售数量计算。
- 市场审计扩展 Base Vault 未完成操作检查。严格审计确认正式目录、标准订单托管、买单冻结、定制市场、汇率市场和 Base Vault 当前均无新增异常。
- 对历史操作 16、17 完成资产核对：关联订单 52 已 `FILLED`、未成交量为 0，托管 9 已 `SETTLED`、数量为 0；它们是旧版本把“成交后再次撤单”误报为 `RECOVERY_REQUIRED` 的审计遗留。当前服务已将同类业务拒绝记为普通 `FAILED`，且不会改写已结算订单或托管。
- 修复重启恢复扫描器仍会把上述 `FAILED / SELL_ORDER_CANCEL` 再次升级的问题。恢复服务现在专门核对撤单关联订单；订单已 `FILLED` 或 `CANCELLED` 时保持终态 `FAILED`，不改写订单和托管，只有无法证明安全终态时才进入人工恢复。
- 验证：`git diff --check`、`bash -n scripts/market-audit.sh` 与标准市场、终端状态、订单弹窗定向 Docker Gradle 测试通过；历史记录结案后再次执行 `scripts/market-audit.sh --strict`。
- 最终交付：Docker Gradle `test` 与 `assemble` 全部通过；新 Lobby 启动后恢复扫描器自动将历史操作 16、17 收敛为安全 `FAILED`，`scripts/market-audit.sh --strict` 返回 `anomaly_count=0`。运行时 JAR SHA256 为 `178f2011eb92499872c1a821701400a4ea9d15fc676d17b5236c28a80a71e56c`，Lobby、S2 mods 目录与 Prism 客户端均已落同一文件；Lobby 达到 `Done (1.242s)!`。S2 未达到启动完成，其既有 `World/level.dat` 读取报 `Unexpected end of ZLIB input stream`，`level.dat_old` 为 0 字节；这是存档恢复事项，不通过重建世界或静默回滚绕过。

### 2026-08-11 - 玩家市场旧命令下线与汇率详情快照竞态修复

- 玩家标准、定制与汇率市场操作统一收口到银河终端 UI；`/jsirgalaxybase market` 不再路由买卖、报价、兑换或手持挂牌等旧玩家命令，补全也不再暴露这些入口。管理员市场恢复命令继续保留，用于资产异常处置。
- 汇率详情选择响应新增服务端权威 `selectedCoinCode`，并贯穿 snapshot、网络序列化和客户端 model。客户端在替换整屏 model 前核对当前待选币种或当前详情币种，丢弃晚到的旧选择响应，避免快速点击 A、B 后被 A 的异步快照覆盖 B。
- 汇率浏览模式仍接受普通刷新；返回浏览、滚动、切页及打开详情时维持既有 Hover 清理规则。共享四列浏览器补充正式空结果提示，不再留下无解释的整块空白。
- 验证：`git diff --check` 通过；补充汇率 A/B 乱序响应状态测试、`selectedCoinCode` 网络往返测试与市场命令表面测试。Docker Gradle 定向测试结果见本轮最终记录。

### 2026-08-10 - 定制与汇率市场工具栏、详情容器边界修复

- 修复非标准市场浏览页顶部空白/只剩范围按钮的问题：工具栏恢复由 `PanelContainer` 绘制真实搜索、刷新或发布、翻页控件，范围按钮继续作为紧凑筛选项，不再用整段自绘覆盖子控件。
- 修复定制与汇率详情动作按钮错误挂载到外层 section 的问题；详情面板改为拥有自身子控件的容器，切换页面、返回浏览和刷新快照时不会遗留跨页面按钮。
- 定制市场不再套用标准市场详情几何，新增专用挂牌详情布局，将商品/价格/托管状态、交易关系与购买/下架/领取动作约束在终端内容区。汇率详情同步压缩 Hero，并固定个人仓来源、正式报价与刷新/确认兑换动作区域。
- 验证：`git diff --check` 与 Docker Gradle 全量测试通过；新增工具栏子控件可见性、详情动作归属及两种专用详情布局边界测试。随后仅部署 Lobby 与客户端，不触碰 S1/S2，不启动客户端。

### 2026-08-09 - 定制市场演示挂牌与汇率专用详情收口

- 修复市场顶栏路由判定顺序：定制、汇率 snapshot 即使同时携带共享标准市场模型，也分别显示“市场 / 定制商品”和“市场 / 汇率市场”，不再误判为总入口。
- 汇率市场移除标准市场订单簿模板，改为专用的任务书硬币浏览、个人 Base Vault 来源、正式报价与兑换操作布局；目录由服务端结构化分页快照驱动，搜索、刷新和翻页均保留服务端上下文。
- 定制市场演示脚本新增 8 条带 `custom-market-ui-demo-v1` 来源标记的只读挂牌。应用服务在冻结资金、结算、下架和领取之前强制拒绝这些记录，演示数据只用于核验四列浏览、Hover 和详情页，不进入真实资产链。
- 验证：`git diff --check` 与 Docker Gradle 全量测试通过；新增汇率详情边界、市场顶栏优先级和演示挂牌只读隔离测试。Lobby、客户端与构建产物 SHA256 均为 `658ac0d069229e4fd585b5762baf320d07b8b2bfb1306086b6c7bf24c72a0b28`；Lobby 达到 `RUNNING` 并记录 `Done (1.425s)!`。PostgreSQL 已写入 8 条只读演示挂牌；严格审计仅报告 2026-08-07 遗留的两条 `SELL_ORDER_CANCEL / RECOVERY_REQUIRED`，本轮新增市场检查项均为 0。

### 2026-08-09 - 定制市场真实分页与非标准详情返回收口

- 修复定制市场此前仅在客户端对一次加载的最多 50 条挂牌再分页的问题。现在 PostgreSQL 按当前范围（全部挂牌、我的出售、待领取）、查询词、偏移和页大小计算总数并返回当前页，终端的总数与上一页/下一页状态来自服务端真实结果。
- 定制浏览 UI 不再回退解析旧的平行字符串列表；结构化浏览条目是唯一渲染来源。旧字段仍保留一轮网络兼容，但不再决定可见挂牌。
- 定制与汇率详情移除局部“返回浏览”按钮，顶栏返回在两种详情模式下统一回到保留搜索、范围、页码和滚动位置的浏览模式；标准市场既有行为未改。
- 没有改写定制挂牌、购买、下架、领取或汇率报价/兑换的服务端结算链。JDBC 分页查询已由编译和服务层定向测试覆盖；真实 PostgreSQL 中超过 50 条挂牌的范围、搜索和翻页仍列为灰度人工验收项。

### 2026-08-09 - 非标准市场三段式语义收口

- 共享市场浏览模型新增市场类型边界。定制挂牌与汇率硬币不再复用标准市场的买一、卖一、可卖、锁定、待收和价格图；卡片与 Hover 分别展示挂牌价格/交易方/交付状态，或硬币面值/目录层级/兑换资格。
- 定制市场发布改为“选择个人 Vault 的单件 -> 在独立价格弹窗填写正整数报价 -> 服务端确认”，不再依赖已移除的常驻价格输入。切换挂牌范围、搜索、翻页均请求服务端刷新，不再将空挂牌 id 误当作详情选择或因刷新强制进入详情。
- 汇率市场的搜索与分页同样通过结构化目录快照驱动；客户端不再重新从本地 `TaskCoinCatalog` 拼列表，避免分页、搜索与服务端目录状态失步。
- 保留服务端既有复核：定制实际购买、下架、领取仍由挂牌服务判断权限和交付状态；汇率目录选择不绕过个人 Base Vault 里的实际硬币与正式报价校验。
- 验证：Docker `compileJava`、市场消息/目录包、市场页面内容与壳层滚动定向测试，以及 `git diff --check` 通过；随后构建并部署 Lobby 与客户端，未启动客户端、未触碰 S1/S2。

### 2026-08-08 - 标准市场实时 JDBC 快照刷新

- 标准商品市场在终端停留期间每五秒请求一次新的服务端 snapshot，并保留搜索、页码、当前商品、图表周期与网格滚动位置。
- 自动刷新仅在标准市场、无 modal 且未编辑市场字段时运行；保持单一请求在途，并在响应丢失后有界恢复。后台 snapshot 不会盖掉打开中的确认弹窗。
- 刷新继续经过 `TerminalMarketService` 与 `StandardizedMarketReadRepository`，读取 PostgreSQL 正式目录、订单簿、成交记录、个人 Vault 可售量和银行状态；客户端不合成价格、盘口或流动性。
- 新增定时刷新状态机测试；Docker Gradle 定向验证与 Lobby/客户端部署在本条之后记录。

### 2026-08-08 - 标准市场紧凑字号与图表边界收口

- 将详情页“24h 行情 / 账户与交付”及其字段统一为盘口使用的 `0.78` 文字比例，并压缩行距与统计区高度；释放出的垂直空间交给五档买盘、最近成交和卖盘。
- 新增市场紧凑文字度量工具，主图坐标使用 `0.68` 比例并按缩放后的真实宽度预留轴空间，降低价格、成交量和时间标签重叠风险。
- Hover 图表坐标使用相同紧凑轴字号与宽度计算，保证最大/最小价格及起止时间始终留在浮层边界内。
- 删除浏览商品卡右下角流动性状态点；流动性文字仍保留在 Hover 中，不损失行情语义。
- 验证：`git diff --check` 与 Docker Gradle 全量测试通过；构建并部署 Lobby 与客户端后由用户执行实机视觉验收。

### 2026-08-08 - 标准市场日内涨跌、图表与交易弹窗收口

- 浏览行情：商品卡的涨跌改为以上海时区当日 `00:00` 后第一笔真实成交为开盘基准，并显示服务端计算的日内涨跌百分比；当日无成交时明确显示中性空态。紧凑卡片、Hover 和详情摘要移除重复的 `STARCOIN` 后缀，货币含义由市场上下文承担。
- 浏览与 Hover：商品卡移除占空间的迷你图，只保留价格、日内涨跌和流动性。Hover 补齐最新价、今日涨跌、零点基准、买一/卖一、24h 成交量、流动性、账户仓状态，并以红绿涨跌柱和明确的价格/时间坐标展示真实成交变化；不足两个真实成交点时不保留空图区域。
- 详情布局：压缩 Hero 与页脚高度，扩大图表和盘口占比；价格区使用蓝色真实成交柱，成交量按相邻价格涨跌显示红绿柱，补齐价格、成交量和时间坐标。买盘使用绿色、成交使用中性色、卖盘使用红色。
- 交易交互：详情页移除常驻价格与数量输入框，只保留买入、卖出、撤单命令。买入/卖出打开独立订单弹窗，在弹窗内选择市价/限价并填写数量、价格，继续复用既有统一订单 payload 和服务端复核链。
- 防叠层：修复 `LabelPanel` 在零高度布局中仍绘制首行的框架缺陷，防止已隐藏标签跨卡片叠层。
- 验证：`git diff --check`、Docker Gradle 全量测试和 `assemble` 通过；与本轮详情分栏一起部署 Lobby 与客户端，由用户执行实机视觉验收。

### 2026-08-08 - 标准市场详情左右主分栏收口

- 依据用户确认的最新效果图，将标准市场详情改为左侧约 48% 的紧凑行情与操作区、右侧约 52% 的全高主图；商品头不再横跨全页，主行情图获得完整纵向空间。
- 左侧依次放置紧凑商品条、买盘/最近成交/卖盘、两列关键统计和买入/卖出/撤单按钮。移除底部三张大状态卡及按钮下方操作提示，交易参数继续在独立弹窗填写。
- 为避免共用布局回归，保留定制市场和汇率市场使用的原 `MarketDetailLayout.within(...)`，仅标准市场改用 `withinStandardSplit(...)`。
- 验证与部署：`git diff --check`、Docker Gradle 全量测试和 `assemble` 通过；构建端、Lobby 与客户端 JAR SHA256 均为 `03d87c311071a35b1667f31016f96607913f100517e136953892ad78da4c5118`。Lobby 已监听 `25564` 并记录 `Done (1.419s)!`；未启动客户端，未触碰 S1/S2。

### 2026-08-08 - 标准市场现代交易终端 P0-P3 第一轮落地

- 主题：以 `/home/u24/图片/市场第三版.png` 为视觉基准，将标准市场从旧托管操作页改为账户仓自动交割的现代交易工作台。
- 交易主链：新增账户库存解析接口及 Base Vault 实现。限价/即时卖出从个人 Vault 跨格聚合预留后进入既有市场托管和订单链；即时剩余量与撤单库存尝试返回 Vault。买入成交自动尝试投递 Vault，容量或交付异常继续保留既有待处理和恢复记录。旧存入/领取路由保留兼容，但不再是新 UI 的常规主流程。
- 行情与 UI：新增批量只读行情投影和成交/订单读取索引；浏览页固定 `4 x 3`，加入真实涨跌、迷你走势、流动性、筛选和排序；Hover 收成真实行情比较卡。详情页采用横向图表、盘口和统一订单票据，页脚只保留账户仓、委托和异常待收货摘要。
- 合同：新增统一 `MARKET_CONFIRM_ORDER` 及可兼容旧 payload 的买卖方向、订单类型、数量、限价和浏览上下文字段；服务端继续重新校验价格、余额、库存与手续费。
- 验证：`git diff --check` 通过；Docker Gradle 全量测试通过（301 个完成，31 个按环境跳过）。详情图后续补接 `1h / 24h / 7d` 真实聚合桶切换，当前显示收盘价线与成交量；完整 OHLC 实体、数据库级全目录排序、盘口回填和最大数量快捷操作明确留作后续，不作完成宣称。

### 2026-08-08 - 标准市场真实行情区间接线与重新部署

- 主题：补齐第一轮复盘发现的“服务端已有三周期聚合、客户端仍固定 24h”断点。
- 结果：标准市场详情图新增 `1h / 24h / 7d` 区间控件；浏览/刷新 payload 保留区间，服务端按区间读取真实桶，并通过兼容价格点合同回写收盘价、成交量和桶时间。区间切换不改撮合、银行、Vault 或定制/汇率市场。
- 边界：当前仍是聚合收盘价线与成交量柱，不是完整 OHLC K 线；全目录数据库级排序、盘口点击回填和“最大”快捷数量仍未完成。
- 验证与部署：`git diff --check`、Docker Gradle 全量测试和 `assemble` 通过（302 个测试完成，31 个按环境跳过）；仅部署 Lobby 与客户端，三处 runtime jar SHA256 均为 `54b8514b85f5f498e58b813bc1927e994262eedbc4f5612f3972d3155865fc6c`。Lobby 达到 `RUNNING` 并记录 `Done (1.723s)!`；S1/S2 未启动，客户端未启动。

### 2026-08-08 - 现代交易终端最新效果图差异基线

- 主题：以 `/home/u24/图片/市场第三版.png` 为标准商品市场唯一视觉目标，复盘当前浏览、Hover 和详情三张实机图。
- 结论：终端壳、四列骨架、真实 ItemStack、Hover 与独立详情路由已经成立；主要差距为浏览仅 8 项且缺少涨跌/迷你走势、Hover 过大且缺少真实行情、详情仍是旧盘口文本和七按钮工作流，以及手动存入/领取仍暴露内部托管语义。
- 产物：新增 `modern-trading-terminal-visual-gap-baseline-2026-08-08.md`，按 UI、行情聚合、交易 payload、账户仓自动交割四类记录差距，并给出 P0-P3 优先级和逐屏验收清单；`modern-trading-terminal-redesign-v1.md` 与文档索引已建立追溯链接。
- 验证：仅文档变更；不修改代码、数据库或部署产物。执行 `git diff --check` 后收口。

### 2026-08-07 - 个人 Base Vault 成为市场唯一物品边界

- 主题：标准、定制与汇率市场统一以个人 Base Vault 为物品来源与收货账户。
- 结果：新增只读 `VaultAssetPickerPopup`，标准市场按正式目录商品聚合选择数量，定制发布与汇率兑换选择精确 Vault 格位。标准入托管复用同一 request id 完成 `Base Vault -> AVAILABLE`，定制市场的单件托管和汇率的任务书硬币扣除继续走 Vault 服务；标准与定制领取均投递个人 Vault，满仓则保留既有待领取状态。定制/汇率页面现在也随市场快照携带只读 Vault 资产，选择器不再依赖玩家背包或当前手持。
- 安全边界：选择器只读，市场动作重新校验 Vault 格位/商品目录/报价；版本冲突、数量不足、Vault 满仓或报价过期不会自动改读玩家背包或自动重试。
- 验证：`git diff --check`、Docker `compileJava` 已通过；待执行市场 payload、服务与 UI 定向测试后部署 Lobby 与客户端，不触碰 S2。

### 2026-07-24 - Base Vault Shift 转移：补齐客户端原版容器预测

- 主题：修复个人 Base Vault 中 Shift 快速转移会使 GTNH 客户端卡死的问题。
- 根因：服务端 `BaseVaultContainer` 已实现 Vault 与玩家背包之间的 `transferStackInSlot(...)`，但客户端占位 `ClientContainer` 沿用 `Container` 的默认空实现。服务端可以成功写入 Vault 审计与槽位，而客户端本地容器预测不执行对应搬运，造成原版槽位同步路径失配。
- 结果：客户端容器现在镜像服务端的 Vault/玩家背包快速转移规则，并补齐原版 `onPickupFromSlot(...)` 回调；服务端同样补齐该回调和“无实际变更返回空”的原版语义。普通点击与既有 PostgreSQL 版本校验、审计、恢复链未改。
- 安全补充：显式拒绝原版 `mode=3` 的创造模式中键复制，避免把跨服持久 Vault 槽位复制到光标而没有对应账本扣减。
- 验证：待执行 Docker 定向编译/测试与 Lobby、客户端部署；人工验收应覆盖玩家背包到 Vault 和 Vault 到玩家背包两个方向的 Shift 操作，以及关闭后重新打开的持久性。

### 2026-07-23 - Base Vault 容器化 Phase 1：原版格位交互接入跨服槽位账本

- 主题：将终端“仓库”从说明 section 替换为服务器权威的原版 `Container + Slot` 保险箱页。
- 结果：个人 Vault 以固定 `9 x 3` 格显示在上方，玩家背包和快捷栏显示在下方；左/右键、拖拽、拆分、双击合并与 Shift 转移继续由原版 Container 语义处理。Vault 格位是会话库存，完成一次真实变动后以原开仓版本进行 PostgreSQL 校验和提交；提交失败会还原本次会话与玩家背包快照，成功操作记录 `VAULT_CONTAINER_MUTATION` 审计。
- 入口与边界：终端导航“仓库”直接打开 Forge GUI ID `41`，不再显示通用仓储说明卡。容器仅开放个人 27 格；企业/公共容量、市场 Vault 选择器、AE2 Cell Bay/Drive/Port 保持后续阶段，不与本轮原版搬运交互混合。
- 验证：`git diff --check`、Docker `assemble`、Docker 定向 `BaseVaultServiceTest` 通过；新增容器快照提交与版本更新回归测试。按灰度约束仅准备部署 Lobby 与客户端，不触碰已知损坏的 S2 世界。

### 2026-07-20 - Base Vault Phase 0：有限账户仓与市场交付边界

- 主题：建立跨服持久的有限 Base Vault，作为个人、企业、公共账户的起步仓与市场交付目标。
- 结果：新增账户、槽位与操作日志迁移；个人容量为 27 格，企业/公共为 54 格。槽位保存完整压缩 `ItemStack` NBT 并遵守原版堆叠上限。标准市场领取、定制市场领取/下架改为投递个人 Vault；Vault 满时资产保留在现有待领取状态。标准市场存入改为 Vault -> 市场托管的共享 JDBC 事务，失败整体回滚。
- 恢复边界：每次 Vault 操作使用 request id 幂等记录；未完成操作拒绝自动重放。玩家背包只会在后续独立 Vault 页的显式存取适配器中接触，市场主页面不再允许把背包作为标准商品存入来源。
- 验证：Docker `compileJava` 通过；定向测试通过 `BaseVaultServiceTest`、`TerminalMarketServiceTest`、`StandardizedSpotMarketServiceTest`；`git diff --check` 通过。未部署，必须先在停服窗口运行新迁移。

### 2026-07-20 - 标准市场浏览：空选中状态不再丢失正式目录页

- 主题：修复标准商品市场首次进入浏览模式时，数据库目录已存在但终端显示 `0 项 / 0/0` 的断链。
- 根因：`TerminalMarketService.createSnapshot(...)` 会先读取数据库正式目录，但当玩家尚未选中商品时提前返回详情空态，未将刚读取的 `catalogPage` 与批量行情摘要附加到该 snapshot；网络层因此只能收到默认空目录。
- 结果：目录附加收口为 `attachCatalogBrowserData(...)`，无选中商品和已选商品两条路径都回传当前搜索页、分页元数据与行情摘要。未选中仅意味着详情页为空，绝不意味着目录为空。
- 验证：新增空选中快照保留正式目录页的回归测试；Docker Gradle 定向测试通过 `TerminalMarketSectionServiceTest`、`TerminalMarketServiceTest`、`TerminalMarketCatalogPacketTest`，`git diff --check` 通过。runtime jar 已部署至 Lobby 与客户端，SHA-256 为 `bdaea9c249eca7e4b173f7f55ecc2d67fdd3d54370e362411d385b71cc77a7f4`；数据库烟测确认正式启用目录为 8 项。

### 2026-07-19 - 市场三段式浏览基础：四列目录、只读 Hover 与独立详情模式

- 主题：将标准市场从常驻三栏工作台转为可被三个市场复用的“浏览 -> 对比 -> 操作”交互基础。
- 结果：新增四列 `MarketItemGridPanel`，搜索和分页保持固定、只有网格可滚动；目录行带真实物品图标与结构化行情摘要。`CanvasScreen` 新增被动 hover layer，层级固定为正文、hover、modal popup，悬浮信息按终端边界自动翻转夹紧，滚动、点击、离开与 popup 打开都会关闭。标准市场点击目录项先沿用现有选择/快照回写，服务端确认选中后进入客户端 `DETAIL` 模式；顶栏返回回到浏览模式并保留浏览上下文。目录快照补充批量成交摘要与最多十二个真实价格点，无真实历史时明确空态。
- 验证：新增 tooltip 定位与四列网格边界测试；更新标准市场布局测试以匹配浏览全页、详情独立页的边界。待执行 Docker Gradle 定向测试、部署 Lobby 与客户端；本轮不启动客户端，实机观感交由人工验收。

### 2026-07-19 - 市场三段式整改第二阶段：标准商品详情工作台边界收口

- 主题：将标准市场点击后的详情模式从临时拼装的工作区，收口为可供定制和汇率市场复用的固定预算详情工作台。
- 结果：新增 `MarketDetailLayout`，由详情父容器统一推导 Hero、三列盘口、资产状态卡和底部交易区的边界；详情页不再拥有整页滚动。交易区将六个输入拆为两行，避免与交易 CTA 在窄高区域竞争同一行；固定动作条补齐领取与撤单，且仅在快照给出目标 custody/order 时启用。库存、待领取、订单/冻结三张卡的标题、数值和语义行分开绘制，消除数值与副文案叠字；浏览网格滚动偏移回写到 `TerminalMarketSectionState`，顶栏从详情返回浏览后可恢复查询、页码和网格位置。既有存入、限价、即时、撤单、领取确认与服务端 snapshot 链未改变。
- 验证：新增 `MarketDetailLayoutTest` 和详情返回浏览上下文测试；Docker Gradle 定向测试通过。待构建部署 Lobby 与客户端；按约定不启动客户端，实机 UI 和点击由人工确认。

### 2026-07-19 - 市场现场可用性：弹窗渲染隔离、受管演示流动性与任务书硬币目录

- 主题：修复市场现场验证中暴露的图标层级、定制市场底部动作条、空经济无法验证买卖，以及汇率页把任务书硬币过度简化为单一目标的问题。
- 结果：`CanvasScreen` 和真实 `ItemStack` 渲染器会在物品图标绘制后清理深度状态，popup 遮罩和正文不再被旧物品图层穿透；定制市场的发布/购买/下架/领取统一保留在固定高度底部操作条内。新增显式 `TaskCoinCatalog`，仅接纳 GTNH 本地语言数据确认的 15 个任务书硬币家族、每族 5 个面额（共 75 种），排除捐赠币与区块加载币；汇率页将它们作为可滚动的正式输入目录，实际报价仍由玩家手持物和服务端规则决定。新增 `market-demo-fixture.sh`，隔离没有托管或资金约束的旧视觉 demo 订单，并创建有真实 escrow/frozen-funds 的受管 Steel 流动性；当前离线 UUID 的玩家账户已单独补齐测试余额和可卖托管库存。审计把已安全失败、没有资产副作用的历史 preflight 操作作为历史计数显示，不再误报成需要恢复的未完成操作。
- 验证：待执行 Docker Gradle 定向测试、严格市场审计与 Lobby/客户端部署；不启动客户端，实际 UI 和点击由人工确认。

### 2026-07-19 - Round 4：仓库跨域转移与审计合同收口

- 主题：把市场托管仓、未来企业仓与公共仓从概念性区分推进为可实现的转移/审计边界，不提前实现不成熟的企业或公共库存。
- 结果：新增仓库转移合同，定义四类资产域、允许/禁止的转移矩阵、`InventoryTransferOperation` 必填字段、request id 幂等要求，以及 Minecraft 物品交付与数据库状态无法原子化时的 `RECOVERY_REQUIRED` 原则。现有市场托管、定制交付和汇率兑换均被明确纳入同一中断处理约束，但仍保持各自独立账本。
- 验证：文档结构复核；未触及运行时代码、数据库或客户端。

### 2026-07-15 - 汇率市场 Round 4：物理任务书硬币与银行结算的中断可审计

- 主题：补齐汇率兑换中“玩家手持任务书硬币”这一非数据库资产与银行正式结算之间的中断边界。
- 结果：兑换在移除手持物品前创建 `EXCHANGE_EXECUTION` 操作日志，并与银行结算复用同一个 request id。成功时日志记录银行交易与兑换记录；明确失败时恢复手持物品并留下已收口失败记录；启动/管理员恢复扫描若发现已经存在银行结算便完成操作，若无法确认结算则标为 `RECOVERY_REQUIRED`，附带物品、数量、meta、槽位等核对元数据，禁止系统擅自重试或静默吞没资产。
- 验证：Docker `compileJava` 已通过；新增 `MarketRecoveryService` 单测覆盖“已结算自动收口”与“未确认物理输入升级人工核对”。不启动客户端。

### 2026-07-15 - 标准市场领取恢复：安全交付失败在事务回滚后即时收口

- 主题：修复标准市场领取时“背包明确未收到物品”的安全失败在 JDBC 事务回滚后仍停留于 `PROCESSING` 的状态滞后。
- 影响范围：`StandardizedSpotMarketService`、PostgreSQL 市场集成测试与市场命令帮助。
- 结果：背包满等 `safeToRestoreClaimable` 失败现在在领取事务回滚后另开收口事务，将托管资产确认回 `CLAIMABLE` 并将操作日志标为 `FAILED`；若此收口本身失败，才按既有规则升级为 `RECOVERY_REQUIRED`。这样玩家可用新请求重试领取，审计也不会把正常的安全失败误表示为卡住的处理中操作。市场帮助同时列出定制市场管理员 `custom recover` 入口。
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过 `StandardizedSpotMarketServiceTest`、`MarketPostgresIntegrationTest`、`GalaxyBaseCommandTest`，其中新增 PostgreSQL 回归覆盖安全发放失败后的 `CLAIMABLE + FAILED` 即时状态。

### 2026-07-15 - 汇率市场二次确认：服务端绑定正式报价而非仅信任客户端弹窗

- 主题：将终端汇率市场的二次确认从“客户端显示过 popup”补为服务端可验证的短时、一次性确认许可。
- 结果：每次服务器生成可执行兑换快照时，会将正式兑换标的、输入物品、数量、资产对、规则版本、限额状态、结算额与汇率绑定到玩家和终端会话；确认时服务端重新读取当前 quote 并逐项校验，成功后即消费许可。手持物品、规则、限额或会话发生变化均不能沿用旧确认，必须刷新报价并重新确认。实际金额仍由 `ExchangeMarketService` 在结算前重新计算，客户端不提供可被信任的金额字段。
- 验证：新增确认闸门单测覆盖一次性消费与报价金额/汇率变化拒绝；后续与 `TerminalServiceTest`、`ExchangeMarketServiceTest` 一起通过 Docker Gradle 定向验证。

### 2026-07-15 - 定制市场交付恢复：区分安全失败与不确定交付

- 主题：为定制挂牌的下架/领取交付补齐防重复派发保护，避免数据库状态与背包发放的非原子边界被静默误判。
- 影响范围：`CustomMarketService`、`GalaxyBaseCommand`、`CustomMarketAuditLog` repository、`custom_market_audit_log` migration、市场审计脚本与定制市场服务测试。
- 结果：终端及兼容命令入口都会在交付前先将挂牌（及成交记录）置于保护状态，并创建 `LISTING_DELIVERY` 审计记录。背包满等明确未交付错误会恢复原始 `ESCROW_HELD` 或 `BUYER_PENDING_CLAIM`；普通运行时中断或显式不确定交付被标为 `DELIVERY_UNKNOWN` 并保持 `EXCEPTION`，后续请求不能自动再次发货，必须由审计/人工恢复流程处理。管理员可用 `/jsirgalaxybase market custom recover <listingId> <restore|complete> [reason]` 显式决议：`restore` 仅在确认未交付后恢复原状态，`complete` 仅在确认已交付后收口状态，两个分支都不会再次操作背包。审计脚本将“处理中”和“不确定交付”纳入严格异常计数。
- 验证：Docker Gradle 定向测试通过 `CustomMarketServiceTest`、`GalaxyBaseCommandTest`、`MarketPostgresIntegrationTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`、`StandardizedSpotMarketServiceTest` 与 `ExchangeMarketServiceTest`；其中新增覆盖未知交付失败、交付成功后完成落库失败，以及对应的禁止自动重试。已通过部署脚本将 runtime jar `73f39d17b1ef0e54afb72711026707ef0f73adb9330e0d4677e9a9c886eb3fb2` 同步到 Lobby、S2、客户端；migration `20260715_003_add_custom_market_delivery_audit_type.sql` 已应用，Lobby 的市场烟雾测试和严格审计通过。S2 jar 已同步，但其 `World/level.dat` 与 `level.dat_old` 均为 EOF 损坏，世界启动失败，和本轮 market jar 或迁移无关。

### 2026-07-15 - 市场恢复链：定制挂牌接入真实背包交付与统一审计

- 主题：收口定制市场过去“只改数据库状态、不把物品交给玩家”的断链，并把它接入现有无客户端市场审计。
- 影响范围：`CustomMarketService`、Minecraft 定制交付适配器、终端定制发布/下架/领取动作、`scripts/market-audit.sh` 与仓库边界文档。
- 结果：终端可以将当前手持的单件物品发布为定制挂牌；卖家下架或买家领取时会先执行背包容量检查和 `ItemStack` 快照交付，再收口数据库状态。市场审计现在额外检查定制挂牌快照缺失、已售缺成交、挂牌/成交交付状态不一致与待领取/异常资产；`--strict` 对真正异常返回非零，待领取只作为运维待办列出。
- 验证：`git diff --check`、`bash -n scripts/market-audit.sh` 通过；只读审计的标准与定制异常计数均为 `0`；Docker Gradle 定向测试通过 `CustomMarketServiceTest`、`TerminalMarketServiceTest` 与 `TerminalServiceTest`。不启动客户端，实机 UI 与点击仍交由人工验收。

### 2026-07-15 - 标准市场 Round 3：确认报价补齐费用、来源与恢复语义

- 主题：让标准市场的确认弹窗不再只写“确认交易”，而是展示服务端生成的真实报价摘要和资产去向。
- 影响范围：`TerminalMarketService`、`TerminalHomeScreen`。
- 结果：即时买入报价明确拆出本金、taker 手续费、冻结/结算总额和极限成交价；即时卖出明确拆出成交额、手续费与净到账。限价买卖、存入、即时买卖、撤单和领取确认弹窗均补充资产来源、冻结/托管去向，以及余额、库存、深度、背包发放失败等服务端拒绝或恢复条件。
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过 `TerminalServiceTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalMarketSectionContentTest`、`TerminalMarketServiceTest`、`StandardizedSpotMarketServiceTest`。未启动客户端。

### 2026-07-15 - 汇率市场 Round 4：正式结算链的无客户端审计接入

- 主题：确认汇率市场不是独立的 GUI 计算，而是具有规则、限额、二次确认、银行结算和审计关联的正式链路。
- 影响范围：`scripts/market-audit.sh`。
- 结果：审计会检查每条 `coin_exchange_record` 对应银行交易的双方审计元数据都标明 `marketType=exchange`；这与现有兑换服务写入的兑换对、规则版本、限额状态和输入资产审计字段共同构成可追溯证据。
- 验证：`bash -n scripts/market-audit.sh`、`scripts/market-audit.sh --strict` 通过；当前库没有历史兑换记录，因此这是结构对账验证，不替代实际兑换样本验证。

### 2026-07-15 - 市场 Round 4：定制挂牌与汇率页面收口为受边界约束工作台

- 主题：将定制市场和汇率市场从旧的卡片内标题、子页返回按钮及自然向下堆叠，收口为与标准市场一致的双栏工作台。
- 影响范围：`TerminalCustomMarketSection`、`TerminalExchangeMarketSection` 与市场滚动布局测试。
- 结果：两页均移除了重复的“返回总入口”和正文标题，统一由终端顶栏的返回、刷新、帮助、关闭控制；左侧挂牌/兑换标的列表成为各自唯一滚动区，右侧商品交付详情或正式报价、执行信息及确认按钮保持固定在页面边界内。定制市场继续使用独立挂牌/交付托管语义，汇率市场不引入商品托管状态。
- 验证：`compileJava`、Docker Gradle 定向测试 `TerminalShellPanelsScrollTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalMarketSectionServiceTest`、`StandardizedSpotMarketServiceTest` 与 `git diff --check` 通过。

### 2026-07-15 - 标准商品目录：完成 8 条 GTNH 正式条目

- 主题：收口历史目录迁移遗留的技术展示名，确保正式目录不再把 `product_key` 直接暴露给玩家。
- 影响范围：新增 GTNH 元数据正式化迁移、目录制度文档和 `market-smoke-test.sh` 自检。
- 结果：将铝锭、铜锭、银锭、铁板和钢板补为经 GTNH 2.8.4 `GregTech.lang` 元数据核验的名称与单位，并写入已有最近成交价和确定性排序；烟雾测试现在会拒绝任何仍以技术键作为展示名的启用目录条目。

### 2026-07-15 - 标准商品市场 Round 3：增加无客户端状态审计

- 主题：为既有托管、冻结资金、领取恢复和市场操作日志提供可重复执行的只读审计入口。
- 影响范围：新增 `scripts/market-audit.sh` 与标准商品目录运维文档。
- 结果：审计可检查目录正式化、开放卖单对应 `ESCROW_SELL`、开放买单冻结资金、陈旧 `CLAIMING`、异常托管和未收口操作；`--strict` 会以非零状态提示管理员干预，恢复动作仍需显式使用 `/jsirgalaxybase market recover`。

### 2026-07-15 - 标准商品市场 Round 2：目录浏览器接入结构化目录与分页

- 主题：将 `MARKET_STANDARDIZED` 左栏从旧的平行字符串商品列表切换到正式目录快照，并使查询和分页通过既有市场 payload 回到服务端。
- 影响范围：`TerminalMarketSectionState`、`TerminalMarketSectionContent`、`TerminalMarketSection`、`TerminalShellPanels`、`TerminalHomeScreen` 及市场布局/内容测试。
- 结果：
  - 左栏优先读取 `CatalogProductModel`，使用正式商品键、registry/meta、展示名、单位、参考价和可交易状态；停用目录项不会作为可点击商品出现。
  - 商品行使用 registry/meta 构建真实 `ItemStack` 图标引用；渲染失败时才退化为已有 badge。
  - 搜索词、页码、上一页/下一页控制写入 `TerminalMarketSectionState -> TerminalMarketActionPayload`，并以既有 `MARKET_REFRESH` 请求服务端目录页，不引入第二条网络链。
  - 左栏仍是标准市场唯一的 `VerticalScrollPanel`；搜索和分页器保持固定，右侧行情、资产状态和动作条保持固定布局。
  - 更新布局测试，验证浏览器正式控件、固定分页器以及右侧无滚动边界。

### 2026-07-15 - 标准商品市场 Round 1 收尾：结构化目录快照与参考价

- 主题：把正式商品目录的商品行、查询和分页状态纳入终端快照/网络契约，替代 UI 只能依赖 `productKeys/productLabels` 平行字符串数组的状态。
- 影响范围：新增 `CatalogProduct` / `CatalogProductModel`，包含商品键、registry/meta、展示名、单位、排序、启用状态、参考价与可交易状态；市场 section 快照、客户端模型和终端网络包均追加目录页数据；新增 `reference_price` 迁移与目录管理 SQL 说明。
- 行为边界：旧字符串数组暂时保留，只用于现有 UI 兼容；Round 2 将改为直接消费结构化目录行。禁用商品仍由 JDBC 查询和目录准入边界排除，不能浏览或通过手持物快捷存入。
- 验证：Docker Gradle `compileJava` 与定向测试通过：`StandardizedMarketCatalogServiceTest`、`TerminalMarketCatalogPacketTest`、`TerminalMarketActionPayloadTest`、`TerminalMarketSectionServiceTest`；覆盖搜索/页码、空目录、禁用准入和目录网络 round-trip。

### 2026-07-14 - 标准商品市场 Round 1：正式目录与托管主链收口

- 主题：把标准商品市场从 GregTech 硬编码推断准入转为管理员维护的 PostgreSQL 正式目录，并让终端浏览来源与玩家手持、订单、成交反推解耦。
- 影响范围：新增 `standardized_market_catalog` 迁移和 JDBC 目录源；`InstitutionCoreModule` 运行时改为注入数据库目录；`StandardizedSpotMarketService` 新增目录分页能力；终端 payload 增加查询、页码和筛选上下文；部署编排在服务端替换 jar 前自动执行版本化迁移。
- 行为边界：卖单和即时卖出仍只消耗 `AVAILABLE`；`ESCROW_SELL`、`CLAIMABLE` 和买单冻结资金继续复用现有订单、托管、银行链。手持物品仅可快捷存入已准入商品，不能再成为正式目录真相来源。
- 验证：`git diff --check`、`bash -n` 通过；Docker Gradle `compileJava` 通过；定向单测通过 `StandardizedMarketCatalogServiceTest`、`TerminalMarketActionPayloadTest`、`TerminalMarketSectionServiceTest`、`StandardizedSpotMarketServiceTest`、`MarketPostgresIntegrationTest`、`InstitutionCoreModuleTest`、`TerminalMarketServiceTest`。
- 后续：Round 2 用正式目录数据收口标准市场浏览器 UI，包括搜索、分页控制、紧凑商品行和结构化目录行网络 DTO；Round 3 覆盖完整交易恢复、审计与异常资产场景。

## 记录规则

- 每次代码变更后，追加一条简要记录
- 每条记录至少包含：
  - 日期
  - 变更主题
  - 影响范围
  - 简要原因
- 如果变更依赖外部制度文档或外部参考源码，应写出引用来源

## 当前关键引用文档

当前开发默认参考下面这些文档：

- `../README.md`
  - 当前项目定位、代码结构和正式架构约束
- `../../Docs/设定.md`
  - 制度目标、职业、市场、贡献度、跨服阶段路线
- `../../Docs/技术边界文档.md`
  - `JsirGalaxyBase` 的责任边界与跨服同步边界
- `../../Docs/做法.md`
  - 群组服、中心数据库、同步方案与后端边界
- `../../Docs/市场经济推进.md`
  - 市场账本、订单、托管库存与一致性要求
- `../../Docs/下次对话议程.md`
  - 当前已定稿制度结论与后续讨论顺序

## 初次对话摘要

### 2026-06-29 - 标准商品市场默认交易草稿与按钮启用链路修复

- 主题：修复 `MARKET_STANDARDIZED` 已显示商品但买卖按钮不可用的问题，把紧凑交易 UI 接回现有确认 popup 与市场服务动作链
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSectionService.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSectionServiceTest.java`、`docs/WORKLOG.md`
- 原因：数据库、运行时目录和终端 snapshot 已能读取 8 个标准商品，但新版紧凑 UI 不再展示价格/数量输入框；旧动作链仍要求 `LimitBuyDraft`、`LimitSellDraft`、`InstantDraft` 中存在价格和数量，导致 `即时买`、`即时卖`、`买挂牌`、`卖挂牌` 在选中商品后仍保持 disabled，没有进入 `TerminalMarketService` 的真实买卖逻辑
- 结果：标准商品 snapshot 现在会从当前盘口和玩家 AVAILABLE 库存生成保守默认草稿：限价买默认最低卖盘价/数量 1，即时买在有卖盘时默认数量 1；限价卖默认最高买盘价/数量 1，即时卖仅在玩家有 AVAILABLE 且有买盘时默认数量 1；显式 payload 输入会被保留，不会被默认值覆盖；按钮启用条件改为基于生成后的可执行草稿和库存/盘口状态
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionServiceTest`、`TerminalMarketServiceTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalMarketSectionStateTest`、`TerminalMarketSectionContentTest`、`StandardizedSpotMarketServiceTest`；`scripts/deploy-jgb.sh --targets lobby,s2,client` 已构建 runtime jar SHA256 `b105a92c1a77ac6df416ecab64fe2368142beff916f83e10e102f3c684c6b6c9` 并替换 Lobby、S2 与客户端 mods；Lobby `market-smoke-test.sh --target lobby` 通过，DB 有 8 个活跃商品且运行时目录准入 8/8；S2 因 `World/level.dat` 与 `level.dat_old` 读取 EOF 导致世界启动失败，未达到 `Done`，不是本轮 market jar 编译或部署失败

### 2026-06-28 - 标准商品市场空列表自测与运行时目录诊断

- 主题：补齐 `MARKET_STANDARDIZED` 空列表问题的可靠自测链路，区分数据库有数据、运行时目录拒绝和终端 snapshot 未刷新三类原因
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/GregTechStandardizedMetalCatalog.java`、`scripts/market-smoke-test.sh`、`docs/WORKLOG.md`
- 原因：实机页面已能正常显示布局，但商品列表仍为 `共 0 项`；直接看 PostgreSQL 只能证明订单/成交/仓储表存在候选 `product_key`，不能证明这些 key 被 `GregTechStandardizedMetalCatalog` 在 Forge/GT 运行时准入。此前 `appendTradableKeys(...)` 会静默丢弃目录拒绝的 key，导致“DB 有数据但页面为空”缺少日志证据
- 结果：`TerminalMarketService` 在标准商品 snapshot 中记录来自活跃订单、成交记录、玩家订单和玩家 custody 的 DB 候选 key；如果候选 key 存在但运行时目录一个都不准入，会按 30 秒节流输出 `[market-smoke]` 诊断日志和前 12 条拒绝原因；新增 `scripts/market-smoke-test.sh --target lobby|s2`，可直接检查 DB 活跃商品、成交、仓储、服务器 `Done` 状态和最新运行时目录拒绝日志
- 补充：`InstitutionCoreModule` 在市场运行时创建完成后会主动执行标准商品目录 smoke，不需要打开 G 也能在服务器日志里看到 `[market-smoke] Standardized market startup smoke admitted ...` 或 `rejected all ...`；脚本现在要求同时存在 DB 候选和运行时准入日志，否则不会把数据链判定为通过
- 修复：实测发现当前 GTNH `gregtech.api.enums.Materials` 没有 `hasMetalItems()` 方法，旧目录适配器把它当作必需方法导致 8 个 DB 商品全部被拒；已改为兼容式可选检查，并以 `SubTag.METAL` 作为真实金属准入判断，同时保留更具体的反射失败信息
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`InstitutionCoreModuleTest`、`StandardizedSpotMarketServiceTest`、`TerminalMarketServiceTest`、`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已部署 runtime jar SHA256 `8f7145db3b9bff94f62549834dd9202fdd69d740c01e4a2af87988e88b0953e7`；`scripts/market-smoke-test.sh --target lobby` 最终通过，DB 有 8 个活跃商品 key、8 类 recent trades、玩家 custody 有 `AVAILABLE/CLAIMABLE` 数据，Lobby 运行时目录准入 `8/8` 个 DB product keys

### 2026-06-28 - 标准商品市场紧凑文字错位修复与分页/上架现状确认

- 主题：修复 `MARKET_STANDARDIZED` 实机截图中搜索状态行和状态小卡片的文字错位，并核清当前标准商品目录、分页和上架链路的真实能力边界
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：实机截图中 `服务: 在线` 与搜索占位文本容易挤压，`库存`、`可领`、`订单` 三个状态卡把标题、数值和副说明压成三行后在 GTNH 字体下仍有错位风险；同时玩家看到 `共 0 项` 后容易误解为分页第 0 页
- 结果：搜索框改为左侧搜索占位、右侧服务状态的单行布局；三张状态卡改为两行结构，第二行直接显示 `数值 + 语义`，不再渲染第三行副说明；新增/保留布局测试约束，确保搜索状态不横向重叠、状态卡文本不纵向重叠；确认当前标准商品页没有分页按钮，`共 0 项` 表示当前发现的商品数为空，标准商品上架链路为“持有准入标准商品 -> 选中商品 -> 存入 AVAILABLE -> 卖挂牌”
- 验证：`git diff --check` 通过；宿主机无 Java，`java -version` 为 `command not found`；当前会话普通 Docker 被 `/var/run/docker.sock` 权限阻止，sudo 被 `no new privileges` 阻止，因此未能执行 Docker Gradle 测试或部署

### 2026-06-22 - 标准商品市场页内标题与返回按钮清理

- 主题：继续收口 `MARKET_STANDARDIZED` 的实机像素预算，删除标准商品页内的重复标题、说明和返回按钮，让左侧浏览器与右侧交易仪表盘直接占满工作区
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：实机截图中 `商品浏览`、`商品详情`、`交易动作` 标题以及 `返回 MARKET` 按钮继续挤占标准商品页内部高度，导致商品摘要、盘口、库存状态和动作按钮仍有文本错位风险；返回/帮助/刷新应归属终端顶栏，而不是重复占用市场工作区
- 结果：标准商品页左侧浏览器移除内部标题和返回按钮，搜索/状态条直接贴近卡片顶部；右侧详情仪表盘移除内部标题和动作反馈标题，摘要/盘口/库存/动作区按剩余高度重新铺开；状态卡短标签改为 `库存`、`可领`、`订单`，降低窄卡片文字拥挤；测试新增约束，确保标准页左栏不再含按钮、右侧只保留 5 个交易动作按钮
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalShellPanelsScrollTest`、`TerminalMarketSectionContentTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `00ced52418da2b099bd87df5d243f3252139e28674ce76609421e95c454a3f51`，Lobby `latest.log` 到达 `Done (1.630s)!`

### 2026-06-22 - 标准商品市场像素预算修复：去标题占高与右侧卡片防重叠

- 主题：继续按效果图整改 `MARKET_STANDARDIZED`，优先解决标准商品页标题/说明占用工作区、右侧商品摘要/盘口/状态/动作区互相重叠的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：上一轮虽然改为二栏工作台，但标准页仍保留 `TerminalMarketSection` 标题/说明和 `TerminalMarketShell.computeWorkbenchLayout(...)` 的 30px section header 预留；右侧详情区还用固定行距和若干最小高度拼接，在实际 GUI scale 下会让商品摘要、订单簿、状态卡和动作按钮互相压住
- 结果：标准页隐藏自身标题/说明，workbench 从内容区顶部开始布局；右侧详情区改为按实际剩余高度分配摘要、订单簿、状态卡和动作区；订单簿行距按卡片高度压缩，状态卡高度不足时隐藏副标题，商品摘要图标和文字按卡片高度重新定位；未选商品时隐藏 6 个数字输入框，只保留底部动作按钮，减少空状态挤压
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalShellPanelsScrollTest`、`TerminalMarketSectionContentTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `35e61825bef46e9a208880544437e164d32b99b1f515b790e6a50c8cb28f4091`，Lobby `latest.log` 到达 `Done (1.548s)!`

### 2026-06-21 - 标准商品市场工作台整改：二栏浏览器与右侧仪表盘

- 主题：按效果图差异先集中整改 `MARKET_STANDARDIZED`，把标准商品市场从“左/中/右三块独立栏目”收口为“左商品浏览器 + 右交易仪表盘”的工作台结构
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：实机页虽然不再越界，但右侧动作栏仍像独立表单列，导致商品详情、行情、状态和交易按钮被挤碎；效果图中的标准市场应以左侧商品列表为唯一滚动区，右侧为无外部滚动的行情/资产/动作仪表盘
- 结果：`TerminalMarketShell.computeWorkbenchLayout(...)` 改为标准市场二栏布局；左侧浏览器保留搜索/过滤外观、列表滚动和返回入口；右侧详情卡内合并商品摘要、买卖盘/最新成交、资产状态卡与底部紧凑动作条；修复商品摘要右侧指标文字和名称区域的宽度分配，避免窄宽度下互相覆盖
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalShellPanelsScrollTest`、`TerminalMarketSectionContentTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `4724ecd36d9f9ffab9a388fa71fab66506bb1958d0a5d23c48f8f58897c8e1e1`，Lobby `latest.log` 到达 `Done (1.692s)!`

### 2026-06-21 - 记录市场终端效果图差异复盘

- 主题：把 `/home/u24/图片/市场.png` 效果图与当前四张实机市场页面的差异沉淀为独立文档，作为后续子页面重构的对照基准
- 影响范围：`docs/market-terminal-effect-gap-review-2026-06-21.md`、`docs/WORKLOG.md`
- 原因：当前 MARKET 总入口已接近方向，但标准商品、定制商品、汇率市场仍存在工作台结构、信息密度、真实物品图标、动作区边界和低价值说明文字等差距，需要先固定问题清单，避免继续零碎 patch
- 结果：新增独立差异复盘，按 MARKET、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 分别记录与效果图差距，并拆分 UI 层问题、可能后端字段缺口和后续执行顺序
- 验证：文档变更，无编译验证

### 2026-06-21 - 终端视觉系统三次收口：比例圆角、顶栏容器约束与低价值文案清理

- 主题：按实机截图继续修复终端公共壳层，解决圆角不自然、顶栏按钮超出 title bar、导航栏标签/图标可读性不足，以及 MARKET 入口存在低价值说明文案的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/framework/RoundedRectPainter.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeLayout.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalIconPainter.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContent.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreenLayoutTest.java`
- 原因：上一轮已有圆角和图标方向，但圆角只是固定 2px 切角，放大后不够自然；顶栏按钮使用固定最小尺寸，可能反过来超过 title bar 高度；左侧导航受配置和最大宽度限制仍偏窄；MARKET 入口页仍显示“列表每 30 秒自动刷新”和“先选市场类型”这类页面说明，信息价值低
- 结果：`RoundedRectPainter` 改为按控件短边乘固定 ratio 计算半径，并逐行绘制圆角实体/边框；顶栏按钮改为按父容器高度扣内边距后夹紧，从右向左布局，新增紧凑 title bar 子控件不越界测试；导航栏默认宽度和最小宽度上调，避免短标签裁剪；图标改为固定点阵模板再按控件尺寸缩放，替代临时线段拼图；MARKET 总入口 lead 改为规则摘要，移除 30 秒刷新页脚，帮助卡改为交易规则提示
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `5f9384e79529bd87c984bedc6f307f8b81a520ca0381c2c2c51a4644ed22c0d1`，Lobby `latest.log` 到达 `Done (1.460s)!`

### 2026-06-21 - 终端视觉系统二次收口：圆角、导航宽度与图标可读性

- 主题：根据实机截图修复终端壳层视觉问题，重点解决左侧导航图标成块、标签裁剪、顶栏按钮过大和面板方角问题
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/framework/RoundedRectPainter.java`、`src/main/java/com/jsirgalaxybase/client/gui/framework/TexturedCanvasPanel.java`、`src/main/java/com/jsirgalaxybase/client/gui/framework/ButtonPanel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeLayout.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalIconPainter.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalIconButtonPanel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketVisuals.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreenLayoutTest.java`
- 原因：上一轮深色主题和图标体系已经接近方向，但实机下导航栏仍过窄，短标签被裁剪；顶栏图标按钮按栏高放大，显得拥挤并接近越界；导航图标使用填充块绘制，小尺寸下会变成白色块；通用面板和按钮仍是硬直角，和效果图的圆角质感差距明显
- 结果：新增 `RoundedRectPainter`，将通用面板、按钮、图标按钮和市场卡片切换为像素伪圆角；导航栏默认宽度提升到可容纳图标 + 两字标签，新增回归测试防止退回窄栏；顶栏图标按钮缩小为紧凑规格，信号图标同步缩小；导航/顶栏图标改为细线像素绘制，减少白块感
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `965525fa6425aa00eb904e68c5bafca2fb479f576d6ca30e818b4fa4b0c6149f`，Lobby `latest.log` 到达 `Done (1.598s)!`

### 2026-06-21 - 终端视觉系统收口：深色主题、顶栏图标按钮与导航图标

- 主题：从市场单页修补上升到终端壳层视觉系统，按效果图方向统一配色、导航图标和右上角图标按钮
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/theme/TerminalThemeRegistry.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalIconKind.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalIconPainter.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalIconButtonPanel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`
- 原因：市场总入口已基本解决越界和重叠，但终端壳层仍是文字按钮和浅蓝灰面板，和效果图中的深色玻璃面板、图标导航、右上角帮助/刷新/返回/关闭图标差距明显；市场说明卡内部的“查看说明”按钮也会和说明文字抢空间
- 结果：新增像素图标绘制器和图标按钮组件；左侧导航改为图标 + 文本，支持首页、职业、公共、市场、传送、银行图标；顶栏右侧改为信号、刷新、帮助、返回、关闭图标按钮；市场说明卡去掉内部按钮，只保留摘要，详细说明统一走顶栏帮助；主题色改为更深的蓝黑面板、暗卡片和蓝色按钮体系
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `d499e4fb7b4f1958a9bb344726d443a79e2c383b8b90de5690a789bd329000b0`，Lobby `latest.log` 到达 `Done (1.684s)!`

### 2026-06-21 - 市场终端 Round 3 复盘修复：非列表区去滚动与固定工作台

- 主题：按实机反馈继续收口市场 UI，可变列表保留独立滚动，其他核心信息区必须在终端边界内固定显示
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalCustomMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalExchangeMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`
- 原因：上一轮虽然解决了明显出界，但总入口卡仍会文字/按钮抢空间，标准商品、定制商品和汇率市场的详情/动作区仍以滚动容器承载普通信息，导致玩家必须滚动才能看完非列表内容，不符合效果图的工作台式信息密度
- 结果：MARKET 入口卡图标和文本槽位压缩，避免 CTA 覆盖正文；标准商品页改为左商品浏览器滚动 + 中行情/仓储固定栏 + 右交易动作固定栏；定制市场改为左挂牌列表滚动 + 右商品详情/反馈/动作固定栏；汇率市场改为左兑换目标/参数 + 右汇率详情/执行固定栏；真实物品图标链路继续保留，无法解析时才退回绘制 badge
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `d4b3d8f698ff8b1876df9b360c743c7b334812fe22c0de0099b5ea930aa062a1`，Lobby `latest.log` 到达 `Done (1.357s)!`

### 2026-06-21 - 市场终端 Round 3 收口：真实物品图标链路

- 主题：继续收口市场终端视觉密度，补齐标准商品、定制挂牌、汇率报价中“只画色块、不显示真实物品”的核心缺口
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketVisuals.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalCustomMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalExchangeMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalCustomMarketSectionModel.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalCustomMarketSectionSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalCustomMarketSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSectionService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java`
- 原因：效果图的市场页以物品图标作为浏览与详情的主要视觉锚点；此前标准商品和定制市场虽然有 badge，但没有真正解析 `ItemStack`，定制 listing 行甚至没有把每行 item identity 传到客户端
- 结果：`TerminalMarketVisuals` 支持从 `modid:item:meta`、`modid:item@meta`、`modid:item#meta` 等 iconRef 渲染真实 MC 物品图标，失败时退回原有 badge；标准商品列表/详情使用 productKey 渲染；定制市场 snapshot/model/network 增加 active/selling/pending listing icon refs，列表和详情均尝试渲染真实物品；汇率报价 hero 使用当前手持物品 id 渲染输入资产，目标资产仍保留货币图标
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `7cb747129fc524ed231d10564940b5c63bc00fdc71729530d0b49e30582bc378`，Lobby `latest.log` 到达 `Done (1.545s)!`

### 2026-06-20 - 市场终端 Round 3：视觉密度与物品感增强

- 主题：继续对照市场效果图优化 MARKET 总入口、标准商品、定制商品、汇率市场的视觉表达，优先解决“栏目紧张、图标少、缺少物品感”的实机观感问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketVisuals.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalCustomMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalExchangeMarketSection.java`、`docs/WORKLOG.md`
- 原因：当前市场功能链路已经基本存在，但客户端 snapshot 尚未携带完整 `ItemStack` 图标字段，页面仍偏文字面板，和效果图中的物品浏览器、listing 卡、汇率卡、状态卡相比缺少视觉锚点
- 结果：新增 `TerminalMarketVisuals` 作为轻量市场视觉 helper，统一绘制市场图标、物品 badge、货币 badge 和状态点；MARKET 入口卡图标与文字/按钮边界重新排布；标准商品浏览器条目、商品摘要和状态卡加入物品/状态视觉锚点并放松行高；定制市场 listing 改成带物品 badge 的可选卡片，详情页加入商品 hero；汇率市场目标列表和报价详情加入货币/汇率视觉卡
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`

### 2026-06-20 - 市场终端 Round 2 复盘修复：仅裁剪网络尾部 padding，避免列表错位

- 主题：复盘 Round 2 代码后修复 viewmodel padding 过滤策略，避免 custom listing 与 exchange target 的并行数组在异常空字段下错位
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalCustomMarketSectionModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalExchangeMarketSectionModel.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContentTest.java`、`docs/WORKLOG.md`
- 原因：Round 2 初版为解决固定长度网络数组补空导致的假空行，采用了删除所有空字符串的 compact 策略；该策略能处理正常尾部 padding，但如果未来并行数组中间出现空字段，可能导致 `ids/lines` 或 `codes/labels` 下标错位
- 结果：过滤策略改为只裁剪末尾 padding，保留中间空字段的下标位置；新增测试覆盖“尾部 padding 被清理”和“中间空字段不触发错位”两种情况
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`、`TerminalExchangeQuoteViewTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已重新部署，runtime jar SHA256 为 `d0be046a9e13840b3898845d2045a40f36ec5341840e167e39b99510b2b55d2d`，Lobby `latest.log` 到达 `Done (1.433s)!`

### 2026-06-20 - 市场终端 Round 2：空态、数据映射与 disabled reason 收口

- 主题：执行市场终端子页面整改计划 Round 2，区分后端无数据和 UI 表达问题，先把空态、数据来源说明和动作禁用原因收清楚
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContent.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalCustomMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalExchangeMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalCustomMarketSectionModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalExchangeMarketSectionModel.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContentTest.java`、`docs/market-terminal-child-pages-defect-plan-2026-06-20.md`、`docs/WORKLOG.md`
- 原因：实机市场子页仍容易把“后端没有商品/listing/quote”表现成未完成 UI；同时 custom listing 网络数组会被固定长度 padding，客户端此前会把空字符串当成空列表行渲染，造成大面积假骨架；exchange 未选择标的、无手持、规则禁兑等状态也缺少清晰禁用原因
- 结果：标准商品市场新增空目录/未选商品原因推导，浏览器无商品时显示正式空态，交易动作提示改为说明“没有可交易标准商品/先选商品”；custom/exchange viewmodel 过滤网络 padding 空字符串，custom 详情追加不可执行原因并统计真实 listing 数，exchange quote 追加不可执行原因并处理无可选标的；文档记录了标准商品目录、行情、custom listing、exchange quote 的实际数据来源和后端缺口
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalServiceTest`、`TerminalMarketServiceTest`、`TerminalExchangeQuoteViewTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `fd9986a5ad90c156a11f7865bba192d4982b3a9e2bcf25ce87e9350a03f86418`，Lobby `latest.log` 到达 `Done (1.916s)!`

### 2026-06-20 - 市场终端 Round 1 复盘修复：紧凑边界与空 handler 防御

- 主题：复盘 Round 1 代码后修复已发现的确定性纰漏，重点补极小 GUI scale 边界和空 action handler 防御
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalCustomMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalExchangeMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：复盘发现 `TerminalCustomMarketSection` 的购买/下架/领取按钮 action 没有显式空 handler 防御；同时 custom/exchange 在极小逻辑尺寸下仍保留固定按钮宽度和堆叠最小高度，理论上可能导致按钮或子卡片越过父容器；`TerminalMarketShell` 也存在用 `Math.max(180/140)` 把布局高度撑出父容器的边界风险
- 结果：custom 动作按钮统一通过安全包装执行；custom/exchange 的堆叠高度、返回按钮宽度、底部动作按钮宽度和内部滚动区高度都改为按父容器实际尺寸收敛；`TerminalMarketShell` 不再用固定最小可用高度撑开市场布局；新增紧凑尺寸回归测试，覆盖 overview、standardized、custom、exchange 在小逻辑 bounds 下直接子控件不越界
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过：`TerminalShellPanelsScrollTest`、`TerminalMarketSectionContentTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已重新部署，runtime jar SHA256 为 `0619b87bd734b1f7854c085d56ec7d8e26de1faf2b72c8450cfc95cabecca70d`，Lobby `latest.log` 到达 `Done (1.581s)!`

### 2026-06-20 - 市场终端 Round 1：子页面结构重排与边界收口

- 主题：执行 `market-terminal-child-pages-defect-plan-2026-06-20.md` 的 Round 1，先修市场子页面结构与边界，不扩后端数据和制度模型
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalCustomMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalExchangeMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：MARKET 总入口虽然已经接近效果图方向，但三个子页面仍表现为通用面板拼接：标准商品市场是三块独立竖栏，定制市场和汇率市场的动作区容易漂浮或贴边，入口卡和共享状态也仍有文字/按钮重叠风险
- 结果：MARKET 总入口卡片内部改成固定图标/标题/摘要/状态/CTA 纵向关系，状态/帮助卡只显示可容纳内容；标准商品市场改成左商品浏览器 + 右侧上下组合工作台，右侧上方承接商品详情/盘口/状态，下方承接交易动作；定制商品市场收成左挂牌浏览器 + 右详情/动作；汇率市场收成左兑换参数 + 右报价详情/动作；自定义和汇率页面不再使用独立底部动作卡，按钮按右侧工作区宽度收进终端内部
- 验证：`git diff --check` 通过；按 Docker Gradle 路径执行定向测试通过：`TerminalShellPanelsScrollTest`、`TerminalMarketSectionContentTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest`；`scripts/deploy-jgb.sh --targets lobby,client` 已构建并部署，runtime jar SHA256 为 `5c004f2e74f8614c7c847bc722a91ff441873cbbc74532d905d05bb6f9ba94cf`，Lobby `latest.log` 到达 `Done (1.516s)!`

### 2026-06-20 - 记录市场终端子页面缺陷与三轮整改计划

- 主题：把当前实机截图中已经确认的市场子页面缺陷、后端数据缺口和后续三轮整改计划沉淀为正式文档
- 影响范围：`docs/market-terminal-child-pages-defect-plan-2026-06-20.md`、`docs/WORKLOG.md`
- 原因：MARKET 总入口已经比早期说明页明显改善，但 `MARKET_STANDARDIZED`、`MARKET_CUSTOM`、`MARKET_EXCHANGE` 仍然存在结构不像效果图、内容/按钮越界或贴边、空数据状态不专业、后端数据缺口与 UI 问题混在一起的问题；如果继续零散 patch，容易反复在截图反馈中修局部而不收敛
- 结果：新增缺陷与计划文档，明确当前问题分为总入口重叠风险、标准商品市场工作台结构错误、定制商品市场 listing-first 表达不足、汇率市场 quote-first 表达不足，以及标准商品目录/行情、定制挂牌、汇率报价等数据缺口；后续按三轮推进：Round 1 子页面结构重排与边界收口，Round 2 空态/数据映射/后端缺口识别，Round 3 视觉密度/交互动作/统一验收
- 验证：本轮为产品与缺陷文档收口，无代码与测试变更

### 2026-06-20 - 市场终端布局修正：GUI scale 窄屏误判与子市场外溢

- 主题：继续对照市场效果图修正实机布局，优先解决“明明窗口很宽但市场页仍竖排/外溢”的结构问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalCustomMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalExchangeMarketSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：GTNH GUI scale 会把实际屏幕宽度换算成更小的逻辑宽度，原先 `500/520` 的堆叠阈值会把大窗口误判成窄屏，导致 MARKET 总入口三卡变成纵向大条、标准商品工作台退回上下/拥挤结构；同时 `MARKET_CUSTOM` 和 `MARKET_EXCHANGE` 还被 `TerminalShellPanels` 外层滚动二次包裹，局部页面高度容易超出终端边界
- 结果：市场专用布局的堆叠阈值下调到更保守的逻辑宽度；标准商品页三栏宽度比例收紧，避免右侧动作栏挤压中栏；定制商品和汇率市场不再被外层 `VerticalScrollPanel` 包裹，改为和标准商品页一样由页面内部管理滚动；补充布局测试，覆盖 460 逻辑宽度下 MARKET 总入口仍保持三入口同排，以及 custom/exchange 不出现外层滚动壳
- 验证：`git diff --check` 通过；当前 Codex 沙箱无法访问 Docker socket，宿主机也没有 Java，因此本轮未能执行 Gradle/JUnit 与部署，需在允许 Docker 的环境下重跑 `TerminalShellPanelsScrollTest` 后再灰度

### 2026-06-15 - 市场终端重构：总入口改三主卡，标准商品页改专用工作台

- 主题：把 `MARKET` 和 `MARKET_STANDARDIZED` 从 generic section 结构真正拉回到效果图方向，不再继续做文字段落和卡片堆叠式微调
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：上一轮虽然已经把市场页和标准商品页从语义上分开，但实际实机仍然表现为“总入口纵向堆块”和“标准商品页上下大框”；问题本质不是文案，而是顶层布局骨架仍然在沿用通用终端 section
- 结果：`MARKET` 根页现改成“三张主入口卡 + 下方状态卡/帮助卡 + 底部刷新脚注”的专用总入口；`MARKET_STANDARDIZED` 现改成三栏工作台：左栏商品浏览，中栏商品摘要/订单簿/状态卡，右栏交易动作块与最近反馈，局部滚动只保留在各自工作区内部；同时 `TerminalMarketShell` 重算了市场页的总入口和标准商品页主布局比例
- 验证：`git diff --check` 通过；宿主机无 Java，按仓库 Docker Gradle 路径执行定向测试：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest` 通过

### 2026-06-15 - 市场终端结构收口：市场共享骨架 + 标准商品工作台

- 主题：不再继续做 MARKET 零碎 UI patch，而是把 Phase 1 和 Phase 2 收口成可复用的市场页骨架，并让 `MARKET_STANDARDIZED` 真正落在这套骨架上
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketShell.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContent.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContentTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：当前 `MARKET` 根页和 `MARKET_STANDARDIZED` 虽然都能用，但仍然过于依赖 generic terminal section 表达；缺的不是一个按钮，而是市场页自己的顶栏上下文、入口布局和工作台布局抽象
- 结果：新增轻量 `TerminalMarketShell` 作为市场共享骨架，统一承载市场页顶栏上下文、MARKET 根页入口布局和标准商品页三栏工作台布局；`MARKET` 根页从“四卡说明区”收口成“一条共享状态 + 三张主入口卡 + 帮助按钮”；`MARKET_STANDARDIZED` 继续保持正式三栏，但标题、卡片职责和滚动边界都改成更明确的交易工作台语义；全程未改服务端字段、业务链或 terminal 主路由
- 验证：`git diff --check` 通过；宿主机无 Java，按仓库 Docker Gradle 路径执行定向测试：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest` 通过

### 2026-06-14 - 市场终端 Phase 2：标准商品市场工作台收口

- 主题：把 `MARKET_STANDARDIZED` 从“已有功能页”继续收口成标准商品市场正式工作台，不扩后端业务模型
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContent.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContentTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionStateTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：当前标准商品市场已经具备真实动作主链，但页面仍偏“功能集合页”，商品浏览、详情层级、动作 enable 条件和最近反馈没有完全收口成顺手的玩家工作流
- 结果：标准商品市场现已按三栏工作台进一步收口：左栏改成更明确的商品目录浏览器并弱化说明字段；中栏按“当前商品 -> 核心行情 -> 个人状态 -> 我的订单 -> CLAIMABLE -> 规则提示”重排；右栏改成交易操作台，分别为存入、限价买、限价卖、即时买、即时卖提供更明确的预览与输入引导，同时继续复用现有 popup 确认链与 snapshot 回写；市场页帮助 popup 也补上标准商品页专属说明
- 验证：`git diff --check` 通过；宿主机无 Java，按仓库 Docker Gradle 路径执行定向测试：`TerminalMarketSectionContentTest`、`TerminalMarketSectionStateTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalMarketActionMessageFactoryTest` 通过

### 2026-06-14 - 市场终端 Phase 1：MARKET 总入口与共享壳收口

- 主题：把 `MARKET` 根页从说明型混排 section 收口成真正的三市场入口页，并顺手固定市场共享壳层的首轮约定
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContent.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContentTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：当前 `MARKET` 根页虽然已有三类市场路由，但 UI 仍更像“说明页 + 字段堆叠”，玩家首屏看到的是解释文本而不是市场分流入口，同时顶栏帮助/刷新和页面主体之间也缺少市场专属约束
- 结果：`MARKET` 根页已改成“共享状态条 + 帮助入口 + 标准/定制/汇率三张入口卡”的专用入口布局，不再在根页混排交易正文；市场页顶栏现对 `MARKET` / `MARKET_STANDARDIZED` / `MARKET_CUSTOM` / `MARKET_EXCHANGE` 统一使用更紧凑的市场上下文文案，并把刷新提升到顶栏小按钮；帮助说明改为通过共享 `帮助` 按钮和市场页 popup 承接，明确 `MARKET` 根页只负责分流与轻量状态，工作页正文继续留给各自子市场
- 验证：`git diff --check` 通过；宿主机无 Java，按仓库 Docker Gradle 路径执行定向测试：`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalHomeScreenModelTest`、`TerminalMarketActionMessageFactoryTest` 通过

### 2026-06-14 - 新增市场终端分阶段执行计划

- 主题：把当前已确认的市场终端方向收口成一份正式阶段计划，便于后续持续开发、AI 交接和验收追溯
- 影响范围：`docs/market-terminal-phased-execution-plan-2026-06-14.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前已经明确 MARKET 根页不能再做成说明页，标准商品市场、定制商品市场、汇率市场也不应按统一大桶继续演化；如果不先钉执行顺序，后续很容易在“是不是一页一个阶段”上反复摇摆
- 结果：新增一份正式计划文档，明确采用 `Phase 1 共享市场壳与 MARKET 总入口 -> Phase 2 标准商品市场 -> Phase 3 定制商品市场 -> Phase 4 汇率市场 -> Phase 5 统一收口` 的顺序，并把每阶段的目标、范围、非目标、产出物和验收标准写清
- 验证：本轮为产品文档收口，无代码与测试变更

### 2026-06-14 - 新增市场仓库 v1 制度边界草案

- 主题：在市场终端概念方向基本确认后，先把“仓库到底是什么”从模糊的页面想象，收口成正式制度边界草案
- 影响范围：`docs/market-warehouse-v1-product-boundary-draft.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前市场系统已经在向服务器核心玩法内核演化；如果不先定义仓库边界，后续很容易把市场托管仓、企业私仓、公共仓和普通大箱子 GUI 混成一团，最终反过来破坏三市场与银行结算的制度模型
- 结果：新增 `市场仓库 v1 产品边界草案`，明确仓库首先是资产状态系统，v1 只优先落标准商品市场托管仓；同时把现实模型拆成 `市场托管仓 / 企业私有仓 / 公共仓` 三类，并强调企业仓与公共仓可以和市场发生流转，但不应直接与市场托管仓混成同一个库存概念
- 验证：本轮为产品文档收口，无代码与测试变更

### 2026-06-14 - 标准商品市场终端窄修复：银行快照回写顺序与即时成交残单语义

- 主题：对“标准商品市场终端收口”后的两个一致性问题做窄修复，不扩市场功能、不改 ServerTools
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketServiceTest.java`、`docs/WORKLOG.md`
- 原因：一是标准商品市场动作完成后，终端快照仍可能先读取旧银行页摘要，导致余额显示落后于本次资金变动；二是即时买入/卖出部分成交后，如果“剩余自动撤回”失败，终端会把整次操作误报成笼统失败，掩盖“已真实部分成交且残单可能仍然挂着”的事实
- 结果：`TerminalService.buildTerminalSnapshot(...)` 现已先执行 market action 再生成 bank snapshot，保证市场动作后的银行摘要读取的是最新状态；`TerminalMarketService` 现把即时买入/卖出的“部分成交但残余撤回失败”单独转成 warning 反馈，明确展示已成交数量、`orderId`、剩余未撤数量、可能仍处于开放/托管状态以及原始撤回失败原因，引导玩家去“我的订单”继续处理
- 验证：`git diff --check` 通过；宿主机 `java` 不可用，按仓库已验证 Docker Gradle 路径执行定向测试：`TerminalServiceTest`、`TerminalMarketServiceTest`、`StandardizedSpotMarketServiceTest` 通过
- 备注：首次 Docker Gradle 运行命中过往进程持有的 workspace `.gradle` 锁；随后改用仓库脚本同款挂载参数（独立 `GRADLE_USER_HOME` + `--project-cache-dir` + `--no-daemon`）重跑通过

### 2026-06-14 - ServerTools 传送页按最终效果图方向做一轮产品化收口

- 主题：在传送链已验证可用后，继续按最终效果图方向把 `SERVER_TOOLS` 页面收口到更接近成品的状态，不扩后端动作范围
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`docs/WORKLOG.md`
- 原因：当前页面主体结构已经稳定，但仍存在两类产品缺口：一是文档要求中的“已知服务器目录”在实际 UI 中不可见；二是“最近传送状态”只突出单条结果，没有把已有 recent ticket 列表转成可读历史摘要
- 结果：左侧传送点卡片顶部新增紧凑服务器目录摘要，明确当前服与已知目录；右侧最近状态卡新增历史条数与紧凑历史摘要；中栏布局随之调整但仍保持列表/详情两块都在终端内滚动；本轮继续保持 warp-only 终端范围，不把 home/back/spawn/rtp/tpa 扩进页面
- 验证：`git diff --check` 通过；`./scripts/build-mod.sh --task assemble` 通过；Docker Gradle 定向测试 `TerminalServiceTest`、`TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest` 通过

### 2026-06-13 - 修复 ServerTools 传送页内容越过终端边界

- 主题：修复 `SERVER_TOOLS` 三段式传送页中列表栏和右侧详情栏越过终端底边的问题，并恢复页面内部滚动
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`docs/terminal-servertools-overflow-bug-2026-06-13.md`、`docs/WORKLOG.md`
- 原因：上一轮为了压缩视觉结构，把右侧详情区从滚动 viewport 改成了直接堆叠，同时 `setBounds` 仍用固定最小高度撑开 section；在实际 Minecraft GUI 高度不足时，中栏和右栏会突破父容器底边，按钮和状态卡被挤到终端外
- 结果：`SERVER_TOOLS` 外层布局只使用父容器真实高度，不再用最小高度撑开；中栏传送点列表继续保留独立 `VerticalScrollPanel`，底部自动刷新提示按剩余高度参与布局；右侧详情、最近状态、反馈、风险提示和确认按钮重新进入同一个内部 `VerticalScrollPanel`，内容多时在终端内部滚轮浏览而不是向外溢出
- 验证：`./scripts/build-mod.sh --task assemble` 通过；`scripts/deploy-gray-chain.sh --jar build/libs/jsirgalaxybase-ed7e2cf-main+ed7e2cfb16-dirty.jar --targets lobby,client` 已部署，jar SHA256 `d1486cc48da02a260d6f75db00bc8a56a8e5f6c19582619007e494ff54d2b365`；Lobby 日志到达 `Done (1.424s)!`

### 2026-06-09 - 修正 ServerTools 页被误判窄屏并补右侧工作台滚动

- 主题：针对实际游戏截图中 `SERVER_TOOLS` 仍退回“上列表 / 下详情”的两段堆叠问题，继续收口传送页布局判定和内部滚动
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：虽然上一轮已经把页面朝效果图重组，但 Minecraft GUI 坐标下的宽度阈值仍然过高，导致常见 GUI scale 下被误判成窄屏并回退到上下堆叠；同时右侧工作台没有独立滚动容器，长内容只能把页面往下顶
- 结果：`SERVER_TOOLS` 窄屏阈值已明显下调，常见宽度下优先保持“左导航 + 中列表 + 右工作台”的三段结构；右侧工作台改成独立 `VerticalScrollPanel`，超长详情/状态可直接滚轮浏览；顶栏对 `SERVER_TOOLS` 也不再拼接长串 detail，只保留更紧凑的传送路径上下文
- 验证：`git diff --check` 通过；Docker Gradle 定向测试 `TerminalShellPanelsScrollTest`、`TerminalHomeScreenLayoutTest`、`TerminalServiceTest` 通过

### 2026-06-09 - 开始把 ServerTools 传送页收口成成品化工作台

- 主题：把 `SERVER_TOOLS` 页面从“后端已通但仍偏工程拼装”的 v1 形态，推进成更接近目标效果图的专用传送工作台
- 影响范围：`docs/terminal-ui-redesign-toward-concept-mockup-2026-06-07.md`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`scripts/lib/deploy-common.sh`
- 原因：当前 `warp` 主链和终端确认链已可用，但与效果图相比，主要差距已不在后端，而在于右侧仍像三张平行卡片、中栏列表还不够产品化、页面缺少图标化层级锚点
- 结果：文档已明确新增两条判断：
  - 右侧应收口为一个连续工作区，而不是 `detail/recent/action` 三张并列卡
  - 现阶段应优先采用轻量绘制的图标化表达，不等待新的素材或纹理资源链
  同时代码开始按该方向重做 `TerminalServerToolsSection`，保留现有 warp 后端调用链，仅重组页面工作区与视觉层级；部署脚本实测时出现 `lobby` 已经 `Done` 但仍被误判失败，因此把 gray-chain 的 `RUNNING` / `Done` 等待窗口进一步放宽，减少 GTNH 慢启动时的假失败

### 2026-06-08 - 终端改成顶部锚定并强制现网比例回落到更小值

- 主题：继续收口终端共享壳层，解决“左栏与内容区缝隙仍大”和“实际游戏里终端看起来仍接近满屏”两个直接可见问题
- 影响范围：`src/main/java/com/jsirgalaxybase/config/ModConfiguration.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeLayout.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalLayoutMetrics.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalHomeSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreenLayoutTest.java`、`docs/WORKLOG.md`
- 原因：虽然上一版已经删除普通页底栏并压缩了字号，但客户端与灰链配置文件仍保留旧的 `0.88 / 0.72 / 0.12`，叠加布局仍在用“伪居中”算法，导致用户实际看到的终端尺寸和位置几乎没有肉眼差异
- 结果：终端默认比例进一步下调到 `width=0.72 / height=0.50 / nav=0.07`；`TerminalHomeLayout` 改成显式顶部锚定，并同步降低最大宽高上限、内边距、导航最大宽度和正文区 gap；左栏继续去外框，仅保留与内容区之间的细竖线；终端共享文本、按钮和 section 内边距再缩一档；部署后直接把客户端、Lobby、S2 的现网 `jsirgalaxybase.cfg` 同步写成新的更小比例，避免旧配置继续覆盖视觉结果
- 验证：`git diff --check` 通过；Docker Gradle 定向测试 `TerminalHomeScreenLayoutTest`、`TerminalShellPanelsScrollTest`、`TerminalServiceTest` 通过；`scripts/deploy-jgb.sh --targets lobby,s2,client` 已完成构建与部署，最新运行 jar 哈希 `2c72f7f3750fb5392483e64f36dba9d58e73941b128d562cc4beaeffffad144f`

### 2026-06-08 - 终端壳层改为常显左栏与上方 75% 工作台

- 主题：把终端壳层从“居中弹窗 + hover 导航”收口到更接近目标图的“常显左栏 + 紧凑顶栏 + 上方固定工作台”，并继续压缩 `SERVER_TOOLS` 页的内容竞争
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeLayout.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreenLayoutTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`docs/WORKLOG.md`
- 原因：实际游戏内终端仍明显偏离目标图，核心问题不再是单页 spacing，而是壳层仍保留 hover 导航和居中弹窗比例，导致下方物品栏不可稳定保留、导航形态不对、`传送` 页主体空间不够完整
- 结果：`TerminalHomeLayout` 现改为更接近屏幕 75% 高度的上方锚定布局，并显式预留底部热键栏可见空间；导航轨默认常显，不再依赖 `EdgeRevealNavigationPanel`；顶栏压缩为更紧凑的单行状态带；`SERVER_TOOLS` 页取消底部 footer 竞争，把空间让给左侧传送点列表和右侧详情/状态/确认区，同时把列表改成单一主卡片并补上底部自动刷新提示
- 验证：`git diff --check` 通过；Docker Gradle 定向测试 `TerminalHomeScreenLayoutTest`、`TerminalShellPanelsScrollTest`、`TerminalServiceTest` 通过；`assemble` 首次尝试被远端 TLS handshake 中断，重试后成功

### 2026-06-08 - 增加灰链与客户端一键部署脚本

- 主题：把反复手工执行的 `build -> 替换 jar -> 重启 lobby/s2 -> 校验 hash/状态` 收口为可重复脚本，减少后续终端与 ServerTools 开发中的部署噪音
- 影响范围：`scripts/lib/deploy-common.sh`、`scripts/build-mod.sh`、`scripts/deploy-gray-chain.sh`、`scripts/deploy-jgb.sh`、`docs/WORKLOG.md`
- 原因：当前 `JsirGalaxyBase` 的验证节奏高度依赖灰链 `Lobby/S2` 和 Prism 客户端实例；如果每次都靠人工逐步部署，不仅重复，而且容易漏掉 hash 一致性、日志 `Done` 校验和旧 jar 清理
- 结果：新增三层脚本链：
  - `scripts/build-mod.sh`：用已验证的 Docker Gradle 路径构建运行 jar
  - `scripts/deploy-gray-chain.sh`：备份并清理旧 `jsirgalaxybase*.jar`，替换 `lobby/s2/client`，重启灰链服，校验 `RUNNING`、`Done` 和哈希一致性
  - `scripts/deploy-jgb.sh`：总控脚本，串联 build/deploy，并支持可选 `--launch-client`
- 验证：`bash -n` 通过；`--dry-run` 串联通过；`scripts/deploy-jgb.sh --skip-build --targets lobby,s2,client` 已真实运行成功，完成 `lobby/s2/client` 同步与哈希校验，不触碰 `S1`

### 2026-06-08 - 终端继续压缩到更接近效果图的密度

- 主题：继续收口终端壳层，把“物品栏可见、左栏更窄、字号更小、普通页无底栏”落成共享终端行为
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/framework/LabelPanel.java`、`src/main/java/com/jsirgalaxybase/client/gui/framework/ButtonPanel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalLayoutMetrics.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeLayout.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanelsScrollTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreenLayoutTest.java`、`docs/WORKLOG.md`
- 原因：上一轮虽然已经改成常显左栏和上方工作台，但实际游戏内仍然偏大；普通页底部 `刷新分区 / 关闭终端` 仍然浪费空间，导航与文字密度也还明显高于目标效果图
- 结果：普通页底部 footer 已删除；壳层比例、上方锚定和左侧导航宽度进一步收紧；终端共享 `LabelPanel` 与 `ButtonPanel` 增加缩放能力，并由 `TerminalPanelFactory` 默认启用更小的终端字号；`TerminalLayoutMetrics` 同步下调行高、按钮高和内边距，使同样区域内可容纳更多内容
- 验证：`git diff --check` 通过；Docker Gradle 定向测试 `TerminalHomeScreenLayoutTest`、`TerminalShellPanelsScrollTest`、`TerminalServiceTest` 通过；`scripts/deploy-jgb.sh --targets lobby,s2,client` 已真实构建并完成新 jar 部署

### 2026-06-08 - 终端壳层高度和传送页栏宽继续整改

- 主题：针对实际游戏内仍然偏满屏的问题，继续压低终端整体高度、扩大底部热键栏预留，并收窄 `SERVER_TOOLS` 传送页中栏
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeLayout.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`src/test/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreenLayoutTest.java`、`docs/WORKLOG.md`
- 原因：前一版虽然已经删除底部 footer 和缩小字号，但当前布局高度比例仍然偏大，`SERVER_TOOLS` 中栏也占用过多横向空间，右侧详情区没有充分接近目标图
- 结果：默认终端高度比例从偏大的 0.79 级别压到 0.68，并提高底部预留空间；导航轨最大宽度进一步收紧；传送页中栏从约 38% 收到约 34%，卡片 gap、padding、传送点行高和右侧详情/状态/action 分区高度全部压缩，给右侧内容更多横向空间
- 验证：`git diff --check` 通过；Docker Gradle 定向测试 `TerminalHomeScreenLayoutTest`、`TerminalShellPanelsScrollTest`、`TerminalServiceTest` 通过；`scripts/deploy-jgb.sh --targets lobby,s2,client` 已真实构建并部署，jar 哈希 `82191c0dea739f2002d7170ffb963cb8db90772909482a8ea6b7845991c791f3`

### 2026-06-07 - 记录传送页与效果图逐项对比后的产品结论更新

- 主题：把 `传送 / 群组服` 当前运行页面与目标效果图做逐项对比，并把“问题到底是什么”从模糊的 UI 不满意收口成可执行的产品结论
- 影响范围：`docs/terminal-ui-redesign-toward-concept-mockup-2026-06-07.md`、`docs/terminal-servertools-page-v1-acceptance-review-2026-06-06.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前页面虽然已经接通真实 warp 后端，也已经能在终端中进入 `传送` 页，但和目标图相比，核心差距并不是 hover 行为或单个 spacing，而是页面仍沿用 generic section/scroll 模型，导致信息架构、主次动作和内容分区都不对
- 结果：文档中已明确记录六类关键差距：`信息架构不对`、`导航形态不对`、`warp 列表不是稳定中栏`、`右栏详情不是结构化面板`、`主次动作关系错误`、`整体仍像系统说明页而非玩家工具页`；同时把后续方向明确改成“保持后端链不变，但把 `传送 / 群组服` 重做成常驻窄导航 + 中栏列表 + 右栏详情/状态/确认按钮的专用传送工具页”，不再把下一轮定义成单纯 hover 或密度微调

### 2026-06-07 - 记录终端朝效果图方向的整体 UI 改造设计与阶段计划

- 主题：在 `Lobby <-> S2` 的真实 warp 链路已经验证通过后，把当前终端工作的主阻塞从“传送是否可用”正式切换为“界面是否好用”，并沉淀一份面向效果图方向的整体设计与实施计划
- 影响范围：`docs/terminal-ui-redesign-toward-concept-mockup-2026-06-07.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前 BetterQuesting 风格终端在功能迁移上已经覆盖银行、市场、群组服传送等正式页面，但实际游戏内使用仍然存在导航过重、顶栏浪费高度、传送页信息层级混杂等问题；需要先确定“继续坚持当前游戏内 GUI 路线，但重做交互结构”的产品决策，再开始阶段性实施
- 结果：新增一份正式设计文档，明确不回退到纯 web 路线、不重开 BetterQuesting 风格技术选型，而是把终端重做拆成四段：`Phase 1 导航壳重做`、`Phase 2 顶栏与全局状态重做`、`Phase 3 传送页专项重做`、`Phase 4 肉眼观察收口`；其中当前后端基线已确认为 `Lobby <-> S2` warp 主链可用，下一轮执行应从导航壳开始

### 2026-06-06 - 验收 ServerTools 群组服终端页 v1 并产出收口 Prompt

- 主题：回顾当前工作树中已出现的 `SERVER_TOOLS` 终端页面实现，判断它是否已经接入真实 ServerTools warp 后端，并把剩余缺口收口成下一轮执行 prompt
- 影响范围：`docs/terminal-servertools-page-v1-acceptance-review-2026-06-06.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前方向已经确认要推进群组服工具和市场产品化；检查代码时发现 ServerTools 终端页并非缺失，而是已有 v1 实现，需要避免后续开发 AI 从零重写
- 结果：确认 `TerminalPage.SERVER_TOOLS`、`SERVER_TOOLS_REFRESH / SELECT_WARP / CONFIRM_WARP`、snapshot / payload / viewmodel / network / client section 已接入；默认服务端 facade 会读取 `ServerToolsModule`、warp 列表和 server directory，确认 warp 复用 `PlayerTeleportService.prepareWarpTeleport(...)` 与 `ServerToolsModule.dispatchTeleport(...)`；剩余缺口收口为 recent transfer ticket 状态展示、默认 facade 后端调用测试、游戏内鼠标浏览验证和 UI 密度复测
- 验证：本轮前半段测试曾被数据盘满和 Gradle 缓存锁阻塞；清理环境后，`compileTestJava` 成功，`TerminalServiceTest`、`TerminalHomeScreenModelTest`、`TerminalPageTest`、`ClusterTeleportServiceTest`、`PlayerArrivalRestoreServiceTest`、`TerminalHomeScreenLayoutTest`、`TerminalShellPanelsScrollTest` 均通过；临时 `galaxy-dev-run` 测试容器已清理，未触碰正式游戏服务器进程

### 2026-06-06 - 收口 ServerTools 终端页 recent transfer 状态与默认 facade 测试

- 主题：把群组服终端页 v1 验收中确认的两项代码缺口直接补齐，不重写现有页面结构
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/cluster/port/TeleportTicketRepository.java`、`src/main/java/com/jsirgalaxybase/modules/cluster/infrastructure/jdbc/JdbcTeleportTicketRepository.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalServerToolsSectionSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalServerToolsSectionModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/PlayerArrivalRestoreServiceTest.java`、`docs/terminal-servertools-page-v1-acceptance-review-2026-06-06.md`
- 原因：`SERVER_TOOLS` 页面已接入真实 warp 主链，但此前还缺少最近传送票据状态展示，以及默认 facade 是否真正走 `prepareWarpTeleport(...)` / `dispatchTeleport(...)` 的可测证明
- 结果：新增 `findRecentForPlayer(...)` 仓储入口并接入 JDBC；ServerTools terminal snapshot 现在可显示最近 transfer ticket 状态行；`TerminalService` 增加 runtime bridge seam，默认 facade 的本服完成、跨服派发、runtime 不可用和后端异常路径均已补单测证明；当前剩余收口仅为游戏内点击验收与必要的局部密度微调

### 2026-05-18 - 记录当前产品方向、进度判断与剩余需求清单

- 主题：把当前共识从“继续单点 UI 打磨”收口为“终端作为全面智能控制台”，并明确下一阶段优先推进群组服工具页面和市场功能/页面产品化
- 影响范围：`docs/current-product-direction-and-gap-review-2026-05-18.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：终端新壳可用性已经明显改善，ServerTools 跨服 warp 已完成真实 Lobby <-> S2 验证，后续重点需要转向把 ServerTools 和三类市场变成终端中的正式玩家工作流
- 结果：新增当前方向与缺口评审文档，明确 terminal foundation、ServerTools/cluster、market 三条线的已完成进度、剩余需求、推荐执行阶段和需要用户确认的产品决策；同时标注旧 Phase 7 交接文档与 2026-05-17 灰度状态记录中的过期状态点

### 2026-05-18 - 完成 ServerTools 跨服 warp 真实灰度验证

- 主题：用游戏内 `/jgbst warp ...` 对 Lobby 与 S2 之间的跨服传送闭环做真实验证，并收口超时问题
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`、Velocity gray chain 配置、`docs/servertools-phase3-gray-rollout-status-2026-05-17.md`、`docs/WORKLOG.md`
- 原因：此前灰度链已启动，但还未实际验证玩家级跨服传送；第一次实测暴露出 transfer ticket TTL 与代理 read timeout 对 GTNH 切服耗时不够宽容
- 结果：`/jgbst warp list` 可列出系统 warp；`/jgbst warp s2test` 完成 Lobby 到 S2；`/jgbst warp lobbytest` 完成 S2 回 Lobby；相关 `cluster_transfer_ticket` 进入 `COMPLETED`；后续 ServerTools 可进入终端页面产品化阶段

### 2026-05-17 - 增加 servertools 命名空间 warp 入口

- 主题：为 ServerTools 增加不与 GTNH 整合包裸 `/warp` 冲突的命名空间入口，优先打通跨服 warp 灰度测试
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/servertools/command/`、`src/main/java/com/jsirgalaxybase/modules/servertools/ServerToolsModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/test/java/com/jsirgalaxybase/modules/servertools/command/JgbServerToolsCommandTest.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/servertools-phase1-command-reference.md`、`docs/WORKLOG.md`
- 原因：灰度实测 `/warp s2test` 与 `/warp lobbytest` 被整合包内其他模组接走，未进入 JsirGalaxyBase 的 `WarpCommand`，导致 `cluster_transfer_ticket` 没有新增记录
- 结果：新增 `/jgbst warp list`、`/jgbst warp <name>`，别名保留 `/jst` 与 `/jsirst`；主命令新增 `/jsirgalaxybase servertools warp ...` 与 `/jsirgalaxybase st warp ...`；裸 `/warp` 仍保留兼容注册，但文档改为以命名空间入口作为验收命令；新入口统一转入 `ServerToolsCommandHandler`，复用 `PlayerTeleportService.prepareWarpTeleport` 与现有 cluster dispatch，不复制传送业务逻辑
- 验证：本机无 Java/JDK，容器内 `compileJava` 在拉取 `ModularUI2:2.3.45-1.7.10` 时因远端 TLS handshake 被断开，未进入 Java 编译阶段；已补最小命令路由测试与手动游戏内/SQL 验证步骤，后续可在依赖缓存可用时重跑 `docker compose -f /media/u24/data/gtnh/docker/projects/docker-compose.yml run --rm -e GRADLE_USER_HOME=/tmp/gradle-home galaxy-dev ./gradlew compileJava --no-configuration-cache -PforceToolchainVersion=17`

### 2026-05-17 - 完成 servertools / cluster Phase 3 灰度链部署准备

- 主题：按第三阶段灰度联调目标，把 Entrance / Lobby / S2 准备到可启动、可观察、可继续做真实跨服联调的状态，同时不触碰在线 S1
- 影响范围：`docs/servertools-phase3-gray-rollout-status-2026-05-17.md`、`docs/README.md`、`docs/WORKLOG.md`、`/media/u24/data/gtnh/data/Galaxy_GTNH_Lobby/**`、`/media/u24/data/gtnh/data/Galaxy_GTNH284_S2/**`、`/media/u24/data/gtnh/docker/projects/.env`、`galaxy-base` PostgreSQL schema、`galaxy-gtnh` 容器内 supervisor 当前配置
- 原因：原 phase 3 prompt 仍按 MCSM 路径描述，但当前机器真实运行链已经是 Docker + supervisor；灰度链此前已有旧 jar 和配置残留，但未真正启动，数据库也没有基础 schema，会阻塞 Lobby / S2 进入 JsirGalaxyBase banking / market / cluster runtime
- 结果：确认当前运行目录为 `/media/u24/data/gtnh/data` 并挂载到容器 `/gtnh/GroupServer`；将当前 `build/libs/jsirgalaxybase-ed7e2cf.jar` 部署到 Lobby 与 S2，旧 `7545ce9` jar 移入各自 `mods_disabled`；用项目 DDL 初始化空库后再执行既有 `scripts/db-migrate.sh` 完成全部版本化迁移；把 PostgreSQL 角色密码对齐到既有 server cfg；在不重启 S1 的前提下通过 supervisor 启动 Entrance、Lobby、S2，并把持久 `.env` 同步为灰度链启用状态；最终 Entrance 监听 `25566`，Lobby 监听 `25564`，S2 监听 `25567`，Lobby / S2 日志均出现 JsirGalaxyBase banking、market、cluster runtime prepared 和 Minecraft `Done`
- 验证：`docker compose ... galaxy-dev ./gradlew assemble --no-configuration-cache -PforceToolchainVersion=17` 成功；部署 jar 与构建 jar SHA256 均为 `02dcd79439cb7e8fa7896299d047d637e3d9dd3bddd7d7b54fdff8beca98e065`；host TCP probes 到 `127.0.0.1:25564` / `25566` / `25567` 成功；`galaxy-gtnh` supervisor 中 S1 uptime 未重置且 S1 的 JsirGalaxyBase 文件未改动

### 2026-04-10 - 把 ServerUtilities 的后续整合边界写入现有架构文档

- 主题：把 `Reference/ServerUtilities` 作为参考源码来源时的整合边界正式写回现有架构文档，明确后续服务器工具能力只能按 cluster / server tools 子系统拆解吸收，不能整包并入
- 影响范围：`README.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：银行与市场主链已经基本成型，后续跨服传送、服务器状态、home 与其他服务器工具能力需要进入正式架构讨论阶段；如果不先写清楚整合规则，后面很容易把 `ServerUtilities` 的整套历史结构直接搬进当前仓库，破坏现有模块化单体边界
- 引用来源：`Reference/ServerUtilities/README.md`、`Reference/ServerUtilities/src/main/java/serverutils/ServerUtilities.java`、`Reference/ServerUtilities/src/main/java/serverutils/data/ServerUtilitiesPlayerData.java`、`Reference/ServerUtilities/src/main/java/serverutils/data/ServerUtilitiesUniverseData.java`、`Reference/ServerUtilities/src/main/java/serverutils/net/ServerUtilitiesNetHandler.java`
- 结果：根架构文档现已明确 `modules.cluster` / `modules.servertools` 的推荐落点，以及 `GatewayAdapter`、`ClusterStateRepository`、`PlayerSnapshotRepository`、`ServerRegistry` 等抽象优先策略，并明确禁止把 `ServerUtilities` 的单入口大模组、全局数据宿主和多通道网络注册表原样迁入

### 2026-04-10 - 产出 ServerUtilities 逐项整合映射表

- 主题：把 `Reference/ServerUtilities` 中后续可能需要的能力逐项列成映射表，并标注为可直接复用、需抽取后复用、建议重写或暂不整合
- 影响范围：`docs/serverutilities-integration-mapping.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：仅靠“需要重构”这一层判断还不够，后续第一批要吸收哪些工具能力，必须落实到具体模块与命令级别，否则仍容易在实现阶段回到“整包看起来都能用”的误判
- 引用来源：`Reference/ServerUtilities/src/main/java/serverutils/data`、`Reference/ServerUtilities/src/main/java/serverutils/command`、`Reference/ServerUtilities/src/main/java/serverutils/client`、`Reference/ServerUtilities/src/main/java/serverutils/ranks`、`Reference/ServerUtilities/src/main/java/serverutils/task`
- 结果：新增正式映射文档，已把 `home` / `warp` / `tpa` / `TeleportTracker` / leaderboard 等列入优先评估清单，把 player data / universe data / net handler / ranks 等标为建议重写，并把 claims、chunkloading、backup、watchdog、invsee 等标记为暂不整合

### 2026-04-11 - 记录第一批 server tools / cluster 已确认需求

- 主题：把 `ServerUtilities` 第一批整合范围中已经拍板的命令、跨服、数据库、权限与 GUI 约束正式写入 docs，供后续执行 prompt 直接引用
- 影响范围：`docs/servertools-phase1-requirements.md`、`docs/serverutilities-integration-mapping.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：后续执行 prompt 已不再缺架构边界，而是缺一份稳定、可引用的需求确认单；如果这些要求只留在对话里，后面实现时仍容易回到“先做单服占位”或“顺手加 GUI”的偏航
- 引用来源：本轮对话中已确认的第一期范围；`docs/serverutilities-integration-mapping.md`
- 结果：新增一份正式需求确认文档，明确 `home` / `back` / `spawn` / `tpa` / `rtp` / `warp` 为一期必须实现范围，明确跨服和数据库为硬约束、GUI 暂缓、权限只做玩家语义但要预留职业 / 贡献度 / 声望制度扩展接口

### 2026-04-11 - 产出可直接交付给另一个 AI 的 server tools 第一期执行 Prompt

- 主题：把已确认需求收口成一份可直接交给另一个 AI 执行的正式 prompt，避免后续实现阶段再次回到“继续评估”或“顺手扩范围”的状态
- 影响范围：`docs/servertools-phase1-execution-prompt-2026-04-11.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：第一期范围、跨服能力、数据库硬约束、权限边界和不做 GUI 都已经明确，当前最需要的是一份强约束执行文本，供后续代码搬运、修改与适配直接使用
- 引用来源：`docs/servertools-phase1-requirements.md`、`docs/serverutilities-integration-mapping.md`、`README.md`
- 结果：新增一份正式执行 prompt，已明确本轮必须实现 `home` / `back` / `spawn` / `tpa` / `rtp` / `warp`，必须支持跨服并直接接数据库，不兼容旧命令格式，不引入旧 rank / net / GUI 框架，只允许按当前项目模块化单体结构完成服务、仓储、命令与 cluster 接线

### 2026-04-11 - 落地 server tools / cluster 第一期命令链、仓储链与模块装配

- 主题：在 `JsirGalaxyBase` 中正式新增 `ClusterModule` 与 `ServerToolsModule`，把 `home` / `back` / `spawn` / `tpa` / `rtp` / `warp` 的最小可运行第一期主链直接落到当前仓库
- 影响范围：`src/main/java/com/jsirgalaxybase/module/ModuleContext.java`、`src/main/java/com/jsirgalaxybase/bootstrap/ModBootstrap.java`、`src/main/java/com/jsirgalaxybase/modules/cluster/`、`src/main/java/com/jsirgalaxybase/modules/servertools/`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/servertools/application/PlayerTeleportServiceTest.java`、`ops/sql/migrations/20260411_001_add_servertools_cluster_phase1.sql`、`docs/servertools-cluster-postgresql-ddl.sql`、`docs/servertools-phase1-command-reference.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：第一期范围已经从“评估”转入“直接实现”，而且需求已明确要求必须走数据库真源、模块生命周期装配、cluster/servertools 双模块边界和跨服票据预留，不允许再停留在单服 NBT / 文件占位版
- 引用来源：`README.md`、`docs/serverutilities-integration-mapping.md`、`docs/servertools-phase1-requirements.md`、`Reference/ServerUtilities/src/main/java/serverutils/data/TeleportTracker.java`、`Reference/ServerUtilities/src/main/java/serverutils/data/TeleportLog.java`、`Reference/ServerUtilities/src/main/java/serverutils/command/tp/`
- 结果：当前仓库已新增 cluster server directory / transfer ticket / homes / back / warp / tpa / rtp 记录表与 fail-fast JDBC factory；`home`、`back`、`spawn`、`tpa`、`rtp`、`warp` 顶层命令已注册到 dedicated server 启动链；本服目标会直接传送，跨服目标会通过 `cluster_transfer_ticket` 和 `GatewayAdapter` 进入下一阶段真实代理接线预留；同时补齐了纯服务层与 cluster 分发层单测，以及一份正式命令/表结构说明文档

### 2026-04-11 - 产出 server tools / cluster 第一期验收收口 Prompt

- 主题：把本轮严格验收发现的阻塞项收口成一份新的整改 prompt，要求后续执行只修已确认问题，不再继续扩大第一期提交范围
- 影响范围：`docs/servertools-phase1-acceptance-close-prompt-2026-04-11.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：本轮验收已经确认第一期主链基本成立，但仍存在 transfer ticket 状态未完整持久化、TPA 目标服未校验以及提交范围失控这三类问题；如果不把这些问题单独固化成整改 prompt，后续执行很容易再次把修复做成新一轮扩范围开发
- 引用来源：本轮严格验收结论；`docs/servertools-phase1-execution-prompt-2026-04-11.md`；`docs/servertools-phase1-command-reference.md`
- 结果：新增一份只面向验收收口的执行 prompt，已经明确本轮仅允许修复 cluster ticket 状态回写与 TPA target server 校验，并明确禁止继续改 terminal / market / bank 等无关范围

### 2026-04-11 - 收口第一期验收问题：cluster ticket 状态回写与 TPA 目标服校验

- 主题：只修第一期验收中已经确认的两处根因问题，不扩大 servertools / cluster 之外的提交范围
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`、`src/main/java/com/jsirgalaxybase/modules/servertools/application/PlayerTeleportService.java`、`src/main/java/com/jsirgalaxybase/modules/servertools/ServerToolsModule.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/servertools/application/PlayerTeleportServiceTest.java`、`docs/servertools-phase1-command-reference.md`、`docs/WORKLOG.md`
- 原因：严格验收确认当前远端派发返回 pending ticket 时没有把 adapter 返回状态统一回写数据库，同时 `tpa <playerName> [targetServerId]` 会把未注册或 disabled 的 serverId 错写进业务记录；这两处都属于第一期实现收口缺陷
- 引用来源：`docs/servertools-phase1-acceptance-close-prompt-2026-04-11.md`、`docs/servertools-phase1-command-reference.md`
- 结果：cluster 远端派发现在只要 adapter 返回 ticket 就会统一持久化其最新状态与消息；TPA 创建链现在会复用 cluster server directory 校验目标服是否存在且 enabled，校验失败时不会写入 `player_tpa_request`；同时补齐了对应定向单测，且没有继续触碰 terminal / market / bank 范围

### 2026-04-12 - 产出 cluster 第二阶段严格验收收口 Prompt

- 主题：补出一份只处理 cluster 第二阶段严格验收阻塞项的收口 prompt，避免下一轮执行继续扩散到新功能或无关模块
- 影响范围：`docs/servertools-phase2-cluster-close-prompt-2026-04-12.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：本轮严格验收确认第二阶段主链虽然已接通，但仍有两个阻塞项未收口：其一是 ticket 过期清理会保留旧 `status_message`，导致 `EXPIRED` 状态与消息语义不一致；其二是重复 `requestId` 命中终态 ticket 时，`ClusterTeleportService` 仍会无条件返回 `PENDING_REMOTE`，破坏最小幂等与恢复语义
- 引用来源：`src/main/java/com/jsirgalaxybase/modules/cluster/infrastructure/jdbc/JdbcTeleportTicketRepository.java`、`src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/PlayerArrivalRestoreServiceTest.java`
- 结果：新增一份只面向这两个阻塞问题的收口 prompt，已经明确本轮只修 ticket 过期消息覆盖、重复 requestId 的终态返回语义，并要求补对应定向测试和 WORKLOG，不再扩大到 terminal / market / bank 或继续加 cluster 新能力

### 2026-04-12 - 收口 cluster 第二阶段严格验收阻塞项：过期 message 终态覆盖与重复 requestId 终态映射

- 主题：只修 cluster 第二阶段严格验收已确认的两个阻塞项，不继续扩 gateway、生命周期或其他模块范围
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`、`src/main/java/com/jsirgalaxybase/modules/cluster/infrastructure/jdbc/JdbcTeleportTicketRepository.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/cluster/application/PlayerArrivalRestoreServiceTest.java`、`docs/WORKLOG.md`
- 原因：当前实现里，过期清理仍会因 `COALESCE` 保留旧的 waiting/retry message，且重复 `requestId` 命中 `FAILED / COMPLETED / EXPIRED` ticket 时会被统一伪装成 `PENDING_REMOTE`；这两点直接破坏终态可观测性与最小幂等语义
- 引用来源：`docs/servertools-phase2-cluster-close-prompt-2026-04-12.md`、`src/main/java/com/jsirgalaxybase/modules/cluster/application/ClusterTeleportService.java`、`src/main/java/com/jsirgalaxybase/modules/cluster/infrastructure/jdbc/JdbcTeleportTicketRepository.java`
- 结果：过期清理现在会无条件把 `status_message` 覆盖成明确的 expired 文案；重复 `requestId` 命中已有 ticket 时，会按真实状态返回 pending/completed/failed 语义，不再把终态 ticket 统一伪装成 pending；同时补了对应定向单测

### 2026-04-12 - 产出第三阶段 MCSM 灰度联调准备执行 Prompt

- 主题：把 server tools / cluster 下一阶段工作收口成一份严格执行 prompt，只允许利用 MCSM 下的代理 / 大厅 / S2 组一条不影响在线 S1 的灰度联调链
- 影响范围：`docs/servertools-phase3-mcsm-gray-rollout-prompt-2026-04-12.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前第二阶段代码语义已经通过严格验收，但现网资源评估确认 Lobby 与 S2 仍未部署 `JsirGalaxyBase` 模组和相关配置，现阶段真正需要的不是继续扩代码，而是把代理 / 大厅 / S2 准备到可启动、可观察、可继续联调的灰度环境，同时绝对不触碰正在承载玩家的 S1
- 引用来源：`../GroupServer/Galaxy_GTNH_Entrance/velocity.toml`、`../GroupServer/Galaxy_GTNH_Lobby/server.properties`、`../GroupServer/Galaxy_GTNH284_S2/server.properties`、`../GroupServer/mcsmanager/daemon/data/InstanceConfig/b3e4f9f9aefd4d2a9f57c338c1a0f3b8.json`、`../GroupServer/mcsmanager/daemon/data/InstanceConfig/d1e307c5d40745079df2e398e9d85db2.json`、`../GroupServer/mcsmanager/daemon/data/InstanceConfig/d705aa8c32c649228a84a323e1504f62.json`
- 结果：新增一份第三阶段严格执行 prompt，已经明确本轮只允许审计、构建、部署并通过 MCSM 启动代理 / Lobby / S2 的灰度链，必须保持不动 S1、不得绕开 MCSM 直接管控实服进程，也不得顺手扩大到 terminal / market / bank 或新一轮 cluster 功能扩写

### 2026-04-12 - 产出 terminal bank / market 审查后续修复 Prompt

- 主题：把 terminal bank / market 审查中确认的两个后续风险收口成一份可直接执行的修复 prompt，避免下一轮实现时再次混入无关 GUI 打磨或业务扩写
- 影响范围：`docs/terminal-bank-market-risk-fix-prompt-2026-04-12.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：本轮代码审查已经确认，一处高优风险是 market/custom 动作后没有联动失效银行快照，导致银行页可能展示过期余额与状态；另一处低优风险是 `TerminalHomeGuiFactory` 旧 market 挂载残留分支仍保留 custom panel 错接线，虽然当前不是主链，但后续误启用会直接带回错误弹窗接线
- 引用来源：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSessionController.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`
- 结果：新增一份 terminal bank / market 风险修复 prompt，已经明确本轮优先补齐跨页银行快照失效链，并最小清理旧 market 挂载残留的 custom panel 错接线，不继续扩大到 market / bank 业务语义、terminal 视觉层或更大范围重构

### 2026-04-11 - 产出第二阶段第一优先级 cross-server gateway 执行 Prompt

- 主题：把 servertools / cluster 下一阶段最优先的工作进一步收口成一份可直接交给另一个 AI 执行的 prompt，聚焦真实跨服闭环而不是继续扩命令数量
- 影响范围：`docs/servertools-phase2-cross-server-gateway-prompt-2026-04-11.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：第一期命令链和数据库真源已经成立，下一阶段真正的高优任务不再是继续加 `servertools` 命令名，而是把 `GatewayAdapter + TransferTicket` 从占位链推进到源服派发、目标服消费、落点恢复和失败可排障的真实跨服闭环
- 引用来源：`docs/servertools-phase1-requirements.md`、`docs/servertools-phase1-command-reference.md`、`docs/servertools-phase1-execution-prompt-2026-04-11.md`、`docs/servertools-phase1-acceptance-close-prompt-2026-04-11.md`
- 结果：新增一份第二阶段第一优先级执行 prompt，已经明确本轮只做真实网关适配边界、transfer ticket 生命周期闭环、目标服消费与落点恢复、最小失败恢复与排障观测，不再扩 terminal / market / bank 或 GUI 范围

### 2026-04-11 - 产出 terminal bank / market 严格收口执行 Prompt

- 主题：补出一份只面向 terminal bank / market sync 回归收口的执行 prompt，避免后续实现继续把主链阻塞项、非阻塞残留和 GUI 打磨需求混在一起
- 影响范围：`docs/terminal-bank-market-strict-close-prompt-2026-04-11.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前 terminal bank / market 的严格审阅已经确认，高风险问题集中在文本输入框再次混用 binder 手工 sync 与 `TextFieldWidget.value(...)` auto sync，以及缺少覆盖 terminal open 装配链的回归测试；如果不单独起一份收口 prompt，后续实现容易顺手扩大到旧装配残留清理、page 重构或视觉打磨
- 引用来源：`docs/terminal-gui-regression-chain-2026-04-05.md`、`docs/terminal-gui-continue-current-implementation-prompt-2026-04-06.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`
- 结果：新增一份 terminal bank / market 严格收口 prompt，已经明确本轮只移除 market 与 bank 文本输入框的手工 sync 注册、补 terminal open 装配链测试、更新 WORKLOG，并明确把旧 market page container 残留和更大范围页面装配测试留到下一轮

### 2026-04-05 - 产出市场终端 asset-first 重构评估与执行 Prompt

- 主题：基于现有三市场终端代码，正式评估“先选物品 / 挂牌 / 标的，再进详情页”的 asset-first 重构方向，并产出下一轮可执行 prompt
- 影响范围：`docs/market-terminal-asset-first-refactor-evaluation-2026-04-05.md`、`docs/market-terminal-asset-first-refactor-prompt-2026-04-05.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前终端虽然已经完成 MARKET 总入口拆分，但只有标准商品市场半成型地具备对象浏览与详情节奏；定制商品市场仍主要是说明页，汇率市场仍以手持驱动面板为主，继续直接堆字段会把终端重新推回统一 MARKET 巨石结构
- 引用来源：`docs/market-three-part-architecture.md`、`docs/market-entry-overview.md`、`docs/custom-market-minimal-model.md`、用户提出的 AE 终端 / 售货机式交互需求
- 结果：新增一份正式评估文档，明确标准商品市场适合完整采用物品优先终端、定制商品市场应做成 listing-first GUI、汇率市场应做成标的优先详情页；并同步新增可直接执行的重构 prompt

### 2026-04-05 - 补充终端 GUI 回归链文档中的验收阻塞与修复指令

- 主题：把本轮验收发现的实际阻塞项补写进终端 GUI 回归链统一文档，避免后续执行 AI 只看“已完成”结论而继续把兼容边界修歪
- 影响范围：`docs/terminal-gui-regression-chain-2026-04-05.md`、`docs/WORKLOG.md`
- 原因：市场终端与汇率市场大批实现已经落地，但定向测试显示旧任务书兑换兼容入口对非任务书物品的拒绝语义发生回归；需要把“问题是什么、允许怎么修、禁止怎么修、修后必须验证什么”直接写进 prompt/事故链文档
- 引用来源：`src/test/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangeServiceTest.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangeService.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/ExchangeMarketService.java`
- 结果：在统一事故文档末尾新增“本轮验收阻塞与修复执行 Prompt”段，明确要求保留新 formal quote 链，同时恢复旧兼容入口对非任务书物品的直接拒绝语义，并要求后续执行补 WORKLOG 和定向测试验证

### 2026-04-06 - 补出终端 GUI 持续实装 Prompt，明确不再保留回退方案

- 主题：补出一份新的终端 GUI 执行 prompt，明确后续只允许继续修改当前 GUI 实现，不再引入 fallback GUI 或新旧双轨并存
- 影响范围：`docs/terminal-gui-continue-current-implementation-prompt-2026-04-06.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前终端 GUI 主链已经落地，后续重点应转为继续打磨当前 ModularUI 实装效果；如果此时继续让执行 AI保留可回退 GUI，会把结构重新带回双轨维护与巨石装配
- 引用来源：`docs/terminal-modularui2-gui-refactor-prompt-2026-04-02.md`、`docs/terminal-gui-regression-chain-2026-04-05.md`、`docs/market-terminal-asset-first-refactor-evaluation-2026-04-05.md`
- 结果：新增一份直接面向下一轮 GUI 持续实装的 prompt，明确要求只在当前 GUI 上继续实现原先效果、保留真实银行/市场/汇率主链，并接受后续基于用户截图继续微调

### 2026-04-06 - 在当前单实现终端上继续收口导航壳与业务详情层次

- 主题：继续直接修改当前 ModularUI 终端实现，把总壳、银行页、标准商品市场、定制商品市场和汇率市场进一步收口成更接近正式终端的层次，而不是再引入任何回退 GUI
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalWidgetFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankPageBuilder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`、`docs/WORKLOG.md`
- 原因：当前 GUI 主链已经可用，但首页/导航/详情页仍有明显“工程拼板态”；银行页说明块仍压着真实操作区，标准商品详情页买卖/仓储/订单主次不够清晰，定制商品与汇率页也还缺少更明确的对象详情节奏
- 引用来源：`docs/terminal-gui-continue-current-implementation-prompt-2026-04-06.md`、`docs/terminal-modularui2-gui-refactor-prompt-2026-04-02.md`、`docs/market-terminal-asset-first-refactor-evaluation-2026-04-05.md`
- 结果：新增共享 hero band / summary banner 壳层，用于统一页头与状态带；终端总壳导航改成更明确的当前页/当前终端分区提示；银行页改成状态、表单预览、服务反馈和确认门禁闭环；标准商品页把详情层进一步收口成商品摘要 + 盘口 + 交易动作台 + 订单/仓储闭环；定制商品和汇率页也补出更明确的当前详情提示与执行顺序说明，且全程保留现有真实银行 / 市场 / 汇率业务链

### 2026-04-06 - 修复终端首页路由矩阵导致的 ModularUI 尺寸循环

- 主题：修复 client 打开终端首页时因为路由矩阵尺寸推导互相依赖而触发的 ModularUI 死循环卡死
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：首页“终端路由矩阵”区块把 `expanded()` 的矩阵容器放进了 `coverChildrenHeight()` 的 section 里，父容器需要靠子项算高度，子项又反过来等待父高度，运行时就会刷出大量 `MUI [SIZING][Column]: Can't cover children when all children depend on their parent!`
- 结果：矩阵区改成由表头和固定行高直接撑开，不再把相对扩展高度控件塞进按子项包高的 section，从而收口终端首页打开即卡死的问题

### 2026-04-14 - 把终端 GUI 长期路线正式改为内置 BetterQuesting 风格框架

- 主题：把终端 GUI 的长期技术路线从继续深化 `ModularUI 2`，正式调整为 vendoring BetterQuesting 风格 GUI 框架，并把改造面与实施顺序写入终端实施方案
- 影响范围：`docs/terminal-plan.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：前序调研和真实兼容性实验已经证明，`ModularUI 2` 属于 GTNH 共享运行时依赖，版本漂移会带来 pack 级 ABI 风险；而终端未来要承载银行、市场与后续 server tools 统一应用壳，更适合采用仓库自有的 `GuiScreenCanvas + panel tree + theme registry` 体系
- 引用来源：`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/GuiScreenCanvas.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/IGuiPanel.java`、`Reference/BetterQuesting/src/main/java/betterquesting/client/themes/ThemeRegistry.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`
- 结果：终端实施方案现已明确哪些业务层保持不动、哪些 `ModularUI` 类必须重写、为什么打开链要从服务端直接开 GUI 改成“服务端授权 + 客户端开屏”，以及后续 vendoring、首页壳、银行页、市场页与旧依赖清理的建议实施顺序

### 2026-04-14 - 把 BetterQuesting 风格 GUI 集成方案拆成独立文档

- 主题：将终端 GUI 的 BetterQuesting 风格 framework 集成设计从 `terminal-plan.md` 中拆出，形成单独的实施方案文档，避免终端总方案与 GUI 内核集成细节混在同一份文档里
- 影响范围：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-plan.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：原终端实施方案需要继续承担入口、分阶段目标和总体路线说明；而 BetterQuesting 风格 GUI 集成已经进入到 vendoring 范围、协议重构、包结构和页面装配层级，继续混写会让后续执行文档失焦
- 引用来源：`docs/terminal-plan.md`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/GuiScreenCanvas.java`、`Reference/BetterQuesting/src/main/java/betterquesting/client/themes/ThemeRegistry.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`
- 结果：现在 `terminal-plan.md` 只保留 GUI 路线结论与索引，具体的 BetterQuesting 风格 GUI 集成边界、协议变化、包结构、迁移顺序和验收标准已经转入独立方案文档，后续真正实装时可直接以该文档为主

### 2026-04-14 - 产出 BetterQuesting 风格 UI framework 第一阶段执行 Prompt

- 主题：补出一份只面向 BetterQuesting 风格 GUI framework 第一阶段落地的执行 prompt，明确本轮只做最小 vendoring、theme/resource 骨架和占位 screen，不提前混入 terminal 迁移
- 影响范围：`docs/terminal-betterquesting-ui-phase1-framework-prompt-2026-04-14.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：下一步已经从“继续评估”转入“交给另一个 AI 实做第一阶段 framework 地基”；如果没有一份强约束 prompt，执行时很容易顺手把 terminal 页面迁移、协议重构和旧 `ModularUI` 清理提前混进来，导致范围失控
- 引用来源：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/GuiScreenCanvas.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/IGuiPanel.java`、`Reference/BetterQuesting/src/main/java/betterquesting/client/themes/ThemeRegistry.java`
- 结果：现已新增一份可直接执行的第一阶段 prompt，明确要求只落地仓库自有 GUI framework 基础层、去 BetterQuesting 全局依赖、建立最小主题骨架并跑通占位 screen，同时明确禁止提前迁 terminal 页面和打开协议

### 2026-04-14 - 落地终端 phase 2 新打开链：服务端授权 + 客户端开 TerminalHomeScreen

- 主题：把终端正式入口从“客户端请求后由服务端直接打开 ModularUI”推进到“客户端请求、服务端授权、客户端自行打开 BetterQuesting 风格 TerminalHomeScreen 占位根屏”的最小闭环
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/network/`、`src/main/java/com/jsirgalaxybase/terminal/client/`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/`、`src/test/java/com/jsirgalaxybase/terminal/`、`src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 1 framework 已经可独立运行，但终端主链仍停在 `OpenTerminalMessage -> TerminalService.openTerminal(player) -> TerminalHomeGuiFactory.INSTANCE.open(player)`；第二阶段需要先把 screen 生命周期和授权协议站稳，而不是提前迁银行页和市场页
- 引用来源：`README.md`、`docs/terminal-plan.md`、`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase1-framework-prompt-2026-04-14.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`
- 结果：新增 `OpenTerminalRequestMessage` 与 `OpenTerminalApprovedMessage`，客户端快捷键和背包按钮现已走新请求链；服务端通过 `TerminalService.approveTerminalClientScreen(...)` 生成最小初始化快照与 session token；客户端收到授权后由 `TerminalClientScreenController` 在主线程打开 `TerminalHomeScreen` 占位根屏；`TerminalFrameworkTestScreen` 的 F8 调试入口仍保留文件但不再注册进正式 client bootstrap；旧 `TerminalHomeGuiFactory`、银行页、市场页与 `OpenTerminalMessage` 旧链仍保留为过渡实现，未提前迁移业务

### 2026-04-15 - 把 TerminalHomeScreen 从 phase 2 占位板推进为 phase 3 首页壳

- 主题：只把新终端主链上的 `TerminalHomeScreen` 推进成真正可承载后续业务页的首页壳，并补齐最小 screen model、共用 shell panel、通知宿主与 popup 生命周期，不提前迁银行页和市场页
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalOpenApproval.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalOpenSummaryFormatter.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalOpenSummaryFormatterTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModelTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 2 已经接通“客户端请求、服务端授权、客户端开屏”的新主链，但 `TerminalHomeScreen` 仍只是单块摘要板，后续银行页和市场页还没有可直接挂接的顶部状态带、导航、通知和 popup 宿主层
- 引用来源：`README.md`、`docs/terminal-plan.md`、`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase1-framework-prompt-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase2-open-chain-prompt-2026-04-14.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalWidgetFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalDialogFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalNotification.java`
- 结果：`TerminalHomeScreenModel` 现已扩成包含状态带、导航项、首页 section 和通知入口位的最小首页壳模型；新增 `TerminalPanelFactory`、`TerminalShellPanels`、`TerminalHomeSection` 与 `TerminalPopupFactory` 作为后续业务页可复用的壳层组件；`TerminalHomeScreen` 已重构为顶部状态带 + 左侧导航 + 主内容区 + 通知宿主 + popup 宿主的首页壳；非首页导航点击现在会走新壳级 popup 而不是偷接旧业务页；旧 `TerminalHomeGuiFactory`、银行页、市场页、binder 和 sync state 继续保留为过渡实现

### 2026-04-15 - 收口 phase 3 当前页语义：selectedPageId 成为首页壳单一真源

- 主题：只修 phase 3 严格验收确认的当前页语义分裂问题，把首页壳当前页统一收口到 `selectedPageId`，不提前进入 phase 4 section 宿主切换
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`、`src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModelTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 3 虽然已经显式传 `selectedPageId`，但首页壳实际判断当前页仍主要依赖 `navItems[].selected`；如果后续服务端只切 page id 或 nav flag 不一致，状态带、主体区和导航高亮就可能出现错页
- 引用来源：`docs/terminal-betterquesting-ui-phase3-home-shell-prompt-2026-04-15.md`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`
- 结果：`selectedPageId` 现在是首页壳当前页语义的唯一真源；`NavItemModel.selected` 仍保留，但只作为模型归一化后的派生高亮结果；模型会把子页 id 映射到顶层导航页并重建 nav 选中态，不再信任调用方传入的 selected 标记；本轮没有开始 section 真切换、动作协议或业务页迁移

### 2026-04-15 - 产出终端 phase 3 严格验收收口 Prompt

- 主题：补出一份只面向 terminal phase 3 严格验收收口项的执行 prompt，明确下一轮只修首页壳 `selectedPageId` 与导航选中态仍然分裂的语义缺口，不提前进入 phase 4
- 影响范围：`docs/terminal-betterquesting-ui-phase3-close-prompt-2026-04-15.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 3 首页壳主结构已经落地并通过静态验收，但严格审阅确认首页当前页语义仍同时依赖 `selectedPageId` 与 `navItems[].selected` 两套来源；如果不先收干净，下一阶段 section 宿主切换和真实页面挂载会先踩当前页高亮与主体区错页风险
- 引用来源：`docs/terminal-betterquesting-ui-phase3-home-shell-prompt-2026-04-15.md`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`
- 结果：现已新增一份 phase 3 收口 prompt，范围收口到首页壳当前页语义的单一真源、最小模型与测试修正，以及 README / WORKLOG 同步，不再把 phase 4 的 section 路由、动作协议和业务页迁移混进这轮

### 2026-04-18 - 产出终端 phase 4 section 宿主与最小协议地基 Prompt

- 主题：补出一份只面向 terminal 新首页壳 section 宿主切换与最小 action / snapshot 协议地基的第四阶段执行 prompt，明确这轮先把新壳做成真正宿主，再进入银行页迁移
- 影响范围：`docs/terminal-betterquesting-ui-phase4-section-host-prompt-2026-04-18.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：原高层集成方案虽然把“迁银行页”列为首页壳之后的下一大阶段，但 phase 3 严格验收与收口已经确认，若不先把首页壳推进成真实 section 宿主，并把 action / snapshot 协议补到最小正式落点，后续银行页迁移仍会缺少稳定宿主和刷新边界

### 2026-04-19 - 修复新终端客户端在窄文本区渲染中文标签时的递归换行崩溃

- 主题：修复 `TerminalHomeScreen` 打开后，`LabelPanel` 在极窄文本宽度下调用原版 `FontRenderer.wrapFormattedStringToWidth` 导致的栈溢出崩溃
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/framework/LabelPanel.java`、`docs/WORKLOG.md`
- 原因：phase 6 市场页接入后，部分标签在小屏或紧凑布局下会落入极窄宽度；1.7.10 原版 `FontRenderer` 对中文等宽字符在过窄宽度下可能返回 `sizeStringToWidth == 0`，随后进入无限递归换行
- 引用来源：`run/client/crash-reports/crash-2026-04-19_16.29.23-client.txt`
- 结果：`LabelPanel` 现已改为使用自有安全换行逻辑，在无法安全消费首字符时回退为截断显示，避免终端页再次因窄文本区渲染而直接崩溃
- 引用来源：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase3-home-shell-prompt-2026-04-15.md`、`docs/terminal-betterquesting-ui-phase3-close-prompt-2026-04-15.md`、`docs/README.md`
- 结果：现已新增一份 phase 4 执行 prompt，范围收口到首页壳 section 切换、最小 `TerminalActionMessage` / `TerminalSnapshotMessage` 落点、README / WORKLOG 同步，以及明确把银行页作为下一阶段的第一张完整业务页，而不是在本轮提前迁移

### 2026-04-18 - 落地终端 phase 4 section 宿主、最小 action 协议与 snapshot 回写链

- 主题：把 `TerminalHomeScreen` 从 phase 3 首页壳推进成真实 section 宿主，并补齐最小 `TerminalActionMessage` / `TerminalSnapshotMessage` 往返链，但不提前迁入银行页与市场页业务内容
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalActionType.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalOpenApproval.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/`、`src/main/java/com/jsirgalaxybase/terminal/client/`、`src/main/java/com/jsirgalaxybase/terminal/client/component/`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalSectionRouterTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModelTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 3 收口后虽然已经保证了 `selectedPageId` 是首页壳单一真源，但主体区仍固定渲染 home sections，导航点击也仍停留在 popup 占位，导致后续银行页迁移依然缺少真实 section 宿主和最小刷新闭环
- 引用来源：`docs/terminal-betterquesting-ui-phase4-section-host-prompt-2026-04-18.md`、`docs/terminal-betterquesting-ui-phase3-close-prompt-2026-04-15.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`
- 结果：`TerminalHomeScreenModel` 已改为 page snapshot 结构，`TerminalShellPanels` 主体区现按顶层 section host 渲染当前 page snapshot，导航点击会先切换本地壳再发最小 action 给服务端，服务端通过 `TerminalService.handleClientAction(...)` 返回新的 shell snapshot，客户端可在已打开的 `TerminalHomeScreen` 上原地刷新；本轮仍刻意保留旧 `ModularUI` 银行页与市场页，不做业务迁移

### 2026-04-18 - 补 phase 4 action/snapshot 往返链定向测试收口

- 主题：只补 phase 4 严格验收指出的缺口，为最小 `TerminalActionMessage` / `TerminalSnapshotMessage` 往返链增加直接定向测试，不改主实现边界
- 影响范围：`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`docs/WORKLOG.md`
- 原因：前一轮 phase 4 代码与现有定向测试已经覆盖 section 宿主和 page snapshot 归一化，但还缺一条直接证明“服务端 action 处理结果可回写为 snapshot，并进入客户端刷新入口”的最小测试，严格口径下不算完全收口
- 引用来源：`docs/terminal-betterquesting-ui-phase4-section-host-prompt-2026-04-18.md`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalActionMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/client/TerminalClientScreenController.java`
- 结果：`TerminalServiceTest` 现已补齐 service -> snapshot model round-trip 验证，以及 `TerminalSnapshotMessage.Handler` 向 `TerminalClientScreenController` 排队刷新模型的直接测试；本轮仍未提前迁银行页、市场页或扩写完整业务协议

### 2026-04-18 - 产出终端 phase 5 银行页迁移 Prompt，并压缩剩余阶段目标

- 主题：补出一份只面向银行页迁移的 phase 5 执行 prompt，并把从当前阶段起后续目标压缩为“再往后不超过五个阶段达到删除旧 terminal ModularUI 实现的条件”
- 影响范围：`docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 4 已经把首页壳推进成真实 section 宿主，下一步不该继续停留在抽象层，而应直接让银行页成为新壳上的第一张完整业务页；同时用户明确希望后续节奏加快，尽量在有限阶段内完成 terminal 对旧 ModularUI 的切换与删除准备
- 引用来源：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase4-section-host-prompt-2026-04-18.md`、`docs/banking-terminal-gui-design.md`、`docs/README.md`
- 结果：现已新增一份 phase 5 prompt，范围收口到银行页真实迁移、bank action / snapshot 闭环、确认 popup 迁移和文档同步，并把后续节奏压缩为 phase 6 市场迁移、phase 7 收干 terminal 旧装配残留、phase 8 cutover、phase 9 删除旧 terminal ModularUI 实现

### 2026-04-18 - 落地终端 phase 5 银行完整业务页迁移

- 主题：把 bank 顶层 section 从 phase 4 的宿主占位推进成新 `TerminalHomeScreen` 上的第一张完整业务页，并接通开户、刷新、转账确认 popup 与 snapshot 回写闭环
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalActionType.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalOpenApproval.java`、`src/main/java/com/jsirgalaxybase/terminal/network/`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalBankSectionModelTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalPopupFactoryTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 4 已经具备 section 宿主与最小 action/snapshot 地基，但银行页仍只是说明性 snapshot；下一步必须直接迁入第一张真实业务页，证明新壳可以承接完整终端业务而不是继续搭壳
- 引用来源：`docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md`、`docs/banking-system-requirements.md`、`docs/banking-terminal-gui-design.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSnapshotProvider.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankingService.java`
- 结果：新增 bank 专属 snapshot/view model、表单 payload 与本地输入状态；`TerminalService` 现已能处理 `BANK_REFRESH`、`BANK_OPEN_ACCOUNT`、`BANK_CONFIRM_TRANSFER` 并回写新的 bank snapshot；`TerminalHomeScreen` 现已通过 `TerminalBankSection` 与 `TerminalPopupFactory` 承接新银行页和转账确认 popup，且没有把旧 `TerminalBankPageBuilder`、`TerminalDialogFactory` 或旧 sync binder 重新接回新壳；同时补齐银行模型、action 回写和 popup 生命周期定向测试

### 2026-04-19 - 收口 phase 5 银行页服务端门禁与确认发送链测试

- 主题：只修 phase 5 严格验收指出的两处收口项：服务端银行 action 的 bank page 语义门禁，以及新壳转账确认 popup 到 `TerminalActionMessage` 发送链的直接测试
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalBankActionMessageFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalActionMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalBankActionMessageFactoryTest.java`、`docs/WORKLOG.md`
- 原因：上一轮严格验收确认，银行 action 在服务端仍可绕开 bank page 语义直接执行，同时 popup 测试只证明了新 modal 生命周期存在，还没有直接证明确认按钮继续走新 action/snapshot 主链
- 引用来源：`docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPopupFactory.java`
- 结果：服务端现在只会在当前 page 属于 bank 语义时执行 `BANK_OPEN_ACCOUNT` / `BANK_CONFIRM_TRANSFER`；同时把“转账确认后进入 `BANK_CONFIRM_TRANSFER` 主链”的消息构造收口到独立 helper，并新增对应定向测试，结合既有 `TerminalPopupFactoryTest` 一起证明确认链继续走新 popup + action/snapshot 主链，而不是回退到旧 Dialog 链

### 2026-04-19 - 产出终端 phase 6 市场总览与标准商品市场迁移 Prompt

- 主题：补出一份只面向 MARKET 总入口与标准商品市场迁移的 phase 6 执行 prompt，并把 phase 6 到 phase 9 的职责边界继续收口固定下来，避免下一轮又把 custom / exchange / cutover / 删除旧实现混在一起
- 影响范围：`docs/terminal-betterquesting-ui-phase6-market-overview-standardized-prompt-2026-04-19.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 5 已经把银行页迁成新壳上的第一张完整业务页，下一步必须直接让 MARKET 根页和标准商品市场进入新壳，证明 terminal 已能承接第二类复杂业务页；同时用户已经明确 phase 7 到 phase 9 的顺序，需立即固化到文档中避免后续节奏漂移
- 引用来源：`docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md`、`docs/market-three-part-architecture.md`、`docs/market-entry-overview.md`、`docs/standardized-market-catalog-boundary.md`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`
- 结果：现已新增一份 phase 6 prompt，明确本轮只迁 MARKET 总入口与标准商品市场真实业务页，要求继续沿用 selectedPageId / action / snapshot 主链、保留 MARKET 根页作为总入口语义、补至少一条真实市场动作回写闭环和一条新 popup 确认链，并明确把定制商品市场、汇率市场、正式 cutover 与旧 terminal ModularUI 删除留给 phase 7 到 phase 9

### 1. 项目定位

- `CustomClient` 不是当前主要 Java 源码仓
- 真正的开发主体是 `JsirGalaxyBase`
- `JsirGalaxyBase` 目标不是普通业务后台，而是：
  - `GTNH 服务器制度核心模组`
  - 并预留后续玩法能力扩展

### 2. 架构总判断

- 不采用传统 Web 框架优先的思路
- 不采用早期扁平模组写法继续扩展
- 当前正式架构定为：
  - `模块化单体`
  - `制度核心 + 能力模块`
  - `服务端权威`
  - `可替换持久化`

### 3. 当前模块边界

- `制度核心模块`
  - 职业
  - 经济
  - 贡献度 / 声望
  - 公共订单 / 公共工程
  - 群组服同步核心状态
- `能力模块`
  - 共享背包
  - 市场终端
  - 其他玩法增强能力
- `诊断模块`
  - 客户端物品导出
  - 开发观测工具

### 5. 本轮代码重构结果

- 删除了旧的示例式写法：
  - `HelloWorldCommand`
  - 旧 `CommonProxy`
  - 旧 `ClientProxy`
  - 旧扁平 `Config`
  - 旧 `client` 包中的导出控制器
- 引入新的启动和模块骨架：
  - `bootstrap/`
  - `module/`
  - `modules/core/`
  - `modules/capability/`
  - `modules/diagnostics/`

## 条目

### 2026-04-19 - 新终端壳接入 MARKET 总入口与标准商品市场

- 主题：把 MARKET 根页和 MARKET_STANDARDIZED 接到 BetterQuesting 风格 `TerminalHomeScreen` 新壳，并补真实标准商品 action / snapshot / popup 闭环
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalActionType.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalMarketActionPayload.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalMarketSectionSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalMarketActionMessageFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalMarketSectionModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionState.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSectionService.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalMarketActionMessageFactoryTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 5 已把银行页迁成新壳上的第一张完整业务页，但 MARKET 根页仍只有占位 section，标准商品市场的真实交易链仍停在旧终端实现，无法满足 phase 6 的范围约束和动作链要求
- 结果：新壳现在会为 MARKET 顶层 page snapshot 携带专用 market section model；MARKET 根页改成共享摘要 + 入口卡，MARKET_STANDARDIZED 改成真实标准商品 section；至少一条真实标准商品动作和一条后处理动作现在通过 `TerminalActionMessage -> TerminalSnapshotMessage` 回写；确认买单与 claim 走新 popup 生命周期；custom / exchange、cutover 与旧 ModularUI 删除仍保持出界

### 2026-04-19 - 产出终端 phase 6 严格验收收口 Prompt

- 主题：补出一份只面向 phase 6 严格验收缺口的收口 prompt，聚焦新壳滚动能力、标准商品市场布局密度和关键数据截断问题
- 影响范围：`docs/terminal-betterquesting-ui-phase6-close-prompt-2026-04-19.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 6 主链已经接通，但严格验收与实际 runClient 目视验证确认 MARKET_STANDARDIZED 仍存在无法滚动、全屏下信息可见面积不足，以及商品 / claim / 规则等内容被固定上限主动裁断的问题；这些缺口会阻塞 phase 7，但又不应把任务扩大成 custom / exchange 迁移
- 结果：现已新增一份 phase 6 close prompt，明确要求只修 framework 层滚动输入与局部滚动宿主、TerminalHomeScreen 与 TerminalMarketSection 的空间策略，以及标准商品市场 section 的关键数据完整显示，并要求保住 phase 6 已成立的 action / snapshot / popup 主链和本地 client/server 联调验证

### 2026-04-19 - 收紧新终端壳缩放与 phase 6 市场页布局密度

- 主题：在 phase 6 收口已恢复滚动与完整数据浏览后，继续把新 terminal 壳的占屏比例和市场页内部 chrome 密度收紧，修复实机目视下仍然“整体过度放大”的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`docs/WORKLOG.md`
- 原因：上一轮主要通过放宽 shell 尺寸上限和引入局部滚动来解决“看不全、滚不动”，但实际进游戏目视后，新壳仍然过于贴满 GUI 视口，状态带、导航项、页头和市场卡片的间距也偏松，导致整体观感依然像被放大过头
- 结果：终端壳现在改为按视口比例取值而不是几乎贴满屏幕；状态带、导航项、页头、footer 与市场 section 的顶部留白、左右分栏比例和买单卡高度也同步压紧，在不回退滚动与长列表能力的前提下提高信息密度并减轻“过度放大”观感

### 2026-04-19 - 产出终端 phase 7 定制商品市场 / 汇率市场迁移 Prompt

- 主题：补出一份只面向 phase 7 的执行 prompt，把剩余 terminal 真实业务页全部迁进新壳，并把新壳对旧 terminal 市场装配的直接依赖残留收干
- 影响范围：`docs/terminal-betterquesting-ui-phase7-custom-exchange-residue-prompt-2026-04-19.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 6 与其收口轮已经把 standardized market 和新壳滚动/布局问题收住，下一步必须明确把 MARKET_CUSTOM、MARKET_EXCHANGE 和旧 market 装配残留一起放进同一阶段完成，避免 phase 8 cutover 前还残留“业务页未迁完”的模糊状态
- 结果：现已新增一份 phase 7 prompt，明确本轮只迁 custom market 与 exchange market 真实业务页、接通各自 action / snapshot / popup 主链，并收干新壳对旧 market builder / binder / dialog / session controller 的直接依赖；同时继续把正式 cutover 与旧 ModularUI 删除留给 phase 8 和 phase 9

### 2026-04-19 - 收口 phase 6 市场页滚动、布局和数据截断缺口

- 主题：在不进入 phase 7 的前提下，把新 terminal framework 与标准商品市场收口到可滚动、可完整浏览、可继续进入下一阶段的状态
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/framework/CanvasScreen.java`、`src/main/java/com/jsirgalaxybase/client/gui/framework/GuiPanel.java`、`src/main/java/com/jsirgalaxybase/client/gui/framework/PanelContainer.java`、`src/main/java/com/jsirgalaxybase/client/gui/framework/VerticalScrollPanel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPanelFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContent.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/test/java/com/jsirgalaxybase/client/gui/framework/VerticalScrollPanelTest.java`、`src/test/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContentTest.java`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 6 运行态验收已经确认新壳没有滚轮输入通路、标准市场正文区尺寸偏紧、商品 / claim / 规则 / 盘口等关键数据仍被固定上限主动截断；如果不先收掉这些可用性问题，phase 7 就会建立在不可完整浏览的页面上
- 结果：framework 现在具备最小可复用滚动能力，滚轮事件会经 `CanvasScreen -> PanelContainer -> VerticalScrollPanel` 分发；`TerminalHomeScreen` 放宽了壳层尺寸上限并扩大正文区；`TerminalMarketSection` 改为大屏优先吃满、局部滚动承接小屏溢出，同时把商品浏览、盘口 / 我的订单、claim 和规则条目改成完整 child 生成，不再靠固定前 N 条硬裁；本轮仍保持 MARKET 根页只做总入口，MARKET_CUSTOM / MARKET_EXCHANGE、cutover 与旧 ModularUI 删除继续明确留给 phase 7 之后

### 2026-04-05 - 恢复终端内容区滚动能力

- 主题：恢复终端导航列与正文列的局部滚动能力，同时保留已经验证有效的单页宿主切页结构
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：上一轮为收口点击与布局问题，先移除了局部滚动控件，结果正文区虽然可正常切页，但较长内容不再能滚动，等于把原本功能一起删掉了
- 结果：把导航列和正文列的 `createScrollableBody(...)` 恢复为真正的 `ListWidget` 局部滚动容器，但不恢复“所有页面同时挂载”的旧正文结构；这样保留单页宿主修复成果的同时，拿回原本的滚动能力

### 2026-04-05 - 补回银行与市场输入框的同步注册

- 主题：补回银行转账表单和市场交易表单在终端面板里的输入同步注册，恢复原本可正常输入的行为
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`docs/WORKLOG.md`
- 原因：客户端崩溃日志明确指向 `Sync handler is not yet initialised!`，说明文本框绑定的 `StringSyncValue` 被创建了，但没有注册进 `PanelSyncManager`；打字时触发 `onTextChanged` 就会直接崩溃
- 结果：已补回银行转账的收款人 / 金额 / 备注字段，以及市场限价 / 即时交易数量字段的 `syncManager.syncValue(...)` 注册；这样恢复原有文本输入与服务端同步能力，而不是继续靠删功能规避问题

### 2026-04-04 - 修复终端主界面打开后整体不可点击

- 主题：移除终端主界面导航列与正文列对 `ListWidget` 的顶层包裹，改回普通布局容器，收口打开终端后整页无法点击的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：最新 client log 在终端打开瞬间连续出现大量 `MUI [SIZING][Column]: Can't cover children when all children depend on their parent!`，而终端主页唯一的 `ListWidget` 同时包住导航和正文，最符合尺寸循环与点击事件被滚动容器吞掉的现象
- 结果：终端主界面改为直接使用普通 `Flow.column()` 承载导航与正文内容，避免顶层滚动容器参与尺寸推导和输入捕获；后续点击验证以最新构建实装结果为准

### 2026-04-04 - 汇率市场 GUI 回归测试最后收口

- 主题：只补 exchange 子页正式 quote / 空状态 / 控制器门禁的回归测试，不再扩实现范围
- 影响范围：`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeQuoteViewTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSessionControllerTest.java`、`docs/WORKLOG.md`
- 原因：上一轮已经把 exchange 子页 GUI 主链落地，但关键事实仍主要依赖人工阅读，需要把正式字段映射、空状态和执行门禁锁成稳定回归
- 结果：新增 exchange 页空状态回归、TerminalMarketService 的 exchange 空状态映射回归，以及 TerminalMarketSessionController 的确认兑换门禁与本地错误反馈回归

### 2026-04-04 - 汇率市场子入口第一轮正式 GUI 实装

- 主题：把汇率市场子页从说明页改成真实可操作的终端页，支持正式 quote 预览、确认弹窗和 GUI 内兑换执行
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeQuoteView.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncState.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSessionController.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSessionController.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/ExchangeMarketService.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeQuoteViewTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/ExchangeMarketServiceTest.java`、`docs/market-entry-overview.md`、`docs/WORKLOG.md`
- 原因：汇率市场子入口已经完成路由拆分，但终端页仍停留在静态说明状态，玩家必须退回命令行才能看到正式 quote 字段和完成兑换，不符合汇率市场子入口的正式 GUI 实装目标
- 引用来源：`docs/market-total-entry-split-tail-close-prompt-2026-04-04.md`、`docs/market-entry-overview.md`
- 结果：汇率市场子页现在会读取当前手持物品并走正式 quote 路径，展示 pairCode / assetCode / ruleVersion / limitStatus / reasonCode / notes 等字段；支持“刷新报价”和“确认兑换”两步动作；确认后通过终端弹窗走服务端兑换，并刷新银行摘要与终端通知

### 2026-04-04 - MARKET 总入口正式拆成三类市场入口

- 主题：把终端与命令层里仍然像“统一 MARKET 大桶”的入口残留，正式拆成标准商品市场、定制商品市场、汇率市场三类入口
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalPageTest.java`、`docs/market-entry-overview.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：三市场边界已经定稿，但 MARKET 根页仍直接落到标准商品交易详情页，命令帮助也仍停留在“统一 market 兼容入口”语义，继续放任会让后续 GUI 和命令补丁再次回到混合大市场
- 引用来源：`docs/market-total-entry-split-prompt-2026-04-04.md`、`docs/market-three-part-architecture.md`、`docs/custom-market-minimal-model.md`
- 结果：MARKET 首页现在只负责总览和三类市场入口；标准商品交易页被下沉到独立子页；定制商品市场和汇率市场有了各自终端落点页；`/jsirgalaxybase market` 帮助明确按三类市场入口分组，并补了一条终端路由层单测

### 2026-04-04 - 产出 MARKET 总入口拆分阶段收口 Prompt

- 主题：为 MARKET 总入口拆分阶段补出最后一轮收口 prompt，聚焦真实路由回归测试和 docs 索引补齐
- 影响范围：`docs/market-total-entry-split-tail-close-prompt-2026-04-04.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：阶段主体已经成立，但当前所谓“终端路由回归测试”仍主要停留在 `TerminalPage` 枚举元数据层，且 `market-entry-overview.md` 尚未正式挂入 docs 索引，还不适合直接无保留关阶段
- 引用来源：`docs/market-total-entry-split-prompt-2026-04-04.md`、`docs/market-entry-overview.md`
- 结果：新增一份只针对最后两处真实缺口的收口 prompt，要求下一轮只补 MARKET 根页与三个子市场页的真实装配回归测试，并同步补齐 README 索引与 WORKLOG 记录

### 2026-04-04 - 产出 MARKET 总入口拆分阶段 Prompt

- 主题：为三市场执行顺序中的最后一步补出 MARKET 总入口拆分 prompt，要求入口层正式按标准商品市场、定制商品市场、汇率市场三类入口收口
- 影响范围：`docs/market-total-entry-split-prompt-2026-04-04.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：汇率市场规则层、标准商品市场目录边界、定制商品市场最小挂牌链都已经完成验收；如果此时还不拆 MARKET 总入口，后续终端和命令层仍会继续沿“统一 MARKET 大桶”漂移
- 引用来源：`docs/market-three-part-architecture.md`、`../../Docs/下次对话议程.md`、`../../Docs/市场经济推进.md`
- 结果：新增一份只针对 MARKET 总入口拆分的正式 prompt，明确这一轮只处理入口、路由、文案和命令帮助分组，不继续扩三类市场各自的业务语义

### 2026-04-04 - 修复终端 GUI 转账表单未同步到服务端

- 主题：修复银行终端 GUI 在点击确认后只显示“已提交”但服务端实际拿到空表单，导致转账并未真正执行的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`docs/WORKLOG.md`
- 原因：终端表单字段虽然绑定了 `StringSyncValue`，但没有通过 `syncManager.syncValue(...)` 注册到 panel sync manager；因此 synced action 在服务端执行时读到的仍是默认空值，命令行链路不受影响所以表现正常
- 结果：银行转账的收款玩家、金额、备注现在会随面板同步到服务端；同时把市场页的限价/即时交易输入一起补上同步注册，避免同类“GUI 看似提交、服务端实际空参数”的问题再次出现

### 2026-04-14 - 落地终端 BetterQuesting 风格 GUI framework 第一阶段地基

- 主题：在不迁移现有 terminal 主链的前提下，新增仓库自有命名空间下的最小 BetterQuesting 风格 GUI framework、theme registry 与占位测试屏
- 影响范围：`src/main/java/com/jsirgalaxybase/client/gui/framework/`、`src/main/java/com/jsirgalaxybase/client/gui/theme/`、`src/main/java/com/jsirgalaxybase/terminal/client/`、`src/main/resources/assets/jsirgalaxybase/textures/gui/framework/`、`src/main/java/com/jsirgalaxybase/terminal/TerminalClientBootstrap.java`、`docs/WORKLOG.md`
- 原因：终端长期 GUI 路线已经明确转向仓库内置的 BetterQuesting 风格 `GuiScreenCanvas + panel tree + theme registry` 体系；本轮需要先把 framework 地基、最小主题骨架和独立可运行 screen 跑通，而不是提前混入 terminal 页面迁移和协议重构
- 引用来源：`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/GuiScreenCanvas.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/IScene.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/IGuiPanel.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/panels/CanvasTextured.java`、`Reference/BetterQuesting/src/main/java/betterquesting/api2/client/gui/controls/PanelButton.java`、`Reference/BetterQuesting/src/main/java/betterquesting/client/themes/ThemeRegistry.java`
- 结果：当前仓库已新增自有的 root screen / scene / panel container / textured canvas / button / popup / theme registry 最小集合，并补出 `TerminalFrameworkTestScreen` 与 client-only 调试热键用于验证根屏、panel 绘制、按钮回调、popup 开关和主题资源访问；现有 `TerminalService.openTerminal(...)`、`TerminalModule` 的旧 `ModularUI` factory 主链与网络协议均保持不动

### 2026-04-14 - 产出 BetterQuesting 风格 UI framework 第二阶段打开链执行 Prompt

- 主题：补出一份只面向终端打开链改造的第二阶段执行 prompt，明确本轮只把 terminal 从“服务端直接开 `ModularUI`”推进到“服务端授权 + 客户端开自有 screen”的最小闭环
- 影响范围：`docs/terminal-betterquesting-ui-phase2-open-chain-prompt-2026-04-14.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：第一阶段 framework 地基已经落下，下一步最关键的不是提前迁银行页和市场页，而是先把新 framework 真正接进 terminal 主链；同时用户已明确当前阶段只按静态验证口径推进，不要求启动游戏做运行态验收
- 引用来源：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase1-framework-prompt-2026-04-14.md`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalMessage.java`、`src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java`
- 结果：现已新增一份 phase 2 prompt，范围收口到新终端打开协议、最小 `TerminalHomeScreen` 占位壳、初始化 snapshot / session model、F8 调试入口收口以及静态编译验证，为后续首页壳和业务页迁移提供正确宿主

### 2026-04-15 - 产出 BetterQuesting 风格 UI framework 第三阶段首页壳执行 Prompt

- 主题：补出一份只面向新 terminal 首页壳和共用组件层的第三阶段执行 prompt，明确本轮只推进 `TerminalHomeScreen`、导航壳、通知宿主、popup 宿主与共用 panel 组件，不提前迁业务页
- 影响范围：`docs/terminal-betterquesting-ui-phase3-home-shell-prompt-2026-04-15.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 2 已经把 terminal 打开链切到“客户端请求、服务端授权、客户端开 `TerminalHomeScreen` 占位根屏”，下一步最关键的不是抢先迁银行页和市场页，而是先把新首页壳做成后续所有业务页的真正宿主
- 引用来源：`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`、`docs/terminal-betterquesting-ui-phase2-open-chain-prompt-2026-04-14.md`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java`、`src/main/java/com/jsirgalaxybase/terminal/client/TerminalClientScreenController.java`
- 结果：现已新增一份 phase 3 prompt，范围收口到首页壳结构、顶栏 / 左侧导航 / 主体区、全局通知与 popup 挂载位、共用 shell / panel 组件抽取以及静态编译验证，为后续银行页与市场页迁移提供稳定壳层

### 2026-04-05 - 收口终端表单双端同步缺口并沉淀排障文档

- 主题：把终端里所有“客户端输入或选择后，再由服务端 synced action 消费”的字段统一改成双端同步注册，而不是只做本地 getter/setter 绑定
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`docs/terminal-sync-form-regression-2026-04-05.md`、`docs/WORKLOG.md`
- 原因：上一步虽然补回了 `syncManager.syncValue(...)` 注册，解决了输入时崩溃，但银行转账实测仍出现“GUI 提交后服务端不处理”；继续排查后确认根因是可编辑字段需要区分客户端缓存和服务端真实会话值，否则服务端动作仍可能读到空值或旧值
- 结果：银行转账表单、市场商品选择、限价/即时交易输入、取消订单与领取托管等动作参数都改成客户端缓存 + 服务端 getter/setter 的双端同步模式，并补一份单独 bug 文档供后续排障复用

### 2026-04-05 - 恢复双端同步后的终端按钮门禁

- 主题：修复银行转账按钮与市场确认按钮在双端同步改造后长期被客户端误判为不可用的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankPageBuilder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`、`docs/WORKLOG.md`
- 原因：可编辑字段切到客户端缓存 + 服务端 getter/setter 之后，页面按钮的 enabled 判定如果仍读取本地 sessionController，就会一直看到旧空值，表现成“按钮消失”或始终灰掉
- 结果：银行转账、市场限价/即时单、撤单/提取、存入以及兑换确认等入口现在统一基于当前 sync 值判定；同时补齐盘口价格预填时对 sync 文本框的直接回写，避免按钮和输入状态再次脱节

### 2026-04-05 - 市场终端从 page-first 收口为 asset-first 导航壳

- 主题：把市场终端正式切成标准商品、定制商品、汇率三条独立状态链，并把三类子页都改成先找对象再进详情的节奏
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalCustomMarket*.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeMarket*.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketServiceTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalCustomMarketSessionControllerTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeMarketSessionControllerTest.java`、`docs/WORKLOG.md`
- 原因：此前终端虽然已经完成三市场路由拆分，但运行时状态仍然集中在统一 market controller/snapshot 里，custom 仍停留在说明页，exchange 仍更像手持说明板，不符合 asset-first 终端目标
- 结果：标准商品市场默认停留在商品浏览层，不再自动选中首个商品；定制商品市场新增 listing-first 的浏览范围、详情与 buy/cancel/claim 确认链；汇率市场新增明确的兑换标的入口与详情层；终端总装配也改成标准商品 / 定制商品 / 汇率三套独立 controller + sync binder，并补了对应回归测试

### 2026-04-05 - 修复银行失败反馈不出泡泡与 DevB 开户抢跑崩溃

- 主题：修正银行终端在 GUI 打开时 HUD 泡泡被全局屏蔽的问题，并去掉开户按钮对未初始化 `InteractionSyncHandler` 的依赖
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalWidgetFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankPageBuilder.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalHudOverlayHandler.java`、`docs/WORKLOG.md`
- 原因：一方面 HUD overlay 之前在任何 `currentScreen != null` 的情况下都会停止渲染，所以终端内触发的银行失败反馈虽然进入了通知队列，却不会冒泡显示；另一方面开户按钮直接绑定 `InteractionSyncHandler`，在 ModularUI sync manager 尚未完成初始化时被 DevB 抢先点击，会触发 `Sync handler is not yet initialised!`
- 结果：银行开户改成普通本地按钮 + synced action 提交，不再走会抢跑的 `InteractionSyncHandler`；银行转账也改成只展示服务端真实反馈；终端所用的 ModularUI 屏幕打开时 HUD 泡泡现在仍可渲染，转账失败和开户结果都能在 GUI 期间直接看到

### 2026-04-05 - 统一终端 GUI 回归事故文档

- 主题：把终端银行 GUI、转账、sync、按钮门禁、失败提示与开户点击崩溃这整套连续回归，从多份零散事故文档统一收口到单一文档
- 影响范围：`docs/terminal-gui-regression-chain-2026-04-05.md`、`docs/terminal-g-key-fatal-sync-incident-2026-04-04.md`、`docs/terminal-sync-form-regression-2026-04-05.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：之前事故记录已经拆成“G 键 fatal 断线”和“表单双端同步回归”两份文档，再叠加 WORKLOG 里的按钮门禁与 HUD 泡泡问题，后续排障需要来回跳文档，信息已经碎片化
- 结果：新增一份统一事故文档，按整条回归链重新整理银行 GUI、转账、按钮、开户与失败反馈问题；原先两份事故文档改成并入说明；docs 索引同步指向新的统一入口

### 2026-04-04 - 定制商品市场最小挂牌链 v1 最后一轮收口

- 主题：补齐定制商品市场最小挂牌链 v1 的完结动作、单件边界和市场 JDBC 列级 fail-fast 校验
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/custom-market-minimal-model.md`、`docs/custom-market-minimal-listing-chain-prompt-2026-04-03.md`、`docs/market-postgresql-ddl.sql`、`ops/sql/migrations/20260404_002_align_custom_market_single_item_claim_completion.sql`、`docs/WORKLOG.md`
- 原因：上一轮实现已经落地最小挂牌、浏览、购买、下架与 pending 主链，但仍存在 `COMPLETED` 不可达、代码仍允许堆叠挂牌、市场 JDBC 只校验表不校验列这 3 个真实收口缺口
- 引用来源：`docs/custom-market-minimal-listing-chain-tail-close-prompt-2026-04-04.md`、`docs/custom-market-minimal-model.md`、`ops/sql/migrations/20260404_001_add_custom_market_minimal_listing_chain.sql`
- 结果：新增买家 `claim` 完结动作，把交付状态闭环到 `COMPLETED` 并清空双方 pending；发布与快照统一收紧为单件挂牌；市场 JDBC 现在和银行一样对缺列 schema 直接 fail-fast，并提示运维运行 `scripts/db-migrate.sh`

### 2026-04-04 - 验收补出定制商品市场最小挂牌链收口 Prompt

- 主题：在定制商品市场最小挂牌链 v1 验收后，补出只针对剩余闭环缺口的收口 prompt
- 影响范围：`docs/custom-market-minimal-listing-chain-tail-close-prompt-2026-04-04.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前实现已经具备挂牌、浏览、购买、下架与 pending 主链，但验收发现仍存在交付状态机未闭环、单件商品边界与实现不一致、市场 JDBC 缺少 fail-fast 列级 schema 校验这 3 个真实收口缺口
- 引用来源：`docs/custom-market-minimal-listing-chain-prompt-2026-04-03.md`、`docs/custom-market-minimal-model.md`
- 结果：新增一份窄范围收口 prompt，要求下一轮只补 pending 到 completed 的完结动作、统一单件商品边界、以及市场 JDBC 的 fail-fast schema 校验，不再继续扩大功能面

### 2026-04-04 - 定制商品市场最小挂牌链 v1 正式落地

- 主题：为定制商品市场补上独立于标准商品订单簿的最小挂牌、快照、购买、下架与 pending 审计主链
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/market-postgresql-ddl.sql`、`docs/custom-market-minimal-model.md`、`ops/sql/migrations/20260404_001_add_custom_market_minimal_listing_chain.sql`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前仓库已经明确三市场分工，但定制商品市场此前没有正式实现；如果继续把非标商品硬塞进标准商品订单簿，会再次模糊标准商品市场与定制商品市场的边界
- 引用来源：`docs/custom-market-minimal-listing-chain-prompt-2026-04-03.md`、`docs/market-three-part-architecture.md`、`../../Docs/市场经济推进.md`、`../../Docs/下次对话议程.md`
- 结果：新增 `CustomMarketListing / ItemSnapshot / TradeRecord / AuditLog` 和对应 JDBC 仓储、DDL、migration、应用服务与兼容命令入口；发布时保存手持物快照，购买后进入定制商品市场自己的 `BUYER_PENDING_CLAIM` 语义，并可通过 `market custom pending` 查看卖家/买家侧待完结记录

### 2026-04-03 - 银行 PostgreSQL 改为显式版本化迁移与 fail-fast schema 校验

- 主题：把银行数据库结构变更从“应用启动时隐式补列”调整为“运维显式执行版本化 migration，应用启动只做 fail-fast 校验”
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankingInfrastructureFactory.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`scripts/db-migrate.sh`、`ops/sql/migrations/20260403_001_align_banking_ledger_entry_frozen_balances.sql`、`docs/postgresql-schema-migrations.md`、`docs/postgresql-local-setup-and-migration.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：数据库结构漂移不应由应用在启动时静默修复；更稳妥的做法是使用可审计的版本化 migration，在停服窗口由运维显式执行，应用只负责在发现旧 schema 时拒绝启动并给出升级入口
- 结果：新增 `scripts/db-migrate.sh` 和版本化 migration 目录/记录表，本地开发库已通过正式 migration 入口完成修复；银行 JDBC 初始化现在会在 schema 过旧时直接提示运维运行 migration，相关 PostgreSQL 集成测试也已改成校验 fail-fast 行为

### 2026-04-03 - 银行 JDBC 基础设施补上旧版 ledger_entry 列自动修复与列级校验

- 主题：修复本地银行转账在写入 `ledger_entry` 时因旧 PostgreSQL 表结构缺列而失败的问题，并把 schema 校验前移到基础设施初始化阶段
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankingInfrastructureFactory.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`docs/WORKLOG.md`
- 原因：旧开发库的 `ledger_entry` 缺少 `frozen_balance_before` / `frozen_balance_after`，但启动阶段此前只验证“表存在”，没有验证“列齐全”，导致问题直到实际转账写库时才暴露
- 结果：银行 JDBC 基础设施初始化现在会先对旧版 `ledger_entry` 自动补齐冻结余额列，再对核心表执行列级校验；新增 PostgreSQL 集成测试，验证删掉这两列后初始化仍能自动修复并正常启动

### 2026-04-03 - 本地联调脚本改为先起 client 再进 server 控制台

- 主题：调整本地三进程联调脚本的启动顺序和前台行为，方便在服务端控制台直接给在线测试号发放初始资金和做转账联调
- 影响范围：`scripts/start-local-test-stack.sh`、`docs/WORKLOG.md`
- 原因：当前银行管理员命令和玩家间转账都依赖在线玩家身份解析；先让 `DevA` / `DevB` 等测试号进入客户端，再把当前终端切到前台 `runServer` 控制台，才能边看服务端日志边直接执行管理员资金操作
- 结果：脚本现在会先后台启动两个 client，再以前台方式启动 `runServer` 并把当前终端附着到服务端控制台；脚本退出时仍会清理它拉起的 client 进程

### 2026-04-03 - 编译并实装最新 JsirGalaxyBase 到实际 client/server mods 目录

- 主题：将当前标准商品市场目录边界收口后的最新构建产物编译完成，并直接实装到真实 client/server 的 mods 目录
- 影响范围：`docs/WORKLOG.md`
- 原因：本轮代码与测试已经完成，但本地 `runServer` 开发运行时在 FML 扫描 `org.antlr:antlr4:4.13.2` 时出现 class 读取异常，无法把“已编译产物可用”继续建立在 dev runtime 成功拉起上，因此改走真实 mods 目录实装路径
- 结果：
  - 使用 `assemble` 成功产出最新正式 jar：`build/libs/jsirgalaxybase-7545ce9-main+7545ce9201-dirty.jar`
  - 已将同一构建实装到客户端 `CustomClient/Galaxy GTNH 2.8.4/instances/Galaxy GTNH 284/.minecraft/mods/` 与服务端 `GroupServer/Galaxy_GTNH284_S1/mods/`
  - 双端落地文件大小一致，均为 `516806` bytes，可确认当前 client/server 已使用同一份 JsirGalaxyBase 构建产物等待后续启动生效

### 2026-04-03 - 标准商品市场目录边界改为单一运行时事实来源

- 主题：把标准商品市场目录 decision 从“命令层、终端层、服务层各自 new 默认目录”收口为同一运行时事实来源
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketServiceTest.java`、`docs/standardized-market-catalog-boundary.md`、`docs/WORKLOG.md`
- 原因：上一阶段虽然已经建立正式目录对象和统一语义，但命令层与终端层仍各自持有默认目录实例，未来一旦运行时目录来源或版本变化，仍可能与真实服务边界分叉
- 引用来源：`docs/standardized-market-catalog-boundary-tail-close-prompt-2026-04-03.md`、`docs/standardized-market-catalog-boundary.md`
- 结果：
  - `InstitutionCoreModule` 现在显式暴露运行时目录检查入口，命令层统一经由运行时 spot market service 取目录 decision
  - `TerminalMarketService` 的商品浏览、选中商品详情、下单、即时成交、手持存入判定已切到运行时目录 decision；默认目录只保留为运行时离线时的窄回退
  - 新增命令层与终端层测试，证明上层会跟随注入的运行时目录 version/source/reject decision，而不是依赖本地默认目录碰巧一致

### 2026-04-03 - 产出定制商品市场最小挂牌链分阶段 Prompt

- 主题：为三市场执行顺序中的下一阶段补出定制商品市场最小挂牌链实现 prompt，并明确按阶段逐步落地的边界
- 影响范围：`docs/custom-market-minimal-listing-chain-prompt-2026-04-03.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：标准商品市场目录边界已经收口，下一阶段不应继续扩标准商品市场或提前拆 MARKET 总入口，而应开始定义定制商品市场自己的挂牌、成交、待领取与交付留痕主链
- 引用来源：`docs/market-three-part-architecture.md`、`../../Docs/下次对话议程.md`、`../../Docs/市场经济推进.md`
- 结果：新增一份面向定制商品市场的正式实现 prompt，要求按“模型与 DDL -> 应用服务 -> 兼容入口 -> 测试与文档”逐阶段推进，并明确禁止复用标准商品市场订单簿与统一仓储模型来硬凑非标商品交易

### 2026-04-03 - 验收补出标准商品市场目录边界收口 Prompt

- 主题：在本轮验收后补出一个只针对“命令层、终端层、服务层仍各自持有默认目录实例”的收口 prompt
- 影响范围：`docs/standardized-market-catalog-boundary-tail-close-prompt-2026-04-03.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：标准商品市场正式目录对象、来源分层、服务层准入和测试主体都已成立，但命令层与终端层仍直接 `createDefaultCatalog(...)`，还没有完全收口成单一运行时目录边界
- 结果：新增一份窄范围收口 prompt，要求把目录 decision 收口为同一运行时事实来源，并补测试证明命令层与终端层不会再和真实服务边界分叉

### 2026-04-03 - 标准商品市场正式目录与准入边界落地

- 主题：把标准商品市场从直接依赖 GregTech 金属临时目录，推进为拥有正式目录版本、准入决策和来源分层的运行时边界
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/application/`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/`、`src/test/java/com/jsirgalaxybase/modules/core/market/infrastructure/jdbc/MarketPostgresIntegrationTest.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/standardized-market-catalog-boundary.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：当前标准商品市场撮合和仓储链已经能跑，但“哪些商品允许进入市场”仍主要被 `GregTechStandardizedMetalCatalog` 这类临时适配实现直接决定，导致正式制度边界没有独立出来
- 引用来源：`docs/market-three-part-architecture.md`、`../../Docs/市场经济推进.md`、`../../Docs/下次对话议程.md`
- 结果：
  - 新增 `StandardizedMarketCatalogVersion`、`StandardizedMarketCatalogEntry`、`StandardizedMarketAdmissionDecision`、`StandardizedMarketAdmissionReason`、`StandardizedMarketCatalogService`、`StandardizedMarketCatalogSource`
  - 当前默认目录版本已固定为 `standardized-spot-catalog-v1`
  - `GregTechStandardizedMetalCatalog` 已下沉为 `目录来源适配器`，不再承担正式制度边界语义
  - `StandardizedSpotMarketService`、命令层与终端层已统一走新的目录准入主路径，并可带出目录版本、准入 reason 与来源标识
  - 新增标准商品目录边界说明文档，并补齐目录服务、来源桥接、服务层拒绝语义与命令层输出测试

### 2026-04-03 - 产出标准商品市场商品目录与正式准入边界 Prompt

- 主题：为汇率市场规则层收口后的下一阶段，产出标准商品市场商品目录与正式准入边界实现 prompt
- 影响范围：`docs/standardized-market-catalog-boundary-prompt-2026-04-03.md`、`docs/README.md`、`docs/WORKLOG.md`
- 原因：汇率市场规则层 v1 已完成收口，正式执行顺序应进入“标准商品市场商品目录与正式准入边界”；当前仓库仍主要依赖 `GregTechStandardizedMetalCatalog` 作为临时准入来源，需要下一阶段 prompt 明确把正式目录边界与临时适配来源拆开
- 引用来源：`docs/market-three-part-architecture.md`、`../../Docs/市场经济推进.md`、`../../Docs/下次对话议程.md`
- 结果：新增一份只针对标准商品市场商品目录与正式准入边界的 prompt，并同步到 docs 索引，作为汇率市场之后的下一阶段实现依据

### 2026-04-03 - 汇率市场兼容 quote 桥收口禁兑结果过早抛错

- 主题：修补汇率市场兼容桥在 `quote hand` 上把禁兑报价过早抛成异常的问题，确保禁兑任务书硬币也能返回正式规则层报价
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangeService.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangeServiceTest.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/WORKLOG.md`
- 原因：正式规则层已经能返回 `DISALLOWED` 报价，但兼容桥此前把 preview 与 execute 共用成“必须可执行”路径，导致 `/jsirgalaxybase market quote hand` 无法展示正式规则字段，并把禁兑误退化成普通拒绝
- 引用来源：`docs/exchange-market-rules-layer-v1-tail-close-prompt-2026-04-03.md`、`docs/market-three-part-architecture.md`
- 结果：
  - 兼容桥已拆开 `preview` 与 `execute` 判断：报价路径允许返回 `DISALLOWED` 正式报价，执行路径仍然在吞物前拒绝不可执行输入
  - `/jsirgalaxybase market quote hand` 现在会输出 `pair`、`ruleVersion`、`limitStatus`、`reasonCode`、`notes` 等正式字段，不再把禁兑误报成“非汇率市场资产”
  - `/jsirgalaxybase market exchange hand` 对同类禁兑输入仍保持拒绝执行语义，且错误消息来自正式规则层 note
  - 新增桥接层测试与命令输出测试，并通过针对性 Gradle 验证

### 2026-04-03 - 验收补出汇率市场兼容 quote 桥收口修补 Prompt

- 主题：在本轮验收后补出一个只针对“禁兑报价被兼容桥过早抛错”的收口修补 prompt
- 影响范围：`docs/exchange-market-rules-layer-v1-tail-close-prompt-2026-04-03.md`、`docs/WORKLOG.md`
- 原因：正式规则层已经能返回 `DISALLOWED` 报价，但兼容桥仍会把这类结果提前抛成异常，导致 `/jsirgalaxybase market quote hand` 无法展示正式 `limitStatus / reasonCode / notes`
- 结果：新增一份窄范围修补 prompt，明确要求把 preview 与 execute 判断拆开，让 `quote hand` 能展示禁兑正式字段，而 `exchange hand` 仍保持拒绝执行

### 2026-04-03 - 汇率市场正式规则层 v1 落地并保留旧命令兼容入口

- 主题：把任务书硬币兑换从“能跑的固定规则入口”收口为汇率市场正式规则层 v1，同时保留旧 `/jsirgalaxybase market quote hand` 与 `/jsirgalaxybase market exchange hand` 作为兼容入口
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/application/`、`src/main/java/com/jsirgalaxybase/modules/core/market/domain/`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`docs/WORKLOG.md`
- 原因：三市场结构已经定稿，当前最先要落的不是继续扩标准商品市场，而是把已有任务书硬币兑换正式归位为 `汇率市场` 规则层，并把旧 MARKET 文案从“商品市场一期”改正为“汇率市场兼容入口”
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/下次对话议程.md`、`docs/market-three-part-architecture.md`
- 结果：
  - 新增正式 `ExchangeMarket*` 规则对象与 `ExchangeMarketService`，把 pair、ruleVersion、limitPolicy、quote result、execution result 明确建模
  - 旧 `TaskCoinExchangeService` 已改为手持物品兼容桥，不再直接承载汇率市场主语义
  - 命令层和终端首页文案已改成“汇率市场兼容入口”，并在报价/执行输出中补充 pair、ruleVersion、limitStatus、reasonCode 等正式字段
  - 修复了任务书硬币不支持档位识别把 `IV` 误判成高阶禁用的问题
  - 已通过针对性测试：`GalaxyBaseCommandTest`、`TaskCoinExchangePlannerTest`、`ExchangeMarketServiceTest`

### 2026-04-03 - 新增本地三进程联调启动脚本

- 主题：为 dedicated server + 双 client 人工联调新增用户可自行执行的一键启动脚本，避免再依赖 agent 后台终端持有进程
- 影响范围：`scripts/start-local-test-stack.sh`、`docs/WORKLOG.md`
- 原因：用户需要自己在本地 shell 中稳定维持三进程联调，并且每次启动前都要自动清理旧的 client/server 进程，避免残留实例和共享 gameDir 干扰测试
- 结果：
  - 新增 `scripts/start-local-test-stack.sh`，会先搜索并杀掉当前工作区相关的 runServer/runClient 进程
  - 脚本按 `server -> client A -> client B` 顺序启动，并为两个 client 显式使用独立 gameDir
  - 脚本在前台作为 supervisor 持有三进程，按 `Ctrl-C` 会统一清理

### 2026-04-03 - 市场三分结构正式设计落库并归位旧实现

- 主题：把市场重新收口为标准商品市场、定制商品市场、汇率市场三条正式产品线，并把现有代码残留归位到正式设计文档
- 影响范围：`docs/market-three-part-architecture.md`、`docs/README.md`、`../Docs/市场经济推进.md`、`../Docs/下次对话议程.md`、`docs/WORKLOG.md`
- 原因：当前仓库虽然已经在制度文档里恢复三市场方向，但仓库内仍缺少一份明确说明“旧单一路线残留属于哪一类市场、哪些应冻结、接下来先做哪条线”的正式设计文档
- 结果：
  - 新增 `docs/market-three-part-architecture.md`，正式写清三类市场边界、共享能力、最小模型拆分、旧实现归位判断和下一阶段执行顺序
  - 明确现有标准化现货代码只属于 `标准商品市场早期残片`，任务书硬币兑换只属于 `汇率市场早期残片`
  - 明确当前还没有真正属于 `定制商品市场` 的正式实现，禁止再把旧 MARKET 页面继续扩成统一大市场
  - 同步更新文档索引与下次议程，把后续优先级改为“先收口三市场执行顺序，再细化汇率和回收规则”

### 2026-04-03 - 三市场下一阶段顺序同步修正

- 主题：把“下一阶段执行顺序”从包含已完成的三市场总设计步骤，修正为从汇率市场正式规则层开始
- 影响范围：`docs/market-three-part-architecture.md`、`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
- 原因：三市场总设计已经正式落库，后续执行顺序应从 `汇率市场正式规则层 -> 标准商品目录与准入边界 -> 定制商品市场最小挂牌链 -> MARKET 总入口拆分` 开始，避免继续把已完成前置步骤写成“下一阶段”
- 结果：正式架构文档与推进文档的下一阶段顺序已统一为从汇率市场正式规则层开始

### 2026-04-03 - 产出汇率市场正式规则层改代码 Prompt

- 主题：为三市场结构验收后的首个代码阶段产出汇率市场正式规则层改代码 prompt
- 影响范围：`docs/exchange-market-rules-layer-code-prompt-2026-04-03.md`、`docs/WORKLOG.md`
- 原因：三市场正式边界和下一阶段顺序已经验收，需要一个明确限制范围的代码 prompt，把当前 `TaskCoinExchange*` 早期入口收口成汇率市场正式规则层
- 结果：新增一份只针对汇率市场正式规则层的代码 prompt，明确禁止继续扩标准商品市场、定制商品市场或 MARKET 总入口

### 2026-04-03 - 撤销金属市场文档方向并恢复三市场结构

- 主题：撤销文档中把市场收窄为金属专场和单一路线的错误方向，统一恢复为标准商品市场、定制商品市场、汇率市场三层结构
- 影响范围：`../Docs/设定.md`、`../Docs/市场经济推进.md`、`../Docs/下次对话议程.md`、`docs/market-gui-phase1-product-detail-prompt-2026-04-02.md`、`docs/market-gui-phase1-tail-fix-prompt-2026-04-03.md`、`docs/banking-java-domain-draft.md`、`.github/agents/GalaxyMod.agent.md`、`docs/WORKLOG.md`
- 原因：用户确认此前讨论的正式市场结构一直是 `标准商品市场 / 定制商品市场 / 汇率市场`，文档里后续把它误收窄成单一商品专场属于方向漂移
- 结果：
  - 主文档已统一改回三市场结构，不再把标准商品市场默认等同于金属专场
  - 两份旧的市场 phase-one prompt 已降级为废弃说明，不再作为后续实现依据
  - 下一阶段已改成先补三市场总设计，再分别推进标准商品市场、定制商品市场和汇率市场边界

### 2026-04-03 - 产出市场三分结构下一阶段 prompt

- 主题：为后续市场开发产出新的下一阶段 prompt，明确从旧单一路线切回三市场结构后的正式执行顺序
- 影响范围：`docs/market-three-part-structure-next-phase-prompt-2026-04-03.md`、`docs/WORKLOG.md`
- 原因：用户要求直接给出下一阶段 prompt，而且当前最重要的不是继续堆旧 MARKET 页，而是先把 `标准商品市场 / 定制商品市场 / 汇率市场` 三条线的边界、共享能力和执行顺序固定下来
- 结果：
  - 新增三市场结构下一阶段 prompt，可直接交给下一轮实现者使用
  - prompt 已明确要求先做三市场总设计与旧实现归位，不再沿旧单一路线继续扩功能

### 2026-04-03 - 市场第一阶段收尾补齐 DDL 与 deposit 恢复收口

- 主题：把旧单一标准商品撮合方案的第一阶段尾差从 Java 语义层推进到 PostgreSQL DDL 与 deposit 恢复路径，避免 AVAILABLE / INVENTORY_DEPOSIT 只停留在枚举里
- 影响范围：`docs/market-postgresql-ddl.sql`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketService.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/MarketRecoveryService.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/`、`src/test/java/com/jsirgalaxybase/modules/core/market/infrastructure/jdbc/`
- 原因：最新审查确认还有两个阻塞点未收口：数据库 check constraint 仍未接受 `AVAILABLE / INVENTORY_DEPOSIT`，且 deposit 失败后会沿通用异常路径把库存语义漂移到错误状态
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/设定.md`
- 结果：
  - PostgreSQL DDL 已同步接受 `market_custody_inventory.custody_status = AVAILABLE` 与 `market_operation_log.operation_type = INVENTORY_DEPOSIT`
  - deposit 失败现在写入专用 recovery metadata，并由 `MarketRecoveryService` 显式收口，不再继续走通用 `escalateGeneric(...)`
  - 新增共享商品目录测试替身与目录校验测试，覆盖“允许的标准商品可通过、普通方块会被拒绝”
  - PostgreSQL 集成测试已切到“先 deposit、再卖出/撤单/claim”的新边界，并新增 deposit DDL 与恢复验证

### 2026-04-03 - 市场第一阶段收尾 prompt 补充阻塞修复与双 client 联调要求

- 主题：把第一阶段收尾 prompt 从“语义修补”补强到“阻塞修复 + 双 client 联调验收”版本
- 影响范围：`docs/market-gui-phase1-tail-fix-prompt-2026-04-03.md`、`docs/WORKLOG.md`
- 原因：最新审查确认除了统一仓储与标准化商品准入外，还存在 DDL 未同步与 deposit 恢复未收口两处阻塞；同时银行转账与市场交易都需要双人在线联调，不能再只靠单 client 自测
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/设定.md`
- 结果：
  - 在 prompt 中新增了 `AVAILABLE / INVENTORY_DEPOSIT` 的 DDL 同步要求
  - 在 prompt 中新增了 deposit 失败恢复或安全回滚必须显式收口的要求
  - 在 prompt 中把“与用户配合的双 client 人工联调”升级为硬性验收条件，明确覆盖银行转账与市场交易链路

### 2026-04-03 - 市场第一阶段收尾修复 prompt

- 主题：为市场终端第一阶段补齐统一仓储与标准化商品准入两处尾巴，产出收尾修复 prompt
- 影响范围：`docs/market-gui-phase1-tail-fix-prompt-2026-04-03.md`、`docs/WORKLOG.md`
- 原因：首轮 MARKET 商品详情页虽然已经接线完成，但审查发现卖单仍依赖手持扣物、标准化商品仍缺少集中准入校验，这两点会直接破坏既定市场边界
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/设定.md`
- 结果：
  - 明确当前已有的是统一市场托管库存与 CLAIMABLE 资产链路，而不是完整的统一仓储可卖库存闭环
  - 要求后续修复优先在现有 market custody inventory 上补 `AVAILABLE` 一类可卖状态，而不是再拆一套平行仓储系统
  - 要求新增集中式标准化商品准入校验，并强制命令层、终端层与服务层统一收口

### 2026-04-03 - 市场终端第一轮旧单一路线商品详情页落地

- 主题：把终端 MARKET 页从静态占位页切到旧单一标准商品撮合方案的商品详情页，并补齐最小本地测试
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/`、`src/main/java/com/jsirgalaxybase/modules/core/market/infrastructure/jdbc/JdbcMarketOrderBookRepository.java`、`src/test/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSessionControllerTest.java`、`docs/WORKLOG.md`
- 原因：市场 GUI 第一轮目标已经从 prompt 明确为“商品点击后的交易详情页”，继续保留只读占位页会阻塞真实市场终端的后续联调与视觉验收
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/设定.md`
- 结果：
  - 新增市场终端的 snapshot、service、session controller、sync binder、sync state 与 page builder，MARKET 页现在可浏览商品、查看买卖盘、提交限价/即时交易、查看个人订单并发起撤单与 CLAIMABLE 提取
  - `TerminalHomeGuiFactory` 已正式接入市场页主面板、6 个确认弹窗和市场 toast/HUD 转发，不再复用旧只读详情页壳
  - 市场订单簿 JDBC 查询现在包含 `PARTIALLY_FILLED`，GUI 可看到剩余可成交深度
  - 新增市场终端本地纯逻辑测试，覆盖输入 sanitize、数量解析与待处理 ID 标记规则

### 2026-04-03 - 修复终端打开即断线的 ModularUI sync 冲突

- 主题：修复按 `G` 打开终端时服务端因 ModularUI sync handler auto/manual 注册冲突而踢线的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`docs/WORKLOG.md`
- 原因：市场页 6 个数值输入框对应的 `StringSyncValue` 被 market binder 手工 `syncValue(...)` 注册后，又被 `TextFieldWidget.value(...)` 作为 auto sync handler 收集，触发 `Old and new sync handler must both be either not auto or auto!`
- 结果：改为与银行页一致，只保留文本框侧的 auto 注册，终端主界面打开时不再因 `jgb_terminal` 包处理中的 sync 冲突被服务端断开

### 2026-04-04 - 修复 G 键打开终端导致 fatal disconnect 的 sync 回退

- 主题：修复按 `G` 打开终端时再次触发的 ModularUI sync auto/manual 混链断线，并补独立事故文档
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java`、`docs/terminal-g-key-fatal-sync-incident-2026-04-04.md`、`docs/WORKLOG.md`
- 原因：市场页 6 个数值输入框的 `StringSyncValue` 又被手工 `syncValue(...)` 注册回 binder，和 `TextFieldWidget.value(...)` 的 auto 注册重新混在同一条 sync 链上，服务端在 `TerminalHomeGuiFactory.open(...)` 期间抛 `Old and new sync handler must both be either not auto or auto!`，客户端因此被 fatal 断开
- 结果：再次移除这 6 个数值输入框的手工 sync 注册，只保留文本框自身的 auto 注册；补充独立事故文档沉淀症状、堆栈、修复方式与复发预防点

### 2026-04-04 - 修复脚本联调环境下银行转账输入框再次触发终端 fatal

- 主题：修复 `scripts/start-local-test-stack.sh` 联调时，按 `G` 打开终端仍因银行页转账输入框重复 sync 注册而 fatal 断线的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSyncBinder.java`、`docs/terminal-g-key-fatal-sync-incident-2026-04-04.md`、`docs/WORKLOG.md`
- 原因：市场页 6 个文本输入框虽然已经移除手工 sync，但银行页的 `bankTransferTargetName`、`bankTransferAmountText`、`bankTransferComment` 仍同时走了 binder 手工 `syncValue(...)` 和 `TextFieldWidget.value(...)` auto sync；终端首页会一并收集 bank 页面控件，因此打开终端时仍会触发同一个 `Old and new sync handler must both be either not auto or auto!`
- 结果：移除银行转账 3 个输入框的手工 sync 注册，保留文本框自身 auto sync；事故文档同步扩大到 market 与 bank 两类文本输入控件，避免以后只修一半

### 2026-04-02 - 市场 GUI 第一轮商品详情页 prompt

- 主题：为终端市场页从只读占位升级到“点击商品后的交易详情页”产出第一轮实现 prompt
- 影响范围：`docs/market-gui-phase1-product-detail-prompt-2026-04-02.md`、`docs/WORKLOG.md`
- 原因：终端第三轮工程收口已经完成，下一阶段不应继续空谈市场页，而要把用户点击商品后的真实交易详情页结构、动作和验收范围写成可直接执行的实现规格
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/设定.md`、`Reference/VendingMachine/src/main/java/com/cubefury/vendingmachine/blocks/gui/`
- 结果：
  - 明确市场页第一轮目标是商品点击后的交易详情页，而不是完整市场总站
  - 把订单簿、下单区、个人订单区、仓储/冻结/待领取区和规则提示区列为首轮必做
  - 明确 VendingMachine 只作为浏览与状态表达参考，不照搬自动售货机式快捷交易交互

### 2026-04-02 - Terminal GUI 第三轮工程收口与市场前置整理

- 主题：在不重写终端框架的前提下，对终端主工厂、通知语义、Dialog 壳层和纯逻辑测试做市场 GUI 前的最后一轮工程收口
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/`、`src/main/java/com/jsirgalaxybase/terminal/TerminalHudNotificationManager.java`、`src/test/java/com/jsirgalaxybase/terminal/`、`src/test/java/com/jsirgalaxybase/terminal/ui/`、`docs/terminal-plan.md`
- 原因：第二轮以后主要风险已经从“能不能做出来”转成“市场 GUI 接进来后主工厂会不会继续膨胀，以及通知/弹窗/本地表单逻辑是否缺少最小保护”
- 结果：
  - 把银行页面构建、银行本地会话状态、银行 sync 绑定和通用 widget helper 从 `TerminalHomeGuiFactory` 明确拆出
  - `TerminalNotification` 现在优先消费结构化 `TerminalActionFeedback`，银行旧文本推断仅保留为 fallback 兼容桥接
  - `TerminalDialogFactory` 升级为 `TerminalDialogSpec` 驱动，支持 severity、尺寸预设和更稳的长正文/detail lines 容器
  - 新增终端侧纯逻辑测试，覆盖通知构造、HUD 队列行为、银行本地 sanitize/parse 和 Dialog 配置默认值

### 2026-04-02 - Terminal GUI 第三轮精修 prompt：工程收口与市场 GUI 前置整理

- 主题：在终端第一轮排版收口、第二轮通知与弹层能力落地之后，补一份进入市场 GUI 前的稳健精修 prompt
- 影响范围：`docs/terminal-gui-phase3-polish-prompt-2026-04-02.md`、`docs/WORKLOG.md`
- 原因：当前终端已经达到可用且观感明显改善的阶段，下一步主要风险不再是功能缺失，而是主工厂继续膨胀、通知语义仍偏文案驱动、弹窗工厂和终端侧逻辑测试不足，若不先收口会放大市场 GUI 接入成本
- 结果：
  - 输出第三轮 GUI 精修 prompt，范围明确限制为主工厂最小职责拆分、通知结构化优先、Dialog 工厂市场前置升级、终端侧纯逻辑测试补齐
  - 明确本轮不做框架重写、不做过度设计、不直接开始完整市场 GUI
  - 保留客户端与服务端实装、启动并人工目检 GUI 的硬性验收要求

### 2026-04-02 - Terminal GUI 第二轮通知层与确认弹窗落地

- 主题：把终端第二轮 prompt 中的通知层、确认弹窗、终端外可见提示与轻量视觉组件正式接入当前银行终端
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/terminal-plan.md`
- 原因：第一轮已经把布局与文本结构收口，但终端仍缺少可复用通知壳层、真实确认交互和终端外提示路径，导致 GUI 还停留在“静态信息板”阶段
- 结果：
  - 新增统一 `TerminalNotification` 模型与 HUD 通知管理器，银行反馈可同时驱动页内通知与终端外提示
  - 客户端 bootstrap 新增终端 HUD overlay 注册，关闭终端后仍可看到银行结果提示
  - 玩家转账改为先经过确认弹窗，再通过 synced action 提交到服务端，不改既有银行业务语义
  - 银行首页新增物品图标状态卡，并显式给按钮接入点击音反馈

### 2026-04-02 - Terminal GUI 第二轮 prompt：通知层、弹层与轻量视觉组件

- 主题：基于第一轮结构收口后的实际游戏内目检结果，沉淀终端 GUI 第二轮 prompt，推进通知层、弹层交互和基础视觉组件能力
- 影响范围：`docs/terminal-gui-phase2-prompt-2026-04-02.md`
- 原因：第一轮已经证明终端文本与排版明显改善，下一步不应继续停留在静态内容卡片，而要开始补齐通知、确认弹窗、终端外可见提示与轻量视觉能力
- 结果：
  - 输出第二轮 GUI prompt，范围收敛为通知层、二级弹层、最小视觉组件与基础声音反馈
  - 将“编译产物必须实装到客户端和服务端并启动，由人工肉眼验证 GUI 效果”写为硬性验收要求
  - 明确第二轮不直接跳去做完整市场 GUI，而是先沉淀可复用交互壳层

### 2026-04-02 - 沉淀本地实装流程到 GalaxyMod agent

- 主题：把本轮 JsirGalaxyBase 本地编译、dedicated server 拉起、client 联调与人工目检流程写入工作区 agent，供后续复用
- 影响范围：`../../.github/agents/GalaxyMod.agent.md`、`docs/WORKLOG.md`
- 原因：本轮已经验证出一套可重复的本地实装顺序，也踩清了 duplicate mod、后台 cwd 丢失、非阻塞 Forge 噪声与市场表缺失这几个关键坑点，需要沉淀为下次可直接复用的操作规范
- 结果：
  - `GalaxyMod` 不再是占位模板，而是明确面向 GTNH / JsirGalaxyBase 开发与实装的自定义代理
  - 写入了标准本地实装流程、推荐启动命令、运行态检查点与常见坑点
  - 后续再做 client/server 实装时，可以直接按 agent 中的顺序执行，不必重新试错

### 2026-04-02 - Terminal GUI 第一轮结构收口

- 主题：对终端首页与银行子页做第一轮结构化 GUI 收口，减少固定高度卡片与长文本爆框风险
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：当前终端虽然已基于 ModularUI2，但正文区仍然大量依赖固定高度卡片和裸 `TextWidget`，不利于长文本、动态状态和后续市场页/通知层继续生长
- 结果：
  - 新增统一文本 helper，把说明文本、动态摘要和滚动摘要从零散裸 `TextWidget` 收口到统一策略
  - `createSectionShell(...)` 现在默认支持内容自适应，仅在少数视觉统一块保留固定高度
  - 页头 lead、toast 正文、bullet panel、data row 与银行说明块改为更偏内容优先的布局
  - 不改终端入口、页路由、银行同步模型与现有银行业务语义

### 2026-04-02 - Terminal GUI 第一轮重构方案与 ModularUI2 API 评估

- 主题：沉淀终端 GUI 的第一轮结构化重构 prompt，并把 ModularUI2 API 能力评估写入文档供后续实现参考
- 影响范围：`docs/terminal-modularui2-gui-refactor-prompt-2026-04-02.md`
- 原因：当前终端页面虽然已切到 ModularUI2，但仍存在长文本溢出、固定高度卡片过多、结构不利于后续市场 GUI 和通知层扩展的问题，需要先形成基于真实框架能力的重构方案
- 结果：
  - 确认当前 GUI 主体基于 ModularUI2 流式布局，不是纯绝对坐标系统
  - 确认框架已具备富文本、滚动文本、贴图、物品展示、对话框、子面板、tooltip 和基础点击音接口
  - 输出第一轮 GUI 重构 prompt，明确本轮先收口文本与布局，再为后续终端桌面化扩展预留结构

### 2026-04-02 - 第二层旧单一商品撮合方案命令层尾修

- 主题：补齐旧单一标准商品撮合方案命令层 cancel/claim 测试覆盖，并修正买单撤销后的释放金额回显
- 影响范围：`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketService.java`、`src/test/java/com/jsirgalaxybase/command/GalaxyBaseCommandTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketServiceTest.java`
- 原因：第二层运行时接线已完成，本轮只剩命令层覆盖和玩家回显这类尾修项，避免把已释放冻结金误报成 `0`
- 结果：
  - 新增卖单撤销、买单撤销、claim 命令分发测试
  - 买单撤销回显改为使用 service 返回的真实 `releasedFunds`
  - 不改运行时装配、不扩阶段边界，只收口命令层可测性和玩家提示准确性

### 2026-04-02 - 第二层旧单一标准商品撮合方案接入服务器运行时

- 主题：把已完成的旧单一标准商品撮合服务层能力正式接入 dedicated-server 运行时、玩家命令入口与人工恢复触发
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/`、`src/test/java/com/jsirgalaxybase/modules/core/`、`src/test/java/com/jsirgalaxybase/command/`、`../../Docs/市场经济推进.md`
- 原因：上一轮虽然已经补齐第二层旧单一商品撮合方案的服务、JDBC 和恢复闭环，但服务器运行时还没有真正挂载市场服务，也没有玩家入口和管理员恢复触发，离“可实际使用”还差最后一段接线
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/技术边界文档.md`
- 结果：
  - `InstitutionCoreModule` 现在会在 dedicated-server 路径下同时装配 banking 与 shared JDBC market runtime
  - `GalaxyBaseCommand` 新增第二层现货命令入口，保留原有 phase-1 `quote/exchange` 路径不混用
  - 卖单创建入口现在会先扣除玩家手持标准化物，再调用市场服务，失败时原样回滚，避免虚空卖单
  - 新增管理员 `market recover` 手动恢复触发与启动时轻量恢复扫描挂点
  - 补充运行时装配测试与命令分发测试，覆盖 dedicated-server 装配、卖单扣物/失败回滚、买单分发、claim 列表与恢复入口

### 2026-04-01 - 旧单一标准商品撮合方案补齐买单恢复与 CLAIMABLE 提取闭环

- 主题：补齐旧单一标准商品撮合方案的买单冻结资金失败恢复闭环与 `CLAIMABLE` 资产提取写路径
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/modules/core/market/`、`docs/market-postgresql-ddl.sql`、`../../Docs/市场经济推进.md`
- 原因：上一轮只完成了最小买卖闭环，买方冻结金异常恢复与玩家真正提取 `CLAIMABLE` 资产仍未收口，存在一致性和可用性缺口
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/技术边界文档.md`
- 结果：
  - 新增 `recoveryMetadataKey`，把买单冻结金恢复与 claim 恢复线索显式落到操作日志
  - `MarketRecoveryService` 可释放未完成买单剩余冻结资金，并把订单状态收口到 `CANCELLED / FILLED`
  - 新增 `ClaimMarketAssetCommand`、`CLAIMING / CLAIMED` 状态与真实 claim delivery port
  - claim 成功后托管资产进入 `CLAIMED`，安全失败则恢复到 `CLAIMABLE`
  - 补齐市场单测与 PostgreSQL 集成测试，覆盖买单恢复和 claim 写路径

### 2026-03-29 - 初始化仓库与工作记录机制

- 主题：初始化本地 git 仓库与 work log 机制
- 影响范围：仓库根目录
- 原因：为后续 GitHub 上传、版本管理和持续开发记录做准备
- 结果：
  - 在 `JsirGalaxyBase` 仓库下初始化了本地 git 仓库
  - 建立本 work log 作为统一开发记录入口
  - 补录初次对话形成的架构和制度上下文
  - 后续每次代码更改都应在此追加简要记录

### 2026-03-29 - 排除外层仓库中的 Reference 目录

- 主题：将 `Reference/` 排除出外层 git 仓库
- 影响范围：`.gitignore`
- 原因：`Reference/` 下包含多个独立 git 仓库，直接加入外层仓库会形成嵌套仓库或 gitlink，上传到 GitHub 后不适合作为当前项目源码的一部分
- 结果：
  - 外层仓库不再跟踪 `Reference/`
  - `Reference/` 继续保留在本地，作为开发参考源码使用

### 2026-03-29 - 合并根文档并停用自动化 workflow

- 主题：重写根 README，建立 `docs/` 目录，并停用 GitHub Actions workflow
- 影响范围：`README.md`、`docs/`、`.github/workflows/`
- 原因：不再需要把架构拆成单独根文档，同时希望把面向玩家和协作者的基础材料集中到 `docs/` 目录中，并关闭当前自动化编译流程
- 结果：
  - 根 `README.md` 合并了原先独立架构文档的核心内容
  - `WORKLOG.md` 迁移到 `docs/WORKLOG.md`
  - 新增 `docs/README.md` 作为文档入口
  - 删除现有自动化编译与 release workflow 文件

### 2026-03-29 - 将项目正式更名为 JsirGalaxyBase

- 主题：将项目名称从 `CustomMod` 统一更名为 `JsirGalaxyBase`
- 影响范围：Java 包名、主类名、命令类名、Gradle 模组元数据、配置路径、README 与 docs 文档
- 原因：`CustomMod` 过于临时和泛化，无法准确承载当前制度核心加能力模块的长期定位；`JsirGalaxyBase` 更适合作为正式项目名
- 结果：
  - 根包切换为项目正式包名
  - 主类切换为 `GalaxyBase`
  - 命令切换为 `/jsirgalaxybase`
  - 模组 `modid` 切换为 `jsirgalaxybase`
  - 文档标题与项目引用同步更新

### 2026-03-29 - 排除本机环境名并建立正式命名约定

- 主题：从代码与元数据中移除本机环境名痕迹，并建立正式命名约定
- 影响范围：Java 根包、Gradle 元数据、mcmod 作者字段、README、工作目录命名说明
- 原因：本机环境名不应进入模组正式命名空间；需要把工程名、模组名、包名和仓库名分开定义清楚
- 结果：
  - Java 根包统一为 `com.jsirgalaxybase`
  - Gradle `modGroup` 与生成的 `Tags` 类路径同步改为 `com.jsirgalaxybase`
  - `mcmod.info` 作者显示改为 `Jsir2022`
  - README 新增命名约定章节，明确仓库名、目录名、模组名、`modid` 和包名的分工

### 2026-03-29 - 工作目录与 GitHub 仓库名对齐

- 主题：将本地工作目录从 `CustomMod` 改为 `JsirGalaxyBase`
- 影响范围：本地仓库目录路径
- 原因：保持本地工作目录与 GitHub 仓库名一致，减少工程名、目录名和远端仓库名之间的混淆
- 结果：
  - 本地仓库目录已改为 `JsirGalaxyBase`
  - 当前仓库名、工作目录名和 GitHub 仓库名保持一致
  - 模组运行时名称继续保持为 `JsirGalaxyBase`

### 2026-03-29 - 统一重命名为 JsirGalaxyBase / GalaxyBase

- 主题：将仓库、模组和文档名称统一切换为 `JsirGalaxyBase`，并把代码主类简写为 `GalaxyBase`
- 影响范围：`README.md`、`docs/`、Gradle 模组元数据、Java 根包、Forge 主类、命令类与本地目录命名
- 原因：用户要求统一正式名称，减少旧命名和运行时代号混杂；同时保留代码类名的可读性
- 结果：
  - 模组展示名与 `modid` 改为 `JsirGalaxyBase` / `jsirgalaxybase`
  - Java 根包改为 `com.jsirgalaxybase`
  - Forge 主类改为 `GalaxyBase`
  - 根命令改为 `/jsirgalaxybase`
  - README 与 docs 的命名约定同步更新
  - 本地工作目录已切换为 `JsirGalaxyBase`
  - GitHub 新仓库地址 `git@github.com:Jsir2022/JsirGalaxyBase.git` 当前尚不存在，远端切换需等待 GitHub 侧先完成仓库重命名

### 2026-03-29 - 确认终端入口与终端壳实施方案

- 主题：确定终端第一阶段采用快捷键入口加背包按钮入口，并先落稳定打开链与占位终端壳
- 影响范围：`docs/terminal-plan.md`、`README.md`、`docs/README.md`
- 原因：终端将承担职业、市场、福利和公共服务的统一入口，必须先把入口链、服务端鉴权与可替换 UI 壳分离清楚
- 结果：
  - 确认终端第一阶段先实现快捷键打开与背包按钮打开
  - 确认两条入口共用同一条服务端打开链
  - 确认 Pad 物品入口延后到后续阶段
  - 确认当前先使用占位终端壳，后续再替换为 `ModularUI 2`

### 2026-03-29 - 终端首页切换到 ModularUI 2

- 主题：移除旧占位 GUI，改为真实的 `ModularUI 2` 终端首页壳
- 影响范围：`dependencies.gradle`、`src/main/java/com/jsirgalaxybase/modules/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/ui/`
- 原因：当前入口链已经稳定，下一步需要把终端正式切到 `ModularUI 2`，为职业、贡献度、声望、公共任务和市场摘要首页建立可扩展的同步面板
- 结果：
  - 新增 `ModularUI2` 依赖并注册终端 UI 工厂
  - 服务端打开链改为 `GuiManager.open(...)`，继续保持服务端权威
  - 删除旧 `IGuiHandler` 和占位容器 / 占位界面实现
  - 新增终端首页快照与只读首页面板，接入职业、贡献度、声望、公共任务、市场摘要五项展示

### 2026-03-29 - 终端补左侧导航与正式分页壳

- 主题：把终端从单页总览扩成左侧导航加右侧内容区的正式框架
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/`、`docs/WORKLOG.md`
- 原因：后续职业、公共事务、市场等页面需要统一壳，不能继续停留在单页演示态
- 结果：
  - 终端改为左侧导航与右侧分页内容结构
  - 首页继续保留制度摘要，同时新增职业、公共、市场三页的只读占位内容
  - 分页状态已接入 `IntSyncValue`，后续可在同一终端壳内继续扩页而不改入口链

### 2026-03-29 - 终端改为宽屏控制台风格

- 主题：参考 AE2 终端比例，重做终端观感与窗口尺寸

### 2026-03-30 - 整理 Ubuntu 24 向日葵安装复用资料

- 主题：补充 Ubuntu 24 下向日葵 15.2.0.63064 的安装记录、复用脚本与依赖包整理目录
- 影响范围：`../../Docs/sunlogin-ubuntu24/`、`docs/WORKLOG.md`
- 原因：本次实际排查出 Ubuntu 24 官方仓库缺失旧版 `libgconf-2-4` 依赖，且 Wayland 会导致向日葵被控黑屏，需要把安装包、步骤和 Xorg 配置整理成可复用资料，便于后续其他机器快速落地
- 结果：
  - 新增 `Docs/sunlogin-ubuntu24/README.md` 记录完整安装与黑屏修复流程
  - 新增 `Docs/sunlogin-ubuntu24/install_sunlogin_ubuntu24.sh` 作为复用脚本
  - 将新版安装包与所需旧依赖 `.deb` 统一整理到 `Docs/sunlogin-ubuntu24/packages/`
  - 记录 GDM 关闭 Wayland、改走 Xorg 的关键配置点
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：上一版窗口过小、信息层级过弱、默认按钮味太重，不适合作为长期制度终端壳
- 结果：
  - 终端窗口显著放大，改成更接近控制台的宽屏比例
  - 新增顶部状态带、侧栏导航、首页摘要卡片和分区内容卡
  - 视觉风格从默认灰底面板改为更偏控制台的深色块面布局

### 2026-03-29 - 终端切到居中加分辨率自适应布局

- 主题：让终端窗口和主内容区按屏幕比例自动缩放并保持居中
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：终端不能只针对当前开发分辨率，必须在不同屏幕尺寸下保持可读和稳定布局
- 结果：
  - 外层面板改为相对屏幕宽高的居中布局
  - 内层主容器、导航列和内容列切到相对宽高分配
  - 页面页脚文案同步更新为自适应布局状态

### 2026-03-29 - 终端窗口比例提升到接近全屏

- 主题：把终端窗口提高到接近屏幕 90% 的使用面积
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：当前终端已经可用，但仍然偏保守，用户希望更接近全屏控制台体验
- 结果：
  - 终端外层窗口改为宽高各占屏幕约 90%
  - 保留居中和相对布局逻辑，不回退到固定像素窗口

### 2026-03-29 - 终端切换到深蓝灰控制台主题

- 主题：参考现代控制面板网页的配色和细边线分区，重做终端首页观感
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：原版 Minecraft 容器风格不适合制度终端，上一版纯色卡片也不够像真正的控制台页面
- 结果：
  - 终端整体配色改为深蓝灰主背景加蓝色高亮
  - 页面结构改成细边线包裹的分区面板，而不是大块卡片堆叠
  - 首页新增路由矩阵、联机概览、公共队列和市场监控四类控制台区块

### 2026-03-29 - 终端压缩左栏并关闭调试噪声

- 主题：缩小左侧导航、把标题并入导航头，并清理开发环境中无意义的启动告警
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`、`run/client/config/`
- 原因：当前样式已定稿，但左栏和顶部标题带占用过多空间，右侧内容区过挤，同时 `ModularUI 2` 调试模式和 Forge 版本检查噪声影响最终体验
- 结果：
  - 标题并入左侧导航头，顶部独立标题带移除
  - 左侧导航列缩窄，右侧内容区获得更多宽高空间
  - 首页和详情页区块高度、表头和文案密度下调，减少挤压与遮挡
  - 关闭 `ModularUI 2` 的测试 GUI 与调试描边，补齐 `NEI` 缺失的 `untranslator.cfg`
  - 关闭开发客户端中的 Forge 版本检查，减少启动时无关异常输出
  - 为开发环境补齐 `minecraft` 与 `gtnhlib` 缺失的 `items` 纹理别名，消除启动时的缺失贴图告警
  - 补齐 `IGuiHolder#createScreen(...)` 并回调若干紧凑行高，消除 `ModularUI 2` 的屏幕创建与纵向 padding 警告
  - 参考 AE2 终端的上下分区布局，把左侧导航改成顶部固定、底部固定、中间弹性伸缩的自适应结构，避免窗口高度变化时溢出边框
  - 重新压缩右侧内容区的长文本和中部比例，避免中文长句在窄栏内被挤成纵向显示
  - 进一步收窄左侧导航为固定窄比例，并移除矩阵左侧辅助说明列，把联机概览缩成纯图例卡，解决中部两块的剩余挤压
  - 把首页高度分配改为固定头部、弹性中段、固定底部，并缩减矩阵行数和右侧概览内容，保证内容不再越出终端下边

### 2026-03-29 - 修正 WorldEdit 服务端分发形态

- 主题：修正 GTNH 服务端侧的 WorldEdit 分发，补齐 CUIFe，并保留 dist 包替代旧 core 的方案
- 影响范围：`GroupServer/packwiz/sync_root/mods/`、`GroupServer/GTNHServerConfig/server_mods/mods/`、`GroupServer/Galaxy_GTNH284_S1/mods/`、`GroupServer/Galaxy_GTNH284_S2/mods/`、`GroupServer/Galaxy_GTNH_Lobby/mods/`
- 原因：复核后确认 `worldedit-forge-mc1.7.10-6.1.1-dist.jar` 已内置 core 类，不应再额外叠加旧 `worldedit-core`；但 `WorldEditCuiFe` 需要同步补到服务端侧，之前这一部分漏放了
- 结果：
  - 保持 `worldedit-forge-mc1.7.10-6.1.1-dist.jar` 作为 WorldEdit 服务端实现
  - 将 `WorldEditCuiFe-v1.0.7` 补入 `GTNHServerConfig` 服务端私货目录和 S1/S2/Lobby live 实例
  - 未执行 live 服重启，当前仍为落盘待生效状态

### 2026-03-29 - 调整 GTNH 默认同步包的跨维传送与黑暗模组

- 主题：在 `packwiz` 同步源中放开 ServerUtilities 跨维传送，并移除 `Darkerer`
- 影响范围：`GroupServer/packwiz/sync_root/serverutilities/server/ranks.txt`、`GroupServer/packwiz/sync_root/mods/`、`GroupServer/packwiz/sync_root/config/`
- 原因：改善玩家跨维 `/home` 和 `/warp` 体验，并移除 GTNH 包内的真实黑暗实现，后续由 `packwiz` 同步到客户端与服务端
- 结果：
  - 为 `player`、`vip`、`admin` 全部开启 `serverutilities.homes.cross_dim` 与 `serverutilities.warps.cross_dim`
  - 从默认同步源删除 `darkerer-1.0.6.jar`
  - 从默认同步源删除 `config/darkerer.cfg`

### 2026-03-29 - 补齐私货 WorldEdit 分发与 live 服跨维传送配置

- 主题：整理根目录私货 jar，准备替换 WorldEdit 为 dist 包，并把 live 实例的 ServerUtilities 跨维权限直接改到位
- 影响范围：`GroupServer/packwiz/sync_root/mods/`、`GroupServer/GTNHServerConfig/`、`GroupServer/Galaxy_GTNH284_S1/`、`GroupServer/Galaxy_GTNH284_S2/`、`GroupServer/Galaxy_GTNH_Lobby/`
- 原因：在不重启 live 服的前提下，先把创世神相关模组文件、WorldEdit 配置镜像和 `/home` `/warp` 跨维权限准备完成
- 结果：
  - 计划用私货 `worldedit-forge-mc1.7.10-6.1.1-dist.jar` 替换旧 `worldedit-core + worldedit-forge` 组合
  - GTNHServerConfig 补齐 `config/worldedit/worldedit.properties` 镜像
  - GTNHServerConfig 与 S1/S2/Lobby 的 `serverutilities/server/ranks.txt` 全部开启跨维 `home/warp`

### 2026-03-30 - 明确群组服一期后端与同步边界

- 主题：把一期后端架构、同步范围和免费传送规则正式写入文档
- 影响范围：`README.md`、`../Docs/群组服.md`、`../Docs/技术边界文档.md`、`docs/WORKLOG.md`
- 原因：当前已明确一期不接 `Redis`，并且需要先把银行系统、主背包同步和免费跨服传送的边界定死，避免后续设计反复摇摆
- 结果：
  - 明确一期唯一中心化存储为 `PostgreSQL`
  - 明确一期采用模组服务端直连 `PostgreSQL`，不单独建设中心后端服务
  - 明确一期共享范围包括制度数据、主物品栏、护甲、经验、血量、饥饿
  - 明确“共享背包”当前等同于玩家按 `E` 打开的主背包数据
  - 明确跨服传送与 `home` 当前为免费规则
  - 明确一期实施顺序为：先银行系统，再落库与同步，再传送

### 2026-03-30 - 新增银行系统一期需求文档

- 主题：把银行系统一期能力、边界和非目标整理成正式需求文档
- 影响范围：`docs/banking-system-requirements.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：当前已经明确银行系统是一切制度与跨服能力的前置底座，需要先把需求边界固定，再进入具体表结构设计
- 结果：
  - 新增银行系统一期需求文档，明确初始玩家余额为 `0`
  - 明确玩家账户、税池、兑换所储备、后续公会资金都必须作为独立账户存在，不能退化为简单变量
  - 明确一期只做固定规则兑换结算，不做单独硬币交易市场与汇率系统
  - 明确一期不做任何扩展金融能力，如利息、贷款、定存等
  - 将银行系统需求文档加入仓库文档入口

### 2026-03-30 - 新增银行系统数据表与事务边界设计

- 主题：基于银行需求文档，落一期数据库表设计与事务边界草案
- 影响范围：`docs/banking-schema-design.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：银行系统需求边界已经固定，需要尽快把账户表、交易表、账本分录表和关键事务流程正式定稿，避免后续编码时重新发明模型
- 结果：
  - 新增银行系统数据表与事务边界设计文档
  - 明确一期核心表为 `bank_account`、`bank_transaction`、`ledger_entry` 与 `coin_exchange_record`
  - 明确所有资金变动必须在同一事务中完成锁定、校验、分录和余额更新
  - 明确幂等键、行级锁、固定加锁顺序和禁止修改历史账本的约束
  - 将数据表设计文档加入仓库文档入口

### 2026-03-30 - 新增银行 Java 领域模型与仓储接口草案

- 主题：把银行需求与表设计翻译成 Java 侧代码骨架
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/`、`docs/banking-java-domain-draft.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：仅有表设计还不够，必须尽快把领域模型、仓储接口和事务边界接口固定下来，避免后续业务实现直接散落 SQL 和字符串常量
- 结果：
  - 新增银行领域对象、关键枚举和仓储接口草案
  - 新增 `BankingTransactionRunner` 事务边界接口，用于表达银行写操作必须运行在同一事务中
  - 新增 Java 侧设计说明文档，建立文档设计与代码骨架之间的映射关系
  - 将 Java 侧设计文档加入仓库文档入口

### 2026-03-30 - 新增银行 PostgreSQL DDL 草案

- 主题：把一期银行表设计正式落成 PostgreSQL SQL 文件
- 影响范围：`docs/banking-postgresql-ddl.sql`、`docs/banking-schema-design.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：仅有 Markdown 表设计还不够，后续开始写 JDBC 仓储或迁移脚本前，需要一份可以直接对照执行和继续演进的 DDL 草案
- 结果：
  - 新增一期银行核心表的 PostgreSQL DDL 草案文件
  - 固定 `bank_account`、`bank_transaction`、`ledger_entry`、`coin_exchange_record` 与可选 `bank_daily_snapshot` 的字段、约束和索引
  - 明确账户 `updated_at` 触发器策略以及金额非负、幂等键唯一、账本顺序唯一等数据库侧约束
  - 将 SQL 草案加入设计文档与仓库文档入口

### 2026-03-30 - 开始落银行应用服务实现

- 主题：把银行一期业务动作从纯文档和接口草图推进到 application 层代码
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/application/`、`src/main/java/com/jsirgalaxybase/modules/core/banking/domain/CoinExchangeRecord.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/repository/LedgerEntryRepository.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：DDL 和领域模型已经齐备，下一步必须先把一期真正的业务动作编排固定下来，后续 JDBC 仓储才能按稳定签名实现
- 结果：
  - 新增 `BankingApplicationService`，统一承接开户、查询、玩家转账、内部划转和硬币兑换结算
  - 新增对应命令对象与 `BankPostingResult`，收束一期业务入参与出参
  - 补上基于 `request_id` 的幂等回放能力所需仓储查询签名
  - 将 `CoinExchangeRecord` 字段补齐到与 PostgreSQL DDL 更一致的状态

### 2026-03-30 - 补齐银行 JDBC 基础设施层

- 主题：把银行 application 层继续落到可对接 PostgreSQL 的 JDBC 边界
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：只有 application service 还不够，必须同步提供事务执行器与仓储实现骨架，后续才能真正接数据库和跑集成验证
- 结果：
  - 新增 JDBC 连接管理器与事务执行器，支持线程内事务连接复用
  - 新增账户、交易、账本分录、兑换记录的 JDBC 仓储实现
  - 将 `SELECT ... FOR UPDATE`、幂等查询、批量追加分录和账户余额更新明确落到代码层
  - 编译验证通过，当前只差 PostgreSQL 连接配置与模块装配

### 2026-03-30 - 接入银行配置项与模块初始化挂载点

- 主题：把银行 JDBC 基础设施从“仅可编译类库”推进到模块生命周期中的可装配状态
- 影响范围：`src/main/java/com/jsirgalaxybase/config/ModConfiguration.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：如果不把配置项和初始化入口接上，后续命令或 GUI 层仍然拿不到银行服务实例
- 结果：
  - 新增 PostgreSQL 银行连接配置项与 `source_server_id` 配置项
  - 新增 `BankingInfrastructure` 聚合对象与基于 `DriverManager` 的 `DataSource` 工厂
  - `InstitutionCoreModule` 已可在服务端按配置准备银行基础设施实例
  - 编译验证通过，当前剩余工作聚焦于 PostgreSQL 驱动依赖、真实连通验证和上层入口接线

### 2026-03-30 - 补充 PostgreSQL 本地安装与迁移说明

- 主题：补上 Ubuntu 宿主机 PostgreSQL 安装、初始化与换机迁移指导
- 影响范围：`docs/postgresql-local-setup-and-migration.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：当前机器没有安装 PostgreSQL，且当前会话没有无密码 sudo，无法直接代装到宿主机；需要把安装与迁移流程沉淀为可执行说明
- 结果：
  - 新增 Ubuntu 24.04 下 PostgreSQL 安装与建库说明
  - 补充基于当前 DDL 的初始化命令
  - 明确换机迁移推荐走逻辑备份而不是直接拷贝数据目录
  - 补充最小备份命令，降低后续换主机时的数据丢失风险

### 2026-03-30 - 完成本机 PostgreSQL 安装与模组真实连通验证

- 主题：把 PostgreSQL 从文档方案推进到宿主机实际运行与模组服务端启动验证
- 影响范围：`dependencies.gradle`、`repositories.gradle`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankingInfrastructureFactory.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`docs/banking-java-domain-draft.md`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：银行 JDBC 实现已经存在，但如果没有真实数据库、真实驱动和真实服务端启动验证，就还不能说明这条链路可用
- 结果：
  - 在 Ubuntu 24.04 宿主机安装并启动 PostgreSQL 16
  - 创建本地开发账号 `jsirgalaxybase_app` 与数据库 `jsirgalaxybase`
  - 将一期银行 DDL 实际执行到本地数据库，确认核心表全部存在
  - 新增 PostgreSQL JDBC 驱动依赖与 Maven Central 仓库声明
  - 启动 `runServer` 完成模组服务端真实联调，日志明确显示银行 PostgreSQL 基础设施已准备并验证成功

### 2026-03-30 - 收紧本地数据库监听并接入银行管理员测试命令

- 主题：把本地 PostgreSQL 显式限制在回环地址，并提供游戏内银行管理测试入口
- 影响范围：`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/module/ModuleManager.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：当前数据库不应暴露外网监听，同时银行系统已经具备基础设施，需要第一个实际管理员入口来驱动开户、查余额、发钱和转账测试
- 结果：
  - PostgreSQL 已显式配置为只监听 `127.0.0.1:5432`
  - 本地开发业务账号密码已切换为用户指定的新密码
  - 在 `/jsirgalaxybase bank` 下新增 `open`、`balance`、`grant`、`transfer` 四个管理员测试子命令
  - 通过实际 `runServer` 自动控制台执行验证了 bank 命令帮助输出与命令注册链路

### 2026-03-31 - 放开 NEI 客户端主配置的本地持久化

- 主题：让 GTNH 客户端的 `NEI` 主配置不再被 `packwiz` 更新与 `Default Configs` 启动流程反复覆盖
- 影响范围：`GroupServer/packwiz/sync_root/packwiz-whitelist.json`、`GroupServer/packwiz/sync_root/config/localconfig.txt`、`GroupServer/packwiz/whitelist-localconfig-notes.md`、`docs/WORKLOG.md`
- 原因：玩家反馈 `NEI` 配置经常被自动替换；排查确认活跃文件 `config/NEI/client.cfg` 未进入白名单，同时 `localconfig.txt` 还在接管整份 `NEI/client.cfg`
- 结果：
  - 将 `config/NEI/client.cfg` 加入 `packwiz` 白名单
  - 保留 `config/NEI/client.cfg.bak` 白名单不变
  - 注释掉 `localconfig.txt` 中对 `[NEI/client.cfg]/*/*` 的接管规则
  - 在既有白名单说明文档中补记 `NEI` 案例，方便后续处理同类客户端配置问题

### 2026-03-30 - 扩展银行管理员命令到系统账户与最近流水查询

- 主题：把第一版银行测试命令从单纯改余额扩展到状态查询与账本查看
- 影响范围：`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：仅有开户、查余额、发钱、转账还不够，管理员需要直接查看最近流水和系统测试账户状态，才能形成最小可用的联调闭环
- 结果：
  - 在 `/jsirgalaxybase bank` 下新增 `ledger`、`system` 和 `system ledger` 命令
  - recent ledger 输出已包含交易号、方向、金额、变动前后余额和时间戳
  - system summary 会显示测试系统账户的编号、类型、状态和当前余额
  - 通过实际 `runServer` 自动执行验证了 system summary 与 system ledger 命令回显

### 2026-03-30 - 扩展银行管理员命令到公共账户、交易详情与系统账户初始化

- 主题：把银行管理命令从单账户测试扩展到公共账户与交易审计层面
- 影响范围：`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/repository/BankTransactionRepository.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankTransactionRepository.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：当前需要直接初始化系统账户集、查看公共账户状态，并能按交易号定位单笔交易详情，方便后续联调和审计
- 结果：
  - 新增 `/jsirgalaxybase bank public` 与 `/jsirgalaxybase bank public ledger` 命令用于查看受管公共/系统账户
  - 新增 `/jsirgalaxybase bank tx <transactionId>` 命令用于查询单笔交易与关联账本分录
  - 新增 `/jsirgalaxybase bank init system` 命令，用于初始化测试系统资金、系统运营账户、税池和兑换储备账户
  - 通过实际 `runServer` 自动执行验证了系统账户初始化、公共账户汇总与交易不存在时的详情查询回显

### 2026-03-30 - 收敛受管系统账户并补齐备份恢复脚本

- 主题：把系统账户模型收敛为 `ops + exchange`，并把 PostgreSQL 逻辑备份/恢复脚本正式落地
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/ManagedBankAccounts.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`scripts/db-backup.sh`、`scripts/db-restore.sh`、`docs/postgresql-backup-and-restore.md`、银行相关文档
- 原因：当前不再需要测试资金池与独立税池，系统运营收支应统一落在 `ops`；同时换机和演练必须从“会手敲命令”升级到“有固定脚本可执行”
- 结果：
  - 受管系统账户收敛为 `ops` 系统运营账户与 `exchange` 兑换储备账户
  - 玩家账户仍保持按需懒初始化，不做自动开户
  - `InstitutionCoreModule` 在服务端启动时自动确保系统账户存在
  - `/jsirgalaxybase bank system` 与 `grant` 改为围绕 `ops` 账户工作，公共账户查询只展示 `ops` 与 `exchange`
  - 新增 PostgreSQL 逻辑备份与恢复脚本，并补充使用方式、演练步骤与风险控制说明

### 2026-03-30 - 增补 systemd 定时备份方案并明确快照技术取舍

- 主题：把 PostgreSQL 备份方案从“手动脚本可用”推进到“systemd 每日自动备份可落地”
- 影响范围：`ops/systemd/`、`scripts/install-systemd-backup.sh`、`docs/postgresql-backup-and-restore.md`、`docs/README.md`
- 原因：当前实际需求已经明确为“单数据库每日一份、保留七份”，需要正式选主方案，并说明为什么不把文件系统快照当当前主链路
- 结果：
  - 新增 system 级 `systemd service + timer` 模板与环境文件样例
  - 新增安装脚本，用于把 unit 安装到 `/etc/systemd/system/` 并启用 timer
  - 明确当前主方案是 `pg_dump -Fc + systemd timer + RETAIN_COUNT=7`
  - 明确文件系统快照不是当前主方案，后续如需更细恢复点应升级到 `pg_basebackup + WAL archive`

### 2026-03-30 - 补齐 PostgreSQL 备份恢复值班手册与真实演练说明

- 主题：把备份恢复文档从“方案说明”补成“后续维护者可以直接照抄命令执行”的操作手册
- 影响范围：`docs/postgresql-backup-and-restore.md`、`docs/WORKLOG.md`
- 原因：当前备份与恢复链路已经真实安装和演练通过，但如果不把日常查看、手动备份、恢复到测试库、覆盖正式库和清理测试库等指令写清楚，后续维护者仍然会不知道怎么用
- 结果：
  - 文档补充了当前机器上的实际部署状态、备份目录与 systemd 单元名称
  - 文档补充了日常查看、立即备份、恢复到测试库、覆盖正式库和删除测试库的完整命令
  - 文档明确了 `oneshot` service 的状态表现与业务账号无建库权限这两个常见注意事项

### 2026-03-30 - 明确银行终端页的信息架构并开始接真实只读快照

- 主题：把普通玩家正式入口的银行 GUI 从“想法”落成文档，并开始按终端壳的嵌套菜单模式实现
- 影响范围：`docs/banking-terminal-gui-design.md`、`docs/README.md`、终端 GUI 与终端快照提供者
- 原因：当前终端已经是左侧导航 + 主区板块的统一壳，银行页不能再做成单独弹窗或纯目录页，而应先展示关键内容，再提供二级子页跳转
- 结果：
  - 文档固定了银行主页、个人账户、转账服务、Exchange 公开页、个人流水五页结构
  - 明确 Exchange 储备余额与最近流水属于玩家公开透明内容，而不是仅供管理员查看
  - 实施方向改为“先做真实只读快照与嵌套菜单，再继续接正式写操作”

### 2026-03-30 - 修正终端页签只换标题不换正文的 ModularUI 用法错误

- 主题：修复终端切到银行页后右侧正文不切换、只有标题变化的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：原先正文区是在 `buildUI()` 时按当时的 `selectedPage` 一次性 `switch` 构建，后续虽然标题使用了动态文本，但正文没有进入框架的启用/禁用重布局链
- 结果：
  - 改为把所有页面挂进同一个正文容器
  - 每个页面容器使用 `setEnabledIf(...)` 绑定 `selectedPage`
  - 父级 `Flow` 开启 `collapseDisabledChild(true)`，让页签变化时正文区实际切换并重新布局

### 2026-03-31 - 修复终端打开即断线并记录两类联调阻塞根因

- 主题：收敛本地专用测试服最近两类核心阻塞：进服阶段的 `Fatally Missing blocks and items`，以及打开终端后的 `Disconnected from server`
- 影响范围：`src/main/java/com/jsirgalaxybase/GalaxyBase.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalNetwork.java`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：一方面 `ModularUI2` 的 dev 运行产物把测试映射带进了 Forge 注册表握手；另一方面 Forge 1.7.10 对自定义包通道名存在 20 字符上限，原终端通道名超长后会在服务端 `C17PacketCustomPayload` 解码阶段直接踢线
- 结果：
  - 在模组入口里忽略了 `modularui2:test_block` 与 `modularui2:test_item` 这类瞬时 dev 缺失映射，进服不再被这类测试映射阻塞
  - 把终端 `SimpleNetworkWrapper` 通道名从超长值收敛到 `jgb_terminal`
  - 确认这类终端断线修复后，客户端与服务端都必须重启，否则旧客户端仍会继续发送旧通道名
  - 把两类问题的根因与处理办法补进 PostgreSQL/联调文档，后续排障可以直接按文档核对

### 2026-03-31 - 明确银行终端“未启用基础设施”其实是服务端配置未打开

- 主题：把银行终端里的“未启用 PostgreSQL 基础设施”从模糊报错改成可操作的配置诊断
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankingService.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSnapshotProvider.java`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：本地专用测试服的 `run/server/config/jsirgalaxybase-server.cfg` 当时仍是默认占位状态，`bankingPostgresEnabled=false`、JDBC 地址仍指向 `db-host`，终端只能统一回显“当前世界未启用 PostgreSQL 银行基础设施”，信息不足以指导维护者下一步该改哪里
- 结果：
  - 银行终端现在会优先区分“服务端显式关闭银行 PostgreSQL”“JDBC 地址未配置”“用户名/密码未填”“初始化失败”几类状态
  - 银行快照页会提示优先检查 `jsirgalaxybase-server.cfg`、JDBC 配置与服务端启动日志
  - 文档补充了本地 `runServer` 联调至少要打开 `bankingPostgresEnabled` 并填好 JDBC 凭据这一前置条件

### 2026-04-01 - 银行第三轮收口：补真实 JDBC 验证、内部划转语义测试与开户复用规则

- 主题：把银行模块当前最大缺口从“服务层逻辑”收敛到“真实 PostgreSQL 路径验证”和“剩余语义定稿”
- 影响范围：`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`docs/banking-java-domain-draft.md`、`docs/banking-terminal-gui-design.md`、`docs/WORKLOG.md`
- 原因：第二轮已经修完 `request_id` 幂等重放、历史余额回放与语义冲突校验，但真实 JDBC 路径验证、`postInternalTransfer` 同等级补测，以及 `openAccount` 命中已有账户时的资料处理规则仍未完全收口
- 结果：
  - 新增真实 PostgreSQL 集成测试，基于独立测试 schema 验证 `saveIfOwnerAbsent`、`saveIfRequestAbsent`、`request_id` 历史余额重放、语义冲突拒绝与事务回滚不半提交
  - 为 `postInternalTransfer` 补齐历史余额回放、`amount`、`fromAccountId`、`toAccountId`、`businessType`、`operatorType`、`operatorRef`、`sourceServerId` 冲突测试
  - 明确 `openAccount(...)` 命中已有账户时保持既有 `display_name` 与 `metadata_json` 不刷新
  - 文档明确当前终端只承担开户、只读快照和玩家转账，任务书硬币兑换真实入口延期到市场阶段

### 2026-04-01 - 银行第四阶段收口：补工厂初始化验证、deduct 管理闭环与独立测试入口

- 主题：把银行一期在“非市场阶段”剩余的初始化链路、管理命令和测试执行入口真正收住
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankingInfrastructureFactory.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`build.gradle.kts`、`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：第三轮后还剩三个明显尾巴没有收口：工厂初始化校验仍写死 `public` schema、`transactionType` 语义矩阵缺显式补测、管理员命令只有 `grant` 没有对称扣减入口，也缺独立的集成测试执行命令
- 结果：
  - `JdbcBankingInfrastructureFactory` 改为按当前 JDBC 连接的 `search_path/currentSchema` 校验必需表，独立 schema 的 PostgreSQL 集成测试终于能真实覆盖工厂初始化路径
  - 新增工厂初始化成功/缺表失败两条真实 PostgreSQL 集成测试，并补上 `transactionType` 冲突测试
  - 新增 `./gradlew bankingIt` 与 `./gradlew banking-it` 两个银行集成测试入口，便于单独跑 PostgreSQL 路径
  - `/jsirgalaxybase bank` 管理命令补上 `deduct <player> <amount> [comment]`，与既有 `grant` 形成对称的管理员修正闭环
  - 明确幂等重放返回的是“历史 availableBalance + 当前非余额字段”的结果对象，而不是完整历史账户快照

### 2026-04-01 - 市场阶段前置分析：收口市场与银行的职责边界

- 主题：在进入市场实施前，把制度文档中的市场需求与现有银行能力做一次正式对齐，避免后续把市场和银行混成一层
- 影响范围：`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
- 原因：银行一期已经完成非市场阶段收口，但市场真正开做之前，必须先明确哪些能力可以直接复用银行，哪些最小能力仍需银行补齐，以及哪些责任必须留在市场模块
- 结果：
  - 在 `../Docs/市场经济推进.md` 中新增“市场阶段与银行系统边界结论”章节
  - 明确市场可直接复用现有银行账户、账本、兑换结算与系统划转能力
  - 明确银行仍需补齐的最小能力是：`冻结资金/解冻资金`、`税池账户`、`市场结算业务类型`

### 2026-04-01 - 市场阶段第一轮：接入真实任务书硬币兑换入口并补银行最小缺口

- 主题：按“真实兑换入口 + 银行最小缺口 + 市场骨架”收下市场阶段第一轮
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/`、`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangePlannerTest.java`、`docs/banking-java-domain-draft.md`、`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
- 原因：制度上已经明确市场阶段不应重做第二套货币底层，但现有代码还缺玩家可调用的真实兑换入口，以及挂单市场前必需的冻结余额、税池账户和市场结算语义边界
- 结果：
  - 新增 `/jsirgalaxybase market quote hand` 与 `/jsirgalaxybase market exchange hand`，先落地“手持一叠任务书硬币”的真实兑换入口
  - 银行应用服务补齐 `冻结 / 解冻 / 从冻结余额结算` 三个最小动作，并新增市场相关交易类型、业务类型与 `tax` 受管公共账户
  - 新增 `modules/core/market/` 首轮骨架，把任务书硬币识别、兑换规则和银行结算桥接从 banking 中拆到 market 侧
  - 当前实现明确为 `source-blind`：仅按 Dreamcraft coin 物品注册名识别，不在本轮尝试从物品本体反推“一次性任务 / 循环悬赏”来源
  - 补了银行冻结生命周期单测与市场任务书硬币规则单测，完整挂单订单簿、托管库存、撮合、CLAIMABLE/EXCEPTION 与崩服恢复仍留待下一轮
  - 明确订单簿、托管库存、撮合、内部操作日志、异常恢复属于市场模块自身，不应继续塞回银行模块

### 2026-04-01 - 市场阶段第二阶段：收口冻结回放语义并切入旧单一标准商品撮合方案最小骨架

- 主题：先把第一层金融底座补到可承载市场，再谨慎切入旧单一标准商品撮合方案
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/`、`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/`、`docs/banking-postgresql-ddl.sql`、`docs/market-postgresql-ddl.sql`、`docs/banking-java-domain-draft.md`、`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
- 原因：第一轮已经补出冻结/解冻/从冻结结算动作，但 replay 语义仍然把 `available` 和 `frozen` 混成“半历史半当前”；同时第二层旧单一商品撮合方案还没有真正的订单 / 托管 / 操作日志结构，仍停在空接口阶段
- 结果：
  - `TaskCoinExchangePlanner` 补齐 `IV = 10000`，并避免把未知更高罗马后缀误判成 `I` 或 `BASE`
  - `ledger_entry` 扩展为同时保存 `available` 与 `frozen` 的 before/after，`freeze/release/settleFrozenTransfer` 的 replay 现在能一致返回历史余额态
  - 补齐银行服务层 replay 单测，以及 PostgreSQL 下的 freeze/release/settle 成功、重放、冲突与回滚回归
  - 新增标准商品、市场订单、托管库存、内部操作日志、成交记录等领域对象与仓储接口，不再只是空接口占位
  - 新增旧单一商品撮合方案的最小应用服务，先支持 `创建卖单`、`撤销卖单` 与 `查看 OPEN 卖单` 读模型骨架，且明确卖单托管与 CLAIMABLE 返还路径
  - 当前仍未进入买单冻结闭环、真实撮合、税收结算、GUI、跨服消息和第三层定制化交易

  ### 2026-04-01 - 市场阶段第三阶段：补请求语义与恢复闭环并打通旧单一路线的最小买卖撮合

  - 主题：先修第二阶段一致性缺口，再把旧单一标准商品撮合方案推进到最小可运行买卖闭环
  - 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/`、`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/infrastructure/jdbc/MarketPostgresIntegrationTest.java`、`build.gradle.kts`、`docs/market-postgresql-ddl.sql`、`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
  - 原因：第二阶段虽然已经有卖单骨架，但 `requestId` 还没有完整请求语义校验，失败路径也缺少最小恢复闭环，同时第三阶段要求的买单冻结、同步撮合、CLAIMABLE 生成和真实 JDBC 市场持久化仍未接通
  - 结果：
    - 旧单一商品撮合服务补齐 `BUY_ORDER_CREATE / BUY_ORDER_CANCEL / MATCH_EXECUTION`，并把买单冻结、撤单释放、同步单商品限价撮合、税池入账、CLAIMABLE 生成与成交记录写入收口到 market application service
    - `MarketOperationLog` 现在按 `requestSemanticsKey` 校验完整请求语义，重复 `requestId` 不再只校验操作类型；卖单创建与撤单失败时会保留相关 `order / custody / trade` 线索，并进入 `RECOVERY_REQUIRED` 或 `FAILED`
    - 新增 `MarketRecoveryService`，可以扫描 `CREATED / PROCESSING / FAILED / RECOVERY_REQUIRED` 并把关联订单与托管库存收口到 `EXCEPTION`
    - 补齐市场 JDBC 基础设施：真实 PostgreSQL 仓储、`JdbcMarketInfrastructureFactory`、`marketIt / market-it` 任务，以及 PostgreSQL 下的卖单创建/撤单、请求语义冲突和恢复扫描回归
    - 为了让 market JDBC 能复用 banking 的同一连接基础设施，把 `AbstractJdbcRepository` 与 `JdbcConnectionCallback` 开放为可跨包复用的公共类型，保持市场与银行共享一条事务链
    - 补了单元测试覆盖：卖单/撤单 request 语义冲突、买单冻结与撤单释放、同步撮合后的成交记录与 CLAIMABLE 资产、恢复扫描收口
    - 已实际执行并通过：`./gradlew test`、`./gradlew bankingIt`、`./gradlew marketIt`

### 2026-04-04 - 将终端正文改为单页宿主

- 主题：继续修复终端打开后聊天栏持续刷 `ModularUI` Column resize 错误的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：终端正文区此前把 home、market、bank 各页同时挂在同一棵 `Flow.column()` 下，再通过 `setEnabledIf(...)` 切换可见性；在当前复杂布局下，隐藏页仍会形成重复的 `Column` 高度求解链，导致父级布局长期不收敛
- 结果：
  - 正文区改为 `SingleChildWidget` 单页宿主，只保留当前选中页的一棵 widget 子树
  - 页签切换时按 `selectedPage` 动态替换正文，移除多页常驻叠加带来的重复 `Column` 布局链

### 2026-04-25 - 产出 Phase 7 交接文档

- 主题：产出终端 GUI 迁移 Phase 7 交接文档，供后续先进 AI 执行 MARKET_CUSTOM 与 MARKET_EXCHANGE 迁移
- 影响范围：`docs/terminal-phase7-handover-to-chatgpt5.5.md`、`docs/WORKLOG.md`
- 原因：Phase 6 已完成标准商品市场迁移与滚动/布局/数据截断修复，Phase 7 需要迁移定制商品市场与汇率市场；为确保后续 AI 能清晰理解上下文、阶段定位、技术约束与验收标准，产出本交接文档
- 引用来源：`docs/terminal-betterquesting-ui-phase7-custom-exchange-residue-prompt-2026-04-19.md`、`docs/terminal-plan.md`、`docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md`
- 结果：产出完整交接文档，包含项目背景、阶段定位、技术架构要点、必须阅读的文档、Phase 7 执行 prompt 核心摘要、验收标准、已知风险与坑点、交接地清单
  - 已实际执行并通过：`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && /media/u24game/gtnh/JsirGalaxyBase/gradlew -p /media/u24game/gtnh/JsirGalaxyBase assemble --no-configuration-cache -PforceToolchainVersion=17`

### 2026-04-25 - Terminal BetterQuesting UI Phase 7：迁入定制商品市场与汇率市场

- 主题：把 MARKET_CUSTOM 与 MARKET_EXCHANGE 两张剩余真实业务页迁入新 `TerminalHomeScreen` 宿主，并收干新壳对旧市场装配残留的直接依赖
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/client/component/`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/`、`src/main/java/com/jsirgalaxybase/terminal/network/`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSectionService.java`、终端相关测试、`docs/README.md`、`docs/WORKLOG.md`
- 原因：phase 6 后新壳已承接 BANK 与 MARKET_STANDARDIZED，phase 7 必须补齐 listing-first 的定制商品市场和 quote-first 的汇率市场，让 phase 8 只剩正式 cutover
- 结果：
  - 新增 `TerminalCustomMarketSection` / `TerminalCustomMarketSectionModel` / `TerminalCustomMarketSectionSnapshot`，承接 active / selling / pending listing 浏览、当前 listing 详情、资产摘要、购买 / 下架 / 领取确认与最近动作反馈
  - 新增 `TerminalExchangeMarketSection` / `TerminalExchangeMarketSectionModel` / `TerminalExchangeMarketSectionSnapshot`，承接兑换标的浏览、formal quote、pair / rule / limit 状态、刷新报价、确认兑换与动作反馈
  - 新增 custom / exchange 独立 action payload，并扩展 `TerminalActionType`、`TerminalActionMessage` 主链与 `TerminalSnapshotMessage` 序列化回写
  - `TerminalPopupFactory` 生命周期下已承接 custom 购买 / 下架 / 领取确认，以及 exchange 确认兑换；确认后继续走新 action -> snapshot 主链
  - `TerminalMarketSectionService` 侧新壳装配不再直接实例化旧 market / custom / exchange session controller，也不借旧 builder、sync binder 或旧 dialog；旧 ModularUI 代码继续保留给 phase 8 cutover 前过渡
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.terminal.TerminalServiceTest --tests com.jsirgalaxybase.terminal.TerminalMarketActionMessageFactoryTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.client.gui.framework.VerticalScrollPanelTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --tests com.jsirgalaxybase.terminal.ui.TerminalMarketServiceTest --tests com.jsirgalaxybase.terminal.ui.TerminalMarketSessionControllerTest --tests com.jsirgalaxybase.terminal.ui.TerminalCustomMarketSessionControllerTest --tests com.jsirgalaxybase.terminal.ui.TerminalExchangeMarketSessionControllerTest --no-configuration-cache -PforceToolchainVersion=17`
  - `./gradlew test --no-configuration-cache -PforceToolchainVersion=17` 当前仍有既存 PostgreSQL 银行集成测试 `BankingPostgresIntegrationTest.factoryCreateRejectsMissingTablesInCurrentSchema` 断言失败；终端定向测试均已通过

### 2026-04-25 - Terminal BetterQuesting UI Phase 8：正式入口 cutover 到新终端壳

- 主题：把 G 键与背包按钮的正式终端打开链收口到 `OpenTerminalRequestMessage -> OpenTerminalApprovedMessage -> TerminalHomeScreen`
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalNetwork.java`、`src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java`、`src/test/java/com/jsirgalaxybase/terminal/TerminalOpenCutoverTest.java`、`docs/README.md`、`docs/terminal-plan.md`、`docs/WORKLOG.md`
- 原因：phase 7 已经让新 `TerminalHomeScreen` 承接 BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 全部正式业务页，phase 8 只需要把正式打开入口切到新壳并把旧 ModularUI 链降级为 legacy fallback
- 结果：
  - G 键与背包按钮正式入口保持默认发送 `OpenTerminalRequestMessage`，服务端通过 `TerminalService.approveTerminalClientScreen(...)` 生成初始 snapshot 与 session token，客户端经 `OpenTerminalApprovedMessage` 打开新 `TerminalHomeScreen`
  - 旧 `OpenTerminalMessage` 明确标注为 legacy ModularUI fallback，并改为调用 `TerminalService.openLegacyTerminal(...)`
  - `TerminalService.openTerminal(...)` 保留为 deprecated 兼容别名，不再作为正式入口语义；`TerminalModule` 日志也明确旧 `TerminalHomeGuiFactory` 只是 fallback factory
  - 新增 `TerminalOpenCutoverTest`，覆盖 approval -> `TerminalHomeScreenModel` 序列化、BANK / MARKET_STANDARDIZED / MARKET_CUSTOM / MARKET_EXCHANGE 新壳模型可打开、key/button 源码装配不再引用旧包、网络注册包含 open/action/snapshot 主链且 legacy packet 明确降级
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.terminal.TerminalOpenCutoverTest --tests com.jsirgalaxybase.terminal.TerminalServiceTest --tests com.jsirgalaxybase.terminal.TerminalMarketActionMessageFactoryTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.client.gui.framework.VerticalScrollPanelTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --tests com.jsirgalaxybase.terminal.ui.TerminalMarketServiceTest --tests com.jsirgalaxybase.terminal.ui.TerminalMarketSessionControllerTest --tests com.jsirgalaxybase.terminal.ui.TerminalCustomMarketSessionControllerTest --tests com.jsirgalaxybase.terminal.ui.TerminalExchangeMarketSessionControllerTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew assemble --no-configuration-cache -PforceToolchainVersion=17`

### 2026-04-25 - Terminal BetterQuesting UI Phase 9：删除旧 terminal ModularUI 过渡实现

- 主题：在 Phase 8 正式入口 cutover 后，删除 terminal 专属旧 ModularUI fallback、旧 GUI 装配层和旧同步/session/dialog 过渡件
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/network/`、`src/main/java/com/jsirgalaxybase/terminal/ui/`、`src/main/java/com/jsirgalaxybase/modules/terminal/TerminalModule.java`、终端相关测试、`docs/README.md`、`docs/terminal-plan.md`、`docs/WORKLOG.md`
- 原因：BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 已经全部由新 `TerminalHomeScreen` 主链承接；旧 `OpenTerminalMessage` / `TerminalHomeGuiFactory` fallback 已不再是正式运行部件，继续保留会增加误接线和回归风险
- 结果：
  - 删除旧 `OpenTerminalMessage`，`TerminalNetwork` 现在只注册新 request / approval / action / snapshot 四类消息
  - 删除 `TerminalService.openLegacyTerminal(...)` 与 deprecated `TerminalService.openTerminal(...)` 兼容别名，`TerminalService` 不再依赖旧 `TerminalHomeGuiFactory`
  - 删除 `TerminalModule` 中 terminal 专属 `GuiManager.registerFactory(...)` 注册和 legacy fallback 日志
  - 删除旧 terminal ModularUI 装配件：`TerminalHomeGuiFactory`、银行/市场 page builder、旧 dialog / widget factory、旧 sync binder / sync state、旧 bank / market / custom / exchange session controller，以及对应旧测试
  - 保留新链仍使用的共享类：`TerminalBankingService`、`TerminalBankSnapshotProvider`、`TerminalHomeSnapshotProvider`、`TerminalMarketService`、`TerminalMarketSectionService`、`TerminalMarketSnapshot`、`TerminalCustomMarketSnapshot`、`TerminalExchangeMarketSnapshot`、`TerminalExchangeQuoteView`、通知与 `TerminalPage`
  - `TerminalMarketService` 已切断对旧 session controller 常量/接口的依赖，custom scope 与 exchange target 由 service / payload 层自身表达
  - 旧入口引用扫描结果：`rg -n "TerminalHomeGuiFactory|OpenTerminalMessage|openLegacyTerminal|openTerminal\\(|TerminalBankPageBuilder|TerminalMarketPageBuilder|TerminalDialogFactory|PanelSyncManager|Terminal[A-Za-z]*SyncBinder|Terminal[A-Za-z]*SessionController|GuiManager\\.registerFactory" src/main/java src/test/java` 无命中
  - terminal 侧 ModularUI 引用扫描结果：`rg -n "com\\.cleanroommc\\.modularui|modularui" src/main/java/com/jsirgalaxybase/terminal src/test/java/com/jsirgalaxybase/terminal` 无命中
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.terminal.TerminalOpenCutoverTest --tests com.jsirgalaxybase.terminal.TerminalServiceTest --tests com.jsirgalaxybase.terminal.TerminalMarketActionMessageFactoryTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.client.gui.framework.VerticalScrollPanelTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --tests com.jsirgalaxybase.terminal.ui.TerminalMarketServiceTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew assemble --no-configuration-cache -PforceToolchainVersion=17`

### 2026-04-25 - Terminal BetterQuesting UI Phase 10：修复新壳缩放、滚动与裁剪

- 主题：只修新 `TerminalHomeScreen` 壳层在高 GUI Scale / 小 GUI 坐标空间下的尺寸、滚动和内容裁剪问题，不改银行/市场业务语义
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeLayout.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、终端壳层布局测试、`docs/WORKLOG.md`
- 原因：实测截图显示新壳在较大 GUI Scale 下仍按硬最小尺寸撑开，导致顶部/底部被挤压；左侧导航与 HOME/CAREER/PUBLIC_SERVICE 等普通 section 页没有局部滚动，section 与通知在内容超出时会越界或重叠
- 结果：
  - 新增 `TerminalHomeLayout`，把主 panel 尺寸改为基于当前 `GuiScreen.width/height` 的安全边距计算；小屏/高 GUI Scale 下不再用 468x320 硬最小值强行撑开
  - `TerminalHomeScreen` 使用新的 layout 结果装配 surface、status band、navigation rail 与 body bounds
  - `TerminalShellPanels.createNavigationRail(...)` 将 nav item 列表放入 `VerticalScrollPanel`，标题固定，导航项超出 rail 时通过滚轮浏览并由 scroll panel scissor 裁剪
  - 普通 section 页正文改为 `VerticalScrollPanel`：section 卡片与通知卡片进入可滚动内容区，底部 session / 刷新 / 关闭按钮继续固定在 body 底部
  - section 均分高度 helper 先扣除 gap 再计算高度，避免 `count * height + gaps` 超过可用高度；通知不再固定裁断前 2 条，而是全部进入滚动区
  - BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 专用业务 section 未改业务语义，继续沿用各自页面内部布局与滚动策略
  - 新增 `TerminalHomeScreenLayoutTest` 与 `TerminalShellPanelsScrollTest`，覆盖小屏 panel 不越安全边距、导航 rail 使用滚动容器、普通 section/notification 使用滚动容器、gap 计算不溢出
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.terminal.client.screen.TerminalHomeScreenLayoutTest --tests com.jsirgalaxybase.terminal.client.component.TerminalShellPanelsScrollTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.client.gui.framework.VerticalScrollPanelTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew test --tests com.jsirgalaxybase.terminal.TerminalOpenCutoverTest --tests com.jsirgalaxybase.terminal.TerminalServiceTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`./gradlew assemble --no-configuration-cache -PforceToolchainVersion=17`
  - 本地体验环境：检测到已有 `runServer` / `runClient` 进程在运行，未杀用户进程；`ss -ltnp | grep 25100` 显示 `127.0.0.1:25100` 已监听，server 日志有 `Done`、banking prepared 与 `Market runtime prepared`，client 日志有 `Registered terminal client entry handlers` 且已连接 `127.0.0.1:25100`

### 2026-06-07 - Terminal ServerTools 收口：传送页改为三栏工具页

- 主题：把 `SERVER_TOOLS` 从 generic section 风格收口为 workflow-first 的专用传送工具页，不改真实 warp 后端主链
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalServerToolsSectionSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalServerToolsSectionModel.java`、相关序列化与测试、`docs/WORKLOG.md`
- 原因：当前 `传送 / 群组服` 页已接通真实 warp 链，但页面仍是混合 section/scroll 形态；warp 列表、说明、状态与按钮竞争同一正文区，不符合三栏传送工具页目标
- 结果：
  - `SERVER_TOOLS` 正文切到专用布局：左侧保留窄导航轨，主体改为服务器目录卡 + warp 列表卡 + 右侧详情/最近状态/风险/主 CTA 三段结构
  - warp 列表改为稳定 item 几何，支持主名、副文案、状态标签与选中高亮，不再依赖普通文本段落
  - 右栏详情改为结构化字段：当前服务器、目标服务器、目标坐标、传送说明；最近传送状态单独成卡
  - 顶部状态栏为 `SERVER_TOOLS` 增加紧凑刷新按钮；底部 footer 不再给该页放同级刷新按钮，只保留弱化关闭入口
  - ServerTools snapshot/model 追加每个 warp 的副文案/状态标签，以及右栏和最近状态所需的结构字段；网络序列化按现有 server-tools nullable block 后续追加
  - 传送确认 popup 继续走现有确认弹窗，服务端仍复用 `PlayerTeleportService.prepareWarpTeleport(...)` 和 `ServerToolsModule.dispatchTeleport(...)`

### 2026-06-14 - Terminal 标准商品市场收口：补齐存入/卖出/即时成交/撤单工作流

- 主题：把 `MARKET_STANDARDIZED` 从“买单 + 文本说明”页收口成标准商品市场正式工作页，不改三市场架构、不重写标准市场后端
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/TerminalActionType.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalMarketActionPayload.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalMarketSectionSnapshot.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalMarketActionMessageFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/TerminalService.java`、`src/main/java/com/jsirgalaxybase/terminal/network/OpenTerminalApprovedMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSectionService.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionContent.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionState.java`、`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java`、`src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java`、`src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalMarketSectionModel.java`、相关测试、`docs/WORKLOG.md`
- 原因：标准市场后端已具备 `depositInventory(...)`、`createSellOrder(...)`、`cancelSellOrder(...)`、`createBuyOrder(...)`、`cancelBuyOrder(...)` 等能力，但终端页此前只露出限价买单和 claim，无法完成玩家实际交易闭环
- 结果：
  - 标准市场新增正式 action：`MARKET_CONFIRM_DEPOSIT_HELD`、`MARKET_CONFIRM_LIMIT_SELL`、`MARKET_CONFIRM_INSTANT_BUY`、`MARKET_CONFIRM_INSTANT_SELL`、`MARKET_CANCEL_ORDER`
  - `TerminalMarketActionPayload` 扩展为兼容旧 4 段和新 9 段编码，保留 `selectedProductKey` / 买单字段，同时追加卖单、即时买卖、撤单所需字段
  - `TerminalMarketSectionService` 与 `TerminalService` 补齐标准市场动作闭环，继续复用 `TerminalMarketService` 已有真实链路：存入、限价卖单、即时买入、即时卖出、撤单、claim 全部通过 action -> snapshot 回写
  - 标准市场 snapshot/model 追加盘口数量、仓储提示、动作预览、订单 id/可撤销标记、卖单/即时交易 drafts 与 `depositEnabled`，网络序列化同步扩展
  - `TerminalShellPanels` 对 `MARKET_STANDARDIZED` 取消外层 generic scroll wrapper，正文直接挂专用 section；`TerminalMarketSection` 改成稳定三栏：左侧商品与仓储、中间盘口/订单/CLAIMABLE、右侧存入/限价买卖/即时买卖动作区
  - 高风险动作统一走确认 popup：存入、限价卖单、即时买入、即时卖出、撤单；原有限价买单和 claim popup 继续保留
  - `CLAIMABLE` 与“我的订单”从纯文本摘要改为可操作区：claim 行可直接弹确认，订单行可直接发起撤单确认
  - 已实际执行并通过：`git diff --check`
  - 已实际执行并通过：`docker compose -f /media/u24/data/gtnh/docker/projects/docker-compose.yml run --rm -e GRADLE_USER_HOME=/tmp/gradle-home galaxy-dev ./gradlew test --tests com.jsirgalaxybase.terminal.TerminalServiceTest --tests com.jsirgalaxybase.terminal.TerminalMarketActionMessageFactoryTest --tests com.jsirgalaxybase.terminal.client.component.TerminalShellPanelsScrollTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --tests com.jsirgalaxybase.modules.core.market.application.StandardizedSpotMarketServiceTest --no-configuration-cache -PforceToolchainVersion=17`
  - 已实际执行并通过：`docker compose -f /media/u24/data/gtnh/docker/projects/docker-compose.yml run --rm -e GRADLE_USER_HOME=/tmp/gradle-home galaxy-dev ./gradlew test --tests com.jsirgalaxybase.terminal.client.screen.TerminalHomeScreenLayoutTest --tests com.jsirgalaxybase.terminal.client.component.TerminalShellPanelsScrollTest --tests com.jsirgalaxybase.terminal.TerminalServiceTest --tests com.jsirgalaxybase.terminal.TerminalMarketActionMessageFactoryTest --tests com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionContentTest --no-configuration-cache -PforceToolchainVersion=17`
  - 本机仍无 `java`，本轮验证全部通过仓库既有 Docker Gradle 路径完成
### 2026-07-20 - Standard market hover lifecycle and visual liquidity fixture

- Fixed the GUI lifecycle fault where a browse hover overlay could survive `initGui()` after a product click rebuilt the terminal into standardized-market `DETAIL` mode. Root rebuild and GUI close now clear transient hover overlays before the new route is drawn.
- Expanded `scripts/market-demo-fixture.sh` to a managed v2 fixture. It seeds every enabled formal standardized product with escrow-backed sell liquidity, frozen-funds-backed buy liquidity, and recent real `market_trade_record` samples for 24-hour volume and hover/detail price history. The fixture is idempotent by product/version and does not mutate player accounts, player custody, or player orders.
- Fixed the standardized buy-order banking correlation overflow: a terminal request id could be valid in the market log yet become too long after `market:buy-freeze:` was prepended for the bank `business_ref VARCHAR(64)`. Long references now use a deterministic UUID-derived compact key, while the full request id remains in the market operation log and bank audit JSON.
- Cancel requests against an order that is already filled/cancelled are now recorded as safe `FAILED` precondition rejections. They no longer rewrite settled orders or custody to `EXCEPTION`; migration `20260720_001_repair_safe_cancel_rejections.sql` repairs the two affected historical cancel logs and their settled order.
- 验证：`git diff --check`、`StandardizedSpotMarketServiceTest`、`TerminalMarketServiceTest`、`TerminalMarketSectionServiceTest`、`MarketItemGridPanelTest` 与 `HoverOverlayPositionerTest` 均通过。无客户端烟测确认严格市场审计异常数为 `0`，正式目录为 8 项、活跃买单 9、卖单 13、24 小时成交 81，8 个商品每项都有至少 10 个真实价格点。runtime jar `1737f92895c92d7d179efee7751c60f228b926da3259c163479bcb76114fd85c` 已部署至 Lobby 与客户端，Lobby 进程为 `RUNNING`。

### 2026-08-08 - Standard market five-level visual closeout

- Compressed the selected-product header to a single row, expanded bid/trade/ask to five rows, split the compact statistics panel into explicit market and account groups, and introduced semantic green/red/amber action buttons.
- Expanded the right chart to use the full detail height. It now connects real price samples with a blue path, retains red/green volume bars, and labels intermediate time coordinates in addition to price and volume axes.
- Upgraded `market-demo-fixture.sh` to v3 with five escrow-backed ask levels, five frozen-funds-backed bid levels and 24 time-ordered real trade samples per enabled catalog product. No client-rendered fake values were added.

### 2026-07-20 - 市场三段式整改 Phase 3：定制与汇率市场接入共用浏览/详情骨架

- 定制市场和汇率市场迁移到共用 `BROWSE -> DETAIL` 客户端状态：浏览使用四列网格，详情单独占用工作区；市场语义仍分别保持挂牌交付链与任务书硬币正式报价链。
- 定制市场保留全部挂牌、我的出售、待领取三个服务器权威范围。切换范围会清空旧选择并请求对应 scope 的新 snapshot，避免本地状态变化而网格继续显示旧范围。
- 汇率市场按完整 `TaskCoinCatalog` 浏览所有任务书硬币，点击仅选择用于展示的目录币种；兑换执行继续由服务端验证玩家实际手持物、报价有效期和限额，不能通过浏览选择绕过 gate。
- 新增 `MarketBrowseDetailController`，统一保存查询、页码、网格滚动位置与当前选择；空 snapshot 会彻底重置该临时状态。更新终端布局测试，使其校验新 browse/detail surface 的边界契约，而非已移除的双常驻面板。
- 验证：`git diff --check`；Docker Gradle 定向通过 `TerminalServiceTest`、`TerminalMarketActionMessageFactoryTest`、`TerminalMarketSectionContentTest`、`TerminalShellPanelsScrollTest`、`MarketItemGridPanelTest`；`assemble` 通过。未启动客户端或做截图验收。

### 2026-07-20 - 银河仓储网络 v1：AE2 实体仓储方向确认

- 确认未来个人、企业和公共仓优先复用 GTNH AE2 的真实存储元件、容量、频道、供电和网络拓扑；JGB 不实现无限虚拟背包，也不复制 ME 单元的物品余额。
- JGB 的责任收口为仓库账户、权限、登记端口、资产域转移、审计、恢复与容量健康摘要；标准市场托管继续是独立数据库结算账本。
- 新增 `docs/warehouse-ae2-integration-v1.md`，定义 `Warehouse Port -> Warehouse Controller -> 已登记 AE2 网络` 的受控接入模型，以及市场托管桥、物品边界与分阶段实施顺序。

### 2026-07-20 - 银河仓储网络 Phase 0：账户绑定 AE2 Drive 调查

- 根据 AE2 Unofficial 源码确认，未来 JGB 仓储的准确模型调整为“账户绑定的
  `Warehouse Drive` + 真实 AE2 Storage Cell”，而不是 JGB 虚拟仓或仅靠 Port
  接入任意 ME 网络。
- `Warehouse Drive` 将以 AE2 `IChestOrDrive` / `ICellContainer` 的方式作为真实
  Grid Host 接线；AE2 自动纳入其 Cell，JGB 不复制物品内容、不另造终端。
- JGB 管理 Drive 归属、Cell Bay 数、插拔/拆卸锁、端口交割、审计与恢复；市场
  托管继续为独立数据库资产域。
- 记录关键边界：接入同一 AE 网络后的原生终端读写需依赖 AE2 Security 的
  `INJECT` / `EXTRACT` 权限；企业/公共仓 v1 不承诺在普通 AE 线缆上实施逐物品
  JGB 权限过滤。详细调查、风险与 Phase 0 尖峰清单见
  `docs/warehouse-ae2-integration-v1.md`。

### 2026-07-20 - 银河仓储网络 Phase 0：Base Vault 起步保险箱补充

- 明确玩家无 AE2 元件时仍需要一个账户绑定的基础存储：`Base Vault`，体验类似
  末影箱但由 JGB 的 PostgreSQL 槽位账本保存，初始固定 27 格，完整保留
  `ItemStack` NBT，支持跨灰度服、企业/公共账户权限、幂等交付与恢复。
- Base Vault 是有限的 `BASE_VAULT` 资产域，承担市场 `CLAIMABLE`、定制交付和
  离线奖励的安全第一落点；满仓时资产留在来源域/PENDING 状态，绝不丢失或重复发放。
- Base Vault 不接入 AE2 Storage Grid；真实 AE2 Cell 容量仍由账户绑定
  `Warehouse Drive` 提供。后续通过受审计 Port 在 Base Vault、AE2 Drive 与市场
  托管之间迁移，避免形成无成本无限 ME 存储。
## 2026-07-24 - Base Vault native container polish

- Compressed the personal Base Vault container to the natural 9 x 3 chest-plus-player-inventory proportion, removing redundant Vault/player-inventory headings and the persistent Shift instruction.
- Kept the Galaxy Terminal visual skin but simplified the header to `银河终端` plus capacity, matching the spatial hierarchy of an Ender Chest rather than a terminal documentation page.
- Added a compact header return button that closes the container and reopens the Galaxy Terminal through the existing terminal approval route.

### 2026-08-07 - Base Vault audited server sort

- Added a Vault-only sort request and header control. It never registers the persistent Vault with Inventory Bogo Sorter and never mutates player inventory, cursor or drops.
- `BaseVaultService` now locks the personal account, deterministically groups/merges full-NBT-identical stacks using `registry-meta-nbt-v1`, and commits changed slots atomically.
- Added `warehouse_operation_slot_change` migration. Every changed sort slot records before/after compressed ItemStack snapshots and version values under its `VAULT_SORT` operation for recovery and audit review.
- Generalized the same audited sort contract to `ENTERPRISE` and `PUBLIC` Base Vault accounts, each with its fixed 54-slot capacity. Their GUI opening remains blocked until enterprise/public authority checks exist; the service path is available to those future authorized controllers now.
- Validation: `git diff --check`, Docker `testClasses`, and targeted Docker tests for `BaseVaultServiceTest` / `VaultSortPlannerTest` pass. The 54-slot enterprise/public test performs a real reorder and asserts two audited slot deltas per account type. No server, client, or database deployment was performed.
- Deployment follow-up: migration `20260807_001_add_base_vault_sort_audit.sql` applied successfully and runtime jar SHA256 `c19e783cde05a651868c6bf72ec123b57dc40ce7c4977c4fbb630d3afbed538d` was copied to Lobby, S2 and the GTNH client. Lobby reached `RUNNING` and logged `Done`. S2 received the same jar but remains `EXITED` because both `World/level.dat` and `level.dat_old` are corrupt; no world recovery or rollback was attempted.
- Vault UI follow-up: replaced the header's text-only `S` sort control with an icon-only three-row sort glyph and a hover label (`整理保险箱`), keeping the action auditable and visually compact.
### 2026-08-07 - 现代交易终端改造方案

- 主题：将标准市场从手动托管操作页收口为以账户仓自动交割为基础的现代交易终端。
- 决策：个人 Base Vault 是当前账户仓；未来已授权 AE 仓优先、Base Vault 回退。卖单确认时内部锁定账户仓物品进入市场托管，买入后自动投递账户仓；只有投递失败才显示待领取恢复动作。`AVAILABLE`、`ESCROW_SELL`、`CLAIMABLE` 与冻结资金保留为审计状态，不再作为玩家日常操作步骤。
- UI 方向：四列浏览卡必须显示真实价格波动；Hover 提供只读行情摘要；点击后进入含真实 K 线/成交量、盘口和可编辑买卖交易单的独立详情页。详见 `docs/modern-trading-terminal-redesign-v1.md`。

### 2026-08-08 - 非标准市场三段式浏览服务端收口

- 定制市场与汇率市场的浏览页统一使用服务端输出的结构化 `TerminalMarketBrowseEntry`、查询和分页边界；客户端不再以平行字符串列表或本地任务书硬币目录作为主要真相来源。
- 汇率市场把用户点选的目录币种保留为详情上下文，但正式报价和执行仍以服务器验证的个人 Base Vault 格位为准，不能通过 UI 选择绕过资产校验。
- 补充 Custom/Exchange 浏览页终端网络包往返测试，覆盖条目、查询词、页码与总数。业务结算、定制挂单交付与标准市场撮合均未改写。

### 2026-08-11 - 市场与仓储边界收口

- 终端请求与快照追加单调递增的响应序号，并保留旧包兼容解码。客户端只接收不早于当前已应用响应的快照，解决汇率报价、自动刷新和页面切换之间的异步回写竞态。
- 标准、定制与汇率市场统一使用轻量自动刷新；输入框正在编辑、确认弹窗打开或页面已经切换时不会用后台刷新覆盖玩家上下文。普通汇率刷新只读取现有报价状态，不再隐式生成新报价。
- 清理玩家可见的协议和资产工程术语：确认弹窗、空态和反馈改为商品、账户仓、待收货、报价状态等玩家语言；内部审计状态与兼容路由继续保留，不作为新 UI 主流程入口。
- 新增 Base Vault 授权地基：个人仓仅本人和管理员可访问；企业/公共 54 格仓为后续权限适配预留，未提供权威授权时默认拒绝。VIEW、DEPOSIT、WITHDRAW、SORT 分离，避免未来组织仓沿用个人仓判断。
- `scripts/market-audit.sh --strict` 扩展仓储运维检查：未完成/待恢复操作、账户容量契约（个人 27，企业/公共 54）和越界槽位，并输出可行动记录及槽位变更数量。
- 本轮自动验证覆盖协议兼容、响应序号、三市场业务服务、市场页面状态、自动刷新、Base Vault 权限与 54 格容量。真实玩家资金和物品交易验收仍由服主单独执行，不以静态测试代替。
- 完整 Docker Gradle 测试通过：共执行 316 项测试，31 项按既有条件跳过，无失败；`git diff --check` 与 `scripts/market-audit.sh --strict` 通过，实时审计 `anomaly_count=0`。审计发现的两条历史失败操作均为对已成交订单的重复撤单，服务端已安全拒绝，不计为资产异常。
- 构建产物 SHA-256 为 `6a151221d6a0c8c8f999cac46c476888e5b5e63d5ff9464c13695c940f69ad72`，Lobby、S2 和客户端目录中的模组文件哈希一致。Lobby 已进入 `Done`，市场启动烟测确认数据库正式目录 `8/8` 准入；S2 虽已替换同一产物，但仍因既有世界数据初始化崩溃而退出，本轮未擅自重建、回滚或修复其世界。
### 2026-08-12 - 市场操作反馈 Toast 与请求 ID 长度收口

- 定位 Lobby 标准市场卖出失败并非数据库断连，而是市场请求 ID 在追加 `:custody` / `:order` 等派生后超过 PostgreSQL `VARCHAR(64)`；新增统一请求 ID 长度预算，根 ID 固定为最多 46 字符，为最长恢复后缀预留空间。
- 市场异常反馈区分连接不可用、数据库约束拒绝和普通业务拒绝，不再把原始 JDBC 文案直接显示给玩家；失败事务保持服务端回滚语义。
- 客户端将市场/银行/传送动作的成功、警告和失败通知桥接到全局右上角 Toast；静态 INFO 说明不会反复弹出。Toast 改为响应式宽度、最多四行正文，错误提示保留 7 秒，便于阅读真实失败原因。
- 验证：`git diff --check` 通过；Docker Gradle 定向执行 `MarketRequestIdFactoryTest`、`TerminalMarketServiceTest` 与 `TerminalServiceTest` 通过。部署后 Lobby 达到 `Done (1.560s)!`，构建产物、Lobby 与 Prism 客户端 JAR SHA256 均为 `5099c5bd49b0af144bfda1cc42c7d109c6313623a10101a044dcd54ea30e1c97`。PostgreSQL 实际约束仍为 `market_operation_log.request_id VARCHAR(64)`，新根 ID/派生后缀测试覆盖此边界。
# 2026-08-12 标准市场个人历史与动作回执收口

- 标准商品详情第三按钮从无目标撤单改为“历史”，新增账户级跨商品订单历史视图。
- 历史支持商品范围、买卖方向、订单状态和时间范围筛选；仅具体可撤订单行提供撤单动作。
- 终端内动作通知按请求序列去重，并在展示前屏蔽数据库/异常实现细节。
- 订单摘要补齐商品键、方向、总量、成交量、剩余量、状态和创建时间，便于玩家追踪部分成交与撤单结果。
- 历史筛选和分页正式下沉到服务端数据库查询：客户端只提交商品范围、买卖方向、状态、时间、页码和页大小，账户身份由服务端当前登录玩家解析。
- 历史页筛选、翻页和具体订单撤单后均重新请求服务端；撤单 payload 保留当前筛选上下文，不再退回默认历史页。
- 修复标准市场草稿默认值补全时丢失历史查询字段的问题，并补充筛选 payload、精确撤单上下文、分页元数据网络往返测试。
- 验证：`git diff --check` 通过；Docker Gradle 定向执行 `TerminalMarketSectionServiceTest`、`TerminalMarketSectionStateTest`、`TerminalMarketActionMessageFactoryTest` 和 `TerminalMarketCatalogPacketTest` 通过。

### 2026-08-15 - 标准市场本地化名称与盘口语义收口

- 标准商品身份继续使用稳定商品键与 `registry + meta`，不把客户端语言写入数据库；商品浏览、详情 Hero 和订单历史改为从真实 `ItemStack` 读取当前客户端语言的显示名，数据库目录名仅作为管理、搜索和异常回退文本。
- 行情图继续允许使用 `CARRY_FORWARD` / `REFERENCE` 桶保持时间轴连续，但“最新成交”列只展示真实成交记录，不再把补齐行情误报为 `x0` 成交。
- 盘口标题统一为“买盘 / 最新成交 / 卖盘”，账户区统一为“可用库存 / 冻结资金 / 当前委托 / 待入库”，并以库存箱、入库箭头和订单单据图标替代三个含义相同的状态点。
- 验证：`git diff --check` 通过；Docker Gradle 完整 `test` 通过（`BUILD SUCCESSFUL`）。
- 部署：构建产物已同步到 Lobby、S2 与客户端，三处 SHA-256 均为 `4e416c24eeb56d4f4f18cd1d8aa01c1d514015bcbe6608993f076cabf9d75826`；Lobby 已启动至 `Done`。S2 仍因既有 `World/level.dat` 与 `level.dat_old` ZLIB/EOF 损坏退出，本轮未擅自重建或回滚世界。
# Standardized market UX completion baseline (2026-08-15)

- Added `docs/standardized-market-ux-completion-v1.md` as the independent completion matrix for order entry, freshness, order history, notifications, localized discovery, charts, explicit states, and non-color-only status.
- The work deliberately preserves server-authoritative pricing, fees, inventory, banking, matching, delivery, and recovery semantics.
- Manual visual acceptance remains a final step after targeted tests and Lobby/S2/client deployment.

### 2026-08-15 - Standardized market UX completion implementation

- Completed the modern order ticket with percentage quantity shortcuts, bid/latest/ask price shortcuts, explicit source/destination, gross estimate, server fee policy, and concrete disabled reasons. Server-side settlement remains authoritative.
- Added explicit live-data freshness states (`FRESH`, `REFRESHING`, `DELAYED`, `STALE`) and prevented localized search text from being replaced while the stable product-key request is in flight.
- Improved personal order history with localized product names, percentage fill progress, lifecycle markers, and cancellation only for orders with an open remainder.
- Replaced generic blank browser panels with distinct catalog, search, filter, and stale-data empty states. Important states continue to include text or numeric meaning in addition to color.
- Validation: `git diff --check`; Docker Gradle targeted tests passed for `TerminalMarketSectionStateTest`, `TerminalMarketSectionContentTest`, `MarketLiveRefreshControllerTest`, `MarketOrderEntryPopupTest`, `TerminalShellPanelsScrollTest`, `TerminalHomeScreenLayoutTest`, `TerminalHomeScreenModelTest`, `TerminalMarketActionMessageFactoryTest`, `TerminalServiceTest`, and `TerminalMarketServiceTest`.
- Dynamic visual acceptance is intentionally left to the user after deployment: localized search, ticket shortcuts and disabled reasons, freshness transitions, history filters/cancellation, notifications, chart interaction, and empty/stale states.
- Deployment verification: runtime artifact SHA-256 `a975e711dd8adbc59d4828164fddd00ff8fe16a0ea0584c92e68cded7a174fd6` matches the Lobby, S2, and Prism client copies. Lobby reached `Done (1.406s)`. S2 received the same artifact but remains `EXITED` because the pre-existing `World/level.dat` and `level.dat_old` both fail with ZLIB/EOF corruption; no S2 world repair, replacement, or rollback was attempted.

### 2026-08-16 - 标准市场撤单与订单资产中心首轮路由收口

- 商品详情的第三主动作改为 `撤单`，详情快照只读取当前商品的个人委托；没有 `OPEN` 或 `PARTIALLY_FILLED` 剩余量时显示明确空态，不会误指向其他商品的订单。
- 新增受限的 `MarketAccountCenterQuery` 与 `MarketAccountCenterSnapshot` 合同：账户所有者不在客户端查询中，搜索词、页码和每页数量均在服务端值对象中收敛；快照以服务端总数计算页数。
- 新增 `MARKET_ACCOUNT_CENTER` 路由并复用已有服务器分页查询和逐行撤单链，页面标题与状态栏明确为“订单与资产中心”。旧历史 payload 保留兼容解码，标准市场撮合、银行、Base Vault 与恢复服务未改写。
- 验证：`git diff --check` 通过。Docker Gradle 编译在本轮环境中启动后未在工具的 30 秒输出窗口内返回最终状态，未部署 Lobby、S2 或客户端。

### 2026-08-16 - 标准市场撤单与订单资产中心完整整改

- 商品详情固定为 `买入 / 卖出 / 撤单`；当前商品撤单弹窗只展示本人 `OPEN` / `PARTIALLY_FILLED` 且剩余量大于零的委托，支持表格区滚动。二次确认展示短订单号、方向、成交/总量、剩余量、预计返还和真实返还目标。
- 撤单请求新增幂等 `requestId` 与订单更新时间版本；终端与业务服务均重新校验玩家归属、状态、剩余量和版本。确认期间变化会警告并刷新；过期版本被归类为安全拒绝，不会把正常订单误标为异常，也不会重复解冻或返还。
- 新增独立全屏 `订单与资产中心`：六项账户摘要、`当前委托 / 成交记录 / 资产与交付 / 历史查询` 四页签、统一筛选栏、唯一表格滚动区与固定分页脚。市场与 Vault 顶栏图标分别直达当前委托和资产交付页签。
- 新增受限的 `MarketAccountCenterQuery`、四类结构化 `MarketAccountCenterSnapshot` 行及结构化网络 DTO。商品身份使用 `registry + meta`，客户端创建真实 `ItemStack` 并本地化名称；中心 UI 不再拆解旧历史显示字符串。旧历史 payload 仅保留兼容解码。
- 当前委托与历史、成交、市场异常、Base Vault 异常均由服务端当前玩家身份查询并使用真实总数分页。资产页合并待收货、Vault 满仓、市场返还/银行解冻失败、Vault `FAILED` 与 `RECOVERY_REQUIRED`；不自动执行恢复动作。
- Toast 支持点击进入对应页签、按记录号筛选并高亮目标。定制挂牌和汇率兑换继续保持原业务入口，不混入标准订单中心。
- 自动验证新增查询上限、`0/1/4/5/8/9/11` 分页边界、`11/4=3`、结构化协议往返、过期版本撤单和重复请求不重复解冻测试。定向 Docker Gradle 已执行 83 项并全部通过；最终完整测试、diff 检查和部署结果见本次交付记录。
- 最终验证：`git diff --check` 通过；Docker Gradle 完整 `test` 通过（298 项完成，32 项按既有条件跳过，无失败）；Lobby 市场烟测通过，数据库 8 个启用标准商品全部被运行时准入，8 类商品均有真实成交数据。
- 部署：`scripts/deploy-jgb.sh --targets lobby,s2,client` 已构建并同步 runtime jar，源码产物、Lobby、S2 与客户端四处 SHA-256 均为 `b435264bb2e6166f291295ad5f7044699791cda6abdaf435c14196e8d650ae9b`；未启动客户端，也未触碰 S1。Lobby 达到 `Done (1.423s)`。S2 收到同一产物后仍因既有世界 NBT 的 `Unexpected end of ZLIB input stream` / `EOFException` 退出，本轮未修复、替换或回滚其世界。

### 2026-08-17 - 订单中心入口、撤单弹窗实底与 GregTech 中文名复核整改

- 根据实际客户端截图复核，确认首轮交付仍有三处不合格：订单中心入口只是 8–12 像素的无文字标题栏图标且中心路由自身不显示入口；当前商品撤单弹窗覆盖了 `drawSelf` 却未绘制父级弹窗底板；GregTech 商品仍可能直接落回目录英文名。
- 市场与 Vault 标题栏入口改为带订单图标的 `订单中心` / `资产中心` 宽按钮，订单中心路由自身继续显示入口并使用亮蓝边框与高亮填充，避免把侧栏的“市场”选中态误称为订单中心高亮。
- 撤单弹窗恢复父级 `ModalPopupPanel` 底板绘制；`POPUP_FILL` 从 alpha `0xF0` 调整为 `0xFF`（100% 不透明）。屏幕外层压暗遮罩保持 `0xAA`（170/255，约 66.7% 不透明），两者语义分离。
- 已核对 Prism 实例 `options.txt` 为 `lang:zh_CN`。商品浏览、详情、撤单标题和结构化商品行统一使用 `registry@meta` 解析真实 `ItemStack`；GregTech 元物品通过其 `GTLanguageManager` 读取中文格式，并使用 OreDict 关联材质替换 `%material`，例如 `Material.steel=钢` 与 `%material锭` 组合为 `钢锭`，不再把服务端英文目录名作为正常显示结果。
- 验证：`git diff --check` 通过；Docker Gradle 定向测试通过 `TerminalMarketVisualsTest`、`TerminalShellPanelsScrollTest`、`TerminalMarketSectionContentTest`、`TerminalThemeRegistryTest`；Docker Gradle 完整 `test` 通过（`BUILD SUCCESSFUL`）。未启动客户端或自动截图。
- 部署：`scripts/deploy-jgb.sh --targets lobby,s2,client` 已构建并同步；构建产物、Lobby、S2 与 Prism 客户端四处 SHA-256 均为 `69a9a68f82c24a57f24c2661dbb3592c6b7280c75ac91d21e50eca7f484bc4a3`，客户端文件时间为 `2026-08-17 12:28:16 +0800`。Lobby 达到 `Done (1.365s)`；S2 仍因既有 `level.dat` / `level.dat_old` 的 `Unexpected end of ZLIB input stream` / `EOFException` 未到 Done，故部署脚本最终返回非零。未启动客户端、未自动截图，目标列表未包含 S1。

### 2026-08-17 - 撤单弹窗终端内约束与订单中心内容区扩容

- 根据实际客户端截图修正缩放布局：当前商品撤单弹窗不再以整个 Minecraft 屏幕为定位边界，改为使用 `TerminalHomeLayout.panelBounds`。弹窗四边始终留在终端内部，高度按实际委托行数计算，最多直接展示 5 行，更多记录只在表格区滚动；单行/空态不再占满终端高度。
- 订单与资产中心重新分配纵向预算：账户摘要卡由 34 压缩至 28，页签由 20 压缩至 18；宽度不少于 600 时，搜索、搜索/重置动作及商品、方向、状态、时间筛选合并为单行。表头偏移从 120 降至 80，窄界面保留双行工具栏并使用 103 偏移，固定页脚和服务端分页语义不变。
- 新增弹窗宿主边界/内容驱动高度测试，以及订单中心宽窄布局的表格预算测试。Docker Gradle 定向测试与完整 `test` 均通过（`BUILD SUCCESSFUL`）；`git diff --check` 通过。未启动客户端或自动截图。
- 部署：`scripts/deploy-jgb.sh --targets lobby,s2,client` 已构建并同步；构建产物、Lobby、S2、Prism 客户端四处 SHA-256 均为 `59ca489d69a3d485a6b1f755e6764ed35ce868c3ea2f9ac474b96d8cf43115bf`，客户端文件时间为 `2026-08-17 22:40:57 +0800`。Lobby 达到 `Done (1.548s)`；S2 仍因既有世界 NBT 的 ZLIB `EOFException` 未到 Done，部署脚本因此返回非零。未启动客户端、未自动截图，部署目标未包含 S1。

### 2026-08-24 - 个人地产 Batch C.2 大地图与交互改造

- 地产附近地图从居中正方形改为完整矩形工作区，最长边按近 9、中 15、远 31 个正方形区块显示；地图工具栏、图例和指针状态收进画布，右侧检查器限制为紧凑宽度。
- 玩家标记改为真实客户端皮肤头像定位针并带方向箭头；悬浮补齐安全长整型方块范围、距离、加载/生物群系、产权号/版本、所有者脱敏状态和操作原因。
- 接入左键选择、4px 拖拽阈值、滚轮锚点缩放、右键权威快照后菜单、`F/R/+/-/Tab/Esc`。确认框打开时继续由终端 popup 层独占输入，认领/放弃仍走服务端身份、半径、版本和幂等校验。
- 认领费用、参考地价、挂牌价和最近过户价只显示“未启用 / 未挂牌 / —”；本批没有增加数据库字段、银行扣款、客户端身份字段或地产交易合同。
- CodeGraph 前置查询覆盖 `TerminalLandMapPanel`、`LandMapViewport`、`TerminalLandSection` 和 `CanvasScreen`；自动测试覆盖矩形数学、输入分流、图层状态和最小 620×340 布局。最终构建、部署和哈希记录在本批交付结果中补齐。
- 最终验证：Docker Gradle 全量 `test` 通过（409 项，37 项环境跳过，0 失败）；另以 Lobby PostgreSQL 配置强制执行 `LandPostgresIntegrationTest` 5 项，0 跳过/失败。`assemble`、语言文件结构检查和 `git diff --check` 均通过。
- 部署：同一 runtime JAR 仅同步到 Lobby 与 Prism 客户端，三处 SHA-256 均为 `7725268364894f4dacd3b25dd7db8917930d06f1c2719bbda36943724f8bc45d`；客户端未启动，S1/S2 不在目标中。Lobby 达到 `Done (1.435s)`，地产运行时以 `SHADOW` 就绪并恢复 1 条有效产权；Lobby 与客户端均继续保留 `ServerUtilities-2.2.2.jar`。
# 2026-08-31 - 物品准入策略终端诊断页

- 终端新增“物品策略”只读页：显示服务器已解析拒绝规则，以及当前玩家、本服范围内最近 12 条拒绝审计。页面不扫描背包、不改写既有物品，也不暴露其他玩家或跨服记录。
- 策略未启用、运行时不可用和审计数据库暂时不可读均显示明确状态；策略本身不会因诊断查询失败降级为允许。
- 新增 PostgreSQL 查询隔离/时间排序覆盖，并回归终端导航与禁用策略安全空态。
# 2026-08-31 - 全局 Hub / Spawn 与目标服 RTP

- `/spawn` 现在可由服务端 `globalHubTarget` 配置提升为跨服全局 Hub；未配置时严格保留当前世界出生点语义。
- `/rtp <targetServerId>` 使用现有 transfer ticket：源服只校验配置和服务器目录，目标服才在自己的维度选择安全随机点，随后以 ticket requestId 幂等写入最终 RTP 审计。源服不会访问或加载远端世界。
- 新增 `20260831_002_add_global_entry_rtp_audit.sql`，为 RTP 审计补齐 requestId 和实际目标服；覆盖配置解析、远端计划不预写伪坐标、目标服解析与 PostgreSQL 迁移/幂等写入。
- 验证：Docker 定向单测、Lobby PostgreSQL 临时 schema 集成测试、完整 Docker `test` 与 `assemble` 均通过；完整测试为 465 项通过、48 项既有环境条件跳过。
- 部署：`20260831_001_add_servertools_admin_audit.sql` 和 `20260831_002_add_global_entry_rtp_audit.sql` 已按迁移历史表应用。runtime JAR SHA-256 `6409361a211ca9644d0031382906f2af625e17de2fe899aadc5db1f548116b20` 已同步至 Lobby 与 Prism 客户端；Lobby 已到 `Done (1.296s)`，客户端未启动，S1/S2 未触碰。全局 Hub 与目标服 RTP 配置保持默认空值，未在本轮启用。

# 2026-09-02 - 银河资产中心统一容器与视觉重构

- 将终端仓储入口重构为紧凑的双列 Vault/Bay 状态卡、两个直接工作区入口和自适应审计区，移除窄终端下强制纵向堆叠造成的越界。
- 新增接近全屏的 `资产概览 / Base Vault / AE2 存储 / 操作记录` 四页签资产中心。Base Vault 27 格、单个真实 AE2 Cell Bay 与玩家背包由同一个服务器权威 Container 管理，不再弹出两套简陋小窗口。
- Forge 只注册一个资产 GUI handler，并兼容旧 Vault/Bay GUI ID；修复同一 Mod 实例重复注册 handler 可能覆盖前一个入口的问题。
- 服务端按页签拒绝隐藏槽位点击，Vault 与 Bay 继续分别使用既有版本、幂等和审计合同；失败交互恢复 Vault、Bay、玩家背包和光标快照。操作记录仅查询当前玩家并合并普通 Vault/Bay 记录。
- 不伪造银行、ME 网络、频道、供电或 64K 容量；Cell 未插入时显示真实空态，插入后才读取其真实 AE2 容量与类型数据。
- 自动验证覆盖 `350×193` 和 `620×340` 布局、终端入口不越界、页签槽位可见性、审计协议上限、Vault 最近操作隔离以及 Bay/Vault 服务回归。Docker Gradle 全量 `test`、使用 Lobby PostgreSQL 配置的 `WarehousePostgresIntegrationTest`、`assemble` 与 `git diff --check` 均通过。
- 部署：runtime JAR、Lobby 与 Prism 客户端三处 SHA-256 均为 `c36ee1b9a837abdd82abe3fa7ddeb01ccf7d642b9342f63804be2fafba5a9aeb`。Lobby 完成 `FMLServerStartedEvent`，日志确认 `Registered unified personal asset center container GUI`；客户端未启动，部署目标未包含 S1、S2。

# 2026-09-02 - 资产显示框架与业务动态后续待办

- 实机复核确认独立资产 Container 仍存在两项架构问题：它没有复用终端壳组件，返回时关闭容器后重新请求终端会闪屏；当前“操作记录”把手工槽位变更和 Bay 版本审计暴露成了玩家内容。
- 后续不把整个终端根页面升级成 Container。计划抽离无继承关系的 `CanvasSceneRuntime`，由普通 `GuiScreen` 宿主和原生 `GuiContainer` 宿主共同委托；在其上分别建立 `TerminalScreenBase` 与 `TerminalContainerScreenBase`，并共享状态栏、导航、主题、弹窗和内容区布局。
- 资产 Container 页面将直接展示 Vault、Cell Bay 与玩家背包，移除说明卡和二次入口；服务端 Container、版本、准入、失败恢复与技术审计继续保留。
- 玩家可见记录改为结构化“资产动态”，只纳入真实卖出成交到账、买入入库、定制交付、撤单返还以及交付/恢复结果；手工存取、整理、Cell 插拔等记录只留在后台审计。卖出不得以挂单托管代替成交，业务链需用稳定关联键去重。
- CodeGraph 复核：`CanvasScreen` 的框架影响面约 47 个符号，而直接改造 `TerminalHomeScreen` 会影响约 202 个符号，因此采用组合式双宿主框架。此次仅更新待办与设计文档，没有修改代码、数据库或部署状态。

# 2026-09-04 - 终端 GUI 双宿主框架与资产页原生化

- 抽取无 Minecraft 宿主继承的 `CanvasSceneRuntime`；`CanvasScreen` 与新增 `CanvasContainerScreen` 共同委托根 Panel、Popup、Hover、主题和输入分派。新增普通/Container 两种终端基类，并用 `TerminalShellFrame` 复用状态栏、导航、背景和 `TerminalHomeLayout`。
- 资产导航现在直接打开统一 Container。存储管理在同一会话中显示 27 格 Base Vault、真实 AE2 Cell Bay 与玩家背包；窄屏纵向堆叠、宽屏并排。背包 Shift-click 固定进入 Vault，Cell 仅能手动拖入 Bay，Vault/Bay 可 Shift-click 返回背包。
- 新增同周期 `TerminalRouteCoordinator`，Container 返回或切换普通终端页面时立即恢复缓存终端壳并请求服务端快照，不再先关闭到游戏画面。
- 玩家“操作记录”改为“资产动态”：结构化投影标准市场卖出结算、实际 Vault 买入交付、定制市场交付、撤单返还、失败与恢复；手工存取、整理和 Cell 插拔仍保留后台审计但不显示。新增定制成交按玩家隔离的近期查询，不新增数据库表。
- CodeGraph 前置确认 `CanvasScreen` 影响 47 个符号、`TerminalAssetCenterContainer` 影响 51 个符号、`TerminalHomeScreen` 影响 202 个符号，因此保留 `GuiScene` 合同并采用组合迁移；完成后重新同步索引并复核 `CanvasSceneRuntime`、`CanvasContainerScreen`、资产 Container 与资产动态投影的影响链。
- 自动验证：双宿主/布局/协议/资产动态定向测试通过；Docker Gradle 全量 `test` 为 492 项、52 项既有环境条件跳过、0 失败；Lobby PostgreSQL 上强制执行市场集成 15/15 与仓储集成 4/4，均 0 跳过、0 失败；`assemble` 与 `git diff --check` 通过。
- 部署：同一 runtime JAR 仅同步到 Lobby 与 Prism 客户端，构建产物和两处目标 SHA-256 均为 `472c675afb3b987da5484ad47420a3262eb23c8b0613e50f2272e07d3e95e717`。Lobby 达到 `Done (1.312s)`；客户端未启动，部署目标不包含 S1、S2。本批没有新增或修改资产数据库结构。

# 2026-09-05 - 资产中心单 Cell 内容浏览与左右分区

- 存储管理调整为明确的左右结构：左侧同时显示 27 格 Base Vault 与玩家背包，右侧始终保留一个真实 Cell Bay；Cell 未插入时不显示空内容网格，插入后直接显示该 Cell 的分页内容。
- 新增服务端 Cell 内容快照与有限存取动作。每种物品按虚拟条目显示真实 `long` 数量；左键提取一组、右键提取一个，手持物品经明确按钮存入。容量、类型与物品准入继续由 AE2 Cell inventory 和现有物品策略决定，不建立外部 AE 网络或第二套库存。
- Cell 写操作复用 Bay 的 PostgreSQL 产权行、版本和操作审计：请求使用幂等 `requestId`，同时校验所有者、Bay 版本和当前服务器侧光标；客户端不能提交玩家身份或任意存取数量，提取物品标识也只作不可信提示并由当前 Cell 重新匹配。协议保留旧资产动态快照和旧请求的兼容解码。
- 本轮只完成资产中心功能与布局，现代化平滑主题暂不实施，等待实机核对后再单独设计。
- 验证：CodeGraph 完成变更后同步并复核资产 GUI、Bay 服务、快照协议和受影响测试链；仓储/协议/布局定向测试、仓储 PostgreSQL 4 项集成测试、全量 `test`（496 项，52 项既有环境条件跳过，0 失败）、`assemble` 与 `git diff --check` 均通过。
- 部署：runtime JAR、Lobby 与 Prism 客户端三处 SHA-256 均为 `edce03d59ac46d70903f9962dc55ff87bd9674dbf7170d0ee0b1d144d7a3223c`。Lobby 达到 `Done (1.281s)` 并注册统一资产中心 Container；客户端未启动，部署目标不包含 S1、S2。本轮没有新增或修改数据库结构。

# 2026-09-05 - Galaxy UI 2 现代 Java 框架决策

- 新增独立设计文档 `galaxy-ui-2-modern-java-framework.md`，确认采用“Vue 式架构、Minecraft 原生渲染”：借鉴组件、Props、Event、Slot、响应式派生状态和单向数据流，不嵌入 JavaScript、DOM、CSS 或 Chromium，也不机械翻译依赖浏览器运行时的 Vue 源码。
- 明确 Forge/Minecraft 只提供 Screen/Container/Slot、输入回调、纹理、字体和 OpenGL/Tessellator 等底层能力；当前 `Canvas*`、Panel、Theme 和输入调度均是项目自建层，下一代必须自行实现约束布局、DrawList、抗锯齿字体、设计 Token、焦点/模态和现代组件。
- CodeGraph 复核当前 `GuiPanel` 影响约 558 个符号、`TerminalShellFrame` 约 255 个、`CanvasSceneRuntime` 约 98 个。最终仍会铲除旧框架，但采用独立 UI2 内核、UI Lab、双宿主、资产中心纵向切换、全页面迁移、最后删除旧框架的替换顺序；Git 提交只解决回滚，不替代迁移期间的可编译基线和行为对照。
- 本轮只记录架构决策和实施路线，没有修改客户端代码、业务合同、数据库或部署状态。

# 2026-09-06 - Galaxy UI 2 C.0-C.2 生产组件与资产中心正式迁移

- C.0：新增 `ComponentUiDocument`、`UiWidgetRegistry`、`UiWidgetAdapter`、`UiActionHandler` 与生产
  `StandardWidgets`；组件绘制先逐组件采集，再按层级稳定合成，解决原生物品层与后续兄弟内容层冲突。
  新增共享 `TerminalAppShell`、普通/Container UI2 终端基类和 `NativeItemBridge`。F8 UI Lab 已迁移到同一生产
  组件实现，不再维护手写 DrawList/裸坐标控件副本。
- C.1：`GuiTerminalAssetCenter` 已切换为 UI2 Container 宿主并新增
  `AssetCenterUiState/Action/Reducer/Effect/Document`。页面保持左侧 Vault 与背包、右侧 Bay 与可选 Cell 内容、
  第二页签系统资产动态；服务端 Container、快照、版本、幂等、准入、失败回滚和无闪烁路由合同未改。旧
  `AssetContentPanel` 以及资产专用旧布局类/测试已删除。
- C.2：三档布局 `350×193`、`427×240`、`620×340`、Modal 阻断、稳定 external region、Cell 中文反馈、
  Vault/Bay/资产动态/协议与双宿主定向回归通过。全量自动测试共 545 项，52 项按既有环境条件跳过，0 失败；
  其中根项目 513 项、UI2 Core 26 项、Lab 2 项、Demo 4 项。Warehouse PostgreSQL 临时 schema 集成测试
  4/4 通过并清理。
- CodeGraph：变更前后均执行 `status/sync`；`GuiTerminalAssetCenter` 影响链为快照消息与 `ClientProxy` 两个外部
  入口，`ComponentUiDocument` 影响资产文档、UI Lab 和组件测试，`TerminalAppShell` 影响两类终端宿主与资产页。
  `affected` 对跨子项目新文件未返回测试，故按影响链人工选择并执行上述定向与全量矩阵。
- 构建：`:ui2-core:test`、`:ui2-lab:test`、`:ui2-demo:test`、`:ui2-demo:renderGolden`、根 `test`、
  `assemble`、`verifyUi2SelfContained` 与 `git diff --check` 均通过。最终 JAR 自包含 UI2 Core/Lab/适配层和许可，
  不含 Qz 运行时引用。
- 部署：同一 JAR 仅同步到 Lobby 与 Prism 客户端，构建产物和两处目标 SHA-256 均为
  `8ea74527024712a2ddba8effe527771d8c7064ebb55a9fe1c9c21d66d502e45c`。Lobby 达到 `Done (1.432s)`；客户端
  未启动，数据库 migration 被显式跳过，S1/S2 不在目标中。
- 当前门槛：C.0/C.1/C.2 自动验收完成，等待玩家人工验证资产中心真实拖放、Shift-click、数字键、双击、Cell
  浏览/存取、Tooltip、Modal 阻断、中文字体和返回路由；通过前不进入 D 阶段。

# 2026-09-06 - Galaxy UI 2 全终端统一路由与业务页面迁移

- 修复首页在高 GUI Scale 下的文字布局：所有 Label 使用主题字号参与测量、换行、省略和裁剪，首页与只读摘要页按窗口宽度切换单列/双列，不再让文本越过卡片或终端窗口。
- 增加客户端独立外观系统和设置页：`glass_mono`、`hacker_green`、`high_contrast` 三套内置主题，Resource Pack 主题索引，紧凑/标准/舒适密度，小窗/标准/宽大窗口和减少动画均即时保存；不进入网络或服务端数据库。
- 建立稳定 `TerminalApplicationScreen` 和 `TerminalPageRegistry`。首页、职业、公共、通知、物品策略、银行、ServerTools、市场总览、标准市场、订单与交付、定制市场、汇率市场和地产均使用同一个窗口化 UI2 宿主；正式普通页面不再回退 `TerminalHomeScreen`。
- 三类市场继续使用既有服务端 payload、玩家身份注入、订单版本、托管、交付与幂等合同。定制发布和汇率输入在现代页面内选择真实 Base Vault 资产，所有确认动作仍由服务端重新校验。
- 地产页面的页签、我的产权、检查器、市场未启用占位、确认框和路由已迁移。真实地形、产权覆盖、拖拽、滚轮、快捷键和客户端缓存暂通过 UI2 `ExternalSurface` 嵌入已验证地图渲染器；Modal 打开时宿主先阻断地图输入。
- CodeGraph 在各阶段执行 `status/sync/impact`，确认正式路由只剩资产 Container 与统一普通宿主。旧 Canvas 完整删除仍有一个明确前置：将 `TerminalLandMapPanel` 抽为无 Canvas 继承的地图后端；在此之前不伪称旧框架已移除。
- 最终自动验收：根项目 538 项（52 项既有环境条件跳过）、UI2 Core 31 项、UI2 Lab 2 项、UI2 Demo 4 项，全部 0 失败；`:ui2-demo:renderGolden`、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 均通过。当前 runtime JAR 为 `jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `e1651ce1c9730f890e37aeed70a1e454117f87c87e5934f13c1ad0b3caa5bdf4`。
- 地产地图后端已从旧 `AbstractGuiPanel/GuiRect` 解耦，改用 UI2 `UiRect`；正式代码中的旧 `CanvasScreen`、`CanvasContainerScreen`、Theme、Popup、Panel、终端页面工厂和重复输入运行时均已删除。`verifyUi2SelfContained` 增加回归守卫，JAR 若重新包含旧 framework/theme 类会直接失败。
- 删除旧专用视觉测试和无调用遗留工具后重新执行完整矩阵：根项目 454 项（52 项既有环境条件跳过）、UI2 Core 31 项、UI2 Lab 2 项、UI2 Demo 4 项，全部 0 失败；Golden 导出、`assemble`、自包含检查与 `git diff --check` 通过。新 runtime JAR SHA-256 为 `7c2b95eccc5209c8d6e9afd1e93a410346c51ebd42b08b6470933fc4c8873fc5`。
- 2026-09-07 部署：同一 runtime JAR 已同步到 Lobby 与 Prism 客户端，构建产物和两处目标 SHA-256 均为 `7c2b95eccc5209c8d6e9afd1e93a410346c51ebd42b08b6470933fc4c8873fc5`。Lobby 重启后为 `RUNNING`，日志达到 `Done (1.455s)`；客户端未启动。部署器只执行既有迁移历史表的幂等存在性检查，输出中没有应用新的业务 migration；S1、S2 未被部署或操作，保持原状态。下一门槛为玩家完整实机视觉与交互矩阵。

# 2026-09-07 - UI2 固定密度、字体来源与文字边界合同

- 根据实机反馈取消玩家可调界面密度和字号：主题继续固定语义字号、行高、间距和控件高度；设置页只保留主题、窗口尺寸、字体来源、减少动画与恢复默认。
- 新增独立字体来源 `MINECRAFT / TERMINAL`。前者直接复用客户端已加载的 `FontRenderer` 并跟随资源包，作为旧配置和恢复默认后的默认值；后者继续使用内置 Noto Sans CJK 平滑字体。普通 Screen 与 Container 的文本测量和绘制统一使用同一选择。
- 旧 `density` 配置键在兼容加载时移除，不进入服务端 Snapshot、网络或数据库。字体选择仍是纯客户端偏好，与主题包相互独立。
- UI2 公共文本流新增固定主题字号下的单行自动适配：优先原字号，空间不足时有限缩小到可读下限，仍不足则省略；异常宽单字不会逃出边界。所有正式终端渲染增加窗口级硬裁剪兜底。
- CodeGraph 前置同步确认 `TerminalPreferences` 影响约 186 个符号、`TextFlow` 约 103 个、`StandardWidgets` 约 80 个，因此改造集中于偏好、双宿主与公共文字组件，没有逐页添加字号补丁。
- 配置迁移测试覆盖独立字体来源持久化、旧 `density` 清理和缺失字体项默认迁移；纯 JUnit 中显式提供 Forge `Configuration` 所需的临时游戏目录，并在测试结束后恢复全局状态。
- 自动验证：UI2 Core/Lab/Demo 与根项目共 499 项（52 项既有环境条件跳过），0 失败；`:ui2-demo:renderGolden`、根 `test`、`assemble`、`verifyUi2SelfContained` 和 `git diff --check` 全部通过。runtime JAR SHA-256 为 `30380622ace6d44f6a576afb0b3ba9dece57ea6fe1664d90628facd889c2cb24`。本轮尚未部署、未启动客户端、未触碰 Lobby/S1/S2 或数据库。

# 2026-09-07 - UI2 统一 Minecraft 字体与语义字号上限

- 根据最终实机决策移除内置 Noto Sans CJK、字体来源选项、后台字形栅格线程、Atlas、逐帧上传和回退切换；UI2 从测量到绘制只使用 Minecraft 当前 `FontRenderer`，自动跟随语言与资源包。
- 客户端兼容加载会清理旧 `density` 和 `fontSource` 配置。设置页只保留主题、窗口尺寸、减少动画和恢复默认；Base JAR 不再包含 CJK 字体或 OFL 文件。
- 公共 `TextFit` 将每个文本框的语义字号视为上限：短文本保持标题/正文/说明的默认层级，溢出时才在可读下限内缩小；单行仍不足则省略，多行先在限定行数内换行，仍不足再省略。文本框光标使用最终适配字号测量。
- CodeGraph `impact` 显示 `MinecraftFontService` 影响链由原 87 个符号收敛为 26 个，`TextFit` 影响 21 个符号；`affected` 对跨模块公共组件未返回测试，因此执行 UI2 Core/Lab/Demo、设置配置与根项目全量矩阵。
- 干净构建验证共 494 项（52 项既有环境条件跳过）、0 失败；`:ui2-demo:renderGolden`、`test`、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 全部通过。JAR 内无 Noto/字体/OFL 残留。
- 已部署同一 runtime JAR 到 Lobby 与 Prism 客户端，SHA-256 均为 `a75dea120b4799159a12cc6706c00cc900e2ea29de27d2415cd892b3064c80f3`；Lobby 由部署器重启并通过完整启动检查，客户端未启动。数据库只执行既有迁移历史表的幂等存在性检查，无业务 migration；S1/S2 未触碰。

# 2026-09-07 - UI2 字体崩溃修复与标准市场工作台恢复

- 根据两份客户端崩溃报告定位到资源包字体测量返回负宽度后直接进入 `UiSize`；新增饱和测量边界，负值、非法缩放和极大结果均被限制为安全尺寸，绘制对齐宽度同步防御。
- 对照 Git 中旧 `TerminalMarketSection`、`TerminalMarketSectionContent` 及既有市场 UX 合同，将过度简化的标准市场 UI2 页面恢复为三栏工作台：左侧商品浏览，中间行情/盘口/个人资产，右侧交易台。
- 重新接回商品筛选、排序、分页，`1h/24h/7d` 行情周期，三档盘口，可用/锁定/待收/冻结资金，以及订单与交付入口；服务端市场 Snapshot、订单 payload、身份、托管和结算合同未修改。
- CodeGraph 前置确认 `MinecraftFontService` 影响 26 个符号、`TerminalStandardMarketDocument` 影响 91 个符号；测试覆盖字体异常测量及三档正式窗口的标准/定制/汇率市场文本边界。
- 定向测试、根项目全量 `test`、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过。runtime JAR、Lobby 和 Prism 客户端 SHA-256 均为 `557d79f2b2295baab5974e384f5e092e1512828c117fae3818b03bebeb0a4de4`；Lobby 完成重启健康检查，客户端未启动，S1/S2 未触碰。

# 2026-09-07 - 标准市场 UI2 渐进式还原整改

- 实机复核确认临时三栏工作台偏离既有市场交互，现已整体替换为“商品目录 -> 悬浮行情 -> 商品详情 -> 下单/撤单浮层或订单中心”的渐进流程；定制与汇率市场没有重排。
- 商品目录使用真实 `ItemStack`、响应式 2/3/4 列网格、搜索/筛选/排序/服务器分页、迷你走势及悬浮行情；详情页恢复左侧商品/五档盘口/24h/个人资产与右侧主图，底部仅保留买入、卖出、撤单、订单中心。
- 新增 UI2 市场图表几何与渲染：OHLC、稀疏折线、单点、空态、成交量、图内十字光标；全部坐标和 long 运算饱和到图表范围，不恢复旧 Canvas 绘制。
- 下单改为稳定键浮层，恢复市价/限价、数量、`25% / 50% / 最大`、`买一 / 最新 / 卖一`、结算预览、禁用原因和二次确认；撤单使用当前商品可撤委托及版本绑定确认。
- 订单与资产中心恢复六项账户摘要、四页签、商品/方向/状态/时间筛选、清除筛选、真实物品行、服务端分页、撤单及领取动作。宿主新增通用外部物品/图表合同，地产地图同步采用同一合同且行为不变。
- 服务端市场 Snapshot、撮合、银行、托管、Vault、交付、恢复、玩家身份、网络 payload 和 PostgreSQL 均未修改；本轮没有数据库 migration。
- CodeGraph `status/sync` 后确认 `TerminalStandardMarketDocument` 影响 161 个符号，通用 `TerminalPageDocument` 影响 256 个符号并覆盖地产外部画布；`affected` 未返回跨模块测试，因此按调用链执行终端、市场、UI2 与全量矩阵。
- 自动验收共 501 项：根项目 456 项（52 项既有环境条件跳过）、UI2 Core 39 项、Lab 2 项、Demo 4 项，0 失败；Golden 导出、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 全部通过。runtime JAR SHA-256 为 `f469da65463007e0fdfabd0f8ec61fb368cab6a428304dd7ccd12eb192d52bb6`。
- 已将同一 runtime JAR 同步到 Lobby 与 Prism 客户端，两处目标 SHA-256 均为 `f469da65463007e0fdfabd0f8ec61fb368cab6a428304dd7ccd12eb192d52bb6`；Lobby 重启后为 `RUNNING`，日志达到 `Done (1.311s)`。客户端未启动，部署器只幂等确认 migration 历史表，没有业务 migration；S1/S2 未触碰。人工门槛为目录、悬浮行情、详情图表、买卖、撤单和订单中心的实机逐项检查。

# 2026-09-07 - UI2 点击、市场详情与帮助层紧急修复

- 检查用户最新客户端崩溃报告 `crash-2026-09-07_19.38.09-client.txt`，确认直接原因是地产“我的产权”页签和内容根重复使用 `land-mine`，`LayoutEngine` 在渲染期拒绝重复键；现将页签与内容键拆为独立稳定常量并增加回归测试。
- 标准市场商品卡点击失败的原因是事件命中卡片内的被动物品/图表子节点后，Card 只接受 TARGET、不接受 BUBBLE；公共控件现只允许“可点击 Card”消费未被子控件处理的冒泡点击，嵌套按钮仍在 TARGET 阶段优先消费，避免双触发。
- 标准市场帮助层增加明确“关闭”操作，并修正帮助状态误走确认框命中区的问题；点击帮助层或按 Enter/Escape 都只关闭当前帮助层，下一次关闭操作才能退出终端。
- CodeGraph 同步后确认 `TerminalLandDocument` 影响 57 个符号、`StandardWidgets` 影响整个 UI2 控件输入面，因此除地产/市场定向测试外执行 UI2 与根项目全量矩阵。
- 自动验证共 504 项：根项目 458 项（52 项既有环境条件跳过）、UI2 Core 40 项、Lab 2 项、Demo 4 项，0 失败；`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过。runtime JAR SHA-256 为 `118d83ed4efe5647db1d36c7508fe063e0f1daf797b1a51f9c34a004261e295a`。
- 同一修复 JAR 已部署 Lobby 与 Prism 客户端，两处目标哈希一致；Lobby 重启后为 `RUNNING`，日志达到 `Done (1.545s)`。客户端未启动，S1/S2 未触碰，数据库没有业务 migration。

# 2026-09-07 - UI2 Modal 失效边界与市场图表渲染修复

- 实机确认首页帮助层虽然消费了 Escape/点击，却未必显式请求 Runtime 重建，旧 Modal 因而持续缓存并拦截 R、返回和关闭。现将“被消费的 UI 输入”定义为 UI2 Runtime 的统一失效边界；关闭状态在下一帧自动重建，普通 Screen 与 Container 共用此行为，不再依赖每个页面手工调用 `invalidate()`。
- 标准市场图表此前在 `RenderItem` 之后直接使用 Minecraft 固定管线绘制，可能继承物品渲染留下的颜色、光照和混合状态，实机表现为整块灰色。现调整为先绘制页面外部图表、后绘制真实物品，并用独立 `GlStateGuard` 隔离两段状态。
- 详情图增加明确周期标题；真实成交不足时保留网格和参考价格线，并显示“暂无成交数据 · 参考价”，避免无成交商品呈现为疑似故障的空白块。服务端行情、撮合和数据库合同未改。
- CodeGraph 同步后复核 `UiRuntime`（71 个受影响符号）和 `MarketChartRenderer`（117 个受影响符号），测试范围覆盖 UI2 Runtime、首页 Modal、标准市场文档与全部终端/业务回归。
- 自动验证共 504 项（52 项既有环境条件跳过），0 失败；`:ui2-core:test`、`:ui2-lab:test`、`:ui2-demo:test`、Golden 导出、根项目 `test`、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 全部通过。runtime JAR SHA-256 为 `1b7dea024df84fe5527d574617c68d01f28949d52a9c7fe71d1ae4794142295e`。
- 同一 JAR 已部署到 Lobby 与 Prism 客户端，构建产物及两处目标哈希一致；Lobby 由部署器完成重启和启动日志健康检查，客户端未启动。部署器仅幂等确认既有 migration 历史表；S1/S2 未触碰。
- 二次实机反馈确认前述隔离只恢复了宿主状态，却没有为外部二维画布建立确定状态；图表实际被恢复后的深度测试遮挡，只露出底层 Card。现于图表阶段显式关闭深度测试/写入与光照、启用标准 Alpha 混合并重置颜色。定向测试、`assemble`、自包含检查与差异检查通过；重新部署后的构建/Lobby/客户端 SHA-256 均为 `581597c3e38afdda0874b6287ad7a44675ec32c4529cf9691055588782a90459`。

# 2026-09-07 - Victoria 3 经济 Mod 源码研究基线

- 通过 GitHub SSH 将 Anbeeld's Stockpile Economy 与 Economic and Financial Mod 拉取到仓库外的 `reference-sources/victoria3-economy/`；固定提交分别为 `c7a7e0cd5c1646b9a449854d0cefb27a7ef8ea9b` 与 `48c3f7014dec0dab86899cca64fce1c06a10e4a1`，不会进入 Base 构建、JAR 或部署。
- 新增 `docs/victoria3-economy-mod-research-2026-09-07.md`，记录 ASE 的目标库存、双阈值滞回、扣除自身订单、批处理和限幅设计，以及 E&F 的货币、银行、利率、债券、股票、通胀、流动性、信用与危机系统。
- 许可证边界：ASE 根目录为 MIT；E&F 当前提交和历史检查未发现许可证文件，因此只研究思想与行为，不复制其代码、数据、GUI 或美术资源。
- 初步建议先执行只读经济事实盘点与历史数据离线计算，再做经济观测页和公共储备回放；未经单独批准不修改市场、银行、Vault、数据库或生产服务器。
# 2026-09-08 - Galaxy UI2 正式终端纯 Java视觉闭环 V.1/V.2 与 V.3 基础

- 新增独立 Java 8 `ui2-terminal` 子模块，依赖方向固定为 `ui2-terminal -> ui2-core`；根 Mod 与 Java2D Demo 均消费同一终端壳和正式 Document。纯模块新增 `TerminalVisualModel`、`VisualItem`、`ExternalVisualRegion`、动作端口、窗口度量与确定性场景，不引用 Minecraft、Forge、LWJGL 或 AE2。
- 首页、设置和资产中心已下沉为平台无关 Document；根 Mod 只负责把现有服务端快照、客户端偏好、`ItemStack`、光标栈及操作动作转换到纯模型。资产中心仍复用原 Container、Vault、背包、Bay、Cell、幂等和版本合同。
- Java2D 报告已从 15 张 UI Lab 扩展到 63 张正式终端截图，覆盖三套主题和 `350×193 / 427×240 / 620×340`。视觉门禁实际发现并修复了资产内容区整体 0 高度、市场商品价格行 0 高度以及物品数量文字行高不足三类布局缺陷，并新增“挂载内容塌缩为 0”失败规则。
- V.3 已建立纯标准市场目录/详情模型和平台无关 `CHART_LINE / CHART_VOLUME` DrawCommand；Java2D 与 Minecraft 渲染后端均支持同一命令。正常、稀疏、单点和空行情场景进入报告，有数据却无图形及空态缺失会使 `verifyTerminalVisuals` 失败。正式下单/撤单/订单中心适配尚未迁移，因此 V.3 保持实施中。
- `scripts/render-ui2-visuals.sh` 现在执行 Core、Terminal、Lab、Demo 测试与 `:ui2-demo:verifyTerminalVisuals`，统一报告仍为 `ui2-demo/build/ui-lab/index.html`。本记录写入时定向视觉矩阵通过：63 张正式终端快照及 15 张 Lab 快照，0 个结构告警。
- 自动验收：根项目 458 项（52 项既有条件跳过）、UI2 Core 41 项、Terminal 1 项、Lab 2 项、Demo 5 项，全部 0 failure/0 error；`test`、`assemble`、`verifyUi2SelfContained`、`verifyTerminalVisuals` 与 `git diff --check` 均通过。运行 JAR SHA-256 为 `9062bbe4d41e4a72a952b569e89e5e512134d948c769a4063cd37ea3c5e6213a`。按阶段约束本轮未部署，未启动客户端，未触碰 Lobby/S1/S2 或数据库。

# 2026-09-08 - 跨服任务平台 Q.2 玩家进度结构化导入

- `galaxy-quest-bq-importer` 将 BQ 玩家进度从不透明 Task JSON 提升为结构化 Task 进度：稳定 task key、类型、
  当前玩家完成标记和数值列表；兼容现网 `data` 整数数组与击杀任务 `value` 标量，同时继续保留完整原始 JSON。
- 增加 9 种现网 Task 与 4 种 Reward 的固定兼容矩阵测试，以及只读 `progressInventory` 工具。S2 实跑读取
  29 个玩家文件、108557 个任务进度、4524 个已完成、1279 个已领取；所有类型均在兼容矩阵内。
- S2 DefaultQuests 再次全量导入 3744 个任务，unknown Task/Reward 集合均为空。现场文件只读挂载，无写回、
  无数据库 migration、无部署；S1 未操作。
- 移除定义导入阶段错误的统一 `target=1` 占位，改为任务类型对应的规范参数；检索、合成、流体和方块破坏
  使用逐项向量目标，击杀、位置、会面、选择奖励与 XP 保留源码中的关键语义。兼容报告现明确输出
  `ADAPTED/UNKNOWN`，不会把“能导入”误报成“运行时已完整支持”。
- `galaxy-quest-core` 的 `TaskProgress` 已升级为向量兼容模型，旧标量构造器保持可用；逐项进度单调合并、
  全项完成判断及形状冲突失败均有测试。新增 `BqProgressMapper` 将旧进度按定义形状映射到核心模型，拒绝
  task 类型、数量或向量宽度漂移，避免跨服迁移静默损坏。
- CodeGraph 复核 `TaskProgress` 影响 43 个符号、`BqStandardCodec` 影响 35 个符号、`BqProgressMapper`
  影响 7 个符号；导入器定向测试、Core 测试、根项目全量 `test`、`assemble` 与 `git diff --check` 通过。
  新构建 JAR SHA-256 为 `bf085903b7a0967fcae9645b352868d5de87030dc625ec1a8680d0089e53fe6c`；本轮未部署。

# 2026-09-09 - 跨服任务平台 Q.3 PostgreSQL 权威运行时第一批

- 新增纯 Java 8 `galaxy-quest-postgres` 模块。第一版 PostgreSQL DDL 包含版本化任务定义、唯一事实收件箱、
  PLAYER/PARTY/TEAM/PUBLIC 进度、BIGINT 数组 Task 进度和唯一奖励资格；尚未接入生产 migration。
- 新增 JDBC 连接/事务边界和 `JdbcQuestRuntimeRepository`。事实、进度和奖励资格变更强制要求活动事务；
  进度读取使用参与主体+任务版本的事务 advisory lock，覆盖“记录尚不存在”时的并发首次更新。
- 核心新增 `QuestRuntimeService`，把事实去重、进度锁定、事实投影、任务归约和奖励资格生成固定在一个事务
  工作流中，重复事实不会再次运行任何任务。
- 使用独立临时 PostgreSQL 数据库和角色完成 8 项仓储/事务测试，其中 4 项为真库测试：相同事实并发、
  不同事实并发首次更新、向量进度往返、奖励资格幂等及强制故障整体回滚全部通过。临时数据库和角色已删除；
  现有数据库用户、业务 schema 和数据未修改。
- CodeGraph 复核 `QuestRuntimeService` 影响 19 个符号、`JdbcQuestRuntimeRepository` 影响 28 个、
  `JdbcQuestTransaction` 影响 17 个。Quest Core、PostgreSQL 模块、根项目全量 `test`、`assemble` 和
  `git diff --check` 均通过；新 JAR SHA-256 为
  `49cae7ad0108e326a2e5c0818154f7294b722c01236175c31e3a68d2aaa5245e`，未部署。

# 2026-09-09 - 跨服任务平台 Q.3 定义发布、奖励恢复与双源冲突预演

- 新增任务定义草稿、内容哈希乐观锁、发布和退役仓储；发布版本不可原地修改，运行时只读取最新发布版本。
- 奖励资格增加租约、尝试和恢复状态机：多工作器通过 `FOR UPDATE SKIP LOCKED` 领取，失败可延迟重试，
  达到上限进入 `ABANDONED`，租约过期可被其他工作器接管，旧工作器确认会被拒绝。外部交付端合同强制
  使用稳定 entitlement key 实现幂等。
- 新增纯 Java 工作器测试，覆盖成功确认、稳定外部键、可重试失败、最大次数放弃和陈旧确认；一次性真
  PostgreSQL 库覆盖草稿冲突、发布不可变、失败重试、租约接管和尝试审计。测试结束后临时库与角色已删除，
  生产数据库和 migration 未修改。
- 新增 BetterQuesting 双源只读冲突分析工具，按单调偏序区分一致、单边存在、左/右领先和真正分叉；向量
  交叉领先、Task 类型或形状漂移不会被静默合并。因 S1 操作需单独授权，本轮未读取 S1 现场数据。
- CodeGraph 前置同步并复核 `JdbcQuestDefinitionRepository`、`JdbcRewardDeliveryRepository` 与
  `RewardDeliveryWorker` 的影响链，改动保持在 quest-core、quest-postgres 和 quest-bq-importer 新模块内。
  三个模块共 36 项测试通过，根项目全量 `test`、`assemble` 与 `git diff --check` 通过；runtime JAR
  SHA-256 为 `42c636444185f0779aa1a5710cfdabf1254b36894d917a559ae58dc4a42121a4`。尚未接入 Mod 启动、
  未部署、未触碰 Lobby/S1/S2。

# 2026-09-09 - 跨服任务平台 Q.4 第一批事件检测器

- 对照 BetterQuesting `EventHandler`、`TaskHunt`、`TaskCrafting` 和 `TaskCheckbox`，采用其成熟的事件/周期
  检测分工，但将调用方向改为 Forge 事件生成不可变事实，禁止事件处理器遍历任务或直接修改进度。
- 新增击杀、合成、熔炼、铁砧事实工厂与服务器事件处理器，过滤客户端世界、FakePlayer 和取消事件；保留
  BQ 对 Shift-click 合成空栈重新求配方数量以及熔炼异常数量的兼容修正。每次服务进程使用启动 ID 加单调
  序号生成唯一事件键，重试继续携带同一个不可变事实。
- 纯核心新增击杀父类型/伤害源求值、合成物品向量求值和服务端绑定复选确认求值；导入的 hunt、crafting、
  checkbox 定义获得稳定 `galaxy:*` fact type。注册名、meta、通配 meta、OreDict 以及三种生产通道均有
  明确合同；NBT 字段已保留但部分匹配尚未宣告完成。
- 当前 Handler 尚未注册，避免在发布定义目录、参与主体解析和生产 DDL 未接线前丢弃事实；没有生产
  migration、部署或服务器操作。CodeGraph 复核 `ItemVectorTaskEvaluator`（21 个影响符号）与
  `QuestGameplayEventHandler`（12 个影响符号）；Quest 模块当前 40 项测试为 0 failure/0 error，其中默认
  环境下 7 项真 PostgreSQL 测试按连接条件跳过（本轮前一批已用一次性真库通过）。根项目 `test`、
  `assemble` 和 `git diff --check` 通过，runtime JAR SHA-256 为
  `6c7978dd7245d44d04b535a5a65e4fbca9249bcf98bf7a77fbddf00e555d8b86`。

# 2026-09-09 - 跨服任务平台 Q.4 发布计划、方块与库存快照

- 定义仓储新增最新发布版本目录，进度查询新增参与主体的已完成前置集合；`QuestEvaluationPlanner` 在
  `QuestRuntimeService` 的同一事务内生成执行计划，先按 fact type 剔除无关任务，再通过显式 Assignment
  Policy 选择 PLAYER/PARTY/TEAM/PUBLIC 范围。当前导入任务默认 PLAYER-only。
- 无相关任务的事件现在返回 `IGNORED` 且不写事实表；定义、成员和前置状态不会在事务外形成陈旧快照。
- 新增方块破坏向量求值和事实采集，保持 BQ“一次破坏只命中第一个需求”的算法，支持直接方块、meta
  通配和 OreDict。
- 新增非消费型库存权威快照：未完成的观察进度允许回落，完成后锁定；同一栈按需求顺序分配，禁止重复
  计数。`consume=true` 的检索不会走快照捷径，后续使用事务提交协议。
- 使用一次性 PostgreSQL 数据库验证最新发布目录和完成前置查询，全部测试通过后临时库与角色已删除；
  生产数据库、migration、Mod 注册和部署均未变更。CodeGraph 复核 `QuestEvaluationPlanner`（15 个影响
  符号）、`InventorySnapshotTaskEvaluator`（20 个）与 `MinecraftQuestFactFactory`（24 个）；Quest 三模块
  共 47 项测试全部执行且 0 failure/0 error/0 skipped，根项目 `test`、`assemble` 与 `git diff --check`
  通过。runtime JAR SHA-256 为 `cb1587546e75ac3a3e289af48ed932795758d465a62653ed2318425b9a06c1d5`。

# 2026-09-09 - 跨服任务平台 Q.4 流体与地点权威观察

- 新增非消费型流体库存快照求值器及 Minecraft 容器扫描，按毫桶和需求顺序分配，避免同一容器内容被多个
  条件重复计数；`consume=true` 明确保持不推进，等待事务型扣除协议。
- 新增地点求值器和每 100 tick 的基础位置事实，对齐 BQ 的维度、三维欧氏/曼哈顿距离、范围、生物群系与
  invert 语义。结构与目标可见性必须携带显式服务端证据；缺失证据按 unknown 处理，不会误判为 false，
  因而 inverted 任务也不会误完成。
- BQ 导入器为 fluid 与 location 写入稳定 fact type；兼容注册表现在覆盖现网 9 类 Task 中的 8 类，尚缺
  meeting。消费型 retrieval/fluid、部分 NBT、结构/视线探针和奖励交付仍保持待实现状态。
- Handler 仍未注册，生产数据库、migration、Mod 生命周期和服务器均未修改。CodeGraph 同步覆盖流体、地点、
  Fact Factory 和 Handler 调用链；Quest 三模块共 49 项测试，0 failure/0 error，其中默认环境下 7 项真库
  测试按连接条件跳过。根项目全量 `test`、`assemble` 与 `git diff --check` 通过，runtime JAR SHA-256 为
  `f9b95d51d842a579b86e5d175e2651c6e88c1acbb02cf504864eb39fcd360d27`。本批未部署。

# 2026-09-09 - 跨服任务平台 Q.4 会面检测与周期探针计划

- 新增 meeting 求值器，支持精确实体、子类型别名、所需数量与可选 NBT；附近实体事实携带完整扫描半径，
  小半径快照无法完成大范围任务，避免把未观察区域错误当成空区域。
- 新增 `QuestObservationPlan`，从发布定义汇总库存、流体、地点和 meeting 的周期观察需求，并取全部会面任务
  的最大半径。Handler 按 BQ 的 20/60/100 tick 周期只生成计划要求的事实，不再无条件扫描和写库。
- BQ 导入器现为当前任务库全部 9 种 Task 写入稳定 fact type，核心注册表也覆盖全部 9 类。这里的“覆盖”
  不等于已经生产启用：消费型任务、BQ 部分 NBT、地点结构/视线探针和奖励交付仍需完成。
- 事件 Handler 依旧未注册，生产 migration、数据库和服务器均未操作。CodeGraph 复核 meeting、周期计划、
  Fact Factory 与 Handler 影响链；Quest 三模块共 51 项测试，0 failure/0 error，默认环境下 7 项真库测试按
  连接条件跳过。根项目 `test`、`assemble` 与 `git diff --check` 通过；runtime JAR SHA-256 为
  `455e37c673a79427940ca2e1f0f892252b2a2c1516f623814d37e35ffaa1ab85`。本轮未部署。

# 2026-09-09 - 跨服任务平台 Q.4 消费型任务 Saga 第一批

- 对照 BQ retrieval/fluid 的检测、手工提交与直接扣除逻辑，新增跨服安全的消费协议：稳定提交键、
  `PREPARED/APPLIED/CONFIRMED/REJECTED` 状态、幂等外部扣除端和确认事实。只有外部扣除返回持久证据后，
  consuming retrieval/fluid 才按逐项增量推进；非消费快照与消费确认通过组合 evaluator 共存。
- 消费事实绑定玩家、Quest UUID、定义版本、Task key 和 item/fluid 种类；Planner 精确验证这些边界，无法
  匹配发布任务时不会把已扣资源静默确认为成功。周期计划也不再为 consume=true 的任务生成无意义快照。
- PostgreSQL DDL 新增消费提交表与恢复索引，JDBC 仓储拒绝同一提交键对应不同内容，并校验合法状态迁移。
  使用一次性独立数据库/角色执行全部真 PostgreSQL 集成测试，覆盖准备幂等、应用、确认、重复确认与内容
  碰撞；测试结束后临时数据库和角色均已删除，现有业务 schema 未修改。
- 尚未实现 Minecraft 玩家 NBT 的幂等扣除端，因此 Handler/消费入口仍不注册；无生产 migration、部署或
  服务器改动。
- 后续补齐服务端规范请求授权器：外部意图不含资源或数量，授权器从最新发布定义、锁定玩家进度、前置任务
  和重复周期生成规范请求；锁定/完成、非消费、错误 Quest/Task 均拒绝。任意请求直提入口已移除，恢复只能
  按数据库既有提交键执行。
- CodeGraph 复核 `ConsumptionSubmissionService`、`ConsumptionRequestAuthorizer`、Planner 与 JDBC 提交仓储
  影响链。Quest 三模块默认矩阵共 56 项，0 failure/0 error，其中 8 项真库测试在默认无连接环境下跳过；
  另使用一次性数据库实际执行全部 8 项 PostgreSQL 集成测试并通过，临时库/角色已删除。根项目 `test`、
  `assemble`、`git diff --check` 通过，runtime JAR SHA-256 为
  `c86229a97680bbf927312dcfdb4cd50597aa9aa60ec158640d6892729610b532`。未部署。

# 2026-09-10 - 跨服任务平台 Q.4 玩家物品幂等消费端

- 新增 Minecraft 服务端物品消费适配：只按服务端 UUID 查找在线玩家，客户端仍不能提交物品定义、数量或
  目标玩家。扣除前先为所有需求生成完整分配计划，同一物品栈不会重复满足多个需求；任何需求不足时背包
  保持不变。
- 物品匹配覆盖注册名、耐久/通配 meta、OreDict，并按 BetterQuesting MIT 源码语义重写完整/部分 NBT
  比较，包括嵌套 Compound、无序 List、跨整数类型数值和带重复控制的 byte/int 数组。
- 每个 submission key 以 SHA-256 标记写入玩家 persisted NBT；背包变更与标记经世界玩家数据接口同步写盘。
  重试命中标记时直接返回既有成功证据，持久化抛错则恢复完整背包快照并撤销标记，Saga 不会进入 APPLIED。
- 新增可注入的在线玩家解析与玩家数据 flush 边界，以及默认服务器实现；未注册 Handler、消费入口或 Mod
  生命周期。本批尚不处理 fluid 容器消费，生产 migration、数据库和服务器均未改动，也未部署。
- CodeGraph 前置复核 `ConsumptionPort`（27 个影响符号）与 `ConsumptionSubmissionService`（43 个）；
  Minecraft 适配编译通过，新增 NBT 语义测试及根项目测试通过。全量测试中的 `ui2-demo` 两项 Java2D 导出
  因容器字体环境失败，排除该已知视觉环境项后根 `:test` 通过；该失败与本批 Quest 代码无调用关系。
  `assemble` 与 `git diff --check` 通过，runtime JAR SHA-256 为
  `a0997dbfb8f81278f4425ef5fd79df043f9f72d23af0a208514ab987129fa1cf`。

# 2026-09-10 - 跨服任务平台 Q.4 流体容器幂等消费端

- `MinecraftPlayerConsumptionPort` 现同时支持 item 与 fluid。流体需求在完整背包副本上依次分配，兼容 Forge
  固定流体容器和 `IFluidContainerItem` 可变容器，并按流体注册名及可选 NBT 匹配；部分排空容器和空容器
  会进入规划后的背包状态，输入背包在规划阶段保持不变。
- 满背包无法接纳返回容器时整次提交拒绝且不扣流体。此处有意不采用 BQ 的掉落空容器行为，因为世界实体
  无法与玩家 persisted NBT 的幂等标记一起回滚；玩家整理出空间后可以使用新的 submission key 重试。
- Forge 流体操作下沉到 `QuestFluidContainerAdapter`，生产实现调用 Forge 1.7.10，测试使用确定性 fake，
  解决无完整 Minecraft Bootstrap 时 `FluidRegistry` 不能初始化的问题。新增测试覆盖 1500 mB 跨容器部分
  排空、返回容器数量守恒、输入不变以及满背包拒绝。
- 根 `:test`（排除既有 `ui2-demo` 字体环境项）通过：464 项、52 项条件跳过、0 失败。Handler、生产
  migration、数据库和服务器未改动，未部署。`assemble` 与 `git diff --check` 通过，runtime JAR SHA-256
  为 `d4324f1202bca016dc13cfe7df20edd9ce03bdc1bd6cc988bd94d2838733fd43`。

# 2026-09-10 - 跨服任务平台 Q.3 奖励类型路由与选择门禁

- 新增纯 Java `RewardDeliveryHandler/RewardDeliveryRouter`：按完整 Reward type ID 精确路由，重复 handler
  注册立即失败，未知类型返回不可重试失败，防止 worker 把尚未支持的奖励静默标记为已领取。
- 新增 PostgreSQL 选择奖励表与 `JdbcRewardChoiceSelectionRepository`。选择只接受服务端会话提供的当前玩家
  UUID，校验 entitlement 所有权、`bq_standard:choice` 类型、可交付状态和 `item.count` 索引范围；首次选择
  不可变，相同选择幂等，不同选择报告冲突。
- Delivery lease 查询会跳过尚未选择的 choice entitlement；选择后才允许租赁，并把数据库中的选择索引作为
  `choice.index` 注入不可变 Reward 参数。客户端提交的 Reward 参数不会进入该链路。
- 使用一次性数据库/角色 `jgb_quest_choice_it` 实际执行全部 PostgreSQL 集成测试，覆盖未选择不可租赁、跨玩家
  拒绝、越界拒绝、同值幂等、改选冲突及租约参数；测试通过后数据库和角色均删除并核验不存在。生产 schema、
  migration、服务器和 Handler 未修改，未部署。Quest Core 36 项、Importer 11 项、PostgreSQL 13 项真库测试
  及根项目 464 项（52 项既有条件跳过）均为 0 失败；`assemble`、`git diff --check` 通过。runtime JAR
  SHA-256 为 `47968f4089b090e234cdc303c7602cb2f1dd0d97a7b638f9910df1be7e6dd367`。

# 2026-09-10 - 跨服任务平台 Q.4 物品、选择与经验奖励交付器

- 对照 BetterQuesting `RewardItem/RewardChoice/RewardXP/XPHelper/NBTReplaceUtil` 源码，实现 PLAYER 范围的
  `bq_standard:item`、`bq_standard:choice` 和 `bq_standard:xp` handler。选择索引只取数据库租约注入值；
  物品 NBT 支持递归 `VAR_NAME/VAR_UUID` 替换；XP 等级曲线与 BQ 的三个区间保持一致。
- 交付前在背包副本中完成全部堆叠规划。背包不足返回可重试失败，不像 BQ 那样生成世界掉落，从而保证
  entitlement 标记、背包或经验变化可由同一次玩家数据持久化覆盖；保存异常恢复玩家状态和标记。
- CodeGraph 前置复核 `RewardDeliveryRouter`（10 个影响符号）及 `MinecraftPlayerConsumptionPort`（42 个）；
  改动后同步新增 5 个文件、修改 1 个文件。新增确定性测试覆盖堆叠、满背包全量拒绝、NBT 替换、XP 边界与
  标记稳定性，连同消费端定向测试共 10 项通过。Quest Core 36 项、Importer 11 项、PostgreSQL 13 项（默认
  环境 9 项真库条件跳过）及根项目 469 项（52 项条件跳过）均为 0 失败；`assemble`、
  `verifyUi2SelfContained` 与 `git diff --check` 通过，runtime JAR SHA-256 为
  `a54b034adcd9feb9434b9dc1931ed65120443074edc8547f3fb8620633f1a0d1`。运行时注册、生产 migration、服务器
  和数据库均未修改，本批不部署；quest-completion 与非 PLAYER 奖励策略留待下一批。

# 2026-09-10 - 跨服任务平台 Q.4 任务完成奖励事务化

- 对照 BetterQuesting `RewardQuestCompletion`，新增纯核心 `QuestCompletionRewardPort` 和对应 delivery
  handler；目标 UUID 只取已发布 Reward 定义参数，非法 UUID 与未发布目标为永久失败。
- PostgreSQL 实现把目标任务进度强制完成和目标奖励资格创建置于同一事务。目标已完成作为幂等成功；目标
  Task 各分量写至定义目标值，使进度快照与完成状态一致。该语义保留 BQ 不检查目标前置的行为，同时利用
  参与主体进度锁、稳定资格键和通用租约恢复消除跨服重复完成/重复发奖。
- CodeGraph 复核 `QuestRuntimeService`（36 个影响符号）、新 handler（12 个）和 JDBC port（11 个）。Core
  39 项测试通过；使用一次性数据库/角色实际执行 PostgreSQL 14 项测试，0 failure/0 error/0 skipped，验证
  完成时间不被重试覆盖、Task 完成及奖励只产生一次。测试库和角色随后删除并核验均为 0。生产 schema、
  Mod 生命周期和服务器未修改，本批不部署。

# 2026-09-10 - 跨服任务平台 Q.4 扩展到完整 BQ Standard 重要类型

- 源码复核发现，现网 9 Task/4 Reward 之外，BQ Standard 还提供实体交互、物品/方块交互、计分板、XP Task，
  以及命令、计分板 Reward。导入兼容集合扩展为 13 Task/6 Reward，避免未来内容或编辑器创建这些类型时被
  错误归为 unknown。
- 新增稳定 fact type 与参数归一化：交互任务保留次数和事件类型；计分板把可能为负的比较阈值单独保存为
  `scoreTarget`，TaskProgress 使用布尔完成目标，避免破坏非负进度不变量；XP 等级阈值按 BQ 曲线转为经验点。
  所有原始配置继续保存在 `bq.raw`，当前未实现的运行适配仍明确为 ADAPTED。
- CodeGraph 复核 `BqStandardCodec`（35 个影响符号）。Importer 12、Core 39、PostgreSQL 14 项以及根项目
  469 项（52 项条件跳过）均 0 失败；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。runtime
  JAR SHA-256 为 `8d6ee70d5bc254b976e2a0072d4b2f25f5cc225ae31279c703c71aeebe022086`。未部署。

# 2026-09-10 - 跨服任务平台 Q.4 计分板、XP 与交互检测

- 新增计分板和非消费 XP evaluator：计分板支持 BQ 六种比较操作和负阈值，完成后锁定；XP 使用绝对经验点
  快照，未完成值允许下降，消费型 XP 不走观察捷径。
- `QuestObservationPlan` 增加发布定义感知的 XP 与计分板目标；Minecraft Handler 按 BQ 的 20/60 tick 周期
  采集服务端权威值。缺失 objective 按定义 criteria 创建，避免每个 Task 自行扫描。
- 新增方块左/右键和实体攻击/交互事实，基础求值覆盖动作、实体父类型、手持物、方块、meta 与 OreDict。
  有 NBT 限制的交互目前 fail-closed，等待 canonical NBT 比较，绝不误推进。Handler 仍未注册。
- CodeGraph 影响范围：`QuestObservationPlan` 29、`InteractionTaskEvaluator` 36、
  `QuestGameplayEventHandler` 19 个符号。Core 43、Importer 12、PostgreSQL 14 项（默认环境 10 项条件跳过）
  以及根项目 469 项（52 项条件跳过）均 0 失败；`assemble`、`verifyUi2SelfContained`、`git diff --check`
  通过。runtime JAR SHA-256 为 `b7966f336cd12662fb9c5efdcaa81318d5eceeafd87acf8c783af88749496cfc`。
  未部署，也未修改生产配置或数据库。

# 2026-09-10 - 跨服任务平台 Q.4 跨模块 canonical NBT 匹配

- 新增纯 Java `PortableNbt/PortableNbtMatcher`：确定性编码保留 Compound、List、String、Number、byte[] 与
  int[] 类型，Compound 键排序，数值跨原 NBT 宽度归一；部分 Compound 和无序集合匹配均禁止一个实际元素
  被重复使用，畸形编码 fail-closed。
- BQ Importer 将带类型后缀的 NBT-JSON 同时写入 `*.nbtPortable`，Minecraft 事实工厂则从真实 `NBTBase`
  生成相同编码；原始字段继续保留用于奖励重建与审计。物品生产、库存、流体、方块、击杀、会面和实体/
  方块交互求值现共用该合同，hunt 和 interact_entity 的目标实体 NBT 采用 BQ 的部分匹配语义。
- CodeGraph 前置同步后复核 `PortableNbt`（49 个影响符号）、`InteractionTaskEvaluator`（36 个）；新增核心、
  导入和 Minecraft 适配测试。Quest Core 与 Importer 共 60 项、根项目 471 项（52 项条件跳过）均 0 失败；
  `compileJava` 通过。未挂载开发字体的普通聚合 `test` 会在既有两项 Java2D 导出测试失败；使用正式视觉脚本
  只读挂载 CJK 开发字体后，UI2 四模块测试及 63 个正式终端视觉快照全部通过。`assemble`、
  `verifyUi2SelfContained` 和 `git diff --check` 通过，runtime JAR SHA-256 为
  `397310493f01bf5731ff3be65fe455f8a411dc7fc4b37085fda4a71f7681ea5b`。未注册 Handler、未部署，生产数据库
  和 migration 未修改。

# 2026-09-10 - 跨服任务平台 Q.4 消费型 XP 与实际扣除向量

- 对照 BQ `TaskXP.detect`，修正原 Saga 只能表达“全额请求/全额确认”的限制。外部扣除结果、Submission 和
  PostgreSQL 现持久化本次实际扣除向量；确认事实使用实际量而非请求上限。物品/流体继续全额原子扣除，XP
  可以按玩家当前经验部分扣除并在后续新提交中继续推进。
- Minecraft 玩家 NBT 的 SHA-256 submission marker 从布尔值升级为实际向量 Compound；读取旧布尔 marker
  时仍按旧全额语义兼容。扣 XP、写 marker 和玩家数据 flush 共享回滚边界，保存失败恢复经验和 marker；
  成功后同步经验条。消费型 XP 已加入授权器和组合求值器，非消费型仍使用周期快照。
- Quest DDL 新增 `applied_values BIGINT[]`，资源类型允许 `xp`；JDBC 在 APPLIED 转换前校验向量宽度、非负、
  不超过请求量。使用一次性 `jgb_quest_xp_it` 数据库与角色实际执行 PostgreSQL 14 项测试，0 skipped/0
  failure，随后删除并核验数据库/角色计数均为 0。CodeGraph 复核 `ConsumptionSubmissionService`（20 个影响
  符号）和 `MinecraftPlayerConsumptionPort`（43 个）；Quest 三模块 75 项、根项目 472 项（52 项条件跳过）、
  63 个终端视觉快照均 0 失败。`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过，runtime JAR
  SHA-256 为 `e0a0fe8bda149327bd71235611a09979de12c4ecfcbc85e00639ba5c95fdd5b5`。生产 schema、Handler 注册、
  服务器和部署均未修改。

# 2026-09-10 - 跨服任务平台 Q.4 计分板与安全命令奖励

- 对照 BQ `RewardScoreboard`，实现 PLAYER 计分板奖励。相对值先转换为 int 饱和的绝对目标并持久化到
  entitlement marker，之后只执行幂等 set；崩溃重试不会把相对奖励重复相加。缺失 objective 按 criteria
  创建并回退 dummy，只读 objective 永久拒绝。
- 对照 BQ `RewardCommand` 保留变量替换和 `viaPlayer`，但把任意命令执行替换为纯核心白名单与
  `CommandRewardExecutor` 合同。执行端必须按 entitlement key 持久去重；默认空白名单，控制字符、超长和
  未允许根命令不会进入执行端。扫描当前 GTNH 默认任务库未发现 command reward，因此该安全默认不改变现有
  内容。没有复制 BQ 网络、调度器或管理员 CommandSender。
- 新增核心策略/handler 测试与 Minecraft 计分目标计算测试；Handler 仍未注册，生产配置、数据库和服务器
  均未修改。Quest Core 51、Importer 12、PostgreSQL 14、根项目 473 项测试均为 0 失败（条件测试按环境跳过）；
  63 个终端视觉快照、`assemble`、`verifyUi2SelfContained` 和 `git diff --check` 通过。runtime JAR SHA-256
  为 `d76ccb3b86d8c5d1d9c4e271fe1179a866cd17bc195f0b19f1207965cd2be83d`，本批未部署。

# 2026-09-10 - 跨服任务平台 Q.6 编辑领域与章节迁移基础

- 对照 BetterQuesting `GuiQuestEditor`、`QuestLine` 与 `QuestLineEntry`，开始吸收“编辑任务内容和编排章节”
  的领域能力，但不复制 GUI、Packet、全局数据库单例或 NBT 文件写入。新增纯 Java `QuestDraftEditor`，支持
  基本字段、前置关系、Task/Reward 的增删、替换和稳定排序；所有操作输出新不可变定义，发布版本不原地改写。
- 新增版本化 `QuestChapterDefinition/QuestChapterEntry` 与章节草稿编辑器；章节只保存展示编排，不承载玩家
  进度。PostgreSQL 新增章节定义表和 JDBC 仓储，复用 DRAFT/PUBLISHED/RETIRED、内容哈希、事务写入及乐观锁，
  为多个管理端跨服编辑提供冲突保护。
- BQ 导入器新增 split QuestLines 只读解析：读取 `QuestLinesOrder.txt` 的章节顺序/名称、`QuestLine.json` 的
  说明/图标/背景，以及位置文件的 UUID、坐标和尺寸；URL-safe Base64 目录后缀按完整 16 字节 UUID 验证，
  避免连字符被误判为名称分隔符。对当前客户端只读实跑通过：3739 个 Quest、46 个章节、3842 个位置，Task/
  Reward 均无未知类型。
- CodeGraph 同步 14 个新增文件；影响范围为 `QuestDraftEditor` 44、`QuestChapterDefinition` 76、
  `JdbcQuestChapterRepository` 17、`BqQuestChapterImporter` 28 个符号。Core 56、Importer 15、PostgreSQL 15、
  根项目 473 项测试均 0 失败（PostgreSQL 10、根项目 52 项按环境跳过）；63 个终端视觉快照、`assemble`、
  `verifyUi2SelfContained` 和 `git diff --check` 通过。runtime JAR SHA-256 为
  `fff4604bdd8d8e6fbe90fab078d4e8fac1220b8101148f7d601c03923fa17485`。生产 migration、Handler 注册、服务器
  和数据库均未修改，本批不部署。

# 2026-09-10 - 跨服任务平台 Q.4/Q.6 行为、领取与编辑类型合同

- 导入并内部化 BQ Quest 行为：七类可见性、图标引用、main/silent、自动领取、锁定进度、simultaneous、
  global/globalShare、音效和相对/固定重复周期。纯核心运行时默认拒绝锁定任务进度，显式属性才允许推进；
  simultaneous 在未同时满足时清除局部结果，可见性求值不依赖 BQ 客户端单例。
- 奖励资格新增 `CLAIMABLE`。普通完成等待玩家本人领取，auto-claim 才直接 `PENDING`；手动 Choice 必须先由
  当前认证玩家做不可变选择，再经领取事务转入投递队列。领取按玩家、Quest 版本和周期加锁，区分未找到、
  非本人、缺少选择、已领取和非法状态；DDL 增加 `claimed_at`，但未向生产数据库执行。
- 新增纯 Java 编辑描述层：`QuestEditorTypeRegistry`、Task/Reward 元素描述、字段类型和结构化参数问题。
  BQ 适配目录完整覆盖 13 种 Standard Task 与 6 种 Reward，能驱动布尔、数字、枚举、UUID、物品/流体/
  方块列表、实体、NBT 与命令控件，并逐项拒绝空身份、非整数和非正数量；不引入 BQ GUI 或网络。
- CodeGraph 复核 `RewardEntitlement`（49 个影响符号）、`QuestBehavior`（90）、
  `QuestEditorTypeRegistry`（25）和 `BqStandardEditorTypeRegistry`（19）。Quest 三模块定向测试通过；另用
  一次性数据库/角色实际执行 PostgreSQL 集成测试，覆盖手动奖励及 Choice 的“选择-领取-租约”完整链，随后
  删除并核验数据库与角色计数均为 0。当前客户端 DefaultQuests 只读复跑为 3739 Quest、46 章节、3842
  位置，未知 Task/Reward 均为空。生产注册、migration、服务器和部署均未修改。
- 最终自动门禁：Core 63、Importer 17、PostgreSQL 19 项均 0 failure/0 error（默认无连接环境下 PostgreSQL
  12 项条件跳过，另已在一次性真库中无跳过通过）；根项目 473 项、52 项条件跳过、0 failure/0 error。
  正式字体挂载的 UI2 脚本验证 63 个终端视觉快照；`assemble`、`verifyUi2SelfContained` 与
  `git diff --check` 均通过。runtime dev JAR SHA-256 为
  `e845ae956199c1e62e173e4729729e90e24dca98bdd02caa6b0db54ffbb4bc4f`。未部署。

# 2026-09-10 - 跨服任务平台 Q.6 玩家任务中心与有界分页

- 新增平台无关 `QuestCenterVisualModel/QuestCenterVisualDocument/QuestCenterActionPort`，玩家任务中心已经能在
  Java2D 与 Minecraft 双宿主之间共享浏览、章节、筛选、详情、Task 进度、奖励与领取动作。浏览、详情、空态、
  错误态覆盖三套主题和三档视口，统一视觉报告现验证 99 个终端快照。
- 修复大型任务库不可用的结构问题：Document 不再静默只绘制前 6/9 个章节和 7 个任务。模型新增查询词、
  章节/任务页索引、页大小、总数和前后页状态，动作端口新增查询与两类翻页意图，页面明确显示分页控件。
- 纯核心新增 `QuestCenterPageRequest/QuestCenterPaginator/QuestCenterPage/QuestCenterChapterSummary`。搜索和
  `all/active/claimable/completed` 筛选先于分页，非法页码会限幅，客户端页大小硬限制为章节 12、任务 24；
  `QuestCenterQuery.loadPage` 使 PostgreSQL 读取结果能在进入网络前缩成有界快照，章节摘要不携带全部布局坐标。
- PostgreSQL `JdbcQuestCenterQuery` 已按服务端参与主体批量连接最新发布定义、章节、玩家进度、Task 向量和
  奖励状态；根适配器可直接映射有界服务端页，不再二次截断。正式终端 Snapshot/Action 接线仍未启用，生产
  migration、Forge Handler、数据库和服务器均未修改，也未部署。
- CodeGraph 同步 21 个文件；`QuestCenterPaginator` 影响 27 个符号，`QuestCenterVisualModel` 影响 194 个符号。
  Core 66 项、UI2 Terminal 4 项以及根适配器 2 项定向测试均 0 failure；99 个视觉快照通过结构审计。
  章节与任务节点使用实体 ID 而非页内序号作为对账 key，翻页不会把上一页控件状态错误保留给下一页实体。
  `assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过；runtime JAR SHA-256 为
  `0043011a280aed98ffd84ad23fbd0537bf9f96ba35b299357ea79fab1e3035b6`。未部署。

# 2026-09-10 - 跨服任务平台 Q.6 Base 终端协议接线

- 新增 Base 自有 `TerminalQuestCenterSectionSnapshot` 和 `TerminalQuestActionPayload`，把玩家任务中心接入现有
  `TerminalSnapshotMessage/TerminalActionMessage` 主链；没有复制 BetterQuesting Packet、客户端数据库或
  玩家身份字段。职业正式路由改用 `TerminalQuestCenterDocument`，共享纯 Java Document，Minecraft 层只解析
  真实物品图标。
- 服务端只从当前认证 `EntityPlayerMP` 的 UUID 构造 `ParticipantId`。查询词、筛选、章节选择和两级页码均受
  限；编码端将章节、任务、Task、Reward 分别截断到 12、24、64、64，解码端拒绝越界数量。详情页刷新保留
  当前任务，只有返回动作退出详情。领取仍明确只读，生产 `QuestCenterQuery` 未装配，现有 BQ 不受影响。
- 新增动作载荷、协议完整详情往返、集合上限、认证身份和快照回传测试。CodeGraph 已同步 18 个改动文件；
  `TerminalService` 影响 396 个符号，`TerminalQuestCenterDocument` 影响 63 个符号，`affected` 未额外识别测试，
  因此执行了全量测试。根项目 481、Quest Core 66、Importer 17、PostgreSQL 20、UI2 Core 41、UI2 Lab 2、
  UI2 Terminal 4、UI2 Demo 5 项均 0 failure/0 error；挂载开发用 CJK 字体后 99 个终端视觉快照通过。
- `assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过；runtime JAR SHA-256 为
  `7d46c38bd001b96eea877f85b80cc131c87b3d1c87ec35d98535d403e8f41c7c`。生产 migration、Handler、数据库、
  Lobby/S1/S2 和客户端均未修改，本批不部署。

# 2026-09-10 - 跨服任务平台 Q.6 只读运行时与认证领取装配

- 新增默认关闭的 `questPostgresReadEnabled` 服务端开关和 `QuestModule`。模块只在专用服务器、共享 Banking
  JDBC 已就绪、五张 Quest 表通过只读校验时安装终端查询；不自动建表、不读取客户端数据库配置、不创建第二套
  凭据。任一条件失败均清空已安装端口并保持任务中心不可用，现有 BetterQuesting 继续工作。
- 根构建开始内嵌 Base 自己的 `galaxy-quest-postgres`；自包含门禁明确检查 Quest Core 的认证领取服务和
  PostgreSQL 查询类存在于最终 JAR。玩家仍只需 Base JAR，不依赖 BetterQuesting API 或额外 Quest Mod。
- 新增 `AuthenticatedQuestClaimService`：客户端只能提交 Quest UUID；服务端在事务内从最新发布定义和当前玩家
  进度推导 version/cycle，然后调用已实现的幂等 Claim Repository。成功进入 PENDING 交付队列，重复、缺 Choice、
  非本人、非法状态与缺失任务均返回明确文案。生产开关保持关闭，领取不会在现网触发。
- CodeGraph 同步 10 个文件；`QuestModule` 影响 43 个符号，`AuthenticatedQuestClaimService` 影响 29 个符号，
  `affected` 未额外识别测试，故执行全量回归。根项目 484 项（52 项环境条件跳过）、Quest Core 68、Importer 17、
  PostgreSQL 20（13 项因本轮未注入测试库环境而条件跳过）、UI2 Core 41、Lab 2、Terminal 4、Demo 5 项均
  0 failure/0 error。99 个视觉快照、`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。
- runtime JAR SHA-256 为 `6329d193131740daeba58ae0c34671690aad676bc01ba90532256e698435b696`。
  本轮未执行生产 migration、未启用 Quest 开关、未注册 Forge Handler、未部署，也未修改 Lobby/S1/S2、客户端
  或生产数据库。
## 2026-09-10 — Q.6 奖励多选项认证边界

- 新增 `AuthenticatedRewardChoiceService`：客户端未来只需提交任务、奖励键和选项序号；玩家身份、已发布定义版本、当前循环及 entitlement key 均由服务端推导。
- 仅允许 `bq_standard:choice` 奖励进入选择仓储，非多选奖励在数据库调用前拒绝；选择仍由 PostgreSQL 仓储保证不可变与冲突检测。
- CodeGraph 确认原 `RewardChoiceSelectionRepository` 尚无调用者，本批先建立纯核心服务，不提前暴露不完整的终端按钮。
- 验证：`:galaxy-quest-core:test` 70 项通过，`git diff --check` 通过；未部署、未修改生产数据库。

### 终端闭环

- `QUEST_SELECT_REWARD_CHOICE` 已贯通 UI2、终端意图协议、认证服务与 PostgreSQL 不可变选择仓储。
- 任务奖励快照现在携带最多 32 个有界选项及服务端读取的已选序号；客户端仍不能提交玩家、定义版本、周期或 entitlement key。
- `JdbcQuestCenterQuery` 联查 choice selection，启动校验增加对应表；UI2 详情使用稳定键显示选项，选定后禁止改选，随后可继续执行原有领取流程。
- 旧 6 段任务意图仍可解码；新协议增加奖励键和 0–31 的选项序号。协议往返测试覆盖选项物品、数量和已选状态。
- 验证：核心、PostgreSQL、UI2 Terminal、根项目全量测试通过；99 个终端视觉快照通过；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。
- 运行 JAR：`build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `8babf202d363bb6ef10c6c0e96b022d581491df6054dd4c3fe6e5602576c789a`。未部署、未修改生产数据库。

## 2026-09-10 — Q.6 跨服任务追踪真源

- 新增纯核心 `AuthenticatedQuestTrackingService` / `QuestTrackingRepository` 与 PostgreSQL `JdbcQuestTrackingRepository`；追踪状态由数据库保存，不复用 BetterQuesting 客户端 JSON。
- 新 DDL `galaxy_quest_player_preference` 以 `(player_id, quest_id)` 唯一，原子切换 `tracked`，并为玩家最近追踪查询建立部分索引。该 DDL 仍只是评审/测试制品，未应用生产库。
- `JdbcQuestCenterQuery`、终端快照/编解码及 UI2 目录/详情已贯通追踪状态；客户端只提交任务 ID，服务端验证最新已发布任务并使用认证玩家身份。
- CodeGraph 影响集中在 QuestModule、TerminalService、查询映射与 UI2 任务文档；因此执行了全量回归。
- 验证：根项目 485 项（52 跳过）、Quest Core 72 项、Quest PostgreSQL 20 项（13 项因集成环境条件跳过）、UI2 Terminal 4 项、UI2 Demo 5 项，均 0 失败；99 个终端视觉快照通过；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。
- 运行 JAR：`build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `7b6d3f84bca076285a8545bf69b6d28d9a227b87de3db7ba35e93ed31ad68eeb`。未部署、未改生产数据库。

## 2026-09-11 — Q.6 已追踪任务 HUD

- 新增有界 `TerminalTrackedQuestSnapshot`：最多 5 个任务、每项最多 3 个目标，只有展示字段，不含玩家身份、版本、周期或任何写权限。
- `TrackedQuestSnapshotFactory` 从服务端 `QuestCenterQuery` 投影数据库真源；`TrackedQuestSyncController` 在登录及低频服务端周期通过 Base 自有消息发送，不使用 BetterQuesting Packet/JSON。
- 客户端 `TrackedQuestHudState` 保存不可变快照，离开世界立即清空；原 HUD 通知层增加左上角任务摘要，最多显示 3 项并对标题、目标文本和进度做宽度限制。
- Forge 事件总线注册被抽成可替换组合边界，避免纯 JVM 测试初始化 Forge 单例；全量测试曾捕获此问题，修复后重跑通过。
- CodeGraph 影响集中在 QuestModule、同步控制器、网络消息和 HUD；执行全量回归而非仅定向测试。
- 验证：根项目 487 项（52 跳过）、Quest Core 72 项、Quest PostgreSQL 20 项（13 项环境条件跳过）、UI2 Terminal 4 项、UI2 Demo 5 项，均 0 失败；99 个终端视觉快照通过；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。
- 运行 JAR：`build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `44c41b41f968c62ec58a8fb3f567d6ec4fe75466b3f6afe428fe39a9b9f6868a`。未部署、未修改生产数据库。

## 2026-09-11 — Q.6 服务端草稿管理命令层

- 新增纯 Java `QuestDraftManagementService`、认证编辑者与权限合同、结构化结果；支持草稿创建、内容哈希乐观锁更新、发布和退役，并保持所有写操作处于 `QuestTransaction` 内。
- `QuestDefinitionValidator` 现在可直接用 `QuestEditorTypeRegistry` 校验 Task/Reward 类型及其字段参数，问题路径稳定到具体元素和字段；未知类型、自依赖与重复 key 均在持久化前拒绝。
- 权限来源被明确限制为服务端认证的 `QuestEditorActor`，客户端未来只发送编辑意图和预期内容哈希，不携带管理员判定。BetterQuesting GUI、Packet、客户端数据库和 JSON 写入均未引入。
- 新增 5 组管理服务测试，覆盖拒绝未授权、结构校验、完整创建/更新/发布/退役事务、过期哈希冲突及不存在目标。CodeGraph 已同步新增文件；Quest Core 77 项、根项目 487 项（52 项环境条件跳过）均 0 failure/0 error，99 个终端视觉快照、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过。
- 运行 JAR：`build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `3b01573ae6179077796dabdce01ed1dde14d750377f3e6125d56fbce5a80057a`。生产组合根、管理网络、数据库和部署尚未改动。

## 2026-09-11 — Q.6 管理查询与 Forge 权限组合

- 新增独立 `QuestDefinitionManagementQuery`、有界请求和分页结果，不扩张运行时 `QuestDefinitionRepository` 的玩家目录合同。PostgreSQL 查询支持 DRAFT/PUBLISHED/RETIRED、名称或完整 UUID 筛选、稳定排序和最多 50 项服务器分页。
- 新增认证查询包装；未授权编辑者不会触发存储访问。`MinecraftQuestEditorAuthorization` 只按当前在线 `EntityPlayerMP` 的认证 UUID 判断，并复用 Forge 命令权限 level 2，客户端无法提交权限布尔值或冒用离线名称。
- BQ Standard 编辑类型规范从导入器下沉至纯 Core；导入器保留兼容门面，生产 QuestModule 可直接组合 13 类 Task、6 类 Reward 的字段规范，不需要把导入 CLI/Gson 打进 Base JAR。
- CodeGraph 影响：管理查询 44 个符号、QuestModule 55 个符号、标准类型注册表 33 个符号。新增 3 项认证查询/边界测试，并扩充 PostgreSQL 乐观锁集成用例验证管理筛选。Quest Core 80 项、根项目 487 项（52 项环境条件跳过）均 0 failure/0 error；Importer/PostgreSQL 定向测试、99 个终端视觉快照、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 全部通过。
- 运行 JAR：`build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `a2dc7225711a78828e07552e432b937b94d83b665e330b80a859c8a0730b18df`。未部署、未执行生产 DDL，Lobby/S1/S2 与客户端均未修改。

## 2026-09-11 — Q.6 任务管理终端只读闭环

- Base 终端新增 `QUEST_ADMIN_OPEN/FILTER/PAGE`，任务动作载荷扩展管理生命周期和页码，同时继续兼容原 6/8 段编码。管理快照最多 50 条，只包含任务 ID、版本、名称、生命周期、内容哈希、发布时间及 Task/Reward 数量。
- `TerminalService` 从当前 `EntityPlayerMP` 传入认证 UUID/名称，认证管理查询再次通过在线实体与 OP level 2 校验；未授权访问返回稳定拒绝页，不执行数据库查询。QuestModule 负责安装和失败时清空查询端口。
- 新增纯 Java `QuestManagementVisualModel/Document/ActionPort`。玩家任务页提供管理入口，管理页支持返回、生命周期筛选和服务器分页；三主题下的正常/空态及三档视口均进入 Java2D 结构审计。
- 新增协议截断、管理动作兼容、认证身份映射与 UI2 视觉测试。根项目 490 项（52 项环境条件跳过）、UI2 Terminal 5 项均 0 failure/0 error；终端视觉快照由 99 增至 117，`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 全部通过。
- 运行 JAR：`build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `770f177ed1e31bf62a99576e9721fcbd76ca86128aef18bd53a2da1dfcb5739f`。未部署、未执行生产 DDL，Lobby/S1/S2 与客户端均未修改。

## 2026-09-11 — Q.6 定义详情与发布生命周期闭环

- 管理查询增加按 `quest_id + version` 读取定义，终端详情以有界结构返回说明、前置、Task、Reward、逻辑和行为选项；UI2 列表可进入详情，并只在 DRAFT/PUBLISHED 状态分别显示发布/退役动作。
- 管理动作只提交任务 ID、版本和已读内容哈希。`TerminalService` 使用当前认证玩家构造编辑者，写服务再次执行服务端授权、类型/参数校验和 PostgreSQL 乐观锁；`QuestModule` 在启动、失败清理与测试复位时成对安装/卸载读写端口。
- 发布前新增依赖图门禁：缺少已发布直接前置、不可完整读取的依赖链、超过有界遍历规模以及由候选最新版本形成的循环均拒绝发布。玩家运行时仍只读取最新已发布版本。
- CodeGraph 已同步；`buildQuestManagementSnapshot` 影响 3 个符号，图校验影响 20 个符号。全量 666 项测试（65 项环境条件跳过）0 failure/0 error，126 个终端视觉快照通过；`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过。
- 运行 JAR：`build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 `1ac899ab4204033ba9717b6f02ce0f2f662ba57d0a3272ca7335571d3a672da1`。未部署、未执行生产 DDL，Lobby/S1/S2 与客户端均未修改。下一项为草稿创建和字段编辑表单。

### 字段编辑命令基础

- 新增平台无关 `QuestDraftEditOperation/QuestDraftEditRequest`，用最多 128 个白名单语义操作表达一次编辑，不接受反射、任意对象图或客户端权限字段。
- `QuestDraftManagementService.applyEdit` 仅允许编辑 DRAFT：先在不可变定义的工作副本上应用完整批次，再复用类型注册表验证并以预期内容哈希执行一次事务写入；错误 key、位置、类型或陈旧哈希均不会产生部分保存。
- 新增原子批量编辑与失败不落库测试；Quest Core 定向测试通过。该合同尚未暴露到网络或正式 UI，下一步是有界操作编解码和由字段描述器驱动的表单。

### 基础字段编辑端到端

- 新增 Base 自有 `TerminalQuestDraftEditPayload`，使用带版本标识的有界二进制载荷承载最多 128 个语义操作；限制载荷、文本、参数数量和值长度，并拒绝未知操作、重复参数与尾随内容，不复用 BetterQuesting Packet。
- `QUEST_ADMIN_EDIT` 已接入正式终端。DRAFT 详情可编辑名称、说明、前置逻辑和目标逻辑；客户端只形成语义批次，服务端重新鉴权、读取当前草稿并通过内容哈希一次提交。保存成功后返回新哈希，UI 编辑会话自动收口；取消不会发送网络动作。
- 编辑页新增三主题、三视口确定性场景。第一次门禁发现 350 宽详情操作栏出现 0 宽按钮，随后把详情头改为响应式两行结构；最终 135 个终端视觉快照全部通过。
- CodeGraph 显示编辑载荷解码影响 40 个符号、编辑会话影响 52 个符号。全量 670 项测试（65 项环境条件跳过）0 failure/0 error；`assemble`、`verifyUi2SelfContained` 和 `git diff --check` 通过。
- 运行 JAR SHA-256：`a18e6157de0a7660dbe20479e5ec4a656f5dd817f966db0c4d00242af0f7051b`。未部署、未执行生产 DDL、未触碰 Lobby/S1/S2 或客户端。下一项为字段描述器驱动的 Task/Reward 编辑器、前置选择器与草稿创建模板。

### Task/Reward 描述器驱动编辑

- 管理快照新增有界 Task/Reward 类型目录，字段描述包含类型、必填、数值范围和枚举选项；目录只在管理查询已经通过服务端授权后生成。协议往返测试覆盖 nullable 上下界和字段类型。
- UI2 不再按具体 BetterQuesting 类型硬编码表单。现有 Task/Reward 行可进入元素编辑器；BOOLEAN、ENUM 与其他字段分别生成开关、受限选择和文本输入，字段超过四项时分页。稳定 key、Task 可选属性及参数通过 `REPLACE_TASK/REPLACE_REWARD` 原子保存并复用服务端校验。
- 元素编辑器增加三主题、三视口结构场景，视觉快照由 135 增至 144；详情和元素编辑在 350×193 下均无 0 宽或越界内容。
- CodeGraph 显示类型目录影响 26 个符号，元素编辑器核心影响 4 个直接符号。全量 670 项测试（65 项环境条件跳过）0 failure/0 error，144 个终端视觉快照通过；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。
- 运行 JAR SHA-256：`d96ee45042e7ea69cb6028ae961f4746001de7b4f67b0a1d81ddd05523ae9da9`。未部署、未执行生产 DDL。下一项为元素新增/删除/排序、前置任务选择器和新草稿模板。

### Task/Reward 元素新增、删除与排序

- DRAFT 详情的目标与奖励卡增加类型目录驱动的新增入口；新元素只允许在服务端已声明的类型间切换，并在必填字段完整后启用保存。现有元素提供上移、下移和两步删除确认。
- Minecraft 适配桥把新元素映射为 `ADD_TASK/ADD_REWARD`，把排序与删除分别映射为 `MOVE_*`、`REMOVE_*`；载荷继续只承载 Base 白名单语义操作，不发送完整定义或客户端权限。
- CodeGraph 同步后确认主要影响为 `QuestManagementVisualDocument`、`TerminalQuestCenterDocument` 和视觉场景。UI2 Terminal 定向测试、根项目全量 670 项测试（65 项环境条件跳过）、`assemble` 与 `verifyUi2SelfContained` 均通过；三主题三视口的终端视觉快照由 144 增至 153，结构告警为 0。
- 未部署、未执行生产 DDL、未触碰 Lobby/S1/S2 或客户端。下一项为前置任务选择器和新草稿模板。

### 前置任务选择器

- DRAFT 基础编辑页增加独立前置选择子页；服务端在详情已授权后读取最多 50 个最新 PUBLISHED 定义作为候选并排除自身，普通管理列表分页合同不变。
- 客户端保存前后 UUID 集合差异，只发出 `ADD_PREREQUISITE/REMOVE_PREREQUISITE` 白名单操作；服务端继续执行内容哈希乐观锁，发布时继续执行完整依赖存在性与循环检测。
- 新增三主题、三视口前置选择场景；162 个终端视觉快照全部通过且结构告警为 0。根项目 670 项测试（65 项环境条件跳过）、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过。运行 JAR SHA-256：`f3de8c1212f071b5c35ab649a337b381c572819411af7e6069e764a9c5c94053`。未部署、未执行生产 DDL。下一项为受限的新草稿模板与创建协议。

### 服务端所有的新草稿模板

- 新增 `QuestDraftTemplate`：空白任务、人工确认目标和经验目标。客户端只提交模板 ID 与任务名称；UUID、版本、逻辑和初始 Task 均由服务端构造，再复用原管理服务授权、标准类型校验和事务创建。
- 终端任务管理列表增加新建入口与独立创建页；`QUEST_ADMIN_CREATE` 使用任务意图协议新增的专用 `managementTemplate` 字段，不复用奖励字段，也不承载玩家身份、任意参数或完整定义；旧 6/8/10/11/12 段意图继续兼容解码。创建成功后返回 DRAFT 查询结果。
- 模板合法性、“身份由服务端生成”和专用模板字段往返有自动测试；创建页增加三主题、三视口场景。全量 673 项测试（65 项环境条件跳过）、171 个终端视觉快照、`assemble`、`verifyUi2SelfContained`、`git diff --check` 全部通过。
- 运行 JAR SHA-256：`b7249a5897ba8df7ba9773174a1e56eebe65d580c27cfc2d02ecdcd9ef180b3c`。未部署、未执行生产 DDL、未触碰 Lobby/S1/S2 或客户端。下一批继续吸收任务行为选项、重复策略和章节编辑能力。

### 重复策略与任务行为编辑

- 草稿语义操作新增末尾兼容的 `SET_OPTION`。服务端白名单覆盖重复周期/边界、可见性、主线、静默、自动领取、锁定进度、同时满足、全局/共享、图标与进度/完成音效；严格拒绝未知键和非法值，并保持批次原子回滚。
- UI2 提供三页响应式表单，分别编辑重复与可见性、执行语义、资源与音效；选项保存只发送实际变化。客户端仍不能提交完整 `RepeatPolicy`、`QuestBehavior` 或任意 JSON。
- 新增连续选项合并及未知选项不落库测试，编辑载荷往返覆盖 `SET_OPTION`；三主题三视口视觉场景增至 189 张且 0 结构告警。全量 675 项测试（65 项跳过）、`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。
- 运行 JAR SHA-256：`1a95d3a71de20a7cea11095f727c184c541a7e21dd844fa46f2596cfc3ed76ba`。未部署、未执行生产 DDL。下一阶段进入章节管理、布局放置和章节发布生命周期。

### 章节管理服务端基础

- 新增章节批量语义编辑请求、结构化结果与认证管理查询。章节草稿支持名称、说明、图标、背景、可见性以及任务位置的放置、移动、缩放和移除；整批先在内存完成并校验，再以预期内容哈希执行一次乐观锁写入。
- 章节发布会重新校验所有位置，并要求引用的任务存在已发布版本；非法输入、越权、未找到与并发冲突使用独立结果。认证查询在访问仓储前检查当前服务端编辑者权限，并提供生命周期、名称/完整 UUID 和最多 50 条的服务器分页。
- `JdbcQuestChapterRepository` 实现管理查询，`QuestModule` 在同一共享 JDBC 与 Forge 权限边界下安装章节读写端口，停用或初始化失败时成对清空。当前尚未增加客户端章节载荷或 UI2 页面，因此正式玩家入口行为未改变。
- CodeGraph 已同步并检查 `QuestChapterManagementService`、`JdbcQuestChapterRepository` 的影响链。Quest Core 定向测试、PostgreSQL/根源码编译与 `QuestModuleTest` 通过；PostgreSQL 真库用例已增加章节名称/生命周期查询断言但未执行生产数据库。未部署、未执行 DDL、未触碰 Lobby/S1/S2 或客户端。下一步为章节专用有界意图载荷和 UI2 布局编辑器。

### 章节终端载荷与纯 Java 布局画布

- 新增独立 `TerminalQuestChapterEditPayload`，完整覆盖章节的 10 类白名单操作，同时限制编码大小、操作数量、文本长度、UUID、版本和预期哈希；畸形编码、未知序号和尾随字节均拒绝，不复用 BQ Packet 或普通任务导航字段。
- 新增平台无关章节管理模型、动作端口和生产 UI2 Document。章节详情使用专用画布按内容边界确定性缩放，可显示负坐标、不同尺寸及重叠任务；后放置条目保持上层绘制/优先命中。选中后可步进移动、扩大或移除，并可从服务端候选加入章节。
- 载荷往返/失败关闭测试通过；章节列表与空间详情在 `350×193`、`427×240`、`620×340` 下结构审计均为 0 问题。当前 Document 尚未接正式终端网络快照，下一步补章节 DTO/编解码、认证路由和 Minecraft Action Bridge。

### 章节管理正式终端闭环

- 正式终端快照和网络编解码新增有界章节管理投影；列表、详情、空间位置和候选任务均不携带玩家身份或写权限。
- `TerminalService` 接入章节打开、筛选、分页、选择、创建、编辑、发布和退役；定向测试证明当前认证玩家身份、20 项服务端分页以及负坐标/尺寸可完整映射到终端 DTO。
- `TerminalQuestCenterDocument` 与 `TerminalApplicationScreen` 已接通章节动作，任务定义管理页增加章节入口。章节 UI2 新增三主题下列表、空态、空间详情共 9 个场景，三档视口共 27 张快照。
- CodeGraph 影响集中于 `TerminalService`、`TerminalQuestCenterDocument` 和 `QuestChapterManagementVisualDocument`，因此回归包含协议、服务端路由、UI2 Terminal 和根构建。
- 自动验收：全仓 686 项测试（65 项环境条件跳过）0 failure/0 error；231 张 UI Lab/正式终端 PNG 与结构快照生成成功；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。运行 JAR SHA-256：`53bcc1a39a6aa757d52609536b1673f97761d18bc3760bf5f089d61aec80a3ff`。
- 本批未部署、未执行生产 DDL、未启用 Quest 生产开关，也未触碰 Lobby/S1/S2 或客户端。章节字段表单、候选搜索和直接拖拽/缩放仍为下一批。

### 章节新建命名表单

- 章节管理的新建动作不再立即提交固定“新任务章节”。新增纯 Java UI2 名称表单，限制 96 字符、空名称禁用提交、取消不产生服务端动作；UUID、版本和初始布局继续完全由服务端创建。
- 新建表单加入三套主题和三档视口，统一视觉报告由 231 增至 240 张 PNG/结构快照，结构问题为 0。
- 全仓 686 项测试（65 项环境条件跳过）0 failure/0 error；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。运行 JAR SHA-256：`fbdb51fa24a02ebef869885cd361f64ff1bf644df051c980b3b2eed9aceb0304`。
- 未部署、未执行生产 DDL。下一项是章节基础属性的两页编辑表单，然后进入候选任务搜索和直接画布拖拽。

### 章节基础属性编辑表单

- DRAFT 章节详情增加两页纯 Java UI2 编辑器：名称/说明/可见性，以及图标/背景/背景尺寸；已发布章节没有编辑入口。
- 保存通过现有 `saveBasics` 桥产生 `SET_NAME/SET_DESCRIPTION/SET_ICON/SET_BACKGROUND/SET_BACKGROUND_SIZE/SET_VISIBILITY` 白名单操作，继续绑定章节版本和已读内容哈希。
- 新增真实点击编辑、保存的动作测试，逐项断言 ID、版本、哈希和六类字段；两页表单加入三主题三视口视觉门禁。
- 全仓 687 项测试（65 项环境条件跳过）0 failure/0 error；258 张 PNG/结构快照生成成功；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。运行 JAR SHA-256：`71522dd13fb39c8260dcf60b539e4d28b29a134fc7c209fb186d72cf009b2051`。
- 未部署、未执行生产 DDL。下一批为候选任务搜索/分页，以及章节画布拖拽、平移和缩放。

### 章节候选检索与直接画布操作

- 候选任务侧栏增加名称/UUID 搜索和每页三项的本地分页，可从服务端授权返回的最多 50 个发布任务中选择任意一项；无匹配结果有明确空态。
- 章节画布增加 Pointer Capture：左键拖拽任务提交 `MOVE`，右键拖拽提交 `RESIZE`，拖动时提供即时轮廓预览；鼠标释放前不修改定义，最终仍由服务端鉴权和乐观锁写入。
- 自动测试真实分发按下/移动/释放事件，验证捕获、负坐标换算、移动与缩放语义；另有搜索中文并加入非首个候选的动作测试。
- 全仓 690 项测试（65 项环境条件跳过）0 failure/0 error；258 张 PNG/结构快照通过；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。运行 JAR SHA-256：`7cfb2ea1ce9fb0eba0a42b3def553807a2591d75ff4806c0d2ad42cd837d0c0c`。
- 未部署、未执行生产 DDL。下一步补画布平移、缩放、网格和键盘精调，然后继续 BetterQuesting 功能差距审计。

### 章节画布导航与键盘精调

- 章节画布增加自适应坐标网格、光标锚定滚轮缩放、中键 Pointer Capture 平移；缩放和平移仅是客户端视图状态，不进入任务定义或服务端载荷。
- DRAFT 中选中任务后，方向键按一个逻辑单位提交既有 `MOVE` 语义动作；没有新增客户端直接写定义的通道，服务端版本、哈希、授权和事务边界保持不变。
- CodeGraph 同步后确认主要影响集中于 `QuestChapterManagementVisualDocument` 及其纯 Java 测试；文件级 `affected` 未发现额外测试入口。定向测试新增网格、缩放、平移、捕获释放和键盘精调覆盖。
- 全仓 692 项测试（65 项环境条件跳过）0 failure/0 error，258 张 UI Lab/正式终端 PNG 与结构快照生成成功；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。运行 JAR SHA-256：`d80dfddf9731f713eec3045c6860e792d0f9bb31d0810684b775295172ff31c4`。
- 本批仍未部署、未执行生产 DDL、未启用 Quest 生产开关。下一步为 BetterQuesting 功能差距审计。

### BetterQuesting 能力矩阵与批量克隆核心

- 新建 `docs/betterquesting-capability-absorption-matrix-2026-09-13.md`，以本地只读源码逐项记录任务定义、13 类 Standard Task、6 类 Reward、编辑、诊断、迁移与影子运行差距；明确不搬运 GUI、Packet、客户端数据库、JSON 存档和 Mod 生命周期。
- 固定兼容提交 `524b365211b6b3a9672cab8ae45b4e07e726d49e` 仍在本地源码对象库中；本地镜像 HEAD 为 `5ff4864bb072c866ac8c527495c76d3d77e4be2f`，后续修复只能作为注明版本的补充参考。
- 新增纯 Java `QuestDefinitionBatchCloner`、有界 `QuestDefinitionCloneRequest` 和结构化结果。组选中复制保持源顺序，为复制品生成不碰撞 UUID，只重映射组内前置，保留组外前置、Task、Reward、重复策略和行为选项。
- `QuestDraftManagementService.cloneDrafts` 在读取任何定义前执行服务端授权，按精确 ID+版本读取，完整校验复制品，并在单个 `QuestTransaction` 中创建所有版本 1 草稿；客户端没有 NBT 或身份写入口。
- CodeGraph `impact` 显示批量克隆影响集中于纯核心管理服务与新增测试，文件级 `affected` 没有遗漏额外测试入口。全仓 698 项测试（65 项环境条件跳过）0 failure/0 error，258 张 UI Lab/正式终端 PNG 与结构快照通过；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。
- 运行 JAR SHA-256：`80415a66ac7fe895b7747d7af8346ccd0d586e035ead65ff94f3b0eb79167ef5`。未部署、未执行生产 DDL、未启用 Quest 生产开关。下一步把任务克隆计划与章节位置合并为服务端原子事务，再开放有界终端动作。

### BQ 替代目标文档进度回填

- 将当前代码与自动门禁证明的已完成能力回填到原始目标文档，而不再只散落在阶段 Worklog；同步修正 Q.4、Q.6 状态。
- 新增“尚未完成”和六项最终验收定义，明确批量编辑、检测兼容、团队奖励、影子运行、迁移演练及冻结 BQ 都是目标组成部分，后续不得把某个中间批次通过误报为完成替代。
- 本次仅更新文档，不修改运行代码、不部署、不执行数据库操作。

### 任务复制与章节布局原子复合操作

- 新增 `QuestChapterCloneService`、有界请求和结果：服务端先授权，再按章节 ID/版本/哈希读取 DRAFT 的真实布局，按精确任务 ID/版本读取源定义；未放置、缺失、发布章节、越界坐标及无效定义都会在写入前拒绝。
- 复制保留 BQ 组选中语义：首个选中位置是锚点，后续位置相对偏移保持；新任务 UUID 只由服务端生成，组内前置重映射、组外前置保留。所有新 DRAFT 定义和章节更新落在同一个 `QuestTransaction`，章节乐观锁冲突将触发事务回滚。
- 纯核心测试覆盖相对位置、内部前置、单事务、未放置源和越权早拒绝。终端专用载荷、服务端路由和 UI 多选还未接入，因此不能向玩家展示或称为完整复制功能。
- 全仓 701 项测试（65 项环境条件跳过）0 failure/0 error，258 张 UI Lab/正式终端 PNG 与结构快照通过；`assemble`、`verifyUi2SelfContained`、`git diff --check` 通过。运行 JAR SHA-256：`dc153ef9e8fa1b3bc0769bdc67f4e929696f528e3d681d73481412622a03479b`。
- 未部署、未执行生产 DDL、未启用 Quest 生产开关。下一步实现专用有界终端载荷及服务端路由。

### 任务复制终端动作与画布多选

- 新增 `TerminalQuestChapterClonePayload` 与 `QUEST_CHAPTER_ADMIN_CLONE`：载荷只允许章节 ID、版本、内容哈希、锚点、查询上下文和 1–128 个任务 ID/版本；Base64 解析要求 magic、边界和无尾随字节，服务端从当前认证玩家构造 `QuestEditorActor`。
- `QuestModule` 在已启用的 PostgreSQL Quest 运行时时安装 `QuestChapterCloneService`；`TerminalService` 将复制结果回填章节管理快照。运行时未启用、越权、缺失、冲突和无效数据均为失败闭合且不产生写入。
- 章节画布增加草稿多选模式与复制按钮；普通单选可复制一项，多选只在显式模式下切换，复制语义继续由服务端读取真实布局并做内部前置重映射。UI2 纯 Java 测试覆盖选择后动作和三档布局。
- CodeGraph 对 `QuestChapterCloneService`、`TerminalQuestChapterClonePayload`、`QuestChapterManagementVisualDocument` 执行 `impact`，并对实际文件执行 `affected`；完整 Docker 门禁 `:galaxy-quest-core:test :galaxy-quest-postgres:test :ui2-core:test :ui2-lab:test :ui2-terminal:test :ui2-demo:test :ui2-demo:renderGolden test assemble verifyUi2SelfContained` 和 `git diff --check` 通过，258 张 PNG 生成。
- 未部署、未执行生产 DDL、未启用 Quest 生产开关；下一项转入 QB.2：章节排序/对齐、依赖连线及影响预览。

### QB.2 章节依赖图只读投影基础

- 根据 BQ `QuestLineDatabase#lineOrder` 与 `NetChapterEdit` 的源码行为复核：章节排序是独立目录顺序，不等同于画布任务坐标；当前 Base 尚无该 PostgreSQL 权威目录模型，不能把拖拽误实现为排序。
- 新增纯 Java `QuestChapterDependencyGraph`：用服务器加载的 `QuestDefinition` 和章节真实放置项生成依赖边，区分 `INTERNAL`、`EXTERNAL` 与 `MISSING`，并提供入边计数；不接受客户端提交的连线或定义。
- `QuestChapterDependencyGraphTest` 覆盖三类边与计数。定向测试后完整 Docker 门禁 `:galaxy-quest-core:test :galaxy-quest-postgres:test :ui2-core:test :ui2-lab:test :ui2-terminal:test :ui2-demo:test :ui2-demo:renderGolden test assemble verifyUi2SelfContained` 与 `git diff --check` 均通过；下一步将该投影接入有界终端快照、网络编码和 UI2 画布连线。
- 预接入复核：章节管理快照采用顺序二进制编码；依赖边必须作为受版本控制的有界扩展，与同一 Base JAR 同步发布，禁止向旧字段序列中直接插入数据或复用 BetterQuesting 网络同步。

### QB.2 章节依赖图正式快照与画布连接

- 新增 `AuthenticatedQuestChapterDependencyQuery`：先以当前服务端 `QuestEditorActor` 鉴权，再读取所选章节的真实版本与章节内任务的最新已发布定义；同时读取直接画布外前置，以准确区分内部边、外部边与缺失前置。客户端没有提供依赖边、定义或玩家身份的入口。
- `TerminalQuestCenterSectionSnapshot.ChapterDetail`、`OpenTerminalApprovedMessage` 和 `TerminalQuestCenterDocument` 现携带最多 8192 条有界依赖边；读端拒绝负数和越界数量。该序列变更要求同一 Base JAR 的客户端与服务端同时更新，未试图兼容或复用 BetterQuesting Packet。
- UI2 章节画布在网格和任务卡之下绘制内部前置的三段正交连接；画布外或缺失前置没有被虚构为节点，后续将在侧栏增加明确的诊断/影响预览。
- 自动覆盖：核心授权/外部/缺失分类，终端编解码往返，UI2 内部边几何与三档布局。CodeGraph 已对 `AuthenticatedQuestChapterDependencyQuery`、`QuestChapterManagementVisualDocument` 执行 `impact`，并对核心、终端网络与 UI 文件执行 `affected`。
- Docker 门禁使用只读预览字体挂载后通过：`:galaxy-quest-core:test`、`:ui2-terminal:test`、根 `test`、`:ui2-demo:test`、`:ui2-demo:renderGolden`、`assemble`、`verifyUi2SelfContained` 与 `git diff --check`；生成 258 张 UI 预览。未部署、未执行生产 DDL、未启用 Quest 生产开关，也未触碰 Lobby/S1/S2 或客户端。

### QB.2 前置诊断摘要

- 章节侧栏新增固定高度的“内部 / 外部 / 缺失”前置汇总，所有三类服务端投影边都会被看见；仅内部边进入画布，避免把画布外定义或坏引用画成可点击的伪节点。
- UI2 回归覆盖最小 `350×193` 视口仍保留诊断区域；`:ui2-terminal:test`、`:galaxy-quest-core:test` 和 `git diff --check` 通过。未部署、未执行 DDL。

### QB.2 章节目录排序与相对移动

- 依据 BetterQuesting `QuestLineDatabase#lineOrder` 与 `NetChapterEdit.reorderChapters` 的语义，新增纯核心 `QuestChapterCatalogEntry`、`QuestChapterCatalogRepository`、`QuestChapterOrderingService`：目录顺序独立于章节定义版本和画布坐标，不允许把拖拽任务卡误当成章节排序。
- PostgreSQL 新增待迁移的 `galaxy_quest_chapter_catalog` 权威目录表；新草稿章节会在同一事务补齐目录项，读取按目录排序。重排在事务内锁定目录、验证客户端不能伪造或遗漏完整集合后原子替换；相对移动由服务端读取完整当前目录后转换为完整顺序，避免分页、筛选或陈旧 UI 覆盖全局目录。
- 终端新增有界 `TerminalQuestChapterOrderPayload` 与 `QUEST_CHAPTER_ADMIN_MOVE_BEFORE`；上/下操作只提交“源章节、目标前置章节（或追加）”和查询上下文，认证、章节目录读取及最终写入均在服务端完成。UI2 回归验证首行下移会发送第二章置于第一章前的相对语义。
- CodeGraph 已复核 `QuestChapterOrderingService`、`JdbcQuestChapterRepository` 及其终端入口影响范围；Docker 全量门禁在只读预览字体挂载下通过：`:galaxy-quest-core:test`、`:galaxy-quest-postgres:test`、`:ui2-terminal:test`、`:ui2-demo:test`、`:ui2-demo:renderGolden`、根 `test`、`assemble`、`verifyUi2SelfContained` 与 `git diff --check`。未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2 或客户端；仍待管理员实机验收及后续对齐/影响预览。

### QB.2 多选对齐与中断工作收口

- 接手时本批处于半接线状态：核心对齐类、协议和服务端路由已添加，但正式 Screen 未实现发送接口、UI 测试桩缺方法，且 QuestModule 异常清理路径误漏了 tracking 服务重置。先停止扩展，逐项补齐并由编译器复核，没有把不可编译状态留给后续批次。
- 新增服务端权威 `QuestChapterAlignmentService`：请求只包含章节 ID/版本/哈希、2–128 个互异已放置任务 ID 和 `LEFT/CENTER_X/RIGHT/TOP/CENTER_Y/BOTTOM` 白名单模式。服务器从当前 DRAFT 真实布局计算选中组包围盒及新坐标，一次性乐观更新；越权、缺失任务、重复选择、非草稿和版本冲突均失败闭合。
- 新增有界 `TerminalQuestChapterAlignmentPayload`、`QUEST_CHAPTER_ADMIN_ALIGN`、QuestModule 组合根和 UI2 动作链。多选时侧栏显示六种对齐按钮并替代单项移动控件，避免小窗堆叠；三档 `350×193`、`427×240`、`620×340` 结构审计均无越界。
- CodeGraph 同步并复核 `QuestChapterAlignmentService`（31 个受影响符号）、`TerminalQuestChapterAlignmentPayload`（30 个）、`QuestChapterManagementVisualDocument`（119 个）和 `QuestModule`（73 个）；`affected` 未推导测试，故显式执行核心、协议、模块、UI2、PostgreSQL 与全量门禁。
- Docker 在只读预览字体挂载下通过 `:galaxy-quest-core:test :galaxy-quest-postgres:test :ui2-core:test :ui2-lab:test :ui2-terminal:test :ui2-demo:test :ui2-demo:renderGolden test assemble verifyUi2SelfContained`，`git diff --check` 通过。运行 JAR SHA-256 为 `f0bb1b428f7e7be13fdb489ef2cdd08a53e238ab628b0020b1d7522f2aa33698`。未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2 或客户端。

### QB.3 任务定义影响预览核心

- CodeGraph 复核显示直接修改任务生命周期服务会影响约 70 个符号、章节生命周期服务会影响约 40 个符号，因此先建立独立只读预检，没有把未验证的图算法直接嵌入退休事务。
- 新增 `QuestDefinitionImpactAnalyzer`、不可变有界报告和 `AuthenticatedQuestDefinitionImpactQuery`：从服务端权威已发布目录计算直接/传递依赖与章节放置，稳定排序并显式标记截断；越权请求在仓储读取前拒绝，客户端不能提交边或“安全”结论。
- 核心测试覆盖直接与两级传递影响、章节放置、草稿排除及保守 `safeToRetire`。本批只是影响预览底座，尚未接终端协议和退休确认，也未部署、未执行 DDL 或触碰任何服务器。
- CodeGraph 对新分析器复核为 34 个局部受影响符号；完整 Docker 门禁 56 项任务通过，包括 Quest Core/PostgreSQL、UI2 全模块、258 张视觉快照、根 `test`、`assemble` 与 `verifyUi2SelfContained`，`git diff --check` 通过。运行 JAR SHA-256：`240279a4a9965f63bbcfe4c2c35633e4098a3cbdf7b89f0b79d23e3477a44014`。
- 随后完成生产接线：`QuestModule` 使用共享 PostgreSQL 连接安装认证影响查询；管理员定义详情显示直接、间接依赖、章节放置及截断摘要。退役按钮仅在服务端报告明确安全时启用，服务端动作仍再次预检，查询缺失、越权、异常或截断一律拒绝退役。
- 影响摘要复用既有有界 options 编解码，避免改动影响 402 个符号的任务快照顺序协议；关系图及最终安全判断始终由服务端生成。三档窗口、全部主题和影响卡进入纯 Java 视觉审计。
- CodeGraph 复核认证查询组合根影响、任务管理快照 7 个直接下游和 UI 文档 88 个局部符号；完整 Docker 门禁再次通过 56 项任务及 258 张视觉快照，`git diff --check` 通过。最新运行 JAR SHA-256：`a7391ccd8d1348500d9350346610c6ff09bca652f5ca9189317376a02485f8ed`。未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰任何服务器或客户端。
- 完成后语义复核发现 `bq_standard:questcompletion` 奖励也会引用任务；影响分析现同时识别前置集合和 Task/Reward 参数中的 `questUuid`，并记录 `PREREQUISITE / TASK_REFERENCE / REWARD_REFERENCE` 来源。新增测试证明“完成另一任务”的奖励引用同样会阻止不安全退役。
- 上述引用补全后再次执行完整 56 项 Docker 门禁，Quest、PostgreSQL、UI2、258 张视觉快照、根测试、整包和自包含检查全部通过；最终 `git diff --check` 通过，运行 JAR SHA-256：`67f0ad0d5bc151ebdc557420c6cd3da00864d912c4fe96ef1c7bc26acfd28cbd`。

### QB.4 批量退役纯核心与原子服务

- 对照 BetterQuesting `ToolboxToolRemove` 与 `ToolboxToolDelete` 的多选语义，Base 没有吸收客户端修改整库 NBT 后同步的做法；新增 1–128 项有界批量请求、结构化影响报告和服务端原子退役服务。
- 批量影响分析会忽略选中集合内部的前置及 Task/Reward 任务引用，但集合外依赖、已发布章节放置、缺失/非发布目标和结果截断全部阻止退役。越权请求在仓储读取和事务开始前失败。
- `QuestDefinitionBatchRetirementService` 在同一 `QuestTransaction` 内重新读取已发布任务与章节目录、核对所有精确版本，再执行全部生命周期写入；任一 JDBC 写冲突通过异常触发整体回滚，不会像 BQ 客户端包那样留下半批状态。
- CodeGraph 同步后确认批量退役服务影响 53 个符号、扩展后的影响分析器影响 69 个符号；文件级 `affected` 没有推导额外测试，因此显式执行 Quest、PostgreSQL、UI2 与根项目完整门禁。
- Docker 完整 56 项任务通过：全仓 732 项测试（66 项环境条件跳过）0 failure/0 error，258 张视觉快照、`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 均通过。运行 JAR SHA-256：`ebbdf6526210146acc0fb263bd5af2041dd94b0d4de11dc1f2fec16dd4433234`。
- 本段只建立可复用的核心合同，尚未接终端协议或正式管理 UI；下一步为有界批量载荷、管理列表多选、确认与完整影响项浏览。未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰任何服务器或客户端。

### QB.4 批量退役终端载荷与管理多选

- 将批量退役接入正式终端链路：`TerminalQuestBatchRetirePayload` 采用 magic、长度和 2–128 项边界，只传任务
  UUID/精确版本、查询上下文、生命周期和页码；客户端不能提交定义、依赖图、身份或“安全”结论。
- `TerminalService` 由当前认证管理员创建核心请求，`QuestModule` 在共享 PostgreSQL 管理器上安装
  `QuestDefinitionBatchRetirementService`；查询、影响预检、版本核对和逐项退役均在同一事务内完成，异常写冲突
  通过事务回滚阻止半批状态。未启用 Quest 运行时或服务未安装时返回失败快照，不产生写入。
- `QuestManagementVisualDocument` 增加显式“多选退役/取消多选”和选中计数；仅已发布条目可进入选择，单击多选行不
  打开详情，提交后清除选择。小视口保留筛选行并通过结构审计，避免批量按钮把控件画到视口外。
- CodeGraph 已同步（1,014 files / 26,939 nodes / 71,432 edges）。`QuestDefinitionBatchRetirementService`
  影响 29 个符号，`TerminalQuestBatchRetirePayload` 影响 32 个符号；文件级 `affected` 未推导额外测试，
  因此显式执行核心、PostgreSQL、UI2、终端和根项目门禁。
- 最新门禁报告：全仓 735 项测试，0 failure/0 error（66 项环境条件跳过）；`ui2-demo:renderGolden` 生成三档
  视觉 PNG/DrawList，`assemble`、`verifyUi2SelfContained` 与 `git diff --check` 通过。运行 JAR SHA-256：
  `539101997e81f61c19f7495cd6cea8880e60e6443fbf084a6b28598349d15203`。
- 本段未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2。仍待独立批量确认框、完整影响项分页
  与移动/属性批处理；这些完成前不宣称 BQ 替代完成。

### QB.4 批量退役确认浮层收口

- 批量退役按钮不再直接发送破坏性动作；在选中至少两项后打开稳定键
  `quest-admin-batch-confirm`，显示退役数量、服务端依赖/章节/版本复检和单事务说明。
- 模态输入层优先消费 Escape、Enter、遮罩点击和确认区域点击，确认只关闭一次并提交快照中的 UUID/版本；弹窗
  打开期间底层列表与快捷键不会收到输入，选中状态在确认提交后清除。
- `:ui2-terminal:test` 通过（含 Escape/Enter 模态阻断测试）；根 `test assemble verifyUi2SelfContained` 在只读
  预览字体挂载下通过，共 736 项测试、0 failure/0 error，最新运行 JAR SHA-256 为
  `8f0da5b3c23b6ea96ac9e0e26da106f939d0c97432244a189a377b3037ef5268`。
- 当前仍未部署、未执行生产 DDL、未启用 Quest 生产开关。QB.4 剩余工作是完整影响项分页/稳定业务键展示，
  以及批量移动和属性修改；完成前不宣称 BQ 替代完成。

### 2026-09-14 - QC 运行时检测接线收口

- CodeGraph 先复核 `QuestRuntimeService`、`RuntimeQuestFactSink` 与 `QuestGameplayEventHandler`：此前只有定义和
  测试引用，没有生产注册调用，检测事实并未真正进入 PostgreSQL 进度运行时。对 `QuestModule` 与事件处理器执行
  `impact`（分别 67、18 个符号），并以 `callers/callees` 确认缺口后再修改。
- `QuestModule` 现在在共享连接通过 schema 校验后装配 `QuestRuntimeService`、`QuestEvaluationPlanner`、BQ 兼容
  evaluator registry、事实幂等仓储、投影器和引擎；`QuestGameplayEventHandler` 同时注册 FML 总线与 Forge 总线，
  事件与周期观察因此拥有真实生产入口。观察计划由已发布目录生成并以 5 秒窗口缓存，瞬时数据库失败保留上次
  成功计划；重复 `serverStarting` 会先解除旧处理器，避免跨世界重复计数。
- 所有运行时对象仍使用当前服务器身份和 banking source server，客户端、未启用 Quest 配置和缺少共享 JDBC 的环境
  保持失败闭合。新增模块测试断言成功组合根会创建运行时服务，并通过测试覆写阻断静态 Forge 总线初始化。
- Docker 编译与定向 `QuestModuleTest` 通过；随后完整门禁（只读挂载 `/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc`
  为 `UI2_PREVIEW_FONT`）通过：根 `test` 共 738 项、66 项条件跳过，0 failure/0 error，`assemble`、
  `verifyUi2SelfContained` 与 `git diff --check` 均通过。最新运行 JAR SHA-256：
  `70e4f3548b6a78a905910bb991e29f0b939b215c481804df6ccd608b17f4942a`。
- 本轮未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2。下一项为 QC crafting statistics
  模式与扩展检测边界；随后再做库存去重、流体/NBT 矩阵和 BQ 影子差异报告。

### 2026-09-14 - QC crafting statistics 兼容

- 对照 BetterQuesting `TaskCrafting.detect()` 的 `allowCraftedFromStatistics` 分支，补充
  `QuestCraftingStatisticObservation` 与 `QuestObservationPlan` 统计需求集合：只从已发布、明确开启该选项的
  crafting 任务收集实际需求物品，按稳定注册名/meta 去重。
- Minecraft 事实工厂新增原版 `StatisticsFile/objectCraftStats` 绝对累计值读取，使用稳定
  `channel=craft-statistics` 事实；`ItemVectorTaskEvaluator` 对该通道取快照最大值而不是叠加增量，并要求
  `allowCraftedFromStatistics=true`。周期事件每 20 tick 只提交需求集合，避免扫描全物品注册表和无界写入。
- 统计读取仅接受 `EntityPlayerMP`，沿用 FakePlayer、客户端世界、取消事件过滤；1.7.10 原版按 Item ID 聚合导致
  meta 维度不完全等价，已在能力矩阵中标记为影子对照差异，不误报为完全兼容。
- 运行时复核发现请求规划器必须与进度仓储共享同一个 `JdbcQuestConnectionManager`，否则规划查询会脱离事实
  事务读取。本次已将 runtime repository、transaction、definition catalog 与 completion query 收口到同一管理器，
  保证“规划—归约—奖励资格”处于同一 PostgreSQL 事务边界。
- `:galaxy-quest-core:test` 通过（新增观察计划与绝对统计快照测试）。随后在只读预览字体挂载下完成完整 Docker
  门禁：`:galaxy-quest-core:test`、`:galaxy-quest-postgres:test`、`:ui2-core:test`、`:ui2-lab:test`、
  `:ui2-terminal:test`、`:ui2-demo:test`、`:ui2-demo:renderGolden`、根 `test`、`assemble` 与
  `verifyUi2SelfContained` 均通过；根共 738 项测试、66 项条件跳过，0 failure/0 error，`git diff --check`
  通过。最新运行 JAR SHA-256：`8b97c0fc938e0db32d93742097b07d17d47e685499ee34bdca466299e706f13f`。
- 未部署、未执行生产 DDL、未启用 Quest 生产开关；下一项为事件取消/FakePlayer 边界、扩展类型注册表、库存去重、
  流体/NBT 矩阵和 BQ 影子差异报告。

### 2026-09-14 - QC 运行时扩展类型目录

- CodeGraph 在编辑前复核 `TaskEvaluatorRegistry`、`QuestEditorTypeRegistry`、`QuestGameplayEventHandler` 与
  `QuestModule` 的调用链；确认此前标准 evaluator 与编辑器描述分别构造，无法让 GTNH 专用类型以同一合同进入
  求值和管理校验。
- 新增纯 Java `QuestRuntimeTypeExtension` 与 `QuestRuntimeTypeRegistry`：以标准 BQ 兼容类型为基线，组合根可
  显式注册成对的 evaluator/编辑器描述；扩展 ID、重复类型、空描述均在组合阶段拒绝。QuestModule 现在让运行时、
  草稿编辑和章节复制共享同一目录，未知类型继续失败闭合。
- 新增核心目录组合与重复注册测试，并在 QuestModule 测试断言生产组合根安装标准目录。未引入 ServiceLoader、反射、
  BQ 类或额外 Mod 依赖；当前没有外部扩展提供者，GTNH 专用类型仍需后续明确语义后显式加入。
- CodeGraph 已同步并执行 `impact QuestRuntimeTypeRegistry`（24 个符号）、`impact QuestRuntimeTypeExtension`
  （29 个符号）、`impact QuestModule`（79 个符号）及实际文件 `affected`；affected 未推导额外测试，故显式执行
  Quest Core、BQ importer 与 QuestModule 定向测试，全部通过。
- 本轮未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2 或客户端。下一项继续处理事件取消/
  FakePlayer 边界、库存快照去重、流体/NBT 兼容矩阵与影子差异报告。

### 2026-09-14 - QC 库存观察去重与服务端玩家边界

- CodeGraph 复核 `QuestGameplayEventHandler`、`MinecraftQuestFactFactory` 的调用链后，收紧事实入口为真实
  `EntityPlayerMP` 且排除 `FakePlayer`、客户端世界；事件取消检查继续在各可取消事件入口执行。
- 新增纯 Java `InventoryObservationEntry` 与 `InventoryObservationCanonicalizer`。背包快照按注册名、meta、
  OreDict 和 portable NBT 合并相同条目，保持首次出现顺序并对数量溢出饱和；不同 NBT 保持独立，避免改变 BQ
  retrieval 的匹配/分配语义。Minecraft 事实工厂现在使用该核心算法，不再维护第二套去重逻辑。
- 新增核心去重、顺序、NBT 隔离和溢出测试；`QuestGameplayEventHandler` 与事实工厂的 CodeGraph `impact` 分别
  覆盖 33/54 个符号，文件级 `affected` 未推导额外测试，故显式执行 Quest Core、根编译与 BQ importer 定向门禁。
- Docker 定向门禁通过：`:galaxy-quest-core:test`、`:galaxy-quest-bq-importer:test`、根 `testClasses`，0 failure。
  本轮未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2 或客户端。下一项为流体/NBT 兼容矩阵
  与 BQ 影子差异报告。

### 2026-09-14 - QD/QE 进度影子差异报告基础

- 新增 `BqShadowProgressAnalyzer`、`BqShadowDifference`、`BqShadowDifferenceReport` 与稳定关系枚举；对只读
  BQ 导入进度和 Base `QuestProgressSnapshot` 按玩家/任务对账，区分 `IDENTICAL`、`BASE_AHEAD`、`BQ_AHEAD`、
  `DIVERGENT`、单侧记录及 `INCOMPARABLE`，并给出完成状态、任务键/类型/向量的具体原因。
- 报告固定最多 4096 行、稳定 UUID 顺序、重复玩家/任务拒绝；没有合并、写库、发奖或接受客户端结论。它是未来
  BQ/Base 并行事实计算报告的可复用证据层，不能替代尚未完成的实时双算接线。
- CodeGraph 在实现前已复核 `BqProgressConflictAnalyzer`、`BqProgressMapper`、`QuestProgressSnapshot` 与
  `QuestEngine` 的关系；文件同步后显式执行 importer 定向测试。`:galaxy-quest-bq-importer:test` 通过，0 failure。
- 本轮未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2 或客户端。下一项仍为流体/NBT 兼容
  矩阵、事件边界及把影子报告接入受控管理诊断入口。

### 2026-09-14 - QC/QE 全量门禁收口

- 在类型目录、服务端玩家边界、库存快照去重和影子差异基础完成后，重新执行 CodeGraph `status/sync`、主要
  符号 `impact` 与实际文件 `affected`，确认没有额外测试被漏选。
- Docker 完整门禁通过：`:galaxy-quest-core:test`、`:galaxy-quest-postgres:test`、`:galaxy-quest-bq-importer:test`、
  `:ui2-core:test`、`:ui2-lab:test`、`:ui2-terminal:test`、`:ui2-demo:test`、`:ui2-demo:renderGolden`、根
  `test`、`assemble`、`verifyUi2SelfContained`；共 744 项测试、66 项条件跳过，0 failure/0 error。
- `git diff --check` 通过；本次运行 JAR SHA-256：
  `12911d81008b2cd51b5cf55a1a1a6aeb6b5f308143ff7baec08ed650339d18b9`。
- 本轮仍未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2 或客户端。当前可继续推进流体/NBT
  兼容矩阵、事件取消边界的细粒度测试，以及把影子报告接入受控管理员诊断入口。

### 2026-09-14 - QE 影子对账边界与流体快照规范化

- CodeGraph 先执行 `status/sync`，并对 `BqShadowProgressAnalyzer`、`FluidObservationCanonicalizer` 与
  `MinecraftQuestFactFactory` 执行 `impact/affected`。影子分析器现在拒绝 PARTY/TEAM/PUBLIC 快照进入玩家对账，
  防止把群体主体 UUID 误当成玩家；任务类型缺失、向量宽度不一致和任务单侧缺失统一标记为
  `INCOMPARABLE`，不再错误归类为某一侧领先。
- 新增纯 Java `FluidObservationEntry` 与 `FluidObservationCanonicalizer`。流体库存事实按流体名＋portable NBT
  合并，保留首次出现顺序和诊断用原始 NBT，数量使用饱和加法；Minecraft 适配器改用该核心算法，匹配器的
  requirement allocation 语义不变。
- 新增影子组主体、任务类型/向量形状及流体名称/NBT/溢出测试。Docker 定向门禁后又完成 Quest Core、
  PostgreSQL、BQ importer、UI2、终端、视觉导出、根 `test`、`assemble` 和 `verifyUi2SelfContained` 全量门禁：
  共 748 项测试、66 项环境条件跳过，0 failure/0 error；`git diff --check` 通过。最新运行 JAR SHA-256：
  `11748467d73adcf615c77f3f97774081ae1071b120acb9a4bf9d7fd4523277d9`。
- 本轮未部署、未执行生产 DDL、未启用 Quest 生产开关，未触碰 Lobby/S1/S2。影子报告仍是只读证据层，下一步
  可接入受控管理员诊断入口，并继续补 fluid/NBT 行为矩阵与实时 shadow 计算接线。

### 2026-09-14 - UI2 与跨服任务平台稳定检查点收口

- 冻结功能范围并完成 Git 审计：558 个非构建未跟踪文件按 UI2、Quest Core、PostgreSQL、BQ importer、
  Minecraft/终端适配、测试、许可证和文档分类；四个新增子项目补齐独立 `build/` 忽略规则。代码、构建、
  许可证和测试以明确路径暂存，没有使用无审计的 `git add -A`。
- CodeGraph 已同步并复核 UI2 Screen/Container 双宿主、`QuestModule`、`QuestRuntimeService`、
  `TerminalService` 与 `BqShadowProgressAnalyzer`。全仓搜索确认正式生产源码不再引用旧 Canvas/Panel 工厂；
  BQ 影子分析器没有生产调用者，仍是离线只读证据层。
- Docker 门禁通过 56 项 Gradle 任务：三个 Quest 模块、四个 UI2 模块、258 张终端视觉快照、根 `test`、
  `assemble` 和 `verifyUi2SelfContained` 均成功；测试报告合计 748 项、66 项环境条件跳过、0 failure、
  0 error，`git diff --check` 通过。
- 运行 JAR 为 `build/libs/jsirgalaxybase-3ca9376-main+3ca937653a-dirty.jar`，SHA-256 为
  `11748467d73adcf615c77f3f97774081ae1071b120acb9a4bf9d7fd4523277d9`。内容审计确认包含 UI2、Quest Core
  和 PostgreSQL 运行代码，不包含 BQ importer、BetterQuesting 类、Gson 导入 CLI、BQ GUI/Packet/JSON 存储。
- 代码稳定检查点提交为 `4f51c92`（`feat: 完成UI2终端与跨服任务平台稳定检查点`）。本次没有部署、没有执行
  PostgreSQL migration、没有启用 Quest 生产开关、没有停用 BetterQuesting，也没有触碰 Lobby/S1/S2/客户端。
- 后续唯一权威进度见 `betterquesting-capability-absorption-matrix-2026-09-13.md`。PARTY/TEAM/PUBLIC 奖励归属、
  管理诊断与受控修复、幂等迁移/回滚、实时影子双算和自动切换证据均明确未完成；历史各段“下一步”只保留
  为当时记录，不再作为当前计划依据。

### 2026-09-16 - PARTY/TEAM/PUBLIC 跨服主体与逐成员奖励归属收口

- PostgreSQL 新增稳定 PARTY/TEAM/PUBLIC 主体、成员关系、`OWNER / ADMIN / MEMBER`、乐观修订、离队、移除、
  所有权转让与归档合同；玩家每种主体类型最多一个活跃成员关系。任务定义的 scope 由服务端选择唯一进度
  主体，事实不会因多重成员关系重复推进。
- 组任务完成或进入重复周期时，在同一事务中冻结当时活跃成员并生成逐玩家 entitlement。后来加入者不补领
  旧周期，离队者保留既得资格；离线交付进入延期而不消耗失败预算。物品、XP、计分板和命令交付统一解析为
  具体 PLAYER 收件人，禁止把组 UUID 当成玩家。
- 领取批次增加服务端 `RewardClaimTarget`，精确包含原进度主体与周期。终端按认证玩家的
  `recipient_player_id` 显示最早尚可领取批次，即使已经离队仍可操作；领取和 Choice 不接受客户端玩家身份，
  并避免不同队伍恰好同周期时串领。终端详情显示奖励来源 scope 与周期。
- 命令奖励默认拒绝，只接受显式根命令白名单；空命令、控制字符、超长命令和前缀伪装均拒绝。任意命令的
  execute/marker 崩溃间隙仍不能承诺 exactly-once，文档明确要求仅配置自身可幂等的审计命令。
- 新增的 `20260914_001_add_cross_server_quest_platform.sql` 是真实增量迁移而非全量 DDL 复制：旧 PLAYER
  entitlement 会回填收件人，旧非 PLAYER 且无法推导收件人的数据失败闭合；唯一键、消费主体检查及失败计数
  均可重复应用。迁移在隔离 PostgreSQL 16 上从旧提交 DDL 连续执行两次通过；未对生产数据库执行。
- CodeGraph 同步后对 `AuthenticatedParticipantAdministrationService`、`RewardClaimTarget`、
  `JdbcRewardClaimRepository` 和 `JdbcQuestCenterQuery` 执行 impact；奖励目标影响 30 个符号，领取仓储影响
  20 个符号，任务中心查询影响 48 个符号。显式覆盖 Quest Core、PostgreSQL、Minecraft 奖励适配、终端网络、
  UI2 主体管理和根项目回归。
- 隔离 PostgreSQL 16 的真实集成测试通过，覆盖离队奖励可见、后加入者无旧资格、同周期不同主体隔离、
  成员冻结、Choice、租约交付和延期重试。完整 Docker 门禁通过 56 项 Gradle 任务；当前报告合计 776 项测试、
  52 项环境条件跳过、0 failure/0 error，258 张视觉快照、`assemble`、`verifyUi2SelfContained` 与
  `git diff --check` 均通过。运行 JAR 为
  `build/libs/jsirgalaxybase-a8ca03e-main+a8ca03eb8d-dirty.jar`，SHA-256：
  `fb9cc0983b7c6ec4aa58f88cd221469d245128a662bd664a91eb9a41fb158540`。
- 本批未部署、未执行生产 DDL、未启用 Quest 生产开关、未停用 BetterQuesting，也未触碰 Lobby/S1/S2/客户端。
  后续仍是管理员诊断与受控修复、迁移 dry-run/回滚、实时影子双算及正式切换证据；这些不属于本批完成声明。
