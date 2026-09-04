# ServerUtilities 整合映射表

日期：2026-04-10

这份文档用于把 `Reference/ServerUtilities` 中后续可能需要吸收的能力逐项列清，并标记为：

- `可直接复用`
- `需抽取后复用`
- `建议重写`
- `暂不整合`

这里的“复用”默认指“为了并入 `JsirGalaxyBase` 内部模块而复用”，不是指继续把 `ServerUtilities` 作为独立模组直接使用。

## 2026-08-22 状态修正

当前不能把“第一批传送能力已落地”描述为“ServerUtilities 已整合完毕”。JGB 已有
`home`、`back`、`spawn`、`warp`、`tpa`、`rtp`、跨服 ticket 和终端群组服传送页，
但 claim、chunk loading、team/rank、领地地图、排行榜及管理工具没有进入 JGB。

实时环境已经安装 ServerUtilities 2.2.2，并且 S1/S2 存在真实 claimed-chunk 数据，
但目标不是长期保留这个独立 Mod。claim、保护、team、chunk loading 和地图能力应重组进
JGB；旧 JAR 只在迁移前提供行为基线和历史数据，灰度验收后从服务器与客户端删除。
详细边界见 `serverutilities-land-property-integration-v1.md`。这项调整不授权立即修改 S1、
删除 JAR 或迁移现有 `.dat` 数据。

## 判断标准

### 可直接复用

- 以纯规则、纯值对象、纯业务流程为主
- 不强依赖 `Universe`、`PlayerData`、`TeamData`、`RankConfigAPI`、旧网络 wrapper 或旧 GUI 壳
- 可以较低成本改包名后接入当前项目自己的模块边界

### 需抽取后复用

- 业务语义有价值
- 但当前实现和 `ServerUtilities` 旧运行时宿主或旧权限/网络层绑得较深
- 需要先抽出 application service、port、value object 或命令语义，再接入当前项目

### 建议重写

- 现有实现高度依赖 `ServerUtilities` 的整体框架
- 或者继续沿用会明显破坏 `JsirGalaxyBase` 的模块化单体边界
- 可以参考功能定义和交互经验，但实现层不建议搬代码

### 暂不整合

- 当前制度主线不急需
- 或者和现阶段目标无关、收益较低、耦合过深
- 后续如果阶段目标变化，再重新评估

## 模块映射

### 传送与 home

- `CmdHome` / `CmdSetHome` / `CmdDelHome` / `CmdBack` / `CmdSpawn` / `CmdWarp` / `CmdSetWarp` / `CmdDelWarp`
  - 状态：`需抽取后复用`
  - 原因：功能目标非常契合当前群组服路线，但现有实现绑定了 `ServerUtilitiesPlayerData`、`ServerUtilitiesUniverseData.WARPS`、`TeleportTracker`、rank config 和旧命令体系
  - 建议落点：`modules.servertools` + `modules.cluster`

- `TeleportTracker` / `TeleportLog` / `TeleportType`
  - 状态：`可直接复用`
  - 原因：这几类更接近传送历史、冷却记录和值对象语义，抽离 `NBT` 壳后可直接作为当前项目传送历史与票据辅助对象参考
  - 参考来源：`Reference/ServerUtilities/src/main/java/serverutils/data/TeleportTracker.java`、`Reference/ServerUtilities/src/main/java/serverutils/data/TeleportLog.java`、`Reference/ServerUtilities/src/main/java/serverutils/data/TeleportType.java`

- `CmdTPA` / `CmdTPAccept` / `CmdTplast` / `CmdRTP`
  - 状态：`需抽取后复用`
  - 原因：玩家体验和规则流程值得参考，但应先按当前制度规则重新定义是否免费、是否跨服、是否允许随机传送，再决定是否接入
  - 建议落点：`modules.servertools`

- `TeleportTask`
  - 状态：`需抽取后复用`
  - 原因：warmup / delayed execute 语义有价值，但当前任务调度和 `Universe`、通知系统强耦合
  - 建议落点：当前项目自己的任务调度或服务端 tick 驱动执行器

