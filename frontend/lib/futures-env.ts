const DEFAULT_FUTURES_API_BASE_URL = "http://127.0.0.1:8080";

export function getFuturesApiBaseUrl() {
  const configuredBaseUrl = process.env.FUTURES_API_BASE_URL;

  if (!configuredBaseUrl && process.env.VERCEL) {
    throw new Error("FUTURES_API_BASE_URL must be set for Vercel deployments.");
  }

  return (configuredBaseUrl ?? DEFAULT_FUTURES_API_BASE_URL).replace(/\/+$/, "");
}

export const FUTURES_API_BASE_URL = getFuturesApiBaseUrl();
