package devkor.ontime_back.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RequestInfoDto {
    private String requestUrl;
    private String requestMethod;
    private String userId;
    private String clientIp;
}
