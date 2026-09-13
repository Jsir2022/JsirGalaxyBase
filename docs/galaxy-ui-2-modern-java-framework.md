# Galaxy UI 2：以 Vue 思想重建现代 Java 终端 UI 框架

日期：2026-09-05

## 决策摘要

JsirGalaxyBase 下一代终端 UI 采用“Vue 式架构、Minecraft 原生渲染”的路线：借鉴 Vue 的组件树、Props、事件、
Slot、响应式派生状态和单向数据流，用 Java 重新设计一套现代 UI 框架；不把 Vue、JavaScript、DOM、CSS 或 Chromium
嵌入 Minecraft。

现有 `client.gui.framework` 最终删除，不作为 Galaxy UI 2 的长期兼容层。删除顺序固定为：新框架独立建成、资产中心
完成纵向验证、终端全部页面迁移、回归通过，最后一次性删除旧框架。已有 Git 提交提供回滚能力，但不能替代迁移期间的
可编译基线、行为对照和逐页验收。

首轮只重建 UI 基础设施，不修改市场、银行、地产、仓储、ServerTools 的服务端合同和业务规则。

## 对“按 Vue 用 Java 重写”的准确理解

可以借鉴：

- 组件是独立、可组合的界面单元；
- Props 向下传递只读输入；
- Event / Action 向上表达用户意图；
- Slot 允许壳组件接纳业务内容；
- State 是当前视图的单一真源；
- Computed 只从 State 派生，不保存第二份可漂移状态；
- Effect 负责网络请求、声音等副作用，不放在绘制函数中；
- 带稳定 Key 的节点在重建组件树时保留焦点、滚动和动画身份；
- 页面使用相同设计 Token 和组件合同，而不是复制绘制代码。

不能直接翻译：

- Vue 源码建立在 JavaScript Proxy、DOM、CSS、浏览器事件、虚拟 DOM 和编译器之上；Java/Minecraft 没有这些运行时；
- Minecraft 每帧仍要重绘可见 UI，虚拟 DOM diff 不能像浏览器一样避免像素绘制；它在这里主要用于保留组件身份、减少
  重复布局和隔离状态，而不是复制浏览器 DOM 更新算法；
- CSS Flex/Grid、字体排版、焦点系统、裁剪、动画和无障碍语义都需要在 Java 框架中明确实现；
- 若以后确实逐段派生 Vue MIT 源码算法，必须在文件中保留 SPDX、来源版本和第三方许可证说明；默认优先根据公开合同
  自主实现，不做机械源码翻译。

## Forge、Minecraft 与自有框架的边界

Forge/Minecraft 提供的是 GUI 宿主和底层能力，不提供浏览器式 Canvas/DOM/CSS。当前项目中的 `CanvasScreen`、
`CanvasSceneRuntime`、`GuiPanel` 等均为 JsirGalaxyBase 自有抽象，不是 Forge API。

| 平台提供 | Galaxy UI 2 必须提供 |
|---|---|
| `GuiScreen` / `GuiContainer` 生命周期 | 组件树、装配、重建和资源释放 |
| 原生 `Container`、`Slot`、物品与光标语义 | 普通 UI 与原生 Slot 的统一分层和输入优先级 |
| 窗口尺寸、GUI Scale 和原始输入回调 | 约束布局、断点、焦点、捕获、拖拽和快捷键 |
| `TextureManager`、`Tessellator`、GL11 上下文 | DrawList、裁剪栈、层级栈、圆角、阴影和状态恢复 |
| `FontRenderer` 基础文字能力 | 现代抗锯齿字体、CJK fallback、字号和数字排版 |
| 原生物品、Tooltip 与槽位同步 | 现代卡片、按钮、表格、弹窗、Toast 和虚拟物品网格 |

Forge 官方文档也将 Screen 描述为输入和绘制宿主，将 Container/Menu 描述为库存数据的交互视图；矩形、字符串、纹理、
物品和 Tooltip 是底层绘制能力。Galaxy UI 2 在这些能力之上搭建完整应用框架，不能假设平台已经提供现代组件系统。

## 现有框架评估

CodeGraph 当前结果：

