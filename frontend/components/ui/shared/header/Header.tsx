import React from "react";
import Link from "next/link";
import { getJwtToken } from "@/utils/auth";
import LoginForm from "./LoginForm";
import { cookies } from "next/headers";
import UserInfo from "./UserInfo";
import LogoutForm from "./LogoutForm";
import Navigation from "./Navigation";
import WithdrawalForm from "./WithdrawalForm";
import { TrendingUp } from "lucide-react";

const Header = async () => {
  const token = await getJwtToken();

  const handleLogout = async () => {
    "use server";

    const cookieStore = await cookies();
    cookieStore.delete("accessToken");
  };

  return (
    <header className="absolute w-full py-main px-main-2 z-50 backdrop-blur-sm min-w-[1200px]">
      <div className="w-full flex relative gap-5 justify-between items-center">
        <div className="font-bold text-lg-custom flex items-center gap-2">
          <Link
            href="/markets"
            className="size-[40px] rounded-main bg-main-blue text-white flex items-center justify-center"
            aria-label="코인 선물 찍먹 홈"
          >
            <TrendingUp size={20} />
          </Link>
          <div className="flex flex-col">
            <span className="font-bold text-lg-custom">코인 선물 찍먹</span>
            <span className="text-sm-custom text-main-dark-gray">
              Bitget 기반 선물 마켓과 포지션 흐름을 연습하는 공간
            </span>
          </div>
        </div>

        <Navigation />

        {!token && <LoginForm />}

        {token && (
          <UserInfo token={token}>
            <LogoutForm action={handleLogout} />
            <WithdrawalForm action={handleLogout} token={token} />
          </UserInfo>
        )}
      </div>
    </header>
  );
};

export default Header;
