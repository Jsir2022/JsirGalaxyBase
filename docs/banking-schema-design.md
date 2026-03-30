# JsirGalaxyBase 银行系统一期数据表与事务边界设计

日期：2026-03-30

## 目标

这份文档基于 [banking-system-requirements.md](banking-system-requirements.md) 的一期需求，直接给出：

- 一期银行系统的数据表设计
- 主键、唯一约束和关键索引建议
- 关键业务动作的事务边界
- 并发与幂等处理原则

对应的 PostgreSQL 结构草案已经单独落成 SQL 文件：

- [banking-postgresql-ddl.sql](banking-postgresql-ddl.sql)

当前目标不是一次设计未来所有金融能力，而是先把一期必须稳定的：

- 玩家账户
- 公共账户
- 账本流水
- 玩家转账
- 系统发放 / 扣减
- 固定规则兑换结算
- 管理修正

先做成一套可落地、可审计、可扩展的数据库基础。

## 核心设计原则

### 1. 账本为真源

- `ledger_entry` 是不可篡改的正式账本流水
- `bank_account` 中的余额字段是当前状态快照
- 余额可以重建，账本流水不能依赖余额反推

### 2. 账户到账户

所有资金变动统一建模为：

- 至少一个账户减少
- 至少一个账户增加

这意味着：

- 玩家转账是账户到账户
- 职业奖金是系统账户到账户
- 运营账户入账是账户到账户
- 管理修正也是账户到账户或单边修正交易

### 3. 单币种整数金额

- 一期只支持 `星光币`
- 所有金额使用 `BIGINT`
- 单位采用最小记账单位，不使用浮点数

当前建议：

- `1 星光币 = 1 最小记账单位`

如果未来需要小数，再单独升级为固定精度整数，不在一期提前复杂化。

### 4. 事务内同时写余额和流水

每笔资金变动必须在同一个数据库事务中完成：

- 锁定账户
- 校验余额
- 写交易主记录
- 写账本分录
- 更新账户余额快照
- 提交事务

不允许：

- 先改余额再补流水
- 先写流水再异步改余额

### 5. 幂等优先

所有会修改资金的接口都必须接受：

- `request_id` 或 `idempotency_key`

并在数据库侧建立唯一约束，防止重复发奖、重复转账、重复兑换。

## 表结构总览

一期建议最少包含下面这些表：

1. `bank_account`
2. `bank_transaction`
3. `ledger_entry`
4. `coin_exchange_record`
5. `bank_daily_snapshot` 可选

其中真正的核心是前三张。

## 1. bank_account

## 用途

- 保存账户当前状态快照
- 支持玩家账户、系统运营账户、兑换所储备账户、后续公会账户等统一建模

## 建议字段

