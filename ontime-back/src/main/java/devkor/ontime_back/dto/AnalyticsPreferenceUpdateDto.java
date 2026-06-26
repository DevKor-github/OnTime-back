package devkor.ontime_back.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public class AnalyticsPreferenceUpdateDto {
    private Object enabled;
    private final Map<String, Object> unknownFields = new HashMap<>();

    @JsonAnySetter
    public void addUnknownField(String name, Object value) {
        unknownFields.put(name, value);
    }

    @AssertTrue(message = "알 수 없는 분석 설정 필드입니다.")
    public boolean isKnownFieldsOnly() {
        return unknownFields.isEmpty();
    }

    @AssertTrue(message = "enabled는 필수 값입니다.")
    public boolean isEnabledPresent() {
        return enabled != null;
    }

    @AssertTrue(message = "enabled는 boolean 값이어야 합니다.")
    public boolean isEnabledBoolean() {
        return enabled == null || enabled instanceof Boolean;
    }

    public Boolean getEnabledValue() {
        return enabled instanceof Boolean value ? value : null;
    }
}
