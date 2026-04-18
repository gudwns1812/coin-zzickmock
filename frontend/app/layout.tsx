import type { Metadata } from "next";
import localFont from "next/font/local";
import { ToastContainer } from "react-toastify";
import SentryProvider from "../components/router/SentryProvider";
import "./globals.css";
import "driver.js/dist/driver.css";
import QueryClientProvider from "../components/router/QueryClientProvider";
import { MSWProvider } from "./MSWProvider";

const pretendard = localFont({
  src: "../public/fonts/PretendardVariable.woff2",
  display: "swap",
  weight: "100 900",
  variable: "--font-pretendard",
});

export const metadata: Metadata = {
  title: {
    template: "코인 선물 찍먹 | %s",
    default: "코인 선물 찍먹",
  },
  description:
    "Bitget 기반 코인 선물 마켓과 포지션 흐름을 체험하는 모의투자 플랫폼",
  keywords: ["코인", "선물", "모의투자", "비트겟", "레버리지"],
  icons: {
    icon: "/favicon.ico",
  },
};

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="kr">
      <body className={`${pretendard.variable} antialiased`}>
        <QueryClientProvider>
          <MSWProvider>
            <ToastContainer
              position="top-center"
              autoClose={1000}
              hideProgressBar={false}
              newestOnTop={false}
              closeOnClick
            />
            <SentryProvider>
              {children}
            </SentryProvider>
          </MSWProvider>
        </QueryClientProvider>
      </body>
    </html>
  );
}
