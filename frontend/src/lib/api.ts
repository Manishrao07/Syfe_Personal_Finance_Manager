import type {
  CategoryListResponse,
  Category,
  GoalListResponse,
  Goal,
  MonthlyReport,
  TransactionListResponse,
  Transaction,
  TransactionType,
  YearlyReport,
} from "./types";

// Always same-origin: next.config.ts rewrites /api/* to the real backend
// server-side, so the browser never makes a cross-site request and the
// session cookie stays a normal first-party cookie.
const BASE_URL = "/api";

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
      ...(init?.headers ?? {}),
    },
  });

  if (res.status === 204) {
    return undefined as T;
  }

  const text = await res.text();
  const data = text ? JSON.parse(text) : undefined;

  if (!res.ok) {
    const message = data?.message ?? `Request failed with status ${res.status}`;
    throw new ApiError(res.status, message);
  }

  return data as T;
}

export const authApi = {
  register: (body: { username: string; password: string; fullName: string; phoneNumber: string }) =>
    request<{ message: string; userId: number }>("/auth/register", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  login: (body: { username: string; password: string }) =>
    request<{ message: string }>("/auth/login", {
      method: "POST",
      body: JSON.stringify(body),
    }),
  logout: () => request<{ message: string }>("/auth/logout", { method: "POST" }),
};

export const categoryApi = {
  list: () => request<CategoryListResponse>("/categories"),
  create: (body: { name: string; type: TransactionType }) =>
    request<Category>("/categories", { method: "POST", body: JSON.stringify(body) }),
  remove: (name: string) =>
    request<{ message: string }>(`/categories/${encodeURIComponent(name)}`, { method: "DELETE" }),
};

export interface TransactionFilters {
  startDate?: string;
  endDate?: string;
  category?: string;
  type?: TransactionType;
}

export const transactionApi = {
  list: (filters: TransactionFilters = {}) => {
    const params = new URLSearchParams();
    if (filters.startDate) params.set("startDate", filters.startDate);
    if (filters.endDate) params.set("endDate", filters.endDate);
    if (filters.category) params.set("category", filters.category);
    if (filters.type) params.set("type", filters.type);
    const qs = params.toString();
    return request<TransactionListResponse>(`/transactions${qs ? `?${qs}` : ""}`);
  },
  create: (body: { amount: number; date: string; category: string; description?: string }) =>
    request<Transaction>("/transactions", { method: "POST", body: JSON.stringify(body) }),
  update: (id: number, body: { amount?: number; category?: string; description?: string }) =>
    request<Transaction>(`/transactions/${id}`, { method: "PUT", body: JSON.stringify(body) }),
  remove: (id: number) => request<{ message: string }>(`/transactions/${id}`, { method: "DELETE" }),
};

export const goalApi = {
  list: () => request<GoalListResponse>("/goals"),
  get: (id: number) => request<Goal>(`/goals/${id}`),
  create: (body: { goalName: string; targetAmount: number; targetDate: string; startDate?: string }) =>
    request<Goal>("/goals", { method: "POST", body: JSON.stringify(body) }),
  update: (id: number, body: { targetAmount?: number; targetDate?: string }) =>
    request<Goal>(`/goals/${id}`, { method: "PUT", body: JSON.stringify(body) }),
  remove: (id: number) => request<{ message: string }>(`/goals/${id}`, { method: "DELETE" }),
};

export const reportApi = {
  monthly: (year: number, month: number) => request<MonthlyReport>(`/reports/monthly/${year}/${month}`),
  yearly: (year: number) => request<YearlyReport>(`/reports/yearly/${year}`),
};
