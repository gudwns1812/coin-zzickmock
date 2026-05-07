"use client";

import {
  DEFAULT_EVENT_SOURCE_BASE_RECONNECT_MS,
  DEFAULT_EVENT_SOURCE_MAX_RECONNECT_MS,
  getEventSourceReconnectDelayMs,
  shouldDeferEventSourceReconnect,
  shouldForceReconnectOnVisible,
  type EventSourceReconnectReason,
  type EventSourceReconnectStatus,
} from "@/hooks/resilientEventSourcePolicy";
import { useCallback, useEffect, useRef, useState } from "react";

type EventSourceFactory = (url: string) => EventSource;

type UseResilientEventSourceOptions = {
  url: string | null;
  enabled?: boolean;
  onMessage: (event: MessageEvent) => void;
  onOpen?: (event: Event) => void;
  onError?: (event: Event) => void;
  onReconnect?: (reason: EventSourceReconnectReason) => void;
  baseReconnectDelayMs?: number;
  eventSourceFactory?: EventSourceFactory;
  maxReconnectDelayMs?: number;
  reconnectOnFocus?: boolean;
  reconnectOnOnline?: boolean;
  reconnectOnVisible?: boolean;
};

type EventSourceCallbacks = Pick<
  UseResilientEventSourceOptions,
  "onError" | "onMessage" | "onOpen" | "onReconnect"
>;

function getDocumentVisibilityState() {
  if (typeof document === "undefined") {
    return "unknown" as const;
  }

  return document.visibilityState;
}

function createDefaultEventSource(url: string) {
  return new EventSource(url);
}

export function useResilientEventSource({
  url,
  enabled = true,
  onMessage,
  onOpen,
  onError,
  onReconnect,
  baseReconnectDelayMs = DEFAULT_EVENT_SOURCE_BASE_RECONNECT_MS,
  eventSourceFactory = createDefaultEventSource,
  maxReconnectDelayMs = DEFAULT_EVENT_SOURCE_MAX_RECONNECT_MS,
  reconnectOnFocus = false,
  reconnectOnOnline = true,
  reconnectOnVisible = true,
}: UseResilientEventSourceOptions) {
  const callbacksRef = useRef<EventSourceCallbacks>({
    onError,
    onMessage,
    onOpen,
    onReconnect,
  });
  const attemptRef = useRef(0);
  const generationRef = useRef(0);
  const reconnectTimerRef = useRef<number | null>(null);
  const streamRef = useRef<EventSource | null>(null);
  const urlRef = useRef(url);
  const enabledRef = useRef(enabled);
  const initialStatus: EventSourceReconnectStatus =
    enabled && url ? "connecting" : "idle";
  const statusRef = useRef<EventSourceReconnectStatus>(initialStatus);
  const hiddenAtRef = useRef<number | null>(null);
  const [status, setStatus] =
    useState<EventSourceReconnectStatus>(initialStatus);

  const updateStatus = useCallback((nextStatus: EventSourceReconnectStatus) => {
    statusRef.current = nextStatus;
    setStatus(nextStatus);
  }, []);

  useEffect(() => {
    callbacksRef.current = {
      onError,
      onMessage,
      onOpen,
      onReconnect,
    };
  }, [onError, onMessage, onOpen, onReconnect]);

  useEffect(() => {
    urlRef.current = url;
    enabledRef.current = enabled;
  }, [enabled, url]);

  const clearReconnectTimer = useCallback(() => {
    if (reconnectTimerRef.current === null) {
      return;
    }

    window.clearTimeout(reconnectTimerRef.current);
    reconnectTimerRef.current = null;
  }, []);

  const closeCurrentStream = useCallback(() => {
    generationRef.current += 1;
    const stream = streamRef.current;
    streamRef.current = null;

    if (stream) {
      stream.close();
    }
  }, []);

  const openStream = useCallback(
    (reason?: EventSourceReconnectReason) => {
      const nextUrl = urlRef.current;

      if (!enabledRef.current || !nextUrl) {
        updateStatus("idle");
        return;
      }

      clearReconnectTimer();
      closeCurrentStream();

      const generation = generationRef.current + 1;
      generationRef.current = generation;
      updateStatus(attemptRef.current > 0 ? "reconnecting" : "connecting");

      if (reason) {
        callbacksRef.current.onReconnect?.(reason);
      }

      const stream = eventSourceFactory(nextUrl);
      streamRef.current = stream;

      stream.onopen = (event) => {
        if (generationRef.current !== generation) {
          return;
        }

        attemptRef.current = 0;
        updateStatus("open");
        callbacksRef.current.onOpen?.(event);
      };

      stream.onmessage = (event) => {
        if (generationRef.current !== generation) {
          return;
        }

        callbacksRef.current.onMessage(event);
      };

      stream.onerror = (event) => {
        if (generationRef.current !== generation) {
          return;
        }

        callbacksRef.current.onError?.(event);
        closeCurrentStream();

        if (
          shouldDeferEventSourceReconnect({
            documentVisibilityState: getDocumentVisibilityState(),
          })
        ) {
          updateStatus("degraded");
          return;
        }

        updateStatus("reconnecting");
        const delayMs = getEventSourceReconnectDelayMs({
          attempt: attemptRef.current,
          baseDelayMs: baseReconnectDelayMs,
          maxDelayMs: maxReconnectDelayMs,
        });
        attemptRef.current += 1;
        callbacksRef.current.onReconnect?.("error");
        reconnectTimerRef.current = window.setTimeout(() => {
          openStream();
        }, delayMs);
      };
    },
    [
      baseReconnectDelayMs,
      clearReconnectTimer,
      closeCurrentStream,
      eventSourceFactory,
      maxReconnectDelayMs,
      updateStatus,
    ]
  );

  const reconnect = useCallback(
    (reason: EventSourceReconnectReason = "manual") => {
      attemptRef.current = 0;
      openStream(reason);
    },
    [openStream]
  );

  useEffect(() => {
    if (!enabled || !url) {
      clearReconnectTimer();
      closeCurrentStream();
      updateStatus("idle");
      return;
    }

    attemptRef.current = 0;
    openStream();

    return () => {
      clearReconnectTimer();
      closeCurrentStream();
    };
  }, [
    clearReconnectTimer,
    closeCurrentStream,
    enabled,
    openStream,
    updateStatus,
    url,
  ]);

  useEffect(() => {
    if (typeof window === "undefined" || typeof document === "undefined") {
      return;
    }

    const handleVisibilityChange = () => {
      if (document.visibilityState === "hidden") {
        hiddenAtRef.current = Date.now();
        return;
      }

      const hiddenAtMs = hiddenAtRef.current;
      const hiddenDurationMs =
        hiddenAtMs === null ? null : Date.now() - hiddenAtMs;
      hiddenAtRef.current = null;

      if (
        reconnectOnVisible &&
        document.visibilityState === "visible" &&
        shouldForceReconnectOnVisible({
          hiddenDurationMs,
          status: statusRef.current,
        })
      ) {
        reconnect("visible");
      }
    };
    const handleOnline = () => {
      if (reconnectOnOnline) {
        reconnect("online");
      }
    };
    const handleFocus = () => {
      if (reconnectOnFocus) {
        reconnect("focus");
      }
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);
    window.addEventListener("online", handleOnline);
    window.addEventListener("focus", handleFocus);

    return () => {
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("focus", handleFocus);
    };
  }, [reconnect, reconnectOnFocus, reconnectOnOnline, reconnectOnVisible]);

  return {
    reconnect,
    status,
  };
}
