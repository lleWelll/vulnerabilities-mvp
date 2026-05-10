import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { roleLabel } from "../auth/roleUtils";

export function DashboardPage() {
  const { user } = useAuth();

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <h1>Dashboard</h1>
          <p className="muted">Signed in as {user?.username} with {roleLabel(user?.role)} access.</p>
        </div>
      </div>

      <div className="dashboard-grid">
        {user?.role === "CLIENT" ? (
          <>
            <article className="card">
              <h2>Payment history</h2>
              <p>View, filter, export, and open payment details.</p>
              <Link className="button button-primary" to="/payments">Open payments</Link>
            </article>
            <article className="card">
              <h2>Create payment</h2>
              <p>Submit a payment using source and receiver account IDs.</p>
              <Link className="button button-secondary" to="/payments/new">New payment</Link>
            </article>
          </>
        ) : null}

        {user?.role === "OPERATOR" ? (
          <>
            <article className="card">
              <h2>Fraud flags</h2>
              <p>Review automatic and manual fraud flags.</p>
              <Link className="button button-primary" to="/fraud/flags">Open flags</Link>
            </article>
            <article className="card">
              <h2>Manual flag</h2>
              <p>Flag a payment by ID with risk level and reason.</p>
              <Link className="button button-secondary" to="/fraud/flag-payment">Flag payment</Link>
            </article>
          </>
        ) : null}
      </div>
    </section>
  );
}
