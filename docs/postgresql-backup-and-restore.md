# JsirGalaxyBase PostgreSQL 备份与恢复脚本说明

日期：2026-03-30

## 核心认识

对当前这个项目，最稳妥的当前主方案不是直接碰 PostgreSQL 数据目录，也不是先上文件系统快照，而是：

- 结构靠仓库里的 DDL 和后续 migration 管
- 数据靠 `pg_dump` / `pg_restore` 做逻辑备份
- 恢复能力靠脚本固化和定期演练保证

原因很直接：

- 逻辑备份跨机器更稳
- 不强依赖完全一致的数据目录与文件系统环境
- 更适合你现在这种单项目、单库、模组直连 PostgreSQL 的形态

当前已经把这个方案继续收敛成：

- `systemd timer` 每天执行一次单库逻辑备份
- `pg_dump -Fc` 作为备份格式
- 默认保留最近 `7` 份，一天一份

当前脚本和 systemd unit 都是围绕这个原则写的。

## 当前机器上的实际部署状态

当前这台机器已经不是“只有脚本”，而是已经实际安装并启用了自动备份：

- 定时器：`jsirgalaxybase-db-backup.timer`
- 服务：`jsirgalaxybase-db-backup.service`
- systemd 环境文件：`/etc/jsirgalaxybase/db-backup.env`
- 备份目录：`/home/u24game/db-backups/jsirgalaxybase`

当前 systemd 行为是：

- 每天自动执行一次
- 默认保留最近 `7` 份
- 首份备份已经实际成功生成

这意味着后续维护者不用重新安装 PostgreSQL 备份体系，只需要会查看状态、手动触发、恢复和演练。

## 当前主方案结论

如果只在下面几个选项里选一个作为主方案：

1. `pg_dump` 逻辑备份
2. 文件系统快照
3. PostgreSQL 物理备份加 WAL 归档

那我当前推荐顺序是：

1. 先落 `pg_dump` 逻辑备份 + `systemd timer`
2. 后续如果需要更细的恢复点，再补 `pg_basebackup + WAL archive`
3. 文件系统快照只作为附加保护，不作为唯一备份来源

原因是：

- 你现在的目标是“每天一份，保留七份”，逻辑备份已经完全够用
- 逻辑备份跨机器最稳，恢复过程最可控
- 快照通常更适合同机快速回滚，不适合作为换机和长期保留的唯一手段
- 真要把恢复点压到“小时级甚至分钟级”，那也应该优先上 PostgreSQL 自己的物理备份链，而不是直接赌文件系统快照

## 当前脚本

仓库里现在提供：

- `scripts/db-backup.sh`
- `scripts/db-restore.sh`
- `scripts/install-systemd-backup.sh`
- `ops/systemd/jsirgalaxybase-db-backup.service`
- `ops/systemd/jsirgalaxybase-db-backup.timer`
- `ops/systemd/jsirgalaxybase-db-backup.env.example`

其中数据库脚本默认都面向本机回环地址上的：

- 主机：`127.0.0.1`
- 端口：`5432`
- 数据库：`jsirgalaxybase`
- 用户：`jsirgalaxybase_app`

而 `systemd` 方案会把这些变量放进：

- `/etc/jsirgalaxybase/db-backup.env`

由 service 读取，不把密码写进 unit 本身。

你仍然可以用环境变量覆盖手动执行时的参数。

## systemd 方案做什么

当前 timer 方案是：

- 每天 `04:15` 触发一次
- 增加最多 `30` 分钟随机延迟，避免所有定时任务同秒挤在一起
- `Persistent=true`，如果机器在触发点关机，开机后会补跑一次
- 实际执行的仍然是 `scripts/db-backup.sh`

也就是：

- 调度交给 `systemd`
- 备份逻辑交给脚本
- 保留策略交给 `RETAIN_COUNT=7`

这条链路简单，而且足够可维护。

## 为什么当前不把快照当主方案

你提到“或者有快照技术”，这个方向不是不能做，但我不建议把它当当前主链路。

### 文件系统快照的问题

如果你说的是：

- `btrfs snapshot`
- `zfs snapshot`
- `LVM snapshot`

那它们的主要问题是：

- 更偏向宿主机级回滚，不是数据库语义级恢复
- 对换机帮助不如逻辑备份直接
- 同机磁盘坏了，快照一起没
- 如果没有配套 PostgreSQL 物理备份规范，只做卷快照容易变成“看起来像备份，实际上恢复演练很差”

