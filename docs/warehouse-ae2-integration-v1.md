# 银河仓储网络 v1：AE2 实体仓储集成决策

日期：2026-07-20

本文是 `market-warehouse-v1-product-boundary-draft.md` 与
`warehouse-transfer-and-audit-boundary-v1.md` 的实现方向补充。前两份文档继续
定义资产域、市场托管与审计边界；本文只定义未来个人、企业和公共仓的实体
存储应如何复用 GTNH 的 AE2。

## 1. 已确认的技术决策

- AE2 负责物品的实际存储、容量、物品种类限制、能源、频道和网络拓扑。
- JsirGalaxyBase 不复制 AE2 存储元件内容，也不创造无限虚拟背包；但每个仓库
  账户拥有一个严格有限、类似末影箱的基础保险箱，作为无 AE2 阶段的起步库存与
  交割缓冲。
- JsirGalaxyBase 负责仓库账户归属、权限、登记端口、资产域转移、审计和恢复。
- 市场托管仓仍是数据库中的独立结算账本；它不退化为普通 AE2 库存，也不与
  个人、企业或公共仓共用余额状态。

这意味着玩家起步时可使用有限的账户保险箱；超过该容量后，主要扩容方式是建造
真实的 AE2 基础设施：ME Drive、存储元件、接口、存储总线和供电网络，而不是
购买一个无限容量的 JGB 菜单升级。

## 2. 基础账户保险箱：有限的虚拟末影箱

每个仓库账户（个人、企业、公共）在创建时拥有一个 **Base Vault**。它是账户绑定、
跨地点和跨灰度服可访问的有限持久 Inventory，而不是玩家角色背包或原版
`InventoryEnderChest` 的别名：

```text
玩家背包 <-> Base Vault（个人 27 格；企业/公共 54 格；持久 ItemStack NBT、账户权限）
                    <-> 市场 CLAIMABLE / 奖励待发 / 定制市场待领
                    <-> 后续 Warehouse Port <-> AE2 Warehouse Drive
```

- Base Vault 是新资产域 `BASE_VAULT`。个人账户有 27 格；企业与公共账户各有 54 格。
  其中的每一格保存完整 `ItemStack` NBT，
  因而能保留附魔、机器配置和定制物品；容量严格为 27 个原版槽位及原物品堆叠
  上限，不按“物品种类”或数据库行数偷扩容。
- 它适合起步玩家、离线奖励、市场购买后的安全领取、定制市场交付和需要人工
  决定目的地的物品。它不应作为无限离线物流总线。
- Base Vault 不实现 `ICellContainer`，也不挂入 AE 网络。否则 AE2 网络会把它当成
  无 Cell 成本的虚拟存储，破坏 GTNH 的容量、频道和电力成长线。
- 玩家可在 JGB 终端或放置的 Warehouse Controller 打开它；这是一套受账户权限
  管理的背包 GUI，不是新的 ME Terminal。个人仓仅本人访问；企业/公共账户由
  对应角色授予 `VIEW`、`DEPOSIT`、`WITHDRAW`。
- 满仓时不会丢物品：市场/奖励的持久资产继续停留在来源域的 `CLAIMABLE` 或
  `PENDING_DELIVERY`，操作返回明确的“基础保险箱空间不足”原因。

**交易容量规则：** 市场成交和银行结算不预留 Base Vault 容量。成交后资产先保持
`CLAIMABLE`；玩家主动领取时才尝试写入 Vault。若 Vault 满仓，领取明确失败，
`CLAIMABLE` 保持不变，玩家腾出空间后可再次领取。AE2 Drive 的可用 byte/type
容量同样不做长期预留，不能把模拟注入误当作永久容量承诺。

原版末影箱不作为持久实现：它属于玩家存档，不表达企业/公共账户、跨服操作日志、
幂等交付或恢复状态。JGB 会复用同样的“账户共享箱子”体验，而使用自己的 PostgreSQL
槽位账本。

## 3. 修订后的核心模型：账户绑定 AE2 Drive

