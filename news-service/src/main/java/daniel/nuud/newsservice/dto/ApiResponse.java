package daniel.nuud.newsservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class ApiResponse {
    private int count;
    private String next_url;
    private String request_id;
    private String status;
    private List<ApiArticle> results;
}
