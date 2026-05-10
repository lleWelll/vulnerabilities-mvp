import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { normalizeApiError } from "../api/apiClient";
import { paymentsApi } from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import { ErrorMessage } from "../components/ErrorMessage";
import { Loading } from "../components/Loading";
import type { ApiClientError, PaymentResponse } from "../types";

export function PaymentDetailsPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const paymentId = Number(id);
  const [payment, setPayment] = useState<PaymentResponse | null>(null);
  const [error, setError] = useState<ApiClientError | Error | null>(null);
  const [loading, setLoading] = useState(true);
  const [confirming, setConfirming] = useState(false);

  useEffect(() => {
    if (!Number.isFinite(paymentId) || paymentId <= 0) {
      setError(new Error("Invalid payment ID."));
      setLoading(false);
      return;
    }
    void loadPayment();
  }, [paymentId]);

  async function loadPayment() {
    setLoading(true);
    setError(null);
    try {
      setPayment(await paymentsApi.getById(paymentId));
    } catch (err) {
      setError(normalizeApiError(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleConfirm() {
    if (!payment || !window.confirm(`Confirm payment #${payment.id}?`)) {
      return;
    }
    setConfirming(true);
    setError(null);
    try {
      setPayment(await paymentsApi.confirm(payment.id));
    } catch (err) {
      setError(normalizeApiError(err));
    } finally {
      setConfirming(false);
    }
  }

  return (
    <section className="page-section narrow">
      <div className="page-heading">
        <div>
          <h1>Payment details</h1>
          <p className="muted">Read access is available to authenticated users permitted by backend rules.</p>
        </div>
      </div>
      <ErrorMessage error={error} />
      {loading ? <Loading label="Loading payment..." /> : null}
      {!loading && payment ? (
        <article className="details-card">
          <dl>
            <dt>ID</dt><dd>{payment.id}</dd>
            <dt>Source account</dt><dd>{payment.sourceAccountId}</dd>
            <dt>Receiver account</dt><dd>{payment.receiverAccountId}</dd>
            <dt>Receiver username</dt><dd>{payment.receiverUsername}</dd>
            <dt>Amount</dt><dd>{payment.amount} {payment.currency}</dd>
            <dt>Status</dt><dd><span className={`badge badge-${payment.status.toLowerCase()}`}>{payment.status}</span></dd>
            <dt>Flagged</dt><dd>{payment.flagged ? "Yes" : "No"}</dd>
            <dt>Description</dt><dd>{payment.description || "No description"}</dd>
            <dt>Created</dt><dd>{formatDate(payment.createdAt)}</dd>
            <dt>Confirmed</dt><dd>{payment.confirmedAt ? formatDate(payment.confirmedAt) : "Not confirmed"}</dd>
          </dl>
          <div className="button-row">
            {user?.role === "CLIENT" && payment.status !== "CONFIRMED" && payment.status !== "REJECTED" ? (
              <button className="button button-primary" type="button" disabled={confirming} onClick={() => void handleConfirm()}>
                {confirming ? "Confirming..." : "Confirm payment"}
              </button>
            ) : null}
            {user?.role === "OPERATOR" ? (
              <Link className="button button-secondary" to={`/fraud/flag-payment?paymentId=${payment.id}`}>Flag payment</Link>
            ) : null}
            <Link className="button button-secondary" to={user?.role === "CLIENT" ? "/payments" : "/fraud/flags"}>Back</Link>
          </div>
        </article>
      ) : null}
    </section>
  );
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}
