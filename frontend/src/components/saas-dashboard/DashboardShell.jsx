import { Sidebar } from "./Sidebar";
import { TopNavbar } from "./TopNavbar";

export function DashboardShell({ children, title }) {
  return (
    <div className="flex min-h-screen w-full overflow-hidden bg-slate-950">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <TopNavbar title={title} />
        <div className="flex-1 overflow-y-auto bg-gradient-to-b from-slate-950 to-slate-900/95 p-6">
          <div className="mx-auto max-w-7xl">{children}</div>
        </div>
      </div>
    </div>
  );
}
