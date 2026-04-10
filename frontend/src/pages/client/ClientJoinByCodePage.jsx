import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../../services/api/client";
import { useI18n } from "../../i18n";
import { DashboardPageShell } from "../../components/shell/DashboardPageShell";

export function ClientJoinByCodePage() {
  const { t } = useI18n();
  const [inviteCode, setInviteCode] = useState("");
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");

  async function submit(e) {
    e.preventDefault();
    try {
      const res = await api("/client/merchant-memberships/join-code", {
        method: "POST",
        body: JSON.stringify({ inviteCode }),
      });
      setInfo(`${t("clientMerchantsJoinSuccess")} #${res.merchantId}`);
      setInviteCode("");
      setError("");
    } catch (e2) {
      setError(e2.message);
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <DashboardPageShell>
        <h1 className="mb-2 text-2xl font-semibold text-slate-900">{t("clientMerchantsJoinTitle")}</h1>
        <p className="mb-6 text-sm text-slate-600">{t("clientMerchantsJoinPageSubtitle")}</p>
        {error && <p className="mb-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}
        {info && <p className="mb-4 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{info}</p>}
        <form className="flex flex-col gap-3 sm:flex-row" onSubmit={submit}>
          <input
            value={inviteCode}
            onChange={(e) => setInviteCode(e.target.value)}
            placeholder={t("clientMerchantsJoinPlaceholder")}
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
          <button type="submit" className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white">
            {t("clientMerchantsJoinSubmit")}
          </button>
        </form>
        <div className="mt-6">
          <Link to="/client/merchants" className="text-sm font-medium text-indigo-600">
            {t("clientMerchantsBackToList")}
          </Link>
        </div>
      </DashboardPageShell>
    </div>
  );
}
