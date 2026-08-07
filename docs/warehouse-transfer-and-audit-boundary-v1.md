# 仓库转移与审计边界 v1

日期：2026-07-19

本文把市场托管仓、未来玩家仓、企业仓、公共仓之间的边界收口为可实现的转移合同。
它不是新 GUI 需求，也不在本轮创建企业或政府库存表；它规定未来实现不能绕开的资产真相、授权与审计约束。

## 1. 四类库存不是同一个表的不同标签

| 资产域 | 真正所有者 | 控制者 | 主要用途 | 当前状态 |
| --- | --- | --- | --- | --- |
| 玩家背包 | 在线玩家 | 在线玩家 | 物理持有、生产、消费 | Minecraft `ItemStack` |
| Base Vault | 个人、企业或公共账户 | 账户权限 | 有限起步仓、市场领取、奖励缓冲 | `warehouse_account` / `warehouse_slot` |
| 市场托管仓 | 玩家或成交受益人 | 市场规则 | 标准商品挂单、撮合、领取 | 已实现 `market_custody_inventory` |
| 企业仓 | 企业实体 | 企业授权成员/岗位 | 原料、成品、调拨、待售 | 未来实现 |
| 公共仓 | 公共实体 | 被授予的公共岗位 | 工程、福利、储备、稳定价格 | 未来实现 |

原则：市场托管仓不兼任企业仓或公共仓。企业/公共资产只有经过明确转移，才可成为市场可交易资产；市场成交资产也必须经过明确领取或调拨，才可离开市场托管。

## 2. 资产标识与余额真相

- 标准商品：`product_key + registry_name + meta + stackable`，必须先通过正式目录准入。
- 非标准商品：只由定制挂牌 `ItemStack` 快照表示，不进入标准商品托管余额。
- 每种持久资产都记录 `owner_type`、`owner_ref`、`custody_domain`、`status`、`quantity` 与最近一次 `operation_id`。
- 余额绝不从 GUI 文本、玩家背包扫描结果或订单推导；查询只能读取对应域的持久账本。背包仅在转入/转出边界作为物理交付端。

## 3. 允许的转移矩阵

| 来源 | 目标 | 允许条件 | 结果 | 审计类型 |
| --- | --- | --- | --- | --- |
| 玩家背包 | Base Vault | 玩家本人、Vault 有空位 | 记录后存入 | `PLAYER_TO_BASE_VAULT` |
| Base Vault | 玩家背包 | 玩家本人、目标背包可接收 | 记录后取出 | `BASE_VAULT_TO_PLAYER` |
| Base Vault | 市场托管仓 | 正式目录准入、玩家本人 | 创建 `AVAILABLE` | `BASE_VAULT_TO_MARKET_DEPOSIT` |
| 市场托管仓 | Base Vault | `CLAIMABLE`、Vault 有空位 | 标记 `CLAIMED` | `MARKET_CLAIM_TO_BASE_VAULT` |
| 市场 `AVAILABLE` | 市场 `ESCROW_SELL` | 卖单校验 | 创建/锁定卖单 | `MARKET_SELL_ESCROW` |
| 市场 `ESCROW_SELL` | 市场 `AVAILABLE` | 撤单或未成交余量 | 解锁卖方资产 | `MARKET_ESCROW_RELEASE` |
| 企业仓 | 市场托管仓 | 企业岗位授权、目录准入 | 企业出库后创建 `AVAILABLE` | `ENTERPRISE_TO_MARKET_DEPOSIT` |
| 市场托管仓 | 企业仓 | 企业收货授权、`CLAIMABLE` | 市场领取到企业库存 | `MARKET_CLAIM_TO_ENTERPRISE` |
| 公共仓 | 市场托管仓 | 公共岗位授权、政策/预算规则 | 公共出库后创建 `AVAILABLE` | `PUBLIC_TO_MARKET_RELEASE` |
| 市场托管仓 | 公共仓 | 公共采购/收储决议、`CLAIMABLE` | 市场领取到公共库存 | `MARKET_CLAIM_TO_PUBLIC` |

禁止的路径：企业仓与公共仓不能直接改写市场 `AVAILABLE` / `ESCROW_SELL` / `CLAIMABLE`；任何“从市场直接扣库存供工程使用”的实现都必须先走 `MARKET_CLAIM_TO_PUBLIC`。

## 4. 统一转移合同

未来每一笔跨域转移均应实现为一个 `InventoryTransferOperation`，至少包含：

```text
requestId              幂等键，全链路唯一
operationId            持久操作日志 ID
sourceDomain           PLAYER_BAG / BASE_VAULT / MARKET_CUSTODY / ENTERPRISE / PUBLIC
targetDomain           同上
ownerType + ownerRef   资产受益人
operatorRef            发起人或系统操作员
authorityRef           企业岗位、公共决议或系统规则依据
productSnapshot        标准品键或定制 ItemStack 快照摘要
quantity               正数且按商品计量规则校验
sourceServerId         发起服务器
correlationId          关联订单、公共工程或企业调拨的追踪键
status                 CREATED / PROCESSING / COMPLETED / FAILED / RECOVERY_REQUIRED
```

`requestId` 必须同时进入市场操作日志、银行/结算记录（如有）和目标仓审计。重复请求只能返回同一最终结果，不能再次移动资产。

## 5. 物理物品边界与恢复原则

Minecraft 背包和 PostgreSQL 不能共享原子事务。因此所有涉及真实 `ItemStack` 的转移必须遵守：

1. 先持久化 `CREATED/PROCESSING` 操作和完整物品快照，再修改背包。
2. 对“明确未交付”失败，恢复原持久状态或原背包，并写明恢复结果。
3. 对进程中断、网络断线或无法证明物品是否交付的情况，必须改为 `RECOVERY_REQUIRED`，禁止自动再次交付。
4. 只有在审计能证明资产已到目标域时才能完成；否则管理员用显式 `restore` 或 `complete` 决议收口，决议本身也必须留痕。

标准市场领取、定制挂牌交付和汇率市场任务书硬币兑换都应逐步统一到这条规则；它们的资产模型不同，但不确定交付的处理原则相同。

## 6. 实施顺序

1. 保持当前标准市场托管、定制交付和汇率兑换各自独立，补齐其操作日志与恢复覆盖。
2. 创建企业仓之前，先落企业实体、成员岗位和独立库存账本；不复用 `market_custody_inventory`。
3. 创建公共仓之前，先落公共实体、预算/决议引用和独立库存账本；不复用企业所有权字段。
4. 三种持久仓均稳定后，再抽取只负责跨域操作与审计的转移服务；此时才考虑统一资产总览页。

## 7. 验收条件

未来任意仓库功能上线前，必须能回答：

- 资产的当前域、受益人和控制者分别是谁？
- 此次转移由谁发起，凭哪项权限或决议？
- 物品、数量、来源、目标与 request id 是否可追溯？
- 发生中断时，系统是否会停止自动重复交付并给出恢复记录？

任何一个答案缺失，就不能把该功能称为“仓库”或接入市场结算。
