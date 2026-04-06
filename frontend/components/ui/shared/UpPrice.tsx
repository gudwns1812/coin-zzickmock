import { Triangle } from "lucide-react";
import React from "react";

const UpPrice = ({
  change,
  changeRate,
}: {
  change: number | string;
  changeRate?: number;
}) => {
  return (
    <span className="text-main-red font-medium flex items-center gap-1 text-sm-custom">
      <Triangle fill="#f04251" className="text-main-red" size={12} />
      {change} {changeRate && `(${changeRate}%)`}
    </span>
  );
};

export default UpPrice;
