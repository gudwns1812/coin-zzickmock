import type { Metadata } from "next";
import localFont from "next/font/local";
import { ToastContainer } from "react-toastify";
import SentryProvider from "../components/router/SentryProvider";
import "./globals.css";
import "driver.js/dist/driver.css";
import QueryClientProvider from "../components/router/QueryClientProvider";
import InvestSurveyProvider from "@/components/router/InvestSurveyProvider";
import { getJwtToken } from "@/utils/auth";
import { MSWProvider } from "./MSWProvider";

const pretendard = localFont({
  src: "../public/fonts/PretendardVariable.woff2",
  display: "swap",
  weight: "100 900",
  variable: "--font-pretendard",
});

export const metadata: Metadata = {
  title: {
    template: "주식 찍먹 | %s",
    default: "주식 찍먹",
  },
  description:
    "주식 시세와 포트폴리오 흐름을 가볍게 확인할 수 있는 주식 플랫폼",
  keywords: ["주식", "투자", "증권", "포트폴리오", "모의투자"],
  icons: {
    icon: "/favicon.ico",
  },
};

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const token = await getJwtToken();

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
              <InvestSurveyProvider token={token}>
                {children}
              </InvestSurveyProvider>
            </SentryProvider>
          </MSWProvider>
        </QueryClientProvider>
      </body>
    </html>
  );
}
