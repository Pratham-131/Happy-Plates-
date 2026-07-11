![CI](https://github.com/Pratham-131/Happy-Plates-/actions/workflows/ci.yml/badge.svg)

# Happy Plates — Full-Stack Food Ordering Platform

A full-stack food ordering application — Spring Boot backend, React (Vite) frontend, MongoDB for data — covering the complete flow from browsing a menu to placing an order.

**Live:** [happy-plates.vercel.app](https://happy-plates.vercel.app) · Backend API: [happy-plates-backend.onrender.com](https://happy-plates-backend.onrender.com)

> Backend runs on Render's free tier and may take 30–60s to wake up on first request.

## What This Project Does

A user registers/logs in, browses the restaurant menu, adds items to a cart, and places an order through checkout. A separate admin role can manage food items (add/edit/delete) and view/update order status. The backend exposes REST APIs for authentication, menu/food items, cart management, orders, and user accounts; the frontend is a React SPA that consumes them.

## Tech Stack

**Backend**
- Java, Spring Boot, Spring Security (JWT-based auth, role-based access for USER/ADMIN)
- MongoDB Atlas (`foodiesdb`)
- Redis (caching)
- Maven
- JUnit — service-layer tests

**Frontend**
- React + Vite, deployed on Vercel

**Infra / DevOps**
- Docker + Docker Compose
- GitHub Actions CI (build + test on every push)
- Deployed: backend on Render (Docker), frontend on Vercel

**Stubbed integrations** (toggle via config flags)
- Razorpay — payment processing
- AWS S3 — file/image storage (stubbed as local disk storage on the backend; menu images are committed as static assets, admin-uploaded images are saved locally and don't persist across redeploys)

## Architecture

Frontend (React SPA on Vercel) talks to the Spring Boot API (on Render) over REST. The API connects to MongoDB Atlas for data, Redis for caching, and integrates with Razorpay (stubbed) for payments.

```
React SPA (Vercel)  --REST-->  Spring Boot API (Render)  -->  MongoDB Atlas (foodiesdb)
                                        |
                                        +--> Razorpay (stubbed)
                                        +--> Redis (caching)
```

Endpoints cover: Auth (register/login/admin-login, JWT issuance), Food (menu items, CRUD for admin), Cart, Orders, and User accounts.

## How to Run

### Prerequisites
- JDK 17+
- Node.js + npm
- MongoDB (local or Atlas connection string)

### Backend
```bash
cd foodiesapi
./mvnw spring-boot:run
```
Runs on `http://localhost:8081`.

### Frontend
```bash
cd foodies
npm install
npm run dev
```
Runs on the Vite dev server (default `http://localhost:5173`), configured via `VITE_API_URL` to call the backend.

### Docker
```bash
docker-compose up
```
Spins up backend, frontend, and Redis together.

### Feature flags
Payment and storage stubs are controlled in `application.properties`:
```
app.stub.payment=true
app.stub.storage=true
```
Set to `false` once real Razorpay/AWS credentials are configured, to use live integrations instead of stubs.

## Current Status

- Full order flow (register → login → browse → cart → checkout → order tracking) verified end-to-end on the live deployment.
- Admin flow (login, food CRUD, order management) verified end-to-end.
- Real food images served from the backend as static assets.
- 22 JUnit tests passing across service layer (User, Food, Order).
- CI pipeline (GitHub Actions) builds and tests backend + frontend on every push.
- Deployed: frontend (Vercel), backend (Render, Docker), database (MongoDB Atlas), cache (Redis on Render).
- Payment and file upload remain local stubs — not yet connected to live Razorpay/AWS accounts.

## Planned / In Progress

- [ ] Real Razorpay webhook handling for order status updates.
- [ ] Live S3/Cloudinary integration for admin-uploaded images (current uploads work locally but don't persist across redeploys on Render's free tier).
- [ ] Circuit breaker / resilience patterns for external service calls.
- [ ] Broader test coverage (controller layer, integration tests).

## License

For learning and portfolio purposes.