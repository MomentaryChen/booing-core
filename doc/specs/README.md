# Spec 流程說明（open / progress / closed）

本專案的 spec 採三段式狀態資料夾管理，避免進行中與已結案文件混雜。

## 目錄用途

- `doc/specs/open/`
  - 新提案、待確認需求、尚未進入實作排程的 spec。
  - **所有擬新增 spec 一律先放這裡**。
- `doc/specs/progress/`
  - 已確認且正在執行中的 spec（設計、開發、驗收進行中）。
- `doc/specs/closed/`
  - 已正式結案的 spec（封存，不作為現行主檔持續大改）。

## 流轉規則

1. 建立新 spec：`open/`
2. 開始執行：`open/ -> progress/`（同檔名移動）
3. 正式結案：`progress/ -> closed/`（同檔名移動）

## 命名規範

- 檔名固定：`YYYY-MM-DD_<kebab-case-topic>.md`
- 範例：`2026-04-10_role-capability-and-merchant-team-definition.md`

## 注意事項

- 同一份 spec 在任一時間只應存在於一個狀態資料夾。
- 移動狀態後，需更新 repo 內所有舊路徑引用。
- 詳細規則請參考：
  - `.cursor/rules/spec-naming-convention.mdc`
  - `.cursor/rules/spec-done-archive.mdc`
