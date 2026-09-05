import { cn } from "@/lib/utils";
import type { TransactionType } from "@/lib/types";

export function TypeBadge({ type }: { type: TransactionType }) {
  const isIncome = type === "INCOME";
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
        isIncome ? "bg-emerald-50 text-emerald-700" : "bg-rose-50 text-rose-700"
      )}
    >
      {isIncome ? "Income" : "Expense"}
    </span>
  );
}

export function Alert({ children, variant = "error" }: { children: React.ReactNode; variant?: "error" | "success" }) {
  return (
    <div
      className={cn(
        "rounded-lg px-3.5 py-2.5 text-sm",
        variant === "error" ? "bg-rose-50 text-rose-700" : "bg-emerald-50 text-emerald-700"
      )}
    >
      {children}
    </div>
  );
}
