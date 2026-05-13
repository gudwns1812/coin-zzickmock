import { AlertCircle, Inbox } from "lucide-react";
import Link from "next/link";

type CommunityStateProps = {
  title: string;
  message: string;
  tone?: "empty" | "error";
  actionHref?: string;
  actionLabel?: string;
};

export default function CommunityState({
  title,
  message,
  tone = "empty",
  actionHref,
  actionLabel,
}: CommunityStateProps) {
  const Icon = tone === "error" ? AlertCircle : Inbox;
  const iconClassName = tone === "error" ? "text-main-red/60" : "text-main-blue/55";

  return (
    <div className="flex min-h-[220px] flex-col items-center justify-center rounded-main border border-main-light-gray bg-white/75 p-main-2 text-center shadow-sm">
      <Icon className={iconClassName} size={42} aria-hidden />
      <h2 className="mt-4 text-xl-custom font-bold text-main-dark-gray">{title}</h2>
      <p className="mt-2 max-w-[520px] text-sm-custom leading-6 text-main-dark-gray/60">
        {message}
      </p>
      {actionHref && actionLabel ? (
        <Link
          className="mt-5 rounded-main bg-main-blue px-main py-2 text-sm-custom font-semibold text-white transition-colors hover:bg-main-blue/85"
          href={actionHref}
        >
          {actionLabel}
        </Link>
      ) : null}
    </div>
  );
}
