package coin.coinzzickmock.feature.market.infrastructure.persistence;

import coin.coinzzickmock.CoinZzickmockApplication;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(tableExists("MARKET_COMPLETED_CANDLES")).isTrue();
    }

    @Test
    void addsLookupAndRollupIndexesOnTimeColumns() throws SQLException {
        Map<String, List<String>> minuteIndexes = indexesOf("MARKET_CANDLES_1M");
        Map<String, List<String>> hourlyIndexes = indexesOf("MARKET_CANDLES_1H");
        Map<String, List<String>> completedIndexes = indexesOf("MARKET_COMPLETED_CANDLES");

        assertThat(minuteIndexes.values())
                .contains(List.of("SYMBOL_ID", "OPEN_TIME"))
                .contains(List.of("OPEN_TIME", "SYMBOL_ID"));
        assertThat(hourlyIndexes.values())
                .contains(List.of("SYMBOL_ID", "OPEN_TIME"))
                .contains(List.of("OPEN_TIME", "SYMBOL_ID"));
        assertThat(completedIndexes.values())
                .contains(List.of("SYMBOL_ID", "CANDLE_INTERVAL", "OPEN_TIME"));
    }

    @Test
    void addsCompletedCandleDiscriminatorColumns() throws SQLException {
        assertThat(columnsOf("MARKET_COMPLETED_CANDLES"))
                .contains(
                        "SYMBOL_ID",
                        "CANDLE_INTERVAL",
                        "OPEN_TIME",
                        "CLOSE_TIME",
                        "OPEN_PRICE",
                        "HIGH_PRICE",
                        "LOW_PRICE",
                        "CLOSE_PRICE",
                        "VOLUME",
                        "QUOTE_VOLUME",
                        "CREATED_AT",
                        "UPDATED_AT"
                )
                .doesNotContain(
                        "SOURCE_INTERVAL",
                        "SOURCE_OPEN_TIME",
                        "SOURCE_CLOSE_TIME",
                        "SOURCE_CANDLE_COUNT"
                );
    }

    @Test
    void copiesExistingHourlyCandlesAndSeedsCalendarCandlesIntoGenericCompletedTable() throws SQLException {
        String databaseName = "market_history_copy_" + System.nanoTime();
        String url = "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1";

        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE market_symbols (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            symbol VARCHAR(30) NOT NULL,
                            display_name VARCHAR(100) NOT NULL,
                            base_asset VARCHAR(20) NOT NULL,
                            quote_asset VARCHAR(20) NOT NULL DEFAULT 'USDT',
                            price_scale TINYINT NOT NULL,
                            quantity_scale TINYINT NOT NULL,
                            price_step DECIMAL(19, 8) NOT NULL,
                            quantity_step DECIMAL(19, 8) NOT NULL,
                            max_leverage INT NOT NULL,
                            active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                            updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                            CONSTRAINT pk_market_symbols PRIMARY KEY (id),
                            CONSTRAINT uk_market_symbols_symbol UNIQUE (symbol)
                        )
                        """);
                statement.execute("""
                        CREATE TABLE market_candles_1h (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            symbol_id BIGINT NOT NULL,
                            open_time DATETIME(6) NOT NULL,
                            close_time DATETIME(6) NOT NULL,
                            open_price DECIMAL(19, 4) NOT NULL,
                            high_price DECIMAL(19, 4) NOT NULL,
                            low_price DECIMAL(19, 4) NOT NULL,
                            close_price DECIMAL(19, 4) NOT NULL,
                            volume DECIMAL(19, 8) NOT NULL,
                            quote_volume DECIMAL(19, 4) NULL,
                            source_minute_open_time DATETIME(6) NOT NULL,
                            source_minute_close_time DATETIME(6) NOT NULL,
                            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                            updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                            CONSTRAINT pk_market_candles_1h PRIMARY KEY (id),
                            CONSTRAINT uk_market_candles_1h_symbol_open_time UNIQUE (symbol_id, open_time),
                            CONSTRAINT fk_market_candles_1h_symbol
                                FOREIGN KEY (symbol_id) REFERENCES market_symbols (id)
                        )
                        """);
                statement.execute("""
                        INSERT INTO market_symbols (
                            symbol, display_name, base_asset, quote_asset, price_scale, quantity_scale,
                            price_step, quantity_step, max_leverage, active
                        )
                        VALUES ('BTCUSDT', 'Bitcoin Perpetual', 'BTC', 'USDT', 1, 4, 0.1, 0.0001, 50, TRUE)
                        """);
            }
            long symbolId;
            try (var statement = connection.prepareStatement("SELECT id FROM market_symbols WHERE symbol = 'BTCUSDT'");
                 var resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                symbolId = resultSet.getLong("id");
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO market_candles_1h (
                        symbol_id, open_time, close_time, open_price, high_price, low_price,
                        close_price, volume, quote_volume, source_minute_open_time,
                        source_minute_close_time, created_at, updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setLong(1, symbolId);
                statement.setObject(2, utcDateTime("2026-04-17T06:00:00Z"));
                statement.setObject(3, utcDateTime("2026-04-17T07:00:00Z"));
                statement.setBigDecimal(4, BigDecimal.valueOf(101000));
                statement.setBigDecimal(5, BigDecimal.valueOf(101500));
                statement.setBigDecimal(6, BigDecimal.valueOf(100500));
                statement.setBigDecimal(7, BigDecimal.valueOf(101250));
                statement.setBigDecimal(8, BigDecimal.valueOf(10.0));
                statement.setBigDecimal(9, BigDecimal.valueOf(1012500.0));
                statement.setObject(10, utcDateTime("2026-04-17T06:00:00Z"));
                statement.setObject(11, utcDateTime("2026-04-17T07:00:00Z"));
                statement.setObject(12, utcDateTime("2026-04-17T07:01:00Z"));
                statement.setObject(13, utcDateTime("2026-04-17T07:02:00Z"));
                statement.executeUpdate();
            }
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V34__generalize_completed_market_candles.sql")
            );

            try (var statement = connection.prepareStatement("""
                    SELECT candle_interval, open_time, close_time, open_price, close_price
                    FROM market_completed_candles
                    ORDER BY candle_interval
                    """);
                 var resultSet = statement.executeQuery()) {
                Map<String, Instant> openTimes = new LinkedHashMap<>();
                Map<String, Instant> closeTimes = new LinkedHashMap<>();
                Map<String, BigDecimal> openPrices = new LinkedHashMap<>();
                Map<String, BigDecimal> closePrices = new LinkedHashMap<>();
                while (resultSet.next()) {
                    String interval = resultSet.getString("candle_interval");
                    openTimes.put(interval, utcInstant(resultSet, "open_time"));
                    closeTimes.put(interval, utcInstant(resultSet, "close_time"));
                    openPrices.put(interval, resultSet.getBigDecimal("open_price"));
                    closePrices.put(interval, resultSet.getBigDecimal("close_price"));
                }
                assertThat(openTimes)
                        .containsEntry("ONE_HOUR", Instant.parse("2026-04-17T06:00:00Z"))
                        .containsEntry("ONE_DAY", Instant.parse("2026-04-17T00:00:00Z"))
                        .containsEntry("ONE_MONTH", Instant.parse("2026-04-01T00:00:00Z"));
                assertThat(openTimes).doesNotContainKey("ONE_WEEK");
                assertThat(closeTimes)
                        .containsEntry("ONE_HOUR", Instant.parse("2026-04-17T07:00:00Z"))
                        .containsEntry("ONE_DAY", Instant.parse("2026-04-18T00:00:00Z"))
                        .containsEntry("ONE_MONTH", Instant.parse("2026-05-01T00:00:00Z"));
                assertThat(closeTimes).doesNotContainKey("ONE_WEEK");
                assertThat(openPrices.values()).allSatisfy(price -> assertThat(price).isEqualByComparingTo("101000"));
                assertThat(closePrices.values()).allSatisfy(price -> assertThat(price).isEqualByComparingTo("101250"));
            }
        }
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

    @Test
    void rejectsInvalidFundingScheduleMetadata() throws SQLException {
        try (var connection = dataSource.getConnection();
             var invalidInterval = connection.prepareStatement("""
                     UPDATE market_symbols
                     SET funding_interval_hours = 0
                     WHERE symbol = 'BTCUSDT'
                     """);
             var invalidAnchor = connection.prepareStatement("""
                     UPDATE market_symbols
                     SET funding_anchor_hour = 24
                     WHERE symbol = 'BTCUSDT'
                     """)) {
            assertThatThrownBy(invalidInterval::executeUpdate).isInstanceOf(SQLException.class);
            assertThatThrownBy(invalidAnchor::executeUpdate).isInstanceOf(SQLException.class);
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

    private static LocalDateTime utcDateTime(String instant) {
        return LocalDateTime.ofInstant(Instant.parse(instant), ZoneOffset.UTC);
    }

    private static Instant utcInstant(ResultSet resultSet, String columnName) throws SQLException {
        return resultSet.getObject(columnName, LocalDateTime.class).toInstant(ZoneOffset.UTC);
    }
}
