# 커뮤니티 기능 명세

## 목적

커뮤니티는 로그인한 사용자가 코인 선물 모의투자 경험을 바탕으로 글, 댓글, 좋아요, 조회수로 소통하는 게시판이다. 공지사항은 운영 공지 표면으로 다루며, 일반 사용자가 생성하거나 공지 카테고리로 변경할 수 없다.

## 범위

### 포함

- 로그인 전용 커뮤니티 목록, 상세, 작성, 수정 화면.
- 카테고리 말머리:
  - `NOTICE` / `공지사항`
  - `CHART_ANALYSIS` / `차트분석`
  - `COIN_INFORMATION` / `코인정보`
  - `CHAT` / `잡담`
- 커뮤니티 목록 상단에 최신 공지사항 3개 고정 표시.
- 일반 글 목록의 카테고리 필터와 기본 페이지네이션.
- 게시글 생성, 자기 게시글 수정, 자기 게시글 삭제.
- 관리자의 공지사항 생성/수정 및 모든 게시글 삭제.
- Tiptap 기반 에디터 JSON 저장/로드/렌더링.
- S3 presigned direct PUT 기반 이미지 업로드와 에디터 이미지 삽입.
- 댓글 목록/작성/자기 댓글 삭제.
- 게시글 좋아요 토글과 좋아요 수 표시.
- 게시글 상세 열람 시 조회수 증가/표시.

### 제외

- 키워드 검색.
- 별도 커뮤니티 관리자 관리 화면.
- 신고, 차단, 모더레이션 워크플로.
- 익명 또는 비로그인 공개 읽기.
- 일반 사용자의 공지사항 생성/변경.
- 대회, 경쟁전, 기간별/주변 순위 같은 확장 리더보드.

## 라우트와 화면

| Route | 목적 | 주요 상태 |
| --- | --- | --- |
| `/community` | 공지 3개 고정 영역과 일반 글 목록 | loading, empty, error, unauthenticated |
| `/community/[postId]` | 게시글 상세, JSON 본문 렌더링, 댓글/좋아요/조회수 | loading, not found/deleted, error |
| `/community/write` | 새 게시글 작성 | draft, validation error, upload pending/error |
| `/community/[postId]/edit` | 자기 게시글 또는 관리자 공지 수정 | loading, forbidden, validation error |

모든 커뮤니티 라우트는 로그인 이후 메인 앱 셸 아래에 둔다. 인증되지 않은 사용자는 기존 보호 라우트 정책처럼 `/login`으로 이동하거나 로그인 필요 상태를 본다.

## 권한 정책

- 읽기와 쓰기 API는 모두 인증 사용자를 요구한다.
- 일반 사용자는 `CHART_ANALYSIS`, `COIN_INFORMATION`, `CHAT` 글만 생성할 수 있다.
- `NOTICE` 생성, `NOTICE`로 카테고리 변경, 공지 유지 목적의 공지 수정은 관리자만 가능하다.
- 게시글 작성자는 자기 게시글의 제목, 카테고리, 본문을 수정할 수 있다. 단, 일반 사용자는 수정 시에도 `NOTICE`를 선택할 수 없다.
- 관리자는 모든 게시글을 삭제할 수 있다.
- 일반 사용자는 자기 게시글만 삭제할 수 있다.
- 댓글 작성자는 자기 댓글만 삭제할 수 있다.
- 관리자는 모든 댓글을 삭제할 수 있다.
- 게시글 작성자가 다른 사용자의 댓글을 삭제하는 권한은 1차 범위에 포함하지 않는다.
- UI의 버튼 노출은 사용자 경험 보조일 뿐이며, 최종 권한 판단은 백엔드 application/domain 정책과 web adapter에서 강제한다.

### 관리자 판단

관리자 여부는 backend auth boundary가 조회한 persisted member role을 기준으로 중앙화한다. 현재 schema 기준은 `members.role == ADMIN`이며, JWT claim이나 UI state만으로 공지 작성/선택, 게시글/댓글 관리자 삭제를 허용하지 않는다. 후속 schema가 `member_roles.role_name` 같은 별도 role table로 분리되면, 커뮤니티 권한 정책은 그 단일 provider/permission rule을 통해서만 갱신한다.

## 콘텐츠 정책

### Tiptap JSON 저장

게시글 본문은 raw HTML이 아니라 Tiptap JSON 문서 문자열로 저장한다. 백엔드는 web DTO 검증만으로 신뢰하지 않고 core-owned validator/value object에서 허용 구조를 확인한다.

허용 baseline:

