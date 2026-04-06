package stock.stockzzickmock.storage.redis.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HolidayToRedis {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic topic; // "stock-channel" 이 주입됨



    public void publish(String message) {
        redisTemplate.convertAndSend(topic.getTopic(), message);
    }

}