2026-07-20 的 Phase 0 调查确认，首版不应把 JGB 仓做成“虚拟库存再
接一个 AE2 Port”。每个个人、企业或公共账户拥有的是一个受规则约束、可直接
接入 AE2 网络的 **Warehouse Drive**：

```text
账户 / 组织
  <-> JGB Warehouse Drive（账户归属、Cell 槽位、拆装权限、审计）
  <-> AE2 cable / Controller / Security / Terminal
  <-> 真实 AE2 Storage Cell（唯一的长期物品容量）
```

- `Warehouse Drive` 实现 AE2 的 `IChestOrDrive` / `ICellContainer`，而不是
  把物品复制进 JGB 数据库；AE2 Storage Grid 会自动发现它提供的 Cell。
- AE2 扩展容量只来自插入的真实 AE2 Storage Cell。没有 Cell 或 Cell 已满时，
  无法向该实体仓领取或调拨；Base Vault 仍作为有限的起步与恢复缓冲存在。
- JGB 不提供另一套 ME Terminal。玩家继续用 AE2 原生终端、接口和自动化访问
  已连接的网络；这保证材质包物品图标、频道、供电和 Cell 类型规则都保持原生。
- “一个 Minecraft 箱子”应表示初始仓库方块/管理容器的体量，而不是再提供 27
  格独立长期物品栏。推荐初始只给 **1 个 Cell Bay**，通过模块升级到 2、4、10
  槽；10 槽与 AE2 原生 Drive 一致。该数值需要产品确认，不能默认给 27 个 Cell
  槽而跳过 AE2 的成长曲线。

`Warehouse Port` 仍有价值，但职责收窄为市场、跨服调拨等跨资产域操作的短暂
缓冲与恢复点；它不再是普通 AE2 存储的唯一入口。

## 4. 权限、拆装与 AE2 安全边界

JGB 必须管理 Drive 的 **Cell 槽位**，而不管理 Cell 内的每一种物品：

- `VIEW`：查看 JGB 仓库登记、健康和审计摘要。
- `CELL_INSTALL` / `CELL_REMOVE`：插入、移除或更换 Cell；只能通过 Warehouse
  Drive 的受控交互完成，禁止漏斗/管道从槽位侧面抽取。
- `AUTOMATION`：授权市场 Port、调拨 Port 等 JGB 机器执行受审计流转。
- `MARKET_TRANSFER`：从账户仓进入市场托管，或从 `CLAIMABLE` 领取至账户仓。
- `ADMIN`：修改所有权、升级槽位数、冻结/解冻 Drive、执行恢复。

拆下非空 Cell 的推荐规则是：所有者或有 `CELL_REMOVE` 的组织角色可以执行，
但必须满足 Drive 未冻结、没有正在进行的 Port 转移、Cell 不在恢复保留状态；
操作记录 Cell 指纹、操作者和时间。物品不需要先清空，因为它们真实保留在 Cell
内；这是 AE2 玩家最符合直觉的行为。方块破坏、扳手拆卸和库存 GUI 都必须走同一
服务端权限检查，不能仅靠客户端隐藏按钮。

**重要限制：** 接入同一 AE2 网络后，原生 ME Terminal 的读写权限由 AE2
Security Grid 的 `INJECT` / `EXTRACT` 等权限控制。JGB 无法在普通线缆上逐项
拦截每个原生 AE2 终端操作。因此：

- 个人仓 v1 以方块物理归属和玩家私有 AE2 网络为基础。
- 企业/公共仓 v1 必须要求接入有 AE2 Security 的网络，并将组织角色映射为建造
  与使用规范；JGB 负责 Drive 槽位、市场/调拨 Port 和审计权限。
- 若未来要求“同一公共 AE 网络中按 JGB 岗位限制每一件物品存取”，必须另做
  Warehouse Gateway / Storage Provider 代理，不能伪称为简单 Drive 接线。

## 5. 受控跨域 Port

