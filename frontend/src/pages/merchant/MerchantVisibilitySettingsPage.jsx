import { useEffect, useState } from "react";
import { api } from "../../services/api/client";
import { getStoredMerchantId } from "../../services/merchant/merchantStorage";
import { DashboardPageShell } from "../../components/shell/DashboardPageShell";
import { useI18n } from "../../i18n";

export function MerchantVisibilitySettingsPage() {
  const { t } = useI18n();
  const merchantId = getStoredMerchantId();
  const [visibility, setVisibility] = useState("PUBLIC");
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");

  useEffect(() => {
    api(`/merchant/${merchantId}/profile`)
      .then(() => api(`/client/merchants`))
      .then((cards) => {
        const found = (cards || []).find((c) => c.merchantId === Number(merchantId));
        if (found?.visibility) setVisibility(found.visibility);
      })
      .catch((e) => setError(e.message));
  }, [merchantId]);

  async function save(e) {
    e.preventDefault();
    try {
      await api(`/merchant/${merchantId}/profile/visibility`, {
        method: "PUT",
        body: JSON.stringify({ visibility }),
      });
      setInfo(t("merchantVisibilitySaved"));
      setError("");
    } catch (e2) {
      setError(e2.message);
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <DashboardPageShell>
        <h1 className="mb-2 text-2xl font-semibold text-slate-900">{t("merchantVisibilityTitle")}</h1>
        <p className="mb-6 text-sm text-slate-600">{t("merchantVisibilitySubtitle")}</p>
        {error && <p className="mb-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}
        {info && <p className="mb-4 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{info}</p>}
        <form className="rounded-xl border border-slate-200 bg-white p-4" onSubmit={save}>
          <label className="mb-4 block text-sm font-medium text-slate-700">{t("merchantVisibilityLabel")}</label>
          <div className="mb-5 flex flex-col gap-2 sm:flex-row">
            <label className="inline-flex items-center gap-2 text-sm">
              <input
                type="radio"
                name="visibility"
                value="PUBLIC"
                checked={visibility === "PUBLIC"}
                onChange={(e) => setVisibility(e.target.value)}
              />
              {t("merchantVisibilityPublic")}
            </label>
            <label className="inline-flex items-center gap-2 text-sm">
              <input
                type="radio"
                name="visibility"
                value="INVITE_ONLY"
                checked={visibility === "INVITE_ONLY"}
                onChange={(e) => setVisibility(e.target.value)}
              />
              {t("merchantVisibilityInviteOnly")}
            </label>
          </div>
          <button type="submit" className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white">
            {t("merchantVisibilitySave")}
          </button>
        </form>
      </DashboardPageShell>
    </div>
  );
}
