# JsirGalaxyBase Work Log

日期：2026-03-29

这份文件用于记录 `JsirGalaxyBase` 的持续开发摘要。
从本次开始，后续每次实际代码变更都应补一条简要 work log。

## 记录规则

- 每次代码变更后，追加一条简要记录
- 每条记录至少包含：
  - 日期
  - 变更主题
  - 影响范围
  - 简要原因
- 如果变更依赖外部制度文档或外部参考源码，应写出引用来源

## 当前关键引用文档

当前开发默认参考下面这些文档：

- `../README.md`
  - 当前项目定位、代码结构和正式架构约束
- `../../Docs/设定.md`
  - 制度目标、职业、市场、贡献度、跨服阶段路线
- `../../Docs/技术边界文档.md`
  - `JsirGalaxyBase` 的责任边界与跨服同步边界
- `../../Docs/做法.md`
  - 群组服、中心数据库、同步方案与后端边界
- `../../Docs/市场经济推进.md`
  - 市场账本、订单、托管库存与一致性要求
- `../../Docs/下次对话议程.md`
  - 当前已定稿制度结论与后续讨论顺序

## 初次对话摘要

### 1. 项目定位

- `CustomClient` 不是当前主要 Java 源码仓
- 真正的开发主体是 `JsirGalaxyBase`
- `JsirGalaxyBase` 目标不是普通业务后台，而是：
  - `GTNH 服务器制度核心模组`
  - 并预留后续玩法能力扩展

### 2. 架构总判断

- 不采用传统 Web 框架优先的思路
- 不采用早期扁平模组写法继续扩展
- 当前正式架构定为：
  - `模块化单体`
  - `制度核心 + 能力模块`
  - `服务端权威`
  - `可替换持久化`

### 3. 当前模块边界

- `制度核心模块`
  - 职业
  - 经济
  - 贡献度 / 声望
  - 公共订单 / 公共工程
  - 群组服同步核心状态
- `能力模块`
  - 共享背包
  - 市场终端
  - 其他玩法增强能力
- `诊断模块`
  - 客户端物品导出
  - 开发观测工具

### 5. 本轮代码重构结果

- 删除了旧的示例式写法：
  - `HelloWorldCommand`
  - 旧 `CommonProxy`
  - 旧 `ClientProxy`
  - 旧扁平 `Config`
  - 旧 `client` 包中的导出控制器
- 引入新的启动和模块骨架：
  - `bootstrap/`
  - `module/`
  - `modules/core/`
  - `modules/capability/`
  - `modules/diagnostics/`

## 条目

### 2026-04-02 - 第二层标准化现货接入服务器运行时

- 主题：把已完成的标准化现货服务层能力正式接入 dedicated-server 运行时、玩家命令入口与人工恢复触发
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/`、`src/test/java/com/jsirgalaxybase/modules/core/`、`src/test/java/com/jsirgalaxybase/command/`、`../../Docs/市场经济推进.md`
- 原因：上一轮虽然已经补齐第二层标准化现货的服务、JDBC 和恢复闭环，但服务器运行时还没有真正挂载市场服务，也没有玩家入口和管理员恢复触发，离“可实际使用”还差最后一段接线
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/技术边界文档.md`
- 结果：
  - `InstitutionCoreModule` 现在会在 dedicated-server 路径下同时装配 banking 与 shared JDBC market runtime
  - `GalaxyBaseCommand` 新增第二层现货命令入口，保留原有 phase-1 `quote/exchange` 路径不混用
  - 卖单创建入口现在会先扣除玩家手持标准化物，再调用市场服务，失败时原样回滚，避免虚空卖单
  - 新增管理员 `market recover` 手动恢复触发与启动时轻量恢复扫描挂点
  - 补充运行时装配测试与命令分发测试，覆盖 dedicated-server 装配、卖单扣物/失败回滚、买单分发、claim 列表与恢复入口

### 2026-04-01 - 市场第三阶段补齐买单恢复与 CLAIMABLE 提取闭环

