import { useCallback, useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { X } from "lucide-react";
import {
  AUTH_OVERLAY_LOGIN,
  AUTH_OVERLAY_REGISTER,
  parseAuthOverlayFromSearch,
  stripAuthOverlayParams,
} from "../../services/auth/sessionRouting";
import { UnifiedLoginForm } from "./UnifiedLoginForm";
import { MerchantRegisterForm } from "./MerchantRegisterForm";
import { useI18n } from "../../i18n";

function getFocusableElements(root) {
  if (!root) return [];
  const sel = [
    'a[href]:not([tabindex="-1"])',
    'button:not([disabled]):not([tabindex="-1"])',
    'input:not([disabled]):not([type="hidden"]):not([tabindex="-1"])',
    'select:not([disabled]):not([tabindex="-1"])',
    'textarea:not([disabled]):not([tabindex="-1"])',
    '[tabindex]:not([tabindex="-1"])',
  ].join(",");
  return Array.from(root.querySelectorAll(sel)).filter(
    (el) => el.offsetParent !== null || el === document.activeElement
  );
}

export function AuthOverlay() {
  const location = useLocation();
  const navigate = useNavigate();
  const { t } = useI18n();
  const panelRef = useRef(null);
  const focusBeforeOpenRef = useRef(null);

  const parsed = parseAuthOverlayFromSearch(location.search);
  const open =
    !!parsed && location.pathname !== "/login" && location.pathname !== "/demo/saas-dashboard";

  const close = useCallback(() => {
    const search = stripAuthOverlayParams(location.search);
    navigate({ pathname: location.pathname, search }, { replace: true });
  }, [location.pathname, location.search, navigate]);

  useEffect(() => {
    if (!open) return undefined;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prevOverflow;
    };
  }, [open]);

  useEffect(() => {
    if (!open) return undefined;
    focusBeforeOpenRef.current = document.activeElement;
    return undefined;
  }, [open]);

  useEffect(() => {
    if (open) return undefined;
    const el = focusBeforeOpenRef.current;
    focusBeforeOpenRef.current = null;
    if (el && typeof el.focus === "function") {
      queueMicrotask(() => {
        try {
          el.focus();
        } catch {
          // ignore
        }
      });
    }
    return undefined;
  }, [open]);

  useEffect(() => {
    if (!open) return undefined;
    const panel = panelRef.current;
    if (!panel) return undefined;

    const tId = window.setTimeout(() => {
      const focusables = getFocusableElements(panel);
      const firstField = focusables.find(
        (el) => el.tagName === "INPUT" || el.tagName === "SELECT" || el.tagName === "TEXTAREA"
      );
      (firstField || focusables[0])?.focus();
    }, 0);

    function onKeyDown(e) {
      if (e.key === "Escape") {
        e.preventDefault();
        close();
        return;
      }
      if (e.key !== "Tab") return;
      const focusables = getFocusableElements(panel);
      if (focusables.length === 0) return;
      const first = focusables[0];
      const last = focusables[focusables.length - 1];
      if (e.shiftKey) {
        if (document.activeElement === first) {
          last.focus();
          e.preventDefault();
        }
      } else if (document.activeElement === last) {
        first.focus();
        e.preventDefault();
      }
    }

    document.addEventListener("keydown", onKeyDown);
    return () => {
      window.clearTimeout(tId);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [close, open, parsed?.mode, parsed?.intent]);

  if (!open || !parsed) {
    return null;
  }

  function updateOverlayParams(next) {
    const params = new URLSearchParams((location.search || "").replace(/^\?/, ""));
    if (next.mode) {
      params.set("auth", next.mode);
    }
    if (next.intent) {
      params.set("intent", next.intent);
    }
    if (next.returnUrl == null || next.returnUrl === "") {
      params.delete("returnUrl");
    } else {
      params.set("returnUrl", next.returnUrl);
    }
    if (next.registered) {
      params.set("registered", "1");
    } else {
      params.delete("registered");
    }
    const search = params.toString();
    navigate({ pathname: location.pathname, search: search ? `?${search}` : "" }, { replace: true });
  }

  function setMode(mode) {
    updateOverlayParams({
      mode,
      intent: mode === AUTH_OVERLAY_REGISTER ? "merchant" : parsed.intent,
      returnUrl: parsed.returnUrl,
      registered: mode === AUTH_OVERLAY_LOGIN ? parsed.registered : false,
    });
  }

  const labelledBy =
    parsed.mode === AUTH_OVERLAY_LOGIN ? "auth-overlay-login-title" : "auth-overlay-register-title";
  const isLoginMode = parsed.mode === AUTH_OVERLAY_LOGIN;

  function onTabsKeyDown(e) {
    if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(e.key)) return;
    e.preventDefault();
    const order = [AUTH_OVERLAY_LOGIN, AUTH_OVERLAY_REGISTER];
    const currentIdx = order.indexOf(parsed.mode);
    if (e.key === "Home") {
      setMode(order[0]);
      return;
    }
    if (e.key === "End") {
      setMode(order[order.length - 1]);
      return;
    }
    const delta = e.key === "ArrowRight" ? 1 : -1;
    const nextIdx = (currentIdx + delta + order.length) % order.length;
    setMode(order[nextIdx]);
  }

  return (
    <div className="auth-overlay-root" role="presentation" onClick={close}>
      <div
        ref={panelRef}
        className="auth-overlay-panel-wrap"
        role="dialog"
        aria-modal="true"
        aria-labelledby={labelledBy}
        onClick={(e) => e.stopPropagation()}
      >
        <div
          className="auth-overlay-tabs"
          role="tablist"
          aria-label={t("authOverlayTabsAria")}
          onKeyDown={onTabsKeyDown}
        >
          <button
            type="button"
            role="tab"
            id="auth-overlay-tab-login"
            aria-selected={parsed.mode === AUTH_OVERLAY_LOGIN}
            aria-controls="auth-overlay-login-panel"
            tabIndex={parsed.mode === AUTH_OVERLAY_LOGIN ? 0 : -1}
            className={`auth-overlay-tab${parsed.mode === AUTH_OVERLAY_LOGIN ? " is-active" : ""}`}
            onClick={() => setMode(AUTH_OVERLAY_LOGIN)}
          >
            {t("authOverlayTabLogin")}
          </button>
          <button
            type="button"
            role="tab"
            id="auth-overlay-tab-register"
            aria-selected={parsed.mode === AUTH_OVERLAY_REGISTER}
            aria-controls="auth-overlay-register-panel"
            tabIndex={parsed.mode === AUTH_OVERLAY_REGISTER ? 0 : -1}
            className={`auth-overlay-tab${parsed.mode === AUTH_OVERLAY_REGISTER ? " is-active" : ""}`}
            onClick={() => setMode(AUTH_OVERLAY_REGISTER)}
          >
            {t("authOverlayTabRegister")}
          </button>
        </div>
        <div
          role="tabpanel"
          id="auth-overlay-login-panel"
          aria-labelledby="auth-overlay-tab-login"
          hidden={!isLoginMode}
          aria-hidden={!isLoginMode}
        >
          {isLoginMode ? (
            <UnifiedLoginForm
              intent={parsed.intent}
              returnUrl={parsed.returnUrl}
              registered={parsed.registered}
              variant="overlay"
            />
          ) : null}
        </div>
        <div
          role="tabpanel"
          id="auth-overlay-register-panel"
          aria-labelledby="auth-overlay-tab-register"
          hidden={isLoginMode}
          aria-hidden={isLoginMode}
        >
          {!isLoginMode ? (
            <MerchantRegisterForm variant="overlay" />
          ) : null}
        </div>
        <button
          type="button"
          className="auth-overlay-close"
          onClick={close}
          aria-label={t("authOverlayCloseAria")}
        >
          <X className="auth-overlay-close-icon" aria-hidden size={20} strokeWidth={2} />
        </button>
      </div>
    </div>
  );
}
