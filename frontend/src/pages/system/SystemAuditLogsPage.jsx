import { useEffect, useState } from "react";
import { api } from "../../services/api/client";
import { useI18n } from "../../i18n";

export function SystemAuditLogsPage() {
  const { t } = useI18n();
  const [rows, setRows] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    api("/system/audit-logs")
      .then((data) => setRows(data || []))
      .catch((e) => setError(e.message));
  }, []);

  return (
    <div className="mx-auto max-w-5xl">
      <section className="mb-4 rounded-xl border border-slate-200 bg-white p-4">
        <h1 className="text-2xl font-semibold text-slate-900">{t("systemAuditLogsTitle")}</h1>
        <p className="mt-1 text-sm text-slate-600">{t("systemAuditLogsSubtitle")}</p>
      </section>
      {error && <p className="mb-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}
      <section className="rounded-xl border border-slate-200 bg-white p-4">
        <div className="space-y-2">
          {rows.map((log) => (
            <article key={log.id} className="rounded-md border border-slate-200 p-3">
              <p className="text-sm font-semibold text-slate-900">
                {log.action} · {log.actor}
              </p>
              <p className="text-xs text-slate-500">
                {log.targetType}#{log.targetId} · {log.createdAt}
              </p>
              <p className="mt-1 text-xs text-slate-600">{log.detail}</p>
            </article>
          ))}
          {!rows.length && <p className="text-sm text-slate-500">{t("systemAuditLogsEmpty")}</p>}
        </div>
      </section>
    </div>
  );
}
