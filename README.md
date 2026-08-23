# 🛒 StyleHub — Full-Stack E-Commerce Microservices Platform

<p align="center">
  <strong>A production-style e-commerce platform built with Spring Boot microservices, Angular, Kafka, JWT, Qdrant RAG, Docker and AWS S3.</strong>
</p>

<p align="center">
  <a href="http://localhost:4200">🛍️ Run the Storefront</a> •
  <a href="http://localhost:8080">🚪 API Gateway</a> •
  <a href="http://localhost:8761">🔎 Eureka</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?logo=openjdk" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?logo=springboot" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Angular-Frontend-red?logo=angular" alt="Angular" />
  <img src="https://img.shields.io/badge/Kafka-Event%20Driven-black?logo=apachekafka" alt="Kafka" />
  <img src="https://img.shields.io/badge/Docker-Containerized-blue?logo=docker" alt="Docker" />
  <img src="https://img.shields.io/badge/RAG-Qdrant-purple" alt="RAG" />
  <img src="https://img.shields.io/badge/Tests-48%20passing-success" alt="48 passing tests" />
</p>

## ✨ Why this project is interesting

StyleHub is not a single Spring Boot application. It is a **full-stack, containerized, event-driven e-commerce system** designed to demonstrate how independently deployable services work together behind a single API Gateway.

The platform includes:

- 🔐 JWT authentication with role-based authorization
- 🚪 API Gateway with centralized routing and security
- 🧭 Eureka service discovery
- 🛍️ Product catalogue with categories, brands, sizes and images
- ☁️ AWS S3 product media integration
- 🛒 Per-user cart with product-price snapshots
- 📦 Order lifecycle and payment state management
- 💳 Payment workflow with idempotency-oriented handling
- 📨 Kafka-based asynchronous payment/notification events
- 🤖 AI shopping assistant using RAG + embeddings + Qdrant
- 🎨 Angular storefront with product discovery, cart and checkout flows
- 🐳 Docker Compose local orchestration
- 🧪 JUnit 5 + Mockito test coverage across the core services
- 🔄 GitHub-based development workflow

---

## 📸 Product Showcase

### StyleHub storefront

<p align="center">
  <img src="https://raw.githubusercontent.com/mantoshkumar9060/Ecommerce_Project/main/docs/screenshots/project-showcase.jpg" alt="StyleHub e-commerce application showcase" width="920" />
</p>

### Checkout flow

<p align="center">
  <img src="https://raw.githubusercontent.com/mantoshkumar9060/Ecommerce_Project/main/docs/screenshots/checkout.svg" alt="StyleHub checkout flow" width="760" />
</p>

---

## 🧠 AI Shopping Assistant (RAG)

The **StyleHub AI Assistant** lets customers ask natural-language shopping questions such as:

> “Puma shoes under ₹3000”

> “Show Nike shoes under ₹5000”

The AI flow retrieves catalogue context from the real product service, performs semantic search against Qdrant vectors, applies structured filters such as brand/category/budget, and generates a concise grounded response.

```text
Customer
   ↓
Angular Assistant
   ↓
API Gateway + JWT
   ↓
ai-service
   ├── Product Service → live catalogue
   ├── Embedding / Chat model
   └── Qdrant → vector retrieval
```

Security model:

| Endpoint | Access |
|---|---|
| `GET /api/v1/ai/health` | Public |
| `POST /api/v1/ai/chat` | Authenticated |
| `POST /api/v1/ai/search` | Authenticated |
| `POST /api/v1/ai/ingest/products` | Admin only |
| `GET /api/v1/ai/ingest/status` | Admin only |

---

## 🏗️ Architecture

<p align="center">
  <img src="https://raw.githubusercontent.com/mantoshkumar9060/Ecommerce_Project/main/docs/architecture/ecommerce-architecture.svg" alt="StyleHub microservices architecture" width="1100" />
</p>

### Request flow

```text
Angular Storefront
        │
        ▼
   API Gateway
        │
        ├── Auth Service ───────► MySQL
        ├── Product Service ────► MySQL / AWS S3
        ├── Cart Service ───────► MySQL
        ├── Order Service ──────► MySQL
        └── Payment Service ────► MySQL

               Eureka
            Service Discovery

Payment completed
       │
       ▼
     Kafka
      ├──────────────► Order Service
      └──────────────► Notification Service
```

**Communication:** REST for synchronous operations, Eureka for discovery, Kafka for asynchronous events.

---

## 🧩 Services

