import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

const axisTick = { fill: "#94a3b8", fontSize: 12 };
const gridStroke = "#334155";

function TrafficTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 shadow-xl">
      <p className="text-xs font-medium text-slate-400">{label}</p>
      <p className="text-sm font-semibold text-white tabular-nums">
        {payload[0].value?.toLocaleString()} visits
      </p>
    </div>
  );
}

export function TrafficLineChart({ data }) {
  return (
    <div className="h-[280px] w-full min-h-[240px]">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid stroke={gridStroke} strokeDasharray="4 8" vertical={false} />
          <XAxis dataKey="day" axisLine={false} tickLine={false} tick={axisTick} dy={8} />
          <YAxis
            axisLine={false}
            tickLine={false}
            tick={axisTick}
            width={44}
            tickFormatter={(v) => (v >= 1000 ? `${v / 1000}k` : String(v))}
          />
          <Tooltip content={<TrafficTooltip />} cursor={{ stroke: "#6366f1", strokeWidth: 1 }} />
          <Line
            type="monotone"
            dataKey="visits"
            stroke="#6366f1"
            strokeWidth={2.5}
            dot={{ fill: "#6366f1", strokeWidth: 0, r: 3 }}
            activeDot={{ r: 5, fill: "#818cf8", stroke: "#c7d2fe", strokeWidth: 2 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
