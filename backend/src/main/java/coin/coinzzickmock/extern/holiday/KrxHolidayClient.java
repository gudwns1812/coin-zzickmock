package coin.coinzzickmock.extern.holiday;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import coin.coinzzickmock.extern.exception.ExternalException;
import coin.coinzzickmock.support.error.ExternalErrorType;

@Component
public class KrxHolidayClient {

    private static final String OTP_URL = "https://open.krx.co.kr/contents/COM/GenerateOTP.jspx";
    private static final String HOLIDAY_DATA_URL = "https://open.krx.co.kr/contents/OPN/99/OPN99000001.jspx";
    private static final String BLD = "MKD/01/0110/01100305/mkd01100305_01";
    private static final String PAGE_PATH = "/contents/MKD/01/0110/01100305/MKD01100305.jsp";
    private static final String GRID_TYPE_KRX = "KRX";
    private static final DateTimeFormatter HOLIDAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public KrxHolidayClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Set<LocalDate> fetchHolidays(int year) {
        String code = generateOtp();
        String responseJson = requestHolidayData(year, code);
        return parseHolidayDates(responseJson);
    }

    private String generateOtp() {
        URI uri = UriComponentsBuilder.fromHttpUrl(OTP_URL)
                .queryParam("name", "form")
                .queryParam("bld", BLD)
                .build()
                .encode()
                .toUri();
        try {
            String code = restTemplate.getForObject(uri, String.class);
            if (code == null || code.isBlank()) {
                throw new ExternalException(ExternalErrorType.KRX_OTP_GENERATION_FAILED);
            }
            return code;
        } catch (RestClientException exception) {
            throw new ExternalException(ExternalErrorType.KRX_OTP_GENERATION_FAILED, exception);
        }
    }

    private String requestHolidayData(int year, String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("search_bas_yy", String.valueOf(year));
        formData.add("gridTp", GRID_TYPE_KRX);
        formData.add("pagePath", PAGE_PATH);
        formData.add("code", code);

        try {
            String responseJson = restTemplate.postForObject(
                    HOLIDAY_DATA_URL,
                    new HttpEntity<>(formData, headers),
                    String.class
            );
            if (responseJson == null || responseJson.isBlank()) {
                throw new ExternalException(ExternalErrorType.KRX_HOLIDAY_REQUEST_FAILED);
            }
            return responseJson;
        } catch (RestClientException exception) {
            throw new ExternalException(ExternalErrorType.KRX_HOLIDAY_REQUEST_FAILED, exception);
        }
    }

    private Set<LocalDate> parseHolidayDates(String responseJson) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseJson);
            JsonNode holidayNodes = rootNode.path("block1");
            if (!holidayNodes.isArray()) {
                throw new ExternalException(ExternalErrorType.KRX_HOLIDAY_RESPONSE_PARSE_FAILED);
            }

            Set<LocalDate> holidays = new HashSet<>();
            for (JsonNode holidayNode : holidayNodes) {
                JsonNode dateNode = holidayNode.path("calnd_dd");
                if (!dateNode.isTextual()) {
                    throw new ExternalException(ExternalErrorType.KRX_HOLIDAY_RESPONSE_PARSE_FAILED);
                }
                holidays.add(LocalDate.parse(dateNode.asText(), HOLIDAY_DATE_FORMAT));
            }
            return holidays;
        } catch (ExternalException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ExternalException(ExternalErrorType.KRX_HOLIDAY_RESPONSE_PARSE_FAILED, exception);
        }
    }
}
