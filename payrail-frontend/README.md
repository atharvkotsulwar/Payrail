PayRail Frontend — React + Vite

This is the frontend UI for PayRail, built using React (Vite).
It demonstrates how a secure frontend integrates with a payments backend using JWT authentication and Stripe test payment flows.

The UI is intentionally simple to keep the focus on payment lifecycle, backend integration, and system behavior.

Tech Stack

React (Vite)

JavaScript

CSS

Fetch API

Features
Authentication

Login screen with JWT-based authentication

JWT stored in browser storage

Authenticated API calls using Bearer token

New Payment

Create payments using Stripe test cards

Dropdown to simulate success and failure scenarios

Immediate payment response from backend

Payment History

Displays all payments made by the user

Latest payments shown first

Ledger View

Displays ledger entries for all payment events

Color-coded entries for success and failure

Screens Implemented

Login Page

Dashboard

New Payment Page

Payment History Page

Ledger Page

Running Locally

Install dependencies
npm install

Start development server
npm run dev

Environment Configuration

Create a .env file in the project root with the following:

VITE_API_BASE_URL=http://localhost:8080

Demo Credentials

Username: admin
Password: password123

API Integration Flow

User logs in and receives a JWT

JWT is attached to API requests via the Authorization header

Payment is initiated from the UI

Backend creates a Stripe PaymentIntent

Stripe webhook confirms the final payment state

UI updates payment history and ledger
