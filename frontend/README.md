# Syfe Finance — Frontend

A Next.js + Tailwind CSS dashboard for the Personal Finance Manager API defined
at the root of this repo. Covers registration/login, transactions, categories,
savings goals, and monthly/yearly reports.

This lives in its own `frontend/` subfolder alongside the Spring Boot backend —
they're independently built and deployed (backend → Render, this → Vercel with
its **Root Directory** project setting pointed at `frontend`), just sharing one
repo for convenience.

## Tech Stack

- Next.js 16 (App Router) + TypeScript
- Tailwind CSS v4
- Recharts (dashboard bar chart, expense breakdown donut chart)
- Session-cookie auth against the Spring Boot backend (`credentials: "include"` on every request — no tokens stored client-side)
- All `/api/*` calls are proxied server-side to the backend via a rewrite in
  `next.config.ts`, so the browser only ever talks to this app's own origin.
  This is deliberate: a direct browser-to-backend call would make the session
  cookie a genuine cross-site (third-party) cookie between the Vercel and
  Render domains, which some browsers/privacy settings block outright even
  with `SameSite=None; Secure` set correctly — silently breaking every
  authenticated request after a seemingly successful login. Proxying makes it
  a normal first-party cookie instead, avoiding the problem entirely.

## Local development

```bash
npm install
npm run dev
```

Set the backend URL in `.env.local` (see `.env.example`) — this is a plain
server-side variable (no `NEXT_PUBLIC_` prefix) since it's only read by the
rewrite in `next.config.ts`, never sent to the browser:

```
BACKEND_API_URL=https://syfe-personal-finance-manager-1uct.onrender.com/api
```

To point at a local backend instead, set it to `http://localhost:8080/api`.

## Deploying to Vercel

1. In the [Vercel dashboard](https://vercel.com/new), import this repo.
2. Under **Root Directory**, select `frontend` (this repo also contains the
   unrelated Java backend at its root, so Vercel needs to know to build only
   this subfolder) — it then auto-detects Next.js, no further config needed.
3. Add an environment variable: `BACKEND_API_URL` = your deployed backend's `/api` base URL. Use the **Config** type, not **Secret** — it needs to be readable by the rewrite at request time.
4. Deploy.

### Backend CORS

Because the browser only ever calls this app's own origin, the backend's CORS
configuration doesn't come into play for this deployment path at all (CORS is
a browser concept — the Vercel-to-Render leg of the proxy is a server-to-server
call). The backend's `app.cors.allowed-origin-patterns` setting exists for
completeness / anyone hitting the API directly from a browser, but isn't
required for this frontend to work.

## Notes

- Auth state is determined by probing `GET /api/categories` on load (200 → logged in, 401 → not) since the API has no dedicated "current user" endpoint.
- The category list endpoint doesn't expose numeric IDs, so transaction filtering uses the backend's `category` (name) query parameter rather than `categoryId`.
- A transaction's `date` is intentionally not editable in the edit form — the API rejects/ignores it, per the assignment spec.
