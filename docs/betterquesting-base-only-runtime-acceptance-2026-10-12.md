# BQ N.9.2 Base-only 隔离运行验收

日期：2026-10-13 更新。状态：N.9.2-D 自动 smoke 已复验；N.9.2-A 仍停在 Minecraft EULA 门槛，Base/Quest 模组尚未加载。隔离 smoke 已包含固定任务书导入和 Base JAR 只读 preflight，但不启动 Forge，不能替代 Base-only 运行时验收。本文是执行合同；阶段证据写入 Worklog。

## 目标

在无 BetterQuesting 的隔离 Base-only Forge 环境中，证明 N.9.1 已自动验收的任务书可由实际 Mod 启动链读取，并验证核心玩家事实、消耗、奖励与持久化链。使用合成新玩家和只读固定 `DefaultQuests` 来源，不迁移旧玩家/Party 数据。

本阶段优先自动化可重复的服务端 smoke test。UI 鼠标/键盘手感、资源包字体与玩家主观体验仍是人工验收，不由 headless 运行或 JUnit 代签。N.9.2 完成也不代表生产切换或 BetterQuesting 全部生态兼容。

## 固定合同与隔离边界

- 所有导入 Quest 为 `PLAYER`；PostgreSQL 是任务定义、进度、奖励状态和恢复记录的权威存储。
- 非消耗目标自动观察；只有消耗目标使用 Quest 级 Detect。浏览详情不消耗背包物品。Choice 持久化/提交使用原始 source index。
- 测试只使用本阶段新建的 loopback 临时 PostgreSQL `_it` 数据库、新建空 world/run directory 和明确合成的新玩家。任务书来源只读挂载。
- 禁止读取 S1/S2/Lobby 的 `Players`、world、权限/白名单或生产进度；禁止使用生产 PostgreSQL、生产 Compose `.env`、生产数据库卷或正式服务连接信息。
- 禁止对正式数据库执行 DDL/migration、部署 JAR、启动/停止正式服务、改正式 Quest/BQ 开关。服务器配置中的 `questPostgresReadEnabled` 只能出现在本阶段一次性隔离配置目录，且只能连接 `_it` 数据库。
- 不清理、覆盖、重命名或复用已有 `run/server`、S1/S2/Lobby 目录。当前仓库 `run/server` 不存在，未来只允许使用新的 ignored build 下专属目录。

## 当前环境评估（只读）

- 可用本机 Docker 镜像包括 `galaxy-dev:local`、`galaxy-gtnh:local`、`galaxy-base:local` 和 `postgres:16-alpine`。开发镜像是构建/测试壳；`galaxy-gtnh:local` 的入口运行 supervisord，默认管理 S1、S2、Lobby、Entrance，**不得用于本测试**；`galaxy-base:local` 属于长期服务边界，**不得启动或复用**。
- 现有 `scripts/start-local-test-stack.sh` 的帮助说明虽称本地手测栈，但实现会先按项目路径匹配并终止已有 Java/Gradle 客户端/服务端进程，再启动两个客户端及 `run/server`。该脚本不满足隔离测试要求，N.9.2 不调用或修改它。
- 当前仓库没有 `run/server`、`run/client` 或 `run/client_traderb`。本轮以 ForgeGradle 的 `runServerWorkingDirectory` 实际运行检查任务，确认运行目录为 `/workspace/JsirGalaxyBase/.gradle/codex-n92-server`；JDK 为 8，主类为 `GradleStartServer`，运行 classpath 共 74 项，筛查到 Base dev JAR 与重编译 Minecraft JAR，未发现 BetterQuesting 或 AE2。该证据只说明 Gradle 参数隔离及 classpath 筛查结果，不代表 Mod 已加载或完整 ModList 已通过。
- 随后仅在 `--network none`、专用目录、loopback 绑定和 Quest/Banks 默认关闭下尝试启动 Forge。Minecraft 显示 EULA 接受提示后退出；执行者输入 `n`，没有替用户接受 EULA。启动未进入 Mod 加载/QuestModule 阶段，没有创建世界或玩家存档。因此 Forge 启动、ModList、干净启动/停止/重启均仍未验证。不要自动设置 `eula=true`；只有用户明确表示接受 Minecraft EULA 后才可继续这一运行门槛。
- `QuestModule` 只有在 Dedicated Server、`questPostgresReadEnabled=true` 且 Institution Core 提供共享 Banking JDBC 时才启动；这不是只读模式，而会安装玩家查询、追踪、Checkbox/Detect、奖励/Choice、消费恢复、事件观察和管理服务。因此测试库必须含经过审查的 schema 与既定 migrations，并使用全新的 `_it` 名称。
- 当前看到的临时 N.9.1 PostgreSQL 容器只用于前阶段，不作为 N.9.2 的数据库目标。N.9.2 使用新的容器、端口、数据库名和报告路径，避免复用批次状态。

