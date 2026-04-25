# 终端 GUI 迁移 Phase 7 交接文档

**交接日期**：2026-04-25  
**目标执行者**：ChatGPT 5.5（或更先进的 AI）  
**当前仓库**：`/media/u24game/gtnh/JsirGalaxyBase`

---

## 一、项目背景与阶段定位

JsirGalaxyBase 是面向 GTNH 服务器的自定义制度模组。当前正在进行 **Terminal BetterQuesting 风格 UI 框架迁移**，目标是将终端从旧的 ModularUI 2 实现迁移到仓库自建的 BetterQuesting 风格 GUI 框架。

**当前阶段进度**：

| Phase | 内容 | 状态 |
|-------|------|------|
| Phase 1 | Framework 地基、主题骨架、占位 screen | ✅ 已完成 |
| Phase 2 | 打开链改造（服务端授权 + 客户端开屏） | ✅ 已完成 |
| Phase 3 | 首页壳（状态带、导航、通知/popup 宿主） | ✅ 已完成 |
| Phase 4 | Section 宿主、最小 action/snapshot 协议 | ✅ 已完成 |
| Phase 5 | 银行页完整迁移（第一张业务页） | ✅ 已完成 |
| Phase 6 | MARKET 总入口 + 标准商品市场迁移 | ✅ 已完成 |
| Phase 6 收口 | 滚动能力、布局密度、数据截断修复 | ✅ 已完成 |
| **Phase 7** | **定制商品市场 + 汇率市场迁移 + 旧装配残留收干** | 🔄 **待执行** |
| Phase 8 | 新终端壳正式 cutover | ⏳ 后续 |
| Phase 9 | 删除旧 terminal ModularUI 实现 | ⏳ 后续 |

---

## 二、Phase 7 核心目标

Phase 7 是迁移的**倒数第二个业务阶段**。本轮完成后，新终端壳必须已经承接 **BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE** 全部四类正式业务页。

### 必须完成的三件事

1. **把 MARKET_CUSTOM（定制商品市场）迁入新壳**
   - 必须是真实的 listing-first 业务页
   - 不能把旧 TerminalMarketPageBuilder 的 custom panel 直接嵌回新壳
   - 必须有独立 section model 与 snapshot

2. **把 MARKET_EXCHANGE（汇率市场）迁入新壳**
   - 必须是真实的 quote/标的优先业务页
   - 不能把旧 TerminalExchangeQuoteView 回嵌到新壳
   - 必须有独立 section model 与 snapshot

3. **收干新壳对旧 terminal 市场装配的直接依赖残留**
   - 新 TerminalHomeScreen 不再直接调用旧 TerminalMarketPageBuilder
   - 新 market section/popup 不再借壳使用旧 sync binder / old dialog / old session controller
   - 旧 terminal 代码可以继续存在，但不能再是新壳的必须依赖

### 关键约束（不允许打破）

- ❌ **不做**新 terminal 正式 cutover
- ❌ **不删除**旧 terminal ModularUI 实现（TerminalHomeGuiFactory、TerminalMarketPageBuilder 等）
- ❌ **不提前做** phase 8 和 phase 9
- ❌ **不允许**把 standardized、custom、exchange 混成“全市场统一超级模型”
- ❌ **不允许**重新引入 PanelSyncManager 语义

---

## 三、技术架构要点

### 3.1 新终端主链关键文件

执行 phase 7 前，必须先阅读以下核心实现文件：

```
src/main/java/com/jsirgalaxybase/terminal/client/screen/TerminalHomeScreen.java
src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalShellPanels.java
src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalSectionRouter.java
src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalPopupFactory.java
src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSection.java
src/main/java/com/jsirgalaxybase/terminal/client/component/TerminalMarketSectionState.java
src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalHomeScreenModel.java
src/main/java/com/jsirgalaxybase/terminal/client/viewmodel/TerminalMarketSectionModel.java
src/main/java/com/jsirgalaxybase/terminal/network/TerminalActionMessage.java
src/main/java/com/jsirgalaxybase/terminal/network/TerminalSnapshotMessage.java
src/main/java/com/jsirgalaxybase/terminal/TerminalService.java
src/main/java/com/jsirgalaxybase/terminal/TerminalActionType.java
src/main/java/com/jsirgalaxybase/terminal/ui/TerminalPage.java
```

