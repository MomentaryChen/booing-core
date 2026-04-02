import { Fragment, useCallback, useEffect, useState } from "react";
import { api } from "../../services/api/client";
import { useI18n } from "../../i18n";

function formatMoney(value, locale) {
  const n = Number(value);
  if (Number.isNaN(n)) return "—";
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency: "TWD",
    maximumFractionDigits: 0,
  }).format(n);
}

function RingGauge({ fraction, label, sublabel, stroke, track = "rgba(148,163,184,0.15)" }) {
  const pct = Math.min(100, Math.max(0, fraction * 100));
  const r = 36;
  const c = 2 * Math.PI * r;
  const dash = (pct / 100) * c;
  return (
    <div className="sys-ring">
      <svg className="sys-ring__svg" viewBox="0 0 100 100" aria-hidden>
        <circle className="sys-ring__track" cx="50" cy="50" r={r} fill="none" stroke={track} />
        <circle
          className="sys-ring__arc"
          cx="50"
          cy="50"
          r={r}
          fill="none"
          stroke={stroke}
          strokeDasharray={`${dash} ${c}`}
          transform="rotate(-90 50 50)"
        />
      </svg>
      <div className="sys-ring__label">
        <strong>{label}</strong>
        <span>{sublabel}</span>
      </div>
    </div>
  );
}

function Sparkline({ series, stroke }) {
  if (!series?.length) return null;
  const max = Math.max(...series, 1);
  const w = 360;
  const h = 80;
  const padX = 6;
  const padY = 8;
  const step = series.length > 1 ? (w - 2 * padX) / (series.length - 1) : 0;
  const points = series
    .map((v, i) => {
      const x = padX + i * step;
      const y = h - padY - (v / max) * (h - 2 * padY);
      return `${x},${y}`;
    })
    .join(" ");
  return (
    <svg className="sys-sparkline" viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="none" aria-hidden>
      <polyline className="sys-sparkline__line" points={points} stroke={stroke} fill="none" />
      {series.map((v, i) => {
        const x = padX + i * step;
        const y = h - padY - (v / max) * (h - 2 * padY);
        return <circle key={i} cx={x} cy={y} r="3" className="sys-sparkline__dot" />;
      })}
    </svg>
  );
}

function HeatMap({ heatMap, heatMax, dayLabels, isZh }) {
  const hours = Array.from({ length: 24 }, (_, i) => i);
  if (!heatMap?.length) return <p className="sys-muted">{isZh ? "無本週資料" : "No data for this week."}</p>;

  return (
    <div className="sys-heat-scroll">
      <div className="sys-heat-grid">
        <div className="sys-heat-corner" />
        {dayLabels.map((lab) => (
          <div key={lab} className="sys-heat-day">
            {lab}
          </div>
        ))}
        {hours.map((h) => (
          <Fragment key={h}>
            <div className="sys-heat-hour">{h}</div>
            {[0, 1, 2, 3, 4, 5, 6].map((d) => {
              const v = heatMap[d]?.[h] ?? 0;
              const intensity = heatMax > 0 ? v / heatMax : 0;
              const title = isZh
                ? `${dayLabels[d]} ${h}:00 — ${v} 筆`
                : `${dayLabels[d]} ${h}:00 — ${v} bookings`;
              return (
                <div
                  key={`${d}-${h}`}
                  className="sys-heat-cell"
                  style={{ "--i": intensity }}
                  title={title}
                />
              );
            })}
          </Fragment>
        ))}
      </div>
    </div>
  );
}

