import Footer from "@/components/ui/shared/Footer";
import { Metadata } from "next";
import React from "react";

export const metadata: Metadata = {
  title: {
    default: "증권",
    template: "주식 찍먹 | %s",
  },
  description: "주식 시세와 시장 흐름을 확인하는 증권 페이지",
};

const StockLayout = ({ children }: { children: React.ReactNode }) => {
  return (
    <>
      <div className="flex flex-col gap-[40px] max-w-[1200px] mx-auto">
        {children}
      </div>
      <Footer />
    </>
  );
};

export default StockLayout;