- 主题：补齐标准化现货市场的买单冻结资金失败恢复闭环与 `CLAIMABLE` 资产提取写路径
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/modules/core/market/`、`docs/market-postgresql-ddl.sql`、`../../Docs/市场经济推进.md`
- 原因：上一轮只完成了最小买卖闭环，买方冻结金异常恢复与玩家真正提取 `CLAIMABLE` 资产仍未收口，存在一致性和可用性缺口
- 引用来源：`../../Docs/市场经济推进.md`、`../../Docs/技术边界文档.md`
- 结果：
  - 新增 `recoveryMetadataKey`，把买单冻结金恢复与 claim 恢复线索显式落到操作日志
  - `MarketRecoveryService` 可释放未完成买单剩余冻结资金，并把订单状态收口到 `CANCELLED / FILLED`
  - 新增 `ClaimMarketAssetCommand`、`CLAIMING / CLAIMED` 状态与真实 claim delivery port
  - claim 成功后托管资产进入 `CLAIMED`，安全失败则恢复到 `CLAIMABLE`
  - 补齐市场单测与 PostgreSQL 集成测试，覆盖买单恢复和 claim 写路径

### 2026-03-29 - 初始化仓库与工作记录机制

- 主题：初始化本地 git 仓库与 work log 机制
- 影响范围：仓库根目录
- 原因：为后续 GitHub 上传、版本管理和持续开发记录做准备
- 结果：
  - 在 `JsirGalaxyBase` 仓库下初始化了本地 git 仓库
  - 建立本 work log 作为统一开发记录入口
  - 补录初次对话形成的架构和制度上下文
  - 后续每次代码更改都应在此追加简要记录

### 2026-03-29 - 排除外层仓库中的 Reference 目录

- 主题：将 `Reference/` 排除出外层 git 仓库
- 影响范围：`.gitignore`
- 原因：`Reference/` 下包含多个独立 git 仓库，直接加入外层仓库会形成嵌套仓库或 gitlink，上传到 GitHub 后不适合作为当前项目源码的一部分
- 结果：
  - 外层仓库不再跟踪 `Reference/`
  - `Reference/` 继续保留在本地，作为开发参考源码使用

### 2026-03-29 - 合并根文档并停用自动化 workflow

- 主题：重写根 README，建立 `docs/` 目录，并停用 GitHub Actions workflow
- 影响范围：`README.md`、`docs/`、`.github/workflows/`
- 原因：不再需要把架构拆成单独根文档，同时希望把面向玩家和协作者的基础材料集中到 `docs/` 目录中，并关闭当前自动化编译流程
- 结果：
  - 根 `README.md` 合并了原先独立架构文档的核心内容
  - `WORKLOG.md` 迁移到 `docs/WORKLOG.md`
  - 新增 `docs/README.md` 作为文档入口
  - 删除现有自动化编译与 release workflow 文件

### 2026-03-29 - 将项目正式更名为 JsirGalaxyBase

- 主题：将项目名称从 `CustomMod` 统一更名为 `JsirGalaxyBase`
- 影响范围：Java 包名、主类名、命令类名、Gradle 模组元数据、配置路径、README 与 docs 文档
- 原因：`CustomMod` 过于临时和泛化，无法准确承载当前制度核心加能力模块的长期定位；`JsirGalaxyBase` 更适合作为正式项目名
- 结果：
  - 根包切换为项目正式包名
  - 主类切换为 `GalaxyBase`
  - 命令切换为 `/jsirgalaxybase`
  - 模组 `modid` 切换为 `jsirgalaxybase`
  - 文档标题与项目引用同步更新

### 2026-03-29 - 排除本机环境名并建立正式命名约定

- 主题：从代码与元数据中移除本机环境名痕迹，并建立正式命名约定
- 影响范围：Java 根包、Gradle 元数据、mcmod 作者字段、README、工作目录命名说明
- 原因：本机环境名不应进入模组正式命名空间；需要把工程名、模组名、包名和仓库名分开定义清楚
- 结果：
  - Java 根包统一为 `com.jsirgalaxybase`
  - Gradle `modGroup` 与生成的 `Tags` 类路径同步改为 `com.jsirgalaxybase`
  - `mcmod.info` 作者显示改为 `Jsir2022`
  - README 新增命名约定章节，明确仓库名、目录名、模组名、`modid` 和包名的分工

### 2026-03-29 - 工作目录与 GitHub 仓库名对齐

- 主题：将本地工作目录从 `CustomMod` 改为 `JsirGalaxyBase`
- 影响范围：本地仓库目录路径
- 原因：保持本地工作目录与 GitHub 仓库名一致，减少工程名、目录名和远端仓库名之间的混淆
- 结果：
  - 本地仓库目录已改为 `JsirGalaxyBase`
  - 当前仓库名、工作目录名和 GitHub 仓库名保持一致
  - 模组运行时名称继续保持为 `JsirGalaxyBase`

### 2026-03-29 - 统一重命名为 JsirGalaxyBase / GalaxyBase

- 主题：将仓库、模组和文档名称统一切换为 `JsirGalaxyBase`，并把代码主类简写为 `GalaxyBase`
- 影响范围：`README.md`、`docs/`、Gradle 模组元数据、Java 根包、Forge 主类、命令类与本地目录命名
- 原因：用户要求统一正式名称，减少旧命名和运行时代号混杂；同时保留代码类名的可读性
- 结果：
  - 模组展示名与 `modid` 改为 `JsirGalaxyBase` / `jsirgalaxybase`
  - Java 根包改为 `com.jsirgalaxybase`
  - Forge 主类改为 `GalaxyBase`
  - 根命令改为 `/jsirgalaxybase`
  - README 与 docs 的命名约定同步更新
  - 本地工作目录已切换为 `JsirGalaxyBase`
  - GitHub 新仓库地址 `git@github.com:Jsir2022/JsirGalaxyBase.git` 当前尚不存在，远端切换需等待 GitHub 侧先完成仓库重命名

### 2026-03-29 - 确认终端入口与终端壳实施方案

- 主题：确定终端第一阶段采用快捷键入口加背包按钮入口，并先落稳定打开链与占位终端壳
- 影响范围：`docs/terminal-plan.md`、`README.md`、`docs/README.md`
- 原因：终端将承担职业、市场、福利和公共服务的统一入口，必须先把入口链、服务端鉴权与可替换 UI 壳分离清楚
- 结果：
  - 确认终端第一阶段先实现快捷键打开与背包按钮打开
  - 确认两条入口共用同一条服务端打开链
  - 确认 Pad 物品入口延后到后续阶段
  - 确认当前先使用占位终端壳，后续再替换为 `ModularUI 2`

### 2026-03-29 - 终端首页切换到 ModularUI 2

- 主题：移除旧占位 GUI，改为真实的 `ModularUI 2` 终端首页壳
- 影响范围：`dependencies.gradle`、`src/main/java/com/jsirgalaxybase/modules/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/`、`src/main/java/com/jsirgalaxybase/terminal/ui/`
- 原因：当前入口链已经稳定，下一步需要把终端正式切到 `ModularUI 2`，为职业、贡献度、声望、公共任务和市场摘要首页建立可扩展的同步面板
- 结果：
  - 新增 `ModularUI2` 依赖并注册终端 UI 工厂
  - 服务端打开链改为 `GuiManager.open(...)`，继续保持服务端权威
  - 删除旧 `IGuiHandler` 和占位容器 / 占位界面实现
  - 新增终端首页快照与只读首页面板，接入职业、贡献度、声望、公共任务、市场摘要五项展示

### 2026-03-29 - 终端补左侧导航与正式分页壳

- 主题：把终端从单页总览扩成左侧导航加右侧内容区的正式框架
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/`、`docs/WORKLOG.md`
- 原因：后续职业、公共事务、市场等页面需要统一壳，不能继续停留在单页演示态
- 结果：
  - 终端改为左侧导航与右侧分页内容结构
  - 首页继续保留制度摘要，同时新增职业、公共、市场三页的只读占位内容
  - 分页状态已接入 `IntSyncValue`，后续可在同一终端壳内继续扩页而不改入口链

### 2026-03-29 - 终端改为宽屏控制台风格

