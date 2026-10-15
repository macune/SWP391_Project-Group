package fu.cinema.controller.staff;

import fu.cinema.dto.response.MovieResponse;
import fu.cinema.entity.Account;
import fu.cinema.repository.AccountRepository;
import fu.cinema.service.PublicMovieService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/staff/movies")
@RequiredArgsConstructor
public class StaffMovieController {

    private final AccountRepository accountRepository;
    private final PublicMovieService movieService; // Tái sử dụng Service y hệt Customer

    @GetMapping("/today")
    public String viewMoviesToday(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Account staffAcc = accountRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        Long branchId = staffAcc.getStaff().getBranch().getBranchId();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // Lấy tất cả phim đang chiếu
        List<MovieResponse> nowShowingMovies = movieService.getMoviesByStatus("NOW_SHOWING");

        // Tạo List chứa Phim kèm Suất chiếu để ném ra View (thay thế Map)
        List<MovieSchedule> movieSchedules = new ArrayList<>();

        for (MovieResponse movie : nowShowingMovies) {
            // Tận dụng hàm getShowtimes (đã tính sẵn ghế trống, hallCapacity)
            var allShowtimes = movieService.getShowtimes(movie.getMovieId(), branchId, today, null);

            // Lọc: Chỉ lấy các suất chiếu TỪ THỜI ĐIỂM HIỆN TẠI TRỞ ĐI
            var futureShowtimes = allShowtimes.stream()
                    .filter(st -> !st.getStartTime().isBefore(now))
                    .collect(Collectors.toList());

            // Chỉ thêm phim vào danh sách hiển thị nếu phim đó CÒN suất chiếu trong tương lai hôm nay
            if (!futureShowtimes.isEmpty()) {
                movieSchedules.add(new MovieSchedule(movie, futureShowtimes));
            }
        }

        model.addAttribute("movieSchedules", movieSchedules);
        model.addAttribute("branchName", staffAcc.getStaff().getBranch().getBranchName());
        model.addAttribute("today", today);

        return "staff/movies-today";
    }

    // Class nội bộ dùng để gói data truyền ra View siêu sạch
    @Data
    @AllArgsConstructor
    public static class MovieSchedule {
        private MovieResponse movie;
        private List<?> showtimes; // Chứa List DTO ShowtimeResponse của bạn
    }
}