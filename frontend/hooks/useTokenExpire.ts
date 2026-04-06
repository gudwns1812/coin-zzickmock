import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "react-toastify";

function useTokenExpire(exp?: number) {
  const [expireText, setExpireText] = useState("00:00");
  const router = useRouter();

  useEffect(() => {
    if (!exp) {
      setExpireText("토큰 정보 없음");
      return;
    }
    const interval = setInterval(() => {
      const now = Math.floor(Date.now() / 1000);
      const diff = exp - now;
      if (diff <= 0) {
        setExpireText("");
        toast.error("로그인 시간이 만료되었습니다");
        clearInterval(interval);
        router.refresh();
      } else {
        const min = Math.floor(diff / 60);
        const sec = diff % 60;
        setExpireText(
          `${min.toString().padStart(2, "0")}:${sec
            .toString()
            .padStart(2, "0")}`
        );
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [exp, router]);

  return expireText;
}

export default useTokenExpire;
