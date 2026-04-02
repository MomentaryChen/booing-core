import { Link } from "react-router-dom";
import { useI18n } from "../../i18n";

export function ClientTodoPage() {
  const { locale } = useI18n();
  const isZh = locale === "zh-TW";

  return (
    <div className="page">
      <header className="page-hero">
        <h1>{isZh ? "客戶預約入口" : "Client booking entry"}</h1>
        <p>
          {isZh
            ? "匿名訪客可先查看店家可預約時段；實際送出預約前需輸入店家提供的邀請碼。"
            : "Anonymous visitors can browse a store schedule first. Invite code verification is required before final booking."}
        </p>
      </header>

      <section className="card">
        <div className="card-header">
          <h2>{isZh ? "使用方式" : "How it works"}</h2>
        </div>
        <ul className="list">
          <li>
            <strong>{isZh ? "第 1 步：前往店家頁" : "Step 1: Open store page"}</strong>
            <span>{isZh ? "由店家分享連結進入，例如 /client/booking/{slug}" : "Use the merchant link, e.g. /client/booking/{slug}"}</span>
          </li>
          <li>
            <strong>{isZh ? "第 2 步：先看時段" : "Step 2: Browse schedules first"}</strong>
            <span>{isZh ? "不登入也可查看服務、日期與可用時段。" : "No login required to view services, dates, and available timeslots."}</span>
          </li>
          <li>
            <strong>{isZh ? "第 3 步：邀請碼 + 基本資料" : "Step 3: Invite code + basic info"}</strong>
            <span>{isZh ? "送出前需輸入邀請碼，並填寫姓名與手機。" : "Before submit, enter invite code, name, and phone number."}</span>
          </li>
        </ul>
        <div style={{ marginTop: 14 }}>
          <Link className="btn btn-primary" to="/client/booking/demo-merchant">
            {isZh ? "前往示範店家預約" : "Open demo merchant booking"}
          </Link>
        </div>
      </section>
    </div>
  );
}
