import { KpiCard } from "./KpiCard";

export function KpiRow({ items }) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {items.map((item) => (
        <KpiCard
          key={item.id}
          label={item.label}
          value={item.value}
          delta={item.delta}
          trend={item.trend}
        />
      ))}
    </div>
  );
}
