package coin.coinzzickmock.feature.member.web;

public record RegisterMemberRequest(
        String account,
        String password,
        String name,
        String nickname,
        String phoneNumber,
        String email,
        String fgOffset,
        AddressRequest address
) {
    public record AddressRequest(
            String zipcode,
            String address,
            String addressDetail
    ) {
    }
}
