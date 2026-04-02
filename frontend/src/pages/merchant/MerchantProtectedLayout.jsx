import { Navigate, Outlet, useLocation } from "react-router-dom";
import { getStoredAccessToken } from "../../services/api/client";
import { getMerchantIdFromAccessToken, getRoleFromAccessToken } from "../../services/platformRole";
import { isMerchantAuthRequired } from "../../services/merchant/merchantAuth";
import { clearStoredMerchantId, getStoredMerchantId, setStoredMerchantId } from "../../services/merchant/merchantStorage";
import { useNavigation } from "../../navigation/NavigationContext";

export function MerchantProtectedLayout() {
  const location = useLocation();
  const { routeKeys, loading } = useNavigation();

  if (!isMerchantAuthRequired()) {
    return <Outlet />;
  }

  const token = getStoredAccessToken();
  if (!token) {
    return <Navigate to="/merchant/login" replace state={{ from: location.pathname }} />;
  }
  const role = getRoleFromAccessToken(token);
  if (role === "SYSTEM_ADMIN") {
    return <Navigate to="/system" replace />;
  }
  if (role !== "MERCHANT" && role !== "SUB_MERCHANT") {
    clearStoredMerchantId();
    return <Navigate to="/merchant/login" replace state={{ from: location.pathname }} />;
  }
  const merchantIdFromToken = getMerchantIdFromAccessToken(token);
  if (!merchantIdFromToken) {
    clearStoredMerchantId();
    return <Navigate to="/merchant/login" replace state={{ from: location.pathname }} />;
  }
  const storedMerchantId = getStoredMerchantId();
  if (storedMerchantId !== merchantIdFromToken) {
    setStoredMerchantId(merchantIdFromToken);
  }

  if (loading) {
    return (
      <div className="nav-loading" role="status">
        <span>Loading…</span>
      </div>
    );
  }

  const canMerchant = routeKeys.some((k) => k.startsWith("nav.merchant"));
  if (canMerchant) {
    return <Outlet />;
  }

  if (role === "MERCHANT" || role === "SUB_MERCHANT") {
    return <Outlet />;
  }

  return <Navigate to="/" replace />;
}
