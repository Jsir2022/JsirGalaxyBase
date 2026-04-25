# JsirGalaxyBase PostgreSQL Schema Migration Guide

日期：2026-04-03

## 当前原则

针对数据库结构变动，当前仓库采用下面这套规则：

- 用版本化 SQL migration 文件管理结构变更
- 由运维在停服窗口显式执行 migration
- 应用启动只做 fail-fast schema 校验，不在启动时偷偷改库
- 每个 migration 都记录到数据库内的 `schema_migration_history`
- migration 执行期间使用 PostgreSQL advisory lock 串行化，避免并发升级

这套做法和常见的数据库变更最佳实践一致：

- 结构变更要有编号和执行顺序
- 生产库升级要有显式运维动作和可审计记录
- 应用发现 schema 漂移时应该拒绝启动并提示升级入口
- 不依赖“应用跑起来时顺手修一下库”这种隐式行为

## 当前入口

检查状态：

```bash
scripts/db-migrate.sh --status
```

执行升级：

```bash
scripts/db-migrate.sh
```

默认连接目标：

- 主机：`127.0.0.1`
- 端口：`5432`
- 数据库：`jsirgalaxybase`
- 用户：`jsirgalaxybase_app`

如需修改，直接通过环境变量覆盖：

```bash
PGHOST=127.0.0.1 PGPORT=5432 PGDATABASE=jsirgalaxybase PGUSER=jsirgalaxybase_app scripts/db-migrate.sh
```

如需密码，继续通过 `PGPASSWORD` 提供。

## 迁移文件位置

版本化 migration 统一放在：

- `ops/sql/migrations/`

当前要求：

- 按文件名字典序执行
- 已发布 migration 文件应保持不可变
- migration SQL 本身应尽量做到可重复执行或至少幂等安全
- 新变更通过新增 migration 文件表达，而不是改历史文件去“重写过去”

## 应用启动行为

银行 JDBC 基础设施现在会：

- 校验核心表是否存在
- 校验核心列是否齐全
- 如果 schema 过旧或漂移，直接抛出带迁移指令的错误

也就是说，如果日志里看到类似：

- `Banking PostgreSQL schema is outdated or drifted ... Run scripts/db-migrate.sh ...`

正确处理方式是：

1. 停服
2. 执行 `scripts/db-migrate.sh`
3. 再启动服务端

而不是让应用继续带着半旧不旧的结构运行。