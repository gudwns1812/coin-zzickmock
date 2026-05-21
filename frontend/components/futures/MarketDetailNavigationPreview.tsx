"use client";

import { getMarketLogoPath, MARKET_SNAPSHOTS } from "@/lib/markets";
import Image from "next/image";

const PREVIEW_MARKET = MARKET_SNAPSHOTS.BTCUSDT;

export default function MarketDetailNavigationPreview() {
  return (
    <div
      aria-hidden="true"
      className="fixed inset-x-0 bottom-0 top-[78px] z-40 overflow-y-auto bg-white"
    >
      <div className="min-w-[1000px] px-main-3 pb-24 pt-2">
        <div className="grid grid-cols-[minmax(0,1fr)_360px] gap-main-2">
          <section className="flex flex-col gap-main-2">
            <div className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
              <div className="mb-main flex gap-2">
                <span className="rounded-full border border-main-blue bg-main-blue px-4 py-2 text-sm-custom font-semibold text-white">
                  BTCUSDT
                </span>
                <span className="rounded-full border border-main-light-gray bg-white px-4 py-2 text-sm-custom font-semibold text-main-dark-gray/60">
                  ETHUSDT
                </span>
              </div>

              <div className="flex items-start justify-between gap-main-2">
                <div>
                  <p className="text-xs-custom font-semibold uppercase tracking-[0.2em] text-main-dark-gray/50">
                    {PREVIEW_MARKET.symbol}
                  </p>
                  <div className="mt-2 flex items-center gap-main">
                    <Image
                      alt="Bitcoin logo"
                      className="rounded-full"
                      height={40}
                      src={getMarketLogoPath(PREVIEW_MARKET.symbol)}
                      width={40}
                    />
                    <h1 className="text-3xl-custom font-bold text-main-dark-gray">
                      {PREVIEW_MARKET.displayName}
                    </h1>
                  </div>
                </div>
                <div className="rounded-main bg-main-light-gray/50 px-main py-3 text-right text-main-dark-gray/55">
                  <p className="text-xs-custom font-semibold uppercase">24H</p>
                  <p className="mt-2 text-lg-custom font-bold">&nbsp;</p>
                </div>
              </div>

              <div className="mt-6 grid grid-cols-5 gap-main">
                {[
                  "최신 체결가",
                  "Mark Price",
                  "Index Price",
                  "Funding",
                  "24h 거래대금",
                ].map((label) => (
                  <div
                    className="rounded-main bg-main-light-gray/35 px-main py-3"
                    key={label}
                  >
                    <p className="text-xs-custom text-main-dark-gray/55">
                      {label}
                    </p>
                    <p className="mt-2 h-5 rounded-full bg-main-light-gray/80" />
                  </div>
                ))}
              </div>
            </div>

            <div className="min-h-[420px] rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
              <div className="h-full min-h-[360px] rounded-main bg-main-light-gray/25" />
            </div>
          </section>

          <aside className="rounded-main border border-main-light-gray bg-white p-main-2 shadow-sm">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-sm-custom font-semibold text-main-dark-gray/60">
                  주문
                </p>
                <h2 className="mt-1 text-xl-custom font-bold text-main-dark-gray">
                  BTCUSDT <span className="font-medium">Perpetual</span>
                </h2>
              </div>
              <span className="rounded-full bg-main-light-gray/70 px-3 py-1 text-sm-custom text-main-dark-gray/55">
                &nbsp;
              </span>
            </div>

            <div className="mt-main-2 grid grid-cols-2 gap-main">
              {["Open", "Close", "Limit", "Market"].map((label) => (
                <div
                  className="rounded-main bg-main-light-gray/50 px-main py-3 text-center text-sm-custom font-semibold text-main-dark-gray/65"
                  key={label}
                >
                  {label}
                </div>
              ))}
            </div>

            <div className="mt-main-2 space-y-main">
              {["Price", "Quantity", "Futures value", "Cost"].map((label) => (
                <div key={label}>
                  <p className="text-xs-custom text-main-dark-gray/55">
                    {label}
                  </p>
                  <div className="mt-2 h-11 rounded-main bg-main-light-gray/45" />
                </div>
              ))}
            </div>
          </aside>
        </div>
      </div>
    </div>
  );
}
