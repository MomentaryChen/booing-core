import { cn } from "../../lib/cn";

const variants = {
  default: "bg-indigo-600 text-white hover:bg-indigo-700 focus-visible:ring-indigo-500",
  secondary:
    "border border-slate-200 bg-white text-slate-900 shadow-sm hover:bg-slate-50 focus-visible:ring-slate-400",
  ghost: "text-slate-700 hover:bg-slate-100 focus-visible:ring-slate-400",
  outline: "border border-slate-200 bg-transparent hover:bg-slate-50 focus-visible:ring-slate-400",
};

const sizes = {
  default: "h-10 px-4 text-sm",
  sm: "h-9 rounded-md px-3 text-sm",
  icon: "h-9 w-9",
};

export function buttonVariants({ variant = "default", size = "default", className } = {}) {
  return cn(
    "inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50",
    variants[variant],
    sizes[size],
    className
  );
}

export function Button({ className, variant = "default", size = "default", type = "button", ...props }) {
  return <button type={type} className={cn(buttonVariants({ variant, size }), className)} {...props} />;
}
