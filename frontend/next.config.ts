import type { NextConfig } from "next";
import path from "node:path";

const BACKEND_API_URL = process.env.BACKEND_API_URL ?? "http://localhost:8080/api";

const nextConfig: NextConfig = {
  turbopack: {
    root: path.join(__dirname),
  },
  // Proxies /api/* to the Spring Boot backend server-side, so the browser only
  // ever talks to this app's own origin. Without this, the session cookie is a
  // genuine cross-site (third-party) cookie between the Vercel and Render
  // domains — which SameSite=None;Secure lets through in most browsers, but
  // which some browsers/profiles block outright regardless (third-party
  // cookie blocking, e.g. some Chrome profiles' privacy settings), silently
  // breaking every authenticated request after a seemingly successful login.
  // Proxying makes the cookie first-party instead, sidestepping the problem
  // entirely rather than depending on cross-site cookie support.
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${BACKEND_API_URL}/:path*`,
      },
    ];
  },
};

export default nextConfig;
