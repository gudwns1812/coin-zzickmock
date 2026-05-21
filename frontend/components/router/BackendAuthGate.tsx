"use client";

import PageReveal from "@/components/ui/shared/PageReveal";
import { useFuturesAuthUser } from "@/hooks/useFuturesAuthUser";
import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";

export default function BackendAuthGate({
  children,
  requireAdmin = false,
}: {
  children: ReactNode;
  requireAdmin?: boolean;
}) {
  const router = useRouter();
  const authQuery = useFuturesAuthUser();
  const authUser = authQuery.data ?? null;
  const isChecking = authQuery.isLoading;
  const isAllowed = requireAdmin ? authUser?.admin === true : Boolean(authUser);

  useEffect(() => {
    if (!isChecking && !isAllowed) {
      router.replace("/login");
    }
  }, [isAllowed, isChecking, router]);

  if (!isChecking && !isAllowed) {
    return null;
  }

  return <PageReveal>{children}</PageReveal>;
}
