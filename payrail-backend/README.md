📦 PayRail — Backend (Spring Boot)
🚀 Overview

PayRail is a production-style payment processing backend built using Spring Boot that demonstrates how real-world payment systems handle secure payments, webhook processing, idempotency, and financial ledgers.

The project simulates a simplified Stripe-based payment gateway where users can:

Authenticate using JWT

Initiate card payments

Track payment history

Maintain a ledger of financial events

Reliably process Stripe webhooks without duplicate side effects

This project is designed to be interview-ready, focusing on correct system behavior, not just happy-path demos.

🧠 Key Concepts Demonstrated

JWT-based authentication

Payment state management

Stripe webhook signature verification

Idempotent webhook handling

Ledger-based financial recording

Global exception handling

Pagination using Spring Data

OpenAPI (Swagger) documentation

🛠 Tech Stack

Java 17

Spring Boot

Spring Security (JWT)

Spring Data JPA

MySQL

Stripe API

Swagger / OpenAPI

Maven

🔐 Authentication

JWT-based authentication

Login endpoint returns a signed JWT

JWT is required for all protected APIs

Demo Credentials
username: admin
password: password123

📌 Core Features
1️⃣ Payment Processing

Card payments using Stripe test tokens

Payment status transitions handled safely

Payment records persisted in MySQL

2️⃣ Webhook Handling (Idempotent)

Stripe webhook signature verification

Each event ID is processed only once

Duplicate webhook events are ignored safely

Final payment states cannot be overwritten

3️⃣ Ledger System

Every financial event is written to a ledger

Ledger entries represent immutable records

Useful for reconciliation and audit trails

4️⃣ Pagination & Sorting

Payment history and ledger APIs support pagination

Latest records are shown first

🔁 Payment Lifecycle
INITIATED → SUCCESS / FAILED


Once a payment reaches a final state, it cannot be downgraded.

📂 Project Structure
src/main/java/com/payrail
├── auth        → JWT authentication
├── payment     → Payment APIs & service logic
├── webhook     → Stripe webhook handling
├── ledger      → Ledger recording
├── repository  → JPA repositories
├── entity      → JPA entities
├── config      → Security & Swagger config
└── exception   → Global exception handling

📄 API Documentation (Swagger)

After running the application:

http://localhost:8080/swagger-ui.html


All APIs are documented with request/response examples.

▶️ Running Locally
Prerequisites

Java 17+

MySQL

Maven

Environment Variables
STRIPE_SECRET_KEY=sk_test_***
STRIPE_WEBHOOK_SECRET=whsec_***
DB_URL=jdbc:mysql://localhost:3306/payrail
DB_USER=root
DB_PASSWORD=yourpassword

Start Application
mvn spring-boot:run

🧪 Stripe Test Tokens

Use Stripe test tokens for demo:

tok_visa        → Successful payment
tok_chargeDeclined → Failed payment
