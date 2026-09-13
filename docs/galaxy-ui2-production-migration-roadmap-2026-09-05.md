# Galaxy UI 2 正式迁移路线

日期：2026-09-05

## 阶段状态

| 阶段 | 状态 | 验收门槛 |
|---|---|---|
| A：独立 Core 与 Java2D Demo | 自动验收完成 | 已通过 |
| B/B.1-B.4：Minecraft 渲染、双宿主与 Qz 算法剪裁 | 人工验收完成 | 圆角、字体、浮层、Slot 已确认 |
| C.0：生产组件与终端公共壳 | 自动验收完成 | Core/Lab/Demo 与双宿主自动测试已通过 |
| C.1：资产中心 UI2 纵向迁移 | 自动验收完成 | 既有 Container/网络合同零变更 |
| C.2：资产中心生产基线 | 人工视觉未通过 | 全屏宿主、藏蓝主题和新旧首页跳变需要整改 |
| C.3：窗口化与玻璃主题 | 人工部分通过 | 窗口和资产可用；首页文字布局实机未通过，已进入 P1 整改 |
| D：普通终端页迁移 | 自动验收完成 | 首页、通知、银行、ServerTools、职业、公共与物品策略统一 UI2 路由 |
| E：市场页迁移 | 自动验收完成，待实机 | 总入口、标准、订单中心、定制、汇率均复用原服务端合同 |
| F：地产地图迁移 | 自动验收完成，待实机 | UI2 页面与真实地图 external region 已接入；地图后端已脱离 Canvas |
| G：删除旧 Canvas | 自动验收完成，待实机 | 旧 Screen、Theme、Popup、Panel 与重复输入系统已删除并有构建守卫 |

## 实施原则

- C.2 的业务操作验收通过，但视觉验收未通过；整改优先统一窗口、主题和首页，不把资产页继续做成孤立特例。
- UI2 Core 保持纯 Java 8，不出现 Minecraft、Forge、LWJGL、ItemStack 或 Slot。
- 资产中心继续复用既有服务端 Container、Snapshot、幂等请求、版本、准入和失败回滚。
- 组件负责主题化绘制和输入合同；业务页面只组合状态、布局、稳定键和 Action，不直接调用 GL 或原版字体。
- 每阶段开始与收口均使用 CodeGraph；测试、产物哈希、部署目标和人工门槛记录到 `docs/WORKLOG.md`。

## C 阶段交付

C.0 提供组件注册表、统一组件文档、动作处理器、Terminal App Shell、普通/Container 终端宿主和
平台隔离的原生物品桥。C.1 将 `GuiTerminalAssetCenter` 改为 UI2 Container 页面，保持左侧 Vault/背包、
右侧 Bay/Cell、第二页签资产动态的已确认布局。C.2 覆盖三档逻辑尺寸和完整 Container 交互回归，部署同一
JAR 到 Lobby 与客户端后等待人工验收。

## C.3 窗口化与玻璃主题整改

- `TerminalWindowMetrics` 是 Screen 与 Container 共用的唯一窗口尺寸合同。UI 宿主仍覆盖整个 Minecraft
  画面以接收事件，但终端内容只绘制在居中的 `350×193` 至 `620×340` 响应式窗口内；常规尺寸保留约
  10% 横向和 12% 纵向世界背景。
- 生产终端使用独立 `GlassTerminalTheme`：半透明黑色窗口、低透明白色层次与边框、冷白文字及克制的
  青绿色信号色。选中项不再整块填充亮蓝，槽位也不再硬编码藏蓝底色。
- 首页新增 `TerminalHomeDocument`。首次打开首页以及资产中心返回首页均使用同一个 UI2 窗口壳；后续完成的
  银行、市场和地产页面也复用该宿主，同时保持其服务端合同不变。
- C.3 自动验收覆盖窗口居中与上下限、首页布局与 Modal、资产全部原生区域落在窗口内、UI2 全量、根项目
  全量、打包及自包含检查。人工门槛为窗口比例、背景透出、玻璃层次、导航密度和首页/资产往返观感。

## 2026-09-06 C 阶段实施结果

- C.0 已落地生产组件注册表、组件文档、动作处理器、标准控件、共享 `TerminalAppShell`、普通与 Container
  终端基类，以及隔离 Minecraft `ItemStack` 的 `NativeItemBridge`。UI Lab 已删除手写 DrawList 页面实现并迁移到
  同一生产组件注册表。
- C.1 已将 `GuiTerminalAssetCenter` 切换为 UI2 Container 宿主；资产网络、服务端 Container、Vault、背包、
  Bay、Cell 快照、版本和幂等合同保持不变。旧资产页 Panel 和两套旧资产布局计算类已经删除。
- C.2 自动验证覆盖 `350×193`、`427×240`、`620×340`，并回归组件全局层级合成、Modal 输入阻断、
  原生槽位桥、虚拟 Cell 物品桥、资产协议、Vault/Bay 服务和 PostgreSQL 仓储集成。
- C.2 实机确认资产操作可用，但全屏比例、主题及返回旧首页未通过视觉门槛。C.3 已完成自动整改，并将
  D.0 首页提前并入同一工作链以消除资产往返的代际断裂；其余 D 阶段页面仍等待本轮人工视觉确认。

## 后续顺序

代码迁移和旧 Canvas 删除已经完成。下一门槛是部署后的完整实机矩阵：逐页导航、三档窗口与密度、三套内置
主题、Resource Pack 回退、Container 操作、市场确认浮层和地产地图输入。

## 2026-09-06 个性化与全页面整改

实机证明 C.3 的窗口矩形正确，但首页固定卡片高度、硬编码字体和无溢出策略导致高 GUI Scale 下文字重叠。
整改计划与主题包合同已拆分到 `galaxy-ui2-terminal-personalization-and-migration-2026-09-06.md`。当前 P1
已补测量驱动换行、最大行数、省略与裁剪；P2/P3 已建立三套主题、客户端独立配置、离散密度/窗口档位、
Resource Pack 注册入口和现代设置页。随后已完成非 Container 统一路由、轻量页面、三类市场、订单中心与
地产页面迁移。随后真实地图表面已从旧 `AbstractGuiPanel` 和 `GuiRect` 解耦，统一使用 UI2 `UiRect`；旧
Canvas Screen、Container、Theme、Popup、Panel、终端工厂和重复输入系统已经删除。地图仍位于 UI2
`NATIVE` 外部区域层，Modal 会先阻断地图输入，不会形成新旧页面跳转。
