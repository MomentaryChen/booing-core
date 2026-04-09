import { Link } from "react-router-dom";
import { ArrowRight, CalendarRange, ListOrdered, Store } from "lucide-react";
import { useI18n } from "../../i18n";
import { DashboardPageShell } from "../../components/shell/DashboardPageShell";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { buttonVariants } from "../../components/ui/button";
import { cn } from "../../lib/cn";

const steps = [
  { icon: Store, titleKey: "clientHomeStep1Title", bodyKey: "clientHomeStep1Body" },
  { icon: CalendarRange, titleKey: "clientHomeStep2Title", bodyKey: "clientHomeStep2Body" },
  { icon: ListOrdered, titleKey: "clientHomeStep3Title", bodyKey: "clientHomeStep3Body" },
];

export function ClientTodoPage() {
  const { t } = useI18n();

  return (
    <div className="mx-auto max-w-3xl">
      <DashboardPageShell>
        <header className="mb-8 space-y-2">
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900 sm:text-3xl">
            {t("clientHomeTitle")}
          </h1>
          <p className="text-sm leading-relaxed text-slate-600 sm:text-base">{t("clientHomeSubtitle")}</p>
        </header>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <span className="flex size-8 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
                <ListOrdered className="size-4" aria-hidden />
              </span>
              {t("clientHomeHowTitle")}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-5">
            <ul className="space-y-4">
              {steps.map(({ icon: Icon, titleKey, bodyKey }) => (
                <li key={titleKey} className="flex gap-3 rounded-lg border border-slate-100 bg-slate-50/50 p-4">
                  <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-md bg-white text-indigo-600 shadow-sm ring-1 ring-slate-200/80">
                    <Icon className="size-4" aria-hidden />
                  </span>
                  <div className="min-w-0 space-y-1">
                    <p className="font-semibold text-slate-900">{t(titleKey)}</p>
                    <p className="text-sm text-slate-600">{t(bodyKey)}</p>
                  </div>
                </li>
              ))}
            </ul>
            <div className="pt-2">
              <Link
                to="/client/booking/demo-merchant"
                className={cn(buttonVariants({ variant: "default" }), "inline-flex")}
              >
                {t("clientHomeCtaDemo")}
                <ArrowRight className="size-4" aria-hidden />
              </Link>
            </div>
          </CardContent>
        </Card>
      </DashboardPageShell>
    </div>
  );
}
