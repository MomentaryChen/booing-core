import { Link, Navigate, Route, Routes, useLocation, useParams, useSearchParams } from "react-router-dom";
import { lazy, Suspense, useEffect, useState } from "react";
import { SystemDashboard } from "./pages/system/SystemDashboard";
import { SystemProtectedLayout } from "./pages/system/SystemProtectedLayout";
import { useI18n } from "./i18n";
import { HomeIntroPage } from "./pages/home/HomeIntroPage";
import { MerchantAppointmentsPage } from "./pages/merchant/MerchantAppointmentsPage";
import { MerchantDashboard } from "./pages/merchant/MerchantDashboard";
import { MerchantScheduleSettingsPage } from "./pages/merchant/MerchantScheduleSettingsPage";
import { MerchantRegisterPage } from "./pages/merchant/MerchantRegisterPage";
import { MerchantLoginPage } from "./pages/merchant/MerchantLoginPage";
import { LoginContextPage } from "./pages/merchant/LoginContextPage";
import { MerchantProtectedLayout } from "./pages/merchant/MerchantProtectedLayout";
import { PublicStorefront } from "./pages/public-store/PublicStorefront";
import { ClientTodoPage } from "./pages/client/ClientTodoPage";
import { ClientProtectedLayout } from "./pages/client/ClientProtectedLayout";
import { getStoredAccessToken, clearStoredAccessToken } from "./services/api/client";
import { isMerchantAuthRequired } from "./services/merchant/merchantAuth";
import { getRoleFromAccessToken } from "./services/platformRole";
import { clearStoredMerchantId } from "./services/merchant/merchantStorage";
import { useNavigation } from "./navigation/NavigationContext";
import { ROUTE_KEYS } from "./navigation/routeKeys";
import {
  AUTH_OVERLAY_LOGIN,
  AUTH_OVERLAY_REGISTER,
  getDefaultPathForIntent,
  hrefAuthOverlay,
  inferLoginIntentFromPath,
} from "./services/auth/sessionRouting";
import { ForbiddenPage } from "./pages/ForbiddenPage";
import { AuthOverlay } from "./components/auth/AuthOverlay";
const SaasDashboardDemoPage = lazy(() =>
  import("./pages/demo/SaasDashboardDemoPage").then((m) => ({ default: m.SaasDashboardDemoPage }))
);

function RedirectStoreToClientBooking() {
  const { slug } = useParams();
  return <Navigate to={`/client/booking/${slug}`} replace />;
}

function LegacyLoginRedirect() {
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const returnUrlParam = params.get("returnUrl");
  const registered = params.get("registered");
  const stateFrom = location.state?.from ? String(location.state.from) : null;
  const intent = inferLoginIntentFromPath(location.pathname);
  const fallback = getDefaultPathForIntent(intent);
  const resolvedReturnUrl = returnUrlParam || stateFrom || fallback;
  const nextParams = new URLSearchParams();
  nextParams.set("intent", intent);
  nextParams.set("returnUrl", resolvedReturnUrl);
  if (registered === "1") {
    nextParams.set("registered", "1");
  }
  return <Navigate to={`/login?${nextParams.toString()}`} replace />;
}

