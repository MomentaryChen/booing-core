import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { api, clearStoredAccessToken, getStoredAccessToken, setStoredAccessToken } from "../../services/api/client";
import { isMerchantAuthRequired } from "../../services/merchant/merchantAuth";
import { getMerchantIdFromAccessToken, getRoleFromAccessToken } from "../../services/platformRole";
import { clearStoredMerchantId, setStoredMerchantId } from "../../services/merchant/merchantStorage";
import { useNavigation } from "../../navigation/NavigationContext";

export function MerchantLoginPage() {
  const navigate = useNavigate();
  const { refresh } = useNavigation();
  const location = useLocation();
  const from =
    location.state?.from &&
    (String(location.state.from).startsWith("/merchant") || String(location.state.from) === "/system")
      ? String(location.state.from)
      : "/merchant";
  const registered = new URLSearchParams(location.search).get("registered") === "1";

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!isMerchantAuthRequired()) {
      navigate("/merchant", { replace: true });
      return;
    }
    const token = getStoredAccessToken();
    if (token) {
      const r = getRoleFromAccessToken(token);
      const mid = getMerchantIdFromAccessToken(token);
      if (mid) setStoredMerchantId(mid);
      else clearStoredMerchantId();
      if (r !== "SYSTEM_ADMIN" && r !== "MERCHANT" && r !== "SUB_MERCHANT") {
        clearStoredAccessToken();
        clearStoredMerchantId();
        return;
      }
      if ((r === "MERCHANT" || r === "SUB_MERCHANT") && !mid) {
        // token is missing required merchant scope; force re-login
        clearStoredAccessToken();
        clearStoredMerchantId();
        return;
      }
      const dest =
        r === "SYSTEM_ADMIN" && (from === "/merchant" || from === "/merchant/") ? "/system" : from;
      navigate(dest, { replace: true });
    }
  }, [navigate, from]);

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const data = await api("/auth/login", {
        method: "POST",
        body: JSON.stringify({ username: username.trim(), password }),
      });
      const role = data.role || "";
      if (role !== "MERCHANT" && role !== "SUB_MERCHANT" && role !== "SYSTEM_ADMIN") {
        setError("此帳號無法使用商戶後台。");
        setLoading(false);
        return;
      }
      setStoredAccessToken(data.accessToken);
      const mid = getMerchantIdFromAccessToken(data.accessToken);
      if (mid) setStoredMerchantId(mid);
      await refresh();
      const next =
        role === "SYSTEM_ADMIN" && (from === "/merchant" || from === "/merchant/")
          ? "/system"
          : from;
      navigate(next, { replace: true });
    } catch (err) {
      setError(err.message || "登入失敗");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="merchant-login-page">
      <div className="merchant-login-card">
        <p className="merchant-login-eyebrow">Merchant</p>
        <h1>商戶登入</h1>
        <p className="merchant-login-lede">登入後即可使用儀表板、預約與排程設定。</p>
        {registered && <p className="merchant-login-banner">註冊成功，請使用商戶帳號登入以繼續。</p>}
        {error && <p className="merchant-login-error">{error}</p>}
        <form onSubmit={onSubmit} className="merchant-login-form">
          <label>
            帳號
            <input
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="例如：merchant"
            />
          </label>
          <label>
            密碼
            <input
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>
          <button type="submit" className="btn btn-primary merchant-login-submit" disabled={loading}>
            {loading ? "登入中…" : "登入"}
          </button>
        </form>
        <p className="merchant-login-footer">
          <Link to="/merchant/register">尚未註冊商戶？</Link>
          <span className="merchant-login-dot"> · </span>
          <Link to="/">返回首頁</Link>
        </p>
      </div>
    </div>
  );
}