- `GuiPanel` 影响约 558 个符号；
- `TerminalShellFrame` 影响约 255 个符号；
- `CanvasSceneRuntime` 影响约 98 个符号；
- `TerminalHomeLayout` 影响约 60 个符号；
- `GuiScene` 已被市场、银行、地产、ServerTools、通知、Popup 和资产中心广泛引用。

现有双宿主验证了一个必须保留的原则：普通页面与原生 Container 页面应共享 UI 框架，但不能把全部页面强行变成
Container。现有框架的主要问题不是这个原则，而是抽象层级仍太低：业务组件直接绘制、硬编码颜色和坐标、依赖原生像素
字体，并由可变 Panel 树同时承担布局、状态、输入和绘制。

因此新框架可以借鉴已经验证过的双宿主行为，但不得依赖旧 `GuiPanel`、`GuiScene`、`TerminalPanelFactory` 或旧主题接口。
迁移期只允许两个框架在不同 Screen 上并存，不做新组件包装旧 Panel 的长期适配器。

## 目标架构

Batch A 已在 `ui2-core` 中建立平台无关地基；Batch B 又将共享场景提取到 `ui2-lab`，并完成 Java2D 与 Minecraft
DrawList renderer、抗锯齿 CJK 字体、普通 Screen 和原生 Container 双宿主。正式终端页面仍未迁移，当前只通过客户端
`F8` UI Lab 验证平台能力；具体完成项与验证命令见 `galaxy-ui-2-rebuild-execution-spec.md`。

```text
服务端 Snapshot / 本地输入
             |
             v
       UiStore<State, Action> ---------> UiEffect（网络、声音、路由）
             |
             v
       Computed / Selector
             |
             v
       Component<Props> + Slot
             |
             v
       Immutable UiElement Tree
             |
       reconcile by type + key
             v
       Retained UiNode Tree
             |
       measure -> layout -> hit test
             v
       DrawList -> UiRenderer -> Minecraft/OpenGL
```

### 1. Core：组件与元素

建议包：`com.jsirgalaxybase.client.ui2.core`

- `UiComponent<P>`：接收不可变 Props，返回 `UiElement`；
- `UiElement`：不可变描述，包含 type、key、props、children；
- `UiNode`：运行期节点，保存测量结果、边界、焦点、滚动和动画状态；
- `UiKey`：列表、页签、路由和动态行的稳定身份；
- `UiSlot`：为 Shell、Card、Dialog、Table 等组件注入内容；
- `UiContext`：只读提供 Theme、Density、Locale、Scale、Clock 和资源服务。

第一版不复制 Vue 模板编译器，不设计 XML/JSON DSL。业务页面直接使用类型安全的 Java builder / factory 组合组件，待
合同稳定后再评估声明式 DSL。

### 2. State：单向状态与副作用

建议包：`com.jsirgalaxybase.client.ui2.state`

- `UiStore<S, A>`：保存页面唯一 State，`dispatch(Action)` 后生成新状态；
- `UiReducer<S, A>`：纯函数状态转换；
- `UiSelector<S, R>`：缓存派生结果，等价于受控 `computed`；
- `UiEffect<A>`：处理网络请求、路由、声音和剪贴板等副作用；
- `UiSignal<T>`：只用于动画时钟、Hover、Focus 等细粒度本地状态，不替代服务端 Snapshot 真源。

服务器 Snapshot 只能通过页面 Reducer 进入 Store。组件不能直接改 Snapshot，绘制函数不能发包，基础控件不能知道市场、
银行或仓储服务。

### 3. Layout：约束式布局

建议包：`com.jsirgalaxybase.client.ui2.layout`

- `Constraints(minWidth, maxWidth, minHeight, maxHeight)`；
- `Size`、`Insets`、`Alignment`、`Placement`；
- `Row`、`Column`、`Stack`、`Flex`、`Grid`、`ScrollView`；
- `Breakpoint`：以逻辑工作区宽高和密度档位选择布局，不直接按 GUI Scale 写页面分支；
- 两阶段 `measure()` / `layout()`，禁止子节点在 `draw()` 中重新决定几何结构；
- 原生 Slot 使用最终布局结果映射到 `guiLeft/guiTop`，保持整数命中边界。

标准验收尺寸至少覆盖约 `350x193` 高 GUI Scale 等效工作区和 `620x340` 标准工作区。

### 4. Rendering：DrawList 与状态安全

