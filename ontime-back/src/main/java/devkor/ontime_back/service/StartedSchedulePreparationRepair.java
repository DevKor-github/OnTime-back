package devkor.ontime_back.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartedSchedulePreparationRepair {
    private final ScheduleService scheduleService;

    @EventListener(ApplicationReadyEvent.class)
    public void repairStartedSchedulesWithoutSnapshots() {
        int repairedCount = scheduleService.repairStartedSchedulePreparationSnapshots();
        if (repairedCount > 0) {
            log.info("Repaired {} started schedules without preparation snapshots", repairedCount);
        }
    }
}
