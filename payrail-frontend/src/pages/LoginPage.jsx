// src/pages/LoginPage.jsx
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { API_BASE_URL, login } from "../api/client";

export default function LoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("password123");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const token = await login(username, password);
      localStorage.setItem("payrail_jwt", token);
      navigate("/dashboard");
    } catch (err) {
      console.error(err);
      if (err.message === "Failed to fetch") {
        setError(
          "Cannot reach backend. Is PayRail backend running on " +
            API_BASE_URL +
            "?"
        );
      } else {
        setError(err.message || "Login failed");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app-root">
      <div className="auth-card">
        <h1 className="app-title">PayRail Admin</h1>
        <p className="app-subtitle">
          Sign in to manage test payments, ledger and Stripe webhooks.
        </p>



        <form onSubmit={handleSubmit} className="form">
          <label className="field">
            <span>Username</span>
            <input
              type="text"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </label>

          <label className="field">
            <span>Password</span>
            <input
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>

          {error && <div className="error-banner">{error}</div>}

          <button
            type="submit"
            className="primary-btn"
            disabled={loading}
          >
            {loading ? "Signing in..." : "Sign in"}
          </button>
        </form>

        <p className="hint">
          Demo creds: <strong>admin / password123</strong>
        </p>
      </div>
    </div>
  );
}
