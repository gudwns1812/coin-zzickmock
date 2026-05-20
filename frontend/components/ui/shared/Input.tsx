import React, { useState, forwardRef } from "react";
import clsx from "clsx";
import { Eye, EyeOff } from "lucide-react";

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  hasShowButton?: boolean;
  leftIcon?: React.ReactNode;
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, hasShowButton = false, leftIcon, ...rest }, ref) => {
    const [isShowPassword, setIsShowPassword] = useState(false);

    const handleShowPassword = () => {
      setIsShowPassword((prev) => !prev);
    };

    return (
      <div className="relative w-full">
        {leftIcon && (
          <span className="pointer-events-none absolute left-main-2 top-1/2 -translate-y-1/2 text-slate-400">
            {leftIcon}
          </span>
        )}
        {hasShowButton && (
          <button
            type="button"
            className="absolute right-main-2 top-1/2 -translate-y-1/2 text-slate-400 transition-colors hover:text-main-blue focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-main-blue/30"
            onClick={handleShowPassword}
            aria-label={isShowPassword ? "비밀번호 숨기기" : "비밀번호 보기"}
          >
            {isShowPassword ? <Eye size={22} /> : <EyeOff size={22} />}
          </button>
        )}
        <input
          {...rest}
          ref={ref}
          type={
            hasShowButton ? (isShowPassword ? "text" : "password") : rest.type
          }
          className={clsx(
            "w-full p-main bg-transparent border border-main-light-gray rounded-main outline-none transition-colors focus:border-main-blue/50 focus:ring-2 focus:ring-main-blue/10",
            leftIcon && "pl-[54px]",
            hasShowButton && "pr-[54px]",
            className
          )}
          tabIndex={1}
        />
      </div>
    );
  }
);

// 💡 forwardRef 사용 시 displayName 설정 권장 (디버깅 편의)
Input.displayName = "Input";

export default Input;
