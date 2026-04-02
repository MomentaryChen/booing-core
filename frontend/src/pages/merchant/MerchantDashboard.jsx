import { Link } from "react-router-dom";
import { useEffect, useState } from "react";
import { api } from "../../services/api/client";
import { getStoredMerchantId } from "../../services/merchant/merchantStorage";

export function MerchantDashboard() {
  const [stats, setStats] = useState({ bookings: 0, services: 0, exceptions: 0 });
  const merchantId = getStoredMerchantId();

  useEffect(() => {
    Promise.all([
      api(`/merchant/${merchantId}/bookings`),
      api(`/merchant/${merchantId}/services`),
      api(`/merchant/${merchantId}/availability-exceptions`),
    ]).then(([b, s, ex]) => setStats({ bookings: b.length, services: s.length, exceptions: ex.length }));
  }, [merchantId]);

  return (
    <div className="page">
      <header className="page-hero">
        <h1>商戶儀表板</h1>
        <p>預約操作與排班設定分離，讓日常執行與規則配置更清楚。</p>
      </header>

      <div className="stats-grid">
        <div className="stat-card"><span>今日/近期預約</span><strong>{stats.bookings}</strong></div>
        <div className="stat-card"><span>服務項目數</span><strong>{stats.services}</strong></div>
        <div className="stat-card"><span>例外規則數</span><strong>{stats.exceptions}</strong></div>
      </div>

      <section className="card">
        <div className="card-header"><h2>商戶工作台</h2></div>
        <p style={{ marginBottom: "12px" }}>
          <Link to="/merchant/register">尚無商戶？前往註冊</Link>
        </p>
        <div className="service-grid">
          <article className="service-item">
            <h3>預約管理頁</h3>
            <p>日視圖時間軸、快速改狀態、手動代客預約。</p>
            <Link className="btn btn-primary" to="/merchant/appointments">前往預約管理</Link>
          </article>
          <article className="service-item">
            <h3>時段設定頁</h3>
            <p>週期排班、例外月曆、衝突預約防呆提示。</p>
            <Link className="btn btn-primary" to="/merchant/settings/schedule">前往時段設定</Link>
          </article>
        </div>
      </section>
    </div>
  );
}
