import { createFuturesBackendApiUrl } from "@/lib/futures-sse-url";

type LoginCredentials = {
  account: string;
  password: string;
};

function jsonPostInit(body: unknown): RequestInit {
  return {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify(body),
  };
}

export async function loginToFutures(credentials: LoginCredentials) {
  return fetch(
    createFuturesBackendApiUrl("/auth/login"),
    jsonPostInit(credentials)
  );
}

export async function logoutFromFutures() {
  const response = await fetch(createFuturesBackendApiUrl("/auth/logout"), {
    method: "POST",
    credentials: "include",
  });

  if (!response.ok) {
    return false;
  }

  const authStateResponse = await fetch(createFuturesBackendApiUrl("/auth/me"), {
    cache: "no-store",
    credentials: "include",
  });

  return authStateResponse.status === 401 || authStateResponse.status === 403;
}
