import { NavLink } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { roleLabel } from "../auth/roleUtils";

export function Navbar() {
  const { user, logout } = useAuth();

  return (
    <header className="topbar">
      <div className="brand">Vulnerabilities MVP</div>
      <nav className="nav-links" aria-label="Main navigation">
        <NavLink to="/dashboard">Dashboard</NavLink>
        {user?.role === "CLIENT" ? (
          <>
            <NavLink to="/payments">Payments</NavLink>
            <NavLink to="/payments/new">New payment</NavLink>
          </>
        ) : null}
        {user?.role === "OPERATOR" ? (
          <>
            <NavLink to="/fraud/flags">Fraud flags</NavLink>
            <NavLink to="/fraud/flag-payment">Flag payment</NavLink>
          </>
        ) : null}
      </nav>
      <div className="user-chip">
        <span>{user?.username}</span>
        <span>{roleLabel(user?.role)}</span>
        <button className="button button-secondary" type="button" onClick={logout}>Logout</button>
      </div>
    </header>
  );
}
