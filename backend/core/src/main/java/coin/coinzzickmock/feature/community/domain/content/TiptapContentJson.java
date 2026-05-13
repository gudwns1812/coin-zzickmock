package coin.coinzzickmock.feature.community.domain.content;

import java.util.Objects;

public final class TiptapContentJson {
    private final String value;
    private final TiptapContentValidationResult validationResult;

    public TiptapContentJson(String value) {
        this(TiptapContentValidator.validate(value, TiptapContentPolicy.withoutImages()));
    }

    private TiptapContentJson(TiptapContentValidator.Validation validation) {
        Objects.requireNonNull(validation, "validation");
        this.value = validation.json();
        this.validationResult = validation.result();
    }

    public static TiptapContentJson of(String value, TiptapContentPolicy policy) {
        return new TiptapContentJson(TiptapContentValidator.validate(value, policy));
    }

    public String value() {
        return value;
    }

    public TiptapContentValidationResult validationResult() {
        return validationResult;
    }
}
