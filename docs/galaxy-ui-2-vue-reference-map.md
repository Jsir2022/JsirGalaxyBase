# Galaxy UI 2：Vue 官方文档参考与 Java/Forge 映射

日期：2026-09-05

## 结论

Vue 适合作为 Galaxy UI 2 的“编程模型老师”，不适合作为可逐行翻译的实现。最有价值的是把页面写法从“修改一棵可变
Panel 树并在 draw 中处理一切”升级为“State 驱动不可变元素树，组件只描述结果，副作用统一出界”。底层渲染仍由
Minecraft 1.7.10 和 Forge 提供的 Screen、Container、Slot、FontRenderer、Tessellator、TextureManager 与 GL11 承担。

官方英文文档和官方中文翻译已放在本地 `Reference/VueDocs`。该目录受仓库 `.gitignore` 保护，是开发机参考快照，不会进入
源码提交或发布 JAR；固定提交和 CC BY 4.0 许可见 `Reference/VueDocs/README-JSIR.md`。

## 当前 Base GUI 的真实来源

当前框架不是直接参考 Vue，也不是 Forge 自带：

1. 早期终端业务页使用 GTNH 生态中的 ModularUI 2。
2. 2026-04 起，为降低共享运行时版本和 ABI 风险，项目参考 BetterQuesting 的 `GuiScreenCanvas`、`IScene`、
   `IGuiPanel`、`PanelButton` 和 `ThemeRegistry`，在 `com.jsirgalaxybase.client.gui` 下重写了自有 Canvas/Panel 框架。
3. 当前 `CanvasScreen extends GuiScreen`，资产页使用 `CanvasContainerScreen extends GuiContainer`；两者共享
   `CanvasSceneRuntime`，这是近期验证过的“双宿主”结构。
4. CodeGraph 显示这两种宿主分别由 `TerminalScreenBase` 和 `TerminalContainerScreenBase` 承接，而 `GuiPanel` 已进入
   终端、市场、地产、Popup 和测试链，不能原地改接口后一次性迁移。

所以 Galaxy UI 2 应复用“普通 Screen 与原生 Container 分开、上层框架共享”的产品结论，但重新实现组件、状态、布局、
渲染和输入内核。

## Vue 概念到 UI2 的映射

截至 Batch B，这份映射已落成实际 `UiRuntime + UiStore + UiElement/UiNode + DrawList` 链路。Java2D 与 Minecraft
不再各自维护 Lab 页面，二者共同消费 `ui2-lab` 的组件文档；平台差异只存在于 renderer、资源和 Screen/Container 宿主。

| Vue 概念 | Galaxy UI 2 对应物 | 不照搬的部分 |
|---|---|---|
| Component | `UiComponent<P>`，根据 Props 和 Context 生成 `UiElement` | `.vue` 文件、模板编译器 |
| Props | 不可变 Java value object | 可变 Map、运行时字符串属性 |
| Emits / Events | 类型安全 `UiAction`，向 Store 或页面控制器提交意图 | 字符串事件名、基础组件直接发网络包 |
| Slot | `UiSlot` / child supplier，为 Shell、Card、Dialog 注入内容 | DOM slot 与 Web Component 兼容层 |
| Reactive State | `UiStore<S, A>` 和不可变页面 State | JavaScript Proxy、深层隐式追踪 |
| Computed | 可缓存的纯 `UiSelector<S, R>` | 在绘制阶段产生副作用 |
| Watch / Effect | 显式 `UiEffect`，处理网络、路由、声音和资源加载 | 任意对象监听和隐式依赖收集 |
| Lifecycle | `mount/update/unmount`，绑定 Screen/Container 和 GPU 资源生命周期 | 浏览器 DOM 生命周期 |
| `v-for` key | `UiKey`，保留列表行、焦点、滚动和动画身份 | 以数组位置充当动态业务键 |
| Provide / Inject | 只读 `UiContext`，提供 Theme、Locale、Density、Clock 和资源 | 隐藏业务服务定位器 |
| Transition | 受控 Motion，限定 opacity/translate/color/少量 scale | DOM class、CSS transition |
| KeepAlive | 可选 RouteStateCache，只保存允许保留的本地 UI 状态 | 保存服务端权威业务快照为第二真源 |
| Teleport | Popup/Tooltip/Toast 进入根 Overlay Layer | DOM 节点搬移 |

## 官方章节如何用于实现

### 第一组：必须形成框架合同

