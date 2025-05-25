package devkor.ontime_back.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DoneStatus {
    LATE,        // 지각종료 - latenesstime > 0
    NORMAL,      // 지각 안 한 종료 - latenesstime = 0
    ABNORMAL,    // 비정상종료 - latenesstime = -2
    NOT_ENDED    // 종료되지 않음 - latenesstime = -1
}
