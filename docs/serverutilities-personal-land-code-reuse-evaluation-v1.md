# ServerUtilities 个人地皮代码复用与 JGB 接入评估 v1

日期：2026-08-22

## 1. 本轮结论

首轮只实现个人地皮、个人固定价挂牌和个人购买，不实现 Team、公共产权、职业配额、
租赁、拍卖和税制。数据模型可以保留未来所有者类型扩展点，但所有服务端写动作只接受
当前玩家 UUID。

ServerUtilities 源码可以按 LGPL-3.0-or-later 复制和修改；JGB 本身公开在 GitHub 并不
自动免除许可证义务。开始复制前应：

- 保留被复制文件的原作者和许可证说明；
- 在仓库增加第三方声明并保留完整 LGPL-3.0 文本；
- 在发布 JAR 中同时包含 JGB 的 MIT 许可证与 ServerUtilities 派生部分声明；
- 公开派生部分对应源码和修改记录；
- 修正当前 `src/main/resources/LICENSE` 的占位版权文本，避免发布包许可证与根目录
  `LICENSE` 不一致。

法律上可以复制整个项目；工程上不应这么做。`Reference/ServerUtilities` 有 592 个 Java
文件、约 54,091 行。claim 核心数据、命令、网络和旧 GUI 的直接候选面约 2,317 行，
其中预计只有约 15% 可以近似原样搬入，另有约 25% 到 35% 适合以复制后重构的方式利用；
其余超过一半应只参考行为，不进入个人地皮首版。

## 2. 逐类复用结论

| ServerUtilities 来源 | 规模 | 结论 | JGB 落点 |
| --- | ---: | --- | --- |
| `lib/math/ChunkDimPos.java` | 81 行 | 高复用；增加 `serverId`，改为不可变 `LandChunkKey` | `modules.land.domain` |
| `data/ClaimResult.java` | 10 行 | 可复制并扩展；删除 `NO_TEAM`，增加版本、归属、保留区和挂牌冻结结果 | `modules.land.domain` |
| `data/ClaimedChunk.java` | 88 行 | 复制骨架后重写 owner/team 字段；保留坐标、loaded、状态思想 | `modules.land.domain` |
| `data/ClaimedChunks.java` | 413 行 | 中等复用；可利用 map、claim/unclaim/load/unload 与保护判断结构，但必须移除全局单例、Universe、Team、rank、旧通知和旧网络 | `LandService` + `LandProtectionIndex` |
| `data/ServerUtilitiesLoadedChunkManager.java` | 175 行 | Forge ticket 机制可复制改造；首个个人交易版本可暂不开启强加载 | `LandChunkLoadingService`，后续开关 |
| `data/ServerUtilitiesTeamData.java` | 317 行 | 个人首版不复制；只保留旧 NBT 解析知识供离线迁移器使用 | `tools/legacy-land-import` |
| `events/chunks/ChunkModifiedEvent.java` | 77 行 | 可参考事件语义；正式业务用 JGB command/result/audit，不依赖 Forge 事件作为交易真相 | `modules.land.application` |
| `handlers/*` 中破坏、放置、交互、攻击和爆炸片段 | 约 100 行有效片段 | 可复制事件接入模式并重写权限调用 | `LandProtectionEventHandler` |
| `MessageClaimedChunks*` | 315 行 | 不复制；旧 wrapper、team 编码和客户端信任边界不适用 | 现有 `TerminalActionMessage` / snapshot 协议 |
| `ClientClaimedChunks.java` | 111 行 | 只借鉴格子状态与 flags；客户端 DTO 按 JGB 快照重写 | `TerminalLandSectionModel` |
| `GuiClaimedChunks.java` | 323 行 | 不复制 GUI 壳；只利用 15x15 网格、边界、拖选和 loaded 图案算法 | JGB terminal panel/canvas |
| `command/chunks/*` | 约 533 行 | 不复制命令框架；保留 claim/unclaim/info 的行为和错误语义作为诊断入口 | JGB 命令与 service 共用 action |

`MessageClaimedChunksModify` 中 `LOAD` 会先落入 `CLAIM` 再执行 load，这是旧交互语义，
不能无条件复制。JGB 应把“认领”和“强加载”做成两个明确确认动作，避免一次点击隐式创建
产权并消耗强加载额度。

## 3. 不要拆成过多模块

个人首版只新增一个内部 `LandModule`，不要先拆成 claim、property、protection、trade 四个
独立 Mod 或四个顶层模块：

```text
modules.land
  domain
    LandChunkKey / PersonalLandTitle / LandListing / LandStatus
  application
    LandService / PersonalLandTradeService
  infrastructure
    jdbc / minecraft / migration
  port
    LandRepository / LandTransactionRunner
```

