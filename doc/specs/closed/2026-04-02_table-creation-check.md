# Spec: 檢查所有 JPA tables 是否都有建立（MySQL dev/prod）
 
- **Date**: 2026-04-02
- **Owner**: PM (booking-core)
- **Status**: Done (archived in `doc/specs/closed/`)

## Closure Handling

- `pm-agent` is the default owner for closure handling after archival.
- `pm-agent` maintains reference integrity if related active specs change.
- If reopened, create a new dated spec under `doc/specs/` and link back to this archived spec.
 
## 問題敘述
需求是「請檢查是否所有的 table 都有建立」。在目前 backend 採用 Spring Boot + JPA/Hibernate，且尚未導入 Flyway/Liquibase 的前提下，DB schema 的事實上來源偏向 **code-first**（Entity 定義 + Hibernate 生成策略）。
 
本規格定義一套**可重複執行**的驗證流程，確保：
- **dev**（`ddl-auto=update`）不會因自動建表而掩蓋 schema 漂移
- **prod**（`ddl-auto=validate`）具備明確的 schema 驗證與「缺表」可追蹤輸出
 
## 現況（以本 repo 設定為準）
- **dev**：`spring.jpa.hibernate.ddl-auto=update`（會自動建立/更新表）
- **prod**：`spring.jpa.hibernate.ddl-auto=validate`（缺表或不符會導致啟動失敗）
- `infra/docker-compose.yml` 的 backend container 以 `SPRING_PROFILES_ACTIVE=prod` 對 MySQL `bookingcore` 啟動
 
## 期望表清單（Source of truth）
### 原則
「期望有哪些表」必須從 **JPA/Hibernate 的 schema metadata** 推導，而不是手工維護表名清單。原因：
- 不一定每張表都對應一個 `@Entity`
  - `@ManyToMany` 可能產生 join table
  - `@ElementCollection` 可能產生 collection table
- naming strategy 可能導致實際表名與類名不同
 
### 建議輸出格式
輸出一份排序後的純表名清單，例如 `expected_tables.txt`，作為 diff 的輸入。
 
### 期望表清單產生方式（優先順序）
- **A. 由 Hibernate metadata 直接列出 physical table names（推薦）**
  - 以 Spring Boot 測試或一個專用 runtime 指令啟動 Hibernate，讀取 mapping metadata 並輸出預期 table names。
  - 優點：包含 implicit tables（join/collection tables）；比掃描 `@Entity/@Table` 更準。
 
- **B. 由 Hibernate schema export（DDL script）萃取表名（次佳）**
  - 產出 create DDL（不套用），再從 DDL 解析 `CREATE TABLE ...` 得到表清單。
  - 優點：接近真實 DB schema；缺點：解析 DDL 需注意 quoting / schema prefix。
 
- **C. 只用 `ddl-auto=validate` 當 gate（必要但不夠）**
  - `validate` 能抓到「不存在 / 欄位型別不符」等問題，但不會自然輸出一份可 diff 的 table 清單（利於稽核/報表）。
 
## 實際表清單（DB 端）
### 不建議只用 `SHOW TABLES`
`SHOW TABLES` 可用於人工確認，但腳本化/可攜性較差、也較難加上 schema/過濾條件。
 
### 建議查詢（MySQL）
以 `information_schema.tables` 為準（請將 `:dbName` 替換為實際資料庫名，例如 `bookingcore`）。
 
```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = :dbName
  AND table_type = 'BASE TABLE'
ORDER BY table_name;
```
 
（選用）若系統使用 VIEW，也可額外拉出：
 
```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = :dbName
  AND table_type = 'VIEW'
ORDER BY table_name;
```
 
## 比對規則（Expected vs Actual）
### 集合差異
- **Missing in DB** \(缺少表\) = Expected − Actual
- **Orphaned in DB** \(多出表\) = Actual − Expected
 
### Gate 政策（建議）
- **prod**
  - Missing in DB：**FAIL**
  - Orphaned in DB：預設 **WARN**（可能是歷史遺留/改名後未清理），但需有處置流程
  - 另以 `ddl-auto=validate` 作為啟動硬門檻（本 repo 已是）
