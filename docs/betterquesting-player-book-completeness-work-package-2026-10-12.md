# BQ N.9.1 两天工作包：玩家任务书内容完整性与隔离初始化准备

日期：2026-10-12。状态：N.9.1 自动验收完成；N.9.2 真实 Forge/GTNH 运行与人工交互验收未开始。本文是 N.9.1 的唯一执行合同；本次自动证据和未覆盖边界以本文及最新 Worklog 为准。

## 当前执行状态（2026-10-12，持续更新）

N.8 自动基线保留；既有 N.8/N.9.1 dirty worktree 完整保留，未重置、清理、提交或推送。N.9.1-A～D 的代码与自动门禁均已完成：全书映射、codec、纯 Java VisualModel 只读审计通过；任务/奖励/Choice/前置末项可达、搜索和返回上下文回归通过；固定来源边界画廊导出完成；隔离库 apply 与真实 Base JAR 只读预检端到端通过。

最终强制构建/测试的 XML 结果为 236 个 suite、1,159 tests、0 skipped/failure/error：Quest Core 301、Postgres 61、Importer 103、UI2 Core 42、Lab 2、Terminal 32、Demo 5、root 613。随后“未注册物品显示占位”回归单独强制重跑 9/9 通过；该 targeted rerun 覆盖模块测试输出目录，因此当前 XML 文件数不再等于此前完整矩阵，完整矩阵计数取自全包运行时捕获结果，不能把 targeted rerun 后剩余的 XML 数误读为全包数量。

固定来源 SHA `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`：3,744 Quest/47 章，以及 5,620 Task、5,448 Reward、9,529 task component、9,333 reward component、3,062 Choice、5,087 前置链接。6 个真实边界 Quest 在 350×193、427×240、620×340 检查 409 帧、202 次页面转换；新增正式 Document HTML/PNG 画廊 81 帧，每帧附 `.ui2.txt` 与 `.drawlist.txt`，位于 `.gradle/codex-n91-build/root/reports/bq-quest-book-visual/`。常规正式终端视觉门禁 252 场景通过。

`:assemble`、`verifyUi2SelfContained`、`renderGolden`、`verifyTerminalVisuals` 与固定来源覆盖门禁成功；强制构建 58 个 actionable tasks 均实际执行。隔离 CLI 在临时 PostgreSQL `jgb_bq_n91_cli_it` 中 apply 3,744 Quest/47 章/3,791 个实体；随后以本轮 Base JAR 执行只读 preflight，source/JAR hash、schema、APPLIED 批次、实体 UNCHANGED 和章节顺序均通过。运行 JAR SHA-256 `e55f1bb3e772c72a4dc4c1370f2ed92386a2f5275f8bbf5dc93c26b6b6de7988`，路径 `.gradle/codex-n91-build/root/libs/jsirgalaxybase-4e39b96-main+4e39b967aa-dirty.jar`。这些仅是隔离自动证据，不代表 Forge 实机或生产初始化。

本轮隔离 Gradle 定向强制重跑：`StandardWidgetsTest` 6/6、`QuestParticipantManagementVisualTest` 9/9、`TerminalVisualScenarioTest` 4/4，合计 19 项，0 skip/failure/error。另有根项目 `TerminalQuestCenterDocumentTest` 1、`TerminalHomeScreenModelTest` 7、`TerminalQuestActionPayloadTest` 7，共 15 项通过。修复了 TextField 过滤 Enter 键导致搜索输入框无法回车提交的问题；同一回归同时验证输入焦点跨后台 Snapshot 重建保留。

