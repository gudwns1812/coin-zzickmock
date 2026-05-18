import type {
  CommunityCategory,
  CommunityPostList,
  CommunityPostSummary,
} from "@/lib/futures-api";
import type { ReactNode } from "react";
import clsx from "clsx";
import {
  ChevronLeft,
  ChevronRight,
  Eye,
  MessageCircle,
  Pin,
  Plus,
  Search,
  ThumbsUp,
} from "lucide-react";
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
  searchQuery: string;
  unavailable: boolean;
  message: string | null;
};

export default function CommunityListView({
  result,
  category,
  searchQuery,
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
  const filteredNotices = filterCommunityPosts(result.pinnedNotices, searchQuery);
  const filteredPosts = filterCommunityPosts(result.posts, searchQuery);
  const hasSearchQuery = searchQuery.length > 0;
  const isFilteredEmpty = filteredPosts.length === 0 && filteredNotices.length === 0;

  return (
    <CommunityShell>
      <h1 className="text-3xl-custom font-extrabold leading-tight text-[#111827]">투자자 커뮤니티</h1>

      <div className="grid gap-main xl:grid-cols-[minmax(0,1fr)_128px]">
        <form
          action="/community"
          className="relative"
          method="get"
          role="search"
        >
          {category ? <input name="category" type="hidden" value={category} /> : null}
          <button
            aria-label="검색"
            className="absolute left-5 top-1/2 -translate-y-1/2 text-main-dark-gray/50 transition-colors hover:text-main-blue focus:outline-none focus:ring-2 focus:ring-main-blue/20"
            type="submit"
          >
            <Search aria-hidden className="size-5" />
          </button>
          <input
            aria-label="게시글 검색"
            className="h-11 w-full rounded-main border border-main-light-gray bg-white pl-12 pr-main-2 text-base-custom font-medium text-[#111827] outline-none transition-colors placeholder:text-main-dark-gray/55 focus:border-main-blue/60 focus:ring-2 focus:ring-main-blue/15"
            defaultValue={searchQuery}
            name="q"
            placeholder="게시글 검색..."
            type="search"
          />
        </form>

        <Link
          className="inline-flex h-11 items-center justify-center gap-1.5 rounded-main bg-main-blue px-main text-base-custom font-bold text-white shadow-sm transition-colors hover:bg-main-blue/90 focus:outline-none focus:ring-2 focus:ring-main-blue/30"
          href="/community/write"
        >
          <Plus aria-hidden className="size-4" /> 글쓰기
        </Link>
      </div>

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

      {filteredNotices.length > 0 ? (
        <section className="rounded-main bg-main-blue/10 p-main shadow-sm ring-2 ring-main-blue/35">
          <h2 className="mb-main flex items-center gap-2 text-lg-custom font-extrabold text-main-blue">
            <Pin aria-hidden className="size-4 fill-main-blue/80" /> 공지사항
          </h2>
          <div className="grid gap-main">
            {filteredNotices.map((post) => (
              <NoticeRow key={post.id} post={post} />
            ))}
          </div>
        </section>
      ) : null}

      <section className="grid gap-main">
        {isEmpty || isFilteredEmpty ? (
          <CommunityState
            title={hasSearchQuery ? "검색 결과가 없습니다" : "아직 게시글이 없습니다"}
            message={
              hasSearchQuery
                ? "다른 검색어로 다시 확인해주세요."
                : "첫 글 작성 기능은 다음 PR에서 연결됩니다. 지금은 공지와 읽기 흐름을 먼저 확인할 수 있습니다."
            }
          />
        ) : (
          <div className="grid gap-main">
            {filteredPosts.map((post) => (
              <CommunityPostCard key={post.id} post={post} />
            ))}
          </div>
        )}
      </section>

      <CommunityPagination
        category={category}
        hasNext={result.page.hasNext}
        page={result.page.page}
        searchQuery={searchQuery}
      />
    </CommunityShell>
  );
}

function CommunityShell({ children }: { children: ReactNode }) {
  return (
    <div className="mx-auto flex w-full max-w-[1120px] flex-col gap-main-2 px-main-2 pb-24 pt-main-3">
      {children}
    </div>
  );
}

function CategoryTab({ href, active, label }: { href: string; active: boolean; label: string }) {
  return (
    <Link
      aria-current={active ? "page" : undefined}
      className={clsx(
        "inline-flex h-10 items-center justify-center rounded-full px-main-2 text-base-custom font-bold transition-all focus:outline-none focus:ring-2 focus:ring-main-blue/25",
        active
          ? "bg-main-blue text-white shadow-sm"
          : "bg-white text-[#374151] shadow-sm ring-1 ring-main-light-gray hover:bg-main-blue/10 hover:text-main-blue"
      )}
      href={href}
    >
      {label}
    </Link>
  );
}

function NoticeRow({ post }: { post: CommunityPostSummary }) {
  return (
    <Link
      className="group grid min-h-[48px] grid-cols-[minmax(0,1fr)_auto] items-center gap-main rounded-main bg-white px-main-2 py-2 shadow-sm ring-1 ring-main-blue/25 transition-colors hover:ring-main-blue/50 focus:outline-none focus:ring-2 focus:ring-main-blue/30"
      href={`/community/${post.id}`}
    >
      <div className="min-w-0">
        <strong className="truncate text-base-custom font-extrabold text-[#1f2937] transition-colors group-hover:text-main-blue">
          {post.title}
        </strong>
      </div>
      <MetricsRow post={post} compact />
    </Link>
  );
}

function CommunityPostCard({ post }: { post: CommunityPostSummary }) {
  return (
    <Link
      className="group grid min-h-[104px] gap-2 rounded-main bg-white p-main-2 shadow-sm ring-1 ring-main-light-gray transition-all hover:-translate-y-0.5 hover:shadow-md hover:ring-main-blue/25 focus:outline-none focus:ring-2 focus:ring-main-blue/30"
      href={`/community/${post.id}`}
    >
      <span className="w-fit rounded-full bg-main-light-gray/45 px-3 py-1 text-xs-custom font-bold text-[#374151]">
        {COMMUNITY_CATEGORY_LABELS[post.category]}
      </span>
      <div>
        <h3 className="text-xl-custom font-extrabold leading-tight text-[#111827] transition-colors group-hover:text-main-blue">
          {post.title}
        </h3>
        <p className="mt-2 text-base-custom font-semibold text-main-dark-gray/75">
          {post.authorNickname}
        </p>
      </div>
      <MetricsRow post={post} />
    </Link>
  );
}

function MetricsRow({ post, compact = false }: { post: CommunityPostSummary; compact?: boolean }) {
  return (
    <div
      className={clsx(
        "flex shrink-0 items-center justify-end text-main-dark-gray/70",
        compact ? "gap-main text-xs-custom" : "gap-main-2 text-sm-custom"
      )}
    >
      <span className="font-semibold text-main-dark-gray/65">{formatCommunityDate(post.createdAt)}</span>
      <Metric icon={<Eye aria-hidden className="size-4" />} value={post.viewCount} label="조회" />
      <Metric icon={<ThumbsUp aria-hidden className="size-4" />} value={post.likeCount} label="좋아요" />
      <Metric icon={<MessageCircle aria-hidden className="size-4" />} value={post.commentCount} label="댓글" />
    </div>
  );
}

function Metric({
  icon,
  value,
  label,
}: {
  icon: ReactNode;
  value: number;
  label: string;
}) {
  return (
    <span className="inline-flex items-center gap-1.5 font-semibold" title={label}>
      {icon}
      {formatCommunityCount(value)}
    </span>
  );
}

function CommunityPagination({
  page,
  hasNext,
  category,
  searchQuery,
}: {
  page: number;
  hasNext: boolean;
  category: Exclude<CommunityCategory, "NOTICE"> | null;
  searchQuery: string;
}) {
  const previousHref = communityHref(Math.max(page - 1, 0), category, searchQuery);
  const nextHref = communityHref(page + 1, category, searchQuery);

  return (
    <div className="flex items-center justify-center gap-3">
      <Link
        aria-disabled={page === 0}
        className={clsx(
          "inline-flex items-center gap-1 rounded-main px-main py-2 text-sm-custom font-semibold shadow-sm transition-colors",
          page === 0 ? "pointer-events-none bg-white text-main-dark-gray/30" : "bg-white text-main-dark-gray/65 hover:text-main-blue"
        )}
        href={previousHref}
      >
        <ChevronLeft size={16} /> 이전
      </Link>
      <span className="rounded-main bg-main-blue px-main py-2 text-sm-custom font-bold text-white shadow-sm">
        {page + 1}
      </span>
      <Link
        aria-disabled={!hasNext}
        className={clsx(
          "inline-flex items-center gap-1 rounded-main px-main py-2 text-sm-custom font-semibold shadow-sm transition-colors",
          !hasNext ? "pointer-events-none bg-white text-main-dark-gray/30" : "bg-white text-main-dark-gray/65 hover:text-main-blue"
        )}
        href={nextHref}
      >
        다음 <ChevronRight size={16} />
      </Link>
    </div>
  );
}

function communityHref(
  page: number,
  category: Exclude<CommunityCategory, "NOTICE"> | null,
  searchQuery = ""
) {
  const params = new URLSearchParams();
  if (category) params.set("category", category);
  if (searchQuery) params.set("q", searchQuery);
  if (page > 0) params.set("page", String(page));
  const query = params.toString();
  return query ? `/community?${query}` : "/community";
}

function filterCommunityPosts(posts: CommunityPostSummary[], searchQuery: string) {
  if (!searchQuery) {
    return posts;
  }

  const normalizedQuery = searchQuery.toLocaleLowerCase("ko-KR");
  return posts.filter((post) => {
    const searchable = [
      post.title,
      post.authorNickname,
      COMMUNITY_CATEGORY_LABELS[post.category],
      formatCommunityDate(post.createdAt),
    ]
      .join(" ")
      .toLocaleLowerCase("ko-KR");

    return searchable.includes(normalizedQuery);
  });
}