建议包：`com.jsirgalaxybase.client.ui2.render`

- 组件只提交 `DrawCommand`，不直接操作 GL11；
- `UiRenderer` 按 layer 和 clip 排序执行 DrawList；
- `ClipStack` 使用 scissor，`LayerStack` 明确普通层、原生 Slot、Tooltip、Popup、Toast；
- `GlStateGuard` 在每批绘制后恢复 blend、texture、depth、scissor、颜色和矩阵状态；
- `SurfacePainter` 支持填充、细边界、圆角、渐变和克制阴影；
- `IconRenderer` 使用统一矢量化路径或高分辨率 atlas；
- `ItemRendererBridge` 只桥接 Minecraft 原生物品；
- `NativeSlotBridge` 保留原生 Slot、拖放、数字键、双击和服务器同步行为。

所谓“Canvas”在新框架中只应是 DrawList 的概念名称，不能让业务页面获得任意 OpenGL 权限。

### 5. Typography：抗锯齿字体是强制基础

建议包：`com.jsirgalaxybase.client.ui2.text`

- 首版实现客户端延迟字形 atlas，按当前 GUI Scale 生成抗锯齿 CJK/Latin glyph；
- 纹理在渲染线程创建、上传和释放，后台只允许做字形测量或像素准备；
- 支持 title、section、body、caption、numeric 等语义样式；
- 金额、数量和表格数字使用 tabular/等宽数字；
- 缺字时按受控 fallback 链查找，最终才退回 Minecraft `FontRenderer`；
- 字体文件必须允许再分发并记录许可证；
- 普通正文不默认绘制像素阴影，只有高噪声世界背景上的 HUD 可以使用轻量描边。

如果字体层没有完成，不应宣布“现代 UI”完成。只换圆角与颜色无法解决高 GUI Scale 下的像素放大。

### 6. Theme：完整设计 Token

建议包：`com.jsirgalaxybase.client.ui2.theme`

- 颜色：background、surface、overlay、border、text、accent、success、warning、danger；
- 空间：统一 spacing scale；
- 形状：radius、divider、outline；
- 字体：样式、字号、行高、字重；
- 控件：compact/normal 高度、最小命中区；
- 动画：duration、easing、reduced-motion；
- 层级：elevation、popup、tooltip、toast。

业务组件不得出现裸 ARGB、随意 padding 或独立动画时长。主题 Token 使用语义名称，组件不依赖某个主题的具体值。

### 7. Input：统一事件、焦点与模态

建议包：`com.jsirgalaxybase.client.ui2.input`

- 捕获/目标/冒泡三阶段指针分派；
- Pointer Capture 保证拖拽释放仍回到原控件；
- Hover、Pressed、Selected、Focused、Disabled 状态统一；
- 键盘焦点顺序、Enter/Space 激活、Escape 优先级；
- Popup/Modal 打开后阻断底层组件和原生 Slot；
- Tooltip 不截获鼠标；
- Container 宿主输入顺序固定为 Modal -> UI2 控件 -> 原生 Slot -> 页面快捷键。

### 8. Motion：克制动画

建议包：`com.jsirgalaxybase.client.ui2.motion`

- 使用真实渲染时间和 partial tick，而不是只按 20 TPS 跳变；
- 首版只支持 opacity、translate、color 和少量 scale；
- Hover 约 100ms，Popup/Toast 约 140-180ms；
- 禁止通过每帧改变宽高制造布局抖动；
- 提供 reduced-motion 配置，将所有动画降为即时状态切换。

## 第一批公共组件

```text
AppShell / NavigationRail / TopBar / PageHeader
Surface / Card / Divider / Badge / StatTile
Button / IconButton / SegmentedTabs
TextField / NumberField / Select / SearchBox
List / DataTable / Pager / ScrollView
Tooltip / ContextMenu / Dialog / Toast
InventoryGrid / NativeSlotRegion / VirtualItemGrid
EmptyState / LoadingState / ErrorState
```

组件必须有完整 Hover、Pressed、Focus、Disabled 和 Loading 状态。危险动作必须使用独立 danger 语义，不依赖按钮文字颜色
临时表达。

## 两种宿主仍然保留

