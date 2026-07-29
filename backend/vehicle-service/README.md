# Vehicle service

The vehicle service manages the active fleet, dated vehicle assignments, and
simulated GPS updates. It registers with Eureka as `vehicle-service` and is
already exposed by the gateway under `/api/vehicles/**`.

All business requests must pass through the API gateway. Direct requests are
rejected unless they include the configured gateway secret.

## Main endpoints

| Method | Endpoint | Roles / purpose |
| --- | --- | --- |
| `POST` | `/api/vehicles` | Manager/Admin: create vehicle |
| `GET` | `/api/vehicles` | Authenticated: list active vehicles |
| `GET` | `/api/vehicles/{vehicleId}` | Authenticated: get vehicle |
| `PUT/PATCH` | `/api/vehicles/{vehicleId}` | Manager/Admin: update details |
| `PATCH` | `/api/vehicles/{vehicleId}/status` | Dispatcher/Manager/Admin: change status |
| `DELETE` | `/api/vehicles/{vehicleId}` | Admin: soft-delete vehicle |
| `POST` | `/api/vehicles/{vehicleId}/assignments` | Dispatcher/Manager/Admin: assign route schedule |
| `GET` | `/api/vehicles/{vehicleId}/assignments` | Dispatcher/Manager/Admin: assignment history |
| `PATCH` | `/api/vehicles/{vehicleId}/assignments/{assignmentId}/complete` | Finish assignment |
| `PATCH` | `/api/vehicles/{vehicleId}/assignments/{assignmentId}/cancel` | Cancel assignment |
| `POST` | `/api/vehicles/{vehicleId}/locations` | Record simulated location |
| `GET` | `/api/vehicles/{vehicleId}/locations/latest` | Authenticated: latest location |
| `GET` | `/api/vehicles/{vehicleId}/locations?limit=50` | Operations: location history |

Assignments validate that the referenced active route and schedule exist and
that the schedule operates on `serviceDate`. A vehicle can have one active
assignment per service date, and a route schedule can have one active vehicle
per service date.

Location recording is intentionally simulated: a conductor or operations user
posts latitude, longitude, optional speed, and optional timestamp. The vehicle
must have status `IN_SERVICE`.

## Local configuration

Required environment variables:

```text
VEHICLE_DB_USERNAME
VEHICLE_DB_PASSWORD
GATEWAY_SHARED_SECRET
```

`VEHICLE_DB_URL` and `VEHICLE_SERVICE_PORT` are optional. Flyway owns schema
creation and Hibernate validates it.

Run tests:

```powershell
.\mvnw.cmd clean test
```

Run the service:

```powershell
.\mvnw.cmd spring-boot:run
```
