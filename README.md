# Servlet API Gateway

A lightweight, high-performance API Gateway built using Jakarta Servlets and Apache Tomcat 10.

## Core Features

- **Request Routing**: Dynamic proxying of HTTP requests (GET, POST, PUT, DELETE) to backend microservices.
- **Authentication & Authorization**: Supports both static API Key validation (`X-API-Key`) and JWT Bearer token validation.
- **Rate Limiting**: In-memory rate limiting (default: 100 requests per minute per IP) using a fixed-window algorithm.
- **Load Balancing**: Built-in Round Robin strategy for distributing traffic across multiple backend instances.
- **Circuit Breaker**: Failure tracking per target URL; automatically trips after a threshold (5 failures) to prevent cascade failure.
- **Caching**: TTL-based in-memory caching for successful GET responses (1-minute default).
- **Request Transformation**: Automatic injection of audit headers (e.g., `X-Proxied-By`).
- **Logging & Monitoring**: Detailed logging of request/response cycles including execution time.
- **Dynamic Configuration**: Supports reloading of routing rules without server restart.
- **Security**: Basic protection against SQL Injection and XSS patterns in query parameters.

## Tech Stack

- **Runtime**: Java 17 / Jakarta EE 10
- **Container**: Apache Tomcat 10.1+
- **Build System**: Apache Maven 3.8+
- **Libraries**: 
  - Jackson (JSON processing)
  - JJWT (JSON Web Token validation)

## Installation & Deployment

### Prerequisites
- JDK 17 or higher
- Maven 3.8+
- Apache Tomcat 10+

### Build
```bash
mvn clean package
```

### Deploy
Copy the generated `target/api-gateway.war` to the `webapps` directory of your Tomcat installation.

## Configuration

The gateway routes are configured via `src/main/resources/routes.json`. 

Example structure:
```json
{
  "routes": [
    {
      "path": "/users",
      "targets": ["http://api1.backend/users", "http://api2.backend/users"]
    }
  ]
}
```

## Admin Endpoints

- **GET `/admin/metrics`**: Returns gateway statistics (total hits, failed requests, route-specific distribution).
- **POST `/admin/reload`**: Triggers a reload of the `routes.json` configuration file into memory.

## Security Credentials (Default)

- **API Key**: `my-secret-key-123`
- **JWT Secret**: `your-very-secure-secret-key-that-is-long-enough-for-hs256`
