# Spring Boot Inventory Management - Deep Dive Architecture
This document serves as a comprehensive, layer-by-layer architectural deep dive into the Spring Boot Inventory Management System. It maps how data enters, how it behaves in memory and the database, and how event-driven patterns scale the system.

---

## 1. Project Structure Breakdown

The codebase strictly follows standard Domain-Driven Design (DDD) layered architecture principles inside a monolithic Spring Boot boundary.

*   **`controller/` (Presentation Layer)**
    *   Split into `admin/` and `mobile/` packages to clearly separate API scopes (B2B vs B2C). Handles HTTP routing, JSON serialization, and input validation.
*   **`service/` (Business Logic Layer)**
    *   Contains the core orchestration. Classes like `OrderService`, `ProductService`, and `NotificationService` manipulate data here. Responsibilities include transaction boundaries and caching logic.
*   **`repository/` (Data Access Layer)**
    *   Spring Data JPA interfaces extending `JpaRepository`. Executes SQL queries, handles JPA pagination, and uses custom `@Query` annotations for optimistic locking and complex reads.
*   **`entity/` (Domain Model)**
    *   Hibernate managed POJOs (e.g., `Order`, `Product`, `User`). Define the exact SQL table schemas, column types, and foreign key relationships.
*   **`dto/` (Data Transfer Objects)**
    *   Records/classes used to move data in and out of the API. Ensures database entities aren't accidentally serialized directly to the frontend.
*   **`config/` (Infrastructure Configuration)**
    *   Holds bean configurations for `kafka/`, `websocket/`, `security` (JWT), and system services (e.g., Swagger/OpenAPI).
*   **`exception/` & `mapper/` (Utilities)**
    *   `exception/` handles global API error shapes. `mapper/` handles DTO-to-Entity translation mappings.

---

## 2. End-to-End Flow: Order Creation

Let's trace **what actually happens** when a customer places an order on their mobile device.

1.  **API Request Enters**
    *   *Class:* `MobileOrderController` -> *Method:* `placeOrder(@Valid OrderRequest, @AuthenticationPrincipal User)`
    *   *Mechanism:* Spring Security intercepts the HTTP request, validates the JWT, extracts the User, and maps the JSON to the `OrderRequest` DTO.
2.  **Controller Handoff**
    *   The controller logs the request and delegates everything to `OrderService.placeOrder(request, user)`.
3.  **Business Logic & Verification**
    *   *Class:* `OrderService` -> *Method:* `placeOrder`
    *   *Mechanism:* The system fetches the required `Product` entities. It checks if `product.getStockQuantity() < itemRequest.getQuantity()`. If true, it throws an `InsufficientStockException`.
4.  **Database Concurrency Check & Update**
    *   *Class:* `ProductRepository` -> *Method:* `decrementStock(...)`
    *   *Mechanism:* An atomic `@Modifying` SQL query executes: `UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity, p.version = p.version + 1 WHERE p.id = :id AND p.stockQuantity >= :quantity AND p.version = :version`.
    *   This is **Optimistic Locking**. If two customers buy the last item at the exact millisecond, one request will update the version column, and the second request will fail (updating 0 rows), thus preventing negative stock.
5.  **Commit the Order**
    *   *Class:* `OrderRepository` -> *Method:* `save(order)`
    *   *Mechanism:* The `Order` and its cascading `OrderItems` are persisted to PostgreSQL. `@CacheEvict(value = {"user_orders", "products"}, allEntries = true)` runs, clearing stale data from memory.
6.  **Response**
    *   The entity is mapped back to an `OrderResponse` DTO and returned as HTTP 201 Created.

---

## 3. Layer-by-Layer Explanation

### Controllers (`MobileOrderController`, `AdminOrderController`)
- **Endpoints:** Define paths like `POST /api/v1/mobile/orders` or `PUT /api/v1/admin/orders/{id}/status`.
- **Purpose:** Strictly routing and HTTP response wrapping. They know nothing about "how" stock is managed, only varying by user roles. Note the use of `@AuthenticationPrincipal` which prevents users from passing spoofed IDs in JSON payloads.

