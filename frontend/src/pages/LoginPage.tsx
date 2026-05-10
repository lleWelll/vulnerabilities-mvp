import { FormEvent, useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ErrorMessage } from "../components/ErrorMessage";
import { FormInput } from "../components/FormInput";
import { normalizeApiError } from "../api/apiClient";

export function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname || "/dashboard";
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<Error | null>(null);
  const [loading, setLoading] = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    if (!username.trim() || !password) {
      setError(new Error("Username and password are required."));
      return;
    }
    setLoading(true);
    try {
      await login({ username, password });
      navigate(from, { replace: true });
    } catch (err) {
      setError(normalizeApiError(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-panel">
        <h1>Sign in</h1>
        <p className="muted">Use a backend account with CLIENT or OPERATOR role.</p>
        <ErrorMessage error={error} />
        <form onSubmit={handleSubmit} className="form-stack">
          <FormInput
            label="Username"
            name="username"
            autoComplete="username"
            maxLength={50}
            pattern="[A-Za-z0-9._-]+"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            required
          />
          <FormInput
            label="Password"
            name="password"
            type="password"
            autoComplete="current-password"
            minLength={5}
            maxLength={72}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
          {/* OWASP-10: Cryptographic Failures - persistent "Keep me signed in" stored JWT in localStorage.
              Исправление: опция удалена, сессия ограничена sessionStorage и exp токена. */}
          <button className="button button-primary" disabled={loading} type="submit">
            {loading ? "Signing in..." : "Sign in"}
          </button>
        </form>
        <p className="auth-link">No account? <Link to="/register">Register</Link></p>
      </section>
    </main>
  );
}
