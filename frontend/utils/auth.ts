"use server";

import { cookies } from "next/headers";
// import jwt from "jsonwebtoken";
import { JwtToken } from "@/type/jwt";
import { jwtDecode } from "jwt-decode";

const SECRET = process.env.JWT_SECRET!;

export async function getJwtToken() {
  const cookieStore = await cookies();
  const token = cookieStore.get("accessToken")?.value;

  if (!token) return null;

  try {
    // const decoded = jwt.verify(token, SECRET);
    const decoded: JwtToken = jwtDecode(token);
    return decoded;
  } catch (err) {
    console.error("유효하지 않은 토큰입니당:", err);
    cookieStore.set({
      name: "accessToken",
      value: "",
      maxAge: 0,
      path: "/",
    });
    return null;
  }
}
