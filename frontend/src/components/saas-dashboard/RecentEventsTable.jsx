const statusBadge = {
  ok: "bg-emerald-500/15 text-emerald-400 ring-1 ring-emerald-500/30",
  warn: "bg-amber-500/15 text-amber-400 ring-1 ring-amber-500/30",
  err: "bg-rose-500/15 text-rose-400 ring-1 ring-rose-500/30",
};

export function RecentEventsTable({ rows }) {
  return (
    <div className="overflow-x-auto rounded-lg border border-slate-800/80">
      <table className="w-full min-w-[520px] border-collapse text-left text-sm">
        <thead>
          <tr className="border-b border-slate-800 bg-slate-900/80">
            <th className="px-4 py-3 font-semibold text-slate-400">Time</th>
            <th className="px-4 py-3 font-semibold text-slate-400">Event</th>
            <th className="px-4 py-3 font-semibold text-slate-400">User</th>
            <th className="px-4 py-3 font-semibold text-slate-400">Status</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-800/90">
          {rows.map((row) => (
            <tr
              key={row.id}
              className="bg-slate-900/30 transition-colors hover:bg-slate-800/40"
            >
              <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-400">
                {row.time}
              </td>
              <td className="px-4 py-3 text-slate-200">{row.event}</td>
              <td className="px-4 py-3 font-mono text-xs text-slate-500">{row.user}</td>
              <td className="px-4 py-3">
                <span
                  className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold capitalize ${statusBadge[row.status] ?? statusBadge.ok}`}
                >
                  {row.status}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