新增 `:verifyBqQuestBookVisualCoverage -PbqDefaultQuests=<read-only source>` 固定来源只读审计。来源 SHA-256 仍为 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`；3,744 Quest 均通过 definition → server snapshot mapper → network codec round-trip → platform-neutral visual model。累计审计 5,620 Task、5,448 Reward、9,529 task component、9,333 reward component、3,062 Choice、5,087 prerequisite link；3,744 个 typed icon 中 514 个带 NBT，图标 NBT 共 100,280 字节；最大描述 5,531 UTF-8 字节，最大单 Quest 编码 Snapshot 33,409 字节。审计没有连接数据库或读取 `Players`。

同一命令选取最大 Task/Reward/Choice/component/说明/前置的 6 个真实 Quest，用正式 `QuestCenterVisualDocument` 在 350×193、427×240、620×340 三档浏览目标、奖励、Choice、前置、说明分页；共检查 409 帧、202 次分页转换，DrawList 零布局告警、节点不越界，最后一页/末项均可达。另为这些真实来源边界导出 81 帧 HTML/PNG 画廊并附布局与 DrawList 文本；两者属于 Java2D/纯 Java证据，不是 Minecraft/Forge 渲染验收。

D 增量：新增离线 `baseOnlyPreflight` JavaExec，要求显式来源/JAR SHA、显式凭据及 `ISOLATED_ONLY`；URL 必须为 loopback 上 `_test`/`_it` 数据库且不能带凭据/query/fragment。数据库查询通过 JDBC read-only 连接，验证 Quest schema、已应用来源批次、所有 Quest/章节逐项 dry-run 为 UNCHANGED、发布数量与章节顺序；不查询玩家进度。`BqMigrationMain` 的 `preview/apply/rollback` 在建连前要求同一隔离 URL 规则。单元 `BqBaseOnlyPreflightTest` 3/3、临时 PostgreSQL `BqMigrationDatabaseIntegrationTest` 14/14 通过；另已对本轮构建的 Base JAR 执行隔离 Gradle CLI `apply` 和只读 preflight：3,744 Quest/47 章、3,791 实体成功发布并全部核验 UNCHANGED，preflight 未写数据库。D 自动验收完成；不等于获准生产初始化或真实新服运行。

工作树同时含先前 N.8 与 N.9.1 修改。继续前检查 `git status` 与逐文件 diff；不得 reset、checkout、批量暂存、清理或覆盖。本机历史 build 目录存在权限保护，Gradle 输出使用 `.gradle/codex-n91-isolated-build.init.gradle` 重定向到忽略目录 `.gradle/codex-n91-build/`，不得改权限或清理旧 `build/`。此前 CodeGraph 已同步 UI2/终端改动；本轮增加 Importer 预检类后必须重新查询其状态/影响链；若 CodeGraph 不可用，应如实记录为工具不可用，不得虚报同步。

本文件是已完成 N.9.1 工作包的合同与证据记录；当前阶段状态只在[能力矩阵](betterquesting-capability-absorption-matrix-2026-09-13.md)维护，执行证据写入 [Worklog](WORKLOG.md)。

## 目标和完成定义

让玩家在 UI2 终端中读完任务说明、查看全部目标及分项进度、查看全部奖励、选择任意合法 Choice，并能搜索和跳转前置任务。准备可重复的隔离数据库初始化与启动预检流程，供后续 Base-only 新服真实运行验收使用。

本包完成须同时满足 N9.1-A～N9.1-D 的验收条件。新增控件、测试数量或截图数量不能单独证明完成；每项必须说明修复了什么内容缺失、如何验证最后一项仍可访问、是否保留服务端授权和原始身份。

工作包启动时估计 15～21 个有效开发小时；N.9.1 自动工作已完成，剩余自动开发工作为 0。后续 N.9.2 Forge/GTNH 验收是新阶段，工作量需在确认隔离服务端、数据库和客户端条件后单独估算，不以本工作包的原估时推导。

## 当前质量评估与接手原则

- 方向保持合理：详情内容沿现有 Snapshot → codec → 平台无关模型 → Minecraft 适配层传递，没有另建任务运行器；Choice/组件保留源索引，typed NBT 仍在 Minecraft 边界解析。
- Choice pager 的 0×0 回归由视口生命周期修正；三档输入测试覆盖长向量末项和原始 Choice index。搜索 Enter、清除、stale snapshot 草稿保护、详情返回上下文和未注册物品占位均有回归。可见前置使用服务端 Query/VisibilityEvaluator 结果，纯 Java自动门禁不代替线上身份或 Forge 授权实测。
- 工作树同时含 N.8 和 N.9.1 的既有未提交改动。文件数量或 `git diff --stat` 不能可靠划分批次。执行者必须先核对 `git status` 与逐文件 diff，只按实际内容继续，不得清理、覆盖或重置。
- 最终全包矩阵为 236 suites/1,159 tests、0 skip/failure/error；固定来源全书映射、6 个边界 Quest 三档全页结构遍历、81 帧 HTML/PNG 画廊、隔离 CLI apply/read-only preflight、assemble 与自包含均 PASS。Forge/GTNH 实机、资源包字体、真实背包/流体和玩家存档仍未覆盖。
- BQ 替代成熟度不适合用单一百分比表达：N.1～N.9.1 的定义导入、既定运行合同、玩家任务书自动完整性和隔离预检门禁已有自动证据；Base-only 新服真实 Forge 运行和人工玩法验收完全未开始。自动阶段完成不等于可停用 BQ。

## 已完成基线和当前缺口

N.1～N.8 的导入、运行接线、参数门禁和代表性自动测试保留为基线。N.8 自动语义矩阵完成不等于玩家端能展示完整任务书，也不等于 Forge 实机通过。

- 固定来源：`/mnt/m600/zhitai-data/gtnh/data/Galaxy_GTNH284_S2/config/betterquesting/DefaultQuests`，仅可只读读取定义目录。
- 来源 SHA-256：`9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。
- 内容：3,744 Quest、47 章、5,620 Task occurrence、5,448 Reward occurrence；64 个未放置 Quest、4 个 seeded XOR warning 保持原有合同。
- 最近已有报告：Quest Core 299、Postgres 61、Importer 99，共 459 项全部执行通过；全工程 1,143 项，52 项非 Quest 环境跳过，零失败/错误；252 个终端视觉场景。此为既有报告，后续开发必须记录新的实际执行结果，不能当作新改动的验收。

