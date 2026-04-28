import MyPageShell from "@/components/mypage/MyPageShell";
import type { ReactNode } from "react";

export default function MyPageLayout({ children }: { children: ReactNode }) {
  return <MyPageShell>{children}</MyPageShell>;
}
