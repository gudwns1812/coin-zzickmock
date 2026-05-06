import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { isSupportedMarketSymbol } from "./lib/markets";

export async function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;
  const userAgent = req.headers.get("user-agent") || "";
  const isMobile =
    /iPhone|Android|Mobile|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
      userAgent
    );

  const token = req.cookies.get("accessToken")?.value;

  if (isMobile && pathname !== "/only-desktop") {
    return NextResponse.redirect(new URL("/only-desktop", req.url));
  }

  if (pathname === "/stock" || pathname.startsWith("/stock/")) {
    return NextResponse.redirect(new URL("/markets", req.url));
  }

  if (
    (pathname.startsWith("/portfolio") ||
      pathname.startsWith("/mypage") ||
      pathname.startsWith("/watchlist") ||
      pathname.startsWith("/admin") ||
      pathname.startsWith("/shop")) &&
    !token
  ) {
    return NextResponse.redirect(new URL("/login", req.url));
  }

  if (pathname === "/portfolio" || pathname.startsWith("/portfolio/")) {
    return NextResponse.redirect(new URL("/mypage", req.url));
  }

  if (pathname.startsWith("/markets/")) {
    const symbol = pathname.split("/")[2];

    if (!isSupportedMarketSymbol(symbol || "")) {
      return NextResponse.redirect(new URL("/markets", req.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - api (API routes)
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico, sitemap.xml, robots.txt (metadata files)
     */
    "/((?!api|monitoring|_next/static|_next/image|favicon.ico|sitemap.xml|robots.txt).*)",
    "/markets/:path*",
  ],
};
