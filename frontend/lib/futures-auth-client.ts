import { fetchFuturesBackendApi } from "@/lib/futures-api-request";
import { createFuturesBackendUrl } from "@/lib/futures-sse-url";

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

export function createGoogleLoginUrl() {
  return createFuturesBackendUrl("/oauth2/authorization/google");
}

export type GoogleOnboardingState = {
  active: boolean;
  emailHint: string | null;
  nameHint: string | null;
};

type FuturesApiResponse<T> = {
  success: boolean;
  data: T;
};

export async function getGoogleOnboardingState() {
  const response = await fetchFuturesBackendApi("/auth/google/onboarding", {
    method: "GET",
    credentials: "include",
  });

  if (!response.ok) {
    return { ok: false, data: null };
  }

  const body = (await response.json()) as FuturesApiResponse<GoogleOnboardingState>;
  return { ok: body.success, data: body.data };
}

export async function linkGoogleToExistingAccount(credentials: LoginCredentials) {
  return fetchFuturesBackendApi("/auth/google/link", jsonPostInit(credentials));
}

export type GoogleSignupProfile = {
  name: string;
  nickname: string;
  email: string;
  phoneNumber: string;
  address: {
    zipcode: string;
    address: string;
    addressDetail: string;
  };
  agreement: boolean;
};

export async function completeGoogleSignup(profile: GoogleSignupProfile) {
  return fetchFuturesBackendApi("/auth/google/signup", jsonPostInit(profile));
}

export async function logoutFromFutures() {
  const response = await fetchFuturesBackendApi("/auth/logout", {
    method: "POST",
    credentials: "include",
  });

  return response.ok;
}
