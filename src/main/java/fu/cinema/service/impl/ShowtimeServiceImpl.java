
package fu.cinema.service.impl;

import fu.cinema.dto.request.ShowtimeRequest;
import fu.cinema.entity.Hall;
import fu.cinema.entity.Movie;
import fu.cinema.entity.Showtime;
import fu.cinema.enums.HallStatus;
import fu.cinema.enums.ShowtimeStatus;
import fu.cinema.exception.ShowtimeConflictException;
import fu.cinema.repository.HallRepository;
import fu.cinema.repository.MovieRepository;
import fu.cinema.repository.ShowtimeRepository;
import fu.cinema.service.ShowtimeService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final HallRepository hallRepository;


    @Override
    @Transactional
    public void createShowtime(ShowtimeRequest request, Long branchId) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không  tìm thấy phim"));

        Hall hall = hallRepository.findById(request.getHallId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng chiếu"));

        if (hall.getBranch() == null
                || !hall.getBranch().getBranchId().equals(branchId)) {
            throw new IllegalArgumentException(
                    "Bạn chỉ có thể tạo suất chiếu cho chi nhánh của mình.");
        }

        if (hall.getStatus() != HallStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Không thể tạo suất chiếu tại phòng chiếu không hoạt động.");
        }

        // Ngày chính + những ngày được chọn thêm
        Set<LocalDate> dates = new LinkedHashSet<>();
        dates.add(request.getShowDate());

        if (request.getDuplicateDates() != null) {
            dates.addAll(request.getDuplicateDates());
        }

        LocalDateTime earliestAllowedTime = LocalDateTime.now().plusHours(24);
        List<Showtime> newShowtimes = new ArrayList<>();

        // Kiểm tra toàn bộ ngày trước khi lưu
        for (LocalDate date : dates) {
            LocalDateTime start = LocalDateTime.of(date, request.getStartTime());
            LocalDateTime end = start.plusMinutes(movie.getDuration());

            if (start.isBefore(earliestAllowedTime)) {
                throw new IllegalArgumentException(
                        "Mỗi suất chiếu phải được lên lịch trước ít nhất 24 giờ.");
            }

            validateNoConflict(hall.getHallId(), start, end);

            newShowtimes.add(Showtime.builder()
                    .movie(movie)
                    .hall(hall)
                    .startTime(start)
                    .endTime(end)
                    .basePrice(request.getBasePrice())
                    .status(ShowtimeStatus.SCHEDULED)
                    .build());
        }

        showtimeRepository.saveAll(newShowtimes);
    }

    @Override
    @Transactional
    public void cancelShowtime(Long showtimeId, Long branchId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Showtime not found"));

        if (!showtime.getHall().getBranch().getBranchId().equals(branchId)) {
            throw new IllegalArgumentException(
                    "You can only cancel showtimes in your own branch");
        }

        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new IllegalArgumentException(
                    "Only scheduled showtimes can be cancelled");
        }

        showtime.setStatus(ShowtimeStatus.CANCELLED);
        showtimeRepository.save(showtime);
    }


    private void validateNoConflict(
            Long hallId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Showtime> conflicts = showtimeRepository
                .findByHall_HallIdAndStartTimeLessThanAndEndTimeGreaterThanAndStatusNot(
                        hallId,
                        endTime,
                        startTime,
                        ShowtimeStatus.CANCELLED
                );

        if (!conflicts.isEmpty()) {
            throw new ShowtimeConflictException(
                    "Phòng chiếu này đã có suất chiếu vào khung giờ đã chọn."
            );
        }
    }
}
