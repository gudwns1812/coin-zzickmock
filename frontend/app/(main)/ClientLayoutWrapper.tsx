"use client";

export default function ClientLayoutWrapper({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="w-full h-full relative overflow-x-hidden">{children}</div>
  );
}
