package fu.cinema.service;

import fu.cinema.dto.request.ManagerRequest;
import fu.cinema.dto.request.StaffRequest;

public interface StaffService {
    void createManager(ManagerRequest request);
    void createStaff(StaffRequest request);
    void updateManager(Long staffId, ManagerRequest request);
    void updateStaff(Long staffId, StaffRequest request);
    void deleteManager(Long staffId);
    void deleteStaff(Long staffId);
}
