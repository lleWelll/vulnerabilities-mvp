import type { ApiClientError } from "../types";

interface ErrorMessageProps {
  error?: string | Error | ApiClientError | null;
}

export function ErrorMessage({ error }: ErrorMessageProps) {
  if (!error) {
    return null;
  }

  const message = typeof error === "string" ? error : error.message;
  const validationErrors = typeof error === "string" ? undefined : (error as ApiClientError).validationErrors;

  return (
    <div className="alert alert-error" role="alert">
      <p>{sanitizeMessage(message)}</p>
      {validationErrors && Object.keys(validationErrors).length > 0 ? (
        <ul>
          {Object.entries(validationErrors).map(([field, fieldError]) => (
            <li key={field}>
              <strong>{field}:</strong> {sanitizeMessage(fieldError)}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

function sanitizeMessage(message: string): string {
  if (!message) {
    return "Request failed.";
  }
  if (message.includes("Exception") || message.includes("at org.") || message.includes("java.")) {
    return "The request could not be completed.";
  }
  return message;
}
