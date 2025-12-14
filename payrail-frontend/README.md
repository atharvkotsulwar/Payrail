💻 PayRail — Frontend (React)
🚀 Overview

This frontend is a React-based UI built to interact with the PayRail backend.
It demonstrates how a secure frontend integrates with a payment backend using JWT authentication and Stripe test flows.

The UI is intentionally simple and clean to keep focus on functionality and system flow.

🛠 Tech Stack

React (Vite)

JavaScript

CSS

Fetch API

📌 Features
🔐 Authentication

Login screen

JWT stored in browser storage

Authenticated API calls using Bearer token

💳 New Payment

Card payment simulation using Stripe test tokens

Dropdown to select success/failure scenarios

Real-time payment response handling

📜 Payment History

Displays all payments made by the user

Latest payments appear first

📘 Ledger View

Displays ledger entries for all payment events

Color-coded entries for clarity (success / failed)

🖥 Screens Implemented

Login Page

Dashboard

New Payment Page

Payment History Page

Ledger Page

▶️ Running Locally
Install Dependencies
npm install

Start Development Server
npm run dev

🔧 Environment Configuration

Create .env file:

VITE_API_BASE_URL=http://localhost:8080

🔑 Demo Credentials
username: admin
password: password123

🔄 API Integration Flow

User logs in → receives JWT

JWT attached to Authorization header

Payment initiated from UI

Backend processes Stripe charge

Webhook confirms final payment state

UI reflects updated history and ledger