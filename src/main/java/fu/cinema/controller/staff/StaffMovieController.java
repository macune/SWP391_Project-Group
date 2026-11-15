package fu.cinema.controller.staff;

import fu.cinema.entity.Account;
import fu.cinema.service.AccountService;
import fu.cinema.service.PublicMovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

@Controller
@RequestMapping("/staff/movies")
@RequiredArgsConstructor
public class StaffMovieController {

    private final AccountService accountService;
    private final PublicMovieService movieService;

    @GetMapping("/today")
    public String viewMoviesToday(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Account staffAcc = accountService.getAccountByUsername(auth.getName());
        if (staffAcc == null || staffAcc.getStaff() == null || staffAcc.getStaff().getBranch() == null) {
            return "redirect:/";
        }
        Long branchId = staffAcc.getStaff().getBranch().getBranchId();

        model.addAttribute("movieSchedules", movieService.getTodayMovieSchedules(branchId));
        model.addAttribute("branchName", staffAcc.getStaff().getBranch().getBranchName());
        model.addAttribute("today", LocalDate.now());

        return "staff/movies-today";
    }
}