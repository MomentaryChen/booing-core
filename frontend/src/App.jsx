import { Link, Navigate, Route, Routes, useLocation, useParams, useSearchParams } from "react-router-dom";
import { lazy, Suspense, useEffect, useState } from "react";
import { SystemDashboard } from "./pages/system/SystemDashboard";
import { SystemProtectedLayout } from "./pages/system/SystemProtectedLayout";
import { SystemUsersPage } from "./pages/system/SystemUsersPage";
import { SystemMerchantsPage } from "./pages/system/SystemMerchantsPage";
import { SystemAuditLogsPage } from "./pages/system/SystemAuditLogsPage";
import { useI18n } from "./i18n";
import { HomeIntroPage } from "./pages/home/HomeIntroPage";
import { MerchantAppointmentsPage } from "./pages/merchant/MerchantAppointmentsPage";
import { MerchantDashboard } from "./pages/merchant/MerchantDashboard";
import { MerchantScheduleSettingsPage } from "./pages/merchant/MerchantScheduleSettingsPage";
import { MerchantInvitationsPage } from "./pages/merchant/MerchantInvitationsPage";
import { MerchantVisibilitySettingsPage } from "./pages/merchant/MerchantVisibilitySettingsPage";
import { MerchantRegisterPage } from "./pages/merchant/MerchantRegisterPage";
import { MerchantLoginPage } from "./pages/merchant/MerchantLoginPage";
import { LoginContextPage } from "./pages/merchant/LoginContextPage";
import { MerchantProtectedLayout } from "./pages/merchant/MerchantProtectedLayout";
import { PublicStorefront } from "./pages/public-store/PublicStorefront";
import { ClientMerchantsPage } from "./pages/client/ClientMerchantsPage";
import { ClientJoinByCodePage } from "./pages/client/ClientJoinByCodePage";
import { ClientJoinedMerchantsPage } from "./pages/client/ClientJoinedMerchantsPage";
import { ClientMerchantDetailPage } from "./pages/client/ClientMerchantDetailPage";
import { ClientProtectedLayout } from "./pages/client/ClientProtectedLayout";
import { SystemTenantBookingOpsPage } from "./pages/system/SystemTenantBookingOpsPage";
import { getStoredAccessToken, clearStoredAccessToken } from "./services/api/client";
import { isMerchantAuthRequired } from "./services/merchant/merchantAuth";
import { clearStoredMerchantId } from "./services/merchant/merchantStorage";
import {
  AUTH_OVERLAY_LOGIN,
  AUTH_OVERLAY_REGISTER,
  hrefAuthOverlay,
  getDefaultPathForIntent,
  inferLoginIntentFromPath,
} from "./services/auth/sessionRouting";
import { ForbiddenPage } from "./pages/ForbiddenPage";
import { AuthOverlay } from "./components/auth/AuthOverlay";
import { WorkspaceLayout } from "./components/navigation/WorkspaceLayout";
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

function TopRightAuthActions() {
  const { t } = useI18n();
  const location = useLocation();
  const [hasMerchantToken, setHasMerchantToken] = useState(() =>
    isMerchantAuthRequired() ? !!getStoredAccessToken() : true
  );

  useEffect(() => {
    setHasMerchantToken(isMerchantAuthRequired() ? !!getStoredAccessToken() : true);
  }, [location.pathname]);

  if (!isMerchantAuthRequired()) {
    return (
      <div className="top-nav-auth">
        <Link
          to={hrefAuthOverlay(location.pathname, location.search, { mode: AUTH_OVERLAY_LOGIN, intent: "merchant" })}
          className="top-nav-minimal-auth-link"
        >
          {t("navMerchantLogin")}
        </Link>
        <Link
          to={hrefAuthOverlay(location.pathname, location.search, { mode: AUTH_OVERLAY_REGISTER, intent: "merchant" })}
          className="top-nav-minimal-auth-link"
        >
          {t("navMerchantRegister")}
        </Link>
      </div>
    );
  }

  if (hasMerchantToken) {
    return (
      <div className="top-nav-auth">
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
      </div>
    );
  }

  return (
    <div className="top-nav-auth">
      <Link
        to={hrefAuthOverlay(location.pathname, location.search, { mode: AUTH_OVERLAY_LOGIN, intent: "merchant" })}
        className="top-nav-minimal-auth-link"
      >
        {t("navMerchantLogin")}
      </Link>
      <Link
        to={hrefAuthOverlay(location.pathname, location.search, { mode: AUTH_OVERLAY_REGISTER, intent: "merchant" })}
        className="top-nav-minimal-auth-link"
      >
        {t("navMerchantRegister")}
      </Link>
    </div>
  );
}

