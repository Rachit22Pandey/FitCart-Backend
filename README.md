# FitCart Backend — Spring Boot + JDBC + MySQL

FitCart is a fitness-focused multi-vendor e-commerce platform for buying and selling fitness products (supplements, workout gear, apparel, etc.) supporting **Buyer**, **Seller**, and **Admin** roles.

This backend is built entirely with **Spring Boot**, **Spring JDBC (`JdbcTemplate`)**, and **MySQL**, adhering to a pure DAO architecture without Spring Data JPA / Hibernate.

---

## 1. Architecture

```text
HTTP Clients (Web Frontend / Postman / Mobile)
       │
       ▼
[ Controller Layer ]      (REST APIs, JSON serialization, status codes)
       │
       ▼
[ Service Layer ]         (Business logic, BCrypt hashing, atomic transactions)
       │
       ▼
[ DAO / JDBC Layer ]      (JdbcTemplate, Parameterized SQL queries, RowMappers)
       │
       ▼
[ MySQL Database ]        (Database: fitcart)
```

### Package Structure
- `com.fitcart.backend.config`: Security & bean configuration (`SecurityConfig` with `BCryptPasswordEncoder`).
- `com.fitcart.backend.controller`: REST Controllers exposing public & protected endpoints.
- `com.fitcart.backend.service`: Transactional business services.
- `com.fitcart.backend.dao` & `impl`: JDBC interfaces and implementations executing parameterized SQL.
- `com.fitcart.backend.model`: Domain POJOs (`User`, `Category`, `Product`, `Inventory`, `CartItem`, `WishlistItem`, `Address`, `Order`, `OrderItem`, `Payment`).
- `com.fitcart.backend.dto`: Request/Response payloads with validation (`RegisterRequest`, `LoginRequest`, `ProductRequest`, `PlaceOrderRequest`, etc.).
- `com.fitcart.backend.exception`: Standardized REST error handlers (`GlobalExceptionHandler`, `BadRequestException`, `ResourceNotFoundException`).

---

## 2. Technology Stack

- **Java**: 26.0.1
- **Spring Boot**: 4.1.1
- **Build Tool**: Apache Maven (with `mvnw` wrapper)
- **Database**: MySQL (`fitcart`)
- **Persistence**: Spring JDBC (`spring-boot-starter-jdbc`) with `JdbcTemplate`
- **Security**: Spring Security + BCrypt password hashing
- **Validation**: Jakarta Bean Validation (`spring-boot-starter-validation`)

---

## 3. Database Configuration & Credentials

Database credentials are **never hardcoded** in source files. Default settings use environment variables with local fallbacks in `src/main/resources/application.properties`:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/fitcart?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}
```

### Running with Local Credentials:

#### Option A: Set Environment Variable (PowerShell)
```powershell
$env:SPRING_DATASOURCE_PASSWORD="YOUR_MYSQL_PASSWORD"
.\mvnw.cmd spring-boot:run
```

#### Option B: Use `application-local.properties` (Untracked)
Create `application-local.properties` in `fitcart-backend/fitcart-backend/`:
```properties
spring.datasource.password=YOUR_MYSQL_PASSWORD
```
*(This file is included in `.gitignore` and is never committed to GitHub).*

---

## 4. Comprehensive REST API Reference

### 4.1 Authentication & Users
| Method | Endpoint | Description | Sample Request |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/register` | Register new account (`BUYER`, `SELLER`, `ADMIN`) | `{"name":"Alex","email":"alex@fit.com","password":"secretPass","role":"BUYER","phone":"9876543210"}` |
| `POST` | `/auth/login` | Log in with email & password | `{"email":"alex@fit.com","password":"secretPass"}` |
| `GET` | `/users` | Get all users | None |
| `GET` | `/users/{id}` | Get user by ID | None |

### 4.2 Categories
| Method | Endpoint | Description | Sample Request |
| :--- | :--- | :--- | :--- |
| `GET` | `/categories` | List all categories | None |
| `GET` | `/categories/{id}` | Get category by ID | None |
| `POST` | `/categories` | Create category (Admin) | `{"categoryName":"Supplements"}` |
| `PUT` | `/categories/{id}` | Update category | `{"categoryName":"Workout Gear"}` |
| `DELETE` | `/categories/{id}` | Delete category | None |

