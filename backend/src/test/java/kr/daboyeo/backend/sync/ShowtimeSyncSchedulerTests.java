package kr.daboyeo.backend.sync;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class ShowtimeSyncSchedulerTests {

    @Test
    void startupTriggerCallsSyncService() {
        ShowtimeSyncService service = mock(ShowtimeSyncService.class);
        ShowtimeSyncScheduler scheduler = new ShowtimeSyncScheduler(service);

        scheduler.runOnStartup();

        verify(service, times(1)).syncDailyShowtimes();
    }

    @Test
    void scheduledTriggerCallsSyncService() {
        ShowtimeSyncService service = mock(ShowtimeSyncService.class);
        ShowtimeSyncScheduler scheduler = new ShowtimeSyncScheduler(service);

        scheduler.runOnSchedule();

        verify(service, times(1)).syncDailyShowtimes();
    }
}
