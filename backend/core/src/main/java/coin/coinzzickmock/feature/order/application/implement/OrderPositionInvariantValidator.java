package coin.coinzzickmock.feature.order.application.implement;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.order.application.dto.CreateOrderCommand;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPositionInvariantValidator {
    private final PositionRepository positionRepository;

    public void validateOpenPositionCompatibility(CreateOrderCommand command) {
        Optional<PositionSnapshot> existing = positionRepository.findOpenPosition(
                command.memberId(),
                command.symbol(),
                command.positionSide()
        );
        if (existing.isPresent()) {
            PositionSnapshot position = existing.orElseThrow();
            if (!position.marginMode().equalsIgnoreCase(command.marginMode())) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
        }

        Optional<PositionSnapshot> symbolMarginPosition = positionRepository.findOpenPositions(command.memberId())
                .stream()
                .filter(candidate -> candidate.symbol().equalsIgnoreCase(command.symbol()))
                .filter(candidate -> candidate.marginMode().equalsIgnoreCase(command.marginMode()))
                .findFirst();
        if (symbolMarginPosition.map(PositionSnapshot::leverage).orElse(command.leverage()) != command.leverage()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
