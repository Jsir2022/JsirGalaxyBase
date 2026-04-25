# Server Tools / Cluster 第一期命令与表结构说明

日期：2026-04-11

这份文档记录当前 `modules.cluster` 与 `modules.servertools` 第一期已经落地的命令格式、跨服边界和数据库落点。

## 当前命令格式

### home

- `/home`
  - 传送到默认 home 名 `home`
- `/home list`
  - 列出当前玩家已保存的 home
- `/home set <name>`
  - 用当前所在位置覆盖或创建指定 home
- `/home delete <name>`
  - 删除指定 home
- `/home <name>`
  - 传送到指定 home

说明：

- 当前 home 直接落 PostgreSQL `player_home`
- home 目标已包含 `server_id + dimension + 坐标 + 朝向`
- 如果目标在当前服，会直接本服传送
- 如果目标在其他服，会创建 `cluster_transfer_ticket`，由源服通过 `BungeeCord-style` gateway adapter 请求代理切服，并在目标服完成最终落点恢复

### back

- `/back`
  - 回到最近一次有效传送前的位置

说明：

- 当前 back 只保留“最近一次有效回退点”
- 回退记录直接落 PostgreSQL `player_back_record`
- `home / back / spawn / tpa / rtp / warp` 进入主链时都会刷新 back 记录

### spawn

- `/spawn`
  - 传送到当前 world 的出生点

说明：

- 当前语义保持为“当前 world 出生点”
- 实现上已经进入统一传送主链，会写 back 记录

### tpa

- `/tpa <playerName>`
  - 向当前服上的目标玩家名发起请求，默认目标服为当前服
- `/tpa <playerName> <targetServerId>`
  - 向指定目标服上的玩家名发起跨服请求
- `/tpa accept <playerName>`
  - 接受来自指定玩家名的请求

说明：

- 当前 `tpa` 请求直接落 PostgreSQL `player_tpa_request`
- 请求里会保存请求者原始落点，用于后续 `back`
- `targetServerId` 现在要求必须是 `cluster_server_directory` 中已注册且 enabled 的 serverId；校验失败会直接报错，不会写入请求记录
- 当前已支持“跨服请求语义 + 数据库存储 + 真实 gateway 派发 + 目标服落点恢复”
- 跨服接受后，源服会把 `cluster_transfer_ticket` 推进到 `DISPATCHED`，等待玩家抵达目标服后恢复最终落点

### rtp

- `/rtp`
  - 在当前 world 内寻找一个安全随机落点并传送

说明：

- 当前是第一期最小可运行版本
- 暂未引入维度白名单和冷却配置文件
- 本次会把随机落点写入 PostgreSQL `player_rtp_record`

### warp

- `/warp`
  - 列出当前所有系统 warp
- `/warp list`
  - 同上
- `/warp <name>`
  - 传送到指定 warp

说明：

- warp 直接落 PostgreSQL `server_warp`
- 当前只实现“系统/制度维护的 warp 可传送链路”
- warp 没有玩家创建命令；建议由 SQL/migration 或后续制度管理链维护

## 数据表落点

本期新增并使用下面这些 PostgreSQL 表：

- `cluster_server_directory`
  - 服务器目录
- `cluster_transfer_ticket`
  - 跨服传送票据
- `player_home`
  - 玩家 home
- `player_back_record`
  - 最近一次有效回退点
- `server_warp`
  - 系统 warp
- `player_tpa_request`
  - tpa 请求与超时状态
- `player_rtp_record`
  - rtp 记录

正式 DDL 见：

- `servertools-cluster-postgresql-ddl.sql`

正式 migration 入口见：

- `../scripts/db-migrate.sh`

## 当前跨服结论

当前结论必须区分三种状态：

- 已完成：
  - `home / back / spawn / tpa / rtp / warp` 的数据库真源、命令主链、服务链、仓储链和本服传送链
- 已完成并可观测：
  - 目标服不等于当前服时，源服会创建 `cluster_transfer_ticket`，通过 `BungeeCord` 插件消息请求代理切服，并把 ticket 推进到 `DISPATCHED`
  - 目标服 dedicated server 会在玩家登录和定时 tick 中扫描属于自己的待恢复 ticket，匹配玩家后执行最终落点恢复，并把 ticket 推进到 `COMPLETED`
  - 过期 ticket 会被推进到 `EXPIRED`；目标服恢复异常会把错误消息写回 `status_message`，保留下一次 tick 重试语义
- 可暂时忽略：
  - 第一阶段未实现 GUI、未实现玩家自建 warp、未实现复杂权限制度判定与在线会话目录

这意味着：

- 现阶段已经不再是“写 ticket + pending”的占位链，而是“源服派发 -> 目标服到达恢复 -> ticket 完成/过期”的最小闭环
- 真实跨服切服仍然依赖外部代理链路，当前默认按 `gateway_endpoint` 或 `server_id` 走 `BungeeCord-style` `Connect` 子通道

## 当前 ticket 生命周期

- `PENDING_GATEWAY`
  - 源服已创建 ticket，尚未把切服请求送入代理
- `DISPATCHED`
  - 源服已向代理发送切服请求，等待玩家进入目标服并恢复最终落点
- `COMPLETED`
  - 目标服已匹配到玩家并成功完成最终落点恢复
- `EXPIRED`
  - ticket 在有效期内未完成目标服恢复，已被 dedicated server 清理
- `FAILED`
  - 源服 gateway 派发本身失败，例如玩家已不在线或代理消息发送失败