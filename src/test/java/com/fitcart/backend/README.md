
# FitCart — Fitness Marketplace Backend

FitCart is an e-commerce application designed for fitness products such as supplements and gym equipment. The backend supports product browsing, shopping carts, wishlists, orders, inventory, user accounts, and role-based access.

## Features

- User login and registration
- Buyer, Seller, and Admin roles
- Product browsing and category management
- Seller product and inventory management
- Shopping cart and wishlist
- Order placement and order history
- JDBC-based database operations
- MySQL database integration
- BCrypt password hashing
- Spring Security authentication and authorization
- Simulated payment records for demonstration

## Technology Stack

- Java 26
- Spring Boot 4.1.1
- Maven
- MySQL
- JDBC / JdbcTemplate
- Spring Security
- HTML, CSS, and JavaScript

## Project Structure

```text
fitcart-backend/
├── src/
│   └── main/
│       ├── java/com/fitcart/backend/
│       │   ├── config/
│       │   ├── controller/
│       │   ├── dao/
│       │   ├── dto/
│       │   ├── exception/
│       │   ├── model/
│       │   └── service/
│       └── resources/
│           ├── static/
│           │   ├── index.html
│           │   ├── login.html
│           │   ├── style.css
│           │   └── app.js
│           └── application.properties
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## Requirements

Before running the application, install:

- A compatible Java Development Kit (JDK 26, matching the current project configuration)
- MySQL Server
- A terminal such as PowerShell
- A web browser

## Database Setup

1. Start your MySQL server.
2. Create or select the `fitcart` database.
3. Ensure the required tables are created.
4. Configure the database connection using your local Spring Boot configuration.

Example database creation:

```sql
CREATE DATABASE fitcart;
```

Configure your local database URL, username, and password using your project's configuration file or environment variables.

**Security:** Never commit database passwords, private credentials, or production secrets to GitHub.

## Run the Application

Open PowerShell in the project directory.

Compile the project:

```powershell
.\mvnw.cmd clean compile
```

Run automated tests:

```powershell
.\mvnw.cmd test
```

Start the application:

```powershell
.\mvnw.cmd spring-boot:run
```

Wait until Spring Boot reports that the application has started.

Open the application in your browser:

- Login page: http://localhost:8080/login.html
- Dashboard: http://localhost:8080/

Keep the terminal running while using the locally hosted website.

## Testing

Run the automated test suite with:

```powershell
.\mvnw.cmd test
```

The test suite covers authentication, controllers, database access, and service-layer functionality. Check the test output for the current results.

## Payments

Payment processing is simulated for educational and demonstration purposes. The application does not process real payments.

## Security Notes

- Existing user passwords have been migrated to BCrypt hashes.
- Spring Security protects authenticated endpoints.
- Access to administrative and seller operations is restricted by role.
- Resource ownership checks should be verified through testing before deployment.
- Configure database credentials securely and use appropriate production security settings before exposing the application publicly.

## Contributors

Add the names of your FitCart team members here.

## License

Add the license required by your project or institution.
