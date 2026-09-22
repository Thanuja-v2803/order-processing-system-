# Order Processing System

Spring Boot backend (REST + RabbitMQ async processing + WebSocket status
push) with a React frontend.

## How it fits together

1. `POST /api/orders` saves the order as `PENDING` and publishes an
   `OrderEvent` to RabbitMQ, returning immediately (`202 Accepted`).
2. `OrderProcessingService` consumes the queue, reserves inventory for
   each item under a pessimistic lock (`InventoryService`), and updates
   the order's status (`PROCESSING` → `CONFIRMED`/`FAILED`).
3. Every status change is broadcast over STOMP/WebSocket to
   `/topic/orders` and `/topic/orders/{id}`.
4. The React frontend fetches the order list once, then patches
   statuses live via `useWebSocket`.

## Running it

**Backend** — needs Postgres and RabbitMQ running locally (or update
`application.yml`):
```
cd backend
mvn spring-boot:run
```

**Frontend**:
```
cd frontend
npm install
npm run dev
```

## Known gaps to fill in before this is production-ready

- **No `Inventory` REST endpoints or seed data.** `InventoryPanel.jsx`
  calls `GET /api/inventory`, but there's no `InventoryController` or
  DB seeding yet — add one mirroring `OrderController`, plus a way to
  create/import inventory rows.
- **Pricing is stubbed.** `OrderService.toOrderItem()` hardcodes
  `unitPrice`; wire it to whatever your real pricing source is.
- **No idempotency check** if the same order event gets redelivered
  after a broker retry — safe here since reservation is a straight
  decrement, but add a guard (e.g. check status before reprocessing)
  if you add steps that aren't naturally idempotent.
- **CORS / allowed origins** in `WebSocketConfig` are wide open
  (`*`) — tighten for anything beyond local dev.
