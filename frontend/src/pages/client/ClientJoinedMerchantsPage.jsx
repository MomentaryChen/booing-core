import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../../services/api/client";
import { useI18n } from "../../i18n";
import { DashboardPageShell } from "../../components/shell/DashboardPageShell";

export function ClientJoinedMerchantsPage() {
  const { t } = useI18n();
  const [joined, setJoined] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    api("/client/merchants/joined")
      .then((data) => setJoined(data || []))
      .catch((e) => setError(e.message));
  }, []);

  return (
    <div className="mx-auto max-w-5xl">
      <DashboardPageShell>
        <h1 className="mb-2 text-2xl font-semibold text-slate-900">{t("clientMerchantsJoinedTitle")}</h1>
        <p className="mb-6 text-sm text-slate-600">{t("clientMerchantsJoinedPageSubtitle")}</p>
        {error && <p className="mb-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}
        <div className="grid gap-3 md:grid-cols-2">
          {joined.map((m) => (
            <article key={m.merchantId} className="rounded-xl border border-slate-200 bg-white p-4">
              <p className="text-base font-semibold text-slate-900">{m.merchantName}</p>
              <p className="text-xs text-slate-500">{m.membershipStatus}</p>
              <div className="mt-3">
                <Link to={`/client/booking/${m.merchantSlug}`} className="text-sm font-medium text-indigo-600">
                  {t("clientMerchantsOpenStore")}
                </Link>
              </div>
            </article>
          ))}
          {!joined.length && <p className="text-sm text-slate-500">{t("clientMerchantsJoinedEmpty")}</p>}
        </div>
      </DashboardPageShell>
    </div>
  );
}