其中：

- `LandService` 负责个人认领、放弃、查询和保护索引刷新；
- `PersonalLandTradeService` 负责挂牌、撤牌、购买和历史；
- `LandProtectionEventHandler` 是 Minecraft 事件适配层，不包含产权规则；
- PostgreSQL 是产权、挂牌和审计真源；
- 本服内存 map 只是高频保护索引，可从数据库重建；
- 客户端只提交 action、坐标和 expectedVersion，玩家 UUID、serverId 与 dimensionId 由
  服务端当前会话重新注入。

## 4. 个人首版数据与动作

### 4.1 最小表

```text
land_title
  title_id / owner_player_ref / status / version
  source_server_id / dimension_id / chunk_x / chunk_z
  created_at / updated_at

land_listing
  listing_id / title_id / seller_player_ref / asking_price
  title_version / status / buyer_player_ref / created_at / updated_at

land_operation_log
  operation_id / request_id / operation_type / player_ref
  title_id / listing_id / semantics_key / status / failure_reason / created_at
```

首版每个 title 只对应一个 chunk，所以暂不增加 parcel 与多区块组合表。未来需要组合地块
时再引入 `land_title_chunk`，不要为未验证需求增加 join 和状态同步。

公共产权虽然后置，但出生点、Lobby、公共道路和管理员保留区不能变成可认领荒地。个人
首版用服务端配置的 server/dimension/chunk deny-list 阻止认领即可；它是准入规则，不是
公共组织产权模型。

唯一约束至少包括：

- `(source_server_id, dimension_id, chunk_x, chunk_z)` 唯一；
- `land_operation_log.request_id` 唯一；
- 一个 title 同时最多一个 `OPEN` listing；
- 金额必须大于零；
- listing 保存挂牌时的 `title_version`。

### 4.2 最小动作

- `LAND_REFRESH`
- `LAND_SELECT_CHUNK`
- `LAND_CLAIM`
- `LAND_UNCLAIM`
- `LAND_LIST_FOR_SALE`
- `LAND_CANCEL_LISTING`
- `LAND_BUY_LISTING`
- `LAND_CHANGE_PAGE`

首轮不开放批量框选、Team 授权、公共地产、强加载交易和职业加成。

## 5. 加入银行与交易系统

地产不是 `ItemStack`，不复用标准订单簿，也不复用定制商品的实体交付状态。可以直接
借鉴 `CustomMarketService` 已验证的这些做法：

- `requestId + semanticsKey` 幂等；
- `lockById` 锁定挂牌；
- 禁止卖家购买自己的挂牌；
- ACTIVE / SOLD / CANCELLED 状态机；
- 正式 audit log；
- 使用现有共享 JDBC transaction runner；
- 买方冻结资金后在同一事务中结算。

但地产购买没有 `BUYER_PENDING_CLAIM`：成交事务提交时产权立即过户，随后刷新本服保护
索引。事务应锁定 listing、title、买方账户和卖方账户，并依次完成：

1. 重验服务端当前买家、挂牌状态、产权版本及买卖双方不同；
2. 冻结并结算买方资金；
3. 资金进入卖方个人账户；
4. 修改 `owner_player_ref` 并递增 title version；
5. 关闭 listing，写交易与审计记录；
6. 提交后刷新内存保护索引并向双方发送结构化回执。

需要在银行枚举中增加清晰业务类型，例如 `LAND_PURCHASE_FREEZE`、
`LAND_PURCHASE_SETTLEMENT`、`LAND_LISTING_FEE`；不能继续把地产交易伪装为普通商品
`MARKET_ORDER_SETTLEMENT`。首轮建议不收挂牌费和税，只处理买方到卖方的价款。

如果提交后缓存刷新失败，数据库产权仍是真相；对应区块进入本服 fail-closed 状态，禁止
旧所有者继续写入，后台从数据库重建索引。不得自动重复扣款。

## 6. 融入现有终端 UI

SU 的 `GuiClaimedChunks` 依赖其自己的 GUI、network wrapper、team color 和客户端全局
数组，不能放进当前 BetterQuesting 风格终端。JGB 应增加顶层 `PROPERTY` / “地产”页，
复用现有终端壳、session token、action、snapshot、popup、Toast 和主题组件。

推荐单页四个页签：

1. `附近地皮`：15x15 区块网格，中心为玩家当前区块；
2. `我的地皮`：个人产权列表；
3. `在售地产`：固定价挂牌浏览；
4. `交易记录`：本人挂牌、购买、出售和异常历史。

页面布局不照搬 SU：

```text
顶部：当前服 / 维度 / 当前区块 / 已认领数 / 银行可用
页签：附近地皮 | 我的地皮 | 在售地产 | 交易记录
左侧：区块地图或列表（唯一滚动区）
右侧：选中地皮详情、坐标、状态、价格和主操作
底部：分页/图例固定，危险动作使用精确确认 popup
```

