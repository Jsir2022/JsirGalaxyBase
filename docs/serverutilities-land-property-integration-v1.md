# ServerUtilities 领地与 JGB 地产系统集成设计 v1

日期：2026-08-22

## 1. 结论与当前边界

`JsirGalaxyBase` 目前只吸收了 ServerUtilities 的第一批传送能力语义：
`home`、`back`、`spawn`、`warp`、`tpa`、`rtp` 以及跨服 transfer ticket。
终端现有 `SERVER_TOOLS` 页面也只是“群组服传送”页，不代表 ServerUtilities
已经整合完毕。

下面这些 ServerUtilities 能力当前都没有进入 JGB 正式业务链：

- chunk claim / unclaim；
- chunk loading / unloading；
- team、ally、member、moderator 与领地权限；
- 领地地图和区块选择；
- 排行榜、rank、invsee、admin panel 等其他工具。

本设计重新把 **领地保护** 纳入近期最小验证，并把 **地产交易** 作为领地真源稳定
之后的独立业务阶段。地产不是标准商品，也不是市场订单簿里的可堆叠物品。

最终交付形态已经明确：服务器和客户端不再安装独立 `ServerUtilities`。JGB 只把它当作
参考实现与一次性历史数据来源，把需要的 claim、保护、team、区块加载和地图交互能力
按 JGB 的模块边界重新实现到同一个 BaseMod JAR 中。完成数据迁移、行为对照和回退演练
后，独立 `ServerUtilities` JAR、配置和运行时依赖应从目标实例删除。

因此本文提到的“迁移期读取”不代表长期适配器架构，更不允许 JGB 在卸载
ServerUtilities 后仍通过类、API、网络包或运行时文件继续依赖它。

## 2. 实时环境事实

2026-08-22 只读检查确认：

- Lobby、S1、S2 与 Prism 客户端都已有 `ServerUtilities-2.2.2.jar`；
- 三服配置均启用了 `chunk_claiming`、`chunk_loading`、`grief_protection` 与
  `interaction_protection`；
- S1 的 `World/serverutilities/teams/claimedchunks` 当前有 23 个团队领地数据文件；
- S2 同目录有 16 个团队领地数据文件；
- Lobby 当前没有团队领地文件；
- S1 是受保护的正式服，本阶段不修改它的 ServerUtilities 配置、领地数据或运行状态；
- S2 仍有独立世界损坏事项，不能把“JAR 已安装”误当成可执行迁移环境。

因此不能采用“直接删 JAR，然后让 JGB 从空库开始”的方式。已有领地必须先离线导出、
映射、核对和导入 JGB；但迁移完成后的运行时只保留 JGB，不保留双 Mod 共存依赖。

## 3. ServerUtilities 当前提供的能力

参考 `Reference/ServerUtilities` 的实际代码，现有 claim 系统包含：

- 以 `server + dimension + chunkX + chunkZ` 为物理保护单元；
- 领地归属于 ServerUtilities `ForgeTeam`，不是直接归属于银行账户；
- claim、unclaim、load、unload 四类修改；
- 团队 claim 数量与强加载数量上限；
- 方块破坏、放置、方块交互、物品使用、实体攻击等事件保护；
- 团队成员、盟友、假人、爆炸与实体交互的可配置权限；
- 客户端区块网格和 claimed / loaded 状态同步；
- 以世界目录 NBT 文件保存 team 与 claimed chunks。

这些经验值得复用，但它的本地 NBT、Universe、ForgeTeam、rank、旧网络和 GUI 不能
成为 JGB 的新制度核心。参考源码采用 LGPL-3.0-or-later，项目已确认可以直接复制和修改
合适的源码并履行对应版权、许可证和源码公开义务。复制后仍须按 JGB 命名空间、模块
生命周期、网络协议和 PostgreSQL 真源改造，不能因为允许复制就整体搬入旧运行时宿主。
具体代码清单见 `serverutilities-personal-land-code-reuse-evaluation-v1.md`。

## 4. 必须区分的三个概念

### 4.1 领地保护

回答“谁能在这个区块破坏、放置、交互和使用机器”。这是实时游戏执行能力。

### 4.2 产权登记

回答“该地皮的权利人是谁、由哪个服务器和维度承认、是否被冻结或争议中”。这是
制度与审计真相。

