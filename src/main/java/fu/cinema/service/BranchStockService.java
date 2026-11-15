package fu.cinema.service;

import fu.cinema.dto.request.BranchStockRequest;
import fu.cinema.dto.response.BranchStockResponse;
import fu.cinema.dto.response.FnBItemResponse;
import fu.cinema.entity.FnBItem;

import java.util.List;
import java.util.Map;

public interface BranchStockService {
    List<BranchStockResponse> getStockByBranch(Long branchId);
    void importStock(BranchStockRequest request);

    void updateStock(Long id, Integer newQuantity);
    Long getCurrentManagerBranchId(String username);
    List<FnBItem> getAvailableFnBItems();

    Map<Long, Integer> getStockQuantityMap(Long branchId);
    List<FnBItemResponse> getAvailableFnBItemsForBranch(Long branchId);
}