- `account_id` BIGSERIAL PRIMARY KEY
- `account_no` VARCHAR(32) NOT NULL UNIQUE
- `account_type` VARCHAR(24) NOT NULL
- `owner_type` VARCHAR(24) NOT NULL
- `owner_ref` VARCHAR(64) NOT NULL
- `currency_code` VARCHAR(16) NOT NULL
- `available_balance` BIGINT NOT NULL DEFAULT 0
- `frozen_balance` BIGINT NOT NULL DEFAULT 0
- `status` VARCHAR(16) NOT NULL
- `version` BIGINT NOT NULL DEFAULT 0
- `display_name` VARCHAR(128) NOT NULL
- `metadata_json` JSONB NOT NULL DEFAULT '{}'::jsonb
- `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()
- `updated_at` TIMESTAMPTZ NOT NULL DEFAULT now()

## 字段说明

- `account_type`
  - 取值示例：`PLAYER`、`PUBLIC_FUND`、`EXCHANGE_RESERVE`、`GUILD`
- `owner_type`
  - 取值示例：`PLAYER_UUID`、`SYSTEM`、`PUBLIC_FUND_CODE`、`GUILD_ID`
- `owner_ref`
  - 对应拥有者的唯一标识
- `version`
  - 用于乐观锁和调试，不替代 `SELECT ... FOR UPDATE`

## 建议唯一约束

- `UNIQUE(account_no)`
- `UNIQUE(owner_type, owner_ref, currency_code)`

这样可以保证：

- 一个玩家在同一币种下只有一个正式账户
- 一个受管系统账户不会重复开户

## 建议索引

- `INDEX idx_bank_account_owner(owner_type, owner_ref)`
- `INDEX idx_bank_account_type(account_type)`
- `INDEX idx_bank_account_status(status)`

## 一期典型账户

- 玩家个人账户
- 系统运营账户
- 兑换所启动储备账户
- 后续公会账户

## 2. bank_transaction

## 用途

- 保存一次完整业务交易的主记录
- 作为多条分录的归属根
- 承载幂等键、业务类型和来源上下文

## 建议字段

- `transaction_id` BIGSERIAL PRIMARY KEY
- `request_id` VARCHAR(64) NOT NULL UNIQUE
- `transaction_type` VARCHAR(32) NOT NULL
- `business_type` VARCHAR(32) NOT NULL
- `business_ref` VARCHAR(64)
- `source_server_id` VARCHAR(64) NOT NULL
- `operator_type` VARCHAR(24) NOT NULL
- `operator_ref` VARCHAR(64)
- `player_ref` VARCHAR(64)
- `comment` VARCHAR(255)
- `extra_json` JSONB NOT NULL DEFAULT '{}'::jsonb
- `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()

## 字段说明

- `request_id`
  - 幂等键，任何会改资金的请求都必须唯一
- `transaction_type`
  - 取值示例：`TRANSFER`、`EXCHANGE`、`ADJUSTMENT`
- `business_type`
  - 取值示例：`PLAYER_TRANSFER`、`LV0_BONUS`、`TASK_COIN_EXCHANGE`、`ADMIN_ADJUSTMENT`
- `business_ref`
  - 引用外部业务主键，例如任务、订单、活动、工单

## 建议索引

- `INDEX idx_bank_transaction_business(business_type, business_ref)`
- `INDEX idx_bank_transaction_player(player_ref)`
- `INDEX idx_bank_transaction_created_at(created_at)`

## 3. ledger_entry

## 用途

- 保存正式账本分录
- 每个账户的增减都落成一条独立分录
- 账本查询、对账、审计、结算全部依赖这张表

## 建议字段

- `entry_id` BIGSERIAL PRIMARY KEY
- `transaction_id` BIGINT NOT NULL REFERENCES bank_transaction(transaction_id)
- `account_id` BIGINT NOT NULL REFERENCES bank_account(account_id)
- `entry_side` VARCHAR(8) NOT NULL
- `amount` BIGINT NOT NULL
- `balance_before` BIGINT NOT NULL
- `balance_after` BIGINT NOT NULL
- `currency_code` VARCHAR(16) NOT NULL
- `sequence_in_tx` SMALLINT NOT NULL
- `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()

## 字段说明

- `entry_side`
  - 取值建议：`DEBIT`、`CREDIT`
- `amount`
  - 始终保存正数
- `balance_before` / `balance_after`
  - 保存账户变动前后的快照，便于审计和问题排查
- `sequence_in_tx`
  - 一个事务里多条分录的稳定顺序

## 建议唯一约束

- `UNIQUE(transaction_id, sequence_in_tx)`

## 建议索引

- `INDEX idx_ledger_entry_account_id(account_id, entry_id DESC)`
- `INDEX idx_ledger_entry_transaction_id(transaction_id)`
- `INDEX idx_ledger_entry_created_at(created_at)`

## 账本约束

- 正式分录一旦写入，不允许物理删除
- 正式分录不允许业务层更新
- 修正只能通过新增一笔修正交易实现

## 4. coin_exchange_record

## 用途

- 保存任务书硬币兑换的业务细节
- 让兑换逻辑和通用转账逻辑解耦
- 为后续贡献度结算提供依据

## 建议字段

- `exchange_id` BIGSERIAL PRIMARY KEY
- `transaction_id` BIGINT NOT NULL UNIQUE REFERENCES bank_transaction(transaction_id)
- `player_ref` VARCHAR(64) NOT NULL
- `coin_family` VARCHAR(32) NOT NULL
- `coin_tier` VARCHAR(16) NOT NULL
- `coin_face_value` BIGINT NOT NULL
- `coin_quantity` BIGINT NOT NULL
- `effective_exchange_value` BIGINT NOT NULL
- `contribution_basis_value` BIGINT NOT NULL
- `rule_version` VARCHAR(32) NOT NULL
- `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()