### 4.3 地产交易

回答“谁可以挂牌、谁付款、何时过户、失败如何恢复”。这是银行和产权登记之间的
事务业务。

ServerUtilities 当前主要实现第一项，并用 team 文件隐含一部分第二项；它没有提供
满足 JGB 银行幂等、跨服审计和恢复要求的正式地产交易合同。

## 5. 推荐架构：JGB 原生实现，ServerUtilities 只参与一次性迁移

### 5.1 JGB 原生边界

新增 JGB 内部端口：

```text
LandProtectionPort
  - currentChunk(player)
  - findClaim(serverId, dimensionId, chunkX, chunkZ)
  - findClaimsByOwner(ownerType, ownerRef)
  - mayBuild(player, chunk, action)
  - claim / unclaim / load / unload
  - protectionHealth(serverId)

JgbLandProtectionService
  - 从 JGB PostgreSQL 加载本服保护快照
  - 在内存中处理高频方块、交互、实体与机器权限判断
  - 通过版本化变更刷新索引，不在每次事件中查询数据库
```

该端口、服务、事件处理器、网络快照和终端页面全部属于 JGB。不得引用
`serverutils.*` 类型，也不得把“ServerUtilities 缺失”设计成运行时降级状态。

### 5.2 一次性迁移工具

为了保留 S1/S2 现存 claim，单独提供可重复运行但不进入正式运行时的迁移工具。当前
组织模型未定，因此第一步只生成 staging，不直接形成可交易产权：

- 只读解析旧 `serverutilities/teams/claimedchunks` 与 team 数据；
- 导出稳定的中间清单，不在原 `.dat` 上原地修改；
- 保存旧 team ID、owner、members、区块和 loaded 状态；
- 生成数量、坐标、所有者和冲突差异报告，不自动把 team 地产归给个人；
- 同一批次重复执行只能得到同一结果，不能重复创建 staging 记录；
- 独立 ServerUtilities 卸载后，迁移工具不参与服务端启动与玩家操作。

旧 loaded 状态只记录用于审计，默认不直接恢复强加载；新系统按 JGB 配额重新申请。

### 5.3 唯一正式运行态

地产交易需要银行结算与产权变更保持同一制度事务。长期应由 JGB PostgreSQL 保存
产权真相，并由 JGB 服务端缓存执行保护：

```text
PostgreSQL land_title / land_chunk
  -> server startup + incremental refresh
  -> per-server in-memory protection index
  -> Forge block/interact/entity/explosion/fake-player handlers
```

保护事件不能每次查询 PostgreSQL。服务端应在启动时加载本服索引，变更时按版本更新；
数据库短时不可用时，对已有保护区使用最后确认快照并默认拒绝不确定写入。

只有在导入比对和灰度保护测试完成后，才允许在单个灰度服停用并移除
ServerUtilities。验收必须证明 JGB 单独安装即可完成启动、认领、保护、重启恢复和页面
同步。S1 的切换必须另行批准，并使用明确停服窗口；不能被普通 JGB 部署脚本顺带执行。

## 6. 最小数据合同

### 6.1 物理地皮键

```text
LandChunkKey
  serverId
  dimensionId
  chunkX
  chunkZ
```

不同服务器的同维度、同坐标不是同一块地。`serverId` 必须来自服务端运行时，客户端
不得提交或伪造产权所在服务器。

### 6.2 产权

```text
LandTitle
  titleId
  titleVersion
  ownerType       PLAYER / TEAM / PUBLIC
  ownerRef
  status          IMPORTED_UNVERIFIED / ACTIVE / FROZEN /
                  TRANSFER_PENDING / DISPUTED / REVOKED
  createdAt / updatedAt
```

当前首版只允许 `PLAYER`，并先验证个人认领、个人挂牌和个人购买。数据合同可以保留
未来 `TEAM` 与 `PUBLIC` 的枚举扩展点，但不实现相关写动作、账户或页面；组织、个人与
公共的制度关系以后单独定稿。旧 ServerUtilities team 名称目前只作为迁移 staging 线索。

### 6.3 地皮组成

下面是未来多区块 parcel 的扩展模型，不进入当前个人首版数据库：

