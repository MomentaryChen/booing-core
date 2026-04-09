import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { api } from "../../services/api/client";
import { isMerchantAuthRequired } from "../../services/merchant/merchantAuth";
import { setStoredMerchantId } from "../../services/merchant/merchantStorage";
import {
  AUTH_OVERLAY_LOGIN,
  hrefAuthOverlay,
  setPendingRegisterUserType,
  trySanitizeServerNextDestination,
} from "../../services/auth/sessionRouting";
import { useI18n } from "../../i18n";

function normalizeSlug(value) {
  return value
    .trim()
    .toLowerCase()
    .replace(/\s+/g, "-")
    .replace(/[^a-z0-9-]/g, "");
}

/**
 * @param {{ variant?: 'page' | 'overlay' }} props
 */
export function MerchantRegisterForm({ variant = "page" }) {
  const { t } = useI18n();
  const location = useLocation();
  const navigate = useNavigate();
  const [registerType, setRegisterType] = useState("MERCHANT");
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [slugTouched, setSlugTouched] = useState(false);
  const [clientUsername, setClientUsername] = useState("");
  const [clientPassword, setClientPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  function onNameChange(v) {
    setName(v);
    if (!slugTouched) {
      setSlug(normalizeSlug(v));
    }
  }

  function onSlugChange(v) {
    setSlugTouched(true);
    setSlug(normalizeSlug(v));
  }

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    if (registerType === "MERCHANT") {
      if (!name.trim() || !slug.trim()) {
        setError(t("registerErrorRequired"));
        return;
      }
    } else {
      if (!clientUsername.trim() || !clientPassword) {
        setError(t("registerErrorClientFields"));
        return;
      }
      if (clientPassword.length < 6) {
        setError(t("registerErrorPasswordLength"));
        return;
      }
    }
    setLoading(true);
    try {
      const body =
        registerType === "MERCHANT"
          ? { registerType, name: name.trim(), slug: slug.trim() }
          : { registerType, username: clientUsername.trim(), password: clientPassword };
      const data = await api("/auth/register", {
        method: "POST",
        body: JSON.stringify(body),
      });
      if (registerType === "MERCHANT") {
        setStoredMerchantId(data.id);
      }
      setPendingRegisterUserType(registerType);
      const next = trySanitizeServerNextDestination(data.nextDestination);
      if (next) {
        navigate(next, { replace: true });
      } else {
        const fallback = hrefAuthOverlay("/", "", {
          mode: AUTH_OVERLAY_LOGIN,
          intent: registerType === "CLIENT" ? "client" : "merchant",
          registered: true,
        });
        navigate(fallback, { replace: true });
      }
    } catch (err) {
      setError(err.message || t("registerErrorGeneric"));
    } finally {
      setLoading(false);
    }
  }

  const switchToLoginTo = hrefAuthOverlay(location.pathname, location.search, {
    mode: AUTH_OVERLAY_LOGIN,
    intent: "merchant",
  });

  const cancelTo = hrefAuthOverlay("/", "", {
    mode: AUTH_OVERLAY_LOGIN,
    intent: "merchant",
  });

  const cardClass =
    variant === "overlay" ? "merchant-login-card merchant-login-card--overlay" : "merchant-login-card";

  const formBody = (
    <>
      {error && (
        <p className="merchant-login-error" role="alert" aria-live="assertive">
          {error}
        </p>
      )}
      <form onSubmit={onSubmit} className="merchant-login-form" aria-busy={loading}>
        <label>
          {t("registerUserTypeLabel")}
          <select
            id="register-user-type"
            value={registerType}
            onChange={(e) => setRegisterType(e.target.value)}
            aria-required="true"
            aria-describedby="register-user-type-help"
          >
            <option value="MERCHANT">{t("registerUserTypeMerchant")}</option>
            <option value="CLIENT">{t("registerUserTypeClient")}</option>
          </select>
        </label>
        <p id="register-user-type-help" className="merchant-register-hint">
          {t("registerUserTypeHelper")}
        </p>
        {registerType === "MERCHANT" ? (
          <>
            <label>
              {t("registerNameLabel")}
              <input
                type="text"
                value={name}
                onChange={(e) => onNameChange(e.target.value)}
                placeholder={t("registerNamePlaceholder")}
                autoComplete="organization"
              />
            </label>
            <label>
              {t("registerSlugLabel")}
              <input
                type="text"
                value={slug}
                onChange={(e) => onSlugChange(e.target.value)}
                placeholder={t("registerSlugPlaceholder")}
                autoComplete="off"
              />
              <span className="merchant-register-hint">{t("registerSlugHint")}</span>
            </label>
          </>
        ) : (
          <>
            <label>
              {t("registerClientUsernameLabel")}
              <input
                type="text"
                value={clientUsername}
                onChange={(e) => setClientUsername(e.target.value)}
                placeholder={t("registerClientUsernamePlaceholder")}
                autoComplete="username"
              />
            </label>
            <label>
              {t("registerClientPasswordLabel")}
              <input
                type="password"
                value={clientPassword}
                onChange={(e) => setClientPassword(e.target.value)}
                placeholder={t("registerClientPasswordPlaceholder")}
                autoComplete="new-password"
              />
              <span className="merchant-register-hint">{t("registerClientPasswordHint")}</span>
            </label>
          </>
        )}
        <div className="merchant-register-actions">
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? t("registerSubmitting") : t("registerSubmit")}
          </button>
          {variant === "page" ? (
            <Link className="btn" to={isMerchantAuthRequired() ? cancelTo : "/merchant"}>
              {isMerchantAuthRequired() ? t("registerCancelLogin") : t("registerCancelDashboard")}
            </Link>
          ) : (
            <Link className="btn" to={switchToLoginTo}>
              {t("registerSwitchToLogin")}
            </Link>
          )}
        </div>
      </form>
    </>
  );

  if (variant === "overlay") {
    return (
      <div className={cardClass}>
        <p className="merchant-login-eyebrow">{t("loginEyebrowBrand")}</p>
        <h1 id="auth-overlay-register-title">{t("registerOverlayTitle")}</h1>
        <p className="merchant-login-lede">{t("registerOverlayLead")}</p>
        {formBody}
      </div>
    );
  }

  return (
    <section className="card" style={{ maxWidth: "32rem" }}>
      <div className="card-header">
        <h2>{t("registerCardTitle")}</h2>
      </div>
      <div style={{ padding: "1rem" }}>{formBody}</div>
    </section>
  );
}
