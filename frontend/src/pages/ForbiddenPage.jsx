import { Link } from "react-router-dom";
import { useI18n } from "../i18n";
import { AUTH_OVERLAY_LOGIN, hrefAuthOverlay } from "../services/auth/sessionRouting";

export function ForbiddenPage() {
  const { t } = useI18n();

  return (
    <div className="page">
      <header className="page-hero">
        <h1>{t("forbidden403Title")}</h1>
        <p>{t("forbidden403Lead")}</p>
      </header>
      <section className="card">
        <Link className="btn btn-primary" to={hrefAuthOverlay("/", "", { mode: AUTH_OVERLAY_LOGIN, intent: "merchant" })}>
          {t("forbidden403CtaLogin")}
        </Link>
        <span style={{ margin: "0 12px" }} />
        <Link className="btn" to="/">
          {t("forbidden403CtaHome")}
        </Link>
      </section>
    </div>
  );
}