export function App() {
  const { t, locale, setLocale } = useI18n();
  const location = useLocation();
  const isSystemWorkspaceRoute = location.pathname === "/system" || location.pathname.startsWith("/system/");
  const isMerchantWorkspaceRoute = location.pathname === "/merchant" || location.pathname.startsWith("/merchant/");
  const isClientWorkspaceRoute =
    location.pathname === "/client" ||
    location.pathname === "/client/merchants" ||
    location.pathname.startsWith("/client/merchants/");
  const isWorkspaceRoute = isSystemWorkspaceRoute || isMerchantWorkspaceRoute || isClientWorkspaceRoute;
  const mainClassName =
    location.pathname === "/demo/saas-dashboard"
      ? "saas-demo-main"
      : isWorkspaceRoute
        ? "workspace-main"
        : "app-main";

  return (
    <div className="app-shell" id="booking-ui-root">
      <nav className="top-nav">
        <Link to="/" className="brand brand-link">
          {t("brand")}
        </Link>
        <div className="top-nav-right">
          <TopRightAuthActions />
          <button
            type="button"
            className="locale-chip"
            onClick={() => setLocale(locale === "zh-TW" ? "en-US" : "zh-TW")}
          >
            {t("locale")}
          </button>
        </div>
      </nav>
      <AuthOverlay />
      <main className={mainClassName}>
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
          <Route path="/403" element={<ForbiddenPage />} />
          <Route path="/login" element={<UnifiedLoginRoute />} />
          <Route path="/login/context" element={<LoginContextPage />} />
          <Route path="/merchant/login" element={<LegacyLoginRedirect />} />
          <Route path="/system/login" element={<LegacyLoginRedirect />} />
          <Route path="/client/login" element={<LegacyLoginRedirect />} />
          <Route path="/merchant/register" element={<MerchantRegisterPage />} />
          <Route element={<MerchantProtectedLayout />}>
            <Route element={<WorkspaceLayout />}>
              <Route path="/merchant" element={<MerchantDashboard />} />
              <Route path="/merchant/appointments" element={<MerchantAppointmentsPage />} />
              <Route path="/merchant/bookings" element={<MerchantAppointmentsPage />} />
              <Route path="/merchant/settings/schedule" element={<MerchantScheduleSettingsPage />} />
              <Route path="/merchant/settings/visibility" element={<MerchantVisibilitySettingsPage />} />
              <Route path="/merchant/invitations" element={<MerchantInvitationsPage />} />
            </Route>
          </Route>
          <Route element={<ClientProtectedLayout />}>
            <Route element={<WorkspaceLayout />}>
              <Route path="/client" element={<ClientMerchantsPage />} />
              <Route path="/client/merchants" element={<ClientMerchantsPage />} />
              <Route path="/client/merchants/join" element={<ClientJoinByCodePage />} />
              <Route path="/client/merchants/joined" element={<ClientJoinedMerchantsPage />} />
              <Route path="/client/merchants/:merchantId" element={<ClientMerchantDetailPage />} />
            </Route>
          </Route>
          <Route element={<SystemProtectedLayout />}>
            <Route element={<WorkspaceLayout />}>
              <Route path="/system" element={<SystemDashboard />} />
              <Route path="/system/merchants" element={<SystemMerchantsPage />} />
              <Route path="/system/audit-logs" element={<SystemAuditLogsPage />} />
              <Route path="/system/users" element={<SystemUsersPage />} />
              <Route path="/system/tenant-booking-ops" element={<SystemTenantBookingOpsPage />} />
              <Route
                path="/system/tenants/:tenantId/merchants/:merchantId/bookings"
                element={<SystemTenantBookingOpsPage />}
              />
            </Route>
          </Route>
          <Route path="/client/booking/:slug" element={<PublicStorefront />} />
          <Route path="/store/:slug" element={<RedirectStoreToClientBooking />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
