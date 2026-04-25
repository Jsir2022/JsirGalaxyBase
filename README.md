# JsirGalaxyBase

GTNH 1.7.10 自定义制度模组工作区。

这个项目的目标不是做一个零散功能合集，也不是做一个普通业务后台。
当前正式目标是做一个面向 GTNH 服务器的制度核心模组，并在同一个模组内逐步承载后续玩法能力扩展。

## 项目定位

`JsirGalaxyBase` 当前要解决的是下面几类问题：

- 职业
- 经济
- 贡献度 / 声望
- 福利
- 公共订单 / 公共工程
- 群组服同步核心状态
- 跨服传送相关状态
- 后续玩法能力模块
  - 例如：`共享背包`
  - 例如：`市场终端`

当前判断很明确：

- `CustomClient` 不是主要 Java 源码仓
- 真正的开发主体是 `JsirGalaxyBase`
- 这个项目应优先保持为：
  - `一个 mod jar`
  - `一个制度核心`
  - `多个内部模块`

## 设计原则

### 模块化单体

- 交付物保持为一个 `mod jar`
- 不拆成多个强耦合小模组
- 代码按领域和能力做内部模块划分

### 制度核心与玩法能力分层

- `制度核心模块`
  - 负责长期稳定的服务器制度规则
  - 包括职业、经济、声望、订单和跨服状态
- `能力模块`
  - 负责可扩展的玩法和工具能力
  - 例如共享背包、市场终端

能力模块不能直接侵入制度核心内部状态。
如果需要和职业、权限、领地、贡献度联动，应由能力模块上报事实，再由制度核心解释规则和结算结果。

### 服务端权威

- 客户端只负责输入、展示和界面
- 规则判定、状态变更、掉落处理、账本变更必须由服务端主导
- 任何可能影响经济、权限、同步一致性的能力，都不能以客户端为真源

### 持久化可替换

- 单服阶段允许落本地存储
- 跨服阶段逐步接入中心化存储
- 领域规则不直接依赖某个具体存储实现

### 群组服存储当前决策

- 群组服一期统一使用 `PostgreSQL`
- 当前不引入 `Redis`
- `PostgreSQL` 既负责制度数据持久化，也负责跨服角色主状态同步
- 一期由模组服务端直接连接 `PostgreSQL`
- 但代码内部必须保留存储抽象层，避免未来切中心后端服务时重写领域规则

当前一期优先级明确为：

1. 完整银行系统
2. 制度数据持久化
3. 玩家主状态跨服同步
4. 免费跨服传送与 `home`

## 当前代码结构

当前代码统一采用下面这套结构：

- `com.jsirgalaxybase.GalaxyBase`
  - Forge Mod 唯一入口
- `com.jsirgalaxybase.bootstrap`
  - 生命周期接入、配置加载、模块装配、命令注册
- `com.jsirgalaxybase.module`
  - 模块抽象、模块上下文、模块管理器
- `com.jsirgalaxybase.modules.core`
  - 制度核心模块
- `com.jsirgalaxybase.modules.capability`
  - 玩法能力模块
- `com.jsirgalaxybase.modules.diagnostics`
  - 调试与开发观测模块

后续如果引入群组服相关工具能力，允许在现有结构下继续扩出：

- `com.jsirgalaxybase.modules.cluster`
  - 群组服同步、跨服传送、服务器目录、在线锁、票据、玩家主状态快照等 cluster 子系统
- `com.jsirgalaxybase.modules.servertools`
  - 面向服务器管理与玩家服务的工具能力模块
  - 例如 `home`、服务器状态展示、全服通知、受制度规则约束的管理型工具入口

## 命名约定

当前项目的命名约定统一如下：

- GitHub 仓库目标名：`JsirGalaxyBase`
- 本地工作目录名：`JsirGalaxyBase`
- 模组展示名：`JsirGalaxyBase`
- 模组 `modid`：`jsirgalaxybase`
- Java 根包：`com.jsirgalaxybase`
- Forge 主类：`com.jsirgalaxybase.GalaxyBase`
- 作者显示名：`Jsir2022`

这几个名字不再混用：

- 不再把本机用户目录名混进 Java 包名
- 不再使用 `CustomMod` 作为项目正式名
- GitHub 远端当前仍指向旧仓库名，待 GitHub 侧完成重命名后再切换到 `JsirGalaxyBase`
- 仓库名与模组名允许不同，但职责要清晰：
  - 仓库名偏工程与版本管理
  - 模组名偏玩家可见与运行时标识

当前内置模块包括：

