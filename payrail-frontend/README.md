# PayRail Frontend — React + Vite

This is the frontend UI for **PayRail**, built using **React (Vite)**.
It demonstrates how a secure frontend integrates with a payments backend using **JWT authentication** and **Stripe test payment flows**.

The UI is intentionally simple to keep the focus on system behavior and payment lifecycle.

---

## Tech Stack
- React (Vite)
- JavaScript
- CSS
- Fetch API

---

## Features

### Authentication
- Login screen with JWT-based authentication
- JWT stored in `localStorage`
- Authenticated API calls using `Authorization: Bearer <token>`

### New Payment
- Create payments using Stripe **test cards**
- Dropdown to simulate success and failure scenarios
- Displays immediate payment response from backend

### Payment History
- Lists all payments created by the user
- Latest payments displayed first

### Ledger View
- Displays ledger entries for all payment events
- Color-coded entries for **success** and **failure**

---

## Screens Implemented
- Login Page
- Dashboard
- New Payment Page
- Payment History Page
- Ledger Page

---

## Running Locally

### Install Dependencies
```bash
npm install
