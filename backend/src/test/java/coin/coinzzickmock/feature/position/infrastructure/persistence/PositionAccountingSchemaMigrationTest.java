package coin.coinzzickmock.feature.position.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.CoinZzickmockApplication;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
class PositionAccountingSchemaMigrationTest {
    @Autowired
    private DataSource dataSource;

    @Test
    void addsOpenPositionFeeAndFundingAccumulators() throws SQLException {
        assertThat(columnsOf("OPEN_POSITIONS"))
                .contains("ACCUMULATED_OPEN_FEE", "ACCUMULATED_FUNDING_COST");
    }

    @Test
    void addsOpenPositionTakeProfitAndStopLossPrices() throws SQLException {
        assertThat(columnsOf("OPEN_POSITIONS"))
                .contains("TAKE_PROFIT_PRICE", "STOP_LOSS_PRICE");
    }

    @Test
    void addsGrossNetFeeAndFundingFieldsToPositionHistory() throws SQLException {
        assertThat(columnsOf("POSITION_HISTORY"))
                .contains(
                        "GROSS_REALIZED_PNL",
                        "OPEN_FEE",
                        "CLOSE_FEE",
                        "TOTAL_FEE",
                        "FUNDING_COST",
                        "NET_REALIZED_PNL"
                );
    }

    private Set<String> columnsOf(String tableName) throws SQLException {
        try (var connection = dataSource.getConnection();
             ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, null)) {
            Set<String> result = new HashSet<>();
            while (columns.next()) {
                result.add(columns.getString("COLUMN_NAME").toUpperCase(Locale.ROOT));
            }
            return result;
        }
    }
}
