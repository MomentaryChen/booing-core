import { NavLink } from "react-router-dom";

const secondaryNav = ["Analytics", "Customers", "Settings"];

export function Sidebar() {
  return (
    <aside className="flex w-60 shrink-0 flex-col border-r border-slate-800/90 bg-slate-950/80">
      <div className="flex h-14 items-center border-b border-slate-800/90 px-5">
        <span className="text-sm font-bold tracking-tight text-white">
          Acme<span className="text-primary">.</span>
        </span>
      </div>
      <nav className="flex flex-1 flex-col gap-0.5 p-3" aria-label="Main">
        <NavLink
          to="/demo/saas-dashboard"
          end
          className={({ isActive }) =>
            `rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
              isActive
                ? "bg-primary/15 text-primary"
                : "text-slate-400 hover:bg-slate-800/60 hover:text-slate-200"
            }`
          }
        >
          Overview
        </NavLink>
        {secondaryNav.map((label) => (
          <span
            key={label}
            className="cursor-default rounded-lg px-3 py-2.5 text-sm font-medium text-slate-600"
            title="Demo UI only"
          >
            {label}
          </span>
        ))}
      </nav>
      <div className="border-t border-slate-800/90 p-3">
        <NavLink
          to="/"
          className="block rounded-lg px-3 py-2.5 text-sm font-medium text-slate-500 transition-colors hover:bg-slate-800/60 hover:text-slate-300"
        >
          ← Booking Core home
        </NavLink>
      </div>
    </aside>
  );
}
