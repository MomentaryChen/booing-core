import { useCallback, useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { X } from "lucide-react";
import {
  AUTH_OVERLAY_LOGIN,
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
  const open = !!parsed && location.pathname !== "/login";

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

  const labelledBy =
    parsed.mode === AUTH_OVERLAY_LOGIN ? "auth-overlay-login-title" : "auth-overlay-register-title";
  const isLoginMode = parsed.mode === AUTH_OVERLAY_LOGIN;

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
        <div className="auth-overlay-chrome">
          <span className="auth-overlay-title" aria-hidden>
            {isLoginMode ? t("authOverlayTabLogin") : t("authOverlayTabRegister")}
          </span>
          <button
            type="button"
            className="auth-overlay-close"
            onClick={close}
            aria-label={t("authOverlayCloseAria")}
          >
            <X className="auth-overlay-close-icon" aria-hidden size={20} strokeWidth={2} />
          </button>
        </div>
        <div className="auth-overlay-body">
          <div hidden={!isLoginMode} aria-hidden={!isLoginMode}>
            {isLoginMode ? (
              <UnifiedLoginForm
                intent={parsed.intent}
                returnUrl={parsed.returnUrl}
                registered={parsed.registered}
                variant="overlay"
              />
            ) : null}
          </div>
          <div hidden={isLoginMode} aria-hidden={isLoginMode}>
            {!isLoginMode ? (
              <MerchantRegisterForm variant="overlay" />
            ) : null}
          </div>
        </div>
      </div>
    </div>
  );
}
