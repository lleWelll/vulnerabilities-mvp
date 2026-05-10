import axios, { AxiosError } from "axios";
import type { ApiClientError, ApiErrorResponse } from "../types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8080/api";
const AUTH_STORAGE_KEY = "vulnerabilities-mvp.auth";

type UnauthorizedHandler = () => void;

let unauthorizedHandler: UnauthorizedHandler | null = null;

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json"
  }
});

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null) {
  unauthorizedHandler = handler;
}

export function getStoredToken(): string | null {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY) || sessionStorage.getItem(AUTH_STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as { token?: string; expiresAt?: number };
    if (!parsed.token || !parsed.expiresAt || Date.now() >= parsed.expiresAt) {
      localStorage.removeItem(AUTH_STORAGE_KEY);
      sessionStorage.removeItem(AUTH_STORAGE_KEY);
      return null;
    }
    return parsed.token;
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    sessionStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function normalizeApiError(error: unknown): ApiClientError {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiErrorResponse>;
    const response = axiosError.response;
    const data = response?.data;
    const message = data?.message || fallbackMessage(response?.status);
    const normalized = new Error(message) as ApiClientError;
    normalized.status = response?.status;
    normalized.validationErrors = data?.validationErrors || {};
    return normalized;
  }

  if (error instanceof Error) {
    return error as ApiClientError;
  }

  return new Error("Unexpected client error") as ApiClientError;
}

apiClient.interceptors.request.use((config) => {
  const token = getStoredToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    const status = error.response?.status;
    if (status === 401 && unauthorizedHandler) {
      unauthorizedHandler();
    }
    return Promise.reject(normalizeApiError(error));
  }
);

function fallbackMessage(status?: number): string {
  if (status === 400) {
    return "The request is invalid. Check the entered data.";
  }
  if (status === 401) {
    return "Your session has expired. Sign in again.";
  }
  if (status === 403) {
    return "You do not have permission to perform this action.";
  }
  if (status === 404) {
    return "The requested resource was not found.";
  }
  if (status === 409) {
    return "The operation conflicts with the current state.";
  }
  if (status && status >= 500) {
    return "The server could not process the request.";
  }
  return "Network request failed.";
}

export { AUTH_STORAGE_KEY };
