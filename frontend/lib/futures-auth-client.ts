import { fetchFuturesBackendApi } from "@/lib/futures-api-request";

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
  return fetchFuturesBackendApi("/auth/login", jsonPostInit(credentials));
}

export async function logoutFromFutures() {
  const response = await fetchFuturesBackendApi("/auth/logout", {
    method: "POST",
    credentials: "include",
  });

  if (!response.ok) {
    return false;
  }

  const authStateResponse = await fetchFuturesBackendApi("/auth/me", {
    cache: "no-store",
    credentials: "include",
  });

  return authStateResponse.status === 401 || authStateResponse.status === 403;
}
