import MyPageShell from "@/components/mypage/MyPageShell";
import { getAuthUser } from "@/lib/futures-api";
import { getJwtToken } from "@/utils/auth";
import type { ReactNode } from "react";

export default async function MyPageLayout({
  children,
}: {
  children: ReactNode;
}) {
  const [authUser, token] = await Promise.all([getAuthUser(), getJwtToken()]);
  const isAdmin = authUser?.admin ?? token?.admin ?? token?.role === "ADMIN";

  return <MyPageShell isAdmin={isAdmin}>{children}</MyPageShell>;
}