### Services (`OrderService`, `NotificationService`)
- **Business Logic:** Heart of the application. `@Transactional` annotations live here. If `placeOrder` throws an exception on line 80, the database rolls back any stock deductions automatically.
- **Key responsibility:** Encapsulates complicated rules (like calculating price grids or sending Kafka events upon status changes).

### Repositories (`ProductRepository`, `OrderRepository`)
- **Database Interaction:** Abstracted away via Hibernate. Note the explicit raw queries like `findLowStockProducts`. Heavily leverages pagination (`Pageable`).

### Entities (`Order`, `Product`, `User`)
- **Data Structure:** Utilize Lombok (`@Getter`, `@Builder`).
- **Relationships:** Uses `@ManyToOne(fetch = FetchType.LAZY)`. Lazy fetching protects memory; `user` data isn't loaded from the DB until specifically requested in Java code!

### Configurations (`SecurityConfig`, `KafkaConfig`, `WebSocketConfig`)
- Setup infrastructure constraints. Security explicitly restricts endpoints under `/api/v1/admin/` to "ROLE_ADMIN". Kafka wires up the producer factories to Bootstrap Servers.

---

## 4. Integration Points (Kafka & WebSockets)

*   **Kafka Integration:** Positioned strictly at boundary state changes. `OrderService.updateOrderStatus` produces a message when an order reaches `DELIVERED`. It doesn't halt the user request to process notifications; it fires the ID into Kafka and forgets about it.
*   **WebSocket Integration:** The `NotificationService` listens to Kafka. It generates a readable `Notification` entity, then fires it over STOMP `SimpMessagingTemplate` to exactly one connected `User` queue.

---

## 5. Execution Flow (Deep Dive into Asynchronous Delivery)

Let's look at the Order Delivered Flow.
*   **In-Memory (Heap):** Admin calls HTTP PUT. `Order` object is fetched from Hibernate L1 cache. Status is changed. An `OrderCompletedEvent` DTO is instantiated in heap memory.
*   **Database (Disk DB):** Hibernate generates an `UPDATE orders SET status = 'DELIVERED'` query.
*   **Asynchronous (Network IO / Kafka):** `KafkaTemplate` serializes the DTO to bytes and ships it over TCP to the physical Kafka Broker. The HTTP request terminates, and the Admin is freed.
*   **Asynchronous (Consumer / Background Thread):** The Kafka listener thread wakes up, pulls the bytes, and rebuilds the `OrderCompletedEvent`.
*   **Real-time (WebSocket):** Spring's STOMP broker looks up the `ConcurrentHashMap` of active TCP sockets for that User's `username` (JWT claim). It frames the text into a WebSocket message and pushes it down the pipe to the mobile/web browser instantly.

---

## 6. Design Patterns Used

1.  **MVC (Model-View-Controller):** Standard Spring Web paradigm. However, because this is an API, the "View" is highly structured JSON (DTOs) rather than HTML.
2.  **Dependency Injection (DI) & Inversion of Control:** Lombok `@RequiredArgsConstructor` handles Constructor Injection. Allows easy mocking of `OrderRepository` when unit testing `OrderService`.
3.  **Repository Pattern / Active Record:** Using Spring Data JPA. Decouples the application from SQL specific logic (mostly).
4.  **Event-Driven Architecture (Publisher-Subscriber):** Using Kafka. `OrderService` (Publisher) doesn't know about `NotificationService` (Subscriber).
5.  **Optimistic Concurrency Control:** Done via `@Version` columns. Fixes race conditions without blocking table locks.

---

## 7. Code Quality & Issues (Critical Mentor Feedback)