### 通知与玩家服务工具

- 通知语义与 HUD 弹出体验
  - 状态：`需抽取后复用`
  - 原因：你当前终端已经有自己的通知链，`ServerUtilities` 可提供“服务端反馈应如何展示”的经验，但不应直接引入其客户端通知框架
  - 参考来源：`Reference/ServerUtilities/src/main/java/serverutils/ServerUtilitiesNotifications.java`、`Reference/ServerUtilities/src/main/java/serverutils/client/NotificationHandler.java`

- `CmdNick` / `CmdNickFor` / `CmdMute` / `CmdUnmute` / `CmdTrashCan` / `CmdKickme`
  - 状态：`暂不整合`
  - 原因：这些更偏通用工具服能力，和当前制度核心、银行、市场、群组服同步主线关系较弱

- `CmdFly` / `CmdGod` / `CmdHeal` / `CmdVanish`
  - 状态：`暂不整合`
  - 原因：更偏管理服 / OP 工具；即使未来要做，也应单独定义管理权限模型，而不是沿用 `ServerUtilities` 现成实现

- `VanishData`
  - 状态：`暂不整合`
  - 原因：与当前制度主线弱相关，而且绑定旧 config/group 与管理工具语义

### 服务器状态、cluster 与跨服协调

- 服务器状态展示、服务器目录、跨服目标服可用性
  - 状态：`建议重写`
  - 原因：`ServerUtilities` 并不是按你当前 `Velocity + BungeeForge + PostgreSQL + GatewayAdapter` 的 cluster 架构设计的，直接复用会把旧单服工具模组思路带进来
  - 建议落点：`modules.cluster`

- `TicketKey`
  - 状态：`可直接复用`
  - 原因：作为轻量 ticket / key 值对象思路可以参考，适合用于传送票据或任务标识的简单建模

- `BlockDimPosStorage`
  - 状态：`需抽取后复用`
  - 原因：home / warp / 落点恢复的数据结构语义可用，但序列化应改为当前项目自己的仓储层而不是继续围绕 NBT

- `BackwardsCompat`
  - 状态：`暂不整合`
  - 原因：这是面向 `FTBU / LatMod` 历史数据迁移的兼容层，只在真的要迁旧服历史数据时再看

### Claims / chunkloading / 团队领地

- `ClaimedChunks` / `ClaimedChunk` / `ClaimResult` / `ServerUtilitiesLoadedChunkManager`
  - 状态：`按行为重写到 JGB；旧数据仅一次性迁移`
  - 原因：领地保护与后续地产交易已经成为正式候选能力，且现服存在真实历史 claim；但现有实现深度依赖 team data、权限、chunk ticket 和本地 NBT，不能成为 JGB 运行时依赖或银行产权真相
  - 建议落点：建立 JGB 原生 `LandProtectionPort`、PostgreSQL 产权登记和服务端内存保护索引；另做不进入正式运行时的一次性旧数据导出/导入工具

- `command/chunks/*`
  - 状态：`只参考命令语义；由 JGB 原生动作重写`
  - 原因：claim/unclaim/load/unload 可作为最小行为参考，但 JGB 动作必须使用服务端身份、request id、版本和结构化回执，不能转发旧客户端网络包

- `GuiClaimedChunks` / `ClientClaimedChunks` / Navigator claims layer
  - 状态：`参考区块网格和同步语义，建议用 JGB 终端重写`
  - 原因：当前终端已经有自己的页面、主题、服务端授权和快照协议；地产应成为独立页面，不把 ServerUtilities GUI 嵌入终端

- 地产挂牌与交易
  - 状态：`ServerUtilities 不提供；由 JGB 独立设计`
  - 原因：唯一地产需要产权版本、挂牌冻结、银行事务、幂等过户、保护缓存刷新和恢复状态；不能靠 unclaim + claim 或直接改 team 文件模拟成交
  - 详细设计：`serverutilities-land-property-integration-v1.md`

