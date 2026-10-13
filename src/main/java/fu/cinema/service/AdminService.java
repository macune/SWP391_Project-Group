package fu.cinema.service;

import fu.cinema.dto.response.StaffResponse;

import java.util.List;

public interface AdminService {
    List<StaffResponse> getAllManagers();
}
