import type { ReactNode } from "react";

type ProtectedPageSkeletonVariant =
  | "generic"
  | "mypage"
  | "shop"
  | "community-list"
  | "community-editor"
  | "community-detail"
  | "watchlist"
  | "admin"
  | "admin-table";

type ProtectedPageSkeletonProps = {
  title?: string;
  description?: string;
  variant?: ProtectedPageSkeletonVariant;
};

const DEFAULT_COPY: Record<
  ProtectedPageSkeletonVariant,
  { eyebrow: string; title: string; description: string }
> = {
  generic: {
    eyebrow: "Protected",
    title: "권한을 확인하고 있습니다",
    description: "보호된 화면을 열기 전에 로그인 상태를 확인합니다.",
  },
  mypage: {
    eyebrow: "My Page",
    title: "마이페이지를 준비하고 있습니다",
    description: "계정 권한 확인 후 자산과 포인트 정보를 불러옵니다.",
  },
  shop: {
    eyebrow: "Point Shop",
    title: "포인트 상점을 준비하고 있습니다",
    description: "로그인 상태를 확인한 뒤 상품과 교환 패널을 표시합니다.",
  },
  "community-list": {
    eyebrow: "Community",
    title: "커뮤니티 목록을 준비하고 있습니다",
    description: "인증 확인 후 게시글 목록과 검색 도구를 표시합니다.",
  },
  "community-editor": {
    eyebrow: "Community",
    title: "게시글 작성 화면을 준비하고 있습니다",
    description: "작성 권한을 확인한 뒤 에디터를 표시합니다.",
  },
  "community-detail": {
    eyebrow: "Community",
    title: "게시글을 준비하고 있습니다",
    description: "접근 권한 확인 후 본문과 댓글을 표시합니다.",
  },
  watchlist: {
    eyebrow: "Watchlist",
    title: "관심 심볼을 준비하고 있습니다",
    description: "계정 권한 확인 후 관심 종목 화면을 표시합니다.",
  },
  admin: {
    eyebrow: "Admin",
    title: "관리자 권한을 확인하고 있습니다",
    description: "관리자 메뉴를 표시하기 전에 권한을 검증합니다.",
  },
  "admin-table": {
    eyebrow: "Admin",
    title: "관리자 데이터를 준비하고 있습니다",
    description: "권한 확인 후 관리 테이블과 작업 버튼을 표시합니다.",
  },
};

export default function ProtectedPageSkeleton({
  title,
  description,
  variant = "generic",
}: ProtectedPageSkeletonProps) {
  const copy = DEFAULT_COPY[variant];

  return (
    <div
      aria-busy="true"
      aria-live="polite"
      className="px-main-2 pb-24 pt-4"
      role="status"
    >
      <section className="overflow-hidden rounded-main border border-main-light-gray bg-white shadow-sm">
        <div className="coin-loading-scan h-[3px] bg-gradient-to-r from-transparent via-main-blue to-transparent" />
        <div className="flex items-start justify-between gap-main-2 p-main-2">
          <div className="min-w-0">
            <p className="text-sm-custom font-semibold text-main-dark-gray/55">
              {copy.eyebrow}
            </p>
            <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
              {title ?? copy.title}
            </h1>
            <p className="mt-2 max-w-[560px] text-sm-custom leading-6 text-main-dark-gray/60">
              {description ?? copy.description}
            </p>
          </div>
          <div className="grid size-[72px] place-items-center rounded-main bg-main-blue/10">
            <span className="size-9 rounded-full bg-main-blue/55 coin-loading-shimmer" />
          </div>
        </div>
      </section>

      <section className="mt-main-2">{renderSkeletonBody(variant)}</section>
    </div>
  );
}

function renderSkeletonBody(variant: ProtectedPageSkeletonVariant): ReactNode {
  if (variant === "shop") {
    return <ShopSkeleton />;
  }
  if (variant === "community-list") {
    return <CommunityListSkeleton />;
  }
  if (variant === "community-editor") {
    return <CommunityEditorSkeleton />;
  }
  if (variant === "community-detail") {
    return <CommunityDetailSkeleton />;
  }
  if (variant === "watchlist") {
    return <MarketCardSkeleton />;
  }
  if (variant === "admin" || variant === "admin-table") {
    return <AdminSkeleton />;
  }
  return <MetricSkeleton />;
}

function MetricSkeleton() {
  return (
    <div className="grid grid-cols-3 gap-main-2">
      {["asset", "point", "history"].map((key) => (
        <article
          className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm"
          key={key}
        >
          <div className="h-3 w-20 rounded-full bg-main-dark-gray/15" />
          <div className="mt-main-2 h-7 w-32 rounded-full bg-main-light-gray/80 coin-loading-shimmer" />
          <div className="mt-main h-3 w-24 rounded-full bg-main-light-gray/60 coin-loading-shimmer" />
        </article>
      ))}
    </div>
  );
}

