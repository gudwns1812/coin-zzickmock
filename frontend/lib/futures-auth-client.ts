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
  const directResponse = await fetch(
    createFuturesBackendApiUrl("/auth/login"),
    jsonPostInit(credentials)
  );

  if (!directResponse.ok) {
    return directResponse;
  }

  const sameOriginResponse = await fetch(
    "/proxy/auth/login",
    jsonPostInit(credentials)
  );

  return sameOriginResponse.ok ? directResponse : sameOriginResponse;
}

export async function logoutFromFutures() {
  const [directResult, sameOriginResult] = await Promise.allSettled([
    fetch(createFuturesBackendApiUrl("/auth/logout"), {
      method: "POST",
      credentials: "include",
    }),
    fetch("/proxy/auth/logout", {
      method: "POST",
      credentials: "include",
    }),
  ]);

  const directOk =
    directResult.status === "fulfilled" && directResult.value.ok;
  const sameOriginOk =
    sameOriginResult.status === "fulfilled" && sameOriginResult.value.ok;

  return directOk || sameOriginOk;
}