2026-10-12 N.9.1 开始前的只读源数据统计与代码复核得到以下缺口。表格是本工作包修复前的基线，不是当前活动缺口：

| 缺口 | 固定来源影响 | 代码证据 |
|---|---|---|
| 详情最多显示 6 个 Task | 18 个 Quest 超过上限，最多 12 个 Task | `QuestCenterVisualDocument.detail/detailLayout` 使用 `Math.min(6, ...)` |
| 详情最多显示 4 组 Reward | 22 个 Quest 超过上限，最多 22 组 Reward | 同页面使用 `Math.min(4, ...)` |
| 每组 Choice 最多显示 4 项 | 70 组超过上限，最多 16 项 | 同页面 Choice build/layout 使用 `Math.min(4, ...)` |
| Item 奖励只映射第一件物品 | 1,486 组 Item Reward 含多件配置 | `TerminalQuestCenterSnapshotMapper.detail` 只取 `item.0.*` |
| Task 没有逐项物品要求与进度 | 1,579 个 Task 含多个物品要求 | Mapper 只生成通用类型名、必需/可选文字及合计进度 |
| 导入图标无法显示 | 3,744 Quest 均有 typed JSON icon | Importer 保存 JSON，Mapper/VisualModelAdapter 将以 `{` 开头的 icon 过滤 |
| 说明只有两行 | 1,968 个说明超过 200 字符，最长 5,523 字符 | `quest-detail-description` 的 `MAX_LINES=2` |
| 搜索尚无可输入入口 | 页面功能缺口，未统计受影响 Quest 数 | `quest-search` 为 Label，页面未调用 `changeQuery` |
| 前置只显示数量 | 无法从详情识别并导航具体前置 | Mapper 只生成“前置任务 N 项” |

以上统计按 Quest、Task 或 Reward occurrence 分别计算，不能互换为受影响玩家数或完成百分比。完整定义仍由后端保存；本包首先修复展示映射与客户端内容可达性。

## N9.1-A：完整详情数据与平台适配（5～7 小时）

交付：

1. 扩展现有服务端 Snapshot、网络 codec、纯 Java VisualModel 和 Minecraft Adapter，使任务要求和奖励能完整对应定义。沿用现有分层，不新增第二套 Quest 系统。
2. 物品类要求显示每项注册名/meta、真实本地化显示名、数量、分项当前进度/目标和必要 NBT；流体显示名称、单位、数量与分项进度；Hunt、Location、Meeting、Block Break、Checkbox 显示本类型的实际目标与必要说明。合计进度可保留，但不得代替分项。
3. Item Reward 显示完整物品向量；Choice 显示全部选项；XP 显示数量/单位；Quest Completion 显示可见目标信息。显示状态与待领取、已领取、投递中/失败等既有权威状态一致，不从客户端推断奖励已经送达。
4. typed icon 从已导入属性解析到现有外部物品区域，保留 meta 和 NBT。纯 Java 模块仍不引用 `ItemStack`、Forge 或 Gson；使用现有平台无关值对象或最小必要扩展。
5. 前置详情包含服务端允许显示的身份、名称、完成状态和导航目标。隐藏/秘密内容仍由服务端决定可见性，不能为了补齐前置名称泄露未授权内容。

