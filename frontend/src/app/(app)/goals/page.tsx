"use client";

import { useEffect, useState } from "react";
import { goalApi, ApiError } from "@/lib/api";
import type { Goal } from "@/lib/types";
import { Card, CardBody } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, FieldError } from "@/components/ui/input";
import { Modal } from "@/components/ui/modal";
import { Spinner } from "@/components/ui/spinner";
import { formatCurrency, formatDate, todayIso } from "@/lib/utils";
import { Pencil, Plus, Target, Trash2 } from "lucide-react";

export default function GoalsPage() {
  const [goals, setGoals] = useState<Goal[]>([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Goal | null>(null);
  const [deleting, setDeleting] = useState<Goal | null>(null);

  async function load() {
    setLoading(true);
    const res = await goalApi.list();
    setGoals(res.goals);
    setLoading(false);
  }

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Savings goals</h1>
          <p className="mt-1 text-sm text-slate-500">Progress = total income − total expenses since the start date.</p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="size-4" />
          New goal
        </Button>
      </div>

      {loading ? (
        <div className="flex h-40 items-center justify-center">
          <Spinner />
        </div>
      ) : goals.length === 0 ? (
        <Card>
          <CardBody className="py-14 text-center">
            <Target className="mx-auto size-8 text-slate-300" />
            <p className="mt-3 text-sm text-slate-500">You don&apos;t have any savings goals yet.</p>
            <Button className="mt-4" size="sm" onClick={() => setCreateOpen(true)}>
              Create your first goal
            </Button>
          </CardBody>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {goals.map((goal) => (
            <GoalCard key={goal.id} goal={goal} onEdit={() => setEditing(goal)} onDelete={() => setDeleting(goal)} />
          ))}
        </div>
      )}

      <CreateGoalModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={() => {
          setCreateOpen(false);
          load();
        }}
      />

      <EditGoalModal
        goal={editing}
        onClose={() => setEditing(null)}
        onSaved={() => {
          setEditing(null);
          load();
        }}
      />

      <Modal open={!!deleting} onClose={() => setDeleting(null)} title="Delete goal">
        <p className="text-sm text-slate-600">
          Delete <span className="font-medium">{deleting?.goalName}</span>? This can&apos;t be undone.
        </p>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={() => setDeleting(null)}>
            Cancel
          </Button>
          <Button
            variant="danger"
            onClick={async () => {
              if (!deleting) return;
              await goalApi.remove(deleting.id);
              setDeleting(null);
              load();
            }}
          >
            Delete
          </Button>
        </div>
      </Modal>
    </div>
  );
}

function GoalCard({ goal, onEdit, onDelete }: { goal: Goal; onEdit: () => void; onDelete: () => void }) {
  const pct = Math.min(100, Math.max(0, goal.progressPercentage));
  const onTrack = goal.progressPercentage >= 0;

  return (
    <Card>
      <CardBody className="space-y-4">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600">
              <Target className="size-4.5" />
            </div>
            <div>
              <p className="font-medium text-slate-900">{goal.goalName}</p>
              <p className="text-xs text-slate-400">Target {formatDate(goal.targetDate)}</p>
            </div>
          </div>
          <div className="flex gap-1">
            <button onClick={onEdit} className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 cursor-pointer">
              <Pencil className="size-4" />
            </button>
            <button onClick={onDelete} className="rounded-lg p-1.5 text-slate-400 hover:bg-rose-50 hover:text-rose-600 cursor-pointer">
              <Trash2 className="size-4" />
            </button>
          </div>
        </div>

        <div>
          <div className="mb-1.5 flex items-baseline justify-between">
            <span className="text-lg font-semibold text-slate-900">{formatCurrency(goal.currentProgress)}</span>
            <span className="text-sm text-slate-400">of {formatCurrency(goal.targetAmount)}</span>
          </div>
          <div className="h-2.5 w-full overflow-hidden rounded-full bg-slate-100">
            <div
              className={`h-full rounded-full transition-all ${onTrack ? "bg-indigo-500" : "bg-rose-500"}`}
              style={{ width: `${pct}%` }}
            />
          </div>
          <div className="mt-1.5 flex items-center justify-between text-xs text-slate-400">
            <span>{goal.progressPercentage.toFixed(1)}% complete</span>
            <span>{formatCurrency(goal.remainingAmount)} to go</span>
          </div>
        </div>
      </CardBody>
    </Card>
  );
}

function CreateGoalModal({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: () => void }) {
  const [goalName, setGoalName] = useState("");
  const [targetAmount, setTargetAmount] = useState("");
  const [targetDate, setTargetDate] = useState("");
  const [startDate, setStartDate] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      setGoalName("");
      setTargetAmount("");
      setTargetDate("");
      setStartDate("");
      setError(null);
    }
  }, [open]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSaving(true);
    try {
      await goalApi.create({
        goalName,
        targetAmount: Number(targetAmount),
        targetDate,
        startDate: startDate || undefined,
      });
      onCreated();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't create this goal.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="New savings goal">
      <form onSubmit={onSubmit} className="space-y-4">
        <FieldError>{error}</FieldError>
        <div>
          <Label htmlFor="g-name">Goal name</Label>
          <Input id="g-name" required value={goalName} onChange={(e) => setGoalName(e.target.value)} placeholder="Emergency Fund" />
        </div>
        <div>
          <Label htmlFor="g-amount">Target amount</Label>
          <Input
            id="g-amount"
            type="number"
            step="0.01"
            min="0.01"
            required
            value={targetAmount}
            onChange={(e) => setTargetAmount(e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="g-target-date">Target date</Label>
          <Input
            id="g-target-date"
            type="date"
            required
            min={todayIso()}
            value={targetDate}
            onChange={(e) => setTargetDate(e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="g-start-date">Start date (optional)</Label>
          <Input id="g-start-date" type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          <p className="mt-1 text-xs text-slate-400">Defaults to today if left blank.</p>
        </div>
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={saving}>
            Create goal
          </Button>
        </div>
      </form>
    </Modal>
  );
}

function EditGoalModal({ goal, onClose, onSaved }: { goal: Goal | null; onClose: () => void; onSaved: () => void }) {
  const [targetAmount, setTargetAmount] = useState("");
  const [targetDate, setTargetDate] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (goal) {
      setTargetAmount(String(goal.targetAmount));
      setTargetDate(goal.targetDate);
      setError(null);
    }
  }, [goal]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!goal) return;
    setError(null);
    setSaving(true);
    try {
      await goalApi.update(goal.id, { targetAmount: Number(targetAmount), targetDate });
      onSaved();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't update this goal.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal open={!!goal} onClose={onClose} title="Edit goal">
      <form onSubmit={onSubmit} className="space-y-4">
        <FieldError>{error}</FieldError>
        <div>
          <Label htmlFor="eg-amount">Target amount</Label>
          <Input
            id="eg-amount"
            type="number"
            step="0.01"
            min="0.01"
            required
            value={targetAmount}
            onChange={(e) => setTargetAmount(e.target.value)}
          />
        </div>
        <div>
          <Label htmlFor="eg-date">Target date</Label>
          <Input
            id="eg-date"
            type="date"
            required
            min={todayIso()}
            value={targetDate}
            onChange={(e) => setTargetDate(e.target.value)}
          />
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
