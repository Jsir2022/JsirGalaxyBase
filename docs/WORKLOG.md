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

### 2026-03-29 - 终端左右栏切换为纵向滚动视口

- 主题：把终端左右两侧从硬性挤压改为可滚动浏览的固定视口
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：当前布局顺序已经可用，但内容增多后仍会触发下边越界与 `ModularUI` 子树重算失败，继续压缩高度只会让可读性变差
- 结果：
  - 左侧导航保持窄栏比例不变，但内部内容超出时改为上下滚动
  - 右侧标题带与页脚保持固定，中间页面主体改为纵向滚动区域
  - 首页和详情页内容容器改回按自然高度排布，为后续继续扩充模块、图标和图片预留空间

### 2026-03-29 - 终端滚动实现改为列表控件方案

- 主题：参考 VendingMachine 的页面结构，把终端滚动区从通用滚动壳改为 `ListWidget`
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：裸 `ScrollWidget` 版本虽然消除了灰块，但未形成可用的鼠标滚轮滚动行为；同仓库参考实现更常用 `ListWidget` 承接固定视口内的纵向内容
- 结果：
  - 左侧导航与右侧页面主体的滚动容器统一改为 `ListWidget`
  - 保留现有窄侧栏和固定标题/页脚布局，不回退主题和页面结构
  - 编译通过，开发客户端启动正常，后续只需要继续验证实际滚轮滚动手感

### 2026-03-29 - 右侧内容栏改为整列滚动

- 主题：把右侧标题带并入滚动内容，同时收紧滚动视口到实际 90% 终端窗口内
- 影响范围：`src/main/java/com/jsirgalaxybase/terminal/ui/TerminalHomeGuiFactory.java`、`docs/WORKLOG.md`
- 原因：当前右侧虽然已经能滚动，但标题固定在外层，且滚动区域底部仍与固定页脚竞争高度，导致可滚动内容下沿超出终端框体
- 结果：
  - 右侧标题、正文与页脚统一纳入单一滚动列
  - 右侧滚动视口只占内容栏实际可见高度，不再从底部顶出 90% 窗口框
  - 编译通过，开发客户端启动正常，可继续在游戏内验证实际滚动边界

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