```text
UiRuntime
  +-- ModernScreenHost extends GuiScreen
  +-- ModernContainerHost<C extends Container> extends GuiContainer

TerminalScreenV2Base
TerminalContainerScreenV2Base<C>
  +-- TerminalAppShell
```

普通终端页继续使用 `GuiScreen`，资产中心等原生库存页使用 `GuiContainer`。两者共享组件、布局、Theme、字体、Popup、
Tooltip、输入和路由，但只有 Container 宿主插入原生 Slot 层。不能为了框架统一而让所有页面常驻库存 Container。

## 替换与删除路线

独立编译、Java2D Demo、自动化边界和逐阶段完成门槛见 `galaxy-ui-2-rebuild-execution-spec.md`。本节只保留总体迁移顺序。

### Phase 0：冻结基线

- 记录当前终端所有路由、Popup、滚动、快捷键和 Container 输入矩阵；
- 保留现有自动测试和两档布局尺寸；
- 业务 UI 在迁移期间只修 P0/P1 缺陷，不继续积累旧框架控件。

### Phase 1：完全独立的 UI2 内核

- 新建 `client.ui2`；
- 完成 Component/Element/Node、Store、Layout、DrawList、Input 和 Theme 最小闭环；
- 新代码不得 import `client.gui.framework` 或 `terminal.client.component`；
- 先用纯 Java 单元测试验证树重建、Key、测量、布局、命中、焦点和模态。

### Phase 2：现代渲染与字体实验室

- 新增仅开发环境可进入的 UI Lab Screen；
- 同屏展示字体、卡片、按钮、输入框、表格、Popup、Tooltip、Toast、滚动和响应式布局；
- 验证抗锯齿字形 atlas、纹理释放、GL 状态恢复和高 GUI Scale；
- 这一阶段先核对视觉，不接真实业务服务。

### Phase 3：双宿主和原生 Slot

- 实现 ModernScreenHost 与 ModernContainerHost；
- 建立 UI -> Slot -> Tooltip -> Modal 的确定绘制顺序；
- 使用假 Container 验证点击、拖放、Shift-click、数字键、双击、光标和 Popup 阻断。

### Phase 4：资产中心纵向切换

- 资产中心作为首个正式页面；
- 保持现有 Vault、Bay、Cell 浏览器和背包 Container 合同不变，只替换客户端视图；
- 验证无闪烁路由、原生 Slot、虚拟 Cell 条目和现代 UI 共存；
- 实机验收通过后，UI2 才有资格成为终端新基线。

### Phase 5：迁移终端壳与普通页面

建议顺序：

1. Terminal App Shell、导航、TopBar、Popup、Toast；
2. 首页、通知、银行、ServerTools；
3. 标准市场、订单与资产中心；
4. 定制市场、汇率市场；
5. 地产地图和复杂自绘页面。

每个页面迁移完成后删除该页面的旧组件，不在业务层保留双实现开关。

### Phase 6：铲除旧框架

只有在全部正式路由已不再引用旧框架后执行：

- 删除 `client.gui.framework`；
- 删除旧 Theme、Panel factory、旧终端组件基类和只为兼容存在的 adapter；
- 使用 CodeGraph 确认旧符号零调用；
- 全量测试、assemble、客户端人工矩阵通过后部署。

这是真正的框架替换，而不是长期双轨。旧框架在 Phase 1-5 期间只承担可运行基线，不能继续扩展。

## 强制工程规则

- Render 函数必须无业务副作用；
- 基础组件不能直接发送网络包；
- 服务端 Snapshot 继续是账户、订单、产权和资产的唯一真源；
- UI2 源码不依赖旧 Panel 框架；
- 除 Renderer/Bridge 外禁止直接调用 GL11、`Gui.drawRect` 或 `FontRenderer`；
- 除 Theme 定义外禁止裸颜色；
- Popup 打开时不得操作底层 Slot；
- 所有纹理、字形页和后台任务必须绑定 Runtime 生命周期并可释放；
- 所有可变列表使用稳定 Key；
- 所有正式控件同时覆盖鼠标、键盘、禁用和焦点状态；
- 不以动画代替明确状态，不以颜色作为唯一状态信号。

## 验收标准

