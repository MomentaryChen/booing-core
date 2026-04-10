import { Link } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { useI18n } from "../../i18n";
import {
  AUTH_OVERLAY_LOGIN,
  AUTH_OVERLAY_REGISTER,
  hrefAuthOverlay,
} from "../../services/auth/sessionRouting";
import { ROUTE_KEYS } from "../../navigation/routeKeys";
import { BOOKING_CORE_REPO_OVERVIEW_URL } from "./homeIntroConstants";
import { useVisibleIntroTiles } from "./useVisibleIntroTiles";
import { DestinationsEmptyIllustration } from "../../components/illustrations/DestinationsEmptyIllustration";
import { buttonVariants } from "../../components/ui/button";
import { cn } from "../../lib/cn";

const heroSrc = "/intro/hero-booking.svg";
const DEMO_STORE_PATH = "/client/booking/demo-merchant";

export function HomeIntroPage() {
  const { t } = useI18n();
  const { visibleTiles, awaitingNavigation } = useVisibleIntroTiles();

  const showDemoClosingCta =
    !awaitingNavigation &&
    visibleTiles.some((tile) => tile.routeKey === ROUTE_KEYS.STORE_PUBLIC);

  return (
    <div className="home-intro relative isolate overflow-x-hidden">
      <div
        className="pointer-events-none absolute left-1/2 top-0 -z-10 h-[min(520px,80vh)] w-[min(100%,960px)] -translate-x-1/2 bg-gradient-to-b from-indigo-100/50 via-sky-50/20 to-transparent motion-reduce:from-indigo-50/40"
        aria-hidden
      />
      <div
        className="pointer-events-none absolute -right-20 top-40 -z-10 h-72 w-72 rounded-full bg-indigo-300/15 blur-3xl motion-reduce:opacity-40"
        aria-hidden
      />
      <div
        className="pointer-events-none absolute -left-16 top-[28rem] -z-10 h-56 w-56 rounded-full bg-slate-300/20 blur-3xl motion-reduce:opacity-40"
        aria-hidden
      />
      <header className="home-intro__hero">
        <img className="home-intro__hero-img" src={heroSrc} alt="" decoding="async" />
        <div className="home-intro__hero-copy">
          <h1>{t("introHeroTitle")}</h1>
          <p className="home-intro__hero-lead">{t("introHeroSubtitle")}</p>
          <p className="home-intro__hero-proof">{t("introHeroProofLine")}</p>
          <div className="home-intro__hero-actions">
            <Link
              to={hrefAuthOverlay("/", "", { mode: AUTH_OVERLAY_REGISTER, intent: "merchant" })}
              className={cn(buttonVariants({ variant: "default", size: "default" }))}
            >
              {t("introHeroCtaRegister")}
            </Link>
          </div>
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
            className="home-intro__loading flex flex-col items-center justify-center gap-4 py-10"
            role="status"
            aria-live="polite"
            aria-busy="true"
          >
            <Loader2
              className="size-5 shrink-0 animate-spin text-indigo-600 motion-reduce:animate-none"
              aria-hidden
            />
            <p className="home-intro__loading-text !m-0 text-center">{t("introDestinationsLoading")}</p>
          </div>
        ) : visibleTiles.length === 0 ? (
          <div
            className="mx-auto flex max-w-md flex-col items-center gap-4 rounded-2xl border border-slate-200/90 bg-white/90 px-6 py-10 text-center shadow-sm backdrop-blur-sm"
            role="status"
          >
            <DestinationsEmptyIllustration className="w-full max-w-[280px]" />
            <h3 className="text-lg font-semibold text-slate-900">{t("introDestinationsEmptyTitle")}</h3>
            <p className="text-sm leading-relaxed text-slate-600">{t("introDestinationsEmptyDescription")}</p>
            <Link
              to={hrefAuthOverlay("/", "", { mode: AUTH_OVERLAY_LOGIN, intent: "merchant" })}
              className={cn(buttonVariants({ variant: "default", size: "default" }), "mt-1")}
            >
              {t("introDestinationsEmptyCtaLogin")}
            </Link>
          </div>
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
