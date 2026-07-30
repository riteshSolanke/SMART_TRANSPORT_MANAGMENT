# Analytics service

The analytics service aggregates service-owned summaries without reading other
microservices' database tables. It persists generated reporting snapshots in
`analytics_db` and runs on port `9096` by default.

Only `TRANSPORT_MANAGER` and `ADMIN` users can access the API.

## API

- `GET /api/analytics/usage?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/analytics/revenue?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/analytics/performance?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `POST /api/analytics/reports?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/analytics/reports`
- `GET /api/analytics/reports/{reportId}`

On-time performance compares each completed assignment timestamp with the
route schedule's expected arrival time. The default grace period is 15 minutes
and can be changed through `ANALYTICS_ON_TIME_GRACE_MINUTES`.