- Nodes: `doc`, `paragraph`, `text`, `heading`(level 1-4), `bulletList`, `orderedList`, `listItem`, `blockquote`, `hardBreak`, `codeBlock`, `image`.
- Marks: `bold`, `italic`, `code`, `link`.
- Link protocol: `http:`, `https:`만 허용.
- Image attrs: 백엔드가 발급/승인한 `objectKey`와 허용된 public/CDN prefix의 `src`만 허용.

제한:

- 최대 JSON byte 크기는 `256 KiB`, 최대 추출 텍스트 길이는 `10,000`자, 최대 document depth는 `20`, 최대 이미지 수는 `10`개로 시작한다. 이 한도를 넘는 payload는 명확한 validation error로 거절한다.
- 알 수 없는 node/mark/attrs, 비허용 link protocol, 임의 외부 이미지 URL은 거절한다.
- 상세 렌더링의 유일한 승인 경로는 JSON을 허용 node별 React renderer로 변환하는 방식이다. raw HTML 직접 렌더링은 모든 경우에 금지하며, 예외가 필요하면 별도 보안 리뷰와 명시 승인을 거친 뒤 명세를 먼저 바꾼다.

## 이미지 업로드 정책

- 업로드 방식은 backend presign endpoint에서 S3 PUT URL을 받은 뒤, frontend가 S3에 직접 PUT하는 흐름을 기본으로 한다.
- backend는 `fileName`, `contentType`, `sizeBytes`를 받아 검증한다.
- 기본 허용 MIME: `image/png`, `image/jpeg`, `image/webp`, `image/gif`.
- size limit은 환경값으로 둔다. 문서 기본값은 `5 MiB` 이하를 권장한다.
- object key는 서버가 생성하며 예시는 `community/{memberId}/{uuid}.{ext}`이다. client가 최종 URL을 임의 지정할 수 없다.
- frontend는 presign에 사용한 `Content-Type`과 동일한 header로 PUT한다.
- presigned URL, credentials, secret 값은 로그/응답 저장소에 남기지 않는다.
- S3 bucket(`S3_BUCKET`), region(`S3_REGION`), public/CDN base URL(`PUBLIC_CDN_BASE_URL` 또는 `CDN_BASE_URL`), key prefix(`S3_KEY_PREFIX`), presign TTL(`PRESIGN_TTL`), allowed MIME(`ALLOWED_MIME`), max bytes(`MAX_BYTES`)는 환경변수로 구성한다.
- 애플리케이션은 시작 시 필수 S3 환경변수의 존재와 기본 형식을 검증한다. 예: region pattern, numeric TTL/bytes, comma-separated MIME list, CDN base URL 형식. 누락/오류가 있으면 어떤 값이 잘못됐는지 명확한 메시지로 fail fast한다. 로컬 safe-mode를 제공한다면 presign endpoint를 비활성화하고 사용자에게 업로드 설정 필요 오류를 반환해야 한다.
- S3 CORS는 커뮤니티 에디터 origin을 AllowedOrigin으로 두고 `PUT`, `OPTIONS`를 허용해야 한다. AllowedHeaders는 최소 `Content-Type`, `Content-MD5`, `x-amz-*` 및 client가 보내는 custom `x-*` headers를 포함한다. 임시 credential header를 쓰는 구성은 `x-amz-security-token`도 허용한다. OPTIONS preflight가 성공해야 한다.

## API 계약 초안

기본 prefix는 기존 선물 API 관례를 따라 `/api/futures`를 사용한다. 구체 DTO 이름은 구현 시 module convention에 맞춰 정하되, 아래 의미 계약을 유지한다.

### 목록

`GET /api/futures/community/posts?category=&page=&size=`

- Auth required.
- `category`는 생략 가능하며 일반 글 목록 필터에만 적용한다.
- 응답:

```json
{
  "pinnedNotices": [
    {
      "id": 1,
      "category": "NOTICE",
      "title": "공지 제목",
      "authorNickname": "운영자",
      "viewCount": 10,
      "likeCount": 2,
      "commentCount": 0,
      "createdAt": "2026-05-13T00:00:00Z"
    }
  ],
  "posts": [],
  "page": { "page": 0, "size": 20, "totalElements": 0, "totalPages": 0, "hasNext": false }
}
```

- `pinnedNotices`는 삭제되지 않은 최신 `NOTICE` 3개를 반환한다.
- `posts`는 삭제되지 않은 일반 글을 최신순으로 반환한다.

### 상세/조회수

`GET /api/futures/community/posts/{postId}`

