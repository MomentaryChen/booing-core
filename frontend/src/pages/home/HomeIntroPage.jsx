import { Link } from "react-router-dom";
import { useI18n } from "../../i18n";
import { ROUTE_KEYS } from "../../navigation/routeKeys";
import { BOOKING_CORE_REPO_OVERVIEW_URL } from "./homeIntroConstants";
import { useVisibleIntroTiles } from "./useVisibleIntroTiles";

const heroSrc = "/intro/hero-booking.svg";
const DEMO_STORE_PATH = "/client/booking/demo-merchant";

export function HomeIntroPage() {
  const { t } = useI18n();
  const { visibleTiles, awaitingNavigation } = useVisibleIntroTiles();

  const showDemoClosingCta =
    !awaitingNavigation &&
    visibleTiles.some((tile) => tile.routeKey === ROUTE_KEYS.STORE_PUBLIC);

  return (
    <div className="home-intro">
      <header className="home-intro__hero">
        <img className="home-intro__hero-img" src={heroSrc} alt="" decoding="async" />
        <div className="home-intro__hero-copy">
          <h1>{t("introHeroTitle")}</h1>
          <p className="home-intro__hero-lead">{t("introHeroSubtitle")}</p>
          <p className="home-intro__hero-proof">{t("introHeroProofLine")}</p>
        </div>
      </header>

      <section className="home-intro__intents" aria-labelledby="home-intro-intents-heading">
        <h2 id="home-intro-intents-heading" className="home-intro__intents-title">
          {t("introIntentSectionTitle")}
        </h2>
        <ul className="home-intro__intent-chips">
          <li className="home-intro__intent-chip">{t("introIntentChipDev")}</li>
          <li className="home-intro__intent-chip">{t("introIntentChipMerchant")}</li>
          <li className="home-intro__intent-chip">{t("introIntentChipVisitor")}</li>
        </ul>
      </section>

      <section className="home-intro__benefits" aria-labelledby="home-intro-benefits-heading">
        <h2 id="home-intro-benefits-heading" className="home-intro__benefits-title">
          {t("introValueSectionTitle")}
        </h2>
        <div className="home-intro__benefit-grid">
          <div className="home-intro__benefit-card">
            <p className="home-intro__benefit-card-text">{t("introValue1")}</p>
          </div>
          <div className="home-intro__benefit-card">
            <p className="home-intro__benefit-card-text">{t("introValue2")}</p>
          </div>
          <div className="home-intro__benefit-card">
            <p className="home-intro__benefit-card-text">{t("introValue3")}</p>
          </div>
        </div>
      </section>

      <section className="home-intro__included" aria-labelledby="home-intro-included-heading">
        <h2 id="home-intro-included-heading" className="home-intro__included-title">
          {t("introIncludedTitle")}
        </h2>
        <ol className="home-intro__included-list">
          <li>{t("introIncluded1")}</li>
          <li>{t("introIncluded2")}</li>
          <li>{t("introIncluded3")}</li>
          <li>{t("introIncluded4")}</li>
        </ol>
      </section>

      <section className="home-intro__destinations" aria-labelledby="home-intro-dest-heading">
        <h2 id="home-intro-dest-heading" className="home-intro__section-title">
          {t("introSectionTitle")}
        </h2>

        {awaitingNavigation ? (
          <div
            className="home-intro__loading"
            role="status"
            aria-live="polite"
            aria-busy="true"
          >
            <div className="home-intro__loading-skel" aria-hidden />
            <p className="home-intro__loading-text">{t("introDestinationsLoading")}</p>
          </div>
        ) : visibleTiles.length === 0 ? (
          <p className="home-intro__empty" role="status">
            {t("introDestinationsEmpty")}
          </p>
        ) : (
          <ul className="home-intro__grid">
            {visibleTiles.map((tile) => (
              <li key={tile.to} className="home-intro__grid-item">
                <Link
                  to={tile.to}
                  className={
                    tile.variant === "demo"
                      ? "home-intro__card home-intro__card--demo"
                      : "home-intro__card"
                  }
                >
                  <div className="home-intro__card-media">
                    <img src={tile.img} alt={t(tile.altKey)} loading="lazy" decoding="async" />
                  </div>
                  <div className="home-intro__card-body">
                    <div className="home-intro__card-head">
                      <h3>{t(tile.titleKey)}</h3>
                      {tile.badgeKey ? (
                        <span className="home-intro__badge">{t(tile.badgeKey)}</span>
                      ) : null}
                    </div>
                    <p>{t(tile.captionKey)}</p>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="home-intro__closing" aria-labelledby="home-intro-closing-heading">
        <h2 id="home-intro-closing-heading" className="home-intro__closing-title">
          {t("introClosingTitle")}
        </h2>
        <p className="home-intro__closing-lead">
          {showDemoClosingCta ? t("introClosingLead") : t("introClosingLeadNoDemo")}
        </p>
        <div className="home-intro__closing-actions">
          {showDemoClosingCta ? (
            <Link className="home-intro__cta home-intro__cta--primary" to={DEMO_STORE_PATH}>
              {t("introClosingCtaDemo")}
            </Link>
          ) : null}
          <a
            className="home-intro__cta home-intro__cta--secondary"
            href={BOOKING_CORE_REPO_OVERVIEW_URL}
            target="_blank"
            rel="noopener noreferrer"
          >
            {t("introClosingReadmeLabel")}
          </a>
        </div>
      </section>

      <footer className="home-intro__footer">
        <p>{t("introFooterNote")}</p>
      </footer>
    </div>
  );
}