### 4.3 Products & Catalog
| Method | Endpoint | Description | Sample Request |
| :--- | :--- | :--- | :--- |
| `GET` | `/products` | Filter & search products | Query params: `?keyword=whey&categoryId=1` |
| `GET` | `/products/{id}` | Get product details + stock | None |
| `GET` | `/products/seller/{sellerId}` | Get products created by seller | None |
| `POST` | `/products` | Create product + initial stock (Seller/Admin) | `{"productName":"Whey Gold 2kg","description":"Isolate Protein","price":59.99,"categoryId":1,"sellerId":2,"initialStock":40}` |
| `PUT` | `/products/{id}` | Update product information | `{"productName":"Whey Gold 2kg","description":"New flavor","price":54.99,"categoryId":1,"sellerId":2,"initialStock":45}` |
| `DELETE` | `/products/{id}` | Delete product and its inventory | None |

### 4.4 Inventory Management
| Method | Endpoint | Description | Sample Request |
| :--- | :--- | :--- | :--- |
| `GET` | `/inventory/product/{productId}` | Get current stock for product | None |
| `PUT` | `/inventory/product/{productId}` | Update stock quantity | `{"stockQuantity":75}` |

### 4.5 Shopping Cart (Buyer)
| Method | Endpoint | Description | Sample Request |
| :--- | :--- | :--- | :--- |
| `POST` | `/cart` | Add item to cart (verifies stock) | `{"userId":1,"productId":5,"quantity":2}` |
| `GET` | `/cart/{userId}` | Get buyer's cart items with subtotals | None |
| `PUT` | `/cart/{cartId}?quantity=3` | Update item quantity | Query param: `?quantity=3` |
| `DELETE` | `/cart/{cartId}` | Remove single item from cart | None |
| `DELETE` | `/cart/user/{userId}` | Clear buyer's entire cart | None |

### 4.6 Wishlist (Buyer)
| Method | Endpoint | Description | Sample Request |
| :--- | :--- | :--- | :--- |
| `POST` | `/wishlist` | Add product to wishlist | `{"userId":1,"productId":5}` |
| `GET` | `/wishlist/{userId}` | Get buyer's saved products | None |
| `DELETE` | `/wishlist/{wishlistId}` | Remove from wishlist | None |
| `DELETE` | `/wishlist/user/{userId}/product/{productId}` | Remove by user & product | None |

### 4.7 Addresses (Buyer)
| Method | Endpoint | Description | Sample Request |
| :--- | :--- | :--- | :--- |
| `POST` | `/addresses` | Add shipping address | `{"userId":1,"street":"42 Gym Rd","city":"FitCity","state":"FitState","postalCode":"110001","country":"India","phone":"9876543210"}` |
| `GET` | `/addresses/user/{userId}` | Get user's addresses | None |
| `GET` | `/addresses/{id}` | Get address by ID | None |
| `PUT` | `/addresses/{id}` | Update address | Address fields |
| `DELETE` | `/addresses/{id}` | Delete address | None |

### 4.8 Orders & Simulated Payments
| Method | Endpoint | Description | Sample Request |
| :--- | :--- | :--- | :--- |
| `POST` | `/orders` | Place order (direct or cart checkout) | `{"userId":1,"useCart":true,"paymentMethod":"UPI","addressId":1}` OR `{"userId":1,"items":[{"productId":2,"quantity":1}],"paymentMethod":"CARD"}` |
| `GET` | `/orders/{id}` | Get full order details | None |
| `GET` | `/orders/user/{userId}` | Buyer's order history | None |
| `GET` | `/orders/seller/{sellerId}` | Orders containing seller's items | None |
| `GET` | `/orders` | All platform orders (Admin) | None |
| `PUT` | `/orders/{id}/status` | Update order status | `{"status":"SHIPPED"}` |

> [!NOTE]
> **Demonstration Payments**: As required, this platform does not implement a mock external gateway pretending to process live bank transactions. Order checkouts record simulated payments tagged with `DEMO_PAID` and unique transaction tokens (`DEMO-TXN-XXXXXXXX`).

---

## 5. Build, Test & Run Commands

```powershell
# Compile all source files
.\mvnw.cmd clean compile

# Execute complete automated test suite (38 unit & integration tests)
.\mvnw.cmd test

# Launch the Spring Boot Web Application
.\mvnw.cmd spring-boot:run
```
When running, the REST backend is served at `http://localhost:8080`.
