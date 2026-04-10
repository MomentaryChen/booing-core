import { useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "../../services/api/client";
import { useI18n } from "../../i18n";

export function SystemTenantBookingOpsPage() {
  const { t } = useI18n();
  const params = useParams();
  const [tenantId, setTenantId] = useState(params.tenantId || "");
  const [merchantId, setMerchantId] = useState(params.merchantId || "");
  const [bookings, setBookings] = useState([]);
  const [eventByBooking, setEventByBooking] = useState({});
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");

  async function loadBookings(e) {
    e.preventDefault();
    try {
      const data = await api(`/system/tenants/${tenantId}/merchants/${merchantId}/bookings`);
      setBookings(data || []);
      setError("");
    } catch (e2) {
      setError(e2.message);
    }
  }

  async function transitionBooking(bookingId) {
    const event = eventByBooking[bookingId] || "CHECK_IN";
    try {
      const res = await api(`/system/tenants/${tenantId}/bookings/${bookingId}/transitions`, {
        method: "POST",
        body: JSON.stringify({ merchantId: Number(merchantId), event, reason: "system-ops-ui" }),
      });
      setInfo(`${t("systemTenantOpsUpdated")} #${res.bookingId}`);
      const data = await api(`/system/tenants/${tenantId}/merchants/${merchantId}/bookings`);
      setBookings(data || []);
      setError("");
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <div className="mx-auto max-w-5xl">
      <section className="mb-6 rounded-xl border border-slate-200 bg-white p-4">
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">{t("systemTenantOpsTitle")}</h1>
        <p className="mt-1 text-sm text-slate-600">{t("systemTenantOpsSubtitle")}</p>
      </section>

      {error && <p className="mb-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}
      {info && <p className="mb-4 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{info}</p>}

      <section className="mb-6 rounded-xl border border-slate-200 bg-white p-4">
        <form className="grid gap-3 sm:grid-cols-3" onSubmit={loadBookings}>
          <input
            value={tenantId}
            onChange={(e) => setTenantId(e.target.value)}
            placeholder={t("systemTenantOpsTenantId")}
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <input
            value={merchantId}
            onChange={(e) => setMerchantId(e.target.value)}
            placeholder={t("systemTenantOpsMerchantId")}
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <button type="submit" className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white">
            {t("systemTenantOpsLoad")}
          </button>
        </form>
      </section>

      <section className="rounded-xl border border-slate-200 bg-white p-4">
        <h2 className="mb-3 text-lg font-semibold text-slate-900">{t("systemTenantOpsBookings")}</h2>
        <div className="space-y-2">
          {bookings.map((b) => (
            <article key={b.id} className="rounded-md border border-slate-200 p-3">
              <p className="text-sm font-semibold text-slate-900">
                #{b.id} · {b.status} · {b.customerName}
              </p>
              <p className="text-xs text-slate-500">{b.startAt}</p>
              <div className="mt-2 flex flex-wrap gap-2">
                <select
                  value={eventByBooking[b.id] || "CHECK_IN"}
                  onChange={(e) => setEventByBooking((v) => ({ ...v, [b.id]: e.target.value }))}
                  className="rounded-md border border-slate-300 px-2 py-1 text-xs"
                >
                  <option value="CHECK_IN">CHECK_IN</option>
                  <option value="COMPLETE">COMPLETE</option>
                  <option value="CANCEL">CANCEL</option>
                  <option value="MARK_NO_SHOW">MARK_NO_SHOW</option>
                </select>
                <button
                  type="button"
                  onClick={() => transitionBooking(b.id)}
                  className="rounded-md border border-indigo-300 px-3 py-1 text-xs text-indigo-700"
                >
                  {t("systemTenantOpsApply")}
                </button>
              </div>
            </article>
          ))}
          {!bookings.length && <p className="text-sm text-slate-500">{t("systemTenantOpsEmpty")}</p>}
        </div>
      </section>
    </div>
  );
}
