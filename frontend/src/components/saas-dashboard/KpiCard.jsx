const trendStyles = {
  up: "text-emerald-400",
  down: "text-rose-400",
  neutral: "text-slate-400",
};

export function KpiCard({ label, value, delta, trend = "neutral" }) {
  const trendClass = trendStyles[trend] ?? trendStyles.neutral;
  return (
    <article className="rounded-card border border-slate-800/90 bg-slate-900/55 p-5 shadow-card backdrop-blur-sm transition-shadow duration-200 ease-out hover:shadow-card-hover">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-2 text-2xl font-semibold tracking-tight text-white tabular-nums">{value}</p>
      {delta ? <p className={`mt-2 text-sm font-medium ${trendClass}`}>{delta}</p> : null}
    </article>
  );
}
