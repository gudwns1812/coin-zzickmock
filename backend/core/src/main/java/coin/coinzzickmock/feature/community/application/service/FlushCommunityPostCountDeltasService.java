package coin.coinzzickmock.feature.community.application.service;

import coin.coinzzickmock.feature.community.application.dto.CommunityPostCountDelta;
import coin.coinzzickmock.feature.community.application.implement.CommunityPostCountDeltaBuffer;
import coin.coinzzickmock.feature.community.application.repository.CommunityPostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FlushCommunityPostCountDeltasService {
    private final CommunityPostCountDeltaBuffer countDeltaBuffer;
    private final CommunityPostRepository communityPostRepository;

    @Transactional
    public int flush() {
        List<CommunityPostCountDelta> deltas = countDeltaBuffer.drain();
        if (deltas.isEmpty()) {
            return 0;
        }
        try {
            communityPostRepository.applyCountDeltas(deltas);
            return deltas.size();
        } catch (RuntimeException exception) {
            countDeltaBuffer.restore(deltas);
            throw exception;
        }
    }
}
