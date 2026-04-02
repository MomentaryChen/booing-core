import { Link, useLocation } from "react-router-dom";
import { useMemo } from "react";
import { useI18n } from "../../i18n";
import { getStoredAccessToken } from "../../services/api/client";
import { isMerchantAuthRequired } from "../../services/merchant/merchantAuth";
import { useNavigation } from "../../navigation/NavigationContext";
import { ROUTE_KEYS } from "../../navigation/routeKeys";

const heroSrc = "/intro/hero-booking.svg";

const tiles = [
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
  },
];

function useVisibleIntroTiles() {
  const location = useLocation();
  const { routeKeys, loading } = useNavigation();

  return useMemo(() => {
    if (!isMerchantAuthRequired()) return tiles;
    if (!getStoredAccessToken()) return tiles;
    if (loading) return [];
    return tiles.filter((tile) => routeKeys.includes(tile.routeKey));
  }, [routeKeys, loading, location.pathname]);
}

export function HomeIntroPage() {
  const { t } = useI18n();
  const visibleTiles = useVisibleIntroTiles();

  return (
    <div className="home-intro">
      <header className="home-intro__hero">
        <img className="home-intro__hero-img" src={heroSrc} alt="" decoding="async" />
        <div className="home-intro__hero-copy">
          <h1>{t("introHeroTitle")}</h1>
          <p>{t("introHeroSubtitle")}</p>
        </div>
      </header>

      <section className="home-intro__gallery" aria-label={t("introGalleryLabel")}>
        <h2 className="home-intro__section-title">{t("introSectionTitle")}</h2>
        <div className="home-intro__grid">
          {visibleTiles.map((tile) => (
            <Link key={tile.to} to={tile.to} className="home-intro__card">
              <div className="home-intro__card-media">
                <img src={tile.img} alt={t(tile.altKey)} loading="lazy" decoding="async" />
              </div>
              <div className="home-intro__card-body">
                <h3>{t(tile.titleKey)}</h3>
                <p>{t(tile.captionKey)}</p>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
