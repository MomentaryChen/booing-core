import { Link, Navigate, Route, Routes, useLocation, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { SystemDashboard } from "./pages/system/SystemDashboard";
import { SystemProtectedLayout } from "./pages/system/SystemProtectedLayout";
import { useI18n } from "./i18n";
import { HomeIntroPage } from "./pages/home/HomeIntroPage";
import { MerchantAppointmentsPage } from "./pages/merchant/MerchantAppointmentsPage";
import { MerchantDashboard } from "./pages/merchant/MerchantDashboard";
import { MerchantScheduleSettingsPage } from "./pages/merchant/MerchantScheduleSettingsPage";
import { MerchantRegisterPage } from "./pages/merchant/MerchantRegisterPage";
import { MerchantLoginPage } from "./pages/merchant/MerchantLoginPage";
import { MerchantProtectedLayout } from "./pages/merchant/MerchantProtectedLayout";
import { PublicStorefront } from "./pages/public-store/PublicStorefront";
import { ClientTodoPage } from "./pages/client/ClientTodoPage";
import { getStoredAccessToken, clearStoredAccessToken } from "./services/api/client";
import { isMerchantAuthRequired } from "./services/merchant/merchantAuth";
import { getRoleFromAccessToken } from "./services/platformRole";
import { clearStoredMerchantId } from "./services/merchant/merchantStorage";
import { useNavigation } from "./navigation/NavigationContext";
import { ROUTE_KEYS } from "./navigation/routeKeys";

function RedirectStoreToClientBooking() {
  const { slug } = useParams();
  return <Navigate to={`/client/booking/${slug}`} replace />;
}

function MerchantNavExtras() {
  const { t } = useI18n();
  const location = useLocation();
  const { hasRouteKey, loading } = useNavigation();
  const [hasMerchantToken, setHasMerchantToken] = useState(() =>
    isMerchantAuthRequired() ? !!getStoredAccessToken() : true
  );

  useEffect(() => {
    setHasMerchantToken(isMerchantAuthRequired() ? !!getStoredAccessToken() : true);
  }, [location.pathname]);

  const platformRole = getRoleFromAccessToken(getStoredAccessToken());
  const showMerchantLink =
    loading ||
    hasRouteKey(ROUTE_KEYS.MERCHANT_DASHBOARD) ||
    platformRole === "MERCHANT" ||
    platformRole === "SUB_MERCHANT" ||
    platformRole === "SYSTEM_ADMIN";

  if (!isMerchantAuthRequired()) {
    return (
      <>
        <Link to="/merchant">{t("navMerchant")}</Link>
        <Link to="/merchant/register">{t("navMerchantRegister")}</Link>
      </>
    );
  }

  return (
    <>
      {hasMerchantToken ? (
        <>
          {showMerchantLink && <Link to="/merchant">{t("navMerchant")}</Link>}
          <button
            type="button"
            className="nav-text-btn"
            onClick={() => {
              clearStoredAccessToken();
              clearStoredMerchantId();
              setHasMerchantToken(false);
              window.location.assign("/merchant/login");
            }}
          >
            {t("navMerchantLogout")}
          </button>
        </>
      ) : (
        <>
          <Link to="/merchant/login">{t("navMerchantLogin")}</Link>
          <Link to="/merchant/register">{t("navMerchantRegister")}</Link>
        </>
      )}
    </>
  );
}

export function App() {
  const { t, locale, setLocale } = useI18n();
  const location = useLocation();
  const isHome = location.pathname === "/";
  const { hasRouteKey, loading } = useNavigation();
  const authRequired = isMerchantAuthRequired();
  const tokenPresent = !!getStoredAccessToken();

  const showAdminNav =
    authRequired && tokenPresent && !loading && hasRouteKey(ROUTE_KEYS.SYSTEM_DASHBOARD);
  const showClientNav =
    !authRequired || !tokenPresent || (!loading && hasRouteKey(ROUTE_KEYS.CLIENT_TODO));
  const showStoreNav =
    !authRequired || !tokenPresent || (!loading && hasRouteKey(ROUTE_KEYS.STORE_PUBLIC));

  return (
    <div className="app-shell">
      <nav className={`top-nav${isHome ? " top-nav--minimal" : ""}`}>
        <Link to="/" className="brand brand-link">
          {t("brand")}
        </Link>
        {!isHome && (
          <div className="nav-links">
            {showAdminNav && <Link to="/system">{t("navAdmin")}</Link>}
            <MerchantNavExtras />
            {showClientNav && <Link to="/client">{t("navClient")}</Link>}
            {showStoreNav && <Link to="/client/booking/demo-merchant">{t("navStore")}</Link>}
          </div>
        )}
        <button
          type="button"
          className="locale-chip"
          onClick={() => setLocale(locale === "zh-TW" ? "en-US" : "zh-TW")}
        >
          {t("locale")}
        </button>
      </nav>
      <main className="app-main">
        <Routes>
          <Route path="/" element={<HomeIntroPage />} />
          <Route element={<SystemProtectedLayout />}>
            <Route path="/system" element={<SystemDashboard />} />
          </Route>
          <Route path="/merchant/login" element={<MerchantLoginPage />} />
          <Route path="/merchant/register" element={<MerchantRegisterPage />} />
          <Route element={<MerchantProtectedLayout />}>
            <Route path="/merchant" element={<MerchantDashboard />} />
            <Route path="/merchant/appointments" element={<MerchantAppointmentsPage />} />
            <Route path="/merchant/settings/schedule" element={<MerchantScheduleSettingsPage />} />
          </Route>
          <Route path="/client" element={<ClientTodoPage />} />
          <Route path="/client/booking/:slug" element={<PublicStorefront />} />
          <Route path="/store/:slug" element={<RedirectStoreToClientBooking />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