约束：

- Task/Reward key、原始 component/Choice index、definition version、Quest/cycle 身份必须稳定。筛选、分页、缺失物品占位不得重编号 Choice 后再把显示索引提交到服务端。
- 未注册或无法解析的物品保留明确占位及原始身份，不能删掉该项或用另一物品替换。客户端不得上传进度、奖励结果、库存范围或完整权威配置。
- 网络保留有界列表、字符串与 NBT 大小检查；超过单页容量采用显式分页/分块合同，不无限放大包，也不静默丢掉尾部内容。未知或 malformed 输入 fail-closed。
- 整改显示层不改变检测、消耗、Choice 锁定、奖励策略、幂等或玩家身份规则。必要的展示 payload 扩展需同时更新编解码与回归；涉及业务语义变更先报告。

验收：真实来源经过 definition → server snapshot → codec round-trip → VisualModel 后，要求/奖励/Choice 的身份、顺序、数量和目标没有丢失；图标从真实 JSON 配置进入显示合同；包含 NBT、缺失注册项、极端数量及隐藏前置测试。

## N9.1-B：玩家页面完整可访问（5～7 小时）

交付：

1. 删除 build/layout 中截断 Task、Reward、Choice 的显示上限；通过滚动、分页或局部页签容纳完整内容，最后一项必须可见、可点。两处布局/渲染逻辑都要整改。
2. 说明区支持完整换行及滚动阅读。保留 BQ/Minecraft 的文本信息和现有合法格式；必要链接行为按已有能力评估，不实现任意 HTML/脚本执行器。正文保持可读字号，不靠无限缩小字体塞入长文。
3. 搜索改为实际输入控件，支持提交、清除和键盘焦点；沿用服务端搜索/分页。返回时保留章节、关键词、筛选、页码与合理的阅读位置。
4. 增加具体前置导航；保留任务追踪、Checkbox、Quest 级 Detect、领取和 Choice 行为。查看目标详情不能触发消耗；Choice 选择发送正确的原始 index。
5. 操作区与内容滚动分离，关键操作在小窗仍可访问；处理加载、空、错误、数据刷新、图标缺失和任务版本变化。玩家默认任务书入口清楚标识个人进度，既有主体管理不冒充导入任务共享进度。

布局自由度：可在现有终端壳内使用“说明/目标/奖励”局部页签或分区滚动，由实现者根据三档尺寸选择。沿用当前主题、字体、窗口设置和 UI2 组件，不重做终端视觉风格。

验收：`350×193`、`427×240`、`620×340` 都能访问所有内容；鼠标滚轮、Tab/Enter、Escape 和返回上下文可测试；当前聚焦项与外部物品位置、裁剪和 Tooltip 对齐。领取、Detect、Checkbox 在授权不满足时具有明确禁用原因。

## N9.1-C：真实内容驱动验收与画廊（3～4 小时）

交付：

1. 对完整固定任务书做只读展示完整性审计，统计 Task/Reward/component/Choice/icon 从定义到展示模型的保留情况。使用稳定来源摘要，避免把 full-book 解析成功当成 full-book UI 可用。
2. 为真实来源的边界任务建立确定性预览：12 个 Task、22 组 Reward、16 个 Choice、最长说明、多物品/多流体、图标 NBT、缺失注册项、隐藏前置与各加载状态。若某最大值属于不同 Quest，分别建场景，不伪称一个真实 Quest 同时具备全部最大值。
3. UI2 三档画廊增加这些场景，自动检查布局、裁剪、区域 ID 和最后一项可达性；输入测试滚动/翻页到末项、选择末尾 Choice、搜索与返回，不能只断言第一页不越界。
4. 精简现有测试中隐含“只显示前 N 项即可”的断言，新增真实缺失回归。测试应先复现内容丢失/不可达，再验证修复。

