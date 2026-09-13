# Galaxy UI 2：Qz 成熟算法剪裁与自包含现代渲染内核

日期：2026-09-05

## 决策

Galaxy UI 2 继续保留自己的纯 Java 8 Core、单向数据流、组件树、DrawList、
Screen/Container 双宿主和 Java2D 测试后端。Base 不依赖、不调用也不打包
`qz_uilib` Mod；整合包现有 Qz-UILib 1.8.2 和 Qz-Miner不升级、不删除、不修改。

Qz 的价值是成熟的渲染和生命周期经验，不是整套 GUI 壳。实现按固定上游提交
审阅少量算法，重新命名并内化到 `com.jsirgalaxybase.client.ui2` 命名空间。
来源、许可证和实际派生文件以 `licenses/Qz-UILib/PROVENANCE.md` 为准。

## 代际取舍

- 1.8.2（MIT）：仅参考基础字体、Shader 和简单图元结构，兼容当前 GTNH
  整合包的事实，不将它当作现代架构基线。
- 4.1.3-LTS（LGPL-3.0-or-later）：用于稳定的 Clip、GL 边界、资源与
  Container 生命周期经验。
- 4.7.0（LGPL-3.0-or-later）：用于异步字形 generation、请求去重和帧预算上传。
- 4.8.0（LGPL-3.0-or-later）：用于圆角几何、宿主状态恢复、Scene 脏标记和
  上传失败回滚经验。

明确排除 Qz DOM/CSS、网络、配置、Mixin、HUD、完整控件库、Signal 状态系统
和 Mod 启动代码。UI2 不替换 Minecraft 全局字体，也不会建立第二套业务网络或容器协议。

## B.1：运行时与 Modal 稳定性

- `InputDispatcher` 用稳定 key 将重建后的焦点、Modal 与 Pointer Capture
  重新绑定；同一个 Modal 不再因 Runtime dirty frame 被反复打开。
- Modal 遮罩、Dialog 本体和关闭按钮分离命中；遮罩、关闭按钮与 Escape
  均只关闭一次，事件不穿透到页面或原生 Slot。
- `UiStore` 将 Effect 重入动作放入同一队列，监听者只观察一次最终稳定状态；
  超过 1000 个动作仍不收敛时 fail-fast，并输出动作诊断。

## B.2：现代图形与裁剪

`MinecraftUiRenderer` 只解释 DrawList，具体职责拆为 Shape、Text、Image、
ClipStack 和 GlStateGuard。圆角半径首先限幅，再按物理半径动态细分；Surface
使用内部实色路径和一物理像素 coverage fringe，Border 使用内外闭合路径
形成的填充环，不使用 `GL_LINE_LOOP`。

普通控件使用 2–4 逻辑像素圆角，大型 Surface 和 Dialog 使用 5–7；阴影限制
为少量透明外扩层，不引入 FBO 或全屏模糊。矩形裁剪先在 framebuffer 坐标求交，
空区域和窗口边缘饱和；UI2 退出时幂等恢复进入前的 scissor 和完整 GL 基线。
Stencil 圆角裁剪只在未来确有需求时加入，本批不为普通矩形付出额外状态成本。

## B.3：字体与响应式生命周期

本阶段最初实现过内置 Noto、后台字形准备与 Atlas 上传。2026-09-07 实机决策已将这条
路径完整移除：正式 UI2 只适配 Minecraft 当前 `FontRenderer`，让语言系统、资源包和
整合包负责字体。Qz 的异步字形 generation、上传队列和 Atlas 代码不再保留。

响应式状态仍保留一帧合并失效、帧末刷新到稳定点和循环上限；它与字体实现无关。

## B.4：UI Lab 与迁移门槛

F8 UI Lab 保留概览、控件、数据、浮层和库存五页，并显示 Minecraft 字体后端、
DrawCommand 数、Clip 深度、frame/render 时间和
reduced-motion。Java2D 与 Minecraft 继续使用同一 `UiLabDocument`。

人工验收必须检查：两档尺寸、圆角/边框、中文、确认框打开和三种关闭路径、浮层阻断、
库存原生 Slot 层级。通过后才进入正式资产中心和终端业务页迁移；旧 Canvas 本批不删除。

## 独立性与交付

构建门禁扫描生产源码和最终 JAR class：不得出现 `club.heiqi`、Qz Mixin、网络类或
Mod ID；JAR 必须包含来源清单、MIT/LGPL 和第三方声明。只部署 Base JAR 到客户端，
不启动客户端，不部署 Lobby、S1、S2。

## 实机验收后的渲染校正

首轮 F8 实机截图暴露了三项平台层偏差，现统一按物理像素和最终字体度量修正：

- 圆角 coverage band 以数学边界为中心向内外各扩半个物理像素；边框 hairline
  固定为一个物理像素，不再使用一个逻辑像素的下限。圆角环改用明确的三角形，避免
  `GL_QUADS` 在细分接缝处出现毛刺。
- UI2 槽位底板和 `NativeSlotBridge` 使用同一固定网格几何；Container 不再额外绘制
  方形槽位底板覆盖圆角。原生物品、矩形命中区和 Tooltip 语义不变。
- 原异步 Noto 与回退方案已被后续决策整体删除；Minecraft 字体从测量到绘制均为同步单一真源。
