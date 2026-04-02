import { useEffect, useMemo, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "../../services/api/client";
import { useI18n } from "../../i18n";

const STEPS = ["service", "date", "time", "details"];
const CLIENT_PROFILE_CACHE_KEY = "booking_core_client_profile";

function serviceImageUrl(serviceId) {
  return `https://picsum.photos/seed/booking-svc-${serviceId}/240/140`;
}

function TimeSlotButton({ slot, active, disabled, label, onSelect }) {
  const [ripple, setRipple] = useState(null);
  const btnRef = useRef(null);

  function handleClick(e) {
    if (disabled || !slot.available) return;
    const el = btnRef.current;
    if (el) {
      const r = el.getBoundingClientRect();
      setRipple({ x: e.clientX - r.left, y: e.clientY - r.top, id: Date.now() });
      window.setTimeout(() => setRipple(null), 650);
    }
    onSelect(slot.startAt);
  }

  return (
    <button
      ref={btnRef}
      type="button"
      className={`user-booking__slot ${active ? "user-booking__slot--active" : ""} ${disabled ? "user-booking__slot--disabled" : ""}`}
      disabled={disabled}
      onClick={handleClick}
    >
      {ripple && (
        <span
          className="user-booking__slot-ripple"
          style={{ left: ripple.x, top: ripple.y }}
          key={ripple.id}
        />
      )}
      {label}
    </button>
  );
}

export function PublicStorefront() {
  const { slug } = useParams();
  const { locale, t } = useI18n();
  const isZh = locale === "zh-TW";
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [stepIndex, setStepIndex] = useState(0);
  const [selectedDate, setSelectedDate] = useState("");
  const [availability, setAvailability] = useState([]);
  const [lock, setLock] = useState(null);
  const [success, setSuccess] = useState(null);
  const [form, setForm] = useState({
    serviceItemId: "",
    resourceId: "",
    startAt: "",
    inviteCode: "",
    customerName: "",
    customerContact: "",
    agreeTerms: false,
    dynamicFieldValues: {},
  });

  const availabilityRequestIdRef = useRef(0);

  const step = STEPS[stepIndex];

  const titles = useMemo(
    () => ({
      service: isZh ? "選擇服務" : "Select Service",
      date: isZh ? "選擇日期" : "Pick a date",
      time: isZh ? "選擇時間" : "Choose time",
      details: isZh ? "您的資料" : "Your details",
    }),
    [isZh]
  );

  useEffect(() => {
    api(`/client/merchant/${slug}`)
      .then((res) => setData(res))
      .catch((e) => setError(e.message));
  }, [slug]);

  useEffect(() => {
    let mounted = true;
    const raw = typeof localStorage !== "undefined" ? localStorage.getItem(CLIENT_PROFILE_CACHE_KEY) : null;
    if (raw) {
      try {
        const cached = JSON.parse(raw);
        if (mounted) {
          setForm((prev) => ({
            ...prev,
            customerName: cached.customerName || prev.customerName,
            customerContact: cached.customerContact || prev.customerContact,
          }));
        }
      } catch (e) {
        // no-op
      }
    }
    api("/client/profile", { withAuth: true })
      .then((profile) => {
        if (!mounted || !profile?.authenticated) return;
        setForm((prev) => ({
          ...prev,
          customerName: prev.customerName || profile.suggestedName || "",
          customerContact: prev.customerContact || profile.suggestedContact || "",
        }));
      })
      .catch(() => {
        // allow anonymous browsing without warnings
      });
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (!data?.merchant?.id || !form.serviceItemId || !selectedDate) {
      setAvailability([]);
      return;
    }
    const requestId = Date.now();
    availabilityRequestIdRef.current = requestId;
    const controller = new AbortController();

    const resourcePart = form.resourceId ? `&resourceId=${form.resourceId}` : "";
    api(
      `/client/availability?merchantId=${data.merchant.id}&serviceItemId=${form.serviceItemId}&date=${selectedDate}${resourcePart}`,
      { signal: controller.signal }
    )
      .then((res) => {
        if (availabilityRequestIdRef.current !== requestId) return;
        setAvailability(res.slots || []);
      })
      .catch((e) => {
        if (controller.signal.aborted) return;
        if (availabilityRequestIdRef.current !== requestId) return;
        setError(e.message);
      });

    return () => controller.abort();
  }, [data, form.serviceItemId, form.resourceId, selectedDate]);

  const dates = useMemo(() => {
    const total = 14;
    const today = new Date();
    return Array.from({ length: total }, (_, index) => {
      const current = new Date(today);
      current.setDate(today.getDate() + index);
      return current.toISOString().slice(0, 10);
    });
  }, []);

  useEffect(() => {
    if (dates.length && !selectedDate) {
      setSelectedDate(dates[0]);
    }
  }, [dates, selectedDate]);

  function clearLock({ errorKey, goToTime } = {}) {
    setLock(null);
    setForm((v) => ({ ...v, startAt: "" }));
    if (goToTime) setStepIndex(2);
    if (errorKey) setError(t(errorKey));
  }

  useEffect(() => {
    if (!lock?.lockId || !lock?.expiresAt) return;
    if (success) return;

    const expiresAtMs = Date.parse(lock.expiresAt);
    if (!Number.isFinite(expiresAtMs)) return;

    const delay = expiresAtMs - Date.now();
    if (delay <= 0) {
      clearLock({ errorKey: "clientBooking.error.lockExpired", goToTime: true });
      return;
    }

    const timerId = window.setTimeout(() => {
      clearLock({ errorKey: "clientBooking.error.lockExpired", goToTime: true });
    }, delay);

    return () => window.clearTimeout(timerId);
  }, [lock?.lockId, lock?.expiresAt, success]);

  function classifyBookingError(message) {
    const m = String(message || "").toLowerCase();

    if (m.includes("invalid invite code")) {
      return { errorKey: "clientBooking.error.inviteInvalid", clearLock: false };
    }
    if (m.includes("not configured") && m.includes("invite")) {
      return { errorKey: "clientBooking.error.inviteCodeNotConfigured", clearLock: true };
    }
    if (m.includes("agree to booking terms")) {
      return { errorKey: "clientBooking.error.termsRequired", clearLock: false };
    }
    if (m.includes("selected slot is currently being held")) {
      return { errorKey: "clientBooking.error.lockHeld", clearLock: true };
    }
    if (m.includes("booking lock expired")) {
      return { errorKey: "clientBooking.error.lockExpired", clearLock: true };
    }
    if (m.includes("outside business hours")) {
      return { errorKey: "clientBooking.error.outsideBusinessHours", clearLock: true };
    }
    if (m.includes("blocked exception")) {
      return { errorKey: "clientBooking.error.blockedWindow", clearLock: true };
    }
    if (m.includes("conflicts with an existing booking")) {
      return { errorKey: "clientBooking.error.conflict", clearLock: true };
    }

    return { errorKey: "clientBooking.error.generic", clearLock: false };
  }

  async function lockSlot(slotStartAt) {
    if (!data?.merchant?.id || !form.serviceItemId) {
      return;
    }
    try {
      const lockRes = await api("/client/booking/lock", {
        method: "POST",
        body: JSON.stringify({
          merchantId: data.merchant.id,
          serviceItemId: Number(form.serviceItemId),
          resourceId: form.resourceId ? Number(form.resourceId) : null,
          startAt: slotStartAt,
        }),
      });
      setLock(lockRes);
      setForm((v) => ({ ...v, startAt: slotStartAt }));
      setError("");
    } catch (e) {
      clearLock({ errorKey: classifyBookingError(e.message).errorKey, goToTime: stepIndex >= 2 });
    }
  }

  async function submitBooking(e) {
    e.preventDefault();
    if (!lock?.lockId) {
      setError(t("clientBooking.error.selectSlotFirst"));
      return;
    }

    // Guard against local clock drift: if lock already expired on UI, re-select instead of failing silently.
    if (lock?.expiresAt) {
      const expiresAtMs = Date.parse(lock.expiresAt);
      if (Number.isFinite(expiresAtMs) && expiresAtMs <= Date.now()) {
        clearLock({ errorKey: "clientBooking.error.lockExpired", goToTime: true });
        return;
      }
    }

    setLoading(true);
    try {
      const booking = await api("/client/booking", {
        method: "POST",
        body: JSON.stringify({
          merchantId: data.merchant.id,
          serviceItemId: Number(form.serviceItemId),
          resourceId: form.resourceId ? Number(form.resourceId) : null,
          lockId: lock.lockId,
          startAt: form.startAt,
          inviteCode: form.inviteCode,
          customerName: form.customerName,
          customerContact: form.customerContact,
          agreeTerms: form.agreeTerms,
          dynamicFieldValues: form.dynamicFieldValues,
        }),
      });
      setError("");
      setSuccess(booking);
      if (typeof localStorage !== "undefined") {
        localStorage.setItem(
          CLIENT_PROFILE_CACHE_KEY,
          JSON.stringify({ customerName: form.customerName, customerContact: form.customerContact })
        );
      }
    } catch (err) {
      const classification = classifyBookingError(err.message);
      if (classification.clearLock) {
        clearLock({ errorKey: classification.errorKey, goToTime: true });
      } else {
        setError(t(classification.errorKey));
      }
    } finally {
      setLoading(false);
    }
  }

  const dynamicOk =
    !(data?.dynamicFields || []).some(
      (f) => f.requiredField && !(form.dynamicFieldValues[f.label] || "").trim()
    );

  const detailsValid =
    form.inviteCode.trim() &&
    form.customerName.trim() &&
    form.customerContact.trim() &&
    form.agreeTerms &&
    dynamicOk;

  const stepCanProceed = {
    service: !!form.serviceItemId,
    date: !!selectedDate,
    time: !!form.startAt && !!lock?.lockId,
    details: detailsValid && !!lock?.lockId,
  };

  function goNext() {
    if (!stepCanProceed[step]) return;
    if (stepIndex < STEPS.length - 1) {
      setStepIndex((i) => i + 1);
    }
  }

  function goBack() {
    if (stepIndex > 0) setStepIndex((i) => i - 1);
  }

  const ctaLabel =
    step === "details"
      ? loading
        ? isZh
          ? "送出中…"
          : "Submitting…"
        : isZh
          ? "確認預約"
          : "Confirm booking"
      : isZh
        ? "繼續"
        : "Continue";

  if (!data) {
    return (
      <div className="user-booking user-booking--loading">
        <p className="user-booking__loading-text">{error || (isZh ? "載入中…" : "Loading…")}</p>
      </div>
    );
  }

  if (success) {
    return (
      <div
        className={`user-booking user-booking--success theme-${data.customization.themePreset || "minimal"}`}
        style={{ "--theme-color": data.customization.themeColor }}
      >
        <div className="user-booking__success-card">
          <h1 className="user-booking__step-title user-booking__step-title--success">
            {isZh ? "預約成功" : "Booking confirmed"}
          </h1>
          <p className="user-booking__muted">
            {isZh ? "請保留以下資訊，方便後續查詢與取消。" : "Please keep this info for future reference."}
          </p>
          <ul className="user-booking__summary-list">
            <li>
              <span className="user-booking__summary-label">{isZh ? "預約代碼" : "Booking code"}</span>
              <span className="user-booking__summary-value">{success.bookingCode}</span>
            </li>
            <li>
              <span className="user-booking__summary-label">{isZh ? "時間" : "Time"}</span>
              <span className="user-booking__summary-value">{new Date(success.startAt).toLocaleString()}</span>
            </li>
            <li>
              <span className="user-booking__summary-label">{isZh ? "查詢連結" : "Query link"}</span>
              <span className="user-booking__summary-value user-booking__summary-value--break">{`${window.location.origin}${success.queryPath}`}</span>
            </li>
          </ul>
        </div>
      </div>
    );
  }

  const themeClass = `theme-${data.customization.themePreset || "minimal"}`;
  const merchantLabel = data.customization.heroTitle || data.merchant.name;

  return (
    <div
      className={`user-booking ${themeClass}`}
      style={{ "--theme-color": data.customization.themeColor || "#3b82f6" }}
    >
      <header className="user-booking__top">
        {stepIndex > 0 ? (
          <button type="button" className="user-booking__back" onClick={goBack} aria-label={isZh ? "返回" : "Back"}>
            ←
          </button>
        ) : (
          <span className="user-booking__back-spacer" />
        )}
        <p className="user-booking__merchant-name">{merchantLabel}</p>
        <span className="user-booking__back-spacer" />
      </header>

      <h1 className="user-booking__step-title">{titles[step]}</h1>

      {step === "service" && (
        <section className="user-booking__panel" aria-label={titles.service}>
          <div className="user-booking__service-list">
            {data.services.map((s) => {
              const selected = String(s.id) === String(form.serviceItemId);
              return (
                <button
                  key={s.id}
                  type="button"
                  className={`user-booking__service-card ${selected ? "user-booking__service-card--active" : ""}`}
                  onClick={() => {
                    setForm((v) => ({ ...v, serviceItemId: String(s.id), startAt: "" }));
                    setLock(null);
                    setSuccess(null);
                    setError("");
                    setStepIndex((i) => (i > 1 ? 2 : i));
                  }}
                >
                  <div className="user-booking__service-media">
                    <img src={serviceImageUrl(s.id)} alt="" loading="lazy" decoding="async" width={120} height={70} />
                  </div>
                  <div className="user-booking__service-body">
                    <span className="user-booking__service-name">{s.name}</span>
                    <span className="user-booking__service-meta">
                      {s.durationMinutes} min · ${s.price}
                    </span>
                  </div>
                </button>
              );
            })}
          </div>
          {!!data.resources?.length && (
            <label className="user-booking__field">
              <span className="user-booking__field-label">{isZh ? "人員 / 資源" : "Professional"}</span>
              <select
                className="user-booking__select"
                value={form.resourceId}
                onChange={(e) => {
                  setForm((v) => ({ ...v, resourceId: e.target.value, startAt: "" }));
                  setLock(null);
                  setSuccess(null);
                  setError("");
                  setStepIndex((i) => (i > 2 ? 2 : i));
                }}
              >
                <option value="">{isZh ? "由系統安排" : "Auto assign"}</option>
                {data.resources.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name}
                  </option>
                ))}
              </select>
            </label>
          )}
        </section>
      )}

      {step === "date" && (
        <section className="user-booking__panel" aria-label={titles.date}>
          <div className="user-booking__day-scroll" role="list">
            {dates.map((d) => {
              const dateObj = new Date(d + "T12:00:00");
              const weekday = dateObj.toLocaleDateString(isZh ? "zh-TW" : "en-US", { weekday: "short" });
              const dayNum = dateObj.getDate();
              const selected = d === selectedDate;
              return (
                <button
                  key={d}
                  type="button"
                  role="listitem"
                  className={`user-booking__day-pill ${selected ? "user-booking__day-pill--active" : ""}`}
                  onClick={() => {
                    setSelectedDate(d);
                    setForm((v) => ({ ...v, startAt: "" }));
                    setLock(null);
                    setSuccess(null);
                    setError("");
                    setStepIndex((i) => (i > 2 ? 2 : i));
                  }}
                >
                  <span className="user-booking__day-weekday">{weekday}</span>
                  <span className="user-booking__day-num">{dayNum}</span>
                </button>
              );
            })}
          </div>
        </section>
      )}

      {step === "time" && (
        <section className="user-booking__panel" aria-label={titles.time}>
          {!form.serviceItemId ? (
            <p className="user-booking__muted">{isZh ? "請先選擇服務。" : "Please select a service first."}</p>
          ) : (
            <div className="user-booking__slots">
              {availability.length === 0 ? (
                <p className="user-booking__muted">{isZh ? "此日無可預約時段。" : "No slots this day."}</p>
              ) : (
                availability.map((slot) => (
                  <TimeSlotButton
                    key={slot.startAt}
                    slot={slot}
                    active={form.startAt === slot.startAt}
                    disabled={!slot.available}
                    label={new Date(slot.startAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                    onSelect={lockSlot}
                  />
                ))
              )}
            </div>
          )}
        </section>
      )}

      {step === "details" && (
        <section className="user-booking__panel" aria-label={titles.details}>
          <form id="user-booking-form" onSubmit={submitBooking} className="user-booking__form">
            <p className="user-booking__muted">
              {t("clientBooking.scheduleAnonymousInfo")}
            </p>
            <label className="user-booking__field">
              <span className="user-booking__field-label">{t("clientBooking.inviteCodeLabel")}</span>
              <input
                className="user-booking__input"
                required
                value={form.inviteCode}
                placeholder={t("clientBooking.inviteCodePlaceholder")}
                onChange={(e) => setForm((v) => ({ ...v, inviteCode: e.target.value }))}
              />
            </label>
            <label className="user-booking__field">
              <span className="user-booking__field-label">{isZh ? "姓名" : "Name"}</span>
              <input
                className="user-booking__input"
                required
                value={form.customerName}
                onChange={(e) => setForm((v) => ({ ...v, customerName: e.target.value }))}
              />
            </label>
            <label className="user-booking__field">
              <span className="user-booking__field-label">{isZh ? "手機號碼" : "Phone"}</span>
              <input
                className="user-booking__input"
                required
                value={form.customerContact}
                onChange={(e) => setForm((v) => ({ ...v, customerContact: e.target.value }))}
              />
            </label>
            {(data.dynamicFields || []).map((field) => (
              <label key={field.id} className="user-booking__field">
                <span className="user-booking__field-label">{field.label}</span>
                <input
                  className="user-booking__input"
                  required={!!field.requiredField}
                  value={form.dynamicFieldValues[field.label] || ""}
                  onChange={(e) =>
                    setForm((v) => ({
                      ...v,
                      dynamicFieldValues: { ...v.dynamicFieldValues, [field.label]: e.target.value },
                    }))
                  }
                />
              </label>
            ))}
            <label className="user-booking__checkbox">
              <input
                type="checkbox"
                checked={form.agreeTerms}
                onChange={(e) => setForm((v) => ({ ...v, agreeTerms: e.target.checked }))}
              />
              <span>{isZh ? "我已閱讀並同意預約須知" : "I agree to the booking terms."}</span>
            </label>
          </form>
        </section>
      )}

      {error && <p className="user-booking__error">{error}</p>}

      <div className="user-booking__cta-bar">
        {step === "details" ? (
          <button
            type="submit"
            form="user-booking-form"
            className="user-booking__cta"
            disabled={loading || !stepCanProceed.details}
          >
            {ctaLabel}
          </button>
        ) : (
          <button type="button" className="user-booking__cta" disabled={!stepCanProceed[step]} onClick={goNext}>
            {ctaLabel}
          </button>
        )}
      </div>
    </div>
  );
}

/** Alias for routes that prefer a user-namespace component name. */
export { PublicStorefront as ClientBookingPage };
