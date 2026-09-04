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
- `/rtp <targetServerId>`
  - 仅当服务器配置了该目标服的 RTP 范围时可用；源服创建既有 transfer ticket，目标服在玩家抵达后才
    从自己的目标维度挑选安全随机落点。

说明：

- 无参数时保持本服随机传送。指定目标服时，源服绝不读取或加载远端世界；实际候选由目标服完成。
- `player_rtp_record` 现在保存唯一 requestId、源服和实际目标服；目标服解析重复 ticket 时按 requestId
  幂等写入同一审计行。
- 尚不包含费用、冷却、自动找服或按玩家动态白名单。

## 2026-08-31 全局 Hub / Spawn 与目标服 RTP

`jsirgalaxybase-server.cfg` 的 `global_entry` 分类引入两个服务端规则，默认都为空，不改变现有行为：

```text
globalHubTarget=serverId|dimension|x|y|z|yaw|pitch
targetServerRtpProfiles <
    serverId|dimension|centerX|fallbackY|centerZ|minDistance|maxDistance
>
```

- `globalHubTarget` 非空时，`/spawn` 使用它作为全局 Hub；为空时仍使用当前世界出生点。两者都走
  `PlayerTeleportService -> ClusterTeleportService -> transfer ticket`，并写入原有 back 记录。
- 每个 `targetServerRtpProfiles` 条目定义一个可选目标服 RTP 区域。源服先要求目标 serverId 已在目录中
  且 enabled，再创建 RTP ticket；目标服匹配 ticket 后才在自己的已运行维度内选择安全方块，记录最终坐标
  并完成 ticket。`fallbackY` 仅是 ticket 的持久落点占位，不是远端随机坐标的伪造结果。
- 配置条目格式、重复 serverId、非有限数、非法距离范围均会在 ServerTools 启动时拒绝。客户端不持有
  Hub、目标服身份、区域范围或候选坐标的权威输入。
- migration 为 `20260831_002_add_global_entry_rtp_audit.sql`；必须先应用再部署，因为运行时会校验
  `player_rtp_record.request_id` 与 `target_server_id`。历史 RTP 行以 `legacy-rtp-<recordId>` 补齐 requestId，
  目标服默认回填为原 sourceServerId，保留原有审计含义。

本批刻意不实现全局自动找服、收费、warmup/cooldown、移动取消或客户端预生成地图。真实代理 Connect、
多账号重连和目标维度加载仍需最后的多人灰度验收。

### servertools / warp

推荐验收入口：

- `/jgbst warp list`
  - 列出当前所有系统 warp
- `/jgbst warp <name>`
  - 传送到指定 warp
- `/jsirgalaxybase servertools warp list`
  - 主命令下的同等入口
- `/jsirgalaxybase servertools warp <name>`
  - 主命令下的同等入口

兼容保留入口：

- `/warp list`
- `/warp <name>`

GTNH 整合包内可能已有其他模组接管 `/warp` 裸命令；跨服 warp 灰度测试请以 `/jgbst warp ...` 或 `/jsirgalaxybase servertools warp ...` 为准。

说明：

- warp 直接落 PostgreSQL `server_warp`
- 当前只实现“系统/制度维护的 warp 可传送链路”
- warp 没有玩家创建命令；建议由 SQL/migration 或后续制度管理链维护
- `jgbst` 命名空间入口复用原有 `PlayerTeleportService.prepareWarpTeleport` 与 cluster dispatch 主链，不复制传送业务逻辑

## Warp 灰度手动验证

游戏内命令：

- `/jgbst warp list`
- `/jgbst warp s2test`
- `/jgbst warp lobbytest`
- `/jsirgalaxybase servertools warp list`
- `/jsirgalaxybase servertools warp s2test`

数据库观测 SQL：

```sql
SELECT warp_name, server_id, enabled
FROM server_warp
ORDER BY warp_name;

SELECT ticket_id, request_id, player_name, source_server_id, target_server_id, status, status_message, created_at, updated_at
FROM cluster_transfer_ticket
ORDER BY created_at DESC
LIMIT 20;
```

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

