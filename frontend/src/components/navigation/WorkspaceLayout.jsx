import { NavLink, Outlet } from "react-router-dom";
import { useI18n } from "../../i18n";
import { useNavigation } from "../../navigation/NavigationContext";

export function WorkspaceLayout() {
  const { t } = useI18n();
  const { items, loading, error, refresh } = useNavigation();

  const navItems = Array.isArray(items)
    ? [...items].sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0))
    : [];

  return (
    <div className="workspace-shell">
      <aside className="workspace-sidebar" aria-label="Primary">
        <nav className="workspace-sidebar__nav" aria-label="Main">
          {loading && (
            <div className="workspace-sidebar__state" role="status">
              {t("navSidebarLoading")}
            </div>
          )}

          {!loading && error && (
            <div className="workspace-sidebar__state workspace-sidebar__state--error" role="alert">
              <p>{t("navSidebarError")}</p>
              <button type="button" className="workspace-sidebar__retry" onClick={() => refresh()}>
                {t("navSidebarRetry")}
              </button>
            </div>
          )}

          {!loading && !error && navItems.length === 0 && (
            <div className="workspace-sidebar__state">{t("navSidebarEmpty")}</div>
          )}

          {!loading &&
            !error &&
            navItems.map((item) => (
              <NavLink
                key={item.routeKey}
                to={item.path}
                end={item.path === "/system" || item.path === "/merchant" || item.path === "/client"}
                className={({ isActive }) =>
                  `workspace-sidebar__item${isActive ? " is-active" : ""}`
                }
              >
                {t(item.labelKey)}
              </NavLink>
            ))}
        </nav>
      </aside>
      <section className="workspace-content">
        <Outlet />
      </section>
    </div>
  );
}
