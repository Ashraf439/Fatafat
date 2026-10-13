# Fatafat

Backend for **Fatafat**, a multi-role food-delivery platform. It serves three web apps (customer, restaurant, admin) from one Spring Boot API: customers browse restaurants and place orders, restaurants onboard (application, admin approval, onboarding fee) and manage menus, staff and timings, and admins review applications.

The frontends live in [fatafat-ui](https://github.com/Ashraf439/fatafat-ui).

## Tech stack

| Area | Choice |
| --- | --- |
| Language / framework | Java 25, Spring Boot 4.1 |
| Persistence | Spring Data JPA, MySQL |
| Cache / rate limiting | Redis |
| Security | Spring Security 7, JWT (JJWT 0.12) in httpOnly cookies |
| Payments | Razorpay (restaurant onboarding fee) |
| Media | Cloudinary (restaurant and menu images) |
| Email | Spring Mail over SMTP |
| Other | Bean Validation, AspectJ (rate-limit aspect), OpenCSV (menu import), Lombok, Actuator |

## Features

- **Auth:** registration for customers, restaurants and riders; email verification; login, refresh and logout; forgot and set password; `/me`.
- **Sessions:** short-lived access token and a rotating 30-day refresh token, both in httpOnly, `SameSite=Strict` cookies. Refresh tokens are stored hashed. Reusing a revoked refresh token revokes all of that user's sessions.
- **RBAC:** roles (`CUSTOMER`, `RESTAURANT`, `RESTAURANT_MANAGER`, `RESTAURANT_STAFF`, `RESTAURANT_CASHIER`, `RIDER`, `ADMIN`, `SUPER_ADMIN`) and per-role and per-user permissions, seeded on startup.
- **Customer:** browse restaurants and menus, saved addresses, price quote, place, track and cancel orders.
- **Restaurant:** onboarding application, Razorpay payment, CSV menu upload, images, opening hours, open/closed status, staff accounts with permissions.
- **Admin:** list, approve and reject restaurant applications.
- **Rate limiting:** Redis-backed `@RateLimit` annotation (used on login, resend-verification, forgot-password and set-password).
- **Demo data:** optional seeder that creates sample restaurants with menus and images.

Orders are priced **entirely on the server** from database prices. Client-supplied amounts are ignored.

## Project layout

```
src/main/java/com/ashraf/
  auth/         login, registration, refresh tokens, email, password flows
  core/         User, Role, Permission entities and repositories
  customer/     customer profile, addresses, orders, pricing
  restaurant/   core (menu, orders, timings), onboarding, staff
  payment/      Razorpay order creation and signature verification
  admin/        admin endpoints
  rider/        rider entities (no API yet)
  commerce/     ratings, complaints, bank details (entities only)
  config/       security, JWT filter, CORS, seeders, Razorpay
  shared/       exceptions, rate limiting, storage, utilities
  seed/         demo data generator
```

## Getting started

### Prerequisites

- JDK 25
- MySQL 8 with an empty database named `fatafat`
- Redis (the included `docker-compose.yml` starts one)
- Accounts for Razorpay (test mode), Cloudinary and an SMTP provider

### Environment variables

`application.properties` reads these. The app will not start without them.

| Variable | Purpose |
| --- | --- |
| `MYSQL_USER`, `MYSQL_PASSWORD` | Database credentials |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Super admin created on first start |
| `MAIL_USERNAME`, `MAIL_APP_PASSWORD` | SMTP login (Gmail app password by default) |
| `RAZOR_API`, `RAZOR_SECRET` | Razorpay key id and secret |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Image storage |

The project uses `dotenv-java`, so you can put these in a `.env` file in the project root (it must stay out of git).

> **Before deploying:** `jwt.secret` in `application.properties` is a placeholder. Replace it with a long random value supplied through the environment, and turn off `spring.jpa.show-sql`, the security `DEBUG` logging and `spring.profiles.active=dev`.

### Run locally

```bash
# 1. Start Redis (and create the MySQL database yourself)
docker compose up redis -d

# 2. Run the API on http://localhost:8080
./mvnw spring-boot:run
```

The database URL is currently `jdbc:mysql://localhost:3306/fatafat`. Schema is managed by Hibernate (`ddl-auto=update`).

On the first start the app seeds roles, permissions and the super admin from `ADMIN_EMAIL` and `ADMIN_PASSWORD`.

### Docker

```bash
docker build -t fatafat .
```

The image builds with Maven on JDK 25 and runs on a JRE 25 Alpine image. The datasource URL is hardcoded to `localhost`, so a containerised app needs it pointed at a reachable MySQL host before it can connect.

### Demo data

Set `seed.demo.enabled=true` to generate sample restaurants (`seed.demo.restaurants`, default 100) with menus from `src/main/resources/seed/menu-catalog.csv`. Owner accounts use the email domain in `seed.demo.email-domain`. Set `seed.demo.images=false` to skip image uploads.

### Local email links

With `app.dev.log-links=true` every emailed link (verification, password reset) is also printed to the log, so you can test flows without opening an inbox.

## Configuration reference

| Property | Default | Notes |
| --- | --- | --- |
| `cors.allowed-origins` | `http://localhost:5173,http://localhost:5174` | Restaurant and customer apps |
| `frontend.restaurant-url` / `frontend.customer-url` | ports 5173 / 5174 | Redirect targets after email verification |
| `cookie.secure` | `true` | Set `false` only for plain-http local development |
| `jwt.expiration-ms` | `86400000` | Access token lifetime |
| `onboarding.fee.amount` | `1000` | Restaurant onboarding fee (INR) |
| `order.pricing.delivery-fee` | `40` | Waived above `free-delivery-above` (`499`) |
| `order.pricing.tax-rate` | `0.05` | |
| `order.pricing.allowed-payment-methods` | `CASH` | Online payment for orders is not enabled yet |
| `spring.servlet.multipart.max-file-size` | `5MB` | Image uploads |

## API overview

All routes are under `/api`. Authentication is by cookie, so clients must send credentials.

**Auth** (`/api/auth`, public unless noted)

`POST register/customer`, `register/restaurant`, `register/rider`, `login`, `refresh`, `logout`, `resend-verification`, `forgot-password`, `set-password` · `GET verify?token=`, `me` (authenticated)

**Customer** (`CUSTOMER`; restaurant browsing is public)

- `GET /customer/restaurants`, `/restaurants/cities`, `/restaurants/{id}`, `/restaurants/{id}/menu`
- `GET|POST /customer/addresses`, `PUT /{id}`, `PATCH /{id}/default`, `DELETE /{id}`
- `POST /customer/orders/quote`, `POST /customer/orders`, `GET /customer/orders`, `GET /customer/orders/{id}`, `PATCH /customer/orders/{id}/cancel`

**Restaurant** (`RESTAURANT` and staff roles)

- `POST /restaurant/onboarding/applications`, `GET /restaurant/onboarding/applications/me`, `POST /restaurant/onboarding/applications/{id}/payment-order`
- `POST /payments/verify`
- `POST /restaurant/menu-upload`, `GET /restaurant/menu`, `POST /restaurant/image`, `POST /restaurant/menu/{menuId}/image`
- `GET|PATCH /restaurant/status`, `GET|PUT|POST /restaurant/timings`, `PATCH|DELETE /restaurant/timings/{id}`
- `POST|GET /restaurant/staff`, `DELETE /restaurant/staff/{userId}`, `PATCH /restaurant/staff/{userId}/permissions`

**Admin** (`ADMIN`, `SUPER_ADMIN`)

- `GET /admin/restaurant-applications`, `PATCH /admin/restaurant-applications/{id}/approve`, `PATCH .../reject`

## Domain flows

**Restaurant onboarding:** `UNDER_REVIEW` → (admin approves) `APPROVED_PENDING_PAYMENT` → (Razorpay payment verified) `LIVE`. Admin rejection moves it to `REJECTED`, after which the owner can reapply.

**Order status:** `PLACED` → `ACCEPTED` → `PREPARING` → `OUT_FOR_DELIVERY` → `DELIVERED`, or `CANCELLED` (customers can cancel only while `PLACED`).

**Account status:** `PENDING_VERIFICATION` → `ACTIVE` (after email verification), or `SUSPENDED`.

## Status and roadmap

Implemented: auth, customer ordering (cash on delivery), restaurant onboarding and management, admin approval.

Not yet implemented: rider delivery flow, ratings and complaints APIs, online payment for orders, Razorpay webhook handling (the route is permitted in the security config but has no handler), and automated tests beyond the context-load test.

## License

No license has been chosen yet. 