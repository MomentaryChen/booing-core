# Auto Dev（依 workflow 啟動需求流程）

你必須**完整依序**執行專案內定義的流程，不可跳步、不可省略 gate。

## 必讀

先讀取並嚴格遵守 repo 根目錄下的：

`.cursor/workflows/auto-dev.md`

## 你要做的事

1. 以 **Execution Mode: auto-run-until-done** 執行：初始化 `maxRounds`、`round`、`status`、`blockingIssues` 如 workflow 所述。
2. **User Requirement** 使用下方「需求內容」；若使用者另在對話中補充，一併納入 intake。
3. 依 workflow 步驟 1→7：分類需求 → PM + Architect 同輪規劃 → 任務清單與交接 → 主迴圈（實作 / UIUX / reviewer / QA / PM 驗收）。
4. **Reviewer gate**：出現 `critical` 時，**立刻**回修、**不進 QA**，修完**立刻**重跑 reviewer，直到無 `critical`；`high` 亦須回修並重跑 reviewer 後才可進 QA。
5. 每輪結束依 workflow 更新 **Output** JSON 結構（`plan`、`deliverables`、`review`、`qa`、`run`、`issues`、`escalation`）。
6. 觸及規格檔時遵守：`doc/specs/YYYY-MM-DD_<kebab-case-topic>.md`；已結案移至 `doc/specs/done/`。

## 需求內容

{{input}}
