import { getFuturesReward, getShopItems } from "@/lib/futures-api";

export default async function ShopPage() {
  const [reward, shopItems] = await Promise.all([
    getFuturesReward(),
    getShopItems(),
  ]);

  return (
    <div className="px-main-2 pb-24 flex flex-col gap-8 pt-4">
      <section className="grid grid-cols-[1.2fr_1fr] gap-main-2">
        <div className="rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray">
          <p className="text-sm-custom text-main-dark-gray/60">Reward Shop</p>
          <h1 className="mt-2 text-3xl-custom font-bold text-main-dark-gray">
            포인트 상점
          </h1>
          <p className="mt-3 text-sm-custom text-main-dark-gray/70 break-keep">
            실현 손익으로 모은 포인트를 cosmetic 아이템으로 바꾸는 공간입니다.
            투자 성능에 직접 영향을 주는 아이템은 두지 않습니다.
          </p>
        </div>

        <div className="rounded-main bg-gradient-to-br from-main-blue to-cyan-500 text-white p-main-2 shadow-sm">
          <p className="text-sm-custom text-white/70">현재 포인트</p>
          <h2 className="mt-3 text-4xl-custom font-bold">
            {reward.rewardPoint} P
          </h2>
          <p className="mt-3 text-sm-custom text-white/80 break-keep">
            포인트는 순실현 손익이 발생한 뒤 적립됩니다. MVP 1차는 고정 구간
            지급 규칙으로 시작합니다.
          </p>
        </div>
      </section>

      <section className="grid grid-cols-3 gap-main-2">
        {shopItems.map((item) => (
          <article
            key={item.code}
            className="rounded-main bg-white p-main-2 shadow-sm border border-main-light-gray flex flex-col gap-4"
          >
            <div className="rounded-main bg-main-light-gray/40 min-h-[140px]" />
            <div>
              <h2 className="text-xl-custom font-semibold text-main-dark-gray">
                {item.name}
              </h2>
              <p className="mt-2 text-sm-custom text-main-dark-gray/60 break-keep">
                {item.description}
              </p>
            </div>
            <div className="mt-auto flex items-center justify-between">
              <span className="text-lg-custom font-bold text-main-blue">
                {item.price} P
              </span>
              <button className="rounded-main border border-main-blue px-main py-2 text-main-blue font-semibold">
                준비 중
              </button>
            </div>
          </article>
        ))}
      </section>
    </div>
  );
}
