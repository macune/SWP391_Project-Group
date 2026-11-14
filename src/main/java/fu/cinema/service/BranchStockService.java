package fu.cinema.service;

import fu.cinema.dto.request.BranchStockRequest;
import fu.cinema.dto.response.BranchStockResponse;
import fu.cinema.entity.FnBItem;

import java.util.List;

public interface BranchStockService {
    List<BranchStockResponse> getStockByBranch(Long branchId);
    void importStock(BranchStockRequest request);


    void updateStock(Long id, Integer newQuantity);
    void deleteStock(Long id);
    Long getCurrentManagerBranchId(String username);
    List<FnBItem> getAvailableFnBItems();
}