- `InstitutionCoreModule`
- `ClientItemDumpModule`

后续内置模块允许继续增加：

- `ClusterModule`
  - 群组服共享状态与跨服运行时协调
- `ServerToolsModule`
  - 建立在制度核心与 cluster 子系统之上的服务器工具能力

## 技术选型

### GUI 框架

- 当前已落地的终端 GUI 仍运行在 `ModularUI 2` 上，但这套实现现在只视为存量过渡方案
- 长期默认 GUI 路线已调整为：在仓库内 vendoring 一套去业务耦合后的 BetterQuesting 风格 GUI 框架，而不是继续把 `ModularUI 2` 作为终端长期内核
- 选择原因：
  - 终端不再继续受制于 GTNH 共享运行时 GUI 依赖的 ABI 漂移
  - BetterQuesting 风格的 `GuiScreenCanvas + panel tree + theme registry` 更适合做统一终端应用壳
  - 布局、主题、弹层和页面生命周期可以由仓库自身控制
- 参考策略：
  - `GT5-Unofficial`：只作为历史上 `ModularUI 2` 业务 GUI 组织方式参考，不再作为终端长期框架方向
  - `VendingMachine`：只作为现有 `ModularUI 2` 终端存量实现的对照样本
  - `BetterQuesting`：作为主题化、切片纹理、全屏页面和 panel tree 的主要 vendoring 蓝本
  - `Applied-Energistics-2-Unofficial`：只参考终端式交互、搜索、筛选、滚动和高密度信息布局，不作为 GUI 框架选型

### GUI 当前约定

- 新终端 GUI 默认走仓库内置的 BetterQuesting 风格 screen/panel/theme 体系
- 不再继续把 `ModularUI 2` 作为终端新页面的默认落点
- 如果必须使用原版 `GuiScreen` / `GuiContainer` 或保留旧 `ModularUI` 页面，需给出明确理由，例如兼容现有容器逻辑或阶段性迁移过渡
- 后续 GUI 相关实现优先沉淀为可复用的主题、组件、弹层和 screen builder，而不是单页面硬编码

## 禁止继续回退到旧写法

以下写法不再作为项目规范：

- `Hello World` 风格的验证命令作为主功能骨架
- 把所有逻辑堆在根包下
- 用单个扁平 `Config` 类承载全部未来配置意图
- 用零散静态管理器直接横跨整个项目共享状态
- 先写功能、后补模块边界

## 新功能接入要求

新增功能时，默认按下面顺序判断和接入：

1. 先判断它属于 `制度核心模块` 还是 `能力模块`
2. 再判断它依赖本地存储、共享存储还是外部真源适配
3. 给它建立独立模块类
4. 通过模块生命周期接入事件、命令和网络
5. 最后补 GUI、配置和持久化

## ServerUtilities 整合边界

后续如果参考 `Reference/ServerUtilities` 吸收服务器工具能力，默认遵守下面这些约束。

### 基本判断

- `ServerUtilities` 只作为参考源码来源，不作为需要被“整体并入”的架构模板
- 不允许把 `ServerUtilities` 的包结构、全局单例、生命周期和网络通道整套照搬进 `JsirGalaxyBase`
- 只能按“领域能力拆解后重组”的方式吸收，而不能把它继续当作一个并列的“工具大模组”塞进当前仓库

参考源码里已经混合了：

- homes / teleport / claims / chunkloading
- permissions / ranks / admin panel
- leaderboard / notification / invsee
- 全局 `player data`、`team data`、`universe data`
- 多套自定义网络 wrapper 与客户端 GUI

这些能力在 `JsirGalaxyBase` 中不能继续以 `ServerUtilities` 的原有聚合方式落地。

### 应如何拆解吸收

- 制度规则相关能力，归入 `modules.core`
  - 例如跨服是否免费、home 是否受职业或权限限制、某个工具动作是否允许执行
- 群组服协调相关能力，归入 `modules.cluster`
  - 例如 `ServerRegistry`、`GatewayStatus`、`TransferTicket`、`OnlineSessionLock`、玩家主状态快照、目标服落点恢复
- 面向玩家和管理员的工具能力，归入 `modules.servertools` 或 `modules.capability`
  - 例如 `home`、服务器列表、全服通知、可视化服务入口、必要的管理型工具
- 纯客户端表现层，只保留当前项目自己的 GUI 壳、网络同步模型和交互规范

### 不允许直接照搬的内容

