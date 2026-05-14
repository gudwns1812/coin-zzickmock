import type {
  CommunityCategory,
  CommunityPostList,
  CommunityPostSummary,
} from "@/lib/futures-api";
import type { ReactNode } from "react";
import clsx from "clsx";
import { ChevronLeft, ChevronRight, Eye, MessageCircle, PencilLine, Pin, ThumbsUp } from "lucide-react";
import Link from "next/link";
import CommunityState from "./CommunityState";
import {
  COMMUNITY_CATEGORY_LABELS,
  COMMUNITY_POST_CATEGORIES,
  formatCommunityCount,
  formatCommunityDate,
} from "./community-format";

type CommunityListViewProps = {
  result: CommunityPostList | null;
  category: Exclude<CommunityCategory, "NOTICE"> | null;
  unavailable: boolean;
  message: string | null;
};

export default function CommunityListView({
  result,
  category,
  unavailable,
  message,
}: CommunityListViewProps) {
  if (unavailable || !result) {
    return (
      <CommunityShell>
        <CommunityState
          title="커뮤니티를 불러오지 못했습니다"
          message={message ?? "잠시 후 다시 시도해주세요."}
          tone="error"
          actionHref="/markets"
          actionLabel="대시보드로 이동"
        />
      </CommunityShell>
    );
  }

  const isEmpty = result.posts.length === 0 && result.pinnedNotices.length === 0;

  return (
    <CommunityShell>
      <section className="rounded-main border border-white/60 bg-white/80 p-main-2 shadow-md backdrop-blur-md">
        <div className="flex items-start justify-between gap-main-2">
          <div>
            <p className="text-sm-custom font-semibold text-main-blue">Community</p>
            <h1 className="mt-2 text-4xl-custom font-bold text-main-dark-gray">
              커뮤니티
            </h1>
            <p className="mt-3 text-sm-custom leading-6 text-main-dark-gray/60">
              공지와 트레이딩 인사이트를 읽고 시장 관점을 빠르게 확인하세요.
            </p>
          </div>
          <Link
            className="inline-flex items-center gap-2 rounded-main bg-main-blue px-main py-3 text-sm-custom font-semibold text-white shadow-sm transition-colors hover:bg-main-blue/90"
            href="/community/write"
          >
            <PencilLine size={16} /> 글쓰기
          </Link>
        </div>
      </section>

      <nav className="flex flex-wrap items-center gap-2" aria-label="커뮤니티 카테고리">
        <CategoryTab href="/community" active={category === null} label="전체" />
        {COMMUNITY_POST_CATEGORIES.map((item) => (
          <CategoryTab
            key={item}
            href={`/community?category=${item}`}
            active={category === item}
            label={COMMUNITY_CATEGORY_LABELS[item]}
          />
        ))}
      </nav>

      {result.pinnedNotices.length > 0 ? (
        <section className="grid gap-3">
          <h2 className="flex items-center gap-2 text-lg-custom font-bold text-main-dark-gray">
            <Pin size={18} className="text-main-blue" /> 최신 공지
          </h2>
          <div className="grid gap-3">
            {result.pinnedNotices.map((post) => (
              <CommunityPostCard key={post.id} post={post} pinned />
            ))}
          </div>
        </section>
      ) : null}

      <section className="grid gap-3">
        <div className="flex items-center justify-between">
          <h2 className="text-lg-custom font-bold text-main-dark-gray">일반 글</h2>
          <span className="text-xs-custom text-main-dark-gray/50">
            총 {formatCommunityCount(result.page.totalElements)}개
          </span>
        </div>

        {isEmpty ? (
          <CommunityState
            title="아직 게시글이 없습니다"
            message="첫 글 작성 기능은 다음 PR에서 연결됩니다. 지금은 공지와 읽기 흐름을 먼저 확인할 수 있습니다."
          />
        ) : (
          <div className="grid gap-3">
            {result.posts.map((post) => (
              <CommunityPostCard key={post.id} post={post} />
            ))}
          </div>
        )}
      </section>

      <CommunityPagination page={result.page.page} hasNext={result.page.hasNext} category={category} />
    </CommunityShell>
  );
}

