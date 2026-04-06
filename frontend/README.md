# frontend

`주식 찍먹` 프론트엔드 워크스페이스입니다.

## Stack

- Next.js 15
- React 19
- TypeScript
- Tailwind CSS 4
- React Query
- Zustand

## Routes

- `/stock`
- `/stock/[code]`
- `/portfolio`
- `/signup`
- `/only-desktop`

## Commands

루트에서 실행:

```bash
npm install
npm run dev
npm run build
```

워크스페이스만 직접 실행:

```bash
npm run dev --workspace frontend
npm run build --workspace frontend
```

## Environment Variables

- `NEXT_PUBLIC_BASE_URL`
- `NEXT_PUBLIC_BASE_URL2`
- `JWT_SECRET`
- `NEXT_PUBLIC_API_MOCKING=enabled` (선택, MSW 사용 시)

## Notes

- 뉴스, 캘린더, 챗봇 관련 프론트 코드는 제거되었습니다.
- Spring Boot 백엔드는 이후 루트의 `backend/` 위치에 추가될 예정입니다.
