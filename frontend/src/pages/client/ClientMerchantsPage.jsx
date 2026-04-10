import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../../services/api/client";
import { useI18n } from "../../i18n";
import { DashboardPageShell } from "../../components/shell/DashboardPageShell";

export function ClientMerchantsPage() {
  const { t } = useI18n();
  const [cards, setCards] = useState([]);
  const [joined, setJoined] = useState([]);
  const [inviteCode, setInviteCode] = useState("");
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");

  async function refresh() {
    try {
      const [allCards, joinedCards] = await Promise.all([
        api("/client/merchants"),
        api("/client/merchants/joined"),
      ]);
      setCards(allCards || []);
      setJoined(joinedCards || []);
      setError("");
    } catch (e) {
      setError(e.message);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  async function joinByCode(e) {
    e.preventDefault();
    try {
      const res = await api("/client/merchant-memberships/join-code", {
        method: "POST",
        body: JSON.stringify({ inviteCode }),
      });
      setInfo(`${t("clientMerchantsJoinSuccess")} #${res.merchantId}`);
      setInviteCode("");
      refresh();
    } catch (e2) {
      setError(e2.message);
    }
  }

  return (
    <div className="mx-auto max-w-5xl">
      <DashboardPageShell>
        <header className="mb-6 space-y-2">
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900 sm:text-3xl">
            {t("clientMerchantsTitle")}
          </h1>
          <p className="text-sm text-slate-600">{t("clientMerchantsSubtitle")}</p>
        </header>

        {error && <p className="mb-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}
        {info && <p className="mb-4 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{info}</p>}

        <section className="mb-8 rounded-xl border border-slate-200 bg-white p-4">
          <h2 className="mb-3 text-lg font-semibold text-slate-900">{t("clientMerchantsJoinTitle")}</h2>
          <form className="flex flex-col gap-3 sm:flex-row" onSubmit={joinByCode}>
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
          <div className="mt-3 flex gap-3">
            <Link to="/client/merchants/join" className="text-xs font-medium text-indigo-600">
              {t("clientMerchantsGoJoinPage")}
            </Link>
            <Link to="/client/merchants/joined" className="text-xs font-medium text-indigo-600">
              {t("clientMerchantsGoJoinedPage")}
            </Link>
          </div>
        </section>

        <section className="mb-8">
          <h2 className="mb-3 text-lg font-semibold text-slate-900">{t("clientMerchantsPublicTitle")}</h2>
          <div className="grid gap-3 md:grid-cols-2">
            {cards.map((m) => (
              <article key={m.merchantId} className="rounded-xl border border-slate-200 bg-white p-4">
                <p className="text-base font-semibold text-slate-900">{m.merchantName}</p>
                <p className="text-xs text-slate-500">{m.visibility}</p>
                <p className="mt-1 text-sm text-slate-600">{m.joinState}</p>
                <div className="mt-3">
                  <Link to={`/client/merchants/${m.merchantId}`} className="mr-3 text-sm font-medium text-indigo-600">
                    {t("clientMerchantDetailTitle")}
                  </Link>
                  <Link to={`/client/booking/${m.merchantSlug}`} className="text-sm font-medium text-indigo-600">
                    {t("clientMerchantsOpenStore")}
                  </Link>
                </div>
              </article>
            ))}
          </div>
        </section>

        <section>
          <h2 className="mb-3 text-lg font-semibold text-slate-900">{t("clientMerchantsJoinedTitle")}</h2>
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
        </section>
      </DashboardPageShell>
    </div>
  );
}
