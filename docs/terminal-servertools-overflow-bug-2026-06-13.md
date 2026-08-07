# Terminal ServerTools Overflow Bug - 2026-06-13

## 状态

- 状态：已修复并部署到 `lobby,client`
- 影响页面：银河终端 `SERVER_TOOLS` / `传送` 页
- 影响范围：客户端终端 UI 布局，不涉及 warp 后端传送链路
- 修复文件：`src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalServerToolsSection.java`
- 部署 jar SHA256：`d1486cc48da02a260d6f75db00bc8a56a8e5f6c19582619007e494ff54d2b365`

## 用户可见现象

实际游戏截图中，终端顶部栏和左侧导航栏显示正常，但 `传送` 页主体区域存在明显越界：

- 中间 `可用传送点` 列表栏向下超过终端底部边界。
- 右侧详情栏的最近状态、反馈、警告和确认按钮被挤到终端外或被底部裁切。
- 右侧内容没有稳定的内部滚动，内容多时不是在终端内滚动，而是继续向下撑开。
- 在不同窗口尺寸和 GUI scale 下，问题表现不一致，但根因相同：子组件高度没有服从父容器高度。

用户指出的关键判断是正确的：导航栏本身不是问题，问题在服务器列表栏和右侧内容栏的最下方已经超出终端边界；同时不能删除上下滚动，因为列表和详情未来会继续增长。

## 期望行为

`传送` 页应保持类似目标效果图的三段结构：

- 左侧：常显导航栏。
- 中栏：传送点列表，在中栏内部滚动。
- 右栏：传送详情、最近状态、风险提示和确认按钮，在右栏内部滚动。

所有内容必须被限制在终端 body 内部。内容增长时应产生内部滚动，而不是改变终端外框尺寸，也不能把按钮或卡片顶出终端底边。

## 前因

本页在前几轮开发中从 generic section 迁移为专用 ServerTools 传送工作台。目标是接近 BetterQuesting 风格的三段式页面，并且保留现有 warp 后端调用链。

前几轮修复重点放在视觉层：

- 调整终端整体比例、顶部栏高度和边距。
- 压缩导航栏、内容栏间距和字体。
- 把页面从上下结构改成左右结构。
- 优化传送点列表和右侧状态卡片的视觉样式。

这些修改改善了外观，但没有优先验证布局合约：每个子组件是否严格落在父容器 bounds 内。结果是外观看起来接近目标图，但一旦实际 Minecraft GUI 给到的 section 高度不足，子组件仍然会突破终端边界。

## 多轮错误修复的原因

### 1. 把结构性越界误判为 spacing 问题

之前反复调整栏宽、间距、字号和卡片高度，但没有先确认 `TerminalServerToolsSection` 的实际布局高度是否超过了父容器。这导致修复方向偏向视觉微调，而不是 bounds 约束。

### 2. 外层 section 强制使用最小高度

问题代码使用了类似逻辑：

```java
int totalHeight = Math.max(SECTION_MIN_HEIGHT, bounds.getHeight());
```

当父容器实际高度小于 `SECTION_MIN_HEIGHT` 时，ServerTools section 仍按更大的高度布局。这样会直接违反父容器给出的 bounds，导致中栏和右栏必然向下溢出。

正确原则是：组件可以有推荐高度，但实际布局必须服从父容器给出的 `bounds.getHeight()`。不能在子组件内部用最小高度反向撑开父容器。

### 3. 右侧内容被错误改成直接堆叠

为了压缩页面结构，上一版把右侧详情块、最近状态、反馈、警告和按钮直接挂到 `workspaceCard` 上，并手动计算每个块的 y 坐标。这使右侧失去了 `VerticalScrollPanel` 的裁剪和滚轮能力。

内容高度一旦超过右栏可用高度，结果只能是继续向下排，最终挤出终端底边。

### 4. 中栏列表 viewport 也有越界风险

