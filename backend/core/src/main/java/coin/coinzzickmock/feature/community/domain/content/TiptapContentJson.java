package coin.coinzzickmock.feature.community.domain.content;

public record TiptapContentJson(String value) {
    public TiptapContentJson {
        TiptapContentValidator.validate(value);
    }

    public static TiptapContentJson from(String value) {
        return new TiptapContentJson(value);
    }
}
