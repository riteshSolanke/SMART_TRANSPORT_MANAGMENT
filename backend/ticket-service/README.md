# Ticket service

The ticket service owns booking records and capacity consumption. Passenger
registration and profile data remain owned by auth-service; tickets reference
the authenticated auth `userId` and do not duplicate credentials or personal
data.

All endpoints must be called through the API gateway.

## Booking

```http
POST /api/tickets
Authorization: Bearer <access-token>
Idempotency-Key: booking-20260803-user42-attempt1
Content-Type: application/json

{
  "routeId": 10,
  "scheduleId": 20,
  "sourceStopId": 100,
  "destinationStopId": 200,
  "serviceDate": "2026-08-03",
  "passengerCount": 2
}
```

`Idempotency-Key` is required and must contain 8 to 64 letters, numbers, dots,
underscores, colons, or hyphens. Retrying the same request with the same key
returns the existing ticket. Reusing a key for a different payload returns
HTTP 409.

Before saving a booking, ticket-service:

1. gets a schedule-specific fare from route-service;
2. rejects a schedule that already departed;
3. obtains the assigned vehicle and capacity from vehicle-service;
4. counts booked/used passengers for the same route, schedule, and date;
5. saves the unit fare and total fare as immutable booking snapshots.

Bookings run at serializable transaction isolation to prevent concurrent
requests from overselling the known vehicle capacity.

## Passenger access

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/tickets` | Book a ticket |
| `GET` | `/api/tickets/me` | List the authenticated passenger's tickets |
| `GET` | `/api/tickets/{ticketId}` | Get an owned ticket |
| `GET` | `/api/tickets/pnr/{pnr}` | Get an owned ticket by PNR |
| `DELETE` | `/api/tickets/{ticketId}` | Cancel before departure |

Passengers cannot read, cancel, or create tickets for another user.
Administrators may book on behalf of another user by supplying `userId`.

## Compatibility

Flyway migration `V2__redesign_ticket_booking.sql` adds the new schedule,
capacity, fare-snapshot, and idempotency columns without deleting legacy
tickets. These fields can be null only on records created before Phase 4.