- Auth required.
- 삭제된 게시글은 일반 사용자에게 not found로 취급한다.
- 1차 정책은 상세 조회 때 조회수를 증가시키되 bot-driven inflation을 막기 위해 최소 throttle을 둔다. 권장 기본값은 같은 post에 대해 같은 authenticated user/session 또는 IP가 1분 안에 반복 조회해도 view count를 최대 1회만 증가시키는 정책이다. 더 긴 windowed counting은 후속 개선으로 분리할 수 있다.
- 응답은 title/category/author/contentJson/counts/viewer permissions(`canEdit`, `canDelete`, `likedByMe`)를 포함한다.

### 게시글 쓰기

- `POST /api/futures/community/posts`
- `PUT /api/futures/community/posts/{postId}`
- `DELETE /api/futures/community/posts/{postId}`

요청 본문 핵심 필드:

```json
{
  "category": "CHAT",
  "title": "제목",
  "contentJson": { "type": "doc", "content": [] },
  "imageObjectKeys": ["community/1/example.webp"]
}
```

- create/update는 title length, category 권한, content JSON whitelist, image object key ownership/prefix를 검증한다. 이미지 검증은 요청된 `objectKey`로 `community_post_images` row를 찾고, `uploader_member_id`가 요청자 `memberId`와 일치하며 `status`가 `PRESIGNED` 또는 `ATTACHED`인지 확인한다. 추가 방어로 `objectKey`가 서버 생성 prefix(`community/{memberId}/`)를 갖는지도 검증한다.
- delete는 soft delete가 기본이다.

### 댓글

- `GET /api/futures/community/posts/{postId}/comments?page=&size=`
- `POST /api/futures/community/posts/{postId}/comments`
- `DELETE /api/futures/community/posts/{postId}/comments/{commentId}`

댓글은 plain text로 시작한다. 댓글 body는 저장 전 앞뒤 공백을 trim하며, trim 후 1자 이상 1,000자 이하만 허용한다. empty 또는 whitespace-only 댓글은 validation error로 거절한다. 댓글 삭제는 soft delete가 기본이며 일반 목록에서는 숨긴다.

### 좋아요

- `POST /api/futures/community/posts/{postId}/like`
- `DELETE /api/futures/community/posts/{postId}/like`

한 회원은 한 게시글에 하나의 active like만 가질 수 있다. `POST`는 idempotent하게 동작한다. 이미 `(post_id, member_id)` like가 있으면 backend는 unique constraint 충돌을 사용자 오류로 노출하지 않고 성공 응답과 현재 count를 반환한다. like 생성/삭제와 denormalized `likeCount` 갱신은 한 트랜잭션에서 수행하며, concurrent duplicate insert는 duplicate-key 처리를 통해 client 실패로 번지지 않게 한다. `DELETE`도 이미 좋아요가 없어도 현재 상태를 성공적으로 반환하는 idempotent 흐름으로 시작한다.

### 이미지 presign

`POST /api/futures/community/images/presign`

요청:

```json
{ "fileName": "chart.webp", "contentType": "image/webp", "sizeBytes": 12345 }
```

응답:

```json
{
  "uploadUrl": "https://s3-presigned-url.example",
  "objectKey": "community/1/uuid.webp",
  "publicUrl": "https://cdn.example/community/1/uuid.webp",
  "contentType": "image/webp",
  "expiresAt": "2026-05-13T00:10:00Z",
  "maxBytes": 5242880
}
```

## DB 계약 초안

실제 migration은 backend storage PR에서 추가한다. PR-1은 테이블 의미와 인덱스 기준만 고정한다.

### `community_posts`

- `id` BIGINT PK
- `author_member_id` BIGINT NOT NULL
- `author_nickname` VARCHAR NOT NULL
  - 작성 시점 닉네임 snapshot이다. 회원 닉네임 변경은 과거 `community_posts.author_nickname` 값을 소급 변경하지 않는다. live nickname sync가 필요하면 후속 명세에서 `members` join 정책으로 바꾼다.
- `category` VARCHAR(32) NOT NULL
- `title` VARCHAR(200) NOT NULL
- `content_json` LONGTEXT 또는 DB 호환 JSON 저장 타입
- `view_count`, `like_count`, `comment_count` BIGINT/INT NOT NULL DEFAULT 0
- `deleted_at` nullable
- `created_at`, `updated_at`, `version`
- Indexes: `(category, deleted_at, created_at)`, `(deleted_at, created_at)`, `(author_member_id, created_at)`

### `community_comments`