```text
LandTitleChunk
  titleId
  serverId
  dimensionId
  chunkX
  chunkZ
  protectionVersion
  migrationSource       NATIVE / SERVERUTILITIES_IMPORT
  migrationRef
```

个人首版每个 `land_title` 直接保存一个 server/dimension/chunk 坐标，不建立
`land_title_chunk`。多区块地产后续要求同服务器、同维度、连通且无重叠，再引入该表
组合为 parcel；不要一开始同时实现自由多边形、道路权和飞地。

### 6.4 操作日志

claim、unclaim、导入确认、冻结、挂牌、撤牌、购买和过户都需要：

```text
requestId / operationId / titleId / expectedVersion
operatorRef / authorityRef / sourceServerId
beforeOwner / afterOwner / amount / status / failureReason
```

重复 `requestId` 只能返回同一结果，不得重复扣款或重复过户。

## 7. 地皮、Team、职业、金钱与市场的关系

这五类能力不能揉成一个“大公会系统”。下图只记录未来需要讨论的边界，不代表 Team、
公共组织或职业规则已经定稿；本轮实现只走“玩家个人—个人账户—个人地皮—个人交易”。

```text
玩家身份 ──加入/退出──> JGB Team ──拥有/授权──> 地皮产权
    │                         │                    │
    ├──选择/发展──> 职业      └──Team 银行账户     └──领地保护与生产空间
    │                         │                    │
    └──个人银行账户───────────┴──付款/收款────────> 地产市场
                                                     │
标准/定制商品市场 <────地皮内生产与物流事实──────────┘
```

箭头表示允许发生的业务关系，不表示职业、Team、银行或市场可以互相修改对方内部状态。

### 7.1 Team、个人与公共关系暂不定稿

当前只确认它们将来不能共用一个模糊 owner 字符串，也不能把 ServerUtilities team 直接
当作 JGB 正式组织。Team 是否等同公会、是否允许多个组织、公共产权由谁管理、组织资金
如何审批、成员退出时共同资产如何处理，都留到个人闭环验证后专门讨论。

因此首版不建 Team 表、不建公共产权、不提供组织账户动作，也不自动迁移旧 team claim。
旧 team ID、owner、members 和区块只进入 migration staging，避免提前替玩家决定产权。

### 7.2 职业是个人发展与规则输入，不是产权主体

现有文档只确认职业将参与权限、贡献度、声望和奖金，但尚未确定职业种类、升级方式和
收益规则。因此地产首版不能被职业设计阻塞，也不能临时发明“地产商职业”。

职业未来可以向制度核心提供规则输入，例如：

- 是否获得额外 claim 配额或特定公共设施使用资格；
- 某类生产、公共工程或市场行为产生怎样的贡献度；
- 职业奖金、任务奖励或手续费优惠采用哪个版本化规则；
- 某些危险机器或工具是否需要职业资质。

职业不应直接决定：地皮归谁、是否能卖 Team 的地、是否能花 Team 资金、成交后钱归谁。
这些分别由产权、Team 授权和银行账户决定。玩家换职业也不能导致地皮自动转移或失效；
若职业只影响新增配额，已有地皮超限时应禁止继续认领，而不是自动没收。

### 7.3 金钱由银行统一结算

- 玩家购买个人地产：个人银行账户付款，产权登记到玩家 UUID；
- 玩家出售个人地产：净款进入个人账户；
- 挂牌费、成交手续费或系统出售公共土地的收入进入明确的系统运营账户；
- 职业奖金、BQ/公共任务奖励只能通过正式系统账户和银行流水发放；
- claim 配额、贡献度和声望不是货币，不得混入 `available_balance`。

最小领地验证阶段建议认领免费，不引入周期地租或自动扣费。这样可以先证明保护正确，
也避免玩家在不知情时持续掉钱。以后若加入认领费、维护费或土地回收，必须独立设计
余额不足、通知、宽限期、冻结和申诉规则，不能直接静默没收。

### 7.4 地产市场是独立市场品类

地产是唯一、有位置、不可堆叠并可能归 Team 所有的资产：

- 不进入标准商品连续订单簿；
- 可复用定制市场的“固定价挂牌、托管状态、购买确认、历史和审计”经验；
- 但必须使用独立的 `LandListing`、产权版本和过户事务，不能把区块伪装成 `ItemStack`；
- 地产交易页可以挂在 MARKET 总入口下，也可以从 PROPERTY 页进入同一地产挂牌服务；
- 商品市场交易的是地皮中生产出的物品，地产市场交易的是生产空间的产权，两者账本
  可以汇总展示，但订单、交付和恢复状态不能混表。

