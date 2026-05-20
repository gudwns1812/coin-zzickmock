let mswReadyPromise: Promise<void> | null = null;

export function isMockServiceWorkerEnabled() {
  return (
    typeof window !== "undefined" &&
    process.env.NEXT_PUBLIC_API_MOCKING === "enabled"
  );
}

export function ensureMswReady(): Promise<void> {
  if (!isMockServiceWorkerEnabled()) {
    return Promise.resolve();
  }

  if (!mswReadyPromise) {
    mswReadyPromise = import("@/mocks/browser")
      .then(async ({ worker }) => {
        await worker.start({
          onUnhandledRequest: "bypass",
        });
      })
      .catch((error: unknown) => {
        console.warn(
          "[MSW] Failed to start mock service worker; continuing without request interception.",
          error
        );
      });
  }

  return mswReadyPromise;
}
