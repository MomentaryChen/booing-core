import { useEffect, useState } from "react";
import { api } from "../../services/api/client";
import { useI18n } from "../../i18n";

export function SystemMerchantsPage() {
  const { t } = useI18n();
  const [rows, setRows] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    api("/system/merchants")
      .then((data) => setRows(data || []))
      .catch((e) => setError(e.message));
  }, []);

  return (
    <div className="mx-auto max-w-5xl">
      <section className="mb-4 rounded-xl border border-slate-200 bg-white p-4">
        <h1 className="text-2xl font-semibold text-slate-900">{t("systemMerchantsTitle")}</h1>
        <p className="mt-1 text-sm text-slate-600">{t("systemMerchantsSubtitle")}</p>
      </section>
      {error && <p className="mb-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}
      <section className="rounded-xl border border-slate-200 bg-white p-4">
        <div className="space-y-2">
          {rows.map((m) => (
            <article key={m.id} className="rounded-md border border-slate-200 p-3">
              <p className="text-sm font-semibold text-slate-900">
                #{m.id} · {m.name}
              </p>
              <p className="text-xs text-slate-500">
                {m.slug} · {m.active ? "ACTIVE" : "INACTIVE"}
              </p>
            </article>
          ))}
          {!rows.length && <p className="text-sm text-slate-500">{t("systemMerchantsEmpty")}</p>}
        </div>
      </section>
    </div>
  );
}
