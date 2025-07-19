package daniel.nuud.companyinfoservice.dto;

import lombok.Data;

@Data
public class ApiResponse {
    private Ticket results;
    private String status;
}
