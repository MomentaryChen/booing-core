import { ROUTE_KEYS } from "../../navigation/routeKeys";

/** Static metadata for home intro destination cards; visibility is filtered in {@link useVisibleIntroTiles}. */
export const INTRO_TILES = [
  {
    routeKey: ROUTE_KEYS.MERCHANT_DASHBOARD,
    to: "/merchant",
    img: "/intro/role-merchant.svg",
    titleKey: "introTileMerchantTitle",
    captionKey: "introTileMerchantCaption",
    altKey: "introTileMerchantAlt",
  },
  {
    routeKey: ROUTE_KEYS.CLIENT_TODO,
    to: "/client",
    img: "/intro/role-client.svg",
    titleKey: "introTileClientTitle",
    captionKey: "introTileClientCaption",
    altKey: "introTileClientAlt",
  },
  {
    routeKey: ROUTE_KEYS.SYSTEM_DASHBOARD,
    to: "/system",
    img: "/intro/role-system.svg",
    titleKey: "introTileSystemTitle",
    captionKey: "introTileSystemCaption",
    altKey: "introTileSystemAlt",
  },
  {
    routeKey: ROUTE_KEYS.STORE_PUBLIC,
    to: "/client/booking/demo-merchant",
    img: "/intro/role-store.svg",
    titleKey: "introTileStoreTitle",
    captionKey: "introTileStoreCaption",
    altKey: "introTileStoreAlt",
    variant: "demo",
    badgeKey: "introTileDemoBadge",
  },
];
