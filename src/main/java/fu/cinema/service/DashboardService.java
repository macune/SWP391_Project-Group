package fu.cinema.service;

import fu.cinema.dto.response.DashboardResponse;

import java.time.LocalDate;

public interface DashboardService {
    DashboardResponse getDashboardData(Long branchId, String period);

    DashboardResponse.Customer getCustomerDashboardData(
            String username,
            int page,
            int pageSize,
            LocalDate showDate,
            Integer month,
            Integer year,
            LocalDate bookingDate,
            Integer bookingMonth,
            Integer bookingYear
    );

    DashboardResponse.Staff getStaffDashboardData(String username);
}
