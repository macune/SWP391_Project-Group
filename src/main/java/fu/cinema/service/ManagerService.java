package fu.cinema.service;

import fu.cinema.dto.response.StaffResponse;

import fu.cinema.entity.Branch;

import java.util.List;

public interface ManagerService {
    List<StaffResponse> getAllStaffs();
    Branch getCurrentManagerBranch();
}
