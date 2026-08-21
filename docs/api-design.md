# REST API Design

## Conventions

- Base path: `/api/v1`
- Media type: `application/json`
- IDs: UUIDs; order numbers are separate human-readable public identifiers.
- Protected endpoints use `Authorization: Bearer <access-token>`.
- Pagination uses `page` (zero-based), `size` (default 20, maximum 100), and repeatable `sort=field,direction`.
- Successful creation returns `201 Created` and a `Location` header where applicable.
- Updates use `PUT` for complete editable representations and `PATCH` for explicit state transitions.

## Authentication

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/auth/register` | Public | Register a customer |
| POST | `/auth/login` | Public | Issue an access/refresh token pair |
| POST | `/auth/refresh` | Public with refresh token | Rotate refresh token and issue a new token pair |
| POST | `/auth/logout` | Public with refresh token | Revoke the presented refresh token; idempotent |
| GET | `/users/me` | Customer/Admin | Current user profile |

## Public catalog

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/products` | Public | Paginated catalog search |
| GET | `/products/{productId}` | Public | Active product detail |
| GET | `/categories` | Public | Active categories |

`GET /products` supports `page`, `size`, `sort`, `category`, `minPrice`, `maxPrice`, and `q`.
Page size is limited to 100. Supported sort fields are `sku`, `name`, `price`, and `createdAt`;
repeat `sort=field,direction` to apply multiple sort orders.

## Cart

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/cart` | Customer | Read own cart and calculated subtotal |
| POST | `/cart/items` | Customer | Add `{productId, quantity}` |
| PUT | `/cart/items/{itemId}` | Customer | Replace item quantity |
| DELETE | `/cart/items/{itemId}` | Customer | Remove an item |
| DELETE | `/cart` | Customer | Clear the cart |

Cart identity always comes from the access token; clients never submit a user ID. Adding a product already in
the cart increases its quantity, while `PUT` replaces the quantity. Cart prices and totals are calculated from
the current catalog. Cart operations validate current inventory for customer feedback but do not reserve or
decrement stock; checkout performs the authoritative inventory check.

## Addresses and orders

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/addresses` | Customer | List own addresses |
| POST | `/addresses` | Customer | Create an address |
| PUT | `/addresses/{addressId}` | Customer | Update own address |
| DELETE | `/addresses/{addressId}` | Customer | Delete own address if allowed |
| POST | `/orders` | Customer | Checkout using `{addressId}` |
| GET | `/orders` | Customer | Paginated own order history |
| GET | `/orders/{orderId}` | Customer | Read own order detail |
| POST | `/orders/{orderId}/cancellation` | Customer | Request cancellation when permitted |

## Administration

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/admin/categories` | Admin | Create category |
| PUT | `/admin/categories/{categoryId}` | Admin | Update category |
| POST | `/admin/products` | Admin | Create product and initial inventory |
| PUT | `/admin/products/{productId}` | Admin | Update product |
| DELETE | `/admin/products/{productId}` | Admin | Discontinue product |
| PUT | `/admin/products/{productId}/inventory` | Admin | Set absolute available stock quantity |
| GET | `/admin/orders` | Admin | Filtered, paginated order queue |
| GET | `/admin/orders/{orderId}` | Admin | Read any order |
| PATCH | `/admin/orders/{orderId}/status` | Admin | Apply a valid status transition |

Admin order filters include `status`, `createdFrom`, `createdTo`, `page`, `size`, and `sort`.

## Representative contracts

Register request:

```json
{
  "firstName": "Avery",
  "lastName": "Chen",
  "email": "avery@example.com",
  "password": "correct-horse-battery-staple"
}
```

Login request:

```json
{
  "email": "avery@example.com",
  "password": "correct-horse-battery-staple"
}
```

Login and refresh response:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "f1qgZ5...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshExpiresIn": 2592000
}
```

Refresh and logout request:

```json
{
  "refreshToken": "f1qgZ5..."
}
```

Every successful refresh invalidates the presented refresh token and returns a replacement. An invalid,
expired, revoked, or reused token returns `401 INVALID_REFRESH_TOKEN`. Logout is idempotent and returns
`204 No Content`, including when the token no longer exists.

Current-user response:

```json
{
  "id": "b03c5d0e-a143-4e12-8812-f5647c1c93e3",
  "firstName": "Avery",
  "lastName": "Chen",
  "email": "avery@example.com",
  "roles": ["ROLE_USER"],
  "createdAt": "2026-08-19T18:30:00Z"
}
```

Product response:

```json
{
  "id": "f7fc17df-9128-44fd-91c0-cfa82c358312",
  "sku": "LAPTOP-001",
  "name": "Developer Laptop",
  "description": "A portable workstation",
  "price": 1299.00,
  "currency": "USD",
  "availableQuantity": 8,
  "active": true,
  "category": { "id": "ca9d7780-d51f-4fc1-8b95-a7eec7c506ba", "name": "Laptops", "slug": "laptops" }
}
```

Cart response:

```json
{
  "id": "06182273-e996-4dfb-aa68-741dfd74aa32",
  "items": [
    {
      "id": "de8cce56-0e31-473a-9dc3-b4eaf69e0943",
      "productId": "f7fc17df-9128-44fd-91c0-cfa82c358312",
      "sku": "LAPTOP-001",
      "name": "Developer Laptop",
      "unitPrice": 1299.00,
      "currency": "USD",
      "quantity": 2,
      "lineTotal": 2598.00,
      "availableQuantity": 8,
      "available": true
    }
  ],
  "totalItems": 2,
  "subtotal": 2598.00,
  "currency": "USD",
  "updatedAt": "2026-08-20T22:00:00Z"
}
```

Admin product creation request:

```json
{
  "sku": "LAPTOP-001",
  "name": "Developer Laptop",
  "description": "A portable workstation",
  "price": 1299.00,
  "currency": "USD",
  "categoryId": "ca9d7780-d51f-4fc1-8b95-a7eec7c506ba",
  "initialQuantity": 8
}
```

Page response:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

Error response:

```json
{
  "timestamp": "2026-08-19T18:30:00Z",
  "status": 409,
  "error": "INSUFFICIENT_INVENTORY",
  "message": "Requested quantity is no longer available",
  "path": "/api/v1/orders",
  "fieldErrors": []
}
```

## HTTP status policy

- `400` malformed input or validation failure
- `401` absent, invalid, or expired authentication
- `403` authenticated but not authorized
- `404` resource absent or not visible to the caller
- `409` uniqueness, inventory concurrency, or invalid state-transition conflict
- `201` creation, `200` retrieval/update, and `204` deletion/logout without a body

The implementation's generated OpenAPI document becomes the detailed source of truth; this file defines the intended resource model and endpoint surface.
