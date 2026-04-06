"use client";

import React, { useRef, useState } from "react";
import EditInfo from "./EditInfo";
import useOutsideClick from "@/hooks/useOutsideClick";
import useTokenExpire from "@/hooks/useTokenExpire";
import { JwtToken } from "@/type/jwt";
import clsx from "clsx";
import { usePathname } from "next/navigation";
import { Clock, RefreshCcw } from "lucide-react";
import { toast } from "react-toastify";

const UserInfo = ({
  token,
  children,
}: {
  token: JwtToken;
  children: React.ReactNode;
}) => {
  const [exp, setExp] = useState(token.exp);
  const [isOpenForm, setIsOpenForm] = useState(false);
  const loginFormRef = useRef<HTMLDivElement | null>(null);
  const pathname = usePathname();

  useOutsideClick(loginFormRef, () => {
    setIsOpenForm(false);
  });

  if (pathname === "/") {
    return null;
  }

  const handleRefreshToken = async () => {
    const res = await fetch(`/proxy/auth/refresh`, {
      credentials: "include",
    });

    if (res.ok) {
      setExp(Math.floor(Date.now() / 1000) + 60 * 60);
      toast.success("로그인 만료시간이 갱신되었습니다");
    } else {
      toast.error("로그인 만료시간 갱신에 실패했습니다");
    }
  };

  return (
    <div className="relative size-fit">
      <div className="flex items-center gap-main">
        <button
          className="text-main-dark-gray hover:text-main-blue transition-colors duration-300"
          onClick={() => setIsOpenForm(!isOpenForm)}
        >
          <b className="underline">{token.memberName}</b> 님
        </button>
        <span className="flex items-center gap-1 text-main-dark-gray text-sm-custom w-[55px]">
          <Clock size={14} /> {useTokenExpire(exp)}
        </span>
        <button
          onClick={handleRefreshToken}
          className="flex items-center gap-1 text-main-dark-gray text-sm-custom hover:text-main-blue transition-colors duration-300"
        >
          <RefreshCcw size={14} />
          세션 초기화
        </button>
      </div>

      <div
        ref={loginFormRef}
        className={clsx(
          "absolute right-0 pt-2 duration-200 z-50",
          isOpenForm ? "block" : "hidden"
        )}
      >
        <div className="bg-white w-[350px] rounded-main shadow-color p-main-2 flex flex-col gap-main">
          <div className="flex items-center gap-main">
            <h2 className="text-main-dark-gray font-bold text-xl-custom">
              내 정보
            </h2>
            <EditInfo token={token} />
          </div>
          <div className="grid grid-cols-[auto_1fr] gap-y-main gap-x-main-2">
            <span>이름</span>
            <span>{token.memberName}</span>

            <span>휴대폰</span>
            <span>{token.phoneNumber}</span>

            <span>이메일</span>
            <span>{token.email}</span>

            <span>집주소</span>
            <span>
              {token.zipCode} {token.Address} {token.AddressDetail}
            </span>
          </div>

          {children}
        </div>
      </div>
    </div>
  );
};

export default UserInfo;
