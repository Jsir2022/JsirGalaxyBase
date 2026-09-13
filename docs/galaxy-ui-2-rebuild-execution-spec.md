# Galaxy UI 2：独立内核、Demo 与终端重构执行规格

日期：2026-09-05

## 目标

根据仓库内固定版本的 Vue 官方编程手册，逐步实现一套适合 Minecraft 1.7.10 / Forge 的现代 Java UI 框架。框架的
组件、状态、布局、输入和 DrawList 应尽可能脱离游戏独立编译和自动测试；只有 Minecraft Screen、OpenGL、字体纹理、
物品渲染和原生 Container/Slot 交互留在游戏适配层。

第一阶段不直接重写整个终端。先完成独立内核和可运行 Demo，再用资产中心作第一条真实垂直链，最后逐页替换旧框架。

## Batch A 实施状态（2026-09-05）

Batch A 已完成：

- `ui2-core` 已作为纯 Java 8 子模块建立，核心 class 文件版本为 52；
- `ui2-demo` 已能以 Java2D 独立渲染五类 UI Lab 页面；
- `renderGolden` 已为 `350x193` 与 `620x340` 导出 10 张 PNG 和 10 份 DrawList 快照；
- 根 Mod 以普通 project dependency 编译核心，并通过文件型 `shadowImplementation` 把核心合入 GTNH 最终运行 JAR；
- 最终运行 JAR 包含 UI2 核心，不包含 Java2D Demo；
- 现有 Canvas、终端路由和业务页面未修改。

Batch A 后不再扩张 Java2D 为生产后端。

## Batch B 实施状态（2026-09-05）

Batch B 已完成平台接入，但仍未迁移正式业务页面：

- 新增纯 Java `ui2-lab`，五页场景、Store、Action、假数据和布局由 Java2D 与 Minecraft 共用；
- `UiRuntime` 已统一重建与 type+key 对账、布局、输入树、DrawList 缓存、失效和卸载；核心键盘合同不再出现 LWJGL 数字键码；
- Minecraft 适配层已提供 `MinecraftUiRenderer`、`ModernScreenHost`、`ModernContainerHost`、`NativeSlotBridge` 与 GL 状态保护；
- UI 几何始终使用逻辑 GUI 坐标，GUI Scale 只参与 scissor 和字形物理密度换算；
- 字体最终实现已调整为只适配 Minecraft 当前 `FontRenderer`，跟随语言系统与资源包；不再内置 Noto、维护异步栅格线程或字形 Atlas；
- 客户端 `F8` 打开普通 UI Lab；Inventory 页按 `C` 进入不会向服务端发送窗口操作的本地 Container Lab；
- 旧 Canvas、正式终端路由、资产中心、服务端合同和数据库均未修改。

下一批为 Batch C：复用现有资产 Container、Vault、背包、Bay 与 Cell 合同，只把资产中心客户端视图迁移到 UI2。

相关基线：

- `galaxy-ui-2-modern-java-framework.md`：总体架构和视觉方向；
- `galaxy-ui-2-vue-reference-map.md`：Vue 概念到 Java/Forge 的映射；
- `../Reference/VueDocs/README-JSIR.md`：官方英文/中文手册版本、阅读顺序和许可。

## “参考 Vue”与“复制 Vue”的差异

本项目参考的是编程模型，不是浏览器运行时。

| Vue / Web | Galaxy UI 2 | 原因 |
|---|---|---|
| `.vue` SFC 与模板编译器 | 类型安全 Java builder/factory | 不引入 Node、JavaScript 或运行时模板解析 |
| JavaScript Proxy 响应式 | 不可变 State + Reducer + Selector | 依赖明确、容易测试，不依靠隐式属性追踪 |
| DOM / Virtual DOM | `UiElement` 描述树 + `UiNode` 运行树 | Minecraft 没有 DOM，但仍需要 type + key 保留节点身份 |
| CSS Flex/Grid | `Constraints`、Row、Column、Stack、Grid | 布局必须适应 GUI Scale，又不能依赖浏览器排版 |
| CSS transition | Motion timeline + DrawCommand 参数 | 每帧渲染，不生成 CSS class |
| 浏览器事件 | 捕获、目标、冒泡和 Pointer Capture | 对接 LWJGL/Screen 鼠标键盘事件 |
| Teleport 到 DOM 节点 | 根 Overlay Layer | 统一 Popup、Tooltip、Toast 和模态层级 |
| KeepAlive | 有白名单的 RouteStateCache | 只保留滚动/筛选等体验状态，不缓存业务真源 |
| Web Renderer | Minecraft/GL Renderer 与 Java2D Demo Renderer | 同一 DrawList 支持自动预览和游戏实装 |