## 2026-08-28 代码框架复核与下一批实施范围

### 已确认的真实调用链

当前跨服传送不是单条代理消息，而是可恢复的两阶段链路：

1. 命令从服务端在线玩家构造 `TeleportActor`，客户端不提交玩家身份。
2. `PlayerTeleportService` 校验业务规则、保存 home/back/TPA/RTP 数据并生成调度计划。
3. `ClusterTeleportService` 校验服务器目录；本服直接执行，跨服先以 `requestId` 创建
   `cluster_transfer_ticket`。
4. `BungeeCordGatewayAdapter` 只发送标准 `Connect` 子通道，请求代理把当前在线玩家切到目标服。
5. 目标服 `PlayerArrivalRestoreService` 在玩家登录和 tick 时按本服 ID、玩家 UUID 查找有效票据，
   恢复维度、坐标与朝向，再推进为 `COMPLETED`。
6. 恢复短时失败时保留 `DISPATCHED` 供后续 tick 重试；超过 TTL 才进入 `EXPIRED`。

这条链已经解决插件消息只能携带在线连接、目标服加载存在时序差异和重复请求的问题。后续功能应
复用它，不新增第二套传送系统。

### 当前可直接进入实现的功能

- 新增 `/sethome [name]` 与 `/delhome [name]`，薄适配到现有 home 服务；`/home set/delete`
  继续兼容。
- 新增 `/tpaccept <player>`，薄适配到现有 TPA 接受服务；`/tpa accept` 继续兼容。
- 增加 `/tpdeny <player>`、`/tpa cancel <player>` 和待处理查询；TPA 状态扩展为
  `PENDING / ACCEPTED / DECLINED / CANCELLED / EXPIRED`。
- 在目标服按当前在线玩家和本服 ID 轮询待处理 TPA，并提供限频提示。该方案只依赖现有
  PostgreSQL，不需要 Presence 或 Redis，也不会跨服公开玩家 UUID。
- 为上述命令与错误反馈增加本地化键；服务端日志仍保留结构化英文诊断信息。
- 为状态迁移、并发接受/拒绝、重复 requestId、过期请求、跨服目标隔离和登录提示增加自动测试。

### 暂不阻塞当前功能的优化

- `/tpa <player>` 自动查找玩家所在服；在 Presence 完成前继续支持显式
  `/tpa <player> <targetServerId>`。
- Redis 或代理伴生消息总线、全局广播、离线消息推送。
- 全局 spawn/hub、跨服 RTP、warmup/cooldown、移动取消和传送收费。
- 玩家自建 warp。`setwarp/delwarp` 若实现，只能是有权限且有审计的制度管理动作。
- death-back、其他 Mod 传送捕获和多点 back 历史。

### 外部实现带来的约束

