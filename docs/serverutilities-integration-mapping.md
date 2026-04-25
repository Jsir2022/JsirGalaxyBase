# ServerUtilities 整合映射表

日期：2026-04-10

这份文档用于把 `Reference/ServerUtilities` 中后续可能需要吸收的能力逐项列清，并标记为：

- `可直接复用`
- `需抽取后复用`
- `建议重写`
- `暂不整合`

这里的“复用”默认指“为了并入 `JsirGalaxyBase` 内部模块而复用”，不是指继续把 `ServerUtilities` 作为独立模组直接使用。

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
  - 状态：`暂不整合`
  - 原因：这些能力本身很大，而且和当前经济、市场、银行、跨服状态同步不是同一个优先级；同时又深度依赖 team data、权限、chunk ticket 和导航联动

- `command/chunks/*`
  - 状态：`暂不整合`
  - 原因：同上，先不要让 claims / chunkloader 抢占当前制度主线开发带宽

### Teams / ranks / 权限体系

- `ServerUtilitiesTeamData`
  - 状态：`建议重写`
  - 原因：这是 `ServerUtilities` 旧 team 宿主的一部分，不适合作为 `JsirGalaxyBase` 的团队、组织或公会模型直接沿用

- `Ranks` / `Rank` / `PlayerRank` / `ServerUtilitiesPermissionHandler`
  - 状态：`建议重写`
  - 原因：权限系统在 `ServerUtilities` 里不是薄层，而是贯穿命令、配置、玩家数据和 GUI；如果直接搬，会把整个权限框架一并带入

- `command/ranks/*`
  - 状态：`暂不整合`
  - 原因：如果以后需要正式权限/组织系统，应从当前制度设计反推，不从 `ServerUtilities` 的 rank GUI 和 handler 出发

- `command/team/*`
  - 状态：`暂不整合`
  - 原因：团队系统当前不是首要目标，而且其实现和旧 team data 紧耦合

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

同时不建议第一批碰：

- claims / chunkloading
- team / ranks 全套
- invsee / NBT 编辑 / admin panel
- backup / shutdown / pregenerator / watchdog

## 一句话结论

`ServerUtilities` 中真正适合当前项目吸收的，不是它“整套服务器工具框架”，而是其中少数已经验证过玩家体验的工具能力和少量值对象 / 业务流程。

后续整合时应优先：

- 抽取传送与 home 语义
- 抽取榜单与通知经验
- 重写运行时宿主、权限、网络、持久化和 cluster 接线

而不是把旧模组整体嵌进 `JsirGalaxyBase`。