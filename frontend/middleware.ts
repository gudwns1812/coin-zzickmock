import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { getJwtToken } from "./utils/auth";

export async function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;
  const userAgent = req.headers.get("user-agent") || "";
  const isMobile =
    /iPhone|Android|Mobile|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
      userAgent
    );

  const token = await getJwtToken();

  if (isMobile && pathname !== "/only-desktop") {
    return NextResponse.redirect(new URL("/only-desktop", req.url));
  }

  if (pathname.startsWith("/portfolio") && !token) {
    return NextResponse.redirect(new URL("/stock", req.url));
  }

  if (pathname.startsWith("/stock/")) {
    // /stock/ 다음의 code 추출
    const code = pathname.split("/")[2];

    // code가 6자리 숫자가 아니면 리다이렉트
    if (!/^\d{6}$/.test(code || "")) {
      return NextResponse.redirect(new URL("/stock", req.url));
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
    "/((?!api|_next/static|_next/image|favicon.ico|sitemap.xml|robots.txt).*)",
    "/stock/:path*",
  ],
};
