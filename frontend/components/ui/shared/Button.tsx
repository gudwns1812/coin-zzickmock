"use client";
import clsx from "clsx";
import React from "react";

type ButtonProps = {
  children: React.ReactNode;
  variant?: "primary" | "ghost" | "danger";
  className?: string;
  onClick?: () => void;
  type?: "button" | "submit" | "reset";
  disabled?: boolean;
  id?: string;
};

const Button = ({
  children,
  variant = "primary",
  className,
  id,
  ...props
}: ButtonProps) => {
  const baseStyles =
    "px-4 py-[5px] rounded-main transition-colors duration-400 font-medium";

  const variantStyles = {
    primary: "bg-main-blue/80 text-white hover:bg-main-blue",
    ghost: "bg-transparent text-main-blue hover:bg-main-blue/10",
    danger: "bg-main-red/80 text-white hover:bg-main-red",
  };

  return (
    <button
      className={clsx(baseStyles, variantStyles[variant], className)}
      id={id}
      {...props}
    >
      {children}
    </button>
  );
};

export default Button;
