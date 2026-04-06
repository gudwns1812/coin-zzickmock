import clsx from "clsx";
import React from "react";

const MyProfit = ({
  title,
  profit,
}: {
  title: string;
  profit: number | null;
}) => {
  if (profit === null)
    return (
      <div className="size-full flex flex-col gap-main justify-around rounded-main p-main bg-white">
        <h2 className="text-md font-bold bg-gradient-to-r from-main-blue to-purple-600 bg-clip-text text-transparent w-fit text-center">
          {title}
        </h2>
        <div>
          <span className={clsx("text-xl-custom font-bold text-main-red")}>
            {Number(135980).toLocaleString()}
          </span>
          <span className="text-sm-custom text-gray-500 font-semibold">
            {" "}
            원
          </span>
        </div>
      </div>
    );

  return (
    <div className="size-full flex flex-col gap-main justify-around rounded-main p-main bg-white">
      <h2 className="text-md font-bold bg-gradient-to-r from-main-blue to-purple-600 bg-clip-text text-transparent w-fit text-center">
        {title}
      </h2>
      <div>
        <span
          className={clsx(
            "text-xl-custom font-bold",
            profit > 0 ? "text-main-red" : "text-main-blue"
          )}
        >
          {profit > 0 ? "+" : ""}
          {profit.toLocaleString()}
        </span>
        <span className="text-sm-custom text-gray-500 font-semibold"> 원</span>
      </div>
    </div>
  );
};

export default MyProfit;
