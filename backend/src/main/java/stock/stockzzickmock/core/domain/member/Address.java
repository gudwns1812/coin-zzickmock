package stock.stockzzickmock.core.domain.member;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Address {
    private final String zipCode;
    private final String address;
    private final String addressDetail;

    public static Address of(String zipCode, String address, String addressDetail) {
        return Address.builder()
                .zipCode(zipCode)
                .address(address)
                .addressDetail(addressDetail)
                .build();
    }
}
