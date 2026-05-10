# Vulnerabilities MVP Frontend

React + Vite frontend for the Spring Boot API in this repository.

## Setup

```bash
cd frontend
npm install
```

Create a local `.env` if the backend is not available through the Vite `/api` proxy:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

`.env` files are ignored by the repository. Use `.env.example` as the template.

## Development

```bash
cd frontend
npm run dev
```

The Vite dev server proxies `/api` requests to `http://localhost:8080`. If you use a different backend URL, set `VITE_API_BASE_URL`.

## Production Build

```bash
cd frontend
npm run build
```

The production files are emitted to `frontend/dist`.

## Covered API

- `POST /api/auth/register`: `/register`, creates a CLIENT user and shows the default account ID returned by the backend.
- `POST /api/auth/login`: `/login`, stores JWT auth state.
- `POST /api/payments`: `/payments/new`, CLIENT only.
- `POST /api/payments/{id}/confirm`: `/payments/:id`, CLIENT only action.
- `GET /api/payments/history`: `/payments`, CLIENT only table with filters and pagination.
- `GET /api/payments/history/export`: `/payments`, CLIENT only CSV/JSON export.
- `GET /api/payments/{id}`: `/payments/:id`, authenticated users with backend read permission.
- `POST /api/fraud/payments/{id}/flag`: `/fraud/flag-payment`, OPERATOR only.
- `GET /api/fraud/flags`: `/fraud/flags`, OPERATOR only table.

There are no backend endpoints for payment edit/delete, account listing, user management, role management, or audit log viewing in the current codebase, so the frontend does not expose those actions.

## Roles And Pages

- `CLIENT`: `/dashboard`, `/payments`, `/payments/new`, `/payments/:id`.
- `OPERATOR`: `/dashboard`, `/fraud/flags`, `/fraud/flag-payment`, `/payments/:id`.
- All protected pages require authentication. Unauthorized role access redirects to `/403`.

The backend roles are `CLIENT` and `OPERATOR`; there is no `ADMIN` role in the current backend.

## Security Notes

- API base URL is read from `VITE_API_BASE_URL`; no secrets are stored in frontend code.
- JWT access token and non-sensitive user metadata are stored only in `sessionStorage`; legacy `localStorage` auth state is cleared on startup/request.
- Axios interceptors attach `Authorization: Bearer <token>` to protected requests.
- Expired or invalid local auth state is cleared automatically. Backend `401` responses log the user out.
- Backend `403` responses are displayed as permission errors and role-only routes are hidden from the menu.
- Backend validation errors are displayed without stack traces or internal exception details.
- The frontend does not use `dangerouslySetInnerHTML` and does not log credentials or tokens.
- The backend currently enables CORS with Spring defaults. For production, configure allowed origins for the deployed frontend origin.
