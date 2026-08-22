# 🛒 Full-Stack E-Commerce Microservices Platform

> A containerized e-commerce platform built with **Java, Spring Boot, Spring Cloud, Angular, Kafka, MySQL, AWS S3 and Docker**.

![Build](https://img.shields.io/github/actions/workflow/status/mantoshkumar9060/Ecommerce_Project/ci.yml?branch=main&label=CI%2FCD)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-Microservices-brightgreen)
![Angular](https://img.shields.io/badge/Frontend-Angular-red)
![Docker](https://img.shields.io/badge/Containerized-Docker-blue)

## 📸 Project Showcase

### StyleHub application

<p align="center">
  <img src="https://raw.githubusercontent.com/mantoshkumar9060/Ecommerce_Project/main/docs/screenshots/project-showcase.jpg" alt="StyleHub e-commerce application showcase" width="900" />
</p>

The running application demonstrates the customer journey from **product discovery → product details → checkout → order history**, backed by independently deployable microservices.

### Checkout flow

<p align="center">
  <img src="https://raw.githubusercontent.com/mantoshkumar9060/Ecommerce_Project/main/docs/screenshots/checkout.svg" alt="StyleHub checkout flow" width="700" />
</p>

The checkout screen is captured from the running Angular storefront and shows the sandbox payment options used by the order/payment flow.

---

## 🚀 What I Built

This project demonstrates how a real-world e-commerce system can be split into independently deployable services. The frontend communicates through a single **API Gateway**, services register with **Eureka**, product images are handled through **AWS S3**, and asynchronous business events are processed with **Kafka**.

### Key capabilities

- JWT-based authentication and protected APIs
- API Gateway routing and centralized entry point
- Eureka service discovery
- Product catalog, categories, brands, sizes and images
- AWS S3-based product image handling
- User cart and product-price snapshot handling
- Order creation and order-state tracking
- Payment callback flow
- Kafka-based asynchronous payment/notification events
- Notification service for event consumption and email/console delivery
- Docker Compose orchestration
- GitHub Actions CI builds for backend services and Angular storefront

---

## 🏗️ Architecture

![E-Commerce Microservices Architecture](https://raw.githubusercontent.com/mantoshkumar9060/Ecommerce_Project/main/docs/architecture/ecommerce-architecture.svg)

### Request and event flow

```text
Angular Storefront
        |
        v
   API Gateway
        |
        +--> Auth Service ---------> MySQL
        +--> Product Service ------> MySQL / AWS S3
        +--> Cart Service ---------> MySQL
        +--> Order Service --------> MySQL
        +--> Payment Service ------> Payment callback
        |
        v
     Eureka Server
   (service discovery)

Payment completed event
        |
        v
      Kafka Topic
      /         \
     v           v
Order Service   Notification Service
```

**Communication model:**
- REST for synchronous client/service operations
- Eureka for service discovery
- Kafka for asynchronous event-driven processing

---

## 🧩 Services

| Service | Port | Responsibility |
|---|---:|---|
| Eureka | 8761 | Service discovery |
| API Gateway | 8080 | Single entry point, routing and security |
| Auth Service | 9091 | Registration, login, BCrypt passwords and JWT issuance |
| Product Service | 9093 | Catalog, brands, categories, sizes and AWS S3 image handling |
| Cart Service | 9094 | Per-user cart and product-price snapshot |
| Order Service | 9095 | Order snapshots and payment state |
| Payment Service | 9096 | Payment intent and callback entry point |
| Notification Service | 9097 | Kafka consumer and notification delivery |
| Storefront | 4200 | Angular customer UI |

---

## 🛠️ Tech Stack

### Backend
- Java 17
- Spring Boot
- Spring Cloud
- Spring Security
- Spring Data JPA / Hibernate
- REST APIs
- JWT
- OpenFeign

### Frontend
- Angular
- TypeScript
- HTML / SCSS

### Data & Messaging
- MySQL
- Apache Kafka

### Cloud & DevOps
- AWS S3
- Docker / Docker Compose
- GitHub Actions
- Maven
- Git

---

## 🔐 Security

The platform uses JWT-based authentication. After login, the client receives an access token and sends it with protected requests:

```http
Authorization: Bearer <accessToken>
```

Passwords are stored using BCrypt hashing. Secrets and environment-specific credentials should remain outside Git through environment variables and `.env` files.

---

## ☁️ AWS S3 Integration

The Product Service configures:

- `S3Client` for S3 operations
- `S3Presigner` for pre-signed URL generation
- Configurable AWS region through application properties

This keeps image storage separate from application containers and supports scalable product media handling.

---

## 🐳 Run Locally

### 1. Clone the repository

```bash
git clone https://github.com/mantoshkumar9060/Ecommerce_Project.git
cd Ecommerce_Project
```

### 2. Configure environment values

Copy the example environment file and provide local values:

```powershell
Copy-Item .env.example .env
```

Do **not** commit real passwords, JWT secrets, AWS credentials or SMTP credentials.

### 3. Start the stack

```powershell
docker compose up --build -d
```

<<<<<<< HEAD
Check running containers:
=======
The API Gateway is available at `http://localhost:8080`, Eureka at
`http://localhost:8761`, MySQL at `localhost:3306`, and Kafka at `localhost:9092`.

## AI product assistant (RAG)

The independent `ai-service` registers with Eureka on its internal port `9098`. It uses the
existing `product-service` through Eureka/Feign, creates embeddings with an OpenAI-compatible
provider, and stores vectors in Qdrant. No existing service calls the AI service.

```text
Angular assistant -> API Gateway (JWT) -> ai-service
                                         |-> product-service (catalogue)
                                         |-> embedding/chat provider
                                         `-> Qdrant (ecommerce_products)
```

Qdrant is exposed for local inspection at `http://localhost:6333` (REST) and `6334` (gRPC).
The collection is created on first ingestion with cosine distance and the configured embedding
dimension. Product/brand vector IDs are deterministic, so running ingestion again updates the
same Qdrant points instead of duplicating them.

Set a real provider key in your uncommitted `.env` before ingestion. The defaults target the
OpenAI-compatible `/v1/embeddings` and `/v1/chat/completions` contracts; another compatible
provider can be selected with `AI_BASE_URL` and model variables. Never commit `AI_API_KEY`.

```powershell
Copy-Item .env.example .env
# Edit .env and set AI_API_KEY=your_real_key
docker compose up -d --build ai-service qdrant api-gateway storefront
```

After all services register in Eureka, send one protected ingestion request using a JWT from
login. Ingestion reads the real paginated product catalogue and indexes active product-brand
variants, including product name, brand, category, description and price.

```powershell
$token = '<JWT from /api/v1/auth/login>'
$headers = @{ Authorization = "Bearer $token"; 'Content-Type' = 'application/json' }
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/ai/ingest/products -Headers $headers
Invoke-RestMethod -Uri http://localhost:8080/api/v1/ai/ingest/status -Headers @{ Authorization = "Bearer $token" }
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/ai/search -Headers $headers -Body '{"query":"running shoes under 2000","limit":5}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/ai/chat -Headers $headers -Body '{"question":"Recommend running shoes under 2000","limit":5}'
```

Only `GET /api/v1/ai/health` is public. Every other `/api/v1/ai/**` endpoint remains JWT
protected at the API Gateway. Without `AI_API_KEY`, the service still starts and health remains
available, while embedding/chat/ingestion requests return a clear `503` configuration error.

The storefront assistant is available from the **Assistant** header action after sign-in. It
calls `/api/v1/ai/chat`, renders grounded answer text plus product cards, and the cards open the
catalogue product or add its real product/brand variant to the cart.

3. In each IntelliJ Run Configuration set `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and optionally `KAFKA_BOOTSTRAP_SERVERS`.
4. Start: Eureka, Auth, Product, Cart, Payment, Order, Notification, then API Gateway.

For Maven commands, use the included settings file so dependencies are cached inside the
project rather than an inaccessible or shared global Maven repository:
>>>>>>> 7f3f803 (feat(ai): implement RAG-based product assistant)

```powershell
docker compose ps
```

### 4. Access the application

| Component | URL |
|---|---|
| Storefront | http://localhost:4200 |
| API Gateway | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| MySQL | localhost:3306 |
| Kafka | localhost:9092 |

---

## 📡 API Examples

### Register

```http
POST http://localhost:8080/api/v1/auth/register
Content-Type: application/json

{"name":"Demo User","email":"demo@example.com","password":"StrongPass123"}
```

### Login

```http
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json
```

Login returns an access token for protected requests.

### Add cart item

```http
POST http://localhost:8080/api/v1/carts/items
Authorization: Bearer <accessToken>

{"productId":1,"brandId":1,"quantity":2}
```

### Create order

```http
POST http://localhost:8080/api/v1/orders
Authorization: Bearer <accessToken>

{"shippingAddress":"Delhi, India - 110001"}
```

---

## 🧪 CI/CD

GitHub Actions builds the Angular storefront and backend services independently. The project currently uses automated build/test jobs to catch integration and compilation issues before further deployment work.

---

## 🔮 Next Improvements

- Inventory reservation and compensating actions for failed payments
- Provider-grade payment webhook signature verification
- Distributed tracing and centralized observability
- Redis caching where appropriate
- Integration tests with containers
- Production secret management
- Deployment to a cloud environment

---

## 👨‍💻 Author

**Mantosh Kumar**

GitHub: https://github.com/mantoshkumar9060

If this project is useful, consider starring the repository.
