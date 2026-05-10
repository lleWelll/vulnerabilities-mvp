import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { normalizeApiError } from "../api/apiClient";
import { fraudApi } from "../api/endpoints";
import { ErrorMessage } from "../components/ErrorMessage";
import { Loading } from "../components/Loading";
import type { ApiClientError, FraudFlagResponse, PagedResponse } from "../types";

export function FraudFlagsPage() {
  const [data, setData] = useState<PagedResponse<FraudFlagResponse> | null>(null);
  const [page, setPage] = useState(0);
  const [error, setError] = useState<ApiClientError | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    void loadFlags();
  }, [page]);

  async function loadFlags() {
    setLoading(true);
    setError(null);
    try {
      setData(await fraudApi.flags({ page, size: 20 }));
    } catch (err) {
      setError(normalizeApiError(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <h1>Fraud flags</h1>
          <p className="muted">Operator-only view of manual and automatic payment flags.</p>
        </div>
        <Link className="button button-primary" to="/fraud/flag-payment">Flag payment</Link>
      </div>

      <ErrorMessage error={error} />
      {loading ? <Loading label="Loading fraud flags..." /> : null}
      {!loading && data?.content.length === 0 ? <div className="empty-state">No fraud flags were found.</div> : null}
      {!loading && data && data.content.length > 0 ? (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Flag ID</th>
                  <th>Payment</th>
                  <th>Owner</th>
                  <th>Receiver</th>
                  <th>Amount</th>
                  <th>Risk</th>
                  <th>Manual</th>
                  <th>Flagged by</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((flag) => (
                  <tr key={flag.fraudFlagId}>
                    <td>{flag.fraudFlagId}</td>
                    <td><Link to={`/payments/${flag.paymentId}`}>{flag.paymentId}</Link></td>
                    <td>{flag.ownerUsername}</td>
                    <td>{flag.receiverUsername}</td>
                    <td>{flag.amount} {flag.currency}</td>
                    <td><span className={`badge badge-risk-${flag.riskLevel.toLowerCase()}`}>{flag.riskLevel}</span></td>
                    <td>{flag.manual ? "Yes" : "No"}</td>
                    <td>{flag.flaggedBy || "System"}</td>
                    <td>{formatDate(flag.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="pagination">
            <button className="button button-secondary" type="button" disabled={page <= 0} onClick={() => setPage(page - 1)}>Previous</button>
            <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)}</span>
            <button className="button button-secondary" type="button" disabled={data.page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next</button>
          </div>
        </>
      ) : null}
    </section>
  );
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}
