import Sidebar from "@/components/ui/Sidebar";
import React from "react";
import Header from "@/components/ui/shared/header/Header";
import { getJwtToken } from "@/utils/auth";

export default async function MainLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const token = await getJwtToken();

  return (
    <div className="w-screen h-screen flex">
      <div className="flex-1 rounded-r-main relative flex flex-col overflow-x-scroll bg-white">
        <Header />
        <div
          id="main-layout"
          className="flex-1 overflow-y-scroll pt-[78px] flex flex-col justify-between bg-white"
        >
          <div className="grow shrink-0 min-w-[1000px]">{children}</div>
        </div>
      </div>
      <Sidebar token={token} />
    </div>
  );
}
