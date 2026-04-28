package coin.coinzzickmock.providers.infrastructure;

public record BitgetTickerData(
        String symbol,
        String lastPr,
        String change24h,
        String indexPrice,
        String fundingRate,
        String markPrice,
        String usdtVolume
) {
}