- `id` BIGINT PK
- `post_id` BIGINT NOT NULL
- `author_member_id` BIGINT NOT NULL
- `author_nickname` VARCHAR NOT NULL
  - 작성 시점 닉네임 snapshot이다. 회원 닉네임 변경은 과거 `community_comments.author_nickname` 값을 소급 변경하지 않는다. live nickname sync가 필요하면 후속 명세에서 `members` join 정책으로 바꾼다.
- `content` VARCHAR/TEXT NOT NULL
- `deleted_at` nullable
- `created_at`, `updated_at`
- Indexes: `(post_id, deleted_at, created_at)`, `(author_member_id, created_at)`

### `community_post_likes`

- `post_id` BIGINT NOT NULL
- `member_id` BIGINT NOT NULL
- `created_at`
- Unique: `(post_id, member_id)`

### `community_post_images` (권장)

- `id` BIGINT PK
- `post_id` nullable until attach
- `uploader_member_id` BIGINT NOT NULL
- `object_key` VARCHAR UNIQUE NOT NULL
- `public_url` VARCHAR NOT NULL
- `content_type` VARCHAR NOT NULL
- `size_bytes` BIGINT NOT NULL
- `status` VARCHAR(32) NOT NULL (`PRESIGNED`, `ATTACHED`, `ORPHANED` 등)
- `created_at`, `updated_at`

### Orphaned image cleanup

이미지 lifecycle은 게시글 저장 안정성을 우선한다. `PRESIGNED` 상태가 `24h` 이상 게시글에 attach되지 않으면 cleanup 후보가 된다. 게시글에서 제거되거나 저장 실패로 연결이 끊긴 이미지는 `ORPHANED` 상태로 표시하고 기본 `30d` 보관 뒤 삭제한다. TTL과 보관 기간은 환경값으로 조정할 수 있다.

정리 작업은 scheduled job 또는 운영 배치로 수행하며, `community_post_images.status`와 `created_at`/`updated_at` threshold로 후보를 찾는다. 작업은 underlying S3 object(`object_key`) 삭제 후 DB row 삭제 또는 상태 전이를 수행하고, 재시도 가능한 idempotent 방식이어야 한다. 로그와 metric에는 object key, status, uploader_member_id, 결과만 남기며 presigned URL이나 credential은 남기지 않는다. S3 삭제와 DB 갱신 사이 장애로 dangling state가 생기지 않도록 two-phase deletion 또는 재시도 가능한 보상 경로를 둔다.

## 프론트엔드 계약

- 서버 상태는 React Query로 관리한다.
- 커뮤니티 서버 데이터를 Zustand에 복제하지 않는다.
- 모든 커뮤니티 API helper는 `response.ok` 확인 뒤 JSON을 파싱한다.
- route files는 얇게 유지하고 route-bound component는 `frontend/components/router/(main)/community/` 아래에 둔다.
- Tiptap editor와 image upload UI는 Client Component다.
- 목록/상세는 loading, empty, error, unauthenticated 상태를 명시한다.
- write/edit 화면은 `docs/design-docs/ui-design/image/post_ex.png`의 큰 제목 입력, 말머리 선택, 툴바, 본문 placeholder 방향을 따르되 기존 금융 대시보드 카드/입력/포커스 규칙을 우선한다.

## 수용 기준

1. 로그인 사용자는 `/community`에서 최신 공지 3개와 일반 글 목록, 카테고리 말머리를 볼 수 있다.
2. 비로그인 사용자는 커뮤니티 화면/API를 사용할 수 없다.
3. 일반 사용자는 공지사항을 생성하거나 공지 카테고리로 바꿀 수 없다.
4. 관리자는 공지사항을 생성/수정할 수 있고 최신 공지 3개가 pinned 영역에 나타난다.
5. 작성자는 자기 게시글을 수정/삭제할 수 있고 다른 사용자 글은 수정/삭제할 수 없다.
6. 관리자는 모든 게시글과 댓글을 삭제할 수 있다.
7. Tiptap JSON 본문이 저장, 재조회, 렌더링 round trip을 통과한다.
8. 에디터 이미지는 backend presign과 S3 PUT을 통해 삽입되고, 저장 시 backend-approved object key만 허용된다.
9. 댓글 작성/삭제와 좋아요/좋아요 취소가 count 일관성을 유지한다.
10. 상세 조회 시 선택한 단순 조회수 정책에 따라 view count가 증가한다.

## 후속 PR 경계

- PR-2: backend core/storage domain, repository contract, Flyway migration, schema docs.
- PR-3: backend app/external HTTP API와 S3 presign adapter.
- PR-4: frontend read path.
- PR-5: frontend write/editor/interactions.
- PR-6: final hardening and end-to-end verification.
