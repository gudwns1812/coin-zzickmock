"use client";

import { ensureMswReady } from "@/lib/msw-ready";
import { useEffect, type ReactNode } from "react";

export const MSWProvider = ({ children }: { children: ReactNode }) => {
  useEffect(() => {
    void ensureMswReady();
  }, []);

  return <>{children}</>;
};