## 实施阶段与当前证据

### N9.2-A：隔离启动链证明

- 核对 ForgeGradle `runServer` 的运行目录、Java 运行时、mod source/classpath、退出行为与日志落点；记录实际解析后的目录。
- 准备仅用于 smoke 的最小服务器配置。先让 Quest runtime 保持关闭，验证全新 world 可以启动、干净停止并再次启动；确认没有连接 S1/S2/Lobby 或其它生产服务。
- 若 ForgeGradle 开发运行无法满足 GTNH/AE2 API、Java 版本或运行目录隔离，先记录精确阻碍与最小隔离方案，不改动正式安装目录。

当前进度：运行目录/JDK/classpath 检查通过；真实 Forge 启动停在 EULA 提示，未达到 Base mod bootstrap。GTNH/AE2 兼容性不作结论。

### N9.2-B：临时数据库与任务书初始化

- 新建 loopback-only、tmpfs、无宿主数据卷的 PostgreSQL 容器，限定资源和唯一端口；测试结束后仅移除本阶段明确创建的容器。
- 只对新 `_it` 数据库应用 Quest 所需 schema/migrations，再运行 N.9.1 importer apply 和只读 preflight；保存 source/JAR hash、批次 ID 与只读检查结果。
- 新建隔离服务器配置，Banking 与 Quest 共用这一个 `_it` JDBC；数据库 URL/用户名/密码不得进入 commit、正式配置、服务器日志或 Worklog。

当前进度：本轮新建的 tmpfs/内网 PostgreSQL `_it` 容器仅供隔离自动测试；固定来源 Quest Detect PostgreSQL 用例及 Quest Postgres、Importer 套件已实际执行。尚未为 Forge 新服配置启用共用 JDBC，也未在 Forge runtime 中运行 importer/preflight。

### N9.2-C：真实运行事实与奖励 smoke

- 先确认测试执行器不会伪造客户端身份、绕过服务端授权或触碰真实玩家档案；使用合成 UUID/合成玩家存档。
- 依固定已导入任务验证：背包 Retrieval 的非消耗观察、Quest 级消耗 Detect、物品不足/失败回滚、Checkbox 服务端确认、Crafting 统计来源、Fluid/容器能力的适用边界、Choice 原始索引及 Item/XP/QuestCompletion 奖励。
- 对每个链路断言数据库进度/幂等键/奖励状态与玩家物品/经验变化一致；明确玩家文件和 PostgreSQL 不是跨存储原子事务，需检查失败回滚与恢复记录。
- 至少做一次服务对象重建、一次干净服务器重启后读取同一合成玩家状态，验证进度/领取/Choice 不重复、不丢失；异常关闭的崩溃边界需要单独标为已测或未测，不得用正常重启冒充。

当前进度：已有 JUnit/隔离 PostgreSQL/真实来源画像的自动证据；这些没有运行 Forge 事件总线、真实 EntityPlayerMP 背包/流体容器、奖励玩家存档或服务器重启链。N9.2-C 尚未通过。

### N9.2-D：可重复门禁与人工交接

- 使用专门、可审计的 `scripts/quest-base-only-smoke.sh` 入口；不得复用会杀进程、启动双客户端或默认指向 `run/server` 的脚本。
- 验收日志不包含密码、生产 URL、真实 UUID/用户名；提供容器/测试 DB/临时世界的清理步骤及失败保护。
- 汇总哪些是自动验证、哪些依赖人工 GUI/玩家操作、哪些仍未覆盖；更新本文件、能力矩阵、README 与 Worklog。

当前进度：可重复的隔离 Quest/PG/UI2 回归、根 `test`、`assemble`、自包含检查、固定任务书 apply 和 Base JAR 只读 preflight 均已实现并从全新输出目录通过。该脚本只启动临时 PostgreSQL 与一次性 Gradle 开发容器，不启动 Minecraft/Forge；临时数据库使用 internal-only 网络、tmpfs、无宿主端口/数据卷，固定任务书与预览字体只读挂载。Forge smoke、临时世界与玩家存档仍须越过 EULA 门槛后另行实现/验证。

复跑命令：`scripts/quest-base-only-smoke.sh`；只检查边界、不创建 Docker 资源可用 `scripts/quest-base-only-smoke.sh --dry-run`。运行要求本机已有 `galaxy-dev:local`、`postgres:16-alpine`、固定 S2 `DefaultQuests` 只读目录和系统 Noto CJK 预览字体。该脚本不会接触现有服务器容器或数据库，不代表真实 Forge runtime smoke。

