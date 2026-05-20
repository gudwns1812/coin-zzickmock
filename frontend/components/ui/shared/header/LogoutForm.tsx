"use client";
import React, { useState } from "react";
import { toast } from "react-toastify";
import { useRouter } from "next/navigation";
import { logoutFromFutures } from "@/lib/futures-auth-client";
import { notifyFuturesAuthChanged } from "@/lib/futures-auth-state";

const LogoutForm = ({ onLoggedOut }: { onLoggedOut: () => void }) => {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleLogout = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    setIsSubmitting(true);

    try {
      const succeeded = await logoutFromFutures();

      if (!succeeded) {
        toast.error("로그아웃에 실패했습니다");
        return;
      }

      onLoggedOut();
      notifyFuturesAuthChanged("logout");
      router.refresh();
      toast.success("로그아웃 되었습니다");
    } catch {
      toast.error("로그아웃에 실패했습니다");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleLogout}>
      <button
        type="submit"
        className="w-full bg-main-red text-white py-2 px-4 rounded-main font-medium text-sm-custom"
        disabled={isSubmitting}
      >
        로그아웃
      </button>
    </form>
  );
};

export default LogoutForm;