- 主题：参考 AE2 终端比例，重做终端观感与窗口尺寸

### 2026-03-30 - 整理 Ubuntu 24 向日葵安装复用资料

- 主题：补充 Ubuntu 24 下向日葵 15.2.0.63064 的安装记录、复用脚本与依赖包整理目录
- 影响范围：`../../Docs/sunlogin-ubuntu24/`、`docs/WORKLOG.md`
- 原因：本次实际排查出 Ubuntu 24 官方仓库缺失旧版 `libgconf-2-4` 依赖，且 Wayland 会导致向日葵被控黑屏，需要把安装包、步骤和 Xorg 配置整理成可复用资料，便于后续其他机器快速落地
- 结果：
  - 新增 `Docs/sunlogin-ubuntu24/README.md` 记录完整安装与黑屏修复流程
  - 新增 `Docs/sunlogin-ubuntu24/install_sunlogin_ubuntu24.sh` 作为复用脚本
  - 将新版安装包与所需旧依赖 `.deb` 统一整理到 `Docs/sunlogin-ubuntu24/packages/`
  - 记录 GDM 关闭 Wayland、改走 Xorg 的关键配置点
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：上一版窗口过小、信息层级过弱、默认按钮味太重，不适合作为长期制度终端壳
- 结果：
  - 终端窗口显著放大，改成更接近控制台的宽屏比例
  - 新增顶部状态带、侧栏导航、首页摘要卡片和分区内容卡
  - 视觉风格从默认灰底面板改为更偏控制台的深色块面布局

### 2026-03-29 - 终端切到居中加分辨率自适应布局

- 主题：让终端窗口和主内容区按屏幕比例自动缩放并保持居中
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：终端不能只针对当前开发分辨率，必须在不同屏幕尺寸下保持可读和稳定布局
- 结果：
  - 外层面板改为相对屏幕宽高的居中布局
  - 内层主容器、导航列和内容列切到相对宽高分配
  - 页面页脚文案同步更新为自适应布局状态

### 2026-03-29 - 终端窗口比例提升到接近全屏

- 主题：把终端窗口提高到接近屏幕 90% 的使用面积
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：当前终端已经可用，但仍然偏保守，用户希望更接近全屏控制台体验
- 结果：
  - 终端外层窗口改为宽高各占屏幕约 90%
  - 保留居中和相对布局逻辑，不回退到固定像素窗口

### 2026-03-29 - 终端切换到深蓝灰控制台主题

- 主题：参考现代控制面板网页的配色和细边线分区，重做终端首页观感
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：原版 Minecraft 容器风格不适合制度终端，上一版纯色卡片也不够像真正的控制台页面
- 结果：
  - 终端整体配色改为深蓝灰主背景加蓝色高亮
  - 页面结构改成细边线包裹的分区面板，而不是大块卡片堆叠
  - 首页新增路由矩阵、联机概览、公共队列和市场监控四类控制台区块

### 2026-03-29 - 终端压缩左栏并关闭调试噪声

- 主题：缩小左侧导航、把标题并入导航头，并清理开发环境中无意义的启动告警
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`、`run/client/config/`
- 原因：当前样式已定稿，但左栏和顶部标题带占用过多空间，右侧内容区过挤，同时 `ModularUI 2` 调试模式和 Forge 版本检查噪声影响最终体验
- 结果：
  - 标题并入左侧导航头，顶部独立标题带移除
  - 左侧导航列缩窄，右侧内容区获得更多宽高空间
  - 首页和详情页区块高度、表头和文案密度下调，减少挤压与遮挡
  - 关闭 `ModularUI 2` 的测试 GUI 与调试描边，补齐 `NEI` 缺失的 `untranslator.cfg`
  - 关闭开发客户端中的 Forge 版本检查，减少启动时无关异常输出
  - 为开发环境补齐 `minecraft` 与 `gtnhlib` 缺失的 `items` 纹理别名，消除启动时的缺失贴图告警
  - 补齐 `IGuiHolder#createScreen(...)` 并回调若干紧凑行高，消除 `ModularUI 2` 的屏幕创建与纵向 padding 警告
  - 参考 AE2 终端的上下分区布局，把左侧导航改成顶部固定、底部固定、中间弹性伸缩的自适应结构，避免窗口高度变化时溢出边框
  - 重新压缩右侧内容区的长文本和中部比例，避免中文长句在窄栏内被挤成纵向显示
  - 进一步收窄左侧导航为固定窄比例，并移除矩阵左侧辅助说明列，把联机概览缩成纯图例卡，解决中部两块的剩余挤压
  - 把首页高度分配改为固定头部、弹性中段、固定底部，并缩减矩阵行数和右侧概览内容，保证内容不再越出终端下边

### 2026-03-29 - 修正 WorldEdit 服务端分发形态

- 主题：修正 GTNH 服务端侧的 WorldEdit 分发，补齐 CUIFe，并保留 dist 包替代旧 core 的方案
- 影响范围：`GroupServer/packwiz/sync_root/mods/`、`GroupServer/GTNHServerConfig/server_mods/mods/`、`GroupServer/Galaxy_GTNH284_S1/mods/`、`GroupServer/Galaxy_GTNH284_S2/mods/`、`GroupServer/Galaxy_GTNH_Lobby/mods/`
- 原因：复核后确认 `worldedit-forge-mc1.7.10-6.1.1-dist.jar` 已内置 core 类，不应再额外叠加旧 `worldedit-core`；但 `WorldEditCuiFe` 需要同步补到服务端侧，之前这一部分漏放了
- 结果：
  - 保持 `worldedit-forge-mc1.7.10-6.1.1-dist.jar` 作为 WorldEdit 服务端实现
  - 将 `WorldEditCuiFe-v1.0.7` 补入 `GTNHServerConfig` 服务端私货目录和 S1/S2/Lobby live 实例
  - 未执行 live 服重启，当前仍为落盘待生效状态

### 2026-03-29 - 调整 GTNH 默认同步包的跨维传送与黑暗模组

