"use client";

import { createGoogleLoginUrl } from "@/lib/futures-auth-client";
import { usePathname } from "next/navigation";

const LoginForm = () => {
  const pathname = usePathname();

  if (pathname === "/") {
    return null;
  }

  return (
    <a
      href={createGoogleLoginUrl()}
      className="inline-flex rounded-main bg-main-blue/80 px-4 py-[5px] font-medium text-white transition-colors duration-400 hover:bg-main-blue"
    >
      Google 로그인
    </a>
  );
};

export default LoginForm;
