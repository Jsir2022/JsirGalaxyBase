# JsirGalaxyBase Docs

这里放的是 `JsirGalaxyBase` 的基础说明和持续记录，方便玩家、协作者和后续维护者快速理解这个项目。

## 当前最建议先看的内容

- `../README.md`
  - 项目定位、代码结构和开发原则
- `WORKLOG.md`
  - 开发工作记录
- `terminal-plan.md`
  - 终端入口与终端壳的实施方案
- `banking-system-requirements.md`
  - 银行系统一期需求与边界
- `banking-schema-design.md`
  - 银行系统一期数据表与事务边界设计
- `banking-postgresql-ddl.sql`
  - 银行系统一期 PostgreSQL DDL 草案
- `market-postgresql-ddl.sql`
  - 市场系统第二阶段最小骨架 DDL 草案
- `banking-java-domain-draft.md`
  - 银行系统 Java 领域模型与仓储接口草案
- `banking-terminal-gui-design.md`
  - 银行终端页面信息架构与嵌套菜单设计
- `postgresql-local-setup-and-migration.md`
  - Ubuntu 本地 PostgreSQL 安装、初始化与迁移说明
- `postgresql-backup-and-restore.md`
  - PostgreSQL 逻辑备份、systemd 定时备份与恢复演练说明

## 这个项目现在是什么

`JsirGalaxyBase` 当前是一个面向 GTNH 服务器的自定义制度模组。

它的核心目标包括：

- 职业
- 市场与经济
- 贡献度 / 声望
- 公共订单 / 公共工程
- 群组服同步核心状态
- 后续玩法能力扩展

## 这个项目现在不是什么

- 不是普通客户端整合包说明文档
- 不是 Bukkit 插件项目
- 不是多仓库拆分项目
- 不是已经完成的正式玩法发布包

当前更准确的理解是：

- 一个还在持续设计和落地中的 GTNH 自定义模组仓库

## 文档说明

- 面向开发和协作的正式入口以 `../README.md` 为准
- 持续变更记录以 `WORKLOG.md` 为准
- 服务器制度细节和更大范围的设计约束仍以工作区 `../Docs/` 下的文档为长期参考