# 标准商品目录 v1

## 制度边界

标准商品市场的正式准入来源是 `standardized_market_catalog`，不是玩家手持物品、订单簿、成交记录或托管资产。

- `AVAILABLE`：玩家已存入市场托管，可创建卖单或即时卖出。
- `ESCROW_SELL`：已被卖单锁定，不能再次出售。
- `CLAIMABLE`：买入成交后等待领取到背包。
- 冻结资金：买单的资金预留；撤单或未成交部分按订单链路返还。

手持物品只用于将已准入商品存入托管的快捷入口。未列入目录的物品必须被拒绝，不能因玩家持有或数据库出现过历史记录而自动进入标准市场。

## 表字段

`standardized_market_catalog` 的关键字段：

- `product_key`：`modid:itemid:meta`，主键。
- `registry_name`、`meta`：用于精确匹配真实 `ItemStack`。
- `display_name`、`unit_label`、`reference_price`、`stackable`：终端展示、参考行情和计量信息。参考价只用于目录与行情提示，不保证成交价。
- `enabled`、`sort_order`：管理员控制准入与目录排序。
- `catalog_version`、`category_code`、`admission_basis`、`source_entry_label`：制度与审计说明。

迁移会把既有订单、托管和成交中出现的商品写入目录，标记为“由既有标准市场记录迁移，待管理员复核”。新服需要管理员显式录入并启用商品。

当前灰度目录已完成第一批 8 个商品的正式化：铁锭、金锭、钢锭、铝锭、铜锭、银锭、铁板和钢板。展示名、单位、排序和参考价均由版本化迁移维护；其中参考价来自当时已存在的最近成交记录，只用于行情提示，不替代订单簿的真实成交价。

## 管理示例

```sql
INSERT INTO standardized_market_catalog (
  product_key, registry_name, meta, display_name, unit_label, reference_price, stackable, enabled, sort_order,
  catalog_version, category_code, admission_basis, source_entry_label
) VALUES (
  'gregtech:gt.metaitem.01:11305', 'gregtech:gt.metaitem.01', 11305,
  'Steel Ingot', 'ingot', 102, TRUE, TRUE, 100,
  'standardized-market-catalog-db-v1', 'metal', '管理员目录准入', '运营目录'
)
ON CONFLICT (product_key) DO UPDATE SET
  display_name = EXCLUDED.display_name,
  unit_label = EXCLUDED.unit_label,
  reference_price = EXCLUDED.reference_price,
  enabled = EXCLUDED.enabled,
  sort_order = EXCLUDED.sort_order,
  updated_at = now();
```

停用商品不会删除历史订单或资产；管理员应先处理开放订单与托管资产，再将 `enabled` 置为 `FALSE`。

### 管理操作

```sql
-- 上架或恢复目录商品。商品随后可浏览；玩家手持同一 registry/meta 时才可存入托管。
UPDATE standardized_market_catalog
SET enabled = TRUE, sort_order = 140, reference_price = 96, updated_at = now()
WHERE product_key = 'minecraft:iron_ingot:0';

-- 停用只阻止新的目录浏览与存入；先人工清理该商品的开放订单和托管资产。
UPDATE standardized_market_catalog
SET enabled = FALSE, updated_at = now()
WHERE product_key = 'minecraft:iron_ingot:0';

-- 审核仍使用技术键的历史回填条目，补全玩家可读名称、单位、排序和参考价。
SELECT product_key, display_name, unit_label, reference_price, enabled, sort_order
FROM standardized_market_catalog
ORDER BY sort_order, product_key;

-- 正式目录不应再以 product_key 作为展示名。该查询应返回 0 行。
SELECT product_key
FROM standardized_market_catalog
WHERE enabled = TRUE
  AND display_name = product_key;
```

## 无客户端自检

部署后使用：

```bash
scripts/market-smoke-test.sh --target lobby
```

脚本验证正式目录、数据库已有交易数据、服务器完成启动以及运行时目录诊断。它用于判断目录、数据库和服务端是否接通；终端布局和鼠标交互仍由游戏内人工验收。

交易、托管与恢复状态使用下面的只读审计脚本检查：

```bash
scripts/market-audit.sh --strict
scripts/market-audit.sh --strict --player <玩家 UUID>
```

严格模式会在启用目录仍有技术展示名、玩家开放卖单没有 `ESCROW_SELL` 托管、活动买单没有足额“剩余限价本金 + 最大买方手续费”预留、订单预留总额缺少银行冻结覆盖、陈旧 `CLAIMING`、`EXCEPTION` 托管，或有未收口市场操作时失败。历史 `demo-market-*` 演示流动性会单独报告而不使严格模式失败，但它不具备正式结算条件，不能继续充当可交易深度。需要本地灰度测试时，用 `scripts/market-demo-fixture.sh --apply` 将这些旧行隔离，并创建一组显式带托管、足额费用预留和受管测试账户的双边流动性。脚本不执行玩家资产恢复；恢复仍由管理员命令 `/jsirgalaxybase market recover` 明确触发。
