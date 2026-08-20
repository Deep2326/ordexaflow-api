# Business Requirements

## Actors

- **Visitor:** browses the public catalog and creates an account.
- **Customer (`ROLE_USER`):** manages their own cart, addresses, and orders.
- **Administrator (`ROLE_ADMIN`):** manages categories, products, inventory, and order status.

Seller accounts are outside Version 1.

## Functional requirements

### Identity and access

- A visitor can register with a unique, normalized email address and a strong password.
- A registered user can log in, refresh a session, and log out.
- Refresh tokens are opaque, stored only as hashes, rotated after every use, and revoked on logout.
- Reuse of a rotated refresh token revokes the remaining session token family.
- Passwords are stored only as adaptive one-way hashes.
- A customer can read or modify only their own private resources.
- Administrative operations require `ROLE_ADMIN`.

### Catalog

- Anyone can list active products and view an active product by ID.
- Product lists support pagination, sorting, category filtering, price range filtering, and text search.
- An administrator can create and update categories and products.
- Product deletion is soft: discontinued products remain available to historical orders but disappear from the public catalog.
- SKU and category slug values are unique.

### Inventory

- Each product has one inventory record with available quantity.
- Only administrators can adjust inventory directly.
- Quantity cannot be negative.
- Checkout rejects unavailable quantities.
- Concurrent checkouts must not oversell stock; optimistic locking detects conflicting updates.

### Cart

- Each customer has at most one active cart.
- A customer can view, add, update, and remove items in their own cart.
- An item quantity must be a positive integer and cannot exceed currently available inventory.
- Adding the same product updates its existing cart item rather than creating a duplicate.
- Catalog prices are displayed in the cart, but the final price is captured again during checkout.

### Orders

- A customer can place an order from a non-empty cart and a valid delivery address.
- Checkout atomically validates inventory, snapshots product name/SKU/price, creates the order and items, reduces inventory, and clears the cart.
- A customer can list and view only their own orders.
- Administrators can list all orders and apply valid status transitions.
- Initial Version 1 status is `PENDING`; payment integration is deferred, so an administrator or later payment adapter moves it to `PAID`.
- Allowed forward flow is `PENDING → PAID → PROCESSING → SHIPPED → DELIVERED`.
- `PENDING` and `PAID` orders may transition to `CANCELLED`; cancellation restores inventory exactly once.
- Terminal states are `DELIVERED` and `CANCELLED`.

## Non-functional requirements

- JSON REST API with OpenAPI documentation.
- UTC timestamps represented as ISO-8601 values.
- Monetary values use `BigDecimal` in Java and fixed-precision `numeric` columns; Version 1 uses one configured currency (USD).
- Every list endpoint is bounded by pagination; maximum page size is 100.
- Database changes are versioned with Flyway; Hibernate schema auto-update is disabled.
- Consistent error bodies contain timestamp, HTTP status, stable error code, message, path, and optional field violations.
- Logs must not contain passwords, tokens, secrets, or payment data.
- Health endpoints support container orchestration; sensitive actuator endpoints are not public.
- Core service rules have unit tests and database behavior has PostgreSQL Testcontainers integration tests.

## Version boundaries

### Version 1 (must have)

Identity, JWT/RBAC, refresh-token rotation/revocation, catalog, categories, inventory, cart, orders,
validation, exception handling, PostgreSQL/Flyway, OpenAPI, tests, Docker, and CI.

### Version 2

OAuth2 login, auditing enhancements, advanced search, performance tuning, and expanded observability.

### Later

Redis, rate limiting, email, a real payment provider, Azure hosting, Key Vault, and Application Insights.

## Acceptance scenarios

1. A visitor registers and logs in, then accesses an authenticated endpoint with an access token.
2. A user rotates a refresh token once; replaying the old token fails and revokes the remaining session.
3. A customer cannot call an admin endpoint or view another customer's order.
4. An admin creates a category, product, and stock; a visitor finds it through catalog filters.
5. A customer adds stock to a cart and checks out; one order is created, stock decreases, price is snapshotted, and the cart is emptied in one transaction.
6. Checkout failure leaves the order, inventory, and cart unchanged.
7. Two customers competing for the final unit cannot both complete checkout.
8. Invalid status transitions return a stable conflict error.
