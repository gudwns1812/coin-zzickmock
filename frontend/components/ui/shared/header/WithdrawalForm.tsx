"use client";
import React, { useState } from "react";
import { toast } from "react-toastify";
import { useFormStatus } from "react-dom";
import { JwtToken } from "@/type/jwt";
import Modal from "../../Modal";
import { useRouter } from "next/navigation";

const WithdrawalForm = ({
  action,
  token,
}: {
  action: (formData: FormData) => void;
  token: JwtToken;
}) => {
  const { pending } = useFormStatus();
  const [isOpenModal, setIsOpenModal] = useState(false);
  const router = useRouter();

  const handleWithdrawal = async (formData: FormData) => {
    const res = await fetch("/proxy/auth/withdraw", {
      method: "DELETE",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        memberId: token.memberId,
      }),
    });

    if (res.ok) {
      action(formData);
      toast.success("탈퇴 되었습니다", { delay: 500 });
      setIsOpenModal(false);
      router.refresh();
    } else {
      setIsOpenModal(false);
      toast.error("탈퇴에 실패했습니다");
    }
  };

  return (
    <>
      <div className="w-full flex justify-center">
        <button
          onClick={() => setIsOpenModal(true)}
          className="w-fit text-sm underline text-main-dark-gray"
          disabled={pending}
        >
          탈퇴하기
        </button>
      </div>

      <Modal
        isOpen={isOpenModal}
        onClose={() => setIsOpenModal(false)}
        hasCloseButton={false}
        isClickOutsideClose={false}
      >
        <form action={handleWithdrawal} className="flex flex-col gap-main">
          <div className="flex flex-col items-center py-main">
            <p className="text-lg text-main-dark-gray">
              탈퇴하시면 모든 데이터가 삭제됩니다.
            </p>
            <p className="text-lg text-main-dark-gray">
              정말 탈퇴하시겠습니까?
            </p>
          </div>
          <div className="flex gap-main">
            <button
              onClick={() => setIsOpenModal(false)}
              className="w-full bg-main-blue/20 text-main-blue px-4 rounded-[10px] py-[5px]"
            >
              취소
            </button>
            <button
              className="w-full bg-main-blue text-white px-4 rounded-[10px] py-[5px]"
              type="submit"
              disabled={pending}
            >
              탈퇴하기
            </button>
          </div>
        </form>
      </Modal>
    </>
  );
};

export default WithdrawalForm;
