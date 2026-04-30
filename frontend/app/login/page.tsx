"use client";

import Button from "@/components/ui/shared/Button";
import Input from "@/components/ui/shared/Input";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "react-toastify";

export default function LoginPage() {
  const router = useRouter();
  const [account, setAccount] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!account || !password) {
      toast.error("아이디와 비밀번호를 입력해주세요.");
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await fetch(`/proxy/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({ account, password }),
      });

      if (!response.ok) {
        toast.error("로그인에 실패했습니다.");
        return;
      }

      toast.success("로그인되었습니다.");
      router.push("/markets");
      router.refresh();
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="min-h-screen flex items-center justify-center bg-main-light-gray/40 px-main">
      <div className="w-full max-w-[480px] rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray">
        <p className="text-sm-custom text-main-dark-gray/60">Login</p>
        <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
          코인 선물 계정 로그인
        </h1>

        <form className="mt-8 flex flex-col gap-main" onSubmit={handleSubmit}>
          <Input
            id="account"
            type="text"
            placeholder="아이디"
            value={account}
            onChange={(event) => setAccount(event.target.value)}
          />
          <Input
            id="password"
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            hasShowButton
          />

          <Button type="submit" variant="primary" className="w-full">
            {isSubmitting ? "로그인 중..." : "로그인"}
          </Button>
        </form>

        <div className="mt-6 flex items-center justify-between text-sm-custom text-main-dark-gray/70">
          <Link href="/signup" className="text-main-blue font-semibold">
            회원가입
          </Link>
          <Link href="/markets" className="font-semibold">
            마켓 먼저 보기
          </Link>
        </div>
      </div>
    </main>
  );
}
