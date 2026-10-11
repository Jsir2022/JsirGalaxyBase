# Base-only 新服任务书初始化

本文只描述离线内容导入。它不读取 BQ 玩家进度，不迁移旧 Party，不部署 Mod，也不替操作者执行生产 DDL。

## 输入与前置检查

输入必须是只读 `DefaultQuests` 目录，至少包含 `Quests/`、`QuestLines/` 与 `QuestLinesOrder.txt`。先运行只读命令：

```bash
./gradlew :galaxy-quest-bq-importer:installDist
galaxy-quest-bq-importer/build/install/galaxy-quest-bq-importer/bin/galaxy-quest-bq-importer \
  plan /readonly/DefaultQuests
```

输出必须满足：`applicable=true`，没有 ERROR；实际出现类型全部为 `SUPPORTED`。当前固定 GTNH 来源应为 3,744 Quest、47 章，来源哈希 `9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc`。64 个未放置 Quest 和 4 个 seeded XOR 属已知 WARNING，不得被工具伪造成章节坐标。

## 数据库预览

数据库必须先由独立变更流程建立 Base Quest DDL。下面变量只可指向全新、明确获授权的目标库；开发验收使用临时隔离 PostgreSQL：

```bash
export BQ_MIGRATION_JDBC_URL='jdbc:postgresql://127.0.0.1:55436/quest_it'
export BQ_MIGRATION_DB_USER='quest_test'
export BQ_MIGRATION_DB_PASSWORD='...'

galaxy-quest-bq-importer/build/install/galaxy-quest-bq-importer/bin/galaxy-quest-bq-importer \
  dry-run /readonly/DefaultQuests
```

`dry-run` 不写数据库。`preview` 会登记预览批次，因此必须额外设置写入确认令牌：

```bash
export BQ_MIGRATION_WRITE_CONFIRM='ISOLATED_ONLY'
galaxy-quest-bq-importer/build/install/galaxy-quest-bq-importer/bin/galaxy-quest-bq-importer \
  preview /readonly/DefaultQuests
```

## 应用与回滚

确认 dry-run/preview 的来源哈希、CREATED/VERSIONED/UNCHANGED、ERROR/WARNING 后，才可在单独授权的变更窗口执行：

```bash
galaxy-quest-bq-importer/build/install/galaxy-quest-bq-importer/bin/galaxy-quest-bq-importer \
  apply /readonly/DefaultQuests
```

相同来源重复 apply 应返回同一批次且不新增版本。内容变化会生成新来源哈希，并只为变化实体发布新版。回滚只接受最新且没有后续依赖的批次：

```bash
galaxy-quest-bq-importer/build/install/galaxy-quest-bq-importer/bin/galaxy-quest-bq-importer \
  rollback <batch-id>
```

回滚内容批次不等于回滚玩家已经产生的进度或奖励；新服启用后，版本发布和玩家补偿属于管理员职责。本文不授权任何生产命令，实际生产初始化仍需单独审批。