验收：报告明确区分全量静态映射、源数据代表场景、纯 Java 输入与 Minecraft/Forge 未验收项。Source mount 或隔离测试库缺失时如实记录 skip；源哈希不同则重新统计/报告，不盲目沿用本文频次。

## N9.1-D：隔离初始化与启动预检准备（2～3 小时）

交付：

1. 核对 `QuestModule` 的启用条件、共享 JDBC、DDL/追加 migration 顺序、schema validation 与 offline importer 入口，形成同一份新服初始化清单。注意当前 `questPostgresReadEnabled` 的名称虽含 Read，实际启用运行服务；文档必须准确说明现有行为。
2. 增加可重复的隔离预检/初始化测试入口：只使用明确的临时目录和临时 PostgreSQL `_test`/`_it` 数据库；默认只读，测试写入需显式确认。禁止生产 URL fallback，检查源码/制品/来源哈希、schema 和导入结果，失败输出具体原因并安全退出。
3. 列出无 BQ 环境所需制品/配置和后续真实 Forge 启动步骤，检查 Base JAR 不依赖 BetterQuesting、Importer CLI/Gson 或 Demo 类。保持 GTNH 必需业务适配边界；不把“无 BQ”误解释为删除整合包其余必需 Mod。
4. 验证预检拒绝非隔离库、缺失 schema/来源/制品的情况，并记录临时容器/schema 的清理与报告输出。不创建整套新运维平台。`baseOnlyPreflight`、写操作建连前隔离 URL 限制及完整 Gradle CLI + 刚构建 Base JAR 的 apply/read-only preflight 均已通过本地隔离验收；实际结果见顶部状态与隔离预检文档。

本包只执行隔离数据库/本地代码测试和生成预检材料；不启动 Minecraft 客户端、既有服务器或新的 Forge 服务端，不自动截取游戏/桌面画面。纯 Java 离屏 PNG 导出与画廊检查属于本包验收。真实运行属于 N.9.2，后续单独安排。

## 执行顺序与代码入口

先完成 A 的稳定展示合同，再完成 B 页面与输入；C 可随 A/B 增加真实回归，D 在核心整改完成后收口。第一天优先 A/B，第二天完成 B/C/D 与构建、报告。

开始先读取开发导航、能力矩阵、本文件和当前 Worklog；用 `git status` 保留已有未提交改动。用 CodeGraph `status/sync`、`node/callers/callees/impact` 核对入口，改动后用 `affected` 选择回归。

主要入口：

- `src/main/java/com/jsirgalaxybase/modules/quest/application/TerminalQuestCenterSnapshotMapper.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalQuestCenterSectionSnapshot.java` 与实际 snapshot codec
- `src/main/java/com/jsirgalaxybase/terminal/client/ui2/QuestCenterVisualModelAdapter.java`
- `src/main/java/com/jsirgalaxybase/terminal/client/ui2/TerminalQuestCenterDocument.java` / `NativeItemBridge`
- `ui2-terminal/src/main/java/com/jsirgalaxybase/ui2/terminal/QuestCenterVisualModel.java`
- `ui2-terminal/src/main/java/com/jsirgalaxybase/ui2/terminal/QuestCenterVisualDocument.java`
- `src/main/java/com/jsirgalaxybase/terminal/TerminalService.java` 的授权查询/动作
- `src/main/java/com/jsirgalaxybase/modules/quest/QuestModule.java`、Quest DDL/migrations 和已有 offline importer

现有代码含长行/紧凑写法；只整理本批触碰的相关逻辑，不全仓格式化，不借此重构任务运行器或重新定义 M.2。

## 固定边界

