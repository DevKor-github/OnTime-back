package devkor.ontime_back.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleStatusTest {

    @Test
    void updateLatenessTimeKeepsScheduleOpenForNullOrSentinelValues() {
        Schedule schedule = Schedule.builder().build();

        schedule.updateLatenessTime(null);
        assertThat(schedule.getDoneStatus()).isEqualTo(DoneStatus.NOT_ENDED);

        schedule.updateLatenessTime(-1);
        assertThat(schedule.getDoneStatus()).isEqualTo(DoneStatus.NOT_ENDED);
    }

    @Test
    void updateLatenessTimeMapsArrivalOutcomeToDoneStatus() {
        Schedule schedule = Schedule.builder().build();

        schedule.updateLatenessTime(12);
        assertThat(schedule.getDoneStatus()).isEqualTo(DoneStatus.LATE);

        schedule.updateLatenessTime(0);
        assertThat(schedule.getDoneStatus()).isEqualTo(DoneStatus.NORMAL);

        schedule.updateLatenessTime(-2);
        assertThat(schedule.getDoneStatus()).isEqualTo(DoneStatus.ABNORMAL);
    }

    @Test
    void finishStoresCompletionTimeAndMapsLatenessToFinalDoneStatus() {
        Instant finishedAt = Instant.parse("2026-05-05T09:00:00Z");
        Schedule schedule = Schedule.builder().build();

        schedule.finish(null, finishedAt);
        assertThat(schedule.getDoneStatus()).isEqualTo(DoneStatus.ABNORMAL);
        assertThat(schedule.getFinishedAt()).isEqualTo(finishedAt);

        schedule.finish(1, finishedAt.plusSeconds(60));
        assertThat(schedule.getDoneStatus()).isEqualTo(DoneStatus.LATE);

        schedule.finish(0, finishedAt.plusSeconds(120));
        assertThat(schedule.getDoneStatus()).isEqualTo(DoneStatus.NORMAL);
    }
}
