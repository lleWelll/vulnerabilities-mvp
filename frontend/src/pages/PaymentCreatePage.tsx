import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { normalizeApiError } from "../api/apiClient";
import { paymentsApi } from "../api/endpoints";
import { ErrorMessage } from "../components/ErrorMessage";
import { FormInput, FormSelect, FormTextarea } from "../components/FormInput";
import type { ApiClientError, CurrencyCode } from "../types";
import { currencies } from "../types";

export function PaymentCreatePage() {
  const navigate = useNavigate();
  const [sourceAccountId, setSourceAccountId] = useState("");
  const [receiverAccountId, setReceiverAccountId] = useState("");
  const [amount, setAmount] = useState("");
  const [currency, setCurrency] = useState<CurrencyCode>("KZT");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<ApiClientError | Error | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    const validationError = validate(sourceAccountId, receiverAccountId, amount, description);
    if (validationError) {
      setError(new Error(validationError));
      return;
    }
    setLoading(true);
    try {
      const payment = await paymentsApi.create({
        sourceAccountId: Number(sourceAccountId),
        receiverAccountId: Number(receiverAccountId),
        amount: normalizeAmount(amount),
        currency,
        description: description.trim() || undefined
      });
      navigate(`/payments/${payment.id}`);
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
          <h1>New payment</h1>
          <p className="muted">Use account IDs from backend seed data or a registration response.</p>
        </div>
      </div>
      <ErrorMessage error={error} />
      <form className="form-stack" onSubmit={handleSubmit}>
        <FormInput
          label="Source account ID"
          type="number"
          min={1}
          value={sourceAccountId}
          onChange={(event) => setSourceAccountId(event.target.value)}
          required
        />
        <FormInput
          label="Receiver account ID"
          type="number"
          min={1}
          value={receiverAccountId}
          onChange={(event) => setReceiverAccountId(event.target.value)}
          required
        />
        <FormInput
          label="Amount"
          type="text"
          inputMode="decimal"
          pattern="\d{1,15}(\.\d{1,2})?"
          placeholder="100.00"
          value={amount}
          onChange={(event) => setAmount(event.target.value)}
          required
        />
        <FormSelect label="Currency" value={currency} onChange={(event) => setCurrency(event.target.value as CurrencyCode)}>
          {currencies.map((item) => <option key={item} value={item}>{item}</option>)}
        </FormSelect>
        <FormTextarea
          label="Description"
          maxLength={255}
          rows={4}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
        />
        <div className="button-row">
          <button className="button button-primary" type="submit" disabled={loading}>{loading ? "Creating..." : "Create payment"}</button>
          <Link className="button button-secondary" to="/payments">Cancel</Link>
        </div>
      </form>
    </section>
  );
}

function validate(sourceAccountId: string, receiverAccountId: string, amount: string, description: string): string | null {
  if (Number(sourceAccountId) <= 0 || Number(receiverAccountId) <= 0) {
    return "Account IDs must be positive numbers.";
  }
  if (sourceAccountId === receiverAccountId) {
    return "Source and receiver accounts must be different.";
  }
  if (!/^\d{1,15}(\.\d{1,2})?$/.test(amount.trim()) || Number(amount) < 0.01) {
    return "Amount must be at least 0.01 with no more than two decimal places.";
  }
  if (description.length > 255) {
    return "Description must be 255 characters or fewer.";
  }
  return null;
}

function normalizeAmount(amount: string): string {
  const [integerPart, fractionPart = ""] = amount.trim().split(".");
  return `${integerPart}.${fractionPart.padEnd(2, "0")}`;
}
