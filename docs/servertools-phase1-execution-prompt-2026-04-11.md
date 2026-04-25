# Server Tools / Cluster 第一期执行 Prompt

日期：2026-04-11

下面这份 prompt 可直接交给另一个 AI，用于在 `JsirGalaxyBase` 中完成第一期 server tools / cluster 能力的搬运、修改与适配。

---

你正在 `JsirGalaxyBase` 仓库中工作。你的任务不是继续评估，而是直接落地第一期 `ServerUtilities` 工具能力整合。

## 目标

把 `Reference/ServerUtilities` 中与传送 / home / warp / tpa / rtp 相关的能力，按当前 `JsirGalaxyBase` 架构重组进仓库，形成一条可运行的第一期 server tools / cluster 主链。

本次实现优先级高于 GUI。不要做 GUI。直接做命令链、服务链、数据库仓储链和跨服接线预留。

## 必须先读的文档

开始实现前，先完整阅读并遵守下面这些文档：

- `README.md`
- `docs/serverutilities-integration-mapping.md`
- `docs/servertools-phase1-requirements.md`
- `docs/WORKLOG.md`
- 如需参考跨服边界，再读：`../Docs/技术边界文档.md` 与 `../Docs/群组服.md`

## 本次必须实现的功能

第一期必须实现下面这些命令能力：

- `home`
- `back`
- `spawn`
- `tpa`
- `rtp`
- `warp`

注意：

- `home` 必须按跨服能力设计
- `warp` 必须按跨服能力设计
- `tpa` / `back` 等传送命令必须允许跨服
- `spawn` 定义保持不变：传送到 world 的出生点
- 所有这些能力都必须直接接数据库，不允许只做本地 NBT / 文件占位版

## 明确约束

### 1. 不兼容旧命令格式

- 不需要兼容 `ServerUtilities` 或 FTBUtilities 的旧命令参数格式
- 不需要复刻旧别名、旧参数顺序或旧返回文案
- 直接按 `JsirGalaxyBase` 当前体系设计“最新版本”的命令格式
- 但命令语义应保持清晰、稳定、可扩展

### 2. 不做 GUI

- 本轮不实现任何 GUI、终端入口或客户端交互壳
- 不要顺手接入 `ModularUI`
- 不要顺手补命令以外的面板

### 3. 不引入旧框架

- 不把 `ServerUtilitiesPlayerData`、`ServerUtilitiesUniverseData`、`ServerUtilitiesNetHandler` 原样迁入
- 不引入 `ServerUtilities` 的旧 rank / permission handler
- 不引入 `ServerUtilities` 的旧 GUI、旧 admin panel、旧多通道网络注册表
- 不把 `ServerUtilities` 当作一整套框架搬进当前项目

### 4. 权限只做玩家语义占位

- 当前不区分 `op` / 非 `op`
- 只做玩家权限语义
- 但必须为未来接入下面这些制度判定预留接口：
  - 职业
  - 贡献度
  - 声望

### 5. 只做第一期范围

下面这些本轮不要碰：

- claims / chunkloading
- teams / ranks 全套
- invsee / NBT 编辑 / admin panel
- backup / shutdown / pregenerator / watchdog
- GUI

## 架构要求

### 新模块与落点

按当前项目架构新增并接入合适模块，优先考虑：

- `modules.cluster`
  - 负责跨服状态、服务器目录、传送票据、玩家跨服落点与共享真源接线
- `modules.servertools`
  - 负责面向玩家的 server tools 能力，例如 home / back / spawn / tpa / rtp / warp 的应用服务与命令入口

如有必要，可以新增：

- `ClusterModule`
- `ServerToolsModule`

但必须通过当前项目自己的 `ModModule` 生命周期接入，不允许绕过现有模块装配体系。

### 需要先定义的抽象

请先建立当前项目自己的抽象，而不是先搬旧代码：

- `GatewayAdapter`
  - 用于跨服传送请求与目标服接线预留
- `ServerRegistry` 或 `ServerDirectory`
  - 用于服务器 ID、展示名、目标服元数据查询
- `PlayerTeleportRepository`
  - 用于存取 homes、warp 目标、back 记录、tpa 请求、随机传送记录等
- `PlayerTeleportHistory` / `TeleportRecord` / `TeleportTarget` 一类值对象
- `PlayerPermissionPolicy` 或类似权限判定端口
  - 先做玩家权限语义占位，后续可接职业 / 贡献度 / 声望