### PostgreSQL 物理快照/归档的适用时机

如果你后面要的是：

- 不止每天一份，而是更小恢复点目标
- 例如“回到今天 14:37 之前”

那应该上的是：

- `pg_basebackup`
- `archive_mode=on`
- `archive_command`
- WAL 归档
- PITR

这才是 PostgreSQL 正统的“快照/时间点恢复”路线。

但它的复杂度比你现在要求的“每天一份保留七份”高一截，所以我不建议现在先上它。

## 备份脚本做什么

`scripts/db-backup.sh` 的职责是：

1. 用 `pg_dump -Fc` 导出 PostgreSQL 自定义格式备份
2. 备份完成后生成 `sha256` 校验文件
3. 用 `pg_restore --list` 做一次归档可读性校验
4. 可选保留最近 `N` 份，自动清理更旧备份

为什么用 `-Fc`：

- 恢复时更灵活
- 可以用 `pg_restore` 做对象级控制
- 文件体积通常比纯 SQL 更友好

## 恢复脚本做什么

`scripts/db-restore.sh` 的职责是：

1. 选择一个备份文件，或自动取最新一份
2. 在恢复前先校验 `sha256`
3. 默认恢复到现有数据库
4. 可选 `--recreate-db` 先删库重建再恢复
5. 恢复完成后执行基础健康检查

恢复脚本不会偷偷停 Minecraft 服务，所以你在恢复前要自己先停服。

这是故意的，因为恢复是高风险操作，不应该默默替你动运行中的服。

## 默认环境变量

两个脚本共用这些环境变量：

- `PGHOST`，默认 `127.0.0.1`
- `PGPORT`，默认 `5432`
- `PGDATABASE`，默认 `jsirgalaxybase`
- `PGUSER`，默认 `jsirgalaxybase_app`
- `PGPASSWORD`，默认空，建议显式传入
- `BACKUP_DIR`，默认 `$HOME/db-backups/jsirgalaxybase`

建议密码不要直接写进脚本，而是临时通过环境变量传入。

## 典型用法

下面这些命令按“当前已经部署好的机器”来写，后续维护者可以直接照抄。

## 日常查看与手动触发

### 1. 查看定时器状态

```bash
sudo systemctl status --no-pager jsirgalaxybase-db-backup.timer
sudo systemctl list-timers --all | grep jsirgalaxybase-db-backup
```

### 2. 查看最近一次备份服务执行情况

```bash
sudo systemctl status --no-pager jsirgalaxybase-db-backup.service
```

### 3. 立即手动执行一次备份

```bash
sudo systemctl start jsirgalaxybase-db-backup.service
sudo systemctl status --no-pager jsirgalaxybase-db-backup.service
```

### 4. 查看备份目录中的文件

```bash
ls -lah /home/u24game/db-backups/jsirgalaxybase
```

正常情况下，每一份备份都应该成对出现：

- 一个 `.dump`
- 一个对应的 `.dump.sha256`

### 1. 手动做一份备份

```bash
cd /media/u24game/gtnh/JsirGalaxyBase
PGPASSWORD='你的密码' scripts/db-backup.sh
```

### 2. 保留最近 7 份备份

```bash
cd /media/u24game/gtnh/JsirGalaxyBase
PGPASSWORD='你的密码' RETAIN_COUNT=7 scripts/db-backup.sh
```

### 3. 恢复最新一份备份到现有数据库

恢复前先停掉模组服务端。

```bash
cd /media/u24game/gtnh/JsirGalaxyBase
PGPASSWORD='你的密码' scripts/db-restore.sh --latest
```

### 4. 删库重建后再恢复

这适合做“整库回滚”或迁移演练。

注意：

- 这条路径需要有创建/删除数据库的权限
- 当前脚本支持两种方式：
	- 提供 `PGADMINUSER` 与可选 `PGADMINPASSWORD`
	- 或者在本机通过 `sudo -u postgres` 执行数据库重建

```bash
cd /media/u24game/gtnh/JsirGalaxyBase
PGPASSWORD='你的密码' scripts/db-restore.sh --latest --recreate-db
```

### 5. 指定某一份备份恢复

```bash
cd /media/u24game/gtnh/JsirGalaxyBase
PGPASSWORD='你的密码' scripts/db-restore.sh "$HOME/db-backups/jsirgalaxybase/jsirgalaxybase-2026-03-30-143000.dump"
```