function ShopSkeleton() {
  return (
    <div className="grid grid-cols-[minmax(0,1fr)_320px] gap-main-2">
      <div className="grid grid-cols-2 gap-main-2">
        {[1, 2, 3, 4].map((item) => (
          <article
            className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm"
            key={item}
          >
            <div className="h-28 rounded-main bg-main-light-gray/65 coin-loading-shimmer" />
            <div className="mt-main-2 h-4 w-28 rounded-full bg-main-dark-gray/15" />
            <div className="mt-main h-3 w-36 rounded-full bg-main-light-gray/70 coin-loading-shimmer" />
          </article>
        ))}
      </div>
      <aside className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <div className="h-4 w-24 rounded-full bg-main-dark-gray/15" />
        <div className="mt-main-2 grid gap-main">
          {[1, 2, 3].map((item) => (
            <div className="h-10 rounded-main bg-main-light-gray/70 coin-loading-shimmer" key={item} />
          ))}
        </div>
      </aside>
    </div>
  );
}

function CommunityListSkeleton() {
  return (
    <div className="grid gap-main-2">
      <div className="rounded-main border border-main-light-gray bg-white p-main shadow-sm">
        <div className="h-10 rounded-full bg-main-light-gray/70 coin-loading-shimmer" />
      </div>
      {[1, 2, 3].map((item) => (
        <article
          className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm"
          key={item}
        >
          <div className="h-4 w-1/2 rounded-full bg-main-dark-gray/15" />
          <div className="mt-main h-3 w-2/3 rounded-full bg-main-light-gray/70 coin-loading-shimmer" />
          <div className="mt-main h-3 w-1/3 rounded-full bg-main-light-gray/60 coin-loading-shimmer" />
        </article>
      ))}
    </div>
  );
}

function CommunityEditorSkeleton() {
  return (
    <div className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
      <div className="h-11 rounded-main bg-main-light-gray/70 coin-loading-shimmer" />
      <div className="mt-main-2 h-64 rounded-main bg-main-light-gray/60 coin-loading-shimmer" />
      <div className="mt-main-2 flex justify-end gap-main">
        <div className="h-10 w-24 rounded-full bg-main-light-gray/70 coin-loading-shimmer" />
        <div className="h-10 w-28 rounded-full bg-main-blue/25 coin-loading-shimmer" />
      </div>
    </div>
  );
}

function CommunityDetailSkeleton() {
  return (
    <div className="grid gap-main-2">
      <article className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <div className="h-6 w-2/3 rounded-full bg-main-dark-gray/15" />
        <div className="mt-main-2 grid gap-main">
          {[1, 2, 3].map((item) => (
            <div className="h-3 rounded-full bg-main-light-gray/70 coin-loading-shimmer" key={item} />
          ))}
        </div>
      </article>
      <div className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
        <div className="h-20 rounded-main bg-main-light-gray/60 coin-loading-shimmer" />
      </div>
    </div>
  );
}

function MarketCardSkeleton() {
  return (
    <div className="grid grid-cols-2 gap-main-2">
      {["BTC", "ETH", "SOL", "XRP"].map((symbol) => (
        <article
          className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm"
          key={symbol}
        >
          <div className="flex items-start justify-between gap-main">
            <div>
              <p className="text-xs-custom font-semibold text-main-dark-gray/45">
                {symbol}
              </p>
              <div className="mt-2 h-6 w-32 rounded-full bg-main-light-gray/75 coin-loading-shimmer" />
            </div>
            <div className="h-7 w-16 rounded-full bg-main-blue/15 coin-loading-shimmer" />
          </div>
          <div className="mt-main-2 grid grid-cols-3 gap-main">
            {[1, 2, 3].map((item) => (
              <div className="h-14 rounded-main bg-main-light-gray/60 coin-loading-shimmer" key={item} />
            ))}
          </div>
        </article>
      ))}
    </div>
  );
}

function AdminSkeleton() {
  return (
    <div className="grid gap-main-2">
      <div className="grid grid-cols-2 gap-main-2">
        {["redemptions", "items"].map((item) => (
          <article
            className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm"
            key={item}
          >
            <div className="h-5 w-36 rounded-full bg-main-blue/20 coin-loading-shimmer" />
            <div className="mt-main h-3 w-24 rounded-full bg-main-light-gray/75 coin-loading-shimmer" />
          </article>
        ))}
      </div>
      <div className="overflow-hidden rounded-main border border-main-light-gray bg-white shadow-sm">
        {[1, 2, 3, 4].map((row) => (
          <div
            className="grid grid-cols-[1.4fr_1fr_1fr_120px] gap-main border-b border-main-light-gray/60 p-main last:border-b-0"
            key={row}
          >
            <div className="h-4 rounded-full bg-main-light-gray/75 coin-loading-shimmer" />
            <div className="h-4 rounded-full bg-main-light-gray/65 coin-loading-shimmer" />
            <div className="h-4 rounded-full bg-main-light-gray/65 coin-loading-shimmer" />
            <div className="h-8 rounded-full bg-main-blue/20 coin-loading-shimmer" />
          </div>
        ))}
      </div>
    </div>
  );
}