- 主题：在 `packwiz` 同步源中放开 ServerUtilities 跨维传送，并移除 `Darkerer`
- 影响范围：`GroupServer/packwiz/sync_root/serverutilities/server/ranks.txt`、`GroupServer/packwiz/sync_root/mods/`、`GroupServer/packwiz/sync_root/config/`
- 原因：改善玩家跨维 `/home` 和 `/warp` 体验，并移除 GTNH 包内的真实黑暗实现，后续由 `packwiz` 同步到客户端与服务端
- 结果：
  - 为 `player`、`vip`、`admin` 全部开启 `serverutilities.homes.cross_dim` 与 `serverutilities.warps.cross_dim`
  - 从默认同步源删除 `darkerer-1.0.6.jar`
  - 从默认同步源删除 `config/darkerer.cfg`

### 2026-03-29 - 补齐私货 WorldEdit 分发与 live 服跨维传送配置

- 主题：整理根目录私货 jar，准备替换 WorldEdit 为 dist 包，并把 live 实例的 ServerUtilities 跨维权限直接改到位
- 影响范围：`GroupServer/packwiz/sync_root/mods/`、`GroupServer/GTNHServerConfig/`、`GroupServer/Galaxy_GTNH284_S1/`、`GroupServer/Galaxy_GTNH284_S2/`、`GroupServer/Galaxy_GTNH_Lobby/`
- 原因：在不重启 live 服的前提下，先把创世神相关模组文件、WorldEdit 配置镜像和 `/home` `/warp` 跨维权限准备完成
- 结果：
  - 计划用私货 `worldedit-forge-mc1.7.10-6.1.1-dist.jar` 替换旧 `worldedit-core + worldedit-forge` 组合
  - GTNHServerConfig 补齐 `config/worldedit/worldedit.properties` 镜像
  - GTNHServerConfig 与 S1/S2/Lobby 的 `serverutilities/server/ranks.txt` 全部开启跨维 `home/warp`

### 2026-03-30 - 明确群组服一期后端与同步边界

- 主题：把一期后端架构、同步范围和免费传送规则正式写入文档
- 影响范围：`README.md`、`../Docs/群组服.md`、`../Docs/技术边界文档.md`、`docs/WORKLOG.md`
- 原因：当前已明确一期不接 `Redis`，并且需要先把银行系统、主背包同步和免费跨服传送的边界定死，避免后续设计反复摇摆
- 结果：
  - 明确一期唯一中心化存储为 `PostgreSQL`
  - 明确一期采用模组服务端直连 `PostgreSQL`，不单独建设中心后端服务
  - 明确一期共享范围包括制度数据、主物品栏、护甲、经验、血量、饥饿
  - 明确“共享背包”当前等同于玩家按 `E` 打开的主背包数据
  - 明确跨服传送与 `home` 当前为免费规则
  - 明确一期实施顺序为：先银行系统，再落库与同步，再传送

### 2026-03-30 - 新增银行系统一期需求文档

- 主题：把银行系统一期能力、边界和非目标整理成正式需求文档
- 影响范围：`docs/banking-system-requirements.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：当前已经明确银行系统是一切制度与跨服能力的前置底座，需要先把需求边界固定，再进入具体表结构设计
- 结果：
  - 新增银行系统一期需求文档，明确初始玩家余额为 `0`
  - 明确玩家账户、税池、兑换所储备、后续公会资金都必须作为独立账户存在，不能退化为简单变量
  - 明确一期只做固定规则兑换结算，不做单独硬币交易市场与汇率系统
  - 明确一期不做任何扩展金融能力，如利息、贷款、定存等
  - 将银行系统需求文档加入仓库文档入口

### 2026-03-30 - 新增银行系统数据表与事务边界设计

- 主题：基于银行需求文档，落一期数据库表设计与事务边界草案
- 影响范围：`docs/banking-schema-design.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：银行系统需求边界已经固定，需要尽快把账户表、交易表、账本分录表和关键事务流程正式定稿，避免后续编码时重新发明模型
- 结果：
  - 新增银行系统数据表与事务边界设计文档
  - 明确一期核心表为 `bank_account`、`bank_transaction`、`ledger_entry` 与 `coin_exchange_record`
  - 明确所有资金变动必须在同一事务中完成锁定、校验、分录和余额更新
  - 明确幂等键、行级锁、固定加锁顺序和禁止修改历史账本的约束
  - 将数据表设计文档加入仓库文档入口

### 2026-03-30 - 新增银行 Java 领域模型与仓储接口草案

- 主题：把银行需求与表设计翻译成 Java 侧代码骨架
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/`、`docs/banking-java-domain-draft.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：仅有表设计还不够，必须尽快把领域模型、仓储接口和事务边界接口固定下来，避免后续业务实现直接散落 SQL 和字符串常量
- 结果：
  - 新增银行领域对象、关键枚举和仓储接口草案
  - 新增 `BankingTransactionRunner` 事务边界接口，用于表达银行写操作必须运行在同一事务中
  - 新增 Java 侧设计说明文档，建立文档设计与代码骨架之间的映射关系
  - 将 Java 侧设计文档加入仓库文档入口

### 2026-03-30 - 新增银行 PostgreSQL DDL 草案