export function SystemDashboard() {
  const { locale } = useI18n();
  const isZh = locale === "zh-TW";
  const [cc, setCc] = useState(null);
  const [overview, setOverview] = useState(null);
  const [merchants, setMerchants] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [settings, setSettings] = useState({
    emailTemplate: "",
    smsTemplate: "",
    maintenanceAnnouncement: "",
  });
  const [templateForm, setTemplateForm] = useState({ domainName: "", fieldsJson: "[]" });
  const [error, setError] = useState("");
  const [opsOpen, setOpsOpen] = useState(false);

  const loadCommandCenter = useCallback(async () => {
    const data = await api("/system/command-center");
    setCc(data);
  }, []);

  const loadOperations = useCallback(async () => {
    const [o, m, t, s] = await Promise.all([
      api("/system/overview"),
      api("/system/merchants"),
      api("/system/domain-templates"),
      api("/system/system-settings"),
    ]);
    setOverview(o);
    setMerchants(m);
    setTemplates(t);
    setSettings(s);
  }, []);

  const refreshAll = useCallback(async () => {
    try {
      await Promise.all([loadCommandCenter(), loadOperations()]);
      setError("");
    } catch (e) {
      setError(e.message);
    }
  }, [loadCommandCenter, loadOperations]);

  useEffect(() => {
    refreshAll();
  }, [refreshAll]);

  useEffect(() => {
    const id = setInterval(() => {
      loadCommandCenter().catch(() => {});
    }, 25000);
    return () => clearInterval(id);
  }, [loadCommandCenter]);

  async function toggleMerchant(merchant) {
    await api(`/system/merchants/${merchant.id}/status`, {
      method: "PUT",
      body: JSON.stringify({ active: !merchant.active }),
    });
    refreshAll();
  }

  async function saveServiceLimit(merchantId, serviceLimit) {
    await api(`/system/merchants/${merchantId}/service-limit`, {
      method: "PUT",
      body: JSON.stringify({ serviceLimit: Number(serviceLimit) }),
    });
    refreshAll();
  }

  async function addTemplate(e) {
    e.preventDefault();
    await api("/system/domain-templates", {
      method: "POST",
      body: JSON.stringify(templateForm),
    });
    setTemplateForm({ domainName: "", fieldsJson: "[]" });
    refreshAll();
  }

  async function deleteTemplate(templateId) {
    await api(`/system/domain-templates/${templateId}`, { method: "DELETE" });
    refreshAll();
  }

  async function saveSettings(e) {
    e.preventDefault();
    await api("/system/system-settings", {
      method: "PUT",
      body: JSON.stringify(settings),
    });
    refreshAll();
  }

  const dayLabels = isZh
    ? ["週一", "週二", "週三", "週四", "週五", "週六", "週日"]
    : cc?.heatDayLabels || ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

  const occupancy = cc?.occupancyRate ?? 0;
  const pendingWeek = cc?.pendingActionsThisWeek ?? 0;
  const pendingTotal = cc?.pendingActionsTotal ?? cc?.pendingActions ?? 0;
  const pendingRing = Math.min(1, pendingWeek / 12);

  return (
    <div className="system-command-center">
      <header className="sys-header">
        <div>
          <p className="sys-eyebrow">{isZh ? "系統控制台" : "Command Center"}</p>
          <h1>{isZh ? "營運態勢儀表板" : "Operations overview"}</h1>
          <p className="sys-sub">
            {isZh
              ? "30 秒內掌握預約熱點、人力負荷與待處理事項。"
              : "Staffing decisions from visual density within seconds of login."}
          </p>
        </div>
        <div className="sys-header-actions">
          <button type="button" className="sys-btn sys-btn--ghost" onClick={() => refreshAll()}>
            {isZh ? "重新整理" : "Refresh"}
          </button>
        </div>
      </header>

      {error && <p className="sys-error">{error}</p>}

      <section className="sys-pane sys-metrics">
        <article className="sys-metric sys-metric--lime">
          <span className="sys-metric__k">{isZh ? "今日預約" : "Today's bookings"}</span>
          <strong className="sys-metric__v">{cc?.todayBookings ?? "—"}</strong>
          <span className="sys-metric__h">{isZh ? "本日開始之場次" : "Starts today"}</span>
        </article>
        <article className="sys-metric sys-metric--growth">
          <span className="sys-metric__k">{isZh ? "本週預約" : "Week volume"}</span>
          <strong className="sys-metric__v">{cc?.weekBookings ?? "—"}</strong>
          <span className="sys-metric__h">{isZh ? "本週曆（週一–週日）" : "Mon–Sun window"}</span>
        </article>
        <article className="sys-metric sys-metric--neutral">
          <span className="sys-metric__k">{isZh ? "活躍商戶" : "Active merchants"}</span>
          <strong className="sys-metric__v">{overview?.activeMerchants ?? "—"}</strong>
          <span className="sys-metric__h">{isZh ? "平台啟用中" : "Platform active"}</span>
        </article>
        <article className="sys-metric sys-metric--revenue">
          <span className="sys-metric__k">{isZh ? "今日營收（估）" : "Revenue today"}</span>
          <strong className="sys-metric__v">{cc != null ? formatMoney(cc.revenueToday, locale) : "—"}</strong>
          <span className="sys-metric__h">{isZh ? "未取消預約加總" : "Non-cancelled services"}</span>
        </article>
        <article className="sys-metric sys-metric--ring">
          <RingGauge
            fraction={occupancy / 100}
            label={`${occupancy}%`}
            sublabel={isZh ? "負荷（估）" : "Load (est.)"}
            stroke="var(--sys-lime)"
          />
        </article>
        <article className="sys-metric sys-metric--ring sys-metric--pending">
          <RingGauge
            fraction={pendingRing}
            label={String(pendingWeek)}
            sublabel={isZh ? "本週待審" : "Pending (wk)"}
            stroke="var(--sys-amber)"
          />
        </article>
      </section>

      <div className="sys-split">
        <section className="sys-pane sys-chart-pane">
          <div className="sys-pane-head">
            <h2>{isZh ? "近 7 日預約走勢" : "7-day booking trend"}</h2>
            <span className="sys-chip sys-chip--lime">{isZh ? "量能" : "Volume"}</span>
          </div>
          <Sparkline series={cc?.sparklineWeek || []} stroke="var(--sys-lime-dim)" />
        </section>
        <section className="sys-pane sys-chart-pane">
          <div className="sys-pane-head">
            <h2>{isZh ? "平台摘要" : "Platform pulse"}</h2>
          </div>
          <ul className="sys-mini-stats">
            <li>
              <span>{isZh ? "總預約" : "All bookings"}</span>
              <strong>{overview?.totalBookings ?? "—"}</strong>
            </li>
            <li>
              <span>{isZh ? "商戶數" : "Merchants"}</span>
              <strong>{overview?.totalMerchants ?? "—"}</strong>
            </li>
            <li>
              <span>{isZh ? "領域模板" : "Templates"}</span>
              <strong>{overview?.domainTemplates ?? "—"}</strong>
            </li>
            <li>
              <span>{isZh ? "待審核（全平台）" : "Pending (all)"}</span>
              <strong>{cc != null ? pendingTotal : "—"}</strong>
            </li>
          </ul>
          <p className="sys-footnote">
            {cc?.occupancyNote}
            {cc?.timeZone
              ? isZh
                ? ` 時區：${cc.timeZone}。`
                : ` Time zone: ${cc.timeZone}.`
              : ""}
          </p>
        </section>
      </div>

      <section className="sys-pane sys-heat-pane">
        <div className="sys-pane-head">
          <h2>{isZh ? "本週預約熱力圖（依開始時間）" : "Weekly heat map (start time)"}</h2>
          <span className="sys-chip sys-chip--amber">{isZh ? "尖峰時段" : "Peak hours"}</span>
        </div>
        <HeatMap heatMap={cc?.heatMap} heatMax={cc?.heatMax ?? 0} dayLabels={dayLabels} isZh={isZh} />
      </section>

      <section className="sys-pane sys-feed-pane">
        <div className="sys-pane-head">
          <h2>{isZh ? "即時動態" : "Live feed"}</h2>
          <span className="sys-chip">{isZh ? "輪詢更新" : "Polling"}</span>
        </div>
        <div className="sys-feed" role="log" aria-live="polite">
          {(cc?.liveFeed || []).map((row) => (
            <div
              key={row.bookingId}
              className={`sys-feed__row sys-feed__row--${String(row.status || "").toLowerCase()}`}
            >
              <div className="sys-feed__main">
                <strong>{row.merchantName}</strong>
                <span className="sys-feed__meta">
                  {row.serviceName} · {row.customerName}
                </span>
              </div>
              <div className="sys-feed__side">
                <time dateTime={row.startAt}>{row.startAt?.replace("T", " ")?.slice(0, 16)}</time>
                <span className="sys-feed__status">{row.status}</span>
              </div>
            </div>
          ))}
          {!(cc?.liveFeed || []).length && (
            <p className="sys-muted sys-feed__empty">{isZh ? "尚無預約紀錄。" : "No bookings yet."}</p>
          )}
        </div>
      </section>

      <section className="sys-ops">
        <button type="button" className="sys-ops-toggle" onClick={() => setOpsOpen((v) => !v)}>
          {isZh ? "平台營運設定" : "Platform operations"}
          <span className="sys-ops-chevron">{opsOpen ? "▾" : "▸"}</span>
        </button>
        {opsOpen && (
          <div className="sys-ops-body">
            <section className="sys-pane sys-ops-card">
              <div className="sys-pane-head">
                <h2>{isZh ? "商戶生命週期" : "Merchant lifecycle"}</h2>
              </div>
              <ul className="sys-list">
                {merchants.map((m) => (
                  <li key={m.id}>
                    <div>
                      <strong>{m.name}</strong>
                      <span className="sys-muted">
                        {m.slug} · {m.active ? (isZh ? "啟用" : "Active") : (isZh ? "停用" : "Off")} · limit{" "}
                        {m.serviceLimit}
                      </span>
                    </div>
                    <div className="sys-inline-actions">
                      <button type="button" className="sys-btn" onClick={() => toggleMerchant(m)}>
                        {m.active ? (isZh ? "停用" : "Deactivate") : (isZh ? "啟用" : "Activate")}
                      </button>
                      <button type="button" className="sys-btn" onClick={() => saveServiceLimit(m.id, m.serviceLimit + 5)}>
                        +5
                      </button>
                    </div>
                  </li>
                ))}
                {!merchants.length && <li className="sys-muted">{isZh ? "無商戶" : "No merchants"}</li>}
              </ul>
            </section>

            <section className="sys-pane sys-ops-card">
              <div className="sys-pane-head">
                <h2>{isZh ? "領域模板" : "Domain templates"}</h2>
              </div>
              <form onSubmit={addTemplate} className="sys-form">
                <label>
                  {isZh ? "領域名稱" : "Domain name"}
                  <input
                    value={templateForm.domainName}
                    onChange={(e) => setTemplateForm((v) => ({ ...v, domainName: e.target.value }))}
                  />
                </label>
                <label>
                  JSON
                  <input
                    value={templateForm.fieldsJson}
                    onChange={(e) => setTemplateForm((v) => ({ ...v, fieldsJson: e.target.value }))}
                  />
                </label>
                <button type="submit" className="sys-btn sys-btn--primary">
                  {isZh ? "新增" : "Add"}
                </button>
              </form>
              <ul className="sys-list">
                {templates.map((t) => (
                  <li key={t.id}>
                    <strong>{t.domainName}</strong>
                    <span className="sys-muted">{t.fieldsJson}</span>
                    <button type="button" className="sys-btn sys-btn--danger" onClick={() => deleteTemplate(t.id)}>
                      {isZh ? "刪除" : "Delete"}
                    </button>
                  </li>
                ))}
              </ul>
            </section>

            <section className="sys-pane sys-ops-card">
              <div className="sys-pane-head">
                <h2>{isZh ? "全域通知" : "Global notifications"}</h2>
              </div>
              <form onSubmit={saveSettings} className="sys-form sys-form--stack">
                <label className="sys-form-full">
                  Email
                  <input
                    value={settings.emailTemplate || ""}
                    onChange={(e) => setSettings((v) => ({ ...v, emailTemplate: e.target.value }))}
                  />
                </label>
                <label className="sys-form-full">
                  SMS
                  <input
                    value={settings.smsTemplate || ""}
                    onChange={(e) => setSettings((v) => ({ ...v, smsTemplate: e.target.value }))}
                  />
                </label>
                <label className="sys-form-full">
                  {isZh ? "維護公告" : "Maintenance"}
                  <input
                    value={settings.maintenanceAnnouncement || ""}
                    onChange={(e) => setSettings((v) => ({ ...v, maintenanceAnnouncement: e.target.value }))}
                  />
                </label>
                <button type="submit" className="sys-btn sys-btn--primary">
                  {isZh ? "儲存" : "Save"}
                </button>
              </form>
            </section>
          </div>
        )}
      </section>
    </div>
  );
}
