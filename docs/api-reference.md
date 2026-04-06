# Complete API Reference Guide
This document details every endpoint in the Inventory Management System, exactly what payload to pass, and a behind-the-scenes look at what the backend does when the endpoint is hit.

---

## 1. Authentication APIs (Mobile)

### `POST /api/v1/mobile/auth/register`
*   **Purpose**: Creates a new customer account.
*   **Requirement**: No authentication needed (Public).
*   **Example Request Body**:
    ```json
    {
      "fullName": "John Doe",
      "email": "john@example.com",
      "password": "SecurePassword123!"
    }
    ```
*   **Backend Flow**: `MobileAuthController` receives the payload. `AuthService` hashes the password using BCrypt and saves the new `User` to PostgreSQL with `status=PENDING_VERIFICATION`. It then generates a 6-digit OTP, records a 5-minute expiry, and triggers `OtpEmailService` to securely dispatch the code asynchronously via Mailtrap. It returns `202 Accepted` instead of a JWT token.

### `POST /api/v1/mobile/auth/verify-otp`
*   **Purpose**: Validates the 6-digit code to completely unlock the account.
*   **Requirement**: No authentication needed (Public).
*   **Example Request Body**:
    ```json
    {
      "email": "john@example.com",
      "otp": "123456"
    }
    ```
*   **Backend Flow**: Checks if the exact account has the matching unexpired OTP and fewer than 3 failed attempts. If valid, changes status to `ACTIVE`, purges OTP columns from memory, and issues the official JWT authentication token.

### `POST /api/v1/mobile/auth/resend-otp`
*   **Purpose**: Requests a fresh 6-digit pin if the old one expired.
*   **Requirement**: No authentication needed (Public).
*   **Example Request Body**:
    ```json
    {
      "email": "john@example.com"
    }
    ```
*   **Backend Flow**: Purges the old OTP, generates a fresh 6-digit pin, resets verification attempts to 0, and retriggers `OtpEmailService`.

### `POST /api/v1/mobile/auth/login`
*   **Purpose**: Authenticates an existing user and returns a token.
*   **Requirement**: No authentication needed (Public).
*   **Example Request Body**:
    ```json
    {
      "email": "john@example.com",
      "password": "SecurePassword123!"
    }
    ```
*   **Backend Flow**: `AuthService` checks the DB for the user and verifies the BCrypt Hash. If the user's `status` equals `PENDING_VERIFICATION`, it securely blocks access and throws an Unauthorized exception. If `ACTIVE` and valid, the `JwtProvider` generates a signed Bearer token and returns it in the response.

---

## 2. Product APIs (Admin & Mobile)

### `POST /api/v1/admin/products`
*   **Purpose**: Allows an administrator to add a new physical item into the inventory.
*   **Requirement**: Header `Authorization: Bearer <token>` (Must have `ROLE_ADMIN`).
*   **Example Request Body**:
    ```json
    {
      "name": "Wireless Headphones",
      "sku": "WH-1002",
      "description": "Noise-cancelling bluetooth headphones",
      "price": 149.99,
      "stockQuantity": 50,
      "category": "Electronics"
    }
    ```
*   **Backend Flow**: `AdminProductController` maps the request. `ProductService` saves a new `Product` entity to PostgreSQL.

### `GET /api/v1/mobile/products`
*   **Purpose**: Allows a customer to browse the product catalog.
*   **Requirement**: Required `ROLE_CUSTOMER` or `ROLE_ADMIN`.
*   **Query Params**: `?search=headphones&category=Electronics&page=0&size=20`
*   **Example Request Body**: *None (GET request)*
*   **Backend Flow**: Hits `MobileProductController`. `ProductService` safely queries the database using `Like` conditions and pagination. Excludes logically deleted items (`deleted = false`). 

---

## 3. Order APIs (Mobile)

### `POST /api/v1/mobile/orders`
*   **Purpose**: Customer places an order for one or more items.
*   **Requirement**: Header `Authorization: Bearer <token>` (Must be authenticated `ROLE_CUSTOMER`).
*   **Example Request Body**:
    ```json
    {
      "items": [
        {
          "productId": 1,
          "quantity": 2
        }
      ]
    }
    ```
*   **Backend Flow (CRITICAL)**: 
    1. Spring Security automatically pulls the `User` from the JWT token so they cannot spoof who is ordering.
    2. `OrderService` verifies if `ProductId=1` has at least 2 units in stock.
    3. Executes Optimistic Locking query to safely subtract 2 units from the inventory `Product` table.
    4. Creates an `Order` and `OrderItem` in the database with status `PENDING`.

### `GET /api/v1/mobile/orders/me`
*   **Purpose**: Customer views their past shopping history.
*   **Requirement**: Header `Authorization: Bearer <token>`.
*   **Example Request Body**: *None (GET request)*
*   **Backend Flow**: Fetches records from `OrderRepository` matching `userId = userId_from_token`, sorts them by newest first, and returns paginated JSON.

---

## 4. Order Management APIs (Admin)

### `GET /api/v1/admin/orders`
*   **Purpose**: View a global ledger of all orders across the system.
*   **Requirement**: Header `Authorization: Bearer <token>` (`ROLE_ADMIN`).
*   **Query Params**: `?status=PENDING&page=0&size=20`
*   **Backend Flow**: Administrative DB lookup across all user IDs.

### `PATCH /api/v1/admin/orders/{id}/status`
*   **Purpose**: Used by the warehouse/admin to update the lifecycle of an order (e.g., shipping the item).
*   **Requirement**: Header `Authorization: Bearer <token>` (`ROLE_ADMIN`).
*   **Example Request Body**:
    ```json
    {
      "status": "DELIVERED"
    }
    ```
*   **Backend Flow**:
    1. Updates the `status` column in PostgreSQL to `DELIVERED`.
    2. **Event Trigger:** Bypasses synchronous notification and fires an `OrderCompletedEvent` DTO directly into the Apache Kafka topic `orders.completed`.

---

## 5. Notification / Real-time APIs

### `GET /api/v1/mobile/notifications` (via NotificationController)
*   **Purpose**: Fetch the history of messages/alerts sent to the user.
*   **Requirement**: Header `Authorization: Bearer <token>`.

### `STOMP WebSockets CONNECT /ws/notifications`
*   **Purpose**: Real-time push connection for live alerts without refreshing.
*   **Connection details**: Connecting client must pass the JWT token in `CONNECT` headers to authenticate the socket.
*   **Backend Flow**: When `NotificationService` consumes the Kafka event from `orders.completed`, it formats a message string, saves a database copy, attempts to send an email, and then uses `SimpMessagingTemplate` to push that message straight through this STOMP socket to the frontend user immediately.