附近地图的算法可以复制改造：15x15 窗口、中心偏移、相邻格边界、已加载斜线和拖选；
第一版只允许单选，所以可以先不搬拖选。地图底图可先使用简洁区块网格，不必为了复刻
SU 地形预览而引入其整个 GUI library。

### 2026-08-24 Batch C.1 真实区块地图修订

Batch C 的纯按钮网格只完成了产权动作验证，实际人工测试证明它缺少地形、玩家位置和
产权边界，不适合作为正式交互。Batch C.1 因此吸收 SU `ThreadReloadChunkSelector` 与
`BuiltinChunkMap` 的地形采样和玩家标记思路，但仍不引入 SU GUI、Team、网络或运行时。

- 客户端只读取已经加载的区块，按顶部方块 `MapColor` 生成 16x16 地形瓦片；未知区块
  使用斜纹占位，不请求服务端地形，也不主动加载区块；
- 产权由服务端按受限视口返回稀疏覆盖记录，玩家身份、服务器和维度仍只取服务端；
- 地图支持近/中/远三级缩放、拖拽平移、定位和刷新，视口中心最多偏离玩家 16 区块；
- 地图浏览不扩大产权操作范围，认领继续限制在玩家当前区块周围 15x15；
- 地形采样派生文件保留 LGPL SPDX 和上游来源说明，JGB 发布包继续携带完整许可证。

### 2026-08-24 Batch C.2 SU 交互取舍与大地图

进一步核对 SU `GuiClaimedChunks` / `GuiChunkSelectorBase` 后确认：SU 使用左键认领、右键
放弃，Shift 切换强加载，Ctrl 框选，Tab 隐藏覆盖层。JGB 只吸收其“地图占主体、工具贴边、
悬浮承载上下文”的交互经验，不复制批量和强加载合同。

- 左键只选择、拖拽只平移；右键先取得目标的服务端权威快照，再打开认领/放弃菜单，
  写动作仍进入精确确认；
- `F` 定位、`R` 刷新、`+/-` 缩放、按住 Tab 临时隐藏产权，图层按钮保存本页显示偏好；
- 矩形地图最长边按近 9、中 15、远 31 个区块计算，所有区块保持正方形；
- 玩家标记使用本地真实皮肤头部和方向箭头，不把皮肤或地形加入服务端网络；
- SU 的 Shift 强加载、Ctrl 框选和右键直接破坏性操作继续明确排除。

终端接线需要增加：

- `TerminalPage.PROPERTY` 和顶层导航图标；
- `TerminalLandSectionSnapshot` / `TerminalLandSectionModel`；
- 上述 `LAND_*` action types 与 payload；
- `TerminalService` 中的 land facade/context/page snapshot；
- `OpenTerminalApprovedMessage` 与 `TerminalSnapshotMessage` 的结构化序列化；
- `TerminalLandSection`、地图 panel、列表、确认 popup 和 Toast 定位；
- PROPERTY 页面布局、序列化、权限和动作回归测试。

MARKET 总入口可以增加“地产交易”卡片，点击后路由到同一个 PROPERTY 页的“在售地产”
页签；不要再创建第二套地产服务。PROPERTY 是产权与管理入口，MARKET 只是交易入口。

## 7. 个人首版与旧领地迁移边界

S1/S2 旧 claim 属于 ServerUtilities team。组织关系尚未定稿时，不能把多人 team 的地产
自动发给 team owner 或某个成员，否则会在迁移中创造未经确认的个人产权。

因此：

- 个人首版先在不带旧 claim 的隔离实例或 Lobby 灰度；
- 可以验证“只安装 JGB、完全不安装 ServerUtilities”是否正常；
- 旧 claim 只导入 migration staging，保留旧 team ID、owner、members 和坐标；
- 只有能够明确证明为单人 team 的记录，才可进入人工确认候选，也不自动激活；
- S1/S2 的最终卸载必须等待组织/个人/公共关系定稿和完整迁移；
- 这不阻塞个人认领和个人地产交易功能本身的开发与验证。

## 8. 推荐实施批次

### Batch A：可编译的复用尖峰

- 加入第三方许可证与文件头规范；
- 复制改造 `ChunkDimPos`、`ClaimResult`、`ClaimedChunk` 的个人版；
- 建立内存 `LandProtectionIndex` 和纯 Java 规则测试；
- 不接数据库、不接 UI、不部署。

### Batch B：个人 claim 与保护

- 增加版本化 PostgreSQL migration 和 schema fail-fast 校验；
- 实现 claim/unclaim/query 和事件保护；
- 在无 SU 的隔离服务端验证重启恢复与保护矩阵。

