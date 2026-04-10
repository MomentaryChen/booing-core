import { Link, useLocation } from "react-router-dom";
import { useEffect, useState } from "react";
import { ArrowRight, CalendarDays, Layers, SlidersHorizontal } from "lucide-react";
import { api } from "../../services/api/client";
import { getStoredMerchantId } from "../../services/merchant/merchantStorage";
import { useI18n } from "../../i18n";
import { DashboardPageShell } from "../../components/shell/DashboardPageShell";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../components/ui/card";
import { buttonVariants } from "../../components/ui/button";
import { AppointmentsModuleArt, ScheduleModuleArt } from "../../components/illustrations/ModuleThumbnails";
import { cn } from "../../lib/cn";
import { AUTH_OVERLAY_REGISTER, hrefAuthOverlay } from "../../services/auth/sessionRouting";

function StatTile({ icon: Icon, label, value }) {
  return (
    <div className="flex items-center gap-3 rounded-xl border border-slate-200/80 bg-white/90 px-4 py-3 shadow-sm">
      <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
        <Icon className="size-5" aria-hidden />
      </span>
      <div className="min-w-0">
        <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
        <p className="text-xl font-semibold tabular-nums text-slate-900">{value}</p>
      </div>
    </div>
  );
}

function ModuleCard({ to, title, description, cta, thumb: Thumb, thumbAlt }) {
  return (
    <Card className="flex h-full flex-col overflow-hidden transition-shadow hover:shadow-md">
      <div
        className="relative aspect-video w-full shrink-0 overflow-hidden bg-slate-100"
        role="img"
        aria-label={thumbAlt}
      >
        <Thumb className="h-full w-full opacity-90 [&>svg]:h-full [&>svg]:w-full" />
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-white/40 to-transparent" aria-hidden />
      </div>
      <CardHeader className="border-0 pb-2">
        <CardTitle className="text-base">{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="mt-auto pt-0">
        <Link
          to={to}
          className={cn(buttonVariants({ variant: "default", size: "sm" }), "w-full sm:w-auto")}
        >
          {cta}
          <ArrowRight className="size-4" aria-hidden />
        </Link>
      </CardContent>
    </Card>
  );
}

export function MerchantDashboard() {
  const { t } = useI18n();
  const location = useLocation();
  const [stats, setStats] = useState({ bookings: 0, services: 0, exceptions: 0 });
  const merchantId = getStoredMerchantId();

  useEffect(() => {
    Promise.all([
      api(`/merchant/${merchantId}/bookings`),
      api(`/merchant/${merchantId}/services`),
      api(`/merchant/${merchantId}/availability-exceptions`),
    ]).then(([b, s, ex]) => setStats({ bookings: b.length, services: s.length, exceptions: ex.length }));
  }, [merchantId]);

  return (
    <div className="mx-auto max-w-5xl">
      <DashboardPageShell>
        <header className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div className="max-w-xl space-y-2">
            <h1 className="text-2xl font-semibold tracking-tight text-slate-900 sm:text-3xl">
              {t("merchantDashboardTitle")}
            </h1>
            <p className="text-sm leading-relaxed text-slate-600 sm:text-base">{t("merchantDashboardSubtitle")}</p>
          </div>
        </header>

        <div className="mb-8 grid gap-3 sm:grid-cols-3">
          <StatTile icon={CalendarDays} label={t("merchantDashboardStatBookings")} value={stats.bookings} />
          <StatTile icon={Layers} label={t("merchantDashboardStatServices")} value={stats.services} />
          <StatTile icon={SlidersHorizontal} label={t("merchantDashboardStatExceptions")} value={stats.exceptions} />
        </div>

        <section className="space-y-4">
          <div className="flex flex-col gap-1 border-b border-slate-200/80 pb-3">
            <h2 className="text-lg font-semibold text-slate-900">{t("merchantDashboardWorkspaceTitle")}</h2>
            <p className="text-sm text-slate-600">
              <span>{t("merchantDashboardRegisterLine")}</span>{" "}
              <Link
                to={hrefAuthOverlay(location.pathname, location.search, {
                  mode: AUTH_OVERLAY_REGISTER,
                  intent: "merchant",
                })}
                className="font-medium text-indigo-600 hover:text-indigo-700"
              >
                {t("merchantDashboardRegisterCta")}
              </Link>
            </p>
          </div>

          <div className="grid gap-6 md:grid-cols-2">
            <ModuleCard
              to="/merchant/appointments"
              title={t("merchantDashboardCardAppointmentsTitle")}
              description={t("merchantDashboardCardAppointmentsDesc")}
              cta={t("merchantDashboardCardAppointmentsCta")}
              thumb={AppointmentsModuleArt}
              thumbAlt={t("merchantDashboardAltAppointmentsThumb")}
            />
            <ModuleCard
              to="/merchant/settings/schedule"
              title={t("merchantDashboardCardScheduleTitle")}
              description={t("merchantDashboardCardScheduleDesc")}
              cta={t("merchantDashboardCardScheduleCta")}
              thumb={ScheduleModuleArt}
              thumbAlt={t("merchantDashboardAltScheduleThumb")}
            />
          </div>
          <div className="pt-2">
            <Link
              to="/merchant/invitations"
              className={cn(buttonVariants({ variant: "outline" }), "inline-flex")}
            >
              {t("merchantDashboardInvitationsCta")}
            </Link>
            <Link
              to="/merchant/settings/visibility"
              className={cn(buttonVariants({ variant: "outline" }), "ml-3 inline-flex")}
            >
              {t("merchantDashboardVisibilityCta")}
            </Link>
          </div>
        </section>
      </DashboardPageShell>
    </div>
  );
}
