"use client";

import { JwtToken } from "@/type/jwt";
import React from "react";
import InterestStocks from "./InterestStocks";

const Interest = ({ token }: { token: JwtToken | null }) => {
  return (
    <div className="size-full">
      <InterestStocks token={token} />
    </div>
  );
};

export default Interest;
