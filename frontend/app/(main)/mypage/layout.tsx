import BackendAuthGate from "@/components/router/BackendAuthGate";
import MyPageShell from "@/components/mypage/MyPageShell";
import type { ReactNode } from "react";

export default async function MyPageLayout({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <BackendAuthGate>
      <MyPageShell isAdmin={false}>{children}</MyPageShell>
    </BackendAuthGate>
  );
}
