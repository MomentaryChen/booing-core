import { Navigate, Outlet, useLocation } from "react-router-dom";
import { getStoredAccessToken } from "../../services/api/client";
import { getRoleFromAccessToken } from "../../services/platformRole";
import { isMerchantAuthRequired } from "../../services/merchant/merchantAuth";
import { useNavigation } from "../../navigation/NavigationContext";
import { ROUTE_KEYS } from "../../navigation/routeKeys";

export function SystemProtectedLayout() {
  const location = useLocation();
  const { hasRouteKey, loading } = useNavigation();

  if (!isMerchantAuthRequired()) {
    return <Outlet />;
  }

  const token = getStoredAccessToken();
  if (!token) {
    return <Navigate to="/merchant/login" replace state={{ from: location.pathname }} />;
  }

  if (loading) {
    return (
      <div className="nav-loading" role="status">
        <span>Loading…</span>
      </div>
    );
  }

  if (hasRouteKey(ROUTE_KEYS.SYSTEM_DASHBOARD)) {
    return <Outlet />;
  }

  const role = getRoleFromAccessToken(token);
  if (role === "SYSTEM_ADMIN") {
    return <Outlet />;
  }

  return <Navigate to="/merchant" replace />;
}
