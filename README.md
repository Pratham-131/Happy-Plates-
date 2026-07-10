![CI](https://github.com/Pratham-131/Happy-Plates-/actions/workflows/ci.yml/badge.svg)

# Happy Plates — Full-Stack Food Ordering Platform

A full-stack food ordering application — Spring Boot backend, React (Vite) frontend, MongoDB for data — covering the complete flow from browsing a menu to placing an order.

## What This Project Does

A user registers/logs in, browses the restaurant menu, adds items to a cart, and places an order through checkout. The backend exposes REST APIs for authentication, menu/food items, cart management, orders, and user accounts; the frontend is a React SPA that consumes them. Payment (Razorpay) and file storage (AWS S3) are wired in as feature-flagged integrations, so the full order pipeline runs locally without needing live third-party accounts.

## Tech Stack

**Backend**
- Java, Spring Boot
- MongoDB (`foodiesdb`)
- JWT-based authentication
- Maven

**Frontend**
- React + Vite

**Stubbed integrations** (toggle via config flags)
- Razorpay — payment processing
- AWS S3 — file/image storage

## Architecture

```
┌──────────────┐         ┌───────────────────┐        ┌────────────────┐
│   React SPA  │ ──────► │  Spring Boot API   │ ─────► │    MongoDB     │
│  (Vite, :5173)│  REST  │     (:8081)        │        │  (foodiesdb)   │
└──────────────┘         └─────────┬──────────┘        └────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    │                               │
            ┌───────▼────────┐            ┌─────────▼────────┐
            │  Razorpay       │            │     AWS S3        │
            │  (stubbed)      │            │    (stubbed)      │
            └─────────────────┘            └────────────────────┘
```

Endpoints cover: Auth (register/login, JWT issuance), Food (menu items), Cart, Orders, and User accounts.

## How to Run

### Prerequisites
- JDK 17+
- Node.js + npm
- MongoDB running locally on `localhost:27017`

### Backend
```bash
cd foodiesapi
./mvnw spring-boot:run
```
Runs on `http://localhost:8081`. Connects to `mongodb://localhost:27017/foodiesdb`.

### Frontend
```bash
cd foodies
npm install
npm run dev
```
Runs on the Vite dev server (default `http://localhost:5173`), configured to call the backend on port 8081.

### Feature flags
Payment and storage stubs are controlled in `application.properties`:
```
app.stub.payment=true
app.stub.storage=true
```
Set to `false` once real Razorpay/AWS credentials are configured, to use live integrations instead of stubs.

## Current Status

- Full order flow (browse → cart → checkout → order) verified end-to-end locally.
- Payment and file storage run as local stubs — not yet connected to live Razorpay/AWS accounts.
- No automated tests yet.
- Runs locally only — not yet deployed.

## Planned / In Progress

- [ ] Move JWT secret and other credentials out of `application.properties` into environment variables (currently committed in plaintext — known issue, fix in progress).
- [ ] Redis caching for menu data and JWT session/token blacklist.
- [ ] Live deployment (backend + frontend + MongoDB Atlas).
- [ ] Real Razorpay webhook handling for order status updates.
- [ ] At least one live integration (S3 or MinIO) replacing a stub.
- [ ] Basic controller/service-layer tests, especially around the order flow.

## License

For learning and portfolio purposes.