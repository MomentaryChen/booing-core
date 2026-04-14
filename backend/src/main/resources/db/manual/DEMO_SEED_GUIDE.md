# Demo Seed Guide

This document explains the demo accounts and data created by:

- `backend/src/main/resources/db/manual/seed_manual_baseline.sql`

## 1) Demo Account Overview

The manual seed script creates these platform users:

- Merchant user (tenant: `manual-seed-merchant`): `merchant.manual@demo.local`
- Merchant user (tenant: `urban-fade-studio`): `merchant.urban@demo.local`
- Merchant user (tenant: `serenity-spa-lab`): `merchant.serenity@demo.local`
- Client user: `client.manual@demo.local`

### Demo Plaintext Credentials

Use this fixed plaintext set for local demo:

| Role | Tenant | Username | Plaintext Password |
| --- | --- | --- | --- |
| Merchant | `manual-seed-merchant` | `merchant.manual@demo.local` | `DemoMerchant123!` |
| Merchant | `urban-fade-studio` | `merchant.urban@demo.local` | `DemoMerchant123!` |
| Merchant | `serenity-spa-lab` | `merchant.serenity@demo.local` | `DemoMerchant123!` |
| Client | - | `client.manual@demo.local` | `DemoClient123!` |

Important:

- The SQL stores bcrypt hashes, not plaintext.
- `seed_manual_baseline.sql` ships with bcrypt hashes matching the plaintext table above, and also
  refreshes `password_hash` for existing demo users on re-run.

### If login still fails (most common causes)

Backend login requires JWT to be enabled:

- Set `booking.platform.jwt.secret` (env: `JWT_SECRET`) to a non-empty secret.

Merchant/client login IDs must be email-shaped (enforced by `AuthService`):

- Always use the `@demo.local` usernames from the table above.

## 2) Admin Account (Separate Path)

`SYSTEM_ADMIN` is not created by this manual demo SQL.

Admin is created by app startup config (if enabled):

- `INTERNAL_SYSTEM_ADMIN_AUTO_PROVISION`
- `INTERNAL_SYSTEM_ADMIN_USERNAME`
- `INTERNAL_SYSTEM_ADMIN_PASSWORD`

If you do not want auto-provision, set `INTERNAL_SYSTEM_ADMIN_AUTO_PROVISION=false` and insert admin manually.

## 3) Merchants Created for UI Diversity

The seed creates multiple public merchants for richer storefront UI:

- `manual-seed-merchant`
- `urban-fade-studio`
- `serenity-spa-lab`

These merchants include:

- service catalog (`service_items`)
- resources (`resource_items`)
- profile data (`merchant_profiles`)
- theme/customization data (`customization_configs`)
- business hours (`business_hours`)

All three merchants have independent login users, memberships, and RBAC bindings.

## 4) Recommended Demo Credential Convention

To keep demos consistent across teammates:

1. Keep the fixed plaintext demo passwords from the table above.
2. If you need to rotate passwords, regenerate bcrypt hashes and update the variables in SQL.
   - Helper: `backend/tools/BcryptGen.java` (see file header for compile/run commands)
3. Re-run `seed_manual_baseline.sql` (safe: idempotent inserts + password refresh updates).

## 5) Quick Verification After Running SQL

Use verification queries at the bottom of `seed_manual_baseline.sql` to confirm:

- platform users exist and are enabled
- merchant membership and RBAC bindings exist
- service list is populated for all demo merchants
- profile/theme/business-hours data exists for all demo merchants

