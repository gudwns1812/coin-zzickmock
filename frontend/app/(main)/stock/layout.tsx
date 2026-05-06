import { Metadata } from "next";
import { redirect } from "next/navigation";

export const metadata: Metadata = {
  title: {
    default: "증권",
    template: "주식 찍먹 | %s",
  },
  description: "주식 시세와 시장 흐름을 확인하는 증권 페이지",
};

const StockLayout = () => {
  redirect("/markets");
};

export default StockLayout;
