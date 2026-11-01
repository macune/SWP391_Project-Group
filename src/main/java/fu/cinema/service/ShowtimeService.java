package fu.cinema.service;

import fu.cinema.dto.request.ShowtimeRequest;

public interface ShowtimeService {

    void createShowtime(ShowtimeRequest request, Long branchId);
    void cancelShowtime(Long showtimeId, Long branchId);
    void updateShowtime(
            Long showtimeId,
            ShowtimeRequest request,
            Long branchId
    );
    void updateShowtimeStatuses();

}