package coin.coinzzickmock.feature.community.domain.content;

public record TiptapContentJson(String value) {
    public static TiptapContentJson of(String value, TiptapContentPolicy policy) {
        return new TiptapContentJson(TiptapContentValidator.validate(value, policy).json());
    }

    public TiptapContentValidationResult validationResult() {
        return TiptapContentValidator.validate(value, TiptapContentPolicy.withoutImages()).result();
    }
}
