package daniel.nuud.companyinfoservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("tickers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TickerEntity {
    @Id
    private String ticker;

    @Column("company_name")
    private String companyName;

    private String currency;
}
