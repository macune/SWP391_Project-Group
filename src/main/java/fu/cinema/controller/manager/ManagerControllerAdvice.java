package fu.cinema.controller.manager;

import fu.cinema.entity.Branch;
import fu.cinema.service.ManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "fu.cinema.controller.manager")
@RequiredArgsConstructor
public class ManagerControllerAdvice {

    private final ManagerService managerService;

    @ModelAttribute("currentBranch")
    public Branch populateCurrentBranch() {
        return managerService.getCurrentManagerBranch();
    }
}
