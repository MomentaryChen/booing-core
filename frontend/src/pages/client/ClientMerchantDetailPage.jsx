import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../../services/api/client";
import { useI18n } from "../../i18n";
import { DashboardPageShell } from "../../components/shell/DashboardPageShell";

export function ClientMerchantDetailPage() {
  const { merchantId } = useParams();
  const { t } = useI18n();
  const [merchant, setMerchant] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api("/client/merchants")
      .then((cards) => {
        const found = (cards || []).find((c) => String(c.merchantId) === String(merchantId));
        if (!found) {
          setError(t("clientMerchantDetailNotFound"));
          return;
        }
        setMerchant(found);
      })
      .catch((e) => setError(e.message));
  }, [merchantId, t]);

  return (
    <div className="mx-auto max-w-3xl">
      <DashboardPageShell>
        <h1 className="mb-2 text-2xl font-semibold text-slate-900">{t("clientMerchantDetailTitle")}</h1>
        {error && <p className="mb-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}
        {merchant && (
          <article className="rounded-xl border border-slate-200 bg-white p-4">
            <p className="text-base font-semibold text-slate-900">{merchant.merchantName}</p>
            <p className="mt-1 text-sm text-slate-600">
              {merchant.visibility} · {merchant.joinState}
            </p>
            <div className="mt-4 flex gap-3">
              <Link to={`/client/booking/${merchant.merchantSlug}`} className="text-sm font-medium text-indigo-600">
                {t("clientMerchantsOpenStore")}
              </Link>
              <Link to="/client/merchants/join" className="text-sm font-medium text-indigo-600">
                {t("clientMerchantDetailJoinCta")}
              </Link>
            </div>
          </article>
        )}
      </DashboardPageShell>
    </div>
  );
}