### 2026-10-13 自动 smoke 复验与 Forge 运行准备边界

- 最新可重复运行 `.gradle/codex-quest-base-only-smoke-20261013005734-105810`：236 个 JUnit XML suite、1,159 tests、52 skipped、0 failures、0 errors（1,107 执行）；52 个 skip 全部是非 Quest 业务模块缺少各自隔离 DB 配置的集成测试。Quest/BQ suite 无 skip。62 个正式 Gradle actionable tasks 全部执行成功，包含 Core/Postgres/Importer/UI2、根 `test`、`assemble`、`verifyUi2SelfContained` 和 `verifyUi2QzLicense`。
- 同一新建 tmpfs `_it` PostgreSQL 中，Importer apply 成功：3,744 Quest、47 chapters，batch `bq-9795d6c7fe71e72e`，来源 SHA-256 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`；保留 68 条既有来源警告（64 未放置、4 seeded XOR）。紧接着只读 Base JAR preflight 报告 3,791 个实体未变更。它们证明导入和 Base-only 内容一致性门禁可重复，不证明游戏 Mod 已启动。
- Base JAR `.gradle/codex-quest-base-only-smoke-20261013005734-105810/build/root/libs/jsirgalaxybase-0.0.0-quest-base-only-smoke.jar` SHA-256 `ec370e5eb27982f9694e6e224a8e84fbea382d7eb02edf4e5ab5230db5d762cd`；包含 Quest runtime 与 provenance/第三方许可证，不含 BQ importer CLI、BetterQuesting runtime 或 Java2D Demo。临时容器和 internal-only 网络已由精确名称 trap 清理；报告与 JAR 留在 ignored run 目录。
- CodeGraph `status` 为 up-to-date。`impact QuestModule.serverStarting` 覆盖 10 个符号和 `QuestModuleTest`；`impact InstitutionCoreModule.serverStarting` 覆盖 5 个符号和 `InstitutionCoreModuleTest`；模块顺序由 `ModBootstrap` 固定为 Institution Core → Quest → 其它模块。真实 runtime 配置若启用 Quest，必须同时启用 Banking PostgreSQL 并共用同一隔离 JDBC；Institution Core 会在 Quest 前初始化托管账户，并尝试市场初始化（市场失败会被捕获并降级），Cluster 也会尝试共用连接（自身失败有隔离）。因此 Forge 专用 `_it` 库至少要有经审查的 Banking 基线 schema、Quest 基线 schema 与本合同列出的 Quest migrations；不能复用当前仅用于 importer 的数据库作为未经准备的 Forge 运行库。
- 当前隔离 `runServer` 目录只有单独的 `server.properties`，没有 `eula.txt`；上次启动在 EULA 提示退出，未达到 Base bootstrap。未创建 `eula=true`，未启动 Forge。继续前唯一需要用户提供的前置是明确表示已阅读并接受 Minecraft EULA；此确认只授权在专用 ignored 目录做一次性隔离启动，不代表允许生产操作。

## 2026-10-12 自动验证记录（非 Forge 实机）

- CodeGraph `status` 为 up-to-date；`impact QuestModule.serverStarting` 覆盖 10 个符号，直接命中 `QuestModuleTest` 的关闭/共享连接/奖励配置分支。`affected` 对 QuestModule、玩法事件处理器与事实工厂给出 45 个测试文件候选；本轮覆盖了 Quest/BQ 相关自动回归。
- `:galaxy-quest-postgres:test`：61/61；`:galaxy-quest-bq-importer:test`：103/103。固定真实来源的 `BqSourceProfileQuestDetectPostgresTest` 执行 1/1；全量来源迁移数据库回归执行 14/14。全工程 `test`：236 suites、1,159 tests、52 skipped、0 failures、0 errors（1,107 项执行通过）。52 个 skip 仅为无各自独立测试库配置的非 Quest 套件：Banking 18、Market 16、ServerTools 7、Land 4、Warehouse 4、ItemPolicy 3；Quest/BQ 测试无跳过。
- 全工程 `assemble` 与 `verifyUi2SelfContained` 通过。运行 JAR `.gradle/codex-n92-build/root/libs/jsirgalaxybase-0.0.0-n92-isolated.jar`，SHA-256 `80bb90b99d781c33eb0ade27611b06d4d2ee2fd3fc5ee6eb1c97f3abb1dcb78f`；包含 Base Quest Core 与 BQ 来源/许可证声明，不含 BQ Importer CLI/包、Gson CLI、Java2D Demo。`BqImportedTaskScope` 是 Base 自有 Quest 代码，不是 BetterQuesting Mod 类。构建使用一次性 isolated 输出目录；因已有声明文件 hash 已核验，命令排除了会联网取许可证文本的 `prepareUi2QzLicense`，没有改动正式构建配置。
- UI2 Java2D 全工程测试需要预览 CJK 字体；首次未提供字体的尝试仅在 Demo 视觉测试失败。将系统 Noto TTC 只读挂载并设置 `UI2_PREVIEW_FONT` 后重跑，全量测试通过；字体没有进入源码或 JAR。测试与构建均使用临时开发容器，CodeStyle/Updater/GitVersion 仅通过本次命令参数关闭。
- 本轮没有部署、生产数据库/DDL、正式服务、玩家旧存档或 Quest/BQ 开关操作。测试容器与内部网络在结果记录后按精确名称清理；既有 N.9.1 测试数据库和其它服务不在清理范围。

### 可重复 smoke 入口复验

- `scripts/quest-base-only-smoke.sh` 与 `scripts/quest-base-only-smoke.init.gradle` 现可从全新隔离构建输出执行上述 Quest/Postgres/Importer、UI2、根 `test`、`assemble` 和 `verifyUi2SelfContained`。Qz LGPL 声明改为仓库内固定来源文件，`verifyUi2QzLicense` 按上游 URL/固定 SHA-256 校验；常规构建不再依赖联网下载该许可证。
- fresh run `.gradle/codex-quest-base-only-smoke-20261012055500-2328654`：预热 11/11 tasks，正式回归与构建 62/62 actionable tasks，全部实际执行成功。236 个 JUnit XML suite、1,159 tests、52 skipped、0 failure、0 error（1,107 未跳过）；52 个 skip 均为非 Quest 模块需要各自独立数据库配置的 PostgreSQL 集成用例：Banking 18、Market 16、ServerTools 7、Land 4、Warehouse 4、ItemPolicy 3。Quest/BQ 自动测试未跳过。
- 运行 JAR：`.gradle/codex-quest-base-only-smoke-20261012055500-2328654/build/root/libs/jsirgalaxybase-0.0.0-quest-base-only-smoke.jar`，SHA-256 `ec370e5eb27982f9694e6e224a8e84fbea382d7eb02edf4e5ab5230db5d762cd`。包含 Quest runtime、Qz LGPL 与第三方声明；ZIP 名称审计确认没有 BQ importer 包、Gson CLI、Java2D Demo 或 BetterQuesting runtime 类；Base 自有 `BqImportedTaskScope` 不属于 BQ Mod。
- 脚本退出后临时 PostgreSQL 容器和 internal-only 网络均已清理。测试报告与运行 JAR 留在该唯一 ignored run 目录中。该门禁仍未启动 Forge，不能证明 Mod bootstrap、真实玩家背包/流体事件、奖励存档或停启恢复。

## 自动验收矩阵

| 范围 | 通过条件 |
|---|---|
| Base-only 启动 | Forge Dedicated Server 加载 Base；类路径/ModList 无 BetterQuesting；Quest runtime 在测试配置中可启停；退出码与日志无未处理异常 |
| 数据库隔离 | 唯一新 `_it` DB；loopback-only；显式隔离凭据；DDL 仅作用于此库；source apply 幂等，preflight read-only |
| 个人语义 | 合成两名玩家互相不可见；参与 Party 不会让导入 PLAYER Quest 共享库存、事实或奖励 |
| 事实链 | 至少覆盖代表性 Retrieval、Crafting、Hunt/Checkbox 和可用 Fluid 链；消耗仅由 Quest 级 Detect 授权，查看内容不改背包 |
| 奖励链 | Item/Choice/XP/QuestCompletion 经运行 handler，失败可恢复，重放不重复交付 |
| 重启 | 干净停止/重建后进度、领奖和 Choice 可读取且不重复；异常崩溃单独结论 |
| 安全 | 不读玩家旧存档；不触生产数据库/Compose；不调用终止现有进程的通用脚本；不部署、不触碰 S1/S2/Lobby |

不能在纯 Dedicated Server 中产生的事实（如玩家 GUI、真实流体容器交互或客户端 Container 操作）必须标注为“未覆盖”，通过既有 JUnit adapter/evaluator 测试只能算补充证据，不能冒充 Forge 实测。

## 下一步执行

下一步是请用户明确确认是否接受 Minecraft EULA。确认前不再启动 Forge、不设置 `eula=true`。得到明确确认后，仍需先复核专属目录、loopback 端口、Quest/Banks 配置关闭状态与测试 DB 隔离，再从 EULA 后的 Base mod bootstrap 开始；如果要求读取/复制完整 S2 客户端或服务器目录、引用正式数据库/凭据，立即停止并改用干净 GTNH/Forge 安装来源。