*   **Code Smell (Transaction Risk):** Inside `OrderService.updateOrderStatus`, you produce the Kafka event *inside* the `@Transactional` method block `publishOrderCompletedEvent(order);`. If the database fails to commit right after this line, the Kafka event is still fired. Users will receive "Order Delivered" notifications for an order that rolled back to "Pending" in the database.
    *   *Fix:* Use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` or `TransactionSynchronizationManager.registerSynchronization`.
*   **Tight Coupling / Extensibility:** Everything looks solid, but `NotificationService` handles both WebSockets AND Emails. This violates the Single Responsibility Principle. If email formatting changes, it could introduce a bug that breaks WebSockets.
*   **Error Handling Gap:** The WebSocket implementation fails silently. In `NotificationService.sendWebSocketMessage`, catching `Exception` and returning `false` masks underlying network problems and will make debugging difficult at runtime.
*   **Caching Strategy:** Caching `user_orders` is great, but invalidating the ENTIRE `products` cache on every single `placeOrder` (`@CacheEvict(..., allEntries = true)`) means your cache hit rate for products will be near 0% under high load. Every time anyone buys anything, the entire product catalog is dumped from RAM.

---

## 8. Scalability & Improvements

*   **Better Architecture:** Split `NotificationService` into two listeners: `EmailNotificationConsumer` and `WebSocketNotificationConsumer`. Both can listen to the exact same Kafka topic (`orders.completed`) using different consumer groups. If Email breaks, WebSockets stay up!
*   **Kafka Optimization:** You use auto-commit offsets. Switch to manual acknowledgment (`ack.acknowledge()`) so if an email fails halfway, the message is not lost.
*   **WebSocket Scaling:** You are using Spring's SimpleBroker. That stores active web sockets purely in RAM on one server. If you deploy 3 servers behind a Load Balancer, a Kafka listener on Server A might try to push a websocket message to a User connected to Server B. You will need to switch the SimpleBroker to a RabbitMQ/ActiveMQ STOMP relay to scale WebSockets across multiple nodes.

---

## 9. Visual Diagrams

**System Architecture & Request Flow:**
```mermaid
graph TD
    Client(Mobile App / Web) -->|REST / JWT| Gateway(Spring Boot Monolith)
    Gateway --> SecurityFilter{JWT Filter}
    SecurityFilter -->|Valid| Controllers
    
    Controllers(Controllers Layer) --> Services(Service Layer)
    Services --> Repositories(Repository Layer)
    Repositories <--> DB[(PostgreSQL)]
    
    Services -->|Produce Event| Kafka[Apache Kafka Cluster]
    Kafka -->|Consume Event| NotificationService(Notification Service)
    
    NotificationService --> EmailAPI[SMTP Provider]
    NotificationService --> WSBroker[WebSocket STOMP Broker]
    
    Client <--|Persistent TCP Socket| WSBroker
```

---

## 10. Summary for Interviews

**If an interviewer asks "Explain your project in 2 minutes":**
> "I built an e-commerce inventory management system using Spring Boot. It provides REST APIs for mobile and admin users, secured by stateless JWT authentication. At its core, it handles concurrent order placement using Optimistic Locking to gracefully prevent overselling without crashing the database. I layered Event-Driven Architecture over it using Apache Kafka: whenever critical data changes—like an order shipping—Kafka decouples the heavy Lifting. It asynchronously triggers downstream micro-services, firing off emails and using STOMP WebSockets to push real-time UI updates straight to the customer's device."

**If an interviewer asks "What architectures/challenges did you implement?" (5 minutes):**
> "My system is built on Domain-Driven Design principles but adapted for a monolithic Spring Boot boundary. I focused heavily on data integrity and throughput.
> 
> 1. **Concurrency Controls:** I encountered race conditions with inventory. If two users buy the last item at the exact same millisecond, standard SQL updates might result in negative stock. I implemented Optimistic Locking using JPA row versions so the slower thread immediately throws a safe exception rather than deadlocking the DB.
> 2. **Event-Driven Decoupling:** I realized sending emails or websocket pushes inside an HTTP transaction was dangerous because if an external SMTP server slowed down, the database transaction was stuck open. So, I integrated Kafka. My Order Service now strictly updates the DB and throws an `OrderCompletedEvent` into a Kafka partition and returns an HTTP 201 immediately. 
> 3. **Real-time Comms:** I implemented a background listener that pulls from Kafka, parses the DTO, and then bridges the gap to the frontend using WebSockets with STOMP formatting. Because I pass my JWTs through the WebSocket headers, my server securely maps active TCP sockets to authenticated users and directs the payloads exactly where they need to go. Fast, resilient, and highly decoupled."
