import type { CommunityCommentList, CommunityPostDetail } from "@/lib/futures-api";
import type { ReactNode } from "react";
import { ArrowLeft, Eye, MessageCircle, ThumbsUp } from "lucide-react";
import Link from "next/link";
import CommunityState from "./CommunityState";
import { CommunityComments, CommunityPostActions } from "./CommunityDetailInteractions";
import CommunityTiptapRenderer from "./CommunityTiptapRenderer";
import {
  COMMUNITY_CATEGORY_LABELS,
  formatCommunityCount,
  formatCommunityDate,
} from "./community-format";

type CommunityDetailViewProps = {
  post: CommunityPostDetail | null;
  comments: CommunityCommentList | null;
  unavailable: boolean;
  message: string | null;
  isLoading?: boolean;
};

export default function CommunityDetailView({
  post,
  comments,
  unavailable,
  message,
  isLoading = false,
}: CommunityDetailViewProps) {
  if (isLoading) {
    return (
      <div className="mx-auto w-full max-w-[1000px] px-main-3 pb-24 pt-4">
        <CommunityState title="게시글을 불러오는 중입니다" message="브라우저에서 로그인 세션을 확인하고 게시글을 가져오고 있습니다." />
      </div>
    );
  }
  if (unavailable || !post) {
    return (
      <div className="mx-auto w-full max-w-[1000px] px-main-3 pb-24 pt-4">
        <CommunityState
          title="게시글을 불러오지 못했습니다"
          message={message ?? "삭제되었거나 접근할 수 없는 게시글입니다."}
          tone="error"
          actionHref="/community"
          actionLabel="목록으로 돌아가기"
        />
      </div>
    );
  }

  return (
    <article className="mx-auto flex w-full max-w-[1000px] flex-col gap-main-2 px-main-3 pb-24 pt-4">
      <Link
        className="inline-flex w-fit items-center gap-2 rounded-full bg-white/70 px-3 py-2 text-sm-custom font-semibold text-main-dark-gray/60 shadow-sm ring-1 ring-white/70 transition-colors hover:text-main-blue"
        href="/community"
      >
        <ArrowLeft size={16} /> 커뮤니티 피드
      </Link>

      <header className="relative overflow-hidden rounded-main bg-white/88 p-main-2 shadow-color ring-1 ring-white/70 backdrop-blur-md">
        <div className="pointer-events-none absolute -right-16 -top-20 h-52 w-52 rounded-full bg-main-blue/10 blur-3xl" />
        <div className="relative">
          <div className="flex flex-wrap items-center gap-2">
            <span className="rounded-full bg-main-blue/10 px-2 py-1 text-xs-custom font-bold text-main-blue ring-1 ring-main-blue/10">
              {COMMUNITY_CATEGORY_LABELS[post.category]}
            </span>
            <span className="rounded-full bg-main-light-gray/35 px-2 py-1 text-xs-custom font-semibold text-main-dark-gray/45">
              {formatCommunityDate(post.createdAt)}
            </span>
          </div>
          <h1 className="mt-5 text-3xl-custom font-bold leading-tight text-main-dark-gray">{post.title}</h1>
          <div className="mt-5 grid gap-main rounded-main bg-white/64 p-main shadow-sm ring-1 ring-main-light-gray/45 sm:grid-cols-[1fr_auto] sm:items-center">
            <p className="text-sm-custom font-semibold text-main-dark-gray">
              <span className="text-main-dark-gray/55">작성자:</span> {post.authorNickname}
            </p>
            <div className="flex flex-wrap items-center gap-2 text-xs-custom text-main-dark-gray/55">
              <Metric icon={<MessageCircle size={15} />} value={post.commentCount} label="댓글" active={post.commentCount > 0} />
              <Metric icon={<ThumbsUp size={15} />} value={post.likeCount} label="좋아요" active={post.likedByMe || post.likeCount > 0} />
              <Metric icon={<Eye size={15} />} value={post.viewCount} label="조회" />
            </div>
          </div>
        </div>
      </header>

      <section className="rounded-main bg-white/92 p-main-2 shadow-md ring-1 ring-main-light-gray/40">
        <CommunityTiptapRenderer contentJson={post.contentJson} />
        <CommunityPostActions post={post} />
      </section>

      <CommunityComments initialComments={comments} postId={post.id} />
    </article>
  );
}

function Metric({ icon, value, label, active = false }: { icon: ReactNode; value: number; label: string; active?: boolean }) {
  return (
    <span
      className={active ? "inline-flex items-center gap-1 rounded-full bg-main-blue/10 px-2 py-1 font-semibold text-main-blue" : "inline-flex items-center gap-1 rounded-full bg-main-light-gray/35 px-2 py-1 font-semibold"}
      title={label}
    >
      {icon}
      {formatCommunityCount(value)}
    </span>
  );
}