### Teams / ranks / 权限体系

- `ServerUtilitiesTeamData`
  - 状态：`建议重写`
  - 原因：这是 `ServerUtilities` 旧 team 宿主的一部分，不适合作为 `JsirGalaxyBase` 的团队、组织或公会模型直接沿用；JGB Team 需要成为成员、地产授权和 Team 银行账户的正式主体

- `Ranks` / `Rank` / `PlayerRank` / `ServerUtilitiesPermissionHandler`
  - 状态：`建议重写`
  - 原因：权限系统在 `ServerUtilities` 里不是薄层，而是贯穿命令、配置、玩家数据和 GUI；如果直接搬，会把整个权限框架一并带入

- `command/ranks/*`
  - 状态：`暂不整合`
  - 原因：如果以后需要正式权限/组织系统，应从当前制度设计反推，不从 `ServerUtilities` 的 rank GUI 和 handler 出发

- `command/team/*`
  - 状态：`个人地产完成后重新设计`
  - 原因：已确认首轮只做个人认领和个人地产交易；Team、个人和公共关系需要单独定稿，不复制旧 team data，也不先创建占位组织模型

### 排行榜、统计与展示

- `Leaderboard` / `LeaderboardValue`
  - 状态：`需抽取后复用`
  - 原因：排行概念和展示方式有价值，适合以后做贡献度、市场成交额、职业产出排行；但现有实现依赖 `Universe` 玩家集合与旧消息链

- `ServerUtilitiesLeaderboards` / `CmdLeaderboard`
  - 状态：`需抽取后复用`
  - 原因：可以参考“榜单入口应该长什么样”，但数据获取与推送层应按当前项目重做

- `CmdDumpStats` / `ServerUtilitiesStats`
  - 状态：`暂不整合`
  - 原因：偏通用统计导出，不是当前制度主线刚需

### Invsee、NBT 编辑、管理面板

- `invsee/*` / `CmdInv`
  - 状态：`暂不整合`
  - 原因：管理型工具，不属于当前银行/市场/跨服制度主线；未来如要做，建议单独定义管理能力模块

- `CmdEditNBT` 与 `net/MessageEditNBT*`
  - 状态：`暂不整合`
  - 原因：高权限运维工具，不应在当前阶段混入制度核心模组主链

- admin panel / `MessageAdminPanel*`
  - 状态：`建议重写`
  - 原因：如果后续需要后台管理界面，应直接基于当前终端或新的管理 GUI 做，不应该引入 `Aurora + 旧面板 action` 体系

### 备份、关服、巡检、预生成

- `CmdBackup` / `task/backup/*`
  - 状态：`暂不整合`
  - 原因：运维能力更适合留在服务器脚本、MCSM 或独立运维工具，不适合优先塞进当前自定义制度模组

- `CmdShutdown` / `CmdShutdownTime` / `ShutdownTask` / `pausewhenempty/*`
  - 状态：`暂不整合`
  - 原因：偏服主运维控制，与当前制度主线不一致

- `pregen/*`
  - 状态：`暂不整合`
  - 原因：世界预生成不是当前制度模组职责

- `watchdog/*`
  - 状态：`暂不整合`
  - 原因：同样属于运维层而不是制度层

### 配置、运行时宿主、网络层

- `ServerUtilities.java`
  - 状态：`建议重写`
  - 原因：这是独立模组入口，不适合并入当前单入口 `GalaxyBase`

- `ServerUtilitiesCommon`
  - 状态：`建议重写`
  - 原因：它是旧运行时总线，绑定权限、reload、config provider、team action、admin action、Universe 与网络初始化，不符合当前模块边界

- `ServerUtilitiesNetHandler`
  - 状态：`建议重写`
  - 原因：多 wrapper 分组方式来自旧历史功能演化，不能直接作为当前项目网络边界

- `ServerUtilitiesPlayerData`
  - 状态：`建议重写`
  - 原因：玩家数据、规则、权限、展示、home、warmup 强耦合，不能直接当成当前玩家主状态宿主

