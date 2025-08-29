package daniel.nuud.companyinfoservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Company {
    @Id
    private String ticker;

    private String description;
    private String name;

    @Column("homepage_url")
    private String homepageUrl;

    @Column("primary_exchange")
    private String primaryExchange;

    @Column("market_cap")
    private String marketCap;

    private String city;
    private String address1;

    @Column("icon_url")
    private String iconUrl;

    @Column("logo_url")
    private String logoUrl;

    private String status;
}
