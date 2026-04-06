"use client";

import clsx from "clsx";
import { usePathname, useRouter } from "next/navigation";
import React, { useRef, useState } from "react";
import { toast } from "react-toastify";
import Input from "../Input";
import Link from "next/link";
import useOutsideClick from "@/hooks/useOutsideClick";
import Button from "../Button";

const LoginForm = () => {
  const [isOpenForm, setIsOpenForm] = useState(false);
  const loginFormRef = useRef<HTMLDivElement | null>(null);
  const [account, setAccount] = useState("");
  const [password, setPassword] = useState("");
  const router = useRouter();
  const pathname = usePathname();

  useOutsideClick(loginFormRef, () => {
    setIsOpenForm(false);
  });

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!account || !password) {
      toast.error("아이디 또는 비밀번호를 입력해주세요");
      return;
    }

    const res = await fetch(`/proxy/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
      body: JSON.stringify({ account, password }),
    });

    if (res.ok) {
      toast.success("로그인에 성공했습니다");
      router.refresh();
    } else {
      toast.error("아이디 / 비밀번호가 일치하지 않습니다");
    }
  };

  if (pathname === "/") {
    return null;
  }

  return (
    <div className="relative size-fit">
      <Button
        onClick={() => {
          setIsOpenForm(!isOpenForm);
        }}
        variant="primary"
      >
        로그인
      </Button>

      <div
        ref={loginFormRef}
        className={clsx(
          "absolute right-0 pt-2 duration-200 z-50",
          isOpenForm ? "block" : "hidden"
        )}
      >
        <form
          className="w-[300px] bg-white rounded-main shadow-color p-main-2 flex flex-col gap-main items-center"
          onSubmit={handleSubmit}
        >
          <Input
            id="account"
            type="text"
            placeholder="아이디"
            value={account}
            onChange={(e) => setAccount(e.target.value)}
          />

          <Input
            id="password"
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            hasShowButton={true}
          />

          <Button type="submit" variant="primary" className="w-full">
            로그인
          </Button>

          <Link href="/signup" className="text-xs-custom">
            회원가입
          </Link>
        </form>
      </div>
    </div>
  );
};

export default LoginForm;
