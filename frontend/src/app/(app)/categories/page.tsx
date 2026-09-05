"use client";

import { useEffect, useState } from "react";
import { categoryApi, ApiError } from "@/lib/api";
import type { Category, TransactionType } from "@/lib/types";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select, FieldError } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { Spinner } from "@/components/ui/spinner";
import { Alert } from "@/components/ui/badge";
import { Plus, Tag, Trash2 } from "lucide-react";

export default function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [deleting, setDeleting] = useState<Category | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  async function load() {
    setLoading(true);
    const res = await categoryApi.list();
    setCategories(res.categories);
    setLoading(false);
  }

  useEffect(() => {
    load();
  }, []);

  const income = categories.filter((c) => c.type === "INCOME");
  const expense = categories.filter((c) => c.type === "EXPENSE");

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Categories</h1>
          <p className="mt-1 text-sm text-slate-500">Default categories plus your own custom ones.</p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="size-4" />
          New category
        </Button>
      </div>

      {loading ? (
        <div className="flex h-40 items-center justify-center">
          <Spinner />
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <CategoryGroup title="Income" tone="emerald" categories={income} onDelete={setDeleting} />
          <CategoryGroup title="Expense" tone="rose" categories={expense} onDelete={setDeleting} />
        </div>
      )}

      <CreateCategoryModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={() => {
          setCreateOpen(false);
          load();
        }}
      />

      <Modal
        open={!!deleting}
        onClose={() => {
          setDeleting(null);
          setDeleteError(null);
        }}
        title="Delete category"
      >
        <div className="space-y-3">
          {deleteError && <Alert>{deleteError}</Alert>}
          <p className="text-sm text-slate-600">
            Delete <span className="font-medium">{deleting?.name}</span>? Categories still used by a transaction
            can&apos;t be deleted.
          </p>
        </div>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={() => setDeleting(null)}>
            Cancel
          </Button>
          <Button
            variant="danger"
            onClick={async () => {
              if (!deleting) return;
              setDeleteError(null);
              try {
                await categoryApi.remove(deleting.name);
                setDeleting(null);
                load();
              } catch (err) {
                setDeleteError(err instanceof ApiError ? err.message : "Couldn't delete this category.");
              }
            }}
          >
            Delete
          </Button>
        </div>
      </Modal>
    </div>
  );
}

function CategoryGroup({
  title,
  tone,
  categories,
  onDelete,
}: {
  title: string;
  tone: "emerald" | "rose";
  categories: Category[];
  onDelete: (c: Category) => void;
}) {
  const dot = tone === "emerald" ? "bg-emerald-500" : "bg-rose-500";
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <span className={`size-2 rounded-full ${dot}`} />
          {title}
        </CardTitle>
      </CardHeader>
      <CardBody className="p-0">
        <ul className="divide-y divide-slate-100">
          {categories.map((c) => (
            <li key={c.name} className="flex items-center justify-between px-5 py-3">
              <span className="flex items-center gap-2 text-sm text-slate-700">
                <Tag className="size-3.5 text-slate-300" />
                {c.name}
                {c.isCustom && (
                  <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-500">
                    custom
                  </span>
                )}
              </span>
              {c.isCustom && (
                <button
                  onClick={() => onDelete(c)}
                  className="rounded-lg p-1.5 text-slate-400 hover:bg-rose-50 hover:text-rose-600 cursor-pointer"
                >
                  <Trash2 className="size-4" />
                </button>
              )}
            </li>
          ))}
        </ul>
      </CardBody>
    </Card>
  );
}

function CreateCategoryModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}) {
  const [name, setName] = useState("");
  const [type, setType] = useState<TransactionType>("EXPENSE");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      setName("");
      setType("EXPENSE");
      setError(null);
    }
  }, [open]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSaving(true);
    try {
      await categoryApi.create({ name, type });
      onCreated();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't create this category.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="New category">
      <form onSubmit={onSubmit} className="space-y-4">
        <FieldError>{error}</FieldError>
        <div>
          <Label htmlFor="cat-name">Name</Label>
          <Input id="cat-name" required value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Freelance" />
        </div>
        <div>
          <Label htmlFor="cat-type">Type</Label>
          <Select id="cat-type" value={type} onChange={(e) => setType(e.target.value as TransactionType)}>
            <option value="INCOME">Income</option>
            <option value="EXPENSE">Expense</option>
          </Select>
        </div>
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={saving}>
            Create
          </Button>
        </div>
      </form>
    </Modal>
  );
}