- [HuskHomes](https://github.com/WiIIiam278/HuskHomes) 的跨服设计把共享 SQL 与
  插件消息/Redis broker 分层；JGB 沿用 PostgreSQL 真源，未来只在确有低延迟需求时增加消息层。
- [HuskHomes 配置文档](https://william278.net/docs/huskhomes/config-files)也把全局 spawn、RTP
  限制、冷却和经济规则做成可选配置，说明它们是规则层扩展，而非基本跨服传送的必要组成。
- [BungeeCord 插件消息文档](https://www.spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel/)
  表明 `Forward`、查询和消息子通道依赖代理与在线连接；它们不能承担唯一持久状态。
- [Velocity 文档](https://docs.papermc.io/velocity/dev/plugin-messaging/)提醒自定义插件消息存在伪造
  风险。未来若引入代理伴生协议，必须验证可信来源、协议版本和 payload 上限。
- [Spigot 社区讨论](https://www.spigotmc.org/threads/sending-player-to-another-server-using-redis.412230/)
  展示了 Redis Pub/Sub 与插件消息两种常见路线。论坛经验仅作为工程取舍参考，不作为安全合同；
  JGB 若采用 Redis，仍需版本化结构、幂等、重连和主线程切换。

## 2026-08-28 ServerTools TPA 闭环实现

### 新增兼容入口

- `/sethome [name]`、`/delhome [name]` 与现有 `/home set/delete` 共用同一 PostgreSQL home 服务。
- `/tpaccept <player>`、`/tpdeny <player>` 与 `/tpa accept/deny` 共用同一 TPA 状态机。
- `/tpa cancel <player> [targetServerId]` 仅能取消自己仍为 `PENDING` 的请求。
- `/tpa status` 只显示当前玩家在当前服的近期发起与收到记录，不显示 UUID、坐标或其他玩家记录。

### TPA 状态与跨服调度

- 状态为 `PENDING / ACCEPTED / DECLINED / CANCELLED / EXPIRED`；接受、拒绝、取消均以
  `WHERE status = 'PENDING'` 条件更新，竞争操作只有一个会成功。
- 接受者在目标服确认时，持久化其当时的维度、坐标和朝向；不会在目标服伪造请求者连接来发代理消息。
- 请求者所在源服的 ServerTools 控制器在登录和每秒 tick 扫描已接受请求；只有请求者仍在线、仍在
  原始源服且未超过原 30 秒时限，才以确定性派发 requestId 调用既有 ticket 与 `Connect` 链。
- 目标服对待处理请求、源服对拒绝/取消/超时结果都做进程内限频提示；服务重启后最多重提示一次，
  正式真源仍在 PostgreSQL，玩家随时可用 `/tpa status` 查询。
- ServerTools 玩家聊天反馈使用中英文 `ChatComponentTranslation` 键；结构化英文错误细节只保留在服务端日志。

### 无真人模拟验收边界

`CrossServerTpaSimulationTest` 使用真实 `PlayerTeleportService`、`AcceptedTpaSourceDispatcher`、
`ClusterTeleportService` 和 `PlayerArrivalRestoreService`，只以记录型 gateway 与本地落点执行器替换外部
网络和客户端。它验证：

- 目标服接受只持久化接受者落点；
- 只有请求者原始源服可以派发，派发 requestId 从 TPA requestId 确定性派生；
- 同一进程的重复 tick 不重复调 gateway，重启后的再扫描由已有 ticket 阻止第二次 gateway 调用；
- 目标服按 ticket 恢复最终落点并完成 ticket；
- 已接受请求在原始 30 秒边界后不再派发。

这不是 Forge fake-player 或机器人方案，不验证代理实际 `Connect`、在线模式身份、客户端重连或真实
维度加载。它用于在 S2 不可启动且暂时缺少双账号时保护业务调度；真实跨服验收仍是上线前必要步骤。

## 2026-08-28 ServerTools 终端快捷传送 1.0

终端原有 `群组服传送` 页继续显示服务器目录、系统 Warp、详情与近期 transfer ticket。本轮在工作区
新增三个经过确认框的快捷操作：

- `回家`：调用默认名 `home` 的已有跨服 home 传送；未设置默认 home 时由服务端拒绝并回写原因；
- `返回`：调用已有 back 记录；无有效记录时由服务端拒绝；
- `出生点`：取当前世界出生点并进入统一传送主链。

终端按钮不直接改坐标，也不向客户端暴露 ticket 身份；每次确认仍在服务端构造 `TeleportActor`、计划和
requestId。首次只覆盖快捷入口，RTP 的安全候选点、命名 home 的创建/删除和 TPA 表单以后独立设计。

## 2026-08-30 ServerTools 终端 1.1：命名 Home

终端 `群组服传送` 页现在在系统 Warp 之外展示当前玩家的个人 Home 列表。每项显示 Home 名称、目标服与
精确维度/坐标；这是一份服务端快照，不公开其他玩家的 Home、UUID 或坐标。

- 输入名称并点击“设定”时，终端只提交名称。服务端用在线玩家当前的 `TeleportActor` 位置调用既有
  `PlayerTeleportService.setHome`，因此仍适用原有名称规范和玩家 Home 配额。
- 选中 Home 后可“前往”或“删除”，两项均需精确确认。传送使用
  `prepareHomeTeleport(..., homeName) -> ClusterTeleportService -> ticket -> 目标服恢复`，本服与跨服路径一致。
- 删除按当前在线玩家 UUID 与名称执行；客户端不能指定所有者、来源服或目标位置。
- 网络 payload 由旧的一段 warp、两段 warp/快捷动作兼容扩展为第三段 Home 名称；同一版本服务端和客户端
  使用新字段，旧 payload 仍默认没有选中 Home。

本批仍不提供 TPA 表单、RTP、Home 重命名或管理其他玩家的 Home；这些功能须各自定义权限和交互合同。

## 2026-08-31 ServerTools 终端 1.2：跨服 TPA 面板

终端 `群组服传送` 页增加紧凑的 TPA 区域：玩家可以输入目标玩家名和明确的目标服务器 ID 发起请求，
并查看自己近期发出或收到的请求。收件箱中仅对 `PENDING` 请求显示“接受 / 拒绝”；发件箱中仅对
`PENDING` 请求显示“取消”。所有动作先进入精确确认框。

- 快照仅返回当前终端玩家的方向、另一位玩家名、目标服务器和状态，不返回请求 UUID、接受者坐标、
  任何第三方请求或玩家位置。
- 客户端不提交自身身份、来源服务器或接受落点；服务端从在线 `EntityPlayerMP` 与本服运行时注入
  `TeleportActor`，并继续使用既有 PostgreSQL 状态机的条件更新。
- 接受后仍由请求者所在源服的 tick/login 调度器以既有 `cluster_transfer_ticket -> Connect -> 落点恢复`
  主链执行跨服传送。终端不会直接切服，也不会创建第二套 ticket。
- 旧的一段 Warp、两段 Warp/快捷动作、三段 Home payload 继续兼容解码；TPA 使用新增的五段版本化
  payload，只有同版终端才会发出它。

本批不提供自动找服、Presence、离线推送、全局通知中心、费用、warmup/cooldown 或跨服 RTP；这些属于
后续单独的规则与运维批次。

## 2026-08-31 系统 Warp 与服务器目录受审计管理

玩家传送仍使用 `/jgbst warp [list|name]`。维护动作不注册裸 `/setwarp`、`/delwarp`，因为当前灰度服
仍保留 ServerUtilities，避免命令冲突；维护者使用下面唯一命名空间入口：

- `/jgbst admin setwarp <名称> [显示名] [说明...]`：以维护者所在的本服、维度、坐标和朝向创建或覆盖
  系统 Warp，并启用它；跨服落点须在目标服由维护者设置，客户端不能提供坐标或服务器身份。
- `/jgbst admin delwarp <名称>`：删除系统 Warp。
- `/jgbst admin warp-enabled <名称> <true|false>`：启停既有系统 Warp。
- `/jgbst admin server-enabled <服务器ID> <true|false>`：维护已注册服务器目录的启用状态；当前服不能从
  自己的运行时禁用，避免把仍在运行的本服误标为不可用。

入口使用原版命令等级 `2`，不接入 ServerUtilities 的 rank/权限总线；后续职业、贡献、声望或组织制度可
在命令前替换为自己的维护者策略。每次状态变更使用独立 requestId，并与 `servertools_admin_operation`
审计行在一个 PostgreSQL 事务中提交；审计保存操作者 UUID/名称、来源服、动作、目标以及结构化前后快照。
审计表缺失时普通玩家传送链不受影响，但管理动作会拒绝，要求先应用 migration。

迁移：`ops/sql/migrations/20260831_001_add_servertools_admin_audit.sql`。本批没有向普通终端玩家页面
暴露管理按钮，也不会公开维护者、UUID、坐标或审计内容；终端继续是只读目录和传送入口。

### 迁移与灰度

- migration：`ops/sql/migrations/20260828_001_expand_tpa_request_lifecycle.sql`。
- 新列缺失时 ServerTools JDBC 初始化会 fail-fast 并要求执行 `scripts/db-migrate.sh`，不会带旧 schema
  静默运行。
- 灰度部署只覆盖 Lobby、S2 与客户端；客户端不启动，S1 和 ServerUtilities JAR 不修改。