### 3.2 旧终端市场实现（只读参考，不能回接）

```
src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketPageBuilder.java
src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketRoutePlan.java
src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSessionController.java
src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncBinder.java
src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSyncState.java
src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketSnapshot.java
src/main/java/com/jsirgalaxybase/terminal/ui/TerminalMarketService.java
src/main/java/com/jsirgalaxybase/terminal/ui/TerminalExchangeQuoteView.java
```

### 3.3 业务层边界

```
src/main/java/com/jsirgalaxybase/modules/core/market/**
```

### 3.4 核心协议

- **TerminalActionMessage**：客户端 → 服务端动作请求
- **TerminalSnapshotMessage**：服务端 → 客户端页面快照回写
- **TerminalPopupFactory + CanvasScreen popup 宿主**：确认弹窗生命周期

### 3.5 Phase 6 已建立的可复用能力

- **VerticalScrollPanel**：局部滚动容器
- **TerminalHomeScreen 尺寸策略**：按视口比例取值（PANEL_WIDTH_RATIO = 0.88F, PANEL_HEIGHT_RATIO = 0.86F）
- **TerminalMarketSection**：标准商品市场 section，已实现滚动与完整数据展示

Phase 7 的 custom / exchange 页面**必须直接复用**这些能力，不能重新走“固定裁断”老路。

---

## 四、必须阅读的文档

执行前必须完整阅读并遵守：

| 文档 | 作用 |
|------|------|
| `docs/README.md` | 项目文档索引，了解整体进度 |
| `docs/terminal-plan.md` | 终端入口与终端壳总方案 |
| `docs/terminal-betterquesting-ui-integration-plan-2026-04-14.md` | 集成方案，覆盖 vendoring、包结构、协议重构 |
| `docs/terminal-betterquesting-ui-phase5-bank-section-prompt-2026-04-18.md` | 银行页迁移参考（第一张完整业务页） |
| `docs/terminal-betterquesting-ui-phase6-market-overview-standardized-prompt-2026-04-19.md` | 标准商品市场迁移参考 |
| `docs/terminal-betterquesting-ui-phase6-close-prompt-2026-04-19.md` | Phase 6 收口 prompt（滚动、布局、数据截断） |
| **`docs/terminal-betterquesting-ui-phase7-custom-exchange-residue-prompt-2026-04-19.md`** | **本轮执行 prompt（核心依据）** |
| `docs/market-three-part-architecture.md` | 市场三分结构边界 |
| `docs/market-entry-overview.md` | MARKET 总入口落地说明 |
| `docs/custom-market-minimal-model.md` | 定制商品市场最小模型 |
| `docs/WORKLOG.md` | 开发工作记录，了解历史变更 |

---

## 五、Phase 7 执行 Prompt 核心摘要

详细内容见 `docs/terminal-betterquesting-ui-phase7-custom-exchange-residue-prompt-2026-04-19.md`，以下是执行要点：

### 5.1 必须完成的内容（共 9 项）

1. **把 MARKET_CUSTOM 升级成真实 CustomMarketSection**
   - listing-first 浏览层
   - listing 摘要/详情层
   - 玩家可操作动作区
   - 待领取/claim/个人挂单摘要
   - 最近动作反馈

2. **把 MARKET_EXCHANGE 升级成真实 ExchangeMarketSection**
   - 标的/对象浏览入口
   - quote/pair/rule/状态摘要
   - 刷新报价动作
   - 确认兑换动作
   - 最近动作反馈

3. **为 custom / exchange 引入独立 section model 与 snapshot**
   - 不能塞进 TerminalMarketSectionModel 硬扛
   - 三个市场模型边界必须清晰