- `ServerUtilitiesUniverseData`
  - 状态：`建议重写`
  - 原因：世界级持久化、日志、claims、warps、ranks 初始化和事件注册混在一起，明显不是单纯存储层

## 第一批推荐整合范围

如果按当前 `JsirGalaxyBase` 主线推进，推荐第一批只考虑下面这些：

- `home` / `sethome` / `delhome`
- `back`
- `spawn` / `warp` 的制度化版本
- `tpa` / `tpaccept` 的制度化版本
- `rtp`
- `TeleportTracker` / `TeleportLog` / `TeleportType` 的抽取
- 服务器状态展示与通知体验的参考语义
- 榜单模型的抽取思路

2026-04-11 已确认：

- `home`、`back`、`spawn`、`tpa`、`rtp`、`warp` 都属于第一期必须实现范围
- `home`、`warp`、`tpa`、`back` 等传送命令必须允许跨服
- 第一期直接接数据库，不走本地 NBT / 文件版占位
- 第一期不做 GUI，优先命令与服务主链
- 权限先按玩家权限语义实现，但后续必须能接职业、贡献度、声望制度

更详细的已确认范围见：`docs/servertools-phase1-requirements.md`

这批能力最有机会进入：

- `modules.cluster`
- `modules.servertools`

同时不建议在传送第一批中顺带碰：

- claims / chunkloading 的完整经济化；应作为独立 JGB 原生地皮阶段推进
- team / ranks 全套；个人地产阶段完全后置
- invsee / NBT 编辑 / admin panel
- backup / shutdown / pregenerator / watchdog

## 2026-08-28 跨服指令实现复核

本轮按现有代码、ServerUtilities 源码、代理协议文档和跨服开源项目重新复核。结论是：
JGB 不缺第二套传送核心，缺的是在现有持久化票据链上补齐玩家可见的命令生命周期。

### 当前 JGB 已有的框架

现有调用链为：

`命令 -> PlayerTeleportService -> PostgreSQL -> ClusterTeleportService -> 本服直传或代理 Connect
-> 目标服 PlayerArrivalRestoreService -> 最终坐标恢复 -> ticket 完成`

- home、warp 和 back 的落点包含 `server_id + dimension + 坐标 + 朝向`，天然可跨服。
- 远程传送先写 `cluster_transfer_ticket`，再请求代理切服；目标服按玩家 UUID 恢复最终落点。
- ticket 有 `requestId` 和明确状态，不应改回只靠内存、NBT 或一次插件消息完成的易丢链路。
- PostgreSQL 是持久真源；BungeeCord 插件消息目前只承担在线玩家的 `Connect` 动作。
- TPA 当前只有 `PENDING / ACCEPTED / EXPIRED`，命令只有发起和接受；拒绝、取消、待处理列表、
  目标玩家提示及中文反馈尚未形成完整体验。

### ServerUtilities 可吸收的命令语义

ServerUtilities 的价值仍然是成熟的玩家入口和使用习惯，而不是它的单服数据宿主：

- `sethome`、`delhome`、`tpaccept` 适合作为 JGB 现有服务的薄兼容命令，不复制业务逻辑。
- `tpdeny`、TPA 超时提示和待处理请求体验值得补齐，但状态必须写入 JGB PostgreSQL。
- `setwarp`、`delwarp` 可作为后续受权限和审计约束的管理入口；不能开放成普通玩家自建 warp。
- SU 的 warmup、cooldown、移动取消、通知样式可作为后续体验优化，不是当前跨服正确性的前置条件。
- `Tplast` 是管理者查询/传送到玩家最后位置，不等同于玩家 `/back`，不应混为一个命令。

### 开源与社区实现对照

- HuskHomes 同时支持跨服 home、warp、TPA 和 RTP，采用共享 SQL 保存业务数据，并允许
  `PLUGIN_MESSAGE` 或 Redis 作为跨服消息 broker。这验证了“持久数据与实时消息分层”的方向，
  但不意味着 JGB 当前必须增加 Redis。
