import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { api } from "../../services/api/client";
import { useI18n } from "../../i18n";

function formatDateTime(value) {
  if (!value) return "—";
  const dt = new Date(value);
  if (Number.isNaN(dt.getTime())) return "—";
  return dt.toLocaleString();
}

function formatTemplate(value, params) {
  if (!params) return value;
  return Object.entries(params).reduce((acc, [k, v]) => acc.replaceAll(`{${k}}`, String(v)), value);
}

export function SystemUsersPage() {
  const { t } = useI18n();
  const [users, setUsers] = useState([]);
  const [roleCatalog, setRoleCatalog] = useState([]);
  const [selectedUserId, setSelectedUserId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(1);
  const [newRoleCode, setNewRoleCode] = useState("");
  const [newMerchantId, setNewMerchantId] = useState("");
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [actionMessage, setActionMessage] = useState("");
  const [statusFeedback, setStatusFeedback] = useState({ type: "", message: "" });
  const [bindingFeedback, setBindingFeedback] = useState({ type: "", message: "" });
  const [staleSelectionMessage, setStaleSelectionMessage] = useState("");
  const [confirmState, setConfirmState] = useState(null);
  const [mobileView, setMobileView] = useState("list");
  const detailLoadSeqRef = useRef(0);
  const dialogRef = useRef(null);
  const previousFocusRef = useRef(null);

  const loadBase = useCallback(async () => {
    setLoading(true);
    setStaleSelectionMessage("");
    try {
      const [u, roles] = await Promise.all([api("/system/users"), api("/system/rbac/roles")]);
      const nextUsers = Array.isArray(u) ? u : [];
      setUsers(nextUsers);
      setRoleCatalog(Array.isArray(roles) ? roles : []);
      const first = nextUsers.length > 0 ? nextUsers[0].id : null;
      setSelectedUserId((prev) => {
        if (prev == null) return first;
        const stillExists = nextUsers.some((user) => user.id === prev);
        if (!stillExists && first != null) {
          setStaleSelectionMessage(t("systemUsers.staleSelectionRebound"));
        }
        if (!stillExists && first == null) {
          setStaleSelectionMessage(t("systemUsers.staleSelectionReset"));
        }
        return stillExists ? prev : first;
      });
      if (Array.isArray(roles) && roles.length > 0) {
        setNewRoleCode((prev) => prev || roles[0].roleCode);
      }
      setError("");
    } catch (e) {
      setError(e.message || t("systemUsers.errorLoad"));
    } finally {
      setLoading(false);
    }
  }, [t]);

  const loadDetail = useCallback(
    async (userId, sequence) => {
      if (!userId) {
        setDetail(null);
        setDetailLoading(false);
        return;
      }
      setDetailLoading(true);
      try {
        const d = await api(`/system/users/${userId}`);
        if (sequence !== detailLoadSeqRef.current) return;
        setDetail(d);
        setError("");
      } catch (e) {
        if (sequence !== detailLoadSeqRef.current) return;
        setError(e.message || t("systemUsers.errorLoad"));
      } finally {
        if (sequence === detailLoadSeqRef.current) setDetailLoading(false);
      }
    },
    [t]
  );

  useEffect(() => {
    loadBase();
  }, [loadBase]);

  useEffect(() => {
    if (selectedUserId) {
      detailLoadSeqRef.current += 1;
      const currentSeq = detailLoadSeqRef.current;
      loadDetail(selectedUserId, currentSeq);
    }
  }, [loadDetail, selectedUserId]);

  useEffect(() => {
    setPage(1);
  }, [query]);

  const PAGE_SIZE = 5;
  const filteredUsers = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return users;
    return users.filter((u) => (u.username || "").toLowerCase().includes(q));
  }, [query, users]);
  const totalPages = Math.max(1, Math.ceil(filteredUsers.length / PAGE_SIZE));
  const pagedUsers = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filteredUsers.slice(start, start + PAGE_SIZE);
  }, [filteredUsers, page]);

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);

  const activeBindings = useMemo(
    () => (detail?.bindings || []).filter((b) => b.status === "ACTIVE"),
    [detail]
  );
  const merchantScopedRoleCodes = useMemo(() => {
    const set = new Set(["MERCHANT", "SUB_MERCHANT"]);
    for (const role of roleCatalog) {
      const roleCode = String(role?.roleCode || "");
      if (roleCode.includes("MERCHANT")) {
        set.add(roleCode);
      }
    }
    for (const b of activeBindings) {
      if (b.merchantId != null) set.add(b.roleCode);
    }
    return set;
  }, [activeBindings, roleCatalog]);

  const selectedSummary = useMemo(
    () => users.find((u) => u.id === detail?.id) || null,
    [detail?.id, users]
  );
  const selectedInFilteredList = useMemo(
    () => filteredUsers.some((u) => u.id === selectedUserId),
    [filteredUsers, selectedUserId]
  );

  const scopeLabel = useCallback(
    (merchantId) =>
      merchantId == null
        ? t("systemUsers.scopeGlobalLabel")
        : `${t("systemUsers.scopeMerchantLabel")}:${merchantId}`,
    [t]
  );
  const announce = useCallback((message) => {
    setActionMessage("");
    window.setTimeout(() => setActionMessage(message), 0);
  }, []);

  const saveBindings = useCallback(
    async (nextBindings) => {
      if (!detail?.id) return;
      setSaving(true);
      setBindingFeedback({ type: "loading", message: t("systemUsers.bindingSaving") });
      try {
        await api(`/system/users/${detail.id}/rbac-bindings`, {
          method: "PUT",
          body: JSON.stringify({ bindings: nextBindings }),
        });
        detailLoadSeqRef.current += 1;
        const currentSeq = detailLoadSeqRef.current;
        await Promise.all([loadBase(), loadDetail(detail.id, currentSeq)]);
        setNewMerchantId("");
        setError("");
        setBindingFeedback({ type: "success", message: t("systemUsers.statusSaved") });
        announce(t("systemUsers.statusSaved"));
      } catch (e) {
        const message = e.message || t("systemUsers.errorSave");
        setError(message);
        setBindingFeedback({ type: "error", message });
      } finally {
        setSaving(false);
      }
    },
    [announce, detail?.id, loadBase, loadDetail, t]
  );

  async function onToggleUserEnabled(forceNextEnabled) {
    if (!detail?.id) return;
    const nextEnabled = forceNextEnabled ?? !detail.enabled;
    setSaving(true);
    setStatusFeedback({ type: "loading", message: t("systemUsers.statusSaving") });
    try {
      await api(`/system/users/${detail.id}/status`, {
        method: "PUT",
        body: JSON.stringify({ enabled: nextEnabled }),
      });
      detailLoadSeqRef.current += 1;
      const currentSeq = detailLoadSeqRef.current;
      await Promise.all([loadBase(), loadDetail(detail.id, currentSeq)]);
      setError("");
      setStatusFeedback({
        type: "success",
        message: nextEnabled ? t("systemUsers.statusEnabled") : t("systemUsers.statusDisabled"),
      });
      announce(nextEnabled ? t("systemUsers.statusEnabled") : t("systemUsers.statusDisabled"));
    } catch (e) {
      const message = e.message || t("systemUsers.errorSave");
      setError(message);
      setStatusFeedback({ type: "error", message });
    } finally {
      setSaving(false);
    }
  }

  function onRemoveBinding(binding) {
    const next = activeBindings
      .filter(
        (b) =>
          !(
            b.roleCode === binding.roleCode &&
            (b.merchantId ?? null) === (binding.merchantId ?? null)
          )
      )
      .map((b) => ({ roleCode: b.roleCode, merchantId: b.merchantId, active: true }));
    saveBindings(next);
  }

  function onAddBinding() {
    if (!newRoleCode) return;
    const isScopedRole = merchantScopedRoleCodes.has(newRoleCode);
    if (isScopedRole && newMerchantId.trim() === "") {
      const message = t("systemUsers.errorMerchantRequired");
      setError(message);
      setBindingFeedback({ type: "error", message });
      return;
    }
    if (isScopedRole) {
      const parsedMerchantId = Number(newMerchantId);
      if (!Number.isFinite(parsedMerchantId) || !Number.isInteger(parsedMerchantId) || parsedMerchantId <= 0) {
        const message = t("systemUsers.errorMerchantInvalid");
        setError(message);
        setBindingFeedback({ type: "error", message });
        return;
      }
    }
    const merchantId =
      isScopedRole && newMerchantId.trim() !== ""
        ? Number(newMerchantId)
        : null;
    const next = [
      ...activeBindings.map((b) => ({
        roleCode: b.roleCode,
        merchantId: b.merchantId,
        active: true,
      })),
      { roleCode: newRoleCode, merchantId, active: true },
    ];
    const dedup = new Map();
    for (const item of next) dedup.set(`${item.roleCode}|${item.merchantId ?? "null"}`, item);
    saveBindings(Array.from(dedup.values()));
  }

  function openToggleConfirm() {
    if (!detail) return;
    const nextEnabled = !detail.enabled;
    setConfirmState({
      title: nextEnabled ? t("systemUsers.confirmEnableTitle") : t("systemUsers.confirmDisableTitle"),
      body: formatTemplate(
        nextEnabled ? t("systemUsers.confirmEnableBody") : t("systemUsers.confirmDisableBody"),
        { username: detail.username }
      ),
      confirmText: nextEnabled ? t("systemUsers.actionEnable") : t("systemUsers.actionDisable"),
      danger: !nextEnabled,
      run: () => onToggleUserEnabled(nextEnabled),
    });
  }

  function openRemoveBindingConfirm(binding) {
    setConfirmState({
      title: t("systemUsers.confirmRemoveBindingTitle"),
      body: `${binding.roleCode} · ${scopeLabel(binding.merchantId)}`,
      confirmText: t("systemUsers.removeBinding"),
      danger: true,
      run: () => onRemoveBinding(binding),
    });
  }

  async function runConfirmAction() {
    if (!confirmState?.run) return;
    const action = confirmState.run;
    setConfirmState(null);
    await action();
  }

  useEffect(() => {
    if (!confirmState) return undefined;
    previousFocusRef.current = document.activeElement;
    window.setTimeout(() => {
      const target = dialogRef.current?.querySelector("button");
      target?.focus();
    }, 0);
    return () => {
      if (previousFocusRef.current && typeof previousFocusRef.current.focus === "function") {
        previousFocusRef.current.focus();
      }
    };
  }, [confirmState]);

  return (
    <div className="sys-users-page">
      <header className="sys-users-header">
        <div>
          <h1>{t("systemUsers.title")}</h1>
          <p>{t("systemUsers.subtitle")}</p>
        </div>
        <button type="button" className="sys-btn sys-btn--ghost" onClick={loadBase} disabled={loading}>
          {t("systemUsers.refresh")}
        </button>
      </header>

      {error && (
        <div className="sys-error" role="alert">
          {error}
        </div>
      )}
      {staleSelectionMessage && (
        <div className="info-banner" role="status" aria-live="polite">
          {staleSelectionMessage}
        </div>
      )}
      <p className="sys-sr-only" aria-live="polite">
        {actionMessage}
      </p>

      <div className="sys-users-layout">
        <section
          className={`sys-pane sys-users-list${mobileView === "detail" ? " is-mobile-hidden" : ""}`}
          aria-label={t("systemUsers.listTitle")}
        >
          <div className="sys-pane-head">
            <h2>{t("systemUsers.listTitle")}</h2>
            <span className="sys-chip">{filteredUsers.length}</span>
          </div>
          <input
            className="sys-input"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={t("systemUsers.searchPlaceholder")}
            aria-label={t("systemUsers.searchPlaceholder")}
          />
          {loading ? (
            <p className="sys-muted">{t("systemUsers.loading")}</p>
          ) : filteredUsers.length === 0 ? (
            <p className="sys-muted">{query.trim() ? t("systemUsers.noResult") : t("systemUsers.empty")}</p>
          ) : (
            <ul className="sys-users-list-items">
              {pagedUsers.map((u) => (
                <li key={u.id}>
                  <button
                    type="button"
                    className={`sys-users-row${selectedUserId === u.id ? " is-active" : ""}`}
                    onClick={() => {
                      setSelectedUserId(u.id);
                      setMobileView("detail");
                    }}
                  >
                    <div className="sys-users-row-top">
                      <strong>{u.username}</strong>
                      <span className={`sys-users-badge ${u.enabled ? "is-enabled" : "is-disabled"}`}>
                        {u.enabled ? t("systemUsers.enabled") : t("systemUsers.disabled")}
                      </span>
                    </div>
                    <span className="sys-muted">{(u.roleCodes || []).join(" / ") || "—"}</span>
                    <span className="sys-muted">
                      {t("systemUsers.updatedAt")}: {formatDateTime(u.updatedAt)}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
          {!loading && filteredUsers.length > PAGE_SIZE && (
            <div className="sys-users-pagination" aria-label={t("systemUsers.paginationLabel")}>
              <button
                type="button"
                className="sys-btn sys-btn--ghost"
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={page <= 1}
              >
                {t("systemUsers.prevPage")}
              </button>
              <span className="sys-muted">{t("systemUsers.pageIndicator", { page, total: totalPages })}</span>
              <button
                type="button"
                className="sys-btn sys-btn--ghost"
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                disabled={page >= totalPages}
              >
                {t("systemUsers.nextPage")}
              </button>
            </div>
          )}
          {!loading && filteredUsers.length > 0 && !selectedInFilteredList && (
            <p className="sys-muted">{t("systemUsers.selectionFilteredOut")}</p>
          )}
        </section>

        <section
          className={`sys-pane sys-users-detail${mobileView === "list" ? " is-mobile-hidden" : ""}`}
          aria-label={t("systemUsers.detailTitle")}
        >
          <div className="sys-pane-head">
            <button
              type="button"
              className="sys-btn sys-btn--ghost sys-users-back-btn"
              onClick={() => setMobileView("list")}
            >
              {t("systemUsers.backToList")}
            </button>
            <h2>{t("systemUsers.detailTitle")}</h2>
          </div>
          {!detail ? (
            <p className="sys-muted">{t("systemUsers.noSelection")}</p>
          ) : detailLoading ? (
            <p className="sys-muted">{t("systemUsers.detailLoading")}</p>
          ) : (
            <>
              <div className="sys-users-block" aria-label={t("systemUsers.zoneDetail")}>
                <h3>{t("systemUsers.zoneDetail")}</h3>
              </div>
              <div className="sys-users-meta-card">
                <div>
                  <p className="sys-users-username">{detail.username}</p>
                  <p className="sys-muted">
                    {t("systemUsers.primaryRole")}: {detail.primaryRole}
                  </p>
                  <p className="sys-muted">
                    {t("systemUsers.lastLoginAt")}: {formatDateTime(detail.lastLoginAt)}
                  </p>
                  <p className="sys-muted">
                    {t("systemUsers.activeBindings")}: {selectedSummary?.activeBindingsCount ?? activeBindings.length}
                  </p>
                </div>
                <span className={`sys-users-badge ${detail.enabled ? "is-enabled" : "is-disabled"}`}>
                  {detail.enabled ? t("systemUsers.enabled") : t("systemUsers.disabled")}
                </span>
              </div>

              <div className="sys-users-block" aria-label={t("systemUsers.zoneActions")}>
                <h3>{t("systemUsers.zoneActions")}</h3>
                <p className="sys-muted">{t("systemUsers.statusActionHint")}</p>
                <button type="button" className="sys-btn" onClick={openToggleConfirm} disabled={saving}>
                  {detail.enabled ? t("systemUsers.actionDisable") : t("systemUsers.actionEnable")}
                </button>
                {statusFeedback.message && (
                  <p
                    className={statusFeedback.type === "error" ? "sys-error" : "sys-muted"}
                    role={statusFeedback.type === "error" ? "alert" : "status"}
                    aria-live="polite"
                  >
                    {statusFeedback.message}
                  </p>
                )}
              </div>

              <div className="sys-users-block" aria-label={t("systemUsers.zoneBindings")}>
                <h3>{t("systemUsers.zoneBindings")}</h3>
                <ul className="sys-list">
                  {activeBindings.map((b) => (
                    <li key={`${b.roleCode}-${b.merchantId ?? "null"}`}>
                      <div>
                        <strong>{b.roleCode}</strong>
                        <span className="sys-muted">{scopeLabel(b.merchantId)}</span>
                      </div>
                      <button
                        type="button"
                        className="sys-btn sys-btn--danger"
                        onClick={() => openRemoveBindingConfirm(b)}
                        disabled={saving}
                      >
                        {t("systemUsers.removeBinding")}
                      </button>
                    </li>
                  ))}
                  {activeBindings.length === 0 && <li className="sys-muted">{t("systemUsers.noBindings")}</li>}
                </ul>

                <div className="sys-users-add-binding">
                  <select
                    value={newRoleCode}
                    onChange={(e) => setNewRoleCode(e.target.value)}
                    className="sys-input"
                    aria-label={t("systemUsers.role")}
                  >
                    {roleCatalog.map((r) => (
                      <option key={r.roleCode} value={r.roleCode}>
                        {r.roleCode}
                      </option>
                    ))}
                  </select>
                  <input
                    className="sys-input"
                    type="number"
                    value={newMerchantId}
                    onChange={(e) => setNewMerchantId(e.target.value)}
                    placeholder={t("systemUsers.merchantIdPlaceholder")}
                    disabled={!merchantScopedRoleCodes.has(newRoleCode)}
                    aria-label={t("systemUsers.merchantIdPlaceholder")}
                  />
                  <button
                    type="button"
                    className="sys-btn sys-btn--primary"
                    onClick={onAddBinding}
                    disabled={saving}
                  >
                    {t("systemUsers.addBinding")}
                  </button>
                </div>
                {bindingFeedback.message && (
                  <p
                    className={bindingFeedback.type === "error" ? "sys-error" : "sys-muted"}
                    role={bindingFeedback.type === "error" ? "alert" : "status"}
                    aria-live="polite"
                  >
                    {bindingFeedback.message}
                  </p>
                )}
              </div>

              <div className="sys-users-block" aria-label={t("systemUsers.zonePermissions")}>
                <h3>{t("systemUsers.zonePermissions")}</h3>
                <div className="sys-users-perm-grid">
                  {(detail.effectivePermissions || []).map((p) => (
                    <code key={p} className="sys-users-perm-chip">
                      {p}
                    </code>
                  ))}
                  {!(detail.effectivePermissions || []).length && (
                    <p className="sys-muted">{t("systemUsers.noPermissions")}</p>
                  )}
                </div>
              </div>
            </>
          )}
        </section>
      </div>

      {confirmState && (
        <div
          className="sys-dialog-backdrop"
          role="presentation"
          onClick={() => setConfirmState(null)}
          onKeyDown={(e) => {
            if (e.key === "Escape") {
              setConfirmState(null);
            }
          }}
        >
          <div
            className="sys-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="sys-confirm-title"
            ref={dialogRef}
            onClick={(e) => e.stopPropagation()}
            onKeyDown={(e) => {
              if (e.key === "Escape") {
                e.preventDefault();
                setConfirmState(null);
                return;
              }
              if (e.key !== "Tab") return;
              const focusables = dialogRef.current?.querySelectorAll(
                'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
              );
              if (!focusables || focusables.length === 0) return;
              const first = focusables[0];
              const last = focusables[focusables.length - 1];
              if (e.shiftKey && document.activeElement === first) {
                e.preventDefault();
                last.focus();
              } else if (!e.shiftKey && document.activeElement === last) {
                e.preventDefault();
                first.focus();
              }
            }}
          >
            <h3 id="sys-confirm-title">{confirmState.title}</h3>
            <p>{confirmState.body}</p>
            <div className="sys-dialog-actions">
              <button
                type="button"
                className={`sys-btn${confirmState.danger ? " sys-btn--danger" : ""}`}
                onClick={runConfirmAction}
                disabled={saving}
              >
                {confirmState.confirmText}
              </button>
              <button
                type="button"
                className="sys-btn sys-btn--ghost"
                onClick={() => setConfirmState(null)}
                disabled={saving}
              >
                {t("systemUsers.cancel")}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