Vue 文档不能成为业务规则来源。账户、订单、资产、产权和传送仍使用服务端 Snapshot、服务端身份和现有业务服务。

## 相对当前 Canvas 框架的重构差异

| 当前实现 | UI2 目标 |
|---|---|
| 可变 `GuiPanel` 同时承担状态、布局、输入和绘制 | Component 描述界面，Store 管状态，Layout 算几何，Input 分发事件，Renderer 只绘制 |
| 页面和控件直接写绝对坐标 | 两阶段 `measure/layout` 与约束布局 |
| Panel 直接调用 `drawRect`、FontRenderer 或 GL | 组件只能生成 DrawList；只有 Renderer/Bridge 可接触平台 API |
| 颜色、间距、边界散落在业务类 | Theme Token 是唯一视觉常量来源 |
| 业务字段和控件状态混在页面对象 | 服务端 Snapshot、本地 View State 和瞬时交互状态明确分层 |
| 输入按 Panel 顺序逐个试探 | 捕获/目标/冒泡、焦点树、Pointer Capture 和模态优先级 |
| Popup、Tooltip、Slot 分层依靠宿主手工协调 | Runtime 统一 `base -> native slot -> tooltip -> modal -> toast` 层级 |
| 原生像素字体被 GUI Scale 放大 | 独立字体服务与抗锯齿字形 atlas；原版字体仅作最终 fallback |
| 每个页面重复处理 loading/empty/error | 公共异步状态组件和统一 Effect 生命周期 |
| 改 `GuiPanel` 会波及大量终端和测试符号 | 新框架独立包落地，按 Screen 纵向迁移，旧页在迁移期保持可运行 |

CodeGraph 当前确认：`CanvasScreen` 和 `CanvasContainerScreen` 分别由两个终端宿主基类承接；`GuiPanel` 已进入 Runtime、
滚动、Popup、地产、市场和测试链。因此禁止在旧接口上做破坏性“就地升级”。

## 构建与模块边界

目标结构：

```text
ui2-core/                         纯 Java 8，可独立编译
  component / element / node
  state / selector / effect contract
  layout / geometry
  input / focus / modal
  theme tokens
  draw-list contract
  motion clock

ui2-lab/                          纯 Java 8，共享 Lab 场景、Store 与假数据
  five-page component document
  inventory external regions

ui2-demo/                         普通 Java 应用，可独立运行
  Java2D renderer
  fake resources / fake item icons
  PNG golden exporter

src/main/java/.../client/ui2/    Minecraft 适配层
  MinecraftUiRenderer
  GlyphAtlasFontService
  ModernScreenHost
  ModernContainerHost
  NativeSlotBridge
  ItemRendererBridge
```

依赖方向只能是：

```text
ui2-demo ----> ui2-lab ----> ui2-core <---- Minecraft adapter <---- terminal pages
```

`ui2-core` 不得依赖 Minecraft、Forge、LWJGL、AE2、ModularUI、BetterQuesting、数据库或网络包。它使用标准 Java 8 API，
不用 record、sealed class、Lombok 或运行时反射；即使主 Mod 使用 Jabel，独立内核仍必须通过普通 Java 8 编译。

优先实现为 Gradle 子模块：

