# PayRail 💳 — Payments + Webhooks + Ledger (Spring Boot + Stripe + MySQL)

PayRail is a production-style payments and ledger service built using **Spring Boot**, **Stripe (PaymentIntents + Webhooks)**, **JWT authentication**, and **MySQL**.

It demonstrates real-world payment workflows such as webhook-driven state transitions, idempotent event handling, and an append-only ledger for auditability.

> ⚠️ This project runs entirely in **Stripe Test Mode**. No real payments are processed.

---

## 🔗 Live Links
- **Frontend (Vercel):** https://payrail-indol.vercel.app/dashboard  
- **Backend (Render):** https://payrail-qefg.onrender.com  
- **Swagger UI:** https://payrail-qefg.onrender.com/swagger-ui/index.html  
- **GitHub Repo:** https://github.com/atharvkotsulwar/Payrail  

---

## ✨ Key Features
- JWT-based authentication
- Stripe PaymentIntent creation & confirmation
- Stripe Webhooks for asynchronous payment status updates
- Webhook signature verification (`STRIPE_WEBHOOK_SECRET`)
- Idempotent webhook processing (prevents duplicate ledger entries)
- Append-only ledger system for payment auditing
- MySQL persistence using JPA/Hibernate
- Pagination & sorting (latest-first)
- Dockerized Spring Boot backend
- Swagger / OpenAPI documentation

---

## 📁 Project Structure
Payrail/
├── payrail-backend/ # Spring Boot API (JWT + Stripe + Webhooks + MySQL)
└── payrail-frontend/ # React + Vite dashboard

---

## 🔐 Demo Credentials
For demo/testing via the UI:

- **Username:** `admin`
- **Password:** `password123`

---

## ▶️ Quick Demo Flow
1. Open the frontend dashboard  
   https://payrail-indol.vercel.app/dashboard
2. Login using demo credentials
3. Create a new payment (amount + currency)
4. Use Stripe test cards:
   - `4242 4242 4242 4242` → successful payment
   - `4000 0000 0000 0002` → failed payment
5. Verify:
   - Payment appears in **Payment History** (latest first)
   - Ledger updates after webhook events

---

## 🔔 Stripe Webhook Setup (Test Mode)

In **Stripe Dashboard → Developers → Webhooks**, add a new endpoint.

### Endpoint URL
https://payrail-qefg.onrender.com/api/webhooks/stripe


### Events to listen to
- `payment_intent.succeeded`
- `payment_intent.payment_failed`

After creating the webhook, copy the **Signing Secret** and configure it in backend environment variables:

STRIPE_WEBHOOK_SECRET=whsec_XXXXXXXX


---

## 🧪 Local Setup (Optional)

### Backend
```bash
cd payrail-backend
# configure environment variables (see .env.example)
mvn spring-boot:run

### Frontend

cd payrail-frontend
npm install
npm run dev

🔧 Environment Variables

Example environment files are provided:

payrail-backend/.env.example

payrail-frontend/.env.example


