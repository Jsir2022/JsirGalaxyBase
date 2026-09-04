# 个人地皮 PostgreSQL 与保护运行时 v1

日期：2026-08-22

## 当前能力

Batch B 已把个人地皮从进程内尖峰升级为服务端 PostgreSQL 真源。正式结构由
`ops/sql/migrations/20260822_001_add_personal_land.sql` 创建，应用启动只校验，不自动建表。

- `land_title` 保存本服、维度、区块、个人 UUID、状态和版本；
- `land_operation_log` 保存幂等请求、语义、结果和结构化前后产权快照；
- 放弃产权写为 `REVOKED` 并递增版本，不删除审计历史；
- 未撤销产权使用部分唯一索引，同一区块只能有一个当前产权；
- 请求、个人配额和区块通过事务级 advisory lock 串行化；
- 本服有效产权在启动时一次性装入不可变保护快照，事件热路径不查询数据库。

## 配置

服务端 `jsirgalaxybase-server.cfg` 的 `land` 分类：

```text
landEnabled=false
landProtectionMode=SHADOW
landMaxClaimsPerPlayer=4
landBlockedDimensions=[]
landReservedChunks=[]
landAllowFakePlayers=false
```

`landReservedChunks` 每项使用 `dimension:chunkX:chunkZ`，server ID 始终来自
`bankingSourceServerId`。启用时若模式、保留区格式、共享 PostgreSQL 或 schema 无效，服务端
拒绝启动地皮运行时，不会静默退回无保护状态。

## 保护边界

`SHADOW` 只记录本应拒绝的限频日志和分类计数；`ENFORCE` 才取消事件。当前矩阵包括方块
破坏/放置、方块和有目标物品交互、非玩家实体攻击、爆炸方块过滤与假玩家。玩家 PvP 不由
地产规则改变。假玩家默认拒绝，全服允许开关只是迁移期兼容措施，不代表已实现按机器授权。

Batch C 已增加终端认领/放弃页面，但仍没有命令、挂牌和交易入口。也不承诺阻止绕过 Forge 事件的 Mod 机器、跨界
活塞或自然流体传播。因此 Lobby 只进入 SHADOW；人工认领与兼容矩阵完成前不移除
ServerUtilities。

## 2026-08-22 灰度状态

- migration 已应用到共享数据库，当前 `land_title=0`、`land_operation_log=0`；
- Lobby 配置为 `landEnabled=true`、`landProtectionMode=SHADOW`、假玩家拒绝；
- Lobby 启动日志确认保护运行时加载 0 个有效产权，并到达 `Done (1.380s)!`；
- Lobby 与客户端使用同一构建 JAR，客户端未启动；
- Lobby 和客户端的 ServerUtilities 均继续保留，S1/S2 未修改。

## 2026-08-23 Batch C 终端合同

- `PROPERTY / 地产` 是独立顶层路由，不进入标准市场订单簿；
- “附近地皮”由服务端按当前玩家维度与区块生成 15x15 结构化网格，客户端不能指定玩家、服务器或维度；
- “我的地皮”使用每页 4 条的服务端分页元数据，11 条记录明确为 3 页；
- 网格外的任意目标会回到玩家当前区块，只有服务端证明属于当前玩家的产权才能从“我的地皮”跨距离选中；
- claim/unclaim 复用 Batch B 的 requestId 幂等、产权归属和版本校验，客户端确认框只是意图确认，不替代服务端复核；
- 页面资源提供 `zh_CN` 与 `en_US`；人工保护矩阵完成前 Lobby 仍只能使用 `SHADOW`。

## 2026-08-24 Batch C.1 地图合同

- `附近地皮` 不再传输固定 225 个状态字符串，而是返回服务端限幅后的视口中心、缩放级别
  和最多 31x31 范围内的稀疏产权/保留区记录；未认领区块隐式表示。
- 客户端地形来自当前世界已经加载的区块，不进入 PostgreSQL、操作日志或网络快照；服务端
  不接受客户端提交的地形状态或“已加载”证明。
- 客户端可以在玩家周围 16 区块内移动视口，但 `LAND_CLAIM` 必须再次按玩家实时区块校验
  半径 7。越界请求返回 `OUT_OF_RANGE`，不得回退为认领玩家脚下区块。
- 旧六段 `TerminalLandActionPayload` 继续解码一版，默认中等缩放；新客户端使用带视口中心
  和缩放枚举的九段合同。服务端与客户端 JAR 必须同步部署。
