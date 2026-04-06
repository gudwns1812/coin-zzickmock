"use client";

import { ChevronLeft } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

export default function ClientLayoutWrapper({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const [dragDistance, setDragDistance] = useState(0);
  const [shouldShowIcon, setShouldShowIcon] = useState(false);
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);

  const MAX_DRAG = 120;
  const TRIGGER_THRESHOLD = 30;

  const handleEnd = () => {
    if (dragDistance >= TRIGGER_THRESHOLD && shouldShowIcon) {
      router.back();
    }
    setDragDistance(0);
    setShouldShowIcon(false);
  };

  const handleWheel = (e: React.WheelEvent<HTMLDivElement>) => {
    const deltaX = e.deltaX;

    if (deltaX < 0) {
      // ✅ 감쇠된 델타값 사용
      const dampenedDelta = Math.sqrt(Math.abs(deltaX)) * 2;
      const updated = Math.min(MAX_DRAG, dragDistance + dampenedDelta);
      setDragDistance(updated);
      setShouldShowIcon(true);
    }

    if (deltaX > 0) {
      setDragDistance(0);
      setShouldShowIcon(false);
    }

    if (Math.abs(deltaX) < 0.5) {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
      timeoutRef.current = setTimeout(handleEnd, 120);
    }
  };

  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, []);

  // 아이콘도 감쇠 거리로 이동
  const dampenedDistance = Math.sqrt(dragDistance) * 10;

  return (
    <div
      className="w-full h-full relative overflow-x-hidden"
      style={{ touchAction: "auto" }}
      onWheel={handleWheel}
    >
      {/* 아이콘 */}
      <div
        className="absolute top-1/2 -translate-y-1/2 left-0 z-50 transition-all duration-200 ease-out pointer-events-none"
        style={{
          transform: `translateX(${dampenedDistance}px)`,
          opacity: shouldShowIcon ? 1 : 0,
        }}
      >
        <ChevronLeft
          size={48}
          className="bg-main-blue/10 text-main-blue rounded-full p-2"
        />
      </div>

      {children}
    </div>
  );
}
