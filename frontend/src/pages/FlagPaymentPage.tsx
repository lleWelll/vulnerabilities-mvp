import { FormEvent, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { normalizeApiError } from "../api/apiClient";
import { fraudApi } from "../api/endpoints";
import { ErrorMessage } from "../components/ErrorMessage";
import { FormInput, FormSelect, FormTextarea } from "../components/FormInput";
import type { ApiClientError, FraudFlagResponse, RiskLevel } from "../types";
import { riskLevels } from "../types";

export function FlagPaymentPage() {
  const [searchParams] = useSearchParams();
  const initialPaymentId = useMemo(() => searchParams.get("paymentId") || "", [searchParams]);
  const [paymentId, setPaymentId] = useState(initialPaymentId);
  const [riskLevel, setRiskLevel] = useState<RiskLevel>("MEDIUM");
  const [reason, setReason] = useState("");
  const [error, setError] = useState<ApiClientError | Error | null>(null);
  const [result, setResult] = useState<FraudFlagResponse | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setResult(null);
    const validationError = validate(paymentId, reason);
    if (validationError) {
      setError(new Error(validationError));
      return;
    }
    if (!window.confirm(`Create ${riskLevel} fraud flag for payment #${paymentId}?`)) {
      return;
    }
    setLoading(true);
    try {
      const response = await fraudApi.flagPayment(Number(paymentId), {
        riskLevel,
        reason: reason.trim()
      });
      setResult(response);
      setReason("");
    } catch (err) {
      setError(normalizeApiError(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="page-section narrow">
      <div className="page-heading">
        <div>
          <h1>Flag payment</h1>
          <p className="muted">Manual fraud flag creation is available only to operators.</p>
        </div>
      </div>
      <ErrorMessage error={error} />
      {result ? (
        <div className="alert alert-success">
          Fraud flag #{result.fraudFlagId} created for payment #{result.paymentId}.
        </div>
      ) : null}
      <form className="form-stack" onSubmit={handleSubmit}>
        <FormInput
          label="Payment ID"
          type="number"
          min={1}
          value={paymentId}
          onChange={(event) => setPaymentId(event.target.value)}
          required
        />
        <FormSelect label="Risk level" value={riskLevel} onChange={(event) => setRiskLevel(event.target.value as RiskLevel)}>
          {riskLevels.map((level) => <option key={level} value={level}>{level}</option>)}
        </FormSelect>
        <FormTextarea
          label="Reason"
          minLength={5}
          maxLength={255}
          rows={5}
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          required
        />
        <div className="button-row">
          <button className="button button-primary" type="submit" disabled={loading}>{loading ? "Creating..." : "Create flag"}</button>
          <Link className="button button-secondary" to="/fraud/flags">Back to flags</Link>
        </div>
      </form>
    </section>
  );
}

function validate(paymentId: string, reason: string): string | null {
  if (Number(paymentId) <= 0) {
    return "Payment ID must be a positive number.";
  }
  if (reason.trim().length < 5 || reason.trim().length > 255) {
    return "Reason must be 5-255 characters.";
  }
  return null;
}