- 主题：把一期银行表设计正式落成 PostgreSQL SQL 文件
- 影响范围：`docs/banking-postgresql-ddl.sql`、`docs/banking-schema-design.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：仅有 Markdown 表设计还不够，后续开始写 JDBC 仓储或迁移脚本前，需要一份可以直接对照执行和继续演进的 DDL 草案
- 结果：
  - 新增一期银行核心表的 PostgreSQL DDL 草案文件
  - 固定 `bank_account`、`bank_transaction`、`ledger_entry`、`coin_exchange_record` 与可选 `bank_daily_snapshot` 的字段、约束和索引
  - 明确账户 `updated_at` 触发器策略以及金额非负、幂等键唯一、账本顺序唯一等数据库侧约束
  - 将 SQL 草案加入设计文档与仓库文档入口

### 2026-03-30 - 开始落银行应用服务实现

- 主题：把银行一期业务动作从纯文档和接口草图推进到 application 层代码
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/application/`、`src/main/java/com/jsirgalaxybase/modules/core/banking/domain/CoinExchangeRecord.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/repository/LedgerEntryRepository.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：DDL 和领域模型已经齐备，下一步必须先把一期真正的业务动作编排固定下来，后续 JDBC 仓储才能按稳定签名实现
- 结果：
  - 新增 `BankingApplicationService`，统一承接开户、查询、玩家转账、内部划转和硬币兑换结算
  - 新增对应命令对象与 `BankPostingResult`，收束一期业务入参与出参
  - 补上基于 `request_id` 的幂等回放能力所需仓储查询签名
  - 将 `CoinExchangeRecord` 字段补齐到与 PostgreSQL DDL 更一致的状态

### 2026-03-30 - 补齐银行 JDBC 基础设施层

- 主题：把银行 application 层继续落到可对接 PostgreSQL 的 JDBC 边界
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：只有 application service 还不够，必须同步提供事务执行器与仓储实现骨架，后续才能真正接数据库和跑集成验证
- 结果：
  - 新增 JDBC 连接管理器与事务执行器，支持线程内事务连接复用
  - 新增账户、交易、账本分录、兑换记录的 JDBC 仓储实现
  - 将 `SELECT ... FOR UPDATE`、幂等查询、批量追加分录和账户余额更新明确落到代码层
  - 编译验证通过，当前只差 PostgreSQL 连接配置与模块装配

### 2026-03-30 - 接入银行配置项与模块初始化挂载点

- 主题：把银行 JDBC 基础设施从“仅可编译类库”推进到模块生命周期中的可装配状态
- 影响范围：`src/main/java/com/jsirgalaxybase/config/ModConfiguration.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：如果不把配置项和初始化入口接上，后续命令或 GUI 层仍然拿不到银行服务实例
- 结果：
  - 新增 PostgreSQL 银行连接配置项与 `source_server_id` 配置项
  - 新增 `BankingInfrastructure` 聚合对象与基于 `DriverManager` 的 `DataSource` 工厂
  - `InstitutionCoreModule` 已可在服务端按配置准备银行基础设施实例
  - 编译验证通过，当前剩余工作聚焦于 PostgreSQL 驱动依赖、真实连通验证和上层入口接线

### 2026-03-30 - 补充 PostgreSQL 本地安装与迁移说明

- 主题：补上 Ubuntu 宿主机 PostgreSQL 安装、初始化与换机迁移指导
- 影响范围：`docs/postgresql-local-setup-and-migration.md`、`docs/README.md`、`README.md`、`docs/WORKLOG.md`
- 原因：当前机器没有安装 PostgreSQL，且当前会话没有无密码 sudo，无法直接代装到宿主机；需要把安装与迁移流程沉淀为可执行说明
- 结果：
  - 新增 Ubuntu 24.04 下 PostgreSQL 安装与建库说明
  - 补充基于当前 DDL 的初始化命令
  - 明确换机迁移推荐走逻辑备份而不是直接拷贝数据目录
  - 补充最小备份命令，降低后续换主机时的数据丢失风险

### 2026-03-30 - 完成本机 PostgreSQL 安装与模组真实连通验证

- 主题：把 PostgreSQL 从文档方案推进到宿主机实际运行与模组服务端启动验证
- 影响范围：`dependencies.gradle`、`repositories.gradle`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankingInfrastructureFactory.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`docs/banking-java-domain-draft.md`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：银行 JDBC 实现已经存在，但如果没有真实数据库、真实驱动和真实服务端启动验证，就还不能说明这条链路可用
- 结果：
  - 在 Ubuntu 24.04 宿主机安装并启动 PostgreSQL 16
  - 创建本地开发账号 `jsirgalaxybase_app` 与数据库 `jsirgalaxybase`
  - 将一期银行 DDL 实际执行到本地数据库，确认核心表全部存在
  - 新增 PostgreSQL JDBC 驱动依赖与 Maven Central 仓库声明
  - 启动 `runServer` 完成模组服务端真实联调，日志明确显示银行 PostgreSQL 基础设施已准备并验证成功

### 2026-03-30 - 收紧本地数据库监听并接入银行管理员测试命令

