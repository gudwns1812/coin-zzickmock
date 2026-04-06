import Footer from "@/components/ui/shared/Footer";
import { Metadata } from "next";
import React from "react";

export const metadata: Metadata = {
  title: "내 포트폴리오",
  description: "보유 종목과 손익 흐름을 확인하는 포트폴리오 페이지",
};

const PortfolioMyLayout = ({ children }: { children: React.ReactNode }) => {
  return (
    <>
      <div className="min-w-[900px] mx-auto">{children}</div>
      <Footer />
    </>
  );
};

export default PortfolioMyLayout;