## 标准恢复流程

下面把“恢复数据库”拆成两个实际场景。

### 场景 A：恢复到临时测试库做演练

这是最推荐、也最安全的方式。

用途：

- 验证备份是否可恢复
- 核对账本和系统账户是否完整
- 不影响当前正式库

步骤：

1. 选择备份文件
2. 恢复到临时测试库
3. 核对核心表与系统账户
4. 验证完后删除临时测试库

命令示例：

```bash
cd /media/u24game/gtnh/JsirGalaxyBase
PGPASSWORD='你的密码' scripts/db-restore.sh /home/u24game/db-backups/jsirgalaxybase/你的备份文件.dump --recreate-db
```

如果要显式指定测试库名：

```bash
cd /media/u24game/gtnh/JsirGalaxyBase
PGPASSWORD='你的密码' PGDATABASE=jsirgalaxybase_restore_test scripts/db-restore.sh /home/u24game/db-backups/jsirgalaxybase/你的备份文件.dump --recreate-db
```

恢复后核对示例：

```bash
PGPASSWORD='你的密码' psql -h 127.0.0.1 -U jsirgalaxybase_app -d jsirgalaxybase_restore_test -Atc "select 'bank_account='||count(*) from bank_account union all select 'bank_transaction='||count(*) from bank_transaction union all select 'ledger_entry='||count(*) from ledger_entry union all select 'coin_exchange_record='||count(*) from coin_exchange_record union all select 'managed_accounts='||count(*) from bank_account where owner_ref in ('SYSTEM_OPERATIONS','EXCHANGE_RESERVE');"
```

删除测试库示例：

```bash
sudo -u postgres psql -d postgres -c 'DROP DATABASE IF EXISTS "jsirgalaxybase_restore_test";'
```

### 场景 B：覆盖正式库恢复

这是事故处理场景，只在你确认正式库需要回滚时使用。

用途：

- 正式回滚数据库到某份备份状态

步骤：

1. 先停 Minecraft 服务端
2. 先确认选中的备份文件没问题
3. 先尽量恢复到测试库再看一遍
4. 再对正式库执行恢复
5. 恢复后核对系统账户、表行数和模组启动期数据库校验
6. 最后再重启服务端

命令示例：

```bash
cd /media/u24game/gtnh/JsirGalaxyBase
PGPASSWORD='你的密码' scripts/db-restore.sh /home/u24game/db-backups/jsirgalaxybase/你的备份文件.dump
```

如果确认需要先删库重建再恢复：

```bash
cd /media/u24game/gtnh/JsirGalaxyBase
PGPASSWORD='你的密码' scripts/db-restore.sh /home/u24game/db-backups/jsirgalaxybase/你的备份文件.dump --recreate-db
```

### 恢复后的最小验收命令

```bash
PGPASSWORD='你的密码' psql -h 127.0.0.1 -U jsirgalaxybase_app -d jsirgalaxybase -Atc "select 'bank_account='||count(*) from bank_account union all select 'bank_transaction='||count(*) from bank_transaction union all select 'ledger_entry='||count(*) from ledger_entry union all select 'coin_exchange_record='||count(*) from coin_exchange_record union all select 'managed_accounts='||count(*) from bank_account where owner_ref in ('SYSTEM_OPERATIONS','EXCHANGE_RESERVE');"
```

然后再看模组日志里 JDBC 启动校验是否正常。

## 给后续维护者的最短指令清单

如果后人只想要“最短可用命令”，看这一段就够了。

### 立即备份一次

```bash
sudo systemctl start jsirgalaxybase-db-backup.service
```

### 查看下一次自动备份时间

```bash
sudo systemctl list-timers --all | grep jsirgalaxybase-db-backup
```

### 查看已有备份文件

```bash
ls -lah /home/u24game/db-backups/jsirgalaxybase
```

### 恢复到测试库演练

```bash
cd /media/u24game/gtnh/JsirGalaxyBase
PGPASSWORD='你的密码' PGDATABASE=jsirgalaxybase_restore_test scripts/db-restore.sh /home/u24game/db-backups/jsirgalaxybase/你的备份文件.dump --recreate-db
```

### 删除测试库

```bash
sudo -u postgres psql -d postgres -c 'DROP DATABASE IF EXISTS "jsirgalaxybase_restore_test";'
```

