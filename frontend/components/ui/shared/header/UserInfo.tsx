"use client";

import React, { useRef, useState } from "react";
import EditInfo from "./EditInfo";
import useOutsideClick from "@/hooks/useOutsideClick";
import useSessionActivityRefresh from "@/hooks/useSessionActivityRefresh";
import { JwtToken } from "@/type/jwt";
import clsx from "clsx";
import { usePathname } from "next/navigation";

const UserInfo = ({
  token,
  children,
}: {
  token: JwtToken;
  children: React.ReactNode;
}) => {
  const [isOpenForm, setIsOpenForm] = useState(false);
  const loginFormRef = useRef<HTMLDivElement | null>(null);
  const pathname = usePathname();
  useSessionActivityRefresh(token.exp);

  useOutsideClick(loginFormRef, () => {
    setIsOpenForm(false);
  });

  if (pathname === "/") {
    return null;
  }

  return (
    <div className="relative size-fit">
      <div className="flex items-center gap-main">
        <button
          className="text-main-dark-gray hover:text-main-blue transition-colors duration-300"
          onClick={() => setIsOpenForm(!isOpenForm)}
        >
          <b className="underline">{token.nickname}</b> 님
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
            <span>닉네임</span>
            <span>{token.nickname}</span>

            <span>휴대폰</span>
            <span>{token.phoneNumber || "-"}</span>

            <span>이메일</span>
            <span>{token.email}</span>

            <span>집주소</span>
            <span>
              {token.zipCode || ""} {token.Address || ""} {token.AddressDetail || ""}
              {!(token.zipCode || token.Address || token.AddressDetail) && "-"}
            </span>
          </div>

          {children}
        </div>
      </div>
    </div>
  );
};

export default UserInfo;