| Service | Port | Responsibility |
|---|---:|---|
| Eureka | 8761 | Service discovery |
| API Gateway | 8080 | Routing, JWT validation, authorization |
| Auth Service | 9091 | Registration, login, BCrypt, JWT issuance, addresses |
| Product Service | 9093 | Catalogue, categories, brands, sizes, product images |
| Cart Service | 9094 | User cart and price snapshots |
| Order Service | 9095 | Order creation and state transitions |
| Payment Service | 9096 | Payment state and callback flow |
| Notification Service | 9097 | Kafka event consumption and notification delivery |
| AI Service | 9098 | RAG search and AI shopping assistant |
| Storefront | 4200 | Angular customer application |

---

## 🛠️ Tech Stack

### Backend

- Java 17
- Spring Boot 4
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

- MySQL 8
- Apache Kafka
- Qdrant

### Cloud & DevOps

- AWS S3
- Docker / Docker Compose
- GitHub
- GitHub Actions
- Maven

### AI

- Retrieval-Augmented Generation (RAG)
- Vector embeddings
- Qdrant semantic search
- OpenAI-compatible / Ollama model interface

---

## 🧪 Testing

The repository includes focused unit/controller tests using **JUnit 5 + Mockito**.

Current test coverage includes the AI service plus the core business microservices:

```text
AI Service               11 tests
Auth Service              9 tests
Cart Service              4 tests
Order Service             5 tests
Payment Service           5 tests
Notification Service     3 tests
API Gateway               2 tests
Product Service           9 tests
─────────────────────────────────
Total                    48 tests
```

The tests focus on meaningful business behavior such as authentication failures, cart rules, order ownership/state transitions, payment idempotency, notification handling, JWT validation and product/category logic.

Run service tests with Maven:

```powershell
cd <service-directory>
mvn test
```

For the Angular storefront:

```powershell
cd storefront
npm.cmd run build
```

---

## 🔐 Security

JWT access tokens are issued by the Auth Service and validated by the API Gateway.

```http
Authorization: Bearer <accessToken>
```

Passwords are stored using BCrypt. Secrets, credentials and environment-specific values should stay outside Git using `.env` / environment variables.

Current gateway rules include:

- Public login/register
- Public product read APIs
- Authenticated customer operations
- Admin-only product management
- Admin-only AI ingestion
- Public AI health endpoint
- Stateless JWT authentication
- Explicit `401` vs `403` handling

---

## ☁️ AWS S3

Product media is stored outside the application database through AWS S3 integration.

The Product Service uses:

- `S3Client` for object operations
- `S3Presigner` for signed access
- Configurable AWS region and credentials

---

## 🐳 Run Locally

### 1. Clone

```bash
git clone https://github.com/mantoshkumar9060/Ecommerce_Project.git
cd Ecommerce_Project
```

### 2. Configure environment

```powershell
Copy-Item .env.example .env
```

Do **not** commit real database passwords, JWT secrets, AWS keys, SMTP credentials or AI provider keys.

### 3. Start the stack

```powershell
docker compose up --build -d
```

Check containers:

```powershell
docker compose ps
```

### 4. Open the application

| Component | URL |
|---|---|
| 🛍️ Storefront | http://localhost:4200 |
| 🚪 API Gateway | http://localhost:8080 |
| 🔎 Eureka | http://localhost:8761 |
| 🧠 Qdrant | http://localhost:6333 |
| 📨 Kafka | localhost:9092 |
| 🗄️ MySQL | localhost:3306 |

---

## 📡 Example API Calls

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

### Product catalogue

```http
GET http://localhost:8080/api/v1/products?page=0&size=20
```

### AI search

```http
POST http://localhost:8080/api/v1/ai/search
Authorization: Bearer <accessToken>
Content-Type: application/json

{"query":"Puma shoes under 3000","limit":5}
```

### AI chat

```http
POST http://localhost:8080/api/v1/ai/chat
Authorization: Bearer <accessToken>
Content-Type: application/json

{"question":"Recommend Puma shoes under 3000","limit":5}
```

---

## 🔄 CI/CD

The repository is structured for GitHub-based development and automated build/test workflows. The project uses Maven for backend services and Angular CLI for the storefront.

---

## 🗺️ Roadmap

### Completed

- [x] Microservices architecture
- [x] API Gateway + Eureka
- [x] JWT security
- [x] Kafka event flow
- [x] AWS S3 integration
- [x] Angular storefront
- [x] RAG-based AI shopping assistant
- [x] JUnit 5 + Mockito test coverage
- [x] Gateway security hardening

### Next

- [ ] Redis caching for high-read catalogue endpoints
- [ ] Cache invalidation tests
- [ ] Kafka retry + dead-letter handling
- [ ] Prometheus + Grafana observability
- [ ] Integration tests with containers
- [ ] Production secret management
- [ ] Cloud deployment

---

## 👨‍💻 Author

**Mantosh Kumar**

GitHub: https://github.com/mantoshkumar9060

⭐ If you find the project useful, consider starring the repository.
