"use client";

import Button from "@/components/ui/shared/Button";
import Input from "@/components/ui/shared/Input";
import PageReveal from "@/components/ui/shared/PageReveal";
import {
  completeGoogleSignup,
  createGoogleLoginUrl,
  getGoogleOnboardingState,
  linkGoogleToExistingAccount,
} from "@/lib/futures-auth-client";
import { notifyFuturesAuthChanged } from "@/lib/futures-auth-state";
import { useQuery } from "@tanstack/react-query";
import { Link2, UserPlus } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { toast } from "react-toastify";

type Mode = "link" | "signup";

export default function GoogleOnboardingPage() {
  const router = useRouter();
  const [mode, setMode] = useState<Mode>("link");
  const [account, setAccount] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [nickname, setNickname] = useState("");
  const [email, setEmail] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [agreement, setAgreement] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const onboardingQuery = useQuery({
    queryKey: ["google-onboarding-state"],
    queryFn: async () => {
      const response = await getGoogleOnboardingState();
      if (!response.ok || !response.data?.active) {
        throw new Error("pending-google-onboarding-not-found");
      }
      return response.data;
    },
    retry: false,
  });

  useEffect(() => {
    if (!onboardingQuery.data) {
      return;
    }
    if (!email && onboardingQuery.data.emailHint) {
      setEmail(onboardingQuery.data.emailHint);
    }
    if (!name && onboardingQuery.data.nameHint) {
      setName(onboardingQuery.data.nameHint);
    }
  }, [email, name, onboardingQuery.data]);

  const onboardingInfoText = onboardingQuery.isLoading
    ? "Google 인증 상태를 확인하는 중입니다."
    : onboardingQuery.isError || !onboardingQuery.data
      ? "인증 상태가 만료되었습니다. 로그인 화면에서 다시 시작해주세요."
      : `Google 계정 정보: ${onboardingQuery.data.emailHint ?? onboardingQuery.data.nameHint ?? "확인됨"}`;

  const finishLogin = () => {
    notifyFuturesAuthChanged("login");
    router.push("/markets");
    router.refresh();
  };

  const handleLink = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      const response = await linkGoogleToExistingAccount({ account, password });
      if (!response.ok) {
        toast.error("기존 계정 정보를 확인해주세요.");
        return;
      }
      toast.success("기존 계정이 Google 로그인에 연결되었습니다.");
      finishLogin();
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSignup = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      const response = await completeGoogleSignup({
        name,
        nickname,
        email,
        phoneNumber,
        agreement,
      });
      if (!response.ok) {
        toast.error("입력값을 확인해주세요.");
        return;
      }
      toast.success("Google 계정으로 새 모의투자 계정이 생성되었습니다.");
      finishLogin();
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="min-h-screen min-w-[1000px] px-main-4 pb-main-8 pt-[118px] text-slate-800">
      <section className="mx-auto flex min-h-[calc(100vh-170px)] max-w-container-main-max items-center justify-center">
        <PageReveal
          className="h-fit w-full max-w-[760px] rounded-[22px] border border-main-light-gray bg-white px-[44px] py-[42px] shadow-[0_18px_36px_rgba(15,23,42,0.13)]"
          variant="auth"
        >
          <h1 className="text-[32px] font-extrabold leading-tight text-slate-900">
            Google 계정 연결
          </h1>
          <p className="mt-main text-lg-custom font-semibold leading-relaxed text-slate-600">
            기존 모의투자 자산을 이어가려면 기존 계정을 연결하고, 처음이면 새 계정으로 시작하세요.
          </p>
          <div className="mt-main-3 rounded-main bg-slate-50 p-main-2 text-sm-custom font-semibold text-slate-600">
            {onboardingInfoText}
          </div>

          {onboardingQuery.isLoading ? (
            <div className="mt-main-4 h-[360px] rounded-[18px] bg-slate-100" aria-hidden="true" />
          ) : onboardingQuery.isError ? (
            <div className="mt-main-4 rounded-[18px] border border-slate-200 bg-slate-50 p-main-3 text-center">
              <p className="text-base-custom font-bold text-slate-700">
                진행 중인 Google 가입 요청이 없습니다.
              </p>
              <a
                href={createGoogleLoginUrl()}
                className="mt-main-2 inline-flex rounded-main bg-main-blue/80 px-4 py-[5px] font-medium text-white transition-colors duration-400 hover:bg-main-blue"
              >
                Google 로그인부터 시작
              </a>
            </div>
          ) : (
            <>
              <div className="mt-main-4 grid grid-cols-2 gap-main">
                <button
                  type="button"
                  onClick={() => setMode("link")}
                  className={`rounded-[16px] border p-main-2 text-left transition ${
                    mode === "link" ? "border-main-blue bg-main-blue/10" : "border-slate-200 bg-white"
                  }`}
                >
                  <Link2 className="mb-main text-main-blue" />
                  <p className="text-lg-custom font-extrabold">기존 계정 연결</p>
                  <p className="mt-main text-sm-custom font-semibold text-slate-600">
                    예전 아이디와 비밀번호로 자산을 그대로 이어갑니다.
                  </p>
                </button>
                <button
                  type="button"
                  onClick={() => setMode("signup")}
                  className={`rounded-[16px] border p-main-2 text-left transition ${
                    mode === "signup" ? "border-main-blue bg-main-blue/10" : "border-slate-200 bg-white"
                  }`}
                >
                  <UserPlus className="mb-main text-main-blue" />
                  <p className="text-lg-custom font-extrabold">새 계정으로 시작</p>
                  <p className="mt-main text-sm-custom font-semibold text-slate-600">
                    프로필 정보를 입력하고 100000 USDT로 시작합니다.
                  </p>
                </button>
              </div>

              {mode === "link" ? (
                <form className="mt-main-4 grid gap-main-2" onSubmit={handleLink}>
                  <Input id="legacy-account" placeholder="기존 아이디" value={account} onChange={(event) => setAccount(event.target.value)} />
                  <Input id="legacy-password" type="password" placeholder="기존 비밀번호" value={password} onChange={(event) => setPassword(event.target.value)} hasShowButton />
                  <Button type="submit" variant="primary" className="h-[54px]" disabled={isSubmitting}>
                    기존 계정 연결
                  </Button>
                </form>
              ) : (
                <form className="mt-main-4 grid grid-cols-2 gap-main-2" onSubmit={handleSignup}>
                  <Input id="google-name" placeholder="이름" value={name} onChange={(event) => setName(event.target.value)} />
                  <Input id="google-nickname" placeholder="닉네임" value={nickname} onChange={(event) => setNickname(event.target.value)} />
                  <Input
                    id="google-email"
                    placeholder="Google 계정 이메일"
                    value={email}
                    readOnly
                    disabled
                    aria-readonly="true"
                    className="cursor-not-allowed bg-slate-50 text-slate-500"
                  />
                  <Input id="google-phone" placeholder="휴대폰 번호" value={phoneNumber} onChange={(event) => setPhoneNumber(event.target.value)} />
                  <p className="col-span-2 text-xs-custom font-semibold text-slate-500">
                    이메일은 인증한 Google 계정 기준으로 고정되며 변경할 수 없습니다.
                  </p>
                  <label className="col-span-2 flex items-center gap-main text-sm-custom font-bold text-slate-700">
                    <input type="checkbox" checked={agreement} onChange={(event) => setAgreement(event.target.checked)} />
                    모의투자 서비스 이용과 리스크 안내에 동의합니다.
                  </label>
                  <Button type="submit" variant="primary" className="col-span-2 h-[54px]" disabled={isSubmitting}>
                    새 계정으로 시작
                  </Button>
                </form>
              )}
            </>
          )}
        </PageReveal>
      </section>
    </main>
  );
}
