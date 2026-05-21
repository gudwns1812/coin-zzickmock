"use client";

import type {
  CommunityApiResult,
  CommunityCommentList,
  CommunityPostDetail,
  CommunityPostList,
} from "@/lib/futures-api";
import {
  createCommunityComment,
  deleteCommunityComment,
  deleteCommunityPost,
  likeCommunityPost,
  unlikeCommunityPost,
} from "@/lib/futures-client-api";
import { invalidateCommunityQueries } from "@/lib/futures-query-invalidation";
import { futuresQueryKeys } from "@/lib/futures-query-keys";
import { useMutation, useQueryClient, type QueryClient } from "@tanstack/react-query";
import { MessageCircle, Pencil, ThumbsUp, Trash2 } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { formatCommunityCount, formatCommunityDate } from "./community-format";

type CommunityPostActionsProps = {
  post: CommunityPostDetail;
};

export function CommunityPostActions({ post }: CommunityPostActionsProps) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [likedByMe, setLikedByMe] = useState(post.likedByMe);
  const [likeCount, setLikeCount] = useState(post.likeCount);
  const [error, setError] = useState<string | null>(null);

  const likeMutation = useMutation({
    mutationFn: async (desiredLikedByMe: boolean) => {
      return desiredLikedByMe
        ? likeCommunityPost(post.id)
        : unlikeCommunityPost(post.id);
    },
    onMutate: async (desiredLikedByMe) => {
      setError(null);
      await queryClient.cancelQueries({ queryKey: futuresQueryKeys.community });
      const rollback = snapshotCommunityLikeQueries(queryClient, post.id);
      updateCommunityLikeQueries(queryClient, post.id, desiredLikedByMe);
      setLikedByMe(desiredLikedByMe);
      setLikeCount((current) => Math.max(0, current + (desiredLikedByMe ? 1 : -1)));
      return rollback;
    },
    onError: (mutationError, _desiredLikedByMe, rollback) => {
      restoreCommunityLikeQueries(queryClient, rollback);
      setLikedByMe(post.likedByMe);
      setLikeCount(post.likeCount);
      setError(errorMessage(mutationError));
    },
    onSuccess: (result) => {
      updateCommunityLikeQueries(queryClient, post.id, result.likedByMe);
      void invalidateCommunityQueries(queryClient);
    },
  });

  useEffect(() => {
    if (likeMutation.isPending) {
      return;
    }
    setLikedByMe(post.likedByMe);
    setLikeCount(post.likeCount);
  }, [post.id, post.likedByMe, post.likeCount]);

  const deleteMutation = useMutation({
    mutationFn: () => deleteCommunityPost(post.id),
    onMutate: () => setError(null),
    onSuccess: () => {
      void invalidateCommunityQueries(queryClient);
      router.push("/community");
    },
    onError: (mutationError) => setError(errorMessage(mutationError)),
  });

  const handleDelete = () => {
    if (window.confirm("게시글을 삭제할까요? 삭제 후 목록에서 보이지 않습니다.")) {
      deleteMutation.mutate();
    }
  };

  return (
    <div className="mt-main flex flex-wrap items-center justify-between gap-main rounded-main bg-main-light-gray/25 p-main">
      <div className="flex flex-wrap items-center gap-2">
        <button
          className={likedByMe ? activeButtonClass : neutralButtonClass}
          disabled={likeMutation.isPending}
          onClick={() => likeMutation.mutate(!likedByMe)}
          type="button"
        >
          <ThumbsUp size={15} /> 좋아요 {formatCommunityCount(likeCount)}
        </button>
        {post.canEdit ? (
          <Link className={neutralButtonClass} href={`/community/${post.id}/edit`}>
            <Pencil size={15} /> 수정
          </Link>
        ) : null}
        {post.canDelete ? (
          <button
            className="inline-flex items-center gap-1 rounded-main bg-red-50 px-main py-2 text-sm-custom font-semibold text-red-600 shadow-sm transition-colors hover:bg-red-100 disabled:opacity-50"
            disabled={deleteMutation.isPending}
            onClick={handleDelete}
            type="button"
          >
            <Trash2 size={15} /> 삭제
          </button>
        ) : null}
      </div>
      {error ? <p className="text-sm-custom text-main-red">{error}</p> : null}
    </div>
  );
}

