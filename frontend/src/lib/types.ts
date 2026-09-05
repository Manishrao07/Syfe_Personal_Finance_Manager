export type TransactionType = "INCOME" | "EXPENSE";

export interface Category {
  name: string;
  type: TransactionType;
  isCustom: boolean;
}

export interface CategoryListResponse {
  categories: Category[];
}

export interface Transaction {
  id: number;
  amount: number;
  date: string;
  category: string;
  description: string | null;
  type: TransactionType;
}

export interface TransactionListResponse {
  transactions: Transaction[];
}

export interface Goal {
  id: number;
  goalName: string;
  targetAmount: number;
  targetDate: string;
  startDate: string;
  currentProgress: number;
  progressPercentage: number;
  remainingAmount: number;
}

export interface GoalListResponse {
  goals: Goal[];
}

export interface MonthlyReport {
  month: number;
  year: number;
  totalIncome: Record<string, number>;
  totalExpenses: Record<string, number>;
  netSavings: number;
}

export interface YearlyReport {
  year: number;
  totalIncome: Record<string, number>;
  totalExpenses: Record<string, number>;
  netSavings: number;
}

export interface ApiErrorBody {
  status: number;
  message: string;
  timestamp: string;
}