- `component-basics`、`props`、`events`、`slots`：定义 Component、Props、Action 和 Slot。
- `reactivity-fundamentals`、`computed`、`watchers`：定义 Store、Reducer、Selector 和 Effect 的边界。
- `lifecycle`：定义节点、纹理、字形页、后台任务和输入捕获的释放责任。
- `rendering-mechanism`：只借鉴 element tree、type + key reconciliation 和批量更新，不复制 VDOM/DOM patcher。

### 第二组：形成应用框架

- `state-management`：服务端 Snapshot 进入客户端 Store 的单向链路。
- `routing`：终端路由、普通 Screen 与 Container Screen 的切换；不引入 Web Router。
- `composables`：提炼 Java controller/service，例如分页、选择、异步加载和确认流程；不模拟 Composition API 语法。
- `keep-alive`：谨慎保留搜索条件、滚动位置等本地体验状态，不缓存银行余额或订单真源。

### 第三组：形成质量门槛

- `performance`：稳定 Key、减少每帧分配、缓存布局与文本测量、虚拟化长列表。
- `accessibility`：键盘焦点、非颜色状态表达、可关闭动画、足够命中区和可预测 Escape。
- `security`：界面文本和本地状态永远不替代服务端验证。
- `transition` / `animation`：只用于反馈与层级变化，不能掩盖网络等待或业务失败。

## 比“直接按 Vue 代码重写”更合适的办法

采用“合同移植 + 垂直样板”，而非“源码翻译”：

1. 先把 Vue 相关概念写成 UI2 的 Java 接口、不可变量和行为测试。
2. 建一个 UI Lab，同时验证 Component、Store、Layout、DrawList、字体、焦点、Popup 和两种宿主。
3. 用资产中心完成第一条真实垂直链：Base Vault + 背包原生 Slot、Bay 原生 Slot、Cell 虚拟内容、弹窗和无闪烁路由。
4. 只有这条链的视觉、输入、资源释放和性能都通过，才迁移终端壳与其他页面。
5. 全部路由迁移完且 CodeGraph 确认旧框架零调用后，再删除旧 Canvas/Panel 框架。

这样既能获得 Vue 的可维护性，又不会把浏览器的 DOM、CSS、Proxy 和编译器负担带进 Minecraft 1.7.10。

## Forge/Minecraft 和传统 Mod GUI 的关系

传统 1.7.10 Mod 通常直接继承 `GuiScreen`，有库存槽位时继承 `GuiContainer` 并配套服务端 `Container`。界面通过
`drawRect`、纹理 blit、`FontRenderer`、`Tessellator`、GL11、原生 Button/Slot 和鼠标键盘回调绘制。Forge 主要提供
注册、事件、网络和对 Minecraft GUI 生命周期的接入；它没有 Vue/浏览器那样的响应式组件、Flex/Grid、CSS、焦点树或
设计系统。

较大型 Mod 往往再自建或采用一层 UI 库，例如 ModularUI、BetterQuesting 的 Panel/Canvas 系统，来解决布局、控件和主题
复用。Base 当前就是后一种，但现有层仍偏“低级 retained panel + 手工坐标”，因此才需要 Galaxy UI 2。

## 落地约束

- 本地 Vue 文档只作参考，不作为 Gradle 依赖，不发布进 JAR。
- 实现依据以本映射和 `galaxy-ui-2-modern-java-framework.md` 为准；Vue 文档不能覆盖项目的服务端权威与安全合同。
- 默认自主实现公开概念；若以后复制 Vue 或其他项目的具体源码，必须单独核对许可证、保留来源并更新第三方声明。
- UI2 的第一批代码不得 import 旧 `client.gui.framework`；旧框架仅在迁移期承载尚未迁移页面。
- 每个 Phase 都先执行 CodeGraph 调用/影响分析，再做定向测试和全量回归。

## Vue 与 Qz 的职责边界

Vue 仍只负责 UI2 的组件、单向数据流、稳定 key、生命周期和批量失效等架构映射；Qz 只提供 Minecraft
渲染实现层的算法经验。两者都不是 Base 的运行时依赖，也不能越过服务端 Snapshot 和 Container 权威边界。
具体 Qz 固定版本、许可证和派生文件清单见
`galaxy-ui2-qz-derived-integration-plan-2026-09-05.md`。

## 上游入口

- Vue 英文文档：`git@github.com:vuejs/docs.git`
- Vue 官方中文翻译：`git@github.com:vuejs-translations/docs-zh-cn.git`
- 在线英文文档：https://vuejs.org/guide/
- 在线中文文档：https://cn.vuejs.org/guide/
