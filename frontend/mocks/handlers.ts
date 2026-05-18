import { http, HttpResponse } from "msw";

export const handlers = [
  http.post("*/api/futures/auth/login", () => {
    return HttpResponse.json({ data: true });
  }),

  http.post("*/api/futures/auth/register", () => {
    return HttpResponse.json({ data: true }, { status: 201 });
  }),

  http.post("*/api/futures/auth/duplicate", async ({ request }) => {
    const body = (await request.json()) as { account?: string };
    const isAvailable = body.account !== "admin";

    return HttpResponse.json(
      { data: isAvailable },
      { status: isAvailable ? 200 : 409 }
    );
  }),

  http.get("*/api/futures/auth/refresh", () => {
    return HttpResponse.json({ data: true });
  }),

  http.post("*/api/futures/auth/invest", () => {
    return HttpResponse.json({ data: true });
  }),

  http.delete("*/api/futures/auth/withdraw", () => {
    return HttpResponse.json({ data: true });
  }),

  http.post("*/api/futures/auth/logout", () => {
    return HttpResponse.json({ data: true });
  }),
];
