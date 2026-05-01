package coin.coinzzickmock.providers.infrastructure;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

final class MetricTags {
    private static final Pattern METER_NAME_PATTERN = Pattern.compile("[a-z][a-z0-9_.-]{0,127}");
    private static final Pattern TAG_KEY_PATTERN = Pattern.compile("[a-z][a-z0-9_]{0,63}");
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("[A-Z0-9_:-]{1,32}");
    private static final Pattern INTERVAL_PATTERN = Pattern.compile("[0-9]+[mhdwM]");
    private static final Pattern RANGE_BUCKET_PATTERN = Pattern.compile("[0-9]{4}(-[0-9]{2})?");
    private static final Pattern ROUTE_PATTERN = Pattern.compile("/[A-Za-z0-9_{}./-]{1,119}");
    private static final Pattern STATUS_PATTERN = Pattern.compile("[1-5][0-9]{2}");
    private static final Pattern STATUS_FAMILY_PATTERN = Pattern.compile("[1-5]xx");
    private static final Pattern BOUNDED_WORD_PATTERN = Pattern.compile("[a-z][a-z0-9_.:-]{0,63}");
    private static final Pattern RAW_DYNAMIC_ROUTE_SEGMENT = Pattern.compile(
            "([0-9]+)|([A-Z0-9]{5,})|([A-Fa-f0-9-]{16,})"
    );
    private static final Pattern JWT_SHAPED_VALUE = Pattern.compile(
            "[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{20,}"
    );
    private static final Set<String> FORBIDDEN_TAG_KEYS = Set.of(
            "memberid",
            "member_id",
            "userid",
            "user_id",
            "accountid",
            "account_id",
            "sessionid",
            "session_id",
            "orderid",
            "order_id",
            "requestid",
            "request_id",
            "rawquery",
            "raw_query",
            "raw_query_string",
            "query",
            "rawcachekey",
            "raw_cache_key",
            "email",
            "phone",
            "phone_number",
            "jwtsubject",
            "jwt_subject",
            "subject",
            "authorization",
            "token"
    );
    private static final Set<String> ALLOWED_TAG_KEYS = Set.of(
            "channel",
            "direction",
            "endpoint_group",
            "failure_category",
            "interval",
            "method",
            "operation",
            "range_bucket",
            "reason",
            "result",
            "route_pattern",
            "size_bucket",
            "source",
            "state",
            "status",
            "status_family",
            "stream",
            "symbol",
            "use_case"
    );
    private static final Set<String> HTTP_METHODS = Set.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS",
            "HEAD"
    );
    private static final Set<String> SOURCES = Set.of(
            "application",
            "bitget",
            "cache",
            "db",
            "http",
            "login",
            "redis",
            "sse",
            "websocket",
            "authenticated_api",
            "unknown"
    );
    private static final Set<String> DIRECTIONS = Set.of("request", "response");

    private MetricTags() {
    }

    static String meterName(String meterName) {
        if (meterName == null || !METER_NAME_PATTERN.matcher(meterName).matches()) {
            throw new IllegalArgumentException("Metric meter name must use lowercase dot notation: " + meterName);
        }
        return meterName;
    }

    static Tags of(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Tags.empty();
        }
        return Tags.of(tags.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> Tag.of(tagKey(entry.getKey()), tagValue(entry.getKey(), entry.getValue())))
                .toList());
    }

    static String tagKey(String key) {
        if (key == null || !TAG_KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("Metric tag key must use lowercase snake_case: " + key);
        }
        if (FORBIDDEN_TAG_KEYS.contains(normalizeKey(key))) {
            throw new IllegalArgumentException("Metric tag key is forbidden: " + key);
        }
        if (!ALLOWED_TAG_KEYS.contains(key)) {
            throw new IllegalArgumentException("Metric tag key is not allow-listed: " + key);
        }
        return key;
    }

    static String tagValue(String key, String value) {
        if (value == null || JWT_SHAPED_VALUE.matcher(value).matches() || !validatorFor(key).test(value)) {
            throw new IllegalArgumentException("Metric tag value is unstable for key " + key + ": " + value);
        }
        return value;
    }

    private static Predicate<String> validatorFor(String key) {
        return switch (tagKey(key)) {
            case "method" -> HTTP_METHODS::contains;
            case "direction" -> DIRECTIONS::contains;
            case "source" -> SOURCES::contains;
            case "status" -> value -> STATUS_PATTERN.matcher(value).matches();
            case "status_family" -> value -> STATUS_FAMILY_PATTERN.matcher(value).matches();
            case "symbol" -> value -> SYMBOL_PATTERN.matcher(value).matches();
            case "interval" -> value -> INTERVAL_PATTERN.matcher(value).matches();
            case "range_bucket" -> value -> RANGE_BUCKET_PATTERN.matcher(value).matches();
            case "route_pattern" -> MetricTags::isStableRoutePattern;
            default -> value -> BOUNDED_WORD_PATTERN.matcher(value).matches();
        };
    }

    private static boolean isStableRoutePattern(String value) {
        return "unmatched".equals(value)
                || (!value.contains("?")
                && !value.contains("=")
                && ROUTE_PATTERN.matcher(value).matches()
                && hasNoRawDynamicRouteSegment(value));
    }

    private static boolean hasNoRawDynamicRouteSegment(String routePattern) {
        for (String segment : routePattern.split("/")) {
            if (segment.isBlank() || segment.contains("{")) {
                continue;
            }
            if (RAW_DYNAMIC_ROUTE_SEGMENT.matcher(segment).matches()) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT).replace("-", "_").replace(".", "_");
    }
}
