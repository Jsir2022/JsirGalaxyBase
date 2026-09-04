# 银河仓储 v2：终端托管存储单元 Bay 与统一资产中心

日期：2026-09-01

## 已确认的产品方向

个人仓储不再要求玩家放置 `Warehouse Drive` 方块。银河终端提供每位玩家一个服务端托管的 **Cell Bay**：
玩家在终端打开单槽库存界面，从背包放入或取出一枚真实 AE2 Storage Cell。Cell 的 NBT、容量与物品/类型
限制继续由 AE2 实现；JGB 只保留 Bay 的个人归属、版本、Cell 快照及审计。

这意味着 Cell **不加入玩家世界的 AE2 网络**。终端不会显示或伪造频道、供电、网络已连接、外部 ME 访问等状态。

## 页面形态

- 顶层页面改名为“银河仓储”，定位为统一的个人资产中心；本批先汇总 Base Vault、终端托管 Cell 与最近操作。
  银行可用、待领取等跨域摘要必须等对应服务端快照合同就绪后再接入，不能在 UI 伪造。
- Base Vault 与 AE2 存储单元属于同一个“存储管理”工作区，不再拆成两张空白说明页或两个 Container。
- 2026-09-04 起原生资产容器复用完整终端壳，页签收敛为 `存储管理 / 资产动态`；旧 `0/1/2/3` 编码兼容一版。
- 同一个服务端权威 `Container` 同时持有 27 格 Base Vault、1 格 Cell Bay 与玩家背包；存储页全部可见，动态页全部隐藏并由服务端拒绝槽位操作。
- 存储页支持拖放、Vault 整理和固定 Shift 规则；唯一真实 Cell 槽无需实体方块。操作失败会恢复 Vault、Bay、玩家背包和光标的本次快照。
- Cell 未插入时只保留真实 Bay 槽与空态，不绘制空的内容浏览器；插入后显示 Cell 名称、容量摘要、物品/类型数量和分页内容。
- Cell 内容区是单 Bay 的轻量 Vault 扩展，不实现完整 ME Terminal：每种物品使用一个虚拟条目和 `long` 数量，左键取一组、右键取一个，手持物品通过明确按钮存入。
- 页面固定以左侧 Base Vault＋玩家背包、右侧 Cell Bay＋Cell 内容分区；不展示或伪造 ME 频道、供电或外部网络状态。
- 资产动态只读并按当前玩家隔离，显示真实业务事件而非 Vault/Bay 技术审计。
- 容器尺寸直接采用 `TerminalHomeLayout`，`350×193` 等效高 GUI Scale 下纵向堆叠，宽工作区并排显示，不把终端外空间当作可绘制范围。

## 2026-09-04 已实施：显示框架与资产内容重构

2026-09-02 实机复核确认：当前独立资产 `GuiContainer` 虽然已经能安全操作真实槽位，但仍是过渡实现。
它自己绘制标题、摘要、页签、卡片和返回按钮，没有复用终端的状态栏、导航、主题组件与页面路由；返回时先关闭容器、
再重新请求终端，因此会出现短暂闪屏。后续不得通过“把整个终端根页面改成 Container”解决，这会让首页、市场、地产、
ServerTools 等非库存页面无意义地承担库存会话。

显示框架应按组合关系拆分，而不是继续堆积单体 GUI：

```text
CanvasSceneRuntime
  - 主题、Panel 树、Popup、Hover、绘制和输入分派
  - 不继承 GuiScreen 或 GuiContainer

CanvasScreenHost extends GuiScreen
CanvasContainerHost extends GuiContainer
  - 两个宿主都委托给同一个 CanvasSceneRuntime

TerminalShellPresenter
  - 状态栏、左侧导航、内容区、路由和终端主题
  - 可同时装配到普通终端页与原生 Container 页

TerminalScreenBase / TerminalContainerScreenBase
  - 分别服务普通终端页面与需要原生 Slot 的终端页面
```

- 保留普通 `TerminalHomeScreen` 的非 Container 定位，不让终端所有页面常驻 Vault/Bay 会话。
- 新资产页改为 `TerminalContainerScreenBase` 的专用实现，但视觉上仍是完整终端壳；进入和退出时直接切换受控屏幕，
  不先显示游戏画面，也不重新创建一套粗糙外观。
- `GuiTerminalAssetCenter` 当前集中绘制与输入代码应拆为资产内容 Panel、槽位布局、业务动态列表和薄 Container 宿主；
  服务端 `TerminalAssetCenterContainer` 继续承担真实槽位、版本、恢复和准入校验。
- 旧资产 GUI ID 保留一版兼容，但不再作为新的页面框架范例。

资产内容区的目标形态：

