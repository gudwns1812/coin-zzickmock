import { getJwtToken } from "@/utils/auth";
import { redirect } from "next/navigation";
import type { ReactNode } from "react";

export default async function AdminLayout({
  children,
}: {
  children: ReactNode;
}) {
  const token = await getJwtToken();
  if (!token) {
    redirect("/login");
  }

  return children;
}
