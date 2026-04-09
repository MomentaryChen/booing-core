import { Navigate, Outlet, useLocation } from "react-router-dom";
import { getStoredAccessToken } from "../../services/api/client";
import { getRoleFromAccessToken } from "../../services/platformRole";
import { isMerchantAuthRequired } from "../../services/merchant/merchantAuth";
import { getUnauthorizedRedirect, hasNamespaceCapability } from "../../services/auth/sessionRouting";

export function ClientProtectedLayout() {
  const location = useLocation();

  if (!isMerchantAuthRequired()) {
    return <Outlet />;
  }

  const token = getStoredAccessToken();
  if (!token) {
    return <Navigate to={getUnauthorizedRedirect({ location, intent: "client" })} replace />;
  }

  const role = getRoleFromAccessToken(token);
  if (hasNamespaceCapability({ intent: "client", role })) {
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
