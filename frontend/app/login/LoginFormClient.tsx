"use client";

import Button from "@/components/ui/shared/Button";
import Input from "@/components/ui/shared/Input";
import PageReveal from "@/components/ui/shared/PageReveal";
import { notifyFuturesAuthChanged } from "@/lib/futures-auth-state";
import { loginToFutures } from "@/lib/futures-auth-client";
import { Lock, Mail } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "react-toastify";

export default function LoginFormClient() {
  const router = useRouter();
  const [account, setAccount] = useState("");
  const [password, setPassword] = useState("");
  const [shouldRememberLogin, setShouldRememberLogin] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!account || !password) {
      toast.error("아이디와 비밀번호를 입력해주세요.");
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await loginToFutures({ account, password });

      if (!response.ok) {
        toast.error("로그인에 실패했습니다.");
        return;
      }

      toast.success("로그인되었습니다.");
      notifyFuturesAuthChanged("login");
      router.push("/markets");
      router.refresh();
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="min-h-screen min-w-[1000px] px-main-4 pb-main-8 pt-[118px] text-slate-800">
      <section className="mx-auto flex min-h-[calc(100vh-170px)] max-w-container-main-max items-center justify-center">
        <PageReveal
          className="h-fit w-full max-w-[530px] rounded-[22px] border border-main-light-gray bg-white px-[44px] py-[46px] shadow-[0_18px_36px_rgba(15,23,42,0.13)]"
          variant="auth"
        >
          <h1 className="text-[36px] font-extrabold leading-tight text-slate-900">
            로그인
          </h1>
          <p className="mt-main text-xl-custom font-semibold text-slate-600">
            포트폴리오 및 시장 현황을 확인하세요
          </p>

          <form
            className="mt-main-4 flex flex-col gap-main-2"
            onSubmit={handleSubmit}
          >
            <div>
              <label
                htmlFor="account"
                className="mb-main block text-base-custom font-bold text-slate-700"
              >
                이메일
              </label>
              <Input
                id="account"
                type="text"
                placeholder="your@email.com"
                value={account}
                onChange={(event) => setAccount(event.target.value)}
                leftIcon={<Mail size={22} strokeWidth={2.2} />}
                className="h-[60px] rounded-[14px] border-slate-200 bg-[#f8fafc] text-lg-custom font-semibold text-slate-800 placeholder:text-slate-500/80 focus:bg-white"
                autoComplete="username"
              />
            </div>

            <div>
              <label
                htmlFor="password"
                className="mb-main block text-base-custom font-bold text-slate-700"
              >
                비밀번호
              </label>
              <Input
                id="password"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                leftIcon={<Lock size={23} strokeWidth={2.2} />}
                hasShowButton
                className="h-[60px] rounded-[14px] border-slate-200 bg-[#f8fafc] text-lg-custom font-semibold tracking-[0.14em] text-slate-800 placeholder:text-slate-500/80 focus:bg-white"
                autoComplete="current-password"
              />
            </div>

            <div className="flex items-center justify-between text-sm-custom font-bold text-slate-700">
              <label className="flex cursor-pointer select-none items-center gap-main">
                <input
                  type="checkbox"
                  checked={shouldRememberLogin}
                  onChange={(event) =>
                    setShouldRememberLogin(event.target.checked)
                  }
                  className="size-[20px] appearance-none rounded-[5px] border-2 border-slate-400 bg-white transition-colors checked:border-main-blue checked:bg-main-blue focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-main-blue/30"
                />
                로그인 상태 유지
              </label>
              <Link href="/login" className="text-main-blue hover:underline">
                비밀번호 찾기
              </Link>
            </div>

            <Button
              type="submit"
              variant="primary"
              className="h-[60px] w-full rounded-[14px] bg-main-blue text-xl-custom font-extrabold shadow-[0_4px_8px_rgba(52,133,250,0.26)] hover:bg-[#2f74e8] disabled:cursor-not-allowed disabled:bg-[#7fa2f4]"
              disabled={isSubmitting}
            >
              {isSubmitting ? "로그인 중..." : "로그인"}
            </Button>
          </form>

          <div className="mt-main-3 flex items-center gap-main-2 text-sm-custom font-bold text-slate-500">
            <span className="h-px flex-1 bg-slate-200" />
            <span>또는</span>
            <span className="h-px flex-1 bg-slate-200" />
          </div>

          <p className="mt-main-3 text-center text-base-custom font-bold text-slate-600">
            계정이 없으신가요?{" "}
            <Link href="/signup" className="text-main-blue hover:underline">
              회원가입
            </Link>
          </p>
        </PageReveal>
      </section>

      <button
        type="button"
        className="fixed bottom-[27px] right-[25px] flex size-[57px] items-center justify-center rounded-full bg-white text-[34px] font-medium leading-none text-slate-900 shadow-[0_3px_14px_rgba(15,23,42,0.25)] transition-transform hover:-translate-y-0.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-main-blue/40"
        aria-label="도움말"
      >
        ?
      </button>
    </main>
  );
}
