"use client";

import { useEffect, useMemo, useState } from "react";
import { categoryApi, transactionApi, ApiError, type TransactionFilters } from "@/lib/api";
import type { Category, Transaction, TransactionType } from "@/lib/types";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select, FieldError } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { Spinner } from "@/components/ui/spinner";
import { TypeBadge } from "@/components/ui/badge";
import { formatCurrency, formatDate, todayIso } from "@/lib/utils";
import { Pencil, Plus, Trash2 } from "lucide-react";

export default function TransactionsPage() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState<TransactionFilters>({});
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Transaction | null>(null);
  const [deleting, setDeleting] = useState<Transaction | null>(null);

  async function loadCategories() {
    const res = await categoryApi.list();
    setCategories(res.categories);
  }

  async function loadTransactions(activeFilters: TransactionFilters) {
    setLoading(true);
    setError(null);
    try {
      const res = await transactionApi.list(activeFilters);
      setTransactions(res.transactions);
    } catch {
      setError("Couldn't load transactions.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadCategories().catch(() => {});
    loadTransactions({});
  }, []);

  function applyFilters(e: React.FormEvent) {
    e.preventDefault();
    loadTransactions(filters);
  }

  function clearFilters() {
    setFilters({});
    loadTransactions({});
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Transactions</h1>
          <p className="mt-1 text-sm text-slate-500">Every income and expense, newest first.</p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="size-4" />
          Add transaction
        </Button>
      </div>

      <Card>
        <CardBody>
          <form onSubmit={applyFilters} className="flex flex-wrap items-end gap-3">
            <div className="w-40">
              <Label htmlFor="startDate">From</Label>
              <Input
                id="startDate"
                type="date"
                value={filters.startDate ?? ""}
                onChange={(e) => setFilters((f) => ({ ...f, startDate: e.target.value || undefined }))}
              />
            </div>
            <div className="w-40">
              <Label htmlFor="endDate">To</Label>
              <Input
                id="endDate"
                type="date"
                value={filters.endDate ?? ""}
                onChange={(e) => setFilters((f) => ({ ...f, endDate: e.target.value || undefined }))}
              />
            </div>
            <div className="w-44">
              <Label htmlFor="category">Category</Label>
              <Select
                id="category"
                value={filters.category ?? ""}
                onChange={(e) => setFilters((f) => ({ ...f, category: e.target.value || undefined }))}
              >
                <option value="">All categories</option>
                {categories.map((c) => (
                  <option key={c.name} value={c.name}>
                    {c.name}
                  </option>
                ))}
              </Select>
            </div>
            <div className="w-36">
              <Label htmlFor="type">Type</Label>
              <Select
                id="type"
                value={filters.type ?? ""}
                onChange={(e) => setFilters((f) => ({ ...f, type: (e.target.value || undefined) as TransactionType }))}
              >
                <option value="">All</option>
                <option value="INCOME">Income</option>
                <option value="EXPENSE">Expense</option>
              </Select>
            </div>
            <Button type="submit" variant="secondary" size="md">
              Apply
            </Button>
            <Button type="button" variant="ghost" size="md" onClick={clearFilters}>
              Clear
            </Button>
          </form>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{transactions.length} transaction{transactions.length === 1 ? "" : "s"}</CardTitle>
        </CardHeader>
        <CardBody className="p-0">
          {loading ? (
            <div className="flex h-40 items-center justify-center">
              <Spinner />
            </div>
          ) : error ? (
            <p className="p-5 text-sm text-rose-600">{error}</p>
          ) : transactions.length === 0 ? (
            <p className="p-5 text-sm text-slate-400">No transactions match these filters.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-left text-xs font-medium uppercase tracking-wide text-slate-400">
                    <th className="px-5 py-3">Date</th>
                    <th className="px-5 py-3">Category</th>
                    <th className="px-5 py-3">Description</th>
                    <th className="px-5 py-3">Type</th>
                    <th className="px-5 py-3 text-right">Amount</th>
                    <th className="px-5 py-3" />
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {transactions.map((tx) => (
                    <tr key={tx.id} className="hover:bg-slate-50/60">
                      <td className="whitespace-nowrap px-5 py-3.5 text-slate-600">{formatDate(tx.date)}</td>
                      <td className="whitespace-nowrap px-5 py-3.5 font-medium text-slate-800">{tx.category}</td>
                      <td className="px-5 py-3.5 text-slate-500">{tx.description || "—"}</td>
                      <td className="whitespace-nowrap px-5 py-3.5">
                        <TypeBadge type={tx.type} />
                      </td>
                      <td
                        className={`whitespace-nowrap px-5 py-3.5 text-right font-semibold ${
                          tx.type === "INCOME" ? "text-emerald-600" : "text-rose-600"
                        }`}
                      >
                        {tx.type === "INCOME" ? "+" : "-"}
                        {formatCurrency(tx.amount)}
                      </td>
                      <td className="whitespace-nowrap px-5 py-3.5">
                        <div className="flex justify-end gap-1">
                          <button
                            onClick={() => setEditing(tx)}
                            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 cursor-pointer"
                          >
                            <Pencil className="size-4" />
                          </button>
                          <button
                            onClick={() => setDeleting(tx)}
                            className="rounded-lg p-1.5 text-slate-400 hover:bg-rose-50 hover:text-rose-600 cursor-pointer"
                          >
                            <Trash2 className="size-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardBody>
      </Card>

      <CreateTransactionModal
        open={createOpen}
        categories={categories}
        onClose={() => setCreateOpen(false)}
        onCreated={() => {
          setCreateOpen(false);
          loadTransactions(filters);
        }}
      />

      <EditTransactionModal
        transaction={editing}
        categories={categories}
        onClose={() => setEditing(null)}
        onSaved={() => {
          setEditing(null);
          loadTransactions(filters);
        }}
      />

      <Modal open={!!deleting} onClose={() => setDeleting(null)} title="Delete transaction">
        <p className="text-sm text-slate-600">
          Delete <span className="font-medium">{deleting?.description || deleting?.category}</span>? This can&apos;t be
          undone.
        </p>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={() => setDeleting(null)}>
            Cancel
          </Button>
          <Button
            variant="danger"
            onClick={async () => {
              if (!deleting) return;
              await transactionApi.remove(deleting.id);
              setDeleting(null);
              loadTransactions(filters);
            }}
          >
            Delete
          </Button>
        </div>
      </Modal>
    </div>
  );
}

function CreateTransactionModal({
  open,
  categories,
  onClose,
  onCreated,
}: {
  open: boolean;
  categories: Category[];
  onClose: () => void;
  onCreated: () => void;
}) {
  const [amount, setAmount] = useState("");
  const [date, setDate] = useState(todayIso());
  const [category, setCategory] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const defaultCategory = useMemo(() => categories[0]?.name ?? "", [categories]);

  useEffect(() => {
    if (open) {
      setAmount("");
      setDate(todayIso());
      setCategory(defaultCategory);
      setDescription("");
      setError(null);
    }
  }, [open, defaultCategory]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSaving(true);
    try {
      await transactionApi.create({ amount: Number(amount), date, category, description: description || undefined });
      onCreated();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't create the transaction.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Add transaction">
      <form onSubmit={onSubmit} className="space-y-4">
        <FieldError>{error}</FieldError>
        <div>
          <Label htmlFor="c-amount">Amount</Label>
          <Input
            id="c-amount"
            type="number"
            step="0.01"
            min="0.01"
            required
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            placeholder="0.00"
          />
        </div>
        <div>
          <Label htmlFor="c-date">Date</Label>
          <Input id="c-date" type="date" required max={todayIso()} value={date} onChange={(e) => setDate(e.target.value)} />
        </div>
        <div>
          <Label htmlFor="c-category">Category</Label>
          <Select id="c-category" required value={category} onChange={(e) => setCategory(e.target.value)}>
            {categories.map((c) => (
              <option key={c.name} value={c.name}>
                {c.name} ({c.type === "INCOME" ? "Income" : "Expense"})
              </option>
            ))}
          </Select>
        </div>
        <div>
          <Label htmlFor="c-description">Description (optional)</Label>
          <Input id="c-description" value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={saving}>
            Add transaction
          </Button>
        </div>
      </form>
    </Modal>
  );
}

function EditTransactionModal({
  transaction,
  categories,
  onClose,
  onSaved,
}: {
  transaction: Transaction | null;
  categories: Category[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [amount, setAmount] = useState("");
  const [category, setCategory] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (transaction) {
      setAmount(String(transaction.amount));
      setCategory(transaction.category);
      setDescription(transaction.description ?? "");
      setError(null);
    }
  }, [transaction]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!transaction) return;
    setError(null);
    setSaving(true);
    try {
      await transactionApi.update(transaction.id, {
        amount: Number(amount),
        category,
        description,
      });
      onSaved();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't update the transaction.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={!!transaction} onClose={onClose} title="Edit transaction">
      <form onSubmit={onSubmit} className="space-y-4">
        <FieldError>{error}</FieldError>
        <div>
          <Label htmlFor="e-amount">Amount</Label>
          <Input
            id="e-amount"
            type="number"
            step="0.01"
            min="0.01"
            required
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="e-date">Date</Label>
          <Input id="e-date" type="date" disabled value={transaction?.date ?? ""} className="bg-slate-50 text-slate-400" />
          <p className="mt-1 text-xs text-slate-400">The date of a transaction can&apos;t be changed.</p>
        </div>
        <div>
          <Label htmlFor="e-category">Category</Label>
          <Select id="e-category" required value={category} onChange={(e) => setCategory(e.target.value)}>
            {categories.map((c) => (
              <option key={c.name} value={c.name}>
                {c.name} ({c.type === "INCOME" ? "Income" : "Expense"})
              </option>
            ))}
          </Select>
        </div>
        <div>
          <Label htmlFor="e-description">Description</Label>
          <Input id="e-description" value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={saving}>
            Save changes
          </Button>
        </div>
      </form>
    </Modal>
  );
}
