import React from "react";
import Header from "@/components/ui/shared/header/Header";

export default async function MainLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="w-screen h-screen relative flex flex-col overflow-x-scroll bg-white">
      <Header />
      <main
        id="main-layout"
        className="flex-1 overflow-y-scroll pt-[78px] flex flex-col justify-between bg-white"
      >
        <div className="grow shrink-0 min-w-[1000px]">{children}</div>
      </main>
    </div>
  );
}
