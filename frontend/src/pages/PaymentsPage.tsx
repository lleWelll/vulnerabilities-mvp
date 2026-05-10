import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { normalizeApiError } from "../api/apiClient";
import { paymentsApi } from "../api/endpoints";
import { ErrorMessage } from "../components/ErrorMessage";
import { FormInput, FormSelect } from "../components/FormInput";
import { Loading } from "../components/Loading";
import type { ApiClientError, PagedResponse, PaymentExportFormat, PaymentResponse, PaymentStatus } from "../types";
import { paymentStatuses } from "../types";

export function PaymentsPage() {
  const [data, setData] = useState<PagedResponse<PaymentResponse> | null>(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [status, setStatus] = useState<PaymentStatus | "">("");
  const [receiverUsername, setReceiverUsername] = useState("");
  const [error, setError] = useState<ApiClientError | null>(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    void loadPayments();
  }, [page, size]);

  async function loadPayments(nextPage = page) {
    setLoading(true);
    setError(null);
    try {
      const response = await paymentsApi.history({
        page: nextPage,
        size,
        status,
        receiverUsername: receiverUsername.trim()
      });
      setData(response);
      setPage(response.page);
    } catch (err) {
      setError(normalizeApiError(err));
    } finally {
      setLoading(false);
    }
  }

  function handleFilter(event: FormEvent) {
    event.preventDefault();
    setPage(0);
    void loadPayments(0);
  }

  async function handleExport(format: PaymentExportFormat) {
    setExporting(true);
    setError(null);
    try {
      const { blob, fileName } = await paymentsApi.exportHistory({
        format,
        page,
        size,
        status,
        receiverUsername: receiverUsername.trim()
      });
      const url = window.URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = fileName;
      anchor.click();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(normalizeApiError(err));
    } finally {
      setExporting(false);
    }
  }

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <h1>Payments</h1>
          <p className="muted">Payment history is scoped to the signed-in client.</p>
        </div>
        <Link className="button button-primary" to="/payments/new">New payment</Link>
      </div>

      <form className="filter-bar" onSubmit={handleFilter}>
        <FormSelect label="Status" value={status} onChange={(event) => setStatus(event.target.value as PaymentStatus | "")}>
          <option value="">All</option>
          {paymentStatuses.map((item) => <option key={item} value={item}>{item}</option>)}
        </FormSelect>
        <FormInput
          label="Receiver username"
          value={receiverUsername}
          maxLength={50}
          pattern="[A-Za-z0-9._-]*"
          onChange={(event) => setReceiverUsername(event.target.value)}
        />
        <FormInput
          label="Page size"
          type="number"
          min={1}
          max={100}
          value={size}
          onChange={(event) => setSize(Number(event.target.value))}
        />
        <button className="button button-secondary" type="submit">Apply</button>
      </form>

      <ErrorMessage error={error} />

      <div className="table-actions">
        <button className="button button-secondary" type="button" disabled={exporting || !data?.content.length} onClick={() => void handleExport("CSV")}>
          Export CSV
        </button>
        <button className="button button-secondary" type="button" disabled={exporting || !data?.content.length} onClick={() => void handleExport("JSON")}>
          Export JSON
        </button>
      </div>

      {loading ? <Loading label="Loading payments..." /> : null}

      {!loading && data?.content.length === 0 ? (
        <div className="empty-state">No payments match the current filters.</div>
      ) : null}

      {!loading && data && data.content.length > 0 ? (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Receiver</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Flagged</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((payment) => (
                  <tr key={payment.id}>
                    <td>{payment.id}</td>
                    <td>{payment.receiverUsername}</td>
                    <td>{payment.amount} {payment.currency}</td>
                    <td><span className={`badge badge-${payment.status.toLowerCase()}`}>{payment.status}</span></td>
                    <td>{payment.flagged ? "Yes" : "No"}</td>
                    <td>{formatDate(payment.createdAt)}</td>
                    <td><Link to={`/payments/${payment.id}`}>Open</Link></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
        </>
      ) : null}
    </section>
  );
}

function Pagination({ page, totalPages, onPageChange }: { page: number; totalPages: number; onPageChange: (page: number) => void }) {
  return (
    <div className="pagination">
      <button className="button button-secondary" type="button" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>Previous</button>
      <span>Page {page + 1} of {Math.max(totalPages, 1)}</span>
      <button className="button button-secondary" type="button" disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1)}>Next</button>
    </div>
  );
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}
