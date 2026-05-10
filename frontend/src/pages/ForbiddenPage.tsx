import { Link } from "react-router-dom";

export function ForbiddenPage() {
  return (
    <section className="page-section narrow">
      <div className="empty-state">
        <h1>403</h1>
        <p>You do not have permission to access this page.</p>
        <Link className="button button-primary" to="/dashboard">Back to dashboard</Link>
      </div>
    </section>
  );
}