- 进入“资产”后直接显示 Base Vault、Cell Bay 与玩家背包，不再先看说明卡再点击二次入口。
- Base Vault 与背包需要在 `350×193` 等效高 GUI Scale 下同时可操作；允许改变 Vault 的视觉列数，但不改变 27 格业务容量。
- Cell Bay 是同一工作区内的真实单槽，展示真实 Cell 名称、bytes、物品数和类型数；不展示虚构网络、供电或频道。
- 总览摘要可以保留为紧凑状态条，但不能挤占主要槽位工作区。

玩家可见的“操作记录”改名为“资产动态”，其合同与内部审计严格分离：

- `VAULT_CONTAINER_MUTATION`、整理、玩家手工存取、Cell 插入/取出等技术操作继续写数据库，用于并发、恢复与追责，
  但不出现在玩家业务动态中。
- 资产动态只显示系统产生的业务事实：买入物品已存入、卖出物品已成交并到账、定制商品交付、撤单库存返还、
  自动交付失败及恢复结果。
- 卖出动态必须来自真实成交记录，不能把挂单进入托管误报为“卖出”；买入入库必须来自真实交付/Vault 写入结果。
- 行数据使用结构化事件、真实 `ItemStack`、数量、金额、时间和业务记录号，由客户端本地化物品名；不得继续拼装
  `SUCCESS | v0 -> v1`、`槽位变更 | 成功` 之类的技术字符串。
- 同一笔成交与入库若来自一条业务链，需要稳定关联键去重；异常仍链接到既有受审计恢复动作，不自动重试。

上述双宿主框架和资产内容已进入实现：普通终端使用 `TerminalScreenBase`，资产 Container 使用
`TerminalContainerScreenBase`，两者通过 `CanvasSceneRuntime` 与 `TerminalShellFrame` 共享场景和终端壳。
资产导航直接进入统一存储管理，旧四页签编码兼容收敛为“存储管理 / 资产动态”。

存储页同时展示 Vault、Bay 和背包。已确认的快捷转移规则是：背包 Shift-click 始终进入 Base Vault，Cell 只能手动
拖入 Bay，Vault/Bay Shift-click 返回背包。插入 Cell 后，右侧直接提供有限分页内容浏览；内容操作由服务端对脱离外部
AE 网络的真实 Cell inventory 执行，准入规则、容量、类型数和 bytes 限制仍由 AE2 决定，并以 Bay 版本和幂等 requestId
防止重复存取。资产动态改为最多 20 条结构化业务行，读取标准市场成交、定制市场交付、
市场异常与 Vault 交付/恢复真源；手工槽位变化、整理和 Cell 插拔不进入玩家动态。不新增数据库表。

## 真源与安全

```text
玩家背包 <-> 服务器授权单槽 Container <-> warehouse_terminal_bay.cell_nbt
                                             |
                                             +-> warehouse_terminal_bay_operation

Cell NBT -> AE2 Cell 内容、容量、物品/类型限制
```

- Bay 的身份一律由服务端 `EntityPlayerMP UUID + bankingSourceServerId` 注入；客户端不能指定所有者或服务器。
- 只接受 AE2 认可的 Storage Cell，且数量固定为一枚；取出为正常背包操作。
- 每次实际 Bay 改变使用 requestId、版本和前后 Cell 快照审计；并发/重放不能覆盖另一会话结果。
- Cell NBT 保存在 PostgreSQL 仅为物品本体持久化，绝不把内容复制到 Base Vault、市场、第二个物品账本或外部 AE 网格。
- Cell 浏览器返回分页后的稀疏物品快照；客户端不提交数量、账户身份或任意 NBT，提取目标只作提示，服务端重新在当前 Cell 中匹配并执行。
- Base Vault 仍是独立 27 格有限账本；它不会实现 `ICellContainer`，也不会被 AE2 扫描。

## 过渡

已经注册的实体 `Warehouse Drive` 保留为 v1 兼容内容，不能作为 v2 主入口，也不新建配方。当前生产库没有
Drive 登记记录；后续维护窗口可单独评估隐藏或移除实体内容，不能修改已应用 migration 历史。

2026-09-02 灰度已应用 `20260901_001_add_terminal_warehouse_bay.sql`，并将同一 JAR 部署至 Lobby 与客户端
目录；客户端没有启动。Lobby 已通过共享 JDBC schema 校验并完成正常启动，Bay 与操作审计表均以空数据开始。

同日完成资产中心视觉与容器合并：Forge 只注册一个资产 GUI handler，兼容旧 Vault/Bay GUI ID 的解码，但终端入口统一打开
新资产中心。此次改动不修改数据库结构、既有 Cell NBT 或 Base Vault 产权数据。

## 不做

- 不做跨服 AE 网络、频道、供电、实体 Drive、Warehouse Port、市场自动投递、组织/公共 Bay。
- 不做搜索、排序选项、合成、跨 Cell 聚合等完整 ME 终端能力，也不让 Cell 内容绕开市场与 Base Vault 的准入规则。
- 不自动把 v1 Drive 中的 Cell 迁入终端 Bay；若以后有真实 v1 数据，必须有玩家确认和可审计迁移。
