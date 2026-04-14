package coin.coinzzickmock.extern.holiday;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import coin.coinzzickmock.extern.exception.ExternalException;
import coin.coinzzickmock.support.error.ExternalErrorType;

class KrxHolidayClientTest {

    private static final String OTP_URL = "https://open.krx.co.kr/contents/COM/GenerateOTP.jspx";
    private static final String HOLIDAY_DATA_URL = "https://open.krx.co.kr/contents/OPN/99/OPN99000001.jspx";

    private MockRestServiceServer server;
    private KrxHolidayClient krxHolidayClient;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        krxHolidayClient = new KrxHolidayClient(restTemplate, new ObjectMapper());
    }

    @Test
    void fetchesHolidayDataWithOtpAndParsesDates() {
        server.expect(requestTo(containsString(OTP_URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("otp-value", MediaType.TEXT_PLAIN));

        server.expect(requestTo(HOLIDAY_DATA_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("search_bas_yy=2026")))
                .andExpect(content().string(containsString("gridTp=KRX")))
                .andExpect(content().string(containsString("pagePath=%2Fcontents%2FMKD%2F01%2F0110%2F01100305%2FMKD01100305.jsp")))
                .andExpect(content().string(containsString("code=otp-value")))
                .andRespond(withSuccess(
                        "{\"block1\":[{\"calnd_dd\":\"2026-01-01\"},{\"calnd_dd\":\"2026-12-31\"}]}",
                        MediaType.APPLICATION_JSON
                ));

        Set<LocalDate> holidays = krxHolidayClient.fetchHolidays(2026);

        assertThat(holidays).containsExactlyInAnyOrder(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        );
        server.verify();
    }

    @Test
    void throwsExternalExceptionWhenOtpGenerationFails() {
        server.expect(requestTo(containsString(OTP_URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> krxHolidayClient.fetchHolidays(2026))
                .isInstanceOf(ExternalException.class)
                .extracting("errorType")
                .isEqualTo(ExternalErrorType.KRX_OTP_GENERATION_FAILED);
    }

    @Test
    void throwsExternalExceptionWhenHolidayRequestFails() {
        server.expect(requestTo(containsString(OTP_URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("otp-value", MediaType.TEXT_PLAIN));

        server.expect(requestTo(HOLIDAY_DATA_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> krxHolidayClient.fetchHolidays(2026))
                .isInstanceOf(ExternalException.class)
                .extracting("errorType")
                .isEqualTo(ExternalErrorType.KRX_HOLIDAY_REQUEST_FAILED);
    }

    @Test
    void throwsExternalExceptionWhenHolidayResponseCannotBeParsed() {
        server.expect(requestTo(containsString(OTP_URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("otp-value", MediaType.TEXT_PLAIN));

        server.expect(requestTo(HOLIDAY_DATA_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"unexpected\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> krxHolidayClient.fetchHolidays(2026))
                .isInstanceOf(ExternalException.class)
                .extracting("errorType")
                .isEqualTo(ExternalErrorType.KRX_HOLIDAY_RESPONSE_PARSE_FAILED);
    }
}
