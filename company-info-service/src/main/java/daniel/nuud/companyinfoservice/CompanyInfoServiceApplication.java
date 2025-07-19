package daniel.nuud.companyinfoservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@SpringBootApplication
@EnableReactiveMongoRepositories
public class CompanyInfoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompanyInfoServiceApplication.class, args);
    }

}
