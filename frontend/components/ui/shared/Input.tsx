import React, { useState, forwardRef } from "react";
import clsx from "clsx";
import { Eye, EyeOff } from "lucide-react";

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  hasShowButton?: boolean;
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, hasShowButton = false, ...rest }, ref) => {
    const [isShowPassword, setIsShowPassword] = useState(false);

    const handleShowPassword = () => {
      setIsShowPassword((prev) => !prev);
    };

    return (
      <div className="relative w-full">
        {hasShowButton && (
          <button
            type="button"
            className="absolute right-main top-1/2 -translate-y-1/2 text-gray-200"
            onClick={handleShowPassword}
          >
            {isShowPassword ? <Eye /> : <EyeOff />}
          </button>
        )}
        <input
          {...rest}
          ref={ref}
          type={
            hasShowButton ? (isShowPassword ? "text" : "password") : rest.type
          }
          className={clsx(
            "w-full p-main bg-transparent border border-main-light-gray rounded-main focus:border-main-blue/50 outline-none",
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
