import { http, HttpResponse } from "msw";

export const handlers = [
  http.post("*/proxy/auth/login", () => {
    return HttpResponse.json({ data: true });
  }),

  http.post("*/proxy/auth/register", () => {
    return HttpResponse.json({ data: true }, { status: 201 });
  }),

  http.post("*/proxy/auth/duplicate", async ({ request }) => {
    const body = (await request.json()) as { account?: string };
    const isAvailable = body.account !== "admin";

    return HttpResponse.json(
      { data: isAvailable },
      { status: isAvailable ? 200 : 409 }
    );
  }),

  http.get("*/proxy/auth/refresh", () => {
    return HttpResponse.json({ data: true });
  }),

  http.post("*/proxy/auth/invest", () => {
    return HttpResponse.json({ data: true });
  }),

  http.delete("*/proxy/auth/withdraw", () => {
    return HttpResponse.json({ data: true });
  }),

  http.post("*/proxy/auth/logout", () => {
    return HttpResponse.json({ data: true });
  }),
];
