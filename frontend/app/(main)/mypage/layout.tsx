import MyPageShell from "@/components/mypage/MyPageShell";
import { getAuthUser } from "@/lib/futures-api";
import { getJwtToken } from "@/utils/auth";
import { redirect } from "next/navigation";
import type { ReactNode } from "react";

export default async function MyPageLayout({
  children,
}: {
  children: ReactNode;
}) {
  const token = await getJwtToken();
  if (!token) {
    redirect("/login");
  }

  const authUser = await getAuthUser();
  const isAdmin = authUser?.admin ?? token?.admin ?? token?.role === "ADMIN";

  return <MyPageShell isAdmin={isAdmin}>{children}</MyPageShell>;
}
