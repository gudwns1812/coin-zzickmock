package coin.coinzzickmock.feature.reward.application.service;

import coin.coinzzickmock.feature.reward.application.result.ShopItemResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetShopItemsService {
    public List<ShopItemResult> getItems() {
        return List.of(
                new ShopItemResult("badge.basic", "프로필 배지", 10, "닉네임 옆에 붙는 기본 배지"),
                new ShopItemResult("theme.cyan", "대시보드 테마", 30, "마켓 화면 강조 색상 테마"),
                new ShopItemResult("title.shark", "칭호", 50, "프로필과 헤더에 표시되는 칭호")
        );
    }
}
