# Brokerage Firm API

This project is a Spring Boot application that provides backend API services for a brokerage firm, allowing employees to manage stock orders for their customers.

## Features

- Create, list, and cancel stock orders
- List customer assets
- Secure API with JWT-based authentication
- Admin and customer-specific roles and permissions
- Comprehensive error handling
- Full test coverage

## Requirements

- Java 21
- Maven 3.8+
- Spring Boot 3.1+

## Getting Started

### Build and Run

```bash
# Clone the repository
git clone https://github.com/raskoldi/brokerage-api.git
cd brokerage-api

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080` by default.

### H2 Console Access

The H2 database console is available at `http://localhost:8080/h2-console` with the following credentials:
- JDBC URL: `jdbc:h2:mem:brokeragedb`
- Username: `sa`
- Password: (leave empty)

## API Endpoints

### Authentication

- `POST /api/auth/login` - Login and retrieve JWT token

### Orders

- `POST /api/orders` - Create a new order
- `GET /api/orders?customerId={id}&startDate={date}&endDate={date}` - List orders by customer ID and date range
- `GET /api/orders/filter` - Filter orders by various criteria
- `DELETE /api/orders/{orderId}` - Cancel a pending order

### Assets

- `GET /api/assets?customerId={id}` - List assets by customer ID
- `GET /api/assets/filter` - Filter assets by various criteria
- `GET /api/assets/{customerId}/{assetName}` - Get specific asset by customer ID and asset name

### Admin

- `POST /api/admin/orders/match` - Match a pending order (admin only)

## Authentication

The API uses JWT tokens for authentication. To access protected endpoints, include the token in the Authorization header:

```
Authorization: Bearer <your_jwt_token>
```

### Default Users

- Admin: `admin` / `admin123`
- Customer: `customer1` / `password123`,`customer2` / `password123`

## Database Schema

The application uses the following database schema:

- **Asset**: customerId, assetName, size, usableSize
- **Order**: customerId, assetName, orderSide, size, price, status, createDate
- **User**: id, username, password, roles
- **Customer**: id, customerName, userId

## Business Rules

1. When creating a BUY order:
   - Check if the customer has enough TRY (usableSize)
   - Reduce the TRY usableSize

2. When creating a SELL order:
   - Check if the customer has enough of the asset (usableSize)
   - Reduce the asset usableSize

3. When canceling an order:
   - Only PENDING orders can be canceled
   - Restore the usableSize of the relevant assets

4. When matching an order:
   - Update the order status to MATCHED
   - Update the asset sizes and usableSize values accordingly

## Testing

To run the tests:

```bash
mvn test
```

## Security

- All endpoints are secured with JWT authentication
- Role-based access control limits customer access to their own data
- Admin users have access to all data and operations

## Additional Notes

- Orders are always against TRY asset (buying or selling with TRY)
- TRY is treated as an asset in the asset table
- When creating or canceling orders, the system checks for sufficient funds and updates asset usableSize accordingly
