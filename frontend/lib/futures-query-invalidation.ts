import type { QueryClient } from "@tanstack/react-query";
import { futuresQueryKeys } from "@/lib/futures-query-keys";

export function invalidateTradingQueries(queryClient: QueryClient) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: futuresQueryKeys.account }),
    queryClient.invalidateQueries({ queryKey: futuresQueryKeys.positions }),
    queryClient.invalidateQueries({ queryKey: futuresQueryKeys.openOrders }),
    queryClient.invalidateQueries({ queryKey: futuresQueryKeys.orderHistory }),
    queryClient.invalidateQueries({ queryKey: futuresQueryKeys.positionHistory }),
  ]);
}

export function invalidateRewardAndShopQueries(queryClient: QueryClient) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: futuresQueryKeys.reward }),
    queryClient.invalidateQueries({ queryKey: futuresQueryKeys.rewardHistory }),
    queryClient.invalidateQueries({ queryKey: futuresQueryKeys.rewardRedemptions }),
    queryClient.invalidateQueries({ queryKey: futuresQueryKeys.rewardShopHistory }),
    queryClient.invalidateQueries({ queryKey: futuresQueryKeys.shopItems }),
    queryClient.invalidateQueries({ queryKey: futuresQueryKeys.shopMe }),
    queryClient.invalidateQueries({ queryKey: futuresQueryKeys.refillStatus }),
  ]);
}

export function invalidateCommunityQueries(queryClient: QueryClient) {
  return queryClient.invalidateQueries({ queryKey: futuresQueryKeys.community });
}
