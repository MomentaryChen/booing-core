export function TopNavbar({ title = "Dashboard" }) {
  return (
    <header className="flex h-14 shrink-0 items-center justify-between gap-4 border-b border-slate-800/90 bg-slate-950/60 px-6 backdrop-blur-md">
      <h1 className="text-lg font-semibold tracking-tight text-white">{title}</h1>
      <div className="flex flex-1 items-center justify-end gap-3">
        <label className="hidden max-w-xs flex-1 sm:block">
          <span className="sr-only">Search</span>
          <input
            type="search"
            placeholder="Search…"
            className="w-full rounded-lg border border-slate-800 bg-slate-900/80 px-3 py-2 text-sm text-slate-200 placeholder:text-slate-600 outline-none ring-primary/40 transition-shadow focus:border-primary/50 focus:ring-2"
          />
        </label>
        <button
          type="button"
          className="flex h-9 w-9 items-center justify-center rounded-lg border border-slate-800 bg-slate-900/80 text-slate-400 transition-colors hover:border-slate-700 hover:text-slate-200"
          aria-label="Notifications"
        >
          <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden>
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
            />
          </svg>
        </button>
        <div
          className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-primary to-indigo-600 text-xs font-bold text-white shadow-md ring-2 ring-slate-800"
          aria-hidden
        >
          VC
        </div>
      </div>
    </header>
  );
}
