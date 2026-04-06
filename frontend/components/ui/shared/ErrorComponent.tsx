import { AlertCircle } from "lucide-react";
import React from "react";
import clsx from "clsx";

interface ErrorComponentProps {
  title?: string;
  message?: string;
  className?: string;
  onRetry?: () => void;
  fullHeight?: boolean;
}

const ErrorComponent = ({
  title = "오류 발생",
  message = "데이터를 불러오는데 실패했습니다.",
  className,
  onRetry,
  fullHeight = true,
}: ErrorComponentProps) => {
  return (
    <div
      className={clsx(
        "flex flex-col items-center justify-center gap-2 w-full bg-red-50/50 rounded-main p-6 border border-red-100",
        fullHeight ? "min-h-[200px] h-full" : "h-fit",
        className
      )}
    >
      <AlertCircle className="w-10 h-10 text-red-500/50" />
      <h3 className="text-lg font-bold text-red-500">{title}</h3>
      <p className="text-sm text-gray-500 text-center whitespace-pre-wrap">
        {message}
      </p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="mt-2 px-4 py-2 text-sm font-semibold text-red-500 bg-red-100/50 hover:bg-red-100 rounded-lg transition-colors"
        >
          다시 시도
        </button>
      )}
    </div>
  );
};

export default ErrorComponent;