4. **把 custom / exchange 动作接到 TerminalActionMessage 主链**
   - 刷新页面
   - custom：购买/取消/claim/下架 至少一条
   - exchange：刷新 quote/确认兑换 至少一条

5. **建立 snapshot 回写闭环**
   - action → service 处理 → snapshot 回写 → 原地刷新
   - 不接受只发 toast 或强制重开 screen

6. **迁移确认语义到新 popup 生命周期**
   - custom：购买确认/取消确认/claim 至少一条
   - exchange：确认兑换
   - 必须走 TerminalPopupFactory / CanvasScreen popup 宿主

7. **收干新壳对旧市场装配的直接依赖**
   - 不再调用旧 TerminalMarketPageBuilder
   - 不再借壳旧 sync binder / dialog / session controller

8. **统一滚动/布局/长列表策略**
   - 复用 phase 6 的 VerticalScrollPanel
   - 不再固定前 N 条裁断

9. **文档与 WORKLOG 同步**

### 5.2 测试要求

- custom action message 构造测试
- exchange action message 构造测试
- popup 主链测试（确认继续走 TerminalActionMessage）
- section 内容/长列表测试（不再被固定上限裁掉）
- 回归测试：TerminalServiceTest、TerminalMarketActionMessageFactoryTest、滚动与内容生成测试

### 5.3 本地联调要求

```bash
# 构建
cd /media/u24game/gtnh/JsirGalaxyBase && ./gradlew assemble

# 启动本地 dedicated server（后台）
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
/media/u24game/gtnh/JsirGalaxyBase/gradlew -p /media/u24game/gtnh/JsirGalaxyBase runServer

# 启动本地 client（后台）
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
/media/u24game/gtnh/JsirGalaxyBase/gradlew -p /media/u24game/gtnh/JsirGalaxyBase runClient --username=Developer
```

**验证点**：
- BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 都能打开
- custom / exchange 页面可滚动并完整浏览
- custom / exchange 关键确认 popup 正常
- 银行页和 standardized 页不被回归打坏

---

## 六、验收标准（必须同时满足）

Phase 7 完成后，必须同时满足以下四点：

1. ✅ 新壳已经承接 BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE **全部四类正式业务页**
2. ✅ custom / exchange 的关键动作与确认语义必须走 **新 action / snapshot / popup 主链**
3. ✅ 新壳**不再直接借壳**旧 market builder / binder / dialog / session controller
4. ✅ 本地 client/server 联调后，可以**直接进入 phase 8**，而不需要再补一轮业务页迁移

**如果这四点没有同时做到，就不算完成。**

---

## 七、已知风险与坑点

1. **不要把旧 UI 容器接回新壳**
   - 旧 TerminalMarketPageBuilder、TerminalExchangeQuoteView 等只能作为业务语义参考
   - UI 层必须基于当前 BetterQuesting 风格 framework 重建

2. **模型边界要清晰**
   - standardized、custom、exchange 必须分开建模
   - 不能搞“全市场统一超级模型”

3. **滚动能力必须复用 phase 6 的**
   - 不要重新写固定裁断逻辑
   - 直接用 VerticalScrollPanel

4. **本地联调必须实际跑**
   - 不能只靠单元测试通过就交付
   - 必须进游戏验证四个页面都能打开和滚动

---

## 八、交接清单

- [x] Phase 7 执行 prompt 已产出：`docs/terminal-betterquesting-ui-phase7-custom-exchange-residue-prompt-2026-04-19.md`
- [x] 文档索引已更新：`docs/README.md`
- [x] WORKLOG 已记录：`docs/WORKLOG.md`
- [x] 本交接文档已产出

**下一步**：请直接阅读 `docs/terminal-betterquesting-ui-phase7-custom-exchange-residue-prompt-2026-04-19.md` 并开始执行。

---

*交接者：GitHub Copilot (MiniMax M2.5)*  
*日期：2026-04-25*