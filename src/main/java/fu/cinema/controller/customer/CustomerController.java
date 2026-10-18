package fu.cinema.controller.customer;

import fu.cinema.dto.response.MovieResponse;
import fu.cinema.entity.Ticket;
import fu.cinema.enums.BookingStatus;
import fu.cinema.enums.MovieStatus;
import fu.cinema.enums.TicketStatus;
import fu.cinema.repository.BookingRepository;
import fu.cinema.repository.TicketRepository;
import fu.cinema.service.PromotionService;
import fu.cinema.service.PublicMovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor

public class CustomerController {

    private final PromotionService promotionService;
    private final PublicMovieService movieService;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    // Trang Vé của tôi
    @GetMapping("/bookings")
    public String myBookings(Model model) {
        model.addAttribute("sectionTitle", "Vé của tôi (Lịch sử đặt vé)");
        return "customer/dashboard";
    }

    //Trang Hồ sơ cá nhân
    @GetMapping("/profile")
    public String myProfile(Model model) {
        model.addAttribute("sectionTitle", "Hồ sơ cá nhân");
        return "customer/dashboard";
    }


    @GetMapping("/promotions")
    public String myPromotions(Model model) {
        model.addAttribute("sectionTitle", "Ưu đãi & Khuyến mãi của tôi");

        model.addAttribute("coupons", promotionService.getActivePromotions());
        return "customer/promotions";
    }

    // Trang Đổi mật khẩu
    @GetMapping("/change-password")
    public String changePassword(Model model) {
        model.addAttribute("sectionTitle", "Đổi mật khẩu bảo mật");
        return "customer/dashboard";
    }
    //Trang tổng quan

    @GetMapping("/dashboard")
    public String customerDashboard(
            @RequestParam(defaultValue = "0") int page,

            // Bộ lọc ngày chiếu giống Manager
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "dd/MM/yyyy")
            LocalDate showDate,

            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,

            // Bộ lọc ngày đặt vé
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "dd/MM/yyyy")
            LocalDate bookingDate,

            @RequestParam(required = false) Integer bookingMonth,
            @RequestParam(required = false) Integer bookingYear,

            Model model
    ) {
        model.addAttribute("sectionTitle", "Tổng quan");

        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        // 1. Thống kê vé sắp chiếu
        long upcomingTicketCount =
                ticketRepository
                        .countByBooking_Account_UsernameAndBooking_StatusAndStatusInAndShowtime_StartTimeAfter(
                                username,
                                BookingStatus.CONFIRMED,
                                List.of(
                                        TicketStatus.BOOKED,
                                        TicketStatus.CHECKED_IN
                                ),
                                LocalDateTime.now()
                        );

        // 2. Tổng đơn đặt vé hoàn tất
        long completedBookingCount =
                bookingRepository.countByAccount_UsernameAndStatus(
                        username,
                        BookingStatus.CONFIRMED
                );

        model.addAttribute("upcomingTicketCount", upcomingTicketCount);
        model.addAttribute("completedBookingCount", completedBookingCount);

        // 3. Tính khoảng thời gian lọc lịch chiếu
        LocalDateTime showFrom = null;
        LocalDateTime showTo = null;


        if (month != null && month >= 1 && month <= 12) {
            LocalDateTime monthFrom =
                    LocalDate.of(year != null ? year : LocalDate.now().getYear(),
                            month, 1).atStartOfDay();

            LocalDateTime monthTo =
                    monthFrom.toLocalDate().plusMonths(1).atStartOfDay();

            showFrom = showFrom == null || monthFrom.isAfter(showFrom)
                    ? monthFrom : showFrom;

            showTo = showTo == null || monthTo.isBefore(showTo)
                    ? monthTo : showTo;
        }

        if (year != null && year >= 1900 && year <= 9998) {
            LocalDateTime yearFrom =
                    LocalDate.of(year, 1, 1).atStartOfDay();

            LocalDateTime yearTo =
                    LocalDate.of(year + 1, 1, 1).atStartOfDay();

            showFrom = showFrom == null || yearFrom.isAfter(showFrom)
                    ? yearFrom : showFrom;

            showTo = showTo == null || yearTo.isBefore(showTo)
                    ? yearTo : showTo;
        }

        // 4. Tính khoảng thời gian lọc ngày đặt vé
        LocalDateTime bookingFrom = null;
        LocalDateTime bookingTo = null;

        if (bookingMonth != null
                && bookingMonth >= 1 && bookingMonth <= 12) {

            int selectedYear = bookingYear != null
                    ? bookingYear : LocalDate.now().getYear();

            YearMonth selectedMonth =
                    YearMonth.of(selectedYear, bookingMonth);

            LocalDateTime monthFrom =
                    selectedMonth.atDay(1).atStartOfDay();

            LocalDateTime monthTo =
                    selectedMonth.plusMonths(1).atDay(1).atStartOfDay();

            bookingFrom = bookingFrom == null || monthFrom.isAfter(bookingFrom)
                    ? monthFrom : bookingFrom;

            bookingTo = bookingTo == null || monthTo.isBefore(bookingTo)
                    ? monthTo : bookingTo;
        }

        if (bookingYear != null
                && bookingYear >= 1900 && bookingYear <= 9998) {

            LocalDateTime yearFrom =
                    LocalDate.of(bookingYear, 1, 1).atStartOfDay();

            LocalDateTime yearTo =
                    LocalDate.of(bookingYear + 1, 1, 1).atStartOfDay();

            bookingFrom = bookingFrom == null || yearFrom.isAfter(bookingFrom)
                    ? yearFrom : bookingFrom;

            bookingTo = bookingTo == null || yearTo.isBefore(bookingTo)
                    ? yearTo : bookingTo;
        }

        // 5. Lịch sử vé: lọc trước, phân trang sau (9 vé/trang)
        Page<Ticket> ticketPage =
                ticketRepository.searchCustomerTickets(
                        username,
                        TicketStatus.LOCKED,
                        showFrom,
                        showTo,
                        bookingFrom,
                        bookingTo,
                        PageRequest.of(Math.max(page, 0), 9)
                );

        model.addAttribute("ticketPage", ticketPage);

        // Giữ lại giá trị bộ lọc trên giao diện
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        model.addAttribute("bookingMonth", bookingMonth);
        model.addAttribute("bookingYear", bookingYear);

        // 6. Top 4 phim theo số vé đặt
        List<Object[]> topRows =
                ticketRepository.findTopMoviesByBookingCount(
                        BookingStatus.CONFIRMED,
                        List.of(
                                TicketStatus.BOOKED,
                                TicketStatus.CHECKED_IN
                        ),
                        MovieStatus.NOW_SHOWING,
                        PageRequest.of(0, 4)
                );

        List<TopMovie> topMovies = topRows.stream()
                .map(row -> new TopMovie(
                        movieService.getMovieDetail((Long) row[0]),
                        ((Number) row[1]).longValue()
                ))
                .toList();

        model.addAttribute("topMovies", topMovies);

        return "customer/dashboard";
    }



    public record TopMovie(MovieResponse movie, long ticketCount) {
    }
}