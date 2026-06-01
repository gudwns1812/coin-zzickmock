import type { NextConfig } from "next";
import createBundleAnalyzer from "@next/bundle-analyzer";
import { FUTURES_API_BASE_URL } from "./lib/futures-env";

function optionalBaseUrl(value: string | undefined) {
  const normalized = value?.trim().replace(/\/+$/, "");
  return normalized || undefined;
}

const PUBLIC_FUTURES_API_BASE_URL =
  optionalBaseUrl(process.env.NEXT_PUBLIC_FUTURES_API_BASE_URL) ?? FUTURES_API_BASE_URL;

const withBundleAnalyzer = createBundleAnalyzer({
  enabled: process.env.ANALYZE === "true",
});

const nextConfig: NextConfig = {
  /* config options here */
  env: {
    NEXT_PUBLIC_FUTURES_API_BASE_URL: PUBLIC_FUTURES_API_BASE_URL,
  },
  async rewrites() {
    return [
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
      "s3-symbol-logo.tradingview.com",
      "encrypted-tbn0.gstatic.com",
    ],
  },
};

export default withBundleAnalyzer(nextConfig);