- `./gradlew :ui2-core:test`：只编译和测试纯内核；
- `./gradlew :ui2-demo:test`：测试 Demo renderer 和场景；
- `./gradlew :ui2-demo:run`：打开独立 Demo；
- `./gradlew :ui2-demo:renderGolden`：无窗口导出固定尺寸 PNG；
- 根 Mod 构建依赖 `ui2-core`，并把内核 class 合入最终 JAR，不要求玩家安装第二个库。

实施第一步必须用一个空模块验证 GTNH Settings/Convention 插件与多项目构建兼容。如果插件阻止子模块，回退方案是根项目
中的独立 `ui2Core` source set，并以空 Forge classpath 单独编译；不能因为构建插件限制而放弃依赖隔离。

## UI2 内核最小合同

### 组件树

- `UiComponent<P>`：纯函数式地把 Props 映射为 `UiElement`；
- `UiElement`：不可变的 type、key、props、children 描述；
- `UiNode`：Runtime 节点，保存边界、焦点、滚动、动画和资源句柄；
- `UiKey`：动态行、路由、页签和槽位区域的稳定身份；
- `UiSlot<P>`：允许壳组件注入内容，作用域数据必须显式声明；
- `UiContext`：只读 Theme、Locale、Density、Clock 和 ResourceResolver。

### 状态与副作用

- `UiStore<S, A>`：页面唯一客户端 State；
- `UiReducer<S, A>`：纯状态转换；
- `UiSelector<S, R>`：缓存派生结果；
- `UiEffect<A>`：网络、声音、路由、剪贴板和资源任务；
- 服务端 Snapshot 通过 Action 进入 Store，Component 和 Renderer 不得直接发包。

### 布局

- `Constraints`、`Size`、`Insets`、`Alignment`；
- Row、Column、Stack、Flex、Grid、ScrollView；
- `measure()` 后才能 `layout()`，`draw()` 不得改变几何结构；
- 使用逻辑尺寸与 density，不能用 GUI Scale 直接乘控件宽高；
- 最小验收工作区是 `350x193` 和 `620x340`。

### 输入

- 鼠标捕获、目标和冒泡；
- Pointer Capture 保证拖动释放回到原目标；
- 独立 Focus Manager，支持 Tab、Shift+Tab、Enter、Space 和 Escape；
- Modal 打开时阻断底层 UI 与原生 Slot；
- Tooltip 永不抢输入；
- 所有输入测试使用合成事件，不依赖 LWJGL。

### 绘制

- Component 只生成 `DrawCommand`：surface、border、text、icon、image、clip、transform；
- `DrawList` 保持稳定顺序和 layer；
- Renderer 负责批处理、裁剪、圆角、阴影及平台状态恢复；
- Minecraft Renderer 和 Java2D Renderer 消费同一个 DrawList；
- Minecraft 物品和 Slot 通过占位 command/bridge 插入，不让纯内核引用 `ItemStack` 或 `Slot`。

## 能自动测试到什么程度

### 完全独立、必须自动化

- Component 重建、type + key 对账、节点保留与卸载；
- Store、Reducer、Selector 缓存和 Effect 调度；
- 正负坐标、极小/极大约束、Row/Column/Grid/Flex/Scroll 布局；
- 命中、Hover、Pressed、Focus、拖拽、滚轮、Escape 和 Modal 阻断；
- Theme Token 完整性、禁用态和非颜色状态表达；
- DrawList 层级、Clip 栈平衡和命令稳定性；
- 动画固定时钟、reduced-motion 与资源取消；
- `350x193`、`620x340` 和中英文长文本场景。

### 可用独立 Demo 自动验证

- Java2D 生成固定尺寸 PNG golden；
- 卡片、按钮、页签、输入框、表格、滚动、弹窗、Toast 的布局和基本视觉；
- 深色主题、间距、圆角、颜色、禁用/选中/危险状态；
- 页面改变 State 后的前后帧差异；
- Demo 截图只用于开发产物对比，不替代实际 Minecraft 字体和 OpenGL 验证。

Golden 测试不能只做逐像素完全相等；应同时保存结构化 DrawList snapshot，并对 PNG 使用尺寸、关键采样点或允许容差的
差异阈值，避免不同 JDK 字体栅格化造成无意义失败。

