import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../../services/api/client";
import { isMerchantAuthRequired } from "../../services/merchant/merchantAuth";
import { setStoredMerchantId } from "../../services/merchant/merchantStorage";

function normalizeSlug(value) {
  return value
    .trim()
    .toLowerCase()
    .replace(/\s+/g, "-")
    .replace(/[^a-z0-9-]/g, "");
}

export function MerchantRegisterPage() {
  const navigate = useNavigate();
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [slugTouched, setSlugTouched] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  function onNameChange(v) {
    setName(v);
    if (!slugTouched) {
      setSlug(normalizeSlug(v));
    }
  }

  function onSlugChange(v) {
    setSlugTouched(true);
    setSlug(normalizeSlug(v));
  }

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    if (!name.trim() || !slug.trim()) {
      setError("請填寫商戶名稱與網址代號（slug）。");
      return;
    }
    setLoading(true);
    try {
      const merchant = await api("/merchant/register", {
        method: "POST",
        body: JSON.stringify({ name: name.trim(), slug: slug.trim() }),
      });
      setStoredMerchantId(merchant.id);
      navigate("/merchant/login?registered=1", { replace: true });
    } catch (err) {
      setError(err.message || "註冊失敗");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <header className="page-hero">
        <h1>商戶註冊</h1>
        <p>建立新的商戶帳號後，即可使用商戶後台管理預約與排班。</p>
      </header>

      <section className="card" style={{ maxWidth: "32rem" }}>
        <div className="card-header">
          <h2>建立商戶</h2>
        </div>
        {error && <p className="error-banner">{error}</p>}
        <form
          onSubmit={onSubmit}
          style={{ padding: "1rem", display: "flex", flexDirection: "column", gap: "1rem" }}
        >
          <label>
            <span>商戶名稱</span>
            <input
              type="text"
              value={name}
              onChange={(e) => onNameChange(e.target.value)}
              placeholder="例如：晨光美髮"
              autoComplete="organization"
            />
          </label>
          <label>
            <span>網址代號（slug）</span>
            <input
              type="text"
              value={slug}
              onChange={(e) => onSlugChange(e.target.value)}
              placeholder="例如：morning-hair"
              autoComplete="off"
            />
            <small style={{ opacity: 0.75 }}>公開預約頁網址會使用此代號，僅限英文、數字與連字號。</small>
          </label>
          <div style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap" }}>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? "送出中…" : "完成註冊"}
            </button>
            <Link className="btn" to={isMerchantAuthRequired() ? "/merchant/login" : "/merchant"}>
              {isMerchantAuthRequired() ? "商戶登入" : "返回儀表板"}
            </Link>
          </div>
        </form>
      </section>
    </div>
  );
}
