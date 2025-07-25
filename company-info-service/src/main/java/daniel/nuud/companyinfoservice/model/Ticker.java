package daniel.nuud.companyinfoservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tickers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ticker {
    @Id
    private String ticker;

    private String companyName;
    private String currency;
}
