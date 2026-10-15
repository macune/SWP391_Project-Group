package fu.cinema.service.impl;

import fu.cinema.dto.request.ShowtimeRequest;
import fu.cinema.entity.Hall;
import fu.cinema.entity.Movie;
import fu.cinema.entity.Showtime;
import fu.cinema.entity.Branch;
import fu.cinema.enums.HallStatus;
import fu.cinema.enums.MovieStatus;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
    private static final long MIN_GAP_MINUTES = 10;

    @Override
    @Transactional
    public void createShowtime(ShowtimeRequest request, Long branchId) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim"));
        if (movie.getStatus() == MovieStatus.ENDED) {
            throw new IllegalArgumentException("Không thể tạo suất chiếu cho phim đã kết thúc.");
        }

        Hall hall = hallRepository.findById(request.getHallId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng chiếu"));

        if (hall.getBranch() == null || !hall.getBranch().getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("Bạn chỉ có thể tạo suất chiếu cho chi nhánh của mình.");
        }

        if (hall.getStatus() != HallStatus.ACTIVE) {
            throw new IllegalArgumentException("Không thể tạo suất chiếu tại phòng chiếu không hoạt động.");
        }

        Set<LocalDate> dates = new LinkedHashSet<>();
        dates.add(request.getShowDate());

        if (request.getDuplicateDates() != null) {
            dates.addAll(request.getDuplicateDates());
        }

        LocalDateTime earliestAllowedTime = LocalDateTime.now().plusHours(24);
        List<Showtime> newShowtimes = new ArrayList<>();
        Branch branch = hall.getBranch();

        for (LocalDate date : dates) {
            LocalDateTime start = LocalDateTime.of(date, request.getStartTime());
            LocalDateTime end = start.plusMinutes(movie.getDuration());

            if (start.isBefore(earliestAllowedTime)) {
                throw new IllegalArgumentException("Mỗi suất chiếu phải được lên lịch trước ít nhất 24 giờ.");
            }

            // 1. KIỂM TRA GIỜ HOẠT ĐỘNG CỦA RẠP
            validateOperatingHours(start, end, branch);
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

        for (int i = 0; i < newShowtimes.size(); i++) {
            Showtime first = newShowtimes.get(i);
            for (int j = i + 1; j < newShowtimes.size(); j++) {
                Showtime second = newShowtimes.get(j);
                if (violatesMinimumGap(first.getStartTime(), first.getEndTime(), second.getStartTime(), second.getEndTime())) {
                    throw new ShowtimeConflictException("Các suất chiếu được tạo cùng lúc phải cách nhau ít nhất 10 phút.");
                }
            }
        }

        showtimeRepository.saveAll(newShowtimes);
    }

    @Override
    @Transactional
    public void cancelShowtime(Long showtimeId, Long branchId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu"));

        if (!showtime.getHall().getBranch().getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("Bạn chỉ có thể hủy các suất chiếu tại chi nhánh của mình.");
        }

        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new IllegalArgumentException("Chỉ có thể hủy các suất chiếu đã được lên lịch.");
        }

        // 2. CHẶN HỦY NẾU ĐÃ CÓ NGƯỜI MUA VÉ
        if (!showtime.getTickets().isEmpty()) {
            throw new IllegalArgumentException("Không thể hủy! Suất chiếu này đã có khách hàng đặt vé.");
        }

        showtime.setStatus(ShowtimeStatus.CANCELLED);
        showtimeRepository.save(showtime);
    }

    @Override
    @Transactional
    public void updateShowtime(Long showtimeId, ShowtimeRequest request, Long branchId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy suất chiếu."));

        if (showtime.getHall().getBranch() == null || !showtime.getHall().getBranch().getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("Bạn không có quyền sửa suất chiếu của chi nhánh khác.");
        }

        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new IllegalArgumentException("Chỉ có thể sửa suất chiếu đang ở trạng thái đã lên lịch.");
        }

        // 3. CHẶN SỬA NẾU ĐÃ CÓ NGƯỜI MUA VÉ
        if (!showtime.getTickets().isEmpty()) {
            throw new IllegalArgumentException("Không thể sửa! Suất chiếu này đã có khách hàng đặt vé.");
        }

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim."));

        Hall hall = hallRepository.findById(request.getHallId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng chiếu."));

        if (hall.getBranch() == null || !hall.getBranch().getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("Bạn chỉ được chọn phòng thuộc chi nhánh mình quản lý.");
        }

        if (hall.getStatus() != HallStatus.ACTIVE) {
            throw new IllegalArgumentException("Không thể xếp lịch vào phòng chiếu không hoạt động.");
        }

        if (request.getShowDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày chiếu không được nằm trong quá khứ.");
        }

        LocalDateTime startDateTime = LocalDateTime.of(request.getShowDate(), request.getStartTime());
        LocalDateTime endDateTime = startDateTime.plusMinutes(movie.getDuration());

        if (startDateTime.isBefore(LocalDateTime.now().plusHours(24))) {
            throw new IllegalArgumentException("Suất chiếu phải được lên lịch trước ít nhất 24 giờ.");
        }

        // 4. KIỂM TRA GIỜ HOẠT ĐỘNG KHI CẬP NHẬT
        validateOperatingHours(startDateTime, endDateTime, hall.getBranch());

        List<Showtime> conflicts = showtimeRepository
                .findByHall_HallIdAndShowtimeIdNotAndStartTimeLessThanAndEndTimeGreaterThanAndStatusNot(
                        hall.getHallId(),
                        showtimeId,
                        endDateTime.plusMinutes(10),
                        startDateTime.minusMinutes(10),
                        ShowtimeStatus.CANCELLED
                );

        if (!conflicts.isEmpty()) {
            throw new ShowtimeConflictException("Phòng chiếu đã có suất khác trong khoảng thời gian này. Các suất chiếu phải cách nhau ít nhất 10 phút.");
        }

        showtime.setMovie(movie);
        showtime.setHall(hall);
        showtime.setStartTime(startDateTime);
        showtime.setEndTime(endDateTime);
        showtime.setBasePrice(request.getBasePrice());

        showtimeRepository.save(showtime);
    }

    private boolean violatesMinimumGap(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2.plusMinutes(MIN_GAP_MINUTES)) && start2.isBefore(end1.plusMinutes(MIN_GAP_MINUTES));
    }

    private void validateNoConflict(Long hallId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Showtime> existingShowtimes = showtimeRepository.findByHall_HallIdAndStatusNot(hallId, ShowtimeStatus.CANCELLED);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Showtime existing : existingShowtimes) {
            LocalDateTime existingStart = existing.getStartTime();
            LocalDateTime existingEnd = existing.getEndTime();

            boolean startsTooEarly = startTime.isBefore(existingEnd.plusMinutes(MIN_GAP_MINUTES));
            boolean endsTooLate = existingStart.isBefore(endTime.plusMinutes(MIN_GAP_MINUTES));

            if (startsTooEarly && endsTooLate) {
                throw new ShowtimeConflictException("Suất chiếu đang bị xung đột với một suất khác bắt đầu lúc "
                        + existingStart.format(timeFormatter)
                        + " đến " + existingEnd.format(timeFormatter)
                        + ". Hãy đổi thời gian phù hợp!");
            }
        }
    }

    // HÀM KIỂM TRA GIỜ HOẠT ĐỘNG CỦA RẠP
    private void validateOperatingHours(LocalDateTime start, LocalDateTime end, Branch branch) {
        LocalTime opening = branch.getOpeningTime();
        LocalTime closing = branch.getClosingTime();

        LocalDateTime openingDateTime = start.toLocalDate().atTime(opening);

        LocalDateTime closingDateTime = start.toLocalDate().atTime(closing);

       if (closing.isBefore(opening)) {
            if (start.toLocalTime().isBefore(closing)) {
                openingDateTime = openingDateTime.minusDays(1);
                closingDateTime = closingDateTime.minusDays(1).plusDays(1);
            } else {
                closingDateTime = closingDateTime.plusDays(1);
            }
        }

        boolean isValidStart = !start.isBefore(openingDateTime) && !start.isAfter(closingDateTime);
        boolean isValidEnd = !end.isBefore(openingDateTime) && !end.isAfter(closingDateTime);

        if (!isValidStart || !isValidEnd) {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            throw new IllegalArgumentException(
                    "Toàn bộ thời gian chiếu (" + start.format(timeFormatter) + " - " + end.format(timeFormatter) +
                            ") phải nằm trọn trong giờ hoạt động của rạp (" + opening.format(timeFormatter) + " - " + closing.format(timeFormatter) + ")."
            );
        }
    }
}