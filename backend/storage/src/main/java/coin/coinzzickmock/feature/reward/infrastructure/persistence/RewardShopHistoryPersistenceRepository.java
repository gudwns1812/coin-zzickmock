package coin.coinzzickmock.feature.reward.infrastructure.persistence;

import coin.coinzzickmock.feature.reward.application.repository.RewardShopHistoryRepository;
import coin.coinzzickmock.feature.reward.application.result.RewardShopHistoryKind;
import coin.coinzzickmock.feature.reward.application.result.RewardShopHistoryResult;
import coin.coinzzickmock.feature.reward.domain.RewardRedemptionStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RewardShopHistoryPersistenceRepository implements RewardShopHistoryRepository {
    private static final String HISTORY_SQL = """
            SELECT 'INSTANT_PURCHASE' AS kind,
                   purchase.purchase_id AS entry_id,
                   purchase.item_code AS item_code,
                   purchase.item_name AS item_name,
                   purchase.item_type AS item_type,
                   purchase.point_amount AS point_amount,
                   purchase.quantity AS quantity,
                   purchase.purchased_at AS event_at,
                   NULL AS submitted_phone_number,
                   NULL AS status,
                   purchase.purchased_at AS purchased_at,
                   NULL AS requested_at,
                   NULL AS sent_at,
                   NULL AS cancelled_at,
                   2 AS sort_kind_priority,
                   purchase.id AS sort_sequence
              FROM reward_shop_purchases purchase
             WHERE purchase.member_id = ?
             UNION ALL
            SELECT 'REDEMPTION_REQUEST' AS kind,
                   redemption.request_id AS entry_id,
                   redemption.item_code AS item_code,
                   redemption.item_name AS item_name,
                   item.item_type AS item_type,
                   redemption.point_amount AS point_amount,
                   1 AS quantity,
                   redemption.requested_at AS event_at,
                   redemption.submitted_phone_number AS submitted_phone_number,
                   redemption.status AS status,
                   NULL AS purchased_at,
                   redemption.requested_at AS requested_at,
                   redemption.sent_at AS sent_at,
                   redemption.cancelled_at AS cancelled_at,
                   1 AS sort_kind_priority,
                   redemption.id AS sort_sequence
              FROM reward_redemption_requests redemption
              JOIN reward_shop_items item
                ON item.id = redemption.shop_item_id
             WHERE redemption.member_id = ?
             ORDER BY event_at DESC, sort_kind_priority DESC, sort_sequence DESC
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<RewardShopHistoryResult> findByMemberId(Long memberId) {
        return jdbcTemplate.query(HISTORY_SQL, (resultSet, rowNumber) -> new RewardShopHistoryResult(
                RewardShopHistoryKind.valueOf(resultSet.getString("kind")),
                resultSet.getString("entry_id"),
                resultSet.getString("item_code"),
                resultSet.getString("item_name"),
                resultSet.getString("item_type"),
                resultSet.getInt("point_amount"),
                resultSet.getInt("quantity"),
                instant(resultSet.getTimestamp("event_at")),
                resultSet.getString("submitted_phone_number"),
                redemptionStatus(resultSet.getString("status")),
                instant(resultSet.getTimestamp("purchased_at")),
                instant(resultSet.getTimestamp("requested_at")),
                instant(resultSet.getTimestamp("sent_at")),
                instant(resultSet.getTimestamp("cancelled_at"))
        ), memberId, memberId);
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static RewardRedemptionStatus redemptionStatus(String status) {
        return status == null ? null : RewardRedemptionStatus.valueOf(status);
    }
}
