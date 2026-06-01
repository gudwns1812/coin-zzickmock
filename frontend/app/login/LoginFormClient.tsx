"use client";

import PageReveal from "@/components/ui/shared/PageReveal";
import { createGoogleLoginUrl } from "@/lib/futures-auth-client";
import { Chrome, ShieldCheck } from "lucide-react";
import Link from "next/link";

export default function LoginFormClient() {
  const googleLoginUrl = createGoogleLoginUrl();

  return (
    <main className="min-h-screen min-w-[1000px] px-main-4 pb-main-8 pt-[118px] text-slate-800">
      <section className="mx-auto flex min-h-[calc(100vh-170px)] max-w-container-main-max items-center justify-center">
        <PageReveal
          className="h-fit w-full max-w-[560px] rounded-[22px] border border-main-light-gray bg-white px-[44px] py-[46px] shadow-[0_18px_36px_rgba(15,23,42,0.13)]"
          variant="auth"
        >
          <div className="flex size-[58px] items-center justify-center rounded-2xl bg-main-blue/10 text-main-blue">
            <ShieldCheck size={30} strokeWidth={2.4} />
          </div>

          <h1 className="mt-main-3 text-[36px] font-extrabold leading-tight text-slate-900">
            Google 계정으로 로그인
          </h1>
          <p className="mt-main text-xl-custom font-semibold leading-relaxed text-slate-600">
            코인 찌찍목은 Google 로그인을 기본 진입 방식으로 사용합니다.
            기존 계정 자산은 로그인 후 연결 흐름에서 그대로 이어갈 수 있습니다.
          </p>

          <a
            href={googleLoginUrl}
            className="mt-main-4 flex h-[60px] w-full items-center justify-center gap-main rounded-[14px] bg-main-blue px-4 py-[5px] text-xl-custom font-extrabold text-white shadow-[0_4px_8px_rgba(52,133,250,0.26)] transition-colors duration-400 hover:bg-[#2f74e8]"
          >
            <Chrome size={24} strokeWidth={2.4} />
            Google로 계속하기
          </a>

          <div className="mt-main-4 rounded-[16px] border border-slate-200 bg-slate-50 p-main-2 text-sm-custom font-semibold leading-relaxed text-slate-600">
            <p>처음 Google로 들어오면 기존 계정 연결 또는 새 계정 시작을 선택합니다.</p>
            <p className="mt-main">기존 계정 연결에는 예전 아이디와 비밀번호가 한 번만 필요합니다.</p>
          </div>

          <p className="mt-main-3 text-center text-sm-custom font-bold text-slate-500">
            회원가입도 Google 로그인 이후 진행합니다. {" "}
            <Link href="/auth/google/onboarding" className="text-main-blue hover:underline">
              진행 중인 가입 보기
            </Link>
          </p>
        </PageReveal>
      </section>
    </main>
  );
}
