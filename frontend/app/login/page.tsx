import { getAuthUser } from "@/lib/futures-api";
import { redirect } from "next/navigation";
import LoginFormClient from "./LoginFormClient";

export default async function LoginPage() {
  const authUser = await getAuthUser();

  if (authUser) {
    redirect("/markets");
  }

  return <LoginFormClient />;
}