### 最终仍需游戏内验证

- OpenGL blend、scissor、depth、texture、矩阵状态是否正确恢复；
- 抗锯齿字形 atlas 在真实 GUI Scale 下的清晰度与 CJK fallback；
- Minecraft ItemRenderer、原生 Tooltip 和 Z 层；
- Container/Slot 的拖放、Shift-click、数字键、双击和服务端同步；
- 页面切换、资源释放和真实帧率；
- 其他 Mod 对渲染状态和输入的兼容影响。

自动化目标不是取消人工验收，而是把人工范围压缩为平台适配与审美判断。

## 独立 UI Lab / Demo 画面

Demo 使用同一 Component、State、Layout、Theme 和 DrawList，只替换 Renderer。默认展示一个现代深色终端样板：

```text
+------------------------------------------------------------------+
| Galaxy UI Lab         350x193 / 620x340       Theme  Motion  FPS |
+----------+-------------------------------------------------------+
| Overview | Typography / spacing / status cards                   |
| Controls | Buttons / inputs / select / tabs / focus              |
| Data     | Table / list / pager / loading / empty / error        |
| Overlay  | Tooltip / context menu / dialog / toast               |
| Inventory| Vault grid / player grid / Bay / virtual Cell preview |
+----------+-------------------------------------------------------+
```

Demo 必须支持：

- 一键切换两种验收尺寸；
- 中英文、长文本、大数字和缺字样例；
- 鼠标、键盘焦点、滚动、弹窗和禁用状态；
- 正常、加载、空、失败四种页面状态；
- 打开/关闭动画与 reduced-motion；
- 显示布局边界、命中区、重绘区和 DrawCommand 数量的调试层；
- 导出 PNG 和结构化 DrawList snapshot。

`Inventory` 页在独立 Demo 中使用假图标和假槽位，仅验证布局、层级和模态阻断；真实物品与原生 Slot 只在游戏内 UI Lab
页面验证。

## 分阶段实施

### Stage 0：构建探针与行为基线

- 建立空的 `ui2-core` / `ui2-demo`，证明能独立 test、run、打包并合入 Mod；
- 冻结旧终端的路由、Popup、滚动、Container 输入和两档布局测试；
- 不修改任何业务页面。

完成门槛：四个独立 Gradle 命令可运行，根 `test` 和 `assemble` 不回归。

### Stage 1：纯 Java 内核

- 完成 geometry、constraints、element/node/key、reconciler、store、selector、input、theme 和 DrawList；
- 使用 RecordingRenderer 和合成输入完成单元测试；
- 禁止出现任何 Minecraft/Forge import。

完成门槛：核心测试无需下载或启动 Minecraft 客户端即可执行。

### Stage 2：Java2D Demo

- 实现 UI Lab 五个样板页、PNG 导出和结构化 golden；
- 先审核整体视觉、字体尺度、间距、颜色和交互反馈；
- 此阶段不连接真实账户、市场或资产服务。

完成门槛：两档尺寸无越界，可运行 Demo 和无头 golden 测试同时通过。

### Stage 3：Minecraft 双宿主适配

- 实现 `ModernScreenHost` 与 `ModernContainerHost`；
- 实现 Minecraft DrawList Renderer、字体 atlas、物品和原生 Slot bridge；
- 增加游戏内 UI Lab，不接生产入口。

完成门槛：同一 Demo 组件在 Java2D 和游戏内结构一致；原生 Slot 输入矩阵通过。

### Stage 4：资产中心垂直迁移

- 复用现有服务端 Container、Base Vault、背包、Bay、Cell 浏览和路由合同；
- 只替换资产中心客户端视图；
- Cell 不存在时不显示 Cell 内容，左右分区沿用已经人工验证的产品决策。

完成门槛：自动测试通过并完成人工视觉/槽位验收后，资产中心成为 UI2 第一张正式页面。

### Stage 5：终端壳与普通页面迁移

- App Shell、导航、TopBar、Popup、Toast；
- 首页、通知、银行、ServerTools；
- 标准市场、订单与资产、定制市场、兑换；
- 地产地图最后迁移。

