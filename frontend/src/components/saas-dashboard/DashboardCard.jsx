/**
 * Stripe-style elevated panel: 12px radius, hover shadow lift.
 */
export function DashboardCard({ title, subtitle, children, className = "" }) {
  return (
    <section
      className={`rounded-card border border-slate-800/90 bg-slate-900/55 p-6 shadow-card backdrop-blur-sm transition-shadow duration-200 ease-out hover:shadow-card-hover ${className}`}
    >
      {(title || subtitle) && (
        <header className="mb-5">
          {title ? (
            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-400">{title}</h2>
          ) : null}
          {subtitle ? <p className="mt-1 text-xs text-slate-500">{subtitle}</p> : null}
        </header>
      )}
      {children}
    </section>
  );
}
