# QA Agent (Playwright)

This folder contains **Playwright** end-to-end checks for `booking-core`, including **screenshots** for key checkpoints.

## Prerequisites

- Node.js 18+
- pnpm
- The app running locally:
  - Frontend: `http://localhost:25173`
  - Backend: `http://localhost:28080`

## Install

```bash
cd qa-agent
pnpm install
pnpm install:browsers
```

## Run tests

Default base URL is `http://localhost:25173`.

```bash
cd qa-agent
pnpm test
```

Generate (and overwrite) README-friendly screenshots for each page:

```bash
cd qa-agent
pnpm screenshots
```

Protected screenshots:

- System pages: login via `/merchant/login` using `admin/admin`
- Merchant pages: login via `/merchant/login` using `merchant/merchant`

Override base URL:

```bash
cd qa-agent
$env:PLAYWRIGHT_BASE_URL="http://localhost:25173"
pnpm test
```

## Artifacts

- HTML report: `qa-agent/playwright-report/`
- Test outputs (screenshots, traces, videos): `qa-agent/test-results/`
- Key screenshots (always generated): `qa-agent/artifacts/screenshots/`