type CommunityLikeQuerySnapshot = {
  postId: number;
  detail: CommunityApiResult<CommunityPostDetail> | undefined;
  lists: Array<[
    readonly unknown[],
    CommunityApiResult<CommunityPostList> | undefined,
  ]>;
};

function snapshotCommunityLikeQueries(queryClient: QueryClient, postId: number): CommunityLikeQuerySnapshot {
  return {
    postId,
    detail: queryClient.getQueryData<CommunityApiResult<CommunityPostDetail>>(
      communityPostDetailQueryKey(postId)
    ),
    lists: queryClient.getQueriesData<CommunityApiResult<CommunityPostList>>({
      queryKey: [...futuresQueryKeys.community, "posts"],
    }),
  };
}

function restoreCommunityLikeQueries(
  queryClient: QueryClient,
  snapshot: CommunityLikeQuerySnapshot | undefined
) {
  if (!snapshot) {
    return;
  }

  queryClient.setQueryData(communityPostDetailQueryKey(snapshot.postId), snapshot.detail);
  for (const [queryKey, data] of snapshot.lists) {
    queryClient.setQueryData(queryKey, data);
  }
}

function updateCommunityLikeQueries(
  queryClient: QueryClient,
  postId: number,
  likedByMe: boolean
) {
  queryClient.setQueryData<CommunityApiResult<CommunityPostDetail>>(
    communityPostDetailQueryKey(postId),
    (current) => {
      if (!current?.data) {
        return current;
      }

      return {
        ...current,
        data: updateCommunityPostLikeFields(current.data, likedByMe),
      };
    }
  );

  queryClient.setQueriesData<CommunityApiResult<CommunityPostList>>(
    { queryKey: [...futuresQueryKeys.community, "posts"] },
    (current) => {
      if (!current?.data) {
        return current;
      }

      return {
        ...current,
        data: {
          ...current.data,
          pinnedNotices: current.data.pinnedNotices.map((post) =>
            post.id === postId ? updateCommunityPostLikeFields(post, likedByMe) : post
          ),
          posts: current.data.posts.map((post) =>
            post.id === postId ? updateCommunityPostLikeFields(post, likedByMe) : post
          ),
        },
      };
    }
  );
}

function updateCommunityPostLikeFields<T extends { likedByMe?: boolean; likeCount: number }>(
  post: T,
  likedByMe: boolean
): T {
  const previousLikedByMe = Boolean(post.likedByMe);
  const likeCount = previousLikedByMe === likedByMe
    ? post.likeCount
    : Math.max(0, post.likeCount + (likedByMe ? 1 : -1));

  return {
    ...post,
    likedByMe,
    likeCount,
  };
}

function communityPostDetailQueryKey(postId: number) {
  return [...futuresQueryKeys.community, postId] as const;
}

type CommunityCommentsProps = {
  postId: number;
  initialComments: CommunityCommentList | null;
};

