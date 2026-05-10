import { FormEvent, useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { normalizeApiError } from "../api/apiClient";
import { useAuth } from "../auth/AuthContext";
import { ErrorMessage } from "../components/ErrorMessage";
import { FormInput } from "../components/FormInput";
import type { RegisterResponse } from "../types";

export function RegisterPage() {
  const { register, isAuthenticated } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<Error | null>(null);
  const [result, setResult] = useState<RegisterResponse | null>(null);
  const [loading, setLoading] = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setResult(null);
    const validationError = validate(username, password);
    if (validationError) {
      setError(new Error(validationError));
      return;
    }
    setLoading(true);
    try {
      const response = await register({ username, password });
      setResult(response);
      setPassword("");
    } catch (err) {
      setError(normalizeApiError(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-panel">
        <h1>Register</h1>
        <p className="muted">New accounts are created with the CLIENT role and a default KZT account.</p>
        <ErrorMessage error={error} />
        {result ? (
          <div className="alert alert-success">
            Account {result.username} created. Default account ID: {result.defaultAccountId} {result.defaultCurrency}.
          </div>
        ) : null}
        <form onSubmit={handleSubmit} className="form-stack">
          <FormInput
            label="Username"
            name="username"
            autoComplete="username"
            minLength={3}
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
            autoComplete="new-password"
            minLength={10}
            maxLength={72}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
          <button className="button button-primary" disabled={loading} type="submit">
            {loading ? "Creating account..." : "Create account"}
          </button>
        </form>
        <p className="auth-link">Already registered? <Link to="/login">Sign in</Link></p>
      </section>
    </main>
  );
}

function validate(username: string, password: string): string | null {
  if (!/^[A-Za-z0-9._-]{3,50}$/.test(username.trim())) {
    return "Username must be 3-50 characters and contain only letters, digits, dots, underscores, and hyphens.";
  }
  if (password.length < 10 || password.length > 72) {
    return "Password must be 10-72 characters.";
  }
  if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).+$/.test(password)) {
    return "Password must contain uppercase, lowercase, digit, and special character.";
  }
  return null;
}