- UI Lab 在 `350x193` 与 `620x340` 等效逻辑工作区不越界；
- GUI Scale 改变不会按比例放大字体锯齿、边界粗细或头像等装饰；
- 中英文混排、数字对齐、缺字 fallback 和长文本裁剪可预测；
- 相同组件在普通 Screen 与 Container Screen 的视觉和输入一致；
- 原生 Slot 的所有 Vanilla 交互保持不变；
- Modal、Tooltip、Toast、右键菜单和焦点层级确定；
- 业务页不含硬编码颜色和重复按钮绘制；
- 页面切换不出现空白中间帧；
- DrawList 数量、字形 atlas 页数、布局耗时和每帧分配可观测；
- 自动测试之外，最终必须完成一次资产中心和市场的真实客户端人工验收。

## 已确认的视觉方向

- 首版只做现代深色主题；
- 使用现代工业控制台密度，不采用手机式超大按钮；
- 中性深色 Surface 分层，冷蓝/青蓝只用于主操作、选中和焦点；
- 普通区域减少实线外框，以明度、留白和局部分隔线建立层次；
- 默认正文无像素阴影；
- Minecraft 物品图标保留原生像素风，终端图标、文字和容器框架使用平滑样式；
- 动画短而克制，并可完全关闭。

## 参考资料

本地固定版本和 Java/Forge 概念映射见：

- `../Reference/VueDocs/README-JSIR.md`
- `galaxy-ui-2-vue-reference-map.md`

本地快照包含 Vue 官方英文 Guide/API/Tutorial 与官方中文翻译，仅作开发参考，不作为运行时依赖或 JAR 内容。Vue 文档正文
采用 CC BY 4.0；实施 UI2 时默认根据公开合同自主实现，而不是复制文档示例或机械翻译 Vue Runtime。

- Vue Components：https://vuejs.org/guide/essentials/component-basics.html
- Vue Props：https://vuejs.org/guide/components/props.html
- Vue Slots：https://vuejs.org/guide/components/slots.html
- Vue State Management：https://vuejs.org/guide/scaling-up/state-management
- Vue Performance：https://vuejs.org/guide/best-practices/performance
- Vue Transition：https://vuejs.org/guide/built-ins/transition
- Element Plus 设计原则：https://element-plus.org/zh-CN/guide/design.html
- Carbon Color Tokens：https://carbondesignsystem.com/elements/color/usage/
- Forge Screens：https://docs.minecraftforge.net/en/1.20.1/gui/screens/
- Forge Menus：https://docs.minecraftforge.net/en/latest/gui/menus/

Forge 链接描述的是当前官方抽象；本项目运行于 1.7.10，对应名称仍是 `GuiScreen`、`GuiContainer`、`Container`、
`Slot`、`FontRenderer`、`Tessellator` 和 GL11，实施时以当前依赖中的实际 API 为准。

## Qz 算法剪裁补充（Batch B.1-B.4）

UI2 不改用 Qz-UILib，也不产生 Qz 运行时依赖。渲染侧只从固定的 Qz 1.8.2、4.1.3-LTS、4.7 与 4.8
提交中内化圆角限幅、嵌套裁剪恢复、字形 generation 和帧预算上传等成熟算法。所有派生代码留在 Base
命名空间并具有 SPDX、上游路径与提交号；完整边界见
`galaxy-ui2-qz-derived-integration-plan-2026-09-05.md`。

这不改变 UI2 的架构所有权：组件、Store、Selector、Effect、Runtime、DrawList、双宿主和 Java2D 后端仍由
Base 定义。Qz DOM/CSS、Signal、Mixin、网络、HUD、配置和 Mod 启动链明确不进入 UI2。

## 首张正式业务页状态（2026-09-06）

资产中心已经成为首张 UI2 正式业务页。其 UI 使用生产组件树和共享终端壳，业务状态只经 Action 进入 Store，
网络副作用集中在 Effect；服务端权威 Container 继续承载 Base Vault、玩家背包和 AE2 Cell Bay。虚拟 Cell
条目通过稳定 external region 交给 Minecraft 物品桥绘制，不把 `ItemStack` 泄漏到纯 Java Core。

自动验收和 Lobby/客户端部署已经完成。UI2 尚未成为所有终端页的默认框架：必须先通过资产中心实机操作门槛，
然后再按首页、通知、银行、ServerTools、市场、地产的顺序迁移；旧 Canvas 只在所有正式路由归零后删除。
