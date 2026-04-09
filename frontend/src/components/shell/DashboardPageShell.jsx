import { cn } from "../../lib/cn";

/**
 * Stripe-like shell: subtle gradient, grid, blur blobs. Decorative layers are non-interactive.
 */
export function DashboardPageShell({ children, className, contentClassName }) {
  return (
    <div
      className={cn(
        "relative isolate overflow-hidden rounded-2xl border border-slate-200/70 bg-gradient-to-b from-white via-slate-50/90 to-slate-100/50 shadow-sm",
        className
      )}
    >
      <div
        className="pointer-events-none absolute inset-0 bg-[linear-gradient(to_right,#cbd5e1_1px,transparent_1px),linear-gradient(to_bottom,#cbd5e1_1px,transparent_1px)] bg-[length:24px_24px] opacity-[0.14]"
        aria-hidden
      />
      <div
        className="pointer-events-none absolute -top-24 right-0 h-72 w-72 rounded-full bg-indigo-500/10 blur-3xl motion-reduce:opacity-60"
        aria-hidden
      />
      <div
        className="pointer-events-none absolute bottom-0 left-0 h-64 w-64 rounded-full bg-slate-400/10 blur-3xl motion-reduce:opacity-60"
        aria-hidden
      />
      <div className={cn("relative z-10 p-6 sm:p-8", contentClassName)}>{children}</div>
    </div>
  );
}
