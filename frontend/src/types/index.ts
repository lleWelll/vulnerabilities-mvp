export type Role = "CLIENT" | "OPERATOR";

export type CurrencyCode = "KZT" | "USD" | "EUR";

export type PaymentStatus = "CREATED" | "CONFIRMED" | "REJECTED" | "FLAGGED";

export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";

export type PaymentExportFormat = "CSV" | "JSON";

export interface ApiErrorResponse {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  validationErrors?: Record<string, string>;
}

export interface ApiClientError extends Error {
  status?: number;
  validationErrors?: Record<string, string>;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  role: Role;
}

export interface RegisterRequest {
  username: string;
  password: string;
}

export interface RegisterResponse {
  userId: number;
  username: string;
  role: Role;
  defaultAccountId: number;
  defaultCurrency: CurrencyCode;
  createdAt: string;
}

export interface AuthUser {
  username: string;
  role: Role;
  userId?: number;
}

export interface AuthState {
  token: string;
  tokenType: string;
  expiresAt: number;
  user: AuthUser;
}

export interface CreatePaymentRequest {
  sourceAccountId: number;
  receiverAccountId: number;
  amount: string;
  currency: CurrencyCode;
  description?: string;
}

export interface PaymentResponse {
  id: number;
  sourceAccountId: number;
  receiverAccountId: number;
  receiverUsername: string;
  amount: number | string;
  currency: CurrencyCode;
  status: PaymentStatus;
  flagged: boolean;
  description?: string | null;
  createdAt: string;
  confirmedAt?: string | null;
}

export interface PaymentHistoryParams {
  page?: number;
  size?: number;
  status?: PaymentStatus | "";
  receiverUsername?: string;
}

export interface ExportHistoryParams extends PaymentHistoryParams {
  format?: PaymentExportFormat;
  fileName?: string;
}

export interface FlagFraudRequest {
  riskLevel: RiskLevel;
  reason: string;
}

export interface FraudFlagResponse {
  fraudFlagId: number;
  paymentId: number;
  ownerUsername: string;
  receiverUsername: string;
  amount: number | string;
  currency: CurrencyCode;
  paymentStatus: PaymentStatus;
  riskLevel: RiskLevel;
  reason: string;
  manual: boolean;
  flaggedBy?: string | null;
  createdAt: string;
}

export const roles: Role[] = ["CLIENT", "OPERATOR"];
export const currencies: CurrencyCode[] = ["KZT", "USD", "EUR"];
export const paymentStatuses: PaymentStatus[] = ["CREATED", "CONFIRMED", "REJECTED", "FLAGGED"];
export const riskLevels: RiskLevel[] = ["LOW", "MEDIUM", "HIGH"];
