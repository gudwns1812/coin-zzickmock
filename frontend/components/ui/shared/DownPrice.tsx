import { Triangle } from "lucide-react";
import React from "react";

const DownPrice = ({
  change,
  changeRate,
}: {
  change?: number | string;
  changeRate?: number;
}) => {
  return (
    <span className="text-main-blue font-medium flex items-center gap-1 text-sm-custom">
      <Triangle
        fill="#3485fa"
        className="text-main-blue rotate-180"
        size={12}
      />
      {change} {changeRate && `(${changeRate}%)`}
    </span>
  );
};

export default DownPrice;
