# Route service

The route service manages active routes, ordered stops, schedules, route
searching, and distance-based fare quotations.

All business endpoints must be called through the API gateway. Creating,
updating, or deleting route data requires `TRANSPORT_MANAGER` or `ADMIN`
unless an endpoint states otherwise.

## Main endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/routes` | Create a route |
| `GET` | `/api/routes` | List active routes |
| `GET` | `/api/routes/{routeId}` | Get an active route with stops and schedules |
| `PUT/PATCH` | `/api/routes/{routeId}` | Update a route |
| `DELETE` | `/api/routes/{routeId}` | Soft-delete a route (admin only) |
| `POST` | `/api/routes/{routeId}/stops` | Add a stop |
| `GET` | `/api/routes/{routeId}/stops/{stopId}` | Get a stop |
| `PUT/PATCH` | `/api/routes/{routeId}/stops/{stopId}` | Update a stop |
| `DELETE` | `/api/routes/{routeId}/stops/{stopId}` | Delete a stop |
| `POST` | `/api/routes/{routeId}/schedules` | Add a schedule |
| `GET` | `/api/routes/{routeId}/schedules/{scheduleId}` | Get an active schedule |
| `PUT/PATCH` | `/api/routes/{routeId}/schedules/{scheduleId}` | Update a schedule |
| `DELETE` | `/api/routes/{routeId}/schedules/{scheduleId}` | Soft-deactivate a schedule |

Schedule days accept comma- or space-separated values from
`MON,TUE,WED,THU,FRI,SAT,SUN`, or `DAILY`.

## Search

```http
GET /api/routes/search?from=Beta&to=Gamma&travelDate=2026-07-27
```

Search matches any ordered stops on an active route, including intermediate
stops. `travelDate` is optional; when supplied, only schedules operating on
that weekday are returned.

Phase 2 supports direct journeys on one route. Transfer planning is not yet
included.

## Fare quotation

Existing ticket-service calls remain supported:

```http
GET /api/routes/{routeId}/fare?sourceStopId=1&destinationStopId=3
```

For schedule-aware peak pricing, provide either `scheduleId` or
`departureTime`, but not both:

```http
GET /api/routes/{routeId}/fare?sourceStopId=1&destinationStopId=3&scheduleId=10
```

Without a schedule or departure time, the endpoint returns the off-peak fare.
