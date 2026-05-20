"use client";

import {
  FUTURES_AUTH_CHANGED_EVENT,
  type FuturesAuthChangeAction,
} from "@/lib/futures-auth-state";
import {
  futuresQueryKeys,
  personalizedQueryKeyPrefixes,
} from "@/lib/futures-query-keys";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactNode, useEffect, useState } from "react";

export default function Providers({ children }: { children: ReactNode }) {
  const [client] = useState(() => new QueryClient());

  useEffect(() => {
    const removePersonalizedQueries = () => {
      for (const queryKey of personalizedQueryKeyPrefixes) {
        client.removeQueries({ queryKey });
      }
    };

    const applyAuthCachePolicy = (event: Event) => {
      const action = getAuthChangeAction(event);

      if (action === "login") {
        removePersonalizedQueries();
        void client.invalidateQueries({ queryKey: futuresQueryKeys.authMe });
        return;
      }

      if (action === "logout" || action === "withdraw") {
        client.setQueryData(futuresQueryKeys.authMe, null);
        removePersonalizedQueries();
        return;
      }

      void client.invalidateQueries({ queryKey: futuresQueryKeys.authMe });
    };

    window.addEventListener(
      FUTURES_AUTH_CHANGED_EVENT,
      applyAuthCachePolicy
    );

    return () => {
      window.removeEventListener(
        FUTURES_AUTH_CHANGED_EVENT,
        applyAuthCachePolicy
      );
    };
  }, [client]);

  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

function getAuthChangeAction(event: Event): FuturesAuthChangeAction {
  if (event instanceof CustomEvent && isAuthChangeAction(event.detail?.action)) {
    return event.detail.action;
  }

  return "refresh";
}

function isAuthChangeAction(
  action: unknown
): action is FuturesAuthChangeAction {
  return (
    action === "login" ||
    action === "logout" ||
    action === "withdraw" ||
    action === "refresh"
  );
}
