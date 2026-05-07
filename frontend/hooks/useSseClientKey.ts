"use client";

import { getOrCreateTabSseClientKey } from "@/lib/sse-client-key";
import { useEffect, useState } from "react";

export function useSseClientKey() {
  const [clientKey, setClientKey] = useState<string | null>(null);

  useEffect(() => {
    setClientKey(getOrCreateTabSseClientKey());
  }, []);

  return clientKey;
}