export function CommunityComments({ postId, initialComments }: CommunityCommentsProps) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [content, setContent] = useState("");
  const [error, setError] = useState<string | null>(null);

  const createMutation = useMutation({
    mutationFn: () => createCommunityComment(postId, content.trim()),
    onMutate: () => setError(null),
    onSuccess: () => {
      setContent("");
      void invalidateCommunityQueries(queryClient);
    },
    onError: (mutationError) => setError(errorMessage(mutationError)),
  });

  const deleteMutation = useMutation({
    mutationFn: (commentId: number) => deleteCommunityComment(postId, commentId),
    onMutate: () => setError(null),
    onSuccess: () => {
      void invalidateCommunityQueries(queryClient);
    },
    onError: (mutationError) => setError(errorMessage(mutationError)),
  });

  const canSubmit = content.trim().length > 0 && content.trim().length <= 1000;

  return (
    <section className="rounded-main bg-white/90 p-main-2 shadow-md ring-1 ring-main-light-gray/40">
      <div className="flex items-center justify-between">
        <h2 className="flex items-center gap-2 text-xl-custom font-bold text-main-dark-gray">
          <MessageCircle size={18} /> 댓글
        </h2>
        <span className="text-xs-custom text-main-dark-gray/50">
          {formatCommunityCount(initialComments?.page.totalElements ?? 0)}개
        </span>
      </div>

      <div className="mt-main grid gap-2">
        <textarea
          id="community-comment"
          name="comment"
          className="min-h-24 rounded-main bg-main-light-gray/25 p-main text-sm-custom outline-none ring-1 ring-main-light-gray/45 transition-all placeholder:text-main-dark-gray/35 focus:bg-white focus:ring-main-blue/35"
          maxLength={1000}
          onChange={(event) => setContent(event.target.value)}
          placeholder="댓글을 입력하세요."
          value={content}
        />
        <div className="flex items-center justify-between gap-main">
          <span className="text-xs-custom text-main-dark-gray/45">
            {content.trim().length}/1000
          </span>
          <button
            className="rounded-main bg-main-blue px-main py-2 text-sm-custom font-semibold text-white shadow-sm transition-all hover:bg-main-blue/90 disabled:bg-main-light-gray disabled:text-main-dark-gray/35 disabled:shadow-none"
            disabled={!canSubmit || createMutation.isPending}
            onClick={() => createMutation.mutate()}
            type="button"
          >
            댓글 등록
          </button>
        </div>
      </div>

      {error ? <p className="mt-3 text-sm-custom text-main-red">{error}</p> : null}

      {initialComments?.comments.length ? (
        <div className="mt-main grid gap-3">
          {initialComments.comments.map((comment) => (
            <article
              key={comment.id}
              className="rounded-main bg-white/76 p-main shadow-sm ring-1 ring-main-light-gray/45"
            >
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="max-w-64 truncate text-xs-custom font-semibold text-main-blue/80">
                  {comment.authorNickname}
                </span>
                <div className="flex items-center gap-2 text-xs-custom text-main-dark-gray/40">
                  <span>{formatCommunityDate(comment.createdAt)}</span>
                  {comment.canDelete ? (
                    <button
                      className="font-semibold text-main-red transition-opacity hover:opacity-80 disabled:opacity-50"
                      disabled={deleteMutation.isPending}
                      onClick={() => deleteMutation.mutate(comment.id)}
                      type="button"
                    >
                      삭제
                    </button>
                  ) : null}
                </div>
              </div>
              <p className="mt-2 whitespace-pre-wrap break-words text-base-custom leading-7 text-main-dark-gray">
                {comment.content}
              </p>
            </article>
          ))}
        </div>
      ) : (
        <div className="mt-main rounded-main bg-main-light-gray/25 p-main text-sm-custom text-main-dark-gray/55 ring-1 ring-white/60">
          아직 댓글이 없습니다. 첫 댓글을 남겨보세요.
        </div>
      )}
    </section>
  );
}

const neutralButtonClass =
  "inline-flex items-center gap-1 rounded-main bg-white px-main py-2 text-sm-custom font-semibold text-main-dark-gray/65 shadow-sm ring-1 ring-main-light-gray/45 transition-colors hover:text-main-blue disabled:opacity-50";
const activeButtonClass =
  "inline-flex items-center gap-1 rounded-main bg-main-blue px-main py-2 text-sm-custom font-semibold text-white shadow-sm transition-colors disabled:opacity-50";

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "요청을 처리하지 못했습니다.";
}