中栏列表高度曾使用固定下限计算，例如剩余空间不足时仍保证滚动区有较大最小高度。这样在卡片高度很小时，列表区域会和底部自动刷新提示互相挤压，甚至一起突破父容器。

### 5. 验收时没有优先检查布局不变量

前几轮主要对比“看起来是否更像效果图”，但没有把下面这些条件作为硬性验收：

- 子组件 bottom 必须小于等于父容器 bottom。
- 长列表必须在列表 viewport 内滚动。
- 长详情必须在详情 viewport 内滚动。
- 按钮不能依赖固定 y 坐标顶到底部外侧。
- 不同窗口尺寸和 GUI scale 下仍要保持父子 bounds 合法。

## 根因

根因是 `TerminalServerToolsSection` 同时违反了两个布局原则：

1. 子 section 使用固定最小高度覆盖了父容器真实高度。
2. 右侧内容没有放在可裁剪、可滚动的 viewport 内。

这不是单纯的视觉问题，也不是 Minecraft GUI scale 的单点问题，而是父子 bounds 合约被破坏。

## 修复方案

### 1. 外层布局只使用父容器真实高度

修复后，`setBounds` 不再用 `SECTION_MIN_HEIGHT` 撑开页面：

```java
int totalHeight = Math.max(1, bounds.getHeight());
```

这样中栏和右栏的高度都来自终端 body 实际提供的高度，不会超过父容器。

### 2. 恢复右侧内部滚动

右侧详情区重新引入 `workspaceScroll`：

- `detailBlockPanel`
- `recentBlockPanel`
- `feedbackPanel`
- `warningPanel`
- `confirmWarpButton`

这些内容全部通过 `workspaceScroll.addScrollableChild(...)` 加入同一个内部滚动 viewport。内容多时右侧内部滚动，终端外框不变。

### 3. 中栏列表继续使用独立滚动

中栏 `warpListScroll` 保留为独立 `VerticalScrollPanel`。列表高度按 `warpListCard` 当前真实剩余高度计算：

- 顶部标题占位固定。
- 底部自动刷新提示只在高度足够时显示。
- 列表 viewport 使用剩余高度，不再用会突破父容器的固定最小值。

### 4. 窄屏布局也避免负高度和越界

窄屏分支现在基于真实可用高度计算中栏和右栏高度，避免把其中一栏设置成超过父容器的固定高度。

## 修复结果

修复后，用户实测确认：

- 左侧导航栏正常。
- 中栏传送点列表被限制在终端边界内。
- 右侧详情栏被限制在终端边界内。
- 右侧内容区域恢复内部滚动。
- 底部不再出现按钮或内容块被挤出终端边界的问题。

## 验证记录

- `git diff --check`：通过。
- `./scripts/build-mod.sh --task assemble`：通过。
- 部署命令：

```bash
scripts/deploy-gray-chain.sh \
  --jar build/libs/jsirgalaxybase-ed7e2cf-main+ed7e2cfb16-dirty.jar \
  --targets lobby,client
```

- hash 校验：

```text
d1486cc48da02a260d6f75db00bc8a56a8e5f6c19582619007e494ff54d2b365
```

- Lobby 日志到达：

```text
Done (1.424s)! For help, type "help" or "?"
```

## 后续防回归要求

后续修改终端页面时，尤其是 `TerminalServerToolsSection`、市场页和银行页，需要先检查下面的硬规则：

1. 子组件不得用固定最小高度反向撑开父容器。
2. 页面内容增长时必须进入 `VerticalScrollPanel` 或等价 viewport。
3. 普通卡片可以固定高度，但整页工作区不能靠手动 y 坐标无限向下堆叠。
4. `setBounds` 中的高度应优先来自父容器真实 `bounds`，推荐高度只能用于滚动内容的 preferred height。
5. 对比效果图时，先确认布局边界合法，再评估视觉风格。

本次事故的直接经验：如果截图里某个栏位底部越过终端边框，优先查父子 bounds 和 scroll viewport，不要继续调 spacing。
