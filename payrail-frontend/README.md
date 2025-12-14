# PayRail Frontend — React + Vite

This is the frontend UI for **PayRail**, built using **React (Vite)**.

It demonstrates how a secure frontend integrates with a payment backend using **JWT authentication** and **Stripe test payment flows**.  
The UI is intentionally simple and clean to keep the focus on functionality, system flow, and backend integration.

---

## 🛠 Tech Stack
- React (Vite)
- JavaScript
- CSS
- Fetch API

---

## ✨ Features

### 🔐 Authentication
- Login screen
- JWT-based authentication
- JWT stored in browser storage
- Authenticated API calls using Bearer token

### 💳 New Payment
- Create payments using Stripe **test cards**
- Dropdown to simulate success and failure scenarios
- Real-time payment response handling

### 📜 Payment History
- Displays all payments made by the user
- Latest payments appear first

### 📘 Ledger View
- Displays ledger entries for all payment events
- Color-coded entries for clarity (success / failure)

---

## 🖥 Screens Implemented
- Login Page
- Dashboard
- New Payment Page
- Payment History Page
- Ledger Page

---

## ▶️ Running Locally

### Install dependencies
npm install
### Start development server
npm run dev


---

## 🔧 Environment Configuration

Create a `.env` file in the project root:

VITE_API_BASE_URL=http://localhost:8080


---

## 🔑 Demo Credentials

Username: admin
Password: password123


---

## 🔄 API Integration Flow
1. User logs in and receives a JWT
2. JWT is attached to API requests via the `Authorization` header
3. Payment is initiated from the UI
4. Backend creates a Stripe PaymentIntent
5. Stripe webhook confirms the final payment state
6. UI reflects updated payment history and ledger

---

## ⚠️ Notes
- This frontend is designed to work with the **PayRail backend**
- All payments run in **Stripe Test Mode**
- No real money transactions are performed
