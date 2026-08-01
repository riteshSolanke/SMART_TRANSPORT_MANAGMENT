# Transport backend

## Local prerequisites

- JDK 21
- MySQL 8
- PowerShell

The backend requires secrets and database credentials through environment
variables. No operational secret is stored in Git. For local development,
each service also imports an optional `.env` file from its working directory
or parent directory. This supports both IntelliJ runs whose working directory
is `backend` and Maven runs started inside an individual service directory.

## Configure a local terminal

Copy `.env.example` to `.env` and replace every `CHANGE_ME` value. Services
started from `backend` or a direct service folder load that file
automatically. The following command remains useful when starting a service
from another working directory:

```powershell
Get-Content .env |
  Where-Object { $_ -and -not $_.StartsWith('#') } |
  ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
  }
```

Use different random values for `JWT_SECRET`, `GATEWAY_SHARED_SECRET`,
`PAYMENT_INTERNAL_SECRET`, and `ANALYTICS_INTERNAL_SECRET`; each must contain
at least 32 characters. Keep
`.env` local—it is ignored by Git.

Flyway creates or upgrades the auth, route, ticket, vehicle, payment, and
analytics schemas. Hibernate validates the result and does not modify it
automatically.

Passenger registration and profile management belong to auth-service.
Ticket-service references the authenticated user ID and stores only
transport-booking data.

## Start order

Run these commands in separate configured terminals:

```powershell
cd eureka-server
.\mvnw.cmd spring-boot:run

cd api-gateway
.\mvnw.cmd spring-boot:run

cd auth-service
.\mvnw.cmd spring-boot:run

cd route-service
.\mvnw.cmd spring-boot:run

cd ticket-service
.\mvnw.cmd spring-boot:run

cd vehicle-service
.\mvnw.cmd spring-boot:run

cd payment-service
.\mvnw.cmd spring-boot:run

cd analytics-service
.\mvnw.cmd spring-boot:run
```

The public local entry point is `http://localhost:9090`. Business services
bind to `127.0.0.1` by default and reject requests that do not carry the
gateway's internal secret.

## Tests

From each service directory:

```powershell
.\mvnw.cmd clean test
```

Security tests are isolated from MySQL and Eureka; they do not mutate a local
database.

## Payment workflow

Booking creates a ten-minute `PENDING_PAYMENT` ticket hold. Create the payment
with `POST /api/payments`, supplying the ticket ID, payment method, and a unique
`Idempotency-Key` header. A successful simulated payment confirms the ticket;
a decline releases the hold. Refunds are available through
`POST /api/payments/{paymentId}/refund` before departure.

The local processor defaults to `SUCCESS`. Set
`PAYMENT_SIMULATOR_OUTCOME=FAILURE` to exercise the declined-payment path. No
card number, UPI secret, or other sensitive payment credential is accepted or
stored by this training implementation.

## Analytics workflow

Transport managers and admins can query live usage, revenue, and operational
performance under `/api/analytics/**`. Generating a report persists an
auditable period snapshot in `analytics_db`. Source data is fetched through
secured service APIs; analytics-service does not read another service's
database directly.
