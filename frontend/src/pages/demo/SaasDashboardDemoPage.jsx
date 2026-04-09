import { DashboardCard } from "../../components/saas-dashboard/DashboardCard";
import { DashboardShell } from "../../components/saas-dashboard/DashboardShell";
import { KpiRow } from "../../components/saas-dashboard/KpiRow";
import {
  kpiMetrics,
  recentEvents,
  trafficSeries,
} from "../../components/saas-dashboard/mockData";
import { RecentEventsTable } from "../../components/saas-dashboard/RecentEventsTable";
import { TrafficLineChart } from "../../components/saas-dashboard/TrafficLineChart";

/**
 * Standalone SaaS dashboard UI demo (Stripe-inspired dark theme, Tailwind).
 * Route: /demo/saas-dashboard — mock data only; not wired to Booking Core APIs.
 */
export function SaasDashboardDemoPage() {
  return (
    <div id="saas-dashboard-root" className="min-h-screen text-slate-100">
      <DashboardShell title="Overview">
        <div className="grid grid-cols-1 gap-6">
          <KpiRow items={kpiMetrics} />

          <DashboardCard title="Traffic" subtitle="Last 7 days — demo series">
            <TrafficLineChart data={trafficSeries} />
          </DashboardCard>

          <DashboardCard title="Latest events" subtitle="Webhook-style activity (sample)">
            <RecentEventsTable rows={recentEvents} />
          </DashboardCard>
        </div>
      </DashboardShell>
    </div>
  );
}
