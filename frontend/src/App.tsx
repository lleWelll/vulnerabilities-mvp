import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { Layout } from "./components/Layout";
import { DashboardPage } from "./pages/DashboardPage";
import { FlagPaymentPage } from "./pages/FlagPaymentPage";
import { ForbiddenPage } from "./pages/ForbiddenPage";
import { FraudFlagsPage } from "./pages/FraudFlagsPage";
import { LoginPage } from "./pages/LoginPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { PaymentCreatePage } from "./pages/PaymentCreatePage";
import { PaymentDetailsPage } from "./pages/PaymentDetailsPage";
import { PaymentsPage } from "./pages/PaymentsPage";
import { RegisterPage } from "./pages/RegisterPage";

export function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/403" element={<ForbiddenPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/payments/:id" element={<PaymentDetailsPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={["CLIENT"]} />}>
        <Route element={<Layout />}>
          <Route path="/payments" element={<PaymentsPage />} />
          <Route path="/payments/new" element={<PaymentCreatePage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={["OPERATOR"]} />}>
        <Route element={<Layout />}>
          <Route path="/fraud/flags" element={<FraudFlagsPage />} />
          <Route path="/fraud/flag-payment" element={<FlagPaymentPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
