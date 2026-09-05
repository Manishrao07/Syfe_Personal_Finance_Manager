"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { goalApi, reportApi, transactionApi } from "@/lib/api";
import type { Goal, MonthlyReport, Transaction } from "@/lib/types";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/card";
import { Spinner } from "@/components/ui/spinner";
import { TypeBadge } from "@/components/ui/badge";
import { formatCurrency, formatDate, MONTH_NAMES } from "@/lib/utils";
import { ArrowDownRight, ArrowUpRight, PiggyBank, TrendingUp } from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export default function DashboardPage() {
  const [loading, setLoading] = useState(true);
  const [report, setReport] = useState<MonthlyReport | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [goals, setGoals] = useState<Goal[]>([]);
  const [error, setError] = useState<string | null>(null);

  const now = useMemo(() => new Date(), []);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      reportApi.monthly(now.getFullYear(), now.getMonth() + 1),
      transactionApi.list(),
      goalApi.list(),
    ])
      .then(([reportRes, txRes, goalRes]) => {
        if (cancelled) return;
        setReport(reportRes);
        setTransactions(txRes.transactions.slice(0, 5));
        setGoals(goalRes.goals.slice(0, 3));
      })
      .catch(() => !cancelled && setError("Couldn't load your dashboard. Please refresh."))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [now]);

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner className="size-6" />
      </div>
    );
  }

  if (error) return <p className="text-sm text-rose-600">{error}</p>;

  const totalIncome = Object.values(report?.totalIncome ?? {}).reduce((a, b) => a + b, 0);
  const totalExpenses = Object.values(report?.totalExpenses ?? {}).reduce((a, b) => a + b, 0);

  const chartData = Array.from(
    new Set([...Object.keys(report?.totalIncome ?? {}), ...Object.keys(report?.totalExpenses ?? {})])
  ).map((category) => ({
    category,
    income: report?.totalIncome[category] ?? 0,
    expense: report?.totalExpenses[category] ?? 0,
  }));

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">
          {MONTH_NAMES[now.getMonth()]} overview
        </h1>
        <p className="mt-1 text-sm text-slate-500">Here&apos;s how your money moved this month.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <SummaryCard
          label="Income"
          value={formatCurrency(totalIncome)}
          icon={<ArrowUpRight className="size-4.5" />}
          tone="emerald"
        />
        <SummaryCard
          label="Expenses"
          value={formatCurrency(totalExpenses)}
          icon={<ArrowDownRight className="size-4.5" />}
          tone="rose"
        />
        <SummaryCard
          label="Net savings"
          value={formatCurrency(report?.netSavings ?? 0)}
          icon={<TrendingUp className="size-4.5" />}
          tone="indigo"
        />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Income vs. expenses by category</CardTitle>
          </CardHeader>
          <CardBody>
            {chartData.length === 0 ? (
              <p className="py-10 text-center text-sm text-slate-400">No transactions this month yet.</p>
            ) : (
              <div className="h-64 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                    <XAxis dataKey="category" tick={{ fontSize: 12, fill: "#64748b" }} axisLine={false} tickLine={false} />
                    <YAxis tick={{ fontSize: 12, fill: "#64748b" }} axisLine={false} tickLine={false} width={48} />
                    <Tooltip
                      formatter={(value) => formatCurrency(Number(value))}
                      contentStyle={{ borderRadius: 12, borderColor: "#e2e8f0", fontSize: 13 }}
                    />
                    <Bar dataKey="income" fill="#10b981" radius={[6, 6, 0, 0]} name="Income" />
                    <Bar dataKey="expense" fill="#f43f5e" radius={[6, 6, 0, 0]} name="Expense" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </CardBody>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Savings goals</CardTitle>
            <Link href="/goals" className="text-xs font-medium text-indigo-600 hover:text-indigo-500">
              View all
            </Link>
          </CardHeader>
          <CardBody className="space-y-4">
            {goals.length === 0 ? (
              <EmptyHint text="No goals yet." linkHref="/goals" linkLabel="Create one" />
            ) : (
              goals.map((goal) => (
                <div key={goal.id}>
                  <div className="mb-1.5 flex items-center justify-between text-sm">
                    <span className="font-medium text-slate-800 flex items-center gap-1.5">
                      <PiggyBank className="size-3.5 text-indigo-500" />
                      {goal.goalName}
                    </span>
                    <span className="text-slate-500">{goal.progressPercentage.toFixed(1)}%</span>
                  </div>
                  <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
                    <div
                      className="h-full rounded-full bg-indigo-500 transition-all"
                      style={{ width: `${Math.min(100, Math.max(0, goal.progressPercentage))}%` }}
                    />
                  </div>
                </div>
              ))
            )}
          </CardBody>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Recent transactions</CardTitle>
          <Link href="/transactions" className="text-xs font-medium text-indigo-600 hover:text-indigo-500">
            View all
          </Link>
        </CardHeader>
        <CardBody className="p-0">
          {transactions.length === 0 ? (
            <div className="p-5">
              <EmptyHint text="No transactions yet." linkHref="/transactions" linkLabel="Add one" />
            </div>
          ) : (
            <ul className="divide-y divide-slate-100">
              {transactions.map((tx) => (
                <li key={tx.id} className="flex items-center justify-between px-5 py-3.5">
                  <div>
                    <p className="text-sm font-medium text-slate-800">{tx.description || tx.category}</p>
                    <p className="text-xs text-slate-400">
                      {tx.category} · {formatDate(tx.date)}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <TypeBadge type={tx.type} />
                    <span
                      className={`text-sm font-semibold ${tx.type === "INCOME" ? "text-emerald-600" : "text-rose-600"}`}
                    >
                      {tx.type === "INCOME" ? "+" : "-"}
                      {formatCurrency(tx.amount)}
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardBody>
      </Card>
    </div>
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

function EmptyHint({ text, linkHref, linkLabel }: { text: string; linkHref: string; linkLabel: string }) {
  return (
    <p className="text-sm text-slate-400">
      {text}{" "}
      <Link href={linkHref} className="font-medium text-indigo-600 hover:text-indigo-500">
        {linkLabel}
      </Link>
    </p>
  );
}
