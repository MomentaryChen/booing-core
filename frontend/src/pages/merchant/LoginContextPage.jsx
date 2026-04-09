import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { api, setStoredAccessToken } from "../../services/api/client";
import { getMerchantIdFromAccessToken } from "../../services/platformRole";
import { clearStoredMerchantId, setStoredMerchantId } from "../../services/merchant/merchantStorage";
import { useNavigation } from "../../navigation/NavigationContext";
import {
  consumePendingRegisterUserType,
  peekPendingRegisterUserType,
  resolvePostLoginDestination,
} from "../../services/auth/sessionRouting";

function contextLabel(opt) {
  if (opt.kind === "MERCHANT_SCOPED") return "商戶儀表板（預覽）";
  if (opt.role === "SYSTEM_ADMIN") return "系統管理員（平台）";
  return opt.role || opt.kind || "情境";
}

export function LoginContextPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { refresh } = useNavigation();

  const intent =
    location.state?.intent === "system" || location.state?.intent === "client"
      ? location.state.intent
      : "merchant";
  const rawReturn = location.state?.returnUrl != null ? String(location.state.returnUrl) : null;

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [options, setOptions] = useState([]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await api("/auth/me");
        if (cancelled) return;
        const ctx = data.availableContexts || [];
        if (ctx.length <= 1) {
          const role = data.role || "";
          await refresh();
          const next = resolvePostLoginDestination({
            returnUrlRaw: rawReturn,
            nextDestination: null,
            role,
            intent,
            registerUserType: peekPendingRegisterUserType(),
          });
          navigate(next, { replace: true });
          consumePendingRegisterUserType();
          return;
        }
        setOptions(ctx);
        setLoading(false);
      } catch (e) {
        if (!cancelled) {
          setError(e.message || "無法載入可選情境");
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [navigate, refresh, intent, rawReturn]);

  async function selectOption(opt) {
    setError("");
    setLoading(true);
    try {
      const data = await api("/auth/context/select", {
        method: "POST",
        body: JSON.stringify({ role: opt.role, merchantId: opt.merchantId }),
      });
      setStoredAccessToken(data.accessToken);
      const mid = getMerchantIdFromAccessToken(data.accessToken);
      if (mid) setStoredMerchantId(mid);
      else clearStoredMerchantId();
      await refresh();
      const next = resolvePostLoginDestination({
        returnUrlRaw: rawReturn,
        nextDestination: data.nextDestination,
        role: data.role,
        intent,
        registerUserType: peekPendingRegisterUserType(),
      });
      navigate(next, { replace: true });
      consumePendingRegisterUserType();
    } catch (err) {
      setError(err.message || "切換失敗");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="merchant-login-page">
      <div className="merchant-login-card">
        <p className="merchant-login-eyebrow">Booking Core</p>
        <h1>選擇登入情境</h1>
        <p className="merchant-login-lede">請選擇要以哪種身分繼續。之後仍可依權限使用導覽列切換區域。</p>
        {error && <p className="merchant-login-error">{error}</p>}
        {loading ? (
          <p className="merchant-login-lede">載入中…</p>
        ) : (
          <div className="merchant-login-form" style={{ gap: "0.75rem" }}>
            {options.map((opt, i) => (
              <button
                key={`${opt.kind}-${opt.role}-${opt.merchantId ?? "x"}-${i}`}
                type="button"
                className="btn btn-primary merchant-login-submit"
                disabled={loading}
                onClick={() => selectOption(opt)}
              >
                {contextLabel(opt)}
              </button>
            ))}
          </div>
        )}
        <p className="merchant-login-footer">
          <Link to="/login">返回登入</Link>
        </p>
      </div>
    </div>
  );
}
