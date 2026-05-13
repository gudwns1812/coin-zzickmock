package coin.coinzzickmock.feature.community.domain.content;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TiptapJsonReader {
    private final String source;
    private int index;

    TiptapJsonReader(String source) {
        this.source = source;
    }

    Object read() {
        Object value = readValue();
        skipWhitespace();
        if (index != source.length()) {
            throw invalid();
        }
        return value;
    }

    private Object readValue() {
        skipWhitespace();
        if (index >= source.length()) {
            throw invalid();
        }
        char current = source.charAt(index);
        return switch (current) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> {
                if (current == '-' || Character.isDigit(current)) {
                    yield readNumber();
                }
                throw invalid();
            }
        };
    }

    private Map<String, Object> readObject() {
        expect('{');
        Map<String, Object> object = new LinkedHashMap<>();
        skipWhitespace();
        if (consume('}')) {
            return object;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw invalid();
            }
            String key = readString();
            if (object.containsKey(key)) {
                throw invalid();
            }
            skipWhitespace();
            expect(':');
            object.put(key, readValue());
            skipWhitespace();
            if (consume('}')) {
                return object;
            }
            expect(',');
        }
    }

    private List<Object> readArray() {
        expect('[');
        List<Object> array = new ArrayList<>();
        skipWhitespace();
        if (consume(']')) {
            return array;
        }
        while (true) {
            array.add(readValue());
            skipWhitespace();
            if (consume(']')) {
                return array;
            }
            expect(',');
        }
    }

    private String readString() {
        expect('"');
        StringBuilder builder = new StringBuilder();
        while (index < source.length()) {
            char current = source.charAt(index++);
            if (current == '"') {
                return builder.toString();
            }
            if (current == '\\') {
                builder.append(readEscapedCharacter());
                continue;
            }
            if (current < 0x20) {
                throw invalid();
            }
            builder.append(current);
        }
        throw invalid();
    }

    private char readEscapedCharacter() {
        if (index >= source.length()) {
            throw invalid();
        }
        char escaped = source.charAt(index++);
        return switch (escaped) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> readUnicodeEscape();
            default -> throw invalid();
        };
    }

    private char readUnicodeEscape() {
        if (index + 4 > source.length()) {
            throw invalid();
        }
        int value = 0;
        for (int i = 0; i < 4; i++) {
            char hex = source.charAt(index++);
            int digit = Character.digit(hex, 16);
            if (digit < 0) {
                throw invalid();
            }
            value = (value << 4) + digit;
        }
        return (char) value;
    }

    private Object readLiteral(String literal, Object value) {
        if (!source.startsWith(literal, index)) {
            throw invalid();
        }
        index += literal.length();
        return value;
    }

    private Integer readNumber() {
        int start = index;
        if (consume('-') && index >= source.length()) {
            throw invalid();
        }
        if (consume('0')) {
            if (index < source.length() && Character.isDigit(source.charAt(index))) {
                throw invalid();
            }
        } else {
            readDigits();
        }
        if (index < source.length() && (source.charAt(index) == '.' || source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
            throw invalid();
        }
        try {
            return Integer.valueOf(source.substring(start, index));
        } catch (NumberFormatException ex) {
            throw invalid();
        }
    }

    private void readDigits() {
        if (index >= source.length() || !Character.isDigit(source.charAt(index))) {
            throw invalid();
        }
        while (index < source.length() && Character.isDigit(source.charAt(index))) {
            index++;
        }
    }

    private void skipWhitespace() {
        while (index < source.length()) {
            char current = source.charAt(index);
            if (current != ' ' && current != '\n' && current != '\r' && current != '\t') {
                return;
            }
            index++;
        }
    }

    private char peek() {
        if (index >= source.length()) {
            throw invalid();
        }
        return source.charAt(index);
    }

    private void expect(char expected) {
        if (!consume(expected)) {
            throw invalid();
        }
    }

    private boolean consume(char expected) {
        if (index < source.length() && source.charAt(index) == expected) {
            index++;
            return true;
        }
        return false;
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.INVALID_REQUEST);
    }
}
