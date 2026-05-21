import BackendAuthGate from "@/components/router/BackendAuthGate";
import type { ReactNode } from "react";

export default async function AdminLayout({
  children,
}: {
  children: ReactNode;
}) {
  return (
    <BackendAuthGate requireAdmin>
      {children}
    </BackendAuthGate>
  );
}