## 设计说明

- 一期不做汇率市场，但固定规则兑换仍然是正式业务
- `rule_version` 用于记录兑换规则版本，避免未来调规则后无法追溯老记录
- `contribution_basis_value` 用于和贡献度系统对接

## 建议索引

- `INDEX idx_coin_exchange_player(player_ref, created_at DESC)`
- `INDEX idx_coin_exchange_rule_version(rule_version)`

## 5. bank_daily_snapshot 可选

## 用途

- 用于报表和对账加速
- 不是权威真源

## 建议字段

- `snapshot_date` DATE NOT NULL
- `account_id` BIGINT NOT NULL REFERENCES bank_account(account_id)
- `opening_balance` BIGINT NOT NULL
- `closing_balance` BIGINT NOT NULL
- `total_credit` BIGINT NOT NULL
- `total_debit` BIGINT NOT NULL
- PRIMARY KEY (`snapshot_date`, `account_id`)

## 说明

- 这张表一期不是必须
- 如果一开始查询压力不大，可以后补

## 一期不单独建表的内容

下面这些一期可以先不拆专表，而是通过 `bank_transaction.business_type` 和 `extra_json` 承载：

- 职业奖金发放明细
- 管理员修正申请单
- 运营账户入账备注

只有当某类业务开始独立复杂化时，再拆专属业务表。

## 事务边界设计

一期关键事务建议如下。

### 1. 开户事务

## 涉及表

- `bank_account`

## 事务步骤

1. 根据 `owner_type + owner_ref + currency_code` 查找现有账户
2. 若已存在，直接返回现有账户
3. 若不存在，插入新账户
4. 提交事务

## 事务要求

- 依赖唯一约束防重
- 并发开户时允许一个成功、另一个读已存在结果

### 2. 玩家转账事务

## 涉及表

- `bank_account`
- `bank_transaction`
- `ledger_entry`

## 事务步骤

1. 校验 `request_id` 是否已存在
2. 按固定顺序锁定源账户和目标账户：`SELECT ... FOR UPDATE`
3. 校验金额大于 `0`
4. 校验源账户 `available_balance >= amount`
5. 插入 `bank_transaction`
6. 写两条 `ledger_entry`
   - 源账户 `DEBIT`
   - 目标账户 `CREDIT`
7. 更新两个账户余额与 `version`
8. 提交事务

## 事务要求

- 源账户扣款和目标账户入账必须原子完成
- 任一步失败都整体回滚
- 幂等键重复时返回原交易结果，不重扣

### 3. 系统奖励发放事务

## 涉及表

- `bank_account`
- `bank_transaction`
- `ledger_entry`

## 事务步骤

1. 锁定系统发放账户与目标玩家账户
2. 校验系统账户余额或根据业务允许透支策略处理
3. 插入 `bank_transaction`
4. 写分录
5. 更新余额
6. 提交事务

## 当前建议

- 即使是系统发钱，也尽量从明确的系统账户转出
- 不建议做“无来源凭空加钱”的隐式逻辑

### 4. 固定规则兑换事务

## 涉及表

- `bank_account`
- `bank_transaction`
- `ledger_entry`
- `coin_exchange_record`

## 事务步骤

