import { useEffect, useState } from "react";
import { api } from "../../services/api/client";
import { getStoredMerchantId } from "../../services/merchant/merchantStorage";
const days = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

function toApiTime(value) {
  return value.length === 5 ? `${value}:00` : value;
}

function toInputTime(value) {
  return (value || "").slice(0, 5);
}

function inBusinessHours(startAt, endAt, rules) {
  const s = new Date(startAt);
  const e = new Date(endAt);
  const dayIdx = (s.getDay() + 6) % 7;
  const day = days[dayIdx];
  const start = `${String(s.getHours()).padStart(2, "0")}:${String(s.getMinutes()).padStart(2, "0")}:00`;
  const end = `${String(e.getHours()).padStart(2, "0")}:${String(e.getMinutes()).padStart(2, "0")}:00`;
  return rules
    .filter((r) => r.dayOfWeek === day)
    .some((r) => start >= r.startTime && end <= r.endTime);
}

export function MerchantScheduleSettingsPage() {
  const merchantId = getStoredMerchantId();
  const [hours, setHours] = useState([]);
  const [exceptions, setExceptions] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [hourDraft, setHourDraft] = useState({ dayOfWeek: "MONDAY", startTime: "09:00", endTime: "18:00" });
  const [exceptionForm, setExceptionForm] = useState({ type: "BLOCK", startAt: "", endAt: "", reason: "" });

  useEffect(() => {
    refresh();
  }, [merchantId]);

  async function refresh() {
    try {
      const [h, ex, b] = await Promise.all([
        api(`/merchant/${merchantId}/business-hours`).catch(() => []),
        api(`/merchant/${merchantId}/availability-exceptions`),
        api(`/merchant/${merchantId}/bookings`),
      ]);
      setHours(h);
      setExceptions(ex);
      setBookings(b);
      setError("");
    } catch (e) {
      setError(e.message);
    }
  }

  function addHour() {
    setHours((prev) => [...prev, { id: `draft-${Date.now()}`, dayOfWeek: hourDraft.dayOfWeek, startTime: toApiTime(hourDraft.startTime), endTime: toApiTime(hourDraft.endTime) }]);
  }

  function updateHour(i, key, value) {
    setHours((prev) => prev.map((it, idx) => (idx === i ? { ...it, [key]: value } : it)));
  }

  function removeHour(i) {
    setHours((prev) => prev.filter((_, idx) => idx !== i));
  }

  async function saveHours() {
    const impacted = bookings.filter((b) => !inBusinessHours(b.startAt, b.endAt, hours));
    if (impacted.length > 0) {
      const ok = window.confirm(`有 ${impacted.length} 筆既有預約可能受影響，是否仍要儲存？`);
      if (!ok) return;
    }
    await api(`/merchant/${merchantId}/business-hours`, {
      method: "PUT",
      body: JSON.stringify(hours.map((h) => ({ dayOfWeek: h.dayOfWeek, startTime: toApiTime(h.startTime), endTime: toApiTime(h.endTime) }))),
    });
    setInfo("排班規則已儲存。");
    refresh();
  }

  async function addException(e) {
    e.preventDefault();
    await api(`/merchant/${merchantId}/availability-exceptions`, {
      method: "POST",
      body: JSON.stringify({
        ...exceptionForm,
        startAt: new Date(exceptionForm.startAt).toISOString().slice(0, 19),
        endAt: new Date(exceptionForm.endAt).toISOString().slice(0, 19),
      }),
    });
    setExceptionForm({ type: "BLOCK", startAt: "", endAt: "", reason: "" });
    setInfo("例外時段已新增。");
    refresh();
  }

  async function deleteException(id) {
    await api(`/merchant/${merchantId}/availability-exceptions/${id}`, { method: "DELETE" });
    setInfo("例外時段已刪除。");
    refresh();
  }

  return (
    <div className="page">
      <header className="page-hero">
        <h1>時段設定（排班規則中心）</h1>
        <p>管理重複性週表與例外月曆，並在衝突時提示既有預約影響。</p>
      </header>
      {error && <p className="error-banner">{error}</p>}
      {info && <p className="info-banner">{info}</p>}

      <section className="card">
        <div className="card-header">
          <h2>常態排班（Recurring Weekly Rules）</h2>
          <button className="btn btn-primary" onClick={saveHours}>儲存規則</button>
        </div>
        <div className="form-grid">
          <label>星期
            <select value={hourDraft.dayOfWeek} onChange={(e) => setHourDraft((v) => ({ ...v, dayOfWeek: e.target.value }))}>
              {days.map((d) => <option key={d} value={d}>{d}</option>)}
            </select>
          </label>
          <label>開始
            <input type="time" value={hourDraft.startTime} onChange={(e) => setHourDraft((v) => ({ ...v, startTime: e.target.value }))} />
          </label>
          <label>結束
            <input type="time" value={hourDraft.endTime} onChange={(e) => setHourDraft((v) => ({ ...v, endTime: e.target.value }))} />
          </label>
          <button type="button" className="btn" onClick={addHour}>新增規則</button>
        </div>
        <ul className="list">
          {hours.map((h, idx) => (
            <li key={h.id || idx}>
              <div className="inline-fields">
                <select value={h.dayOfWeek} onChange={(e) => updateHour(idx, "dayOfWeek", e.target.value)}>{days.map((d) => <option key={d} value={d}>{d}</option>)}</select>
                <input type="time" value={toInputTime(h.startTime)} onChange={(e) => updateHour(idx, "startTime", toApiTime(e.target.value))} />
                <input type="time" value={toInputTime(h.endTime)} onChange={(e) => updateHour(idx, "endTime", toApiTime(e.target.value))} />
                <button className="btn btn-danger" onClick={() => removeHour(idx)}>刪除</button>
              </div>
            </li>
          ))}
        </ul>
      </section>

      <section className="card">
        <div className="card-header"><h2>例外月曆（Exception Calendar）</h2></div>
        <form onSubmit={addException} className="form-grid">
          <label>類型
            <select value={exceptionForm.type} onChange={(e) => setExceptionForm((v) => ({ ...v, type: e.target.value }))}>
              <option value="BLOCK">BLOCK（店休/封鎖）</option>
              <option value="OVERRIDE_OPEN">OVERRIDE_OPEN（特別加開）</option>
            </select>
          </label>
          <label>開始
            <input type="datetime-local" value={exceptionForm.startAt} onChange={(e) => setExceptionForm((v) => ({ ...v, startAt: e.target.value }))} />
          </label>
          <label>結束
            <input type="datetime-local" value={exceptionForm.endAt} onChange={(e) => setExceptionForm((v) => ({ ...v, endAt: e.target.value }))} />
          </label>
          <label>原因
            <input value={exceptionForm.reason} onChange={(e) => setExceptionForm((v) => ({ ...v, reason: e.target.value }))} />
          </label>
          <button className="btn btn-primary full" type="submit">新增例外</button>
        </form>
        <ul className="list">
          {exceptions.map((ex) => (
            <li key={ex.id}>
              <strong>{ex.type}</strong>
              <span>{ex.startAt} ~ {ex.endAt} / {ex.reason}</span>
              <button className="btn btn-danger" onClick={() => deleteException(ex.id)}>刪除</button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}