- HuskHomes 用命名 warp 表示全局 spawn，并允许 RTP 指定服务器/世界；这些属于后续全局入口优化，
  不改变本批先补齐现有命令的顺序。
- BungeeCord 标准子通道已经提供 `Connect`、`ConnectOther`、`GetPlayerServer`、`Message` 和
  `Forward`。其中插件消息通常依赖在线玩家作为载体，不能替代 JGB 的持久 ticket 和目标服恢复。
- 社区常见的另一条路线是 Redis Pub/Sub 传递在线状态、通知和切服信号；它适合服务器数量、代理
  数量或事件量上升后的低延迟广播，但需要处理消息版本、重复、乱序、断线和异步线程切回主线程。

参考资料：

- [HuskHomes 开源仓库](https://github.com/WiIIiam278/HuskHomes)
- [HuskHomes 配置与跨服 broker](https://william278.net/docs/huskhomes/config-files)
- [HuskHomes 命令参考](https://github.com/WiIIiam278/HuskHomes/blob/master/docs/Commands.md)
- [Velocity 插件消息安全与兼容说明](https://docs.papermc.io/velocity/dev/plugin-messaging/)
- [Bukkit/BungeeCord 插件消息子通道](https://www.spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel/)
- [Spigot 社区关于 Redis 与插件消息的跨服传送讨论](https://www.spigotmc.org/threads/sending-player-to-another-server-using-redis.412230/)

### 当前实现批与优化池的边界

当前应直接实现：

1. 为现有 home 和 TPA 服务增加 `sethome`、`delhome`、`tpaccept` 兼容命令。
2. 将 TPA 状态补齐为可拒绝、可取消、可查询、可过期，并保留 `requestId` 幂等与服务端身份校验。
3. 目标服只查询发给本服玩家的待处理请求，在玩家登录/定时 tick 时提示；不要求 Redis，也不允许
   客户端指定被查询身份。
4. 命令反馈改为本地化键，至少完整覆盖中英文，不继续把英文业务字符串直接拼到聊天栏。
5. 保留显式 `targetServerId` 的跨服 TPA 合同，先让现有能力可靠可用。

后续优化项：

- 全局 Presence 与 `/tpa <player>` 自动解析玩家所在服。
- 抽象 `ClusterMessageBus`，按规模选择代理伴生插件或 Redis；PostgreSQL ticket 仍是持久真源。
- 全局 `/hub`、全局 spawn、指定目标服/世界 RTP。
- warmup、cooldown、移动取消、费用规则、离线通知和终端通知中心。
- `/back` 捕获死亡和其他 Mod 触发的传送；当前只记录 JGB 自己的传送。
- 自定义代理消息使用版本化结构化 envelope，并校验来源、长度、枚举和 requestId；不使用逗号拼接协议。
- 多代理部署、broker 重连、消息重复/乱序和指标告警专项测试。

因此，Redis、自动找服和全局通知属于明确记录的扩展项，但不再被当成本期补齐 SU 玩家指令的前置条件。

### 2026-08-28 本批落地状态

已原生落地 `sethome/delhome/tpaccept/tpdeny` 兼容入口，以及可取消、可拒绝、可查询、可过期的
PostgreSQL TPA 生命周期。跨服接受改为“目标服保存接受坐标，源服用请求者真实在线连接派发 ticket”，
不复制 SU 单服数据宿主，也不引入 Redis。全局 Presence、自动目标服查找、全局通知、warmup/cooldown
和跨服 RTP 继续后置。

## 一句话结论

`ServerUtilities` 中真正适合当前项目吸收的，不是它“整套服务器工具框架”，而是其中少数已经验证过玩家体验的工具能力和少量值对象 / 业务流程。

后续整合时应优先：

- 抽取传送与 home 语义
- 抽取榜单与通知经验
- 重写运行时宿主、权限、网络、持久化和 cluster 接线

而不是把旧模组整体嵌进 `JsirGalaxyBase`。