function UnifiedLoginRoute() {
  const [searchParams] = useSearchParams();
  const rawIntent = searchParams.get("intent");
  const intent = rawIntent === "system" || rawIntent === "client" ? rawIntent : "merchant";
  const returnUrl = searchParams.get("returnUrl");
  return (
    <MerchantLoginPage
      intent={intent}
      returnUrl={returnUrl}
    />
  );
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
        <Link
          to={hrefAuthOverlay(location.pathname, location.search, { mode: AUTH_OVERLAY_REGISTER, intent: "merchant" })}
        >
          {t("navMerchantRegister")}
        </Link>
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
              window.location.assign("/?auth=login&intent=merchant");
            }}
          >
            {t("navMerchantLogout")}
          </button>
        </>
      ) : (
        <>
          <Link to={hrefAuthOverlay(location.pathname, location.search, { mode: AUTH_OVERLAY_LOGIN, intent: "merchant" })}>
            {t("navMerchantLogin")}
          </Link>
          <Link
            to={hrefAuthOverlay(location.pathname, location.search, { mode: AUTH_OVERLAY_REGISTER, intent: "merchant" })}
          >
            {t("navMerchantRegister")}
          </Link>
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

  const isSaasDashboardDemo = location.pathname === "/demo/saas-dashboard";

  return (
    <div
      className={isSaasDashboardDemo ? "saas-demo-root" : "app-shell"}
      id={isSaasDashboardDemo ? undefined : "booking-ui-root"}
    >
      {!isSaasDashboardDemo && (
      <nav className={`top-nav${isHome ? " top-nav--minimal" : ""}`}>
        <Link to="/" className="brand brand-link">
          {t("brand")}
        </Link>
        {isHome ? (
          <div className="top-nav-minimal-trail">
            <div className="top-nav-minimal-auth">
              <Link
                to={hrefAuthOverlay("/", location.search, { mode: AUTH_OVERLAY_LOGIN, intent: "merchant" })}
                className="top-nav-minimal-auth-link"
              >
                {t("navMerchantLogin")}
              </Link>
              <Link
                to={hrefAuthOverlay("/", location.search, { mode: AUTH_OVERLAY_REGISTER, intent: "merchant" })}
                className="top-nav-minimal-auth-link"
              >
                {t("navMerchantRegister")}
              </Link>
            </div>
            <button
              type="button"
              className="locale-chip"
              onClick={() => setLocale(locale === "zh-TW" ? "en-US" : "zh-TW")}
            >
              {t("locale")}
            </button>
          </div>
        ) : (
          <>
            <div className="nav-links">
              {showAdminNav && <Link to="/system">{t("navAdmin")}</Link>}
              <MerchantNavExtras />
              {showClientNav && <Link to="/client">{t("navClient")}</Link>}
              {showStoreNav && <Link to="/client/booking/demo-merchant">{t("navStore")}</Link>}
            </div>
            <button
              type="button"
              className="locale-chip"
              onClick={() => setLocale(locale === "zh-TW" ? "en-US" : "zh-TW")}
            >
              {t("locale")}
            </button>
          </>
        )}
      </nav>
      )}
      {!isSaasDashboardDemo && <AuthOverlay />}
      <main className={isSaasDashboardDemo ? "saas-demo-main" : "app-main"}>
        <Routes>
          <Route
            path="/demo/saas-dashboard"
            element={
              <Suspense
                fallback={
                  <div className="flex min-h-screen items-center justify-center bg-slate-950 text-slate-500">
                    Loading…
                  </div>
                }
              >
                <SaasDashboardDemoPage />
              </Suspense>
            }
          />
          <Route path="/" element={<HomeIntroPage />} />
          <Route element={<SystemProtectedLayout />}>
            <Route path="/system" element={<SystemDashboard />} />
          </Route>
          <Route path="/403" element={<ForbiddenPage />} />
          <Route path="/login" element={<UnifiedLoginRoute />} />
          <Route path="/login/context" element={<LoginContextPage />} />
          <Route path="/merchant/login" element={<LegacyLoginRedirect />} />
          <Route path="/system/login" element={<LegacyLoginRedirect />} />
          <Route path="/client/login" element={<LegacyLoginRedirect />} />
          <Route path="/merchant/register" element={<MerchantRegisterPage />} />
          <Route element={<MerchantProtectedLayout />}>
            <Route path="/merchant" element={<MerchantDashboard />} />
            <Route path="/merchant/appointments" element={<MerchantAppointmentsPage />} />
            <Route path="/merchant/settings/schedule" element={<MerchantScheduleSettingsPage />} />
          </Route>
          <Route element={<ClientProtectedLayout />}>
            <Route path="/client" element={<ClientTodoPage />} />
          </Route>
          <Route path="/client/booking/:slug" element={<PublicStorefront />} />
          <Route path="/store/:slug" element={<RedirectStoreToClientBooking />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