- 不直接复用 `serverutils.ServerUtilities` 这种单入口大模组结构
- 不把 `ServerUtilitiesPlayerData`、`ServerUtilitiesUniverseData`、`ServerUtilitiesTeamData` 这种全局宿主对象原样搬进当前项目
- 不把 `ServerUtilitiesNetHandler` 这种按历史功能堆叠出来的多通道网络注册表直接复制进来
- 不为了兼容参考工程而重新引入跨模块静态管理器、全局数据中心或新的“第二套框架”
- 不把 claims、chunkloading、admin panel、rank GUI 等与当前制度目标无关的能力先整体引入再做裁剪

### 推荐的吸收方式

- 先识别参考能力属于哪一个子系统，再建立当前项目自己的端口与门面
- 先定义抽象，再写适配器
  - 例如 `GatewayAdapter`
  - 例如 `ClusterStateRepository`
  - 例如 `PlayerSnapshotRepository`
  - 例如 `ServerDirectory` / `ServerRegistry`
- 对外部真源或共享存储的访问，继续走当前项目自己的仓储 / infrastructure factory / fail-fast schema validation 体系
- 命令、终端和后续 GUI 应优先依赖当前项目自己的 runtime facade，而不是直接依赖底层 JDBC、网络消息或参考工程中的旧 service

### 当前明确建议

- 如果后续要吸收 `home`、跨服传送、服务器状态展示、公告或其他服务器工具能力，优先新建 `ClusterModule` 与 `ServerToolsModule`
- `InstitutionCoreModule` 应继续聚焦制度真源与稳定规则，不继续扩张成“什么都管”的服务器工具总线
- `ServerUtilities` 中真正可复用的，主要是“功能清单”和“交互经验”，不是它现有的整体实现边界

本条约束的目的，是确保 `JsirGalaxyBase` 继续保持“一个制度核心模组 + 多个内部模块”的架构，而不是退化成把另一套历史工具模组整体嵌进来。

## 群组服一期范围

当前群组服一期默认包含：

- 完整银行系统
- 制度数据中心化存储
- 玩家主物品栏同步
- 护甲栏同步
- 经验同步
- 血量同步
- 饥饿同步
- 免费跨服传送
- 免费 `home`

这里的“共享背包”当前定义为：

- 玩家自己按 `E` 打开的主背包数据

当前不额外承诺：

- 末影箱同步
- 特殊额外槽位同步
- 任意第三方模组私有玩家存档同步

## 文档目录

玩家和协作者可以优先看下面这些文档：

- [docs/README.md](docs/README.md)
  - 文档入口与项目简介
- [docs/WORKLOG.md](docs/WORKLOG.md)
  - 开发工作记录
- [docs/servertools-phase1-command-reference.md](docs/servertools-phase1-command-reference.md)
  - server tools / cluster 第一期命令格式、当前跨服执行边界与数据库落点说明
- [docs/terminal-plan.md](docs/terminal-plan.md)
  - 终端入口、服务端打开链与后续终端壳的实施方案
- [Reference/ServerUtilities/README.md](Reference/ServerUtilities/README.md)
  - 外部参考源码入口，仅用于后续 server tools / cluster 能力拆解吸收，不作为当前项目架构模板
- [docs/banking-system-requirements.md](docs/banking-system-requirements.md)
  - 银行系统一期需求、边界与非目标
- [docs/banking-schema-design.md](docs/banking-schema-design.md)
  - 银行系统一期数据表、约束与事务边界设计
- [docs/banking-postgresql-ddl.sql](docs/banking-postgresql-ddl.sql)
  - 银行系统一期 PostgreSQL DDL 草案
- [docs/servertools-cluster-postgresql-ddl.sql](docs/servertools-cluster-postgresql-ddl.sql)
  - server tools / cluster 第一期 PostgreSQL DDL，覆盖 home/back/warp/tpa/rtp 与 transfer ticket
- [docs/banking-java-domain-draft.md](docs/banking-java-domain-draft.md)
  - 银行系统 Java 领域模型与仓储接口草案
- [docs/postgresql-local-setup-and-migration.md](docs/postgresql-local-setup-and-migration.md)
  - Ubuntu 本地 PostgreSQL 安装、初始化与迁移说明

当前开发默认还会参考工作区中的制度文档：

- `../Docs/设定.md`
- `../Docs/技术边界文档.md`
- `../Docs/做法.md`
- `../Docs/市场经济推进.md`
- `../Docs/下次对话议程.md`

## Work Log 约束

- 后续每次实际代码更改，都要同步更新 [docs/WORKLOG.md](docs/WORKLOG.md)
- work log 只需要简要记录：
  - 变更主题
  - 影响范围
  - 原因
  - 如有必要，附上引用文档