1. 校验 `request_id` 是否已存在
2. 锁定兑换所储备账户与玩家账户
3. 计算有效兑换价值
4. 校验兑换所储备账户余额是否足够
5. 插入 `bank_transaction`
6. 写两条 `ledger_entry`
   - 兑换所储备账户 `DEBIT`
   - 玩家账户 `CREDIT`
7. 插入 `coin_exchange_record`
8. 更新账户余额
9. 提交事务

## 事务要求

- 兑换入账与兑换记录必须在同一个事务内完成
- 后续贡献度系统读取 `coin_exchange_record`，不要靠解析备注文案

### 5. 运营账户入账事务

## 涉及表

- `bank_account`
- `bank_transaction`
- `ledger_entry`

## 事务说明

- 运营账户也是正式账户
- 任何手续费、捐赠、地皮购买等收入都必须表现为运营账户正式入账
- 运营账户对玩家的支出也必须走正式分录，不能直接改余额

### 6. 管理员修正事务

## 涉及表

- `bank_account`
- `bank_transaction`
- `ledger_entry`

## 事务步骤

1. 校验操作者权限
2. 锁定目标账户与修正来源账户或修正汇总账户
3. 插入 `bank_transaction`，类型为 `ADJUSTMENT`
4. 写正式分录
5. 更新余额
6. 提交事务

## 事务要求

- 必须保留修正原因和操作者标识
- 不允许直接篡改历史分录

## 并发与锁设计

### 1. 锁账户，不锁玩家

资金事务并发控制的最小单位建议是：

- `bank_account` 行级锁

而不是：

- 玩家全局大锁

### 2. 固定加锁顺序

凡是会同时锁多个账户的事务，都必须按统一顺序加锁：

- 按 `account_id` 从小到大锁定

这样可降低死锁概率。

### 3. 余额更新规则

余额更新采用：

- 事务内 `SELECT ... FOR UPDATE`
- 然后 `UPDATE bank_account SET available_balance = ..., version = version + 1`

不要依赖纯乐观锁独自承担资金一致性。

### 4. 幂等规则

任何写资金的接口都必须要求调用方提供：

- `request_id`

并在 `bank_transaction.request_id` 上建立唯一约束。

## 删除与回滚策略

### 不允许的操作

- 删除正式交易主记录
- 删除正式账本分录
- 更新正式分录金额

### 允许的纠错方式

- 新增一笔反向修正交易
- 新增一笔补偿交易

## 一期推荐枚举值

### account_type

- `PLAYER`
- `PUBLIC_FUND`
- `EXCHANGE_RESERVE`
- `SYSTEM`
- `GUILD`

### status

- `ACTIVE`
- `FROZEN`
- `CLOSED`

### transaction_type

- `TRANSFER`
- `EXCHANGE`
- `ADJUSTMENT`
- `SYSTEM_GRANT`
- `SYSTEM_DEDUCT`

### business_type

- `PLAYER_TRANSFER`
- `LV0_BONUS`
- `TASK_COIN_EXCHANGE`
- `OPS_INCOME`
- `ADMIN_ADJUSTMENT`
- `ACTIVITY_REWARD`

## 一期最小落地建议

如果要最快进入编码，一期可以先实现下面三张强制核心表：

1. `bank_account`
2. `bank_transaction`
3. `ledger_entry`

然后再补：

4. `coin_exchange_record`

这个顺序的好处是：

- 玩家转账
- 系统发奖
- 税池入账
- 管理员修正

都能先共用同一套账户与账本基础。

## 当前结论

一期银行系统最合适的数据库设计，不是“只存一个余额字段”，而是：

- 用 `bank_account` 保存账户当前状态
- 用 `bank_transaction` 保存业务交易根
- 用 `ledger_entry` 保存不可篡改的正式账本分录
- 用单独事务保证每次资金变动原子提交
- 用 `request_id` 保证幂等
- 用独立账户承接运营账户、兑换储备和后续公会资金

这样后面继续做：

- 贡献度结算
- 声望沉淀
- 公会账户
- 跨服共享状态

时都不需要重写银行底层。