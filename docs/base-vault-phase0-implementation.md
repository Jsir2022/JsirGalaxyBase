# Base Vault Phase 0 实施记录

日期：2026-07-20

## 已落地的持久边界

- 新增 `warehouse_account`、`warehouse_slot`、`warehouse_operation_log` 迁移。
- 账户以 `PERSONAL`、`ENTERPRISE`、`PUBLIC` 区分；默认容量分别为 27、54、54 格。
- 每个槽位保存完整压缩 `ItemStack` NBT、摘要字段和版本号。服务端按原版最大堆叠数
  合并，不以数据库行数扩容。
- 每次 Vault 交付和出库生成全局唯一 `request_id` 操作日志。已完成请求幂等返回，未完成
  请求拒绝自动重复交付，留给恢复流程处理。

## 已接入的市场路径

- 标准市场领取使用 `VaultMarketClaimDeliveryPort`，目标为个人 Base Vault；Vault 满时市场
  `CLAIMABLE` 不会被标记为已领取。
- 定制市场取消/领取使用 `VaultCustomMarketDeliveryPort`，目标同样为个人 Base Vault。
- 标准市场“存入市场托管”从 Base Vault 提取正式目录商品，并与市场托管写入运行在同一个
  JDBC 事务中；任一步失败会整体回滚，不再存在先扣 Vault 再尽力补回的旁路。
- 定制市场发布改为从指定 Base Vault 槽位提取 1 件并进入 custom escrow；旧的“当前手持
  单件”发布入口不再允许作为资产来源。
- 汇率市场的执行入口改为从指定 Base Vault 槽位提取任务书硬币，随后沿用既有正式报价与
  银行结算。报价浏览不再扫描当前手持物品。

## 仍未接入的边界

- 终端 Vault 页与玩家背包的显式存取适配器尚未接入。它必须先建立 `PROCESSING /
  RECOVERY_REQUIRED` 的物理背包交付协议，不能直接在 UI click handler 修改背包。
- 终端目前已提供 Vault 一级页面与账户容量摘要，但尚未提供完整的格位网格、单击与 Shift
  存取客户端控件。因此定制发布和汇率兑换已接受的 `selectedVaultSlot` 需要由该下一切片
  的 Vault 选择器填写；服务端不会退回猜测当前手持物。
- 奖励的 `VaultDeliveryPort`、企业/公共权限、AE2 Drive、Cell Bay 与实体 Port 均不属于
  本次范围。

## 验证

- `BaseVaultServiceTest` 覆盖个人 27 格、堆叠合并、满仓拒绝、幂等投递和按数量提取。
- 标准市场服务和终端市场服务定向回归随本轮执行。

## 运行前要求

部署该版本前，必须在服务端停服窗口执行 `scripts/db-migrate.sh`，确认
`20260720_002_add_base_vault.sql` 已记录在 `schema_migration_history`。运行时会校验三张
仓储表；缺表时市场运行时会拒绝初始化，而不是静默回退到玩家背包。
