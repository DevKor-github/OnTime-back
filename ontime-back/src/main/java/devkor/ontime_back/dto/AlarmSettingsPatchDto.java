package devkor.ontime_back.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public class AlarmSettingsPatchDto {
    private Object alarmsEnabled;
    private Object defaultAlarmOffsetMinutes;
    private final Map<String, Object> unknownFields = new HashMap<>();

    @JsonAnySetter
    public void addUnknownField(String name, Object value) {
        unknownFields.put(name, value);
    }

    @AssertTrue(message = "알 수 없는 알람 설정 필드입니다.")
    public boolean isKnownFieldsOnly() {
        return unknownFields.isEmpty();
    }

    @AssertTrue(message = "변경할 알람 설정을 하나 이상 입력해야 합니다.")
    public boolean isNotEmpty() {
        return alarmsEnabled != null || defaultAlarmOffsetMinutes != null || !unknownFields.isEmpty();
    }

    @AssertTrue(message = "alarmsEnabled는 boolean 값이어야 합니다.")
    public boolean isAlarmsEnabledValid() {
        return alarmsEnabled == null || alarmsEnabled instanceof Boolean;
    }

    @AssertTrue(message = "defaultAlarmOffsetMinutes는 0 이상 1440 이하의 정수여야 합니다.")
    public boolean isDefaultAlarmOffsetMinutesValid() {
        Integer value = getDefaultAlarmOffsetMinutesValue();
        return defaultAlarmOffsetMinutes == null || (value != null && value >= 0 && value <= 1440);
    }

    public Boolean getAlarmsEnabledValue() {
        return alarmsEnabled instanceof Boolean value ? value : null;
    }

    public Integer getDefaultAlarmOffsetMinutesValue() {
        if (defaultAlarmOffsetMinutes instanceof Integer value) {
            return value;
        }
        if (defaultAlarmOffsetMinutes instanceof Long value && value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
            return value.intValue();
        }
        if (defaultAlarmOffsetMinutes instanceof Short value) {
            return value.intValue();
        }
        if (defaultAlarmOffsetMinutes instanceof Byte value) {
            return value.intValue();
        }
        if (defaultAlarmOffsetMinutes instanceof BigInteger value
                && value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0
                && value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0) {
            return value.intValue();
        }
        return null;
    }
}
