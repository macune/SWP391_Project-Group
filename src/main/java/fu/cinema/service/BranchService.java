package fu.cinema.service;

import fu.cinema.dto.request.BranchRequest;
import fu.cinema.dto.response.BranchResponse;
import fu.cinema.entity.Account;

import java.util.List;

public interface BranchService {
    List<BranchResponse> getAllBranches();
    List<Account> getEligibleUsersForManager();
    BranchResponse createBranch(BranchRequest request);
    BranchResponse updateBranch(BranchRequest request);
    void deleteBranch(Long branchId);
    void assignManager(Long branchId, Long accountId);
    void removeManager(Long branchId);
}