import { Metadata } from "next";
import React from "react";

export const metadata: Metadata = {
  title: "회원가입",
  description: "코인 선물 찍먹 회원가입 페이지",
};

const SignupLayout = ({ children }: { children: React.ReactNode }) => {
  return <>{children}</>;
};

export default SignupLayout;