### Batch C：终端地产页

- 加 `PROPERTY` 路由、快照、15x15 网格和精确确认；
- 先交付“附近地皮 / 我的地皮”，交易按钮保持关闭；
- 验证只表格/地图滚动和高 GUI Scale 边界。

### Batch D：个人地产交易

- 加挂牌、撤牌、购买、银行结算和交易历史；
- MARKET 卡片跳转 PROPERTY 的在售页签；
- 验证重复 requestId、确认前过户、余额不足、自买、缓存刷新失败和数据库断连。

### Batch E：旧数据 staging

- 只读导出旧 team claims；
- 生成冲突与待确认报告；
- 不在组织模型确定前做 S1/S2 最终过户或删除。

## 9. 本轮明确不做

- Team、盟友、公共产权和组织银行账户；
- 职业、贡献度或声望决定 claim 配额；
- 批量框选、多区块 parcel、拍卖、租赁、抵押和周期地租；
- 直接复制 SU 的 Universe、ForgeTeam、PermissionAPI、network wrapper 或 GUI framework；
- 在个人 MVP 完成前复制 chunkloading 全套；
- 修改、关闭或删除 S1/S2 当前 ServerUtilities。

## 10. 2026-08-22 Batch A 实现记录

本轮已经完成 Batch A 的可编译代码尖峰：

- 增加 `LandModule` 并注册进 `ModBootstrap`；
- 增加跨服不可变区块键、个人产权、产权状态、动作结果与结构化回执；
- 增加 `requestId + semanticsKey` 幂等的个人认领/放弃服务；
- 服务端规则支持单人上限、禁用维度与保留区块；
- 放弃产权重新校验当前所有者、版本和产权状态；
- 增加可从仓储快照重建的只读保护索引，写入时采用不可变快照替换；
- 增加内存仓储供规则尖峰和纯 Java 测试使用；
- 增加第三方声明、派生文件 SPDX/来源头，并将 LGPL 全文和第三方声明打入发布 JAR。

当前实现刻意还不能在游戏中认领或交易。`LandModule` 使用进程内仓储，默认个人上限 4
只是测试期占位，不是正式制度参数；进程重启后数据不会保留，也没有 Forge 保护事件、
PostgreSQL migration、终端 PROPERTY 页面、银行结算或旧 SU 数据迁移。下一批必须先完成
PostgreSQL 真源、配置化规则和事件保护，才能进入无 SU 隔离服实测。

验证结果：Docker Gradle 地皮定向测试通过，`assemble` 通过；构建 JAR 已核验包含
`META-INF/THIRD_PARTY_NOTICES.md` 与
`META-INF/licenses/ServerUtilities-LGPL-3.0-or-later.txt`。本轮未部署、未删除任何
ServerUtilities JAR，也未触碰 S1。

## 11. 2026-08-22 Batch B 实现记录

Batch B 已增加 `land_title` 与 `land_operation_log` 版本化 migration、JDBC 仓储、共享事务、
schema fail-fast 和服务端配置。放弃产权不再物理删除，而是写入 `REVOKED` 和新版本；旧
claim 请求在撤销后重放也只返回原回执，不会把过期产权重新放回保护索引。

保护执行层覆盖方块破坏/放置、方块与有目标物品交互、非玩家实体攻击、爆炸受影响方块和
假玩家。`SHADOW` 只记录，`ENFORCE` 才取消；玩家 PvP 不变，假玩家默认拒绝。当前仍没有
玩家命令或终端入口，也不保证覆盖绕过 Forge 事件的机器、跨界活塞和自然流体传播。

Lobby 首轮只允许启用 `SHADOW`，并继续保留 ServerUtilities。终端认领入口和人工保护矩阵
完成前，不允许切换 `ENFORCE` 或删除独立 ServerUtilities。S1、S2 与旧 Team claim 不在
Batch B 范围内。

## 12. 2026-08-23 Batch C 实现记录

Batch C 已接入独立 `PROPERTY` 顶层页面、结构化网络快照、15x15 附近地皮网格、“我的地皮”
服务端分页以及 claim/unclaim 精确确认。身份、服务器和维度均从服务端当前玩家运行态取得；
客户端只提交选中坐标、页签、页码、requestId 和放弃所需 expectedVersion，任意越界空坐标
不会成为远程认领入口。

本批仍不显示“在售地产 / 交易记录”，不接银行，也不改变保护切换门槛。自动化验证完成后
部署范围仅为 Lobby 与客户端；Lobby 保持 `SHADOW` 并保留 ServerUtilities。实际玩家、假人、
机器、爆炸、流体和跨区块行为仍属于最后的人工矩阵。