```text
Base Vault / 账户 Warehouse Drive / 已登记 AE2 网络
  <-> Warehouse Port（小型物理缓冲）
  <-> 市场托管、跨服调拨或公共资产域
```

`Warehouse Port` 是受控物流边界，不是无限物品容器：

- 只提供小型 Minecraft 物理缓冲容量。
- 每次导入、导出都生成幂等 request id 与审计记录。
- 根据账户权限、端口登记、服务器状态、吞吐限额和物品规则决定是否执行。
- 需要市场交割、跨服调拨或公共资产域审计的物品，才通过该端口进入 JGB 管理
  范围；普通 AE2 网络内存取不经过 JGB 账本。

禁止将任意玩家 ME 网络直接暴露成“远程数据库仓”。这样会绕过企业权限、无法
可靠审计，也会把断电、区块卸载、网络拓扑变化和物品交付不确定性放大为资产
一致性风险。

## 6. 账本只保存什么

JGB 的仓储账本不保存每一个 AE2 单元格的完整内容；仅 Base Vault 按槽位保存完整
ItemStack NBT。其余内容保存：

- 仓库账户：`PERSONAL`、`ENTERPRISE`、`PUBLIC`。
- Base Vault 槽位、版本、预留状态及其完整 ItemStack NBT 快照。
- Warehouse Drive、Cell 槽位、端口、服务器与 AE2 网络的登记关系。
- 授权角色：查看、存入、提取、自动化、市场划转、管理。
- 容量/健康摘要：Drive 在线、Cell 状态、已用/可用容量、物品类型数、端口吞吐
  和最后同步时间。
- 进出流水：操作者、端口、物品摘要、数量、来源/目标域、request id、结果与恢复状态。

Base Vault 的余额真相在其受事务保护的槽位账本中；AE2 物品余额真相仍在已登记
网络中。JGB 仅在明确的受控操作成功后，记录可审计的“已交付”事实；不能根据
GUI 文本、缓存或上一次扫描结果推断资产已经移动。

## 7. 与市场托管的桥接

标准市场继续拥有 `AVAILABLE`、`ESCROW_SELL`、`CLAIMABLE` 等独立托管状态。
个人/企业/公共 AE2 仓与市场之间只能走显式转移操作：

```text
AE2 Warehouse Drive -> Warehouse Port -> 市场 AVAILABLE -> ESCROW_SELL
市场 CLAIMABLE -> Warehouse Port -> AE2 Warehouse Drive
市场 CLAIMABLE -> Base Vault（v1 默认安全领取目标）
```

- 市场出售前，物品必须经 Port 从 AE2 实体库存抽出，再写入市场托管。
- 成交后的领取 v1 优先投递到 Base Vault；满仓时留在 `CLAIMABLE`，不自动重复
  发放。后续可由玩家选择 Port 投递到目标 AE2 Drive；端口/网络不可用同样留在
  `CLAIMABLE`。
- 企业和公共仓使用相同的 Port 协议，但权限依据分别是企业岗位和公共决议。

定制商品市场继续使用单件挂牌交付；汇率市场继续使用任务书硬币兑换，不共享
标准市场的托管状态。

## 8. Phase 0 调查结果与实施前验证

已验证的 AE2 扩展点（参考 `Reference/Applied-Energistics-2-Unofficial`）：

- 原生 `TileDrive` 是 `AENetworkInvTile + IChestOrDrive`，使用内部 Cell inventory；
  原生 Drive 槽数为 10，且仅接受 `cell().isCellHandled(itemStack)` 的真实 Cell。
- `IStorageGrid` 对同时作为 `IGridHost` 的 `ICellContainer` 自动发现，不需要 JGB
  手动注册为独立 Storage Provider。
- `IMEInventory` 支持 `Actionable.SIMULATE` 与实际执行，可用于 Port 在跨数据库
  交割前先检查可注入/可抽取数量；模拟不是长期容量预留，执行仍必须走幂等恢复链。
