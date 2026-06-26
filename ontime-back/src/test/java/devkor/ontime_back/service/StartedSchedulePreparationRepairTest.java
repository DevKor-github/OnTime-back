package devkor.ontime_back.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartedSchedulePreparationRepairTest {

    @Mock
    private ScheduleService scheduleService;

    private StartedSchedulePreparationRepair repair;

    @BeforeEach
    void setUp() {
        repair = new StartedSchedulePreparationRepair(scheduleService);
    }

    @Test
    void repairStartedSchedulesWithoutSnapshotsDelegatesStartupRepair() {
        when(scheduleService.repairStartedSchedulePreparationSnapshots()).thenReturn(3);

        repair.repairStartedSchedulesWithoutSnapshots();

        verify(scheduleService).repairStartedSchedulePreparationSnapshots();
    }
}
