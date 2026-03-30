# JsirGalaxyBase 银行系统 Java 领域模型与仓储接口草案

日期：2026-03-30

## 目标

这份文档把 [banking-system-requirements.md](banking-system-requirements.md) 和 [banking-schema-design.md](banking-schema-design.md) 翻译成 Java 侧的一期代码骨架。

当前目标不是直接实现 JDBC，而是先把：

- 领域对象
- 关键枚举
- 仓储接口
- 事务边界接口

先固定下来，避免后续业务实现一边写 SQL 一边倒逼模型返工。

## 当前包结构

一期建议采用下面这套包结构：

- `com.jsirgalaxybase.modules.core.banking.domain`
  - 银行领域对象与枚举
- `com.jsirgalaxybase.modules.core.banking.repository`
  - 仓储接口与事务接口

当前已经新增的核心草案对象包括：

- `BankAccount`
- `BankTransaction`
- `LedgerEntry`
- `CoinExchangeRecord`

当前已经开始落地的应用层对象包括：

- `BankingApplicationService`
- `BankPostingResult`
- `OpenAccountCommand`
- `PlayerTransferCommand`
- `InternalTransferCommand`
- `CoinExchangeSettlementCommand`

当前已经开始落地的 JDBC 基础设施对象包括：

- `JdbcConnectionManager`
- `JdbcBankingTransactionRunner`
- `JdbcBankAccountRepository`
- `JdbcBankTransactionRepository`
- `JdbcLedgerEntryRepository`
- `JdbcCoinExchangeRecordRepository`

以及枚举：

- `BankAccountType`
- `BankAccountStatus`
- `BankTransactionType`
- `BankBusinessType`
- `LedgerEntrySide`

## 领域模型与数据库表的映射

### BankAccount

对应表：`bank_account`

职责：

- 表示账户当前状态快照
- 承接玩家账户、系统运营账户、兑换储备、公会等统一账户模型

当前保留字段：

- 账户标识
- 账户类型
- 所有者类型与引用
- 可用余额与冻结余额
- 账户状态
- 版本号
- 展示名
- 元数据 JSON

当前玩家个人账户的唯一标识策略已经固定为：

- `owner_type = PLAYER_UUID`
- `owner_ref = 玩家 UUID 字符串`
- 再叠加 `currency_code` 形成唯一账户约束

当前个人账户初始化策略是：

- 先不在玩家注册或登录时自动建户
- 由 `openAccount(...)` 和当前管理员命令入口按需懒初始化
- 也就是玩家第一次被执行 `open`、`balance`、`grant`、`transfer` 等银行动作时，如果账户不存在，就即时开户，初始余额为 `0`

### BankTransaction

对应表：`bank_transaction`

职责：

- 表示一次完整的银行业务动作
- 承接幂等键、业务类型、来源服和操作人上下文

### LedgerEntry

对应表：`ledger_entry`

职责：

- 表示一条正式账本分录
- 每个账户的增减都单独记一条分录
- 供查询、审计、对账和结算使用

### CoinExchangeRecord

对应表：`coin_exchange_record`

职责：

- 表示一次任务书硬币兑换的业务明细
- 和通用转账主记录分离
- 供后续贡献度结算直接读取

## 仓储接口草案

### BankAccountRepository

职责：

- 查询账户
- 开户保存
- 锁定账户
- 更新余额快照

当前接口包含：

- `findById(...)`
- `findByOwner(...)`
- `save(...)`
- `lockById(...)`
- `lockByIdsInOrder(...)`
- `updateBalances(...)`

其中：

- `lockByIdsInOrder(...)` 对应数据库层“固定加锁顺序”的事务约束

### BankTransactionRepository

职责：

- 基于 `request_id` 查幂等记录
- 保存交易主记录

### LedgerEntryRepository

职责：

- 批量追加正式分录
- 查询账户最近流水

### CoinExchangeRecordRepository

职责：

- 保存兑换业务记录
- 按交易或玩家查询兑换历史

### BankingTransactionRunner

职责：

- 显式表达“银行业务必须运行在数据库事务中”
- 避免未来业务层绕过事务边界直接拼仓储调用

## 当前已开始实现的应用服务能力

当前 banking 包已经不再只是文档映射和接口草图，而是补上了第一批应用服务编排：

- 开户与按拥有者查询账户
- 查询最近流水
- 玩家对玩家转账
- 系统账户参与的内部划转
- 任务书硬币兑换结算
- 基于 `request_id` 的幂等回放

其中：

- `BankingApplicationService` 负责参数校验、账户状态校验、余额校验、统一事务封装和分录生成
- 具体 PostgreSQL/JDBC 细节仍留在仓储实现层，当前还没有直接写死在服务中
- `InternalTransferCommand` 用于承接系统奖励、系统运营账户收支、管理员修正等“一切本质上都是账户到账户”的动作

当前 JDBC 层已经补上：

- 线程内事务连接复用
- `SELECT ... FOR UPDATE` 对应的账户加锁读取
- 主记录、账本分录、兑换记录的基础 CRUD
- 基于 `version` 的账户余额乐观更新

当前还没有接入的内容包括：

- GUI、网络包或跨服同步入口调用 application service

当前已经接入的第一个上层入口是管理员测试命令：

- `/jsirgalaxybase bank open <player>`
- `/jsirgalaxybase bank balance <player>`
- `/jsirgalaxybase bank ledger <player> [limit]`
- `/jsirgalaxybase bank system [summary]`
- `/jsirgalaxybase bank system ledger [limit]`
- `/jsirgalaxybase bank grant <player> <amount> [comment]`
- `/jsirgalaxybase bank transfer <fromPlayer> <toPlayer> <amount> [comment]`

后续又进一步扩展了：

- `/jsirgalaxybase bank public [all|ops|exchange]`
- `/jsirgalaxybase bank public ledger <ops|exchange> [limit]`
- `/jsirgalaxybase bank tx <transactionId>`
- `/jsirgalaxybase bank init system`

当前受管系统账户已经收敛为两类：

- `ops` 系统运营账户
- `exchange` 兑换储备账户

其中：

- 玩家个人账户继续保持按需懒初始化，不自动开户
- `ops` 与 `exchange` 会在服务端启动时自动确保存在

其中配置与模块初始化部分已经开始接入：

- `ModConfiguration` 已新增 PostgreSQL 银行配置项
- `InstitutionCoreModule` 已可在服务端按配置准备 JDBC banking infrastructure
- 当前已经完成本机 PostgreSQL 的真实连通与启动期校验
- 当前已经接入管理员命令入口，并完成了服务端实际命令回显验证
- 当前仍未做的是 GUI 和跨服同步等更正式的上层调用链

## 为什么先做接口而不是 JDBC 实现

当前这样做有几个目的：

- 先把领域边界和仓储边界钉死
- 让银行业务服务层能先围绕接口设计
- 避免未来从直连数据库切到更重的存储实现时重写业务规则

## 当前不在这份草案里直接实现的内容

- JDBC DAO 具体实现
- PostgreSQL DDL 执行脚本
- 领域服务实现
- 命令对象与应用服务编排
- Forge 生命周期接入

这些内容下一步可以继续细化。

其中 PostgreSQL 结构草案已经补到独立 SQL 文件：

- [banking-postgresql-ddl.sql](banking-postgresql-ddl.sql)

## 推荐的下一步

当前最自然的继续顺序是：

1. 基于现有 DDL 草案补 migration 版本脚本与初始化账户脚本
2. 把当前测试命令继续扩成更完整的管理与审计工具
3. 然后补更完整的恢复演练、迁移脚本与正式玩家侧入口