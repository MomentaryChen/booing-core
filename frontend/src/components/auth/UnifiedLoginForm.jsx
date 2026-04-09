import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { api, clearStoredAccessToken, getStoredAccessToken, setStoredAccessToken } from "../../services/api/client";
import { isMerchantAuthRequired } from "../../services/merchant/merchantAuth";
import { getMerchantIdFromAccessToken, getRoleFromAccessToken } from "../../services/platformRole";
import { clearStoredMerchantId, setStoredMerchantId } from "../../services/merchant/merchantStorage";
import { useNavigation } from "../../navigation/NavigationContext";
import {
  AUTH_OVERLAY_REGISTER,
  consumePendingRegisterUserType,
  hrefAuthOverlay,
  peekPendingRegisterUserType,
  resolvePostLoginDestination,
} from "../../services/auth/sessionRouting";
import { useI18n } from "../../i18n";

/**
 * @param {{ intent?: string, returnUrl?: string | null, registered?: boolean, variant?: 'page' | 'overlay' }} props
 */
export function UnifiedLoginForm({
  intent: intentProp = "merchant",
  returnUrl: returnUrlProp = null,
  registered = false,
  variant = "page",
}) {
  const navigate = useNavigate();
  const { refresh } = useNavigation();
  const location = useLocation();
  const { t } = useI18n();
  const fromState = location.state?.from ? String(location.state.from) : null;
  const rawReturn = returnUrlProp || fromState || null;

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!isMerchantAuthRequired()) {
      navigate("/merchant", { replace: true });
      return;
    }
    const token = getStoredAccessToken();
    if (token) {
      const r = getRoleFromAccessToken(token);
      const mid = getMerchantIdFromAccessToken(token);
      if (mid) setStoredMerchantId(mid);
      else clearStoredMerchantId();
      if (r !== "SYSTEM_ADMIN" && r !== "MERCHANT" && r !== "SUB_MERCHANT" && r !== "CLIENT") {
        clearStoredAccessToken();
        clearStoredMerchantId();
        return;
      }
      if ((r === "MERCHANT" || r === "SUB_MERCHANT") && !mid) {
        clearStoredAccessToken();
        clearStoredMerchantId();
        return;
      }
      const dest = resolvePostLoginDestination({
        returnUrlRaw: rawReturn,
        nextDestination: null,
        role: r,
        intent: intentProp,
        registerUserType: peekPendingRegisterUserType(),
      });
      navigate(dest, { replace: true });
      consumePendingRegisterUserType();
    }
  }, [navigate, rawReturn, intentProp]);

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const data = await api("/auth/login", {
        method: "POST",
        body: JSON.stringify({ username: username.trim(), password }),
      });
      const role = data.role || "";
      if (role !== "MERCHANT" && role !== "SUB_MERCHANT" && role !== "SYSTEM_ADMIN" && role !== "CLIENT") {
        setError(t("loginErrorInvalidRole"));
        setLoading(false);
        return;
      }
      setStoredAccessToken(data.accessToken);
      const mid = getMerchantIdFromAccessToken(data.accessToken);
      if (mid) setStoredMerchantId(mid);
      let me;
      try {
        me = await api("/auth/me");
      } catch (meErr) {
        setError(meErr.message || t("loginErrorVerifyFailed"));
        setLoading(false);
        return;
      }
      if (me.availableContexts && me.availableContexts.length > 1) {
        navigate("/login/context", { replace: true, state: { intent: intentProp, returnUrl: rawReturn } });
        setLoading(false);
        return;
      }
      await refresh();
      const next = resolvePostLoginDestination({
        returnUrlRaw: rawReturn,
        nextDestination: data.nextDestination,
        role,
        intent: intentProp,
        registerUserType: peekPendingRegisterUserType(),
      });
      navigate(next, { replace: true });
      consumePendingRegisterUserType();
    } catch (err) {
      setError(err.message || t("loginErrorGeneric"));
    } finally {
      setLoading(false);
    }
  }

  const registerTo = hrefAuthOverlay(location.pathname, location.search, {
    mode: AUTH_OVERLAY_REGISTER,
    intent: intentProp === "system" || intentProp === "client" ? intentProp : "merchant",
  });

  const cardClass =
    variant === "overlay" ? "merchant-login-card merchant-login-card--overlay" : "merchant-login-card";

  return (
    <div className={cardClass}>
      <p className="merchant-login-eyebrow">{t("loginEyebrowBrand")}</p>
      <h1 id="auth-overlay-login-title">{t("loginTitleUnified")}</h1>
      <p className="merchant-login-lede">{t("loginLeadUnified")}</p>
      {registered && <p className="merchant-login-banner">{t("loginRegisteredBanner")}</p>}
      {error && (
        <p className="merchant-login-error" role="alert" aria-live="assertive">
          {error}
        </p>
      )}
      <form onSubmit={onSubmit} className="merchant-login-form" aria-busy={loading}>
        <label>
          {t("loginUsernameLabel")}
          <input
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder={t("loginUsernamePlaceholder")}
          />
        </label>
        <label>
          {t("loginPasswordLabel")}
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        <button type="submit" className="btn btn-primary merchant-login-submit" disabled={loading}>
          {loading ? t("loginSubmitting") : t("loginSubmit")}
        </button>
      </form>
      <p className="merchant-login-footer">
        <Link to={registerTo}>{t("loginFooterRegister")}</Link>
        <span className="merchant-login-dot"> · </span>
        <Link to="/">{t("loginFooterHome")}</Link>
      </p>
    </div>
  );
}
