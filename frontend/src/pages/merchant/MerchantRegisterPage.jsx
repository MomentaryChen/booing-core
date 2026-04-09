import { Link } from "react-router-dom";
import { isMerchantAuthRequired } from "../../services/merchant/merchantAuth";
import { AUTH_OVERLAY_LOGIN, hrefAuthOverlay } from "../../services/auth/sessionRouting";
import { MerchantRegisterForm } from "../../components/auth/MerchantRegisterForm";
import { useI18n } from "../../i18n";

export function MerchantRegisterPage() {
  const { t } = useI18n();

  const skipTo = hrefAuthOverlay("/", "", { mode: AUTH_OVERLAY_LOGIN, intent: "merchant" });

  return (
    <div className="page">
      <header className="page-hero">
        <h1>{t("registerPageTitle")}</h1>
        <p>{t("registerPageLead")}</p>
        {isMerchantAuthRequired() ? (
          <p className="page-hero-meta">
            <Link to={skipTo}>{t("registerPageSkipToLogin")}</Link>
          </p>
        ) : null}
      </header>

      <MerchantRegisterForm variant="page" />
    </div>
  );
}
