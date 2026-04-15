# Merchant Settings Toast Feedback

## Scope
- Add toast feedback for success/failure on `/merchant` settings-related actions.
- Provide lightweight interaction on failure (retry action where applicable).

## Implementation Summary
- Mounted global merchant toast host in `frontend/src/apps/merchant/layouts/MerchantLayout.tsx`.
- Added shared toast primitives/store in:
  - `frontend/src/components/ui/toast.tsx`
  - `frontend/src/components/ui/use-toast.ts`
  - `frontend/src/components/ui/toaster.tsx`
- Integrated success/failure toast behavior into:
  - `frontend/src/apps/merchant/pages/SettingsPage.tsx`
  - `frontend/src/apps/merchant/pages/SchedulePage.tsx`
  - `frontend/src/apps/merchant/pages/ResourcesPage.tsx`
  - `frontend/src/apps/merchant/pages/TeamsPage.tsx`
  - `frontend/src/apps/merchant/pages/BookingsPage.tsx`
  - `frontend/src/apps/merchant/pages/BootstrapGatePage.tsx`

## Validation Evidence
- Frontend build: `npm run build` PASS.
- Reviewer result: No critical findings.
- QA mapping prepared against success checklist including normal, edge, and rapid-click scenarios.

## Closeout
- Decision: Closed.
- Unresolved follow-ups:
  - Validate keyboard/screen-reader flow for toast action/close in manual QA.
  - Observe post-release toast frequency/noise and refine copy if needed.
