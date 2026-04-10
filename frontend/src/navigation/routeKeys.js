/** Must match {@code platform_pages.route_key} seeded in backend. */
export const ROUTE_KEYS = {
  SYSTEM_DASHBOARD: "nav.system.dashboard",
  SYSTEM_USERS: "nav.system.users",
  MERCHANT_DASHBOARD: "nav.merchant.dashboard",
  MERCHANT_APPOINTMENTS: "nav.merchant.appointments",
  MERCHANT_SCHEDULE: "nav.merchant.schedule",
  CLIENT_TODO: "nav.client.todo",
  STORE_PUBLIC: "nav.store.public",
};

export const ALL_TOP_ROUTE_KEYS = [
  ROUTE_KEYS.SYSTEM_DASHBOARD,
  ROUTE_KEYS.SYSTEM_USERS,
  ROUTE_KEYS.MERCHANT_DASHBOARD,
  ROUTE_KEYS.CLIENT_TODO,
  ROUTE_KEYS.STORE_PUBLIC,
];
