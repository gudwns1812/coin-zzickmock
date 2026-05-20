import BackendAuthGate from "@/components/router/BackendAuthGate";
import ProtectedPageSkeleton from "@/components/ui/shared/ProtectedPageSkeleton";
import type { ReactNode } from "react";

export default async function AdminLayout({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <BackendAuthGate requireAdmin fallback={<ProtectedPageSkeleton variant="admin" />}>
      {children}
    </BackendAuthGate>
  );
}
