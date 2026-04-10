import { Navigate, Outlet, useLocation } from "react-router-dom";
import { getStoredAccessToken } from "../../services/api/client";
import { getRoleFromAccessToken } from "../../services/platformRole";
import { isMerchantAuthRequired } from "../../services/merchant/merchantAuth";
import { useNavigation } from "../../navigation/NavigationContext";
import { getUnauthorizedRedirect, hasNamespaceCapability } from "../../services/auth/sessionRouting";
import { useI18n } from "../../i18n";

export function SystemProtectedLayout() {
  const location = useLocation();
  const { t } = useI18n();
  const { routeKeys, loading, error, refresh } = useNavigation();

  if (!isMerchantAuthRequired()) {
    return <Outlet />;
  }

  const token = getStoredAccessToken();
  if (!token) {
    return <Navigate to={getUnauthorizedRedirect({ location, intent: "system" })} replace />;
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
  if (hasNamespaceCapability({ intent: "system", role, routeKeys })) {
    return <Outlet />;
  }

  return <Navigate to="/403" replace />;
}
