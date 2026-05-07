import type { NextConfig } from "next";
import createBundleAnalyzer from "@next/bundle-analyzer";
import { FUTURES_API_BASE_URL } from "./lib/futures-env";

const withBundleAnalyzer = createBundleAnalyzer({
  enabled: process.env.ANALYZE === "true",
});

const nextConfig: NextConfig = {
  /* config options here */
  async rewrites() {
    return [
      {
        // 로컬 인증 서버
        source: "/proxy/auth/:path*",
        destination: `${FUTURES_API_BASE_URL}/api/futures/auth/:path*`,
      },
      {
        // 메인서버
        source: "/proxy/:path*",
        destination: "https://news-toss.click/api/:path*",
      },
      {
        // 증권서버
        source: "/proxy2/:path*",
        destination: "http://43.201.62.55:8080/api/:path*",
      },
      {
        // 코인 선물 백엔드
        source: "/proxy-futures/:path*",
        destination: `${FUTURES_API_BASE_URL}/api/futures/:path*`,
      },
    ];
  },
  images: {
    unoptimized: true,
    dangerouslyAllowSVG: true,
    domains: [
      "placehold.co",
      "imgnews.pstatic.net",
      "ssl.pstatic.net",
      "s3-symbol-logo.tradingview.com",
      "encrypted-tbn0.gstatic.com",
      "thumb.tossinvest.com",
    ],
  },
};

export default withBundleAnalyzer(nextConfig);
