# JsirGalaxyBase 本地 PostgreSQL 安装与迁移说明

日期：2026-03-30

## 当前环境结论

当前这台机器已经确认：

- 系统是 Ubuntu 24.04
- `apt-get` 可用
- 已安装 `PostgreSQL 16.13`
- 已创建本地开发数据库 `jsirgalaxybase`
- 已完成当前一期银行 DDL 初始化
- 已显式收紧为只监听 `127.0.0.1:5432`

这意味着当前本机已经具备继续做 JDBC 与模组真实联调的基础环境。

当前本机数据库不对外开放端口，模组只允许通过本地回环地址连接。

## Ubuntu 24.04 推荐安装命令

建议安装服务端和客户端：

```bash
sudo apt-get update
sudo apt-get install -y postgresql postgresql-client
```

安装完成后，建议确认服务状态：

```bash
sudo systemctl status postgresql
```

如果服务未自动启动：

```bash
sudo systemctl enable --now postgresql
```

## 创建项目数据库与用户

建议不要直接用 `postgres` 超级用户跑项目，而是单独建库和账号。

先进入 postgres 账户：

```bash
sudo -u postgres psql
```

然后执行：

```sql
CREATE USER jsirgalaxybase_app WITH PASSWORD '请改成强密码';
CREATE DATABASE jsirgalaxybase OWNER jsirgalaxybase_app;
GRANT ALL PRIVILEGES ON DATABASE jsirgalaxybase TO jsirgalaxybase_app;
```

退出：

```sql
\q
```

## 用当前仓库 DDL 初始化表结构

项目的一期表结构草案已经在：

- [banking-postgresql-ddl.sql](banking-postgresql-ddl.sql)

初始化命令：

```bash
psql "postgresql://jsirgalaxybase_app:你的密码@127.0.0.1:5432/jsirgalaxybase" -f docs/banking-postgresql-ddl.sql
```

## 当前项目配置项

代码侧已经新增了 PostgreSQL 银行配置项，后续会从模组配置读取：

- `bankingPostgresEnabled`
- `bankingJdbcUrl`
- `bankingJdbcUsername`
- `bankingJdbcPassword`
- `bankingSourceServerId`

当前推荐值示例：

```text
bankingPostgresEnabled=true
bankingJdbcUrl=jdbc:postgresql://127.0.0.1:5432/jsirgalaxybase
bankingJdbcUsername=jsirgalaxybase_app
bankingJdbcPassword=请改成强密码
bankingSourceServerId=local-dev
```

## PostgreSQL 迁移到其他 Ubuntu 主机是否轻松

结论：轻松，而且是 PostgreSQL 的常规操作场景。

只要你不要把唯一数据只放在单机磁盘上、不做备份，迁移到另一台 Ubuntu 主机通常是非常稳的。

最推荐的是：

- 结构走版本化 DDL / migration
- 数据走逻辑备份
- 迁移前做一次最终增量停写或停服切换

对于你这个项目，一期最实用的方案是：

1. 新机器安装同主版本或更高兼容版本 PostgreSQL
2. 在新机器创建数据库和业务账号
3. 从旧机器导出逻辑备份
4. 在新机器恢复备份
5. 修改模组 JDBC 配置指向新主机
6. 验证账户余额、流水条数、关键公共账户是否一致

## 推荐迁移方式

### 方案 A：逻辑备份迁移

这是最适合当前项目的一期方案。

导出：

```bash
pg_dump -Fc -d "postgresql://jsirgalaxybase_app:你的密码@旧主机:5432/jsirgalaxybase" -f jsirgalaxybase.dump
```

恢复：

```bash
createdb -h 新主机 -U jsirgalaxybase_app jsirgalaxybase
pg_restore -h 新主机 -U jsirgalaxybase_app -d jsirgalaxybase --clean --if-exists jsirgalaxybase.dump
```

优点：

- 最稳妥
- 最适合跨机器迁移
- 不依赖直接复制数据目录
- 方便先在新机器试恢复再切换

### 方案 B：全实例逻辑备份

如果以后不止一个数据库，也可以导出整个实例：

```bash
pg_dumpall -f full-instance.sql
```

