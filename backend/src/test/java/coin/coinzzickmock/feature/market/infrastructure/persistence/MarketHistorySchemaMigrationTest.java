package coin.coinzzickmock.feature.market.infrastructure.persistence;

import coin.coinzzickmock.CoinZzickmockApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CoinZzickmockApplication.class)
@ActiveProfiles("test")
class MarketHistorySchemaMigrationTest {
    @Autowired
    private DataSource dataSource;

    @Test
    void createsMarketHistoryTables() throws SQLException {
        assertThat(tableExists("MARKET_SYMBOLS")).isTrue();
        assertThat(tableExists("MARKET_CANDLES_1M")).isTrue();
        assertThat(tableExists("MARKET_CANDLES_1H")).isTrue();
    }

    @Test
    void addsLookupAndRollupIndexesOnTimeColumns() throws SQLException {
        Map<String, List<String>> minuteIndexes = indexesOf("MARKET_CANDLES_1M");
        Map<String, List<String>> hourlyIndexes = indexesOf("MARKET_CANDLES_1H");

        assertThat(minuteIndexes.values())
                .contains(List.of("SYMBOL_ID", "OPEN_TIME"))
                .contains(List.of("OPEN_TIME", "SYMBOL_ID"));
        assertThat(hourlyIndexes.values())
                .contains(List.of("SYMBOL_ID", "OPEN_TIME"))
                .contains(List.of("OPEN_TIME", "SYMBOL_ID"));
    }

    @Test
    void removesTradeCountColumnsFromMarketHistoryTables() throws SQLException {
        assertThat(columnsOf("MARKET_CANDLES_1M")).doesNotContain("TRADE_COUNT");
        assertThat(columnsOf("MARKET_CANDLES_1H")).doesNotContain("TRADE_COUNT");
    }

    @Test
    void addsFundingScheduleColumnsToMarketSymbols() throws SQLException {
        assertThat(columnsOf("MARKET_SYMBOLS"))
                .contains("FUNDING_INTERVAL_HOURS", "FUNDING_ANCHOR_HOUR", "FUNDING_TIME_ZONE");
    }

    @Test
    void seedsDefaultFundingScheduleMetadataForSymbols() throws SQLException {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("""
                     SELECT funding_interval_hours, funding_anchor_hour, funding_time_zone
                     FROM market_symbols
                     WHERE symbol = 'BTCUSDT'
                     """)) {
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt("funding_interval_hours")).isEqualTo(8);
                assertThat(result.getInt("funding_anchor_hour")).isEqualTo(1);
                assertThat(result.getString("funding_time_zone")).isEqualTo("Asia/Seoul");
            }
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (var connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private Map<String, List<String>> indexesOf(String tableName) throws SQLException {
        try (var connection = dataSource.getConnection();
             ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, tableName, false, false)) {
            return readIndexes(indexes);
        }
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

    private Map<String, List<String>> readIndexes(ResultSet indexes) throws SQLException {
        Map<String, Map<Short, String>> indexColumns = new LinkedHashMap<>();
        while (indexes.next()) {
            String indexName = indexes.getString("INDEX_NAME");
            String columnName = indexes.getString("COLUMN_NAME");
            short ordinalPosition = indexes.getShort("ORDINAL_POSITION");

            if (indexName == null || columnName == null) {
                continue;
            }

            String normalizedIndexName = indexName.toUpperCase(Locale.ROOT);
            indexColumns.computeIfAbsent(normalizedIndexName, ignored -> new LinkedHashMap<>())
                    .put(ordinalPosition, columnName.toUpperCase(Locale.ROOT));
        }

        Map<String, List<String>> orderedIndexes = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Short, String>> entry : indexColumns.entrySet()) {
            orderedIndexes.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
        }
        return orderedIndexes;
    }
}
