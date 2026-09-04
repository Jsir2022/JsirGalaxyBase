# 物品准入策略 v1

日期：2026-08-30

## 目标与边界

本模块是 `BanItem / ItemBlacklist` 能力的 JGB 服务端适配，不复制其客户端提示、物品扫描或自动删除行为。
它仅在受控资产域的**进入点**拒绝物品，并保存拒绝审计：

- 标准市场托管：`MARKET_CUSTODY`；
- 定制单件挂牌托管：`CUSTOM_MARKET_ESCROW`；
- Base Vault 原生容器的存入：`BASE_VAULT`；
- 为未来受审计地产自动化预留：`LAND_AUTOMATION`，本批不挂 Forge 自动化事件。

普通背包、已存在的 Vault 内容、已存在的市场托管和世界方块均不被扫描、删除、转换或自动回滚。策略不是反作弊系统，也不替代地产保护或 AE2 安全网格。

## 身份和规则

服务端以真实 `ItemStack` 的注册名和 metadata 判定；不读取显示名，也不依赖客户端字符串。规则只在服务器配置
`jsirgalaxybase-server.cfg` 中出现：

```text
itemPolicyEnabled=false
itemPolicyRules <
    sample-deny|MARKET_CUSTODY,CUSTOM_MARKET_ESCROW|modid:item:0
>
```

单条语法严格为：

```text
rule-id|SCOPE,SCOPE|modid:item[:meta]
```

- 缺失 `meta` 表示该物品的全部 metadata；
- scope 仅可为 `MARKET_CUSTODY`、`CUSTOM_MARKET_ESCROW`、`BASE_VAULT`、`LAND_AUTOMATION`；
- 启用时，空规则、重复规则 ID、未知 scope、无效 matcher、负 metadata 都会在启动前 fail-fast；
- 默认关闭且空规则，部署 JAR 不会改变现有物品行为。

本批不提供公共“完整禁用表”。实际规则必须由运营方按 Mod、物品和资产域逐项确认后配置；先灰度 `itemPolicyEnabled=false`，再应用 migration、配置少量规则并开启。

## 审计与故障语义

启用策略必须同时具备 InstitutionCore 的共享 PostgreSQL 连接和 `item_policy_audit` 表，否则服务端启动失败，禁止静默降级成未审计的允许模式。拒绝事件写入：

- 本服 `source_server_id`；
- 服务端注入的玩家引用；
- scope、操作名、注册名、metadata、规则 ID；
- 固定 `DENIED` 决策和时间。

对应 migration 为 `ops/sql/migrations/20260830_001_add_item_policy_audit.sql`；其中按玩家/时间和规则/时间建立索引，供终端诊断页或管理审计查询使用。

## 终端诊断页

终端的“物品策略”页只读展示本服当前已解析的规则，以及当前玩家在本服最近 12 条拒绝记录。每条记录包含资产域、入口动作、稳定物品键、metadata、匹配规则和时间；查询条件固定为服务端注入的当前玩家与本服 ID，不能借由客户端参数查看其他玩家或其他服务器。

策略未启用或服务端运行时不可用时，页面明确显示未启用状态。审计读取暂时失败时，页面显示可恢复的诊断提示，而非将策略静默改成允许。该页面不扫描背包、不修改物品、不提供批量清理操作。

## 当前接入链

```text
Base Vault -> 标准市场托管      : 取物前判定，拒绝时 Vault 不变
Base Vault -> 定制挂牌托管      : 取物前判定，拒绝时 Vault 不变
玩家背包 -> Base Vault 容器     : 变更提交前仅检查新增/增加的物品，拒绝后恢复原背包/Vault 快照
```

最终写入仍由既有 Base Vault / 市场事务处理；策略不创建第二套库存、订单或转移账本。

## 自动化验证

- 纯策略：解析、范围、metadata、重复/未知规则、禁用状态和“先审计再拒绝”；
- 既有 Vault、Terminal 市场与地产配置回归；
- PostgreSQL 集成测试（有 `JGB_BANKING_IT_*` 时启用）：缺表 fail-fast、拒绝审计落库，以及按本服/当前玩家隔离并按时间倒序的终端审计查询。

后续可扩展：地产自动化调用、Warehouse Port/AE2 转移调用，以及审计保留期；不在本批添加自动重试、物品删除、批量清理或客户端绕过开关。
