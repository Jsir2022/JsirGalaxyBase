# Galaxy UI2 终端个性化与全页面迁移计划

日期：2026-09-06

## 目标

将 UI2 从首页与资产中心样板推进为完整终端应用。终端必须保持居中窗口形态，允许每位玩家在本机选择主题、
窗口尺寸和动效；所有正式页面最终使用同一主题、Minecraft 字体、文字布局、路由、弹窗与输入体系。

## 整改前实机结论

- 首页和资产中心是当前仅有的两个正式 UI2 页面，其余导航仍进入旧 Canvas `TerminalHomeScreen`。
- 首页截图中的主要故障不是窗口矩形失效，而是固定卡片高度小于三行文字所需高度，且 Label 没有换行、
  省略和裁剪合同。
- 旧 `terminalPanelWidthRatio` 等配置只作用于旧布局；UI2 此前固定使用 90% × 88%，因此不存在真正可用的
  玩家缩放设置。
- 资产中心的原生 Slot 必须保持 16 逻辑像素命中合同。终端不提供字号或界面密度调节；主题字号和控件密度固定，页面必须根据窗口约束自行换行、有限缩字、省略或滚动。

## 阶段状态

| 阶段 | 状态 | 内容 |
|---|---|---|
| P1 文字与布局正确性 | 自动验收完成 | 测量驱动换行、最大行数、省略、裁剪、主题字体角色、首页紧凑两列 |
| P2 外观系统 | 自动验收完成 | `TerminalPreferences`、三套主题、窗口档位、独立客户端配置、资源包入口 |
| P3 设置页面 | 自动验收完成 | 导航底部设置入口、即时应用、恢复默认、减少动画 |
| P4 轻量页面统一路由 | 自动验收完成 | 首页、设置、通知、银行、ServerTools、职业、公共与物品策略均进入同一 UI2 宿主 |
| P5 复杂业务页面 | 自动验收完成，待实机 | 三类市场、订单与交付及地产均已进入 UI2 |
| P6 旧 Canvas 删除 | 自动验收完成，待实机 | 地图后端已改用 UI2 几何；旧 Screen、Theme、Popup、Panel 与输入运行时已删除 |

## 主题包合同

内置主题为 `glass_mono`、`hacker_green`、`high_contrast`。主题是客户端偏好，不进入服务端 Snapshot、网络或
PostgreSQL。外部主题由当前启用的 Minecraft Resource Pack 在
`assets/jsirgalaxybase/ui2/themes/index.json` 注册，索引格式为：

```json
{
  "schemaVersion": 1,
  "themes": [
    {
      "id": "example_theme",
      "label": "示例主题",
      "base": "glass_mono",
      "resource": "ui2/themes/example_theme/theme.json"
    }
  ]
}
```

主题定义允许覆盖 `colors`、`spacing`、`radii`、`typography`、`controls`、`motion` 和 `elevation`。颜色使用
`#RRGGBB` 或 `#AARRGGBB`。首版禁止 Java、脚本、自定义 Shader、网络和业务行为；单个主题损坏时忽略该项，
整个索引不可用时回退内置主题。

## 缩放合同

- Minecraft GUI Scale：平台物理密度，UI2 不覆盖。
- 窗口档位：`COMPACT / STANDARD / LARGE`，只改变窗口占视口比例。
- 终端字号、行高、间距和控件高度由主题固定，玩家不能单独调节字号或界面密度。
- 每个文本框的语义字号是其默认上限：内容放得下时绝不放大；空间不足时在可读下限内逐级缩小。单行仍放不下则省略，多行优先在限定行数内换行，缩小后仍溢出才省略。
- 所有文字绘制受所属控件裁剪，所有正式终端 DrawList 再受终端窗口硬裁剪；业务页面估算错误也不得把内容画到窗口外。
- 原生 Slot、物品、鼠标命中和服务端 Container 坐标不做任意缩放；页面围绕固定槽位重新排布。

## 字体合同

- 正式 UI2 只使用客户端已经加载的 Minecraft `FontRenderer`，由 Minecraft 的语言系统和当前资源包决定实际字形。
- Base 不内置、不下载、不打包 Noto CJK 或其他替代字体，也不建立字形 Atlas、后台栅格线程和逐帧上传路径，因此不存在终端字体冷启动替换。
- UI2 测量、换行、缩字、省略和绘制共享当前 `FontRenderer`；更换资源包后由 Minecraft 的正常资源重载生效。
- 旧客户端配置中的 `density` 和 `fontSource` 均在读取时删除。

## 后续迁移顺序

1. `TerminalApplicationScreen` 已成为所有非 Container 正式页面的稳定宿主；路由切页不再创建旧首页。
2. 银行与 ServerTools 已使用 UI2 页面状态，同时复用原服务端消息合同。
3. 标准市场、订单/交付、定制市场和汇率市场已迁移；确认动作继续服务端复核。
4. 地产外壳、列表、检查器与 Modal 已迁移；真实地图通过 `ExternalSurface` 按 UI2 层级嵌入，地图后端不再继承旧 Panel，并保留既有地形缓存与输入合同。
5. CodeGraph 和源码守卫确认正式代码不再引用旧 Canvas；构建任务会拒绝重新打包旧 framework/theme 类。

每阶段必须覆盖 `350×193`、`427×240`、`620×340`，检查文字命令和交互区域不越过终端窗口；执行 UI2
Core/Lab/Demo、根项目全量测试、`assemble`、`verifyUi2SelfContained` 和 `git diff --check`。