### 数据库要求

- 直接使用数据库作为真源
- 参考当前银行 / 市场模块的仓储和 JDBC 工厂结构
- 新能力的数据结构与 schema 校验应遵循当前项目已有风格：
  - port / repository 抽象
  - infrastructure factory
  - fail-fast schema validation
- 如果要新增 DDL、schema 设计或 migration 文档，请同步补 docs

### 与银行 / 市场现有结构保持一致

尽量对齐当前项目已经成立的模式：

- 应用服务负责业务语义
- 仓储负责持久化访问
- 基础设施负责 JDBC / schema / runtime 装配
- 命令层只调用当前项目自己的 service / runtime facade

不要在命令里直接堆 JDBC 细节。

## 功能设计要求

### home

- 支持设置、查询、删除和传送
- 必须支持跨服目标
- 数据模型不能只存“当前服维度 + 坐标”，必须允许包含：
  - 服务器 ID
  - 维度
  - 坐标
  - 朝向或必要元数据

### back

- 至少支持“最近一次有效传送落点回退”
- 必须允许跨服
- 回退记录必须落数据库，不允许只靠内存或 NBT

### spawn

- 保持“传送到 world 出生点”这个语义
- 但实现上应纳入新的传送主链，而不是单独手写成旁路逻辑

### tpa

- 支持发起与接受
- 必须允许跨服
- 需要有最小可用的请求 / 接受 / 超时模型
- 先做最小实现，但要为后续撤销、拒绝、冷却预留空间

### rtp

- 必须进入第一期范围
- 可以先做最小可运行版本
- 维度白名单、冷却等可以先做保守默认值，但结构上要留扩展点

### warp

- 按“服务器预定义命名落点”实现
- 必须支持跨服
- 不要先实现成纯本服坐标映射
- 是否允许玩家创建不是本轮重点；先把“系统/制度维护的 warp 可传送”链路做通

## 对参考源码的使用方式

只允许按下面方式使用 `Reference/ServerUtilities`：

- 参考命令语义
- 参考传送与 home 的业务流程
- 参考 `TeleportTracker` / `TeleportLog` / `TeleportType` 这类值对象思路

不允许按下面方式使用：

- 整包复制 `serverutils.data`
- 整包复制 `serverutils.net`
- 整包复制 `serverutils.command`
- 依赖旧 `Universe` / `PlayerData` / `TeamData` 体系作为当前实现宿主

## 代码变更要求

- 只做与本次范围直接相关的最小必要改动
- 保持现有风格
- 新增模块、服务、仓储时沿用当前命名风格
- 新增 docs 时写清楚边界与表结构意图
- 每次实际代码变更后，必须同步更新 `docs/WORKLOG.md`

## 文档要求

如果本轮实现引入了新的数据库表、配置项或命令格式，请同步更新必要文档，至少考虑：

- `docs/README.md`
- `docs/WORKLOG.md`
- 新的 schema / requirements / command 文档

## 验证要求

至少完成下面这些验证：

- 代码可以编译
- 相关测试可运行则运行；如果缺测试，要补最小必要测试
- 命令主链可覆盖：
  - home 设置 / 传送 / 删除
  - back 记录与回退
  - spawn 传送
  - tpa 请求与接受
  - rtp 最小链路
  - warp 查询与传送
- 数据写入和读取走数据库而不是本地 NBT / 文件
- 文档和 WORKLOG 已同步更新

## 推荐执行顺序

建议按下面顺序推进：

1. 建立 `modules.cluster` / `modules.servertools` 模块骨架与运行时装配
2. 定义传送相关值对象、端口、仓储接口和权限占位接口
3. 设计数据库表与 JDBC 仓储实现，并接入 fail-fast schema 校验
4. 实现 `home` / `back` / `spawn` 主链
5. 实现 `warp`
6. 实现 `tpa`
7. 实现 `rtp`
8. 补命令入口与测试
9. 更新 docs 与 WORKLOG

## 最终交付要求

最终交付时，请给出：

- 完成了哪些命令与服务
- 新增了哪些模块、仓储、表结构或配置
- 哪些参考代码是抽取复用，哪些是重写
- 运行了哪些测试或验证
- 还有哪些后续可扩展点留待下一阶段

---

如果实现过程中发现某个细节未定，不要擅自扩大范围；优先采用“最小可运行实现 + 清晰扩展点”的方式完成第一期。