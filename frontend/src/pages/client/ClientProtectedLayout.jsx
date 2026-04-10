import { Navigate, Outlet, useLocation } from "react-router-dom";
import { getStoredAccessToken } from "../../services/api/client";
import { getRoleFromAccessToken } from "../../services/platformRole";
import { isMerchantAuthRequired } from "../../services/merchant/merchantAuth";
import { getUnauthorizedRedirect, hasNamespaceCapability } from "../../services/auth/sessionRouting";
import { useNavigation } from "../../navigation/NavigationContext";
import { useI18n } from "../../i18n";

export function ClientProtectedLayout() {
  const location = useLocation();
  const { t } = useI18n();
  const { routeKeys, loading, error, refresh } = useNavigation();

  if (!isMerchantAuthRequired()) {
    return <Outlet />;
  }

  const token = getStoredAccessToken();
  if (!token) {
    return <Navigate to={getUnauthorizedRedirect({ location, intent: "client" })} replace />;
  }

  if (loading) {
    return (
      <div className="nav-loading" role="status">
        <span>{t("navSidebarLoading")}</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="nav-loading nav-loading--error" role="alert">
        <p>{t("navSidebarError")}</p>
        <button type="button" className="workspace-sidebar__retry" onClick={() => refresh()}>
          {t("navSidebarRetry")}
        </button>
      </div>
    );
  }

  const role = getRoleFromAccessToken(token);
  if (hasNamespaceCapability({ intent: "client", role, routeKeys })) {
    return <Outlet />;
  }
  if (role === "SYSTEM_ADMIN") {
    return <Navigate to="/system" replace />;
  }
  if (role === "MERCHANT" || role === "SUB_MERCHANT") {
    return <Navigate to="/merchant" replace />;
  }

  return <Navigate to={getUnauthorizedRedirect({ location, intent: "client" })} replace />;
}
