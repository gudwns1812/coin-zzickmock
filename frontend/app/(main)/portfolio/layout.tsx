import Footer from "@/components/ui/shared/Footer";
import { Metadata } from "next";
import React from "react";

export const metadata: Metadata = {
  title: "선물 계정",
  description: "포지션, 증거금, 손익 흐름을 확인하는 선물 계정 페이지",
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