个人地产挂牌必须验证当前玩家就是产权人。挂牌期间冻结产权变更；成交时锁定挂牌、
产权和买卖双方账户，并在同一 PostgreSQL 事务中完成付款、收款、产权变更和挂牌关闭。

### 7.5 形成的经济循环

个人首版的关系是：玩家通过现有银行或商品交易获得资金，用个人账户购买生产地皮；
地皮承载 GTNH 机器、仓储与生产；产出再进入标准或定制市场；收入回到个人账户。
职业、BQ、Team 与公共制度以后接入时仍不能绕过银行账本和产权审计。

该循环不要求首版同时完成全部模块。现阶段顺序调整为：JGB 原生个人 claim 与保护 →
个人固定价挂牌和购买 → 旧数据 staging → 再讨论 Team、公共产权与职业/BQ 规则。
未来扩展点可以保留，但首版不以未定稿组织模型阻塞个人交易。

## 8. 地产交易模型

地产属于唯一资产，首版采用固定价挂牌，不进入标准商品连续订单簿：

```text
LandListing
  listingId
  titleId
  sellerRef
  askingPrice
  titleVersion
  status          OPEN / SOLD / CANCELLED / EXPIRED / EXCEPTION
```

首版明确不做：竞价拍卖、租赁、抵押、贷款、税收、地役权、分期付款和做空。

### 8.1 挂牌

服务端验证：

- 当前身份是已确认产权人；
- title 为 `ACTIVE` 且版本一致；
- 所有组成区块仍由对应保护执行层承认；
- 地皮不在公共保留区、争议、迁移或其他挂牌中；
- 强加载状态与交易分离，挂牌不自动出售 chunk-loading 权益。

挂牌后产权进入可交易冻结：允许正常使用，但禁止拆分、合并、再次挂牌或变更关键
授权。

### 8.2 购买与过户

购买必须使用银行可用余额和服务端当前玩家身份：

1. 锁定 listing、title、买卖双方银行账户；
2. 重验挂牌、产权、版本、买卖双方不同、价格与保护映射；
3. 扣买方、入卖方并记录银行业务引用；
4. 修改产权人、关闭挂牌、记录过户操作；
5. 刷新本服 JGB 保护索引；
6. 返回结构化回执。

如果产权数据库事务失败，银行事务必须整体回滚。若数据库已经提交但本服保护缓存尚未
确认新版本，产权进入 `TRANSFER_PENDING` 或 `RECOVERY_REQUIRED`，禁止自动再次扣款；
管理员通过受审计恢复动作收口。保护索引可以从已提交的 PostgreSQL 真相重建，因此它
不是第二产权真源。

### 8.3 强加载

产权与 chunk loading 必须分离：

- 买地不等于购买永久强加载额度；
- 过户时原有 loaded 状态默认关闭；
- 新产权人按自己的额度和规则重新申请；
- chunk-loading 额度、衰减、离线策略和服务器性能属于独立制度。

## 9. GTNH 保护风险

只拦截玩家 `BreakEvent` / `PlaceEvent` 不能宣称完成领地保护。GTNH 还需要验证：

- 假人和机器玩家；
- 采矿机、采石场、泵、机械臂、管道和物流；
- AE2 接口、存储总线和自动化；
- 流体流动、活塞、爆炸、火焰、末影人和实体破坏；
- 容器、机器 GUI、按钮、扳手和配置工具；
- 跨区块多方块机器；
- 管理员绕过与审计。

ServerUtilities 已覆盖其中一部分，但不能从源码存在处理器推断整合包内所有机器都
安全。正式替换前必须建立实际 Mod 行为矩阵。

BanItem / ItemBlacklist 的物品策略适配以后可补充“某些危险工具不得在非授权领地
使用”，但它不是领地最小保护验证的前置条件。

## 10. 终端页面

新增独立顶层 `PROPERTY` / “地产”页，不把它塞进现有“传送”页，也不伪装成标准
商品市场。

### v0 原生保护验证页