这适合小规模环境，但对单项目来说通常不如 `pg_dump -Fc` 灵活。

### 方案 C：物理迁移 / 数据目录复制

可以做，但不建议作为你当前的一期主方案。

原因：

- 必须非常注意 PostgreSQL 版本一致
- 必须保证停库状态或使用官方物理备份机制
- 比逻辑备份更容易在换机时踩细节坑

对你现在这个项目，逻辑备份已经够用了。

## 如何避免换机时数据丢失

关键不是“换不换主机”，而是“有没有稳定备份和恢复演练”。

当前最实用的防丢策略：

1. 每天自动执行一次 `pg_dump -Fc`
2. 备份文件同步到另一块磁盘或另一台机器
3. 每次大版本改库前，先额外做一次手动备份
4. 至少做一次真实恢复演练，确认备份不是坏的

一个很实用的习惯是：

- 结构文件放仓库
- 数据备份放仓库外
- 每次迁移先恢复到测试机验证，再切生产指向

## 对当前项目的建议

对 `JsirGalaxyBase` 当前阶段，建议按这个顺序推进：

1. 先把宿主机 PostgreSQL 装好
2. 让 DDL 成功跑起来
3. 补 PostgreSQL JDBC 驱动依赖
4. 做一次真实连通验证
5. 立刻加上最小备份脚本

## 最小备份命令示例

```bash
mkdir -p "$HOME/db-backups/jsirgalaxybase"
pg_dump -Fc -d "postgresql://jsirgalaxybase_app:你的密码@127.0.0.1:5432/jsirgalaxybase" -f "$HOME/db-backups/jsirgalaxybase/jsirgalaxybase-$(date +%F-%H%M%S).dump"
```

这个命令非常值得尽早固定下来。

当前仓库已经补上可直接复用的脚本：

- `scripts/db-backup.sh`
- `scripts/db-restore.sh`

以及更完整的说明文档：

- [postgresql-backup-and-restore.md](postgresql-backup-and-restore.md)

## 常见联调故障

### 终端银行页提示“当前世界未启用 PostgreSQL 银行基础设施”

这通常不是 GUI 自己坏了，而是独立服务端没有把银行基础设施真正初始化起来。

本地 `runServer` 联调至少要满足：

- `run/server/config/jsirgalaxybase-server.cfg` 中 `bankingPostgresEnabled=true`
- `bankingJdbcUrl` 指向真实本机库，例如 `jdbc:postgresql://127.0.0.1:5432/jsirgalaxybase`
- `bankingJdbcUsername` 与 `bankingJdbcPassword` 填入真实业务账号
- 表结构已经用 [banking-postgresql-ddl.sql](banking-postgresql-ddl.sql) 初始化完成

如果这些条件不满足，终端开户/转账会失败，银行页也会显示基础设施不可用。

### 打开终端后立刻 Disconnected from server

这是一次 Forge 1.7.10 自定义网络包约束踩坑：自定义通道名最大只能是 20 个字符。

本项目曾经把终端 `SimpleNetworkWrapper` 通道命名为 `jsirgalaxybase.terminal`，长度 23，结果客户端一发送开终端消息，服务端就在 `C17PacketCustomPayload` 解码时断开连接。

修复方式：

- 把终端网络通道名缩短到 20 字符以内
- 当前已改为 `jgb_terminal`
- 修复后必须同时重启服务端和客户端，否则旧客户端仍会继续发送旧通道名

### 进服时报 Fatally Missing blocks and items

这次本地联调里，根因不是 `JsirGalaxyBase` 新增了错误物品，而是 `ModularUI2` 的 dev 运行产物带入了测试映射：

- `modularui2:test_block`
- `modularui2:test_item`

修复方式分两层：

- 在 [src/main/java/com/jsirgalaxybase/GalaxyBase.java](../src/main/java/com/jsirgalaxybase/GalaxyBase.java) 里通过 `FMLMissingMappingsEvent#getAll()` 忽略这些瞬时 dev 映射
- 避免把 `ModularUI2 :dev` 当成运行时工件使用，降低再次把测试物品带进运行时的概率