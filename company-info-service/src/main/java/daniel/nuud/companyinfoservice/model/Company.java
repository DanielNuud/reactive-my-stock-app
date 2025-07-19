package daniel.nuud.companyinfoservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Company {
    @Id
    private String ticker;

    private String description;
    private String name;
    private String homepageUrl;
    private String primaryExchange;
    private String marketCap;
    private String city;
    private String address1;
    private String iconUrl;
    private String logoUrl;
    private String status;
}
