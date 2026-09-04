# AE2 银河仓储 v1：个人 Warehouse Drive

> **状态：兼容保留。** 该批的实体 Drive 已曾部署，但新的产品主入口改为
> [`ae2-galactic-warehouse-terminal-bay-v2.md`](ae2-galactic-warehouse-terminal-bay-v2.md) 的终端托管 Cell Bay。
> 不移除既有方块 ID、数据库 migration 或真实数据；不再为 v1 Drive 扩展新功能或新配方。

日期：2026-08-31

## 目标

本批把已验证但未注册的单 Cell `WarehouseDriveTile` 变成正式的**个人** AE2 实体仓入口。它使用
AE2 的真实 Storage Cell、频道、供电和 Storage Grid；JGB 只保存方块归属、登记位置、操作审计和
可恢复的运行状态摘要。

这不是第二个 ME 终端，也不是把 AE2 库存复制进 PostgreSQL。

## v1 范围

- 注册 `Warehouse Drive` 方块与 Tile；只公开第 0 个、真实的 AE2 Cell Bay。
- 放置时由服务端把当前位置登记给放置玩家；非所有者、假玩家、爆炸和未登记方块不得操作或拆除。
- 所有者右键持 Storage Cell 安装；潜行空手右键只在 Cell 确认为空时取出。普通右键不打开另一套
  ME GUI，而是提示玩家从银河终端查看状态与审计。
- 拆除要求所有者且 Bay 为空；被拒绝的安装、取出、拆除同样写入操作审计。
- 终端新增“银河仓储”只读页面，展示本人已登记 Drive、是否所在服/区块已加载、Cell、频道/供电、
  物品/类型摘要以及最近审计。它不读取或列举 ME 物品。
- 仓储运行时只使用 `InstitutionCoreModule` 提供的共享 JDBC 连接；`warehouseEnabled=true` 时缺迁移、
  连接或 AE2 都 fail-fast。

## 真源与数据边界

```text
个人身份 --服务端校验--> warehouse_drive 登记/所有权/版本
     |                              |
     |                              +--> warehouse_drive_operation 审计
     v
Warehouse Drive -> 真实 AE2 Storage Cell -> 真实 AE2 Storage Grid
```

- `warehouse_drive` 是方块登记和所有权真源；坐标以本服 ID、维度、方块坐标唯一标识活跃 Drive。
- `warehouse_drive_operation` 记录服务端生成的 request id、动作、操作者、结果和状态摘要。它不是物品
  账本，不记录 Cell 的完整物品内容。
- AE2 Cell 内物品的真源永远是 Cell NBT / 已连接 AE2 网络。
- Base Vault 继续是独立 PostgreSQL 槽位账本，绝不实现 `ICellContainer`、绝不挂入 Storage Grid。

## 明确不做

- 不实现 Warehouse Port、市场自动投递、AE2 到市场入托管、跨服 AE 网络或远程 ME 访问。
- 不实现企业仓、公共仓、Team 授权、职业权限、Cell Bay 升级或新的 ME Terminal。
- 不把普通 AE2 网络内的每次存取写进 JGB 审计，也不尝试拦截原生 ME Terminal。
- 不对 Cell 非空时的方块破坏做“自动搬出”或删除；必须由所有者先安全取出空 Cell。

## 后续 Batch：Warehouse Port

Port 才是跨资产域边界：`市场 CLAIMABLE -> Port -> 指定 Drive` 与
`Drive -> Port -> 市场 AVAILABLE`。它必须使用独立 request id、模拟后执行、断电/区块未加载恢复状态与
物品准入策略；本批 Drive 不预先实现或暴露这些动作。

## 验收

- 一 Bay 之外的 Cell 永远不可插入或侧向访问。
- 所有操作都以服务端注入的玩家 UUID、本服 ID 和方块坐标校验；客户端不能指定所有者。
- 非所有者、假玩家、Cell 非空拆除和爆炸均不能让 Drive 或 Cell 丢失。
- 终端只读快照不会加载未加载区块，也不传输 ME 地形或物品清单。
- 自动测试覆盖服务层、所有权、审计、Cell Bay、注册/移除和终端快照；真实 AE2 接线、频道、供电、
  chunk reload 与原生终端互操作仍留给最后单人世界事件矩阵验证。
