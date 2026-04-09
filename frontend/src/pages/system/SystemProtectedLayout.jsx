import { Navigate, Outlet, useLocation } from "react-router-dom";
import { getStoredAccessToken } from "../../services/api/client";
import { getRoleFromAccessToken } from "../../services/platformRole";
import { isMerchantAuthRequired } from "../../services/merchant/merchantAuth";
import { useNavigation } from "../../navigation/NavigationContext";
import { getUnauthorizedRedirect, hasNamespaceCapability } from "../../services/auth/sessionRouting";

export function SystemProtectedLayout() {
  const location = useLocation();
  const { routeKeys, loading } = useNavigation();

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
        <span>Loading…</span>
      </div>
    );
  }

  const role = getRoleFromAccessToken(token);
  if (hasNamespaceCapability({ intent: "system", role, routeKeys })) {
    return <Outlet />;
  }

  return <Navigate to="/403" replace />;
}
