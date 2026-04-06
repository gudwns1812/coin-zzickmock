import React from "react";
import clsx from "clsx";

interface TooltipProps {
  message: string;
  position?: "top" | "bottom" | "left" | "right";
  icon: React.ReactNode;
}

const Tooltip: React.FC<TooltipProps> = ({
  message,
  position = "top",
  icon,
}) => {
  const basePosition =
    "absolute opacity-0 peer-hover:opacity-100 transition-opacity duration-300 whitespace-nowrap text-white text-sm-custom bg-black/80 rounded-main px-main py-1 z-50";

  const positionClasses = {
    top: "bottom-full left-1/2 -translate-x-1/2 mb-2",
    bottom: "top-full left-1/2 -translate-x-1/2 mt-2",
    left: "top-1/2 right-full -translate-y-1/2 mr-2",
    right: "top-1/2 left-full -translate-y-1/2 ml-2",
  };

  return (
    <div className="relative inline-block">
      <div className="peer text-main-dark-gray/50 hover:text-main-blue transition-colors duration-300 cursor-pointer">
        {icon}
      </div>
      <div className={clsx(basePosition, positionClasses[position])}>
        <p>{message}</p>
      </div>
    </div>
  );
};

export default Tooltip;
