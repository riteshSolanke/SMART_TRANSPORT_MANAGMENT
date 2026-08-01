# TransitFlow React Web Application

Responsive React frontend for the Smart Public Transport Ticketing & Route
Management System. The application is written in JavaScript and JSX only.

## Included workflows

- Password and OTP authentication, passenger registration, and password reset
- Role-aware navigation for passenger, conductor, dispatcher, manager, and admin
- Route discovery and route management
- Passenger booking, ticket lifecycle, and payment processing
- Fleet assignment and simulated location tracking
- Usage, revenue, and service-performance analytics
- User and staff administration

## Frontend stack

- React 19 and Vite
- React Router
- TanStack Query and TanStack Table
- React Hook Form and Zod
- Axios with access-token refresh handling
- React Icons, React Hot Toast, and SweetAlert2
- Recharts
- Vitest and Testing Library

## Local development

The backend gateway is expected at `http://127.0.0.1:9090`. In development,
the API client connects to that gateway directly using its configured CORS
policy. This avoids coupling API behavior to the development server's proxy.

```powershell
npm install
npm run dev
```

Open `http://127.0.0.1:5173`.

To point the UI at another gateway, copy `.env.example` to `.env.local` and set
`VITE_API_BASE_URL`. Production deployments should set that value to their
public gateway URL, or leave it empty when the gateway is served on the same
origin.

## Quality checks

```powershell
npm run lint
npm run test
npm run build
```
