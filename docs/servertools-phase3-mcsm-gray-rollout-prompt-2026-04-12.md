# Server Tools / Cluster 第三阶段 MCSM 灰度联调准备执行 Prompt

日期：2026-04-12

下面这份 prompt 用于推进 `JsirGalaxyBase` 当前 `servertools / cluster` 的下一阶段工作。

这不是继续改第二阶段 cluster 代码语义的 prompt。

这也不是直接动线上 S1 的 prompt。

你的任务是：

- 在现有群组服资源中，只利用 MCSM 下的代理、大厅、S2 三个实例
- 在明确不触碰 S1 的前提下
- 把 `JsirGalaxyBase` 灰度联调环境准备到“可启动、可进入、可观察、可继续做跨服联调”的状态

---

你正在同时使用下面两个工作区：

- `JsirGalaxyBase`
- `GroupServer`

请直接在现有仓库与群组服目录中工作，不要写空规划。

## 开始前必须先读

开始执行前，先完整阅读并遵守下面这些文档：

- `README.md`
- `docs/servertools-phase1-command-reference.md`
- `docs/servertools-phase2-cross-server-gateway-prompt-2026-04-11.md`
- `docs/servertools-phase2-cluster-close-prompt-2026-04-12.md`
- `docs/servertools-cluster-postgresql-ddl.sql`
- `docs/WORKLOG.md`
- `../.github/skills/gtnh-server-admin/SKILL.md`

如果你需要核对群组服与制度边界，再读：

- `../Docs/技术边界文档.md`
- `../Docs/群组服.md`

## 当前已确认的实服资源边界

当前群组服资源位于 `../GroupServer/`，并由本地 MCSM 管理。

本轮只允许使用下面三套实例：

1. 代理：`../GroupServer/Galaxy_GTNH_Entrance`
2. 大厅：`../GroupServer/Galaxy_GTNH_Lobby`
3. S2：`../GroupServer/Galaxy_GTNH284_S2`

对应的 MCSM 实例信息已经确认：

- 代理实例昵称：`Galaxy_GTNH_Entrance`
- 大厅实例昵称：`Galaxy_GTNH_Lobby`
- S2 实例昵称：`Galaxy_GTNH284_S2`

当前已确认的网络拓扑：

- Velocity 监听：`0.0.0.0:25566`
- Lobby 后端：`127.0.0.1:25564`
- S2 后端：`127.0.0.1:25567`

当前代理配置只挂了大厅和 S2，没有把 S1 纳入本轮灰度链。

## 本轮绝对禁止事项

### 1. 不允许动 S1

下面这些事情都不允许做：

- 不修改 `../GroupServer/Galaxy_GTNH284_S1/**`
- 不修改 S1 的配置
- 不重启 S1
- 不停止 S1
- 不把代理流量切到 S1
- 不为了“顺手统一”去给 S1 同步 jar 或配置

只要你需要动 S1，说明你已经超出本轮范围。

### 2. 不允许绕开 MCSM 直接管实服进程

对于代理、大厅、S2：

- 不要手工 `java -jar ...` 直拉
- 不要直接 kill Java 进程
- 不要脱离 MCSM 的受控实例去启动同目录服务

如果需要启停，只允许走 MCSM 管理链路或与之等价的实例级受控方式。

### 3. 不允许把本轮变成新一轮 cluster 代码扩写

本轮重点不是继续加状态机，不是继续改 terminal / market / bank，也不是继续做 GUI。

下面这些范围本轮不要碰：

- `terminal/...`
- `modules.core.market/...`
- `modules.core.banking/...`
- 新命令类别
- 新 GUI
- S1 联机迁移

如果你发现代码层还有小缺口，只允许做“阻塞灰度启动或阻塞联调”的最小修复。

## 本轮目标

本轮只做下面四件事：

1. 产出或确认可部署到 Lobby 与 S2 的 `JsirGalaxyBase` 模组产物
2. 把 Lobby 与 S2 准备到能加载该模组并能正常启动的状态
3. 保持代理只服务 Lobby 与 S2 这条灰度链，不影响 S1
4. 把灰度联调环境准备到下一轮可以继续做真实跨服传送验证的程度

换句话说，本轮验收标准不是“把所有跨服功能在实服跑通”，而是：

- 灰度链实例边界明确
- 实例启动受控
- 模组已部署
- 配置已落位
- 日志和端口可验证
- 不影响 S1

## 当前已知事实

执行前你应默认接受下面这些事实，并围绕它们工作：

1. 当前在线运行的只有 S1
2. 代理、大厅、S2 当前未确认在线
3. Lobby 与 S2 目录下当前还没有现成的 `JsirGalaxyBase` 模组落点
4. Lobby 与 S2 目录下当前也没有现成的 `galaxybase / bank / cluster` 相关配置落点
5. 代理当前只配置了 `galaxy_gtnh_lobby` 和 `galaxy_gtnh284_s2`

因此本轮优先级不是“立刻实测跨服命令”，而是“先把灰度链部署准备好”。

## 必须完成的任务

### 1. 先做灰度链现状审计

先基于真实目录与 MCSM 配置，确认并记录下面内容：

- 代理、大厅、S2 的实例目录
- 各自端口
- 各自 server-id / server-name
- 当前是否在线
- 当前是否已有 `JsirGalaxyBase` 相关 jar
- 当前是否已有相关配置落点

这一步不是可选项。

如果审计结果和上面“当前已知事实”不一致，以实查结果为准，但必须在最终汇报里写清楚。

