import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <section className="page-section narrow">
      <div className="empty-state">
        <h1>404</h1>
        <p>The requested page was not found.</p>
        <Link className="button button-primary" to="/dashboard">Back to dashboard</Link>
      </div>
    </section>
  );
}
