# Payment service

The payment service owns simulated fare charges, payment history, idempotent
retries, and pre-departure refunds. It runs on port `9095` by default and stores
only payment metadata—never card, CVV, PIN, or UPI credentials.

## API

- `POST /api/payments` — process a ticket payment; requires
  `Idempotency-Key` (8–64 URL-safe characters).
- `GET /api/payments/me` — authenticated passenger payment history.
- `GET /api/payments/{paymentId}` — payment details for the owner or admin.
- `GET /api/payments/ticket/{ticketId}` — payment attempts for a ticket.
- `POST /api/payments/{paymentId}/refund` — refund a successful payment before
  ticket departure.

Example request body:

```json
{
  "ticketId": 1,
  "paymentMethod": "UPI"
}
```

Supported methods are `CARD`, `UPI`, `WALLET`, and `CASH`. The outcome is
controlled locally by `PAYMENT_SIMULATOR_OUTCOME=SUCCESS|FAILURE`.