### 2. 构建可用于实服灰度部署的模组产物

你需要在 `JsirGalaxyBase` 中完成本轮灰度部署所需的构建。

最低要求：

- 使用当前仓库生成最新可部署 jar
- 明确本轮实际部署的是哪个 jar
- 不要把开发态 `runServer` 目录里的产物误当成群组服生产部署文件直接乱拷

如果仓库已有明确发布产物位置，按现有结构使用；如果没有，就按当前 Gradle 构建结果选择最小正确产物。

### 3. 只向 Lobby 与 S2 部署，不向 S1 部署

部署目标只允许是：

- `../GroupServer/Galaxy_GTNH_Lobby/mods/`
- `../GroupServer/Galaxy_GTNH284_S2/mods/`

如果需要配置落点，只允许写到 Lobby 与 S2 对应目录。

你需要根据 `JsirGalaxyBase` 当前实现判断是否还要同步下列配置：

- 模组主配置
- cluster / servertools 相关配置
- banking / JDBC 连接配置

但仍然只允许落到 Lobby 与 S2。

### 4. 让 Lobby 与 S2 的 server identity 与 cluster 语义一致

本轮必须核对 Lobby 与 S2 的后端身份是否与 cluster runtime 预期一致。

至少要确认：

- Lobby 的 `server-id` 是 `galaxy_gtnh_lobby`
- S2 的 `server-id` 是 `galaxy_gtnh284_s2`
- 如有 `JsirGalaxyBase` 自身配置中的本服 serverId，也要与各自实例一致

如果当前模组配置不存在这些字段，就不要凭空发明新配置；
如果存在，就必须确保与后端真实身份一致。

### 5. 补齐启动前置条件

你需要检查并处理 Lobby / S2 启动 `JsirGalaxyBase` 所需的前置条件，包括但不限于：

- 模组依赖是否齐全
- PostgreSQL / banking / cluster schema 是否满足当前 fail-fast 要求
- 配置文件是否缺失
- 代理到后端的链路是否仍保持 Lobby / S2 封闭灰度边界

注意：

- 如果发现数据库 schema 不满足，允许执行既有迁移入口
- 但必须使用项目既有的迁移方式，不要手工临时改表
- 不要顺手改 bank / market 业务规则

### 6. 通过 MCSM 受控方式启动代理、大厅、S2

在部署和配置完成后，按灰度链路启动：

1. 代理
2. 大厅
3. S2

启动后至少验证：

- 端口是否监听
- 进程是否存在
- 日志是否进入稳定运行阶段
- 是否存在 mod 缺失 / 配置缺失 / schema drift / duplicate mod 之类阻塞错误

如果某实例启动失败，本轮要以“修到可启动”为目标，但仍不得扩大到无关模块。

### 7. 只做最小灰度联调探活，不做 S1 级线上切换

本轮启动成功后，允许做最小探活验证，例如：

- 代理是否能看到 Lobby 与 S2
- 后端是否完成 Forge / mod 加载
- 是否存在 `JsirGalaxyBase` 启动期异常

如果环境允许，允许继续做“仅在灰度链内”的最小进入验证；
但不要求本轮一定把所有 cluster 命令链在实服跑完。

本轮核心仍然是：

- 灰度部署落成
- 可启动
- 可继续联调

## 推荐执行顺序

建议按下面顺序做，不要跳步骤：

1. 审计当前代理 / Lobby / S2 / MCSM 现状
2. 在 `JsirGalaxyBase` 中构建最新产物
3. 部署 jar 到 Lobby / S2
4. 补齐 Lobby / S2 必要配置
5. 检查数据库与 migration 前置条件
6. 用 MCSM 方式启动代理 / Lobby / S2
7. 检查端口、进程、日志
8. 做最小灰度探活

## 推荐修改范围

本轮优先允许修改：

- `docs/WORKLOG.md`
- 必要的 `docs/*.md`
- `../GroupServer/Galaxy_GTNH_Lobby/**`
- `../GroupServer/Galaxy_GTNH284_S2/**`
- `../GroupServer/Galaxy_GTNH_Entrance/**`
- 必要时 `JsirGalaxyBase` 的最小阻塞修复

但如果代码修复不是灰度部署阻塞项，就不要顺手改。

## 文档要求

本轮至少要更新：

- `docs/WORKLOG.md`

如果你发现需要把灰度部署步骤沉淀成后续可复用说明，可以新增一份“灰度联调准备说明”文档；
但不要写成空泛规划稿，必须和本轮真实动作一致。

## 最终汇报要求

完成后必须明确汇报下面这些内容：

1. 代理、大厅、S2 的实例映射与端口
2. 本轮实际部署了哪个 `JsirGalaxyBase` jar
3. 哪些文件被部署到 Lobby / S2
4. 哪些配置被新增或修改
5. 是否执行了 migration 或其他前置修复
6. 代理 / Lobby / S2 是否都已成功启动
7. 各自监听地址或端口状态
8. 日志中是否还有阻塞错误
9. 本轮是否做到“不碰 S1”
10. 留给下一轮的明确事项是什么

## 明确禁止事项再强调一次

- 不动 S1
- 不重启 S1
- 不给 S1 部署 jar 或配置
- 不把灰度链路切到 S1
- 不扩大到 terminal / market / bank 新开发
- 不把这轮写成“建议后续做”的空方案

这轮的验收标准很直接：

如果最后 Lobby / S2 仍没有部署 `JsirGalaxyBase`，或者启动链仍不受控，或者过程中碰了 S1，那就不算完成。