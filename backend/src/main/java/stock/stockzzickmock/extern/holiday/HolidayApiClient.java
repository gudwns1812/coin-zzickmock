package stock.stockzzickmock.extern.holiday;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import stock.stockzzickmock.extern.holiday.dto.HolidayItem;
import stock.stockzzickmock.extern.holiday.dto.Response;
import stock.stockzzickmock.support.error.CoreException;
import stock.stockzzickmock.support.error.ExternalErrorType;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HolidayApiClient {

    private static final String HOLIDAY_API_URL = "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo";
    private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestTemplate restTemplate;

    @Value("${data.service.key}")
    private String key;

    public List<LocalDate> fetchCurrentMonthHolidays(LocalDate today) {
        URI uri = URI.create(String.format(
                "%s?serviceKey=%s&solYear=%d&solMonth=%02d",
                HOLIDAY_API_URL,
                key,
                today.getYear(),
                today.getMonthValue()
        ));

        String xml = restTemplate.getForObject(uri, String.class);

        try {
            Response response = new XmlMapper().readValue(xml, Response.class);
            if (response.getBody() == null || response.getBody().getItems() == null) {
                return List.of();
            }

            List<HolidayItem> items = response.getBody().getItems().getItem();
            if (items == null) {
                return List.of();
            }

            return items.stream()
                    .map(item -> LocalDate.parse(item.getLocdate(), API_DATE_FORMAT))
                    .toList();
        } catch (Exception exception) {
            throw new CoreException(ExternalErrorType.HOLIDAY_API_PARSE_FAILED, exception);
        }
    }
}
