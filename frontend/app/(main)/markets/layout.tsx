import Footer from "@/components/ui/shared/Footer";
import type { Metadata } from "next";
import React from "react";

export const metadata: Metadata = {
  title: {
    default: "마켓",
    template: "코인 선물 찍먹 | %s",
  },
  description: "Bitget 기반 BTCUSDT, ETHUSDT 선물 마켓을 살펴보는 공간",
};

export default function MarketsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <>
      <div className="flex flex-col gap-10 max-w-[1200px] mx-auto">{children}</div>
      <Footer />
    </>
  );
}

