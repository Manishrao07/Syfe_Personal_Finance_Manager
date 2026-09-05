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

## Local development

```bash
npm install
npm run dev
```

Set the backend URL in `.env.local` (see `.env.example`):

```
NEXT_PUBLIC_API_BASE_URL=https://syfe-personal-finance-manager-1uct.onrender.com/api
```

To point at a local backend instead, set it to `http://localhost:8080/api` — the backend's `local` profile already allows CORS from `http://localhost:3000` by default.

## Deploying to Vercel

1. In the [Vercel dashboard](https://vercel.com/new), import this repo.
2. Under **Root Directory**, select `frontend` (this repo also contains the
   unrelated Java backend at its root, so Vercel needs to know to build only
   this subfolder) — it then auto-detects Next.js, no further config needed.
3. Add an environment variable: `NEXT_PUBLIC_API_BASE_URL` = your deployed backend's `/api` base URL. (`NEXT_PUBLIC_*` vars are inlined at build time, so this must be set in Vercel's project settings, not just `.env.local`.)
4. Deploy.

### Backend CORS

The backend's `render` profile allows any `https://*.vercel.app` origin by default (see `app.cors.allowed-origin-patterns` in the backend's `application.yml`), so a fresh Vercel deployment should work without any backend changes. To restrict it to your exact production domain instead, set the `CORS_ALLOWED_ORIGINS` environment variable on the backend's Render service (comma-separated if you need more than one origin).

## Notes

- Auth state is determined by probing `GET /api/categories` on load (200 → logged in, 401 → not) since the API has no dedicated "current user" endpoint.
- The category list endpoint doesn't expose numeric IDs, so transaction filtering uses the backend's `category` (name) query parameter rather than `categoryId`.
- A transaction's `date` is intentionally not editable in the edit form — the API rejects/ignores it, per the assignment spec.