- 主题：把本地 PostgreSQL 显式限制在回环地址，并提供游戏内银行管理测试入口
- 影响范围：`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/module/ModuleManager.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：当前数据库不应暴露外网监听，同时银行系统已经具备基础设施，需要第一个实际管理员入口来驱动开户、查余额、发钱和转账测试
- 结果：
  - PostgreSQL 已显式配置为只监听 `127.0.0.1:5432`
  - 本地开发业务账号密码已切换为用户指定的新密码
  - 在 `/jsirgalaxybase bank` 下新增 `open`、`balance`、`grant`、`transfer` 四个管理员测试子命令
  - 通过实际 `runServer` 自动控制台执行验证了 bank 命令帮助输出与命令注册链路

### 2026-03-31 - 放开 NEI 客户端主配置的本地持久化

- 主题：让 GTNH 客户端的 `NEI` 主配置不再被 `packwiz` 更新与 `Default Configs` 启动流程反复覆盖
- 影响范围：`GroupServer/packwiz/sync_root/packwiz-whitelist.json`、`GroupServer/packwiz/sync_root/config/localconfig.txt`、`GroupServer/packwiz/whitelist-localconfig-notes.md`、`docs/WORKLOG.md`
- 原因：玩家反馈 `NEI` 配置经常被自动替换；排查确认活跃文件 `config/NEI/client.cfg` 未进入白名单，同时 `localconfig.txt` 还在接管整份 `NEI/client.cfg`
- 结果：
  - 将 `config/NEI/client.cfg` 加入 `packwiz` 白名单
  - 保留 `config/NEI/client.cfg.bak` 白名单不变
  - 注释掉 `localconfig.txt` 中对 `[NEI/client.cfg]/*/*` 的接管规则
  - 在既有白名单说明文档中补记 `NEI` 案例，方便后续处理同类客户端配置问题

### 2026-03-30 - 扩展银行管理员命令到系统账户与最近流水查询

- 主题：把第一版银行测试命令从单纯改余额扩展到状态查询与账本查看
- 影响范围：`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：仅有开户、查余额、发钱、转账还不够，管理员需要直接查看最近流水和系统测试账户状态，才能形成最小可用的联调闭环
- 结果：
  - 在 `/jsirgalaxybase bank` 下新增 `ledger`、`system` 和 `system ledger` 命令
  - recent ledger 输出已包含交易号、方向、金额、变动前后余额和时间戳
  - system summary 会显示测试系统账户的编号、类型、状态和当前余额
  - 通过实际 `runServer` 自动执行验证了 system summary 与 system ledger 命令回显

### 2026-03-30 - 扩展银行管理员命令到公共账户、交易详情与系统账户初始化

- 主题：把银行管理命令从单账户测试扩展到公共账户与交易审计层面
- 影响范围：`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/repository/BankTransactionRepository.java`、`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankTransactionRepository.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：当前需要直接初始化系统账户集、查看公共账户状态，并能按交易号定位单笔交易详情，方便后续联调和审计
- 结果：
  - 新增 `/jsirgalaxybase bank public` 与 `/jsirgalaxybase bank public ledger` 命令用于查看受管公共/系统账户
  - 新增 `/jsirgalaxybase bank tx <transactionId>` 命令用于查询单笔交易与关联账本分录
  - 新增 `/jsirgalaxybase bank init system` 命令，用于初始化测试系统资金、系统运营账户、税池和兑换储备账户
  - 通过实际 `runServer` 自动执行验证了系统账户初始化、公共账户汇总与交易不存在时的详情查询回显

### 2026-03-30 - 收敛受管系统账户并补齐备份恢复脚本

- 主题：把系统账户模型收敛为 `ops + exchange`，并把 PostgreSQL 逻辑备份/恢复脚本正式落地
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/ManagedBankAccounts.java`、`src/main/java/com/jsirgalaxybase/modules/core/InstitutionCoreModule.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`scripts/db-backup.sh`、`scripts/db-restore.sh`、`docs/postgresql-backup-and-restore.md`、银行相关文档
- 原因：当前不再需要测试资金池与独立税池，系统运营收支应统一落在 `ops`；同时换机和演练必须从“会手敲命令”升级到“有固定脚本可执行”
- 结果：
  - 受管系统账户收敛为 `ops` 系统运营账户与 `exchange` 兑换储备账户
  - 玩家账户仍保持按需懒初始化，不做自动开户
  - `InstitutionCoreModule` 在服务端启动时自动确保系统账户存在
  - `/jsirgalaxybase bank system` 与 `grant` 改为围绕 `ops` 账户工作，公共账户查询只展示 `ops` 与 `exchange`
  - 新增 PostgreSQL 逻辑备份与恢复脚本，并补充使用方式、演练步骤与风险控制说明

### 2026-03-30 - 增补 systemd 定时备份方案并明确快照技术取舍

- 主题：把 PostgreSQL 备份方案从“手动脚本可用”推进到“systemd 每日自动备份可落地”
- 影响范围：`ops/systemd/`、`scripts/install-systemd-backup.sh`、`docs/postgresql-backup-and-restore.md`、`docs/README.md`
- 原因：当前实际需求已经明确为“单数据库每日一份、保留七份”，需要正式选主方案，并说明为什么不把文件系统快照当当前主链路
- 结果：
  - 新增 system 级 `systemd service + timer` 模板与环境文件样例
  - 新增安装脚本，用于把 unit 安装到 `/etc/systemd/system/` 并启用 timer
  - 明确当前主方案是 `pg_dump -Fc + systemd timer + RETAIN_COUNT=7`
  - 明确文件系统快照不是当前主方案，后续如需更细恢复点应升级到 `pg_basebackup + WAL archive`

### 2026-03-30 - 补齐 PostgreSQL 备份恢复值班手册与真实演练说明

- 主题：把备份恢复文档从“方案说明”补成“后续维护者可以直接照抄命令执行”的操作手册
- 影响范围：`docs/postgresql-backup-and-restore.md`、`docs/WORKLOG.md`
- 原因：当前备份与恢复链路已经真实安装和演练通过，但如果不把日常查看、手动备份、恢复到测试库、覆盖正式库和清理测试库等指令写清楚，后续维护者仍然会不知道怎么用
- 结果：
  - 文档补充了当前机器上的实际部署状态、备份目录与 systemd 单元名称
  - 文档补充了日常查看、立即备份、恢复到测试库、覆盖正式库和删除测试库的完整命令
  - 文档明确了 `oneshot` service 的状态表现与业务账号无建库权限这两个常见注意事项

### 2026-03-30 - 明确银行终端页的信息架构并开始接真实只读快照

- 主题：把普通玩家正式入口的银行 GUI 从“想法”落成文档，并开始按终端壳的嵌套菜单模式实现
- 影响范围：`docs/banking-terminal-gui-design.md`、`docs/README.md`、终端 GUI 与终端快照提供者
- 原因：当前终端已经是左侧导航 + 主区板块的统一壳，银行页不能再做成单独弹窗或纯目录页，而应先展示关键内容，再提供二级子页跳转
- 结果：
  - 文档固定了银行主页、个人账户、转账服务、Exchange 公开页、个人流水五页结构
  - 明确 Exchange 储备余额与最近流水属于玩家公开透明内容，而不是仅供管理员查看
  - 实施方向改为“先做真实只读快照与嵌套菜单，再继续接正式写操作”

### 2026-03-30 - 修正终端页签只换标题不换正文的 ModularUI 用法错误

- 主题：修复终端切到银行页后右侧正文不切换、只有标题变化的问题
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：原先正文区是在 `buildUI()` 时按当时的 `selectedPage` 一次性 `switch` 构建，后续虽然标题使用了动态文本，但正文没有进入框架的启用/禁用重布局链
- 结果：
  - 改为把所有页面挂进同一个正文容器
  - 每个页面容器使用 `setEnabledIf(...)` 绑定 `selectedPage`
  - 父级 `Flow` 开启 `collapseDisabledChild(true)`，让页签变化时正文区实际切换并重新布局

### 2026-03-31 - 修复终端打开即断线并记录两类联调阻塞根因

- 主题：收敛本地专用测试服最近两类核心阻塞：进服阶段的 `Fatally Missing blocks and items`，以及打开终端后的 `Disconnected from server`
- 影响范围：`src/main/java/com/jsirgalaxybase/GalaxyBase.java`、`src/main/java/com/jsirgalaxybase/terminal/network/TerminalNetwork.java`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：一方面 `ModularUI2` 的 dev 运行产物把测试映射带进了 Forge 注册表握手；另一方面 Forge 1.7.10 对自定义包通道名存在 20 字符上限，原终端通道名超长后会在服务端 `C17PacketCustomPayload` 解码阶段直接踢线
- 结果：
  - 在模组入口里忽略了 `modularui2:test_block` 与 `modularui2:test_item` 这类瞬时 dev 缺失映射，进服不再被这类测试映射阻塞
  - 把终端 `SimpleNetworkWrapper` 通道名从超长值收敛到 `jgb_terminal`
  - 确认这类终端断线修复后，客户端与服务端都必须重启，否则旧客户端仍会继续发送旧通道名
  - 把两类问题的根因与处理办法补进 PostgreSQL/联调文档，后续排障可以直接按文档核对

### 2026-03-31 - 明确银行终端“未启用基础设施”其实是服务端配置未打开

- 主题：把银行终端里的“未启用 PostgreSQL 基础设施”从模糊报错改成可操作的配置诊断
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankingService.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalBankSnapshotProvider.java`、`docs/postgresql-local-setup-and-migration.md`、`docs/WORKLOG.md`
- 原因：本地专用测试服的 `run/server/config/jsirgalaxybase-server.cfg` 当时仍是默认占位状态，`bankingPostgresEnabled=false`、JDBC 地址仍指向 `db-host`，终端只能统一回显“当前世界未启用 PostgreSQL 银行基础设施”，信息不足以指导维护者下一步该改哪里
- 结果：
  - 银行终端现在会优先区分“服务端显式关闭银行 PostgreSQL”“JDBC 地址未配置”“用户名/密码未填”“初始化失败”几类状态
  - 银行快照页会提示优先检查 `jsirgalaxybase-server.cfg`、JDBC 配置与服务端启动日志
  - 文档补充了本地 `runServer` 联调至少要打开 `bankingPostgresEnabled` 并填好 JDBC 凭据这一前置条件

### 2026-04-01 - 银行第三轮收口：补真实 JDBC 验证、内部划转语义测试与开户复用规则

- 主题：把银行模块当前最大缺口从“服务层逻辑”收敛到“真实 PostgreSQL 路径验证”和“剩余语义定稿”
- 影响范围：`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`docs/banking-java-domain-draft.md`、`docs/banking-terminal-gui-design.md`、`docs/WORKLOG.md`
- 原因：第二轮已经修完 `request_id` 幂等重放、历史余额回放与语义冲突校验，但真实 JDBC 路径验证、`postInternalTransfer` 同等级补测，以及 `openAccount` 命中已有账户时的资料处理规则仍未完全收口
- 结果：
  - 新增真实 PostgreSQL 集成测试，基于独立测试 schema 验证 `saveIfOwnerAbsent`、`saveIfRequestAbsent`、`request_id` 历史余额重放、语义冲突拒绝与事务回滚不半提交
  - 为 `postInternalTransfer` 补齐历史余额回放、`amount`、`fromAccountId`、`toAccountId`、`businessType`、`operatorType`、`operatorRef`、`sourceServerId` 冲突测试
  - 明确 `openAccount(...)` 命中已有账户时保持既有 `display_name` 与 `metadata_json` 不刷新
  - 文档明确当前终端只承担开户、只读快照和玩家转账，任务书硬币兑换真实入口延期到市场阶段

### 2026-04-01 - 银行第四阶段收口：补工厂初始化验证、deduct 管理闭环与独立测试入口

- 主题：把银行一期在“非市场阶段”剩余的初始化链路、管理命令和测试执行入口真正收住
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/JdbcBankingInfrastructureFactory.java`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`build.gradle.kts`、`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`docs/banking-java-domain-draft.md`、`docs/WORKLOG.md`
- 原因：第三轮后还剩三个明显尾巴没有收口：工厂初始化校验仍写死 `public` schema、`transactionType` 语义矩阵缺显式补测、管理员命令只有 `grant` 没有对称扣减入口，也缺独立的集成测试执行命令
- 结果：
  - `JdbcBankingInfrastructureFactory` 改为按当前 JDBC 连接的 `search_path/currentSchema` 校验必需表，独立 schema 的 PostgreSQL 集成测试终于能真实覆盖工厂初始化路径
  - 新增工厂初始化成功/缺表失败两条真实 PostgreSQL 集成测试，并补上 `transactionType` 冲突测试
  - 新增 `./gradlew bankingIt` 与 `./gradlew banking-it` 两个银行集成测试入口，便于单独跑 PostgreSQL 路径
  - `/jsirgalaxybase bank` 管理命令补上 `deduct <player> <amount> [comment]`，与既有 `grant` 形成对称的管理员修正闭环
  - 明确幂等重放返回的是“历史 availableBalance + 当前非余额字段”的结果对象，而不是完整历史账户快照

### 2026-04-01 - 市场阶段前置分析：收口市场与银行的职责边界

- 主题：在进入市场实施前，把制度文档中的市场需求与现有银行能力做一次正式对齐，避免后续把市场和银行混成一层
- 影响范围：`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
- 原因：银行一期已经完成非市场阶段收口，但市场真正开做之前，必须先明确哪些能力可以直接复用银行，哪些最小能力仍需银行补齐，以及哪些责任必须留在市场模块
- 结果：
  - 在 `../Docs/市场经济推进.md` 中新增“市场阶段与银行系统边界结论”章节
  - 明确市场可直接复用现有银行账户、账本、兑换结算与系统划转能力
  - 明确银行仍需补齐的最小能力是：`冻结资金/解冻资金`、`税池账户`、`市场结算业务类型`

### 2026-04-01 - 市场阶段第一轮：接入真实任务书硬币兑换入口并补银行最小缺口

- 主题：按“真实兑换入口 + 银行最小缺口 + 市场骨架”收下市场阶段第一轮
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/`、`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/main/java/com/jsirgalaxybase/command/GalaxyBaseCommand.java`、`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeSnapshotProvider.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/TaskCoinExchangePlannerTest.java`、`docs/banking-java-domain-draft.md`、`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
- 原因：制度上已经明确市场阶段不应重做第二套货币底层，但现有代码还缺玩家可调用的真实兑换入口，以及挂单市场前必需的冻结余额、税池账户和市场结算语义边界
- 结果：
  - 新增 `/jsirgalaxybase market quote hand` 与 `/jsirgalaxybase market exchange hand`，先落地“手持一叠任务书硬币”的真实兑换入口
  - 银行应用服务补齐 `冻结 / 解冻 / 从冻结余额结算` 三个最小动作，并新增市场相关交易类型、业务类型与 `tax` 受管公共账户
  - 新增 `modules/core/market/` 首轮骨架，把任务书硬币识别、兑换规则和银行结算桥接从 banking 中拆到 market 侧
  - 当前实现明确为 `source-blind`：仅按 Dreamcraft coin 物品注册名识别，不在本轮尝试从物品本体反推“一次性任务 / 循环悬赏”来源
  - 补了银行冻结生命周期单测与市场任务书硬币规则单测，完整挂单订单簿、托管库存、撮合、CLAIMABLE/EXCEPTION 与崩服恢复仍留待下一轮
  - 明确订单簿、托管库存、撮合、内部操作日志、异常恢复属于市场模块自身，不应继续塞回银行模块

### 2026-04-01 - 市场阶段第二阶段：收口冻结回放语义并切入标准化现货市场最小骨架

- 主题：先把第一层金融底座补到可承载市场，再谨慎切入第二层标准化现货市场
- 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/`、`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/modules/core/banking/application/BankingApplicationServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/BankingPostgresIntegrationTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/`、`docs/banking-postgresql-ddl.sql`、`docs/market-postgresql-ddl.sql`、`docs/banking-java-domain-draft.md`、`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
- 原因：第一轮已经补出冻结/解冻/从冻结结算动作，但 replay 语义仍然把 `available` 和 `frozen` 混成“半历史半当前”；同时第二层标准化市场还没有真正的订单 / 托管 / 操作日志结构，仍停在空接口阶段
- 结果：
  - `TaskCoinExchangePlanner` 补齐 `IV = 10000`，并避免把未知更高罗马后缀误判成 `I` 或 `BASE`
  - `ledger_entry` 扩展为同时保存 `available` 与 `frozen` 的 before/after，`freeze/release/settleFrozenTransfer` 的 replay 现在能一致返回历史余额态
  - 补齐银行服务层 replay 单测，以及 PostgreSQL 下的 freeze/release/settle 成功、重放、冲突与回滚回归
  - 新增标准化商品、市场订单、托管库存、内部操作日志、成交记录等领域对象与仓储接口，不再只是空接口占位
  - 新增标准化现货市场最小应用服务，先支持 `创建卖单`、`撤销卖单` 与 `查看 OPEN 卖单` 读模型骨架，且明确卖单托管与 CLAIMABLE 返还路径
  - 当前仍未进入买单冻结闭环、真实撮合、税收结算、GUI、跨服消息和第三层定制化交易

  ### 2026-04-01 - 市场阶段第三阶段：补请求语义与恢复闭环并打通最小买卖撮合

  - 主题：先修第二阶段一致性缺口，再把标准化现货市场推进到最小可运行买卖闭环
  - 影响范围：`src/main/java/com/jsirgalaxybase/modules/core/banking/infrastructure/jdbc/`、`src/main/java/com/jsirgalaxybase/modules/core/market/`、`src/test/java/com/jsirgalaxybase/modules/core/market/application/StandardizedSpotMarketServiceTest.java`、`src/test/java/com/jsirgalaxybase/modules/core/market/infrastructure/jdbc/MarketPostgresIntegrationTest.java`、`build.gradle.kts`、`docs/market-postgresql-ddl.sql`、`../Docs/市场经济推进.md`、`docs/WORKLOG.md`
  - 原因：第二阶段虽然已经有卖单骨架，但 `requestId` 还没有完整请求语义校验，失败路径也缺少最小恢复闭环，同时第三阶段要求的买单冻结、同步撮合、CLAIMABLE 生成和真实 JDBC 市场持久化仍未接通
  - 结果：
    - 标准化现货市场服务补齐 `BUY_ORDER_CREATE / BUY_ORDER_CANCEL / MATCH_EXECUTION`，并把买单冻结、撤单释放、同步单商品限价撮合、税池入账、CLAIMABLE 生成与成交记录写入收口到 market application service
    - `MarketOperationLog` 现在按 `requestSemanticsKey` 校验完整请求语义，重复 `requestId` 不再只校验操作类型；卖单创建与撤单失败时会保留相关 `order / custody / trade` 线索，并进入 `RECOVERY_REQUIRED` 或 `FAILED`
    - 新增 `MarketRecoveryService`，可以扫描 `CREATED / PROCESSING / FAILED / RECOVERY_REQUIRED` 并把关联订单与托管库存收口到 `EXCEPTION`
    - 补齐市场 JDBC 基础设施：真实 PostgreSQL 仓储、`JdbcMarketInfrastructureFactory`、`marketIt / market-it` 任务，以及 PostgreSQL 下的卖单创建/撤单、请求语义冲突和恢复扫描回归
    - 为了让 market JDBC 能复用 banking 的同一连接基础设施，把 `AbstractJdbcRepository` 与 `JdbcConnectionCallback` 开放为可跨包复用的公共类型，保持市场与银行共享一条事务链
    - 补了单元测试覆盖：卖单/撤单 request 语义冲突、买单冻结与撤单释放、同步撮合后的成交记录与 CLAIMABLE 资产、恢复扫描收口
    - 已实际执行并通过：`./gradlew test`、`./gradlew bankingIt`、`./gradlew marketIt`