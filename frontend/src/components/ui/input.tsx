import { cn } from "@/lib/utils";
import type { InputHTMLAttributes, LabelHTMLAttributes, SelectHTMLAttributes } from "react";

export function Label(props: LabelHTMLAttributes<HTMLLabelElement>) {
  return <label {...props} className={cn("block text-sm font-medium text-slate-700 mb-1.5", props.className)} />;
}

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        "w-full rounded-lg border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-900",
        "placeholder:text-slate-400 outline-none transition-shadow",
        "focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100",
        className
      )}
      {...props}
    />
  );
}

export function Select({ className, children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={cn(
        "w-full rounded-lg border border-slate-200 bg-white px-3.5 py-2.5 text-sm text-slate-900",
        "outline-none transition-shadow focus:border-indigo-400 focus:ring-4 focus:ring-indigo-100",
        className
      )}
      {...props}
    >
      {children}
    </select>
  );
}

export function FieldError({ children }: { children?: string | null }) {
  if (!children) return null;
  return <p className="mt-1.5 text-sm text-rose-600">{children}</p>;
}
