package coin.coinzzickmock.feature.reward.domain;

import java.util.List;

public final class RewardShopCatalog {
    private static final List<RewardShopItem> DEFAULT_ITEMS = List.of(
            new RewardShopItem("badge.basic", "프로필 배지", 10, "닉네임 옆에 붙는 기본 배지"),
            new RewardShopItem("theme.cyan", "대시보드 테마", 30, "마켓 화면 강조 색상 테마"),
            new RewardShopItem("title.shark", "칭호", 50, "프로필과 헤더에 표시되는 칭호")
    );

    private RewardShopCatalog() {
    }

    public static List<RewardShopItem> defaultItems() {
        return DEFAULT_ITEMS;
    }
}