- Base 自包含；PostgreSQL 权威；UI2 正式入口；固定任务书导入为 PLAYER，新玩家从零开始。
- 不读取 `Players`、旧进度/领取/Choice 历史或旧 Party；不恢复旧进度迁移/实时影子路线。
- 不新增职业、公共任务、观察站、BQ 新类型支持或奖励策略；原 BQ 可缩放章节关系图另行安排，本包先完成具体前置导航。
- 不连接生产数据库、不执行生产 DDL、不部署 Lobby/S1/S2/正式客户端、不启用现有服务器 Quest 或停用 BQ；不启动客户端/Forge server，不自动操作 GUI 或截取游戏/桌面画面；允许纯 Java 离屏 PNG 导出。
- 不删除 N.8 已有证据，不宣称完整 BQ 插件生态或全部任务的实机兼容。
- 本包不授权 Git 提交、推送或清理既有工作树。修复与报告留在工作树；记录 HEAD、最终制品与 diff 边界。

## 验收与交付

按风险运行 mapper/codec/UI2/输入定向回归，以及 Quest 三模块、UI2 模块和根测试；执行 `assemble`、`verifyUi2SelfContained`、Golden/终端视觉任务和 `git diff --check`。使用既有 Docker Gradle 环境与受控 worker 数，不加载生产 Compose `.env`。隔离测试数据库独立提供；开发预览 CJK 字体仅只读挂载，不进入运行 JAR。

最终报告记录实际测试数量、failure/error、skip 原因、是否实际执行或 up-to-date、来源哈希、CodeGraph 影响范围、视觉报告路径、JAR 路径与 SHA-256、当前 HEAD 和未完成项。历史 1,143/52 是基线，不是必须凑齐的数字；数量减少必须说明。

更新本文件的任务状态、能力矩阵、开发导航、活动路线、验收记录和 Worklog。阶段只在满足本项完成条件后标为“自动验收完成”；玩家观感/真实 Forge 保持未验收。

| 任务 | 状态 | 完成证据 |
|---|---|---|
| N9.1-A 完整展示合同 | 自动验收完成 | 固定来源 3,744 Quest definition→Mapper→codec→VisualModel 全量只读审计；分项组件、typed icon/NBT、原始 Choice index、缺失项占位与协议向量边界有回归。隐藏内容由服务端既有 Query/VisibilityEvaluator 投影授权，客户端不推导可见性 |
| N9.1-B 页面内容可达性 | 自动验收完成 | 三档 UI2 遍历 409 帧/202 次转换，零布局告警且末项可达；搜索输入/Enter/清除、stale snapshot 草稿保护、返回上下文、Choice 与前置末项有回归；对应终端/输入套件通过 |
| N9.1-C 真实内容自动验收 | 自动验收完成（Java2D/结构） | 6 个真实边界 Quest 的 81 帧 HTML/PNG 画廊及 `.ui2.txt`/`.drawlist.txt` 审计；252 场景正式终端视觉门禁通过。不代表 Minecraft/Forge 像素或玩家观感验收 |
| N9.1-D 隔离初始化预检 | 自动验收完成（隔离环境） | 单测 3/3、临时 PG 集成 14/14；Gradle CLI apply 3,744 Quest/47 章/3,791 实体后，用当前 Base JAR 执行只读 preflight，source/JAR hash、schema、APPLIED 批次、UNCHANGED 内容和章节顺序均通过。无生产连接/DDL |
| N.9.2 Base-only 真实运行 | 未开始，不在本包执行 | Forge、真实玩家/存档/流体、重启与交互另行验收 |

## 后续执行提示

N.9.1 自动工作包已收口。下一入口是独立的 N.9.2 Base-only Forge/GTNH 新服运行验收：在隔离环境验证启动组合、真实背包/流体事实、奖励写入与存档、重启恢复及玩家操作。开始前另立验收合同并审查 `questPostgresReadEnabled` 的完整启用影响；本工作包不授权任何生产 DDL、部署或开关变更。继续维护当前 dirty tree 时仍须先看 `git status`/diff，不得 reset、checkout、清理、覆盖、全量暂存、提交或推送。

产品合同仍为：导入 Quest 为 PLAYER；非消耗任务自动更新，消耗项使用 Quest 级 Detect，查看内容不能消耗物品；Choice 必须提交原始 source index。逐 Task 消耗提交 UI 已从纯 Java合同和终端桥移除，服务端兼容消息保留。N.9.1 自动验收已完成；本地 Java2D/隔离数据库证据不代表真实 Forge 运行、人工交互验收或 BQ 正式替代。N.9.2 仍未开始，生产 DB、DDL、服务器开关与部署需要另行授权。