function CommunityShell({ children }: { children: ReactNode }) {
  return <div className="mx-auto flex w-full max-w-[1180px] flex-col gap-main-2 px-main-3 pb-24 pt-4">{children}</div>;
}

function CategoryTab({ href, active, label }: { href: string; active: boolean; label: string }) {
  return (
    <Link
      aria-current={active ? "page" : undefined}
      className={clsx(
        "rounded-main px-main py-2 text-sm-custom font-semibold transition-colors",
        active ? "bg-main-blue text-white shadow-sm" : "bg-white text-main-dark-gray/65 hover:text-main-blue"
      )}
      href={href}
    >
      {label}
    </Link>
  );
}

function CommunityPostCard({ post, pinned = false }: { post: CommunityPostSummary; pinned?: boolean }) {
  return (
    <Link
      className={clsx(
        "group rounded-main border bg-white/85 p-main shadow-sm transition-all hover:-translate-y-0.5 hover:border-main-blue/35 hover:shadow-md",
        pinned ? "border-main-blue/25" : "border-main-light-gray"
      )}
      href={`/community/${post.id}`}
    >
      <div className="flex items-start justify-between gap-main">
        <div>
          <div className="flex items-center gap-2">
            <span className={clsx(
              "rounded-full px-2 py-1 text-xs-custom font-bold",
              pinned ? "bg-main-blue/10 text-main-blue" : "bg-main-light-gray/60 text-main-dark-gray/60"
            )}>
              {COMMUNITY_CATEGORY_LABELS[post.category]}
            </span>
            <span className="text-xs-custom text-main-dark-gray/45">{formatCommunityDate(post.createdAt)}</span>
          </div>
          <h3 className="mt-3 text-lg-custom font-bold text-main-dark-gray transition-colors group-hover:text-main-blue">
            {post.title}
          </h3>
          <p className="mt-2 text-sm-custom text-main-dark-gray/55">by {post.authorNickname}</p>
        </div>
        <div className="flex items-center gap-3 text-xs-custom text-main-dark-gray/50">
          <Metric icon={<Eye size={14} />} value={post.viewCount} label="조회" />
          <Metric icon={<ThumbsUp size={14} />} value={post.likeCount} label="좋아요" />
          <Metric icon={<MessageCircle size={14} />} value={post.commentCount} label="댓글" />
        </div>
      </div>
    </Link>
  );
}

function Metric({ icon, value, label }: { icon: ReactNode; value: number; label: string }) {
  return (
    <span className="inline-flex items-center gap-1" title={label}>
      {icon}
      {formatCommunityCount(value)}
    </span>
  );
}

function CommunityPagination({ page, hasNext, category }: { page: number; hasNext: boolean; category: Exclude<CommunityCategory, "NOTICE"> | null }) {
  const previousHref = communityHref(Math.max(page - 1, 0), category);
  const nextHref = communityHref(page + 1, category);

  return (
    <div className="flex items-center justify-center gap-3">
      <Link
        aria-disabled={page === 0}
        className={clsx(
          "inline-flex items-center gap-1 rounded-main px-main py-2 text-sm-custom font-semibold",
          page === 0 ? "pointer-events-none bg-main-light-gray/45 text-main-dark-gray/35" : "bg-white text-main-dark-gray/65 hover:text-main-blue"
        )}
        href={previousHref}
      >
        <ChevronLeft size={16} /> 이전
      </Link>
      <span className="rounded-main bg-white px-main py-2 text-sm-custom font-bold text-main-dark-gray">
        {page + 1}
      </span>
      <Link
        aria-disabled={!hasNext}
        className={clsx(
          "inline-flex items-center gap-1 rounded-main px-main py-2 text-sm-custom font-semibold",
          !hasNext ? "pointer-events-none bg-main-light-gray/45 text-main-dark-gray/35" : "bg-white text-main-dark-gray/65 hover:text-main-blue"
        )}
        href={nextHref}
      >
        다음 <ChevronRight size={16} />
      </Link>
    </div>
  );
}

function communityHref(page: number, category: Exclude<CommunityCategory, "NOTICE"> | null) {
  const params = new URLSearchParams();
  if (category) params.set("category", category);
  if (page > 0) params.set("page", String(page));
  const query = params.toString();
  return query ? `/community?${query}` : "/community";
}
