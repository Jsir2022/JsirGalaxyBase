# 终端 GUI 双宿主架构

日期：2026-09-04

## 目标

终端的普通信息页继续使用 `GuiScreen`，需要 Minecraft 原生槽位的页面使用 `GuiContainer`。两种宿主共享同一套
Canvas 场景生命周期、终端布局、状态栏、导航、主题和弹窗，不让所有终端页面为了少数库存页面常驻 Container。

## 分层

```text
CanvasSceneRuntime
  ├─ CanvasScreen -> TerminalScreenBase -> TerminalHomeScreen
  └─ CanvasContainerScreen -> TerminalContainerScreenBase -> GuiTerminalAssetCenter

TerminalShellFrame
  ├─ TerminalHomeLayout
  ├─ status band
  └─ navigation rail
```

- `CanvasSceneRuntime` 不继承 Minecraft GUI 类，只持有根 Panel、Popup、Hover 与主题，并负责绘制和输入分派。
- `CanvasScreen` 与 `CanvasContainerScreen` 是薄宿主，继续实现既有 `GuiScene`，所以现有 Panel 无需改接口。
- Container 绘制顺序固定为终端底层、槽位背景、原生 Slot/物品/Tooltip、终端 Popup。Popup 存在时原生槽位不接收输入。
- `TerminalHomeLayout` 是两种宿主共同使用的唯一终端外框坐标合同；原生 Slot 坐标由内容布局换算为相对 `guiLeft/guiTop`。
- `TerminalShellFrame` 组合状态栏和导航，不承载具体业务页面。

## 路由

`TerminalClientScreenController` 缓存最后一次服务端终端模型。Container 返回普通页面时，
`TerminalRouteCoordinator` 在同一客户端调度中先恢复缓存终端壳，再请求目标页快照；不先关闭到游戏画面，避免闪屏。
服务器仍会正常关闭当前窗口，客户端缓存只用于显示与导航，不能决定账户身份或库存结果。

## 资产中心首个落地页

- “资产”导航直接打开权威 Container，不再先进入说明卡页面。
- “存储管理”同时显示 27 格 Base Vault、一个真实 AE2 Cell Bay 和玩家背包。
- 内容区按资产语义固定分栏：左侧纵向放置 Vault 与玩家背包，右侧放置 Bay 与 Cell 内容；Bay 始终可见，未插入 Cell 时不绘制内容网格。
- 背包 Shift-click 始终进入 Base Vault；AE2 Cell 必须手动拖入 Bay；Vault/Bay Shift-click 返回背包。
- Cell 内容不是原生 Slot，而是画在原生容器宿主中的服务端虚拟条目；这样可表达超过堆叠上限的 `long` 数量，同时仍由同一服务端 Container 执行光标更新、版本校验和持久化。
- “资产动态”隐藏并禁用全部 Slot，只展示服务端结构化业务投影。

## 约束

- 客户端 Container 只是 Forge 同步所需的槽位镜像，不是资产真源；写入仍由服务端 Container、Vault 与 Bay 服务提交。
- 旧资产页签编码 `0/1/2` 一版内均映射到存储管理，`3` 映射到资产动态。
- 不新增资产数据库表，不将手工存取、整理和 Cell 插拔暴露成玩家业务动态。

## 验证与灰度

- 双宿主、终端壳、`350×193`/`620×340` 资产布局、结构化协议与业务动态定向测试通过。
- 全量测试 492 项，52 项按既有环境条件跳过，0 失败；市场 PostgreSQL 15/15、仓储 PostgreSQL 4/4 均通过。
- 2026-09-04 的 runtime JAR 只部署到 Lobby 和 Prism 客户端；Lobby 已到 `Done`，客户端未启动，S1/S2 未触碰。
