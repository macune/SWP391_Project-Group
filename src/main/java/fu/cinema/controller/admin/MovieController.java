package fu.cinema.controller.admin;

import fu.cinema.dto.request.MovieRequest;
import fu.cinema.dto.response.MovieResponse;
import fu.cinema.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/movies")
@PreAuthorize("hasRole('ADMIN')")
public class MovieController {

    @Autowired
    private MovieService movieService;

    // 1. HIỂN THỊ DANH SÁCH & TÌM KIẾM
    @GetMapping
    public String listMovies(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        model.addAttribute("movies", movieService.getAllMovies(keyword));
        model.addAttribute("keyword", keyword);
        return "admin/list-movies";
    }

    // 2. HIỂN THỊ FORM THÊM MỚI (RỖNG)
    @GetMapping("/add")
    public String showAddForm(Model model) {
        if (!model.containsAttribute("movieRequest")) {
            model.addAttribute("movieRequest", new MovieRequest());
        }
        return "admin/add-movie";
    }

    // 3. LƯU PHIM MỚI (CÓ CẢNH BÁO TRÙNG LẶP)
    @PostMapping("/add")
    public String processAddMovie(@ModelAttribute("movieRequest") MovieRequest request,
                                  @RequestParam(required = false, name = "confirmDuplicate") Boolean confirmDuplicate,
                                  Model model) {

        if (!Boolean.TRUE.equals(confirmDuplicate) && movieService.checkDuplicateTitle(request.getTitle())) {
            model.addAttribute("duplicateWarning", "Phim '" + request.getTitle() + "' đã tồn tại trong hệ thống! Bạn có chắc chắn muốn thêm một bản ghi mới với tên trùng lặp không?");
            model.addAttribute("movieRequest", request);
            return "admin/add-movie";
        }
        movieService.addMovie(request);
        return "redirect:/admin/movies?success=true";
    }

    // 4. LOAD DỮ LIỆU BẢN GHI CŨ
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        if (!model.containsAttribute("movieRequest")) {
            MovieResponse movie = movieService.getMovieById(id);

            // Map dữ liệu từ Response sang Request để đổ lên form HTML
            MovieRequest request = MovieRequest.builder()
                    .title(movie.getTitle())
                    .description(movie.getDescription())
                    .duration(movie.getDuration())
                    .releaseDate(movie.getReleaseDate())
                    .language(movie.getLanguage())
                    .genre(movie.getGenre())
                    .ageRating(movie.getAgeRating())
                    .posterUrl(movie.getPosterUrl())
                    .trailerUrl(movie.getTrailerUrl())
                    .status(movie.getStatus())
                    .build();
            model.addAttribute("movieRequest", request);
        }
        model.addAttribute("movieId", id);
        return "admin/edit-movie";
    }

    // 5. LƯU DỮ LIỆU SAU KHI SỬA
    @PostMapping("/edit/{id}")
    public String processEditMovie(@PathVariable("id") Long id,
                                   @ModelAttribute("movieRequest") MovieRequest request,
                                   @RequestParam(required = false, name = "confirmDuplicate") Boolean confirmDuplicate,
                                   Model model) {

        // Kiểm tra trùng tên nhưng PHẢI BỎ QUA ID của chính bộ phim đang sửa
        if (!Boolean.TRUE.equals(confirmDuplicate) && movieService.checkDuplicateTitleForEdit(request.getTitle(), id)) {

            model.addAttribute("duplicateWarning", "Tên phim '" + request.getTitle() + "' đã bị trùng với một phim khác trong hệ thống! Bạn có chắc chắn muốn đổi sang tên này không?");
            model.addAttribute("movieRequest", request);
            model.addAttribute("movieId", id);

            return "admin/edit-movie";
        }

        movieService.updateMovie(id, request);
        return "redirect:/admin/movies?success=true";
    }

    // 6. XÓA PHIM
    @GetMapping("/delete/{id}")
    public String deleteMovie(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            // Cố gắng xóa phim
            movieService.deleteMovie(id);

            // Nếu phim CHƯA TỪNG có suất chiếu -> Xóa thành công
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa phim thành công vì phim này chưa từng có lịch chiếu.");
            return "redirect:/admin/movies";

        } catch (IllegalArgumentException e) {
            // Nếu phim ĐÃ CÓ suất chiếu -> Service ném lỗi -> Bắt lỗi và hiện thông báo đỏ
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/movies";

        } catch (Exception e) {
            // Bắt các lỗi hệ thống khác
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Không thể xóa bộ phim này!");
            return "redirect:/admin/movies";
        }
    }
}