- **dev**
  - 仍輸出 Missing/Orphaned 報表（避免 `update` 靜默建表造成「不知不覺新增/漂移」）
  - 不一定要 fail（視團隊容忍度），但至少要能在 CI/本地跑出一致結果
 
## 可重複驗證流程（Runbook）
### Dev（本機直跑）
- **前置**
  - MySQL 可連線（預設：host `localhost`、port `23306`、db `bookingcore`，以 `application-dev.yml` env 預設為準）
 
- **步驟**
  - 以「期望表清單產生方式 A/B」輸出 `expected_tables.txt`
  - 以 `information_schema` 查詢輸出 `actual_tables.txt`
  - 產出 diff：
    - `missing_tables.txt`
    - `orphaned_tables.txt`
 
- **補充**
  - dev 的 `ddl-auto=update` 會自動建表，因此「缺表」通常會被自動補上；仍需關注的是：
    - 是否出現不預期的新表（Orphaned/新增）
    - 是否存在 implicit tables（join/collection）導致的命名落差
 
### Prod（容器/部署環境）
- **前置**
  - 使用 `SPRING_PROFILES_ACTIVE=prod`
  - 目標 MySQL 使用 env 注入（在 compose 中是 `DB_URL/DB_USERNAME/DB_PASSWORD`）
 
- **步驟**
  - 先跑 **validate gate**：以 prod 設定啟動應用（或啟動一個 schema-validate job）
    - 啟動失敗即代表 schema mismatch（包含缺表/缺欄位/型別不符等）
  - 再跑「期望 vs 實際表名 diff」並保存報表（便於稽核與追蹤）
 
## 多租戶（Multi-tenancy）注意事項（必列）
本需求雖以「表是否存在」為主，但多租戶隔離的常見風險多在欄位/索引層級：
- 若採 **single schema + `tenant_id`**（目前設定看起來最可能），建議擴充驗證：
  - tenant-scoped tables 必須有 `tenant_id` 欄位（以及必要的 NOT NULL / index 規範）
  - 需維護一份**允許不含 `tenant_id` 的全域表** allowlist（例如系統字典/模板）
- **重要陷阱**：Hibernate implicit join table 通常不會自帶 `tenant_id`；若租戶隔離要求嚴格，應避免使用 implicit join table，改以顯式 join entity 方式建模。
 
## 風險與常見陷阱
- **Implicit tables 未被 `@Entity/@Table` 掃描到**
- **Naming strategy / 大小寫差異**：`OrderItem` → `order_item`，以及 MySQL 在不同 OS 的大小寫行為差異
- **Schema/catalog 不一致**：`@Table(schema=...)` 與實際查詢 `table_schema` 不一致會造成誤判
- **“存在”不代表 “匹配”**：表名存在但欄位/約束不符，需靠 `validate`（或更完整的 schema diff）補強
 
## Acceptance criteria（可驗證）
- 能在 dev/prod 針對同一個 MySQL schema 產出：
  - `expected_tables.txt`（由 JPA/Hibernate 推導）
  - `actual_tables.txt`（由 MySQL `information_schema` 查得）
  - `missing_tables.txt` / `orphaned_tables.txt`
- 在 prod profile 下：
  - 若缺表/欄位/型別不符，`ddl-auto=validate` 導致啟動失敗（硬門檻）
  - 同時仍可取得「缺少哪些表」的清單（利於稽核/追蹤）
 
## Handoff / ownership
- **architect-agent（諮詢/把關）**
  - 確認多租戶模型下「tenant_id 欄位/索引」的驗證範圍與 allowlist 策略
- **backend-engineer-agent（落地實作）**
  - 提供一個可重複執行的方式輸出 `expected_tables.txt`（metadata dump 或 schema export）
  - 提供一個可重複執行的方式從 MySQL 輸出 `actual_tables.txt` 並做 diff
- **devops-agent（自動化）**
  - 在 CI/部署流程中加入 prod-grade 的 validate gate 與報表保存（artifact）
- **qa-agent（驗證）**
  - 在乾淨環境跑一次流程並附上輸出檔案/截圖（若有）
 