- AE2 的 I/O Port 是 Cell 迁移设备，不适合作为市场资产的唯一事务边界；JGB 的
  Port 应采用独立短缓冲和 request id，避免 AE 操作与 PostgreSQL 之间出现无记录
  的半完成交割。
- 现有定制市场 `CustomMarketItemSnapshot` 已能压缩、持久化并恢复完整 `ItemStack`
  NBT；Base Vault 可抽出通用的 NBT codec，但不能直接复用“单件挂牌”的业务模型。
- 当前标准/定制市场 `Minecraft*MarketClaimDeliveryPort` 只会向在线玩家背包投递；
  Base Vault 应成为新的优先 delivery port，以消除离线和背包已满造成的交付阻塞。

Phase 0 已完成设计与尖峰验证；下一个实施切片是有限 Base Vault 的正式持久域和
市场交付适配，不开始 AE2 Drive 或 Cell Bay：

1. 建立 Base Vault 槽位账本尖峰：27 槽、完整 NBT 往返、堆叠合并、容量预留、
   离线账户、并发幂等交付和崩溃恢复；确认它不向 AE2 Storage Grid 暴露。
2. 建立最小 `WarehouseDriveTile` 技术尖峰，确认接线、频道、供电、Cell 插拔、
   chunk reload 后的 Storage Grid 刷新均与原生 Drive 一致。
3. 验证 Drive 自定义受控 inventory 能拦住侧面自动化、方块破坏与 GUI 拆装，且
   不影响 AE2 通过网络访问 Cell 内容。
4. 验证对目标 ME inventory 的模拟注入/提取、断电、网络分裂和 Cell 满载时的
   结果，形成市场领取/入托管的恢复状态表。
5. 以 Base Vault 替换现有“直接投背包”交付适配器的默认目标，保留玩家背包作为
   显式取出目标；验证市场、奖励、定制市场的失败不重复发放。
6. 明确 Tier 0 Cell Bay 数、升级槽位、个人/企业/公共账户归属，以及企业是否把
   原生 AE2 Security 作为 v1 强制前置条件。

## 9. v1 容量与玩法边界

- 容量由 AE2 Storage Cell 真实决定；JGB 不额外售卖“无限格子”。
- 每个账户另有固定 Base Vault：个人 27 格，企业和公共账户各 54 格。它是起步与
  交割保障，不随数据库行数、账户等级或物品种类隐式膨胀。是否允许后续购买第二个
  Base Vault 是独立玩法决策，v1 默认不允许。
- JGB 可限制账户绑定 Drive 的 Cell Bay 数、端口数、自动化吞吐或账户级别，以形成
  职业、企业与公共设施的成长线。
- 首版只支持可稳定识别的 ItemStack；复杂 NBT、绑定物、容器物和流体必须有
  明确白名单或拒绝原因，不能静默归一化。
- 首版不实现跨服 AE2 远程网络。跨服移动先作为带恢复状态的仓储调拨任务。

## 10. 推荐实现顺序

1. Phase 0：完成 Base Vault 与 Warehouse Drive 尖峰、拆装/网络/模拟验证，不接
   正式市场 UI。
2. 实现 Base Vault 账户、槽位账本、角色权限、终端入口和交付 adapter；先将市场
   领取与奖励可靠投递到 Base Vault。
3. 定义 Drive 登记、Cell Bay 权限、端口登记和操作日志。
4. 实现单个 `Warehouse Port` 的受控 AE2 <-> 市场缓冲、断电与区块不可用恢复。
5. 最后添加企业仓、公共仓、跨服调拨和更高阶自动化权限。

## 11. 非目标

- 不把 PostgreSQL 当作 ME Drive 的替代品。
- 不把 Base Vault 挂进 AE2 网络或把它宣传为无 Cell 成本的 ME 存储。
- 不让终端页面绕过受控 Port 直接抽取 ME 网络物品；普通原生 AE2 使用仍按其
  网络安全规则运行。
- 不把市场托管、企业库存与公共储备混成同一张余额表。
- 不在第一版实现无限远程 ME、跨服实时同步或无人审计的自动发货。
