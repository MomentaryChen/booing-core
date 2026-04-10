import { useEffect, useState } from "react";
import { api } from "../../services/api/client";
import { getStoredMerchantId } from "../../services/merchant/merchantStorage";
import { DashboardPageShell } from "../../components/shell/DashboardPageShell";
import { useI18n } from "../../i18n";

export function MerchantInvitationsPage() {
  const { t } = useI18n();
  const merchantId = getStoredMerchantId();
  const [rows, setRows] = useState([]);
  const [inviteeUsername, setInviteeUsername] = useState("");
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");

  async function refresh() {
    try {
      const data = await api(`/merchant/${merchantId}/invitations`);
      setRows(data || []);
      setError("");
    } catch (e) {
      setError(e.message);
    }
  }

  useEffect(() => {
    refresh();
  }, [merchantId]);

  async function createInvitation(e) {
    e.preventDefault();
    try {
      await api(`/merchant/${merchantId}/invitations`, {
        method: "POST",
        body: JSON.stringify({ inviteeUsername }),
      });
      setInviteeUsername("");
      setInfo(t("merchantInvitationsCreated"));
      refresh();
    } catch (e2) {
      setError(e2.message);
    }
  }

  async function revokeInvitation(invitationId) {
    try {
      await api(`/merchant/${merchantId}/invitations/${invitationId}`, {
        method: "PATCH",
        body: JSON.stringify({ status: "REVOKED" }),
      });
      refresh();
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <div className="mx-auto max-w-5xl">
      <DashboardPageShell>
        <header className="mb-6 space-y-2">
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900 sm:text-3xl">
            {t("merchantInvitationsTitle")}
          </h1>
          <p className="text-sm text-slate-600">{t("merchantInvitationsSubtitle")}</p>
        </header>

        {error && <p className="mb-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}
        {info && <p className="mb-4 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{info}</p>}

        <section className="mb-6 rounded-xl border border-slate-200 bg-white p-4">
          <h2 className="mb-3 text-lg font-semibold text-slate-900">{t("merchantInvitationsCreateTitle")}</h2>
          <form className="flex flex-col gap-3 sm:flex-row" onSubmit={createInvitation}>
            <input
              value={inviteeUsername}
              onChange={(e) => setInviteeUsername(e.target.value)}
              placeholder={t("merchantInvitationsInviteePlaceholder")}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
            />
            <button type="submit" className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white">
              {t("merchantInvitationsCreateSubmit")}
            </button>
          </form>
        </section>

        <section className="rounded-xl border border-slate-200 bg-white p-4">
          <h2 className="mb-3 text-lg font-semibold text-slate-900">{t("merchantInvitationsListTitle")}</h2>
          <div className="space-y-2">
            {rows.map((row) => (
              <article key={row.invitationId} className="rounded-md border border-slate-200 p-3">
                <p className="text-sm font-semibold text-slate-900">
                  {row.inviteeUsername} · {row.status}
                </p>
                <p className="mt-1 break-all text-xs text-slate-500">{row.inviteCode}</p>
                {row.status === "PENDING" && (
                  <button
                    type="button"
                    className="mt-2 rounded-md border border-rose-300 px-3 py-1 text-xs text-rose-700"
                    onClick={() => revokeInvitation(row.invitationId)}
                  >
                    {t("merchantInvitationsRevoke")}
                  </button>
                )}
              </article>
            ))}
            {!rows.length && <p className="text-sm text-slate-500">{t("merchantInvitationsEmpty")}</p>}
          </div>
        </section>
      </DashboardPageShell>
    </div>
  );
}
