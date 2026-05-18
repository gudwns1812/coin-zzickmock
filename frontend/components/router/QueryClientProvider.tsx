"use client";

import { FUTURES_AUTH_CHANGED_EVENT } from "@/lib/futures-auth-state";
import { personalizedQueryKeyPrefixes } from "@/lib/futures-query-keys";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactNode, useEffect, useState } from "react";

export default function Providers({ children }: { children: ReactNode }) {
  const [client] = useState(() => new QueryClient());

  useEffect(() => {
    const clearPersonalizedQueries = () => {
      for (const queryKey of personalizedQueryKeyPrefixes) {
        client.removeQueries({ queryKey });
      }
    };

    window.addEventListener(
      FUTURES_AUTH_CHANGED_EVENT,
      clearPersonalizedQueries
    );

    return () => {
      window.removeEventListener(
        FUTURES_AUTH_CHANGED_EVENT,
        clearPersonalizedQueries
      );
    };
  }, [client]);

  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}
