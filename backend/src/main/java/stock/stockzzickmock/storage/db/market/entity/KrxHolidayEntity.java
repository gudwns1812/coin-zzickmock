package stock.stockzzickmock.storage.db.market.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import stock.stockzzickmock.storage.db.BaseTimeEntity;

@Entity
@Table(
        name = "krx_holiday",
        uniqueConstraints = @UniqueConstraint(name = "uk_krx_holiday_market_date", columnNames = {"market_code", "holiday_date"})
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KrxHolidayEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "krx_holiday_id")
    private Long id;

    @Column(name = "market_code", nullable = false, length = 10)
    private String marketCode;

    @Column(name = "holiday_year", nullable = false)
    private Integer holidayYear;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    public static KrxHolidayEntity of(String marketCode, LocalDate holidayDate) {
        return KrxHolidayEntity.builder()
                .marketCode(marketCode)
                .holidayYear(holidayDate.getYear())
                .holidayDate(holidayDate)
                .build();
    }
}
