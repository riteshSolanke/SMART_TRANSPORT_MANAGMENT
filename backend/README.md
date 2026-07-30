# Transport backend

## Local prerequisites

- JDK 21
- MySQL 8
- PowerShell

The backend requires secrets and database credentials through environment
variables. No operational secret is stored in Git.

## Configure a local terminal

Copy `.env.example` to `.env`, replace every `CHANGE_ME` value, and load it
into each PowerShell terminal before starting a service:

```powershell
Get-Content .env |
  Where-Object { $_ -and -not $_.StartsWith('#') } |
  ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
  }
```

Use different random values for `JWT_SECRET` and `GATEWAY_SHARED_SECRET`;
each must contain at least 32 characters. Keep `.env` local—it is ignored by
Git.

Flyway creates or upgrades the auth, route, ticket, and vehicle schemas. Hibernate
validates the result and does not modify it automatically.

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
