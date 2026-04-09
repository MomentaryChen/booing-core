Execution Mode: auto-run-until-done

0. Initialize run context:
   - `maxRounds = 10`
   - `round = 0`
   - `status = "in_progress"`
   - `blockingIssues = []`

1. Intake requirement from user (`{{input}}`) and classify as frontend / backend / full-stack / infra / spec-only.
2. Planning round: call `pm-agent` and `architect-agent` together; produce scope, risks, acceptance criteria, dependency order, and done definition.
3. Build task list with owners and handoff contracts (inputs, outputs, tests required, completion signal).

4. Main loop:
   - While `status == "in_progress"` and `round < maxRounds`:
     - `round = round + 1`
     - Execute tasks by dependency order:
       - Call domain agent (`frontend-engineer-agent` / `backend-engineer-agent` / `devops-agent` / etc.).
       - If task is substantial UI page/flow work, include `uiux-agent` in the same round before implementation.
       - Pass prior artifacts/spec decisions into next agent.
     - After each code delivery, run `reviewer-agent` gate (mandatory).
      - If reviewer has `critical` findings:
        - immediately stop progression to QA in current round
        - send required fixes to responsible implementation agent
        - rerun `reviewer-agent` after fix (repeat until no `critical`)
      - If reviewer has `high` (but no `critical`) findings:
        - send task back to responsible implementation agent in same loop round
        - rerun reviewer before entering QA
     - Run QA validation (happy path + edge cases + regressions + tenant/scope boundaries).
       - If QA fails: send failing cases to responsible implementation agent and continue loop.
     - PM completion check:
       - If acceptance criteria all pass and no unresolved high/critical findings and QA pass: set `status = "done"`.
       - Else: keep `status = "in_progress"` and continue next round.

5. Stop conditions:
   - Success: `status == "done"`.
   - Failure: `round >= maxRounds` and still not done -> `status = "blocked"` and raise escalation package.

6. Escalation package when blocked:
   - unresolved findings
   - failed QA cases
   - attempted fixes by round
   - decision requests for PM/architect

7. If a spec is created/updated, enforce naming:
   - `doc/specs/YYYY-MM-DD_<kebab-case-topic>.md`
   - move completed specs to `doc/specs/done/` (filename unchanged).

Output:
{
  "plan": {
    "scope": "",
    "tasks": [],
    "dependencies": [],
    "acceptanceCriteria": []
  },
  "deliverables": {
    "codeChanges": [],
    "specChanges": [],
    "testEvidence": []
  },
  "review": {
    "findings": [],
    "status": "pass|blocked"
  },
  "qa": {
    "status": "pass|fail",
    "cases": [],
    "regressions": []
  },
  "run": {
    "status": "in_progress|done|blocked",
    "round": 0,
    "maxRounds": 10,
    "escalationRequired": false
  },
  "issues": [],
  "escalation": {
    "unresolvedFindings": [],
    "failedCases": [],
    "decisionRequests": []
  }
}

User Requirement:
{{input}}