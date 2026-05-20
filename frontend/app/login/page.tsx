import Header from "@/components/ui/shared/header/Header";
import LoginFormClient from "./LoginFormClient";

export default async function LoginPage() {
  return (
    <div className="min-h-screen overflow-x-auto bg-gradient-to-br from-[#fbfdff] via-[#f6f9ff] to-[#eef5ff]">
      <Header />
      <LoginFormClient />
    </div>
  );
}
