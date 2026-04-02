import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "../../services/api/client";
import { getStoredMerchantId } from "../../services/merchant/merchantStorage";
import "./MerchantAppointmentsPage.css";

const DAY_START_HOUR = 7;
const DAY_END_HOUR = 21;
const HOUR_ROWS = Array.from({ length: DAY_END_HOUR - DAY_START_HOUR }, (_, i) => DAY_START_HOUR + i);

function localDateKey(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function startOfDay(d) {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  return x;
}

function startOfWeekMonday(d) {
  const x = startOfDay(d);
  const day = x.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  x.setDate(x.getDate() + diff);
  return x;
}

function parseApiDate(iso) {
  return new Date(iso);
}

function layoutBooking(booking, dayDate) {
  const dayStart = new Date(dayDate);
  dayStart.setHours(DAY_START_HOUR, 0, 0, 0);
  const dayEnd = new Date(dayDate);
  dayEnd.setHours(DAY_END_HOUR, 0, 0, 0);

  const s = parseApiDate(booking.startAt);
  const e = parseApiDate(booking.endAt);
  const clampStart = s < dayStart ? dayStart : s;
  const clampEnd = e > dayEnd ? dayEnd : e;
  if (clampEnd <= clampStart) return null;

  const totalMin = (DAY_END_HOUR - DAY_START_HOUR) * 60;
  const fromStartMin = (clampStart.getTime() - dayStart.getTime()) / 60000;
  const durMin = (clampEnd.getTime() - clampStart.getTime()) / 60000;
  return {
    topPct: (fromStartMin / totalMin) * 100,
    heightPct: Math.max((durMin / totalMin) * 100, 3),
  };
}

function bookingsForDate(bookings, date) {
  const key = localDateKey(date);
  return bookings.filter((b) => localDateKey(parseApiDate(b.startAt)) === key);
}

function conflictIdsForDay(dayBookings) {
  const ids = new Set();
  for (let i = 0; i < dayBookings.length; i += 1) {
    for (let j = i + 1; j < dayBookings.length; j += 1) {
      const a = dayBookings[i];
      const b = dayBookings[j];
      const as = parseApiDate(a.startAt).getTime();
      const ae = parseApiDate(a.endAt).getTime();
      const bs = parseApiDate(b.startAt).getTime();
      const be = parseApiDate(b.endAt).getTime();
      if (as < be && bs < ae) {
        ids.add(a.id);
        ids.add(b.id);
      }
    }
  }
  return ids;
}

function formatRangeLabel(anchorDate, viewMode) {
  const opts = { year: "numeric", month: "long", day: "numeric" };
  if (viewMode === "day") {
    return anchorDate.toLocaleDateString("zh-TW", { ...opts, weekday: "long" });
  }
  const w0 = startOfWeekMonday(anchorDate);
  const w6 = new Date(w0);
  w6.setDate(w6.getDate() + 6);
  const y = w0.getFullYear();
  return `${y} 年 ${w0.getMonth() + 1} 月 ${w0.getDate()} 日 — ${w6.getMonth() + 1} 月 ${w6.getDate()} 日`;
}

const STATUS_LABEL = {
  PENDING: "待確認",
  CONFIRMED: "已確認",
  CHECKED_IN: "已報到",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
  NO_SHOW: "未出席",
  LATE: "遲到",
};

export function MerchantAppointmentsPage() {
  const merchantId = getStoredMerchantId();
  const [bookings, setBookings] = useState([]);
  const [services, setServices] = useState([]);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [viewMode, setViewMode] = useState("week");
  const [anchorDate, setAnchorDate] = useState(() => startOfDay(new Date()));
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [form, setForm] = useState({
    serviceItemId: "",
    startAt: "",
    customerName: "",
    customerContact: "",
  });

  const weekDays = useMemo(() => {
    const w0 = startOfWeekMonday(anchorDate);
    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(w0);
      d.setDate(w0.getDate() + i);
      return d;
    });
  }, [anchorDate]);

  const serviceById = useMemo(() => {
    const m = new Map();
    services.forEach((s) => m.set(s.id, s));
    return m;
  }, [services]);

  useEffect(() => {
    refresh();
  }, [merchantId]);

  useEffect(() => {
    if (!selectedBooking) return undefined;
    const onKey = (e) => {
      if (e.key === "Escape") setSelectedBooking(null);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [selectedBooking]);

  async function refresh() {
    try {
      const [b, s] = await Promise.all([
        api(`/merchant/${merchantId}/bookings`),
        api(`/merchant/${merchantId}/services`),
      ]);
      setBookings(b);
      setServices(s);
      setError("");
    } catch (e) {
      setError(e.message);
    }
  }

  async function createManualBooking(e) {
    e.preventDefault();
    try {
      await api(`/merchant/${merchantId}/bookings`, {
        method: "POST",
        body: JSON.stringify({
          ...form,
          serviceItemId: Number(form.serviceItemId),
          startAt: new Date(form.startAt).toISOString().slice(0, 19),
        }),
      });
      setForm({ serviceItemId: "", startAt: "", customerName: "", customerContact: "" });
      setInfo("已手動建立預約。");
      refresh();
    } catch (e2) {
      setError(e2.message);
    }
  }

  const updateStatus = useCallback(
    async (bookingId, status) => {
      await api(`/merchant/${merchantId}/bookings/${bookingId}/status`, {
        method: "PUT",
        body: JSON.stringify({ status }),
      });
      setInfo(`狀態已更新為 ${STATUS_LABEL[status] || status}`);
      setSelectedBooking((cur) => (cur && cur.id === bookingId ? { ...cur, status } : cur));
      refresh();
    },
    [merchantId]
  );

  const navigatePrev = () => {
    const d = new Date(anchorDate);
    if (viewMode === "day") d.setDate(d.getDate() - 1);
    else d.setDate(d.getDate() - 7);
    setAnchorDate(startOfDay(d));
  };

  const navigateNext = () => {
    const d = new Date(anchorDate);
    if (viewMode === "day") d.setDate(d.getDate() + 1);
    else d.setDate(d.getDate() + 7);
    setAnchorDate(startOfDay(d));
  };

  const goToday = () => setAnchorDate(startOfDay(new Date()));

  const openDrawer = (b) => setSelectedBooking(b);
  const closeDrawer = () => setSelectedBooking(null);

  const selectedConflicts = useMemo(() => {
    if (!selectedBooking) return false;
    const dayList = bookingsForDate(bookings, parseApiDate(selectedBooking.startAt));
    return conflictIdsForDay(dayList).has(selectedBooking.id);
  }, [selectedBooking, bookings]);

  const renderBookingBlock = (b, dayDate, conflictIds) => {
    const layout = layoutBooking(b, dayDate);
    if (!layout) return null;
    const svc = serviceById.get(b.serviceItemId);
    const t0 = parseApiDate(b.startAt).toLocaleTimeString("zh-TW", { hour: "2-digit", minute: "2-digit" });
    const t1 = parseApiDate(b.endAt).toLocaleTimeString("zh-TW", { hour: "2-digit", minute: "2-digit" });
    const conflict = conflictIds.has(b.id);
    const cancelled = b.status === "CANCELLED";
    return (
      <button
        key={b.id}
        type="button"
        className={`calendar-block${conflict ? " calendar-block--conflict" : ""}${cancelled ? " calendar-block--cancelled" : ""}`}
        style={{ top: `${layout.topPct}%`, height: `${layout.heightPct}%` }}
        onClick={() => openDrawer(b)}
      >
        <span className="calendar-block__time">
          {t0} — {t1}
        </span>
        <span className="calendar-block__name">{b.customerName}</span>
        <span className="calendar-block__meta">{svc?.name || "服務"} · {STATUS_LABEL[b.status] || b.status}</span>
      </button>
    );
  };

  const hasAnyInView = useMemo(() => {
    if (viewMode === "day") {
      return bookingsForDate(bookings, anchorDate).length > 0;
    }
    return weekDays.some((d) => bookingsForDate(bookings, d).length > 0);
  }, [bookings, anchorDate, viewMode, weekDays]);

  return (
    <div className="appointments-dashboard">
      <header className="appointments-dashboard__masthead">
        <p className="appointments-dashboard__eyebrow">Merchant · Appointments</p>
        <h1 className="appointments-dashboard__title">預約行事曆</h1>
        <p className="appointments-dashboard__lede">
          以留白與網格凸顯可排時段；重疊預約以邊線提示。點選區塊自右側檢視客戶細節，無需離開主畫面。
        </p>
      </header>

      {error && <p className="dash-banner dash-banner--error">{error}</p>}
      {info && <p className="dash-banner dash-banner--info">{info}</p>}

      <div className="dash-toolbar">
        <div className="dash-toolbar__nav">
          <button type="button" onClick={navigatePrev} aria-label="上一段時間">
            ‹
          </button>
          <span className="dash-toolbar__date-label">{formatRangeLabel(anchorDate, viewMode)}</span>
          <button type="button" onClick={navigateNext} aria-label="下一段時間">
            ›
          </button>
          <button type="button" className="dash-toolbar__today" onClick={goToday}>
            今天
          </button>
        </div>
        <div className="dash-segmented" role="group" aria-label="檢視模式">
          <button type="button" aria-pressed={viewMode === "week"} onClick={() => setViewMode("week")}>
            週
          </button>
          <button type="button" aria-pressed={viewMode === "day"} onClick={() => setViewMode("day")}>
            日
          </button>
        </div>
      </div>

      <section className="dash-glass-form" aria-labelledby="dash-form-title">
        <div className="dash-glass-form__head">
          <h2 id="dash-form-title">手動代客預約</h2>
          <p>為來電或現場客戶快速建立一筆預約。</p>
        </div>
        <form onSubmit={createManualBooking} className="form-grid">
          <label>
            服務
            <select
              value={form.serviceItemId}
              onChange={(e) => setForm((v) => ({ ...v, serviceItemId: e.target.value }))}
            >
              <option value="">請選擇服務</option>
              {services.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            開始時間
            <input
              type="datetime-local"
              value={form.startAt}
              onChange={(e) => setForm((v) => ({ ...v, startAt: e.target.value }))}
            />
          </label>
          <label>
            客戶姓名
            <input
              value={form.customerName}
              onChange={(e) => setForm((v) => ({ ...v, customerName: e.target.value }))}
            />
          </label>
          <label>
            聯絡方式
            <input
              value={form.customerContact}
              onChange={(e) => setForm((v) => ({ ...v, customerContact: e.target.value }))}
            />
          </label>
          <button className="btn-dash-primary" type="submit">
            建立預約
          </button>
        </form>
      </section>

      <div className="dash-calendar-shell">
        <div className="dash-calendar-shell__bar">
          <span>Calendar</span>
          <div className="dash-legend">
            <span className="dash-legend__item">
              <span className="dash-legend__swatch" aria-hidden /> 預約
            </span>
            <span className="dash-legend__item">
              <span className="dash-legend__swatch dash-legend__swatch--conflict" aria-hidden /> 時段重疊
            </span>
          </div>
        </div>
        <div className="dash-cal-scroll">
          {viewMode === "day" && (
            <div className="dash-cal-day">
              <div className="dash-cal-day__times" aria-hidden>
                <div style={{ height: 52 }} />
                {HOUR_ROWS.map((h) => (
                  <div key={h} className="dash-cal-day__time-slot">
                    {String(h).padStart(2, "0")}:00
                  </div>
                ))}
              </div>
              <div>
                <div
                  style={{
                    height: 52,
                    display: "flex",
                    alignItems: "center",
                    paddingLeft: 12,
                    borderBottom: "1px solid var(--dash-gray-100, #eeeff2)",
                    fontSize: "0.8125rem",
                    fontWeight: 600,
                    color: "var(--dash-charcoal, #1a1d24)",
                  }}
                >
                  {anchorDate.toLocaleDateString("zh-TW", { weekday: "long", month: "long", day: "numeric" })}
                </div>
                <div className="dash-cal-day__canvas-wrap">
                  <div className="dash-cal-day__grid-bg" />
                  <div className="dash-cal-day__blocks">
                    {(() => {
                      const dayList = bookingsForDate(bookings, anchorDate);
                      const cids = conflictIdsForDay(dayList);
                      return dayList.map((b) => renderBookingBlock(b, anchorDate, cids));
                    })()}
                  </div>
                </div>
              </div>
            </div>
          )}

          {viewMode === "week" && (
            <div className="dash-cal-week">
              <div className="dash-cal-week__corner" />
              {weekDays.map((d) => {
                const active = localDateKey(d) === localDateKey(anchorDate);
                return (
                  <button
                    key={localDateKey(d)}
                    type="button"
                    className={`dash-cal-week__day-head${active ? " dash-cal-week__day-head--active" : ""}`}
                    onClick={() => {
                      setAnchorDate(startOfDay(d));
                      setViewMode("day");
                    }}
                  >
                    <abbr title={d.toLocaleDateString("zh-TW")}>
                      {["週一", "週二", "週三", "週四", "週五", "週六", "週日"][(d.getDay() + 6) % 7]}
                    </abbr>
                    <strong>{d.getDate()}</strong>
                  </button>
                );
              })}
              <div className="dash-cal-week__times">
                {HOUR_ROWS.map((h) => (
                  <div key={h} className="dash-cal-week__time-slot">
                    {String(h).padStart(2, "0")}:00
                  </div>
                ))}
              </div>
              {weekDays.map((d) => {
                const dayList = bookingsForDate(bookings, d);
                const cids = conflictIdsForDay(dayList);
                return (
                  <div key={localDateKey(d)} className="dash-cal-week__col">
                    <div className="dash-cal-week__grid-bg" />
                    <div className="dash-cal-week__blocks">{dayList.map((b) => renderBookingBlock(b, d, cids))}</div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
        {!hasAnyInView && (
          <p className="dash-empty-cal">此檢視範圍內尚無預約。空白時段即為可排程空間。</p>
        )}
      </div>

      <div
        className={`dash-drawer-backdrop${selectedBooking ? " dash-drawer-backdrop--open" : ""}`}
        onClick={closeDrawer}
        aria-hidden={!selectedBooking}
      />
      <aside
        className={`dash-drawer${selectedBooking ? " dash-drawer--open" : ""}`}
        aria-hidden={!selectedBooking}
        aria-labelledby="drawer-title"
      >
        {selectedBooking && (
          <>
            <div className="dash-drawer__header">
              <div className="dash-drawer__header-top">
                <h2 id="drawer-title">{selectedBooking.customerName}</h2>
                <button type="button" className="dash-drawer__close" onClick={closeDrawer} aria-label="關閉">
                  ×
                </button>
              </div>
              <span
                className={`dash-drawer__badge${selectedConflicts ? " dash-drawer__badge--conflict" : ""}`}
              >
                {selectedConflicts ? "與其他預約重疊" : STATUS_LABEL[selectedBooking.status] || selectedBooking.status}
              </span>
            </div>
            <div className="dash-drawer__body">
              <div className="dash-drawer__section">
                <h3>聯絡</h3>
                <p>
                  {/^[\d\s+()-]+$/.test(String(selectedBooking.customerContact || "").trim()) ? (
                    <a href={`tel:${String(selectedBooking.customerContact).replace(/\s/g, "")}`}>
                      {selectedBooking.customerContact}
                    </a>
                  ) : (
                    selectedBooking.customerContact
                  )}
                </p>
              </div>
              <div className="dash-drawer__section">
                <h3>時間</h3>
                <p>
                  {parseApiDate(selectedBooking.startAt).toLocaleString("zh-TW", {
                    dateStyle: "medium",
                    timeStyle: "short",
                  })}
                  <br />
                  至{" "}
                  {parseApiDate(selectedBooking.endAt).toLocaleString("zh-TW", {
                    dateStyle: "medium",
                    timeStyle: "short",
                  })}
                </p>
              </div>
              <div className="dash-drawer__section">
                <h3>服務</h3>
                <p>{serviceById.get(selectedBooking.serviceItemId)?.name || `ID ${selectedBooking.serviceItemId}`}</p>
              </div>
              <div className="dash-drawer__section">
                <h3>操作</h3>
                <div className="dash-drawer__actions">
                  <button type="button" className="dash-drawer__btn-primary" onClick={() => updateStatus(selectedBooking.id, "CHECKED_IN")}>
                    報到
                  </button>
                  <button type="button" onClick={() => updateStatus(selectedBooking.id, "COMPLETED")}>
                    完成
                  </button>
                  <button type="button" onClick={() => updateStatus(selectedBooking.id, "LATE")}>
                    遲到
                  </button>
                  <button type="button" className="dash-drawer__btn-danger" onClick={() => updateStatus(selectedBooking.id, "CANCELLED")}>
                    取消
                  </button>
                </div>
              </div>
            </div>
          </>
        )}
      </aside>
    </div>
  );
}
