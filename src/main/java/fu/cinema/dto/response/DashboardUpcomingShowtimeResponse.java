package fu.cinema.dto.response;

import fu.cinema.enums.ShowtimeStatus;

import java.time.LocalDateTime;

public record DashboardUpcomingShowtimeResponse(
        String movieTitle,
        String posterUrl,
        String hallName,
        LocalDateTime startTime,
        ShowtimeStatus status
) {
}
