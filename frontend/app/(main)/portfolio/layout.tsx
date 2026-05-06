import { Metadata } from "next";
import { redirect } from "next/navigation";

export const metadata: Metadata = {
  title: "선물 계정",
  description: "포지션, 증거금, 손익 흐름을 확인하는 선물 계정 페이지",
};

const PortfolioMyLayout = () => {
  redirect("/mypage");
};

export default PortfolioMyLayout;
