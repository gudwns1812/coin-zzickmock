import BackendAuthGate from "@/components/router/BackendAuthGate";
import MyPageShell from "@/components/mypage/MyPageShell";
import ProtectedPageSkeleton from "@/components/ui/shared/ProtectedPageSkeleton";
import type { ReactNode } from "react";

export default async function MyPageLayout({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <BackendAuthGate
      fallback={
        <MyPageShell isAdmin={false}>
          <ProtectedPageSkeleton variant="mypage" />
        </MyPageShell>
      }
    >
      <MyPageShell isAdmin={false}>{children}</MyPageShell>
    </BackendAuthGate>
  );
}
