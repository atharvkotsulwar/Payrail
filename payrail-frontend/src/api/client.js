// src/api/client.js

let API_BASE_URL = "http://localhost:8080";

if (typeof import.meta !== "undefined" && import.meta.env?.VITE_API_BASE_URL) {
  API_BASE_URL = String(import.meta.env.VITE_API_BASE_URL).replace(/\/$/, "");
}

const JWT_KEY = "payrail_jwt";

// ---------- helpers ----------
export function getJwt() {
  return localStorage.getItem(JWT_KEY) || "";
}

export function setJwt(token) {
  if (!token) return;
  localStorage.setItem(JWT_KEY, String(token).trim());
}

export function clearJwt() {
  localStorage.removeItem(JWT_KEY);
}

function buildHeaders(extra = {}, auth = true) {
  const headers = { "Content-Type": "application/json", ...extra };

  if (auth) {
    const jwt = getJwt();
    if (jwt) headers.Authorization = `Bearer ${jwt}`;
  }

  return headers;
}

async function parseResponse(res) {
  const contentType = res.headers.get("content-type") || "";
  const isJson = contentType.includes("application/json");

  // Try to parse body even on errors, so UI gets meaningful message
  let data = null;
  try {
    data = isJson ? await res.json() : await res.text();
  } catch {
    data = null;
  }

  if (res.ok) return data;

  // Normalize error message
  const msg =
    (data && typeof data === "object" && (data.message || data.error)) ||
    (typeof data === "string" && data.trim()) ||
    `Request failed (${res.status})`;

  const err = new Error(msg);
  err.status = res.status;
  err.data = data;
  throw err;
}

// ---------- core request ----------
export async function apiRequest(path, options = {}) {
  const {
    method = "GET",
    body,
    headers = {},
    auth = true,
    signal,
  } = options;

  const url = `${API_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`;

  const res = await fetch(url, {
    method,
    headers: buildHeaders(headers, auth),
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  });

  return parseResponse(res);
}

// ---------- auth ----------
export async function login(username, password) {
  const data = await apiRequest("/auth/login", {
    method: "POST",
    auth: false,
    body: { username, password },
  });

  // Support backend returning either { token } or plain token string
  const token =
    (data && typeof data === "object" && data.token) ||
    (typeof data === "string" && data);

  if (!token) throw new Error("Login succeeded but token not found in response.");

  return String(token).trim();
}

export { API_BASE_URL };
