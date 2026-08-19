# Ecommerce Microservices

Spring Boot microservices backend designed for an Angular client. The client calls only the API Gateway; authentication and user identity are handled with JWT.

## Services

| Service | Port | Responsibility |
|---|---:|---|
| Eureka | 8761 | Service discovery |
| API Gateway | 8080 | Routing and JWT verification |
| Auth | 9092 | Registration, login, BCrypt passwords, JWT issuance |
| Product | 9093 | Catalog and private S3 image handling |
| Cart | 9094 | Per-user cart and product-price snapshot |
| Order | 9095 | Order snapshots and payment state |
| Payment | 9096 | Payment intent and callback entry point |
| Notification | 9097 | Kafka consumer; console or SMTP email notification |

## Architecture

```text
Angular -> API Gateway -> Auth / Product / Cart / Order / Payment
                              |
Order -> Cart (Feign)          |
Payment callback -> Kafka payment.completed -> Order + Notification
```

The payment event flow is a choreography Saga starting point: a payment failure makes the order `PAYMENT_FAILED`; success makes it `PAID`. Inventory reservation/release should be the next compensating participant when stock management is added.

## Local setup

1. Copy `.env.example` to your local environment (do not commit secrets).
2. Start infrastructure:

```powershell
docker compose up -d
```

To run the complete stack in containers (including the services), build it first:

```powershell
docker compose up --build
```

The API Gateway is available at `http://localhost:8080`, Eureka at
`http://localhost:8761`, MySQL at `localhost:3306`, and Kafka at `localhost:9092`.

3. In each IntelliJ Run Configuration set `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and optionally `KAFKA_BOOTSTRAP_SERVERS`.
4. Start: Eureka, Auth, Product, Cart, Payment, Order, Notification, then API Gateway.

For Maven commands, use the included settings file so dependencies are cached inside the
project rather than an inaccessible or shared global Maven repository:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd auth-service
mvn -s ..\maven-settings.xml -DskipTests compile
```

Use the same command from any service directory. `JAVA_HOME` must point to an installed
JDK, not a removed JDK path.

## Gateway API examples

Register:

```http
POST http://localhost:8080/api/v1/auth/register
Content-Type: application/json

{"name":"Demo User","email":"demo@example.com","password":"StrongPass123"}
```

Login returns `accessToken`. Send it for protected calls:

```http
Authorization: Bearer <accessToken>
```

Add cart item:

```http
POST http://localhost:8080/api/v1/carts/items

{"productId":1,"brandId":1,"quantity":2}
```

Create order:

```http
POST http://localhost:8080/api/v1/orders

{"shippingAddress":"Delhi, India - 110001"}
```

## Provider integrations

`/api/v1/payments/callback` is a development callback contract, not a real gateway integration. A real Razorpay/Stripe integration must verify the provider webhook signature before submitting a callback. SMTP delivery requires `NOTIFICATION_EMAIL_ENABLED=true` plus real SMTP credentials. SMS and WhatsApp also require approved provider/business accounts; keep those credentials in a secret manager, not Git.
