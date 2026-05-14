package devkor.ontime_back.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AlarmSettingsPatchDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsUnknownAlarmSettingFields() throws Exception {
        AlarmSettingsPatchDto dto = objectMapper.readValue("{\"token\":\"secret\"}", AlarmSettingsPatchDto.class);

        assertThat(messages(dto)).contains("알 수 없는 알람 설정 필드입니다.");
    }

    @Test
    void rejectsEmptyPatchBody() throws Exception {
        AlarmSettingsPatchDto dto = objectMapper.readValue("{}", AlarmSettingsPatchDto.class);

        assertThat(messages(dto)).contains("변경할 알람 설정을 하나 이상 입력해야 합니다.");
    }

    @Test
    void rejectsNonBooleanAlarmsEnabled() throws Exception {
        AlarmSettingsPatchDto dto = objectMapper.readValue("{\"alarmsEnabled\":\"true\"}", AlarmSettingsPatchDto.class);

        assertThat(messages(dto)).contains("alarmsEnabled는 boolean 값이어야 합니다.");
    }

    @Test
    void rejectsNonIntegralDefaultAlarmOffset() throws Exception {
        AlarmSettingsPatchDto dto = objectMapper.readValue("{\"defaultAlarmOffsetMinutes\":5.5}", AlarmSettingsPatchDto.class);

        assertThat(messages(dto)).contains("defaultAlarmOffsetMinutes는 0 이상 1440 이하의 정수여야 합니다.");
        assertThat(dto.getDefaultAlarmOffsetMinutesValue()).isNull();
    }

    @Test
    void rejectsOutOfRangeDefaultAlarmOffset() throws Exception {
        AlarmSettingsPatchDto dto = objectMapper.readValue("{\"defaultAlarmOffsetMinutes\":1441}", AlarmSettingsPatchDto.class);

        assertThat(messages(dto)).contains("defaultAlarmOffsetMinutes는 0 이상 1440 이하의 정수여야 합니다.");
    }

    @Test
    void acceptsSupportedIntegralDefaultAlarmOffsetTypes() {
        assertOffsetValue(10, 10);
        assertOffsetValue(20L, 20);
        assertOffsetValue((short) 30, 30);
        assertOffsetValue((byte) 40, 40);
        assertOffsetValue(BigInteger.valueOf(50), 50);
    }

    private Set<String> messages(AlarmSettingsPatchDto dto) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }

    private void assertOffsetValue(Object rawValue, int expectedValue) {
        AlarmSettingsPatchDto dto = new AlarmSettingsPatchDto();
        ReflectionTestUtils.setField(dto, "defaultAlarmOffsetMinutes", rawValue);

        assertThat(validator.validate(dto)).isEmpty();
        assertThat(dto.getDefaultAlarmOffsetMinutesValue()).isEqualTo(expectedValue);
    }
}
