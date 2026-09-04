# 定制商品市场最小挂牌链 v1 模型说明

这份文档只说明 `定制商品市场最小挂牌链 v1` 的正式模型，不讨论后续 GUI、总入口拆分或完整物流系统。

## 为什么它不属于标准商品订单簿

- 标准商品市场处理的是可目录化、可统一计量、可批量撮合的标准化标的
- 定制商品市场处理的是单件、带 NBT、二手、非标商品
- 当前最小挂牌链 v1 明确收紧为“从个人 Base Vault 单格托管的一件物品”，不接受一个挂牌里挂出多件堆叠物
- 因此定制商品市场不复用 `market_order` / `market_custody_inventory` 作为主链，而是有自己独立的挂牌、快照、成交、审计表

## 最小正式对象

- `CustomMarketListing`
  - 代表一条定制商品挂牌
  - 核心字段：卖家、买家、价格、币种、挂牌状态、交付状态、sourceServerId
- `CustomMarketItemSnapshot`
  - 保存发布时的物品快照
  - 核心字段：itemId、meta、stackSize、displayName、nbtSnapshot
- `CustomMarketTradeRecord`
  - 保存成交留痕与成交时的交付状态
- `CustomMarketAuditLog`
  - 保存 requestId 幂等保护和业务审计痕迹

## 最小状态机

- 发布挂牌
  - `listingStatus=ACTIVE`
  - `deliveryStatus=ESCROW_HELD`
- 买家购买
  - 货币通过银行冻结并结算
  - `listingStatus=SOLD`
  - `deliveryStatus=BUYER_PENDING_CLAIM`
- 买家领取
  - 根据成交快照把单件物品投递到买家个人 Base Vault
  - `listingStatus=SOLD`
  - `deliveryStatus=COMPLETED`
- 卖家下架未成交挂牌
  - `listingStatus=CANCELLED`
  - `deliveryStatus=CANCELLED`

## 交付异常与恢复边界

- 安全可判定的投递失败会恢复到原先可领取/可返还状态，不能凭失败回执把物品吞掉或重复发放。
- 投递结果未知、或投递后写入完成状态失败时，记录保持锁定的异常态；玩家不能盲目重复领取。
- 管理员只能使用已有受审计的“确认完成 / 恢复返还”动作处理未知态；本版不自动重试，也不建立第二套物品账本。

## 第一版交付语义

- 第一版采用“发布即托管快照，购买后进入买家待领取，领取后正式完结”语义
- 因为这条链属于定制商品市场，所以不会落入标准商品市场的 `CLAIMABLE` 资产语义
- 卖家侧 `pending` 视图只展示“已售但仍待买家领取的待完结记录”
- 买家完成 `claim` 后，卖家侧和买家侧的 `pending` 记录都应消失，不再长期堆积

## 当前玩家入口与兼容命令

银河终端的定制市场页是正式玩家入口；以下命令保留用于兼容联调与管理员恢复：

- `/jsirgalaxybase market custom list hand <price>`
- `/jsirgalaxybase market custom browse [limit]`
- `/jsirgalaxybase market custom inspect <listingId>`
- `/jsirgalaxybase market custom buy <listingId>`
- `/jsirgalaxybase market custom claim <listingId>`
- `/jsirgalaxybase market custom cancel <listingId>`
- `/jsirgalaxybase market custom pending`
