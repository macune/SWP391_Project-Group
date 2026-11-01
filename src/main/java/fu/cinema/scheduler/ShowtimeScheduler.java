package fu.cinema.scheduler;

import fu.cinema.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShowtimeScheduler {

    private final ShowtimeService showtimeService;

    @Scheduled(fixedRate = 10000)
    public void updateShowtimeStatuses() {
        showtimeService.updateShowtimeStatuses();
    }
}