import { Link } from "react-router-dom";
import { useI18n } from "../../i18n";
import { useVisibleIntroTiles } from "./useVisibleIntroTiles";

const heroSrc = "/intro/hero-booking.svg";

export function HomeIntroPage() {
  const { t } = useI18n();
  const { visibleTiles, awaitingNavigation } = useVisibleIntroTiles();

  return (
    <div className="home-intro">
      <header className="home-intro__hero">
        <img className="home-intro__hero-img" src={heroSrc} alt="" decoding="async" />
        <div className="home-intro__hero-copy">
          <h1>{t("introHeroTitle")}</h1>
          <p className="home-intro__hero-lead">{t("introHeroSubtitle")}</p>
        </div>
      </header>

      <section className="home-intro__values" aria-labelledby="home-intro-values-heading">
        <h2 id="home-intro-values-heading" className="home-intro__values-title">
          {t("introValueSectionTitle")}
        </h2>
        <ul className="home-intro__value-list">
          <li>{t("introValue1")}</li>
          <li>{t("introValue2")}</li>
          <li>{t("introValue3")}</li>
        </ul>
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

      <footer className="home-intro__footer">
        <p>{t("introFooterNote")}</p>
      </footer>
    </div>
  );
}
