import { apiClient } from "./apiClient";
import type {
  CsrfTokenResponse,
  CreatePaymentRequest,
  ExportHistoryParams,
  FlagFraudRequest,
  FraudFlagResponse,
  LoginRequest,
  LoginResponse,
  PagedResponse,
  PaymentHistoryParams,
  PaymentResponse,
  RegisterRequest,
  RegisterResponse
} from "../types";

export const authApi = {
  csrf: async () => {
    const { data } = await apiClient.get<CsrfTokenResponse>("/auth/csrf");
    return data;
  },
  login: async (payload: LoginRequest) => {
    const { data } = await apiClient.post<LoginResponse>("/auth/login", payload);
    return data;
  },
  register: async (payload: RegisterRequest) => {
    const { data } = await apiClient.post<RegisterResponse>("/auth/register", payload);
    return data;
  }
};

export const paymentsApi = {
  create: async (payload: CreatePaymentRequest) => {
    const { data } = await apiClient.post<PaymentResponse>("/payments", payload);
    return data;
  },
  confirm: async (paymentId: number) => {
    const { data } = await apiClient.post<PaymentResponse>(`/payments/${paymentId}/confirm`);
    return data;
  },
  history: async (params: PaymentHistoryParams = {}) => {
    const { data } = await apiClient.get<PagedResponse<PaymentResponse>>("/payments/history", {
      params: cleanParams(params)
    });
    return data;
  },
  exportHistory: async (params: ExportHistoryParams = {}) => {
    const response = await apiClient.get<Blob>("/payments/history/export", {
      params: cleanParams(params),
      responseType: "blob"
    });
    const disposition = response.headers["content-disposition"];
    return {
      blob: response.data,
      fileName: parseFileName(disposition) || defaultExportFileName(params.format)
    };
  },
  getById: async (paymentId: number) => {
    const { data } = await apiClient.get<PaymentResponse>(`/payments/${paymentId}`);
    return data;
  }
};

export const fraudApi = {
  flagPayment: async (paymentId: number, payload: FlagFraudRequest) => {
    const { data } = await apiClient.post<FraudFlagResponse>(`/fraud/payments/${paymentId}/flag`, payload);
    return data;
  },
  flags: async (params: { page?: number; size?: number } = {}) => {
    const { data } = await apiClient.get<PagedResponse<FraudFlagResponse>>("/fraud/flags", {
      params: cleanParams(params)
    });
    return data;
  }
};

function cleanParams<T extends object>(params: T): Partial<T> {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== "")
  ) as Partial<T>;
}

function parseFileName(disposition?: string): string | null {
  if (!disposition) {
    return null;
  }
  const match = disposition.match(/filename="?([^"]+)"?/i);
  return match?.[1] || null;
}

function defaultExportFileName(format?: string): string {
  const extension = format === "JSON" ? "json" : "csv";
  return `payment-history.${extension}`;
}
