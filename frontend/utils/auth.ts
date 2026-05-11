"use server";

import { cookies } from "next/headers";
import { JwtToken } from "@/type/jwt";
import { jwtDecode } from "jwt-decode";

type RawJwtToken = Partial<JwtToken> & {
  memberId?: number | string;
  memberName?: string;
};

export async function getJwtToken() {
  const cookieStore = await cookies();
  const token = cookieStore.get("accessToken")?.value;

  if (!token) return null;

  try {
    const decoded = jwtDecode<RawJwtToken>(token);
    const legacyAccount =
      typeof decoded.memberId === "string" ? decoded.memberId : "";
    return {
      ...decoded,
      memberId:
        typeof decoded.memberId === "number"
          ? decoded.memberId
          : Number(decoded.memberId) || 0,
      account: decoded.account ?? legacyAccount,
      nickname: decoded.nickname ?? decoded.memberName ?? decoded.account ?? legacyAccount,
    } as JwtToken;
  } catch (err) {
    cookieStore.set({
      name: "accessToken",
      value: "",
      maxAge: 0,
      path: "/",
    });
    return null;
  }
}