- 当前服务器、维度、区块坐标；
- 当前区块是否被认领；
- JGB 产权主体、保护版本与 loaded 状态；
- 当前玩家个人 claim 配额；
- 本维度附近的简单区块网格；
- “刷新”“查看我的领地”和单区块 claim / unclaim；
- 不提供 Team、交易和批量操作。

### v1 领地操作页

- 选中单个区块；
- claim / unclaim 精确确认；
- 服务端权限、配额、维度和冲突复核；
- 修改后的快照和审计回执；
- chunk load / unload 放在独立控制区。

### v2 地产页

- “我的产权 / 在售地产 / 交易历史”三个页签；
- 固定价挂牌、撤牌、购买与过户；
- 银行余额、产权状态、保护健康和风险提示；
- 异常过户只进入恢复中心，不自动重复执行。

## 11. 最小验证与迁移顺序

### Phase 0：冻结参考行为与历史清单

- 不改 S1，不修改任何旧 team/claim 文件；
- 从参考源码和隔离测试记录 claim、unclaim、load、unload、地图及保护行为；
- 生成 S1/S2 历史 claim 的只读导出清单和校验摘要；
- 把参考结果固化为 JGB 测试夹具，而不是建立运行时 Adapter；
- 这一阶段结束后，JGB 的编译和启动仍不得要求 ServerUtilities 存在。

### Phase 1：JGB 原生个人地皮闭环

- 建立 `land_title`、`land_title_chunk`、操作日志和本服内存保护索引；
- 实现单区块 claim / unclaim、查询、配额和版本校验；
- 实现 JGB 自己的网络快照与 PROPERTY 页面；
- 在完全不安装 ServerUtilities 的隔离实例验证重启恢复；
- 不启用银行收费、Team 和地产交易。

### Phase 2：GTNH 保护矩阵

- 验证玩家、假人、爆炸、容器、机器、管线、AE2 与跨区块多方块；
- 数据库断连时对已知保护区 fail closed，对未确认写动作给出明确反馈；
- 验证管理员受审计绕过和公共保留区；
- 保护矩阵未通过前不迁移真实地产。

### Phase 3：个人固定价地产交易灰度

- 只开放已确认、无争议的单区块产权；
- 只支持个人账户付款、个人产权过户和个人收款；
- 银行、挂牌和产权在同一事务中完成，缓存刷新失败进入恢复；
- 不开放拍卖、租赁、抵押、职业优惠和自动地租；

### Phase 4：无旧领地灰度卸载

- 在隔离实例或 Lobby 备份并移除 ServerUtilities；
- 证明只安装 JGB 时启动、个人认领、保护、交易、页面和重启恢复均正常；
- 保留明确回退包，但不保留长期双写或运行时依赖。

### Phase 5：旧数据 staging 与组织制度后续

- 将旧 team claim 导入 staging，生成冲突和待确认报告，不自动归给个人；
- 组织、个人和公共产权关系定稿后再设计正式映射；
- S1/S2 的最终迁移与独立 ServerUtilities 删除必须另行批准停服窗口。

## 12. 当前待确认的产品问题

1. 最小地皮是否固定为一个 16x16 chunk；本文建议是。
2. 个人是否必须加入 Team 才能认领；已确认首版不需要，个人可独立认领和交易。
3. 哪些服务器和维度允许新增地皮；Lobby、出生区和公共设施默认禁止。
4. 首版 claim 是否免费；建议先免费验证保护，地产挂牌才收取独立手续费。
5. 地皮上已有机器和物品是否随产权整体转移；技术上保护权会转移，但游戏内物品所有权
   无法逐件证明，因此确认框必须明确“按现状交付”，争议制度后置。
6. 是否允许出售正在强加载、包含公共设施或跨越公共道路的地皮；首版全部禁止。

## 13. 非目标

- 不整体复制 ServerUtilities 的 Universe、team、rank、网络和 GUI；
- 不让正式运行态依赖独立 ServerUtilities JAR、API、类或数据文件；
- 不直接编辑现有 `.dat` 文件完成过户；
- 不把地皮塞进标准商品订单簿；
- 不在最小验证中实现税收、租赁、抵押、拍卖、企业和公共产权；
- 不因普通开发部署而触碰 S1、清空现有 claim 或提前关闭现有保护；
- 不把 chunk loading 与产权绑定出售。