每迁完一页就删除该页旧组件；不设置永久双实现开关。

### Stage 6：删除旧框架

- CodeGraph 确认 `client.gui.framework`、旧 Theme 和旧 Panel 零生产调用；
- 删除旧框架、adapter 和只为旧框架存在的测试；
- 全量测试、assemble、客户端人工矩阵后部署。

## 首批不得做的事情

- 不直接铲除当前可运行的 Canvas 框架；
- 不在内核尚未稳定时迁移市场或地产；
- 不嵌入 Chromium、WebView、Node 或 JavaScript 引擎；
- 不写 XML/JSON/HTML 模板 DSL；
- 不让 Java2D 成为生产渲染器；
- 不把客户端 State 变成业务真源；
- 不为了现代外观重写现有服务端网络和 Container 合同；
- 不以一张静态效果图代替焦点、输入、资源释放和性能测试。

## 验收与决策点

每个 Stage 独立提交并满足：

- `git diff --check`；
- 对应 `:ui2-core:test` / `:ui2-demo:test`；
- 根项目定向测试、全量 `test` 和 `assemble`；
- CodeGraph `impact/affected` 选择回归范围；
- 文档与 `WORKLOG.md` 同步。

只有三个明确的人工作业点：Stage 2 的 Demo 视觉确认、Stage 3 的游戏内 UI Lab 平台确认、Stage 4 的资产中心真实操作确认。
如果 Java2D Demo 因运行环境不可用，保底仍可用 DrawList snapshot 和游戏内 UI Lab；但不能跳过纯内核自动测试。

## Stage 3.1：Qz 成熟算法加固结果

- Modal 重建以稳定 key 保留有效焦点和捕获，遮罩、按钮与 Escape 均只关闭一次；已修复 Modal 文字误入
  CONTENT 层导致后续 MODAL 命令触发 DrawList 顺序异常的崩溃。
- Minecraft Renderer 已拆为 DrawList 解释器、Shape、Text、Image、ClipStack 与 GlStateGuard；圆角采用动态
  细分、实色内部和一物理像素 coverage fringe，边框采用填充环。
- 字形任务具有 generation、去重与每帧 48 个上传预算；过期结果拒绝，失败 Atlas generation 整体回滚。
- UI Lab 概览显示字体/Atlas/命令/裁剪/frame/render 诊断；`350x193`、`620x340` 的共享场景继续由
  Java2D golden 和 Minecraft 双宿主共同消费。
- 构建门禁验证生产 class 不引用 Qz 包或 Mod ID，且最终 JAR 包含 Qz 来源清单与 MIT/LGPL 文本。

F8 UI Lab 的视觉、Modal 和库存页人工验收已经通过；Stage 4 已于 2026-09-06 完成自动迁移与部署，当前仅等待
正式资产中心的真实拖放、Shift-click、数字键、双击、Cell 浏览、Tooltip、Modal 和返回路由人工验收。

## Stage 4 自动实施结果

- UI Lab 与正式资产中心共同使用 `ComponentUiDocument`、`UiWidgetRegistry` 和 `StandardWidgets`，不再由 Lab
  手写另一套 DrawList 与裸坐标命中逻辑。
- `TerminalAppShell` 同时服务普通 Screen 与 Container 宿主；资产中心采用
  `AssetCenterUiState -> AssetCenterAction -> AssetCenterReducer -> AssetCenterEffect -> AssetCenterDocument`
  的单向链路，只有 Effect 可以发送资产业务网络请求。
- `NativeSlotBridge` 继续定位 Vault、Bay 与背包原生 Slot；`NativeItemBridge` 只在 Minecraft 适配层绘制虚拟
  Cell 内容的真实 `ItemStack`。UI2 Core 仍不依赖 Minecraft 类型。
- 旧资产 `AssetContentPanel`、`TerminalAssetCenterContentLayout` 与 `TerminalAssetCenterLayout` 已删除；服务端
  Container、Snapshot、数据库和业务真源没有变更。
