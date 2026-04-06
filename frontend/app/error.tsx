"use client";

import React from "react";
import { useRouter } from "next/navigation";

const Error = () => {
  const router = useRouter();

  return (
    <div className="w-full h-screen flex flex-col justify-center items-center bg-gradient-to-br from-red-100 via-yellow-100 to-pink-100 text-center px-4">
      <h1 className="text-[8rem] font-extrabold text-main-red leading-none animate-pulse drop-shadow-lg">
        500
      </h1>
      <h2 className="text-2xl-custom md:text-3xl-custom font-semibold text-gray-700 mt-4">
        알 수 없는 오류가 발생했어요
      </h2>
      <p className="text-gray-500 mt-2 mb-6">
        서버에 문제가 생겼거나, 잠시 후 다시 시도해주세요.
      </p>

      <div className="flex gap-4">
        <button
          onClick={() => router.refresh()}
          className="px-6 py-3 bg-main-red text-white rounded-xl shadow-md hover:bg-main-red/80 transition-all duration-300"
        >
          다시 시도하기
        </button>
        <button
          onClick={() => router.push("/stock")}
          className="px-6 py-3 bg-gray-300 text-gray-800 rounded-xl shadow-md hover:bg-gray-400 transition-all duration-300"
        >
          홈으로
        </button>
      </div>

      {/* <div className="mt-10 opacity-80">
        <img
          src="/error-illustration.svg"
          alt="error illustration"
          className="w-60 mx-auto animate-fade-in"
        />
      </div> */}
    </div>
  );
};

export default Error;
