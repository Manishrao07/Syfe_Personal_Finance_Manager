"use client";

import { useEffect, useMemo, useState } from "react";
import { reportApi, ApiError } from "@/lib/api";
import type { MonthlyReport, YearlyReport } from "@/lib/types";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/card";
import { Select } from "@/components/ui/input";
import { Spinner } from "@/components/ui/spinner";
import { formatCurrency, MONTH_NAMES } from "@/lib/utils";
import { ArrowDownRight, ArrowUpRight, TrendingUp } from "lucide-react";
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";

const CURRENT_YEAR = new Date().getFullYear();
const YEARS = Array.from({ length: 6 }, (_, i) => CURRENT_YEAR - i);
const COLORS = ["#6366f1", "#10b981", "#f43f5e", "#f59e0b", "#0ea5e9", "#a855f7", "#14b8a6"];

export default function ReportsPage() {
  const [mode, setMode] = useState<"monthly" | "yearly">("monthly");
  const [year, setYear] = useState(CURRENT_YEAR);
  const [month, setMonth] = useState(new Date().getMonth() + 1);
  const [report, setReport] = useState<MonthlyReport | YearlyReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    const call = mode === "monthly" ? reportApi.monthly(year, month) : reportApi.yearly(year);
    call
      .then(setReport)
      .catch((err) => setError(err instanceof ApiError ? err.message : "Couldn't load this report."))
      .finally(() => setLoading(false));
  }, [mode, year, month]);

  const totalIncome = useMemo(
    () => Object.values(report?.totalIncome ?? {}).reduce((a, b) => a + b, 0),
    [report]
  );
  const totalExpenses = useMemo(
    () => Object.values(report?.totalExpenses ?? {}).reduce((a, b) => a + b, 0),
    [report]
  );

  const expenseSlices = useMemo(
    () => Object.entries(report?.totalExpenses ?? {}).map(([name, value]) => ({ name, value })),
    [report]
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Reports</h1>
        <p className="mt-1 text-sm text-slate-500">Income and expenses broken down by category.</p>
      </div>

      <Card>
        <CardBody className="flex flex-wrap items-end gap-3">
          <div className="w-36">
            <Select value={mode} onChange={(e) => setMode(e.target.value as "monthly" | "yearly")}>
              <option value="monthly">Monthly</option>
              <option value="yearly">Yearly</option>
            </Select>
          </div>
          {mode === "monthly" && (
            <div className="w-40">
              <Select value={month} onChange={(e) => setMonth(Number(e.target.value))}>
                {MONTH_NAMES.map((m, i) => (
                  <option key={m} value={i + 1}>
                    {m}
                  </option>
                ))}
              </Select>
            </div>
          )}
          <div className="w-28">
            <Select value={year} onChange={(e) => setYear(Number(e.target.value))}>
              {YEARS.map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </Select>
          </div>
        </CardBody>
      </Card>

      {loading ? (
        <div className="flex h-64 items-center justify-center">
          <Spinner className="size-6" />
        </div>
      ) : error ? (
        <p className="text-sm text-rose-600">{error}</p>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <SummaryCard label="Total income" value={formatCurrency(totalIncome)} icon={<ArrowUpRight className="size-4.5" />} tone="emerald" />
            <SummaryCard label="Total expenses" value={formatCurrency(totalExpenses)} icon={<ArrowDownRight className="size-4.5" />} tone="rose" />
            <SummaryCard label="Net savings" value={formatCurrency(report?.netSavings ?? 0)} icon={<TrendingUp className="size-4.5" />} tone="indigo" />
          </div>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>Income by category</CardTitle>
              </CardHeader>
              <CardBody>
                <CategoryList data={report?.totalIncome ?? {}} tone="emerald" />
              </CardBody>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Expenses by category</CardTitle>
              </CardHeader>
              <CardBody className="space-y-4">
                {expenseSlices.length > 0 && (
                  <div className="h-52 w-full">
                    <ResponsiveContainer width="100%" height="100%">
                      <PieChart>
                        <Pie
                          data={expenseSlices}
                          dataKey="value"
                          nameKey="name"
                          innerRadius={45}
                          outerRadius={75}
                          paddingAngle={expenseSlices.length > 1 ? 2 : 0}
                          isAnimationActive={false}
                        >
                          {expenseSlices.map((_, i) => (
                            <Cell key={i} fill={COLORS[i % COLORS.length]} />
                          ))}
                        </Pie>
                        <Tooltip formatter={(value) => formatCurrency(Number(value))} contentStyle={{ borderRadius: 12, fontSize: 13 }} />
                      </PieChart>
                    </ResponsiveContainer>
                  </div>
                )}
                <CategoryList data={report?.totalExpenses ?? {}} tone="rose" />
              </CardBody>
            </Card>
          </div>
        </>
      )}
    </div>
  );
}

function CategoryList({ data, tone }: { data: Record<string, number>; tone: "emerald" | "rose" }) {
  const entries = Object.entries(data);
  if (entries.length === 0) {
    return <p className="text-sm text-slate-400">No activity in this period.</p>;
  }
  const dot = tone === "emerald" ? "bg-emerald-500" : "bg-rose-500";
  return (
    <ul className="space-y-2.5">
      {entries.map(([name, value]) => (
        <li key={name} className="flex items-center justify-between text-sm">
          <span className="flex items-center gap-2 text-slate-600">
            <span className={`size-2 rounded-full ${dot}`} />
            {name}
          </span>
          <span className="font-medium text-slate-900">{formatCurrency(value)}</span>
        </li>
      ))}
    </ul>
  );
}

function SummaryCard({
  label,
  value,
  icon,
  tone,
}: {
  label: string;
  value: string;
  icon: React.ReactNode;
  tone: "emerald" | "rose" | "indigo";
}) {
  const toneClasses = {
    emerald: "bg-emerald-50 text-emerald-600",
    rose: "bg-rose-50 text-rose-600",
    indigo: "bg-indigo-50 text-indigo-600",
  }[tone];

  return (
    <Card>
      <CardBody className="flex items-center gap-4">
        <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${toneClasses}`}>{icon}</div>
        <div>
          <p className="text-xs font-medium text-slate-500">{label}</p>
          <p className="text-lg font-semibold text-slate-900">{value}</p>
        </div>
      </CardBody>
    </Card>
  );
}