## 我对备份脚本的要求

一个可用的当前备份方案，至少要满足下面几点：

- 默认参数安全，别一上来就删库
- 恢复前做校验，别把坏包当好包
- 输出明确，知道它到底备份了哪一份、恢复了哪一份
- 和仓库结构解耦，备份文件默认落仓库外
- 能直接被 `systemd timer` 调用

当前这套脚本加 unit 文件就是按这个标准写的。

## 我不推荐的做法

当前阶段不推荐把主方案放在：

- 直接复制 PostgreSQL 数据目录
- 只依赖文件系统快照
- 只保留 SQL 文档，不做真实备份文件
- 有备份但从来不恢复演练

原因分别是：

- 物理拷贝更吃 PostgreSQL 版本、停库时机和宿主环境细节
- 快照对宿主环境绑定更深，而且不能替代单独的异机可恢复备份
- 只有 DDL 没有数据备份，出了事恢复不了余额和流水
- 不演练就不知道脚本、账号权限、校验链路是否真能跑通

## 推荐安装方式

当前我建议直接使用 system 级 `systemd timer`，而不是 `cron`。

原因：

- 有明确的 service / timer 状态可查
- `Persistent=true` 可以补跑遗漏任务
- 更适合后续再加告警、资源限制和依赖关系

安装命令建议：

```bash
cd /media/u24game/gtnh/JsirGalaxyBase
sudo BACKUP_RUN_AS_USER=u24game scripts/install-systemd-backup.sh
```

安装后要做两件事：

1. 编辑 `/etc/jsirgalaxybase/db-backup.env`，填入真实 `PGPASSWORD`
2. 手动跑一次 service，确认首份备份成功

手动触发方式：

```bash
sudo systemctl start jsirgalaxybase-db-backup.service
sudo systemctl status --no-pager jsirgalaxybase-db-backup.service
sudo systemctl list-timers --all | grep jsirgalaxybase-db-backup
```

## 当前已完成的真实演练

当前已经实际完成过一次“备份文件恢复到临时测试库”的演练，结论是：

- 最新 `.dump` 可以成功通过 `sha256` 校验
- 可以成功恢复到临时测试库 `jsirgalaxybase_restore_test`
- 恢复后核心表计数与线上基线一致
- `ops` 与 `exchange` 两个系统账户也都恢复成功

本次演练也顺手修正了恢复脚本里两类真实问题：

- `--recreate-db` 路径原先的 psql 变量替换写法不稳
- 业务账号本身没有建库权限，所以脚本改为支持管理员连接或本机 `sudo -u postgres`

## 推荐操作节奏

对你现在这套本地开发和后续换机场景，我建议：

1. 每天自动备份一次
2. 每次改 DDL 或大规模联调前再手动备份一次
3. 每周至少做一次“恢复到测试库”的演练
4. 真迁移主机时，先恢复到新机测试，再切 JDBC 指向

## 常见注意事项

### 1. 看到 service 是 inactive (dead) 不代表失败

这个 service 是 `oneshot`，跑完退出是正常表现。

关键看的是：

- `ExecStart` 是否 `status=0/SUCCESS`
- 备份目录里是否出现新的 `.dump` 和 `.sha256`

### 2. 业务账号默认没有建库权限

当前 `jsirgalaxybase_app` 默认不是 `CREATEDB` 角色。

所以：

- 日常连接数据库没问题
- 但 `--recreate-db` 需要走管理员连接或 `sudo -u postgres`

### 3. 恢复正式库前必须先停服

因为恢复脚本不会替你停 Minecraft 服务端。

这条规则不要省略。

## 恢复后的最小核验

恢复完不要只看脚本退出码，至少再核验下面几件事：

1. `bank_account` 行数是否符合预期
2. `bank_transaction` 和 `ledger_entry` 行数是否符合预期
3. `ops` 和 `exchange` 两个系统账户是否存在
4. 随机抽几笔交易，看账本前后余额是否连得上
5. 模组服务端重新启动后，JDBC 启动校验是否通过

## 后续可以继续增强的点

如果后面你想把运维再往前推一步，下一批增强建议是：

1. 再加一个“恢复到临时测试库”的演练脚本
2. 再加一个备份后同步到第二块磁盘或另一台机器的步骤
3. 如果恢复点目标不再满足于“天级”，再升级到 `pg_basebackup + WAL archive`