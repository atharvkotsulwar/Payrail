import { useEffect, useMemo, useState } from "react";
import { apiRequest } from "../api/client"; // ✅ use your existing client.js

// ---------------- Token dropdown (custom, nicer than native select) ----------------
function TokenSelect({ value, onChange, options }) {
  const [open, setOpen] = useState(false);

  const selected = options.find((o) => o.value === value) || options[0];

  useEffect(() => {
    const onDoc = (e) => {
      const el = e.target;
      if (!el?.closest?.(".tokenSelect")) setOpen(false);
    };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, []);

  return (
    <div className="tokenSelect">
      <button
        type="button"
        className="tokenSelectBtn"
        onClick={() => setOpen((s) => !s)}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        <span className="tokenSelectValue">
          {selected?.label || selected?.title || selected?.value}
        </span>
        <span className="tokenSelectChevron">▾</span>
      </button>

      {open ? (
        <div className="tokenMenu" role="listbox">
          {options.map((o) => {
            const active = o.value === value;
            const dotClass =
              o.tone === "success"
                ? "tokenDot tokenDotSuccess"
                : o.tone === "fail"
                ? "tokenDot tokenDotFail"
                : "tokenDot tokenDotCustom";

            return (
              <button
                key={o.value}
                type="button"
                className={`tokenMenuItem ${active ? "tokenMenuItemActive" : ""}`}
                onClick={() => {
                  onChange(o.value);
                  setOpen(false);
                }}
              >
                <span className={dotClass} />
                <span style={{ minWidth: 0, overflow: "hidden" }}>
                  <div
                    style={{
                      fontWeight: 700,
                      fontSize: 13,
                      overflow: "hidden",
                      textOverflow: "ellipsis",
                      whiteSpace: "nowrap",
                    }}
                  >
                    {o.title}
                  </div>
                  {o.sub ? (
                    <div
                      style={{
                        fontSize: 12,
                        opacity: 0.75,
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                      }}
                    >
                      {o.sub}
                    </div>
                  ) : null}
                </span>
              </button>
            );
          })}
        </div>
      ) : null}
    </div>
  );
}

function formatDate(iso) {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return String(iso);
  return d.toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" });
}

export default function DashboardPage() {
  const [active, setActive] = useState("new"); // new | history | ledger

  return (
    <div className="page">
      <aside className="sidebar">
        <div className="brand">
          <div className="brandAvatar">P</div>
          <div>
            <div className="brandTitle">PayRail</div>
            <div className="brandSub">Stripe • Ledger • Webhooks</div>
          </div>
        </div>

        <nav className="nav">
          <NavItem active={active === "new"} onClick={() => setActive("new")}>
            New Payment
          </NavItem>
          <NavItem active={active === "history"} onClick={() => setActive("history")}>
            Payment History
          </NavItem>
          <NavItem active={active === "ledger"} onClick={() => setActive("ledger")}>
            Ledger
          </NavItem>
        </nav>

        <div className="sidebarFooter">
          <button
            className="logout-btn"
            onClick={() => {
              // ✅ clear any stored auth
              localStorage.removeItem("token");
              localStorage.removeItem("accessToken");
              localStorage.removeItem("jwt");
              localStorage.removeItem("payrail_jwt");

              sessionStorage.removeItem("token");
              sessionStorage.removeItem("accessToken");
              sessionStorage.removeItem("jwt");

              localStorage.removeItem("user");

              window.location.href = "/login";
            }}
          >
            Logout
          </button>
        </div>
      </aside>

      <main className="content">
        {active === "new" && <NewPaymentSection />}
        {active === "history" && <PaymentHistorySection />}
        {active === "ledger" && <LedgerSnapshotSection />}
      </main>
    </div>
  );
}

/* ---------------- UI bits ---------------- */

function NavItem({ active, children, onClick }) {
  return (
    <button className={`navItem ${active ? "navItemActive" : ""}`} onClick={onClick}>
      {children}
    </button>
  );
}

function SectionHeader({ title, subtitle, right }) {
  return (
    <div className="sectionHead">
      <div>
        <h2 className="section-title">{title}</h2>
        {subtitle ? <p className="section-subtitle">{subtitle}</p> : null}
      </div>
      {right ? <div className="sectionRight">{right}</div> : null}
    </div>
  );
}

function Pill({ children, tone = "default" }) {
  return <span className={`pill pill-${tone}`}>{children}</span>;
}

function Field({ label, hint, children }) {
  return (
    <div className="fieldWrap">
      <label className="fieldLabel">{label}</label>
      {children}
      {hint ? <div className="fieldHint">{hint}</div> : null}
    </div>
  );
}

/* ---------------- New Payment ---------------- */

function PaymentCardPreview({ amount, currency, token }) {
  const pretty = (() => {
    const n = Number(amount || 0);
    if (!Number.isFinite(n)) return "—";
    return `${(n / 100).toFixed(2)} ${String(currency || "").toUpperCase()}`;
  })();

  return (
    <div className="cardPreview">
      <div className="cardPreviewTop">
        <div>
          <div className="cardBrand">PayRail</div>
          <div className="cardAmount">{pretty}</div>
        </div>
        <div className="badgeTest">TEST</div>
      </div>

      <div className="cardMetaRow">
        <div className="metaBox">
          <div className="metaLabel">Currency</div>
          <div className="metaValue">{(currency || "—").toUpperCase()}</div>
        </div>
        <div className="metaBox">
          <div className="metaLabel">Token</div>
          <div className="metaValue">{token || "—"}</div>
        </div>
      </div>

      <div className="cardFooter">
        <span>Visa • **** 4242</span>
        <span>Powered by Stripe</span>
      </div>
    </div>
  );
}

function ReceiptCard({ result }) {
  const status = (result?.status || "").toString().toUpperCase();
  const ok = status === "SUCCESS";
  const failed = status === "FAILED";

  const rows = [
    ["Payment ID", result?.paymentId],
    ["Amount (cents)", result?.amount],
    ["Currency", (result?.currency || "").toUpperCase()],
    ["Status", result?.status],
    ["Created At", formatDate(result?.createdAt)],
  ];

  return (
    <div className="receiptCard">
      <div className="receiptHeader">
        <div>
          <div className="receiptTitle">Payment Receipt</div>
        </div>
        <div className={`statusPill ${ok ? "ok" : failed ? "danger" : "warn"}`}>
          {status || "UNKNOWN"}
        </div>
      </div>

      <div className="kv">
        {rows.map(([k, v]) => (
          <div className="kvRow" key={k}>
            <div className="kvKey">{k}</div>
            <div className="kvVal">{String(v ?? "—")}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

function NewPaymentSection() {
  const [amount, setAmount] = useState(100);
  const [currency, setCurrency] = useState("usd");

  // ✅ Token dropdown presets (success + fail)
  // Note: Use Stripe test tokens (these are guaranteed to trigger the exact outcome).
  const TOKEN_PRESETS = [
    {
      title: "Success (Visa)",
      sub: "tok_visa",
      value: "tok_visa",
      tone: "success",
    },
    {
      title: "Declined",
      sub: "tok_chargeDeclined",
      value: "tok_chargeDeclined",
      tone: "fail",
    },
    {
      title: "Insufficient Funds",
      sub: "tok_chargeDeclinedInsufficientFunds",
      value: "tok_chargeDeclinedInsufficientFunds",
      tone: "fail",
    },
    {
      title: "Expired Card",
      sub: "tok_chargeDeclinedExpiredCard",
      value: "tok_chargeDeclinedExpiredCard",
      tone: "fail",
    },
    {
      title: "Incorrect CVC",
      sub: "tok_chargeDeclinedIncorrectCvc",
      value: "tok_chargeDeclinedIncorrectCvc",
      tone: "fail",
    },
    {
      title: "Custom token",
      sub: "Paste any Stripe token",
      value: "__custom__",
      tone: "custom",
    },
  ];

  const [tokenPreset, setTokenPreset] = useState("tok_visa");
  const [customToken, setCustomToken] = useState("");
  const tokenHeader = tokenPreset === "__custom__" ? customToken : tokenPreset;

  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setResult(null);
    setLoading(true);

    try {
      const data = await apiRequest("/payment/charge", {
        method: "POST",
        headers: { token: tokenHeader },
        body: { amount: Number(amount), currency: currency.toLowerCase() },
      });
      setResult(data);
    } catch (err) {
      setError(err?.data?.message || err?.message || "Payment failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <SectionHeader title="New Payment" subtitle="Fill the card → charge → get a clean receipt." />

      <div className="paygrid">
        <div className="panel">
          <PaymentCardPreview amount={amount} currency={currency} token={tokenHeader} />

          <form className="form" onSubmit={handleSubmit}>
            <div className="grid2">
              <Field label="Amount (cents)">
                <input
                  type="number"
                  min="1"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                />
              </Field>

              <Field label="Currency" hint="usd, inr, eur ...">
                <input
                  type="text"
                  value={currency}
                  onChange={(e) => setCurrency(e.target.value.toLowerCase())}
                />
              </Field>
            </div>

            <Field label="Stripe token (header: token)" hint="Pick success/fail tokens quickly.">
              <TokenSelect value={tokenPreset} onChange={setTokenPreset} options={TOKEN_PRESETS} />

              {tokenPreset === "__custom__" ? (
                <input
                  type="text"
                  placeholder="Enter token e.g. tok_visa"
                  value={customToken}
                  onChange={(e) => setCustomToken(e.target.value)}
                />
              ) : null}
            </Field>

            {error ? <div className="error-banner">{error}</div> : null}

            <div className="actions">
              <button
                type="button"
                className="btn-secondary"
                onClick={() => {
                  setResult(null);
                  setError("");
                }}
              >
                Clear
              </button>
              <button type="submit" className="btn-primary" disabled={loading}>
                {loading ? "Processing..." : "Charge Card"}
              </button>
            </div>
          </form>
        </div>

        <div className="panel">
          {!result ? (
            <div className="emptyState">
              <div className="emptyTitle">Payment Receipt</div>
              <div className="emptySub">Submit a charge to see formatted output here.</div>
            </div>
          ) : (
            <ReceiptCard result={result} />
          )}
        </div>
      </div>
    </>
  );
}

/* ---------------- Payment History ---------------- */

function StatusPill({ status }) {
  const s = String(status || "").toUpperCase();
  const tone =
    s === "SUCCESS"
      ? "ok"
      : s === "FAILED"
      ? "danger"
      : s === "PENDING"
      ? "warn"
      : "default";
  return <Pill tone={tone}>{s || "UNKNOWN"}</Pill>;
}

function PaymentHistorySection() {
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const res = await apiRequest(`/payment/history?page=${page}&size=${size}`, {
        method: "GET",
      });
      setData(res);
    } catch (e) {
      setError(e?.data?.message || e?.message || "Failed to load history");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  const rows = Array.isArray(data) ? data : data?.items || data?.content || [];
  const sortedRows = useMemo(() => {
    const copy = [...(rows || [])];
    copy.sort((a, b) => {
      const da = new Date(a?.createdAt || 0).getTime();
      const db = new Date(b?.createdAt || 0).getTime();
      return db - da; // newest first
    });
    return copy;
  }, [rows]);

  const totalPages = Array.isArray(data) ? null : data?.totalPages ?? null;

  return (
    <>
      <SectionHeader
        title="Payment History"
        right={
          <button className="btn-secondary" onClick={load} disabled={loading}>
            {loading ? "Refreshing..." : "Refresh"}
          </button>
        }
      />

      {error ? <div className="error-banner">{error}</div> : null}

      <div className="panel">
        <div className="tableWrap">
          <table className="table">
            <thead>
              <tr>
                <th>Payment ID</th>
                <th>Amount</th>
                <th>Currency</th>
                <th>Status</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {sortedRows?.length ? (
                sortedRows.map((r, idx) => (
                  <tr key={r.paymentId || r.id || idx}>
                    <td className="mono">
                      {r.paymentId || r.id || "—"}
                      <button
                        className="copyBtn"
                        onClick={() =>
                          navigator.clipboard.writeText(String(r.paymentId || r.id || ""))
                        }
                        title="Copy"
                      >
                        copy
                      </button>
                    </td>
                    <td>{String(r.amount ?? "—")}</td>
                    <td>{String(r.currency ?? "—").toUpperCase()}</td>
                    <td>
                      <StatusPill status={r.status} />
                    </td>
                    <td className="mono">{formatDate(r.createdAt)}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} className="emptyCell">
                    {loading ? "Loading..." : "No history yet"}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="pager">
          <button
            className="btn-secondary"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
          >
            Prev
          </button>

          <div className="pagerMid">
            <Pill>Page: {page + 1}</Pill>
            {totalPages !== null ? <Pill>Total: {totalPages}</Pill> : null}
          </div>

          <button
            className="btn-secondary"
            onClick={() => setPage((p) => p + 1)}
            disabled={totalPages !== null ? page + 1 >= totalPages : false}
          >
            Next
          </button>
        </div>
      </div>
    </>
  );
}

/* ---------------- Ledger Snapshot ---------------- */

function normalizeLedgerRow(r) {
  const entryId = r.entryId ?? r.id ?? r.ledgerId ?? r.ledgerEntryId ?? "—";

  const paymentId =
    r.paymentId ??
    r.payment_id ??
    r.stripePaymentId ??
    r.chargeId ??
    r.charge_id ??
    r.referenceId ??
    r.refId ??
    "—";

  const amount = r.amount ?? r.amountCents ?? r.amount_cents ?? "—";

  const type = r.type ?? r.entryType ?? r.eventType ?? r.event_type ?? r.status ?? "—";

  const createdAt = r.createdAt ?? r.created_at ?? r.timestamp ?? r.created ?? null;

  return { entryId, paymentId, amount, type, createdAt, raw: r };
}

function LedgerSnapshotSection() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const res = await apiRequest("/payment/ledger", { method: "GET" });
      setData(res);
    } catch (e) {
      setError(e?.data?.message || e?.message || "Failed to load ledger");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const rowsRaw = Array.isArray(data) ? data : data?.entries || [];
  const rows = rowsRaw.map(normalizeLedgerRow);

  const sortedRows = useMemo(() => {
    const copy = [...rows];
    copy.sort((a, b) => {
      const da = new Date(a?.createdAt || 0).getTime();
      const db = new Date(b?.createdAt || 0).getTime();
      if (db !== da) return db - da; // newest first

      // fallback: higher numeric entryId first (if present)
      const ia = Number(a?.entryId || 0);
      const ib = Number(b?.entryId || 0);
      return ib - ia;
    });
    return copy;
  }, [rows]);

  const total = sortedRows.reduce((sum, r) => sum + Number(r.amount || 0), 0);

  return (
    <>
      <SectionHeader
        title="Ledger"
        right={
          <button className="btn-secondary" onClick={load} disabled={loading}>
            {loading ? "Refreshing..." : "Refresh"}
          </button>
        }
      />

      {error ? <div className="error-banner">{error}</div> : null}

      <div className="panel">
        <div className="statsRow">
          <div className="statCard">
            <div className="statLabel">Entries</div>
            <div className="statVal">{sortedRows.length}</div>
          </div>

          <div className="statCard">
            <div className="statLabel">Total Amount</div>
            <div className="statVal">{total}</div>
          </div>
        </div>

        <div className="tableWrap">
          <table className="table">
            <thead>
              <tr>
                <th>#</th>
                <th>Payment</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {sortedRows?.length ? (
                sortedRows.map((r, idx) => (
                  <tr key={`${r.paymentId}-${idx}`}>
                    <td className="mono">{idx + 1}</td>
                    <td className="mono">{String(r.paymentId ?? "—")}</td>
                    <td>{String(r.amount ?? "—")}</td>
                    <td>
                      <StatusPill status={r.type} />
                    </td>
                    <td className="mono">{formatDate(r.createdAt)}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} className="emptyCell">
                    {loading ? "Loading..." : "No ledger entries yet"}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
